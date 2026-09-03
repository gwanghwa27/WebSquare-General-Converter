package com.example.xfdltracker.batch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 없는 의존성 없는 {@link BatchPathBoundary}/{@link BatchOutputPreflight} 단위 테스트.
 * root 겹침 판정, output real-path 경계, 기존 output 파일과의 충돌을 pipeline conversion 없이
 * 직접 검증한다(둘 다 package-private이므로 같은 패키지에서 직접 호출).
 */
public class BatchOutputPreflightTest {

    private static int failures = 0;
    private static boolean junctionOutputInRootAliasTestExecuted = false;
    private static boolean danglingJunctionPureLogicTestExecuted = false;
    private static boolean danglingJunctionPreflightTestExecuted = false;
    private static boolean outputSymlinkIntermediateTestExecuted = false;
    private static boolean nonRegularTargetEntryTestExecuted = false;
    private static boolean danglingTargetSymlinkTestExecuted = false;
    private static String danglingTargetSymlinkSkipReason = null;

    public static void main(String[] args) throws Exception {
        testRootOverlapEqualDetected();
        testRootOverlapOutputDescendantOfInputDetected();
        testRootOverlapInputDescendantOfOutputDetected();
        testRootNoOverlapForSiblings();
        testMissingTailResolvesUnderExistingAncestor();
        testFindFirstIntermediateAliasIssueNoAliasForPlainParent();
        testFindFirstIntermediateAliasIssueNoAliasForMissingParent();
        testFindFirstIntermediateAliasIssueDetectsJunctionAlias();
        testFindFirstIntermediateAliasIssueDetectsDanglingJunctionAsUnresolvable();
        testFindFirstIntermediateAliasIssueRejectsSymbolicLinkRegardlessOfTarget();
        testOutputPreflightRejectsPathEscapingOutputRootViaJunction();
        testOutputPreflightRejectsIntermediateAliasEvenWhenFinalTargetStaysInRoot();
        testOutputPreflightRejectsDanglingJunctionIntermediate();
        testOutputPreflightRejectsPreexistingTarget();
        testOutputPreflightRejectsPreexistingDirectoryTarget();
        testOutputPreflightRejectsNonRegularFinalTargetEntry();
        testOutputPreflightRejectsDanglingSymlinkFinalTarget();
        testOutputPreflightAllowsFreshNonColliding();

        System.out.println("BATCH_REAL_JUNCTION_OUTPUT_IN_ROOT_ALIAS_TEST_EXECUTED="
                + junctionOutputInRootAliasTestExecuted);
        System.out.println("BATCH_REAL_DANGLING_JUNCTION_TEST_EXECUTED="
                + (danglingJunctionPureLogicTestExecuted && danglingJunctionPreflightTestExecuted));
        System.out.println("BATCH_OUTPUT_SYMLINK_INTERMEDIATE_TEST_EXECUTED=" + outputSymlinkIntermediateTestExecuted);
        System.out.println("BATCH_REAL_NONREGULAR_TARGET_ENTRY_TEST_EXECUTED=" + nonRegularTargetEntryTestExecuted);
        System.out.println("BATCH_REAL_DANGLING_TARGET_SYMLINK_TEST_EXECUTED=" + danglingTargetSymlinkTestExecuted
                + (danglingTargetSymlinkSkipReason == null ? "" : " reason=" + danglingTargetSymlinkSkipReason));

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    // ---- root 겹침(overlap) 판정 ----

    private static void testRootOverlapEqualDetected() throws Exception {
        File shared = tempDir();
        Path a = BatchPathBoundary.resolveRealPathAllowingMissingTail(shared);
        Path b = BatchPathBoundary.resolveRealPathAllowingMissingTail(shared);
        assertTrue("root-overlap-equal: same real path", a.equals(b));
    }

    private static void testRootOverlapOutputDescendantOfInputDetected() throws Exception {
        File input = tempDir();
        File output = new File(input, "nested-output");
        Path realInput = BatchPathBoundary.resolveRealPathAllowingMissingTail(input);
        Path realOutput = BatchPathBoundary.resolveRealPathAllowingMissingTail(output);
        assertTrue("root-overlap-output-descendant: detected", realOutput.startsWith(realInput));
    }

    private static void testRootOverlapInputDescendantOfOutputDetected() throws Exception {
        File output = tempDir();
        File input = new File(output, "nested-input");
        input.mkdirs();
        Path realInput = BatchPathBoundary.resolveRealPathAllowingMissingTail(input);
        Path realOutput = BatchPathBoundary.resolveRealPathAllowingMissingTail(output);
        assertTrue("root-overlap-input-descendant: detected", realInput.startsWith(realOutput));
    }

    private static void testRootNoOverlapForSiblings() throws Exception {
        File a = tempDir();
        File b = tempDir();
        Path realA = BatchPathBoundary.resolveRealPathAllowingMissingTail(a);
        Path realB = BatchPathBoundary.resolveRealPathAllowingMissingTail(b);
        assertTrue("root-no-overlap-siblings: neither is equal", !realA.equals(realB));
        assertTrue("root-no-overlap-siblings: neither contains the other",
                !realA.startsWith(realB) && !realB.startsWith(realA));
    }

    private static void testMissingTailResolvesUnderExistingAncestor() throws Exception {
        File existingParent = tempDir();
        File notYetCreated = new File(existingParent, "a/b/c-output-root");
        Path realExistingParent = BatchPathBoundary.resolveRealPathAllowingMissingTail(existingParent);
        Path realMissingTail = BatchPathBoundary.resolveRealPathAllowingMissingTail(notYetCreated);
        assertTrue("missing-tail: resolves under existing ancestor", realMissingTail.startsWith(realExistingParent));
        assertEquals("missing-tail: exact tail preserved", "c-output-root",
                realMissingTail.getFileName().toString());
    }

    private static void testFindFirstIntermediateAliasIssueNoAliasForPlainParent() throws Exception {
        File root = tempDir();
        Path realRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(root);
        File plainSub = new File(root, "PlainSub");
        plainSub.mkdirs();
        BatchPathBoundary.IntermediateAliasFinding finding =
                BatchPathBoundary.findFirstIntermediateAliasIssue(realRoot, "PlainSub/Leaf.xml");
        assertTrue("plain-parent: NONE (no alias)",
                finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.NONE);
    }

    /** 아직 진짜로 없는 parent 구간은(NOFOLLOW 기준) NONE으로 통과해야 한다(디렉터리 생성 없음). */
    private static void testFindFirstIntermediateAliasIssueNoAliasForMissingParent() throws Exception {
        File root = tempDir();
        Path realRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(root);
        BatchPathBoundary.IntermediateAliasFinding finding =
                BatchPathBoundary.findFirstIntermediateAliasIssue(realRoot, "NotYetCreated/Leaf.xml");
        assertTrue("missing-parent: NONE (genuinely not yet created)",
                finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.NONE);
        assertTrue("missing-parent: preflight check itself must not create directories",
                !new File(root, "NotYetCreated").exists());
    }

    private static void testFindFirstIntermediateAliasIssueDetectsJunctionAlias() throws Exception {
        File root = tempDir();
        Path realRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(root);
        File realSub = new File(root, "RealSub");
        realSub.mkdirs();
        File aliasSub = new File(root, "AliasSub");
        if (!tryCreateJunction(aliasSub, realSub)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] junction-alias pure-logic test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        BatchPathBoundary.IntermediateAliasFinding finding =
                BatchPathBoundary.findFirstIntermediateAliasIssue(realRoot, "AliasSub/Leaf.xml");
        assertTrue("junction-alias: classified as ALIAS",
                finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.ALIAS);
        Path expectedRealSub = BatchPathBoundary.resolveRealPathAllowingMissingTail(realSub);
        assertTrue("junction-alias: resolves to the real target directory",
                finding.aliasRealPath != null && finding.aliasRealPath.equals(expectedRealSub));
    }

    /**
     * BATCH_UNRESOLVABLE_EXISTING_OUTPUT_COMPONENT_TEST -- junction 생성 후 target을 지우면 entry
     * 자체는 NOFOLLOW로 여전히 "존재"하지만(이 machine 실측: default exists=false, NOFOLLOW
     * exists=true, toRealPath는 NoSuchFileException) NONE(아직 없음)이 아니라 UNRESOLVABLE이어야 한다.
     */
    private static void testFindFirstIntermediateAliasIssueDetectsDanglingJunctionAsUnresolvable() throws Exception {
        File root = tempDir();
        Path realRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(root);
        File brokenLink = new File(root, "Broken");
        if (!tryCreateDanglingJunction(brokenLink)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] dangling-junction pure-logic test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        danglingJunctionPureLogicTestExecuted = true;
        BatchPathBoundary.IntermediateAliasFinding finding =
                BatchPathBoundary.findFirstIntermediateAliasIssue(realRoot, "Broken/Leaf.xml");
        assertTrue("dangling-junction: NOT classified as NONE",
                finding.kind != BatchPathBoundary.IntermediateAliasFinding.Kind.NONE);
        assertTrue("dangling-junction: classified as UNRESOLVABLE",
                finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.UNRESOLVABLE);
    }

    /** BATCH_OUTPUT_SYMBOLIC_LINK_INTERMEDIATE_ALWAYS_REJECTED -- 대상 해석과 무관하게 항상 거부. */
    private static void testFindFirstIntermediateAliasIssueRejectsSymbolicLinkRegardlessOfTarget() throws Exception {
        File root = tempDir();
        Path realRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(root);
        File target = new File(root, "RealDir");
        target.mkdirs();
        File linkDir = new File(root, "LinkDir");
        try {
            Files.createSymbolicLink(linkDir.toPath(), target.toPath());
        } catch (Exception e) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] output symlink-intermediate test -- "
                    + e.getClass().getSimpleName() + ":" + e.getMessage());
            return;
        }
        outputSymlinkIntermediateTestExecuted = true;
        BatchPathBoundary.IntermediateAliasFinding finding =
                BatchPathBoundary.findFirstIntermediateAliasIssue(realRoot, "LinkDir/Leaf.xml");
        assertTrue("output-symlink-intermediate: classified as SYMBOLIC_LINK",
                finding.kind == BatchPathBoundary.IntermediateAliasFinding.Kind.SYMBOLIC_LINK);
    }

    // ---- output preflight: real-path 경계 / 기존 파일 충돌 ----

    private static void testOutputPreflightRejectsPathEscapingOutputRootViaJunction() throws Exception {
        File outputRoot = tempDir();
        File outsideDir = tempDir();
        File aliasedSub = new File(outputRoot, "sub");
        if (!tryCreateJunction(aliasedSub, outsideDir)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] output-escape-via-junction test -- "
                    + "mklink /J unavailable or failed in this environment");
            return;
        }
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        File escapingTarget = new File(aliasedSub, "Result.xml");
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "Result.xfdl"), "Result.xfdl", escapingTarget, "sub/Result.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("output-escape-via-junction: fails closed", threw);
        assertTrue("output-escape-via-junction: reason", message != null && message.contains("batch_output_root_escape"));
    }

    /**
     * outputRoot/sub가 outputRoot/realSub로의 junction이면, outputRoot/sub/A.xml의 최종 real path는
     * 여전히 outputRoot 안이지만(REAL_PATH_CONTAINMENT_ALONE_IS_OUTPUT_ALIAS_SAFETY_AUTHORITY = FALSE),
     * publication이 alias 부모를 통과하므로 여전히 fail-closed해야 한다.
     */
    private static void testOutputPreflightRejectsIntermediateAliasEvenWhenFinalTargetStaysInRoot() throws Exception {
        File outputRoot = tempDir();
        File realSub = new File(outputRoot, "realSub");
        realSub.mkdirs();
        File aliasSub = new File(outputRoot, "sub");
        if (!tryCreateJunction(aliasSub, realSub)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] intermediate-alias-in-root test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        junctionOutputInRootAliasTestExecuted = true;
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        File plannedTarget = new File(aliasSub, "A.xml");
        Path finalRealTarget = BatchPathBoundary.resolveRealPathAllowingMissingTail(plannedTarget);
        assertTrue("intermediate-alias-in-root: precondition -- final real path IS still inside output root",
                finalRealTarget.startsWith(realOutputRoot));

        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "A.xfdl"), "sub/A.xfdl", plannedTarget, "sub/A.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("intermediate-alias-in-root: fails closed despite in-root final real path", threw);
        assertTrue("intermediate-alias-in-root: reason",
                message != null && message.contains("batch_output_intermediate_alias_rejected"));
        assertTrue("intermediate-alias-in-root: no output file materialized", !plannedTarget.exists());
        assertTrue("intermediate-alias-in-root: aliased real target also not materialized",
                !finalRealTarget.toFile().exists());
    }

    /**
     * Part J 회귀: outputRoot/broken이 이미 지워진 target을 가리키는 junction이면(dangling), 그
     * 부모가 "아직 없음"으로 오판돼 넘어가지 않고 conversion 시작 전에 fail-closed해야 하며
     * A.xml은 어디에도 생성되지 않아야 한다.
     */
    private static void testOutputPreflightRejectsDanglingJunctionIntermediate() throws Exception {
        File outputRoot = tempDir();
        File brokenLink = new File(outputRoot, "broken");
        if (!tryCreateDanglingJunction(brokenLink)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] dangling-junction preflight test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        danglingJunctionPreflightTestExecuted = true;
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        File plannedTarget = new File(brokenLink, "A.xml");
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "A.xfdl"), "broken/A.xfdl", plannedTarget, "broken/A.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("dangling-junction-preflight: fails closed", threw);
        assertTrue("dangling-junction-preflight: reason",
                message != null && message.contains("batch_output_intermediate_path_unresolvable"));
        assertTrue("dangling-junction-preflight: no output materialized", !plannedTarget.exists());
    }

    private static void testOutputPreflightRejectsPreexistingTarget() throws Exception {
        File outputRoot = tempDir();
        File preexisting = new File(outputRoot, "Already.xml");
        String originalBytes = "PRE_EXISTING_BYTES_MUST_NOT_CHANGE";
        writeFile(preexisting, originalBytes);
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "Already.xfdl"), "Already.xfdl", preexisting, "Already.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("preexisting-output: fails closed", threw);
        assertTrue("preexisting-output: reason",
                message != null && message.contains("batch_preexisting_output_rejected") && message.contains("Already.xml"));
        String afterBytes = new String(Files.readAllBytes(preexisting.toPath()), StandardCharsets.UTF_8);
        assertEquals("preexisting-output: bytes untouched", originalBytes, afterBytes);
    }

    /** BATCH_PREEXISTING_DIRECTORY_TARGET_TEST -- 정확한 .xml 경로에 디렉터리가 있어도 점유로 본다. */
    private static void testOutputPreflightRejectsPreexistingDirectoryTarget() throws Exception {
        File outputRoot = tempDir();
        File occupyingDir = new File(outputRoot, "Already.xml");
        occupyingDir.mkdirs();
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "Already.xfdl"), "Already.xfdl", occupyingDir, "Already.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("preexisting-directory-target: fails closed", threw);
        assertTrue("preexisting-directory-target: reason",
                message != null && message.contains("batch_preexisting_output_rejected"));
        assertTrue("preexisting-directory-target: directory entry preserved", occupyingDir.isDirectory());
    }

    /**
     * BATCH_REAL_NONREGULAR_TARGET_ENTRY_TEST -- 계획된 {@code *.xml} 경로 자체를 실제 dangling
     * junction이 점유하면 "이미 점유"로 간주해야 한다(link를 따라가는 exists였다면 놓쳤을 사례,
     * Correction 3의 중간경로 문제와 동일한 클래스의 버그를 최종 대상 자체에 대해 검증).
     */
    private static void testOutputPreflightRejectsNonRegularFinalTargetEntry() throws Exception {
        File outputRoot = tempDir();
        File danglingTarget = new File(outputRoot, "Already.xml");
        if (!tryCreateDanglingJunction(danglingTarget)) {
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] non-regular final-target test -- mklink /J "
                    + "unavailable or failed in this environment");
            return;
        }
        nonRegularTargetEntryTestExecuted = true;
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "Already.xfdl"), "Already.xfdl", danglingTarget, "Already.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("nonregular-final-target: fails closed (not misread as nonexistent)", threw);
        assertTrue("nonregular-final-target: reason",
                message != null && message.contains("batch_preexisting_output_rejected"));
    }

    /** BATCH_REAL_DANGLING_TARGET_SYMLINK_TEST -- 실제 symlink 생성 가능 시에만 실행(불가하면 정직히 skip). */
    private static void testOutputPreflightRejectsDanglingSymlinkFinalTarget() throws Exception {
        File outputRoot = tempDir();
        File neverCreatedTarget = new File(outputRoot, "target-for-dangling-symlink");
        File danglingSymlink = new File(outputRoot, "Already.xml");
        try {
            Files.createSymbolicLink(danglingSymlink.toPath(), neverCreatedTarget.toPath());
        } catch (Exception e) {
            danglingTargetSymlinkTestExecuted = false;
            danglingTargetSymlinkSkipReason = e.getClass().getSimpleName() + ":" + e.getMessage();
            System.out.println("[SKIPPED_ENVIRONMENT_LIMITATION] dangling target symlink test unavailable -- "
                    + danglingTargetSymlinkSkipReason);
            return;
        }
        danglingTargetSymlinkTestExecuted = true;
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        List<BatchOutputPlanner.PlannedConversion> plans = Collections.singletonList(
                newPlan(new File(dummySource, "Already.xfdl"), "Already.xfdl", danglingSymlink, "Already.xml"));

        boolean threw = false;
        String message = null;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
            message = e.getMessage();
        }
        assertTrue("dangling-target-symlink: fails closed", threw);
        assertTrue("dangling-target-symlink: reason",
                message != null && message.contains("batch_preexisting_output_rejected"));
    }

    /** BATCH_GENUINELY_MISSING_FINAL_TARGET_TEST -- parent조차 없는 fresh target은 preflight를 통과한다. */
    private static void testOutputPreflightAllowsFreshNonColliding() throws Exception {
        File outputRoot = tempDir();
        Path realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        File dummySource = tempDir();
        File freshTarget = new File(outputRoot, "Fresh/Result.xml");
        List<BatchOutputPlanner.PlannedConversion> plans = Arrays.asList(
                newPlan(new File(dummySource, "Result.xfdl"), "Result.xfdl", freshTarget, "Fresh/Result.xml"));

        boolean threw = false;
        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("fresh-non-colliding: preflight passes", !threw);
    }

    private static BatchOutputPlanner.PlannedConversion newPlan(
            File sourceFile, String relativeSourcePath, File targetFile, String relativeTargetPath) {
        return new BatchOutputPlanner.PlannedConversion(sourceFile, relativeSourcePath, targetFile, relativeTargetPath);
    }

    /** junction을 만든 뒤 그 target 디렉터리를 지워 dangling 상태로 만든다(실패 시 false). */
    private static boolean tryCreateDanglingJunction(File link) {
        File target = new File(link.getParentFile(), link.getName() + "-target-" + System.nanoTime());
        if (!target.mkdirs() || !tryCreateJunction(link, target)) {
            return false;
        }
        return target.delete();
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

    // ---- fixture/assertion 도우미 ----

    private static File tempDir() throws Exception {
        return Files.createTempDirectory("batch-output-preflight-test").toFile();
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

    private static void assertEquals(String message, String expected, String actual) {
        assertTrue(message + " expected=" + expected + " actual=" + actual,
                expected == null ? actual == null : expected.equals(actual));
    }
}
