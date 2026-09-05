package com.example.xfdltracker.semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * design/xplatform_semantic_predicates.md + confidence_fallback_policy.md의 결과 모델을
 * 표현하는 순수 data model. 어떤 기존 변환 흐름에도 연결되지 않는다 -- SemanticRegionSegmenter가
 * 이 타입을 생성/소비할 때 비로소 실제 판정 로직과 연결된다.
 */
public class SemanticRegionResult {

    private String semanticType;
    private String confidence;
    private final List<String> matchedFeatures = new ArrayList<String>();
    private final List<String> contradictingFeatures = new ArrayList<String>();
    private final List<String> geometryEvidence = new ArrayList<String>();
    private final List<String> hierarchyEvidence = new ArrayList<String>();
    private final List<String> componentEvidence = new ArrayList<String>();
    private final List<String> behavioralEvidence = new ArrayList<String>();
    private String recommendedTemplateFamily;
    private String recommendedVariant;
    private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    private String fallback;

    /**
     * 서로 다른 canonical family가 같은 source region을 중복 소유하지 않는지 검증하기 위한
     * scope-qualified 경로(bare id 속성값이 아님). shadow validation 전용이며 production
     * 변환 흐름에는 쓰이지 않는다.
     */
    private String sourceRegionId;

    /**
     * anchor {@code Element}로부터 {@link SourceStructuralIdentity#build}로 계산한, DOM ancestry
     * + 형제 position만으로 결정되는 globally-unique identity(id 속성은 보지 않음). {@link
     * #sourceRegionId}와 역할이 다르며, 구조 관계 그래프가 노드 identity로 사용한다.
     */
    private String sourceStructuralId;

    public String getSemanticType() { return semanticType; }
    public void setSemanticType(String semanticType) { this.semanticType = semanticType; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public List<String> getMatchedFeatures() { return matchedFeatures; }
    public List<String> getContradictingFeatures() { return contradictingFeatures; }
    public List<String> getGeometryEvidence() { return geometryEvidence; }
    public List<String> getHierarchyEvidence() { return hierarchyEvidence; }
    public List<String> getComponentEvidence() { return componentEvidence; }
    public List<String> getBehavioralEvidence() { return behavioralEvidence; }

    public String getRecommendedTemplateFamily() { return recommendedTemplateFamily; }
    public void setRecommendedTemplateFamily(String recommendedTemplateFamily) {
        this.recommendedTemplateFamily = recommendedTemplateFamily;
    }

    public String getRecommendedVariant() { return recommendedVariant; }
    public void setRecommendedVariant(String recommendedVariant) {
        this.recommendedVariant = recommendedVariant;
    }

    public Map<String, Object> getParameters() { return parameters; }

    public String getFallback() { return fallback; }
    public void setFallback(String fallback) { this.fallback = fallback; }

    public String getSourceRegionId() { return sourceRegionId; }
    public void setSourceRegionId(String sourceRegionId) { this.sourceRegionId = sourceRegionId; }

    public String getSourceStructuralId() { return sourceStructuralId; }
    public void setSourceStructuralId(String sourceStructuralId) { this.sourceStructuralId = sourceStructuralId; }

    /**
     * predicate가 판정 시점에 이미 확인한 leaf Element에서 직접 읽은 {@link
     * SourcePayloadEvidenceItem} 목록. post-hoc DOM rediscovery로 채우지 않으며, 비어 있으면
     * payload layer가 어떤 값도 추측하지 않는다는 뜻이다.
     */
    private final List<SourcePayloadEvidenceItem> payloadEvidence = new ArrayList<SourcePayloadEvidenceItem>();

    public List<SourcePayloadEvidenceItem> getPayloadEvidence() { return payloadEvidence; }

    /**
     * SPLIT_LAYOUT.columns 전용 ordering evidence carrier. key는 column child Element의
     * sourceStructuralId, value는 exact geometry foundation이 정한 0-based left-ascending rank다.
     * consumer는 exact match가 아니라 structuralId prefix로 조회한다.
     */
    private final Map<String, Integer> splitColumnGeometryOrderBySiblingStructuralId =
            new LinkedHashMap<String, Integer>();

    public Map<String, Integer> getSplitColumnGeometryOrderBySiblingStructuralId() {
        return splitColumnGeometryOrderBySiblingStructuralId;
    }

    /**
     * BUTTON_GROUP 전용 구조적 cardinality evidence -- flattenedButtons().size()를 predicate
     * 판정 시점에 그대로 복사한다(parameters/componentEvidence 재사용 없음). BUTTON_GROUP이 아닌
     * region은 항상 null. actual leaf 개수로부터 역산 금지(trailing Button 손실 탐지 불가능해짐).
     */
    private Integer buttonGroupExpectedButtonCount;

    public Integer getButtonGroupExpectedButtonCount() { return buttonGroupExpectedButtonCount; }
    public void setButtonGroupExpectedButtonCount(Integer buttonGroupExpectedButtonCount) {
        this.buttonGroupExpectedButtonCount = buttonGroupExpectedButtonCount;
    }

    /**
     * null이면 이 region이 어떤 TAB_CONTROL의 direct TabPage 안에도 속하지 않는다는 뜻. non-null이면
     * 가장 가까운 그 TAB_CONTROL의 정확한 membership이며, predicate가 아는 DOM ancestry로부터만
     * 채워진다(문자열 prefix 추론으로 재구성하지 않음).
     */
    private TabPageMembership tabPageMembership;

    public TabPageMembership getTabPageMembership() { return tabPageMembership; }
    public void setTabPageMembership(TabPageMembership tabPageMembership) {
        this.tabPageMembership = tabPageMembership;
    }

    /**
     * control의 {@code sourceComponentStructuralId}를 key로 option 선언 resolve 결과(성공/실패
     * 모두)를 담는다(evidence 없는 control은 map에 아예 없음). BUSINESS_TABLE도 같이 채워질 수
     * 있으나 실제 소비/fail-closed 강제는 SEARCH_AREA뿐이다(extractor가 family로 gate).
     */
    private final Map<String, SourceOptionResolution> optionResolutionBySourceComponentStructuralId =
            new LinkedHashMap<String, SourceOptionResolution>();

    public Map<String, SourceOptionResolution> getOptionResolutionBySourceComponentStructuralId() {
        return optionResolutionBySourceComponentStructuralId;
    }

    /**
     * TAB_CONTROL 전용 static 구조 주소 evidence -- source id/ordinal을 typed로 보존한다. TAB_CONTROL이
     * 아닌 region은 항상 null이며, target runtime id/target XML authority가 아니다.
     */
    private TabControlStaticStructureEvidence tabControlStaticStructureEvidence;

    public TabControlStaticStructureEvidence getTabControlStaticStructureEvidence() {
        return tabControlStaticStructureEvidence;
    }

    public void setTabControlStaticStructureEvidence(
            TabControlStaticStructureEvidence tabControlStaticStructureEvidence) {
        this.tabControlStaticStructureEvidence = tabControlStaticStructureEvidence;
    }
}
