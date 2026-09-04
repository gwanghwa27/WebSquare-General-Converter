#!/bin/sh
# 폐쇄망 batch 변환 entrypoint(Slice 99F). JDK 1.8.0 family 게이트/compile/regression 로직을
# 여기서 재구현하지 않고 verify-standalone.bat에 전부 위임한다(이 프로젝트의 정규 entrypoint는
# Windows batch다 -- 이 .sh는 cmd.exe를 통해 그 batch를 그대로 호출하는 best-effort 브리지다).

# XFDL 파싱/target XML 생성/legacy 변환기 호출은 Java orchestrator
# (com.example.xfdltracker.batch.ClosedNetworkBatchCli)의 책임이다.

# 사용법: sh closed-network-import/BATCH-CONVERT.sh <inputRoot> <outputRoot> <runtimeProfileFile>
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [ "$#" -ne 3 ]; then
  echo "usage: sh closed-network-import/BATCH-CONVERT.sh <inputRoot> <outputRoot> <runtimeProfileFile>"
  exit 2
fi

echo "== Closed-network batch conversion (delegates JDK 1.8.0 family gate to verify-standalone.bat) =="
if ! command -v cmd.exe >/dev/null 2>&1; then
  echo "[BATCH_FAIL] cmd.exe not available in this shell -- verify-standalone.bat (the authoritative"
  echo "JDK 1.8.0 family gate) cannot be invoked from here. This entrypoint does not duplicate its"
  echo "JDK/version logic, so it refuses to proceed rather than weaken or bypass that gate (batch_jdk_family_gate_unreachable)."
  exit 1
fi
if ! command -v cygpath >/dev/null 2>&1; then
  echo "[BATCH_FAIL] cygpath not available -- cannot reliably hand this repo path to cmd.exe without"
  echo "risking a mis-invocation that silently skips verify-standalone.bat (batch_jdk_family_gate_unreachable)."
  exit 1
fi
STANDALONE_LOG="$(mktemp 2>/dev/null || echo "$REPO_ROOT/build/batch-convert-standalone.log")"
LAUNCHER_BAT="$(mktemp -u 2>/dev/null || echo "$REPO_ROOT/build/batch-convert-launcher").bat"
# 경로에 실제 백슬래시가 들어가면 printf 포맷 문자열의 이스케이프 처리와 충돌해 문자가 깨지는
# 사례를 실측으로 확인했으므로 heredoc만 쓴다. "cd /d" 뒤 상대경로로 call하면 이 bridge에서
# 파일을 못 찾는 사례도 실측했으므로, 절대경로(forward-slash)를 call에 직접 넘긴다.
WIN_VERIFY_SCRIPT_FWD="$(cygpath -m "$REPO_ROOT")/verify-standalone.bat"
cat > "$LAUNCHER_BAT" <<EOF
@echo off
call "$WIN_VERIFY_SCRIPT_FWD"
exit /b %errorlevel%
EOF
# MSYS/Git-Bash가 "/c"를 cmd.exe 옵션이 아니라 POSIX 경로로 오인해 자동 변환하면 cmd.exe가
# 명령을 전혀 실행하지 못하고 조용히 빈 세션만 열었다가 종료하는 사례를 실측으로 확인했다 --
# MSYS_NO_PATHCONV로 그 자동 변환을 끈다(실제 실행 여부는 아래 PASS marker로 다시 확인).
MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*" cmd.exe /c "$(cygpath -w "$LAUNCHER_BAT")" > "$STANDALONE_LOG" 2>&1
STANDALONE_EXIT=$?
rm -f "$LAUNCHER_BAT"
cat "$STANDALONE_LOG"
# exit code만으로는 신뢰하지 않는다 -- 이 브리지가 target batch를 실제로 실행하지 못하고도
# 조용히 0을 반환한 사례가 있었으므로, verify-standalone.bat 자신이 찍는 확정적 PASS marker가
# 출력에 실제로 있는지까지 함께 확인해야 JDK 1.8.0 family 게이트를 통과한 것으로 인정한다.
if [ "$STANDALONE_EXIT" -ne 0 ] || ! grep -q "\[STANDALONE_VERIFICATION_PASS\]" "$STANDALONE_LOG"; then
  echo "[BATCH_FAIL] verify-standalone.bat did not pass -- JDK 1.8.0 family gate/compile/regression must pass"
  echo "before batch conversion begins (batch_jdk_family_gate_not_passed). See its output above."
  rm -f "$STANDALONE_LOG"
  exit 1
fi
rm -f "$STANDALONE_LOG"

echo
echo "-- Batch conversion --"
java -cp "$REPO_ROOT/build/classes" com.example.xfdltracker.batch.ClosedNetworkBatchCli "$1" "$2" "$3"
exit $?
