package com.example.xfdltracker.runtime;

/** 스크린샷 증거만으로는 호출 규약을 알 수 없으므로, 모든 seed 항목은 {@link #UNKNOWN}을 사용한다. */
public enum RuntimeCapabilityAsyncModel {
    UNKNOWN,
    SYNCHRONOUS,
    ASYNCHRONOUS
}
