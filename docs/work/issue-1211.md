# Issue #1211 — Equal startup Gantt view

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1211
Contract frozen at: 2026-11-17, issue creation
Baseline HEAD: `d6ef9b53064eb1414498069c5d601fadc26ccc18`
Baseline working tree: clean

## Goal and scope

On first display without saved view workspace, give the task table and Gantt equal widths and fit the project date span inside the Gantt viewport. Preserve stored workspace state.

Out of scope: MPO format changes and non-Gantt view defaults.
Blockers: required `build` and `GUI acceptance` checks.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | First display divides available width equally | pass | Focused divider arithmetic assertion; UI lifecycle suite |
| A2 | Project start and finish fit in initial Gantt viewport | implemented | `SelectedTasksTimescaleZoom.zoomProject` scheduled after viewport sizing |
| A3 | Saved workspace divider and zoom remain restored | pass by flow | Only no-workspace startup requests initial fit; existing restore path retained |
| A4 | UI regression checks and PR required workflows pass | open | pending |

## Current slice

**Next slice:** review, commit, publish PR, and run required CI.
**Done when:** PR checks pass and merge completes.
**Then:** close the issue with the merged change and exact evidence.

## Remaining blockers

- Physical GUI acceptance and required CI checks are pending.

## Latest evidence

- Tests/build: `:microproject_ui:test --tests "com.microproject.pm.graphic.views.SplittedViewLifecycleTest" :microproject_ui:compileGuiTestJava --console=plain` — BUILD SUCCESSFUL.
- Whitespace: `git diff --check` — clean.
- Physical GUI/manual: pending.
- CI: pending.
