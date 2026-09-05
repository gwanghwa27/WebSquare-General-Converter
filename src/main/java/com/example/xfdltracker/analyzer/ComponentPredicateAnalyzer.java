package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.BusinessTableFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.ButtonElementFact;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.ButtonGroupFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.GridFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.SearchAreaFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.TabControlFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.TableCellFact;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.TableStructureFacts;
import com.example.xfdltracker.analyzer.ComponentPredicateAnalysis.TitleBarFacts;
import com.example.xfdltracker.converter.ComponentLayoutConverter;
import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.semantic.SourceOptionResolution;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Component Analyzer stage 구현. GRID/TAB_CONTROL/BUSINESS_TABLE/SEARCH_AREA/TITLE_BAR/
 * BUTTON_GROUP의 component-level predicate 계산만 담당한다(동일 threshold/어휘/geometry 규칙).
 * confidence/variant/target-template는 만들지 않으며, SPLIT_LAYOUT은 범위 밖이다.
 */
public final class ComponentPredicateAnalyzer {

    private static final List<String> SHARED_LABEL_CONTROL_TAGS =
            Arrays.asList("Edit", "Combo", "Calendar", "CheckBox", "Radio");

    private final ComponentLayoutConverter layoutConverter = new ComponentLayoutConverter();
    private final ComponentMappingRegistry componentMappings = new ComponentMappingRegistry();

    public ComponentPredicateAnalysis analyze(Element component, XfdlAnalysisResult analysis) {
        if (component == null) {
            throw new IllegalArgumentException("component_predicate_analyzer: component must not be null");
        }

        GridFacts grid = new GridFacts("Grid".equals(sourceTagName(component)));
        TabControlFacts tabControl = new TabControlFacts("Tab".equals(sourceTagName(component)));

        List<Element> children = directElementChildren(component);
        boolean isContainer = componentMappings.isContainer(sourceTagName(component));

        TitleBarFacts titleBar = analyzeTitleBar(isContainer, children);

        Set<String> eventBoundComponentPaths = collectEventBoundComponentPaths(analysis);
        ButtonGroupFacts buttonGroup = analyzeButtonGroup(component, isContainer, children, eventBoundComponentPaths);

        TableStructureFacts sharedStructure = null;
        boolean businessTableMatched = false;
        boolean searchAreaMatched = false;
        if (isContainer && children.size() >= 2) {
            String geometryStatus = layoutConverter.classifyLayoutGeometry(children);
            if ("TABLE_LAYOUT_HIGH_CONFIDENCE".equals(geometryStatus)) {
                List<List<Element>> rows = layoutConverter.buildTableRows(children);
                if (!rows.isEmpty() && allRowsExclusivelyLabelControlPairs(rows)) {
                    PeerSearchResult peer = findNearestStructuralPeer(component, eventBoundComponentPaths);
                    sharedStructure = snapshotTableStructure(rows, peer);
                    if (peer.foundGrid) {
                        searchAreaMatched = true;
                    } else {
                        businessTableMatched = true;
                    }
                }
            }
        }

        BusinessTableFacts businessTable = new BusinessTableFacts(businessTableMatched, sharedStructure);
        SearchAreaFacts searchArea = new SearchAreaFacts(searchAreaMatched, sharedStructure);

        return new ComponentPredicateAnalysis(grid, tabControl, businessTable, searchArea, titleBar, buttonGroup);
    }

    // ---- TITLE_BAR 판별 ----------------------------------------------------------------------------

    private TitleBarFacts analyzeTitleBar(boolean isContainer, List<Element> children) {
        if (!isContainer || children.isEmpty()) {
            return new TitleBarFacts(false);
        }
        int leadingStaticCount = leadingStaticCount(children);
        if (leadingStaticCount != 1) {
            return new TitleBarFacts(false);
        }
        return new TitleBarFacts(titleBarStructureMatches(children));
    }

    /** 기존 predicate를 그대로 재현(threshold/어휘 불변). */
    boolean titleBarStructureMatches(List<Element> children) {
        if (children.isEmpty() || !"Static".equals(sourceTagName(children.get(0)))) {
            return false;
        }
        List<Element> rest = children.subList(1, children.size());
        if (!rest.isEmpty() && !allButtons(rest)) {
            return false;
        }
        return leadingStaticHasMinimumLeft(children);
    }

    private boolean leadingStaticHasMinimumLeft(List<Element> children) {
        Double leadingLeft = parseDoubleAttr(children.get(0), "left");
        if (leadingLeft == null) {
            return false;
        }
        for (int i = 1; i < children.size(); i++) {
            Double otherLeft = parseDoubleAttr(children.get(i), "left");
            if (otherLeft == null || otherLeft < leadingLeft) {
                return false;
            }
        }
        return true;
    }

    private int leadingStaticCount(List<Element> children) {
        int count = 0;
        while (count < children.size() && "Static".equals(sourceTagName(children.get(count)))) {
            count++;
        }
        return count;
    }

    private boolean allButtons(List<Element> elements) {
        for (Element element : elements) {
            if (!"Button".equals(sourceTagName(element))) {
                return false;
            }
        }
        return true;
    }

    // ---- BUTTON_GROUP 판별 ---------------------------------------------------------------------------

    private ButtonGroupFacts analyzeButtonGroup(
            Element container, boolean isContainer, List<Element> children, Set<String> eventBoundComponentPaths) {
        if (!isContainer || children.isEmpty() || leadingStaticCount(children) != 0) {
            return new ButtonGroupFacts(false, false, null, Collections.<ButtonElementFact>emptyList());
        }

        List<Element> flattenedButtons = new ArrayList<Element>();
        boolean anyWrapperMerged = false;
        for (Element child : children) {
            String tag = sourceTagName(child);
            if ("Button".equals(tag)) {
                flattenedButtons.add(child);
                continue;
            }
            if ("Div".equals(tag)) {
                List<Element> wrapperChildren = directElementChildren(child);
                if (!wrapperChildren.isEmpty() && allButtons(wrapperChildren)) {
                    String reason = opaqueBoundaryReason(child, eventBoundComponentPaths);
                    if (reason != null) {
                        return new ButtonGroupFacts(false, false, null, Collections.<ButtonElementFact>emptyList());
                    }
                    flattenedButtons.addAll(wrapperChildren);
                    anyWrapperMerged = true;
                    continue;
                }
            }
            return new ButtonGroupFacts(false, false, null, Collections.<ButtonElementFact>emptyList());
        }
        if (flattenedButtons.isEmpty()) {
            return new ButtonGroupFacts(false, false, null, Collections.<ButtonElementFact>emptyList());
        }

        String position = determineButtonGroupPosition(container, flattenedButtons);
        return finishButtonGroup(anyWrapperMerged, flattenedButtons, position);
    }

    private ButtonGroupFacts finishButtonGroup(boolean anyWrapperMerged, List<Element> flattenedButtons, String position) {
        if (position == null) {
            return new ButtonGroupFacts(false, false, null, Collections.<ButtonElementFact>emptyList());
        }
        List<ButtonElementFact> snapshot = new ArrayList<ButtonElementFact>();
        for (Element button : flattenedButtons) {
            snapshot.add(new ButtonElementFact(
                    SourceStructuralIdentity.build(button),
                    nullableAttribute(button, "text"),
                    nullableAttribute(button, "value"),
                    buildEventComponentPath(button)));
        }
        return new ButtonGroupFacts(true, anyWrapperMerged, position, snapshot);
    }

    private String determineButtonGroupPosition(Element container, List<Element> buttons) {
        if (container == null) {
            return null;
        }
        Double containerWidth = parseDoubleAttr(container, "width");
        if (containerWidth == null || containerWidth <= 0) {
            return null;
        }
        double midpoint = containerWidth / 2.0;
        boolean anyLeft = false;
        boolean anyRight = false;
        for (Element button : buttons) {
            Double left = parseDoubleAttr(button, "left");
            if (left == null) {
                return null;
            }
            Double width = parseDoubleAttr(button, "width");
            double center = left + (width == null ? 0.0 : width / 2.0);
            if (center < midpoint) {
                anyLeft = true;
            } else {
                anyRight = true;
            }
        }
        if (anyLeft == anyRight) {
            return null;
        }
        return anyLeft ? "left_buttons" : "right_buttons";
    }

    // ---- BUSINESS_TABLE / SEARCH_AREA 공용 구조 ----

    private boolean allRowsExclusivelyLabelControlPairs(List<List<Element>> rows) {
        for (List<Element> row : rows) {
            if (!isExclusivelyLabelControlPairs(row)) {
                return false;
            }
        }
        return true;
    }

    private boolean isExclusivelyLabelControlPairs(List<Element> elements) {
        if (elements.isEmpty() || elements.size() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < elements.size(); i += 2) {
            String labelTag = sourceTagName(elements.get(i));
            String controlTag = sourceTagName(elements.get(i + 1));
            if (!"Static".equals(labelTag) || !SHARED_LABEL_CONTROL_TAGS.contains(controlTag)) {
                return false;
            }
        }
        return true;
    }

    private TableStructureFacts snapshotTableStructure(List<List<Element>> rows, PeerSearchResult peer) {
        List<List<TableCellFact>> snapshot = new ArrayList<List<TableCellFact>>();
        for (List<Element> row : rows) {
            List<TableCellFact> rowFacts = new ArrayList<TableCellFact>();
            for (Element cell : row) {
                String tag = sourceTagName(cell);
                // Combo/Radio만 option 선언을 가질 수 있다. 이 시점엔 SEARCH_AREA/BUSINESS_TABLE
                // 분류가 아직 안 끝났으므로(GRID peer 판정은 이후) family와 무관하게 항상 계산한다
                // -- family별 소비/강제는 TargetPayloadExtractor가 결정한다.
                SourceOptionResolution optionResolution = ("Combo".equals(tag) || "Radio".equals(tag))
                        ? SourceOptionSetResolver.resolve(cell) : null;
                rowFacts.add(new TableCellFact(
                        SourceStructuralIdentity.build(cell), tag,
                        nullableAttribute(cell, "text"), nullableAttribute(cell, "value"), optionResolution));
            }
            snapshot.add(rowFacts);
        }
        return new TableStructureFacts(snapshot, peer.foundGrid, peer.opaqueBoundaryReason);
    }

    private static final class PeerSearchResult {
        private boolean foundGrid;
        private String opaqueBoundaryReason;
    }

    private PeerSearchResult findNearestStructuralPeer(Element container, Set<String> eventBoundComponentPaths) {
        Node parentNode = container.getParentNode();
        if (!(parentNode instanceof Element)) {
            return new PeerSearchResult();
        }
        List<Element> siblings = directElementChildren((Element) parentNode);
        int idx = indexOfIdentity(siblings, container);
        if (idx < 0 || idx + 1 >= siblings.size()) {
            return new PeerSearchResult();
        }
        return searchForwardFrom(siblings, idx + 1, eventBoundComponentPaths);
    }

    private PeerSearchResult searchForwardFrom(
            List<Element> siblings, int fromIndexInclusive, Set<String> eventBoundComponentPaths) {
        for (int i = fromIndexInclusive; i < siblings.size(); i++) {
            Element candidate = siblings.get(i);
            if ("Grid".equals(sourceTagName(candidate))) {
                PeerSearchResult found = new PeerSearchResult();
                found.foundGrid = true;
                return found;
            }
            String reason = opaqueBoundaryReason(candidate, eventBoundComponentPaths);
            if (reason != null) {
                PeerSearchResult blocked = new PeerSearchResult();
                blocked.opaqueBoundaryReason = reason;
                return blocked;
            }
            List<Element> innerChildren = directElementChildren(candidate);
            if (!innerChildren.isEmpty()) {
                PeerSearchResult inner = searchForwardFrom(innerChildren, 0, eventBoundComponentPaths);
                if (inner.foundGrid || inner.opaqueBoundaryReason != null) {
                    return inner;
                }
            }
        }
        return new PeerSearchResult();
    }

    private String opaqueBoundaryReason(Element element, Set<String> eventBoundComponentPaths) {
        if (hasNonEmptyAttribute(element, "visible") || hasNonEmptyAttribute(element, "enable")) {
            return "visible_or_enable_boundary";
        }
        String path = buildEventComponentPath(element);
        if (path.length() > 0 && eventBoundComponentPaths.contains(path)) {
            return "event_boundary";
        }
        return null;
    }

    private boolean hasNonEmptyAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value != null && value.trim().length() > 0;
    }

    private int indexOfIdentity(List<Element> elements, Element target) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    // ---- 공용 저수준 도우미(기계적 DOM 탐색, 이관된 predicate 로직 아님) ----

    private String buildEventComponentPath(Element element) {
        String ownId = trimOrEmpty(element.getAttribute("id"));
        if (ownId.length() == 0) {
            return "";
        }
        String path = ownId;
        Node parent = element.getParentNode();
        while (parent instanceof Element) {
            Element parentElement = (Element) parent;
            String tag = sourceTagName(parentElement);
            if ("Form".equals(tag)) {
                break;
            }
            if ("Div".equals(tag)) {
                String parentId = trimOrEmpty(parentElement.getAttribute("id"));
                if (parentId.length() > 0) {
                    path = parentId + "." + path;
                }
            }
            parent = parent.getParentNode();
        }
        return path;
    }

    private Set<String> collectEventBoundComponentPaths(XfdlAnalysisResult analysis) {
        if (analysis == null) {
            return Collections.emptySet();
        }
        Set<String> paths = new HashSet<String>();
        for (EventBinding event : analysis.getEvents()) {
            String componentId = trimOrEmpty(event.getComponentId());
            if (componentId.length() > 0) {
                paths.add(componentId);
            }
        }
        return paths;
    }

    private Double parseDoubleAttr(Element element, String name) {
        String raw = element.getAttribute(name);
        if (raw == null || raw.trim().length() == 0) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nullableAttribute(Element element, String name) {
        String raw = element.getAttribute(name);
        return (raw == null || raw.trim().length() == 0) ? null : raw.trim();
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private List<Element> directElementChildren(Element parent) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private String sourceTagName(Element element) {
        if (element == null) {
            return "";
        }
        String localName = element.getLocalName();
        if (localName != null && localName.length() > 0) {
            return localName;
        }
        String tagName = element.getTagName();
        if (tagName == null) {
            return "";
        }
        int colon = tagName.indexOf(':');
        return colon >= 0 && colon + 1 < tagName.length() ? tagName.substring(colon + 1) : tagName;
    }
}
