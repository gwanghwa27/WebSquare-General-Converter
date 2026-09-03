package com.example.xfdltracker.behavior;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * {@code TargetScriptTranslator}만 생성하는 불변 대상측 함수 모델. {@code identifier}는 소스 선언명을
 * 그대로 사용한다. {@code finalizedBodySource}는 번역된 대상 문장 텍스트이며 원본 소스 부분 문자열이
 * 아니고, 발행 후 변경되지 않는다.
 */
public final class TargetScwinFunctionModel {

    private final String identifier;
    private final List<String> parameters;
    private final String finalizedBodySource;

    public TargetScwinFunctionModel(String identifier, List<String> parameters, String finalizedBodySource) {
        if (identifier == null || identifier.trim().length() == 0) {
            throw new IllegalArgumentException("target_scwin_function_model: identifier must not be null/blank");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("target_scwin_function_model: parameters must not be null");
        }
        if (finalizedBodySource == null) {
            throw new IllegalArgumentException("target_scwin_function_model: finalizedBodySource must not be null");
        }
        this.identifier = identifier;
        this.parameters = Collections.unmodifiableList(new ArrayList<String>(parameters));
        this.finalizedBodySource = finalizedBodySource;
    }

    public String getIdentifier() { return identifier; }
    public List<String> getParameters() { return parameters; }
    public String getFinalizedBodySource() { return finalizedBodySource; }
}
