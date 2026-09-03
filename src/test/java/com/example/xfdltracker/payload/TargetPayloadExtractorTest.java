package com.example.xfdltracker.payload;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.binding.SourceBindingAnalyzer;
import com.example.xfdltracker.binding.SourceBindingReference;
import com.example.xfdltracker.composition.CompositionDecision;
import com.example.xfdltracker.composition.CompositionEvaluator;
import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetCompositionPlanBuilder;
import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JUnit 없는 의존성 없는 단위 테스트. {@link SemanticRegionSegmenter} -&gt;
 * {@link CompositionEvaluator} -&gt; {@link TargetCompositionPlanBuilder} 순서로 이어지는 -&gt;
 * {@link TargetPayloadExtractor} 전체 파이프라인을 실제로 실행해 검증한다.
 */
public class TargetPayloadExtractorTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTitleBarDisplayText();
        testButtonGroupCaptionAndSafeUniqueEvent();
        testGridColumnEvidence();
        testTabControlLabels();
        testRawOnlyNotPromoted();
        testTargetSyntheticSkipped();
        testCrossWiringSameSourceRegionIdScope();
        testOrderIndependence();

        testAmbiguousEventCorrelationAcrossNonDivScopes();
        testDuplicateIdenticalEventBindingSafelyDeduplicated();
        testConflictingFunctionNameForSameEventAmbiguous();
        testEventEvidenceProvenanceTamperExplicitFailure();
        testEventEvidenceOrderIndependence();

        testEventMissingFunctionNameExplicitFailure();
        testEventBlankEventNameExplicitFailure();
        testEventBlankFunctionNameExplicitFailure();
        testEventWrongKindExplicitFailure();
        testNonEventFunctionNameInjectionExplicitFailure();
        testEventNonButtonTamperExplicitFailure();
        testButtonCaptionNonButtonTamperExplicitFailure();

        testSafeEventInputOrderReverse();
        testDuplicateEventOrderReverse();
        testConflictingHandlerReverse();
        testMultiButtonMixedEventOrder();

        testSearchAreaEvidenceCapturedAtPredicateTime();
        testSearchAreaPayloadFromEvidenceOnly();
        testSearchAreaWrapperNormalizationEvidenceUsesPredicateLeaf();
        testSearchAreaNoEmissionMeansNoEvidence();
        testBusinessTablePayloadFromEvidenceOnly();
        testBindingCollisionNeverGuessed();

        testBusinessTableMissingRegionExplicitFailure();
        testBusinessTableWrongFamilySameAnchorExplicitFailure();
        testBusinessTableDuplicateRegionExplicitFailure();
        testBusinessTableOwnerMismatchExplicitFailure();
        testBusinessTableMissingComponentExplicitFailure();
        testBusinessTableOutsideSubtreeExplicitFailure();
        testBusinessTableWrongKindExplicitFailure();
        testBusinessTableRoleSourceElementMismatchExplicitFailure();
        testSearchAreaCannotUseBusinessTableEvidence();
        testBusinessTableCannotUseSearchAreaEvidence();
        testBusinessTableOrderIndependence();

        testBusinessTableAsymmetricRowsStructuredDataExact();
        testBusinessTableNullRowIndexExplicitFailure();
        testBusinessTableNegativeRowIndexRejectedAtConstruction();
        testBusinessTableConflictingStructuralTupleExplicitFailure();
        testBusinessTableDuplicateRowCellOwnershipExplicitFailure();
        testBusinessTableInconsistentPairMetadataExplicitFailure();
        testBusinessTablePairCardinalityViolationExplicitFailure();
        testBusinessTableStructuralDataOrderIndependence();
        testSearchAreaStructuredDataPreservedExactCopy();
        testSearchAreaMultiRowVariablePairCountProductionPathPreservation();

        testBusinessTableVerticalPlanHorizontalRegionExplicitFailure();
        testBusinessTableHorizontalPlanHorizontalRegionPayloadSucceeds();
        testButtonGroupVariantMismatchExplicitFailure();

        testBusinessTableVerticalVerticalUnemittableExplicitFailure();
        testButtonGroupEmbeddedEmbeddedUnemittableExplicitFailure();
        testButtonGroupFixedFooterFixedFooterUnemittableExplicitFailure();
        testButtonGroupStandaloneVariantPayloadSucceeds();
        testButtonGroupTitleBarAttachedVariantPayloadSucceeds();

        testWrongFamilySameAnchorExplicitFailure();
        testDuplicateRegionExplicitFailure();
        testEvidenceOwnerMismatchExplicitFailure();
        testMissingRegionExplicitFailure();
        testSourceRootNullExplicitFailure();
        testSourceTextAttributeAbsentProducesNoDisplayText();
        testInvalidKindValueComboExplicitFailure();

        testTitleBarEvidenceCapturedAtPredicateTime();
        testTitleBarWrongRoleEvidenceFails();
        testTitleBarOwnerMismatchFails();
        testTitleBarMissingComponentFails();

        testButtonGroupTransparentWrapperEvidenceAndPayload();
        testButtonGroupOpaqueBoundaryNoEvidence();
        testButtonGroupCrossWiringSameIdNestedScope();
        testButtonGroupEvidenceOrderIndependence();

        testTabControlEvidenceCapturedAtPredicateTime();
        testTabControlWrongRoleEvidenceFails();
        testNonexistentSourceComponentExplicitFailure();
        testEvidenceComponentOutsideRegionSubtreeExplicitFailure();
        testDuplicateSourceOrderExplicitFailure();
        testRegionsAndEvidenceOrderIndependence();

        testButtonGroupExpectedButtonCountPropagatedExactly();
        testButtonGroupButtonWithoutTextOrValueRetainsStructuralLeafWithOrdinal();
        testNonButtonGroupPayloadHasNullExpectedStructuralMemberCount();

        testGridAmbiguousMultiFormatFailsClosedBeforeRenderer();

        testCheckBoxDatasetBoundBusinessTableFailsClosedBeforeRenderer();
        testCheckBoxDatasetBoundSearchAreaStructurallyDifferentFixtureFailsClosed();
        testCheckBoxUnboundNoBindDatasetStillSucceeds();
        testCheckBoxBindingFalsePositiveEditTargetDoesNotRejectCheckBox();
        testCheckBoxBindingFalsePositiveOtherCheckBoxTargetDoesNotRejectThisOne();
        testCheckBoxUnrelatedAmbiguousBindingDoesNotContaminate();
        testCheckBoxRelevantAmbiguousBindingFailsClosed();
        testCheckBoxAmbiguousCandidateSetIncludingEditAlsoFailsForCheckBox();
        testCheckBoxUnrelatedAmbiguousEditCandidatesDoNotRejectCheckBox();
        testCheckBoxDatasetBoundNameHeuristicIrrelevant();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- fixture 구성 ----

    /** test-only convenience -- production {@link TargetPayloadExtractor}는 binding evidence를
     *  스스로 계산하지 않으므로, 여기서 {@link SourceBindingAnalyzer}를 먼저 호출해 넘겨준다. */
    private static List<TargetNodePayload> extractWithBindings(
            Element sourceRoot, TargetCompositionPlan plan, List<SemanticRegionResult> regions) {
        List<SourceBindingReference> bindingReferences = sourceRoot == null
                ? new ArrayList<SourceBindingReference>() : new SourceBindingAnalyzer().analyze(sourceRoot);
        return new TargetPayloadExtractor().extract(sourceRoot, plan, regions, bindingReferences);
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    private static final class Fixture {
        final List<SemanticRegionResult> regions;
        final TargetCompositionPlan plan;
        Fixture(List<SemanticRegionResult> regions, TargetCompositionPlan plan) {
            this.regions = regions;
            this.plan = plan;
        }
    }

    private static Fixture buildFixture(Element form) {
        return buildFixtureWithAnalysis(form, null);
    }

    /** EVENT evidence는 predicate-time에 segmenter가 생성하므로, analysis를 EVENT correlation에
     * 반영하려면 segment() 호출 자체에 넘겨야 한다(extract()는 analysis를 받지 않음). */
    private static Fixture buildFixtureWithAnalysis(Element form, XfdlAnalysisResult analysis) {
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form, analysis);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        return new Fixture(regions, plan);
    }

    /**
     * Form 하나 아래: TITLE_BAR(titleBar1), BUTTON_GROUP(btnGroup1), SEARCH_AREA(search1) 바로
     * 뒤에 GRID(grid1)(SEARCH_AREA wrapper-normalization peer search가 grid1을 찾도록), TAB_CONTROL(tab1).
     */
    private static Element buildFixtureForm(Document doc) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", "titleBar1");
        titleBar.setAttribute("width", "400");
        Element titleStatic = doc.createElement("Static");
        titleStatic.setAttribute("id", "titleText1");
        titleStatic.setAttribute("text", "화면 제목");
        titleStatic.setAttribute("left", "0");
        Element closeButton = doc.createElement("Button");
        closeButton.setAttribute("id", "btnClose");
        closeButton.setAttribute("text", "닫기");
        closeButton.setAttribute("left", "350");
        titleBar.appendChild(titleStatic);
        titleBar.appendChild(closeButton);
        form.appendChild(titleBar);

        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroup1");
        btnGroup.setAttribute("width", "200");
        Element saveButton = doc.createElement("Button");
        saveButton.setAttribute("id", "btnSave");
        saveButton.setAttribute("text", "저장");
        saveButton.setAttribute("left", "10");
        Element cancelButton = doc.createElement("Button");
        cancelButton.setAttribute("id", "btnCancel");
        cancelButton.setAttribute("text", "취소");
        cancelButton.setAttribute("left", "60");
        btnGroup.appendChild(saveButton);
        btnGroup.appendChild(cancelButton);
        form.appendChild(btnGroup);

        Element search = doc.createElement("Div");
        search.setAttribute("id", "search1");
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        label1.setAttribute("text", "이름");
        label1.setAttribute("left", "0");
        label1.setAttribute("top", "0");
        label1.setAttribute("width", "50");
        label1.setAttribute("height", "20");
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "editName");
        edit1.setAttribute("left", "60");
        edit1.setAttribute("top", "0");
        edit1.setAttribute("width", "100");
        edit1.setAttribute("height", "20");
        Element label2 = doc.createElement("Static");
        label2.setAttribute("id", "lbl2");
        label2.setAttribute("text", "상태");
        label2.setAttribute("left", "0");
        label2.setAttribute("top", "30");
        label2.setAttribute("width", "50");
        label2.setAttribute("height", "20");
        Element combo1 = doc.createElement("Combo");
        combo1.setAttribute("id", "comboStatus");
        combo1.setAttribute("left", "60");
        combo1.setAttribute("top", "30");
        combo1.setAttribute("width", "100");
        combo1.setAttribute("height", "20");
        search.appendChild(label1);
        search.appendChild(edit1);
        search.appendChild(label2);
        search.appendChild(combo1);
        form.appendChild(search);

        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grid1");
        Element formats = doc.createElement("Formats");
        Element format = doc.createElement("Format");
        format.setAttribute("id", "default");
        Element columns = doc.createElement("Columns");
        Element col1 = doc.createElement("Column");
        col1.setAttribute("size", "80");
        Element col2 = doc.createElement("Column");
        col2.setAttribute("size", "120");
        columns.appendChild(col1);
        columns.appendChild(col2);
        format.appendChild(columns);
        Element headBand = doc.createElement("Band");
        headBand.setAttribute("id", "head");
        Element headCell1 = doc.createElement("Cell");
        headCell1.setAttribute("col", "0");
        headCell1.setAttribute("row", "0");
        headCell1.setAttribute("text", "이름");
        Element headCell2 = doc.createElement("Cell");
        headCell2.setAttribute("col", "1");
        headCell2.setAttribute("row", "0");
        headCell2.setAttribute("text", "상태");
        headBand.appendChild(headCell1);
        headBand.appendChild(headCell2);
        Element bodyBand = doc.createElement("Band");
        bodyBand.setAttribute("id", "body");
        Element bodyCell1 = doc.createElement("Cell");
        bodyCell1.setAttribute("col", "0");
        bodyCell1.setAttribute("row", "0");
        bodyCell1.setAttribute("text", "bind:col1");
        bodyBand.appendChild(bodyCell1);
        format.appendChild(headBand);
        format.appendChild(bodyBand);
        formats.appendChild(format);
        grid.appendChild(formats);
        form.appendChild(grid);

        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        Element page1 = doc.createElement("Tabpage");
        page1.setAttribute("id", "page1");
        page1.setAttribute("text", "첫번째 탭");
        Element page2 = doc.createElement("Tabpage");
        page2.setAttribute("id", "page2");
        page2.setAttribute("titletext", "두번째 탭");
        tab.appendChild(page1);
        tab.appendChild(page2);
        form.appendChild(tab);

        return form;
    }

    private static TargetNodePayload findPayload(List<TargetNodePayload> payloads, String planNodeId) {
        for (TargetNodePayload p : payloads) {
            if (p.getPlanNodeId().equals(planNodeId)) {
                return p;
            }
        }
        return null;
    }

    private static String structuralIdOf(Element element) {
        return SourceStructuralIdentity.build(element);
    }

    // ---- 테스트 ----

    private static void testTitleBarDisplayText() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        Element titleBar = (Element) form.getElementsByTagName("Div").item(0);
        String planNodeId = structuralIdOf(titleBar);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("title_bar: payload present", payload != null);
        assertEquals("title_bar: item count", "1", String.valueOf(payload.getItems().size()));
        TargetLeafPayload item = payload.getItems().get(0);
        assertEquals("title_bar: category", "DISPLAY_TEXT", item.getCategory().name());
        assertEquals("title_bar: value", "화면 제목", item.getValue());
        assertEquals("title_bar: evidence kind", "source_text_attribute", item.getSourceEvidenceKind());
    }

    /** btnGroup1은 유일한 scope이므로 componentId path가 document 전체에서 정확히 1개
     * Element만 가리킨다 -- EVENT가 정확히 1건 생성돼야 한다. */
    private static void testButtonGroupCaptionAndSafeUniqueEvent() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);

        Element btnGroup = (Element) form.getElementsByTagName("Div").item(1);
        String planNodeId = structuralIdOf(btnGroup);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save"));
        Fixture fx = buildFixtureWithAnalysis(form, analysis);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("button_group: payload present", payload != null);

        int displayTextCount = 0;
        int eventCount = 0;
        boolean sawSaveCaption = false;
        boolean sawCancelCaption = false;
        boolean sawSaveEvent = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.DISPLAY_TEXT) {
                displayTextCount++;
                if ("저장".equals(item.getValue())) sawSaveCaption = true;
                if ("취소".equals(item.getValue())) sawCancelCaption = true;
            } else if (item.getCategory() == TargetPayloadCategory.EVENT) {
                eventCount++;
                if ("onclick".equals(item.getValue())
                        && "fn_save".equals(item.getStructuredData().get("functionName"))) {
                    sawSaveEvent = true;
                }
            }
        }
        assertEquals("button_group: 2 captions", "2", String.valueOf(displayTextCount));
        assertTrue("button_group: save caption", sawSaveCaption);
        assertTrue("button_group: cancel caption", sawCancelCaption);
        assertEquals("button_group: exactly 1 safe unique event (only btnSave bound)",
                "1", String.valueOf(eventCount));
        assertTrue("button_group: save event correlated", sawSaveEvent);
    }

    /** {@code getExpectedStructuralMemberCount()}는 실제 flattenedButtons 개수(2)와 일치해야
     * 하고, 각 button leaf의 {@code structuredData["buttonOrdinal"]}은 0/1을 정확히 담아야 한다. */
    private static void testButtonGroupExpectedButtonCountPropagatedExactly() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Element btnGroup = (Element) form.getElementsByTagName("Div").item(1);
        String planNodeId = structuralIdOf(btnGroup);

        Fixture fx = buildFixtureWithAnalysis(form, new XfdlAnalysisResult());
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("expected_count: payload present", payload != null);
        assertEquals("expected_count: exactly 2 (flattenedButtons.size())", "2",
                String.valueOf(payload.getExpectedStructuralMemberCount()));

        boolean sawOrdinal0 = false;
        boolean sawOrdinal1 = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() != TargetPayloadCategory.DISPLAY_TEXT) {
                continue;
            }
            Object ordinal = item.getStructuredData().get("buttonOrdinal");
            assertTrue("expected_count: button leaf carries non-null buttonOrdinal", ordinal != null);
            if (Integer.valueOf(0).equals(ordinal)) sawOrdinal0 = true;
            if (Integer.valueOf(1).equals(ordinal)) sawOrdinal1 = true;
        }
        assertTrue("expected_count: buttonOrdinal 0 present", sawOrdinal0);
        assertTrue("expected_count: buttonOrdinal 1 present", sawOrdinal1);
    }

    /** text/value가 모두 없는 Button도 묵시적으로 누락되면 안 된다 -- structural leaf는
     * value=null, buttonOrdinal 유지 상태로 존재해야 하고, expectedStructuralMemberCount는
     * 실제 flattenedButtons 개수(3)와 일치해야 한다. */
    private static void testButtonGroupButtonWithoutTextOrValueRetainsStructuralLeafWithOrdinal() throws Exception {
        Document doc = newDocument();
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
        btnB.setAttribute("text", "B");
        btnB.setAttribute("left", "60");
        Element btnNoText = doc.createElement("Button");
        btnNoText.setAttribute("id", "btnBlank");
        btnNoText.setAttribute("left", "110");
        btnGroup.appendChild(btnA);
        btnGroup.appendChild(btnB);
        btnGroup.appendChild(btnNoText);
        form.appendChild(btnGroup);
        String planNodeId = structuralIdOf(btnGroup);

        Fixture fx = buildFixtureWithAnalysis(form, new XfdlAnalysisResult());
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("no_text_value: payload present", payload != null);
        assertEquals("no_text_value: expectedStructuralMemberCount == 3", "3",
                String.valueOf(payload.getExpectedStructuralMemberCount()));

        int buttonLeafCount = 0;
        boolean sawNullValueOrdinal2 = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() != TargetPayloadCategory.DISPLAY_TEXT) {
                continue;
            }
            buttonLeafCount++;
            Object ordinal = item.getStructuredData().get("buttonOrdinal");
            if (Integer.valueOf(2).equals(ordinal)) {
                assertTrue("no_text_value: ordinal-2 leaf has null presentation value", item.getValue() == null);
                sawNullValueOrdinal2 = true;
            }
        }
        assertEquals("no_text_value: exactly 3 structural button leaves (not silently dropped)",
                "3", String.valueOf(buttonLeafCount));
        assertTrue("no_text_value: the textless/valueless button retains its structural leaf+ordinal",
                sawNullValueOrdinal2);
    }

    /** BUTTON_GROUP이 아닌 payload는 expectedStructuralMemberCount가 항상 null이어야 한다. */
    private static void testNonButtonGroupPayloadHasNullExpectedStructuralMemberCount() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Element titleBar = (Element) form.getElementsByTagName("Div").item(0);
        String planNodeId = structuralIdOf(titleBar);

        Fixture fx = buildFixtureWithAnalysis(form, new XfdlAnalysisResult());
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("non_button_group: TITLE_BAR payload present", payload != null);
        assertTrue("non_button_group: expectedStructuralMemberCount is null for TITLE_BAR",
                payload.getExpectedStructuralMemberCount() == null);
    }

    private static void testGridColumnEvidence() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        Element grid = (Element) form.getElementsByTagName("Grid").item(0);
        String planNodeId = structuralIdOf(grid);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("grid: payload present", payload != null);
        assertEquals("grid: item count (2 head + 1 body)", "3", String.valueOf(payload.getItems().size()));
        boolean sawHeadName = false;
        boolean sawBodyBind = false;
        for (TargetLeafPayload item : payload.getItems()) {
            assertEquals("grid: category", "GRID_COLUMN", item.getCategory().name());
            if ("head".equals(item.getStructuredData().get("band")) && "이름".equals(item.getValue())) {
                sawHeadName = true;
            }
            if ("body".equals(item.getStructuredData().get("band")) && "bind:col1".equals(item.getValue())) {
                sawBodyBind = true;
            }
        }
        assertTrue("grid: head cell text present", sawHeadName);
        assertTrue("grid: body cell raw text present (payload does not interpret bind: prefix)", sawBodyBind);
    }

    /**
     * MULTI_FORMAT_AMBIGUITY_FAILS_BEFORE_RENDERER_TEST -- Grid에 두번째 Format을 추가해(선택
     * 근거 없음) v6 전체 경로(segmenter -&gt; evaluator -&gt; plan -&gt; extractor)를 실제로 태우면
     * {@link TargetPayloadExtractor}가 렌더러 도달 전에 명시적으로 fail-closed됨을 증명한다.
     */
    private static void testGridAmbiguousMultiFormatFailsClosedBeforeRenderer() throws Exception {
        Document doc = newDocument();
        final Element form = buildFixtureForm(doc);
        Element grid = (Element) form.getElementsByTagName("Grid").item(0);
        Element formats = (Element) grid.getElementsByTagName("Formats").item(0);
        Element secondFormat = doc.createElement("Format");
        secondFormat.setAttribute("id", "alternate");
        formats.appendChild(secondFormat);

        final Fixture fx = buildFixture(form);
        assertThrowsIllegalState("grid_ambiguous_multi_format_before_renderer", new Runnable() {
            public void run() {
                extractWithBindings(form, fx.plan, fx.regions);
            }
        });
    }

    /**
     * Slice 99C -- CheckBox id를 가리키는 real 증거 기반 {@code <BindItem compid=.../>}가 source
     * 어딘가(예: Bind 블록)에 있으면(BUSINESS_TABLE control 위치) propid/값 계약이 전혀 증명되지
     * 않았으므로 렌더러 도달 전에 명시적으로 fail-closed됨을 증명한다.
     */
    private static void testCheckBoxDatasetBoundBusinessTableFailsClosedBeforeRenderer() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table1", "lbl1", "chk1"));
        form.appendChild(bindItemFor(doc, "b1", "chk1", "checked", "dsAgree", "AGREE"));

        final Fixture fx = buildFixture(form);
        assertThrowsIllegalState("checkbox_dataset_bound_business_table", new Runnable() {
            public void run() {
                extractWithBindings(form, fx.plan, fx.regions);
            }
        });
    }

    /**
     * 구조적으로 다른 두번째 fixture(SEARCH_AREA 경로, Grid peer 존재, 다른 propid/datasetid/columnid
     * 조합)도 동일한 명시적 fail-closed 계약으로 귀결됨을 증명한다 -- 표면 구조가 달라도 결과는 동일.
     */
    private static void testCheckBoxDatasetBoundSearchAreaStructurallyDifferentFixtureFailsClosed() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "search1", "lblAgree", "agreeChk"));
        form.appendChild(bindItemFor(doc, "b2", "agreeChk", "value", "dsOther", "FLAG"));
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grid1");
        form.appendChild(grid);

        final Fixture fx = buildFixture(form);
        assertThrowsIllegalState("checkbox_dataset_bound_search_area_structurally_different", new Runnable() {
            public void run() {
                extractWithBindings(form, fx.plan, fx.regions);
            }
        });
    }

    /** 회귀 -- 이 id를 가리키는 BindItem이 전혀 없는 unbound CheckBox는 기존과 동일하게 정상 추출된다. */
    private static void testCheckBoxUnboundNoBindDatasetStillSucceeds() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table2", "lbl2", "chk2"));

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        assertTrue("checkbox-unbound: extraction succeeds without any BindItem targeting it", !payloads.isEmpty());
    }

    /**
     * CHECKBOX_BINDING_FALSE_POSITIVE_ISOLATION_TEST(1/2) -- 다른 컴포넌트(Edit)를 가리키는
     * BindItem이 있어도, 그와 무관한 CheckBox는 정상 추출된다(잘못된 correlation 반례).
     */
    private static void testCheckBoxBindingFalsePositiveEditTargetDoesNotRejectCheckBox() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table3", "lbl3", "chkSafe"));
        Element edit = doc.createElement("Edit");
        edit.setAttribute("id", "edtElsewhere");
        form.appendChild(edit);
        form.appendChild(bindItemFor(doc, "bEdit", "edtElsewhere", "value", "dsX", "COL_X"));

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        assertTrue("false-positive-edit-target: unrelated CheckBox still extracted", !payloads.isEmpty());
    }

    /**
     * CHECKBOX_BINDING_FALSE_POSITIVE_ISOLATION_TEST(2/2) -- 다른 CheckBox(compid가 문서에 실존
     * 하지 않음, unresolved)를 가리키는 BindItem이 있어도 이 CheckBox는 정상 추출된다.
     */
    private static void testCheckBoxBindingFalsePositiveOtherCheckBoxTargetDoesNotRejectThisOne() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table4", "lbl4", "chkSafe2"));
        form.appendChild(bindItemFor(doc, "bOther", "chkSomewhereElseNotPresent", "checked", "dsY", "COL_Y"));

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        assertTrue("false-positive-other-checkbox-target: unrelated CheckBox still extracted", !payloads.isEmpty());
    }

    /**
     * CHECKBOX_DATASET_NAME_HEURISTIC_TEST -- CheckBox의 id/name 문자열이 무엇이든 판정은 오직
     * BindItem의 compid 참조 존재 여부로만 결정되며, control 이름에서 의미를 추론하지 않는다.
     */
    private static void testCheckBoxDatasetBoundNameHeuristicIrrelevant() throws Exception {
        Document doc = newDocument();
        final Element formA = doc.createElement("Form");
        doc.appendChild(formA);
        formA.appendChild(businessTableWithCheckBoxControl(doc, "tableA", "lblA", "zzzUnrelatedName"));
        formA.appendChild(bindItemFor(doc, "bA", "zzzUnrelatedName", "checked", "dsA", "COL_A"));
        final Fixture fxA = buildFixture(formA);
        assertThrowsIllegalState("checkbox_name_heuristic_irrelevant_A", new Runnable() {
            public void run() {
                extractWithBindings(formA, fxA.plan, fxA.regions);
            }
        });

        Document doc2 = newDocument();
        final Element formB = doc2.createElement("Form");
        doc2.appendChild(formB);
        formB.appendChild(businessTableWithCheckBoxControl(doc2, "tableB", "lblB", "checkedDefaultAgree"));
        formB.appendChild(bindItemFor(doc2, "bB", "checkedDefaultAgree", "checked", "dsB", "COL_B"));
        final Fixture fxB = buildFixture(formB);
        assertThrowsIllegalState("checkbox_name_heuristic_irrelevant_B", new Runnable() {
            public void run() {
                extractWithBindings(formB, fxB.plan, fxB.regions);
            }
        });
    }

    /**
     * UNRELATED_AMBIGUOUS_BINDING_ISOLATION_TEST -- compid가 문서 안에서 2개 Element(둘 다 이
     * CheckBox는 아님)에 동시에 매치되면 ambiguous로 남지만, 이 CheckBox는 후보 목록에 없으므로
     * 영향받지 않고 정상 추출된다.
     */
    private static void testCheckBoxUnrelatedAmbiguousBindingDoesNotContaminate() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table5", "lbl5", "chkSafe3"));
        Element dupA = doc.createElement("Div");
        dupA.setAttribute("id", "dupTarget");
        Element dupB = doc.createElement("Div");
        dupB.setAttribute("id", "dupTarget");
        form.appendChild(dupA);
        form.appendChild(dupB);
        form.appendChild(bindItemFor(doc, "bAmbig", "dupTarget", "checked", "dsZ", "COL_Z"));

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        assertTrue("unrelated-ambiguous: unrelated CheckBox still extracted (not a candidate)",
                !payloads.isEmpty());
    }

    /**
     * CHECKBOX_RELEVANT_AMBIGUOUS_BINDING_TEST -- compid가 CheckBox 2개(같은 id)에 동시에
     * 매치되면 ambiguous 상태이지만, 후보 목록에 이 CheckBox가 포함돼 있으므로 명시적으로
     * fail-closed된다("unbound으로 넘어가도 된다"가 아님).
     */
    private static void testCheckBoxRelevantAmbiguousBindingFailsClosed() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table6", "lbl6", "dupChk"));
        Element secondCheckBox = doc.createElement("CheckBox");
        secondCheckBox.setAttribute("id", "dupChk");
        form.appendChild(secondCheckBox);
        form.appendChild(bindItemFor(doc, "bAmbigChk", "dupChk", "checked", "dsAmbig", "COL_AMBIG"));

        final Fixture fx = buildFixture(form);
        assertThrowsIllegalState("checkbox_relevant_ambiguous_binding", new Runnable() {
            public void run() {
                extractWithBindings(form, fx.plan, fx.regions);
            }
        });
    }

    /**
     * CHECKBOX_RELEVANT_AMBIGUOUS_BINDING_TEST(mixed) -- compid 후보가 CheckBox 1개 + Edit 1개
     * (같은 id)로 ambiguous하면, 그중 CheckBox 후보에 대해서도 동일하게 fail-closed된다.
     */
    private static void testCheckBoxAmbiguousCandidateSetIncludingEditAlsoFailsForCheckBox() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table7", "lbl7", "dupMixed"));
        Element edit = doc.createElement("Edit");
        edit.setAttribute("id", "dupMixed");
        form.appendChild(edit);
        form.appendChild(bindItemFor(doc, "bAmbigMixed", "dupMixed", "value", "dsMixed", "COL_MIXED"));

        final Fixture fx = buildFixture(form);
        assertThrowsIllegalState("checkbox_ambiguous_mixed_edit_candidate", new Runnable() {
            public void run() {
                extractWithBindings(form, fx.plan, fx.regions);
            }
        });
    }

    /**
     * UNRELATED_AMBIGUOUS_BINDING_ISOLATION_TEST(Edit 후보만) -- compid 후보가 서로 다른 두
     * Edit(같은 id)뿐이고 CheckBox는 후보에 없으면, 무관한 CheckBox는 정상 추출된다.
     */
    private static void testCheckBoxUnrelatedAmbiguousEditCandidatesDoNotRejectCheckBox() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        form.appendChild(businessTableWithCheckBoxControl(doc, "table8", "lbl8", "chkSafe4"));
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "dupEdit");
        Element edit2 = doc.createElement("Edit");
        edit2.setAttribute("id", "dupEdit");
        form.appendChild(edit1);
        form.appendChild(edit2);
        form.appendChild(bindItemFor(doc, "bAmbigEdit", "dupEdit", "value", "dsEdit", "COL_EDIT"));

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        assertTrue("unrelated-ambiguous-edit-candidates: unrelated CheckBox still extracted",
                !payloads.isEmpty());
    }

    /** label(Static) + CheckBox(control) 1쌍짜리 BUSINESS_TABLE-eligible container. BindItem은
     * 별도로 {@link #bindItemFor}로 추가한다(unbound 회귀는 그냥 생략). */
    private static Element businessTableWithCheckBoxControl(
            Document doc, String containerId, String labelId, String checkBoxId) {
        Element container = doc.createElement("Div");
        container.setAttribute("id", containerId);
        Element label = doc.createElement("Static");
        label.setAttribute("id", labelId);
        label.setAttribute("text", "동의");
        label.setAttribute("left", "0");
        label.setAttribute("top", "0");
        label.setAttribute("width", "50");
        label.setAttribute("height", "20");
        Element checkBox = doc.createElement("CheckBox");
        checkBox.setAttribute("id", checkBoxId);
        checkBox.setAttribute("left", "60");
        checkBox.setAttribute("top", "0");
        checkBox.setAttribute("width", "100");
        checkBox.setAttribute("height", "20");
        container.appendChild(label);
        container.appendChild(checkBox);
        return container;
    }

    /** {@code DatasetBinding.xfdl} 실 corpus fixture와 동일한 구조의 {@code <Bind><BindItem .../></Bind>}. */
    private static Element bindItemFor(
            Document doc, String bindItemId, String compId, String propId, String datasetId, String columnId) {
        Element bind = doc.createElement("Bind");
        Element bindItem = doc.createElement("BindItem");
        bindItem.setAttribute("id", bindItemId);
        bindItem.setAttribute("compid", compId);
        bindItem.setAttribute("propid", propId);
        bindItem.setAttribute("datasetid", datasetId);
        bindItem.setAttribute("columnid", columnId);
        bind.appendChild(bindItem);
        return bind;
    }

    private static void testTabControlLabels() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        Element tab = (Element) form.getElementsByTagName("Tab").item(0);
        String planNodeId = structuralIdOf(tab);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("tab_control: payload present", payload != null);
        assertEquals("tab_control: item count", "2", String.valueOf(payload.getItems().size()));
        boolean sawFirst = false;
        boolean sawSecondViaTitletext = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if ("첫번째 탭".equals(item.getValue())) {
                sawFirst = true;
                assertEquals("tab_control: page1 evidence kind", "source_text_attribute", item.getSourceEvidenceKind());
            }
            if ("두번째 탭".equals(item.getValue())) {
                sawSecondViaTitletext = true;
                assertEquals("tab_control: page2 evidence kind (titletext fallback)",
                        "source_titletext_attribute", item.getSourceEvidenceKind());
            }
        }
        assertTrue("tab_control: page1 label", sawFirst);
        assertTrue("tab_control: page2 label via titletext fallback", sawSecondViaTitletext);
    }

    /** RAW_ONLY -> payload 자동 승격 = 0: analysis를 아예 넘기지 않으면(null) EVENT payload가
     * 그냥 없어야 한다(추측 생성 금지). BINDING은 생성 경로 자체가 없으므로 항상 0이어야 한다. */
    private static void testRawOnlyNotPromoted() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        for (TargetNodePayload payload : payloads) {
            for (TargetLeafPayload item : payload.getItems()) {
                assertTrue("raw_only: no BINDING (no producer this round)",
                        item.getCategory() != TargetPayloadCategory.BINDING);
                assertTrue("raw_only: no EVENT without XfdlAnalysisResult",
                        item.getCategory() != TargetPayloadCategory.EVENT);
            }
        }
    }

    /** TARGET_SYNTHETIC(PAGING 등) payload는 발명하지 않는다 -- 실제로 0건이어야 한다. */
    private static void testTargetSyntheticSkipped() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        CompositionDecision gridDecision = null;
        for (SemanticRegionResult region : fx.regions) {
            CompositionDecision decision = evaluator.evaluate(region);
            decisions.add(decision);
            if ("GRID".equals(decision.getFamily())) {
                gridDecision = decision;
            }
        }
        assertTrue("target_synthetic_skip: grid decision found", gridDecision != null);
        String syntheticId = com.example.xfdltracker.composition.TargetSyntheticIdentity.build(
                gridDecision, "paging", "PAGING", "fixture-discriminator");
        CompositionDecision pagingDecision = evaluator.createTargetSyntheticDecision("PAGING", null, syntheticId);
        boolean assigned = evaluator.assignSlot(gridDecision, "paging", pagingDecision);
        assertTrue("target_synthetic_skip: assignSlot succeeded", assigned);

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, fx.regions);
        String syntheticNodeId = "target_synthetic:" + syntheticId;
        assertTrue("target_synthetic_skip: no payload for TARGET_SYNTHETIC node",
                findPayload(payloads, syntheticNodeId) == null);
    }

    /** sourceRegionId collision scope에서도 cross-wiring = 0. */
    private static void testCrossWiringSameSourceRegionIdScope() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element outerTab = doc.createElement("Tab");
        outerTab.setAttribute("id", "tabOuter");
        Element pageA = doc.createElement("Tabpage");
        pageA.setAttribute("id", "shared");
        pageA.setAttribute("text", "바깥 탭");
        outerTab.appendChild(pageA);
        form.appendChild(outerTab);

        Element innerTab = doc.createElement("Tab");
        innerTab.setAttribute("id", "tabInner");
        Element pageB = doc.createElement("Tabpage");
        pageB.setAttribute("id", "shared");
        pageB.setAttribute("text", "안쪽 탭");
        innerTab.appendChild(pageB);
        form.appendChild(innerTab);

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);

        String outerNodeId = structuralIdOf(outerTab);
        String innerNodeId = structuralIdOf(innerTab);
        assertTrue("cross_wiring: distinct plan node ids", !outerNodeId.equals(innerNodeId));

        TargetNodePayload outerPayload = findPayload(payloads, outerNodeId);
        TargetNodePayload innerPayload = findPayload(payloads, innerNodeId);
        assertTrue("cross_wiring: outer payload present", outerPayload != null);
        assertTrue("cross_wiring: inner payload present", innerPayload != null);
        assertEquals("cross_wiring: outer label", "바깥 탭", outerPayload.getItems().get(0).getValue());
        assertEquals("cross_wiring: inner label", "안쪽 탭", innerPayload.getItems().get(0).getValue());
    }

    /** 입력 순서에 결과가 의존하면 안 된다. */
    private static void testOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> forward = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            forward.add(evaluator.evaluate(region));
        }
        List<CompositionDecision> reversed = new ArrayList<CompositionDecision>(forward);
        java.util.Collections.reverse(reversed);

        TargetCompositionPlan planForward = new TargetCompositionPlanBuilder().build(forward);
        TargetCompositionPlan planReversed = new TargetCompositionPlanBuilder().build(reversed);

        List<TargetNodePayload> payloadsForward =
                extractWithBindings(form, planForward, regions);
        List<TargetNodePayload> payloadsReversed =
                extractWithBindings(form, planReversed, regions);

        assertEquals("order_independence: same node count",
                String.valueOf(payloadsForward.size()), String.valueOf(payloadsReversed.size()));
        for (TargetNodePayload fp : payloadsForward) {
            TargetNodePayload rp = findPayload(payloadsReversed, fp.getPlanNodeId());
            assertTrue("order_independence: node " + fp.getPlanNodeId() + " present in both", rp != null);
            assertEquals("order_independence: item count for " + fp.getPlanNodeId(),
                    String.valueOf(fp.getItems().size()), String.valueOf(rp.getItems().size()));
        }
    }

    // ---- 모호한 상관관계 방지 회귀 테스트 ----

    private static void testAmbiguousEventCorrelationAcrossNonDivScopes() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tab1");
        tab.setAttribute("width", "0");
        form.appendChild(tab);

        Element pageA = doc.createElement("Tabpage");
        pageA.setAttribute("id", "pageA");
        pageA.setAttribute("width", "200");
        Element buttonA = doc.createElement("Button");
        buttonA.setAttribute("id", "sameButton");
        buttonA.setAttribute("text", "확인A");
        buttonA.setAttribute("left", "10");
        pageA.appendChild(buttonA);
        tab.appendChild(pageA);

        Element pageB = doc.createElement("Tabpage");
        pageB.setAttribute("id", "pageB");
        pageB.setAttribute("width", "200");
        Element buttonB = doc.createElement("Button");
        buttonB.setAttribute("id", "sameButton");
        buttonB.setAttribute("text", "확인B");
        buttonB.setAttribute("left", "10");
        pageB.appendChild(buttonB);
        tab.appendChild(pageB);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("sameButton", "onclick", "fn_confirmA"));
        Fixture fx = buildFixtureWithAnalysis(form, analysis);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        for (TargetNodePayload payload : payloads) {
            for (TargetLeafPayload item : payload.getItems()) {
                assertTrue("ambiguous_event: no EVENT payload guessed for ambiguous componentId",
                        item.getCategory() != TargetPayloadCategory.EVENT);
            }
        }
        boolean sawA = false;
        boolean sawB = false;
        for (TargetNodePayload payload : payloads) {
            for (TargetLeafPayload item : payload.getItems()) {
                if ("확인A".equals(item.getValue())) sawA = true;
                if ("확인B".equals(item.getValue())) sawB = true;
            }
        }
        assertTrue("ambiguous_event: captionA still safe (direct Element read)", sawA);
        assertTrue("ambiguous_event: captionB still safe (direct Element read)", sawB);
    }

    private static void testDuplicateIdenticalEventBindingSafelyDeduplicated() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save"));
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save"));
        Fixture fx = buildFixtureWithAnalysis(form, analysis);

        Element btnGroup = (Element) form.getElementsByTagName("Div").item(1);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(btnGroup));
        int eventCount = 0;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) eventCount++;
        }
        assertEquals("duplicate_identical_event: safely deduplicated to 1", "1", String.valueOf(eventCount));
    }

    private static void testConflictingFunctionNameForSameEventAmbiguous() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save"));
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save_v2"));
        Fixture fx = buildFixtureWithAnalysis(form, analysis);

        Element btnGroup = (Element) form.getElementsByTagName("Div").item(1);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(btnGroup));
        int eventCount = 0;
        if (payload != null) {
            for (TargetLeafPayload item : payload.getItems()) {
                if (item.getCategory() == TargetPayloadCategory.EVENT) eventCount++;
            }
        }
        assertEquals("conflicting_function_name: no EVENT guessed for conflicting onclick",
                "0", String.valueOf(eventCount));
    }

    /** role="event" evidence도 다른 role과 동일한 fail-closed provenance 계약을 따라야 한다 --
     * sourceComponentStructuralId가 존재하지 않는 Element를 가리키면 명시적 실패해야 한다. */
    private static void testEventEvidenceProvenanceTamperExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("event_provenance_tamper: region found", buttonGroup != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        // 존재하지 않는 Element를 가리키는 변조된 EVENT evidence.
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), "Form[0]/Div[1]/Button[99]", "event", "event_binding",
                "onclick", "fn_tampered", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("event_provenance_tamper", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** EVENT evidence를 포함한 evidence 저장 순서를 뒤집어도 sourceOrder 기준으로 payload가
     * 동일해야 한다. */
    private static void testEventEvidenceOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("btnGroup1.btnSave", "onclick", "fn_save"));
        analysis.getEvents().add(new EventBinding("btnGroup1.btnCancel", "onclick", "fn_cancel"));
        Fixture fx = buildFixtureWithAnalysis(form, analysis);
        SemanticRegionResult buttonGroup = findRegionByFamily(fx.regions, "BUTTON_GROUP");
        assertTrue("event_order_independence: region found", buttonGroup != null);
        Element btnGroupDiv = (Element) form.getElementsByTagName("Div").item(1);

        List<TargetNodePayload> forwardPayloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload forward = findPayload(forwardPayloads, structuralIdOf(btnGroupDiv));

        java.util.Collections.reverse(buttonGroup.getPayloadEvidence());
        List<TargetNodePayload> reversedPayloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload reversed = findPayload(reversedPayloads, structuralIdOf(btnGroupDiv));

        assertTrue("event_order_independence: forward present", forward != null);
        assertTrue("event_order_independence: reversed present", reversed != null);
        assertEquals("event_order_independence: same item count",
                String.valueOf(forward.getItems().size()), String.valueOf(reversed.getItems().size()));
        for (int i = 0; i < forward.getItems().size(); i++) {
            assertEquals("event_order_independence: item[" + i + "] value matches",
                    forward.getItems().get(i).getValue(), reversed.getItems().get(i).getValue());
            assertEquals("event_order_independence: item[" + i + "] category matches",
                    forward.getItems().get(i).getCategory().name(), reversed.getItems().get(i).getCategory().name());
        }
        int eventCount = 0;
        for (TargetLeafPayload item : forward.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) eventCount++;
        }
        assertEquals("event_order_independence: 2 EVENT items (btnSave + btnCancel both unique)",
                "2", String.valueOf(eventCount));
    }

    // ---- EVENT evidence 무결성 강화 ----

    private static final class ButtonGroupHardeningFixture {
        final Element form;
        final TargetCompositionPlan plan;
        final List<SemanticRegionResult> regions;
        final SemanticRegionResult buttonGroup;
        ButtonGroupHardeningFixture(Element form, TargetCompositionPlan plan,
                List<SemanticRegionResult> regions, SemanticRegionResult buttonGroup) {
            this.form = form;
            this.plan = plan;
            this.regions = regions;
            this.buttonGroup = buttonGroup;
        }
    }

    private static ButtonGroupHardeningFixture buttonGroupRegionAndPlan(Document doc) throws Exception {
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("button_group_final_hardening: region found", buttonGroup != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        return new ButtonGroupHardeningFixture(form, plan, regions, buttonGroup);
    }

    /** eventName은 있지만 functionName=null인 EVENT evidence는 명시적 실패해야 한다. */
    private static void testEventMissingFunctionNameExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnSave = (Element) fx.form.getElementsByTagName("Button").item(0);
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(btnSave), "event", "event_binding",
                "onclick", null, 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("event_missing_function_name", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** eventName이 공백 문자열이면 명시적 실패해야 한다. */
    private static void testEventBlankEventNameExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnSave = (Element) fx.form.getElementsByTagName("Button").item(0);
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(btnSave), "event", "event_binding",
                "   ", "fn_save", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("event_blank_event_name", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** functionName이 공백 문자열이면 명시적 실패해야 한다. */
    private static void testEventBlankFunctionNameExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnSave = (Element) fx.form.getElementsByTagName("Button").item(0);
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(btnSave), "event", "event_binding",
                "onclick", "   ", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("event_blank_function_name", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** producer는 오직 "event_binding"만 만든다 -- 다른 kind로 구성된 event evidence는
     * 명시적 실패해야 한다. */
    private static void testEventWrongKindExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnSave = (Element) fx.form.getElementsByTagName("Button").item(0);
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(btnSave), "event", "source_text_attribute",
                "onclick", "fn_save", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("event_wrong_kind", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** role="button" caption evidence에 functionName이 붙어 있으면(어떤 producer도 만들지
     * 않는 형태) 명시적 실패해야 한다. */
    private static void testNonEventFunctionNameInjectionExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnSave = (Element) fx.form.getElementsByTagName("Button").item(0);
        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(btnSave), "button", "source_text_attribute",
                "저장", "fnInjected", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("non_event_function_name_injection", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** BUTTON_GROUP subtree 안의 실제 wrapper Div(containment는 통과)를 가리키는 EVENT
     * evidence는 role/source element contract 위반으로 명시적 실패해야 한다(실제 tag를
     * 확인, structuralId prefix 파싱이 아님). */
    private static void testEventNonButtonTamperExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnGroupDiv = (Element) fx.form.getElementsByTagName("Div").item(1);
        Element fakeWrapper = doc.createElement("Div");
        fakeWrapper.setAttribute("id", "fakeWrapperForEventTamper");
        btnGroupDiv.appendChild(fakeWrapper);
        // segmentation은 이미 끝났으므로 기존 evidence는 그대로다 -- 변조된 evidence가
        // 가리킬, containment는 유효하지만 tag가 잘못된 실제 Element를 추가할 뿐이다.

        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(fakeWrapper), "event", "event_binding",
                "onclick", "fn_tampered", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("event_non_button_tamper", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 동일한 wrapper Div를 role="button" caption evidence의 sourceComponentStructuralId로
     * 가리키면 명시적 실패해야 한다. */
    private static void testButtonCaptionNonButtonTamperExplicitFailure() throws Exception {
        Document doc = newDocument();
        ButtonGroupHardeningFixture fx = buttonGroupRegionAndPlan(doc);
        SemanticRegionResult buttonGroup = fx.buttonGroup;

        Element btnGroupDiv = (Element) fx.form.getElementsByTagName("Div").item(1);
        Element fakeWrapper = doc.createElement("Div");
        fakeWrapper.setAttribute("id", "fakeWrapperForCaptionTamper");
        fakeWrapper.setAttribute("text", "가짜캡션");
        btnGroupDiv.appendChild(fakeWrapper);

        buttonGroup.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                buttonGroup.getSourceStructuralId(), structuralIdOf(fakeWrapper), "button",
                "source_text_attribute", "가짜캡션", 99));

        final TargetCompositionPlan plan = fx.plan;
        final Element finalForm = fx.form;
        final List<SemanticRegionResult> finalRegions = fx.regions;
        assertThrowsIllegalState("button_caption_non_button_tamper", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    // ---- EVENT 입력 순서 결정론 ----

    /** btnGroupMulti 하나 아래 btnSolo(단일 Button)만 있는 fixture -- 같은 Button에 서로 다른
     * eventName 2개를 붙여 same-Button 내부 EVENT ordering determinism을 테스트하기 위함. */
    private static Element buildSingleButtonEventOrderForm(Document doc) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupOrder");
        btnGroup.setAttribute("width", "100");
        Element btnSolo = doc.createElement("Button");
        btnSolo.setAttribute("id", "btnSolo");
        btnSolo.setAttribute("text", "단독");
        btnSolo.setAttribute("left", "10");
        btnGroup.appendChild(btnSolo);
        form.appendChild(btnGroup);
        return form;
    }

    /** btnGroupMulti 아래 btnA, btnB(source DOM 순서대로) -- 서로 다른 Button 각각에 이벤트를
     * 붙여 multi-button 시나리오(Button order 보존 + global input order 무관)를 테스트하기
     * 위함. */
    private static Element buildTwoButtonEventOrderForm(Document doc) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupMulti");
        btnGroup.setAttribute("width", "200");
        Element btnA = doc.createElement("Button");
        btnA.setAttribute("id", "btnA");
        btnA.setAttribute("text", "A");
        btnA.setAttribute("left", "10");
        Element btnB = doc.createElement("Button");
        btnB.setAttribute("id", "btnB");
        btnB.setAttribute("text", "B");
        btnB.setAttribute("left", "60");
        btnGroup.appendChild(btnA);
        btnGroup.appendChild(btnB);
        form.appendChild(btnGroup);
        return form;
    }

    private static List<TargetLeafPayload> segmentAndExtractButtonGroupPayload(
            Element form, XfdlAnalysisResult analysis) throws Exception {
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form, analysis);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("event_order: BUTTON_GROUP region found", buttonGroup != null);
        TargetNodePayload payload = findPayload(payloads, buttonGroup.getSourceStructuralId());
        return payload == null ? new ArrayList<TargetLeafPayload>() : payload.getItems();
    }

    private static String eventEvidenceTuplesOf(List<SemanticRegionResult> regions) {
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        StringBuilder sb = new StringBuilder();
        for (SourcePayloadEvidenceItem item : buttonGroup.getPayloadEvidence()) {
            sb.append(item.getEvidenceRole()).append('|').append(item.getEvidenceKind()).append('|')
                    .append(item.getValue()).append('|').append(item.getFunctionName()).append('|')
                    .append(item.getSourceComponentStructuralId()).append('|').append(item.getSourceOrder())
                    .append(';');
        }
        return sb.toString();
    }

    private static String payloadTuplesOf(List<TargetLeafPayload> items) {
        StringBuilder sb = new StringBuilder();
        for (TargetLeafPayload item : items) {
            sb.append(item.getCategory().name()).append('|').append(item.getValue()).append('|');
            if (item.getStructuredData() != null) {
                sb.append(item.getStructuredData().get("functionName"));
            }
            sb.append(';');
        }
        return sb.toString();
    }

    /** 같은 Button에 EventBinding 2개의 입력 순서를 뒤집어도 predicate-time evidence와
     * Target EVENT payload의 ordered tuple이 완전히 동일해야 한다. */
    private static void testSafeEventInputOrderReverse() throws Exception {
        Document docA = newDocument();
        Element formA = buildSingleButtonEventOrderForm(docA);
        XfdlAnalysisResult analysisA = new XfdlAnalysisResult();
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnClick"));
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onkeyup", "fnKey"));
        List<SemanticRegionResult> regionsA = new SemanticRegionSegmenter().segment(formA, analysisA);
        List<TargetLeafPayload> payloadA = segmentAndExtractButtonGroupPayload(formA, analysisA);

        Document docB = newDocument();
        Element formB = buildSingleButtonEventOrderForm(docB);
        XfdlAnalysisResult analysisB = new XfdlAnalysisResult();
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onkeyup", "fnKey"));
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnClick"));
        List<SemanticRegionResult> regionsB = new SemanticRegionSegmenter().segment(formB, analysisB);
        List<TargetLeafPayload> payloadB = segmentAndExtractButtonGroupPayload(formB, analysisB);

        assertEquals("safe_event_reverse: predicate-time evidence ordered tuples identical",
                eventEvidenceTuplesOf(regionsA), eventEvidenceTuplesOf(regionsB));
        assertEquals("safe_event_reverse: Target EVENT payload ordered tuples identical",
                payloadTuplesOf(payloadA), payloadTuplesOf(payloadB));
        int eventCountA = 0;
        for (TargetLeafPayload item : payloadA) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) eventCountA++;
        }
        assertEquals("safe_event_reverse: 2 EVENT items", "2", String.valueOf(eventCountA));
    }

    /** 항목 5 duplicate order reverse: 완전 동일한 (eventName, functionName) 튜플의 duplicate
     * EventBinding 2개의 입력 순서를 바꿔도 -- EVENT는 여전히 1건이고 ordered tuple도 동일해야
     * 한다. */
    private static void testDuplicateEventOrderReverse() throws Exception {
        Document docA = newDocument();
        Element formA = buildSingleButtonEventOrderForm(docA);
        XfdlAnalysisResult analysisA = new XfdlAnalysisResult();
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnSave"));
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnSave"));
        List<SemanticRegionResult> regionsA = new SemanticRegionSegmenter().segment(formA, analysisA);

        Document docB = newDocument();
        Element formB = buildSingleButtonEventOrderForm(docB);
        XfdlAnalysisResult analysisB = new XfdlAnalysisResult();
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnSave"));
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnSave"));
        java.util.Collections.reverse(analysisB.getEvents());
        List<SemanticRegionResult> regionsB = new SemanticRegionSegmenter().segment(formB, analysisB);

        assertEquals("duplicate_order_reverse: ordered tuples identical",
                eventEvidenceTuplesOf(regionsA), eventEvidenceTuplesOf(regionsB));
        SemanticRegionResult buttonGroupA = findRegionByFamily(regionsA, "BUTTON_GROUP");
        int eventCount = 0;
        for (SourcePayloadEvidenceItem item : buttonGroupA.getPayloadEvidence()) {
            if ("event".equals(item.getEvidenceRole())) eventCount++;
        }
        assertEquals("duplicate_order_reverse: safely deduplicated to 1", "1", String.valueOf(eventCount));
    }

    /** 항목 5 conflicting handler reverse: 같은 eventName에 서로 다른 functionName 2개(onclick→fnA,
     * onclick→fnB)의 입력 순서를 바꿔도 -- 둘 다 EVENT 0건이어야 한다(first/last-wins 금지). */
    private static void testConflictingHandlerReverse() throws Exception {
        Document docA = newDocument();
        Element formA = buildSingleButtonEventOrderForm(docA);
        XfdlAnalysisResult analysisA = new XfdlAnalysisResult();
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnA"));
        analysisA.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnB"));
        List<SemanticRegionResult> regionsA = new SemanticRegionSegmenter().segment(formA, analysisA);

        Document docB = newDocument();
        Element formB = buildSingleButtonEventOrderForm(docB);
        XfdlAnalysisResult analysisB = new XfdlAnalysisResult();
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnB"));
        analysisB.getEvents().add(new EventBinding("btnGroupOrder.btnSolo", "onclick", "fnA"));
        List<SemanticRegionResult> regionsB = new SemanticRegionSegmenter().segment(formB, analysisB);

        assertConflictingHandlerEventCountIsZero("conflicting_handler_reverse: forward order", regionsA);
        assertConflictingHandlerEventCountIsZero("conflicting_handler_reverse: reversed order", regionsB);
    }

    private static void assertConflictingHandlerEventCountIsZero(String label, List<SemanticRegionResult> regions) {
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        int eventCount = 0;
        for (SourcePayloadEvidenceItem item : buttonGroup.getPayloadEvidence()) {
            if ("event".equals(item.getEvidenceRole())) eventCount++;
        }
        assertEquals(label + ": 0 EVENT regardless of input order", "0", String.valueOf(eventCount));
    }

    /** btnA/btnB 각각 자기 event를 가지며, EventBinding 전역 입력 순서를 섞어도 Button
     * predicate source order가 보존되고, 각 Button의 event는 자기 자신에게만 정확히
     * correlate돼야 한다(global alphabetic order로 섞이지 않음). */
    private static void testMultiButtonMixedEventOrder() throws Exception {
        Document docForward = newDocument();
        Element formForward = buildTwoButtonEventOrderForm(docForward);
        XfdlAnalysisResult analysisForward = new XfdlAnalysisResult();
        analysisForward.getEvents().add(new EventBinding("btnGroupMulti.btnA", "onclick", "fnA"));
        analysisForward.getEvents().add(new EventBinding("btnGroupMulti.btnB", "onclick", "fnB"));
        List<SemanticRegionResult> regionsForward =
                new SemanticRegionSegmenter().segment(formForward, analysisForward);

        Document docShuffled = newDocument();
        Element formShuffled = buildTwoButtonEventOrderForm(docShuffled);
        XfdlAnalysisResult analysisShuffled = new XfdlAnalysisResult();
        analysisShuffled.getEvents().add(new EventBinding("btnGroupMulti.btnB", "onclick", "fnB"));
        analysisShuffled.getEvents().add(new EventBinding("btnGroupMulti.btnA", "onclick", "fnA"));
        List<SemanticRegionResult> regionsShuffled =
                new SemanticRegionSegmenter().segment(formShuffled, analysisShuffled);

        assertEquals("multi_button_mixed_order: ordered tuples identical regardless of global "
                        + "EventBinding input order",
                eventEvidenceTuplesOf(regionsForward), eventEvidenceTuplesOf(regionsShuffled));

        SemanticRegionResult buttonGroup = findRegionByFamily(regionsForward, "BUTTON_GROUP");
        List<SourcePayloadEvidenceItem> evidence = buttonGroup.getPayloadEvidence();
        Element btnA = (Element) formForward.getElementsByTagName("Button").item(0);
        Element btnB = (Element) formForward.getElementsByTagName("Button").item(1);
        int btnASourceOrder = -1, btnBSourceOrder = -1;
        for (SourcePayloadEvidenceItem item : evidence) {
            if (!"event".equals(item.getEvidenceRole())) continue;
            if (structuralIdOf(btnA).equals(item.getSourceComponentStructuralId())) btnASourceOrder = item.getSourceOrder();
            if (structuralIdOf(btnB).equals(item.getSourceComponentStructuralId())) btnBSourceOrder = item.getSourceOrder();
        }
        assertTrue("multi_button_mixed_order: btnA EVENT found", btnASourceOrder >= 0);
        assertTrue("multi_button_mixed_order: btnB EVENT found", btnBSourceOrder >= 0);
        assertTrue("multi_button_mixed_order: btnA(source-first) EVENT ordered before btnB "
                        + "(Button predicate source order preserved, not global alphabetic)",
                btnASourceOrder < btnBSourceOrder);
    }

    // ---- Source Payload Evidence 기반 ----

    /** Segmenter가 predicate 판정 시점에 실제 (label, control) 2 pair를 evidence로 남기는지
     * 직접 확인한다(TargetPayloadExtractor를 거치지 않고 SemanticRegionResult 자체를 검사). */
    private static void testSearchAreaEvidenceCapturedAtPredicateTime() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);

        SemanticRegionResult searchArea = null;
        for (SemanticRegionResult region : regions) {
            if ("SEARCH_AREA".equals(region.getSemanticType())) {
                searchArea = region;
            }
        }
        assertTrue("search_area_evidence: SEARCH_AREA region found", searchArea != null);
        List<SourcePayloadEvidenceItem> evidence = searchArea.getPayloadEvidence();
        assertEquals("search_area_evidence: 2 pairs -> 4 items", "4", String.valueOf(evidence.size()));

        Element label1 = (Element) form.getElementsByTagName("Static").item(1); // titleText1이 index 0이다.
        Element edit1 = (Element) form.getElementsByTagName("Edit").item(0);
        Element label2 = (Element) form.getElementsByTagName("Static").item(2);
        Element combo1 = (Element) form.getElementsByTagName("Combo").item(0);

        boolean sawLabel1 = false, sawEdit1 = false, sawLabel2 = false, sawCombo1 = false;
        for (SourcePayloadEvidenceItem item : evidence) {
            assertEquals("search_area_evidence: semanticRegionStructuralId matches region",
                    searchArea.getSourceStructuralId(), item.getSemanticRegionStructuralId());
            if ("label".equals(item.getEvidenceRole()) && "이름".equals(item.getValue())) {
                assertEquals("search_area_evidence: label1 structuralId exact",
                        structuralIdOf(label1), item.getSourceComponentStructuralId());
                sawLabel1 = true;
            }
            if ("control".equals(item.getEvidenceRole()) && "Edit".equals(item.getValue())
                    && structuralIdOf(edit1).equals(item.getSourceComponentStructuralId())) {
                sawEdit1 = true;
            }
            if ("label".equals(item.getEvidenceRole()) && "상태".equals(item.getValue())) {
                assertEquals("search_area_evidence: label2 structuralId exact",
                        structuralIdOf(label2), item.getSourceComponentStructuralId());
                sawLabel2 = true;
            }
            if ("control".equals(item.getEvidenceRole()) && "Combo".equals(item.getValue())
                    && structuralIdOf(combo1).equals(item.getSourceComponentStructuralId())) {
                sawCombo1 = true;
            }
        }
        assertTrue("search_area_evidence: label1(이름) exact identity", sawLabel1);
        assertTrue("search_area_evidence: control1(Edit) exact identity", sawEdit1);
        assertTrue("search_area_evidence: label2(상태) exact identity", sawLabel2);
        assertTrue("search_area_evidence: control2(Combo) exact identity", sawCombo1);
    }

    /** TargetPayloadExtractor는 evidence만 읽어 DISPLAY_TEXT 2 + CONTROL_TYPE 2를 만든다 --
     * source DOM/geometry를 다시 계산하지 않는다(항목 7). */
    private static void testSearchAreaPayloadFromEvidenceOnly() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        Element search = (Element) form.getElementsByTagName("Div").item(2);
        String planNodeId = structuralIdOf(search);

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, planNodeId);
        assertTrue("search_area_payload: payload present", payload != null);

        int displayTextCount = 0;
        int controlTypeCount = 0;
        boolean sawName = false, sawStatus = false, sawEdit = false, sawCombo = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.DISPLAY_TEXT) {
                displayTextCount++;
                if ("이름".equals(item.getValue())) sawName = true;
                if ("상태".equals(item.getValue())) sawStatus = true;
            } else if (item.getCategory() == TargetPayloadCategory.CONTROL_TYPE) {
                controlTypeCount++;
                if ("Edit".equals(item.getValue())) sawEdit = true;
                if ("Combo".equals(item.getValue())) sawCombo = true;
            } else {
                failures++;
                System.out.println("[FAIL] search_area_payload: unexpected category " + item.getCategory());
            }
        }
        assertEquals("search_area_payload: DISPLAY_TEXT count", "2", String.valueOf(displayTextCount));
        assertEquals("search_area_payload: CONTROL_TYPE count", "2", String.valueOf(controlTypeCount));
        assertTrue("search_area_payload: label 이름", sawName);
        assertTrue("search_area_payload: label 상태", sawStatus);
        assertTrue("search_area_payload: control Edit", sawEdit);
        assertTrue("search_area_payload: control Combo", sawCombo);
    }

    /**
     * search1의 label/control pair 뒤 투명 wrapper Div 안에 Grid가 중첩돼도 wrapper-normalization
     * peer search가 통과해 SEARCH_AREA로 판정된다. 이때도 evidence의 leaf identity는 wrapper가
     * 아니라 label/control Element 자신을 정확히 가리켜야 한다.
     */
    private static void testSearchAreaWrapperNormalizationEvidenceUsesPredicateLeaf() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element search = doc.createElement("Div");
        search.setAttribute("id", "search1");
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        label1.setAttribute("text", "이름");
        label1.setAttribute("left", "0");
        label1.setAttribute("top", "0");
        label1.setAttribute("width", "50");
        label1.setAttribute("height", "20");
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "editName");
        edit1.setAttribute("left", "60");
        edit1.setAttribute("top", "0");
        edit1.setAttribute("width", "100");
        edit1.setAttribute("height", "20");
        search.appendChild(label1);
        search.appendChild(edit1);
        form.appendChild(search);

        Element transparentWrapper = doc.createElement("Div");
        transparentWrapper.setAttribute("id", "transparentWrap");
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "nestedGrid1");
        transparentWrapper.appendChild(nestedGrid);
        form.appendChild(transparentWrapper);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = null;
        for (SemanticRegionResult region : regions) {
            if ("SEARCH_AREA".equals(region.getSemanticType())) {
                searchArea = region;
            }
        }
        assertTrue("wrapper_normalization_evidence: SEARCH_AREA emitted through transparent wrapper",
                searchArea != null);
        List<SourcePayloadEvidenceItem> evidence = searchArea.getPayloadEvidence();
        assertEquals("wrapper_normalization_evidence: 1 pair -> 2 items", "2", String.valueOf(evidence.size()));
        boolean labelMatches = false;
        boolean controlMatches = false;
        for (SourcePayloadEvidenceItem item : evidence) {
            if ("label".equals(item.getEvidenceRole())) {
                labelMatches = structuralIdOf(label1).equals(item.getSourceComponentStructuralId());
            }
            if ("control".equals(item.getEvidenceRole())) {
                controlMatches = structuralIdOf(edit1).equals(item.getSourceComponentStructuralId());
            }
        }
        assertTrue("wrapper_normalization_evidence: label identity exact (not wrapper)", labelMatches);
        assertTrue("wrapper_normalization_evidence: control identity exact (not wrapper)", controlMatches);

        // payload layer는 wrapper를 전혀 건드리지 않음을 전체 파이프라인으로도 확인한다.
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(search));
        assertTrue("wrapper_normalization_evidence: payload present via full pipeline", payload != null);
        assertEquals("wrapper_normalization_evidence: 1 DISPLAY_TEXT + 1 CONTROL_TYPE",
                "2", String.valueOf(payload.getItems().size()));
    }

    /** SEARCH_AREA/BUSINESS_TABLE 둘 다 emit되지 않는 구조(교대 쌍이 아님)에서는 evidence도
     * 전혀 생성되지 않는다 -- SemanticRegionResult 자체가 만들어지지 않으므로 자명하지만,
     * payload 생성 경로에도 아무 영향이 없음을 함께 확인한다. */
    private static void testSearchAreaNoEmissionMeansNoEvidence() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element container = doc.createElement("Div");
        container.setAttribute("id", "notAPair");
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        label1.setAttribute("text", "이름");
        label1.setAttribute("left", "0");
        label1.setAttribute("top", "0");
        label1.setAttribute("width", "50");
        label1.setAttribute("height", "20");
        Element label2 = doc.createElement("Static");
        label2.setAttribute("id", "lbl2");
        label2.setAttribute("text", "설명");
        label2.setAttribute("left", "60");
        label2.setAttribute("top", "0");
        label2.setAttribute("width", "50");
        label2.setAttribute("height", "20");
        // Static, Static -- 교대 (label, control) 쌍이 아니므로 공용 gate가 거부한다.
        container.appendChild(label1);
        container.appendChild(label2);
        form.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult region : regions) {
            assertTrue("no_emission: no SEARCH_AREA/BUSINESS_TABLE emitted",
                    !"SEARCH_AREA".equals(region.getSemanticType()) && !"BUSINESS_TABLE".equals(region.getSemanticType()));
        }
        assertEquals("no_emission: no regions at all for this fixture", "0", String.valueOf(regions.size()));
    }

    /**
     * SEARCH_AREA/BUSINESS_TABLE이 공유하는 gate이므로 evidence는 동일하게 capture되고,
     * label→DISPLAY_TEXT/control→CONTROL_TYPE payload가 투영된다. row/cell 같은 구조적
     * placement는 evidence에 없으므로 여전히 만들지 않는다.
     */
    private static void testBusinessTablePayloadFromEvidenceOnly() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element container = doc.createElement("Div");
        container.setAttribute("id", "table1");
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        label1.setAttribute("text", "품목");
        label1.setAttribute("left", "0");
        label1.setAttribute("top", "0");
        label1.setAttribute("width", "50");
        label1.setAttribute("height", "20");
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "editItem");
        edit1.setAttribute("left", "60");
        edit1.setAttribute("top", "0");
        edit1.setAttribute("width", "100");
        edit1.setAttribute("height", "20");
        Element label2 = doc.createElement("Static");
        label2.setAttribute("id", "lbl2");
        label2.setAttribute("text", "수량");
        label2.setAttribute("left", "0");
        label2.setAttribute("top", "30");
        label2.setAttribute("width", "50");
        label2.setAttribute("height", "20");
        Element edit2 = doc.createElement("Edit");
        edit2.setAttribute("id", "editQty");
        edit2.setAttribute("left", "60");
        edit2.setAttribute("top", "30");
        edit2.setAttribute("width", "100");
        edit2.setAttribute("height", "20");
        // scope 안에 Grid가 없어 wrapper-normalization peer search가 실패 -> BUSINESS_TABLE.
        container.appendChild(label1);
        container.appendChild(edit1);
        container.appendChild(label2);
        container.appendChild(edit2);
        form.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = null;
        for (SemanticRegionResult region : regions) {
            if ("BUSINESS_TABLE".equals(region.getSemanticType())) {
                businessTable = region;
            }
        }
        assertTrue("business_table_evidence: BUSINESS_TABLE emitted", businessTable != null);
        assertEquals("business_table_evidence: 2 pairs -> 4 items captured (shared gate)",
                "4", String.valueOf(businessTable.getPayloadEvidence().size()));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(container));
        assertTrue("business_table_payload: payload present", payload != null);

        int displayTextCount = 0;
        int controlTypeCount = 0;
        boolean sawItem = false, sawQty = false, sawEditItem = false, sawEditQty = false;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.DISPLAY_TEXT) {
                displayTextCount++;
                if ("품목".equals(item.getValue())) {
                    sawItem = true;
                    assertStructuralTuple("business_table_payload: label 품목", item, 0, 0, 0);
                }
                if ("수량".equals(item.getValue())) {
                    sawQty = true;
                    // 다른 row이지만 품목과 같은 pairIndexInRow(0) -- row-local이며 rowIndex로 구분된다.
                    assertStructuralTuple("business_table_payload: label 수량", item, 1, 0, 0);
                }
            } else if (item.getCategory() == TargetPayloadCategory.CONTROL_TYPE) {
                controlTypeCount++;
                if ("Edit".equals(item.getValue()) && structuralIdOf(edit1).equals(item.getSourceComponentStructuralId())) {
                    sawEditItem = true;
                    assertStructuralTuple("business_table_payload: control editItem", item, 0, 1, 0);
                }
                if ("Edit".equals(item.getValue()) && structuralIdOf(edit2).equals(item.getSourceComponentStructuralId())) {
                    sawEditQty = true;
                    assertStructuralTuple("business_table_payload: control editQty", item, 1, 1, 0);
                }
            } else {
                failures++;
                System.out.println("[FAIL] business_table_payload: unexpected category " + item.getCategory());
            }
        }
        assertEquals("business_table_payload: DISPLAY_TEXT count", "2", String.valueOf(displayTextCount));
        assertEquals("business_table_payload: CONTROL_TYPE count", "2", String.valueOf(controlTypeCount));
        assertTrue("business_table_payload: label 품목", sawItem);
        assertTrue("business_table_payload: label 수량", sawQty);
        assertTrue("business_table_payload: control editItem", sawEditItem);
        assertTrue("business_table_payload: control editQty", sawEditQty);
    }

    /** {@code structuredData}의 rowIndex/cellIndexInRow/pairIndexInRow가 예상 값과 정확히
     * 일치하는지 확인하는 공용 assertion helper. */
    private static void assertStructuralTuple(
            String label, TargetLeafPayload item, int expectedRow, int expectedCell, int expectedPair) {
        assertEquals(label + ": rowIndex", String.valueOf(expectedRow),
                String.valueOf(item.getStructuredData().get("rowIndex")));
        assertEquals(label + ": cellIndexInRow", String.valueOf(expectedCell),
                String.valueOf(item.getStructuredData().get("cellIndexInRow")));
        assertEquals(label + ": pairIndexInRow", String.valueOf(expectedPair),
                String.valueOf(item.getStructuredData().get("pairIndexInRow")));
    }

    /** Binding collision regression: Tabpage/GroupBox 아래 같은 compid를 가진 Edit 2개가 있어도
     * binding structural evidence가 추측 생성되지 않는다(BINDING 생성 경로 자체가 없음). */
    private static void testBindingCollisionNeverGuessed() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element groupA = doc.createElement("GroupBox");
        groupA.setAttribute("id", "groupA");
        Element editA = doc.createElement("Edit");
        editA.setAttribute("id", "sameEdit");
        groupA.appendChild(editA);
        form.appendChild(groupA);

        Element groupB = doc.createElement("GroupBox");
        groupB.setAttribute("id", "groupB");
        Element editB = doc.createElement("Edit");
        editB.setAttribute("id", "sameEdit");
        groupB.appendChild(editB);
        form.appendChild(groupB);

        Element bindItem = doc.createElement("BindItem");
        bindItem.setAttribute("compid", "sameEdit");
        bindItem.setAttribute("propid", "value");
        bindItem.setAttribute("datasetid", "ds1");
        bindItem.setAttribute("columnid", "COL1");
        form.appendChild(bindItem);
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", "ds1");
        form.appendChild(dataset);

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        for (TargetNodePayload payload : payloads) {
            for (TargetLeafPayload item : payload.getItems()) {
                assertTrue("binding_collision: no BINDING structural evidence guessed",
                        item.getCategory() != TargetPayloadCategory.BINDING);
            }
        }
    }

    // ==== BUSINESS_TABLE Evidence-Only Target Payload 투영 ====

    /** label1+control1(품목/Edit), label2+control2(수량/Edit) 2 pair -- Grid가 scope 안에
     * 없으므로 wrapper-normalization peer search가 실패해 BUSINESS_TABLE로 판정된다. */
    private static Element buildBusinessTableFixtureForm(Document doc) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element container = doc.createElement("Div");
        container.setAttribute("id", "table1");
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        label1.setAttribute("text", "품목");
        label1.setAttribute("left", "0");
        label1.setAttribute("top", "0");
        label1.setAttribute("width", "50");
        label1.setAttribute("height", "20");
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "editItem");
        edit1.setAttribute("left", "60");
        edit1.setAttribute("top", "0");
        edit1.setAttribute("width", "100");
        edit1.setAttribute("height", "20");
        Element label2 = doc.createElement("Static");
        label2.setAttribute("id", "lbl2");
        label2.setAttribute("text", "수량");
        label2.setAttribute("left", "0");
        label2.setAttribute("top", "30");
        label2.setAttribute("width", "50");
        label2.setAttribute("height", "20");
        Element edit2 = doc.createElement("Edit");
        edit2.setAttribute("id", "editQty");
        edit2.setAttribute("left", "60");
        edit2.setAttribute("top", "30");
        edit2.setAttribute("width", "100");
        edit2.setAttribute("height", "20");
        container.appendChild(label1);
        container.appendChild(edit1);
        container.appendChild(label2);
        container.appendChild(edit2);
        form.appendChild(container);
        return form;
    }

    private static SemanticRegionResult businessTableRegion(String structuralId, int columnPairCount, int rowCount) {
        SemanticRegionResult r = new SemanticRegionResult();
        r.setSemanticType("BUSINESS_TABLE");
        r.setRecommendedTemplateFamily("BUSINESS_TABLE");
        r.setRecommendedVariant("horizontal");
        r.setConfidence("HIGH");
        r.setSourceStructuralId(structuralId);
        r.getParameters().put("column_pair_count", columnPairCount);
        r.getParameters().put("row_count", rowCount);
        return r;
    }

    /** 항목 8 missing region: SOURCE_SEMANTIC BUSINESS_TABLE Plan node가 있는데 regions에서 매칭되는
     * region이 0개면 -- evidence-backed projection이므로 명시적 실패다(SEARCH_AREA와 동일 계약,
     * 별도 loose validator 없음). */
    private static void testBusinessTableMissingRegionExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div = doc.createElement("Div");
        form.appendChild(div);
        String x = structuralIdOf(div);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(businessTableRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        final List<SemanticRegionResult> emptyRegions = new ArrayList<SemanticRegionResult>();

        assertThrowsIllegalState("business_table_missing_region", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, emptyRegions);
            }
        });
    }

    /** 항목 8 wrong-family same structuralId: Plan node는 BUSINESS_TABLE/structuralId=X를
     * 가리키는데, 실제 공급된 regions에는 같은 X에 SEARCH_AREA region만 있으면 -- family
     * mismatch로 명시적 실패해야 한다(항목 9 family isolation과도 직결). */
    private static void testBusinessTableWrongFamilySameAnchorExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div0 = doc.createElement("Div");
        form.appendChild(div0);
        String x = structuralIdOf(div0);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(businessTableRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult searchAreaRegionAtX = searchAreaRegion(x, 1, 1);
        searchAreaRegionAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Static[0]", "label", "source_text_attribute", "품목", 0));
        searchAreaRegionAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Edit[1]", "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(searchAreaRegionAtX);

        assertThrowsIllegalState("business_table_wrong_family", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 8 duplicate region: 같은 structuralId를 가진 BUSINESS_TABLE region이 2개면 -- silent
     * last-wins 대신 명시적 실패. */
    private static void testBusinessTableDuplicateRegionExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div1 = doc.createElement("Div");
        form.appendChild(div1);
        String x = structuralIdOf(div1);

        SemanticRegionResult regionA = businessTableRegion(x, 1, 1);
        SemanticRegionResult regionB = businessTableRegion(x, 1, 1);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(businessTableRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(regionA);
        regions.add(regionB);

        assertThrowsIllegalState("business_table_duplicate_region", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 8 owner mismatch: evidence.semanticRegionStructuralId가 자신을 소유한 BUSINESS_TABLE
     * region의 sourceStructuralId와 다르면 -- 명시적 실패. */
    private static void testBusinessTableOwnerMismatchExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div2 = doc.createElement("Div");
        form.appendChild(div2);
        String x = structuralIdOf(div2);
        String y = "Form[0]/Div[9]";

        SemanticRegionResult region = businessTableRegion(x, 1, 1);
        region.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                y, "Form[0]/Div[9]/Static[0]", "label", "source_text_attribute", "다른구역라벨", 0));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(businessTableRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(region);

        assertThrowsIllegalState("business_table_owner_mismatch", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 8 missing component: 실제 Segmenter가 만든 진짜 BUSINESS_TABLE region에, sourceRoot
     * 안에 존재하지 않는 sourceComponentStructuralId를 가진 evidence를 덧붙이면 -- 명시적 실패. */
    private static void testBusinessTableMissingComponentExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_missing_component: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        businessTable.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                businessTable.getSourceStructuralId(), "Form[0]/Div[0]/Static[99]", "label",
                "source_text_attribute", "존재하지않음", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("business_table_missing_component", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 항목 8 outside subtree: sourceRoot 안에는 실존하지만 BUSINESS_TABLE region subtree 밖에
     * 있는 Element(별도 독립 Div의 Static)를 가리키는 evidence -- 실제 parent chain 검증으로
     * 명시적 실패. */
    private static void testBusinessTableOutsideSubtreeExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        Element outsideStatic = doc.createElement("Static");
        outsideStatic.setAttribute("id", "outsideLabel");
        outsideStatic.setAttribute("text", "바깥라벨");
        form.appendChild(outsideStatic); // descendant가 아닌 BUSINESS_TABLE container의 sibling이다.

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_outside_subtree: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        businessTable.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                businessTable.getSourceStructuralId(), structuralIdOf(outsideStatic), "label",
                "source_text_attribute", "바깥라벨", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("business_table_outside_subtree", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 항목 8 wrong role/kind: evidenceKind="source_text_attribute"인데 value가 없으면(모순된
     * evidence) -- 명시적 실패(SEARCH_AREA의 동일 검증을 그대로 재사용, 별도 BUSINESS_TABLE
     * validator 없음). */
    private static void testBusinessTableWrongKindExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_wrong_kind: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element label1 = (Element) form.getElementsByTagName("Static").item(0);
        businessTable.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                businessTable.getSourceStructuralId(), structuralIdOf(label1), "label",
                "source_text_attribute", null, 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("business_table_wrong_kind", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 항목 8 role/source Element mismatch: role="control"이 실제로는 Static(허용 어휘 밖)을
     * 가리키면 -- producer invariant 위반으로 명시적 실패. */
    private static void testBusinessTableRoleSourceElementMismatchExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_role_mismatch: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element label1 = (Element) form.getElementsByTagName("Static").item(0);
        businessTable.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                businessTable.getSourceStructuralId(), structuralIdOf(label1), "control",
                "source_tag_name", "Static", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("business_table_role_mismatch", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 항목 9 SEARCH_AREA isolation: Plan node가 SEARCH_AREA인데 실제 공급된 regions에는 같은
     * structuralId의 BUSINESS_TABLE region만 있으면 -- family mismatch로 명시적 실패해야
     * 한다(BUSINESS_TABLE evidence를 SEARCH_AREA가 빌려쓰면 안 됨). */
    private static void testSearchAreaCannotUseBusinessTableEvidence() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div = doc.createElement("Div");
        form.appendChild(div);
        String x = structuralIdOf(div);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(searchAreaRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult businessTableAtX = businessTableRegion(x, 1, 1);
        businessTableAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Static[0]", "label", "source_text_attribute", "품목", 0));
        businessTableAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Edit[1]", "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(businessTableAtX);

        assertThrowsIllegalState("search_area_cannot_use_business_table_evidence", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 9 BUSINESS_TABLE isolation: 반대 방향(Plan은 BUSINESS_TABLE, 공급된 region은
     * SEARCH_AREA)도 동일하게 명시적 실패해야 한다. */
    private static void testBusinessTableCannotUseSearchAreaEvidence() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div = doc.createElement("Div");
        form.appendChild(div);
        String x = structuralIdOf(div);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(businessTableRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult searchAreaAtX = searchAreaRegion(x, 1, 1);
        searchAreaAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Static[0]", "label", "source_text_attribute", "이름", 0));
        searchAreaAtX.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Edit[1]", "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(searchAreaAtX);

        assertThrowsIllegalState("business_table_cannot_use_search_area_evidence", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 10 order independence: regions list 순서와 evidence 저장 순서를 뒤집어도 explicit
     * sourceOrder 기준으로 BUSINESS_TABLE payload가 동일해야 한다. */
    private static void testBusinessTableOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_order_independence: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element container = (Element) form.getElementsByTagName("Div").item(0);
        List<TargetNodePayload> payloadsForward = extractWithBindings(form, plan, regions);
        TargetNodePayload forward = findPayload(payloadsForward, structuralIdOf(container));

        List<SemanticRegionResult> reversedRegions = new ArrayList<SemanticRegionResult>(regions);
        java.util.Collections.reverse(reversedRegions);
        java.util.Collections.reverse(businessTable.getPayloadEvidence());

        List<TargetNodePayload> payloadsReversed = extractWithBindings(form, plan, reversedRegions);
        TargetNodePayload reversed = findPayload(payloadsReversed, structuralIdOf(container));

        assertTrue("business_table_order_independence: forward present", forward != null);
        assertTrue("business_table_order_independence: reversed present", reversed != null);
        assertEquals("business_table_order_independence: same item count",
                String.valueOf(forward.getItems().size()), String.valueOf(reversed.getItems().size()));
        for (int i = 0; i < forward.getItems().size(); i++) {
            assertEquals("business_table_order_independence: item[" + i + "] value matches",
                    forward.getItems().get(i).getValue(), reversed.getItems().get(i).getValue());
        }
    }

    // ---- BUSINESS_TABLE Structural Leaf Payload 기반 ----

    /** Asymmetric-row fixture(row0=2 pairs, row1=1 pair) -- 실제 파이프라인을 통과해 각 leaf의
     * {@code structuredData}에 정확한 rowIndex/cellIndexInRow/pairIndexInRow가 담기는지 확인한다. */
    private static void testBusinessTableAsymmetricRowsStructuredDataExact() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element container = doc.createElement("Div");
        container.setAttribute("id", "asymTable");
        Element r0l0 = mkCell(doc, "Static", "r0l0", "A", 0, 0, 50, 20);
        Element r0c0 = mkCell(doc, "Edit", "r0c0", null, 60, 0, 100, 20);
        Element r0l1 = mkCell(doc, "Static", "r0l1", "B", 160, 0, 50, 20);
        Element r0c1 = mkCell(doc, "Edit", "r0c1", null, 220, 0, 100, 20);
        Element r1l0 = mkCell(doc, "Static", "r1l0", "C", 0, 30, 50, 20);
        Element r1c0 = mkCell(doc, "Edit", "r1c0", null, 60, 30, 100, 20);
        container.appendChild(r0l0);
        container.appendChild(r0c0);
        container.appendChild(r0l1);
        container.appendChild(r0c1);
        container.appendChild(r1l0);
        container.appendChild(r1c0);
        form.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(container));
        assertTrue("asymmetric_structured_data: payload present", payload != null);
        assertEquals("asymmetric_structured_data: 6 items", "6", String.valueOf(payload.getItems().size()));

        for (TargetLeafPayload item : payload.getItems()) {
            String id = item.getSourceComponentStructuralId();
            if (id.equals(structuralIdOf(r0l0))) assertStructuralTuple("r0l0", item, 0, 0, 0);
            else if (id.equals(structuralIdOf(r0c0))) assertStructuralTuple("r0c0", item, 0, 1, 0);
            else if (id.equals(structuralIdOf(r0l1))) assertStructuralTuple("r0l1", item, 0, 2, 1);
            else if (id.equals(structuralIdOf(r0c1))) assertStructuralTuple("r0c1", item, 0, 3, 1);
            else if (id.equals(structuralIdOf(r1l0))) assertStructuralTuple("r1l0", item, 1, 0, 0);
            else if (id.equals(structuralIdOf(r1c0))) assertStructuralTuple("r1c0", item, 1, 1, 0);
            else {
                failures++;
                System.out.println("[FAIL] asymmetric_structured_data: unexpected leaf " + id);
            }
        }
    }

    private static Element mkCell(Document doc, String tag, String id, String text, int left, int top, int width,
            int height) {
        Element e = doc.createElement(tag);
        e.setAttribute("id", id);
        if (text != null) e.setAttribute("text", text);
        e.setAttribute("left", String.valueOf(left));
        e.setAttribute("top", String.valueOf(top));
        e.setAttribute("width", String.valueOf(width));
        e.setAttribute("height", String.valueOf(height));
        return e;
    }

    /** 항목 1 null structural field tamper: rowIndex가 null인 BUSINESS_TABLE label evidence는
     * 명시적 실패해야 한다. */
    private static void testBusinessTableNullRowIndexExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 0,
                null, Integer.valueOf(0), Integer.valueOf(0)));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", null, 1,
                Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(0)));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_null_row_index", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 2 negative index tamper: {@link SourcePayloadEvidenceItem} 생성자 자신이 이미
     * 음수 rowIndex를 막는다(모델 레벨 guard) -- extractor까지 갈 필요 없이 생성 시점에 즉시
     * 실패해야 한다. */
    private static void testBusinessTableNegativeRowIndexRejectedAtConstruction() throws Exception {
        boolean threw = false;
        try {
            new SourcePayloadEvidenceItem(
                    "Form[0]/Div[0]", "Form[0]/Div[0]/Static[0]", "label", "source_text_attribute", "품목", null,
                    0, Integer.valueOf(-1), Integer.valueOf(0), Integer.valueOf(0));
        } catch (IllegalArgumentException e) {
            threw = true;
            System.out.println("[PASS] business_table_negative_row_index: rejected at construction -- "
                    + e.getMessage());
        }
        assertTrue("business_table_negative_row_index: IllegalArgumentException thrown", threw);
    }

    /** 항목 4 conflicting structural tuple: 같은 sourceComponentStructuralId를 가리키는 evidence
     * 2개가 서로 다른 row/cell/pair 튜플을 주장하면 -- 명시적 실패(둘 중 하나를 조용히 고르지
     * 않음). */
    private static void testBusinessTableConflictingStructuralTupleExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        // 같은 sourceComponentStructuralId(outLabel[0])가 서로 다른 튜플을 두 번 주장한다.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 0,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 1,
                Integer.valueOf(5), Integer.valueOf(5), Integer.valueOf(5)));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_conflicting_tuple", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 5 duplicate row+cell ownership: 서로 다른 두 source component가 같은
     * (rowIndex, cellIndexInRow)를 주장하면 -- 명시적 실패. */
    private static void testBusinessTableDuplicateRowCellOwnershipExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 0,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));
        // 다른 실제 component(outControl[0])도 같은 (rowIndex=0, cellIndexInRow=0)을 주장한다.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", null, 1,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_duplicate_row_cell_ownership", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 8 inconsistent pair metadata: label의 cellIndexInRow가 producer invariant
     * (pairIndexInRow*2)와 어긋나면 -- 명시적 실패(sourceOrder를 다시 읽지 않고, 이미 있는
     * 필드들 사이의 정합성만 확인). */
    private static void testBusinessTableInconsistentPairMetadataExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        // label이 pairIndexInRow=0(cellIndexInRow=0 기대)인데 cellIndexInRow=3 -- 불일치.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 0,
                Integer.valueOf(0), Integer.valueOf(3), Integer.valueOf(0)));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", null, 1,
                Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(0)));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_inconsistent_pair_metadata", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 7 cardinality violation: 같은 (rowIndex, pairIndexInRow)에 label이 2개(control 0개)면
     * -- producer가 실제로 보장하는 정확한 1:1 cardinality 위반으로 명시적 실패. */
    private static void testBusinessTablePairCardinalityViolationExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element container = doc.createElement("Div");
        container.setAttribute("id", "table1");
        Element label1 = mkCell(doc, "Static", "lbl1", "A", 0, 0, 50, 20);
        Element label2 = mkCell(doc, "Static", "lbl2", "B", 60, 0, 50, 20);
        container.appendChild(label1);
        container.appendChild(label2);
        form.appendChild(container);
        String x = structuralIdOf(container);

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        // label 2개는 각각 내부적으로 정합적(cellIndexInRow == pairIndexInRow*2)이라 정합성
        // 검사는 통과하지만 둘 다 control이 없다 -- cardinality 규칙(1 label + 1 control)만
        // 따로 검증한다.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(label1), "label", "source_text_attribute", "A", null, 0,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(label2), "label", "source_text_attribute", "B", null, 1,
                Integer.valueOf(0), Integer.valueOf(2), Integer.valueOf(1)));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        try {
            extractWithBindings(form, plan, regions);
            failures++;
            System.out.println("[FAIL] business_table_pair_cardinality_violation: expected IllegalStateException "
                    + "but none was thrown");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("must have exactly 1 label + 1 control")) {
                System.out.println("[PASS] business_table_pair_cardinality_violation: explicit failure -- "
                        + e.getMessage());
            } else {
                failures++;
                System.out.println("[FAIL] business_table_pair_cardinality_violation: failed for the wrong "
                        + "reason -- " + e.getMessage());
            }
        }
    }

    /** 항목 10 input-order determinism: BUSINESS_TABLE evidence 저장 순서를 뒤집어도, 각 leaf의
     * structuredData 값(row/cell/pair)이 정확히 동일해야 한다(집계 검증이 Map 기반이라 순서
     * 무관). */
    private static void testBusinessTableStructuralDataOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildBusinessTableFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult businessTable = findRegionByFamily(regions, "BUSINESS_TABLE");
        assertTrue("business_table_structural_order_independence: region found", businessTable != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        Element container = (Element) form.getElementsByTagName("Div").item(0);

        List<TargetNodePayload> forwardPayloads = extractWithBindings(form, plan, regions);
        TargetNodePayload forward = findPayload(forwardPayloads, structuralIdOf(container));

        java.util.Collections.reverse(businessTable.getPayloadEvidence());
        List<TargetNodePayload> reversedPayloads = extractWithBindings(form, plan, regions);
        TargetNodePayload reversed = findPayload(reversedPayloads, structuralIdOf(container));

        assertEquals("business_table_structural_order_independence: same item count",
                String.valueOf(forward.getItems().size()), String.valueOf(reversed.getItems().size()));
        for (int i = 0; i < forward.getItems().size(); i++) {
            TargetLeafPayload f = forward.getItems().get(i);
            TargetLeafPayload r = reversed.getItems().get(i);
            assertEquals("business_table_structural_order_independence: item[" + i + "] rowIndex",
                    String.valueOf(f.getStructuredData().get("rowIndex")),
                    String.valueOf(r.getStructuredData().get("rowIndex")));
            assertEquals("business_table_structural_order_independence: item[" + i + "] cellIndexInRow",
                    String.valueOf(f.getStructuredData().get("cellIndexInRow")),
                    String.valueOf(r.getStructuredData().get("cellIndexInRow")));
            assertEquals("business_table_structural_order_independence: item[" + i + "] pairIndexInRow",
                    String.valueOf(f.getStructuredData().get("pairIndexInRow")),
                    String.valueOf(r.getStructuredData().get("pairIndexInRow")));
        }
    }

    /** SEARCH_AREA는 BUSINESS_TABLE과 동일한 producer를 공유하므로 evidence의
     * rowIndex/cellIndexInRow/pairIndexInRow가 SEARCH_AREA {@code structuredData}에도
     * 재계산 없이 그대로(exact-copy) 반영돼야 한다. */
    private static void testSearchAreaStructuredDataPreservedExactCopy() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        Fixture fx = buildFixture(form);

        Element search = (Element) form.getElementsByTagName("Div").item(2);
        SemanticRegionResult searchArea = findSearchArea(fx.regions);
        assertTrue("search_area_structured_data_preserved: SEARCH_AREA region found", searchArea != null);
        Map<String, SourcePayloadEvidenceItem> evidenceByComponent = new LinkedHashMap<String, SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : searchArea.getPayloadEvidence()) {
            evidenceByComponent.put(item.getSourceComponentStructuralId(), item);
        }

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(search));
        assertTrue("search_area_structured_data_preserved: payload present", payload != null);
        assertTrue("search_area_structured_data_preserved: at least one leaf", !payload.getItems().isEmpty());
        for (TargetLeafPayload item : payload.getItems()) {
            SourcePayloadEvidenceItem source = evidenceByComponent.get(item.getSourceComponentStructuralId());
            assertTrue("search_area_structured_data_preserved: matching source evidence exists for "
                    + item.getSourceComponentStructuralId(), source != null);
            assertEquals("search_area_structured_data_preserved: rowIndex exact-copy for " + item.getValue(),
                    String.valueOf(source.getRowIndex()), String.valueOf(item.getStructuredData().get("rowIndex")));
            assertEquals("search_area_structured_data_preserved: cellIndexInRow exact-copy for " + item.getValue(),
                    String.valueOf(source.getCellIndexInRow()),
                    String.valueOf(item.getStructuredData().get("cellIndexInRow")));
            assertEquals("search_area_structured_data_preserved: pairIndexInRow exact-copy for " + item.getValue(),
                    String.valueOf(source.getPairIndexInRow()),
                    String.valueOf(item.getStructuredData().get("pairIndexInRow")));
        }
    }

    /** 행마다 pair 개수가 다른(row0=2, row1=1) SEARCH_AREA fixture를 실제 파이프라인으로 실행해,
     * 모든 leaf가 Integer rowIndex/cellIndexInRow/pairIndexInRow를 정확히 보존하며 row/pair
     * identity가 leaf list의 위치가 아니라 sourceComponentStructuralId로만 식별됨을 확인한다. */
    private static void testSearchAreaMultiRowVariablePairCountProductionPathPreservation() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element search = doc.createElement("Div");
        search.setAttribute("id", "searchMulti");

        // row 0 (top=0): 두 pair
        appendLabeledControlPair(doc, search, "r0lbl0", "Name", "r0ctl0", "Edit", 0, 0, 60);
        appendLabeledControlPair(doc, search, "r0lbl1", "Status", "r0ctl1", "Combo", 0, 170, 230);
        // row 1 (top=30): 한 pair
        appendLabeledControlPair(doc, search, "r1lbl0", "Kind", "r1ctl0", "Radio", 30, 0, 60);
        form.appendChild(search);

        // label/control table을 BUSINESS_TABLE이 아닌 SEARCH_AREA로 판정하려면 인접한
        // 구조적 GRID peer가 필요하다 -- buildFixtureForm()과 동일한 이유로 최소 GRID를 추가.
        form.appendChild(buildMinimalGridPeer(doc));

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("multi_row_pairs: SEARCH_AREA region found", searchArea != null);
        assertEquals("multi_row_pairs: row_count parameter", "2",
                String.valueOf(searchArea.getParameters().get("row_count")));

        List<SourcePayloadEvidenceItem> labelEvidence = new ArrayList<SourcePayloadEvidenceItem>();
        List<SourcePayloadEvidenceItem> controlEvidence = new ArrayList<SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : searchArea.getPayloadEvidence()) {
            if ("label".equals(item.getEvidenceRole())) { labelEvidence.add(item); }
            if ("control".equals(item.getEvidenceRole())) { controlEvidence.add(item); }
        }
        assertEquals("multi_row_pairs: 3 label evidence items", "3", String.valueOf(labelEvidence.size()));
        assertEquals("multi_row_pairs: 3 control evidence items", "3", String.valueOf(controlEvidence.size()));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(search));
        assertTrue("multi_row_pairs: payload present", payload != null);
        assertEquals("multi_row_pairs: 6 leaves (3 labels + 3 controls)", "6", String.valueOf(payload.getItems().size()));

        Map<String, SourcePayloadEvidenceItem> evidenceByComponent = new LinkedHashMap<String, SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : searchArea.getPayloadEvidence()) {
            evidenceByComponent.put(item.getSourceComponentStructuralId(), item);
        }
        for (TargetLeafPayload leaf : payload.getItems()) {
            Object rowObj = leaf.getStructuredData().get("rowIndex");
            Object cellObj = leaf.getStructuredData().get("cellIndexInRow");
            Object pairObj = leaf.getStructuredData().get("pairIndexInRow");
            assertTrue("multi_row_pairs: rowIndex is Integer for " + leaf.getSourceComponentStructuralId(),
                    rowObj instanceof Integer);
            assertTrue("multi_row_pairs: cellIndexInRow is Integer for " + leaf.getSourceComponentStructuralId(),
                    cellObj instanceof Integer);
            assertTrue("multi_row_pairs: pairIndexInRow is Integer for " + leaf.getSourceComponentStructuralId(),
                    pairObj instanceof Integer);

            SourcePayloadEvidenceItem source = evidenceByComponent.get(leaf.getSourceComponentStructuralId());
            assertTrue("multi_row_pairs: matching upstream evidence exists for "
                    + leaf.getSourceComponentStructuralId(), source != null);
            assertEquals("multi_row_pairs: rowIndex exact-copy for " + leaf.getSourceComponentStructuralId(),
                    String.valueOf(source.getRowIndex()), String.valueOf(rowObj));
            assertEquals("multi_row_pairs: cellIndexInRow exact-copy for " + leaf.getSourceComponentStructuralId(),
                    String.valueOf(source.getCellIndexInRow()), String.valueOf(cellObj));
            assertEquals("multi_row_pairs: pairIndexInRow exact-copy for " + leaf.getSourceComponentStructuralId(),
                    String.valueOf(source.getPairIndexInRow()), String.valueOf(pairObj));
        }

        // row/pair identity는 leaf list 순서로 유추할 수 없다 -- 리스트를 뒤집어도 크기가
        // 동일함을 확인해 위의 exact-copy 검증이 위치가 아닌 식별자 기반임을 보강한다.
        List<TargetLeafPayload> reversed = new ArrayList<TargetLeafPayload>(payload.getItems());
        Collections.reverse(reversed);
        assertEquals("multi_row_pairs: reversed list same size (order-independence sanity check)",
                String.valueOf(payload.getItems().size()), String.valueOf(reversed.size()));
    }

    private static Element buildMinimalGridPeer(Document doc) {
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "gridPeerMulti");
        Element formats = doc.createElement("Formats");
        Element format = doc.createElement("Format");
        format.setAttribute("id", "default");
        Element columns = doc.createElement("Columns");
        Element col1 = doc.createElement("Column");
        col1.setAttribute("size", "80");
        columns.appendChild(col1);
        format.appendChild(columns);
        Element headBand = doc.createElement("Band");
        headBand.setAttribute("id", "head");
        Element headCell1 = doc.createElement("Cell");
        headCell1.setAttribute("col", "0");
        headCell1.setAttribute("row", "0");
        headCell1.setAttribute("text", "Col");
        headBand.appendChild(headCell1);
        Element bodyBand = doc.createElement("Band");
        bodyBand.setAttribute("id", "body");
        Element bodyCell1 = doc.createElement("Cell");
        bodyCell1.setAttribute("col", "0");
        bodyCell1.setAttribute("row", "0");
        bodyCell1.setAttribute("text", "bind:col1");
        bodyBand.appendChild(bodyCell1);
        format.appendChild(headBand);
        format.appendChild(bodyBand);
        formats.appendChild(format);
        grid.appendChild(formats);
        return grid;
    }

    private static void appendLabeledControlPair(
            Document doc, Element parent, String labelId, String labelText, String controlId, String controlTag,
            int top, int labelLeft, int controlLeft) {
        Element label = doc.createElement("Static");
        label.setAttribute("id", labelId);
        label.setAttribute("text", labelText);
        label.setAttribute("left", String.valueOf(labelLeft));
        label.setAttribute("top", String.valueOf(top));
        label.setAttribute("width", "50");
        label.setAttribute("height", "20");
        parent.appendChild(label);

        Element control = doc.createElement(controlTag);
        control.setAttribute("id", controlId);
        control.setAttribute("left", String.valueOf(controlLeft));
        control.setAttribute("top", String.valueOf(top));
        control.setAttribute("width", "100");
        control.setAttribute("height", "20");
        parent.appendChild(control);
    }

    // ---- Plan<->Region Variant 정합성 강화 ----

    private static Element buildLabelControlDivForm(Document doc, String divId, Element[] outLabel, Element[] outControl) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div0 = doc.createElement("Div");
        div0.setAttribute("id", divId);
        Element label1 = doc.createElement("Static");
        label1.setAttribute("id", "lbl1");
        div0.appendChild(label1);
        Element edit1 = doc.createElement("Edit");
        edit1.setAttribute("id", "editItem");
        div0.appendChild(edit1);
        form.appendChild(div0);
        outLabel[0] = label1;
        outControl[0] = edit1;
        return form;
    }

    /** Plan node는 BUSINESS_TABLE.vertical(source predicate가 만든 적 없는 catalog상 variant)을
     * 가리키는데 region이 recommendedVariant="horizontal"이면 variant mismatch로 명시적
     * 실패해야 한다(payload를 조용히 0건으로 만들지 않음). */
    private static void testBusinessTableVerticalPlanHorizontalRegionExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        planRegion.setRecommendedVariant("vertical");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1); // recommendedVariant="horizontal" 값을 사용한다.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", 0));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_vertical_plan_horizontal_region", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 4 정상 대조군: Plan node와 region 둘 다 recommendedVariant="horizontal"(유일한
     * source-evidenced variant)로 일치하면 -- 새 variant integrity 검증이 정상 경로를 막지
     * 않고 payload가 그대로 생성돼야 한다. */
    private static void testBusinessTableHorizontalPlanHorizontalRegionPayloadSucceeds() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1); // recommendedVariant="horizontal" 값을 사용한다.
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1); // recommendedVariant="horizontal" 값을 사용한다.
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", null, 0,
                Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0)));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", null, 1,
                Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(0)));
        List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, x);
        assertTrue("business_table_horizontal_match: payload present", payload != null);
        assertEquals("business_table_horizontal_match: item count", "2", String.valueOf(payload.getItems().size()));
    }

    /** BUTTON_GROUP은 variant가 실제로 standalone/title_bar_attached 중 동적으로 결정되므로,
     * Plan="standalone"인데 공급된 region은 실제로 title_bar_attached인 경우를 재현해
     * 공통 variant 검증이 BUSINESS_TABLE 전용이 아님을 확인한다. */
    private static void testButtonGroupVariantMismatchExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupVariant");
        btnGroup.setAttribute("width", "100");
        Element btn = doc.createElement("Button");
        btn.setAttribute("id", "btnOnly");
        btn.setAttribute("text", "확인");
        btn.setAttribute("left", "10");
        btnGroup.appendChild(btn);
        form.appendChild(btnGroup);
        String x = structuralIdOf(btnGroup);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult actualRegion = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("button_group_variant_mismatch: region found", actualRegion != null);
        assertEquals("button_group_variant_mismatch: actual variant is standalone (no adjacent TITLE_BAR)",
                "standalone", actualRegion.getRecommendedVariant());

        // Plan node은 카탈로그상 유효한 또 다른 실제 BUTTON_GROUP variant를 주장한다.
        SemanticRegionResult planRegion = new SemanticRegionResult();
        planRegion.setSemanticType("BUTTON_GROUP");
        planRegion.setRecommendedTemplateFamily("BUTTON_GROUP");
        planRegion.setRecommendedVariant("title_bar_attached");
        planRegion.setConfidence("HIGH");
        planRegion.setSourceStructuralId(x);
        planRegion.getParameters().put("position", "left");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("button_group_variant_mismatch", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    // ---- Source-Emittable Variant 정합성 게이트 ----

    /** Plan.variant와 Region.recommendedVariant가 둘 다 "vertical"로 일치해도(exact-match는
     * 통과) 그 값 자체를 현재 source predicate가 실제로 emit한 적이 없으므로 source-emittable
     * 게이트가 명시적 실패해야 한다("일치하니까 정상"이라고 조용히 통과시키지 않는다). */
    private static void testBusinessTableVerticalVerticalUnemittableExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element[] outLabel = new Element[1];
        final Element[] outControl = new Element[1];
        final Element form = buildLabelControlDivForm(doc, "table1", outLabel, outControl);
        String x = structuralIdOf((Element) form.getElementsByTagName("Div").item(0));

        SemanticRegionResult planRegion = businessTableRegion(x, 1, 1);
        planRegion.setRecommendedVariant("vertical");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = businessTableRegion(x, 1, 1);
        actualRegion.setRecommendedVariant("vertical"); // plan과 동일 -- exact-match만으로는 통과됨
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outLabel[0]), "label", "source_text_attribute", "품목", 0));
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(outControl[0]), "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("business_table_vertical_vertical_unemittable", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    private static SemanticRegionResult buttonGroupUnemittableVariantRegion(String structuralId, String variant) {
        SemanticRegionResult r = new SemanticRegionResult();
        r.setSemanticType("BUTTON_GROUP");
        r.setRecommendedTemplateFamily("BUTTON_GROUP");
        r.setRecommendedVariant(variant);
        r.setConfidence("HIGH");
        r.setSourceStructuralId(structuralId);
        r.getParameters().put("position", "left");
        return r;
    }

    /** 항목 4 tamper: BUTTON_GROUP.embedded는 catalog엔 있지만 {@code determineButtonGroupVariant}가
     * 절대 만들지 않는다(standalone/title_bar_attached 둘 중 하나만 반환) -- Plan/Region 둘 다
     * "embedded"로 일치해도 명시적 실패해야 한다. */
    private static void testButtonGroupEmbeddedEmbeddedUnemittableExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupEmbedded");
        Element btn = doc.createElement("Button");
        btn.setAttribute("id", "btnOnly");
        btn.setAttribute("text", "확인");
        btn.setAttribute("left", "10");
        btnGroup.appendChild(btn);
        form.appendChild(btnGroup);
        String x = structuralIdOf(btnGroup);

        SemanticRegionResult planRegion = buttonGroupUnemittableVariantRegion(x, "embedded");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = buttonGroupUnemittableVariantRegion(x, "embedded");
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(btn), "button", "source_text_attribute", "확인", 0));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("button_group_embedded_embedded_unemittable", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 4 tamper: BUTTON_GROUP.fixed_footer도 동일하게 catalog엔 있지만 source predicate가
     * 절대 만들지 않는 값이다 -- 명시적 실패해야 한다. */
    private static void testButtonGroupFixedFooterFixedFooterUnemittableExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupFixedFooter");
        Element btn = doc.createElement("Button");
        btn.setAttribute("id", "btnOnly");
        btn.setAttribute("text", "확인");
        btn.setAttribute("left", "10");
        btnGroup.appendChild(btn);
        form.appendChild(btnGroup);
        String x = structuralIdOf(btnGroup);

        SemanticRegionResult planRegion = buttonGroupUnemittableVariantRegion(x, "fixed_footer");
        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(planRegion);
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult actualRegion = buttonGroupUnemittableVariantRegion(x, "fixed_footer");
        actualRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, structuralIdOf(btn), "button", "source_text_attribute", "확인", 0));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(actualRegion);

        assertThrowsIllegalState("button_group_fixed_footer_fixed_footer_unemittable", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 항목 5 정상 regression: 실제 predicate가 만드는 BUTTON_GROUP.standalone(인접 TITLE_BAR
     * 없음)이 새 source-emittable 게이트를 통과해 정상 payload를 만들어야 한다. */
    private static void testButtonGroupStandaloneVariantPayloadSucceeds() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element btnGroup = doc.createElement("Div");
        btnGroup.setAttribute("id", "btnGroupStandaloneOnly");
        btnGroup.setAttribute("width", "100");
        Element btn = doc.createElement("Button");
        btn.setAttribute("id", "btnOnly");
        btn.setAttribute("text", "확인");
        btn.setAttribute("left", "10");
        btnGroup.appendChild(btn);
        form.appendChild(btnGroup);

        Fixture fx = buildFixture(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(fx.regions, "BUTTON_GROUP");
        assertTrue("button_group_standalone_variant: region found", buttonGroup != null);
        assertEquals("button_group_standalone_variant: actual variant is standalone",
                "standalone", buttonGroup.getRecommendedVariant());

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        TargetNodePayload payload = findPayload(payloads, buttonGroup.getSourceStructuralId());
        assertTrue("button_group_standalone_variant: payload present", payload != null);
        assertEquals("button_group_standalone_variant: 1 caption item", "1", String.valueOf(payload.getItems().size()));
    }

    /** 항목 5 정상 regression: 실제 predicate가 만드는 BUTTON_GROUP.title_bar_attached(직전 형제가
     * TITLE_BAR 구조)가 새 source-emittable 게이트를 통과해 정상 payload를 만들어야 한다. */
    private static void testButtonGroupTitleBarAttachedVariantPayloadSucceeds() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc); // titleBar1 바로 뒤에 btnGroup1 -- 인접 조건 충족
        Fixture fx = buildFixture(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(fx.regions, "BUTTON_GROUP");
        assertTrue("button_group_title_bar_attached_variant: region found", buttonGroup != null);
        assertEquals("button_group_title_bar_attached_variant: actual variant is title_bar_attached",
                "title_bar_attached", buttonGroup.getRecommendedVariant());

        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);
        Element btnGroupDiv = (Element) form.getElementsByTagName("Div").item(1);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(btnGroupDiv));
        assertTrue("button_group_title_bar_attached_variant: payload present", payload != null);
        assertTrue("button_group_title_bar_attached_variant: has items", !payload.getItems().isEmpty());
    }

    // ---- Evidence 무결성 + Plan/Region 상관관계 강화 ----

    private static SemanticRegionResult searchAreaRegion(String structuralId, int columnCount, int rowCount) {
        SemanticRegionResult r = new SemanticRegionResult();
        r.setSemanticType("SEARCH_AREA");
        r.setRecommendedTemplateFamily("SEARCH_AREA");
        r.setRecommendedVariant("basic");
        r.setConfidence("HIGH");
        r.setSourceStructuralId(structuralId);
        r.getParameters().put("column_count", columnCount);
        r.getParameters().put("row_count", rowCount);
        return r;
    }

    /**
     * Plan node는 SEARCH_AREA/structuralId=X를 가리키는데, 공급된 {@code regions}에는 같은
     * structuralId에 BUSINESS_TABLE region만 존재하면 -- family가 다르다는 것 자체가
     * correlation이 깨졌다는 신호이므로 명시적 실패해야 한다(payload 0건으로 조용히 넘기지 않음).
     */
    private static void testWrongFamilySameAnchorExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div0 = doc.createElement("Div");
        form.appendChild(div0);
        String x = structuralIdOf(div0);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(searchAreaRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        SemanticRegionResult businessTableRegion = new SemanticRegionResult();
        businessTableRegion.setSemanticType("BUSINESS_TABLE");
        businessTableRegion.setRecommendedTemplateFamily("BUSINESS_TABLE");
        businessTableRegion.setSourceStructuralId(x);
        businessTableRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Static[0]", "label", "source_text_attribute", "품목", 0));
        businessTableRegion.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                x, "Form[0]/Div[0]/Edit[1]", "control", "source_tag_name", "Edit", 1));
        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(businessTableRegion);

        assertThrowsIllegalState("wrong_family", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** 같은 sourceStructuralId를 가진 SEARCH_AREA region이 2개면(서로 다른 evidence 값) --
     * silent last-wins 대신 명시적으로 실패해야 한다. */
    private static void testDuplicateRegionExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div1 = doc.createElement("Div");
        form.appendChild(div1);
        String x = structuralIdOf(div1);

        SemanticRegionResult regionA = searchAreaRegion(x, 1, 1);
        SemanticRegionResult regionB = searchAreaRegion(x, 1, 1);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(searchAreaRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(regionA);
        regions.add(regionB);

        assertThrowsIllegalState("duplicate_region", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** evidence.semanticRegionStructuralId가 자신을 소유한 region의 sourceStructuralId와
     * 다르면 -- skip이 아니라 명시적으로 실패해야 한다. */
    private static void testEvidenceOwnerMismatchExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div2 = doc.createElement("Div");
        form.appendChild(div2);
        String x = structuralIdOf(div2);
        String y = "Form[0]/Div[9]";
        // 다른 region의 id -- ownership mismatch 검사가 provenance lookup보다 먼저 일어나므로
        // 이 DOM에 실존할 필요가 없다.

        SemanticRegionResult region = searchAreaRegion(x, 1, 1);
        region.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                y, "Form[0]/Div[9]/Static[0]", "label", "source_text_attribute", "다른구역라벨", 0));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(searchAreaRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        final List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(region);

        assertThrowsIllegalState("owner_mismatch", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, regions);
            }
        });
    }

    /** SOURCE_SEMANTIC SEARCH_AREA Plan node가 있는데 regions에서 매칭되는 region이 0개면 --
     * SEARCH_AREA는 evidence-backed projection이므로 정상 GAP이 아니라 명시적 실패다. */
    private static void testMissingRegionExplicitFailure() throws Exception {
        Document doc = newDocument();
        final Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element div = doc.createElement("Div");
        form.appendChild(div);
        String x = structuralIdOf(div);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        CompositionDecision decision = evaluator.evaluate(searchAreaRegion(x, 1, 1));
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        decisions.add(decision);
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        final List<SemanticRegionResult> emptyRegions = new ArrayList<SemanticRegionResult>();

        assertThrowsIllegalState("missing_region", new Runnable() {
            public void run() {
                extractWithBindings(form, plan, emptyRegions);
            }
        });
    }

    /** sourceRoot=null이면(유효해 보이는 SEARCH_AREA region/evidence가 있어도) 즉시
     * IllegalArgumentException -- provenance validation을 건너뛰는 경로를 아예 허용하지 않는다. */
    private static void testSourceRootNullExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        final List<SemanticRegionResult> finalRegions = regions;

        try {
            extractWithBindings(null, plan, finalRegions);
            failures++;
            System.out.println("[FAIL] source_root_null: expected IllegalArgumentException but none was thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("[PASS] source_root_null: explicit failure -- " + e.getMessage());
        }
    }

    /** evidenceKind="source_text_attribute_absent"(라벨에 text/value 속성이 둘 다 없음)는
     * legitimate producer 상태이므로 shape 검증은 통과한다. 이 leaf는 값이 null이어도 묵시적으로
     * 사라지면 안 된다 -- 짝을 이루는 "control" evidence가 고아가 되기 때문이다. */
    private static void testSourceTextAttributeAbsentProducesNoDisplayText() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("absent_kind: SEARCH_AREA region found", searchArea != null);
        int originalEvidenceCount = searchArea.getPayloadEvidence().size();

        SourcePayloadEvidenceItem originalLabel = null;
        for (SourcePayloadEvidenceItem item : searchArea.getPayloadEvidence()) {
            if ("label".equals(item.getEvidenceRole())) {
                originalLabel = item;
                break;
            }
        }
        assertTrue("absent_kind: a real label evidence item exists", originalLabel != null);
        searchArea.getPayloadEvidence().remove(originalLabel);
        searchArea.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                originalLabel.getSemanticRegionStructuralId(), originalLabel.getSourceComponentStructuralId(),
                "label", "source_text_attribute_absent", null, null, originalLabel.getSourceOrder(),
                originalLabel.getRowIndex(), originalLabel.getCellIndexInRow(), originalLabel.getPairIndexInRow()));

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element search = (Element) form.getElementsByTagName("Div").item(2);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(search));
        assertTrue("absent_kind: payload still present", payload != null);
        assertEquals("absent_kind: leaf count unchanged (null-value label leaf survives, nothing dropped)",
                String.valueOf(originalEvidenceCount), String.valueOf(payload.getItems().size()));

        TargetLeafPayload nullValueLeaf = null;
        for (TargetLeafPayload leaf : payload.getItems()) {
            if (originalLabel.getSourceComponentStructuralId().equals(leaf.getSourceComponentStructuralId())) {
                nullValueLeaf = leaf;
            }
        }
        assertTrue("absent_kind: the null-value label leaf itself survives", nullValueLeaf != null);
        assertTrue("absent_kind: its value is null", nullValueLeaf.getValue() == null);
        assertEquals("absent_kind: structuredData rowIndex preserved unchanged", String.valueOf(originalLabel.getRowIndex()),
                String.valueOf(nullValueLeaf.getStructuredData().get("rowIndex")));
        assertEquals("absent_kind: structuredData cellIndexInRow preserved unchanged",
                String.valueOf(originalLabel.getCellIndexInRow()),
                String.valueOf(nullValueLeaf.getStructuredData().get("cellIndexInRow")));
        assertEquals("absent_kind: structuredData pairIndexInRow preserved unchanged",
                String.valueOf(originalLabel.getPairIndexInRow()),
                String.valueOf(nullValueLeaf.getStructuredData().get("pairIndexInRow")));
    }

    /** evidenceKind="source_text_attribute"인데 value가 없으면(모순된 evidence) -- 조용히
     * 보정하지 말고 즉시 실패해야 한다. */
    private static void testInvalidKindValueComboExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("invalid_kind_value: SEARCH_AREA region found", searchArea != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element label1 = (Element) form.getElementsByTagName("Static").item(1);
        // 모순된 evidence: "source_text_attribute"(실제 값이 존재함)를 주장하지만 value=null이다.
        searchArea.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                searchArea.getSourceStructuralId(), structuralIdOf(label1), "label",
                "source_text_attribute", null, 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("invalid_kind_value", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 실제 Segmenter가 만든 진짜 SEARCH_AREA region에, sourceRoot 안에 존재하지 않는
     * sourceComponentStructuralId를 가진 evidence를 하나 덧붙이면 -- 명시적으로 실패해야 한다. */
    private static void testNonexistentSourceComponentExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("nonexistent_component: SEARCH_AREA region found", searchArea != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        searchArea.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                searchArea.getSourceStructuralId(), "Form[0]/Div[2]/Static[99]", "label",
                "source_text_attribute", "존재하지않음", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("nonexistent_component", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 실제 SEARCH_AREA region에, sourceRoot 안에는 실존하지만 그 region의 subtree 밖에 있는
     * (예: TITLE_BAR의 Static) Element를 가리키는 evidence를 덧붙이면 -- structuralId
     * prefix 파싱이 아니라 실제 parent chain 검증으로 명시적으로 실패해야 한다. */
    private static void testEvidenceComponentOutsideRegionSubtreeExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("outside_subtree: SEARCH_AREA region found", searchArea != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        // titleText1은 DOM에 실존하지만 SEARCH_AREA container의 후손이 아니다 -- 실존하는
        // 잘못된 subtree의 Element.
        Element titleStatic = (Element) form.getElementsByTagName("Static").item(0);
        searchArea.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                searchArea.getSourceStructuralId(), structuralIdOf(titleStatic), "label",
                "source_text_attribute", "화면 제목", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("outside_subtree", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 같은 region 안에서 두 evidence item이 같은 sourceOrder를 가지면 -- 정렬해서 정상화하지
     * 말고 명시적으로 실패해야 한다. */
    private static void testDuplicateSourceOrderExplicitFailure() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("duplicate_order: SEARCH_AREA region found", searchArea != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        // 첫 번째 label이 이미 쓰는 sourceOrder=0을 다른 유효한 item에 중복 사용한다.
        Element label1 = (Element) form.getElementsByTagName("Static").item(1);
        searchArea.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                searchArea.getSourceStructuralId(), structuralIdOf(label1), "label",
                "source_text_attribute", "중복순서", 0));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("duplicate_order", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** valid data에 대해서는 regions list 순서와 evidence list 순서를 뒤집어도 동일한 payload가
     * 나와야 한다(순서는 오직 explicit sourceOrder로만 결정된다). */
    private static void testRegionsAndEvidenceOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult searchArea = findSearchArea(regions);
        assertTrue("regions_order_independence: SEARCH_AREA region found", searchArea != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        List<TargetNodePayload> payloadsForward = extractWithBindings(form, plan, regions);

        List<SemanticRegionResult> reversedRegions = new ArrayList<SemanticRegionResult>(regions);
        java.util.Collections.reverse(reversedRegions);
        // SEARCH_AREA region 내부의 evidence 저장 순서도 함께 뒤섞는다.
        java.util.Collections.reverse(searchArea.getPayloadEvidence());

        List<TargetNodePayload> payloadsReversed =
                extractWithBindings(form, plan, reversedRegions);

        TargetNodePayload forward = findPayload(payloadsForward, structuralIdOf(
                (Element) form.getElementsByTagName("Div").item(2)));
        TargetNodePayload reversed = findPayload(payloadsReversed, structuralIdOf(
                (Element) form.getElementsByTagName("Div").item(2)));
        assertTrue("regions_order_independence: forward payload present", forward != null);
        assertTrue("regions_order_independence: reversed payload present", reversed != null);
        assertEquals("regions_order_independence: same item count",
                String.valueOf(forward.getItems().size()), String.valueOf(reversed.getItems().size()));
        for (int i = 0; i < forward.getItems().size(); i++) {
            assertEquals("regions_order_independence: item[" + i + "] value matches",
                    forward.getItems().get(i).getValue(), reversed.getItems().get(i).getValue());
        }
    }

    // ==== TITLE_BAR / BUTTON_GROUP / TAB_CONTROL Predicate-Time Evidence 처리 ====

    /** TITLE_BAR predicate가 확정한 leading Static 자체가 predicate-time evidence로 정확히
     * capture됨을 확인한다(Segmenter 레벨) -- 이후 payload도 동일하게 나온다. */
    private static void testTitleBarEvidenceCapturedAtPredicateTime() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBar = findRegionByFamily(regions, "TITLE_BAR");
        assertTrue("title_bar_evidence: region found", titleBar != null);
        assertEquals("title_bar_evidence: exactly 1 item", "1", String.valueOf(titleBar.getPayloadEvidence().size()));
        SourcePayloadEvidenceItem item = titleBar.getPayloadEvidence().get(0);
        assertEquals("title_bar_evidence: role", "title_label", item.getEvidenceRole());
        assertEquals("title_bar_evidence: value", "화면 제목", item.getValue());
        assertEquals("title_bar_evidence: kind", "source_text_attribute", item.getEvidenceKind());
        Element titleStatic = (Element) form.getElementsByTagName("Static").item(0);
        assertEquals("title_bar_evidence: structuralId exact", structuralIdOf(titleStatic),
                item.getSourceComponentStructuralId());
        assertTrue("title_bar_evidence: rowIndex is null (row concept only applies to label/control)",
                item.getRowIndex() == null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        Element titleBarDiv = (Element) form.getElementsByTagName("Div").item(0);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(titleBarDiv));
        assertTrue("title_bar_evidence: payload present via evidence-only path", payload != null);
        assertEquals("title_bar_evidence: payload item count", "1", String.valueOf(payload.getItems().size()));
        assertEquals("title_bar_evidence: payload value", "화면 제목", payload.getItems().get(0).getValue());
    }

    /** TITLE_BAR region에 role="button"(BUTTON_GROUP 전용) evidence가 섞여 들어가면 -- 다른
     * family evidence가 silent ignore되지 않고 명시적으로 실패해야 한다(항목 6). */
    private static void testTitleBarWrongRoleEvidenceFails() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBar = findRegionByFamily(regions, "TITLE_BAR");
        assertTrue("title_bar_wrong_role: region found", titleBar != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element closeButton = (Element) form.getElementsByTagName("Button").item(0);
        titleBar.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                titleBar.getSourceStructuralId(), structuralIdOf(closeButton), "button",
                "source_text_attribute", "닫기", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("title_bar_wrong_role", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** TITLE_BAR evidence의 semanticRegionStructuralId가 다른 region을 가리키면 -- 명시적 실패. */
    private static void testTitleBarOwnerMismatchFails() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBar = findRegionByFamily(regions, "TITLE_BAR");
        assertTrue("title_bar_owner_mismatch: region found", titleBar != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element titleStatic = (Element) form.getElementsByTagName("Static").item(0);
        titleBar.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                "Form[0]/Div[9]", structuralIdOf(titleStatic), "title_label",
                "source_text_attribute", "다른구역", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("title_bar_owner_mismatch", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** TITLE_BAR evidence의 sourceComponentStructuralId가 존재하지 않으면 -- 명시적 실패. */
    private static void testTitleBarMissingComponentFails() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBar = findRegionByFamily(regions, "TITLE_BAR");
        assertTrue("title_bar_missing_component: region found", titleBar != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        titleBar.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                titleBar.getSourceStructuralId(), "Form[0]/Div[0]/Static[99]", "title_label",
                "source_text_attribute", "존재하지않음", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("title_bar_missing_component", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    /** 투명 Div wrapper(자식이 전부 Button, opaque boundary 없음) 안의 Button들이 predicate가
     * 확정한 flattenedButtons에 그대로 포함되어 evidence로 정확히 capture됨을 확인한다. */
    private static void testButtonGroupTransparentWrapperEvidenceAndPayload() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element container = doc.createElement("Div");
        container.setAttribute("id", "btnGroupWrap");
        container.setAttribute("width", "300");
        Element btnA = doc.createElement("Button");
        btnA.setAttribute("id", "btnA");
        btnA.setAttribute("text", "A");
        btnA.setAttribute("left", "10");
        Element wrapper = doc.createElement("Div");
        wrapper.setAttribute("id", "wrap");
        Element btnB = doc.createElement("Button");
        btnB.setAttribute("id", "btnB");
        btnB.setAttribute("text", "B");
        btnB.setAttribute("left", "60");
        Element btnC = doc.createElement("Button");
        btnC.setAttribute("id", "btnC");
        btnC.setAttribute("text", "C");
        btnC.setAttribute("left", "110");
        wrapper.appendChild(btnB);
        wrapper.appendChild(btnC);
        container.appendChild(btnA);
        container.appendChild(wrapper);
        form.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("wrapper_button_evidence: region found", buttonGroup != null);
        assertEquals("wrapper_button_evidence: 3 button evidence items (A + wrapper's B,C)",
                "3", String.valueOf(buttonGroup.getPayloadEvidence().size()));
        boolean sawA = false, sawB = false, sawC = false;
        for (SourcePayloadEvidenceItem item : buttonGroup.getPayloadEvidence()) {
            assertEquals("wrapper_button_evidence: role", "button", item.getEvidenceRole());
            if ("A".equals(item.getValue())) {
                sawA = structuralIdOf(btnA).equals(item.getSourceComponentStructuralId());
            }
            if ("B".equals(item.getValue())) {
                sawB = structuralIdOf(btnB).equals(item.getSourceComponentStructuralId());
            }
            if ("C".equals(item.getValue())) {
                sawC = structuralIdOf(btnC).equals(item.getSourceComponentStructuralId());
            }
        }
        assertTrue("wrapper_button_evidence: btnA exact identity", sawA);
        assertTrue("wrapper_button_evidence: btnB exact identity (from inside transparent wrapper)", sawB);
        assertTrue("wrapper_button_evidence: btnC exact identity (from inside transparent wrapper)", sawC);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);
        TargetNodePayload payload = findPayload(payloads, structuralIdOf(container));
        assertTrue("wrapper_button_evidence: payload present", payload != null);
        assertEquals("wrapper_button_evidence: payload item count matches evidence",
                "3", String.valueOf(payload.getItems().size()));
    }

    /** 불투명(visible 속성을 가진) wrapper가 섞이면 predicate 자체가 BUTTON_GROUP을 발행하지
     * 않는다(blind merge 금지, 기존 규칙 무변경) -- region이 없으므로 evidence도 없다. */
    private static void testButtonGroupOpaqueBoundaryNoEvidence() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element container = doc.createElement("Div");
        container.setAttribute("id", "btnGroupOpaque");
        container.setAttribute("width", "300");
        Element btnX = doc.createElement("Button");
        btnX.setAttribute("id", "btnX");
        btnX.setAttribute("text", "X");
        btnX.setAttribute("left", "10");
        Element opaqueWrapper = doc.createElement("Div");
        opaqueWrapper.setAttribute("id", "opaqueWrap");
        opaqueWrapper.setAttribute("visible", "true");
        Element btnY = doc.createElement("Button");
        btnY.setAttribute("id", "btnY");
        btnY.setAttribute("text", "Y");
        btnY.setAttribute("left", "60");
        opaqueWrapper.appendChild(btnY);
        container.appendChild(btnX);
        container.appendChild(opaqueWrapper);
        form.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        assertTrue("opaque_boundary_no_evidence: no BUTTON_GROUP emitted",
                findRegionByFamily(regions, "BUTTON_GROUP") == null);
        for (SemanticRegionResult region : regions) {
            assertTrue("opaque_boundary_no_evidence: no stray evidence anywhere in this fixture",
                    region.getPayloadEvidence().isEmpty());
        }
    }

    /** 서로 다른 두 BUTTON_GROUP container 아래 같은 bare id("sharedBtn")를 가진 Button이
     * 있어도(캡션은 다름) -- 각 region은 자기 자신의 evidence만 소유하고, payload도 섞이지
     * 않아야 한다(cross-wiring=0). */
    private static void testButtonGroupCrossWiringSameIdNestedScope() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element groupA = doc.createElement("Div");
        groupA.setAttribute("id", "groupA");
        groupA.setAttribute("width", "200");
        Element buttonA = doc.createElement("Button");
        buttonA.setAttribute("id", "sharedBtn");
        buttonA.setAttribute("text", "그룹A버튼");
        buttonA.setAttribute("left", "10");
        groupA.appendChild(buttonA);
        form.appendChild(groupA);

        Element groupB = doc.createElement("Div");
        groupB.setAttribute("id", "groupB");
        groupB.setAttribute("width", "200");
        Element buttonB = doc.createElement("Button");
        buttonB.setAttribute("id", "sharedBtn");
        buttonB.setAttribute("text", "그룹B버튼");
        buttonB.setAttribute("left", "10");
        groupB.appendChild(buttonB);
        form.appendChild(groupB);

        Fixture fx = buildFixture(form);
        List<TargetNodePayload> payloads = extractWithBindings(form, fx.plan, fx.regions);

        TargetNodePayload payloadA = findPayload(payloads, structuralIdOf(groupA));
        TargetNodePayload payloadB = findPayload(payloads, structuralIdOf(groupB));
        assertTrue("button_group_cross_wiring: groupA payload present", payloadA != null);
        assertTrue("button_group_cross_wiring: groupB payload present", payloadB != null);
        assertEquals("button_group_cross_wiring: groupA caption exact", "그룹A버튼",
                payloadA.getItems().get(0).getValue());
        assertEquals("button_group_cross_wiring: groupB caption exact", "그룹B버튼",
                payloadB.getItems().get(0).getValue());
    }

    /** BUTTON_GROUP evidence 저장 순서를 뒤집어도 sourceOrder 기준으로 payload가 동일해야 한다. */
    private static void testButtonGroupEvidenceOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult buttonGroup = findRegionByFamily(regions, "BUTTON_GROUP");
        assertTrue("button_group_order_independence: region found", buttonGroup != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        Element btnGroupDiv = (Element) form.getElementsByTagName("Div").item(1);

        List<TargetNodePayload> forwardPayloads = extractWithBindings(form, plan, regions);
        TargetNodePayload forward = findPayload(forwardPayloads, structuralIdOf(btnGroupDiv));

        java.util.Collections.reverse(buttonGroup.getPayloadEvidence());
        List<TargetNodePayload> reversedPayloads = extractWithBindings(form, plan, regions);
        TargetNodePayload reversed = findPayload(reversedPayloads, structuralIdOf(btnGroupDiv));

        assertTrue("button_group_order_independence: forward present", forward != null);
        assertTrue("button_group_order_independence: reversed present", reversed != null);
        assertEquals("button_group_order_independence: same item count",
                String.valueOf(forward.getItems().size()), String.valueOf(reversed.getItems().size()));
        for (int i = 0; i < forward.getItems().size(); i++) {
            assertEquals("button_group_order_independence: item[" + i + "] value matches",
                    forward.getItems().get(i).getValue(), reversed.getItems().get(i).getValue());
        }
    }

    /** TAB_CONTROL predicate가 확정한 직계 Tabpage 목록이 evidence로 정확히 capture됨을
     * 확인한다(text/titletext fallback 포함). */
    private static void testTabControlEvidenceCapturedAtPredicateTime() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult tabControl = findRegionByFamily(regions, "TAB_CONTROL");
        assertTrue("tab_control_evidence: region found", tabControl != null);
        assertEquals("tab_control_evidence: 2 items", "2", String.valueOf(tabControl.getPayloadEvidence().size()));

        Element page1 = (Element) form.getElementsByTagName("Tabpage").item(0);
        Element page2 = (Element) form.getElementsByTagName("Tabpage").item(1);
        boolean sawPage1 = false, sawPage2 = false;
        for (SourcePayloadEvidenceItem item : tabControl.getPayloadEvidence()) {
            assertEquals("tab_control_evidence: role", "tab_label", item.getEvidenceRole());
            if ("첫번째 탭".equals(item.getValue())) {
                assertEquals("tab_control_evidence: page1 kind", "source_text_attribute", item.getEvidenceKind());
                sawPage1 = structuralIdOf(page1).equals(item.getSourceComponentStructuralId());
            }
            if ("두번째 탭".equals(item.getValue())) {
                assertEquals("tab_control_evidence: page2 kind (titletext fallback)",
                        "source_titletext_attribute", item.getEvidenceKind());
                sawPage2 = structuralIdOf(page2).equals(item.getSourceComponentStructuralId());
            }
        }
        assertTrue("tab_control_evidence: page1 exact identity", sawPage1);
        assertTrue("tab_control_evidence: page2 exact identity", sawPage2);
    }

    /** TAB_CONTROL region에 role="title_label"(TITLE_BAR 전용) evidence가 섞여 들어가면 --
     * 명시적으로 실패해야 한다(항목 6). */
    private static void testTabControlWrongRoleEvidenceFails() throws Exception {
        Document doc = newDocument();
        Element form = buildFixtureForm(doc);
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult tabControl = findRegionByFamily(regions, "TAB_CONTROL");
        assertTrue("tab_control_wrong_role: region found", tabControl != null);

        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        final TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);

        Element page1 = (Element) form.getElementsByTagName("Tabpage").item(0);
        tabControl.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                tabControl.getSourceStructuralId(), structuralIdOf(page1), "title_label",
                "source_text_attribute", "첫번째 탭", 99));

        final Element finalForm = form;
        final List<SemanticRegionResult> finalRegions = regions;
        assertThrowsIllegalState("tab_control_wrong_role", new Runnable() {
            public void run() {
                extractWithBindings(finalForm, plan, finalRegions);
            }
        });
    }

    private static SemanticRegionResult findSearchArea(List<SemanticRegionResult> regions) {
        return findRegionByFamily(regions, "SEARCH_AREA");
    }

    private static SemanticRegionResult findRegionByFamily(List<SemanticRegionResult> regions, String family) {
        for (SemanticRegionResult region : regions) {
            if (family.equals(region.getSemanticType())) {
                return region;
            }
        }
        return null;
    }

    private static void assertThrowsIllegalState(String label, Runnable action) {
        try {
            action.run();
            failures++;
            System.out.println("[FAIL] " + label + ": expected IllegalStateException but none was thrown");
        } catch (IllegalStateException e) {
            System.out.println("[PASS] " + label + ": explicit failure -- " + e.getMessage());
        }
    }

    // ==== assertion 헬퍼 ====

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " expected=<" + expected + "> actual=<" + actual + ">");
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
