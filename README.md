# WebSquare General Converter

raw XFDL을 입력으로 받아 WebSquare XML을 생성하는 독립(standalone) converter다. 폐쇄망 반입/오프라인
실행에 대한 상세 설치·빌드·실행·검증 절차는 `README-OFFLINE.md`와 `docs/OFFLINE-USER-GUIDE.md`를
참고한다.

## 1. 프로젝트 개요

이 프로젝트는 XPlatform XFDL 화면을 WebSquare XML로 변환하는 Java 8 source project다.
Maven/Gradle/외부 JAR 의존 없이, JDK 1.8.0 family(예: 1.8.0_111/1.8.0_503 등, exact update
버전 고정 없음)만으로 compile/convert/verify할 수 있다. 배포용 컴파일 산출물(binary)이 아니라
폐쇄망에서 직접 컴파일하고 실행할 수 있는 source project로 구성되어 있다.

## 2. Accepted Pipeline

현재 accepted 표준 변환 경로는 다음 단계로 구성된다.

```
raw XFDL
→ Component Predicate / Layout analysis
→ Semantic Region Segmenter
→ Semantic Intermediate Model
→ Composition Evaluation
→ TargetCompositionPlan
→ TargetPayload
→ AtomicWebSquareRenderer
→ CompositionRenderer
→ TargetDocumentAssembler
→ TargetXmlSerializer
→ WebSquare XML
```

## 3. Public Standalone API

프로그램적(Java API) 단일 진입점은 다음과 같다.

```java
com.example.xfdltracker.pipeline.TargetWebSquarePipeline.convert(
    File sourceXfdl,
    File targetWebSquareXml,
    TargetPipelineConfig config
)
```

`TargetPipelineConfig`의 `runtimeProfile`은 **required/non-null**이다 — 호출자가 자신의 실제 대상
환경에 맞는 `TargetRuntimeProfile`을 직접 구성해서 넘겨야 하며, 이 프로젝트는 임의의 "기본"
runtime profile을 발명하지 않는다.

## 4. Runtime Architecture

- **requirements-only**: runtime capability 요구사항을 검증만 하며, 어떤 runtime support
  리소스도 발행(emit)하지 않는다.
- **fail-closed**: 사용 불가능한 runtime capability 요구사항은 fail-closed한다(예외 발생, 부분
  발행 없음).
- converter는 runtime function 자체를 실행하지 않는다 — 오직 요구사항을 정적으로 검증할 뿐이다.
- `CommonRuntimeCapabilityCatalog`가 canonical capability authority다.
- unknown capability 또는 unknown semantics를 임의로 승인하지 않는다 — 카탈로그에 없는 ID는
  변환을 시작하기 전에 명시적으로 fail-closed한다.

## 5. Renderer Architecture

- 렌더러는 `TargetCompositionPlan`과 validated `TargetPayload`만 소비한다.
- source DOM/raw XFDL 재분석이나 semantic invention을 렌더러 단계에서 수행하지 않는다.
- exact logical target identity correlation을 유지한다 — source 식별자와 target 식별자 사이의
  대응 관계가 추측이 아닌 명시적 규칙으로 결정된다.

## 6. Legacy Independence

현재 accepted 경로(`TargetWebSquarePipeline`과 그 하위 구성요소)는 legacy converter/generator/
runtime fallback(`XPlatformProjectConverter`, `WebSquareGenerator` 등)에 의존하지 않는다 — 이
경계는 architecture boundary이며, 이 문서는 과거 프로젝트의 역사나 repository provenance를 현재
제품 소개의 중심 내용으로 다루지 않는다. 과거 이력 자체는 `OFFLINE-IMPORT-MANIFEST.md`(historical
evidence)와 `FROZEN-DO-NOT-MODIFY.md`(historical policy record)에 별도로 보존되어 있다.

## 7. Closed Contract Limitations

다음 항목은 `CLOSED_CONTRACT_LIMITATION`이며, 기술 준비 상태(READY)가 이 항목들을 지원 기능으로
바꾸지 않는다 — 각 항목은 accepted 경로가 렌더러 도달 전에 명시적으로 fail-closed하여 대상 XML을
전혀 발행하지 않는 항목이다.

- **Defect 2**(`CONTENT_NOT_READY` false-negative)
- **GRID-3**(다중 Format(default/alternate) 모호성)
- **CheckBox dataset-bound**
- **CheckBox unbound**
- **automatic page-init**(`ev:onpageload`)

추가로 `V5 getScope`(`TabRuntimeScriptGenerator`의 `component('grp_content').getScope()`)와
`class merge`(legacy `cssclass` 병합) concern은 accepted 경로에 도달하지 않는
**legacy-only historical concern**으로 닫혀 있다 — 이는 실제 legacy runtime 검증이 완료됐다는
뜻이 아니며, accepted 경로가 해당 코드 자체를 호출하지 않는다는 구조적 사실에 근거한 종결이다.
상세 근거는 `README-OFFLINE.md` 항목 8을 참고한다.

## 8. JDK

- required version family: **JDK 1.8.0**(예: `1.8.0_111`, `1.8.0_503` 등 — exact update 번호
  고정은 요구하지 않는다). `java`/`javac` 각각의 버전 token이 독립적으로 family에 속해야 하며,
  둘의 update 번호가 서로 달라도(예: java=`1.8.0_111`, javac=`1.8.0_503`) 무방하다.
- Java 7/9/11/17/21 등 1.8.0 family가 아닌 버전은 fail-closed된다.
- `verify-standalone.bat`가 실제로 검사하는 것은 현재 process `PATH`에서 resolve된 `java`/
  `javac`의 버전 출력 문자열뿐이다 — 두 실행파일이 같은 설치 디렉터리(filesystem same-home)에서
  왔는지를 검사하는 별도 로직은 없다. process-local(비영속) `JAVA_HOME`/`PATH` selection은
  운영자가 두 실행파일을 원하는 설치본으로 일치시키는 방법일 뿐, verifier가 `JAVA_HOME` 값
  자체를 읽거나 강제하지는 않는다.
- 시스템 전역 PATH나 레지스트리를 영구적으로 변경할 필요는 없다.
- **historical**: Slice 99I에서 same-home `C:\Program Files\Java\jdk1.8.0_111`(java/javac 모두
  exact 1.8.0_111)로 인증한 과거 사실은 `HISTORICAL_EXACT_JDK_1_8_0_111_CERTIFICATION = PASS`로
  보존되어 있다(`README-OFFLINE.md` 항목 7 참고) — 이는 과거 인증 기록이며 현재 gate가 요구하는
  최소 조건이 아니다.

## 9. Build / Verify

현재 저장소에 실제로 존재하는 스크립트만 사용한다.

```
build.bat            (Windows)
./build.sh            (Linux/Unix)
verify-standalone.bat (Windows, 검증 authority)
verify-offline.bat / ./verify-offline.sh  (verify-standalone.bat에 위임하는 thin wrapper)
```

`verify-standalone.bat`의 JDK 1.8.0 family gate를 우회하는 방법은 존재하지 않으며, 이 문서는 그런
우회 방법을 제안하지 않는다 — java/javac 중 하나라도 1.8.0 family가 아니면 검증은
`[TARGET_JDK_MISMATCH_FAIL]`로 fail-closed된다(정상 동작).

## 10. Closed-Network Conversion

현재 operational entrypoint는 두 가지이며, 역할이 명확히 나뉜다.

- **root convenience entrypoint**: `closed-network-batch-convert.bat` — 저장소 root에서 바로
  실행하는 thin delegation wrapper다. caller argument vector(`%*`)를 그대로 아래 delegated
  구현에 전달하고, 그 exit code를 그대로 반환할 뿐이다. JDK 게이트/변환/runtime profile 해석
  로직을 스스로 구현하지 않는다 — 새로운 converter나 독립 인증 authority가 아니다.
- **delegated authoritative batch implementation**: `closed-network-import\BATCH-CONVERT.cmd` —
  JDK 1.8.0 family gate 위임(`verify-standalone.bat` 경유)과 실제 batch 변환 로직을 보유한
  authoritative 구현이다. root wrapper를 거치든 직접 실행하든 동일한 이 구현이 실행된다.

```
closed-network-batch-convert.bat inputRoot outputRoot runtimeProfileFile
```

인자 순서는 `inputRoot` → `outputRoot` → `runtimeProfileFile`이다. `runtimeProfileFile`은 여전히
required이며, root wrapper는 어떤 implicit default도 도입하지 않는다. runtime profile은 호출자가
명시적으로 제공해야 하며, 다음은 명시적으로 금지된다.

- implicit default runtime profile
- capability auto-enable
- source로부터의 capability inference

## 11. Batch Safety

`closed-network-import\BATCH-CONVERT.cmd`(및 그 하위 batch 구성요소)는 다음 안전 계약을 지킨다.
root convenience entrypoint(`closed-network-batch-convert.bat`)는 이 계약을 스스로 재구현하지
않고 delegated 구현으로 그대로 전달할 뿐이다.

- recursive exact lowercase `.xfdl` discovery(정확히 `.xfdl` 확장자만 real path 기준 재귀 탐색)
- deterministic relative ordering
- symlink/junction/path alias는 fail-closed(조용히 건너뛰지 않음)
- 이미 존재하는 target(preexisting target)에는 실패 처리 — silent overwrite 금지
- 입력이 0건이면 fail(공허한 배치를 성공으로 보고하지 않음)
- 여러 입력 중 첫 conversion 실패 시 즉시 중단(stop)
- 그 이전에 이미 끝난 입력의 성공 결과(previous success)는 그대로 보존
- 실패한 파일에 대한 partial XML은 발행하지 않는다
