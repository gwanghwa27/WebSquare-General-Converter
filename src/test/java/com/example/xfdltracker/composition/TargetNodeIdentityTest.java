package com.example.xfdltracker.composition;

import com.example.xfdltracker.payload.TargetLeafPayload;
import com.example.xfdltracker.payload.TargetNodePayload;
import com.example.xfdltracker.renderer.AtomicRenderResult;
import com.example.xfdltracker.renderer.AtomicWebSquareRenderer;
import com.example.xfdltracker.renderer.CompositionRenderResult;
import com.example.xfdltracker.renderer.CompositionRenderer;
import com.example.xfdltracker.renderer.RenderStatus;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link TargetNodeIdentity} carrier의 validation/equality 계약과, {@link AtomicRenderResult}/
 * {@link CompositionRenderResult}가 성공/실패 모든 status에서 exact provenance tuple을 보존하는지,
 * {@link CompositionRenderer}의 exact-tuple correlation이 value-only로 fallback하지 않는지 검증한다.
 */
public class TargetNodeIdentityTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testRejectsNullKind();
        testRejectsNullValue();
        testRejectsEmptyValue();
        testRejectsWhitespaceOnlyValue();
        testStoredValueIsNotTrimmedOrNormalized();
        testEqualityRequiresKindAndValue();
        testSameValueDifferentKindAreUnequal();

        testAtomicSuccessResultPreservesExactIdentity();
        testAtomicFailureResultPreservesExactIdentity();
        testCompositionSuccessResultPreservesExactIdentity();
        testCompositionFailureResultPreservesExactIdentity();

        testSameIdentityValueDifferentKindDoesNotCorrelateInComposition();

        testNoDownstreamOriginToKindInferenceInRendererSource();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testRejectsNullKind() {
        boolean threw = false;
        try {
            new TargetNodeIdentity(null, "v1");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("null kind rejected", threw);
    }

    private static void testRejectsNullValue() {
        boolean threw = false;
        try {
            new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, null);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("null value rejected", threw);
    }

    private static void testRejectsEmptyValue() {
        boolean threw = false;
        try {
            new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("empty value rejected", threw);
    }

    private static void testRejectsWhitespaceOnlyValue() {
        boolean threw = false;
        try {
            new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "   ");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue("whitespace-only value rejected", threw);
    }

    private static void testStoredValueIsNotTrimmedOrNormalized() {
        TargetNodeIdentity identity = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "  padded  ");
        assertEquals("stored value is the exact original String (not trimmed)", "  padded  ", identity.getValue());
    }

    private static void testEqualityRequiresKindAndValue() {
        TargetNodeIdentity a = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "same");
        TargetNodeIdentity b = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "same");
        assertTrue("same kind+value are equal", a.equals(b));
        assertTrue("same kind+value have same hashCode", a.hashCode() == b.hashCode());
    }

    private static void testSameValueDifferentKindAreUnequal() {
        TargetNodeIdentity sourceStructural = new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "same");
        TargetNodeIdentity targetSynthetic = new TargetNodeIdentity(TargetNodeIdentityKind.TARGET_SYNTHETIC, "same");
        assertTrue("same value but different kind are NOT equal (no value-only equality fallback)",
                !sourceStructural.equals(targetSynthetic));
    }

    private static void testAtomicSuccessResultPreservesExactIdentity() throws Exception {
        TargetCompositionNode node = titleBarNode("provAtomicOk");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());
        TargetNodePayload payload = new TargetNodePayload(
                node.getIdentityKind(), "provAtomicOk", Collections.<TargetLeafPayload>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(plan, Collections.singletonList(payload));
        assertEquals("atomic success: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        assertTrue("atomic success: result carries the exact same TargetNodeIdentity as the Plan node",
                node.getIdentity().equals(results.get(0).getIdentity()));
    }

    private static void testAtomicFailureResultPreservesExactIdentity() throws Exception {
        TargetCompositionNode node = new TargetCompositionNode(
                "provAtomicFail", "TREEVIEW", "basic", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "provAtomicFail",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "provAtomicFail"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());

        List<AtomicRenderResult> results = new AtomicWebSquareRenderer().render(
                plan, Collections.<TargetNodePayload>emptyList());
        assertEquals("atomic failure: status", "UNSUPPORTED_FAMILY", String.valueOf(results.get(0).getStatus()));
        assertTrue("atomic failure: result still carries the exact same TargetNodeIdentity as the Plan node",
                node.getIdentity().equals(results.get(0).getIdentity()));
    }

    private static void testCompositionSuccessResultPreservesExactIdentity() throws Exception {
        TargetCompositionNode node = titleBarNode("provCompOk");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());
        TargetNodePayload payload = new TargetNodePayload(
                node.getIdentityKind(), "provCompOk", Collections.<TargetLeafPayload>emptyList());
        List<AtomicRenderResult> atomicResults =
                new AtomicWebSquareRenderer().render(plan, Collections.singletonList(payload));

        List<CompositionRenderResult> results = new CompositionRenderer().render(plan, atomicResults);
        assertEquals("composition success: status", "RENDERED", String.valueOf(results.get(0).getStatus()));
        assertTrue("composition success: result carries the exact same TargetNodeIdentity as the Plan node",
                node.getIdentity().equals(results.get(0).getIdentity()));
    }

    private static void testCompositionFailureResultPreservesExactIdentity() throws Exception {
        TargetCompositionNode node = titleBarNode("provCompFail");
        TargetCompositionPlan plan = new TargetCompositionPlan(
                Collections.singletonList(node), Collections.<TargetCompositionEdge>emptyList());

        List<CompositionRenderResult> results = new CompositionRenderer().render(
                plan, Collections.<AtomicRenderResult>emptyList());
        assertEquals("composition failure: status", "INTEGRITY_VIOLATION", String.valueOf(results.get(0).getStatus()));
        assertTrue("composition failure: result still carries the exact same TargetNodeIdentity as the Plan node",
                node.getIdentity().equals(results.get(0).getIdentity()));
    }

    /** 두 Plan node의 identity value는 같지만 kind가 다를 때, {@link CompositionRenderer}가
     * 절대 같은 것으로 correlate하지 않고 각자 자기 자신의 {@link AtomicRenderResult}에만
     * correlate됨을 확인한다(value-only lookup으로 cross-wire되지 않음). */
    private static void testSameIdentityValueDifferentKindDoesNotCorrelateInComposition() throws Exception {
        TargetCompositionNode sourceNode = new TargetCompositionNode(
                "dupValueNode", "TITLE_BAR", "title_only", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, "dupValueNode",
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, "dupValueNode"));
        TargetCompositionNode syntheticNode = new TargetCompositionNode(
                "dupValueNode", "TITLE_BAR", "title_only", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.TARGET_SYNTHETIC, null,
                new TargetNodeIdentity(TargetNodeIdentityKind.TARGET_SYNTHETIC, "dupValueNode"));
        TargetCompositionPlan plan = new TargetCompositionPlan(
                java.util.Arrays.asList(sourceNode, syntheticNode), Collections.<TargetCompositionEdge>emptyList());

        TargetNodePayload sourcePayload = new TargetNodePayload(
                TargetNodeIdentityKind.SOURCE_STRUCTURAL, "dupValueNode", Collections.<TargetLeafPayload>emptyList());
        List<AtomicRenderResult> atomicResults =
                new AtomicWebSquareRenderer().render(plan, Collections.singletonList(sourcePayload));
        // syntheticNode는 매칭되는 payload envelope가 없으므로 -> 그 자신의 atomic result는
        // UNSUPPORTED(envelope 누락)이며, sourceNode와 같은 String 값을 공유하더라도
        // sourceNode의 RENDERED result로 혼동되지 않는다.
        assertEquals("dup-value: sourceNode atomic status", "RENDERED", String.valueOf(atomicResults.get(0).getStatus()));
        assertEquals("dup-value: syntheticNode atomic status (own envelope missing, not cross-wired)",
                "INTEGRITY_VIOLATION", String.valueOf(atomicResults.get(1).getStatus()));

        List<CompositionRenderResult> compositionResults = new CompositionRenderer().render(plan, atomicResults);
        assertEquals("dup-value: sourceNode composition status", "RENDERED",
                String.valueOf(compositionResults.get(0).getStatus()));
        assertEquals("dup-value: syntheticNode composition status (correctly finds its OWN failed atomic result, "
                        + "not sourceNode's RENDERED one)",
                "ATOMIC_RENDER_UNAVAILABLE", String.valueOf(compositionResults.get(1).getStatus()));
        assertTrue("dup-value: sourceNode result identity kind is SOURCE_STRUCTURAL",
                compositionResults.get(0).getIdentity().getKind() == TargetNodeIdentityKind.SOURCE_STRUCTURAL);
        assertTrue("dup-value: syntheticNode result identity kind is TARGET_SYNTHETIC (not cross-wired to the "
                        + "SOURCE_STRUCTURAL sibling that shares the same String value)",
                compositionResults.get(1).getIdentity().getKind() == TargetNodeIdentityKind.TARGET_SYNTHETIC);
    }

    /** {@code AtomicWebSquareRenderer}/{@code CompositionRenderer} 실제 소스 텍스트를 읽어
     * {@code .getOrigin()} 호출(comment 제외)이 전혀 없음을 확인한다 -- downstream
     * origin-to-kind inference가 코드에 남아있지 않다는 것을 텍스트 부재로 증명한다. */
    private static void testNoDownstreamOriginToKindInferenceInRendererSource() throws Exception {
        String[] files = {
                "src/main/java/com/example/xfdltracker/renderer/AtomicWebSquareRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/CompositionRenderer.java"
        };
        for (String path : files) {
            File file = new File(path);
            assertTrue("origin-audit: " + path + " exists", file.isFile());
            String content = stripComments(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
            assertTrue("origin-audit: " + file.getName() + " does not call .getOrigin() in actual code "
                            + "(comments excluded) -- no downstream Origin-to-kind inference",
                    !content.contains(".getOrigin()"));
        }
    }

    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
            } else {
                out.append(source.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    private static TargetCompositionNode titleBarNode(String nodeId) {
        return new TargetCompositionNode(
                nodeId, "TITLE_BAR", "title_only", "HIGH", new LinkedHashMap<String, Object>(), null,
                CompositionDecision.Origin.SOURCE_SEMANTIC, nodeId,
                new TargetNodeIdentity(TargetNodeIdentityKind.SOURCE_STRUCTURAL, nodeId));
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("[FAIL] " + label + " -- expected=<" + expected + "> actual=<" + actual + ">");
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }

    private static void assertTrue(String label, boolean condition) {
        if (!condition) {
            System.out.println("[FAIL] " + label);
            failures++;
        } else {
            System.out.println("[PASS] " + label);
        }
    }
}
