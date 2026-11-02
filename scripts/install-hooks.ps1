#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    安装 git pre-push 钩子：推送前自动跑本地 CI，不过就拦住。

.DESCRIPTION
    钩子调用 scripts/ci.ps1，退出码非 0 时 git 会中止推送。
    紧急情况可以用 git push --no-verify 跳过。
#>

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$hookPath = Join-Path $repoRoot '.git\hooks\pre-push'

if (-not (Test-Path (Join-Path $repoRoot '.git'))) {
    Write-Host '当前目录不是 git 仓库根目录的上级，退出。' -ForegroundColor Red
    exit 1
}

$content = @'
#!/bin/sh
# 由 scripts/install-hooks.ps1 生成：推送前跑本地 CI
# 紧急跳过：git push --no-verify

ROOT="$(git rev-parse --show-toplevel)"
echo "[pre-push] 本地 CI 启动（跳过请用 git push --no-verify）"

# 有 PowerShell 7 就用 pwsh，没有就退回 Windows PowerShell 5.1
if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -File "$ROOT/scripts/ci.ps1"
else
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ROOT/scripts/ci.ps1"
fi
exit $?
'@

# 必须 LF：CRLF 会让 sh 报 "\r: command not found"
$normalized = $content -replace "`r`n", "`n"
[System.IO.File]::WriteAllText($hookPath, $normalized, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "已安装：$hookPath" -ForegroundColor Green
Write-Host '下次 git push 会自动跑 CI。' -ForegroundColor DarkGray