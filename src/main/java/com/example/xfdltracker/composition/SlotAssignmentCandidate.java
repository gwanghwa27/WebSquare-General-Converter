package com.example.xfdltracker.composition;

/**
 * {@link SemanticRegionGraph}의 실제 DOM 관계와 {@link CompositionRuleCatalog}의 SLOT_FILL
 * 규칙이 동시에 성립할 때만 만들어지는 미확정 제안. {@link SlotAssignment}(catalog 검증을 통과해
 * 확정된 배정)과 달리 {@code assignSlot}이 전혀 호출되지 않으며, 여러 candidate가 동시에 나와도 자동 선택하지 않는다.
 */
public class SlotAssignmentCandidate {

    private final String parentStructuralId;
    private final String childStructuralId;
    private final String parentFamily;
    private final String childFamily;
    private final String slot;
    private final String compositionRuleId;
    private final SemanticRegionRelationship.RelationshipType sourceRelationshipType;
    private final String evidence;
    private final Integer splitColumnOrderRank;

    public SlotAssignmentCandidate(
            String parentStructuralId, String childStructuralId, String parentFamily, String childFamily,
            String slot, String compositionRuleId,
            SemanticRegionRelationship.RelationshipType sourceRelationshipType, String evidence) {
        this(parentStructuralId, childStructuralId, parentFamily, childFamily, slot, compositionRuleId,
                sourceRelationshipType, evidence, null);
    }

    /**
     * {@code splitColumnOrderRank} 전용 overload(재계산 없이 그대로 옮김). SPLIT_LAYOUT.columns가
     * 아니면 항상 null이다.
     */
    public SlotAssignmentCandidate(
            String parentStructuralId, String childStructuralId, String parentFamily, String childFamily,
            String slot, String compositionRuleId,
            SemanticRegionRelationship.RelationshipType sourceRelationshipType, String evidence,
            Integer splitColumnOrderRank) {
        this.parentStructuralId = parentStructuralId;
        this.childStructuralId = childStructuralId;
        this.parentFamily = parentFamily;
        this.childFamily = childFamily;
        this.slot = slot;
        this.compositionRuleId = compositionRuleId;
        this.sourceRelationshipType = sourceRelationshipType;
        this.evidence = evidence;
        this.splitColumnOrderRank = splitColumnOrderRank;
    }

    public String getParentStructuralId() { return parentStructuralId; }
    public String getChildStructuralId() { return childStructuralId; }
    public String getParentFamily() { return parentFamily; }
    public String getChildFamily() { return childFamily; }
    public String getSlot() { return slot; }
    public String getCompositionRuleId() { return compositionRuleId; }
    public SemanticRegionRelationship.RelationshipType getSourceRelationshipType() { return sourceRelationshipType; }
    public String getEvidence() { return evidence; }

    /** SPLIT_LAYOUT.columns 후보에서만 non-null(accepted geometry order evidence가 있을 때). */
    public Integer getSplitColumnOrderRank() { return splitColumnOrderRank; }
}
