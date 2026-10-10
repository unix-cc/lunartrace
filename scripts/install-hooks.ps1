# Install the local gate (L1): copy scripts/hooks/* into .git/hooks/*.
# Notes:
#   - This file is intentionally ASCII-only: Windows PowerShell 5.1 reads .ps1 as ANSI
#     unless it has a UTF-8 BOM, so non-ASCII text here would break parsing.
#   - It does NOT touch any git config (no core.hooksPath), to avoid side effects.
$ErrorActionPreference = "Stop"

$scriptsDir = $PSScriptRoot
$repoRoot = Split-Path -Parent $scriptsDir
$gitDir = Join-Path $repoRoot ".git"
$hooksDir = Join-Path $gitDir "hooks"

if (-not (Test-Path $gitDir)) {
    Write-Error "No .git directory found. Run 'git init' in $repoRoot first."
    exit 1
}

New-Item -ItemType Directory -Force -Path $hooksDir | Out-Null

foreach ($name in @("pre-commit", "commit-msg")) {
    $source = Join-Path $scriptsDir "hooks\$name"
    $target = Join-Path $hooksDir $name
    Copy-Item -Path $source -Destination $target -Force
    Write-Host "installed .git/hooks/$name"
}

Write-Host "Done. To bypass the gate, put [skip-hook] and a reason in the commit message."
