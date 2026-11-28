# Issue #1458 — タイムスケールとジョブのイベント型を enum と型付きの値にする

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1458
Contract frozen at: issue #1458 and comments retrieved 2026-11-28; no comments
Baseline HEAD: `c7a4d021c4ab3dc5a8912f0d0090911db708328a`
Baseline working tree: clean isolated worktree; original checkout has pre-existing user changes

## Goal and scope

Replace time-scale integer flags with `ChangeType`, update converter/listener usage, make the specified event and transaction contracts explicit, and preserve visible-range behavior.

Out of scope: tick/label behavior; transaction timing/rollback; `ObjectSelectionEvent` constructor; persistence format changes.
Blockers: none; approved determination is recorded in #1437.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Old constants absent; `getType()` returns enum | pass | `git grep` 0 matches; `TimeScaleEvent.getType()` is `ChangeType` |
| A2 | Event not stored in POD/MPO/workspace | pass | resource/exchange/persistence searches 0; workspace stores scale index/origin/end |
| A3 | `ScaledScrollPaneTest` passes | pass | UI focused suite, 8 tests, 0 failures |
| A4 | Core and UI full test suites pass | pass | core 1,510 and UI 2,000 tests, 0 failures |
| A5 | `git diff --check` clean | pass | exit 0 |

## Current slice

**Next slice:** review final diff, commit, open PR, and wait for required CI.
**Done when:** PR checklist is `OK` and PR checks pass on the pushed head.
**Then:** report PR and remaining required checks; do not merge.

## Remaining blockers

- Required PR `build` and `GUI acceptance` checks have not run yet.

## Latest evidence

- Tests: focused core job, focused UI time-scale, full core, and full UI tests all succeeded on the current uncommitted tree; final log files are under `%TEMP%\microproject-issue1458-final`.
- Physical GUI/manual: no user command route changed; time-scale behavior covered by `ScaledScrollPaneTest`.
- Persistence: no resource, exchange, or core persistence references; workspace state stores only current scale index, origin, and end.
- CI: pending PR candidate SHA.
