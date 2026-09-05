package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.semantic.SourceOptionItem;
import com.example.xfdltracker.semantic.SourceOptionOriginKind;
import com.example.xfdltracker.semantic.SourceOptionResolution;
import com.example.xfdltracker.semantic.SourceOptionSetEvidence;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * narrow option subset(Slice 102D)만 지원하는 독립 resolver다(legacy 참조 없음). 만족하면
 * {@link SourceOptionSetEvidence}, 아니면 {@link SourceOptionResolution} fail-closed reason,
 * evidence가 없으면 {@code null}(plain control)이다. trim/정렬/중복제거는 하지 않는다.
 */
final class SourceOptionSetResolver {

    /** narrow subset이 지원하는 simple Dataset ID predicate(Slice 102D Correction 항목 8) --
     *  proven XPlatform 전체 문법이 아니라 현재 검증된 부분집합만 허용한다. */
    private static final Pattern SIMPLE_DATASET_ID = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private SourceOptionSetResolver() {
    }

    static SourceOptionResolution resolve(Element control) {
        boolean hasInnerDatasetAttr = control.hasAttribute("innerdataset");
        boolean hasCodeColumnAttr = control.hasAttribute("codecolumn");
        boolean hasDataColumnAttr = control.hasAttribute("datacolumn");
        Element inlineDataset = findDirectChildByTag(control, "Dataset");

        // attribute presence 자체(빈 문자열 포함)를 evidence로 본다 -- length()>0만 보면 빈
        // innerdataset="" 선언이 plain으로 잘못 강등된다(Correction 항목 7).
        boolean anyEvidence = hasInnerDatasetAttr || hasCodeColumnAttr || hasDataColumnAttr || inlineDataset != null;
        if (!anyEvidence) {
            // plain control -- 세 attribute 모두 absent이고 direct child Dataset도 absent다.
            return null;
        }

        if (inlineDataset != null) {
            // control 자신의 direct child Dataset이 하나라도 있으면 innerdataset 값(hybrid
            // 포함)과 무관하게 항상 fail-closed한다(Correction 항목 4, RESOLVED 승격 금지).
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_INLINE_UNPROVEN);
        }

        String innerDataset = control.getAttribute("innerdataset");
        if (innerDataset.length() == 0) {
            // attribute 자체가 absent(codecolumn/datacolumn만 있는 malformed 선언)이거나,
            // attribute는 있지만 값이 빈 문자열인 경우 모두 dataset missing으로 fail-closed한다.
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_DATASET_MISSING);
        }
        if (!SIMPLE_DATASET_ID.matcher(innerDataset).matches()) {
            // "@"/"::"/"."/"/"·공백·leading digit 등 simple ID predicate 위반은 legacy scope
            // 정규화가 필요한 non-simple 참조라 out-of-scope로 명시적으로 거부한다.
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
        }
        if (!hasCodeColumnAttr || control.getAttribute("codecolumn").length() == 0) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_CODECOLUMN_MISSING);
        }
        if (!hasDataColumnAttr || control.getAttribute("datacolumn").length() == 0) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_DATACOLUMN_MISSING);
        }
        String codeColumn = control.getAttribute("codecolumn");
        String dataColumn = control.getAttribute("datacolumn");

        // resolution scope authority는 문서 전체가 아니라 control의 nearest enclosing source
        // Form이다(Correction 항목 3, CONTROL_NEAREST_ENCLOSING_FORM = DATASET_RESOLUTION_SCOPE).
        Element enclosingForm = findNearestEnclosingForm(control);
        if (enclosingForm == null) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_DATASET_MISSING);
        }
        List<Element> allMatchesInForm = findDescendantsByTagAndId(enclosingForm, "Dataset", innerDataset);
        if (allMatchesInForm.isEmpty()) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_DATASET_MISSING);
        }
        // 허용 external 위치는 정확히 Form>Dataset 또는 Form>Objects>Dataset뿐이다 -- 다른
        // control의 inline child Dataset 등은 이 필터에서 항상 제외된다(Correction 항목 3/5).
        List<Element> allowedMatches = filterAllowedExternalLocation(allMatchesInForm, enclosingForm);
        if (allowedMatches.isEmpty()) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_SCOPE_UNSUPPORTED);
        }
        if (allowedMatches.size() > 1) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_DATASET_AMBIGUOUS);
        }
        Element dataset = allowedMatches.get(0);

        Element columnInfo = findDirectChildByTag(dataset, "ColumnInfo");
        List<Element> columns = columnInfo == null
                ? new ArrayList<Element>() : directChildrenByTag(columnInfo, "Column");
        int codeColumnMatches = countById(columns, codeColumn);
        if (codeColumnMatches == 0) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_COLUMN_NOT_FOUND);
        }
        if (codeColumnMatches > 1) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_COLUMN_AMBIGUOUS);
        }
        int dataColumnMatches = countById(columns, dataColumn);
        if (dataColumnMatches == 0) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_COLUMN_NOT_FOUND);
        }
        if (dataColumnMatches > 1) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_COLUMN_AMBIGUOUS);
        }

        Element rowsElement = findDirectChildByTag(dataset, "Rows");
        if (rowsElement == null) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROWS_MISSING);
        }
        List<Element> rowElements = directChildrenByTag(rowsElement, "Row");
        if (rowElements.isEmpty()) {
            return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROWS_EMPTY);
        }

        List<SourceOptionItem> items = new ArrayList<SourceOptionItem>();
        Set<String> seenValues = new HashSet<String>();
        for (int rowOrdinal = 0; rowOrdinal < rowElements.size(); rowOrdinal++) {
            Element row = rowElements.get(rowOrdinal);
            List<Element> cols = directChildrenByTag(row, "Col");

            List<Element> valueCols = colsById(cols, codeColumn);
            if (valueCols.isEmpty()) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROW_VALUE_MISSING);
            }
            if (valueCols.size() > 1) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROW_VALUE_AMBIGUOUS);
            }
            List<Element> labelCols = colsById(cols, dataColumn);
            if (labelCols.isEmpty()) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROW_LABEL_MISSING);
            }
            if (labelCols.size() > 1) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_ROW_LABEL_AMBIGUOUS);
            }

            // XML parsing으로 얻은 실제 semantic text 그대로 보존한다 -- trim 없음.
            String value = valueCols.get(0).getTextContent();
            String label = labelCols.get(0).getTextContent();
            if (value == null || value.length() == 0) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_VALUE_EMPTY);
            }
            if (label == null || label.length() == 0) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_LABEL_EMPTY);
            }
            if (!seenValues.add(value)) {
                return SourceOptionResolution.failed(SourceOptionResolution.REASON_VALUE_DUPLICATE);
            }
            items.add(new SourceOptionItem(rowOrdinal, value, label));
        }

        SourceOptionSetEvidence evidence = new SourceOptionSetEvidence(
                SourceStructuralIdentity.build(control), SourceStructuralIdentity.build(dataset), innerDataset,
                SourceOptionOriginKind.EXTERNAL_FORM_LOCAL_DATASET_REFERENCE, codeColumn, dataColumn, items);
        return SourceOptionResolution.resolved(evidence);
    }

    /** control의 조상을 거슬러 올라가며 첫 번째로 만나는 tag="Form" element를 찾는다(문서
     *  root가 Form 자체인 경우와 FDL로 감싸인 경우 모두 동일하게 동작). */
    private static Element findNearestEnclosingForm(Element control) {
        Node parent = control.getParentNode();
        while (parent instanceof Element) {
            Element parentElement = (Element) parent;
            if ("Form".equals(sourceTagName(parentElement))) {
                return parentElement;
            }
            parent = parentElement.getParentNode();
        }
        return null;
    }

    /** 후보 중 부모가 enclosingForm 자신이거나(Form&gt;Dataset), 부모가 Objects이고 그
     *  Objects의 부모가 enclosingForm인 것(Form&gt;Objects&gt;Dataset)만 남긴다. */
    private static List<Element> filterAllowedExternalLocation(List<Element> candidates, Element enclosingForm) {
        List<Element> allowed = new ArrayList<Element>();
        for (Element candidate : candidates) {
            Node parentNode = candidate.getParentNode();
            if (!(parentNode instanceof Element)) {
                continue;
            }
            Element parent = (Element) parentNode;
            if (parent == enclosingForm) {
                allowed.add(candidate);
            } else if ("Objects".equals(sourceTagName(parent)) && parent.getParentNode() == enclosingForm) {
                allowed.add(candidate);
            }
        }
        return allowed;
    }

    private static Element findDirectChildByTag(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && tag.equals(sourceTagName((Element) node))) {
                return (Element) node;
            }
        }
        return null;
    }

    private static List<Element> directChildrenByTag(Element parent, String tag) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && tag.equals(sourceTagName((Element) node))) {
                result.add((Element) node);
            }
        }
        return result;
    }

    /** 같은 source Form 문서 전체(깊이 무관)에서 tag+id가 정확히 일치하는 Element를 전부 모은다
     *  (경로 가정 없음 -- Objects wrapper 유무 등 위치 variation과 무관하게 항상 동작해야 한다). */
    private static List<Element> findDescendantsByTagAndId(Element root, String tag, String id) {
        List<Element> out = new ArrayList<Element>();
        collectDescendantsByTagAndId(root, tag, id, out);
        return out;
    }

    private static void collectDescendantsByTagAndId(Element element, String tag, String id, List<Element> out) {
        if (tag.equals(sourceTagName(element)) && id.equals(element.getAttribute("id"))) {
            out.add(element);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                collectDescendantsByTagAndId((Element) node, tag, id, out);
            }
        }
    }

    private static int countById(List<Element> elements, String id) {
        int count = 0;
        for (Element e : elements) {
            if (id.equals(e.getAttribute("id"))) {
                count++;
            }
        }
        return count;
    }

    private static List<Element> colsById(List<Element> cols, String id) {
        List<Element> result = new ArrayList<Element>();
        for (Element c : cols) {
            if (id.equals(c.getAttribute("id"))) {
                result.add(c);
            }
        }
        return result;
    }

    private static String sourceTagName(Element element) {
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
