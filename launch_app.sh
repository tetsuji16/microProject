#!/bin/bash
# MSYS-safe direct launcher for the built installDist app.
# Avoids the .bat wrapper (which mishandles quoted Windows paths under MSYS bash).
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cygpath -w "$REPO/modules/micrproject_ui/build/install/micrproject_ui")"
LOG="$(cygpath -w "$REPO/app_launch.log")"
JH="$(cygpath -w "C:/Program Files/Java/jdk-26.0.1")"
JAVA_EXE="$JH\\bin\\java.exe"
# Fixed classpath order: micrproject_ui.jar first, then jgoodies-forms, then the rest.
CLASSPATH="$APP_HOME\\lib\\micrproject_ui.jar;$APP_HOME\\lib\\jgoodies-forms-1.9.0.jar;$APP_HOME\\lib\\*"
export JAVA_HOME="$JH"
"$JAVA_EXE" -classpath "$CLASSPATH" com.microproject.main.Main > "$LOG" 2>&1
