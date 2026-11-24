# Issue #1294 Work Packet: Assignment Information Dialog

## Contract and scope
- Issue #1294; PR #1346; branch `issue/1294-assignment-info-dialog`; base `master`.
- Provide Assignment Information from Task Usage and Resource Usage Format tabs for a selected assignment row. Edit units, work contour, and plain-text notes; preserve read-only project behavior; OK is one undoable edit, Cancel is a no-op, and MPO save/reload retains units and notes.
- Out of scope: tracking fields, rich-text notes, double-click route, Project Server booking fields.

## Implementation
- Command is cataloged only for Task Usage and Resource Usage Format tabs and enabled for a selected editable assignment in UsageDetailView.
- The dialog uses `AbstractDialog`/`PopupDialogSupport`, plain text notes, localized EN/JA validation and contour labels, and avoids rewriting unchanged unit values.
- Application-layer edit service applies only changed fields as one transaction and rolls back on failure.
- Unit tests cover edit/undo, cancel, read-only, failure rollback, MPO round-trip and ribbon enablement. Physical Robot test exercises the Task Usage ribbon route, edits, undo and cancel.

## Acceptance evidence (tested against integrated local candidate)
| Criterion | Evidence | Status |
|---|---|---|
| Format tabs, selection enablement, read-only disabled | `RibbonButtonBehaviorTest.assignmentInformationIsEnabledOnlyForAnEditableAssignmentInUsageViews` | Pass |
| Edit, undo, cancel, validation rollback | `:microproject_ui:test --tests '*AssignmentInformation*'` | Pass (prior run; same production/test sources) |
| MPO units/notes persistence | `AssignmentInformationEditServiceTest` within focused test run | Pass |
| Localization and ribbon structure | focused `RibbonUiAuditTest`, `RibbonCommandCatalogTest`, `RibbonStructureTest`, `MspRibbonOrderDiffTest`; source inventory | Pass (prior run; same relevant sources/docs) |
| UI compilation | `:microproject_ui:compileJava :microproject_ui:compileTestJava :microproject_ui:compileGuiTestJava` | Pass on current integrated candidate |
| Full UI unit suite | `:microproject_ui:test` | Pass on current integrated candidate |
| Physical GUI route | `:microproject_ui:guiTest --tests 'com.microproject.ui.dialog.AssignmentInformationGuiAcceptanceTest'` | Blocked locally: external application window overlapped test window; environment contention artifact recorded by GUI harness. Await Windows GUI acceptance CI. |
| Required PR checks | `build`, `GUI acceptance` | Pending exact pushed head |

## Current slice and blockers
- Integrated latest `origin/master` including #1349. Build and GUI acceptance CI for the exact pushed SHA are the remaining gates.
- Do not close issue #1294 until required checks succeed and physical route evidence is available.
