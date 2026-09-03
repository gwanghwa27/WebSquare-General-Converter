package com.example.xfdltracker.renderer;

/** {@link AtomicWebSquareRenderer}가 각 node에 대해 내리는 판정. 미지원/위반을 조용히 건너뛰지 않고 항상 명시적으로 보고한다. */
public enum RenderStatus {

    /** 실제로 target DOM fragment를 만들었다. */
    RENDERED,

    /** 이 node의 family가 renderer 지원 범위 밖이다. */
    UNSUPPORTED_FAMILY,

    /** family는 지원되지만 variant가 이 renderer가 아는 것이 아니다. */
    UNSUPPORTED_VARIANT,

    /** Plan node와 Target Payload 사이의 identity/cardinality/leaf-category 계약을 위반한
     * 입력 -- source를 재분석해 보완하지 않고 fail-closed 처리한다. */
    INTEGRITY_VIOLATION
}
