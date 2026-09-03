package com.example.xfdltracker.behavior;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 대상 문서 통합 전용: 완성된 {@link Document}와 확정된 {@link TargetScriptArtifact}를 받아
 * {@code xhtml:script} 요소를 문서의 기존 {@code head}에 추가한다. 소스 분석/번역/참조 해석/DOM 접근은
 * 하지 않으며, 아티팩트를 변경하지도 않는다. 디스크 직렬화는 {@code TargetXmlSerializer}의 몫이다.
 */
public final class TargetScriptDocumentIntegrator {

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    public Document integrate(Document completedTargetDocument, TargetScriptArtifact finalizedTargetScriptArtifact) {
        if (completedTargetDocument == null) {
            throw new IllegalArgumentException(
                    "target_script_document_integrator: completedTargetDocument must not be null");
        }
        if (finalizedTargetScriptArtifact == null) {
            throw new IllegalArgumentException(
                    "target_script_document_integrator: finalizedTargetScriptArtifact must not be null");
        }
        if (finalizedTargetScriptArtifact.getFunctionsInOrder().isEmpty()) {
            return completedTargetDocument;
        }

        Element head = locateUniqueHead(completedTargetDocument);
        Element script = completedTargetDocument.createElementNS(XHTML_NS, "script");
        script.setAttribute("type", "javascript");
        script.appendChild(completedTargetDocument.createCDATASection(
                serializeArtifact(finalizedTargetScriptArtifact)));
        head.appendChild(script);
        return completedTargetDocument;
    }

    private Element locateUniqueHead(Document doc) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            throw new IllegalStateException(
                    "target_script_document_integrator: completedTargetDocument has no root element");
        }
        Element head = null;
        Node child = root.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element candidate = (Element) child;
                if ("head".equals(candidate.getLocalName()) && XHTML_NS.equals(candidate.getNamespaceURI())) {
                    if (head != null) {
                        throw new IllegalStateException(
                                "target_script_document_integrator: target document head is not uniquely "
                                        + "identifiable (more than one xhtml:head found)");
                    }
                    head = candidate;
                }
            }
            child = child.getNextSibling();
        }
        if (head == null) {
            throw new IllegalStateException(
                    "target_script_document_integrator: target document head could not be identified");
        }
        return head;
    }

    /** 함수마다 {@code scwin.<식별자> = function(<매개변수>){ <본문> };} 형태로 아티팩트 순서대로 출력. */
    private String serializeArtifact(TargetScriptArtifact artifact) {
        StringBuilder sb = new StringBuilder();
        for (TargetScwinFunctionModel function : artifact.getFunctionsInOrder()) {
            if (sb.length() > 0) { sb.append('\n'); }
            sb.append("scwin.").append(function.getIdentifier()).append(" = function(")
                    .append(String.join(", ", function.getParameters())).append("){ ")
                    .append(function.getFinalizedBodySource()).append(" };");
        }
        return sb.toString();
    }
}
