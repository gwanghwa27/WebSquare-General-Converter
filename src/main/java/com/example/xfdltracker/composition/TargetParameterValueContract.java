package com.example.xfdltracker.composition;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link TemplateFamilyCatalog}는 parameter 이름만 알 뿐 값의 도메인이나 origin별 투영 가능성은 모른다.
 * {@link #isKnownDomainViolation}(target 값 도메인 위반)과 {@link #isNotProjectableFromSourceSemantic}(SOURCE_SEMANTIC
 * 투영 가능 여부)을 분리해서 답한다 -- 합치면 잘못된 결론에 이른다. 알려진 조합 없으면 둘 다 항상 false.
 */
public final class TargetParameterValueContract {

    private static final Set<String> BUTTON_GROUP_POSITION_DOMAIN = unmodifiableSet("top", "bottom");

    private static Set<String> unmodifiableSet(String... values) {
        Set<String> s = new LinkedHashSet<String>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    private TargetParameterValueContract() {
    }

    /**
     * {@code family}+{@code key} 조합에 알려진 target 값 도메인이 있고 {@code value}가 그
     * 도메인 밖(또는 {@code null}/문자열이 아님)이면 {@code true}. 알려진 도메인이 없는
     * 조합은 항상 {@code false}(이 계약이 관여하지 않는 조합 -- 새 schema를 발명하지 않는다).
     */
    public static boolean isKnownDomainViolation(String family, String key, Object value) {
        if ("BUTTON_GROUP".equals(family) && "position".equals(key)) {
            return !(value instanceof String) || !BUTTON_GROUP_POSITION_DOMAIN.contains(value);
        }
        return false;
    }

    /**
     * {@code family}+{@code key} 조합이 SOURCE_SEMANTIC origin에서는 값과 무관하게 정당하게
     * 투영될 수 없는 것으로 알려져 있으면 true. TARGET_SYNTHETIC 등 다른 origin에는 적용하지 않는다.
     */
    public static boolean isNotProjectableFromSourceSemantic(String family, String key) {
        return "BUTTON_GROUP".equals(family) && "position".equals(key);
    }
}
