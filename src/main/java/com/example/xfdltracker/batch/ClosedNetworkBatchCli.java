package com.example.xfdltracker.batch;

import com.example.xfdltracker.pipeline.TargetPipelineConfig;
import com.example.xfdltracker.runtime.CommonRuntimeCapabilityCatalog;
import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 폐쇄망 batch 변환 CLI: 인자 파싱, root 경계 검증, profile 로딩, input 탐색, output 계획, output
 * preflight(경계/기존 파일 충돌), pipeline 실행, 결과 보고만 순서대로 수행한다. XFDL 해석/target
 * XML 구성/legacy 변환기 호출은 하지 않으며, exact-JDK 게이트는 호출 스크립트가 위임하는 authority다.
 */
public final class ClosedNetworkBatchCli {

    private ClosedNetworkBatchCli() {}

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode);
    }

    /** 테스트 가능한 순수 진입점 -- {@code System.exit}를 호출하지 않는다. */
    static int run(String[] args, PrintStream out) {
        if (args == null || args.length != 3) {
            out.println("usage: ClosedNetworkBatchCli <inputRoot> <outputRoot> <runtimeProfileFile>");
            return 2;
        }
        File inputRoot = new File(args[0]);
        File outputRoot = new File(args[1]);
        File profileFile = new File(args[2]);

        if (!inputRoot.isDirectory()) {
            out.println("[BATCH_FAIL] input root is not a directory -- " + inputRoot.getAbsolutePath()
                    + " (batch_input_root_not_directory)");
            return 2;
        }
        if (outputRoot.exists() && !outputRoot.isDirectory()) {
            out.println("[BATCH_FAIL] output root exists and is not a directory -- " + outputRoot.getAbsolutePath()
                    + " (batch_output_root_not_directory)");
            return 2;
        }

        Path realInputRoot;
        Path realOutputRoot;
        try {
            realInputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(inputRoot);
            realOutputRoot = BatchPathBoundary.resolveRealPathAllowingMissingTail(outputRoot);
        } catch (IOException e) {
            out.println("[BATCH_FAIL] failed to resolve input/output root real path -- " + e.getMessage()
                    + " (batch_root_real_path_unresolvable)");
            return 2;
        }
        if (realInputRoot.equals(realOutputRoot)) {
            out.println("[BATCH_FAIL] input root and output root must not be the same directory -- "
                    + realInputRoot + " (batch_input_output_root_identical)");
            return 2;
        }
        if (realOutputRoot.startsWith(realInputRoot)) {
            out.println("[BATCH_FAIL] output root must not be nested under input root -- " + realOutputRoot
                    + " is under " + realInputRoot + " (batch_output_root_descendant_of_input_root)");
            return 2;
        }
        if (realInputRoot.startsWith(realOutputRoot)) {
            out.println("[BATCH_FAIL] input root must not be nested under output root -- " + realInputRoot
                    + " is under " + realOutputRoot + " (batch_input_root_descendant_of_output_root)");
            return 2;
        }

        CommonRuntimeCapabilityCatalog catalog = CommonRuntimeCapabilityCatalog.createSeeded();
        TargetRuntimeProfile profile;
        try {
            profile = BatchRuntimeProfileLoader.load(profileFile, catalog);
        } catch (Exception e) {
            out.println("[BATCH_FAIL] " + e.getMessage());
            return 2;
        }

        List<File> discovered;
        try {
            discovered = BatchSourceDiscovery.discover(inputRoot);
        } catch (RuntimeException e) {
            out.println("[BATCH_FAIL] " + e.getMessage());
            return 2;
        }
        if (discovered.isEmpty()) {
            out.println("[BATCH_FAIL] no .xfdl files found under " + inputRoot.getAbsolutePath()
                    + " -- refusing to report success for a vacuous batch (batch_zero_input_rejected)");
            return 2;
        }

        List<BatchOutputPlanner.PlannedConversion> plans;
        try {
            plans = BatchOutputPlanner.plan(inputRoot, outputRoot, discovered);
        } catch (RuntimeException e) {
            out.println("[BATCH_FAIL] " + e.getMessage());
            return 2;
        }

        try {
            BatchOutputPreflight.verify(realOutputRoot, plans);
        } catch (Exception e) {
            out.println("[BATCH_FAIL] " + e.getMessage());
            return 2;
        }

        out.println("[BATCH_INPUT_COUNT] " + plans.size());

        TargetPipelineConfig config = new TargetPipelineConfig(profile);
        BatchConversionRunner.Outcome outcome = BatchConversionRunner.run(plans, config);

        for (String s : outcome.getSucceeded()) {
            out.println("[BATCH_OUTPUT_OK] " + s);
        }

        if (outcome.isFailure()) {
            out.println("[BATCH_FAILED_FILE] " + outcome.getFailedRelativePath() + " -- " + outcome.getFailureReason());
            for (String skipped : outcome.getSkippedAfterFailure()) {
                out.println("[BATCH_SKIPPED_AFTER_FAILURE] " + skipped);
            }
            out.println("[BATCH_RESULT_FAIL] succeeded=" + outcome.getSucceeded().size() + " failed=1 skipped="
                    + outcome.getSkippedAfterFailure().size());
            return 1;
        }

        out.println("[BATCH_RESULT_PASS] succeeded=" + outcome.getSucceeded().size());
        return 0;
    }
}
