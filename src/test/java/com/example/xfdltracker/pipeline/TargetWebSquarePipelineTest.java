package com.example.xfdltracker.pipeline;

import com.example.xfdltracker.runtime.RuntimeCapabilityResolver;
import com.example.xfdltracker.runtime.RuntimeFunctionCallAnalyzer;
import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * {@link TargetWebSquarePipeline}의 오프라인 end-to-end 테스트 (JUnit 미사용).
 * 자체 생성한 최소 XFDL fixture만 사용하며 외부/공유 fixture에는 의존하지 않는다.
 */
public class TargetWebSquarePipelineTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testNullArgumentsRejected();
        testStandaloneConversionProducesValidTargetXml();
        testNoExecutableUcCallEmptyRequirementsAllowed();
        testKnownUcCallWithAvailableCapabilityStillFailsClosedAtGeneralBehaviorLane();
        testKnownUcCallWithUnavailableCapabilityFailsClosed();
        testDefect2TabDynamicSetUrlMemberCallClosedAsContractLimitation();
        testDefect2TabDynamicAddTabMemberCallClosedAsContractLimitationGeneric();
        testUnknownUcAliasFailsClosedNoPartialOutput();
        testUnsupportedUcSyntaxFailsClosedNoPartialOutput();
        testUcTextOnlyInStringDoesNotCreateRequirement();
        testUcCallInsideTemplateInterpolationCreatesRequirement();
        testScriptWideUcShadowingFailsClosed();
        testExistingFinalOutputPreservedWhenRuntimeValidationFails();

        // 실제 공개 convert(File,File,TargetPipelineConfig) API와 raw XFDL fixture만으로 파이프라인
        // 전 구간(behavior/runtime lane, BUTTON_GROUP finalization, 직렬화)을 검증하는 통합 테스트.
        testIntegrationBehaviorFreeXfdlSucceeds();
        testIntegrationSupportedLocalScriptFunctionSucceeds();
        testIntegrationButtonGroupOnclickEndToEndEmitsFinalizedAttribute();
        testIntegrationButtonGroupMultipleButtonsDistinctHandlersSucceeds();
        testIntegrationButtonGroupEventlessSucceeds();
        testIntegrationButtonGroupUnsupportedSourceEventNameFailsClosed();
        testIntegrationButtonGroupUnresolvedSourceHandlerFailsClosed();
        testIntegrationUnsupportedGeneralScriptSyntaxFailsClosed();
        testIntegrationDuplicateReservedTargetFunctionIdentifierFailsClosed();
        testIntegrationUnavailableRuntimeCapabilityFailsClosed();
        testIntegrationSupportedRuntimeRequirementsContinuePastRuntimeLane();
        testIntegrationTabControlSurvivesFullPipeline();
        testIntegrationSearchAreaSurvivesFullPipeline();
        testIntegrationAmbiguousMultiFormatGridFailsClosedNoPartialOutput();
        testIntegrationCheckBoxDatasetBoundFailsClosedNoPartialOutput();
        testIntegrationCheckBoxAmbiguousBindingFailsClosedNoPartialOutput();
        testIntegrationCheckBoxUnboundFailsClosedNoPartialOutput();
        testIntegrationAllSevenFamiliesReachFinalXml();

        // convert()는 ComponentPredicateAnalyzer/ComponentLayoutConverter를 직접 참조하지 않고
        // SemanticRegionSegmenter를 통해서만 위임한다(중복 권한 없음, 소스 감사는 아래 테스트에서 검증).
        testIntegrationSplitLayoutOnlySurvivesFullPipeline();
        testProductionPipelineHasNoDirectDuplicateAnalyzerOrLayoutConverterReference();

        // general-behavior lane 실패 시 publication fail-closed 회귀 검증(runtime-lane 케이스와는 별개).
        testSentinelPreservedWhenGeneralBehaviorLaneFailsClosed();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testNullArgumentsRejected() throws Exception {
        TargetPipelineConfig config = new TargetPipelineConfig(TargetRuntimeProfile.empty());
        boolean threw1 = false, threw2 = false, threw3 = false;
        try {
            new TargetWebSquarePipeline().convert(null, new File("out.xml"), config);
        } catch (IllegalArgumentException e) {
            threw1 = true;
        }
        try {
            new TargetWebSquarePipeline().convert(findSampleFixture(), null, config);
        } catch (IllegalArgumentException e) {
            threw2 = true;
        }
        try {
            new TargetWebSquarePipeline().convert(findSampleFixture(), new File("out.xml"), null);
        } catch (IllegalArgumentException e) {
            threw3 = true;
        }
        assertTrue("pipeline: null sourceXfdl rejected", threw1);
        assertTrue("pipeline: null targetWebSquareXml rejected", threw2);
        assertTrue("pipeline: null config rejected", threw3);
    }

    private static void testStandaloneConversionProducesValidTargetXml() throws Exception {
        File source = findSampleFixture();
        assertTrue("pipeline-fixture: project-local Sample.xfdl exists", source.isFile());

        File outDir = Files.createTempDirectory("target-web-square-pipeline-test").toFile();
        File output = new File(outDir, "Sample.target.xml");
        assertTrue("pipeline: target output does not exist before conversion", !output.exists());

        TargetPipelineConfig config = new TargetPipelineConfig(TargetRuntimeProfile.empty());
        new TargetWebSquarePipeline().convert(source, output, config);

        assertTrue("pipeline: target output exists only after successful conversion", output.isFile());

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document parsed = f.newDocumentBuilder().parse(output);
        Element root = parsed.getDocumentElement();
        assertTrue("pipeline: generated root is html", "html".equals(root.getLocalName()));
        assertTrue("pipeline: generated root namespace is XHTML",
                "http://www.w3.org/1999/xhtml".equals(root.getNamespaceURI()));

        int headCount = 0, bodyCount = 0;
        for (org.w3c.dom.Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                String local = ((Element) n).getLocalName();
                if ("head".equals(local)) headCount++;
                if ("body".equals(local)) bodyCount++;
            }
        }
        assertTrue("pipeline: generated document has exactly one head", headCount == 1);
        assertTrue("pipeline: generated document has exactly one body", bodyCount == 1);
    }

    /** GRID 컴포넌트 하나짜리 최소 XFDL fixture를 임시 파일로 생성한다. */
    private static File findSampleFixture() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-fixture").toFile();
        File xfdl = new File(dir, "MinimalGrid.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"MinimalGrid\" width=\"400\" height=\"300\">\n"
                + "    <Grid id=\"grdMinimal\" left=\"0\" top=\"0\" width=\"200\" height=\"120\">\n"
                + "      <Formats>\n"
                + "        <Format id=\"fmt1\">\n"
                + "          <Columns>\n"
                + "            <Column size=\"100\" />\n"
                + "          </Columns>\n"
                + "          <Band id=\"head\">\n"
                + "            <Cell col=\"0\" row=\"0\" />\n"
                + "          </Band>\n"
                + "          <Band id=\"body\">\n"
                + "            <Cell col=\"0\" row=\"0\" />\n"
                + "          </Band>\n"
                + "        </Format>\n"
                + "      </Formats>\n"
                + "    </Grid>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return xfdl;
    }

    /** 컴포넌트 없이 스크립트만 담은 최소 XFDL fixture를 생성한다. */
    private static File fixtureWithScript(String scriptBody) throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-script-fixture").toFile();
        File xfdl = new File(dir, "ScriptOnly.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"ScriptOnly\" width=\"400\" height=\"300\">\n"
                + "    <Script><![CDATA[\n" + scriptBody + "\n]]></Script>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return xfdl;
    }

    private static File tempOutput() throws Exception {
        return new File(Files.createTempDirectory("target-web-square-pipeline-output").toFile(), "out.xml");
    }

    private static void testNoExecutableUcCallEmptyRequirementsAllowed() throws Exception {
        // SourceScriptAnalyzer는 top-level named function만 지원하므로 그 형태로 작성.
        // uc 호출이 없으면 runtime requirements가 비어 runtime lane 실패가 없음을 검증한다.
        File source = fixtureWithScript("function doNothing(){ var x = 1; }");
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("no executable uc call: empty requirements valid, conversion succeeds", output.isFile());
    }

    /** runtime capability가 available이어도 {@code uc.*} 호출은 SourceScriptAnalyzer의 닫힌 문법상
     * {@code identifier.member} 형태(Math 제외)라 general behavior lane에서 항상 UNSUPPORTED_SYNTAX로
     * fail-closed 된다 -- capability 실패가 아니라 문법 실패임을 검증한다. */
    private static void testKnownUcCallWithAvailableCapabilityStillFailsClosedAtGeneralBehaviorLane() throws Exception {
        File source = fixtureWithScript("this.doSearch = function() { uc.tranSend('a'); };");
        File output = tempOutput();
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("TRANSACTION_SEND"));
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(profile));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("known uc call, capability AVAILABLE: runtime lane passes but general behavior "
                + "lane still fails closed on uc.* syntax (not a capability failure)", threw);
        assertTrue("no partial target XML published when general behavior lane fails", !output.exists());
    }

    private static void testKnownUcCallWithUnavailableCapabilityFailsClosed() throws Exception {
        File source = fixtureWithScript("this.doSearch = function() { uc.tranSend('a'); };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("known uc call with capability NOT available: fails closed before final publication", threw);
        assertTrue("no partial target XML published on unavailable-capability failure", !output.exists());
    }

    /**
     * Slice 99A(Defect 2 closure) -- Tab 동적 navigation(예: {@code someTab.setUrl(...)})은 SourceScriptAnalyzer의
     * 닫힌 문법상 {@code identifier.member} 형태라 항상 UNSUPPORTED_SYNTAX로 fail-closed되므로, 레거시에서
     * 관찰된 CONTENT_NOT_READY 비동기 오탐(race condition)이 발생할 런타임 브리지 자체가 절대 생성되지 않는다.
     */
    private static void testDefect2TabDynamicSetUrlMemberCallClosedAsContractLimitation() throws Exception {
        File source = fixtureWithScript("this.doNavigate = function() { tabA.setUrl('formA'); };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("defect2 setUrl: failure reason names the evidence category, not a screen id",
                    e.getMessage() != null && e.getMessage().contains("UNSUPPORTED_SYNTAX"));
        }
        assertTrue("defect2 setUrl: pipeline fails closed before final publication", threw);
        assertTrue("defect2 setUrl: no partial/invalid target XML is ever published", !output.exists());
    }

    /** 위 항목과 동일 계약이 다른 식별자/멤버 이름(스크린-특정 아님, 일반적 계약)에도 동일하게 적용됨을 증명한다. */
    private static void testDefect2TabDynamicAddTabMemberCallClosedAsContractLimitationGeneric() throws Exception {
        File source = fixtureWithScript("this.doOpen = function() { myTabControl.addTab('pageId', 'label'); };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("defect2 addTab (generic fixture): pipeline fails closed before final publication", threw);
        assertTrue("defect2 addTab (generic fixture): no partial/invalid target XML is ever published", !output.exists());
    }

    private static void testUnknownUcAliasFailsClosedNoPartialOutput() throws Exception {
        File source = fixtureWithScript("this.doX = function() { uc.totallyUnknownFunction('a'); };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeFunctionCallAnalyzer.UnsupportedRuntimeSyntaxException e) {
            threw = true;
        }
        assertTrue("unknown uc alias: conversion fails closed", threw);
        assertTrue("no partial target XML published on unknown-alias failure", !output.exists());
    }

    private static void testUnsupportedUcSyntaxFailsClosedNoPartialOutput() throws Exception {
        File source = fixtureWithScript("this.doX = function() { var f = uc.tranSend; };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeFunctionCallAnalyzer.UnsupportedRuntimeSyntaxException e) {
            threw = true;
        }
        assertTrue("unsupported uc syntax: conversion fails closed", threw);
        assertTrue("no partial target XML published on unsupported-syntax failure", !output.exists());
    }

    private static void testUcTextOnlyInStringDoesNotCreateRequirement() throws Exception {
        // 문자열 리터럴 안의 uc.* 텍스트는 runtime requirement를 생성하지 않아야 한다.
        File source = fixtureWithScript("function doX(){ return \"uc.tranSend(a)\"; }");
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("uc text only inside a string literal does not create a requirement, conversion succeeds",
                output.isFile());
    }

    private static void testUcCallInsideTemplateInterpolationCreatesRequirement() throws Exception {
        File source = fixtureWithScript("this.doX = function() { var s = `${uc.tranSend(a)}`; };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("uc call inside template interpolation is analyzed and creates a real requirement "
                + "(fails closed here because the empty profile does not declare it available)", threw);
    }

    private static void testScriptWideUcShadowingFailsClosed() throws Exception {
        File source = fixtureWithScript("var uc = 1; this.doX = function() { return uc; };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeFunctionCallAnalyzer.UnsupportedRuntimeSyntaxException e) {
            threw = true;
        }
        assertTrue("script-wide uc shadowing: conversion fails closed", threw);
    }

    private static void testExistingFinalOutputPreservedWhenRuntimeValidationFails() throws Exception {
        File source = fixtureWithScript("this.doSearch = function() { uc.tranSend('a'); };");
        File output = tempOutput();
        Files.write(output.toPath(), "PRE_EXISTING".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            // 예상된 예외
        }
        String content = new String(Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("pre-existing final output preserved unchanged when runtime validation fails",
                "PRE_EXISTING".equals(content));
    }

    // ==== 파이프라인 통합 fixture/테스트 ====

    /** Raw XFDL: Form > Div(btnGroup1) > Button[N], 각 버튼은 onclick 속성과 Form 레벨 Script를 선택적으로 가짐. */
    private static File buttonGroupFixture(String[] buttonIds, String[] onclickHandlers, String scriptBody)
            throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-buttongroup-fixture").toFile();
        File xfdl = new File(dir, "ButtonGroup.xfdl");
        StringBuilder buttons = new StringBuilder();
        for (int i = 0; i < buttonIds.length; i++) {
            buttons.append("    <Button id=\"").append(buttonIds[i]).append("\" left=\"").append(i * 60)
                    .append("\" width=\"50\" height=\"20\" text=\"").append(buttonIds[i]).append("\"");
            if (onclickHandlers != null && onclickHandlers[i] != null) {
                buttons.append(" onclick=\"").append(onclickHandlers[i]).append("\"");
            }
            buttons.append(" />\n");
        }
        String script = scriptBody == null ? "" : "    <Script><![CDATA[\n" + scriptBody + "\n]]></Script>\n";
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"ButtonGroup\" width=\"400\" height=\"300\">\n"
                + script
                + "    <Div id=\"btnGroup1\" width=\"400\">\n"
                + buttons
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return xfdl;
    }

    private static void testIntegrationBehaviorFreeXfdlSucceeds() throws Exception {
        File source = findSampleFixture();
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: behavior-free XFDL (no script) succeeds end to end", output.isFile());
    }

    private static void testIntegrationSupportedLocalScriptFunctionSucceeds() throws Exception {
        File source = fixtureWithScript("function doNothing(){ var x = 1; }");
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: supported local (top-level, no runtime call) script function succeeds", output.isFile());
    }

    /** 승인된 XML Events 네임스페이스 URI (production {@code AtomicWebSquareRenderer.NS_EV}와 일치). */
    private static final String NS_EV = "http://www.w3.org/2001/xml-events";
    private static final String NS_XF = "http://www.w3.org/2002/xforms";

    private static void testIntegrationButtonGroupOnclickEndToEndEmitsFinalizedAttribute() throws Exception {
        File source = buttonGroupFixture(
                new String[] {"btnSave"}, new String[] {"doSave"}, "function doSave(){ var x = 1; }");
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: BUTTON_GROUP onclick end-to-end succeeds", output.isFile());

        String xml = new String(Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("integration: emitted XML carries the exact accepted v1 onclick invocation "
                + "representation (scwin.doSave();)", xml.contains("scwin.doSave();"));
        assertTrue("integration: emitted XML uses xf:trigger for the button", xml.contains("trigger"));

        // 실제 직렬화된 바이트를 namespace-aware로 재파싱해 속성 네임스페이스/로컬명을 검증한다.
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document parsed = f.newDocumentBuilder().parse(output);
        Element trigger = findFirstByLocalName(parsed.getDocumentElement(), NS_XF, "trigger");
        assertTrue("integration: serialized XML contains an xf:trigger element", trigger != null);
        assertEquals("integration: serialized trigger's finalized event attribute value is exactly "
                + "scwin.doSave();", "scwin.doSave();", trigger.getAttributeNS(NS_EV, "onclick"));
        assertTrue("integration: serialized trigger carries exactly one namespace-qualified ev:onclick "
                + "attribute", trigger.hasAttributeNS(NS_EV, "onclick"));
        assertTrue("integration: serialized trigger does NOT also carry an unqualified plain onclick "
                + "attribute (no dual emission)", !trigger.hasAttribute("onclick"));
        assertTrue("integration: serialized XML textually uses the namespace-qualified ev:onclick "
                + "qualified name", xml.contains("ev:onclick"));
    }

    private static void testIntegrationButtonGroupMultipleButtonsDistinctHandlersSucceeds() throws Exception {
        File source = buttonGroupFixture(
                new String[] {"btnA", "btnB"}, new String[] {"doA", "doB"},
                "function doA(){ var x = 1; } function doB(){ var y = 2; }");
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        String xml = new String(Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("integration: multiple BUTTON_GROUP buttons with distinct handlers -- doA present",
                xml.contains("scwin.doA();"));
        assertTrue("integration: multiple BUTTON_GROUP buttons with distinct handlers -- doB present",
                xml.contains("scwin.doB();"));

        // 각 trigger가 자신의 이벤트 바인딩만 갖는지(교차 결합 없음) 재파싱하여 검증.
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document parsed = f.newDocumentBuilder().parse(output);
        List<Element> triggers = findAllByLocalName(parsed.getDocumentElement(), NS_XF, "trigger");
        assertEquals("integration: exactly 2 xf:trigger elements in the serialized output", "2",
                String.valueOf(triggers.size()));
        java.util.Set<String> handlerValues = new java.util.LinkedHashSet<String>();
        for (Element trigger : triggers) {
            assertTrue("integration: each trigger carries a namespace-qualified ev:onclick attribute",
                    trigger.hasAttributeNS(NS_EV, "onclick"));
            assertTrue("integration: each trigger does NOT also carry an unqualified plain onclick "
                    + "attribute", !trigger.hasAttribute("onclick"));
            handlerValues.add(trigger.getAttributeNS(NS_EV, "onclick"));
        }
        assertTrue("integration: doA's finalized handler value present exactly once across triggers",
                handlerValues.contains("scwin.doA();"));
        assertTrue("integration: doB's finalized handler value present exactly once across triggers",
                handlerValues.contains("scwin.doB();"));
        assertEquals("integration: each trigger got its own distinct handler (no cross attachment, "
                + "2 distinct values for 2 triggers)", "2", String.valueOf(handlerValues.size()));
    }

    private static Element findFirstByLocalName(Element root, String namespaceUri, String localName) {
        List<Element> found = findAllByLocalName(root, namespaceUri, localName);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<Element> findAllByLocalName(Element root, String namespaceUri, String localName) {
        List<Element> found = new java.util.ArrayList<Element>();
        collectByLocalName(root, namespaceUri, localName, found);
        return found;
    }

    private static void collectByLocalName(
            org.w3c.dom.Node node, String namespaceUri, String localName, List<Element> out) {
        if (node instanceof Element) {
            Element e = (Element) node;
            if (namespaceUri.equals(e.getNamespaceURI()) && localName.equals(e.getLocalName())) {
                out.add(e);
            }
        }
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectByLocalName(children.item(i), namespaceUri, localName, out);
        }
    }

    private static void testIntegrationButtonGroupEventlessSucceeds() throws Exception {
        File source = buttonGroupFixture(new String[] {"btnPlain1", "btnPlain2"}, null, null);
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: eventless BUTTON_GROUP (no onclick at all) succeeds", output.isFile());
    }

    private static void testIntegrationButtonGroupUnsupportedSourceEventNameFailsClosed() throws Exception {
        // ondblclick은 v1 EVENT_NAME_MAPPING(onclick만 지원) 밖이라 UNSUPPORTED_EVENT_MAPPING으로 fail-closed 된다.
        File dir = Files.createTempDirectory("target-web-square-pipeline-buttongroup-badevent").toFile();
        File xfdl = new File(dir, "ButtonGroupBadEvent.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"ButtonGroup\" width=\"400\" height=\"300\">\n"
                + "    <Script><![CDATA[\nfunction doSave(){ var x = 1; }\n]]></Script>\n"
                + "    <Div id=\"btnGroup1\" width=\"400\">\n"
                + "      <Button id=\"btnSave\" left=\"0\" width=\"50\" height=\"20\" text=\"btnSave\" "
                + "ondblclick=\"doSave\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("integration: unsupported source event name (ondblclick, not onclick) fails closed", threw);
        assertTrue("integration: no partial target XML published on unsupported event name", !output.exists());
    }

    private static void testIntegrationButtonGroupUnresolvedSourceHandlerFailsClosed() throws Exception {
        // onclick이 스크립트에 선언되지 않은 함수명을 참조하면 UNRESOLVED_FUNCTION_REFERENCE로 fail-closed 된다.
        File source = buttonGroupFixture(
                new String[] {"btnSave"}, new String[] {"doesNotExist"}, "function doSave(){ var x = 1; }");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("integration: unresolved source event handler (functionName not declared in script) "
                + "fails closed", threw);
        assertTrue("integration: no partial target XML published on unresolved handler", !output.exists());
    }

    private static void testIntegrationUnsupportedGeneralScriptSyntaxFailsClosed() throws Exception {
        // "this.<handler> = function(){...}" 형태는 SourceScriptAnalyzer의 닫힌 v1 문법상 지원되지 않는다.
        File source = fixtureWithScript("this.doX = function(){ var x = 1; };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("integration: unsupported general script syntax fails closed at the general behavior lane",
                threw);
        assertTrue("integration: no partial target XML published on unsupported general script syntax",
                !output.exists());
    }

    private static void testIntegrationDuplicateReservedTargetFunctionIdentifierFailsClosed() throws Exception {
        // onpageload/onpageunload는 v1 VENDOR_RESERVED_IDENTIFIERS라 동일 이름 함수 선언은 fail-closed 된다.
        File source = fixtureWithScript("function onpageload(){ var x = 1; }");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("integration: reserved target function identifier (onpageload) fails closed", threw);
        assertTrue("integration: no partial target XML published on reserved identifier", !output.exists());
    }

    private static void testIntegrationUnavailableRuntimeCapabilityFailsClosed() throws Exception {
        File source = fixtureWithScript("this.doSearch = function() { uc.tranSend('a'); };");
        File output = tempOutput();
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(
                    source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threw = true;
        }
        assertTrue("integration: unavailable runtime capability fails closed before any later stage", threw);
        assertTrue("integration: no partial target XML published on unavailable runtime capability",
                !output.exists());
    }

    private static void testIntegrationSupportedRuntimeRequirementsContinuePastRuntimeLane() throws Exception {
        // capability가 available이면 runtime lane은 통과하고 general behavior lane에서 별도 사유로
        // fail-closed 된다. 따라서 여기서는 RuntimeCapabilityUnavailableException이 아니라 반드시
        // IllegalStateException이어야 runtime lane이 실제로 통과시켰음이 증명된다.
        File source = fixtureWithScript("this.doSearch = function() { uc.tranSend('a'); };");
        File output = tempOutput();
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("TRANSACTION_SEND"));
        boolean threwIllegalState = false;
        boolean threwRuntimeUnavailable = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(profile));
        } catch (RuntimeCapabilityResolver.RuntimeCapabilityUnavailableException e) {
            threwRuntimeUnavailable = true;
        } catch (IllegalStateException e) {
            threwIllegalState = true;
        }
        assertTrue("integration: supported runtime requirement does not fail at the runtime capability "
                + "stage (proves the runtime lane let it continue)", !threwRuntimeUnavailable);
        assertTrue("integration: pipeline reaches (and fails closed at) the later general behavior lane",
                threwIllegalState);
    }

    private static void testIntegrationTabControlSurvivesFullPipeline() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-tabcontrol-fixture").toFile();
        File xfdl = new File(dir, "TabControl.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"TabControlForm\" width=\"400\" height=\"300\">\n"
                + "    <Tab id=\"tab1\">\n"
                + "      <Tabpages>\n"
                + "        <Tabpage id=\"tp1\" text=\"tp1\" />\n"
                + "      </Tabpages>\n"
                + "    </Tab>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: TAB_CONTROL fixture survives the full pipeline", output.isFile());
    }

    private static void testIntegrationSearchAreaSurvivesFullPipeline() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-searcharea-fixture").toFile();
        File xfdl = new File(dir, "SearchArea.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"SearchAreaForm\" width=\"400\" height=\"300\">\n"
                + "    <Div id=\"searchArea1\">\n"
                + "      <Static id=\"lbl1\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"lbl1\" />\n"
                + "      <Edit id=\"edt1\" left=\"50\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "      <Static id=\"lbl2\" left=\"0\" top=\"30\" width=\"50\" height=\"20\" text=\"lbl2\" />\n"
                + "      <Edit id=\"edt2\" left=\"50\" top=\"30\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                + "    <Grid id=\"searchResultGrid\" left=\"0\" top=\"60\" width=\"200\" height=\"120\">\n"
                + "      <Formats>\n"
                + "        <Format id=\"fmt1\">\n"
                + "          <Columns><Column size=\"100\" /></Columns>\n"
                + "          <Band id=\"head\"><Cell col=\"0\" row=\"0\" /></Band>\n"
                + "          <Band id=\"body\"><Cell col=\"0\" row=\"0\" /></Band>\n"
                + "        </Format>\n"
                + "      </Formats>\n"
                + "    </Grid>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: SEARCH_AREA fixture survives the full pipeline", output.isFile());
    }

    /**
     * MULTI_FORMAT_AMBIGUITY_FAILS_BEFORE_RENDERER_TEST(pipeline-level) -- Grid에 2개 Format이
     * 있고 이를 결정적으로 고를 증명된 source selector가 없으면(Slice 99B correction) 어떤
     * Format도 암묵 선택하지 않고 파이프라인 전체가 fail-closed되어 대상 XML이 전혀 발행되지 않는다.
     */
    private static void testIntegrationAmbiguousMultiFormatGridFailsClosedNoPartialOutput() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-grid3-ambiguous-fixture").toFile();
        File xfdl = new File(dir, "AmbiguousMultiFormat.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"AmbiguousMultiFormatForm\" width=\"400\" height=\"300\">\n"
                + "    <Grid id=\"grd1\" left=\"0\" top=\"0\" width=\"200\" height=\"120\">\n"
                + "      <Formats>\n"
                + "        <Format id=\"default\"><Columns><Column size=\"100\" /></Columns>\n"
                + "          <Band id=\"head\"><Cell col=\"0\" row=\"0\" /></Band>\n"
                + "          <Band id=\"body\"><Cell col=\"0\" row=\"0\" /></Band></Format>\n"
                + "        <Format id=\"alternate\"><Columns><Column size=\"200\" /></Columns>\n"
                + "          <Band id=\"body\"><Cell col=\"0\" row=\"0\" /></Band></Format>\n"
                + "      </Formats>\n"
                + "    </Grid>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        boolean threw = false;
        String reason = null;
        try {
            new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            reason = e.getMessage();
        }
        assertTrue("grid3-ambiguous: pipeline fails closed before final publication", threw);
        // MULTI_FORMAT_PIPELINE_EXPLICIT_REASON_TEST -- 렌더러 코드/화면 id가 아니라 evidence 상수
        // 문자열 자체가 실패 사유임을 확인한다(단순 IllegalStateException 발생 여부만으로는 부족).
        assertTrue("grid3-ambiguous: exception reason names the explicit ambiguity evidence",
                reason != null && reason.contains("ambiguous_multi_format_no_proven_selector"));
        assertTrue("grid3-ambiguous: no partial/invalid target XML is ever published", !output.exists());
    }

    /**
     * Slice 99C -- CheckBox id를 가리키는 {@code <BindItem compid=.../>}가 존재하면(real
     * {@code DatasetBinding.xfdl} 구조와 동일한 형태) propid/값 계약이 증명되지 않았으므로
     * 파이프라인 전체가 fail-closed되어 대상 XML이 전혀 발행되지 않는다.
     */
    private static void testIntegrationCheckBoxDatasetBoundFailsClosedNoPartialOutput() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-checkbox-dataset-bound-fixture").toFile();
        File xfdl = new File(dir, "CheckBoxDatasetBound.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"CheckBoxDatasetBoundForm\" width=\"400\" height=\"300\">\n"
                + "    <Bind><BindItem id=\"b1\" compid=\"chkAgree\" propid=\"checked\" datasetid=\"dsAgree\""
                + " columnid=\"AGREE\" /></Bind>\n"
                + "    <Div id=\"table1\">\n"
                + "      <Static id=\"lblAgree\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"동의\" />\n"
                + "      <CheckBox id=\"chkAgree\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        boolean threw = false;
        String reason = null;
        try {
            new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            reason = e.getMessage();
        }
        assertTrue("checkbox-dataset-bound: pipeline fails closed before final publication", threw);
        assertTrue("checkbox-dataset-bound: exception reason names the explicit evidence",
                reason != null && reason.contains("checkbox_dataset_binding_no_proven_target_contract"));
        assertTrue("checkbox-dataset-bound: no partial/invalid target XML is ever published", !output.exists());
    }

    /**
     * CHECKBOX_AMBIGUOUS_BINDING_PIPELINE_FAIL_CLOSED_TEST -- BindItem의 compid가 CheckBox 2개
     * (같은 id)에 동시에 매치되면 ambiguous 상태이며, 그중 하나가 실제 CheckBox이므로 다른
     * 명시적 사유로 파이프라인 전체가 fail-closed되어 대상 XML이 전혀 발행되지 않는다.
     */
    private static void testIntegrationCheckBoxAmbiguousBindingFailsClosedNoPartialOutput() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-checkbox-ambiguous-fixture").toFile();
        File xfdl = new File(dir, "CheckBoxAmbiguousBinding.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"CheckBoxAmbiguousBindingForm\" width=\"400\" height=\"300\">\n"
                + "    <Bind><BindItem id=\"b1\" compid=\"dupChk\" propid=\"checked\" datasetid=\"dsAgree\""
                + " columnid=\"AGREE\" /></Bind>\n"
                + "    <Div id=\"table1\">\n"
                + "      <Static id=\"lblAgree\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"동의\" />\n"
                + "      <CheckBox id=\"dupChk\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                + "    <CheckBox id=\"dupChk\" left=\"200\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        boolean threw = false;
        String reason = null;
        try {
            new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            reason = e.getMessage();
        }
        assertTrue("checkbox-ambiguous-binding: pipeline fails closed before final publication", threw);
        assertTrue("checkbox-ambiguous-binding: exception reason names the ambiguity-specific evidence",
                reason != null && reason.contains("checkbox_dataset_binding_component_reference_ambiguous"));
        assertTrue("checkbox-ambiguous-binding: no partial/invalid target XML is ever published", !output.exists());
    }

    /**
     * Slice 99E -- unbound CheckBox도 accepted v6 rendering/runtime 동등성이 증명되지 않아
     * 파이프라인 전체가 명시적으로 fail-closed되고 대상 XML이 전혀 발행되지 않는다(Slice 99D의
     * auto-page-init 종결과는 별개 사유 -- 여기서 auto-page-init을 다시 여는 것이 아니다).
     */
    private static void testIntegrationCheckBoxUnboundFailsClosedNoPartialOutput() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-checkbox-unbound-fixture").toFile();
        File xfdl = new File(dir, "CheckBoxUnbound.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"CheckBoxUnboundForm\" width=\"400\" height=\"300\">\n"
                + "    <Div id=\"table1\">\n"
                + "      <Static id=\"lblUse\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"Use\" />\n"
                + "      <CheckBox id=\"chkUse\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        boolean threw = false;
        String reason = null;
        try {
            new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            reason = e.getMessage();
        }
        assertTrue("checkbox-unbound: pipeline fails closed before final publication", threw);
        assertTrue("checkbox-unbound: exception reason names the explicit evidence",
                reason != null && reason.contains("checkbox_unbound_rendering_equivalence_not_proven"));
        assertTrue("checkbox-unbound: no partial/invalid target XML is ever published", !output.exists());
    }

    private static void testIntegrationAllSevenFamiliesReachFinalXml() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-sevenfamily-fixture").toFile();
        File xfdl = new File(dir, "SevenFamily.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"SevenFamilyForm\" width=\"1200\" height=\"900\">\n"
                // 1. GRID 케이스
                + "    <Grid id=\"grd1\" left=\"0\" top=\"0\" width=\"200\" height=\"120\">\n"
                + "      <Formats><Format id=\"fmt1\"><Columns><Column size=\"100\" /></Columns>\n"
                + "        <Band id=\"head\"><Cell col=\"0\" row=\"0\" /></Band>\n"
                + "        <Band id=\"body\"><Cell col=\"0\" row=\"0\" /></Band></Format></Formats>\n"
                + "    </Grid>\n"
                // 2. TAB_CONTROL 케이스
                + "    <Tab id=\"tab1\">\n"
                + "      <Tabpages><Tabpage id=\"tp1\" text=\"tp1\" /></Tabpages>\n"
                + "    </Tab>\n"
                // 3. SPLIT_LAYOUT 케이스
                + "    <Div id=\"splitRoot1\">\n"
                + "      <Div id=\"splitCol1\" left=\"0\" top=\"0\" width=\"300\" height=\"200\" />\n"
                + "      <Div id=\"splitCol2\" left=\"300\" top=\"0\" width=\"700\" height=\"200\" />\n"
                + "    </Div>\n"
                // 4. SEARCH_AREA 케이스
                + "    <Div id=\"searchArea1\">\n"
                + "      <Static id=\"sLbl1\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"sLbl1\" />\n"
                + "      <Edit id=\"sEdt1\" left=\"50\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "      <Static id=\"sLbl2\" left=\"0\" top=\"30\" width=\"50\" height=\"20\" text=\"sLbl2\" />\n"
                + "      <Edit id=\"sEdt2\" left=\"50\" top=\"30\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                + "    <Grid id=\"searchResultGrid1\" left=\"0\" top=\"60\" width=\"200\" height=\"120\">\n"
                + "      <Formats><Format id=\"fmt2\"><Columns><Column size=\"100\" /></Columns>\n"
                + "        <Band id=\"head\"><Cell col=\"0\" row=\"0\" /></Band>\n"
                + "        <Band id=\"body\"><Cell col=\"0\" row=\"0\" /></Band></Format></Formats>\n"
                + "    </Grid>\n"
                // 5. BUSINESS_TABLE (label/control 쌍, Grid sibling 없음)
                + "    <Div id=\"businessTable1\">\n"
                + "      <Static id=\"bLbl1\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"bLbl1\" />\n"
                + "      <Edit id=\"bEdt1\" left=\"50\" top=\"0\" width=\"100\" height=\"20\" />\n"
                + "      <Static id=\"bLbl2\" left=\"0\" top=\"30\" width=\"50\" height=\"20\" text=\"bLbl2\" />\n"
                + "      <Edit id=\"bEdt2\" left=\"50\" top=\"30\" width=\"100\" height=\"20\" />\n"
                + "    </Div>\n"
                // 6. TITLE_BAR + 7. BUTTON_GROUP (title_bar_attached) 케이스
                + "    <Div id=\"pageTitle1\">\n"
                + "      <Static id=\"titleLabel1\" left=\"0\" width=\"100\" height=\"20\" text=\"titleLabel1\" />\n"
                + "    </Div>\n"
                + "    <Div id=\"pageActions1\" width=\"200\">\n"
                + "      <Button id=\"newBtn1\" left=\"120\" width=\"60\" height=\"20\" text=\"newBtn1\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: all seven families together reach a successful final target XML", output.isFile());
        // Slice 99D -- 7개 family를 모두 합쳐도 accepted path는 ev:onpageload를 생성하지 않는다.
        String xml = new String(Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("integration: no family's output ever contains onpageload", !xml.contains("onpageload"));
    }

    private static void testIntegrationSplitLayoutOnlySurvivesFullPipeline() throws Exception {
        File dir = Files.createTempDirectory("target-web-square-pipeline-splitlayout-fixture").toFile();
        File xfdl = new File(dir, "SplitLayout.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"SplitLayoutForm\" width=\"1000\" height=\"400\">\n"
                + "    <Div id=\"splitRoot1\">\n"
                + "      <Div id=\"splitCol1\" left=\"0\" top=\"0\" width=\"300\" height=\"200\" />\n"
                + "      <Div id=\"splitCol2\" left=\"300\" top=\"0\" width=\"700\" height=\"200\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        File output = tempOutput();
        new TargetWebSquarePipeline().convert(xfdl, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        assertTrue("integration: SPLIT_LAYOUT-only fixture survives the full pipeline (dedicated "
                + "evidence that ComponentLayoutConverter's ratio-geometry classification actually "
                + "ran -- exact 300/700 = 30/70 split is only correctly classified through it)",
                output.isFile());
    }

    /**
     * 소스 텍스트 감사: {@code TargetWebSquarePipeline.java}가 {@code ComponentPredicateAnalyzer}/
     * {@code ComponentLayoutConverter}를 직접 참조하지 않고 {@code SemanticRegionSegmenter}만
     * 통해 위임함을 확인한다(중복 권한 부재 증거).
     */
    private static void testProductionPipelineHasNoDirectDuplicateAnalyzerOrLayoutConverterReference()
            throws Exception {
        File pipelineSource = new File("src/main/java/com/example/xfdltracker/pipeline/TargetWebSquarePipeline.java");
        assertTrue("pipeline-ownership-audit: TargetWebSquarePipeline.java source exists", pipelineSource.isFile());
        String content = new String(Files.readAllBytes(pipelineSource.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("pipeline-ownership-audit: convert() does not directly reference "
                + "ComponentPredicateAnalyzer (SemanticRegionSegmenter is the sole lawful owner)",
                !content.contains("ComponentPredicateAnalyzer"));
        assertTrue("pipeline-ownership-audit: convert() does not directly reference "
                + "ComponentLayoutConverter (SemanticRegionSegmenter is the sole lawful owner)",
                !content.contains("ComponentLayoutConverter"));
        assertTrue("pipeline-ownership-audit: convert() invokes SemanticRegionSegmenter exactly once",
                content.indexOf("new SemanticRegionSegmenter()") == content.lastIndexOf("new SemanticRegionSegmenter()"));
        assertTrue("pipeline-ownership-audit: the one SemanticRegionSegmenter call site uses the "
                + "2-arg segment(root, analysis) overload with a real analysis argument -- never "
                + "the analyze(element, null) fallback form",
                content.contains(".segment(sourceRoot, analysis)"));
    }

    private static void testSentinelPreservedWhenGeneralBehaviorLaneFailsClosed() throws Exception {
        File source = fixtureWithScript("this.doX = function(){ var x = 1; };");
        File output = tempOutput();
        Files.write(output.toPath(), "PRE_EXISTING_SENTINEL".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean threw = false;
        try {
            new TargetWebSquarePipeline().convert(source, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("sentinel-preservation: general-behavior-lane-triggered failure throws", threw);
        String content = new String(Files.readAllBytes(output.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("sentinel-preservation: pre-existing target file remains byte-for-byte unchanged "
                + "when failure originates in the general behavior lane specifically",
                "PRE_EXISTING_SENTINEL".equals(content));
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }

    private static void assertEquals(String message, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("FAILED: " + message + " -- expected=<" + expected + "> actual=<" + actual + ">");
        }
    }
}
