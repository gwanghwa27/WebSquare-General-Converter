package com.example.xfdltracker.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 발견된 각 {@code .xfdl} input에 대해 정확히 하나의 결정적 output 경로를 계산한다(input root 기준
 * 상대경로 보존, 확장자만 {@code .xml}로 치환). 두 input이 같은 output 경로로 귀결되면 어떤 결과도
 * 발행하기 전에 fail-closed한다.
 */
public final class BatchOutputPlanner {

    private static final String SOURCE_EXTENSION = ".xfdl";
    private static final String TARGET_EXTENSION = ".xml";

    private BatchOutputPlanner() {}

    public static List<PlannedConversion> plan(File inputRoot, File outputRoot, List<File> discoveredInputs) {
        if (inputRoot == null || outputRoot == null || discoveredInputs == null) {
            throw new IllegalArgumentException(
                    "batch_output_planner: inputRoot/outputRoot/discoveredInputs must not be null");
        }
        List<PlannedConversion> plans = new ArrayList<PlannedConversion>();
        Map<String, String> claimedBy = new LinkedHashMap<String, String>();
        for (File input : discoveredInputs) {
            String relativeSourcePath = BatchSourceDiscovery.relativePath(inputRoot, input);
            if (!relativeSourcePath.endsWith(SOURCE_EXTENSION)) {
                throw new IllegalStateException(
                        "batch_output_planner: discovered input does not end with " + SOURCE_EXTENSION + " -- "
                                + relativeSourcePath);
            }
            String relativeTargetPath = relativeSourcePath.substring(
                    0, relativeSourcePath.length() - SOURCE_EXTENSION.length()) + TARGET_EXTENSION;
            String priorOwner = claimedBy.get(relativeTargetPath);
            if (priorOwner != null) {
                throw new IllegalStateException(
                        "batch_output_planner: output path collision -- " + relativeTargetPath
                                + " is claimed by both " + priorOwner + " and " + relativeSourcePath
                                + " (batch_output_path_collision)");
            }
            claimedBy.put(relativeTargetPath, relativeSourcePath);
            File target = new File(outputRoot, relativeTargetPath.replace('/', File.separatorChar));
            plans.add(new PlannedConversion(input, relativeSourcePath, target, relativeTargetPath));
        }
        return plans;
    }

    public static final class PlannedConversion {
        private final File sourceFile;
        private final String relativeSourcePath;
        private final File targetFile;
        private final String relativeTargetPath;

        PlannedConversion(File sourceFile, String relativeSourcePath, File targetFile, String relativeTargetPath) {
            this.sourceFile = sourceFile;
            this.relativeSourcePath = relativeSourcePath;
            this.targetFile = targetFile;
            this.relativeTargetPath = relativeTargetPath;
        }

        public File getSourceFile() { return sourceFile; }
        public String getRelativeSourcePath() { return relativeSourcePath; }
        public File getTargetFile() { return targetFile; }
        public String getRelativeTargetPath() { return relativeTargetPath; }
    }
}
