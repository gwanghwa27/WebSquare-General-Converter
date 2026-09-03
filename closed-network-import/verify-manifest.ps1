# BUILD-AND-VERIFY.cmd 보조 스크립트, Production 변환기 아님.
# -ManifestPath의 각 항목(sha256sum -c 형식)을 -RepoRoot 기준 SHA-256과 대조한다(Get-FileHash만 사용).
# 출력: 불일치/누락 파일별 1줄 + MANIFEST_TOTAL/MANIFEST_MISMATCH, exit 0/1.

param(
    [Parameter(Mandatory = $true)]
    [string]$RepoRoot,
    [Parameter(Mandatory = $true)]
    [string]$ManifestPath
)

$lines = Get-Content -LiteralPath $ManifestPath -Encoding UTF8
$total = 0
$mismatch = 0

foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if ($trimmed.Length -eq 0) { continue }
    $m = [regex]::Match($trimmed, '^([0-9a-fA-F]{64})\s+\*?(.+)$')
    if (-not $m.Success) { continue }
    $expected = $m.Groups[1].Value.ToLower()
    $relPath = $m.Groups[2].Value
    $total++
    $fullPath = Join-Path $RepoRoot $relPath
    if (-not (Test-Path -LiteralPath $fullPath)) {
        Write-Output ("MISSING: " + $relPath)
        $mismatch++
        continue
    }
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $fullPath).Hash.ToLower()
    if ($actual -ne $expected) {
        Write-Output ("MISMATCH: " + $relPath + " expected=" + $expected + " actual=" + $actual)
        $mismatch++
    }
}

Write-Output ("MANIFEST_TOTAL=" + $total)
Write-Output ("MANIFEST_MISMATCH=" + $mismatch)

if ($mismatch -gt 0) { exit 1 } else { exit 0 }
