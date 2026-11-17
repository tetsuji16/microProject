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
| A3 | MPP and MSPDI import/export map split segments; fixtures cover interoperability and preservation. | **Open — not complete** | Microsoft's Task schema allows one optional `Stop` and one optional `Resume` value. Its repeatable Task `TimephasedData` describes specified task series such as percent complete, while assignment `TimephasedData` describes assignment work. Neither documents a task-owned collection of split intervals. Existing `MPXConverter` maps Stop/Resume; MPP fixture `mpp9timephasedsegments.mpp` proves a custom assignment work contour only, not task split preservation. No lossless arbitrary or multi-gap roundtrip fixture is established. |
| A4 | Gantt renders split segments and interactive tool follows Microsoft interaction; Undo/Redo and persistence work. | Interaction implemented; hosted physical proof pending | Split mode now requires a drag from the split point through the resume date. `GanttInteractorDragTest.splitActionRequiresDraggingToAResumeDate` checks the input gate; the Robot journey uses the Task ribbon and drags to a resume date, then checks undo/persistence. Read-only project entry is rejected by the shared mode gate. |
| A5 | Task ribbon `TaskSplit` activates the interactive Gantt split tool and is no longer a deferred MISSING_CONTROLS exception. | Implemented; hosted physical proof pending | Ribbon action and existing popup use the same project-level `canStartSplitMode` gate. Split mode applies to the bar chosen after entry, so prior selection does not restrict the target; read-only task targets are rejected by `TaskCommandGateway`. Focused Robot tests cover the enabled route and read-only project button state; hosted target runs pending. |

## Current slice

**Current slice:** Complete the Task ribbon path and make Gantt split interaction follow Microsoft's documented click-at-split-point, drag-to-resume-date workflow.
**Done when:** Ribbon action is enabled on Gantt views only, activates split mode, is mapped to idMso `TaskSplit`, unit tests and a Robot journey cover the ribbon-to-drag route, and hosted GUI evidence succeeds.
**Then:** Continue A3 interoperability work; keep original criteria open until every row is proven.

## Remaining blockers

- A3 remains an implementation and fixture gap: multi-segment MPP/MSPDI import/export has not been proven or implemented. Microsoft sources: [Task element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc), [Tasks schema](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016), [TimephasedData element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/timephaseddata-element?view=project-client-2016), and [Task.Split method](https://learn.microsoft.com/en-us/office/vba/api/project.task.split). The schema exposes single Stop/Resume properties and timephased values; it does not document a repeated task split record. This supports the current interoperability limit, not a claim that every MPP encoding has been examined.
- A4/A5 hosted split Robot proof is pending on the final candidate. GUI smoke passed on `0db812b` (44009902075) and `5d1f2b2` (44011603159). Split target runs `44010405763` and `44010784956` first exposed an offscreen task bar (project auto-scheduled the June fixture into November); `44011623123` and `44012191839` then exposed an offscreen earlier resize target. Their screenshots show the intended MainRibbonFrame and no foreign foreground overlap; the dragged date auto-scrolled to March and changed the task to April. A viewport preflight was added; runs `44012714923` and `44013101846` reproduced the preflight failure with task start June 10, target point `(730,297)`, viewport `(753,287,252x374)`. The common cause is using task start as project chart origin: even when the bar itself is fully visible, the requested three-day earlier date lies before the viewport. The new fixture separates project origin (June 1) from task start (June 10), scrolls the regular Gantt horizontal scrollbar to include the task bar plus both resize destinations, and checks each dispatch point remains inside the viewport. Re-run all required journeys on one final SHA; do not treat prior failures as passed evidence. Local Robot is not used because another user is using the desktop.

## Latest evidence

- Baseline inspection: task split model, MPO sidecar, MPP/MSPDI mapping, Gantt drawing, popup split tool, and Undo/Redo journey exist at baseline SHA.
- Microsoft interaction source: https://support.microsoft.com/en-us/project/split-a-task — Task > Schedule > Split Task; click task bar at split point and drag second segment to resume date; sections may be joined by dragging together. Treat as document-derived compatibility behavior.
- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --tests "com.microproject.ui.ribbon.RibbonCommandCatalogTest" --tests "com.microproject.ui.ribbon.MspRibbonSpecDiffTest" --tests "com.microproject.menu.RibbonStructureTest" --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --tests "com.microproject.pm.graphic.gantt.GanttInteractorDragTest" --console=plain` — PASS; `./gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — PASS; `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.ScheduleServiceSplitTest" :microproject_exchange:test --tests "com.microproject.exchange.MpoHistoricalFixtureTest" --tests "com.microproject.exchange.MpoFileImporterTest" --console=plain` — PASS.
- Microsoft source: https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc, https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016, https://learn.microsoft.com/en-us/office-project/xml-data-interchange/timephaseddata-element?view=project-client-2016, and https://learn.microsoft.com/en-us/office/vba/api/project.task.split. Task `Stop`/`Resume` is a single pair; task `TimephasedData` and assignment work contours have distinct series/ownership. See A3 above; no split roundtrip is inferred from assignment work data.
- On SHA `36aa53e382f4281310e6d757bc329d97ad2125bb` (including #1196), GUI smoke `44012684132` and all-OS Build `44012684355` are pending at packet update. Target split runs `44012714923` and `44013101846` reproduce the same preflight mismatch. Current unpushed correction has project origin June 1, task start June 10, and aligns the scroll range to contain the task plus requested resize dates. `compileGuiTestJava` and `git diff --check` pass locally. Re-run split, read-only, dependency journey, and all-OS Build on one final SHA.
- Physical GUI/manual: pending hosted GUI workflow; local desktop intentionally not used due contention.
- CI: pending exact candidate SHA.
