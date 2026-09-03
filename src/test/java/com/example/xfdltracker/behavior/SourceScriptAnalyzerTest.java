package com.example.xfdltracker.behavior;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SourceScriptAnalyzer}에 대한 오프라인, 의존성 없는(no JUnit) 테스트 -- 일반
 * Source-Script 동작 검증 영역.
 */
public class SourceScriptAnalyzerTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testEmptyScript();
        testOneValidNamedFunction();
        testMultipleValidNamedFunctionsPreserveOrder();
        testParametersPreserveOrder();
        testDuplicateFunctionNameIntegrityViolation();
        testLocalVariableWithoutInitializer();
        testLocalVariableWithInitializer();
        testIdentifierAssignment();
        testReturnWithoutExpression();
        testReturnWithExpression();
        testIfStatement();
        testIfElseStatement();
        testWhileStatement();
        testClassicForWithIncrementAssignment();
        testBreakStatement();
        testContinueStatement();
        testExpressionStatementLocalCall();
        testLocalFunctionCallExpression();
        testEachAllowedMathCall();
        testEachSupportedUnaryOperator();
        testArithmeticOperators();
        testComparisonOperators();
        testLogicalOperators();
        testTernaryExpression();
        testCommentsAndWhitespaceNotSemanticAuthority();
        testForbiddenConstructsFailClosed();
        testDefect2TabDynamicNavigationMemberCallFailsClosedGenerically();

        // ==== 지원되는 단일 문장 제어흐름 body 관련 항목
        // (CONTROL_FLOW_BLOCK_BODY_SUPPORTED / CONTROL_FLOW_SINGLE_STATEMENT_BODY_SUPPORTED)도 포함 ====
        testBracelessIf();
        testBracelessIfElse();
        testMixedBlockThenSingleElseIf();
        testMixedSingleThenBlockElseIf();
        testDanglingElseNearestIfAssociation();
        testBracelessWhile();
        testBracelessClassicFor();
        testBracelessNestedControlFlow();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void testEmptyScript() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze("");
        assertEquals("empty-script: status", "ANALYZED", String.valueOf(result.getStatus()));
        assertEquals("empty-script: function count", "0",
                String.valueOf(result.getAnalysis().getFunctionsInDeclarationOrder().size()));
    }

    private static void testOneValidNamedFunction() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze("function foo(){ return 1; }");
        assertEquals("one-function: status", "ANALYZED", String.valueOf(result.getStatus()));
        List<SourceFunctionModel> functions = result.getAnalysis().getFunctionsInDeclarationOrder();
        assertEquals("one-function: count", "1", String.valueOf(functions.size()));
        assertEquals("one-function: name", "foo", functions.get(0).getDeclaredName());
    }

    private static void testMultipleValidNamedFunctionsPreserveOrder() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(
                "function a(){} function b(){} function c(){}");
        List<SourceFunctionModel> functions = result.getAnalysis().getFunctionsInDeclarationOrder();
        assertEquals("order: count", "3", String.valueOf(functions.size()));
        assertEquals("order: names", "a,b,c",
                functions.get(0).getDeclaredName() + "," + functions.get(1).getDeclaredName() + ","
                        + functions.get(2).getDeclaredName());
    }

    private static void testParametersPreserveOrder() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze("function f(a, b, c){}");
        SourceFunctionModel f = result.getAnalysis().getFunctionsInDeclarationOrder().get(0);
        assertEquals("params: order", "a,b,c", String.join(",", f.getParameterNames()));
    }

    private static void testDuplicateFunctionNameIntegrityViolation() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze("function f(){} function f(){}");
        assertEquals("duplicate: status", "INTEGRITY_VIOLATION", String.valueOf(result.getStatus()));
        assertTrue("duplicate: analysis absent", result.getAnalysis() == null);
        assertTrue("duplicate: reason present", result.getReason() != null && result.getReason().length() > 0);
    }

    private static SourceFunctionModel oneFunction(String script) {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(script);
        assertEquals("[" + script + "]: status", "ANALYZED", String.valueOf(result.getStatus()));
        return result.getAnalysis().getFunctionsInDeclarationOrder().get(0);
    }

    private static SourceStatementNode onlyStatement(String script) {
        List<SourceStatementNode> body = oneFunction(script).getBodyStatements();
        assertEquals("[" + script + "]: body statement count", "1", String.valueOf(body.size()));
        return body.get(0);
    }

    private static void testLocalVariableWithoutInitializer() {
        SourceStatementNode s = onlyStatement("function f(){ var x; }");
        assertEquals("var-no-init: kind", "VARIABLE_DECLARATION", String.valueOf(s.getKind()));
        assertEquals("var-no-init: identifier", "x", s.getIdentifier());
        assertTrue("var-no-init: initializer null", s.getInitializerOrAssignedExpression() == null);
    }

    private static void testLocalVariableWithInitializer() {
        SourceStatementNode s = onlyStatement("function f(){ var x = 1; }");
        assertEquals("var-init: kind", "VARIABLE_DECLARATION", String.valueOf(s.getKind()));
        assertTrue("var-init: initializer present", s.getInitializerOrAssignedExpression() != null);
        assertEquals("var-init: literal", "1", s.getInitializerOrAssignedExpression().getLiteralSource());
    }

    private static void testIdentifierAssignment() {
        SourceFunctionModel fn = oneFunction("function f(){ var x; x = 2; }");
        SourceStatementNode assign = fn.getBodyStatements().get(1);
        assertEquals("assign: kind", "IDENTIFIER_ASSIGNMENT", String.valueOf(assign.getKind()));
        assertEquals("assign: identifier", "x", assign.getIdentifier());
    }

    private static void testReturnWithoutExpression() {
        SourceStatementNode s = onlyStatement("function f(){ return; }");
        assertEquals("return-void: kind", "RETURN", String.valueOf(s.getKind()));
        assertTrue("return-void: expression null", s.getReturnExpression() == null);
    }

    private static void testReturnWithExpression() {
        SourceStatementNode s = onlyStatement("function f(){ return 1; }");
        assertEquals("return-expr: kind", "RETURN", String.valueOf(s.getKind()));
        assertTrue("return-expr: expression present", s.getReturnExpression() != null);
    }

    private static void testIfStatement() {
        SourceStatementNode s = onlyStatement("function f(){ if (1) { return 1; } }");
        assertEquals("if: kind", "IF_ELSE", String.valueOf(s.getKind()));
        assertTrue("if: elseBranch absent", s.getElseBranch() == null);
        assertEquals("if: then size", "1", String.valueOf(s.getThenBranch().size()));
    }

    private static void testIfElseStatement() {
        SourceStatementNode s = onlyStatement("function f(){ if (1) { return 1; } else { return 2; } }");
        assertEquals("if-else: kind", "IF_ELSE", String.valueOf(s.getKind()));
        assertTrue("if-else: elseBranch present", s.getElseBranch() != null);
        assertEquals("if-else: else size", "1", String.valueOf(s.getElseBranch().size()));
    }

    private static void testWhileStatement() {
        SourceStatementNode s = onlyStatement("function f(){ while (1) { break; } }");
        assertEquals("while: kind", "WHILE", String.valueOf(s.getKind()));
        assertEquals("while: body size", "1", String.valueOf(s.getBody().size()));
    }

    private static void testClassicForWithIncrementAssignment() {
        SourceStatementNode s = onlyStatement("function f(){ for (var i = 0; i < 10; i = i + 1) { continue; } }");
        assertEquals("for: kind", "CLASSIC_FOR", String.valueOf(s.getKind()));
        assertEquals("for: init kind", "VARIABLE_DECLARATION", String.valueOf(s.getForInit().getKind()));
        assertEquals("for: update kind", "IDENTIFIER_ASSIGNMENT", String.valueOf(s.getForUpdate().getKind()));
        assertEquals("for: update identifier", "i", s.getForUpdate().getIdentifier());
    }

    private static void testBreakStatement() {
        SourceStatementNode s = onlyStatement("function f(){ while(1){ break; } }").getBody().get(0);
        assertEquals("break: kind", "BREAK", String.valueOf(s.getKind()));
    }

    private static void testContinueStatement() {
        SourceStatementNode s = onlyStatement("function f(){ while(1){ continue; } }").getBody().get(0);
        assertEquals("continue: kind", "CONTINUE", String.valueOf(s.getKind()));
    }

    private static void testExpressionStatementLocalCall() {
        SourceFunctionModel fn = oneFunction("function f(){ g(); } function g(){}");
        SourceStatementNode s = fn.getBodyStatements().get(0);
        assertEquals("expr-stmt: kind", "EXPRESSION_STATEMENT", String.valueOf(s.getKind()));
        assertEquals("expr-stmt: expr kind", "LOCAL_FUNCTION_CALL",
                String.valueOf(s.getExpressionStatementExpression().getKind()));
    }

    private static void testLocalFunctionCallExpression() {
        SourceStatementNode s = onlyStatement("function f(){ return g(1, 2); }");
        SourceExpressionNode call = s.getReturnExpression();
        assertEquals("call: kind", "LOCAL_FUNCTION_CALL", String.valueOf(call.getKind()));
        assertEquals("call: target", "g", call.getIdentifierName());
        assertEquals("call: arg count", "2", String.valueOf(call.getCallArguments().size()));
    }

    private static void testEachAllowedMathCall() {
        String[] members = {"abs", "max", "min", "floor", "ceil", "round"};
        for (String member : members) {
            SourceStatementNode s = onlyStatement("function f(){ return Math." + member + "(1); }");
            SourceExpressionNode expr = s.getReturnExpression();
            assertEquals("math-" + member + ": kind", "ALLOWED_MATH_CALL", String.valueOf(expr.getKind()));
            assertEquals("math-" + member + ": member", member, expr.getMathMemberName());
        }
    }

    private static void testEachSupportedUnaryOperator() {
        Map<String, String> forms = new LinkedHashMap<String, String>();
        forms.put("!", "!x");
        forms.put("-", "-x");
        forms.put("+", "+x");
        forms.put("typeof", "typeof x");
        for (Map.Entry<String, String> entry : forms.entrySet()) {
            SourceFunctionModel fn = oneFunction("function f(){ var x = 1; return " + entry.getValue() + "; }");
            SourceExpressionNode expr = fn.getBodyStatements().get(1).getReturnExpression();
            assertEquals("unary-" + entry.getKey() + ": kind", "UNARY", String.valueOf(expr.getKind()));
            assertEquals("unary-" + entry.getKey() + ": operator", entry.getKey(), expr.getUnaryOperator());
        }
    }

    private static void testArithmeticOperators() {
        String[] ops = {"+", "-", "*", "/", "%"};
        for (String op : ops) {
            SourceStatementNode s = onlyStatement("function f(){ return 1 " + op + " 2; }");
            SourceExpressionNode expr = s.getReturnExpression();
            assertEquals("arith-" + op + ": kind", "BINARY", String.valueOf(expr.getKind()));
            assertEquals("arith-" + op + ": operator", op, expr.getBinaryOperator());
        }
    }

    private static void testComparisonOperators() {
        String[] ops = {"==", "===", "!=", "!==", "<", "<=", ">", ">="};
        for (String op : ops) {
            SourceStatementNode s = onlyStatement("function f(){ return 1 " + op + " 2; }");
            SourceExpressionNode expr = s.getReturnExpression();
            assertEquals("cmp-" + op + ": kind", "BINARY", String.valueOf(expr.getKind()));
            assertEquals("cmp-" + op + ": operator", op, expr.getBinaryOperator());
        }
    }

    private static void testLogicalOperators() {
        String[] ops = {"&&", "||"};
        for (String op : ops) {
            SourceStatementNode s = onlyStatement("function f(){ return 1 " + op + " 2; }");
            SourceExpressionNode expr = s.getReturnExpression();
            assertEquals("logical-" + op + ": kind", "LOGICAL", String.valueOf(expr.getKind()));
            assertEquals("logical-" + op + ": operator", op, expr.getLogicalOperator());
        }
    }

    private static void testTernaryExpression() {
        SourceStatementNode s = onlyStatement("function f(){ return 1 ? 2 : 3; }");
        SourceExpressionNode expr = s.getReturnExpression();
        assertEquals("ternary: kind", "CONDITIONAL", String.valueOf(expr.getKind()));
    }

    private static void testCommentsAndWhitespaceNotSemanticAuthority() {
        SourceScriptAnalysisResult withComments = new SourceScriptAnalyzer().analyze(
                "// leading comment\nfunction f( a , b ){ /* inline */ return a + b; } // trailing\n");
        SourceScriptAnalysisResult withoutComments = new SourceScriptAnalyzer().analyze("function f(a,b){return a+b;}");
        assertEquals("comments: status", "ANALYZED", String.valueOf(withComments.getStatus()));
        SourceFunctionModel a = withComments.getAnalysis().getFunctionsInDeclarationOrder().get(0);
        SourceFunctionModel b = withoutComments.getAnalysis().getFunctionsInDeclarationOrder().get(0);
        assertEquals("comments: same param count", String.valueOf(b.getParameterNames().size()),
                String.valueOf(a.getParameterNames().size()));
        assertEquals("comments: same body statement kind", String.valueOf(b.getBodyStatements().get(0).getKind()),
                String.valueOf(a.getBodyStatements().get(0).getKind()));
    }

    private static void testForbiddenConstructsFailClosed() {
        Map<String, String> forbidden = new LinkedHashMap<String, String>();
        forbidden.put("array literal", "function f(){ var x = [1,2]; }");
        forbidden.put("object literal", "function f(){ var x = {a:1}; }");
        forbidden.put("member read", "function f(){ return x.y; }");
        forbidden.put("member write", "function f(){ x.y = 1; }");
        forbidden.put("source component API form", "function f(){ return comp.getValue(); }");
        forbidden.put("unknown global call", "function f(){ alert(1); }");
        forbidden.put("uc.*", "function f(){ uc.doSomething(); }");
        forbidden.put("this", "function f(){ return this; }");
        forbidden.put("Date", "function f(){ return Date; }");
        forbidden.put("Promise", "function f(){ return Promise; }");
        forbidden.put("async", "function f(){ return async; }");
        forbidden.put("await", "function f(){ return await; }");
        forbidden.put("setTimeout", "function f(){ setTimeout(g, 1); }");
        forbidden.put("setInterval", "function f(){ setInterval(g, 1); }");
        forbidden.put("eval", "function f(){ eval(x); }");
        forbidden.put("Function constructor", "function f(){ return Function; }");
        forbidden.put("JSON", "function f(){ return JSON; }");
        forbidden.put("try", "function f(){ try { return 1; } catch(e) {} }");
        forbidden.put("catch", "function f(){ try {} catch(e) { return 1; } }");
        forbidden.put("throw", "function f(){ throw x; }");
        forbidden.put("switch", "function f(){ switch(x){} }");
        forbidden.put("new", "function f(){ return new x(); }");
        forbidden.put("bitwise", "function f(){ return 1 & 2; }");
        forbidden.put("++", "function f(){ var i = 0; i++; }");
        forbidden.put("--", "function f(){ var i = 0; i--; }");
        forbidden.put("comma", "function f(){ return 1, 2; }");
        forbidden.put("instanceof", "function f(){ return x instanceof y; }");
        forbidden.put("in", "function f(){ return x in y; }");
        forbidden.put("for-in", "function f(){ for (x in y) { return 1; } }");
        forbidden.put("for-of", "function f(){ for (x of y) { return 1; } }");
        forbidden.put("destructuring", "function f(){ var {a,b} = x; }");
        forbidden.put("default parameter", "function f(a = 1){}");

        for (Map.Entry<String, String> entry : forbidden.entrySet()) {
            SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(entry.getValue());
            assertEquals("forbidden[" + entry.getKey() + "]: status", "UNSUPPORTED_SYNTAX",
                    String.valueOf(result.getStatus()));
            assertTrue("forbidden[" + entry.getKey() + "]: analysis absent", result.getAnalysis() == null);
        }
    }

    /**
     * Slice 99A(Defect 2 closure) -- Tab 동적 navigation({@code setUrl}/{@code addTab})은 {@code identifier.member}
     * 형태이므로 항상 UNSUPPORTED_SYNTAX로 거부되며, 이유 문구는 화면 이름이 아닌 식별자/구문 범주만 담는다.
     * 두 fixture(setUrl/addTab, 서로 다른 식별자명)로 화면-특정이 아닌 일반 계약임을 증명한다.
     */
    private static void testDefect2TabDynamicNavigationMemberCallFailsClosedGenerically() {
        SourceScriptAnalysisResult setUrlResult =
                new SourceScriptAnalyzer().analyze("function f(){ tabA.setUrl('formA'); }");
        assertEquals("defect2-setUrl: status", "UNSUPPORTED_SYNTAX", String.valueOf(setUrlResult.getStatus()));
        assertTrue("defect2-setUrl: reason names member-access category, not a screen id",
                setUrlResult.getReason() != null
                        && setUrlResult.getReason().contains("unsupported arbitrary member access on identifier"));

        SourceScriptAnalysisResult addTabResult =
                new SourceScriptAnalyzer().analyze("function f(){ myTabControl.addTab('pageId', 'label'); }");
        assertEquals("defect2-addTab: status", "UNSUPPORTED_SYNTAX", String.valueOf(addTabResult.getStatus()));
        assertTrue("defect2-addTab: reason names member-access category, not a screen id",
                addTabResult.getReason() != null
                        && addTabResult.getReason().contains("unsupported arbitrary member access on identifier"));
    }

    private static void testBracelessIf() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(
                "function f(x){ if (x) return 1; return 0; }");
        assertEquals("braceless-if: status", "ANALYZED", String.valueOf(result.getStatus()));
        SourceFunctionModel fn = result.getAnalysis().getFunctionsInDeclarationOrder().get(0);
        SourceStatementNode ifNode = fn.getBodyStatements().get(0);
        assertEquals("braceless-if: kind", "IF_ELSE", String.valueOf(ifNode.getKind()));
        assertEquals("braceless-if: thenBranch size", "1", String.valueOf(ifNode.getThenBranch().size()));
        assertEquals("braceless-if: then statement kind", "RETURN",
                String.valueOf(ifNode.getThenBranch().get(0).getKind()));
        assertTrue("braceless-if: no elseBranch", ifNode.getElseBranch() == null);
    }

    private static void testBracelessIfElse() {
        SourceFunctionModel fn = oneFunction("function f(x){ if (x) return 1; else return 2; }");
        SourceStatementNode ifNode = fn.getBodyStatements().get(0);
        assertEquals("braceless-if-else: thenBranch size", "1", String.valueOf(ifNode.getThenBranch().size()));
        assertTrue("braceless-if-else: elseBranch present", ifNode.getElseBranch() != null);
        assertEquals("braceless-if-else: elseBranch size", "1", String.valueOf(ifNode.getElseBranch().size()));
        assertEquals("braceless-if-else: then kind", "RETURN", String.valueOf(ifNode.getThenBranch().get(0).getKind()));
        assertEquals("braceless-if-else: else kind", "RETURN", String.valueOf(ifNode.getElseBranch().get(0).getKind()));
    }

    private static void testMixedBlockThenSingleElseIf() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(
                "function f(x){ if (x) { return 1; } else return 2; }");
        assertEquals("mixed-block-then-single-else: status", "ANALYZED", String.valueOf(result.getStatus()));
        SourceStatementNode ifNode = result.getAnalysis().getFunctionsInDeclarationOrder().get(0)
                .getBodyStatements().get(0);
        assertEquals("mixed-block-then-single-else: thenBranch size", "1", String.valueOf(ifNode.getThenBranch().size()));
        assertEquals("mixed-block-then-single-else: elseBranch size", "1", String.valueOf(ifNode.getElseBranch().size()));
    }

    private static void testMixedSingleThenBlockElseIf() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(
                "function f(x){ if (x) return 1; else { return 2; } }");
        assertEquals("mixed-single-then-block-else: status", "ANALYZED", String.valueOf(result.getStatus()));
        SourceStatementNode ifNode = result.getAnalysis().getFunctionsInDeclarationOrder().get(0)
                .getBodyStatements().get(0);
        assertEquals("mixed-single-then-block-else: thenBranch size", "1", String.valueOf(ifNode.getThenBranch().size()));
        assertEquals("mixed-single-then-block-else: elseBranch size", "1", String.valueOf(ifNode.getElseBranch().size()));
    }

    /** JavaScript의 nearest-unmatched-if 의미론: {@code else}는 {@code if (a)}가 아니라
     * {@code if (b)}에 바인딩되어야 한다. 이는 오직 파서 문법(recursive descent)만으로
     * 결정되며, 소스 들여쓰기/공백과는 무관하다. */
    private static void testDanglingElseNearestIfAssociation() {
        SourceScriptAnalysisResult result = new SourceScriptAnalyzer().analyze(
                "function f(a,b){ if (a) if (b) return 1; else return 2; return 0; }");
        assertEquals("dangling-else: status", "ANALYZED", String.valueOf(result.getStatus()));
        List<SourceStatementNode> body = result.getAnalysis().getFunctionsInDeclarationOrder().get(0)
                .getBodyStatements();
        assertEquals("dangling-else: outer body statement count", "2", String.valueOf(body.size()));
        SourceStatementNode outerIf = body.get(0);
        assertEquals("dangling-else: outer kind", "IF_ELSE", String.valueOf(outerIf.getKind()));
        assertTrue("dangling-else: outer has no elseBranch", outerIf.getElseBranch() == null);
        assertEquals("dangling-else: outer thenBranch size", "1", String.valueOf(outerIf.getThenBranch().size()));
        SourceStatementNode innerIf = outerIf.getThenBranch().get(0);
        assertEquals("dangling-else: inner kind", "IF_ELSE", String.valueOf(innerIf.getKind()));
        assertTrue("dangling-else: inner has elseBranch (else binds to nearest if)", innerIf.getElseBranch() != null);
        assertEquals("dangling-else: inner elseBranch size", "1", String.valueOf(innerIf.getElseBranch().size()));
        assertEquals("dangling-else: inner then kind", "RETURN", String.valueOf(innerIf.getThenBranch().get(0).getKind()));
        assertEquals("dangling-else: inner else kind", "RETURN", String.valueOf(innerIf.getElseBranch().get(0).getKind()));
    }

    private static void testBracelessWhile() {
        SourceFunctionModel fn = oneFunction("function f(x){ while (x) x = x - 1; return x; }");
        SourceStatementNode whileNode = fn.getBodyStatements().get(0);
        assertEquals("braceless-while: kind", "WHILE", String.valueOf(whileNode.getKind()));
        assertEquals("braceless-while: body size", "1", String.valueOf(whileNode.getBody().size()));
        assertEquals("braceless-while: body statement kind", "IDENTIFIER_ASSIGNMENT",
                String.valueOf(whileNode.getBody().get(0).getKind()));
    }

    private static void testBracelessClassicFor() {
        SourceFunctionModel fn = oneFunction(
                "function f(n){ var total = 0; for (var i=0; i<n; i=i+1) total=total+i; return total; }");
        SourceStatementNode forNode = fn.getBodyStatements().get(1);
        assertEquals("braceless-for: kind", "CLASSIC_FOR", String.valueOf(forNode.getKind()));
        assertEquals("braceless-for: body size", "1", String.valueOf(forNode.getBody().size()));
        assertEquals("braceless-for: body statement kind", "IDENTIFIER_ASSIGNMENT",
                String.valueOf(forNode.getBody().get(0).getKind()));
    }

    private static void testBracelessNestedControlFlow() {
        SourceFunctionModel fn = oneFunction("function f(x){ if (x) while (x) x = x - 1; return x; }");
        SourceStatementNode ifNode = fn.getBodyStatements().get(0);
        assertEquals("braceless-nested: outer kind", "IF_ELSE", String.valueOf(ifNode.getKind()));
        assertEquals("braceless-nested: outer thenBranch size", "1", String.valueOf(ifNode.getThenBranch().size()));
        SourceStatementNode whileNode = ifNode.getThenBranch().get(0);
        assertEquals("braceless-nested: inner kind", "WHILE", String.valueOf(whileNode.getKind()));
        assertEquals("braceless-nested: inner body size", "1", String.valueOf(whileNode.getBody().size()));
        assertEquals("braceless-nested: inner body statement kind", "IDENTIFIER_ASSIGNMENT",
                String.valueOf(whileNode.getBody().get(0).getKind()));
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
