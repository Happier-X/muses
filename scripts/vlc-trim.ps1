# 按 scripts/vlc-trim-keep.txt 白名单裁剪 VLC 便携目录（纯音频播放最小集）。
# 用法：
#   pwsh -File scripts/vlc-trim.ps1 -Source <VLC 便携目录> -Destination <输出目录>
# 幂等：每次先清空 Destination 再重建。
param(
    [Parameter(Mandatory = $true)][string]$Source,
    [Parameter(Mandatory = $true)][string]$Destination,
    [string]$KeepList = (Join-Path $PSScriptRoot 'vlc-trim-keep.txt')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path (Join-Path $Source 'libvlc.dll'))) {
    throw "源目录不含 libvlc.dll：$Source"
}
if (-not (Test-Path $KeepList)) {
    throw "白名单文件不存在：$KeepList"
}

# 解析白名单（[root]/[dir]/[file] 三段）
$section = ''
$rootFiles = @()
$keepDirs = @()
$keepFiles = @()
# -Encoding UTF8：Windows PowerShell 5.1 下无 BOM 的 UTF-8 默认按 ANSI 解码，中文注释会乱码
foreach ($raw in Get-Content -LiteralPath $KeepList -Encoding UTF8) {
    $line = $raw.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { continue }
    if ($line -match '^\[(.+)\]$') { $section = $Matches[1].ToLowerInvariant(); continue }
    switch ($section) {
        'root' { $rootFiles += $line }
        'dir' { $keepDirs += $line }
        'file' { $keepFiles += $line }
        default { throw "白名单格式错误（未声明段落）：$line" }
    }
}

if (Test-Path $Destination) { Remove-Item -LiteralPath $Destination -Recurse -Force }
New-Item -ItemType Directory -Path $Destination -Force | Out-Null

$copied = 0

foreach ($f in $rootFiles) {
    $src = Join-Path $Source $f
    if (Test-Path $src) {
        Copy-Item -LiteralPath $src -Destination (Join-Path $Destination $f) -Force
        $copied++
    }
}

# plugins 目录：先按整目录保留，再按文件名补齐
$srcPlugins = Join-Path $Source 'plugins'
$dstPlugins = Join-Path $Destination 'plugins'
foreach ($d in $keepDirs) {
    $src = Join-Path $srcPlugins $d
    if (Test-Path $src) {
        Copy-Item -LiteralPath $src -Destination $dstPlugins -Recurse -Force
        $copied++
    }
    else {
        Write-Warning "白名单目录缺失（VLC 版本差异？）：plugins/$d"
    }
}

foreach ($entry in $keepFiles) {
    $src = Join-Path $srcPlugins ($entry -replace '/', [IO.Path]::DirectorySeparatorChar)
    if (Test-Path $src) {
        $dstDir = Join-Path $dstPlugins (Split-Path $entry -Parent)
        New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
        Copy-Item -LiteralPath $src -Destination $dstDir -Force
        $copied++
    }
    else {
        Write-Warning "白名单文件缺失（VLC 版本差异？）：plugins/$entry"
    }
}

$size = (Get-ChildItem -LiteralPath $Destination -Recurse -File | Measure-Object -Property Length -Sum).Sum
Write-Host ("[vlc-trim] 保留 $copied 项，输出 {0:N1} MB -> $Destination" -f ($size / 1MB))
