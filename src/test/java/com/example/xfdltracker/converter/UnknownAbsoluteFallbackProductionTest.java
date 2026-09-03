package com.example.xfdltracker.converter;

import com.example.xfdltracker.XfdlFunctionTracker;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.parser.XfdlReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 실제 production 변환 entrypoint({@link WebSquareGenerator#generate})로
 * unknown-absolute-fallback fixture를 검증하는 offline 테스트. root Layout은 무조건
 * pass-through이므로 fallback 분기는 non-root(nested) Layout으로 거치게 한다.
 */
public class UnknownAbsoluteFallbackProductionTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testIrregularRegionGeometryIsGenuinelyNonTable();
        testProductionEntrypointFallsBackToAbsoluteWithoutForcingCanonicalFamily();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /**
     * production {@link ComponentLayoutConverter#classifyLayoutGeometry}를 그대로 호출해, 이
     * fixture가 GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED 플래그와 무관하게 진짜 비규칙적
     * geometry임을 독립적으로 증명한다.
     */
    private static void testIrregularRegionGeometryIsGenuinelyNonTable() throws Exception {
        File xfdlFile = writeSyntheticFixture();
        Document source = new XfdlReader().read(xfdlFile);
        Element freeLayout = findById(source.getDocumentElement(), "freeLayout");
        assertTrue("irregular-geometry: freeLayout element found in parsed source", freeLayout != null);

        List<Element> children = directElementChildren(freeLayout);
        assertEquals("irregular-geometry: 4 synthetic children present", "4", String.valueOf(children.size()));

        String classification = new ComponentLayoutConverter().classifyLayoutGeometry(children);
        assertEquals("irregular-geometry: classifyLayoutGeometry itself reports ABSOLUTE_LAYOUT_FALLBACK "
                        + "for this region (genuinely irregular by that method's own overlap contract, "
                        + "independent of the GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED flag)",
                "ABSOLUTE_LAYOUT_FALLBACK", classification);
    }

    /**
     * {@link XfdlFunctionTracker#analyze} -> {@link WebSquareGenerator#generate} 두 단계로
     * 실제 production entrypoint를 통해 변환한다. 검증: 변환 성공, canonical table 구조 미생성,
     * 4개 component의 geometry/content가 손실 없이 보존됨.
     */
    private static void testProductionEntrypointFallsBackToAbsoluteWithoutForcingCanonicalFamily() throws Exception {
        File xfdlFile = writeSyntheticFixture();
        File outputFile = new File(xfdlFile.getParentFile(), "UnknownAbsoluteFallbackFixture.out.xml");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        XfdlFunctionTracker tracker = new XfdlFunctionTracker();
        XfdlAnalysisResult analysis = tracker.analyze(xfdlFile);
        // 실제 production entrypoint 그대로 호출 -- 예외 없이 끝나야 "변환 실패 없음".
        new WebSquareGenerator().generate(xfdlFile, outputFile, analysis);

        assertTrue("production-fallback: output file was written", outputFile.isFile());

        Document output = parseXml(outputFile);
        assertTrue("production-fallback: output XML is well-formed and parseable "
                + "(변환 실패 없음)", output != null);

        // canonical table 구조가 강제 생성되면 안 된다(Layout-as-table 변환은 항상
        // tagname="table" xf:group를 만듦).
        List<Element> tableGroups = findAllByTagAndAttr(output, "xf:group", "tagname", "table");
        assertEquals("production-fallback: no canonical table structure forced", "0",
                String.valueOf(tableGroups.size()));

        // freeLayout의 실제 width/height(800x400)를 basis로 기대 style을 산출한다.
        Document source = new XfdlReader().read(xfdlFile);
        Element freeLayout = findById(source.getDocumentElement(), "freeLayout");
        ComponentLayoutConverter layoutConverter = new ComponentLayoutConverter();
        double[] basis = layoutConverter.resolveLayoutBasis(freeLayout);
        assertTrue("production-fallback: freeLayout basis resolves (800x400 declared)", basis != null);

        assertComponentPreserved(output, layoutConverter, source, "staIrregular1", "w2:span", basis,
                "label", "Irregular Label A");
        assertComponentPreserved(output, layoutConverter, source, "edtIrregular2", "xf:input", basis,
                "initValue", "Irregular Value B");
        assertComponentPreserved(output, layoutConverter, source, "imgIrregular3", "w2:image", basis, null, null);
        assertComponentPreserved(output, layoutConverter, source, "divIrregular4", "xf:group", basis, null, null);
    }

    /**
     * source의 {@code sourceId} element를 찾아 {@link ComponentLayoutConverter#buildPercentComponentStyle}로
     * 기대 style을 계산하고, output에 동일 style의 element가 정확히 하나 존재함을 확인한다
     * (geometry preservation). {@code contentAttr}가 있으면 content도 함께 검증한다.
     */
    private static void assertComponentPreserved(
            Document output, ComponentLayoutConverter layoutConverter, Document source,
            String sourceId, String expectedTargetTag, double[] basis,
            String contentAttr, String expectedContentValue) {
        Element sourceEl = findById(source.getDocumentElement(), sourceId);
        assertTrue("preserve[" + sourceId + "]: source element found", sourceEl != null);

        String expectedStyle = layoutConverter.buildPercentComponentStyle(sourceEl, basis[0], basis[1], true);
        assertTrue("preserve[" + sourceId + "]: expected percent style resolvable (basis not unresolved)",
                expectedStyle != null);

        List<Element> matches = findAllByTagAndAttr(output, expectedTargetTag, "style", expectedStyle);
        assertEquals("preserve[" + sourceId + "]: exactly one " + expectedTargetTag
                + " with geometry-preserving style=" + expectedStyle, "1", String.valueOf(matches.size()));

        if (contentAttr != null) {
            String actual = matches.isEmpty() ? null : matches.get(0).getAttribute(contentAttr);
            assertEquals("preserve[" + sourceId + "]: content attribute " + contentAttr + " preserved",
                    expectedContentValue, actual);
        }
    }

    // ---- fixture 생성 도우미 ----

    private static File writeSyntheticFixture() throws Exception {
        File dir = new File("build/test-tmp/unknown-absolute-fallback");
        dir.mkdirs();
        File file = new File(dir, "UnknownAbsoluteFallbackFixture.xfdl");
        String xfdl =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"UnknownAbsoluteFallbackFixture\" width=\"800\" height=\"600\">\n"
                + "    <Div id=\"freeAreaWrapper\" left=\"0\" top=\"0\" width=\"800\" height=\"400\">\n"
                + "      <Layout id=\"freeLayout\" left=\"0\" top=\"0\" width=\"800\" height=\"400\">\n"
                + "        <Static id=\"staIrregular1\" text=\"Irregular Label A\""
                + " left=\"13\" top=\"7\" width=\"140\" height=\"18\" />\n"
                + "        <Edit id=\"edtIrregular2\" value=\"Irregular Value B\""
                + " left=\"512\" top=\"233\" width=\"90\" height=\"22\" />\n"
                + "        <ImageViewer id=\"imgIrregular3\""
                + " left=\"7\" top=\"311\" width=\"64\" height=\"64\" />\n"
                // staIrregular1과 의도적으로 겹치도록 배치 -- classifyLayoutGeometry는 overlap
                // 여부만 검사하므로 이 fixture는 그 계약상 ABSOLUTE_LAYOUT_FALLBACK으로 판정된다.
                + "        <Div id=\"divIrregular4\""
                + " left=\"100\" top=\"10\" width=\"77\" height=\"129\" />\n"
                + "      </Layout>\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Writer writer = new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8);
        try {
            writer.write(xfdl);
        } finally {
            writer.close();
        }
        return file;
    }

    // ---- DOM 생성 도우미 ----

    private static Document parseXml(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(file);
    }

    private static Element findById(Element root, String id) {
        if (id.equals(root.getAttribute("id"))) {
            return root;
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element found = findById((Element) node, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<Element> directElementChildren(Element parent) {
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

    private static List<Element> findAllByTagAndAttr(Document doc, String tagName, String attrName, String attrValue) {
        List<Element> result = new ArrayList<Element>();
        collectByTagAndAttr(doc.getDocumentElement(), tagName, attrName, attrValue, result);
        return result;
    }

    private static void collectByTagAndAttr(
            Element el, String tagName, String attrName, String attrValue, List<Element> out) {
        if (el == null) {
            return;
        }
        if (tagName.equals(el.getTagName()) && attrValue.equals(el.getAttribute(attrName))) {
            out.add(el);
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                collectByTagAndAttr((Element) node, tagName, attrName, attrValue, out);
            }
        }
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
