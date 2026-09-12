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
| Status Date / Mark on Track / Update Project | `RibbonStatusDate`, `RibbonMarkOnTrack`, existing `RibbonUpdateProject` | matching action ids | Status Date and Mark on Track in task-row popup | `Ctrl+Alt+S`, `Ctrl+Shift+T` | Writable project; Mark on Track additionally requires typed task selection | Typed request/outcome source, Status Date/Mark boundaries, MPO persistence; physical GUI evidence remains final-gate work | `TaskProgressServiceTest`; `UpdateProjectRequestTest`; `MpoFileImporterTest.statusDateSurvivesMpoRoundTrip`; #557/#558 |

### Route invariant

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

## Existing evidence and DPI/locale audit

The visual harness is parameterized by `guiTestLocale` and
`guiTestUiScale` in `modules/micrproject_ui/build.gradle.kts`.  The targeted
Task Information/Ribbon Robot fixture is
`TaskInformationRibbonGuiAcceptanceTest`; it asserts physical selection,
button enablement, dialog visibility, tab/view construction, model state, and
cleanup.  `RibbonTabGuiAcceptanceTest` additionally checks real mouse clicks,
collapsed popup reachability, command-band bounds, and screen captures.

Run each as a separate Gradle process (the JVM UI scale is startup-only):

```powershell
foreach ($locale in @('ja','en')) {
  foreach ($scale in @('1','1.25','1.5')) {
    .\gradlew.bat :micrproject_ui:guiTest `
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
