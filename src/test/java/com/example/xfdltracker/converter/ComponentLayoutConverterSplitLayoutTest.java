package com.example.xfdltracker.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Offline 최소 unit test(JUnit 미사용). design/regression_fixture_spec.md의 split-layout
 * 관련 fixture 의미를 classifyColumnRatioGeometry/resolveExactColumnRatios로 재현한다.
 * src/test/java 소속이라 build.bat 대상이 아니므로 별도 javac/java로 수동 실행해야 한다.
 */
public class ComponentLayoutConverterSplitLayoutTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSplitLayoutHighConfidenceExactRatio();
        testExactThirdsColThirtyThree();
        testNearThirdsDoesNotPromoteToColThirtyThree();
        testSplitLayoutLowConfidenceOverlap();
        testSplitLayoutLowConfidenceGap();
        testGeometryToleranceUncalibratedConservative();
        testNoNearestColNRoundingForNearMiss();
        testExistingClassifyLayoutGeometryContractUnaffected();
        testZeroWidthFirstChildRejected();
        testZeroWidthMiddleChildSameLeftTieRejected();
        testZeroWidthLastChildRejected();
        testNegativeWidthChildRejected();
        testZeroWidthRejectionIndependentOfEncounterOrder();
        testExistingOverlapGapTopHeightRejectionUnaffectedByPositivityGuard();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** split-layout-high-confidence: 정확히 30/70 -> SPLIT_LAYOUT_RATIO_EXACT_MATCH, ratios=[col_3,col_7]. */
    private static void testSplitLayoutHighConfidenceExactRatio() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = twoColumns(0, 300, 300, 700, 0, 400);

        String status = converter.classifyColumnRatioGeometry(children);
        assertEquals("split-layout-high-confidence: status", "SPLIT_LAYOUT_RATIO_EXACT_MATCH", status);

        String[] ratios = converter.resolveExactColumnRatios(children);
        assertArrayEquals("split-layout-high-confidence: ratios", new String[] {"col_3", "col_7"}, ratios);
    }

    /**
     * col_N(1..9)로 표현 불가능한 정확한 3등분은 col_33 사용: 폭이 완전히 같은 3형제만
     * SPLIT_LAYOUT_RATIO_EXACT_MATCH(ratios=[col_33,col_33,col_33])로 매치되어야 하며,
     * approximate tolerance나 col_3/col_3/col_4 대체는 허용되지 않는다.
     */
    private static void testExactThirdsColThirtyThree() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = threeColumns(0, 333, 333, 333, 666, 333, 0, 400);

        assertEquals("exact-thirds-col-33: status",
                "SPLIT_LAYOUT_RATIO_EXACT_MATCH", converter.classifyColumnRatioGeometry(children));
        String[] ratios = converter.resolveExactColumnRatios(children);
        assertArrayEquals("exact-thirds-col-33: ratios", new String[] {"col_33", "col_33", "col_33"}, ratios);
    }

    /**
     * col_33 오검출 방지: 폭이 거의 같지만(333/333/334) 정확히 같지는 않은 3개 형제는
     * approximate 3등분으로 col_33을 추측하면 안 되고, col_N으로도 정확히 표현되지 않으므로
     * FIXED_WIDTH_FALLBACK이어야 한다.
     */
    private static void testNearThirdsDoesNotPromoteToColThirtyThree() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = threeColumns(0, 333, 333, 333, 666, 334, 0, 400);

        assertEquals("near-thirds-no-col-33-promotion: status",
                "FIXED_WIDTH_FALLBACK", converter.classifyColumnRatioGeometry(children));
        assertNull("near-thirds-no-col-33-promotion: ratios", converter.resolveExactColumnRatios(children));
    }

    /** split-layout-low-confidence (overlap): 겹치는 두 형제 -> GEOMETRY_INVALID. */
    private static void testSplitLayoutLowConfidenceOverlap() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        // left=0..300 과 left=250..950 -> 250 < 300 이므로 overlap.
        List<Element> children = twoColumns(0, 300, 250, 700, 0, 400);

        assertEquals("split-layout-low-confidence(overlap): existing classifyLayoutGeometry unaffected",
                "ABSOLUTE_LAYOUT_FALLBACK", converter.classifyLayoutGeometry(children));
        assertEquals("split-layout-low-confidence(overlap): status",
                "GEOMETRY_INVALID", converter.classifyColumnRatioGeometry(children));
        assertNull("split-layout-low-confidence(overlap): ratios",
                converter.resolveExactColumnRatios(children));
    }

    /** split-layout-low-confidence (gap): 겹치지 않지만 사이가 벌어짐 -> GEOMETRY_INVALID. */
    private static void testSplitLayoutLowConfidenceGap() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        // left=0..300 과 left=400..1000 -> 300과 400 사이 gap(100), overlap은 아님.
        List<Element> children = twoColumns(0, 300, 400, 600, 0, 400);

        assertEquals("split-layout-low-confidence(gap): existing classifyLayoutGeometry unaffected"
                        + " (gap은 overlap이 아니므로 TABLE_LAYOUT_HIGH_CONFIDENCE로 남아야 한다)",
                "TABLE_LAYOUT_HIGH_CONFIDENCE", converter.classifyLayoutGeometry(children));
        assertEquals("split-layout-low-confidence(gap): status",
                "GEOMETRY_INVALID", converter.classifyColumnRatioGeometry(children));
        assertNull("split-layout-low-confidence(gap): ratios",
                converter.resolveExactColumnRatios(children));
    }

    /**
     * geometry-tolerance-uncalibrated-conservative: 분할 자체는 성립하지만 비율이 col_N과
     * 정확히 일치하지 않음(29.6/70.4) -> FIXED_WIDTH_FALLBACK, ratios=null.
     */
    private static void testGeometryToleranceUncalibratedConservative() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = twoColumns(0, 296, 296, 704, 0, 400);

        assertEquals("geometry-tolerance-uncalibrated-conservative: existing classifyLayoutGeometry unaffected",
                "TABLE_LAYOUT_HIGH_CONFIDENCE", converter.classifyLayoutGeometry(children));
        assertEquals("geometry-tolerance-uncalibrated-conservative: status",
                "FIXED_WIDTH_FALLBACK", converter.classifyColumnRatioGeometry(children));
        assertNull("geometry-tolerance-uncalibrated-conservative: ratios",
                converter.resolveExactColumnRatios(children));
    }

    /**
     * "nearest col_N rounding 금지" 확인: col_N 경계에 아주 가깝지만(29.99/70.01) 정확히
     * 일치하지 않으면 여전히 FIXED_WIDTH_FALLBACK -- 근접도로 canonical 매치를 추측하지 않는다.
     */
    private static void testNoNearestColNRoundingForNearMiss() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = twoColumns(0, 299.9, 299.9, 700.1, 0, 400);

        assertEquals("no-nearest-col-N-rounding: status",
                "FIXED_WIDTH_FALLBACK", converter.classifyColumnRatioGeometry(children));
        assertNull("no-nearest-col-N-rounding: ratios", converter.resolveExactColumnRatios(children));
    }

    /**
     * 레이어 2 migration strategy 재확인: 기존 classifyLayoutGeometry()의 3-state 반환값 자체가
     * 이번 확장으로 전혀 바뀌지 않았음을 별도로 재검증한다(UNRESOLVED_LAYOUT 케이스 포함).
     */
    private static void testExistingClassifyLayoutGeometryContractUnaffected() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        assertEquals("classifyLayoutGeometry: empty children -> UNRESOLVED_LAYOUT",
                "UNRESOLVED_LAYOUT", converter.classifyLayoutGeometry(new ArrayList<Element>()));
    }

    /**
     * 첫 cell이 zero-width면 contiguous chain은 성립하고 hasExactOverlap도 overlap으로
     * 잡아내지 못하지만, strict-positive-width guard가 sort/chain 검증 이전에 fail-closed해야 한다.
     */
    private static void testZeroWidthFirstChildRejected() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = twoColumns(0, 0, 0, 700, 0, 400);

        assertEquals("zero-width-first-child: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(children));
        assertNull("zero-width-first-child: ratios", converter.resolveExactColumnRatios(children));
    }

    /**
     * 가운데 cell이 zero-width이고 다음 cell과 left가 동일한 tie 상황: guard가 없으면
     * stable sort가 원본 삽입 순서로 이 tie를 조용히 깨뜨릴 수 있었다.
     */
    private static void testZeroWidthMiddleChildSameLeftTieRejected() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = threeColumns(0, 300, 300, 0, 300, 400, 0, 400);

        assertEquals("zero-width-middle-child-same-left-tie: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(children));
        assertNull("zero-width-middle-child-same-left-tie: ratios", converter.resolveExactColumnRatios(children));
    }

    /** 마지막 cell이 zero-width인 경우도 GEOMETRY_INVALID여야 한다. */
    private static void testZeroWidthLastChildRejected() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = threeColumns(0, 300, 300, 700, 1000, 0, 0, 400);

        assertEquals("zero-width-last-child: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(children));
        assertNull("zero-width-last-child: ratios", converter.resolveExactColumnRatios(children));
    }

    /** negative width는 normalize하지 않고 그대로 fail-closed해야 한다. */
    private static void testNegativeWidthChildRejected() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        List<Element> children = twoColumns(0, -50, -50, 750, 0, 400);

        assertEquals("negative-width-child: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(children));
        assertNull("negative-width-child: ratios", converter.resolveExactColumnRatios(children));
    }

    /** zero-width degenerate 조합은 encounter order(삽입 순서)를 바꿔도 결과가 바뀌지 않아야 한다. */
    private static void testZeroWidthRejectionIndependentOfEncounterOrder() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();
        Document doc = newDocument();
        Element zeroWidthAtThreeHundred = newDiv(doc, 300, 0, 0, 400);
        Element normalAtThreeHundred = newDiv(doc, 300, 0, 400, 400);
        Element leading = newDiv(doc, 0, 0, 300, 400);

        List<Element> encounterOrderA = Arrays.asList(leading, zeroWidthAtThreeHundred, normalAtThreeHundred);
        List<Element> encounterOrderB = Arrays.asList(normalAtThreeHundred, leading, zeroWidthAtThreeHundred);

        assertEquals("zero-width-encounter-order-A: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(encounterOrderA));
        assertEquals("zero-width-encounter-order-B: status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(encounterOrderB));
    }

    /** positivity guard 추가가 기존 overlap/gap/top-height mismatch rejection standing을
     * 바꾸지 않았음을 재확인한다(모두 positive width 조합, guard 개입 여지 없음). */
    private static void testExistingOverlapGapTopHeightRejectionUnaffectedByPositivityGuard() throws Exception {
        ComponentLayoutConverter converter = new ComponentLayoutConverter();

        List<Element> overlapping = twoColumns(0, 300, 250, 700, 0, 400);
        assertEquals("positivity-guard-unaffected(overlap): status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(overlapping));

        List<Element> gapped = twoColumns(0, 300, 400, 600, 0, 400);
        assertEquals("positivity-guard-unaffected(gap): status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(gapped));

        Document doc = newDocument();
        Element mismatchedHeight = newDiv(doc, 300, 0, 700, 300);
        Element leading = newDiv(doc, 0, 0, 300, 400);
        List<Element> mismatchedTopHeight = Arrays.asList(leading, mismatchedHeight);
        assertEquals("positivity-guard-unaffected(top-height mismatch): status", "GEOMETRY_INVALID",
                converter.classifyColumnRatioGeometry(mismatchedTopHeight));
    }

    // ---- fixture 생성 도우미 ----

    private static List<Element> twoColumns(
            double left1, double width1, double left2, double width2, double top, double height) throws Exception {
        Document doc = newDocument();
        Element div1 = newDiv(doc, left1, top, width1, height);
        Element div2 = newDiv(doc, left2, top, width2, height);
        return Arrays.asList(div1, div2);
    }

    private static List<Element> threeColumns(
            double left1, double width1, double left2, double width2, double left3, double width3,
            double top, double height) throws Exception {
        Document doc = newDocument();
        Element div1 = newDiv(doc, left1, top, width1, height);
        Element div2 = newDiv(doc, left2, top, width2, height);
        Element div3 = newDiv(doc, left3, top, width3, height);
        return Arrays.asList(div1, div2, div3);
    }

    private static Element newDiv(Document doc, double left, double top, double width, double height) {
        Element div = doc.createElement("Div");
        div.setAttribute("left", formatAttr(left));
        div.setAttribute("top", formatAttr(top));
        div.setAttribute("width", formatAttr(width));
        div.setAttribute("height", formatAttr(height));
        return div;
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

    // ---- assertion 검증(no JUnit) ----

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertNull(String label, Object actual) {
        if (actual != null) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=null actual=" + actual);
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
}
