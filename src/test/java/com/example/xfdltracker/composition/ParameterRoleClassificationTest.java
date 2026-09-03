package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link ParameterRoleRegistry}(stateless facade)와 authoritative store인
 * {@link TemplateFamilyCatalog.FamilyDefinition}이 role-classification contract를 구현하는지
 * 검증한다. 34/38 address는 production이 declare했고, 나머지 4개만 이 test가 fixture로 소비한다.
 */
public class ParameterRoleClassificationTest {

    private static int failures = 0;

    private static final String[] ALL_FAMILY_NAMES = {
        "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "PAGING", "TAB_CONTROL", "TREEVIEW",
        "BUTTON_GROUP", "SPLIT_LAYOUT", "AGREEMENT_LIST", "CATEGORY_FILTER", "INFOBOX",
        "LOADING_INDICATOR"
    };

    public static void main(String[] args) throws Exception {
        // 아래 4개 unresolved fixture 주소가 다른 test에서 declare되기 전에 먼저 실행되어야 한다.
        testSlice92DeterminedAddressesMigrated();
        testSlice92UnresolvedAddressesRemainMissing();
        testSlice92FullEnumerationCountsMatch();

        testValidExplicitEmptyRoleSet();
        testMissingAndExplicitEmptyAreObservablyDistinct();
        testValidSingleStructuralRole();
        testValidSingleTargetVisibleRole();
        testValidDistinctTwoRoleSet();
        testMissingDeclaration();
        testFailureStatusesRejectEligibilityQuery();
        testDuplicateIdenticalRoleTokenFails();
        testUnknownRoleTokenFails();
        testUnknownParameterFails();
        testUnknownFamilyFails();
        testMembershipMismatchFails();
        testFamilyParameterScopeVariantlessAddressability();
        testVariantParameterScopeNonemptyVariantAddressability();
        testInvalidScopeCardinalityCombinationFails();
        testDeterministicRepeatedLookup();
        testSecondDeclareForSameScopeRejected();
        testDeclarationAuthorityIsCatalogColocated();
        testSlice71SelectedAddressMigrated();
        testSlice71VerticalRowCountNowMigrated();
        testSlice71UnrelatedAddressNowMigrated();
        testSlice72SelectedAddressMigrated();
        testSlice72PreviouslyMigratedRowCountUnchanged();
        testSlice72VerticalColumnPairCountNowMigrated();
        testSlice72ColWidthNowMigrated();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static List<String> tokens(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }

    // ---- determined-migration / unresolved-absence / full-count 검증 ----

    private static void assertMigrated(String family, String variant, String parameter, String expectedRole) {
        ParameterRoleAuthorityScope scope = (variant == null)
                ? ParameterRoleAuthorityScope.familyParameter(family, parameter)
                : ParameterRoleAuthorityScope.variantParameter(family, variant, parameter);
        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        String label = "slice92-migrated:" + scope;
        assertEquals(label + ":status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        boolean expectStructural = "STRUCTURAL_PARTICIPATION".equals(expectedRole);
        assertEquals(label + ":structural", String.valueOf(expectStructural),
                String.valueOf(result.isStructuralParticipationEligible()));
        assertEquals(label + ":target-visible", String.valueOf(!expectStructural),
                String.valueOf(result.isTargetVisibleValueParticipationEligible()));
    }

    /** Slice 91에서 결정된 34개 address 전부(frozen 2개 포함)가 정확히 그 role로 production에
     * 선언되어 있는지 검증한다. */
    private static void testSlice92DeterminedAddressesMigrated() throws Exception {
        assertMigrated("SEARCH_AREA", "basic", "column_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SEARCH_AREA", "basic", "row_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SEARCH_AREA", "additional_conditions", "column_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SEARCH_AREA", "additional_conditions", "row_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SEARCH_AREA", "compact_inline", "column_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SEARCH_AREA", "compact_inline", "row_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("TITLE_BAR", "title_only", "subtitle_depth", "TARGET_VISIBLE_VALUE_PARTICIPATION");
        assertMigrated("TITLE_BAR", "title_only", "position", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "horizontal", "column_pair_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "horizontal", "row_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "horizontal", "col_width", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "vertical", "column_pair_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "vertical", "row_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUSINESS_TABLE", "vertical", "col_width", "STRUCTURAL_PARTICIPATION");
        assertMigrated("GRID", "basic", "column_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("GRID", "basic", "column_width", "STRUCTURAL_PARTICIPATION");
        assertMigrated("GRID", "dual_header", "column_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("GRID", "dual_header", "column_width", "STRUCTURAL_PARTICIPATION");
        assertMigrated("PAGING", null, "page_size", "TARGET_VISIBLE_VALUE_PARTICIPATION");
        assertMigrated("TAB_CONTROL", "basic", "tab_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("TREEVIEW", "basic", "show_tree_depth", "TARGET_VISIBLE_VALUE_PARTICIPATION");
        assertMigrated("BUTTON_GROUP", "title_bar_attached", "position", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUTTON_GROUP", "standalone", "position", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUTTON_GROUP", "embedded", "position", "STRUCTURAL_PARTICIPATION");
        assertMigrated("BUTTON_GROUP", "fixed_footer", "position", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "ratio_split", "column_ratio", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "ratio_split", "fixed_width_px", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "fixed_flex", "column_ratio", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "fixed_flex", "fixed_width_px", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "shuttle", "column_ratio", "STRUCTURAL_PARTICIPATION");
        assertMigrated("SPLIT_LAYOUT", "shuttle", "fixed_width_px", "STRUCTURAL_PARTICIPATION");
        assertMigrated("AGREEMENT_LIST", null, "nesting_depth", "STRUCTURAL_PARTICIPATION");
        assertMigrated("CATEGORY_FILTER", "basic", "option_count", "STRUCTURAL_PARTICIPATION");
        assertMigrated("INFOBOX", "basic", "item_count", "STRUCTURAL_PARTICIPATION");
    }

    /** 남은 4개 unresolved address는 production이 절대 declare하지 않았어야 한다 -- 이 test가
     * main() 앞부분에서 실행되어야 다른 test의 declare fixture로 소비되기 전 상태를 확인한다. */
    private static void testSlice92UnresolvedAddressesRemainMissing() throws Exception {
        String[][] unresolved = {
            {"GRID", "basic", "row_height"},
            {"GRID", "dual_header", "row_height"},
            {"GRID", "basic", "visible_row_num"},
            {"GRID", "dual_header", "visible_row_num"}
        };
        for (String[] addr : unresolved) {
            ParameterRoleAuthorityScope scope =
                    ParameterRoleAuthorityScope.variantParameter(addr[0], addr[1], addr[2]);
            ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
            assertEquals("slice92-unresolved-still-missing:" + scope, "MISSING_INCOMPLETE_CLASSIFICATION",
                    result.getStatus().name());
        }
    }

    /** 전체 38개 address 공간을 catalog 자신의 variants/parameters로부터 다시 enumerate해서,
     * duplicate/missing-for-determined/extra/cross-wire/fake-sentinel이 전혀 없음을 종합적으로
     * 검증한다. */
    private static void testSlice92FullEnumerationCountsMatch() throws Exception {
        int total = 0;
        int structural = 0;
        int targetVisible = 0;
        int unresolved = 0;

        for (String familyName : ALL_FAMILY_NAMES) {
            TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(familyName);
            List<String> variants = def.getVariants();
            List<String> parameters = def.getParameters();

            if (variants.isEmpty()) {
                for (String parameter : parameters) {
                    total++;
                    ParameterRoleAuthorityScope scope =
                            ParameterRoleAuthorityScope.familyParameter(familyName, parameter);
                    ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
                    if ("VALID_EXPLICIT_ROLE_SET".equals(result.getStatus().name())) {
                        if (result.isStructuralParticipationEligible()) {
                            structural++;
                        } else if (result.isTargetVisibleValueParticipationEligible()) {
                            targetVisible++;
                        } else {
                            assertTrue("slice92-enum: unexpected empty role set at " + scope, false);
                        }
                    } else if ("MISSING_INCOMPLETE_CLASSIFICATION".equals(result.getStatus().name())) {
                        unresolved++;
                    } else {
                        assertTrue("slice92-enum: unexpected status " + result.getStatus().name()
                                + " at " + scope, false);
                    }
                }
            } else {
                for (String variant : variants) {
                    for (String parameter : parameters) {
                        total++;
                        ParameterRoleAuthorityScope scope =
                                ParameterRoleAuthorityScope.variantParameter(familyName, variant, parameter);
                        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
                        if ("VALID_EXPLICIT_ROLE_SET".equals(result.getStatus().name())) {
                            if (result.isStructuralParticipationEligible()) {
                                structural++;
                            } else if (result.isTargetVisibleValueParticipationEligible()) {
                                targetVisible++;
                            } else {
                                assertTrue("slice92-enum: unexpected empty role set at " + scope, false);
                            }
                        } else if ("MISSING_INCOMPLETE_CLASSIFICATION".equals(result.getStatus().name())) {
                            unresolved++;
                        } else {
                            assertTrue("slice92-enum: unexpected status " + result.getStatus().name()
                                    + " at " + scope, false);
                        }
                    }
                }
            }
        }

        assertEquals("slice92-enum: total address count", "38", String.valueOf(total));
        assertEquals("slice92-enum: structural count", "31", String.valueOf(structural));
        assertEquals("slice92-enum: target-visible count", "3", String.valueOf(targetVisible));
        assertEquals("slice92-enum: unresolved count", "4", String.valueOf(unresolved));
    }

    // ---- 범용 registry 메커니즘 테스트 ----

    private static void testValidExplicitEmptyRoleSet() throws Exception {
        // fixture A -- production이 declare하지 않는 4개 address 중 하나.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "row_height");
        ParameterRoleRegistry.declare(scope, tokens());

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("explicit-empty: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("explicit-empty: roles set is empty", result.getRoles().isEmpty());
        assertFalse("explicit-empty: structural ineligible", result.isStructuralParticipationEligible());
        assertFalse("explicit-empty: target-visible ineligible", result.isTargetVisibleValueParticipationEligible());
    }

    private static void testMissingAndExplicitEmptyAreObservablyDistinct() throws Exception {
        // fixture B (GRID.dual_header.row_height) -- read-only, 아직 missing 상태(나중에 소비됨).
        ParameterRoleAuthorityScope missingScope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "dual_header", "row_height");

        // fixture A 재사용, 위에서 이미 explicit-empty로 declare됨 -- read-only, 두 번째 declare() 없음.
        ParameterRoleAuthorityScope explicitEmptyScope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "row_height");

        ParameterRoleClassification missingResult = ParameterRoleRegistry.classify(missingScope);
        ParameterRoleClassification explicitEmptyResult = ParameterRoleRegistry.classify(explicitEmptyScope);

        assertEquals("missing-vs-empty: missing status", "MISSING_INCOMPLETE_CLASSIFICATION",
                missingResult.getStatus().name());
        assertEquals("missing-vs-empty: explicit-empty status", "VALID_EXPLICIT_ROLE_SET",
                explicitEmptyResult.getStatus().name());
        assertTrue("missing-vs-empty: statuses differ",
                missingResult.getStatus() != explicitEmptyResult.getStatus());
    }

    private static void testValidSingleStructuralRole() throws Exception {
        // SEARCH_AREA.basic.column_count는 migrated 상태(STRUCTURAL_PARTICIPATION) -- read-only 재사용.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("SEARCH_AREA", "basic", "column_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("single-structural: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("single-structural: structural eligible", result.isStructuralParticipationEligible());
        assertFalse("single-structural: target-visible ineligible",
                result.isTargetVisibleValueParticipationEligible());
    }

    private static void testValidSingleTargetVisibleRole() throws Exception {
        // PAGING.page_size는 migrated 상태(TARGET_VISIBLE_VALUE_PARTICIPATION) -- read-only 재사용.
        ParameterRoleAuthorityScope scope = ParameterRoleAuthorityScope.familyParameter("PAGING", "page_size");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("single-target-visible: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertFalse("single-target-visible: structural ineligible", result.isStructuralParticipationEligible());
        assertTrue("single-target-visible: target-visible eligible",
                result.isTargetVisibleValueParticipationEligible());
    }

    private static void testValidDistinctTwoRoleSet() throws Exception {
        // fixture D (GRID.dual_header.visible_row_num) -- migrated address 중 두 role을 동시에
        // 가진 것이 없어서, 아직 미확정인 새 address가 여기서 필요하다.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "dual_header", "visible_row_num");
        ParameterRoleRegistry.declare(scope, tokens("STRUCTURAL_PARTICIPATION", "TARGET_VISIBLE_VALUE_PARTICIPATION"));

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("two-role: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("two-role: structural eligible", result.isStructuralParticipationEligible());
        assertTrue("two-role: target-visible eligible", result.isTargetVisibleValueParticipationEligible());
    }

    private static void testMissingDeclaration() throws Exception {
        // fixture C (GRID.basic.visible_row_num) -- read-only, 아직 missing 상태(나중에 소비됨).
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "visible_row_num");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("missing: status", "MISSING_INCOMPLETE_CLASSIFICATION", result.getStatus().name());
        assertTrue("missing: reason present", result.getReason() != null);
    }

    private static void testFailureStatusesRejectEligibilityQuery() throws Exception {
        // fixture C 재사용(여전히 missing, read-only, 나중에 소비됨).
        ParameterRoleAuthorityScope missingScope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "visible_row_num");
        ParameterRoleClassification missingResult = ParameterRoleRegistry.classify(missingScope);

        boolean threw = false;
        try {
            missingResult.isStructuralParticipationEligible();
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("fail-closed: eligibility query on MISSING throws instead of returning false", threw);

        ParameterRoleAuthorityScope unknownScope =
                ParameterRoleAuthorityScope.familyParameter("NOT_A_REAL_FAMILY", "x");
        ParameterRoleClassification unknownResult = ParameterRoleRegistry.classify(unknownScope);
        boolean threw2 = false;
        try {
            unknownResult.isTargetVisibleValueParticipationEligible();
        } catch (IllegalStateException e) {
            threw2 = true;
        }
        assertTrue("fail-closed: eligibility query on UNKNOWN_PARAMETER throws instead of returning false",
                threw2);
    }

    private static void testDuplicateIdenticalRoleTokenFails() throws Exception {
        // fixture B (GRID.dual_header.row_height)를 소비함, 지금까지는 read-only였음.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "dual_header", "row_height");
        ParameterRoleRegistry.declare(scope, tokens("STRUCTURAL_PARTICIPATION", "STRUCTURAL_PARTICIPATION"));

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("duplicate-token: status", "MALFORMED_CONFLICTING_CLASSIFICATION", result.getStatus().name());
        assertTrue("duplicate-token: reason mentions duplicate",
                result.getReason() != null && result.getReason().indexOf("duplicate") >= 0);
    }

    private static void testUnknownRoleTokenFails() throws Exception {
        // fixture C (GRID.basic.visible_row_num)를 소비함, 지금까지는 read-only였음.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "visible_row_num");
        ParameterRoleRegistry.declare(scope, tokens("NOT_A_REAL_ROLE"));

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("unknown-token: status", "MALFORMED_CONFLICTING_CLASSIFICATION", result.getStatus().name());
    }

    private static void testUnknownParameterFails() throws Exception {
        // family는 known(PAGING)이지만 parameter key가 그 family에 없음 -- declare 없이 classify.
        // "not_a_real_parameter"는 PAGING의 실제 parameter가 아니므로, page_size가 migrated된
        // 것과 무관하게 scope identity 자체가 다르다(충돌 없음).
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.familyParameter("PAGING", "not_a_real_parameter");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("unknown-parameter: status", "UNKNOWN_PARAMETER", result.getStatus().name());
    }

    private static void testUnknownFamilyFails() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.familyParameter("NOT_A_REAL_FAMILY", "anything");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("unknown-family: status", "UNKNOWN_PARAMETER", result.getStatus().name());
    }

    private static void testMembershipMismatchFails() throws Exception {
        ParameterRoleAuthorityScope scope = ParameterRoleAuthorityScope.variantParameter(
                "BUSINESS_TABLE", "not_a_real_variant", "row_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("membership-mismatch: status", "MALFORMED_CONFLICTING_CLASSIFICATION",
                result.getStatus().name());
        assertTrue("membership-mismatch: reason mentions membership_mismatch",
                result.getReason() != null && result.getReason().indexOf("membership_mismatch") >= 0);
    }

    private static void testFamilyParameterScopeVariantlessAddressability() throws Exception {
        // AGREEMENT_LIST.nesting_depth는 migrated 상태(FAMILY_PARAMETER kind) -- read-only 재사용.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.familyParameter("AGREEMENT_LIST", "nesting_depth");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("variantless-addressability: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
    }

    private static void testVariantParameterScopeNonemptyVariantAddressability() throws Exception {
        // GRID.basic.column_count는 migrated 상태(STRUCTURAL_PARTICIPATION) -- read-only 재사용.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("GRID", "basic", "column_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("variant-addressability: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("variant-addressability: structural eligible", result.isStructuralParticipationEligible());
    }

    private static void testInvalidScopeCardinalityCombinationFails() throws Exception {
        // FAMILY_PARAMETER scope를 variant-bearing family(BUSINESS_TABLE)에 억지로 적용 -- classify만
        // 하므로(declare 없음) production의 BUSINESS_TABLE.horizontal.row_count(VARIANT_PARAMETER,
        // 다른 Kind이므로 scope identity가 다름) declaration과 충돌하지 않는다.
        ParameterRoleAuthorityScope familyScopeOnVariantBearingFamily =
                ParameterRoleAuthorityScope.familyParameter("BUSINESS_TABLE", "row_count");
        ParameterRoleClassification resultA = ParameterRoleRegistry.classify(familyScopeOnVariantBearingFamily);
        assertEquals("invalid-scope-shape(FAMILY_PARAMETER on variant-bearing family): status",
                "MALFORMED_CONFLICTING_CLASSIFICATION", resultA.getStatus().name());

        // VARIANT_PARAMETER scope를 variantless family(PAGING)에 억지로 적용 -- scope identity가
        // production의 FAMILY_PARAMETER(PAGING,page_size) declaration과 다르므로(Kind가 다름)
        // 충돌하지 않는다.
        ParameterRoleAuthorityScope variantScopeOnVariantlessFamily = ParameterRoleAuthorityScope.variantParameter(
                "PAGING", "some_variant", "page_size");
        ParameterRoleClassification resultB = ParameterRoleRegistry.classify(variantScopeOnVariantlessFamily);
        assertEquals("invalid-scope-shape(VARIANT_PARAMETER on variantless family): status",
                "MALFORMED_CONFLICTING_CLASSIFICATION", resultB.getStatus().name());
    }

    private static void testDeterministicRepeatedLookup() throws Exception {
        // SPLIT_LAYOUT.ratio_split.column_ratio는 migrated 상태 -- read-only 반복 조회.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "ratio_split", "column_ratio");

        ParameterRoleClassification first = ParameterRoleRegistry.classify(scope);
        ParameterRoleClassification second = ParameterRoleRegistry.classify(scope);
        assertEquals("deterministic: repeated status equal", first.getStatus().name(), second.getStatus().name());
        assertEquals("deterministic: repeated eligibility equal",
                String.valueOf(first.isStructuralParticipationEligible()),
                String.valueOf(second.isStructuralParticipationEligible()));

        // scope 값 동등성으로도 같은 declaration에 도달해야 한다 -- 새 scope 객체(동일 identity).
        ParameterRoleAuthorityScope sameScopeNewInstance =
                ParameterRoleAuthorityScope.variantParameter("SPLIT_LAYOUT", "ratio_split", "column_ratio");
        ParameterRoleClassification third = ParameterRoleRegistry.classify(sameScopeNewInstance);
        assertEquals("deterministic: equal-scope new instance same status",
                first.getStatus().name(), third.getStatus().name());
    }

    private static void testSecondDeclareForSameScopeRejected() throws Exception {
        // TAB_CONTROL.basic.tab_count는 이미 production에 의해 declare되어 있음 -- 이 test가
        // 자체적으로 첫 declare()를 호출하지 않고도, 다시 declare하면 거부되어야 한다.
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("TAB_CONTROL", "basic", "tab_count");

        boolean threw = false;
        try {
            ParameterRoleRegistry.declare(scope, tokens("TARGET_VISIBLE_VALUE_PARTICIPATION"));
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("independent-conflicting-declaration: second declare(...) for same scope rejected", threw);
    }

    /** {@link ParameterRoleRegistry}는 stateless facade일 뿐이고, authoritative data는
     * {@link TemplateFamilyCatalog.FamilyDefinition} 자신이 들고 있음을 확인한다 -- registry를
     * 거치지 않고 {@code getDeclaredRoleTokens}로 직접 읽어도 동일 데이터가 보여야 한다. */
    private static void testDeclarationAuthorityIsCatalogColocated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("CATEGORY_FILTER", "basic", "option_count");

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get("CATEGORY_FILTER");
        List<String> raw = def.getDeclaredRoleTokens(scope);
        assertTrue("catalog-colocated: FamilyDefinition itself exposes the raw declaration",
                raw != null && raw.size() == 1 && "STRUCTURAL_PARTICIPATION".equals(raw.get(0)));
    }

    /** production static initializer가 직접 declare한 상태를, 이 test는 declare()를 다시
     * 호출하지 않고 classify/조회만 한다(중복 declare는 재호출 시 예외가 되므로 하지 않는다). */
    private static void testSlice71SelectedAddressMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "row_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice71-migrated: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("slice71-migrated: structural eligible", result.isStructuralParticipationEligible());
        assertFalse("slice71-migrated: target-visible ineligible",
                result.isTargetVisibleValueParticipationEligible());

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get("BUSINESS_TABLE");
        List<String> raw = def.getDeclaredRoleTokens(scope);
        assertTrue("slice71-migrated: raw token directly queryable from catalog/model",
                raw != null && raw.size() == 1 && "STRUCTURAL_PARTICIPATION".equals(raw.get(0)));
    }

    /** BUSINESS_TABLE.vertical.row_count는 한때 missing이었으나 이제 production이 declare한
     * migrated address다. */
    private static void testSlice71VerticalRowCountNowMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "vertical", "row_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice92-vertical-row-count-migrated: status", "VALID_EXPLICIT_ROLE_SET",
                result.getStatus().name());
        assertTrue("slice92-vertical-row-count-migrated: structural eligible",
                result.isStructuralParticipationEligible());
    }

    /** INFOBOX.basic.item_count는 한때 완전히 무관한 missing 주소였으나 이제 production이
     * declare한 migrated address다. */
    private static void testSlice71UnrelatedAddressNowMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("INFOBOX", "basic", "item_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice92-infobox-item-count-migrated: status", "VALID_EXPLICIT_ROLE_SET",
                result.getStatus().name());
        assertTrue("slice92-infobox-item-count-migrated: structural eligible",
                result.isStructuralParticipationEligible());
    }

    /** production static initializer가 직접 declare한 상태를 재호출 없이 classify/조회만 한다.
     * row_count declaration과 완전히 독립적이다. */
    private static void testSlice72SelectedAddressMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "column_pair_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice72-migrated: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("slice72-migrated: structural eligible", result.isStructuralParticipationEligible());
        assertFalse("slice72-migrated: target-visible ineligible",
                result.isTargetVisibleValueParticipationEligible());

        TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get("BUSINESS_TABLE");
        List<String> raw = def.getDeclaredRoleTokens(scope);
        assertTrue("slice72-migrated: raw token directly queryable from catalog/model",
                raw != null && raw.size() == 1 && "STRUCTURAL_PARTICIPATION".equals(raw.get(0)));
    }

    /** Slice 72 (B) -- Slice 71에서 migration된 row_count가 이번 Slice의 column_pair_count
     * migration으로 인해 변경되지 않았음을 재확인한다(두 declaration의 독립성). */
    private static void testSlice72PreviouslyMigratedRowCountUnchanged() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "row_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice72-row-count-unchanged: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("slice72-row-count-unchanged: structural eligible", result.isStructuralParticipationEligible());
        assertFalse("slice72-row-count-unchanged: target-visible ineligible",
                result.isTargetVisibleValueParticipationEligible());
    }

    /** BUSINESS_TABLE.vertical.column_pair_count는 한때 missing이었으나 이제 production이
     * declare한 migrated address다. */
    private static void testSlice72VerticalColumnPairCountNowMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "vertical", "column_pair_count");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice92-vertical-column-pair-count-migrated: status", "VALID_EXPLICIT_ROLE_SET",
                result.getStatus().name());
        assertTrue("slice92-vertical-column-pair-count-migrated: structural eligible",
                result.isStructuralParticipationEligible());
    }

    /** BUSINESS_TABLE.horizontal.col_width는 한때 missing이었으나 이제 production이 declare한
     * migrated address다. */
    private static void testSlice72ColWidthNowMigrated() throws Exception {
        ParameterRoleAuthorityScope scope =
                ParameterRoleAuthorityScope.variantParameter("BUSINESS_TABLE", "horizontal", "col_width");

        ParameterRoleClassification result = ParameterRoleRegistry.classify(scope);
        assertEquals("slice92-col-width-migrated: status", "VALID_EXPLICIT_ROLE_SET", result.getStatus().name());
        assertTrue("slice92-col-width-migrated: structural eligible", result.isStructuralParticipationEligible());
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("[FAIL] " + label + " -- expected=<" + expected + "> actual=<" + actual + ">");
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            System.out.println("[FAIL] " + label);
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertFalse(String label, boolean condition) {
        assertTrue(label, !condition);
    }
}
