package com.example.xfdltracker.runtime;

/**
 * v1에서는 어떤 capability도 대상 런타임 리소스에 바인딩되지 않으므로, 시드 카탈로그 항목은 모두
 * {@link #UNBOUND}를 사용한다.
 */
public enum RuntimeCapabilityTargetBindingStatus {
    UNBOUND,
    BOUND
}
