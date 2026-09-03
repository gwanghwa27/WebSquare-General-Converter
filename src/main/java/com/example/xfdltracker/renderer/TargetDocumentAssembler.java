package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetNodeIdentity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link TargetCompositionPlan}과 {@link CompositionRenderResult} 목록만 소비한다(source DOM/legacy generator 없음).
 * correlation key는 오직 {@link TargetNodeIdentity} -- 중복/orphan/missing 결과는 fail-closed.
 * root 노드만 body에 append한다(자식은 이미 CompositionRenderer가 부모 안에 붙였다).
 */
public final class TargetDocumentAssembler {

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    public Document assemble(TargetCompositionPlan plan, List<CompositionRenderResult> results) {
        if (plan == null || results == null) {
            throw new IllegalArgumentException("target_document_assembler: plan/results must not be null");
        }

        Set<TargetNodeIdentity> planIdentities = new HashSet<TargetNodeIdentity>();
        for (TargetCompositionNode node : plan.getNodes()) {
            planIdentities.add(node.getIdentity());
        }

        Map<TargetNodeIdentity, CompositionRenderResult> byIdentity =
                new HashMap<TargetNodeIdentity, CompositionRenderResult>();
        for (CompositionRenderResult result : results) {
            if (byIdentity.containsKey(result.getIdentity())) {
                throw new IllegalStateException(
                        "target_document_assembler: duplicate identity in result collection, identity="
                                + result.getIdentity());
            }
            if (!planIdentities.contains(result.getIdentity())) {
                throw new IllegalStateException(
                        "target_document_assembler: orphan result identity not present in Plan, identity="
                                + result.getIdentity());
            }
            byIdentity.put(result.getIdentity(), result);
        }
        for (TargetCompositionNode node : plan.getNodes()) {
            if (!byIdentity.containsKey(node.getIdentity())) {
                throw new IllegalStateException(
                        "target_document_assembler: missing result for required Plan node, identity="
                                + node.getIdentity());
            }
        }

        // root뿐 아니라 모든 Plan node가 RENDERED여야 한다 -- parent RENDERED + child non-RENDERED는 금지.
        for (TargetCompositionNode node : plan.getNodes()) {
            CompositionRenderResult nodeResult = byIdentity.get(node.getIdentity());
            if (nodeResult.getStatus() != CompositionRenderStatus.RENDERED) {
                throw new IllegalStateException(
                        "target_document_assembler: required Plan node is not RENDERED, identity="
                                + node.getIdentity() + " status=" + nodeResult.getStatus()
                                + " reason=" + nodeResult.getFailureReason());
            }
        }

        Document doc = newDocument();
        Element html = doc.createElementNS(XHTML_NS, "html");
        doc.appendChild(html);
        html.appendChild(doc.createElementNS(XHTML_NS, "head"));
        Element body = doc.createElementNS(XHTML_NS, "body");
        html.appendChild(body);

        // root 노드만 body에 append(자식은 이미 CompositionRenderer가 부모 element 안에 붙임).
        for (TargetCompositionNode root : plan.getRootNodes()) {
            CompositionRenderResult result = byIdentity.get(root.getIdentity());
            Element imported = (Element) doc.importNode(result.getTargetElement(), true);
            body.appendChild(imported);
        }

        return doc;
    }

    private Document newDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new IllegalStateException("target_document_assembler: failed to create target document", e);
        }
    }
}
