package com.example.xfdltracker.pipeline;

import com.example.xfdltracker.runtime.TargetRuntimeProfile;

/**
 * {@link TargetWebSquarePipeline#convert}용 불변 설정. 필드는 {@code runtimeProfile} 하나뿐이며,
 * 호출자가 반드시 제공해야 한다 -- 기본값을 만들어 채우지 않는다. 필드 추가는 별도 아키텍처 결정 필요.
 */
public final class TargetPipelineConfig {

    private final TargetRuntimeProfile runtimeProfile;

    public TargetPipelineConfig(TargetRuntimeProfile runtimeProfile) {
        if (runtimeProfile == null) {
            throw new IllegalArgumentException("target_pipeline_config: runtimeProfile must not be null");
        }
        this.runtimeProfile = runtimeProfile;
    }

    public TargetRuntimeProfile getRuntimeProfile() { return runtimeProfile; }
}
