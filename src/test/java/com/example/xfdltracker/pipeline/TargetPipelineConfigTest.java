package com.example.xfdltracker.pipeline;

import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import java.util.Collections;

/** 외부 의존성 없는(non-JUnit) {@link TargetPipelineConfig} 단위 테스트. */
public class TargetPipelineConfigTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testRuntimeProfileRequired();
        testRuntimeProfileStoredExactly();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testRuntimeProfileRequired() {
        boolean threw = false;
        try {
            new TargetPipelineConfig(null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("target_pipeline_config: null runtimeProfile rejected", threw);
    }

    private static void testRuntimeProfileStoredExactly() {
        TargetRuntimeProfile profile = new TargetRuntimeProfile(Collections.singleton("MESSAGE_DIALOG"));
        TargetPipelineConfig config = new TargetPipelineConfig(profile);
        assertTrue("target_pipeline_config: stored runtimeProfile is the exact instance supplied",
                config.getRuntimeProfile() == profile);
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
