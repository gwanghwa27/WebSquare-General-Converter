package com.example.xfdltracker.payload;

/**
 * {@link TargetPayloadBehaviorFinalizer#finalize} 결과 pure-data. {@link #getFinalizedPayload()}는
 * {@link PayloadBehaviorFinalizationStatus#FINALIZED}일 때만 non-null이며, 그 외에는 항상 null이다
 * ({@link #getFailureReason()}이 대신 채워진다). 부분 finalize를 성공으로 반환하지 않는다.
 */
public final class TargetPayloadBehaviorFinalizationResult {

    private final PayloadBehaviorFinalizationStatus status;
    private final TargetNodePayload finalizedPayload;
    private final String failureReason;

    private TargetPayloadBehaviorFinalizationResult(
            PayloadBehaviorFinalizationStatus status, TargetNodePayload finalizedPayload, String failureReason) {
        if (status == null) {
            throw new IllegalArgumentException("target_payload_behavior_finalization_result: status must not be null");
        }
        this.status = status;
        this.finalizedPayload = finalizedPayload;
        this.failureReason = failureReason;
    }

    static TargetPayloadBehaviorFinalizationResult finalized(TargetNodePayload finalizedPayload) {
        if (finalizedPayload == null) {
            throw new IllegalArgumentException(
                    "target_payload_behavior_finalization_result: finalizedPayload must not be null for FINALIZED");
        }
        return new TargetPayloadBehaviorFinalizationResult(
                PayloadBehaviorFinalizationStatus.FINALIZED, finalizedPayload, null);
    }

    static TargetPayloadBehaviorFinalizationResult notFinalized(
            PayloadBehaviorFinalizationStatus status, String failureReason) {
        if (status == PayloadBehaviorFinalizationStatus.FINALIZED) {
            throw new IllegalArgumentException(
                    "target_payload_behavior_finalization_result: notFinalized() must not be called with FINALIZED");
        }
        return new TargetPayloadBehaviorFinalizationResult(status, null, failureReason);
    }

    public PayloadBehaviorFinalizationStatus getStatus() { return status; }
    public TargetNodePayload getFinalizedPayload() { return finalizedPayload; }
    public String getFailureReason() { return failureReason; }
}
