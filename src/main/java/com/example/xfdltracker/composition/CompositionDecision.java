package com.example.xfdltracker.composition;

import com.example.xfdltracker.semantic.TabPageMembership;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code SemanticRegionResult -> Atomic Template Catalog -> Composition Decision} 연결의 마지막
 * 단계인 pure-data 결과 모델. WebSquare CSS/class/XML node는 담지 않으며, 실제 target DOM 렌더러는
 * 이 phase의 범위가 아니다.
 */
public class CompositionDecision {

    /**
     * 이 decision이 어디서 만들어졌는지 표시한다. {@code null}은 아직 어느 사전 경로도 거치지 않은
     * decision을 뜻하며, {@link CompositionEvaluator#assignSlot}은 origin이 null인 child를 항상 거부한다.
     * setOrigin/setEligible은 package-private이나, 진짜 방어선은 여전히 assignSlot의 재검증이다.
     */
    public enum Origin {
        /** {@link CompositionEvaluator#evaluate}가 실제 source {@code SemanticRegionResult}를
         * catalog와 대조해서 만든 decision. */
        SOURCE_SEMANTIC,
        /** {@link CompositionEvaluator#createTargetSyntheticDecision}이 target-side composition
         * invariant(예: PAGING) 검증 목적으로만 만든 decision. source detector/emission으로
         * 계산되지 않는다. */
        TARGET_SYNTHETIC
    }

    private String sourceRegionId;

    /**
     * {@code SemanticRegionResult.sourceStructuralId}를 그대로 복사한 pure metadata 필드(추측/재계산 없음).
     * TARGET_SYNTHETIC decision은 source anchor가 없으므로 null이어도 정상이다.
     * {@code SemanticRegionGraph} join에는 반드시 이 필드만 쓴다({@link #getSourceRegionId()}는 쓰지 않음).
     */
    private String sourceStructuralId;

    /**
     * {@code origin == TARGET_SYNTHETIC} decision에 caller가 부여하는 explicit, deterministic
     * identity. random UUID나 traversal encounter order로 채우지 않으며(같은 family+parameter라도
     * 서로 다른 entity일 수 있음), 비어 있으면 {@link TargetCompositionPlanBuilder}가 거부한다.
     */
    private String targetSyntheticId;

    private String family;
    private String variant;
    private String confidence;
    private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    private final List<SlotAssignment> slotAssignments = new ArrayList<SlotAssignment>();
    private String fallback;
    private boolean eligible;
    private Origin origin;
    private final List<String> reasons = new ArrayList<String>();

    public Origin getOrigin() { return origin; }
    /** package-private -- {@link CompositionEvaluator}만 설정한다(mutability audit, 위 클래스 javadoc 참고). */
    void setOrigin(Origin origin) { this.origin = origin; }

    public String getSourceRegionId() { return sourceRegionId; }
    public void setSourceRegionId(String sourceRegionId) { this.sourceRegionId = sourceRegionId; }

    public String getSourceStructuralId() { return sourceStructuralId; }
    /** package-private -- {@link CompositionEvaluator}만 설정한다. */
    void setSourceStructuralId(String sourceStructuralId) { this.sourceStructuralId = sourceStructuralId; }

    public String getTargetSyntheticId() { return targetSyntheticId; }
    /** package-private -- {@link CompositionEvaluator#createTargetSyntheticDecision(String, Map, String)}만 설정한다. */
    void setTargetSyntheticId(String targetSyntheticId) { this.targetSyntheticId = targetSyntheticId; }

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    /** catalog에 존재하지 않는 parameter는 절대 여기에 채우지 않는다({@link CompositionEvaluator} 참고). */
    public Map<String, Object> getParameters() { return parameters; }

    /**
     * {@link CompositionEvaluator#assignSlot}의 catalog-backed 검증(존재하는 slot인지, catalog
     * rule이 허용하는 자식 family인지, cardinality를 넘지 않는지)을 통과한 배정만 여기 쌓인다.
     * 직접 add해서 검증을 우회하지 말 것 -- {@code assignSlot}을 통해서만 채워야 한다.
     */
    public List<SlotAssignment> getSlotAssignments() { return slotAssignments; }

    public String getFallback() { return fallback; }
    public void setFallback(String fallback) { this.fallback = fallback; }

    /**
     * catalog-backed validation을 전부 통과했는지(family가 source-acceptable이고 variant/parameter가
     * 모두 catalog에 존재함). false면 이 decision은 향후 실제 구조를 만드는 데 쓰여서는 안 된다는 뜻이며,
     * 이유는 {@link #getReasons()}에 남는다.
     */
    public boolean isEligible() { return eligible; }
    /** package-private -- {@link CompositionEvaluator}만 설정한다(mutability audit, 위 클래스 javadoc 참고). */
    void setEligible(boolean eligible) { this.eligible = eligible; }

    /** eligible=false가 된 이유, 또는 eligible=true라도 LOW/fallback 등 참고할 evidence. */
    public List<String> getReasons() { return reasons; }

    /**
     * {@code SemanticRegionResult.tabPageMembership}을 그대로 복사한 pure metadata. null이면 이
     * source region이 어떤 TAB_CONTROL page에도 속하지 않는다.
     */
    private TabPageMembership tabPageMembership;

    public TabPageMembership getTabPageMembership() { return tabPageMembership; }
    /** package-private -- {@link CompositionEvaluator}만 설정한다(다른 setter들과 동일한
     * mutability 원칙). */
    void setTabPageMembership(TabPageMembership tabPageMembership) { this.tabPageMembership = tabPageMembership; }
}
