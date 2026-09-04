# 폐쇄망 1회 반입 가이드 (One-shot Closed Network Import)

> **폐쇄망 반입 대상은 물리적으로 1개 파일이다**: 이 저장소 루트를 통째로
> 압축한 ZIP 파일(예: `websquare-general-converter-<commit>.zip` -- 실제
> 파일명은 이 저장소를 압축하는 사용자가 임의로 정하며, 동봉된
> `.zip.sha256`으로 무결성 확인) **하나만** 반입한다. **`closed-network-import/`
> 폴더만 따로 복사하지 말 것** -- 이 폴더는 소스를 담고 있지 않다(검증
> kit일 뿐이며, `src/main/java` 등 실제 소스는 저장소 루트의 다른
> 디렉터리에 있다). ZIP을 풀면 이 저장소의 전체 editable source가 그대로
> 들어있다.

이 문서는 이 저장소(main)를 폐쇄망에 **한 번만** 반입해서 build →
conversion → regression → class-policy 검증까지 마치고, 이후 폐쇄망
WebSquare Studio에서 최종 확인할 수 있도록 안내한다. 인터넷 문서 링크에
의존하지 않는다 -- 필요한 모든 것은 이 저장소 안에 있다.

## 0. 중요 -- 이 저장소 자체가 이미 self-contained project다

이 `closed-network-import/` 디렉터리는 별도의 파일 복사본 묶음이
**아니다**. 이 저장소(WebSquare General Converter, git 저장소) 전체가
이미 완전한 독립 프로젝트다(`build.sh`/`build.bat`,
`convert-sample.sh`/`.bat`, `verify-offline.sh`/`.bat`, `src/`,
`sample-phase3-project/`, `analysis/` 등 전부 저장소 root에 존재). 따라서:

**"1회 반입" = 이 저장소(또는 이 commit 시점의 전체 디렉터리)를 폐쇄망에
한 번 복사/체크아웃하는 것 자체다.** 이 디렉터리는 그 반입 이후 실행할
**무결성 확인 + 빌드/회귀 + class-policy 검증을 자동화하는 kit**이며,
저장소를 다시 쪼개어 담지 않는다 -- 이미 저장소 전체가 반입 대상이므로
별도로 이중으로 담지 않는다.

## 1. 반입 위치

폐쇄망 Windows WebSquare 개발 환경에서, 원하는 작업 위치에 이 저장소
전체를 복사한다. 경로 자체는 자유롭게 선택 가능 -- 공백/한글 경로도
지원한다(하위 도구들이 전부 quoting을 지킴).

## 2. 기존 project backup 방법

반입 전, 기존에 이미 폐쇄망에 있던 이전 반입 사본이 있다면:

```
robocopy "<이전 반입 경로>" "<이전 반입 경로>.bak-YYYYMMDD" /E
```
(또는 폴더 전체를 다른 이름으로 복사해 두는 것으로 충분 -- git 저장소이므로
`.git` 히스토리 자체가 이미 이전 상태의 백업이기도 하다.)

## 3. 변경 source 적용 방법

이 저장소(main, 최신 commit 기준)를 그대로 폐쇄망에 복사하면 끝이다.
별도 patch 적용 단계가 없다 -- "몇 개 파일만 덮어쓰기" 방식이 아니라 전체
디렉터리 자체가 최신 상태다. 어떤 commit이 어떤 파일을 변경했는지 추적하고
싶으면 이 저장소의 git 히스토리(`git log`)를 변경 이력 참고 자료/historical
evidence/provenance 확인 수단으로 사용할 수 있다 -- 다만 git commit 이력
자체는 현재 architecture, runtime contract, capability 지원 범위 또는
accepted behavior의 authority가 아니다. 현재 architecture와 accepted
behavior는 Reviewer가 승인한 architecture standing과 accepted current
source/contract를 기준으로 판단하며, 이 문서를 포함한 active
documentation(`README.md`, `README-OFFLINE.md`,
`docs/OFFLINE-USER-GUIDE.md` 등)은 그 accepted architecture를 사용자에게
설명하는 안내 문서일 뿐, 문서 자체가 Reviewer standing을 변경하거나 새로운
capability를 승인하는 authority는 아니다. 이 문서에 라운드별 변경 파일
목록을 별도로 유지하지 않는다.

## 4. Canonical contents.css에 대해 -- 실제 CSS는 Git 미추적, metadata만 보관

`resources/target-websquare/WebContent/assets/css/contents-css-metadata.json`에
실제 운영 contents.css의 SHA-256/선택자 목록/구조 semantic 요약만 보관한다.
**실제 CSS 파일 전체는 Git에 추적하지 않는다**(외부 실 운영 자산이므로 --
상세: `analysis/repository-external-artifact-policy.md`). contents.css는
이미 `websquare/config.xml`의 `<stylesheet earlyImportList="...">` 설정을
통해 폐쇄망 프로젝트에 전역 로딩되고 있음이 확인됐다(`analysis/
contents-css-integration-audit.md`). **이 저장소는 실제 운영
`WebContent/assets/css/contents.css`를 자동으로 배포/덮어쓰지 않는다** --
이 converter는 CSS 파일을 배포하는 코드를 포함하지 않고, 빌드/회귀도 이
파일의 존재를 요구하지 않는다(`EXTERNAL_FILE_REQUIRED_FOR_BUILD = NO`).
필요하면 SHA 비교로 실제 운영 파일과 metadata 기록이 같은지만 확인한다:

```
certutil -hashfile "C:\실제프로젝트경로\WebContent\assets\css\contents.css" SHA256
```
metadata에 기록된 SHA(`9634dbcd506d3eeaf1a238e4157059d6c3c4c2facdd85039ba8b46a30c9bcd62`)와
같으면 지금까지의 구조 분석(shbox/dfbox/tbbox 등)이 실제 운영 CSS 기준
그대로 유효하다는 뜻이다. 로컬에 참조용 사본을 직접 두고 싶다면
`resources/target-websquare/WebContent/assets/css/contents.css`에 두면
되지만(OPTIONAL_LOCAL_EVIDENCE_CHECK), 이 사본은 Git에 커밋되지 않는다.

## 5. Build 방법

폐쇄망 JDK 1.8.0_111 기준(exact JDK 요구사항, `verify-offline.sh`/`.bat`
참고):

```
cd <반입한 저장소 루트>
build.bat
```
(Windows) 또는 `sh build.sh`(WSL/Git Bash 있는 경우).

## 6. 변환 방법 -- non-operational legacy entrypoint 안내 (Slice 98BH correction)

**`convert-sample.bat`/`convert-sample.sh`는 현재 disabled 상태다.** 실행하면
`[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]` 메시지와 함께 즉시
종료하며 어떤 변환도 수행하지 않는다. 아래에 있던 legacy
`XPlatformProjectConverter` 직접 실행 예시도 더 이상 제공하지 않는다(실행
가능한 legacy 변환 명령이 아니다).

**현재 accepted 표준 경로**:
```
raw XFDL
→ com.example.xfdltracker.pipeline.TargetWebSquarePipeline.convert(File, File, TargetPipelineConfig)
→ WebSquare XML
```
호출자가 자신의 `TargetRuntimeProfile`을 직접 구성해서 넘겨야 하며, 임의의
"기본" profile은 여전히 존재하지 않는다(자세한 내용: `docs/OFFLINE-USER-GUIDE.md`
항목 2-1).

**여러 파일을 한 번에 변환하려면(Slice 99F)** `closed-network-import\
BATCH-CONVERT.cmd`(정규 platform)/`.sh`(best-effort bridge)를 쓴다 -- 호출자가
`inputRoot`/`outputRoot`/runtime profile 파일 세 인자를 명시적으로 제공해야
하며, exact-JDK 게이트는 `verify-standalone.bat`에 위임한다(자체 재구현 없음).
`inputRoot`/`outputRoot`는 같거나 서로 nested되면 안 되고(real path 기준),
심볼릭 링크 항목은 확장자/대상과 무관하게 항상 거부하며, junction 등
Java가 심볼릭 링크로 인식 못 하는 별칭도 root 밖 이탈이든 root 안에서의
단순 중복 노출이든 real path 재확인으로 거부한다(Slice 99F Correction 2).
output 쪽은 최종 real path가 root 안이어도 그 경로 중간에 별칭이 있으면
동일하게 거부하며, 중간 구간이 존재하는지는 링크를 따라가지 않고
판정하므로(NOFOLLOW) 대상이 지워진 dangling alias를 "아직 없음"으로
착각하지 않는다(Slice 99F Correction 3). 계획된 최종 `.xml` 경로 자체도
같은 NOFOLLOW 기준으로 이미 점유돼 있는지 확인하며(entry 종류 무관 --
파일/디렉터리/dangling entry 전부 포함, Slice 99F Correction 4), 어떤
변환도 시작되기 전에 fail-closed하고 기존 entry는 건드리지 않는다.
이 batch 실행의 성공/실패는 아래 7번 `MANIFEST.sha256` 비교와 무관하다
(별개 authority).
자세한 문법은 `docs/OFFLINE-USER-GUIDE.md` 항목 2-2, 예시는
`closed-network-import\example-runtime-profile.txt` 참고.

## 7. Regression 방법 -- BUILD-AND-VERIFY는 변환 entrypoint가 아님

`closed-network-import\BUILD-AND-VERIFY.cmd`/`BUILD-AND-VERIFY.sh`는 현재
MANIFEST.sha256 무결성 확인 + clean compile까지만 수행한 뒤
`[CURRENT_PROJECT_CLI_CONFIGURATION_CONTRACT_BLOCKER]`를 출력하고 실패로
종료한다 -- **변환/회귀 entrypoint가 아니다**(legacy 변환 의존 단계는 전부
제거됨, 실제 batch 변환은 위 `BATCH-CONVERT.cmd`/`.sh` 참고).

standalone 검증 authority는 `verify-standalone.bat`이다(`verify-offline.bat`/
`verify-offline.sh`는 여기로 위임하는 thin wrapper):
```
verify-standalone.bat
```
또는
```
sh verify-offline.sh
```

**MANIFEST 관련 참고**: `MANIFEST.sha256`는 텍스트 파일(`.java`/`.md`/
`.sh` 등)의 줄바꿈(LF/CRLF)에 영향을 받는다. git의 `core.autocrlf` 설정이
반입 환경에서 다르면(예: 이 저장소를 준비한 개발 환경과 폐쇄망 Windows
환경의 git 설정 차이) MANIFEST 비교에서 **텍스트 파일만** mismatch로
표시될 수 있다 -- 이는 실제 내용 손상이 아니라 줄바꿈 정규화 차이일
가능성이 높다(빌드/실행에는 영향 없음, javac/node/python 전부 CRLF와 LF를
동일하게 처리한다). 반면 `resources/target-websquare/WebContent/assets/
css/contents.css`(REFERENCE_ONLY, 원본이 LF-only)처럼 byte-exact 여부가
중요한 파일은 mismatch가 나오면 반드시 직접 SHA 값을 비교해 확인한다
(4번 항목의 canonical SHA `9634dbcd506d3eeaf1a238e4157059d6c3c4c2facdd85039ba8b46a30c9bcd62`
참고). `.java` 소스 파일 mismatch는 clean compile이 실제로 성공하는지로
교차 확인하면 된다(내용이 실제로 손상됐다면 컴파일이 실패한다).

## 8. STT00030 생성 방법 (역사적 자료 -- non-operational, 재현 불가)

**이 절은 legacy `XPlatformProjectConverter` 기준 역사적 절차이며, 현재
disabled 상태다(항목 6 참고). 실행 가능한 명령을 제공하지 않는다.** 실제 화면
evidence가 필요하면 `TargetWebSquarePipeline.convert(File, File,
TargetPipelineConfig)`를 해당 XFDL 파일에 대해 직접 호출해야 한다.

(역사적 설명, 재현 불가) 과거에는 생성된 출력 XML에서 다음을 확인했었다:
- `Div01`/`Div00`/`Div02`/`Div03` → `xf:group`(무변경)
- `Div02`/`Div03`의 `style`에 `background:...` 보존 여부
- `Div01_MNG_BOCD`(Combo) → `disabledClass="w2selectbox_disabled"` 포함

## 9. Studio에서 확인할 항목

**Slice 99H 명확화**: 아래 항목은 항목 8과 동일한 legacy `XPlatformProjectConverter`/STT00030
기준 역사적 Studio 체크리스트다(재현 불가). 특히 항목 5의 `btn_cm`/`wq_gvw`는 accepted 경로가
source `cssclass`를 병합 발행한다는 주장이 아니다 -- accepted `TargetWebSquarePipeline`은
GRID에 고정 `wq_gvw`만 독립 발행하고(`CLASS_MERGE_RUNTIME_RESOLUTION_CLASS =
LEGACY_ONLY_NOT_ACCEPTED_PATH_CONCERN`, `README-OFFLINE.md` 항목 8 참고) BUTTON에는 `btn_cm`을
전혀 발행하지 않는다.

1. `Div01`(Calendar/Combo) 표시 여부
2. `Div00`(조회/엑셀 버튼) 표시 여부
3. `Div02`/`Div03`(우측 버튼 4개씩) 표시 여부 -- 특히 배경색이 이제
   보이는지(`background:#ffEEEfff;`/`background: #ffffffff;` 보존 확인)
4. Combo(`Div01_MNG_BOCD`) disabled 상태일 때 `w2selectbox_disabled`
   스타일(회색 배경, `#bdbeca` 텍스트)이 실제로 적용되는지
5. Button/Grid(`btn_cm`/`wq_gvw`) 기존 스타일이 그대로 유지되는지(회귀
   없음 확인용)
6. 전체 화면 layout(percentage geometry)이 이전 라운드 대비 달라지지
   않았는지

## 10. 실패 시 rollback 방법

- 이 반입 디렉터리 전체를 지우고 2번에서 만든 backup으로 복원한다.
- 또는 git 저장소라면: `git log --oneline`으로 이전 커밋 확인 후
  `git checkout <이전 커밋 SHA>`로 되돌린다(이 저장소는 각 commit이
  개별적으로 구분되어 세밀한 rollback이 가능하다).
- `WebContent/assets/css/contents.css`(실제 운영 파일)는 이 저장소가
  건드리지 않으므로 별도 rollback이 필요 없다(4번 참고).

## 참고 문서

- `analysis/contents-css-integration-audit.md` -- CSS 전역 로딩/base
  widget class 조사(historical audit record)
- `analysis/target-class-state-policy-audit.md` -- class/state policy
  관련 historical audit record
- `analysis/freeze-vs-candidate-function-diff.md` -- 과거 freeze 시점
  기준 함수 단위 diff 이력(historical record)
- `README-OFFLINE.md`, `OFFLINE-IMPORT-MANIFEST.md` -- 이 저장소 자체의
  폐쇄망 반입 원칙(`README-OFFLINE.md`는 현재 product documentation,
  `OFFLINE-IMPORT-MANIFEST.md`는 과거 freeze 시점의 historical evidence)
