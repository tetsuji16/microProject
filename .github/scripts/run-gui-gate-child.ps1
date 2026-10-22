param(
  [Parameter(Mandatory = $true)]
  [string]$ConfigurationFile
)

$ErrorActionPreference = 'Stop'

try {
  $configuration = Get-Content -LiteralPath $ConfigurationFile -Raw | ConvertFrom-Json
  Set-Location -LiteralPath $configuration.WorkingDirectory
  & $configuration.GradleWrapper @($configuration.Arguments)
  $exitCode = $LASTEXITCODE
  if ($null -eq $exitCode) {
    throw 'Gradle wrapper did not provide an exit code.'
  }

  [System.IO.File]::WriteAllText(
    $configuration.ExitCodeFile,
    [string][int]$exitCode,
    [System.Text.UTF8Encoding]::new($false))
  exit ([int]$exitCode)
} catch {
  [Console]::Error.WriteLine("GUI gate child failed before recording Gradle exit status: $_")
  exit 1
}
