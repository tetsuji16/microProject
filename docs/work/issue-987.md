# Issue #987 — Availability and cost-rate row removal does not mark the project unsaved

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/987
Contract frozen at: 2026-10-31, issue body at intake
Baseline HEAD: `47a8239fe`
Baseline working tree: clean worktree `claude/mpo-pod-io`

## Goal and scope

Row delete, row cut, and Undo/Redo in the Resource Information tables leave `project.needsSaving()` true, so closing cannot silently lose the edit.

Out of scope: adding an undo stack to these tables.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Delete and cut mark the project, for both tables | pass | `ResourceTableEditDirtyTest`; 4 route cases red with the listener disabled |
| A2 | Undo/Redo leaves the project marked | pass | `projectUndoAndRedoKeepRowRemovalUnsaved`; see the decision below |
| A3 | Opening or refilling a table is not an edit | pass | `refillingSheetForAnotherResourceDoesNotMarkProject` |
| A4 | No serialized field change | pass | Only `ResourceInformationDialog.trackTableEdits` changed |
| A5 | One Robot journey for the real route | open | `ResourceAvailabilityRowDeleteGuiAcceptanceTest`, compiled; targeted Windows run pending |

## Decision

Table node models have no undo controller (`NodeModelFactory.createNodeModelFromCollection` sets none; the `initUndo` calls in the tables are commented out), so the only Undo/Redo a user can reach is the project's, and it never changes these rows. The contract tested is therefore that project Undo/Redo leaves the rows alone and keeps the removal unsaved.

## Next slice

Run the targeted GUI workflow on the PR branch, then merge after CI is green for the exact SHA.
