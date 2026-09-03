package com.example.xfdltracker.composition;

/**
 * {@link SlotAssignmentCandidate} 하나에 대한 caller의 명시적 결정(승인/거부/미결)만 표현하는
 * pure-data 모델. identity는 parentStructuralId/childStructuralId/slot/compositionRuleId 4개
 * 필드 조합이다({@link #matchesIdentity}). 상태 전이는 {@link CandidateResolutionRegistry}만 수행하며 기본 상태는 항상 UNRESOLVED다.
 */
public class CandidateResolution {

    public enum Status {
        UNRESOLVED,
        APPROVED,
        REJECTED
    }

    private final String parentStructuralId;
    private final String childStructuralId;
    private final String slot;
    private final String compositionRuleId;
    private Status status = Status.UNRESOLVED;
    private String reason;

    CandidateResolution(String parentStructuralId, String childStructuralId, String slot, String compositionRuleId) {
        this.parentStructuralId = parentStructuralId;
        this.childStructuralId = childStructuralId;
        this.slot = slot;
        this.compositionRuleId = compositionRuleId;
    }

    public String getParentStructuralId() { return parentStructuralId; }
    public String getChildStructuralId() { return childStructuralId; }
    public String getSlot() { return slot; }
    public String getCompositionRuleId() { return compositionRuleId; }
    public Status getStatus() { return status; }
    public String getReason() { return reason; }

    /** package-private -- {@link CandidateResolutionRegistry}만 호출한다. */
    void approve(String reason) {
        this.status = Status.APPROVED;
        this.reason = reason;
    }

    /** package-private -- {@link CandidateResolutionRegistry}만 호출한다. */
    void reject(String reason) {
        this.status = Status.REJECTED;
        this.reason = reason;
    }

    boolean matchesIdentity(SlotAssignmentCandidate candidate) {
        return candidate != null
                && parentStructuralId.equals(candidate.getParentStructuralId())
                && childStructuralId.equals(candidate.getChildStructuralId())
                && slot.equals(candidate.getSlot())
                && compositionRuleId.equals(candidate.getCompositionRuleId());
    }
}
