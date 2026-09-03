package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link SlotAssignmentCandidate} 목록을 감싸 각 candidate마다 하나의 {@link CandidateResolution}
 * (기본 UNRESOLVED)을 유지하는, APPROVED/REJECTED 전이의 유일한 진입점. 자동 승인은 없으며,
 * fabricated candidate(원본에 identity가 없는 것)는 approve/reject해도 false만 반환하고 아무 것도 바뀌지 않는다.
 */
public class CandidateResolutionRegistry {

    private final List<CandidateResolution> resolutions;

    public CandidateResolutionRegistry(List<SlotAssignmentCandidate> candidates) {
        List<CandidateResolution> built = new ArrayList<CandidateResolution>();
        if (candidates != null) {
            for (SlotAssignmentCandidate candidate : candidates) {
                built.add(new CandidateResolution(
                        candidate.getParentStructuralId(), candidate.getChildStructuralId(),
                        candidate.getSlot(), candidate.getCompositionRuleId()));
            }
        }
        this.resolutions = built;
    }

    public List<CandidateResolution> getResolutions() {
        return Collections.unmodifiableList(resolutions);
    }

    /**
     * @return {@code candidate}가 이 registry의 원본 candidate 목록에 실제로 존재하고 그
     * resolution을 APPROVED로 전이시켰으면 {@code true}. {@code candidate}가 fabricated(원본에
     * 없음)이면 {@code false}이고 아무것도 바뀌지 않는다.
     */
    public boolean approve(SlotAssignmentCandidate candidate, String reason) {
        CandidateResolution match = findResolution(candidate);
        if (match == null) {
            return false;
        }
        match.approve(reason);
        return true;
    }

    /** {@link #approve}와 동일한 존재-검증 규칙으로 REJECTED로 전이시킨다. */
    public boolean reject(SlotAssignmentCandidate candidate, String reason) {
        CandidateResolution match = findResolution(candidate);
        if (match == null) {
            return false;
        }
        match.reject(reason);
        return true;
    }

    private CandidateResolution findResolution(SlotAssignmentCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        for (CandidateResolution resolution : resolutions) {
            if (resolution.matchesIdentity(candidate)) {
                return resolution;
            }
        }
        return null;
    }
}
