package com.example.xfdltracker.composition;

/**
 * {@link TargetCompositionPlan}의 pure-data edge. {@link CompositionEvaluator#assignSlot}을 통해
 * 검증되고 실제로 쌓인 {@link SlotAssignment}만이 이 edge의 근거가 된다.
 */
public class TargetCompositionEdge {

    private final TargetCompositionNode parent;
    private final String slot;
    private final TargetCompositionNode child;
    private final Integer pageOrdinal;

    TargetCompositionEdge(TargetCompositionNode parent, String slot, TargetCompositionNode child) {
        this(parent, slot, child, null);
    }

    /**
     * {@code pageOrdinal}은 TAB_CONTROL.panes edge에서만 non-null(&gt;=0)이며, 그 외에는 항상
     * null이다. {@link TargetCompositionPlanBuilder}가 exact parent-membership 검증을 통과한 뒤에만 채운다.
     */
    TargetCompositionEdge(TargetCompositionNode parent, String slot, TargetCompositionNode child, Integer pageOrdinal) {
        if (pageOrdinal != null && pageOrdinal.intValue() < 0) {
            throw new IllegalArgumentException(
                    "target_composition_edge: pageOrdinal must be >= 0, but was " + pageOrdinal);
        }
        this.parent = parent;
        this.slot = slot;
        this.child = child;
        this.pageOrdinal = pageOrdinal;
    }

    public TargetCompositionNode getParent() { return parent; }
    public String getSlot() { return slot; }
    public TargetCompositionNode getChild() { return child; }

    /** TAB_CONTROL.panes edge를 제외하면 항상 null. */
    public Integer getPageOrdinal() { return pageOrdinal; }
}
