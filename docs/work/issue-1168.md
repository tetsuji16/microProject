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
| A1 | Core split segment model and duration/schedule semantics agree with Microsoft desktop docs; edge behavior has focused regression coverage. | Existing coverage passes; semantic compatibility is only partial | `ScheduleServiceSplitTest` covers gap/overlap/extension, clearing generated intervals, and undo/redo; no changes to core semantics in this slice. Microsoft documents the interaction and resulting interruption, not every calendar edge. |
| A2 | MPO stores/reloads split segments and reads older files with defaults. | Existing coverage passes | `MpoFileImporterTest` includes split provenance, legacy records without provenance, and roundtrip cases; `MpoHistoricalFixtureTest.v024TaggedWriterFixturePreservesLiteralNativePayloadsAndReopens` opens a v0.24 fixture with a manual split interval. |
| A3 | MPP and MSPDI import/export map split segments; fixtures cover interoperability and preservation. | **Open — not complete** | `MPXConverter` maps task `Stop`/`Resume` and assignment timephased work, but this does not establish lossless mapping of arbitrary task split intervals in both directions. Microsoft Task XML schema has a single Stop/Resume pair; no multi-segment task split collection is documented. No verified roundtrip fixtures for split intervals. |
| A4 | Gantt renders split segments and interactive tool follows Microsoft interaction; Undo/Redo and persistence work. | Interaction implemented; hosted physical proof pending | Split mode now requires a drag from the split point through the resume date. `GanttInteractorDragTest.splitActionRequiresDraggingToAResumeDate` checks the input gate; the Robot journey uses the Task ribbon and drags to a resume date, then checks undo/persistence. Read-only project entry is rejected by the shared mode gate. |
| A5 | Task ribbon `TaskSplit` activates the interactive Gantt split tool and is no longer a deferred MISSING_CONTROLS exception. | Implemented; hosted physical proof pending | Ribbon action and existing popup use the same project-level `canStartSplitMode` gate. Split mode applies to the bar chosen after entry, so prior selection does not restrict the target; read-only task targets are rejected by `TaskCommandGateway`. Focused Robot tests cover the enabled route and read-only project button state; hosted target runs pending. |

## Current slice

**Current slice:** Complete the Task ribbon path and make Gantt split interaction follow Microsoft's documented click-at-split-point, drag-to-resume-date workflow.
**Done when:** Ribbon action is enabled on Gantt views only, activates split mode, is mapped to idMso `TaskSplit`, unit tests and a Robot journey cover the ribbon-to-drag route, and hosted GUI evidence succeeds.
**Then:** Continue A3 interoperability work; keep original criteria open until every row is proven.

## Remaining blockers

- A3 remains an implementation and fixture gap: multi-segment MPP/MSPDI import/export has not been proven or implemented.
- A4/A5 hosted split Robot run is pending. The first targeted run on `0748c0029` failed before interaction because the GUI journey had not initialized a ribbon window (`projectWindow` null). The fixture also originally placed the ribbon action and gesture on distinct Gantt instances; the journey now uses the visible ribbon document's Gantt for both. The read-only disabled-state Robot passed on `3424bd3a8` (run `44007573699`). Local Robot is not used because another user is using the desktop; use hosted Windows GUI acceptance.

## Latest evidence

- Baseline inspection: task split model, MPO sidecar, MPP/MSPDI mapping, Gantt drawing, popup split tool, and Undo/Redo journey exist at baseline SHA.
- Microsoft interaction source: https://support.microsoft.com/en-us/project/split-a-task — Task > Schedule > Split Task; click task bar at split point and drag second segment to resume date; sections may be joined by dragging together. Treat as document-derived compatibility behavior.
- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --tests "com.microproject.ui.ribbon.RibbonCommandCatalogTest" --tests "com.microproject.ui.ribbon.MspRibbonSpecDiffTest" --tests "com.microproject.menu.RibbonStructureTest" --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --tests "com.microproject.pm.graphic.gantt.GanttInteractorDragTest" --console=plain` — PASS; `./gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — PASS; `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.ScheduleServiceSplitTest" :microproject_exchange:test --tests "com.microproject.exchange.MpoHistoricalFixtureTest" --tests "com.microproject.exchange.MpoFileImporterTest" --console=plain` — PASS.
- Microsoft source: https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc and https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016; schemas expose task Stop/Resume fields, not a repeated split interval list. This is evidence for a compatibility limit, not proof that task split data can never be represented through assignment contours.
- CI: initial Build run `44004084267` failed due to ribbon registration and icon-size omissions. Those fixes passed all OS Build on `0748c0029` (run `44005858379`); the read-only gateway changes have not yet passed CI on an exact candidate SHA.
- Hosted physical GUI: `44005862276` on `0748c0029` failed pre-gesture with the missing `projectWindow` setup; read-only target `44007573699` passed on `3424bd3a8`; split-target run `44007903963` is pending.
- Physical GUI/manual: pending hosted GUI workflow; local desktop intentionally not used due contention.
- CI: pending exact candidate SHA.
