package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link SemanticRegionRelationshipExtractor}가 만든 결과를 담는 pure-data 컨테이너. 노드/관계 목록은
 * structuralId 기반이며 Element나 WebSquare/CSS/XML 정보는 담지 않는다. 노드 목록은 List다 -- 동일 Element가
 * 여러 semanticType으로 판정되면 같은 structuralId를 공유하는 노드 여러 개로 정직하게 표현한다(강제 단일화 없음).
 */
public class SemanticRegionGraph {

    private final List<SemanticRegionGraphNode> nodes = new ArrayList<SemanticRegionGraphNode>();
    private final List<SemanticRegionRelationship> relationships = new ArrayList<SemanticRegionRelationship>();

    void addNode(SemanticRegionGraphNode node) {
        if (node != null) {
            nodes.add(node);
        }
    }

    void addRelationship(SemanticRegionRelationship relationship) {
        if (relationship != null) {
            relationships.add(relationship);
        }
    }

    /** 실제로 DOM anchor Element와 대응이 확인된 노드만 담는다 -- 강제 선택되지 않은
     * ambiguous/no-emission region의 synthetic node는 없다. */
    public List<SemanticRegionGraphNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /** 이 graph에 실제로 등장하는 distinct structuralId 집합(디버깅/gate 검증용 편의 메서드). */
    public Set<String> getNodeStructuralIds() {
        Set<String> ids = new LinkedHashSet<String>();
        for (SemanticRegionGraphNode node : nodes) {
            ids.add(node.getStructuralId());
        }
        return Collections.unmodifiableSet(ids);
    }

    public List<SemanticRegionRelationship> getRelationships() {
        return Collections.unmodifiableList(relationships);
    }

    public List<SemanticRegionRelationship> getRelationshipsOfType(
            SemanticRegionRelationship.RelationshipType type) {
        List<SemanticRegionRelationship> result = new ArrayList<SemanticRegionRelationship>();
        for (SemanticRegionRelationship relationship : relationships) {
            if (relationship.getRelationshipType() == type) {
                result.add(relationship);
            }
        }
        return result;
    }
}
