#!/bin/bash
# Launch the built app opening a sample .pod directly (separate instance from any running one).
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(cygpath -w "$REPO/modules/micrproject_ui/build/install/micrproject_ui")"
JH="$(cygpath -w "C:/Program Files/Java/jdk-26.0.1")"
JAVA_EXE="$JH\\bin\\java.exe"
CLASSPATH="$APP_HOME\\lib\\micrproject_ui.jar;$APP_HOME\\lib\\jgoodies-forms-1.9.0.jar;$APP_HOME\\lib\\*"
export JAVA_HOME="$JH"
SAMPLE="$(cygpath -w "$REPO/samples/Commercial construction project plan.pod")"
LOG="$(cygpath -w "$REPO/app_sample.log")"
"$JAVA_EXE" -classpath "$CLASSPATH" com.microproject.main.Main "$SAMPLE" > "$LOG" 2>&1
