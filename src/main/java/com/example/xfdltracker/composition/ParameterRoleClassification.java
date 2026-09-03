package com.example.xfdltracker.composition;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@link ParameterRoleRegistry#classify}의 결과 pure-data. 4개 상태(VALID_EXPLICIT_ROLE_SET/
 * UNKNOWN_PARAMETER/MISSING_INCOMPLETE_CLASSIFICATION/MALFORMED_CONFLICTING_CLASSIFICATION)만
 * 존재하며, VALID_EXPLICIT_ROLE_SET이 아닌 상태에서 eligibility를 물으면 false 대신 예외로 fail-closed한다.
 */
public final class ParameterRoleClassification {

    public enum Status {
        VALID_EXPLICIT_ROLE_SET,
        UNKNOWN_PARAMETER,
        MISSING_INCOMPLETE_CLASSIFICATION,
        MALFORMED_CONFLICTING_CLASSIFICATION
    }

    private final Status status;
    private final Set<ParameterRole> roles;
    private final String reason;

    private ParameterRoleClassification(Status status, Set<ParameterRole> roles, String reason) {
        this.status = status;
        this.roles = roles;
        this.reason = reason;
    }

    static ParameterRoleClassification valid(Set<ParameterRole> roles) {
        Set<ParameterRole> copy = roles.isEmpty()
                ? Collections.<ParameterRole>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(roles));
        return new ParameterRoleClassification(Status.VALID_EXPLICIT_ROLE_SET, copy, null);
    }

    static ParameterRoleClassification unknownParameter(String reason) {
        return new ParameterRoleClassification(Status.UNKNOWN_PARAMETER, null, reason);
    }

    static ParameterRoleClassification missing(String reason) {
        return new ParameterRoleClassification(Status.MISSING_INCOMPLETE_CLASSIFICATION, null, reason);
    }

    static ParameterRoleClassification malformed(String reason) {
        return new ParameterRoleClassification(Status.MALFORMED_CONFLICTING_CLASSIFICATION, null, reason);
    }

    public Status getStatus() { return status; }

    /** 상태가 {@code MALFORMED_CONFLICTING_CLASSIFICATION}/{@code UNKNOWN_PARAMETER}/
     * {@code MISSING_INCOMPLETE_CLASSIFICATION}일 때만 non-null. */
    public String getReason() { return reason; }

    /** {@link Status#VALID_EXPLICIT_ROLE_SET}에서만 non-null(빈 set 포함). 다른 상태에서는 {@code null} --
     * 직접 읽어 "빈 set이니까 false"로 오독하지 않도록 {@link #isStructuralParticipationEligible()} 등을 통해서만 조회한다. */
    public Set<ParameterRole> getRoles() { return roles; }

    /** {@code VALID_EXPLICIT_ROLE_SET}에서만 authoritative. 그 외 상태에서 호출하면 failure를
     * {@code false}와 혼동하지 않도록 예외로 fail-closed한다. */
    public boolean isStructuralParticipationEligible() {
        requireValid();
        return roles.contains(ParameterRole.STRUCTURAL_PARTICIPATION);
    }

    /** {@code VALID_EXPLICIT_ROLE_SET}에서만 authoritative. {@code true}는 "future target-visible value
     * contract 대상이 될 수 있다"는 뜻일 뿐, lawful origin/value-domain/carrier/renderer emission 등은 확립하지 않는다. */
    public boolean isTargetVisibleValueParticipationEligible() {
        requireValid();
        return roles.contains(ParameterRole.TARGET_VISIBLE_VALUE_PARTICIPATION);
    }

    private void requireValid() {
        if (status != Status.VALID_EXPLICIT_ROLE_SET) {
            throw new IllegalStateException(
                    "parameter_role_classification: participation eligibility query requires "
                            + "VALID_EXPLICIT_ROLE_SET, actual status=" + status
                            + (reason == null ? "" : (":" + reason)));
        }
    }
}
