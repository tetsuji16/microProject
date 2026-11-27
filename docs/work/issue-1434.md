# Issue #1434 — Select All editor and view-row follow-up

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1434 (closed by the original implementation; this is a follow-up reference)
Contract frozen at: 2026-11-28, original issue body and merged PR #1480
Baseline HEAD: `c4dad23e1c90cbc5c657032c151ff61f90d92a6f`
Baseline working tree: clean

## Goal and scope

Make `SpreadSheet.selectAll()` commit a valid active cell edit and classify rows in the visible view coordinate system.

Out of scope: status-bar counts (#1435), changes to the shared `getNodeAtRow` API, editor startup or value conversion.
Blockers: required PR checks on the follow-up PR.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| F1 | Active Name-cell text is committed when Select All is invoked; rejected commits leave the active edit intact. | pass | `SpreadSheetVoidRowSelectionTest.selectAllCommitsTheActiveCellEdit` (old implementation left `Before`; fixed implementation commits `After`). |
| F2 | Sorted view rows are translated to model rows before deciding whether they represent void rows. | pass | `SpreadSheetVoidRowSelectionTest.selectAllUsesVisibleRowsWhenRowsAreSorted`; old code selected `[0, 1]` instead of expected `[50, 51]`. |
| F3 | Original issue acceptance remains satisfied. | pass / reused | Original PR #1480 evidence plus current candidate `a96db4b0c71c5f1cebc49c1962eb998a29b4b5f0`; scoped diff changes editor finalization and sorted/filtered coordinates only. |
| F4 | Physical Select All commits multi-character input, preserves the rendered Name and task selection, and leaves the edit on the normal Undo/Redo history. | pass | `TwoDocumentShortcutIsolationGuiAcceptanceTest.physicalCtrlDeleteAndUndoStayWithEachActiveDocument` on the real `DocumentFrame`; physical Ctrl+Z restores the prior empty Name and Ctrl+Y restores the committed text. |
| F5 | Required `build` and `GUI acceptance` checks pass on the PR candidate. | open | PR #1482 head `eafeeded2`; run on the exact candidate SHA. |

## Current slice

**Next slice:** wait for PR #1482 required checks and review comments on the exact head SHA.
**Done when:** all original criteria and focused follow-up criteria are evidenced and required checks pass.
**Then:** parent agent reviews and integrates PR #1482.

## Remaining blockers

- F5: required checks on the exact candidate SHA.

## Latest evidence

- Candidate: `eafeeded2` (PR #1482 head; source plus follow-up Robot test and current issue packet).
- Tests/build: `:microproject_ui:test --tests com.microproject.ui.frame.SpreadSheetVoidRowSelectionTest --console=plain` → `BUILD SUCCESSFUL`; `RibbonButtonBehaviorTest` → `BUILD SUCCESSFUL`; full `:microproject_ui:test` → `BUILD SUCCESSFUL` in 2m9s.
- Physical GUI/manual: real `DocumentFrame`, Robot typed `Physical selection edit`, pressed `Ctrl+Shift+Space`; editor stopped, model Name and rendered Name matched, selected row still resolved to the same task, physical Ctrl+Z restored the prior name and Ctrl+Y restored the edit. Empty project and 11-task sample were checked on candidate `a96db4b0c71c5f1cebc49c1962eb998a29b4b5f0` by the #1435 worker; original PR #1480 also records the sample Undo check.
- CI: pending on [PR #1482](https://github.com/tetsuji16/microProject/pull/1482), head `eafeeded2`.

For reused evidence, the production diff is limited to active-editor commit and conversion from view row to model row in `selectAll`; it does not alter the unsorted empty/11-task rendering or undo behavior tested by the original PR.
