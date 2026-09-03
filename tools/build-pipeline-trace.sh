#!/bin/sh
# legacy XPlatformProjectConverter/Git 추적 도구, disabled.
# TargetWebSquarePipeline이 accepted 경로다.
set -u

echo "[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] This script is disabled."
echo "TargetWebSquarePipeline is the accepted conversion architecture. A caller-supplied"
echo "TargetRuntimeProfile is required for every conversion; no approved generic pipeline-trace/"
echo "batch profile exists, so no default can be invented here. Legacy XPlatformProjectConverter"
echo "fallback is forbidden."
exit 1
