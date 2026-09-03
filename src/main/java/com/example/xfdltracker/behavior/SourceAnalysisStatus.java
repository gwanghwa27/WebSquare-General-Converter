package com.example.xfdltracker.behavior;

/**
 * {@code SourceScriptAnalyzer}의 정확한 closed 결과 상태 집합이다.
 * 추가적인 성공/부분 성공 상태는 없다.
 */
public enum SourceAnalysisStatus {
    ANALYZED,
    UNSUPPORTED_SYNTAX,
    INTEGRITY_VIOLATION
}
