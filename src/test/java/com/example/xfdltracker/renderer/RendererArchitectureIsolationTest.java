package com.example.xfdltracker.renderer;

import com.example.xfdltracker.composition.TargetCompositionPlan;
import com.example.xfdltracker.payload.TargetNodePayload;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * renderer 패키지 소스가 upstream(source-side) 타입을 참조하지 않는지 파일 텍스트로 확인하고,
 * public {@link AtomicWebSquareRenderer#render}의 파라미터 타입이 {@code TargetCompositionPlan}/
 * {@code List<TargetNodePayload>}뿐인지(source DOM Element/Document 미입력) reflection으로 확인한다.
 */
public class RendererArchitectureIsolationTest {

    private static int failures = 0;

    private static final String[] FORBIDDEN_TYPE_NAMES = {
            "XfdlAnalysisResult", "SemanticRegionResult", "SemanticRegionGraph",
            "SlotAssignmentCandidate", "CandidateResolution", "BindingAnalyzer", "EventBinding",
            "ComponentMappingRegistry", "WebSquareGenerator",
            // ComponentPredicateAnalyzer/ComponentPredicateAnalysis/ComponentLayoutConverter는
            // SemanticRegionSegmenter만 위임하는 component-level/pre-semantic 협력자다 --
            // renderer 계층이 이들을 직접 참조해서는 안 된다.
            "ComponentPredicateAnalysis", "ComponentPredicateAnalyzer", "ComponentLayoutConverter",
            // Slice 102D -- source-lane option evidence 타입(semantic/analyzer 패키지)도 renderer가
            // 직접 참조해선 안 된다. renderer는 오직 target-lane TargetOptionItem(payload 패키지)만
            // 소비한다 -- source Dataset identity/원본 resolve 결과를 볼 수 없어야 한다.
            "SourceOptionSetEvidence", "SourceOptionResolution", "SourceOptionSetResolver", "SourceOptionItem"
    };

    public static void main(String[] args) throws Exception {
        testNoForbiddenUpstreamTypeReferencesInRendererSource();
        testRenderMethodInputBoundaryIsPlanAndPayloadOnly();
        testRenderMethodReturnTypeIsFamilyNeutralResultContract();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testNoForbiddenUpstreamTypeReferencesInRendererSource() throws Exception {
        File dir = new File("src/main/java/com/example/xfdltracker/renderer");
        assertTrue("renderer source directory exists", dir.isDirectory());
        File[] files = dir.listFiles();
        assertTrue("renderer source directory has at least one .java file",
                files != null && files.length > 0);

        int javaFileCount = 0;
        for (File file : files) {
            if (!file.getName().endsWith(".java")) {
                continue;
            }
            javaFileCount++;
            String content = stripComments(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
            for (String forbidden : FORBIDDEN_TYPE_NAMES) {
                // "EventBinding"은 금지 타입이지만 합법적인 TargetEventBinding의 부분 문자열이기도 하다 --
                // 오탐을 막기 위해 TargetEventBinding을 먼저 제거한 뒤 순수 "EventBinding" 참조만 검사한다.
                String scanned = content.replace("TargetEventBinding", "");
                assertTrue("architecture-isolation: " + file.getName()
                                + " does not reference " + forbidden + " in actual code (comments excluded)",
                        !scanned.contains(forbidden));
            }
        }
        assertTrue("architecture-isolation: at least one renderer .java file scanned", javaFileCount > 0);
    }

    private static void testRenderMethodInputBoundaryIsPlanAndPayloadOnly() throws Exception {
        Method render = AtomicWebSquareRenderer.class.getMethod("render", TargetCompositionPlan.class, List.class);
        Class<?>[] paramTypes = render.getParameterTypes();
        assertEquals("render() parameter count", "2", String.valueOf(paramTypes.length));
        assertEquals("render() param[0] type", "TargetCompositionPlan", paramTypes[0].getSimpleName());
        assertEquals("render() param[1] type", "List", paramTypes[1].getSimpleName());

        // public 메서드 전체 중 org.w3c.dom.Element/Document를 파라미터로 받는 것이 없는지 확인.
        for (Method m : AtomicWebSquareRenderer.class.getMethods()) {
            if (m.getDeclaringClass() != AtomicWebSquareRenderer.class) {
                continue;
            }
            for (Class<?> paramType : m.getParameterTypes()) {
                assertTrue("architecture-isolation: public method " + m.getName()
                                + " does not accept org.w3c.dom.Element/Document as input",
                        !"Element".equals(paramType.getSimpleName()) && !"Document".equals(paramType.getSimpleName()));
            }
        }
    }

    /**
     * {@link AtomicWebSquareRenderer#render}의 return type이 family별 클래스가 아니라
     * family-neutral한 {@link AtomicRenderResult}인지 확인하고, 제거된 legacy
     * {@code TitleBarRenderResult} 클래스가 다시 존재하지 않는지(회귀 방지) 검증한다.
     */
    private static void testRenderMethodReturnTypeIsFamilyNeutralResultContract() throws Exception {
        Method render = AtomicWebSquareRenderer.class.getMethod("render", TargetCompositionPlan.class, List.class);
        Type genericReturnType = render.getGenericReturnType();
        assertTrue("render() return type is a parameterized List<...>",
                genericReturnType instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        assertEquals("render() return element type is family-neutral AtomicRenderResult",
                "AtomicRenderResult", ((Class<?>) elementType).getSimpleName());

        boolean oldFamilySpecificTypeStillExists;
        try {
            Class.forName("com.example.xfdltracker.renderer.TitleBarRenderResult");
            oldFamilySpecificTypeStillExists = true;
        } catch (ClassNotFoundException e) {
            oldFamilySpecificTypeStillExists = false;
        }
        assertTrue("family-specific TitleBarRenderResult no longer exists as a class",
                !oldFamilySpecificTypeStillExists);
    }

    /** {@code //}/{@code /* *}{@code /} 주석을 제거한다 -- 실제 코드만 검사해야 하며 javadoc의
     * 금지 타입 언급으로 인한 오탐을 막기 위함이다. 문자열 리터럴 내 주석 기호는 등장하지 않으므로
     * 이 단순 구현으로 충분하다. */
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
