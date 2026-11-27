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
| F2 | Sorted and filtered view rows are translated to model rows before deciding whether they represent void rows. | pass | `SpreadSheetVoidRowSelectionTest.selectAllUsesVisibleRowsWhenRowsAreSorted` and `selectAllResolvesFilteredModelRowsBeyondTheViewCount`; the latter fails if model-row access is bounded by filtered view count. |
| F3 | Original issue acceptance remains satisfied. | pass / reused | Original PR #1480 evidence plus #1435 GUI candidate `a96db4b0c71c5f1cebc49c1962eb998a29b4b5f0`; its selection result and empty-selection event path remain compatible with the #1481 status-bar fix. |
| F4 | Physical Select All commits multi-character input, preserves the rendered Name and task selection, and leaves the edit on the normal Undo/Redo history. | pass | `TwoDocumentShortcutIsolationGuiAcceptanceTest.physicalCtrlDeleteAndUndoStayWithEachActiveDocument` on the real `DocumentFrame`; physical Ctrl+Z restores the prior empty Name and Ctrl+Y restores the committed text. |
| F5 | Required `build` and `GUI acceptance` checks pass on the PR candidate. | open | PR #1482 latest head after this push; wait for required checks. |
| F6 | A sorted/filtered selected view row resolves to the same domain node in selected-node queries and selection events. | pass | `SpreadSheetVoidRowSelectionTest.selectAllResolvesFilteredModelRowsBeyondTheViewCount` asserts `getSelectedNodes()`, `getSelectedNodesImpl()`, event nodes and currentNode all identify Zulu (view row 0, model row 2). |
| F7 | A rejected editor commit preserves uncommitted input and the existing row selection. | pass | `SpreadSheetVoidRowSelectionTest.selectAllKeepsRejectedCellEditAndExistingSelection` uses an editor whose `stopCellEditing()` rejects; editing, text, and prior selection remain. |

## Current slice

**Next slice:** wait for PR #1482 required checks and review comments on the exact head SHA.
**Done when:** all original criteria and focused follow-up criteria are evidenced and required checks pass.
**Then:** parent agent reviews and integrates PR #1482.

## Remaining blockers

- F5: required checks on the exact candidate SHA.

## Latest evidence

- Candidate code: `1535e89241ec44135605177124290c8517b051e9`, integrated with #1481 master merge `30b41ba73`.
- Tests/build: focused `SpreadSheetVoidRowSelectionTest` → `BUILD SUCCESSFUL`, 6 tests; full `:microproject_ui:test` after selected-node/event and rejection regressions → `BUILD SUCCESSFUL` in 2m; physical journey `TwoDocumentShortcutIsolationGuiAcceptanceTest.physicalCtrlDeleteAndUndoStayWithEachActiveDocument` after these changes → `BUILD SUCCESSFUL` in 23s; inventory script exit 0 on previous candidate.
- Selection regression: filtered Zulu is view row 0/model row 2; `getSelectedNodes()`, `getSelectedNodesImpl()`, event nodes and currentNode all resolve to Zulu. Rejected editor test confirms active editor, uncommitted text, and previous selection survive.
- Physical GUI/manual: after #1481 integration, real `DocumentFrame` Robot typed `Physical selection edit`, pressed `Ctrl+Shift+Space`; editor stopped, model Name and rendered Name matched, selected row still resolved to the same task, physical Ctrl+Z restored the prior name and Ctrl+Y restored the edit. Empty project and 11-task sample were checked on #1435 candidate `a96db4b0c71c5f1cebc49c1962eb998a29b4b5f0`; original PR #1480 also records the sample Undo check.
- CI: pending for commit `1535e89241ec44135605177124290c8517b051e9` on [PR #1482](https://github.com/tetsuji16/microProject/pull/1482).

For reused evidence, the production diff commits the active editor through its existing contract and resolves rows through the model's row accessor after view-to-model conversion. It does not alter the unsorted empty/11-task selection or rendering behavior tested by the original PR. The `getNodeForDisplayRow` documentation now describes model-row access rather than limiting it to renderers; no shared `getNodeAtRow` API change was made. #1481's empty selection notification and task-only status count passed in the merged master UI suite.
