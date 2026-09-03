package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link CompositionDecision} + validated {@link SlotAssignment} 상태를 {@link TargetCompositionPlan}으로 투영한다.
 * caller의 raw decision을 {@link CompositionEvaluator#decisionIntegrityFailureReason}로 매번 재검증하며,
 * invalid input/중복 anchor/cycle은 정규화하지 않고 {@link IllegalStateException}으로 즉시 실패한다.
 */
public class TargetCompositionPlanBuilder {

    private final CompositionEvaluator integrityChecker = new CompositionEvaluator();

    /**
     * @param rootCandidateDecisions root/standalone 후보 decision 목록(SlotAssignment child로 도달 가능한 것은
     * 순회 중 자동 발견되어 없어도 됨)
     * @throws IllegalStateException decision-integrity 재검증 실패, 중복 anchor/assignment, cycle 발견 시
     */
    public TargetCompositionPlan build(List<CompositionDecision> rootCandidateDecisions) {
        List<TargetCompositionNode> nodes = new ArrayList<TargetCompositionNode>();
        List<TargetCompositionEdge> edges = new ArrayList<TargetCompositionEdge>();
        IdentityHashMap<CompositionDecision, TargetCompositionNode> visited =
                new IdentityHashMap<CompositionDecision, TargetCompositionNode>();
        IdentityHashMap<CompositionDecision, Boolean> onStack =
                new IdentityHashMap<CompositionDecision, Boolean>();
        Map<String, CompositionDecision> structuralIdOwners = new LinkedHashMap<String, CompositionDecision>();
        Map<String, CompositionDecision> targetSyntheticIdOwners = new LinkedHashMap<String, CompositionDecision>();

        if (rootCandidateDecisions != null) {
            for (CompositionDecision root : rootCandidateDecisions) {
                if (root == null) {
                    throw new IllegalStateException(
                            "invalid_plan_input: root candidate decision list contains a null entry");
                }
                if (visited.containsKey(root)) {
                    continue; // 다른 root의 SlotAssignment 순회 과정에서 이미 생성됨.
                }
                TargetCompositionNode node = createNode(root, structuralIdOwners, targetSyntheticIdOwners);
                visited.put(root, node);
                nodes.add(node);
                visitAssignments(root, node, visited, onStack, structuralIdOwners, targetSyntheticIdOwners,
                        nodes, edges);
            }
        }

        return new TargetCompositionPlan(nodes, edges);
    }

    private void visitAssignments(
            CompositionDecision decision, TargetCompositionNode node,
            IdentityHashMap<CompositionDecision, TargetCompositionNode> visited,
            IdentityHashMap<CompositionDecision, Boolean> onStack,
            Map<String, CompositionDecision> structuralIdOwners,
            Map<String, CompositionDecision> targetSyntheticIdOwners,
            List<TargetCompositionNode> nodes, List<TargetCompositionEdge> edges) {
        List<SlotAssignment> assignments = decision.getSlotAssignments();
        if (assignments.isEmpty()) {
            return;
        }
        // SPLIT_LAYOUT.columns의 approved assignment만 geometry order로 재정렬한다(no-op for other family/slot).
        assignments = resolveSplitColumnOrderedAssignments(decision, assignments);
        if ("LOW".equals(decision.getConfidence())
                && decision.getFallback() != null && decision.getFallback().length() > 0) {
            // LOW+fallback decision은 canonical structural children을 가질 수 없다 -- assignSlot이
            // 이를 이미 거부하므로, 여기 도달했다면 방어가 우회된 것이다.
            throw new IllegalStateException(
                    "invalid_slot_assignment: LOW confidence + fallback decision (family="
                            + decision.getFamily() + ") has " + assignments.size()
                            + " slotAssignment(s) -- a LOW+fallback parent must never own canonical "
                            + "structural children");
        }

        TemplateFamilyCatalog.FamilyDefinition parentDef = TemplateFamilyCatalog.get(decision.getFamily());
        onStack.put(decision, Boolean.TRUE);
        Map<String, Integer> slotCounts = new LinkedHashMap<String, Integer>();
        Map<String, Boolean> edgeAlreadyCreated = new LinkedHashMap<String, Boolean>();

        for (SlotAssignment assignment : assignments) {
            String slot = assignment.getSlot();
            CompositionDecision child = assignment.getChild();

            if (parentDef == null || !parentDef.hasSlot(slot)) {
                throw new IllegalStateException(
                        "invalid_slot_assignment: parent family=" + decision.getFamily()
                                + " does not have slot=" + slot + " in the current catalog");
            }
            CompositionRule slotRule = CompositionRuleCatalog.slotFillRule(decision.getFamily(), slot);
            if (child == null || slotRule == null || !slotRule.getAllowedChildFamilies().contains(child.getFamily())) {
                throw new IllegalStateException(
                        "invalid_slot_assignment: slot=" + slot + " on family=" + decision.getFamily()
                                + " no longer accepts child family="
                                + (child == null ? "null" : child.getFamily()) + " under the current catalog");
            }
            if (!child.isEligible()) {
                throw new IllegalStateException(
                        "invalid_slot_assignment: child (family=" + child.getFamily() + ") for slot=" + slot
                                + " is not eligible");
            }
            String childIntegrityFailure = integrityChecker.decisionIntegrityFailureReason(child, slot, "");
            if (childIntegrityFailure != null) {
                throw new IllegalStateException("invalid_slot_assignment: " + childIntegrityFailure);
            }

            CompositionRule cardinalityRule = CompositionRuleCatalog.cardinalityRule(decision.getFamily(), slot);
            if (cardinalityRule != null && cardinalityRule.getMaxCardinality() != null) {
                Integer countSoFar = slotCounts.get(slot);
                int newCount = (countSoFar == null ? 0 : countSoFar.intValue()) + 1;
                slotCounts.put(slot, Integer.valueOf(newCount));
                if (newCount > cardinalityRule.getMaxCardinality().intValue()) {
                    throw new IllegalStateException(
                            "invalid_slot_assignment: cardinality_exceeded for slot=" + slot
                                    + " on family=" + decision.getFamily());
                }
            }

            if (onStack.containsKey(child)) {
                throw new IllegalStateException(
                        "cycle_detected: family=" + child.getFamily()
                                + " (sourceStructuralId=" + child.getSourceStructuralId()
                                + ") reappears as its own ancestor via slot=" + slot);
            }

            TargetCompositionNode childNode = visited.get(child);
            if (childNode == null) {
                childNode = createNode(child, structuralIdOwners, targetSyntheticIdOwners);
                visited.put(child, childNode);
                nodes.add(childNode);
                visitAssignments(child, childNode, visited, onStack, structuralIdOwners, targetSyntheticIdOwners,
                        nodes, edges);
            }

            String edgeKey = slot + "|" + childNode.getNodeId();
            if (edgeAlreadyCreated.containsKey(edgeKey)) {
                // 같은 (parent, slot, child) 조합이 중복 -- 조용히 정규화하지 않고 명시적으로 실패한다.
                throw new IllegalStateException(
                        "duplicate_slot_assignment: parent family=" + decision.getFamily() + " slot=" + slot
                                + " child nodeId=" + childNode.getNodeId()
                                + " appears more than once in getSlotAssignments()");
            }
            edgeAlreadyCreated.put(edgeKey, Boolean.TRUE);

            // TAB_CONTROL.panes edge만 pageOrdinal을 갖는다. child의 membership이 parent의
            // sourceStructuralId와 정확히 일치해야 하며(prefix 비교 없음), 부재/불일치는 fail closed.
            Integer pageOrdinal = null;
            if ("TAB_CONTROL".equals(decision.getFamily()) && "panes".equals(slot)) {
                com.example.xfdltracker.semantic.TabPageMembership membership = child.getTabPageMembership();
                if (membership == null) {
                    throw new IllegalStateException(
                            "tab_control_panes_membership_missing: child (family=" + child.getFamily()
                                    + ", sourceStructuralId=" + child.getSourceStructuralId()
                                    + ") has no TabPageMembership -- cannot attach to TAB_CONTROL.panes without exact "
                                    + "page membership");
                }
                if (!membership.getContainingTabControlStructuralId().equals(decision.getSourceStructuralId())) {
                    throw new IllegalStateException(
                            "tab_control_panes_membership_mismatch: child TabPageMembership.containingTabControlStructuralId="
                                    + membership.getContainingTabControlStructuralId() + " does not exactly match "
                                    + "selected parent TAB_CONTROL sourceStructuralId=" + decision.getSourceStructuralId());
                }
                pageOrdinal = Integer.valueOf(membership.getPageOrdinal());
            }
            edges.add(new TargetCompositionEdge(node, slot, childNode, pageOrdinal));
        }

        onStack.remove(decision);
    }

    /**
     * family가 {@code SPLIT_LAYOUT}이 아니면 {@code assignments}를 그대로 반환한다. SPLIT_LAYOUT이면 {@code columns}
     * slot의 원래 index 위치는 유지한 채 각 assignment의 {@link SlotAssignment#getSplitColumnOrderRank()} 값으로
     * 재배치한다(다른 slot은 건드리지 않음). fail-closed: rank가 null/중복이면 {@link IllegalStateException}.
     */
    private List<SlotAssignment> resolveSplitColumnOrderedAssignments(
            CompositionDecision decision, List<SlotAssignment> assignments) {
        if (!"SPLIT_LAYOUT".equals(decision.getFamily())) {
            return assignments;
        }
        List<Integer> columnsPositions = new ArrayList<Integer>();
        List<SlotAssignment> columns = new ArrayList<SlotAssignment>();
        for (int i = 0; i < assignments.size(); i++) {
            SlotAssignment assignment = assignments.get(i);
            if ("columns".equals(assignment.getSlot())) {
                columnsPositions.add(Integer.valueOf(i));
                columns.add(assignment);
            }
        }
        if (columns.size() < 2) {
            // 0~1개는 순서가 자명해 rank evidence 없이도 fail-closed하지 않는다. 2개 이상만 검증 대상.
            return assignments;
        }

        final IdentityHashMap<SlotAssignment, Integer> rankByAssignment =
                new IdentityHashMap<SlotAssignment, Integer>();
        Set<Integer> seenRanks = new HashSet<Integer>();
        for (SlotAssignment assignment : columns) {
            Integer rank = assignment.getSplitColumnOrderRank();
            if (rank == null) {
                CompositionDecision child = assignment.getChild();
                throw new IllegalStateException(
                        "split_layout_columns_order_unresolved: approved SPLIT_LAYOUT.columns assignment"
                                + " (child sourceStructuralId=" + (child == null ? null : child.getSourceStructuralId())
                                + ") carries no explicit NORMALIZED_HORIZONTAL_SPATIAL_RELATION rank evidence"
                                + " -- refusing to fall back to encounter order");
            }
            if (!seenRanks.add(rank)) {
                throw new IllegalStateException(
                        "split_layout_columns_order_ambiguous: duplicate geometry order rank=" + rank
                                + " among approved SPLIT_LAYOUT.columns assignments -- refusing to guess a"
                                + " total order");
            }
            rankByAssignment.put(assignment, rank);
        }

        List<SlotAssignment> sortedColumns = new ArrayList<SlotAssignment>(columns);
        Collections.sort(sortedColumns, new Comparator<SlotAssignment>() {
            public int compare(SlotAssignment a, SlotAssignment b) {
                return rankByAssignment.get(a).compareTo(rankByAssignment.get(b));
            }
        });

        List<SlotAssignment> ordered = new ArrayList<SlotAssignment>(assignments);
        for (int i = 0; i < columnsPositions.size(); i++) {
            ordered.set(columnsPositions.get(i).intValue(), sortedColumns.get(i));
        }
        return ordered;
    }

    /**
     * @return 새 {@link TargetCompositionNode}. decision-integrity 재검증 실패, SOURCE_SEMANTIC인데
     * sourceStructuralId 없음, 또는 duplicate anchor면 조용히 건너뛰지 않고
     * {@link IllegalStateException}으로 즉시 실패한다.
     */
    private TargetCompositionNode createNode(
            CompositionDecision decision, Map<String, CompositionDecision> structuralIdOwners,
            Map<String, CompositionDecision> targetSyntheticIdOwners) {
        String integrityFailure = integrityChecker.decisionIntegrityFailureReason(decision, "plan", "");
        if (integrityFailure != null) {
            throw new IllegalStateException("invalid_plan_input: " + integrityFailure);
        }

        String nodeId;
        String sourceStructuralId = null;
        if (decision.getOrigin() == CompositionDecision.Origin.SOURCE_SEMANTIC) {
            String structuralId = decision.getSourceStructuralId();
            if (structuralId == null || structuralId.length() == 0) {
                throw new IllegalStateException(
                        "invalid_plan_input: SOURCE_SEMANTIC decision (family=" + decision.getFamily()
                                + ") has no sourceStructuralId -- Plan node identity cannot be established");
            }
            CompositionDecision existingOwner = structuralIdOwners.get(structuralId);
            if (existingOwner != null && existingOwner != decision) {
                throw new IllegalStateException(
                        "duplicate_source_semantic_anchor: two different CompositionDecision entries share "
                                + "sourceStructuralId=" + structuralId + " -- refusing to silently pick either one");
            }
            structuralIdOwners.put(structuralId, decision);
            nodeId = structuralId;
            sourceStructuralId = structuralId;
        } else {
            // DFS encounter order로 node id를 부여하지 않는다 -- caller가 명시적으로 부여한
            // targetSyntheticId만 identity로 쓴다(family+parameter가 같아도 서로 다른 entity일 수 있음).
            String targetSyntheticId = decision.getTargetSyntheticId();
            if (targetSyntheticId == null || targetSyntheticId.length() == 0) {
                throw new IllegalStateException(
                        "invalid_plan_input: TARGET_SYNTHETIC decision (family=" + decision.getFamily()
                                + ") has no targetSyntheticId -- Plan node identity cannot be established "
                                + "deterministically");
            }
            CompositionDecision existingOwner = targetSyntheticIdOwners.get(targetSyntheticId);
            if (existingOwner != null && existingOwner != decision) {
                throw new IllegalStateException(
                        "duplicate_target_synthetic_id: two different CompositionDecision entries share "
                                + "targetSyntheticId=" + targetSyntheticId
                                + " -- refusing to silently pick either one");
            }
            targetSyntheticIdOwners.put(targetSyntheticId, decision);
            // SOURCE_SEMANTIC nodeId 네임스페이스와 겹치지 않도록 접두어를 붙인다.
            nodeId = "target_synthetic:" + targetSyntheticId;
        }

        TargetNodeIdentity identity = new TargetNodeIdentity(identityKindFor(decision.getOrigin()), nodeId);

        return new TargetCompositionNode(
                nodeId, decision.getFamily(), decision.getVariant(), decision.getConfidence(),
                decision.getParameters(), decision.getFallback(), decision.getOrigin(), sourceStructuralId,
                identity);
    }

    /**
     * {@code (Origin) -> TargetNodeIdentityKind} 매핑이 구체화되는 유일한 production 지점.
     * open-ended default 분기 없음 -- null/unmapped Origin은 fail closed.
     */
    private TargetNodeIdentityKind identityKindFor(CompositionDecision.Origin origin) {
        if (origin == CompositionDecision.Origin.SOURCE_SEMANTIC) {
            return TargetNodeIdentityKind.SOURCE_STRUCTURAL;
        }
        if (origin == CompositionDecision.Origin.TARGET_SYNTHETIC) {
            return TargetNodeIdentityKind.TARGET_SYNTHETIC;
        }
        throw new IllegalStateException(
                "invalid_plan_input: unmapped CompositionDecision.Origin=" + origin
                        + " -- refusing to construct exact node identity without an explicit, authorized mapping");
    }
}
