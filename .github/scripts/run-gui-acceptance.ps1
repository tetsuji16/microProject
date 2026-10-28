param(
  [ValidateSet('smoke', 'full', 'visual')]
  [string]$Suite = 'smoke',
  [string]$TestFilter,
  [ValidateSet('ja', 'en')]
  [string]$Locale = 'ja',
  [ValidateSet('1.0', '1.25', '1.5')]
  [string]$Scale = '1.0'
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
New-Item -ItemType Directory -Force -Path $gateLogs | Out-Null

function Disable-HostedToastNotifications {
  if (-not $script:isGitHubActions) { return }
  # GUI runners are dedicated, short-lived accounts. Request that user toast
  # notifications be disabled before launching Swing. This registry setting does
  # not guarantee that Windows will avoid notification-center or tray-overflow
  # flyouts. Preserve the native foreground-overlap gate.
  $notificationKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\PushNotifications'
  New-Item -Path $notificationKey -Force | Out-Null
  Set-ItemProperty -LiteralPath $notificationKey -Name ToastEnabled -Type DWord -Value 0
  $effectiveValue = (Get-ItemProperty -LiteralPath $notificationKey -Name ToastEnabled).ToastEnabled
  if ([int]$effectiveValue -ne 0) {
    throw "Could not disable hosted-user toast notifications; ToastEnabled=$effectiveValue"
  }
  Write-Host 'Set hosted-user toast notification preference to disabled before GUI acceptance (HKCU PushNotifications ToastEnabled=0); shell flyouts may still occur.'
  if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
    Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value '- Hosted GUI preparation: ToastEnabled=0 was set and read back; this does not guarantee prevention of notification-center or tray-overflow flyouts. desktop contention detection remains enabled.'
  }
}

Disable-HostedToastNotifications
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

function Minimize-HostedRunnerConsole([switch]$Quiet) {
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
    if (-not $Quiet) {
      Write-Host "Minimized the hosted-runner console before GUI gate: $windowTitle"
    }
  } elseif (-not $Quiet) {
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
  $deadline = [Diagnostics.Stopwatch]::StartNew()
  $processExited = $false
  while ($deadline.Elapsed.TotalSeconds -lt $timeoutSeconds) {
    if ($script:isGitHubActions) { Minimize-HostedRunnerConsole -Quiet }
    if ($process.WaitForExit(1000)) {
      $processExited = $true
      break
    }
  }
  if (-not $processExited) {
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

# Stable-candidate PRs use these visual acceptance selectors at 100% and in
# each locale/scale leg. Full ja/100% remains the complete functional suite.
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
    $methodPattern = '(?m)^\s*@Test\s*(?:\r?\n\s*)+void\s+' + [regex]::Escape($methodName) + '\s*\('
    $methodMatches = [regex]::Matches([System.IO.File]::ReadAllText($sourcePath), $methodPattern)
    if ($methodMatches.Count -ne 1) {
      throw "Visual GUI test method must map to exactly one @Test source method: $testSelector ($sourcePath)"
    }
  }
}

try {
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
    Write-Host 'GUI acceptance Step 2/2: run the visual matrix after the 100% suite passed.'
    foreach ($locale in @('ja', 'en')) {
      # Step 1 already ran the Japanese 100% baseline for either the full suite
      # or these selected visual classes. Test English 100% plus both locales
      # at higher scales without repeating the Japanese baseline.
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
