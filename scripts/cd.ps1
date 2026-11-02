#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    本地 CD：把 CI 产出的 jar 打成镜像、起容器、做健康检查，失败自动回滚。

.DESCRIPTION
    前置：已跑过 scripts/ci.ps1（需要 hub-starter/target 下的 jar）。
    依赖：Docker Desktop 已启动。

.EXAMPLE
    pwsh scripts/cd.ps1
    pwsh scripts/cd.ps1 -Port 8081 -HealthTimeoutSeconds 180
#>

[CmdletBinding()]
param(
    [string]$Tag = '',
    [int]$Port = 8080,
    [int]$HealthTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'

# 本脚本每步都显式判 $LASTEXITCODE；PS 5.1 下 2>&1 叠加 Stop 会把原生 stderr
# 当成异常抛出，所以对 docker 调用前统一放宽
$PSNativeEap = $ErrorActionPreference

# Jenkins 的 bat 步骤按 UTF-8 解码子进程 stdout；显式统一编码，避免中文在控制台乱码
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$repoRoot    = Split-Path -Parent $PSScriptRoot
$deployDir   = Join-Path $repoRoot '.deploy'
$composeFile = Join-Path $repoRoot 'docker\app-compose.yml'
$dockerfile  = Join-Path $repoRoot 'docker\Dockerfile'
$imageRepo   = 'agentic-travel-hub'

function Step([string]$Text) {
    Write-Host ''
    Write-Host ('─' * 66) -ForegroundColor DarkGray
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host ('─' * 66) -ForegroundColor DarkGray
}

# ═══════════════════════════════════════════════════════════════
# 1 前置检查
# ═══════════════════════════════════════════════════════════════

Step '1/5 前置检查'

& docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host '  Docker 守护进程没起来。' -ForegroundColor Red
    Write-Host '  请先启动 Docker Desktop，等托盘图标变绿后重跑。' -ForegroundColor Yellow
    exit 1
}
Write-Host '  Docker 就绪' -ForegroundColor Green

$jar = Get-ChildItem (Join-Path $repoRoot 'hub-starter\target') -Filter '*.jar' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) {
    Write-Host '  没找到 jar，请先跑一次 CI：pwsh scripts/ci.ps1' -ForegroundColor Red
    exit 1
}
Write-Host "  产物：$($jar.Name)" -ForegroundColor Green

# 数据库在宿主机没起来时，应用能启动但健康检查会是 DOWN，先提醒一句
$dbUp = Test-NetConnection -ComputerName 127.0.0.1 -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $dbUp) {
    Write-Host '  提醒：宿主机 5432 没监听，应用会启动但 /actuator/health 大概率为 DOWN' -ForegroundColor Yellow
}

# ═══════════════════════════════════════════════════════════════
# 2 解析版本号
# ═══════════════════════════════════════════════════════════════

Step '2/5 解析版本'

if (-not $Tag) {
    $metaPath = Join-Path $repoRoot 'target\ci-meta.json'
    if (Test-Path $metaPath) {
        $Tag = (Get-Content $metaPath -Raw | ConvertFrom-Json).commit
        Write-Host "  取自 ci-meta.json：$Tag" -ForegroundColor DarkGray
    } else {
        $Tag = (& git rev-parse --short HEAD)
        Write-Host "  取自 git：$Tag" -ForegroundColor DarkGray
    }
}
if (-not $Tag) { $Tag = 'local' }

$image = "${imageRepo}:$Tag"
Write-Host "  镜像：$image" -ForegroundColor Green

# ═══════════════════════════════════════════════════════════════
# 3 构建镜像（保留上一版用于回滚）
# ═══════════════════════════════════════════════════════════════

Step '3/5 构建镜像'

New-Item -ItemType Directory -Force -Path $deployDir | Out-Null
Copy-Item $jar.FullName (Join-Path $deployDir 'app.jar') -Force

# 上一次的 latest 存成 previous：回滚靠它，生产环境应该用不可变 tag + registry
# 不要用 docker image inspect 判存在性：镜像不存在时它写 stderr，
# 在 $ErrorActionPreference='Stop' 下会被 PowerShell 5.1 当成异常直接终止脚本。
$prevId = (& docker images -q "${imageRepo}:latest" 2>$null)
$hasPrevious = -not [string]::IsNullOrWhiteSpace(($prevId | Out-String).Trim())
if ($hasPrevious) {
    & docker tag "${imageRepo}:latest" "${imageRepo}:previous" | Out-Null
    Write-Host '  已保留上一版为 previous（用于失败回滚）' -ForegroundColor DarkGray
}

$buildStart = Get-Date
& docker build -f $dockerfile -t $image -t "${imageRepo}:latest" $deployDir
if ($LASTEXITCODE -ne 0) {
    Write-Host '  镜像构建失败。' -ForegroundColor Red
    exit 1
}
$buildSeconds = [math]::Round(((Get-Date) - $buildStart).TotalSeconds, 1)
Write-Host "  构建完成（${buildSeconds}s）" -ForegroundColor Green

# ═══════════════════════════════════════════════════════════════
# 4 起容器
# ═══════════════════════════════════════════════════════════════

Step '4/5 启动容器'

$env:APP_IMAGE = $image
$env:APP_PORT  = "$Port"

& docker compose -f $composeFile up -d --force-recreate
if ($LASTEXITCODE -ne 0) {
    Write-Host '  容器启动失败。' -ForegroundColor Red
    exit 1
}

# ═══════════════════════════════════════════════════════════════
# 5 健康检查
# ═══════════════════════════════════════════════════════════════

Step '5/5 健康检查'

$healthUrl = "http://localhost:$Port/actuator/health"
$deadline  = (Get-Date).AddSeconds($HealthTimeoutSeconds)
$status    = $null

Write-Host "  探测 $healthUrl" -ForegroundColor DarkGray
while ((Get-Date) -lt $deadline) {
    try {
        $resp = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
        if ($resp.Content -match '"status"\s*:\s*"([A-Z]+)"') {
            $status = $Matches[1]
            break
        }
    }
    catch {
        # 端口还没起来，继续等
    }
    Write-Host '.' -NoNewline -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
}
Write-Host ''

if ($status -eq 'UP') {
    Write-Host ''
    Write-Host "  部署成功：$image 已运行在 http://localhost:$Port" -ForegroundColor Green
    exit 0
}

if ($status) {
    # 进程起来了，但依赖没连上——不算部署失败，但要如实报出来
    Write-Host ''
    Write-Host "  容器已启动，但健康状态是 $status（多半是数据库/Redis 没连上）。" -ForegroundColor Yellow
    Write-Host "  看日志：docker logs -f travel-hub-app" -ForegroundColor DarkGray
    exit 0
}

Write-Host ''
Write-Host "  ${HealthTimeoutSeconds}s 内没等到健康响应，判定部署失败。" -ForegroundColor Red
& docker logs --tail 40 travel-hub-app

if ($hasPrevious) {
    Write-Host ''
    Write-Host '  正在回滚到上一版…' -ForegroundColor Yellow
    $env:APP_IMAGE = "${imageRepo}:previous"
    & docker compose -f $composeFile up -d --force-recreate | Out-Null
    Write-Host '  已回滚。' -ForegroundColor Yellow
}
exit 1