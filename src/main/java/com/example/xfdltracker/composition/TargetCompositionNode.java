package com.example.xfdltracker.composition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Target Composition Plan의 pure-data 노드. 검증을 통과한 {@link CompositionDecision} 하나를 그대로 투영한 것일
 * 뿐, WebSquare tag/class/style/XML DOM은 담지 않는다({@link TargetCompositionPlanBuilder}만 생성).
 * {@code nodeId}는 SOURCE_SEMANTIC이면 sourceStructuralId, TARGET_SYNTHETIC이면 caller 부여 targetSyntheticId다.
 */
public class TargetCompositionNode {

    private final String nodeId;
    private final String family;
    private final String variant;
    private final String confidence;
    private final Map<String, Object> parameters;
    private final String fallback;
    private final CompositionDecision.Origin origin;
    private final String sourceStructuralId;
    private final TargetNodeIdentity identity;

    TargetCompositionNode(
            String nodeId, String family, String variant, String confidence,
            Map<String, Object> parameters, String fallback, CompositionDecision.Origin origin,
            String sourceStructuralId, TargetNodeIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException(
                    "target_composition_node: identity must not be null -- every node must carry an already "
                            + "materialized exact (IDENTITY_KIND, IDENTITY_VALUE) tuple");
        }
        this.nodeId = nodeId;
        this.family = family;
        this.variant = variant;
        this.confidence = confidence;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(parameters));
        this.fallback = fallback;
        this.origin = origin;
        this.sourceStructuralId = sourceStructuralId;
        this.identity = identity;
    }

    public String getNodeId() { return nodeId; }
    public String getFamily() { return family; }
    public String getVariant() { return variant; }
    public String getConfidence() { return confidence; }

    /** catalog-validated parameter만 담긴, 원본 decision의 parameters를 그대로 복사한 불변 map. */
    public Map<String, Object> getParameters() { return parameters; }

    /** LOW/fallback decision이라면 그 fallback 값을 그대로 보존한다(canonical 값으로 승격하지 않음). */
    public String getFallback() { return fallback; }

    public CompositionDecision.Origin getOrigin() { return origin; }

    /** {@code origin == SOURCE_SEMANTIC}일 때만 non-null. TARGET_SYNTHETIC은 source anchor가 없다. */
    public String getSourceStructuralId() { return sourceStructuralId; }

    /** createNode()에서 이미 materialize된 exact provenance tuple을 그대로 반환한다(재계산 없음). */
    public TargetNodeIdentity getIdentity() { return identity; }

    /** Plan/Payload correlation authority의 kind 축. {@link #getIdentity()}에 delegate만 한다. */
    public TargetNodeIdentityKind getIdentityKind() {
        return identity.getKind();
    }
}
