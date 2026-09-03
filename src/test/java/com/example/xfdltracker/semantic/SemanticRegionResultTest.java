package com.example.xfdltracker.semantic;

/**
 * {@link SemanticRegionResult}가 순수 data model로서 semantic_type/confidence/evidence/
 * recommended family·variant/parameters/fallback 필드를 round-trip으로 보존하는지 확인한다.
 * 변환 흐름과의 연결 여부는 이 test의 범위가 아니다.
 */
public class SemanticRegionResultTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testFieldsRoundTrip();
        testEvidenceListsAreIndependentMutableCollections();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testFieldsRoundTrip() {
        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType("SPLIT_LAYOUT");
        result.setConfidence("HIGH");
        result.setRecommendedTemplateFamily("SPLIT_LAYOUT");
        result.setRecommendedVariant("ratio_split");
        result.setFallback("FIXED_WIDTH_FALLBACK");
        result.getMatchedFeatures().add("component_evidence:sibling_container_pair");
        result.getContradictingFeatures().add("negative:none");
        result.getGeometryEvidence().add("column_ratio_exact_match");
        result.getHierarchyEvidence().add("n/a");
        result.getComponentEvidence().add("Div,Div");
        result.getBehavioralEvidence().add("n/a");
        result.getParameters().put("column_ratio", "3:7");

        assertEquals("semanticType", "SPLIT_LAYOUT", result.getSemanticType());
        assertEquals("confidence", "HIGH", result.getConfidence());
        assertEquals("recommendedTemplateFamily", "SPLIT_LAYOUT", result.getRecommendedTemplateFamily());
        assertEquals("recommendedVariant", "ratio_split", result.getRecommendedVariant());
        assertEquals("fallback", "FIXED_WIDTH_FALLBACK", result.getFallback());
        assertEquals("matchedFeatures.size", "1", String.valueOf(result.getMatchedFeatures().size()));
        assertEquals("contradictingFeatures.size", "1", String.valueOf(result.getContradictingFeatures().size()));
        assertEquals("geometryEvidence.size", "1", String.valueOf(result.getGeometryEvidence().size()));
        assertEquals("hierarchyEvidence.size", "1", String.valueOf(result.getHierarchyEvidence().size()));
        assertEquals("componentEvidence.size", "1", String.valueOf(result.getComponentEvidence().size()));
        assertEquals("behavioralEvidence.size", "1", String.valueOf(result.getBehavioralEvidence().size()));
        assertEquals("parameters.get(column_ratio)", "3:7", String.valueOf(result.getParameters().get("column_ratio")));
    }

    private static void testEvidenceListsAreIndependentMutableCollections() {
        SemanticRegionResult a = new SemanticRegionResult();
        SemanticRegionResult b = new SemanticRegionResult();
        a.getGeometryEvidence().add("a-only");

        assertEquals("independent instances: b.geometryEvidence.size", "0",
                String.valueOf(b.getGeometryEvidence().size()));
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
