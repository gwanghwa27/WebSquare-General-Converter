package com.example.xfdltracker.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 구조화된 {@link SourceScriptAnalysis} 모델만 소비한다(원본 재파싱/DOM 접근/실행 없음). 소스
 * 함수마다 정확히 하나의 {@link TargetScwinFunctionModel}을 생성하며, 번역 불가 함수가 있으면
 * 전체 번역을 중단한다. 대상 식별자는 소스 선언명을 그대로 사용하며 중복은 {@link TargetScriptArtifact}가 차단한다.
 */
public final class TargetScriptTranslator {

    private static final Set<String> VENDOR_RESERVED_IDENTIFIERS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("onpageload", "onpageunload")));

    private static final Pattern TARGET_IDENTIFIER_GRAMMAR = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*$");

    private static final class UnresolvedFunctionReferenceException extends RuntimeException {
        UnresolvedFunctionReferenceException(String message) { super(message); }
    }

    public TargetScriptTranslationResult translate(SourceScriptAnalysis analysis) {
        if (analysis == null) {
            return TargetScriptTranslationResult.failure(
                    TargetTranslationStatus.INTEGRITY_VIOLATION,
                    "target_script_translator: analysis must not be null");
        }
        List<TargetScwinFunctionModel> functions = new ArrayList<TargetScwinFunctionModel>();
        try {
            for (SourceFunctionModel source : analysis.getFunctionsInDeclarationOrder()) {
                String declaredName = source.getDeclaredName();
                if (VENDOR_RESERVED_IDENTIFIERS.contains(declaredName)) {
                    return TargetScriptTranslationResult.failure(
                            TargetTranslationStatus.INTEGRITY_VIOLATION,
                            "target_script_translator: source function declaration uses a vendor-reserved v1 "
                                    + "identifier -- \"" + declaredName + "\"");
                }
                if (!TARGET_IDENTIFIER_GRAMMAR.matcher(declaredName).matches()) {
                    return TargetScriptTranslationResult.failure(
                            TargetTranslationStatus.INTEGRITY_VIOLATION,
                            "target_script_translator: source declaration name is not a target-compatible "
                                    + "identifier -- \"" + declaredName + "\"");
                }
                String finalizedBodySource = serializeStatements(source.getBodyStatements(), analysis);
                functions.add(new TargetScwinFunctionModel(declaredName, source.getParameterNames(), finalizedBodySource));
            }
        } catch (UnresolvedFunctionReferenceException unresolved) {
            return TargetScriptTranslationResult.failure(
                    TargetTranslationStatus.UNRESOLVED_FUNCTION_REFERENCE, unresolved.getMessage());
        }
        return TargetScriptTranslationResult.translated(new TargetScriptArtifact(functions));
    }

    // ==== 결정적 target body 직렬화(closed 모델만, 원본 source 텍스트 없음) ====

    private String serializeStatements(List<SourceStatementNode> statements, SourceScriptAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        for (SourceStatementNode statement : statements) {
            if (sb.length() > 0) { sb.append(' '); }
            sb.append(serializeStatement(statement, analysis));
        }
        return sb.toString();
    }

    private String serializeStatement(SourceStatementNode node, SourceScriptAnalysis analysis) {
        switch (node.getKind()) {
            case VARIABLE_DECLARATION: {
                String init = node.getInitializerOrAssignedExpression() == null ? ""
                        : " = " + serializeExpression(node.getInitializerOrAssignedExpression(), analysis);
                return "var " + node.getIdentifier() + init + ";";
            }
            case IDENTIFIER_ASSIGNMENT:
                return node.getIdentifier() + " = "
                        + serializeExpression(node.getInitializerOrAssignedExpression(), analysis) + ";";
            case RETURN: {
                String expr = node.getReturnExpression() == null ? ""
                        : " " + serializeExpression(node.getReturnExpression(), analysis);
                return "return" + expr + ";";
            }
            case IF_ELSE: {
                StringBuilder sb = new StringBuilder();
                sb.append("if (").append(serializeExpression(node.getCondition(), analysis)).append(") { ")
                        .append(serializeStatements(node.getThenBranch(), analysis)).append(" }");
                if (node.getElseBranch() != null) {
                    sb.append(" else { ").append(serializeStatements(node.getElseBranch(), analysis)).append(" }");
                }
                return sb.toString();
            }
            case CLASSIC_FOR: {
                String init = serializeForClause(node.getForInit(), analysis);
                String update = serializeForClause(node.getForUpdate(), analysis);
                return "for (" + init + "; " + serializeExpression(node.getForCondition(), analysis) + "; " + update
                        + ") { " + serializeStatements(node.getBody(), analysis) + " }";
            }
            case WHILE:
                return "while (" + serializeExpression(node.getCondition(), analysis) + ") { "
                        + serializeStatements(node.getBody(), analysis) + " }";
            case BREAK:
                return "break;";
            case CONTINUE:
                return "continue;";
            case EXPRESSION_STATEMENT:
                return serializeExpression(node.getExpressionStatementExpression(), analysis) + ";";
            default:
                throw new IllegalStateException("target_script_translator: unreachable statement kind: " + node.getKind());
        }
    }

    /** {@link #serializeStatement}과 동일하나 for절용으로 종결 세미콜론 없이 출력한다. */
    private String serializeForClause(SourceStatementNode node, SourceScriptAnalysis analysis) {
        if (node.getKind() == StatementKind.VARIABLE_DECLARATION) {
            String init = node.getInitializerOrAssignedExpression() == null ? ""
                    : " = " + serializeExpression(node.getInitializerOrAssignedExpression(), analysis);
            return "var " + node.getIdentifier() + init;
        }
        return node.getIdentifier() + " = " + serializeExpression(node.getInitializerOrAssignedExpression(), analysis);
    }

    private String serializeExpression(SourceExpressionNode node, SourceScriptAnalysis analysis) {
        switch (node.getKind()) {
            case PRIMITIVE_LITERAL:
                return node.getLiteralSource();
            case IDENTIFIER_REFERENCE:
                return node.getIdentifierName();
            case LOCAL_FUNCTION_CALL: {
                String target = node.getIdentifierName();
                if (!analysis.hasDeclaredFunction(target)) {
                    throw new UnresolvedFunctionReferenceException(
                            "target_script_translator: unresolved local function reference -- \"" + target
                                    + "\" is not an exact declared function in this SourceScriptAnalysis");
                }
                return "scwin." + target + "(" + serializeArguments(node.getCallArguments(), analysis) + ")";
            }
            case ALLOWED_MATH_CALL:
                return "Math." + node.getMathMemberName() + "(" + serializeArguments(node.getCallArguments(), analysis) + ")";
            case UNARY: {
                String op = node.getUnaryOperator();
                String operand = serializeExpression(node.getOperand(), analysis);
                return "typeof".equals(op) ? ("(typeof " + operand + ")") : ("(" + op + operand + ")");
            }
            case BINARY:
                return "(" + serializeExpression(node.getLeft(), analysis) + " " + node.getBinaryOperator() + " "
                        + serializeExpression(node.getRight(), analysis) + ")";
            case LOGICAL:
                return "(" + serializeExpression(node.getLeft(), analysis) + " " + node.getLogicalOperator() + " "
                        + serializeExpression(node.getRight(), analysis) + ")";
            case CONDITIONAL:
                return "(" + serializeExpression(node.getConditionalCondition(), analysis) + " ? "
                        + serializeExpression(node.getConditionalThen(), analysis) + " : "
                        + serializeExpression(node.getConditionalElse(), analysis) + ")";
            default:
                throw new IllegalStateException("target_script_translator: unreachable expression kind: " + node.getKind());
        }
    }

    private String serializeArguments(List<SourceExpressionNode> arguments, SourceScriptAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) { sb.append(", "); }
            sb.append(serializeExpression(arguments.get(i), analysis));
        }
        return sb.toString();
    }
}
