package com.example.xfdltracker.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TAB_CONTROL 하나의 static 구조 주소 evidence(source DOM {@code Element} 미보존, JSON/구분자
 * 직렬화 없음). orderedStaticPages는 {@code directTabpages()} 순서 그대로 보존된 defensive copy다.
 * source id는 target runtime identity가 아니라 source-side resolution key일 뿐이다.
 */
public final class TabControlStaticStructureEvidence {

    private final String tabControlSourceId;
    private final String tabControlStructuralId;
    private final List<StaticTabPageEntry> orderedStaticPages;

    public TabControlStaticStructureEvidence(
            String tabControlSourceId, String tabControlStructuralId, List<StaticTabPageEntry> orderedStaticPages) {
        if (tabControlSourceId == null) {
            throw new IllegalArgumentException(
                    "tab_control_static_structure_evidence: tabControlSourceId must not be null");
        }
        if (tabControlStructuralId == null || tabControlStructuralId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "tab_control_static_structure_evidence: tabControlStructuralId must not be null/blank");
        }
        if (orderedStaticPages == null) {
            throw new IllegalArgumentException(
                    "tab_control_static_structure_evidence: orderedStaticPages must not be null");
        }
        for (StaticTabPageEntry entry : orderedStaticPages) {
            if (entry == null) {
                throw new IllegalArgumentException(
                        "tab_control_static_structure_evidence: orderedStaticPages must not contain null");
            }
        }
        this.tabControlSourceId = tabControlSourceId;
        this.tabControlStructuralId = tabControlStructuralId;
        this.orderedStaticPages = Collections.unmodifiableList(new ArrayList<StaticTabPageEntry>(orderedStaticPages));
    }

    /** raw {@code Tab} {@code id} 속성 값(blank 허용, null 불가) -- source-side resolution key일 뿐이다. */
    public String getTabControlSourceId() { return tabControlSourceId; }

    /** 이 TAB_CONTROL 자신의 {@link SourceStructuralIdentity}. */
    public String getTabControlStructuralId() { return tabControlStructuralId; }

    /** {@code directTabpages()} 순서 그대로 보존된 불변 목록(정렬/중복제거 없음). */
    public List<StaticTabPageEntry> getOrderedStaticPages() { return orderedStaticPages; }
}
