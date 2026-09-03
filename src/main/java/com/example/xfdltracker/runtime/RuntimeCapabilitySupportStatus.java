package com.example.xfdltracker.runtime;

/** 기능별로 확인된 지원 상태를 나타내는 정확한 집합이며, 추가되는 값은 없다. */
public enum RuntimeCapabilitySupportStatus {
    DOCUMENTED_NAME_ONLY,
    SIGNATURE_CONFIRMED,
    IMPLEMENTABLE,
    EXTERNAL_SERVICE_REQUIRED,
    ENVIRONMENT_SPECIFIC,
    UNSUPPORTED
}
