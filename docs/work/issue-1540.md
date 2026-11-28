# Issue #1540 — AutoRecovery後のF2編集が古いSpreadsheetの選択通知で失敗する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1540
Contract frozen at: 2026-11-28, issue body updated to include TEST_PLAN regression row
Baseline HEAD: `d992c1801ebae4dbae59e26d465fcbe3be9fcdff`
Baseline working tree: clean isolated worktree; primary workspace contains unrelated user artifacts and remains untouched

## Goal and scope

When a document's Spreadsheet is cleaned up, release it from the shared editor-table tracker so editing a different/recovered document cannot clear selection on the closed table and invoke its disposed frame listener.

Out of scope: changing generic GraphicManager selection semantics, recovery concurrency/snapshot format, or project file formats. Issue #531 is a separate AutoRecovery concurrency defect.
Blockers: none known; related open PRs have no failed required checks or actionable review comments.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | cleaned-up JTable selection does not change when another table begins editing | pass | focused test failed before implementation at test line 84, then passed on `caa6b0f5` plus lifecycle fix |
| A2 | only the identity-matching currently tracked JTable is released during cleanup | pass | `cleaningTrackedTableReleasesItBeforeAnotherTableBeginsEditing` asserts preserved closed-table selection and recovered table remains tracked |
| A3 | AutoRecovery physical F2 editing, Undo/Redo, save, MPO reload pass | pass | `AutoRecoveryGuiAcceptanceTest.recoversSnapshotAndSavesTheRecoveredDocument`, 1 test passed on current implementation |
| A4 | UI unit tests, source inventory, and diff check pass | pass | `:microproject_ui:test`: 2,001 tests, 0 failures/errors, 4 skipped; inventory exit 0; diff check clean |
| A5 | TEST_PLAN records the editor tracker cleanup lifecycle regression | pass | existing Spreadsheet cleanup row now names `SpreadSheetEditorTableTrackingTest` and its invariant |
| A6 | required PR build and GUI acceptance pass | pass | PR #1541 at head `1f61d908a`: [Build matrix](https://github.com/tetsuji16/microProject/actions/runs/46107347989), [GUI acceptance](https://github.com/tetsuji16/microProject/actions/runs/46107346826), [checklist](https://github.com/tetsuji16/microProject/actions/runs/46107346853) passed; final packet-only head checks are tracked on the [PR checks page](https://github.com/tetsuji16/microProject/pull/1541/checks). |

## Current slice

**Next slice:** verify exact-head checks after this packet update, update the PR evidence, and resolve any actionable review comments.
**Done when:** the final PR head has passing required checks and no unresolved actionable review.
**Then:** integrate the fix and resume release stabilization.

## Remaining blockers

- Exact-head Build and GUI acceptance must be confirmed after the packet-only update.

## Latest evidence

- Tests/build: pre-fix `:microproject_ui:test --tests "com.microproject.ui.view.spreadsheet.editor.SpreadSheetEditorTableTrackingTest"` failed in `cleaningTrackedTableReleasesItBeforeAnotherTableBeginsEditing` at line 84; after fix on `b8a40b60c`, focused class 4/0 and full UI suite 2,001/0 failures/errors/4 skipped.
- Physical GUI/manual: `:microproject_ui:guiTest --tests "com.microproject.ui.frame.AutoRecoveryGuiAcceptanceTest.recoversSnapshotAndSavesTheRecoveredDocument" --max-workers=1 --console=plain --no-daemon` passed 1/0 on `b8a40b60c`; includes recovered-project F2, Undo/Redo, save, and MPO reload.
- CI: no PR yet; exact-head PR checks will be required.
