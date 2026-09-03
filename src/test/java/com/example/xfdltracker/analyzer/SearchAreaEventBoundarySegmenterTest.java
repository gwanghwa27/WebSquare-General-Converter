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
 * Offline, dependency-free(no JUnit) unit test. {@code segment(Element, XfdlAnalysisResult)}의
 * event boundary evidence를 검증한다. {@link EventBinding}의 componentId 생성 규칙(자신의 id +
 * "Div" 조상 prefix, "."로 연결, Form에서 멈춤)을 그대로 재현해 fixture를 만든다.
 */
public class SearchAreaEventBoundarySegmenterTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testEventBoundWrapperBlocksSearchAreaDetection();
        testNoEventBindingPreservesExistingSearchAreaDetection();
        testNestedScopeSameBareIdDoesNotCauseFalseEventMatch();
        testNoAnalysisArgumentBehavesExactlyLikeSingleArgOverload();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** event-bound wrapper(내부 Grid 중첩)는 투명하게 통과하지 않고 event_boundary에서 멈춘다. */
    private static void testEventBoundWrapperBlocksSearchAreaDetection() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element wrapper = newDiv(doc, "toggleWrapper"); // visible/enable 없음 -- event만으로 불투명해야 함
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "hiddenGrid");
        wrapper.appendChild(nestedGrid);
        parent.appendChild(container);
        parent.appendChild(wrapper);

        // componentId는 "parent.toggleWrapper"여야 정확히 매치된다.
        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("parent.toggleWrapper", "onclick", "toggleConditions"));

        List<SemanticRegionResult> results = regionResultsOf(parent, analysis, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("event-boundary: count", "1", String.valueOf(results.size()));

        SemanticRegionResult r = results.get(0);
        assertEquals("event-boundary: semanticType is BUSINESS_TABLE (no blind merge past event boundary)",
                "BUSINESS_TABLE", r.getSemanticType());
        assertTrue("event-boundary: evidence records event_boundary (not visible_or_enable)",
                containsHierarchyEvidence(r, "wrapper_normalization_stopped_at_event_boundary"));
    }

    /** event binding이 없고 wrapper가 진짜 투명하면 기존 SEARCH_AREA detection이 유지된다. */
    private static void testNoEventBindingPreservesExistingSearchAreaDetection() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element wrapper = newDiv(doc, "toggleWrapper");
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "resultGrid");
        wrapper.appendChild(nestedGrid);
        parent.appendChild(container);
        parent.appendChild(wrapper);

        XfdlAnalysisResult analysis = new XfdlAnalysisResult(); // events 비어있음
        List<SemanticRegionResult> results = regionResultsOf(parent, analysis, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("no-event-binding: count", "1", String.valueOf(results.size()));
        assertEquals("no-event-binding: semanticType is SEARCH_AREA (transparent wrapper skipped as before)",
                "SEARCH_AREA", results.get(0).getSemanticType());
    }

    /** outer/inner 두 scope에 동일 bare id "toggle" wrapper가 있을 때, event binding이 걸린
     *  outer만 막히고 inner는 투명하게 통과해야 한다(오연결 방지). */
    private static void testNestedScopeSameBareIdDoesNotCauseFalseEventMatch() throws Exception {
        // outer scope 구조: parent(Div) > container, outerScope(Div) > toggle(Div, id="toggle", event-bound) > Grid
        Document outerDoc = newDocument();
        Element parent = newDiv(outerDoc, "parent");
        Element outerContainer = newLabelControlPairsRow(outerDoc, "outerRow", 0);
        Element outerScope = newDiv(outerDoc, "outerScope");
        Element outerToggle = newDiv(outerDoc, "toggle"); // bare id "toggle"를 사용한다.
        Element outerGrid = outerDoc.createElement("Grid");
        outerGrid.setAttribute("id", "outerGrid");
        outerToggle.appendChild(outerGrid);
        outerScope.appendChild(outerToggle);
        parent.appendChild(outerContainer);
        parent.appendChild(outerScope);

        // inner scope: 완전히 별도 문서의 parent2(Div) > container2, innerScope(Div) >
        // toggle(Div, 동일 bare id "toggle", event 없음) > Grid.
        Document innerDoc = newDocument();
        Element parent2 = newDiv(innerDoc, "parent2");
        Element innerContainer = newLabelControlPairsRow(innerDoc, "innerRow", 0);
        Element innerScope = newDiv(innerDoc, "innerScope");
        Element innerToggle = newDiv(innerDoc, "toggle"); // 동일 bare id "toggle", 다른 Div 조상 scope
        Element innerGrid = innerDoc.createElement("Grid");
        innerGrid.setAttribute("id", "innerGrid");
        innerToggle.appendChild(innerGrid);
        innerScope.appendChild(innerToggle);
        parent2.appendChild(innerContainer);
        parent2.appendChild(innerScope);

        // outerToggle의 componentId: "parent.outerScope.toggle"(둘 다 Div이므로 prefix됨).
        XfdlAnalysisResult analysis = new XfdlAnalysisResult();
        analysis.getEvents().add(new EventBinding("parent.outerScope.toggle", "onclick", "toggleOuter"));

        List<SemanticRegionResult> outerResults = regionResultsOf(parent, analysis, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("nested-scope-no-false-match: outer count", "1", String.valueOf(outerResults.size()));
        assertEquals("nested-scope-no-false-match: outer blocked by its own event binding",
                "BUSINESS_TABLE", outerResults.get(0).getSemanticType());

        List<SemanticRegionResult> innerResults = regionResultsOf(parent2, analysis, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("nested-scope-no-false-match: inner count", "1", String.valueOf(innerResults.size()));
        assertEquals("nested-scope-no-false-match: inner NOT falsely blocked by outer's event binding (same bare id, different scope)",
                "SEARCH_AREA", innerResults.get(0).getSemanticType());
    }

    /** analysis 인자를 아예 생략한 segment(Element) 호출은 event boundary를 절대 적용하지 않는다. */
    private static void testNoAnalysisArgumentBehavesExactlyLikeSingleArgOverload() throws Exception {
        Document doc = newDocument();
        Element parent = newDiv(doc, "parent");
        Element container = newLabelControlPairsRow(doc, "conditionRow", 0);
        Element wrapper = newDiv(doc, "toggleWrapper");
        Element nestedGrid = doc.createElement("Grid");
        nestedGrid.setAttribute("id", "resultGrid");
        wrapper.appendChild(nestedGrid);
        parent.appendChild(container);
        parent.appendChild(wrapper);

        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(parent); // 기존 1-arg 호출
        List<SemanticRegionResult> results = filterByType(all, "SEARCH_AREA", "BUSINESS_TABLE");
        assertEquals("legacy-1-arg-call: count", "1", String.valueOf(results.size()));
        assertEquals("legacy-1-arg-call: semanticType still SEARCH_AREA (transparent wrapper, no event context)",
                "SEARCH_AREA", results.get(0).getSemanticType());
    }

    // ---- fixture 생성 도우미 ----

    private static Element newLabelControlPairsRow(Document doc, String id, double topOffset) {
        Element container = newDiv(doc, id);
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

    private static Element newDiv(Document doc, String id) {
        Element div = doc.createElement("Div");
        div.setAttribute("id", id);
        return div;
    }

    private static List<SemanticRegionResult> regionResultsOf(
            Element root, XfdlAnalysisResult analysis, String... semanticTypes) {
        List<SemanticRegionResult> all = new SemanticRegionSegmenter().segment(root, analysis);
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

    private static boolean containsHierarchyEvidence(SemanticRegionResult result, String needle) {
        for (String evidence : result.getHierarchyEvidence()) {
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
