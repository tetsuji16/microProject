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
| Failed-edit Undo/history | `AssignmentInformationDialogTest.headlessEditServiceRestoresAllFieldsWhenAFieldMutationFails` asserts model rollback, retained `canUndo`, and the existing prior edit remains the top/only Undo entry. | Pass locally. |
| Task/Resource Usage physical route (JA) | Exact head `c856cef48`; focused Robot run [45440937818](https://github.com/tetsuji16/microProject/actions/runs/45440937818) passed. Physically opens, edits, confirms with OK, verifies model and Undo, then opens Resource Usage and cancels with Escape. PR smoke [45440878099](https://github.com/tetsuji16/microProject/actions/runs/45440878099) also passed. | Pass on prior head; rerun required after #1351 integration and GUI layout assertions. |
| English dialog presentation | Focused workflow [45443300889](https://github.com/tetsuji16/microProject/actions/runs/45443300889) dispatched on `1b3fdaa6a` with `gui_test_locale=en`; it predates the new title and text-height assertions. | In progress; rerun on assertion head required. |
| GUI display invariants | Added locale title assertion and `DialogLayoutAssertions.assertTextControlsAtPreferredHeight` for both usage dialogs. | Compiles locally; physical run pending. |
| Exact-head cross-platform Build / GUI | #1351 master merge integrated as `1b3fdaa6a`; GUI [45442811106](https://github.com/tetsuji16/microProject/actions/runs/45442811106) and Build [45442810986](https://github.com/tetsuji16/microProject/actions/runs/45442810986) started on that head. Prior-head GUI smoke and focused physical run passed; prior Build had Ubuntu/Windows pass before macOS was cancelled when master was integrated. | New verification pending after the bilingual assertions are committed. |

## Remaining work
- Commit and push the locale/title/text-height GUI assertions, then run exact-head physical journeys in Japanese and English plus Build and GUI acceptance after the #1351 master merge.
- Do not close issue until physical route and required checks pass.
