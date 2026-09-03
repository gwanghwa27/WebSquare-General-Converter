package com.example.xfdltracker.behavior;

/**
 * {@code TargetScriptTranslator}의 불변 결과 래퍼. {@code status == TRANSLATED}일 때만
 * {@code artifact}가 non-null이며, 그 외 상태는 항상 non-blank {@code reason}을 갖고 {@code artifact}는
 * 절대 공개하지 않는다.
 */
public final class TargetScriptTranslationResult {

    private final TargetTranslationStatus status;
    private final TargetScriptArtifact artifact;
    private final String reason;

    private TargetScriptTranslationResult(TargetTranslationStatus status, TargetScriptArtifact artifact, String reason) {
        this.status = status;
        this.artifact = artifact;
        this.reason = reason;
    }

    public static TargetScriptTranslationResult translated(TargetScriptArtifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("target_script_translation_result: artifact must not be null");
        }
        return new TargetScriptTranslationResult(TargetTranslationStatus.TRANSLATED, artifact, null);
    }

    public static TargetScriptTranslationResult failure(TargetTranslationStatus status, String reason) {
        if (status == TargetTranslationStatus.TRANSLATED) {
            throw new IllegalArgumentException("target_script_translation_result: failure() must not use TRANSLATED");
        }
        if (reason == null || reason.trim().length() == 0) {
            throw new IllegalArgumentException("target_script_translation_result: reason must not be null/blank");
        }
        return new TargetScriptTranslationResult(status, null, reason);
    }

    public TargetTranslationStatus getStatus() { return status; }
    public TargetScriptArtifact getArtifact() { return artifact; }
    public String getReason() { return reason; }
}
