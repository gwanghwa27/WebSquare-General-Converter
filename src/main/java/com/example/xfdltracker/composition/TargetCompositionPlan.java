package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * {@link CompositionDecision} + validated {@link SlotAssignment} 상태를 투영한 pure-data
 * intermediate structure. WebSquare XML/CSS/class는 담지 않는다. {@link TargetCompositionPlanBuilder}만 생성한다.
 */
public class TargetCompositionPlan {

    private final List<TargetCompositionNode> nodes;
    private final List<TargetCompositionEdge> edges;

    TargetCompositionPlan(List<TargetCompositionNode> nodes, List<TargetCompositionEdge> edges) {
        this.nodes = Collections.unmodifiableList(new ArrayList<TargetCompositionNode>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<TargetCompositionEdge>(edges));
    }

    public List<TargetCompositionNode> getNodes() { return nodes; }
    public List<TargetCompositionEdge> getEdges() { return edges; }

    /**
     * 어떤 edge의 child로도 등장하지 않는 node -- root/standalone. 여러 개가 정상이다(전체 screen
     * tree를 임의로 하나의 root 아래 묶지 않는다 -- DOM proximity/order로 합치지 않는다).
     */
    public List<TargetCompositionNode> getRootNodes() {
        Set<TargetCompositionNode> childNodes =
                Collections.newSetFromMap(new IdentityHashMap<TargetCompositionNode, Boolean>());
        for (TargetCompositionEdge edge : edges) {
            childNodes.add(edge.getChild());
        }
        List<TargetCompositionNode> roots = new ArrayList<TargetCompositionNode>();
        for (TargetCompositionNode node : nodes) {
            if (!childNodes.contains(node)) {
                roots.add(node);
            }
        }
        return Collections.unmodifiableList(roots);
    }
}
