package com.example.xfdltracker.payload;

/**
 * {@link TargetLeafPayload}의 종류. 실제 production evidence가 존재함을 확인한 값만 담는다.
 * source sub-element provenance는 별도 enum 값이 아니라 {@link
 * TargetLeafPayload#getSourceComponentStructuralId()} 필드로 표현한다(최소 분류 원칙).
 */
public enum TargetPayloadCategory {

    /** {@code text}/{@code value} source 속성 직접 읽기(production {@code WebSquareGenerator
     * .copyBasicProperties}/tabpage label 추출과 동일한 fallback 순서를 재사용). */
    DISPLAY_TEXT,

    /** {@code BindingAnalyzer}의 {@code ComponentBinding}/{@code ItemsetBinding} 결과. */
    BINDING,

    /** {@code XfdlAnalysisResult.getEvents()}의 {@code EventBinding}(componentId/eventName/
     * functionName)만. function body/script 의미는 절대 포함하지 않는다. */
    EVENT,

    /** {@code GridFormatParser}의 {@code CellDef}/{@code GridFormat} 결과. */
    GRID_COLUMN,

    /** source 컴포넌트 태그 이름 자체(예: SEARCH_AREA pair의 control이 Edit/Combo/Calendar/
     * CheckBox/Radio 중 무엇인지) -- 이미 SemanticRegionSegmenter의 SHARED_LABEL_CONTROL_TAGS
     * 판정이 실제로 구분해 쓰는 것과 동일한 구조적 사실이다. */
    CONTROL_TYPE
}
