@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem 외부 의존성 없는 오프라인 빌드: javac만 사용(Maven/Gradle/외부 JAR 없음).
rem src\main\java를 build\classes\로 컴파일하며, 소스 트리 자체에는 .class를 생성하지 않는다.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "SRC_ROOT=%PROJECT_ROOT%src\main\java"
set "BUILD_DIR=%PROJECT_ROOT%build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "SOURCES_LIST=%BUILD_DIR%\sources.txt"

echo == XPlatform to WebSquare Converter - Offline Build ==
echo Project root: %PROJECT_ROOT%

where java >nul 2>nul
if errorlevel 1 (
  echo [FAIL] java not found on PATH. A JDK 1.8.0 family installation's bin\ must be on PATH.
  exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
  echo [FAIL] javac not found on PATH. A JDK 1.8.0 family installation's bin\ must be on PATH.
  exit /b 1
)

for /f "usebackq delims=" %%v in (`java -version 2^>^&1`) do (
  if not defined JAVA_VER_LINE set "JAVA_VER_LINE=%%v"
)
for /f "usebackq delims=" %%v in (`javac -version 2^>^&1`) do (
  if not defined JAVAC_VER_LINE set "JAVAC_VER_LINE=%%v"
)
echo java -version:  %JAVA_VER_LINE%
echo javac -version: %JAVAC_VER_LINE%

rem 아래 family 판정은 verify-standalone.bat Step 1(mandatory gate)과 동일한 anchored 규칙을
rem 그대로 재사용한다(substring/contains 매치 아님). 이 스크립트는 non-authoritative build
rem helper이므로 mismatch여도 compile은 계속 진행한다 -- 실패 처리는 하지 않는다.
set "TARGET_JDK_FAMILY=1.8.0"
set "JAVA_TOKEN=!JAVA_VER_LINE:*"=!"
set "JAVA_TOKEN=!JAVA_TOKEN:"=!"
set "JAVAC_TOKEN="
for /f "tokens=2" %%a in ("%JAVAC_VER_LINE%") do if not defined JAVAC_TOKEN set "JAVAC_TOKEN=%%a"
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

set "BOTH_FAMILY_MATCH=0"
if "%JAVA_FAMILY_MATCH%"=="1" if "%JAVAC_FAMILY_MATCH%"=="1" set "BOTH_FAMILY_MATCH=1"
if "%BOTH_FAMILY_MATCH%"=="1" (
  echo [TARGET_JDK_MATCH] Detected JDK %TARGET_JDK_FAMILY% family ^(java=%JAVA_TOKEN%, javac=%JAVAC_TOKEN%^).
) else (
  echo [TARGET_JDK_MISMATCH_WARNING] Detected JDK is not in the %TARGET_JDK_FAMILY% family for both java and javac ^(java=%JAVA_TOKEN%, javac=%JAVAC_TOKEN%^). Compile will still be attempted; this warning does NOT mean the build failed, and this build script does NOT certify TARGET_JDK_RUNTIME_VERIFIED. Use verify-offline.bat for the mandatory JDK family gate.
)

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"

set "COUNT=0"
for /f "delims=" %%f in ('dir /s /b "%SRC_ROOT%\*.java"') do (
  set "line=%%f"
  set "line=!line:\=/!"
  echo !line!>>"%SOURCES_LIST%"
  set /a COUNT+=1
)
echo Java source files: %COUNT%

javac -encoding UTF-8 -d "%CLASSES_DIR%" "@%SOURCES_LIST%"
if errorlevel 1 (
  echo [BUILD_FAIL] javac reported errors.
  exit /b 1
)

echo [BUILD_OK] Compiled %COUNT% source files into %CLASSES_DIR% (0 errors).
exit /b 0
