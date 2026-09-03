package com.example.xfdltracker.behavior;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TargetScriptDocumentIntegrator}에 대한 오프라인, 의존성 없는(no JUnit) 테스트 --
 * 일반 Source-Script 동작 검증 영역.
 */
public class TargetScriptDocumentIntegratorTest {

    private static int failures = 0;
    private static final String NS = "http://www.w3.org/1999/xhtml";

    public static void main(String[] args) throws Exception {
        testOneFunctionEmitsOneScriptElement();
        testScriptParentIsHead();
        testNamespaceExactlyXhtml();
        testTypeAttributeExact();
        testContentIsCdata();
        testExactScwinAssignmentPattern();
        testParameterOrderExact();
        testMultipleFunctionsPreserveArtifactOrder();
        testZeroFunctionsAddsNoScriptElement();
        testExistingBodyContentUnchanged();
        testNoSourceMetadataEmitted();
        testNoRawSourceScriptCopied();
        testMissingHeadFailsClosed();
        testArtifactNotMutated();
        testNullDocumentFailsClosed();
        testNullArtifactFailsClosed();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static Document newCompletedTargetDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element html = doc.createElementNS(NS, "html");
        doc.appendChild(html);
        html.appendChild(doc.createElementNS(NS, "head"));
        Element body = doc.createElementNS(NS, "body");
        Element marker = doc.createElementNS("http://www.inswave.com/websquare", "w2:dfbox");
        marker.setAttribute("id", "markerX");
        body.appendChild(marker);
        html.appendChild(body);
        return doc;
    }

    private static TargetScriptArtifact oneFunctionArtifact(String identifier, List<String> params, String finalizedBody) {
        List<TargetScwinFunctionModel> functions = new ArrayList<TargetScwinFunctionModel>();
        functions.add(new TargetScwinFunctionModel(identifier, params, finalizedBody));
        return new TargetScriptArtifact(functions);
    }

    private static Element findScriptElement(Document doc) {
        Element head = (Element) doc.getDocumentElement().getFirstChild();
        Node child = head.getFirstChild();
        while (child != null) {
            if (child instanceof Element && "script".equals(((Element) child).getLocalName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static void testOneFunctionEmitsOneScriptElement() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element head = (Element) doc.getDocumentElement().getFirstChild();
        int scriptCount = 0;
        Node child = head.getFirstChild();
        while (child != null) {
            if (child instanceof Element && "script".equals(((Element) child).getLocalName())) { scriptCount++; }
            child = child.getNextSibling();
        }
        assertEquals("one-function: script element count", "1", String.valueOf(scriptCount));
    }

    private static void testScriptParentIsHead() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        Element head = (Element) doc.getDocumentElement().getFirstChild();
        assertTrue("script-parent: is head", script.getParentNode() == head);
    }

    private static void testNamespaceExactlyXhtml() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        assertEquals("namespace: exact", NS, script.getNamespaceURI());
    }

    private static void testTypeAttributeExact() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        assertEquals("type-attribute: exact", "javascript", script.getAttribute("type"));
    }

    private static void testContentIsCdata() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        assertTrue("content-cdata: first child is CDATASection", script.getFirstChild() instanceof CDATASection);
    }

    private static void testExactScwinAssignmentPattern() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact(
                "myHandler", java.util.Arrays.asList("a", "b"), "return (a + b);");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        String content = script.getFirstChild().getNodeValue();
        assertEquals("scwin-pattern: exact", "scwin.myHandler = function(a, b){ return (a + b); };", content);
    }

    private static void testParameterOrderExact() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact(
                "f", java.util.Arrays.asList("x", "y", "z"), "");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        String content = findScriptElement(doc).getFirstChild().getNodeValue();
        assertTrue("param-order: exact substring present", content.contains("function(x, y, z){"));
    }

    private static void testMultipleFunctionsPreserveArtifactOrder() throws Exception {
        Document doc = newCompletedTargetDocument();
        List<TargetScwinFunctionModel> functions = new ArrayList<TargetScwinFunctionModel>();
        functions.add(new TargetScwinFunctionModel("z", Collections.<String>emptyList(), ""));
        functions.add(new TargetScwinFunctionModel("a", Collections.<String>emptyList(), ""));
        TargetScriptArtifact artifact = new TargetScriptArtifact(functions);
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        String content = findScriptElement(doc).getFirstChild().getNodeValue();
        int zIndex = content.indexOf("scwin.z");
        int aIndex = content.indexOf("scwin.a");
        assertTrue("multi-order: z before a (artifact order, not alphabetized)", zIndex >= 0 && zIndex < aIndex);
    }

    private static void testZeroFunctionsAddsNoScriptElement() throws Exception {
        Document doc = newCompletedTargetDocument();
        new TargetScriptDocumentIntegrator().integrate(doc, TargetScriptArtifact.empty());
        assertTrue("zero-functions: no script element added", findScriptElement(doc) == null);
    }

    private static void testExistingBodyContentUnchanged() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element body = (Element) doc.getDocumentElement().getChildNodes().item(1);
        assertEquals("body-unchanged: localName", "body", body.getLocalName());
        Element marker = (Element) body.getFirstChild();
        assertEquals("body-unchanged: marker id preserved", "markerX", marker.getAttribute("id"));
    }

    private static void testNoSourceMetadataEmitted() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element script = findScriptElement(doc);
        assertEquals("no-source-metadata: no attributes besides type", "1", String.valueOf(script.getAttributes().getLength()));
    }

    private static void testNoRawSourceScriptCopied() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact(
                "foo", Collections.<String>emptyList(), "return scwin.helper(1);");
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        String content = findScriptElement(doc).getFirstChild().getNodeValue();
        assertTrue("no-raw-source: content is exactly the finalized body wrapper, nothing extraneous",
                content.equals("scwin.foo = function(){ return scwin.helper(1); };"));
    }

    private static void testMissingHeadFailsClosed() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().newDocument();
        Element html = doc.createElementNS(NS, "html");
        doc.appendChild(html); // head 자식이 전혀 없는 경우
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        boolean threw = false;
        try {
            new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue("missing-head: fails closed", threw);
    }

    private static void testArtifactNotMutated() throws Exception {
        Document doc = newCompletedTargetDocument();
        TargetScriptArtifact artifact = oneFunctionArtifact("foo", Collections.<String>emptyList(), "return 1;");
        int before = artifact.getFunctionsInOrder().size();
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        assertEquals("artifact-not-mutated: function count unchanged", String.valueOf(before),
                String.valueOf(artifact.getFunctionsInOrder().size()));
    }

    private static void testNullDocumentFailsClosed() {
        TargetScriptArtifact artifact = TargetScriptArtifact.empty();
        boolean threw = false;
        try {
            new TargetScriptDocumentIntegrator().integrate(null, artifact);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue("null-document: fails closed", threw);
    }

    private static void testNullArtifactFailsClosed() throws Exception {
        Document doc = newCompletedTargetDocument();
        boolean threw = false;
        try {
            new TargetScriptDocumentIntegrator().integrate(doc, null);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue("null-artifact: fails closed", threw);
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
