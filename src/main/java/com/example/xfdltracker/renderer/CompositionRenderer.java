package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetCompositionEdge;
import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetNodeIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link AtomicWebSquareRenderer}가 만든 per-node fragment들을 {@code TargetCompositionPlan.getEdges()}가
 * 확정한 parent-slot-child 순서 그대로 하나의 target DOM tree로 조립한다. source DOM/geometry는 참조하지 않는다.
 * missing edge/identity mismatch는 추론하지 않고 {@link CompositionRenderStatus}로 보고한다.
 */
public final class CompositionRenderer {

    /**
     * @param plan 이미 검증된 {@link TargetCompositionPlan}
     * @param atomicResults plan.getNodes()와 같은 nodeId 집합을 가리키는 결과 목록
     * @return plan.getNodes()와 정확히 같은 순서/개수의 결과
     */
    public List<CompositionRenderResult> render(TargetCompositionPlan plan, List<AtomicRenderResult> atomicResults) {
        Map<TargetNodeIdentity, AtomicRenderResult> atomicByIdentity = indexAtomicByIdentity(plan, atomicResults);
        Map<String, List<TargetCompositionEdge>> edgesByParentNodeId = indexEdgesByParent(plan);
        Document doc = newTargetDocument();
        IdentityHashMap<TargetCompositionNode, CompositionRenderResult> memo =
                new IdentityHashMap<TargetCompositionNode, CompositionRenderResult>();

        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>();
        for (TargetCompositionNode node : plan.getNodes()) {
            results.add(compose(node, atomicByIdentity, edgesByParentNodeId, doc, memo));
        }
        return results;
    }

    /**
     * exact {@code (IDENTITY_KIND, IDENTITY_VALUE)} tuple({@link TargetNodeIdentity#equals})로
     * 색인하는 lookup 전용 map -- provenance authority는 각 result/node 자신에 있다. 중복
     * tuple이나 Plan node에 대응하지 않는 tuple은 fail-closed.
     */
    private Map<TargetNodeIdentity, AtomicRenderResult> indexAtomicByIdentity(
            TargetCompositionPlan plan, List<AtomicRenderResult> atomicResults) {
        Map<TargetNodeIdentity, AtomicRenderResult> index = new LinkedHashMap<TargetNodeIdentity, AtomicRenderResult>();
        if (atomicResults != null) {
            for (AtomicRenderResult result : atomicResults) {
                if (index.containsKey(result.getIdentity())) {
                    throw new IllegalStateException(
                            "composition_renderer: duplicate AtomicRenderResult for identity="
                                    + result.getIdentity());
                }
                index.put(result.getIdentity(), result);
            }
        }
        Set<TargetNodeIdentity> planIdentities = new LinkedHashSet<TargetNodeIdentity>();
        for (TargetCompositionNode node : plan.getNodes()) {
            planIdentities.add(node.getIdentity());
        }
        for (TargetNodeIdentity resultIdentity : index.keySet()) {
            if (!planIdentities.contains(resultIdentity)) {
                throw new IllegalStateException(
                        "composition_renderer: orphan AtomicRenderResult -- identity=\"" + resultIdentity
                                + "\" does not correspond to any node in the given TargetCompositionPlan "
                                + "(stale/tampered identity; refusing to silently ignore it)");
            }
        }
        return index;
    }

    private Map<String, List<TargetCompositionEdge>> indexEdgesByParent(TargetCompositionPlan plan) {
        Map<String, List<TargetCompositionEdge>> index = new LinkedHashMap<String, List<TargetCompositionEdge>>();
        for (TargetCompositionEdge edge : plan.getEdges()) {
            String parentNodeId = edge.getParent().getNodeId();
            List<TargetCompositionEdge> bucket = index.get(parentNodeId);
            if (bucket == null) {
                bucket = new ArrayList<TargetCompositionEdge>();
                index.put(parentNodeId, bucket);
            }
            bucket.add(edge);
        }
        return index;
    }

    /**
     * node 하나를 composed 상태로 만든다(memoized -- 같은 node 객체가 여러 parent edge에서
     * 재참조돼도 재조립하지 않는다). 매번 importNode로 fresh clone을 만들어 붙이므로 DOM
     * ownership 충돌이 없다.
     */
    private CompositionRenderResult compose(
            TargetCompositionNode node, Map<TargetNodeIdentity, AtomicRenderResult> atomicByIdentity,
            Map<String, List<TargetCompositionEdge>> edgesByParentNodeId, Document doc,
            IdentityHashMap<TargetCompositionNode, CompositionRenderResult> memo) {
        CompositionRenderResult cached = memo.get(node);
        if (cached != null) {
            return cached;
        }

        AtomicRenderResult atomic = atomicByIdentity.get(node.getIdentity());
        if (atomic == null) {
            CompositionRenderResult result = CompositionRenderResult.notComposed(
                    node.getIdentity(), CompositionRenderStatus.INTEGRITY_VIOLATION,
                    "missing_atomic_result_for_node:" + node.getIdentity());
            memo.put(node, result);
            return result;
        }
        if (atomic.getStatus() != RenderStatus.RENDERED) {
            CompositionRenderResult result = CompositionRenderResult.notComposed(
                    node.getIdentity(), CompositionRenderStatus.ATOMIC_RENDER_UNAVAILABLE,
                    "atomic_render_not_available:" + atomic.getStatus() + ":" + atomic.getFailureReason());
            memo.put(node, result);
            return result;
        }

        Element element = (Element) doc.importNode(atomic.getTargetElement(), true);

        List<TargetCompositionEdge> edges = edgesByParentNodeId.get(node.getNodeId());
        if (edges != null) {
            for (TargetCompositionEdge edge : edges) {
                TargetCompositionNode child = edge.getChild();
                CompositionRenderResult childResult =
                        compose(child, atomicByIdentity, edgesByParentNodeId, doc, memo);
                if (childResult.getStatus() != CompositionRenderStatus.RENDERED) {
                    CompositionRenderResult result = CompositionRenderResult.notComposed(
                            node.getIdentity(), CompositionRenderStatus.CHILD_COMPOSITION_FAILED,
                            "child_composition_failed:slot=" + edge.getSlot() + ":childIdentity="
                                    + child.getIdentity() + ":childStatus=" + childResult.getStatus());
                    memo.put(node, result);
                    return result;
                }
                SlotAttachment attachment = SlotAttachment.lookup(node.getFamily(), edge.getSlot());
                if (attachment == null) {
                    CompositionRenderResult result = CompositionRenderResult.notComposed(
                            node.getIdentity(), CompositionRenderStatus.UNSUPPORTED_SLOT_ATTACHMENT,
                            "unsupported_slot_attachment:family=" + node.getFamily() + ":slot=" + edge.getSlot());
                    memo.put(node, result);
                    return result;
                }
                Element childElement = (Element) doc.importNode(childResult.getTargetElement(), true);

                // page-ordinal-aware 경로는 일반 wrapper 경로와 분리된다 -- pageOrdinal의
                // non-null/범위/page attachment 존재를 여기서도 재확인한다(upstream을 맹신하지 않음).
                if (attachment.isPageOrdinalAware()) {
                    Integer pageOrdinal = edge.getPageOrdinal();
                    if (pageOrdinal == null || pageOrdinal.intValue() < 0) {
                        CompositionRenderResult result = CompositionRenderResult.notComposed(
                                node.getIdentity(), CompositionRenderStatus.INTEGRITY_VIOLATION,
                                "panes_edge_missing_or_negative_page_ordinal:slot=" + edge.getSlot()
                                        + ":pageOrdinal=" + pageOrdinal);
                        memo.put(node, result);
                        return result;
                    }
                    Element originalPageContent = atomic.getPageContentAttachments().get(pageOrdinal);
                    if (originalPageContent == null) {
                        CompositionRenderResult result = CompositionRenderResult.notComposed(
                                node.getIdentity(), CompositionRenderStatus.INTEGRITY_VIOLATION,
                                "missing_page_attachment:family=" + node.getFamily() + ":pageOrdinal=" + pageOrdinal);
                        memo.put(node, result);
                        return result;
                    }
                    Element importedPageContent =
                            locateImportedEquivalent(atomic.getTargetElement(), element, originalPageContent);
                    attachment.attachToPage(childElement, importedPageContent);
                } else {
                    if (edge.getPageOrdinal() != null) {
                        CompositionRenderResult result = CompositionRenderResult.notComposed(
                                node.getIdentity(), CompositionRenderStatus.INTEGRITY_VIOLATION,
                                "non_panes_edge_carrying_page_ordinal:family=" + node.getFamily()
                                        + ":slot=" + edge.getSlot() + ":pageOrdinal=" + edge.getPageOrdinal());
                        memo.put(node, result);
                        return result;
                    }
                    attachment.attach(doc, element, childElement);
                }
            }
        }

        CompositionRenderResult result = CompositionRenderResult.composed(node.getIdentity(), element);
        memo.put(node, result);
        return result;
    }

    /**
     * {@code element}는 importNode deep clone이므로 원본 {@code originalTarget}과 identity가
     * 다르다. child index 순서(importNode가 보존하는 유일한 대응 관계)로 원본 경로를 구해 clone
     * 쪽에서 동일 경로를 다시 밟는다 -- family별 DOM 레이아웃을 몰라도 된다.
     */
    private Element locateImportedEquivalent(Element originalRoot, Element importedRoot, Element originalTarget) {
        List<Integer> path = new ArrayList<Integer>();
        Node current = originalTarget;
        while (current != originalRoot) {
            Node parent = current.getParentNode();
            if (parent == null) {
                throw new IllegalStateException(
                        "composition_renderer: pageContentAttachments target is not in the atomic result's own "
                                + "targetElement subtree");
            }
            path.add(0, Integer.valueOf(indexOfChild(parent, current)));
            current = parent;
        }
        Node walker = importedRoot;
        for (Integer index : path) {
            walker = walker.getChildNodes().item(index.intValue());
        }
        return (Element) walker;
    }

    private int indexOfChild(Node parent, Node child) {
        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) == child) {
                return i;
            }
        }
        throw new IllegalStateException("composition_renderer: child not found among parent's own childNodes");
    }

    private Document newTargetDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new IllegalStateException("composition_renderer: failed to create target document", e);
        }
    }
}
