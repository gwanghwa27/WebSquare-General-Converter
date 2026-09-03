package com.example.xfdltracker.semantic;

/**
 * semantic predicate가 family를 확정하는 시점에, 이미 확인한 leaf {@code Element}에서
 * 직접 읽은 최소 primitive evidence 하나(DOM 참조는 보존하지 않음). {@code TargetPayloadExtractor}가
 * 이후 source DOM을 재분석해 leaf 구조를 "재발견"하지 않도록 옮겨 담는다.
 */
public final class SourcePayloadEvidenceItem {

    private final String semanticRegionStructuralId;
    private final String sourceComponentStructuralId;
    private final String evidenceRole;
    private final String evidenceKind;
    private final String value;
    private final String functionName;
    private final int sourceOrder;
    private final Integer rowIndex;
    private final Integer cellIndexInRow;
    private final Integer pairIndexInRow;

    public SourcePayloadEvidenceItem(
            String semanticRegionStructuralId,
            String sourceComponentStructuralId,
            String evidenceRole,
            String evidenceKind,
            String value,
            int sourceOrder) {
        this(semanticRegionStructuralId, sourceComponentStructuralId, evidenceRole, evidenceKind, value, null,
                sourceOrder);
    }

    /**
     * role이 "event"일 때만 쓰는 최소 확장 -- value는 eventName, 이 필드는 functionName을 담는다
     * (다른 role은 항상 null). 별도 클래스를 새로 만들지 않고 기존 계약을 재사용한다.
     */
    public SourcePayloadEvidenceItem(
            String semanticRegionStructuralId,
            String sourceComponentStructuralId,
            String evidenceRole,
            String evidenceKind,
            String value,
            String functionName,
            int sourceOrder) {
        this(semanticRegionStructuralId, sourceComponentStructuralId, evidenceRole, evidenceKind, value,
                functionName, sourceOrder, null);
    }

    /**
     * role이 "label"/"control"일 때만 쓰는 최소 확장 -- row의 0-based 위치를 predicate가 이미
     * 계산한 값 그대로 옮긴다. 비대칭 행에서는 sourceOrder만으로 row 경계를 복원할 수 없어
     * 필요하다(그 외 role은 항상 null).
     */
    public SourcePayloadEvidenceItem(
            String semanticRegionStructuralId,
            String sourceComponentStructuralId,
            String evidenceRole,
            String evidenceKind,
            String value,
            String functionName,
            int sourceOrder,
            Integer rowIndex) {
        this(semanticRegionStructuralId, sourceComponentStructuralId, evidenceRole, evidenceKind, value,
                functionName, sourceOrder, rowIndex, null, null);
    }

    /**
     * rowIndex만으로는 row-local cell/pair 경계를 표현하지 못하므로(비대칭 행) predicate가
     * 이미 아는 cellIndexInRow(label=짝수/control=홀수)와 pairIndexInRow(유일한 pair identity는
     * (rowIndex, pairIndexInRow) 튜플)를 그대로 옮긴다. label/control 외 role은 항상 null.
     */
    public SourcePayloadEvidenceItem(
            String semanticRegionStructuralId,
            String sourceComponentStructuralId,
            String evidenceRole,
            String evidenceKind,
            String value,
            String functionName,
            int sourceOrder,
            Integer rowIndex,
            Integer cellIndexInRow,
            Integer pairIndexInRow) {
        if (semanticRegionStructuralId == null || semanticRegionStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "source_payload_evidence_item: semanticRegionStructuralId must not be null/blank");
        }
        if (sourceComponentStructuralId == null || sourceComponentStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "source_payload_evidence_item: sourceComponentStructuralId must not be null/blank");
        }
        if (evidenceRole == null || evidenceRole.trim().length() == 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: evidenceRole must not be null/blank");
        }
        if (evidenceKind == null || evidenceKind.trim().length() == 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: evidenceKind must not be null/blank");
        }
        if (sourceOrder < 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: sourceOrder must not be negative");
        }
        if (rowIndex != null && rowIndex.intValue() < 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: rowIndex must not be negative");
        }
        if (cellIndexInRow != null && cellIndexInRow.intValue() < 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: cellIndexInRow must not be negative");
        }
        if (pairIndexInRow != null && pairIndexInRow.intValue() < 0) {
            throw new IllegalArgumentException("source_payload_evidence_item: pairIndexInRow must not be negative");
        }
        this.semanticRegionStructuralId = semanticRegionStructuralId;
        this.sourceComponentStructuralId = sourceComponentStructuralId;
        this.evidenceRole = evidenceRole;
        this.evidenceKind = evidenceKind;
        this.value = value;
        this.functionName = functionName;
        this.sourceOrder = sourceOrder;
        this.rowIndex = rowIndex;
        this.cellIndexInRow = cellIndexInRow;
        this.pairIndexInRow = pairIndexInRow;
    }

    /** 이 evidence가 속한 semantic region(=predicate가 확정한 container)의 {@link
     * SourceStructuralIdentity}. Plan node의 {@code sourceStructuralId}와 그대로 대응한다. */
    public String getSemanticRegionStructuralId() { return semanticRegionStructuralId; }

    /** 이 evidence 값이 실제로 나온 leaf {@code Element} 자신의 {@link SourceStructuralIdentity}. */
    public String getSourceComponentStructuralId() { return sourceComponentStructuralId; }

    /** 이 leaf가 predicate 구조 안에서 맡은 역할(예: {@code "label"}/{@code "control"}) --
     * family마다 의미가 다를 수 있으므로 자유 문자열이되, predicate 코드 자신이 채운다(추측 없음). */
    public String getEvidenceRole() { return evidenceRole; }

    /** 이 값이 어떤 production 경로에서 나왔는지(예: {@code "source_text_attribute"}/
     * {@code "source_tag_name"}) -- payload layer가 provenance를 추적하기 위한 최소 태그. */
    public String getEvidenceKind() { return evidenceKind; }

    /** nullable -- 이 evidence가 identity/role만 나타내고 별도 primitive 값이 없는 경우 null. */
    public String getValue() { return value; }

    /** nullable -- role이 {@code "event"}일 때만 채워진다(functionName). 다른 모든 role에서는
     * 항상 {@code null}이다. */
    public String getFunctionName() { return functionName; }

    /** predicate가 이 leaf를 확정한 순서(예: row/pair 순서)를 명시적으로 부여한 값 --
     * DOM iteration이나 list 저장 순서에 우연히 의존하지 않고, 이 값 하나로 항상 같은 순서를
     * 재현하기 위함이다. */
    public int getSourceOrder() { return sourceOrder; }

    /** nullable -- role이 {@code "label"}/{@code "control"}일 때만 채워진다(0-based row 위치,
     * {@code ComponentLayoutConverter.buildTableRows}가 이미 계산한 것을 그대로 옮긴 값). 다른
     * 모든 role에서는 항상 {@code null}이다. */
    public Integer getRowIndex() { return rowIndex; }

    /** nullable -- role이 {@code "label"}/{@code "control"}일 때만 채워진다(0-based, 이 leaf가
     * 자신의 row 안에서 몇 번째 cell인지). 다른 모든 role에서는 항상 {@code null}이다. */
    public Integer getCellIndexInRow() { return cellIndexInRow; }

    /** nullable -- role이 {@code "label"}/{@code "control"}일 때만 채워진다(0-based, row-local
     * pair 순번). region 전체의 global cardinality가 아니다 -- 유일한 pair identity는 반드시
     * {@code (rowIndex, pairIndexInRow)} 튜플이다. 다른 role에서는 항상 {@code null}이다. */
    public Integer getPairIndexInRow() { return pairIndexInRow; }
}
