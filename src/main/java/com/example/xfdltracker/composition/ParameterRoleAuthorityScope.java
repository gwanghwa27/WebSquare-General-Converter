package com.example.xfdltracker.composition;

/**
 * role declaration/query의 identity key. variant가 없는 family는 {@link Kind#FAMILY_PARAMETER},
 * 있으면 {@link Kind#VARIANT_PARAMETER}만 유효하다(실제 variant-cardinality 일치 여부는
 * {@link ParameterRoleRegistry#classify}가 검증). fake/sentinel variant는 쓰지 않는다.
 */
public final class ParameterRoleAuthorityScope {

    public enum Kind {
        FAMILY_PARAMETER,
        VARIANT_PARAMETER
    }

    private final Kind kind;
    private final String family;
    private final String variant;
    private final String parameterKey;

    private ParameterRoleAuthorityScope(Kind kind, String family, String variant, String parameterKey) {
        this.kind = kind;
        this.family = family;
        this.variant = variant;
        this.parameterKey = parameterKey;
    }

    public static ParameterRoleAuthorityScope familyParameter(String family, String parameterKey) {
        requireNonBlank(family, "family");
        requireNonBlank(parameterKey, "parameterKey");
        return new ParameterRoleAuthorityScope(Kind.FAMILY_PARAMETER, family, null, parameterKey);
    }

    public static ParameterRoleAuthorityScope variantParameter(String family, String variant, String parameterKey) {
        requireNonBlank(family, "family");
        requireNonBlank(variant, "variant");
        requireNonBlank(parameterKey, "parameterKey");
        return new ParameterRoleAuthorityScope(Kind.VARIANT_PARAMETER, family, variant, parameterKey);
    }

    private static void requireNonBlank(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "parameter_role_authority_scope: " + label + " must not be null/blank");
        }
    }

    public Kind getKind() { return kind; }
    public String getFamily() { return family; }

    /** {@link Kind#FAMILY_PARAMETER}일 때는 항상 {@code null} -- wildcard/sentinel이 아니라
     * "이 scope kind에는 variant identity component 자체가 없다"는 뜻이다. */
    public String getVariant() { return variant; }

    public String getParameterKey() { return parameterKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ParameterRoleAuthorityScope)) { return false; }
        ParameterRoleAuthorityScope other = (ParameterRoleAuthorityScope) o;
        return kind == other.kind
                && family.equals(other.family)
                && (variant == null ? other.variant == null : variant.equals(other.variant))
                && parameterKey.equals(other.parameterKey);
    }

    @Override
    public int hashCode() {
        int result = kind.hashCode();
        result = 31 * result + family.hashCode();
        result = 31 * result + (variant == null ? 0 : variant.hashCode());
        result = 31 * result + parameterKey.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (kind == Kind.FAMILY_PARAMETER) {
            return "FAMILY_PARAMETER(" + family + "," + parameterKey + ")";
        }
        return "VARIANT_PARAMETER(" + family + "," + variant + "," + parameterKey + ")";
    }
}
