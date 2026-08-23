#!/usr/bin/env bash
# Copyright (c) 2026 microProject
# SPDX-License-Identifier: MIT
#
# Recomputes the "Baseline And Update Ratio" figures reported in README.md.
#
# The numbers in README.md go stale on every commit (issue #351). Run this
# script to regenerate them instead of hand-counting:
#
#   ./scripts/baseline_update_ratio.sh
#
# Determinism notes:
# - `--no-renames` is used on purpose. With git's default rename detection the
#   diff exceeds `diff.renameLimit`, git prints a warning and silently falls
#   back to a partial detection, so the line totals are not reproducible
#   between machines/git versions. `--no-renames` counts a rename as a delete
#   plus an add, which is stable everywhere.
# - "Changed baseline files" counts only paths that existed in the baseline
#   commit; "changed paths (total)" also includes files added after it.
set -euo pipefail

BASELINE="${1:-0530be227f4a10c5545cce8d3db20ac5a4d76a66}"
HEAD_REF="${2:-HEAD}"

cd "$(dirname "$0")/.."

if ! git cat-file -e "${BASELINE}^{commit}" 2>/dev/null; then
	echo "baseline commit not found: ${BASELINE}" >&2
	exit 1
fi

base_list="$(mktemp)"
changed_list="$(mktemp)"
trap 'rm -f "$base_list" "$changed_list"' EXIT

git ls-tree -r --name-only "$BASELINE" | sort >"$base_list"
git diff --name-only --no-renames "$BASELINE" "$HEAD_REF" | sort -u >"$changed_list"

baseline_files="$(wc -l <"$base_list" | tr -d ' ')"
changed_paths="$(wc -l <"$changed_list" | tr -d ' ')"
changed_baseline_files="$(comm -12 "$base_list" "$changed_list" | wc -l | tr -d ' ')"

stat_line="$(git diff --shortstat --no-renames "$BASELINE" "$HEAD_REF")"
insertions="$(printf '%s' "$stat_line" | grep -oE '[0-9]+ insertion' | grep -oE '[0-9]+' || echo 0)"
deletions="$(printf '%s' "$stat_line" | grep -oE '[0-9]+ deletion' | grep -oE '[0-9]+' || echo 0)"
changed_lines="$((insertions + deletions))"

percent="$(awk -v a="$changed_baseline_files" -v b="$baseline_files" 'BEGIN{ if (b==0) print "0.0"; else printf "%.1f", (a*100.0)/b }')"

echo "baseline commit: ${BASELINE}"
echo "compared against: ${HEAD_REF} ($(git rev-parse --short "$HEAD_REF"))"
echo "- Changed tracked file paths since \`$(printf '%s' "$BASELINE" | cut -c1-8)\`: \`${changed_baseline_files} / ${baseline_files}\` (\`${percent}%\` of the baseline tracked file count)"
echo "- Changed tracked text lines since \`$(printf '%s' "$BASELINE" | cut -c1-8)\` (insertions + deletions): \`${changed_lines}\`"
echo "- Changed paths including files added after the baseline: \`${changed_paths}\`"
