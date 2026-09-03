package com.example.xfdltracker.payload;

import com.example.xfdltracker.analyzer.SourcePredicateVariantContract;
import com.example.xfdltracker.binding.SourceBindingReference;
import com.example.xfdltracker.composition.CompositionDecision;
import com.example.xfdltracker.composition.TargetCompositionNode;
import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.converter.GridFormatParser;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link TargetCompositionPlan}의 각 {@code SOURCE_SEMANTIC} node를 읽기 전용으로 순회해
 * {@link TargetNodePayload}로 투영한다. evidence-backed family는 {@link SourcePayloadEvidenceItem}만
 * 읽으며(GRID만 anchor 예외), source DOM은 provenance 검증용으로만 walk한다.
 */
public final class TargetPayloadExtractor {

    /** family별로 허용되는 evidence role -- 다른 family의 evidence가 같은 structuralId에 섞여도
     * target payload로 승격되지 않도록 family마다 자기 role만 허용한다. */
    private static final Map<String, Set<String>> ALLOWED_ROLES_BY_FAMILY;
    static {
        Map<String, Set<String>> m = new LinkedHashMap<String, Set<String>>();
        m.put("SEARCH_AREA", unmodifiableSet("label", "control"));
        m.put("TITLE_BAR", unmodifiableSet("title_label"));
        m.put("BUTTON_GROUP", unmodifiableSet("button", "event"));
        m.put("TAB_CONTROL", unmodifiableSet("tab_label"));
        // BUSINESS_TABLE은 SEARCH_AREA와 같은 producer를 공유하므로 동일한 label/control
        // evidence role을 쓴다 -- 새 evidence 종류를 만들지 않는다.
        m.put("BUSINESS_TABLE", unmodifiableSet("label", "control"));
        ALLOWED_ROLES_BY_FAMILY = Collections.unmodifiableMap(m);
    }

    /**
     * family+role 조합마다 producer가 구조적으로 보장하는 실제 source tag만 인정한다 -- 이미
     * 확정된 predicate invariant를 evidence 소비 시점에 재확인하는 것뿐이다(wrapper Div 등
     * 엉뚱한 Element가 사칭하면 즉시 실패). SEARCH_AREA/BUSINESS_TABLE은 label/control 어휘를 공유한다.
     */
    private static final Set<String> SHARED_LABEL_CONTROL_TAGS =
            unmodifiableSet("Edit", "Combo", "Calendar", "CheckBox", "Radio");

    private static Set<String> unmodifiableSet(String... values) {
        Set<String> s = new LinkedHashSet<String>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    private final GridFormatParser gridFormatParser = new GridFormatParser();

    /** binding evidence는 반드시 상위 orchestration(예: {@code TargetWebSquarePipeline})이 미리
     *  계산해 넘겨야 한다 -- 이 클래스는 어떤 binding analyzer도 스스로 호출하지 않는다. */
    public List<TargetNodePayload> extract(
            Element sourceRoot, TargetCompositionPlan plan, List<SemanticRegionResult> regions,
            List<SourceBindingReference> bindingReferences) {
        // sourceRoot는 optional escape hatch가 아니다 -- provenance validation을 건너뛰는 경로를 없앤다.
        if (sourceRoot == null) {
            throw new IllegalArgumentException(
                    "target_payload_extractor: sourceRoot must not be null -- source component "
                            + "provenance validation cannot be skipped");
        }
        // Slice 99C correction -- binding 선언 evidence는 upstream(SourceBindingAnalyzer)에서 이미
        // 정확히 한 번 resolve된 채로 들어온다. 여기서는 raw source를 다시 스캔하지 않는다.
        List<SourceBindingReference> bindings = bindingReferences == null
                ? Collections.<SourceBindingReference>emptyList() : bindingReferences;

        Map<String, List<Element>> elementsByStructuralId = new LinkedHashMap<String, List<Element>>();
        indexStructuralIdentities(sourceRoot, elementsByStructuralId);

        // 여러 region이 같은 sourceStructuralId를 가질 수 있으므로(stale/tampered 입력 방어) last-wins
        // Map이 아니라 List로 모은다 -- extractFromEvidence가 목록 크기로 duplicate를 거부한다.
        Map<String, List<SemanticRegionResult>> regionsByStructuralId = new LinkedHashMap<String, List<SemanticRegionResult>>();
        if (regions != null) {
            for (SemanticRegionResult region : regions) {
                if (region.getSourceStructuralId() != null && region.getSourceStructuralId().length() > 0) {
                    List<SemanticRegionResult> list = regionsByStructuralId.get(region.getSourceStructuralId());
                    if (list == null) {
                        list = new ArrayList<SemanticRegionResult>();
                        regionsByStructuralId.put(region.getSourceStructuralId(), list);
                    }
                    list.add(region);
                }
            }
        }

        List<TargetNodePayload> result = new ArrayList<TargetNodePayload>();
        for (TargetCompositionNode node : plan.getNodes()) {
            if (node.getOrigin() != CompositionDecision.Origin.SOURCE_SEMANTIC) {
                // TARGET_SYNTHETIC(예: PAGING) payload는 발명하지 않는다 -- source region
                // evidence를 절대 사용하지 않는다.
                continue;
            }
            if (ALLOWED_ROLES_BY_FAMILY.containsKey(node.getFamily())) {
                // extractFromEvidence는 검증 실패 시 항상 예외를 던진다(silent skip 없음). items가
                // 비어 있어도(예: 0..1 cardinality) envelope 자체는 항상 만든다 -- "검증된 leaf
                // 0개"와 "envelope 자체 없음"을 renderer가 구분할 수 있어야 한다.
                List<TargetLeafPayload> items = extractFromEvidence(
                        node, node.getFamily(), regionsByStructuralId, elementsByStructuralId, bindings);
                // 모든 evidence-backed family envelope은 identityKind를 함께 싣는다(node.getOrigin()에서
                // 직접 유도, 문자열 parsing 없음).
                Integer expectedStructuralMemberCount = "BUTTON_GROUP".equals(node.getFamily())
                        ? readButtonGroupExpectedCount(node, regionsByStructuralId)
                        : null;
                result.add(new TargetNodePayload(
                        node.getIdentityKind(), node.getNodeId(), items, expectedStructuralMemberCount));
            } else {
                // frozen renderer integrity 계약상 모든 supported Plan node는 정확히 하나의
                // TargetNodePayload와 correlation돼야 한다(items가 비어도 envelope은 항상 만든다) --
                // "semantic value 없음"과 "envelope 자체 없음"은 다른 사실이다.
                Element anchor = resolveUniqueElement(elementsByStructuralId, node.getSourceStructuralId());
                if (anchor == null) {
                    throw new IllegalStateException(
                            "target_payload_extractor: no unique source anchor Element found for "
                                    + "sourceStructuralId=" + node.getSourceStructuralId() + " (family="
                                    + node.getFamily() + ") -- a Plan node's source anchor must resolve to "
                                    + "exactly one Element; refusing to silently omit its Payload envelope");
                }
                List<TargetLeafPayload> items = extractForFamily(node.getFamily(), anchor);
                // SPLIT_LAYOUT/GRID 둘 다 이 anchor 기반 분기를 공유하며 identityKind를 명시적으로 싣는다.
                result.add(new TargetNodePayload(node.getIdentityKind(), node.getNodeId(), items));
            }
        }
        return result;
    }

    /**
     * {@code SemanticRegionResult.buttonGroupExpectedButtonCount}를 그대로 복사한다(재검증 없이
     * extractFromEvidence가 이미 확인한 동일 region을 재조회). 값이 null이거나 음수이면 fallback
     * 없이 즉시 fail-closed한다 -- leaf 개수 등 어떤 대체 수단으로도 복구/추정하지 않는다.
     */
    private Integer readButtonGroupExpectedCount(
            TargetCompositionNode node, Map<String, List<SemanticRegionResult>> regionsByStructuralId) {
        List<SemanticRegionResult> candidates = regionsByStructuralId.get(node.getSourceStructuralId());
        if (candidates == null || candidates.size() != 1) {
            throw new IllegalStateException(
                    "target_payload_extractor: BUTTON_GROUP expected-button-count lookup found "
                            + (candidates == null ? 0 : candidates.size()) + " region(s) for sourceStructuralId="
                            + node.getSourceStructuralId() + " (expected exactly 1, already validated upstream)");
        }
        Integer expectedCount = candidates.get(0).getButtonGroupExpectedButtonCount();
        if (expectedCount == null) {
            throw new IllegalStateException(
                    "target_payload_extractor: BUTTON_GROUP node sourceStructuralId=" + node.getSourceStructuralId()
                            + " has no buttonGroupExpectedButtonCount on its SemanticRegionResult -- refusing to "
                            + "fall back to actual leaf count or any other derived value");
        }
        if (expectedCount.intValue() < 0) {
            throw new IllegalStateException(
                    "target_payload_extractor: BUTTON_GROUP node sourceStructuralId=" + node.getSourceStructuralId()
                            + " has negative buttonGroupExpectedButtonCount=" + expectedCount);
        }
        return expectedCount;
    }

    /** GRID만 예외로 anchor 기반을 유지한다({@link GridFormatParser} 재사용은 leaf 재도출이 아니다).
     * 나머지 non-evidence-backed family는 추측 구현하지 않고 빈 목록을 반환한다. */
    private List<TargetLeafPayload> extractForFamily(String family, Element anchor) {
        if ("GRID".equals(family)) {
            return extractGrid(anchor);
        }
        return new ArrayList<TargetLeafPayload>();
    }

    /**
     * evidence-backed family 공통 추출 -- family마다 검증 로직을 복붙하지 않고 이 메서드가 공유한다.
     * fail-closed 계약: region 없음/중복/family 불일치, evidence ownership 불일치, role/kind/value
     * 불일치, sourceOrder 음수/중복, provenance 위반은 모두 즉시 실패한다.
     */
    private List<TargetLeafPayload> extractFromEvidence(
            TargetCompositionNode node,
            String family,
            Map<String, List<SemanticRegionResult>> regionsByStructuralId,
            Map<String, List<Element>> elementsByStructuralId,
            List<SourceBindingReference> bindingReferences) {
        String structuralId = node.getSourceStructuralId();
        List<SemanticRegionResult> candidates = regionsByStructuralId.get(structuralId);
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "target_payload_extractor: no SemanticRegionResult found for " + family
                            + " sourceStructuralId=" + structuralId + " -- this family is evidence-backed "
                            + "and cannot produce payload without a matching region");
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException(
                    "target_payload_extractor: duplicate SemanticRegionResult -- sourceStructuralId="
                            + structuralId + " has " + candidates.size() + " region entries; refusing to "
                            + "silently select one (silent duplicate-region selection is forbidden)");
        }
        SemanticRegionResult region = candidates.get(0);
        if (!family.equals(region.getSemanticType())) {
            throw new IllegalStateException(
                    "target_payload_extractor: region at sourceStructuralId=" + structuralId
                            + " has semanticType=" + region.getSemanticType() + ", expected " + family
                            + " (family mismatch between Plan node and correlated region)");
        }
        // family가 일치해도 variant가 다르면 같은 anchor에 모순된 semantic state가 공존하는 것이다.
        // 정상 pipeline에서는 항상 일치하며, 불일치는 stale/tampered regions일 때만 발생한다.
        // null은 wildcard가 아니다(둘 다 null일 때만 "같음").
        String planVariant = node.getVariant();
        String regionVariant = region.getRecommendedVariant();
        boolean variantMatches = planVariant == null ? regionVariant == null : planVariant.equals(regionVariant);
        if (!variantMatches) {
            throw new IllegalStateException(
                    "target_payload_extractor: variant mismatch between Plan node and correlated region -- "
                            + "family=" + family + " sourceStructuralId=" + structuralId + " planNode.variant=\""
                            + planVariant + "\" region.recommendedVariant=\"" + regionVariant + "\"");
        }
        // 값이 catalog-valid이지만 source predicate가 실제로 만든 적 없는 상태(source-unemittable)라면
        // "일치"만으로 통과시키지 않는다. 판단은 {@link SourcePredicateVariantContract}에 위임하며,
        // target-side catalog validity는 건드리지 않는다.
        if (!SourcePredicateVariantContract.isSourceEmittable(family, planVariant)) {
            throw new IllegalStateException(
                    "target_payload_extractor: variant=\"" + planVariant + "\" for family=" + family
                            + " sourceStructuralId=" + structuralId + " is catalog-valid but not "
                            + "source-emittable by the current predicate -- refusing to project source "
                            + "evidence for a semantic state the source predicate never actually produces");
        }
        List<SourcePayloadEvidenceItem> evidence = region.getPayloadEvidence();
        if (evidence.isEmpty()) {
            return new ArrayList<TargetLeafPayload>();
        }

        Element regionAnchor = resolveElementOrFail(
                elementsByStructuralId, region.getSourceStructuralId(), "region anchor");

        Set<String> allowedRoles = ALLOWED_ROLES_BY_FAMILY.get(family);
        Set<Integer> seenOrders = new java.util.HashSet<Integer>();
        for (SourcePayloadEvidenceItem item : evidence) {
            if (!structuralId.equals(item.getSemanticRegionStructuralId())) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidence ownership mismatch -- region sourceStructuralId="
                                + structuralId + " but evidence.semanticRegionStructuralId="
                                + item.getSemanticRegionStructuralId());
            }
            if (!allowedRoles.contains(item.getEvidenceRole())) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidenceRole=\"" + item.getEvidenceRole() + "\" is not "
                                + "valid for family=" + family + " (allowed roles=" + allowedRoles + ")");
            }
            validateEvidenceShape(item);
            if (!seenOrders.add(Integer.valueOf(item.getSourceOrder()))) {
                throw new IllegalStateException(
                        "target_payload_extractor: duplicate sourceOrder=" + item.getSourceOrder()
                                + " within region sourceStructuralId=" + structuralId);
            }
            Element leaf = resolveElementOrFail(
                    elementsByStructuralId, item.getSourceComponentStructuralId(), "evidence component");
            if (!isDescendant(leaf, regionAnchor)) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidence component outside region subtree -- "
                                + "sourceComponentStructuralId=" + item.getSourceComponentStructuralId()
                                + " is not a descendant of region anchor " + region.getSourceStructuralId());
            }
            validateSourceElementRoleContract(family, item.getEvidenceRole(), leaf);
            rejectUnprovenCheckBoxContract(leaf, item.getSourceComponentStructuralId(), bindingReferences);
        }
        validateBusinessTableStructuralIntegrity(family, evidence);

        List<SourcePayloadEvidenceItem> ordered = new ArrayList<SourcePayloadEvidenceItem>(evidence);
        Collections.sort(ordered, new Comparator<SourcePayloadEvidenceItem>() {
            public int compare(SourcePayloadEvidenceItem a, SourcePayloadEvidenceItem b) {
                return Integer.compare(a.getSourceOrder(), b.getSourceOrder());
            }
        });
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        for (SourcePayloadEvidenceItem item : ordered) {
            // role="button"은 "value==null이면 leaf 없음" 규칙의 예외다 -- 텍스트/값이 둘 다 없는
            // Button도 evidence item은 항상 존재하므로, 여기서 버리면 Button의 구조적 존재 자체가
            // 소실된다(다운스트림 TargetPayloadBehaviorFinalizer가 이 leaf 존재를 요구).
            boolean isButtonRole = "BUTTON_GROUP".equals(family) && "button".equals(item.getEvidenceRole());
            // TAB_CONTROL.tab_label도 동일한 결함 계열: 라벨이 없어도 evidence item은 항상 나오므로
            // 여기서 드롭하면 페이지 개수/순서가 leaf만으로는 신뢰할 수 없게 된다.
            boolean isTabLabelRole = "TAB_CONTROL".equals(family) && "tab_label".equals(item.getEvidenceRole());
            // SEARCH_AREA.label도 동일한 결함 계열: text/value가 둘 다 없어도 evidence item은 항상
            // 나오며, 여기서 드롭하면 짝인 control evidence가 고아가 된다.
            boolean isSearchAreaLabelRole = "SEARCH_AREA".equals(family) && "label".equals(item.getEvidenceRole());
            if (item.getValue() == null && !isButtonRole && !isTabLabelRole && !isSearchAreaLabelRole) {
                continue;
            }
            if (isButtonRole) {
                Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
                // buttonOrdinal = producer가 부여한 flattenedButtons index(sourceOrder) 그대로 -- 재계산 금지.
                structuredData.put("buttonOrdinal", Integer.valueOf(item.getSourceOrder()));
                items.add(new TargetLeafPayload(
                        TargetPayloadCategory.DISPLAY_TEXT, item.getValue(), structuredData,
                        item.getEvidenceKind(), item.getSourceComponentStructuralId()));
                continue;
            }
            if (isTabLabelRole) {
                // pageOrdinal = producer의 sourceOrder 그대로(BUTTON_GROUP.buttonOrdinal과 동일 패턴,
                // 이 좁은 범위에 한해 Reviewer 승인됨) -- leaf 위치/라벨 텍스트 등에서 재계산하지 않는다.
                Map<String, Object> structuredData = new LinkedHashMap<String, Object>();
                structuredData.put("pageOrdinal", Integer.valueOf(item.getSourceOrder()));
                items.add(new TargetLeafPayload(
                        TargetPayloadCategory.DISPLAY_TEXT, item.getValue(), structuredData,
                        item.getEvidenceKind(), item.getSourceComponentStructuralId()));
                continue;
            }
            if ("event".equals(item.getEvidenceRole())) {
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("eventName", item.getValue());
                data.put("functionName", item.getFunctionName());
                items.add(new TargetLeafPayload(
                        TargetPayloadCategory.EVENT, item.getValue(), data, item.getEvidenceKind(),
                        item.getSourceComponentStructuralId()));
            } else {
                TargetPayloadCategory category =
                        "control".equals(item.getEvidenceRole()) ? TargetPayloadCategory.CONTROL_TYPE
                                : TargetPayloadCategory.DISPLAY_TEXT;
                // BUSINESS_TABLE/SEARCH_AREA는 같은 producer를 공유하므로 row/cell/pair를 그대로
                // structuredData에 복사한다 -- 새 geometry나 renderer 재계산 없음.
                Map<String, Object> structuredData = null;
                if ("BUSINESS_TABLE".equals(family) || "SEARCH_AREA".equals(family)) {
                    structuredData = new LinkedHashMap<String, Object>();
                    structuredData.put("rowIndex", item.getRowIndex());
                    structuredData.put("cellIndexInRow", item.getCellIndexInRow());
                    structuredData.put("pairIndexInRow", item.getPairIndexInRow());
                }
                items.add(new TargetLeafPayload(
                        category, item.getValue(), structuredData, item.getEvidenceKind(),
                        item.getSourceComponentStructuralId()));
            }
        }
        return items;
    }

    /**
     * family별 실제 producer가 생성하는 role/evidenceKind/value 조합만 인정한다. label/title_label/
     * button은 text-then-value 계약(source_text_attribute[_absent])을 공유, control은
     * source_tag_name만, tab_label은 별도 titletext 계약을 쓴다. 잘못된 evidence를 정상화하지 않는다.
     */
    private void validateEvidenceShape(SourcePayloadEvidenceItem item) {
        String role = item.getEvidenceRole();
        String kind = item.getEvidenceKind();
        String value = item.getValue();
        // functionName은 role="event" 전용 필드 -- 다른 role에 붙어 있으면 evidence 오염 신호다.
        if (!"event".equals(role) && item.getFunctionName() != null) {
            throw new IllegalStateException(
                    "target_payload_extractor: evidenceRole=\"" + role + "\" must not carry a functionName "
                            + "(functionName is reserved for role=\"event\"), but functionName=\""
                            + item.getFunctionName() + "\"");
        }
        if ("label".equals(role) || "title_label".equals(role) || "button".equals(role)) {
            if ("source_text_attribute".equals(kind) || "source_value_attribute".equals(kind)) {
                if (value == null || value.length() == 0) {
                    throw new IllegalStateException(
                            "target_payload_extractor: evidenceKind=\"" + kind + "\" requires a non-null/"
                                    + "non-empty value, but value=" + (value == null ? "null" : "\"\""));
                }
            } else if ("source_text_attribute_absent".equals(kind)) {
                if (value != null) {
                    throw new IllegalStateException(
                            "target_payload_extractor: evidenceKind=\"source_text_attribute_absent\" requires "
                                    + "value=null, but value=\"" + value + "\"");
                }
            } else {
                throw new IllegalStateException(
                        "target_payload_extractor: unrecognized evidenceKind=\"" + kind + "\" for role=" + role);
            }
        } else if ("control".equals(role)) {
            if (!"source_tag_name".equals(kind)) {
                throw new IllegalStateException(
                        "target_payload_extractor: unrecognized evidenceKind=\"" + kind + "\" for role=control");
            }
            if (value == null || value.length() == 0) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidenceKind=\"source_tag_name\" requires a non-null/"
                                + "non-empty value");
            }
        } else if ("event".equals(role)) {
            if (!"event_binding".equals(kind)) {
                throw new IllegalStateException(
                        "target_payload_extractor: unrecognized evidenceKind=\"" + kind + "\" for role=event "
                                + "(producer only ever emits \"event_binding\")");
            }
            if (value == null || value.trim().length() == 0) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidenceKind=\"event_binding\" requires a non-null/"
                                + "non-blank eventName value, but value=" + (value == null ? "null" : "\"" + value + "\""));
            }
            if (item.getFunctionName() == null || item.getFunctionName().trim().length() == 0) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidenceKind=\"event_binding\" requires a non-null/"
                                + "non-blank functionName, but functionName="
                                + (item.getFunctionName() == null ? "null" : "\"" + item.getFunctionName() + "\""));
            }
        } else if ("tab_label".equals(role)) {
            if ("source_text_attribute".equals(kind) || "source_titletext_attribute".equals(kind)) {
                if (value == null || value.length() == 0) {
                    throw new IllegalStateException(
                            "target_payload_extractor: evidenceKind=\"" + kind + "\" requires a non-null/"
                                    + "non-empty value, but value=" + (value == null ? "null" : "\"\""));
                }
            } else if ("source_tab_label_absent".equals(kind)) {
                if (value != null) {
                    throw new IllegalStateException(
                            "target_payload_extractor: evidenceKind=\"source_tab_label_absent\" requires "
                                    + "value=null, but value=\"" + value + "\"");
                }
            } else {
                throw new IllegalStateException(
                        "target_payload_extractor: unrecognized evidenceKind=\"" + kind + "\" for role=tab_label");
            }
        } else {
            throw new IllegalStateException("target_payload_extractor: unrecognized evidenceRole=\"" + role + "\"");
        }
        if (item.getSourceOrder() < 0) {
            throw new IllegalStateException("target_payload_extractor: sourceOrder must be >= 0");
        }
    }

    /**
     * provenance 검증(존재/유일/containment)만으로는 wrapper Div 같은 엉뚱한 Element가 evidence
     * identity를 사칭해도 걸러내지 못한다. family+role마다 producer가 구조적으로 보장하는 실제
     * source tag만 인정한다 -- 새 semantic 판정이 아니라 producer invariant 재확인이다.
     */
    private void validateSourceElementRoleContract(String family, String role, Element leaf) {
        String tag = sourceTagName(leaf);
        String expectedTag;
        if ("BUTTON_GROUP".equals(family) && ("button".equals(role) || "event".equals(role))) {
            expectedTag = "Button";
        } else if ("TITLE_BAR".equals(family) && "title_label".equals(role)) {
            expectedTag = "Static";
        } else if ("TAB_CONTROL".equals(family) && "tab_label".equals(role)) {
            expectedTag = "Tabpage";
        } else if (("SEARCH_AREA".equals(family) || "BUSINESS_TABLE".equals(family)) && "label".equals(role)) {
            expectedTag = "Static";
        } else if (("SEARCH_AREA".equals(family) || "BUSINESS_TABLE".equals(family)) && "control".equals(role)) {
            if (!SHARED_LABEL_CONTROL_TAGS.contains(tag)) {
                throw new IllegalStateException(
                        "target_payload_extractor: evidenceRole=\"control\" for family=" + family + " must resolve "
                                + "to a source Element with tag in " + SHARED_LABEL_CONTROL_TAGS + ", but actual "
                                + "tag=\"" + tag + "\" (producer invariant violated -- wrong source Element)");
            }
            return;
        } else {
            return;
        }
        if (!expectedTag.equals(tag)) {
            throw new IllegalStateException(
                    "target_payload_extractor: evidenceRole=\"" + role + "\" for family=" + family + " must "
                            + "resolve to a source Element with tag=\"" + expectedTag + "\", but actual tag=\""
                            + tag + "\" (producer invariant violated -- wrong source Element)");
        }
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

    /**
     * upstream이 이미 계산해 넘긴 binding evidence만 소비한다(raw source 재스캔 없음). binding 계약
     * 판정을 먼저 하고(exact-resolved/ambiguous 각각 별도 사유), 문제가 없어 unbound로 판명되면
     * Slice 99E 사유로 이어서 fail-closed한다(accepted v6 출력의 rendering 동등성 미증명).
     */
    private void rejectUnprovenCheckBoxContract(
            Element leaf, String leafStructuralId, List<SourceBindingReference> bindingReferences) {
        if (!"CheckBox".equals(sourceTagName(leaf))) {
            return;
        }
        for (SourceBindingReference reference : bindingReferences) {
            if (reference.getResolution() == SourceBindingReference.ComponentResolution.RESOLVED_EXACT_ONE_COMPONENT
                    && leafStructuralId.equals(reference.getResolvedComponentStructuralIdentity())) {
                throw new IllegalStateException(
                        "target_payload_extractor: CheckBox is the exact-resolved target of a source BindItem "
                                + "with no proven propid/value-semantics contract for CheckBox -- refusing to "
                                + "guess (checkbox_dataset_binding_no_proven_target_contract:compid="
                                + reference.getCompid() + ")");
            }
            if (reference.getResolution() == SourceBindingReference.ComponentResolution.UNRESOLVED_AMBIGUOUS_COMPONENT_MATCH
                    && reference.getCandidateComponentStructuralIdentities().contains(leafStructuralId)) {
                throw new IllegalStateException(
                        "target_payload_extractor: CheckBox is one of multiple ambiguous candidates for a source "
                                + "BindItem component reference -- refusing to guess which candidate is intended "
                                + "(checkbox_dataset_binding_component_reference_ambiguous:compid="
                                + reference.getCompid() + ")");
            }
        }
        throw new IllegalStateException(
                "target_payload_extractor: accepted v6 CheckBox rendering/runtime equivalence against "
                        + "historical widget/bootstrap evidence is not proven -- refusing to publish an "
                        + "unverified representation (checkbox_unbound_rendering_equivalence_not_proven)");
    }

    /**
     * BUSINESS_TABLE/SEARCH_AREA의 label/control evidence set이 자체적으로 일관된 구조인지
     * fail-closed 검증한다: row/cell/pair 모두 non-null, 중복/충돌 소유 금지,
     * cellIndexInRow는 pairIndexInRow*2(+1)와 일치, (row,pair)마다 label 1 + control 1이어야 한다.
     */
    private void validateBusinessTableStructuralIntegrity(String family, List<SourcePayloadEvidenceItem> evidence) {
        if (!"BUSINESS_TABLE".equals(family) && !"SEARCH_AREA".equals(family)) {
            return;
        }
        Map<String, int[]> tupleBySourceComponent = new LinkedHashMap<String, int[]>();
        Map<String, String> ownerByRowCell = new LinkedHashMap<String, String>();
        Map<String, List<SourcePayloadEvidenceItem>> itemsByRowPair =
                new LinkedHashMap<String, List<SourcePayloadEvidenceItem>>();

        for (SourcePayloadEvidenceItem item : evidence) {
            String role = item.getEvidenceRole();
            if (!"label".equals(role) && !"control".equals(role)) {
                continue;
            }
            Integer row = item.getRowIndex();
            Integer cell = item.getCellIndexInRow();
            Integer pair = item.getPairIndexInRow();
            if (row == null || cell == null || pair == null) {
                throw new IllegalStateException(
                        "target_payload_extractor: " + family + " role=\"" + role + "\" evidence for "
                                + "sourceComponentStructuralId=" + item.getSourceComponentStructuralId()
                                + " is missing structural metadata (rowIndex=" + row + " cellIndexInRow=" + cell
                                + " pairIndexInRow=" + pair + ") -- all three are required for BUSINESS_TABLE");
            }

            String componentId = item.getSourceComponentStructuralId();
            if (tupleBySourceComponent.containsKey(componentId)) {
                throw new IllegalStateException(
                        "target_payload_extractor: sourceComponentStructuralId=" + componentId + " has more "
                                + "than one BUSINESS_TABLE structural evidence entry -- refusing to silently "
                                + "pick one (duplicate/conflicting row/cell/pair evidence for the same leaf)");
            }
            tupleBySourceComponent.put(componentId, new int[] {row.intValue(), cell.intValue(), pair.intValue()});

            String rowCellKey = row + ":" + cell;
            String existingOwner = ownerByRowCell.get(rowCellKey);
            if (existingOwner != null && !existingOwner.equals(componentId)) {
                throw new IllegalStateException(
                        "target_payload_extractor: row/cell position (rowIndex=" + row + ", cellIndexInRow="
                                + cell + ") is claimed by two different source components (" + existingOwner
                                + " and " + componentId + ")");
            }
            ownerByRowCell.put(rowCellKey, componentId);

            int expectedCell = "label".equals(role) ? pair.intValue() * 2 : pair.intValue() * 2 + 1;
            if (cell.intValue() != expectedCell) {
                throw new IllegalStateException(
                        "target_payload_extractor: " + family + " role=\"" + role + "\" sourceComponentStructuralId="
                                + componentId + " has cellIndexInRow=" + cell + " inconsistent with "
                                + "pairIndexInRow=" + pair + " (expected cellIndexInRow=" + expectedCell + ")");
            }

            String rowPairKey = row + ":" + pair;
            List<SourcePayloadEvidenceItem> group = itemsByRowPair.get(rowPairKey);
            if (group == null) {
                group = new ArrayList<SourcePayloadEvidenceItem>();
                itemsByRowPair.put(rowPairKey, group);
            }
            group.add(item);
        }

        for (Map.Entry<String, List<SourcePayloadEvidenceItem>> entry : itemsByRowPair.entrySet()) {
            int labelCount = 0;
            int controlCount = 0;
            for (SourcePayloadEvidenceItem item : entry.getValue()) {
                if ("label".equals(item.getEvidenceRole())) {
                    labelCount++;
                } else {
                    controlCount++;
                }
            }
            if (labelCount != 1 || controlCount != 1) {
                throw new IllegalStateException(
                        "target_payload_extractor: " + family + " pair(rowIndex:pairIndexInRow=" + entry.getKey()
                                + ") must have exactly 1 label + 1 control, but found label=" + labelCount
                                + " control=" + controlCount);
            }
        }
    }

    /** structuralId가 sourceRoot 안에서 정확히 하나의 Element를 가리키는지 확인한다(0개/2개 이상 모두 실패). */
    private Element resolveElementOrFail(Map<String, List<Element>> elementsByStructuralId, String structuralId, String label) {
        List<Element> matches = elementsByStructuralId.get(structuralId);
        if (matches == null || matches.isEmpty()) {
            throw new IllegalStateException(
                    "target_payload_extractor: " + label + " sourceComponentStructuralId=\"" + structuralId
                            + "\" does not exist in sourceRoot");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "target_payload_extractor: " + label + " sourceComponentStructuralId=\"" + structuralId
                            + "\" matches " + matches.size() + " distinct Elements (ambiguous provenance)");
        }
        return matches.get(0);
    }

    /** anchor 없이 lenient하게 단일 매치를 찾는다(GRID의 관대한 skip 동작 유지용 -- 0개/2개
     * 이상이면 그냥 null, 여기서는 실패하지 않는다). */
    private Element resolveUniqueElement(Map<String, List<Element>> elementsByStructuralId, String structuralId) {
        List<Element> matches = elementsByStructuralId.get(structuralId);
        return (matches != null && matches.size() == 1) ? matches.get(0) : null;
    }

    /** {@code getParentNode()} chain만 따라 확인한다 -- structuralId 문자열 prefix 파싱 금지. */
    private boolean isDescendant(Element candidate, Element ancestor) {
        Node current = candidate.getParentNode();
        while (current instanceof Element) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParentNode();
        }
        return false;
    }

    /**
     * GRID: {@code GridFormatParser}를 그대로 재사용한다(새 파서 없음). head/body/summ band의
     * 모든 Cell을 GRID_COLUMN leaf 하나씩으로 옮긴다 -- Cell이 제공하는 필드만 담는다.
     */
    private List<TargetLeafPayload> extractGrid(Element gridElement) {
        List<TargetLeafPayload> items = new ArrayList<TargetLeafPayload>();
        GridFormatParser.GridFormatSelection selection = gridFormatParser.resolveFormat(gridElement);
        // Slice 99B correction -- source 문법에 다중 Format 중 활성 Format을 고르는 selector로
        // 증명된 evidence가 없다. 이 상태를 렌더러의 missing-parameter 실패에 떠넘기지 않고, 여기서
        // 명시적/결정적으로 fail-closed한다(렌더러는 이 지점에 도달하지 않는다).
        if (selection.requiresExplicitAmbiguityFailure()) {
            throw new IllegalStateException(
                    "target_payload_extractor: GRID has multiple Format definitions with no proven "
                            + "source selector authority -- refusing to guess an active Format ("
                            + selection.getEvidence() + ")");
        }
        // Format이 아예 없는 경우(NO_FORMAT_DEFINITION)는 기존부터 non-fatal한 정상 케이스이므로
        // leaf를 만들지 않고 그대로 빈 목록을 반환한다.
        if (!selection.isResolved()) {
            return items;
        }
        GridFormatParser.GridFormat format = selection.getFormat();
        addGridCells(items, "head", format.getHeadCells());
        addGridCells(items, "body", format.getBodyCells());
        addGridCells(items, "summ", format.getSummCells());
        return items;
    }

    private void addGridCells(List<TargetLeafPayload> items, String band, List<GridFormatParser.CellDef> cells) {
        for (GridFormatParser.CellDef cell : cells) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("band", band);
            data.put("col", cell.getCol());
            data.put("row", cell.getRow());
            data.put("colSpan", cell.getColSpan());
            data.put("rowSpan", cell.getRowSpan());
            data.put("displayType", cell.getDisplayType());
            data.put("editType", cell.getEditType());
            data.put("align", cell.getAlign());
            data.put("readOnly", cell.isReadOnly());
            if (cell.getComboDataset().length() > 0) {
                data.put("comboDataset", cell.getComboDataset());
            }
            if (cell.getComboCodeColumn().length() > 0) {
                data.put("comboCodeColumn", cell.getComboCodeColumn());
            }
            if (cell.getComboDataColumn().length() > 0) {
                data.put("comboDataColumn", cell.getComboDataColumn());
            }
            items.add(new TargetLeafPayload(
                    TargetPayloadCategory.GRID_COLUMN, cell.getText(), data, "grid_format_parser", null));
        }
    }

    private void indexStructuralIdentities(Element element, Map<String, List<Element>> out) {
        String structuralId = SourceStructuralIdentity.build(element);
        List<Element> list = out.get(structuralId);
        if (list == null) {
            list = new ArrayList<Element>();
            out.put(structuralId, list);
        }
        list.add(element);
        for (Element child : directElementChildren(element)) {
            indexStructuralIdentities(child, out);
        }
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

}
