package com.example.xfdltracker.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component Analyzer stage output model. 6개 family(GRID/TAB_CONTROL/BUSINESS_TABLE/
 * SEARCH_AREA/TITLE_BAR/BUTTON_GROUP) 슬롯은 항상 non-null이다. SPLIT_LAYOUT 상태나
 * confidence/variant/{@code SemanticRegionResult} 권한, mutable DOM 참조는 전혀 담지 않는다.
 */
public final class ComponentPredicateAnalysis {

    private final GridFacts grid;
    private final TabControlFacts tabControl;
    private final BusinessTableFacts businessTable;
    private final SearchAreaFacts searchArea;
    private final TitleBarFacts titleBar;
    private final ButtonGroupFacts buttonGroup;

    public ComponentPredicateAnalysis(
            GridFacts grid, TabControlFacts tabControl, BusinessTableFacts businessTable,
            SearchAreaFacts searchArea, TitleBarFacts titleBar, ButtonGroupFacts buttonGroup) {
        if (grid == null || tabControl == null || businessTable == null || searchArea == null
                || titleBar == null || buttonGroup == null) {
            throw new IllegalArgumentException(
                    "component_predicate_analysis: all six family fact bundles must be non-null");
        }
        this.grid = grid;
        this.tabControl = tabControl;
        this.businessTable = businessTable;
        this.searchArea = searchArea;
        this.titleBar = titleBar;
        this.buttonGroup = buttonGroup;
    }

    public GridFacts getGrid() { return grid; }
    public TabControlFacts getTabControl() { return tabControl; }
    public BusinessTableFacts getBusinessTable() { return businessTable; }
    public SearchAreaFacts getSearchArea() { return searchArea; }
    public TitleBarFacts getTitleBar() { return titleBar; }
    public ButtonGroupFacts getButtonGroup() { return buttonGroup; }

    public static final class GridFacts {
        private final boolean matched;
        public GridFacts(boolean matched) { this.matched = matched; }
        public boolean isMatched() { return matched; }
    }

    public static final class TabControlFacts {
        private final boolean matched;
        public TabControlFacts(boolean matched) { this.matched = matched; }
        public boolean isMatched() { return matched; }
    }

    public static final class TitleBarFacts {
        private final boolean matched;
        public TitleBarFacts(boolean matched) { this.matched = matched; }
        public boolean isMatched() { return matched; }
    }

    public static final class BusinessTableFacts {
        private final boolean matched;
        private final TableStructureFacts structure;
        public BusinessTableFacts(boolean matched, TableStructureFacts structure) {
            this.matched = matched;
            this.structure = structure;
        }
        public boolean isMatched() { return matched; }
        /** nullable -- 공유 table/search 구조 evidence 단계에 도달했을 때만 non-null. */
        public TableStructureFacts getStructure() { return structure; }
    }

    public static final class SearchAreaFacts {
        private final boolean matched;
        private final TableStructureFacts structure;
        public SearchAreaFacts(boolean matched, TableStructureFacts structure) {
            this.matched = matched;
            this.structure = structure;
        }
        public boolean isMatched() { return matched; }
        /** nullable -- 공유 table/search 구조 evidence 단계에 도달했을 때만 non-null. */
        public TableStructureFacts getStructure() { return structure; }
    }

    /** {@link BusinessTableFacts}/{@link SearchAreaFacts}가 공유하는 component당 1개 구조 스냅샷. */
    public static final class TableStructureFacts {
        private final List<List<TableCellFact>> rows;
        private final boolean peerFoundGrid;
        private final String peerOpaqueBoundaryReason;

        public TableStructureFacts(
                List<List<TableCellFact>> rows, boolean peerFoundGrid, String peerOpaqueBoundaryReason) {
            if (rows == null) {
                throw new IllegalArgumentException("table_structure_facts: rows must not be null");
            }
            List<List<TableCellFact>> copy = new ArrayList<List<TableCellFact>>();
            for (List<TableCellFact> row : rows) {
                copy.add(Collections.unmodifiableList(new ArrayList<TableCellFact>(row)));
            }
            this.rows = Collections.unmodifiableList(copy);
            this.peerFoundGrid = peerFoundGrid;
            this.peerOpaqueBoundaryReason = peerOpaqueBoundaryReason;
        }

        public List<List<TableCellFact>> getRows() { return rows; }
        public boolean isPeerFoundGrid() { return peerFoundGrid; }
        /** nullable -- 기존 PeerSearchResult 값이 null인 경우(투명 boundary)와 정확히 동일. */
        public String getPeerOpaqueBoundaryReason() { return peerOpaqueBoundaryReason; }
    }

    public static final class TableCellFact {
        private final String structuralId;
        private final String sourceTagName;
        private final String textAttribute;
        private final String valueAttribute;

        public TableCellFact(String structuralId, String sourceTagName, String textAttribute, String valueAttribute) {
            this.structuralId = structuralId;
            this.sourceTagName = sourceTagName;
            this.textAttribute = textAttribute;
            this.valueAttribute = valueAttribute;
        }

        public String getStructuralId() { return structuralId; }
        public String getSourceTagName() { return sourceTagName; }
        /** nullable -- source "text" 속성 원본 유지, 없으면 null. */
        public String getTextAttribute() { return textAttribute; }
        /** nullable -- source "value" 속성 원본 유지, 없으면 null. */
        public String getValueAttribute() { return valueAttribute; }
    }

    public static final class ButtonGroupFacts {
        private final boolean matched;
        private final boolean anyWrapperMerged;
        private final String position;
        private final List<ButtonElementFact> flattenedButtons;

        public ButtonGroupFacts(
                boolean matched, boolean anyWrapperMerged, String position, List<ButtonElementFact> flattenedButtons) {
            this.matched = matched;
            this.anyWrapperMerged = anyWrapperMerged;
            this.position = position;
            this.flattenedButtons = Collections.unmodifiableList(
                    new ArrayList<ButtonElementFact>(flattenedButtons == null
                            ? Collections.<ButtonElementFact>emptyList() : flattenedButtons));
        }

        public boolean isMatched() { return matched; }
        public boolean isAnyWrapperMerged() { return anyWrapperMerged; }
        /** nullable -- 기존 determineButtonGroupPosition이 null을 반환하는 경우와 동일. */
        public String getPosition() { return position; }
        public List<ButtonElementFact> getFlattenedButtons() { return flattenedButtons; }
    }

    /** BUTTON_GROUP 전용 스냅샷. eventComponentPath를 포함해 Segmenter의 event-evidence 경로가
     *  원본 button Element를 재수집하지 않도록 한다. */
    public static final class ButtonElementFact {
        private final String structuralId;
        private final String textAttribute;
        private final String valueAttribute;
        private final String eventComponentPath;

        public ButtonElementFact(
                String structuralId, String textAttribute, String valueAttribute, String eventComponentPath) {
            this.structuralId = structuralId;
            this.textAttribute = textAttribute;
            this.valueAttribute = valueAttribute;
            this.eventComponentPath = eventComponentPath;
        }

        public String getStructuralId() { return structuralId; }
        public String getTextAttribute() { return textAttribute; }
        public String getValueAttribute() { return valueAttribute; }
        /** buildEventComponentPath 반환값 그대로(빈 문자열도 정상값). */
        public String getEventComponentPath() { return eventComponentPath; }
    }
}
