#!/bin/sh
# 폐쇄망 manifest/build 확인 후 legacy 변환 이전에 fail-closed 종료한다. 사용법:
# sh closed-network-import/BUILD-AND-VERIFY.sh (실행 예시)
# legacy 변환/CSS 정책 검증 단계는 모두 제거됨 -- 표준 검증 authority는 verify-standalone.bat이다.
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

FAIL=0

echo "== One-shot closed-network manifest/build check (legacy conversion disabled) =="
echo "Repo root: $REPO_ROOT"
echo

echo "-- [1/2] MANIFEST.sha256 integrity --"
if command -v sha256sum >/dev/null 2>&1; then
  if (cd "$REPO_ROOT" && sha256sum -c "closed-network-import/MANIFEST.sha256" > closed-network-import/manifest-check.log 2>&1); then
    echo "[PASS] MANIFEST.sha256 all files match."
  else
    echo "[FAIL] MANIFEST mismatch or missing files -- see closed-network-import/manifest-check.log"
    FAIL=1
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] sha256sum not found."
fi
echo

echo "-- [2/2] Clean compile --"
rm -rf build
mkdir -p build/classes
find src/main/java -name '*.java' > build/srclist.txt
if javac -encoding UTF-8 -d build/classes @build/srclist.txt; then
  echo "[PASS] clean compile."
else
  echo "[FAIL] javac compile failed."
  FAIL=1
fi
rm -f build/srclist.txt
echo

echo "[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER] Legacy sample-project conversion (and"
echo "the legacy CSS-class/state policy checks that depended on its output) is disabled in this"
echo "script -- it only checks manifest integrity and clean compile, it is not a conversion"
echo "entrypoint. TargetWebSquarePipeline is the accepted conversion architecture. Legacy conversion"
echo "fallback (XPlatformProjectConverter / WebSquareGenerator) is forbidden. For actual batch"
echo "conversion, use closed-network-import/BATCH-CONVERT.cmd (or .sh) <inputRoot> <outputRoot>"
echo "<runtimeProfileFile> (Slice 99F) -- it requires an explicit caller-supplied TargetRuntimeProfile"
echo "file (see closed-network-import/example-runtime-profile.txt) and delegates its JDK 1.8.0 family gate to"
echo "verify-standalone.bat. For standalone acceptance verification, use verify-standalone.bat (or"
echo "verify-offline.bat/verify-offline.sh, which delegate to it)."
FAIL=1

if [ "$FAIL" = "0" ]; then
  echo "== RESULT: ALL GATES PASS =="
  exit 0
else
  echo "== RESULT: ONE OR MORE GATES FAILED (legacy conversion path disabled) =="
  exit 1
fi
