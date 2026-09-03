package com.example.xfdltracker.semantic;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * 실제 {@code Element}로부터 DOM ancestry + 형제 position만으로 결정되는 globally-unique
 * identity 문자열을 계산하는 pure helper. id/text/geometry는 보지 않는다. root까지 각 조상의
 * "tag[형제 중 순번]"을 "/"로 이어붙인다 -- 두 Element가 다르면 경로 어딘가에서 반드시 값이 갈린다.
 */
public final class SourceStructuralIdentity {

    private SourceStructuralIdentity() {
    }

    public static String build(Element element) {
        if (element == null) {
            return "";
        }
        List<String> segments = new ArrayList<String>();
        Node current = element;
        while (current instanceof Element) {
            Element currentElement = (Element) current;
            Node parent = currentElement.getParentNode();
            int index = (parent instanceof Element)
                    ? indexAmongElementSiblings((Element) parent, currentElement) : 0;
            segments.add(sourceTagName(currentElement) + "[" + index + "]");
            current = parent;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = segments.size() - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(segments.get(i));
        }
        return sb.toString();
    }

    private static int indexAmongElementSiblings(Element parent, Element target) {
        int index = 0;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node == target) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private static String sourceTagName(Element element) {
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
