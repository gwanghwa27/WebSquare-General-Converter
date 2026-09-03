package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.semantic.SemanticRegionResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline, dependency-free(no JUnit) unit test. TITLE_BAR/BUTTON_GROUP predicate와
 * conflict-handling(ambiguity 시 HOLD)을 검증한다.
 */
public class TitleBarButtonGroupSegmenterTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCanonicalTitleBarDetected();
        testCanonicalStandaloneButtonGroupDetected();
        testTitleBarAttachedButtonGroupDetected();
        testGridAdjacencyAloneDoesNotImplyTitleBarAttached();
        testAdjacentButtonWrappersMergeWithoutBoundary();
        testVisibleEnableBoundaryBlocksButtonWrapperMerge();
        testEventBoundaryBlocksButtonWrapperMerge();
        testVisibleBoundaryPreservesTwoSeparateButtonGroupInstances();
        testEventBoundaryPreservesTwoSeparateButtonGroupInstances();
        testAmbiguousTitleBarVsButtonGroupIsNotForced();
        testCategoryFilterLikeAmbiguityIsNotForced();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** 선행 단일 Static + 오른쪽 Button 없음(가장 단순한 canonical 형태) -> TITLE_BAR/title_only. */
    private static void testCanonicalTitleBarDetected() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "titleBar", 400);
        Element titleStatic = newElement(doc, "Static", "titleLabel", 0, 200, 20);
        container.appendChild(titleStatic);

        List<SemanticRegionResult> results = regionResultsOf(container, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("canonical-title-bar: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("canonical-title-bar: semanticType", "TITLE_BAR", r.getSemanticType());
        assertEquals("canonical-title-bar: variant", "title_only", r.getRecommendedVariant());
        assertEquals("canonical-title-bar: confidence", "HIGH", r.getConfidence());
    }

    /** 자식이 전부 Button, 구조적 후보 없음 -> BUTTON_GROUP/standalone, 둘 다 중점보다 왼쪽 ->
     *  결과 position=left_buttons가 되어야 한다. */
    private static void testCanonicalStandaloneButtonGroupDetected() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "actions", 200);
        container.appendChild(newElement(doc, "Button", "btn1", 0, 80, 20));
        container.appendChild(newElement(doc, "Button", "btn2", 10, 60, 20));

        List<SemanticRegionResult> results = regionResultsOf(container, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("canonical-standalone-button-group: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("canonical-standalone-button-group: semanticType", "BUTTON_GROUP", r.getSemanticType());
        assertEquals("canonical-standalone-button-group: variant", "standalone", r.getRecommendedVariant());
        assertEquals("canonical-standalone-button-group: position",
                "left_buttons", String.valueOf(r.getParameters().get("position")));
    }

    /** 자식이 전부 Button인 컨테이너의 바로 앞 형제가 실제 TITLE_BAR 구조로 확인되면
     *  BUTTON_GROUP/title_bar_attached(Grid 인접이 아니라 실제 sibling evidence로 판정). */
    private static void testTitleBarAttachedButtonGroupDetected() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "root", 0);
        Element titleContainer = newDiv(doc, "sectionTitle", 200);
        titleContainer.appendChild(newElement(doc, "Static", "sectionLabel", 0, 100, 20));
        Element buttonContainer = newDiv(doc, "headerActions", 200);
        buttonContainer.appendChild(newElement(doc, "Button", "refreshBtn", 120, 60, 20));
        root.appendChild(titleContainer);
        root.appendChild(buttonContainer);

        List<SemanticRegionResult> results = regionResultsOf(root, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("title-bar-attached-button-group: count", "2", String.valueOf(results.size()));

        SemanticRegionResult titleResult = firstOfType(results, "TITLE_BAR");
        SemanticRegionResult buttonGroupResult = firstOfType(results, "BUTTON_GROUP");
        assertTrue("title-bar-attached-button-group: sibling TITLE_BAR detected", titleResult != null);
        assertTrue("title-bar-attached-button-group: BUTTON_GROUP detected", buttonGroupResult != null);
        assertEquals("title-bar-attached-button-group: variant",
                "title_bar_attached", buttonGroupResult.getRecommendedVariant());
    }

    /** Grid 인접만으로는 title_bar_attached를 증명할 수 없다 -- 실제 TITLE_BAR sibling이
     *  없으면 standalone 유지. */
    private static void testGridAdjacencyAloneDoesNotImplyTitleBarAttached() throws Exception {
        Document doc = newDocument();
        Element root = newDiv(doc, "root", 0);
        Element buttonContainer = newDiv(doc, "gridActions", 200);
        buttonContainer.appendChild(newElement(doc, "Button", "refreshBtn", 120, 60, 20));
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "resultGrid");
        root.appendChild(buttonContainer);
        root.appendChild(grid);

        List<SemanticRegionResult> results = regionResultsOf(root, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("grid-adjacency-alone-not-sufficient: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("grid-adjacency-alone-not-sufficient: semanticType", "BUTTON_GROUP", r.getSemanticType());
        assertEquals("grid-adjacency-alone-not-sufficient: variant stays standalone (no forced attachment)",
                "standalone", r.getRecommendedVariant());
    }

    /** boundary 없는 인접 button wrapper 2개는 투명하게 병합되어 부모 레벨에서 하나의
     *  BUTTON_GROUP으로 인식된다(각 wrapper는 width 없어 독립 발행 불가). */
    private static void testAdjacentButtonWrappersMergeWithoutBoundary() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "footerActions", 300);
        Element wrapperA = newDiv(doc, "wrapA", null);
        wrapperA.appendChild(newElement(doc, "Button", "cancelBtn", 0, 50, 20));
        Element wrapperB = newDiv(doc, "wrapB", null);
        wrapperB.appendChild(newElement(doc, "Button", "saveBtn", 60, 50, 20));
        container.appendChild(wrapperA);
        container.appendChild(wrapperB);

        List<SemanticRegionResult> results = regionResultsOf(container, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("wrapper-merge-no-boundary: count", "1", String.valueOf(results.size()));
        SemanticRegionResult r = results.get(0);
        assertEquals("wrapper-merge-no-boundary: semanticType", "BUTTON_GROUP", r.getSemanticType());
        assertTrue("wrapper-merge-no-boundary: evidence records transparent wrapper merge",
                containsMatchedFeature(r, "all_children_button_via_transparent_wrapper_merge"));
        assertEquals("wrapper-merge-no-boundary: merged button_count via component_evidence",
                "true", String.valueOf(containsComponentEvidence(r, "button_count=2")));
    }

    /** 두 번째 wrapper가 독립적 visible 속성을 가지면 병합하지 않는다. 각 wrapper는 width가
     *  없어 독립 발행도 불가하므로 전체 결과는 0개(조용한 대체 없이 미발행). */
    private static void testVisibleEnableBoundaryBlocksButtonWrapperMerge() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "footerActions", 300);
        Element wrapperA = newDiv(doc, "wrapA", null);
        wrapperA.appendChild(newElement(doc, "Button", "cancelBtn", 0, 50, 20));
        Element wrapperB = newDiv(doc, "wrapB", null);
        wrapperB.setAttribute("visible", "cond_visible");
        wrapperB.appendChild(newElement(doc, "Button", "saveBtn", 60, 50, 20));
        container.appendChild(wrapperA);
        container.appendChild(wrapperB);

        List<SemanticRegionResult> results = regionResultsOf(container, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("wrapper-merge-visible-boundary: count", "0", String.valueOf(results.size()));
    }

    /** 두 번째 wrapper가 독립적 event binding을 가지면 마찬가지로 merge 금지, 전체 결과 0개. */
    private static void testEventBoundaryBlocksButtonWrapperMerge() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "footerActions", 300);
        Element wrapperA = newDiv(doc, "wrapA", null);
        wrapperA.appendChild(newElement(doc, "Button", "cancelBtn", 0, 50, 20));
        Element wrapperB = newDiv(doc, "wrapB", null);
        wrapperB.appendChild(newElement(doc, "Button", "saveBtn", 60, 50, 20));
        container.appendChild(wrapperA);
        container.appendChild(wrapperB);

        // componentId = "footerActions.wrapB"(부모 Div id prefix 규칙).
        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("footerActions.wrapB", "onclick", "toggleSave"));

        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(container, analysis);
        List<SemanticRegionResult> results = filterByType(all, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("wrapper-merge-event-boundary: count", "0", String.valueOf(results.size()));
    }

    /** merge가 막히면 각 wrapper가 독립적인 BUTTON_GROUP.standalone 인스턴스로 각각 남아야
     *  한다(각 wrapper에 자신의 width/geometry 부여). */
    private static void testVisibleBoundaryPreservesTwoSeparateButtonGroupInstances() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "boundaryPreserveScope", 300);
        Element wrapperA = newDiv(doc, "wrapA", 150);
        wrapperA.appendChild(newElement(doc, "Button", "cancelBtn", 0, 50, 20));
        Element wrapperB = newDiv(doc, "wrapB", 150);
        wrapperB.setAttribute("enable", "cond_perm_check");
        wrapperB.appendChild(newElement(doc, "Button", "saveBtn", 90, 50, 20));
        container.appendChild(wrapperA);
        container.appendChild(wrapperB);

        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(container);
        List<SemanticRegionResult> merged = filterByType(all, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("visible-boundary-preserve: no single merged instance at parent scope", "0",
                String.valueOf(countByRegion(merged, "boundaryPreserveScope")));

        SemanticRegionResult resultA = firstByRegion(all, "wrapA");
        SemanticRegionResult resultB = firstByRegion(all, "wrapB");
        assertTrue("visible-boundary-preserve: wrapA survives as its own BUTTON_GROUP instance", resultA != null);
        assertTrue("visible-boundary-preserve: wrapB survives as its own BUTTON_GROUP instance", resultB != null);
        assertEquals("visible-boundary-preserve: wrapA semanticType", "BUTTON_GROUP", resultA.getSemanticType());
        assertEquals("visible-boundary-preserve: wrapB semanticType", "BUTTON_GROUP", resultB.getSemanticType());
        assertEquals("visible-boundary-preserve: wrapA variant standalone", "standalone", resultA.getRecommendedVariant());
        assertEquals("visible-boundary-preserve: wrapB variant standalone", "standalone", resultB.getRecommendedVariant());
    }

    /** 위와 동일하되 boundary 원인이 enable 속성이 아니라 event binding인 경우. */
    private static void testEventBoundaryPreservesTwoSeparateButtonGroupInstances() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "eventBoundaryPreserveScope", 300);
        Element wrapperA = newDiv(doc, "evWrapA", 150);
        wrapperA.appendChild(newElement(doc, "Button", "cancelBtn2", 0, 50, 20));
        Element wrapperB = newDiv(doc, "evWrapB", 150);
        wrapperB.appendChild(newElement(doc, "Button", "saveBtn2", 90, 50, 20));
        container.appendChild(wrapperA);
        container.appendChild(wrapperB);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("eventBoundaryPreserveScope.evWrapB", "onclick", "toggleSave2"));

        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(container, analysis);
        List<SemanticRegionResult> merged = filterByType(all, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("event-boundary-preserve: no single merged instance at parent scope", "0",
                String.valueOf(countByRegion(merged, "eventBoundaryPreserveScope")));

        SemanticRegionResult resultA = firstByRegion(all, "evWrapA");
        SemanticRegionResult resultB = firstByRegion(all, "evWrapB");
        assertTrue("event-boundary-preserve: evWrapA survives as its own BUTTON_GROUP instance", resultA != null);
        assertTrue("event-boundary-preserve: evWrapB survives as its own BUTTON_GROUP instance", resultB != null);
        assertEquals("event-boundary-preserve: evWrapA variant standalone", "standalone", resultA.getRecommendedVariant());
        assertEquals("event-boundary-preserve: evWrapB variant standalone", "standalone", resultB.getRecommendedVariant());
    }

    /** 선행 Static 1개 뒤에 Button 아닌 내용이 섞이면 TITLE_BAR/BUTTON_GROUP 둘 다 미발행. */
    private static void testAmbiguousTitleBarVsButtonGroupIsNotForced() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "mixedHeader", 400);
        container.appendChild(newElement(doc, "Static", "titleLabel", 0, 200, 20));
        container.appendChild(newElement(doc, "Button", "actionBtn", 300, 60, 20));
        container.appendChild(newElement(doc, "Static", "trailingNote", 210, 80, 20));

        List<SemanticRegionResult> results = regionResultsOf(container, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("ambiguous-title-bar-vs-button-group: count", "0", String.valueOf(results.size()));
    }

    /** 선행 Static 2개 이상 연속(CATEGORY_FILTER-like)이면 미구현 CATEGORY_FILTER도, TITLE_BAR/
     *  BUTTON_GROUP 강제 분류도 하지 않는다. */
    private static void testCategoryFilterLikeAmbiguityIsNotForced() throws Exception {
        Document doc = newDocument();
        Element container = newDiv(doc, "filterLikeList", 400);
        container.appendChild(newElement(doc, "Static", "item1", 0, 100, 20));
        container.appendChild(newElement(doc, "Static", "item2", 100, 100, 20));
        container.appendChild(newElement(doc, "Static", "item3", 200, 100, 20));

        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(container);
        List<SemanticRegionResult> titleOrButton = filterByType(all, "TITLE_BAR", "BUTTON_GROUP");
        assertEquals("category-filter-like-ambiguity: title/button count", "0", String.valueOf(titleOrButton.size()));
        assertEquals("category-filter-like-ambiguity: no CATEGORY_FILTER ever emitted",
                "0", String.valueOf(filterByType(all, "CATEGORY_FILTER").size()));
    }

    // ---- fixture 생성 도우미 ----

    private static Element newDiv(Document doc, String id, Integer width) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        if (width != null) {
            div.setAttribute("width", String.valueOf(width));
        }
        return div;
    }

    private static Element newDiv(Document doc, String id, int width) {
        return newDiv(doc, id, Integer.valueOf(width));
    }

    private static Element newElement(Document doc, String tag, String id, double left, double width, double height) {
        Element el = doc.createElement(tag);
        el.setAttribute("id", id);
        el.setAttribute("left", formatAttr(left));
        el.setAttribute("top", "0");
        el.setAttribute("width", formatAttr(width));
        el.setAttribute("height", formatAttr(height));
        return el;
    }

    private static String formatAttr(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static List<SemanticRegionResult> regionResultsOf(Element root, String... semanticTypes) {
        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(root);
        return filterByType(all, semanticTypes);
    }

    private static List<SemanticRegionResult> filterByType(List<SemanticRegionResult> all, String... semanticTypes) {
        List<SemanticRegionResult> filtered = new ArrayList<SemanticRegionResult>();
        for (SemanticRegionResult r : all) {
            for (String type : semanticTypes) {
                if (type.equals(r.getSemanticType())) {
                    filtered.add(r);
                    break;
                }
            }
        }
        return filtered;
    }

    /** {@code sourceRegionId}는 scope-qualified path다. fixture bare id는 트리 안에서 유일하므로
     *  suffix 일치로 찾아도 모호하지 않다. */
    private static boolean regionMatches(String actualRegionId, String expectedBareId) {
        return actualRegionId != null
                && (actualRegionId.equals(expectedBareId) || actualRegionId.endsWith("." + expectedBareId));
    }

    private static int countByRegion(List<SemanticRegionResult> results, String sourceRegionId) {
        int count = 0;
        for (SemanticRegionResult r : results) {
            if (regionMatches(r.getSourceRegionId(), sourceRegionId)) {
                count++;
            }
        }
        return count;
    }

    private static SemanticRegionResult firstByRegion(List<SemanticRegionResult> results, String sourceRegionId) {
        for (SemanticRegionResult r : results) {
            if (regionMatches(r.getSourceRegionId(), sourceRegionId)) {
                return r;
            }
        }
        return null;
    }

    private static SemanticRegionResult firstOfType(List<SemanticRegionResult> results, String semanticType) {
        for (SemanticRegionResult r : results) {
            if (semanticType.equals(r.getSemanticType())) {
                return r;
            }
        }
        return null;
    }

    private static boolean containsMatchedFeature(SemanticRegionResult result, String needle) {
        for (String feature : result.getMatchedFeatures()) {
            if (needle.equals(feature)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsComponentEvidence(SemanticRegionResult result, String needle) {
        for (String evidence : result.getComponentEvidence()) {
            if (needle.equals(evidence)) {
                return true;
            }
        }
        return false;
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
