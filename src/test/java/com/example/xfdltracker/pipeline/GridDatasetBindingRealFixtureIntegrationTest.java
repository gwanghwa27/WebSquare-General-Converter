package com.example.xfdltracker.pipeline;

import com.example.xfdltracker.runtime.TargetRuntimeProfile;

import java.io.File;
import java.nio.file.Files;

/**
 * Slice 102F -- 실제 corpus fixture(ComponentMethodConversion.xfdl, binddataset+bind:CODE)가
 * 전체 파이프라인에서 새 GRID binding fail-closed guard를 트리거함을 증명한다. fixture는
 * 임시 디렉터리로 복사만 하고 절대 수정하지 않는다.
 */
public class GridDatasetBindingRealFixtureIntegrationTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testComponentMethodConversionRealFixtureFailsClosedNoPartialOutput();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testComponentMethodConversionRealFixtureFailsClosedNoPartialOutput() throws Exception {
        File root = repositoryRoot();
        File realFixture = new File(root, "sample-phase3-project/Form/ComponentMethodConversion.xfdl");
        assertTrue("real-fixture: ComponentMethodConversion.xfdl exists in tracked corpus", realFixture.isFile());

        File tempDir = Files.createTempDirectory("grid-binding-real-fixture").toFile();
        File xfdlCopy = new File(tempDir, "ComponentMethodConversion.xfdl");
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
        assertTrue("real-fixture: pipeline fails closed before final publication", threw);
        assertTrue("real-fixture: exception reason names the exact new binding-contract evidence",
                reason != null && reason.contains("grid_single_format_binding_contract_not_implemented"));
        assertTrue("real-fixture: no partial/invalid target XML is ever published", !output.exists());
    }

    private static File repositoryRoot() {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "build.bat").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("grid_binding_real_fixture_test: could not locate sanctioned working-copy root");
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("[FAIL] " + message);
        } else {
            System.out.println("[PASS] " + message);
        }
    }
}
