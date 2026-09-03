package com.example.xfdltracker.payload;

import com.example.xfdltracker.behavior.TargetScriptArtifact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * pre-render payload behavior finalization 전용. {@link TargetNodePayload}와 finalize된
 * {@link TargetScriptArtifact}를 소비해 {@link TargetEventBinding}으로 보강된 결과를 만든다 --
 * source script 파싱/DOM 분석/렌더링은 하지 않으며, 첫 위반에서 즉시 short-circuit한다(partial success 없음).
 */
public final class TargetPayloadBehaviorFinalizer {

    private static final String BUTTON_ORDINAL_KEY = "buttonOrdinal";
    private static final String FUNCTION_NAME_KEY = "functionName";

    /** v1 유한 event-name mapping -- onclick만 지원, 일반화 없음. */
    private static final Map<String, String> EVENT_NAME_MAPPING;
    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("onclick", "onclick");
        EVENT_NAME_MAPPING = m;
    }

    public TargetPayloadBehaviorFinalizationResult finalize(TargetNodePayload payload, TargetScriptArtifact scriptArtifact) {
        if (payload == null) {
            throw new IllegalArgumentException("target_payload_behavior_finalizer: payload must not be null");
        }
        if (scriptArtifact == null) {
            throw new IllegalArgumentException("target_payload_behavior_finalizer: scriptArtifact must not be null");
        }

        // expectedStructuralMemberCount는 TargetNodePayload에서만 읽는다 -- fallback 없음.
        Integer expectedCount = payload.getExpectedStructuralMemberCount();
        if (expectedCount == null) {
            return TargetPayloadBehaviorFinalizationResult.notFinalized(
                    PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                    "expectedStructuralMemberCount is absent (null) -- no fallback to actual leaf count, "
                            + "max(ordinal)+1, event count, componentEvidence, source DOM, or analyzer rerun "
                            + "is permitted");
        }
        if (expectedCount.intValue() < 0) {
            return TargetPayloadBehaviorFinalizationResult.notFinalized(
                    PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                    "expectedStructuralMemberCount is negative: " + expectedCount);
        }
        int expectedButtonCount = expectedCount.intValue();

        // BUTTON_GROUP만 non-null expectedStructuralMemberCount로 이 메서드에 도달하며, 그 DISPLAY_TEXT
        // leaf는 전부 button-role structural leaf다.
        List<TargetLeafPayload> buttonLeaves = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.DISPLAY_TEXT) {
                buttonLeaves.add(item);
            }
        }
        if (buttonLeaves.size() != expectedButtonCount) {
            return TargetPayloadBehaviorFinalizationResult.notFinalized(
                    PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                    "structural button leaf count (" + buttonLeaves.size() + ") != expectedStructuralMemberCount ("
                            + expectedButtonCount + ")");
        }

        // ordinal 집합은 정확히 {0..expectedButtonCount-1}이어야 한다 -- 누락/중복/음수/범위초과 모두 fail-closed.
        Map<Integer, TargetLeafPayload> leafByOrdinal = new LinkedHashMap<Integer, TargetLeafPayload>();
        for (TargetLeafPayload leaf : buttonLeaves) {
            Object ordinalObj = leaf.getStructuredData().get(BUTTON_ORDINAL_KEY);
            if (ordinalObj == null) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "structural button leaf is missing structuredData[\"buttonOrdinal\"]");
            }
            if (!(ordinalObj instanceof Integer)) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "structural button leaf buttonOrdinal is not an Integer: " + ordinalObj.getClass());
            }
            int ordinal = ((Integer) ordinalObj).intValue();
            if (ordinal < 0) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "structural button leaf buttonOrdinal is negative: " + ordinal);
            }
            if (ordinal >= expectedButtonCount) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "structural button leaf buttonOrdinal=" + ordinal + " >= expectedButtonCount="
                                + expectedButtonCount);
            }
            if (leafByOrdinal.containsKey(Integer.valueOf(ordinal))) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "duplicate buttonOrdinal=" + ordinal);
            }
            leafByOrdinal.put(Integer.valueOf(ordinal), leaf);
        }
        for (int i = 0; i < expectedButtonCount; i++) {
            if (!leafByOrdinal.containsKey(Integer.valueOf(i))) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "missing buttonOrdinal=" + i + " (expected set 0.." + (expectedButtonCount - 1)
                                + " is not fully represented -- possible trailing Button loss)");
            }
        }

        // cardinality 검증 통과 후에만 structuralId -> buttonOrdinal index를 만든다(유일성 검증 포함).
        Map<String, Integer> structuralIdToOrdinal = new LinkedHashMap<String, Integer>();
        for (Map.Entry<Integer, TargetLeafPayload> entry : leafByOrdinal.entrySet()) {
            String structuralId = entry.getValue().getSourceComponentStructuralId();
            if (structuralId == null || structuralId.trim().length() == 0) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "structural button leaf at ordinal=" + entry.getKey()
                                + " has null/blank sourceComponentStructuralId");
            }
            Integer existing = structuralIdToOrdinal.get(structuralId);
            if (existing != null && !existing.equals(entry.getKey())) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "source structural identity " + structuralId + " maps to multiple ordinals ("
                                + existing + " and " + entry.getKey() + ")");
            }
            structuralIdToOrdinal.put(structuralId, entry.getKey());
        }

        // event leaf를 identity index -> event-name mapping -> finalized target function 순서로 resolve한다.
        List<TargetLeafPayload> eventLeaves = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : payload.getItems()) {
            if (item.getCategory() == TargetPayloadCategory.EVENT) {
                eventLeaves.add(item);
            }
        }

        Map<TargetLeafPayload, TargetEventBinding> finalizedByLeaf =
                new LinkedHashMap<TargetLeafPayload, TargetEventBinding>();
        Set<String> uniquenessKeys = new LinkedHashSet<String>();
        for (TargetLeafPayload eventLeaf : eventLeaves) {
            String structuralId = eventLeaf.getSourceComponentStructuralId();
            if (structuralId == null || structuralId.trim().length() == 0) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "event evidence leaf has null/blank sourceComponentStructuralId");
            }
            Integer buttonOrdinal = structuralIdToOrdinal.get(structuralId);
            if (buttonOrdinal == null) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "event evidence sourceComponentStructuralId=" + structuralId
                                + " does not exact-match any validated button identity index entry "
                                + "(references a nonexistent/non-represented Button)");
            }

            String sourceEventName = eventLeaf.getValue();
            String targetEventLocalName = EVENT_NAME_MAPPING.get(sourceEventName);
            if (targetEventLocalName == null) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.UNSUPPORTED_EVENT_MAPPING,
                        "eventName=\"" + sourceEventName + "\" is not in the finite v1 event mapping "
                                + "(only onclick is supported)");
            }

            String sourceFunctionName = (String) eventLeaf.getStructuredData().get(FUNCTION_NAME_KEY);
            if (sourceFunctionName == null || sourceFunctionName.trim().length() == 0
                    || !scriptArtifact.containsFinalizedTargetFunctionIdentifier(sourceFunctionName)) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.UNRESOLVED_FUNCTION_REFERENCE,
                        "source functionName=\"" + sourceFunctionName + "\" does not resolve to a finalized "
                                + "target function identifier in the given TargetScriptArtifact");
            }
            String targetFunctionIdentifier = sourceFunctionName;

            String uniquenessKey = buttonOrdinal + ":" + targetEventLocalName;
            if (!uniquenessKeys.add(uniquenessKey)) {
                return TargetPayloadBehaviorFinalizationResult.notFinalized(
                        PayloadBehaviorFinalizationStatus.INTEGRITY_VIOLATION,
                        "duplicate finalized event binding key (buttonOrdinal=" + buttonOrdinal
                                + ", targetEventLocalName=\"" + targetEventLocalName + "\")");
            }

            finalizedByLeaf.put(eventLeaf, new TargetEventBinding(
                    buttonOrdinal.intValue(), targetEventLocalName, targetFunctionIdentifier));
        }

        // event leaf는 finalized binding을 담은 불변 복사본으로, 나머지 leaf는 그대로 통과시킨다.
        List<TargetLeafPayload> finalizedItems = new ArrayList<TargetLeafPayload>();
        for (TargetLeafPayload item : payload.getItems()) {
            TargetEventBinding binding = finalizedByLeaf.get(item);
            if (binding != null) {
                finalizedItems.add(new TargetLeafPayload(
                        item.getCategory(), item.getValue(), item.getStructuredData(),
                        item.getSourceEvidenceKind(), item.getSourceComponentStructuralId(), binding));
            } else {
                finalizedItems.add(item);
            }
        }
        TargetNodePayload finalizedPayload = new TargetNodePayload(
                payload.getIdentityKind(), payload.getPlanNodeId(), finalizedItems, expectedCount);
        return TargetPayloadBehaviorFinalizationResult.finalized(finalizedPayload);
    }
}
