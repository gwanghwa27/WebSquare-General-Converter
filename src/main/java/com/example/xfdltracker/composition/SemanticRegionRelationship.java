package com.example.xfdltracker.composition;

/**
 * 이미 발행된 두 region 사이의, 실제 source DOM에서 확정적으로 계산된 구조적 관계 하나를
 * 표현한다. endpoint는 {@link SemanticRegionGraphNode#getStructuralId()}만 담는다(sourceRegionId는
 * non-Div scope에서 globally unique하지 않아 쓰지 않음).
 */
public class SemanticRegionRelationship {

    /**
     * DOM에서 확정적으로 계산 가능한 관계만 존재한다(geometry/proximity 기반 관계나
     * {@code structuralId}/{@code sourceRegionId} prefix 추론 기반 관계는 없음).
     */
    public enum RelationshipType {
        /** {@code to}의 anchor Element가 {@code from}의 anchor Element의 **직계** DOM 자식
         * (중간 Element가 하나도 없음). {@code from}=parent, {@code to}=child. */
        DIRECT_PARENT,
        /** {@code to}의 anchor Element가 {@code from}의 anchor Element의 (직계가 아닌) 조상-자손
         * 관계. {@code from}=ancestor, {@code to}=descendant. */
        ANCESTOR_CONTAINS,
        /** 두 region의 anchor Element가 동일한 DOM 부모 아래 직계 형제 관계에 있다.
         * {@code from}은 DOM 순서상 먼저 오는 region(earlier)이고, {@code to}는
         * 나중에 오는 region(later)이다. */
        DIRECT_SIBLING_ORDER
    }

    private final String fromStructuralId;
    private final String toStructuralId;
    private final RelationshipType relationshipType;
    private final Integer sourceOrder;
    private final String evidence;
    private final Integer splitColumnOrderRank;

    /**
     * @param fromStructuralId/toStructuralId 관계 종류별 parent/child, ancestor/descendant, 또는 source DOM 순서상 먼저/나중 region
     * @param sourceOrder DIRECT_SIBLING_ORDER에서만 의미(0-based index), 그 외 {@code null}
     * @param evidence 실제 DOM 계산 근거 문자열(추측 근거가 아니다)
     */
    public SemanticRegionRelationship(
            String fromStructuralId, String toStructuralId, RelationshipType relationshipType,
            Integer sourceOrder, String evidence) {
        this(fromStructuralId, toStructuralId, relationshipType, sourceOrder, evidence, null);
    }

    /**
     * {@code splitColumnOrderRank} 전용 overload. DIRECT_PARENT/ANCESTOR_CONTAINS에서만 의미가
     * 있으며, {@code from}이 accepted SPLIT_LAYOUT geometry order evidence를 가진 경우에만
     * non-null이다. {@code sourceOrder}와는 완전히 별개 필드다.
     */
    public SemanticRegionRelationship(
            String fromStructuralId, String toStructuralId, RelationshipType relationshipType,
            Integer sourceOrder, String evidence, Integer splitColumnOrderRank) {
        this.fromStructuralId = fromStructuralId;
        this.toStructuralId = toStructuralId;
        this.relationshipType = relationshipType;
        this.sourceOrder = sourceOrder;
        this.evidence = evidence;
        this.splitColumnOrderRank = splitColumnOrderRank;
    }

    public String getFromStructuralId() { return fromStructuralId; }
    public String getToStructuralId() { return toStructuralId; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public Integer getSourceOrder() { return sourceOrder; }
    public String getEvidence() { return evidence; }

    /** DIRECT_PARENT/ANCESTOR_CONTAINS에서, {@code from}이 SPLIT_LAYOUT이고 accepted geometry
     * order evidence를 가질 때만 non-null(그 외에는 항상 {@code null}). */
    public Integer getSplitColumnOrderRank() { return splitColumnOrderRank; }
}
