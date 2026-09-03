package com.example.xfdltracker.behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 의존성 없는 자체 토크나이저 + 재귀 하강 파서. 지원 서브셋 이외의 구문은
 * {@link SourceAnalysisStatus#UNSUPPORTED_SYNTAX}로 fail-closed 처리한다(성공 노드 대체/누락 금지).
 * {@code uc.*}는 "Math.<member>(...)만 허용" 규칙으로 이미 차단되며, 이 클래스는 {@code RuntimeFunctionCallAnalyzer}와 상호 호출되지 않는다.
 */
public final class SourceScriptAnalyzer {

    private static final Set<String> UNSUPPORTED_RESERVED_GLOBAL_IDENTIFIERS = unmodifiableSetOf(
            "this", "new", "Date", "Promise", "async", "await", "setTimeout", "setInterval", "eval", "Function",
            "JSON", "alert");
    private static final Set<String> ALLOWED_MATH_MEMBERS = unmodifiableSetOf(
            "abs", "max", "min", "floor", "ceil", "round");
    private static final Set<String> BOOLEAN_NULL_LITERALS = unmodifiableSetOf("true", "false", "null");

    private static Set<String> unmodifiableSetOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(values)));
    }

    public SourceScriptAnalysisResult analyze(String sourceScript) {
        if (sourceScript == null) {
            return SourceScriptAnalysisResult.integrityViolation("source_script_analyzer: sourceScript must not be null");
        }
        try {
            List<Token> tokens = new Lexer(sourceScript).tokenize();
            List<SourceFunctionModel> functions = new Parser(tokens).parseProgram();
            SourceScriptAnalysis analysis = new SourceScriptAnalysis(functions);
            return SourceScriptAnalysisResult.analyzed(analysis);
        } catch (UnsupportedSyntaxException unsupported) {
            return SourceScriptAnalysisResult.unsupportedSyntax(unsupported.getMessage());
        } catch (IllegalStateException duplicateDeclaration) {
            return SourceScriptAnalysisResult.integrityViolation(duplicateDeclaration.getMessage());
        }
    }

    // ==== tokenizer 구현 =========================================================================

    private enum TokenType { IDENTIFIER, NUMBER, STRING, PUNCTUATOR, EOF }

    private static final class Token {
        final TokenType type;
        final String text;
        Token(TokenType type, String text) { this.type = type; this.text = text; }
    }

    private static final class UnsupportedSyntaxException extends RuntimeException {
        UnsupportedSyntaxException(String message) { super(message); }
    }

    private static final class Lexer {
        private final String src;
        private int pos;

        Lexer(String src) { this.src = src; this.pos = 0; }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<Token>();
            while (true) {
                skipTrivia();
                if (pos >= src.length()) {
                    tokens.add(new Token(TokenType.EOF, ""));
                    return tokens;
                }
                char c = src.charAt(pos);
                if (Character.isDigit(c)) {
                    tokens.add(scanNumber());
                } else if (c == '"' || c == '\'') {
                    tokens.add(scanString(c));
                } else if (Character.isLetter(c) || c == '_' || c == '$') {
                    tokens.add(scanIdentifier());
                } else {
                    tokens.add(scanPunctuator());
                }
            }
        }

        private void skipTrivia() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isWhitespace(c)) {
                    pos++;
                } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                    while (pos < src.length() && src.charAt(pos) != '\n') { pos++; }
                } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '*') {
                    pos += 2;
                    while (pos + 1 < src.length() && !(src.charAt(pos) == '*' && src.charAt(pos + 1) == '/')) { pos++; }
                    pos = Math.min(pos + 2, src.length());
                } else {
                    return;
                }
            }
        }

        private Token scanNumber() {
            int start = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; }
            if (pos < src.length() && src.charAt(pos) == '.' && pos + 1 < src.length()
                    && Character.isDigit(src.charAt(pos + 1))) {
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) { pos++; }
            }
            return new Token(TokenType.NUMBER, src.substring(start, pos));
        }

        private Token scanString(char quote) {
            int start = pos;
            pos++;
            while (pos < src.length() && src.charAt(pos) != quote) {
                if (src.charAt(pos) == '\\' && pos + 1 < src.length()) { pos++; }
                pos++;
            }
            if (pos >= src.length()) {
                throw new UnsupportedSyntaxException("source_script_analyzer: unterminated string literal");
            }
            pos++;
            return new Token(TokenType.STRING, src.substring(start, pos));
        }

        private Token scanIdentifier() {
            int start = pos;
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_'
                    || src.charAt(pos) == '$')) {
                pos++;
            }
            return new Token(TokenType.IDENTIFIER, src.substring(start, pos));
        }

        private static final String[] MULTI_CHAR_PUNCTUATORS = {
                "===", "!==", ">>>", "==", "!=", "<=", ">=", "&&", "||", "++", "--", "<<", ">>"
        };

        private Token scanPunctuator() {
            for (String candidate : MULTI_CHAR_PUNCTUATORS) {
                if (src.regionMatches(pos, candidate, 0, candidate.length())) {
                    pos += candidate.length();
                    return new Token(TokenType.PUNCTUATOR, candidate);
                }
            }
            char c = src.charAt(pos);
            pos++;
            return new Token(TokenType.PUNCTUATOR, String.valueOf(c));
        }
    }

    // ==== parser 구현 =============================================================================

    private static final class Parser {
        private final List<Token> tokens;
        private int index;

        Parser(List<Token> tokens) { this.tokens = tokens; this.index = 0; }

        List<SourceFunctionModel> parseProgram() {
            List<SourceFunctionModel> functions = new ArrayList<SourceFunctionModel>();
            while (peek(0).type != TokenType.EOF) {
                if (peek(0).type == TokenType.IDENTIFIER && "function".equals(peek(0).text)) {
                    functions.add(parseFunctionDeclaration());
                } else {
                    throw unsupported("unsupported top-level construct (only named function declarations are "
                            + "supported at top level): " + describe(peek(0)));
                }
            }
            return functions;
        }

        private SourceFunctionModel parseFunctionDeclaration() {
            advance(); // 'function' 토큰을 건너뛴다.
            Token nameToken = peek(0);
            if (nameToken.type != TokenType.IDENTIFIER) {
                throw unsupported("function declaration requires an explicit name (anonymous function expressions "
                        + "are not supported at top level)");
            }
            advance();
            expectPunct("(");
            List<String> parameters = new ArrayList<String>();
            if (!isPunct(peek(0), ")")) {
                while (true) {
                    Token paramToken = peek(0);
                    if (paramToken.type != TokenType.IDENTIFIER) {
                        throw unsupported("unsupported parameter form (only fixed identifier parameter names are "
                                + "supported -- no destructuring): " + describe(paramToken));
                    }
                    advance();
                    if (isPunct(peek(0), "=")) {
                        throw unsupported("default parameters are not supported: " + paramToken.text);
                    }
                    parameters.add(paramToken.text);
                    if (isPunct(peek(0), ",")) {
                        advance();
                        continue;
                    }
                    break;
                }
            }
            expectPunct(")");
            List<SourceStatementNode> body = parseBlock();
            return new SourceFunctionModel(nameToken.text, parameters, body);
        }

        private List<SourceStatementNode> parseBlock() {
            expectPunct("{");
            List<SourceStatementNode> statements = new ArrayList<SourceStatementNode>();
            while (!isPunct(peek(0), "}")) {
                if (peek(0).type == TokenType.EOF) {
                    throw unsupported("unterminated block (missing '}')");
                }
                statements.add(parseStatement());
            }
            expectPunct("}");
            return statements;
        }

        /** if/while/for 본문은 {@code { ... }} 블록 또는 단일 문장 모두 허용하며, 단일 문장은
         * {@code parseStatement()}로 파싱 후 단일 원소 리스트로 정규화한다 -- 중괄호 유무는 표면 구문일
         * 뿐 의미 모델에는 반영하지 않는다. */
        private List<SourceStatementNode> parseStatementBody() {
            if (isPunct(peek(0), "{")) {
                return parseBlock();
            }
            return Collections.singletonList(parseStatement());
        }

        private SourceStatementNode parseStatement() {
            Token t = peek(0);
            if (t.type == TokenType.IDENTIFIER) {
                if ("var".equals(t.text)) { return parseVariableDeclaration(); }
                if ("return".equals(t.text)) { return parseReturn(); }
                if ("if".equals(t.text)) { return parseIf(); }
                if ("for".equals(t.text)) { return parseFor(); }
                if ("while".equals(t.text)) { return parseWhile(); }
                if ("break".equals(t.text)) { advance(); expectPunct(";"); return SourceStatementNode.breakStatement(); }
                if ("continue".equals(t.text)) { advance(); expectPunct(";"); return SourceStatementNode.continueStatement(); }
                Token t1 = peek(1);
                if (t1.type == TokenType.PUNCTUATOR && "=".equals(t1.text)) {
                    advance();
                    advance();
                    SourceExpressionNode expr = parseExpression();
                    expectPunct(";");
                    return SourceStatementNode.identifierAssignment(t.text, expr);
                }
            }
            SourceExpressionNode expr = parseExpression();
            expectPunct(";");
            return SourceStatementNode.expressionStatement(expr);
        }

        private SourceStatementNode parseVariableDeclaration() {
            advance(); // 'var' 토큰을 건너뛴다.
            Token nameToken = peek(0);
            if (nameToken.type != TokenType.IDENTIFIER) {
                throw unsupported("unsupported variable declaration target (destructuring is not supported): "
                        + describe(nameToken));
            }
            advance();
            SourceExpressionNode initializer = null;
            if (isPunct(peek(0), "=")) {
                advance();
                initializer = parseExpression();
            }
            expectPunct(";");
            return SourceStatementNode.variableDeclaration(nameToken.text, initializer);
        }

        private SourceStatementNode parseReturn() {
            advance(); // 'return' 토큰을 건너뛴다.
            if (isPunct(peek(0), ";")) {
                advance();
                return SourceStatementNode.returnStatement(null);
            }
            SourceExpressionNode expr = parseExpression();
            expectPunct(";");
            return SourceStatementNode.returnStatement(expr);
        }

        private SourceStatementNode parseIf() {
            advance(); // 'if' 토큰을 건너뛴다.
            expectPunct("(");
            SourceExpressionNode condition = parseExpression();
            expectPunct(")");
            List<SourceStatementNode> thenBranch = parseStatementBody();
            List<SourceStatementNode> elseBranch = null;
            if (peek(0).type == TokenType.IDENTIFIER && "else".equals(peek(0).text)) {
                advance();
                if (peek(0).type == TokenType.IDENTIFIER && "if".equals(peek(0).text)) {
                    elseBranch = Collections.singletonList(parseIf());
                } else {
                    elseBranch = parseStatementBody();
                }
            }
            return SourceStatementNode.ifElse(condition, thenBranch, elseBranch);
        }

        private SourceStatementNode parseFor() {
            advance(); // 'for' 토큰을 건너뛴다.
            expectPunct("(");
            SourceStatementNode init = parseForInit();
            expectPunct(";");
            SourceExpressionNode condition = parseExpression();
            expectPunct(";");
            SourceStatementNode update = parseForUpdate();
            expectPunct(")");
            List<SourceStatementNode> body = parseStatementBody();
            return SourceStatementNode.classicFor(init, condition, update, body);
        }

        private SourceStatementNode parseForInit() {
            Token t = peek(0);
            if (t.type == TokenType.IDENTIFIER && "var".equals(t.text)) {
                advance();
                Token nameToken = peek(0);
                if (nameToken.type != TokenType.IDENTIFIER) {
                    throw unsupported("unsupported classic-for init target: " + describe(nameToken));
                }
                advance();
                SourceExpressionNode initializer = null;
                if (isPunct(peek(0), "=")) {
                    advance();
                    initializer = parseExpression();
                }
                return SourceStatementNode.variableDeclaration(nameToken.text, initializer);
            }
            Token t1 = peek(1);
            if (t.type == TokenType.IDENTIFIER && t1.type == TokenType.PUNCTUATOR && "=".equals(t1.text)) {
                advance();
                advance();
                SourceExpressionNode expr = parseExpression();
                return SourceStatementNode.identifierAssignment(t.text, expr);
            }
            throw unsupported("unsupported classic-for init form: " + describe(t));
        }

        private SourceStatementNode parseForUpdate() {
            Token t = peek(0);
            Token t1 = peek(1);
            if (t.type == TokenType.IDENTIFIER && t1.type == TokenType.PUNCTUATOR && "=".equals(t1.text)) {
                advance();
                advance();
                SourceExpressionNode expr = parseExpression();
                return SourceStatementNode.identifierAssignment(t.text, expr);
            }
            throw unsupported("unsupported classic-for update form (only \"i = i + 1\" style assignment is "
                    + "supported -- no ++/--): " + describe(t));
        }

        private SourceStatementNode parseWhile() {
            advance(); // 'while' 토큰을 건너뛴다.
            expectPunct("(");
            SourceExpressionNode condition = parseExpression();
            expectPunct(")");
            List<SourceStatementNode> body = parseStatementBody();
            return SourceStatementNode.whileLoop(condition, body);
        }

        // ---- 표현식, 낮은 순위에서 높은 순위로 ----

        private SourceExpressionNode parseExpression() { return parseConditional(); }

        private SourceExpressionNode parseConditional() {
            SourceExpressionNode condition = parseLogicalOr();
            if (isPunct(peek(0), "?")) {
                advance();
                SourceExpressionNode thenExpr = parseExpression();
                expectPunct(":");
                SourceExpressionNode elseExpr = parseExpression();
                return SourceExpressionNode.conditional(condition, thenExpr, elseExpr);
            }
            return condition;
        }

        private SourceExpressionNode parseLogicalOr() {
            SourceExpressionNode left = parseLogicalAnd();
            while (isPunct(peek(0), "||")) {
                advance();
                left = SourceExpressionNode.logical("||", left, parseLogicalAnd());
            }
            return left;
        }

        private SourceExpressionNode parseLogicalAnd() {
            SourceExpressionNode left = parseEquality();
            while (isPunct(peek(0), "&&")) {
                advance();
                left = SourceExpressionNode.logical("&&", left, parseEquality());
            }
            return left;
        }

        private static final Set<String> EQUALITY_OPS = unmodifiableSetOf("==", "===", "!=", "!==");
        private static final Set<String> RELATIONAL_OPS = unmodifiableSetOf("<", "<=", ">", ">=");
        private static final Set<String> ADDITIVE_OPS = unmodifiableSetOf("+", "-");
        private static final Set<String> MULTIPLICATIVE_OPS = unmodifiableSetOf("*", "/", "%");
        private static final Set<String> UNARY_OPS = unmodifiableSetOf("!", "-", "+");

        private SourceExpressionNode parseEquality() {
            SourceExpressionNode left = parseRelational();
            while (peek(0).type == TokenType.PUNCTUATOR && EQUALITY_OPS.contains(peek(0).text)) {
                String op = advance().text;
                left = SourceExpressionNode.binary(op, left, parseRelational());
            }
            return left;
        }

        private SourceExpressionNode parseRelational() {
            SourceExpressionNode left = parseAdditive();
            while (peek(0).type == TokenType.PUNCTUATOR && RELATIONAL_OPS.contains(peek(0).text)) {
                String op = advance().text;
                left = SourceExpressionNode.binary(op, left, parseAdditive());
            }
            return left;
        }

        private SourceExpressionNode parseAdditive() {
            SourceExpressionNode left = parseMultiplicative();
            while (peek(0).type == TokenType.PUNCTUATOR && ADDITIVE_OPS.contains(peek(0).text)) {
                String op = advance().text;
                left = SourceExpressionNode.binary(op, left, parseMultiplicative());
            }
            return left;
        }

        private SourceExpressionNode parseMultiplicative() {
            SourceExpressionNode left = parseUnary();
            while (peek(0).type == TokenType.PUNCTUATOR && MULTIPLICATIVE_OPS.contains(peek(0).text)) {
                String op = advance().text;
                left = SourceExpressionNode.binary(op, left, parseUnary());
            }
            return left;
        }

        private SourceExpressionNode parseUnary() {
            Token t = peek(0);
            if (t.type == TokenType.PUNCTUATOR && UNARY_OPS.contains(t.text)) {
                advance();
                return SourceExpressionNode.unary(t.text, parseUnary());
            }
            if (t.type == TokenType.IDENTIFIER && "typeof".equals(t.text)) {
                advance();
                return SourceExpressionNode.unary("typeof", parseUnary());
            }
            return parsePrimary();
        }

        private SourceExpressionNode parsePrimary() {
            Token t = peek(0);
            if (t.type == TokenType.NUMBER) { advance(); return SourceExpressionNode.primitiveLiteral(t.text); }
            if (t.type == TokenType.STRING) { advance(); return SourceExpressionNode.primitiveLiteral(t.text); }
            if (t.type == TokenType.IDENTIFIER) {
                if (BOOLEAN_NULL_LITERALS.contains(t.text)) {
                    advance();
                    return SourceExpressionNode.primitiveLiteral(t.text);
                }
                if (UNSUPPORTED_RESERVED_GLOBAL_IDENTIFIERS.contains(t.text)) {
                    throw unsupported("unsupported reserved/global identifier: " + t.text);
                }
                if ("Math".equals(t.text)) {
                    advance();
                    expectPunct(".");
                    Token member = peek(0);
                    if (member.type != TokenType.IDENTIFIER || !ALLOWED_MATH_MEMBERS.contains(member.text)) {
                        throw unsupported("unsupported Math member (only abs/max/min/floor/ceil/round are "
                                + "supported): " + describe(member));
                    }
                    advance();
                    expectPunct("(");
                    List<SourceExpressionNode> args = parseArgumentList();
                    expectPunct(")");
                    return SourceExpressionNode.allowedMathCall(member.text, args);
                }
                String name = t.text;
                advance();
                if (isPunct(peek(0), "(")) {
                    advance();
                    List<SourceExpressionNode> args = parseArgumentList();
                    expectPunct(")");
                    if (isPunct(peek(0), ".") || isPunct(peek(0), "[")) {
                        throw unsupported("unsupported chained member access on call result");
                    }
                    return SourceExpressionNode.localFunctionCall(name, args);
                }
                if (isPunct(peek(0), ".") || isPunct(peek(0), "[")) {
                    throw unsupported("unsupported arbitrary member access on identifier: " + name);
                }
                return SourceExpressionNode.identifierReference(name);
            }
            if (isPunct(t, "(")) {
                advance();
                SourceExpressionNode expr = parseExpression();
                expectPunct(")");
                return expr;
            }
            throw unsupported("unsupported expression construct: " + describe(t));
        }

        private List<SourceExpressionNode> parseArgumentList() {
            List<SourceExpressionNode> args = new ArrayList<SourceExpressionNode>();
            if (isPunct(peek(0), ")")) { return args; }
            while (true) {
                args.add(parseExpression());
                if (isPunct(peek(0), ",")) { advance(); continue; }
                break;
            }
            return args;
        }

        // ---- token stream 도우미 ----

        private Token peek(int lookahead) {
            int i = index + lookahead;
            return i < tokens.size() ? tokens.get(i) : tokens.get(tokens.size() - 1);
        }

        private Token advance() {
            Token t = peek(0);
            if (t.type != TokenType.EOF) { index++; }
            return t;
        }

        private boolean isPunct(Token t, String text) {
            return t.type == TokenType.PUNCTUATOR && text.equals(t.text);
        }

        private void expectPunct(String text) {
            if (!isPunct(peek(0), text)) {
                throw unsupported("expected \"" + text + "\" but found " + describe(peek(0)));
            }
            advance();
        }

        private String describe(Token t) {
            return t.type == TokenType.EOF ? "<end of script>" : ("\"" + t.text + "\"");
        }

        private UnsupportedSyntaxException unsupported(String message) {
            return new UnsupportedSyntaxException("source_script_analyzer: " + message);
        }
    }
}
