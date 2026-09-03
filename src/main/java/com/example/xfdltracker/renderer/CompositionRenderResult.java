package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetNodeIdentity;
import org.w3c.dom.Element;

/**
 * {@link CompositionRenderer}가 node 하나에 대해 만드는 결과 pure-data. {@link
 * AtomicRenderResult}와 동일한 shape 계약: {@link #getTargetElement()}는 RENDERED일 때만
 * non-null, {@link #getIdentity()}의 provenance tuple은 status 무관 항상 non-null이다.
 */
public final class CompositionRenderResult {

    private final TargetNodeIdentity identity;
    private final CompositionRenderStatus status;
    private final Element targetElement;
    private final String failureReason;

    private CompositionRenderResult(
            TargetNodeIdentity identity, CompositionRenderStatus status, Element targetElement,
            String failureReason) {
        if (identity == null) {
            throw new IllegalArgumentException(
                    "composition_render_result: identity must not be null -- every result (including failures) "
                            + "must carry the exact (IDENTITY_KIND, IDENTITY_VALUE) provenance tuple intrinsically");
        }
        this.identity = identity;
        this.status = status;
        this.targetElement = targetElement;
        this.failureReason = failureReason;
    }

    static CompositionRenderResult composed(TargetNodeIdentity identity, Element targetElement) {
        return new CompositionRenderResult(identity, CompositionRenderStatus.RENDERED, targetElement, null);
    }

    static CompositionRenderResult notComposed(
            TargetNodeIdentity identity, CompositionRenderStatus status, String reason) {
        return new CompositionRenderResult(identity, status, null, reason);
    }

    public TargetNodeIdentity getIdentity() { return identity; }
    public CompositionRenderStatus getStatus() { return status; }
    public Element getTargetElement() { return targetElement; }
    public String getFailureReason() { return failureReason; }
}
