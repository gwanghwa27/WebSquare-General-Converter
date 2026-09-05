package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.converter.ComponentLayoutConverter;
import com.example.xfdltracker.converter.GridFormatParser;
import com.example.xfdltracker.mapping.ComponentMappingRegistry;
import com.example.xfdltracker.model.EventBinding;
import com.example.xfdltracker.model.XfdlAnalysisResult;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;
import com.example.xfdltracker.semantic.SourceStructuralIdentity;
import com.example.xfdltracker.semantic.TabPageMembership;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shadow-only Semantic Region Segmenter. 7개 predicate(GRID/TAB_CONTROL/SPLIT_LAYOUT/
 * SEARCH_AREA/BUSINESS_TABLE.horizontal/TITLE_BAR/BUTTON_GROUP) 지원, 나머지는 미구현.
 * 기존 변환 흐름(WebSquareGenerator 등)에 연결되지 않는 독립 관찰 전용 API다.
 */
public class SemanticRegionSegmenter {

    /**
     * GRID/TAB_CONTROL/BUSINESS_TABLE/SEARCH_AREA/TITLE_BAR/BUTTON_GROUP의 component-level
     * predicate 계산을 담당하는 협력자. confidence/variant/{@code SemanticRegionResult} 생성
     * 권한은 여전히 이 클래스(Segmenter)만 갖는다.
     */
    private final ComponentPredicateAnalyzer componentPredicateAnalyzer;

    public SemanticRegionSegmenter() {
        this(new ComponentPredicateAnalyzer());
    }

    public SemanticRegionSegmenter(ComponentPredicateAnalyzer componentPredicateAnalyzer) {
        if (componentPredicateAnalyzer == null) {
            throw new IllegalArgumentException("semantic_region_segmenter: componentPredicateAnalyzer must not be null");
        }
        this.componentPredicateAnalyzer = componentPredicateAnalyzer;
    }

    /**
     * SPLIT_LAYOUT geometry 판정은 {@link ComponentLayoutConverter}의 기존 로직을 그대로
     * 재사용한다(col_N/col_33 판정을 이 클래스에서 복제하지 않음). SPLIT_LAYOUT은 Component
     * Predicate 이관 범위 밖이다.
     */
    private final ComponentLayoutConverter layoutConverter = new ComponentLayoutConverter();

    /**
     * SPLIT_LAYOUT "형제 컨테이너" 자격 판정은 기존 authoritative source인
     * {@link ComponentMappingRegistry#isContainer(String)}를 그대로 재사용하며, 대상은
     * Div/GroupBox/PopupDiv/Tab/Tabpage 컴포넌트다.
     */
    private final ComponentMappingRegistry componentMappings = new ComponentMappingRegistry();

    /** GRID의 column_count/column_width는 기존 {@link GridFormatParser}를 재사용해 추출한다. */
    private final GridFormatParser gridFormatParser = new GridFormatParser();

    /**
     * SEARCH_AREA/BUSINESS_TABLE.horizontal이 공유하는 control 어휘(Edit/Combo/Calendar/
     * CheckBox/Radio). Button은 BUTTON_GROUP/TITLE_BAR와 겹치므로 의도적으로 제외한다.
     */
    private static final List<String> SHARED_LABEL_CONTROL_TAGS =
            Arrays.asList("Edit", "Combo", "Calendar", "CheckBox", "Radio");

    /** SEARCH_AREA column_count(행당 라벨/값 쌍)의 카탈로그 관찰 범위 상한값(1-4). */
    private static final int SEARCH_AREA_OBSERVED_MAX_PAIR_COUNT = 4;

    /**
     * source 트리를 재귀 순회하며 매칭되는 모든 인스턴스에 대해 {@link SemanticRegionResult}를
     * 만든다. 중첩된 Tab도 각각 독립 결과를 갖는다(병합하지 않음). 각 element 방문 시 그
     * 직계 자식 집합도 SPLIT_LAYOUT 후보로 함께 평가한다.
     */
    public List<SemanticRegionResult> segment(Element root) {
        return segment(root, null);
    }

    /**
     * {@code analysis}가 null이면 event boundary 축 없이 visible/enable만으로 판정하는
     * conservative shadow 분석이다. 넘기면 {@link EventBinding} 목록으로 event boundary도
     * 채운다. 기존 변환 흐름에는 여전히 연결되지 않는다.
     */
    public List<SemanticRegionResult> segment(Element root, XfdlAnalysisResult analysis) {
        List<SemanticRegionResult> results = new ArrayList<SemanticRegionResult>();
        if (root != null) {
            // BUTTON_GROUP EVENT evidence 상관을 위한 안전성 색인. analysis 없으면 계산 안 함.
            Map<String, Set<Element>> eventPathIndex = analysis == null
                    ? Collections.<String, Set<Element>>emptyMap() : indexEventPathsForCorrelation(root);
            // pass-local, identity-keyed 캐시: segment() 호출마다 새로 만들며 필드로 공유하지 않는다.
            Map<Element, ComponentPredicateAnalysis> predicateCache =
                    new java.util.IdentityHashMap<Element, ComponentPredicateAnalysis>();
            walk(root, results, collectEventBoundComponentPaths(analysis), analysis, eventPathIndex, predicateCache,
                    null, null, null);
        }
        return results;
    }

    /** {@link ComponentPredicateAnalyzer#analyze} 호출의 단일 진입점. 모든 호출자가 이 캐시를
     *  거치므로 같은 {@code element}는 pass당 최대 1회만 분석되며, 항상 동일한 {@code analysis}를
     *  쓴다(null로 치환하지 않음). */
    private ComponentPredicateAnalysis analyzeCached(
            Element element, XfdlAnalysisResult analysis, Map<Element, ComponentPredicateAnalysis> predicateCache) {
        ComponentPredicateAnalysis cached = predicateCache.get(element);
        if (cached != null) {
            return cached;
        }
        ComponentPredicateAnalysis computed = componentPredicateAnalyzer.analyze(element, analysis);
        predicateCache.put(element, computed);
        onPredicateAnalysisComputed(element, analysis);
        return computed;
    }

    /**
     * package-private no-op 훅. cache-miss로 실제 analyze가 호출될 때마다 정확히 1회 호출되어
     * 테스트가 캐시 exactly-once 동작을 검증할 수 있게 한다. 기본 동작에는 영향 없음.
     */
    void onPredicateAnalysisComputed(Element element, XfdlAnalysisResult analysis) {
    }

    /**
     * {@code currentMembership}은 이미 확정된 가장 가까운 TAB_CONTROL page, {@code
     * pendingTabControlStructuralId}/{@code pendingTabControlDirectPages}는 실제 {@code Tabpage}를
     * 만날 때 확정될 미확정 TAB_CONTROL이다(안쪽이 바깥쪽을 가림, nearest wins).
     */
    private void walk(
            Element element, List<SemanticRegionResult> results, Set<String> eventBoundComponentPaths,
            XfdlAnalysisResult analysis, Map<String, Set<Element>> eventPathIndex,
            Map<Element, ComponentPredicateAnalysis> predicateCache,
            TabPageMembership currentMembership,
            String pendingTabControlStructuralId, List<Element> pendingTabControlDirectPages) {
        TabPageMembership effectiveMembership = currentMembership;
        String childPendingTabControlStructuralId = pendingTabControlStructuralId;
        List<Element> childPendingTabControlDirectPages = pendingTabControlDirectPages;
        if (pendingTabControlDirectPages != null) {
            int pageOrdinal = identityIndexOf(pendingTabControlDirectPages, element);
            if (pageOrdinal >= 0) {
                effectiveMembership = new TabPageMembership(pendingTabControlStructuralId, pageOrdinal);
                childPendingTabControlStructuralId = null;
                childPendingTabControlDirectPages = null;
            }
        }

        // element당 정확히 한 번만 계산(analyzeCached 경유); 아래 family는 모두 이 facts를 재사용하고
        // 나중의 인접-형제 TITLE_BAR 검사도 같은 캐시 결과를 재사용한다.
        ComponentPredicateAnalysis facts = analyzeCached(element, analysis, predicateCache);
        int resultsSizeBeforeThisElement = results.size();

        if (facts.getGrid().isMatched()) {
            SemanticRegionResult gridResult = buildComponentPredicateResult("GRID", element);
            applyGridFormatParameters(gridResult, element);
            results.add(gridResult);
        } else if (facts.getTabControl().isMatched()) {
            SemanticRegionResult tabResult = buildComponentPredicateResult("TAB_CONTROL", element);
            applyTabControlEvidence(tabResult, element);
            results.add(tabResult);
            // 이 TAB_CONTROL의 자식들은 이제부터 자기 자신의 pages를 기준으로 해석되며,
            // 이 하위 subtree에 대해 아직 보류 중이던 외부 TAB_CONTROL을 대체(shadow)한다.
            childPendingTabControlStructuralId = tabResult.getSourceStructuralId();
            childPendingTabControlDirectPages = directTabpages(element);
        }

        List<Element> children = directElementChildren(element);
        SemanticRegionResult splitLayoutCandidate = evaluateSplitLayoutRegion(element, children);
        SemanticRegionResult titleOrButtonCandidate = evaluateTitleBarOrButtonGroupRegion(
                element, children, facts, analysis, eventPathIndex, predicateCache);
        addWithoutDuplicateRegionOwnership(results, splitLayoutCandidate, titleOrButtonCandidate);
        evaluateSearchAreaOrBusinessTableRegion(element, facts, results);

        for (int i = resultsSizeBeforeThisElement; i < results.size(); i++) {
            results.get(i).setTabPageMembership(effectiveMembership);
        }

        for (Element child : children) {
            walk(child, results, eventBoundComponentPaths, analysis, eventPathIndex, predicateCache,
                    effectiveMembership, childPendingTabControlStructuralId, childPendingTabControlDirectPages);
        }
    }

    /** identity(==) 기반 인덱스 조회. 구조적으로 같은 값을 갖는 다른 Element와 혼동하지 않기 위함. */
    private int identityIndexOf(List<Element> elements, Element target) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * SPLIT_LAYOUT과 BUTTON_GROUP 게이트가 둘 다 "자식 전부 Div 컨테이너"를 허용하므로,
     * 같은 부모가 우연히 둘 다 매치될 수 있다. 두 후보가 non-null이면(항상 같은 region) 조용히
     * 하나를 고르지 않고 둘 다 발행하지 않는다(HOLD).
     */
    private void addWithoutDuplicateRegionOwnership(
            List<SemanticRegionResult> results, SemanticRegionResult a, SemanticRegionResult b) {
        if (a != null && b != null) {
            return;
        }
        if (a != null) {
            results.add(a);
        }
        if (b != null) {
            results.add(b);
        }
    }

    /**
     * children을 SPLIT_LAYOUT sibling region 후보로 평가한다({@link ComponentMappingRegistry#isContainer(String)}
     * 인 경우만 대상). geometry 무효/컨테이너 미달이면 결과 없음, exact match면 HIGH/ratio_split,
     * 비율만 다르면 LOW/FIXED_WIDTH_FALLBACK.
     */
    private SemanticRegionResult evaluateSplitLayoutRegion(Element parent, List<Element> children) {
        if (children.size() < 2 || !allEligibleContainers(children)) {
            return null;
        }
        String status = layoutConverter.classifyColumnRatioGeometry(children);
        if ("GEOMETRY_INVALID".equals(status)) {
            return null;
        }

        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType("SPLIT_LAYOUT");
        result.setRecommendedTemplateFamily("SPLIT_LAYOUT");
        // region은 children이 아니라 그 children을 담는 parent 컨테이너다.
        result.setSourceRegionId(buildEventComponentPath(parent));
        result.setSourceStructuralId(SourceStructuralIdentity.build(parent));
        result.getHierarchyEvidence().add("same_parent_sibling_count=" + children.size());
        result.getComponentEvidence().add("columns_source_order=" + sourceOrderIds(children));

        // buildTableRows가 반환한 left-ascending 순서를 그대로 ordering evidence로 기록한다.
        List<Element> geometricOrderElements = firstRowOf(layoutConverter.buildTableRows(children));
        for (int rank = 0; rank < geometricOrderElements.size(); rank++) {
            result.getSplitColumnGeometryOrderBySiblingStructuralId().put(
                    SourceStructuralIdentity.build(geometricOrderElements.get(rank)), Integer.valueOf(rank));
        }

        if ("SPLIT_LAYOUT_RATIO_EXACT_MATCH".equals(status)) {
            String[] geometricOrderLabels = layoutConverter.resolveExactColumnRatios(children);
            String[] sourceOrderLabels = alignToSourceOrder(children, geometricOrderElements, geometricOrderLabels);

            result.setConfidence("HIGH");
            result.setRecommendedVariant("ratio_split");
            result.getGeometryEvidence().add("column_ratio_exact_match=" + sourceOrderJoin(sourceOrderLabels));
            result.getParameters().put("column_ratio", sourceOrderLabels);
        } else {
            // geometry는 성립하나 canonical ratio가 아님. 근접값을 승격시키지 않으므로 variant는 비움.
            result.setConfidence("LOW");
            result.setFallback("FIXED_WIDTH_FALLBACK");
            result.getGeometryEvidence().add("column_ratio_non_exact_uncalibrated");
        }
        return result;
    }

    private boolean allEligibleContainers(List<Element> children) {
        for (Element child : children) {
            if (!componentMappings.isContainer(sourceTagName(child))) {
                return false;
            }
        }
        return true;
    }

    private List<Element> firstRowOf(List<List<Element>> rows) {
        return rows.isEmpty() ? new ArrayList<Element>() : rows.get(0);
    }

    /** left-ascending 순서의 라벨을 Element에 대응시킨 뒤 원본 DOM 순서로 재배열한다. */
    private String[] alignToSourceOrder(List<Element> sourceOrder, List<Element> geometricOrderElements, String[] geometricOrderLabels) {
        Map<Element, String> labelByElement = new IdentityHashMap<Element, String>();
        for (int i = 0; i < geometricOrderElements.size() && i < geometricOrderLabels.length; i++) {
            labelByElement.put(geometricOrderElements.get(i), geometricOrderLabels[i]);
        }
        String[] aligned = new String[sourceOrder.size()];
        for (int i = 0; i < sourceOrder.size(); i++) {
            aligned[i] = labelByElement.get(sourceOrder.get(i));
        }
        return aligned;
    }

    private String sourceOrderIds(List<Element> children) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(children.get(i).getAttribute("id"));
        }
        return sb.toString();
    }

    private String sourceOrderJoin(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /**
     * SEARCH_AREA/BUSINESS_TABLE.horizontal 공통 판정: children이 (Static, control) 교대 쌍이면
     * 후보가 되고, Wrapper Normalization으로 Grid를 찾으면 SEARCH_AREA, 못 찾으면
     * BUSINESS_TABLE.horizontal이다(패턴 미성립 시 HOLD, facts는 {@link ComponentPredicateAnalyzer} 재사용).
     */
    private void evaluateSearchAreaOrBusinessTableRegion(
            Element container, ComponentPredicateAnalysis facts, List<SemanticRegionResult> results) {
        boolean searchAreaMatched = facts.getSearchArea().isMatched();
        boolean businessTableMatched = facts.getBusinessTable().isMatched();
        if (!searchAreaMatched && !businessTableMatched) {
            return;
        }
        ComponentPredicateAnalysis.TableStructureFacts structure = searchAreaMatched
                ? facts.getSearchArea().getStructure() : facts.getBusinessTable().getStructure();
        List<List<ComponentPredicateAnalysis.TableCellFact>> rows = structure.getRows();

        int maxPairCount = 0;
        for (List<ComponentPredicateAnalysis.TableCellFact> row : rows) {
            maxPairCount = Math.max(maxPairCount, row.size() / 2);
        }

        SemanticRegionResult result = new SemanticRegionResult();
        result.setSourceRegionId(buildEventComponentPath(container));
        result.setSourceStructuralId(SourceStructuralIdentity.build(container));
        result.getHierarchyEvidence().add("row_count=" + rows.size());
        result.getComponentEvidence().add("pair_pattern=static_control_alternating");
        captureLabelControlPairEvidence(result, rows);

        if (searchAreaMatched) {
            result.setSemanticType("SEARCH_AREA");
            result.setRecommendedTemplateFamily("SEARCH_AREA");
            result.setRecommendedVariant("basic");
            // 카탈로그 관찰 범위(1-4)를 넘는 pair 개수는 HIGH로 자동 승격하지 않는다.
            result.setConfidence(maxPairCount > SEARCH_AREA_OBSERVED_MAX_PAIR_COUNT ? "MEDIUM" : "HIGH");
            result.getHierarchyEvidence().add("wrapper_normalization_nearest_peer=GRID");
            if (maxPairCount > SEARCH_AREA_OBSERVED_MAX_PAIR_COUNT) {
                result.getHierarchyEvidence().add("column_count_exceeds_observed_range_1_to_4");
            }
            result.getParameters().put("column_count", maxPairCount);
            result.getParameters().put("row_count", rows.size());
        } else {
            result.setSemanticType("BUSINESS_TABLE");
            result.setRecommendedTemplateFamily("BUSINESS_TABLE");
            result.setRecommendedVariant("horizontal");
            // 다중 행 규칙성이 HIGH 근거이므로 행이 하나뿐이면 MEDIUM으로 낮춘다.
            result.setConfidence(rows.size() >= 2 ? "HIGH" : "MEDIUM");
            String reason = structure.getPeerOpaqueBoundaryReason();
            result.getHierarchyEvidence().add(reason != null
                    ? "wrapper_normalization_stopped_at_" + reason
                    : "wrapper_normalization_no_grid_found_in_scope");
            result.getParameters().put("column_pair_count", maxPairCount);
            result.getParameters().put("row_count", rows.size());
        }
        results.add(result);
    }

    /**
     * 이미 검증된 {@code rows}의 leaf 값들을 {@link SourcePayloadEvidenceItem}으로 옮겨 담는다
     * (geometry 재계산 없음). SEARCH_AREA/BUSINESS_TABLE 어느 쪽이든 evidence capture는 동일하다.
     */
    private void captureLabelControlPairEvidence(
            SemanticRegionResult result, List<List<ComponentPredicateAnalysis.TableCellFact>> rows) {
        String regionStructuralId = result.getSourceStructuralId();
        int pairIndex = 0;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<ComponentPredicateAnalysis.TableCellFact> row = rows.get(rowIndex);
            for (int i = 0; i + 1 < row.size(); i += 2) {
                ComponentPredicateAnalysis.TableCellFact label = row.get(i);
                ComponentPredicateAnalysis.TableCellFact control = row.get(i + 1);
                Integer rowIdx = Integer.valueOf(rowIndex);
                Integer pairIdxInRow = Integer.valueOf(i / 2);
                addTextOrValueEvidenceItemFromFact(
                        result, regionStructuralId, label.getStructuralId(), label.getTextAttribute(),
                        label.getValueAttribute(), "label", pairIndex * 2, rowIdx, Integer.valueOf(i), pairIdxInRow);
                result.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                        regionStructuralId, control.getStructuralId(), "control", "source_tag_name",
                        control.getSourceTagName(), null, pairIndex * 2 + 1, rowIdx, Integer.valueOf(i + 1),
                        pairIdxInRow));
                if (control.getOptionResolution() != null) {
                    // Slice 102D -- option 선언 evidence(성공/실패 모두)가 있으면 control의
                    // structuralId를 key로 그대로 옮긴다(재계산/재판정 없음).
                    result.getOptionResolutionBySourceComponentStructuralId().put(
                            control.getStructuralId(), control.getOptionResolution());
                }
                pairIndex++;
            }
        }
    }

    /** {@link #addTextOrValueEvidenceItem}의 fact 기반 버전. 이미 스냅샷된 String 값에 대해
     *  동일한 text 우선/value fallback 계약을 적용한다(SEARCH_AREA/BUSINESS_TABLE/BUTTON_GROUP 공용). */
    private void addTextOrValueEvidenceItemFromFact(
            SemanticRegionResult result, String regionStructuralId, String structuralId, String textAttribute,
            String valueAttribute, String role, int sourceOrder, Integer rowIndex, Integer cellIndexInRow,
            Integer pairIndexInRow) {
        String value;
        String kind;
        String text = trimOrEmpty(textAttribute);
        if (text.length() > 0) {
            value = text;
            kind = "source_text_attribute";
        } else {
            String fallback = trimOrEmpty(valueAttribute);
            value = fallback.length() > 0 ? fallback : null;
            kind = fallback.length() > 0 ? "source_value_attribute" : "source_text_attribute_absent";
        }
        result.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                regionStructuralId, structuralId, role, kind, value, null, sourceOrder, rowIndex, cellIndexInRow,
                pairIndexInRow));
    }

    /**
     * {@code text} 우선/{@code value} fallback 계약으로 evidence 값을 읽는다. SEARCH_AREA/
     * BUSINESS_TABLE/TITLE_BAR/BUTTON_GROUP이 모두 같은 helper를 공유한다. 둘 다 없으면
     * value=null로 명시적 "값 없음" evidence를 남긴다.
     */
    private void addTextOrValueEvidenceItem(
            SemanticRegionResult result, String regionStructuralId, Element element, String role, int sourceOrder) {
        addTextOrValueEvidenceItem(result, regionStructuralId, element, role, sourceOrder, null, null, null);
    }

    /** {@code rowIndex}/{@code cellIndexInRow}/{@code pairIndexInRow}까지 함께 담는 overload.
     *  row 개념이 없는 title_label/button 호출부는 5-arg 버전에서 셋 다 null로 위임한다. */
    private void addTextOrValueEvidenceItem(
            SemanticRegionResult result, String regionStructuralId, Element element, String role, int sourceOrder,
            Integer rowIndex, Integer cellIndexInRow, Integer pairIndexInRow) {
        String value;
        String kind;
        String text = trimOrEmpty(element.getAttribute("text"));
        if (text.length() > 0) {
            value = text;
            kind = "source_text_attribute";
        } else {
            String fallback = trimOrEmpty(element.getAttribute("value"));
            value = fallback.length() > 0 ? fallback : null;
            kind = fallback.length() > 0 ? "source_value_attribute" : "source_text_attribute_absent";
        }
        result.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                regionStructuralId, SourceStructuralIdentity.build(element), role, kind, value, null, sourceOrder,
                rowIndex, cellIndexInRow, pairIndexInRow));
    }

    /**
     * TAB_CONTROL 전용. {@code text} 우선, 없으면 {@code titletext} fallback(둘 다 없으면
     * {@code source_tab_label_absent}로 명시). value fallback 계약과는 속성이 다르므로 kind
     * 이름을 분리한다.
     */
    private void addTabLabelEvidenceItem(
            SemanticRegionResult result, String regionStructuralId, Element element, int sourceOrder) {
        String value;
        String kind;
        String text = trimOrEmpty(element.getAttribute("text"));
        if (text.length() > 0) {
            value = text;
            kind = "source_text_attribute";
        } else {
            String fallback = trimOrEmpty(element.getAttribute("titletext"));
            value = fallback.length() > 0 ? fallback : null;
            kind = fallback.length() > 0 ? "source_titletext_attribute" : "source_tab_label_absent";
        }
        result.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                regionStructuralId, SourceStructuralIdentity.build(element), "tab_label", kind, value, sourceOrder));
    }

    /**
     * TITLE_BAR/BUTTON_GROUP.title_bar_attached/CATEGORY_FILTER는 자식 구성만으로 구분한다:
     * 선행 Static 1개+나머지 Button -&gt; TITLE_BAR, 전부 Button -&gt; BUTTON_GROUP, 선행 Static
     * 2개 이상은 CATEGORY_FILTER 미구현이라 HOLD(dispatch는 계산된 {@code facts}만 사용).
     */
    private SemanticRegionResult evaluateTitleBarOrButtonGroupRegion(
            Element container, List<Element> children, ComponentPredicateAnalysis facts,
            XfdlAnalysisResult analysis, Map<String, Set<Element>> eventPathIndex,
            Map<Element, ComponentPredicateAnalysis> predicateCache) {
        if (facts.getTitleBar().isMatched()) {
            return evaluateTitleBarCandidate(container, children);
        }
        if (facts.getButtonGroup().isMatched()) {
            return evaluateButtonGroupCandidate(
                    container, facts.getButtonGroup(), analysis, eventPathIndex, predicateCache);
        }
        return null;
    }

    /**
     * HIGH 조건: 첫 자식이 단일 Static, 나머지는 전부 Button(텍스트 기반 confidence 판정은
     * 미구현이라 구조 evidence 성립 시 항상 HIGH). gate는 호출자가 이미 확인했으므로 evidence만 구성.
     */
    private SemanticRegionResult evaluateTitleBarCandidate(Element container, List<Element> children) {
        List<Element> rest = children.subList(1, children.size());
        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType("TITLE_BAR");
        result.setRecommendedTemplateFamily("TITLE_BAR");
        result.setRecommendedVariant("title_only");
        result.setConfidence("HIGH");
        result.setSourceRegionId(buildEventComponentPath(container));
        result.setSourceStructuralId(SourceStructuralIdentity.build(container));
        result.getMatchedFeatures().add("leading_single_static");
        result.getComponentEvidence().add("leading_static_id=" + children.get(0).getAttribute("id"));
        if (!rest.isEmpty()) {
            result.getMatchedFeatures().add("trailing_buttons_only");
            result.getComponentEvidence().add("trailing_button_count=" + rest.size());
        }
        // predicate가 이미 확정한 leading Static을 재도출 없이 payload evidence로 남긴다.
        addTextOrValueEvidenceItem(result, result.getSourceStructuralId(), children.get(0), "title_label", 0);
        return result;
    }

    /**
     * HIGH 조건: 자식이 전부 Button인 컨테이너(투명 wrapper 내부 Button도 병합 인식, 불투명
     * wrapper는 병합하지 않고 자신이 별도 후보로 재평가됨). matched/facts는 {@code buttonGroupFacts}
     * 스냅샷에서만 읽는다(재수집 없음).
     */
    private SemanticRegionResult evaluateButtonGroupCandidate(
            Element container, ComponentPredicateAnalysis.ButtonGroupFacts buttonGroupFacts,
            XfdlAnalysisResult analysis, Map<String, Set<Element>> eventPathIndex,
            Map<Element, ComponentPredicateAnalysis> predicateCache) {
        List<ComponentPredicateAnalysis.ButtonElementFact> flattenedButtons = buttonGroupFacts.getFlattenedButtons();

        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType("BUTTON_GROUP");
        result.setRecommendedTemplateFamily("BUTTON_GROUP");
        result.setRecommendedVariant(determineButtonGroupVariant(container, analysis, predicateCache));
        result.setConfidence("HIGH");
        result.setSourceRegionId(buildEventComponentPath(container));
        result.setSourceStructuralId(SourceStructuralIdentity.build(container));
        result.getMatchedFeatures().add(buttonGroupFacts.isAnyWrapperMerged()
                ? "all_children_button_via_transparent_wrapper_merge"
                : "all_children_button");
        result.getComponentEvidence().add("button_count=" + flattenedButtons.size());
        // 기대값 authority: 다운스트림에서 실제 살아남는 leaf/ordinal 개수와 독립적으로 유지한다.
        result.setButtonGroupExpectedButtonCount(Integer.valueOf(flattenedButtons.size()));
        result.getParameters().put("position", buttonGroupFacts.getPosition());
        // 이미 확정된 flattenedButtons를 재도출 없이 payload evidence로 남긴다(순서는 source DOM 순서).
        for (int i = 0; i < flattenedButtons.size(); i++) {
            ComponentPredicateAnalysis.ButtonElementFact button = flattenedButtons.get(i);
            addTextOrValueEvidenceItemFromFact(
                    result, result.getSourceStructuralId(), button.getStructuralId(), button.getTextAttribute(),
                    button.getValueAttribute(), "button", i, null, null, null);
        }
        // EventBinding correlation이 안전한 경우(정확히 1개 Element에 매치)에만 EVENT evidence를
        // 남긴다(0 match/1 exact/2+ ambiguous, 동일 튜플 dedup, 충돌 functionName 거부).
        captureButtonGroupEventEvidence(result, flattenedButtons, analysis, eventPathIndex, flattenedButtons.size());
        return result;
    }

    /**
     * button path가 {@code eventPathIndex}에서 정확히 1개 Element만 가리킬 때만 안전하다(2개
     * 이상은 AMBIGUOUS_CORRELATION으로 미발행). 같은 eventName의 functionName이 전부 같으면
     * dedup 발행, 섞이면(충돌) 만들지 않는다(first/last-wins 금지).
     */
    private void captureButtonGroupEventEvidence(
            SemanticRegionResult result, List<ComponentPredicateAnalysis.ButtonElementFact> flattenedButtons,
            XfdlAnalysisResult analysis, Map<String, Set<Element>> eventPathIndex, int startOrder) {
        if (analysis == null) {
            return;
        }
        int order = startOrder;
        for (ComponentPredicateAnalysis.ButtonElementFact button : flattenedButtons) {
            String path = button.getEventComponentPath();
            if (path == null || path.length() == 0) {
                continue;
            }
            Set<Element> candidates = eventPathIndex.get(path);
            if (candidates == null || candidates.size() != 1) {
                continue;
            }

            // source model에는 명시적 이벤트 순서가 없다(encounter order는 계약이 아님). eventName은
            // 이미 dedup-validated이므로 TreeMap으로 사전식 정렬해 serialization determinism을 확보한다.
            Map<String, Set<String>> functionNamesByEvent = new java.util.TreeMap<String, Set<String>>();
            Map<String, EventBinding> anyBindingByEvent = new java.util.LinkedHashMap<String, EventBinding>();
            for (EventBinding event : analysis.getEvents()) {
                if (!path.equals(trimOrEmpty(event.getComponentId()))) {
                    continue;
                }
                Set<String> functionNames = functionNamesByEvent.get(event.getEventName());
                if (functionNames == null) {
                    functionNames = new HashSet<String>();
                    functionNamesByEvent.put(event.getEventName(), functionNames);
                }
                functionNames.add(event.getFunctionName());
                anyBindingByEvent.put(event.getEventName(), event);
            }

            String buttonStructuralId = button.getStructuralId();
            for (Map.Entry<String, Set<String>> entry : functionNamesByEvent.entrySet()) {
                if (entry.getValue().size() != 1) {
                    continue;
                }
                EventBinding event = anyBindingByEvent.get(entry.getKey());
                result.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                        result.getSourceStructuralId(), buttonStructuralId, "event", "event_binding",
                        event.getEventName(), event.getFunctionName(), order));
                order++;
            }
        }
    }

    /**
     * {@code title_bar_attached}는 markup만으로 구분되지 않으므로, 실제 TITLE_BAR 구조인 형제
     * (바로 앞/뒤)가 있을 때만 그렇게 판정한다. 없으면 standalone을 보수적 기본값으로 쓴다.
     */
    private String determineButtonGroupVariant(
            Element container, XfdlAnalysisResult analysis, Map<Element, ComponentPredicateAnalysis> predicateCache) {
        return isAdjacentToTitleBar(container, analysis, predicateCache) ? "title_bar_attached" : "standalone";
    }

    private boolean isAdjacentToTitleBar(
            Element container, XfdlAnalysisResult analysis, Map<Element, ComponentPredicateAnalysis> predicateCache) {
        Node parentNode = container.getParentNode();
        if (!(parentNode instanceof Element)) {
            return false;
        }
        List<Element> siblings = directElementChildren((Element) parentNode);
        int idx = indexOfIdentity(siblings, container);
        if (idx < 0) {
            return false;
        }
        if (idx > 0 && matchesTitleBarStructure(siblings.get(idx - 1), analysis, predicateCache)) {
            return true;
        }
        return idx + 1 < siblings.size()
                && matchesTitleBarStructure(siblings.get(idx + 1), analysis, predicateCache);
    }

    /** {@link ComponentPredicateAnalyzer}의 TITLE_BAR 판정을 sibling 후보에 재적용한다
     *  ({@link #analyzeCached} 경유이므로 최대 1회만 분석되고, 항상 pass의 실제 analysis를 쓴다). */
    private boolean matchesTitleBarStructure(
            Element candidate, XfdlAnalysisResult analysis, Map<Element, ComponentPredicateAnalysis> predicateCache) {
        if (!componentMappings.isContainer(sourceTagName(candidate))) {
            return false;
        }
        return analyzeCached(candidate, analysis, predicateCache).getTitleBar().isMatched();
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

    /**
     * {@code XfdlReader.buildComponentPath}와 동일 알고리즘: "Div" 조상만 id로 prefix해 Form까지
     * "."로 연결한다. 이래야 {@code EventBinding.componentId}와 정확히 대응된다.
     */
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

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** {@code analysis}의 event 바인딩 componentId 집합을 한 번만 계산한다(null이면 빈 집합). */
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

    /**
     * source 트리 전체의 id를 가진 Element에 대해 correlation key -> 실제 Element 집합을 만든다.
     * {@link #captureButtonGroupEventEvidence}가 path 유일성을 판단하는 근거다.
     */
    private Map<String, Set<Element>> indexEventPathsForCorrelation(Element root) {
        Map<String, Set<Element>> out = new java.util.LinkedHashMap<String, Set<Element>>();
        indexEventPathsForCorrelation(root, out);
        return out;
    }

    private void indexEventPathsForCorrelation(Element element, Map<String, Set<Element>> out) {
        String path = buildEventComponentPath(element);
        if (path.length() > 0) {
            Set<Element> set = out.get(path);
            if (set == null) {
                set = new java.util.LinkedHashSet<Element>();
                out.put(path, set);
            }
            set.add(element);
        }
        for (Element child : directElementChildren(element)) {
            indexEventPathsForCorrelation(child, out);
        }
    }

    private int indexOfIdentity(List<Element> elements, Element target) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) == target) {
                return i;
            }
        }
        return -1;
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

    /** GRID/TAB_CONTROL은 component_evidence 하나만으로 HIGH에 도달하는 hard predicate다.
     *  variant는 항상 basic 고정. */
    private SemanticRegionResult buildComponentPredicateResult(String family, Element source) {
        SemanticRegionResult result = new SemanticRegionResult();
        result.setSemanticType(family);
        result.setConfidence("HIGH");
        result.setRecommendedTemplateFamily(family);
        result.setRecommendedVariant("basic");
        result.setSourceRegionId(buildEventComponentPath(source));
        result.setSourceStructuralId(SourceStructuralIdentity.build(source));
        result.getComponentEvidence().add("source_component_type=" + sourceTagName(source));
        result.getComponentEvidence().add("source_id=" + source.getAttribute("id"));
        result.getMatchedFeatures().add("hard_predicate:component_type=" + sourceTagName(source));
        return result;
    }

    /**
     * TAB_CONTROL 판정과 독립적으로 evidence만 채운다(판정 자체에 영향 없음). 직계 Tabpage만
     * 대상으로 하며, {@code tab_count}는 같은 {@code pages} 크기를 그대로 옮긴 값이다.
     */
    private void applyTabControlEvidence(SemanticRegionResult result, Element tabSource) {
        List<Element> pages = directTabpages(tabSource);
        for (int i = 0; i < pages.size(); i++) {
            addTabLabelEvidenceItem(result, result.getSourceStructuralId(), pages.get(i), i);
        }
        result.getParameters().put("tab_count", Integer.valueOf(pages.size()));
    }

    /** 직계 Tabpage, 또는 직계 Tabpages의 자식 Tabpage만 모은다(더 깊은 중첩은 별도 TAB_CONTROL로 평가). */
    private List<Element> directTabpages(Element tab) {
        List<Element> result = new ArrayList<Element>();
        for (Element child : directElementChildren(tab)) {
            String tag = sourceTagName(child);
            if ("Tabpage".equals(tag)) {
                result.add(child);
            } else if ("Tabpages".equals(tag)) {
                for (Element grandchild : directElementChildren(child)) {
                    if ("Tabpage".equals(sourceTagName(grandchild))) {
                        result.add(grandchild);
                    }
                }
            }
        }
        return result;
    }

    /**
     * head Band Cell 개수로 column_count, Format Column size로 column_width를 채운다(단일
     * Format일 때만 resolved). 다중 Format 모호/중복 상태의 실제 fail-closed 권한은 이 클래스가
     * 아니라 {@link com.example.xfdltracker.payload.TargetPayloadExtractor}가 갖는다.
     */
    private void applyGridFormatParameters(SemanticRegionResult result, Element gridSource) {
        GridFormatParser.GridFormatSelection selection = gridFormatParser.resolveFormat(gridSource);
        result.getComponentEvidence().add("grid_format_selection=" + selection.getEvidence());
        if (!selection.isResolved()) {
            return;
        }
        GridFormatParser.GridFormat format = selection.getFormat();
        if (!format.getHeadCells().isEmpty()) {
            result.getParameters().put("column_count", format.getHeadCells().size());
            result.getComponentEvidence().add("column_count_source=head_band_cell_count");
        }
        if (!format.getColumnWidths().isEmpty()) {
            result.getParameters().put("column_width", format.getColumnWidths().toArray(new String[0]));
            result.getComponentEvidence().add("column_width_source=format_column_size");
        }
    }

    /** local name 우선, 없으면 prefix를 뗀 tagName. 기존 source 태그 판정 규칙을 재사용한다. */
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
