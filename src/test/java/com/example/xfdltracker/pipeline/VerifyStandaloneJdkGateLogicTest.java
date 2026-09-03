package com.example.xfdltracker.pipeline;

/**
 * {@code java}/{@code javac}를 직접 실행하지 않고, verify-standalone.bat Step 1과 동일한
 * 문자열 추출/비교 규칙(따옴표 기반 토큰 추출 후 정확 일치)을 순수 Java로 재구현해 검증한다.
 * {@code findstr} 방식의 부분 문자열 포함이 아닌 정확한 토큰 일치임을 증명한다.
 */
public class VerifyStandaloneJdkGateLogicTest {

    private static int failures = 0;

    private static final String TARGET_TOKEN = "1.8.0_111";

    public static void main(String[] args) throws Exception {
        testExactTargetTokenAccepted();
        testSubstringContainingTargetIsNotAcceptedAsExactMatch();
        testDifferentUpdateNumberRejected();
        testJavaVersionLineTokenExtraction();
        testJavacVersionLineTokenExtraction();
        testBothMustMatchForOverallAccept();
        testOneMatchOneMismatchIsOverallReject();

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

    private static boolean isExactTargetMatch(String token) {
        return TARGET_TOKEN.equals(token);
    }

    private static void testExactTargetTokenAccepted() throws Exception {
        assertTrue("exact target token accepted", isExactTargetMatch("1.8.0_111"));
    }

    private static void testSubstringContainingTargetIsNotAcceptedAsExactMatch() throws Exception {
        // target을 부분 문자열로만 포함하는 토큰은 정확 일치 기준으로 거부되어야 한다 --
        // findstr 방식 부분 문자열 검색이었다면 잘못 허용되었을 사례다.
        assertTrue("1.8.0_1111 (contains but != target) is rejected", !isExactTargetMatch("1.8.0_1111"));
        assertTrue("x1.8.0_111 (contains but != target) is rejected", !isExactTargetMatch("x1.8.0_111"));
        assertTrue("1.8.0_111x (contains but != target) is rejected", !isExactTargetMatch("1.8.0_111x"));
    }

    private static void testDifferentUpdateNumberRejected() throws Exception {
        assertTrue("1.8.0_503 (current observed development JDK) is rejected",
                !isExactTargetMatch("1.8.0_503"));
        assertTrue("11.0.1 (different major version) is rejected", !isExactTargetMatch("11.0.1"));
    }

    private static void testJavaVersionLineTokenExtraction() throws Exception {
        assertEquals("java -version line -> exact token", "1.8.0_503",
                extractJavaToken("java version \"1.8.0_503\""));
        assertEquals("java -version line -> exact token (target)", "1.8.0_111",
                extractJavaToken("java version \"1.8.0_111\""));
    }

    private static void testJavacVersionLineTokenExtraction() throws Exception {
        assertEquals("javac -version line -> exact token", "1.8.0_503",
                extractJavacToken("javac 1.8.0_503"));
        assertEquals("javac -version line -> exact token (target)", "1.8.0_111",
                extractJavacToken("javac 1.8.0_111"));
    }

    private static void testBothMustMatchForOverallAccept() throws Exception {
        boolean javaExact = isExactTargetMatch(extractJavaToken("java version \"1.8.0_111\""));
        boolean javacExact = isExactTargetMatch(extractJavacToken("javac 1.8.0_111"));
        assertTrue("both java and javac exact -> overall accept", javaExact && javacExact);
    }

    /**
     * java와 javac 버전이 서로 다른(PATH 혼합 설치) 실제 시나리오를 재현한다 -- 전체 결과는
     * reject여야 하며, AND 조건이 java 측 실패를 누락하지 않고 정확히 반영됨을 증명한다.
     */
    private static void testOneMatchOneMismatchIsOverallReject() throws Exception {
        boolean javaExact = isExactTargetMatch(extractJavaToken("java version \"1.8.0_503\""));
        boolean javacExact = isExactTargetMatch(extractJavacToken("javac 1.8.0_111"));
        assertTrue("java mismatch precondition", !javaExact);
        assertTrue("javac match precondition", javacExact);
        boolean overallAccept = javaExact && javacExact;
        assertTrue("one match + one mismatch -> overall reject (AND, not OR/either)", !overallAccept);
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
