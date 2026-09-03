package com.example.xfdltracker.composition;

import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SemanticRegionResult} 목록과 원본 source {@code Element} 트리를 받아, 실제 DOM에서 확정적으로
 * 계산 가능한 구조적 관계만 담은 {@link SemanticRegionGraph}를 만든다(geometry/proximity 발명 금지).
 * duplicate anchor defense: 서로 다른 두 결과가 같은 sourceStructuralId를 가지면 {@link IllegalStateException}을 던진다.
 */
public class SemanticRegionRelationshipExtractor {

    /**
     * @param root segment(root, ...)에 실제로 넘겨졌던 것과 동일한 source Element 트리
     * @param results segmenter가 conflict를 해소해 발행한 결과 목록(순서 무관)
     * @throws IllegalStateException 서로 다른 두 결과가 같은 sourceStructuralId를 가질 때
     */
    public SemanticRegionGraph buildGraph(Element root, List<SemanticRegionResult> results) {
        SemanticRegionGraph graph = new SemanticRegionGraph();
        if (root == null || results == null || results.isEmpty()) {
            return graph;
        }

        Map<String, Element> elementByStructuralId = new LinkedHashMap<String, Element>();
        collectAllStructuralIds(root, elementByStructuralId);

        // SPLIT_LAYOUT.columns의 geometry 순서 근거는 structuralId 완전 일치(exact-lookup) 방식으로만 조회한다.
        Map<String, Map<String, Integer>> splitColumnGeometryOrderByParentStructuralId =
                new LinkedHashMap<String, Map<String, Integer>>();
        for (SemanticRegionResult result : results) {
            String structuralId = result.getSourceStructuralId();
            if (structuralId == null || structuralId.length() == 0) {
                continue;
            }
            if (!result.getSplitColumnGeometryOrderBySiblingStructuralId().isEmpty()) {
                splitColumnGeometryOrderByParentStructuralId.put(
                        structuralId, result.getSplitColumnGeometryOrderBySiblingStructuralId());
            }
        }

        Set<String> seenStructuralIds = new LinkedHashSet<String>();
        List<String> nodeIds = new ArrayList<String>();
        for (SemanticRegionResult result : results) {
            String structuralId = result.getSourceStructuralId();
            if (structuralId == null || structuralId.length() == 0) {
                continue;
            }
            if (!seenStructuralIds.add(structuralId)) {
                throw new IllegalStateException(
                        "duplicate_structural_anchor: two different SemanticRegionResult entries share "
                                + "sourceStructuralId=" + structuralId + " -- this violates the ownership "
                                + "invariant that a single real DOM anchor must not carry two distinct "
                                + "canonical results; refusing to silently merge or double-register");
            }
            Element anchor = elementByStructuralId.get(structuralId);
            if (anchor == null) {
                // sourceStructuralId가 root 아래에서 실제로 발견된 Element와 대응하지 않는 경우
                // (예: root mismatch)이며, 이때 임의로 node를 만들어내지 않는다.
                continue;
            }
            graph.addNode(new SemanticRegionGraphNode(structuralId, result.getSourceRegionId(), result.getSemanticType()));
            nodeIds.add(structuralId);
        }

        for (int i = 0; i < nodeIds.size(); i++) {
            for (int j = 0; j < nodeIds.size(); j++) {
                if (i == j) {
                    continue;
                }
                String aId = nodeIds.get(i);
                String bId = nodeIds.get(j);
                SemanticRegionRelationship containment = containmentRelationship(
                        aId, elementByStructuralId.get(aId), bId, elementByStructuralId.get(bId),
                        splitColumnGeometryOrderByParentStructuralId.get(aId));
                if (containment != null) {
                    graph.addRelationship(containment);
                }
            }
        }

        for (int i = 0; i < nodeIds.size(); i++) {
            for (int j = i + 1; j < nodeIds.size(); j++) {
                String aId = nodeIds.get(i);
                String bId = nodeIds.get(j);
                SemanticRegionRelationship sibling = siblingRelationship(
                        aId, elementByStructuralId.get(aId), bId, elementByStructuralId.get(bId));
                if (sibling != null) {
                    graph.addRelationship(sibling);
                }
            }
        }

        return graph;
    }

    /**
     * {@code element}부터 모든 자손 {@code Element}까지 {@link SourceStructuralIdentity#build}로 계산한
     * identity를 key로 채운다(순회 순서 무관). 이 메서드 호출 동안만 존재하는 지역 자료구조이며
     * {@link SemanticRegionGraph}/{@link SemanticRegionRelationship}에는 저장되지 않는다.
     */
    private void collectAllStructuralIds(Element element, Map<String, Element> out) {
        String structuralId = SourceStructuralIdentity.build(element);
        if (structuralId.length() > 0) {
            out.put(structuralId, element);
        }
        for (Element child : directElementChildren(element)) {
            collectAllStructuralIds(child, out);
        }
    }

    /**
     * {@code a}가 {@code b}의 조상인지 {@code getParentNode()} 체인으로 확인한다(hops=1이면
     * DIRECT_PARENT, 2 이상이면 ANCESTOR_CONTAINS). {@code ancestorGeometryOrder}가 주어지면 entry
     * child의 structuralId로 exact-key 조회해 geometry rank를 relationship에 싣는다.
     */
    private SemanticRegionRelationship containmentRelationship(
            String aId, Element a, String bId, Element b, Map<String, Integer> ancestorGeometryOrder) {
        if (a == null || b == null) {
            return null;
        }
        int hops = 0;
        Element entryChild = b;
        Node current = b.getParentNode();
        while (current instanceof Element) {
            hops++;
            if (current == a) {
                SemanticRegionRelationship.RelationshipType type = hops == 1
                        ? SemanticRegionRelationship.RelationshipType.DIRECT_PARENT
                        : SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS;
                Integer rank = null;
                if (ancestorGeometryOrder != null && !ancestorGeometryOrder.isEmpty()) {
                    rank = ancestorGeometryOrder.get(SourceStructuralIdentity.build(entryChild));
                }
                return new SemanticRegionRelationship(
                        aId, bId, type, null, "dom_ancestor_hops=" + hops, rank);
            }
            entryChild = (Element) current;
            current = current.getParentNode();
        }
        return null;
    }

    /**
     * {@code a}/{@code b}의 anchor Element가 실제로 동일한 DOM 부모의 직계 자식인지 확인한다.
     * 같은 부모면 그 부모의 직계 Element 자식 목록에서 각자의 index로 source 순서를 정해
     * (먼저 오는 쪽이 {@code from}) {@code DIRECT_SIBLING_ORDER} 관계 하나를 반환한다.
     */
    private SemanticRegionRelationship siblingRelationship(
            String aId, Element a, String bId, Element b) {
        if (a == null || b == null) {
            return null;
        }
        Node parentA = a.getParentNode();
        if (parentA == null || parentA != b.getParentNode() || !(parentA instanceof Element)) {
            return null;
        }
        List<Element> siblings = directElementChildren((Element) parentA);
        int indexA = indexOfIdentity(siblings, a);
        int indexB = indexOfIdentity(siblings, b);
        if (indexA < 0 || indexB < 0 || indexA == indexB) {
            return null;
        }
        boolean aFirst = indexA < indexB;
        String earlierId = aFirst ? aId : bId;
        String laterId = aFirst ? bId : aId;
        int earlierIndex = Math.min(indexA, indexB);
        return new SemanticRegionRelationship(
                earlierId, laterId, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER,
                Integer.valueOf(earlierIndex), "dom_sibling_index=" + earlierIndex);
    }

    private int indexOfIdentity(List<Element> elements, Element target) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private List<Element> directElementChildren(Element parent) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }
}
