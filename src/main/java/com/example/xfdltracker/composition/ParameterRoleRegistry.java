package com.example.xfdltracker.composition;

import java.util.List;

/**
 * 어떤 semantic role fact도 자체 보관하지 않는 stateless facade다(static-only). authoritative raw
 * role declaration data는 오직 {@link TemplateFamilyCatalog.FamilyDefinition}만 소유하며, 이
 * 클래스는 declare 위임과 classify의 catalog/vocabulary 검증만 수행한다.
 */
public final class ParameterRoleRegistry {

    private ParameterRoleRegistry() {
    }

    /**
     * {@code scope}에 대한 raw role token 선언을 해당 family의 {@link TemplateFamilyCatalog.FamilyDefinition}에
     * 위임해 등록한다(이 메서드는 authority를 갖지 않는다).
     * @throws IllegalArgumentException scope/rawRoleTokens가 null이거나 family가 미확인일 때
     */
    public static void declare(ParameterRoleAuthorityScope scope, List<String> rawRoleTokens) {
        if (scope == null) {
            throw new IllegalArgumentException("parameter_role_registry: scope must not be null");
        }
        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(scope.getFamily());
        if (def == null) {
            throw new IllegalArgumentException(
                    "parameter_role_registry: declare(...) for unknown family=" + scope.getFamily());
        }
        def.declareRole(scope, rawRoleTokens);
    }

    /**
     * {@code scope}를 catalog membership -> scope-shape 일치 -> 선언 존재 여부 -> raw token
     * vocabulary/duplicate 순서로 검증해 classify한다. 매번 family model에서 다시 읽어 재검증하며
     * 캐시하지 않는다.
     */
    public static ParameterRoleClassification classify(ParameterRoleAuthorityScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("parameter_role_registry: scope must not be null");
        }

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(scope.getFamily());
        if (def == null) {
            return ParameterRoleClassification.unknownParameter("unknown_family:" + scope.getFamily());
        }
        if (!def.hasParameter(scope.getParameterKey())) {
            return ParameterRoleClassification.unknownParameter(
                    "unknown_parameter:" + scope.getFamily() + ":" + scope.getParameterKey());
        }

        boolean familyHasVariants = !def.getVariants().isEmpty();
        if (scope.getKind() == ParameterRoleAuthorityScope.Kind.FAMILY_PARAMETER) {
            if (familyHasVariants) {
                return ParameterRoleClassification.malformed(
                        "invalid_authority_scope_shape:family_parameter_scope_used_for_variant_bearing_family:"
                                + scope.getFamily());
            }
        } else {
            if (!familyHasVariants) {
                return ParameterRoleClassification.malformed(
                        "invalid_authority_scope_shape:variant_parameter_scope_used_for_variantless_family:"
                                + scope.getFamily());
            }
            if (!def.hasVariant(scope.getVariant())) {
                return ParameterRoleClassification.malformed(
                        "membership_mismatch:variant_not_in_family:" + scope.getFamily() + ":"
                                + scope.getVariant());
            }
        }

        List<String> raw = def.getDeclaredRoleTokens(scope);
        if (raw == null) {
            return ParameterRoleClassification.missing("missing_declaration:" + scope);
        }
        if (raw.isEmpty()) {
            return ParameterRoleClassification.valid(java.util.Collections.<ParameterRole>emptySet());
        }

        java.util.Set<ParameterRole> validated = java.util.EnumSet.noneOf(ParameterRole.class);
        for (String token : raw) {
            ParameterRole role;
            try {
                role = ParameterRole.valueOf(token);
            } catch (IllegalArgumentException e) {
                return ParameterRoleClassification.malformed(
                        "unknown_role_token:" + scope + ":" + token);
            }
            if (!validated.add(role)) {
                return ParameterRoleClassification.malformed(
                        "malformed_duplicate_role_token:" + scope + ":" + token);
            }
        }
        return ParameterRoleClassification.valid(validated);
    }
}
