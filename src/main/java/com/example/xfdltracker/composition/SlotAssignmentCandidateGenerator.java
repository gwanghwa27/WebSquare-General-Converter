package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link SemanticRegionGraph}(실제 DOM 관계)와 evaluate()된 {@link CompositionDecision} 목록을 받아,
 * {@link CompositionRuleCatalog} SLOT_FILL 규칙과 실제 관계가 동시에 성립하는 조합만 후보로 제안한다
 * ({@code assignSlot}은 호출하지 않음 -- 배정이 아니다). caller의 eligible을 신뢰하지 않고 재검증한다.
 */
public class SlotAssignmentCandidateGenerator {

    private final CompositionEvaluator integrityChecker = new CompositionEvaluator();

    /**
     * @param graph 실제 DOM 관계만 담은 그래프
     * @param decisions evaluate()로 만든 decision 목록(SOURCE_SEMANTIC 외 후보 불가, integrity 재검증 필요)
     * @throws IllegalStateException 같은 sourceStructuralId를 가진 SOURCE_SEMANTIC decision이 중복될 때
     */
    public List<SlotAssignmentCandidate> generateCandidates(
            SemanticRegionGraph graph, List<CompositionDecision> decisions) {
        List<SlotAssignmentCandidate> candidates = new ArrayList<SlotAssignmentCandidate>();
        if (graph == null || decisions == null || decisions.isEmpty()) {
            return candidates;
        }

        Map<String, List<SemanticRegionGraphNode>> nodesByStructuralId = indexNodesByStructuralId(graph);
        Map<String, CompositionDecision> decisionByStructuralId =
                selectIntegrityValidDecisions(decisions, nodesByStructuralId);
        if (decisionByStructuralId.isEmpty()) {
            return candidates;
        }

        List<SemanticRegionRelationship> containmentRelationships = new ArrayList<SemanticRegionRelationship>();
        containmentRelationships.addAll(
                graph.getRelationshipsOfType(SemanticRegionRelationship.RelationshipType.DIRECT_PARENT));
        containmentRelationships.addAll(
                graph.getRelationshipsOfType(SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS));

        for (SemanticRegionRelationship relationship : containmentRelationships) {
            CompositionDecision parent = decisionByStructuralId.get(relationship.getFromStructuralId());
            CompositionDecision child = decisionByStructuralId.get(relationship.getToStructuralId());
            if (parent == null || child == null) {
                continue;
            }
            if ("LOW".equals(parent.getConfidence())
                    && parent.getFallback() != null && parent.getFallback().length() > 0) {
                // LOW+fallback decision은 canonical structural candidate의 근거가 되지 않는다.
                continue;
            }

            TemplateFamilyCatalog.FamilyDefinition parentDef = TemplateFamilyCatalog.get(parent.getFamily());
            if (parentDef == null) {
                continue;
            }
            for (String slot : parentDef.getSlots()) {
                CompositionRule rule = CompositionRuleCatalog.slotFillRule(parent.getFamily(), slot);
                if (rule == null || !rule.getAllowedChildFamilies().contains(child.getFamily())) {
                    continue;
                }
                candidates.add(new SlotAssignmentCandidate(
                        relationship.getFromStructuralId(), relationship.getToStructuralId(),
                        parent.getFamily(), child.getFamily(), slot, rule.getId(),
                        relationship.getRelationshipType(),
                        "documented_slot_fill_rule_matches_actual_dom_containment:" + relationship.getEvidence(),
                        relationship.getSplitColumnOrderRank()));
            }
        }

        return candidates;
    }

    /** {@code structuralId -> 그 id를 가진 모든 graph node}(co-located node 지원). */
    private Map<String, List<SemanticRegionGraphNode>> indexNodesByStructuralId(SemanticRegionGraph graph) {
        Map<String, List<SemanticRegionGraphNode>> index = new LinkedHashMap<String, List<SemanticRegionGraphNode>>();
        for (SemanticRegionGraphNode node : graph.getNodes()) {
            List<SemanticRegionGraphNode> bucket = index.get(node.getStructuralId());
            if (bucket == null) {
                bucket = new ArrayList<SemanticRegionGraphNode>();
                index.put(node.getStructuralId(), bucket);
            }
            bucket.add(node);
        }
        return index;
    }

    /**
     * decision-integrity + graph-node consistency + duplicate-anchor defense를 전부 통과한
     * SOURCE_SEMANTIC decision만 {@code structuralId -> decision} map으로 반환한다. 걸러진 decision은
     * 조용히 후보에서 빠질 뿐 별도 reason을 남기지 않는다.
     */
    private Map<String, CompositionDecision> selectIntegrityValidDecisions(
            List<CompositionDecision> decisions, Map<String, List<SemanticRegionGraphNode>> nodesByStructuralId) {
        Map<String, CompositionDecision> result = new LinkedHashMap<String, CompositionDecision>();
        Set<String> seenSourceSemanticStructuralIds = new LinkedHashSet<String>();

        for (CompositionDecision decision : decisions) {
            if (decision.getOrigin() != CompositionDecision.Origin.SOURCE_SEMANTIC) {
                continue;
            }
            String structuralId = decision.getSourceStructuralId();
            if (structuralId == null || structuralId.length() == 0) {
                continue;
            }
            if (!seenSourceSemanticStructuralIds.add(structuralId)) {
                throw new IllegalStateException(
                        "duplicate_source_semantic_decision: two different CompositionDecision entries share "
                                + "sourceStructuralId=" + structuralId + " with origin=SOURCE_SEMANTIC -- this "
                                + "violates the ownership invariant that a single real DOM anchor must not carry "
                                + "two distinct SOURCE_SEMANTIC decisions; refusing to silently overwrite or pick "
                                + "first/last");
            }

            String integrityFailure =
                    integrityChecker.decisionIntegrityFailureReason(decision, "candidate_precheck", "");
            if (integrityFailure != null) {
                continue;
            }

            List<SemanticRegionGraphNode> nodesAtId = nodesByStructuralId.get(structuralId);
            if (nodesAtId == null || nodesAtId.isEmpty()) {
                continue;
            }
            boolean familyMatchesAnyNode = false;
            for (SemanticRegionGraphNode node : nodesAtId) {
                if (decision.getFamily() != null && decision.getFamily().equals(node.getSemanticType())) {
                    familyMatchesAnyNode = true;
                    break;
                }
            }
            if (!familyMatchesAnyNode) {
                // graph-node consistency 위반: 이 structuralId의 실제 anchor는 다른 family로
                // 판정되었는데, decision은 다른 family를 주장한다 -- 채택하지 않는다.
                continue;
            }

            result.put(structuralId, decision);
        }
        return result;
    }
}
