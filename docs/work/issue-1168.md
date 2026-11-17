# Issue #1168 — Split Task

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1168
Contract frozen at: 2026-11-17, issue revision 1
Baseline HEAD: `a1add999f289676c5f50148a240ea7445694eb3d`
Baseline working tree: clean, isolated worktree

## Goal and scope

Implement Microsoft Project desktop Task > Schedule > Split Task (idMso `TaskSplit`) across the task model, schedule/duration behavior, MPO read/write with old-file defaults, MPP/MSPDI exchange, segmented Gantt rendering, interactive Split Task, and ribbon wiring.

Out of scope: Project Server/Online; unrelated ribbon compatibility gaps.
Blockers: none identified.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Core split segment model and duration/schedule semantics agree with Microsoft desktop docs; edge behavior has focused regression coverage. | Existing, not independently audited | Current master `NormalTask`, `TaskSplitInterval`, `ScheduleServiceSplitTest`; scope not changed by this slice |
| A2 | MPO stores/reloads split segments and reads older files with defaults. | Existing, not independently audited | Current master MPO sidecar and Gantt GUI roundtrip; old-file default still needs explicit audit |
| A3 | MPP and MSPDI import/export map split segments; fixtures cover interoperability and preservation. | Open | Existing conversion code found; full inbound/outbound and boundary evidence pending |
| A4 | Gantt renders split segments and interactive tool follows Microsoft interaction; Undo/Redo and persistence work. | Partial | Existing Gantt split mode + Robot journey; interaction contract needs comparison to docs and physical GUI evidence |
| A5 | Task ribbon `TaskSplit` activates the interactive Gantt split tool and is no longer a deferred MISSING_CONTROLS exception. | Pass for wiring; exact hosted physical route pending | `MspRibbonOrderDiffTest` + `RibbonCommandCatalogTest` pass on candidate commit; `GanttBarDateDragGuiAcceptanceTest` now uses Task ribbon route, hosted run pending |

## Current slice

**Next slice:** Wire a Task ribbon command to the existing Gantt split-mode transition and remove its deferred ribbon exception.
**Done when:** Ribbon action is enabled on Gantt views only, activates existing split mode, control is mapped to idMso `TaskSplit`, focused contract test passes, and a Robot journey uses the ribbon route.
**Then:** Audit and complete A3, followed by A1/A2/A4 evidence gaps; keep original criteria open until proven.

## Remaining blockers

- A1/A2/A3/A4 lack a current acceptance audit against original issue contract; existing implementation is not by itself proof of completion.
- Physical GUI route cannot run locally because another user is using Robot desktop; use hosted Windows GUI acceptance.

## Latest evidence

- Baseline inspection: task split model, MPO sidecar, MPP/MSPDI mapping, Gantt drawing, popup split tool, and Undo/Redo journey exist at baseline SHA.
- Microsoft interaction source: https://support.microsoft.com/en-us/project/split-a-task — Task > Schedule > Split Task; click task bar at split point and drag second segment to resume date; sections may be joined by dragging together. Treat as document-derived compatibility behavior.
- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --tests "com.microproject.ui.ribbon.RibbonCommandCatalogTest" --console=plain` — PASS; `./gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — PASS. Candidate SHA to be added after commit.
- Physical GUI/manual: pending hosted GUI workflow; local desktop intentionally not used due contention.
- CI: pending exact candidate SHA.
