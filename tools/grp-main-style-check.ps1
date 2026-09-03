# build-pipeline-trace.bat 보조 스크립트, Production 변환기 아님(같은 tools/ 내부, 외부 의존성 아님).
# 사용법: -File grp-main-style-check.ps1 -TargetPath "<file>"
# 출력 3줄: RESULTAREA=..., GRPMAIN=..., EMPTY=YES|NO|N/A

param(
    [Parameter(Mandatory = $true)]
    [string]$TargetPath
)

$content = Get-Content -LiteralPath $TargetPath -Raw -Encoding UTF8

$resultAreaMatch = [regex]::Match($content, 'id=.grp_resultArea.[^/\r\n]*')
if ($resultAreaMatch.Success) {
    $resultAreaText = $resultAreaMatch.Value
} else {
    $resultAreaText = '(not found)'
}

$mainMatch = [regex]::Match($content, 'id=.grp_main.[^/\r\n]*')
if ($mainMatch.Success) {
    $mainText = $mainMatch.Value
} else {
    $mainText = '(not found)'
}

if ($mainMatch.Success) {
    if ($mainMatch.Value -match 'style=""') {
        $empty = 'YES'
    } else {
        $empty = 'NO'
    }
} else {
    $empty = 'N/A'
}

# 호출자가 batch(.bat)이므로 <>&|^는 cmd.exe에서 리다이렉션/파이프/이스케이프로 해석되어
# 파싱을 깨뜨린다(실제로 XML 태그의 ">가 리다이렉션으로 오인된 사례 있음). 출력 전에 제거한다.
$resultAreaText = $resultAreaText -replace '[<>&|^]', ''
$mainText = $mainText -replace '[<>&|^]', ''

Write-Output ('RESULTAREA=' + $resultAreaText)
Write-Output ('GRPMAIN=' + $mainText)
Write-Output ('EMPTY=' + $empty)
