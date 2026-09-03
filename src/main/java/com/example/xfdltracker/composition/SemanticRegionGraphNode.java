package com.example.xfdltracker.composition;

/**
 * {@link SemanticRegionGraph}의 노드 하나. 순수 data이며 Element는 담지 않는다. {@code sourceRegionId}는
 * non-Div scope에서 서로 다른 Element가 같은 값을 가질 수 있어 참고/디버깅용일 뿐이며, relationship endpoint는
 * 오직 DOM ancestry로 계산된 {@code structuralId}만 사용한다.
 */
public class SemanticRegionGraphNode {

    private final String structuralId;
    private final String sourceRegionId;
    private final String semanticType;

    public SemanticRegionGraphNode(String structuralId, String sourceRegionId, String semanticType) {
        this.structuralId = structuralId;
        this.sourceRegionId = sourceRegionId;
        this.semanticType = semanticType;
    }

    /** Graph 전용, 실제 DOM 위치로부터 계산된 globally-unique identity. relationship endpoint로 쓰인다. */
    public String getStructuralId() { return structuralId; }

    /** 기존 production correlation key(참고/디버깅용) -- non-Div scope에서는 다른 노드끼리 값이 같을 수 있다. */
    public String getSourceRegionId() { return sourceRegionId; }

    public String getSemanticType() { return semanticType; }
}
