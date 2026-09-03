package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@code composition_rules.md}의 25개 규칙(순서 6/Slot-fill 6/Merge 2/Nesting 6/Cardinality 5)을 그대로
 * 옮긴 정적 목록이다. 문서에 없는 조합은 UNRESOLVED이며 추가하지 않는다. GRID.paging은 별도 SLOT_FILL 행 없이
 * ORDERING_6에 {@code slots=["paging"]}을 함께 실어 slot 조회가 찾도록 했다.
 */
public final class CompositionRuleCatalog {

    private static final List<CompositionRule> RULES = new ArrayList<CompositionRule>();

    private static void add(CompositionRule rule) {
        RULES.add(rule);
    }

    private static List<String> of(String... values) {
        return Arrays.asList(values);
    }

    private static final List<String> NONE = Collections.emptyList();

    static {
        // ---- 1. 순서 규칙 (evidence-backed) -- 6개 ----
        add(new CompositionRule("ORDERING_1", CompositionRule.RuleType.ORDERING,
                of("SEARCH_AREA"), NONE, of("GRID", "BUSINESS_TABLE"), null, null, null,
                "SEARCH_AREA는 항상 자신이 필터링하는 컨텐츠보다 먼저 오며, 뒤에 오는 경우는 없다"));
        add(new CompositionRule("ORDERING_2", CompositionRule.RuleType.ORDERING,
                of("TITLE_BAR"), NONE, of("GRID", "BUSINESS_TABLE", "TAB_CONTROL"), null, null, null,
                "TITLE_BAR는 자신이 제목을 붙이는 컨텐츠보다 먼저 온다"));
        add(new CompositionRule("ORDERING_3", CompositionRule.RuleType.ORDERING,
                of("CATEGORY_FILTER"), NONE, of("GRID", "BUSINESS_TABLE"), null, null, null,
                "CATEGORY_FILTER는 자신이 필터링하는 컨텐츠보다 먼저 온다"
                        + "(candidate family -- 여전히 runtime 비활성)"));
        add(new CompositionRule("ORDERING_4", CompositionRule.RuleType.ORDERING,
                of("BUTTON_GROUP"), NONE, NONE, null, null, null,
                "수정자 없는/상단 위치의 BUTTON_GROUP(title_bar_attached, bot 없음)은 섹션을 여는"
                        + " 역할이며 컨텐츠보다 먼저 온다(뒤따르는 family를 구체적으로 한정하지 않음)"));
        add(new CompositionRule("ORDERING_5", CompositionRule.RuleType.ORDERING,
                NONE, NONE, of("BUTTON_GROUP"), null, null, null,
                "bot 수정자가 붙거나 섹션 끝의 standalone/btnbox인 BUTTON_GROUP은 섹션을 닫는"
                        + " 역할이며 컨텐츠 다음에 온다(선행하는 family를 구체적으로 한정하지 않음)"));
        add(new CompositionRule("ORDERING_6", CompositionRule.RuleType.ORDERING,
                of("GRID"), of("paging"), of("PAGING"), null, null, null,
                "PAGING이 존재하는 경우 항상 GRID의 gvwbox 안에서 마지막 자식으로 위치한다"
                        + "(target-side 관계 -- PAGING source detector는 여전히 없음)"));

        // ---- 2. Slot-fill 규칙 -- 6개 ----
        add(new CompositionRule("SLOT_FILL_1", CompositionRule.RuleType.SLOT_FILL,
                of("BUSINESS_TABLE"), of("td_content"), of("BUTTON_GROUP"), null, null, null,
                "임의의 FORM_FIELD 컨트롤, 일반 텍스트/span, 아이콘 링크, 내장된 BUTTON_GROUP"
                        + "(FORM_FIELD/텍스트/아이콘 링크는 Template Family가 아니므로 여기서는"
                        + " 유일한 catalog family인 BUTTON_GROUP만 allowedChildFamilies로 표현)"));
        add(new CompositionRule("SLOT_FILL_2", CompositionRule.RuleType.SLOT_FILL,
                of("SEARCH_AREA"), of("condition_rows"), NONE, null, null, null,
                "condition_rows의 cell(td)은 임의의 FORM_FIELD 컨트롤로 채워진다"
                        + "(FORM_FIELD는 Template Family가 아니므로 allowedChildFamilies는 비어 있음)"));
        add(new CompositionRule("SLOT_FILL_3", CompositionRule.RuleType.SLOT_FILL,
                of("AGREEMENT_LIST"), of("control_or_children"), of("AGREEMENT_LIST"), null, null, null,
                "radio_group/checkbox_group(Template Family 아님) 또는 중첩된 AGREEMENT_LIST ul"
                        + "(HOLD family -- 여전히 runtime 비활성)"));
        add(new CompositionRule("SLOT_FILL_4", CompositionRule.RuleType.SLOT_FILL,
                of("SPLIT_LAYOUT"), of("columns"), of(
                        "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "TREEVIEW",
                        "BUTTON_GROUP", "SPLIT_LAYOUT"),
                null, null, null,
                "columns slot을 채울 수 있는 family 목록(SPLIT_LAYOUT 자기 자신 포함, 중첩은 미확인/WEAK)"));
        add(new CompositionRule("SLOT_FILL_5", CompositionRule.RuleType.SLOT_FILL,
                of("TAB_CONTROL"), of("panes"),
                of("GRID", "TAB_CONTROL", "SPLIT_LAYOUT", "SEARCH_AREA", "BUSINESS_TABLE", "TITLE_BAR", "BUTTON_GROUP"),
                null, null, null,
                "Slice 98BH(Round 3/6, TAB_CONTROL Exact Page Membership) -- 완전히 중첩된 TAB_CONTROL을"
                        + " 포함한 현재 7개 ACCEPTABLE source-predicate family 전부를 panes 자식으로 허용한다."
                        + " 이것은 composition permission일 뿐이다 -- 없는 semantic region을 만들거나,"
                        + " family 분류를 바꾸거나, page membership을 추론하지 않는다(어떤 자식이 실제로"
                        + " 어느 page에 배정되는지는 TabPageMembership + TargetCompositionPlanBuilder의"
                        + " exact parent-membership 검증이 별도로 결정한다). 명시된 7개 밖의 family는"
                        + " 여전히 허용되지 않는다(임의 wildcard 아님)."));
        add(new CompositionRule("SLOT_FILL_6", CompositionRule.RuleType.SLOT_FILL,
                of("GRID"), of("columns", "row_template"), NONE, null, null, null,
                "FORM_FIELD와 대응하는 컬럼 inputType(text/checkbox/radio/select/calendar/link/button)"
                        + "으로 채워진다(FORM_FIELD는 Template Family가 아니므로 allowedChildFamilies는 비어 있음)"));

        // ---- 3. Merge 가능 여부와 Merge 금지 -- 2개 ----
        add(new CompositionRule("MERGE_1", CompositionRule.RuleType.MERGE,
                NONE, NONE, NONE, null, null, null,
                "순수 시각적 wrapper인 인접 source 컨테이너는 동일 target family instance의 반복"
                        + " 가능한 slot(예: BUTTON_GROUP의 left_buttons/right_buttons)으로 merge 허용"));
        add(new CompositionRule("MERGE_2", CompositionRule.RuleType.MERGE,
                NONE, NONE, NONE, null, null, null,
                "독립적 visible/enable/event/permission boundary, runtime hide/show, business action"
                        + " grouping, 독립적 binding이 있으면 시각적으로 인접해도 merge 금지 --"
                        + " 각자 자신만의 family instance/slot 항목으로 남는다"));

        // ---- 4. Nesting 규칙 -- 6개 ----
        add(new CompositionRule("NESTING_1", CompositionRule.RuleType.NESTING,
                of("TAB_CONTROL"), NONE, NONE, null, null, Boolean.TRUE,
                "예 -- 확인됨(04 [03] 탭 + 서브 탭)"));
        add(new CompositionRule("NESTING_2", CompositionRule.RuleType.NESTING,
                of("AGREEMENT_LIST"), NONE, NONE, null, null, Boolean.TRUE,
                "예 -- 확인됨, 깊이 제한 없음(HOLD family -- 여전히 runtime 비활성)"));
        add(new CompositionRule("NESTING_3", CompositionRule.RuleType.NESTING,
                of("TREEVIEW"), NONE, NONE, null, null, Boolean.TRUE,
                "w2:node 내부 재귀(family 자체가 다른 TREEVIEW 인스턴스 안에 중첩되는 것은 아님,"
                        + " HOLD family -- 여전히 runtime 비활성)"));
        add(new CompositionRule("NESTING_4", CompositionRule.RuleType.NESTING,
                of("SPLIT_LAYOUT"), NONE, NONE, null, null, null,
                "미확인(WEAK) -- ly_column 안에 lybox가 들어간 예시 없음, true/false 어느 쪽도"
                        + " 추측하지 않는다(selfNestingAllowed=null)"));
        add(new CompositionRule("NESTING_5", CompositionRule.RuleType.NESTING,
                of("SEARCH_AREA"), NONE, NONE, null, null, Boolean.FALSE,
                "아니오 -- shbox_inner는 다른 shbox_inner 안에 중첩되지 않음"
                        + "(additional_conditions는 형제로 존재)"));
        add(new CompositionRule("NESTING_6", CompositionRule.RuleType.NESTING,
                of("BUSINESS_TABLE"), NONE, NONE, null, null, Boolean.FALSE,
                "아니오 -- tbbox는 자신의 td 안에 중첩되지 않음(복합 셀은 ly_form 사용)"));

        // ---- 5. Cardinality 요약 -- 5개 ----
        add(new CompositionRule("CARDINALITY_1", CompositionRule.RuleType.CARDINALITY,
                of("SPLIT_LAYOUT"), of("columns"), NONE, Integer.valueOf(2), null, null,
                "호스트당 최소 2, 최대 제한 없음"));
        add(new CompositionRule("CARDINALITY_2", CompositionRule.RuleType.CARDINALITY,
                of("TAB_CONTROL"), of("panes"), NONE, Integer.valueOf(2), null, null,
                "tabs/panes 최소 2, 최대 제한 없음(04 [02]에서 21개까지 확인)"));
        add(new CompositionRule("CARDINALITY_3", CompositionRule.RuleType.CARDINALITY,
                of("BUSINESS_TABLE", "SEARCH_AREA"), NONE, NONE, Integer.valueOf(1), Integer.valueOf(4), null,
                "행당 컬럼 쌍 최소 1, 최대 4(관찰된 값 -- slot 개념이 아니라 parameter 값 범위이므로"
                        + " slots는 비어 있다)"));
        add(new CompositionRule("CARDINALITY_4", CompositionRule.RuleType.CARDINALITY,
                of("AGREEMENT_LIST"), NONE, NONE, Integer.valueOf(1), null, null,
                "중첩 깊이 최소 1, 최대 제한 없음(HOLD family -- slot 개념 아님)"));
        add(new CompositionRule("CARDINALITY_5", CompositionRule.RuleType.CARDINALITY,
                of("GRID"), NONE, NONE, Integer.valueOf(1), Integer.valueOf(16), null,
                "컬럼 최소 1, 최대 16(관찰된 값 -- slot 개념이 아니라 parameter 값 범위)"));
    }

    private CompositionRuleCatalog() {
    }

    public static List<CompositionRule> all() {
        return Collections.unmodifiableList(RULES);
    }

    public static List<CompositionRule> byType(CompositionRule.RuleType type) {
        List<CompositionRule> result = new ArrayList<CompositionRule>();
        for (CompositionRule rule : RULES) {
            if (rule.getRuleType() == type) {
                result.add(rule);
            }
        }
        return result;
    }

    /** {@code family}의 {@code slot}을 채울 수 있는 규칙을 찾는다(SLOT_FILL뿐 아니라 slots를 함께 실은 ORDERING_6도 포함). */
    public static CompositionRule slotFillRule(String family, String slot) {
        for (CompositionRule rule : RULES) {
            if (!rule.getAllowedChildFamilies().isEmpty() && rule.appliesTo(family, slot)) {
                return rule;
            }
        }
        return null;
    }

    /** {@code family}의 {@code slot}에 대해 문서화된 cardinality 규칙(있으면). */
    public static CompositionRule cardinalityRule(String family, String slot) {
        for (CompositionRule rule : RULES) {
            if (rule.getRuleType() == CompositionRule.RuleType.CARDINALITY && rule.appliesTo(family, slot)) {
                return rule;
            }
        }
        return null;
    }
}
