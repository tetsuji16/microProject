$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$temporaryRoot = [System.IO.Path]::GetTempPath()
$workspace = Join-Path $temporaryRoot ('microproject-issue967-' + [Guid]::NewGuid().ToString('N'))
$resolvedTemporaryRoot = [System.IO.Path]::GetFullPath($temporaryRoot)
$resolvedWorkspace = [System.IO.Path]::GetFullPath($workspace)
if (-not $resolvedWorkspace.StartsWith($resolvedTemporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "Issue #967 workspace escaped temp: $resolvedWorkspace"
}
New-Item -ItemType Directory -Path $resolvedWorkspace | Out-Null
$env:MICROPROJECT_ISSUE967_WORKSPACE = $resolvedWorkspace
$runner = Join-Path $repositoryRoot '.github/scripts/run-gui-acceptance.ps1'
$testClass = 'com.microproject.pm.graphic.frames.FreshProcessMpoRestartGuiAcceptanceTest'
$success = $false
Push-Location $repositoryRoot
try {
  foreach ($phase in @('phaseOneOpensPodInTheUiAddsARowAndSavesAsMpo', 'phaseTwoStartsInANewJvmAndEditsTheSavedMpo')) {
    Write-Host "Running issue #967 phase: $phase"
    & $runner -Suite full -TestFilter "$testClass.$phase" -Locale ja -Scale 1.0
    $marker = Join-Path $resolvedWorkspace ($phase -like 'phaseOne*' ? 'phase-one.complete' : 'phase-two.complete')
    if (-not (Test-Path -LiteralPath $marker -PathType Leaf)) {
      throw "GUI runner returned without executing and completing $phase. Workspace: $resolvedWorkspace"
    }
    if ($phase -like 'phaseOne*') {
      foreach ($required in @('fresh-process.mpo', 'expected-rows.txt', 'phase-one.pid')) {
        if (-not (Test-Path -LiteralPath (Join-Path $resolvedWorkspace $required) -PathType Leaf)) {
          throw "Phase one omitted required evidence $required. Workspace: $resolvedWorkspace"
        }
      }
    }
  }
  $success = $true
} finally {
  Pop-Location
  Remove-Item Env:MICROPROJECT_ISSUE967_WORKSPACE -ErrorAction SilentlyContinue
  if ($success) {
    $confirmedWorkspace = (Resolve-Path -LiteralPath $resolvedWorkspace).Path
    if (-not $confirmedWorkspace.StartsWith($resolvedTemporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
      throw "Refusing to remove workspace outside temp: $confirmedWorkspace"
    }
    Remove-Item -LiteralPath $confirmedWorkspace -Recurse -Force
  } else {
    $evidenceRoot = Join-Path $repositoryRoot 'modules/microproject_ui/build/reports/guiTest-artifacts'
    $evidenceTarget = Join-Path $evidenceRoot ('issue-967-fresh-process-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $evidenceTarget -Force | Out-Null
    Copy-Item -LiteralPath $resolvedWorkspace -Destination (Join-Path $evidenceTarget 'workspace') -Recurse
    Write-Host "Preserved diagnostic workspace: $resolvedWorkspace"
    Write-Host "Failure evidence for GUI workflow upload: $evidenceTarget"
  }
}
