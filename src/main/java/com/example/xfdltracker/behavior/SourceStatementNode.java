package com.example.xfdltracker.behavior;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * 불변 폐쇄형 판별 문장 노드. 정확히 하나의 {@link StatementKind}만 가지며, 해당 종류의 필드만
 * 생성 시 채워진다. IF_ELSE/CLASSIC_FOR/WHILE 본문은 {@code List<SourceStatementNode>}(블록)이다.
 * else-if는 별도 노드 종류 없이 else 분기에 중첩된 IF_ELSE 하나로 표현한다.
 */
public final class SourceStatementNode {

    private final StatementKind kind;
    private final String identifier;
    private final SourceExpressionNode initializerOrAssignedExpression;
    private final SourceExpressionNode returnExpression;
    private final SourceExpressionNode condition;
    private final List<SourceStatementNode> thenBranch;
    private final List<SourceStatementNode> elseBranch;
    private final SourceStatementNode forInit;
    private final SourceExpressionNode forCondition;
    private final SourceStatementNode forUpdate;
    private final List<SourceStatementNode> body;
    private final SourceExpressionNode expressionStatementExpression;

    private SourceStatementNode(
            StatementKind kind, String identifier, SourceExpressionNode initializerOrAssignedExpression,
            SourceExpressionNode returnExpression, SourceExpressionNode condition,
            List<SourceStatementNode> thenBranch, List<SourceStatementNode> elseBranch, SourceStatementNode forInit,
            SourceExpressionNode forCondition, SourceStatementNode forUpdate, List<SourceStatementNode> body,
            SourceExpressionNode expressionStatementExpression) {
        this.kind = kind;
        this.identifier = identifier;
        this.initializerOrAssignedExpression = initializerOrAssignedExpression;
        this.returnExpression = returnExpression;
        this.condition = condition;
        this.thenBranch = thenBranch == null ? null
                : Collections.unmodifiableList(new ArrayList<SourceStatementNode>(thenBranch));
        this.elseBranch = elseBranch == null ? null
                : Collections.unmodifiableList(new ArrayList<SourceStatementNode>(elseBranch));
        this.forInit = forInit;
        this.forCondition = forCondition;
        this.forUpdate = forUpdate;
        this.body = body == null ? null : Collections.unmodifiableList(new ArrayList<SourceStatementNode>(body));
        this.expressionStatementExpression = expressionStatementExpression;
    }

    public static SourceStatementNode variableDeclaration(String identifier, SourceExpressionNode initializerOrNull) {
        requireIdentifier(identifier);
        return new SourceStatementNode(
                StatementKind.VARIABLE_DECLARATION, identifier, initializerOrNull, null, null, null, null, null,
                null, null, null, null);
    }

    public static SourceStatementNode identifierAssignment(String identifier, SourceExpressionNode expression) {
        requireIdentifier(identifier);
        if (expression == null) {
            throw new IllegalArgumentException("source_statement_node: expression must not be null");
        }
        return new SourceStatementNode(
                StatementKind.IDENTIFIER_ASSIGNMENT, identifier, expression, null, null, null, null, null, null,
                null, null, null);
    }

    public static SourceStatementNode returnStatement(SourceExpressionNode expressionOrNull) {
        return new SourceStatementNode(
                StatementKind.RETURN, null, null, expressionOrNull, null, null, null, null, null, null, null, null);
    }

    public static SourceStatementNode ifElse(
            SourceExpressionNode condition, List<SourceStatementNode> thenBranch, List<SourceStatementNode> elseBranchOrNull) {
        if (condition == null) {
            throw new IllegalArgumentException("source_statement_node: condition must not be null");
        }
        if (thenBranch == null) {
            throw new IllegalArgumentException("source_statement_node: thenBranch must not be null");
        }
        return new SourceStatementNode(
                StatementKind.IF_ELSE, null, null, null, condition, thenBranch, elseBranchOrNull, null, null, null,
                null, null);
    }

    public static SourceStatementNode classicFor(
            SourceStatementNode forInit, SourceExpressionNode forCondition, SourceStatementNode forUpdate,
            List<SourceStatementNode> body) {
        if (forInit == null || forCondition == null || forUpdate == null || body == null) {
            throw new IllegalArgumentException("source_statement_node: for init/condition/update/body must not be null");
        }
        if (forInit.getKind() != StatementKind.VARIABLE_DECLARATION && forInit.getKind() != StatementKind.IDENTIFIER_ASSIGNMENT) {
            throw new IllegalArgumentException("source_statement_node: for init must be VARIABLE_DECLARATION or IDENTIFIER_ASSIGNMENT");
        }
        if (forUpdate.getKind() != StatementKind.IDENTIFIER_ASSIGNMENT) {
            throw new IllegalArgumentException("source_statement_node: for update must be IDENTIFIER_ASSIGNMENT (e.g. i = i + 1)");
        }
        return new SourceStatementNode(
                StatementKind.CLASSIC_FOR, null, null, null, null, null, null, forInit, forCondition, forUpdate,
                body, null);
    }

    public static SourceStatementNode whileLoop(SourceExpressionNode condition, List<SourceStatementNode> body) {
        if (condition == null || body == null) {
            throw new IllegalArgumentException("source_statement_node: condition/body must not be null");
        }
        return new SourceStatementNode(
                StatementKind.WHILE, null, null, null, condition, null, null, null, null, null, body, null);
    }

    public static SourceStatementNode breakStatement() {
        return new SourceStatementNode(
                StatementKind.BREAK, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static SourceStatementNode continueStatement() {
        return new SourceStatementNode(
                StatementKind.CONTINUE, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static SourceStatementNode expressionStatement(SourceExpressionNode expression) {
        if (expression == null) {
            throw new IllegalArgumentException("source_statement_node: expression must not be null");
        }
        return new SourceStatementNode(
                StatementKind.EXPRESSION_STATEMENT, null, null, null, null, null, null, null, null, null, null,
                expression);
    }

    private static void requireIdentifier(String identifier) {
        if (identifier == null || identifier.trim().length() == 0) {
            throw new IllegalArgumentException("source_statement_node: identifier must not be null/blank");
        }
    }

    public StatementKind getKind() { return kind; }
    public String getIdentifier() { return identifier; }
    public SourceExpressionNode getInitializerOrAssignedExpression() { return initializerOrAssignedExpression; }
    public SourceExpressionNode getReturnExpression() { return returnExpression; }
    public SourceExpressionNode getCondition() { return condition; }
    public List<SourceStatementNode> getThenBranch() { return thenBranch; }
    public List<SourceStatementNode> getElseBranch() { return elseBranch; }
    public SourceStatementNode getForInit() { return forInit; }
    public SourceExpressionNode getForCondition() { return forCondition; }
    public SourceStatementNode getForUpdate() { return forUpdate; }
    public List<SourceStatementNode> getBody() { return body; }
    public SourceExpressionNode getExpressionStatementExpression() { return expressionStatementExpression; }
}
