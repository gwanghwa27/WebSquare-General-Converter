package com.example.xfdltracker.composition;

/**
 * render-result exact provenance carrier: {@link TargetCompositionNode}가 identity 생성 시점에
 * materialize하는 불변 {@code (IDENTITY_KIND, IDENTITY_VALUE)} tuple이며, 모든 downstream render-result
 * artifact가 intrinsic하게 지녀야 한다. pure value carrier로 새 semantic authority를 만들지 않는다.
 */
public final class TargetNodeIdentity {

    private final TargetNodeIdentityKind kind;
    private final String value;

    public TargetNodeIdentity(TargetNodeIdentityKind kind, String value) {
        if (kind == null) {
            throw new IllegalArgumentException("target_node_identity: kind must not be null");
        }
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "target_node_identity: value must not be null/blank -- every exact provenance tuple "
                            + "must carry a non-blank identity value");
        }
        this.kind = kind;
        this.value = value;
    }

    public TargetNodeIdentityKind getKind() { return kind; }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TargetNodeIdentity)) {
            return false;
        }
        TargetNodeIdentity that = (TargetNodeIdentity) other;
        return this.kind == that.kind && this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return 31 * kind.hashCode() + value.hashCode();
    }

    @Override
    public String toString() {
        return kind + ":" + value;
    }
}
