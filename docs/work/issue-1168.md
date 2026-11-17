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
- A4/A5 hosted split Robot proof is pending on the rebased candidate. Prior exact-SHA GUI smoke `44009902075` passed, but target split runs `44010405763` and `44010784956` both failed before any gesture at `findResizeHandle`: the ribbon Gantt displayed 2026-11-22 onward while the fixture tasks were scheduled 2026-11-17..19. Both screenshots show the intended MainRibbonFrame, with no foreign foreground overlap. The stable cause is a fixture/project-start and viewport mismatch after switching the journey to the real ribbon Gantt. The fixture now sets an explicit project start and scrolls through the Gantt's regular horizontal scrollbar, then waits for the complete task bar to be within the visible viewport before any gesture. Re-run all required journeys on one final SHA; do not treat prior failures as passed evidence. Local Robot is not used because another user is using the desktop.

## Latest evidence

- Baseline inspection: task split model, MPO sidecar, MPP/MSPDI mapping, Gantt drawing, popup split tool, and Undo/Redo journey exist at baseline SHA.
- Microsoft interaction source: https://support.microsoft.com/en-us/project/split-a-task — Task > Schedule > Split Task; click task bar at split point and drag second segment to resume date; sections may be joined by dragging together. Treat as document-derived compatibility behavior.
- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --tests "com.microproject.ui.ribbon.RibbonCommandCatalogTest" --tests "com.microproject.ui.ribbon.MspRibbonSpecDiffTest" --tests "com.microproject.menu.RibbonStructureTest" --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --tests "com.microproject.pm.graphic.gantt.GanttInteractorDragTest" --console=plain` — PASS; `./gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — PASS; `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.ScheduleServiceSplitTest" :microproject_exchange:test --tests "com.microproject.exchange.MpoHistoricalFixtureTest" --tests "com.microproject.exchange.MpoFileImporterTest" --console=plain` — PASS.
- Microsoft source: https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc, https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016, https://learn.microsoft.com/en-us/office-project/xml-data-interchange/timephaseddata-element?view=project-client-2016, and https://learn.microsoft.com/en-us/office/vba/api/project.task.split. Task `Stop`/`Resume` is a single pair; task `TimephasedData` and assignment work contours have distinct series/ownership. See A3 above; no split roundtrip is inferred from assignment work data.
- Exact SHA `0db812b2a98f6a7ce6e149fffcb4e28c5dc74ff3` after #1195: all-OS Build `44009902088` passed; hosted GUI smoke `44009902075` passed and included the prior dependency drag journey. Two split-focused runs `44010405763` and `44010784956` reproduced the fixture/viewport failure described above.
- Current candidate includes #1186 (`8ba205a20`) and the focused fixture/viewport correction. `compileGuiTestJava` and `git diff --check` pass locally. Hosted split, read-only, dependency journey, and all-OS Build still need to run on one final SHA after this correction.
- Physical GUI/manual: pending hosted GUI workflow; local desktop intentionally not used due contention.
- CI: pending exact candidate SHA.
