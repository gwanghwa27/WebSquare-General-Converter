package com.example.xfdltracker.runtime;

import java.util.Set;

/**
 * 외부 의존성 없는(non-JUnit) {@link RuntimeFunctionCallAnalyzer} 단위 테스트.
 */
public class RuntimeFunctionCallAnalyzerTest {

    private static int failures = 0;
    private static final CommonRuntimeCapabilityCatalog CATALOG = CommonRuntimeCapabilityCatalog.createSeeded();

    public static void main(String[] args) {
        // 직접 호출 성공 사례
        testSimpleDirectCall();
        testWhitespaceBetweenTokens();
        testCommentBetweenTokens();
        testNewlineBeforeParen();
        testTwoDistinctCallsProduceTwoRequirements();
        testRepeatedSameCapabilityDeduplicates();

        // 코드가 아닌 대상 제외
        testSingleQuotedStringIgnored();
        testDoubleQuotedStringIgnored();
        testLineCommentIgnored();
        testBlockCommentIgnored();
        testPlainTemplateTextIgnored();
        testRegexLiteralIgnored();
        testRegexCharacterClassIgnored();

        // 템플릿 실행
        testTemplateInterpolationRecognized();
        testNestedInterpolationBracesDoNotTerminateEarly();
        testMalformedTemplateFailsClosed();

        // 슬래시 동작
        testDivisionNotTreatedAsRegex();
        testRegexAtExpressionStartProducesNoRequirement();
        testRegexAfterControlHeaderCloseRecognized();
        testExpressionParenCloseThenSlashIsDivision();
        testUnterminatedRegexFailsClosed();
        testEscapedSlashInRegexHandled();

        // namespace 한정
        testKnownAliasRecognized();
        testQualifiedPropertyUcNotGlobal();
        testWhitespaceSeparatedQualifiedPropertyUcNotGlobal();
        testCommentSeparatedQualifiedPropertyUcNotGlobal();
        testNewlineSeparatedQualifiedPropertyUcNotGlobal();
        testUnknownAliasFailsClosed();
        testComputedUcPropertyFailsClosed();
        testUcMemberReferenceWithoutCallFailsClosed();
        testFirstClassUcUseFailsClosed();
        testOptionalChainingUcFailsClosed();

        // 예약된 binding
        testVarUcFailsClosed();
        testLetUcFailsClosed();
        testConstUcFailsClosed();
        testFunctionDeclarationNamedUcFailsClosed();
        testFunctionParameterUcFailsClosed();
        testCatchParameterUcFailsClosed();
        testDestructuringBindingWithUcFailsClosed();

        // namespace 변경
        testUcAssignmentFailsClosed();
        testUcCompoundAssignmentFailsClosed();
        testUcPostfixIncrementFailsClosed();
        testUcPrefixIncrementFailsClosed();
        testDeleteUcFailsClosed();

        // catalog semantics 검증
        testResolvedRequirementsContainOnlyCanonicalIds();
        testEmptyScriptReturnsEmptyRequirements();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static RuntimeRequirementSet analyze(String script) {
        return new RuntimeFunctionCallAnalyzer().analyze(script, CATALOG);
    }

    private static boolean throwsUnsupported(String script) {
        try {
            analyze(script);
            return false;
        } catch (RuntimeFunctionCallAnalyzer.UnsupportedRuntimeSyntaxException e) {
            return true;
        }
    }

    private static void testSimpleDirectCall() {
        Set<String> ids = analyze("uc.tranSend(a, b);").getRequiredCapabilityIds();
        assertTrue("simple direct call recognized", ids.contains("TRANSACTION_SEND"));
    }

    private static void testWhitespaceBetweenTokens() {
        Set<String> ids = analyze("uc . tranSend ( a );").getRequiredCapabilityIds();
        assertTrue("whitespace between tokens recognized", ids.contains("TRANSACTION_SEND"));
    }

    private static void testCommentBetweenTokens() {
        Set<String> ids = analyze("uc /* c */ . /* c */ tranSend /* c */ (a);").getRequiredCapabilityIds();
        assertTrue("comment between allowed tokens recognized", ids.contains("TRANSACTION_SEND"));
    }

    private static void testNewlineBeforeParen() {
        Set<String> ids = analyze("uc.tranSend\n(\na\n);").getRequiredCapabilityIds();
        assertTrue("newline before opening parenthesis recognized", ids.contains("TRANSACTION_SEND"));
    }

    private static void testTwoDistinctCallsProduceTwoRequirements() {
        Set<String> ids = analyze("uc.tranSend(a); uc.getCommonCode(b);").getRequiredCapabilityIds();
        assertTrue("two distinct calls -> two requirements",
                ids.contains("TRANSACTION_SEND") && ids.contains("COMMON_CODE_GET") && ids.size() == 2);
    }

    private static void testRepeatedSameCapabilityDeduplicates() {
        Set<String> ids = analyze("uc.tranSend(a); uc.tranSend(b);").getRequiredCapabilityIds();
        assertTrue("repeated same capability deduplicates", ids.size() == 1);
    }

    private static void testSingleQuotedStringIgnored() {
        Set<String> ids = analyze("var s = 'uc.tranSend(a)';").getRequiredCapabilityIds();
        assertTrue("uc call inside single-quoted string ignored", ids.isEmpty());
    }

    private static void testDoubleQuotedStringIgnored() {
        Set<String> ids = analyze("var s = \"uc.tranSend(a)\";").getRequiredCapabilityIds();
        assertTrue("uc call inside double-quoted string ignored", ids.isEmpty());
    }

    private static void testLineCommentIgnored() {
        Set<String> ids = analyze("// uc.tranSend(a)\nvar x = 1;").getRequiredCapabilityIds();
        assertTrue("uc call inside line comment ignored", ids.isEmpty());
    }

    private static void testBlockCommentIgnored() {
        Set<String> ids = analyze("/* uc.tranSend(a) */ var x = 1;").getRequiredCapabilityIds();
        assertTrue("uc call inside block comment ignored", ids.isEmpty());
    }

    private static void testPlainTemplateTextIgnored() {
        Set<String> ids = analyze("var s = `text uc.tranSend(a) more text`;").getRequiredCapabilityIds();
        assertTrue("uc call inside plain template text ignored", ids.isEmpty());
    }

    private static void testRegexLiteralIgnored() {
        Set<String> ids = analyze("var r = /uc\\.tranSend\\(/;").getRequiredCapabilityIds();
        assertTrue("uc-like text inside regex literal ignored", ids.isEmpty());
    }

    private static void testRegexCharacterClassIgnored() {
        Set<String> ids = analyze("var r = /[uc./tranSend(]/;").getRequiredCapabilityIds();
        assertTrue("uc-like text with slash inside regex character class ignored", ids.isEmpty());
    }

    private static void testTemplateInterpolationRecognized() {
        Set<String> ids = analyze("var s = `${uc.tranSend(a)}`;").getRequiredCapabilityIds();
        assertTrue("uc call inside template interpolation recognized", ids.contains("TRANSACTION_SEND"));
    }

    private static void testNestedInterpolationBracesDoNotTerminateEarly() {
        Set<String> ids = analyze("var s = `${ (function(){ if (true) { } return uc.tranSend(a); })() }`;")
                .getRequiredCapabilityIds();
        assertTrue("nested object/block braces inside interpolation do not terminate it early",
                ids.contains("TRANSACTION_SEND"));
    }

    private static void testMalformedTemplateFailsClosed() {
        assertTrue("unterminated template literal fails closed", throwsUnsupported("var s = `abc"));
        assertTrue("unterminated template interpolation fails closed", throwsUnsupported("var s = `${abc`;"));
    }

    private static void testDivisionNotTreatedAsRegex() {
        Set<String> ids = analyze("var x = a / b; uc.tranSend(x);").getRequiredCapabilityIds();
        assertTrue("division expression does not become regex and does not block later recognition",
                ids.contains("TRANSACTION_SEND"));
    }

    private static void testRegexAtExpressionStartProducesNoRequirement() {
        Set<String> ids = analyze("var r = /uc.tranSend/; uc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("regex literal at lawful expression-start context produces no requirement from its own text, "
                        + "later real call still recognized",
                ids.size() == 1 && ids.contains("TRANSACTION_SEND"));
    }

    private static void testRegexAfterControlHeaderCloseRecognized() {
        Set<String> ids = analyze("if (a) /pattern/.test(value); uc.tranSend(x);").getRequiredCapabilityIds();
        assertTrue("regex after if-condition control-header close is recognized as regex, not division "
                        + "(script does not fail closed on ambiguous slash here)",
                ids.contains("TRANSACTION_SEND"));
    }

    private static void testExpressionParenCloseThenSlashIsDivision() {
        Set<String> ids = analyze("var y = (a + b) / c; uc.tranSend(x);").getRequiredCapabilityIds();
        assertTrue("confirmed expression-paren close followed by slash treated as division",
                ids.contains("TRANSACTION_SEND"));
    }

    private static void testUnterminatedRegexFailsClosed() {
        assertTrue("unterminated regex literal fails closed", throwsUnsupported("var r = /abc"));
    }

    private static void testEscapedSlashInRegexHandled() {
        Set<String> ids = analyze("var r = /a\\/b/; uc.tranSend(x);").getRequiredCapabilityIds();
        assertTrue("escaped slash inside regex handled, does not terminate regex early",
                ids.contains("TRANSACTION_SEND"));
    }

    private static void testKnownAliasRecognized() {
        Set<String> ids = analyze("uc.getCommonCode(a);").getRequiredCapabilityIds();
        assertTrue("known documented alias resolves through catalog", ids.contains("COMMON_CODE_GET"));
    }

    private static void testQualifiedPropertyUcNotGlobal() {
        Set<String> ids = analyze("object.uc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("object.uc.tranSend(...) does not resolve as global uc namespace", ids.isEmpty());
    }

    /** qualification 회귀 검증: 점(.) 앞뒤에 공백이 있는 경우에도 global uc namespace로 해석되면 안 된다. */
    private static void testWhitespaceSeparatedQualifiedPropertyUcNotGlobal() {
        Set<String> ids = analyze("object . uc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("object . uc.tranSend(...) (whitespace around dot) does not resolve as global uc namespace",
                ids.isEmpty());
    }

    /** qualification 회귀 검증: 점(.) 사이에 comment가 끼어 있는 경우에도 global uc namespace로 해석되면 안 된다. */
    private static void testCommentSeparatedQualifiedPropertyUcNotGlobal() {
        Set<String> ids = analyze("object./*comment*/uc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("object./*comment*/uc.tranSend(...) does not resolve as global uc namespace", ids.isEmpty());
    }

    /** qualification 회귀 검증: 점(.) 앞뒤에 줄바꿈이 있는 경우에도 global uc namespace로 해석되면 안 된다. */
    private static void testNewlineSeparatedQualifiedPropertyUcNotGlobal() {
        Set<String> ids = analyze("object\n.\nuc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("object <newline> . <newline> uc.tranSend(...) does not resolve as global uc namespace",
                ids.isEmpty());
    }

    private static void testUnknownAliasFailsClosed() {
        assertTrue("unknown direct alias fails closed", throwsUnsupported("uc.someUnknownFunction(a);"));
    }

    private static void testComputedUcPropertyFailsClosed() {
        assertTrue("computed uc property access fails closed", throwsUnsupported("uc['tranSend'](a);"));
    }

    private static void testUcMemberReferenceWithoutCallFailsClosed() {
        assertTrue("uc member reference without immediate call fails closed", throwsUnsupported("var f = uc.tranSend;"));
    }

    private static void testFirstClassUcUseFailsClosed() {
        assertTrue("first-class uc use fails closed", throwsUnsupported("var common = uc;"));
    }

    private static void testOptionalChainingUcFailsClosed() {
        assertTrue("uc?.tranSend(...) fails closed", throwsUnsupported("uc?.tranSend(a);"));
        assertTrue("uc.tranSend?.(...) fails closed", throwsUnsupported("uc.tranSend?.(a);"));
    }

    private static void testVarUcFailsClosed() {
        assertTrue("var uc fails closed", throwsUnsupported("var uc = 1; uc.tranSend(a);"));
    }

    private static void testLetUcFailsClosed() {
        assertTrue("let uc fails closed", throwsUnsupported("let uc = 1;"));
    }

    private static void testConstUcFailsClosed() {
        assertTrue("const uc fails closed", throwsUnsupported("const uc = 1;"));
    }

    private static void testFunctionDeclarationNamedUcFailsClosed() {
        assertTrue("function uc(...) fails closed", throwsUnsupported("function uc() {}"));
    }

    private static void testFunctionParameterUcFailsClosed() {
        assertTrue("function parameter named uc fails closed", throwsUnsupported("function f(uc) { return uc; }"));
    }

    private static void testCatchParameterUcFailsClosed() {
        assertTrue("catch parameter named uc fails closed", throwsUnsupported("try { a(); } catch (uc) { }"));
    }

    private static void testDestructuringBindingWithUcFailsClosed() {
        assertTrue("simple destructuring binding with uc fails closed", throwsUnsupported("var {uc} = obj;"));
    }

    private static void testUcAssignmentFailsClosed() {
        assertTrue("uc = value fails closed", throwsUnsupported("uc = value;"));
    }

    private static void testUcCompoundAssignmentFailsClosed() {
        assertTrue("uc += value fails closed", throwsUnsupported("uc += value;"));
    }

    private static void testUcPostfixIncrementFailsClosed() {
        assertTrue("uc++ fails closed", throwsUnsupported("uc++;"));
    }

    private static void testUcPrefixIncrementFailsClosed() {
        assertTrue("++uc fails closed", throwsUnsupported("++uc;"));
    }

    private static void testDeleteUcFailsClosed() {
        assertTrue("delete uc fails closed", throwsUnsupported("delete uc;"));
    }

    private static void testResolvedRequirementsContainOnlyCanonicalIds() {
        Set<String> ids = analyze("uc.tranSend(a);").getRequiredCapabilityIds();
        assertTrue("resolved requirement set contains canonical id, not the alias text",
                ids.contains("TRANSACTION_SEND") && !ids.contains("uc.tranSend"));
    }

    private static void testEmptyScriptReturnsEmptyRequirements() {
        assertTrue("empty script returns empty requirement set", analyze("").isEmpty());
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
