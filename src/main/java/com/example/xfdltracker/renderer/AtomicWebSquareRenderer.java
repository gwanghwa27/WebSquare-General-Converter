package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.composition.TargetNodeIdentity;
import com.example.xfdltracker.composition.TargetNodeIdentityKind;
import com.example.xfdltracker.payload.TargetEventBinding;
import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.payload.TargetPayloadCategory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@code TargetCompositionPlan} + validated Target Payload만 소비하는 Atomic WebSquare Renderer.
 * source DOM/{@code XfdlAnalysisResult}/{@code SemanticRegionResult}는 참조하지 않는다(architecture test로 검증).
 * frozen target contract(root_tag/required_structure/slots)를 따르며, evidence 없는 부분은 항상 빈 shell만 만든다.
 */
public final class AtomicWebSquareRenderer {

    private static final String NS_XF = "http://www.w3.org/2002/xforms";
    private static final String NS_W2 = "http://www.inswave.com/websquare";
    /** BUTTON_GROUP 이벤트 attribute의 EV 네임스페이스 -- Reviewer 승인 값(표준 XML Events 네임스페이스),
     * 레거시 {@code WebSquareGenerator}에서 유도/검증한 값이 아니다. */
    private static final String NS_EV = "http://www.w3.org/2001/xml-events";
    private static final String BUSINESS_TABLE_VARIANT_HORIZONTAL = "horizontal";

    /**
     * @param plan 이미 검증된 {@link TargetCompositionPlan}
     * @param payloads validated Target Payload 목록(순서 무관 -- nodeId로 색인 후 plan 순서로 순회)
     * @return plan.getNodes()와 정확히 같은 순서/개수의 결과(node당 결과 하나, 생략 없음)
     */
    public List<AtomicRenderResult> render(TargetCompositionPlan plan, List<TargetNodePayload> payloads) {
        Map<String, List<TargetNodePayload>> payloadsByNodeId = indexByNodeId(payloads);

        // family별 렌더링 이전에, Plan의 node identity 집합에 없는 orphan payload가 있는지부터
        // 전역으로 확인한다 -- family-agnostic한 Plan<->Payload 정합성 문제다.
        Set<String> planNodeIds = new java.util.LinkedHashSet<String>();
        for (TargetCompositionNode node : plan.getNodes()) {
            planNodeIds.add(node.getNodeId());
        }
        for (String payloadNodeId : payloadsByNodeId.keySet()) {
            if (!planNodeIds.contains(payloadNodeId)) {
                throw new IllegalStateException(
                        "atomic_web_square_renderer: orphan payload -- planNodeId=\"" + payloadNodeId
                                + "\" does not correspond to any node in the given TargetCompositionPlan "
                                + "(stale/tampered payload identity; refusing to silently ignore it)");
            }
        }

        List<AtomicRenderResult> results = new ArrayList<AtomicRenderResult>();
        for (TargetCompositionNode node : plan.getNodes()) {
            String family = node.getFamily();
            if ("TITLE_BAR".equals(family)) {
                results.add(renderTitleBarNode(node, payloadsByNodeId));
            } else if ("BUSINESS_TABLE".equals(family)) {
                results.add(renderBusinessTableNode(node, payloadsByNodeId));
            } else if ("SPLIT_LAYOUT".equals(family)) {
                results.add(renderSplitLayoutNode(node, payloadsByNodeId));
            } else if ("GRID".equals(family)) {
                results.add(renderGridNode(node, payloadsByNodeId));
            } else if ("TAB_CONTROL".equals(family)) {
                results.add(renderTabControlNode(node, payloadsByNodeId));
            } else if ("SEARCH_AREA".equals(family)) {
                results.add(renderSearchAreaNode(node, payloadsByNodeId));
            } else if ("BUTTON_GROUP".equals(family)) {
                results.add(renderButtonGroupNode(node, payloadsByNodeId));
            } else {
                results.add(AtomicRenderResult.notSupported(
                        node.getIdentity(), RenderStatus.UNSUPPORTED_FAMILY,
                        "unsupported_family:" + family));
            }
        }
        return results;
    }

    /**
     * Plan-node-당-envelope 계약 -- envelope 0개("leaf 0개"와 다름)와 2개 이상 모두 명시적으로
     * 거부한다. 통과하면 null을 반환한다.
     */
    private String envelopeCountFailureReason(String nodeId, List<TargetNodePayload> matches) {
        if (matches == null || matches.isEmpty()) {
            return "missing_payload_envelope_for_node:" + nodeId;
        }
        if (matches.size() > 1) {
            return "duplicate_payload_for_node:" + nodeId;
        }
        return null;
    }

    /**
     * {@code (IDENTITY_KIND, IDENTITY_VALUE)} tuple correlation을 모든 supported family에 균일 적용하는 helper.
     * kind/value가 각각 정확히 일치해야만 correlation 성공이다 -- value만 같아도 kind가 다르면 실패다.
     */
    private static final class PayloadResolution {
        final TargetNodePayload envelope;
        final String failureReason;
        private PayloadResolution(TargetNodePayload envelope, String failureReason) {
            this.envelope = envelope;
            this.failureReason = failureReason;
        }
        static PayloadResolution resolved(TargetNodePayload envelope) {
            return new PayloadResolution(envelope, null);
        }
        static PayloadResolution failed(String reason) {
            return new PayloadResolution(null, reason);
        }
    }

    private PayloadResolution resolveExactlyOneCorrelatedPayload(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        List<TargetNodePayload> matches = payloadsByNodeId.get(node.getNodeId());
        String envelopeFailure = envelopeCountFailureReason(node.getNodeId(), matches);
        if (envelopeFailure != null) {
            return PayloadResolution.failed(envelopeFailure);
        }
        TargetNodePayload envelope = matches.get(0);
        TargetNodeIdentityKind expectedKind = node.getIdentityKind();
        if (envelope.getIdentityKind() != expectedKind) {
            return PayloadResolution.failed(
                    "identity_kind_mismatch:expected=" + expectedKind + ":actual=" + envelope.getIdentityKind());
        }
        if (!node.getNodeId().equals(envelope.getIdentityValue())) {
            return PayloadResolution.failed(
                    "identity_value_mismatch:expected=" + node.getNodeId()
                            + ":actual=" + envelope.getIdentityValue());
        }
        return PayloadResolution.resolved(envelope);
    }

    private Map<String, List<TargetNodePayload>> indexByNodeId(List<TargetNodePayload> payloads) {
        Map<String, List<TargetNodePayload>> index = new LinkedHashMap<String, List<TargetNodePayload>>();
        if (payloads == null) {
            return index;
        }
        for (TargetNodePayload payload : payloads) {
            List<TargetNodePayload> bucket = index.get(payload.getPlanNodeId());
            if (bucket == null) {
                bucket = new ArrayList<TargetNodePayload>();
                index.put(payload.getPlanNodeId(), bucket);
            }
            bucket.add(payload);
        }
        return index;
    }

    private AtomicRenderResult renderTitleBarNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"title_only".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:TITLE_BAR:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        // items가 비어 있는 것은 title_label slot의 0 cardinality(frozen contract가 허용)에 해당하는 정상 상태다.
        List<TargetLeafPayload> items = resolution.envelope.getItems();

        String titleLabelText = null;
        int titleLabelCount = 0;
        for (TargetLeafPayload item : items) {
            if (item.getCategory() != TargetPayloadCategory.DISPLAY_TEXT) {
                return AtomicRenderResult.notSupported(
                        node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION,
                        "unexpected_leaf_category:TITLE_BAR:" + item.getCategory());
            }
            titleLabelCount++;
            titleLabelText = item.getValue();
        }
        if (titleLabelCount > 1) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION,
                    "title_label_cardinality_exceeded:" + titleLabelCount);
        }

        Element dfbox = buildTitleBarFragment(titleLabelCount == 1 ? titleLabelText : null);
        return AtomicRenderResult.rendered(node.getIdentity(), dfbox);
    }

    /**
     * frozen required_structure를 그대로 따른다: text가 있으면 {@code dfbox > f1 > df_tit},
     * 없으면 빈 {@code dfbox}만(leading_extra/right_actions는 evidence 없음으로 항상 생략).
     */
    private Element buildTitleBarFragment(String titleLabelText) {
        Document doc = newTargetDocument();
        Element dfbox = doc.createElementNS(NS_XF, "xf:group");
        dfbox.setAttribute("class", "dfbox");
        if (titleLabelText != null) {
            Element f1 = doc.createElementNS(NS_XF, "xf:group");
            f1.setAttribute("class", "f1");
            Element dfTit = doc.createElementNS(NS_W2, "w2:textbox");
            dfTit.setAttribute("class", "df_tit");
            dfTit.setTextContent(titleLabelText);
            f1.appendChild(dfTit);
            dfbox.appendChild(f1);
        }
        doc.appendChild(dfbox);
        return dfbox;
    }

    /**
     * {@code SPLIT_LAYOUT}의 structural_classes({@code lybox}/{@code ly_column})만 렌더링한다.
     * slot의 실제 자식은 {@code CompositionRenderer}가 edge를 통해 조립한다(이 클래스는 node 하나만 본다).
     * variant는 predicate가 실제로 emit하는 {@code ratio_split}만 지원한다.
     */
    private AtomicRenderResult renderSplitLayoutNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"ratio_split".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:SPLIT_LAYOUT:" + node.getVariant());
        }

        // structural-only family라도 envelope 검증을 생략하지 않는다. SPLIT_LAYOUT의 semantic
        // content는 전부 별도 Plan node/edge로 표현되므로, envelope은 정확히 하나 존재하되 그
        // items는 항상 비어 있어야 한다.
        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        List<TargetLeafPayload> items = resolution.envelope.getItems();
        if (!items.isEmpty()) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION,
                    "unexpected_leaf_category:SPLIT_LAYOUT:" + items.get(0).getCategory());
        }

        Document doc = newTargetDocument();
        Element lybox = doc.createElementNS(NS_XF, "xf:group");
        lybox.setAttribute("class", "lybox");
        doc.appendChild(lybox);
        return AtomicRenderResult.rendered(node.getIdentity(), lybox);
    }

    /** 내부 fail-fast 신호 -- {@link #renderGridNode}가 던지고 스스로 잡아 notSupported로 변환한다. */
    private static final class GridStructuralViolation extends RuntimeException {
        GridStructuralViolation(String reason) {
            super(reason);
        }
    }

    /**
     * {@code GRID}의 required_structure만 렌더링한다: {@code gvwbox > wq_gvw > (header > row >
     * column) + (gBody > row > column)}. dataList/binding/label text는 evidence 미확정으로 렌더링하지 않는다.
     * variant는 predicate가 실제로 emit하는 {@code basic}만 지원한다.
     */
    private AtomicRenderResult renderGridNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"basic".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:GRID:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }
        List<TargetLeafPayload> items = resolution.envelope.getItems();

        try {
            int columnCount = requireGridPositiveIntParameter(node, "column_count");
            validateGridColumnBand(items, "head", columnCount);
            validateGridColumnBand(items, "body", columnCount);
            Element gvwbox = buildGridFragment(columnCount);
            return AtomicRenderResult.rendered(node.getIdentity(), gvwbox);
        } catch (GridStructuralViolation violation) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, violation.getMessage());
        }
    }

    /**
     * band(head/body)의 {@code GRID_COLUMN} leaf가 정확히 columnCount개이고 col이 0..N-1
     * dense, row는 항상 0, colSpan/rowSpan은 항상 1인지 검증한다. summ band는 검증 대상이
     * 아니다(out of scope).
     */
    /** 존재/Integer 타입/양수 검증 -- GRID 전용 {@link GridStructuralViolation}으로 던진다. */
    private int requireGridPositiveIntParameter(TargetCompositionNode node, String key) {
        Object value = node.getParameters().get(key);
        if (value == null) {
            throw new GridStructuralViolation("missing_parameter:" + key);
        }
        if (!(value instanceof Integer)) {
            throw new GridStructuralViolation(
                    "invalid_parameter_type:" + key + ":" + value.getClass().getSimpleName());
        }
        int intValue = ((Integer) value).intValue();
        if (intValue <= 0) {
            throw new GridStructuralViolation("invalid_parameter_value:" + key + ":" + intValue);
        }
        return intValue;
    }

    private void validateGridColumnBand(List<TargetLeafPayload> items, String band, int columnCount) {
        Set<Integer> seenCols = new LinkedHashSet<Integer>();
        int matchedCount = 0;
        for (TargetLeafPayload item : items) {
            if (item.getCategory() != TargetPayloadCategory.GRID_COLUMN) {
                continue;
            }
            Map<String, Object> data = item.getStructuredData();
            if (!band.equals(data.get("band"))) {
                continue;
            }
            matchedCount++;
            int col = requireGridInt(data, "col", band);
            int row = requireGridInt(data, "row", band);
            int colSpan = requireGridInt(data, "colSpan", band);
            int rowSpan = requireGridInt(data, "rowSpan", band);
            if (row != 0) {
                throw new GridStructuralViolation(
                        "unsupported_grid_row:band=" + band + ":row=" + row
                                + " (basic variant supports a single header/template row only)");
            }
            if (colSpan != 1 || rowSpan != 1) {
                throw new GridStructuralViolation(
                        "unsupported_grid_span:band=" + band + ":col=" + col + ":colSpan=" + colSpan
                                + ":rowSpan=" + rowSpan + " (merged cells are out of scope this wave)");
            }
            if (col < 0 || col >= columnCount) {
                throw new GridStructuralViolation(
                        "grid_col_out_of_declared_range:band=" + band + ":col=" + col);
            }
            if (!seenCols.add(Integer.valueOf(col))) {
                throw new GridStructuralViolation("duplicate_grid_col:band=" + band + ":col=" + col);
            }
        }
        if (matchedCount != columnCount) {
            throw new GridStructuralViolation(
                    "grid_column_count_mismatch:band=" + band + ":observed=" + matchedCount
                            + ":expected=" + columnCount);
        }
    }

    private int requireGridInt(Map<String, Object> data, String key, String band) {
        Object value = data.get(key);
        if (!(value instanceof Integer)) {
            throw new GridStructuralViolation(
                    "malformed_structured_data:band=" + band + ":" + key + ":"
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return ((Integer) value).intValue();
    }

    private Element buildGridFragment(int columnCount) {
        Document doc = newTargetDocument();
        Element gvwbox = doc.createElementNS(NS_XF, "xf:group");
        gvwbox.setAttribute("class", "gvwbox");

        Element gridView = doc.createElementNS(NS_W2, "w2:gridView");
        gridView.setAttribute("class", "wq_gvw");
        gvwbox.appendChild(gridView);

        Element header = doc.createElementNS(NS_W2, "w2:header");
        gridView.appendChild(header);
        Element headerRow = doc.createElementNS(NS_W2, "w2:row");
        header.appendChild(headerRow);
        for (int i = 0; i < columnCount; i++) {
            headerRow.appendChild(doc.createElementNS(NS_W2, "w2:column"));
        }

        Element gBody = doc.createElementNS(NS_W2, "w2:gBody");
        gridView.appendChild(gBody);
        Element bodyRow = doc.createElementNS(NS_W2, "w2:row");
        gBody.appendChild(bodyRow);
        for (int i = 0; i < columnCount; i++) {
            bodyRow.appendChild(doc.createElementNS(NS_W2, "w2:column"));
        }

        doc.appendChild(gvwbox);
        return gvwbox;
    }

    /** 내부 fail-fast 신호 -- {@link #renderTabControlNode}가 던지고 스스로 잡아 notSupported로 변환한다. */
    private static final class TabControlStructuralViolation extends RuntimeException {
        TabControlStructuralViolation(String reason) {
            super(reason);
        }
    }

    /**
     * {@code basic} variant만 지원한다. Target 구조: root {@code w2:tabControl} 아래 page 순서대로
     * {@code w2:tabs}(label) + {@code w2:content}(v1은 src 없음). page 개수의 유일한 권위는
     * {@code tab_count} 파라미터(leaf 개수로 역산하지 않음), label 권위는 pageOrdinal 붙은 DISPLAY_TEXT leaf다.
     */
    private AtomicRenderResult renderTabControlNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"basic".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:TAB_CONTROL:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        try {
            int tabCount = requireTabControlPositiveIntParameter(node, "tab_count");
            String[] labels = validateAndOrderTabLabels(resolution.envelope.getItems(), tabCount);
            TabControlFragment fragment = buildTabControlFragment(tabCount, labels);
            return AtomicRenderResult.renderedTabControl(
                    node.getIdentity(), fragment.root, fragment.pageContentAttachments);
        } catch (TabControlStructuralViolation violation) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, violation.getMessage());
        } catch (IllegalArgumentException malformedAttachments) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, malformedAttachments.getMessage());
        }
    }

    private int requireTabControlPositiveIntParameter(TargetCompositionNode node, String key) {
        Object value = node.getParameters().get(key);
        if (value == null) {
            throw new TabControlStructuralViolation("missing_parameter:" + key);
        }
        if (!(value instanceof Integer)) {
            throw new TabControlStructuralViolation(
                    "invalid_parameter_type:" + key + ":" + value.getClass().getSimpleName());
        }
        int intValue = ((Integer) value).intValue();
        if (intValue <= 0) {
            throw new TabControlStructuralViolation("invalid_parameter_value:" + key + ":" + intValue);
        }
        return intValue;
    }

    /**
     * tab_label leaf가 정확히 tabCount개, pageOrdinal이 0..tabCount-1 dense인지 검증하고
     * ordinal 순서로 정렬된 label 배열을 반환한다. label 값이 null/blank이면 즉시 실패한다
     * (빈 문자열/placeholder를 발명하지 않는다).
     */
    private String[] validateAndOrderTabLabels(List<TargetLeafPayload> items, int tabCount) {
        String[] labels = new String[tabCount];
        boolean[] seen = new boolean[tabCount];
        int matchedCount = 0;
        for (TargetLeafPayload item : items) {
            if (item.getCategory() != TargetPayloadCategory.DISPLAY_TEXT) {
                throw new TabControlStructuralViolation("unexpected_leaf_category:TAB_CONTROL:" + item.getCategory());
            }
            Object ordinalObj = item.getStructuredData().get("pageOrdinal");
            if (!(ordinalObj instanceof Integer)) {
                throw new TabControlStructuralViolation(
                        "malformed_structured_data:pageOrdinal:"
                                + (ordinalObj == null ? "null" : ordinalObj.getClass().getSimpleName()));
            }
            int ordinal = ((Integer) ordinalObj).intValue();
            if (ordinal < 0 || ordinal >= tabCount) {
                throw new TabControlStructuralViolation("page_ordinal_out_of_declared_range:" + ordinal);
            }
            if (seen[ordinal]) {
                throw new TabControlStructuralViolation("duplicate_page_ordinal:" + ordinal);
            }
            if (item.getValue() == null) {
                throw new TabControlStructuralViolation("tab_label_without_text_or_titletext:pageOrdinal=" + ordinal);
            }
            if (item.getValue().trim().length() == 0) {
                throw new TabControlStructuralViolation("tab_label_blank:pageOrdinal=" + ordinal);
            }
            seen[ordinal] = true;
            labels[ordinal] = item.getValue();
            matchedCount++;
        }
        if (matchedCount != tabCount) {
            throw new TabControlStructuralViolation(
                    "tab_label_count_mismatch:observed=" + matchedCount + ":expected=" + tabCount);
        }
        return labels;
    }

    private static final class TabControlFragment {
        final Element root;
        final Map<Integer, Element> pageContentAttachments;
        TabControlFragment(Element root, Map<Integer, Element> pageContentAttachments) {
            this.root = root;
            this.pageContentAttachments = pageContentAttachments;
        }
    }

    /**
     * root {@code w2:tabControl} 아래 page 순서대로 {@code w2:tabs}(label) + {@code w2:content}를
     * 만든다. v1은 src를 emit하지 않으며, page DOM id는 부여하지 않는다.
     */
    private TabControlFragment buildTabControlFragment(int tabCount, String[] labels) {
        Document doc = newTargetDocument();
        Element tabControl = doc.createElementNS(NS_W2, "w2:tabControl");
        doc.appendChild(tabControl);

        Map<Integer, Element> pageContentAttachments = new LinkedHashMap<Integer, Element>();
        for (int i = 0; i < tabCount; i++) {
            Element tabs = doc.createElementNS(NS_W2, "w2:tabs");
            tabs.setAttribute("label", labels[i]);
            tabControl.appendChild(tabs);

            Element content = doc.createElementNS(NS_W2, "w2:content");
            tabControl.appendChild(content);
            pageContentAttachments.put(Integer.valueOf(i), content);
        }
        return new TabControlFragment(tabControl, pageContentAttachments);
    }

    private Document newTargetDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new IllegalStateException("atomic_web_square_renderer: failed to create target document", e);
        }
    }

    /** 내부 fail-fast 신호 -- {@code renderBusinessTableNode}가 던지고 스스로 잡는다(이 node만의 검증 실패). */
    private static final class BusinessTableStructuralViolation extends RuntimeException {
        BusinessTableStructuralViolation(String reason) {
            super(reason);
        }
    }

    /**
     * BUSINESS_TABLE.horizontal의 structural shell만 렌더링한다(td_content/실제 control element는
     * 만들지 않는다). CONTROL_TYPE leaf는 row/cell/pair invariant 검증에만 쓰고 value는 읽지 않는다.
     */
    private AtomicRenderResult renderBusinessTableNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!BUSINESS_TABLE_VARIANT_HORIZONTAL.equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:BUSINESS_TABLE:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        try {
            List<TargetLeafPayload> items = resolution.envelope.getItems();
            int rowCount = requirePositiveIntParameter(node, "row_count");
            int columnPairCount = requirePositiveIntParameter(node, "column_pair_count");
            String[][] labelGrid = validateAndBuildLabelGrid(items, rowCount, columnPairCount);
            Element tbbox = buildBusinessTableFragment(rowCount, columnPairCount, labelGrid);
            return AtomicRenderResult.rendered(node.getIdentity(), tbbox);
        } catch (BusinessTableStructuralViolation violation) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, violation.getMessage());
        }
    }

    /** Plan parameter가 존재/Integer/양수인지 확인한다(상류 검증을 신뢰하지 않고 직접 방어). */
    private int requirePositiveIntParameter(TargetCompositionNode node, String key) {
        Object value = node.getParameters().get(key);
        if (value == null) {
            throw new BusinessTableStructuralViolation("missing_parameter:" + key);
        }
        if (!(value instanceof Integer)) {
            throw new BusinessTableStructuralViolation(
                    "invalid_parameter_type:" + key + ":" + value.getClass().getSimpleName());
        }
        int intValue = ((Integer) value).intValue();
        if (intValue <= 0) {
            throw new BusinessTableStructuralViolation("invalid_parameter_value:" + key + ":" + intValue);
        }
        return intValue;
    }

    /**
     * row/cell/pair integrity를 전부 검증하고 labelGrid[rowIndex][pairIndexInRow]=label text
     * 배열을 반환한다(payload 목록 순서 무관). category/cellIndex parity/dense range/label+control
     * 1개씩을 검증하며, CONTROL_TYPE.value(source tag name)는 읽지 않는다.
     */
    private String[][] validateAndBuildLabelGrid(
            List<TargetLeafPayload> items, int rowCount, int columnPairCount) {
        String[][] labelGrid = new String[rowCount][columnPairCount];
        boolean[][] hasLabel = new boolean[rowCount][columnPairCount];
        boolean[][] hasControl = new boolean[rowCount][columnPairCount];
        Set<Integer> observedRows = new LinkedHashSet<Integer>();

        for (TargetLeafPayload item : items) {
            TargetPayloadCategory category = item.getCategory();
            if (category != TargetPayloadCategory.DISPLAY_TEXT && category != TargetPayloadCategory.CONTROL_TYPE) {
                throw new BusinessTableStructuralViolation(
                        "unexpected_leaf_category:BUSINESS_TABLE:" + category);
            }
            int rowIndex = requireNonNegativeStructuredDataInt(item, "rowIndex");
            int cellIndexInRow = requireNonNegativeStructuredDataInt(item, "cellIndexInRow");
            int pairIndexInRow = requireNonNegativeStructuredDataInt(item, "pairIndexInRow");

            boolean isLabel = category == TargetPayloadCategory.DISPLAY_TEXT;
            int expectedCellIndex = isLabel ? pairIndexInRow * 2 : pairIndexInRow * 2 + 1;
            if (cellIndexInRow != expectedCellIndex) {
                throw new BusinessTableStructuralViolation(
                        "cell_index_mismatch:" + category + ":row=" + rowIndex
                                + ":pair=" + pairIndexInRow + ":cellIndex=" + cellIndexInRow);
            }
            if (rowIndex >= rowCount || pairIndexInRow >= columnPairCount) {
                throw new BusinessTableStructuralViolation(
                        "cell_out_of_declared_range:row=" + rowIndex + ":pair=" + pairIndexInRow);
            }
            observedRows.add(Integer.valueOf(rowIndex));

            if (isLabel) {
                if (hasLabel[rowIndex][pairIndexInRow]) {
                    throw new BusinessTableStructuralViolation(
                            "duplicate_display_text:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                if (item.getValue() == null) {
                    throw new BusinessTableStructuralViolation(
                            "display_text_value_missing:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                hasLabel[rowIndex][pairIndexInRow] = true;
                labelGrid[rowIndex][pairIndexInRow] = item.getValue();
            } else {
                if (hasControl[rowIndex][pairIndexInRow]) {
                    throw new BusinessTableStructuralViolation(
                            "duplicate_control_type:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                hasControl[rowIndex][pairIndexInRow] = true;
                // CONTROL_TYPE.value는 읽지 않는다 -- control-side 존재 evidence로만 쓴다.
            }
        }

        // rowIndex가 이미 0<=rowIndex<rowCount로 range-검증됐으므로 size()==rowCount이면
        // observedRows는 반드시 {0..rowCount-1} 전체와 같다(pigeonhole).
        if (observedRows.size() != rowCount) {
            throw new BusinessTableStructuralViolation(
                    "row_count_mismatch:observed=" + observedRows.size() + ":expected=" + rowCount);
        }
        for (int r = 0; r < rowCount; r++) {
            for (int p = 0; p < columnPairCount; p++) {
                if (!hasLabel[r][p]) {
                    throw new BusinessTableStructuralViolation("missing_display_text:row=" + r + ":pair=" + p);
                }
                if (!hasControl[r][p]) {
                    throw new BusinessTableStructuralViolation("missing_control_type:row=" + r + ":pair=" + p);
                }
            }
        }
        return labelGrid;
    }

    private int requireNonNegativeStructuredDataInt(TargetLeafPayload item, String key) {
        Object value = item.getStructuredData().get(key);
        if (!(value instanceof Integer)) {
            throw new BusinessTableStructuralViolation(
                    "malformed_structured_data:" + key + ":"
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        int intValue = ((Integer) value).intValue();
        if (intValue < 0) {
            throw new BusinessTableStructuralViolation("malformed_structured_data:" + key + ":negative:" + intValue);
        }
        return intValue;
    }

    /**
     * frozen required_structure를 그대로 따른다({@code xf:group + tagname} 표현 관례 재사용,
     * {@code WebSquareGenerator}는 호출하지 않는다). td는 항상 빈 shell, col_width는 계산하지 않는다.
     */
    private Element buildBusinessTableFragment(int rowCount, int columnPairCount, String[][] labelGrid) {
        Document doc = newTargetDocument();
        Element tbbox = doc.createElementNS(NS_XF, "xf:group");
        tbbox.setAttribute("class", "tbbox");

        Element table = doc.createElementNS(NS_XF, "xf:group");
        table.setAttribute("class", "w2tb tb");
        table.setAttribute("tagname", "table");
        tbbox.appendChild(table);

        Element attributes = doc.createElementNS(NS_W2, "w2:attributes");
        Element summary = doc.createElementNS(NS_W2, "w2:summary");
        attributes.appendChild(summary);
        table.appendChild(attributes);

        Element colgroup = doc.createElementNS(NS_XF, "xf:group");
        colgroup.setAttribute("tagname", "colgroup");
        table.appendChild(colgroup);
        int colCount = columnPairCount * 2;
        for (int i = 0; i < colCount; i++) {
            Element col = doc.createElementNS(NS_XF, "xf:group");
            col.setAttribute("tagname", "col");
            colgroup.appendChild(col);
        }

        for (int r = 0; r < rowCount; r++) {
            Element tr = doc.createElementNS(NS_XF, "xf:group");
            tr.setAttribute("tagname", "tr");
            table.appendChild(tr);
            for (int p = 0; p < columnPairCount; p++) {
                Element th = doc.createElementNS(NS_XF, "xf:group");
                th.setAttribute("class", "w2tb_th");
                th.setAttribute("tagname", "th");
                // 일반 setAttribute("w2:scope",...)는 prefix가 undeclared로 남아 Transformer 직렬화 시
                // 거부된다 -- setAttributeNS로 NS_W2에 바인딩한다(속성 값/계약은 동일).
                th.setAttributeNS(NS_W2, "w2:scope", "row");
                th.setTextContent(labelGrid[r][p]);
                tr.appendChild(th);

                Element td = doc.createElementNS(NS_XF, "xf:group");
                td.setAttribute("class", "w2tb_td");
                td.setAttribute("tagname", "td");
                tr.appendChild(td);
            }
        }

        doc.appendChild(tbbox);
        return tbbox;
    }

    // ==== SEARCH_AREA 렌더링 ====

    /**
     * {@code SEARCH_AREA.basic}만 지원한다(predicate가 실제로 emit하는 유일한 variant). BUSINESS_TABLE과
     * leaf shape은 같지만 target mapping은 다르다 -- row별 독립적인 dense pair 범위만 요구하고
     * rectangular column_pair_count는 강제하지 않는다.
     */
    private AtomicRenderResult renderSearchAreaNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"basic".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:SEARCH_AREA:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        try {
            List<TargetLeafPayload> items = resolution.envelope.getItems();
            int rowCount = requireSearchAreaPositiveIntParameter(node, "row_count");
            Map<Integer, Map<Integer, SearchAreaPair>> structure =
                    validateAndBuildSearchAreaStructure(items, rowCount);
            Element root = buildSearchAreaFragment(structure);
            return AtomicRenderResult.rendered(node.getIdentity(), root);
        } catch (SearchAreaStructuralViolation violation) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, violation.getMessage());
        }
    }

    private int requireSearchAreaPositiveIntParameter(TargetCompositionNode node, String key) {
        Object value = node.getParameters().get(key);
        if (value == null) {
            throw new SearchAreaStructuralViolation("missing_parameter:" + key);
        }
        if (!(value instanceof Integer)) {
            throw new SearchAreaStructuralViolation(
                    "invalid_parameter_type:" + key + ":" + value.getClass().getSimpleName());
        }
        int intValue = ((Integer) value).intValue();
        if (intValue <= 0) {
            throw new SearchAreaStructuralViolation("invalid_parameter_value:" + key + ":" + intValue);
        }
        return intValue;
    }

    private int requireSearchAreaNonNegativeStructuredDataInt(TargetLeafPayload item, String key) {
        Object value = item.getStructuredData().get(key);
        if (!(value instanceof Integer)) {
            throw new SearchAreaStructuralViolation(
                    "malformed_structured_data:" + key + ":"
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        int intValue = ((Integer) value).intValue();
        if (intValue < 0) {
            throw new SearchAreaStructuralViolation("malformed_structured_data:" + key + ":negative:" + intValue);
        }
        return intValue;
    }

    private static final class SearchAreaPair {
        String labelText;
        String controlSourceTagName;
    }

    /**
     * row order는 {@link TreeMap}(both levels)이 Integer 키로 구조적으로 보장하므로 input list 순서는 무관하다.
     * row별 독립적인 dense pair 범위만 요구한다(BUSINESS_TABLE처럼 균일 column_count 강제 없음).
     */
    private Map<Integer, Map<Integer, SearchAreaPair>> validateAndBuildSearchAreaStructure(
            List<TargetLeafPayload> items, int rowCount) {
        Map<Integer, Map<Integer, SearchAreaPair>> byRow = new TreeMap<Integer, Map<Integer, SearchAreaPair>>();
        Set<Integer> observedRows = new LinkedHashSet<Integer>();

        for (TargetLeafPayload item : items) {
            TargetPayloadCategory category = item.getCategory();
            if (category != TargetPayloadCategory.DISPLAY_TEXT && category != TargetPayloadCategory.CONTROL_TYPE) {
                throw new SearchAreaStructuralViolation("unexpected_leaf_category:SEARCH_AREA:" + category);
            }
            int rowIndex = requireSearchAreaNonNegativeStructuredDataInt(item, "rowIndex");
            int cellIndexInRow = requireSearchAreaNonNegativeStructuredDataInt(item, "cellIndexInRow");
            int pairIndexInRow = requireSearchAreaNonNegativeStructuredDataInt(item, "pairIndexInRow");

            boolean isLabel = category == TargetPayloadCategory.DISPLAY_TEXT;
            int expectedCellIndex = isLabel ? pairIndexInRow * 2 : pairIndexInRow * 2 + 1;
            if (cellIndexInRow != expectedCellIndex) {
                throw new SearchAreaStructuralViolation(
                        "cell_index_mismatch:" + category + ":row=" + rowIndex + ":pair=" + pairIndexInRow
                                + ":cellIndex=" + cellIndexInRow);
            }
            if (rowIndex >= rowCount) {
                throw new SearchAreaStructuralViolation("row_out_of_declared_range:row=" + rowIndex);
            }
            observedRows.add(Integer.valueOf(rowIndex));

            Map<Integer, SearchAreaPair> row = byRow.get(Integer.valueOf(rowIndex));
            if (row == null) {
                row = new TreeMap<Integer, SearchAreaPair>();
                byRow.put(Integer.valueOf(rowIndex), row);
            }
            SearchAreaPair pair = row.get(Integer.valueOf(pairIndexInRow));
            if (pair == null) {
                pair = new SearchAreaPair();
                row.put(Integer.valueOf(pairIndexInRow), pair);
            }

            if (isLabel) {
                if (pair.labelText != null) {
                    throw new SearchAreaStructuralViolation(
                            "duplicate_display_text:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                if (item.getValue() == null) {
                    throw new SearchAreaStructuralViolation(
                            "display_text_value_missing:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                pair.labelText = item.getValue();
            } else {
                if (pair.controlSourceTagName != null) {
                    throw new SearchAreaStructuralViolation(
                            "duplicate_control_type:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                if (item.getValue() == null) {
                    throw new SearchAreaStructuralViolation(
                            "control_type_value_missing:row=" + rowIndex + ":pair=" + pairIndexInRow);
                }
                pair.controlSourceTagName = item.getValue();
            }
        }

        if (observedRows.size() != rowCount) {
            throw new SearchAreaStructuralViolation(
                    "row_count_mismatch:observed=" + observedRows.size() + ":expected=" + rowCount);
        }
        for (Map.Entry<Integer, Map<Integer, SearchAreaPair>> rowEntry : byRow.entrySet()) {
            Map<Integer, SearchAreaPair> row = rowEntry.getValue();
            int expectedPairCountInRow = row.size();
            for (int p = 0; p < expectedPairCountInRow; p++) {
                SearchAreaPair pair = row.get(Integer.valueOf(p));
                if (pair == null) {
                    throw new SearchAreaStructuralViolation(
                            "pair_index_not_dense:row=" + rowEntry.getKey() + ":missing_pair=" + p);
                }
                if (pair.labelText == null) {
                    throw new SearchAreaStructuralViolation(
                            "missing_display_text:row=" + rowEntry.getKey() + ":pair=" + p);
                }
                if (pair.controlSourceTagName == null) {
                    throw new SearchAreaStructuralViolation(
                            "missing_control_type:row=" + rowEntry.getKey() + ":pair=" + p);
                }
            }
        }
        return byRow;
    }

    /**
     * frozen v1 target DOM: root {@code xf:group}, 행마다 자식 {@code xf:group}, pair마다
     * label + control(label이 먼저). BUSINESS_TABLE 구조/source CSS class 재구성 없음.
     */
    private Element buildSearchAreaFragment(Map<Integer, Map<Integer, SearchAreaPair>> structure) {
        Document doc = newTargetDocument();
        Element root = doc.createElementNS(NS_XF, "xf:group");
        doc.appendChild(root);

        for (Map.Entry<Integer, Map<Integer, SearchAreaPair>> rowEntry : structure.entrySet()) {
            Element rowGroup = doc.createElementNS(NS_XF, "xf:group");
            root.appendChild(rowGroup);
            for (Map.Entry<Integer, SearchAreaPair> pairEntry : rowEntry.getValue().entrySet()) {
                SearchAreaPair pair = pairEntry.getValue();
                Element label = doc.createElementNS(NS_W2, "w2:textbox");
                label.setTextContent(pair.labelText);
                rowGroup.appendChild(label);
                rowGroup.appendChild(buildSearchAreaControlElement(doc, pair.controlSourceTagName));
            }
        }
        return root;
    }

    /**
     * source tag -> target control 1:1 매핑(Edit/Combo/Calendar/CheckBox/Radio 다섯 값만).
     * 다른 값은 lexical 유사성으로 재해석하지 않고 즉시 fail-closed한다(generic fallback 없음).
     */
    private Element buildSearchAreaControlElement(Document doc, String sourceTagName) {
        if ("Edit".equals(sourceTagName)) {
            return doc.createElementNS(NS_XF, "xf:input");
        } else if ("Combo".equals(sourceTagName)) {
            Element select1 = doc.createElementNS(NS_XF, "xf:select1");
            select1.setAttribute("appearance", "minimal");
            return select1;
        } else if ("Calendar".equals(sourceTagName)) {
            return doc.createElementNS(NS_W2, "w2:inputCalendar");
        } else if ("CheckBox".equals(sourceTagName)) {
            Element select = doc.createElementNS(NS_XF, "xf:select");
            select.setAttribute("appearance", "full");
            return select;
        } else if ("Radio".equals(sourceTagName)) {
            Element select1 = doc.createElementNS(NS_XF, "xf:select1");
            select1.setAttribute("appearance", "full");
            return select1;
        }
        throw new SearchAreaStructuralViolation("unsupported_control_type:" + sourceTagName);
    }

    /** 내부 fail-fast 신호 -- {@code renderSearchAreaNode}가 던지고 스스로 잡는다. */
    private static final class SearchAreaStructuralViolation extends RuntimeException {
        SearchAreaStructuralViolation(String reason) {
            super(reason);
        }
    }

    // ==== BUTTON_GROUP 렌더링 ====

    /**
     * title_bar_attached/standalone 두 variant는 동일한 v1 atomic DOM을 만든다(variant는 provenance 정보일 뿐).
     * source position/geometry는 읽지도 투영하지도 않는다. root {@code xf:group} 아래 각 button leaf가
     * buttonOrdinal 오름차순으로 {@code xf:trigger} + {@code xf:label}이 된다.
     */
    private AtomicRenderResult renderButtonGroupNode(
            TargetCompositionNode node, Map<String, List<TargetNodePayload>> payloadsByNodeId) {
        if (!"title_bar_attached".equals(node.getVariant()) && !"standalone".equals(node.getVariant())) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.UNSUPPORTED_VARIANT,
                    "unsupported_variant:BUTTON_GROUP:" + node.getVariant());
        }

        PayloadResolution resolution = resolveExactlyOneCorrelatedPayload(node, payloadsByNodeId);
        if (resolution.failureReason != null) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, resolution.failureReason);
        }

        try {
            TargetNodePayload envelope = resolution.envelope;
            Integer expectedCount = envelope.getExpectedStructuralMemberCount();
            if (expectedCount == null) {
                throw new ButtonGroupStructuralViolation("missing_expected_structural_member_count");
            }
            if (expectedCount.intValue() < 0) {
                throw new ButtonGroupStructuralViolation(
                        "negative_expected_structural_member_count:" + expectedCount);
            }
            int expected = expectedCount.intValue();

            Map<Integer, TargetLeafPayload> buttonByOrdinal = validateAndCollectButtonLeaves(envelope, expected);
            Map<Integer, Element> triggerByOrdinal = new TreeMap<Integer, Element>();
            Document doc = newTargetDocument();
            Element root = doc.createElementNS(NS_XF, "xf:group");
            doc.appendChild(root);
            for (Map.Entry<Integer, TargetLeafPayload> entry : buttonByOrdinal.entrySet()) {
                String presentation = entry.getValue().getValue();
                if (presentation == null || presentation.trim().length() == 0) {
                    throw new ButtonGroupStructuralViolation(
                            "button_without_usable_presentation_value:buttonOrdinal=" + entry.getKey());
                }
                Element trigger = doc.createElementNS(NS_XF, "xf:trigger");
                trigger.setAttribute("type", "button");
                Element label = doc.createElementNS(NS_XF, "xf:label");
                label.setTextContent(presentation);
                trigger.appendChild(label);
                root.appendChild(trigger);
                triggerByOrdinal.put(entry.getKey(), trigger);
            }

            attachFinalizedButtonGroupEvents(envelope, expected, triggerByOrdinal);
            return AtomicRenderResult.rendered(node.getIdentity(), root);
        } catch (ButtonGroupStructuralViolation violation) {
            return AtomicRenderResult.notSupported(
                    node.getIdentity(), RenderStatus.INTEGRITY_VIOLATION, violation.getMessage());
        }
    }

    /**
     * expectedStructuralMemberCount(독립 권위, 재도출 없음)를 기준으로 button 구조 leaf 집합을
     * 검증한다: buttonOrdinal이 {0..expected-1} dense set이어야 하며 leaf 개수도 정확히 일치해야 한다.
     */
    private Map<Integer, TargetLeafPayload> validateAndCollectButtonLeaves(TargetNodePayload envelope, int expected) {
        Map<Integer, TargetLeafPayload> byOrdinal = new TreeMap<Integer, TargetLeafPayload>();
        int structuralLeafCount = 0;
        for (TargetLeafPayload item : envelope.getItems()) {
            if (item.getCategory() != TargetPayloadCategory.DISPLAY_TEXT) {
                continue;
            }
            structuralLeafCount++;
            Object ordinalObj = item.getStructuredData().get("buttonOrdinal");
            if (!(ordinalObj instanceof Integer)) {
                throw new ButtonGroupStructuralViolation(
                        "malformed_structured_data:buttonOrdinal:"
                                + (ordinalObj == null ? "null" : ordinalObj.getClass().getSimpleName()));
            }
            int ordinal = ((Integer) ordinalObj).intValue();
            if (ordinal < 0 || ordinal >= expected) {
                throw new ButtonGroupStructuralViolation("button_ordinal_out_of_declared_range:" + ordinal);
            }
            if (byOrdinal.containsKey(Integer.valueOf(ordinal))) {
                throw new ButtonGroupStructuralViolation("duplicate_button_ordinal:" + ordinal);
            }
            byOrdinal.put(Integer.valueOf(ordinal), item);
        }
        if (structuralLeafCount != expected) {
            throw new ButtonGroupStructuralViolation(
                    "structural_button_leaf_count_mismatch:observed=" + structuralLeafCount + ":expected=" + expected);
        }
        for (int i = 0; i < expected; i++) {
            if (!byOrdinal.containsKey(Integer.valueOf(i))) {
                throw new ButtonGroupStructuralViolation("missing_button_ordinal:" + i);
            }
        }
        return byOrdinal;
    }

    /**
     * EVENT leaf를 순회하되 오직 {@link TargetLeafPayload#getFinalizedTargetEventBinding()}만
     * 읽는다 -- raw source 필드는 전혀 사용하지 않는다. 각 finalized binding은 정확히 하나의
     * 기존 trigger에만 attach된다(잘못된 ordinal은 맵 조회 실패로 즉시 걸러진다).
     */
    private void attachFinalizedButtonGroupEvents(
            TargetNodePayload envelope, int expected, Map<Integer, Element> triggerByOrdinal) {
        Set<String> seenKeys = new LinkedHashSet<String>();
        for (TargetLeafPayload item : envelope.getItems()) {
            if (item.getCategory() != TargetPayloadCategory.EVENT) {
                continue;
            }
            TargetEventBinding binding = item.getFinalizedTargetEventBinding();
            if (binding == null) {
                throw new ButtonGroupStructuralViolation("event_leaf_without_finalized_target_event_binding");
            }
            int ordinal = binding.getButtonOrdinal();
            if (ordinal < 0 || ordinal >= expected || !triggerByOrdinal.containsKey(Integer.valueOf(ordinal))) {
                throw new ButtonGroupStructuralViolation(
                        "finalized_event_binding_references_nonexistent_button_ordinal:" + ordinal);
            }
            String targetEventLocalName = binding.getTargetEventLocalName();
            if (!"onclick".equals(targetEventLocalName)) {
                throw new ButtonGroupStructuralViolation(
                        "unsupported_finalized_target_event_local_name:" + targetEventLocalName);
            }
            String key = ordinal + ":" + targetEventLocalName;
            if (!seenKeys.add(key)) {
                throw new ButtonGroupStructuralViolation("duplicate_finalized_event_binding:" + key);
            }
            // target event attribute는 namespace-aware ev:onclick(NS_EV)으로만 emit한다(plain onclick과
            // 병행 emit 없음). 호출 값은 Reviewer 승인된 "scwin.<identifier>(...)" 규약을 쓰며,
            // finalized target identifier만 읽는다(raw functionName 읽지 않음).
            Element trigger = triggerByOrdinal.get(Integer.valueOf(ordinal));
            trigger.setAttributeNS(NS_EV, "ev:" + targetEventLocalName,
                    "scwin." + binding.getTargetFunctionIdentifier() + "();");
        }
    }

    /** 내부 fail-fast 신호 -- {@code renderButtonGroupNode}가 던지고 스스로 잡는다. */
    private static final class ButtonGroupStructuralViolation extends RuntimeException {
        ButtonGroupStructuralViolation(String reason) {
            super(reason);
        }
    }
}
