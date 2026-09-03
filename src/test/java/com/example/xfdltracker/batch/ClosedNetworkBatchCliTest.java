package com.example.xfdltracker.batch;

import com.example.xfdltracker.runtime.CommonRuntimeCapabilityCatalog;
import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** JUnit 없는 의존성 없는 {@link ClosedNetworkBatchCli}/batch package 단위+통합 테스트. */
public class ClosedNetworkBatchCliTest {

    private static int failures = 0;
    private static boolean junctionOutputInRootAliasCliTestExecuted = false;

    private static final String EDIT_XFDL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<FDL version=\"1.5\">\n"
                    + "  <Form id=\"EditForm\" width=\"400\" height=\"300\">\n"
                    + "    <Div id=\"table1\">\n"
                    + "      <Static id=\"lbl1\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"Name\" />\n"
                    + "      <Edit id=\"edt1\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                    + "    </Div>\n"
                    + "  </Form>\n"
                    + "</FDL>\n";

    private static final String MSG_REQUIRING_XFDL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<FDL version=\"1.5\">\n"
                    + "  <Form id=\"MsgForm\" width=\"400\" height=\"300\">\n"
                    + "    <Div id=\"table1\">\n"
                    + "      <Static id=\"lbl1\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"Name\" />\n"
                    + "      <Edit id=\"edt1\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                    + "    </Div>\n"
                    + "  </Form>\n"
                    + "  <Script><![CDATA[function f(){ uc.msg(\"hi\"); }]]></Script>\n"
                    + "</FDL>\n";

    private static final String CHECKBOX_UNBOUND_XFDL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<FDL version=\"1.5\">\n"
                    + "  <Form id=\"CheckBoxUnboundForm\" width=\"400\" height=\"300\">\n"
                    + "    <Div id=\"table1\">\n"
                    + "      <Static id=\"lblUse\" left=\"0\" top=\"0\" width=\"50\" height=\"20\" text=\"Use\" />\n"
                    + "      <CheckBox id=\"chkUse\" left=\"60\" top=\"0\" width=\"100\" height=\"20\" />\n"
                    + "    </Div>\n"
                    + "  </Form>\n"
                    + "</FDL>\n";

    public static void main(String[] args) throws Exception {
        testArgumentCountValidation();
        testMissingInputRoot();
        testOutputRootIsExistingFile();
        testInputOutputRootIdentical();
        testOutputRootNestedUnderInputRootRejected();
        testInputRootNestedUnderOutputRootRejected();
        testZeroXfdlInputRejected();
        testUnknownCapabilityIdFailsClosed();
        testExplicitEmptyProfileParsedAsEmpty();
        testExplicitProfileCommentsAndBlankLinesIgnored();
        testDeterministicDiscoveryAndRelativePathAndExtensionMapping();
        testOutputPathCollisionFailsBeforePublishing();
        testInputRequiringUnavailableCapabilityFails();
        testMultipleInputSuccess();
        testOneLaterInputFailureProducesNonzeroAndNoPartialXmlForFailedFile();
        testPreexistingLaterOutputBlocksAllConversionBeforeItStarts();
        testJunctionOutputInRootAliasBlocksAllConversion();
        testCheckBoxFixtureInBatchFailsWithAcceptedReason();
        testScreenFileNamesDoNotAlterBehavior();
        testBatchPackageNeverReferencesLegacyConverter();

        System.out.println("BATCH_REAL_JUNCTION_OUTPUT_IN_ROOT_ALIAS_CLI_TEST_EXECUTED="
                + junctionOutputInRootAliasCliTestExecuted);

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- 인자/root 검증 ----

    private static void testArgumentCountValidation() {
        Capture c = runCli(new String[] {"onlyOneArg"});
        assertEquals("arg-count: exit code", 2, c.exitCode);
        assertTrue("arg-count: usage printed", c.text.contains("usage:"));
    }

    private static void testMissingInputRoot() throws Exception {
        File missing = new File(tempDir(), "does-not-exist");
        File outputRoot = tempDir();
        File profile = writeProfile("");
        Capture c = runCli(new String[] {missing.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("missing-input-root: exit code", 2, c.exitCode);
        assertTrue("missing-input-root: reason", c.text.contains("batch_input_root_not_directory"));
    }

    private static void testOutputRootIsExistingFile() throws Exception {
        File inputRoot = tempDir();
        File outputAsFile = new File(tempDir(), "not-a-dir.txt");
        Files.write(outputAsFile.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        File profile = writeProfile("");
        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputAsFile.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("output-root-is-file: exit code", 2, c.exitCode);
        assertTrue("output-root-is-file: reason", c.text.contains("batch_output_root_not_directory"));
    }

    private static void testInputOutputRootIdentical() throws Exception {
        File shared = tempDir();
        File profile = writeProfile("");
        Capture c = runCli(new String[] {shared.getAbsolutePath(), shared.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("identical-roots: exit code", 2, c.exitCode);
        assertTrue("identical-roots: reason", c.text.contains("batch_input_output_root_identical"));
    }

    private static void testOutputRootNestedUnderInputRootRejected() throws Exception {
        File inputRoot = tempDir();
        File outputRoot = new File(inputRoot, "nested-output");
        File profile = writeProfile("");
        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("output-nested-under-input: exit code", 2, c.exitCode);
        assertTrue("output-nested-under-input: reason", c.text.contains("batch_output_root_descendant_of_input_root"));
    }

    private static void testInputRootNestedUnderOutputRootRejected() throws Exception {
        File outputRoot = tempDir();
        File inputRoot = new File(outputRoot, "nested-input");
        inputRoot.mkdirs();
        File profile = writeProfile("");
        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("input-nested-under-output: exit code", 2, c.exitCode);
        assertTrue("input-nested-under-output: reason", c.text.contains("batch_input_root_descendant_of_output_root"));
    }

    private static void testZeroXfdlInputRejected() throws Exception {
        File inputRoot = tempDir();
        File outputRoot = tempDir();
        File profile = writeProfile("");
        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("zero-input: exit code", 2, c.exitCode);
        assertTrue("zero-input: reason", c.text.contains("batch_zero_input_rejected"));
    }

    // ---- runtime-profile 계약 ----

    private static void testUnknownCapabilityIdFailsClosed() throws Exception {
        File profile = writeProfile("NOT_A_REAL_CAPABILITY\n");
        boolean threw = false;
        String message = null;
        try {
            BatchRuntimeProfileLoader.load(profile, CommonRuntimeCapabilityCatalog.createSeeded());
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("unknown-capability: fails closed", threw);
        assertTrue("unknown-capability: reason names the bad id",
                message != null && message.contains("batch_runtime_profile_unknown_capability_id")
                        && message.contains("NOT_A_REAL_CAPABILITY"));
    }

    private static void testExplicitEmptyProfileParsedAsEmpty() throws Exception {
        File profile = writeProfile("");
        TargetRuntimeProfile p = BatchRuntimeProfileLoader.load(profile, CommonRuntimeCapabilityCatalog.createSeeded());
        assertTrue("explicit-empty-profile: no capability available", !p.isAvailable("MESSAGE_DIALOG"));
        assertTrue("explicit-empty-profile: capability id set is empty", p.getAvailableCapabilityIds().isEmpty());
    }

    private static void testExplicitProfileCommentsAndBlankLinesIgnored() throws Exception {
        File profile = writeProfile("# comment line\n\n  MESSAGE_DIALOG  \n\n# another comment\nSCREEN_OPEN\n");
        TargetRuntimeProfile p = BatchRuntimeProfileLoader.load(profile, CommonRuntimeCapabilityCatalog.createSeeded());
        assertTrue("profile-comments: MESSAGE_DIALOG available", p.isAvailable("MESSAGE_DIALOG"));
        assertTrue("profile-comments: SCREEN_OPEN available", p.isAvailable("SCREEN_OPEN"));
        assertEquals("profile-comments: exactly 2 ids parsed", 2, p.getAvailableCapabilityIds().size());
    }

    // ---- 탐색/상대경로/확장자 매핑/충돌 검증 ----

    private static void testDeterministicDiscoveryAndRelativePathAndExtensionMapping() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "Zeta/A.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "Alpha/B.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "Alpha/B.notxfdl"), "not a source file");
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("discovery: exit code", 0, c.exitCode);
        assertTrue("discovery: exactly 2 xfdl inputs counted", c.text.contains("[BATCH_INPUT_COUNT] 2"));
        assertTrue("discovery: deterministic order Alpha before Zeta",
                c.text.indexOf("Alpha/B.xfdl") < c.text.indexOf("Zeta/A.xfdl"));
        assertTrue("discovery: relative path preserved for output",
                new File(outputRoot, "Alpha/B.xml").isFile());
        assertTrue("discovery: relative path preserved for output",
                new File(outputRoot, "Zeta/A.xml").isFile());
        assertTrue("discovery: non-.xfdl file not converted",
                !new File(outputRoot, "Alpha/B.notxfdl.xml").exists());
    }

    private static void testOutputPathCollisionFailsBeforePublishing() throws Exception {
        // 단일 root 재귀 discovery는 이 충돌을 구조적으로 만들 수 없으므로(서로 다른 경로는 suffix
        // 제거 후에도 서로 다름), planner를 직접 duplicate 목록으로 호출해 defense-in-depth 경로를
        // 증명한다(중복 discovery 결과를 넘기는 미래 caller/버그를 가정).
        File inputRoot = tempDir();
        File dup = new File(inputRoot, "Same.xfdl");
        writeFile(dup, EDIT_XFDL);
        File outputRoot = tempDir();

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPlanner.plan(inputRoot, outputRoot, java.util.Arrays.asList(dup, dup));
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("collision: fails before publishing", threw);
        assertTrue("collision: explicit reason", message != null && message.contains("batch_output_path_collision"));
    }

    // ---- runtime capability 게이팅 + 부분 실패 semantics 검증 ----

    /**
     * runtime capability lane(A)이 general behavior lane(B)보다 먼저 실행되므로, capability가
     * 없으면 lane A가 즉시 fail-closed한다. capability를 줘도 lane B가 여전히 {@code uc.*}를
     * 거부하는 것은 이 batch와 무관한 기존 accepted 아키텍처 사실이다(테스트 대상 아님).
     */
    private static void testInputRequiringUnavailableCapabilityFails() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "Msg.xfdl"), MSG_REQUIRING_XFDL);
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("unavailable-capability: exit code", 1, c.exitCode);
        assertTrue("unavailable-capability: reason mentions MESSAGE_DIALOG",
                c.text.contains("MESSAGE_DIALOG") && c.text.contains("NOT_AVAILABLE_IN_TARGET_RUNTIME_PROFILE"));
        assertTrue("unavailable-capability: no partial output published", !new File(outputRoot, "Msg.xml").exists());
    }

    private static void testMultipleInputSuccess() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "One.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "Two.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "Three.xfdl"), EDIT_XFDL);
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("multi-success: exit code", 0, c.exitCode);
        assertTrue("multi-success: result line", c.text.contains("[BATCH_RESULT_PASS] succeeded=3"));
        assertTrue("multi-success: One.xml", new File(outputRoot, "One.xml").isFile());
        assertTrue("multi-success: Two.xml", new File(outputRoot, "Two.xml").isFile());
        assertTrue("multi-success: Three.xml", new File(outputRoot, "Three.xml").isFile());
    }

    private static void testOneLaterInputFailureProducesNonzeroAndNoPartialXmlForFailedFile() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "A_ok.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "B_fail.xfdl"), CHECKBOX_UNBOUND_XFDL);
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("later-failure: exit code", 1, c.exitCode);
        assertTrue("later-failure: earlier success reported", c.text.contains("[BATCH_OUTPUT_OK] A_ok.xfdl"));
        assertTrue("later-failure: failed file reported", c.text.contains("[BATCH_FAILED_FILE] B_fail.xfdl"));
        assertTrue("later-failure: earlier output preserved on disk", new File(outputRoot, "A_ok.xml").isFile());
        assertTrue("later-failure: no partial output for failed file", !new File(outputRoot, "B_fail.xml").exists());
        assertTrue("later-failure: result line reports partial completion",
                c.text.contains("[BATCH_RESULT_FAIL] succeeded=1 failed=1 skipped=0"));
    }

    /**
     * 결정적 discovery 순서상 먼저 오는 A_ok.xfdl이 존재하더라도, 나중에 오는 B_conflict.xfdl의
     * output이 이미 존재하면 preflight가 모든 계획을 미리 검사하므로 A_ok조차 변환이 시작되지
     * 않아야 한다(conversion 호출 수 0 증명, 기존 파일 bytes 불변 증명).
     */
    private static void testPreexistingLaterOutputBlocksAllConversionBeforeItStarts() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "A_ok.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "B_conflict.xfdl"), EDIT_XFDL);
        File outputRoot = tempDir();
        File preexisting = new File(outputRoot, "B_conflict.xml");
        String originalBytes = "PRE_EXISTING_BYTES_MUST_NOT_CHANGE";
        writeFile(preexisting, originalBytes);
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("preexisting-later-output: exit code", 2, c.exitCode);
        assertTrue("preexisting-later-output: reason",
                c.text.contains("batch_preexisting_output_rejected") && c.text.contains("B_conflict.xml"));
        assertTrue("preexisting-later-output: earlier input never converted",
                !new File(outputRoot, "A_ok.xml").exists());
        String afterBytes = new String(Files.readAllBytes(preexisting.toPath()), StandardCharsets.UTF_8);
        assertEquals("preexisting-later-output: existing bytes untouched", originalBytes, afterBytes);
    }

    /**
     * outputRoot/sub가 outputRoot/realSub로의 junction이면 outputRoot/sub/A.xml의 최종 real path는
     * 여전히 outputRoot 안이지만, 원래 성공했을 known-good input(A.xfdl)조차 변환이 시작되지 않고
     * A.xml이 어디에도(alias 경로/실제 경로 어느 쪽에도) 생성되지 않아야 한다(black-box 증명).
     */
    private static void testJunctionOutputInRootAliasBlocksAllConversion() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "sub/A.xfdl"), EDIT_XFDL);
        File outputRoot = tempDir();
        File realSub = new File(outputRoot, "realSub");
        realSub.mkdirs();
        File aliasSub = new File(outputRoot, "sub");
        if (!tryCreateJunction(aliasSub, realSub)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] CLI-level output intermediate-alias test -- "
                    + "mklink /J unavailable or failed in this environment");
            return;
        }
        junctionOutputInRootAliasCliTestExecuted = true;
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("junction-output-in-root-alias: exit code", 2, c.exitCode);
        assertTrue("junction-output-in-root-alias: reason",
                c.text.contains("batch_output_intermediate_alias_rejected"));
        assertTrue("junction-output-in-root-alias: no output via alias path",
                !new File(aliasSub, "A.xml").exists());
        assertTrue("junction-output-in-root-alias: no output via real path either",
                !new File(realSub, "A.xml").exists());
    }

    private static boolean tryCreateJunction(File link, File target) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            byte[] tmp = new byte[4096];
            while (p.getInputStream().read(tmp) != -1) {
                continue;
            }
            int rc = p.waitFor();
            return rc == 0 && link.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static void testCheckBoxFixtureInBatchFailsWithAcceptedReason() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "OnlyCheckBox.xfdl"), CHECKBOX_UNBOUND_XFDL);
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("checkbox-in-batch: exit code", 1, c.exitCode);
        assertTrue("checkbox-in-batch: uses the already-accepted CheckBox reason (not weakened)",
                c.text.contains("checkbox_unbound_rendering_equivalence_not_proven"));
        assertTrue("checkbox-in-batch: no partial output", !new File(outputRoot, "OnlyCheckBox.xml").exists());
    }

    private static void testScreenFileNamesDoNotAlterBehavior() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "화면_1.xfdl"), EDIT_XFDL);
        writeFile(new File(inputRoot, "weird name with spaces.xfdl"), EDIT_XFDL);
        File outputRoot = tempDir();
        File profile = writeProfile("");

        Capture c = runCli(new String[] {inputRoot.getAbsolutePath(), outputRoot.getAbsolutePath(), profile.getAbsolutePath()});
        assertEquals("filename-agnostic: exit code", 0, c.exitCode);
        assertTrue("filename-agnostic: result", c.text.contains("[BATCH_RESULT_PASS] succeeded=2"));
    }

    // ---- legacy isolation 정적 소스 검사 ----

    private static void testBatchPackageNeverReferencesLegacyConverter() throws Exception {
        File batchSrcDir = new File("src/main/java/com/example/xfdltracker/batch");
        assertTrue("legacy-isolation: batch source directory exists", batchSrcDir.isDirectory());
        File[] files = batchSrcDir.listFiles();
        assertTrue("legacy-isolation: batch source files found", files != null && files.length > 0);
        for (File f : files) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue("legacy-isolation: " + f.getName() + " does not reference WebSquareGenerator",
                    !content.contains("WebSquareGenerator"));
            assertTrue("legacy-isolation: " + f.getName() + " does not reference XPlatformProjectConverter",
                    !content.contains("XPlatformProjectConverter"));
        }
    }

    // ---- fixture 도우미 ----

    private static File tempDir() throws Exception {
        return Files.createTempDirectory("closed-network-batch-cli-test").toFile();
    }

    private static File writeProfile(String content) throws Exception {
        File f = new File(tempDir(), "profile.txt");
        writeFile(f, content);
        return f;
    }

    private static void writeFile(File f, String content) throws Exception {
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class Capture {
        final int exitCode;
        final String text;
        Capture(int exitCode, String text) {
            this.exitCode = exitCode;
            this.text = text;
        }
    }

    private static Capture runCli(String[] args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream captured = new PrintStream(buffer);
        int exitCode = ClosedNetworkBatchCli.run(args, captured);
        captured.flush();
        return new Capture(exitCode, new String(buffer.toByteArray(), StandardCharsets.UTF_8));
    }

    // ---- assertion 도우미 ----

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + message);
        } else {
            System.out.println("[PASS] " + message);
        }
    }

    private static void assertEquals(String message, int expected, int actual) {
        assertTrue(message + " expected=" + expected + " actual=" + actual, expected == actual);
    }

    private static void assertEquals(String message, String expected, String actual) {
        assertTrue(message + " expected=" + expected + " actual=" + actual,
                expected == null ? actual == null : expected.equals(actual));
    }
}
