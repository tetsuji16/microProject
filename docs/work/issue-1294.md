# Issue #1294 Work Packet: Assignment Information Dialog

## Contract
- PR #1346 / issue #1294; branch `issue/1294-assignment-info-dialog`.
- Assignment Information opens from Task Usage Format and Resource Usage Format for a selected assignment. Edit units, work contour and plain-text notes; read-only identity/work/date/cost fields remain fixed. OK creates one Undo edit; Cancel is a no-op; read-only projects disable editing; MPO save/reload retains units and notes.
- Out of scope: tracking fields, rich-text notes, double-click and Project Server booking fields.

## Implementation
- Ribbon catalog primary scope is Format and command is permitted only on the two usage Format tabs; removed the stale Task Properties placement and updated ribbon/Microsoft control maps.
- Dialog uses `AbstractDialog`/`PopupDialogSupport`, localized EN/JA labels and validation.
- Edit service applies a single undo group; failed application restores model state, discards edits from its checkpoint and retains prior history.
- Headless tests cover changed fields, Undo, Cancel, read-only, failure rollback/history preservation, MPO persistence and ribbon enablement.
- `@GuiJourney(RIBBON_CLICK)` physically exercises Task Usage Format edit/Undo and Resource Usage Format select/cancel. The failure path captures the screen and resolver/action state before cleanup.

## Evidence
| Criterion | Evidence | Status |
|---|---|---|
| Failure rollback/history; MPO and dialog semantics | `:microproject_ui:test --tests 'com.microproject.ui.dialog.AssignmentInformationDialogTest'` | Pass |
| Ribbon ownership and MSP mapping | focused `RibbonStructureTest`, `MspRibbonOrderDiffTest`, `RibbonCommandCatalogTest` | Pass after latest placement correction |
| GUI source | `:microproject_ui:compileGuiTestJava` | Pass |
| GUI route on CI SHA `46485e17a` | Smoke ran; Task Usage Format button enabled/showing and clicked, but no dialog appeared. Cleanup occurred before existing automatic screenshot, so artifact showed only desktop. No foreground contention evidence. | Fail; candidate diagnostic recapture added |
| Local physical route | `:microproject_ui:guiTest --tests 'com.microproject.ui.dialog.AssignmentInformationGuiAcceptanceTest'` | Blocked by external foreground window before Robot input; no app closed |
| Full UI tests and exact-head required checks | Prior candidate passed full UI tests; current corrective diff needs fresh CI | Pending after push |

## Remaining work
- Verify click dispatch and resolver state with new pre-click assertions/cleanup-time diagnostics; run exact-head Build and GUI acceptance.
- Do not close issue until physical route and required checks pass.
