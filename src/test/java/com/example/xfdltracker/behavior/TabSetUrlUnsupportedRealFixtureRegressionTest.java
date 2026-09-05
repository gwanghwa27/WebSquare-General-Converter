package com.example.xfdltracker.behavior;

import com.example.xfdltracker.parser.XfdlReader;
import com.example.xfdltracker.pipeline.TargetPipelineConfig;
import com.example.xfdltracker.pipeline.TargetWebSquarePipeline;
import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import org.w3c.dom.Document;

import java.io.File;
import java.nio.file.Files;

/**
 * Slice 101H -- Phase A(typed evidence/context builder) 추가 후에도 실제 corpus의
 * {@code this.tabMain.pageA.set_url(...)}가 여전히 SourceScriptAnalyzer/pipeline 양쪽에서
 * 기존과 동일하게 fail-closed임을 증명한다(context는 어디에도 연결되지 않았다).
 */
public class TabSetUrlUnsupportedRealFixtureRegressionTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testSourceScriptAnalyzerStillRejectsThisReservedIdentifier();
        testPipelineStillFailsClosedOnRealFixture();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testSourceScriptAnalyzerStillRejectsThisReservedIdentifier() throws Exception {
        File root = repositoryRoot();
        File realFixture = new File(root, "sample-phase3-project/Form/TabAsyncRapidSetUrl.xfdl");
        assertTrue("regression: TabAsyncRapidSetUrl.xfdl exists in tracked corpus", realFixture.isFile());

        XfdlReader reader = new XfdlReader();
        Document doc = reader.read(realFixture);
        String script = reader.extractScript(doc);

        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(script);
        assertEquals("regression: analyze(String) status still UNSUPPORTED_SYNTAX",
                "UNSUPPORTED_SYNTAX", String.valueOf(result.getStatus()));
        assertTrue("regression: reason still names the reserved identifier this",
                result.getReason() != null && result.getReason().contains("this"));
    }

    private static void testPipelineStillFailsClosedOnRealFixture() throws Exception {
        File root = repositoryRoot();
        File realFixture = new File(root, "sample-phase3-project/Form/TabAsyncRapidSetUrl.xfdl");

        File tempDir = Files.createTempDirectory("tab-set-url-regression").toFile();
        File xfdlCopy = new File(tempDir, "TabAsyncRapidSetUrl.xfdl");
        Files.copy(realFixture.toPath(), xfdlCopy.toPath());
        File output = new File(tempDir, "output.xml");

        boolean threw = false;
        String reason = null;
        try {
            new TargetWebSquarePipeline()
                    .convert(xfdlCopy, output, new TargetPipelineConfig(TargetRuntimeProfile.empty()));
        } catch (IllegalStateException e) {
            threw = true;
            reason = e.getMessage();
        }
        assertTrue("regression: pipeline still fails closed", threw);
        assertTrue("regression: failure reason still cites unsupported reserved identifier this",
                reason != null && reason.contains("this"));
        assertTrue("regression: no partial/invalid target XML is ever published", !output.exists());
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
                "tab_set_url_unsupported_regression_test: could not locate sanctioned working-copy root");
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
