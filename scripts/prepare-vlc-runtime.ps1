# 准备随包内置的 VLC 运行时（裁剪到纯音频最小集），供 :composeApp 的 jpackage 打包使用。
#
# 源优先级（自上而下取第一个可用者）：
#   1. -Source / $env:MUSES_VLC_DIR    已解压的 VLC 目录（含 libvlc.dll）
#   2. -SourceZip / $env:MUSES_VLC_ZIP VLC win64 zip（官方 vlc-<ver>-win64.zip）
#   3. 仓库 spike-vlcj/vlc-portable/vlc-3.0.21        开发机本地便携版
#   4. 仓库 spike-vlcj/vlc-portable/vlc-3.0.21-win64.zip
# 全部缺失时：打印警告并正常退出（不产出内置运行时，构建继续，运行期回退系统已装 VLC）。
#
# 用法：
#   pwsh -File scripts/prepare-vlc-runtime.ps1 -Destination <appResourcesDir>/vlc
param(
    [string]$Source = $env:MUSES_VLC_DIR,
    [string]$SourceZip = $env:MUSES_VLC_ZIP,
    [Parameter(Mandatory = $true)][string]$Destination,
    [string]$Staging = ''
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$repoRoot = Split-Path -Parent $PSScriptRoot
$portableDir = Join-Path $repoRoot 'spike-vlcj/vlc-portable/vlc-3.0.21'
$portableZip = Join-Path $repoRoot 'spike-vlcj/vlc-portable/vlc-3.0.21-win64.zip'
$trimScript = Join-Path $PSScriptRoot 'vlc-trim.ps1'
if (-not $Staging) {
    $Staging = Join-Path (Split-Path -Parent $Destination) 'vlc-staging'
}

function Test-VlcDir([string]$path) {
    return $path -and (Test-Path (Join-Path $path 'libvlc.dll'))
}

$resolved = $null
if ((Test-VlcDir $Source)) {
    $resolved = (Resolve-Path $Source).Path
    Write-Host "[prepare-vlc] 源：MUSES_VLC_DIR（$resolved）"
}
elseif ($SourceZip -and (Test-Path $SourceZip)) {
    Write-Host "[prepare-vlc] 源：MUSES_VLC_ZIP（$SourceZip）"
    $resolved = $SourceZip
}
elseif ((Test-VlcDir $portableDir)) {
    $resolved = (Resolve-Path $portableDir).Path
    Write-Host "[prepare-vlc] 源：仓库便携版（$resolved）"
}
elseif (Test-Path $portableZip) {
    Write-Host "[prepare-vlc] 源：仓库便携 zip（$portableZip）"
    $resolved = $portableZip
}
else {
    Write-Warning "[prepare-vlc] 未找到 VLC 源（MUSES_VLC_DIR / MUSES_VLC_ZIP / spike-vlcj 均不可用）：跳过内置运行时，发行版需用户自装 VLC 桌面版"
    exit 0
}

if ($resolved -like '*.zip') {
    if (Test-VlcDir $Staging) {
        Write-Host "[prepare-vlc] 复用已解压 staging：$Staging"
    }
    else {
        Write-Host "[prepare-vlc] 解压到 staging：$Staging"
        if (Test-Path $Staging) { Remove-Item -LiteralPath $Staging -Recurse -Force }
        New-Item -ItemType Directory -Path $Staging -Force | Out-Null
        # Expand-Archive 在 PS 5.1 对 >2GB/大量文件较慢但可用；VLC 包约 43MB
        Expand-Archive -LiteralPath $resolved -DestinationPath $Staging -Force
        # zip 顶层是 vlc-<ver>/：去掉这层，统一成「含 libvlc.dll 的目录」
        $inner = Get-ChildItem -LiteralPath $Staging -Directory | Select-Object -First 1
        if ($inner -and -not (Test-VlcDir $Staging)) {
            Get-ChildItem -LiteralPath $inner.FullName -Force | Move-Item -Destination $Staging -Force
            Remove-Item -LiteralPath $inner.FullName -Recurse -Force
        }
    }
    $sourceDir = $Staging
}
else {
    $sourceDir = $resolved
}

if (-not (Test-VlcDir $sourceDir)) {
    throw "[prepare-vlc] 源目录不含 libvlc.dll：$sourceDir"
}
if (-not (Test-Path $trimScript)) {
    throw "[prepare-vlc] 裁剪脚本缺失：$trimScript"
}

& $trimScript -Source $sourceDir -Destination $Destination
if (-not (Test-VlcDir $Destination)) {
    throw "[prepare-vlc] 裁剪产出异常（未见 libvlc.dll）：$Destination"
}
Write-Host "[prepare-vlc] 内置 VLC 就绪：$Destination"
