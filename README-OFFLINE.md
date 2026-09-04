# WebSquare General Converter
## 폐쇄망 반입/오프라인 실행용 Source Project

상세 설치/실행/검증 방법:

- `docs/OFFLINE-USER-GUIDE.md`
- `docs/OFFLINE-USER-GUIDE.docx`
- `docs/OFFLINE-USER-GUIDE.pdf`

**현재 accepted standalone 변환 경로**: `com.example.xfdltracker.pipeline.TargetWebSquarePipeline`
(raw XFDL → WebSquare XML), 호출자가 자신의 `TargetRuntimeProfile`을 직접 구성해 넘겨야 한다. 검증
authority는 `verify-standalone.bat`(`verify-offline.bat`/`verify-offline.sh`는 여기로 위임하는
thin wrapper일 뿐, 별도 검증 로직이 없다). 아래 항목 5의 `convert-sample.*`는 **non-operational legacy
entrypoint로 disabled**되어 있다 -- 실행하면 즉시 `[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]`
메시지와 함께 종료하며 어떤 변환도 수행하지 않는다. 여러 XFDL 파일을 한 번에 변환하려면(Slice 99F)
저장소 root의 `closed-network-batch-convert.bat`(Slice 100D, thin delegation wrapper) 또는 이를
통해 위임되는 authoritative 구현 `closed-network-import\BATCH-CONVERT.cmd`(또는 `.sh`)를 사용한다
-- 두 경로 모두 호출자가 명시적으로 제공한 `inputRoot`/`outputRoot`/runtime profile 파일 세 인자를
요구하며, 암묵적 기본 profile은 여전히 존재하지 않는다(자세한 내용: `docs/OFFLINE-USER-GUIDE.md`
항목 2-1/항목 9, `closed-network-import\example-runtime-profile.txt`).

## 1. 프로젝트 목적

WebSquare General Converter는 raw XFDL을 입력으로 받아 WebSquare XML을 생성하는 독립(standalone)
converter다. 이 문서는 그 소스 전체를 인터넷 없는 폐쇄망 PC로 반입해, Maven/Gradle/외부 JAR 없이
JDK 1.8.0 family(예: 1.8.0_111/1.8.0_503 등, exact update 버전 고정 없음)만으로 compile/convert/
verify할 수 있도록 안내한다. 전체 아키텍처 개요와 accepted pipeline 설명은 루트 `README.md`를
참고한다.

## 2. 요구 환경

- JDK **1.8.0 family**(exact update 버전 고정 없음 -- 예: `1.8.0_111`, `1.8.0_503`)
- UTF-8
- Maven/Gradle **불필요**
- 외부 JAR **불필요**
- 인터넷 연결 **불필요**

## 3. IDE 선택

이 프로젝트는 특정 IDE에 종속되지 않는다.

지원:

- IntelliJ IDEA
- Eclipse
- Command Line

### IntelliJ

Project SDK:
설치된 JDK 1.8.0 family(예: JDK 1.8.0_111)

Language Level:
8

### Eclipse

Installed JRE:
설치된 JDK 1.8.0 family(예: JDK 1.8.0_111)

Execution Environment:
JavaSE-1.8

Compiler compliance:
1.8

둘 중 하나를 선택해서 사용할 수 있으며 제공된 `build.bat`/`build.sh`가 최종 기준 빌드 방법이다.

## 4. Command line build

```
build.bat        (Windows)
./build.sh        (Linux/Unix)
```

## 5. 변환 -- non-operational legacy entrypoint (비운영)

`convert-sample.bat`/`convert-sample.sh`는 disabled된 non-operational legacy entrypoint다 --
실행하면 `[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]` 메시지와 함께 즉시 종료 코드
1로 종료하며 어떤 변환도 수행하지 않는다. 실제 변환은
`com.example.xfdltracker.pipeline.TargetWebSquarePipeline.convert(File, File,
TargetPipelineConfig)`를 자신의 `TargetRuntimeProfile`과 함께 프로그램적으로 직접 호출해야 한다
(`docs/OFFLINE-USER-GUIDE.md` 항목 2-1 참고).

## 6. Offline verification

```
verify-offline.bat        (Windows)
./verify-offline.sh        (Linux/Unix)
```

## 7. 현재 검증 상태

controls:
148 scanned / 146 PASS / 0 MISMATCH / 2 UNSUPPORTED

Dataset:
13/13

Grid:
GRID-1 REAL_RUNTIME_VERIFIED
GRID-2 REAL_RUNTIME_VERIFIED
GRID-3 CLOSED_CONTRACT_LIMITATION (Slice 99B correction)

CheckBox unbound (역사적 widget/bootstrap evidence, legacy 출력 기준):
REAL_RUNTIME_VERIFIED
(WIDGET/BOOTSTRAP SEMANTICS)

CheckBox unbound accepted-v6 rendering-equivalence:
CLOSED_CONTRACT_LIMITATION (Slice 99E -- 위 역사적 검증은 legacy WebSquareGenerator 출력 기준이며,
Slice 99E에서 감사한 pre-99E accepted-v6 attempted structural representation(xf:select
appearance="full")과 렌더링/runtime 동등하다는 증거가 repository 안에 없고 real WebSquare runtime
환경도 이 프로젝트에 없어 실행 검증도 불가능했으므로, unbound CheckBox도 dataset-bound와 동일하게
렌더러 도달 전에 명시적으로 fail-closed한다. 현재 accepted path는 이 xf:select 구조를 더 이상 발행하지
않는다)

Auto page init:
CLOSED_CONTRACT_LIMITATION (Slice 99D -- accepted path never generates ev:onpageload, 0 consumers)

Closed-network batch entrypoint:
IMPLEMENTED_AND_TARGET_JDK_CERTIFIED (Slice 99F 구현 + Slice 99I 인증 -- closed-network-import\
BATCH-CONVERT.cmd/.sh, explicit caller-supplied TargetRuntimeProfile file required, no implicit
default; delegates the JDK 1.8.0 family gate to verify-standalone.bat rather than duplicating it -- 아래
Target JDK 인증 근거와 동일하게 정규 entrypoint smoke와 raw committed HEAD smoke 둘 다 PASS로
확인됨). Slice 100D에서 저장소 root convenience entrypoint `closed-network-batch-convert.bat`가
추가됐다 -- 이 wrapper는 위 `BATCH-CONVERT.cmd`로만 그대로 위임하는 thin delegation일 뿐 별도의
converter나 독립 인증 authority가 아니다.

Phase1 SHA:
PASS

## 8. 남은 known gaps

Defect 2(CONTENT_NOT_READY)는 Slice 99A에서, GRID-3(다중 Format)는 Slice 99B correction에서, CheckBox
dataset-bound는 Slice 99C에서, `ev:onpageload` 자동 page-init은 Slice 99D에서, CheckBox unbound의
accepted-v6 rendering-equivalence는 Slice 99E에서 각각 CLOSED_CONTRACT_LIMITATION으로 종결
(`docs/OFFLINE-USER-GUIDE.md` 항목 13 참고 -- CheckBox는 dataset-bound(BindItem 존재)든 unbound든 모두
렌더러 도달 전에 명시적으로 fail-closed되어 대상 XML을 전혀 발행하지 않는다; `ev:onpageload`는 accepted
path가 애초에 생성하지 않아 의존하는 산출물이 0건).

`V5_RUNTIME_REGRESSION_REQUIRED`(`TabRuntimeScriptGenerator`의 `component('grp_content').getScope()`가
`xf:group` root에서 실제 v5 런타임에 동작하는지 미검증)는 Slice 99G에서
CLOSED_NOT_APPLICABLE_TO_ACCEPTED_PATH로 종결됐다 -- 그 역사적 getScope 동작 자체는 여전히
real-runtime 미검증 상태로 남아 있으나(`V5_RUNTIME_REGRESSION_REAL_RUNTIME_VERIFIED = FALSE`,
이번 Slice가 새로 검증한 것이 아님), `TabRuntimeScriptGenerator`는 현재 accepted 경로
(`TargetWebSquarePipeline`)의 어떤 클래스에서도 직접/간접 호출되지 않고 forbidden legacy
`WebSquareGenerator`/`XPlatformProjectConverter`에서만 호출되며, accepted 경로가 생성하는
TAB_CONTROL 대상 XML(`w2:tabControl`/`w2:tabs`/`w2:content`)에는 `getScope`/
`xplatform-tab-runtime.js` 참조가 0건이다. 따라서 이 gap은 현재 accepted-path의 product/runtime
gap이 아니라 legacy 전용 미검증 사안으로 재분류됐다(`OFFLINE-IMPORT-MANIFEST.md` 항목 6 참고).

`CLASS_MERGE_RUNTIME_REQUIRED`(`TabExternalContent` 등 legacy `WebSquareGenerator`의
`copyAttributeIfPresent`+`resolveVideoEvidenceBaseClass`+`appendClassTokenIfAbsent`로 source
`cssclass`를 `btn_cm`/`wq_gvw` base class와 병합하는 동작이 실제 v5 런타임에서 동작하는지
미검증)는 Slice 99H에서 `CLOSED_NOT_APPLICABLE_TO_ACCEPTED_PATH`로 종결됐다 -- 그 legacy 병합
동작 자체는 여전히 real-runtime 미검증 상태로 남아 있으나(`CLASS_MERGE_LEGACY_RUNTIME_VERIFIED
= FALSE`, 이번 Slice가 새로 검증한 것이 아님), 세 관심사를 분리해 확인한 결과 (1) legacy 병합
로직과 `PropertyMappingRegistry`의 `cssclass -> class` 매핑 둘 다 accepted 경로에서 호출되지
않고, (2) accepted `AtomicWebSquareRenderer`는 GRID에 고정 base class `wq_gvw`만 독립적으로
발행하며(BUTTON은 `btn_cm`을 전혀 발행하지 않음, source cssclass와 무관), (3) accepted 경로는
source `cssclass` 속성 자체를 어느 단계에서도 읽거나 발행하지 않는다(합성 BUTTON/GRID fixture로
실증). 현재 문서 어디에도 accepted 경로의 source cssclass 병합 지원을 주장하지 않으므로 이는
현재 accepted-path의 product/runtime gap이 아니다(`OFFLINE-IMPORT-MANIFEST.md` 항목 7 참고).

Target JDK 1.8.0_111 인증(Slice 99I): `TARGET_JDK_1_8_0_111_VERIFICATION = PASS`,
`TARGET_JDK_EXACT_RUNTIME_AND_COMPILER_CERTIFIED = TRUE`. same-home 짝 확보:
`C:\Program Files\Java\jdk1.8.0_111`(java=1.8.0_111, javac=1.8.0_111) -- 별도 다운로드/설치 없이
로컬에 이미 존재하던 설치본이며, process-local(비영속) 환경으로만 활성화해 확인했다(시스템
JAVA_HOME/PATH는 이 인증 과정에서 영구 변경되지 않음). 근거: 작업 트리 `verify-standalone.bat` PASS,
raw Git blob으로만 구체화한 committed HEAD의 `verify-standalone.bat` PASS, 정규
`BATCH-CONVERT.cmd` smoke PASS, raw committed `BATCH-CONVERT.cmd` smoke PASS(4건 전부).
**주의**: 기본 shell에서 `java` 명령 자체는 여전히 다른 PATH 항목(java8path redirector,
1.8.0_503)으로 resolve될 수 있다 -- 이는 시스템 전역 기본 java가 1.8.0_111이라는 뜻이 아니라,
exact target JDK 짝이 로컬에 인증 가능한 상태로 존재하고 필요 시 process-local로 정확히 선택
가능하다는 뜻이다. `CLOSED_NETWORK_IMPORT_TECHNICAL_READINESS = READY` -- 이는 accepted
아키텍처가 정확한 인증 target JDK 아래서 빌드/검증/실행 가능함을 뜻할 뿐, 위에 기록된 fail-closed
계약 한계(Defect 2/GRID-3/CheckBox/auto page-init)를 무효화하지 않으며, GitHub Push/공개 배포가
완료됐다는 뜻도 아니다(원격 게시는 여전히 사용자 전용 작업이다).

이 인증에 이르기까지의 과거 조사 기록(historical evidence)은 `docs/FINAL-VERIFICATION-REPORT.md`,
`docs/followup-checkBox-ready-jdk-phase1-final.md`에 남아 있다 -- 이 기록 자체는 현재 verification
authority가 아니다. 현재 architecture와 accepted behavior는 Reviewer가 승인한 architecture
standing과 accepted current source/contract를 기준으로 판단하며, 이 문서(항목 7 포함)는 그 승인된
standing을 사용자에게 보고/설명하는 문서일 뿐 이 문서 자체가 independent architecture authority는
아니다.

**현재(Slice 100E-I) gate standing**(위 Slice 99I 단락과 구분되는 별도 standing -- 과거 인증
사실을 현재 mandatory 요구사항으로 재해석하지 않는다):
`HISTORICAL_EXACT_JDK_1_8_0_111_CERTIFICATION = PASS`(위 Slice 99I 인증 그대로 보존),
`CURRENT_REQUIRED_JDK_VERSION_FAMILY = 1.8.0`, `CURRENT_EXACT_UPDATE_PINNING_REQUIRED = FALSE`.
현재 `verify-standalone.bat`는 java/javac 버전 token이 각각 `1.8.0` family(예: `1.8.0_111`,
`1.8.0_503`)에 속하기만 하면 PASS로 인정하며, 두 실행파일이 같은 설치 경로에서 왔는지 확인하는
filesystem 검사는 하지 않는다(`README.md` 항목 8 참고).

## 9. WebSquare Runtime 관련 주의사항

- 실제 폐쇄망 WebSquare 서버/Studio/dev pack은 이 프로젝트에 포함되지 않음
- generated XML 배포 시 해당 환경의 WebSquare wpack 절차가 필요할 수 있음

## 10. 과거 freeze/baseline 기록과의 관계 (참고용 pointer)

이 저장소 자체는 현재 WebSquare General Converter의 standalone source이며, 이 프로젝트 소개의
중심은 위 항목들이 설명하는 현재 accepted 아키텍처와 사용법이다. 이 저장소가 파생되어 나온 과거
freeze/baseline 이력(예: Phase4 계열 baseline과 그로부터 파생됐던 이전 candidate 상태)은 이
문서의 현재 authority가 아니며, 그 historical fact 자체는 `OFFLINE-IMPORT-MANIFEST.md`(해당
freeze 시점의 historical evidence)와 `FROZEN-DO-NOT-MODIFY.md`(과거 frozen snapshot의 historical
policy record)에 그대로 보존되어 있다.
