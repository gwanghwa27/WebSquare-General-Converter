package com.example.xfdltracker.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 결정적, 토큰 인식 단일 패스 어휘 스캐너 겸 직접 호출 인식기 -- 완전한 JS AST 파서가 아니다.
 * {@code uc}는 스크립트 전역 예약어이며, 바인딩/재할당/변형 구문이 있으면 전체 분석을 무효화한다.
 * 지원 형태는 비한정 {@code uc.<Identifier>(...)} 직접 호출뿐이다.
 */
public final class RuntimeFunctionCallAnalyzer {

    public RuntimeRequirementSet analyze(String script, CommonRuntimeCapabilityCatalog catalog) {
        if (script == null) {
            throw new IllegalArgumentException("runtime_function_call_analyzer: script must not be null");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("runtime_function_call_analyzer: catalog must not be null");
        }
        if (script.length() == 0) {
            return RuntimeRequirementSet.empty();
        }
        return new Scanner(script, catalog).run();
    }

    /** 잘못되었거나 모호하거나 미지원인 {@code uc}/어휘 구성에 대한 fail-closed 신호. */
    public static final class UnsupportedRuntimeSyntaxException extends RuntimeException {
        public UnsupportedRuntimeSyntaxException(String message) {
            super(message);
        }
    }

    private static final Set<String> CONTROL_HEADER_KEYWORDS = setOf("if", "while", "for", "with", "switch", "catch");
    private static final Set<String> EXPRESSION_EXPECTED_KEYWORDS = setOf(
            "typeof", "in", "of", "new", "return", "throw", "case", "do", "else", "yield", "void", "delete",
            "instanceof");
    private static final Set<String> BINDING_KEYWORDS = setOf("var", "let", "const");

    private static Set<String> setOf(String... values) {
        Set<String> s = new LinkedHashSet<String>();
        for (String v : values) {
            s.add(v);
        }
        return java.util.Collections.unmodifiableSet(s);
    }

    private enum ParenRole { CONTROL_HEADER, EXPRESSION, FUNCTION_PARAMS, CATCH_PARAMS }

    private enum SlashExpectation { REGEX_ALLOWED, DIVISION_ALLOWED, AMBIGUOUS }

    private static final class Frame {
        final char kind; // '(' , '{' , '['
        final ParenRole parenRole;
        final boolean braceIsInterpolation;
        final boolean braceRoleKnown;

        Frame(char kind, ParenRole parenRole, boolean braceIsInterpolation, boolean braceRoleKnown) {
            this.kind = kind;
            this.parenRole = parenRole;
            this.braceIsInterpolation = braceIsInterpolation;
            this.braceRoleKnown = braceRoleKnown;
        }
    }

    private static final class Scanner {
        private final String src;
        private final int len;
        private final CommonRuntimeCapabilityCatalog catalog;
        private int pos;
        private final Set<String> requirements = new LinkedHashSet<String>();
        private final Deque<Frame> delimiterStack = new ArrayDeque<Frame>();
        /** 복귀할 렉싱 모드 스택 (TRUE=템플릿 리터럴 내부, FALSE=일반 코드). */
        private final Deque<Boolean> templateModeStack = new ArrayDeque<Boolean>();

        private SlashExpectation slashExpectation = SlashExpectation.REGEX_ALLOWED;
        private String lastKeyword = null;
        private boolean pendingControlHeaderParen = false;
        private boolean pendingFunctionParen = false;
        private boolean pendingCatchParen = false;
        /** 직전 문자가 '.'였는지 여부. */
        private boolean lastWasDot = false;
        /** 접두 '++'/'--' 스캔 직후 TRUE, 다음 atom 소비 시 해제. */
        private boolean pendingPrefixIncDec = false;

        Scanner(String src, CommonRuntimeCapabilityCatalog catalog) {
            this.src = src;
            this.len = src.length();
            this.catalog = catalog;
        }

        RuntimeRequirementSet run() {
            scanCode(false);
            if (!delimiterStack.isEmpty()) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: unbalanced delimiter at end of script");
            }
            return new RuntimeRequirementSet(requirements);
        }

        /** EOF 또는 템플릿 보간 '}' 닫힘까지 일반 실행 코드를 스캔한다. */
        private void scanCode(boolean insideInterpolation) {
            while (pos < len) {
                char c = src.charAt(pos);

                if (c == '\'') {
                    scanQuotedString('\'');
                    afterAtom();
                    continue;
                }
                if (c == '"') {
                    scanQuotedString('"');
                    afterAtom();
                    continue;
                }
                if (c == '`') {
                    pos++;
                    templateModeStack.push(Boolean.TRUE);
                    scanTemplateText();
                    continue;
                }
                if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                    while (pos < len && src.charAt(pos) != '\n') {
                        pos++;
                    }
                    continue;
                }
                if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                    scanBlockComment();
                    continue;
                }
                if (c == '/') {
                    handleSlash();
                    continue;
                }
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }
                if (isIdentifierStart(c)) {
                    int start = pos;
                    while (pos < len && isIdentifierPart(src.charAt(pos))) {
                        pos++;
                    }
                    handleIdentifier(src.substring(start, pos));
                    continue;
                }
                if (Character.isDigit(c)) {
                    while (pos < len && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '.'
                            || src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                        // 보수적 범위 제한 숫자 리터럴 스캔 (1.5, 0x1F, 1e-10 포함)
                        char n = src.charAt(pos);
                        if ((n == '+' || n == '-') && !(pos > start() && (src.charAt(pos - 1) == 'e' || src.charAt(pos - 1) == 'E'))) {
                            break;
                        }
                        pos++;
                    }
                    afterAtom();
                    continue;
                }
                if (c == '(') {
                    ParenRole role = pendingFunctionParen ? ParenRole.FUNCTION_PARAMS
                            : pendingCatchParen ? ParenRole.CATCH_PARAMS
                            : pendingControlHeaderParen ? ParenRole.CONTROL_HEADER
                            : ParenRole.EXPRESSION;
                    pendingControlHeaderParen = false;
                    pendingFunctionParen = false;
                    pendingCatchParen = false;
                    delimiterStack.push(new Frame('(', role, false, true));
                    pos++;
                    slashExpectation = SlashExpectation.REGEX_ALLOWED;
                    lastWasDot = false;
                    continue;
                }
                if (c == ')') {
                    Frame f = popExpecting('(');
                    pos++;
                    slashExpectation = (f != null && f.parenRole == ParenRole.CONTROL_HEADER)
                            ? SlashExpectation.REGEX_ALLOWED : SlashExpectation.DIVISION_ALLOWED;
                    lastWasDot = false;
                    continue;
                }
                if (c == '{') {
                    delimiterStack.push(new Frame('{', null, false, false));
                    pos++;
                    slashExpectation = SlashExpectation.REGEX_ALLOWED;
                    lastWasDot = false;
                    continue;
                }
                if (c == '}') {
                    if (insideInterpolation && !delimiterStack.isEmpty()
                            && delimiterStack.peek().braceIsInterpolation) {
                        delimiterStack.pop();
                        pos++;
                        return; // 상위 scanTemplateText()로 복귀
                    }
                    Frame f = popExpecting('{');
                    pos++;
                    if (f == null) {
                        throw new UnsupportedRuntimeSyntaxException(
                                "runtime_function_call_analyzer: unmatched '}' ");
                    }
                    slashExpectation = f.braceIsInterpolation
                            ? SlashExpectation.AMBIGUOUS
                            : SlashExpectation.AMBIGUOUS;
                    lastWasDot = false;
                    continue;
                }
                if (c == '[') {
                    delimiterStack.push(new Frame('[', null, false, true));
                    pos++;
                    slashExpectation = SlashExpectation.REGEX_ALLOWED;
                    lastWasDot = false;
                    continue;
                }
                if (c == ']') {
                    popExpecting('[');
                    pos++;
                    slashExpectation = SlashExpectation.DIVISION_ALLOWED;
                    lastWasDot = false;
                    continue;
                }
                if (c == '.') {
                    pos++;
                    lastWasDot = true;
                    slashExpectation = SlashExpectation.REGEX_ALLOWED;
                    continue;
                }
                if ((c == '+' && pos + 1 < len && src.charAt(pos + 1) == '+')
                        || (c == '-' && pos + 1 < len && src.charAt(pos + 1) == '-')) {
                    pos += 2;
                    lastWasDot = false;
                    if (slashExpectation == SlashExpectation.REGEX_ALLOWED) {
                        // 접두 위치: 다음 atom(식별자면)이 변형 대상
                        pendingPrefixIncDec = true;
                    } else {
                        // 접미 위치: 이전 값에 적용, 표현식 계속
                        slashExpectation = SlashExpectation.DIVISION_ALLOWED;
                    }
                    continue;
                }
                // 그 외 단일 문자 구두점/연산자
                pos++;
                lastWasDot = false;
                pendingPrefixIncDec = false;
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
            }
            if (insideInterpolation) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: unterminated template interpolation");
            }
        }

        private int start() { return pos; }

        private Frame popExpecting(char openKind) {
            if (delimiterStack.isEmpty()) {
                return null;
            }
            Frame top = delimiterStack.peek();
            if (top.kind != openKind) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: mismatched delimiter, expected close of '" + openKind + "'");
            }
            return delimiterStack.pop();
        }

        private void afterAtom() {
            slashExpectation = SlashExpectation.DIVISION_ALLOWED;
            lastWasDot = false;
        }

        private void scanQuotedString(char quote) {
            pos++; // opening quote를 건너뛴다.
            while (true) {
                if (pos >= len) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated string literal");
                }
                char c = src.charAt(pos);
                if (c == '\\') {
                    pos += 2;
                    continue;
                }
                if (c == quote) {
                    pos++;
                    return;
                }
                if (c == '\n') {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated string literal (newline)");
                }
                pos++;
            }
        }

        private void scanBlockComment() {
            pos += 2;
            while (true) {
                if (pos + 1 >= len) {
                    if (pos >= len || src.charAt(pos) != '*') {
                        throw new UnsupportedRuntimeSyntaxException(
                                "runtime_function_call_analyzer: unterminated block comment");
                    }
                }
                if (pos >= len) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated block comment");
                }
                if (src.charAt(pos) == '*' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                    pos += 2;
                    return;
                }
                pos++;
            }
        }

        private void handleSlash() {
            if (slashExpectation == SlashExpectation.DIVISION_ALLOWED) {
                pos++;
                if (pos < len && src.charAt(pos) == '=') {
                    pos++;
                }
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
                lastWasDot = false;
                return;
            }
            if (slashExpectation == SlashExpectation.AMBIGUOUS) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: ambiguous slash context (division vs regex literal)");
            }
            scanRegexLiteral();
            afterAtom();
        }

        private void scanRegexLiteral() {
            pos++; // opening '/'를 건너뛴다.
            boolean inClass = false;
            while (true) {
                if (pos >= len) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated regex literal");
                }
                char c = src.charAt(pos);
                if (c == '\n') {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated regex literal (newline)");
                }
                if (c == '\\') {
                    pos += 2;
                    continue;
                }
                if (c == '[') {
                    inClass = true;
                    pos++;
                    continue;
                }
                if (c == ']' && inClass) {
                    inClass = false;
                    pos++;
                    continue;
                }
                if (c == '/' && !inClass) {
                    pos++;
                    break;
                }
                pos++;
            }
            while (pos < len && isIdentifierPart(src.charAt(pos))) {
                pos++; // 정규식 플래그, 코드로 분석하지 않음
            }
        }

        private void scanTemplateText() {
            while (true) {
                if (pos >= len) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: unterminated template literal");
                }
                char c = src.charAt(pos);
                if (c == '\\') {
                    pos += 2;
                    continue;
                }
                if (c == '`') {
                    pos++;
                    templateModeStack.pop();
                    afterAtom();
                    return;
                }
                if (c == '$' && pos + 1 < len && src.charAt(pos + 1) == '{') {
                    pos += 2;
                    delimiterStack.push(new Frame('{', null, true, true));
                    scanCode(true);
                    continue; // 보간 종료 후 템플릿 텍스트 재개
                }
                pos++;
            }
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_' || c == '$';
        }

        private boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }

        private void skipWhitespaceAndComments(int[] cursor) {
            while (cursor[0] < len) {
                char c = src.charAt(cursor[0]);
                if (Character.isWhitespace(c)) {
                    cursor[0]++;
                    continue;
                }
                if (c == '/' && cursor[0] + 1 < len && src.charAt(cursor[0] + 1) == '/') {
                    while (cursor[0] < len && src.charAt(cursor[0]) != '\n') {
                        cursor[0]++;
                    }
                    continue;
                }
                if (c == '/' && cursor[0] + 1 < len && src.charAt(cursor[0] + 1) == '*') {
                    cursor[0] += 2;
                    while (cursor[0] + 1 < len && !(src.charAt(cursor[0]) == '*' && src.charAt(cursor[0] + 1) == '/')) {
                        cursor[0]++;
                    }
                    cursor[0] = Math.min(cursor[0] + 2, len);
                    continue;
                }
                break;
            }
        }

        private void handleIdentifier(String name) {
            boolean qualified = lastWasDot;
            lastWasDot = false;
            String precedingKeyword = lastKeyword;
            lastKeyword = null;

            if (CONTROL_HEADER_KEYWORDS.contains(name)) {
                lastKeyword = name;
                pendingControlHeaderParen = !"catch".equals(name);
                pendingCatchParen = "catch".equals(name);
                pendingPrefixIncDec = false;
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
                checkBindingKeywordFollowUp(name);
                return;
            }
            if ("function".equals(name)) {
                pendingFunctionParen = true;
                pendingPrefixIncDec = false;
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
                checkBindingKeywordFollowUp(name);
                return;
            }
            if ("class".equals(name) || BINDING_KEYWORDS.contains(name)) {
                pendingPrefixIncDec = false;
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
                checkBindingKeywordFollowUp(name);
                return;
            }
            if (EXPRESSION_EXPECTED_KEYWORDS.contains(name)) {
                lastKeyword = name;
                pendingPrefixIncDec = false;
                slashExpectation = SlashExpectation.REGEX_ALLOWED;
                return;
            }

            if (!qualified && "uc".equals(name)) {
                boolean prefixMutation = pendingPrefixIncDec;
                pendingPrefixIncDec = false;
                if (prefixMutation) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: increment/decrement mutation of reserved identifier "
                                    + "uc -- fail closed");
                }
                if ("delete".equals(precedingKeyword)) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: delete of reserved identifier uc -- fail closed");
                }
                handleUnqualifiedUc();
                return;
            }

            pendingPrefixIncDec = false;
            afterAtom();
        }

        /** 바인딩 키워드 뒤 다음 토큰을 미리보기(소비 없이)하여 "uc" 바인딩이면 fail closed -- 스코프
         *  인식 파싱은 시도하지 않는다. */
        private void checkBindingKeywordFollowUp(String keyword) {
            int[] cursor = { pos };
            skipWhitespaceAndComments(cursor);
            if (cursor[0] >= len) {
                return;
            }
            char c = src.charAt(cursor[0]);
            if (c == '{' || c == '[') {
                // 바인딩 키워드 뒤 구조분해 패턴: 닫힘 전 어디든 "uc"가 나타나면 안전하게 증명할 수
                // 없으므로 보수적으로 fail closed.
                if (destructuringContainsUc(cursor[0], c)) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: destructuring binding may contain reserved "
                                    + "identifier uc, cannot safely classify -- fail closed");
                }
                return;
            }
            if (c == '(' && ("function".equals(keyword) || "catch".equals(keyword))) {
                // function/catch 매개변수 목록: 닫힘 전 어디든 "uc"가 있으면 fail closed
                if (destructuringContainsUc(cursor[0], '(')) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: reserved identifier uc used as a " + keyword
                                    + " parameter -- fail closed");
                }
                return;
            }
            if (isIdentifierStart(c)) {
                int idStart = cursor[0];
                int idEnd = idStart;
                while (idEnd < len && isIdentifierPart(src.charAt(idEnd))) {
                    idEnd++;
                }
                String id = src.substring(idStart, idEnd);
                if ("uc".equals(id)) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: reserved identifier uc used as a " + keyword
                                    + " binding target -- fail closed");
                }
            }
        }

        private boolean destructuringContainsUc(int openIndex, char openChar) {
            char closeChar = openChar == '{' ? '}' : openChar == '(' ? ')' : ']';
            int depth = 0;
            int i = openIndex;
            while (i < len) {
                char c = src.charAt(i);
                if (c == openChar) {
                    depth++;
                } else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        return false;
                    }
                } else if (isIdentifierStart(c)) {
                    int start = i;
                    while (i < len && isIdentifierPart(src.charAt(i))) {
                        i++;
                    }
                    if ("uc".equals(src.substring(start, i))) {
                        return true;
                    }
                    continue;
                }
                i++;
            }
            throw new UnsupportedRuntimeSyntaxException(
                    "runtime_function_call_analyzer: unterminated destructuring binding pattern");
        }

        private void handleUnqualifiedUc() {
            int[] cursor = { pos };
            skipWhitespaceAndComments(cursor);

            // 네임스페이스 변형/재할당 (uc =, uc +=, uc++; delete uc는 키워드 지점에서 처리)
            if (cursor[0] < len) {
                char c = src.charAt(cursor[0]);
                if (c == '=' && !(cursor[0] + 1 < len && (src.charAt(cursor[0] + 1) == '=' || src.charAt(cursor[0] + 1) == '>'))) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: assignment to reserved identifier uc -- fail closed");
                }
                if ((c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '&' || c == '|' || c == '^')
                        && cursor[0] + 1 < len && src.charAt(cursor[0] + 1) == '=') {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: compound assignment to reserved identifier uc -- fail closed");
                }
                if ((c == '+' && cursor[0] + 1 < len && src.charAt(cursor[0] + 1) == '+')
                        || (c == '-' && cursor[0] + 1 < len && src.charAt(cursor[0] + 1) == '-')) {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: increment/decrement mutation of reserved identifier uc "
                                    + "-- fail closed");
                }
            }

            if (cursor[0] >= len || src.charAt(cursor[0]) != '.') {
                // uc 뒤에 '.'가 없음: 옵셔널 체이닝, 계산된 접근, 단독 값 사용
                if (cursor[0] < len && src.charAt(cursor[0]) == '?' && cursor[0] + 1 < len
                        && src.charAt(cursor[0] + 1) == '.') {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: optional chaining on reserved identifier uc "
                                    + "is unsupported -- fail closed");
                }
                if (cursor[0] < len && src.charAt(cursor[0]) == '[') {
                    throw new UnsupportedRuntimeSyntaxException(
                            "runtime_function_call_analyzer: computed member access on reserved identifier uc "
                                    + "is unsupported -- fail closed");
                }
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: reserved identifier uc used as a first-class value "
                                + "is unsupported -- fail closed");
            }

            cursor[0]++; // '.'을 소비한다.
            skipWhitespaceAndComments(cursor);
            if (cursor[0] < len && src.charAt(cursor[0]) == '?') {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: optional chaining on reserved identifier uc "
                                + "is unsupported -- fail closed");
            }
            if (cursor[0] >= len || !isIdentifierStart(src.charAt(cursor[0]))) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: malformed member access after reserved identifier uc "
                                + "-- fail closed");
            }
            int aliasStart = cursor[0];
            while (cursor[0] < len && isIdentifierPart(src.charAt(cursor[0]))) {
                cursor[0]++;
            }
            String aliasName = src.substring(aliasStart, cursor[0]);

            skipWhitespaceAndComments(cursor);
            if (cursor[0] >= len || src.charAt(cursor[0]) != '(') {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: uc." + aliasName
                                + " referenced without an immediate call -- fail closed");
            }

            String alias = "uc." + aliasName;
            String capabilityId = catalog.resolveAliasToCapabilityId(alias);
            if (capabilityId == null) {
                throw new UnsupportedRuntimeSyntaxException(
                        "runtime_function_call_analyzer: unknown uc alias " + alias
                                + " -- fail closed, no canonical ID invented");
            }
            requirements.add(capabilityId);

            // 인식된 "uc" "." 식별자 뒤로 실제 스캐너 위치를 이동; 여는 '('는 메인 루프가 처리하도록 남김.
            pos = cursor[0];
            slashExpectation = SlashExpectation.REGEX_ALLOWED;
            lastWasDot = false;
        }
    }
}
