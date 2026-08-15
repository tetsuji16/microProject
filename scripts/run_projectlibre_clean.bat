@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "GRADLEW=%PROJECT_ROOT%\gradlew.bat"
set "LIB_DIR=%PROJECT_ROOT%\modules\projectlibre_ui\build\install\projectlibre_ui\lib"
set "ARG_FILE=%TEMP%\projectlibre-java-args-%RANDOM%.txt"

if not exist "%GRADLEW%" (
  echo ERROR: Gradle wrapper not found at "%GRADLEW%".
  exit /b 1
)

call :StopProjectLibreProcesses

pushd "%PROJECT_ROOT%"
call "%GRADLEW%" clean build installDist --console=plain
set "GRADLE_EXIT=%ERRORLEVEL%"
popd
if not "%GRADLE_EXIT%"=="0" exit /b %GRADLE_EXIT%

if not exist "%LIB_DIR%\projectlibre_ui.jar" (
  echo ERROR: installDist output not found at "%LIB_DIR%".
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$lib = '%LIB_DIR%';" ^
  "$arg = '%ARG_FILE%';" ^
  "$preferred = @('projectlibre_ui.jar','projectlibre_application.jar','projectlibre_exchange.jar','projectlibre_reports.jar','projectlibre_core.jar','projectlibre-contrib.jar');" ^
  "$paths = New-Object System.Collections.Generic.List[string];" ^
  "foreach ($name in $preferred) { $paths.Add((Join-Path $lib $name)) }" ^
  "Get-ChildItem -LiteralPath $lib -Filter *.jar | Sort-Object Name | Where-Object { $preferred -notcontains $_.Name } | ForEach-Object { $paths.Add($_.FullName) };" ^
  "@('--enable-native-access=ALL-UNNAMED','-classpath', ($paths -join ';'), 'com.microproject.main.Main') | Set-Content -LiteralPath $arg -Encoding ASCII"
if errorlevel 1 exit /b %ERRORLEVEL%

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)

"%JAVA_EXE%" @"%ARG_FILE%" %*
set "EXIT_CODE=%ERRORLEVEL%"

del "%ARG_FILE%" >nul 2>&1
exit /b %EXIT_CODE%

:StopProjectLibreProcesses
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$procs = Get-CimInstance Win32_Process -Filter \"Name = 'java.exe' OR Name = 'javaw.exe'\" | Where-Object { $_.CommandLine -match 'projectlibre' };" ^
  "foreach ($proc in $procs) { Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue }"
exit /b 0
