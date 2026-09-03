package com.example.xfdltracker.binding;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * source의 component-agnostic {@code <BindItem compid= propid= datasetid= columnid=/>} 선언
 * 하나를 그대로 보존하는 immutable evidence. compid resolution은 생성 시점에 정확히 한 번
 * id-attribute exact match로 수행되며, 이후 소비자는 이 결과만 쓰고 원본 DOM을 재조회하지 않는다.
 */
public final class SourceBindingReference {

    /** 0개/2개 이상 매치를 첫 값 선택 없이 각각 구분되는 상태로 남긴다. */
    public enum ComponentResolution {
        RESOLVED_EXACT_ONE_COMPONENT,
        UNRESOLVED_NO_COMPONENT_MATCH,
        UNRESOLVED_AMBIGUOUS_COMPONENT_MATCH
    }

    private final String bindingStructuralIdentity;
    private final String compid;
    private final String propid;
    private final String datasetid;
    private final String columnid;
    private final ComponentResolution resolution;
    private final String resolvedComponentStructuralIdentity;
    private final List<String> candidateComponentStructuralIdentities;

    public SourceBindingReference(
            String bindingStructuralIdentity, String compid, String propid, String datasetid, String columnid,
            ComponentResolution resolution, String resolvedComponentStructuralIdentity,
            List<String> candidateComponentStructuralIdentities) {
        this.bindingStructuralIdentity = bindingStructuralIdentity;
        this.compid = compid;
        this.propid = propid;
        this.datasetid = datasetid;
        this.columnid = columnid;
        this.resolution = resolution;
        this.resolvedComponentStructuralIdentity = resolvedComponentStructuralIdentity;
        // 순서 보존 + 중복 제거(id attribute 인덱싱 단계에서 이미 Element 단위로 유일하지만,
        // 이 record 자체의 불변식으로 한 번 더 보장한다) -- 첫 값 선택이 아니라 전체 후보 유지.
        Set<String> ordered = new LinkedHashSet<String>(candidateComponentStructuralIdentities == null
                ? Collections.<String>emptyList() : candidateComponentStructuralIdentities);
        this.candidateComponentStructuralIdentities =
                Collections.unmodifiableList(new java.util.ArrayList<String>(ordered));
    }

    /** BindItem 선언 자체의 structural identity (DOM ancestry 기반, id/text 무관). */
    public String getBindingStructuralIdentity() {
        return bindingStructuralIdentity;
    }

    public String getCompid() {
        return compid;
    }

    public String getPropid() {
        return propid;
    }

    public String getDatasetid() {
        return datasetid;
    }

    public String getColumnid() {
        return columnid;
    }

    public ComponentResolution getResolution() {
        return resolution;
    }

    /** {@code RESOLVED_EXACT_ONE_COMPONENT}일 때만 non-null -- compid가 가리키는 실제 Element의
     *  structural identity({@link com.example.xfdltracker.semantic.SourceStructuralIdentity} 계산값)다. */
    public String getResolvedComponentStructuralIdentity() {
        return resolvedComponentStructuralIdentity;
    }

    /** id가 매치된 모든 Element의 structural identity를 순서 보존/중복 제거해 담는다 -- ambiguous일
     *  때 후보 전체(2개 이상), unresolved일 때 빈 목록, resolved일 때 그 하나만 담긴다. */
    public List<String> getCandidateComponentStructuralIdentities() {
        return candidateComponentStructuralIdentities;
    }
}
