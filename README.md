# WebSquare General Converter

raw XFDL을 입력으로 받아 WebSquare XML을 생성하는 독립(standalone) converter다. 폐쇄망 반입/오프라인
실행에 대한 상세 설치·빌드·실행·검증 절차는 `README-OFFLINE.md`와 `docs/OFFLINE-USER-GUIDE.md`를
참고한다.

## 1. 프로젝트 개요

이 프로젝트는 XPlatform XFDL 화면을 WebSquare XML로 변환하는 Java 8 source project다.
Maven/Gradle/외부 JAR 의존 없이, JDK 1.8.0_111만으로 compile/convert/verify할 수 있다. 배포용
컴파일 산출물(binary)이 아니라 폐쇄망에서 직접 컴파일하고 실행할 수 있는 source project로
구성되어 있다.

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

- exact certified target: **JDK 1.8.0_111**.
- `java.exe`와 `javac.exe`가 **same-home**(동일 설치 경로)에서 모두 정확히 1.8.0_111이어야 한다.
- process-local(비영속) `JAVA_HOME`/`PATH` selection이 accepted certification 방법이다.
- 시스템 전역 PATH나 레지스트리를 영구적으로 변경할 필요는 없다.

## 9. Build / Verify

현재 저장소에 실제로 존재하는 스크립트만 사용한다.

```
build.bat            (Windows)
./build.sh            (Linux/Unix)
verify-standalone.bat (Windows, 검증 authority)
verify-offline.bat / ./verify-offline.sh  (verify-standalone.bat에 위임하는 thin wrapper)
```

`verify-standalone.bat`의 exact-JDK gate를 우회하는 방법은 존재하지 않으며, 이 문서는 그런 우회
방법을 제안하지 않는다 — java/javac가 정확히 1.8.0_111이 아니면 검증은 `[TARGET_JDK_MISMATCH_FAIL]`로
fail-closed된다(정상 동작).

## 10. Closed-Network Conversion

현재 operational entrypoint는 다음이다.

```
closed-network-import\BATCH-CONVERT.cmd inputRoot outputRoot runtimeProfileFile
```

인자 순서는 `inputRoot` → `outputRoot` → `runtimeProfileFile`이다. runtime profile은 호출자가
명시적으로 제공해야 하며, 다음은 명시적으로 금지된다.

- implicit default runtime profile
- capability auto-enable
- source로부터의 capability inference

## 11. Batch Safety

`closed-network-import\BATCH-CONVERT.cmd`(및 그 하위 batch 구성요소)는 다음 안전 계약을 지킨다.

- recursive exact lowercase `.xfdl` discovery(정확히 `.xfdl` 확장자만 real path 기준 재귀 탐색)
- deterministic relative ordering
- symlink/junction/path alias는 fail-closed(조용히 건너뛰지 않음)
- 이미 존재하는 target(preexisting target)에는 실패 처리 — silent overwrite 금지
- 입력이 0건이면 fail(공허한 배치를 성공으로 보고하지 않음)
- 여러 입력 중 첫 conversion 실패 시 즉시 중단(stop)
- 그 이전에 이미 끝난 입력의 성공 결과(previous success)는 그대로 보존
- 실패한 파일에 대한 partial XML은 발행하지 않는다

**주의**: 이 Slice(100C) 시점에는 저장소 root에 별도의 thin delegation wrapper(예:
`closed-network-batch-convert.bat`)가 **아직 존재하지 않는다**. 위 `BATCH-CONVERT.cmd`가 현재
유일한 실제 operational closed-network entrypoint이며, 이 문서는 아직 존재하지 않는 root-level
wrapper를 이미 사용 가능한 것처럼 기술하지 않는다.
