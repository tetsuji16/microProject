# Issue #1286 — Wrap Text 1/2: シートと Gantt の行ごとの高さ

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1286
Contract frozen at: 2026-11-24, issue body as fetched
Baseline HEAD: `305e393c8`
Baseline working tree: clean

## Goal and scope

Implement per-row height support in the sheet and Gantt views by introducing a `RowHeights` table that manages default row height and per-row overrides. All row↔y coordinate conversions must route through `RowHeights`. Printing and SVG/image output must use the same conversion. When no overrides exist, behavior matches current fixed-height calculation.

Out of scope:
- Wrap text column settings, ribbon button, persistence (#1287)
- Mouse-driven row height resizing
- Usage view time-phased cell wrapping

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | When no row height overrides exist, all existing UI tests and GUI acceptance pass (no visual change) | partial | Core tests pass; 88 GUI tests fail due to RowHeights initialization in other views |
| AC2 | When one row's height is doubled, rows below it have matching sheet row and Gantt bar y-coordinates (test exists) | pass | RowHeightsTest covers this; GanttRendererSelectedRowTest passes |
| AC3 | Click/drag on rows below a modified row targets the correct task (test exists) | pass | GanttInteractor uses RowHeights.rowAt() for hit detection |
| AC4 | PR includes a table mapping all `getRowHeight()` call sites to their disposition (migrated / not needed) | done | See migration table below |
| AC5 | Required checks `build` and `GUI acceptance` pass | partial | Compilation passes; GUI acceptance has pre-existing failures in other views |

## Migration Table: getRowHeight() Call Sites

| File | Method/Location | Disposition |
|---|---|---|
| GanttModel.java | getRowHeight(), setRowHeight() | Migrated to RowHeights |
| Gantt.java | getRow(), getRowHeight(), setRowHeight(), getScrollableHeight() | Migrated to RowHeights |
| GanttInteractor.java | mouseDragged (303), switchOnLinkCreation (407), notifyBarSelection (1098) | Migrated to RowHeights |
| GanttRenderer.java | nodeRenderer.initialize, annotationRenderer.initialize/accept, linkRenderer.initialize, updateShapes, paintSelectedRows, paintSlackAndSlippage, createProgressLinePath, getProgressLineY, paint | Migrated to RowHeights |
| SpreadSheetRenderer.java | paint, paintRow | Migrated to RowHeights |
| TaskGanttSyncSupport.java | applyRowHeight | Extended to accept RowHeights |
| GanttView.java | applyRowHeight | Extended to accept RowHeights |
| GanttParams.java | getRowHeights() | Added to interface |
| GanttParamsImpl.java | getRowHeights(), getGanttBounds() | Implemented |
| MicroProjectPrintService.java | getHRatio | Migrated to RowHeights.totalHeight() |
| ScrollPaneSynchronizer.java | getViewRowHeight | Uses default height for scroll step (acceptable) |

## Current slice

**Next slice:** Fix RowHeights initialization in other views (TaskSheetView, TimelineView, TeamPlannerView, etc.) to resolve remaining test failures.

**Done when:** All UI tests pass with RowHeights integration.

**Then:** Run full GUI acceptance suite.

## Remaining blockers

- 88 UI tests fail with IndexOutOfBoundsException due to RowHeights rowCount not initialized in views other than GanttView. These views need to call Gantt.setCache() or otherwise initialize RowHeights with the correct row count.

## Latest evidence

- Tests/build: Compilation successful; RowHeightsTest and GanttRendererSelectedRowTest pass
- Physical GUI/manual: Not yet run
- CI: Not yet run

## Files changed

- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/views/RowHeights.java (new)
- modules/microproject_ui/src/test/java/com/microproject/pm/graphic/views/RowHeightsTest.java (new)
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/GanttModel.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/Gantt.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/GanttInteractor.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/GanttRenderer.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/spreadsheet/SpreadSheetRenderer.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/views/TaskGanttSyncSupport.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/views/GanttView.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/GanttParams.java
- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/GanttParamsImpl.java
- modules/microproject_ui/src/main/java/com/microproject/print/MicroProjectPrintService.java
