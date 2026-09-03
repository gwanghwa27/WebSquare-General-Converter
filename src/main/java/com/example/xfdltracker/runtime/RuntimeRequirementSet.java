package com.example.xfdltracker.runtime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 소스 화면에서 발견된 정규 capability ID의 불변 집합. Set 의미론으로 중복은 자동 축소되며, 이 클래스
 * 자체는 소스 해석을 하지 않고 추출 단계가 제공한 값을 저장만 한다.
 */
public final class RuntimeRequirementSet {

    private final Set<String> requiredCapabilityIds;

    public RuntimeRequirementSet(Set<String> requiredCapabilityIds) {
        if (requiredCapabilityIds == null) {
            throw new IllegalArgumentException("runtime_requirement_set: requiredCapabilityIds must not be null");
        }
        this.requiredCapabilityIds = Collections.unmodifiableSet(new LinkedHashSet<String>(requiredCapabilityIds));
    }

    public static RuntimeRequirementSet empty() {
        return new RuntimeRequirementSet(Collections.<String>emptySet());
    }

    public Set<String> getRequiredCapabilityIds() { return requiredCapabilityIds; }

    public boolean isEmpty() { return requiredCapabilityIds.isEmpty(); }
}
