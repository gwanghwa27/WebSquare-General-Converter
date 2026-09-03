package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code atomic_template_catalog.md}/{@code template_family_matrix.md}에 적힌 13개 family의
 * variant/parameter/slot/status 값을 그대로 옮긴 순수 정적 조회 테이블(추측 보정 없음).
 * {@link SourcePredicateStatus#ACCEPTABLE}인 7개 family만 source에서 실제로 나올 수 있다.
 */
public final class TemplateFamilyCatalog {

    /** {@code atomic_template_catalog.yaml}의 {@code target_family_status}와 동일한 값. */
    public enum TargetFamilyStatus {
        CONFIRMED,
        CANDIDATE_INSUFFICIENT_EVIDENCE,
        CONFIRMED_TARGET_ONLY
    }

    /** {@code atomic_template_catalog.yaml}의 {@code source_predicate_status}와 동일한 값. */
    public enum SourcePredicateStatus {
        ACCEPTABLE,
        HOLD,
        NOT_APPLICABLE
    }

    public static final class FamilyDefinition {
        private final String name;
        private final List<String> variants;
        private final List<String> parameters;
        private final List<String> slots;
        private final TargetFamilyStatus targetFamilyStatus;
        private final SourcePredicateStatus sourcePredicateStatus;

        /** family/variant parameter-role metadata의 authoritative raw declaration store. 이
         * {@code FamilyDefinition}이 유일한 authoritative source이며, {@code ParameterRoleRegistry}는
         * 이 field를 소유하지 않고 메서드로만 읽고 쓴다(stateless facade). */
        private final Map<ParameterRoleAuthorityScope, List<String>> roleDeclarations =
                new LinkedHashMap<ParameterRoleAuthorityScope, List<String>>();

        private FamilyDefinition(
                String name, List<String> variants, List<String> parameters, List<String> slots,
                TargetFamilyStatus targetFamilyStatus, SourcePredicateStatus sourcePredicateStatus) {
            this.name = name;
            this.variants = Collections.unmodifiableList(variants);
            this.parameters = Collections.unmodifiableList(parameters);
            this.slots = Collections.unmodifiableList(slots);
            this.targetFamilyStatus = targetFamilyStatus;
            this.sourcePredicateStatus = sourcePredicateStatus;
        }

        /**
         * {@code scope}에 대한 raw role token 선언을 등록한다. 빈 리스트는 explicit empty, 호출 자체가
         * 없으면 missing이다. vocabulary/duplicate 검증은 조회 시점({@link ParameterRoleRegistry#classify})에 한다.
         * @throws IllegalStateException 같은 scope에 이미 선언이 존재(덮어쓰기/병합 금지)
         */
        void declareRole(ParameterRoleAuthorityScope scope, List<String> rawRoleTokens) {
            if (scope == null) {
                throw new IllegalArgumentException("family_definition: scope must not be null");
            }
            if (!name.equals(scope.getFamily())) {
                throw new IllegalArgumentException(
                        "family_definition: scope family=" + scope.getFamily()
                                + " does not match this FamilyDefinition=" + name);
            }
            if (rawRoleTokens == null) {
                throw new IllegalArgumentException(
                        "family_definition: rawRoleTokens must not be null (use an empty list for an "
                                + "explicit empty declaration; omitting declareRole(...) entirely represents "
                                + "missing classification)");
            }
            if (roleDeclarations.containsKey(scope)) {
                throw new IllegalStateException(
                        "family_definition: independent conflicting declaration for scope=" + scope
                                + " -- a second declareRole(...) call for the same authority key is not "
                                + "silently merged/overwritten");
            }
            roleDeclarations.put(scope, new ArrayList<String>(rawRoleTokens));
        }

        /** {@code scope}에 대해 등록된 raw role token을 그대로 반환한다(중복/순서 보존). 선언
         * 자체가 없으면 {@code null} -- 빈 리스트(explicit empty)와 절대 혼동하지 않는다. */
        List<String> getDeclaredRoleTokens(ParameterRoleAuthorityScope scope) {
            List<String> raw = roleDeclarations.get(scope);
            return raw == null ? null : Collections.unmodifiableList(raw);
        }

        public String getName() { return name; }
        public List<String> getVariants() { return variants; }
        public List<String> getParameters() { return parameters; }
        public List<String> getSlots() { return slots; }
        public TargetFamilyStatus getTargetFamilyStatus() { return targetFamilyStatus; }
        public SourcePredicateStatus getSourcePredicateStatus() { return sourcePredicateStatus; }

        public boolean hasVariant(String variant) {
            return variant != null && variants.contains(variant);
        }

        public boolean hasParameter(String parameter) {
            return parameter != null && parameters.contains(parameter);
        }

        public boolean hasSlot(String slot) {
            return slot != null && slots.contains(slot);
        }
    }

    private static final Map<String, FamilyDefinition> FAMILIES = new LinkedHashMap<String, FamilyDefinition>();

    private static void add(
            String name, String[] variants, String[] parameters, String[] slots,
            TargetFamilyStatus targetFamilyStatus, SourcePredicateStatus sourcePredicateStatus) {
        FAMILIES.put(name, new FamilyDefinition(
                name, Arrays.asList(variants), Arrays.asList(parameters), Arrays.asList(slots),
                targetFamilyStatus, sourcePredicateStatus));
    }

    static {
        // template_family_matrix.md 표를 그대로 옮김(순서도 표와 동일, family = 13개).
        add("SEARCH_AREA",
                new String[] {"basic", "additional_conditions", "compact_inline"},
                new String[] {"column_count", "row_count"},
                new String[] {"condition_rows", "additional_conditions", "search_actions"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("TITLE_BAR",
                new String[] {"title_only"},
                new String[] {"subtitle_depth", "position"},
                new String[] {"title_label", "leading_extra", "right_actions"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("BUSINESS_TABLE",
                new String[] {"horizontal", "vertical"},
                new String[] {"column_pair_count", "row_count", "col_width"},
                new String[] {"rows", "cells", "td_content"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("GRID",
                new String[] {"basic", "dual_header"},
                new String[] {"column_count", "column_width", "row_height", "visible_row_num"},
                new String[] {"columns", "row_template", "paging"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("PAGING",
                new String[] {},
                new String[] {"page_size"},
                new String[] {},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.HOLD);
        add("TAB_CONTROL",
                new String[] {"basic"},
                new String[] {"tab_count"},
                new String[] {"tabs", "panes", "adjacent_actions"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("TREEVIEW",
                new String[] {"basic"},
                new String[] {"show_tree_depth"},
                new String[] {"nodes"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.HOLD);
        add("BUTTON_GROUP",
                new String[] {"title_bar_attached", "standalone", "embedded", "fixed_footer"},
                new String[] {"position"},
                new String[] {"left_buttons", "right_buttons"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("SPLIT_LAYOUT",
                new String[] {"ratio_split", "fixed_flex", "shuttle"},
                new String[] {"column_ratio", "fixed_width_px"},
                new String[] {"columns", "transfer_controls"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.ACCEPTABLE);
        add("AGREEMENT_LIST",
                new String[] {},
                new String[] {"nesting_depth"},
                new String[] {"items", "title", "control_or_children"},
                TargetFamilyStatus.CONFIRMED, SourcePredicateStatus.HOLD);
        add("CATEGORY_FILTER",
                new String[] {"basic"},
                new String[] {"option_count"},
                new String[] {"options", "extra_filter"},
                TargetFamilyStatus.CANDIDATE_INSUFFICIENT_EVIDENCE, SourcePredicateStatus.HOLD);
        add("INFOBOX",
                new String[] {"basic"},
                new String[] {"item_count"},
                new String[] {"items"},
                TargetFamilyStatus.CANDIDATE_INSUFFICIENT_EVIDENCE, SourcePredicateStatus.HOLD);
        add("LOADING_INDICATOR",
                new String[] {"basic"},
                new String[] {},
                new String[] {},
                TargetFamilyStatus.CONFIRMED_TARGET_ONLY, SourcePredicateStatus.NOT_APPLICABLE);

        // evidence-backed 선언: 나머지 authority address는 의도적으로 undeclared(missing) 상태다.
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "row_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "column_pair_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));

        // GRID.basic/dual_header의 row_height/visible_row_num은 evidence 부족으로 의도적으로 undeclared.
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "basic", "column_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "basic", "row_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "additional_conditions", "column_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "additional_conditions", "row_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "compact_inline", "column_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "compact_inline", "row_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("TITLE_BAR", "title_only", "subtitle_depth"),
                Collections.singletonList("TARGET_VISIBLE_VALUE_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("TITLE_BAR", "title_only", "position"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "col_width"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "vertical", "column_pair_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "vertical", "row_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "vertical", "col_width"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "column_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "column_width"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("GRID", "dual_header", "column_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("GRID", "dual_header", "column_width"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.familyParameter("PAGING", "page_size"),
                Collections.singletonList("TARGET_VISIBLE_VALUE_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("TAB_CONTROL", "basic", "tab_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("TREEVIEW", "basic", "show_tree_depth"),
                Collections.singletonList("TARGET_VISIBLE_VALUE_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUTTON_GROUP", "title_bar_attached", "position"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUTTON_GROUP", "standalone", "position"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUTTON_GROUP", "embedded", "position"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("BUTTON_GROUP", "fixed_footer", "position"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "ratio_split", "column_ratio"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "ratio_split", "fixed_width_px"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "fixed_flex", "column_ratio"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "fixed_flex", "fixed_width_px"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "shuttle", "column_ratio"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "shuttle", "fixed_width_px"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.familyParameter("AGREEMENT_LIST", "nesting_depth"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("CATEGORY_FILTER", "basic", "option_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
        ParameterRoleRegistry.declare(
                ParameterRoleAuthorityScope.variantParameter("INFOBOX", "basic", "item_count"),
                Collections.singletonList("STRUCTURAL_PARTICIPATION"));
    }

    private TemplateFamilyCatalog() {
    }

    public static FamilyDefinition get(String family) {
        return family == null ? null : FAMILIES.get(family);
    }

    public static boolean isKnownFamily(String family) {
        return get(family) != null;
    }

    /**
     * catalog에 존재하고 {@link SourcePredicateStatus#ACCEPTABLE}인 family만 true. candidate/HOLD
     * family는 항상 false다.
     */
    public static boolean isSourceAcceptable(String family) {
        FamilyDefinition def = get(family);
        return def != null && def.getSourcePredicateStatus() == SourcePredicateStatus.ACCEPTABLE;
    }
}
