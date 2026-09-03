package com.example.xfdltracker.semantic;

/**
 * 이 region이 어느 TAB_CONTROL의 어느 direct TabPage 안에서 만들어졌는지 담는 immutable
 * pure-data. containingTabControlStructuralId는 정확한 SourceStructuralIdentity, pageOrdinal은
 * directTabpages() 순서의 zero-based index다. tab label/DOM 참조는 담지 않는다.
 */
public final class TabPageMembership {

    private final String containingTabControlStructuralId;
    private final int pageOrdinal;

    public TabPageMembership(String containingTabControlStructuralId, int pageOrdinal) {
        if (containingTabControlStructuralId == null || containingTabControlStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "tab_page_membership: containingTabControlStructuralId must not be null/blank");
        }
        if (pageOrdinal < 0) {
            throw new IllegalArgumentException(
                    "tab_page_membership: pageOrdinal must be >= 0, but was " + pageOrdinal);
        }
        this.containingTabControlStructuralId = containingTabControlStructuralId;
        this.pageOrdinal = pageOrdinal;
    }

    public String getContainingTabControlStructuralId() { return containingTabControlStructuralId; }
    public int getPageOrdinal() { return pageOrdinal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TabPageMembership)) {
            return false;
        }
        TabPageMembership other = (TabPageMembership) o;
        return pageOrdinal == other.pageOrdinal
                && containingTabControlStructuralId.equals(other.containingTabControlStructuralId);
    }

    @Override
    public int hashCode() {
        return 31 * containingTabControlStructuralId.hashCode() + pageOrdinal;
    }

    @Override
    public String toString() {
        return "TabPageMembership{containingTabControlStructuralId=\"" + containingTabControlStructuralId
                + "\", pageOrdinal=" + pageOrdinal + "}";
    }
}
