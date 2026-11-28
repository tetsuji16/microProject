# Issue #1461 — ExtendedDateFormat を java.time で書き直す

State: IMPLEMENTATION COMPLETE; follow-up PR awaiting checks
Issue: https://github.com/tetsuji16/microProject/issues/1461
Prior formatter-only PR: https://github.com/tetsuji16/microProject/pull/1501 (merged at `e71f9cb`; did not include the owner-approved package-wide migration)
Approved scope clarification: https://github.com/tetsuji16/microProject/issues/1461#issuecomment-7232208674
Current branch: `codex/issue-1461-package-grep`

## Goal and scope

Remove `java.util.Date` / `java.util.Calendar` from all production sources in `com.microproject.timescale` while preserving epoch-millisecond GMT schedule semantics, configured numeric timescale fields/patterns, interval boundaries, labels, and formatter behavior. Migrated `CalendarUtil`, `TimeInterval`, `TimeIterator`, `TimeScale`, and dependent callsites in script interval formatting, `CoordinatesConverter`, and `GanttRenderer`.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | `ExtendedDateFormatTest` passes before and after formatter rewrite | pass | Baseline test-only commit `e2b3b2198`: 3 tests, 0 failures; candidate: 6 tests, 0 failures |
| A2 | No `SimpleDateFormat` inheritance | pass | Search `rg -n 'extends SimpleDateFormat' ...ExtendedDateFormat.java`: 0 matches |
| A3 | Exact timescale package Date/Calendar grep returns 0 | pass | Exact issue `git grep`: 0 matches |
| A4 | Four i18n keys and strings unchanged | pass | `git diff origin/master -- modules/microproject_core/src/main/resources`: empty |
| A5 | Full core and UI tests pass, including `ScaledScrollPaneTest` | pass | Core: 1,523 tests, 0 failures, 2 skipped; UI: 2,000 tests, 0 failures, 4 skipped |
| A6 | `git diff --check` clean | pass | Exact command: no output |
| CI | PR `build`, `GUI acceptance`, and platform verification | pending | Awaiting new PR candidate SHA |

## Current slice

Implementation is complete in the isolated branch; create a follow-up PR that closes #1461 because the prior `Refs #1461` formatter-only PR merged before the package-wide criterion was met.

## Remaining blockers

- New PR `build`, `GUI acceptance`, and platform verification must pass on the pushed candidate SHA; then review comments must be resolved.

## Latest evidence

- `.\gradlew.bat :microproject_core:test --tests "com.microproject.timescale.ExtendedDateFormatTest" --console=plain`: BUILD SUCCESSFUL; 6 tests, 0 failures.
- `.\gradlew.bat :microproject_core:test --console=plain`: BUILD SUCCESSFUL; 1,523 tests, 0 failures, 2 skipped.
- `.\gradlew.bat :microproject_ui:test --console=plain`: BUILD SUCCESSFUL; 2,000 tests, 0 failures, 4 skipped; includes `ScaledScrollPaneTest`.
- Exact package Date/Calendar grep: 0 matches; `git diff --check`: clean.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: passed.
- `git diff origin/master -- modules/microproject_core/src/main/resources`: empty.
