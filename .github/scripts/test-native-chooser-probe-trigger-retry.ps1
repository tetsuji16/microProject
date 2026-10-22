$ErrorActionPreference = 'Stop'

$monitorScript = Join-Path $PSScriptRoot 'capture-gui-desktop-state.ps1'
$source = Get-Content -LiteralPath $monitorScript -Raw
$tokens = $null
$parseErrors = $null
$ast = [System.Management.Automation.Language.Parser]::ParseInput($source, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -gt 0) {
  throw "Desktop monitor script has parse errors: $($parseErrors | Out-String)"
}
$functionAst = $ast.Find({
  param($node)
  $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
    $node.Name -eq 'Read-NativeChooserProbeTrigger'
}, $true)
if ($null -eq $functionAst) {
  throw 'Native chooser trigger reader was not found.'
}
Invoke-Expression $functionAst.Extent.Text

$triggerFile = Join-Path ([System.IO.Path]::GetTempPath()) ("native-chooser-trigger-{0}.txt" -f [Guid]::NewGuid().ToString('N'))
$unlockJob = $null
try {
  [System.IO.File]::WriteAllText($triggerFile, 'deadlineUtcTicks=123')
  $lockReadyFile = "$triggerFile.locked"
  $unlockJob = Start-Job -ArgumentList $triggerFile, $lockReadyFile -ScriptBlock {
    param($path, $readyPath)
    $stream = [System.IO.FileStream]::new($path, [System.IO.FileMode]::Open,
      [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    [System.IO.File]::WriteAllText($readyPath, 'locked')
    Start-Sleep -Milliseconds 125
    $stream.Dispose()
  }
  $lockDeadline = [DateTime]::UtcNow.AddSeconds(5)
  while (-not (Test-Path -LiteralPath $lockReadyFile) -and [DateTime]::UtcNow -lt $lockDeadline) {
    Start-Sleep -Milliseconds 10
  }
  if (-not (Test-Path -LiteralPath $lockReadyFile)) { throw 'Locking process did not acquire the trigger file.' }

  $actual = Read-NativeChooserProbeTrigger $triggerFile
  if ($actual -ne 'deadlineUtcTicks=123') {
    throw "Unexpected trigger content: $actual"
  }
  Receive-Job -Job $unlockJob -Wait | Out-Null
  if ($unlockJob.State -ne 'Completed') {
    throw "Trigger unlock job ended in state $($unlockJob.State)."
  }
  Write-Output 'PASS: trigger read succeeds after a transient exclusive file lock.'
} finally {
  if ($null -ne $unlockJob) {
    Receive-Job -Job $unlockJob -Wait -ErrorAction SilentlyContinue | Out-Null
    Remove-Job -Job $unlockJob -Force -ErrorAction SilentlyContinue
  }
  Remove-Item -LiteralPath $triggerFile -Force -ErrorAction SilentlyContinue
  Remove-Item -LiteralPath "$triggerFile.locked" -Force -ErrorAction SilentlyContinue
}
