param(
  [ValidateSet('smoke', 'full')]
  [string]$Suite = 'smoke'
)

$ErrorActionPreference = 'Stop'
$gateLogs = 'modules/microproject_ui/build/reports/guiTest-artifacts'
New-Item -ItemType Directory -Force -Path $gateLogs | Out-Null

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

function Invoke-GuiGate([string]$label, [string[]]$arguments) {
  $safe = ($label -replace '[^A-Za-z0-9_.-]', '_')
  $stdout = Join-Path $gateLogs "$safe.stdout.log"
  $stderr = Join-Path $gateLogs "$safe.stderr.log"
  $testResults = Join-Path $PWD 'modules/microproject_ui/build/test-results/guiTest'
  New-Item -ItemType Directory -Force -Path $testResults | Out-Null
  Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force
  $process = Start-Process -FilePath (Join-Path $PWD 'gradlew.bat') `
    -ArgumentList $arguments -PassThru -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr
  if (-not $process.WaitForExit(900000)) {
    taskkill.exe /PID $process.Id /T /F | Out-Null
    Save-GuiFailureScreenshot $label
    throw "GUI gate watchdog timed out: $label"
  }
  $stdoutText = Get-Content -LiteralPath $stdout -Raw
  $stderrText = Get-Content -LiteralPath $stderr -Raw
  $stdoutText
  $stderrText
  # Alert.error/Alert.warn intentionally logs expected validation cases. Detect
  # uncaught failures in process logs and deferred EDT exceptions in test XML.
  $unexpectedError = '(?im)Exception in thread|(?:^|\s)(?:java\.)?lang\.NullPointerException(?:$|\s)|(?:^|\s)(?:java\.)?lang\.ClassCastException(?:$|\s)|(?:^|\s)(?:java\.)?lang\.ArrayIndexOutOfBoundsException(?:$|\s)'
  if (($stdoutText + "`n" + $stderrText) -match $unexpectedError) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate emitted an unexpected exception/error diagnostic: $label"
  }
  if ($process.ExitCode -ne 0) {
    Save-GuiFailureScreenshot $label
    throw "GUI gate failed: $label exit=$($process.ExitCode)"
  }
  $deferredExceptions = Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue |
    Select-String -Pattern $unexpectedError
  if ($null -ne $deferredExceptions) {
    Save-GuiFailureScreenshot $label
    $files = ($deferredExceptions | Select-Object -ExpandProperty Path -Unique) -join ', '
    throw "GUI gate found an unexpected deferred GUI exception in test XML: $label ($files)"
  }
  $allowedSkipReasons = @(
    'Direct command sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.',
    'This visual-matrix case is the high-DPI counterpart of the 100% command sweep.'
  )
  $unexpectedSkips = @()
  foreach ($resultFile in (Get-ChildItem -LiteralPath $testResults -Filter '*.xml' -File -ErrorAction SilentlyContinue)) {
    [xml]$resultXml = Get-Content -LiteralPath $resultFile.FullName -Raw
    foreach ($testCase in @($resultXml.testsuite.testcase)) {
      if ($null -ne $testCase.skipped) {
        $reason = [string]$testCase.skipped.message
        if ($allowedSkipReasons -notcontains $reason) {
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
# audit selects full and adds locale × scale visual checks without publishing.
Invoke-GuiGate "functional-$Suite-ja-100" @(
  ':microproject_ui:guiTest', '--max-workers=1', '--console=plain',
  "-PguiTestSuite=$Suite", '-PguiTestLocale=ja', '-PguiTestUiScale=1.0'
)

if ($Suite -eq 'full') {
  $visualTests = @(
    'com.microproject.dialog.ProjectDialogGuiAcceptanceTest',
    'com.microproject.dialog.ChangeWorkingTimeDialogGuiAcceptanceTest',
    'com.microproject.dialog.ProjectInformationDialogGuiAcceptanceTest',
    'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest',
    'com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest'
  )
  foreach ($locale in @('ja', 'en')) {
    foreach ($scale in @('1.0', '1.25', '1.5')) {
      Write-Host "GUI visual gate: locale=$locale scale=$scale"
      $gradleArgs = @(
        ':microproject_ui:guiTest', '--max-workers=1', '--rerun-tasks', '--console=plain',
        "-PguiTestLocale=$locale", "-PguiTestUiScale=$scale"
      )
      foreach ($testClass in $visualTests) { $gradleArgs += @('--tests', $testClass) }
      Invoke-GuiGate "visual-$locale-$scale" $gradleArgs
    }
  }
}
