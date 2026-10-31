# Issue #979 - Spreadsheet name-cell editor: single previous-table record, shared render setup

State: IN PROGRESS (Robot/U-26 evidence pending CI)
Issue: https://github.com/tetsuji16/microProject/issues/979
Contract frozen at: issue text as delegated 2026-10-31
Baseline HEAD: `25c41fd32`
Baseline working tree: clean worktree `microProject-w979` (branch claude/issue-979)

## Goal and scope

Remove the duplicated `lastTable` in `SpreadSheetNameCellEditor`, share the
name-cell content setup between renderer and editor, and share the typed-character
predicate used by `MicroProjectDateField` and `ChangeAwareTextField`.

Out of scope: the offline `NameCellComponent.getComponent(value,node,params)` path
(different data source), other editors, MSP parity claims.
Blockers: none

## Design decisions

- One previous-table record lives in `SpreadSheetCellEditorAdapter`
  (`trackEditingTable`, `lastTable()`), held as a `WeakReference` so a closed
  document's table is no longer retained (the old static strong field retained it).
- `NameCellComponent.setupNode(...)` is the single text/level/icon/font/TaskFontStyle
  setup used by `getComponent(...)` and the name editor.
- Editor now applies `TaskFontStyle`: product consistency choice (edit text matches
  the rendered task text style). No Microsoft source cited; not a document-derived
  decision.
- `ImeTextInputSupport.isPlainTypedCharacter(KeyEvent, JTextComponent)` is the shared
  predicate; `ChangeAwareTextField` keeps its extra "text unchanged" condition.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | exactly one `lastTable`; render setup shared | pass | commit on claude/issue-979 |
| A2 | U-26 satisfied (multi-char physical route, attachment burst, IME reconversion, domain+rendered values, save/reload) | pending | existing `robotTypesDurationAndPercentAsOneInputTransaction` and `robotFirstKeyEditsStableTask...` must pass in CI |
| A3 | physical Robot: moving to another table stops the edit | pending | `U26SpreadsheetInputTransactionGuiAcceptanceTest.robotMovingToAnotherTableStopsTheNameCellEdit` in CI |

## Current slice

**Next slice:** run the U-26 GUI class in CI on the candidate SHA.
**Done when:** both existing U-26 journeys and the new journey pass.
**Then:** integrate and close.

## Remaining blockers

- A2, A3 Robot evidence (not run locally; desktop is shared).

## Latest evidence

- Tests: `:microproject_ui:test --tests *SpreadSheetEditorTableTrackingTest --tests *EditorSelectionBehaviorTest` pass (3 + 8);
  full `:microproject_ui:test` BUILD SUCCESSFUL. Regression: `SpreadSheetEditorTableTrackingTest`
  (name->other and other->name table switch; font parity with renderer).
- Compile: `:microproject_ui:compileGuiTestJava` BUILD SUCCESSFUL.
- Physical GUI/manual: pending (CI).
- CI: pending.

## CI evidence

Robot `U26SpreadsheetInputTransactionGuiAcceptanceTest` passed, all 3 cases including the table move, in run 41167312172 (ja 100%, SHA 6015cd0b9). A double-click on a task row opens Task Information, so the journey starts edits from a typed key.
