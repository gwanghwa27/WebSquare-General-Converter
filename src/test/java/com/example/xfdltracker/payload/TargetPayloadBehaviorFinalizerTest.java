package com.example.xfdltracker.payload;

import com.example.xfdltracker.behavior.TargetScriptArtifact;
import com.example.xfdltracker.composition.TargetNodeIdentityKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link TargetPayloadBehaviorFinalizer}에 대한 오프라인 단위 테스트(JUnit 미사용).
 * 실제 파이프라인과 동일한 형태의 {@link TargetNodePayload}/{@link TargetLeafPayload}를
 * 직접 구성해 finalizer 로직만 격리 검증한다.
 */
public class TargetPayloadBehaviorFinalizerTest {

    private static int failures = 0;
    private static final TargetNodeIdentityKind KIND = TargetNodeIdentityKind.SOURCE_STRUCTURAL;
    private static final String NODE_ID = "Form[0]/Div[1]";

    public static void main(String[] args) throws Exception {
        testCardinalityExpectedZeroActualEmptyValid();
        testCardinalityExpectedOneOrdinalZeroValid();
        testCardinalityExpectedFourOrdinals0123Valid();
        testCardinalityExpectedFourActual012Fails();
        testCardinalityMissingOrdinalFails();
        testCardinalityDuplicateOrdinalFails();
        testCardinalityNegativeOrdinalFails();
        testCardinalityOutOfRangeOrdinalFails();
        testCardinalityAbsentExpectedCountFails();
        testCardinalityNonIntegerOrdinalFails();

        testIdentityIndexValidExactMapping();
        testIdentityIndexDuplicateStructuralIdDifferentOrdinalsFails();
        testIdentityIndexBlankStructuralIdFails();

        testEventlessButtonGroupFinalizes();
        testValidOnclickResolves();
        testEventRefersToExactButtonStructuralIdentity();
        testUnmappedEventFailsUnsupportedEventMapping();
        testOnloadEventNeverMapsToTargetOnpageload();
        testUnresolvedFunctionFails();
        testEventRefersToNonexistentButtonFails();
        testDuplicateFinalizedOnclickForSameButtonFails();
        testTwoDifferentButtonsEachHaveOwnOnclick();
        testFinalizedEventHasExactFields();

        testFinalizedEventLeafContainsBinding();
        testFinalizerOutputPreservesSourceEvidenceAsNonRendererProvenance();
        testNoRawFunctionNameCopiedIntoBinding();
        testNoSourceStructuralIdCopiedIntoBinding();
        testNoTextNoValueButtonLeafSurvivesFinalization();

        testInputPayloadNotMutated();
        testFinalizedOutputCollectionsUnmodifiable();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- cardinality(개수) 검증 ----

    private static void testCardinalityExpectedZeroActualEmptyValid() {
        TargetNodePayload payload = payload(0, Collections.<TargetLeafPayload>emptyList());
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("expected0_actual0", PayloadBehaviorFinalizationStatus.FINALIZED, result);
    }

    private static void testCardinalityExpectedOneOrdinalZeroValid() {
        TargetNodePayload payload = payload(1, list(buttonLeaf(0, "b0", "A")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("expected1_ordinal0", PayloadBehaviorFinalizationStatus.FINALIZED, result);
    }

    private static void testCardinalityExpectedFourOrdinals0123Valid() {
        TargetNodePayload payload = payload(4, list(
                buttonLeaf(0, "b0", "A"), buttonLeaf(1, "b1", "B"),
                buttonLeaf(2, "b2", "C"), buttonLeaf(3, "b3", "D")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("expected4_actual0123", PayloadBehaviorFinalizationStatus.FINALIZED, result);
    }

    private static void testCardinalityExpectedFourActual012Fails() {
        TargetNodePayload payload = payload(4, list(
                buttonLeaf(0, "b0", "A"), buttonLeaf(1, "b1", "B"), buttonLeaf(2, "b2", "C")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("trailing_loss_expected4_actual012", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityMissingOrdinalFails() {
        TargetNodePayload payload = payload(3, list(buttonLeaf(0, "b0", "A"), buttonLeaf(2, "b2", "C")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("missing_ordinal_1", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityDuplicateOrdinalFails() {
        TargetNodePayload payload = payload(2, list(buttonLeaf(0, "b0", "A"), buttonLeaf(0, "b1", "B")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("duplicate_ordinal", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityNegativeOrdinalFails() {
        TargetNodePayload payload = payload(1, list(buttonLeaf(-1, "b0", "A")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("negative_ordinal", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityOutOfRangeOrdinalFails() {
        TargetNodePayload payload = payload(1, list(buttonLeaf(5, "b0", "A")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("out_of_range_ordinal", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityAbsentExpectedCountFails() {
        TargetNodePayload payload = new TargetNodePayload(KIND, NODE_ID, list(buttonLeaf(0, "b0", "A")), null);
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("absent_expected_count", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testCardinalityNonIntegerOrdinalFails() {
        Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
        structuredData.put("buttonOrdinal", "0");
        TargetLeafPayload malformed = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", structuredData, "source_text_attribute", "b0");
        TargetNodePayload payload = payload(1, list(malformed));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("non_integer_ordinal", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    // ---- identity index 검증 ----

    private static void testIdentityIndexValidExactMapping() {
        TargetNodePayload payload = payload(2, list(
                buttonLeaf(0, "b0", "A"), buttonLeaf(1, "b1", "B"),
                eventLeaf("b0", "onclick", "fn_a")));
        TargetScriptArtifact artifact = artifactWith("fn_a");
        TargetPayloadBehaviorFinalizationResult result = new TargetPayloadBehaviorFinalizer().finalize(payload, artifact);
        assertStatus("identity_index_valid", PayloadBehaviorFinalizationStatus.FINALIZED, result);
    }

    private static void testIdentityIndexDuplicateStructuralIdDifferentOrdinalsFails() {
        // ordinal은 다르지만 source structural id가 동일한 버튼 두 개 -- identity index 구성이 fail-closed 되어야 한다.
        TargetNodePayload payload = payload(2, list(buttonLeaf(0, "bDup", "A"), buttonLeaf(1, "bDup", "B")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("identity_index_duplicate_structural_id", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testIdentityIndexBlankStructuralIdFails() {
        TargetLeafPayload blankIdLeaf = new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, "A", oneEntry("buttonOrdinal", Integer.valueOf(0)),
                "source_text_attribute", "   ");
        TargetNodePayload payload = payload(1, list(blankIdLeaf));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("identity_index_blank_structural_id", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    // ---- events 검증 ----

    private static void testEventlessButtonGroupFinalizes() {
        TargetNodePayload payload = payload(2, list(buttonLeaf(0, "b0", "A"), buttonLeaf(1, "b1", "B")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("eventless_finalizes", PayloadBehaviorFinalizationStatus.FINALIZED, result);
        assertEquals("eventless_finalizes: zero finalized bindings is lawful", "0",
                String.valueOf(countFinalizedBindings(result.getFinalizedPayload())));
    }

    private static void testValidOnclickResolves() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        assertStatus("valid_onclick_resolves", PayloadBehaviorFinalizationStatus.FINALIZED, result);
    }

    private static void testEventRefersToExactButtonStructuralIdentity() {
        TargetNodePayload payload = payload(2, list(
                buttonLeaf(0, "b0", "Save"), buttonLeaf(1, "b1", "Cancel"),
                eventLeaf("b1", "onclick", "fn_cancel")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_cancel"));
        assertStatus("event_exact_structural_identity", PayloadBehaviorFinalizationStatus.FINALIZED, result);
        TargetEventBinding binding = onlyFinalizedBinding(result.getFinalizedPayload());
        assertEquals("event_exact_structural_identity: bound to ordinal 1 (b1), not 0",
                "1", String.valueOf(binding.getButtonOrdinal()));
    }

    private static void testUnmappedEventFailsUnsupportedEventMapping() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "ondblclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        assertStatus("unmapped_event", PayloadBehaviorFinalizationStatus.UNSUPPORTED_EVENT_MAPPING, result);
    }

    /**
     * Slice 99D -- EVENT_NAME_MAPPING은 "onclick" 1건만 존재한다. source가 "onload"(page
     * lifecycle event)를 요청해도 target ev:onpageload로 승격되지 않고 동일하게 fail-closed된다
     * -- 이 finalizer가 accepted path에서 ev:onpageload를 생성할 수 없다는 구조적 증거다.
     */
    private static void testOnloadEventNeverMapsToTargetOnpageload() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onload", "fn_onload")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_onload"));
        assertStatus("onload_event_not_mapped", PayloadBehaviorFinalizationStatus.UNSUPPORTED_EVENT_MAPPING, result);
    }

    private static void testUnresolvedFunctionFails() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_never_translated")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("unresolved_function", PayloadBehaviorFinalizationStatus.UNRESOLVED_FUNCTION_REFERENCE, result);
    }

    private static void testEventRefersToNonexistentButtonFails() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("bGhost", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        assertStatus("event_nonexistent_button", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testDuplicateFinalizedOnclickForSameButtonFails() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"),
                eventLeaf("b0", "onclick", "fn_save"),
                eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        assertStatus("duplicate_finalized_onclick", PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION, result);
    }

    private static void testTwoDifferentButtonsEachHaveOwnOnclick() {
        TargetNodePayload payload = payload(2, list(
                buttonLeaf(0, "b0", "Save"), buttonLeaf(1, "b1", "Cancel"),
                eventLeaf("b0", "onclick", "fn_save"), eventLeaf("b1", "onclick", "fn_cancel")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save", "fn_cancel"));
        assertStatus("two_buttons_own_onclick", PayloadBehaviorFinalizationStatus.FINALIZED, result);
        assertEquals("two_buttons_own_onclick: 2 finalized bindings", "2",
                String.valueOf(countFinalizedBindings(result.getFinalizedPayload())));
    }

    private static void testFinalizedEventHasExactFields() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        TargetEventBinding binding = onlyFinalizedBinding(result.getFinalizedPayload());
        assertEquals("finalized_fields: buttonOrdinal", "0", String.valueOf(binding.getButtonOrdinal()));
        assertEquals("finalized_fields: targetEventLocalName", "onclick", binding.getTargetEventLocalName());
        assertEquals("finalized_fields: targetFunctionIdentifier", "fn_save", binding.getTargetFunctionIdentifier());
    }

    // ---- authority(권한) 검증 ----

    private static void testFinalizedEventLeafContainsBinding() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        boolean found = false;
        for (TargetLeafPayload item : result.getFinalizedPayload().getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) {
                assertTrue("event_leaf_contains_binding: non-null", item.getFinalizedTargetEventBinding() != null);
                found = true;
            }
        }
        assertTrue("event_leaf_contains_binding: event leaf was examined", found);
    }

    private static void testFinalizerOutputPreservesSourceEvidenceAsNonRendererProvenance() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        for (TargetLeafPayload item : result.getFinalizedPayload().getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) {
                assertEquals("source_evidence_preserved: sourceComponentStructuralId still present",
                        "b0", item.getSourceComponentStructuralId());
            }
        }
        pass("source_evidence_preserved: not deleted (finalizer does not strip pre-render provenance)");
    }

    private static void testNoRawFunctionNameCopiedIntoBinding() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save_raw")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save_raw"));
        TargetEventBinding binding = onlyFinalizedBinding(result.getFinalizedPayload());
        // TargetEventBinding은 buttonOrdinal/targetEventLocalName/targetFunctionIdentifier 3개 필드뿐이며
        // 별도 raw functionName 필드는 없다 -- targetFunctionIdentifier가 유일한 함수 식별 필드다.
        assertEquals("no_raw_function_name_field: targetFunctionIdentifier is the sole function field",
                "fn_save_raw", binding.getTargetFunctionIdentifier());
    }

    private static void testNoSourceStructuralIdCopiedIntoBinding() {
        TargetNodePayload payload = payload(1, list(
                buttonLeaf(0, "b0", "Save"), eventLeaf("b0", "onclick", "fn_save")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        TargetEventBinding binding = onlyFinalizedBinding(result.getFinalizedPayload());
        assertFalse("no_source_structural_id_field: binding.toString() must not contain the source structuralId",
                binding.toString().contains("b0"));
    }

    private static void testNoTextNoValueButtonLeafSurvivesFinalization() {
        TargetNodePayload payload = payload(2, list(buttonLeaf(0, "b0", "A"), buttonLeaf(1, "b1", null)));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        assertStatus("no_text_value_survives", PayloadBehaviorFinalizationStatus.FINALIZED, result);
        boolean sawNullValueOrdinal1 = false;
        for (TargetLeafPayload item : result.getFinalizedPayload().getItems()) {
            if (Integer.valueOf(1).equals(item.getStructuredData().get("buttonOrdinal"))) {
                assertTrue("no_text_value_survives: still present with null value", item.getValue() == null);
                sawNullValueOrdinal1 = true;
            }
        }
        assertTrue("no_text_value_survives: finalizer did not remove the no-text/no-value structural leaf",
                sawNullValueOrdinal1);
    }

    // ---- immutability(불변성) 검증 ----

    private static void testInputPayloadNotMutated() {
        TargetLeafPayload eventLeaf = eventLeaf("b0", "onclick", "fn_save");
        TargetNodePayload payload = payload(1, list(buttonLeaf(0, "b0", "Save"), eventLeaf));
        new TargetPayloadBehaviorFinalizer().finalize(payload, artifactWith("fn_save"));
        assertTrue("input_not_mutated: original event leaf still has null finalizedTargetEventBinding",
                eventLeaf.getFinalizedTargetEventBinding() == null);
    }

    private static void testFinalizedOutputCollectionsUnmodifiable() {
        TargetNodePayload payload = payload(1, list(buttonLeaf(0, "b0", "A")));
        TargetPayloadBehaviorFinalizationResult result =
                new TargetPayloadBehaviorFinalizer().finalize(payload, TargetScriptArtifact.empty());
        try {
            result.getFinalizedPayload().getItems().add(buttonLeaf(1, "b1", "B"));
            fail("output_unmodifiable: expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            pass("output_unmodifiable: items list is unmodifiable");
        }
    }

    // ---- fixture 목록 ----

    private static TargetNodePayload payload(int expectedCount, List<TargetLeafPayload> items) {
        return new TargetNodePayload(KIND, NODE_ID, items, Integer.valueOf(expectedCount));
    }

    private static List<TargetLeafPayload> list(TargetLeafPayload... items) {
        return new ArrayList<TargetLeafPayload>(java.util.Arrays.asList(items));
    }

    private static TargetLeafPayload buttonLeaf(int ordinal, String structuralId, String presentationValue) {
        Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
        structuredData.put("buttonOrdinal", Integer.valueOf(ordinal));
        return new TargetLeafPayload(
                TargetPayloadCategory.DISPLAY_TEXT, presentationValue, structuredData,
                "source_text_attribute", structuralId);
    }

    private static TargetLeafPayload eventLeaf(String structuralId, String eventName, String functionName) {
        Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
        structuredData.put("eventName", eventName);
        structuredData.put("functionName", functionName);
        return new TargetLeafPayload(
                TargetPayloadCategory.EVENT, eventName, structuredData, "event_binding", structuralId);
    }

    private static Map<String, Object> oneEntry(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(key, value);
        return m;
    }

    /** identifier마다 최소한의 실제 {@code TargetScwinFunctionModel}(빈 파라미터, 빈 본문)을 만들어
     * {@link TargetScriptArtifact}를 구성한다 -- finalizer는 함수 본문을 읽지 않고 식별자 인덱스만
     * 사용하므로, production과 동일하게 실제 함수 모델로 뒷받침된 식별자만 해석 가능하다. */
    private static TargetScriptArtifact artifactWith(String... identifiers) {
        List<com.example.xfdltracker.behavior.TargetScwinFunctionModel> functions =
                new ArrayList<com.example.xfdltracker.behavior.TargetScwinFunctionModel>();
        for (String identifier : identifiers) {
            functions.add(new com.example.xfdltracker.behavior.TargetScwinFunctionModel(
                    identifier, Collections.<String>emptyList(), ""));
        }
        return new TargetScriptArtifact(functions);
    }

    private static int countFinalizedBindings(TargetNodePayload payload) {
        int count = 0;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getFinalizedTargetEventBinding() != null) {
                count++;
            }
        }
        return count;
    }

    private static TargetEventBinding onlyFinalizedBinding(TargetNodePayload payload) {
        TargetEventBinding found = null;
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getFinalizedTargetEventBinding() != null) {
                if (found != null) {
                    throw new IllegalStateException("more than one finalized binding present");
                }
                found = item.getFinalizedTargetEventBinding();
            }
        }
        return found;
    }

    // ---- assertion 도우미 ----

    private static void assertStatus(
            String label, PayloadBehaviorFinalizationStatus expected, TargetPayloadBehaviorFinalizationResult result) {
        if (result.getStatus() != expected) {
            fail(label + " -- expected status=" + expected + " actual=" + result.getStatus()
                    + " reason=" + result.getFailureReason());
        } else {
            pass(label + " -- status=" + expected);
        }
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(label + " -- expected=<" + expected + "> actual=<" + actual + ">");
        } else {
            pass(label);
        }
    }

    private static void assertTrue(String label, boolean actual) {
        if (!actual) {
            fail(label + " -- expected true");
        } else {
            pass(label);
        }
    }

    private static void assertFalse(String label, boolean actual) {
        if (actual) {
            fail(label + " -- expected false");
        } else {
            pass(label);
        }
    }

    private static void pass(String label) {
        System.out.println("[PASS] " + label);
    }

    private static void fail(String label) {
        System.out.println("[FAIL] " + label);
        failures++;
    }
}
