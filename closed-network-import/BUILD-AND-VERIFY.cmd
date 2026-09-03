@echo off
setlocal EnableDelayedExpansion
REM 폐쇄망 manifest/build 확인. legacy 변환은 비활성화됨.
REM 사용법: closed-network-import\BUILD-AND-VERIFY.cmd

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.."
set "REPO_ROOT=%CD%"

set "FAIL=0"

echo == One-shot closed-network manifest/build check (legacy conversion disabled) ==
echo Repo root: %REPO_ROOT%
echo.

set "PS_AVAILABLE=1"
powershell -NoProfile -Command "$null" >nul 2>nul
if errorlevel 1 set "PS_AVAILABLE=0"
echo POWERSHELL_AVAILABLE=%PS_AVAILABLE%
echo.

echo -- [1/2] MANIFEST.sha256 integrity --
if "%PS_AVAILABLE%"=="0" goto skip_manifest
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%verify-manifest.ps1" -RepoRoot "%REPO_ROOT%" -ManifestPath "%SCRIPT_DIR%MANIFEST.sha256"
if errorlevel 1 (
  echo [FAIL] MANIFEST mismatch or missing files.
  set "FAIL=1"
) else (
  echo [PASS] MANIFEST.sha256 all files match.
)
goto after_manifest
:skip_manifest
echo [SKIPPED_OPTIONAL_TOOL] PowerShell not available for SHA-256 check.
:after_manifest
echo.

echo -- [2/2] Clean compile --
if exist "build" rmdir /s /q "build"
mkdir "build\classes"
set "SRCLIST=build\srclist.txt"
dir /s /b "src\main\java\*.java" > "%SRCLIST%"
javac -encoding UTF-8 -d "build\classes" @"%SRCLIST%"
if errorlevel 1 (
  echo [FAIL] javac compile failed.
  set "FAIL=1"
) else (
  echo [PASS] clean compile.
)
if exist "%SRCLIST%" del /q "%SRCLIST%"
echo.

echo [CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] Legacy sample-project conversion (and
echo the legacy CSS-class/state and XML-output checks that depended on its output) is disabled in
echo this script. TargetWebSquarePipeline is the accepted conversion architecture. A caller-supplied
echo TargetRuntimeProfile is required for every conversion; no approved generic/default profile
echo currently exists for a batch CLI covering an arbitrary multi-file project, so no default can
echo be invented here. Legacy conversion fallback (XPlatformProjectConverter / WebSquareGenerator)
echo is forbidden. Use TargetWebSquarePipeline.convert(File, File, TargetPipelineConfig) directly,
echo supplying your own TargetRuntimeProfile. For standalone acceptance verification, use
echo verify-standalone.bat (or verify-offline.bat/verify-offline.sh, which delegate to it).
set "FAIL=1"

if "%FAIL%"=="0" (
  echo == RESULT: ALL GATES PASS ==
  popd
  exit /b 0
) else (
  echo == RESULT: ONE OR MORE GATES FAILED (legacy conversion path disabled) ==
  popd
  exit /b 1
)
