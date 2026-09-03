package com.example.xfdltracker.payload;

/** {@code TargetPayloadBehaviorFinalizer}가 내리는 판정. 조용히 건너뛰지 않고 항상 명시적 상태로 보고한다. */
public enum PayloadBehaviorFinalizationStatus {

    /** 모든 button cardinality/event correlation/event mapping/function 해석이 성공했다. */
    FINALIZED,

    /** role="event" evidence의 eventName이 유한한 v1 매핑(onclick만)에 속하지 않는다. */
    UNSUPPORTED_EVENT_MAPPING,

    /** eventName은 매핑됐지만 참조된 source functionName이 finalized target identifier index에
     * 존재하지 않는다(아직 번역되지 않았거나 번역에 실패함). */
    UNRESOLVED_FUNCTION_REFERENCE,

    /** button cardinality 위반, 중복/누락 ordinal, structural identity 충돌, event가 존재하지
     * 않는 Button을 참조, 중복 finalized binding key 등 -- Plan/Payload 자신의 무결성 위반. */
    INTEGRITY_VIOLATION
}
