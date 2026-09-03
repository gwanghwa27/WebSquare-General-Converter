package com.example.xfdltracker.runtime;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 대상 런타임 환경에서 명시적으로 사용 가능하다고 선언된 capability ID의 불변 프로필. 기본 사용 가능
 * 상태는 없다 -- {@link #getAvailableCapabilityIds()}에 없는 capability는 사용 불가로 간주되어
 * {@code RuntimeCapabilityResolver}가 fail-closed 처리한다.
 */
public final class TargetRuntimeProfile {

    private final Set<String> availableCapabilityIds;

    public TargetRuntimeProfile(Set<String> availableCapabilityIds) {
        if (availableCapabilityIds == null) {
            throw new IllegalArgumentException("target_runtime_profile: availableCapabilityIds must not be null");
        }
        this.availableCapabilityIds = Collections.unmodifiableSet(new LinkedHashSet<String>(availableCapabilityIds));
    }

    public static TargetRuntimeProfile empty() {
        return new TargetRuntimeProfile(Collections.<String>emptySet());
    }

    public boolean isAvailable(String capabilityId) {
        return availableCapabilityIds.contains(capabilityId);
    }

    public Set<String> getAvailableCapabilityIds() { return availableCapabilityIds; }
}
