# Issue #1390 — TimeUtil.toUTCString を java.time にし、使っていない共有の SimpleDateFormat を消す

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1390
Contract frozen at: 2026-11-25 (issue body as written)
Baseline HEAD: `8b38c7369`
Baseline working tree: clean except an incidental rewrite of
`samples/CCPM path comparison 日本語.mpo`, which is left untouched.

## Goal and scope

`TimeUtil.toUTCString` builds a `SimpleDateFormat` on every call; replace it with
one immutable, shared `DateTimeFormatter` and delete the unused
`CalendarDefinition.CalendarIterator.f` field that still holds a shared
`SimpleDateFormat`.

Out of scope: other `TimeUtil` methods, `DateTime` format helpers, and the
1582 Gregorian-cutover difference accepted by ADR-0003.
Blockers: none (ADR-0003 from #1389 is merged as `8b38c7369`).

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | The step-1 tests pass before and after the replacement, and the PR records both results | pass | pre-change run 2026-11-25T11:27:03Z at `8b38c7369`: `TimeUtilTest` 3 tests / 0 failures; post-change run at the same tree with the new implementation: 3 tests / 0 failures |
| A2 | `rg -n "SimpleDateFormat" .../core/time/TimeUtil.java` is empty | pass | `grep -c` → 0 |
| A3 | `rg -n "static SimpleDateFormat f" .../calendar/CalendarDefinition.java` is empty | pass | `grep -c` → 0 |
| A4 | `FormatConcurrencyTest` and `:microproject_core:test` pass | pass | `FormatConcurrencyTest` 2 tests / 0 failures; `:microproject_core:test` 1466 tests, 2 skipped, 0 failures |
| A5 | The PR's `build` and `GUI acceptance` checks pass | pass | PR #1396 at `017cd8dd2`: `build`, `verify` (windows/ubuntu/macos), `GUI acceptance` and CodeRabbit all pass |

## Current slice

**Next slice:** push the branch, open the pull request, and read the required checks.
**Done when:** `build` and `GUI acceptance` succeed on the pushed head.
**Then:** merge and close the issue.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `.\gradlew.bat :microproject_core:test --console=plain` → BUILD
  SUCCESSFUL, 1466 tests / 2 skipped / 0 failures at `8b38c7369` + working tree.
- Physical GUI/manual: not applicable — no Swing or user-facing route changes.
- CI: PR #1396 `build` / `verify` (3 OS) / `GUI acceptance` / CodeRabbit pass at `017cd8dd2`.
