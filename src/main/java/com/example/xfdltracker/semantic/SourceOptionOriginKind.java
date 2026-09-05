package com.example.xfdltracker.semantic;

/**
 * Combo/Radio option 선언의 source 구조 구분(Slice 102D). RESOLVED로 승격 가능한 것은
 * {@link #EXTERNAL_FORM_LOCAL_DATASET_REFERENCE}뿐이며, {@link #INLINE_CHILD_DATASET}은
 * 인식만 하고 항상 fail-closed한다(범위 밖).
 */
public enum SourceOptionOriginKind {

    /** control의 {@code innerdataset} attribute가 같은 source Form 문서 안의 sibling
     * {@code Dataset} element를 참조하는 form. */
    EXTERNAL_FORM_LOCAL_DATASET_REFERENCE,

    /** control의 직계 자식으로 {@code Dataset}이 인라인 선언된 form(attribute 참조 없음). Slice
     * 102D는 이 origin kind를 항상 {@code search_area_inline_option_dataset_unproven}으로
     * fail-closed한다 -- RESOLVED evidence를 만들지 않는다. */
    INLINE_CHILD_DATASET
}
