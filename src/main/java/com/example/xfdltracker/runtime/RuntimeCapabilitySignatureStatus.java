package com.example.xfdltracker.runtime;

/**
 * 근거 자료만으로는 완전한 시그니처를 확정할 수 없으므로 시드 항목은 모두 {@link #UNCONFIRMED}를
 * 사용한다 -- 필드가 조용히 "확정" 상태로 기본값을 갖지 않도록 존재하는 열거형.
 */
public enum RuntimeCapabilitySignatureStatus {
    UNCONFIRMED,
    CONFIRMED
}
