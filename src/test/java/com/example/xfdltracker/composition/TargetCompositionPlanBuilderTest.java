package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;
import com.example.xfdltracker.semantic.TabPageMembership;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link TargetCompositionPlanBuilder}가 validated {@link SlotAssignment}만으로 edge를 만드는지,
 * root/standalone을 임의로 합치지 않는지, LOW/fallback decision이 canonical children을 갖지
 * 않는지, duplicate/cycle이 안전하게 처리되는지를 검증하는 오프라인 unit test.
 */
public class TargetCompositionPlanBuilderTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testStandaloneNodePreserved();
        testApprovedCompositionProducesOneNodeOneEdge();
        testCandidateOnlyProducesZeroEdges();
        testAmbiguityOnlyApprovedAssignmentBecomesEdge();
        testLowFallbackPreservesNodeNoCanonicalEdge();
        testPagingTargetInvariantEdgeCreatedSourceEmissionZero();
        testSelfNestingCycleExplicitlyRejected();

        // ---- Plan Input-Integrity 최종 강화 ----
        testStandaloneInvalidVariantDecisionFailsBuild();
        testStandaloneInvalidParameterDecisionFailsBuild();
        testInvalidVariantAlongsideValidDecisionFailsWholeBuild();
        testDuplicateSlotAssignmentFailsBuild();

        // ---- Plan Identity Determinism 최종 강화 ----
        testTargetSyntheticIdOrderIndependence();
        testMixedSourceAndSyntheticOrderIndependence();
        testTargetSyntheticIdCollisionRejected();
        testDifferentParentSameSlotSameChildNotFalsePositiveDuplicate();

        // ---- SPLIT_LAYOUT.columns Plan Ordering (explicit upstream assignment-rank 기반) ----
        testSplitColumnPlanOrderMatchesGeometryRegardlessOfAssignmentOrder();
        testSplitColumnPlanOrderStableAcrossDifferentSourceArrangements();
        testUnapprovedSplitColumnCandidateNotMaterialized();
        testInvalidZeroWidthSplitGeometryNeverBecomesDecision();
        testOverlapSplitGeometryNeverBecomesDecision();
        testNonSplitFamilySlotOrderPreservedUnchanged();
        testMissingRankEvidenceFailsClosed();
        testCompetingRanksOnSameChildFailsClosed();
        testDuplicateRankAmongDistinctChildrenFailsClosed();
        testMisleadingStructuralIdPrefixCausesNoCrossWire();

        // ==== TAB_CONTROL 정확한 page membership Plan validation ====
        testTabControlPanesValidMembershipCreatesEdgePageOrdinal();
        testTabControlPanesMissingMembershipFailsClosed();
        testTabControlPanesParentMismatchFailsClosed();
        testTabControlPanesPageOrdinalPreservedUnchanged();
        testNonTabControlEdgeHasNullPageOrdinal();
        testTabControlPanesSevenFamilyChildPermissionExactNoWildcard();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** assignment 없는 valid GRID 하나 -- plan node 1개, root/standalone으로 보존된다. */
    private static void testStandaloneNodePreserved() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "standaloneGrid15");
        form.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        assertEquals("standalone: precondition -- exactly 1 decision", "1", String.valueOf(decisions.size()));
        assertTrue("standalone: precondition -- GRID eligible", decisions.get(0).isEligible());

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        assertEquals("standalone: plan has exactly 1 node", "1", String.valueOf(plan.getNodes().size()));
        assertEquals("standalone: plan has 0 edges", "0", String.valueOf(plan.getEdges().size()));
        assertEquals("standalone: the node is a root/standalone node", "1",
                String.valueOf(plan.getRootNodes().size()));
        assertEquals("standalone: root node family is GRID", "GRID", plan.getRootNodes().get(0).getFamily());
    }

    /** 실제 assignSlot 성공 상태(SPLIT_LAYOUT.columns <- GRID) -- node 2개, edge 정확히 1개. */
    private static void testApprovedCompositionProducesOneNodeOneEdge() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);
        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        CompositionDecision child = fx.decisionByStructuralId.get(candidate.getChildStructuralId());
        assertTrue("approved-composition: precondition -- assignSlot succeeds",
                new CompositionEvaluator().assignSlot(parent, candidate.getSlot(), child));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        assertEquals("approved-composition: plan has exactly 2 nodes (parent+child)", "2",
                String.valueOf(plan.getNodes().size()));
        assertEquals("approved-composition: plan has exactly 1 edge", "1", String.valueOf(plan.getEdges().size()));
        TargetCompositionEdge edge = plan.getEdges().get(0);
        assertEquals("approved-composition: edge slot is columns", "columns", edge.getSlot());
        assertEquals("approved-composition: edge parent family is SPLIT_LAYOUT", "SPLIT_LAYOUT",
                edge.getParent().getFamily());
        assertEquals("approved-composition: edge child family is GRID", "GRID", edge.getChild().getFamily());
        assertEquals("approved-composition: only the parent is a root (child is not standalone)", "1",
                String.valueOf(plan.getRootNodes().size()));
    }

    /** candidate만 존재하고 승인/apply 안 됨 -- slotAssignments=0 -- plan edge=0. */
    private static void testCandidateOnlyProducesZeroEdges() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        assertTrue("candidate-only: precondition -- at least 1 candidate exists", !fx.candidates.isEmpty());
        for (CompositionDecision decision : fx.decisions) {
            assertEquals("candidate-only: precondition -- slotAssignments 0 (" + decision.getFamily() + ")", "0",
                    String.valueOf(decision.getSlotAssignments().size()));
        }

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        assertEquals("candidate-only: plan has 0 edges", "0", String.valueOf(plan.getEdges().size()));
        assertEquals("candidate-only: plan has 2 standalone root nodes (SPLIT_LAYOUT, GRID)", "2",
                String.valueOf(plan.getRootNodes().size()));
    }

    /** ambiguous 2 candidates 중 하나만 approve+apply -- plan edge에는 승인된 것만 등장. */
    private static void testAmbiguityOnlyApprovedAssignmentBecomesEdge() throws Exception {
        AmbiguityFixture fx = buildAmbiguityFixture();
        SlotAssignmentCandidate chosen = findCandidate(fx.candidates, "SPLIT_LAYOUT", "BUTTON_GROUP", "columns");
        SlotAssignmentCandidate other = findCandidate(fx.candidates, "BUSINESS_TABLE", "BUTTON_GROUP", "td_content");
        assertTrue("ambiguity: precondition -- both candidates found", chosen != null && other != null);

        CompositionDecision chosenParent = fx.decisionByStructuralId.get(chosen.getParentStructuralId());
        CompositionDecision chosenChild = fx.decisionByStructuralId.get(chosen.getChildStructuralId());
        assertTrue("ambiguity: precondition -- assignSlot succeeds for the chosen candidate only",
                new CompositionEvaluator().assignSlot(chosenParent, chosen.getSlot(), chosenChild));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        assertEquals("ambiguity: plan has exactly 1 edge (only the approved+applied candidate)", "1",
                String.valueOf(plan.getEdges().size()));
        assertEquals("ambiguity: the edge's slot is columns (the chosen candidate's slot)", "columns",
                plan.getEdges().get(0).getSlot());
        assertEquals("ambiguity: the edge's parent family is SPLIT_LAYOUT (the chosen candidate's parent)",
                "SPLIT_LAYOUT", plan.getEdges().get(0).getParent().getFamily());
    }

    /** LOW + FIXED_WIDTH_FALLBACK SPLIT_LAYOUT -- node는 보존, canonical columns edge는 0. */
    private static void testLowFallbackPreservesNodeNoCanonicalEdge() throws Exception {
        CompositionDecision lowFallbackParent = new CompositionDecision();
        lowFallbackParent.setFamily("SPLIT_LAYOUT");
        lowFallbackParent.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        lowFallbackParent.setSourceStructuralId("Form[0]/Div[0]");
        lowFallbackParent.setConfidence("LOW");
        lowFallbackParent.setFallback("FIXED_WIDTH_FALLBACK");
        lowFallbackParent.setEligible(true);

        CompositionDecision gridChild = new CompositionDecision();
        gridChild.setFamily("GRID");
        gridChild.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        gridChild.setSourceStructuralId("Form[0]/Div[0]/Grid[0]");
        gridChild.setEligible(true);

        boolean assigned = new CompositionEvaluator().assignSlot(lowFallbackParent, "columns", gridChild);
        assertTrue("low-fallback: precondition -- assignSlot itself already refuses a LOW+fallback parent",
                !assigned);
        assertEquals("low-fallback: precondition -- slotAssignments still 0", "0",
                String.valueOf(lowFallbackParent.getSlotAssignments().size()));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(lowFallbackParent);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        assertEquals("low-fallback: node preserved (1)", "1", String.valueOf(plan.getNodes().size()));
        assertEquals("low-fallback: fallback value preserved", "FIXED_WIDTH_FALLBACK",
                plan.getNodes().get(0).getFallback());
        assertEquals("low-fallback: variant not promoted to canonical (still null)", null,
                plan.getNodes().get(0).getVariant());
        assertEquals("low-fallback: canonical columns edge count = 0", "0", String.valueOf(plan.getEdges().size()));
    }

    /** GRID.paging <- TARGET_SYNTHETIC PAGING -- edge 생성 가능, source PAGING emission은 여전히 0. */
    private static void testPagingTargetInvariantEdgeCreatedSourceEmissionZero() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "pagingParentGrid15");
        form.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult r : results) {
            assertTrue("paging-target-invariant: source emission never PAGING", !"PAGING".equals(r.getSemanticType()));
        }

        List<CompositionDecision> decisions = evaluateAll(results);
        CompositionDecision gridDecision = decisions.get(0);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        String targetSyntheticId = gridDecision.getSourceStructuralId() + "#paging";
        CompositionDecision pagingDecision =
                evaluator.createTargetSyntheticDecision("PAGING", null, targetSyntheticId);
        assertTrue("paging-target-invariant: precondition -- TARGET_SYNTHETIC PAGING decision eligible",
                pagingDecision.isEligible());
        assertTrue("paging-target-invariant: precondition -- assignSlot succeeds",
                evaluator.assignSlot(gridDecision, "paging", pagingDecision));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        assertEquals("paging-target-invariant: plan has 2 nodes (GRID + TARGET_SYNTHETIC PAGING)", "2",
                String.valueOf(plan.getNodes().size()));
        assertEquals("paging-target-invariant: plan has exactly 1 edge", "1", String.valueOf(plan.getEdges().size()));
        TargetCompositionEdge edge = plan.getEdges().get(0);
        assertEquals("paging-target-invariant: edge slot is paging", "paging", edge.getSlot());
        assertEquals("paging-target-invariant: edge child family is PAGING", "PAGING", edge.getChild().getFamily());
        assertTrue("paging-target-invariant: edge child origin is TARGET_SYNTHETIC",
                edge.getChild().getOrigin() == CompositionDecision.Origin.TARGET_SYNTHETIC);
        assertEquals("paging-target-invariant: edge child sourceStructuralId is null (no source anchor)", null,
                edge.getChild().getSourceStructuralId());
        assertEquals("paging-target-invariant: edge child nodeId is derived from the explicit targetSyntheticId",
                "target_synthetic:" + targetSyntheticId, edge.getChild().getNodeId());
    }

    /** invalid variant를 가진 standalone root-level 후보 decision -- build() 자체가 명시적으로 실패한다. */
    private static void testStandaloneInvalidVariantDecisionFailsBuild() throws Exception {
        CompositionDecision tampered = new CompositionDecision();
        tampered.setFamily("GRID");
        tampered.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tampered.setSourceStructuralId("Form[0]/Grid[0]");
        tampered.setVariant("nonexistent_variant");
        tampered.setEligible(true);

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(tampered);
        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("standalone-invalid-variant: exception message mentions invalid_plan_input",
                    expected.getMessage().contains("invalid_plan_input"));
        }
        assertTrue("standalone-invalid-variant: build() throws for a standalone invalid-variant decision "
                + "not connected to any assignment (no silent skip)", threw);
    }

    /** invalid parameter key를 가진 standalone root-level 후보 decision -- build() 자체가 명시적으로 실패한다. */
    private static void testStandaloneInvalidParameterDecisionFailsBuild() throws Exception {
        CompositionDecision tampered = new CompositionDecision();
        tampered.setFamily("GRID");
        tampered.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tampered.setSourceStructuralId("Form[0]/Grid[0]");
        tampered.setEligible(true);
        tampered.getParameters().put("nonexistent_parameter_key", "irrelevant_value");

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(tampered);
        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("standalone-invalid-parameter: exception message mentions invalid_plan_input",
                    expected.getMessage().contains("invalid_plan_input"));
        }
        assertTrue("standalone-invalid-parameter: build() throws for a standalone invalid-parameter decision "
                + "not connected to any assignment (no silent skip)", threw);
    }

    /** valid GRID + 별개의 standalone invalid TITLE_BAR 후보를 함께 넘기면, valid decision이
     * 있어도 전체 build()가 실패해야 한다(부분 plan 반환 금지). */
    private static void testInvalidVariantAlongsideValidDecisionFailsWholeBuild() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "validGrid15b");
        form.appendChild(grid);
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        assertTrue("invalid-alongside-valid: precondition -- the real GRID decision is eligible",
                decisions.get(0).isEligible());

        CompositionDecision invalidTitleBar = new CompositionDecision();
        invalidTitleBar.setFamily("TITLE_BAR");
        invalidTitleBar.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        invalidTitleBar.setSourceStructuralId("Form[0]/TitleBar[0]");
        invalidTitleBar.setVariant("nonexistent_variant");
        invalidTitleBar.setEligible(true);
        decisions.add(invalidTitleBar);

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue("invalid-alongside-valid: build() fails even though one decision in the list is valid "
                + "(no silent partial plan)", threw);
    }

    /** 같은 (parent, slot, child)가 assignSlot 두 번 성공으로 중복 생성되면, build()가 명시적으로
     * 실패한다(조용한 중복 제거 금지 -- Plan Builder는 upstream corrupted state를 정규화하지 않음). */
    private static void testDuplicateSlotAssignmentFailsBuild() throws Exception {
        CompositionDecision outerTab = new CompositionDecision();
        outerTab.setFamily("TAB_CONTROL");
        outerTab.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        outerTab.setSourceStructuralId("Form[0]/Tab[0]");
        outerTab.setEligible(true);

        CompositionDecision innerTab = new CompositionDecision();
        innerTab.setFamily("TAB_CONTROL");
        innerTab.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        innerTab.setSourceStructuralId("Form[0]/Tab[0]/Tabpage[0]/Tab[0]");
        innerTab.setEligible(true);
        // duplicate-edge 체크 이전에 exact membership이 먼저 요구된다.
        innerTab.setTabPageMembership(new TabPageMembership("Form[0]/Tab[0]", 0));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("duplicate-assignment: precondition -- first assignSlot succeeds",
                evaluator.assignSlot(outerTab, "panes", innerTab));
        assertTrue("duplicate-assignment: precondition -- second identical assignSlot ALSO succeeds "
                + "(no cardinality max, no built-in dedup at assignSlot level)",
                evaluator.assignSlot(outerTab, "panes", innerTab));
        assertEquals("duplicate-assignment: precondition -- raw slotAssignments list really has 2 entries",
                "2", String.valueOf(outerTab.getSlotAssignments().size()));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(outerTab);
        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("duplicate-assignment: exception message mentions duplicate_slot_assignment",
                    expected.getMessage().contains("duplicate_slot_assignment"));
        }
        assertTrue("duplicate-assignment: build() throws IllegalStateException for the duplicate "
                + "(parent, slot, child) triple -- no silent dedup", threw);
    }

    /** TAB_CONTROL A.panes -> B, B.panes -> A -- catalog가 실제로 허용하는 self-nesting 규칙으로
     * 구성 가능한 cycle. 명시적으로 거부되어야 한다(조용한 재귀 plan 생성 금지). */
    private static void testSelfNestingCycleExplicitlyRejected() throws Exception {
        CompositionDecision tabA = new CompositionDecision();
        tabA.setFamily("TAB_CONTROL");
        tabA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tabA.setSourceStructuralId("Form[0]/Tab[0]");
        tabA.setEligible(true);

        CompositionDecision tabB = new CompositionDecision();
        tabB.setFamily("TAB_CONTROL");
        tabB.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tabB.setSourceStructuralId("Form[0]/Tab[0]/Tabpage[0]/Tab[0]");
        tabB.setEligible(true);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("cycle: precondition -- A.panes <- B assignSlot succeeds (catalog allows self-nesting)",
                evaluator.assignSlot(tabA, "panes", tabB));
        assertTrue("cycle: precondition -- B.panes <- A assignSlot ALSO succeeds (assignSlot has no cycle check)",
                evaluator.assignSlot(tabB, "panes", tabA));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(tabA);
        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("cycle: exception message mentions cycle_detected",
                    expected.getMessage().contains("cycle_detected"));
        }
        assertTrue("cycle: build() explicitly rejects the cycle (IllegalStateException), no silent recursive plan",
                threw);
    }

    /**
     * 서로 독립된 두 GRID(A, C)가 각각 TARGET_SYNTHETIC PAGING 자식을 갖는다. root 목록을
     * [A,C]/[C,A] 순서로 각각 build()해도 Grid A의 PAGING 자식 nodeId가 동일해야 한다
     * (traversal encounter order에 의존하지 않음).
     */
    private static void testTargetSyntheticIdOrderIndependence() throws Exception {
        CompositionDecision gridA = new CompositionDecision();
        gridA.setFamily("GRID");
        gridA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        gridA.setSourceStructuralId("Form[0]/Grid[0]");
        gridA.setEligible(true);

        CompositionDecision gridC = new CompositionDecision();
        gridC.setFamily("GRID");
        gridC.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        gridC.setSourceStructuralId("Form[0]/Grid[1]");
        gridC.setEligible(true);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision pagingA =
                evaluator.createTargetSyntheticDecision("PAGING", null, "Form[0]/Grid[0]#paging");
        CompositionDecision pagingC =
                evaluator.createTargetSyntheticDecision("PAGING", null, "Form[0]/Grid[1]#paging");
        assertTrue("order-independence: precondition -- assignSlot A succeeds",
                evaluator.assignSlot(gridA, "paging", pagingA));
        assertTrue("order-independence: precondition -- assignSlot C succeeds",
                evaluator.assignSlot(gridC, "paging", pagingC));

        List<CompositionDecision> orderAC = new ArrayList<CompositionDecision>();
        orderAC.add(gridA);
        orderAC.add(gridC);
        TargetCompositionPlan planAC = new TargetCompositionPlanBuilder().build(orderAC);

        List<CompositionDecision> orderCA = new ArrayList<CompositionDecision>();
        orderCA.add(gridC);
        orderCA.add(gridA);
        TargetCompositionPlan planCA = new TargetCompositionPlanBuilder().build(orderCA);

        String pagingIdUnderGridA_AC = childNodeIdForParent(planAC, "Form[0]/Grid[0]");
        String pagingIdUnderGridA_CA = childNodeIdForParent(planCA, "Form[0]/Grid[0]");
        assertEquals("order-independence: Grid[0]'s PAGING child nodeId is identical regardless of "
                + "root list order", pagingIdUnderGridA_AC, pagingIdUnderGridA_CA);
        String pagingIdUnderGridC_AC = childNodeIdForParent(planAC, "Form[0]/Grid[1]");
        String pagingIdUnderGridC_CA = childNodeIdForParent(planCA, "Form[0]/Grid[1]");
        assertEquals("order-independence: Grid[1]'s PAGING child nodeId is identical regardless of "
                + "root list order", pagingIdUnderGridC_AC, pagingIdUnderGridC_CA);
        assertTrue("order-independence: the two different GRIDs' PAGING children still get distinct ids",
                !pagingIdUnderGridA_AC.equals(pagingIdUnderGridC_AC));
    }

    /** SOURCE_SEMANTIC과 TARGET_SYNTHETIC이 섞인 혼합 fixture를 [A,B,C]/[C,B,A] 두 순서로
     * build()해, node/edge/root identity 집합이 완전히 동일한지 확인한다. */
    private static void testMixedSourceAndSyntheticOrderIndependence() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);
        CompositionDecision splitParent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        CompositionDecision gridChild = fx.decisionByStructuralId.get(candidate.getChildStructuralId());
        assertTrue("mixed-order-independence: precondition -- assignSlot(columns) succeeds",
                new CompositionEvaluator().assignSlot(splitParent, candidate.getSlot(), gridChild));

        CompositionDecision standaloneGridWithPaging = new CompositionDecision();
        standaloneGridWithPaging.setFamily("GRID");
        standaloneGridWithPaging.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        standaloneGridWithPaging.setSourceStructuralId("Form[0]/Grid[9]");
        standaloneGridWithPaging.setEligible(true);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision pagingChild =
                evaluator.createTargetSyntheticDecision("PAGING", null, "Form[0]/Grid[9]#paging");
        assertTrue("mixed-order-independence: precondition -- assignSlot(paging) succeeds",
                evaluator.assignSlot(standaloneGridWithPaging, "paging", pagingChild));

        CompositionDecision standaloneTitleBar = new CompositionDecision();
        standaloneTitleBar.setFamily("TITLE_BAR");
        standaloneTitleBar.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        standaloneTitleBar.setSourceStructuralId("Form[0]/TitleBar[9]");
        standaloneTitleBar.setEligible(true);

        List<CompositionDecision> orderABC = new ArrayList<CompositionDecision>();
        orderABC.add(splitParent);
        orderABC.add(standaloneGridWithPaging);
        orderABC.add(standaloneTitleBar);
        TargetCompositionPlan planABC = new TargetCompositionPlanBuilder().build(orderABC);

        List<CompositionDecision> orderCBA = new ArrayList<CompositionDecision>();
        orderCBA.add(standaloneTitleBar);
        orderCBA.add(standaloneGridWithPaging);
        orderCBA.add(splitParent);
        TargetCompositionPlan planCBA = new TargetCompositionPlanBuilder().build(orderCBA);

        assertEquals("mixed-order-independence: node identity set is identical", nodeIdSet(planABC).toString(),
                nodeIdSet(planCBA).toString());
        assertEquals("mixed-order-independence: edge (parentId,slot,childId) set is identical",
                edgeKeySet(planABC).toString(), edgeKeySet(planCBA).toString());
        assertEquals("mixed-order-independence: root identity set is identical", rootIdSet(planABC).toString(),
                rootIdSet(planCBA).toString());
        assertEquals("mixed-order-independence: precondition -- 5 nodes total (SPLIT_LAYOUT, nested GRID, "
                + "standalone GRID, its PAGING child, standalone TITLE_BAR)", "5",
                String.valueOf(planABC.getNodes().size()));
    }

    /** 서로 다른 두 CompositionDecision 객체가 같은 targetSyntheticId를 주장하면 build()가
     * 명시적으로 실패한다(duplicate_source_semantic_anchor와 동일한 정책). */
    private static void testTargetSyntheticIdCollisionRejected() throws Exception {
        CompositionDecision gridA = new CompositionDecision();
        gridA.setFamily("GRID");
        gridA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        gridA.setSourceStructuralId("Form[0]/Grid[20]");
        gridA.setEligible(true);

        CompositionDecision gridB = new CompositionDecision();
        gridB.setFamily("GRID");
        gridB.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        gridB.setSourceStructuralId("Form[0]/Grid[21]");
        gridB.setEligible(true);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        String collidingId = "duplicated_id_by_mistake";
        CompositionDecision pagingA = evaluator.createTargetSyntheticDecision("PAGING", null, collidingId);
        CompositionDecision pagingB = evaluator.createTargetSyntheticDecision("PAGING", null, collidingId);
        assertTrue("synthetic-id-collision: precondition -- assignSlot A succeeds",
                evaluator.assignSlot(gridA, "paging", pagingA));
        assertTrue("synthetic-id-collision: precondition -- assignSlot B succeeds",
                evaluator.assignSlot(gridB, "paging", pagingB));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(gridA);
        decisions.add(gridB);
        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("synthetic-id-collision: exception message mentions duplicate_target_synthetic_id",
                    expected.getMessage().contains("duplicate_target_synthetic_id"));
        }
        assertTrue("synthetic-id-collision: build() explicitly rejects two different decision instances "
                + "claiming the same targetSyntheticId", threw);
    }

    /** 서로 다른 parent A.columns/B.columns가 같은 child X를 가리키는 것은 duplicate가 아니다 --
     * 두 edge 모두 정상 생성되어야 한다(오판 금지). */
    private static void testDifferentParentSameSlotSameChildNotFalsePositiveDuplicate() throws Exception {
        CompositionDecision splitA = new CompositionDecision();
        splitA.setFamily("SPLIT_LAYOUT");
        splitA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        splitA.setSourceStructuralId("Form[0]/Div[30]");
        splitA.setEligible(true);

        CompositionDecision splitB = new CompositionDecision();
        splitB.setFamily("SPLIT_LAYOUT");
        splitB.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        splitB.setSourceStructuralId("Form[0]/Div[31]");
        splitB.setEligible(true);

        CompositionDecision sharedGridChild = new CompositionDecision();
        sharedGridChild.setFamily("GRID");
        sharedGridChild.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        sharedGridChild.setSourceStructuralId("Form[0]/Div[30]/Grid[0]");
        sharedGridChild.setEligible(true);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("different-parent-same-child: precondition -- A.columns <- shared child succeeds",
                evaluator.assignSlot(splitA, "columns", sharedGridChild));
        assertTrue("different-parent-same-child: precondition -- B.columns <- SAME shared child ALSO succeeds "
                + "(assignSlot does not forbid a child from having multiple parents)",
                evaluator.assignSlot(splitB, "columns", sharedGridChild));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(splitA);
        decisions.add(splitB);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        assertEquals("different-parent-same-child: plan has exactly 3 nodes (splitA, splitB, shared GRID -- "
                + "not duplicated)", "3", String.valueOf(plan.getNodes().size()));
        assertEquals("different-parent-same-child: plan has exactly 2 edges (one per parent, NOT falsely "
                + "collapsed/rejected as a duplicate)", "2", String.valueOf(plan.getEdges().size()));
    }

    // ---- SPLIT_LAYOUT.columns Plan Ordering: explicit upstream assignment-rank로만 결정,
    // sourceStructuralId prefix/ancestor 추론은 사용하지 않는다. ----

    /**
     * 3-column SPLIT_LAYOUT을 approval order/DOM order/ratio-size order와 모두 다르게 구성해,
     * 최종 Plan의 columns edge 순서가 오직 explicit rank evidence(geometry)로만 결정됨을 확인한다.
     */
    private static void testSplitColumnPlanOrderMatchesGeometryRegardlessOfAssignmentOrder() throws Exception {
        SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 2, 1}); // DOM 순서: w300,w100,w600
        approveAndApplyColumns(fx, fx.childW600, fx.childW300, fx.childW100);

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        List<String> order = columnsChildStructuralIdOrder(plan, fx.splitParent.getSourceStructuralId());
        assertEquals("plan-order: exactly 3 columns edges", "3", String.valueOf(order.size()));
        assertEquals("plan-order: geometry left-ascending order (w300@left0, w600@left300, w100@left900) "
                + "wins over approval order / DOM source order / ratio-size order",
                fx.gridW300Id + "," + fx.gridW600Id + "," + fx.gridW100Id, joinList(order));
    }

    /** 동일 geometry를 서로 다른 DOM append order 2가지로 구성해도 Plan columns order가 항상
     * 동일한 논리적 geometry order(w300,w600,w100)임을 확인한다. */
    private static void testSplitColumnPlanOrderStableAcrossDifferentSourceArrangements() throws Exception {
        {
            SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 1, 2}); // DOM 순서: w300,w600,w100
            approveAndApplyColumns(fx, fx.childW300, fx.childW600, fx.childW100);
            TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
            List<String> order = columnsChildStructuralIdOrder(plan, fx.splitParent.getSourceStructuralId());
            assertEquals("source-arrangement-stability(DOM order w300,w600,w100): normalized geometry order",
                    "w300,w600,w100", normalizeColumnOrder(fx, order));
        }
        {
            SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {2, 1, 0}); // DOM 순서: w100,w600,w300
            approveAndApplyColumns(fx, fx.childW100, fx.childW600, fx.childW300);
            TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
            List<String> order = columnsChildStructuralIdOrder(plan, fx.splitParent.getSourceStructuralId());
            assertEquals("source-arrangement-stability(DOM order w100,w600,w300): normalized geometry order "
                    + "identical to the other DOM arrangement above -- geometry order is DOM-arrangement "
                    + "independent", "w300,w600,w100", normalizeColumnOrder(fx, order));
        }
    }

    /** fixture-specific raw structuralId 목록을 known width label(w300/w600/w100)로 정규화한다. */
    private static String normalizeColumnOrder(SplitFixture3 fx, List<String> structuralIdOrder) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < structuralIdOrder.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String id = structuralIdOrder.get(i);
            if (id.equals(fx.gridW300Id)) {
                sb.append("w300");
            } else if (id.equals(fx.gridW600Id)) {
                sb.append("w600");
            } else if (id.equals(fx.gridW100Id)) {
                sb.append("w100");
            } else {
                sb.append("UNKNOWN(" + id + ")");
            }
        }
        return sb.toString();
    }

    /** 3개 candidate 중 2개만 approve+apply하면, Plan columns edge는 그 2개만이고(순서는 여전히
     * geometry), 승인하지 않은 나머지 하나는 Plan child로 승격되지 않는다. */
    private static void testUnapprovedSplitColumnCandidateNotMaterialized() throws Exception {
        SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 1, 2});
        approveAndApplyColumns(fx, fx.childW300, fx.childW100);
        // w600은 의도적으로 approve하지 않는다(candidate만 존재).

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        List<String> order = columnsChildStructuralIdOrder(plan, fx.splitParent.getSourceStructuralId());
        assertEquals("unapproved-excluded: only the 2 approved columns become edges", "2",
                String.valueOf(order.size()));
        assertEquals("unapproved-excluded: approved subset still in geometry order (w300@left0, w100@left900)",
                fx.gridW300Id + "," + fx.gridW100Id, joinList(order));
        assertTrue("unapproved-excluded: the unapproved w600 GRID node is still a standalone root "
                        + "(not silently promoted into the SPLIT_LAYOUT via ordering)",
                rootIdSet(plan).contains(fx.childW600.getSourceStructuralId()));
    }

    /** assignSlot의 legacy 3-arg overload(rank=null)로 approve를 우회 시뮬레이션하면(즉 explicit
     * rank evidence가 전혀 없는 상태), 2개 이상의 columns 중 하나라도 rank가 없으면 build()가
     * fail-closed해야 한다. */
    private static void testMissingRankEvidenceFailsClosed() throws Exception {
        SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 1, 2});
        CompositionEvaluator evaluator = new CompositionEvaluator();
        // 3-arg legacy overload는 rank를 항상 null로 둔다(explicit evidence 없음을 시뮬레이션).
        assertTrue("missing-rank: precondition -- assignSlot(w300, no rank) succeeds",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW300));
        assertTrue("missing-rank: precondition -- assignSlot(w600, no rank) succeeds",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW600));

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(fx.decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("missing-rank: exception mentions split_layout_columns_order_unresolved",
                    expected.getMessage().contains("split_layout_columns_order_unresolved"));
        }
        assertTrue("missing-rank: build() fails closed instead of falling back to encounter order", threw);
    }

    /** 동일 child에 대해 서로 다른 두 SlotAssignment(서로 다른 explicit rank)가 존재하면(예:
     * 조작/재시도로 인한 competing evidence), build()가 fail-closed해야 한다(어느 쪽이든 조용히
     * 하나를 고르지 않는다). */
    private static void testCompetingRanksOnSameChildFailsClosed() throws Exception {
        SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 1, 2});
        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("competing-ranks: precondition -- first assignSlot(w300, rank=0) succeeds",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW300, Integer.valueOf(0)));
        assertTrue("competing-ranks: precondition -- second assignSlot(w300, rank=1) ALSO succeeds "
                + "(assignSlot itself has no built-in dedup)",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW300, Integer.valueOf(1)));
        assertTrue("competing-ranks: precondition -- assignSlot(w600, rank=2) succeeds",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW600, Integer.valueOf(2)));

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(fx.decisions);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue("competing-ranks: build() fails closed (no silent pick of either competing rank)", threw);
    }

    /** 서로 다른 두 child가 같은 rank를 명시적으로 주장하면(예: 동일 rank evidence를 잘못 부여받은
     * 경우), build()가 fail-closed해야 한다. */
    private static void testDuplicateRankAmongDistinctChildrenFailsClosed() throws Exception {
        SplitFixture3 fx = buildThreeColumnSplitFixture(new int[] {0, 1, 2});
        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("duplicate-rank: precondition -- assignSlot(w300, rank=0) succeeds",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW300, Integer.valueOf(0)));
        assertTrue("duplicate-rank: precondition -- assignSlot(w600, rank=0) ALSO succeeds (distinct child, "
                + "same rank value -- deliberately conflicting evidence)",
                evaluator.assignSlot(fx.splitParent, "columns", fx.childW600, Integer.valueOf(0)));

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(fx.decisions);
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("duplicate-rank: exception mentions split_layout_columns_order_ambiguous",
                    expected.getMessage().contains("split_layout_columns_order_ambiguous"));
        }
        assertTrue("duplicate-rank: build() fails closed instead of guessing a total order", threw);
    }

    /**
     * 한 child의 sourceStructuralId가 다른 child의 id를 문자열로 확장한 "misleading prefix"
     * 관계여도, explicit rank evidence를 문자열 관계와 정반대로 부여하면 결과는 rank를 따라야
     * 한다 -- prefix/ancestor 문자열 추론이 남아있지 않음을 증명한다.
     */
    private static void testMisleadingStructuralIdPrefixCausesNoCrossWire() throws Exception {
        CompositionDecision splitParent = new CompositionDecision();
        splitParent.setFamily("SPLIT_LAYOUT");
        splitParent.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        splitParent.setSourceStructuralId("Form[0]/Div[96]");
        splitParent.setVariant("ratio_split");
        splitParent.setEligible(true);

        // childA의 structuralId가 childB의 structuralId를 문자열로 포함하는 misleading prefix 관계.
        CompositionDecision childB = new CompositionDecision();
        childB.setFamily("GRID");
        childB.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        childB.setSourceStructuralId("Form[0]/Div[96]/Grid[0]");
        childB.setEligible(true);

        CompositionDecision childA = new CompositionDecision();
        childA.setFamily("GRID");
        childA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        childA.setSourceStructuralId("Form[0]/Div[96]/Grid[0]/Fake[0]/Grid[0]");
        childA.setEligible(true);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        // explicit rank는 문자열 관계와 반대로 childA=0(먼저), childB=1(나중)로 부여한다.
        assertTrue("misleading-prefix: precondition -- assignSlot(childA, rank=0) succeeds",
                evaluator.assignSlot(splitParent, "columns", childA, Integer.valueOf(0)));
        assertTrue("misleading-prefix: precondition -- assignSlot(childB, rank=1) succeeds",
                evaluator.assignSlot(splitParent, "columns", childB, Integer.valueOf(1)));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(splitParent);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<String> order = columnsChildStructuralIdOrder(plan, splitParent.getSourceStructuralId());
        assertEquals("misleading-prefix: explicit rank order wins (childA then childB), NOT the "
                + "string-prefix relation (which would have suggested the opposite/nested reading)",
                childA.getSourceStructuralId() + "," + childB.getSourceStructuralId(), joinList(order));
    }

    /** zero-width column을 가진 3-sibling container는 Slice 95 guard에 의해 애초에
     * GEOMETRY_INVALID이므로 SPLIT_LAYOUT SemanticRegionResult/decision 자체가 생성되지
     * 않는다 -- Plan ordering 문제가 될 candidate 자체가 없음을 확인한다. */
    private static void testInvalidZeroWidthSplitGeometryNeverBecomesDecision() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitRoot = newDiv(doc, "t96ZeroWidthSplitRoot");
        Element col1 = newDivWithGeometry(doc, "t96ZwCol1", 0, 0, 0, 200); // 폭(width)이 0이다.
        Element col2 = newDivWithGeometry(doc, "t96ZwCol2", 0, 0, 700, 200);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult r : results) {
            assertTrue("zero-width-no-split-decision: no SPLIT_LAYOUT result is ever produced",
                    !"SPLIT_LAYOUT".equals(r.getSemanticType()));
        }
    }

    /** overlap하는 column geometry도 동일하게 SPLIT_LAYOUT candidate 자체를 만들지 않는다. */
    private static void testOverlapSplitGeometryNeverBecomesDecision() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitRoot = newDiv(doc, "t96OverlapSplitRoot");
        Element col1 = newDivWithGeometry(doc, "t96OvCol1", 0, 0, 300, 200);
        Element col2 = newDivWithGeometry(doc, "t96OvCol2", 250, 0, 700, 200); // col1과 겹친다.
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult r : results) {
            assertTrue("overlap-no-split-decision: no SPLIT_LAYOUT result is ever produced",
                    !"SPLIT_LAYOUT".equals(r.getSemanticType()));
        }
    }

    /** SPLIT_LAYOUT이 아닌 family(TAB_CONTROL)의 다중 panes assignment는 이번 Slice 이전과
     * 동일하게 순수 assignSlot 호출(list 삽입) 순서를 그대로 유지해야 한다(재정렬 없음). */
    private static void testNonSplitFamilySlotOrderPreservedUnchanged() throws Exception {
        CompositionDecision outerTab = new CompositionDecision();
        outerTab.setFamily("TAB_CONTROL");
        outerTab.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        outerTab.setSourceStructuralId("Form[0]/Tab[96]");
        outerTab.setEligible(true);

        CompositionDecision paneB = new CompositionDecision();
        paneB.setFamily("TAB_CONTROL");
        paneB.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        paneB.setSourceStructuralId("Form[0]/Tab[96]/Tabpage[0]/Tab[0]");
        paneB.setEligible(true);
        // TAB_CONTROL.panes는 exact TabPageMembership이 선택된 parent와 일치해야 한다.
        paneB.setTabPageMembership(new TabPageMembership("Form[0]/Tab[96]", 0));

        CompositionDecision paneA = new CompositionDecision();
        paneA.setFamily("TAB_CONTROL");
        paneA.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        paneA.setSourceStructuralId("Form[0]/Tab[96]/Tabpage[1]/Tab[0]");
        paneA.setEligible(true);
        paneA.setTabPageMembership(new TabPageMembership("Form[0]/Tab[96]", 1));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        // 의도적으로 "B before A" 순서로 배정한다(어떤 lexical/geometry 기준으로도 이미 정렬된
        // 순서가 아니라는 것 자체는 중요하지 않다 -- 핵심은 "정렬하지 않고 그대로"라는 사실이다).
        assertTrue("non-split-order-preserved: precondition -- assignSlot(paneB) succeeds",
                evaluator.assignSlot(outerTab, "panes", paneB));
        assertTrue("non-split-order-preserved: precondition -- assignSlot(paneA) succeeds",
                evaluator.assignSlot(outerTab, "panes", paneA));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(outerTab);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        List<String> order = new ArrayList<String>();
        for (TargetCompositionEdge edge : plan.getEdges()) {
            if ("panes".equals(edge.getSlot())) {
                order.add(edge.getChild().getSourceStructuralId());
            }
        }
        assertEquals("non-split-order-preserved: panes edges appear in exact assignSlot call order (B,A), "
                + "not re-sorted", paneB.getSourceStructuralId() + "," + paneA.getSourceStructuralId(),
                joinList(order));
    }

    // ==== TAB_CONTROL 정확한 page membership Plan validation ====

    private static CompositionDecision tabControlDecision(String structuralId) {
        CompositionDecision d = new CompositionDecision();
        d.setFamily("TAB_CONTROL");
        d.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        d.setSourceStructuralId(structuralId);
        d.setEligible(true);
        return d;
    }

    private static void testTabControlPanesValidMembershipCreatesEdgePageOrdinal() throws Exception {
        CompositionDecision outer = tabControlDecision("Form[0]/Tab[0]");
        CompositionDecision child = tabControlDecision("Form[0]/Tab[0]/Tabpage[0]/Tab[0]");
        child.setTabPageMembership(new TabPageMembership("Form[0]/Tab[0]", 2));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("panes-valid-membership: assignSlot succeeds", evaluator.assignSlot(outer, "panes", child));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(outer));
        assertEquals("panes-valid-membership: exactly 1 edge", "1", String.valueOf(plan.getEdges().size()));
        TargetCompositionEdge edge = plan.getEdges().get(0);
        assertEquals("panes-valid-membership: edge.pageOrdinal == membership.pageOrdinal", "2",
                String.valueOf(edge.getPageOrdinal()));
    }

    private static void testTabControlPanesMissingMembershipFailsClosed() throws Exception {
        CompositionDecision outer = tabControlDecision("Form[0]/Tab[1]");
        CompositionDecision child = tabControlDecision("Form[0]/Tab[1]/Tabpage[0]/Tab[0]");
        // setTabPageMembership(...) 호출을 하지 않으므로 membership은 null로 유지된다.

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("panes-missing-membership: assignSlot succeeds", evaluator.assignSlot(outer, "panes", child));

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(outer));
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("panes-missing-membership: message mentions membership_missing",
                    expected.getMessage().contains("tab_control_panes_membership_missing"));
        }
        assertTrue("panes-missing-membership: build() throws", threw);
    }

    private static void testTabControlPanesParentMismatchFailsClosed() throws Exception {
        CompositionDecision outer = tabControlDecision("Form[0]/Tab[2]");
        CompositionDecision child = tabControlDecision("Form[0]/Tab[2]/Tabpage[0]/Tab[0]");
        // 정확히 불일치하는 경우 -- membership이 실제로 배정되는 TAB_CONTROL과는 다른
        // TAB_CONTROL을 주장한다.
        child.setTabPageMembership(new TabPageMembership("Form[0]/Tab[999]", 0));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("panes-parent-mismatch: assignSlot succeeds", evaluator.assignSlot(outer, "panes", child));

        boolean threw = false;
        try {
            new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(outer));
        } catch (IllegalStateException expected) {
            threw = true;
            assertTrue("panes-parent-mismatch: message mentions membership_mismatch",
                    expected.getMessage().contains("tab_control_panes_membership_mismatch"));
        }
        assertTrue("panes-parent-mismatch: build() throws", threw);
    }

    private static void testTabControlPanesPageOrdinalPreservedUnchanged() throws Exception {
        CompositionDecision outer = tabControlDecision("Form[0]/Tab[3]");
        CompositionDecision childA = tabControlDecision("Form[0]/Tab[3]/Tabpage[0]/Tab[0]");
        childA.setTabPageMembership(new TabPageMembership("Form[0]/Tab[3]", 0));
        CompositionDecision childB = tabControlDecision("Form[0]/Tab[3]/Tabpage[1]/Tab[0]");
        childB.setTabPageMembership(new TabPageMembership("Form[0]/Tab[3]", 7));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        assertTrue("panes-preserved: assignSlot(A) succeeds", evaluator.assignSlot(outer, "panes", childA));
        assertTrue("panes-preserved: assignSlot(B) succeeds", evaluator.assignSlot(outer, "panes", childB));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(outer));
        Map<String, Integer> ordinalByChildStructuralId = new LinkedHashMap<String, Integer>();
        for (TargetCompositionEdge edge : plan.getEdges()) {
            ordinalByChildStructuralId.put(edge.getChild().getSourceStructuralId(), edge.getPageOrdinal());
        }
        assertEquals("panes-preserved: childA pageOrdinal unchanged (0)", "0",
                String.valueOf(ordinalByChildStructuralId.get(childA.getSourceStructuralId())));
        assertEquals("panes-preserved: childB pageOrdinal unchanged (7, not renumbered/sorted)", "7",
                String.valueOf(ordinalByChildStructuralId.get(childB.getSourceStructuralId())));
    }

    private static void testNonTabControlEdgeHasNullPageOrdinal() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);
        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        CompositionDecision child = fx.decisionByStructuralId.get(candidate.getChildStructuralId());
        assertTrue("non-tab-control-null-ordinal: assignSlot succeeds",
                new CompositionEvaluator().assignSlot(parent, candidate.getSlot(), child));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(fx.decisions);
        assertEquals("non-tab-control-null-ordinal: exactly 1 edge", "1", String.valueOf(plan.getEdges().size()));
        assertTrue("non-tab-control-null-ordinal: pageOrdinal is null for a SPLIT_LAYOUT.columns edge",
                plan.getEdges().get(0).getPageOrdinal() == null);
    }

    /** SLOT_FILL_5(TAB_CONTROL.panes)의 allowedChildFamilies가 정확히 7개(GRID/TAB_CONTROL/
     * SPLIT_LAYOUT/SEARCH_AREA/BUSINESS_TABLE/TITLE_BAR/BUTTON_GROUP)이고 그 밖의 family는
     * 여전히 거부됨을 확인한다(임의 wildcard가 아님). */
    private static void testTabControlPanesSevenFamilyChildPermissionExactNoWildcard() throws Exception {
        java.util.Set<String> allowed = new java.util.LinkedHashSet<String>(
                CompositionRuleCatalog.slotFillRule("TAB_CONTROL", "panes").getAllowedChildFamilies());
        java.util.Set<String> expected = new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
                "GRID", "TAB_CONTROL", "SPLIT_LAYOUT", "SEARCH_AREA", "BUSINESS_TABLE", "TITLE_BAR", "BUTTON_GROUP"));
        assertEquals("seven-family-exact: size", "7", String.valueOf(allowed.size()));
        assertTrue("seven-family-exact: set equals exactly the 7 accepted families", allowed.equals(expected));
        assertTrue("seven-family-exact: unrelated family (e.g. TREEVIEW) not permitted",
                !allowed.contains("TREEVIEW"));
    }

    private static final class SplitFixture3 {
        List<CompositionDecision> decisions;
        Map<String, CompositionDecision> decisionByStructuralId;
        SemanticRegionGraph graph;
        List<SlotAssignmentCandidate> candidates;
        CompositionDecision splitParent;
        CompositionDecision childW300;
        CompositionDecision childW600;
        CompositionDecision childW100;
        String gridW300Id;
        String gridW600Id;
        String gridW100Id;
    }

    /**
     * 3-column SPLIT_LAYOUT fixture(geometry 고정: w300@left0, w600@left300, w100@left900).
     * {@code domAppendOrder}로 DOM/source 구조만 바꿔 permutation-stability를 검증하며,
     * candidate generation까지 실제 production pipeline을 그대로 수행한다.
     */
    private static SplitFixture3 buildThreeColumnSplitFixture(int[] domAppendOrder) throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitRoot = newDiv(doc, "t96SplitRoot");

        Element colW300 = newDivWithGeometry(doc, "t96ColW300", 0, 0, 300, 200);
        Element wrapW300 = newDiv(doc, "t96WrapW300");
        Element gridW300 = newElement(doc, "Grid", "t96GridW300");
        wrapW300.appendChild(gridW300);
        colW300.appendChild(wrapW300);

        Element colW600 = newDivWithGeometry(doc, "t96ColW600", 300, 0, 600, 200);
        Element wrapW600 = newDiv(doc, "t96WrapW600");
        Element gridW600 = newElement(doc, "Grid", "t96GridW600");
        wrapW600.appendChild(gridW600);
        colW600.appendChild(wrapW600);

        Element colW100 = newDivWithGeometry(doc, "t96ColW100", 900, 0, 100, 200);
        Element wrapW100 = newDiv(doc, "t96WrapW100");
        Element gridW100 = newElement(doc, "Grid", "t96GridW100");
        wrapW100.appendChild(gridW100);
        colW100.appendChild(wrapW100);

        Element[] cols = {colW300, colW600, colW100};
        for (int idx : domAppendOrder) {
            splitRoot.appendChild(cols[idx]);
        }
        form.appendChild(splitRoot);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        Map<String, CompositionDecision> byStructuralId = indexByStructuralId(decisions);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        SplitFixture3 fx = new SplitFixture3();
        fx.decisions = decisions;
        fx.decisionByStructuralId = byStructuralId;
        fx.graph = graph;
        fx.candidates = candidates;
        fx.gridW300Id = SourceStructuralIdentity.build(gridW300);
        fx.gridW600Id = SourceStructuralIdentity.build(gridW600);
        fx.gridW100Id = SourceStructuralIdentity.build(gridW100);

        for (CompositionDecision d : decisions) {
            if ("SPLIT_LAYOUT".equals(d.getFamily())) {
                fx.splitParent = d;
            }
        }
        assertTrue("three-column-split-fixture: precondition -- SPLIT_LAYOUT decision found",
                fx.splitParent != null);
        assertEquals("three-column-split-fixture: precondition -- HIGH/ratio_split",
                "ratio_split", fx.splitParent.getVariant());

        fx.childW300 = byStructuralId.get(fx.gridW300Id);
        fx.childW600 = byStructuralId.get(fx.gridW600Id);
        fx.childW100 = byStructuralId.get(fx.gridW100Id);
        assertTrue("three-column-split-fixture: precondition -- all 3 GRID children resolved",
                fx.childW300 != null && fx.childW600 != null && fx.childW100 != null);
        return fx;
    }

    /** {@code children}의 SPLIT_LAYOUT.columns candidate들을 실제 production approval/apply
     * 경로로 승인/적용한다 -- rank evidence는 이 경로를 통해서만 {@link SlotAssignment}에 옮겨진다. */
    private static void approveAndApplyColumns(SplitFixture3 fx, CompositionDecision... children) {
        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        for (CompositionDecision child : children) {
            SlotAssignmentCandidate candidate = findCandidateForChild(
                    fx.candidates, "SPLIT_LAYOUT", child.getSourceStructuralId(), "columns");
            assertTrue("approve-and-apply-columns: precondition -- candidate found for child "
                    + child.getSourceStructuralId(), candidate != null);
            assertTrue("approve-and-apply-columns: precondition -- approve succeeds for child "
                    + child.getSourceStructuralId(), registry.approve(candidate, "slice96-revision-test"));
        }
        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("approve-and-apply-columns: applied count matches requested children",
                String.valueOf(children.length), String.valueOf(applied));
    }

    private static SlotAssignmentCandidate findCandidateForChild(
            List<SlotAssignmentCandidate> candidates, String parentFamily, String childStructuralId, String slot) {
        for (SlotAssignmentCandidate candidate : candidates) {
            if (parentFamily.equals(candidate.getParentFamily()) && slot.equals(candidate.getSlot())
                    && childStructuralId.equals(candidate.getChildStructuralId())) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> columnsChildStructuralIdOrder(TargetCompositionPlan plan, String parentStructuralId) {
        List<String> order = new ArrayList<String>();
        for (TargetCompositionEdge edge : plan.getEdges()) {
            if ("columns".equals(edge.getSlot()) && parentStructuralId.equals(edge.getParent().getSourceStructuralId())) {
                order.add(edge.getChild().getSourceStructuralId());
            }
        }
        return order;
    }

    private static String joinList(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private static String childNodeIdForParent(TargetCompositionPlan plan, String parentStructuralId) {
        for (TargetCompositionEdge edge : plan.getEdges()) {
            if (parentStructuralId.equals(edge.getParent().getSourceStructuralId())) {
                return edge.getChild().getNodeId();
            }
        }
        return null;
    }

    private static java.util.Set<String> nodeIdSet(TargetCompositionPlan plan) {
        java.util.Set<String> ids = new java.util.TreeSet<String>();
        for (TargetCompositionNode node : plan.getNodes()) {
            ids.add(node.getNodeId());
        }
        return ids;
    }

    private static java.util.Set<String> edgeKeySet(TargetCompositionPlan plan) {
        java.util.Set<String> keys = new java.util.TreeSet<String>();
        for (TargetCompositionEdge edge : plan.getEdges()) {
            keys.add(edge.getParent().getNodeId() + "|" + edge.getSlot() + "|" + edge.getChild().getNodeId());
        }
        return keys;
    }

    private static java.util.Set<String> rootIdSet(TargetCompositionPlan plan) {
        java.util.Set<String> ids = new java.util.TreeSet<String>();
        for (TargetCompositionNode node : plan.getRootNodes()) {
            ids.add(node.getNodeId());
        }
        return ids;
    }

    // ---- 헬퍼(helpers) ----

    private static List<CompositionDecision> evaluateAll(List<SemanticRegionResult> results) {
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        CompositionEvaluator evaluator = new CompositionEvaluator();
        for (SemanticRegionResult r : results) {
            decisions.add(evaluator.evaluate(r));
        }
        return decisions;
    }

    private static Map<String, CompositionDecision> indexByStructuralId(List<CompositionDecision> decisions) {
        Map<String, CompositionDecision> map = new LinkedHashMap<String, CompositionDecision>();
        for (CompositionDecision d : decisions) {
            if (d.getSourceStructuralId() != null && d.getSourceStructuralId().length() > 0) {
                map.put(d.getSourceStructuralId(), d);
            }
        }
        return map;
    }

    private static SlotAssignmentCandidate findCandidate(
            List<SlotAssignmentCandidate> candidates, String parentFamily, String childFamily, String slot) {
        for (SlotAssignmentCandidate candidate : candidates) {
            if (parentFamily.equals(candidate.getParentFamily()) && childFamily.equals(candidate.getChildFamily())
                    && slot.equals(candidate.getSlot())) {
                return candidate;
            }
        }
        return null;
    }

    // ---- fixture 모음 ----

    private static final class Fixture {
        List<SemanticRegionResult> results;
        List<CompositionDecision> decisions;
        Map<String, CompositionDecision> decisionByStructuralId;
        SemanticRegionGraph graph;
        List<SlotAssignmentCandidate> candidates;
    }

    private static final class AmbiguityFixture {
        Map<String, CompositionDecision> decisionByStructuralId;
        List<CompositionDecision> decisions;
        SemanticRegionGraph graph;
        List<SlotAssignmentCandidate> candidates;
    }

    private static Fixture buildSplitLayoutWithNestedGridFixture() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitRoot = newDiv(doc, "fxSplitRoot");
        Element col1 = newDivWithGeometry(doc, "fxCol1", 0, 0, 500, 200);
        Element wrapper = newDiv(doc, "fxWrapper");
        Element grid = newElement(doc, "Grid", "fxGrid");
        wrapper.appendChild(grid);
        col1.appendChild(wrapper);
        Element col2 = newDivWithGeometry(doc, "fxCol2", 500, 0, 500, 200);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        Fixture fx = new Fixture();
        fx.results = new SemanticRegionSegmenter().segment(form);
        fx.decisions = evaluateAll(fx.results);
        fx.decisionByStructuralId = indexByStructuralId(fx.decisions);
        fx.graph = new SemanticRegionRelationshipExtractor().buildGraph(form, fx.results);
        fx.candidates = new SlotAssignmentCandidateGenerator().generateCandidates(fx.graph, fx.decisions);
        return fx;
    }

    /** BUTTON_GROUP이 SPLIT_LAYOUT/BUSINESS_TABLE 양쪽의 실제 조상 대상인 ambiguity fixture를 재구성한다. */
    private static AmbiguityFixture buildAmbiguityFixture() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRoot = newDiv(doc, "ambSplitRoot15");
        Element col1 = newDivWithGeometry(doc, "ambCol115", 0, 0, 500, 400);
        Element col2 = newDivWithGeometry(doc, "ambCol215", 500, 0, 500, 400);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        Element tableWrap = newDiv(doc, "ambTableWrap15");
        Element label1 = newElement(doc, "Static", "ambLabel115");
        setGeometry(label1, 0, 0, 80, 20);
        Element edit1 = newElement(doc, "Edit", "ambEdit115");
        setGeometry(edit1, 90, 0, 100, 20);
        Element label2 = newElement(doc, "Static", "ambLabel215");
        setGeometry(label2, 0, 30, 80, 20);
        Element edit2 = newElement(doc, "Edit", "ambEdit215");
        setGeometry(edit2, 90, 30, 100, 20);
        tableWrap.appendChild(label1);
        tableWrap.appendChild(edit1);
        tableWrap.appendChild(label2);
        tableWrap.appendChild(edit2);
        col1.appendChild(tableWrap);

        Element buttonGroupWrap = newDivWithGeometry(doc, "ambButtonWrap15", 0, 0, 100, 40);
        Element btn1 = newElement(doc, "Button", "ambBtn115");
        setGeometry(btn1, 10, 0, 60, 20);
        buttonGroupWrap.appendChild(btn1);
        edit2.appendChild(buttonGroupWrap);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        AmbiguityFixture fx = new AmbiguityFixture();
        fx.decisionByStructuralId = indexByStructuralId(decisions);
        fx.decisions = decisions;
        fx.graph = graph;
        fx.candidates = new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        return fx;
    }

    private static Element newDiv(Document doc, String id) { return newElement(doc, "Div", id); }

    private static Element newDivWithGeometry(Document doc, String id, double left, double top, double width, double height) {
        Element div = newDiv(doc, id);
        setGeometry(div, left, top, width, height);
        return div;
    }

    private static Element newElement(Document doc, String tag, String id) {
        Element element = doc.createElement(tag);
        element.setAttribute("id", id);
        return element;
    }

    private static void setGeometry(Element el, double left, double top, double width, double height) {
        el.setAttribute("left", formatAttr(left));
        el.setAttribute("top", formatAttr(top));
        el.setAttribute("width", formatAttr(width));
        el.setAttribute("height", formatAttr(height));
    }

    private static String formatAttr(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + label);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
