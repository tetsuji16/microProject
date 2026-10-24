# Issue #899 — Yes/No 列を MSP と同じ「はい／いいえ」の一覧選択にし、タスク モード列を追加する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/899
Contract frozen at: 2026-10-24, issue body as filed (user chose "all Yes/No columns")
Baseline HEAD: `07becd0e3` (#894 branch, PR #898)
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Spreadsheet Yes/No cells show text (Yes/No, ja はい/いいえ) and are edited by
choosing from a list. `Field.manuallyScheduled` is the MSP Task Mode column:
an icon plus Manually Scheduled / Auto Scheduled. Stored values stay Boolean.

Out of scope: Task Information dialog layout (it keeps its check box, labelled
手動スケジュール); placeholder task mode (no placeholder state in the model).
Blockers: none.

Document-derived compatibility decision (not a measured MSP run):
- Microsoft Support "Task Mode (task field)" (ja): field ［タスク モード］, values
  手動スケジュール／自動スケジュール, data type はい/いいえ, 「タスク モードを変更する
  には、フィールドをクリックします。」
- Microsoft Support "Flag1-20 fields": the user selects Yes or No in the cell;
  the default is No.
- Narrowest Office-consistent click rule: the first click selects the cell (the
  existing selection contract); a click on the already-active cell, F2, Alt+Down
  or typing opens the list. Double-click still opens Task Information.

## Design

- Core `BooleanText`: one label/choices/parse rule. `Field.toText` shows labels
  for Boolean values; `Field.preprocessText` parses labels, the localized Yes/No
  and true/false, yes/no, y/n, 1/0. A blank cell means No, and other text raises
  the invalid-choice error.
- Strings: `Field.manuallyScheduled` → Task Mode / タスク モード, new
  `TaskMode.manual|auto`, ja `Text.No` fixed from 番号 to いいえ.
- UI: one Yes/No renderer and list editor, installed for every Boolean column.
  The list commits the label through the existing `setValueAt` / Undo path.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Yes/No text instead of check boxes; empty rows blank | pass (unit, GUI pending CI) | `SpreadsheetGridBorderTest.yesNoRenderer*` |
| A2 | Physical click / F2 / Alt+Down / typing open one list; one Undo/Redo step | pending CI | `physicalTaskModeColumnShowsTextAndIsChosenFromAList` (passed locally before the width fix) |
| A3 | Task Mode column: header, icon + mode text, two choices | core pass | `BooleanTextTest` |
| A4 | Stored Boolean unchanged; MPO/MPP/MPX/XML/XLSX no regression | pass | `gradlew build` (all module tests) |
| A5 | Text input accepts labels and true/false; others rejected | core pass | `BooleanTextTest` |
| A6 | ja/en, UI scale 1.0/1.5 physical GUI | pending CI | targeted gui-acceptance dispatches |

## Current slice

**Next slice:** targeted CI GUI runs (ja 1.0, ja 1.5, en; U-26 ja) on the PR head.
**Done when:** all pass with 0 skipped.
**Then:** integration (PR) and close.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `gradlew build` passed. The legacy report baseline records the Yes/No text delta (Task Information Field.manuallyScheduled: frozen true/false → MSP text). The Task Mode default width is 120 (spreadsheet minimum 110) so the text does not wrap.
- Physical GUI/manual: the subagent's run before the width/popup fix passed 21/21 in ja 1.0, ja 1.5 and en, and U-26 2/2. The local rerun after the fix was blocked by GUI_ENVIRONMENT_CONTENDED (the Claude desktop window overlapped the test window); this is not a product failure.
- Known gap: the physical Escape key is not covered in the grid fixture (its DocumentFrame does not own the test sheet); cancel is tested through `cancelCellEditing`, the call the Escape handler makes.
- CI: pending
