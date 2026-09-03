package com.example.xfdltracker.behavior;

import com.example.xfdltracker.composition.TargetNodeIdentityKind;
import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadBehaviorFinalizationResult;
import com.example.xfdltracker.payload.TargetPayloadBehaviorFinalizer;
import com.example.xfdltracker.payload.TargetPayloadCategory;
import com.example.xfdltracker.payload.PayloadBehaviorFinalizationStatus;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * local behavior-lane 전체(source script -&gt; analyzer -&gt; translator -&gt; artifact -&gt;
 * document integrator)를 검증하는 offline 테스트. {@code TargetDocumentAssembler}가 만드는 것과
 * 동일한 completed-target-document fixture로 대체하며, Round 2 호환성도 함께 검증한다.
 */
public class BehaviorLaneIntegrationTest {

    private static int failures = 0;
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    public static void main(String[] args) throws Exception {
        testFullSourceToDocumentPipeline();
        testRound2FinalizerCompatibilityWithTranslatedArtifact();
        testFinalizedIdentifierHasConcreteTargetFunctionModelForDocumentEmission();
        testZeroFunctionArtifactUnresolvedEverywhere();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testFullSourceToDocumentPipeline() throws Exception {
        String rawSource = "function computeTotal(a, b){ return a + b; }";

        SourceScriptAnalysisResult analysisResult = new SourceScriptAnalyzer().analyze(rawSource);
        assertEquals("pipeline: analyzer status", "ANALYZED", String.valueOf(analysisResult.getStatus()));

        TargetScriptTranslationResult translationResult = new TargetScriptTranslator().translate(
                analysisResult.getAnalysis());
        assertEquals("pipeline: translator status", "TRANSLATED", String.valueOf(translationResult.getStatus()));

        Document completedTargetDocument = newCompletedTargetDocumentFixture();
        Document result = new TargetScriptDocumentIntegrator().integrate(
                completedTargetDocument, translationResult.getArtifact());
        assertTrue("pipeline: same Document instance returned (no restructuring)", result == completedTargetDocument);

        Element head = (Element) result.getDocumentElement().getFirstChild();
        Element script = null;
        Node child = head.getFirstChild();
        while (child != null) {
            if (child instanceof Element && "script".equals(((Element) child).getLocalName())) {
                script = (Element) child;
            }
            child = child.getNextSibling();
        }
        assertTrue("pipeline: script element present in head", script != null);
        assertEquals("pipeline: exact finalized script content",
                "scwin.computeTotal = function(a, b){ return (a + b); };", script.getFirstChild().getNodeValue());
    }

    private static void testRound2FinalizerCompatibilityWithTranslatedArtifact() throws Exception {
        String rawSource = "function handleClick(){ var noop = 1; }";
        SourceScriptAnalysisResult analysisResult = new SourceScriptAnalyzer().analyze(rawSource);
        assertEquals("round2-compat: analyzer status", "ANALYZED", String.valueOf(analysisResult.getStatus()));
        TargetScriptTranslationResult translationResult = new TargetScriptTranslator().translate(
                analysisResult.getAnalysis());
        assertEquals("round2-compat: translator status", "TRANSLATED", String.valueOf(translationResult.getStatus()));
        TargetScriptArtifact realTranslatedArtifact = translationResult.getArtifact();
        assertTrue("round2-compat: artifact contains handleClick",
                realTranslatedArtifact.containsFinalizedTargetFunctionIdentifier("handleClick"));

        // BUTTON_GROUP source-derived payload/event evidence를 기존 테스트와 동일한 형태로 구성.
        Map<String, Object> buttonStructuredData = new LinkedHashMap<String, Object>();
        buttonStructuredData.put("buttonOrdinal", Integer.valueOf(0));
        TargetLeafPayload buttonLeaf = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Save", buttonStructuredData, "source_text_attribute", "b0");

        Map<String, Object> eventStructuredData = new LinkedHashMap<String, Object>();
        eventStructuredData.put("eventName", "onclick");
        eventStructuredData.put("functionName", "handleClick");
        TargetLeafPayload eventLeaf = new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", eventStructuredData, "event_binding", "b0");

        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(buttonLeaf);
        items.add(eventLeaf);
        TargetNodePayload payload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, "Form[0]/Div[1]", items, Integer.valueOf(1));

        TargetPayloadBehaviorFinalizationResult finalizationResult =
                new TargetPayloadBehaviorFinalizer().finalize(payload, realTranslatedArtifact);
        assertEquals("round2-compat: finalization status", "FINALIZED", String.valueOf(finalizationResult.getStatus()));

        TargetLeafPayload finalizedEventLeaf = null;
        for (TargetLeafPayload item : finalizationResult.getFinalizedPayload().getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) { finalizedEventLeaf = item; }
        }
        assertTrue("round2-compat: finalized event leaf present", finalizedEventLeaf != null);
        assertTrue("round2-compat: finalizedTargetEventBinding present",
                finalizedEventLeaf.getFinalizedTargetEventBinding() != null);
        assertEquals("round2-compat: exact finalized targetFunctionIdentifier", "handleClick",
                finalizedEventLeaf.getFinalizedTargetEventBinding().getTargetFunctionIdentifier());
    }

    /** finalizer가 resolve하는 identifier는 반드시 integrator가 emit 가능한 구체적
     * {@code TargetScwinFunctionModel}을 갖는다는 불변식을 검증한다 -- identifier만 있고
     * 구현이 없는 채로 finalize가 성공하는 결함을 방지한다. */
    private static void testFinalizedIdentifierHasConcreteTargetFunctionModelForDocumentEmission() throws Exception {
        SourceScriptAnalysisResult analysisResult = new SourceScriptAnalyzer().analyze(
                "function handleClick(){ return 1; }");
        assertEquals("completeness: analyzer status", "ANALYZED", String.valueOf(analysisResult.getStatus()));
        TargetScriptTranslationResult translationResult = new TargetScriptTranslator().translate(
                analysisResult.getAnalysis());
        assertEquals("completeness: translator status", "TRANSLATED", String.valueOf(translationResult.getStatus()));
        TargetScriptArtifact artifact = translationResult.getArtifact();

        TargetNodePayload payload = buttonGroupOnclickPayload("handleClick");
        TargetPayloadBehaviorFinalizationResult finalizationResult =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifact);
        assertEquals("completeness: finalization status", "FINALIZED", String.valueOf(finalizationResult.getStatus()));
        TargetLeafPayload finalizedEventLeaf = null;
        for (TargetLeafPayload item : finalizationResult.getFinalizedPayload().getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) { finalizedEventLeaf = item; }
        }
        assertEquals("completeness: resolved targetFunctionIdentifier", "handleClick",
                finalizedEventLeaf.getFinalizedTargetEventBinding().getTargetFunctionIdentifier());

        // 같은 artifact를 integrator에 전달하면 해당 identifier의 구체적 정의를 emit해야 한다.
        Document doc = newCompletedTargetDocumentFixture();
        new TargetScriptDocumentIntegrator().integrate(doc, artifact);
        Element head = (Element) doc.getDocumentElement().getFirstChild();
        Element script = null;
        Node child = head.getFirstChild();
        while (child != null) {
            if (child instanceof Element && "script".equals(((Element) child).getLocalName())) { script = (Element) child; }
            child = child.getNextSibling();
        }
        assertTrue("completeness: script element emitted", script != null);
        String content = script.getFirstChild().getNodeValue();
        assertTrue("completeness: emitted CDATA contains the concrete handleClick definition",
                content.contains("scwin.handleClick = function("));
    }

    /** zero-function artifact는 identifier index, document emission, finalizer
     * (반드시 {@code UNRESOLVED_FUNCTION_REFERENCE}) 어디서도 resolve되지 않아야 한다. */
    private static void testZeroFunctionArtifactUnresolvedEverywhere() throws Exception {
        TargetScriptArtifact emptyArtifact = TargetScriptArtifact.empty();
        assertTrue("zero-function: containsFinalizedTargetFunctionIdentifier false",
                !emptyArtifact.containsFinalizedTargetFunctionIdentifier("handleClick"));

        Document doc = newCompletedTargetDocumentFixture();
        new TargetScriptDocumentIntegrator().integrate(doc, emptyArtifact);
        Element head = (Element) doc.getDocumentElement().getFirstChild();
        assertTrue("zero-function: integrator emits no script element", head.getFirstChild() == null);

        TargetNodePayload payload = buttonGroupOnclickPayload("handleClick");
        TargetPayloadBehaviorFinalizationResult finalizationResult =
                new TargetPayloadBehaviorFinalizer().finalize(payload, emptyArtifact);
        assertEquals("zero-function: finalizer status", "UNRESOLVED_FUNCTION_REFERENCE",
                String.valueOf(finalizationResult.getStatus()));
    }

    private static TargetNodePayload buttonGroupOnclickPayload(String functionName) {
        Map<String, Object> buttonStructuredData = new LinkedHashMap<String, Object>();
        buttonStructuredData.put("buttonOrdinal", Integer.valueOf(0));
        TargetLeafPayload buttonLeaf = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Save", buttonStructuredData, "source_text_attribute", "b0");

        Map<String, Object> eventStructuredData = new LinkedHashMap<String, Object>();
        eventStructuredData.put("eventName", "onclick");
        eventStructuredData.put("functionName", functionName);
        TargetLeafPayload eventLeaf = new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", eventStructuredData, "event_binding", "b0");

        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(buttonLeaf);
        items.add(eventLeaf);
        return new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "Form[0]/Div[1]", items, Integer.valueOf(1));
    }

    private static Document newCompletedTargetDocumentFixture() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().newDocument();
        Element html = doc.createElementNS(XHTML_NS, "html");
        doc.appendChild(html);
        html.appendChild(doc.createElementNS(XHTML_NS, "head"));
        html.appendChild(doc.createElementNS(XHTML_NS, "body"));
        return doc;
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
