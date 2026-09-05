package com.example.xfdltracker.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combo/Radio option 선언이 narrow subset(Slice 102D)을 완전히 만족했을 때만 만들어지는 typed,
 * immutable evidence다(JSON/구분자 직렬화나 XML fragment 우회 없음). renderer는 이 타입을 직접
 * 소비하지 않는다(extractor가 target-lane {@code TargetOptionItem}으로 변환한 뒤에만 전달).
 */
public final class SourceOptionSetEvidence {

    private final String sourceControlStructuralId;
    private final String sourceDatasetStructuralId;
    private final String sourceDatasetId;
    private final SourceOptionOriginKind originKind;
    private final String codeColumnId;
    private final String dataColumnId;
    private final List<SourceOptionItem> items;

    public SourceOptionSetEvidence(
            String sourceControlStructuralId,
            String sourceDatasetStructuralId,
            String sourceDatasetId,
            SourceOptionOriginKind originKind,
            String codeColumnId,
            String dataColumnId,
            List<SourceOptionItem> items) {
        if (sourceControlStructuralId == null || sourceControlStructuralId.length() == 0) {
            throw new IllegalArgumentException(
                    "source_option_set_evidence: sourceControlStructuralId must not be null/blank");
        }
        if (sourceDatasetStructuralId == null || sourceDatasetStructuralId.length() == 0) {
            throw new IllegalArgumentException(
                    "source_option_set_evidence: sourceDatasetStructuralId must not be null/blank");
        }
        if (sourceDatasetId == null || sourceDatasetId.length() == 0) {
            throw new IllegalArgumentException("source_option_set_evidence: sourceDatasetId must not be null/blank");
        }
        if (originKind == null) {
            throw new IllegalArgumentException("source_option_set_evidence: originKind must not be null");
        }
        if (codeColumnId == null || codeColumnId.length() == 0) {
            throw new IllegalArgumentException("source_option_set_evidence: codeColumnId must not be null/blank");
        }
        if (dataColumnId == null || dataColumnId.length() == 0) {
            throw new IllegalArgumentException("source_option_set_evidence: dataColumnId must not be null/blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("source_option_set_evidence: items must not be null/empty");
        }
        this.sourceControlStructuralId = sourceControlStructuralId;
        this.sourceDatasetStructuralId = sourceDatasetStructuralId;
        this.sourceDatasetId = sourceDatasetId;
        this.originKind = originKind;
        this.codeColumnId = codeColumnId;
        this.dataColumnId = dataColumnId;
        this.items = Collections.unmodifiableList(new ArrayList<SourceOptionItem>(items));
    }

    /** option을 선언한 Combo/Radio control 자신의 {@link SourceStructuralIdentity}. */
    public String getSourceControlStructuralId() { return sourceControlStructuralId; }

    /** 이 evidence의 근거가 된 실제 {@code Dataset} element의 {@link SourceStructuralIdentity} --
     *  control의 structural identity와 절대 혼동하지 않는다(별도 identity 공간). */
    public String getSourceDatasetStructuralId() { return sourceDatasetStructuralId; }

    /** source Dataset의 {@code id} 속성 원문. target runtime id로 승격하지 않는다(감사/추적 전용). */
    public String getSourceDatasetId() { return sourceDatasetId; }

    public SourceOptionOriginKind getOriginKind() { return originKind; }

    /** Dataset ColumnInfo 안에서 option value(=target xf:value)를 공급하는 column id. */
    public String getCodeColumnId() { return codeColumnId; }

    /** Dataset ColumnInfo 안에서 option label(=target xf:label)을 공급하는 column id. */
    public String getDataColumnId() { return dataColumnId; }

    /** source Row document order 그대로 보존된 option 목록(정렬/중복제거/필터링 없음). */
    public List<SourceOptionItem> getItems() { return items; }
}
