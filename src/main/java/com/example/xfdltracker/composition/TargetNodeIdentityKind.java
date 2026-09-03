package com.example.xfdltracker.composition;

/**
 * {@link TargetCompositionNode}/{@code TargetNodePayload} 사이 Plan<->Payload correlation authority를
 * 이루는 두 축(kind, value) 중 kind 축의 정확히 두 개 허용 값. {@link CompositionDecision.Origin}에서만
 * 직접 유도하며, {@code nodeId} 문자열 형태(prefix 등)를 검사해서 추론하지 않는다.
 */
public enum TargetNodeIdentityKind {

    /** {@code origin == SOURCE_SEMANTIC}인 node -- identity value는 그 node의 {@code
     * sourceStructuralId}(실제 DOM 위치 기반, target-visible ID 아님). */
    SOURCE_STRUCTURAL,

    /** {@code origin == TARGET_SYNTHETIC}인 node -- identity value는 caller가 명시적으로 부여한
     * {@code targetSyntheticId}를 반영한 이 Plan 안에서만 유효한 identity(target DOM id/semantic authority 아님). */
    TARGET_SYNTHETIC
}
