# BUILD-AND-VERIFY.cmd 보조 스크립트, Production 변환기 아님.
# -OutputRoot 하위 *.xml이 well-formed인지 .NET System.Xml로 검사한다(외부 의존성 없음).
# 출력: XML_PARSE_TOTAL/XML_PARSE_ERR + 실패 파일별 FAIL 줄, exit 0/1.

param(
    [Parameter(Mandatory = $true)]
    [string]$OutputRoot
)

$files = Get-ChildItem -Path $OutputRoot -Filter *.xml -Recurse -File
$err = 0
foreach ($f in $files) {
    try {
        $doc = New-Object System.Xml.XmlDocument
        $doc.Load($f.FullName)
    } catch {
        Write-Output ("FAIL: " + $f.FullName + " -- " + $_.Exception.Message)
        $err++
    }
}
Write-Output ("XML_PARSE_TOTAL=" + $files.Count)
Write-Output ("XML_PARSE_ERR=" + $err)

if ($err -gt 0) { exit 1 } else { exit 0 }
