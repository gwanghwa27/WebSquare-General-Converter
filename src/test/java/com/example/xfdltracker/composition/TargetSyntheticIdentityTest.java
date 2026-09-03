package com.example.xfdltracker.composition;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TargetSyntheticIdentity}가 explicit {@code stableDiscriminator}로 같은 parent/slot/
 * family 아래 서로 다른 논리적 TARGET_SYNTHETIC entity를 구별하면서도(false collision 없음)
 * 결정적인지(입력 순서 무관, random/index/counter 미사용)를 검증하는 오프라인 unit test.
 */
public class TargetSyntheticIdentityTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSameContextSameDiscriminatorSameIdentity();
        testSameContextDifferentDiscriminatorDistinctIdentity();
        testDifferentParentSyntheticIdentityDistinct();
        testDifferentSlotOrFamilyProducesDistinctIdentity();
        testOrderIndependenceWithExplicitDiscriminator();
        testTwoDistinctSyntheticDecisionsUnderSameParentSlotNoCollisionInPlan();
        testDifferentGridsPagingIdentityDistinct();
        testRejectsParentWithoutEstablishedIdentity();
        testRejectsEmptySlotOrFamilyOrDiscriminator();
        testTargetSyntheticParentChain();

        // ---- Collision-Safe Synthetic Identity 인코딩 최종본 ----
        testDelimiterAmbiguityNoLongerCollides();
        testNestedParentIdentityStableUnderCollisionSafeEncoding();
        testDiscriminatorArbitrarySeparatorContentNoCollision();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 같은 (parent, slot, family, discriminator) 조합을 몇 번을 다시 호출해도 항상 동일한 identity가 나온다. */
    private static void testSameContextSameDiscriminatorSameIdentity() throws Exception {
        CompositionDecision parent = sourceSemanticGrid("Form[0]/Grid[0]");
        String id1 = TargetSyntheticIdentity.build(parent, "paging", "PAGING", "only_paging_instance");
        String id2 = TargetSyntheticIdentity.build(parent, "paging", "PAGING", "only_paging_instance");
        assertEquals("same-discriminator: repeated calls produce the identical id", id1, id2);

        // 완전히 새로운 (하지만 논리적으로 같은) parent 객체 인스턴스로도 동일해야 한다 -- object
        // identity가 아니라 semantic identity(구조적 경로 문자열)에만 의존하기 때문.
        CompositionDecision parentAgain = sourceSemanticGrid("Form[0]/Grid[0]");
        String id3 = TargetSyntheticIdentity.build(parentAgain, "paging", "PAGING", "only_paging_instance");
        assertEquals("same-discriminator: a different but logically-identical parent object yields the "
                + "same id", id1, id3);
    }

    /** 같은 parent/slot/family라도 discriminator가 다르면 서로 다른 논리적 entity로 취급돼
     * identity가 달라진다(same-context multi-synthetic). */
    private static void testSameContextDifferentDiscriminatorDistinctIdentity() throws Exception {
        CompositionDecision parent = sourceSemanticGrid("Form[0]/Grid[0]");
        String idFirst = TargetSyntheticIdentity.build(parent, "paging", "PAGING", "first_paging_entity");
        String idSecond = TargetSyntheticIdentity.build(parent, "paging", "PAGING", "second_paging_entity");
        assertTrue("distinct-discriminator: different discriminators under the same parent/slot/family "
                + "produce distinct identities", !idFirst.equals(idSecond));
    }

    /** parent가 다르면(다른 sourceStructuralId) discriminator가 같아도 identity가 달라진다. */
    private static void testDifferentParentSyntheticIdentityDistinct() throws Exception {
        CompositionDecision parentA = sourceSemanticGrid("Form[0]/Grid[0]");
        CompositionDecision parentB = sourceSemanticGrid("Form[0]/Grid[1]");
        String idA = TargetSyntheticIdentity.build(parentA, "paging", "PAGING", "only_paging_instance");
        String idB = TargetSyntheticIdentity.build(parentB, "paging", "PAGING", "only_paging_instance");
        assertTrue("distinct-parent: different parents produce distinct identities", !idA.equals(idB));
    }

    /** 같은 parent/discriminator라도 slot이나 targetFamily가 다르면 identity도 달라진다. */
    private static void testDifferentSlotOrFamilyProducesDistinctIdentity() throws Exception {
        CompositionDecision parent = sourceSemanticGrid("Form[0]/Grid[0]");
        String idPaging = TargetSyntheticIdentity.build(parent, "paging", "PAGING", "d");
        String idOtherSlot = TargetSyntheticIdentity.build(parent, "columns", "PAGING", "d");
        String idOtherFamily = TargetSyntheticIdentity.build(parent, "paging", "OTHER_FAMILY", "d");
        assertTrue("distinct-slot: different slot produces a different id", !idPaging.equals(idOtherSlot));
        assertTrue("distinct-family: different targetFamily produces a different id",
                !idPaging.equals(idOtherFamily));
    }

    /** helper로 만든 explicit discriminator identity를 factory에 넘겨 assignSlot까지 성공시키고,
     * 결과 Plan에서 입력 순서를 바꿔도 동일한 PAGING identity가 나오는지 확인한다. */
    private static void testOrderIndependenceWithExplicitDiscriminator() throws Exception {
        CompositionDecision gridA = sourceSemanticGrid("Form[0]/Grid[0]");
        CompositionDecision gridC = sourceSemanticGrid("Form[0]/Grid[1]");

        CompositionEvaluator evaluator = new CompositionEvaluator();
        // test 전용 stable discriminator -- production PAGING discriminator policy를 새로 만들지
        // 않는다(production TARGET_SYNTHETIC call count는 여전히 0).
        String pagingIdA = TargetSyntheticIdentity.build(gridA, "paging", "PAGING", "the_one_paging_instance");
        String pagingIdC = TargetSyntheticIdentity.build(gridC, "paging", "PAGING", "the_one_paging_instance");
        CompositionDecision pagingA = evaluator.createTargetSyntheticDecision("PAGING", null, pagingIdA);
        CompositionDecision pagingC = evaluator.createTargetSyntheticDecision("PAGING", null, pagingIdC);
        assertTrue("order-independence: assignSlot(A) succeeds", evaluator.assignSlot(gridA, "paging", pagingA));
        assertTrue("order-independence: assignSlot(C) succeeds", evaluator.assignSlot(gridC, "paging", pagingC));

        List<CompositionDecision> orderAC = new ArrayList<CompositionDecision>();
        orderAC.add(gridA);
        orderAC.add(gridC);
        TargetCompositionPlan planAC = new TargetCompositionPlanBuilder().build(orderAC);

        List<CompositionDecision> orderCA = new ArrayList<CompositionDecision>();
        orderCA.add(gridC);
        orderCA.add(gridA);
        TargetCompositionPlan planCA = new TargetCompositionPlanBuilder().build(orderCA);

        String nodeIdInAC = childNodeIdForParent(planAC, "Form[0]/Grid[0]");
        String nodeIdInCA = childNodeIdForParent(planCA, "Form[0]/Grid[0]");
        assertEquals("order-independence: same logical PAGING identity regardless of root list order",
                nodeIdInAC, nodeIdInCA);

        // 재생성(같은 논리적 GRID.paging을 다시 helper로 계산)해도 동일한 identity가 나와야 한다.
        String pagingIdARecomputed =
                TargetSyntheticIdentity.build(gridA, "paging", "PAGING", "the_one_paging_instance");
        assertEquals("order-independence: recomputing the identity for the same logical entity yields "
                + "the same value", pagingIdA, pagingIdARecomputed);
    }

    /** 같은 parent/slot/family 아래 distinct discriminator를 가진 두 TARGET_SYNTHETIC decision을
     * 둘 다 assignSlot 성공시키고 build하면, false collision 없이 2 node + 2 edge가 만들어져야 한다. */
    private static void testTwoDistinctSyntheticDecisionsUnderSameParentSlotNoCollisionInPlan() throws Exception {
        CompositionDecision grid = sourceSemanticGrid("Form[0]/Grid[0]");
        CompositionEvaluator evaluator = new CompositionEvaluator();

        String idFirst = TargetSyntheticIdentity.build(grid, "paging", "PAGING", "first_paging_entity");
        String idSecond = TargetSyntheticIdentity.build(grid, "paging", "PAGING", "second_paging_entity");
        CompositionDecision firstPaging = evaluator.createTargetSyntheticDecision("PAGING", null, idFirst);
        CompositionDecision secondPaging = evaluator.createTargetSyntheticDecision("PAGING", null, idSecond);

        assertTrue("no-false-collision: assignSlot(first) succeeds",
                evaluator.assignSlot(grid, "paging", firstPaging));
        assertTrue("no-false-collision: assignSlot(second) succeeds",
                evaluator.assignSlot(grid, "paging", secondPaging));

        List<CompositionDecision> roots = new ArrayList<CompositionDecision>();
        roots.add(grid);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(roots);

        assertEquals("no-false-collision: plan has exactly 3 nodes (grid + 2 distinct PAGING entities)",
                "3", String.valueOf(plan.getNodes().size()));
        assertEquals("no-false-collision: plan has exactly 2 edges (no silent merge, no rejected collision)",
                "2", String.valueOf(plan.getEdges().size()));
        assertTrue("no-false-collision: the two PAGING node ids are distinct",
                !plan.getEdges().get(0).getChild().getNodeId().equals(plan.getEdges().get(1).getChild().getNodeId()));
    }

    /** 서로 다른 GRID의 PAGING은(같은 discriminator를 써도) 서로 다른 identity를 갖는다. */
    private static void testDifferentGridsPagingIdentityDistinct() throws Exception {
        CompositionDecision gridA = sourceSemanticGrid("Form[0]/Grid[5]");
        CompositionDecision gridB = sourceSemanticGrid("Form[0]/Grid[6]");
        String pagingIdA = TargetSyntheticIdentity.build(gridA, "paging", "PAGING", "the_one_paging_instance");
        String pagingIdB = TargetSyntheticIdentity.build(gridB, "paging", "PAGING", "the_one_paging_instance");
        assertTrue("distinct-grids-paging: two different GRIDs' PAGING identities are distinct",
                !pagingIdA.equals(pagingIdB));
    }

    /** parent의 identity가 아직 확정되지 않았으면(sourceStructuralId 비어 있음) 거부한다. */
    private static void testRejectsParentWithoutEstablishedIdentity() throws Exception {
        CompositionDecision parentWithoutId = new CompositionDecision();
        parentWithoutId.setFamily("GRID");
        parentWithoutId.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        parentWithoutId.setEligible(true);
        // sourceStructuralId를 세팅하지 않음(비어 있음).

        boolean threw = false;
        try {
            TargetSyntheticIdentity.build(parentWithoutId, "paging", "PAGING", "d");
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue("rejects-no-identity: build() throws for a parent with no established identity", threw);
    }

    /** slot/targetFamily/stableDiscriminator가 비어 있으면 거부한다. */
    private static void testRejectsEmptySlotOrFamilyOrDiscriminator() throws Exception {
        CompositionDecision parent = sourceSemanticGrid("Form[0]/Grid[0]");
        boolean threwForSlot = false;
        try {
            TargetSyntheticIdentity.build(parent, "", "PAGING", "d");
        } catch (IllegalArgumentException expected) {
            threwForSlot = true;
        }
        assertTrue("rejects-empty-slot: build() throws for an empty slot", threwForSlot);

        boolean threwForFamily = false;
        try {
            TargetSyntheticIdentity.build(parent, "paging", null, "d");
        } catch (IllegalArgumentException expected) {
            threwForFamily = true;
        }
        assertTrue("rejects-empty-family: build() throws for a null targetFamily", threwForFamily);

        boolean threwForBlankDiscriminator = false;
        try {
            TargetSyntheticIdentity.build(parent, "paging", "PAGING", "   ");
        } catch (IllegalArgumentException expected) {
            threwForBlankDiscriminator = true;
        }
        assertTrue("rejects-blank-discriminator: build() throws for a blank stableDiscriminator",
                threwForBlankDiscriminator);

        boolean threwForNullDiscriminator = false;
        try {
            TargetSyntheticIdentity.build(parent, "paging", "PAGING", null);
        } catch (IllegalArgumentException expected) {
            threwForNullDiscriminator = true;
        }
        assertTrue("rejects-null-discriminator: build() throws for a null stableDiscriminator",
                threwForNullDiscriminator);
    }

    /** parent 자신이 TARGET_SYNTHETIC이어도 그 값을 그대로 parent identity로 써서 다음 단계
     * identity를 만들 수 있다(체인 가능성). 같은 체인을 두 번 재구성해도 동일한 결과가 나온다. */
    private static void testTargetSyntheticParentChain() throws Exception {
        CompositionDecision level1Parent = sourceSemanticGrid("Form[0]/Grid[0]");
        CompositionEvaluator evaluator = new CompositionEvaluator();

        String level1IdFirst = TargetSyntheticIdentity.build(level1Parent, "paging", "PAGING", "d1");
        CompositionDecision syntheticParentFirst =
                evaluator.createTargetSyntheticDecision("PAGING", null, level1IdFirst);
        String chainedFirst = TargetSyntheticIdentity.build(
                syntheticParentFirst, "some_hypothetical_slot", "OTHER_FAMILY", "d2");

        String level1IdSecond = TargetSyntheticIdentity.build(level1Parent, "paging", "PAGING", "d1");
        CompositionDecision syntheticParentSecond =
                evaluator.createTargetSyntheticDecision("PAGING", null, level1IdSecond);
        String chainedSecond = TargetSyntheticIdentity.build(
                syntheticParentSecond, "some_hypothetical_slot", "OTHER_FAMILY", "d2");

        assertEquals("synthetic-parent-chain: rebuilding the same 2-level chain independently yields the "
                + "same final identity", chainedFirst, chainedSecond);
        assertTrue("synthetic-parent-chain: the chained (level-2) identity differs from the level-1 identity "
                + "it was built from", !chainedFirst.equals(level1IdFirst));
    }

    /** 회귀: nested 2단계 체인 튜플과 flat 단일 단계 튜플이 raw {@code "#"} concatenation이라면
     * byte-for-byte 같은 문자열을 냈다 -- collision-safe encoding 적용 후에는 서로 달라야 한다. */
    private static void testDelimiterAmbiguityNoLongerCollides() throws Exception {
        CompositionDecision level1Parent = sourceSemanticGrid("Form[0]/Grid[0]");
        String level1Id = TargetSyntheticIdentity.build(level1Parent, "paging", "PAGING", "d1");
        CompositionDecision level1SyntheticDecision =
                new CompositionEvaluator().createTargetSyntheticDecision("PAGING", null, level1Id);

        // Tuple A: nested chain -- parent identity 자체가 이미 level-1 build에서 나온
        // "#"처럼 인코딩된 metadata를 포함하고 있다.
        String tupleAIdentity =
                TargetSyntheticIdentity.build(level1SyntheticDecision, "childslot", "CHILDFAM", "d2");

        // Tuple B: 완전히 다른, flat한 single-level tuple인데 그 discriminator가 우연히
        // (raw "#" concatenation 하에서) tuple A와 정확히 같은 문자열을 만들어내는 텍스트를 담고 있다.
        CompositionDecision parentB = sourceSemanticGrid("Form[0]/Grid[0]");
        String tupleBIdentity =
                TargetSyntheticIdentity.build(parentB, "paging", "PAGING", "d1#childslot#CHILDFAM#d2");

        assertTrue("delimiter-ambiguity: two logically distinct tuples (nested chain vs flat "
                + "discriminator-stuffed) no longer collide after collision-safe encoding",
                !tupleAIdentity.equals(tupleBIdentity));
    }

    /** TARGET_SYNTHETIC parent 아래 또 TARGET_SYNTHETIC child identity를 만들 때, parent
     * identity에 이미 encoding metadata가 있어도 서로 다른 level-1 discriminator는 항상 다른
     * level-2 identity를 낳아야 한다. */
    private static void testNestedParentIdentityStableUnderCollisionSafeEncoding() throws Exception {
        CompositionDecision grid = sourceSemanticGrid("Form[0]/Grid[0]");
        CompositionEvaluator evaluator = new CompositionEvaluator();

        String level1IdA = TargetSyntheticIdentity.build(grid, "paging", "PAGING", "d1");
        String level1IdB = TargetSyntheticIdentity.build(grid, "paging", "PAGING", "d1-other");
        assertTrue("nested-parent: precondition -- the two level-1 identities are themselves distinct",
                !level1IdA.equals(level1IdB));

        CompositionDecision level1DecisionA = evaluator.createTargetSyntheticDecision("PAGING", null, level1IdA);
        CompositionDecision level1DecisionB = evaluator.createTargetSyntheticDecision("PAGING", null, level1IdB);

        String level2IdA = TargetSyntheticIdentity.build(level1DecisionA, "childslot", "CHILDFAM", "d2");
        String level2IdB = TargetSyntheticIdentity.build(level1DecisionB, "childslot", "CHILDFAM", "d2");
        assertTrue("nested-parent: level-2 identities built on top of distinct level-1 parents remain "
                + "distinct even though both parent identities already contain the helper's own "
                + "encoding metadata", !level2IdA.equals(level2IdB));

        // 재계산해도 안정적이어야 한다.
        String level2IdARecomputed = TargetSyntheticIdentity.build(
                evaluator.createTargetSyntheticDecision("PAGING", null, level1IdA), "childslot", "CHILDFAM", "d2");
        assertEquals("nested-parent: recomputing the same 2-level chain yields the same identity",
                level2IdA, level2IdARecomputed);
    }

    /** separator-like 문자({@code "#"}, {@code ":"} 등)를 포함한 discriminator도 값이 다르면
     * 다른 identity, 같으면 같은 identity를 낸다(collision 없음). */
    private static void testDiscriminatorArbitrarySeparatorContentNoCollision() throws Exception {
        CompositionDecision parent = sourceSemanticGrid("Form[0]/Grid[0]");
        String[] separatorLikeDiscriminators = {"#", "a#b", ":", "/", "12:embedded", "TSI|fake"};

        java.util.Set<String> producedIdentities = new java.util.HashSet<String>();
        for (String discriminator : separatorLikeDiscriminators) {
            String id = TargetSyntheticIdentity.build(parent, "paging", "PAGING", discriminator);
            assertTrue("discriminator-arbitrary-content: '" + discriminator + "' produces a NEW distinct "
                    + "identity not already produced by a different discriminator in this set",
                    producedIdentities.add(id));

            String idRecomputed = TargetSyntheticIdentity.build(parent, "paging", "PAGING", discriminator);
            assertEquals("discriminator-arbitrary-content: recomputing with the same separator-like "
                    + "discriminator '" + discriminator + "' yields the same identity", id, idRecomputed);
        }
        assertEquals("discriminator-arbitrary-content: all " + separatorLikeDiscriminators.length
                + " separator-like discriminators produced distinct identities (0 collisions)",
                String.valueOf(separatorLikeDiscriminators.length), String.valueOf(producedIdentities.size()));
    }

    private static String childNodeIdForParent(TargetCompositionPlan plan, String parentStructuralId) {
        for (TargetCompositionEdge edge : plan.getEdges()) {
            if (parentStructuralId.equals(edge.getParent().getSourceStructuralId())) {
                return edge.getChild().getNodeId();
            }
        }
        return null;
    }

    private static CompositionDecision sourceSemanticGrid(String structuralId) {
        CompositionDecision decision = new CompositionDecision();
        decision.setFamily("GRID");
        decision.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        decision.setSourceStructuralId(structuralId);
        decision.setEligible(true);
        return decision;
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
