package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

/**
 * {@code SemanticRegionResult -> TemplateFamilyCatalog -> CompositionDecision} 최소 골격을
 * 검증하는 오프라인 unit test. WebSquare XML은 만들지 않으며, 이 테스트도 그것을 검증하지 않는다.
 */
public class CompositionFoundationTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCatalogFamilyCounts();

        testGridEligible();
        testTabControlEligible();
        testTabControlTabCountFlowsThroughExistingValidation();
        testSearchAreaEligible();
        testBusinessTableEligible();
        testTitleBarEligible();
        testButtonGroupEligible();
        testButtonGroupTitleBarAttachedPositionExcluded();
        testButtonGroupPositionTamperRejected();
        testButtonGroupPositionTargetSyntheticNotBlockedBySourceGate();
        testSplitLayoutHighEligible();
        testSplitLayoutLowFallbackPreserved();

        testUnknownFamilyNotCorrected();
        testUnknownVariantNotCorrected();
        testUnknownParameterNotCorrected();
        testCategoryFilterLikeInputNeverEligible();
        testHoldFamilyNeverEligibleEvenIfSynthesized();

        testPagingTargetInvariant();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** catalog audit: families=13, target_family_status 분포(confirmed=10/candidate=2/confirmed_target_only=1). */
    private static void testCatalogFamilyCounts() throws Exception {
        String[] allFamilies = {
                "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "PAGING", "TAB_CONTROL",
                "TREEVIEW", "BUTTON_GROUP", "SPLIT_LAYOUT", "AGREEMENT_LIST", "CATEGORY_FILTER",
                "INFOBOX", "LOADING_INDICATOR"
        };
        int confirmed = 0;
        int candidate = 0;
        int confirmedTargetOnly = 0;
        int acceptable = 0;
        int hold = 0;
        int notApplicable = 0;
        for (String family : allFamilies) {
            TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
            assertTrue("catalog-audit: " + family + " is known", def != null);
            switch (def.getTargetFamilyStatus()) {
                case CONFIRMED: confirmed++; break;
                case CANDIDATE_INSUFFICIENT_EVIDENCE: candidate++; break;
                case CONFIRMED_TARGET_ONLY: confirmedTargetOnly++; break;
            }
            switch (def.getSourcePredicateStatus()) {
                case ACCEPTABLE: acceptable++; break;
                case HOLD: hold++; break;
                case NOT_APPLICABLE: notApplicable++; break;
            }
        }
        assertEquals("catalog-audit: total families", "13", String.valueOf(allFamilies.length));
        assertEquals("catalog-audit: target_family_status=CONFIRMED count", "10", String.valueOf(confirmed));
        assertEquals("catalog-audit: target_family_status=CANDIDATE_INSUFFICIENT_EVIDENCE count", "2", String.valueOf(candidate));
        assertEquals("catalog-audit: target_family_status=CONFIRMED_TARGET_ONLY count", "1", String.valueOf(confirmedTargetOnly));
        assertEquals("catalog-audit: source_predicate_status=ACCEPTABLE count", "7", String.valueOf(acceptable));
        assertEquals("catalog-audit: source_predicate_status=HOLD count", "5", String.valueOf(hold));
        assertEquals("catalog-audit: source_predicate_status=NOT_APPLICABLE count", "1", String.valueOf(notApplicable));
    }

    private static void testGridEligible() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grd1");
        doc.appendChild(grid);

        SemanticRegionResult result = firstResultOf(new SemanticRegionSegmenter().segment(grid));
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("grid-eligible: decision eligible", decision.isEligible());
        assertEquals("grid-eligible: family", "GRID", decision.getFamily());
        assertEquals("grid-eligible: variant", "basic", decision.getVariant());
        assertEquals("grid-eligible: confidence", "HIGH", decision.getConfidence());
        assertEquals("grid-eligible: reasons empty", "0", String.valueOf(decision.getReasons().size()));
    }

    private static void testTabControlEligible() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        doc.appendChild(tab);

        SemanticRegionResult result = firstResultOf(new SemanticRegionSegmenter().segment(tab));
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("tab-control-eligible: decision eligible", decision.isEligible());
        assertEquals("tab-control-eligible: family", "TAB_CONTROL", decision.getFamily());
        assertEquals("tab-control-eligible: variant", "basic", decision.getVariant());
        // tab_count는 TemplateFamilyCatalog의 TAB_CONTROL parameter 목록에 있어 기존
        // evaluate() 경로를 그대로 통과한다. direct Tabpage가 없으므로 0이어야 한다.
        assertEquals("tab-control-eligible: tab_count parameter present via existing validation path",
                "0", String.valueOf(decision.getParameters().get("tab_count")));
        assertEquals("tab-control-eligible: reasons empty (tab_count not rejected as unknown_parameter)",
                "0", String.valueOf(decision.getReasons().size()));
    }

    /** direct Tabpage 3개가 있는 TAB_CONTROL이 evaluate()를 거쳐도 tab_count=3이 그대로
     * decision.getParameters()에 남고 eligible/reasons에 영향을 주지 않는다. */
    private static void testTabControlTabCountFlowsThroughExistingValidation() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        for (int i = 0; i < 3; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            tab.appendChild(page);
        }
        doc.appendChild(tab);

        SemanticRegionResult result = firstResultOf(new SemanticRegionSegmenter().segment(tab));
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("tab-count-through-validation: decision eligible", decision.isEligible());
        assertEquals("tab-count-through-validation: tab_count", "3",
                String.valueOf(decision.getParameters().get("tab_count")));
        assertEquals("tab-count-through-validation: reasons empty",
                "0", String.valueOf(decision.getReasons().size()));
    }

    private static void testSearchAreaEligible() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", null);
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "resultGrid");
        parent.appendChild(container);
        parent.appendChild(grid);

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "SEARCH_AREA");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("search-area-eligible: decision eligible", decision.isEligible());
        assertEquals("search-area-eligible: family", "SEARCH_AREA", decision.getFamily());
        assertEquals("search-area-eligible: variant", "basic", decision.getVariant());
        assertTrue("search-area-eligible: column_count parameter preserved",
                decision.getParameters().containsKey("column_count"));
    }

    private static void testBusinessTableEligible() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", null);
        Element container = newDiv(doc, "grid2Rows", null);
        appendLabelControlPair(doc, container, "label1", "Edit", 0, 0);
        appendLabelControlPair(doc, container, "label2", "Edit", 200, 0);
        appendLabelControlPair(doc, container, "label3", "Edit", 0, 30);
        appendLabelControlPair(doc, container, "label4", "Edit", 200, 30);
        parent.appendChild(container);

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "BUSINESS_TABLE");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("business-table-eligible: decision eligible", decision.isEligible());
        assertEquals("business-table-eligible: family", "BUSINESS_TABLE", decision.getFamily());
        assertEquals("business-table-eligible: variant", "horizontal", decision.getVariant());
    }

    private static void testTitleBarEligible() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "titleBar", 400);
        container.appendChild(newElement(doc, "Static", "titleLabel", 0, 200, 20));

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(container), "TITLE_BAR");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("title-bar-eligible: decision eligible", decision.isEligible());
        assertEquals("title-bar-eligible: family", "TITLE_BAR", decision.getFamily());
        assertEquals("title-bar-eligible: variant", "title_only", decision.getVariant());
    }

    private static void testButtonGroupEligible() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "actions", 200);
        container.appendChild(newElement(doc, "Button", "btn1", 0, 80, 20));
        container.appendChild(newElement(doc, "Button", "btn2", 10, 60, 20));

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(container), "BUTTON_GROUP");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("button-group-eligible: decision eligible", decision.isEligible());
        assertEquals("button-group-eligible: family", "BUTTON_GROUP", decision.getFamily());
        assertEquals("button-group-eligible: variant", "standalone", decision.getVariant());
        // SOURCE_SEMANTIC + BUTTON_GROUP + position 조합은 target으로 투영 불가로 알려져 있다.
        // decision.getParameters()에 이 값이 들어가면 안 되지만 family 전체는 eligible이어야 한다.
        assertTrue("button-group-eligible: source position value present on SemanticRegionResult",
                result.getParameters().get("position") != null);
        assertTrue("button-group-eligible: target-invalid position NOT copied into decision.getParameters()",
                !decision.getParameters().containsKey("position"));
        assertTrue("button-group-eligible: source_projection_not_supported reason recorded",
                containsReasonPrefix(decision, "source_projection_not_supported:BUTTON_GROUP:position:"));
    }

    /** title_bar_attached variant에서도 동일한 domain mismatch가 excluded되는지 확인. */
    private static void testButtonGroupTitleBarAttachedPositionExcluded() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", null);
        Element titleBar = newDiv(doc, "titleBar", null);
        titleBar.appendChild(newElement(doc, "Static", "lbl", 0, 100, 20));
        Element buttonGroup = newDiv(doc, "actions", 200);
        buttonGroup.appendChild(newElement(doc, "Button", "btn1", 0, 80, 20));
        parent.appendChild(titleBar);
        parent.appendChild(buttonGroup);

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "BUTTON_GROUP");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("button-group-title-bar-attached: decision eligible", decision.isEligible());
        assertEquals("button-group-title-bar-attached: variant", "title_bar_attached", decision.getVariant());
        assertTrue("button-group-title-bar-attached: target-invalid position NOT copied",
                !decision.getParameters().containsKey("position"));
    }

    /** tamper 방어: evaluate()를 우회해 decision.getParameters()에 직접 target-invalid 값을
     * 주입하면 decisionIntegrityFailureReason이 명시적으로 거부해야 한다. */
    private static void testButtonGroupPositionTamperRejected() throws Exception {
        CompositionEvaluator evaluator = new CompositionEvaluator();

        // SOURCE_SEMANTIC + BUTTON_GROUP + position 조합은 값이 무엇이든(target-valid여도)
        // source_projection_not_supported로 거부되어야 한다 -- source evidence 없이는 안 된다.
        String[] tamperedValues = {"left_buttons", "right_buttons", "top", "bottom", "sideways", null};
        for (String value : tamperedValues) {
            CompositionDecision tampered = freshButtonGroupDecision(CompositionDecision.Origin.SOURCE_SEMANTIC);
            tampered.getParameters().put("position", value);
            String reason = evaluator.decisionIntegrityFailureReason(tampered, "test", "");
            assertTrue("tamper-SOURCE_SEMANTIC-position=" + value + ": rejected", reason != null);
            assertTrue("tamper-SOURCE_SEMANTIC-position=" + value + ": reason mentions source_projection_not_supported",
                    reason.contains("source_projection_not_supported") && reason.contains("BUTTON_GROUP")
                            && reason.contains("position"));
        }
    }

    /** source projection gate는 origin==SOURCE_SEMANTIC에만 적용된다. TARGET_SYNTHETIC
     * decision에 target-valid top/bottom이 있으면 이 gate로 거부되지 않아야 하지만, target
     * 값-도메인 검사는 여전히 적용된다. */
    private static void testButtonGroupPositionTargetSyntheticNotBlockedBySourceGate() throws Exception {
        CompositionEvaluator evaluator = new CompositionEvaluator();

        CompositionDecision synthetic = freshButtonGroupDecision(CompositionDecision.Origin.TARGET_SYNTHETIC);
        synthetic.getParameters().put("position", "top");
        String reason = evaluator.decisionIntegrityFailureReason(synthetic, "test", "");
        assertTrue("target-synthetic-position=top: source projection gate does NOT apply", reason == null);

        CompositionDecision syntheticBottom = freshButtonGroupDecision(CompositionDecision.Origin.TARGET_SYNTHETIC);
        syntheticBottom.getParameters().put("position", "bottom");
        String reasonBottom = evaluator.decisionIntegrityFailureReason(syntheticBottom, "test", "");
        assertTrue("target-synthetic-position=bottom: source projection gate does NOT apply", reasonBottom == null);

        // target 값-도메인 검사는 origin과 무관하게 여전히 적용된다 -- TARGET_SYNTHETIC라도
        // target-invalid 값(left_buttons)은 여전히 거부되어야 한다.
        CompositionDecision syntheticInvalid = freshButtonGroupDecision(CompositionDecision.Origin.TARGET_SYNTHETIC);
        syntheticInvalid.getParameters().put("position", "left_buttons");
        String reasonInvalid = evaluator.decisionIntegrityFailureReason(syntheticInvalid, "test", "");
        assertTrue("target-synthetic-position=left_buttons: still rejected by target value-domain check",
                reasonInvalid != null && reasonInvalid.contains("invalid_parameter_value"));
    }

    private static CompositionDecision freshButtonGroupDecision(CompositionDecision.Origin origin) {
        CompositionDecision decision = new CompositionDecision();
        decision.setOrigin(origin);
        decision.setFamily("BUTTON_GROUP");
        return decision;
    }

    private static boolean containsReasonPrefix(CompositionDecision decision, String prefix) {
        for (String reason : decision.getReasons()) {
            if (reason.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void testSplitLayoutHighEligible() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "splitParent", null);
        parent.appendChild(newDivWithGeometry(doc, "col30", 0, 0, 300, 200));
        parent.appendChild(newDivWithGeometry(doc, "col70", 300, 0, 700, 200));

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "SPLIT_LAYOUT");
        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("split-layout-high-eligible: decision eligible", decision.isEligible());
        assertEquals("split-layout-high-eligible: family", "SPLIT_LAYOUT", decision.getFamily());
        assertEquals("split-layout-high-eligible: variant", "ratio_split", decision.getVariant());
        assertTrue("split-layout-high-eligible: column_ratio parameter preserved",
                decision.getParameters().containsKey("column_ratio"));
    }

    /** SPLIT_LAYOUT LOW/FIXED_WIDTH_FALLBACK -- canonical ratio_split variant로 강제 승격하지
     * 않고 fallback을 보존한 채로도 여전히 eligible한 candidate로 남아야 한다. */
    private static void testSplitLayoutLowFallbackPreserved() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "splitParentNonExact", null);
        parent.appendChild(newDivWithGeometry(doc, "colA", 0, 0, 296, 200));
        parent.appendChild(newDivWithGeometry(doc, "colB", 296, 0, 704, 200));

        SemanticRegionResult result = firstByType(new SemanticRegionSegmenter().segment(parent), "SPLIT_LAYOUT");
        assertTrue("split-layout-low-fallback: source result confidence LOW", "LOW".equals(result.getConfidence()));
        assertTrue("split-layout-low-fallback: source result has no recommendedVariant",
                result.getRecommendedVariant() == null);

        CompositionDecision decision = new CompositionEvaluator().evaluate(result);

        assertTrue("split-layout-low-fallback: decision still eligible (fallback path, not an error)",
                decision.isEligible());
        assertEquals("split-layout-low-fallback: family", "SPLIT_LAYOUT", decision.getFamily());
        assertTrue("split-layout-low-fallback: variant NOT forced to ratio_split (stays null)",
                decision.getVariant() == null);
        assertEquals("split-layout-low-fallback: fallback preserved", "FIXED_WIDTH_FALLBACK", decision.getFallback());
        assertTrue("split-layout-low-fallback: reasons record the fallback",
                decision.getReasons().contains("fallback_preserved:FIXED_WIDTH_FALLBACK"));
        assertTrue("split-layout-low-fallback: reasons record LOW confidence",
                decision.getReasons().contains("low_confidence_no_canonical_rewrite"));
        assertTrue("split-layout-low-fallback: no column_ratio parameter fabricated",
                !decision.getParameters().containsKey("column_ratio"));
    }

    /** 완전히 존재하지 않는 family -- ineligible, 추측 보정 없이 그대로 실패로 기록. */
    private static void testUnknownFamilyNotCorrected() throws Exception {
        SemanticRegionResult fake = new SemanticRegionResult();
        fake.setSemanticType("NOT_A_REAL_FAMILY");
        fake.setRecommendedTemplateFamily("NOT_A_REAL_FAMILY");
        fake.setConfidence("HIGH");

        CompositionDecision decision = new CompositionEvaluator().evaluate(fake);

        assertTrue("unknown-family: ineligible", !decision.isEligible());
        assertTrue("unknown-family: reason recorded",
                decision.getReasons().contains("unknown_family:NOT_A_REAL_FAMILY"));
        assertTrue("unknown-family: family echoed as-is (not corrected)",
                "NOT_A_REAL_FAMILY".equals(decision.getFamily()));
    }

    /** 실제 존재하는 family + catalog에 없는 variant -- ineligible, variant를 가장 가까운 값으로 보정하지 않음. */
    private static void testUnknownVariantNotCorrected() throws Exception {
        SemanticRegionResult fake = new SemanticRegionResult();
        fake.setSemanticType("SEARCH_AREA");
        fake.setRecommendedTemplateFamily("SEARCH_AREA");
        fake.setRecommendedVariant("nonexistent_variant_xyz");
        fake.setConfidence("HIGH");

        CompositionDecision decision = new CompositionEvaluator().evaluate(fake);

        assertTrue("unknown-variant: ineligible", !decision.isEligible());
        assertTrue("unknown-variant: reason recorded",
                decision.getReasons().contains("unknown_variant:SEARCH_AREA:nonexistent_variant_xyz"));
        assertTrue("unknown-variant: decision variant stays unset (not guess-corrected to e.g. \"basic\")",
                decision.getVariant() == null);
    }

    /** 실제 family/variant + catalog에 없는 parameter key -- ineligible, 그 parameter를 옮기지 않음. */
    private static void testUnknownParameterNotCorrected() throws Exception {
        SemanticRegionResult fake = new SemanticRegionResult();
        fake.setSemanticType("GRID");
        fake.setRecommendedTemplateFamily("GRID");
        fake.setRecommendedVariant("basic");
        fake.setConfidence("HIGH");
        fake.getParameters().put("column_count", 5); // 유효
        fake.getParameters().put("made_up_parameter", "x"); // 무효

        CompositionDecision decision = new CompositionEvaluator().evaluate(fake);

        assertTrue("unknown-parameter: ineligible", !decision.isEligible());
        assertTrue("unknown-parameter: reason recorded",
                decision.getReasons().contains("unknown_parameter:GRID:made_up_parameter"));
        assertTrue("unknown-parameter: valid parameter still preserved",
                decision.getParameters().containsKey("column_count"));
        assertTrue("unknown-parameter: invalid parameter NOT copied",
                !decision.getParameters().containsKey("made_up_parameter"));
    }

    /** 실제 Segmenter는 CATEGORY_FILTER를 절대 발행하지 않지만, 만에 하나 이런 semantic input이
     * 주어지더라도 CompositionEvaluator가 독립적으로(defense-in-depth) 거부해야 한다. */
    private static void testCategoryFilterLikeInputNeverEligible() throws Exception {
        SemanticRegionResult fake = new SemanticRegionResult();
        fake.setSemanticType("CATEGORY_FILTER");
        fake.setRecommendedTemplateFamily("CATEGORY_FILTER");
        fake.setRecommendedVariant("basic");
        fake.setConfidence("LOW");

        CompositionDecision decision = new CompositionEvaluator().evaluate(fake);

        assertTrue("category-filter-like: never eligible", !decision.isEligible());
        assertTrue("category-filter-like: reason cites HOLD source_predicate_status",
                decision.getReasons().contains("family_not_source_acceptable:CATEGORY_FILTER:HOLD"));
    }

    /** 나머지 HOLD family(TREEVIEW/AGREEMENT_LIST/INFOBOX)/NOT_APPLICABLE(LOADING_INDICATOR)도 동일하게 거부되는지 재확인. */
    private static void testHoldFamilyNeverEligibleEvenIfSynthesized() throws Exception {
        String[] holdFamilies = {"TREEVIEW", "AGREEMENT_LIST", "INFOBOX"};
        for (String family : holdFamilies) {
            SemanticRegionResult fake = new SemanticRegionResult();
            fake.setSemanticType(family);
            fake.setRecommendedTemplateFamily(family);
            fake.setConfidence("MEDIUM");
            CompositionDecision decision = new CompositionEvaluator().evaluate(fake);
            assertTrue("hold-family-never-eligible[" + family + "]: ineligible", !decision.isEligible());
        }

        SemanticRegionResult loadingIndicator = new SemanticRegionResult();
        loadingIndicator.setSemanticType("LOADING_INDICATOR");
        loadingIndicator.setRecommendedTemplateFamily("LOADING_INDICATOR");
        loadingIndicator.setConfidence("HIGH");
        CompositionDecision decision = new CompositionEvaluator().evaluate(loadingIndicator);
        assertTrue("hold-family-never-eligible[LOADING_INDICATOR/NOT_APPLICABLE]: ineligible", !decision.isEligible());
    }

    /** GRID.paging slot이 PAGING family와 target측에서 관계를 가진다는 사실을 오직 catalog
     * 데이터로만 검증한다(source evidence 아님). production Segmenter가 PAGING을 절대 발행하지
     * 않음(source emission = 0)도 재확인한다. */
    private static void testPagingTargetInvariant() throws Exception {
        TemplateFamilyCatalog.FamilyDefinition gridDef = TemplateFamilyCatalog.get("GRID");
        assertTrue("paging-target-invariant: GRID.paging slot declared in catalog", gridDef.hasSlot("paging"));

        TemplateFamilyCatalog.FamilyDefinition pagingDef = TemplateFamilyCatalog.get("PAGING");
        assertTrue("paging-target-invariant: PAGING family known to catalog", pagingDef != null);
        assertTrue("paging-target-invariant: PAGING target_family_status=CONFIRMED",
                pagingDef.getTargetFamilyStatus() == TemplateFamilyCatalog.TargetFamilyStatus.CONFIRMED);
        assertTrue("paging-target-invariant: PAGING source_predicate_status=HOLD",
                pagingDef.getSourcePredicateStatus() == TemplateFamilyCatalog.SourcePredicateStatus.HOLD);

        // synthetic target-composition input(source evidence 아님) -- HOLD이므로 여전히 ineligible이어야 한다.
        SemanticRegionResult syntheticPaging = new SemanticRegionResult();
        syntheticPaging.setSemanticType("PAGING");
        syntheticPaging.setRecommendedTemplateFamily("PAGING");
        syntheticPaging.getParameters().put("page_size", 10);
        CompositionDecision decision = new CompositionEvaluator().evaluate(syntheticPaging);
        assertTrue("paging-target-invariant: synthetic PAGING decision still ineligible for source composition",
                !decision.isEligible());
        assertTrue("paging-target-invariant: family gate short-circuits before parameter copy (parameters stay empty)",
                decision.getParameters().isEmpty());

        // 실제 production Segmenter는 어떤 fixture에서도 PAGING을 발행하지 않는다(source emission = 0).
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grdPagingCheck");
        doc.appendChild(grid);
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(grid);
        for (SemanticRegionResult r : results) {
            assertTrue("paging-target-invariant: real Segmenter never emits PAGING",
                    !"PAGING".equals(r.getSemanticType()));
        }
    }

    // ---- fixture 생성 도우미 ----

    private static SemanticRegionResult firstResultOf(List<SemanticRegionResult> results) {
        assertTrue("firstResultOf: at least one result", !results.isEmpty());
        return results.get(0);
    }

    private static SemanticRegionResult firstByType(List<SemanticRegionResult> results, String semanticType) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                return r;
            }
        }
        assertTrue("firstByType: " + semanticType + " present in results", false);
        return null;
    }

    private static Element newLabelControlPairsRow(Document doc, String id, double topOffset) {
        Element container = newDiv(doc, id, null);
        appendLabelControlPair(doc, container, id + "_label1", "Edit", 0, topOffset);
        appendLabelControlPair(doc, container, id + "_label2", "Edit", 200, topOffset);
        return container;
    }

    private static void appendLabelControlPair(
            Document doc, Element container, String idPrefix, String controlTag, double left, double top) {
        Element label = doc.createElement("Static");
        label.setAttribute("id", idPrefix + "_static");
        setGeometry(label, left, top, 50, 20);
        Element control = doc.createElement(controlTag);
        control.setAttribute("id", idPrefix + "_" + controlTag.toLowerCase());
        setGeometry(control, left + 50, top, 150, 20);
        container.appendChild(label);
        container.appendChild(control);
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
        setGeometry(div, left, top, width, height);
        return div;
    }

    private static Element newElement(Document doc, String tag, String id, double left, double width, double height) {
        Element el = doc.createElement(tag);
        el.setAttribute("id", id);
        setGeometry(el, left, 0, width, height);
        return el;
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
