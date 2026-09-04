@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem standalone production acceptance verifier 권위(SELECTED_STANDALONE_VERIFICATION_ENTRY).
rem 이 저장소의 source/test/build 산출물만 사용하며 외부 프로젝트 경로를 참조하지 않는다.
rem verify-offline.bat는 이 스크립트에 위임하는 wrapper일 뿐, 별도 authority가 아니다.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "SRC_ROOT=%PROJECT_ROOT%src\main\java"
set "TEST_SRC_ROOT=%PROJECT_ROOT%src\test\java"
set "BUILD_DIR=%PROJECT_ROOT%build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "TEST_CLASSES_DIR=%BUILD_DIR%\test-classes"

echo == Standalone Production Acceptance Verification (Slice 98BG) ==
echo Project root: %PROJECT_ROOT%

rem ---- Step 1: JDK/버전 게이트 ------------------------------------------------------------
where java >nul 2>nul
if errorlevel 1 (
  echo [FAIL] java not found on PATH.
  exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
  echo [FAIL] javac not found on PATH.
  exit /b 1
)
rem 버전 토큰의 JDK 1.8.0 family 여부를 anchored 방식으로 판정한다(substring/contains 매치
rem 금지 -- "11.8.0"/"1.8.01"/"1.8.1"이 family로 오인되면 안 됨, exact update suffix pinning은
rem 더 이상 요구하지 않는다). java/javac 둘 다 family match여야 한다(양쪽 독립 판정, AND 결합).
set "TARGET_JDK_FAMILY=1.8.0"

for /f "usebackq delims=" %%v in (`java -version 2^>^&1`) do (
  if not defined JAVA_VER_LINE set "JAVA_VER_LINE=%%v"
)
echo java -version: %JAVA_VER_LINE%
rem java -version 출력의 따옴표 안 토큰만 정확히 추출한다(따옴표 중복 분리 오류를 피하기 위해
rem 첫 따옴표까지 잘라내고 끝 따옴표를 제거하는 방식 -- findstr 부분일치 사용 안 함).
set "JAVA_TOKEN=!JAVA_VER_LINE:*"=!"
set "JAVA_TOKEN=!JAVA_TOKEN:"=!"
echo java runtime version token: %JAVA_TOKEN%

for /f "usebackq delims=" %%v in (`javac -version 2^>^&1`) do (
  if not defined JAVAC_VER_LINE set "JAVAC_VER_LINE=%%v"
)
echo javac -version: %JAVAC_VER_LINE%
rem javac -version 출력(따옴표 없음)에서 공백으로 구분된 두번째 토큰만 정확히 추출한다.
set "JAVAC_TOKEN="
for /f "tokens=2" %%a in ("%JAVAC_VER_LINE%") do if not defined JAVAC_TOKEN set "JAVAC_TOKEN=%%a"
echo javac compiler version token: %JAVAC_TOKEN%

rem family match 조건: "1.8.0" 자체이거나 "1.8.0_" 뒤에 숫자 1개 이상만 허용한다(0-9를 모두
rem 제거해 빈 값이면 순수 숫자였다는 뜻, "U" 접두로 빈 문자열/변수소실 문제를 피한다). 개별
rem 자리 제거로 처리해 for-loop 변수를 substitution search key로 쓸 때의 파싱 결함을 피한다.
if not defined JAVA_TOKEN set "JAVA_TOKEN=UNPARSED_VERSION_TOKEN"
if not defined JAVAC_TOKEN set "JAVAC_TOKEN=UNPARSED_VERSION_TOKEN"

set "JAVA_FAMILY_MATCH=0"
if "%JAVA_TOKEN%"=="%TARGET_JDK_FAMILY%" set "JAVA_FAMILY_MATCH=1"
set "JAVA_PREFIX_OK=0"
if "%JAVA_TOKEN:~0,6%"=="%TARGET_JDK_FAMILY%_" set "JAVA_PREFIX_OK=1"
set "JAVA_UPDATE=U"
if "%JAVA_PREFIX_OK%"=="1" set "JAVA_UPDATE=U%JAVA_TOKEN:~6%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:0=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:1=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:2=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:3=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:4=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:5=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:6=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:7=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:8=%"
set "JAVA_UPDATE_STRIPPED=%JAVA_UPDATE_STRIPPED:9=%"
if "%JAVA_PREFIX_OK%"=="1" if not "%JAVA_UPDATE%"=="U" if "%JAVA_UPDATE_STRIPPED%"=="U" set "JAVA_FAMILY_MATCH=1"

rem javac token도 java와 완전히 동일한 규칙으로 독립 판정한다(서로 다른 update suffix라도
rem 각자 family이기만 하면 된다 -- JAVA_AND_JAVAC_SAME_UPDATE_SUFFIX_REQUIRED = FALSE).
set "JAVAC_FAMILY_MATCH=0"
if "%JAVAC_TOKEN%"=="%TARGET_JDK_FAMILY%" set "JAVAC_FAMILY_MATCH=1"
set "JAVAC_PREFIX_OK=0"
if "%JAVAC_TOKEN:~0,6%"=="%TARGET_JDK_FAMILY%_" set "JAVAC_PREFIX_OK=1"
set "JAVAC_UPDATE=U"
if "%JAVAC_PREFIX_OK%"=="1" set "JAVAC_UPDATE=U%JAVAC_TOKEN:~6%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:0=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:1=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:2=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:3=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:4=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:5=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:6=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:7=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:8=%"
set "JAVAC_UPDATE_STRIPPED=%JAVAC_UPDATE_STRIPPED:9=%"
if "%JAVAC_PREFIX_OK%"=="1" if not "%JAVAC_UPDATE%"=="U" if "%JAVAC_UPDATE_STRIPPED%"=="U" set "JAVAC_FAMILY_MATCH=1"

rem batch의 "if A if B (...) else (...)" 연쇄는 else가 두번째 if에만 붙어 첫 조건 거짓 시
rem 두 분기 모두 건너뛰는 결함이 있다(실제 java/javac 버전 불일치 오탐 사례로 발견됨).
rem BOTH_FAMILY_MATCH로 조건을 먼저 하나로 합친 뒤 단일 if/else로 판정해 이 결함을 피한다.
set "BOTH_FAMILY_MATCH=0"
if "%JAVA_FAMILY_MATCH%"=="1" if "%JAVAC_FAMILY_MATCH%"=="1" set "BOTH_FAMILY_MATCH=1"
if "%BOTH_FAMILY_MATCH%"=="1" (
  echo [TARGET_JDK_MATCH] Detected JDK %TARGET_JDK_FAMILY% family ^(java=%JAVA_TOKEN%, javac=%JAVAC_TOKEN%, both match, exact update suffix not required^).
) else (
  rem VERIFY_STANDALONE_TARGET_JDK_MISMATCH_RESULT_REQUIRED = FAIL -- 불일치는 경고로 끝나지
  rem 않는다. 대체 JDK/PATH 재작성/다운로드/네트워크 접근 없이 compile/test 전에 fail-closed한다.
  echo [TARGET_JDK_MISMATCH_FAIL] Detected JDK does not match the accepted %TARGET_JDK_FAMILY% family for BOTH java and javac -- java_token=%JAVA_TOKEN% java_family_match=%JAVA_FAMILY_MATCH% javac_token=%JAVAC_TOKEN% javac_family_match=%JAVAC_FAMILY_MATCH%
  echo [STANDALONE_VERIFICATION_FAIL] JDK %TARGET_JDK_FAMILY% family required for BOTH java and javac, mismatch is fail-closed.
  exit /b 1
)

rem ---- Step 2: 로컬 build 출력 정리 ----------------------------------------------------
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"
mkdir "%TEST_CLASSES_DIR%"

rem ---- Step 3: production source 컴파일(project-local만) -----------------------------
set "SOURCES_LIST=%BUILD_DIR%\standalone-sources.txt"
for /f "delims=" %%f in ('dir /s /b "%SRC_ROOT%\*.java"') do (
  set "line=%%f"
  set "line=!line:\=/!"
  echo !line!>>"%SOURCES_LIST%"
)
javac -encoding UTF-8 -d "%CLASSES_DIR%" "@%SOURCES_LIST%"
if errorlevel 1 (
  echo [STANDALONE_VERIFICATION_FAIL] production compile failed.
  exit /b 1
)
echo [STANDALONE_PRODUCTION_COMPILE_OK]

rem ---- Step 4: project-local test 컴파일 -------------------------------------------------
set "TEST_SOURCES_LIST=%BUILD_DIR%\standalone-test-sources.txt"
for /f "delims=" %%f in ('dir /s /b "%TEST_SRC_ROOT%\*.java"') do (
  set "line=%%f"
  set "line=!line:\=/!"
  echo !line!>>"%TEST_SOURCES_LIST%"
)
javac -encoding UTF-8 -cp "%CLASSES_DIR%" -d "%TEST_CLASSES_DIR%" "@%TEST_SOURCES_LIST%"
if errorlevel 1 (
  echo [STANDALONE_VERIFICATION_FAIL] test compile failed.
  exit /b 1
)
echo [STANDALONE_TEST_COMPILE_OK]

rem ---- Step 5: 직접 영향받는 standalone/runtime/assembler test -----------------------
set "DIRECT_CLASSES=com.example.xfdltracker.pipeline.TargetPipelineConfigTest com.example.xfdltracker.runtime.RuntimeFoundationTest com.example.xfdltracker.runtime.RuntimeFunctionCallAnalyzerTest com.example.xfdltracker.analyzer.ComponentPredicateAnalyzerTest com.example.xfdltracker.renderer.TargetDocumentAssemblerTest com.example.xfdltracker.renderer.TargetXmlSerializerTest com.example.xfdltracker.pipeline.StandaloneDependencyIsolationTest com.example.xfdltracker.pipeline.TargetWebSquarePipelineTest"
set "DIRECT_FAIL=0"
for %%c in (%DIRECT_CLASSES%) do (
  echo -- running %%c --
  java -cp "%CLASSES_DIR%;%TEST_CLASSES_DIR%" %%c
  if errorlevel 1 set "DIRECT_FAIL=1"
)
if "%DIRECT_FAIL%"=="1" (
  echo [STANDALONE_VERIFICATION_FAIL] one or more directly-affected standalone tests failed.
  exit /b 1
)
echo [STANDALONE_DIRECT_TESTS_PASS]

rem ---- Step 6: 전체 project-local test class 실행(외부 historical 자원 불필요) --
set "ALL_FAIL=0"
for /f "delims=" %%f in ('dir /s /b "%TEST_CLASSES_DIR%\*Test.class"') do (
  set "cn=%%f"
  set "cn=!cn:%TEST_CLASSES_DIR%\=!"
  set "cn=!cn:\=.!"
  set "cn=!cn:.class=!"
  echo !cn! | findstr /c:"$" >nul
  if errorlevel 1 (
    java -cp "%CLASSES_DIR%;%TEST_CLASSES_DIR%" !cn! >nul 2>nul
    if errorlevel 1 (
      echo [FAILED_TEST_CLASS] !cn!
      set "ALL_FAIL=1"
    )
  )
)
if "%ALL_FAIL%"=="1" (
  echo [STANDALONE_VERIFICATION_FAIL] one or more project-local test classes failed.
  exit /b 1
)
echo [STANDALONE_ALL_PROJECT_LOCAL_TESTS_PASS]

rem Step 7-9: 변환 fixture/구조 검증/실패시 부분발행금지는 step 5의
rem TargetWebSquarePipelineTest/TargetXmlSerializerTest가 이미 검증했다.
echo [STANDALONE_CONVERSION_FIXTURE_VERIFIED_VIA_STEP_5]

rem ---- Step 10: 정적 dependency audit ----------------------------------------------------
rem com.example.xfdltracker.pipeline.StandaloneDependencyIsolationTest가 강제하며, step 5에서
rem 이미 실행/통과했다.
echo [STANDALONE_DEPENDENCY_AUDIT_PASS]

echo [STANDALONE_VERIFICATION_PASS]
exit /b 0
