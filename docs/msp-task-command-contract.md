# MSP task-command contract and evidence matrix

This is the compatibility record for issue #530/#464.  It replaces a
desktop-to-desktop comparison (which is not reproducible in the build
environment) with Microsoft's published Project desktop behavior and
microProject's executable evidence.  A row is only considered conformant when
the command has one canonical route and the observable model, selection,
undo, and persistence checks pass.

## Published MSP baseline

The baseline is Microsoft's [keyboard shortcut reference](https://support.microsoft.com/en-us/accessibility/project/keyboard-shortcuts-for-project),
[subtask and summary-task guidance](https://support.microsoft.com/en-us/project/create-and-work-with-subtasks-and-summary-tasks-in-project-desktop),
and [task-link guidance](https://support.microsoft.com/en-US/project/change-a-task-link).
Those pages define the user-visible semantics; they do not imply that an
internal implementation or file format must be identical.  In particular,
`OutlineLevel` is the hierarchy observable exposed by Project's object model
([Microsoft Learn](https://learn.microsoft.com/en-us/office/vba/api/project.task.outlinelevel)).

## Command contract

| Command / MSP semantic | Ribbon | Main menu | Task popup | Shortcut | Selection and lock precondition | Undo / save-reload acceptance | Evidence |
|---|---|---|---|---|---|---|---|
| Task Information (display the selected task details) | `RibbonTaskInformation` | Information action | Information item | `Shift+F2`, `Ctrl+Shift+P` | Typed task-row selection; disabled for no selection, read-only, or locked task | Dialog edits are one undo unit and survive native save/reload | `TaskInformationRibbonGuiAcceptanceTest.robotClickOnTaskPropertiesInformationOpensTaskInformation`; `MicrosoftShortcutsRootPaneTest` |
| Insert task | `RibbonInsertTask` / `RibbonInsert` | Insert task action | Insert item | `Insert` (and existing `Ctrl+K`) | Active task sheet and writable document; insertion is rejected without a valid selection | New task appears in the model and after save/reload; undo removes it | `RibbonButtonBehaviorTest`; `RibbonExternalCommandGuiAcceptanceTest`; `MicrosoftShortcutsRootPaneTest` |
| Indent / Outdent (create or remove subtask hierarchy) | `RibbonIndent`, `RibbonOutdent` | Indent/Outdent actions | Same command items | `Alt+Shift+Right`, `Alt+Shift+Left` | Whole task-row selection; first-level outdent and locked/read-only documents are rejected | `OutlineLevel`/parent-child relationships, visible rows, undo/redo, and save/reload agree | `TaskInformationRibbonGuiAcceptanceTest` indent/outdent cases; `MicrosoftShortcutsRootPaneTest`; `RibbonButtonBehaviorTest` |
| Link / Unlink tasks (dependency relationship) | `RibbonLink`, `RibbonUnlink` | Link/Unlink actions | Link/Unlink items | `Ctrl+F2`, `Ctrl+Shift+F2` | Two compatible task rows for link; existing dependency for unlink; reject cycles and locked/read-only state | Dependency model and Gantt links update, one undo unit, and round-trip through native persistence | `RibbonButtonBehaviorTest`; `MicrosoftShortcutsRootPaneTest`; dependency GUI acceptance tests |
| Delete selected task/data | `RibbonDelete` | Delete action | Delete item | `Delete`, `Ctrl+-` | Typed selection (cell vs whole row); reject read-only/locked mutation | Model/view selection is repaired, undo restores the exact row/data, save/reload preserves result | `RibbonButtonBehaviorTest`; `MicrosoftShortcutsRootPaneTest`; task-table Robot tests |
| Expand / Collapse / Show all | `RibbonExpand`, `RibbonCollapse` | Outline actions | Outline items | `Alt+Shift+=`, `Alt+Shift+-` | Task/summary row selection; no mutation in read-only view | Visible row set and selection remain consistent; undo/redo where applicable; persisted outline remains intact | `RibbonButtonBehaviorTest`; `HideShowTasksGuiAcceptanceTest`; `MicrosoftShortcutsRootPaneTest` |
| Task Mode (manual / automatic scheduling) | `RibbonTaskModeManual`, `RibbonTaskModeAutomatic` | `TaskModeManual`, `TaskModeAutomatic` actions | Task row popup items | `Ctrl+Shift+M`, `Ctrl+Alt+M` | Typed task-row selection; reject no selection, read-only, or locked mutation | `TaskModeService` applies one Undo edit; `manuallyScheduled` survives MPO save/reload | `TaskModeServiceTest`; `MpoFileImporterTest.taskSchedulingModeSurvivesMpoRoundTrip`; route matrix (Robot pending) |
| Status Date / Mark on Track / Update Project | `RibbonStatusDate`, `RibbonMarkOnTrack`, existing `RibbonUpdateProject` | matching action ids | Status Date and Mark on Track in task-row popup | `Ctrl+Alt+S`, `Ctrl+Shift+T` | Writable project; Status Date opens a date/NA dialog and cancel is a no-op; the ribbon displays the current localized date value followed by `Status Date:`; Mark on Track requires a typed task-row selection; Update Project defaults to Selected Tasks when tasks are selected and Entire Project otherwise | Status Date is separate from Update Project's “through” date; Update Project defaults both date fields to the effective Status Date, skips summary-task date mutations, and is one Undo/Redo unit without changing Status Date; Status Date edits use one Undo/Redo edit and survive MPO round-trip | `TaskProgressServiceTest`, `UpdateProjectRequestTest`; `MpoFileImporterTest.statusDateSurvivesMpoRoundTrip`; `TaskInformationRibbonGuiAcceptanceTest` physically selects a date and NA, verifies Cancel, model/ribbon Undo/Redo and MPO reload |

Status Date entry and visibility are documented in Microsoft's [Start fields
reference](https://support.microsoft.com/en-us/project/start-fields) for Project
Online Desktop Client and Project Standard/Professional 2016–2024. Microsoft
documents where to set the value, but does not specify the ribbon control's
exact inline text ordering; value-before-caption is a document-derived UI
decision, not a claim that the local screenshot could be inspected (its GitHub
attachment URL currently returns 404). `Ctrl+Alt+S` and `Ctrl+Shift+T` are
retained microProject convenience shortcuts; Microsoft's published shortcut
reference does not document either binding, so they are not claimed as MSP
shortcuts.

### Route invariant

MSP compatibility references: [Update completed tasks quickly](https://support.microsoft.com/en-us/project/update-completed-tasks-quickly) and [Update work on a project](https://support.microsoft.com/en-us/project/update-work-on-a-project), for Project Online Desktop Client and Project Standard/Professional 2016–2024. The former places Mark on Track on Task → Schedule and instructs users to select the tasks to update. It explicitly says that an empty selection means Entire Project for Update Project, but does not say that for Mark on Track. Therefore the no-selection-disabled rule is a document-derived compatibility decision that preserves the documented selection scope. The guide describes the result as scheduled percent complete; this implementation derives that percentage using each task's working calendar. For Japanese labels and location, see Microsoft's [Project working-time guide](https://support.microsoft.com/ja-jp/project/set-the-general-working-days-and-times-for-a-project) and [Status Date field documentation](https://support.microsoft.com/ja-jp/project/start-fields). These are the currently published desktop guides and list applicable editions through Project 2024.

Every surface resolves to the same typed `DocumentFrame.routeTaskCommand`
pipeline (selection resolver → writable/lock guard → domain mutation → one
Undo operation → view refresh).  The root-pane `WHEN_IN_FOCUSED_WINDOW`
InputMap is the sole global shortcut layer.  Popup and menu actions delegate to
the same action constants; they must not mutate the model directly.  This is
why a route can be tested once for semantics and then tested at each physical
surface for dispatch.

### #558 headless boundary fixture and physical handoff

The reusable core fixture uses a writable project with one leaf task, a zero-day
milestone, and a second task linked by a predecessor dependency.  The route
matrix must exercise `CommandId.UPDATE_PROJECT` with: no selection, read-only
project, invalid/empty date, milestone, progress at 0% and 100%, and linked
tasks.  Expected results are `REJECTED` with a stable reason for invalid
preconditions, `NO_CHANGE` when the requested state is already present, or
`CHANGED` with affected task IDs and `activeView=task`.

The physical handoff target is
`TaskInformationRibbonGuiAcceptanceTest.updateProjectRibbonRouteOpensAndConfirmsDialogPhysically`;
the same visible ribbon item is `RibbonUpdateProject`, the menu action is
`UpdateProject`, and all surfaces must resolve through
`DocumentFrame.routeTaskCommand(CommandId.UPDATE_PROJECT)`.  The final Robot
journey must assert model/view state, one Undo/Redo, and MPO save/reload.

## Issue #592 — Status Date, progress controls, and working time

Microsoft's [Start fields reference](https://support.microsoft.com/en-us/project/start-fields)
documents Status Date as a project-level scheduling reference. [Update completed
tasks quickly](https://support.microsoft.com/en-us/project/update-completed-tasks-quickly)
documents selecting tasks before Mark on Track. [Create a new base
calendar](https://support.microsoft.com/en-us/project/create-a-new-base-calendar)
documents the Change Working Time entry point, Work Weeks > Details, day-level
working/nonworking settings, and working-time intervals. [Add a holiday to the
project calendar](https://support.microsoft.com/en-us/project/add-a-holiday-to-the-project-calendar)
documents date-bounded Exceptions and their Details/recurrence settings. These
Microsoft Project desktop guides cover supported Project desktop editions as
listed on those pages; an observed UI difference not covered by them is marked
as a document-derived implementation decision rather than empirical MSP proof.

| Command | Required MSP behavior | Current microProject route/contract | Evidence / remaining scope |
|---|---|---|---|
| Status Date | Project-level date is independently editable and visible; NA is a distinct unset state. | Ribbon/date-picker, popup and shortcut converge on one editor; Cancel is a no-op; date/NA changes are undoable and persist. | Physical routes and model/ribbon/Undo/Redo/save-reload checks pass in the targeted ja/en × 100/125/150% matrix. |
| Mark on Track | User selects the tasks to update; completion follows the schedule through the status date. | Typed task-row selection, working-calendar calculation, one mutation/Undo path; no-selection is disabled/rejected. Summary values are derived from child tasks and are not directly written. | Ribbon, menu, popup and shortcut Robot paths verify changed/restored model state; core boundaries cover 0/100%, milestone, working time, summary aggregation, and Undo/Redo; targeted locale/DPI runs pass. |
| Update Project | User chooses progress update, date, and scope (selected tasks or whole project); it is separate from changing Status Date. | Defaults to selected tasks when selected, whole project otherwise; date fields use Status Date; one Undo edit; Cancel leaves progress and Status Date unchanged. Summary rows remain derived and unmodified; selected scope excludes an unselected linked successor while entire-project scope includes all eligible leaves. | Ribbon submission and menu scope/default/Cancel Robot paths, linked predecessor/successor + summary core test, Undo/Redo and MPO reload pass. UI Robot routes use the canonical selection/dialog pipeline; model boundary is separately exercised with hierarchy and link. |
| Change Working Time | Calendar selection; standard weekly workday/time rules; date-specific Work Weeks and Exceptions; Details defines selected days/times and recurrence for exceptions. Resource-specific schedules are edited through the resource calendar. | Work Weeks tab adds, edits, and removes dated patterns with independent weekday work/nonwork and interval editing. Exceptions tab adds/edits/removes dated and recurring exceptions; Add initializes its date range from calendar selection; Details retains working hours and daily/weekly/monthly/yearly recurrence bounds. Recurrence rules drive schedule occurrences and survive MPXJ import/export and project-calendar POD round-trip without flattening. All edits remain staged until parent OK and commit as one compound Undo. | `robotAddsRecurringExceptionCommitsUndoesRedoesAndPersists` physically edits resource-, project-, and task-specific calendars in one dialog, confirms staging until OK, one Ctrl+Z/Ctrl+Y for all three, and POD reload for all three. `TaskInformationRibbonGuiAcceptanceTest.robotChangeWorkingTimeCommandResolvesSelectedResourceCalendar` verifies the ribbon route resolves the selected resource calendar and Cancel leaves it unchanged. `PodRoundTripTest.resourceSpecificRecurringCalendarSurvivesNativePodRoundTrip` isolates resource recurrence serialization. Project ribbon route opens/cancels. Exceptions Add and Details layout bounds passed ja/en × 100/125/150%. No installed-MSP empirical claim is made. |

Recurring calendar exceptions are represented as retained rules, with expanded
occurrences used for scheduling but kept distinct from explicitly dated
exceptions when exporting. Core tests cover rule expansion and invalid inputs;
exchange and native POD tests cover import/export and save/reload. A physical GUI
journey creates a recurring exception from a selected date, commits it, verifies
one Undo/Redo, and reloads the retained recurrence rule from POD. The editor
exposes all four recurrence patterns and both end conditions; the current Robot
semantic journey exercises daily/count-bounded recurrence, while recurrence
model/exchange tests cover representative weekly, monthly, and yearly cases.
Rules reject malformed values before mutating the calendar.
For absolute monthly/yearly patterns, a requested day absent from a month is
clamped to that month's last day (for example, February 29 becomes February 28
in non-leap years). Microsoft's [Project XML `Exception` schema](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/exception-element?view=project-client-2016)
defines `MonthDay` and the recurrence range but does not specify this invalid-date edge; this is recorded as a
document-derived compatibility decision, cross-checked against MPXJ's MSPDI
occurrence expansion, not as a measured MSP-client result.

Change Working Time command contract: precondition is a writable selected
calendar, including an editable project calendar or resource-specific calendar
shown in `For calendar`. Microsoft documents resource-specific schedules in
[Set a unique schedule for a specific resource](https://support.microsoft.com/en-us/project/set-a-unique-schedule-for-a-specific-resource).
In-dialog changes remain scratch state until OK; Cancel must discard all edits.
The canonical state transition must apply the exact calendar diff, refresh
calendar consumers, create one Undo unit, and survive native save/reload.
Specific date Exceptions override Work Weeks; a bounded Work Week overrides the
regular weekly pattern only within its inclusive range. The latter priority is
a narrow Office-consistent document-derived rule because the cited support
pages describe the controls but not precedence between overlapping rules.

The Exception Details editor must retain the MSP distinction between a single
date-range exception and a recurring exception. The core model now retains the
recurrence rule (including its end condition) alongside schedule occurrences,
and converters preserve it rather than writing occurrences as one-off dates.
The exception editor and physical test now exercise this document-derived
requirement from the `Details`/recurrence control
documented in Microsoft's [Add a holiday to the project calendar](https://support.microsoft.com/en-us/project/add-a-holiday-to-the-project-calendar)
guide, not a claim about undocumented recurrence edge cases.

Work Weeks Details edits each selected weekday independently, including
working/nonworking state and all configured daily working-time intervals. A
name and date range editor alone is not equivalent. Adding, replacing, or
removing a Work Week or Exception is part of the same pending calendar edit:
switching calendars stages it; parent OK commits all staged calendars as one
Undo unit; Cancel discards every staged change. Robot acceptance proves the
dialog Add/Details route, parent OK, one Undo/Redo and native POD round-trip for
Work Weeks and recurring Exceptions. One semantic journey edits resource-,
project-, and task-specific calendars before parent OK, then proves staging,
compound Undo/Redo, and persistence for all three. Calendar-switch cancellation
is independently tested.

## Existing evidence and DPI/locale audit

The visual harness is parameterized by `guiTestLocale` and
`guiTestUiScale` in `modules/microproject_ui/build.gradle.kts`.  The targeted
Task Information/Ribbon Robot fixture is
`TaskInformationRibbonGuiAcceptanceTest`; it asserts physical selection,
button enablement, dialog visibility, tab/view construction, model state, and
cleanup.  `RibbonTabGuiAcceptanceTest` additionally checks real mouse clicks,
collapsed popup reachability, command-band bounds, and screen captures.

Issue #592 targeted Robot rerun (2026-09-23) exercised Status Date ribbon/date
picker, popup and shortcut; Mark on Track ribbon, popup, shortcut and disabled
no-selection state; Update Project ribbon submission and menu dialog/Undo/Redo/
MPO reload; and Change Working Time open/Cancel. All selected cases passed for
Japanese and English at 100%, 125%, and 150% scale. This matrix verifies the
existing surfaces. Work Weeks editing is additionally covered by a Robot path
for Add, Details, parent OK, physical Ctrl+Z/Ctrl+Y, and native POD save/reload.
Exception editing is covered by a Robot path for selected-date initialization,
recurring Details, parent OK, physical Ctrl+Z/Ctrl+Y, and native POD save/reload;
the Project ribbon button's open/Cancel route is separately tested. The physical
Exceptions Add and Details surfaces passed Japanese/English × 100/125/150%
bounds checks. A fresh targeted visual audit (2026-09-24) also ran
`ChangeWorkingTimeDialogGuiAcceptanceTest.robotOpensWorkingTimeDialogAndCancelsWithoutCommit`
for all six locale/scale pairs, asserting the calendar, Work Weeks, Exceptions,
Work Week Details, and Exception Details surfaces; it ran
`TaskInformationRibbonGuiAcceptanceTest.robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix`
for all six pairs, asserting the Status Date value-before-caption rendering and
the Status Date and Update Project dialogs. Each case opens the real UI through
Robot and cancels without committing project changes. All twelve runs passed.
Treat that matrix as layout evidence only; semantic mutation, Undo, and
persistence are verified once in a representative GUI environment, not
repeated at every DPI/locale.
This separation is deliberate: scaling can expose clipping, overlap, or controls
outside the viewport, while locale can change label dimensions. Neither changes
the calendar mutation contract, so rerunning the full semantic journey six
times adds runtime without an independent behavioral assertion. The resource
calendar editability boundary is one additional Robot case because it exercises
a distinct scope/precondition, not another visual permutation.

Run each as a separate Gradle process (the JVM UI scale is startup-only):

```powershell
foreach ($locale in @('ja','en')) {
  foreach ($scale in @('1','1.25','1.5')) {
    .\gradlew.bat :microproject_ui:guiTest `
      "-PguiTestLocale=$locale" "-PguiTestUiScale=$scale" `
      --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest" `
      --tests "com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest" `
      --max-workers=1 --no-daemon --console=plain
  }
}
```

At 125% and 150%, the harness performs bounds/capture and targeted command
checks; the full-width all-command sweep is intentionally limited to 100% by
the documented `W-464-DPI-FULLWIDTH` waiver.  This is a screen-width test
limitation, not a semantic waiver.  The existing acceptance record in
`TEST_PLAN.md` reports successful Japanese and English 100/125/150% targeted
Task Information runs and the corresponding high-DPI full-suite runs.

### Evidence matrix (issue #464)

| Locale | 100% | 125% | 150% | Scope / source |
|---|---|---|---|---|
| 日本語 (`ja`) | PASS (recorded) | PASS (recorded; targeted rerun passed) | PASS (recorded) | Task Information, Ribbon, Assignment, CCPM dialog layout and Robot checks |
| English (`en`) | PASS (recorded) | PASS (recorded) | PASS (recorded) | Same targeted surfaces; high-DPI full-suite results are recorded separately |

The current rerun initially exposed an unrelated test-source compile blocker in
`CommandRouteMatrixTest` (an anonymous-class field name shadowed the local
`TestFrame`); renaming the local to `testFrame` fixed that source error. A
subsequent Gradle daemon was externally stopped while rebuilding, so that
attempt is not counted as a product or layout failure. No waiver is applied to
Task Information, Assignment, CCPM, or targeted ribbon layout assertions.

## Remaining gaps

The published MSP list includes commands without a microProject equivalent
(for example some Timeline/Project Online-only commands and “show all outline
levels” where no registered handler exists).  They remain explicitly unbound
and must not be represented as successful no-ops.  A future issue should add a
typed action plus the same route/evidence row before binding any such key.
