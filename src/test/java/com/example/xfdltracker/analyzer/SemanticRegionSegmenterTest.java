package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.binding.SourceBindingAnalyzer;
import com.example.xfdltracker.binding.SourceBindingReference;
import com.example.xfdltracker.composition.CompositionDecision;
import com.example.xfdltracker.composition.CompositionEvaluator;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetCompositionPlanBuilder;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadCategory;
import com.example.xfdltracker.payload.TargetPayloadExtractor;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 외부 의존성 없는(no JUnit) offline 단위 테스트다. GRID/TAB_CONTROL/SPLIT_LAYOUT Shadow Segmenter
 * 범위를 검증한다. 실제 XPlatform source 태그 이름을 그대로 사용한다(가상 태그 없음).
 */
public class SemanticRegionSegmenterTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testGridComponentDetected();
        testGridColumnParameterExtraction();
        testGridParametersAbsentWhenNoFormat();
        testTabControlComponentDetected();
        testTabControlTabCountZeroWhenNoTabpage();
        testTabControlTabCountSingleDirectTabpage();
        testTabControlTabCountMultipleDirectTabpages();
        testTabControlTabCountViaTabpagesWrapperOneLevelOnly();
        testTabControlTabCountMatchesTabLabelEvidenceCount();
        testNestedTabScopeNotMixed();
        testNestedTabScopeTabCountNotMixed();
        testPlainComponentsNotMisdetected();

        testSplitLayoutExactRatioRegion();
        testSplitLayoutExactThirdsRegion();
        testSplitLayoutUncalibratedNonExactFallback();
        testSplitLayoutGapNoEmission();
        testSplitLayoutOverlapNoEmission();
        testSplitLayoutColumnSourceOrderPreserved();
        testSplitLayoutReversedThirdsSourceOrderPreserved();
        testSplitLayoutUnrelatedSiblingsDoNotForceRegion();
        testSplitLayoutNonContainerControlsNotForcedIntoRegion();

        testButtonGroupEveryFlattenedButtonHasExactlyOneSemanticButtonEvidenceItem();
        testButtonGroupSegmenterToExtractorIntegrationCrossesLossBoundary();

        testTabPageMembershipSinglePage();
        testTabPageMembershipMultiPageZeroBasedOrder();
        testTabPageMembershipExactContainingStructuralId();
        testTabPageMembershipRegionOutsideTabControlIsNull();
        testTabPageMembershipNestedTabControlCarriesOuterMembership();
        testTabPageMembershipNestedDescendantUsesNearestInnerMembership();
        testTabPageMembershipSameOrdinalUnderDifferentParentsUnambiguous();

        testPredicateAnalysisCacheAnalyzesEachElementAtMostOncePerPass();
        testPredicateAnalysisCacheThreadsSameAnalysisObjectThroughoutPass();
        testPredicateAnalysisCacheIsFreshPerIndependentSegmentInvocation();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** GRID component -> GRID/HIGH/basic, 정확히 1건. */
    private static void testGridComponentDetected() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grd1");
        doc.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(grid);

        assertEquals("grid: result count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("grid: semanticType", "GRID", r.getSemanticType());
        assertEquals("grid: recommendedTemplateFamily", "GRID", r.getRecommendedTemplateFamily());
        assertEquals("grid: recommendedVariant", "basic", r.getRecommendedVariant());
        assertEquals("grid: confidence", "HIGH", r.getConfidence());
    }

    /** head Band Cell 5개 -> column_count/column_width가 family 판정과 독립적으로 정확히 추출된다. */
    private static void testGridColumnParameterExtraction() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grd2");

        Element formats = doc.createElement("Formats");
        Element format = doc.createElement("Format");
        format.setAttribute("id", "default");

        Element columns = doc.createElement("Columns");
        int[] sizes = {80, 120, 100, 150, 90};
        for (int size : sizes) {
            Element column = doc.createElement("Column");
            column.setAttribute("size", String.valueOf(size));
            columns.appendChild(column);
        }
        format.appendChild(columns);

        Element headBand = doc.createElement("Band");
        headBand.setAttribute("id", "head");
        for (int i = 0; i < sizes.length; i++) {
            Element cell = doc.createElement("Cell");
            cell.setAttribute("col", String.valueOf(i));
            cell.setAttribute("row", "0");
            headBand.appendChild(cell);
        }
        format.appendChild(headBand);

        formats.appendChild(format);
        grid.appendChild(formats);
        doc.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(grid);

        assertEquals("grid-column-parameter-extraction: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("grid-column-parameter-extraction: family unchanged", "GRID", r.getSemanticType());
        assertEquals("grid-column-parameter-extraction: confidence unchanged", "HIGH", r.getConfidence());
        assertEquals("grid-column-parameter-extraction: variant unchanged", "basic", r.getRecommendedVariant());
        assertEquals("grid-column-parameter-extraction: column_count",
                "5", String.valueOf(r.getParameters().get("column_count")));
        Object widthObj = r.getParameters().get("column_width");
        assertTrue("grid-column-parameter-extraction: column_width present", widthObj instanceof String[]);
        String[] widths = (String[]) widthObj;
        assertEquals("grid-column-parameter-extraction: column_width length", "5", String.valueOf(widths.length));
        assertEquals("grid-column-parameter-extraction: column_width values, in order, no rounding",
                "[80, 120, 100, 150, 90]", Arrays.toString(widths));
    }

    /** Format/Formats 없는 Grid -> column_count/column_width를 추측해 채우지 않는다. */
    private static void testGridParametersAbsentWhenNoFormat() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grd3");
        doc.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(grid);

        assertEquals("grid-no-format: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("grid-no-format: family unchanged", "GRID", r.getSemanticType());
        assertTrue("grid-no-format: no column_count guessed", !r.getParameters().containsKey("column_count"));
        assertTrue("grid-no-format: no column_width guessed", !r.getParameters().containsKey("column_width"));
    }

    /** TAB_CONTROL component -> TAB_CONTROL/HIGH/basic, 정확히 1건. */
    private static void testTabControlComponentDetected() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);

        assertEquals("tab: result count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("tab: semanticType", "TAB_CONTROL", r.getSemanticType());
        assertEquals("tab: recommendedTemplateFamily", "TAB_CONTROL", r.getRecommendedTemplateFamily());
        assertEquals("tab: recommendedVariant", "basic", r.getRecommendedVariant());
        assertEquals("tab: confidence", "HIGH", r.getConfidence());
    }

    /** direct Tabpage가 없으면 tab_count는 0이어야 한다(추측 없음). */
    private static void testTabControlTabCountZeroWhenNoTabpage() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabNoPages");
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult r = results.get(0);
        assertEquals("tab-count-zero: tab_count", "0", String.valueOf(r.getParameters().get("tab_count")));
    }

    /** direct Tabpage 1개 -> tab_count=1. */
    private static void testTabControlTabCountSingleDirectTabpage() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        Element page = doc.createElement("Tabpage");
        page.setAttribute("id", "p1");
        tab.appendChild(page);
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult r = results.get(0);
        assertEquals("tab-count-single: tab_count", "1", String.valueOf(r.getParameters().get("tab_count")));
    }

    /** direct Tabpage 4개(순서 그대로 나열) -> tab_count=4(exact, 반올림/추측 없음). */
    private static void testTabControlTabCountMultipleDirectTabpages() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        for (int i = 0; i < 4; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            tab.appendChild(page);
        }
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult r = results.get(0);
        assertEquals("tab-count-multiple: tab_count", "4", String.valueOf(r.getParameters().get("tab_count")));
    }

    /** {@code directTabpages}는 직계 Tabpages wrapper 한 단계 안의 Tabpage도 센다. */
    private static void testTabControlTabCountViaTabpagesWrapperOneLevelOnly() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        Element tabpages = doc.createElement("Tabpages");
        for (int i = 0; i < 3; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            tabpages.appendChild(page);
        }
        tab.appendChild(tabpages);
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult r = results.get(0);
        assertEquals("tab-count-wrapper: tab_count", "3", String.valueOf(r.getParameters().get("tab_count")));
    }

    /** tab_count는 tab_label evidence 개수와 항상 정확히 일치해야 한다. */
    private static void testTabControlTabCountMatchesTabLabelEvidenceCount() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        for (int i = 0; i < 5; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            tab.appendChild(page);
        }
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult r = results.get(0);
        int evidenceCount = 0;
        for (com.example.xfdltracker.semantic.SourcePayloadEvidenceItem item : r.getPayloadEvidence()) {
            if ("tab_label".equals(item.getEvidenceRole())) {
                evidenceCount++;
            }
        }
        assertEquals("tab-count-matches-evidence: tab_label evidence count", "5", String.valueOf(evidenceCount));
        assertEquals("tab-count-matches-evidence: tab_count parameter",
                String.valueOf(evidenceCount), String.valueOf(r.getParameters().get("tab_count")));
    }

    /** nested Tab: outer/inner 두 인스턴스가 서로 다른 결과로 분리되고 섞이지 않음을 확인한다. */
    private static void testNestedTabScopeNotMixed() throws Exception {
        Document doc = newDocument();
        Element outerTab = doc.createElement("Tab");
        outerTab.setAttribute("id", "outerTab");
        Element tabpages = doc.createElement("Tabpages");
        Element outerPage = doc.createElement("Tabpage");
        outerPage.setAttribute("id", "outerPage1");
        Element innerTab = doc.createElement("Tab");
        innerTab.setAttribute("id", "innerTab");
        Element innerPage = doc.createElement("Tabpage");
        innerPage.setAttribute("id", "innerPage1");
        Element leaf = doc.createElement("Static");
        leaf.setAttribute("id", "s1");

        innerPage.appendChild(leaf);
        innerTab.appendChild(innerPage);
        outerPage.appendChild(innerTab);
        tabpages.appendChild(outerPage);
        outerTab.appendChild(tabpages);
        doc.appendChild(outerTab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(outerTab);

        assertEquals("nested-tab: result count", "2", String.valueOf(results.size()));

        SemanticRegionResult outer = results.get(0);
        SemanticRegionResult inner = results.get(1);
        assertEquals("nested-tab: outer semanticType", "TAB_CONTROL", outer.getSemanticType());
        assertEquals("nested-tab: inner semanticType", "TAB_CONTROL", inner.getSemanticType());

        assertTrue("nested-tab: outer result carries its own source_id",
                containsEvidence(outer, "source_id=outerTab"));
        assertFalse("nested-tab: outer result does NOT carry inner source_id (no scope mixing)",
                containsEvidence(outer, "source_id=innerTab"));
        assertTrue("nested-tab: inner result carries its own source_id",
                containsEvidence(inner, "source_id=innerTab"));
        assertFalse("nested-tab: inner result does NOT carry outer source_id (no scope mixing)",
                containsEvidence(inner, "source_id=outerTab"));
    }

    /** outer Tab의 tab_count는 자신의 direct Tabpage만 반영하고 inner Tab의 개수와 합산되지 않는다. */
    private static void testNestedTabScopeTabCountNotMixed() throws Exception {
        Document doc = newDocument();
        Element outerTab = doc.createElement("Tab");
        outerTab.setAttribute("id", "outerTab");
        Element tabpages = doc.createElement("Tabpages");
        Element outerPage = doc.createElement("Tabpage");
        outerPage.setAttribute("id", "outerPage1");
        Element innerTab = doc.createElement("Tab");
        innerTab.setAttribute("id", "innerTab");
        Element innerPage1 = doc.createElement("Tabpage");
        innerPage1.setAttribute("id", "innerPage1");
        Element innerPage2 = doc.createElement("Tabpage");
        innerPage2.setAttribute("id", "innerPage2");

        innerTab.appendChild(innerPage1);
        innerTab.appendChild(innerPage2);
        outerPage.appendChild(innerTab);
        tabpages.appendChild(outerPage);
        outerTab.appendChild(tabpages);
        doc.appendChild(outerTab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(outerTab);
        SemanticRegionResult outer = results.get(0);
        SemanticRegionResult inner = results.get(1);

        assertEquals("nested-tab-count: outer tab_count is exactly its own 1 direct Tabpage",
                "1", String.valueOf(outer.getParameters().get("tab_count")));
        assertEquals("nested-tab-count: inner tab_count is exactly its own 2 direct Tabpages",
                "2", String.valueOf(inner.getParameters().get("tab_count")));
    }

    /** 일반 Div/Static/Button -> 오검출 없음(결과 0건). */
    private static void testPlainComponentsNotMisdetected() throws Exception {
        Document doc = newDocument();
        Element div = doc.createElement("Div");
        div.setAttribute("id", "d1");
        Element sta = doc.createElement("Static");
        sta.setAttribute("id", "s1");
        Element btn = doc.createElement("Button");
        btn.setAttribute("id", "b1");
        div.appendChild(sta);
        div.appendChild(btn);
        doc.appendChild(div);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(div);

        assertEquals("plain-components: result count", "0", String.valueOf(results.size()));
    }

    /** 정확히 30/70 비율인 sibling region -> SPLIT_LAYOUT HIGH ratio_split, column_ratio=[col_3,col_7]가 되어야 한다. */
    private static void testSplitLayoutExactRatioRegion() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colA = newDiv(doc, "colA", 0, 0, 300, 400);
        Element colB = newDiv(doc, "colB", 300, 0, 700, 400);
        parent.appendChild(colA);
        parent.appendChild(colB);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("exact-ratio-region: count", "1", String.valueOf(splitLayouts.size()));

        SemanticRegionResult r = splitLayouts.get(0);
        assertEquals("exact-ratio-region: semanticType", "SPLIT_LAYOUT", r.getSemanticType());
        assertEquals("exact-ratio-region: recommendedTemplateFamily", "SPLIT_LAYOUT", r.getRecommendedTemplateFamily());
        assertEquals("exact-ratio-region: recommendedVariant", "ratio_split", r.getRecommendedVariant());
        assertEquals("exact-ratio-region: confidence", "HIGH", r.getConfidence());
        assertEquals("exact-ratio-region: fallback", "null", String.valueOf(r.getFallback()));
        assertArrayEquals("exact-ratio-region: column_ratio",
                new String[] {"col_3", "col_7"}, (String[]) r.getParameters().get("column_ratio"));
    }

    /** 정확한 3등분 -> SPLIT_LAYOUT HIGH + column_ratio=[col_33,col_33,col_33]. */
    private static void testSplitLayoutExactThirdsRegion() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element col1 = newDiv(doc, "col1", 0, 0, 333, 400);
        Element col2 = newDiv(doc, "col2", 333, 0, 333, 400);
        Element col3 = newDiv(doc, "col3", 666, 0, 333, 400);
        parent.appendChild(col1);
        parent.appendChild(col2);
        parent.appendChild(col3);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("exact-thirds-region: count", "1", String.valueOf(splitLayouts.size()));

        SemanticRegionResult r = splitLayouts.get(0);
        assertEquals("exact-thirds-region: confidence", "HIGH", r.getConfidence());
        assertEquals("exact-thirds-region: recommendedVariant", "ratio_split", r.getRecommendedVariant());
        assertArrayEquals("exact-thirds-region: column_ratio",
                new String[] {"col_33", "col_33", "col_33"}, (String[]) r.getParameters().get("column_ratio"));
    }

    /** geometry는 성립하나 canonical과 일치하지 않는 비율 -> LOW + fallback=FIXED_WIDTH_FALLBACK. */
    private static void testSplitLayoutUncalibratedNonExactFallback() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colA = newDiv(doc, "colA", 0, 0, 296, 400);
        Element colB = newDiv(doc, "colB", 296, 0, 704, 400);
        parent.appendChild(colA);
        parent.appendChild(colB);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("uncalibrated-non-exact: count", "1", String.valueOf(splitLayouts.size()));

        SemanticRegionResult r = splitLayouts.get(0);
        assertEquals("uncalibrated-non-exact: semanticType", "SPLIT_LAYOUT", r.getSemanticType());
        assertEquals("uncalibrated-non-exact: confidence", "LOW", r.getConfidence());
        assertEquals("uncalibrated-non-exact: fallback", "FIXED_WIDTH_FALLBACK", r.getFallback());
        assertEquals("uncalibrated-non-exact: recommendedVariant NOT promoted to ratio_split",
                "null", String.valueOf(r.getRecommendedVariant()));
        assertEquals("uncalibrated-non-exact: no column_ratio parameter",
                "null", String.valueOf(r.getParameters().get("column_ratio")));
    }

    /** gap(overlap 아님) -> SPLIT_LAYOUT emit 없음. */
    private static void testSplitLayoutGapNoEmission() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colA = newDiv(doc, "colA", 0, 0, 300, 400);
        Element colB = newDiv(doc, "colB", 400, 0, 600, 400);
        parent.appendChild(colA);
        parent.appendChild(colB);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("gap-no-emission: count", "0", String.valueOf(splitLayouts.size()));
    }

    /** overlap -> SPLIT_LAYOUT emit 없음. */
    private static void testSplitLayoutOverlapNoEmission() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colA = newDiv(doc, "colA", 0, 0, 300, 400);
        Element colB = newDiv(doc, "colB", 250, 0, 700, 400);
        parent.appendChild(colA);
        parent.appendChild(colB);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("overlap-no-emission: count", "0", String.valueOf(splitLayouts.size()));
    }

    /** source DOM 순서가 기하학적 left 순서와 반대여도 column_ratio는 source DOM 순서를 따라야 한다. */
    private static void testSplitLayoutColumnSourceOrderPreserved() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colB = newDiv(doc, "colB", 300, 0, 700, 400); // 오른쪽 70% 영역
        Element colA = newDiv(doc, "colA", 0, 0, 300, 400);   // 왼쪽 30% 영역
        // DOM 순서(colB, colA)를 기하학적 left 순서와 반대로.
        parent.appendChild(colB);
        parent.appendChild(colA);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("column-order-preserved: count", "1", String.valueOf(splitLayouts.size()));

        SemanticRegionResult r = splitLayouts.get(0);
        assertArrayEquals("column-order-preserved: column_ratio aligned to source DOM order [right70,left30]",
                new String[] {"col_7", "col_3"}, (String[]) r.getParameters().get("column_ratio"));
        assertTrue("column-order-preserved: columns_source_order reflects actual DOM order",
                containsEvidence(r, "columns_source_order=colB,colA"));

        // 정상 DOM 순서([left30,right70])는 여전히 [col_3,col_7] 유지.
        Document doc2 = newDocument();
        Element parent2 = newDiv(doc2, "parent2", -1, -1, -1, -1);
        Element left30 = newDiv(doc2, "left30", 0, 0, 300, 400);
        Element right70 = newDiv(doc2, "right70", 300, 0, 700, 400);
        parent2.appendChild(left30);
        parent2.appendChild(right70);
        List<SemanticRegionResult> normalOrder = splitLayoutResultsOf(parent2);
        assertArrayEquals("column-order-preserved: normal DOM order [left30,right70] unaffected",
                new String[] {"col_3", "col_7"}, (String[]) normalOrder.get(0).getParameters().get("column_ratio"));
    }

    /** col_33 x3도 reversed DOM에서 columns_source_order가 실제 DOM 순서를 보존하는지 검증한다. */
    private static void testSplitLayoutReversedThirdsSourceOrderPreserved() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element col3 = newDiv(doc, "col3", 666, 0, 333, 400);
        Element col2 = newDiv(doc, "col2", 333, 0, 333, 400);
        Element col1 = newDiv(doc, "col1", 0, 0, 333, 400);
        // DOM 순서를 기하학적 left 순서와 반대로(col3, col2, col1).
        parent.appendChild(col3);
        parent.appendChild(col2);
        parent.appendChild(col1);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("reversed-thirds: count", "1", String.valueOf(splitLayouts.size()));

        SemanticRegionResult r = splitLayouts.get(0);
        assertArrayEquals("reversed-thirds: column_ratio still 3x col_33",
                new String[] {"col_33", "col_33", "col_33"}, (String[]) r.getParameters().get("column_ratio"));
        assertTrue("reversed-thirds: columns_source_order reflects actual (reversed) DOM order",
                containsEvidence(r, "columns_source_order=col3,col2,col1"));
    }

    /** geometry 없는 관련 없는 형제가 섞이면 partition이 깨져 SPLIT_LAYOUT을 만들지 않는다. */
    private static void testSplitLayoutUnrelatedSiblingsDoNotForceRegion() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element colA = newDiv(doc, "colA", 0, 0, 300, 400);
        Element colB = newDiv(doc, "colB", 300, 0, 700, 400);
        Element unrelated = doc.createElement("Static");
        unrelated.setAttribute("id", "unrelated");
        // unrelated는 left/top/width/height가 없는 진짜 무관한 형제.
        parent.appendChild(colA);
        parent.appendChild(colB);
        parent.appendChild(unrelated);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("unrelated-sibling-no-forced-region: count", "0", String.valueOf(splitLayouts.size()));
    }

    /** container 미등록 control(Edit) 2개가 완벽한 geometry를 가져도 SPLIT_LAYOUT으로 처리하지 않는다. */
    private static void testSplitLayoutNonContainerControlsNotForcedIntoRegion() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent", -1, -1, -1, -1);
        Element editA = doc.createElement("Edit");
        editA.setAttribute("id", "editA");
        editA.setAttribute("left", "0");
        editA.setAttribute("top", "0");
        editA.setAttribute("width", "300");
        editA.setAttribute("height", "400");
        Element editB = doc.createElement("Edit");
        editB.setAttribute("id", "editB");
        editB.setAttribute("left", "300");
        editB.setAttribute("top", "0");
        editB.setAttribute("width", "700");
        editB.setAttribute("height", "400");
        parent.appendChild(editA);
        parent.appendChild(editB);

        List<SemanticRegionResult> splitLayouts = splitLayoutResultsOf(parent);
        assertEquals("non-container-controls-no-forced-region: count", "0", String.valueOf(splitLayouts.size()));
    }

    private static List<SemanticRegionResult> splitLayoutResultsOf(Element root) {
        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(root);
        List<SemanticRegionResult> splitLayouts = new ArrayList<SemanticRegionResult>();
        for (SemanticRegionResult r : all) {
            if ("SPLIT_LAYOUT".equals(r.getSemanticType())) {
                splitLayouts.add(r);
            }
        }
        return splitLayouts;
    }

    private static Element newDiv(Document doc, String id, double left, double top, double width, double height) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        if (left >= 0) div.setAttribute("left", formatAttr(left));
        if (top >= 0) div.setAttribute("top", formatAttr(top));
        if (width >= 0) div.setAttribute("width", formatAttr(width));
        if (height >= 0) div.setAttribute("height", formatAttr(height));
        return div;
    }

    private static String formatAttr(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static boolean containsEvidence(SemanticRegionResult result, String needle) {
        for (String evidence : result.getComponentEvidence()) {
            if (needle.equals(evidence)) {
                return true;
            }
        }
        return false;
    }

    // ==== TAB_CONTROL 정확한 page membership =====================================================

    /** {@code Div > Static[text]}(TITLE_BAR predicate) -- membership 검증용 leaf region producer. */
    private static Element buildTitleBarLeafFixture(Document doc, String idPrefix, String text) {
        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", idPrefix + "Bar");
        titleBar.setAttribute("left", "0");
        Element label = doc.createElement("Static");
        label.setAttribute("id", idPrefix + "Label");
        label.setAttribute("left", "0");
        label.setAttribute("text", text);
        titleBar.appendChild(label);
        return titleBar;
    }

    private static SemanticRegionResult findRegionBySemanticType(
            List<SemanticRegionResult> results, String semanticType) {
        SemanticRegionResult found = null;
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                if (found != null) {
                    throw new IllegalStateException("more than one " + semanticType + " region in fixture");
                }
                found = r;
            }
        }
        return found;
    }

    private static void testTabPageMembershipSinglePage() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabSingle");
        Element page = doc.createElement("Tabpage");
        page.setAttribute("id", "p0");
        page.appendChild(buildTitleBarLeafFixture(doc, "leaf0", "Leaf0"));
        tab.appendChild(page);
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult titleBarRegion = findRegionBySemanticType(results, "TITLE_BAR");
        assertTrue("membership-single: TITLE_BAR found", titleBarRegion != null);
        assertTrue("membership-single: membership non-null", titleBarRegion.getTabPageMembership() != null);
        assertEquals("membership-single: pageOrdinal", "0",
                String.valueOf(titleBarRegion.getTabPageMembership().getPageOrdinal()));
    }

    private static void testTabPageMembershipMultiPageZeroBasedOrder() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabMulti");
        for (int i = 0; i < 3; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            page.appendChild(buildTitleBarLeafFixture(doc, "leaf" + i, "Leaf" + i));
            tab.appendChild(page);
        }
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        Map<String, Integer> ordinalByLeafText = new LinkedHashMap<String, Integer>();
        for (SemanticRegionResult r : results) {
            if ("TITLE_BAR".equals(r.getSemanticType())) {
                assertTrue("membership-multi: TITLE_BAR has membership", r.getTabPageMembership() != null);
                ordinalByLeafText.put(r.getSourceStructuralId(), r.getTabPageMembership().getPageOrdinal());
            }
        }
        assertEquals("membership-multi: exactly 3 TITLE_BAR regions", "3", String.valueOf(ordinalByLeafText.size()));
        // zero-based order 보존 확인: distinct ordinals {0,1,2}
        java.util.Set<Integer> ordinals = new java.util.HashSet<Integer>(ordinalByLeafText.values());
        assertEquals("membership-multi: distinct ordinal count", "3", String.valueOf(ordinals.size()));
        assertTrue("membership-multi: ordinal 0 present", ordinals.contains(Integer.valueOf(0)));
        assertTrue("membership-multi: ordinal 1 present", ordinals.contains(Integer.valueOf(1)));
        assertTrue("membership-multi: ordinal 2 present", ordinals.contains(Integer.valueOf(2)));
    }

    private static void testTabPageMembershipExactContainingStructuralId() throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabExactId");
        Element page = doc.createElement("Tabpage");
        page.setAttribute("id", "p0");
        page.appendChild(buildTitleBarLeafFixture(doc, "leaf0", "Leaf0"));
        tab.appendChild(page);
        doc.appendChild(tab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(tab);
        SemanticRegionResult tabControlRegion = findRegionBySemanticType(results, "TAB_CONTROL");
        SemanticRegionResult titleBarRegion = findRegionBySemanticType(results, "TITLE_BAR");
        assertEquals("membership-exact-id: containingTabControlStructuralId == TAB_CONTROL's own sourceStructuralId",
                tabControlRegion.getSourceStructuralId(),
                titleBarRegion.getTabPageMembership().getContainingTabControlStructuralId());
    }

    private static void testTabPageMembershipRegionOutsideTabControlIsNull() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element outsideBar = buildTitleBarLeafFixture(doc, "outside", "Outside");
        form.appendChild(outsideBar);
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabSibling");
        form.appendChild(tab);
        doc.appendChild(form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBarRegion = findRegionBySemanticType(results, "TITLE_BAR");
        assertTrue("membership-outside: TITLE_BAR found", titleBarRegion != null);
        assertTrue("membership-outside: membership is null (not inside any TAB_CONTROL page)",
                titleBarRegion.getTabPageMembership() == null);
    }

    private static void testTabPageMembershipNestedTabControlCarriesOuterMembership() throws Exception {
        Document doc = newDocument();
        Element outerTab = doc.createElement("Tab");
        outerTab.setAttribute("id", "outerTab");
        Element outerPage = doc.createElement("Tabpage");
        outerPage.setAttribute("id", "outerP0");
        Element innerTab = doc.createElement("Tab");
        innerTab.setAttribute("id", "innerTab");
        Element innerPage = doc.createElement("Tabpage");
        innerPage.setAttribute("id", "innerP0");
        innerTab.appendChild(innerPage);
        outerPage.appendChild(innerTab);
        outerTab.appendChild(outerPage);
        doc.appendChild(outerTab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(outerTab);
        SemanticRegionResult outerRegion = null;
        SemanticRegionResult innerRegion = null;
        // prefix 길이로 식별 금지 -- 두 anchor Element의 SourceStructuralIdentity와 직접 비교.
        String outerId = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(outerTab);
        String innerId = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(innerTab);
        for (SemanticRegionResult r : results) {
            if (!"TAB_CONTROL".equals(r.getSemanticType())) continue;
            if (outerId.equals(r.getSourceStructuralId())) outerRegion = r;
            if (innerId.equals(r.getSourceStructuralId())) innerRegion = r;
        }
        assertTrue("membership-nested-outer: outer TAB_CONTROL region found", outerRegion != null);
        assertTrue("membership-nested-outer: inner TAB_CONTROL region found", innerRegion != null);
        assertTrue("membership-nested-outer: outer region has null membership (not inside any page itself)",
                outerRegion.getTabPageMembership() == null);
        assertTrue("membership-nested-outer: inner TAB_CONTROL region carries the OUTER membership",
                innerRegion.getTabPageMembership() != null);
        assertEquals("membership-nested-outer: inner region's containingTabControlStructuralId == outer's own id",
                outerId, innerRegion.getTabPageMembership().getContainingTabControlStructuralId());
        assertEquals("membership-nested-outer: inner region's pageOrdinal == outer page index (0)",
                "0", String.valueOf(innerRegion.getTabPageMembership().getPageOrdinal()));
    }

    private static void testTabPageMembershipNestedDescendantUsesNearestInnerMembership() throws Exception {
        Document doc = newDocument();
        Element outerTab = doc.createElement("Tab");
        outerTab.setAttribute("id", "outerTab2");
        Element outerPage = doc.createElement("Tabpage");
        outerPage.setAttribute("id", "outerP0");
        Element innerTab = doc.createElement("Tab");
        innerTab.setAttribute("id", "innerTab2");
        Element innerPage = doc.createElement("Tabpage");
        innerPage.setAttribute("id", "innerP0");
        Element deepLeaf = buildTitleBarLeafFixture(doc, "deep", "Deep");
        innerPage.appendChild(deepLeaf);
        innerTab.appendChild(innerPage);
        outerPage.appendChild(innerTab);
        outerTab.appendChild(outerPage);
        doc.appendChild(outerTab);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(outerTab);
        String innerId = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(innerTab);
        SemanticRegionResult deepRegion = findRegionBySemanticType(results, "TITLE_BAR");
        assertTrue("membership-nested-descendant: deep TITLE_BAR found", deepRegion != null);
        assertTrue("membership-nested-descendant: has membership", deepRegion.getTabPageMembership() != null);
        assertEquals("membership-nested-descendant: uses NEAREST (inner) TAB_CONTROL, not the outer one",
                innerId, deepRegion.getTabPageMembership().getContainingTabControlStructuralId());
        assertEquals("membership-nested-descendant: pageOrdinal relative to inner page (0)",
                "0", String.valueOf(deepRegion.getTabPageMembership().getPageOrdinal()));
    }

    private static void testTabPageMembershipSameOrdinalUnderDifferentParentsUnambiguous() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element tabA = doc.createElement("Tab");
        tabA.setAttribute("id", "tabA");
        Element pageA0 = doc.createElement("Tabpage");
        pageA0.setAttribute("id", "pA0");
        pageA0.appendChild(buildTitleBarLeafFixture(doc, "leafA", "LeafA"));
        tabA.appendChild(pageA0);
        form.appendChild(tabA);

        Element tabB = doc.createElement("Tab");
        tabB.setAttribute("id", "tabB");
        Element pageB0 = doc.createElement("Tabpage");
        pageB0.setAttribute("id", "pB0");
        pageB0.appendChild(buildTitleBarLeafFixture(doc, "leafB", "LeafB"));
        tabB.appendChild(pageB0);
        form.appendChild(tabB);
        doc.appendChild(form);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        String tabAId = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(tabA);
        String tabBId = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(tabB);
        int foundA = 0;
        int foundB = 0;
        for (SemanticRegionResult r : results) {
            if (!"TITLE_BAR".equals(r.getSemanticType())) continue;
            assertTrue("same-ordinal-unambiguous: leaf has membership", r.getTabPageMembership() != null);
            assertEquals("same-ordinal-unambiguous: pageOrdinal is 0 for both", "0",
                    String.valueOf(r.getTabPageMembership().getPageOrdinal()));
            String containing = r.getTabPageMembership().getContainingTabControlStructuralId();
            if (tabAId.equals(containing)) foundA++;
            if (tabBId.equals(containing)) foundB++;
        }
        assertEquals("same-ordinal-unambiguous: exactly one leaf correctly attributed to tabA", "1",
                String.valueOf(foundA));
        assertEquals("same-ordinal-unambiguous: exactly one leaf correctly attributed to tabB", "1",
                String.valueOf(foundB));
    }

    /** Form/Div(bg1, width 300) 아래 Button 3개: btnA(text), btnB(value fallback), btnC(둘 다 없음). */
    private static Element buildThreeButtonFixture(Document doc) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "bg1");
        btnGroup.setAttribute("width", "300");
        Element btnA = doc.createElement("Button");
        btnA.setAttribute("id", "btnA");
        btnA.setAttribute("text", "A");
        btnA.setAttribute("left", "10");
        Element btnB = doc.createElement("Button");
        btnB.setAttribute("id", "btnB");
        btnB.setAttribute("value", "v2");
        btnB.setAttribute("left", "60");
        Element btnC = doc.createElement("Button");
        btnC.setAttribute("id", "btnC");
        btnC.setAttribute("left", "110");
        btnGroup.appendChild(btnA);
        btnGroup.appendChild(btnB);
        btnGroup.appendChild(btnC);
        form.appendChild(btnGroup);
        return form;
    }

    /** text 없는 세 번째 Button도 Segmenter 결과 자체에 role="button" evidence item으로 존재함을
     *  TargetPayloadExtractor를 거치지 않고 직접 검증한다. */
    private static void testButtonGroupEveryFlattenedButtonHasExactlyOneSemanticButtonEvidenceItem()
            throws Exception {
        Document doc = newDocument();
        Element form = buildThreeButtonFixture(doc);
        Element btnGroup = (Element) form.getElementsByTagName("Div").item(0);
        Element btnA = (Element) btnGroup.getElementsByTagName("Button").item(0);
        Element btnB = (Element) btnGroup.getElementsByTagName("Button").item(1);
        Element btnC = (Element) btnGroup.getElementsByTagName("Button").item(2);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult buttonGroup = null;
        for (SemanticRegionResult r : results) {
            if ("BUTTON_GROUP".equals(r.getSemanticType())) {
                buttonGroup = r;
                break;
            }
        }
        assertTrue("semantic_button_evidence: BUTTON_GROUP region found", buttonGroup != null);

        // flattenedButtons.size()(3)이 실제 role="button" evidence item 개수와 일치해야 한다.
        assertEquals("semantic_button_evidence: buttonGroupExpectedButtonCount == 3", "3",
                String.valueOf(buttonGroup.getButtonGroupExpectedButtonCount()));

        List<SourcePayloadEvidenceItem> buttonItems = new ArrayList<SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : buttonGroup.getPayloadEvidence()) {
            if ("button".equals(item.getEvidenceRole())) {
                buttonItems.add(item);
            }
        }
        assertEquals("semantic_button_evidence: exactly 3 role=button evidence items "
                + "(flattenedButtons.size() == semantic role=button evidence count)",
                "3", String.valueOf(buttonItems.size()));

        boolean sawOrder0 = false, sawOrder1 = false, sawOrder2 = false;
        SourcePayloadEvidenceItem thirdItem = null;
        java.util.Set<Integer> orders = new java.util.HashSet<Integer>();
        for (SourcePayloadEvidenceItem item : buttonItems) {
            assertTrue("semantic_button_evidence: no duplicate sourceOrder",
                    orders.add(Integer.valueOf(item.getSourceOrder())));
            if (item.getSourceOrder() == 0) sawOrder0 = true;
            if (item.getSourceOrder() == 1) sawOrder1 = true;
            if (item.getSourceOrder() == 2) { sawOrder2 = true; thirdItem = item; }
        }
        assertTrue("semantic_button_evidence: sourceOrder set is exactly {0,1,2} (0 present)", sawOrder0);
        assertTrue("semantic_button_evidence: sourceOrder set is exactly {0,1,2} (1 present)", sawOrder1);
        assertTrue("semantic_button_evidence: sourceOrder set is exactly {0,1,2} (2 present)", sawOrder2);

        assertTrue("semantic_button_evidence: third (no-text/no-value) button evidence item exists",
                thirdItem != null);
        assertTrue("semantic_button_evidence: third button evidence value is null (absence preserved, "
                + "not suppressed)", thirdItem.getValue() == null);
        assertEquals("semantic_button_evidence: third button exact source structural identity preserved",
                SourceStructuralIdentity.build(btnC), thirdItem.getSourceComponentStructuralId());

        // 처음 두 evidence item의 identity/value와 text->value->null 순서 규칙도 확인.
        for (SourcePayloadEvidenceItem item : buttonItems) {
            if (item.getSourceOrder() == 0) {
                assertEquals("semantic_button_evidence: btnA structuralId",
                        SourceStructuralIdentity.build(btnA), item.getSourceComponentStructuralId());
                assertEquals("semantic_button_evidence: btnA value == text attribute", "A", item.getValue());
            } else if (item.getSourceOrder() == 1) {
                assertEquals("semantic_button_evidence: btnB structuralId",
                        SourceStructuralIdentity.build(btnB), item.getSourceComponentStructuralId());
                assertEquals("semantic_button_evidence: btnB value == value-attribute fallback",
                        "v2", item.getValue());
            }
        }
    }

    /** End-to-end: 실제 Segmenter 출력을 Composition stage를 거쳐 TargetPayloadExtractor까지
     *  통과시켜 semantic->payload 경계를 실제로 건넌다. */
    private static void testButtonGroupSegmenterToExtractorIntegrationCrossesLossBoundary() throws Exception {
        Document doc = newDocument();
        Element form = buildThreeButtonFixture(doc);
        Element btnGroup = (Element) form.getElementsByTagName("Div").item(0);
        String planNodeId = SourceStructuralIdentity.build(btnGroup);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);

        TargetNodePayload payload = null;
        for (TargetNodePayload p : payloads) {
            if (planNodeId.equals(p.getPlanNodeId())) {
                payload = p;
                break;
            }
        }
        assertTrue("segmenter_to_extractor: BUTTON_GROUP payload present", payload != null);
        assertEquals("segmenter_to_extractor: expectedStructuralMemberCount == 3", "3",
                String.valueOf(payload.getExpectedStructuralMemberCount()));

        List<TargetLeafPayload> buttonLeaves = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.DISPLAY_TEXT
                    && item.getStructuredData().containsKey("buttonOrdinal")) {
                buttonLeaves.add(item);
            }
        }
        assertEquals("segmenter_to_extractor: exactly 3 role=button TargetLeafPayload entries "
                + "(no source Button silently lost)", "3", String.valueOf(buttonLeaves.size()));

        boolean sawOrdinal0 = false, sawOrdinal1 = false;
        TargetLeafPayload ordinal2Leaf = null;
        java.util.Set<Object> ordinals = new java.util.HashSet<Object>();
        for (TargetLeafPayload item : buttonLeaves) {
            Object ordinal = item.getStructuredData().get("buttonOrdinal");
            assertTrue("segmenter_to_extractor: no duplicate buttonOrdinal", ordinals.add(ordinal));
            if (Integer.valueOf(0).equals(ordinal)) sawOrdinal0 = true;
            if (Integer.valueOf(1).equals(ordinal)) sawOrdinal1 = true;
            if (Integer.valueOf(2).equals(ordinal)) ordinal2Leaf = item;
        }
        assertTrue("segmenter_to_extractor: buttonOrdinal set is exactly {0,1,2} (0 present)", sawOrdinal0);
        assertTrue("segmenter_to_extractor: buttonOrdinal set is exactly {0,1,2} (1 present)", sawOrdinal1);
        assertTrue("segmenter_to_extractor: third button leaf (ordinal 2) exists", ordinal2Leaf != null);
        assertTrue("segmenter_to_extractor: third button leaf has null presentation value "
                + "(absence preserved, not silently dropped)", ordinal2Leaf.getValue() == null);
    }

    // ==== pass-local component predicate 분석 캐시 =========================================

    /** {@code onPredicateAnalysisComputed} 훅을 오버라이드해 pass 동안 실제 analyze된 모든
     *  Element/XfdlAnalysisResult 쌍을 기록한다(관찰만 하며 analyzer를 대체하지 않음). */
    private static final class RecordingSegmenter extends SemanticRegionSegmenter {
        final List<Element> computedElements = new ArrayList<Element>();
        final List<XfdlAnalysisResult> computedAnalysis = new ArrayList<XfdlAnalysisResult>();

        @Override
        void onPredicateAnalysisComputed(Element element, com.example.xfdltracker.model.XfdlAnalysisResult analysis) {
            computedElements.add(element);
            computedAnalysis.add(analysis);
        }
    }

    /** pageTitle(TITLE_BAR) + pageActions(BUTTON_GROUP)는 pageTitle이 두 경로(자신의 region
     *  판정 + pageActions의 title_bar_attached 인접 검사)에서 분석되는 fixture다. 캐시 없이는
     *  2회, 캐시 있으면 정확히 1회 analyze되어야 한다. */
    private static Element buildTitleBarAdjacentButtonGroupFixture(Document doc) {
        Element root = doc.createElement("Div");
        root.setAttribute("id", "cacheRoot");

        Element pageTitle = doc.createElement("Div");
        pageTitle.setAttribute("id", "cachePageTitle");
        Element titleLabel = doc.createElement("Static");
        titleLabel.setAttribute("id", "cacheTitleLabel");
        titleLabel.setAttribute("left", "0");
        titleLabel.setAttribute("width", "100");
        titleLabel.setAttribute("height", "20");
        pageTitle.appendChild(titleLabel);

        Element pageActions = doc.createElement("Div");
        pageActions.setAttribute("id", "cachePageActions");
        pageActions.setAttribute("width", "200");
        Element newBtn = doc.createElement("Button");
        newBtn.setAttribute("id", "cacheNewBtn");
        newBtn.setAttribute("left", "120");
        newBtn.setAttribute("width", "60");
        newBtn.setAttribute("height", "20");
        pageActions.appendChild(newBtn);

        root.appendChild(pageTitle);
        root.appendChild(pageActions);
        return root;
    }

    private static void testPredicateAnalysisCacheAnalyzesEachElementAtMostOncePerPass() throws Exception {
        Document doc = newDocument();
        Element root = buildTitleBarAdjacentButtonGroupFixture(doc);

        RecordingSegmenter segmenter = new RecordingSegmenter();
        List<SemanticRegionResult> results = segmenter.segment(root);

        // precondition: fixture가 실제로 TITLE_BAR/BUTTON_GROUP 쌍을 만드는지 확인.
        boolean sawTitleBar = false, sawButtonGroupAttached = false;
        for (SemanticRegionResult r : results) {
            if ("TITLE_BAR".equals(r.getSemanticType())) sawTitleBar = true;
            if ("BUTTON_GROUP".equals(r.getSemanticType())
                    && "title_bar_attached".equals(r.getRecommendedVariant())) sawButtonGroupAttached = true;
        }
        assertTrue("predicate-cache: precondition -- TITLE_BAR detected", sawTitleBar);
        assertTrue("predicate-cache: precondition -- BUTTON_GROUP(title_bar_attached) detected",
                sawButtonGroupAttached);

        Map<Element, Integer> countByIdentity = new IdentityHashMap<Element, Integer>();
        for (Element e : segmenter.computedElements) {
            Integer prior = countByIdentity.get(e);
            countByIdentity.put(e, prior == null ? 1 : prior + 1);
        }
        for (Map.Entry<Element, Integer> entry : countByIdentity.entrySet()) {
            assertEquals("predicate-cache: element " + entry.getKey().getAttribute("id")
                    + " analyzed at most once (exactly once, since it was analyzed at all) in this pass",
                    "1", String.valueOf(entry.getValue()));
        }

        boolean pageTitleAnalyzed = countByIdentity.containsKey(titleBarPageTitleElement(root));
        assertTrue("predicate-cache: precondition -- the doubly-referenced pageTitle element was "
                + "actually analyzed (both call paths reach analyzeCached)", pageTitleAnalyzed);
    }

    private static void testPredicateAnalysisCacheThreadsSameAnalysisObjectThroughoutPass() throws Exception {
        Document doc = newDocument();
        Element root = buildTitleBarAdjacentButtonGroupFixture(doc);
        XfdlAnalysisResult analysis = new XfdlAnalysisResult();

        RecordingSegmenter segmenter = new RecordingSegmenter();
        segmenter.segment(root, analysis);

        assertTrue("predicate-cache: at least one real analyze() call recorded",
                !segmenter.computedAnalysis.isEmpty());
        for (XfdlAnalysisResult recorded : segmenter.computedAnalysis) {
            assertTrue("predicate-cache: every analyze() call in the pass received the exact same "
                    + "XfdlAnalysisResult object instance (identity, not just equality)",
                    recorded == analysis);
        }
    }

    private static void testPredicateAnalysisCacheIsFreshPerIndependentSegmentInvocation() throws Exception {
        Document doc1 = newDocument();
        Element root1 = buildTitleBarAdjacentButtonGroupFixture(doc1);
        RecordingSegmenter firstPass = new RecordingSegmenter();
        firstPass.segment(root1);
        int firstPassComputedCount = firstPass.computedElements.size();
        assertTrue("predicate-cache: precondition -- first pass computed at least one element",
                firstPassComputedCount > 0);

        Document doc2 = newDocument();
        Element root2 = buildTitleBarAdjacentButtonGroupFixture(doc2);
        RecordingSegmenter secondPass = new RecordingSegmenter();
        secondPass.segment(root2);
        int secondPassComputedCount = secondPass.computedElements.size();

        assertEquals("predicate-cache: an independent second segment() invocation on an "
                + "equivalent-shape fixture starts from a fresh cache and re-computes the same "
                + "number of elements as the first pass (no cross-pass reuse suppressing computation)",
                String.valueOf(firstPassComputedCount), String.valueOf(secondPassComputedCount));

        // 두 pass의 Element는 서로 다른 Document의 disjoint object set이어야 한다(전역 캐시 없음 확인).
        Map<Element, Boolean> firstPassElements = new IdentityHashMap<Element, Boolean>();
        for (Element e : firstPass.computedElements) {
            firstPassElements.put(e, Boolean.TRUE);
        }
        for (Element e : secondPass.computedElements) {
            assertTrue("predicate-cache: second pass's computed elements are not the same object "
                    + "identities as the first pass's (fresh cache, no static/global sharing)",
                    !firstPassElements.containsKey(e));
        }
    }

    /** id로 fixture tree 안의 {@code cachePageTitle} Div를 다시 찾는다. */
    private static Element titleBarPageTitleElement(Element root) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element && "cachePageTitle".equals(((Element) n).getAttribute("id"))) {
                return (Element) n;
            }
        }
        return null;
    }

    /** test-only convenience -- production {@link TargetPayloadExtractor}는 binding evidence를
     *  스스로 계산하지 않으므로, 여기서 {@link SourceBindingAnalyzer}를 먼저 호출해 넘겨준다. */
    private static List<TargetNodePayload> extractWithBindings(
            Element sourceRoot, TargetCompositionPlan plan, List<SemanticRegionResult> regions) {
        List<SourceBindingReference> bindingReferences = sourceRoot == null
                ? new ArrayList<SourceBindingReference>() : new SourceBindingAnalyzer().analyze(sourceRoot);
        return new TargetPayloadExtractor().extract(sourceRoot, plan, regions, bindingReferences);
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

    private static void assertArrayEquals(String label, String[] expected, String[] actual) {
        if (!Arrays.equals(expected, actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
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

    private static void assertFalse(String label, boolean actual) {
        if (actual) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=false actual=true");
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
