package com.example.xfdltracker.behavior;

import com.example.xfdltracker.analyzer.SemanticRegionSegmenter;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.SourcePayloadEvidenceItem;
import com.example.xfdltracker.semantic.StaticTabPageEntry;
import com.example.xfdltracker.semantic.TabControlStaticStructureEvidence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 외부 의존성 없는(no JUnit) offline 단위 테스트다. {@code TabStaticReceiverContextBuilder}/
 * {@code TabStaticReceiverResolutionContext}의 hierarchical resolution/ambiguity/immutability를
 * 검증한다(production wiring 없음, 실제 SemanticRegionSegmenter 출력만 소비).
 */
public class TabStaticReceiverContextBuilderTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSingleExactPairResolves();
        testUnknownTabControlIsMissing();
        testUnknownTabPageIsMissing();
        testNullTabControlIsMissing();
        testNullTabPageIsMissing();
        testBlankTabControlIsMissing();
        testBlankTabPageIsMissing();
        testWhitespaceOnlyTabControlQueryIsMissing();
        testWhitespaceOnlyTabPageQueryIsMissing();
        testWhitespaceOnlyRawEvidenceWithWhitespaceQueryIsMissing();
        testDuplicateTabControlExactIdIsAmbiguous();
        testDuplicateTabControlExactIdQueriedPageOnlyInOneStillAmbiguous();
        testDuplicatePageIdWithinSingleTabControlIsAmbiguous();
        testCaseMismatchIsMissing();
        testWhitespaceMismatchIsMissing();
        testDifferentTabControlsResolveIndependently();
        testNonTabControlRegionIgnoredByBuilder();
        testTabControlRegionWithNullTypedEvidenceFailsClosed();
        testBuilderIgnoresDiagnosticStringPollution();
        testContextResolutionUnaffectedByMutatingInputRegionsListAfterBuild();
        testOrderedStaticPagesGetterRejectsModification();
        testTabControlStaticStructureEvidenceImmutableAgainstConstructorInputMutation();
        testTabControlStaticStructureEvidenceRejectsWhitespaceOnlyStructuralId();
        testStaticTabPageEntryRejectsWhitespaceOnlyStructuralId();
        testTabStaticReceiverResolutionResolvedRejectsWhitespaceOnlyStructuralId();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testSingleExactPairResolves() throws Exception {
        List<SemanticRegionResult> regions = tabControlRegion("tabMain", "pageA");
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabMain", "pageA");
        assertEquals("single-exact-pair: status", "RESOLVED", String.valueOf(result.getStatus()));
        assertEquals("single-exact-pair: pageOrdinal", "0", String.valueOf(result.getPageOrdinal()));
        assertTrue("single-exact-pair: tabControlStructuralId non-blank",
                result.getTabControlStructuralId() != null && result.getTabControlStructuralId().length() > 0);
    }

    private static void testUnknownTabControlIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabOther", "pageA");
        assertEquals("unknown-tabcontrol: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testUnknownTabPageIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabMain", "pageZ");
        assertEquals("unknown-tabpage: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testNullTabControlIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage(null, "pageA");
        assertEquals("null-tabcontrol: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testNullTabPageIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabMain", null);
        assertEquals("null-tabpage: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testBlankTabControlIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("", "pageA");
        assertEquals("blank-tabcontrol: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testBlankTabPageIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabMain", "");
        assertEquals("blank-tabpage: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testWhitespaceOnlyTabControlQueryIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage(" ", "pageA");
        assertEquals("whitespace-only-tabcontrol-query: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testWhitespaceOnlyTabPageQueryIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabMain", " ");
        assertEquals("whitespace-only-tabpage-query: status", "MISSING", String.valueOf(result.getStatus()));
    }

    /** v2 defect 재현: raw evidence 자신이 whitespace-only source id를 losslessly 보존하고 있어도
     *  같은 whitespace-only 문자열로 query하면 RESOLVED가 아니라 MISSING이어야 한다. */
    private static void testWhitespaceOnlyRawEvidenceWithWhitespaceQueryIsMissing() throws Exception {
        List<SemanticRegionResult> regions = tabControlRegion(" ", " ");
        assertEquals("whitespace-raw-evidence: tabControlSourceId preserved raw", " ",
                regions.get(0).getTabControlStaticStructureEvidence().getTabControlSourceId());
        assertEquals("whitespace-raw-evidence: tabPageSourceId preserved raw", " ",
                regions.get(0).getTabControlStaticStructureEvidence().getOrderedStaticPages().get(0)
                        .getTabPageSourceId());

        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);
        TabStaticReceiverResolution result = context.resolveStaticTabPage(" ", " ");
        assertEquals("whitespace-raw-evidence-plus-whitespace-query: status", "MISSING",
                String.valueOf(result.getStatus()));
    }

    private static void testDuplicateTabControlExactIdIsAmbiguous() throws Exception {
        List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.addAll(tabControlRegion("dupTab", "pageA"));
        regions.addAll(tabControlRegion("dupTab", "pageB"));
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        TabStaticReceiverResolution result = context.resolveStaticTabPage("dupTab", "pageA");
        assertEquals("duplicate-tabcontrol: status", "AMBIGUOUS", String.valueOf(result.getStatus()));
    }

    /** 두 TabControl이 같은 id를 갖고, queried page가 한쪽에만 존재해도 AMBIGUOUS여야 한다(first-match 금지). */
    private static void testDuplicateTabControlExactIdQueriedPageOnlyInOneStillAmbiguous() throws Exception {
        List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.addAll(tabControlRegion("dupTab2", "onlyInFirst"));
        regions.addAll(tabControlRegion("dupTab2", "onlyInSecond"));
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        TabStaticReceiverResolution result = context.resolveStaticTabPage("dupTab2", "onlyInFirst");
        assertEquals("duplicate-tabcontrol-asymmetric-page: status", "AMBIGUOUS", String.valueOf(result.getStatus()));
    }

    private static void testDuplicatePageIdWithinSingleTabControlIsAmbiguous() throws Exception {
        List<SemanticRegionResult> regions = tabControlRegion("tabDupPage", "same", "same");
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabDupPage", "same");
        assertEquals("duplicate-page-in-tabcontrol: status", "AMBIGUOUS", String.valueOf(result.getStatus()));
    }

    private static void testCaseMismatchIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("TabMain", "PageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabmain", "pagea");
        assertEquals("case-mismatch: status", "MISSING", String.valueOf(result.getStatus()));
    }

    private static void testWhitespaceMismatchIsMissing() throws Exception {
        TabStaticReceiverResolutionContext context =
                new TabStaticReceiverContextBuilder().build(tabControlRegion("tabMain", "pageA"));
        TabStaticReceiverResolution result = context.resolveStaticTabPage(" tabMain ", " pageA ");
        assertEquals("whitespace-mismatch: status", "MISSING", String.valueOf(result.getStatus()));
    }

    /** 두 TabControl을 같은 document의 sibling으로 둬(실제 conversion과 동일하게 한 번만 segment) 서로
     *  다른 structuralId를 갖게 한다 -- 별개 document의 root Tab은 둘 다 "Tab[0]"라 우연히 같아진다. */
    private static void testDifferentTabControlsResolveIndependently() throws Exception {
        Document doc = newDocument();
        Element container = doc.createElement("Div");
        container.setAttribute("id", "container");
        Element tabOne = doc.createElement("Tab");
        tabOne.setAttribute("id", "tabOne");
        appendPages(doc, tabOne, "pageOneA", "pageOneB");
        Element tabTwo = doc.createElement("Tab");
        tabTwo.setAttribute("id", "tabTwo");
        appendPages(doc, tabTwo, "pageTwoA");
        container.appendChild(tabOne);
        container.appendChild(tabTwo);
        doc.appendChild(container);

        List<SemanticRegionResult> regions = new SemanticRegionSegmenter().segment(container);
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        TabStaticReceiverResolution first = context.resolveStaticTabPage("tabOne", "pageOneB");
        TabStaticReceiverResolution second = context.resolveStaticTabPage("tabTwo", "pageTwoA");
        assertEquals("independent-resolve: tabOne.pageOneB status", "RESOLVED", String.valueOf(first.getStatus()));
        assertEquals("independent-resolve: tabOne.pageOneB pageOrdinal", "1", String.valueOf(first.getPageOrdinal()));
        assertEquals("independent-resolve: tabTwo.pageTwoA status", "RESOLVED", String.valueOf(second.getStatus()));
        assertEquals("independent-resolve: tabTwo.pageTwoA pageOrdinal", "0",
                String.valueOf(second.getPageOrdinal()));
        assertTrue("independent-resolve: different TabControls have different structuralId",
                !first.getTabControlStructuralId().equals(second.getTabControlStructuralId()));
    }

    /** family가 TAB_CONTROL이 아닌 region은(설령 evidence가 붙어 있어도) builder가 소비하지 않는다. */
    private static void testNonTabControlRegionIgnoredByBuilder() throws Exception {
        List<SemanticRegionResult> gridRegions = new ArrayList<SemanticRegionResult>();
        Document doc = newDocument();
        Element grid = doc.createElement("Grid");
        grid.setAttribute("id", "grdX");
        doc.appendChild(grid);
        gridRegions.addAll(new SemanticRegionSegmenter().segment(grid));

        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(gridRegions);
        TabStaticReceiverResolution result = context.resolveStaticTabPage("grdX", "anything");
        assertEquals("non-tabcontrol-ignored: status", "MISSING", String.valueOf(result.getStatus()));
    }

    /** family가 TAB_CONTROL인데 typed evidence가 null이면 architecture invariant violation으로 fail-closed. */
    private static void testTabControlRegionWithNullTypedEvidenceFailsClosed() throws Exception {
        SemanticRegionResult malformed = new SemanticRegionResult();
        malformed.setRecommendedTemplateFamily("TAB_CONTROL");
        malformed.setSourceStructuralId("Tab[0]");
        List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>();
        regions.add(malformed);

        boolean threw = false;
        try {
            new TabStaticReceiverContextBuilder().build(regions);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("null-typed-evidence-fail-closed: builder throws IllegalStateException", threw);
    }

    /** componentEvidence/parameters/payloadEvidence에 오염된 문자열이 있어도 resolver는 typed evidence만 따른다. */
    private static void testBuilderIgnoresDiagnosticStringPollution() throws Exception {
        List<SemanticRegionResult> regions = tabControlRegion("tabClean", "pageClean");
        SemanticRegionResult region = regions.get(0);
        region.getComponentEvidence().add("source_id=tabPolluted");
        region.getParameters().put("tab_count", Integer.valueOf(999));
        region.getPayloadEvidence().add(new SourcePayloadEvidenceItem(
                region.getSourceStructuralId(), region.getSourceStructuralId(), "tab_label",
                "source_text_attribute", "pagePolluted", 0));

        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);
        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabClean", "pageClean");
        assertEquals("diagnostic-pollution-ignored: still resolves via typed evidence",
                "RESOLVED", String.valueOf(result.getStatus()));
        TabStaticReceiverResolution pollutedLookup = context.resolveStaticTabPage("tabPolluted", "pagePolluted");
        assertEquals("diagnostic-pollution-ignored: polluted string keys do not resolve",
                "MISSING", String.valueOf(pollutedLookup.getStatus()));
    }

    /** build() 이후 입력 regions list를 mutate해도 context의 resolution 결과는 바뀌지 않는다. */
    private static void testContextResolutionUnaffectedByMutatingInputRegionsListAfterBuild() throws Exception {
        List<SemanticRegionResult> regions = new ArrayList<SemanticRegionResult>(tabControlRegion("tabFix", "pageX"));
        TabStaticReceiverResolutionContext context = new TabStaticReceiverContextBuilder().build(regions);

        regions.clear();
        regions.addAll(tabControlRegion("tabOther", "pageY"));

        TabStaticReceiverResolution result = context.resolveStaticTabPage("tabFix", "pageX");
        assertEquals("post-build-mutation-unaffected: original pair still resolves",
                "RESOLVED", String.valueOf(result.getStatus()));
    }

    private static void testOrderedStaticPagesGetterRejectsModification() throws Exception {
        List<SemanticRegionResult> regions = tabControlRegion("tabImmutable", "pageX");
        boolean threw = false;
        try {
            regions.get(0).getTabControlStaticStructureEvidence().getOrderedStaticPages().clear();
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue("ordered-pages-unmodifiable: clear() throws UnsupportedOperationException", threw);
    }

    /** 생성 후 constructor에 넘긴 원본 mutable list를 바꿔도 evidence 내부 상태는 영향받지 않는다. */
    private static void testTabControlStaticStructureEvidenceImmutableAgainstConstructorInputMutation()
            throws Exception {
        List<StaticTabPageEntry> mutableInput = new ArrayList<StaticTabPageEntry>();
        mutableInput.add(new StaticTabPageEntry("p0", "Tab[0]/Tabpage[0]", 0));
        TabControlStaticStructureEvidence evidence =
                new TabControlStaticStructureEvidence("tabX", "Tab[0]", mutableInput);

        mutableInput.add(new StaticTabPageEntry("p1", "Tab[0]/Tabpage[1]", 1));
        assertEquals("evidence-immutable-against-ctor-input: size unaffected by later mutation of original list",
                "1", String.valueOf(evidence.getOrderedStaticPages().size()));
    }

    /** whitespace-only tabControlStructuralId는 blank invariant 위반으로 거부돼야 한다. */
    private static void testTabControlStaticStructureEvidenceRejectsWhitespaceOnlyStructuralId() throws Exception {
        boolean threw = false;
        try {
            new TabControlStaticStructureEvidence("t", " ", new ArrayList<StaticTabPageEntry>());
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("whitespace-only-tabcontrol-structuralid: constructor throws IllegalArgumentException", threw);
    }

    /** whitespace-only tabPageStructuralId는 blank invariant 위반으로 거부돼야 한다. */
    private static void testStaticTabPageEntryRejectsWhitespaceOnlyStructuralId() throws Exception {
        boolean threw = false;
        try {
            new StaticTabPageEntry("p", " ", 0);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("whitespace-only-tabpage-structuralid: constructor throws IllegalArgumentException", threw);
    }

    /** resolved(...)의 whitespace-only tabControlStructuralId도 동일하게 거부돼야 한다. */
    private static void testTabStaticReceiverResolutionResolvedRejectsWhitespaceOnlyStructuralId() throws Exception {
        boolean threw = false;
        try {
            TabStaticReceiverResolution.resolved(" ", 0);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("whitespace-only-resolution-structuralid: resolved(...) throws IllegalArgumentException", threw);
    }

    private static List<SemanticRegionResult> tabControlRegion(String tabId, String... pageIds) throws Exception {
        Document doc = newDocument();
        Element tab = doc.createElement("Tab");
        tab.setAttribute("id", tabId);
        appendPages(doc, tab, pageIds);
        doc.appendChild(tab);
        return new SemanticRegionSegmenter().segment(tab);
    }

    private static void appendPages(Document doc, Element tab, String... pageIds) {
        for (String pageId : pageIds) {
            Element page = doc.createElement("Tabpage");
            page.setAttribute("id", pageId);
            tab.appendChild(page);
        }
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
