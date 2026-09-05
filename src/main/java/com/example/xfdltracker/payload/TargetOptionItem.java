package com.example.xfdltracker.payload;

/**
 * narrow option subset(Slice 102D)의 target-side materialized option 항목 하나. source의
 * {@code SourceOptionItem}과 shape은 같지만 타입을 의도적으로 분리한다(source Dataset identity를
 * renderer가 직접 볼 수 없도록). renderer는 {@code value}/{@code label}/{@code rowOrdinal}만 읽는다.
 */
public final class TargetOptionItem {

    private final int rowOrdinal;
    private final String value;
    private final String label;

    public TargetOptionItem(int rowOrdinal, String value, String label) {
        if (rowOrdinal < 0) {
            throw new IllegalArgumentException("target_option_item: rowOrdinal must not be negative");
        }
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("target_option_item: value must not be null/empty");
        }
        if (label == null || label.length() == 0) {
            throw new IllegalArgumentException("target_option_item: label must not be null/empty");
        }
        this.rowOrdinal = rowOrdinal;
        this.value = value;
        this.label = label;
    }

    public int getRowOrdinal() { return rowOrdinal; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
}
