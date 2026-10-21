$ErrorActionPreference = 'Stop'

$monitorScript = Join-Path $PSScriptRoot 'capture-gui-desktop-state.ps1'
$tempDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("microproject-gui-monitor-" + [Guid]::NewGuid().ToString('N'))
$logFile = Join-Path $tempDirectory 'monitor.log'
$diagnosticLogFile = Join-Path $tempDirectory 'monitor.diagnostic.log'
$targetProcess = $null
$monitorProcess = $null
$monitorReadyObservedUtc = $null
$targetExitObservedUtc = $null
$monitorWaitStartedUtc = $null
$preserveArtifacts = $false

New-Item -ItemType Directory -Path $tempDirectory | Out-Null
try {
  $powerShellExe = (Get-Process -Id $PID).Path
  $targetProcess = Start-Process -FilePath $powerShellExe `
    -ArgumentList '-NoProfile -NonInteractive -Command', '"Start-Sleep -Seconds 60"' `
    -PassThru -WindowStyle Hidden
  $monitorArguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -TargetPid {1} -Watch -LogFile "{2}" -DiagnosticLogFile "{3}"' -f `
    $monitorScript, $targetProcess.Id, $logFile, $diagnosticLogFile
  $monitorProcess = Start-Process -FilePath $powerShellExe -ArgumentList $monitorArguments `
    -PassThru -WindowStyle Hidden

  $startupDeadlineUtc = [DateTime]::UtcNow.AddSeconds(30)
  $monitorReady = $false
  while ([DateTime]::UtcNow -lt $startupDeadlineUtc) {
    $targetProcess.Refresh()
    if ($targetProcess.HasExited) {
      throw 'The dedicated monitor target exited before monitor startup was observed.'
    }
    $monitorProcess.Refresh()
    if ($monitorProcess.HasExited) {
      throw "The desktop monitor exited before startup was observed (exit code $($monitorProcess.ExitCode))."
    }
    if (Test-Path -LiteralPath $diagnosticLogFile) {
      $diagnosticLogText = Get-Content -LiteralPath $diagnosticLogFile -Raw
      if ($diagnosticLogText -match '(?m)^monitorStarted=.*monitorPid=\d+ targetPid=\d+') {
        $monitorReady = $true
        $monitorReadyObservedUtc = [DateTime]::UtcNow.ToString('o')
        break
      }
    }
    Start-Sleep -Milliseconds 50
  }
  if (-not $monitorReady) {
    throw 'The desktop monitor did not report startup within 30 seconds.'
  }

  $targetProcess.Refresh()
  if ($targetProcess.HasExited) {
    throw 'The dedicated monitor target exited before it could be stopped after monitor startup.'
  }
  Stop-Process -Id $targetProcess.Id -Force
  if (-not $targetProcess.WaitForExit(10000)) {
    throw 'The dedicated monitor target did not exit within 10 seconds after it was stopped.'
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
  if (-not [string]::IsNullOrWhiteSpace($logText)) {
    throw "The desktop monitor wrote a non-overlap record to the overlap event log. Log: $logText"
  }
  $diagnosticLogText = Get-Content -LiteralPath $diagnosticLogFile -Raw
  if ($diagnosticLogText -notmatch '(?m)^monitorStarted=.*monitorPid=\d+ targetPid=\d+') {
    throw 'The desktop monitor did not write its process identity to the diagnostic log.'
  }
  if ($diagnosticLogText -notmatch '(?m)^monitorStopped=.*reason=target-exited\r?$') {
    throw "The desktop monitor did not record target-process shutdown. Diagnostic log: $diagnosticLogText"
  }
  if ($diagnosticLogText -notmatch '(?m)^monitorFirstTargetCheckExited=') {
    throw 'The desktop monitor did not write its first target-check diagnostic.'
  }

  foreach ($releasedLog in @($logFile, $diagnosticLogFile)) {
    $exclusiveStream = [System.IO.FileStream]::new($releasedLog, [System.IO.FileMode]::Open,
      [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
    $exclusiveStream.Dispose()
  }
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
  $diagnosticLogTail = '(monitor diagnostic log not created)'
  if (Test-Path -LiteralPath $diagnosticLogFile) {
    try {
      $diagnosticLogTail = (Get-Content -LiteralPath $diagnosticLogFile -Tail 40 -ErrorAction Stop) -join [Environment]::NewLine
    } catch {
      $diagnosticLogTail = "(unable to read monitor diagnostic log: $($_.Exception.Message))"
    }
  }
  $diagnostic = @"
Desktop monitor lifecycle failure diagnostics:
  monitorReadyObservedUtc=$monitorReadyObservedUtc
  targetExitObservedUtc=$targetExitObservedUtc
  monitorWaitStartedUtc=$monitorWaitStartedUtc
  monitor=$monitorState cpu=$monitorCpu
  tempDirectory=$tempDirectory
  monitorLogTail:
$logTail
  monitorDiagnosticLogTail:
$diagnosticLogTail
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
