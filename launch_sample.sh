#!/bin/bash
# Launch the built app opening a sample .pod directly (separate instance from any running one).
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cygpath -w "$REPO/modules/micrproject_ui/build/install/micrproject_ui")"
JH="$(cygpath -w "$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | sed 's/.*= *//')")"
JAVA_EXE="$JH\\bin\\java.exe"
CLASSPATH="$APP_HOME\\lib\\micrproject_ui.jar;$APP_HOME\\lib\\jgoodies-forms-1.9.0.jar;$APP_HOME\\lib\\*"
SAMPLE="$(cygpath -w "$REPO/samples/Commercial construction project plan.pod")"
LOG="$(cygpath -w "$REPO/app_sample.log")"
export JAVA_HOME="$JH"
"$JAVA_EXE" -classpath "$CLASSPATH" com.microproject.main.Main "$SAMPLE" > "$LOG" 2>&1
