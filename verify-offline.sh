#!/bin/sh
# STANDALONE_VERIFIER_AUTHORITY = verify-standalone.bat. 이 스크립트는 독자 검증 로직 없이
# cmd.exe로 verify-standalone.bat을 위임 호출하는 thin delegator다(JDK 1.8.0 family 게이트 재구현 없음).
# cmd.exe가 없으면 검증 로직을 흉내내지 않고 fail-closed로 종료한다.
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "== XPlatform to WebSquare Converter - Offline Verification (delegates to verify-standalone.bat) =="
echo "Script dir: $SCRIPT_DIR"

if ! command -v cmd.exe >/dev/null 2>&1; then
  echo "[VERIFIER_DELEGATION_UNAVAILABLE] cmd.exe (the Windows command bridge needed to invoke"
  echo "verify-standalone.bat from this shell) was not found on PATH in this environment."
  echo "[STANDALONE_VERIFICATION_FAIL] the authoritative verifier (verify-standalone.bat) could not"
  echo "be invoked -- this script does not reproduce its verification logic independently."
  exit 1
fi

cd "$SCRIPT_DIR" || {
  echo "[STANDALONE_VERIFICATION_FAIL] could not change into script directory: $SCRIPT_DIR"
  exit 1
}

cmd.exe //c ".\\verify-standalone.bat"
STANDALONE_EXIT=$?

echo
if [ "$STANDALONE_EXIT" = "0" ]; then
  echo "[CORE_VERIFICATION_PASS]"
  exit 0
else
  echo "[CORE_VERIFICATION_FAIL] verify-standalone.bat did not pass -- see its output above (this includes the JDK 1.8.0 family gate, which is never bypassed or reimplemented here)."
  exit 1
fi
