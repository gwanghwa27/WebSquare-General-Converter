package com.example.xfdltracker.behavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 최종 확정된 불변 아티팩트: 순서가 보존된 {@code List<TargetScwinFunctionModel>}와 여기서 파생된
 * 식별자 인덱스(중복 불가)만 보유한다. {@code TargetScriptTranslator}만 생성한다.
 * 식별자 인덱스는 항상 내부적으로 파생되며 외부에서 별도로 주입될 수 없다.
 */
public final class TargetScriptArtifact {

    private final List<TargetScwinFunctionModel> functionsInOrder;
    private final Set<String> finalizedTargetFunctionIdentifiers;

    /** 유일한 생성 경로 -- 번역에 성공한 {@link SourceFunctionModel}마다 하나씩, 번역 순서대로 담긴다. */
    public TargetScriptArtifact(List<TargetScwinFunctionModel> functionsInOrder) {
        if (functionsInOrder == null) {
            throw new IllegalArgumentException("target_script_artifact: functionsInOrder must not be null");
        }
        Set<String> identifiers = new LinkedHashSet<String>();
        for (TargetScwinFunctionModel function : functionsInOrder) {
            if (function == null) {
                throw new IllegalArgumentException("target_script_artifact: function must not be null");
            }
            if (!identifiers.add(function.getIdentifier())) {
                throw new IllegalArgumentException(
                        "target_script_artifact: duplicate finalized target function identifier -- \""
                                + function.getIdentifier() + "\"");
            }
        }
        this.functionsInOrder = Collections.unmodifiableList(new ArrayList<TargetScwinFunctionModel>(functionsInOrder));
        this.finalizedTargetFunctionIdentifiers = Collections.unmodifiableSet(identifiers);
    }

    public static TargetScriptArtifact empty() {
        return new TargetScriptArtifact(Collections.<TargetScwinFunctionModel>emptyList());
    }

    /** 대상 식별자는 소스 함수 선언명을 그대로 사용하므로, 확정 인덱스에 대한 정확 일치 검사로 충분하다. */
    public boolean containsFinalizedTargetFunctionIdentifier(String targetFunctionIdentifier) {
        return targetFunctionIdentifier != null
                && finalizedTargetFunctionIdentifiers.contains(targetFunctionIdentifier);
    }

    public Set<String> getFinalizedTargetFunctionIdentifiers() { return finalizedTargetFunctionIdentifiers; }

    public List<TargetScwinFunctionModel> getFunctionsInOrder() { return functionsInOrder; }
}
