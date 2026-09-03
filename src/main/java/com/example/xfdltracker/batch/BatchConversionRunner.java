package com.example.xfdltracker.batch;

import com.example.xfdltracker.pipeline.TargetPipelineConfig;
import com.example.xfdltracker.pipeline.TargetWebSquarePipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 계획된 conversion을 결정적 순서로 실행한다. 각 개별 output은 {@link TargetWebSquarePipeline}이
 * 이미 원자적으로 발행하므로 이 클래스는 파일 쓰기를 직접 하지 않는다. 첫 실패에서 즉시 멈추며,
 * 이미 끝난 output은 그대로 두고 성공/실패/미시도 목록을 보고한다(부분 완료를 성공으로 위장 안 함).
 */
public final class BatchConversionRunner {

    private BatchConversionRunner() {}

    public static Outcome run(List<BatchOutputPlanner.PlannedConversion> plans, TargetPipelineConfig config) {
        if (plans == null || config == null) {
            throw new IllegalArgumentException("batch_conversion_runner: plans/config must not be null");
        }
        List<String> succeeded = new ArrayList<String>();
        for (int i = 0; i < plans.size(); i++) {
            BatchOutputPlanner.PlannedConversion plan = plans.get(i);
            try {
                new TargetWebSquarePipeline().convert(plan.getSourceFile(), plan.getTargetFile(), config);
                succeeded.add(plan.getRelativeSourcePath());
            } catch (RuntimeException e) {
                List<String> remaining = new ArrayList<String>();
                for (int j = i + 1; j < plans.size(); j++) {
                    remaining.add(plans.get(j).getRelativeSourcePath());
                }
                return Outcome.failure(succeeded, plan.getRelativeSourcePath(), String.valueOf(e.getMessage()),
                        remaining);
            }
        }
        return Outcome.success(succeeded);
    }

    public static final class Outcome {
        private final List<String> succeeded;
        private final String failedRelativePath;
        private final String failureReason;
        private final List<String> skippedAfterFailure;

        private Outcome(List<String> succeeded, String failedRelativePath, String failureReason,
                List<String> skippedAfterFailure) {
            this.succeeded = Collections.unmodifiableList(new ArrayList<String>(succeeded));
            this.failedRelativePath = failedRelativePath;
            this.failureReason = failureReason;
            this.skippedAfterFailure = Collections.unmodifiableList(new ArrayList<String>(skippedAfterFailure));
        }

        static Outcome success(List<String> succeeded) {
            return new Outcome(succeeded, null, null, Collections.<String>emptyList());
        }

        static Outcome failure(List<String> succeeded, String failedRelativePath, String failureReason,
                List<String> skippedAfterFailure) {
            return new Outcome(succeeded, failedRelativePath, failureReason, skippedAfterFailure);
        }

        public boolean isFailure() { return failedRelativePath != null; }
        public List<String> getSucceeded() { return succeeded; }
        public String getFailedRelativePath() { return failedRelativePath; }
        public String getFailureReason() { return failureReason; }
        public List<String> getSkippedAfterFailure() { return skippedAfterFailure; }
    }
}
