package com.example.xfdltracker.semantic;

/**
 * Combo/Radio control의 option 선언 resolve 결과(Slice 102D) -- {@link #resolved} 성공 evidence
 * 또는 {@link #failed} deterministic reason 중 하나다(plain control은 null로 표현, 인스턴스 아님).
 * reason 문자열은 {@code TargetPayloadExtractor}가 예외를 던질 때도 그대로 재사용한다.
 */
public final class SourceOptionResolution {

    public static final String REASON_DATASET_MISSING = "search_area_option_dataset_missing";
    public static final String REASON_DATASET_AMBIGUOUS = "search_area_option_dataset_ambiguous";
    public static final String REASON_CODECOLUMN_MISSING = "search_area_option_codecolumn_missing";
    public static final String REASON_DATACOLUMN_MISSING = "search_area_option_datacolumn_missing";
    public static final String REASON_COLUMN_NOT_FOUND = "search_area_option_column_not_found";
    public static final String REASON_COLUMN_AMBIGUOUS = "search_area_option_column_ambiguous";
    public static final String REASON_ROW_VALUE_MISSING = "search_area_option_row_value_missing";
    public static final String REASON_ROW_LABEL_MISSING = "search_area_option_row_label_missing";
    public static final String REASON_ROW_VALUE_AMBIGUOUS = "search_area_option_row_value_ambiguous";
    public static final String REASON_ROW_LABEL_AMBIGUOUS = "search_area_option_row_label_ambiguous";
    public static final String REASON_ROWS_MISSING = "search_area_option_rows_missing";
    public static final String REASON_ROWS_EMPTY = "search_area_option_rows_empty";
    public static final String REASON_VALUE_EMPTY = "search_area_option_value_empty";
    public static final String REASON_LABEL_EMPTY = "search_area_option_label_empty";
    public static final String REASON_VALUE_DUPLICATE = "search_area_option_value_duplicate";
    public static final String REASON_INLINE_UNPROVEN = "search_area_inline_option_dataset_unproven";
    public static final String REASON_SCOPE_UNSUPPORTED = "search_area_option_dataset_scope_unsupported";

    private final SourceOptionSetEvidence evidence;
    private final String failureReason;

    private SourceOptionResolution(SourceOptionSetEvidence evidence, String failureReason) {
        this.evidence = evidence;
        this.failureReason = failureReason;
    }

    public static SourceOptionResolution resolved(SourceOptionSetEvidence evidence) {
        if (evidence == null) {
            throw new IllegalArgumentException("source_option_resolution: evidence must not be null for resolved()");
        }
        return new SourceOptionResolution(evidence, null);
    }

    public static SourceOptionResolution failed(String failureReason) {
        if (failureReason == null || failureReason.length() == 0) {
            throw new IllegalArgumentException(
                    "source_option_resolution: failureReason must not be null/blank for failed()");
        }
        return new SourceOptionResolution(null, failureReason);
    }

    public boolean isResolved() { return evidence != null; }

    public SourceOptionSetEvidence getEvidence() {
        if (evidence == null) {
            throw new IllegalStateException(
                    "source_option_resolution: getEvidence() called on a failed resolution (failureReason="
                            + failureReason + ")");
        }
        return evidence;
    }

    public String getFailureReason() {
        if (failureReason == null) {
            throw new IllegalStateException(
                    "source_option_resolution: getFailureReason() called on a resolved resolution");
        }
        return failureReason;
    }
}
