package com.example.xfdltracker.composition;

/**
 * {@code targetSyntheticId}를 결정적 규칙으로 만드는 stateless pure helper(I/O 없음). parent/slot/
 * targetFamily/stableDiscriminator 4 component를 length-prefixed encoding으로 결합해 서로 다른
 * 4-튜플은 항상 다른 문자열을 낸다(injective). random UUID/counter/hash는 쓰지 않는다.
 */
public final class TargetSyntheticIdentity {

    private static final String PREFIX = "TSI|";

    private TargetSyntheticIdentity() {
    }

    /**
     * @param parent/slot/targetFamily identity 확정된 parent decision, 채울 slot 이름, 만들 family
     * @param stableDiscriminator 같은 parent/slot/family 아래 다른 entity와 구별하는 결정적 값(UUID/counter 금지)
     * @throws IllegalArgumentException parent identity 미확정, 또는 인자 중 하나라도 비어 있을 때
     */
    public static String build(
            CompositionDecision parent, String slot, String targetFamily, String stableDiscriminator) {
        if (parent == null) {
            throw new IllegalArgumentException("target_synthetic_identity: parent must not be null");
        }
        if (slot == null || slot.length() == 0) {
            throw new IllegalArgumentException("target_synthetic_identity: slot must not be empty");
        }
        if (targetFamily == null || targetFamily.length() == 0) {
            throw new IllegalArgumentException("target_synthetic_identity: targetFamily must not be empty");
        }
        if (stableDiscriminator == null || stableDiscriminator.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "target_synthetic_identity: stableDiscriminator must not be null/blank -- the "
                            + "identity of this synthetic entity cannot be established without it "
                            + "(catalog does not guarantee singleton cardinality for this slot)");
        }

        String parentIdentity;
        if (parent.getOrigin() == CompositionDecision.Origin.SOURCE_SEMANTIC) {
            parentIdentity = parent.getSourceStructuralId();
        } else if (parent.getOrigin() == CompositionDecision.Origin.TARGET_SYNTHETIC) {
            parentIdentity = parent.getTargetSyntheticId();
        } else {
            parentIdentity = null;
        }
        if (parentIdentity == null || parentIdentity.length() == 0) {
            throw new IllegalArgumentException(
                    "target_synthetic_identity: parent (family=" + parent.getFamily()
                            + ") has no established identity yet (origin=" + parent.getOrigin() + ")");
        }

        return encode(parentIdentity, slot, targetFamily, stableDiscriminator);
    }

    /**
     * netstring 방식 length-prefixed encoding. component 경계가 선언된 길이로만 정해지므로
     * 내부 문자 구성과 무관하게 injective하다.
     */
    private static String encode(String... components) {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (String component : components) {
            sb.append(component.length()).append(':').append(component);
        }
        return sb.toString();
    }
}
