package com.example.xfdltracker.composition;

import com.example.xfdltracker.semantic.SemanticRegionResult;

import java.util.Map;

/**
 * {@link SemanticRegionResult}를 {@link TemplateFamilyCatalog}에 대조해 {@link CompositionDecision}으로
 * 변환한다. WebSquare XML을 생성하지 않는 순수 관찰/검증 계층이다.
 * family/variant/parameter는 catalog에 없으면 무조건 ineligible이며, 추측 보정하지 않는다.
 */
public class CompositionEvaluator {

    public CompositionDecision evaluate(SemanticRegionResult result) {
        CompositionDecision decision = new CompositionDecision();
        decision.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        if (result == null) {
            decision.setEligible(false);
            decision.getReasons().add("null_result");
            return decision;
        }

        decision.setSourceRegionId(result.getSourceRegionId());
        decision.setSourceStructuralId(result.getSourceStructuralId());
        // exact copy -- DOM 접근, 문자열 재구성, 그래프 추론 없음.
        decision.setTabPageMembership(result.getTabPageMembership());
        decision.setConfidence(result.getConfidence());
        decision.setFallback(result.getFallback());

        String family = result.getRecommendedTemplateFamily();
        decision.setFamily(family);
        if (family == null || family.length() == 0) {
            decision.setEligible(false);
            decision.getReasons().add("missing_family");
            return decision;
        }

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
        if (def == null) {
            decision.setEligible(false);
            decision.getReasons().add("unknown_family:" + family);
            return decision;
        }
        if (def.getSourcePredicateStatus() != TemplateFamilyCatalog.SourcePredicateStatus.ACCEPTABLE) {
            decision.setEligible(false);
            decision.getReasons().add(
                    "family_not_source_acceptable:" + family + ":" + def.getSourcePredicateStatus());
            return decision;
        }

        boolean valid = true;

        String variant = result.getRecommendedVariant();
        if (variant != null && variant.length() > 0) {
            if (def.hasVariant(variant)) {
                decision.setVariant(variant);
            } else {
                valid = false;
                decision.getReasons().add("unknown_variant:" + family + ":" + variant);
            }
        }

        for (Map.Entry<String, Object> entry : result.getParameters().entrySet()) {
            if (def.hasParameter(entry.getKey())) {
                // 이 family+key가 SOURCE_SEMANTIC에서 투영 불가로 알려져 있으면(예:
                // BUTTON_GROUP.position) 값과 무관하게 이 parameter만 스킵한다 -- family 전체를
                // ineligible로 만들지 않는다.
                if (TargetParameterValueContract.isNotProjectableFromSourceSemantic(family, entry.getKey())) {
                    decision.getReasons().add(
                            "source_projection_not_supported:" + family + ":" + entry.getKey() + ":"
                                    + entry.getValue());
                    continue;
                }
                // 값이 known target value domain과 다르면 이 parameter의 target projection만 HOLD한다.
                if (TargetParameterValueContract.isKnownDomainViolation(family, entry.getKey(), entry.getValue())) {
                    decision.getReasons().add(
                            "target_parameter_domain_mismatch_hold:" + family + ":" + entry.getKey() + ":"
                                    + entry.getValue());
                    continue;
                }
                decision.getParameters().put(entry.getKey(), entry.getValue());
            } else {
                valid = false;
                decision.getReasons().add("unknown_parameter:" + family + ":" + entry.getKey());
            }
        }

        if (result.getFallback() != null && result.getFallback().length() > 0) {
            decision.getReasons().add("fallback_preserved:" + result.getFallback());
        }
        if ("LOW".equals(result.getConfidence())) {
            decision.getReasons().add("low_confidence_no_canonical_rewrite");
        }

        decision.setEligible(valid);
        return decision;
    }

    /**
     * {@code parent}/{@code child} decision을 {@link CompositionRuleCatalog} 규칙과 일관될 때만 {@code slot}에
     * 배정한다(DOM sibling/geometry 자동 추론 없음). caller가 세팅한 값을 신뢰하지 않고
     * {@link #decisionIntegrityFailureReason}으로 매번 재검증하며, 거부 이유는 {@code parent.getReasons()}에 남는다.
     */
    public boolean assignSlot(CompositionDecision parent, String slot, CompositionDecision child) {
        return assignSlot(parent, slot, child, null);
    }

    /**
     * {@code splitColumnOrderRank} 전용 overload. 검증/거부 규칙은 3-arg
     * {@link #assignSlot(CompositionDecision, String, CompositionDecision)}와 동일하며, 성공 시
     * {@link SlotAssignment}에 이 rank를 함께 싣는 것만 다르다. 이 메서드는 rank 값을 계산/추론하지 않는다.
     */
    public boolean assignSlot(
            CompositionDecision parent, String slot, CompositionDecision child, Integer splitColumnOrderRank) {
        if (parent == null || slot == null || slot.length() == 0 || child == null) {
            return false;
        }

        String parentIntegrityFailure = decisionIntegrityFailureReason(parent, slot, "parent_");
        if (parentIntegrityFailure != null) {
            parent.getReasons().add("slot_assignment_rejected:" + parentIntegrityFailure);
            return false;
        }

        TemplateFamilyCatalog.FamilyDefinition parentDef = TemplateFamilyCatalog.get(parent.getFamily());
        if (parentDef == null || !parentDef.hasSlot(slot)) {
            parent.getReasons().add("slot_assignment_rejected:unknown_slot:" + slot);
            return false;
        }

        if ("LOW".equals(parent.getConfidence())
                && parent.getFallback() != null && parent.getFallback().length() > 0) {
            parent.getReasons().add("slot_assignment_rejected:low_confidence_canonical_rewrite:" + slot);
            return false;
        }

        String childIntegrityFailure = decisionIntegrityFailureReason(child, slot, "");
        if (childIntegrityFailure != null) {
            parent.getReasons().add("slot_assignment_rejected:" + childIntegrityFailure);
            return false;
        }

        CompositionRule slotRule = CompositionRuleCatalog.slotFillRule(parent.getFamily(), slot);
        if (slotRule == null || !slotRule.getAllowedChildFamilies().contains(child.getFamily())) {
            parent.getReasons().add(
                    "slot_assignment_rejected:invalid_child_family:" + slot + ":" + child.getFamily());
            return false;
        }

        if (!child.isEligible()) {
            parent.getReasons().add("slot_assignment_rejected:child_not_eligible:" + slot);
            return false;
        }

        CompositionRule cardinalityRule = CompositionRuleCatalog.cardinalityRule(parent.getFamily(), slot);
        if (cardinalityRule != null && cardinalityRule.getMaxCardinality() != null) {
            int currentCount = 0;
            for (SlotAssignment existing : parent.getSlotAssignments()) {
                if (slot.equals(existing.getSlot())) {
                    currentCount++;
                }
            }
            if (currentCount + 1 > cardinalityRule.getMaxCardinality().intValue()) {
                parent.getReasons().add("slot_assignment_rejected:cardinality_exceeded:" + slot);
                return false;
            }
        }

        parent.getSlotAssignments().add(new SlotAssignment(slot, child, splitColumnOrderRank));
        return true;
    }

    /**
     * {@code decision.isEligible()}을 신뢰하지 않고 origin/family catalog status/variant/parameter를 매번
     * 재검증한다(eligible 필드 자체는 보지 않음). 문제 없으면 {@code null}, 있으면
     * {@code rolePrefix + code + ":" + slot} 형태 reason을 반환한다. package-private로 candidate precheck에서도 재사용된다.
     */
    String decisionIntegrityFailureReason(CompositionDecision decision, String slot, String rolePrefix) {
        if (decision.getOrigin() == null) {
            return rolePrefix + "untracked_origin:" + slot;
        }
        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(decision.getFamily());
        if (def == null) {
            return rolePrefix + "unknown_family:" + slot;
        }
        if (decision.getOrigin() == CompositionDecision.Origin.SOURCE_SEMANTIC
                && def.getSourcePredicateStatus() != TemplateFamilyCatalog.SourcePredicateStatus.ACCEPTABLE) {
            return rolePrefix + "source_gate_bypassed:" + slot + ":" + decision.getFamily();
        }
        if (decision.getOrigin() == CompositionDecision.Origin.TARGET_SYNTHETIC
                && def.getTargetFamilyStatus() == TemplateFamilyCatalog.TargetFamilyStatus.CANDIDATE_INSUFFICIENT_EVIDENCE) {
            return rolePrefix + "target_gate_bypassed:" + slot + ":" + decision.getFamily();
        }
        if (decision.getVariant() != null && !def.hasVariant(decision.getVariant())) {
            return rolePrefix + "invalid_variant:" + slot + ":" + decision.getFamily() + ":" + decision.getVariant();
        }
        for (Map.Entry<String, Object> entry : decision.getParameters().entrySet()) {
            if (!def.hasParameter(entry.getKey())) {
                return rolePrefix + "invalid_parameter:" + slot + ":" + decision.getFamily() + ":" + entry.getKey();
            }
            // 정상 evaluate() 경로는 이 parameter를 애초에 넣지 않으므로, 여기서 걸리는 것은
            // evaluate()를 우회한 tampered 입력뿐이다. SOURCE_SEMANTIC에만 적용한다.
            if (decision.getOrigin() == CompositionDecision.Origin.SOURCE_SEMANTIC
                    && TargetParameterValueContract.isNotProjectableFromSourceSemantic(
                            decision.getFamily(), entry.getKey())) {
                return rolePrefix + "source_projection_not_supported:" + slot + ":" + decision.getFamily() + ":"
                        + entry.getKey() + ":" + entry.getValue();
            }
            if (TargetParameterValueContract.isKnownDomainViolation(
                    decision.getFamily(), entry.getKey(), entry.getValue())) {
                return rolePrefix + "invalid_parameter_value:" + slot + ":" + decision.getFamily() + ":"
                        + entry.getKey() + ":" + entry.getValue();
            }
        }
        return null;
    }

    /**
     * source evidence 없이 target-side composition invariant(예: PAGING)를 위한 decision을 만드는 유일한 경로.
     * family가 catalog에 있고 target status가 CANDIDATE_INSUFFICIENT_EVIDENCE가 아닐 때만 eligible=true.
     * {@link CompositionDecision.Origin#TARGET_SYNTHETIC}으로 표시한다.
     */
    public CompositionDecision createTargetSyntheticDecision(String family, Map<String, Object> parameters) {
        return createTargetSyntheticDecision(family, parameters, null);
    }

    /**
     * 위 2-arg 버전과 동일한 검증에 더해, caller가 결정한 explicit {@code targetSyntheticId}를 decision에 싣는다.
     * random UUID 생성이나 family/parameter로부터의 추측은 하지 않는다(같은 family+parameter라도 다른 entity일 수 있음).
     * {@link TargetCompositionPlanBuilder}는 이 필드가 채워진 decision만 Plan node로 받아들인다.
     */
    public CompositionDecision createTargetSyntheticDecision(
            String family, Map<String, Object> parameters, String targetSyntheticId) {
        CompositionDecision decision = new CompositionDecision();
        decision.setOrigin(CompositionDecision.Origin.TARGET_SYNTHETIC);
        decision.setFamily(family);
        decision.setTargetSyntheticId(targetSyntheticId);

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
        if (def == null) {
            decision.setEligible(false);
            decision.getReasons().add("unknown_family:" + family);
            return decision;
        }
        if (def.getTargetFamilyStatus() == TemplateFamilyCatalog.TargetFamilyStatus.CANDIDATE_INSUFFICIENT_EVIDENCE) {
            decision.setEligible(false);
            decision.getReasons().add(
                    "target_status_not_confirmed:" + family + ":" + def.getTargetFamilyStatus());
            return decision;
        }

        boolean valid = true;
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (def.hasParameter(entry.getKey())) {
                    decision.getParameters().put(entry.getKey(), entry.getValue());
                } else {
                    valid = false;
                    decision.getReasons().add("unknown_parameter:" + family + ":" + entry.getKey());
                }
            }
        }

        decision.setEligible(valid);
        return decision;
    }
}
