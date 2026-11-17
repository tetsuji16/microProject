# Issue #1179 — Resource Graph exact peaks and bar overlap

## Acceptance

| ID | Requirement | Status / evidence |
|---|---|---|
| A1 | Bar Styles exposes and persists a 0–100 percent bar overlap; rendered bars use that overlap. | Implemented; settings and interval-width tests added. |
| A2 | Peak Units uses the maximum simultaneous contour units inside each graph period. | Implemented in `ChartModel` from assignment work contour intervals intersected with each assignment's effective calendar; `ResourceGraphDataTest` covers overlap, nonoverlapping shifts, weekends, holidays, and actual month boundaries. |

Project-level regressions create real assignments and route them through `ChartModel.computeResourceGraph`. They check the plotted and rendered peak for simultaneous assignments, exclude default assignments for unassigned tasks, and verify that adjacent shifts, overnight/weekend gaps, and a holiday do not create false peaks. Timescale bucket ends come from `TimeIterator` boundaries (including the final bucket), covering the March 2024 DST transition and variable month ends. Dependency-split contour fragments preserve effective assignment units; the core regression checks both fragment units and total assignment work.

Microsoft documents the `PercentBarOverlap` property as an integer from 0 to 100 in [ResourceGraphBarStyles](https://learn.microsoft.com/en-us/office/vba/api/project.application.resourcegraphbarstyles). Resource Graph Peak Units are the highest allocation percentages that occur in a period, per [View resource workloads and availability](https://support.microsoft.com/en-US/project/view-resource-workloads-and-availability-in-project-desktop). The 100% default preserves the previous full-width overlay rendering.

## Scope and baseline

- Scope: issue #1179 only; no physical Robot run because another agent is using the shared desktop.
- Worktree: isolated `codex/issue-1179-resource-graph`; baseline `a1add999f289676c5f50148a240ea7445694eb3d`.
- Baseline status: clean.
- No acceptance criteria deferred.

## Current verification / next action

- Core scheduling regressions passed: `:microproject_core:test`.
- Targeted Resource Graph Gradle tests passed: `:microproject_ui:test --tests "com.microproject.pm.graphic.chart.ResourceGraphDataTest" --tests "com.microproject.pm.graphic.chart.ResourceGraphSettingsTest" --console=plain`.
- Test-task inventory and GUI source compilation passed: `verifyTestTasksEnabled :microproject_ui:compileGuiTestJava --console=plain`.
- Hosted Robot journey added for the real Bar Styles ribbon route, physical spinner entry, rendered interval widths, and workspace save/restore. GUI source compiles with `:microproject_ui:compileGuiTestJava --console=plain`; hosted GUI acceptance is pending on the updated PR head.
- PR: https://github.com/tetsuji16/microProject/pull/1191. Never run local physical Robot coverage while the shared desktop is occupied.
