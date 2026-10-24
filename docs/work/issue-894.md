# Issue #894 — タスクのない行にチェックボックスを描画しない

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/894
Contract frozen at: 2026-10-24, issue body as filed 2026-10-23
Baseline HEAD: `9b7f2be8d` (origin/master)
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Rows without a task draw no value in any column, boolean (check box)
columns included.

Out of scope: column header wrapping (#895), other empty-row styling.
Blockers: none.

Cause: `SpreadSheetUtils.getValueAt` returns `null` for void rows, and the
shared `OfflineCapableBooleanRenderer` drew `null` as an unchecked box. It now
draws a transparent icon of the check box's size for `null`, so the row height
and the cell background, grid and selection borders are unchanged. Real task
values are always `Boolean`, so they still draw the box.

Document-derived compatibility decision: Task Mode is a per-task field
(Microsoft Support, "Task Mode (task field)"); a row with no task has no value,
so Office-style tables show it blank. Not a measured MSP run.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Rows without a task draw no value, check boxes included | pass (unit) | `SpreadsheetGridBorderTest.booleanRendererPaintsNoCheckBoxForAnEmptyRow` (fails without the fix) |
| A2 | Clicking/typing in an empty row to create a task works as before | pass (GUI) | renderer-only change; Robot click on a void row: no exception, no editor, no task (same as before) |
| A3 | Task-row check box display, toggle, Undo, save/reload unchanged | pass (GUI) | `physicalManualSchedulingCheckBoxIsAbsentOnEmptyRowsAndStillTogglesTaskRows`: Space toggle, Ctrl+Z/Ctrl+Y, MPO reload by UID |
| A4 | ja physical GUI images at UI scale 1.0 and 1.5 | pass (GUI) | screen pixels of empty vs task cells; `task-table-empty-row-boolean.png` at 1.0 and 1.5 |

## Current slice

**Next slice:** integration (PR) and close.
**Done when:** CI is green on the PR head.
**Then:** close #894.

## Remaining blockers

- none

## Latest evidence

- Tests/build: renderer unit tests pass; the new test fails on `9b7f2be8d` without the fix.
- Physical GUI/manual: `guiTest --tests *TaskTableGanttGridGuiAcceptanceTest` ja, 21 pass at UI scale 1.0 and at 1.5, 0 skipped.
- Observed, out of scope: a single mouse click on a task row's check box only selects the row; Space starts the editor and a second Space toggles. Not changed here.
- CI: pending
