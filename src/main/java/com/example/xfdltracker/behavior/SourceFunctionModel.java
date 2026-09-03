package com.example.xfdltracker.behavior;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * 최상위 명명 소스 함수 선언 하나의 불변 구조 모델. 원본 본문 문자열은 의미 근거로 갖지 않으며,
 * {@code bodyStatements}({@link SourceStatementNode} 모델)만이 {@code TargetScriptTranslator}가
 * 읽을 수 있는 표현이다.
 */
public final class SourceFunctionModel {

    private final String declaredName;
    private final List<String> parameterNames;
    private final List<SourceStatementNode> bodyStatements;

    public SourceFunctionModel(String declaredName, List<String> parameterNames, List<SourceStatementNode> bodyStatements) {
        if (declaredName == null || declaredName.trim().length() == 0) {
            throw new IllegalArgumentException("source_function_model: declaredName must not be null/blank");
        }
        if (parameterNames == null) {
            throw new IllegalArgumentException("source_function_model: parameterNames must not be null");
        }
        if (bodyStatements == null) {
            throw new IllegalArgumentException("source_function_model: bodyStatements must not be null");
        }
        for (String parameterName : parameterNames) {
            if (parameterName == null || parameterName.trim().length() == 0) {
                throw new IllegalArgumentException("source_function_model: parameterNames must not contain null/blank");
            }
        }
        this.declaredName = declaredName;
        this.parameterNames = Collections.unmodifiableList(new ArrayList<String>(parameterNames));
        this.bodyStatements = Collections.unmodifiableList(new ArrayList<SourceStatementNode>(bodyStatements));
    }

    public String getDeclaredName() { return declaredName; }
    public List<String> getParameterNames() { return parameterNames; }
    public List<SourceStatementNode> getBodyStatements() { return bodyStatements; }
}
