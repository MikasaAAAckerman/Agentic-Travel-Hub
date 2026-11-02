#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    本地 CI 流水线：推送前先自己把"能不能过"跑一遍。

.DESCRIPTION
    五个阶段：环境检查 -> 编译 -> 测试 -> 打包 -> 产物指纹。
    任一段失败立即中断，退出码非 0（git pre-push 钩子靠它拦推送）。
    完整日志落在 target/ci-logs/ 下，控制台只显示关键行。

.EXAMPLE
    pwsh scripts/ci.ps1
    pwsh scripts/ci.ps1 -SkipTests
#>

[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

$repoRoot  = Split-Path -Parent $PSScriptRoot
$logDir    = Join-Path $repoRoot 'target/ci-logs'
$targetDir = Join-Path $repoRoot 'target'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# 不依赖 PATH 里恰好有 mvn.cmd
$mvn = (Get-Command mvn.cmd -ErrorAction SilentlyContinue)
$mvn = if ($mvn) { $mvn.Source } else { 'mvn.cmd' }

$startedAt    = Get-Date
$stageResults = [System.Collections.Generic.List[object]]::new()
$script:report = [System.Collections.Generic.List[string]]::new()

function Write-Banner([string]$Text) {
    Write-Host ''
    Write-Host ('─' * 66) -ForegroundColor DarkGray
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host ('─' * 66) -ForegroundColor DarkGray
}

function Get-NativeOutput {
    <#
      执行原生程序并回收所有输出行。
      PS 5.1 下 $ErrorActionPreference='Stop' 叠加 2>&1 会把 stderr 的一行
      当成 NativeCommandError 抛出（java -version 正是往 stderr 写的），
      所以这里临时放宽，只把输出交回去。
    #>
    param(
        [Parameter(Mandatory)][string]$Command,
        [string[]]$Arguments = @()
    )

    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $lines = @(& $Command @Arguments 2>&1 | ForEach-Object { "$_" })
    $ErrorActionPreference = $prevEap
    return $lines
}

function Invoke-Maven {
    <#
      跑一次 maven，全量日志进文件，控制台只回显关键行。
      返回原生退出码，由调用方决定是否中断。
    #>
    param(
        [Parameter(Mandatory)][string]$LogName,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    $logFile = Join-Path $logDir "$LogName.log"
    if (Test-Path $logFile) { Remove-Item $logFile -Force }

    # PS 5.1 陷阱：$ErrorActionPreference='Stop' 叠加 2>&1 时，
    # 原生程序往 stderr 写一行就会被当成 NativeCommandError 抛出。
    # 这里临时放宽，成败只认退出码。
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'

    $start = Get-Date
    & $mvn @Arguments 2>&1 | ForEach-Object {
        $line = "$_"
        Add-Content -Path $logFile -Value $line -Encoding utf8

        if ($line -match '^\[ERROR\]') {
            Write-Host "  $line" -ForegroundColor Red
        }
        elseif ($line -match 'BUILD SUCCESS') {
            Write-Host "  $line" -ForegroundColor Green
        }
        elseif ($line -match 'BUILD FAILURE') {
            Write-Host "  $line" -ForegroundColor Red
        }
        elseif ($line -match '^\[INFO\] Building |^\[INFO\] Tests run:|^\[WARNING\] .*deprecat') {
            Write-Host "  $line" -ForegroundColor DarkGray
        }
    }
    $code = $LASTEXITCODE

    $ErrorActionPreference = $prevEap

    $script:lastMavenSeconds = [math]::Round(((Get-Date) - $start).TotalSeconds, 1)
    return $code
}

function Invoke-Stage {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][scriptblock]$Action
    )

    Write-Banner $Name
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $ok = $true
    $detail = ''

    try {
        $detail = & $Action
    }
    catch {
        $ok = $false
        $detail = $_.Exception.Message
        Write-Host "  x $detail" -ForegroundColor Red
    }

    $sw.Stop()
    $seconds = [math]::Round($sw.Elapsed.TotalSeconds, 1)
    $stageResults.Add([pscustomobject]@{ Stage = $Name; Seconds = $seconds; Ok = $ok; Detail = $detail })

    if (-not $ok) {
        Write-Host ''
        Write-Host "流水线在「$Name」中断，已阻止继续。" -ForegroundColor Red
        Write-Host "完整日志：$logDir" -ForegroundColor DarkGray
        exit 1
    }

    $suffix = if ($detail) { " - $detail" } else { '' }
    Write-Host ("  v 完成 ({0:N1}s){1}" -f $seconds, $suffix) -ForegroundColor Green
}

# ═══════════════════════════════════════════════════════════════
# 1 环境检查
# ═══════════════════════════════════════════════════════════════

Invoke-Stage '1/5 环境检查' {
    $javaLine = @(Get-NativeOutput -Command 'java' -Arguments @('-version'))[0]
    if ($javaLine -notmatch '"(\d+)') { throw "java 不可用，请确认 PATH（返回：$javaLine）" }
    $major = [int]$Matches[1]
    if ($major -lt 21) { throw "需要 JDK 21+，当前：$javaLine" }

    $mvnLine = @(Get-NativeOutput -Command $mvn -Arguments @('-v'))[0]
    if (-not $mvnLine) { throw 'mvn 不可用，请确认 PATH' }

    $script:gitBranch = @(Get-NativeOutput -Command 'git' -Arguments @('rev-parse', '--abbrev-ref', 'HEAD'))[0]
    $script:gitSha    = @(Get-NativeOutput -Command 'git' -Arguments @('rev-parse', '--short', 'HEAD'))[0]
    $dirtyLines       = @(Get-NativeOutput -Command 'git' -Arguments @('status', '--porcelain') | Where-Object { $_ })
    $script:gitDirty  = $dirtyLines.Count -gt 0

    $dirtyMark = if ($script:gitDirty) { '（有未提交改动）' } else { '' }
    "JDK $major | $mvnLine | $($script:gitBranch) @ $($script:gitSha)$dirtyMark"
}

# ═══════════════════════════════════════════════════════════════
# 2 编译
# ═══════════════════════════════════════════════════════════════

Invoke-Stage '2/5 编译' {
    $code = Invoke-Maven -LogName 'compile' -Arguments @('-B', 'clean', 'compile', '-DskipTests')
    if ($code -ne 0) { throw '编译失败，详见 target/ci-logs/compile.log' }
    "耗时 $($script:lastMavenSeconds)s"
}

# ═══════════════════════════════════════════════════════════════
# 3 测试
# ═══════════════════════════════════════════════════════════════

Invoke-Stage '3/5 测试' {
    if ($SkipTests) { return '已跳过（-SkipTests）' }

    $code = Invoke-Maven -LogName 'test' -Arguments @('-B', 'test')
    if ($code -ne 0) { throw '测试失败，详见 target/ci-logs/test.log' }

    # 汇总各模块 surefire 报告
    $total = @{ run = 0; fail = 0; error = 0; skip = 0 }
    $reportDirs = Get-ChildItem -Path $repoRoot -Directory |
        ForEach-Object { Join-Path $_.FullName 'target\surefire-reports' } |
        Where-Object { Test-Path $_ }

    foreach ($dir in $reportDirs) {
        Get-ChildItem $dir -Filter '*.txt' -File | ForEach-Object {
            $first = Get-Content $_.FullName -TotalCount 4 | Where-Object { $_ -match 'Tests run:' } | Select-Object -First 1
            if ($first -match 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)') {
                $total.run   += [int]$Matches[1]
                $total.fail  += [int]$Matches[2]
                $total.error += [int]$Matches[3]
                $total.skip  += [int]$Matches[4]
            }
        }
    }

    $script:testSummary = "$($total.run) 个用例 / 失败 $($total.fail) / 错误 $($total.error) / 跳过 $($total.skip)"
    if ($total.fail -gt 0 -or $total.error -gt 0) { throw "测试未全绿：$($script:testSummary)" }
    $script:testSummary
}

# ═══════════════════════════════════════════════════════════════
# 4 打包
# ═══════════════════════════════════════════════════════════════

Invoke-Stage '4/5 打包' {
    $code = Invoke-Maven -LogName 'package' -Arguments @('-B', 'package', '-DskipTests')
    if ($code -ne 0) { throw '打包失败，详见 target/ci-logs/package.log' }
    "耗时 $($script:lastMavenSeconds)s"
}

# ═══════════════════════════════════════════════════════════════
# 5 产物指纹
# ═══════════════════════════════════════════════════════════════

Invoke-Stage '5/5 产物指纹' {
    $jar = Get-ChildItem (Join-Path $repoRoot 'hub-starter\target') -Filter '*.jar' -File |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $jar) { throw 'hub-starter/target 下没有 jar' }

    $hash   = (Get-FileHash $jar.FullName -Algorithm SHA256).Hash
    $sizeMb = [math]::Round($jar.Length / 1MB, 1)

    # 给 CD 读的元数据
    $meta = [ordered]@{
        commit  = $script:gitSha
        branch  = $script:gitBranch
        dirty   = $script:gitDirty
        builtAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')
        jar     = $jar.Name
        sizeMB  = $sizeMb
        sha256  = $hash
        tests   = $(if ($script:testSummary) { $script:testSummary } else { 'skipped' })
    }
    $meta | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $targetDir 'ci-meta.json') -Encoding utf8

    $script:artifactLine = "$($jar.Name) | ${sizeMb}MB | sha256 $($hash.Substring(0, 16))..."
    $script:artifactLine
}

# ═══════════════════════════════════════════════════════════════
# 报告
# ═══════════════════════════════════════════════════════════════

Write-Banner '流水线报告'

foreach ($r in $stageResults) {
    $mark  = if ($r.Ok) { 'v' } else { 'x' }
    $color = if ($r.Ok) { 'Green' } else { 'Red' }
    Write-Host ("  {0}  {1,-14} {2,7:N1}s  {3}" -f $mark, $r.Stage, $r.Seconds, $r.Detail) -ForegroundColor $color
}

$totalSeconds = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 1)
Write-Host ''
Write-Host ("  总计 {0:N1}s | {1} | {2}" -f $totalSeconds, $script:gitBranch, $script:gitSha) -ForegroundColor Cyan

$reportLines = @(
    "Agentic-Travel-Hub local CI"
    "time    : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    "commit  : $($script:gitBranch) @ $($script:gitSha)$(if ($script:gitDirty) { ' (dirty)' } else { '' })"
    "tests   : $(if ($script:testSummary) { $script:testSummary } else { 'skipped' })"
    "artifact: $($script:artifactLine)"
    "total   : $($totalSeconds)s"
)
$reportLines | Set-Content -Path (Join-Path $targetDir 'ci-report.txt') -Encoding utf8

Write-Host ''
Write-Host '  全部通过，可以推送。' -ForegroundColor Green
exit 0