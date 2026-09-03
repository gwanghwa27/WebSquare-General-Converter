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
rem 버전 토큰을 정확히 추출/비교한다(substring 포함 매치 금지 -- "1.8.0_1111"이 "1.8.0_111"로
rem 오인되면 안 됨). java/javac 둘 다 정확히 일치해야 한다(VERIFY_STANDALONE_JAVA_AND_JAVAC_BOTH_REQUIRED = TRUE).
set "TARGET_JDK_TOKEN=1.8.0_111"

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

set "JAVA_EXACT=0"
set "JAVAC_EXACT=0"
if "%JAVA_TOKEN%"=="%TARGET_JDK_TOKEN%" set "JAVA_EXACT=1"
if "%JAVAC_TOKEN%"=="%TARGET_JDK_TOKEN%" set "JAVAC_EXACT=1"
rem batch의 "if A if B (...) else (...)" 연쇄는 else가 두번째 if에만 붙어 첫 조건 거짓 시
rem 두 분기 모두 건너뛰는 결함이 있다(실제 java/javac 버전 불일치 오탐 사례로 발견됨).
rem BOTH_EXACT로 조건을 먼저 하나로 합친 뒤 단일 if/else로 판정해 이 결함을 피한다.
set "BOTH_EXACT=0"
if "%JAVA_EXACT%"=="1" if "%JAVAC_EXACT%"=="1" set "BOTH_EXACT=1"
if "%BOTH_EXACT%"=="1" (
  echo [TARGET_JDK_MATCH] Detected exact JDK %TARGET_JDK_TOKEN% ^(java and javac both match^).
) else (
  rem VERIFY_STANDALONE_TARGET_JDK_MISMATCH_RESULT_REQUIRED = FAIL -- 불일치는 경고로 끝나지
  rem 않는다. 대체 JDK/PATH 재작성/다운로드/네트워크 접근 없이 compile/test 전에 fail-closed한다.
  echo [TARGET_JDK_MISMATCH_FAIL] Detected JDK does not exactly match the accepted target %TARGET_JDK_TOKEN% -- java_token=%JAVA_TOKEN% java_exact=%JAVA_EXACT% javac_token=%JAVAC_TOKEN% javac_exact=%JAVAC_EXACT%
  echo [STANDALONE_VERIFICATION_FAIL] exact target JDK %TARGET_JDK_TOKEN% required for BOTH java and javac, mismatch is fail-closed.
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
