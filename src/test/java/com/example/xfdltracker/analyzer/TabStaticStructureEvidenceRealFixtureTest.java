package com.example.xfdltracker.analyzer;

import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.semantic.SemanticRegionResult;
import com.example.xfdltracker.semantic.StaticTabPageEntry;
import com.example.xfdltracker.semantic.TabControlStaticStructureEvidence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;

/**
 * Slice 101H -- 실제 tracked corpus fixture에서 typed static structure evidence가 exact 값을
 * 보존함을 증명한다. fixture는 읽기만 하고 수정하지 않으며, pipeline wiring은 하지 않는다.
 */
public class TabStaticStructureEvidenceRealFixtureTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testTabAsyncRapidSetUrlRealFixtureExactEvidence();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testTabAsyncRapidSetUrlRealFixtureExactEvidence() throws Exception {
        File root = repositoryRoot();
        File realFixture = new File(root, "sample-phase3-project/Form/TabAsyncRapidSetUrl.xfdl");
        assertTrue("real-fixture: TabAsyncRapidSetUrl.xfdl exists in tracked corpus", realFixture.isFile());

        Document doc = new XfdlReader().read(realFixture);
        Element formRoot = doc.getDocumentElement();
        List<SemanticRegionResult> results = new SemanticRegionSegmenter().segment(formRoot);

        SemanticRegionResult tabControl = null;
        for (SemanticRegionResult r : results) {
            if ("TAB_CONTROL".equals(r.getRecommendedTemplateFamily())) {
                tabControl = r;
                break;
            }
        }
        assertTrue("real-fixture: TAB_CONTROL region found", tabControl != null);

        TabControlStaticStructureEvidence evidence = tabControl.getTabControlStaticStructureEvidence();
        assertTrue("real-fixture: typed evidence non-null", evidence != null);
        assertEquals("real-fixture: tabControlSourceId", "tabMain", evidence.getTabControlSourceId());
        assertEquals("real-fixture: orderedStaticPages size",
                "1", String.valueOf(evidence.getOrderedStaticPages().size()));
        StaticTabPageEntry entry = evidence.getOrderedStaticPages().get(0);
        assertEquals("real-fixture: page tabPageSourceId", "pageA", entry.getTabPageSourceId());
        assertEquals("real-fixture: page pageOrdinal", "0", String.valueOf(entry.getPageOrdinal()));
    }

    private static File repositoryRoot() {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "build.bat").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException(
                "tab_static_structure_evidence_real_fixture_test: could not locate sanctioned working-copy root");
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("[FAIL] " + label + " -- expected=" + expected + " actual=" + actual);
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + label);
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
