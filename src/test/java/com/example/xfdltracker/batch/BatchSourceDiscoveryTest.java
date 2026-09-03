package com.example.xfdltracker.batch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * JUnit 없는 의존성 없는 {@link BatchSourceDiscovery} 단위 테스트. real-path 경계/심볼릭 링크
 * 거부/alias 순환 감지 로직을 순수 로직 테스트와, 이 machine에서 실제로 만들 수 있는 real NTFS
 * junction/symlink를 이용한 filesystem 레벨 회귀 테스트로 함께 검증한다.
 */
public class BatchSourceDiscoveryTest {

    private static int failures = 0;
    private static boolean realSymlinkTestExecuted = false;
    private static String realSymlinkSkipReason = null;
    private static boolean junctionEscapeTestExecuted = false;
    private static boolean junctionCycleTestExecuted = false;
    private static boolean junctionInRootAliasTestExecuted = false;

    public static void main(String[] args) throws Exception {
        testDiscoverFindsXfdlRecursivelyDeterministicOrder();
        testDiscoverIgnoresNonXfdlFiles();
        testDiscoverRejectsNonDirectoryInputRoot();
        testRelativePathWithinRootSucceeds();
        testRelativePathOutsideRootFailsClosed();
        testRealSymlinkXfdlInputRejected();
        testRealSymlinkNonXfdlEntryAlsoRejected();
        testJunctionInputRootEscapeFailsClosed();
        testJunctionCycleFailsClosed();
        testJunctionInRootAliasFailsClosed();

        System.out.println("BATCH_REAL_SYMLINK_TEST_EXECUTED=" + realSymlinkTestExecuted
                + (realSymlinkSkipReason == null ? "" : " reason=" + realSymlinkSkipReason));
        System.out.println("BATCH_REAL_JUNCTION_ESCAPE_TEST_EXECUTED=" + junctionEscapeTestExecuted);
        System.out.println("BATCH_REAL_JUNCTION_CYCLE_TEST_EXECUTED=" + junctionCycleTestExecuted);
        System.out.println("BATCH_REAL_JUNCTION_IN_ROOT_ALIAS_TEST_EXECUTED=" + junctionInRootAliasTestExecuted);

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- 순수 discovery/relativePath 로직 ----

    private static void testDiscoverFindsXfdlRecursivelyDeterministicOrder() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "Zeta/A.xfdl"), "z");
        writeFile(new File(inputRoot, "Alpha/B.xfdl"), "b");
        List<File> found = BatchSourceDiscovery.discover(inputRoot);
        assertEquals("discover: count", 2, found.size());
        assertTrue("discover: Alpha before Zeta",
                BatchSourceDiscovery.relativePath(inputRoot, found.get(0)).startsWith("Alpha"));
        assertTrue("discover: second is Zeta",
                BatchSourceDiscovery.relativePath(inputRoot, found.get(1)).startsWith("Zeta"));
    }

    private static void testDiscoverIgnoresNonXfdlFiles() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "Keep.xfdl"), "k");
        writeFile(new File(inputRoot, "Ignore.notxfdl"), "i");
        writeFile(new File(inputRoot, "Ignore.XFDL"), "case-sensitive-check");
        List<File> found = BatchSourceDiscovery.discover(inputRoot);
        assertEquals("ignore-non-xfdl: exact-case .xfdl only", 1, found.size());
        assertTrue("ignore-non-xfdl: kept file is Keep.xfdl",
                BatchSourceDiscovery.relativePath(inputRoot, found.get(0)).equals("Keep.xfdl"));
    }

    private static void testDiscoverRejectsNonDirectoryInputRoot() throws Exception {
        File notADir = new File(tempDir(), "file.txt");
        writeFile(notADir, "x");
        boolean threw = false;
        try {
            BatchSourceDiscovery.discover(notADir);
        } catch (IllegalArgumentException e) {
            threw = true;
            assertTrue("reject-non-directory: reason", e.getMessage().contains("batch_input_root_not_directory"));
        }
        assertTrue("reject-non-directory: fails closed", threw);
    }

    private static void testRelativePathWithinRootSucceeds() throws Exception {
        File inputRoot = tempDir();
        File file = new File(inputRoot, "Sub/Leaf.xfdl");
        writeFile(file, "x");
        String relative = BatchSourceDiscovery.relativePath(inputRoot, file);
        assertEquals("relativePath-within: value", "Sub/Leaf.xfdl", relative);
    }

    private static void testRelativePathOutsideRootFailsClosed() throws Exception {
        File inputRoot = tempDir();
        File outside = new File(tempDir(), "Elsewhere.xfdl");
        writeFile(outside, "x");
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.relativePath(inputRoot, outside);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("relativePath-outside: fails closed", threw);
        assertTrue("relativePath-outside: reason", message != null && message.contains("batch_input_root_escape"));
    }

    // ---- 실제 filesystem alias(symlink/junction) 회귀 ----

    private static void testRealSymlinkXfdlInputRejected() throws Exception {
        File inputRoot = tempDir();
        File targetFile = new File(inputRoot, "Target.xfdl");
        writeFile(targetFile, "x");
        File linkFile = new File(inputRoot, "Link.xfdl");
        try {
            Files.createSymbolicLink(linkFile.toPath(), targetFile.toPath());
        } catch (Exception e) {
            realSymlinkTestExecuted = false;
            realSymlinkSkipReason = e.getClass().getSimpleName() + ":" + e.getMessage();
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] real symlink creation unavailable in this "
                    + "environment (no SeCreateSymbolicLinkPrivilege / Developer Mode) -- " + realSymlinkSkipReason);
            return;
        }
        realSymlinkTestExecuted = true;
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.discover(inputRoot);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("real-symlink: fails closed", threw);
        assertTrue("real-symlink: reason",
                message != null && message.contains("batch_input_symbolic_link_entry_rejected"));
    }

    /** 확장자가 {@code .xfdl}이 아닌 심볼릭 링크도 동일하게 거부되어야 한다(확장자 무관 증명). */
    private static void testRealSymlinkNonXfdlEntryAlsoRejected() throws Exception {
        File inputRoot = tempDir();
        File targetFile = new File(inputRoot, "Notes.txt");
        writeFile(targetFile, "x");
        File linkFile = new File(inputRoot, "Alias.txt");
        try {
            Files.createSymbolicLink(linkFile.toPath(), targetFile.toPath());
        } catch (Exception e) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] non-.xfdl real symlink creation unavailable -- "
                    + e.getClass().getSimpleName() + ":" + e.getMessage());
            return;
        }
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.discover(inputRoot);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("real-symlink-non-xfdl: fails closed despite non-.xfdl name", threw);
        assertTrue("real-symlink-non-xfdl: reason",
                message != null && message.contains("batch_input_symbolic_link_entry_rejected"));
    }

    private static void testJunctionInputRootEscapeFailsClosed() throws Exception {
        File inputRoot = tempDir();
        File outsideDir = tempDir();
        writeFile(new File(outsideDir, "Secret.xfdl"), "secret");
        File escapeLink = new File(inputRoot, "escape");
        if (!tryCreateJunction(escapeLink, outsideDir)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] junction escape test -- mklink /J unavailable "
                    + "or failed in this environment");
            return;
        }
        junctionEscapeTestExecuted = true;
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.discover(inputRoot);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("junction-escape: fails closed", threw);
        assertTrue("junction-escape: reason", message != null && message.contains("batch_input_root_escape"));
    }

    private static void testJunctionCycleFailsClosed() throws Exception {
        File inputRoot = tempDir();
        writeFile(new File(inputRoot, "Normal.xfdl"), "n");
        File loopLink = new File(inputRoot, "loop");
        if (!tryCreateJunction(loopLink, inputRoot)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] junction cycle test -- mklink /J unavailable "
                    + "or failed in this environment");
            return;
        }
        junctionCycleTestExecuted = true;
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.discover(inputRoot);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("junction-cycle: fails closed", threw);
        assertTrue("junction-cycle: reason",
                message != null && message.contains("batch_input_directory_alias_or_cycle_rejected"));
    }

    /**
     * inputRoot/real/A.xfdl와 inputRoot/alias(같은 real/로의 junction)는 조상으로 되돌아가는
     * 진짜 순환이 아니지만, 같은 real 디렉터리를 두 lexical 경로로 노출하므로 여전히 거부되어야
     * 하고 어느 쪽 경로도 "이겨서" 결과에 남으면 안 된다.
     */
    private static void testJunctionInRootAliasFailsClosed() throws Exception {
        File inputRoot = tempDir();
        File realDir = new File(inputRoot, "real");
        writeFile(new File(realDir, "A.xfdl"), "a");
        File aliasDir = new File(inputRoot, "alias");
        if (!tryCreateJunction(aliasDir, realDir)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] in-root junction alias test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        junctionInRootAliasTestExecuted = true;
        boolean threw = false;
        String message = null;
        try {
            BatchSourceDiscovery.discover(inputRoot);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("junction-in-root-alias: fails closed", threw);
        assertTrue("junction-in-root-alias: reason",
                message != null && message.contains("batch_input_directory_alias_or_cycle_rejected"));
    }

    private static boolean tryCreateJunction(File link, File target) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            drain(p.getInputStream());
            int rc = p.waitFor();
            return rc == 0 && link.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static void drain(InputStream in) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) != -1) {
            buf.write(tmp, 0, n);
        }
    }

    // ---- fixture/assertion 도우미 ----

    private static File tempDir() throws Exception {
        return Files.createTempDirectory("batch-source-discovery-test").toFile();
    }

    private static void writeFile(File f, String content) throws Exception {
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

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
