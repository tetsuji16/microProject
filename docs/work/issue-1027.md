# Issue #1027 — Switch GUI smoke to tagged journeys

State: COMPLETE
Issue: https://github.com/tetsuji16/microProject/issues/1027
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `25b534e927fc8963429c5208c83c8de7a96fc787` (#1029 merged)
Baseline working tree: clean

## Goal and scope

Make `smoke` an alias of `@GuiJourney`, remove the hand-maintained smoke patterns, add a one-tag-per-mechanism Gradle guard, and update policy docs. Preserve tests and assertions.

Out of scope: removing Robot tests, moving tests to L2, changing visual/full contents, and stable-release policy.
Blockers: none; prerequisite PR #1029 merged.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Smoke selected by journey tag; hand-maintained patterns removed | pass | PR #1031 merged; Gradle smoke/journeys use the same tag selection |
| A2 | Each InputMechanism appears exactly once; guard reports missing/duplicate | pass | Guard probe failed with expected missing and duplicate values |
| A3 | Every old smoke item classified in PR body | pass | PR #1031 body lists all 33 methods and two class entries, including stale selector mapping |
| A4 | Specified docs match new behavior | pass | TEST_PLAN and three GUI/test strategy docs updated |
| A5 | Compile, dry-run, and negative guard probe pass | pass | Local compile/dry-runs and negative guard probe passed |
| A6 | Exact PR smoke CI and 3 OS Build pass | pass | Exact head `36ee88f8`; hosted GUI smoke and Linux/Windows/macOS Build passed |

## Current slice

**Next slice:** None.
**Done when:** All acceptance criteria passed and PR #1031 merged.
**Then:** Complete.

## Remaining blockers

- None.

## Latest evidence

- Baseline clean; prerequisite #1029 merged as `25b534e9`.
- The stale selector `physicalGanttWheelScrollsByOneOwnerStep` maps to the existing `physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep`, now tagged `GANTT_WHEEL`.
- Added missing mechanisms using existing Robot tests: `TASK_ROW_DRAG`, `TASK_ROW_CLICK`, and `GANTT_WHEEL`; no Robot test or assertion added, removed, disabled, or weakened.
- Local checks passed: `:microproject_ui:compileGuiTestJava`; smoke and journeys `--dry-run`; negative guard probe; `git diff --check`; `python scripts/tests/test_release_workflow_policy.py` (12 tests).
- First PR run showed the GUI selector guards assumed `@Test` immediately preceded methods. Both PowerShell selection and Python test now allow additional method annotations.
- Exact PR head `36ee88f8c8d14ae55b6d68091bbcb6662b27c9c0`: GUI smoke passed ([run 42656824611](https://github.com/tetsuji16/microProject/actions/runs/42656824611)); Linux/Windows/macOS Build passed ([run 42656825012](https://github.com/tetsuji16/microProject/actions/runs/42656825012)).
- [PR #1031](https://github.com/tetsuji16/microProject/pull/1031) merged as `2b55231d8fb540c9cd60b4a03cc47d16237bb3a3`; issue #1027 closed.
