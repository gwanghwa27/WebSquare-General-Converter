# XPlatform → WebSquare Converter
## 폐쇄망 반입용 Source Project

상세 설치/실행/검증 방법:

- `docs/OFFLINE-USER-GUIDE.md`
- `docs/OFFLINE-USER-GUIDE.docx`
- `docs/OFFLINE-USER-GUIDE.pdf`

**현재 accepted standalone 변환 경로**: `com.example.xfdltracker.pipeline.TargetWebSquarePipeline`
(raw XFDL → WebSquare XML), 호출자가 자신의 `TargetRuntimeProfile`을 직접 구성해 넘겨야 한다. 검증
authority는 `verify-standalone.bat`(`verify-offline.bat`/`verify-offline.sh`는 여기로 위임하는
thin wrapper일 뿐, 별도 검증 로직이 없다). 아래 항목 5의 `convert-sample.*`는 **non-operational legacy
entrypoint로 disabled**되어 있다 -- 실행하면 즉시 `[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]`
메시지와 함께 종료하며 어떤 변환도 수행하지 않는다. 자세한 내용은 `docs/OFFLINE-USER-GUIDE.md` 항목 2-1/항목 9를
참고.

## 1. 프로젝트 목적

Phase4-derived working candidate(누적 Production 수정 8건 포함)를 인터넷 없는 폐쇄망 PC로 반입해,
Maven/Gradle/외부 JAR 없이 JDK 1.8.0_111만으로 compile/convert/verify할 수 있게 만든 독립 소스
프로젝트다. Phase4 original ZIP baseline은 IMMUTABLE/FROZEN이며 이 프로젝트는 그 위에 파생된
working candidate를 COPY한 것이다.

## 2. 요구 환경

- JDK **1.8.0_111**
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
JDK 1.8.0_111

Language Level:
8

### Eclipse

Installed JRE:
JDK 1.8.0_111

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
CLOSED_CONTRACT_LIMITATION (Slice 99E -- 위 역사적 검증은 legacy WebSquareGenerator 출력 기준이며, 현재
accepted v6 CheckBox 출력(xf:select appearance="full")과 렌더링/runtime 동등하다는 증거가 repository
안에 없고 real WebSquare runtime 환경도 이 프로젝트에 없어 실행 검증도 불가능하므로, unbound CheckBox도
dataset-bound와 동일하게 렌더러 도달 전에 명시적으로 fail-closed한다)

Auto page init:
CLOSED_CONTRACT_LIMITATION (Slice 99D -- accepted path never generates ev:onpageload, 0 consumers)

Phase1 SHA:
PASS

## 8. 남은 known gaps

Defect 2(CONTENT_NOT_READY)는 Slice 99A에서, GRID-3(다중 Format)는 Slice 99B correction에서, CheckBox
dataset-bound는 Slice 99C에서, `ev:onpageload` 자동 page-init은 Slice 99D에서, CheckBox unbound의
accepted-v6 rendering-equivalence는 Slice 99E에서 각각 CLOSED_CONTRACT_LIMITATION으로 종결
(`docs/OFFLINE-USER-GUIDE.md` 항목 13 참고 -- CheckBox는 dataset-bound(BindItem 존재)든 unbound든 모두
렌더러 도달 전에 명시적으로 fail-closed되어 대상 XML을 전혀 발행하지 않는다; `ev:onpageload`는 accepted
path가 애초에 생성하지 않아 의존하는 산출물이 0건). 이 5건은 모두 종결됐으나, 이것이 전체 남은
product/runtime known gap이 0건이라는 의미는 아니다 -- `V5_RUNTIME_REGRESSION_REQUIRED`/
`CLASS_MERGE_RUNTIME_REQUIRED`(`OFFLINE-IMPORT-MANIFEST.md` 참고)는 이 Slice에서 새로 판단하지
않았으며, 그 문서에 이미 기록된 기존 standing(미검증 상태) 그대로 남아 있다.

별도 certification blocker 1건: Target JDK 1.8.0_111 확보/검증(BLOCKED_BY_DISTRIBUTION — 폐쇄망에서
별도 확보 필요, `verify-offline.*`의 1단계 게이트로 확인).

상세는 `docs/FINAL-VERIFICATION-REPORT.md`, `docs/followup-checkBox-ready-jdk-phase1-final.md` 참고.

## 9. WebSquare Runtime 관련 주의사항

- 실제 폐쇄망 WebSquare 서버/Studio/dev pack은 이 프로젝트에 포함되지 않음
- generated XML 배포 시 해당 환경의 WebSquare wpack 절차가 필요할 수 있음

## 10. immutable Phase4 baseline과 현재 working candidate의 관계

Phase4 original ZIP baseline은 절대 수정하지 않는다. 이 반입 프로젝트의 소스는 그 baseline에서
파생된 Phase4-derived working candidate(`work/phase4-working/...`, 누적 Production 수정 8건 포함)를
그대로 COPY한 것이며, baseline 자체를 대체하거나 덮어쓰지 않는다.
