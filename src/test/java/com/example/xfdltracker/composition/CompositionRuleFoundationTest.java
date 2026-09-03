package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * catalog counts 고정, {@link CompositionEvaluator#assignSlot}의 slot assignment contract,
 * origin(SOURCE_SEMANTIC/TARGET_SYNTHETIC) 분리 및 우회 차단을 검증하는 오프라인 unit test.
 * 모든 parent-child 관계는 이 테스트가 직접 만든 explicit/synthetic {@link CompositionDecision}이다.
 */
public class CompositionRuleFoundationTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCatalogCounts();
        testCompositionRuleCount();

        testValidSlotAssignmentAccepted();
        testUnknownSlotRejected();
        testInvalidChildFamilyRejected();
        testCardinalityOverflowRejected();
        testIneligibleChildRejected();

        testPagingTargetSlotInvariant();
        testSplitLayoutColumnsSlotContract();

        testHighSplitLayoutCanonicalColumnsAccepted();
        testLowSplitLayoutFallbackCanonicalSlotRejected();

        testOriginBypassRejected();
        testCandidateFamiliesRejectedViaTargetSyntheticFactoryToo();
        testNoCardinalityRuleHasFiniteMaxAtSlotLevel();

        testCandidateHoldFamiliesStillNeverEligible();
        testRealSegmenterNeverEmitsPaging();

        // ---- Eligibility-Bypass 방지 최종 강화 ----
        testDirectPagingSourceSemanticEligibleTrueRejected();
        testDirectTreeviewSourceSemanticEligibleTrueRejected();
        testDirectCategoryFilterTargetSyntheticEligibleTrueRejected();
        testLegitimateTargetSyntheticPagingStillAccepted();
        testLegitimateSourceSemanticGridStillAccepted();
        testNullOriginStillRejected();
        testBypassedParentRejected();

        // ---- Decision-Integrity 최종 강화 ----
        testDirectSourceInvalidVariantRejected();
        testDirectSourceInvalidParameterRejected();
        testDirectTargetSyntheticInvalidParameterRejected();
        testDirectParentInvalidVariantRejected();
        testDirectParentInvalidParameterRejected();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 1. Catalog invariant hardening: 실제 문서 수량과 정확히 일치해야 한다(다르면 코드가 아니라 불일치를 보고). */
    private static void testCatalogCounts() throws Exception {
        String[] allFamilies = {
                "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "PAGING", "TAB_CONTROL",
                "TREEVIEW", "BUTTON_GROUP", "SPLIT_LAYOUT", "AGREEMENT_LIST", "CATEGORY_FILTER",
                "INFOBOX", "LOADING_INDICATOR"
        };
        assertEquals("catalog-counts: families", "13", String.valueOf(allFamilies.length));

        int variantTotal = 0;
        int parameterTotal = 0;
        int slotTotal = 0;
        for (String family : allFamilies) {
            TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
            assertTrue("catalog-counts: " + family + " known", def != null);
            variantTotal += def.getVariants().size();
            parameterTotal += def.getParameters().size();
            slotTotal += def.getSlots().size();
        }
        assertEquals("catalog-counts: variants (atomic_template_catalog.md 합산)", "20", String.valueOf(variantTotal));
        assertEquals("catalog-counts: parameters (atomic_template_catalog.md 합산)", "20", String.valueOf(parameterTotal));
        assertEquals("catalog-counts: slots (atomic_template_catalog.md 합산)", "26", String.valueOf(slotTotal));
    }

    /** composition_rules.md의 5개 절(순서6+slot-fill6+merge2+nesting6+cardinality5=25)과 정확히 일치. */
    private static void testCompositionRuleCount() throws Exception {
        assertEquals("composition-rule-count: total", "25", String.valueOf(CompositionRuleCatalog.all().size()));
        assertEquals("composition-rule-count: ORDERING", "6",
                String.valueOf(CompositionRuleCatalog.byType(CompositionRule.RuleType.ORDERING).size()));
        assertEquals("composition-rule-count: SLOT_FILL", "6",
                String.valueOf(CompositionRuleCatalog.byType(CompositionRule.RuleType.SLOT_FILL).size()));
        assertEquals("composition-rule-count: MERGE", "2",
                String.valueOf(CompositionRuleCatalog.byType(CompositionRule.RuleType.MERGE).size()));
        assertEquals("composition-rule-count: NESTING", "6",
                String.valueOf(CompositionRuleCatalog.byType(CompositionRule.RuleType.NESTING).size()));
        assertEquals("composition-rule-count: CARDINALITY", "5",
                String.valueOf(CompositionRuleCatalog.byType(CompositionRule.RuleType.CARDINALITY).size()));
    }

    /** SPLIT_LAYOUT.columns 슬롯에 실제 SEARCH_AREA/GRID decision을 배정 -- catalog rule이 허용하는 조합. */
    private static void testValidSlotAssignmentAccepted() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");
        CompositionDecision childSearchArea = syntheticEligibleDecision("SEARCH_AREA", "basic");
        CompositionDecision childGrid = syntheticEligibleDecision("GRID", "basic");

        CompositionEvaluator evaluator = new CompositionEvaluator();
        boolean firstAccepted = evaluator.assignSlot(parent, "columns", childSearchArea);
        boolean secondAccepted = evaluator.assignSlot(parent, "columns", childGrid);

        assertTrue("valid-slot-assignment: first child accepted", firstAccepted);
        assertTrue("valid-slot-assignment: second child accepted", secondAccepted);
        assertEquals("valid-slot-assignment: parent has 2 slot assignments",
                "2", String.valueOf(parent.getSlotAssignments().size()));
        assertEquals("valid-slot-assignment: first assignment slot name", "columns",
                parent.getSlotAssignments().get(0).getSlot());
        assertTrue("valid-slot-assignment: first assignment child is the SEARCH_AREA decision",
                parent.getSlotAssignments().get(0).getChild() == childSearchArea);
    }

    /** catalog에 존재하지 않는 slot 이름 -- 거부, 가장 가까운 실제 slot으로 추측 이동하지 않음. */
    private static void testUnknownSlotRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");
        CompositionDecision child = syntheticEligibleDecision("GRID", "basic");

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "not_a_real_slot", child);

        assertTrue("unknown-slot: rejected", !accepted);
        assertEquals("unknown-slot: parent has 0 slot assignments", "0",
                String.valueOf(parent.getSlotAssignments().size()));
        assertTrue("unknown-slot: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:unknown_slot:not_a_real_slot"));
    }

    /** 실제 존재하는 slot이지만 catalog rule이 허용하지 않는 자식 family -- 거부. */
    private static void testInvalidChildFamilyRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("GRID", "basic");
        CompositionDecision invalidChild = syntheticEligibleDecision("TITLE_BAR", "title_only");

        // GRID의 columns/row_template slot은 FORM_FIELD 컨트롤 전용이며 어떤 Template Family도
        // 허용 목록에 없다 -- TITLE_BAR를 넣으려는 시도는 항상 거부되어야 한다.
        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", invalidChild);

        assertTrue("invalid-child-family: rejected", !accepted);
        assertTrue("invalid-child-family: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:invalid_child_family:columns:TITLE_BAR"));
    }

    /**
     * SPLIT_LAYOUT.columns/TAB_CONTROL.panes 둘 다 문서상 max=null(제한 없음)이라 실제 overflow를
     * 만들 수 없다 -- 임의의 상한을 새로 발명하지 않고, "max가 없으면 다 수용된다"는 대조군으로
     * cardinality gate 동작을 검증한다. production catalog 데이터는 변경하지 않는다.
     */
    private static void testCardinalityOverflowRejected() throws Exception {
        // 문서 자체 확인: SPLIT_LAYOUT.columns/TAB_CONTROL.panes는 둘 다 max=null(제한 없음).
        CompositionRule splitColumns = CompositionRuleCatalog.cardinalityRule("SPLIT_LAYOUT", "columns");
        assertTrue("cardinality-overflow: SPLIT_LAYOUT.columns min=2", Integer.valueOf(2).equals(splitColumns.getMinCardinality()));
        assertTrue("cardinality-overflow: SPLIT_LAYOUT.columns max unbounded (문서 그대로, 새 상한 발명 안 함)",
                splitColumns.getMaxCardinality() == null);

        // assignSlot 자체의 cardinality 게이트 동작은, catalog에 실제 존재하는 유일한 유의미한
        // 대조군인 "max가 없으면 몇 개를 배정해도 거부되지 않는다"로 검증한다(문서에 없는 상한을
        // 새로 만들어 overflow를 인위적으로 재현하지 않는다).
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        int accepted = 0;
        for (int i = 0; i < 5; i++) {
            if (evaluator.assignSlot(parent, "columns", syntheticEligibleDecision("GRID", "basic"))) {
                accepted++;
            }
        }
        assertEquals("cardinality-overflow: unbounded slot accepts all 5 (no fabricated max)", "5", String.valueOf(accepted));
    }

    /** eligible=false인 child는 검증을 통과한 것으로 취급하지 않는다 -- slot에 넣지 않음. */
    private static void testIneligibleChildRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        SemanticRegionResult badResult = new SemanticRegionResult();
        badResult.setSemanticType("SEARCH_AREA");
        badResult.setRecommendedTemplateFamily("SEARCH_AREA");
        badResult.setRecommendedVariant("nonexistent_variant"); // catalog에 없음 -> ineligible
        CompositionDecision ineligibleChild = new CompositionEvaluator().evaluate(badResult);
        assertTrue("ineligible-child: precondition -- child itself is ineligible", !ineligibleChild.isEligible());

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", ineligibleChild);

        assertTrue("ineligible-child: assignment rejected", !accepted);
        assertTrue("ineligible-child: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:child_not_eligible:columns"));
    }

    /**
     * PAGING target invariant -- source PAGING 자동 생성 경로 없이 오직
     * {@link CompositionEvaluator#createTargetSyntheticDecision}(TARGET_SYNTHETIC 전용)로만
     * GRID.paging이 PAGING을 target child로 허용함을 검증. source PAGING은 여전히 ineligible.
     */
    private static void testPagingTargetSlotInvariant() throws Exception {
        CompositionDecision gridParent = syntheticEligibleDecision("GRID", "basic");

        TemplateFamilyCatalog.FamilyDefinition pagingDef = TemplateFamilyCatalog.get("PAGING");
        assertTrue("paging-slot-invariant: PAGING has no catalog variants (문서: 없음)",
                pagingDef.getVariants().isEmpty());
        assertTrue("paging-slot-invariant: PAGING has page_size parameter", pagingDef.hasParameter("page_size"));

        // (a) source SemanticRegionResult(PAGING) -> 여전히 ineligible (SOURCE_SEMANTIC 경로).
        SemanticRegionResult sourcePagingResult = new SemanticRegionResult();
        sourcePagingResult.setSemanticType("PAGING");
        sourcePagingResult.setRecommendedTemplateFamily("PAGING");
        CompositionDecision sourcePagingDecision = new CompositionEvaluator().evaluate(sourcePagingResult);
        assertTrue("paging-slot-invariant: SOURCE_SEMANTIC PAGING decision still ineligible",
                !sourcePagingDecision.isEligible());
        assertTrue("paging-slot-invariant: SOURCE_SEMANTIC PAGING decision origin is SOURCE_SEMANTIC",
                sourcePagingDecision.getOrigin() == CompositionDecision.Origin.SOURCE_SEMANTIC);

        // (b) TARGET_SYNTHETIC 전용 사전 경로로만 만든 PAGING decision -- target slot contract 전용.
        Map<String, Object> pagingParams = new LinkedHashMap<String, Object>();
        pagingParams.put("page_size", 20);
        CompositionDecision pagingTargetChild =
                new CompositionEvaluator().createTargetSyntheticDecision("PAGING", pagingParams);
        assertTrue("paging-slot-invariant: TARGET_SYNTHETIC PAGING decision is eligible",
                pagingTargetChild.isEligible());
        assertTrue("paging-slot-invariant: TARGET_SYNTHETIC PAGING decision origin is TARGET_SYNTHETIC",
                pagingTargetChild.getOrigin() == CompositionDecision.Origin.TARGET_SYNTHETIC);

        boolean accepted = new CompositionEvaluator().assignSlot(gridParent, "paging", pagingTargetChild);

        assertTrue("paging-slot-invariant: GRID.paging accepts TARGET_SYNTHETIC PAGING child", accepted);
        assertEquals("paging-slot-invariant: gridParent has exactly 1 slot assignment", "1",
                String.valueOf(gridParent.getSlotAssignments().size()));
        assertEquals("paging-slot-invariant: assignment slot name is 'paging'", "paging",
                gridParent.getSlotAssignments().get(0).getSlot());

        // 반대로, 어떤 다른 family(예: GRID 자기 자신)를 GRID.paging에 넣으려 하면 거부돼야 한다
        // (paging slot이 무엇이든 다 받아주는 게 아니라 PAGING만 허용함을 재확인).
        CompositionDecision wrongChild = syntheticEligibleDecision("GRID", "basic");
        boolean wrongAccepted = new CompositionEvaluator().assignSlot(gridParent, "paging", wrongChild);
        assertTrue("paging-slot-invariant: non-PAGING child rejected from GRID.paging", !wrongAccepted);
    }

    /** SPLIT_LAYOUT.columns slot contract -- catalog rule이 정의한 7개 family만 허용, 그 외는 거부. */
    private static void testSplitLayoutColumnsSlotContract() throws Exception {
        CompositionRule rule = CompositionRuleCatalog.slotFillRule("SPLIT_LAYOUT", "columns");
        assertTrue("split-layout-columns-contract: rule found", rule != null);
        assertEquals("split-layout-columns-contract: allowed family count", "7",
                String.valueOf(rule.getAllowedChildFamilies().size()));

        String[] allowed = {"SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "TREEVIEW", "BUTTON_GROUP", "SPLIT_LAYOUT"};
        for (String family : allowed) {
            assertTrue("split-layout-columns-contract: " + family + " is allowed",
                    rule.getAllowedChildFamilies().contains(family));
        }
        assertTrue("split-layout-columns-contract: PAGING is NOT allowed in columns slot",
                !rule.getAllowedChildFamilies().contains("PAGING"));
    }

    /** HIGH confidence + canonical variant(ratio_split)의 SPLIT_LAYOUT.columns 배정은 그대로 허용된다. */
    private static void testHighSplitLayoutCanonicalColumnsAccepted() throws Exception {
        CompositionDecision highParent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");
        assertEquals("high-split-columns-accepted: precondition confidence", "HIGH", highParent.getConfidence());
        assertTrue("high-split-columns-accepted: precondition no fallback", highParent.getFallback() == null);

        boolean accepted = new CompositionEvaluator().assignSlot(
                highParent, "columns", syntheticEligibleDecision("GRID", "basic"));

        assertTrue("high-split-columns-accepted: HIGH ratio_split columns assignment accepted", accepted);
        assertEquals("high-split-columns-accepted: 1 slot assignment recorded", "1",
                String.valueOf(highParent.getSlotAssignments().size()));
    }

    /**
     * 실제 Segmenter가 만드는 SPLIT_LAYOUT LOW+FIXED_WIDTH_FALLBACK 결과는 fallback 값을 보존하되,
     * 그 decision의 columns slot에 canonical 배정을 시도하면 거부되어야 한다
     * (사유 코드: {@code slot_assignment_rejected:low_confidence_canonical_rewrite}).
     */
    private static void testLowSplitLayoutFallbackCanonicalSlotRejected() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "splitParentNonExact", null);
        parent.appendChild(newDivWithGeometry(doc, "colA", 0, 0, 296, 200));
        parent.appendChild(newDivWithGeometry(doc, "colB", 296, 0, 704, 200));

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "SPLIT_LAYOUT");
        CompositionDecision lowFallbackParent = new CompositionEvaluator().evaluate(result);

        assertTrue("low-split-columns-rejected: decision eligible", lowFallbackParent.isEligible());
        assertEquals("low-split-columns-rejected: confidence is LOW", "LOW", lowFallbackParent.getConfidence());
        assertTrue("low-split-columns-rejected: variant still not forced", lowFallbackParent.getVariant() == null);
        assertEquals("low-split-columns-rejected: fallback preserved before rejection attempt",
                "FIXED_WIDTH_FALLBACK", lowFallbackParent.getFallback());

        boolean accepted = new CompositionEvaluator().assignSlot(
                lowFallbackParent, "columns", syntheticEligibleDecision("GRID", "basic"));

        assertTrue("low-split-columns-rejected: canonical columns assignment rejected", !accepted);
        assertEquals("low-split-columns-rejected: no slot assignment recorded", "0",
                String.valueOf(lowFallbackParent.getSlotAssignments().size()));
        assertTrue("low-split-columns-rejected: reason recorded",
                lowFallbackParent.getReasons().contains(
                        "slot_assignment_rejected:low_confidence_canonical_rewrite:columns"));
        assertEquals("low-split-columns-rejected: fallback still preserved after rejection",
                "FIXED_WIDTH_FALLBACK", lowFallbackParent.getFallback());
    }

    /**
     * Origin bypass 차단 -- evaluate()/createTargetSyntheticDecision() 어느 쪽도 거치지 않고
     * 직접 만든 뒤 setEligible(true)만 호출한 child는, origin이 null이라는 이유만으로 거부되어야 한다.
     */
    private static void testOriginBypassRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision bypassChild = new CompositionDecision();
        bypassChild.setFamily("GRID");
        bypassChild.setVariant("basic");
        bypassChild.setConfidence("HIGH");
        bypassChild.setEligible(true); // evaluate()/createTargetSyntheticDecision() 어느 쪽도 거치지 않음
        assertTrue("origin-bypass-rejected: precondition -- origin is null", bypassChild.getOrigin() == null);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", bypassChild);

        assertTrue("origin-bypass-rejected: rejected despite eligible=true", !accepted);
        assertTrue("origin-bypass-rejected: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:untracked_origin:columns"));
    }

    /** candidate family(CANDIDATE_INSUFFICIENT_EVIDENCE)는 TARGET_SYNTHETIC 경로로도 활성화되지 않는다. */
    private static void testCandidateFamiliesRejectedViaTargetSyntheticFactoryToo() throws Exception {
        String[] candidateFamilies = {"CATEGORY_FILTER", "INFOBOX"};
        for (String family : candidateFamilies) {
            TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
            assertTrue("candidate-rejected-via-target-synthetic[" + family + "]: precondition CANDIDATE_INSUFFICIENT_EVIDENCE",
                    def.getTargetFamilyStatus()
                            == TemplateFamilyCatalog.TargetFamilyStatus.CANDIDATE_INSUFFICIENT_EVIDENCE);

            CompositionDecision decision =
                    new CompositionEvaluator().createTargetSyntheticDecision(family, null);
            assertTrue("candidate-rejected-via-target-synthetic[" + family + "]: still ineligible",
                    !decision.isEligible());
            assertTrue("candidate-rejected-via-target-synthetic[" + family + "]: reason recorded",
                    decision.getReasons().contains(
                            "target_status_not_confirmed:" + family + ":CANDIDATE_INSUFFICIENT_EVIDENCE"));
        }
    }

    /**
     * 5개 cardinality 규칙 전체를 순회 -- slot을 갖는 규칙은 둘 다 max=null(제한 없음)이고,
     * finite max를 가진 규칙은 slots가 비어 있다(parameter 값 범위이지 slot 개념이 아님).
     * 즉 slot-assignment 레벨에서 실행 가능한 finite max cardinality 규칙은 문서상 없다.
     */
    private static void testNoCardinalityRuleHasFiniteMaxAtSlotLevel() throws Exception {
        int rulesWithSlotsAndFiniteMax = 0;
        int rulesWithFiniteMaxButNoSlots = 0;
        for (CompositionRule rule : CompositionRuleCatalog.byType(CompositionRule.RuleType.CARDINALITY)) {
            boolean hasSlots = !rule.getSlots().isEmpty();
            boolean hasFiniteMax = rule.getMaxCardinality() != null;
            if (hasSlots && hasFiniteMax) {
                rulesWithSlotsAndFiniteMax++;
            }
            if (!hasSlots && hasFiniteMax) {
                rulesWithFiniteMaxButNoSlots++;
            }
        }
        assertEquals("no-finite-slot-cardinality: 0 cardinality rules combine a slot with a finite max "
                + "(문서 현황 -- 임의 상한을 새로 만들지 않음)", "0", String.valueOf(rulesWithSlotsAndFiniteMax));
        assertEquals("no-finite-slot-cardinality: 2 cardinality rules have finite max but no slot concept "
                + "(CARDINALITY_3 column_pair_count 1~4, CARDINALITY_5 column_count 1~16 -- parameter 값 범위)",
                "2", String.valueOf(rulesWithFiniteMaxButNoSlots));
    }

    /** candidate/HOLD source activation은 여전히 0(Slice 10과 동일하게 재확인). */
    private static void testCandidateHoldFamiliesStillNeverEligible() throws Exception {
        String[] neverActivate = {
                "TREEVIEW", "AGREEMENT_LIST", "CATEGORY_FILTER", "INFOBOX", "PAGING", "LOADING_INDICATOR"
        };
        for (String family : neverActivate) {
            SemanticRegionResult fake = new SemanticRegionResult();
            fake.setSemanticType(family);
            fake.setRecommendedTemplateFamily(family);
            CompositionDecision decision = new CompositionEvaluator().evaluate(fake);
            assertTrue("candidate-hold-never-eligible[" + family + "]", !decision.isEligible());
        }
    }

    /** PAGING source emission = 0(실제 Segmenter 재확인). */
    private static void testRealSegmenterNeverEmitsPaging() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grdPagingCheck2");
        doc.appendChild(grid);
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(grid);
        for (SemanticRegionResult r : results) {
            assertTrue("real-segmenter-no-paging: never PAGING", !"PAGING".equals(r.getSemanticType()));
        }
    }

    /**
     * Defense 1: family=PAGING, origin=SOURCE_SEMANTIC, eligible=true를 직접 조작한 child --
     * source_predicate_status=HOLD인 PAGING을 SOURCE_SEMANTIC이라 주장하는 것 자체가 거부되어야 한다.
     */
    private static void testDirectPagingSourceSemanticEligibleTrueRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision directPaging = new CompositionDecision();
        directPaging.setFamily("PAGING");
        directPaging.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        directPaging.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", directPaging);

        assertTrue("direct-paging-source-semantic-rejected: rejected despite eligible=true", !accepted);
        assertTrue("direct-paging-source-semantic-rejected: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:source_gate_bypassed:columns:PAGING"));
        assertEquals("direct-paging-source-semantic-rejected: no slot assignment recorded", "0",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /**
     * Defense 2: family=TREEVIEW, origin=SOURCE_SEMANTIC, eligible=true를 직접 조작한 child --
     * TREEVIEW는 SLOT_FILL_4로 SPLIT_LAYOUT.columns 허용 목록에 있어, gate가 없으면
     * invalid_child_family 체크를 그냥 통과했을 시나리오를 재현한다.
     */
    private static void testDirectTreeviewSourceSemanticEligibleTrueRejected() throws Exception {
        CompositionRule rule = CompositionRuleCatalog.slotFillRule("SPLIT_LAYOUT", "columns");
        assertTrue("direct-treeview-source-semantic-rejected: precondition -- TREEVIEW is in the allow-list "
                + "(so only the origin/catalog gate can be blocking it)",
                rule.getAllowedChildFamilies().contains("TREEVIEW"));

        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision directTreeview = new CompositionDecision();
        directTreeview.setFamily("TREEVIEW");
        directTreeview.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        directTreeview.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", directTreeview);

        assertTrue("direct-treeview-source-semantic-rejected: rejected despite eligible=true "
                + "and family being in the columns allow-list", !accepted);
        assertTrue("direct-treeview-source-semantic-rejected: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:source_gate_bypassed:columns:TREEVIEW"));
    }

    /**
     * Defense 3: family=CATEGORY_FILTER, origin=TARGET_SYNTHETIC, eligible=true를 직접 조작한
     * child -- 허용 목록에 없어도 invalid_child_family보다 먼저 target_gate_bypassed로 거부됨을 검증.
     */
    private static void testDirectCategoryFilterTargetSyntheticEligibleTrueRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision directCategoryFilter = new CompositionDecision();
        directCategoryFilter.setFamily("CATEGORY_FILTER");
        directCategoryFilter.setOrigin(CompositionDecision.Origin.TARGET_SYNTHETIC);
        directCategoryFilter.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", directCategoryFilter);

        assertTrue("direct-category-filter-target-synthetic-rejected: rejected despite eligible=true", !accepted);
        assertTrue("direct-category-filter-target-synthetic-rejected: reason recorded (fires before "
                + "invalid_child_family, proving the gate is independent of the slot-fill allow-list)",
                parent.getReasons().contains(
                        "slot_assignment_rejected:target_gate_bypassed:columns:CATEGORY_FILTER"));
    }

    /** Defense 4: 정상적으로 factory를 거쳐 만든 TARGET_SYNTHETIC PAGING은 여전히 GRID.paging에 accepted. */
    private static void testLegitimateTargetSyntheticPagingStillAccepted() throws Exception {
        CompositionDecision gridParent = syntheticEligibleDecision("GRID", "basic");
        CompositionDecision legitPaging = new CompositionEvaluator()
                .createTargetSyntheticDecision("PAGING", null);

        boolean accepted = new CompositionEvaluator().assignSlot(gridParent, "paging", legitPaging);

        assertTrue("legitimate-target-synthetic-paging-accepted: GRID.paging accepts factory-made PAGING", accepted);
    }

    /** Defense 5: 정상적으로 evaluate()를 거쳐 만든 SOURCE_SEMANTIC GRID는 기존처럼 valid child로 동작. */
    private static void testLegitimateSourceSemanticGridStillAccepted() throws Exception {
        CompositionDecision splitParent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");
        CompositionDecision legitGrid = syntheticEligibleDecision("GRID", "basic");

        boolean accepted = new CompositionEvaluator().assignSlot(splitParent, "columns", legitGrid);

        assertTrue("legitimate-source-semantic-grid-accepted: SPLIT_LAYOUT.columns accepts real GRID decision",
                accepted);
    }

    /** Defense 6: origin=null인 child는 여전히(하드닝 이후에도) 거부된다. */
    private static void testNullOriginStillRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision noOriginChild = new CompositionDecision();
        noOriginChild.setFamily("GRID");
        noOriginChild.setEligible(true);
        assertTrue("null-origin-still-rejected: precondition -- origin is null", noOriginChild.getOrigin() == null);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", noOriginChild);

        assertTrue("null-origin-still-rejected: rejected", !accepted);
        assertTrue("null-origin-still-rejected: reason recorded",
                parent.getReasons().contains("slot_assignment_rejected:untracked_origin:columns"));
    }

    /**
     * Parent-side defense: 직접 조작한 CATEGORY_FILTER decision을 parent로 사용하는 시도 --
     * "options"는 실존 slot이라 parent gate가 없으면 unknown_slot으로 걸러지지 않는다.
     * parent도 child와 동일하게 origin/catalog 재검증을 받음을 증명한다.
     */
    private static void testBypassedParentRejected() throws Exception {
        TemplateFamilyCatalog.FamilyDefinition categoryFilterDef = TemplateFamilyCatalog.get("CATEGORY_FILTER");
        assertTrue("bypassed-parent-rejected: precondition -- CATEGORY_FILTER really has an 'options' slot "
                + "(so only the parent gate can be blocking it, not unknown_slot)",
                categoryFilterDef.hasSlot("options"));

        CompositionDecision bypassedParent = new CompositionDecision();
        bypassedParent.setFamily("CATEGORY_FILTER");
        bypassedParent.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        bypassedParent.setEligible(true);

        CompositionDecision anyChild = syntheticEligibleDecision("GRID", "basic");

        boolean accepted = new CompositionEvaluator().assignSlot(bypassedParent, "options", anyChild);

        assertTrue("bypassed-parent-rejected: rejected despite parent eligible=true", !accepted);
        assertTrue("bypassed-parent-rejected: reason recorded",
                bypassedParent.getReasons().contains(
                        "slot_assignment_rejected:parent_source_gate_bypassed:options:CATEGORY_FILTER"));
    }

    /**
     * Integrity 1: direct construct로 만든 GRID child에 catalog에 없는
     * variant="nonexistent_variant"를 심어도, decision-integrity 재검증이 이를 잡아내야 한다.
     */
    private static void testDirectSourceInvalidVariantRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision directInvalidVariant = new CompositionDecision();
        directInvalidVariant.setFamily("GRID");
        directInvalidVariant.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        directInvalidVariant.setVariant("nonexistent_variant");
        directInvalidVariant.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", directInvalidVariant);

        assertTrue("direct-source-invalid-variant-rejected: rejected despite eligible=true", !accepted);
        assertTrue("direct-source-invalid-variant-rejected: reason recorded",
                parent.getReasons().contains(
                        "slot_assignment_rejected:invalid_variant:columns:GRID:nonexistent_variant"));
    }

    /** Integrity 2: family=GRID, origin=SOURCE_SEMANTIC, eligible=true + parameter="made_up_parameter" -- 거부. */
    private static void testDirectSourceInvalidParameterRejected() throws Exception {
        CompositionDecision parent = syntheticEligibleDecision("SPLIT_LAYOUT", "ratio_split");

        CompositionDecision directInvalidParameter = new CompositionDecision();
        directInvalidParameter.setFamily("GRID");
        directInvalidParameter.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        directInvalidParameter.getParameters().put("made_up_parameter", "x");
        directInvalidParameter.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(parent, "columns", directInvalidParameter);

        assertTrue("direct-source-invalid-parameter-rejected: rejected despite eligible=true", !accepted);
        assertTrue("direct-source-invalid-parameter-rejected: reason recorded",
                parent.getReasons().contains(
                        "slot_assignment_rejected:invalid_parameter:columns:GRID:made_up_parameter"));
    }

    /**
     * Integrity 3: factory를 거치지 않고 직접 조작한 TARGET_SYNTHETIC PAGING + 없는 parameter --
     * GRID.paging 배정 거부. TARGET_SYNTHETIC 경로도 재검증받음을 증명.
     */
    private static void testDirectTargetSyntheticInvalidParameterRejected() throws Exception {
        CompositionDecision gridParent = syntheticEligibleDecision("GRID", "basic");

        CompositionDecision directInvalidTargetPaging = new CompositionDecision();
        directInvalidTargetPaging.setFamily("PAGING");
        directInvalidTargetPaging.setOrigin(CompositionDecision.Origin.TARGET_SYNTHETIC);
        directInvalidTargetPaging.getParameters().put("made_up_parameter", 1);
        directInvalidTargetPaging.setEligible(true);

        boolean accepted = new CompositionEvaluator().assignSlot(gridParent, "paging", directInvalidTargetPaging);

        assertTrue("direct-target-synthetic-invalid-parameter-rejected: rejected despite eligible=true", !accepted);
        assertTrue("direct-target-synthetic-invalid-parameter-rejected: reason recorded",
                gridParent.getReasons().contains(
                        "slot_assignment_rejected:invalid_parameter:paging:PAGING:made_up_parameter"));
    }

    /** Parent integrity: parent 쪽에 invalid variant를 직접 심어도 재검증에서 거부된다. */
    private static void testDirectParentInvalidVariantRejected() throws Exception {
        CompositionDecision bogusParent = new CompositionDecision();
        bogusParent.setFamily("SPLIT_LAYOUT");
        bogusParent.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        bogusParent.setVariant("nonexistent_variant");
        bogusParent.setEligible(true);

        CompositionDecision child = syntheticEligibleDecision("GRID", "basic");

        boolean accepted = new CompositionEvaluator().assignSlot(bogusParent, "columns", child);

        assertTrue("direct-parent-invalid-variant-rejected: rejected despite parent eligible=true", !accepted);
        assertTrue("direct-parent-invalid-variant-rejected: reason recorded",
                bogusParent.getReasons().contains(
                        "slot_assignment_rejected:parent_invalid_variant:columns:SPLIT_LAYOUT:nonexistent_variant"));
    }

    /** Parent integrity: parent 쪽에 invalid parameter를 직접 심어도 재검증에서 거부된다. */
    private static void testDirectParentInvalidParameterRejected() throws Exception {
        CompositionDecision bogusParent = new CompositionDecision();
        bogusParent.setFamily("SPLIT_LAYOUT");
        bogusParent.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        bogusParent.getParameters().put("made_up_parameter", "x");
        bogusParent.setEligible(true);

        CompositionDecision child = syntheticEligibleDecision("GRID", "basic");

        boolean accepted = new CompositionEvaluator().assignSlot(bogusParent, "columns", child);

        assertTrue("direct-parent-invalid-parameter-rejected: rejected despite parent eligible=true", !accepted);
        assertTrue("direct-parent-invalid-parameter-rejected: reason recorded",
                bogusParent.getReasons().contains(
                        "slot_assignment_rejected:parent_invalid_parameter:columns:SPLIT_LAYOUT:made_up_parameter"));
    }

    // ---- fixture 생성 도우미 ----

    /**
     * SOURCE_SEMANTIC origin이 표시된 eligible decision을 {@link CompositionEvaluator#evaluate}
     * 실제 경로로만 만든다 -- direct construct로 origin 검증을 우회하지 않는다.
     */
    private static CompositionDecision syntheticEligibleDecision(String family, String variant) {
        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType(family);
        result.setRecommendedTemplateFamily(family);
        result.setRecommendedVariant(variant);
        result.setConfidence("HIGH");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);
        if (!decision.isEligible()) {
            assertTrue("syntheticEligibleDecision[" + family + "/" + variant + "]: must be eligible via evaluate()",
                    false);
        }
        return decision;
    }

    private static SemanticRegionResult firstByType(List<SemanticRegionResult> results, String semanticType) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                return r;
            }
        }
        assertTrue("firstByType: " + semanticType + " present", false);
        return null;
    }

    private static Element newDiv(Document doc, String id, Integer width) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        if (width != null) {
            div.setAttribute("width", String.valueOf(width));
        }
        return div;
    }

    private static Element newDivWithGeometry(Document doc, String id, double left, double top, double width, double height) {
        Element div = newDiv(doc, id, null);
        el_setGeometry(div, left, top, width, height);
        return div;
    }

    private static void el_setGeometry(Element el, double left, double top, double width, double height) {
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
