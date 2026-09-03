package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline, dependency-free(no JUnit) unit test. 7-Family Shadow Integration Gate -- 하나의
 * synthetic 트리에 7개 family를 동시 배치해 상호 정합성(중복 소유 없음, scope 소실 없음, 충돌
 * 없음, HOLD family 오발행 없음)을 검증한다. 새 predicate는 추가하지 않는다.
 */
public class SevenFamilyIntegrationTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSourceRegionIdIsScopeQualifiedNotBareId();
        testIntegrationOwnershipNoDuplicateOrLoss();
        testConflictRegressionInIntegratedTree();
        testHoldLikeFixturesProduceNoAcceptableFamilyEmission();
        testSplitLayoutFallbackIntegration();
        testBehavioralBoundaryIntegration();
        testButtonGroupMergeIntegration();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** {@code sourceRegionId}는 bare id가 아니라 scope-qualified path여야 한다. 서로 다른 조상
     *  scope에 우연히 같은 bare id("item")가 있어도 서로 다른 sourceRegionId를 가지며,
     *  duplicate ownership 오탐을 유발하지 않는다. */
    private static void testSourceRegionIdIsScopeQualifiedNotBareId() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "scopeSafetyRoot", null);

        Element outerScope = newDiv(doc, "outerScope", null);
        Element outerItem = newDiv(doc, "item", null);
        outerItem.appendChild(newElement(doc, "Static", "outerLabel", 0, 100, 20));
        outerScope.appendChild(outerItem);
        root.appendChild(outerScope);

        Element innerScope = newDiv(doc, "innerScope", null);
        Element innerItem = newDiv(doc, "item", 200);
        innerItem.appendChild(newElement(doc, "Button", "innerBtn1", 0, 80, 20));
        innerItem.appendChild(newElement(doc, "Button", "innerBtn2", 10, 60, 20));
        innerScope.appendChild(innerItem);
        root.appendChild(innerScope);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root);

        SemanticRegionResult outerResult = firstByTypeAndRegion(results, "TITLE_BAR", "outerScope.item");
        SemanticRegionResult innerResult = firstByTypeAndRegion(results, "BUTTON_GROUP", "innerScope.item");
        assertTrue("scope-safe-id: outer item (TITLE_BAR) found via scope-qualified suffix", outerResult != null);
        assertTrue("scope-safe-id: inner item (BUTTON_GROUP) found via scope-qualified suffix", innerResult != null);

        assertTrue("scope-safe-id: sourceRegionId is NOT the bare id alone",
                !"item".equals(outerResult.getSourceRegionId()) && !"item".equals(innerResult.getSourceRegionId()));
        assertTrue("scope-safe-id: outer/inner item resolve to DIFFERENT sourceRegionId despite same bare id",
                !outerResult.getSourceRegionId().equals(innerResult.getSourceRegionId()));
        assertTrue("scope-safe-id: outer sourceRegionId carries its own ancestor scope",
                outerResult.getSourceRegionId().endsWith("outerScope.item"));
        assertTrue("scope-safe-id: inner sourceRegionId carries its own ancestor scope",
                innerResult.getSourceRegionId().endsWith("innerScope.item"));

        // 서로 다른 실제 region이므로 family가 달라도 duplicate ownership 위반이 아니다.
        assertNoDuplicateRegionOwnership(results, "scope-safe-id");
    }

    /** 통합 ownership 검사: SPLIT_LAYOUT(자식에 GRID/TAB_CONTROL 중첩) + SEARCH_AREA +
     *  BUSINESS_TABLE + TITLE_BAR/BUTTON_GROUP을 함께 배치해 중복 소유/손실/동시 emit이
     *  없는지 확인한다. */
    private static void testIntegrationOwnershipNoDuplicateOrLoss() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "integrationRoot", null);

        // A. SPLIT_LAYOUT -- 왼쪽 컬럼에 GRID, 오른쪽 컬럼에 TAB_CONTROL 중첩.
        Element splitParent = newDiv(doc, "splitParent", null);
        Element colLeft = newDivWithGeometry(doc, "colLeft", 0, 0, 500, 300);
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "nestedGrid1");
        colLeft.appendChild(nestedGrid);
        Element colRight = newDivWithGeometry(doc, "colRight", 500, 0, 500, 300);
        Element nestedTab = doc.createElement("Tab");
        nestedTab.setAttribute("id", "nestedTab1");
        Element tabpages = doc.createElement("Tabpages");
        Element tabpage = doc.createElement("Tabpage");
        tabpage.setAttribute("id", "tp1");
        tabpages.appendChild(tabpage);
        nestedTab.appendChild(tabpages);
        colRight.appendChild(nestedTab);
        splitParent.appendChild(colLeft);
        splitParent.appendChild(colRight);
        root.appendChild(splitParent);

        // B. SEARCH_AREA(searchArea1) -- 뒤에 Grid(searchResultGrid) 형제.
        Element searchAreaScope = newDiv(doc, "searchAreaScope", null);
        Element searchArea1 = newLabelControlPairsRow(doc, "searchArea1", 0);
        Element searchResultGrid = doc.createElement("Grid");
        searchResultGrid.setAttribute("id", "searchResultGrid");
        searchAreaScope.appendChild(searchArea1);
        searchAreaScope.appendChild(searchResultGrid);
        root.appendChild(searchAreaScope);

        // C. BUSINESS_TABLE(businessTable1) -- Grid 형제 없음, 2행으로 HIGH confidence.
        Element businessScope = newDiv(doc, "businessScope", null);
        Element businessTable1 = newDiv(doc, "businessTable1", null);
        appendLabelControlPair(doc, businessTable1, "biz_r1", "Edit", 0, 0);
        appendLabelControlPair(doc, businessTable1, "biz_r2", "Edit", 0, 30);
        businessScope.appendChild(businessTable1);
        root.appendChild(businessScope);

        // D. TITLE_BAR(pageTitle) + BUTTON_GROUP(pageActions, title_bar_attached) 조합 구성.
        Element headerScope = newDiv(doc, "headerScope", null);
        Element pageTitle = newDiv(doc, "pageTitle", null);
        pageTitle.appendChild(newElement(doc, "Static", "pageLabel", 0, 100, 20));
        Element pageActions = newDiv(doc, "pageActions", 200);
        pageActions.appendChild(newElement(doc, "Button", "newBtn", 120, 60, 20));
        headerScope.appendChild(pageTitle);
        headerScope.appendChild(pageActions);
        root.appendChild(headerScope);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root);

        assertNoDuplicateRegionOwnership(results, "integration-ownership");

        assertEquals("integration: SPLIT_LAYOUT count", "1",
                String.valueOf(countByTypeAndRegion(results, "SPLIT_LAYOUT", "splitParent")));
        assertEquals("integration: nested GRID not swallowed by parent SPLIT_LAYOUT", "1",
                String.valueOf(countByTypeAndRegion(results, "GRID", "nestedGrid1")));
        assertEquals("integration: nested TAB_CONTROL not swallowed by parent SPLIT_LAYOUT", "1",
                String.valueOf(countByTypeAndRegion(results, "TAB_CONTROL", "nestedTab1")));

        assertEquals("integration: SEARCH_AREA only (not also BUSINESS_TABLE) for searchArea1", "1",
                String.valueOf(countByTypeAndRegion(results, "SEARCH_AREA", "searchArea1")));
        assertEquals("integration: no BUSINESS_TABLE for searchArea1", "0",
                String.valueOf(countByTypeAndRegion(results, "BUSINESS_TABLE", "searchArea1")));

        assertEquals("integration: BUSINESS_TABLE only (not also SEARCH_AREA) for businessTable1", "1",
                String.valueOf(countByTypeAndRegion(results, "BUSINESS_TABLE", "businessTable1")));
        assertEquals("integration: no SEARCH_AREA for businessTable1", "0",
                String.valueOf(countByTypeAndRegion(results, "SEARCH_AREA", "businessTable1")));

        assertEquals("integration: TITLE_BAR for pageTitle", "1",
                String.valueOf(countByTypeAndRegion(results, "TITLE_BAR", "pageTitle")));
        assertEquals("integration: BUTTON_GROUP for pageActions", "1",
                String.valueOf(countByTypeAndRegion(results, "BUTTON_GROUP", "pageActions")));
        SemanticRegionResult buttonGroup = firstByTypeAndRegion(results, "BUTTON_GROUP", "pageActions");
        assertEquals("integration: BUTTON_GROUP variant title_bar_attached (real TITLE_BAR sibling present)",
                "title_bar_attached", buttonGroup.getRecommendedVariant());
        assertEquals("integration: no TITLE_BAR for pageActions (TITLE_BAR/BUTTON_GROUP not double-owned)", "0",
                String.valueOf(countByTypeAndRegion(results, "TITLE_BAR", "pageActions")));
    }

    /** conflict regression -- 통합 트리 안에서도 TITLE_BAR/BUTTON_GROUP ambiguous, CATEGORY_FILTER-like HOLD 유지. */
    private static void testConflictRegressionInIntegratedTree() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "conflictRoot", null);

        Element ambiguousContainer = newDiv(doc, "ambiguousMixed", null);
        ambiguousContainer.appendChild(newElement(doc, "Static", "titleLabel", 0, 200, 20));
        ambiguousContainer.appendChild(newElement(doc, "Button", "actionBtn", 300, 60, 20));
        ambiguousContainer.appendChild(newElement(doc, "Static", "trailingNote", 210, 80, 20));
        root.appendChild(ambiguousContainer);

        Element filterLikeContainer = newDiv(doc, "filterLikeList", null);
        filterLikeContainer.appendChild(newElement(doc, "Static", "item1", 0, 100, 20));
        filterLikeContainer.appendChild(newElement(doc, "Static", "item2", 100, 100, 20));
        filterLikeContainer.appendChild(newElement(doc, "Static", "item3", 200, 100, 20));
        root.appendChild(filterLikeContainer);

        // 충돌 억제가 다른 정상 매치까지 막지 않는지 확인하기 위해 진짜 TITLE_BAR/BUTTON_GROUP도 함께 배치.
        Element canonicalTitle = newDiv(doc, "canonicalTitle", null);
        canonicalTitle.appendChild(newElement(doc, "Static", "canonicalLabel", 0, 100, 20));
        root.appendChild(canonicalTitle);

        Element canonicalButtons = newDiv(doc, "canonicalButtons", 200);
        canonicalButtons.appendChild(newElement(doc, "Button", "b1", 0, 80, 20));
        canonicalButtons.appendChild(newElement(doc, "Button", "b2", 10, 60, 20));
        root.appendChild(canonicalButtons);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root);

        assertEquals("conflict-regression: ambiguousMixed produces no forced family", "0",
                String.valueOf(countByRegion(results, "ambiguousMixed")));
        assertEquals("conflict-regression: filterLikeList produces no forced family", "0",
                String.valueOf(countByRegion(results, "filterLikeList")));
        assertEquals("conflict-regression: no CATEGORY_FILTER emitted anywhere", "0",
                String.valueOf(countByType(results, "CATEGORY_FILTER")));
        assertEquals("conflict-regression: canonical TITLE_BAR still detected alongside HOLD cases", "1",
                String.valueOf(countByTypeAndRegion(results, "TITLE_BAR", "canonicalTitle")));
        assertEquals("conflict-regression: canonical BUTTON_GROUP still detected alongside HOLD cases", "1",
                String.valueOf(countByTypeAndRegion(results, "BUTTON_GROUP", "canonicalButtons")));
    }

    /** HOLD no-emission integration: 5개 HOLD family-like 구조를 만들어 ACCEPTABLE family가
     *  emit되지 않음을 확인한다. fixture는 다른 7개 family의 실제 hard evidence와 겹치지
     *  않게 의도적으로 구성했다. */
    private static void testHoldLikeFixturesProduceNoAcceptableFamilyEmission() throws Exception {
        // TREEVIEW-like: 들여쓰기 계층, 각 레벨이 형제 Static 2개(리프 라벨)를 가짐 -- Grid/Tab
        // 없음, label/control 교대쌍 아님, all-Button 아님.
        Document doc1 = newDocument();
        Element treeRoot = newDiv(doc1, "treeViewLike", null);
        Element level1 = newDiv(doc1, "treeLevel1", null);
        level1.appendChild(newElement(doc1, "Static", "node1", 0, 100, 20));
        level1.appendChild(newElement(doc1, "Static", "node2", 100, 100, 20));
        treeRoot.appendChild(level1);
        assertEquals("hold-like: TREEVIEW-like produces 0 results", "0",
                String.valueOf(new SemanticRegionSegmenter().segment(treeRoot).size()));

        // AGREEMENT_LIST-like: 반복되는 CheckBox 3개(라벨 Static 선행 없음, Button도 아님).
        Document doc2 = newDocument();
        Element agreementList = newDiv(doc2, "agreementListLike", null);
        agreementList.appendChild(newElement(doc2, "CheckBox", "agree1", 0, 20, 20));
        agreementList.appendChild(newElement(doc2, "CheckBox", "agree2", 30, 20, 20));
        agreementList.appendChild(newElement(doc2, "CheckBox", "agree3", 60, 20, 20));
        assertEquals("hold-like: AGREEMENT_LIST-like produces 0 results", "0",
                String.valueOf(new SemanticRegionSegmenter().segment(agreementList).size()));

        // CATEGORY_FILTER-like: 반복되는 Static 리스트(활성 상태 신호는 이 모델로 확인 불가).
        Document doc3 = newDocument();
        Element categoryFilter = newDiv(doc3, "categoryFilterLike", null);
        categoryFilter.appendChild(newElement(doc3, "Static", "cat1", 0, 80, 20));
        categoryFilter.appendChild(newElement(doc3, "Static", "cat2", 80, 80, 20));
        assertEquals("hold-like: CATEGORY_FILTER-like produces 0 results", "0",
                String.valueOf(new SemanticRegionSegmenter().segment(categoryFilter).size()));

        // INFOBOX-like: 불릿 텍스트 목록(Static 3개 연속).
        Document doc4 = newDocument();
        Element infobox = newDiv(doc4, "infoboxLike", null);
        infobox.appendChild(newElement(doc4, "Static", "info1", 0, 300, 20));
        infobox.appendChild(newElement(doc4, "Static", "info2", 0, 300, 20));
        infobox.appendChild(newElement(doc4, "Static", "info3", 0, 300, 20));
        assertEquals("hold-like: INFOBOX-like produces 0 results", "0",
                String.valueOf(new SemanticRegionSegmenter().segment(infobox).size()));

        // PAGING-like: "N / total" 형태의 페이지 표시(Static + Edit + Static, button 없음) --
        // 선행 Static 1개 뒤에 non-Button 내용이 섞여 TITLE_BAR로도 강제되지 않는 케이스.
        Document doc5 = newDocument();
        Element paging = newDiv(doc5, "pagingLike", null);
        paging.appendChild(newElement(doc5, "Static", "pageInfo", 0, 60, 20));
        paging.appendChild(newElement(doc5, "Edit", "pageInput", 60, 40, 20));
        paging.appendChild(newElement(doc5, "Static", "ofTotal", 100, 40, 20));
        assertEquals("hold-like: PAGING-like produces 0 results", "0",
                String.valueOf(new SemanticRegionSegmenter().segment(paging).size()));
    }

    /** fallback integration -- SPLIT_LAYOUT의 exact/non-exact/invalid geometry 3갈래와 UNKNOWN
     *  region이 통합 상태에서도 억지로 canonical family를 만들지 않음을 확인. */
    private static void testSplitLayoutFallbackIntegration() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "fallbackRoot", null);

        Element exactParent = newDiv(doc, "fallbackExactParent", null);
        exactParent.appendChild(newDivWithGeometry(doc, "fbCol30", 0, 0, 300, 200));
        exactParent.appendChild(newDivWithGeometry(doc, "fbCol70", 300, 0, 700, 200));
        root.appendChild(exactParent);

        Element nonExactParent = newDiv(doc, "fallbackNonExactParent", null);
        nonExactParent.appendChild(newDivWithGeometry(doc, "fbColA", 0, 0, 296, 200));
        nonExactParent.appendChild(newDivWithGeometry(doc, "fbColB", 296, 0, 704, 200));
        root.appendChild(nonExactParent);

        Element invalidParent = newDiv(doc, "fallbackInvalidParent", null);
        invalidParent.appendChild(newDivWithGeometry(doc, "fbColX", 0, 0, 300, 200));
        invalidParent.appendChild(newDivWithGeometry(doc, "fbColY", 250, 0, 700, 200)); // 겹침(overlap) 발생
        root.appendChild(invalidParent);

        Element unknownRegion = newDiv(doc, "unknownRegion", null);
        unknownRegion.appendChild(newElement(doc, "Edit", "input1", 0, 100, 20));
        unknownRegion.appendChild(newElement(doc, "Combo", "combo1", 100, 100, 20));
        root.appendChild(unknownRegion);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root);

        SemanticRegionResult exact = firstByTypeAndRegion(results, "SPLIT_LAYOUT", "fallbackExactParent");
        assertTrue("split-fallback-integration: exact ratio -> SPLIT_LAYOUT present", exact != null);
        assertEquals("split-fallback-integration: exact ratio variant", "ratio_split", exact.getRecommendedVariant());
        assertEquals("split-fallback-integration: exact ratio confidence", "HIGH", exact.getConfidence());

        SemanticRegionResult nonExact = firstByTypeAndRegion(results, "SPLIT_LAYOUT", "fallbackNonExactParent");
        assertTrue("split-fallback-integration: non-exact -> SPLIT_LAYOUT present (LOW)", nonExact != null);
        assertEquals("split-fallback-integration: non-exact fallback", "FIXED_WIDTH_FALLBACK", nonExact.getFallback());
        assertTrue("split-fallback-integration: non-exact variant NOT promoted to ratio_split",
                nonExact.getRecommendedVariant() == null);

        assertEquals("split-fallback-integration: invalid geometry -> no SPLIT_LAYOUT emission", "0",
                String.valueOf(countByTypeAndRegion(results, "SPLIT_LAYOUT", "fallbackInvalidParent")));

        assertEquals("split-fallback-integration: UNKNOWN region -> no canonical family forced", "0",
                String.valueOf(countByRegion(results, "unknownRegion")));
    }

    /** behavioral boundary integration -- visible/enable/event boundary가 무관한 다른 family와
     *  같은 트리/같은 segment() 호출에서도 경계를 넘어 병합되지 않는지 확인한다. */
    private static void testBehavioralBoundaryIntegration() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "boundaryIntegrationRoot", null);

        // 노이즈: 무관한 진짜 GRID, 진짜 TITLE_BAR.
        Element noiseGrid = doc.createElement("Grid");
        noiseGrid.setAttribute("id", "noiseGrid");
        root.appendChild(noiseGrid);
        Element noiseTitle = newDiv(doc, "noiseTitle", null);
        noiseTitle.appendChild(newElement(doc, "Static", "noiseLabel", 0, 100, 20));
        root.appendChild(noiseTitle);

        // visible/enable boundary: label/control pair + visible wrapper(내부 Grid) -- merge 금지.
        Element visibleScope = newDiv(doc, "visibleBoundaryScope", null);
        Element conditionRow1 = newLabelControlPairsRow(doc, "conditionRow1", 0);
        Element visibleWrapper = newDiv(doc, "visibleWrapper", null);
        visibleWrapper.setAttribute("visible", "cond_visible");
        Element hiddenGrid1 = doc.createElement("Grid");
        hiddenGrid1.setAttribute("id", "hiddenGrid1");
        visibleWrapper.appendChild(hiddenGrid1);
        visibleScope.appendChild(conditionRow1);
        visibleScope.appendChild(visibleWrapper);
        root.appendChild(visibleScope);

        // event boundary: label/control pair + event-bound wrapper(내부 Grid) -- merge 금지.
        Element eventScope = newDiv(doc, "eventBoundaryScope", null);
        Element conditionRow2 = newLabelControlPairsRow(doc, "conditionRow2", 0);
        Element eventWrapper = newDiv(doc, "eventWrapper", null);
        Element hiddenGrid2 = doc.createElement("Grid");
        hiddenGrid2.setAttribute("id", "hiddenGrid2");
        eventWrapper.appendChild(hiddenGrid2);
        eventScope.appendChild(conditionRow2);
        eventScope.appendChild(eventWrapper);
        root.appendChild(eventScope);

        // buildEventComponentPath는 Form까지 모든 Div 조상을 prefix하므로 root의 id도 포함해야 한다.
        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding(
                "boundaryIntegrationRoot.eventBoundaryScope.eventWrapper", "onclick", "toggleX"));

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root, analysis);

        assertEquals("boundary-integration: noise GRID unaffected", "1",
                String.valueOf(countByTypeAndRegion(results, "GRID", "noiseGrid")));
        assertEquals("boundary-integration: noise TITLE_BAR unaffected", "1",
                String.valueOf(countByTypeAndRegion(results, "TITLE_BAR", "noiseTitle")));

        SemanticRegionResult visibleResult = firstByTypeAndRegion(results, "BUSINESS_TABLE", "conditionRow1");
        assertTrue("boundary-integration: visible boundary blocks merge -> BUSINESS_TABLE", visibleResult != null);
        assertTrue("boundary-integration: visible boundary evidence recorded",
                containsHierarchyEvidence(visibleResult, "wrapper_normalization_stopped_at_visible_or_enable_boundary"));

        SemanticRegionResult eventResult = firstByTypeAndRegion(results, "BUSINESS_TABLE", "conditionRow2");
        assertTrue("boundary-integration: event boundary blocks merge -> BUSINESS_TABLE", eventResult != null);
        assertTrue("boundary-integration: event boundary evidence recorded",
                containsHierarchyEvidence(eventResult, "wrapper_normalization_stopped_at_event_boundary"));

        assertNoDuplicateRegionOwnership(results, "boundary-integration");
    }

    /** BUTTON_GROUP merge/no-merge를 다른 family들과 같은 트리에서 재확인. */
    private static void testButtonGroupMergeIntegration() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "mergeIntegrationRoot", null);

        Element noiseSearchScope = newDiv(doc, "noiseSearchScope", null);
        Element noiseSearchArea = newLabelControlPairsRow(doc, "noiseSearchArea", 0);
        Element noiseGrid = doc.createElement("Grid");
        noiseGrid.setAttribute("id", "noiseSearchGrid");
        noiseSearchScope.appendChild(noiseSearchArea);
        noiseSearchScope.appendChild(noiseGrid);
        root.appendChild(noiseSearchScope);

        Element mergeScope = newDiv(doc, "mergeScope", 300);
        Element wrapMergeA = newDiv(doc, "wrapMergeA", null);
        wrapMergeA.appendChild(newElement(doc, "Button", "mergeBtnA", 0, 50, 20));
        Element wrapMergeB = newDiv(doc, "wrapMergeB", null);
        wrapMergeB.appendChild(newElement(doc, "Button", "mergeBtnB", 60, 50, 20));
        mergeScope.appendChild(wrapMergeA);
        mergeScope.appendChild(wrapMergeB);
        root.appendChild(mergeScope);

        Element mergeBoundaryScope = newDiv(doc, "mergeBoundaryScope", 300);
        Element wrapBoundaryA = newDiv(doc, "wrapBoundaryA", null);
        wrapBoundaryA.appendChild(newElement(doc, "Button", "boundaryBtnA", 0, 50, 20));
        Element wrapBoundaryB = newDiv(doc, "wrapBoundaryB", null);
        wrapBoundaryB.setAttribute("visible", "cond");
        wrapBoundaryB.appendChild(newElement(doc, "Button", "boundaryBtnB", 60, 50, 20));
        mergeBoundaryScope.appendChild(wrapBoundaryA);
        mergeBoundaryScope.appendChild(wrapBoundaryB);
        root.appendChild(mergeBoundaryScope);

        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(root);

        assertEquals("merge-integration: noise SEARCH_AREA unaffected", "1",
                String.valueOf(countByTypeAndRegion(results, "SEARCH_AREA", "noiseSearchArea")));
        assertEquals("merge-integration: unblocked merge -> BUTTON_GROUP", "1",
                String.valueOf(countByTypeAndRegion(results, "BUTTON_GROUP", "mergeScope")));
        assertEquals("merge-integration: blocked merge -> no BUTTON_GROUP for mergeBoundaryScope", "0",
                String.valueOf(countByRegion(results, "mergeBoundaryScope")));

        assertNoDuplicateRegionOwnership(results, "merge-integration");
    }

    // ---- fixture 생성 도우미 ----

    private static Element newLabelControlPairsRow(Document doc, String id, double topOffset) {
        Element container = newDiv(doc, id, null);
        appendLabelControlPair(doc, container, id + "_label1", "Edit", 0, topOffset);
        appendLabelControlPair(doc, container, id + "_label2", "Edit", 200, topOffset);
        return container;
    }

    private static void appendLabelControlPair(
            Document doc, Element container, String idPrefix, String controlTag, double left, double top) {
        Element label = doc.createElement("Static");
        label.setAttribute("id", idPrefix + "_static");
        setGeometry(label, left, top, 50, 20);
        Element control = doc.createElement(controlTag);
        control.setAttribute("id", idPrefix + "_" + controlTag.toLowerCase());
        setGeometry(control, left + 50, top, 150, 20);
        container.appendChild(label);
        container.appendChild(control);
    }

    private static Element newDiv(Document doc, String id, Integer width) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        if (width != null) {
            div.setAttribute("width", String.valueOf(width));
        }
        return div;
    }

    private static Element newDivWithGeometry(Document doc, String id, double left, double top, double width, double height) {
        Element div = newDiv(doc, id, null);
        setGeometry(div, left, top, width, height);
        return div;
    }

    private static Element newElement(Document doc, String tag, String id, double left, double width, double height) {
        Element el = doc.createElement(tag);
        el.setAttribute("id", id);
        setGeometry(el, left, 0, width, height);
        return el;
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

    // ---- 결과 목록 질의 도우미 ----

    /** {@code sourceRegionId}는 scope-qualified path다. fixture bare id는 트리 전체에서 유일하므로
     *  suffix 일치로 찾아도 모호하지 않다. */
    private static boolean regionMatches(String actualRegionId, String expectedBareId) {
        return actualRegionId != null
                && (actualRegionId.equals(expectedBareId) || actualRegionId.endsWith("." + expectedBareId));
    }

    private static int countByTypeAndRegion(List<SemanticRegionResult> results, String semanticType, String regionId) {
        int count = 0;
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType()) && regionMatches(r.getSourceRegionId(), regionId)) {
                count++;
            }
        }
        return count;
    }

    private static SemanticRegionResult firstByTypeAndRegion(List<SemanticRegionResult> results, String semanticType, String regionId) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType()) && regionMatches(r.getSourceRegionId(), regionId)) {
                return r;
            }
        }
        return null;
    }

    private static int countByRegion(List<SemanticRegionResult> results, String regionId) {
        int count = 0;
        for (SemanticRegionResult r : results) {
            if (regionMatches(r.getSourceRegionId(), regionId)) {
                count++;
            }
        }
        return count;
    }

    private static int countByType(List<SemanticRegionResult> results, String semanticType) {
        int count = 0;
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                count++;
            }
        }
        return count;
    }

    /** 같은 sourceRegionId를 가진 결과들이 전부 같은 semanticType인지(중복 소유 없음) 확인. */
    private static void assertNoDuplicateRegionOwnership(List<SemanticRegionResult> results, String label) {
        Map<String, String> typeByRegion = new HashMap<String, String>();
        for (SemanticRegionResult r : results) {
            String regionId = r.getSourceRegionId();
            if (regionId == null || regionId.length() == 0) {
                continue;
            }
            String existingType = typeByRegion.get(regionId);
            if (existingType == null) {
                typeByRegion.put(regionId, r.getSemanticType());
            } else if (!existingType.equals(r.getSemanticType())) {
                failures++;
                System.out.println("[FAIL] " + label + ": duplicate region ownership -- regionId=" + regionId
                        + " claimed by both " + existingType + " and " + r.getSemanticType());
                return;
            }
        }
        System.out.println("[PASS] " + label + ": no duplicate region ownership");
    }

    private static boolean containsHierarchyEvidence(SemanticRegionResult result, String needle) {
        for (String evidence : result.getHierarchyEvidence()) {
            if (needle.equals(evidence)) {
                return true;
            }
        }
        return false;
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
