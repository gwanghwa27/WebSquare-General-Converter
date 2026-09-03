package com.example.xfdltracker.behavior;

/**
 * {@code TargetScriptTranslator}의 정확한 closed 결과 상태 집합이다.
 * 추가되는 값은 없다.
 */
public enum TargetTranslationStatus {
    TRANSLATED,
    UNSUPPORTED_SYNTAX,
    UNSUPPORTED_COMPONENT_API,
    UNRESOLVED_FUNCTION_REFERENCE,
    RUNTIME_CAPABILITY_UNAVAILABLE,
    INTEGRITY_VIOLATION
}
