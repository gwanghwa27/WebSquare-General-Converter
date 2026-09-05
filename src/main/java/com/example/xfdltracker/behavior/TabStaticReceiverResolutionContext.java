package com.example.xfdltracker.behavior;

import com.example.xfdltracker.semantic.StaticTabPageEntry;
import com.example.xfdltracker.semantic.TabControlStaticStructureEvidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code TabStaticReceiverContextBuilder}만 생성하는 immutable resolution snapshot. 오직 typed
 * {@code TabControlStaticStructureEvidence}만 소비하며(source DOM 재조회 없음), exact raw-string
 * 매칭만 수행한다(trim/case-fold 없음, first-match/silent-overwrite/encounter-order 없음).
 */
public final class TabStaticReceiverResolutionContext {

    private final List<TabControlStaticStructureEvidence> evidenceList;

    TabStaticReceiverResolutionContext(List<TabControlStaticStructureEvidence> evidenceList) {
        if (evidenceList == null) {
            throw new IllegalArgumentException(
                    "tab_static_receiver_resolution_context: evidenceList must not be null");
        }
        this.evidenceList = Collections.unmodifiableList(
                new ArrayList<TabControlStaticStructureEvidence>(evidenceList));
    }

    /**
     * exact raw {@code tabControlSourceId}/{@code tabPageSourceId} 쌍만 resolve한다. 두 query
     * 값 중 하나라도 null/blank면 즉시 {@code MISSING}이다(빈 값은 유효한 receiver key가 될 수 없음).
     */
    public TabStaticReceiverResolution resolveStaticTabPage(String tabControlSourceId, String tabPageSourceId) {
        if (tabControlSourceId == null || tabControlSourceId.trim().length() == 0) {
            return TabStaticReceiverResolution.missing(
                    "tab_static_receiver_resolution_context: tabControlSourceId must not be null/blank");
        }
        if (tabPageSourceId == null || tabPageSourceId.trim().length() == 0) {
            return TabStaticReceiverResolution.missing(
                    "tab_static_receiver_resolution_context: tabPageSourceId must not be null/blank");
        }

        TabControlStaticStructureEvidence matchedTabControl = null;
        int tabControlMatchCount = 0;
        for (TabControlStaticStructureEvidence evidence : evidenceList) {
            if (tabControlSourceId.equals(evidence.getTabControlSourceId())) {
                tabControlMatchCount++;
                matchedTabControl = evidence;
            }
        }
        if (tabControlMatchCount == 0) {
            return TabStaticReceiverResolution.missing(
                    "tab_static_receiver_resolution_context: no TabControl with source id \"" + tabControlSourceId
                            + "\"");
        }
        if (tabControlMatchCount >= 2) {
            return TabStaticReceiverResolution.ambiguous(
                    "tab_static_receiver_resolution_context: " + tabControlMatchCount
                            + " TabControl matches for source id \"" + tabControlSourceId + "\"");
        }

        StaticTabPageEntry matchedPage = null;
        int pageMatchCount = 0;
        for (StaticTabPageEntry entry : matchedTabControl.getOrderedStaticPages()) {
            if (tabPageSourceId.equals(entry.getTabPageSourceId())) {
                pageMatchCount++;
                matchedPage = entry;
            }
        }
        if (pageMatchCount == 0) {
            return TabStaticReceiverResolution.missing(
                    "tab_static_receiver_resolution_context: no direct static Tabpage with source id \""
                            + tabPageSourceId + "\" in TabControl \"" + tabControlSourceId + "\"");
        }
        if (pageMatchCount >= 2) {
            return TabStaticReceiverResolution.ambiguous(
                    "tab_static_receiver_resolution_context: " + pageMatchCount
                            + " direct static Tabpage matches for source id \"" + tabPageSourceId
                            + "\" in TabControl \"" + tabControlSourceId + "\"");
        }
        return TabStaticReceiverResolution.resolved(
                matchedTabControl.getTabControlStructuralId(), matchedPage.getPageOrdinal());
    }
}
