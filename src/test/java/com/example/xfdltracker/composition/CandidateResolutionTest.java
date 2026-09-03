package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * candidate가 caller의 명시적 승인 없이는 절대 실제 assignment로 이어지지 않는지(자동 승인 0),
 * 승인된 candidate만 {@link CompositionEvaluator#assignSlot}을 통해 적용되는지(우회 없음),
 * fabricated/stale approval이 전부 거부되고 중복 적용이 없는지를 검증하는 오프라인 unit test.
 */
public class CandidateResolutionTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCandidateGenerationAloneProducesZeroAssignments();
        testSingleCandidateUnapprovedProducesZeroAssignments();
        testExactApprovalAppliesViaAssignSlot();
        testRejectedCandidateProducesZeroAssignment();
        testAmbiguousTwoCandidatesNoAutoSelection();
        testAmbiguousExplicitApprovalAppliesOnlyChosen();
        testFabricatedApprovalRejected();
        testStaleTamperedApprovalRejectedAtApply();
        testRepeatedApprovalNoDuplicateAssignment();
        testTabpageStructuralCorrelationNoCrossWiring();
        testGroupBoxStructuralCorrelationNoCrossWiring();
        testTargetSyntheticNeverApprovable();
        testPagingSourceEmissionStillZero();

        // ---- Approval Integrity + Idempotency 최종 강화 ----
        testStandaloneResolutionBypassingRegistryNeverReachesApplier();
        testCrossInstanceApplierIdempotency();
        testCrossRegistryIdempotency();
        testCrossRuleSameAssignmentConvergenceNotApplicable();
        testFailedApplyThenCorrectedRetrySucceeds();

        // ---- Generated-Candidate Provenance 최종 강화 ----
        testFabricatedCandidateWithFreshRegistryRejectedAtApply();
        testValidGeneratedCandidateStillAppliesOnce();
        testRuleIdTamperedResolutionRejectedAtApply();
        testStaleGraphWithoutRelationshipRejectsApproval();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** candidate 생성만으로는 어떤 decision의 slotAssignments도 바뀌지 않는다. */
    private static void testCandidateGenerationAloneProducesZeroAssignments() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        for (CompositionDecision decision : fx.decisions) {
            assertEquals("generation-alone: slotAssignments 0 (" + decision.getFamily() + ")", "0",
                    String.valueOf(decision.getSlotAssignments().size()));
        }
        assertTrue("generation-alone: at least 1 candidate exists", !fx.candidates.isEmpty());
    }

    /** candidate가 정확히 1개뿐이어도, 명시적으로 승인하지 않으면 assignment는 0이다. */
    private static void testSingleCandidateUnapprovedProducesZeroAssignments() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        assertEquals("single-unapproved: exactly 1 candidate (precondition)", "1", String.valueOf(fx.candidates.size()));

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        assertTrue("single-unapproved: resolution is UNRESOLVED by default",
                registry.getResolutions().get(0).getStatus() == CandidateResolution.Status.UNRESOLVED);

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("single-unapproved: applied count 0", "0", String.valueOf(applied));
        for (CompositionDecision decision : fx.decisions) {
            assertEquals("single-unapproved: slotAssignments still 0 (" + decision.getFamily() + ")", "0",
                    String.valueOf(decision.getSlotAssignments().size()));
        }
    }

    /** 정확한 candidate를 명시 승인하면 assignSlot을 통해 실제 assignment가 1개 생긴다. */
    private static void testExactApprovalAppliesViaAssignSlot() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        boolean approved = registry.approve(candidate, "test-approved");
        assertTrue("exact-approval: approve() returns true for a real candidate", approved);
        assertTrue("exact-approval: resolution status is APPROVED",
                registry.getResolutions().get(0).getStatus() == CandidateResolution.Status.APPROVED);

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("exact-approval: applied count 1", "1", String.valueOf(applied));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        CompositionDecision child = fx.decisionByStructuralId.get(candidate.getChildStructuralId());
        assertEquals("exact-approval: parent has exactly 1 slot assignment", "1",
                String.valueOf(parent.getSlotAssignments().size()));
        assertTrue("exact-approval: the assignment's child is the approved candidate's child decision",
                parent.getSlotAssignments().get(0).getChild() == child);
        assertEquals("exact-approval: assignment slot matches candidate slot", candidate.getSlot(),
                parent.getSlotAssignments().get(0).getSlot());
    }

    /** REJECTED candidate는 apply해도 assignment를 만들지 않는다. */
    private static void testRejectedCandidateProducesZeroAssignment() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        boolean rejected = registry.reject(candidate, "test-rejected");
        assertTrue("rejected-candidate: reject() returns true for a real candidate", rejected);

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("rejected-candidate: applied count 0", "0", String.valueOf(applied));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("rejected-candidate: parent slotAssignments still 0", "0",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** ambiguous 2 candidates(SPLIT_LAYOUT.columns / BUSINESS_TABLE.td_content) -- 둘 다 UNRESOLVED, apply해도 0. */
    private static void testAmbiguousTwoCandidatesNoAutoSelection() throws Exception {
        AmbiguityFixture fx = buildAmbiguityFixture();
        assertTrue("ambiguity-no-selection: at least the 2 documented candidates for the same "
                + "BUTTON_GROUP child exist (SPLIT_LAYOUT.columns and BUSINESS_TABLE.td_content -- "
                + "a 3rd legitimate candidate, SPLIT_LAYOUT.columns<-BUSINESS_TABLE itself, also "
                + "coexists since BUSINESS_TABLE is itself descended from SPLIT_LAYOUT and is in "
                + "SLOT_FILL_4's allow-list; that is a separate real ambiguity, not a bug)",
                findCandidate(fx.candidates, "SPLIT_LAYOUT", "BUTTON_GROUP", "columns") != null
                        && findCandidate(fx.candidates, "BUSINESS_TABLE", "BUTTON_GROUP", "td_content") != null);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        for (CandidateResolution resolution : registry.getResolutions()) {
            assertTrue("ambiguity-no-selection: every resolution starts UNRESOLVED",
                    resolution.getStatus() == CandidateResolution.Status.UNRESOLVED);
        }

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("ambiguity-no-selection: applied count 0 (no automatic selection)", "0", String.valueOf(applied));
    }

    /** ambiguous 2 candidates 중 하나만 명시 승인 -- 그것만 적용되고 나머지는 UNRESOLVED로 남는다. */
    private static void testAmbiguousExplicitApprovalAppliesOnlyChosen() throws Exception {
        AmbiguityFixture fx = buildAmbiguityFixture();
        SlotAssignmentCandidate chosen = findCandidate(fx.candidates, "SPLIT_LAYOUT", "BUTTON_GROUP", "columns");
        SlotAssignmentCandidate other = findCandidate(fx.candidates, "BUSINESS_TABLE", "BUTTON_GROUP", "td_content");
        assertTrue("ambiguity-explicit-approval: precondition -- both candidates found", chosen != null && other != null);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        assertTrue("ambiguity-explicit-approval: approve chosen", registry.approve(chosen, "picked-split-layout"));

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("ambiguity-explicit-approval: applied count 1", "1", String.valueOf(applied));

        CompositionDecision splitParent = fx.decisionByStructuralId.get(chosen.getParentStructuralId());
        CompositionDecision tableParent = fx.decisionByStructuralId.get(other.getParentStructuralId());
        assertEquals("ambiguity-explicit-approval: SPLIT_LAYOUT gained the assignment", "1",
                String.valueOf(splitParent.getSlotAssignments().size()));
        assertEquals("ambiguity-explicit-approval: BUSINESS_TABLE did NOT gain an assignment "
                + "(its candidate was never approved)", "0", String.valueOf(tableParent.getSlotAssignments().size()));

        boolean otherStillUnresolved = false;
        for (CandidateResolution resolution : registry.getResolutions()) {
            if (resolution.matchesIdentity(other) && resolution.getStatus() == CandidateResolution.Status.UNRESOLVED) {
                otherStillUnresolved = true;
            }
        }
        assertTrue("ambiguity-explicit-approval: the other candidate remains UNRESOLVED "
                + "(not auto-rejected -- no documented basis to do so)", otherStillUnresolved);
    }

    /** 원본 candidate 목록에 없는 fabricated candidate로 approve 시도 -- 거부(false), 상태 변화 없음. */
    private static void testFabricatedApprovalRejected() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate fabricated = new SlotAssignmentCandidate(
                "Form[0]/Div[99]", "Form[0]/Div[99]/Grid[0]", "SPLIT_LAYOUT", "GRID", "columns",
                "SLOT_FILL_4", SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, "fabricated");

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        boolean approved = registry.approve(fabricated, "should-not-work");
        assertTrue("fabricated-approval: approve() returns false for a nonexistent candidate", !approved);

        for (CandidateResolution resolution : registry.getResolutions()) {
            assertTrue("fabricated-approval: no real resolution was accidentally approved",
                    resolution.getStatus() == CandidateResolution.Status.UNRESOLVED);
        }
    }

    /**
     * 정상 승인 후, apply 시점에 child decision을 invalid variant로 조작된 것으로 바꿔치기하면
     * assignSlot 자체의 decision-integrity 재검증이 거부한다 -- approval이 그 검증을 우회하지
     * 않는다.
     */
    private static void testStaleTamperedApprovalRejectedAtApply() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        assertTrue("stale-tampered: approve succeeds", registry.approve(candidate, "approved-before-tamper"));

        CompositionDecision tamperedChild = new CompositionDecision();
        tamperedChild.setFamily("GRID");
        tamperedChild.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tamperedChild.setSourceStructuralId(candidate.getChildStructuralId());
        tamperedChild.setVariant("nonexistent_variant"); // 잘못된 값 -- decision-integrity가 이를 잡아내야 한다.
        tamperedChild.setEligible(true);

        Map<String, CompositionDecision> tamperedMap = new LinkedHashMap<String, CompositionDecision>(fx.decisionByStructuralId);
        tamperedMap.put(candidate.getChildStructuralId(), tamperedChild); // 승인 이후 stale 값으로 바꿔치기한다.

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, tamperedMap);
        assertEquals("stale-tampered: applied count 0 (assignSlot rejected the tampered decision)", "0",
                String.valueOf(applied));

        CompositionDecision parent = tamperedMap.get(candidate.getParentStructuralId());
        assertEquals("stale-tampered: parent slotAssignments still 0", "0",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** 동일 approval을 두 번 apply해도 duplicate SlotAssignment가 생기지 않는다. */
    private static void testRepeatedApprovalNoDuplicateAssignment() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        registry.approve(candidate, "approved-once");

        CandidateResolutionApplier applier = new CandidateResolutionApplier(new CompositionEvaluator());
        int firstApply = applier.applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        int secondApply = applier.applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);

        assertEquals("repeated-approval: first apply count 1", "1", String.valueOf(firstApply));
        assertEquals("repeated-approval: second apply count 0 (idempotent)", "0", String.valueOf(secondApply));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("repeated-approval: parent has exactly 1 slot assignment, not 2", "1",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** Tabpage 동일 sourceRegionId collision에서도 structuralId 기준으로 정확한 candidate만 승인/적용된다. */
    private static void testTabpageStructuralCorrelationNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element outerTab = newElement(doc, "Tab", "corrTab");
        form.appendChild(outerTab);

        Element pageA = newElement(doc, "Tabpage", "pageA");
        Element splitA = buildSplitLayoutWithNestedGridFixtureUnder(doc, pageA, "A");
        pageA.appendChild(splitA);
        outerTab.appendChild(pageA);

        Element pageB = newElement(doc, "Tabpage", "pageB");
        Element splitB = buildSplitLayoutWithNestedGridFixtureUnder(doc, pageB, "A"); // A와 동일한 bare id를 사용한다.
        pageB.appendChild(splitB);
        outerTab.appendChild(pageB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> splitResults = allByType(results, "SPLIT_LAYOUT");
        assertEquals("tabpage-correlation: sourceRegionId identical for both SPLIT_LAYOUT (real collision)",
                splitResults.get(0).getSourceRegionId(), splitResults.get(1).getSourceRegionId());
        assertTrue("tabpage-correlation: sourceStructuralId distinct",
                !splitResults.get(0).getSourceStructuralId().equals(splitResults.get(1).getSourceStructuralId()));

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        // SLOT_FILL_5(TAB_CONTROL.panes)는 7-family 전체를 허용하며, 이 generator는 page-scoping
        // 개념이 없어(그 구분은 PlanBuilder의 TabPageMembership 몫) outerTab.panes에 대해 4개의
        // ANCESTOR_CONTAINS candidate를 추가로 만든다. 기존 SPLIT_LAYOUT.columns 2개 + 4 = 6.
        assertEquals("tabpage-correlation: 6 candidates total (2 SPLIT_LAYOUT.columns "
                + "+ 4 TAB_CONTROL.panes now that SLOT_FILL_5 covers the 7-family set)", "6",
                String.valueOf(candidates.size()));

        SlotAssignmentCandidate candidateForA = null;
        for (SlotAssignmentCandidate c : candidates) {
            if (c.getParentStructuralId().equals(splitResults.get(0).getSourceStructuralId())) {
                candidateForA = c;
            }
        }
        assertTrue("tabpage-correlation: found the candidate belonging to pageA's SPLIT_LAYOUT", candidateForA != null);

        Map<String, CompositionDecision> decisionByStructuralId = indexByStructuralId(decisions);
        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(candidates);
        registry.approve(candidateForA, "approve-only-pageA-instance");
        new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, graph, decisions, decisionByStructuralId);

        CompositionDecision splitADecision = decisionByStructuralId.get(splitResults.get(0).getSourceStructuralId());
        CompositionDecision splitBDecision = decisionByStructuralId.get(splitResults.get(1).getSourceStructuralId());
        assertEquals("tabpage-correlation: pageA's SPLIT_LAYOUT got the assignment", "1",
                String.valueOf(splitADecision.getSlotAssignments().size()));
        assertEquals("tabpage-correlation: pageB's SPLIT_LAYOUT (cross-wiring check) got NOTHING", "0",
                String.valueOf(splitBDecision.getSlotAssignments().size()));
    }

    /** GroupBox 동일 sourceRegionId collision에서도 동일하게 structuralId 기준 정확한 승인/적용, cross-wiring 0. */
    private static void testGroupBoxStructuralCorrelationNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element groupA = newElement(doc, "GroupBox", "grpA");
        Element splitA = buildSplitLayoutWithNestedGridFixtureUnder(doc, groupA, "GA");
        groupA.appendChild(splitA);
        form.appendChild(groupA);

        Element groupB = newElement(doc, "GroupBox", "grpB");
        Element splitB = buildSplitLayoutWithNestedGridFixtureUnder(doc, groupB, "GA"); // 동일한 bare id를 사용한다.
        groupB.appendChild(splitB);
        form.appendChild(groupB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> splitResults = allByType(results, "SPLIT_LAYOUT");
        assertEquals("groupbox-correlation: sourceRegionId identical (real collision)",
                splitResults.get(0).getSourceRegionId(), splitResults.get(1).getSourceRegionId());
        assertTrue("groupbox-correlation: sourceStructuralId distinct",
                !splitResults.get(0).getSourceStructuralId().equals(splitResults.get(1).getSourceStructuralId()));

        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);

        SlotAssignmentCandidate candidateForA = null;
        for (SlotAssignmentCandidate c : candidates) {
            if (c.getParentStructuralId().equals(splitResults.get(0).getSourceStructuralId())) {
                candidateForA = c;
            }
        }
        assertTrue("groupbox-correlation: found the candidate belonging to groupA's SPLIT_LAYOUT", candidateForA != null);

        Map<String, CompositionDecision> decisionByStructuralId = indexByStructuralId(decisions);
        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(candidates);
        registry.approve(candidateForA, "approve-only-groupA-instance");
        new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, graph, decisions, decisionByStructuralId);

        CompositionDecision splitADecision = decisionByStructuralId.get(splitResults.get(0).getSourceStructuralId());
        CompositionDecision splitBDecision = decisionByStructuralId.get(splitResults.get(1).getSourceStructuralId());
        assertEquals("groupbox-correlation: groupA's SPLIT_LAYOUT got the assignment", "1",
                String.valueOf(splitADecision.getSlotAssignments().size()));
        assertEquals("groupbox-correlation: groupB's SPLIT_LAYOUT got NOTHING (cross-wiring check)", "0",
                String.valueOf(splitBDecision.getSlotAssignments().size()));
    }

    /** TARGET_SYNTHETIC decision은 candidate 생성 단계에서 배제되므로 resolution 계층에 들어오지
     * 않는다 -- fabricated candidate를 만들어 승인 시도해도 원본 목록에 없어 거부된다. */
    private static void testTargetSyntheticNeverApprovable() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        CompositionDecision targetSyntheticPaging = new CompositionEvaluator()
                .createTargetSyntheticDecision("PAGING", null);
        targetSyntheticPaging.setSourceStructuralId("Form[0]/Div[0]/Tab[0]"); // 실제 GRID 노드가 아닌 임의 값이다.

        SlotAssignmentCandidate fabricated = new SlotAssignmentCandidate(
                fx.candidates.get(0).getParentStructuralId(), targetSyntheticPaging.getSourceStructuralId(),
                fx.candidates.get(0).getParentFamily(), "PAGING", fx.candidates.get(0).getSlot(),
                fx.candidates.get(0).getCompositionRuleId(), fx.candidates.get(0).getSourceRelationshipType(),
                "fabricated-target-synthetic-attempt");

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        boolean approved = registry.approve(fabricated, "should-not-work");
        assertTrue("target-synthetic-never-approvable: fabricated TARGET_SYNTHETIC-referencing "
                + "candidate rejected (not in original candidate set)", !approved);
    }

    /** PAGING source emission = 0 재확인(regression). */
    private static void testPagingSourceEmissionStillZero() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "pagingCheckGrid14");
        form.appendChild(grid);
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        for (SemanticRegionResult r : results) {
            assertTrue("paging-source-emission: never PAGING", !"PAGING".equals(r.getSemanticType()));
        }
    }

    /**
     * {@code CandidateResolution}은 package-private이라 registry 없이도 직접 APPROVED를 만들
     * 수 있지만, {@link CandidateResolutionApplier#applyApproved}는 {@link
     * CandidateResolutionRegistry}만 받으므로 그런 standalone resolution은 apply 경로가 없다.
     */
    private static void testStandaloneResolutionBypassingRegistryNeverReachesApplier() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate realCandidate = fx.candidates.get(0);

        // Registry를 거치지 않고 직접 만든 standalone resolution -- package-private 접근으로
        // 실제 가능함(감사 결과 확인).
        CandidateResolution standalone = new CandidateResolution(
                "Form[0]/Div[123]", "Form[0]/Div[123]/Grid[0]", "columns", "SLOT_FILL_4");
        standalone.approve("bypassing-registry-directly");
        assertTrue("standalone-bypass: the standalone resolution IS approved (package-private access works)",
                standalone.getStatus() == CandidateResolution.Status.APPROVED);

        CandidateResolutionRegistry realRegistry = new CandidateResolutionRegistry(fx.candidates);
        realRegistry.approve(realCandidate, "legitimately-approved");
        boolean standaloneAppearsInRealRegistry = false;
        for (CandidateResolution resolution : realRegistry.getResolutions()) {
            if (resolution == standalone) {
                standaloneAppearsInRealRegistry = true;
            }
        }
        assertTrue("standalone-bypass: the standalone resolution never appears in a real registry's "
                + "getResolutions() (no mutator exists to inject it)", !standaloneAppearsInRealRegistry);

        // applyApproved의 시그니처는 더 이상 단순 Iterable<CandidateResolution>을 받지 않는다 --
        // 오직 real registry만 전달할 수 있으며, standalone 객체는 apply()에 도달할 경로가 아예 없다.
        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(realRegistry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("standalone-bypass: only the legitimately-approved real candidate was applied", "1",
                String.valueOf(applied));
    }

    /** cross-instance idempotency: 새 Applier 인스턴스로 같은 approval을 다시 apply해도 assignment는 여전히 1개. */
    private static void testCrossInstanceApplierIdempotency() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);
        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        registry.approve(candidate, "approved-once");

        int firstApply = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("cross-instance-idempotency: first apply count 1", "1", String.valueOf(firstApply));

        // 완전히 새로운 Applier 인스턴스 -- 첫 번째 인스턴스와 공유하는 인메모리 상태가 없다.
        int secondApply = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("cross-instance-idempotency: second apply (new Applier instance) count 0", "0",
                String.valueOf(secondApply));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("cross-instance-idempotency: parent still has exactly 1 slot assignment", "1",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** cross-registry idempotency: 새 Registry(같은 candidate로 재구성) + 새 Applier로 다시 apply해도 assignment는 여전히 1개. */
    private static void testCrossRegistryIdempotency() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry firstRegistry = new CandidateResolutionRegistry(fx.candidates);
        firstRegistry.approve(candidate, "approved-first-registry");
        int firstApply = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(firstRegistry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("cross-registry-idempotency: first apply count 1", "1", String.valueOf(firstApply));

        // 같은 candidate 목록으로 새로 구성한 Registry -- 같은 candidate를 다시 approve한다.
        CandidateResolutionRegistry secondRegistry = new CandidateResolutionRegistry(fx.candidates);
        secondRegistry.approve(candidate, "approved-second-registry");
        int secondApply = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(secondRegistry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("cross-registry-idempotency: second apply (new Registry, new Applier) count 0", "0",
                String.valueOf(secondApply));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("cross-registry-idempotency: parent still has exactly 1 slot assignment, not 2", "1",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /**
     * {@code slotFillRule(family, slot)}은 항상 정확히 하나의 rule만 반환하는 함수형 조회라,
     * 서로 다른 두 compositionRuleId가 같은 (parent, slot, child)로 수렴하는 시나리오는 현재
     * catalog 설계상 구조적으로 발생할 수 없다 -- 가짜 규칙 없이 그 구조적 이유를 검증한다.
     */
    private static void testCrossRuleSameAssignmentConvergenceNotApplicable() throws Exception {
        String[] families = {
                "SEARCH_AREA", "TITLE_BAR", "BUSINESS_TABLE", "GRID", "TAB_CONTROL", "BUTTON_GROUP", "SPLIT_LAYOUT"
        };
        for (String family : families) {
            TemplateFamilyCatalog.FamilyDefinition def = TemplateFamilyCatalog.get(family);
            for (String slot : def.getSlots()) {
                CompositionRule rule = CompositionRuleCatalog.slotFillRule(family, slot);
                if (rule == null) {
                    continue;
                }
                // slotFillRule은 같은 (family, slot)에 대해 항상 동일한 rule 객체를 반환한다 --
                // 다시 호출해도 결과가 같아야 한다(변할 수 있는 탐색이 아니라 함수형 조회이므로).
                CompositionRule ruleAgain = CompositionRuleCatalog.slotFillRule(family, slot);
                assertTrue("cross-rule-convergence-na: slotFillRule(" + family + "," + slot + ") is "
                        + "deterministic (same rule id every time)", rule.getId().equals(ruleAgain.getId()));
            }
        }
    }

    /**
     * retry semantics: 변조된 decision 때문에 apply가 실패해도(assignment 0) lock되지 않으며,
     * decision을 정상 값으로 교체한 뒤 다시 apply하면 성공한다(assignment 1).
     */
    private static void testFailedApplyThenCorrectedRetrySucceeds() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);
        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        registry.approve(candidate, "approved-before-tamper");

        CompositionDecision tamperedChild = new CompositionDecision();
        tamperedChild.setFamily("GRID");
        tamperedChild.setOrigin(CompositionDecision.Origin.SOURCE_SEMANTIC);
        tamperedChild.setSourceStructuralId(candidate.getChildStructuralId());
        tamperedChild.setVariant("nonexistent_variant");
        tamperedChild.setEligible(true);

        Map<String, CompositionDecision> tamperedMap = new LinkedHashMap<String, CompositionDecision>(fx.decisionByStructuralId);
        tamperedMap.put(candidate.getChildStructuralId(), tamperedChild);

        CandidateResolutionApplier applier = new CandidateResolutionApplier(new CompositionEvaluator());
        int failedApply = applier.applyApproved(registry, fx.graph, fx.decisions, tamperedMap);
        assertEquals("failed-then-retry: first attempt (tampered) applied count 0", "0", String.valueOf(failedApply));

        // map을 실제(변조되지 않은) decisions로 복원한 뒤 재시도 -- 같은 Applier 인스턴스, 같은
        // approved resolution으로 이번에는 성공해야 한다(실패했다고 영구히 lock되면 안 된다).
        int retryApply = applier.applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("failed-then-retry: retry with corrected decision applied count 1", "1",
                String.valueOf(retryApply));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("failed-then-retry: parent has exactly 1 slot assignment after successful retry", "1",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /**
     * {@link SlotAssignmentCandidateGenerator}가 실제로는 만들지 않을 cross-wired candidate를
     * 직접 construct해 fresh registry로 approve+apply한다. provenance 재검증(Generator를 다시
     * 호출해 재생성 가능한 identity와 대조)이 실제 Graph에 없는 관계를 잡아내 거부해야 한다.
     */
    private static void testFabricatedCandidateWithFreshRegistryRejectedAtApply() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRootA = newDiv(doc, "provSplitRootA");
        Element colA1 = newDivWithGeometry(doc, "provColA1", 0, 0, 500, 200);
        Element wrapperA = newDiv(doc, "provWrapperA");
        Element gridA = newElement(doc, "Grid", "provGridA");
        wrapperA.appendChild(gridA);
        colA1.appendChild(wrapperA);
        Element colA2 = newDivWithGeometry(doc, "provColA2", 500, 0, 500, 200);
        splitRootA.appendChild(colA1);
        splitRootA.appendChild(colA2);
        form.appendChild(splitRootA);

        Element standaloneWrapperB = newDiv(doc, "provWrapperB");
        Element gridB = newElement(doc, "Grid", "provGridB");
        standaloneWrapperB.appendChild(gridB);
        form.appendChild(standaloneWrapperB); // splitRootA와 DOM 관계가 없는 무관한 sibling이다.

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        Map<String, CompositionDecision> decisionByStructuralId = indexByStructuralId(decisions);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        CompositionDecision splitDecisionA = null;
        List<CompositionDecision> gridDecisions = new ArrayList<CompositionDecision>();
        for (CompositionDecision d : decisions) {
            if ("SPLIT_LAYOUT".equals(d.getFamily())) {
                splitDecisionA = d;
            }
            if ("GRID".equals(d.getFamily())) {
                gridDecisions.add(d);
            }
        }
        assertEquals("fabricated-fresh-registry: precondition -- 2 GRID decisions (A's real child, B standalone)",
                "2", String.valueOf(gridDecisions.size()));
        CompositionDecision gridDecisionB = gridDecisions.get(1); // splitRootA의 real child가 아닌 standalone 노드다.

        boolean realRelationshipExists = false;
        for (SemanticRegionRelationship r : graph.getRelationships()) {
            if (splitDecisionA.getSourceStructuralId().equals(r.getFromStructuralId())
                    && gridDecisionB.getSourceStructuralId().equals(r.getToStructuralId())) {
                realRelationshipExists = true;
            }
        }
        assertTrue("fabricated-fresh-registry: precondition -- no real graph relationship links "
                + "splitA to gridB", !realRelationshipExists);

        SlotAssignmentCandidate fabricatedCrossWired = new SlotAssignmentCandidate(
                splitDecisionA.getSourceStructuralId(), gridDecisionB.getSourceStructuralId(),
                "SPLIT_LAYOUT", "GRID", "columns", "SLOT_FILL_4",
                SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS,
                "fabricated_cross_wired_not_producible_by_generator");
        CandidateResolutionRegistry freshRegistry =
                new CandidateResolutionRegistry(java.util.Collections.singletonList(fabricatedCrossWired));
        assertTrue("fabricated-fresh-registry: approve succeeds (registry itself has no provenance "
                + "check of its own -- it only checks membership in its own 1-candidate list)",
                freshRegistry.approve(fabricatedCrossWired, "fabricated-cross-wiring-attempt"));

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(freshRegistry, graph, decisions, decisionByStructuralId);
        assertEquals("fabricated-fresh-registry: applied count 0 (not re-derivable by the real generator)",
                "0", String.valueOf(applied));
        assertEquals("fabricated-fresh-registry: splitA slotAssignments still 0", "0",
                String.valueOf(splitDecisionA.getSlotAssignments().size()));
    }

    /** 정상 흐름 회귀: 실제 generator output candidate는 provenance 재검증을 통과해 여전히 1건 적용된다. */
    private static void testValidGeneratedCandidateStillAppliesOnce() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        assertTrue("valid-generated-candidate: approve succeeds", registry.approve(candidate, "legit-approval"));

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("valid-generated-candidate: applied count 1 (real generator output, provenance-valid)",
                "1", String.valueOf(applied));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("valid-generated-candidate: parent has exactly 1 slot assignment", "1",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** parent/child/slot은 실제 candidate와 같지만 compositionRuleId만 이 family+slot에
     * 맞지 않는 다른 rule id로 바꿔치기 -- exact-identity 재검증이 이를 잡아내 거부해야 한다. */
    private static void testRuleIdTamperedResolutionRejectedAtApply() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate real = fx.candidates.get(0);
        assertEquals("rule-id-tamper: precondition -- real candidate's rule is SLOT_FILL_4",
                "SLOT_FILL_4", real.getCompositionRuleId());

        SlotAssignmentCandidate ruleTampered = new SlotAssignmentCandidate(
                real.getParentStructuralId(), real.getChildStructuralId(), real.getParentFamily(),
                real.getChildFamily(), real.getSlot(), "SLOT_FILL_1", // 실재하는 rule id지만 이 family+slot의 rule은 아니다.
                real.getSourceRelationshipType(), "rule_id_tampered");

        CandidateResolutionRegistry registry =
                new CandidateResolutionRegistry(java.util.Collections.singletonList(ruleTampered));
        assertTrue("rule-id-tamper: approve succeeds (registry only checks membership in its own list)",
                registry.approve(ruleTampered, "rule-id-tamper-attempt"));

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("rule-id-tamper: applied count 0 (rule id mismatch -- exact identity fails)",
                "0", String.valueOf(applied));

        CompositionDecision parent = fx.decisionByStructuralId.get(real.getParentStructuralId());
        assertEquals("rule-id-tamper: parent slotAssignments still 0", "0",
                String.valueOf(parent.getSlotAssignments().size()));
    }

    /** candidate 승인은 실제 관계가 있는 graph로 이뤄졌지만, apply 시점에 관계를 전혀 담지 않은
     * 별개의 graph를 넘기면 generator가 원래 candidate를 재생성할 수 없어 거부되어야 한다. */
    private static void testStaleGraphWithoutRelationshipRejectsApproval() throws Exception {
        Fixture fx = buildSplitLayoutWithNestedGridFixture();
        SlotAssignmentCandidate candidate = fx.candidates.get(0);

        CandidateResolutionRegistry registry = new CandidateResolutionRegistry(fx.candidates);
        assertTrue("stale-graph: approve succeeds", registry.approve(candidate, "approved-before-graph-swap"));

        // 완전히 비어 있는 document -> node/relationship이 전혀 없는 빈 graph. 실제(변조되지
        // 않은) decisions/decisionByStructuralId는 그대로 전달하며, provenance 재도출에 쓰이는
        // graph만 stale/무관한 값으로 바꾼다.
        Document emptyDoc = newDocument();
        Element emptyForm = emptyDoc.createElement("Form");
        List<SemanticRegionResult> emptyResults = new SemanticRegionSegmenter().segment(emptyForm);
        SemanticRegionGraph unrelatedGraph =
                new SemanticRegionRelationshipExtractor().buildGraph(emptyForm, emptyResults);
        assertTrue("stale-graph: precondition -- the substituted graph has no relationships",
                unrelatedGraph.getRelationships().isEmpty());

        int applied = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, unrelatedGraph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("stale-graph: applied count 0 (generator cannot re-derive the candidate from "
                + "a graph that never contained the relationship)", "0", String.valueOf(applied));

        CompositionDecision parent = fx.decisionByStructuralId.get(candidate.getParentStructuralId());
        assertEquals("stale-graph: parent slotAssignments still 0", "0",
                String.valueOf(parent.getSlotAssignments().size()));

        // REAL graph로 복원한 뒤 재시도 -- 같은 approval이 이번에는 성공해야 한다(stale-graph로
        // 인한 거부가 resolution을 영구히 lock하면 안 되며, 이는 stale-decision 재시도 의미론과 동일하다).
        int retried = new CandidateResolutionApplier(new CompositionEvaluator())
                .applyApproved(registry, fx.graph, fx.decisions, fx.decisionByStructuralId);
        assertEquals("stale-graph: retry with the real graph applies successfully (count 1)", "1",
                String.valueOf(retried));
    }

    // ---- fixture 생성 도우미 ----

    private static final class Fixture {
        List<SemanticRegionResult> results;
        List<CompositionDecision> decisions;
        Map<String, CompositionDecision> decisionByStructuralId;
        SemanticRegionGraph graph;
        List<SlotAssignmentCandidate> candidates;
    }

    private static final class AmbiguityFixture {
        Map<String, CompositionDecision> decisionByStructuralId;
        List<CompositionDecision> decisions;
        SemanticRegionGraph graph;
        List<SlotAssignmentCandidate> candidates;
    }

    private static Fixture buildSplitLayoutWithNestedGridFixture() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        buildSplitLayoutWithNestedGridFixtureUnder(doc, form, "fx");

        Fixture fx = new Fixture();
        fx.results = new SemanticRegionSegmenter().segment(form);
        fx.decisions = evaluateAll(fx.results);
        fx.decisionByStructuralId = indexByStructuralId(fx.decisions);
        fx.graph = new SemanticRegionRelationshipExtractor().buildGraph(form, fx.results);
        fx.candidates = new SlotAssignmentCandidateGenerator().generateCandidates(fx.graph, fx.decisions);
        return fx;
    }

    /** SPLIT_LAYOUT(col_5/col_5) + col1 안쪽 wrapper를 거친 nested GRID를 {@code parent} 아래 만든다. */
    private static Element buildSplitLayoutWithNestedGridFixtureUnder(Document doc, Element parent, String idPrefix) {
        Element splitRoot = newDiv(doc, idPrefix + "SplitRoot");
        Element col1 = newDivWithGeometry(doc, idPrefix + "Col1", 0, 0, 500, 200);
        Element wrapper = newDiv(doc, idPrefix + "Wrapper");
        Element grid = newElement(doc, "Grid", idPrefix + "Grid");
        wrapper.appendChild(grid);
        col1.appendChild(wrapper);
        Element col2 = newDivWithGeometry(doc, idPrefix + "Col2", 500, 0, 500, 200);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        parent.appendChild(splitRoot);
        return splitRoot;
    }

    /** BUTTON_GROUP이 SPLIT_LAYOUT/BUSINESS_TABLE 양쪽의 실제 조상 대상인 ambiguity fixture를 재구성한다. */
    private static AmbiguityFixture buildAmbiguityFixture() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRoot = newDiv(doc, "ambSplitRoot14");
        Element col1 = newDivWithGeometry(doc, "ambCol114", 0, 0, 500, 400);
        Element col2 = newDivWithGeometry(doc, "ambCol214", 500, 0, 500, 400);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        Element tableWrap = newDiv(doc, "ambTableWrap14");
        Element label1 = newElement(doc, "Static", "ambLabel114");
        setGeometry(label1, 0, 0, 80, 20);
        Element edit1 = newElement(doc, "Edit", "ambEdit114");
        setGeometry(edit1, 90, 0, 100, 20);
        Element label2 = newElement(doc, "Static", "ambLabel214");
        setGeometry(label2, 0, 30, 80, 20);
        Element edit2 = newElement(doc, "Edit", "ambEdit214");
        setGeometry(edit2, 90, 30, 100, 20);
        tableWrap.appendChild(label1);
        tableWrap.appendChild(edit1);
        tableWrap.appendChild(label2);
        tableWrap.appendChild(edit2);
        col1.appendChild(tableWrap);

        Element buttonGroupWrap = newDivWithGeometry(doc, "ambButtonWrap14", 0, 0, 100, 40);
        Element btn1 = newElement(doc, "Button", "ambBtn114");
        setGeometry(btn1, 10, 0, 60, 20);
        buttonGroupWrap.appendChild(btn1);
        edit2.appendChild(buttonGroupWrap);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<CompositionDecision> decisions = evaluateAll(results);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        AmbiguityFixture fx = new AmbiguityFixture();
        fx.decisionByStructuralId = indexByStructuralId(decisions);
        fx.decisions = decisions;
        fx.graph = graph;
        fx.candidates = new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        return fx;
    }

    // ---- 공용 도우미 ----

    private static List<CompositionDecision> evaluateAll(List<SemanticRegionResult> results) {
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        CompositionEvaluator evaluator = new CompositionEvaluator();
        for (SemanticRegionResult result : results) {
            decisions.add(evaluator.evaluate(result));
        }
        return decisions;
    }

    private static Map<String, CompositionDecision> indexByStructuralId(List<CompositionDecision> decisions) {
        Map<String, CompositionDecision> map = new LinkedHashMap<String, CompositionDecision>();
        for (CompositionDecision decision : decisions) {
            String id = decision.getSourceStructuralId();
            if (id != null && id.length() > 0) {
                map.put(id, decision);
            }
        }
        return map;
    }

    private static SlotAssignmentCandidate findCandidate(
            List<SlotAssignmentCandidate> candidates, String parentFamily, String childFamily, String slot) {
        for (SlotAssignmentCandidate candidate : candidates) {
            if (parentFamily.equals(candidate.getParentFamily()) && childFamily.equals(candidate.getChildFamily())
                    && slot.equals(candidate.getSlot())) {
                return candidate;
            }
        }
        return null;
    }

    private static List<SemanticRegionResult> allByType(List<SemanticRegionResult> results, String semanticType) {
        List<SemanticRegionResult> found = new ArrayList<SemanticRegionResult>();
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                found.add(r);
            }
        }
        return found;
    }

    // ---- fixture 빌더 도우미 ----

    private static Element newDiv(Document doc, String id) {
        return newElement(doc, "Div", id);
    }

    private static Element newDivWithGeometry(Document doc, String id, double left, double top, double width, double height) {
        Element div = newDiv(doc, id);
        setGeometry(div, left, top, width, height);
        return div;
    }

    private static Element newElement(Document doc, String tag, String id) {
        Element element = doc.createElement(tag);
        element.setAttribute("id", id);
        return element;
    }

    private static void setGeometry(Element el, double left, double top, double width, double height) {
        el.setAttribute("left", formatAttr(left));
        el.setAttribute("top", formatAttr(top));
        el.setAttribute("width", formatAttr(width));
        el.setAttribute("height", formatAttr(height));
    }

    private static String formatAttr(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean actual) {
        if (!actual) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=true actual=false");
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
