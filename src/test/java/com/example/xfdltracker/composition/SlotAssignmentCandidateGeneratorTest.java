package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SlotAssignmentCandidateGenerator}가 실제 DOM 관계와 catalog SLOT_FILL 규칙이 동시에
 * 성립할 때만 candidate를 만드는지, {@code sourceStructuralId}만으로 cross-wiring 없이 연결하는지,
 * {@code assignSlot}/{@code slotAssignments}를 절대 건드리지 않는지를 검증하는 오프라인 unit test.
 */
public class SlotAssignmentCandidateGeneratorTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTabpageCorrelationNoCrossWiring();
        testGroupBoxCorrelationNoCrossWiring();
        testResultOrderIndependence();
        testValidDocumentedRelationshipProducesCandidate();
        testInvalidFamilyRelationProducesNoCandidate();
        testAmbiguityPreservedNoAutoSelection();
        testLowSplitFallbackProducesNoCandidate();
        testTargetSyntheticPagingNeverBecomesCandidate();
        testCandidateGenerationNeverTouchesSlotAssignments();
        testPagingSourceEmissionStillZero();

        // ---- Candidate Integrity 강화 ----
        testValidGridDecisionCandidateRegression();
        testInvalidVariantProducesNoCandidate();
        testInvalidParameterProducesNoCandidate();
        testFamilyNodeMismatchProducesNoCandidate();
        testDuplicateSourceSemanticDecisionRejected();
        testNullOriginDecisionProducesNoCandidate();
        testTargetSyntheticNeverProducesCandidateHardened();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** Tabpage 아래 동일 bare id("item")의 Grid 2개가 서로 다른 sourceStructuralId를 갖고,
     * 각자 정확한 Graph node/Decision과만 연결됨을 확인한다. */
    private static void testTabpageCorrelationNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element tab = newElement(doc, "Tab", "mainTab");
        form.appendChild(tab);

        Element pageA = newElement(doc, "Tabpage", "pageA");
        Element gridA = newElement(doc, "Grid", "item");
        pageA.appendChild(gridA);
        tab.appendChild(pageA);

        Element pageB = newElement(doc, "Tabpage", "pageB");
        Element gridB = newElement(doc, "Grid", "item");
        pageB.appendChild(gridB);
        tab.appendChild(pageB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> grids = allByType(results, "GRID");
        assertEquals("tabpage-correlation: 2 GRID results", "2", String.valueOf(grids.size()));
        assertEquals("tabpage-correlation: sourceRegionId identical (precondition, real collision)",
                grids.get(0).getSourceRegionId(), grids.get(1).getSourceRegionId());
        assertTrue("tabpage-correlation: sourceStructuralId distinct",
                !grids.get(0).getSourceStructuralId().equals(grids.get(1).getSourceStructuralId()));

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision decisionA = findByStructuralId(decisions, grids.get(0).getSourceStructuralId());
        CompositionDecision decisionB = findByStructuralId(decisions, grids.get(1).getSourceStructuralId());
        assertTrue("tabpage-correlation: decision A structuralId is an actual graph node",
                graph.getNodeStructuralIds().contains(decisionA.getSourceStructuralId()));
        assertTrue("tabpage-correlation: decision B structuralId is an actual graph node",
                graph.getNodeStructuralIds().contains(decisionB.getSourceStructuralId()));
        assertTrue("tabpage-correlation: no cross-wiring (A and B remain distinct decisions)",
                decisionA != decisionB && !decisionA.getSourceStructuralId().equals(decisionB.getSourceStructuralId()));
    }

    /** structural correlation 2: GroupBox(Div가 아닌 container)에서도 동일하게 성립. */
    private static void testGroupBoxCorrelationNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element groupA = newElement(doc, "GroupBox", "grpA");
        Element gridA = newElement(doc, "Grid", "item");
        groupA.appendChild(gridA);
        form.appendChild(groupA);

        Element groupB = newElement(doc, "GroupBox", "grpB");
        Element gridB = newElement(doc, "Grid", "item");
        groupB.appendChild(gridB);
        form.appendChild(groupB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> grids = allByType(results, "GRID");
        assertEquals("groupbox-correlation: sourceRegionId identical (precondition)",
                grids.get(0).getSourceRegionId(), grids.get(1).getSourceRegionId());
        assertTrue("groupbox-correlation: sourceStructuralId distinct",
                !grids.get(0).getSourceStructuralId().equals(grids.get(1).getSourceStructuralId()));

        List<CompositionDecision> decisions = evaluateAll(results);
        CompositionDecision decisionA = findByStructuralId(decisions, grids.get(0).getSourceStructuralId());
        CompositionDecision decisionB = findByStructuralId(decisions, grids.get(1).getSourceStructuralId());
        assertTrue("groupbox-correlation: no cross-wiring", decisionA != decisionB);
    }

    /** result/decision 목록 순서를 뒤집어도 candidate 집합이 동일해야 한다. */
    private static void testResultOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        List<CompositionDecision> reversed = new ArrayList<CompositionDecision>(decisions);
        Collections.reverse(reversed);

        SlotAssignmentCandidateGenerator generator = new SlotAssignmentCandidateGenerator();
        List<SlotAssignmentCandidate> candidatesOriginal = generator.generateCandidates(graph, decisions);
        List<SlotAssignmentCandidate> candidatesReversed = generator.generateCandidates(graph, reversed);

        assertTrue("result-order-independence: at least 1 candidate to make the test meaningful",
                !candidatesOriginal.isEmpty());
        assertEquals("result-order-independence: same candidate count",
                String.valueOf(candidatesOriginal.size()), String.valueOf(candidatesReversed.size()));
        assertTrue("result-order-independence: identical candidate sets",
                sameCandidateSet(candidatesOriginal, candidatesReversed));
    }

    /** SPLIT_LAYOUT.columns 안쪽(wrapper를 거친 ANCESTOR_CONTAINS) GRID -- SLOT_FILL_4가 GRID를
     * 허용하므로 candidate가 생성되어야 한다. slotAssignments는 여전히 0. */
    private static void testValidDocumentedRelationshipProducesCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitRoot = buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        SlotAssignmentCandidate splitToGrid = findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns");
        assertTrue("valid-relationship: SPLIT_LAYOUT.columns <- GRID candidate exists", splitToGrid != null);
        assertEquals("valid-relationship: composition rule id is SLOT_FILL_4",
                "SLOT_FILL_4", splitToGrid.getCompositionRuleId());
        assertTrue("valid-relationship: sourceRelationshipType is ANCESTOR_CONTAINS (wrapper hop)",
                splitToGrid.getSourceRelationshipType() == SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS);

        for (CompositionDecision decision : decisions) {
            assertEquals("valid-relationship: slotAssignments still 0 after candidate generation ("
                    + decision.getFamily() + ")", "0", String.valueOf(decision.getSlotAssignments().size()));
        }
    }

    /** GRID(자신의 columns/row_template/paging slot 어디에도 SPLIT_LAYOUT을 허용하지 않음) 안에 SPLIT_LAYOUT 중첩 -- candidate 0. */
    private static void testInvalidFamilyRelationProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "outerGrid");
        form.appendChild(grid);
        buildSplitLayoutWithNestedGridFixture(doc, grid); // Grid 내부에 유효한 SPLIT_LAYOUT 구조를 중첩시킨다.

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        assertTrue("invalid-family-relation: precondition -- both GRID(outer) and SPLIT_LAYOUT emitted",
                firstByTypeOrNull(results, "GRID") != null && firstByTypeOrNull(results, "SPLIT_LAYOUT") != null);

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        assertTrue("invalid-family-relation: precondition -- GRID(outer) ANCESTOR_CONTAINS SPLIT_LAYOUT",
                !graph.getRelationshipsOfType(SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS).isEmpty());

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        for (SlotAssignmentCandidate candidate : candidates) {
            assertTrue("invalid-family-relation: no candidate with GRID as parent (GRID's slots never allow "
                    + "a Template Family child)", !"GRID".equals(candidate.getParentFamily()));
        }
    }

    /** 하나의 BUTTON_GROUP이 SPLIT_LAYOUT과 BUSINESS_TABLE 양쪽의 ANCESTOR_CONTAINS 대상이
     * 되는 실제 catalog 규칙 조합 -- 두 candidate 모두 보존되어야 하며 자동 선택하지 않는다. */
    private static void testAmbiguityPreservedNoAutoSelection() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRoot = newDiv(doc, "ambSplitRoot");
        Element col1 = newDivWithGeometry(doc, "ambCol1", 0, 0, 500, 400);
        Element col2 = newDivWithGeometry(doc, "ambCol2", 500, 0, 500, 400);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        // container=tableWrap 자체에 대한 BUSINESS_TABLE: 그 DIRECT children은 top 좌표가 같은
        // 2개 row에 걸쳐 Static/Edit이 번갈아 나와야 한다(buildTableRows는 공유 top 좌표로 그룹핑).
        Element tableWrap = newDiv(doc, "ambTableWrap");
        Element label1 = newElement(doc, "Static", "ambLabel1");
        setGeometry(label1, 0, 0, 80, 20);
        Element edit1 = newElement(doc, "Edit", "ambEdit1");
        setGeometry(edit1, 90, 0, 100, 20);
        Element label2 = newElement(doc, "Static", "ambLabel2");
        setGeometry(label2, 0, 30, 80, 20);
        Element edit2 = newElement(doc, "Edit", "ambEdit2");
        setGeometry(edit2, 90, 30, 100, 20);
        tableWrap.appendChild(label1);
        tableWrap.appendChild(edit1);
        tableWrap.appendChild(label2);
        tableWrap.appendChild(edit2);
        col1.appendChild(tableWrap);

        List<SemanticRegionResult> resultsForTableCheck = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult tableCheck = firstByTypeOrNull(resultsForTableCheck, "BUSINESS_TABLE");
        assertTrue("ambiguity: precondition -- BUSINESS_TABLE actually emitted for ambTableWrap itself",
                tableCheck != null && "ambSplitRoot.ambCol1.ambTableWrap".equals(tableCheck.getSourceRegionId()));

        // edit2의 descendant로 중첩된 BUTTON_GROUP(여전히 ambTableWrap의 subtree 안에 있어서
        // ambSplitRoot와 ambTableWrap 둘 다에게 동시에 ANCESTOR_CONTAINS된다) -- 의도적으로
        // DOM상 이례적인 중첩이지만 구조적으로는 합법이며, 실제 ambiguity 시나리오를 검증한다.
        Element buttonGroupWrap = newDivWithGeometry(doc, "ambButtonWrap", 0, 0, 100, 40);
        Element btn1 = newElement(doc, "Button", "ambBtn1");
        setGeometry(btn1, 10, 0, 60, 20);
        buttonGroupWrap.appendChild(btn1);
        edit2.appendChild(buttonGroupWrap);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        assertTrue("ambiguity: precondition -- BUTTON_GROUP actually emitted", firstByTypeOrNull(results, "BUTTON_GROUP") != null);

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        SlotAssignmentCandidate fromSplit = findCandidate(candidates, "SPLIT_LAYOUT", "BUTTON_GROUP", "columns");
        SlotAssignmentCandidate fromTable = findCandidate(candidates, "BUSINESS_TABLE", "BUTTON_GROUP", "td_content");
        assertTrue("ambiguity: candidate from SPLIT_LAYOUT.columns preserved", fromSplit != null);
        assertTrue("ambiguity: candidate from BUSINESS_TABLE.td_content preserved", fromTable != null);
        assertTrue("ambiguity: the two candidates point to the same child BUTTON_GROUP decision",
                fromSplit.getChildStructuralId().equals(fromTable.getChildStructuralId()));
        assertTrue("ambiguity: no automatic selection happened -- both remain in the candidate list",
                candidates.contains(fromSplit) && candidates.contains(fromTable));
    }

    /** LOW+FIXED_WIDTH_FALLBACK SPLIT_LAYOUT 안쪽 GRID -- canonical slot candidate = 0. */
    private static void testLowSplitFallbackProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element splitParent = newDiv(doc, "lowSplitParent");
        Element colA = newDivWithGeometry(doc, "lowColA", 0, 0, 296, 200);
        Element colB = newDivWithGeometry(doc, "lowColB", 296, 0, 704, 200);
        Element wrapper = newDiv(doc, "lowWrapper");
        Element grid = newElement(doc, "Grid", "lowGrid");
        wrapper.appendChild(grid);
        colA.appendChild(wrapper);
        splitParent.appendChild(colA);
        splitParent.appendChild(colB);
        form.appendChild(splitParent);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult splitResult = firstByType(results, "SPLIT_LAYOUT");
        assertEquals("low-split-fallback: precondition -- LOW confidence", "LOW", splitResult.getConfidence());
        assertEquals("low-split-fallback: precondition -- FIXED_WIDTH_FALLBACK",
                "FIXED_WIDTH_FALLBACK", splitResult.getFallback());

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        for (SlotAssignmentCandidate candidate : candidates) {
            assertTrue("low-split-fallback: no candidate with the LOW+fallback SPLIT_LAYOUT as parent",
                    !"SPLIT_LAYOUT".equals(candidate.getParentFamily()));
        }
    }

    /** TARGET_SYNTHETIC PAGING decision이 실제 관계의 endpoint와 같은 structuralId를 갖게
     * 되어도, origin 필터에 의해 candidate 생성에 전혀 관여하지 않는다. */
    private static void testTargetSyntheticPagingNeverBecomesCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element tab = newElement(doc, "Tab", "pagingTab");
        Element grid = newElement(doc, "Grid", "pagingGrid");
        tab.appendChild(grid);
        form.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision gridDecision = findByFamily(decisions, "GRID");
        CompositionDecision targetSyntheticPaging = new CompositionEvaluator()
                .createTargetSyntheticDecision("PAGING", null);
        targetSyntheticPaging.setSourceStructuralId(gridDecision.getSourceStructuralId()); // 의도적으로 겹치게 만든다.
        assertTrue("target-synthetic-paging: precondition -- eligible via factory", targetSyntheticPaging.isEligible());
        assertTrue("target-synthetic-paging: precondition -- origin TARGET_SYNTHETIC",
                targetSyntheticPaging.getOrigin() == CompositionDecision.Origin.TARGET_SYNTHETIC);

        List<CompositionDecision> withImpostor = new ArrayList<CompositionDecision>(decisions);
        withImpostor.add(targetSyntheticPaging);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, withImpostor);

        for (SlotAssignmentCandidate candidate : candidates) {
            assertTrue("target-synthetic-paging: never appears as parent family",
                    !"PAGING".equals(candidate.getParentFamily()));
            assertTrue("target-synthetic-paging: never appears as child family",
                    !"PAGING".equals(candidate.getChildFamily()));
        }
    }

    /** candidate 생성 전후로 모든 decision의 slotAssignments가 그대로 0(assignSlot 미호출). */
    private static void testCandidateGenerationNeverTouchesSlotAssignments() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        for (CompositionDecision decision : decisions) {
            assertEquals("slot-assignments-untouched: 0 before generation", "0",
                    String.valueOf(decision.getSlotAssignments().size()));
        }

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("slot-assignments-untouched: at least 1 candidate was generated", !candidates.isEmpty());

        for (CompositionDecision decision : decisions) {
            assertEquals("slot-assignments-untouched: still 0 after generation (candidate != assignment)", "0",
                    String.valueOf(decision.getSlotAssignments().size()));
        }
    }

    /** PAGING source emission = 0 재확인(regression). */
    private static void testPagingSourceEmissionStillZero() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "pagingCheckGrid");
        form.appendChild(grid);
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult r : results) {
            assertTrue("paging-source-emission: never PAGING", !"PAGING".equals(r.getSemanticType()));
        }
    }

    /** regression: 하드닝 이후에도 정상 GRID decision + 정상 graph node -> 기존 SPLIT_LAYOUT.columns candidate 그대로 유지. */
    private static void testValidGridDecisionCandidateRegression() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("valid-grid-regression: SPLIT_LAYOUT.columns <- GRID candidate still present",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") != null);
    }

    /**
     * invalid variant: 실제 GRID의 structuralId를 그대로 쓰되 직접 construct + 존재하지 않는
     * variant를 심은 decision으로 교체 -- decision-integrity 재검증이 걸러내 candidate 0.
     */
    private static void testInvalidVariantProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision realGrid = findByFamily(decisions, "GRID");
        String gridStructuralId = realGrid.getSourceStructuralId();
        decisions.remove(realGrid);

        CompositionDecision bypassedGrid = new CompositionDecision();
        bypassedGrid.setFamily("GRID");
        bypassedGrid.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        bypassedGrid.setSourceStructuralId(gridStructuralId);
        bypassedGrid.setVariant("nonexistent_variant");
        bypassedGrid.setEligible(true);
        decisions.add(bypassedGrid);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("invalid-variant: no SPLIT_LAYOUT.columns <- GRID candidate (bypassed decision rejected)",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") == null);
    }

    /** invalid parameter: 존재하지 않는 parameter key를 심은 decision -- candidate 0. */
    private static void testInvalidParameterProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision realGrid = findByFamily(decisions, "GRID");
        String gridStructuralId = realGrid.getSourceStructuralId();
        decisions.remove(realGrid);

        CompositionDecision bypassedGrid = new CompositionDecision();
        bypassedGrid.setFamily("GRID");
        bypassedGrid.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        bypassedGrid.setSourceStructuralId(gridStructuralId);
        bypassedGrid.getParameters().put("made_up_parameter", "x");
        bypassedGrid.setEligible(true);
        decisions.add(bypassedGrid);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("invalid-parameter: no SPLIT_LAYOUT.columns <- GRID candidate (bypassed decision rejected)",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") == null);
    }

    /** 실제 GRID node의 structuralId에 family=TITLE_BAR로 조작한 decision을 심으면
     * graph-node consistency 검증이 걸러내 candidate 0. */
    private static void testFamilyNodeMismatchProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision realGrid = findByFamily(decisions, "GRID");
        String gridStructuralId = realGrid.getSourceStructuralId();
        decisions.remove(realGrid);

        CompositionDecision impostor = new CompositionDecision();
        impostor.setFamily("TITLE_BAR"); // 유효한 catalog family이며 ACCEPTABLE source status다.
        impostor.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        impostor.setSourceStructuralId(gridStructuralId); // 이 anchor의 실제 노드는 GRID다.
        impostor.setEligible(true);
        decisions.add(impostor);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("family-node-mismatch: no SPLIT_LAYOUT.columns <- GRID candidate",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") == null);
        assertTrue("family-node-mismatch: no SPLIT_LAYOUT.columns <- TITLE_BAR candidate either "
                + "(TITLE_BAR isn't the real anchor's semanticType)",
                findCandidate(candidates, "SPLIT_LAYOUT", "TITLE_BAR", "columns") == null);
    }

    /** duplicate decision defense: 같은 structuralId를 가진 SOURCE_SEMANTIC decision 2개 -- 명시적 거부(예외). */
    private static void testDuplicateSourceSemanticDecisionRejected() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "dupCandidateGrid");
        form.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult gridResult = firstByType(results, "GRID");

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decisionA = evaluator.evaluate(gridResult);
        CompositionDecision decisionB = evaluator.evaluate(gridResult); // 동일한 anchor, 동일한 structuralId를 사용한다.
        assertTrue("duplicate-decision: precondition -- both share the same sourceStructuralId",
                decisionA.getSourceStructuralId().equals(decisionB.getSourceStructuralId()));

        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decisionA);
        decisions.add(decisionB);

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        boolean threw = false;
        try {
            new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue("duplicate-decision: generateCandidates refuses (throws) instead of silently "
                + "overwriting or picking first/last", threw);
    }

    /** origin=null(직접 construct, evaluate()/factory 미경유) decision -- candidate 생성에 전혀 관여하지 않는다. */
    private static void testNullOriginDecisionProducesNoCandidate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision realGrid = findByFamily(decisions, "GRID");
        String gridStructuralId = realGrid.getSourceStructuralId();
        decisions.remove(realGrid);

        CompositionDecision noOriginDecision = new CompositionDecision();
        noOriginDecision.setFamily("GRID");
        noOriginDecision.setSourceStructuralId(gridStructuralId);
        noOriginDecision.setEligible(true);
        assertTrue("null-origin: precondition -- origin is null", noOriginDecision.getOrigin() == null);
        decisions.add(noOriginDecision);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("null-origin: no SPLIT_LAYOUT.columns <- GRID candidate",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") == null);
    }

    /** TARGET_SYNTHETIC(LOADING_INDICATOR) decision이 실제 관계 endpoint의 structuralId와
     * 조작으로 겹쳐도 candidate에 전혀 관여하지 않는다(origin 필터가 먼저 차단). */
    private static void testTargetSyntheticNeverProducesCandidateHardened() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixture(doc, form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision realGrid = findByFamily(decisions, "GRID");
        CompositionDecision targetSyntheticLoading = new CompositionEvaluator()
                .createTargetSyntheticDecision("LOADING_INDICATOR", null);
        targetSyntheticLoading.setSourceStructuralId(realGrid.getSourceStructuralId()); // 의도적으로 겹치게 만든다.
        assertTrue("target-synthetic-hardened: precondition -- eligible via factory",
                targetSyntheticLoading.isEligible());
        decisions.add(targetSyntheticLoading);

        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        for (SlotAssignmentCandidate candidate : candidates) {
            assertTrue("target-synthetic-hardened: never appears as parent or child family",
                    !"LOADING_INDICATOR".equals(candidate.getParentFamily())
                            && !"LOADING_INDICATOR".equals(candidate.getChildFamily()));
        }
        // 실제 GRID candidate는 여전히 온전해야 한다 -- 인위적으로 만든 TARGET_SYNTHETIC entry가
        // 그것을 밀어내거나 막지 않았어야 한다.
        assertTrue("target-synthetic-hardened: real SPLIT_LAYOUT.columns <- GRID candidate unaffected",
                findCandidate(candidates, "SPLIT_LAYOUT", "GRID", "columns") != null);
    }

    // ---- fixture 생성 도우미 ----

    /** SPLIT_LAYOUT(HIGH/ratio_split, col_5/col_5) 하나 + col1 안쪽 wrapper를 거친 nested GRID. */
    private static Element buildSplitLayoutWithNestedGridFixture(Document doc, Element parent) {
        Element splitRoot = newDiv(doc, "fxSplitRoot");
        Element col1 = newDivWithGeometry(doc, "fxCol1", 0, 0, 500, 200);
        Element wrapper = newDiv(doc, "fxWrapper");
        Element grid = newElement(doc, "Grid", "fxGrid");
        wrapper.appendChild(grid);
        col1.appendChild(wrapper);
        Element col2 = newDivWithGeometry(doc, "fxCol2", 500, 0, 500, 200);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        parent.appendChild(splitRoot);
        return splitRoot;
    }

    // ---- decision/graph 도우미 ----

    private static List<CompositionDecision> evaluateAll(List<SemanticRegionResult> results) {
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        CompositionEvaluator evaluator = new CompositionEvaluator();
        for (SemanticRegionResult result : results) {
            decisions.add(evaluator.evaluate(result));
        }
        return decisions;
    }

    private static CompositionDecision findByStructuralId(List<CompositionDecision> decisions, String structuralId) {
        for (CompositionDecision decision : decisions) {
            if (structuralId.equals(decision.getSourceStructuralId())) {
                return decision;
            }
        }
        assertTrue("findByStructuralId: " + structuralId + " present", false);
        return null;
    }

    private static CompositionDecision findByFamily(List<CompositionDecision> decisions, String family) {
        for (CompositionDecision decision : decisions) {
            if (family.equals(decision.getFamily())) {
                return decision;
            }
        }
        assertTrue("findByFamily: " + family + " present", false);
        return null;
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

    private static boolean sameCandidateSet(List<SlotAssignmentCandidate> a, List<SlotAssignmentCandidate> b) {
        return candidateKeySet(a).equals(candidateKeySet(b));
    }

    private static java.util.Set<String> candidateKeySet(List<SlotAssignmentCandidate> candidates) {
        java.util.Set<String> keys = new java.util.LinkedHashSet<String>();
        for (SlotAssignmentCandidate c : candidates) {
            keys.add(c.getParentStructuralId() + "|" + c.getChildStructuralId() + "|" + c.getSlot() + "|"
                    + c.getCompositionRuleId());
        }
        return keys;
    }

    // ---- 결과 목록 도우미 ----

    private static SemanticRegionResult firstByType(List<SemanticRegionResult> results, String semanticType) {
        SemanticRegionResult found = firstByTypeOrNull(results, semanticType);
        assertTrue("firstByType: " + semanticType + " present", found != null);
        return found;
    }

    private static SemanticRegionResult firstByTypeOrNull(List<SemanticRegionResult> results, String semanticType) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                return r;
            }
        }
        return null;
    }

    private static List<SemanticRegionResult> allByType(List<SemanticRegionResult> results, String semanticType) {
        List<SemanticRegionResult> found = new ArrayList<SemanticRegionResult>();
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                found.add(r);
            }
        }
        return found;
    }

    // ---- fixture 빌더 도우미 ----

    private static Element newDiv(Document doc, String id) {
        return newElement(doc, "Div", id);
    }

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

    private static void assertTrue(String label, boolean actual) {
        if (!actual) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=true actual=false");
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
