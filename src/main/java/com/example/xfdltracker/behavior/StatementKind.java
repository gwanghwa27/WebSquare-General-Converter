package com.example.xfdltracker.behavior;

/**
 * {@link SourceStatementNode}가 표현 가능한 폐쇄형 문장 종류 집합. {@code UNKNOWN} 값은 없으며, 이 집합
 * 밖의 구문은 {@code SourceScriptAnalyzer}가 {@link SourceAnalysisStatus#UNSUPPORTED_SYNTAX}로 거부한다.
 */
public enum StatementKind {
    VARIABLE_DECLARATION,
    IDENTIFIER_ASSIGNMENT,
    RETURN,
    IF_ELSE,
    CLASSIC_FOR,
    WHILE,
    BREAK,
    CONTINUE,
    EXPRESSION_STATEMENT
}
