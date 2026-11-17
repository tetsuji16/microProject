# Issue #1179 — Resource Graph exact peaks and bar overlap

## Acceptance

| ID | Requirement | Status / evidence |
|---|---|---|
| A1 | Bar Styles exposes and persists a 0–100 percent bar overlap; rendered bars use that overlap. | Implemented; settings and interval-width tests added. |
| A2 | Peak Units uses the maximum simultaneous contour units inside each graph period. | Implemented in `ChartModel` from assignment work contour intervals; `ResourceGraphDataTest` covers overlap and period boundaries. |

The project-level regression creates two real assignments on the same resource, routes them through `ChartModel.computeResourceGraph`, and checks both the plotted data series and rendered bar dataset report 2.0 peak units.

Microsoft documents the `PercentBarOverlap` property as an integer from 0 to 100 in [ResourceGraphBarStyles](https://learn.microsoft.com/en-us/office/vba/api/project.application.resourcegraphbarstyles). Resource Graph Peak Units are the highest allocation percentages that occur in a period, per [View resource workloads and availability](https://support.microsoft.com/en-US/project/view-resource-workloads-and-availability-in-project-desktop). The 100% default preserves the previous full-width overlay rendering.

## Scope and baseline

- Scope: issue #1179 only; no physical Robot run because another agent is using the shared desktop.
- Worktree: isolated `codex/issue-1179-resource-graph`; baseline `a1add999f289676c5f50148a240ea7445694eb3d`.
- Baseline status: clean.
- No acceptance criteria deferred.

## Current verification / next action

- Targeted Gradle task passed: `:microproject_ui:test --tests "com.microproject.pm.graphic.chart.ResourceGraphDataTest" --tests "com.microproject.pm.graphic.chart.ResourceGraphSettingsTest" --rerun-tasks --console=plain`.
- Hosted Robot journey added for the real Bar Styles ribbon route, physical spinner entry, rendered interval widths, and workspace save/restore. GUI source compiles with `:microproject_ui:compileGuiTestJava --console=plain`; hosted GUI acceptance is pending on the updated PR head.
- PR: https://github.com/tetsuji16/microProject/pull/1191. Never run local physical Robot coverage while the shared desktop is occupied.
