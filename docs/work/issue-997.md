# Issue #997 — Resource table first row and the other tables' row menus

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/997
Contract frozen: issue body at intake
Baseline HEAD: `255a82274`

## Goal and scope

The Resource Information dialog's Resource Availability and Cost Rate tables
refuse to remove their first row, and the earlier `SpreadSheet.hasRowPopup` fix
exposed the row popup of every other non-task table. Both routes must leave rows,
backing data, the clipboard and the saved state consistent, and every command the
popup offers must act on the clicked row.

Out of scope: adding an undo stack to the interval tables; changing the MPO/POD
wire formats; Project Server/PWA behaviour.
Blockers: none.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | First availability row: Delete and Cut are refused with the existing message, row and data stay aligned, the project is not marked unsaved | Pass | `ResourceAvailabilityRowDeleteGuiAcceptanceTest`, `ResourceTableEditDirtyTest.firstAvailabilityRowCannotBeDeletedOrCut` |
| A2 | Same contract for the first cost rate row | Pass | `ResourceCostRateRowDeleteGuiAcceptanceTest` (PR #1140), `ResourceTableEditDirtyTest.firstCostRateRowCannotBeDeletedOrCut` |
| A3 | A refused cut leaves the clipboard unchanged | Pass | PR #1098; the Robot journey compares the clipboard marker before and after the refusal |
| A4 | The decision is recorded as a document-derived compatibility decision | Pass | "Decision" below |
| A5 | The resource sheet popup offers its own commands once, no task-only command, and Delete removes the row | Pass locally | `NonTaskRowPopupCommandsTest.resourceSheetPopupOffersItsOwnCommandsOnce`, `resourceSheetPopupDeleteRemovesTheSelectedResource` |
| A6 | The Task Information dependency table offers Delete and it removes the link | Pass locally | `NonTaskRowPopupCommandsTest.dependencyTablePopupDeleteRemovesThePredecessor` |

## Decision

Document-derived compatibility decision (not a measured MSP run).

Microsoft documents both tables as effective-dated rows that start from the
resource's current entry and gain further rows only when the rate or the
availability changes:

- Resource availability: https://support.microsoft.com/en-us/project/change-resource-availability-without-using-a-calendar
- Resource cost rates: https://support.microsoft.com/en-US/project/enter-costs-for-resources

The first row therefore carries the entry that applies before the first change.
With it removed there is no entry for the period before the earliest remaining
date, so the table, and the availability lookup that reads it, would disagree.
Microsoft documents adding rows and changing their dates, not removing the
initial row.

Rules this fork applies, consistent with Office selection and Undo behaviour:

1. Delete on a protected first row is refused, the row and the backing list stay
   aligned, the existing `ValueObjectForIntervalTable.YouCannotRemoveTheFirst`
   message is shown, and the project is not marked unsaved.
2. Cut on a protected row is refused as a whole: the row is not put on the
   clipboard, so a paste cannot duplicate a row that stayed in the table.
3. A multi-row selection cuts the removable rows only.

## Current slice

**Next slice:** verify the remaining newly exposed popups in a desktop session
(resource assignment, timesheet, project table) and record the result here.
**Done when:** the remaining tables' commands are checked, the required `build`
and `GUI acceptance` checks pass on the pull request, and the issue records the
outcome.
**Then:** close after the reported symptom (a protected first row and the other
tables' menus) is confirmed on a desktop build.

## Latest evidence

- Focused command: `.\gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.NonTaskRowPopupCommandsTest" --console=plain`.
- Before the popup change the resource sheet showed two identically labelled "New"
  items (`InsertTask` and `InsertResource`) and a task-only "Paste Inserted Tasks".
- Physical GUI: the availability and cost rate Robot journeys run in the Windows
  `GUI acceptance` workflow; the resource-sheet and dependency-table routes are
  covered headlessly at the action layer in this slice.
