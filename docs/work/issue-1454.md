# Issue #1454 — Stabilize split assignment gap test

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1454
Contract frozen at: 2026-11-28, issue #1454
Baseline HEAD: `cdd19b3c955a0ab70a3d2dda50b3bdb1df41e3fd`
Baseline working tree: clean

## Goal and scope

Make `splitAssignmentGapDoesNotCountAsResourceOverload` deterministic across Monday through Friday by anchoring the project start and measuring the gap in two working days.

Out of scope: production `TeamPlannerService` behavior and unrelated test date changes.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | Monday-Friday: old 48-hour gap fails only Friday; two-workday gap passes all five | pass | `c42bda97f`; pre-fix focused run: Friday case only failed (13 tests, 1 failure); post-fix focused run: `BUILD SUCCESSFUL` |
| AC2 | Both `assertFalse` statements remain unchanged | pass | `c42bda97f`; scoped `git diff` review shows both assertion lines unchanged |
| AC3 | `TeamPlannerService.java` remains unchanged | pass | `c42bda97f`; `git diff --quiet origin/master -- .../TeamPlannerService.java` exit 0 |
| AC4 | `:microproject_core:test` passes | pass | `c42bda97f`; `:microproject_core:test --console=plain` → `BUILD SUCCESSFUL` |
| AC5 | `build` and `GUI acceptance` pass | open | pending |

## Current slice

**Next slice:** Commit and publish the verified test change, then wait for required PR checks.
**Done when:** AC5 has evidence from the PR head.
**Then:** Report the completed issue.

## Remaining blockers

- AC5 requires PR build and GUI acceptance checks.

## Latest evidence

- Tests/build (tested SHA `c42bda97fb119e6bf399ed2a7c90923df1f23427`): pre-fix `:microproject_core:test --tests "*TeamPlannerServiceTest" --console=plain` → Friday parameter failed (13 tests, 1 failure); post-fix focused suite → `BUILD SUCCESSFUL`; `:microproject_core:test --console=plain` → `BUILD SUCCESSFUL`; source inventory check exit 0; `git diff --check` clean.
- Physical GUI/manual: not applicable; no user-visible route changed
- CI: pending
