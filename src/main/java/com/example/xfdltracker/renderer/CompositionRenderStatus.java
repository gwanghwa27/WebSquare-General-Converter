package com.example.xfdltracker.renderer;

/**
 * {@link CompositionRenderer}가 {@code TargetCompositionPlan}의 각 node에 대해 내리는 판정.
 * {@link AtomicWebSquareRenderer}의 {@link RenderStatus}와 같은 계약(항상 명시적 상태, 조용한 skip 없음)을
 * composition 단계에도 그대로 적용한다 -- fail-open default 없음.
 */
public enum CompositionRenderStatus {

    /** 이 node 자신의 atomic fragment + 모든 approved 자식이 정상적으로 조립됐다. */
    RENDERED,

    /** 이 node의 {@link AtomicRenderResult}가 {@link RenderStatus#RENDERED}가 아니다(atomic
     * 단계에서 이미 unsupported/integrity-violation으로 판정됨) -- composition은 그 판정을
     * 그대로 승계할 뿐, source를 다시 분석해 보완하지 않는다. */
    ATOMIC_RENDER_UNAVAILABLE,

    /** 이 node 자신의 atomic fragment는 정상이지만, Plan edge로 연결된 자식 중 하나가 composed
     * 되지 못했다(재귀적으로 그 자식의 실제 실패 상태를 아우른다) -- 조용히 그 자식을 빼고
     * 조립하지 않는다. */
    CHILD_COMPOSITION_FAILED,

    /** {@code (parentFamily, slot)} 조합에 대해 이 renderer가 아는 attachment 규칙이 없다 -- catalog/Plan edge는
     * 실재하지만 exact target contract를 아직 구현하지 않았다는 뜻이다. wrapper/class를 즉석 발명해 채우지 않는다. */
    UNSUPPORTED_SLOT_ATTACHMENT,

    /** Plan과 {@link AtomicRenderResult} 목록 사이의 identity 정합성 위반(예: Plan node에 대응하는
     * atomic 결과가 아예 없음, 또는 그 반대) -- stale/tampered 입력을 조용히 무시하지 않는다. */
    INTEGRITY_VIOLATION
}
