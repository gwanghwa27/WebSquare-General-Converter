package com.example.xfdltracker.behavior;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 소스 스크립트 분석 성공 결과의 불변 표현: 최상위 명명 함수 선언 인덱스를 선언 순서대로 보유한다.
 * 선언 유일성은 이 클래스가 직접 소유하며, 별도 SymbolTable은 두지 않는다. DOM/런타임 리졸버 참조는
 * 갖지 않는다.
 */
public final class SourceScriptAnalysis {

    private final List<SourceFunctionModel> functionsInDeclarationOrder;
    private final Map<String, SourceFunctionModel> functionsByDeclaredName;

    public SourceScriptAnalysis(List<SourceFunctionModel> functionsInDeclarationOrder) {
        if (functionsInDeclarationOrder == null) {
            throw new IllegalArgumentException("source_script_analysis: functionsInDeclarationOrder must not be null");
        }
        Map<String, SourceFunctionModel> byName = new LinkedHashMap<String, SourceFunctionModel>();
        for (SourceFunctionModel function : functionsInDeclarationOrder) {
            if (function == null) {
                throw new IllegalArgumentException("source_script_analysis: function must not be null");
            }
            if (byName.containsKey(function.getDeclaredName())) {
                throw new IllegalStateException(
                        "source_script_analysis: duplicate top-level named function declaration -- \""
                                + function.getDeclaredName() + "\" (INTEGRITY_VIOLATION)");
            }
            byName.put(function.getDeclaredName(), function);
        }
        this.functionsInDeclarationOrder = Collections.unmodifiableList(
                new ArrayList<SourceFunctionModel>(functionsInDeclarationOrder));
        this.functionsByDeclaredName = Collections.unmodifiableMap(byName);
    }

    public List<SourceFunctionModel> getFunctionsInDeclarationOrder() { return functionsInDeclarationOrder; }

    public boolean hasDeclaredFunction(String declaredName) {
        return declaredName != null && functionsByDeclaredName.containsKey(declaredName);
    }

    public SourceFunctionModel getDeclaredFunction(String declaredName) {
        return declaredName == null ? null : functionsByDeclaredName.get(declaredName);
    }
}
