package com.example.xfdltracker.behavior;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * 불변 폐쇄형 판별 표현식 노드. 정확히 하나의 {@link ExpressionKind}만 가지며, 해당 종류의 필드만
 * 생성 시 채워진다. {@code SourceScriptAnalyzer}의 파서만 생성하며, 리터럴 텍스트를 제외하고는
 * 원본 소스 문자열을 의미 근거로 갖지 않는다.
 */
public final class SourceExpressionNode {

    private final ExpressionKind kind;
    private final String literalSource;
    private final String identifierName;
    private final List<SourceExpressionNode> callArguments;
    private final String mathMemberName;
    private final String unaryOperator;
    private final SourceExpressionNode operand;
    private final String binaryOperator;
    private final SourceExpressionNode left;
    private final SourceExpressionNode right;
    private final String logicalOperator;
    private final SourceExpressionNode conditionalCondition;
    private final SourceExpressionNode conditionalThen;
    private final SourceExpressionNode conditionalElse;

    private SourceExpressionNode(
            ExpressionKind kind, String literalSource, String identifierName,
            List<SourceExpressionNode> callArguments, String mathMemberName, String unaryOperator,
            SourceExpressionNode operand, String binaryOperator, SourceExpressionNode left,
            SourceExpressionNode right, String logicalOperator, SourceExpressionNode conditionalCondition,
            SourceExpressionNode conditionalThen, SourceExpressionNode conditionalElse) {
        this.kind = kind;
        this.literalSource = literalSource;
        this.identifierName = identifierName;
        this.callArguments = callArguments == null ? Collections.<SourceExpressionNode>emptyList()
                : Collections.unmodifiableList(new ArrayList<SourceExpressionNode>(callArguments));
        this.mathMemberName = mathMemberName;
        this.unaryOperator = unaryOperator;
        this.operand = operand;
        this.binaryOperator = binaryOperator;
        this.left = left;
        this.right = right;
        this.logicalOperator = logicalOperator;
        this.conditionalCondition = conditionalCondition;
        this.conditionalThen = conditionalThen;
        this.conditionalElse = conditionalElse;
    }

    public static SourceExpressionNode primitiveLiteral(String literalSource) {
        if (literalSource == null || literalSource.length() == 0) {
            throw new IllegalArgumentException("source_expression_node: literalSource must not be null/empty");
        }
        return new SourceExpressionNode(
                ExpressionKind.PRIMITIVE_LITERAL, literalSource, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    public static SourceExpressionNode identifierReference(String identifierName) {
        requireIdentifier(identifierName, "identifierName");
        return new SourceExpressionNode(
                ExpressionKind.IDENTIFIER_REFERENCE, null, identifierName, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    public static SourceExpressionNode localFunctionCall(String identifierName, List<SourceExpressionNode> arguments) {
        requireIdentifier(identifierName, "identifierName");
        if (arguments == null) {
            throw new IllegalArgumentException("source_expression_node: arguments must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.LOCAL_FUNCTION_CALL, null, identifierName, arguments, null, null, null, null, null,
                null, null, null, null, null);
    }

    public static SourceExpressionNode allowedMathCall(String mathMemberName, List<SourceExpressionNode> arguments) {
        requireIdentifier(mathMemberName, "mathMemberName");
        if (arguments == null) {
            throw new IllegalArgumentException("source_expression_node: arguments must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.ALLOWED_MATH_CALL, null, null, arguments, mathMemberName, null, null, null, null,
                null, null, null, null, null);
    }

    public static SourceExpressionNode unary(String unaryOperator, SourceExpressionNode operand) {
        requireIdentifier(unaryOperator, "unaryOperator");
        if (operand == null) {
            throw new IllegalArgumentException("source_expression_node: operand must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.UNARY, null, null, null, null, unaryOperator, operand, null, null, null, null, null,
                null, null);
    }

    public static SourceExpressionNode binary(String binaryOperator, SourceExpressionNode left, SourceExpressionNode right) {
        requireIdentifier(binaryOperator, "binaryOperator");
        if (left == null || right == null) {
            throw new IllegalArgumentException("source_expression_node: left/right must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.BINARY, null, null, null, null, null, null, binaryOperator, left, right, null, null,
                null, null);
    }

    public static SourceExpressionNode logical(String logicalOperator, SourceExpressionNode left, SourceExpressionNode right) {
        requireIdentifier(logicalOperator, "logicalOperator");
        if (left == null || right == null) {
            throw new IllegalArgumentException("source_expression_node: left/right must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.LOGICAL, null, null, null, null, null, null, null, left, right, logicalOperator,
                null, null, null);
    }

    public static SourceExpressionNode conditional(
            SourceExpressionNode condition, SourceExpressionNode thenExpression, SourceExpressionNode elseExpression) {
        if (condition == null || thenExpression == null || elseExpression == null) {
            throw new IllegalArgumentException("source_expression_node: condition/then/else must not be null");
        }
        return new SourceExpressionNode(
                ExpressionKind.CONDITIONAL, null, null, null, null, null, null, null, null, null, null, condition,
                thenExpression, elseExpression);
    }

    private static void requireIdentifier(String value, String fieldName) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("source_expression_node: " + fieldName + " must not be null/blank");
        }
    }

    public ExpressionKind getKind() { return kind; }
    public String getLiteralSource() { return literalSource; }
    public String getIdentifierName() { return identifierName; }
    public List<SourceExpressionNode> getCallArguments() { return callArguments; }
    public String getMathMemberName() { return mathMemberName; }
    public String getUnaryOperator() { return unaryOperator; }
    public SourceExpressionNode getOperand() { return operand; }
    public String getBinaryOperator() { return binaryOperator; }
    public SourceExpressionNode getLeft() { return left; }
    public SourceExpressionNode getRight() { return right; }
    public String getLogicalOperator() { return logicalOperator; }
    public SourceExpressionNode getConditionalCondition() { return conditionalCondition; }
    public SourceExpressionNode getConditionalThen() { return conditionalThen; }
    public SourceExpressionNode getConditionalElse() { return conditionalElse; }
}
