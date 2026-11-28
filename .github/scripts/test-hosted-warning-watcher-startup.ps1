$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
. (Join-Path $PSScriptRoot 'hosted-watcher-startup.ps1')
$temporaryRoot = [System.IO.Path]::GetTempPath()
$testRoot = Join-Path $temporaryRoot ('microproject-hosted-watcher-' + [Guid]::NewGuid().ToString('N'))
if (-not [System.IO.Path]::GetFullPath($testRoot).StartsWith([System.IO.Path]::GetFullPath($temporaryRoot), [StringComparison]::OrdinalIgnoreCase)) {
  throw "Watcher test workspace escaped the temp directory: $testRoot"
}
New-Item -ItemType Directory -Path $testRoot | Out-Null
$powerShellExe = Join-Path $PSHOME 'pwsh.exe'
$process = $null

function Write-TestScript([string]$Path, [string]$Body) {
  [System.IO.File]::WriteAllText($Path, $Body, [System.Text.UTF8Encoding]::new($false))
}

function Invoke-TestWatcher([string]$ScriptPath, [string]$ReadyLog, [int]$TimeoutSeconds) {
  $arguments = '-NoProfile -NonInteractive -File "{0}"' -f $ScriptPath
  Start-HostedWatcherProcess -FilePath $powerShellExe -ArgumentList $arguments `
    -WorkingDirectory $repositoryRoot -LogFile $ReadyLog -ReadyPattern 'hosted test watcher ready' `
    -StartupTimeoutSeconds $TimeoutSeconds
}

try {
  $readyLog = Join-Path $testRoot 'delayed-watcher.log'
  $delayedScript = Join-Path $testRoot 'delayed-watcher.ps1'
  $readyLogLiteral = $readyLog.Replace("'", "''")
  Write-TestScript $delayedScript "Start-Sleep -Milliseconds 1200`nAdd-Content -LiteralPath '$readyLogLiteral' -Value 'hosted test watcher ready'`nStart-Sleep -Seconds 60`n"
  $startup = Invoke-TestWatcher $delayedScript $readyLog 30
  $process = $startup.Process
  if ($startup.ElapsedMilliseconds -lt 1000) { throw "Delayed watcher returned too early: $($startup.ElapsedMilliseconds)ms" }
  if (-not (Test-Path -LiteralPath $startup.StdoutFile) -or -not (Test-Path -LiteralPath $startup.StderrFile)) {
    throw 'Successful watcher startup did not create stdout and stderr evidence files.'
  }
  if ($process.HasExited) { throw 'Watcher exited immediately after reporting readiness.' }
  Stop-Process -Id $process.Id -Force
  if (-not $process.WaitForExit(5000)) { throw 'Successful watcher did not stop during cleanup.' }
  $process = $null
  Write-Host "PASS: delayed watcher became ready and stopped cleanly after $($startup.ElapsedMilliseconds)ms."

  $failedScript = Join-Path $testRoot 'failed-watcher.ps1'
  $failedLog = Join-Path $testRoot 'failed-watcher.log'
  $failedBody = "[Console]::Out.WriteLine('watcher stdout marker')`n[Console]::Error.WriteLine('watcher stderr marker')`nexit 17`n"
  Write-TestScript $failedScript $failedBody
  $failureText = ''
  try {
    [void](Invoke-TestWatcher $failedScript $failedLog 5)
    throw 'An immediately failing watcher unexpectedly passed startup.'
  } catch {
    $failureText = $_.Exception.Message
  }
  foreach ($required in @('exitCode=17', 'watcher stdout marker', 'watcher stderr marker', 'elapsedMilliseconds=', 'watcherLogTail:')) {
    if ($failureText -notmatch [regex]::Escape($required)) { throw "Immediate failure diagnostics omitted '$required': $failureText" }
  }
  Write-Host 'PASS: immediate watcher failure preserves PID, exit code, stdout, stderr, elapsed time, and watcher log state.'

  $timeoutScript = Join-Path $testRoot 'timeout-watcher.ps1'
  $timeoutLog = Join-Path $testRoot 'timeout-watcher.log'
  Write-TestScript $timeoutScript "Start-Sleep -Seconds 30`n"
  $timeoutText = ''
  try {
    [void](Invoke-TestWatcher $timeoutScript $timeoutLog 1)
    throw 'A watcher that never reports readiness unexpectedly passed startup.'
  } catch {
    $timeoutText = $_.Exception.Message
  }
  foreach ($required in @('within 1 seconds', 'pid=', 'stoppedAfterTimeout', 'stdout:', 'stderr:')) {
    if ($timeoutText -notmatch [regex]::Escape($required)) { throw "Timeout diagnostics omitted '$required': $timeoutText" }
  }
  Write-Host 'PASS: watcher timeout is bounded, child is stopped, and diagnostic evidence is retained.'
} finally {
  if ($null -ne $process -and -not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    [void]$process.WaitForExit(5000)
  }
  $resolvedRoot = (Resolve-Path -LiteralPath $testRoot).Path
  if (-not $resolvedRoot.StartsWith([System.IO.Path]::GetFullPath($temporaryRoot), [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove watcher test workspace outside temp: $resolvedRoot"
  }
  Remove-Item -LiteralPath $resolvedRoot -Recurse -Force
}
