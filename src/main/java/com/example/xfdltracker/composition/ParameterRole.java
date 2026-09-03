package com.example.xfdltracker.composition;

/**
 * family/variant parameter가 참여할 수 있는 semantic responsibility category. 이 2개만 존재하며,
 * lawful origin/value-domain validation/renderer emission은 이 enum이 결정하지 않는다.
 */
public enum ParameterRole {

    /** composition/target structure 결정(예: loop bound, cardinality)에 참여할 수 있음. */
    STRUCTURAL_PARTICIPATION,

    /** future target-visible value contract의 대상이 될 수 있음. 이 role 하나만으로는 lawful
     * origin/validation/carrier/mapping/즉시 emission 중 어떤 것도 확립되지 않는다. */
    TARGET_VISIBLE_VALUE_PARTICIPATION
}
