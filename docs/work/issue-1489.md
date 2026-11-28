# Issue #1489 — 遅い日付の後退計算を参照時まで遅延する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1489
Contract frozen at: 2026-11-28; latest implementation-boundary comment https://github.com/tetsuji16/microProject/issues/1489#issuecomment-7232497266
Baseline HEAD: `4f6247dad7bc277ff38ce345af2f6b3ded0bbf43`
Baseline working tree: clean isolated worktree; primary checkout has unrelated pre-existing changes

## Goal and scope

Defer late-date recomputation on forward-scheduled projects without reverse-scheduled tasks until the first late-date/slack read; preserve immediate values, pass state, and schedule/Undo behavior. Keep eager passes on projects with reverse-scheduled (ALAP) tasks because late bounds feed their forward dates.

Out of scope: changing forward schedule/dependency meaning, project boundary meaning, bulk insertion #1178, GUI display behavior.
Blockers: required PR CI. Open PR preflight: #1500/#1501 checks passed; #1497 was in progress with no failures and no actionable review comments at intake.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| C1 | Deterministic counter proves each unobserved link addition leaves the backward pass pending; first late read runs it once | pass | On unchanged master `4f6247d`, new regression test failed `expected 1, was 2`; post-change `TaskDependencyInvalidationTest` passes on `c36264c0b`, checking pass=1 after each link, pass=2 on first late read and no repeat |
| C2 | First late read refreshes tasks outside dependency closure; repeat reads do not calculate again | pass | `TaskDependencyInvalidationTest.extendingCriticalPathRefreshesLateDatesOutsideTheChangedDependencyClosure` and new per-pass regression pass on `c36264c0b` |
| C3 | total/free slack use current late dates and match a full recalculation | pass | New regression on `c36264c0b` asserts slack reads do not repeat pass and both values match after `project.recalculate()` |
| C4 | existing schedule equivalence and Undo/late-date behavior pass | pass | `IncrementalScheduleEquivalenceTest` and `extendingCriticalPathRefreshesLateDatesOutsideTheChangedDependencyClosure` passed in full core suite on `c36264c0b` |
| C5 | core module test and diff check pass | pass | `:microproject_core:test` on `c36264c0b`: 1511 tests, 0 failures, 2 skipped; `git diff --check` empty |
| C6 | PR build and GUI acceptance pass | open | Pending exact PR head |

## Current slice

**Next slice:** Stage intended files, commit, push, create PR, and wait for required checks/reviews.
**Done when:** PR checklist returns `OK`; required checks pass on the exact pushed SHA; review comments are resolved.
**Then:** Report PR URL and evidence to the parent; do not merge.

## Remaining blockers

- C6: PR `build` and `GUI acceptance` checks.

## Latest evidence

- Tests/build: Tested SHA: `c36264c0b`. Focused `TaskDependencyInvalidationTest` and `com.microproject.schedule.criticalpath.*` both `BUILD SUCCESSFUL`; full `:microproject_core:test` `BUILD SUCCESSFUL` (1511 tests, 0 failures, 2 skipped); includes `IncrementalScheduleEquivalenceTest`.
- Static checks: `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` exit 0; `git diff --check` empty.
- Physical GUI/manual: not applicable; no user-visible route changes.
- CI: pending PR creation.
