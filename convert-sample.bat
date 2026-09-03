@echo off
setlocal
chcp 65001 >nul

rem legacy XPlatformProjectConverter 호출을 제거하고 비활성화된 entrypoint로 전환됨.
rem 범용 배치 CLI를 위한 Reviewer-approved 기본 TargetRuntimeProfile이 없어 default를
rem 발명하지 않는다 -- TargetWebSquarePipeline.convert()를 직접 호출해야 한다.

echo == XPlatform to WebSquare Converter - Sample Conversion ==
echo.
echo [CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] This script is disabled.
echo TargetWebSquarePipeline is the accepted conversion architecture. A caller-supplied
echo TargetRuntimeProfile is required for every conversion. No approved generic/default
echo TargetRuntimeProfile currently exists for a batch CLI covering an arbitrary multi-file
echo project, so no default can be invented here. Legacy conversion fallback
echo ^(XPlatformProjectConverter / WebSquareGenerator^) is forbidden.
echo.
echo Use com.example.xfdltracker.pipeline.TargetWebSquarePipeline.convert(File, File,
echo TargetPipelineConfig) directly, supplying your own TargetRuntimeProfile.
exit /b 1
