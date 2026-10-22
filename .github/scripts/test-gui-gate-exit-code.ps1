$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$childScript = Join-Path $PSScriptRoot 'run-gui-gate-child.ps1'
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) "gui-gate-exit-code-$([Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $testRoot | Out-Null

function Invoke-ExitCodeCase([string]$name, [string[]]$gradleArguments, [bool]$expectSuccess) {
  $caseRoot = Join-Path $testRoot $name
  New-Item -ItemType Directory -Path $caseRoot | Out-Null
  $exitCodeFile = Join-Path $caseRoot 'exit-code.txt'
  $configurationFile = Join-Path $caseRoot 'invocation.json'
  $configuration = @{
    WorkingDirectory = $repositoryRoot
    GradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
    Arguments = $gradleArguments
    ExitCodeFile = $exitCodeFile
  } | ConvertTo-Json -Depth 4
  [System.IO.File]::WriteAllText($configurationFile, $configuration,
    [System.Text.UTF8Encoding]::new($false))

  $stdout = Join-Path $caseRoot 'stdout.log'
  $stderr = Join-Path $caseRoot 'stderr.log'
  $childArguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -ConfigurationFile "{1}"' -f `
    $childScript, $configurationFile
  $windowsPowerShell = (Get-Command powershell.exe -ErrorAction Stop).Source
  $process = Start-Process -FilePath $windowsPowerShell `
    -ArgumentList $childArguments -PassThru -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr -WindowStyle Hidden
  if (-not $process.WaitForExit(180000)) {
    taskkill.exe /PID $process.Id /T /F | Out-Null
    throw "Timed out running $name Gradle exit-code test case."
  }

  if (-not (Test-Path -LiteralPath $exitCodeFile)) {
    $errorText = if (Test-Path -LiteralPath $stderr) { Get-Content -LiteralPath $stderr -Raw } else { '' }
    throw "$name did not record an exit code. Child stderr: $errorText"
  }
  $exitCodeText = (Get-Content -LiteralPath $exitCodeFile -Raw).Trim()
  $exitCode = 0
  if (-not [int]::TryParse($exitCodeText, [ref]$exitCode)) {
    throw "$name recorded an unparseable exit code: '$exitCodeText'"
  }
  if ($expectSuccess -and $exitCode -ne 0) {
    throw "$name expected exit code 0, got $exitCode. See $stderr"
  }
  if (-not $expectSuccess -and $exitCode -eq 0) {
    throw "$name expected a nonzero exit code, got 0. See $stderr"
  }
  Write-Host "$name recorded Gradle exit code $exitCode"
}

try {
  Invoke-ExitCodeCase 'success' @(':microproject_ui:tasks', '--max-workers=1', '--console=plain') $true
  Invoke-ExitCodeCase 'failure' @(':microproject_ui:no-such-gui-exit-code-test-task', '--console=plain') $false
  Write-Host 'GUI gate exit-code handoff passed success and failure cases.'
} finally {
  Remove-Item -LiteralPath $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}
