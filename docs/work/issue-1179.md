# Issue #1179 — Resource Graph exact peaks and bar overlap

## Acceptance

| ID | Requirement | Status / evidence |
|---|---|---|
| A1 | Bar Styles exposes and persists a 0–100 percent bar overlap; rendered bars use that overlap. | Implemented; settings and interval-width tests added. |
| A2 | Peak Units uses the maximum simultaneous contour units inside each graph period. | Implemented in `ChartModel` from assignment work contour intervals intersected with each assignment's effective calendar; `ResourceGraphDataTest` covers overlap, nonoverlapping shifts, weekends, holidays, and actual month boundaries. |

Project-level regressions create real assignments and route them through `ChartModel.computeResourceGraph`. They check the plotted and rendered peak for simultaneous assignments, exclude default assignments for unassigned tasks, and verify that adjacent shifts, overnight/weekend gaps, and a holiday do not create false peaks. Peak over-allocation compares work and availability at matching instants, so a period-average capacity does not color a bar incorrectly when availability changes within a period. Timescale bucket ends come from `TimeIterator` boundaries (including the final bucket), covering the March 2024 DST transition and variable month ends. Dependency-split contour fragments preserve effective assignment units; the core regression checks both fragment units and total assignment work.

Microsoft documents the `PercentBarOverlap` property as an integer from 0 to 100 in [ResourceGraphBarStyles](https://learn.microsoft.com/en-us/office/vba/api/project.application.resourcegraphbarstyles). Resource Graph Peak Units are the highest allocation percentages that occur in a period, per [View resource workloads and availability](https://support.microsoft.com/en-US/project/view-resource-workloads-and-availability-in-project-desktop). The 100% default preserves the previous full-width overlay rendering.

## Scope and baseline

- Scope: issue #1179 only; GUI Robot validation runs hosted because another agent is using the shared desktop.
- Worktree: isolated `codex/issue-1179-resource-graph`; baseline `a1add999f289676c5f50148a240ea7445694eb3d`.
- Baseline status: clean.
- No acceptance criteria deferred.

The hosted GUI route found that cached-view workspace restore introduced a regression: the new `DocumentFrame.restoreWorkspace` pass re-applied an existing `ChartView` after main-view restore had cleared its legend selection. The test recorded `selectedRow=false` and no plot datasets after restore. `DocumentFrame` now snapshots each existing chart's `ChartLegend.selectedObjects` and the resources selected in the actual legend tree before changing the main view, then reapplies both after restoring the cached chart. The regular chart legend now explicitly allows discontiguous selection; Resource Graph keeps its intentional single-resource mode. A unit regression drives a real task selection event and then a two-resource legend selection, verifying task and multi-resource selection survive workspace restore on the non-Resource-Graph chart. The Resource Graph hosted route verifies its physically selected legend row and rendered dataset after restore.

## Current verification / next action

- Core scheduling regressions passed: `:microproject_core:test`.
- Targeted Resource Graph Gradle tests passed: `:microproject_ui:test --tests "com.microproject.pm.graphic.chart.ResourceGraphDataTest" --tests "com.microproject.pm.graphic.chart.ResourceGraphSettingsTest" --console=plain`.
- Test-task inventory and GUI source compilation passed: `verifyTestTasksEnabled :microproject_ui:compileGuiTestJava --console=plain`.
- `:microproject_ui:test --tests 'com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest' --console=plain` passed, including task plus multiple resource selection restore.
- Hosted GUI smoke passed on head `6d47355e3`. Focused Resource Graph run `44025010114` failed on pre-fix head `692830cdc` after confirming the warning was visible and dismissed; post-restore diagnostics showed `selectedRow=false`, `datasets=[]`. Native diagnostics showed the app foreground and no foreign-window overlap. Root cause was a stale selection list for direct tree input and single-selection mode on the regular chart. The current candidate fixes both and is being reverified.
- PR: https://github.com/tetsuji16/microProject/pull/1191. Never run local physical Robot coverage while the shared desktop is occupied.
