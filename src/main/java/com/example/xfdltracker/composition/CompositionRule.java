package com.example.xfdltracker.composition;

import java.util.Collections;
import java.util.List;

/**
 * {@code composition_rules.md}의 25개 규칙(순서/slot-fill/merge/nesting/cardinality)을 한
 * 스키마로 표현하는 pure-data 모델. 규칙 종류에 따라 필드 상당수가 비어 있을 수 있다(예:
 * ORDERING/MERGE는 slot 없음).
 */
public class CompositionRule {

    public enum RuleType {
        ORDERING,
        SLOT_FILL,
        MERGE,
        NESTING,
        CARDINALITY
    }

    private final String id;
    private final RuleType ruleType;
    private final List<String> subjectFamilies;
    private final List<String> slots;
    private final List<String> allowedChildFamilies;
    private final Integer minCardinality;
    private final Integer maxCardinality;
    private final Boolean selfNestingAllowed;
    private final String description;

    public CompositionRule(
            String id, RuleType ruleType, List<String> subjectFamilies, List<String> slots,
            List<String> allowedChildFamilies, Integer minCardinality, Integer maxCardinality,
            Boolean selfNestingAllowed, String description) {
        this.id = id;
        this.ruleType = ruleType;
        this.subjectFamilies = Collections.unmodifiableList(subjectFamilies);
        this.slots = Collections.unmodifiableList(slots);
        this.allowedChildFamilies = Collections.unmodifiableList(allowedChildFamilies);
        this.minCardinality = minCardinality;
        this.maxCardinality = maxCardinality;
        this.selfNestingAllowed = selfNestingAllowed;
        this.description = description;
    }

    public String getId() { return id; }
    public RuleType getRuleType() { return ruleType; }
    public List<String> getSubjectFamilies() { return subjectFamilies; }
    public List<String> getSlots() { return slots; }
    public List<String> getAllowedChildFamilies() { return allowedChildFamilies; }
    public Integer getMinCardinality() { return minCardinality; }
    public Integer getMaxCardinality() { return maxCardinality; }
    public Boolean getSelfNestingAllowed() { return selfNestingAllowed; }
    public String getDescription() { return description; }

    public boolean appliesTo(String family, String slot) {
        return subjectFamilies.contains(family) && slots.contains(slot);
    }
}
