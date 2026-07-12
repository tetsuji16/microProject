@echo off
setlocal
set SCRIPT_DIR=%~dp0
call "%SCRIPT_DIR%run_projectlibre_clean.bat" %*
exit /b %ERRORLEVEL%
