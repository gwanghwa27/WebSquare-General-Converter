package com.example.xfdltracker.analyzer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

/**
 * {@link ComponentPredicateAnalysis}/{@link ComponentPredicateAnalyzer}(컴포넌트 술어 재사용
 * 가능 증거 캐리어 계약)에 대한 오프라인, 의존성 없는(no JUnit) 단위 테스트.
 */
public class ComponentPredicateAnalyzerTest {

    private static int failures = 0;
    private static final ComponentPredicateAnalyzer ANALYZER = new ComponentPredicateAnalyzer();

    public static void main(String[] args) throws Exception {
        testAllSixFamiliesAlwaysNonNull();
        testNoMatchProducesEmptyImmutableCollections();

        testGridMatched();
        testGridNotMatchedForOtherTag();

        testTabControlMatched();

        testTitleBarMatched();

        testButtonGroupMatched();
        testButtonGroupPositionAndFlattenedButtons();
        testButtonGroupFlattenedButtonsExposeButtonElementFactWithEventComponentPath();
        testButtonGroupFlattenedButtonsAreImmutable();

        testBusinessTableMatchedNoGridPeer();
        testSearchAreaMatchedWithGridPeer();
        testBusinessTableAndSearchAreaShareSameStructureInstance();
        testNoTableStructureWhenGateFails();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testAllSixFamiliesAlwaysNonNull() throws Exception {
        Element bare = element("Static");
        ComponentPredicateAnalysis facts = ANALYZER.analyze(bare, null);
        assertTrue("grid facts non-null", facts.getGrid() != null);
        assertTrue("tabControl facts non-null", facts.getTabControl() != null);
        assertTrue("businessTable facts non-null", facts.getBusinessTable() != null);
        assertTrue("searchArea facts non-null", facts.getSearchArea() != null);
        assertTrue("titleBar facts non-null", facts.getTitleBar() != null);
        assertTrue("buttonGroup facts non-null", facts.getButtonGroup() != null);
    }

    private static void testNoMatchProducesEmptyImmutableCollections() throws Exception {
        Element bare = element("Static");
        ComponentPredicateAnalysis facts = ANALYZER.analyze(bare, null);
        assertTrue("no match: grid.matched=false", !facts.getGrid().isMatched());
        assertTrue("no match: buttonGroup.flattenedButtons is empty", facts.getButtonGroup().getFlattenedButtons().isEmpty());
        boolean threw = false;
        try {
            facts.getButtonGroup().getFlattenedButtons().add(null);
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue("buttonGroup.flattenedButtons is unmodifiable", threw);
    }

    private static void testGridMatched() throws Exception {
        Element grid = element("Grid");
        ComponentPredicateAnalysis facts = ANALYZER.analyze(grid, null);
        assertTrue("Grid tag matches GridFacts", facts.getGrid().isMatched());
        assertTrue("Grid tag does not match TabControlFacts", !facts.getTabControl().isMatched());
    }

    private static void testGridNotMatchedForOtherTag() throws Exception {
        Element other = element("Static");
        assertTrue("non-Grid tag does not match GridFacts", !ANALYZER.analyze(other, null).getGrid().isMatched());
    }

    private static void testTabControlMatched() throws Exception {
        Element tab = element("Tab");
        assertTrue("Tab tag matches TabControlFacts", ANALYZER.analyze(tab, null).getTabControl().isMatched());
    }

    private static void testTitleBarMatched() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        Element staticLabel = doc.createElement("Static");
        staticLabel.setAttribute("id", "s1");
        staticLabel.setAttribute("left", "0");
        Element button = doc.createElement("Button");
        button.setAttribute("id", "b1");
        button.setAttribute("left", "50");
        container.appendChild(staticLabel);
        container.appendChild(button);
        doc.appendChild(container);

        ComponentPredicateAnalysis facts = ANALYZER.analyze(container, null);
        assertTrue("leading single Static + trailing Button matches TitleBarFacts", facts.getTitleBar().isMatched());
        assertTrue("TITLE_BAR match does not also match ButtonGroupFacts", !facts.getButtonGroup().isMatched());
    }

    private static void testButtonGroupMatched() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        container.setAttribute("width", "200");
        Element b1 = doc.createElement("Button");
        b1.setAttribute("id", "b1");
        b1.setAttribute("left", "10");
        b1.setAttribute("width", "40");
        Element b2 = doc.createElement("Button");
        b2.setAttribute("id", "b2");
        b2.setAttribute("left", "60");
        b2.setAttribute("width", "40");
        container.appendChild(b1);
        container.appendChild(b2);
        doc.appendChild(container);

        ComponentPredicateAnalysis facts = ANALYZER.analyze(container, null);
        assertTrue("all-Button children matches ButtonGroupFacts", facts.getButtonGroup().isMatched());
        assertTrue("BUTTON_GROUP match does not also match TitleBarFacts", !facts.getTitleBar().isMatched());
    }

    private static void testButtonGroupPositionAndFlattenedButtons() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        container.setAttribute("width", "200");
        Element b1 = doc.createElement("Button");
        b1.setAttribute("id", "b1");
        b1.setAttribute("left", "10");
        b1.setAttribute("width", "20");
        b1.setAttribute("text", "OK");
        container.appendChild(b1);
        doc.appendChild(container);

        ComponentPredicateAnalysis.ButtonGroupFacts buttonGroup = ANALYZER.analyze(container, null).getButtonGroup();
        assertTrue("position resolved to left_buttons", "left_buttons".equals(buttonGroup.getPosition()));
        assertTrue("exactly one flattened button snapshot", buttonGroup.getFlattenedButtons().size() == 1);
        assertTrue("flattened button text snapshot preserved",
                "OK".equals(buttonGroup.getFlattenedButtons().get(0).getTextAttribute()));
        assertTrue("flattened button structuralId snapshot present",
                buttonGroup.getFlattenedButtons().get(0).getStructuralId() != null);
    }

    private static void testButtonGroupFlattenedButtonsExposeButtonElementFactWithEventComponentPath() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        container.setAttribute("id", "grp");
        container.setAttribute("width", "200");
        Element b1 = doc.createElement("Button");
        b1.setAttribute("id", "b1");
        b1.setAttribute("left", "10");
        b1.setAttribute("width", "20");
        b1.setAttribute("text", "OK");
        Element b2 = doc.createElement("Button");
        b2.setAttribute("id", "b2");
        b2.setAttribute("left", "40");
        b2.setAttribute("width", "20");
        b2.setAttribute("value", "CANCEL");
        container.appendChild(b1);
        container.appendChild(b2);
        doc.appendChild(container);

        List<ComponentPredicateAnalysis.ButtonElementFact> flattened =
                ANALYZER.analyze(container, null).getButtonGroup().getFlattenedButtons();
        assertTrue("exactly two flattened button facts", flattened.size() == 2);
        assertTrue("button 1 eventComponentPath is container-id-prefixed",
                "grp.b1".equals(flattened.get(0).getEventComponentPath()));
        assertTrue("button 2 eventComponentPath is container-id-prefixed",
                "grp.b2".equals(flattened.get(1).getEventComponentPath()));
        assertTrue("eventComponentPath ordering matches flattened button ordering (b1 before b2)",
                flattened.get(0).getEventComponentPath().compareTo(flattened.get(1).getEventComponentPath()) < 0);
        assertTrue("button 1 text preserved", "OK".equals(flattened.get(0).getTextAttribute()));
        assertTrue("button 1 value absent", flattened.get(0).getValueAttribute() == null);
        assertTrue("button 2 text absent", flattened.get(1).getTextAttribute() == null);
        assertTrue("button 2 value fallback preserved", "CANCEL".equals(flattened.get(1).getValueAttribute()));
        assertTrue("button 1 structuralId present", flattened.get(0).getStructuralId() != null);
        assertTrue("button 2 structuralId present", flattened.get(1).getStructuralId() != null);
    }

    private static void testButtonGroupFlattenedButtonsAreImmutable() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        container.setAttribute("width", "200");
        Element b1 = doc.createElement("Button");
        b1.setAttribute("id", "b1");
        b1.setAttribute("left", "10");
        b1.setAttribute("width", "20");
        container.appendChild(b1);
        doc.appendChild(container);

        List<ComponentPredicateAnalysis.ButtonElementFact> flattened =
                ANALYZER.analyze(container, null).getButtonGroup().getFlattenedButtons();
        boolean threw = false;
        try {
            flattened.add(null);
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue("non-empty flattenedButtons is unmodifiable", threw);
    }

    private static void testBusinessTableMatchedNoGridPeer() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element table = labelControlContainer(doc, "table1");
        Element unrelated = doc.createElement("Static");
        unrelated.setAttribute("id", "unrelated");
        form.appendChild(table);
        form.appendChild(unrelated);
        doc.appendChild(form);

        ComponentPredicateAnalysis facts = ANALYZER.analyze(table, null);
        assertTrue("no Grid peer -> BusinessTableFacts.matched", facts.getBusinessTable().isMatched());
        assertTrue("no Grid peer -> SearchAreaFacts not matched", !facts.getSearchArea().isMatched());
        assertTrue("structure snapshot present", facts.getBusinessTable().getStructure() != null);
        assertTrue("peerFoundGrid=false", !facts.getBusinessTable().getStructure().isPeerFoundGrid());
    }

    private static void testSearchAreaMatchedWithGridPeer() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element table = labelControlContainer(doc, "table2");
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grid1");
        form.appendChild(table);
        form.appendChild(grid);
        doc.appendChild(form);

        ComponentPredicateAnalysis facts = ANALYZER.analyze(table, null);
        assertTrue("Grid peer found -> SearchAreaFacts.matched", facts.getSearchArea().isMatched());
        assertTrue("Grid peer found -> BusinessTableFacts not matched", !facts.getBusinessTable().isMatched());
        assertTrue("peerFoundGrid=true", facts.getSearchArea().getStructure().isPeerFoundGrid());
    }

    private static void testBusinessTableAndSearchAreaShareSameStructureInstance() throws Exception {
        Document doc = newDocument();
        Element form = doc.createElement("Form");
        Element table = labelControlContainer(doc, "table3");
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grid1");
        form.appendChild(table);
        form.appendChild(grid);
        doc.appendChild(form);

        ComponentPredicateAnalysis facts = ANALYZER.analyze(table, null);
        assertTrue("BusinessTableFacts.structure and SearchAreaFacts.structure are the exact same instance",
                facts.getBusinessTable().getStructure() == facts.getSearchArea().getStructure());
    }

    private static void testNoTableStructureWhenGateFails() throws Exception {
        Element notAContainer = element("Static");
        ComponentPredicateAnalysis facts = ANALYZER.analyze(notAContainer, null);
        assertTrue("gate fails -> BusinessTableFacts.structure is null", facts.getBusinessTable().getStructure() == null);
        assertTrue("gate fails -> SearchAreaFacts.structure is null", facts.getSearchArea().getStructure() == null);
    }

    private static Element labelControlContainer(Document doc, String id) {
        Element container = doc.createElement("Div");
        container.setAttribute("id", id);
        Element label = doc.createElement("Static");
        label.setAttribute("id", id + "_lbl");
        label.setAttribute("left", "0");
        label.setAttribute("top", "0");
        label.setAttribute("width", "50");
        label.setAttribute("height", "20");
        label.setAttribute("text", "Name");
        Element control = doc.createElement("Edit");
        control.setAttribute("id", id + "_edt");
        control.setAttribute("left", "60");
        control.setAttribute("top", "0");
        control.setAttribute("width", "100");
        control.setAttribute("height", "20");
        container.appendChild(label);
        container.appendChild(control);
        return container;
    }

    private static Element element(String tag) throws Exception {
        Document doc = newDocument();
        Element e = doc.createElement(tag);
        e.setAttribute("id", "e1");
        doc.appendChild(e);
        return e;
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        return f.newDocumentBuilder().newDocument();
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
