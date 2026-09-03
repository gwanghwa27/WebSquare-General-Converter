package com.example.xfdltracker.payload;

import com.example.xfdltracker.composition.TargetNodeIdentityKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 하나의 {@code TargetCompositionNode}에 딸린 STRUCTURED_AVAILABLE leaf payload 묶음. Plan/Payload
 * correlation authority는 정확히 {@code (IDENTITY_KIND, IDENTITY_VALUE)} tuple이다(join key는 이
 * 값 하나 -- {@code sourceRegionId}는 쓰지 않음). {@code identityKind}가 null이면 즉시 거부한다.
 */
public final class TargetNodePayload {

    private final String planNodeId;
    private final TargetNodeIdentityKind identityKind;
    private final List<TargetLeafPayload> items;
    private final Integer expectedStructuralMemberCount;

    public TargetNodePayload(TargetNodeIdentityKind identityKind, String planNodeId, List<TargetLeafPayload> items) {
        this(identityKind, planNodeId, items, null);
    }

    /**
     * {@code expectedStructuralMemberCount}는 BUTTON_GROUP node에서만 non-null이며(다른 family는
     * null) target XML에는 emit되지 않는다 -- {@code TargetPayloadBehaviorFinalizer}가 이 값으로
     * cardinality를 검증한다.
     */
    public TargetNodePayload(
            TargetNodeIdentityKind identityKind, String planNodeId, List<TargetLeafPayload> items,
            Integer expectedStructuralMemberCount) {
        if (planNodeId == null || planNodeId.trim().length() == 0) {
            throw new IllegalArgumentException("target_node_payload: planNodeId must not be null/blank");
        }
        if (identityKind == null) {
            throw new IllegalArgumentException(
                    "target_node_payload: identityKind must not be null -- every Payload envelope must carry "
                            + "an explicit (IDENTITY_KIND, IDENTITY_VALUE) tuple, never a value-only identity");
        }
        if (expectedStructuralMemberCount != null && expectedStructuralMemberCount.intValue() < 0) {
            throw new IllegalArgumentException(
                    "target_node_payload: expectedStructuralMemberCount must not be negative, but was "
                            + expectedStructuralMemberCount);
        }
        this.identityKind = identityKind;
        this.planNodeId = planNodeId;
        this.items = Collections.unmodifiableList(
                new ArrayList<TargetLeafPayload>(items == null ? Collections.<TargetLeafPayload>emptyList() : items));
        this.expectedStructuralMemberCount = expectedStructuralMemberCount;
    }

    public String getPlanNodeId() { return planNodeId; }

    /** identity tuple의 kind 축 -- 생성자가 null을 거부하므로 항상 non-null. */
    public TargetNodeIdentityKind getIdentityKind() { return identityKind; }

    /** identity tuple의 value 축. {@link #getPlanNodeId()}와 동일 값을 의도 명시용으로 별도 노출한다. */
    public String getIdentityValue() { return planNodeId; }

    public List<TargetLeafPayload> getItems() { return items; }

    /** 4-arg 생성자 참고. non-BUTTON_GROUP node는 항상 null. */
    public Integer getExpectedStructuralMemberCount() { return expectedStructuralMemberCount; }
}
