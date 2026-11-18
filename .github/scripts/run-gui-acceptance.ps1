param(
  [ValidateSet('smoke', 'journeys', 'full', 'visual')]
  [string]$Suite = 'smoke',
  [string]$TestFilter,
  [ValidateSet('ja', 'en')]
  [string]$Locale = 'ja',
  [ValidateSet('1.0', '1.25', '1.5')]
  [string]$Scale = '1.0',
  [switch]$StableReleaseGate
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
if (-not [string]::IsNullOrWhiteSpace($TestFilter) -and $TestFilter -notmatch '^com\.microproject(?:\.[A-Za-z_$][A-Za-z0-9_$]*){2,}$') {
  throw 'TestFilter must be a fully qualified com.microproject test class or method name containing only Java identifier segments.'
}
$gateLogs = 'modules/microproject_ui/build/reports/guiTest-artifacts'
$gateFailures = [System.Collections.Generic.List[string]]::new()
$hostedWarningWatcher = $null
$isGitHubActions = $env:GITHUB_ACTIONS -eq 'true'
$hostedPolicySnapshots = [System.Collections.Generic.List[object]]::new()
New-Item -ItemType Directory -Force -Path $gateLogs | Out-Null

$primaryDisplay = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
Write-Host "GUI runner primary display bounds: $($primaryDisplay.Width)x$($primaryDisplay.Height)"
if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
  Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value "`n- Observed primary display bounds: ``$($primaryDisplay.Width)x$($primaryDisplay.Height)`` (unchanged).`n`n| Gate leg | Tests | Failures | Errors | Skips |`n|---|---:|---:|---:|---:|"
}

function Start-HostedWarningWatcher {
  if ($null -ne $script:hostedWarningWatcher -and -not $script:hostedWarningWatcher.HasExited) { return }
  $watcherScript = Join-Path $PWD '.github/scripts/dismiss-hosted-windows-performance-dialogs.ps1'
  $watcherLog = Join-Path $script:gateLogs 'hosted-windows-dialog-watcher.log'
  $argumentLine = '-NoProfile -NonInteractive -File "{0}" -LogFile "{1}"' -f $watcherScript, $watcherLog
  $script:hostedWarningWatcher = Start-Process -FilePath (Join-Path $PSHOME 'pwsh.exe') `
    -ArgumentList $argumentLine -PassThru -WindowStyle Hidden
  Start-Sleep -Milliseconds 300
  if ($script:hostedWarningWatcher.HasExited) {
    throw 'Could not start the bounded hosted paging-file warning watcher.'
  }
  Write-Host "Started strict hosted paging-file warning watcher (PID $($script:hostedWarningWatcher.Id)); log=$watcherLog"
}

function Stop-HostedWarningWatcher {
  if ($null -eq $script:hostedWarningWatcher) { return }
  if (-not $script:hostedWarningWatcher.HasExited) {
    Stop-Process -Id $script:hostedWarningWatcher.Id -Force -ErrorAction SilentlyContinue
    [void]$script:hostedWarningWatcher.WaitForExit(5000)
  }
  $script:hostedWarningWatcher = $null
}

function Start-HostedPolicyValue([string]$path, [string]$name, [string]$description) {
  if (-not $script:isGitHubActions) { return }
  $createdKeyPaths = [System.Collections.Generic.List[string]]::new()
  $currentPath = ''
  foreach ($part in $path.Split('\')) {
    $currentPath = if ([string]::IsNullOrEmpty($currentPath)) { $part } else { "$currentPath\$part" }
    $probe = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($currentPath, $false)
    if ($null -eq $probe) {
      $createdKeyPaths.Add($currentPath)
    } else {
      $probe.Dispose()
    }
  }
  $snapshot = @{
    Path = $path
    Name = $name
    Description = $description
    KeyExisted = $createdKeyPaths.Count -eq 0
    CreatedKeyPaths = @($createdKeyPaths.ToArray())
    ValueStateCaptured = $false
    ValueExists = $false
    Value = $null
    Kind = $null
  }
  $script:hostedPolicySnapshots.Add($snapshot)
  $key = $null
  try {
    $key = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey($path)
    if ($null -eq $key) { throw "Could not create or open hosted GUI policy key: HKCU:\$path" }
    $exists = $key.GetValueNames() -contains $name
    $value = if ($exists) { $key.GetValue($name) } else { $null }
    $kind = if ($exists) { $key.GetValueKind($name) } else { $null }
    $snapshot.ValueExists = $exists
    $snapshot.Value = $value
    $snapshot.Kind = $kind
    $snapshot.ValueStateCaptured = $true
    if ($exists -and $kind -ne [Microsoft.Win32.RegistryValueKind]::DWord) { throw "Hosted GUI policy has unsupported existing value type: $kind" }
    $key.SetValue($name, 1, [Microsoft.Win32.RegistryValueKind]::DWord)
    $key.Flush()
    if ([int]$key.GetValue($name) -ne 1 -or $key.GetValueKind($name) -ne [Microsoft.Win32.RegistryValueKind]::DWord) {
      throw "Hosted GUI policy $name did not read back as DWORD 1"
    }
    Write-Host "Temporarily enabled $name for $description; the original value will be restored in finally."
    if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
      Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value "- Hosted GUI policy ``$name`` ($description): temporarily set and read back as DWORD 1; original value will be restored after the gate."
    }
  } catch {
    $setupError = $_.Exception.Message
    if ($null -ne $key) { $key.Dispose(); $key = $null }
    try {
      Stop-HostedPolicyValues
    } catch {
      $rollbackError = $_.Exception.Message
      throw "Hosted GUI policy setup failed ($setupError); rollback also failed ($rollbackError)"
    }
    throw
  } finally {
    if ($null -ne $key) { $key.Dispose() }
  }
}

function Start-HostedPolicies {
  Start-HostedPolicyValue 'Software\Policies\Microsoft\Windows\Explorer' 'DisableNotificationCenter' 'hosted GUI notification center'
  Start-HostedPolicyValue 'Software\Microsoft\Windows\CurrentVersion\Policies\Explorer' 'NoWinKeys' 'hosted GUI Windows-key hotkeys'
  Restart-HostedExplorerShell
}

function Initialize-HostedGuiHotkeyProbe {
  if ('HostedGuiHotkeyProbe' -as [type]) { return }
  Add-Type @'
using System;
using System.Runtime.InteropServices;

public static class HostedGuiHotkeyProbe {
  private const byte VkLWin = 0x5B;
  private const byte VkA = 0x41;
  private const byte VkEscape = 0x1B;
  private const uint KeyUp = 0x0002;

  [DllImport("user32.dll", CharSet = CharSet.Auto)]
  private static extern IntPtr FindWindow(string className, string windowName);

  [DllImport("user32.dll")]
  private static extern bool IsWindowVisible(IntPtr handle);

  [DllImport("user32.dll")]
  private static extern void keybd_event(byte virtualKey, byte scanCode, uint flags, UIntPtr extraInfo);

  public static bool IsQuickSettingsVisible() {
    IntPtr handle = FindWindow("ControlCenterWindow", null);
    return handle != IntPtr.Zero && IsWindowVisible(handle);
  }

  public static bool IsTaskbarVisible() {
    IntPtr handle = FindWindow("Shell_TrayWnd", null);
    return handle != IntPtr.Zero && IsWindowVisible(handle);
  }

  public static void PressWindowsA() {
    keybd_event(VkLWin, 0, 0, UIntPtr.Zero);
    try {
      keybd_event(VkA, 0, 0, UIntPtr.Zero);
      keybd_event(VkA, 0, KeyUp, UIntPtr.Zero);
    } finally {
      keybd_event(VkLWin, 0, KeyUp, UIntPtr.Zero);
    }
  }

  public static void PressEscape() {
    keybd_event(VkEscape, 0, 0, UIntPtr.Zero);
    keybd_event(VkEscape, 0, KeyUp, UIntPtr.Zero);
  }
}
'@
}

function Restart-HostedExplorerShell {
  if (-not $script:isGitHubActions) { return }
  Initialize-HostedGuiHotkeyProbe
  if ([HostedGuiHotkeyProbe]::IsQuickSettingsVisible()) {
    Save-GuiFailureScreenshot 'hosted-shell-restart-started-contended'
    throw 'Hosted GUI desktop was already showing Windows Quick Settings before the policy refresh; Explorer was not restarted.'
  }

  $existingExplorer = @(Get-Process -Name explorer -ErrorAction SilentlyContinue)
  if ($existingExplorer.Count -eq 0) { throw 'Hosted Explorer shell process was missing before the NoWinKeys policy refresh.' }
  $existingExplorerIds = @($existingExplorer | ForEach-Object { $_.Id })
  $existingExplorer | Stop-Process -Force
  $stopDeadline = [DateTime]::UtcNow.AddSeconds(5)
  while (@(Get-Process -Name explorer -ErrorAction SilentlyContinue | Where-Object { $_.Id -in $existingExplorerIds }).Count -gt 0 -and [DateTime]::UtcNow -lt $stopDeadline) {
    Start-Sleep -Milliseconds 100
  }
  if (@(Get-Process -Name explorer -ErrorAction SilentlyContinue | Where-Object { $_.Id -in $existingExplorerIds }).Count -gt 0) {
    throw 'Hosted Explorer shell did not stop for the NoWinKeys policy refresh.'
  }
  if (@(Get-Process -Name explorer -ErrorAction SilentlyContinue).Count -eq 0) {
    Start-Process -FilePath (Join-Path $env:SystemRoot 'explorer.exe')
  }
  $shellDeadline = [DateTime]::UtcNow.AddSeconds(15)
  while (-not [HostedGuiHotkeyProbe]::IsTaskbarVisible() -and [DateTime]::UtcNow -lt $shellDeadline) {
    Start-Sleep -Milliseconds 100
  }
  if (-not [HostedGuiHotkeyProbe]::IsTaskbarVisible()) {
    throw 'Hosted Explorer shell did not restore the taskbar after the NoWinKeys policy refresh.'
  }
  Write-Host 'Restarted the hosted Explorer shell so the temporary user policies take effect.'
}

function Test-HostedWindowsKeyHotkeysDisabled {
  if (-not $script:isGitHubActions) { return }
  Initialize-HostedGuiHotkeyProbe
  if ([HostedGuiHotkeyProbe]::IsQuickSettingsVisible()) {
    Save-GuiFailureScreenshot 'hosted-hotkey-preflight-started-contended'
    throw 'Hosted GUI desktop was already showing Windows Quick Settings before the Windows-key policy preflight.'
  }

  [HostedGuiHotkeyProbe]::PressWindowsA()
  Start-Sleep -Milliseconds 400
  if ([HostedGuiHotkeyProbe]::IsQuickSettingsVisible()) {
    Save-GuiFailureScreenshot 'hosted-no-win-keys-preflight'
    [HostedGuiHotkeyProbe]::PressEscape()
    $deadline = [DateTime]::UtcNow.AddSeconds(3)
    while ([HostedGuiHotkeyProbe]::IsQuickSettingsVisible() -and [DateTime]::UtcNow -lt $deadline) {
      Start-Sleep -Milliseconds 100
    }
    throw 'Hosted NoWinKeys policy did not block Win+A; Quick Settings opened during the preflight. The GUI suite was not started.'
  }

  Write-Host 'Hosted NoWinKeys policy preflight passed: Win+A did not open Quick Settings.'
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
    Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value '- Hosted NoWinKeys preflight passed: Win+A did not open the Windows Quick Settings window.'
  }
}

function Restore-HostedPolicyValue($snapshot) {
  $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($snapshot.Path, $true)
  if ($null -eq $key -and $snapshot.KeyExisted) { throw "Could not reopen hosted GUI policy key for restoration: HKCU:\$($snapshot.Path)" }
  try {
    if ($snapshot.ValueStateCaptured) {
      if ($null -ne $key) {
        if ($snapshot.ValueExists) {
          $key.SetValue($snapshot.Name, $snapshot.Value, $snapshot.Kind)
        } else {
          $key.DeleteValue($snapshot.Name, $false)
        }
        $key.Flush()
        $exists = $key.GetValueNames() -contains $snapshot.Name
        if ($snapshot.ValueExists) {
          if (-not $exists -or $key.GetValue($snapshot.Name) -ne $snapshot.Value -or $key.GetValueKind($snapshot.Name) -ne $snapshot.Kind) {
            throw "Original hosted GUI policy $($snapshot.Name) was not restored exactly"
          }
        } elseif ($exists) {
          throw "Temporary hosted GUI policy $($snapshot.Name) remains after restoration"
        }
      } elseif ($snapshot.ValueExists) {
        throw "Original hosted GUI policy key for $($snapshot.Name) is missing during restoration"
      }
    }

    if ($null -ne $key) {
      $key.Dispose()
      $key = $null
    }

    if (-not $snapshot.KeyExisted) {
      foreach ($createdPath in @($snapshot.CreatedKeyPaths | Sort-Object { $_.Length } -Descending)) {
        $createdKey = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($createdPath, $true)
        if ($null -eq $createdKey) { continue }
        try {
          if ($createdKey.GetValueNames().Count -gt 0 -or $createdKey.GetSubKeyNames().Count -gt 0) {
            throw "Refusing to remove temporary policy key containing unrelated data: HKCU:\$createdPath"
          }
        } finally {
          $createdKey.Dispose()
        }
        $separator = $createdPath.LastIndexOf('\')
        $parentPath = if ($separator -ge 0) { $createdPath.Substring(0, $separator) } else { '' }
        $childName = if ($separator -ge 0) { $createdPath.Substring($separator + 1) } else { $createdPath }
        $parentKey = if ([string]::IsNullOrEmpty($parentPath)) { [Microsoft.Win32.Registry]::CurrentUser } else { [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($parentPath, $true) }
        if ($null -eq $parentKey) { throw "Could not open temporary policy parent for cleanup: HKCU:\$parentPath" }
        try {
          $parentKey.DeleteSubKey($childName, $false)
        } finally {
          if (-not [string]::IsNullOrEmpty($parentPath)) { $parentKey.Dispose() }
        }
        $verifyCreatedKey = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($createdPath, $false)
        if ($null -ne $verifyCreatedKey) {
          $verifyCreatedKey.Dispose()
          throw "Temporary policy key remains after cleanup: HKCU:\$createdPath"
        }
      }
      $verifyOriginalPath = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($snapshot.Path, $false)
      if ($null -ne $verifyOriginalPath) {
        $verifyOriginalPath.Dispose()
        throw "Original absent hosted GUI policy key remains after restoration: HKCU:\$($snapshot.Path)"
      }
    }
    Write-Host "Restored and verified the original hosted GUI policy state for $($snapshot.Name)."
  } finally {
    if ($null -ne $key) { $key.Dispose() }
  }
}

function Stop-HostedPolicyValues {
  $restartShell = @($script:hostedPolicySnapshots | Where-Object { $_.Name -eq 'NoWinKeys' }).Count -gt 0
  for ($index = $script:hostedPolicySnapshots.Count - 1; $index -ge 0; $index--) {
    $snapshot = $script:hostedPolicySnapshots[$index]
    Restore-HostedPolicyValue $snapshot
    $script:hostedPolicySnapshots.RemoveAt($index)
    if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
      Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value "- Hosted GUI policy ``$($snapshot.Name)``: original value restored and verified (or temporary value verified absent)."
    }
  }
  if ($restartShell) { Restart-HostedExplorerShell }
}

function Complete-GuiAcceptanceCleanup {
  try {
    Stop-HostedPolicyValues
  } finally {
    Stop-HostedWarningWatcher
  }
}

function Save-GuiFailureScreenshot([string]$label) {
  try {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    $bounds = [System.Windows.Forms.SystemInformation]::VirtualScreen
    if ($bounds.Width -le 0 -or $bounds.Height -le 0) { return }
    $bitmap = [Drawing.Bitmap]::new($bounds.Width, $bounds.Height)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    try {
      $graphics.CopyFromScreen($bounds.Left, $bounds.Top, 0, 0, $bitmap.Size)
      $safe = ($label -replace '[^A-Za-z0-9_.-]', '_')
      $bitmap.Save((Join-Path $gateLogs "$safe.failure.png"), [Drawing.Imaging.ImageFormat]::Png)
    } finally {
      $graphics.Dispose()
      $bitmap.Dispose()
    }
  } catch {
    Write-Warning "Could not capture GUI failure screenshot: $_"
  }
}

function Minimize-HostedRunnerConsole {
  if (-not ('MicroProject.GuiTestDesktop' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;
namespace MicroProject {
  public static class GuiTestDesktop {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr window, StringBuilder text, int capacity);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr window, int command);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr window);
  }
}
'@
  }
  $foreground = [MicroProject.GuiTestDesktop]::GetForegroundWindow()
  if ($foreground -eq [IntPtr]::Zero) { return }
  $title = [System.Text.StringBuilder]::new(512)
  [void][MicroProject.GuiTestDesktop]::GetWindowText($foreground, $title, $title.Capacity)
  $windowTitle = $title.ToString()
  if ($windowTitle.IndexOf('C:\ProgramData\GitHub\Host', [StringComparison]::OrdinalIgnoreCase) -ge 0) {
    [void][MicroProject.GuiTestDesktop]::ShowWindow($foreground, 6) # SW_MINIMIZE
    Start-Sleep -Milliseconds 150
    if (-not [MicroProject.GuiTestDesktop]::IsIconic($foreground)) {
      throw "Could not minimize the known hosted-runner console before GUI tests: $windowTitle"
    }
    Write-Host "Minimized the hosted-runner console before GUI gate: $windowTitle"
  } else {
    Write-Host "Foreground window before GUI gate: $windowTitle"
  }
}

function Invoke-GuiGate([string]$label, [string[]]$arguments, [int]$timeoutSeconds) {
  if ($script:isGitHubActions) {
    Start-HostedWarningWatcher
    Minimize-HostedRunnerConsole
  }
  $safe = ($label -replace '[^A-Za-z0-9_.-]', '_')
  $attempt = [Guid]::NewGuid().ToString('N')
  $guiArtifacts = Join-Path $PWD "modules/microproject_ui/build/reports/guiTest-artifacts/$safe-$attempt"
  New-Item -ItemType Directory -Force -Path $guiArtifacts | Out-Null
  $contentionMarker = Join-Path $guiArtifacts 'environment-contended.marker'
  $gradleExitCodeFile = Join-Path $guiArtifacts 'gradle-exit-code.txt'
  $gradleInvocationFile = Join-Path $guiArtifacts 'gradle-invocation.json'
  $stdout = Join-Path $gateLogs "$safe.stdout.log"
  $stderr = Join-Path $gateLogs "$safe.stderr.log"
  $testResults = Join-Path $PWD 'modules/microproject_ui/build/test-results/guiTest'
  New-Item -ItemType Directory -Force -Path $testResults | Out-Null
  Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
  $gradleArguments = @($arguments + "-PguiTestArtifactsDir=$guiArtifacts" + "-PguiTestContentionMarker=$contentionMarker")
  $gradleInvocation = @{
    WorkingDirectory = $PWD.Path
    GradleWrapper = Join-Path $PWD 'gradlew.bat'
    Arguments = $gradleArguments
    ExitCodeFile = $gradleExitCodeFile
  } | ConvertTo-Json -Depth 4
  [System.IO.File]::WriteAllText($gradleInvocationFile, $gradleInvocation,
    [System.Text.UTF8Encoding]::new($false))
  $childScript = Join-Path $PSScriptRoot 'run-gui-gate-child.ps1'
  $childArguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -ConfigurationFile "{1}"' -f `
    $childScript, $gradleInvocationFile
  $windowsPowerShell = (Get-Command powershell.exe -ErrorAction Stop).Source
  $process = Start-Process -FilePath $windowsPowerShell `
    -ArgumentList $childArguments `
    -PassThru -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr -WindowStyle Hidden
  $timer = [System.Diagnostics.Stopwatch]::StartNew()
  if (-not $process.WaitForExit($timeoutSeconds * 1000)) {
    $timer.Stop()
    taskkill.exe /PID $process.Id /T /F | Out-Null
    Save-GuiFailureScreenshot $label
    if (Test-Path -LiteralPath $contentionMarker) {
      $markerText = Get-Content -LiteralPath $contentionMarker -Raw
      throw "GUI_ENVIRONMENT_CONTENDED: desktop interference was recorded and the GUI gate also timed out: $label marker=$markerText"
    }
    throw "GUI gate watchdog timed out: $label after $($timer.Elapsed.ToString('hh\:mm\:ss')) (limit $([TimeSpan]::FromSeconds($timeoutSeconds).ToString('hh\:mm\:ss'))); this is a gate timeout, not an assertion result."
  }
  $timer.Stop()
  Write-Host "GUI gate elapsed: $label $($timer.Elapsed.ToString('hh\:mm\:ss')) (limit $([TimeSpan]::FromSeconds($timeoutSeconds).ToString('hh\:mm\:ss')))"
  $stdoutText = Get-Content -LiteralPath $stdout -Raw
  $stderrText = Get-Content -LiteralPath $stderr -Raw
  $stdoutText
  $stderrText
  $evidenceDirectory = Join-Path $gateLogs "$safe-junit"
  New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
  $resultFiles = @(Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue)
  $testCount = 0
  $failureCount = 0
  $errorCount = 0
  $skipCount = 0
  foreach ($resultFile in $resultFiles) {
    Copy-Item -LiteralPath $resultFile.FullName -Destination $evidenceDirectory -Force
    [xml]$countsXml = Get-Content -LiteralPath $resultFile.FullName -Raw
    foreach ($suiteNode in @($countsXml.SelectNodes('//testsuite'))) {
      $testCount += [int]$suiteNode.tests
      $failureCount += [int]$suiteNode.failures
      $errorCount += [int]$suiteNode.errors
      $skipCount += [int]$suiteNode.skipped
    }
  }
  Write-Host "GUI gate results: $label tests=$testCount failures=$failureCount errors=$errorCount skipped=$skipCount"
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
    Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value "| ``$label`` | $testCount | $failureCount | $errorCount | $skipCount |"
  }
  # Alert.error/Alert.warn intentionally logs expected validation cases. Detect
  # uncaught failures in process logs and deferred EDT exceptions in test XML.
  $unexpectedError = '(?im)Exception in thread|(?:^|\s)(?:java\.)?lang\.NullPointerException(?:$|\s)|(?:^|\s)(?:java\.)?lang\.ClassCastException(?:$|\s)|(?:^|\s)(?:java\.)?lang\.ArrayIndexOutOfBoundsException(?:$|\s)'
  if (($stdoutText + "`n" + $stderrText) -match $unexpectedError) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate emitted an unexpected exception/error diagnostic: $label"
  }
  $contentionEvidence = @(Get-ChildItem -LiteralPath $guiArtifacts -Filter '*.foreground-overlap.txt' -File -ErrorAction SilentlyContinue |
    Where-Object { Select-String -LiteralPath $_.FullName -SimpleMatch 'GUI_ENVIRONMENT_CONTENDED_CANDIDATE' -Quiet })
  if ((Test-Path -LiteralPath $contentionMarker) -or $contentionEvidence.Count -gt 0) {
    Save-GuiFailureScreenshot $label
    $files = ($contentionEvidence | ForEach-Object FullName) -join ', '
    $markerText = if (Test-Path -LiteralPath $contentionMarker) { Get-Content -LiteralPath $contentionMarker -Raw } else { '' }
    throw "GUI_ENVIRONMENT_CONTENDED: desktop interference stopped $label. The original JUnit results remain failed and were not retried or suppressed. Marker=$markerText Evidence=$files"
  }
  $gradleExitCodeText = '(status file missing)'
  if (Test-Path -LiteralPath $gradleExitCodeFile) {
    $gradleExitCodeText = (Get-Content -LiteralPath $gradleExitCodeFile -Raw -ErrorAction Stop).Trim()
  }
  $gradleExitCode = 0
  if (-not [int]::TryParse($gradleExitCodeText, [ref]$gradleExitCode)) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate did not produce a valid Gradle exit status: $label file=$gradleExitCodeFile value=$gradleExitCodeText"
  }
  if ($gradleExitCode -ne 0) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate failed: $label exit=$gradleExitCode"
  }
  $deferredExceptions = @()
  foreach ($resultFile in $resultFiles) {
    [xml]$resultXml = Get-Content -LiteralPath $resultFile.FullName -Raw
    foreach ($systemError in @($resultXml.SelectNodes('//system-err'))) {
      if ([string]$systemError.InnerText -match $unexpectedError) {
        $detail = ([string]$systemError.InnerText).Trim()
        if ($detail.Length -gt 4000) { $detail = $detail.Substring(0, 4000) + ' … (truncated)' }
        $deferredExceptions += "$($resultFile.FullName): $detail"
      }
    }
  }
  if ($deferredExceptions.Count -gt 0) {
    Save-GuiFailureScreenshot $label
    $details = ($deferredExceptions | Select-Object -Unique) -join [Environment]::NewLine
    throw "GUI gate found an unexpected deferred GUI exception in test XML: $label`n$details"
  }
  $allowedSkipReasons = @(
    'Direct command sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.',
    'This visual-matrix case is the high-DPI counterpart of the 100% command sweep.',
    'Direct ribbon color gallery requires full-width desktop; overflow coverage is provided by taskColorGalleriesApplySelectedTasksWithUndoRedoAndMpoReload'
  )
  $unexpectedSkips = @()
  foreach ($resultFile in $resultFiles) {
    [xml]$resultXml = Get-Content -LiteralPath $resultFile.FullName -Raw
    foreach ($testCase in @($resultXml.testsuite.testcase)) {
      if ($null -ne $testCase.skipped) {
        $reason = [string]$testCase.skipped.message
        $isAllowedSkip = $false
        foreach ($allowedReason in $allowedSkipReasons) {
          if ($reason.EndsWith($allowedReason, [StringComparison]::Ordinal)) {
            $isAllowedSkip = $true
            break
          }
        }
        if (-not $isAllowedSkip) {
          $unexpectedSkips += "$($testCase.classname).$($testCase.name): $reason"
        }
      }
    }
  }
  if ($unexpectedSkips.Count -gt 0) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate found unexplained skipped test(s): $label ($($unexpectedSkips -join '; '))"
  }
}

# Stable-candidate releases use these visual acceptance selectors at 100%.
# Full ja/100% remains the complete functional suite.
$visualClasses = @(
  'com.microproject.dialog.ProjectDialogGuiAcceptanceTest',
  'com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest',
  'com.microproject.dialog.ProjectInformationDialogGuiAcceptanceTest',
  'com.microproject.dialog.BaselineDialogGuiAcceptanceTest',
  'com.microproject.dialog.ResourceMappingDialogGuiAcceptanceTest',
  'com.microproject.pm.graphic.spreadsheet.TaskInformationGuiAcceptanceTest',
  'com.microproject.pm.graphic.frames.workspace.DefaultFrameManagerGuiAcceptanceTest',
  'com.microproject.ui.shell.WindowShellNativeDecorationGuiAcceptanceTest',
  'com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest',
  'com.microproject.dialog.FlatLafLegacyDialogRefreshGuiAcceptanceTest',
  'com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest'
)
$taskInformationTestClass = 'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest'
# Keep scale-sensitive report/view surfaces, viewport and dialog-fit checks,
# and physical routes whose coordinates or available screen area are asserted.
# The remaining functional/mutation and route-variant cases still run in full ja/100%.
$taskInformationVisualMethods = @(
  'physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute',
  'physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues',
  'physicalWhoDoesWhatReportShowsResourceGroupsTasksAndWorkTotalsWithoutMutatingProject',
  'robotOpensIssue590DialogsWithoutClippedText',
  'robotClickOnTaskPropertiesInformationOpensTaskInformation',
  'usageDetailViewRouteConstructsWithoutRuntimeFailure',
  'physicalSelectedTasksZoomFitsBothSeparatedGanttTasksWithoutChangingModelOrUndo',
  'robotBottomViewSelectionRemainsVisibleUntilExplicitNoSubWindow',
  'robotCalendarOptionsRibbonRouteOpensUsableDialog',
  'robotChangeWorkingTimeProjectRibbonRouteOpensAndCancels',
  'robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix',
  'robotCalendarCommandOpensUsableCalendarDialog',
  'robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews',
  'calendarDependencyMarkerOpensTheLinkDialogAndCanRemoveTheLink',
  'timesheetRouteConstructsItsDedicatedSpreadsheet',
  'statusDateRibbonRouteUsesSharedMutationFixture',
  'physicalTaskInspectorFollowsSelectionAndNavigatesOnlyVisiblePredecessors',
  'physicalIncompleteTaskFilterPreservesDomainAndSelectsVisibleTaskByStableId',
  'physicalTaskSortSupportsAscendingStartAndDescendingFinishForViewAndOutline',
  'physicalTaskRowHeaderDragReordersStableTasksAndSurvivesUndoAndReload',
  'secondaryDocumentWindowUsesTheSameRibbonShell'
)
$visualTests = @($visualClasses + @($taskInformationVisualMethods | ForEach-Object { "$taskInformationTestClass.$_" }))

$guiTestSourceRoot = Join-Path $PWD 'modules/microproject_ui/src/guiTest/java'
foreach ($testSelector in $visualTests) {
  $testClass = $testSelector
  $methodName = $null
  $methodPrefix = "$taskInformationTestClass."
  if ($testSelector.StartsWith($methodPrefix, [StringComparison]::Ordinal)) {
    $testClass = $taskInformationTestClass
    $methodName = $testSelector.Substring($methodPrefix.Length)
  }
  $sourcePath = Join-Path $guiTestSourceRoot (($testClass.Replace('.', '\')) + '.java')
  if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
    throw "Visual GUI test selector does not map to a source file: $testSelector ($sourcePath)"
  }
  if ($null -ne $methodName) {
    $methodPattern = '(?m)^\s*@Test\s*(?:\r?\n\s*@[^\r\n]+\s*)*(?:\r?\n\s*)+void\s+' + [regex]::Escape($methodName) + '\s*\('
    $methodMatches = [regex]::Matches([System.IO.File]::ReadAllText($sourcePath), $methodPattern)
    if ($methodMatches.Count -ne 1) {
      throw "Visual GUI test method must map to exactly one @Test source method: $testSelector ($sourcePath)"
    }
  }
}

try {
  Start-HostedPolicies
  Test-HostedWindowsKeyHotkeysDisabled
  if (-not [string]::IsNullOrWhiteSpace($TestFilter)) {
    $safeFilter = ($TestFilter -replace '[^A-Za-z0-9_.-]', '_')
    $safeScale = $Scale.Replace('.', '-')
    Write-Host "Run targeted GUI test filter: $TestFilter (locale=$Locale, scale=$Scale). This run is not full GUI acceptance coverage."
    $focusedArgs = @(
      ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
      "-PguiTestLocale=$Locale", "-PguiTestUiScale=$Scale", '-PguiTestSuite=full',
      '--tests', $TestFilter
    )
    Invoke-GuiGate "focused-$safeFilter-$Locale-$safeScale" $focusedArgs 1200
    return
  }

  Write-Host "GUI acceptance Step 1/2: run the '$Suite' suite at 100% (ja)."
  try {
    $functionalTimeoutSeconds = if ($Suite -eq 'full') { 2700 } else { 1200 }
    $functionalArgs = @(
      ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
      '-PguiTestLocale=ja', '-PguiTestUiScale=1.0'
    )
    if ($Suite -eq 'visual') {
      foreach ($testClass in $visualTests) { $functionalArgs += @('--tests', $testClass) }
    } else {
      $functionalArgs += "-PguiTestSuite=$Suite"
    }
    Invoke-GuiGate "functional-$Suite-ja-100" $functionalArgs $functionalTimeoutSeconds
  } catch {
    $gateFailures.Add($_.Exception.Message)
    throw "GUI acceptance Step 1/2 failed at 100%; Step 2 (locale/scale matrix) was not run: $($_.Exception.Message)"
  }

  if ($Suite -in @('full', 'visual')) {
    if ($StableReleaseGate) {
      Write-Host 'GUI acceptance Step 2/2: run the English 100% visual leg after the Japanese 100% suite passed.'
      $locale = 'en'
      $scale = '1.0'
      Write-Host "GUI visual gate: locale=$locale scale=$scale"
      $gradleArgs = @(
        ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
        "-PguiTestLocale=$locale", "-PguiTestUiScale=$scale"
      )
      foreach ($testClass in $visualTests) { $gradleArgs += @('--tests', $testClass) }
      Invoke-GuiGate "visual-$locale-$scale" $gradleArgs 1200
    } else {
      Write-Host 'GUI acceptance Step 2/2: run the broader visual matrix after the 100% suite passed.'
      foreach ($locale in @('ja', 'en')) {
        # Step 1 already ran the Japanese 100% baseline. Run the remaining
        # visual legs without repeating that baseline.
        $scales = if ($locale -eq 'ja') { @('1.25', '1.5') } else { @('1.0', '1.25', '1.5') }
        foreach ($scale in $scales) {
          Write-Host "GUI visual gate: locale=$locale scale=$scale"
          $gradleArgs = @(
            ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
            "-PguiTestLocale=$locale", "-PguiTestUiScale=$scale"
          )
          foreach ($testClass in $visualTests) { $gradleArgs += @('--tests', $testClass) }
          try {
            Invoke-GuiGate "visual-$locale-$scale" $gradleArgs 1200
          } catch {
            if ($_.Exception.Message -match 'GUI_ENVIRONMENT_CONTENDED|GUI_ENVIRONMENT_MONITOR_UNAVAILABLE') {
              throw "Full GUI audit stopped at $locale/$scale because the desktop session became unusable: $($_.Exception.Message)"
            }
            $gateFailures.Add($_.Exception.Message)
            Write-Warning "Continuing GUI audit after visual gate failure: $($_.Exception.Message)"
          }
        }
      }
    }
  }
} finally {
  Complete-GuiAcceptanceCleanup
}

if ($gateFailures.Count -gt 0) {
  throw "GUI acceptance Step 2/2 completed with $($gateFailures.Count) failure(s): $($gateFailures -join ' | ')"
}
