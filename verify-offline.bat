@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem verify-offline.bat는 verify-standalone.bat에 위임하는 thin wrapper다(단일 verifier 권위,
rem 독자 검증 로직 없음). legacy 변환/외부 저장소 의존 단계는 모두 제거되었다 -- exact-JDK
rem 게이트를 포함한 verify-standalone.bat 출력을 그대로 노출한다(우회/은폐 없음).

set "SCRIPT_DIR=%~dp0"

echo == XPlatform to WebSquare Converter - Offline Verification (delegates to verify-standalone.bat) ==
echo.

call "%SCRIPT_DIR%verify-standalone.bat"
set "STANDALONE_EXIT=%errorlevel%"

echo.
if "%STANDALONE_EXIT%"=="0" (
  echo [CORE_VERIFICATION_PASS]
  exit /b 0
) else (
  echo [CORE_VERIFICATION_FAIL] verify-standalone.bat did not pass -- see its output above ^(this includes the exact-JDK gate, which is never bypassed here^).
  exit /b 1
)
