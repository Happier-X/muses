#Requires -Version 5.1
<#
.SYNOPSIS
  Install Muses debug APK to MuMu emulator (default 127.0.0.1:7555) as an
  in-place UPDATE. Never uninstalls, so app data is preserved.
.DESCRIPTION
  Local builds default to versionCode=1. If the device already has a higher
  versionCode installed, a plain install fails with
  INSTALL_FAILED_VERSION_DOWNGRADE. This script reads the installed
  versionCode first and injects versionCode=installed+1 at build time, so
  every install is an upgrade and user data survives.
  Rule: update-install only. Uninstall requires explicit human approval.
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/install-mumu.ps1
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/install-mumu.ps1 -Serial 21463ba3 -SkipLaunch
#>
param(
  [string]$Serial = "127.0.0.1:7555",
  [string]$Package = "com.muses.player",
  [switch]$SkipLaunch
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

$escaped = [regex]::Escape($Serial)
$online = adb devices | Where-Object { $_ -match "^$escaped\s+device\s*$" }
if (-not $online) { throw "Device $Serial is not connected (see: adb devices)" }

$installedVc = $null
# 注意：adb 输出是字符串数组，必须先 -join 成单串再 -match，否则 $Matches 不会被填充
$dump = ((adb -s $Serial shell dumpsys package $Package 2>$null) -join "`n")
if ($dump -match "versionCode=(\d+)") { $installedVc = [int]$Matches[1] }

$gradleArgs = @(":app:installMusesDebug", "--offline")
if ($installedVc -ne $null) {
  $nextVc = $installedVc + 1
  Write-Host "[install-mumu] installed versionCode=$installedVc -> injecting versionCode=$nextVc (upgrade, data kept)"
  $gradleArgs += "-Pandroid.injected.versionCode=$nextVc"
} else {
  Write-Host "[install-mumu] $Package not installed on $Serial -> fresh install"
}

$env:ANDROID_SERIAL = $Serial
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { throw "gradle install failed (exit=$LASTEXITCODE)" }

if (-not $SkipLaunch) {
  adb -s $Serial shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
  Write-Host "[install-mumu] launched $Package on $Serial"
}
