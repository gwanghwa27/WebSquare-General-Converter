# build-pipeline-trace.bat 보조 스크립트, Production 변환기 아님.
# 사용법: -File file-sha256.ps1 -TargetPath "<file>"
# 출력: 파일의 SHA-256 해시 한 줄.

param(
    [Parameter(Mandatory = $true)]
    [string]$TargetPath
)

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $TargetPath
Write-Output $hash.Hash
