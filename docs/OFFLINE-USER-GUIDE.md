# XPlatform → WebSquare Converter
## 폐쇄망 반입용 Source Project 설치·빌드·실행·검증 가이드

프로젝트명: `xplatform-to-websquare-offline-import`
대상 환경: 인터넷이 없는 폐쇄망 PC (Windows / Linux)

---

## 1. 문서 목적

이 문서는 `xplatform-to-websquare-offline-import` 프로젝트를 폐쇄망 PC로 반입한 뒤, 별도의
인터넷 연결이나 Maven/Gradle 없이 소스를 열고, 빌드하고, sample project를 변환하고, 포함된
offline verifier로 결과를 검증하는 전체 절차를 설명한다.

## 2. 프로젝트 개요

이 프로젝트는 XPlatform XFDL 화면을 WebSquare 5.0.5 XML/JS로 변환하는 Java 8 converter의
**소스 프로젝트**다. 배포용 컴파일 산출물(binary)이 아니라, 폐쇄망에서 직접 컴파일하고 실행할 수
있는 **source project**로 구성되어 있다.

## 2-1. 표준 변환 경로: TargetWebSquarePipeline (Slice 98BH)

**이 절이 현재 표준 조작 경로다.** 항목 9(`convert-sample.*`)와 항목 10의 legacy 8단계 표는 이 프로젝트의
더 이전 Phase4 legacy converter 경로(`com.example.xfdltracker.project.XPlatformProjectConverter`)를
기술한 **역사적 자료**이며, 현재 accepted standalone 변환/검증 authority가 **아니다**. `convert-sample.*`는
현재 **non-operational legacy entrypoint로 disabled**되어 있다 -- 실행하면
`[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]`와 함께 즉시 종료하며 어떤 변환도 수행하지
않는다(항목 9). 삭제하지 않고 그대로 남겨두었지만, 폐쇄망 standalone 반입 승인의 근거로 사용하지 않는다.

**현재 accepted 표준 경로**:

```
raw XFDL
→ com.example.xfdltracker.pipeline.TargetWebSquarePipeline.convert(File, File, TargetPipelineConfig)
→ WebSquare XML
```

이 경로는 프로그램적(Java API) 진입점이다 — 호출자가 자신의 실제 대상 환경에 맞는
`TargetPipelineConfig`/`TargetRuntimeProfile`을 직접 구성해서 넘겨야 하며, 이 프로젝트는 임의의
"기본" runtime profile을 발명하지 않는다(모든 runtime capability가 사용 가능하다고 가정하는 permissive
기본값도 제공하지 않는다). 범용 배치 CLI(`convert-sample.*`처럼 여러 파일을 한 번에 처리하는 스크립트)를
이 경로로 기계적으로 재배선하지 않은 이유도 동일하다 — 임의 대상 환경을 대표하는 단일 기본
`TargetRuntimeProfile`이 존재하지 않기 때문이다.

**지원하는 7개 필수 family**: `GRID`, `TAB_CONTROL`, `SPLIT_LAYOUT`, `SEARCH_AREA`,
`BUSINESS_TABLE`, `TITLE_BAR`, `BUTTON_GROUP`.

**현재 동작 제한(정확히 문서화)**:
- 일반 스크립트(behavior) 지원 범위는 **유한한 subset**이다 — `SourceScriptAnalyzer`가 지원하는
  top-level 함수 선언 등 닫힌 문법만 지원한다.
- 지원되지 않는 문법은 **fail-closed**한다(변환 전체가 예외로 실패하며, 부분 결과를 발행하지 않는다).
- `uc.*` 호출은 general behavior lane에서 **번역되지 않는다** — `uc.*`가 포함된 스크립트는 해당
  runtime capability가 실제로 사용 가능해도 general behavior lane에서 항상 fail-closed한다
  (`GENERAL_SCRIPT_UC_CALL_TRANSLATION_MODEL = FAIL_CLOSED`).
- Runtime Option C는 **requirements-only**다 — runtime capability 요구사항을 검증만 하며, 어떤
  runtime support 리소스도 발행(emit)하지 않는다.
- 사용 불가능한 runtime capability 요구사항은 **fail-closed**한다(예외 발생, 부분 발행 없음).
- exact target JDK는 여전히 **1.8.0_111**이다. 이 프로젝트의 현재 개발/검증 JDK가 1.8.0_111과 정확히
  일치하기 전까지 target-JDK 최종 검증 완료를 주장하지 않는다.

**standalone 검증 authority**: `verify-standalone.bat`(Windows). 항목 10의 `verify-offline.*`는 이제
`verify-standalone.bat`에 위임하는 thin wrapper로, exact-JDK gate를 포함해 동일한 검증 결과를
낸다(legacy `XPlatformProjectConverter`/Phase1 SHA verifier 단계는 항목 10 표에 여전히 기술되어 있지만
verify-offline이 더 이상 그 단계들을 실행하지 않는다 — 항목 10 안내를 참고).

## 3. 반입 프로젝트와 Phase4 baseline 관계

- **Phase4 original ZIP baseline**은 이 프로젝트 작업의 원본이며 **IMMUTABLE/FROZEN**으로 취급된다 —
  이번 반입 프로젝트 생성 과정에서도 전혀 수정하지 않았다.
- 이 프로젝트의 소스는 Phase4 baseline에서 파생된 **Phase4-derived working candidate**
  (`work/phase4-working/...`)를 COPY한 것이며, 그 working candidate에 누적된 **Production 수정 8건**을
  모두 포함한다(항목 12 참고).
- 즉 이 프로젝트 = Phase4 baseline + 검증된 8건의 최소 수정. baseline 자체를 바꾼 것이 아니라, 그 위에
  파생된 상태를 그대로 반입용으로 옮긴 것이다.

## 4. 필수 환경

| 항목 | 요구사항 |
|---|---|
| OS | Windows 또는 Linux/Unix (둘 다 지원 — `build.bat`/`build.sh` 등 스크립트 쌍 제공) |
| JDK | **1.8.0_111** (exact) |
| 문자 인코딩 | UTF-8 |
| Maven | 불필요 |
| Gradle | 불필요 |
| 외부 JAR | 불필요 |
| 인터넷 연결 | 불필요 |

## 5. 폴더 구조 설명

```
xplatform-to-websquare-offline-import/
├─ src/main/java/...          Production Java source 전체 (76개 파일)
├─ sample-phase3-project/     135 XFDL + 14 XJS 입력 샘플
├─ sample-phase3-output/      최신 working candidate 기준 reference 출력 (136개 XML, 덮어쓰지 않음)
├─ audit/                     Python 기반 audit/verifier 스크립트 + Phase1 fixture
├─ tools/verifier-src/        Java 기반 Phase1ShaVerifier (Production source와 물리적으로 분리)
├─ docs/                      핵심 문서 4종 + 본 가이드(md/docx/pdf)
├─ .idea/                     IntelliJ 최소 project metadata (선택)
├─ .project / .classpath / .settings/   Eclipse 최소 project metadata (선택)
├─ build.bat / build.sh
├─ convert-sample.bat / convert-sample.sh   (non-operational legacy entrypoint, 비운영 -- 항목 9 참고)
├─ verify-offline.bat / verify-offline.sh
├─ README-OFFLINE.md
├─ OFFLINE-IMPORT-MANIFEST.md
├─ SHA256SUMS.txt
└─ .gitignore
```

`build/`(컴파일 산출물)는 스크립트 실행 시에만 생성되는 임시 디렉터리이며, 반입 ZIP 자체에는
포함되지 않는다.

## 6. IDE 선택 가이드

이 프로젝트는 특정 IDE에 종속되지 않는다. **IntelliJ IDEA / Eclipse / Command Line** 중 자유롭게
선택할 수 있으며, 어느 쪽도 필수 의존성이 아니다. **Command Line(`build.bat`/`build.sh`)이 최종
기준 빌드 방법**이고, IDE는 이 소스를 열고 실행하는 편의 수단이다.

### 6.1 IntelliJ IDEA 사용

1. **프로젝트 Open**: IntelliJ에서 `File → Open`으로 `xplatform-to-websquare-offline-import` 폴더
   자체를 연다(포함된 `.idea/` metadata를 인식한다).
2. **Project SDK 설정**: `File → Project Structure → Project → SDK`에서 폐쇄망 PC에 설치된
   **JDK 1.8.0_111**을 선택(없으면 `Add SDK`로 등록).
3. **Language Level 확인**: 같은 화면에서 Language level이 **8**로 되어 있는지 확인
   (`.idea/misc.xml`에 `JDK_1_8`로 이미 지정되어 있음).
4. **Source root 확인**: `src/main/java`가 소스 루트로 인식되는지 확인(`.iml`에 이미 지정됨).
5. **실행 방법**: IntelliJ 내장 컴파일러로 `Build → Build Project`를 실행하거나, IntelliJ의
   Terminal 탭에서 `build.bat`/`build.sh`를 직접 실행해도 된다.

### 6.2 Eclipse 사용

1. **Import**: `File → Import → Existing Projects into Workspace`(또는 폴더 구조에 따라
   `File → Import → Projects from Folder or Archive`)로 `xplatform-to-websquare-offline-import`
   폴더를 선택한다(포함된 `.project`/`.classpath`를 인식한다).
2. **Installed JRE 등록**: `Window → Preferences → Java → Installed JREs`에서 폐쇄망 PC의
   **JDK 1.8.0_111**을 추가 등록한다.
3. **JavaSE-1.8 Execution Environment 연결**: 같은 Preferences 화면의
   `Installed JREs` 또는 `Execution Environments`에서 **JavaSE-1.8**에 방금 등록한
   JDK 1.8.0_111을 연결한다(`.classpath`가 `JavaSE-1.8` 컨테이너를 참조하도록 이미 구성되어 있다).
4. **Compiler compliance 확인**: `Window → Preferences → Java → Compiler`에서
   Compiler compliance level이 **1.8**인지 확인(`.settings/org.eclipse.jdt.core.prefs`에 이미 지정됨).
5. **source/output folder 확인**: `src/main/java`가 source, `build/classes`가 output으로 지정되어
   있는지 확인.
6. **실행 방법**: Eclipse 빌드는 저장 시 자동 컴파일되며, Package Explorer에서 원하는 클래스를
   `Run As → Java Application`으로 실행할 수 있다. 또는 Eclipse의 Terminal/외부 셸에서
   `build.bat`/`build.sh`를 직접 실행해도 된다.

## 7. IDE 없이 Command Prompt / Shell 사용

방법 C(권장, 가장 단순):

```
JAVA_HOME을 JDK 1.8.0_111 설치 경로로 설정
→ build.bat (Windows) 또는 ./build.sh (Linux/Unix)
→ (변환) TargetWebSquarePipeline.convert(File, File, TargetPipelineConfig) 직접 호출 -- 항목 2-1 참고
→ verify-offline.bat 또는 ./verify-offline.sh
```

**주의**: `convert-sample.bat`/`convert-sample.sh`는 **non-operational legacy entrypoint**(비운영,
legacy 진입점)다 — 실행하면 즉시 `[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]` 메시지와
함께 종료 코드 1로 종료하며 어떤 변환도 수행하지 않는다(항목 9 참고). 변환은 반드시
`TargetWebSquarePipeline`을 프로그램적으로(Java API) 직접 호출해야 한다.

## 8. Build 방법

Windows:
```
build.bat
```

Linux/Unix:
```
./build.sh
```

두 스크립트 모두 다음을 수행한다:
- 현재 `JAVA_HOME`/`java`/`javac` 버전 표시
- 정확히 1.8.0_111이면 `[TARGET_JDK_MATCH]`, 아니면 `[TARGET_JDK_MISMATCH_WARNING]`(빌드 자체는 계속
  진행 — 이 경고는 실패가 아니며, target JDK 인증으로 승격되지도 않는다. 인증은 항목 10 참고)
- `src/main/java` 전체를 `build/classes/`로 컴파일(소스 트리 자체에는 `.class`를 생성하지 않음)

## 9. Sample 변환 방법 -- non-operational legacy entrypoint (비운영, 항목 2-1 참고)

**`convert-sample.bat`/`convert-sample.sh`는 현재 비활성화(disabled)된 non-operational legacy
entrypoint다.** 이 스크립트들은 과거 legacy `XPlatformProjectConverter`를 호출했으나, 이제는 실행
즉시 `[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]` 메시지를 출력하고 종료 코드 1로
종료한다 — legacy 변환 호출 자체가 스크립트에서 완전히 제거되었다. 어떤 변환도 수행하지 않으므로,
**이 절에는 실행 가능한 변환 명령 예시를 더 이상 제공하지 않는다.**

정확한 blocker 사유: 임의의 다중 파일 프로젝트를 한 번에 처리하는 범용 배치 CLI를 위한
Reviewer-approved 기본/일반 `TargetRuntimeProfile` 정책이 아직 존재하지 않는다(항목 2-1 참고 — 임의
대상 환경을 대표하는 단일 기본 profile을 발명하지 않는다).

**현재 accepted 변환 방법**은 항목 2-1의 `TargetWebSquarePipeline.convert(File, File,
TargetPipelineConfig)`를 호출자가 자신의 `TargetRuntimeProfile`과 함께 프로그램적으로 직접
호출하는 것뿐이다.

## 10. Offline Verification

**주의(Slice 98BH hardening 반영)**: `verify-offline.*`는 이제 `verify-standalone.bat`에 위임하는
thin wrapper다 — 아래 8단계 표는 이전(legacy) 버전이 실제로 수행하던 단계를 기술한 역사적 설명이며,
현재 `verify-offline.*`는 더 이상 `convert-sample.*`/Phase1 SHA verifier/reference-output diff를
실행하지 않는다. 현재 `verify-offline.*`가 실제로 수행하는 것은 exact JDK gate(fail-closed) +
production/test compile + 전체 project-local test suite 실행이며, 이는 `verify-standalone.bat`와
동일하다(항목 2-1 참고). exact-JDK 실패 시 시각적으로 여전히 명확히 표시된다.

```
verify-offline.bat        (Windows)
./verify-offline.sh       (Linux/Unix)
```

(legacy) 8단계 구성 -- 역사적 설명:

| 단계 | 내용 | 의미 |
|---|---|---|
| 1 | exact JDK 1.8.0_111 게이트 | **mandatory core gate**. `java`/`javac` 둘 다 정확히 1.8.0_111이어야 PASS. 다른 8u 버전/JDK11/17/21/`--release 8`은 인정하지 않음 |
| 2 | clean compile | `build.bat`/`build.sh` 재실행 |
| 3 | sample conversion | (역사적 설명, 현재 미실행) `convert-sample.bat`/`.sh` 재실행, 149/149 확인 -- 이 스크립트는 현재 non-operational legacy entrypoint로 disabled됨(항목 9) |
| 4 | 생성된 output XML 개수 | 136개 확인 |
| 5 | Phase1 SHA verifier | Python(있으면) + Java(항상, mandatory) 양쪽 실행 |
| 6 | source tree `.class`/`.jar` 존재 여부 | 0/0이어야 PASS |
| 7 | reference output diff 요약 | `build/sample-output/` vs `sample-phase3-output/` |
| 8 | (optional) Node.js JS syntax check | Node 없으면 `SKIPPED_OPTIONAL_TOOL` |

결과 표시는 `PASS` / `FAIL` / `SKIPPED_OPTIONAL_TOOL` 세 가지다. **Python이나 Node가 없어도
`SKIPPED_OPTIONAL_TOOL`로만 표시되며 core verification 전체를 실패로 만들지 않는다** — 1~4, 5(Java
verifier만), 6이 core이고, 8과 5의 Python 부분은 optional이다.

**exact JDK 1.8.0_111이 없는 PC에서는 1단계가 반드시 FAIL하고 전체 결과가
`[CORE_VERIFICATION_FAIL]`로 끝난다 — 이는 정상 동작이며 converter defect가 아니다.** 다른 JDK로는
target-JDK 인증을 받을 수 없다는 원칙을 이 스크립트가 강제하는 것뿐이다.

## 11. Phase1 SHA 검증

확정된 추출 recipe(생성된 **출력** XML 기준, 소스 XFDL이 아님):

1. 컨버터가 생성한 WebSquare 출력 XML을 대상으로 한다.
2. 문서 내 모든 `<script>` 요소를 XML 파서(textContent/itertext 의미)로 문서 순서대로 읽는다.
3. 각 요소의 텍스트를 `"\n"`으로 join한다.
4. 끝의 개행을 제거한 뒤 정확히 1개의 `"\n"`을 추가한다.
5. UTF-8(BOM 없음)로 인코딩한다.
6. SHA-256을 계산한다.

Expected:
- `Sample`: `f82379cfb619d611ae4137032af43fd10faf3df88f018ca5db3b72c490f4d3fe`
- `CommentProtection`: `14f3466acde50241698ccf21edec5807464a2a6e903854c78ed59332c7b2b987`

실행:
```
python audit/phase1_sha_verifier.py audit/phase1_sha_manifest.json
```
또는 (Python 없이, JDK만으로):
```
javac -encoding UTF-8 -d build/verifier-classes tools/verifier-src/com/example/xfdltracker/verifier/Phase1ShaVerifier.java
java -cp build/verifier-classes com.example.xfdltracker.verifier.Phase1ShaVerifier audit/phase1_sha_manifest.json
```
두 verifier는 동일한 recipe를 구현한다. **주의(역사적)**: 이 절의 Python/Java Phase1 SHA
verifier는 legacy 변환 출력에 의존하는 절차이며, 현재 `verify-offline.*`는 더 이상 이 단계를
자동 실행하지 않는다(항목 2-1/항목 10 참고) -- 필요하면 위 명령을 수동으로 직접 실행해야 한다.

## 12. 현재 검증된 기능

- control structure (id/type/geometry/hierarchy/property): **148 scanned / 146 PASS / 0 MISMATCH**
- geometry / hierarchy: 대표 화면 REAL_RUNTIME_VERIFIED
- Dataset: **13/13**
- Grid: GRID-1(구조+interaction) / GRID-2 REAL_RUNTIME_VERIFIED
- Static / Edit: 정적 value 렌더링 REAL_RUNTIME_VERIFIED
- CheckBox(unbound) — 역사적 widget/bootstrap evidence: **REAL_RUNTIME_VERIFIED** — 과거 legacy
  `WebSquareGenerator` 출력 기준으로 `addItem(value,label)` 호출, 실제 `<input>`+`<label>` 생성,
  click→checked→`getValue()` round-trip을 실 엔진에서 확인(역사적 검증 기록, 항목 13-5 참고). **이
  검증은 legacy 출력에 대한 것이며, 현재 accepted v6 path의 CheckBox 출력(`<xf:select
  appearance="full"/>`)이 이 역사적 widget 경로와 렌더링/runtime 측면에서 동등한지는 별도로
  검증되지 않았다**(`CHECKBOX_UNBOUND_ACCEPTED_V6_RENDERING_EQUIVALENCE = NOT_VERIFIED`, 항목 13-5
  참고). accepted v6 CheckBox 출력이 `ev:onpageload`/script bootstrap을 전혀 생성하지 않는다는
  사실(항목 13-4, Slice 99D CLOSED_CONTRACT_LIMITATION)은 이 rendering-equivalence 문제와는 별개이며
  자동 page-init 신뢰성 문제 자체가 발행된 산출물에 적용되지 않는다는 판정은 그대로 유지된다.
- Phase1 SHA: **STATIC_VERIFIED / PASS**

## 13. 현재 남은 제한

**제품/Runtime known gap**(항목 1은 Slice 99A, 항목 2는 Slice 99B correction, 항목 3은 Slice 99C, 항목
4는 Slice 99D에서 각각 `CLOSED_CONTRACT_LIMITATION`으로 종결; 항목 5는 Slice 99D correction 1에서 새로
식별된 CheckBox accepted-v6 rendering-equivalence 문제로 `NOT_VERIFIED`인 채 아직 열려 있음):
1. Defect 2 — `CONTENT_NOT_READY` false-negative: **CLOSED_CONTRACT_LIMITATION**(Slice 99A). Tab 동적
   navigation(`someTab.setUrl(...)`/`addTab(...)` 등)은 `identifier.member` 형태라 `SourceScriptAnalyzer`가
   항상 `UNSUPPORTED_SYNTAX`로 결정적으로 거부하며, `TargetWebSquarePipeline`은 이 시점에 전체 변환을
   중단하고 대상 XML을 전혀 발행하지 않는다(`TargetWebSquarePipelineTest`의
   `testDefect2TabDynamicSetUrlMemberCallClosedAsContractLimitation`/
   `testDefect2TabDynamicAddTabMemberCallClosedAsContractLimitationGeneric`, `SourceScriptAnalyzerTest`의
   `testDefect2TabDynamicNavigationMemberCallFailsClosedGenerically`로 검증). 즉 레거시에서 관찰된
   비동기 readiness 오탐(race condition)을 만들어낼 런타임 브리지 자체가 accepted 아키텍처에서는
   생성되지 않는다 -- 안전하지 않은 재현 대신 명시적 fail-closed 계약으로 닫힌 상태다.
2. GRID-3 — 다중 Format(default/alternate) 정의: **CLOSED_CONTRACT_LIMITATION**(Slice 99B
   correction). Format이 1개면 그 topology(Columns/Rows/Bands/Cells)가 완전히 파싱/resolve되어
   기존과 동일하게 지원된다. Format이 2개 이상 정의돼 있으면 `GridFormatParser#resolveFormat`은
   Format 정의 개수를 감지하고 id 중복만 검사할 뿐, 그중 어느 것이 실제 사용할 "활성 Format"인지
   결정적으로 고를 수 있는 source-side selector가 증명되지 않았으므로(id 문자열 의미, 선언 순서,
   Grid의 어떤 attribute도 evidence 없음 -- Slice 99B correction 재조사 결과) 어떤 Format의
   topology도 활성으로 선택/parse하지 않는다 -- "모든 Format이 파싱/검증됐다"는 주장은 하지
   않는다. 따라서 Format이 2개 이상이면 활성 topology 선택 이전 단계에서 항상 명시적으로
   unresolved로 남고, `TargetPayloadExtractor`가 렌더러 도달 전에 결정적으로 fail-closed되어
   대상 XML을 전혀 발행하지 않는다(`GridFormatParserMultiFormatTest`,
   `TargetPayloadExtractorTest`의 `testGridAmbiguousMultiFormatFailsClosedBeforeRenderer`,
   `TargetWebSquarePipelineTest`의 `testIntegrationAmbiguousMultiFormatGridFailsClosedNoPartialOutput`로
   검증). 이는 "동적 전환 제한"이 아니라 selector 증거 부재에 따른 계약상 한계이며, 향후 source
   문법에서 활성 Format selector가 실제로 증명되기 전에는 확장하지 않는다.
3. CheckBox — dataset-bound 케이스: **CLOSED_CONTRACT_LIMITATION**(Slice 99C, correction 2).
   generic한 `<BindItem compid= propid= datasetid= columnid=/>` 선언 문법 자체는 corpus로 증명되어
   있다(`DatasetBinding.xfdl`, Edit 대상). 그러나 **shipped CheckBox 대상 BindItem 실사용례는
   corpus/dev pack 어디에도 없으며**, CheckBox의 checked/unchecked 값 semantics와 target
   `w2:checkbox` binding/runtime 계약 모두 증명되지 않았다. accepted binding resolution은
   evidence-bounded exact-reference subset뿐이다(`SourceBindingAnalyzer`가 compid를 문서 안
   id-attribute exact match로만 resolve, dotted-path 등 미증명 확장 없음). unbound CheckBox
   (corpus 유일 사례)는 기존과 동일하게 그대로 지원된다. source의 어떤 BindItem이 정확히 하나의
   CheckBox Element로 exact resolve되면 `TargetPayloadExtractor`가 그 의미를 추측하지 않고 렌더러
   도달 전에 fail-closed된다(`checkbox_dataset_binding_no_proven_target_contract`). 같은 id를
   가진 Element가 2개 이상이라 ambiguous하게 resolve될 때 그 후보 중 하나가 이 CheckBox이면 --
   "증명이 없으니 unbound로 넘어간다"가 아니라 -- 별도의 명시적 사유
   (`checkbox_dataset_binding_component_reference_ambiguous`)로 동일하게 fail-closed된다. 무관한
   컴포넌트끼리만 얽힌 ambiguous binding은 이 CheckBox에 영향을 주지 않는다(`SourceBindingAnalyzerTest`,
   `TargetPayloadExtractorTest`, `TargetWebSquarePipelineTest`의
   `testIntegrationCheckBoxDatasetBoundFailsClosedNoPartialOutput`/
   `testIntegrationCheckBoxAmbiguousBindingFailsClosedNoPartialOutput`로 검증). "CheckBox dataset
   binding source semantics가 완전히 검증됐다"는 주장이 아니라, 정반대로 그 계약이 증명되지
   않았다는 사실 자체가 이 종결의 근거다.
4. `ev:onpageload` 자동 page-init 신뢰성: **CLOSED_CONTRACT_LIMITATION**(Slice 99D). 4가지 별개
   질문으로 나누어 판단한다 -- (a) event 선언 생성: accepted v6 path는 `ev:onpageload`를 **전혀
   생성하지 않는다**(`AtomicWebSquareRenderer`가 target event local name으로 `onclick`만 인정,
   `TargetPayloadBehaviorFinalizer.EVENT_NAME_MAPPING`도 `onclick` 1건만 존재 -- source `onload`도
   동일하게 `UNSUPPORTED_EVENT_MAPPING`으로 fail-closed됨); (b) handler body 생성: accepted path에
   해당 없음; (c) 실제 자동 발화: 과거(레거시 `WebSquareGenerator`, `pageLoadStatements`/`addItem`
   채널) 조사에서도 "확인하지 못함"으로 스스로 기록됐고, 그 채널 자체가 accepted v6 path에서
   `WebSquareGenerator`와 함께 도달 불가; (d) 지원 context 신뢰성: 위 이유로 해당 없음. 즉 accepted
   path에는 자동 page-init에 의존하는 산출물이 **0건**이다 -- unbound CheckBox는 빈
   `<xf:select appearance="full"/>`로 렌더되어 script/head 콘텐츠 없이도 발행되며(`addItem` 호출
   없음), BIND-1도 `WebSquareGenerator` 전용 수정이라 accepted path에 승계되지 않았다. "자동 발화가
   검증됐다"는 주장이 아니라, accepted path가 애초에 이 메커니즘을 만들지 않는다는 구조적 사실이
   종결 근거다(`TargetPayloadBehaviorFinalizerTest`의 `testOnloadEventNeverMapsToTargetOnpageload`,
   `TargetWebSquarePipelineTest`의 `testIntegrationCheckBoxUnboundSucceedsWithoutPageInitLifecycle`/
   `testIntegrationAllSevenFamiliesReachFinalXml`로 생성 XML에 `onpageload` 부재를 검증).
5. CheckBox(unbound) accepted-v6 rendering-equivalence: **NOT_VERIFIED**(Slice 99D correction 1에서
   새로 식별, 아직 열려 있음). 역사적 widget/bootstrap evidence(`w2:checkbox`, `addItem(value,label)`,
   실제 `<input>`+`<label>` 생성, click-checked-`getValue()` round-trip)는 legacy
   `WebSquareGenerator` 출력에 대해서만 확인된 것이다
   (`CHECKBOX_UNBOUND_HISTORICAL_RUNTIME_EVIDENCE = REAL_RUNTIME_VERIFIED_WIDGET_BOOTSTRAP_SEMANTICS`).
   accepted v6 path는 동일 CheckBox를 `<xf:select appearance="full"/>`(XForms, 자식/ref/items 없음)로
   렌더하며, 이 역사적 경로와 구조적으로 다르다. 두 표현이 실제 렌더링/interaction 측면에서 동등한지는
   증명되지 않았다(`CHECKBOX_UNBOUND_ACCEPTED_V6_RENDERING_EQUIVALENCE = NOT_VERIFIED`). 역사적 검증
   기록이 현재 accepted v6 출력의 검증 authority가 되는 것은 아니다
   (`HISTORICAL_CHECKBOX_RUNTIME_EVIDENCE_IS_ACCEPTED_V6_RENDERING_EQUIVALENCE_AUTHORITY = FALSE`).
   이는 CheckBox dataset-bound 계약 한계(항목 3, binding 증명 부재로 fail-closed되는 별개 경로)나
   `ev:onpageload` 자동 page-init 종결(항목 4, producer/consumer 부재로 `CLOSED_CONTRACT_LIMITATION`)
   과는 별개의 관심사이며, 이 항목이 항목 4의 auto-page-init 종결을 다시 여는 것은 아니다.

**별도 certification blocker 1건**:
- exact JDK 1.8.0_111 확보/검증 상태: **BLOCKED_BY_DISTRIBUTION**. 이 프로젝트를 패키징한 온라인 PC에서
  신뢰 가능한 portable 1.8.0_111 바이너리를 확보하지 못했다(Oracle은 로그인 필수라 시도하지 않았고, 그
  외 아카이브는 설치형이거나 update 번호가 다름). **폐쇄망 사용자가 별도로 exact 1.8.0_111을 확보해
  `verify-offline.*`의 1단계 게이트를 통과시켜야 한다.**

## 14. WebSquare 배포 관련 주의사항

이 프로젝트에는 다음이 **포함되지 않는다**:
- WebSquare server / WebSquare Studio
- WebSquare engine JAR
- Tomcat
- MariaDB
- license

이 프로젝트는 XFDL → WebSquare XML/JS **변환기 소스**만 제공한다. 생성된 XML을 실제 WebSquare 환경에
배포하려면, 해당 기관 폐쇄망의 WebSquare 환경에 설치된 **wpack 컴파일 도구와 배포 절차**를 별도로
따라야 한다(이 프로젝트가 그 절차를 대신하지 않는다).

## 15. 오류 해결 / Troubleshooting

| 증상 | 원인/조치 |
|---|---|
| `JAVA_HOME` 미설정 | `java`/`javac`가 PATH에 없으면 `build.bat`/`build.sh`가 즉시 `[FAIL]`로 종료. JDK 1.8.0_111의 `bin/`을 PATH에 추가하거나 `JAVA_HOME`을 설정 |
| java/javac 버전 불일치 | build helper(`build.bat`/`build.sh`)는 `[TARGET_JDK_MISMATCH_WARNING]`만 표시하고 빌드는 계속 진행한다(경고, 실패 아님). 표준 검증 authority(`verify-standalone.bat`, `verify-offline.*`는 위임)는 동일 불일치를 `[TARGET_JDK_MISMATCH_FAIL]`로 fail-closed 처리한다(검증 실패) — exact 1.8.0_111만 인정 |
| JDK가 아닌 JRE만 등록된 경우 | `javac`가 없으므로 컴파일 자체가 불가. JRE가 아닌 **JDK**를 설치/등록해야 함 |
| IntelliJ SDK 설정 오류 | `File → Project Structure → SDKs`에서 JDK 1.8.0_111의 실제 설치 경로를 다시 지정 |
| Eclipse Installed JRE 오류 | `Window → Preferences → Java → Installed JREs`에서 등록 상태 확인, `JavaSE-1.8` Execution Environment에 연결되어 있는지 재확인 |
| Eclipse compiler level 오류 | `Window → Preferences → Java → Compiler`에서 Compliance level을 1.8로 재설정 |
| UTF-8 문제 | 모든 스크립트가 `-encoding UTF-8`/`chcp 65001`을 명시적으로 사용 — 콘솔 폰트가 한글을 지원하지 않으면 글자가 깨져 보일 뿐 데이터 자체는 정상 |
| `build/classes` 삭제 후 재빌드 | `build.bat`/`build.sh`는 매 실행 시 `build/`를 삭제 후 재생성하므로 별도 수동 삭제 불필요 |
| (역사적, 현재 미해당) Python/Node 미설치 시 optional verification skip | `verify-offline.*`는 현재 exact JDK gate + compile + 전체 test suite만 수행하며 Python/Node 관련 optional 단계가 없다(항목 10 참고) |
| generated output과 reference output 차이 확인 | `verify-offline.*`는 이 비교를 더 이상 자동 수행하지 않는다(legacy 변환 출력에 의존하던 단계이며 제거됨, 항목 10) -- 필요하면 `build/sample-output/`과 `sample-phase3-output/`을 직접 diff 도구로 비교 |

## 16. 보안/폐쇄망 주의사항

- 인터넷 접근 기능 없음(Production Java 코드가 외부 URL을 호출하지 않고, 스크립트도 curl/wget/download를
  실행하지 않음)
- 외부 repository 접근 없음(Maven/Gradle 자체가 없음)
- 실제 credential 포함 안 됨(반입 프로젝트 sanitize 결과는
  `docs/OFFLINE-DOCUMENT-SANITIZATION.md` 참고)
- license 포함 안 됨
- 개발 PC absolute path 의존 없음(모든 스크립트는 `SCRIPT_DIR`/`PROJECT_ROOT` 기준 상대경로 사용)

## 17. 최종 운영 체크리스트

```
[ ] JDK 1.8.0_111 확인 (java -version / javac -version 둘 다 정확히 일치해야 함)
[ ] IntelliJ 또는 Eclipse SDK 연결 (또는 Command Line만 사용)
[ ] build 성공 (build.bat / build.sh)
[ ] verify-standalone.bat(또는 verify-offline.bat/verify-offline.sh) 실행 -- exact JDK 미보유 시
    [TARGET_JDK_MISMATCH_FAIL]로 종료하는 것이 정상 동작이다
[ ] 실제 변환이 필요하면 TargetWebSquarePipeline.convert(File, File, TargetPipelineConfig)를
    자신의 TargetRuntimeProfile과 함께 프로그램적으로 직접 호출한다(항목 2-1) --
    convert-sample.bat/convert-sample.sh는 non-operational legacy entrypoint이며 실행해도
    변환을 수행하지 않는다(항목 9)
```

아래 항목은 legacy Phase4 baseline 회귀 비교를 위한 **역사적 체크리스트**이며, 현재
`convert-sample.*`가 disabled되어 더 이상 재현할 수 없다(항목 9 참고, 실행 불가):

```
[ ] (역사적, 재현 불가) sample conversion 149/149
[ ] (역사적, 재현 불가) XML 생성 확인 (136개)
[ ] (역사적, 재현 불가) Phase1 SHA PASS
[ ] (역사적, 재현 불가) reference diff 검토
```

---

*이 문서(`docs/OFFLINE-USER-GUIDE.md`)가 canonical source이며, 동일 revision에서 생성된
`docs/OFFLINE-USER-GUIDE.docx`/`docs/OFFLINE-USER-GUIDE.pdf`가 함께 제공된다.*
