package com.example.xfdltracker.composition;

/**
 * 검증을 통과해 부모 {@link CompositionDecision}의 특정 slot에 실제로 배정된 자식 하나를 표현하는
 * pure-data 모델. {@link CompositionEvaluator#assignSlot}을 통해서만 생성된다.
 */
public class SlotAssignment {

    private final String slot;
    private final CompositionDecision child;
    private final Integer splitColumnOrderRank;

    public SlotAssignment(String slot, CompositionDecision child) {
        this(slot, child, null);
    }

    /**
     * {@code splitColumnOrderRank} 전용 overload. SPLIT_LAYOUT.columns의 accepted geometry order
     * evidence를 그대로 실어 나르는 evidence carrier일 뿐이다(새 authority 아님).
     */
    public SlotAssignment(String slot, CompositionDecision child, Integer splitColumnOrderRank) {
        this.slot = slot;
        this.child = child;
        this.splitColumnOrderRank = splitColumnOrderRank;
    }

    public String getSlot() { return slot; }
    public CompositionDecision getChild() { return child; }

    /** SPLIT_LAYOUT.columns approved assignment에서만 non-null. */
    public Integer getSplitColumnOrderRank() { return splitColumnOrderRank; }
}
