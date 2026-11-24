# Issue #1294 Work Packet: Assignment Information Dialog

## Contract
- PR #1346 / issue #1294; branch `issue/1294-assignment-info-dialog`.
- Open Assignment Information from Task Usage Format and Resource Usage Format for a selected assignment row. Edit units, work contour and plain-text notes; read-only identity/work/date/cost fields remain fixed. OK creates one Undo edit; Cancel is a no-op; read-only projects disable editing; MPO save/reload retains units and notes.
- Out of scope: tracking fields, rich-text notes, double-click and Project Server booking fields.

## Implementation
- Ribbon catalog scopes the command to the two usage Format tabs; stale Task Properties placement removed.
- Dialog uses `AbstractDialog`/`PopupDialogSupport`, localized EN/JA labels and validation.
- Edit service publishes a single undo group after success. Failure restores the model, discards group edits since a checkpoint and preserves earlier history.
- Physical journey enters Task Usage Format, selects an assignment, physically edits units and undoes, then enters Resource Usage Format and cancels.
- The action reports guard rejection reasons and presenter visibility through the existing command outcome API; test seam allows direct Action route verification.

## Evidence and current diagnosis
| Criterion | Evidence | Status |
|---|---|---|
| Assignment edits, MPO, rollback/history, Undo | focused `AssignmentInformationDialogTest` | Pass |
| Ribbon scope/MSP control maps | `RibbonStructureTest`, `MspRibbonOrderDiffTest`, `RibbonCommandCatalogTest` | Pass |
| Direct document Action route | `RibbonButtonBehaviorTest.assignmentInformationActionDispatchesTheResolvedAssignmentToItsPresenter` | Pass; exact assignment target and DISPATCHED result |
| GUI source | `:microproject_ui:compileGuiTestJava -PguiTestSuite=smoke` | Pass |
| Prior physical CI (`4394f9e96`) | Physical button listener fired; UsageDetailView, one selected assignment, resolver `ASSIGNMENT:true`, button enabled, registered Action correct. Full CI stack proves dialog construction failed at `AssignmentInformationDialog.java:97`: `CellConstraints.TOP` was passed as horizontal alignment, causing JGoodies `IllegalArgumentException`. | Root cause fixed in `4fdfc120b` by swapping horizontal/vertical alignment arguments. |
| Failed-edit Undo/history | `AssignmentInformationDialogTest.headlessEditServiceRestoresAllFieldsWhenAFieldMutationFails` asserts model rollback, retained `canUndo`, and the existing prior edit remains the top/only Undo entry. | Pass locally on candidate changes. |
| Modeless visibility reporting | Dialog is constructed nonmodal, so `WindowOpened` can be delivered after `doModal()` returns. Presenter now reports `dialog.isShowing() || opened.get()`. | Included in `4fdfc120b`; physical acceptance pending. |
| Physical OK follow-through | First fixed-candidate run opened the dialog and accepted units input, then the GUI fixture passed an absent `named(..., "ok")` component (AbstractDialog buttons have no component name) to Robot, causing test-side NPE. The fixture now resolves the localized visible OK button and clicks it with Robot. | Test fixture correction compiles locally; rerun pending. |
| Local Robot attempt | External foreground window blocked before input; no app closed. | Environment blocked |
| Required exact-head Build/GUI checks | Latest diagnostic candidate | Pending after push |

## Remaining work
- Push the GUI fixture correction, then inspect exact-head Build and Windows GUI acceptance results; confirm the physical usage-format route opens, edits, undoes, and cancels as specified.
- Do not close issue until physical route and required checks pass.
