#!/bin/bash
# MSYS-safe Gradle wrapper launcher.  Windows-native java.exe cannot open the
# wrapper jar from an MSYS path, so derive windows paths via cygpath.
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME_WIN="$(cygpath -w "$REPO")"
JH="$(cygpath -w "$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | sed 's/.*= *//')")"
export JAVA_HOME="$JH"
CLASSPATH="$APP_HOME_WIN\\gradle\\wrapper\\gradle-wrapper.jar"
JAVACMD="$JH\\bin\\java.exe"
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
