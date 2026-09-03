package com.example.xfdltracker.composition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code APPROVED} {@link CandidateResolution}만 {@link CompositionDecision#getSlotAssignments()}로 옮기며,
 * 반드시 {@link CompositionEvaluator#assignSlot}을 통해서만 적용한다(catalog/decision-integrity 재검증 우회 없음).
 * registry 존재만으로 신뢰하지 않고 {@link SlotAssignmentCandidateGenerator}를 재호출해 exact match일 때만 적용한다.
 */
public class CandidateResolutionApplier {

    private final CompositionEvaluator evaluator;
    private final SlotAssignmentCandidateGenerator candidateGenerator = new SlotAssignmentCandidateGenerator();

    public CandidateResolutionApplier(CompositionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * @param registry approve/reject로 전이된 resolution만 소비하는 registry
     * @param graph decisions 재검증에 쓰이는 현재 유효한 DOM 관계 그래프 및 decision 목록/조회 테이블
     * @return 새로 적용된 candidate 개수(이미 적용됐거나 거부된 것은 제외)
     */
    public int applyApproved(
            CandidateResolutionRegistry registry, SemanticRegionGraph graph, List<CompositionDecision> decisions,
            Map<String, CompositionDecision> decisionByStructuralId) {
        // identity key -> regenerated candidate. splitColumnOrderRank를 뒤에서 exact 재사용하기 위함.
        Map<String, SlotAssignmentCandidate> currentlyGeneratableCandidatesByIdentity =
                new HashMap<String, SlotAssignmentCandidate>();
        for (SlotAssignmentCandidate candidate : candidateGenerator.generateCandidates(graph, decisions)) {
            currentlyGeneratableCandidatesByIdentity.put(candidateIdentityKey(
                    candidate.getParentStructuralId(), candidate.getChildStructuralId(), candidate.getSlot(),
                    candidate.getCompositionRuleId()), candidate);
        }

        int appliedCount = 0;
        for (CandidateResolution resolution : registry.getResolutions()) {
            if (resolution.getStatus() != CandidateResolution.Status.APPROVED) {
                continue;
            }
            String resolutionIdentity = candidateIdentityKey(
                    resolution.getParentStructuralId(), resolution.getChildStructuralId(), resolution.getSlot(),
                    resolution.getCompositionRuleId());
            SlotAssignmentCandidate regeneratedCandidate =
                    currentlyGeneratableCandidatesByIdentity.get(resolutionIdentity);
            if (regeneratedCandidate == null) {
                // ruleId가 조작되었거나 이 graph+decisions로는 생성될 수 없는 candidate -- 거부한다.
                continue;
            }
            CompositionDecision parent = decisionByStructuralId.get(resolution.getParentStructuralId());
            CompositionDecision child = decisionByStructuralId.get(resolution.getChildStructuralId());
            if (parent == null || child == null) {
                continue;
            }
            if (alreadyAssigned(parent, resolution.getSlot(), child)) {
                continue; // idempotent 처리: 이 slot+child의 SlotAssignment가 이미 존재한다.
            }
            if (evaluator.assignSlot(
                    parent, resolution.getSlot(), child, regeneratedCandidate.getSplitColumnOrderRank())) {
                appliedCount++;
            }
        }
        return appliedCount;
    }

    private static String candidateIdentityKey(
            String parentStructuralId, String childStructuralId, String slot, String compositionRuleId) {
        return parentStructuralId + "|" + childStructuralId + "|" + slot + "|" + compositionRuleId;
    }

    /**
     * parent의 slotAssignments에서 같은 slot+sourceStructuralId의 child가 이미 배정됐는지 확인한다
     * (객체 identity가 아니라 structural identity로 비교).
     */
    private boolean alreadyAssigned(CompositionDecision parent, String slot, CompositionDecision child) {
        String childStructuralId = child.getSourceStructuralId();
        for (SlotAssignment existing : parent.getSlotAssignments()) {
            if (!slot.equals(existing.getSlot())) {
                continue;
            }
            CompositionDecision existingChild = existing.getChild();
            if (existingChild != null && childStructuralId != null
                    && childStructuralId.equals(existingChild.getSourceStructuralId())) {
                return true;
            }
        }
        return false;
    }
}
