$ErrorActionPreference = 'Continue'
$diagnostics = 'modules/microproject_ui/build/reports/guiTest-artifacts'
New-Item -ItemType Directory -Force -Path $diagnostics | Out-Null
Get-Process java,javaw -ErrorAction SilentlyContinue |
  Select-Object Id,ProcessName,CPU,StartTime,Path |
  Out-File (Join-Path $diagnostics 'java-processes.txt')
$jcmd = Join-Path $env:JAVA_HOME 'bin/jcmd.exe'
if (-not (Test-Path -LiteralPath $jcmd)) { $jcmd = 'jcmd' }
Get-Process java,javaw -ErrorAction SilentlyContinue | ForEach-Object {
  $dump = Join-Path $diagnostics ("thread-dump-$($_.Id).txt")
  & $jcmd $_.Id Thread.print 2>&1 | Out-File $dump
}
