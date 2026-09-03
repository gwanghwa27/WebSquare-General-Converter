package com.example.xfdltracker.behavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TargetScriptTranslator}에 대한 offline 테스트.
 */
public class TargetScriptTranslatorTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testValidSingleFunctionTranslated();
        testMultipleFunctionsPreserveOrder();
        testExactTargetIdentifierNoRenameNoSuffix();
        testExactParameterOrder();
        testLocalVariableTranslatedDeterministically();
        testAssignmentTranslated();
        testReturnTranslated();
        testIfElseTranslated();
        testWhileTranslated();
        testClassicForTranslated();
        testLocalHelperCallTranslatedToScwinIdentifier();
        testAllowedMathCallPreserved();
        testUnaryBinaryLogicalConditionalSemanticsPreserved();
        testUnresolvedLocalFunctionReference();
        testOnpageloadIntegrityViolation();
        testOnpageunloadIntegrityViolation();
        testInvalidTargetIdentifierFailsClosed();
        testArtifactOrderExact();
        testArtifactIdentifierIndexExact();
        testArtifactDuplicateProtection();
        testArtifactImmutable();

        // TargetScriptArtifact 불변식: identifierIndex는 항상 functionsInOrder의 identifier
        // 집합과 정확히 일치해야 한다.
        testArtifactZeroModelsEmptyIndex();
        testArtifactTwoModelsOrderAndIndexExact();
        testArtifactExactlyOneProductionConstructorTakingFunctionModelList();
        testArtifactUnbackedIdentifierStructurallyImpossible();

        // 중괄호 없는 단일 문장 control-flow body: 소스 중괄호 유무와 무관하게 번역 결과가
        // 동일해야 한다.
        testBracelessIfTranslatedSameAsBraced();
        testBracelessIfElseTranslatedSameAsBraced();
        testBracelessWhileTranslatedSameAsBraced();
        testBracelessForTranslatedSameAsBraced();
        testDanglingElseTranslatedPreservesNearestIfSemantics();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static SourceScriptAnalysis analyze(String script) {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(script);
        assertEquals("[" + script + "]: analyzer status", "ANALYZED", String.valueOf(result.getStatus()));
        return result.getAnalysis();
    }

    private static void testValidSingleFunctionTranslated() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function foo(){ return 1; }"));
        assertEquals("valid-single: status", "TRANSLATED", String.valueOf(result.getStatus()));
        assertEquals("valid-single: function count", "1",
                String.valueOf(result.getArtifact().getFunctionsInOrder().size()));
    }

    private static void testMultipleFunctionsPreserveOrder() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function a(){} function b(){} function c(){}"));
        List<TargetScwinFunctionModel> functions = result.getArtifact().getFunctionsInOrder();
        assertEquals("multi-order: count", "3", String.valueOf(functions.size()));
        assertEquals("multi-order: names", "a,b,c",
                functions.get(0).getIdentifier() + "," + functions.get(1).getIdentifier() + ","
                        + functions.get(2).getIdentifier());
    }

    private static void testExactTargetIdentifierNoRenameNoSuffix() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function myHandler(){}"));
        assertEquals("exact-identifier: name", "myHandler",
                result.getArtifact().getFunctionsInOrder().get(0).getIdentifier());
    }

    private static void testExactParameterOrder() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function f(a, b, c){}"));
        assertEquals("param-order: exact", "a,b,c",
                String.join(",", result.getArtifact().getFunctionsInOrder().get(0).getParameters()));
    }

    private static void testLocalVariableTranslatedDeterministically() {
        String body = translatedBody("function f(){ var x = 1; }");
        assertEquals("var-translated: exact", "var x = 1;", body);
    }

    private static void testAssignmentTranslated() {
        String body = translatedBody("function f(){ var x; x = 2; }");
        assertEquals("assign-translated: exact", "var x; x = 2;", body);
    }

    private static void testReturnTranslated() {
        String body = translatedBody("function f(){ return 1; }");
        assertEquals("return-translated: exact", "return 1;", body);
        assertEquals("return-void-translated: exact", "return;", translatedBody("function f(){ return; }"));
    }

    private static void testIfElseTranslated() {
        String body = translatedBody("function f(){ if (1) { return 1; } else { return 2; } }");
        assertEquals("if-else-translated: exact", "if (1) { return 1; } else { return 2; }", body);
    }

    private static void testWhileTranslated() {
        String body = translatedBody("function f(){ while (1) { break; } }");
        assertEquals("while-translated: exact", "while (1) { break; }", body);
    }

    private static void testClassicForTranslated() {
        String body = translatedBody("function f(){ for (var i = 0; i < 10; i = i + 1) { continue; } }");
        assertEquals("for-translated: exact", "for (var i = 0; (i < 10); i = (i + 1)) { continue; }", body);
    }

    private static void testLocalHelperCallTranslatedToScwinIdentifier() {
        String body = translatedBody("function f(){ return helper(1); } function helper(x){ return x; }");
        assertEquals("helper-call-translated: exact", "return scwin.helper(1);", body);
    }

    private static void testAllowedMathCallPreserved() {
        String body = translatedBody("function f(){ return Math.abs(-1); }");
        assertEquals("math-preserved: exact", "return Math.abs((-1));", body);
    }

    private static void testUnaryBinaryLogicalConditionalSemanticsPreserved() {
        assertEquals("unary-preserved: exact", "return (!1);", translatedBody("function f(){ return !1; }"));
        assertEquals("binary-preserved: exact", "return (1 + 2);", translatedBody("function f(){ return 1 + 2; }"));
        assertEquals("logical-preserved: exact", "return (1 && 2);", translatedBody("function f(){ return 1 && 2; }"));
        assertEquals("conditional-preserved: exact", "return (1 ? 2 : 3);",
                translatedBody("function f(){ return 1 ? 2 : 3; }"));
    }

    private static void testUnresolvedLocalFunctionReference() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function f(){ return notDeclared(1); }"));
        assertEquals("unresolved: status", "UNRESOLVED_FUNCTION_REFERENCE", String.valueOf(result.getStatus()));
        assertTrue("unresolved: artifact absent", result.getArtifact() == null);
        assertTrue("unresolved: reason present", result.getReason() != null && result.getReason().length() > 0);
    }

    private static void testOnpageloadIntegrityViolation() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function onpageload(){ return 1; }"));
        assertEquals("onpageload: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("onpageload: artifact absent", result.getArtifact() == null);
    }

    private static void testOnpageunloadIntegrityViolation() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function onpageunload(){ return 1; }"));
        assertEquals("onpageunload: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("onpageunload: artifact absent", result.getArtifact() == null);
    }

    private static void testInvalidTargetIdentifierFailsClosed() {
        // analyzer의 IDENTIFIER 문법을 우회해 translator 자체의 독립적인 target-identifier
        // 검증을 defense-in-depth로 시험한다.
        SourceFunctionModel invalid = new SourceFunctionModel(
                "123bad-name", Collections.<String>emptyList(), Collections.<SourceStatementNode>emptyList());
        SourceScriptAnalysis analysis = new SourceScriptAnalysis(java.util.Arrays.asList(invalid));
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(analysis);
        assertEquals("invalid-identifier: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("invalid-identifier: artifact absent", result.getArtifact() == null);
    }

    private static void testArtifactOrderExact() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function z(){} function a(){} function m(){}"));
        List<TargetScwinFunctionModel> functions = result.getArtifact().getFunctionsInOrder();
        assertEquals("artifact-order: exact declaration order preserved (not alphabetized)", "z,a,m",
                functions.get(0).getIdentifier() + "," + functions.get(1).getIdentifier() + ","
                        + functions.get(2).getIdentifier());
    }

    private static void testArtifactIdentifierIndexExact() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(
                analyze("function a(){} function b(){}"));
        TargetScriptArtifact artifact = result.getArtifact();
        assertTrue("artifact-index: contains a", artifact.containsFinalizedTargetFunctionIdentifier("a"));
        assertTrue("artifact-index: contains b", artifact.containsFinalizedTargetFunctionIdentifier("b"));
        assertTrue("artifact-index: does not contain c", !artifact.containsFinalizedTargetFunctionIdentifier("c"));
        assertEquals("artifact-index: size", "2", String.valueOf(artifact.getFinalizedTargetFunctionIdentifiers().size()));
    }

    private static void testArtifactDuplicateProtection() {
        List<TargetScwinFunctionModel> duplicated = new ArrayList<TargetScwinFunctionModel>();
        duplicated.add(new TargetScwinFunctionModel("dup", Collections.<String>emptyList(), ""));
        duplicated.add(new TargetScwinFunctionModel("dup", Collections.<String>emptyList(), ""));
        boolean threw = false;
        try {
            new TargetScriptArtifact(duplicated);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue("artifact-duplicate: rejected", threw);
    }

    private static void testArtifactImmutable() {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(analyze("function a(){}"));
        boolean threw = false;
        try {
            result.getArtifact().getFunctionsInOrder().add(
                    new TargetScwinFunctionModel("x", Collections.<String>emptyList(), ""));
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue("artifact-immutable: functions list unmodifiable", threw);
        threw = false;
        try {
            result.getArtifact().getFinalizedTargetFunctionIdentifiers().add("x");
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue("artifact-immutable: identifier index unmodifiable", threw);
    }

    private static void testArtifactZeroModelsEmptyIndex() {
        TargetScriptArtifact artifact = new TargetScriptArtifact(Collections.<TargetScwinFunctionModel>emptyList());
        assertEquals("artifact-zero-models: functions empty", "0", String.valueOf(artifact.getFunctionsInOrder().size()));
        assertEquals("artifact-zero-models: index empty", "0",
                String.valueOf(artifact.getFinalizedTargetFunctionIdentifiers().size()));
        assertEquals("artifact-zero-models: TargetScriptArtifact.empty() matches", "0",
                String.valueOf(TargetScriptArtifact.empty().getFinalizedTargetFunctionIdentifiers().size()));
    }

    private static void testArtifactTwoModelsOrderAndIndexExact() {
        List<TargetScwinFunctionModel> functions = new ArrayList<TargetScwinFunctionModel>();
        functions.add(new TargetScwinFunctionModel("a", Collections.<String>emptyList(), ""));
        functions.add(new TargetScwinFunctionModel("b", Collections.<String>emptyList(), ""));
        TargetScriptArtifact artifact = new TargetScriptArtifact(functions);
        assertEquals("artifact-two-models: order a,b",
                artifact.getFunctionsInOrder().get(0).getIdentifier() + ","
                        + artifact.getFunctionsInOrder().get(1).getIdentifier(), "a,b");
        assertEquals("artifact-two-models: index size", "2",
                String.valueOf(artifact.getFinalizedTargetFunctionIdentifiers().size()));
        assertTrue("artifact-two-models: index contains a", artifact.containsFinalizedTargetFunctionIdentifier("a"));
        assertTrue("artifact-two-models: index contains b", artifact.containsFinalizedTargetFunctionIdentifier("b"));
    }

    /** 공개 생성자는 정확히 하나이며 {@code List<TargetScwinFunctionModel>}만 받는다 --
     * backing 함수 모델 없는 identifier는 production API로 만들 수 없다. */
    private static void testArtifactExactlyOneProductionConstructorTakingFunctionModelList() {
        java.lang.reflect.Constructor<?>[] constructors = TargetScriptArtifact.class.getConstructors();
        assertEquals("artifact-single-constructor: exactly one public constructor", "1",
                String.valueOf(constructors.length));
        Class<?>[] paramTypes = constructors[0].getParameterTypes();
        assertEquals("artifact-single-constructor: exactly one parameter", "1", String.valueOf(paramTypes.length));
        assertEquals("artifact-single-constructor: parameter type is List", "interface java.util.List",
                paramTypes[0].toString());
    }

    /** functionsInOrder가 빈 리스트면 identifier index도 항상 빈 상태다 -- 유일한 생성자가
     * index를 함수 모델 리스트로부터만 도출하므로 구조적으로 불가능함을 직접 증명한다. */
    private static void testArtifactUnbackedIdentifierStructurallyImpossible() {
        TargetScriptArtifact fromEmptyList = new TargetScriptArtifact(Collections.<TargetScwinFunctionModel>emptyList());
        assertTrue("artifact-unbacked-identifier: empty function list can never produce a non-empty index",
                fromEmptyList.getFinalizedTargetFunctionIdentifiers().isEmpty());
        assertTrue("artifact-unbacked-identifier: containsFinalizedTargetFunctionIdentifier false for any name",
                !fromEmptyList.containsFinalizedTargetFunctionIdentifier("handleClick"));
    }

    /** braceless와 braced 형태의 단일 문장 control-flow body는 동일한 target body로
     * 번역돼야 한다(target 출력은 항상 중괄호로 정규화). */
    private static void testBracelessIfTranslatedSameAsBraced() {
        String braceless = translatedBody("function f(x){ if (x) return 1; return 0; }");
        String braced = translatedBody("function f(x){ if (x) { return 1; } return 0; }");
        assertEquals("braceless-if-translated: matches braced form", braced, braceless);
        assertEquals("braceless-if-translated: exact", "if (x) { return 1; } return 0;", braceless);
    }

    private static void testBracelessIfElseTranslatedSameAsBraced() {
        String braceless = translatedBody("function f(x){ if (x) return 1; else return 2; }");
        String braced = translatedBody("function f(x){ if (x) { return 1; } else { return 2; } }");
        assertEquals("braceless-if-else-translated: matches braced form", braced, braceless);
        assertEquals("braceless-if-else-translated: exact", "if (x) { return 1; } else { return 2; }", braceless);
    }

    private static void testBracelessWhileTranslatedSameAsBraced() {
        String braceless = translatedBody("function f(x){ while (x) x = x - 1; return x; }");
        String braced = translatedBody("function f(x){ while (x) { x = x - 1; } return x; }");
        assertEquals("braceless-while-translated: matches braced form", braced, braceless);
        assertEquals("braceless-while-translated: exact", "while (x) { x = (x - 1); } return x;", braceless);
    }

    private static void testBracelessForTranslatedSameAsBraced() {
        String braceless = translatedBody(
                "function f(n){ var total = 0; for (var i=0; i<n; i=i+1) total=total+i; return total; }");
        String braced = translatedBody(
                "function f(n){ var total = 0; for (var i=0; i<n; i=i+1) { total=total+i; } return total; }");
        assertEquals("braceless-for-translated: matches braced form", braced, braceless);
        assertEquals("braceless-for-translated: exact",
                "var total = 0; for (var i = 0; (i < n); i = (i + 1)) { total = (total + i); } return total;",
                braceless);
    }

    private static void testDanglingElseTranslatedPreservesNearestIfSemantics() {
        String body = translatedBody("function f(a,b){ if (a) if (b) return 1; else return 2; return 0; }");
        assertEquals("dangling-else-translated: else binds to inner if, exact",
                "if (a) { if (b) { return 1; } else { return 2; } } return 0;", body);
    }

    private static String translatedBody(String script) {
        TargetScriptTranslationResult result = new TargetScriptTranslator().translate(analyze(script));
        assertEquals("[" + script + "]: translation status", "TRANSLATED", String.valueOf(result.getStatus()));
        return result.getArtifact().getFunctionsInOrder().get(0).getFinalizedBodySource();
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
