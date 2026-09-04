@echo off
setlocal EnableDelayedExpansion
REM 폐쇄망 batch 변환 entrypoint(Slice 99F). JDK 1.8.0 family 게이트/compile/regression 로직을
REM 여기서 재구현하지 않고 verify-standalone.bat에 전부 위임한다(중복/우회 없음, 그 게이트를
REM 통과하지 못하면 변환을 시작하지 않는다).

REM XFDL 파싱/target XML 생성/legacy 변환기 호출은 이 스크립트가 아니라 Java orchestrator
REM (com.example.xfdltracker.batch.ClosedNetworkBatchCli)의 책임이다.

REM 사용법: closed-network-import\BATCH-CONVERT.cmd inputRoot outputRoot runtimeProfileFile
REM   inputRoot/outputRoot -- 재귀 탐색할 *.xfdl 폴더 / 결과 XML을 쓸 폴더(상대경로 보존)
REM   runtimeProfileFile   -- 정규 capability ID 목록(줄마다 하나, closed-network-import\

REM   example-runtime-profile.txt 문법 참고)

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.."
set "REPO_ROOT=%CD%"

if "%~3"=="" goto usage
goto args_ok

:usage
echo usage: closed-network-import\BATCH-CONVERT.cmd inputRoot outputRoot runtimeProfileFile
popd
exit /b 2

:args_ok
echo == Closed-network batch conversion (delegates JDK 1.8.0 family gate to verify-standalone.bat) ==
call "%REPO_ROOT%\verify-standalone.bat"
if errorlevel 1 goto jdk_gate_failed
goto run_batch

:jdk_gate_failed
echo [BATCH_FAIL] verify-standalone.bat did not pass -- JDK 1.8.0 family gate/compile/regression must pass
echo before batch conversion begins (batch_jdk_family_gate_not_passed). See its output above.
popd
exit /b 1

:run_batch
echo.
echo -- Batch conversion --
java -cp "%REPO_ROOT%\build\classes" com.example.xfdltracker.batch.ClosedNetworkBatchCli "%~1" "%~2" "%~3"
set "BATCH_EXIT=%errorlevel%"
popd
exit /b %BATCH_EXIT%
