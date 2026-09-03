package com.example.xfdltracker.converter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * GRID-3 -- {@link GridFormatParser#resolveFormat} 다중 Format 계약 offline 단위 테스트(no
 * JUnit). 활성 Format을 고르는 selector로 증명된 evidence가 없으므로, Format이 2개 이상이면
 * 항상 명시적으로 unresolved(ambiguous)여야 함을 증명한다.
 */
public class GridFormatParserMultiFormatTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSingleFormatRegressionUnaffected();
        testMultiFormatFirstEntryHeuristic();
        testMultiFormatDefaultNameHeuristic();
        testMultiFormatOrderIndependence();
        testAmbiguousNoProvenSelectorFailsClosed();
        testDuplicateFormatIdentityFailsClosed();
        testNoFormatsElementUnresolvedNonFatal();
        testEmptyFormatsElementUnresolvedNonFatal();
        testDifferingTopologyBetweenFormatsStillUniformlyAmbiguous();
        testMultiFormatMalformedSecondaryCannotBypassAmbiguityContract();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 단일 Format(count=1) -- 그대로 SINGLE_FORMAT_RESOLVED로 선택된다(기존 회귀 없음). */
    private static void testSingleFormatRegressionUnaffected() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        Element format = oneColumnFormat(doc, "onlyOne", "bind:A");
        formats.appendChild(format);
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("single-format: resolved", selection.isResolved());
        assertEquals("single-format: state", "SINGLE_FORMAT_RESOLVED", selection.getState().name());
        assertEquals("single-format: id", "onlyOne", selection.getFormat().getId());
    }

    /**
     * MULTI_FORMAT_FIRST_ENTRY_HEURISTIC_TEST -- 두 Format 중 첫번째로 선언된 쪽이 선택 근거가
     * 될 수 없음을 증명한다. Grid에 어떤 selector attribute도 없으므로 첫 선언 여부와 무관하게
     * 반드시 ambiguous(unresolved)여야 하며, 어떤 Format도 암묵적으로 골라지지 않는다.
     */
    private static void testMultiFormatFirstEntryHeuristic() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        formats.appendChild(oneColumnFormat(doc, "first", "bind:FIRST"));
        formats.appendChild(oneColumnFormat(doc, "second", "bind:SECOND"));
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("first-entry-heuristic: NOT resolved (first declared is not implicitly chosen)",
                !selection.isResolved());
        assertTrue("first-entry-heuristic: requires explicit upstream ambiguity failure",
                selection.requiresExplicitAmbiguityFailure());
        assertEquals("first-entry-heuristic: state", "MULTI_FORMAT_SELECTION_UNRESOLVED",
                selection.getState().name());
    }

    /**
     * MULTI_FORMAT_DEFAULT_NAME_HEURISTIC_TEST -- id="default"인 Format이 섞여 있어도 이름의
     * 어휘적 의미("default")는 selection 근거가 아니므로 여전히 ambiguous여야 함을 증명한다.
     */
    private static void testMultiFormatDefaultNameHeuristic() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        formats.appendChild(oneColumnFormat(doc, "default", "bind:DEFAULT"));
        formats.appendChild(oneColumnFormat(doc, "alternate", "bind:ALTERNATE"));
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("default-name-heuristic: NOT resolved (id=\"default\" is not implicitly chosen)",
                !selection.isResolved());
        assertEquals("default-name-heuristic: state", "MULTI_FORMAT_SELECTION_UNRESOLVED",
                selection.getState().name());
    }

    /**
     * MULTI_FORMAT_ORDER_INDEPENDENCE_TEST -- 동일한 두 Format을 선언 순서만 뒤바꿔도 결과는
     * 여전히 동일하게 ambiguous(unresolved)다 -- 선언 순서는 결과에 어떤 영향도 주지 않는다.
     */
    private static void testMultiFormatOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element gridForward = doc.createElement("Grid");
        Element formatsForward = doc.createElement("Formats");
        formatsForward.appendChild(oneColumnFormat(doc, "alpha", "bind:A"));
        formatsForward.appendChild(oneColumnFormat(doc, "beta", "bind:B"));
        gridForward.appendChild(formatsForward);

        Document doc2 = newDocument();
        Element gridReversed = doc2.createElement("Grid");
        Element formatsReversed = doc2.createElement("Formats");
        formatsReversed.appendChild(oneColumnFormat(doc2, "beta", "bind:B"));
        formatsReversed.appendChild(oneColumnFormat(doc2, "alpha", "bind:A"));
        gridReversed.appendChild(formatsReversed);

        GridFormatParser.GridFormatSelection forward = new GridFormatParser().resolveFormat(gridForward);
        GridFormatParser.GridFormatSelection reversed = new GridFormatParser().resolveFormat(gridReversed);
        assertTrue("order-independence: forward NOT resolved", !forward.isResolved());
        assertTrue("order-independence: reversed NOT resolved", !reversed.isResolved());
        assertEquals("order-independence: same state regardless of declaration order",
                forward.getState().name(), reversed.getState().name());
    }

    /** 두 Format, selector 없음 -- 애매하므로 어떤 Format도 선택하지 않고 fail-closed 근거를 남긴다. */
    private static void testAmbiguousNoProvenSelectorFailsClosed() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        formats.appendChild(oneColumnFormat(doc, "default", "bind:A"));
        formats.appendChild(oneColumnFormat(doc, "alternate", "bind:B"));
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("ambiguous: NOT resolved (no proven selector)", !selection.isResolved());
        assertTrue("ambiguous: evidence names the missing-selector category, not a screen id",
                selection.getEvidence().startsWith("ambiguous_multi_format_no_proven_selector"));
    }

    /** 같은 id를 가진 Format이 2개 이상이면(구조적 identity 충돌) selector 존재 여부와 무관하게 fail-closed된다. */
    private static void testDuplicateFormatIdentityFailsClosed() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        formats.appendChild(oneColumnFormat(doc, "dup", "bind:A"));
        formats.appendChild(oneColumnFormat(doc, "dup", "bind:B"));
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("duplicate-identity: NOT resolved", !selection.isResolved());
        assertEquals("duplicate-identity: state", "DUPLICATE_FORMAT_IDENTITY", selection.getState().name());
        assertTrue("duplicate-identity: evidence names duplicate_format_identity",
                selection.getEvidence().startsWith("duplicate_format_identity"));
        assertTrue("duplicate-identity: requires explicit upstream ambiguity failure",
                selection.requiresExplicitAmbiguityFailure());
    }

    /** Formats 요소 자체가 없으면 NO_FORMAT_DEFINITION(non-fatal) -- 기존 no-Format 회귀와 동일 계열. */
    private static void testNoFormatsElementUnresolvedNonFatal() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("no-formats-element: NOT resolved", !selection.isResolved());
        assertEquals("no-formats-element: state", "NO_FORMAT_DEFINITION", selection.getState().name());
        assertTrue("no-formats-element: does NOT require explicit ambiguity failure (legitimate absence)",
                !selection.requiresExplicitAmbiguityFailure());
    }

    /** Formats 요소는 있으나 Format 자식이 하나도 없으면 마찬가지로 NO_FORMAT_DEFINITION(non-fatal). */
    private static void testEmptyFormatsElementUnresolvedNonFatal() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("empty-formats-element: NOT resolved", !selection.isResolved());
        assertEquals("empty-formats-element: state", "NO_FORMAT_DEFINITION", selection.getState().name());
        assertTrue("empty-formats-element: does NOT require explicit ambiguity failure",
                !selection.requiresExplicitAmbiguityFailure());
    }

    /** 두 Format의 구조 topology(column 개수)가 서로 달라도 여전히 균일하게 ambiguous하다(비대칭이 선택 근거가 되지 않음). */
    private static void testDifferingTopologyBetweenFormatsStillUniformlyAmbiguous() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");

        Element narrow = doc.createElement("Format");
        narrow.setAttribute("id", "narrow");
        Element narrowColumns = doc.createElement("Columns");
        Element narrowColumn = doc.createElement("Column");
        narrowColumn.setAttribute("size", "300");
        narrowColumns.appendChild(narrowColumn);
        narrow.appendChild(narrowColumns);
        formats.appendChild(narrow);

        Element wide = doc.createElement("Format");
        wide.setAttribute("id", "wide");
        Element wideColumns = doc.createElement("Columns");
        int[] sizes = {80, 120, 100};
        for (int size : sizes) {
            Element c = doc.createElement("Column");
            c.setAttribute("size", String.valueOf(size));
            wideColumns.appendChild(c);
        }
        wide.appendChild(wideColumns);
        formats.appendChild(wide);

        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("differing-topology: NOT resolved despite structural asymmetry", !selection.isResolved());
        assertEquals("differing-topology: state", "MULTI_FORMAT_SELECTION_UNRESOLVED", selection.getState().name());
    }

    /**
     * MULTI_FORMAT_MALFORMED_SECONDARY_CANNOT_BYPASS_AMBIGUITY_CONTRACT_TEST -- 두번째 Format이
     * malformed 좌표(숫자 아닌 col/row)를 가져도, 다중 Format은 애초에 topology를 parse하지
     * 않으므로 malformed 여부와 무관하게 여전히 명시적으로 ambiguous(unresolved)다.
     */
    private static void testMultiFormatMalformedSecondaryCannotBypassAmbiguityContract() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        Element formats = doc.createElement("Formats");
        formats.appendChild(oneColumnFormat(doc, "wellFormed", "bind:OK"));

        Element malformed = doc.createElement("Format");
        malformed.setAttribute("id", "malformed");
        Element malformedColumns = doc.createElement("Columns");
        Element malformedColumn = doc.createElement("Column");
        malformedColumn.setAttribute("size", "100");
        malformedColumns.appendChild(malformedColumn);
        malformed.appendChild(malformedColumns);
        Element malformedBand = doc.createElement("Band");
        malformedBand.setAttribute("id", "body");
        Element malformedCell = doc.createElement("Cell");
        malformedCell.setAttribute("col", "not-a-number");
        malformedCell.setAttribute("row", "also-not-a-number");
        malformedCell.setAttribute("text", "bind:MALFORMED");
        malformedBand.appendChild(malformedCell);
        malformed.appendChild(malformedBand);
        formats.appendChild(malformed);
        grid.appendChild(formats);

        GridFormatParser.GridFormatSelection selection = new GridFormatParser().resolveFormat(grid);
        assertTrue("malformed-secondary: NOT resolved despite malformed secondary Format",
                !selection.isResolved());
        assertEquals("malformed-secondary: state", "MULTI_FORMAT_SELECTION_UNRESOLVED",
                selection.getState().name());
        assertTrue("malformed-secondary: requires explicit upstream ambiguity failure",
                selection.requiresExplicitAmbiguityFailure());
        assertTrue("malformed-secondary: well-formed first Format is not implicitly selected",
                selection.getFormat() == null);
    }

    private static Element oneColumnFormat(Document doc, String id, String bodyCellText) {
        Element format = doc.createElement("Format");
        format.setAttribute("id", id);
        Element columns = doc.createElement("Columns");
        Element column = doc.createElement("Column");
        column.setAttribute("size", "100");
        columns.appendChild(column);
        format.appendChild(columns);
        Element body = doc.createElement("Band");
        body.setAttribute("id", "body");
        Element cell = doc.createElement("Cell");
        cell.setAttribute("text", bodyCellText);
        body.appendChild(cell);
        format.appendChild(body);
        return format;
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

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + label);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
