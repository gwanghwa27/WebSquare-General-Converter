@echo off
setlocal DisableDelayedExpansion

REM legacy XPlatformProjectConverter 추적 도구, disabled.
REM TargetWebSquarePipeline이 accepted 경로다.

echo [CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] This script is disabled.
echo TargetWebSquarePipeline is the accepted conversion architecture. A caller-supplied
echo TargetRuntimeProfile is required for every conversion; no approved generic pipeline-trace/
echo batch profile exists, so no default can be invented here. Legacy XPlatformProjectConverter
echo fallback is forbidden.
exit /b 1
