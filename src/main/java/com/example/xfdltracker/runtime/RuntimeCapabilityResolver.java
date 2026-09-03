package com.example.xfdltracker.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RuntimeRequirementSet}을 {@link TargetRuntimeProfile}과 정규 카탈로그에 대해 fail-closed로
 * 검증한다 (문서 조립 이전 수행). 이 클래스는 {@code uc.*} 함수를 실행하지 않으며, 요구 capability
 * 집합이 문서 조립으로 진행 가능한지만 판단한다.
 */
public final class RuntimeCapabilityResolver {

    /**
     * @throws RuntimeCapabilityUnavailableException capabilityId가 카탈로그에 없거나 UNSUPPORTED이거나
     *         profile에 명시적으로 선언되지 않은 경우 fail-closed -- 기본 사용 가능 capability는 없음.
     */
    public void validate(RuntimeRequirementSet requirements, TargetRuntimeProfile profile,
            CommonRuntimeCapabilityCatalog catalog) {
        if (requirements == null || profile == null || catalog == null) {
            throw new IllegalArgumentException(
                    "runtime_capability_resolver: requirements/profile/catalog must not be null");
        }
        List<String> failures = new ArrayList<String>();
        for (String capabilityId : requirements.getRequiredCapabilityIds()) {
            CommonRuntimeCapabilityDefinition def = catalog.get(capabilityId);
            if (def == null) {
                failures.add(capabilityId + ":UNKNOWN_TO_CANONICAL_CATALOG");
                continue;
            }
            if (def.getSupportStatus() == RuntimeCapabilitySupportStatus.UNSUPPORTED) {
                failures.add(capabilityId + ":UNSUPPORTED");
                continue;
            }
            if (!profile.isAvailable(capabilityId)) {
                failures.add(capabilityId + ":NOT_AVAILABLE_IN_TARGET_RUNTIME_PROFILE");
            }
        }
        if (!failures.isEmpty()) {
            throw new RuntimeCapabilityUnavailableException(failures);
        }
    }

    public static final class RuntimeCapabilityUnavailableException extends RuntimeException {
        private final List<String> failedCapabilityIds;

        RuntimeCapabilityUnavailableException(List<String> failedCapabilityIds) {
            super("runtime_capability_resolver: fail-closed, unavailable/unsupported/unknown capabilities="
                    + failedCapabilityIds);
            this.failedCapabilityIds = failedCapabilityIds;
        }

        public List<String> getFailedCapabilityIds() { return failedCapabilityIds; }
    }
}
