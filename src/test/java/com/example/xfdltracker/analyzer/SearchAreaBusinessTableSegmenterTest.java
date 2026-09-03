package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 외부 의존성 없는(JUnit 없이 동작하는) 단독 실행 테스트다. SEARCH_AREA/BUSINESS_TABLE.horizontal Shadow
 * discrimination(Wrapper Normalization 모델)을 검증한다.
 */
public class SearchAreaBusinessTableSegmenterTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCanonicalSearchAreaWithGridSibling();
        testSameStructureNoSearchContextBecomesBusinessTable();
        testAmbiguousCandidateNoSearchAreaMisdetection();
        testTwoRowsBusinessTableIsHighConfidence();
        testBehavioralBoundaryPreventsWrapperMerge();
        testTransparentWrapperIsSkippedToFindGrid();
        testCardinalityBeyondObservedRangeDowngradesToMedium();
        testCardinalityWithinObservedRangeStaysHigh();
        testTwoRowsBusinessTableRowIndexMatchesActualRows();
        testAsymmetricRowsPreserveCellPairIndexEvidence();
        testNonApplicableRolesHaveNullCellPairFields();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** search-area-basic: label/input 쌍 뒤에 Grid가 바로 이어지면 SEARCH_AREA HIGH. */
    private static void testCanonicalSearchAreaWithGridSibling() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "resultGrid");
        parent.appendChild(container);
        parent.appendChild(grid);

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("canonical-search-area: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("canonical-search-area: semanticType", "SEARCH_AREA", r.getSemanticType());
        assertEquals("canonical-search-area: recommendedTemplateFamily", "SEARCH_AREA", r.getRecommendedTemplateFamily());
        assertEquals("canonical-search-area: recommendedVariant", "basic", r.getRecommendedVariant());
        assertEquals("canonical-search-area: confidence", "HIGH", r.getConfidence());
    }

    /** 동일 구조라도 조회 context(Grid)가 없으면 BUSINESS_TABLE.horizontal(MEDIUM, 단일 행)로
     *  판정되며 SEARCH_AREA로 오검출되지 않는다. */
    private static void testSameStructureNoSearchContextBecomesBusinessTable() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        parent.appendChild(container); // 마지막 자식 -- 뒤에 아무것도 없음

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("no-search-context: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("no-search-context: semanticType is BUSINESS_TABLE (not SEARCH_AREA)",
                "BUSINESS_TABLE", r.getSemanticType());
        assertEquals("no-search-context: recommendedVariant", "horizontal", r.getRecommendedVariant());
        assertEquals("no-search-context: confidence MEDIUM (single row)", "MEDIUM", r.getConfidence());
    }

    /** search-area-ambiguous와 동일 구조: SEARCH_AREA로 추측 승격되지 않았는지 명시적으로 재확인. */
    private static void testAmbiguousCandidateNoSearchAreaMisdetection() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        parent.appendChild(container);

        List<SemanticRegionResult> searchAreaResults = regionResultsOf(parent, "SEARCH_AREA");
        assertEquals("ambiguous-no-misdetection: SEARCH_AREA count", "0", String.valueOf(searchAreaResults.size()));
    }

    /** input-form-basic: 조회 맥락 없는 2-행 label/control 격자 -> BUSINESS_TABLE.horizontal HIGH. */
    private static void testTwoRowsBusinessTableIsHighConfidence() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newDiv(doc, "grid2Rows");
        appendLabelControlPair(doc, container, "label1", "Edit", 0, 0);
        appendLabelControlPair(doc, container, "label2", "Edit", 200, 0);
        appendLabelControlPair(doc, container, "label3", "Edit", 0, 30);
        appendLabelControlPair(doc, container, "label4", "Edit", 200, 30);
        parent.appendChild(container); // 인접 Grid/검색 Button 없음

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("two-rows-business-table: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("two-rows-business-table: semanticType", "BUSINESS_TABLE", r.getSemanticType());
        assertEquals("two-rows-business-table: recommendedVariant", "horizontal", r.getRecommendedVariant());
        assertEquals("two-rows-business-table: confidence HIGH (multi-row regularity)", "HIGH", r.getConfidence());
    }

    /** container와 Grid 사이에 독립 visible 속성의 불투명 wrapper가 있으면, 안에 Grid가 있어도
     *  건너뛰지 않는다(BUSINESS_TABLE로 판정). */
    private static void testBehavioralBoundaryPreventsWrapperMerge() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element opaqueWrapper = newDiv(doc, "additionalConditions");
        opaqueWrapper.setAttribute("visible", "ds1:cond_expr"); // 독립적인 visible condition -- 불투명
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "hiddenGrid");
        opaqueWrapper.appendChild(nestedGrid); // wrapper 안에 Grid가 있어도 건너뛰면 안 됨
        parent.appendChild(container);
        parent.appendChild(opaqueWrapper);

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("behavioral-boundary: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("behavioral-boundary: semanticType is BUSINESS_TABLE (no blind merge past opaque wrapper)",
                "BUSINESS_TABLE", r.getSemanticType());
        assertTrue("behavioral-boundary: evidence records stop at opaque boundary",
                containsHierarchyEvidence(r, "wrapper_normalization_stopped_at_visible_or_enable_boundary"));
    }

    /** 대조군: 불투명 속성 없는 투명 wrapper 안에 Grid가 있으면 그 안까지 탐색해 SEARCH_AREA로 판정한다. */
    private static void testTransparentWrapperIsSkippedToFindGrid() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element transparentWrapper = newDiv(doc, "pureVisualWrapper"); // visible/enable 없음
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "resultGrid");
        transparentWrapper.appendChild(nestedGrid);
        parent.appendChild(container);
        parent.appendChild(transparentWrapper);

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("transparent-wrapper-skip: count", "1", String.valueOf(results.size()));
        assertEquals("transparent-wrapper-skip: semanticType", "SEARCH_AREA", results.get(0).getSemanticType());
    }

    /** 관찰 범위(1-4쌍) 초과 6쌍 + Grid context -> SEARCH_AREA는 여전히 발동하되 confidence는
     *  MEDIUM, column_count는 실제 값(6) 그대로 보존된다. */
    private static void testCardinalityBeyondObservedRangeDowngradesToMedium() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRowWithCount(doc, "wideConditionRow", 0, 6);
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "resultGrid");
        parent.appendChild(container);
        parent.appendChild(grid);

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("cardinality-beyond-range: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("cardinality-beyond-range: semanticType still SEARCH_AREA (not forced to HOLD)",
                "SEARCH_AREA", r.getSemanticType());
        assertEquals("cardinality-beyond-range: recommendedVariant unchanged", "basic", r.getRecommendedVariant());
        assertEquals("cardinality-beyond-range: confidence downgraded to MEDIUM (6 pairs > observed max 4)",
                "MEDIUM", r.getConfidence());
        assertEquals("cardinality-beyond-range: column_count preserves actual pair count, not truncated to 4",
                "6", String.valueOf(r.getParameters().get("column_count")));
    }

    /** 대조군: 관찰 범위(1-4쌍) 이내면 기존과 동일하게 HIGH를 유지한다(회귀 없음 확인). */
    private static void testCardinalityWithinObservedRangeStaysHigh() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRowWithCount(doc, "fourPairConditionRow", 0, 4);
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "resultGrid2");
        parent.appendChild(container);
        parent.appendChild(grid);

        List<SemanticRegionResult> results = regionResultsOf(parent, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("cardinality-within-range: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("cardinality-within-range: semanticType", "SEARCH_AREA", r.getSemanticType());
        assertEquals("cardinality-within-range: confidence stays HIGH at the observed boundary (4 pairs)",
                "HIGH", r.getConfidence());
        assertEquals("cardinality-within-range: column_count", "4", String.valueOf(r.getParameters().get("column_count")));
    }

    /** 2행 x 2쌍(대칭) BUSINESS_TABLE에서 predicate가 확정한 row 구조가
     *  {@link SourcePayloadEvidenceItem#getRowIndex()}에 그대로 보존되는지 확인한다. */
    private static void testTwoRowsBusinessTableRowIndexMatchesActualRows() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newDiv(doc, "grid2Rows");
        appendLabelControlPair(doc, container, "label1", "Edit", 0, 0);
        appendLabelControlPair(doc, container, "label2", "Edit", 200, 0);
        appendLabelControlPair(doc, container, "label3", "Edit", 0, 30);
        appendLabelControlPair(doc, container, "label4", "Edit", 200, 30);
        parent.appendChild(container);

        List<SemanticRegionResult> results = regionResultsOf(parent, "BUSINESS_TABLE");
        assertEquals("two-rows-row-index: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        List<SourcePayloadEvidenceItem> evidence = r.getPayloadEvidence();
        assertEquals("two-rows-row-index: 2 pairs x 2 rows -> 8 items", "8", String.valueOf(evidence.size()));

        int row0Count = 0, row1Count = 0;
        for (SourcePayloadEvidenceItem item : evidence) {
            assertTrue("two-rows-row-index: rowIndex must not be null for role=" + item.getEvidenceRole(),
                    item.getRowIndex() != null);
            if (item.getSourceOrder() < 4) {
                assertEquals("two-rows-row-index: item[sourceOrder=" + item.getSourceOrder() + "] rowIndex=0",
                        "0", String.valueOf(item.getRowIndex()));
                row0Count++;
            } else {
                assertEquals("two-rows-row-index: item[sourceOrder=" + item.getSourceOrder() + "] rowIndex=1",
                        "1", String.valueOf(item.getRowIndex()));
                row1Count++;
            }
        }
        assertEquals("two-rows-row-index: row0 item count", "4", String.valueOf(row0Count));
        assertEquals("two-rows-row-index: row1 item count", "4", String.valueOf(row1Count));
    }

    /** row0=2쌍, row1=1쌍(비대칭)일 때 {@code cellIndexInRow}/{@code pairIndexInRow}가 row-local
     *  경계를 explicit하게 표현하며, (rowIndex, pairIndexInRow) 튜플로 서로 다른 행의 같은
     *  ordinal도 명확히 구분됨을 확인한다. */
    private static void testAsymmetricRowsPreserveCellPairIndexEvidence() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newDiv(doc, "asymmetricRows");
        appendLabelControlPair(doc, container, "r1label1", "Edit", 0, 0);
        appendLabelControlPair(doc, container, "r1label2", "Edit", 200, 0);
        appendLabelControlPair(doc, container, "r2label1", "Edit", 0, 30);
        parent.appendChild(container);

        List<SemanticRegionResult> results = regionResultsOf(parent, "BUSINESS_TABLE");
        assertEquals("asymmetric-rows: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        List<SourcePayloadEvidenceItem> evidence = r.getPayloadEvidence();
        assertEquals("asymmetric-rows: 3 pairs -> 6 items", "6", String.valueOf(evidence.size()));

        // sourceOrder별 기대 {rowIndex, cellIndexInRow, pairIndexInRow}.
        int[][] expected = { {0, 0, 0}, {0, 1, 0}, {0, 2, 1}, {0, 3, 1}, {1, 0, 0}, {1, 1, 0} };
        for (SourcePayloadEvidenceItem item : evidence) {
            int[] exp = expected[item.getSourceOrder()];
            assertEquals("asymmetric-rows: item[sourceOrder=" + item.getSourceOrder() + "] rowIndex",
                    String.valueOf(exp[0]), String.valueOf(item.getRowIndex()));
            assertEquals("asymmetric-rows: item[sourceOrder=" + item.getSourceOrder() + "] cellIndexInRow",
                    String.valueOf(exp[1]), String.valueOf(item.getCellIndexInRow()));
            assertEquals("asymmetric-rows: item[sourceOrder=" + item.getSourceOrder() + "] pairIndexInRow",
                    String.valueOf(exp[2]), String.valueOf(item.getPairIndexInRow()));
        }

        // 같은 행의 label/control은 동일한 pair identity(rowIndex, pairIndexInRow)를 공유한다.
        SourcePayloadEvidenceItem row0Pair0Label = evidence.get(0);
        SourcePayloadEvidenceItem row0Pair0Control = evidence.get(1);
        assertEquals("asymmetric-rows: row0/pair0 label+control share rowIndex",
                String.valueOf(row0Pair0Label.getRowIndex()), String.valueOf(row0Pair0Control.getRowIndex()));
        assertEquals("asymmetric-rows: row0/pair0 label+control share pairIndexInRow",
                String.valueOf(row0Pair0Label.getPairIndexInRow()), String.valueOf(row0Pair0Control.getPairIndexInRow()));

        // 같은 행 안의 다른 pair는 pair identity를 공유하지 않는다.
        SourcePayloadEvidenceItem row0Pair1Label = evidence.get(2);
        assertTrue("asymmetric-rows: row0/pair0 and row0/pair1 have different pairIndexInRow",
                !row0Pair0Label.getPairIndexInRow().equals(row0Pair1Label.getPairIndexInRow()));

        // 서로 다른 행은 같은 pairIndexInRow ordinal을 공유할 수 있다(row-local) -- 튜플로 구분.
        SourcePayloadEvidenceItem row1Pair0Label = evidence.get(4);
        assertEquals("asymmetric-rows: row1/pair0 shares the SAME pairIndexInRow ordinal as row0/pair0 "
                        + "(row-local, not global)",
                String.valueOf(row0Pair0Label.getPairIndexInRow()), String.valueOf(row1Pair0Label.getPairIndexInRow()));
        assertTrue("asymmetric-rows: but row0/pair0 and row1/pair0 are distinct rows (rowIndex differs)",
                !row0Pair0Label.getRowIndex().equals(row1Pair0Label.getRowIndex()));
    }

    /** non-applicable role(title_label 등)에서는 rowIndex/cellIndexInRow/pairIndexInRow가
     *  전부 null이어야 한다(row 개념 없는 role에 값이 새어들지 않음). */
    private static void testNonApplicableRolesHaveNullCellPairFields() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element titleBar = newDiv(doc, "titleBar1");
        Element titleStatic = doc.createElement("Static");
        titleStatic.setAttribute("id", "titleText1");
        titleStatic.setAttribute("text", "화면 제목");
        titleStatic.setAttribute("left", "0");
        titleBar.appendChild(titleStatic);
        form.appendChild(titleBar);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBarRegion = null;
        for (SemanticRegionResult region : regions) {
            if ("TITLE_BAR".equals(region.getSemanticType())) titleBarRegion = region;
        }
        assertTrue("non_applicable_roles: TITLE_BAR region found", titleBarRegion != null);
        assertEquals("non_applicable_roles: 1 title_label item", "1",
                String.valueOf(titleBarRegion.getPayloadEvidence().size()));
        SourcePayloadEvidenceItem item = titleBarRegion.getPayloadEvidence().get(0);
        assertEquals("non_applicable_roles: role is title_label", "title_label", item.getEvidenceRole());
        assertTrue("non_applicable_roles: rowIndex is null", item.getRowIndex() == null);
        assertTrue("non_applicable_roles: cellIndexInRow is null", item.getCellIndexInRow() == null);
        assertTrue("non_applicable_roles: pairIndexInRow is null", item.getPairIndexInRow() == null);
    }

    // ---- fixture 생성 도우미 ----

    /** label(Static)+control(Edit) 2쌍을 가진 하나의 행 컨테이너(top=topOffset)를 만든다. */
    private static Element newLabelControlPairsRow(Document doc, String id, double topOffset) {
        Element container = newDiv(doc, id);
        appendLabelControlPair(doc, container, id + "_label1", "Edit", 0, topOffset);
        appendLabelControlPair(doc, container, id + "_label2", "Edit", 200, topOffset);
        return container;
    }

    /** label(Static)+control(Edit) N쌍(각 쌍 폭 200)을 가진 하나의 행 컨테이너를 만든다. */
    private static Element newLabelControlPairsRowWithCount(Document doc, String id, double topOffset, int pairCount) {
        Element container = newDiv(doc, id);
        for (int i = 0; i < pairCount; i++) {
            appendLabelControlPair(doc, container, id + "_label" + i, "Edit", i * 200, topOffset);
        }
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

    private static Element newDiv(Document doc, String id) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        return div;
    }

    private static List<SemanticRegionResult> regionResultsOf(Element root, String... semanticTypes) {
        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(root);
        List<SemanticRegionResult> filtered = new ArrayList<SemanticRegionResult>();
        for (SemanticRegionResult r : all) {
            for (String type : semanticTypes) {
                if (type.equals(r.getSemanticType())) {
                    filtered.add(r);
                    break;
                }
            }
        }
        return filtered;
    }

    private static boolean containsHierarchyEvidence(SemanticRegionResult result, String needle) {
        for (String evidence : result.getHierarchyEvidence()) {
            if (needle.equals(evidence)) {
                return true;
            }
        }
        return false;
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
