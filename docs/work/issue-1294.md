# Issue #1294 Work Packet: Assignment Information Dialog

## Contract
- PR #1346 / issue #1294, branch `issue/1294-assignment-info-dialog`.
- Open Assignment Information from the Task Usage and Resource Usage Format tabs for a selected assignment row. Edit units, work contour and plain-text notes; task/resource, work, dates and cost are read-only. OK is one undoable edit; Cancel is a no-op; read-only projects disable edits; MPO save/reload retains units and notes.
- Out of scope: tracking fields, rich-text notes, double-click route and Project Server booking fields.

## Implementation
- Catalog/enable only on the two usage Format tabs for an editable selected assignment.
- Dialog uses `AbstractDialog`/`PopupDialogSupport`; labels and validation are localized EN/JA.
- Application edit service publishes one undo group after success. On failure it restores the model, discards edits since the undo checkpoint and preserves prior history.
- Headless tests cover edit/undo, cancel, read-only, failure rollback/history preservation, MPO round-trip and ribbon enablement.
- `@GuiJourney(RIBBON_CLICK)` physically exercises Task Usage Format, units edit and Undo, then Resource Usage Format, selected assignment, command and Escape cancellation.

## Evidence on latest integrated local candidate
| Criterion | Evidence | Status |
|---|---|---|
| Failure rollback/history | `:microproject_ui:test --tests 'com.microproject.ui.dialog.AssignmentInformationDialogTest'` | Pass |
| Full UI unit suite | `:microproject_ui:test` | Pass |
| Main/test/GUI journey compile | `:microproject_ui:compileJava :microproject_ui:compileTestJava :microproject_ui:compileGuiTestJava -PguiTestSuite=smoke` | Pass |
| Physical GUI route | `:microproject_ui:guiTest --tests 'com.microproject.ui.dialog.AssignmentInformationGuiAcceptanceTest'` | Blocked before first Robot input: foreground Claude PDF window overlapped the test window (`GUI_ENVIRONMENT_CONTENDED`); no app was closed. |
| Required checks | Exact pushed candidate | Pending after final push |

## Remaining work
- Push after latest origin/master #1354 integration; inspect exact-head Build and GUI acceptance checks.
- Do not close issue until required checks pass and physical route evidence is available.
