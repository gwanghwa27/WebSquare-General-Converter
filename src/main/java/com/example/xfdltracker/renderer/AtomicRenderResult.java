package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetNodeIdentity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code TargetCompositionPlan}의 node 하나에 대한 render 결과 pure-data. family에 종속되지
 * 않는다(family/variant 판별은 Plan node가 이미 가짐). {@link #getTargetElement()}는 RENDERED일
 * 때만 non-null이며, {@link #getIdentity()}의 provenance tuple은 성공/실패 무관 항상 non-null이다.
 */
public final class AtomicRenderResult {

    private final TargetNodeIdentity identity;
    private final RenderStatus status;
    private final Element targetElement;
    private final String failureReason;
    private final Map<Integer, Element> pageContentAttachments;

    private AtomicRenderResult(
            TargetNodeIdentity identity, RenderStatus status, Element targetElement, String failureReason,
            Map<Integer, Element> pageContentAttachments) {
        if (identity == null) {
            throw new IllegalArgumentException(
                    "atomic_render_result: identity must not be null -- every result (including failures) must "
                            + "carry the exact (IDENTITY_KIND, IDENTITY_VALUE) provenance tuple intrinsically");
        }
        this.identity = identity;
        this.status = status;
        this.targetElement = targetElement;
        this.failureReason = failureReason;
        this.pageContentAttachments = Collections.unmodifiableMap(pageContentAttachments == null
                ? new LinkedHashMap<Integer, Element>() : new LinkedHashMap<Integer, Element>(pageContentAttachments));
    }

    static AtomicRenderResult rendered(TargetNodeIdentity identity, Element targetElement) {
        return new AtomicRenderResult(identity, RenderStatus.RENDERED, targetElement, null, null);
    }

    /**
     * pageContentAttachments는 정확히 {@code 0..size()-1} key set이어야 하고 모든 value는
     * non-null/targetElement subtree 내부여야 하며 같은 Element가 두 ordinal에 중복 매핑될 수
     * 없다. 위반 시 IllegalArgumentException(호출자가 잡아 INTEGRITY_VIOLATION으로 변환).
     */
    static AtomicRenderResult renderedTabControl(
            TargetNodeIdentity identity, Element targetElement, Map<Integer, Element> pageContentAttachments) {
        if (targetElement == null) {
            throw new IllegalArgumentException("atomic_render_result: renderedTabControl requires a non-null targetElement");
        }
        if (pageContentAttachments == null || pageContentAttachments.isEmpty()) {
            throw new IllegalArgumentException(
                    "atomic_render_result: renderedTabControl requires a non-empty pageContentAttachments map");
        }
        int expectedSize = pageContentAttachments.size();
        java.util.IdentityHashMap<Element, Integer> seenElements = new java.util.IdentityHashMap<Element, Integer>();
        for (int ordinal = 0; ordinal < expectedSize; ordinal++) {
            Element value = pageContentAttachments.get(Integer.valueOf(ordinal));
            if (value == null) {
                throw new IllegalArgumentException(
                        "atomic_render_result: pageContentAttachments key set is not exactly 0.."
                                + (expectedSize - 1) + " (missing ordinal=" + ordinal + ")");
            }
            if (!isDescendantOrSelf(value, targetElement)) {
                throw new IllegalArgumentException(
                        "atomic_render_result: pageContentAttachments[" + ordinal
                                + "] is not in the targetElement subtree");
            }
            Integer priorOwner = seenElements.put(value, Integer.valueOf(ordinal));
            if (priorOwner != null) {
                throw new IllegalArgumentException(
                        "atomic_render_result: the same Element instance is mapped to two ordinals ("
                                + priorOwner + " and " + ordinal + ")");
            }
        }
        return new AtomicRenderResult(identity, RenderStatus.RENDERED, targetElement, null, pageContentAttachments);
    }

    private static boolean isDescendantOrSelf(Element candidate, Element root) {
        Node current = candidate;
        while (current != null) {
            if (current == root) {
                return true;
            }
            current = current.getParentNode();
        }
        return false;
    }

    static AtomicRenderResult notSupported(TargetNodeIdentity identity, RenderStatus status, String reason) {
        return new AtomicRenderResult(identity, status, null, reason, null);
    }

    public TargetNodeIdentity getIdentity() { return identity; }
    public RenderStatus getStatus() { return status; }
    public Element getTargetElement() { return targetElement; }
    public String getFailureReason() { return failureReason; }

    /** RENDERED TAB_CONTROL 결과만 non-empty, 그 외에는 항상 빈 map(never null). 외부 불변. */
    public Map<Integer, Element> getPageContentAttachments() { return pageContentAttachments; }
}
