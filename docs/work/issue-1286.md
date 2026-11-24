# Issue #1286 — Wrap Text 1/2: シートと Gantt の行ごとの高さ

State: IN REVIEW
Issue: https://github.com/tetsuji16/microProject/issues/1286
Base: current `master` after package layout #1310–#1314
Scope: per-row geometry for task sheet and Gantt; wrapping controls/persistence remain #1287.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| AC1 | No overrides preserve existing fixed-height behavior and UI tests pass | Complete | `RowHeightsTest`; `:microproject_ui:test` passed (1,906 tests) after row-count synchronization. |
| AC2 | A taller row keeps spreadsheet and Gantt row positions aligned | Complete | Shared `RowHeights`; `GanttRendererSelectedRowTest.taskRowHitTestingAndBarPositionUsePerRowHeights`. |
| AC3 | Click and drag below a taller row target the visible task | Complete | `GanttRendererSelectedRowTest.calendarClickAndDragBelowAnOverriddenRowTargetTheVisibleTasks`. |
| AC4 | All existing `getRowHeight()` call sites are accounted for | Complete | Migration table below. |
| AC5 | Required `build` and `GUI acceptance` checks pass | Pending exact candidate SHA | Run after pushing this integration commit. |

## Migration table

| Area / call sites | Disposition |
|---|---|
| `Gantt.java`, `GanttModel.java` | Row lookup, total height, and scroll geometry use `RowHeights`; the fixed height accessor remains for default-size/overscan calculations. Row count follows the active cache as it changes. |
| `GanttInteractor.java`, `GanttUI.java` | Pointer hit testing, calendar click/drag selection, and bar y positions use cumulative row coordinates. |
| `GanttRenderer.java`, `SpreadSheetRenderer.java` | Bars, annotations, selected rows, row backgrounds, and separators use `RowHeights`. |
| `GanttParams.java`, `GanttParamsImpl.java` | Expose row geometry to screen, print, and SVG renderers; defaults remain available for uniform row setup. |
| `TaskGanttSyncSupport.java`, `GanttView.java` | Apply row heights to the task JTable and the matching Gantt model. |
| `MicroProjectPrintService.java`, `GanttSVGRenderer.java` | Print bounds and SVG rendering use `GanttParamsImpl.getRowHeights()`; SVG receives the default-height table for its generated cache. |
| `ScrollPaneSynchronizer.java` | Reads one default row height only to choose a five-row wheel step; it does not convert row indices or positions. Viewport synchronization uses shared pixel coordinates. |
| `SpreadSheetParamsImpl.java`, `CommonSpreadSheet.java` | Keep uniform spreadsheet sizing for non-Gantt tables; the task Gantt sheet receives per-row heights via `TaskGanttSyncSupport`. |
| `TimeSpreadSheet.java`, `UsageDetailView.java` | Use the independent time-phased Usage grid and are outside this task's scope. |

## Root causes resolved

- Initial Gantt layout can paint before cache rows exist. `GanttRenderer.updateShapes` now exits for a zero-row geometry instead of requesting iterator `-1`.
- Insertions and deletions change cache size after `setCache`. `GanttModel` and `GanttParamsImpl` synchronize row counts when geometry is requested; shrinking the table removes overrides for removed rows.
- Gantt pointer and separator paths still used fixed-height arithmetic. They now use cumulative row coordinates.

## Verification

- `.\gradlew.bat :microproject_ui:compileJava :microproject_ui:compileTestJava --console=plain` — PASS on integrated current-master tree.
- `.\gradlew.bat :microproject_ui:test --tests 'com.microproject.ui.view.gantt.GanttRendererSelectedRowTest' --tests 'com.microproject.ui.view.views.RowHeightsTest' --console=plain` — PASS.
- `.\gradlew.bat :microproject_ui:test --console=plain` — PASS, 1,906 tests, before the final separator assertion and click/drag test were added; the focused command above covers those additions.
- `git diff --cached --check` — PASS.
- `build` and `GUI acceptance` — pending exact integrated candidate SHA.
- Physical GUI run — pending GUI acceptance workflow.

Next: push the non-force integration merge and require `build` plus `GUI acceptance` on that exact SHA.
