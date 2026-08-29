#!/bin/bash
# MSYS-safe launcher for the built microProject desktop app.
# Bypasses the generated bin/*.bat (which mishandles quoted windows paths under
# MSYS bash) and starts java directly with a fixed classpath (UI jar FIRST so the
# DefaultFormBuilder shim resolves to the bundled version).
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cygpath -w "$REPO/modules/micrproject_ui/build/install/micrproject_ui")"
LOG="$(cygpath -w "$REPO/build/app_launch.log")"
JH="$(cygpath -w "$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | sed 's/.*= *//')")"
JAVA_EXE="$JH\\bin\\java.exe"
CLASSPATH="$APP_HOME\\lib\\micrproject_ui.jar;$APP_HOME\\lib\\jgoodies-forms-1.9.0.jar;$APP_HOME\\lib\\*"
export JAVA_HOME="$JH"
"$JAVA_EXE" -classpath "$CLASSPATH" com.microproject.main.Main > "$LOG" 2>&1
