package com.example.xfdltracker.renderer;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.composition.CompositionDecision;
import com.example.xfdltracker.composition.CompositionEvaluator;
import com.example.xfdltracker.composition.SemanticRegionGraph;
import com.example.xfdltracker.composition.SemanticRegionRelationshipExtractor;
import com.example.xfdltracker.composition.SlotAssignmentCandidate;
import com.example.xfdltracker.composition.SlotAssignmentCandidateGenerator;
import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetCompositionPlanBuilder;
import com.example.xfdltracker.composition.TargetNodeIdentity;
import com.example.xfdltracker.composition.TargetNodeIdentityKind;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link TargetDocumentAssembler}에 대한 독립 실행형 단위 테스트(JUnit 미사용).
 * GRID 픽스처를 실제 파이프라인(Segmenter-&gt;Evaluator-&gt;PlanBuilder-&gt;Renderer)으로 처리해
 * 얻은 진짜 Plan/result로 assembler의 실패 경로를 직접 검증한다.
 */
public class TargetDocumentAssemblerTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testHtmlHeadBodySkeletonAndRootAttachment();
        testDuplicateIdentityFailsClosed();
        testOrphanIdentityFailsClosed();
        testMissingPlanResultFailsClosed();
        testNonRenderedRequiredResultFailsClosed();

        // buildGridFixture()는 단일 루트만 다루므로, 아래는 실제 2-노드 Plan(루트+비루트 자식)을
        // production 경로(CompositionEvaluator.assignSlot/PlanBuilder)로 생성해 비루트 실패
        // 케이스를 조작된 두 번째 루트가 아닌 진짜 비루트 노드로 검증한다.
        testMultiLevelAllNodesRenderedAssembles();
        testMultiLevelNonRootUnsupportedFamilyFailsClosed();
        testMultiLevelNonRootUnsupportedVariantFailsClosed();
        testMultiLevelNonRootIntegrityViolationFailsClosed();
        testMultiLevelNonRootResultMissingFailsClosed();
        testMultiLevelDuplicateNonRootResultFailsClosed();
        testMultiLevelOrphanNonRootResultFailsClosed();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static Fixture buildGridFixture() throws Exception {
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "assemblerGrid1");
        grid.setAttribute("left", "0");
        Element formats = doc.createElement("Formats");
        Element format = doc.createElement("Format");
        format.setAttribute("id", "fmt1");
        Element columns = doc.createElement("Columns");
        Element column = doc.createElement("Column");
        column.setAttribute("size", "100");
        columns.appendChild(column);
        format.appendChild(columns);
        Element band = doc.createElement("Band");
        band.setAttribute("id", "head");
        Element cell = doc.createElement("Cell");
        cell.setAttribute("col", "0");
        cell.setAttribute("row", "0");
        band.appendChild(cell);
        format.appendChild(band);
        Element bodyBand = doc.createElement("Band");
        bodyBand.setAttribute("id", "body");
        Element bodyCell = doc.createElement("Cell");
        bodyCell.setAttribute("col", "0");
        bodyCell.setAttribute("row", "0");
        bodyBand.appendChild(bodyCell);
        format.appendChild(bodyBand);
        formats.appendChild(format);
        grid.appendChild(formats);
        doc.appendChild(grid);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(grid);
        SemanticRegionResult gridRegion = null;
        for (SemanticRegionResult r : regions) {
            if ("GRID".equals(r.getSemanticType())) {
                gridRegion = r;
            }
        }
        assertTrue("assembler-fixture: GRID region found (precondition)", gridRegion != null);

        CompositionDecision decision = new CompositionEvaluator().evaluate(gridRegion);
        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(Arrays.asList(decision));
        List<com.example.xfdltracker.binding.SourceBindingReference> bindingReferences =
                new com.example.xfdltracker.binding.SourceBindingAnalyzer().analyze(grid);
        List<com.example.xfdltracker.payload.TargetNodePayload> payloads =
                new com.example.xfdltracker.payload.TargetPayloadExtractor()
                        .extract(grid, plan, regions, bindingReferences);
        List<AtomicRenderResult> atomic = new AtomicWebSquareRenderer().render(plan, payloads);
        List<CompositionRenderResult> composed = new CompositionRenderer().render(plan, atomic);

        Fixture fx = new Fixture();
        fx.plan = plan;
        fx.results = composed;
        return fx;
    }

    private static void testHtmlHeadBodySkeletonAndRootAttachment() throws Exception {
        Fixture fx = buildGridFixture();
        Document assembled = new TargetDocumentAssembler().assemble(fx.plan, fx.results);

        Element root = assembled.getDocumentElement();
        assertTrue("assembler: root element is html", "html".equals(root.getLocalName()));
        assertTrue("assembler: root element namespace is XHTML",
                "http://www.w3.org/1999/xhtml".equals(root.getNamespaceURI()));

        int headCount = 0, bodyCount = 0;
        Element body = null;
        for (org.w3c.dom.Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                String local = ((Element) n).getLocalName();
                if ("head".equals(local)) headCount++;
                if ("body".equals(local)) { bodyCount++; body = (Element) n; }
            }
        }
        assertTrue("assembler: exactly one head", headCount == 1);
        assertTrue("assembler: exactly one body", bodyCount == 1);
        assertTrue("assembler: body has exactly one appended root fragment (single GRID root)",
                body != null && body.getChildNodes().getLength() == 1);
    }

    private static void testDuplicateIdentityFailsClosed() throws Exception {
        Fixture fx = buildGridFixture();
        List<CompositionRenderResult> withDuplicate = new ArrayList<CompositionRenderResult>(fx.results);
        withDuplicate.add(fx.results.get(0));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, withDuplicate);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("assembler: duplicate identity in result collection fails closed", threw);
    }

    private static void testOrphanIdentityFailsClosed() throws Exception {
        Fixture fx = buildGridFixture();
        List<CompositionRenderResult> orphaned = new ArrayList<CompositionRenderResult>(fx.results);
        TargetNodeIdentity orphanIdentity = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "not_in_plan");
        Element doc = newDocument().createElement("div");
        orphaned.add(CompositionRenderResult.composed(orphanIdentity, doc));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, orphaned);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("assembler: orphan identity not present in Plan fails closed", threw);
    }

    private static void testMissingPlanResultFailsClosed() throws Exception {
        Fixture fx = buildGridFixture();
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, new ArrayList<CompositionRenderResult>());
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("assembler: missing result for a required Plan node fails closed", threw);
    }

    private static void testNonRenderedRequiredResultFailsClosed() throws Exception {
        Fixture fx = buildGridFixture();
        TargetNodeIdentity identity = fx.results.get(0).getIdentity();
        List<CompositionRenderResult> failed = new ArrayList<CompositionRenderResult>();
        failed.add(CompositionRenderResult.notComposed(identity, CompositionRenderStatus.ATOMIC_RENDER_UNAVAILABLE, "test"));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, failed);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("assembler: non-RENDERED required root result fails closed", threw);
    }

    // ==== 다중 레벨 Plan/result 상관관계 검증 ====

    /**
     * SPLIT_LAYOUT(루트) + 중첩 GRID(비루트) 픽스처를 공개 API(CompositionEvaluator)로만 구성한다.
     * TargetCompositionPlanBuilderTest에서 이미 승인된 것과 동일한 실제 부모/자식 엣지이며 조작된
     * 루트가 아니다. assembler는 identity/status/element로만 상관관계를 판단하므로 result는 직접 구성.
     */
    private static MultiLevelFixture buildMultiLevelTabControlFixture() throws Exception {
        Document srcDoc = newDocument();
        Element form = srcDoc.createElement("Form");
        Element splitRoot = srcDoc.createElement("Div");
        splitRoot.setAttribute("id", "asmSplitRoot");
        Element col1 = srcDoc.createElement("Div");
        col1.setAttribute("id", "asmCol1");
        col1.setAttribute("left", "0");
        col1.setAttribute("top", "0");
        col1.setAttribute("width", "500");
        col1.setAttribute("height", "200");
        Element grid = srcDoc.createElement("Grid");
        grid.setAttribute("id", "asmGrid1");
        col1.appendChild(grid);
        Element col2 = srcDoc.createElement("Div");
        col2.setAttribute("id", "asmCol2");
        col2.setAttribute("left", "500");
        col2.setAttribute("top", "0");
        col2.setAttribute("width", "500");
        col2.setAttribute("height", "200");
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(form);
        CompositionEvaluator evaluator = new CompositionEvaluator();
        List<CompositionDecision> decisions = new ArrayList<CompositionDecision>();
        for (SemanticRegionResult region : regions) {
            decisions.add(evaluator.evaluate(region));
        }
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, regions);
        List<SlotAssignmentCandidate> candidates =
                new SlotAssignmentCandidateGenerator().generateCandidates(graph, decisions);
        assertTrue("multi-level-fixture: precondition -- at least 1 SPLIT_LAYOUT.columns candidate found",
                !candidates.isEmpty());

        CompositionDecision splitDecision = null;
        CompositionDecision gridDecision = null;
        for (CompositionDecision d : decisions) {
            if ("SPLIT_LAYOUT".equals(d.getFamily())) {
                splitDecision = d;
            } else if ("GRID".equals(d.getFamily())) {
                gridDecision = d;
            }
        }
        assertTrue("multi-level-fixture: precondition -- SPLIT_LAYOUT decision found", splitDecision != null);
        assertTrue("multi-level-fixture: precondition -- GRID decision found", gridDecision != null);

        SlotAssignmentCandidate candidate = null;
        for (SlotAssignmentCandidate c : candidates) {
            if (splitDecision.getSourceStructuralId().equals(c.getParentStructuralId())
                    && gridDecision.getSourceStructuralId().equals(c.getChildStructuralId())) {
                candidate = c;
            }
        }
        assertTrue("multi-level-fixture: precondition -- SPLIT_LAYOUT.columns<-GRID candidate found",
                candidate != null);
        assertTrue("multi-level-fixture: assignSlot succeeds",
                evaluator.assignSlot(splitDecision, candidate.getSlot(), gridDecision));

        TargetCompositionPlan plan = new TargetCompositionPlanBuilder().build(decisions);
        assertEquals("multi-level-fixture: precondition -- 2 nodes", "2", String.valueOf(plan.getNodes().size()));
        assertEquals("multi-level-fixture: precondition -- 1 edge", "1", String.valueOf(plan.getEdges().size()));

        TargetCompositionNode rootNode = null;
        TargetCompositionNode childNode = null;
        for (TargetCompositionNode node : plan.getNodes()) {
            if (splitDecision.getSourceStructuralId().equals(node.getSourceStructuralId())) {
                rootNode = node;
            } else {
                childNode = node;
            }
        }
        assertTrue("multi-level-fixture: precondition -- root node found", rootNode != null);
        assertTrue("multi-level-fixture: precondition -- non-root child node found", childNode != null);

        MultiLevelFixture fx = new MultiLevelFixture();
        fx.plan = plan;
        fx.rootIdentity = rootNode.getIdentity();
        fx.childIdentity = childNode.getIdentity();
        fx.baseline = new ArrayList<CompositionRenderResult>();
        fx.baseline.add(CompositionRenderResult.composed(fx.rootIdentity, newDocument().createElement("div")));
        fx.baseline.add(CompositionRenderResult.composed(fx.childIdentity, newDocument().createElement("div")));
        return fx;
    }

    private static void testMultiLevelAllNodesRenderedAssembles() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        Document assembled = new TargetDocumentAssembler().assemble(fx.plan, fx.baseline);
        assertTrue("multi-level: all-RENDERED (root + non-root) assembles successfully",
                assembled.getDocumentElement() != null);
    }

    /**
     * assembler는 {@link CompositionRenderStatus}만 확인하므로, 원자 단계의 UNSUPPORTED_FAMILY
     * 판정도 ATOMIC_RENDER_UNAVAILABLE로 전달된다 -- 원래 판정은 reason 문자열에 보존된다.
     */
    private static void testMultiLevelNonRootUnsupportedFamilyFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>();
        results.add(fx.baseline.get(0));
        results.add(CompositionRenderResult.notComposed(
                fx.childIdentity, CompositionRenderStatus.ATOMIC_RENDER_UNAVAILABLE, "UNSUPPORTED_FAMILY"));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: non-root UNSUPPORTED_FAMILY (atomic) fails closed even though root is RENDERED",
                threw);
    }

    private static void testMultiLevelNonRootUnsupportedVariantFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>();
        results.add(fx.baseline.get(0));
        results.add(CompositionRenderResult.notComposed(
                fx.childIdentity, CompositionRenderStatus.ATOMIC_RENDER_UNAVAILABLE, "UNSUPPORTED_VARIANT"));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: non-root UNSUPPORTED_VARIANT (atomic) fails closed even though root is RENDERED",
                threw);
    }

    private static void testMultiLevelNonRootIntegrityViolationFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>();
        results.add(fx.baseline.get(0));
        results.add(CompositionRenderResult.notComposed(
                fx.childIdentity, CompositionRenderStatus.INTEGRITY_VIOLATION, "test"));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: non-root INTEGRITY_VIOLATION fails closed even though root is RENDERED", threw);
    }

    private static void testMultiLevelNonRootResultMissingFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>();
        results.add(fx.baseline.get(0));
        // 비루트 자식 result를 의도적으로 누락시킴.
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: missing non-root required Plan node result fails closed", threw);
    }

    private static void testMultiLevelDuplicateNonRootResultFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>(fx.baseline);
        results.add(fx.baseline.get(1));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: duplicate non-root render result correlation fails closed", threw);
    }

    private static void testMultiLevelOrphanNonRootResultFailsClosed() throws Exception {
        MultiLevelFixture fx = buildMultiLevelTabControlFixture();
        List<CompositionRenderResult> results = new ArrayList<CompositionRenderResult>(fx.baseline);
        TargetNodeIdentity orphan = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL,
                "Form[0]/Tab[assembler0]/Tabpage[0]/Tab[not_in_plan]");
        results.add(CompositionRenderResult.composed(orphan, newDocument().createElement("div")));
        boolean threw = false;
        try {
            new TargetDocumentAssembler().assemble(fx.plan, results);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("multi-level: orphan non-root-shaped result not represented in Plan fails closed", threw);
    }

    private static final class MultiLevelFixture {
        TargetCompositionPlan plan;
        TargetNodeIdentity rootIdentity;
        TargetNodeIdentity childIdentity;
        List<CompositionRenderResult> baseline;
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().newDocument();
    }

    private static final class Fixture {
        TargetCompositionPlan plan;
        List<CompositionRenderResult> results;
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }

    private static void assertEquals(String message, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("FAILED: " + message + " -- expected=<" + expected + "> actual=<" + actual + ">");
        }
    }
}
