# Issue #1415 — Gantt drawings: data model and MPO persistence

**State:** Implementation complete, local verification green, CI pending
**Contract frozen at:** 2026-11-26
**Baseline HEAD:** `880b61642` (origin/master)
**Branch:** `issue-1415-gantt-drawings`

## Acceptance criteria (from issue)

| ID | Required result/evidence | Status | Evidence |
|---|---|---|---|
| AC1 | Add, move, format and delete are one Undo each and Undo/Redo restore the drawing (test) | done | `GanttDrawingsTest.everyDrawingOperationIsOneUndoStep` |
| AC2 | An MPO round trip keeps kind, anchor, offsets, colors and text (test) | done | `MpoFileImporterTest.ganttDrawingsSurviveMpoSaveAndReload` |
| AC3 | An MPO without the entry loads zero drawings without an exception (test) | done | `MpoFileImporterTest.mpoWithoutGanttDrawingsRestoresNoDrawings` |
| AC4 | Deleting a task removes its drawings and one Undo restores both (test) | done | `GanttDrawingsTest.deletingATaskRemovesItsDrawingsAndOneUndoRestoresBoth` |
| AC5 | `:microproject_core:test` and `:microproject_exchange:test` pass | done | see verification log |
| AC6 | `git diff --check` is empty | done | exit 0 |
| AC7 | PR `build` and `GUI acceptance` pass | pending | CI on the pushed head |

## Implementation

- `modules/microproject_core/src/main/java/com/microproject/model/gantt/GanttDrawing.java`
  - value type (kind, anchor task uid, anchor side, offsets, size, ARGB line/fill color, text); no drawing code
- `.../model/gantt/GanttDrawings.java`
  - ordered list in `Project` transient document state (like `PertManualPositions`), monotonic ids
  - `add`, `move`, `format`, `remove`, `removeForTask` each post one `GanttDrawingEdit`;
    `setDrawings` is the silent loader path and `removeForTaskSilently` serves replayed removals
- `.../undo/GanttDrawingEdit.java`
  - one before/after list snapshot per operation, so one Undo/Redo step
- `.../model/task/Project.java`
  - `getGanttDrawings()` / `findGanttDrawings()`; `remove(...)` also removes the drawings anchored to a
    deleted task inside the same undo update, so one Undo restores the task and its drawings
- `modules/microproject_exchange/src/main/java/com/microproject/exchange/MpoSidecarJson.java`
  - `ganttDrawingsJson` / `restoreGanttDrawings` for `microproject/gantt-drawings.json`, tasks by serialized uid
- `MpoFileImporter.java`
  - new `GANTT_DRAWINGS_ENTRY`, written next to the PERT entry, restored in `restoreTaskExtensions`,
    and excluded from the retained-extension copy

## Scope kept

POD bytes are unchanged (drawings are transient state). MSPDI/MPP do not carry drawings. Date anchors,
polygons, patterns and z-order commands stay out (child B / later issues).

## Verification commands run

```text
.\gradlew.bat :microproject_core:test --tests "com.microproject.model.gantt.*" --console=plain
.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.ganttDrawingsSurviveMpoSaveAndReload" --tests "com.microproject.exchange.MpoFileImporterTest.mpoWithoutGanttDrawingsRestoresNoDrawings" --console=plain
.\gradlew.bat :microproject_core:test --console=plain
.\gradlew.bat :microproject_exchange:test --console=plain
git diff --check
```

## Next step

Commit, push, open the PR with every completion criterion, wait for `build` and `GUI acceptance`, then merge.
