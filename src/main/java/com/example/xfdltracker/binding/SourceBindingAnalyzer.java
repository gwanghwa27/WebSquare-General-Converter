package com.example.xfdltracker.binding;

import com.example.xfdltracker.semantic.SourceStructuralIdentity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * source 전체에서 실제 corpus로 증명된 generic binding 선언(BindItem)을 읽기 전용으로 순회해
 * {@link SourceBindingReference} 목록으로 만든다. raw source를 스캔하는 유일한 accepted-path
 * 지점이며, 다운스트림은 이 결과만 쓴다. CheckBox 등 특정 컴포넌트를 이름으로 특별 취급하지 않는다.
 */
public final class SourceBindingAnalyzer {

    public List<SourceBindingReference> analyze(Element sourceRoot) {
        List<SourceBindingReference> result = new ArrayList<SourceBindingReference>();
        if (sourceRoot == null) {
            return result;
        }
        Map<String, List<Element>> elementsById = new LinkedHashMap<String, List<Element>>();
        indexElementsById(sourceRoot, elementsById);

        List<Element> bindItems = new ArrayList<Element>();
        collectBindItems(sourceRoot, bindItems);

        for (Element bindItem : bindItems) {
            String compid = firstNonEmpty(bindItem, "compid", "componentid");
            String propid = firstNonEmpty(bindItem, "propid", "propertyid");
            String datasetid = firstNonEmpty(bindItem, "datasetid", "dataset");
            String columnid = firstNonEmpty(bindItem, "columnid", "column");

            SourceBindingReference.ComponentResolution resolution;
            String resolvedId = null;
            List<String> candidateIds = new ArrayList<String>();
            if (compid.length() == 0) {
                resolution = SourceBindingReference.ComponentResolution.UNRESOLVED_NO_COMPONENT_MATCH;
            } else {
                List<Element> matches = elementsById.get(compid);
                if (matches == null || matches.isEmpty()) {
                    resolution = SourceBindingReference.ComponentResolution.UNRESOLVED_NO_COMPONENT_MATCH;
                } else if (matches.size() > 1) {
                    // 같은 id를 가진 Element가 문서 안에 2개 이상이면 첫 값을 고르지 않고 명시적으로
                    // ambiguous 상태로 남긴다(legacy BindingModel.findComponentBinding과 동일한 전제).
                    // 후보 전부를 버리지 않고 candidate identity로 보존한다(누가 승자인지 정하지 않음).
                    resolution = SourceBindingReference.ComponentResolution.UNRESOLVED_AMBIGUOUS_COMPONENT_MATCH;
                    for (Element match : matches) {
                        candidateIds.add(SourceStructuralIdentity.build(match));
                    }
                } else {
                    resolution = SourceBindingReference.ComponentResolution.RESOLVED_EXACT_ONE_COMPONENT;
                    resolvedId = SourceStructuralIdentity.build(matches.get(0));
                    candidateIds.add(resolvedId);
                }
            }

            result.add(new SourceBindingReference(
                    SourceStructuralIdentity.build(bindItem), compid, propid, datasetid, columnid,
                    resolution, resolvedId, candidateIds));
        }
        return result;
    }

    private void collectBindItems(Element element, List<Element> out) {
        if ("BindItem".equals(sourceTagName(element))) {
            out.add(element);
        }
        for (Element child : directElementChildren(element)) {
            collectBindItems(child, out);
        }
    }

    /** id attribute가 있는 모든 Element를 id 값으로 인덱싱한다(중복 id도 그대로 List에 누적). */
    private void indexElementsById(Element element, Map<String, List<Element>> out) {
        String id = element.getAttribute("id");
        if (id != null && id.length() > 0) {
            List<Element> list = out.get(id);
            if (list == null) {
                list = new ArrayList<Element>();
                out.put(id, list);
            }
            list.add(element);
        }
        for (Element child : directElementChildren(element)) {
            indexElementsById(child, out);
        }
    }

    private String firstNonEmpty(Element element, String primaryAttr, String fallbackAttr) {
        String value = element.getAttribute(primaryAttr);
        if (value != null && value.length() > 0) {
            return value;
        }
        String fallback = element.getAttribute(fallbackAttr);
        return fallback == null ? "" : fallback;
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
        String localName = element.getLocalName();
        if (localName != null && localName.length() > 0) {
            return localName;
        }
        String tagName = element.getTagName();
        int colon = tagName.indexOf(':');
        return colon >= 0 ? tagName.substring(colon + 1) : tagName;
    }
}
