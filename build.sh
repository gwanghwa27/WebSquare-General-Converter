#!/bin/sh
# 외부 의존성 없는 오프라인 빌드: javac만 사용(Maven/Gradle/외부 JAR 없음).
# src/main/java를 build/classes/로 컴파일하며, 소스 트리 자체에는 .class를 생성하지 않는다.
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
SRC_ROOT="$PROJECT_ROOT/src/main/java"
BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
SOURCES_LIST="$BUILD_DIR/sources.txt"

echo "== XPlatform to WebSquare Converter - Offline Build =="
echo "Project root: $PROJECT_ROOT"

if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  echo "[FAIL] java/javac not found on PATH. A JDK 1.8.0 family installation's bin/ must be on PATH."
  exit 1
fi

JAVA_VER="$(java -version 2>&1 | head -n 1)"
JAVAC_VER="$(javac -version 2>&1 | head -n 1)"
echo "java -version:  $JAVA_VER"
echo "javac -version: $JAVAC_VER"

JAVA_TOKEN="$(printf '%s\n' "$JAVA_VER" | sed -n 's/.*"\([^"]*\)".*/\1/p')"
JAVAC_TOKEN="$(printf '%s\n' "$JAVAC_VER" | awk '{print $2}')"

# verify-standalone.bat Step 1(mandatory gate)과 동일한 anchored family 규칙이다(substring
# 매치 아님, case 패턴은 전체 문자열 기준). non-authoritative build helper이므로 mismatch여도
# compile은 계속 진행한다(실패 처리 없음, mandatory gate는 verify-offline.sh가 위임하는 쪽).
is_jdk_1_8_0_family() {
  case "$1" in
    1.8.0) return 0 ;;
    1.8.0_*)
      u="${1#1.8.0_}"
      case "$u" in
        ''|*[!0-9]*) return 1 ;;
        *) return 0 ;;
      esac
      ;;
    *) return 1 ;;
  esac
}

if is_jdk_1_8_0_family "$JAVA_TOKEN" && is_jdk_1_8_0_family "$JAVAC_TOKEN"; then
  echo "[TARGET_JDK_MATCH] Detected JDK 1.8.0 family (java=$JAVA_TOKEN, javac=$JAVAC_TOKEN)."
else
  echo "[TARGET_JDK_MISMATCH_WARNING] Detected JDK is not in the 1.8.0 family for both java and javac (java=$JAVA_TOKEN, javac=$JAVAC_TOKEN). Compile will still be attempted; this warning does NOT mean the build failed, and this build script does NOT certify TARGET_JDK_RUNTIME_VERIFIED. Use verify-offline.sh for the mandatory JDK family gate."
fi

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

# argfile에 forward slash를 쓰면 JDK9+의 "@sources.txt + Windows backslash" 파싱 결함을
# 피할 수 있다(javac는 Windows에서도 forward slash 경로를 허용한다).
find "$SRC_ROOT" -name '*.java' | sed 's#\\#/#g' > "$SOURCES_LIST"
COUNT=$(wc -l < "$SOURCES_LIST" | tr -d ' ')
echo "Java source files: $COUNT"

javac -encoding UTF-8 -d "$CLASSES_DIR" "@$SOURCES_LIST"
echo "[BUILD_OK] Compiled $COUNT source files into $CLASSES_DIR (0 errors)."
