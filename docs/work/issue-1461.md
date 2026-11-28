# Issue #1461 — ExtendedDateFormat を java.time で書き直す

State: COMPLETE; PR #1501 awaits required checks
Issue: https://github.com/tetsuji16/microProject/issues/1461
PR: https://github.com/tetsuji16/microProject/pull/1501
Approved scope clarification: https://github.com/tetsuji16/microProject/issues/1461#issuecomment-7232208674
Branch: `codex/issue-1461-package-grep` (pushes to existing PR branch `issue/1461-extended-date-format`)

## Goal and scope

Remove `java.util.Date` / `java.util.Calendar` from all production sources in `com.microproject.timescale` while preserving epoch-millisecond GMT schedule semantics, configured numeric timescale fields/patterns, interval boundaries, labels, and formatter behavior. Migrated `CalendarUtil`, `TimeInterval`, `TimeIterator`, `TimeScale`, and dependent callsites in script interval formatting, `CoordinatesConverter`, and `GanttRenderer`.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | `ExtendedDateFormatTest` passes before and after formatter rewrite | pass | Baseline test-only commit `e2b3b2198`: 3 tests, 0 failures; candidate: 6 tests, 0 failures |
| A2 | No `SimpleDateFormat` inheritance | pass | `rg -n 'extends SimpleDateFormat' ...ExtendedDateFormat.java`: 0 matches |
| A3 | Exact timescale package Date/Calendar grep returns 0 | pass | Exact issue `git grep` command: 0 matches |
| A4 | Four i18n keys and strings unchanged | pass | `git diff origin/master -- modules/microproject_core/src/main/resources`: empty |
| A5 | Full core and UI tests pass, including `ScaledScrollPaneTest` | pass | Core: 1,523 tests, 0 failures, 2 skipped; UI: 2,000 tests, 0 failures, 4 skipped |
| A6 | `git diff --check` clean | pass | Exact command: no output |
| CI | PR `build` and `GUI acceptance` | pending | Awaiting pushed candidate SHA |

## Current slice

Implementation is complete; existing PR #1501 is updated with the complete package migration. No further source changes are planned unless CI/review finds an issue.

## Remaining blockers

- PR #1501 `build` and `GUI acceptance` must pass on the pushed candidate head; then update issue/PR closure status truthfully.

## Latest evidence

- `:microproject_core:test --tests "com.microproject.timescale.ExtendedDateFormatTest" --console=plain`: BUILD SUCCESSFUL; 6 tests, 0 failures.
- `:microproject_core:test --console=plain`: BUILD SUCCESSFUL; 1,523 tests, 0 failures, 2 skipped.
- `:microproject_ui:test --console=plain`: BUILD SUCCESSFUL; 2,000 tests, 0 failures, 4 skipped; includes `ScaledScrollPaneTest`.
- Exact package Date/Calendar grep: 0 matches; `git diff --check`: clean.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: passed.
- `git diff origin/master -- modules/microproject_core/src/main/resources`: empty.
