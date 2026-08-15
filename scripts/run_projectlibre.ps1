# ProjectLibre incremental launcher
#
# Preferred one-step verification flow:
#   .\scripts\run_micrproject_clean.bat
#
# Use this script when you want to reuse an existing installDist output or
# refresh it without switching to the clean build wrapper.
#
# Usage:
#   .\scripts\run_projectlibre.ps1
#   .\scripts\run_projectlibre.ps1 -SkipBuild
#   .\scripts\run_projectlibre.ps1 -Clean

[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$Clean,
    [string]$LogRoot = ".\build\logs\projectlibre"
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$launcherPath = Join-Path $projectRoot "modules\micrproject_ui\build\install\micrproject_ui\bin\micrproject_ui.bat"
$gradlePath = Join-Path $projectRoot "gradlew.bat"
$resolvedLogRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $LogRoot))
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionLogDir = Join-Path $resolvedLogRoot $timestamp
$latestLogDir = Join-Path $resolvedLogRoot "latest"
$launcherLog = Join-Path $sessionLogDir "launcher.log"
$appStdoutLog = Join-Path $sessionLogDir "app.stdout.log"
$appStderrLog = Join-Path $sessionLogDir "app.stderr.log"

New-Item -ItemType Directory -Force -Path $sessionLogDir | Out-Null

function Write-Status {
    param(
        [string]$Message,
        [string]$Color = "Gray"
    )

    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Write-Host $line -ForegroundColor $Color
    Add-Content -LiteralPath $launcherLog -Value $line
}

function Get-ProjectLibreJavaProcesses {
    $javaProcessNames = @("java", "javaw")
    $javaProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object { $_.CommandLine -match "projectlibre" }

    foreach ($proc in $javaProcesses) {
        $psProc = Get-Process -Id $proc.ProcessId -ErrorAction SilentlyContinue
        if ($psProc -and $javaProcessNames -contains $psProc.ProcessName) {
            [PSCustomObject]@{
                Id = $psProc.Id
                Name = $psProc.ProcessName
                MainWindowHandle = $psProc.MainWindowHandle
                MainWindowTitle = $psProc.MainWindowTitle
                CommandLine = $proc.CommandLine
            }
        }
    }
}

function Stop-ExistingProjectLibre {
    $running = @(Get-ProjectLibreJavaProcesses)
    if (-not $running) {
        return
    }

    Write-Status "Stopping existing ProjectLibre Java processes: $($running.Id -join ', ')" "Yellow"
    foreach ($proc in $running) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
}

function Invoke-Build {
    $gradleArgs = @()
    if ($Clean) {
        $gradleArgs += "clean"
        $gradleArgs += "cleanLegacyPackagingArtifacts"
    }
    $gradleArgs += ":micrproject_ui:installDist"
    $gradleArgs += "--console=plain"

    Write-Status "Refreshing installed app layout with Gradle: $($gradleArgs -join ' ')" "Cyan"
    & $gradlePath @gradleArgs 2>&1 | Tee-Object -FilePath $launcherLog -Append
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle installDist failed with exit code $LASTEXITCODE."
    }
}

function Update-LatestLogs {
    if (Test-Path -LiteralPath $latestLogDir) {
        Remove-Item -LiteralPath $latestLogDir -Recurse -Force
    }
    Copy-Item -LiteralPath $sessionLogDir -Destination $latestLogDir -Recurse
}

Write-Status "Project root: $projectRoot"
Write-Status "Session logs: $sessionLogDir"

if (-not (Test-Path -LiteralPath $gradlePath)) {
    throw "Could not find Gradle wrapper at $gradlePath"
}

if (-not $SkipBuild) {
    Invoke-Build
} elseif (-not (Test-Path -LiteralPath $launcherPath)) {
    Write-Status "Installed app layout is missing, so a build refresh is required." "Yellow"
    Invoke-Build
} else {
    Write-Status "Skipping build refresh and using the existing installDist output." "Yellow"
}

if (-not (Test-Path -LiteralPath $launcherPath)) {
    throw "Launcher not found after build: $launcherPath"
}

Stop-ExistingProjectLibre

Write-Status "Launching ProjectLibre from installDist." "Cyan"
$process = Start-Process `
    -FilePath $launcherPath `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $appStdoutLog `
    -RedirectStandardError $appStderrLog `
    -PassThru

Start-Sleep -Seconds 6

$running = @(Get-ProjectLibreJavaProcesses)
if (-not $running) {
    Update-LatestLogs
    Write-Status "ProjectLibre did not appear to start. Check logs in $sessionLogDir" "Red"
    throw "ProjectLibre launch could not be verified."
}

$windowed = $running | Where-Object { $_.MainWindowHandle -ne 0 -or $_.MainWindowTitle }
if ($windowed) {
    Write-Status "ProjectLibre is running. Window title: $($windowed[0].MainWindowTitle)" "Green"
} else {
    Write-Status "ProjectLibre Java process is running, but no window title was detected yet." "Yellow"
}

Write-Status "Launcher PID: $($process.Id)"
Write-Status "App stdout log: $appStdoutLog"
Write-Status "App stderr log: $appStderrLog"

Update-LatestLogs
