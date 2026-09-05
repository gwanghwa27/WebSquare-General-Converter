package com.example.xfdltracker.behavior;

import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.TabControlStaticStructureEvidence;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code SemanticRegionSegmenter} 출력만 소비해 {@code TabStaticReceiverResolutionContext}를
 * 만든다(source DOM 재조회 없음). family가 TAB_CONTROL인 region만 대상으로 하며,
 * {@code componentEvidence}/{@code parameters}/payload evidence 문자열은 읽지 않는다.
 */
public final class TabStaticReceiverContextBuilder {

    private static final String TAB_CONTROL_FAMILY = "TAB_CONTROL";

    public TabStaticReceiverResolutionContext build(List<SemanticRegionResult> regions) {
        if (regions == null) {
            throw new IllegalArgumentException("tab_static_receiver_context_builder: regions must not be null");
        }
        List<TabControlStaticStructureEvidence> evidenceList = new ArrayList<TabControlStaticStructureEvidence>();
        for (SemanticRegionResult region : regions) {
            if (region == null) {
                throw new IllegalArgumentException("tab_static_receiver_context_builder: regions must not contain "
                        + "null");
            }
            if (!TAB_CONTROL_FAMILY.equals(region.getRecommendedTemplateFamily())) {
                continue;
            }
            TabControlStaticStructureEvidence evidence = region.getTabControlStaticStructureEvidence();
            if (evidence == null) {
                throw new IllegalStateException(
                        "tab_static_receiver_context_builder: TAB_CONTROL region has no typed static structure "
                                + "evidence -- architecture invariant violation, sourceStructuralId="
                                + region.getSourceStructuralId());
            }
            evidenceList.add(evidence);
        }
        return new TabStaticReceiverResolutionContext(evidenceList);
    }
}
