package com.example.xfdltracker.composition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * catalog 규모(families=13/variants=20/parameters=20/slots=26/composition_rules=25)가 변하지
 * 않았음을 고정하는 오프라인 unit test(catalog-count lock) -- 숫자가 바뀌면 금지된 "새 source
 * predicate/새 composition rule/slot"이 실수로 추가됐다는 신호다.
 */
public class CatalogCountLockTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testFamilyCount();
        testVariantCount();
        testParameterCount();
        testSlotCount();
        testCompositionRuleCount();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static final String[] KNOWN_FAMILIES = {
            "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "PAGING", "TAB_CONTROL", "TREEVIEW",
            "BUTTON_GROUP", "SPLIT_LAYOUT", "AGREEMENT_LIST", "CATEGORY_FILTER", "INFOBOX", "LOADING_INDICATOR"
    };

    private static void testFamilyCount() throws Exception {
        assertEquals("family-count: exactly 13 known families", "13", String.valueOf(KNOWN_FAMILIES.length));
        for (String family : KNOWN_FAMILIES) {
            assertTrue("family-count: " + family + " is a known family", TemplateFamilyCatalog.isKnownFamily(family));
        }
        assertTrue("family-count: an unknown family is really unknown",
                !TemplateFamilyCatalog.isKnownFamily("NOT_A_REAL_FAMILY"));
    }

    private static void testVariantCount() throws Exception {
        int total = 0;
        for (String family : KNOWN_FAMILIES) {
            total += TemplateFamilyCatalog.get(family).getVariants().size();
        }
        assertEquals("variant-count: 20 variants total across all 13 families", "20", String.valueOf(total));
    }

    private static void testParameterCount() throws Exception {
        int total = 0;
        for (String family : KNOWN_FAMILIES) {
            total += TemplateFamilyCatalog.get(family).getParameters().size();
        }
        assertEquals("parameter-count: 20 parameters total across all 13 families", "20", String.valueOf(total));
    }

    private static void testSlotCount() throws Exception {
        int total = 0;
        for (String family : KNOWN_FAMILIES) {
            total += TemplateFamilyCatalog.get(family).getSlots().size();
        }
        assertEquals("slot-count: 26 slots total across all 13 families", "26", String.valueOf(total));
    }

    private static void testCompositionRuleCount() throws Exception {
        List<CompositionRule> rules = CompositionRuleCatalog.all();
        assertEquals("composition-rule-count: exactly 25 rules", "25", String.valueOf(rules.size()));

        Set<String> ids = new HashSet<String>();
        for (CompositionRule rule : rules) {
            ids.add(rule.getId());
        }
        assertEquals("composition-rule-count: all 25 rule ids are unique", "25", String.valueOf(ids.size()));

        int ordering = 0, slotFill = 0, merge = 0, nesting = 0, cardinality = 0;
        for (CompositionRule rule : rules) {
            switch (rule.getRuleType()) {
                case ORDERING: ordering++; break;
                case SLOT_FILL: slotFill++; break;
                case MERGE: merge++; break;
                case NESTING: nesting++; break;
                case CARDINALITY: cardinality++; break;
                default: break;
            }
        }
        assertEquals("composition-rule-count: 6 ORDERING rules", "6", String.valueOf(ordering));
        assertEquals("composition-rule-count: 6 SLOT_FILL rules", "6", String.valueOf(slotFill));
        assertEquals("composition-rule-count: 2 MERGE rules", "2", String.valueOf(merge));
        assertEquals("composition-rule-count: 6 NESTING rules", "6", String.valueOf(nesting));
        assertEquals("composition-rule-count: 5 CARDINALITY rules", "5", String.valueOf(cardinality));
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + label);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
