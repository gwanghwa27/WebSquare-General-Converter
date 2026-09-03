package com.example.xfdltracker.payload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STRUCTURED_AVAILABLE atomic leaf payload datum 하나. WebSquare tag/CSS class/XML DOM/target
 * style 문자열은 절대 담지 않는다 -- 기존 production analysis가 이미 제공하는 structured evidence만
 * 옮겨 담는 pure-data 값이다.
 */
public final class TargetLeafPayload {

    private final TargetPayloadCategory category;
    private final String value;
    private final Map<String, Object> structuredData;
    private final String sourceEvidenceKind;
    private final String sourceComponentStructuralId;
    private final TargetEventBinding finalizedTargetEventBinding;

    public TargetLeafPayload(
            TargetPayloadCategory category,
            String value,
            Map<String, Object> structuredData,
            String sourceEvidenceKind,
            String sourceComponentStructuralId) {
        this(category, value, structuredData, sourceEvidenceKind, sourceComponentStructuralId, null);
    }

    /**
     * {@code finalizedTargetEventBinding}은 {@code TargetPayloadBehaviorFinalizer}만 채운다.
     * renderer는 오직 이 값만 event correlation 권위로 읽는다. setter는 없다 --
     * finalizer는 mutate 대신 이 생성자로 새 immutable 복사본을 만든다.
     */
    public TargetLeafPayload(
            TargetPayloadCategory category,
            String value,
            Map<String, Object> structuredData,
            String sourceEvidenceKind,
            String sourceComponentStructuralId,
            TargetEventBinding finalizedTargetEventBinding) {
        if (category == null) {
            throw new IllegalArgumentException("target_leaf_payload: category must not be null");
        }
        if (sourceEvidenceKind == null || sourceEvidenceKind.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "target_leaf_payload: sourceEvidenceKind must not be null/blank -- every "
                            + "payload item must be traceable to the specific production model that "
                            + "produced it (renderer must never have to re-derive this)");
        }
        this.category = category;
        this.value = value;
        this.structuredData = Collections.unmodifiableMap(
                new LinkedHashMap<String, Object>(structuredData == null ? Collections.emptyMap() : structuredData));
        this.sourceEvidenceKind = sourceEvidenceKind;
        this.sourceComponentStructuralId = sourceComponentStructuralId;
        this.finalizedTargetEventBinding = finalizedTargetEventBinding;
    }

    public TargetPayloadCategory getCategory() { return category; }
    public String getValue() { return value; }
    public Map<String, Object> getStructuredData() { return structuredData; }
    public String getSourceEvidenceKind() { return sourceEvidenceKind; }

    /** null 가능 -- anchor Element에서 직접 나온 leaf는 null, sub-element(Button/label/control 등)에서
     * 나온 leaf는 그 sub-element의 {@code SourceStructuralIdentity}를 담는다. */
    public String getSourceComponentStructuralId() { return sourceComponentStructuralId; }

    /** null 가능(finalize 전이거나 event가 아닌 leaf). non-null이면 renderer는 이 값만 event
     * correlation 권위로 쓴다. */
    public TargetEventBinding getFinalizedTargetEventBinding() { return finalizedTargetEventBinding; }
}
