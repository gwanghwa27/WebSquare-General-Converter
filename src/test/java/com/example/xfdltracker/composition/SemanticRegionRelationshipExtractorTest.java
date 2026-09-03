package com.example.xfdltracker.composition;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link SemanticRegionRelationshipExtractor}가 실제 source DOM에서만 관계를 계산하는지
 * (geometry/proximity 발명 없음), conflict-suppressed/no-emission region이 synthetic node가
 * 되지 않는지를 검증하는 오프라인 unit test.
 */
public class SemanticRegionRelationshipExtractorTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testIntegratedContainmentTree();
        testTitleBarButtonGroupSiblingOrder();
        testNestedSameBareIdScopeSafeRelationships();
        testConflictSuppressedRegionProducesNoGraphNode();
        testCategoryFilterLikeNoEmissionProducesNoGraphNode();
        testSharedDottedPrefixSiblingsNotMisreadAsContainment();
        testOverlappingGeometryAcrossUnrelatedSubtreesProducesNoRelationship();
        testEmptyInputsProduceEmptyGraph();

        // ---- Structural Identity + Relationship Endpoint 강화 ----
        testTabpageSameBareIdSourceRegionIdCollision();
        testTabpageSameBareIdStructuralIdsDistinctAndNoCrossWiring();
        testGroupBoxSameBareIdStructuralIdsDistinctAndNoCrossWiring();
        testRelationshipDirectionSemanticsForAllThreeTypes();
        testStructuralIdPrefixNotUsedForHierarchy();

        // ---- Direct Structural Anchor 최종 강화 ----
        testResultOrderIndependence();
        testDuplicateStructuralAnchorDefense();
        testSourceStructuralIdSetDirectlyBySegmenter();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /**
     * SPLIT_LAYOUT parent 아래 wrapper를 거친 nested GRID(ANCESTOR_CONTAINS), Tab(TAB_CONTROL,
     * ANCESTOR_CONTAINS), 그 Tab이 직접 담는 GRID(DIRECT_PARENT)까지 하나의 통합 트리에서
     * 실제 DOM containment 관계를 확인한다.
     */
    private static void testIntegratedContainmentTree() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRoot = newDiv(doc, "splitRoot");
        form.appendChild(splitRoot);

        Element col1 = newDivWithGeometry(doc, "col1", 0, 0, 500, 200);
        Element col1Wrapper = newDiv(doc, "col1Wrapper");
        Element grdNested = newElement(doc, "Grid", "grdNested");
        col1Wrapper.appendChild(grdNested);
        col1.appendChild(col1Wrapper);
        splitRoot.appendChild(col1);

        Element col2 = newDivWithGeometry(doc, "col2", 500, 0, 500, 200);
        Element tabNested = newElement(doc, "Tab", "tabNested");
        Element grdInTab = newElement(doc, "Grid", "grdInTab");
        tabNested.appendChild(grdInTab);
        col2.appendChild(tabNested);
        splitRoot.appendChild(col2);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult splitLayout = firstByType(results, "SPLIT_LAYOUT");
        assertEquals("integrated-containment: SPLIT_LAYOUT confidence", "HIGH", splitLayout.getConfidence());

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        String splitId = structuralIdOf(graph, splitLayout.getSourceRegionId(), "SPLIT_LAYOUT", 0);
        String gridNestedId = structuralIdOf(graph, firstByType(results, "GRID", "grdNested").getSourceRegionId(), "GRID", 0);
        String tabId = structuralIdOf(graph, firstByType(results, "TAB_CONTROL").getSourceRegionId(), "TAB_CONTROL", 0);
        String gridInTabId = structuralIdOf(graph, firstByType(results, "GRID", "grdInTab").getSourceRegionId(), "GRID", 0);

        assertTrue("integrated-containment: SPLIT_LAYOUT is a graph node", graph.getNodeStructuralIds().contains(splitId));
        assertTrue("integrated-containment: nested GRID is a graph node", graph.getNodeStructuralIds().contains(gridNestedId));
        assertTrue("integrated-containment: TAB_CONTROL is a graph node", graph.getNodeStructuralIds().contains(tabId));
        assertTrue("integrated-containment: GRID inside Tab is a graph node", graph.getNodeStructuralIds().contains(gridInTabId));

        assertTrue("integrated-containment: SPLIT_LAYOUT ANCESTOR_CONTAINS nested GRID (2+ hops through col1Wrapper)",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, splitId, gridNestedId));
        assertTrue("integrated-containment: SPLIT_LAYOUT ANCESTOR_CONTAINS TAB_CONTROL (via col2)",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, splitId, tabId));
        assertTrue("integrated-containment: TAB_CONTROL DIRECT_PARENT the GRID it immediately contains",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, tabId, gridInTabId));
        assertTrue("integrated-containment: SPLIT_LAYOUT ANCESTOR_CONTAINS the deeper nested GRID too",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, splitId, gridInTabId));
        assertTrue("integrated-containment: SPLIT_LAYOUT is NOT DIRECT_PARENT of the deeper nested GRID (real hop count respected)",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, splitId, gridNestedId));
    }

    /** TITLE_BAR(titleWrap)와 BUTTON_GROUP(buttonWrap)이 Form의 실제 직계 형제일 때 source-order relationship만 기록. */
    private static void testTitleBarButtonGroupSiblingOrder() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element titleWrap = newDiv(doc, "titleWrap");
        Element lblTitle = newElement(doc, "Static", "lblTitle");
        setGeometry(lblTitle, 0, 0, 100, 20);
        Element btnHelp = newElement(doc, "Button", "btnHelp");
        setGeometry(btnHelp, 200, 0, 60, 20);
        titleWrap.appendChild(lblTitle);
        titleWrap.appendChild(btnHelp);
        form.appendChild(titleWrap);

        Element buttonWrap = newDivWithGeometry(doc, "buttonWrap", 0, 40, 500, 30);
        Element btnSave = newElement(doc, "Button", "btnSave");
        setGeometry(btnSave, 10, 0, 60, 20);
        Element btnCancel = newElement(doc, "Button", "btnCancel");
        setGeometry(btnCancel, 80, 0, 60, 20);
        buttonWrap.appendChild(btnSave);
        buttonWrap.appendChild(btnCancel);
        form.appendChild(buttonWrap);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult titleBar = firstByType(results, "TITLE_BAR");
        SemanticRegionResult buttonGroup = firstByType(results, "BUTTON_GROUP");

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String titleId = structuralIdOf(graph, titleBar.getSourceRegionId(), "TITLE_BAR", 0);
        String buttonId = structuralIdOf(graph, buttonGroup.getSourceRegionId(), "BUTTON_GROUP", 0);

        assertTrue("sibling-order: TITLE_BAR/BUTTON_GROUP source-order relationship recorded",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, titleId, buttonId));
        assertTrue("sibling-order: no containment relationship invented between actual siblings",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, titleId, buttonId)
                && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, titleId, buttonId));

        SemanticRegionRelationship sibling = findRelationship(
                graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, titleId, buttonId);
        assertEquals("sibling-order: earlier sibling index recorded as 0 (titleWrap comes first in source)",
                "0", String.valueOf(sibling.getSourceOrder()));
    }

    /**
     * 동일 bare id("item"/"gridChild")가 서로 다른 Div scope 아래 반복돼도, 실제 DOM 기반 관계
     * 계산은 각자의 진짜 Tab-Grid 쌍끼리만 DIRECT_PARENT로 연결하고 cross-wiring되지 않는다.
     */
    private static void testNestedSameBareIdScopeSafeRelationships() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element outer = newDiv(doc, "outer");
        Element itemOuter = newElement(doc, "Tab", "item");
        Element gridOuter = newElement(doc, "Grid", "gridChild");
        itemOuter.appendChild(gridOuter);
        outer.appendChild(itemOuter);
        form.appendChild(outer);

        Element innerWrap = newDiv(doc, "innerWrap");
        Element itemInner = newElement(doc, "Tab", "item");
        Element gridInner = newElement(doc, "Grid", "gridChild");
        itemInner.appendChild(gridInner);
        innerWrap.appendChild(itemInner);
        form.appendChild(innerWrap);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> tabs = allByType(results, "TAB_CONTROL");
        assertEquals("nested-same-bare-id(Div): 2 TAB_CONTROL regions (same bare id, different scope)", "2", String.valueOf(tabs.size()));
        assertTrue("nested-same-bare-id(Div): TAB_CONTROL sourceRegionIds are scope-qualified and distinct "
                        + "(Div ancestor prefixing already disambiguates this case)",
                !tabs.get(0).getSourceRegionId().equals(tabs.get(1).getSourceRegionId()));

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String outerTabId = structuralIdOf(graph, "outer.item", "TAB_CONTROL", 0);
        String outerGridId = structuralIdOf(graph, "outer.gridChild", "GRID", 0);
        String innerTabId = structuralIdOf(graph, "innerWrap.item", "TAB_CONTROL", 0);
        String innerGridId = structuralIdOf(graph, "innerWrap.gridChild", "GRID", 0);

        assertTrue("nested-same-bare-id(Div): outer Tab DIRECT_PARENT of its own grid",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, outerTabId, outerGridId));
        assertTrue("nested-same-bare-id(Div): inner Tab DIRECT_PARENT of its own grid",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, innerTabId, innerGridId));
        assertTrue("nested-same-bare-id(Div): no cross-scope relationship (outer -> inner)",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, outerTabId, innerGridId));
        assertTrue("nested-same-bare-id(Div): no cross-scope relationship (inner -> outer)",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, innerTabId, outerGridId));
    }

    /**
     * non-Div scope collision 재현: {@code Tabpage}는 {@code Div}-only prefix 규칙에서
     * 제외되므로, 서로 다른 두 Grid Element가 동일 {@code sourceRegionId} 문자열을 낳는다
     * (production correlation 경로는 건드리지 않고 관찰만 한다).
     */
    private static void testTabpageSameBareIdSourceRegionIdCollision() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element tab = newElement(doc, "Tab", "mainTab");
        form.appendChild(tab);

        Element pageA = newElement(doc, "Tabpage", "pageA");
        Element gridA = newElement(doc, "Grid", "item");
        pageA.appendChild(gridA);
        tab.appendChild(pageA);

        Element pageB = newElement(doc, "Tabpage", "pageB");
        Element gridB = newElement(doc, "Grid", "item");
        pageB.appendChild(gridB);
        tab.appendChild(pageB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> grids = allByType(results, "GRID");
        assertEquals("tabpage-collision: 2 real GRID results", "2", String.valueOf(grids.size()));
        assertEquals("tabpage-collision: sourceRegionId of grid #1 is bare 'item' (Tabpage not prefixed)",
                "item", grids.get(0).getSourceRegionId());
        assertEquals("tabpage-collision: sourceRegionId of grid #2 is ALSO bare 'item' -- real collision, "
                + "not a test artifact (this is exactly why structuralId exists)",
                "item", grids.get(1).getSourceRegionId());
    }

    /**
     * 위 Tabpage collision 상황에서도 {@code structuralId}는 두 Grid를 서로 다른 노드로
     * 구분하고, TAB_CONTROL과 각자의 진짜 Grid 사이에만 ANCESTOR_CONTAINS를 만들며
     * (Tabpage 한 홉이라 DIRECT_PARENT 아님) cross-wiring이 없다.
     */
    private static void testTabpageSameBareIdStructuralIdsDistinctAndNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element tab = newElement(doc, "Tab", "mainTab");
        form.appendChild(tab);

        Element pageA = newElement(doc, "Tabpage", "pageA");
        Element gridA = newElement(doc, "Grid", "item");
        pageA.appendChild(gridA);
        tab.appendChild(pageA);

        Element pageB = newElement(doc, "Tabpage", "pageB");
        Element gridB = newElement(doc, "Grid", "item");
        pageB.appendChild(gridB);
        tab.appendChild(pageB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        assertEquals("tabpage-structural-id: 3 graph nodes total (TAB_CONTROL + 2 GRID)",
                "3", String.valueOf(graph.getNodes().size()));

        String tabId = structuralIdOf(graph, "mainTab", "TAB_CONTROL", 0);
        String gridAId = structuralIdOf(graph, "item", "GRID", 0);
        String gridBId = structuralIdOf(graph, "item", "GRID", 1);

        assertTrue("tabpage-structural-id: the two GRID nodes get DIFFERENT structuralId despite "
                + "identical sourceRegionId", !gridAId.equals(gridBId));
        assertTrue("tabpage-structural-id: mainTab ANCESTOR_CONTAINS grid #1 (via Tabpage, 2 hops)",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, tabId, gridAId));
        assertTrue("tabpage-structural-id: mainTab ANCESTOR_CONTAINS grid #2 (via Tabpage, 2 hops)",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, tabId, gridBId));
        assertTrue("tabpage-structural-id: grid #1 and grid #2 are DOM siblings-of-cousins, not direct "
                + "siblings themselves (different Tabpage parents) -- no DIRECT_SIBLING_ORDER between them",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, gridAId, gridBId)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, gridBId, gridAId));
    }

    /**
     * Structural Identity Hardening -- {@code GroupBox}(Div가 아닌 또 다른 registered
     * container)에서도 동일 bare id collision이 재현되고, structuralId가 이를 올바르게
     * 분리함을 확인한다(요구사항: "Div가 아닌 다른 registered container도 1건 확인").
     */
    private static void testGroupBoxSameBareIdStructuralIdsDistinctAndNoCrossWiring() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element groupA = newElement(doc, "GroupBox", "grpA");
        Element gridA = newElement(doc, "Grid", "item");
        groupA.appendChild(gridA);
        form.appendChild(groupA);

        Element groupB = newElement(doc, "GroupBox", "grpB");
        Element gridB = newElement(doc, "Grid", "item");
        groupB.appendChild(gridB);
        form.appendChild(groupB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        List<SemanticRegionResult> grids = allByType(results, "GRID");
        assertEquals("groupbox-collision: 2 real GRID results", "2", String.valueOf(grids.size()));
        assertEquals("groupbox-collision: both sourceRegionId are the bare 'item' (GroupBox not prefixed, real collision)",
                "item", grids.get(0).getSourceRegionId());
        assertEquals("groupbox-collision: both sourceRegionId are the bare 'item' (GroupBox not prefixed, real collision)",
                "item", grids.get(1).getSourceRegionId());

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String gridAId = structuralIdOf(graph, "item", "GRID", 0);
        String gridBId = structuralIdOf(graph, "item", "GRID", 1);
        assertTrue("groupbox-structural-id: distinct structuralId despite identical sourceRegionId",
                !gridAId.equals(gridBId));

        // groupA/groupB는 child 1개뿐이라 graph node가 아니며, gridA/gridB는 서로 다른 GroupBox
        // 자식이라 DOM sibling도 아니다 -- 둘을 잇는 relationship이 없어야 한다.
        assertTrue("groupbox-structural-id: no relationship invented between the two unrelated (cousin) GRIDs",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, gridAId, gridBId)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, gridAId, gridBId)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, gridBId, gridAId));
    }

    /**
     * Relationship endpoint 의미(계약) 검증: DIRECT_PARENT는 from=parent/to=child,
     * ANCESTOR_CONTAINS는 from=ancestor/to=descendant, DIRECT_SIBLING_ORDER는
     * from=earlier/to=later -- 반대 방향으로는 존재하지 않아야 한다.
     */
    private static void testRelationshipDirectionSemanticsForAllThreeTypes() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element tab = newElement(doc, "Tab", "dirTab");
        Element grid = newElement(doc, "Grid", "dirGrid");
        tab.appendChild(grid);
        form.appendChild(tab);

        Element wrapper = newDiv(doc, "dirWrapper");
        Element grid2 = newElement(doc, "Grid", "dirGrid2");
        wrapper.appendChild(grid2);
        form.appendChild(wrapper);

        Element siblingA = newElement(doc, "Grid", "dirSibA");
        Element siblingB = newElement(doc, "Grid", "dirSibB");
        Element siblingParent = newDiv(doc, "dirSibParent");
        siblingParent.appendChild(siblingA);
        siblingParent.appendChild(siblingB);
        form.appendChild(siblingParent);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        String tabId = structuralIdOf(graph, "dirTab", "TAB_CONTROL", 0);
        String gridId = structuralIdOf(graph, "dirGrid", "GRID", 0);
        SemanticRegionRelationship directParent = findRelationship(
                graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, tabId, gridId);
        assertTrue("direction-semantics: DIRECT_PARENT exists with from=parent(tab)/to=child(grid)", directParent != null);
        assertTrue("direction-semantics: DIRECT_PARENT does NOT exist in the reverse direction",
                findRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, gridId, tabId) == null);

        // ANCESTOR_CONTAINS 방향을 명확히 검증하기 위해 3-hop ancestor chain을 구성한다.
        Element outerTab = newElement(doc, "Tab", "ancTab");
        Element midWrapper = newDiv(doc, "ancMid");
        Element deepGrid = newElement(doc, "Grid", "ancGrid");
        midWrapper.appendChild(deepGrid);
        outerTab.appendChild(midWrapper);
        Document doc2 = newDocument();
        Element form2 = doc2.createElement("Form");
        Element outerTab2 = importDeep(doc2, outerTab);
        form2.appendChild(outerTab2);
        List<SemanticRegionResult> results2 = new SemanticRegionSegmenter().segment(form2);
        SemanticRegionGraph graph2 = new SemanticRegionRelationshipExtractor().buildGraph(form2, results2);
        String outerTabId = structuralIdOf(graph2, "ancTab", "TAB_CONTROL", 0);
        String deepGridId = structuralIdOf(graph2, "ancMid.ancGrid", "GRID", 0);
        assertTrue("direction-semantics: ANCESTOR_CONTAINS exists with from=ancestor/to=descendant",
                hasRelationship(graph2, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, outerTabId, deepGridId));
        assertTrue("direction-semantics: ANCESTOR_CONTAINS does NOT exist in the reverse direction",
                !hasRelationship(graph2, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, deepGridId, outerTabId));

        String sibAId = structuralIdOf(graph, "dirSibParent.dirSibA", "GRID", 0);
        String sibBId = structuralIdOf(graph, "dirSibParent.dirSibB", "GRID", 0);
        assertTrue("direction-semantics: DIRECT_SIBLING_ORDER exists with from=earlier(A)/to=later(B)",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, sibAId, sibBId));
        assertTrue("direction-semantics: DIRECT_SIBLING_ORDER does NOT exist in the reverse direction",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, sibBId, sibAId));
    }

    /**
     * structuralId prefix-only inference = 0 증명: "outer" Div 아래 실제 형제인 두 GRID의
     * structuralId가 앞부분 segment를 공유해도, containment로 오인되지 않고 오직 실제 DOM
     * sibling 관계로만 기록된다.
     */
    private static void testStructuralIdPrefixNotUsedForHierarchy() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element outer = newDiv(doc, "outer");
        Element gridA = newElement(doc, "Grid", "gridA");
        Element gridB = newElement(doc, "Grid", "gridB");
        outer.appendChild(gridA);
        outer.appendChild(gridB);
        form.appendChild(outer);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String idA = structuralIdOf(graph, "outer.gridA", "GRID", 0);
        String idB = structuralIdOf(graph, "outer.gridB", "GRID", 0);

        assertTrue("structural-id-prefix: recorded only as DIRECT_SIBLING_ORDER",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, idA, idB));
        assertTrue("structural-id-prefix: never misread as ANCESTOR_CONTAINS/DIRECT_PARENT in either direction",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, idA, idB)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, idB, idA)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, idA, idB)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, idB, idA));
    }

    /**
     * SPLIT_LAYOUT vs BUTTON_GROUP conflict로 Segmenter가 둘 다 suppress한 container는
     * segment() 결과 목록에 애초에 없으므로 graph node로도 생성되지 않는다.
     */
    private static void testConflictSuppressedRegionProducesNoGraphNode() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element conflictParent = newDivWithGeometry(doc, "conflictParent", 0, 0, 1000, 100);
        form.appendChild(conflictParent);

        Element leftButtons = newDivWithGeometry(doc, "leftButtons", 0, 0, 500, 100);
        Element btnA = newElement(doc, "Button", "btnA");
        setGeometry(btnA, 10, 0, 60, 20);
        leftButtons.appendChild(btnA);
        Element rightButtons = newDivWithGeometry(doc, "rightButtons", 500, 0, 500, 100);
        Element btnB = newElement(doc, "Button", "btnB");
        setGeometry(btnB, 10, 0, 60, 20);
        rightButtons.appendChild(btnB);
        conflictParent.appendChild(leftButtons);
        conflictParent.appendChild(rightButtons);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        String suppressedId = "conflictParent";
        assertTrue("conflict-suppressed: no result at all (of any type) carries the conflictParent "
                + "sourceRegionId itself (precondition -- SPLIT_LAYOUT and BUTTON_GROUP both matched "
                + "conflictParent's own children and were mutually suppressed; leftButtons/rightButtons "
                + "may still separately produce their own nested BUTTON_GROUP, which is unrelated)",
                findBySourceRegionIdOrNull(results, suppressedId) == null);

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);

        for (SemanticRegionGraphNode node : graph.getNodes()) {
            assertTrue("conflict-suppressed: no graph node for the suppressed conflictParent region",
                    !suppressedId.equals(node.getSourceRegionId()));
        }
    }

    /** CATEGORY_FILTER-like(2개 이상 연속 leading Static) -- HOLD, 무발행 -> graph node 없음. */
    private static void testCategoryFilterLikeNoEmissionProducesNoGraphNode() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element holdParent = newDiv(doc, "holdParent");
        holdParent.appendChild(newElement(doc, "Static", "opt1"));
        holdParent.appendChild(newElement(doc, "Static", "opt2"));
        holdParent.appendChild(newElement(doc, "Static", "opt3"));
        form.appendChild(holdParent);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        assertTrue("category-filter-like-no-emission: no TITLE_BAR/BUTTON_GROUP emitted (precondition)",
                firstByTypeOrNull(results, "TITLE_BAR") == null && firstByTypeOrNull(results, "BUTTON_GROUP") == null);

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        for (SemanticRegionGraphNode node : graph.getNodes()) {
            assertTrue("category-filter-like-no-emission: no node for holdParent",
                    !"holdParent".equals(node.getSourceRegionId()));
        }
    }

    /**
     * sourceRegionId prefix-only inference = 0 증명(regression): "outer" Div 아래 실제 형제인
     * 두 GRID는 dotted prefix를 공유하지만 오직 실제 DOM sibling 관계로만 기록된다.
     */
    private static void testSharedDottedPrefixSiblingsNotMisreadAsContainment() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element outer = newDiv(doc, "outer");
        Element gridA = newElement(doc, "Grid", "gridA");
        Element gridB = newElement(doc, "Grid", "gridB");
        outer.appendChild(gridA);
        outer.appendChild(gridB);
        form.appendChild(outer);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult resultA = findBySourceRegionId(results, "outer.gridA");
        SemanticRegionResult resultB = findBySourceRegionId(results, "outer.gridB");
        assertTrue("shared-prefix-siblings: both sourceRegionIds share the 'outer.' dotted prefix (precondition)",
                resultA.getSourceRegionId().startsWith("outer.") && resultB.getSourceRegionId().startsWith("outer."));

        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String idA = structuralIdOf(graph, "outer.gridA", "GRID", 0);
        String idB = structuralIdOf(graph, "outer.gridB", "GRID", 0);

        assertTrue("shared-prefix-siblings: recorded only as DIRECT_SIBLING_ORDER",
                hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_SIBLING_ORDER, idA, idB));
        assertTrue("shared-prefix-siblings: never misread as ANCESTOR_CONTAINS/DIRECT_PARENT in either direction",
                !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, idA, idB)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.ANCESTOR_CONTAINS, idB, idA)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, idA, idB)
                        && !hasRelationship(graph, SemanticRegionRelationship.RelationshipType.DIRECT_PARENT, idB, idA));
    }

    /**
     * geometry 기반 관계 추론 = 0 증명: 서로 무관한 부모 아래의 두 GRID가 완전히 동일한
     * geometry를 가져도, 실제 DOM 관계가 없으므로 relationship이 생성되지 않는다.
     */
    private static void testOverlappingGeometryAcrossUnrelatedSubtreesProducesNoRelationship() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element wrapperA = newDivWithGeometry(doc, "wrapperA", 0, 0, 300, 300);
        Element gridA = newElement(doc, "Grid", "grdOverlapA");
        setGeometry(gridA, 10, 10, 100, 100);
        wrapperA.appendChild(gridA);

        Element wrapperB = newDivWithGeometry(doc, "wrapperB", 900, 900, 300, 300);
        Element gridB = newElement(doc, "Grid", "grdOverlapB");
        setGeometry(gridB, 10, 10, 100, 100); // gridA와 geometry는 동일하지만 실제 DOM parent는 다르다.
        wrapperB.appendChild(gridB);

        form.appendChild(wrapperA);
        form.appendChild(wrapperB);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionGraph graph = new SemanticRegionRelationshipExtractor().buildGraph(form, results);
        String idA = structuralIdOf(graph, "wrapperA.grdOverlapA", "GRID", 0);
        String idB = structuralIdOf(graph, "wrapperB.grdOverlapB", "GRID", 0);

        for (SemanticRegionRelationship relationship : graph.getRelationships()) {
            boolean touchesBoth = (idA.equals(relationship.getFromStructuralId()) && idB.equals(relationship.getToStructuralId()))
                    || (idB.equals(relationship.getFromStructuralId()) && idA.equals(relationship.getToStructuralId()));
            assertTrue("overlapping-geometry-unrelated-subtrees: no relationship between geometry-identical "
                    + "but DOM-unrelated GRIDs", !touchesBoth);
        }
    }

    /**
     * result-order independence: 동일 DOM/결과 집합에 대해 결과 목록 순서를 뒤집어도
     * node structuralId 집합과 relationship 집합이 완전히 동일해야 한다.
     */
    private static void testResultOrderIndependence() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");

        Element splitRoot = newDiv(doc, "splitRoot");
        Element col1 = newDivWithGeometry(doc, "col1", 0, 0, 500, 200);
        Element grdNested = newElement(doc, "Grid", "grdNested");
        col1.appendChild(grdNested);
        Element col2 = newDivWithGeometry(doc, "col2", 500, 0, 500, 200);
        Element tabNested = newElement(doc, "Tab", "tabNested");
        col2.appendChild(tabNested);
        splitRoot.appendChild(col1);
        splitRoot.appendChild(col2);
        form.appendChild(splitRoot);

        List<SemanticRegionResult> originalOrder = new SemanticRegionSegmenter().segment(form);
        assertTrue("result-order-independence: at least 3 results to make order meaningful",
                originalOrder.size() >= 3);

        List<SemanticRegionResult> reversedOrder = new ArrayList<SemanticRegionResult>(originalOrder);
        java.util.Collections.reverse(reversedOrder);

        SemanticRegionRelationshipExtractor extractor = new SemanticRegionRelationshipExtractor();
        SemanticRegionGraph graphOriginal = extractor.buildGraph(form, originalOrder);
        SemanticRegionGraph graphReversed = extractor.buildGraph(form, reversedOrder);

        assertEquals("result-order-independence: same node count", String.valueOf(graphOriginal.getNodes().size()),
                String.valueOf(graphReversed.getNodes().size()));
        assertTrue("result-order-independence: identical node structuralId sets",
                graphOriginal.getNodeStructuralIds().equals(graphReversed.getNodeStructuralIds()));
        assertEquals("result-order-independence: same relationship count",
                String.valueOf(graphOriginal.getRelationships().size()),
                String.valueOf(graphReversed.getRelationships().size()));
        assertTrue("result-order-independence: every relationship in the original graph is also present "
                + "in the reversed-order graph", sameRelationshipSet(graphOriginal, graphReversed));
    }

    /**
     * duplicate anchor defense: hand-built synthetic 두 {@code SemanticRegionResult}가 같은
     * {@code sourceStructuralId}를 가지면, graph 생성은 조용히 merge하지 않고 명시적으로 실패한다.
     */
    private static void testDuplicateStructuralAnchorDefense() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "dupGrid");
        form.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult real = firstByType(results, "GRID");
        String sharedStructuralId = real.getSourceStructuralId();
        assertTrue("duplicate-anchor-defense: precondition -- real result has a non-empty sourceStructuralId",
                sharedStructuralId != null && sharedStructuralId.length() > 0);

        SemanticRegionResult impostor = new SemanticRegionResult();
        impostor.setSemanticType("TAB_CONTROL");
        impostor.setRecommendedTemplateFamily("TAB_CONTROL");
        impostor.setSourceRegionId("dupGrid");
        impostor.setSourceStructuralId(sharedStructuralId); // 의도적으로 `real`과 동일한 anchor를 사용한다.

        List<SemanticRegionResult> conflicting = new ArrayList<SemanticRegionResult>();
        conflicting.add(real);
        conflicting.add(impostor);

        boolean threw = false;
        try {
            new SemanticRegionRelationshipExtractor().buildGraph(form, conflicting);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue("duplicate-anchor-defense: buildGraph refuses (throws) instead of silently merging "
                + "or double-registering two results sharing one sourceStructuralId", threw);
    }

    /**
     * SemanticRegionResult가 sourceStructuralId를 Segmenter 자신으로부터 직접 받는지(후처리
     * 추측이 아니라) 확인 -- 실제 anchor Element로부터 계산한 값과 정확히 일치해야 한다.
     */
    private static void testSourceStructuralIdSetDirectlyBySegmenter() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element grid = newElement(doc, "Grid", "directGrid");
        form.appendChild(grid);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(form);
        SemanticRegionResult gridResult = firstByType(results, "GRID");
        String expected = com.example.xfdltracker.semantic.SourceStructuralIdentity.build(grid);

        assertTrue("segmenter-direct-anchor: sourceStructuralId is non-empty", gridResult.getSourceStructuralId() != null
                && gridResult.getSourceStructuralId().length() > 0);
        assertEquals("segmenter-direct-anchor: sourceStructuralId matches direct recomputation from the same anchor Element",
                expected, gridResult.getSourceStructuralId());
    }

    private static boolean sameRelationshipSet(SemanticRegionGraph a, SemanticRegionGraph b) {
        for (SemanticRegionRelationship r1 : a.getRelationships()) {
            if (findRelationship(b, r1.getRelationshipType(), r1.getFromStructuralId(), r1.getToStructuralId()) == null) {
                return false;
            }
        }
        for (SemanticRegionRelationship r2 : b.getRelationships()) {
            if (findRelationship(a, r2.getRelationshipType(), r2.getFromStructuralId(), r2.getToStructuralId()) == null) {
                return false;
            }
        }
        return true;
    }

    /** null/empty 입력 -- 빈 graph, 예외 없음. */
    private static void testEmptyInputsProduceEmptyGraph() throws Exception {
        SemanticRegionRelationshipExtractor extractor = new SemanticRegionRelationshipExtractor();
        SemanticRegionGraph g1 = extractor.buildGraph(null, null);
        assertTrue("empty-inputs: null root/results -> no nodes", g1.getNodes().isEmpty());
        assertTrue("empty-inputs: null root/results -> no relationships", g1.getRelationships().isEmpty());

        Document doc = newDocument();
        Element form = doc.createElement("Form");
        SemanticRegionGraph g2 = extractor.buildGraph(form, java.util.Collections.<SemanticRegionResult>emptyList());
        assertTrue("empty-inputs: empty results list -> no nodes", g2.getNodes().isEmpty());
    }

    // ---- graph 질의 도우미 ----

    /** {@code sourceRegionId}+{@code semanticType}가 일치하는 노드 중 {@code occurrenceIndex}번째
     * (등장 순서, 0-based)의 structuralId를 찾는다 -- collision fixture(같은 sourceRegionId를
     * 공유하는 2개 이상의 노드)를 다루기 위한 헬퍼. */
    private static String structuralIdOf(
            SemanticRegionGraph graph, String sourceRegionId, String semanticType, int occurrenceIndex) {
        int seen = 0;
        for (SemanticRegionGraphNode node : graph.getNodes()) {
            if (sourceRegionId.equals(node.getSourceRegionId()) && semanticType.equals(node.getSemanticType())) {
                if (seen == occurrenceIndex) {
                    return node.getStructuralId();
                }
                seen++;
            }
        }
        assertTrue("structuralIdOf: occurrence " + occurrenceIndex + " of " + semanticType + "@" + sourceRegionId
                + " present", false);
        return null;
    }

    private static boolean hasRelationship(
            SemanticRegionGraph graph, SemanticRegionRelationship.RelationshipType type,
            String fromId, String toId) {
        return findRelationship(graph, type, fromId, toId) != null;
    }

    private static SemanticRegionRelationship findRelationship(
            SemanticRegionGraph graph, SemanticRegionRelationship.RelationshipType type,
            String fromId, String toId) {
        for (SemanticRegionRelationship relationship : graph.getRelationshipsOfType(type)) {
            if (fromId.equals(relationship.getFromStructuralId())
                    && toId.equals(relationship.getToStructuralId())) {
                return relationship;
            }
        }
        return null;
    }

    // ---- 결과 목록 도우미 ----

    private static SemanticRegionResult firstByType(List<SemanticRegionResult> results, String semanticType) {
        SemanticRegionResult found = firstByTypeOrNull(results, semanticType);
        assertTrue("firstByType: " + semanticType + " present", found != null);
        return found;
    }

    private static SemanticRegionResult firstByType(
            List<SemanticRegionResult> results, String semanticType, String bareIdSuffix) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType()) && r.getSourceRegionId() != null
                    && r.getSourceRegionId().endsWith(bareIdSuffix)) {
                return r;
            }
        }
        assertTrue("firstByType: " + semanticType + " ending with " + bareIdSuffix + " present", false);
        return null;
    }

    private static SemanticRegionResult firstByTypeOrNull(List<SemanticRegionResult> results, String semanticType) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                return r;
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

    private static SemanticRegionResult findBySourceRegionIdOrNull(List<SemanticRegionResult> results, String sourceRegionId) {
        for (SemanticRegionResult r : results) {
            if (sourceRegionId.equals(r.getSourceRegionId())) {
                return r;
            }
        }
        return null;
    }

    private static SemanticRegionResult findBySourceRegionId(List<SemanticRegionResult> results, String sourceRegionId) {
        SemanticRegionResult found = findBySourceRegionIdOrNull(results, sourceRegionId);
        assertTrue("findBySourceRegionId: " + sourceRegionId + " present", found != null);
        return found;
    }

    // ---- fixture 생성 도우미 ----

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

    /** {@code source}(다른 Document 소속일 수 있음)를 {@code targetDoc} 소유로 깊은 복사한다. */
    private static Element importDeep(Document targetDoc, Element source) {
        return (Element) targetDoc.importNode(source, true);
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
