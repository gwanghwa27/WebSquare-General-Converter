package com.example.xfdltracker.semantic;

/**
 * TAB_CONTROL의 direct static Tabpage 하나를 나타내는 immutable evidence. tabPageSourceId는
 * trim/case-normalization/sanitization 없이 raw {@code id} 속성 값 그대로다(target runtime
 * identity가 아니다). pageOrdinal은 {@code directTabpages()} 순서의 zero-based index다.
 */
public final class StaticTabPageEntry {

    private final String tabPageSourceId;
    private final String tabPageStructuralId;
    private final int pageOrdinal;

    public StaticTabPageEntry(String tabPageSourceId, String tabPageStructuralId, int pageOrdinal) {
        if (tabPageSourceId == null) {
            throw new IllegalArgumentException("static_tab_page_entry: tabPageSourceId must not be null");
        }
        if (tabPageStructuralId == null || tabPageStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "static_tab_page_entry: tabPageStructuralId must not be null/blank");
        }
        if (pageOrdinal < 0) {
            throw new IllegalArgumentException("static_tab_page_entry: pageOrdinal must be >= 0, but was "
                    + pageOrdinal);
        }
        this.tabPageSourceId = tabPageSourceId;
        this.tabPageStructuralId = tabPageStructuralId;
        this.pageOrdinal = pageOrdinal;
    }

    /** raw {@code Tabpage} {@code id} 속성 값(blank 허용, null 불가) -- source-side resolution key일 뿐이다. */
    public String getTabPageSourceId() { return tabPageSourceId; }

    /** 이 Tabpage 자신의 {@link SourceStructuralIdentity}. */
    public String getTabPageStructuralId() { return tabPageStructuralId; }

    /** {@code directTabpages()} 순서의 zero-based index. */
    public int getPageOrdinal() { return pageOrdinal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaticTabPageEntry)) {
            return false;
        }
        StaticTabPageEntry other = (StaticTabPageEntry) o;
        return pageOrdinal == other.pageOrdinal
                && tabPageSourceId.equals(other.tabPageSourceId)
                && tabPageStructuralId.equals(other.tabPageStructuralId);
    }

    @Override
    public int hashCode() {
        int result = tabPageSourceId.hashCode();
        result = 31 * result + tabPageStructuralId.hashCode();
        result = 31 * result + pageOrdinal;
        return result;
    }

    @Override
    public String toString() {
        return "StaticTabPageEntry{tabPageSourceId=\"" + tabPageSourceId + "\", tabPageStructuralId=\""
                + tabPageStructuralId + "\", pageOrdinal=" + pageOrdinal + "}";
    }
}
