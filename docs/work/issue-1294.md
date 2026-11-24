# Issue #1294 Work Packet: Assignment Information Dialog

## Contract
- PR #1346 / issue #1294, branch `issue/1294-assignment-info-dialog`.
- Open Assignment Information from the Task Usage and Resource Usage Format tabs for a selected assignment row. Edit units, work contour and plain-text notes; task/resource, work, dates and cost are read-only. OK is one undoable edit; Cancel is a no-op; read-only projects disable edits; MPO save/reload retains units and notes.
- Out of scope: tracking fields, rich-text notes, double-click route and Project Server booking fields.

## Implementation
- Catalog and enable command only on the two usage Format tabs for an editable selected assignment.
- Dialog uses `AbstractDialog`/`PopupDialogSupport`; labels and validation are localized EN/JA.
- Application service posts a single edit group only after success. On failure it restores the model and discards edits since the undo checkpoint, preserving prior history.
- Headless tests cover edit/undo, cancel, read-only, failure rollback/history preservation, MPO round-trip and ribbon enablement.
- GUI journey now uses Task Usage Format, physical keyboard editing + Undo, then Resource Usage Format assignment selection + command + Escape cancel.

## Evidence on local candidate before latest master integration
| Criterion | Evidence | Status |
|---|---|---|
| Service mutation, rollback and Undo history | `:microproject_ui:test --tests 'com.microproject.ui.dialog.AssignmentInformationDialogTest'` | Pass |
| All UI unit tests | `:microproject_ui:test` | Pass |
| GUI journey source compile and journey contract | `:microproject_ui:compileGuiTestJava -PguiTestSuite=smoke` | Pass |
| GUI physical route | `:microproject_ui:guiTest --tests 'com.microproject.ui.dialog.AssignmentInformationGuiAcceptanceTest'` | Blocked before first Robot input: foreground Claude PDF window overlapped test window (`GUI_ENVIRONMENT_CONTENDED`); no app was closed. |
| Required build and GUI acceptance | Latest prior candidate `a09d89c42` | Build and GUI acceptance passed, but predates the corrected journey and undo fix. Re-run after push. |

## Remaining work
- Latest `origin/master` includes #1354 (`21b1f5383`). Integrate it, rerun checks, push branch, and inspect build/GUI acceptance on exact head.
- Do not close issue until exact-head required checks and physical route evidence pass.
