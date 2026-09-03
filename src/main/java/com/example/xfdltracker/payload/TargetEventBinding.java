package com.example.xfdltracker.payload;

/**
 * {@code TargetPayloadBehaviorFinalizer}가 만드는 finalized target event binding 하나의
 * immutable pure-data. renderer는 오직 이 값만 event correlation 권위로 읽는다 --
 * source DOM/structural identity/raw functionName은 담지 않는다.
 */
public final class TargetEventBinding {

    private final int buttonOrdinal;
    private final String targetEventLocalName;
    private final String targetFunctionIdentifier;

    public TargetEventBinding(int buttonOrdinal, String targetEventLocalName, String targetFunctionIdentifier) {
        if (buttonOrdinal < 0) {
            throw new IllegalArgumentException(
                    "target_event_binding: buttonOrdinal must be >= 0, but was " + buttonOrdinal);
        }
        if (targetEventLocalName == null || targetEventLocalName.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "target_event_binding: targetEventLocalName must not be null/blank");
        }
        if (targetFunctionIdentifier == null || targetFunctionIdentifier.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "target_event_binding: targetFunctionIdentifier must not be null/blank");
        }
        this.buttonOrdinal = buttonOrdinal;
        this.targetEventLocalName = targetEventLocalName;
        this.targetFunctionIdentifier = targetFunctionIdentifier;
    }

    public int getButtonOrdinal() { return buttonOrdinal; }
    public String getTargetEventLocalName() { return targetEventLocalName; }
    public String getTargetFunctionIdentifier() { return targetFunctionIdentifier; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TargetEventBinding)) {
            return false;
        }
        TargetEventBinding other = (TargetEventBinding) o;
        return buttonOrdinal == other.buttonOrdinal
                && targetEventLocalName.equals(other.targetEventLocalName)
                && targetFunctionIdentifier.equals(other.targetFunctionIdentifier);
    }

    @Override
    public int hashCode() {
        int result = buttonOrdinal;
        result = 31 * result + targetEventLocalName.hashCode();
        result = 31 * result + targetFunctionIdentifier.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TargetEventBinding{buttonOrdinal=" + buttonOrdinal + ", targetEventLocalName=\""
                + targetEventLocalName + "\", targetFunctionIdentifier=\"" + targetFunctionIdentifier + "\"}";
    }
}
