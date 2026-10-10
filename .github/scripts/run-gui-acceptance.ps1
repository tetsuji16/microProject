param(
  [ValidateSet('smoke', 'full')]
  [string]$Suite = 'smoke'
)

$ErrorActionPreference = 'Stop'
$gateLogs = 'modules/microproject_ui/build/reports/guiTest-artifacts'
$gateFailures = [System.Collections.Generic.List[string]]::new()
$hostedWarningWatcher = $null
New-Item -ItemType Directory -Force -Path $gateLogs | Out-Null

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
  Start-HostedWarningWatcher
  Minimize-HostedRunnerConsole
  $safe = ($label -replace '[^A-Za-z0-9_.-]', '_')
  $attempt = [Guid]::NewGuid().ToString('N')
  $guiArtifacts = Join-Path $PWD "modules/microproject_ui/build/reports/guiTest-artifacts/$safe-$attempt"
  $contentionMarker = Join-Path $guiArtifacts 'environment-contended.marker'
  $stdout = Join-Path $gateLogs "$safe.stdout.log"
  $stderr = Join-Path $gateLogs "$safe.stderr.log"
  $testResults = Join-Path $PWD 'modules/microproject_ui/build/test-results/guiTest'
  New-Item -ItemType Directory -Force -Path $testResults | Out-Null
  Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
  $process = Start-Process -FilePath (Join-Path $PWD 'gradlew.bat') `
    -ArgumentList ($arguments + "-PguiTestArtifactsDir=$guiArtifacts" + "-PguiTestContentionMarker=$contentionMarker") `
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
  foreach ($resultFile in $resultFiles) {
    Copy-Item -LiteralPath $resultFile.FullName -Destination $evidenceDirectory -Force
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
  if ($process.ExitCode -ne 0) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate failed: $label exit=$($process.ExitCode)"
  }
  $deferredExceptions = @()
  foreach ($resultFile in $resultFiles) {
    [xml]$resultXml = Get-Content -LiteralPath $resultFile.FullName -Raw
    foreach ($systemError in @($resultXml.SelectNodes('//system-err'))) {
      if ([string]$systemError.InnerText -match $unexpectedError) {
        $deferredExceptions += $resultFile.FullName
      }
    }
  }
  if ($deferredExceptions.Count -gt 0) {
    Save-GuiFailureScreenshot $label
    $files = ($deferredExceptions | Select-Object -Unique) -join ', '
    throw "GUI gate found an unexpected deferred GUI exception in test XML: $label ($files)"
  }
  $allowedSkipReasons = @(
    'Direct command sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.',
    'This visual-matrix case is the high-DPI counterpart of the 100% command sweep.'
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

# Release runs the efficient shared-cause smoke gate. The scheduled/manual
# audit selects full and runs acceptance in two strict steps: first every test
# at 100%, then (only after Step 1 passes) the locale × scale visual matrix.
try {
  Write-Host "GUI acceptance Step 1/2: run the complete '$Suite' suite at 100% (ja)."
  try {
    $functionalTimeoutSeconds = if ($Suite -eq 'full') { 2700 } else { 1200 }
    Invoke-GuiGate "functional-$Suite-ja-100" @(
      ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
      "-PguiTestSuite=$Suite", '-PguiTestLocale=ja', '-PguiTestUiScale=1.0'
    ) $functionalTimeoutSeconds
  } catch {
    $gateFailures.Add($_.Exception.Message)
    throw "GUI acceptance Step 1/2 failed at 100%; Step 2 (locale/scale matrix) was not run: $($_.Exception.Message)"
  }

  if ($Suite -eq 'full') {
    Write-Host 'GUI acceptance Step 2/2: run the visual matrix after the full 100% suite passed.'
    $visualTests = @(
      'com.microproject.dialog.ProjectDialogGuiAcceptanceTest',
      'com.microproject.dialog.ChangeWorkingTimeDialogGuiAcceptanceTest',
      'com.microproject.dialog.ProjectInformationDialogGuiAcceptanceTest',
      'com.microproject.dialog.ResourceMappingDialogGuiAcceptanceTest',
      'com.microproject.pm.graphic.spreadsheet.TaskInformationGuiAcceptanceTest',
      'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest',
      'com.microproject.pm.graphic.frames.workspace.DefaultFrameManagerGuiAcceptanceTest',
      'com.microproject.ui.shell.WindowShellNativeDecorationGuiAcceptanceTest',
      'com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest',
      'com.microproject.dialog.FlatLafLegacyDialogRefreshGuiAcceptanceTest'
    )
    foreach ($locale in @('ja', 'en')) {
      # Step 1 already ran the entire Japanese suite at 100%, including these
      # visual cases. Keep English 100% as a locale check, then test both
      # locales at higher scales without repeating the Japanese baseline.
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
            throw "Full GUI matrix stopped at $locale/$scale because the desktop session became unusable: $($_.Exception.Message)"
          }
          $gateFailures.Add($_.Exception.Message)
          Write-Warning "Continuing GUI audit after visual gate failure: $($_.Exception.Message)"
        }
      }
    }
  }
} finally {
  Stop-HostedWarningWatcher
}

if ($gateFailures.Count -gt 0) {
  throw "GUI acceptance Step 2/2 completed with $($gateFailures.Count) failure(s): $($gateFailures -join ' | ')"
}
