package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.binding.SourceBindingAnalyzer;
import com.example.xfdltracker.binding.SourceBindingReference;
import com.example.xfdltracker.payload.TargetEventBinding;
import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetOptionItem;
import com.example.xfdltracker.payload.TargetPayloadCategory;
import com.example.xfdltracker.payload.TargetPayloadExtractor;
import com.example.xfdltracker.renderer.AtomicWebSquareRenderer;
import com.example.xfdltracker.renderer.RenderStatus;
import com.example.xfdltracker.renderer.AtomicRenderResult;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;

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
 * {@link AtomicWebSquareRenderer}가 실제 {@code TargetCompositionPlan}/{@code TargetNodePayload}
 * 만으로 각 family/variant를 deterministic하게 렌더링하는지 검증하는 오프라인 unit test.
 * renderer 자신은 이 패키지의 어떤 타입도 직접 참조하지 않는다({@code RendererArchitectureIsolationTest} 참고).
 */
public class AtomicWebSquareRendererTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTitleBarWithTitleLabelRendered();
        testTitleBarWithExplicitEmptyEnvelopeRendersBareDfbox();
        testTitleBarDeterministicAcrossPayloadOrder();
        testMultipleRootsIndependentRendering();
        testNonTitleBarFamilyUnsupported();
        testWrongVariantUnsupported();
        testDuplicatePayloadForSameNodeRejected();
        testDuplicateTitleLabelCardinalityRejected();
        testWrongLeafCategoryRejected();
        testMissingPayloadEnvelopeForSupportedNodeRejected();
        testTitleBarIdentityKindMismatchRejected();
        testOrphanPayloadRejectedGlobally();
        testCrossWiredPayloadToOtherRealNodeCausesMissingAndDuplicate();

        testBusinessTableOneByOneRendered();
        testBusinessTableNByMRendered();
        testBusinessTableControlTypeValueNeverProjected();
        testBusinessTablePayloadOrderPermutationDeterministic();
        testBusinessTableMultipleRootsWithTitleBarDeterministic();
        testBusinessTableWrongVariantUnsupported();
        testBusinessTableMissingEnvelopeRejected();
        testBusinessTableIdentityKindMismatchRejected();
        testBusinessTableDuplicateEnvelopeRejected();
        testBusinessTableEmptyEnvelopeRejected();
        testBusinessTableRowCountMissingRejected();
        testBusinessTableRowCountZeroRejected();
        testBusinessTableRowCountWrongTypeRejected();
        testBusinessTableColumnPairCountMissingRejected();
        testBusinessTableRowCountMismatchRejected();
        testBusinessTableSparsePairIndexRejected();
        testBusinessTableDuplicateDisplayTextRejected();
        testBusinessTableDuplicateControlTypeRejected();
        testBusinessTableMissingControlTypeRejected();
        testBusinessTableCellIndexMismatchRejected();
        testBusinessTableMalformedStructuredDataRejected();
        testBusinessTableUnexpectedLeafCategoryRejected();
        testBusinessTableOrphanCrossWireStillEnforced();

        // ==== SPLIT_LAYOUT 렌더링 ====
        testSplitLayoutRatioSplitRendersBareLybox();
        testSplitLayoutZeroPayloadRejected();
        testSplitLayoutDuplicatePayloadRejected();
        testSplitLayoutIdentityKindMismatchRejected();
        testSplitLayoutIdentityValueMismatchRejected();
        testSplitLayoutCrossWiredChildPayloadFailsClosed();
        testSplitLayoutOrphanPayloadRejectedGlobally();
        testSplitLayoutFixedFlexUnsupportedVariant();
        testSplitLayoutShuttleUnsupportedVariant();
        testSplitLayoutTamperedVariantUnsupported();
        testSplitLayoutUnexpectedLeafCategoryRejected();
        testSplitLayoutSyntheticLookingValueDoesNotInferKind();

        // ==== GRID.basic 구조 셸(structural shell) ====
        testGridBasicTwoColumnRendered();
        testGridDualHeaderUnsupportedVariant();
        testGridZeroPayloadRejected();
        testGridColumnCountMissingRejected();
        testGridColumnCountMismatchRejected();
        testGridDuplicateColRejected();
        testGridMergedCellUnsupportedRejected();
        testGridDuplicatePayloadForNodeRejected();
        testGridIdentityKindMismatchRejected();
        testGridIdentityValueMismatchOrphanRejected();
        testProductionPayloadProducerNeverCreatesNullIdentityKind();

        // ==== TAB_CONTROL.basic 원자적(atomic) 렌더링 ====
        testTabControlOnePageRendered();
        testTabControlMultiplePagesPreserveLabelOrder();
        testTabControlExactTabsAndContentCount();
        testTabControlPageContentAttachmentsKeySetExact();
        testTabControlPageContentAttachmentsAreDescendantsOfRoot();
        testTabControlPageContentAttachmentsAreExactContentElements();
        testTabControlPageContentAttachmentsUnmodifiable();
        testTabControlNoDuplicateElementAcrossOrdinals();
        testTabControlNonTabControlResultAttachmentMapEmpty();
        testTabControlUnsupportedVariantAttachmentMapEmpty();
        testTabControlNoExternalSrcEmitted();
        testTabControlMissingLabelFailsClosed();
        testTabControlWrongVariantUnsupported();
        testTabControlTabCountMissingParameterRejected();
        testTabControlDuplicatePageOrdinalRejected();
        testTabControlPageOrdinalCountMismatchRejected();

        // ==== TAB_CONTROL tab-label pageOrdinal 페이로드 투영
        // (Reviewer가 승인한 TargetLeafPayload.structuredData["pageOrdinal"]) ====
        testTabControlSegmenterToExtractorExactPageOrdinalPropagation();
        testTabControlMissingLabelSegmenterToExtractorEvidencePreserved();
        testTabControlNonIntegerPageOrdinalRejected();
        testTabControlNegativePageOrdinalRejected();
        testTabControlOutOfRangePageOrdinalRejected();
        testTabControlMissingPageOrdinalFieldRejected();
        testTabControlBlankLabelRejected();

        // ==== SEARCH_AREA.basic 원자적(atomic) 렌더링 ====
        testSearchAreaSingleRowSinglePair();
        testSearchAreaMultipleRowsVariablePairCounts();
        testSearchAreaRowsOrderedByAscendingRowIndexEvenIfShuffled();
        testSearchAreaPairsOrderedByAscendingPairIndexEvenIfShuffled();
        testSearchAreaLabelBeforeControlWithinEveryPair();
        testSearchAreaEditMapsToXfInput();
        testSearchAreaComboMapsToXfSelect1Minimal();
        testSearchAreaCalendarMapsToW2InputCalendar();
        testSearchAreaCheckBoxFailsClosedBeforeRenderer();
        testSearchAreaRadioMapsToXfSelect1Full();
        testSearchAreaAllFourNonCheckBoxControlMappingsAcrossMultipleRows();
        testSearchAreaRootIsXfGroup();
        testSearchAreaEachRowIsDirectXfGroupChild();
        testSearchAreaNoShbox();
        testSearchAreaNoShboxInner();
        testSearchAreaNoBusinessTableSpecificStructure();
        testSearchAreaMissingRowIndexRejected();
        testSearchAreaNonIntegerRowIndexRejected();
        testSearchAreaNegativeRowIndexRejected();
        testSearchAreaMissingCellIndexInRowRejected();
        testSearchAreaNonIntegerCellIndexInRowRejected();
        testSearchAreaNegativeCellIndexInRowRejected();
        testSearchAreaMissingPairIndexInRowRejected();
        testSearchAreaNonIntegerPairIndexInRowRejected();
        testSearchAreaNegativePairIndexInRowRejected();
        testSearchAreaDuplicateLabelForOnePairRejected();
        testSearchAreaDuplicateControlForOnePairRejected();
        testSearchAreaMissingLabelRejected();
        testSearchAreaMissingControlRejected();
        testSearchAreaUnsupportedControlTypeRejected();

        // ==== SEARCH_AREA Combo/Radio option 정적 렌더링(Slice 102D) ====
        testSearchAreaComboWithOptionsRendersStaticChoices();
        testSearchAreaRadioWithOptionsRendersStaticChoices();
        testSearchAreaOptionItemsOrderedByAscendingRowOrdinalEvenIfShuffled();
        testSearchAreaPlainComboAndRadioRenderNoStaticChoices();
        testSearchAreaOptionItemsDuplicateRowOrdinalRejected();
        testSearchAreaOptionItemsNonDenseRowOrdinalRejected();
        testSearchAreaOptionItemsWrongElementTypeRejected();

        // ==== BUTTON_GROUP 원자적(atomic) 렌더링 ====
        testButtonGroupOneEventlessButtonRenders();
        testButtonGroupMultipleEventlessButtonsPreserveOrdinalOrder();
        testButtonGroupStandaloneVariantRenders();
        testButtonGroupTitleBarAttachedVariantRendersSameStructure();
        testButtonGroupRootIsXfGroup();
        testButtonGroupEveryButtonIsXfTrigger();
        testButtonGroupEveryTriggerHasTypeButton();
        testButtonGroupEveryTriggerHasOneXfLabel();
        testButtonGroupNoTextNoValueButtonRejected();
        testButtonGroupExpectedCountMissingRejected();
        testButtonGroupExpectedCountMismatchRejected();
        testButtonGroupMissingOrdinalRejected();
        testButtonGroupDuplicateOrdinalRejected();
        testButtonGroupNegativeOrdinalRejected();
        testButtonGroupOutOfRangeOrdinalRejected();
        testButtonGroupValidFinalizedOnclickAttachesToExactButton();
        testButtonGroupTwoButtonsDistinctHandlersDoNotCrossAttach();
        testButtonGroupEventlessAndEventfulCoexist();
        testButtonGroupEventLeafWithoutFinalizedBindingRejected();
        testButtonGroupFinalizedEventToNonexistentOrdinalRejected();
        testButtonGroupDuplicateFinalizedOnclickForSameButtonRejected();
        testButtonGroupUnsupportedFinalizedTargetEventLocalNameRejected();
        testButtonGroupRendererDoesNotReadRawFunctionName();
        testButtonGroupRendererDoesNotUseSourceStructuralIdForCorrelation();
        testButtonGroupRawEventEvidenceWithoutFinalizedBindingCannotRenderSuccessfully();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 정상 TITLE_BAR.title_only + title_label 1개 -> deterministic dfbox>f1>df_tit fragment. */
    private static void testTitleBarWithTitleLabelRendered() throws Exception {
        Document doc = newDocument();
        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", "bar1");
        titleBar.setAttribute("left", "0");
        Element label = doc.createElement("Static");
        label.setAttribute("id", "lbl1");
        label.setAttribute("left", "0");
        label.setAttribute("text", "Section Title");
        titleBar.appendChild(label);
        doc.appendChild(titleBar);

        RealPipelineResult built = runRealPipeline(titleBar, "TITLE_BAR");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(built.plan, built.payloads);

        assertEquals("with-title: result count", "1", String.valueOf(results.size()));
        AtomicRenderResult result = results.get(0);
        assertEquals("with-title: status", "RENDERED", String.valueOf(result.getStatus()));
        Element dfbox = result.getTargetElement();
        assertEquals("with-title: root class", "dfbox", dfbox.getAttribute("class"));
        assertEquals("with-title: root children count", "1", String.valueOf(dfbox.getChildNodes().getLength()));
        Element f1 = (Element) dfbox.getFirstChild();
        assertEquals("with-title: f1 class", "f1", f1.getAttribute("class"));
        Element dfTit = (Element) f1.getFirstChild();
        assertEquals("with-title: df_tit class", "df_tit", dfTit.getAttribute("class"));
        assertEquals("with-title: df_tit text", "Section Title", dfTit.getTextContent());
    }

    /** title Static에 text가 없어도 TargetPayloadExtractor는 explicit empty envelope을 만든다
     * ("envelope 부재"와 "leaf 0개" 구분). renderer는 이를 title_label 0 cardinality의 정상
     * 상태로 취급해 빈 dfbox만 만들고 실패하지 않아야 한다. */
    private static void testTitleBarWithExplicitEmptyEnvelopeRendersBareDfbox() throws Exception {
        Document doc = newDocument();
        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", "bar2");
        titleBar.setAttribute("left", "0");
        Element label = doc.createElement("Static");
        label.setAttribute("id", "lbl2");
        label.setAttribute("left", "0");
        titleBar.appendChild(label);
        doc.appendChild(titleBar);

        RealPipelineResult built = runRealPipeline(titleBar, "TITLE_BAR");
        assertEquals("explicit-empty-envelope: exactly one envelope produced for this node (precondition)",
                "1", String.valueOf(built.payloads.size()));
        assertEquals("explicit-empty-envelope: envelope has zero items (precondition)",
                "0", String.valueOf(built.payloads.get(0).getItems().size()));

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(built.plan, built.payloads);
        assertEquals("explicit-empty-envelope: result count", "1", String.valueOf(results.size()));
        AtomicRenderResult result = results.get(0);
        assertEquals("explicit-empty-envelope: status", "RENDERED", String.valueOf(result.getStatus()));
        Element dfbox = result.getTargetElement();
        assertEquals("explicit-empty-envelope: root class", "dfbox", dfbox.getAttribute("class"));
        assertEquals("explicit-empty-envelope: no children (leading_extra/right_actions omitted)",
                "0", String.valueOf(dfbox.getChildNodes().getLength()));
    }

    /** payload list의 순서(encounter order)가 결과에 영향을 주지 않는지 확인(2개 TITLE_BAR
     * 각각의 payload를 반대 순서로 넘겨도 각 nodeId의 render 결과는 동일). */
    private static void testTitleBarDeterministicAcrossPayloadOrder() throws Exception {
        TargetCompositionNode nodeA = titleBarNode("nodeA", "title_only");
        TargetCompositionNode nodeB = titleBarNode("nodeB", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(nodeA, nodeB), Collections.<TargetCompositionEdge>emptyList());

        TargetNodePayload payloadA = titleLabelPayload("nodeA", "Alpha");
        TargetNodePayload payloadB = titleLabelPayload("nodeB", "Beta");

        List<AtomicRenderResult> forward = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payloadA, payloadB));
        List<AtomicRenderResult> reversed = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payloadB, payloadA));

        assertEquals("order-independence: nodeA text (forward)",
                "Alpha", ((Element) forward.get(0).getTargetElement().getFirstChild().getFirstChild()).getTextContent());
        assertEquals("order-independence: nodeA text (reversed payload order)",
                "Alpha", ((Element) reversed.get(0).getTargetElement().getFirstChild().getFirstChild()).getTextContent());
        assertEquals("order-independence: nodeB text (forward)",
                "Beta", ((Element) forward.get(1).getTargetElement().getFirstChild().getFirstChild()).getTextContent());
        assertEquals("order-independence: nodeB text (reversed payload order)",
                "Beta", ((Element) reversed.get(1).getTargetElement().getFirstChild().getFirstChild()).getTextContent());
    }

    /** 여러 root(TITLE_BAR 2개 + 관련 없는 TREEVIEW 1개, renderer 미지원 family)가 섞여 있어도
     * 각자 독립적으로 처리된다(multiple roots는 정상). family-agnostic unsupported 케이스는
     * 미지원인 TREEVIEW로 표현한다. */
    private static void testMultipleRootsIndependentRendering() throws Exception {
        TargetCompositionNode titleA = titleBarNode("titleA", "title_only");
        TargetCompositionNode titleB = titleBarNode("titleB", "title_only");
        TargetCompositionNode unsupported = new TargetCompositionNode(
                "treeX", "TREEVIEW", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "treeX",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "treeX"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(titleA, unsupported, titleB), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(titleLabelPayload("titleA", "A"), titleLabelPayload("titleB", "B")));

        assertEquals("multi-root: result count", "3", String.valueOf(results.size()));
        assertEquals("multi-root: titleA status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        assertEquals("multi-root: unsupported family status", "UNSUPPORTED_FAMILY",
                String.valueOf(results.get(1).getStatus()));
        assertEquals("multi-root: titleB status", "RENDERED", String.valueOf(results.get(2).getStatus()));
    }

    /** family가 renderer 지원 범위 밖(TREEVIEW)이면 임의로 렌더링하지 않고 명시적
     * UNSUPPORTED_FAMILY를 반환한다. */
    private static void testNonTitleBarFamilyUnsupported() throws Exception {
        TargetCompositionNode unsupported = new TargetCompositionNode(
                "tree1", "TREEVIEW", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "tree1",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "tree1"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(unsupported), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, Collections.<TargetNodePayload>emptyList());
        assertEquals("wrong-family: status", "UNSUPPORTED_FAMILY", String.valueOf(results.get(0).getStatus()));
        assertTrue("wrong-family: reason mentions family",
                results.get(0).getFailureReason().contains("TREEVIEW"));
    }

    /** family=TITLE_BAR이지만 variant가 title_only가 아니면(tamper) UNSUPPORTED_VARIANT. */
    private static void testWrongVariantUnsupported() throws Exception {
        TargetCompositionNode tampered = titleBarNode("tamperedVariant", "bogus_variant");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(tampered), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, Collections.<TargetNodePayload>emptyList());
        assertEquals("wrong-variant: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
        assertTrue("wrong-variant: reason mentions bogus_variant",
                results.get(0).getFailureReason().contains("bogus_variant"));
    }

    /** 같은 nodeId에 대해 TargetNodePayload가 2개 존재하면(중복) 어느 하나를 조용히 고르지
     * 않고 명시적으로 거부한다. */
    private static void testDuplicatePayloadForSameNodeRejected() throws Exception {
        TargetCompositionNode node = titleBarNode("dup1", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(titleLabelPayload("dup1", "First"), titleLabelPayload("dup1", "Second")));
        assertEquals("duplicate-payload: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("duplicate-payload: reason mentions duplicate",
                results.get(0).getFailureReason().contains("duplicate_payload_for_node"));
    }

    /** 같은 node의 payload 안에 DISPLAY_TEXT leaf가 2개 이상이면(title_label 0..1 cardinality
     * 위반) 명시적으로 거부한다. */
    private static void testDuplicateTitleLabelCardinalityRejected() throws Exception {
        TargetCompositionNode node = titleBarNode("card1", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(TargetPayloadCategory.DISPLAY_TEXT, "One", null, "test_fixture", "card1"));
        items.add(new TargetLeafPayload(TargetPayloadCategory.DISPLAY_TEXT, "Two", null, "test_fixture", "card1"));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "card1", items);

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payload));
        assertEquals("cardinality: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("cardinality: reason mentions cardinality_exceeded",
                results.get(0).getFailureReason().contains("title_label_cardinality_exceeded"));
    }

    /** TITLE_BAR node의 payload에 DISPLAY_TEXT가 아닌 category(예: EVENT)가 섞여 들어오면
     * (tamper) 명시적으로 거부한다. */
    private static void testWrongLeafCategoryRejected() throws Exception {
        TargetCompositionNode node = titleBarNode("cat1", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        Map<String, Object> eventData = new LinkedHashMap<String, Object>();
        eventData.put("eventName", "onclick");
        eventData.put("functionName", "doSomething");
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(TargetPayloadCategory.EVENT, "onclick", eventData, "test_fixture", "cat1"));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "cat1", items);

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payload));
        assertEquals("wrong-category: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("wrong-category: reason mentions unexpected_leaf_category",
                results.get(0).getFailureReason().contains("unexpected_leaf_category"));
    }

    /** 지원되는 TITLE_BAR node에 대응하는 TargetNodePayload envelope이 아예 없으면(0개),
     * "title_label 0개"인 정상 상태와 다르게 취급해 명시적으로 거부한다. */
    private static void testMissingPayloadEnvelopeForSupportedNodeRejected() throws Exception {
        TargetCompositionNode node = titleBarNode("noenv1", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, Collections.<TargetNodePayload>emptyList());
        assertEquals("missing-envelope: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("missing-envelope: reason mentions missing_payload_envelope_for_node",
                results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
    }

    /** TITLE_BAR도 exact identity tuple correlation을 적용받는다 -- 같은 value, 다른 kind인
     * envelope은 map lookup에는 성공하지만 kind 검사에서 명시적으로 거부된다. */
    private static void testTitleBarIdentityKindMismatchRejected() throws Exception {
        TargetCompositionNode node = titleBarNode("tbKindMismatch", "title_only");
        TargetNodePayload wrongKindPayload = new TargetNodePayload(
                TargetNodeIdentityKind.TARGET_SYNTHETIC, "tbKindMismatch", new ArrayList<TargetLeafPayload>());
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(wrongKindPayload));
        assertEquals("tb-kind-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("tb-kind-mismatch: reason mentions identity_kind_mismatch",
                results.get(0).getFailureReason().contains("identity_kind_mismatch"));
    }

    /** Plan에 존재하지 않는 nodeId를 가리키는 orphan payload는 조용히 무시되지 않고 render()
     * 자체가 fail-closed(예외)해야 한다 -- 다른 node에 정상 envelope이 있어도 마찬가지다. */
    private static void testOrphanPayloadRejectedGlobally() throws Exception {
        TargetCompositionNode node = titleBarNode("real1", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());

        List<TargetNodePayload> payloads = java.util.Arrays.asList(
                titleLabelPayload("real1", "Real"),
                titleLabelPayload("ghostNodeThatDoesNotExist", "Ghost"));

        boolean threw = false;
        String message = null;
        try {
            new AtomicWebSquareRenderer().render(plan, payloads);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("orphan-payload: render() throws IllegalStateException", threw);
        assertTrue("orphan-payload: message mentions the orphan nodeId",
                message != null && message.contains("ghostNodeThatDoesNotExist"));
    }

    /** node A의 payload identity가 node B의 nodeId로 cross-wire되면, A는 envelope을 잃어
     * (missing_payload_envelope) 실패하고, B는 정상+hijack된 envelope 2개로(duplicate)
     * 실패한다 -- 어느 쪽도 조용히 넘어가지 않는다. */
    private static void testCrossWiredPayloadToOtherRealNodeCausesMissingAndDuplicate() throws Exception {
        TargetCompositionNode nodeA = titleBarNode("nodeA_cw", "title_only");
        TargetCompositionNode nodeB = titleBarNode("nodeB_cw", "title_only");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(nodeA, nodeB), Collections.<TargetCompositionEdge>emptyList());

        TargetNodePayload payloadBOwn = titleLabelPayload("nodeB_cw", "Beta");
        TargetNodePayload payloadAHijackedToB = titleLabelPayload("nodeB_cw", "Alpha-hijacked");

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payloadBOwn, payloadAHijackedToB));

        assertEquals("cross-wire: nodeA status (lost its own envelope)",
                "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("cross-wire: nodeA reason mentions missing_payload_envelope_for_node",
                results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
        assertEquals("cross-wire: nodeB status (now has 2 envelopes)",
                "INTEGRITY_VIOLATION", String.valueOf(results.get(1).getStatus()));
        assertTrue("cross-wire: nodeB reason mentions duplicate_payload_for_node",
                results.get(1).getFailureReason().contains("duplicate_payload_for_node"));
    }

    // ==== BUSINESS_TABLE.horizontal 구조 셸(structural shell) ====

    /** 1 row x 1 pair -- 최소 크기에서 exact hierarchy(root/table/colgroup(2 col)/tr/th/td),
     * th text, th w2:scope=row, td 빈 shell을 확인한다. */
    private static void testBusinessTableOneByOneRendered() throws Exception {
        TargetCompositionNode node = businessTableNode("bt1x1", "horizontal", 1, 1);
        TargetCompositionPlan plan = singleNodePlan(node);
        TargetNodePayload payload = validBusinessTablePayload("bt1x1", 1, 1, "Edit");

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payload));
        assertEquals("1x1: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        Element tbbox = results.get(0).getTargetElement();
        assertEquals("1x1: root class", "tbbox", tbbox.getAttribute("class"));
        assertEquals("1x1: root children count", "1", String.valueOf(tbbox.getChildNodes().getLength()));

        Element table = (Element) tbbox.getFirstChild();
        assertEquals("1x1: table class", "w2tb tb", table.getAttribute("class"));
        assertEquals("1x1: table tagname", "table", table.getAttribute("tagname"));
        assertEquals("1x1: table children count (attributes+colgroup+1 tr)", "3",
                String.valueOf(table.getChildNodes().getLength()));

        Element attributes = (Element) table.getChildNodes().item(0);
        assertEquals("1x1: attributes tag", "w2:attributes", attributes.getTagName());
        Element summary = (Element) attributes.getFirstChild();
        assertEquals("1x1: summary tag", "w2:summary", summary.getTagName());
        assertEquals("1x1: summary is empty", "", summary.getTextContent());

        Element colgroup = (Element) table.getChildNodes().item(1);
        assertEquals("1x1: colgroup tagname", "colgroup", colgroup.getAttribute("tagname"));
        assertEquals("1x1: col count == pair_count*2", "2", String.valueOf(colgroup.getChildNodes().getLength()));
        for (int i = 0; i < colgroup.getChildNodes().getLength(); i++) {
            assertEquals("1x1: col[" + i + "] tagname",
                    "col", ((Element) colgroup.getChildNodes().item(i)).getAttribute("tagname"));
        }

        Element tr = (Element) table.getChildNodes().item(2);
        assertEquals("1x1: tr tagname", "tr", tr.getAttribute("tagname"));
        assertEquals("1x1: tr children count (th+td)", "2", String.valueOf(tr.getChildNodes().getLength()));
        Element th = (Element) tr.getChildNodes().item(0);
        assertEquals("1x1: th class", "w2tb_th", th.getAttribute("class"));
        assertEquals("1x1: th tagname", "th", th.getAttribute("tagname"));
        assertEquals("1x1: th w2:scope", "row", th.getAttribute("w2:scope"));
        assertEquals("1x1: th text", "R0P0", th.getTextContent());
        Element td = (Element) tr.getChildNodes().item(1);
        assertEquals("1x1: td class", "w2tb_td", td.getAttribute("class"));
        assertEquals("1x1: td tagname", "td", td.getAttribute("tagname"));
        assertEquals("1x1: td is empty shell (no children)", "0", String.valueOf(td.getChildNodes().getLength()));
    }

    /** N rows x M pairs(2x3) -- row/th/td 개수와 col count == M*2가 정확한지 확인. */
    private static void testBusinessTableNByMRendered() throws Exception {
        int rowCount = 2;
        int pairCount = 3;
        TargetCompositionNode node = businessTableNode("btNxM", "horizontal", rowCount, pairCount);
        TargetCompositionPlan plan = singleNodePlan(node);
        TargetNodePayload payload = validBusinessTablePayload("btNxM", rowCount, pairCount, "Combo");

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payload));
        assertEquals("NxM: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        Element tbbox = results.get(0).getTargetElement();
        Element table = (Element) tbbox.getFirstChild();
        Element colgroup = (Element) table.getChildNodes().item(1);
        assertEquals("NxM: col count == pair_count*2", String.valueOf(pairCount * 2),
                String.valueOf(colgroup.getChildNodes().getLength()));
        assertEquals("NxM: tr count == row_count", String.valueOf(rowCount + 2 /* attributes 행 + colgroup 행 */),
                String.valueOf(table.getChildNodes().getLength()));
        for (int r = 0; r < rowCount; r++) {
            Element tr = (Element) table.getChildNodes().item(2 + r);
            assertEquals("NxM: tr[" + r + "] children count", String.valueOf(pairCount * 2),
                    String.valueOf(tr.getChildNodes().getLength()));
            for (int p = 0; p < pairCount; p++) {
                Element th = (Element) tr.getChildNodes().item(p * 2);
                assertEquals("NxM: th[" + r + "][" + p + "] text", "R" + r + "P" + p, th.getTextContent());
                assertEquals("NxM: th[" + r + "][" + p + "] w2:scope", "row", th.getAttribute("w2:scope"));
                Element td = (Element) tr.getChildNodes().item(p * 2 + 1);
                assertEquals("NxM: td[" + r + "][" + p + "] empty",
                        "0", String.valueOf(td.getChildNodes().getLength()));
            }
        }
    }

    /** CONTROL_TYPE.value(source tag name)만 다르고 나머지가 동일한 두 fixture(Edit vs Combo)의
     * target fragment가 완전히 동일해야 한다 -- source tag name이 target structure 결정에
     * 관여하지 않음을 확인. */
    private static void testBusinessTableControlTypeValueNeverProjected() throws Exception {
        TargetCompositionNode nodeEdit = businessTableNode("btEdit", "horizontal", 1, 2);
        TargetCompositionNode nodeCombo = businessTableNode("btCombo", "horizontal", 1, 2);
        TargetNodePayload payloadEdit = validBusinessTablePayload("btEdit", 1, 2, "Edit");
        TargetNodePayload payloadCombo = validBusinessTablePayload("btCombo", 1, 2, "Combo");

        Element fragmentEdit = new AtomicWebSquareRenderer().render(
                singleNodePlan(nodeEdit), java.util.Arrays.asList(payloadEdit)).get(0).getTargetElement();
        Element fragmentCombo = new AtomicWebSquareRenderer().render(
                singleNodePlan(nodeCombo), java.util.Arrays.asList(payloadCombo)).get(0).getTargetElement();

        assertEquals("control-type-not-projected: fragment XML identical regardless of source tag value",
                elementToDebugString(fragmentEdit), elementToDebugString(fragmentCombo));
    }

    /** payload list의 encounter order가 결과에 영향을 주지 않는지 확인(같은 leaf 집합을
     * 반대 순서로 넘겨도 동일 fragment). */
    private static void testBusinessTablePayloadOrderPermutationDeterministic() throws Exception {
        TargetCompositionNode node = businessTableNode("btOrder", "horizontal", 2, 2);
        List<TargetLeafPayload> items = validBusinessTableItems(2, 2, "Edit");
        List<TargetLeafPayload> reversedItems = new ArrayList<TargetLeafPayload>(items);
        Collections.reverse(reversedItems);

        TargetNodePayload forwardPayload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btOrder", items);
        TargetNodePayload reversedPayload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btOrder", reversedItems);

        Element forwardFragment = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(forwardPayload)).get(0).getTargetElement();
        Element reversedFragment = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(reversedPayload)).get(0).getTargetElement();

        assertEquals("order-independence(BUSINESS_TABLE): identical fragment regardless of leaf encounter order",
                elementToDebugString(forwardFragment), elementToDebugString(reversedFragment));
    }

    /** TITLE_BAR + BUSINESS_TABLE이 섞인 multiple roots에서도 각자 독립적으로 deterministic하게
     * 처리된다. */
    private static void testBusinessTableMultipleRootsWithTitleBarDeterministic() throws Exception {
        TargetCompositionNode title = titleBarNode("titleRoot", "title_only");
        TargetCompositionNode table = businessTableNode("tableRoot", "horizontal", 1, 1);
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(title, table), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(
                        titleLabelPayload("titleRoot", "Header"),
                        validBusinessTablePayload("tableRoot", 1, 1, "Edit")));

        assertEquals("multi-root(mixed): result count", "2", String.valueOf(results.size()));
        assertEquals("multi-root(mixed): titleRoot status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        assertEquals("multi-root(mixed): tableRoot status", "RENDERED", String.valueOf(results.get(1).getStatus()));
        assertEquals("multi-root(mixed): titleRoot still dfbox",
                "dfbox", results.get(0).getTargetElement().getAttribute("class"));
        assertEquals("multi-root(mixed): tableRoot still tbbox",
                "tbbox", results.get(1).getTargetElement().getAttribute("class"));
    }

    /** family=BUSINESS_TABLE이지만 variant가 horizontal이 아니면(예: vertical, 아직 미지원)
     * 결과는 UNSUPPORTED_VARIANT가 되어야 한다. */
    private static void testBusinessTableWrongVariantUnsupported() throws Exception {
        TargetCompositionNode node = businessTableNode("btVertical", "vertical", 1, 1);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("bt-wrong-variant: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-wrong-variant: reason mentions vertical",
                results.get(0).getFailureReason().contains("vertical"));
    }

    private static void testBusinessTableMissingEnvelopeRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btNoEnv", "horizontal", 1, 1);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("bt-missing-envelope: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-missing-envelope: reason", results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
    }

    /** BUSINESS_TABLE도 exact identity tuple correlation을 적용받는다 -- rendering behavior는
     * 바뀌지 않고 identity kind가 다른 envelope만 명시적으로 거부한다. */
    private static void testBusinessTableIdentityKindMismatchRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btKindMismatch", "horizontal", 1, 1);
        TargetNodePayload wrongKindPayload = new TargetNodePayload(
                TargetNodeIdentityKind.TARGET_SYNTHETIC, "btKindMismatch",
                validBusinessTableItems(1, 1, "Edit"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(wrongKindPayload));
        assertEquals("bt-kind-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-kind-mismatch: reason mentions identity_kind_mismatch",
                results.get(0).getFailureReason().contains("identity_kind_mismatch"));
    }

    private static void testBusinessTableDuplicateEnvelopeRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btDupEnv", "horizontal", 1, 1);
        TargetNodePayload payload1 = validBusinessTablePayload("btDupEnv", 1, 1, "Edit");
        TargetNodePayload payload2 = validBusinessTablePayload("btDupEnv", 1, 1, "Edit");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload1, payload2));
        assertEquals("bt-dup-envelope: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-dup-envelope: reason", results.get(0).getFailureReason().contains("duplicate_payload_for_node"));
    }

    /** envelope은 존재하지만 items가 비어 있으면 -- BUSINESS_TABLE의 rows(1..N)/cells(1..N per
     * row) cardinality는 TITLE_BAR의 title_label(0..1)과 달리 최소 1을 요구하므로 실패해야
     * 한다(TITLE_BAR의 "explicit empty envelope = 정상"과 대비). */
    private static void testBusinessTableEmptyEnvelopeRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btEmptyEnv", "horizontal", 1, 1);
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btEmptyEnv", new ArrayList<TargetLeafPayload>());
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-empty-envelope: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
    }

    private static void testBusinessTableRowCountMissingRejected() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("column_pair_count", Integer.valueOf(1));
        TargetCompositionNode node = businessTableNodeWithParams("btNoRowCount", "horizontal", params);
        TargetNodePayload payload = validBusinessTablePayload("btNoRowCount", 1, 1, "Edit");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-row-count-missing: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-row-count-missing: reason", results.get(0).getFailureReason().contains("missing_parameter:row_count"));
    }

    private static void testBusinessTableRowCountZeroRejected() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("row_count", Integer.valueOf(0));
        params.put("column_pair_count", Integer.valueOf(1));
        TargetCompositionNode node = businessTableNodeWithParams("btZeroRowCount", "horizontal", params);
        TargetNodePayload payload = validBusinessTablePayload("btZeroRowCount", 1, 1, "Edit");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-row-count-zero: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-row-count-zero: reason", results.get(0).getFailureReason().contains("invalid_parameter_value:row_count"));
    }

    private static void testBusinessTableRowCountWrongTypeRejected() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("row_count", "1");
        params.put("column_pair_count", Integer.valueOf(1));
        TargetCompositionNode node = businessTableNodeWithParams("btWrongTypeRowCount", "horizontal", params);
        TargetNodePayload payload = validBusinessTablePayload("btWrongTypeRowCount", 1, 1, "Edit");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-row-count-wrong-type: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-row-count-wrong-type: reason", results.get(0).getFailureReason().contains("invalid_parameter_type:row_count"));
    }

    private static void testBusinessTableColumnPairCountMissingRejected() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("row_count", Integer.valueOf(1));
        TargetCompositionNode node = businessTableNodeWithParams("btNoPairCount", "horizontal", params);
        TargetNodePayload payload = validBusinessTablePayload("btNoPairCount", 1, 1, "Edit");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-pair-count-missing: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-pair-count-missing: reason", results.get(0).getFailureReason().contains("missing_parameter:column_pair_count"));
    }

    /** Plan parameter(row_count=2)와 실제 payload 구조(row 0만 존재)가 불일치하면 실패한다. */
    private static void testBusinessTableRowCountMismatchRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btRowMismatch", "horizontal", 2, 1);
        TargetNodePayload payload = validBusinessTablePayload("btRowMismatch", 1, 1, "Edit"); // row 0만 존재한다.
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-row-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-row-mismatch: reason", results.get(0).getFailureReason().contains("row_count_mismatch"));
    }

    /** row 안에서 pairIndexInRow가 sparse하면(row0가 pair0만 갖고 pair1이 없음, column_pair_count=2)
     * 실패한다. */
    private static void testBusinessTableSparsePairIndexRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btSparsePair", "horizontal", 1, 2);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "R0P0", 0, 0, 0));
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Edit", 0, 1, 0));
        // pair 1 완전히 누락(column_pair_count=2인데 pair 0만 존재).
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btSparsePair", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-sparse-pair: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-sparse-pair: reason mentions missing leaf for pair 1",
                results.get(0).getFailureReason().contains("pair=1"));
    }

    private static void testBusinessTableDuplicateDisplayTextRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btDupLabel", "horizontal", 1, 1);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "First", 0, 0, 0));
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "Second", 0, 0, 0));
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Edit", 0, 1, 0));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btDupLabel", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-dup-label: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-dup-label: reason", results.get(0).getFailureReason().contains("duplicate_display_text"));
    }

    private static void testBusinessTableDuplicateControlTypeRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btDupControl", "horizontal", 1, 1);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "Label", 0, 0, 0));
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Edit", 0, 1, 0));
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Combo", 0, 1, 0));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btDupControl", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-dup-control: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-dup-control: reason", results.get(0).getFailureReason().contains("duplicate_control_type"));
    }

    /** pair의 label만 있고 control이 없으면(각 pair는 label 1 + control 1 정확히 필요) 실패한다. */
    private static void testBusinessTableMissingControlTypeRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btNoControl", "horizontal", 1, 1);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "Label", 0, 0, 0));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btNoControl", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-no-control: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-no-control: reason", results.get(0).getFailureReason().contains("missing_control_type"));
    }

    /** producer invariant(label의 cellIndexInRow == pairIndexInRow*2)를 위반하는 tamper된
     * structuredData는 즉시 거부한다. */
    private static void testBusinessTableCellIndexMismatchRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btCellMismatch", "horizontal", 1, 1);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "Label", 0, 5, 0)); // 정상값은 0이어야 한다.
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Edit", 0, 1, 0));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btCellMismatch", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-cell-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-cell-mismatch: reason", results.get(0).getFailureReason().contains("cell_index_mismatch"));
    }

    /** structuredData가 null/negative/wrong-type이면(model상 가능한 tamper) 즉시 거부한다. */
    private static void testBusinessTableMalformedStructuredDataRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btMalformedSd", "horizontal", 1, 1);
        Map<String, Object> badStructuredData = new LinkedHashMap<String, Object>();
        badStructuredData.put("rowIndex", Integer.valueOf(-1));
        badStructuredData.put("cellIndexInRow", Integer.valueOf(0));
        badStructuredData.put("pairIndexInRow", Integer.valueOf(0));
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Label", badStructuredData, "test_fixture", "x"));
        items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, "Edit", 0, 1, 0));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btMalformedSd", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-malformed-sd: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-malformed-sd: reason", results.get(0).getFailureReason().contains("malformed_structured_data"));
    }

    /** BUSINESS_TABLE node의 payload에 DISPLAY_TEXT/CONTROL_TYPE이 아닌 category(EVENT)가
     * 섞여 들어오면 거부한다. */
    private static void testBusinessTableUnexpectedLeafCategoryRejected() throws Exception {
        TargetCompositionNode node = businessTableNode("btEventLeaf", "horizontal", 1, 1);
        Map<String, Object> eventData = new LinkedHashMap<String, Object>();
        eventData.put("eventName", "onclick");
        eventData.put("functionName", "doSomething");
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(TargetPayloadCategory.EVENT, "onclick", eventData, "test_fixture", "x"));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "btEventLeaf", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("bt-event-leaf: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("bt-event-leaf: reason", results.get(0).getFailureReason().contains("unexpected_leaf_category"));
    }

    /** 기존 global orphan/cross-wire fail-closed gate가 BUSINESS_TABLE에도 동일하게 적용됨을
     * 확인한다(family-agnostic 계약이므로 새 로직이 필요 없다). */
    private static void testBusinessTableOrphanCrossWireStillEnforced() throws Exception {
        TargetCompositionNode node = businessTableNode("btOrphanCheck", "horizontal", 1, 1);
        TargetNodePayload ownPayload = validBusinessTablePayload("btOrphanCheck", 1, 1, "Edit");
        TargetNodePayload orphanPayload = validBusinessTablePayload("ghostBt", 1, 1, "Edit");

        boolean threw = false;
        try {
            new AtomicWebSquareRenderer().render(
                    singleNodePlan(node), java.util.Arrays.asList(ownPayload, orphanPayload));
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("bt-orphan: message mentions ghost nodeId", e.getMessage().contains("ghostBt"));
        }
        assertTrue("bt-orphan: render() throws IllegalStateException for orphan payload", threw);
    }

    // ==== SPLIT_LAYOUT 검증 ====

    /** SOURCE_STRUCTURAL identity kind로 empty-semantic envelope을 만든다(SPLIT_LAYOUT node의
     * 정상 identity kind). */
    private static TargetNodePayload emptySplitPayload(String identityValue) {
        return new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, identityValue, new ArrayList<TargetLeafPayload>());
    }

    /** SPLIT node + 정확히 하나의 empty-semantic Payload -> 렌더링 성공. catalog의
     * structural_classes(lybox, ly_column)만 렌더링하며, columns의 실제 자식은 이 atomic
     * renderer의 책임이 아니므로(별도 CompositionRenderer가 조립) lybox는 항상 빈 shell이다. */
    private static void testSplitLayoutRatioSplitRendersBareLybox() throws Exception {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("column_ratio", new String[] {"col_3", "col_7"});
        TargetCompositionNode node = new TargetCompositionNode(
                "split1", "SPLIT_LAYOUT", "ratio_split", "HIGH", params, null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split1",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split1"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(emptySplitPayload("split1")));
        assertEquals("split-ratio: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        Element lybox = results.get(0).getTargetElement();
        assertEquals("split-ratio: root class", "lybox", lybox.getAttribute("class"));
        assertEquals("split-ratio: no children (columns assembled by CompositionRenderer)",
                "0", String.valueOf(lybox.getChildNodes().getLength()));
    }

    /** SPLIT node + zero Payload -> INTEGRITY_VIOLATION. semantic value가 empty인 것과 Payload
     * 자체가 없는 것은 다른 사실이므로, envelope 부재는 SPLIT_LAYOUT에도 명시적으로 거부된다. */
    private static void testSplitLayoutZeroPayloadRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split2", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split2",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split2"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("split-zero-payload: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-zero-payload: reason mentions missing_payload_envelope_for_node",
                results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
    }

    /** 검증 C -- SPLIT node + 중복된 Payload -> INTEGRITY_VIOLATION이어야 한다. */
    private static void testSplitLayoutDuplicatePayloadRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split2dup", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split2dup",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split2dup"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(
                        emptySplitPayload("split2dup"), emptySplitPayload("split2dup")));
        assertEquals("split-dup-payload: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-dup-payload: reason mentions duplicate_payload_for_node",
                results.get(0).getFailureReason().contains("duplicate_payload_for_node"));
    }

    /** identity value는 같고 identity kind만 다른 Payload -> correlation 금지, fail-closed.
     * value가 우연히 일치해 맵 lookup에 성공해도 kind가 다르면 명시적으로 거부한다 --
     * 진짜 tuple correlation 실패다. */
    private static void testSplitLayoutIdentityKindMismatchRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split2kind", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split2kind",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split2kind"));
        // value(identityValue)는 node.getNodeId()와 정확히 동일하다 -- 오직 kind만 틀렸다.
        TargetNodePayload wrongKindPayload = new TargetNodePayload(
                TargetNodeIdentityKind.TARGET_SYNTHETIC, "split2kind", new ArrayList<TargetLeafPayload>());
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(wrongKindPayload));
        assertEquals("split-kind-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-kind-mismatch: reason mentions identity_kind_mismatch",
                results.get(0).getFailureReason().contains("identity_kind_mismatch"));
    }

    /** Test E -- Plan/Payload identity value mismatch -> INTEGRITY_VIOLATION(같은 kind, 다른
     * value -- 이 node의 envelope이 없으므로 항목 Test B와 동일 경로). */
    private static void testSplitLayoutIdentityValueMismatchRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split2valA", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split2valA",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split2valA"));
        TargetNodePayload wrongValuePayload = emptySplitPayload("split2valB"); // 실제 노드가 아닌 orphan 아래에 있다.
        boolean threw = false;
        try {
            new AtomicWebSquareRenderer().render(
                    singleNodePlan(node), java.util.Arrays.asList(wrongValuePayload));
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("split-value-mismatch: message mentions the mismatched value",
                    e.getMessage().contains("split2valB"));
        }
        assertTrue("split-value-mismatch: render() throws (orphan payload, no node owns this value)", threw);
    }

    /** cross-wired child Payload -> fail-closed. 두 real SPLIT_LAYOUT node 중 하나가 다른 쪽의
     * identity로 hijack되면, hijack당한 쪽은 missing, hijack한 쪽은 duplicate로 실패한다. */
    private static void testSplitLayoutCrossWiredChildPayloadFailsClosed() throws Exception {
        TargetCompositionNode nodeA = new TargetCompositionNode(
                "splitCwA", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "splitCwA",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "splitCwA"));
        TargetCompositionNode nodeB = new TargetCompositionNode(
                "splitCwB", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "splitCwB",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "splitCwB"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(nodeA, nodeB), Collections.<TargetCompositionEdge>emptyList());

        TargetNodePayload payloadBOwn = emptySplitPayload("splitCwB");
        TargetNodePayload payloadAHijackedToB = emptySplitPayload("splitCwB");

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, java.util.Arrays.asList(payloadBOwn, payloadAHijackedToB));
        assertEquals("split-cross-wire: nodeA status (lost its own envelope)",
                "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-cross-wire: nodeA reason mentions missing_payload_envelope_for_node",
                results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
        assertEquals("split-cross-wire: nodeB status (now has 2 envelopes)",
                "INTEGRITY_VIOLATION", String.valueOf(results.get(1).getStatus()));
        assertTrue("split-cross-wire: nodeB reason mentions duplicate_payload_for_node",
                results.get(1).getFailureReason().contains("duplicate_payload_for_node"));
    }

    /** Test G -- orphan Payload(Plan에 없는 nodeId) -> render() 전체가 fail-closed(기존 global
     * orphan 계약이 SPLIT_LAYOUT에도 동일하게 적용됨을 재확인). */
    private static void testSplitLayoutOrphanPayloadRejectedGlobally() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "splitOrphanCheck", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "splitOrphanCheck",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "splitOrphanCheck"));
        boolean threw = false;
        try {
            new AtomicWebSquareRenderer().render(
                    singleNodePlan(node), java.util.Arrays.asList(
                            emptySplitPayload("splitOrphanCheck"), emptySplitPayload("ghostSplitNode")));
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("split-orphan: message mentions ghost nodeId", e.getMessage().contains("ghostSplitNode"));
        }
        assertTrue("split-orphan: render() throws IllegalStateException for orphan payload", threw);
    }

    /** {@code fixed_flex}는 catalog상 CONFIRMED이지만 현재 source predicate가 실제로 emit하는
     * variant가 아니므로(코드 감사 확인) 명시적으로 UNSUPPORTED_VARIANT다. */
    private static void testSplitLayoutFixedFlexUnsupportedVariant() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split3", "SPLIT_LAYOUT", "fixed_flex", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split3",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split3"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("split-fixed-flex: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-fixed-flex: reason mentions fixed_flex",
                results.get(0).getFailureReason().contains("fixed_flex"));
    }

    /** {@code shuttle}도 fixed_flex와 동일한 근거로 미지원(되돌림). */
    private static void testSplitLayoutShuttleUnsupportedVariant() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split4", "SPLIT_LAYOUT", "shuttle", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split4",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split4"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("split-shuttle: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-shuttle: reason mentions shuttle", results.get(0).getFailureReason().contains("shuttle"));
    }

    private static void testSplitLayoutTamperedVariantUnsupported() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split5", "SPLIT_LAYOUT", "bogus_variant", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split5",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split5"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("split-tampered: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
    }

    /** unexpected leaf category가 섞이면(tamper) SPLIT_LAYOUT도 명시적으로 거부한다. */
    private static void testSplitLayoutUnexpectedLeafCategoryRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "split6", "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "split6",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split6"));
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(TargetPayloadCategory.DISPLAY_TEXT, "unexpected", null, "test_fixture", "split6"));
        TargetNodePayload payload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, "split6", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("split-unexpected-leaf: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("split-unexpected-leaf: reason", results.get(0).getFailureReason().contains("unexpected_leaf_category"));
    }

    /** identity value가 synthetic-looking 문자열({@code "target_synthetic:"} prefix)을 가져도,
     * kind는 오직 명시적 {@code identityKind} 필드로만 결정된다 -- 문자열 형태로 kind를
     * 추론하지 않고 정상 RENDERED됨을 확인한다. */
    private static void testSplitLayoutSyntheticLookingValueDoesNotInferKind() throws Exception {
        String syntheticLookingValue = "target_synthetic:split7";
        TargetCompositionNode node = new TargetCompositionNode(
                syntheticLookingValue, "SPLIT_LAYOUT", "ratio_split", "HIGH", new LinkedHashMap<String, Object>(),
                null, CompositionDecision.Origin.SOURCE_SEMANTIC, syntheticLookingValue,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, syntheticLookingValue));
        assertEquals("synthetic-looking-value: node identity kind is derived from origin, not string shape",
                "SOURCE_STRUCTURAL", String.valueOf(node.getIdentityKind()));
        TargetNodePayload payload = emptySplitPayload(syntheticLookingValue);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("synthetic-looking-value: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
    }

    // ==== GRID.basic 구조 뼈대 ====

    private static TargetCompositionNode gridNode(String nodeId, int columnCount) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("column_count", Integer.valueOf(columnCount));
        return new TargetCompositionNode(
                nodeId, "GRID", "basic", "HIGH", params, null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static TargetLeafPayload gridColumnLeaf(
            String band, int col, int row, int colSpan, int rowSpan, String text) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("band", band);
        data.put("col", Integer.valueOf(col));
        data.put("row", Integer.valueOf(row));
        data.put("colSpan", Integer.valueOf(colSpan));
        data.put("rowSpan", Integer.valueOf(rowSpan));
        return new TargetLeafPayload(TargetPayloadCategory.GRID_COLUMN, text, data, "grid_format_parser", null);
    }

    private static List<TargetLeafPayload> validGridItems(int columnCount) {
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        for (int c = 0; c < columnCount; c++) {
            items.add(gridColumnLeaf("head", c, 0, 1, 1, "Col" + c));
            items.add(gridColumnLeaf("body", c, 0, 1, 1, null));
        }
        return items;
    }

    /** Test J -- GRID exactly-one explicit tuple Payload -> 기존 GRID behavior 정상(구조/검증
     * 로직 완전히 동일, GRID semantic rendering 확장 없음). */
    private static void testGridBasicTwoColumnRendered() throws Exception {
        TargetCompositionNode node = gridNode("grid2col", 2);
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "grid2col", validGridItems(2));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("grid-2col: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        Element gvwbox = results.get(0).getTargetElement();
        assertEquals("grid-2col: root class", "gvwbox", gvwbox.getAttribute("class"));
        Element gridView = (Element) gvwbox.getFirstChild();
        assertEquals("grid-2col: gridView tag", "w2:gridView", gridView.getTagName());
        assertEquals("grid-2col: gridView class", "wq_gvw", gridView.getAttribute("class"));
        Element header = (Element) gridView.getChildNodes().item(0);
        assertEquals("grid-2col: header tag", "w2:header", header.getTagName());
        Element headerRow = (Element) header.getFirstChild();
        assertEquals("grid-2col: header row column count", "2",
                String.valueOf(headerRow.getChildNodes().getLength()));
        Element gBody = (Element) gridView.getChildNodes().item(1);
        assertEquals("grid-2col: gBody tag", "w2:gBody", gBody.getTagName());
        Element bodyRow = (Element) gBody.getFirstChild();
        assertEquals("grid-2col: body row column count", "2",
                String.valueOf(bodyRow.getChildNodes().getLength()));
    }

    private static void testGridDualHeaderUnsupportedVariant() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "gridDual", "GRID", "dual_header", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "gridDual",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridDual"));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("grid-dual-header: status", "UNSUPPORTED_VARIANT", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-dual-header: reason mentions dual_header",
                results.get(0).getFailureReason().contains("dual_header"));
    }

    /** GRID zero Payload -> fail-closed. envelope 부재 자체를 정확히
     * {@code missing_payload_envelope_for_node}로 명시적으로 거부한다(items 0개와는 다른 사실). */
    private static void testGridZeroPayloadRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridNoEnv", 2);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), Collections.<TargetNodePayload>emptyList());
        assertEquals("grid-zero-payload: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-zero-payload: reason mentions missing_payload_envelope_for_node",
                results.get(0).getFailureReason().contains("missing_payload_envelope_for_node"));
    }

    private static void testGridColumnCountMissingRejected() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "gridNoCount", "GRID", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "gridNoCount",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridNoCount"));
        TargetNodePayload emptyEnvelope = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridNoCount", new ArrayList<TargetLeafPayload>());
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(emptyEnvelope));
        assertEquals("grid-no-count: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-no-count: reason", results.get(0).getFailureReason().contains("missing_parameter:column_count"));
    }

    private static void testGridColumnCountMismatchRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridMismatch", 3);
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridMismatch", validGridItems(2)); // 2개만 있으나 3개가 필요하다.
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("grid-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-mismatch: reason", results.get(0).getFailureReason().contains("grid_column_count_mismatch"));
    }

    private static void testGridDuplicateColRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridDupCol", 2);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(gridColumnLeaf("head", 0, 0, 1, 1, "A"));
        items.add(gridColumnLeaf("head", 0, 0, 1, 1, "B")); // col=0이 중복된다.
        items.add(gridColumnLeaf("body", 0, 0, 1, 1, null));
        items.add(gridColumnLeaf("body", 1, 0, 1, 1, null));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridDupCol", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("grid-dup-col: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-dup-col: reason", results.get(0).getFailureReason().contains("duplicate_grid_col"));
    }

    private static void testGridMergedCellUnsupportedRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridMerged", 2);
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(gridColumnLeaf("head", 0, 0, 2, 1, "Merged")); // colSpan=2로 설정한다.
        items.add(gridColumnLeaf("body", 0, 0, 1, 1, null));
        items.add(gridColumnLeaf("body", 1, 0, 1, 1, null));
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridMerged", items);
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload));
        assertEquals("grid-merged: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-merged: reason", results.get(0).getFailureReason().contains("unsupported_grid_span"));
    }

    /** Test L -- GRID 동일 exact tuple Payload 2개 이상 -> fail-closed. */
    private static void testGridDuplicatePayloadForNodeRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridDupEnv", 1);
        TargetNodePayload payload1 = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridDupEnv", validGridItems(1));
        TargetNodePayload payload2 = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridDupEnv", validGridItems(1));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(payload1, payload2));
        assertEquals("grid-dup-env: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-dup-env: reason", results.get(0).getFailureReason().contains("duplicate_payload_for_node"));
    }

    /** Test M -- GRID identity kind mismatch(같은 value, 다른 kind) -> successful correlation
     * 금지, fail-closed. */
    private static void testGridIdentityKindMismatchRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridKindMismatch", 1);
        TargetNodePayload wrongKindPayload = new TargetNodePayload(
                TargetNodeIdentityKind.TARGET_SYNTHETIC, "gridKindMismatch", validGridItems(1));
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                singleNodePlan(node), java.util.Arrays.asList(wrongKindPayload));
        assertEquals("grid-kind-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("grid-kind-mismatch: reason mentions identity_kind_mismatch",
                results.get(0).getFailureReason().contains("identity_kind_mismatch"));
    }

    /** Test N -- GRID identity value mismatch(orphan) -> fail-closed(전역 orphan 검사로
     * render() 자체가 예외). */
    private static void testGridIdentityValueMismatchOrphanRejected() throws Exception {
        TargetCompositionNode node = gridNode("gridValueMismatchA", 1);
        TargetNodePayload wrongValuePayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, "gridValueMismatchB", validGridItems(1));
        boolean threw = false;
        try {
            new AtomicWebSquareRenderer().render(
                    singleNodePlan(node), java.util.Arrays.asList(wrongValuePayload));
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue("grid-value-mismatch: message mentions the mismatched value",
                    e.getMessage().contains("gridValueMismatchB"));
        }
        assertTrue("grid-value-mismatch: render() throws (orphan payload, no node owns this value)", threw);
    }

    /** production Payload producer(TargetPayloadExtractor)가 supported rendered node에 null
     * identityKind를 생성하지 않음을 real pipeline으로 검증한다 -- 생성자 자신이 이미
     * identityKind==null을 거부하므로, 예외 없이 성공한다는 사실 자체가 증거다. */
    private static void testProductionPayloadProducerNeverCreatesNullIdentityKind() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "prodGrid1");
        grid.setAttribute("left", "0");
        doc.appendChild(grid);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(grid);
        SemanticRegionResult gridRegion = null;
        for (SemanticRegionResult r : regions) {
            if ("GRID".equals(r.getSemanticType())) {
                gridRegion = r;
            }
        }
        assertTrue("prod-payload: GRID region found (precondition)", gridRegion != null);

        CompositionDecision decision = new CompositionEvaluator().evaluate(gridRegion);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(decision));
        // TargetNodePayload 생성자가 identityKind==null을 거부하므로, 아래 extract() 호출이
        // 예외 없이 성공한다는 것 자체가 "production 경로가 null identityKind를 만들지 않는다"
        // 는 것을 실증한다.
        List<TargetNodePayload> payloads = extractWithBindings(grid, plan, regions);
        assertEquals("prod-payload: exactly one GRID envelope produced", "1", String.valueOf(payloads.size()));
        assertTrue("prod-payload: identityKind is explicitly non-null (constructor already enforces this)",
                payloads.get(0).getIdentityKind() != null);
        assertEquals("prod-payload: identityKind matches LOGICAL_TARGET_NODE_IDENTITY_CONSTRUCTION authority "
                        + "(node.getOrigin() derived, SOURCE_SEMANTIC -> SOURCE_STRUCTURAL)",
                "SOURCE_STRUCTURAL", String.valueOf(payloads.get(0).getIdentityKind()));
    }

    // ---- fixture 헬퍼 ----

    private static TargetCompositionNode titleBarNode(String nodeId, String variant) {
        return new TargetCompositionNode(
                nodeId, "TITLE_BAR", variant, "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static TargetNodePayload titleLabelPayload(String nodeId, String text) {
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        items.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, text, null, "source_text_attribute", nodeId));
        return new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId, items);
    }

    private static TargetCompositionPlan singleNodePlan(TargetCompositionNode node) {
        return new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
    }

    private static TargetCompositionNode businessTableNode(
            String nodeId, String variant, int rowCount, int columnPairCount) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("row_count", Integer.valueOf(rowCount));
        params.put("column_pair_count", Integer.valueOf(columnPairCount));
        return businessTableNodeWithParams(nodeId, variant, params);
    }

    private static TargetCompositionNode businessTableNodeWithParams(
            String nodeId, String variant, Map<String, Object> params) {
        return new TargetCompositionNode(
                nodeId, "BUSINESS_TABLE", variant, "HIGH", params, null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static TargetLeafPayload businessTableLeaf(
            TargetPayloadCategory category, String value, int rowIndex, int cellIndexInRow, int pairIndexInRow) {
        Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
        structuredData.put("rowIndex", Integer.valueOf(rowIndex));
        structuredData.put("cellIndexInRow", Integer.valueOf(cellIndexInRow));
        structuredData.put("pairIndexInRow", Integer.valueOf(pairIndexInRow));
        return new TargetLeafPayload(category, value, structuredData, "test_fixture", "x");
    }

    /** 완전한 rowCount x columnPairCount grid의 label+control leaf 전체를 만든다. control tag는
     * {@link AtomicWebSquareRenderer}가 target rendering에 절대 쓰지 않는 값이다. */
    private static List<TargetLeafPayload> validBusinessTableItems(
            int rowCount, int columnPairCount, String controlTag) {
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        for (int r = 0; r < rowCount; r++) {
            for (int p = 0; p < columnPairCount; p++) {
                items.add(businessTableLeaf(TargetPayloadCategory.DISPLAY_TEXT, "R" + r + "P" + p, r, p * 2, p));
                items.add(businessTableLeaf(TargetPayloadCategory.CONTROL_TYPE, controlTag, r, p * 2 + 1, p));
            }
        }
        return items;
    }

    private static TargetNodePayload validBusinessTablePayload(
            String nodeId, int rowCount, int columnPairCount, String controlTag) {
        return new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId, validBusinessTableItems(rowCount, columnPairCount, controlTag));
    }

    /** XML 직렬화 대신 순수 DOM walk로 tag/class/attribute/text만 재귀적으로 비교 가능한
     * 문자열로 만든다 -- 두 fragment의 완전한 구조적 동일성 확인용. */
    private static String elementToDebugString(Element e) {
        StringBuilder sb = new StringBuilder();
        appendElementDebug(e, sb);
        return sb.toString();
    }

    private static void appendElementDebug(Element e, StringBuilder sb) {
        sb.append('<').append(e.getTagName());
        org.w3c.dom.NamedNodeMap attrs = e.getAttributes();
        java.util.List<String> attrNames = new ArrayList<String>();
        for (int i = 0; i < attrs.getLength(); i++) {
            attrNames.add(attrs.item(i).getNodeName());
        }
        Collections.sort(attrNames);
        for (String name : attrNames) {
            sb.append(' ').append(name).append('=').append(e.getAttribute(name));
        }
        sb.append('>');
        org.w3c.dom.NodeList children = e.getChildNodes();
        boolean hasElementChild = false;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                hasElementChild = true;
                appendElementDebug((Element) children.item(i), sb);
            }
        }
        if (!hasElementChild) {
            sb.append("text=").append(e.getTextContent());
        }
        sb.append("</").append(e.getTagName()).append('>');
    }

    // ==== TAB_CONTROL.basic atomic 렌더링 ====

    private static final String NS_W2_TEST = "http://www.inswave.com/websquare";

    private static Element buildTabControlFixture(Document doc, String[] pageLabelsOrNull) {
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", "tabRoot");
        for (int i = 0; i < pageLabelsOrNull.length; i++) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", "p" + i);
            if (pageLabelsOrNull[i] != null) {
                page.setAttribute("text", pageLabelsOrNull[i]);
            }
            tab.appendChild(page);
        }
        doc.appendChild(tab);
        return tab;
    }

    private static void testTabControlOnePageRendered() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"Only Page"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(built.plan, built.payloads);

        assertEquals("tab-one-page: result count", "1", String.valueOf(results.size()));
        AtomicRenderResult result = results.get(0);
        assertEquals("tab-one-page: status", "RENDERED", String.valueOf(result.getStatus()));
        Element root = result.getTargetElement();
        assertEquals("tab-one-page: root localName", "tabControl", root.getLocalName());
        assertEquals("tab-one-page: root namespace", NS_W2_TEST, root.getNamespaceURI());
    }

    private static void testTabControlMultiplePagesPreserveLabelOrder() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"Alpha", "Beta", "Gamma"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(built.plan, built.payloads);
        AtomicRenderResult result = results.get(0);
        assertEquals("tab-multi-page: status", "RENDERED", String.valueOf(result.getStatus()));

        Element root = result.getTargetElement();
        List<String> observedLabels = new ArrayList<String>();
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("tabs".equals(child.getLocalName())) {
                observedLabels.add(child.getAttribute("label"));
            }
        }
        assertEquals("tab-multi-page: exact label order", "Alpha,Beta,Gamma", String.join(",", observedLabels));
    }

    private static void testTabControlExactTabsAndContentCount() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", "C", "D"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        Element root = result.getTargetElement();
        int tabsCount = 0;
        int contentCount = 0;
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("tabs".equals(child.getLocalName())) tabsCount++;
            if ("content".equals(child.getLocalName())) contentCount++;
        }
        assertEquals("tab-exact-counts: w2:tabs count", "4", String.valueOf(tabsCount));
        assertEquals("tab-exact-counts: w2:content count", "4", String.valueOf(contentCount));
    }

    private static void testTabControlPageContentAttachmentsKeySetExact() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", "C"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        Map<Integer, Element> attachments = result.getPageContentAttachments();
        assertEquals("tab-attachment-keys: size", "3", String.valueOf(attachments.size()));
        for (int i = 0; i < 3; i++) {
            assertTrue("tab-attachment-keys: key " + i + " present", attachments.containsKey(Integer.valueOf(i)));
        }
    }

    private static void testTabControlPageContentAttachmentsAreDescendantsOfRoot() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        Element root = result.getTargetElement();
        for (Element content : result.getPageContentAttachments().values()) {
            boolean isDescendant = false;
            org.w3c.dom.Node current = content.getParentNode();
            while (current != null) {
                if (current == root) { isDescendant = true; break; }
                current = current.getParentNode();
            }
            assertTrue("tab-attachment-descendants: content is under root", isDescendant);
        }
    }

    private static void testTabControlPageContentAttachmentsAreExactContentElements() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        for (Element content : result.getPageContentAttachments().values()) {
            assertEquals("tab-attachment-is-content: localName", "content", content.getLocalName());
            assertEquals("tab-attachment-is-content: namespace", NS_W2_TEST, content.getNamespaceURI());
        }
    }

    private static void testTabControlPageContentAttachmentsUnmodifiable() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        boolean threw = false;
        try {
            result.getPageContentAttachments().put(Integer.valueOf(99), doc.createElement("x"));
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue("tab-attachment-unmodifiable: UnsupportedOperationException thrown", threw);
    }

    private static void testTabControlNoDuplicateElementAcrossOrdinals() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", "C"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        java.util.IdentityHashMap<Element, Boolean> seen = new java.util.IdentityHashMap<Element, Boolean>();
        for (Element content : result.getPageContentAttachments().values()) {
            assertTrue("tab-no-duplicate-element: unique instance", seen.put(content, Boolean.TRUE) == null);
        }
    }

    private static void testTabControlNonTabControlResultAttachmentMapEmpty() throws Exception {
        Document doc = newDocument();
        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", "barX");
        titleBar.setAttribute("left", "0");
        Element label = doc.createElement("Static");
        label.setAttribute("id", "lblX");
        label.setAttribute("left", "0");
        label.setAttribute("text", "Section Title");
        titleBar.appendChild(label);
        doc.appendChild(titleBar);
        RealPipelineResult built = runRealPipeline(titleBar, "TITLE_BAR");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        assertEquals("tab-non-tab-control-empty: status", "RENDERED", String.valueOf(result.getStatus()));
        assertTrue("tab-non-tab-control-empty: attachments empty", result.getPageContentAttachments().isEmpty());
    }

    private static void testTabControlUnsupportedVariantAttachmentMapEmpty() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "n1", "TAB_CONTROL", "dual_header", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "n1",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "n1"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        TargetNodePayload payload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, "n1", Collections.<TargetLeafPayload>emptyList());
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(plan, java.util.Arrays.asList(payload)).get(0);
        assertEquals("tab-unsupported-variant-empty: status", "UNSUPPORTED_VARIANT", String.valueOf(result.getStatus()));
        assertTrue("tab-unsupported-variant-empty: attachments empty", result.getPageContentAttachments().isEmpty());
    }

    private static void testTabControlNoExternalSrcEmitted() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);

        for (Element content : result.getPageContentAttachments().values()) {
            assertTrue("tab-no-external-src: no src attribute", !content.hasAttribute("src"));
        }
        Element root = result.getTargetElement();
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            assertTrue("tab-no-external-src: tabs has no src attribute", !child.hasAttribute("src"));
        }
    }

    private static void testTabControlMissingLabelFailsClosed() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", null});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("tab-missing-label-fails: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("tab-missing-label-fails: attachments empty", result.getPageContentAttachments().isEmpty());
    }

    private static void testTabControlWrongVariantUnsupported() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        TargetCompositionNode original = built.plan.getNodes().get(0);
        TargetCompositionNode tamperedNode = new TargetCompositionNode(
                original.getNodeId(), "TAB_CONTROL", "wrong_variant", "HIGH", original.getParameters(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, original.getSourceStructuralId(), original.getIdentity());
        TargetCompositionPlan tamperedPlan = new TargetCompositionPlan(
                java.util.Arrays.asList(tamperedNode), Collections.<TargetCompositionEdge>emptyList());
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(tamperedPlan, built.payloads).get(0);
        assertEquals("tab-wrong-variant: status", "UNSUPPORTED_VARIANT", String.valueOf(result.getStatus()));
    }

    private static void testTabControlTabCountMissingParameterRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        TargetCompositionNode original = built.plan.getNodes().get(0);
        TargetCompositionNode tamperedNode = new TargetCompositionNode(
                original.getNodeId(), "TAB_CONTROL", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, original.getSourceStructuralId(), original.getIdentity());
        TargetCompositionPlan tamperedPlan = new TargetCompositionPlan(
                java.util.Arrays.asList(tamperedNode), Collections.<TargetCompositionEdge>emptyList());
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(tamperedPlan, built.payloads).get(0);
        assertEquals("tab-missing-tab-count: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testTabControlDuplicatePageOrdinalRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        Map<String, Object> ordinal0 = new LinkedHashMap<String, Object>();
        ordinal0.put("pageOrdinal", Integer.valueOf(0));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", ordinal0, "source_text_attribute", "p0"));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal0, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-duplicate-page-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testTabControlPageOrdinalCountMismatchRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", "C"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> onlyTwoOfThree = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : built.payloads.get(0).getItems()) {
            if (!Integer.valueOf(2).equals(item.getStructuredData().get("pageOrdinal"))) {
                onlyTwoOfThree.add(item);
            }
        }
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), onlyTwoOfThree);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-page-ordinal-count-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    /** 3-page valid fixture(A,B,C)로 실제 Segmenter -&gt; Extractor 경로를 직접 검증한다: semantic
     * evidence 단계와 payload 단계가 같은 값 집합을 가지며, 후자가 전자의 exact copy임을
     * sourceComponentStructuralId 대응으로 확인한다. */
    private static void testTabControlSegmenterToExtractorExactPageOrdinalPropagation() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", "C"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");

        List<SourcePayloadEvidenceItem> tabLabelEvidence = new ArrayList<SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : built.targetRegion.getPayloadEvidence()) {
            if ("tab_label".equals(item.getEvidenceRole())) {
                tabLabelEvidence.add(item);
            }
        }
        assertEquals("tab-propagation: semantic tab_label evidence count", "3",
                String.valueOf(tabLabelEvidence.size()));
        java.util.Set<Integer> semanticOrders = new java.util.TreeSet<Integer>();
        for (SourcePayloadEvidenceItem item : tabLabelEvidence) {
            semanticOrders.add(Integer.valueOf(item.getSourceOrder()));
        }
        assertEquals("tab-propagation: semantic sourceOrder set", "[0, 1, 2]", semanticOrders.toString());

        List<TargetLeafPayload> tabLabelLeaves = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : built.payloads.get(0).getItems()) {
            if (item.getStructuredData().containsKey("pageOrdinal")) {
                tabLabelLeaves.add(item);
            }
        }
        assertEquals("tab-propagation: target payload tab_label leaf count", "3",
                String.valueOf(tabLabelLeaves.size()));
        java.util.Set<Integer> payloadOrdinals = new java.util.TreeSet<Integer>();
        for (TargetLeafPayload item : tabLabelLeaves) {
            payloadOrdinals.add((Integer) item.getStructuredData().get("pageOrdinal"));
        }
        assertEquals("tab-propagation: target payload structuredData pageOrdinal set", "[0, 1, 2]",
                payloadOrdinals.toString());

        // 리스트 위치가 아니라 sourceComponentStructuralId를 키로 삼는 정확한 복사 검증이다 --
        // 만약 추출 로직이 item.getSourceOrder()를 복사하는 대신 리스트 순서에서 ordinal을
        // 다시 계산하도록 바뀌더라도, 이 컴포넌트별 대조 검사가 이를 잡아낸다.
        Map<String, Integer> semanticOrderByComponent = new LinkedHashMap<String, Integer>();
        for (SourcePayloadEvidenceItem item : tabLabelEvidence) {
            semanticOrderByComponent.put(item.getSourceComponentStructuralId(), Integer.valueOf(item.getSourceOrder()));
        }
        for (TargetLeafPayload leaf : tabLabelLeaves) {
            Integer expectedOrdinal = semanticOrderByComponent.get(leaf.getSourceComponentStructuralId());
            assertTrue("tab-propagation: leaf sourceComponentStructuralId matches a semantic evidence item",
                    expectedOrdinal != null);
            assertEquals("tab-propagation: exact-copy pageOrdinal for " + leaf.getSourceComponentStructuralId(),
                    String.valueOf(expectedOrdinal), String.valueOf(leaf.getStructuredData().get("pageOrdinal")));
        }

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(built.plan, built.payloads);
        AtomicRenderResult result = results.get(0);
        assertEquals("tab-propagation: render status", "RENDERED", String.valueOf(result.getStatus()));
        Element root = result.getTargetElement();
        int tabsCount = 0;
        int contentCount = 0;
        List<String> observedLabels = new ArrayList<String>();
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("tabs".equals(child.getLocalName())) { tabsCount++; observedLabels.add(child.getAttribute("label")); }
            if ("content".equals(child.getLocalName())) { contentCount++; }
        }
        assertEquals("tab-propagation: w2:tabs count", "3", String.valueOf(tabsCount));
        assertEquals("tab-propagation: exact label order", "A,B,C", String.join(",", observedLabels));
        assertEquals("tab-propagation: w2:content count", "3", String.valueOf(contentCount));
        assertEquals("tab-propagation: pageContentAttachments key set size", "3",
                String.valueOf(result.getPageContentAttachments().size()));
        for (int i = 0; i < 3; i++) {
            assertTrue("tab-propagation: pageContentAttachments key " + i + " present",
                    result.getPageContentAttachments().containsKey(Integer.valueOf(i)));
        }
    }

    /** 3-page fixture(page2 label 없음)에서 semantic evidence/target payload 단계 둘 다 3개
     * 항목이 생존함(silent leaf loss 없음)을 확인한 뒤, atomic render가 INTEGRITY_VIOLATION으로
     * fail-closed하는 것을 확인한다. */
    private static void testTabControlMissingLabelSegmenterToExtractorEvidencePreserved() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B", null});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");

        List<SourcePayloadEvidenceItem> tabLabelEvidence = new ArrayList<SourcePayloadEvidenceItem>();
        for (SourcePayloadEvidenceItem item : built.targetRegion.getPayloadEvidence()) {
            if ("tab_label".equals(item.getEvidenceRole())) {
                tabLabelEvidence.add(item);
            }
        }
        assertEquals("tab-missing-propagation: semantic tab_label evidence count (no silent loss)", "3",
                String.valueOf(tabLabelEvidence.size()));
        SourcePayloadEvidenceItem thirdEvidence = null;
        for (SourcePayloadEvidenceItem item : tabLabelEvidence) {
            if (item.getSourceOrder() == 2) { thirdEvidence = item; }
        }
        assertTrue("tab-missing-propagation: third (sourceOrder=2) evidence item exists", thirdEvidence != null);
        assertTrue("tab-missing-propagation: third evidence value is null", thirdEvidence.getValue() == null);

        List<TargetLeafPayload> tabLabelLeaves = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : built.payloads.get(0).getItems()) {
            if (item.getStructuredData().containsKey("pageOrdinal")) {
                tabLabelLeaves.add(item);
            }
        }
        assertEquals("tab-missing-propagation: target payload tab_label leaf count (no silent loss)", "3",
                String.valueOf(tabLabelLeaves.size()));
        java.util.Set<Integer> payloadOrdinals = new java.util.TreeSet<Integer>();
        boolean sawNullValueLeaf = false;
        for (TargetLeafPayload leaf : tabLabelLeaves) {
            payloadOrdinals.add((Integer) leaf.getStructuredData().get("pageOrdinal"));
            if (Integer.valueOf(2).equals(leaf.getStructuredData().get("pageOrdinal"))) {
                sawNullValueLeaf = (leaf.getValue() == null);
            }
        }
        assertEquals("tab-missing-propagation: target payload pageOrdinal set", "[0, 1, 2]", payloadOrdinals.toString());
        assertTrue("tab-missing-propagation: pageOrdinal=2 leaf has null value", sawNullValueLeaf);

        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("tab-missing-propagation: render status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("tab-missing-propagation: attachments empty", result.getPageContentAttachments().isEmpty());
    }

    private static void testTabControlNonIntegerPageOrdinalRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        Map<String, Object> nonInteger = new LinkedHashMap<String, Object>();
        nonInteger.put("pageOrdinal", "0");
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", nonInteger, "source_text_attribute", "p0"));
        Map<String, Object> ordinal1 = new LinkedHashMap<String, Object>();
        ordinal1.put("pageOrdinal", Integer.valueOf(1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal1, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-non-integer-page-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testTabControlNegativePageOrdinalRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        Map<String, Object> negative = new LinkedHashMap<String, Object>();
        negative.put("pageOrdinal", Integer.valueOf(-1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", negative, "source_text_attribute", "p0"));
        Map<String, Object> ordinal1 = new LinkedHashMap<String, Object>();
        ordinal1.put("pageOrdinal", Integer.valueOf(1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal1, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-negative-page-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testTabControlOutOfRangePageOrdinalRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        Map<String, Object> outOfRange = new LinkedHashMap<String, Object>();
        outOfRange.put("pageOrdinal", Integer.valueOf(2));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", outOfRange, "source_text_attribute", "p0"));
        Map<String, Object> ordinal1 = new LinkedHashMap<String, Object>();
        ordinal1.put("pageOrdinal", Integer.valueOf(1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal1, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-out-of-range-page-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testTabControlMissingPageOrdinalFieldRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", new LinkedHashMap<String, Object>(),
                "source_text_attribute", "p0"));
        Map<String, Object> ordinal1 = new LinkedHashMap<String, Object>();
        ordinal1.put("pageOrdinal", Integer.valueOf(1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal1, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-missing-page-ordinal-field: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    /** SemanticRegionSegmenter.addTabLabelEvidenceItem은 이미 공백을 null로 정규화하므로 실제
     * blank leaf는 tampered TargetLeafPayload로만 구성 가능하다. */
    private static void testTabControlBlankLabelRejected() throws Exception {
        Document doc = newDocument();
        Element tab = buildTabControlFixture(doc, new String[] {"A", "B"});
        RealPipelineResult built = runRealPipeline(tab, "TAB_CONTROL");
        List<TargetLeafPayload> tamperedItems = new ArrayList<TargetLeafPayload>();
        Map<String, Object> ordinal0 = new LinkedHashMap<String, Object>();
        ordinal0.put("pageOrdinal", Integer.valueOf(0));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "   ", ordinal0, "source_text_attribute", "p0"));
        Map<String, Object> ordinal1 = new LinkedHashMap<String, Object>();
        ordinal1.put("pageOrdinal", Integer.valueOf(1));
        tamperedItems.add(new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", ordinal1, "source_text_attribute", "p1"));
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("tab-blank-label: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("tab-blank-label: attachments empty", result.getPageContentAttachments().isEmpty());
    }

    // ==== SEARCH_AREA.basic atomic 렌더링 ====

    private static final String NS_XF_TEST = "http://www.w3.org/2002/xforms";
    /** BUTTON_GROUP_TARGET_EVENT_NAMESPACE = ACCEPTED_EV_NAMESPACE, Reviewer가 승인/고정한
     * EV 네임스페이스 URI (production {@code AtomicWebSquareRenderer.NS_EV}와 일치). */
    private static final String NS_EV_TEST = "http://www.w3.org/2001/xml-events";

    private static Element buildSearchAreaFixture(Document doc, String[][][] rows) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element search = doc.createElement("Div");
        search.setAttribute("id", "search1");
        int idx = 0;
        for (int r = 0; r < rows.length; r++) {
            for (int p = 0; p < rows[r].length; p++) {
                appendSearchAreaPair(doc, search, "lbl" + idx, rows[r][p][0], "ctl" + idx, rows[r][p][1],
                        r * 30, p * 170, p * 170 + 60);
                idx++;
            }
        }
        form.appendChild(search);
        // ComponentPredicateAnalyzer는 라벨/컨트롤 테이블을 BUSINESS_TABLE이 아닌 SEARCH_AREA로
        // 분류하기 위해 가장 가까운 구조적 GRID peer가 필요하다(findNearestStructuralPeer).
        form.appendChild(buildMinimalGridPeerForSearchArea(doc));
        return form;
    }

    private static void appendSearchAreaPair(
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

    private static Element buildMinimalGridPeerForSearchArea(Document doc) {
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "gridPeer");
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

    private static List<Element> directChildren(Element parent) {
        List<Element> children = new ArrayList<Element>();
        org.w3c.dom.Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) { children.add((Element) child); }
            child = child.getNextSibling();
        }
        return children;
    }

    private static void collectAllElements(Element el, List<Element> out) {
        out.add(el);
        for (Element child : directChildren(el)) { collectAllElements(child, out); }
    }

    private static void testSearchAreaSingleRowSinglePair() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-single: status", "RENDERED", String.valueOf(result.getStatus()));
        Element root = result.getTargetElement();
        List<Element> rows = directChildren(root);
        assertEquals("search-single: row count", "1", String.valueOf(rows.size()));
        List<Element> pairChildren = directChildren(rows.get(0));
        assertEquals("search-single: pair element count (label+control)", "2", String.valueOf(pairChildren.size()));
    }

    private static void testSearchAreaMultipleRowsVariablePairCounts() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {
                {{"Name", "Edit"}, {"Status", "Combo"}}, {{"Kind", "Radio"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-variable: status", "RENDERED", String.valueOf(result.getStatus()));
        List<Element> rows = directChildren(result.getTargetElement());
        assertEquals("search-variable: row count", "2", String.valueOf(rows.size()));
        assertEquals("search-variable: row0 pair element count", "4", String.valueOf(directChildren(rows.get(0)).size()));
        assertEquals("search-variable: row1 pair element count", "2", String.valueOf(directChildren(rows.get(1)).size()));
    }

    private static void testSearchAreaRowsOrderedByAscendingRowIndexEvenIfShuffled() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {
                {{"Row0", "Edit"}}, {{"Row1", "Edit"}}, {{"Row2", "Edit"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        List<TargetLeafPayload> shuffled = new ArrayList<TargetLeafPayload>(built.payloads.get(0).getItems());
        Collections.reverse(shuffled);
        TargetNodePayload shuffledPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), shuffled);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(shuffledPayload)).get(0);
        assertEquals("search-row-order: status", "RENDERED", String.valueOf(result.getStatus()));
        List<Element> rows = directChildren(result.getTargetElement());
        assertEquals("search-row-order: row count", "3", String.valueOf(rows.size()));
        for (int i = 0; i < 3; i++) {
            Element label = directChildren(rows.get(i)).get(0);
            assertEquals("search-row-order: row " + i + " label text (ascending rowIndex, not list order)",
                    "Row" + i, label.getTextContent());
        }
    }

    private static void testSearchAreaPairsOrderedByAscendingPairIndexEvenIfShuffled() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {
                {{"Pair0", "Edit"}, {"Pair1", "Edit"}, {"Pair2", "Edit"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        List<TargetLeafPayload> shuffled = new ArrayList<TargetLeafPayload>(built.payloads.get(0).getItems());
        Collections.reverse(shuffled);
        TargetNodePayload shuffledPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), shuffled);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(shuffledPayload)).get(0);
        assertEquals("search-pair-order: status", "RENDERED", String.valueOf(result.getStatus()));
        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        assertEquals("search-pair-order: pair element count", "6", String.valueOf(pairChildren.size()));
        for (int p = 0; p < 3; p++) {
            Element label = pairChildren.get(p * 2);
            assertEquals("search-pair-order: pair " + p + " label text (ascending pairIndexInRow, not list order)",
                    "Pair" + p, label.getTextContent());
        }
    }

    private static void testSearchAreaLabelBeforeControlWithinEveryPair() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}, {"Status", "Combo"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        assertEquals("search-label-first: pair0 label localName", "textbox", pairChildren.get(0).getLocalName());
        assertEquals("search-label-first: pair0 control localName", "input", pairChildren.get(1).getLocalName());
        assertEquals("search-label-first: pair1 label localName", "textbox", pairChildren.get(2).getLocalName());
        assertEquals("search-label-first: pair1 control localName", "select1", pairChildren.get(3).getLocalName());
    }

    private static Element renderSingleControl(String controlTag) throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Field", controlTag}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-control-" + controlTag + ": status", "RENDERED", String.valueOf(result.getStatus()));
        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        return pairChildren.get(1);
    }

    private static void testSearchAreaEditMapsToXfInput() throws Exception {
        Element control = renderSingleControl("Edit");
        assertEquals("search-edit: localName", "input", control.getLocalName());
        assertEquals("search-edit: namespace", NS_XF_TEST, control.getNamespaceURI());
        assertTrue("search-edit: no appearance attribute", !control.hasAttribute("appearance"));
    }

    private static void testSearchAreaComboMapsToXfSelect1Minimal() throws Exception {
        Element control = renderSingleControl("Combo");
        assertEquals("search-combo: localName", "select1", control.getLocalName());
        assertEquals("search-combo: namespace", NS_XF_TEST, control.getNamespaceURI());
        assertEquals("search-combo: appearance", "minimal", control.getAttribute("appearance"));
    }

    private static void testSearchAreaCalendarMapsToW2InputCalendar() throws Exception {
        Element control = renderSingleControl("Calendar");
        assertEquals("search-calendar: localName", "inputCalendar", control.getLocalName());
        assertEquals("search-calendar: namespace", NS_W2_TEST, control.getNamespaceURI());
        assertTrue("search-calendar: no appearance attribute", !control.hasAttribute("appearance"));
    }

    /**
     * Slice 99E -- CheckBox는 accepted v6 rendering/runtime 동등성이 증명되지 않아 이제 renderer에
     * 전혀 도달하지 못한다({@code TargetPayloadExtractor}가 먼저 fail-closed). primary 실패가
     * renderer 이전임을 여기서 증명한다.
     */
    private static void testSearchAreaCheckBoxFailsClosedBeforeRenderer() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Field", "CheckBox"}}});
        try {
            runRealPipeline(form, "SEARCH_AREA");
            failures++;
            System.out.println("[FAIL] search-checkbox-fail-closed: expected IllegalStateException but none "
                    + "was thrown");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("checkbox_unbound_rendering_equivalence_not_proven")) {
                System.out.println("[PASS] search-checkbox-fail-closed: explicit failure before renderer -- "
                        + e.getMessage());
            } else {
                failures++;
                System.out.println("[FAIL] search-checkbox-fail-closed: wrong reason -- " + e.getMessage());
            }
        }
    }

    private static void testSearchAreaRadioMapsToXfSelect1Full() throws Exception {
        Element control = renderSingleControl("Radio");
        assertEquals("search-radio: localName", "select1", control.getLocalName());
        assertEquals("search-radio: namespace", NS_XF_TEST, control.getNamespaceURI());
        assertEquals("search-radio: appearance", "full", control.getAttribute("appearance"));
    }

    /**
     * Slice 99E -- CheckBox는 accepted v6 rendering/runtime 동등성 미증명으로 fail-closed되므로
     * 이 다중 row/control 매핑 검증에서는 제외한다(별도로 fail-closed 전용 테스트가 증명).
     */
    private static void testSearchAreaAllFourNonCheckBoxControlMappingsAcrossMultipleRows() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {
                {{"F1", "Edit"}, {"F2", "Combo"}}, {{"F3", "Calendar"}}, {{"F5", "Radio"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-all-four: status", "RENDERED", String.valueOf(result.getStatus()));
        List<Element> rows = directChildren(result.getTargetElement());
        assertEquals("search-all-four: row count", "3", String.valueOf(rows.size()));
        String[] expectedLocalNames = {"input", "select1", "inputCalendar", "select1"};
        int i = 0;
        for (Element row : rows) {
            List<Element> pairChildren = directChildren(row);
            for (int c = 1; c < pairChildren.size(); c += 2) {
                assertEquals("search-all-four: control " + i + " localName", expectedLocalNames[i],
                        pairChildren.get(c).getLocalName());
                i++;
            }
        }
        assertEquals("search-all-four: exactly 4 controls visited", "4", String.valueOf(i));
    }

    private static void testSearchAreaRootIsXfGroup() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        Element root = result.getTargetElement();
        assertEquals("search-root: localName", "group", root.getLocalName());
        assertEquals("search-root: namespace", NS_XF_TEST, root.getNamespaceURI());
    }

    private static void testSearchAreaEachRowIsDirectXfGroupChild() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"A", "Edit"}}, {{"B", "Edit"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        for (Element row : directChildren(result.getTargetElement())) {
            assertEquals("search-row-shape: localName", "group", row.getLocalName());
            assertEquals("search-row-shape: namespace", NS_XF_TEST, row.getNamespaceURI());
        }
    }

    private static void testSearchAreaNoShbox() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}, {"Status", "Combo"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        List<Element> all = new ArrayList<Element>();
        collectAllElements(result.getTargetElement(), all);
        for (Element el : all) {
            assertTrue("search-no-shbox: class is not 'shbox' for " + el.getLocalName(),
                    !"shbox".equals(el.getAttribute("class")));
        }
    }

    private static void testSearchAreaNoShboxInner() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}, {"Status", "Combo"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        List<Element> all = new ArrayList<Element>();
        collectAllElements(result.getTargetElement(), all);
        for (Element el : all) {
            assertTrue("search-no-shbox-inner: class is not 'shbox_inner' for " + el.getLocalName(),
                    !"shbox_inner".equals(el.getAttribute("class")));
        }
    }

    private static void testSearchAreaNoBusinessTableSpecificStructure() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}, {"Status", "Combo"}}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        List<Element> all = new ArrayList<Element>();
        collectAllElements(result.getTargetElement(), all);
        for (Element el : all) {
            assertTrue("search-no-business-table: no 'tagname' attribute (BUSINESS_TABLE-only marker) on "
                    + el.getLocalName(), !el.hasAttribute("tagname"));
            assertTrue("search-no-business-table: class is not 'tbbox' for " + el.getLocalName(),
                    !"tbbox".equals(el.getAttribute("class")));
            assertTrue("search-no-business-table: localName is not 'gridView' for " + el.getLocalName(),
                    !"gridView".equals(el.getLocalName()));
        }
    }

    // ---- 잘못된(malformed) SEARCH_AREA 페이로드 -----------------------------------------------

    private static RealPipelineResult basicSearchAreaPipeline() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaFixture(doc, new String[][][] {{{"Name", "Edit"}}});
        return runRealPipeline(form, "SEARCH_AREA");
    }

    private static TargetLeafPayload labelLeaf(int row, int cell, int pair) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("rowIndex", Integer.valueOf(row));
        data.put("cellIndexInRow", Integer.valueOf(cell));
        data.put("pairIndexInRow", Integer.valueOf(pair));
        return new TargetLeafPayload(TargetPayloadCategory.DISPLAY_TEXT, "Name", data, "source_text_attribute", "lbl0");
    }

    private static TargetLeafPayload controlLeaf(int row, int cell, int pair, String tag) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("rowIndex", Integer.valueOf(row));
        data.put("cellIndexInRow", Integer.valueOf(cell));
        data.put("pairIndexInRow", Integer.valueOf(pair));
        return new TargetLeafPayload(TargetPayloadCategory.CONTROL_TYPE, tag, data, "source_tag_name", "ctl0");
    }

    private static AtomicRenderResult renderTamperedSearchArea(
            RealPipelineResult built, List<TargetLeafPayload> tamperedItems) {
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tamperedItems);
        return new AtomicWebSquareRenderer().render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
    }

    private static void testSearchAreaMissingRowIndexRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("cellIndexInRow", Integer.valueOf(0));
        labelData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-missing-row: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNonIntegerRowIndexRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", "0");
        labelData.put("cellIndexInRow", Integer.valueOf(0));
        labelData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-non-integer-row: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNegativeRowIndexRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(labelLeaf(-1, 0, 0), controlLeaf(-1, 1, 0, "Edit")));
        assertEquals("search-negative-row: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaMissingCellIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", Integer.valueOf(0));
        labelData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-missing-cell: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNonIntegerCellIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", Integer.valueOf(0));
        labelData.put("cellIndexInRow", "0");
        labelData.put("pairIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-non-integer-cell: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNegativeCellIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(labelLeaf(0, -1, 0), controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-negative-cell: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaMissingPairIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", Integer.valueOf(0));
        labelData.put("cellIndexInRow", Integer.valueOf(0));
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-missing-pair: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNonIntegerPairIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        Map<String, Object> labelData = new LinkedHashMap<String, Object>();
        labelData.put("rowIndex", Integer.valueOf(0));
        labelData.put("cellIndexInRow", Integer.valueOf(0));
        labelData.put("pairIndexInRow", "0");
        TargetLeafPayload label = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Name", labelData, "source_text_attribute", "lbl0");
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(label, controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-non-integer-pair: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaNegativePairIndexInRowRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(labelLeaf(0, 0, -1), controlLeaf(0, 1, -1, "Edit")));
        assertEquals("search-negative-pair: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaDuplicateLabelForOnePairRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(
                labelLeaf(0, 0, 0), labelLeaf(0, 0, 0), controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-duplicate-label: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaDuplicateControlForOnePairRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(
                labelLeaf(0, 0, 0), controlLeaf(0, 1, 0, "Edit"), controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-duplicate-control: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaMissingLabelRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(controlLeaf(0, 1, 0, "Edit")));
        assertEquals("search-missing-label: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaMissingControlRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(labelLeaf(0, 0, 0)));
        assertEquals("search-missing-control: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testSearchAreaUnsupportedControlTypeRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        AtomicRenderResult result = renderTamperedSearchArea(
                built, java.util.Arrays.asList(labelLeaf(0, 0, 0), controlLeaf(0, 1, 0, "TextArea")));
        assertEquals("search-unsupported-control: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    // ---- SEARCH_AREA Combo/Radio option 렌더링(Slice 102D) -----------------------------------------

    /** label+Combo/Radio 1개 pair 뒤에 Dataset 참조 option을 붙인 실제(fabrication 없는) fixture다.
     *  {@code SourceOptionSetResolver}가 narrow subset을 성공적으로 resolve하도록
     *  innerdataset/codecolumn/datacolumn과 실제 sibling Dataset을 함께 구성한다. */
    private static Element buildSearchAreaOptionFixture(
            Document doc, String controlTag, String datasetId, String codeCol, String dataCol,
            String[][] rowsData) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);

        Element objects = doc.createElement("Objects");
        Element dataset = doc.createElement("Dataset");
        dataset.setAttribute("id", datasetId);
        Element columnInfo = doc.createElement("ColumnInfo");
        Element codeColumn = doc.createElement("Column");
        codeColumn.setAttribute("id", codeCol);
        Element dataColumn = doc.createElement("Column");
        dataColumn.setAttribute("id", dataCol);
        columnInfo.appendChild(codeColumn);
        columnInfo.appendChild(dataColumn);
        dataset.appendChild(columnInfo);
        Element rows = doc.createElement("Rows");
        for (String[] rowData : rowsData) {
            Element row = doc.createElement("Row");
            Element codeColEl = doc.createElement("Col");
            codeColEl.setAttribute("id", codeCol);
            codeColEl.setTextContent(rowData[0]);
            Element dataColEl = doc.createElement("Col");
            dataColEl.setAttribute("id", dataCol);
            dataColEl.setTextContent(rowData[1]);
            row.appendChild(codeColEl);
            row.appendChild(dataColEl);
            rows.appendChild(row);
        }
        dataset.appendChild(rows);
        objects.appendChild(dataset);
        form.appendChild(objects);

        Element search = doc.createElement("Div");
        search.setAttribute("id", "search1");
        Element label = doc.createElement("Static");
        label.setAttribute("id", "lbl0");
        label.setAttribute("text", "Field");
        label.setAttribute("left", "0");
        label.setAttribute("top", "0");
        label.setAttribute("width", "50");
        label.setAttribute("height", "20");
        Element control = doc.createElement(controlTag);
        control.setAttribute("id", "ctl0");
        control.setAttribute("innerdataset", datasetId);
        control.setAttribute("codecolumn", codeCol);
        control.setAttribute("datacolumn", dataCol);
        control.setAttribute("left", "60");
        control.setAttribute("top", "0");
        control.setAttribute("width", "100");
        control.setAttribute("height", "20");
        search.appendChild(label);
        search.appendChild(control);
        form.appendChild(search);
        form.appendChild(buildMinimalGridPeerForSearchArea(doc));
        return form;
    }

    private static void assertStaticOptionItem(String label, Element item, String expectedLabelText, String expectedValueText) {
        assertEquals(label + ": localName", "item", item.getLocalName());
        assertEquals(label + ": namespace", NS_XF_TEST, item.getNamespaceURI());
        List<Element> children = directChildren(item);
        assertEquals(label + ": child count", "2", String.valueOf(children.size()));
        assertEquals(label + ": child0 localName", "label", children.get(0).getLocalName());
        assertEquals(label + ": child0 namespace", NS_XF_TEST, children.get(0).getNamespaceURI());
        assertEquals(label + ": child0 text", expectedLabelText, children.get(0).getTextContent());
        assertEquals(label + ": child1 localName", "value", children.get(1).getLocalName());
        assertEquals(label + ": child1 namespace", NS_XF_TEST, children.get(1).getNamespaceURI());
        assertEquals(label + ": child1 text", expectedValueText, children.get(1).getTextContent());
    }

    private static void assertNoItemsetOrDataListOrScriptAnywhere(String labelPrefix, Element root) {
        List<Element> all = new ArrayList<Element>();
        collectAllElements(root, all);
        for (Element el : all) {
            assertTrue(labelPrefix + ": no xf:itemset anywhere (found " + el.getLocalName() + ")",
                    !"itemset".equals(el.getLocalName()));
            assertTrue(labelPrefix + ": no w2:dataList anywhere (found " + el.getLocalName() + ")",
                    !"dataList".equals(el.getLocalName()));
            assertTrue(labelPrefix + ": no script element anywhere (found " + el.getLocalName() + ")",
                    !"script".equals(el.getLocalName()));
        }
    }

    /**
     * 항목 13(positive: Combo) -- narrow subset을 만족하는 Combo는 xf:select1[appearance=minimal]
     * 아래 정확히 xf:choices 1개, 그 안에 source Row document order 그대로의 xf:item(label/value)
     * 목록이 렌더링된다. xf:itemset/w2:dataList/script는 어디에도 나타나지 않는다.
     */
    private static void testSearchAreaComboWithOptionsRendersStaticChoices() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaOptionFixture(doc, "Combo", "dsStatus", "CD", "NM",
                new String[][] {{"A", "사용"}, {"B", "미사용"}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-combo-options: status", "RENDERED", String.valueOf(result.getStatus()));

        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        Element control = pairChildren.get(1);
        assertEquals("search-combo-options: control localName", "select1", control.getLocalName());
        assertEquals("search-combo-options: control appearance", "minimal", control.getAttribute("appearance"));

        List<Element> controlChildren = directChildren(control);
        assertEquals("search-combo-options: exactly 1 xf:choices child", "1", String.valueOf(controlChildren.size()));
        Element choices = controlChildren.get(0);
        assertEquals("search-combo-options: choices localName", "choices", choices.getLocalName());
        assertEquals("search-combo-options: choices namespace", NS_XF_TEST, choices.getNamespaceURI());

        List<Element> items = directChildren(choices);
        assertEquals("search-combo-options: item count", "2", String.valueOf(items.size()));
        assertStaticOptionItem("search-combo-options: item0", items.get(0), "사용", "A");
        assertStaticOptionItem("search-combo-options: item1", items.get(1), "미사용", "B");

        assertNoItemsetOrDataListOrScriptAnywhere("search-combo-options", result.getTargetElement());
    }

    /**
     * 항목 13(positive: Radio) -- narrow subset을 만족하는 Radio는 xf:select1[appearance=full]
     * 아래 정확히 xf:choices 1개, source Row document order 그대로의 xf:item 목록이 렌더링된다.
     */
    private static void testSearchAreaRadioWithOptionsRendersStaticChoices() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaOptionFixture(doc, "Radio", "dsType", "CODE", "NAME",
                new String[][] {{"1", "일반"}, {"2", "특수"}, {"3", "기타"}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("search-radio-options: status", "RENDERED", String.valueOf(result.getStatus()));

        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        Element control = pairChildren.get(1);
        assertEquals("search-radio-options: control localName", "select1", control.getLocalName());
        assertEquals("search-radio-options: control appearance", "full", control.getAttribute("appearance"));

        List<Element> controlChildren = directChildren(control);
        assertEquals("search-radio-options: exactly 1 xf:choices child", "1", String.valueOf(controlChildren.size()));
        Element choices = controlChildren.get(0);
        assertEquals("search-radio-options: choices localName", "choices", choices.getLocalName());
        assertEquals("search-radio-options: choices namespace", NS_XF_TEST, choices.getNamespaceURI());

        List<Element> items = directChildren(choices);
        assertEquals("search-radio-options: item count", "3", String.valueOf(items.size()));
        assertStaticOptionItem("search-radio-options: item0", items.get(0), "일반", "1");
        assertStaticOptionItem("search-radio-options: item1", items.get(1), "특수", "2");
        assertStaticOptionItem("search-radio-options: item2", items.get(2), "기타", "3");

        assertNoItemsetOrDataListOrScriptAnywhere("search-radio-options", result.getTargetElement());
    }

    /** payload list 저장 순서가 아니라 각 item이 들고 있는 명시적 rowOrdinal로 재정렬됨을
     *  증명한다(이 파일의 다른 모든 order-independence 계약과 동일한 원칙). */
    private static void testSearchAreaOptionItemsOrderedByAscendingRowOrdinalEvenIfShuffled() throws Exception {
        Document doc = newDocument();
        Element form = buildSearchAreaOptionFixture(doc, "Combo", "dsStatus", "CD", "NM",
                new String[][] {{"A", "Alpha"}, {"B", "Beta"}, {"C", "Gamma"}});
        RealPipelineResult built = runRealPipeline(form, "SEARCH_AREA");

        List<TargetLeafPayload> tampered = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : built.payloads.get(0).getItems()) {
            if (item.getCategory() == TargetPayloadCategory.CONTROL_TYPE) {
                @SuppressWarnings("unchecked")
                List<TargetOptionItem> optionItems =
                        (List<TargetOptionItem>) item.getStructuredData().get("optionItems");
                List<TargetOptionItem> shuffled = new ArrayList<TargetOptionItem>(optionItems);
                Collections.reverse(shuffled);
                Map<String, Object> data = new LinkedHashMap<String, Object>(item.getStructuredData());
                data.put("optionItems", shuffled);
                tampered.add(new TargetLeafPayload(item.getCategory(), item.getValue(), data,
                        item.getSourceEvidenceKind(), item.getSourceComponentStructuralId()));
            } else {
                tampered.add(item);
            }
        }
        TargetNodePayload tamperedPayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, built.payloads.get(0).getPlanNodeId(), tampered);
        AtomicRenderResult result = new AtomicWebSquareRenderer()
                .render(built.plan, java.util.Arrays.asList(tamperedPayload)).get(0);
        assertEquals("search-option-order: status", "RENDERED", String.valueOf(result.getStatus()));

        List<Element> pairChildren = directChildren(directChildren(result.getTargetElement()).get(0));
        Element control = pairChildren.get(1);
        List<Element> items = directChildren(directChildren(control).get(0));
        assertEquals("search-option-order: item count", "3", String.valueOf(items.size()));
        assertStaticOptionItem(
                "search-option-order: item0 (ascending rowOrdinal, not shuffled list order)",
                items.get(0), "Alpha", "A");
        assertStaticOptionItem("search-option-order: item1", items.get(1), "Beta", "B");
        assertStaticOptionItem("search-option-order: item2", items.get(2), "Gamma", "C");
    }

    /** 항목 21(plain-control 회귀) -- option evidence가 전혀 없는 기존 plain Combo/Radio는
     *  여전히 xf:choices 없이 빈 xf:select1로 그대로 렌더링된다(기존 동작 변화 없음). */
    private static void testSearchAreaPlainComboAndRadioRenderNoStaticChoices() throws Exception {
        Element comboControl = renderSingleControl("Combo");
        assertEquals("search-plain-combo: no children (no xf:choices)", "0",
                String.valueOf(directChildren(comboControl).size()));
        Element radioControl = renderSingleControl("Radio");
        assertEquals("search-plain-radio: no children (no xf:choices)", "0",
                String.valueOf(directChildren(radioControl).size()));
    }

    private static TargetLeafPayload controlLeafWithOptionItems(
            int row, int cell, int pair, String tag, List<?> optionItems) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("rowIndex", Integer.valueOf(row));
        data.put("cellIndexInRow", Integer.valueOf(cell));
        data.put("pairIndexInRow", Integer.valueOf(pair));
        data.put("optionItems", optionItems);
        return new TargetLeafPayload(TargetPayloadCategory.CONTROL_TYPE, tag, data, "source_tag_name", "ctl0");
    }

    /** renderer는 payload list 순서를 신뢰하지 않고 rowOrdinal 기준 {@code TreeMap}으로 재정렬한다
     *  -- 그 방어 로직이 실제로 중복 rowOrdinal(payload tampering)을 fail-closed하는지 확인한다. */
    private static void testSearchAreaOptionItemsDuplicateRowOrdinalRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        List<TargetOptionItem> tamperedOptions = java.util.Arrays.asList(
                new TargetOptionItem(0, "A", "Alpha"), new TargetOptionItem(0, "B", "Beta"));
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(
                labelLeaf(0, 0, 0), controlLeafWithOptionItems(0, 1, 0, "Combo", tamperedOptions)));
        assertEquals("search-option-duplicate-row-ordinal: status", "INTEGRITY_VIOLATION",
                String.valueOf(result.getStatus()));
    }

    /** rowOrdinal이 0..N-1로 dense하지 않으면(중간이 비어 있으면) fail-closed한다 --
     *  fake item으로 구멍을 메우지 않는다. */
    private static void testSearchAreaOptionItemsNonDenseRowOrdinalRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        List<TargetOptionItem> tamperedOptions = java.util.Arrays.asList(
                new TargetOptionItem(0, "A", "Alpha"), new TargetOptionItem(2, "C", "Gamma"));
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(
                labelLeaf(0, 0, 0), controlLeafWithOptionItems(0, 1, 0, "Combo", tamperedOptions)));
        assertEquals("search-option-non-dense-row-ordinal: status", "INTEGRITY_VIOLATION",
                String.valueOf(result.getStatus()));
    }

    /** structuredData["optionItems"] 리스트 원소가 {@code TargetOptionItem}이 아니면(payload
     *  tampering) fail-closed한다. */
    private static void testSearchAreaOptionItemsWrongElementTypeRejected() throws Exception {
        RealPipelineResult built = basicSearchAreaPipeline();
        List<Object> tamperedOptions = java.util.Arrays.asList((Object) "not-an-option-item");
        AtomicRenderResult result = renderTamperedSearchArea(built, java.util.Arrays.asList(
                labelLeaf(0, 0, 0), controlLeafWithOptionItems(0, 1, 0, "Combo", tamperedOptions)));
        assertEquals("search-option-wrong-element-type: status", "INTEGRITY_VIOLATION",
                String.valueOf(result.getStatus()));
    }

    // ==== BUTTON_GROUP 원자적(atomic) 렌더링 ====

    private static Element buildButtonGroupFixture(Document doc, String... buttonTexts) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element group = doc.createElement("Div");
        group.setAttribute("id", "btnGroup1");
        group.setAttribute("width", "400");
        for (int i = 0; i < buttonTexts.length; i++) {
            Element button = doc.createElement("Button");
            button.setAttribute("id", "btn" + i);
            if (buttonTexts[i] != null) { button.setAttribute("text", buttonTexts[i]); }
            button.setAttribute("left", String.valueOf(i * 60));
            group.appendChild(button);
        }
        form.appendChild(group);
        return form;
    }

    private static Element buildTitleBarAttachedButtonGroupFixture(Document doc, String... buttonTexts) {
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element titleBar = doc.createElement("Div");
        titleBar.setAttribute("id", "titleBar1");
        titleBar.setAttribute("width", "400");
        Element titleStatic = doc.createElement("Static");
        titleStatic.setAttribute("id", "titleText1");
        titleStatic.setAttribute("text", "Screen Title");
        titleStatic.setAttribute("left", "0");
        titleBar.appendChild(titleStatic);
        form.appendChild(titleBar);

        Element group = doc.createElement("Div");
        group.setAttribute("id", "btnGroup1");
        group.setAttribute("width", "400");
        for (int i = 0; i < buttonTexts.length; i++) {
            Element button = doc.createElement("Button");
            button.setAttribute("id", "btn" + i);
            if (buttonTexts[i] != null) { button.setAttribute("text", buttonTexts[i]); }
            button.setAttribute("left", String.valueOf(i * 60));
            group.appendChild(button);
        }
        form.appendChild(group);
        return form;
    }

    private static TargetCompositionNode buttonGroupNode(String nodeId, String variant) {
        return new TargetCompositionNode(
                nodeId, "BUTTON_GROUP", variant, "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static AtomicRenderResult renderButtonGroup(String variant, Integer expectedCount, List<TargetLeafPayload> items) {
        TargetCompositionNode node = buttonGroupNode("bg1", variant);
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(node), Collections.<TargetCompositionEdge>emptyList());
        TargetNodePayload payload = new TargetNodePayload(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "bg1", items, expectedCount);
        return new AtomicWebSquareRenderer().render(plan, java.util.Arrays.asList(payload)).get(0);
    }

    private static TargetLeafPayload buttonGroupButtonLeaf(int ordinal, String text) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("buttonOrdinal", Integer.valueOf(ordinal));
        return new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, text, data, "source_text_attribute", "b" + ordinal);
    }

    private static TargetLeafPayload buttonGroupFinalizedEventLeaf(
            int ordinal, String targetEventLocalName, String targetFunctionIdentifier) {
        TargetEventBinding binding = new TargetEventBinding(ordinal, targetEventLocalName, targetFunctionIdentifier);
        return new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", Collections.<String, Object>emptyMap(), "event_binding",
                "b" + ordinal, binding);
    }

    private static TargetLeafPayload buttonGroupRawEventLeafNoBinding(int ordinal, String rawFunctionName) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("eventName", "onclick");
        data.put("functionName", rawFunctionName);
        return new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", data, "event_binding", "b" + ordinal);
    }

    private static Element buttonGroupRoot(AtomicRenderResult result) {
        assertEquals("button-group: status", "RENDERED", String.valueOf(result.getStatus()));
        return result.getTargetElement();
    }

    private static void testButtonGroupOneEventlessButtonRenders() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "Save");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        Element root = buttonGroupRoot(result);
        List<Element> triggers = directChildren(root);
        assertEquals("bg-one-eventless: trigger count", "1", String.valueOf(triggers.size()));
        Element label = directChildren(triggers.get(0)).get(0);
        assertEquals("bg-one-eventless: label text", "Save", label.getTextContent());
    }

    private static void testButtonGroupMultipleEventlessButtonsPreserveOrdinalOrder() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "A", "B", "C");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        Element root = buttonGroupRoot(result);
        List<Element> triggers = directChildren(root);
        assertEquals("bg-multi-eventless: trigger count", "3", String.valueOf(triggers.size()));
        for (int i = 0; i < 3; i++) {
            String expected = String.valueOf((char) ('A' + i));
            Element label = directChildren(triggers.get(i)).get(0);
            assertEquals("bg-multi-eventless: label " + i + " text (ascending buttonOrdinal)",
                    expected, label.getTextContent());
        }
    }

    private static void testButtonGroupStandaloneVariantRenders() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "Save");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        assertEquals("bg-standalone: plan variant", "standalone", built.plan.getNodes().get(0).getVariant());
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("bg-standalone: status", "RENDERED", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupTitleBarAttachedVariantRendersSameStructure() throws Exception {
        Document doc = newDocument();
        Element form = buildTitleBarAttachedButtonGroupFixture(doc, "Save");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        assertEquals("bg-title-attached: plan variant", "title_bar_attached",
                built.plan.getNodes().get(0).getVariant());
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        Element root = buttonGroupRoot(result);
        assertEquals("bg-title-attached: root localName", "group", root.getLocalName());
        assertEquals("bg-title-attached: root namespace", NS_XF_TEST, root.getNamespaceURI());
        List<Element> triggers = directChildren(root);
        assertEquals("bg-title-attached: trigger count (same v1 atomic structure as standalone)", "1",
                String.valueOf(triggers.size()));
        assertEquals("bg-title-attached: trigger localName", "trigger", triggers.get(0).getLocalName());
    }

    private static void testButtonGroupRootIsXfGroup() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "Save");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        Element root = buttonGroupRoot(new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0));
        assertEquals("bg-root: localName", "group", root.getLocalName());
        assertEquals("bg-root: namespace", NS_XF_TEST, root.getNamespaceURI());
    }

    private static void testButtonGroupEveryButtonIsXfTrigger() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "A", "B");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        Element root = buttonGroupRoot(new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0));
        for (Element trigger : directChildren(root)) {
            assertEquals("bg-every-trigger: localName", "trigger", trigger.getLocalName());
            assertEquals("bg-every-trigger: namespace", NS_XF_TEST, trigger.getNamespaceURI());
        }
    }

    private static void testButtonGroupEveryTriggerHasTypeButton() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "A", "B");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        Element root = buttonGroupRoot(new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0));
        for (Element trigger : directChildren(root)) {
            assertEquals("bg-trigger-type: type attribute", "button", trigger.getAttribute("type"));
        }
    }

    private static void testButtonGroupEveryTriggerHasOneXfLabel() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "A", "B");
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        Element root = buttonGroupRoot(new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0));
        for (Element trigger : directChildren(root)) {
            List<Element> children = directChildren(trigger);
            assertEquals("bg-trigger-label: exactly one child", "1", String.valueOf(children.size()));
            assertEquals("bg-trigger-label: localName", "label", children.get(0).getLocalName());
            assertEquals("bg-trigger-label: namespace", NS_XF_TEST, children.get(0).getNamespaceURI());
        }
    }

    private static void testButtonGroupNoTextNoValueButtonRejected() throws Exception {
        Document doc = newDocument();
        Element form = buildButtonGroupFixture(doc, "Save", null);
        RealPipelineResult built = runRealPipeline(form, "BUTTON_GROUP");
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(built.plan, built.payloads).get(0);
        assertEquals("bg-no-text-no-value: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupExpectedCountMissingRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup(
                "standalone", null, java.util.Arrays.asList(buttonGroupButtonLeaf(0, "A")));
        assertEquals("bg-missing-expected-count: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupExpectedCountMismatchRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup(
                "standalone", Integer.valueOf(2), java.util.Arrays.asList(buttonGroupButtonLeaf(0, "A")));
        assertEquals("bg-expected-count-mismatch: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupMissingOrdinalRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(2),
                java.util.Arrays.asList(buttonGroupButtonLeaf(0, "A"), buttonGroupButtonLeaf(0, "B")));
        assertEquals("bg-missing-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupDuplicateOrdinalRejected() throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("buttonOrdinal", Integer.valueOf(0));
        TargetLeafPayload dup = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "B", data, "source_text_attribute", "b1");
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(2),
                java.util.Arrays.asList(buttonGroupButtonLeaf(0, "A"), dup));
        assertEquals("bg-duplicate-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupNegativeOrdinalRejected() throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("buttonOrdinal", Integer.valueOf(-1));
        TargetLeafPayload negative = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", data, "source_text_attribute", "b0");
        AtomicRenderResult result = renderButtonGroup(
                "standalone", Integer.valueOf(1), java.util.Arrays.asList(negative));
        assertEquals("bg-negative-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupOutOfRangeOrdinalRejected() throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("buttonOrdinal", Integer.valueOf(5));
        TargetLeafPayload outOfRange = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", data, "source_text_attribute", "b0");
        AtomicRenderResult result = renderButtonGroup(
                "standalone", Integer.valueOf(1), java.util.Arrays.asList(outOfRange));
        assertEquals("bg-out-of-range-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupValidFinalizedOnclickAttachesToExactButton() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupFinalizedEventLeaf(0, "onclick", "handleSave")));
        Element root = buttonGroupRoot(result);
        Element trigger = directChildren(root).get(0);
        assertEquals("bg-valid-onclick: attribute", "scwin.handleSave();",
                trigger.getAttributeNS(NS_EV_TEST, "onclick"));
        assertTrue("bg-valid-onclick: no unqualified plain onclick attribute is also emitted",
                !trigger.hasAttribute("onclick"));
    }

    private static void testButtonGroupTwoButtonsDistinctHandlersDoNotCrossAttach() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(2), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupButtonLeaf(1, "Cancel"),
                buttonGroupFinalizedEventLeaf(0, "onclick", "handleSave"),
                buttonGroupFinalizedEventLeaf(1, "onclick", "handleCancel")));
        Element root = buttonGroupRoot(result);
        List<Element> triggers = directChildren(root);
        assertEquals("bg-no-cross-attach: trigger0 handler", "scwin.handleSave();",
                triggers.get(0).getAttributeNS(NS_EV_TEST, "onclick"));
        assertEquals("bg-no-cross-attach: trigger1 handler", "scwin.handleCancel();",
                triggers.get(1).getAttributeNS(NS_EV_TEST, "onclick"));
    }

    private static void testButtonGroupEventlessAndEventfulCoexist() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(2), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupButtonLeaf(1, "Info"),
                buttonGroupFinalizedEventLeaf(0, "onclick", "handleSave")));
        Element root = buttonGroupRoot(result);
        List<Element> triggers = directChildren(root);
        assertTrue("bg-mixed: trigger0 has handler", triggers.get(0).hasAttributeNS(NS_EV_TEST, "onclick"));
        assertTrue("bg-mixed: trigger1 has no handler (eventless is valid)",
                !triggers.get(1).hasAttributeNS(NS_EV_TEST, "onclick"));
    }

    private static void testButtonGroupEventLeafWithoutFinalizedBindingRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupRawEventLeafNoBinding(0, "handleSave")));
        assertEquals("bg-no-finalized-binding: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupFinalizedEventToNonexistentOrdinalRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupFinalizedEventLeaf(9, "onclick", "handleSave")));
        assertEquals("bg-event-nonexistent-ordinal: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupDuplicateFinalizedOnclickForSameButtonRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupFinalizedEventLeaf(0, "onclick", "handleSave"),
                buttonGroupFinalizedEventLeaf(0, "onclick", "handleSaveAgain")));
        assertEquals("bg-duplicate-finalized-onclick: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupUnsupportedFinalizedTargetEventLocalNameRejected() throws Exception {
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), buttonGroupFinalizedEventLeaf(0, "ondblclick", "handleSave")));
        assertEquals("bg-unsupported-event-local-name: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
    }

    private static void testButtonGroupRendererDoesNotReadRawFunctionName() throws Exception {
        // finalized binding은 "handleSaveFinal"을 가리킨다; 만약 렌더러가 잘못하여 raw
        // structuredData를 읽는다면 전혀 다른 값이 나온다 -- 출력 속성이 오직 finalized
        // 식별자만 반영하는지 확인한다.
        Map<String, Object> deceptiveRawData = new LinkedHashMap<String, Object>();
        deceptiveRawData.put("eventName", "onclick");
        deceptiveRawData.put("functionName", "rawFunctionNameThatMustNeverAppear");
        TargetEventBinding binding = new TargetEventBinding(0, "onclick", "handleSaveFinal");
        TargetLeafPayload eventLeaf = new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", deceptiveRawData, "event_binding", "b0", binding);
        AtomicRenderResult result = renderButtonGroup("standalone", Integer.valueOf(1), java.util.Arrays.asList(
                buttonGroupButtonLeaf(0, "Save"), eventLeaf));
        Element trigger = directChildren(buttonGroupRoot(result)).get(0);
        assertEquals("bg-no-raw-function-name: attribute uses only finalized identifier",
                "scwin.handleSaveFinal();", trigger.getAttributeNS(NS_EV_TEST, "onclick"));
        assertTrue("bg-no-raw-function-name: raw functionName never leaks into output",
                !trigger.getAttributeNS(NS_EV_TEST, "onclick").contains("rawFunctionNameThatMustNeverAppear"));
    }

    private static void testButtonGroupRendererDoesNotUseSourceStructuralIdForCorrelation() throws Exception {
        // button leaf와 event leaf는 의도적으로 서로 무관/불일치하는 sourceComponentStructuralId
        // 값을 갖는다 -- 그럼에도 binding.buttonOrdinal만으로 상관관계가 성립해야 하며, 이는
        // structuralId가 이 용도로는 결코 사용되지 않음을 증명한다.
        Map<String, Object> buttonData = new LinkedHashMap<String, Object>();
        buttonData.put("buttonOrdinal", Integer.valueOf(0));
        TargetLeafPayload button = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "Save", buttonData, "source_text_attribute",
                "Form[0]/Div[3]/Button[7]");
        TargetEventBinding binding = new TargetEventBinding(0, "onclick", "handleSave");
        TargetLeafPayload eventLeaf = new TargetLeafPayload(
                TargetPayloadCategory.EVENT, "onclick", Collections.<String, Object>emptyMap(), "event_binding",
                "Form[0]/Div[99]/UnrelatedElement[3]", binding);
        AtomicRenderResult result = renderButtonGroup(
                "standalone", Integer.valueOf(1), java.util.Arrays.asList(button, eventLeaf));
        Element trigger = directChildren(buttonGroupRoot(result)).get(0);
        assertEquals("bg-no-structural-id-correlation: attaches via buttonOrdinal regardless of "
                + "mismatched sourceComponentStructuralId", "scwin.handleSave();",
                trigger.getAttributeNS(NS_EV_TEST, "onclick"));
    }

    private static void testButtonGroupRawEventEvidenceWithoutFinalizedBindingCannotRenderSuccessfully() throws Exception {
        // 실제 event evidence는 XfdlAnalysisResult.getEvents()에서 온다 -- 두-인자
        // segment(root, analysis) overload를 직접 써서 진짜 production event evidence를 재현한다.
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        doc.appendChild(form);
        Element group = doc.createElement("Div");
        group.setAttribute("id", "btnGroup1");
        group.setAttribute("width", "400");
        Element button = doc.createElement("Button");
        button.setAttribute("id", "btnSave");
        button.setAttribute("text", "Save");
        button.setAttribute("left", "0");
        group.appendChild(button);
        form.appendChild(group);

        com.example.xfdltracker.model.XfdlAnalysisResult analysis = new com.example.xfdltracker.model.XfdlAnalysisResult();
        analysis.getEvents().add(new com.example.xfdltracker.model.EventBinding("btnGroup1.btnSave", "onclick", "handleSave"));

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form, analysis);
        SemanticRegionResult target = null;
        for (SemanticRegionResult r : regions) {
            if ("BUTTON_GROUP".equals(r.getSemanticType())) { target = r; }
        }
        assertTrue("bg-raw-evidence: BUTTON_GROUP region found", target != null);
        CompositionDecision decision = new CompositionEvaluator().evaluate(target);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(decision));
        List<TargetNodePayload> payloads = extractWithBindings(form, plan, regions);

        boolean hasRawEventLeafWithoutBinding = false;
        for (TargetLeafPayload item : payloads.get(0).getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT && item.getFinalizedTargetEventBinding() == null) {
                hasRawEventLeafWithoutBinding = true;
            }
        }
        assertTrue("bg-raw-evidence: real extraction produces a raw EVENT leaf with no finalized binding",
                hasRawEventLeafWithoutBinding);

        // TargetPayloadExtractor는 finalizedTargetEventBinding 없는 raw event evidence만 만든다
        // (별도 finalizer 단계가 필요) -- rendering은 raw eventName/functionName을 신뢰하지 않고 fail-closed해야 한다.
        AtomicRenderResult result = new AtomicWebSquareRenderer().render(plan, payloads).get(0);
        assertEquals("bg-raw-evidence-no-finalized-binding: status", "INTEGRITY_VIOLATION",
                String.valueOf(result.getStatus()));
    }

    private static final class RealPipelineResult {
        TargetCompositionPlan plan;
        List<TargetNodePayload> payloads;
        SemanticRegionResult targetRegion;
    }

    /** 실제 파이프라인(Segmenter -&gt; Evaluator -&gt; PlanBuilder -&gt; PayloadExtractor)을
     * 그대로 통과시켜, 지정한 family의 root Plan/Payload를 얻는다(fabrication 없음). */
    private static RealPipelineResult runRealPipeline(Element sourceRoot, String expectedFamily) throws Exception {
        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(sourceRoot);
        SemanticRegionResult target = null;
        for (SemanticRegionResult r : regions) {
            if (expectedFamily.equals(r.getSemanticType())) {
                target = r;
            }
        }
        assertTrue("real-pipeline: " + expectedFamily + " region found", target != null);

        CompositionDecision decision = new CompositionEvaluator().evaluate(target);
        assertTrue("real-pipeline: decision eligible", decision.isEligible());

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(java.util.Arrays.asList(decision));
        List<TargetNodePayload> payloads = extractWithBindings(sourceRoot, plan, regions);

        RealPipelineResult result = new RealPipelineResult();
        result.plan = plan;
        result.payloads = payloads;
        result.targetRegion = target;
        return result;
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
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        DocumentBuilder b = f.newDocumentBuilder();
        return b.newDocument();
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
