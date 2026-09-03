package com.example.xfdltracker.analyzer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * family+variant 조합에 대해 {@link SemanticRegionSegmenter}가 실제로 emit 가능한 variant인지만
 * 답하는 pure contract(catalog validity는 다루지 않음 -- catalog에는 있지만 source가 emit하지
 * 않는 variant도 존재할 수 있다). 목록에 없는 family는 항상 {@code false}를 반환한다.
 */
public final class SourcePredicateVariantContract {

    private static final Map<String, Set<String>> SOURCE_EMITTABLE_VARIANTS_BY_FAMILY;
    static {
        Map<String, Set<String>> m = new LinkedHashMap<String, Set<String>>();
        m.put("SEARCH_AREA", unmodifiableSet("basic"));
        m.put("TITLE_BAR", unmodifiableSet("title_only"));
        m.put("TAB_CONTROL", unmodifiableSet("basic"));
        m.put("BUSINESS_TABLE", unmodifiableSet("horizontal"));
        m.put("BUTTON_GROUP", unmodifiableSet("standalone", "title_bar_attached"));
        SOURCE_EMITTABLE_VARIANTS_BY_FAMILY = Collections.unmodifiableMap(m);
    }

    private static Set<String> unmodifiableSet(String... values) {
        Set<String> s = new LinkedHashSet<String>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    private SourcePredicateVariantContract() {
    }

    /** {@code variant}가 {@code null}이면 항상 {@code false}(모든 evidence-backed family는
     *  항상 explicit non-null variant를 만들므로 null wildcard를 허용하지 않는다). */
    public static boolean isSourceEmittable(String family, String variant) {
        if (variant == null) {
            return false;
        }
        Set<String> allowed = SOURCE_EMITTABLE_VARIANTS_BY_FAMILY.get(family);
        return allowed != null && allowed.contains(variant);
    }
}
