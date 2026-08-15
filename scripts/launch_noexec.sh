#!/usr/bin/env bash
# Launch WITHOUT exec so java stays a child and inherits our stderr redirect.
# Logs are written inside the script so background-process /tmp isolation
# cannot hide them from the host file system.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
LIB_DIR="$ROOT/modules/projectlibre_ui/build/install/projectlibre_ui/lib"
ARG_FILE_RAW="$(mktemp -t projectlibre-java-args.XXXXXX.txt)"
ARG_FILE="$(cygpath -w "$ARG_FILE_RAW")"
LOG="${PROJECTLIBRE_LOG:-$ROOT/build/gui_dbg.log}"
trap 'rm -f "$ARG_FILE_RAW"' EXIT

JAVA_EXE="$(command -v java)"
if [ -z "${JAVA_HOME:-}" ] && [ -x "/c/Program Files/Java/jdk-26.0.1/bin/java.exe" ]; then
  JAVA_EXE="/c/Program Files/Java/jdk-26.0.1/bin/java.exe"
fi

preferred=(
  projectlibre_ui.jar projectlibre_application.jar projectlibre_exchange.jar
  projectlibre_reports.jar projectlibre_core.jar projectlibre-contrib.jar
)
paths=()
for name in "${preferred[@]}"; do
  paths+=("$(cygpath -w "$LIB_DIR/$name")")
done
while IFS= read -r f; do
  bn="$(basename "$f")"
  skip=0
  for p in "${preferred[@]}"; do
    [ "$bn" = "$p" ] && skip=1 && break
  done
  [ "$skip" -eq 1 ] || paths+=("$(cygpath -w "$f")")
done < <(ls -1 "$LIB_DIR"/*.jar | sort)

{
  echo --enable-native-access=ALL-UNNAMED
  echo -classpath
  ( IFS=';'; echo "${paths[*]}" )
  echo com.microproject.main.Main
} > "$ARG_FILE"

echo "Launching with $JAVA_EXE"
echo "Logging to $LOG"
"$JAVA_EXE" @"$ARG_FILE" "$@" > "$LOG" 2>&1
