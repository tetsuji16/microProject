# Issue #997 — Resource table first row and the other tables' row menus

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/997
Contract frozen: issue body at intake
Baseline HEAD: `255a82274`

## Goal and scope

The Resource Information dialog's Resource Availability and Cost Rate tables
refuse to remove their first row, and the earlier `SpreadSheet.hasRowPopup` fix
exposed the row popup of every other non-task table. Both routes must behave as
Microsoft Project behaves, must leave rows, backing data, the clipboard and the
saved state consistent, and every command a popup offers must act on the clicked
row.

Out of scope: adding an undo stack to the interval tables; changing the MPO/POD
wire formats; Project Server/PWA behaviour.
Blockers: none.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | First availability row: Delete and Cut are refused with the existing message, row and data stay aligned, the project is not marked unsaved | Pass | `ResourceAvailabilityRowDeleteGuiAcceptanceTest`, `ResourceTableEditDirtyTest.firstAvailabilityRowCannotBeDeletedOrCut` |
| A2 | Same contract for the first cost rate row | Pass | `ResourceCostRateRowDeleteGuiAcceptanceTest` (PR #1140), `ResourceTableEditDirtyTest.firstCostRateRowCannotBeDeletedOrCut` |
| A3 | A refused cut leaves the clipboard unchanged | Pass | PR #1098; both Robot journeys compare a clipboard marker before and after the refusal |
| A4 | The decision is recorded as a document-derived compatibility decision | Pass | "Decision" below |
| A5 | The resource sheet popup offers its own commands once, no task-only command, and Delete removes the row | Pass | `NonTaskRowPopupCommandsTest.resourceSheetPopupOffersItsOwnCommandsOnce`, `resourceSheetPopupDeleteRemovesTheSelectedResource` |
| A6 | The Task Information dependency table offers Delete and it removes the link | Pass | `NonTaskRowPopupCommandsTest.dependencyTablePopupDeleteRemovesThePredecessor` |
| A7 | Delete and Ctrl+X reach a table shown in a dialog, as they do in Microsoft Project, and refuse the first row exactly like the popup | Pass | `ResourceIntervalTableKeyboardGuiAcceptanceTest` (physical Robot, both tables), `DialogTableKeyboardRouteTest` |
| A8 | The project table, the Resource Usage assignment table and the timesheet table offer the commands their configuration declares, with no task-only command and no duplicate | Pass | `NonTaskRowPopupCommandsTest.projectTablePopupOffersItsCopyCommandOnly`, `resourceAssignmentTablePopupOffersItsOwnCommandsOnce`, `timesheetTableHasNoRowPopup` |
| A9 | The Resource Sheet row popup opens and deletes the clicked resource through the physical right-click | Pass | `ResourceSheetRowPopupGuiAcceptanceTest` (physical Robot) |
| A10 | The assignment entry table in Assign Resources and Replace keeps the Copy command its configuration declares, Ctrl+C copies the selected resource row, and the timesheet table keeps no row command | Pass | `AssignmentDialogTableRouteTest`, `AssignmentDialogGuiAcceptanceTest.robotAssignRouteRejectsLockedTaskThenAssignsAndUndoes` (physical Robot Ctrl+C) |

## Decision

Document-derived compatibility decision (not a measured MSP run).

Microsoft documents both interval tables as effective-dated rows that start from
the resource's current entry and gain further rows only when the rate or the
availability changes:

- Resource availability: https://support.microsoft.com/en-us/project/change-resource-availability-without-using-a-calendar
- Resource cost rates: https://support.microsoft.com/en-US/project/enter-costs-for-resources

The first row therefore carries the entry that applies before the first change.
With it removed there is no entry for the period before the earliest remaining
date, so the table and the availability lookup that reads it would disagree.
Microsoft documents adding rows and changing their dates, not removing the
initial row.

Microsoft edits the tables it shows in dialogs with the keyboard, so the dialog
window - not only the document window - owns Delete, Ctrl+X, Ctrl+C and Ctrl+V
for its own table (`DialogSpreadSheetShortcuts`, installed by
`InformationDialog` and by the shared `DialogEntrySpreadSheet` of the assignment
dialogs). Each key resolves to the same table action the row popup uses; the
document keeps its single root-pane registration, so no key fires twice.

Rules this fork applies, consistent with Office selection and Undo behaviour:

1. Delete on a protected first row is refused from the row popup and from the
   keyboard, the row and the backing list stay aligned, the existing
   `ValueObjectForIntervalTable.YouCannotRemoveTheFirst` message is shown, and
   the project is not marked unsaved.
2. Cut on a protected row is refused as a whole: nothing reaches the clipboard,
   so a paste cannot duplicate a row that stayed in the table.
3. A multi-row selection cuts the removable rows only.
4. The assignment entry table keeps `ActionList.assignmentEntry` (Copy only) as
   its row command, so its popup and Ctrl+C copy the selected resource row and
   Delete/Cut/Paste stay absent. The timesheet table declares no row command;
   its dialog keys are installed but inert, and it opens no row popup.

## Current slice

**Next slice:** none. The assignment dialogs now own the editing keys and keep
their configured Copy command; the pull request's required `build` and
`GUI acceptance` checks remain.
**Done when:** the pull request's required `build` and `GUI acceptance` checks pass.
**Then:** close after the reported symptom is confirmed on the packaged desktop build.

## Latest evidence

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.assignment.AssignmentDialogTableRouteTest" --console=plain` - failed before the wiring (the assignment entry table had no configured Copy command or popup and neither assignment dialog bound the editing keys) and passed after.
- `.\gradlew.bat :microproject_ui:test --console=plain` - passed.
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.assignment.AssignmentDialogGuiAcceptanceTest.robotAssignRouteRejectsLockedTaskThenAssignsAndUndoes" --console=plain` - passed on a Windows desktop session (1 journey, none skipped); the Robot journey now presses Ctrl+C in Assign Resources and requires the selected resource row on the clipboard.
- Previous slice, not rerun for this change: `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.ResourceSheetRowPopupGuiAcceptanceTest" --tests "com.microproject.dialog.ResourceIntervalTableKeyboardGuiAcceptanceTest" --tests "com.microproject.dialog.ResourceAvailabilityRowDeleteGuiAcceptanceTest" --console=plain` - passed on a Windows desktop session (5 journeys, none skipped).
- Red check: the keyboard journey fails for both tables with the `InformationDialog` wiring removed, so the new journey covers the fix.
- Table verification before the popup fix: the resource sheet showed two identically labelled "New" items (`InsertTask` and `InsertResource`) and a task-only "Paste Inserted Tasks"; the assignment table showed the same task-only items.
- The timesheet table (`TimesheetEntryPane`) sets an empty action list on purpose, so it opens no row popup; that is the intended contract, not a missing menu.
