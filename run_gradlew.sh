#!/bin/bash
# MSYS-safe Gradle wrapper launcher: translates paths to Windows form so the
# Windows-native java.exe can open the wrapper jar and the build/install layout.
set -e
REPO="$(cd "$(dirname "$0")" && pwd)"
APP_HOME_WIN="$(cygpath -w "$REPO")"
JAVA_HOME_WIN="$(cygpath -w "C:/Program Files/Java/jdk-26.0.1")"
export JAVA_HOME="$JAVA_HOME_WIN"
CLASSPATH="$APP_HOME_WIN\\gradle\\wrapper\\gradle-wrapper.jar"
JAVACMD="$JAVA_HOME_WIN\\bin\\java.exe"
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
