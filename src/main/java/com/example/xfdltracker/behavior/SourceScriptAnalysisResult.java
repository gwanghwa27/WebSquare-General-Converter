package com.example.xfdltracker.behavior;

/**
 * {@code SourceScriptAnalyzer}의 불변 결과 래퍼. {@code status == ANALYZED}일 때만 {@code analysis}가
 * non-null이며, 그 외 상태는 항상 non-blank {@code reason}을 갖고 {@code analysis}는 절대 공개하지 않는다.
 */
public final class SourceScriptAnalysisResult {

    private final SourceAnalysisStatus status;
    private final SourceScriptAnalysis analysis;
    private final String reason;

    private SourceScriptAnalysisResult(SourceAnalysisStatus status, SourceScriptAnalysis analysis, String reason) {
        this.status = status;
        this.analysis = analysis;
        this.reason = reason;
    }

    public static SourceScriptAnalysisResult analyzed(SourceScriptAnalysis analysis) {
        if (analysis == null) {
            throw new IllegalArgumentException("source_script_analysis_result: analysis must not be null");
        }
        return new SourceScriptAnalysisResult(SourceAnalysisStatus.ANALYZED, analysis, null);
    }

    public static SourceScriptAnalysisResult unsupportedSyntax(String reason) {
        return new SourceScriptAnalysisResult(SourceAnalysisStatus.UNSUPPORTED_SYNTAX, null, requireReason(reason));
    }

    public static SourceScriptAnalysisResult integrityViolation(String reason) {
        return new SourceScriptAnalysisResult(SourceAnalysisStatus.INTEGRITY_VIOLATION, null, requireReason(reason));
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.trim().length() == 0) {
            throw new IllegalArgumentException("source_script_analysis_result: reason must not be null/blank");
        }
        return reason;
    }

    public SourceAnalysisStatus getStatus() { return status; }
    public SourceScriptAnalysis getAnalysis() { return analysis; }
    public String getReason() { return reason; }
}
