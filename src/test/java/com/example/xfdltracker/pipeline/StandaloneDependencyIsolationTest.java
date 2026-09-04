package com.example.xfdltracker.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 외부 의존성 없는(non-JUnit) 정적 텍스트 감사. 표준 production 소스에서 legacy/외부 저장소
 * 참조를 스캔한다({@code RendererArchitectureIsolationTest}와 동일 컨벤션, 실제 파일 텍스트
 * 스캔이며 컴파일된 클래스 reflection이 아니다).
 */
public class StandaloneDependencyIsolationTest {

    private static int failures = 0;

    private static final String[] FORBIDDEN_TOKENS = {
            "XPlatformProjectConverter",
            "WebSquareGenerator",
            "XfdlToWebSquare",
            "offline-import",
            "offline_import",
            "D:\\Claude\\Projects\\xplatform-websquare-validation",
            "xplatform-websquare-template-catalog"
    };

    /**
     * production 트리 전체(약 156개 파일)를 재귀 스캔한다(패키지 화이트리스트는 새 파일
     * 누락 위험이 있어 폐기). {@code EXCLUDED_DORMANT_FILES} 3건만 제외되며, 그 정당성은
     * {@link #testExcludedDormantFilesAreNotReferencedByInScopeFiles()}가 증명한다.
     */
    private static final String[] EXCLUDED_DORMANT_FILES = {
            "src/main/java/com/example/xfdltracker/project/XPlatformProjectConverter.java",
            "src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java",
            "src/main/java/com/example/xfdltracker/XfdlToWebSquare.java"
    };

    /**
     * 재귀 스캔에 반드시 포함되어야 하는 핵심 소스 파일 목록(디렉터리 열거 가정이 아니라
     * 직접 검증한다).
     */
    private static final String[] REQUIRED_COVERAGE_FILES = {
            "src/main/java/com/example/xfdltracker/parser/XfdlReader.java",
            "src/main/java/com/example/xfdltracker/XfdlFunctionTracker.java",
            "src/main/java/com/example/xfdltracker/pipeline/TargetWebSquarePipeline.java",
            "src/main/java/com/example/xfdltracker/analyzer/SemanticRegionSegmenter.java",
            "src/main/java/com/example/xfdltracker/analyzer/ComponentPredicateAnalyzer.java",
            "src/main/java/com/example/xfdltracker/converter/ComponentLayoutConverter.java"
    };

    /** {@code TargetDocumentAssembler}/{@code TargetXmlSerializer}는 source analyzer/parser 패키지를 import하면 안 된다. */
    private static final String[] ASSEMBLER_SERIALIZER_FORBIDDEN_IMPORT_TOKENS = {
            "import com.example.xfdltracker.analyzer.",
            "import com.example.xfdltracker.parser.",
            "import com.example.xfdltracker.semantic.",
            "import com.example.xfdltracker.runtime."
    };

    public static void main(String[] args) throws Exception {
        testNoForbiddenTokenInAnyScannedFile();
        testRequiredCoverageFilesAreInScanSet();
        testExcludedDormantFilesAreNotReferencedByInScopeFiles();
        testAssemblerAndSerializerImportNoSourceOrRuntimePackage();
        testRuntimeResolverNotInvokedFromRendererOrAssembler();
        testScanLogicActuallyRejectsAnExplicitForbiddenDependency();
        testVerificationScriptsDoNotInvokeForbiddenLegacyClasses();
        testDisabledLegacyScriptsHaveNoLegacyGeneratedOutputAuthority();
        testDocumentationDoesNotInstructUseOfForbiddenExternalRepository();
        testOperationalDocumentationDirectsToCurrentPipelineNotLegacy();
        testDisabledLegacyScriptsHaveNoReachableInvocationLine();
        testEchoWithCommandSubstitutionIsClassifiedExecutable();
        testDisabledOperationalScriptsExecuteNoGitCommand();
        testRecursiveOperationalScriptScanFindsNoForbiddenExecutableInvocation();
        testDisabledLegacyScriptsTerminateWithBlockerBeforeAnyLegacyLogic();
        testVerifyOfflineBatIsThinDelegatorOnly();
        testVerifyOfflineShHasNoIndependentVerificationLogic();
        testOfflineUserGuideDocxReflectsCurrentOperationalStanding();
        testNoForbiddenSectionSignCharacterInReviewRelevantTextFiles();
        testProjectOwnedJavaCommentGroupsDoNotExceedThreeContentLines();
        testProjectOwnedScriptCommentGroupsDoNotExceedThreeContentLines();
        testCommentDetectorFlagsKnownEnglishOnlyExamples();
        testJavaLexicalCommentExtractorHandlesInlineAndStringLiterals();
        testProjectOwnedJavaCommentContentLinesHaveKoreanContext();
        testProjectOwnedScriptCommentContentLinesHaveKoreanContext();
        testGitAttributesDeclaresNonTextPolicyForWindowsScripts();
        testAuthoritativeExactJdkGateScriptsAreCoveredByNonTextPolicy();
        testGovernedWindowsScriptsHaveCrlfOnlyRawBytes();
        testGovernedWindowsScriptInventoryHasNoUndeclaredAddition();
        testAcceptedPathHasNoTabRuntimeScriptGeneratorReference();
        testAcceptedTabControlOutputContainsNoLegacyGetScopeOrRuntimeScript();
        testAcceptedPathHasNoLegacyClassMergeHelperReference();
        testAcceptedButtonGridClassOutputMatchesAuditedContract();

        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println(failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static final String PRODUCTION_ROOT = "src/main/java/com/example/xfdltracker";

    /** {@code dir} 하위 모든 {@code .java} 파일을 repo-root 상대경로로 재귀 수집한다. */
    private static void collectJavaFiles(File repoRoot, File dir, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        String repoRootPath = repoRoot.getAbsolutePath().replace('\\', '/');
        for (File child : children) {
            if (child.isDirectory()) {
                collectJavaFiles(repoRoot, child, out);
            } else if (child.getName().endsWith(".java")) {
                String abs = child.getAbsolutePath().replace('\\', '/');
                String rel = abs.startsWith(repoRootPath) ? abs.substring(repoRootPath.length() + 1) : abs;
                out.add(rel);
            }
        }
    }

    private static void testNoForbiddenTokenInAnyScannedFile() throws Exception {
        File root = repositoryRoot();
        File productionRoot = new File(root, PRODUCTION_ROOT);
        assertTrue("dependency-audit: production root exists: " + PRODUCTION_ROOT, productionRoot.isDirectory());

        List<String> allFiles = new ArrayList<String>();
        collectJavaFiles(root, productionRoot, allFiles);
        assertTrue("dependency-audit: recursive scan found a substantial number of production "
                + "files (full-tree coverage, Part G)", allFiles.size() >= 100);

        List<String> excluded = Arrays.asList(EXCLUDED_DORMANT_FILES);
        List<String> scanned = new ArrayList<String>();
        for (String relativePath : allFiles) {
            if (!excluded.contains(relativePath)) {
                scanned.add(relativePath);
            }
        }
        assertTrue("dependency-audit: exclusion set removed exactly " + EXCLUDED_DORMANT_FILES.length
                        + " file(s) from the full recursive scan",
                allFiles.size() - scanned.size() == EXCLUDED_DORMANT_FILES.length);

        int scannedCount = 0;
        for (String relativePath : scanned) {
            File f = new File(root, relativePath);
            assertTrue("dependency-audit: scanned file exists: " + relativePath, f.isFile());
            // 주석/javadoc은 forbidden-token 검사 전에 제거한다(순수 설명용 언급은 실제
            // 코드 의존성이 아니므로) -- 실제 import/호출은 주석 제거 후에도 그대로 걸린다.
            String text = stripComments(
                    new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            for (String token : FORBIDDEN_TOKENS) {
                assertTrue("dependency-audit: " + relativePath + " must not contain forbidden token \"" + token
                                + "\" in actual code (comments excluded)",
                        !text.contains(token));
            }
            scannedCount++;
        }
        assertTrue("dependency-audit: at least 100 production files scanned across the full "
                + "recursive standalone tree (Part G full-coverage strengthening)", scannedCount >= 100);
    }

    /**
     * 재귀 스캔 대상에 핵심 production 파일(XfdlReader/XfdlFunctionTracker/
     * TargetWebSquarePipeline 등)이 실제로 포함되는지, 같은 재귀 목록을 직접 대조해
     * 확인한다(수기 목록 가정이 아님).
     */
    private static void testRequiredCoverageFilesAreInScanSet() throws Exception {
        File root = repositoryRoot();
        File productionRoot = new File(root, PRODUCTION_ROOT);
        List<String> allFiles = new ArrayList<String>();
        collectJavaFiles(root, productionRoot, allFiles);
        for (String required : REQUIRED_COVERAGE_FILES) {
            assertTrue("dependency-audit: required coverage file is present in the recursive scan "
                            + "set: " + required,
                    allFiles.contains(required));
        }
        // behavior/runtime/renderer 패키지별로 최소 1개 파일씩 커버되는지 확인한다.
        String[] requiredCoveragePackages = {
                "src/main/java/com/example/xfdltracker/behavior",
                "src/main/java/com/example/xfdltracker/runtime",
                "src/main/java/com/example/xfdltracker/renderer",
                "src/main/java/com/example/xfdltracker/payload",
                "src/main/java/com/example/xfdltracker/composition"
        };
        for (String pkg : requiredCoveragePackages) {
            boolean found = false;
            for (String f : allFiles) {
                if (f.startsWith(pkg + "/")) {
                    found = true;
                    break;
                }
            }
            assertTrue("dependency-audit: required coverage package has at least one scanned file: " + pkg, found);
        }
    }

    /**
     * 제외된 3개 dormant 파일이 다른 모든 scanned 파일에서 클래스명으로 참조되지 않음을
     * 별도로 증명한다({@code testNoForbiddenTokenInAnyScannedFile}과 같은 재귀 집합을
     * 재확인, defense-in-depth).
     */
    private static void testExcludedDormantFilesAreNotReferencedByInScopeFiles() throws Exception {
        File root = repositoryRoot();
        File productionRoot = new File(root, PRODUCTION_ROOT);
        List<String> allFiles = new ArrayList<String>();
        collectJavaFiles(root, productionRoot, allFiles);
        List<String> excluded = Arrays.asList(EXCLUDED_DORMANT_FILES);
        String[] excludedClassNames = {"XPlatformProjectConverter", "WebSquareGenerator", "XfdlToWebSquare"};

        for (String relativePath : allFiles) {
            if (excluded.contains(relativePath)) {
                continue;
            }
            File f = new File(root, relativePath);
            String text = stripComments(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            for (String className : excludedClassNames) {
                assertTrue("dependency-audit: in-scope file " + relativePath
                                + " must not import/reference excluded dormant class \"" + className + "\"",
                        !text.contains(className));
            }
        }
    }

    private static void testAssemblerAndSerializerImportNoSourceOrRuntimePackage() throws Exception {
        File root = repositoryRoot();
        String[] targets = {
                "src/main/java/com/example/xfdltracker/renderer/TargetDocumentAssembler.java",
                "src/main/java/com/example/xfdltracker/renderer/TargetXmlSerializer.java"
        };
        for (String relativePath : targets) {
            String text = new String(
                    Files.readAllBytes(new File(root, relativePath).toPath()), StandardCharsets.UTF_8);
            for (String forbiddenImport : ASSEMBLER_SERIALIZER_FORBIDDEN_IMPORT_TOKENS) {
                assertTrue(relativePath + ": must not import forbidden package prefix \"" + forbiddenImport + "\"",
                        !text.contains(forbiddenImport));
            }
        }
    }

    private static void testRuntimeResolverNotInvokedFromRendererOrAssembler() throws Exception {
        File root = repositoryRoot();
        List<String> rendererFiles = Arrays.asList(
                "src/main/java/com/example/xfdltracker/renderer/AtomicWebSquareRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/CompositionRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/TargetDocumentAssembler.java");
        for (String relativePath : rendererFiles) {
            String text = new String(
                    Files.readAllBytes(new File(root, relativePath).toPath()), StandardCharsets.UTF_8);
            assertTrue(relativePath + ": must not reference RuntimeCapabilityResolver",
                    !text.contains("RuntimeCapabilityResolver"));
        }
    }

    /** {@code RendererArchitectureIsolationTest}와 동일한 최소 주석 제거 구현 -- {@code //}와
     * 블록 주석(javadoc 포함)을 지우고 실제 코드만 남긴다. */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
            } else {
                out.append(source.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    /**
     * 부정 회귀: 스캔 로직 자체(주석 제거 + forbidden-token 포함 검사)가 실제 forbidden
     * dependency를 정말로 거부하는지, 임시 파일에 직접 토큰을 넣어 확인한다(현재 파일이
     * 우연히 깨끗한 것만으로는 부족함).
     */
    private static void testScanLogicActuallyRejectsAnExplicitForbiddenDependency() throws Exception {
        File tempFile = File.createTempFile("dependency-audit-negative-fixture", ".java");
        tempFile.deleteOnExit();
        try {
            String forbiddenCode = "package com.example.fixture;\n"
                    + "import com.example.xfdltracker.converter.WebSquareGenerator;\n"
                    + "public class Fixture { void m() { new XPlatformProjectConverter(); } }\n";
            Files.write(tempFile.toPath(), forbiddenCode.getBytes(StandardCharsets.UTF_8));
            String scanned = stripComments(
                    new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8));
            boolean detectedWebSquareGenerator = scanned.contains("WebSquareGenerator");
            boolean detectedXPlatformProjectConverter = scanned.contains("XPlatformProjectConverter");
            assertTrue("dependency-audit-negative: scan logic detects a real (non-comment) "
                    + "WebSquareGenerator reference", detectedWebSquareGenerator);
            assertTrue("dependency-audit-negative: scan logic detects a real (non-comment) "
                    + "XPlatformProjectConverter reference", detectedXPlatformProjectConverter);

            String commentOnlyCode = "package com.example.fixture;\n"
                    + "// mentions WebSquareGenerator only in a comment, never in real code\n"
                    + "public class Fixture2 { }\n";
            Files.write(tempFile.toPath(), commentOnlyCode.getBytes(StandardCharsets.UTF_8));
            String scannedCommentOnly = stripComments(
                    new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8));
            assertTrue("dependency-audit-negative: comment-only mention is correctly excluded "
                    + "(proves the audit is precise, not merely permissive)",
                    !scannedCommentOnly.contains("WebSquareGenerator"));
        } finally {
            tempFile.delete();
        }
    }

    /**
     * standalone verifier 권위 스크립트(및 그 위임 wrapper)는 forbidden legacy 클래스를
     * 호출하면 안 된다. convert-sample 계열/BUILD-AND-VERIFY.sh는 이미 별도로 blocker 처리된
     * legacy demo 스크립트라 여기서는 제외한다.
     */
    private static void testVerificationScriptsDoNotInvokeForbiddenLegacyClasses() throws Exception {
        File root = repositoryRoot();
        String[] verificationScripts = {
                "verify-standalone.bat",
                "verify-offline.bat",
                "verify-offline.sh"
        };
        String[] forbiddenInvocationTokens = {
                "XPlatformProjectConverter",
                "WebSquareGenerator",
                "convert-sample"
        };
        for (String relativePath : verificationScripts) {
            File f = new File(root, relativePath);
            assertTrue("script-audit: verification script exists: " + relativePath, f.isFile());
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            for (String token : forbiddenInvocationTokens) {
                assertTrue("script-audit: " + relativePath + " must not operationally invoke/reference \""
                                + token + "\"",
                        !text.contains(token));
            }
        }
    }

    /**
     * 현재 운영 문서가 forbidden 외부 legacy 저장소를 clone/사용하라고 안내하면 안 된다.
     * 주요 폐쇄망 사용자 가이드 2종에서 해당 URL을 스캔한다.
     */
    private static void testDocumentationDoesNotInstructUseOfForbiddenExternalRepository() throws Exception {
        File root = repositoryRoot();
        String[] docs = {
                "README-OFFLINE.md",
                "docs/OFFLINE-USER-GUIDE.md",
                "closed-network-import/README-KO.md"
        };
        String forbiddenUrl = "github.com/gwanghwa27";
        for (String relativePath : docs) {
            File f = new File(root, relativePath);
            assertTrue("doc-audit: documentation file exists: " + relativePath, f.isFile());
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue("doc-audit: " + relativePath + " must not instruct use of the forbidden "
                            + "external legacy repository (" + forbiddenUrl + ")",
                    !text.contains(forbiddenUrl));
        }
    }

    /**
     * 운영 문서 3종(closed-network-import/README-KO.md 포함)이 모두 TargetWebSquarePipeline을
     * accepted 경로로 안내하고, 실행 가능한 legacy converter/generator 호출 예시가 없는지
     * 확인한다(비운영/역사적 라벨이 붙은 언급은 실행 명령이 아니므로 허용).
     */
    private static void testOperationalDocumentationDirectsToCurrentPipelineNotLegacy() throws Exception {
        File root = repositoryRoot();
        String[] docs = {
                "README-OFFLINE.md",
                "docs/OFFLINE-USER-GUIDE.md",
                "closed-network-import/README-KO.md"
        };
        String[] forbiddenClassNames = {"XPlatformProjectConverter", "WebSquareGenerator"};
        for (String relativePath : docs) {
            File f = new File(root, relativePath);
            assertTrue("doc-audit: documentation file exists: " + relativePath, f.isFile());
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);

            assertTrue("doc-audit: " + relativePath + " directs users to TargetWebSquarePipeline",
                    text.contains("TargetWebSquarePipeline"));

            // 실행 가능한 "java -cp ... <forbiddenClass>" 예시는 문서 어디에도 있으면 안 된다.
            for (String rawLine : text.split("\n")) {
                String line = rawLine.trim();
                boolean looksLikeJavaInvocation = line.startsWith("java ") || line.contains(" java -cp")
                        || line.contains("java -cp");
                if (!looksLikeJavaInvocation) {
                    continue;
                }
                for (String className : forbiddenClassNames) {
                    assertTrue("doc-audit: " + relativePath + " must not present a runnable java "
                                    + "invocation naming forbidden class \"" + className + "\": \"" + line + "\"",
                            !line.contains(className));
                }
            }

            // 비활성화 스크립트 파일명 그대로가 독립 명령줄로 등장하면 실행 예시로 간주한다
            // (forward-slash/backslash 경로 형태 모두 검사).
            String[] disabledScriptCommandForms = {
                    "convert-sample.bat", "./convert-sample.sh",
                    "closed-network-import/BUILD-AND-VERIFY.sh", "sh closed-network-import/BUILD-AND-VERIFY.sh",
                    "closed-network-import\\BUILD-AND-VERIFY.cmd", "closed-network-import/BUILD-AND-VERIFY.cmd"
            };
            for (String rawLine : text.split("\n")) {
                String line = rawLine.trim();
                for (String cmd : disabledScriptCommandForms) {
                    boolean isBareCommandLine = line.equals(cmd)
                            || line.startsWith(cmd + " ") || line.startsWith(cmd + "\t");
                    assertTrue("doc-audit: " + relativePath + " must not present a bare runnable "
                                    + "command line for the disabled legacy script \"" + cmd + "\": \"" + line + "\"",
                            !isBareCommandLine);
                }
            }
        }
    }

    /**
     * 비활성화된 스크립트마다 legacy 변환 명령이 실제로 도달 불가함을 증명한다(NOTICE 존재
     * 여부만으로는 부족). 순수 주석/echo 줄은 제외하고, 남은 "실행 가능" 줄에 forbidden
     * legacy 클래스명이 없는지 확인한다.
     */
    /**
     * 플랫폼 쌍(.bat/.sh 등) 4개 operational script를 모두 이 상수에 나열한다 -- 한쪽
     * 플랫폼(.cmd)만 빠져 감사를 피해가는 사고를 방지한다.
     */
    private static final String[] DISABLED_LEGACY_SCRIPTS = {
            "convert-sample.bat",
            "convert-sample.sh",
            "closed-network-import/BUILD-AND-VERIFY.cmd",
            "closed-network-import/BUILD-AND-VERIFY.sh",
            "tools/build-pipeline-trace.bat",
            "tools/build-pipeline-trace.sh"
    };

    /**
     * shell 스크립트는 echo 줄이라도 {@code $(...)}/backtick 치환부가 있으면 실행 가능한
     * 것으로 취급한다({@code echo "HEAD=$(git rev-parse HEAD)"}가 실제로 git을 실행하는
     * 사례). batch에는 이 위험이 없어 순수 REM/echo 줄만 안전 처리한다.
     */
    private static List<String> extractExecutableSegments(String content, boolean isShellScript) {
        List<String> segments = new ArrayList<String>();
        for (String rawLine : content.split("\n")) {
            String line = rawLine.trim();
            String lower = line.toLowerCase(java.util.Locale.ROOT);
            boolean isCommentLine = isShellScript
                    ? lower.startsWith("#")
                    : (lower.startsWith("rem ") || lower.equals("rem"));
            if (isCommentLine) {
                continue;
            }
            if (isShellScript) {
                // 순수 echo 줄 자체는 inert이지만, $(...)/backtick 치환부는 echo 여부와 무관하게
                // 항상 실행 가능한 것으로 추출한다(치환 안의 git 호출 등을 놓치지 않기 위함).
                boolean isBareEchoLine = lower.startsWith("echo");
                if (!isBareEchoLine) {
                    segments.add(line);
                }
                java.util.regex.Matcher dollarParen =
                        java.util.regex.Pattern.compile("\\$\\(([^)]*)\\)").matcher(line);
                while (dollarParen.find()) {
                    segments.add(dollarParen.group(1));
                }
                java.util.regex.Matcher backtick =
                        java.util.regex.Pattern.compile("`([^`]*)`").matcher(line);
                while (backtick.find()) {
                    segments.add(backtick.group(1));
                }
            } else {
                boolean isBareEchoLine = lower.startsWith("echo");
                if (!isBareEchoLine) {
                    segments.add(line);
                }
            }
        }
        return segments;
    }

    private static boolean isShellScriptPath(String relativePath) {
        return relativePath.endsWith(".sh") || relativePath.endsWith(".ps1");
    }

    private static void testDisabledLegacyScriptsHaveNoReachableInvocationLine() throws Exception {
        File root = repositoryRoot();
        String[] forbiddenClassNames = {"XPlatformProjectConverter", "WebSquareGenerator"};
        for (String relativePath : DISABLED_LEGACY_SCRIPTS) {
            File f = new File(root, relativePath);
            assertTrue("legacy-script-audit: disabled script exists: " + relativePath, f.isFile());
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            List<String> segments = extractExecutableSegments(content, isShellScriptPath(relativePath));
            for (String segment : segments) {
                for (String className : forbiddenClassNames) {
                    assertTrue("legacy-script-audit: " + relativePath + " has a potentially "
                                    + "executable segment referencing forbidden class \""
                                    + className + "\": \"" + segment + "\"",
                            !segment.contains(className));
                }
            }
        }
    }

    /**
     * {@code echo "HEAD=$(git rev-parse HEAD)"}가 실행 가능한 git 호출로 분류되는지 확인하는
     * 정확한 회귀 -- "echo 줄은 항상 안전"이라는 옛 규칙이 더 이상 유효하지 않음을 증명한다.
     */
    private static void testEchoWithCommandSubstitutionIsClassifiedExecutable() throws Exception {
        String fixture = "echo \"HEAD=$(git rev-parse HEAD)\"";
        List<String> segments = extractExecutableSegments(fixture, true);
        boolean foundGitSubstitution = false;
        for (String segment : segments) {
            if (segment.contains("git rev-parse HEAD")) {
                foundGitSubstitution = true;
            }
        }
        assertTrue("shell-audit: echo line containing $(git rev-parse HEAD) is classified as "
                + "executable (command substitution extracted), not silently skipped as a bare echo",
                foundGitSubstitution);
    }

    /**
     * 비활성화된 operational script는 직접 호출이든 shell 치환 경유든 실행 가능한 git
     * 서브커맨드를 포함하면 안 된다(DISABLED_PRE_GIT_OPERATIONAL_SCRIPTS_EXECUTE_GIT = FALSE).
     */
    private static void testDisabledOperationalScriptsExecuteNoGitCommand() throws Exception {
        File root = repositoryRoot();
        String[] gitSubcommands = {
                "status", "diff", "log", "show", "remote", "rev-parse", "ls-files",
                "add", "commit", "push", "fetch", "pull", "checkout", "switch", "branch", "tag", "init"
        };
        for (String relativePath : DISABLED_LEGACY_SCRIPTS) {
            File f = new File(root, relativePath);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            List<String> segments = extractExecutableSegments(content, isShellScriptPath(relativePath));
            for (String segment : segments) {
                for (String sub : gitSubcommands) {
                    boolean looksLikeGitInvocation = segment.contains("git " + sub)
                            || segment.contains("git\t" + sub);
                    assertTrue("git-execution-audit: " + relativePath + " has a potentially "
                                    + "executable segment invoking \"git " + sub + "\": \"" + segment + "\"",
                            !looksLikeGitInvocation);
                }
            }
        }
    }

    /**
     * 비활성화된 스크립트마다 실제로 machine-readable blocker marker를 출력하고 non-zero로
     * 종료하는지 확인한다.
     */
    private static void testDisabledLegacyScriptsTerminateWithBlockerBeforeAnyLegacyLogic() throws Exception {
        File root = repositoryRoot();
        String[] disabledScripts = DISABLED_LEGACY_SCRIPTS;
        for (String relativePath : disabledScripts) {
            File f = new File(root, relativePath);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue("legacy-script-audit: " + relativePath + " emits the "
                            + "[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] machine-readable marker",
                    content.contains("[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]"));
            boolean hasNonZeroExit = content.contains("exit /b 1") || content.contains("exit 1")
                    || content.contains("FAIL=1");
            assertTrue("legacy-script-audit: " + relativePath + " terminates non-zero", hasNonZeroExit);
        }
    }

    /**
     * VERIFY_OFFLINE_BAT_IS_THIN_DELEGATOR = TRUE: verify-offline.bat는 verify-standalone.bat만
     * 호출해야 하며 compile/test/JDK-gate 로직을 재구현하면 안 된다.
     */
    private static void testVerifyOfflineBatIsThinDelegatorOnly() throws Exception {
        File root = repositoryRoot();
        File f = new File(root, "verify-offline.bat");
        String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue("verify-offline.bat: calls verify-standalone.bat",
                content.contains("verify-standalone.bat"));
        assertTrue("verify-offline.bat: does not reimplement javac compilation itself",
                !content.contains("javac -encoding"));
        assertTrue("verify-offline.bat: does not reimplement the exact-JDK findstr/token gate itself",
                !content.contains("TARGET_JDK_TOKEN"));
    }

    /**
     * verify-offline.sh는 cmd.exe로 verify-standalone.bat에 위임하거나 fail-closed해야 하며,
     * compile/test/JDK-gate 로직을 자체적으로 복제하면 안 된다.
     */
    private static void testVerifyOfflineShHasNoIndependentVerificationLogic() throws Exception {
        File root = repositoryRoot();
        File f = new File(root, "verify-offline.sh");
        String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue("verify-offline.sh: does not reimplement javac compilation itself",
                !content.contains("javac -encoding"));
        assertTrue("verify-offline.sh: does not reimplement running individual test classes itself",
                !content.contains("DIRECT_CLASSES") && !content.contains("TEST_CLASSES_DIR"));
        boolean delegatesViaCmd = content.contains("cmd.exe") && content.contains("verify-standalone.bat");
        boolean failsClosedWhenUnavailable = content.contains("VERIFIER_DELEGATION_UNAVAILABLE");
        assertTrue("verify-offline.sh: either delegates to verify-standalone.bat via a lawful "
                        + "command bridge, or fails closed when that bridge is unavailable",
                delegatesViaCmd && failsClosedWhenUnavailable);
    }

    /**
     * 비활성화된 4개 스크립트 중 어느 것도 legacy 변환 출력(class/state 정책, HOLD 누출,
     * XML well-formedness, Phase1 SHA)을 검증 authority로 취급하면 안 된다.
     */
    private static void testDisabledLegacyScriptsHaveNoLegacyGeneratedOutputAuthority() throws Exception {
        File root = repositoryRoot();
        String[] legacyOutputAuthorityTokens = {
                "class-policy-check.ps1", "xml-wellformed-check.ps1", "sample-output", "btn_cm", "wq_gvw"
        };
        for (String relativePath : DISABLED_LEGACY_SCRIPTS) {
            File f = new File(root, relativePath);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            for (String token : legacyOutputAuthorityTokens) {
                assertTrue("legacy-script-audit: " + relativePath + " must not treat legacy generated "
                                + "output as a validation authority (token \"" + token + "\" absent)",
                        !content.contains(token));
            }
        }
    }

    /**
     * 고정 목록({@code DISABLED_LEGACY_SCRIPTS})은 이미 두 차례 신규 스크립트를 놓친 적이
     * 있어, 저장소 전체의 *.bat/*.cmd/*.sh/*.ps1을 재귀 스캔한다(.git/build 제외) -- 새
     * 스크립트가 추가돼도 목록 갱신 없이 자동으로 감사된다.
     */
    private static void collectScriptFiles(File repoRoot, File dir, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        String repoRootPath = repoRoot.getAbsolutePath().replace('\\', '/');
        String[] scriptExtensions = {".bat", ".cmd", ".sh", ".ps1"};
        for (File child : children) {
            String name = child.getName();
            if (child.isDirectory()) {
                if (".git".equals(name) || "build".equals(name)) {
                    continue;
                }
                collectScriptFiles(repoRoot, child, out);
            } else {
                for (String ext : scriptExtensions) {
                    if (name.endsWith(ext)) {
                        String abs = child.getAbsolutePath().replace('\\', '/');
                        String rel = abs.startsWith(repoRootPath) ? abs.substring(repoRootPath.length() + 1) : abs;
                        out.add(rel);
                        break;
                    }
                }
            }
        }
    }

    private static void testRecursiveOperationalScriptScanFindsNoForbiddenExecutableInvocation() throws Exception {
        File root = repositoryRoot();
        List<String> allScripts = new ArrayList<String>();
        collectScriptFiles(root, root, allScripts);
        assertTrue("recursive-script-audit: found a substantial number of repository script files",
                allScripts.size() >= 10);

        boolean coversPipelineTraceBat = allScripts.contains("tools/build-pipeline-trace.bat");
        boolean coversPipelineTraceSh = allScripts.contains("tools/build-pipeline-trace.sh");
        assertTrue("recursive-script-audit: recursive scan set includes tools/build-pipeline-trace.bat "
                + "(OPERATIONAL_SCRIPT_AUDIT_COVERS_TOOLS_BUILD_PIPELINE_TRACE_BAT = TRUE)",
                coversPipelineTraceBat);
        assertTrue("recursive-script-audit: recursive scan set includes tools/build-pipeline-trace.sh "
                + "(OPERATIONAL_SCRIPT_AUDIT_COVERS_TOOLS_BUILD_PIPELINE_TRACE_SH = TRUE)",
                coversPipelineTraceSh);

        String[] forbiddenClassNames = {"XPlatformProjectConverter", "WebSquareGenerator"};
        for (String relativePath : allScripts) {
            File f = new File(root, relativePath);
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            List<String> segments = extractExecutableSegments(content, isShellScriptPath(relativePath));
            for (String segment : segments) {
                for (String className : forbiddenClassNames) {
                    assertTrue("recursive-script-audit: " + relativePath + " has a potentially "
                                    + "executable segment referencing forbidden class \""
                                    + className + "\": \"" + segment + "\"",
                            !segment.contains(className));
                }
            }
        }
    }

    /**
     * Java 표준 라이브러리만으로(Apache POI 등 없이) {@code docs/OFFLINE-USER-GUIDE.docx}가
     * 현재 운영 standing을 반영하는지 좁게 감사한다. DOCX는 ZIP이므로 {@link ZipFile}로
     * {@code word/document.xml} 원문을 읽어 확인한다.
     */
    private static void testOfflineUserGuideDocxReflectsCurrentOperationalStanding() throws Exception {
        File root = repositoryRoot();
        File docx = new File(root, "docs/OFFLINE-USER-GUIDE.docx");
        assertTrue("docx-audit: docs/OFFLINE-USER-GUIDE.docx exists", docx.isFile());
        if (!docx.isFile()) {
            return;
        }

        String documentXml;
        ZipFile zip = new ZipFile(docx);
        try {
            ZipEntry entry = zip.getEntry("word/document.xml");
            assertTrue("docx-audit: docs/OFFLINE-USER-GUIDE.docx contains word/document.xml "
                    + "(is a well-formed DOCX/OOXML package)", entry != null);
            if (entry == null) {
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream in = zip.getInputStream(entry);
            try {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
            } finally {
                in.close();
            }
            documentXml = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            zip.close();
        }

        assertTrue("docx-audit: word/document.xml mentions TargetWebSquarePipeline (accepted "
                + "conversion architecture)", documentXml.contains("TargetWebSquarePipeline"));
        assertTrue("docx-audit: word/document.xml mentions TargetRuntimeProfile (caller-supplied "
                + "profile requirement)", documentXml.contains("TargetRuntimeProfile"));
        assertTrue("docx-audit: word/document.xml shows the disabled-entrypoint blocker marker",
                documentXml.contains("CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER"));
        assertTrue("docx-audit: word/document.xml mentions verify-standalone.bat (standalone "
                + "verification authority)", documentXml.contains("verify-standalone.bat"));
        assertTrue("docx-audit: word/document.xml labels convert-sample.* as a non-operational "
                        + "legacy entrypoint (not a current runnable command)",
                documentXml.contains("non-operational") || documentXml.contains("disabled"));
        // Word가 run을 예측 불가하게 나누므로, 이 검사는 Reviewer가 지적한 특정 구버전
        // 문구만 가드한다(범용 파서 아님).
        assertTrue("docx-audit: word/document.xml does not contain the stale current-looking "
                        + "\"Sample 변환 방법\" heading without the non-operational "
                        + "qualifier immediately following it",
                !documentXml.contains("Sample 변환 방법 -- ")
                        || documentXml.contains("non-operational legacy entrypoint"));
    }

    private static final char FORBIDDEN_SECTION_SIGN = '\u00A7';

    /**
     * Slice 98BH(Post-ACCEPT Global Non-Semantic Cleanup, Part I) -- 회귀 가드: review-relevant
     * UTF-8 텍스트 파일에 forbidden section-sign 문자(항목 참조는 "항목 N" 형태만 허용)가 재등장하면
     * 실패한다. DOCX/PDF 등 바이너리는 별도 경로(word/document.xml, 렌더링 QA)로 검사한다.
     */
    private static void testNoForbiddenSectionSignCharacterInReviewRelevantTextFiles() throws Exception {
        File root = repositoryRoot();
        List<String> textFiles = new ArrayList<String>();
        collectJavaFiles(root, new File(root, "src/main/java"), textFiles);
        collectJavaFiles(root, new File(root, "src/test/java"), textFiles);
        collectFilesByExtension(root, new File(root, "docs"), ".md", textFiles);
        collectFilesByExtension(root, new File(root, "closed-network-import"), ".md", textFiles);
        collectFilesByExtension(root, new File(root, "analysis"), ".md", textFiles);
        String[] explicitFiles = {"README-OFFLINE.md", "CLAUDE.local.md"};
        for (String rel : explicitFiles) {
            if (new File(root, rel).isFile()) {
                textFiles.add(rel);
            }
        }
        assertTrue("section-sign-guard: at least one review-relevant text file was scanned",
                textFiles.size() > 50);

        int scanned = 0;
        for (String relativePath : textFiles) {
            File f = new File(root, relativePath);
            if (!f.isFile()) {
                continue;
            }
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertTrue("section-sign-guard: " + relativePath + " must not contain the forbidden "
                            + "section-sign character (use \"항목 N\" style Korean text instead)",
                    text.indexOf(FORBIDDEN_SECTION_SIGN) < 0);
            scanned++;
        }
        assertTrue("section-sign-guard: scanned a substantial number of review-relevant text files",
                scanned > 50);
    }

    /** {@code dir} 하위에서 {@code extension} 확장자 파일을 재귀 수집한다(repo-root 상대경로). */
    private static void collectFilesByExtension(File repoRoot, File dir, String extension, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        String repoRootPath = repoRoot.getAbsolutePath().replace('\\', '/');
        for (File child : children) {
            if (child.isDirectory()) {
                collectFilesByExtension(repoRoot, child, extension, out);
            } else if (child.getName().endsWith(extension)) {
                String abs = child.getAbsolutePath().replace('\\', '/');
                String rel = abs.startsWith(repoRootPath) ? abs.substring(repoRootPath.length() + 1) : abs;
                out.add(rel);
            }
        }
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction, Part F) -- project-owned Java 주석 블록/그룹이
     * 3줄(구분자 제외)을 넘지 않는지 회귀 검증한다. 표준 라이브러리(java.util.regex)만 사용한다.
     */
    private static void testProjectOwnedJavaCommentGroupsDoNotExceedThreeContentLines() throws Exception {
        File root = repositoryRoot();
        List<String> javaFiles = new ArrayList<String>();
        collectJavaFiles(root, new File(root, "src/main/java"), javaFiles);
        collectJavaFiles(root, new File(root, "src/test/java"), javaFiles);
        assertTrue("comment-line-limit-guard: scanned a substantial number of Java files",
                javaFiles.size() > 100);

        int overLimit = 0;
        for (String relativePath : javaFiles) {
            File f = new File(root, relativePath);
            if (!f.isFile()) {
                continue;
            }
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);

            java.util.regex.Matcher blockMatcher =
                    java.util.regex.Pattern.compile("/\\*.*?\\*/", java.util.regex.Pattern.DOTALL).matcher(text);
            while (blockMatcher.find()) {
                int contentLines = countJavaBlockContentLines(blockMatcher.group());
                assertTrue("comment-line-limit-guard: " + relativePath + " has a /* */ comment block "
                                + "exceeding 3 content lines (" + contentLines + " lines): "
                                + blockMatcher.group().substring(0, Math.min(80, blockMatcher.group().length())),
                        contentLines <= 3);
                if (contentLines > 3) {
                    overLimit++;
                }
            }

            String[] lines = text.split("\n", -1);
            int run = 0;
            for (int i = 0; i <= lines.length; i++) {
                boolean isLineComment = i < lines.length && lines[i].trim().startsWith("//");
                if (isLineComment) {
                    run++;
                } else {
                    if (run > 3) {
                        overLimit++;
                        assertTrue("comment-line-limit-guard: " + relativePath + " has a consecutive // "
                                        + "comment group exceeding 3 content lines (" + run + " lines) ending "
                                        + "near source line " + i, false);
                    }
                    run = 0;
                }
            }
        }
        assertTrue("comment-line-limit-guard: no Java comment block/group exceeds 3 content lines "
                + "(JAVA_COMMENT_GROUPS_OVER_3_CORE_LINES = 0)", overLimit == 0);
    }

    /** {@code /* ... *}{@code /} 블록의 실제 내용 줄 수를 센다(구분자 {@code /**}, {@code *}{@code /}, 단독 {@code *}는 제외). */
    private static int countJavaBlockContentLines(String block) {
        String[] lines = block.split("\n", -1);
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals("/**") || trimmed.equals("/*") || trimmed.equals("*/") || trimmed.equals("*")) {
                continue;
            }
            if (trimmed.length() == 0) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction, Part F) -- project-owned 스크립트(.bat/.cmd/
     * .sh/.ps1) 주석 그룹이 3줄을 넘지 않는지 검증한다. 이전에 놓쳤던 두 PowerShell 파일도
     * 재귀 스캔 대상에 포함됨을 명시적으로 확인한다.
     */
    private static void testProjectOwnedScriptCommentGroupsDoNotExceedThreeContentLines() throws Exception {
        File root = repositoryRoot();
        List<String> scripts = new ArrayList<String>();
        collectScriptFiles(root, root, scripts);
        assertTrue("comment-line-limit-guard: covers tools/file-sha256.ps1",
                scripts.contains("tools/file-sha256.ps1"));
        assertTrue("comment-line-limit-guard: covers tools/grp-main-style-check.ps1",
                scripts.contains("tools/grp-main-style-check.ps1"));

        int overLimit = 0;
        for (String relativePath : scripts) {
            File f = new File(root, relativePath);
            if (!f.isFile()) {
                continue;
            }
            boolean isBatch = relativePath.endsWith(".bat") || relativePath.endsWith(".cmd");
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String[] lines = content.split("\r\n|\n", -1);
            int run = 0;
            for (int i = 0; i <= lines.length; i++) {
                String trimmed = i < lines.length ? lines[i].trim() : "";
                String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
                boolean isCommentLine = i < lines.length && (isBatch
                        ? (lower.startsWith("rem ") || lower.equals("rem") || trimmed.startsWith("::"))
                        : (trimmed.startsWith("#") && !trimmed.startsWith("#!")));
                if (isCommentLine) {
                    run++;
                } else {
                    if (run > 3) {
                        overLimit++;
                        assertTrue("comment-line-limit-guard: " + relativePath + " has a comment group "
                                        + "exceeding 3 content lines (" + run + " lines) ending near source line "
                                        + i, false);
                    }
                    run = 0;
                }
            }
        }
        assertTrue("comment-line-limit-guard: no script comment group exceeds 3 content lines "
                + "(SCRIPT_COMMENT_GROUPS_OVER_3_CORE_LINES = 0)", overLimit == 0);
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction 3, Part A) -- 정확한 최종 규칙: 주석 내용 줄에
     * ASCII 알파벳이 하나라도 있으면 같은 줄에 한글이 있어야 한다. 단어 수 임계값이나 식별자/CamelCase/
     * ALL_CAPS 제거 같은 폭넓은 예외는 사용하지 않는다.
     */
    private static boolean lineIsEnglishOnlyNaturalLanguage(String contentLine) {
        return containsAsciiAlpha(contentLine) && !containsHangul(contentLine);
    }

    private static boolean containsAsciiAlpha(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsHangul(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '가' && c <= '힣') {
                return true;
            }
        }
        return false;
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction 3, Part B) -- Java 소스를 문자 단위로 순회하며
     * string/char literal, line/block(Javadoc) comment 상태를 정확히 구분하는 결정론적 lexical
     * scanner(외부 파서 의존성 없음). 반환값은 각 주석의 내용 줄(구분자 제거, trim 완료) 목록이다.
     */
    private static List<String> extractJavaCommentContentLines(String source) {
        List<String> out = new ArrayList<String>();
        int n = source.length();
        int i = 0;
        final int STATE_CODE = 0, STATE_STRING = 1, STATE_CHAR = 2, STATE_LINE_COMMENT = 3, STATE_BLOCK_COMMENT = 4;
        int state = STATE_CODE;
        StringBuilder buf = null;
        while (i < n) {
            char c = source.charAt(i);
            if (state == STATE_CODE) {
                if (c == '"') {
                    state = STATE_STRING;
                    i++;
                } else if (c == '\'') {
                    state = STATE_CHAR;
                    i++;
                } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                    state = STATE_LINE_COMMENT;
                    buf = new StringBuilder();
                    i += 2;
                } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                    state = STATE_BLOCK_COMMENT;
                    buf = new StringBuilder();
                    i += 2;
                } else {
                    i++;
                }
            } else if (state == STATE_STRING) {
                if (c == '\\' && i + 1 < n) {
                    i += 2;
                } else if (c == '"') {
                    state = STATE_CODE;
                    i++;
                } else if (c == '\n') {
                    state = STATE_CODE;
                    i++;
                } else {
                    i++;
                }
            } else if (state == STATE_CHAR) {
                if (c == '\\' && i + 1 < n) {
                    i += 2;
                } else if (c == '\'') {
                    state = STATE_CODE;
                    i++;
                } else if (c == '\n') {
                    state = STATE_CODE;
                    i++;
                } else {
                    i++;
                }
            } else if (state == STATE_LINE_COMMENT) {
                if (c == '\n') {
                    addCommentLine(out, buf.toString());
                    state = STATE_CODE;
                    buf = null;
                    i++;
                } else {
                    buf.append(c);
                    i++;
                }
            } else { // block comment 상태(STATE_BLOCK_COMMENT)를 처리한다.
                if (c == '*' && i + 1 < n && source.charAt(i + 1) == '/') {
                    for (String rawLine : buf.toString().split("\n", -1)) {
                        addCommentLine(out, rawLine);
                    }
                    state = STATE_CODE;
                    buf = null;
                    i += 2;
                } else {
                    buf.append(c);
                    i++;
                }
            }
        }
        if (state == STATE_LINE_COMMENT && buf != null) {
            addCommentLine(out, buf.toString());
        }
        return out;
    }

    private static void addCommentLine(List<String> out, String rawLine) {
        String t = rawLine.trim();
        if (t.startsWith("*")) {
            t = t.substring(1).trim();
        }
        if (t.length() > 0) {
            out.add(t);
        }
    }

    /** 알려진 영어 전용 예시(Reviewer 지적 사례)가 실제로 위반으로 걸리는지, 대응하는 한국어 수정본은
     * 통과하는지 확인하는 음성/양성 회귀. */
    private static void testCommentDetectorFlagsKnownEnglishOnlyExamples() throws Exception {
        String[] badExamples = {
                "positive direct call", "fixture helpers", "SEARCH_AREA", "Usage: -File foo.ps1",
                "Step 1: JDK/version gate", "no head child at all", "opening quote",
                "inline English comment", "@param value description", "@return result"
        };
        for (String bad : badExamples) {
            assertTrue("comment-language-guard: detector must flag known English-only example: \"" + bad + "\"",
                    lineIsEnglishOnlyNaturalLanguage(bad));
        }
        String[] goodExamples = {
                "opening quote를 건너뛴다.", "fixture 생성 도우미.", "SEARCH_AREA 렌더링.",
                "TargetWebSquarePipeline 경로를 사용한다.", "사용법: -File foo.ps1",
                "Step 1에서 JDK/version gate를 확인한다.", "@param value 입력 값.", "@return 변환 결과.",
                "{@link TargetWebSquarePipeline} 관련 경로.", "예: scwin.handleClick();",
                "직접 호출 성공 사례.", "GRID/TAB_CONTROL/SPLIT_LAYOUT 계열이다."
        };
        for (String good : goodExamples) {
            assertTrue("comment-language-guard: detector must NOT flag acceptable line: \"" + good + "\"",
                    !lineIsEnglishOnlyNaturalLanguage(good));
        }
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction 3, Part B) -- lexical comment extractor가 inline
     * 주석을 실제로 감사 대상에 포함하며, 문자열 리터럴 내부의 "//"를 주석으로 오인하지 않음을 확인한다.
     * INLINE_JAVA_LINE_COMMENTS_ARE_AUDITED / JAVA_STRING_LITERAL_SLASH_SLASH_IS_NOT_COMMENT 증거.
     */
    private static void testJavaLexicalCommentExtractorHandlesInlineAndStringLiterals() throws Exception {
        String inlineSource = "int x = 1; // inline English comment";
        List<String> inlineComments = extractJavaCommentContentLines(inlineSource);
        assertTrue("comment-language-guard: inline // comment must be extracted",
                inlineComments.size() == 1 && inlineComments.get(0).equals("inline English comment"));
        assertTrue("comment-language-guard: extracted inline comment must be flagged English-only",
                lineIsEnglishOnlyNaturalLanguage(inlineComments.get(0)));

        String stringLiteralSource = "String x = \"// not a comment\";";
        List<String> noComments = extractJavaCommentContentLines(stringLiteralSource);
        assertTrue("comment-language-guard: \"//\" inside a string literal must not be treated as a comment",
                noComments.isEmpty());

        String charLiteralSource = "char c = '/'; int y = 2; // real comment after char literal";
        List<String> afterCharLiteral = extractJavaCommentContentLines(charLiteralSource);
        assertTrue("comment-language-guard: comment after a char literal containing '/' must still be found",
                afterCharLiteral.size() == 1 && afterCharLiteral.get(0).equals("real comment after char literal"));

        String blockSource = "/** 첫 줄 설명.\n * second line without Korean\n */";
        List<String> blockLines = extractJavaCommentContentLines(blockSource);
        assertTrue("comment-language-guard: block comment must yield 2 content lines",
                blockLines.size() == 2);
        assertTrue("comment-language-guard: block comment second line must be flagged English-only",
                lineIsEnglishOnlyNaturalLanguage(blockLines.get(1)));
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction 3, Part B/D) -- project-owned Java 주석의 모든
     * 내용 줄(전체 줄 주석/inline 주석/block/Javadoc 포함)을 lexical scanner로 정확히 추출해
     * ASCII 알파벳이 있는데 한글이 없는 영어 전용 줄이 없는지 확인한다.
     */
    private static void testProjectOwnedJavaCommentContentLinesHaveKoreanContext() throws Exception {
        File root = repositoryRoot();
        List<String> javaFiles = new ArrayList<String>();
        collectJavaFiles(root, new File(root, "src/main/java"), javaFiles);
        collectJavaFiles(root, new File(root, "src/test/java"), javaFiles);

        int violations = 0;
        for (String relativePath : javaFiles) {
            File f = new File(root, relativePath);
            if (!f.isFile()) {
                continue;
            }
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            for (String content : extractJavaCommentContentLines(text)) {
                if (lineIsEnglishOnlyNaturalLanguage(content)) {
                    violations++;
                    assertTrue("comment-language-guard: " + relativePath + " has an English-only "
                                    + "comment content line with no Korean context: \"" + content + "\"", false);
                }
            }
        }
        assertTrue("comment-language-guard: no Java comment content line is English-only "
                + "(JAVA_COMMENT_CONTENT_LINES_WITH_ASCII_ALPHA_AND_NO_KOREAN = 0)", violations == 0);
    }

    /**
     * Slice 98BH(Final Pre-Git Cleanup Correction 2, Part E) -- project-owned 스크립트 주석의
     * 모든 내용 줄을 검사한다(shebang은 제외). 원리는 Java 검사와 동일하다.
     */
    private static void testProjectOwnedScriptCommentContentLinesHaveKoreanContext() throws Exception {
        File root = repositoryRoot();
        List<String> scripts = new ArrayList<String>();
        collectScriptFiles(root, root, scripts);

        int violations = 0;
        for (String relativePath : scripts) {
            File f = new File(root, relativePath);
            if (!f.isFile()) {
                continue;
            }
            boolean isBatch = relativePath.endsWith(".bat") || relativePath.endsWith(".cmd");
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String[] lines = content.split("\r\n|\n", -1);
            for (String rawLine : lines) {
                String t = rawLine.trim();
                String lower = t.toLowerCase(java.util.Locale.ROOT);
                boolean isCommentLine = isBatch
                        ? (lower.startsWith("rem ") || lower.equals("rem") || t.startsWith("::"))
                        : (t.startsWith("#") && !t.startsWith("#!"));
                if (!isCommentLine) {
                    continue;
                }
                String stripped = isBatch
                        ? t.replaceFirst("(?i)^(rem\\s+|::)", "").trim()
                        : t.replaceFirst("^#+\\s?", "").trim();
                if (stripped.length() == 0) {
                    continue;
                }
                if (lineIsEnglishOnlyNaturalLanguage(stripped)) {
                    violations++;
                    assertTrue("comment-language-guard: " + relativePath + " has an English-only "
                                    + "comment content line with no Korean context: \"" + stripped + "\"", false);
                }
            }
        }
        assertTrue("comment-language-guard: no script comment content line is English-only "
                + "(SCRIPT_COMMENT_CONTENT_LINES_WITH_ASCII_ALPHA_AND_NO_KOREAN = 0)", violations == 0);
    }

    /**
     * Slice 99F Correction 5 -- {@code .gitattributes}가 {@code *.bat}/{@code *.cmd}를 {@code
     * -text}로 선언하는지 저장소 소유 파일 자체에서 직접 검증한다(개발자 개인의 core.autocrlf
     * 값에 의존하지 않음, {@code eol=crlf}만으로는 blob 바이트가 안 바뀜을 외부 실증함).
     */
    private static void testGitAttributesDeclaresNonTextPolicyForWindowsScripts() throws Exception {
        File root = repositoryRoot();
        File attrFile = new File(root, ".gitattributes");
        assertTrue("gitattributes-policy: .gitattributes exists at repository root", attrFile.isFile());
        if (!attrFile.isFile()) {
            return;
        }
        java.util.Map<String, java.util.Set<String>> patternToAttrs = parseGitAttributes(attrFile);
        assertTrue("gitattributes-policy: *.bat pattern declares -text",
                patternToAttrs.containsKey("*.bat") && patternToAttrs.get("*.bat").contains("-text"));
        assertTrue("gitattributes-policy: *.cmd pattern declares -text",
                patternToAttrs.containsKey("*.cmd") && patternToAttrs.get("*.cmd").contains("-text"));
    }

    /**
     * exact-JDK 게이트 경로(verify-standalone.bat, closed-network-import\BATCH-CONVERT.cmd)가
     * 실제로 그 non-text 정책이 적용되는 확장자에 해당하는지 개별적으로 다시 확인한다(패턴이
     * 좁혀지거나 파일이 재배치되어도 이 핵심 두 파일이 커버에서 빠지면 실패해야 한다).
     */
    private static void testAuthoritativeExactJdkGateScriptsAreCoveredByNonTextPolicy() throws Exception {
        File root = repositoryRoot();
        File attrFile = new File(root, ".gitattributes");
        assertTrue("gitattributes-coverage: .gitattributes exists", attrFile.isFile());
        if (!attrFile.isFile()) {
            return;
        }
        java.util.Map<String, java.util.Set<String>> patternToAttrs = parseGitAttributes(attrFile);
        String[] authoritativeScripts = {
                "verify-standalone.bat",
                "closed-network-import/BATCH-CONVERT.cmd"
        };
        for (String relativePath : authoritativeScripts) {
            assertTrue("gitattributes-coverage: " + relativePath + " exists",
                    new File(root, relativePath).isFile());
            String extensionPattern = relativePath.endsWith(".bat") ? "*.bat" : "*.cmd";
            java.util.Set<String> attrs = patternToAttrs.get(extensionPattern);
            assertTrue("gitattributes-coverage: " + relativePath + " covered by " + extensionPattern
                            + " -text", attrs != null && attrs.contains("-text"));
        }
    }

    /**
     * Slice 99F Correction 6 -- 7개 governed Windows script의 raw 바이트를 디스크에서 직접 읽어
     * CRLF만 있는지 검증한다({@code core.autocrlf}/git 명령을 전혀 사용하지 않음, {@code
     * .gitattributes} 존재만으로 통과시키지 않고 실제 파일 바이트 자체를 확인한다).
     */
    private static final String[] GOVERNED_WINDOWS_SCRIPTS = {
            "build.bat",
            "closed-network-import/BATCH-CONVERT.cmd",
            "closed-network-import/BUILD-AND-VERIFY.cmd",
            "convert-sample.bat",
            "tools/build-pipeline-trace.bat",
            "verify-offline.bat",
            "verify-standalone.bat"
    };

    private static void testGovernedWindowsScriptsHaveCrlfOnlyRawBytes() throws Exception {
        File root = repositoryRoot();
        for (String relativePath : GOVERNED_WINDOWS_SCRIPTS) {
            File f = new File(root, relativePath);
            assertTrue("crlf-invariant: " + relativePath + " exists", f.isFile());
            if (!f.isFile()) {
                continue;
            }
            byte[] raw = Files.readAllBytes(f.toPath());
            int crlf = 0, bareLf = 0, bareCr = 0;
            for (int i = 0; i < raw.length; i++) {
                if (raw[i] == '\r') {
                    if (i + 1 < raw.length && raw[i + 1] == '\n') {
                        crlf++;
                        i++;
                    } else {
                        bareCr++;
                    }
                } else if (raw[i] == '\n') {
                    bareLf++;
                }
            }
            assertTrue("crlf-invariant: " + relativePath + " has at least one CRLF pair", crlf > 0);
            assertTrue("crlf-invariant: " + relativePath + " has zero bare LF (found " + bareLf + ")",
                    bareLf == 0);
            assertTrue("crlf-invariant: " + relativePath + " has zero bare CR (found " + bareCr + ")",
                    bareCr == 0);
        }
    }

    /** governed 7-경로 목록 밖에 새 {@code *.bat}/{@code *.cmd}가 조용히 추가되지 않았는지 확인한다. */
    private static void testGovernedWindowsScriptInventoryHasNoUndeclaredAddition() throws Exception {
        File root = repositoryRoot();
        List<String> allScripts = new ArrayList<String>();
        collectScriptFiles(root, root, allScripts);
        java.util.Set<String> governed = new java.util.LinkedHashSet<String>(Arrays.asList(GOVERNED_WINDOWS_SCRIPTS));
        for (String relativePath : allScripts) {
            if (!(relativePath.endsWith(".bat") || relativePath.endsWith(".cmd"))) {
                continue;
            }
            assertTrue("crlf-invariant-inventory: " + relativePath + " is covered by the governed "
                            + "seven-script byte-policy check", governed.contains(relativePath));
        }
    }

    private static java.util.Map<String, java.util.Set<String>> parseGitAttributes(File attrFile) throws Exception {
        String content = new String(Files.readAllBytes(attrFile.toPath()), StandardCharsets.UTF_8);
        java.util.Map<String, java.util.Set<String>> result = new java.util.LinkedHashMap<String, java.util.Set<String>>();
        for (String rawLine : content.split("\r\n|\n", -1)) {
            String line = rawLine.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2) {
                continue;
            }
            java.util.Set<String> attrs = new java.util.LinkedHashSet<String>(
                    Arrays.asList(tokens).subList(1, tokens.length));
            result.put(tokens[0], attrs);
        }
        return result;
    }

    /**
     * V5_RUNTIME_REGRESSION_REQUIRED(Slice 99G) 재분류 근거: TabRuntimeScriptGenerator는 accepted
     * 경로 어디에서도 참조되지 않고 forbidden legacy WebSquareGenerator/XPlatformProjectConverter
     * 에서만 호출된다. batch 변환 authority가 여전히 TargetWebSquarePipeline임도 함께 확인한다.
     */
    private static void testAcceptedPathHasNoTabRuntimeScriptGeneratorReference() throws Exception {
        File root = repositoryRoot();
        List<String> acceptedPathFiles = Arrays.asList(
                "src/main/java/com/example/xfdltracker/pipeline/TargetWebSquarePipeline.java",
                "src/main/java/com/example/xfdltracker/renderer/AtomicWebSquareRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/CompositionRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/TargetDocumentAssembler.java",
                "src/main/java/com/example/xfdltracker/behavior/TargetScriptDocumentIntegrator.java",
                "src/main/java/com/example/xfdltracker/payload/TargetPayloadBehaviorFinalizer.java",
                "src/main/java/com/example/xfdltracker/batch/ClosedNetworkBatchCli.java",
                "src/main/java/com/example/xfdltracker/batch/BatchConversionRunner.java");
        String batchRunnerText = null;
        for (String relativePath : acceptedPathFiles) {
            String text = new String(
                    Files.readAllBytes(new File(root, relativePath).toPath()), StandardCharsets.UTF_8);
            assertTrue(relativePath + ": accepted path must not reference TabRuntimeScriptGenerator",
                    !text.contains("TabRuntimeScriptGenerator"));
            if (relativePath.endsWith("BatchConversionRunner.java")) {
                batchRunnerText = text;
            }
        }
        assertTrue("v5-gap: BatchConversionRunner.java must invoke TargetWebSquarePipeline "
                        + "(batch conversion authority)",
                batchRunnerText != null && batchRunnerText.contains("TargetWebSquarePipeline"));
    }

    /**
     * accepted TAB_CONTROL 산출물이 실제로 getScope/xplatform-tab-runtime.js를 발행하지 않음을
     * 실제 파이프라인 실행으로 증명한다(정적 텍스트 추정이 아니라 real output 검사).
     */
    private static void testAcceptedTabControlOutputContainsNoLegacyGetScopeOrRuntimeScript() throws Exception {
        File dir = Files.createTempDirectory("v5-gap-tabcontrol-fixture").toFile();
        File xfdl = new File(dir, "TabControl.xfdl");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"TabControlForm\" width=\"400\" height=\"300\">\n"
                + "    <Tab id=\"tab1\">\n"
                + "      <Tabpages>\n"
                + "        <Tabpage id=\"tp1\" text=\"tp1\" />\n"
                + "      </Tabpages>\n"
                + "    </Tab>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(xfdl.toPath(), content.getBytes(StandardCharsets.UTF_8));
        File output = new File(dir, "TabControl.xml");
        new TargetWebSquarePipeline().convert(
                xfdl, output, new TargetPipelineConfig(com.example.xfdltracker.runtime.TargetRuntimeProfile.empty()));
        String generated = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertTrue("v5-gap: accepted TAB_CONTROL output must contain w2:tabControl",
                generated.contains("w2:tabControl"));
        assertTrue("v5-gap: accepted TAB_CONTROL output must not contain getScope",
                !generated.contains("getScope"));
        assertTrue("v5-gap: accepted TAB_CONTROL output must not reference xplatform-tab-runtime.js",
                !generated.contains("xplatform-tab-runtime.js"));
    }

    /**
     * CLASS_MERGE_RUNTIME_REQUIRED(Slice 99H) 재분류 근거: legacy 병합 helper와
     * PropertyMappingRegistry 둘 다 accepted 경로 어디에서도 참조되지 않는다 --
     * PropertyMappingRegistry는 legacy Phase3ScreenAnalyzer에서만 소비된다.
     */
    private static void testAcceptedPathHasNoLegacyClassMergeHelperReference() throws Exception {
        File root = repositoryRoot();
        List<String> acceptedPathFiles = Arrays.asList(
                "src/main/java/com/example/xfdltracker/pipeline/TargetWebSquarePipeline.java",
                "src/main/java/com/example/xfdltracker/renderer/AtomicWebSquareRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/CompositionRenderer.java",
                "src/main/java/com/example/xfdltracker/renderer/TargetDocumentAssembler.java",
                "src/main/java/com/example/xfdltracker/behavior/TargetScriptDocumentIntegrator.java",
                "src/main/java/com/example/xfdltracker/payload/TargetPayloadBehaviorFinalizer.java",
                "src/main/java/com/example/xfdltracker/payload/TargetPayloadExtractor.java",
                "src/main/java/com/example/xfdltracker/batch/ClosedNetworkBatchCli.java",
                "src/main/java/com/example/xfdltracker/batch/BatchConversionRunner.java");
        String[] forbiddenClassMergeTokens = {
                "PropertyMappingRegistry",
                "resolveVideoEvidenceBaseClass",
                "appendClassTokenIfAbsent"
        };
        for (String relativePath : acceptedPathFiles) {
            String text = new String(
                    Files.readAllBytes(new File(root, relativePath).toPath()), StandardCharsets.UTF_8);
            for (String token : forbiddenClassMergeTokens) {
                assertTrue(relativePath + ": accepted path must not reference " + token,
                        !text.contains(token));
            }
        }
    }

    /**
     * accepted BUTTON/GRID 산출물의 class 동작이 감사된 계약과 일치함을 실제 파이프라인 실행으로
     * 증명한다: GRID는 고정 wq_gvw만 발행, BUTTON은 어떤 class도 발행하지 않으며 source
     * cssclass 값은 둘 다 산출물에 나타나지 않는다(병합 없음).
     */
    private static void testAcceptedButtonGridClassOutputMatchesAuditedContract() throws Exception {
        File buttonDir = Files.createTempDirectory("class-merge-gap-button-fixture").toFile();
        File buttonXfdl = new File(buttonDir, "ButtonCssClass.xfdl");
        String buttonContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"ButtonCssClass\" width=\"400\" height=\"300\">\n"
                + "    <Div id=\"btnGroup1\" width=\"400\">\n"
                + "      <Button id=\"btnSave\" left=\"0\" width=\"50\" height=\"20\" text=\"btnSave\" "
                + "cssclass=\"custom_source_class\" />\n"
                + "    </Div>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(buttonXfdl.toPath(), buttonContent.getBytes(StandardCharsets.UTF_8));
        File buttonOutput = new File(buttonDir, "ButtonCssClass.xml");
        new TargetWebSquarePipeline().convert(buttonXfdl, buttonOutput,
                new TargetPipelineConfig(com.example.xfdltracker.runtime.TargetRuntimeProfile.empty()));
        String buttonGenerated = new String(Files.readAllBytes(buttonOutput.toPath()), StandardCharsets.UTF_8);
        assertTrue("class-merge-gap: accepted BUTTON output must not contain btn_cm",
                !buttonGenerated.contains("btn_cm"));
        assertTrue("class-merge-gap: accepted BUTTON output must not contain source cssclass value",
                !buttonGenerated.contains("custom_source_class"));

        File gridDir = Files.createTempDirectory("class-merge-gap-grid-fixture").toFile();
        File gridXfdl = new File(gridDir, "GridCssClass.xfdl");
        String gridContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<FDL version=\"1.5\">\n"
                + "  <Form id=\"GridCssClass\" width=\"400\" height=\"300\">\n"
                + "    <Grid id=\"grdMinimal\" left=\"0\" top=\"0\" width=\"200\" height=\"120\" "
                + "cssclass=\"custom_source_class\">\n"
                + "      <Formats>\n"
                + "        <Format id=\"fmt1\">\n"
                + "          <Columns>\n"
                + "            <Column size=\"100\" />\n"
                + "          </Columns>\n"
                + "          <Band id=\"head\">\n"
                + "            <Cell col=\"0\" row=\"0\" />\n"
                + "          </Band>\n"
                + "          <Band id=\"body\">\n"
                + "            <Cell col=\"0\" row=\"0\" />\n"
                + "          </Band>\n"
                + "        </Format>\n"
                + "      </Formats>\n"
                + "    </Grid>\n"
                + "  </Form>\n"
                + "</FDL>\n";
        Files.write(gridXfdl.toPath(), gridContent.getBytes(StandardCharsets.UTF_8));
        File gridOutput = new File(gridDir, "GridCssClass.xml");
        new TargetWebSquarePipeline().convert(gridXfdl, gridOutput,
                new TargetPipelineConfig(com.example.xfdltracker.runtime.TargetRuntimeProfile.empty()));
        String gridGenerated = new String(Files.readAllBytes(gridOutput.toPath()), StandardCharsets.UTF_8);
        assertTrue("class-merge-gap: accepted GRID output must contain fixed base class wq_gvw",
                gridGenerated.contains("wq_gvw"));
        assertTrue("class-merge-gap: accepted GRID output must not contain source cssclass value",
                !gridGenerated.contains("custom_source_class"));
    }

    private static File repositoryRoot() {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (new File(dir, "build.bat").isFile()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("dependency-audit: could not locate sanctioned working-copy root");
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            failures++;
            System.out.println("FAILED: " + message);
        }
    }
}
