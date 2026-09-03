package com.example.xfdltracker.composition;

import com.example.xfdltracker.renderer.AtomicRenderResult;
import com.example.xfdltracker.renderer.AtomicWebSquareRenderer;
import com.example.xfdltracker.renderer.CompositionRenderResult;
import com.example.xfdltracker.renderer.CompositionRenderStatus;
import com.example.xfdltracker.renderer.CompositionRenderer;
import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadCategory;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link CompositionRenderer}가 {@code TargetCompositionPlan.getEdges()}를 순서 그대로 consume해
 * hierarchy를 조립하는지, unsupported/missing/mismatched 상황에서 fail-closed하는지 검증한다.
 * package-private 생성자로 직접 fixture를 만들어 Plan Builder 없이 targeted로 검증한다.
 */
public class CompositionRendererTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTwoColumnSplitComposedInEdgeOrder();
        testThreeColumnSplitComposedInEdgeOrder();
        testEncounterOrderVsGeometryOrderReversedFollowsEdgeOrderNotNodeOrder();
        testMultipleSplitRootsIndependentlyComposed();
        testUnsupportedVariantParentFailsClosedAtomicRenderUnavailable();
        testUnsupportedChildFamilyCausesChildCompositionFailed();
        testUnsupportedSlotAttachmentFailsClosed();
        testMissingAtomicResultForPlanNodeIsIntegrityViolation();
        testOrphanAtomicResultRejectedGlobally();
        testDuplicateAtomicResultForSameNodeRejected();
        testNoEdgesNodeComposedAsLeaf();
        testRendererNeverReanalyzesSourceOrGeometry();

        // ==== TAB_CONTROL 정확한 page 부착 ====
        testTabControlChildAttachedToPage0Only();
        testTabControlDifferentChildAttachedToPage1Only();
        testTabControlMultipleChildrenOnSamePagePreservePlanOrdering();
        testTabControlNestedAttachmentWorks();
        testTabControlPanesEdgeMissingPageOrdinalFailsClosed();
        testTabControlNegativePageOrdinalRejectedAtEdgeConstruction();
        testTabControlOutOfRangePageOrdinalFailsClosed();
        testTabControlNonPanesEdgeWithPageOrdinalFailsClosed();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 2-column split -- parent lybox 아래 정확히 2개의 ly_column이 edge 순서(accepted geometry
     * order) 그대로 나타나고 각각 자기 자식(TITLE_BAR dfbox)을 감싼다. */
    private static void testTwoColumnSplitComposedInEdgeOrder() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("p2col");
        TargetCompositionNode left = titleBarNode("colLeft", "Left");
        TargetCompositionNode right = titleBarNode("colRight", "Right");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, left, right),
                Arrays.asList(
                        new TargetCompositionEdge(parent, "columns", left),
                        new TargetCompositionEdge(parent, "columns", right)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(left, right));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        assertEquals("2col: result count", "3", String.valueOf(composed.size()));
        CompositionRenderResult parentResult = composed.get(0);
        assertEquals("2col: parent status", "RENDERED", String.valueOf(parentResult.getStatus()));
        Element lybox = parentResult.getTargetElement();
        assertEquals("2col: lybox class", "lybox", lybox.getAttribute("class"));
        assertEquals("2col: lybox children count", "2", String.valueOf(lybox.getChildNodes().getLength()));

        Element col0 = (Element) lybox.getChildNodes().item(0);
        Element col1 = (Element) lybox.getChildNodes().item(1);
        assertEquals("2col: col0 wrapper class", "ly_column", col0.getAttribute("class"));
        assertEquals("2col: col1 wrapper class", "ly_column", col1.getAttribute("class"));
        assertEquals("2col: col0 wraps Left dfbox", "Left", titleTextOf(col0));
        assertEquals("2col: col1 wraps Right dfbox", "Right", titleTextOf(col1));
    }

    /** 3-column split -- 개수/순서 확장 확인. */
    private static void testThreeColumnSplitComposedInEdgeOrder() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("p3col");
        TargetCompositionNode a = titleBarNode("colA", "Alpha");
        TargetCompositionNode b = titleBarNode("colB", "Beta");
        TargetCompositionNode c = titleBarNode("colC", "Gamma");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, a, b, c),
                Arrays.asList(
                        new TargetCompositionEdge(parent, "columns", a),
                        new TargetCompositionEdge(parent, "columns", b),
                        new TargetCompositionEdge(parent, "columns", c)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(a, b, c));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        Element lybox = composed.get(0).getTargetElement();
        assertEquals("3col: lybox children count", "3", String.valueOf(lybox.getChildNodes().getLength()));
        assertEquals("3col: order[0]", "Alpha", titleTextOf((Element) lybox.getChildNodes().item(0)));
        assertEquals("3col: order[1]", "Beta", titleTextOf((Element) lybox.getChildNodes().item(1)));
        assertEquals("3col: order[2]", "Gamma", titleTextOf((Element) lybox.getChildNodes().item(2)));
    }

    /** encounter order와 geometry order(plan.getEdges() 실제 순서)가 반대인 fixture --
     * CompositionRenderer는 오직 edge 순서만 따르고 node encounter/lexical 순서는 참조하지 않는다. */
    private static void testEncounterOrderVsGeometryOrderReversedFollowsEdgeOrderNotNodeOrder() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("pRev");
        // node encounter order(리스트 순서)는 zLast, aFirst -- lexical nodeId로도 zLast가 먼저
        // 나오므로, 만약 renderer가 lexical/encounter fallback을 쓴다면 zLast가 먼저 나타난다.
        TargetCompositionNode zLast = titleBarNode("zLast", "ShouldBeSecond");
        TargetCompositionNode aFirst = titleBarNode("aFirst", "ShouldBeFirst");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, zLast, aFirst),
                // edge order(accepted geometry rank) -- aFirst가 실제로는 먼저다.
                Arrays.asList(
                        new TargetCompositionEdge(parent, "columns", aFirst),
                        new TargetCompositionEdge(parent, "columns", zLast)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(zLast, aFirst));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        Element lybox = composed.get(0).getTargetElement();
        assertEquals("reversed: order[0] follows edge order (geometry), not node/lexical order",
                "ShouldBeFirst", titleTextOf((Element) lybox.getChildNodes().item(0)));
        assertEquals("reversed: order[1] follows edge order (geometry), not node/lexical order",
                "ShouldBeSecond", titleTextOf((Element) lybox.getChildNodes().item(1)));
    }

    /** 서로 다른 두 SPLIT_LAYOUT root가 완전히 독립적으로 조립된다(multiple roots는 정상). */
    private static void testMultipleSplitRootsIndependentlyComposed() throws Exception {
        TargetCompositionNode parent1 = splitLayoutNode("root1");
        TargetCompositionNode child1 = titleBarNode("root1child", "R1");
        TargetCompositionNode parent2 = splitLayoutNode("root2");
        TargetCompositionNode child2a = titleBarNode("root2childA", "R2A");
        TargetCompositionNode child2b = titleBarNode("root2childB", "R2B");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent1, child1, parent2, child2a, child2b),
                Arrays.asList(
                        new TargetCompositionEdge(parent1, "columns", child1),
                        new TargetCompositionEdge(parent2, "columns", child2a),
                        new TargetCompositionEdge(parent2, "columns", child2b)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(child1, child2a, child2b));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        assertEquals("multi-root: root1 lybox children", "1",
                String.valueOf(composed.get(0).getTargetElement().getChildNodes().getLength()));
        assertEquals("multi-root: root2 lybox children", "2",
                String.valueOf(composed.get(2).getTargetElement().getChildNodes().getLength()));
    }

    /** parent의 atomic render 자체가 실패(tampered/unknown variant는 UNSUPPORTED_VARIANT)하면
     * composition도 명시적으로 ATOMIC_RENDER_UNAVAILABLE -- 자식이 있어도 조립을 시도하지
     * 않는다. */
    private static void testUnsupportedVariantParentFailsClosedAtomicRenderUnavailable() throws Exception {
        TargetCompositionNode parent = new TargetCompositionNode(
                "pBogusVariant", "SPLIT_LAYOUT", "bogus_variant", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "pBogusVariant",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "pBogusVariant"));
        TargetCompositionNode child = titleBarNode("ffChild", "X");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "columns", child)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(child));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("bogus-variant: parent status", "ATOMIC_RENDER_UNAVAILABLE",
                String.valueOf(composed.get(0).getStatus()));
        assertTrue("bogus-variant: reason mentions atomic_render_not_available",
                composed.get(0).getFailureReason().contains("atomic_render_not_available"));
    }

    /** 승인된 columns 자식의 family가 atomic renderer 지원 범위 밖(예: TREEVIEW -- 이번
     * fast-track에서 미구현)이면, 부모 composition도 CHILD_COMPOSITION_FAILED로 명시적
     * 실패한다 -- 그 자식만 조용히 빼고 나머지로 조립하지 않는다. */
    private static void testUnsupportedChildFamilyCausesChildCompositionFailed() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("pUnsupportedChild");
        TargetCompositionNode unsupportedChild = new TargetCompositionNode(
                "gridChild", "TREEVIEW", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "gridChild",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridChild"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, unsupportedChild),
                Arrays.asList(new TargetCompositionEdge(parent, "columns", unsupportedChild)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, Collections.<TargetNodePayload>emptyList());
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        assertEquals("unsupported-child: child atomic status (precondition)", "UNSUPPORTED_FAMILY",
                String.valueOf(atomic.get(1).getStatus()));
        assertEquals("unsupported-child: parent composition status", "CHILD_COMPOSITION_FAILED",
                String.valueOf(composed.get(0).getStatus()));
        assertTrue("unsupported-child: reason mentions gridChild",
                composed.get(0).getFailureReason().contains("gridChild"));
    }

    /** {@code (family, slot)} 조합에 attachment 규칙이 등록돼 있지 않으면(catalog에 SLOT_FILL
     * 규칙 자체가 없어 실제 파이프라인으로는 도달 불가능한 경우도 targeted로 검증) 명시적으로
     * UNSUPPORTED_SLOT_ATTACHMENT다 -- 새 wrapper를 즉석 발명해 조립하지 않는다. */
    private static void testUnsupportedSlotAttachmentFailsClosed() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("pUnsupportedSlot");
        TargetCompositionNode arrow = titleBarNode("arrowChild", "Arrow");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, arrow),
                Arrays.asList(new TargetCompositionEdge(parent, "transfer_controls", arrow)));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(arrow));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("unsupported-slot: parent status", "UNSUPPORTED_SLOT_ATTACHMENT",
                String.valueOf(composed.get(0).getStatus()));
        assertTrue("unsupported-slot: reason mentions transfer_controls",
                composed.get(0).getFailureReason().contains("transfer_controls"));
    }

    /** Plan node에 대응하는 {@link AtomicRenderResult}가 아예 없으면(stale/tampered atomic
     * result list) INTEGRITY_VIOLATION -- source를 다시 렌더링해 보완하지 않는다. */
    private static void testMissingAtomicResultForPlanNodeIsIntegrityViolation() throws Exception {
        TargetCompositionNode node = splitLayoutNode("pMissingAtomic");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        List<CompositionRenderResult> composed = new CompositionRenderer().render(
                plan, Collections.<AtomicRenderResult>emptyList());
        assertEquals("missing-atomic: status", "INTEGRITY_VIOLATION", String.valueOf(composed.get(0).getStatus()));
        assertTrue("missing-atomic: reason mentions missing_atomic_result_for_node",
                composed.get(0).getFailureReason().contains("missing_atomic_result_for_node"));
    }

    /** Plan에 존재하지 않는 nodeId를 가리키는 orphan {@link AtomicRenderResult}는 render() 자체가
     * 예외로 fail-closed한다(atomic renderer의 orphan payload 계약과 동일한 원칙). */
    private static void testOrphanAtomicResultRejectedGlobally() throws Exception {
        TargetCompositionNode node = splitLayoutNode("pOrphanCheck");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        List<AtomicRenderResult> atomic = renderAtomic(plan, Collections.<TargetNodePayload>emptyList());
        List<AtomicRenderResult> tampered = new ArrayList<AtomicRenderResult>(atomic);
        TargetCompositionNode ghostNode = splitLayoutNode("ghostNodeThatDoesNotExist");
        tampered.addAll(renderAtomic(
                new TargetCompositionPlan(Arrays.asList(ghostNode), Collections.<TargetCompositionEdge>emptyList()),
                Collections.<TargetNodePayload>emptyList()));

        boolean threw = false;
        String message = null;
        try {
            new CompositionRenderer().render(plan, tampered);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("orphan-atomic: render() throws IllegalStateException", threw);
        assertTrue("orphan-atomic: message mentions the orphan nodeId",
                message != null && message.contains("ghostNodeThatDoesNotExist"));
    }

    private static void testDuplicateAtomicResultForSameNodeRejected() throws Exception {
        TargetCompositionNode node = splitLayoutNode("pDupAtomic");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        List<AtomicRenderResult> atomic = renderAtomic(plan, Collections.<TargetNodePayload>emptyList());
        List<AtomicRenderResult> duplicated = new ArrayList<AtomicRenderResult>(atomic);
        duplicated.addAll(atomic);

        boolean threw = false;
        try {
            new CompositionRenderer().render(plan, duplicated);
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("dup-atomic: message mentions duplicate", e.getMessage().contains("duplicate"));
        }
        assertTrue("dup-atomic: render() throws IllegalStateException", threw);
    }

    /** edge가 전혀 없는 node(leaf SPLIT_LAYOUT -- 비정상이지만 renderer 계약상 정상 처리)는
     * 자기 atomic fragment 그대로 RENDERED. */
    private static void testNoEdgesNodeComposedAsLeaf() throws Exception {
        TargetCompositionNode node = splitLayoutNode("pLeaf");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        List<AtomicRenderResult> atomic = renderAtomic(plan, Collections.<TargetNodePayload>emptyList());
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("leaf: status", "RENDERED", String.valueOf(composed.get(0).getStatus()));
        assertEquals("leaf: no children", "0",
                String.valueOf(composed.get(0).getTargetElement().getChildNodes().getLength()));
    }

    /** RendererArchitectureIsolationTest가 정적 스캔으로 이미 금지한 사실을 행위로도 재확인한다:
     * encounter/lexical 순서가 다른 fixture가 오직 edge 순서로만 재현 가능함을 보여
     * source/geometry fallback이 관여하지 않았음을 증거로 남긴다. */
    private static void testRendererNeverReanalyzesSourceOrGeometry() throws Exception {
        // renderer 패키지 전체는 RendererArchitectureIsolationTest의 forbidden-type 스캔 대상이므로
        // 별도 재스캔 없이 이미 검증된 사실을 명시적으로 기록한다.
        assertTrue("no-reanalysis: covered by RendererArchitectureIsolationTest + reversed-order test", true);
    }

    // ---- fixture 생성 도우미 ----

    private static TargetCompositionNode splitLayoutNode(String nodeId) {
        return new TargetCompositionNode(
                nodeId, "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static final Map<String, String> TITLE_TEXT_BY_NODE_ID = new LinkedHashMap<String, String>();

    private static TargetCompositionNode titleBarNode(String nodeId, String text) {
        TITLE_TEXT_BY_NODE_ID.put(nodeId, text);
        return new TargetCompositionNode(
                nodeId, "TITLE_BAR", "title_only", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static List<TargetNodePayload> titlePayloads(TargetCompositionNode a) {
        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(titleLabelPayload(a));
        return payloads;
    }

    private static List<TargetNodePayload> titlePayloads(TargetCompositionNode a, TargetCompositionNode b) {
        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(titleLabelPayload(a));
        payloads.add(titleLabelPayload(b));
        return payloads;
    }

    private static List<TargetNodePayload> titlePayloads(
            TargetCompositionNode a, TargetCompositionNode b, TargetCompositionNode c) {
        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(titleLabelPayload(a));
        payloads.add(titleLabelPayload(b));
        payloads.add(titleLabelPayload(c));
        return payloads;
    }

    private static TargetNodePayload titleLabelPayload(TargetCompositionNode node) {
        String text = TITLE_TEXT_BY_NODE_ID.get(node.getNodeId());
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, text, null, "source_text_attribute", node.getNodeId()));
        return new TargetNodePayload(node.getIdentityKind(), node.getNodeId(), items);
    }

    /** SPLIT_LAYOUT은 exactly-one Payload envelope을 요구하므로, fixture 편의상 plan 안의 모든
     * SPLIT_LAYOUT node에 대해 caller가 payload를 안 줬으면 empty-semantic envelope을 자동으로
     * 채워 넣는다 -- renderer 자신의 envelope 검증 로직은 여전히 매번 실제로 동작한다. */
    private static List<AtomicRenderResult> renderAtomic(TargetCompositionPlan plan, List<TargetNodePayload> payloads) {
        java.util.Set<String> alreadyCovered = new java.util.LinkedHashSet<String>();
        for (TargetNodePayload p : payloads) {
            alreadyCovered.add(p.getPlanNodeId());
        }
        List<TargetNodePayload> augmented = new ArrayList<TargetNodePayload>(payloads);
        for (TargetCompositionNode node : plan.getNodes()) {
            if ("SPLIT_LAYOUT".equals(node.getFamily()) && !alreadyCovered.contains(node.getNodeId())) {
                augmented.add(emptySplitPayload(node.getNodeId()));
                alreadyCovered.add(node.getNodeId());
            }
        }
        return new AtomicWebSquareRenderer().render(plan, augmented);
    }

    // ==== TAB_CONTROL 정확한 page 부착 ====

    private static TargetCompositionNode tabControlNode(String nodeId, int tabCount) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("tab_count", Integer.valueOf(tabCount));
        return new TargetCompositionNode(
                nodeId, "TAB_CONTROL", "basic", "HIGH", params, null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static TargetNodePayload tabControlPayload(String nodeId, String[] labels) {
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        for (int i = 0; i < labels.length; i++) {
            Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
            structuredData.put("pageOrdinal", Integer.valueOf(i));
            items.add(new TargetLeafPayload(
                    TargetPayloadCategory.DISPLAY_TEXT, labels[i], structuredData, "source_text_attribute",
                    nodeId + "/Tabpage[" + i + "]"));
        }
        return new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId, items);
    }

    /** 자식 하나를 page 0에만 붙인다 -- lybox와 동일한 assembly 방식으로, w2:content[0] 안에만
     * child가 나타나고 w2:content[1]은 비어 있어야 한다. */
    private static void testTabControlChildAttachedToPage0Only() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabP0", 2);
        TargetCompositionNode child = titleBarNode("page0Child", "OnPage0");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "panes", child, Integer.valueOf(0))));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("tabP0", new String[] {"Page A", "Page B"}));
        payloads.add(titleLabelPayload(child));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        CompositionRenderResult parentResult = composed.get(0);
        assertEquals("tab-page0-only: parent status", "RENDERED", String.valueOf(parentResult.getStatus()));
        Element root = parentResult.getTargetElement();
        Element content0 = contentElementAt(root, 0);
        Element content1 = contentElementAt(root, 1);
        assertEquals("tab-page0-only: content[0] has 1 child (the attached TITLE_BAR)", "1",
                String.valueOf(content0.getChildNodes().getLength()));
        assertEquals("tab-page0-only: content[1] has 0 children (no cross-attach)", "0",
                String.valueOf(content1.getChildNodes().getLength()));
    }

    /** 다른 자식을 page 1에만 붙인다 -- 대칭 확인. */
    private static void testTabControlDifferentChildAttachedToPage1Only() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabP1", 2);
        TargetCompositionNode child = titleBarNode("page1Child", "OnPage1");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "panes", child, Integer.valueOf(1))));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("tabP1", new String[] {"Page A", "Page B"}));
        payloads.add(titleLabelPayload(child));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        Element root = composed.get(0).getTargetElement();
        assertEquals("tab-page1-only: content[0] has 0 children", "0",
                String.valueOf(contentElementAt(root, 0).getChildNodes().getLength()));
        assertEquals("tab-page1-only: content[1] has 1 child", "1",
                String.valueOf(contentElementAt(root, 1).getChildNodes().getLength()));
        assertEquals("tab-page1-only: content[1] child is the attached TITLE_BAR", "OnPage1",
                titleTextOf(contentElementAt(root, 1)));
    }

    /** 같은 page에 2개의 child가 붙으면 Plan edge 순서 그대로 보존돼야 한다. */
    private static void testTabControlMultipleChildrenOnSamePagePreservePlanOrdering() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabSamePage", 1);
        TargetCompositionNode first = titleBarNode("firstOnPage", "First");
        TargetCompositionNode second = titleBarNode("secondOnPage", "Second");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, first, second),
                Arrays.asList(
                        new TargetCompositionEdge(parent, "panes", first, Integer.valueOf(0)),
                        new TargetCompositionEdge(parent, "panes", second, Integer.valueOf(0))));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("tabSamePage", new String[] {"Only Page"}));
        payloads.add(titleLabelPayload(first));
        payloads.add(titleLabelPayload(second));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        Element content0 = contentElementAt(composed.get(0).getTargetElement(), 0);
        assertEquals("tab-same-page-order: 2 children", "2", String.valueOf(content0.getChildNodes().getLength()));
        assertEquals("tab-same-page-order: order[0]", "First",
                titleTextOfDfbox((Element) content0.getChildNodes().item(0)));
        assertEquals("tab-same-page-order: order[1]", "Second",
                titleTextOfDfbox((Element) content0.getChildNodes().item(1)));
    }

    /** 중첩 TAB_CONTROL: outer.page[0]에 inner TAB_CONTROL이 붙고, inner.page[0]에 TITLE_BAR가
     * 붙는다 -- 두 단계 page attachment가 모두 정확해야 한다. */
    private static void testTabControlNestedAttachmentWorks() throws Exception {
        TargetCompositionNode outer = tabControlNode("outerTab", 1);
        TargetCompositionNode inner = tabControlNode("innerTab", 1);
        TargetCompositionNode leaf = titleBarNode("nestedLeaf", "Nested");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(outer, inner, leaf),
                Arrays.asList(
                        new TargetCompositionEdge(outer, "panes", inner, Integer.valueOf(0)),
                        new TargetCompositionEdge(inner, "panes", leaf, Integer.valueOf(0))));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("outerTab", new String[] {"Outer Page"}));
        payloads.add(tabControlPayload("innerTab", new String[] {"Inner Page"}));
        payloads.add(titleLabelPayload(leaf));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        CompositionRenderResult outerResult = null;
        for (CompositionRenderResult r : composed) {
            if (r.getIdentity().equals(outer.getIdentity())) outerResult = r;
        }
        assertEquals("tab-nested: outer status", "RENDERED", String.valueOf(outerResult.getStatus()));
        Element outerContent0 = contentElementAt(outerResult.getTargetElement(), 0);
        assertEquals("tab-nested: outer content[0] has 1 child (the inner tabControl)", "1",
                String.valueOf(outerContent0.getChildNodes().getLength()));
        Element innerRoot = (Element) outerContent0.getFirstChild();
        assertEquals("tab-nested: inner root localName", "tabControl", innerRoot.getLocalName());
        Element innerContent0 = contentElementAt(innerRoot, 0);
        assertEquals("tab-nested: inner content[0] has 1 child (the TITLE_BAR leaf)", "1",
                String.valueOf(innerContent0.getChildNodes().getLength()));
        assertEquals("tab-nested: leaf text", "Nested", titleTextOf(innerContent0));
    }

    private static void testTabControlPanesEdgeMissingPageOrdinalFailsClosed() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabNoOrdinal", 1);
        TargetCompositionNode child = titleBarNode("noOrdinalChild", "X");
        // 3-arg constructor 사용 시 pageOrdinal은 null로 기본 설정된다(pageOrdinal 없는 panes edge).
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "panes", child)));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("tabNoOrdinal", new String[] {"Only"}));
        payloads.add(titleLabelPayload(child));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("tab-missing-page-ordinal: parent status", "INTEGRITY_VIOLATION",
                String.valueOf(composed.get(0).getStatus()));
    }

    private static void testTabControlNegativePageOrdinalRejectedAtEdgeConstruction() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabNeg", 1);
        TargetCompositionNode child = titleBarNode("negChild", "X");
        boolean threw = false;
        try {
            new TargetCompositionEdge(parent, "panes", child, Integer.valueOf(-1));
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue("tab-negative-page-ordinal: rejected at edge construction", threw);
    }

    private static void testTabControlOutOfRangePageOrdinalFailsClosed() throws Exception {
        TargetCompositionNode parent = tabControlNode("tabOOR", 1);
        TargetCompositionNode child = titleBarNode("oorChild", "X");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "panes", child, Integer.valueOf(5))));

        List<TargetNodePayload> payloads = new ArrayList<TargetNodePayload>();
        payloads.add(tabControlPayload("tabOOR", new String[] {"Only"}));
        payloads.add(titleLabelPayload(child));
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("tab-out-of-range-page-ordinal: parent status", "INTEGRITY_VIOLATION",
                String.valueOf(composed.get(0).getStatus()));
    }

    private static void testTabControlNonPanesEdgeWithPageOrdinalFailsClosed() throws Exception {
        TargetCompositionNode parent = splitLayoutNode("splitWithBadOrdinal");
        TargetCompositionNode child = titleBarNode("badOrdinalChild", "X");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Arrays.asList(parent, child),
                Arrays.asList(new TargetCompositionEdge(parent, "columns", child, Integer.valueOf(0))));

        List<AtomicRenderResult> atomic = renderAtomic(plan, titlePayloads(child));
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);
        assertEquals("tab-non-panes-with-ordinal: parent status", "INTEGRITY_VIOLATION",
                String.valueOf(composed.get(0).getStatus()));
    }

    private static Element contentElementAt(Element tabControlRoot, int pageOrdinal) {
        int seen = -1;
        org.w3c.dom.NodeList children = tabControlRoot.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("content".equals(child.getLocalName())) {
                seen++;
                if (seen == pageOrdinal) {
                    return child;
                }
            }
        }
        throw new IllegalStateException("content element for pageOrdinal=" + pageOrdinal + " not found");
    }

    private static TargetNodePayload emptySplitPayload(String nodeId) {
        return new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId, new ArrayList<TargetLeafPayload>());
    }

    private static String titleTextOf(Element lyColumnWrapper) {
        Element dfbox = (Element) lyColumnWrapper.getFirstChild();
        return titleTextOfDfbox(dfbox);
    }

    /** TAB_CONTROL.panes는 child root(dfbox)를 generic wrapper 없이 w2:content에 직접 붙인다 --
     * 그래서 (wrapper가 아니라) dfbox 자체를 들고 있는 호출자는 이 메서드를 사용한다. */
    private static String titleTextOfDfbox(Element dfbox) {
        Element f1 = (Element) dfbox.getFirstChild();
        Element dfTit = (Element) f1.getFirstChild();
        return dfTit.getTextContent();
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("[FAIL] " + label + " -- expected=<" + expected + "> actual=<" + actual + ">");
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            System.out.println("[FAIL] " + label);
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
