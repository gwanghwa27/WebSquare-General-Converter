package com.example.xfdltracker.converter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 2 UI 레이아웃 변환기. XPlatform 컴포넌트의 위치/크기를 WebSquare용 CSS로 변환한다.
 * 확정적으로 계산 가능한 좌표만 변환하며, 위험한 상대 참조식은 변환하지 않고
 * 생성기의 TODO 로그로 확인하도록 한다.
 */
public class ComponentLayoutConverter {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile(
            "([-+]?\\d+(?:\\.\\d+)?)(px|%)?",
            Pattern.CASE_INSENSITIVE);

    /**
     * source의 raw {@code style} 속성에서 병합을 허용하는 순수 visual property 화이트리스트.
     * geometry/구조 property는 절대 포함하지 않는다 -- geometry converter가 항상 authority이다.
     * {@link #appendVisualStyle} 전용.
     */
    private static final Set<String> SAFE_SOURCE_STYLE_PROPERTIES = buildSafeSourceStyleProperties();

    private static Set<String> buildSafeSourceStyleProperties() {
        Set<String> props = new LinkedHashSet<String>();
        props.add("background");
        props.add("background-color");
        props.add("border");
        props.add("color");
        props.add("font");
        props.add("font-size");
        props.add("font-weight");
        props.add("text-align");
        props.add("padding");
        props.add("visibility");
        props.add("opacity");
        return java.util.Collections.unmodifiableSet(props);
    }

    /** source가 left/top/width/height 등 위치/크기 속성을 하나라도 가지는지 여부. */
    public boolean hasGeometry(Element source) {
        return resolveGeometry(source).hasAnyPositionOrSize();
    }

    /** 일반 UI 컴포넌트의 CSS style 문자열을 생성한다(px, position 포함). */
    public String buildComponentStyle(Element source) {
        return buildComponentStyle(source, true);
    }

    /**
     * 일반 UI 컴포넌트의 CSS style(px) 문자열을 생성한다. {@code includePosition=false}면 Table 셀처럼
     * structural placement가 이미 위치를 결정하는 경우 {@code position:absolute}/left/top을 생성하지
     * 않는다.
     */
    public String buildComponentStyle(Element source, boolean includePosition) {
        Geometry geometry = resolveGeometry(source);
        StringBuilder style = new StringBuilder();

        if (geometry.hasAnyPositionOrSize()) {
            if (includePosition) {
                style.append("position:absolute;");
                appendCssLength(style, "left", geometry.left);
                appendCssLength(style, "top", geometry.top);
            }
            appendCssLength(style, "width", geometry.width);
            appendCssLength(style, "height", geometry.height);
        }

        appendVisualStyle(source, style);
        return style.toString();
    }

    /**
     * 루트 group의 style을 생성한다. Form/Layout 크기를 반영하여
     * XPlatform 절대좌표 기준으로 자식 컴포넌트를 배치할 수 있게 한다.
     */
    public String buildRootStyle(Document source) {
        StringBuilder style = new StringBuilder();
        // position:relative/overflow:hidden은 emit하지 않는다.

        Geometry geometry = findFormGeometry(source);
        if (geometry != null) {
            appendCssLength(style, "width", geometry.width);
            appendCssLength(style, "height", geometry.height);
        }

        if (style.indexOf("width:") < 0) {
            style.append("width:").append(formatPercent(100.0)).append(";");
        }
        if (style.indexOf("height:") < 0) {
            style.append("height:").append(formatPercent(100.0)).append(";");
        }
        return style.toString();
    }

    /** percentage 값을 소수점 첫째 자리까지 반올림해 "N.N%"로 포맷한다(예: 4.2105% -&gt; 4.2%). */
    public String formatPercent(double value) {
        return java.math.BigDecimal.valueOf(value)
                .setScale(1, java.math.RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    /**
     * percentage 기준(basis)은 항상 자식을 담는 {@code Layout} 엘리먼트 자신의 width/height(px)다
     * (Form root든 Div 내부든 동일 방식). width/height가 없거나 파싱 불가/0 이하이면 null(호출부가
     * unresolved로 처리).
     */
    public double[] resolveLayoutBasis(Element layout) {
        Geometry g = resolveGeometry(layout);
        if (isEmpty(g.width) || isEmpty(g.height)) {
            return null;
        }
        ParsedLength w = parseLength(g.width);
        ParsedLength h = parseLength(g.height);
        if (w == null || h == null || w.value <= 0.0 || h.value <= 0.0) {
            return null;
        }
        return new double[] {w.value, h.value};
    }

    /**
     * Form 바로 아래 Layout wrapper가 없거나 최상위 Layout에 크기가 없을 때의 fallback basis.
     * Form 자신의 선언 geometry만 사용한다(화면별 하드코딩 없음).
     */
    public double[] resolveFormBasis(Document source) {
        Geometry g = findFormGeometry(source);
        if (g == null || isEmpty(g.width) || isEmpty(g.height)) {
            return null;
        }
        ParsedLength w = parseLength(g.width);
        ParsedLength h = parseLength(g.height);
        if (w == null || h == null || w.value <= 0.0 || h.value <= 0.0) {
            return null;
        }
        return new double[] {w.value, h.value};
    }

    /**
     * source의 left/top/width/height를 basis 기준 percentage style로 변환한다.
     * {@code includePosition=false}면 left/top을 emit하지 않는다. geometry가 전혀 없으면 visual
     * style만 반환하고, basis/좌표를 확정적으로 읽을 수 없으면 null(호출부가 px fallback 결정).
     */
    public String buildPercentComponentStyle(
            Element source, double basisWidth, double basisHeight, boolean includePosition) {
        Geometry geometry = resolveGeometry(source);
        if (!geometry.hasAnyPositionOrSize()) {
            StringBuilder style = new StringBuilder();
            appendVisualStyle(source, style);
            return style.toString();
        }
        if (basisWidth <= 0.0 || basisHeight <= 0.0) {
            return null;
        }

        ParsedLength left = isEmpty(geometry.left) ? null : parseLength(geometry.left);
        ParsedLength top = isEmpty(geometry.top) ? null : parseLength(geometry.top);
        ParsedLength width = isEmpty(geometry.width) ? null : parseLength(geometry.width);
        ParsedLength height = isEmpty(geometry.height) ? null : parseLength(geometry.height);
        if (width == null || height == null) {
            return null;
        }
        if (includePosition && (left == null || top == null)) {
            return null;
        }

        StringBuilder style = new StringBuilder();
        if (includePosition) {
            style.append("position:absolute;");
            style.append("left:").append(formatPercent(left.value / basisWidth * 100.0)).append(";");
            style.append("top:").append(formatPercent(top.value / basisHeight * 100.0)).append(";");
        }
        style.append("width:").append(formatPercent(width.value / basisWidth * 100.0)).append(";");
        style.append("height:").append(formatPercent(height.value / basisHeight * 100.0)).append(";");
        appendVisualStyle(source, style);
        return style.toString();
    }

    /**
     * Table row wrapper(xf:group)의 style. row 안 셀들의 실제 top/height 분포로 row의 세로
     * footprint를 구해 basisHeight 대비 비율로 변환한다(균등분할 금지). 좌우 offset은 structural
     * placement로 대체하므로 emit하지 않는다. 계산 불가면 null.
     */
    public String buildTableRowStyle(List<Element> row, double basisHeight) {
        if (basisHeight <= 0.0) {
            return null;
        }
        double rowHeight = resolveRowBasisHeight(row);
        if (rowHeight <= 0.0) {
            return null;
        }
        return "width:" + formatPercent(100.0) + ";height:"
                + formatPercent(rowHeight / basisHeight * 100.0) + ";";
    }

    /**
     * Table cell wrapper(xf:group)의 style. 셀 자신의 width를 basisWidth 대비 비율로 변환한다(19번
     * 규칙). height는 row를 100% 채운다(structural placement). 계산 불가면 null.
     */
    public String buildTableCellStyle(Element cell, double basisWidth) {
        if (basisWidth <= 0.0) {
            return null;
        }
        double cellWidth = resolveCellBasisWidth(cell);
        if (cellWidth <= 0.0) {
            return null;
        }
        return "width:" + formatPercent(cellWidth / basisWidth * 100.0) + ";height:"
                + formatPercent(100.0) + ";";
    }

    /**
     * table cell 내부 컴포넌트는 cell/row 자신을 채우므로, Div/Layout 전체 basis가 아니라 cell 자신의
     * width(px)를 그 컴포넌트의 percent 계산 basis로 재사용한다({@link #buildTableCellStyle}과 동일
     * 값). 계산 불가면 -1.
     */
    public double resolveCellBasisWidth(Element cell) {
        if (cell == null) {
            return -1.0;
        }
        Geometry g = resolveGeometry(cell);
        ParsedLength width = isEmpty(g.width) ? null : parseLength(g.width);
        if (width == null || width.value <= 0.0) {
            return -1.0;
        }
        return width.value;
    }

    /**
     * {@link #resolveCellBasisWidth}와 동일한 목적으로, row 안 셀들의 top/height 분포로 row의 세로
     * footprint(px)를 구한다({@link #buildTableRowStyle}과 로직 공유). 계산 불가면 -1.
     */
    public double resolveRowBasisHeight(List<Element> row) {
        if (row == null || row.isEmpty()) {
            return -1.0;
        }
        double minTop = Double.MAX_VALUE;
        double maxBottom = -Double.MAX_VALUE;
        for (Element cell : row) {
            Geometry g = resolveGeometry(cell);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (top == null || height == null) {
                return -1.0;
            }
            minTop = Math.min(minTop, top.value);
            maxBottom = Math.max(maxBottom, top.value + height.value);
        }
        double rowHeight = maxBottom - minTop;
        return rowHeight > 0.0 ? rowHeight : -1.0;
    }

    /**
     * 자식 geometry만으로 table topology 판정: 동일 top 좌표(exact equality)만 같은 row로 묶는다.
     * geometry 미확정/자식 없음은 UNRESOLVED_LAYOUT, 겹침은 ABSOLUTE_LAYOUT_FALLBACK, 그 외 TABLE_LAYOUT_HIGH_CONFIDENCE.
     */
    public String classifyLayoutGeometry(List<Element> children) {
        List<CellGeometry> cells = resolveCellGeometries(children);
        if (cells == null || cells.isEmpty()) {
            return "UNRESOLVED_LAYOUT";
        }
        if (hasOverlap(cells)) {
            return "ABSOLUTE_LAYOUT_FALLBACK";
        }
        return "TABLE_LAYOUT_HIGH_CONFIDENCE";
    }

    /**
     * {@link #classifyLayoutGeometry}가 {@code TABLE_LAYOUT_HIGH_CONFIDENCE}를 반환한 경우에만
     * 호출한다. top 오름차순으로 정렬된 row 목록을, 각 row는 left 오름차순으로 정렬된 셀 목록으로
     * 반환한다.
     */
    public List<List<Element>> buildTableRows(List<Element> children) {
        List<CellGeometry> cells = resolveCellGeometries(children);
        if (cells == null) {
            return new ArrayList<List<Element>>();
        }
        Map<Double, List<CellGeometry>> byTop = groupByTop(cells);
        List<Double> tops = new ArrayList<Double>(byTop.keySet());
        java.util.Collections.sort(tops);

        List<List<Element>> rows = new ArrayList<List<Element>>();
        for (Double top : tops) {
            List<CellGeometry> row = byTop.get(top);
            java.util.Collections.sort(row, new java.util.Comparator<CellGeometry>() {
                public int compare(CellGeometry a, CellGeometry b) {
                    return Double.compare(a.left, b.left);
                }
            });
            List<Element> rowElements = new ArrayList<Element>();
            for (CellGeometry c : row) {
                rowElements.add(c.element);
            }
            rows.add(rowElements);
        }
        return rows;
    }

    /**
     * SPLIT_LAYOUT 판정: 형제가 부모 폭을 exact ratio 어휘로 정확히 분할하는지 {@link BigDecimal}
     * 정수 연산(epsilon 없음)으로 확인한다. GEOMETRY_INVALID/SPLIT_LAYOUT_RATIO_EXACT_MATCH/
     * FIXED_WIDTH_FALLBACK(비정확, tolerance 미보정이라 보수 처리) 중 하나를 반환한다.
     */
    public String classifyColumnRatioGeometry(List<Element> children) {
        List<ExactCellGeometry> row = resolveExactSingleRowPartition(children);
        if (row == null) {
            return "GEOMETRY_INVALID";
        }
        return exactColumnRatioLabelsOf(row) != null ? "SPLIT_LAYOUT_RATIO_EXACT_MATCH" : "FIXED_WIDTH_FALLBACK";
    }

    /**
     * {@link #classifyColumnRatioGeometry}가 SPLIT_LAYOUT_RATIO_EXACT_MATCH를 반환한 경우에만
     * 의미 있는 값을 반환한다(그 외 null). left 오름차순, 각 원소는 catalog decorative_class
     * 라벨(col_N 또는 col_33 x3).
     */
    public String[] resolveExactColumnRatios(List<Element> children) {
        List<ExactCellGeometry> row = resolveExactSingleRowPartition(children);
        return row == null ? null : exactColumnRatioLabelsOf(row);
    }

    /**
     * children이 동일 top/height의 2개 이상 형제이고 gap/overlap 없이 부모 폭을 연속 분할할 때만
     * 정렬된 목록을 반환(epsilon 없이 {@link BigDecimal#compareTo}). width가 0 이하인 cell이 있으면
     * DOM 삽입 순서가 암묵적 tie-breaker가 되므로 즉시 null(fail-closed).
     */
    private List<ExactCellGeometry> resolveExactSingleRowPartition(List<Element> children) {
        List<ExactCellGeometry> cells = resolveExactCellGeometries(children);
        if (cells == null || cells.size() < 2 || hasExactOverlap(cells) || hasNonPositiveWidth(cells)) {
            return null;
        }
        List<ExactCellGeometry> sorted = new ArrayList<ExactCellGeometry>(cells);
        java.util.Collections.sort(sorted, new java.util.Comparator<ExactCellGeometry>() {
            public int compare(ExactCellGeometry a, ExactCellGeometry b) {
                return a.left.compareTo(b.left);
            }
        });
        BigDecimal top = sorted.get(0).top;
        BigDecimal height = sorted.get(0).height;
        BigDecimal expectedLeft = sorted.get(0).left;
        for (ExactCellGeometry c : sorted) {
            if (c.top.compareTo(top) != 0
                    || c.height.compareTo(height) != 0
                    || c.left.compareTo(expectedLeft) != 0) {
                return null;
            }
            expectedLeft = c.left.add(c.width);
        }
        return sorted;
    }

    /**
     * 정렬된 partition이 exact ratio 어휘와 일치하면 그 라벨 배열, 아니면 null. 정확한 3등분
     * (col_33 x3)을 col_N보다 먼저 검사한다 -- 1/3은 N/10 형태로 표현 불가능하므로(설계상 col_33은
     * col_N의 예외 케이스) 서로 겹치지 않는다.
     */
    private String[] exactColumnRatioLabelsOf(List<ExactCellGeometry> sortedRow) {
        String[] thirds = exactThirdsLabelsOf(sortedRow);
        if (thirds != null) {
            return thirds;
        }
        return exactColNLabelsOf(sortedRow);
    }

    /** 정확히 3개의 형제이고 폭이 서로 완전히 동일하면(=정확한 3등분) col_33 x3, 아니면 null. */
    private String[] exactThirdsLabelsOf(List<ExactCellGeometry> sortedRow) {
        if (sortedRow.size() != 3) {
            return null;
        }
        BigDecimal w0 = sortedRow.get(0).width;
        BigDecimal w1 = sortedRow.get(1).width;
        BigDecimal w2 = sortedRow.get(2).width;
        if (w0.compareTo(w1) == 0 && w1.compareTo(w2) == 0) {
            return new String[] {"col_33", "col_33", "col_33"};
        }
        return null;
    }

    /**
     * 각 셀 폭이 col_N 정수(1..9)와 일치하는지 정수 교차곱셈으로만 판정한다(division 없음,
     * 근접값 승격 없음). 합이 정확히 10이어야 하며, 매치 실패 시 전체 null.
     */
    private String[] exactColNLabelsOf(List<ExactCellGeometry> sortedRow) {
        BigDecimal totalWidth = BigDecimal.ZERO;
        for (ExactCellGeometry c : sortedRow) {
            totalWidth = totalWidth.add(c.width);
        }
        if (totalWidth.signum() <= 0) {
            return null;
        }
        String[] labels = new String[sortedRow.size()];
        int sum = 0;
        for (int i = 0; i < sortedRow.size(); i++) {
            BigDecimal width = sortedRow.get(i).width;
            int matchedN = -1;
            for (int candidate = 1; candidate <= 9; candidate++) {
                if (width.multiply(BigDecimal.TEN).compareTo(totalWidth.multiply(BigDecimal.valueOf(candidate))) == 0) {
                    matchedN = candidate;
                    break;
                }
            }
            if (matchedN < 0) {
                return null;
            }
            labels[i] = "col_" + matchedN;
            sum += matchedN;
        }
        return sum == 10 ? labels : null;
    }

    /**
     * children이 비어있거나 left/top/width/height를 원본 length 문자열로부터 정확히(exact
     * decimal) 읽을 수 없으면 null. {@link #resolveCellGeometries}(double 기반, 기존
     * classifyLayoutGeometry/buildTableRows 전용)와는 완전히 별개의 경로다.
     */
    private List<ExactCellGeometry> resolveExactCellGeometries(List<Element> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        List<ExactCellGeometry> cells = new ArrayList<ExactCellGeometry>();
        for (Element child : children) {
            Geometry g = resolveGeometry(child);
            BigDecimal left = exactLength(g.left);
            BigDecimal top = exactLength(g.top);
            BigDecimal width = exactLength(g.width);
            BigDecimal height = exactLength(g.height);
            if (left == null || top == null || width == null || height == null) {
                return null;
            }
            cells.add(new ExactCellGeometry(left, top, width, height));
        }
        return cells;
    }

    /** 정규화된 length 문자열(예: "300px", "300")의 숫자 부분을 {@link BigDecimal}로 정확히 파싱한다. */
    private BigDecimal exactLength(String normalized) {
        if (isEmpty(normalized)) {
            return null;
        }
        Matcher matcher = NUMBER_WITH_UNIT.matcher(normalized.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** width가 0 이하인 cell이 하나라도 있으면 true (epsilon 없이 {@link BigDecimal#signum()}). */
    private boolean hasNonPositiveWidth(List<ExactCellGeometry> cells) {
        for (ExactCellGeometry c : cells) {
            if (c.width.signum() <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExactOverlap(List<ExactCellGeometry> cells) {
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                ExactCellGeometry a = cells.get(i);
                ExactCellGeometry b = cells.get(j);
                boolean xOverlap = a.left.compareTo(b.left.add(b.width)) < 0
                        && b.left.compareTo(a.left.add(a.width)) < 0;
                boolean yOverlap = a.top.compareTo(b.top.add(b.height)) < 0
                        && b.top.compareTo(a.top.add(a.height)) < 0;
                if (xOverlap && yOverlap) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@link #classifyColumnRatioGeometry}/{@link #resolveExactColumnRatios} 전용, BigDecimal
     * 기반 cell. element 자체는 라벨 판정에 쓰이지 않으므로 저장하지 않는다.
     */
    private static final class ExactCellGeometry {
        private final BigDecimal left;
        private final BigDecimal top;
        private final BigDecimal width;
        private final BigDecimal height;

        private ExactCellGeometry(BigDecimal left, BigDecimal top, BigDecimal width, BigDecimal height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * children이 비어있거나 left/top/width/height를 확정적으로 읽을 수 없으면 null. child 수
     * 자체는 fallback 사유로 쓰지 않는다(1-row/소수 child Div도 table 변환 대상, 14번 규칙).
     */
    private List<CellGeometry> resolveCellGeometries(List<Element> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        List<CellGeometry> cells = new ArrayList<CellGeometry>();
        for (Element child : children) {
            Geometry g = resolveGeometry(child);
            ParsedLength left = isEmpty(g.left) ? null : parseLength(g.left);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength width = isEmpty(g.width) ? null : parseLength(g.width);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (left == null || top == null || width == null || height == null) {
                return null;
            }
            cells.add(new CellGeometry(child, left.value, top.value, width.value, height.value));
        }
        return cells;
    }

    private Map<Double, List<CellGeometry>> groupByTop(List<CellGeometry> cells) {
        Map<Double, List<CellGeometry>> map = new LinkedHashMap<Double, List<CellGeometry>>();
        for (CellGeometry c : cells) {
            List<CellGeometry> row = map.get(c.top);
            if (row == null) {
                row = new ArrayList<CellGeometry>();
                map.put(c.top, row);
            }
            row.add(c);
        }
        return map;
    }

    private boolean hasOverlap(List<CellGeometry> cells) {
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                CellGeometry a = cells.get(i);
                CellGeometry b = cells.get(j);
                boolean xOverlap = a.left < b.left + b.width && b.left < a.left + a.width;
                boolean yOverlap = a.top < b.top + b.height && b.top < a.top + a.height;
                if (xOverlap && yOverlap) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class CellGeometry {
        private final Element element;
        private final double left;
        private final double top;
        private final double width;
        private final double height;

        private CellGeometry(Element element, double left, double top, double width, double height) {
            this.element = element;
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * grp_resultArea의 style을 생성한다. width:100%는 percentage 자식의 containing block을 위한 구조적 상수(화면별 계산값 아님).
     * 유효한 양수 height가 있으면 추가로 반환한다(position/overflow는 emit하지 않음).
     */
    public String buildMainAreaStyle(Document source) {
        StringBuilder style = new StringBuilder();
        style.append("width:").append(formatPercent(100.0)).append(";");

        Geometry geometry = findFormGeometry(source);
        if (geometry == null || isEmpty(geometry.height)) {
            return style.toString();
        }
        ParsedLength parsed = parseLength(geometry.height);
        if (parsed == null || parsed.value <= 0.0) {
            return style.toString();
        }

        appendCssLength(style, "height", geometry.height);
        return style.toString();
    }

    /**
     * grp_main의 style을 생성한다. height는 Form 선언값이 아니라 실제 authored content extent를 사용하고,
     * position:relative를 선언해 grp_main 자신이 absolute 자식들의 containing block이 되게 한다.
     */
    public String buildMainContentAreaStyle(Document source) {
        double contentHeight = resolveContentExtentHeight(source);
        if (contentHeight <= 0.0) {
            return "position:relative;" + buildMainAreaStyle(source);
        }
        StringBuilder style = new StringBuilder();
        style.append("position:relative;");
        style.append("width:").append(formatPercent(100.0)).append(";");
        style.append("height:").append(formatNumber(contentHeight)).append("px;");
        return style.toString();
    }

    /**
     * source Form의 최상위 Layout 직계 자식들의 content extent(px)를 계산한다: max(child.top +
     * child.height). 최상위 Layout을 찾을 수 없으면 -1.
     */
    public double resolveContentExtentHeight(Document source) {
        if (source == null) {
            return -1.0;
        }
        Element layout = findFirstElement(source, "Layout");
        if (layout == null) {
            return -1.0;
        }
        List<Element> children = new ArrayList<Element>();
        NodeList nodeList = layout.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                children.add((Element) node);
            }
        }
        return resolveContentExtentHeight(children);
    }

    /**
     * {@link #resolveContentExtentHeight(Document)}와 동일 계산을 이미 확보한 children 목록으로
     * 바로 수행한다(중복 탐색 방지). top/height를 읽을 수 없는 자식은 제외하며, 대상이 없으면 -1.
     */
    public double resolveContentExtentHeight(List<Element> children) {
        if (children == null || children.isEmpty()) {
            return -1.0;
        }
        double maxBottom = -1.0;
        for (Element child : children) {
            Geometry g = resolveGeometry(child);
            ParsedLength top = isEmpty(g.top) ? null : parseLength(g.top);
            ParsedLength height = isEmpty(g.height) ? null : parseLength(g.height);
            if (top == null || height == null) {
                continue;
            }
            double bottom = top.value + height.value;
            if (bottom > maxBottom) {
                maxBottom = bottom;
            }
        }
        return maxBottom;
    }

    /** 어떤 XPlatform 위치 속성을 사용했는지 진단용 문자열로 반환한다. */
    public String describeLayoutSource(Element source) {
        if (source == null) {
            return "";
        }
        String positionType = trim(source.getAttribute("positiontype"));
        String position2 = trim(source.getAttribute("position2"));
        String position = trim(source.getAttribute("position"));

        if (position2.length() > 0
                && ("position2".equalsIgnoreCase(positionType)
                || position.length() == 0)) {
            return "position2=" + position2;
        }
        if (position.length() > 0) {
            return "position=" + position;
        }
        if (hasExplicitGeometry(source)) {
            return "left/top/width/height";
        }
        return "";
    }

    /** 음수/지원하지 않는 단위/역전 좌표 때문에 크기를 안전하게 계산할 수 없는지 확인한다. */
    public boolean hasInvalidSize(Element source) {
        if (source == null) {
            return false;
        }

        String width = trim(source.getAttribute("width"));
        String height = trim(source.getAttribute("height"));
        if ((width.length() > 0 && normalizeNonNegativeLength(width) == null)
                || (height.length() > 0 && normalizeNonNegativeLength(height) == null)) {
            return true;
        }

        String position = trim(source.getAttribute("position"));
        if (position.length() > 0) {
            Geometry geometry = parsePosition(position);
            if (geometry == null) {
                return true;
            }
            if ((!isEmpty(geometry.left) && !isEmpty(geometry.right) && isEmpty(geometry.width))
                    || (!isEmpty(geometry.top) && !isEmpty(geometry.bottom) && isEmpty(geometry.height))) {
                return true;
            }
        }

        String position2 = trim(source.getAttribute("position2"));
        if (position2.length() > 0) {
            String[] parts = position2.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                int colon = part.indexOf(':');
                if (colon <= 0 || colon >= part.length() - 1) {
                    continue;
                }
                String key = part.substring(0, colon).toLowerCase();
                String value = part.substring(colon + 1);
                if (("w".equals(key) || "h".equals(key))
                        && normalizeNonNegativeLength(value) == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasUnsupportedRelativeLayout(Element source) {
        if (source == null) {
            return false;
        }
        String position2 = trim(source.getAttribute("position2"));
        if (position2.length() == 0) {
            return false;
        }

        String[] parts = position2.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int colon = part.indexOf(':');
            if (colon <= 0 || colon == part.length() - 1) {
                continue;
            }
            String key = part.substring(0, colon).toLowerCase();
            if (!("l".equals(key) || "t".equals(key) || "r".equals(key)
                    || "b".equals(key) || "w".equals(key) || "h".equals(key))) {
                continue;
            }
            String value = part.substring(colon + 1);
            if (!isSupportedLength(value)) {
                return true;
            }
        }
        return false;
    }

    private Geometry findFormGeometry(Document source) {
        if (source == null) {
            return null;
        }

        Element formElement = findFirstElement(source, "Form");
        if (formElement != null) {
            Geometry form = resolveGeometry(formElement);
            if (!isEmpty(form.width) || !isEmpty(form.height)) {
                return form;
            }
        }

        Element layout = findFirstElement(source, "Layout");
        if (layout != null) {
            Geometry geometry = new Geometry();
            geometry.width = normalizeNonNegativeLength(layout.getAttribute("width"));
            geometry.height = normalizeNonNegativeLength(layout.getAttribute("height"));
            if (!isEmpty(geometry.width) || !isEmpty(geometry.height)) {
                return geometry;
            }
        }

        return null;
    }

    private Element findFirstElement(Document source, String tagName) {
        NodeList elements = source.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Node node = elements.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            String localName = element.getLocalName();
            String actual = localName != null && localName.length() > 0
                    ? localName : element.getTagName();
            int colon = actual.indexOf(':');
            if (colon >= 0) {
                actual = actual.substring(colon + 1);
            }
            if (tagName.equals(actual)) {
                return element;
            }
        }
        return null;
    }

    private Geometry resolveGeometry(Element source) {
        Geometry geometry = new Geometry();
        if (source == null) {
            return geometry;
        }

        // 일부 XFDL은 left/top/width/height를 직접 가지고 있으므로 먼저 읽는다.
        geometry.left = normalizeLength(source.getAttribute("left"));
        geometry.top = normalizeLength(source.getAttribute("top"));
        geometry.width = normalizeNonNegativeLength(source.getAttribute("width"));
        geometry.height = normalizeNonNegativeLength(source.getAttribute("height"));
        geometry.right = normalizeLength(source.getAttribute("right"));
        geometry.bottom = normalizeLength(source.getAttribute("bottom"));

        String positionType = trim(source.getAttribute("positiontype"));
        String position2 = trim(source.getAttribute("position2"));
        String position = trim(source.getAttribute("position"));

        Geometry parsed = null;
        if (position2.length() > 0
                && ("position2".equalsIgnoreCase(positionType)
                || position.length() == 0)) {
            parsed = parsePosition2(position2);
        }
        if (parsed == null && position.length() > 0) {
            parsed = parsePosition(position);
        }

        if (parsed != null) {
            // position/position2 값이 있으면 XPlatform 원본 위치 정의를 우선한다.
            geometry.mergeFrom(parsed);
        }

        return geometry;
    }

    private Geometry parsePosition(String value) {
        String[] parts = trim(value).split("\\s+");
        if (parts.length < 5 || !"absolute".equalsIgnoreCase(parts[0])) {
            return null;
        }

        String left = normalizeLength(parts[1]);
        String top = normalizeLength(parts[2]);
        String right = normalizeLength(parts[3]);
        String bottom = normalizeLength(parts[4]);

        Geometry geometry = new Geometry();
        geometry.left = left;
        geometry.top = top;
        geometry.right = right;
        geometry.bottom = bottom;

        geometry.width = subtractLengths(right, left);
        geometry.height = subtractLengths(bottom, top);
        return geometry;
    }

    private Geometry parsePosition2(String value) {
        String[] parts = trim(value).split("\\s+");
        Map<String, String> values = new LinkedHashMap<String, String>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if ("absolute".equalsIgnoreCase(part)) {
                continue;
            }
            int colon = part.indexOf(':');
            if (colon <= 0 || colon >= part.length() - 1) {
                continue;
            }
            String key = part.substring(0, colon).toLowerCase();
            String raw = part.substring(colon + 1);
            if (("l".equals(key) || "t".equals(key) || "r".equals(key)
                    || "b".equals(key) || "w".equals(key) || "h".equals(key))
                    && isSupportedLength(raw)) {
                values.put(key, normalizeLength(raw));
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        Geometry geometry = new Geometry();
        geometry.left = values.get("l");
        geometry.top = values.get("t");
        geometry.right = values.get("r");
        geometry.bottom = values.get("b");
        geometry.width = normalizeNonNegativeLength(values.get("w"));
        geometry.height = normalizeNonNegativeLength(values.get("h"));

        if (isEmpty(geometry.width)) {
            geometry.width = subtractLengths(geometry.right, geometry.left);
        }
        if (isEmpty(geometry.height)) {
            geometry.height = subtractLengths(geometry.bottom, geometry.top);
        }
        return geometry;
    }

    private void appendVisualStyle(Element source, StringBuilder style) {
        if ("false".equalsIgnoreCase(trim(source.getAttribute("visible")))) {
            style.append("display:none;");
        }

        String color = trim(source.getAttribute("color"));
        if (color.length() > 0) {
            style.append("color:").append(color).append(';');
        }

        String background = trim(source.getAttribute("background"));
        if (background.length() > 0) {
            style.append("background:").append(background).append(';');
        }

        String cursor = trim(source.getAttribute("cursor")).toLowerCase();
        if (isSafeCursor(cursor)) {
            style.append("cursor:").append(cursor).append(';');
        }

        String opacity = trim(source.getAttribute("opacity"));
        if (opacity.length() > 0 && NUMBER.matcher(opacity).matches()) {
            double value = Double.parseDouble(opacity);
            if (value >= 0.0 && value <= 100.0) {
                if (value > 1.0) {
                    value = value / 100.0;
                }
                style.append("opacity:").append(formatNumber(value)).append(';');
            }
        }

        appendAlignment(source.getAttribute("align"), style);
        appendPadding(source.getAttribute("padding"), style);
        appendSourceInlineVisualStyle(source, style);
    }

    /**
     * source의 raw {@code style} 속성 중 {@link #SAFE_SOURCE_STYLE_PROPERTIES} 화이트리스트에
     * 있는 순수 visual property만 병합한다. geometry/구조 property는 배제해 기존 geometry 결과를 덮어쓰지 않는다.
     */
    private void appendSourceInlineVisualStyle(Element source, StringBuilder style) {
        String raw = trim(source.getAttribute("style"));
        if (raw.length() == 0) {
            return;
        }
        String[] declarations = raw.split(";");
        for (int i = 0; i < declarations.length; i++) {
            String decl = declarations[i].trim();
            if (decl.length() == 0) {
                continue;
            }
            int colon = decl.indexOf(':');
            if (colon <= 0 || colon >= decl.length() - 1) {
                continue;
            }
            String property = decl.substring(0, colon).trim().toLowerCase();
            String value = decl.substring(colon + 1).trim();
            if (value.length() == 0 || !SAFE_SOURCE_STYLE_PROPERTIES.contains(property)) {
                continue;
            }
            style.append(property).append(':').append(value).append(';');
        }
    }

    private boolean isSafeCursor(String value) {
        return "auto".equals(value) || "default".equals(value) || "pointer".equals(value)
                || "text".equals(value) || "wait".equals(value) || "help".equals(value)
                || "move".equals(value) || "crosshair".equals(value) || "not-allowed".equals(value);
    }

    private void appendAlignment(String align, StringBuilder style) {
        String value = trim(align).toLowerCase();
        if (value.length() == 0) {
            return;
        }
        String[] parts = value.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if ("left".equals(part) || "center".equals(part) || "right".equals(part)
                    || "justify".equals(part)) {
                style.append("text-align:").append(part).append(';');
            } else if ("top".equals(part) || "middle".equals(part) || "bottom".equals(part)) {
                String css = "middle".equals(part) ? "middle" : part;
                style.append("vertical-align:").append(css).append(';');
            }
        }
    }

    private void appendPadding(String padding, StringBuilder style) {
        String value = trim(padding);
        if (value.length() == 0) {
            return;
        }
        String[] parts = value.split("[ ,]+", -1);
        if (parts.length < 1 || parts.length > 4) {
            return;
        }
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = normalizeNonNegativeLength(parts[i]);
            if (p == null || p.length() == 0) {
                return;
            }
            if (i > 0) {
                css.append(' ');
            }
            css.append(toCssLength(p));
        }
        style.append("padding:").append(css).append(';');
    }

    private void appendCssLength(StringBuilder style, String name, String value) {
        if (isEmpty(value)) {
            return;
        }
        style.append(name).append(':').append(toCssLength(value)).append(';');
    }

    private String toCssLength(String value) {
        String v = trim(value);
        if (v.length() == 0) {
            return v;
        }
        if (v.endsWith("%") || v.toLowerCase().endsWith("px")
                || "auto".equalsIgnoreCase(v)) {
            return v;
        }
        if (NUMBER.matcher(v).matches()) {
            return v + "px";
        }
        return v;
    }

    private String normalizeLength(String value) {
        String v = trim(value);
        if (v.length() == 0) {
            return null;
        }
        if (isSupportedLength(v)) {
            return v;
        }
        return null;
    }

    private String normalizeNonNegativeLength(String value) {
        String normalized = normalizeLength(value);
        if (normalized == null || "auto".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        ParsedLength parsed = parseLength(normalized);
        return parsed != null && parsed.value >= 0.0 ? normalized : null;
    }

    private boolean isSupportedLength(String value) {
        String v = trim(value);
        if (v.length() == 0 || "auto".equalsIgnoreCase(v)) {
            return v.length() > 0;
        }
        return NUMBER_WITH_UNIT.matcher(v).matches();
    }

    private String subtractLengths(String end, String start) {
        if (isEmpty(end) || isEmpty(start)) {
            return null;
        }

        ParsedLength endValue = parseLength(end);
        ParsedLength startValue = parseLength(start);
        if (endValue == null || startValue == null) {
            return null;
        }
        if (!endValue.unit.equals(startValue.unit)) {
            return null;
        }

        double difference = endValue.value - startValue.value;
        if (difference < 0.0) {
            return null;
        }
        return formatNumber(difference) + endValue.unit;
    }

    private ParsedLength parseLength(String value) {
        Matcher matcher = NUMBER_WITH_UNIT.matcher(trim(value));
        if (!matcher.matches()) {
            return null;
        }
        String unit = matcher.group(2);
        if (unit == null) {
            unit = "";
        }
        return new ParsedLength(Double.parseDouble(matcher.group(1)), unit);
    }

    private boolean hasExplicitGeometry(Element source) {
        return trim(source.getAttribute("left")).length() > 0
                || trim(source.getAttribute("top")).length() > 0
                || trim(source.getAttribute("width")).length() > 0
                || trim(source.getAttribute("height")).length() > 0;
    }

    private String formatNumber(double value) {
        long integer = (long) value;
        if (value == integer) {
            return String.valueOf(integer);
        }
        String result = String.valueOf(value);
        while (result.indexOf('.') >= 0 && result.endsWith("0")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ParsedLength {
        private final double value;
        private final String unit;

        private ParsedLength(double value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    private static final class Geometry {
        private String left;
        private String top;
        private String right;
        private String bottom;
        private String width;
        private String height;

        private boolean hasAnyPositionOrSize() {
            return left != null || top != null || width != null || height != null;
        }

        private void mergeFrom(Geometry other) {
            if (other == null) {
                return;
            }
            if (other.left != null) left = other.left;
            if (other.top != null) top = other.top;
            if (other.right != null) right = other.right;
            if (other.bottom != null) bottom = other.bottom;
            if (other.width != null) width = other.width;
            if (other.height != null) height = other.height;
        }
    }
}
