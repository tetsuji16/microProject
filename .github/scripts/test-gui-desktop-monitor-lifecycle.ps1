$ErrorActionPreference = 'Stop'

$monitorScript = Join-Path $PSScriptRoot 'capture-gui-desktop-state.ps1'
$tempDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("microproject-gui-monitor-" + [Guid]::NewGuid().ToString('N'))
$logFile = Join-Path $tempDirectory 'monitor.log'
$targetProcess = $null
$monitorProcess = $null
$targetExitObservedUtc = $null
$monitorWaitStartedUtc = $null
$preserveArtifacts = $false

New-Item -ItemType Directory -Path $tempDirectory | Out-Null
try {
  $powerShellExe = (Get-Process -Id $PID).Path
  $targetProcess = Start-Process -FilePath $powerShellExe `
    -ArgumentList '-NoProfile -NonInteractive -Command', '"Start-Sleep -Seconds 2"' `
    -PassThru -WindowStyle Hidden
  $monitorArguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -TargetPid {1} -Watch -LogFile "{2}"' -f `
    $monitorScript, $targetProcess.Id, $logFile
  $monitorProcess = Start-Process -FilePath $powerShellExe -ArgumentList $monitorArguments `
    -PassThru -WindowStyle Hidden
  if (-not $targetProcess.WaitForExit(10000)) {
    throw 'The short-lived monitor target did not exit within 10 seconds.'
  }
  $targetExitObservedUtc = [DateTime]::UtcNow.ToString('o')
  $monitorWaitStartedUtc = [DateTime]::UtcNow.ToString('o')
  if (-not $monitorProcess.WaitForExit(10000)) {
    throw 'The desktop monitor did not stop within 10 seconds after its target exited.'
  }
  if ($monitorProcess.ExitCode -ne 0) {
    throw "The desktop monitor exited with code $($monitorProcess.ExitCode)."
  }

  $logText = Get-Content -LiteralPath $logFile -Raw
  if ($logText -notmatch '(?m)^monitorStarted=') {
    throw 'The desktop monitor did not write its start record.'
  }
  if ($logText -notmatch '(?m)^monitorStopped=.*reason=target-exited\r?$') {
    throw "The desktop monitor did not record target-process shutdown. Log: $logText"
  }

  $exclusiveStream = [System.IO.FileStream]::new($logFile, [System.IO.FileMode]::Open,
    [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
  $exclusiveStream.Dispose()
  Write-Output 'PASS: desktop monitor exits after its target and releases the log file.'
} catch {
  $preserveArtifacts = $true
  $monitorState = 'unavailable'
  $monitorCpu = 'unavailable'
  if ($null -ne $monitorProcess) {
    try {
      $monitorProcess.Refresh()
      $monitorState = "pid=$($monitorProcess.Id) hasExited=$($monitorProcess.HasExited)"
      $monitorCpu = $monitorProcess.TotalProcessorTime.ToString()
    } catch {
      $monitorState = "pid=$($monitorProcess.Id) state-unavailable=$($_.Exception.Message)"
    }
  }
  $logTail = '(monitor log not created)'
  if (Test-Path -LiteralPath $logFile) {
    try {
      $logTail = (Get-Content -LiteralPath $logFile -Tail 40 -ErrorAction Stop) -join [Environment]::NewLine
    } catch {
      $logTail = "(unable to read monitor log: $($_.Exception.Message))"
    }
  }
  $diagnostic = @"
Desktop monitor lifecycle failure diagnostics:
  targetExitObservedUtc=$targetExitObservedUtc
  monitorWaitStartedUtc=$monitorWaitStartedUtc
  monitor=$monitorState cpu=$monitorCpu
  tempDirectory=$tempDirectory
  monitorLogTail:
$logTail
"@
  Write-Error -Message $diagnostic -ErrorAction Continue
  throw
} finally {
  if ($null -ne $monitorProcess -and -not $monitorProcess.HasExited) {
    Stop-Process -Id $monitorProcess.Id -Force -ErrorAction SilentlyContinue
  }
  if ($null -ne $targetProcess -and -not $targetProcess.HasExited) {
    Stop-Process -Id $targetProcess.Id -Force -ErrorAction SilentlyContinue
  }
  if (-not $preserveArtifacts) {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
  }
}
