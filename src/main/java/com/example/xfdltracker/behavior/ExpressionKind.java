package com.example.xfdltracker.behavior;

/**
 * {@link SourceExpressionNode}가 표현 가능한 폐쇄형 표현식 종류 집합. {@code UNKNOWN} 값은 없으며, 이
 * 집합 밖의 구문은 {@code SourceScriptAnalyzer}가 {@link SourceAnalysisStatus#UNSUPPORTED_SYNTAX}로
 * 거부한다.
 */
public enum ExpressionKind {
    PRIMITIVE_LITERAL,
    IDENTIFIER_REFERENCE,
    LOCAL_FUNCTION_CALL,
    UNARY,
    BINARY,
    LOGICAL,
    CONDITIONAL,
    ALLOWED_MATH_CALL
}
