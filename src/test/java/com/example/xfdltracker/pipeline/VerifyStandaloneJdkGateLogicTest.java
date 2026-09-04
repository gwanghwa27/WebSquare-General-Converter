package com.example.xfdltracker.pipeline;

/**
 * {@code java}/{@code javac}를 직접 실행하지 않고, verify-standalone.bat Step 1(Slice 100E-I)과
 * 동일한 토큰 추출/판정 규칙을 순수 Java로 재구현해 검증한다. exact update pinning 대신
 * "1.8.0" family(자체이거나 "1.8.0_" 뒤에 숫자만 1개 이상)만 anchored 방식으로 인정한다.
 */
public class VerifyStandaloneJdkGateLogicTest {

    private static int failures = 0;

    private static final String TARGET_FAMILY = "1.8.0";

    public static void main(String[] args) throws Exception {
        testFamilyTokenAccepted();
        testNonFamilyTokenRejected();
        testAnchorRejectsLookalikeValues();
        testEmptyAndUndefinedTokenRejected();
        testJavaVersionLineTokenExtraction();
        testJavacVersionLineTokenExtraction();
        testAllPassPairsAccepted();
        testMixedUpdateSuffixPairAccepted();
        testOneFamilyOneNonFamilyIsOverallReject();
        testBothNonFamilyIsOverallReject();
        testParseFailurePairIsOverallReject();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    /** verify-standalone.bat 동작 재현: 첫 '"' 이전 부분을 제거하고 남은 '"'도 제거한다. */
    private static String extractJavaToken(String javaVersionLine) {
        int firstQuote = javaVersionLine.indexOf('"');
        String afterFirstQuote = firstQuote < 0 ? javaVersionLine : javaVersionLine.substring(firstQuote + 1);
        return afterFirstQuote.replace("\"", "");
    }

    /** verify-standalone.bat 동작 재현: javac 버전 라인의 두 번째 공백 구분 토큰을 취한다. */
    private static String extractJavacToken(String javacVersionLine) {
        String[] parts = javacVersionLine.trim().split("\\s+");
        return parts.length >= 2 ? parts[1] : "";
    }

    /**
     * verify-standalone.bat Step 1의 anchored family 판정을 그대로 재현한다: token이
     * "1.8.0" 그 자체이거나, "1.8.0_" 뒤에 숫자만 1개 이상 있어야 한다.
     */
    private static boolean isFamilyMatch(String token) {
        if (token == null) {
            return false;
        }
        if (token.equals(TARGET_FAMILY)) {
            return true;
        }
        String prefix = TARGET_FAMILY + "_";
        if (!token.startsWith(prefix)) {
            return false;
        }
        String update = token.substring(prefix.length());
        if (update.isEmpty()) {
            return false;
        }
        for (int i = 0; i < update.length(); i++) {
            char c = update.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static void testFamilyTokenAccepted() throws Exception {
        assertTrue("1.8.0 (bare family, no suffix) accepted", isFamilyMatch("1.8.0"));
        assertTrue("1.8.0_111 accepted", isFamilyMatch("1.8.0_111"));
        assertTrue("1.8.0_503 accepted", isFamilyMatch("1.8.0_503"));
        assertTrue("1.8.0_1111 (multi-digit update) accepted", isFamilyMatch("1.8.0_1111"));
    }

    private static void testNonFamilyTokenRejected() throws Exception {
        assertTrue("1.7.0_80 (different update-track major) rejected", !isFamilyMatch("1.7.0_80"));
        assertTrue("9 rejected", !isFamilyMatch("9"));
        assertTrue("11 rejected", !isFamilyMatch("11"));
        assertTrue("17 rejected", !isFamilyMatch("17"));
    }

    /**
     * naive prefix/contains 판정이었다면 잘못 통과했을 함정 사례들 -- anchored 판정만이
     * 이들을 정확히 거부함을 증명한다(Slice 100E evidence 항목 5의 test matrix).
     */
    private static void testAnchorRejectsLookalikeValues() throws Exception {
        assertTrue("1.8 (two components only) rejected", !isFamilyMatch("1.8"));
        assertTrue("1.8.1 (wrong third component) rejected", !isFamilyMatch("1.8.1"));
        assertTrue("1.8.01 (no underscore separator) rejected", !isFamilyMatch("1.8.01"));
        assertTrue("11.8.0 (wrong leading component) rejected", !isFamilyMatch("11.8.0"));
        assertTrue("x1.8.0_111 (leading garbage) rejected", !isFamilyMatch("x1.8.0_111"));
        assertTrue("1.8.0_ (trailing underscore, no digits) rejected", !isFamilyMatch("1.8.0_"));
        assertTrue("1.8.0_ABC (non-digit update) rejected", !isFamilyMatch("1.8.0_ABC"));
    }

    private static void testEmptyAndUndefinedTokenRejected() throws Exception {
        assertTrue("empty token rejected", !isFamilyMatch(""));
        assertTrue("null token (undefined/parse failure) rejected", !isFamilyMatch(null));
    }

    private static void testJavaVersionLineTokenExtraction() throws Exception {
        assertEquals("java -version line -> exact token", "1.8.0_503",
                extractJavaToken("java version \"1.8.0_503\""));
        assertEquals("java -version line -> exact token (111)", "1.8.0_111",
                extractJavaToken("java version \"1.8.0_111\""));
        assertEquals("java -version line -> exact token (bare family)", "1.8.0",
                extractJavaToken("java version \"1.8.0\""));
    }

    private static void testJavacVersionLineTokenExtraction() throws Exception {
        assertEquals("javac -version line -> exact token", "1.8.0_503",
                extractJavacToken("javac 1.8.0_503"));
        assertEquals("javac -version line -> exact token (111)", "1.8.0_111",
                extractJavacToken("javac 1.8.0_111"));
    }

    /**
     * java/javac 둘 다 family이기만 하면 PASS다 -- 서로 다른 update suffix 조합도 포함한다
     * (JAVA_AND_JAVAC_SAME_UPDATE_SUFFIX_REQUIRED = FALSE, same-home filesystem 비교는 이
     * gate에 존재하지 않는다).
     */
    private static void testAllPassPairsAccepted() throws Exception {
        assertTrue("java=1.8.0_111 / javac=1.8.0_111 -> accept",
                isFamilyMatch(extractJavaToken("java version \"1.8.0_111\""))
                        && isFamilyMatch(extractJavacToken("javac 1.8.0_111")));
        assertTrue("java=1.8.0_503 / javac=1.8.0_503 -> accept",
                isFamilyMatch(extractJavaToken("java version \"1.8.0_503\""))
                        && isFamilyMatch(extractJavacToken("javac 1.8.0_503")));
        assertTrue("java=1.8.0_111 / javac=1.8.0_503 (mixed update) -> accept",
                isFamilyMatch(extractJavaToken("java version \"1.8.0_111\""))
                        && isFamilyMatch(extractJavacToken("javac 1.8.0_503")));
        assertTrue("java=1.8.0 (bare) / javac=1.8.0_503 -> accept",
                isFamilyMatch(extractJavaToken("java version \"1.8.0\""))
                        && isFamilyMatch(extractJavacToken("javac 1.8.0_503")));
    }

    /** 사용자 요구의 핵심 사례를 별도로 명시적 회귀로 고정한다. */
    private static void testMixedUpdateSuffixPairAccepted() throws Exception {
        boolean javaFamily = isFamilyMatch(extractJavaToken("java version \"1.8.0_111\""));
        boolean javacFamily = isFamilyMatch(extractJavacToken("javac 1.8.0_503"));
        assertTrue("java=1.8.0_111 precondition is family", javaFamily);
        assertTrue("javac=1.8.0_503 precondition is family", javacFamily);
        assertTrue("mixed update suffix, both family -> overall PASS (AND of two independent checks)",
                javaFamily && javacFamily);
    }

    private static void testOneFamilyOneNonFamilyIsOverallReject() throws Exception {
        boolean javaFamily = isFamilyMatch(extractJavaToken("java version \"11.0.1\""));
        boolean javacFamily = isFamilyMatch(extractJavacToken("javac 1.8.0_111"));
        assertTrue("java non-family precondition", !javaFamily);
        assertTrue("javac family precondition", javacFamily);
        assertTrue("java non-family + javac family -> overall reject",
                !(javaFamily && javacFamily));

        boolean javaFamily2 = isFamilyMatch(extractJavaToken("java version \"1.8.0_111\""));
        boolean javacFamily2 = isFamilyMatch(extractJavacToken("javac 17.0.2"));
        assertTrue("java family precondition (2)", javaFamily2);
        assertTrue("javac non-family precondition (2)", !javacFamily2);
        assertTrue("java family + javac non-family -> overall reject",
                !(javaFamily2 && javacFamily2));
    }

    private static void testBothNonFamilyIsOverallReject() throws Exception {
        boolean javaFamily = isFamilyMatch(extractJavaToken("java version \"9\""));
        boolean javacFamily = isFamilyMatch(extractJavacToken("javac 9"));
        assertTrue("both non-family -> overall reject", !(javaFamily && javacFamily));
    }

    /** java -version/javac -version 출력이 예상 형식이 아니어서 토큰 추출 자체가 실패한 경우. */
    private static void testParseFailurePairIsOverallReject() throws Exception {
        String javaToken = extractJavaToken("Unexpected Corrupted Output With No Quotes");
        boolean javaFamily = isFamilyMatch(javaToken);
        assertTrue("unparseable java -version output -> not family", !javaFamily);

        String javacToken = extractJavacToken("");
        boolean javacFamily = isFamilyMatch(javacToken);
        assertTrue("empty javac -version output -> not family", !javacFamily);

        assertTrue("parse failure on either side -> overall reject",
                !(javaFamily && javacFamily));
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }

    private static void assertEquals(String message, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            failures++;
            System.out.println("FAILED: " + message + " -- expected=<" + expected + "> actual=<" + actual + ">");
        }
    }
}
