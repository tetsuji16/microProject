#!/usr/bin/env bash
# Launch the already-built installDist layout with the fixed classpath order
# so DefaultFormBuilder resolves to the bundled compatibility shim (mirrors run_projectlibre_clean.bat).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd -P)"
LIB_DIR="$ROOT/modules/projectlibre_ui/build/install/projectlibre_ui/lib"
ARG_FILE_RAW="$(mktemp -t projectlibre-java-args.XXXXXX.txt)"
# java.exe is a Windows-native binary; it cannot read MSYS /tmp paths. Translate to a Windows path.
ARG_FILE="$(cygpath -w "$ARG_FILE_RAW")"
trap 'rm -f "$ARG_FILE_RAW"' EXIT

JAVA_EXE="$(command -v java)"
# Prefer the installed JDK 26 if JAVA_HOME unset
if [ -z "${JAVA_HOME:-}" ] && [ -x "/c/Program Files/Java/jdk-26.0.1/bin/java.exe" ]; then
  JAVA_EXE="/c/Program Files/Java/jdk-26.0.1/bin/java.exe"
fi

preferred=(
  projectlibre_ui.jar
  projectlibre_application.jar
  projectlibre_exchange.jar
  projectlibre_reports.jar
  projectlibre_core.jar
  projectlibre-contrib.jar
)

paths=()
for name in "${preferred[@]}"; do
  paths+=("$(cygpath -w "$LIB_DIR/$name")")
done

# Append remaining jars, sorted by name, excluding preferred ones
while IFS= read -r f; do
  bn="$(basename "$f")"
  skip=0
  for p in "${preferred[@]}"; do
    [ "$bn" = "$p" ] && skip=1 && break
  done
  [ "$skip" -eq 1 ] || paths+=("$(cygpath -w "$f")")
done < <(ls -1 "$LIB_DIR"/*.jar | sort)

# Write arg file (Windows path separators)
{
  echo --enable-native-access=ALL-UNNAMED
  echo -classpath
  ( IFS=';'; echo "${paths[*]}" )
  echo com.projectlibre1.main.Main
} > "$ARG_FILE"

echo "Launching with $JAVA_EXE"
echo "Arg file: $ARG_FILE"
exec "$JAVA_EXE" @"$ARG_FILE" "$@"
