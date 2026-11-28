function Start-HostedWatcherProcess {
  [CmdletBinding()]
  param(
    [Parameter(Mandatory = $true)][string]$FilePath,
    [Parameter(Mandatory = $true)][string]$ArgumentList,
    [Parameter(Mandatory = $true)][string]$WorkingDirectory,
    [Parameter(Mandatory = $true)][string]$LogFile,
    [Parameter(Mandatory = $true)][string]$ReadyPattern,
    [ValidateRange(1, 120)][int]$StartupTimeoutSeconds = 30
  )

  $logDirectory = Split-Path -Parent $LogFile
  New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
  $stdoutFile = Join-Path $logDirectory 'hosted-windows-dialog-watcher.stdout.log'
  $stderrFile = Join-Path $logDirectory 'hosted-windows-dialog-watcher.stderr.log'
  $diagnosticFile = Join-Path $logDirectory 'hosted-windows-dialog-watcher.startup.txt'
  foreach ($path in @($LogFile, $stdoutFile, $stderrFile, $diagnosticFile)) {
    Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
  }

  $process = $null
  $timer = [System.Diagnostics.Stopwatch]::StartNew()
  $startError = $null
  try {
    $process = Start-Process -FilePath $FilePath -ArgumentList $ArgumentList `
      -WorkingDirectory $WorkingDirectory -PassThru -WindowStyle Hidden `
      -RedirectStandardOutput $stdoutFile -RedirectStandardError $stderrFile
  } catch {
    $startError = $_.Exception.Message
  }

  $ready = $false
  if ($null -ne $process) {
    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
      $process.Refresh()
      if (Test-Path -LiteralPath $LogFile -PathType Leaf) {
        $logText = Get-Content -LiteralPath $LogFile -Raw -ErrorAction SilentlyContinue
        if ($logText -match $ReadyPattern) {
          $process.Refresh()
          if (-not $process.HasExited) { $ready = $true }
          break
        }
      }
      if ($process.HasExited) { break }
      Start-Sleep -Milliseconds 100
    }
  }
  $timer.Stop()

  if ($ready) {
    return [pscustomobject]@{
      Process = $process
      LogFile = $LogFile
      StdoutFile = $stdoutFile
      StderrFile = $stderrFile
      DiagnosticFile = $diagnosticFile
      ElapsedMilliseconds = $timer.ElapsedMilliseconds
    }
  }

  $processState = 'not-started'
  if ($null -ne $process) {
    $process.Refresh()
    $processState = "pid=$($process.Id) hasExited=$($process.HasExited)"
    if ($process.HasExited) {
      $processState += " exitCode=$($process.ExitCode)"
    } else {
      & taskkill.exe /PID $process.Id /T /F 2>&1 | Out-Null
      $stopped = $process.WaitForExit(5000)
      $process.Refresh()
      $processState += " stoppedAfterTimeout hasExited=$($process.HasExited) waitSucceeded=$stopped"
      if ($process.HasExited) { $processState += " exitCode=$($process.ExitCode)" }
    }
  }
  $stdoutText = if (Test-Path -LiteralPath $stdoutFile) { Get-Content -LiteralPath $stdoutFile -Raw -ErrorAction SilentlyContinue } else { '(not created)' }
  $stderrText = if (Test-Path -LiteralPath $stderrFile) { Get-Content -LiteralPath $stderrFile -Raw -ErrorAction SilentlyContinue } else { '(not created)' }
  $watcherLogText = if (Test-Path -LiteralPath $LogFile) { Get-Content -LiteralPath $LogFile -Tail 40 -ErrorAction SilentlyContinue } else { '(not created)' }
  $diagnostic = @"
startupSucceeded=false
filePath=$FilePath
workingDirectory=$WorkingDirectory
elapsedMilliseconds=$($timer.ElapsedMilliseconds)
startupTimeoutSeconds=$StartupTimeoutSeconds
processState=$processState
startError=$startError
stdoutFile=$stdoutFile
stderrFile=$stderrFile
watcherLogFile=$LogFile
stdout:
$stdoutText
stderr:
$stderrText
watcherLogTail:
$watcherLogText
"@
  [System.IO.File]::WriteAllText($diagnosticFile, $diagnostic, [System.Text.UTF8Encoding]::new($false))
  throw "Hosted watcher did not become ready within $StartupTimeoutSeconds seconds. Diagnostics: $diagnosticFile`n$diagnostic"
}
