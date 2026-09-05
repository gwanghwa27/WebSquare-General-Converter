package com.example.xfdltracker.semantic;

/**
 * source Dataset의 Row 하나에서 결정적으로 추출한 option 항목 하나(Slice 102D). value/label은
 * source Row의 실제 semantic text 그대로이며(trim 등 정규화 없음), {@code rowOrdinal}은 그 Row가
 * source {@code Rows} 안에서 차지하는 0-based document order다(재정렬/추정 없음).
 */
public final class SourceOptionItem {

    private final int rowOrdinal;
    private final String value;
    private final String label;

    public SourceOptionItem(int rowOrdinal, String value, String label) {
        if (rowOrdinal < 0) {
            throw new IllegalArgumentException("source_option_item: rowOrdinal must not be negative");
        }
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("source_option_item: value must not be null/empty");
        }
        if (label == null || label.length() == 0) {
            throw new IllegalArgumentException("source_option_item: label must not be null/empty");
        }
        this.rowOrdinal = rowOrdinal;
        this.value = value;
        this.label = label;
    }

    public int getRowOrdinal() { return rowOrdinal; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
}
