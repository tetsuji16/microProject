# MSP Ribbon and structural-command comparison

This document is the evidence register for issues #548 and #549.  The
comparison is against Microsoft-published Project documentation; an installed
Microsoft Project Desktop application is not available in this environment, so
the rows marked `manual` remain open until a physical MSP run is recorded.

## Sources

* [Learn the Project 2010 ribbon](https://support.microsoft.com/en-us/office/learn-the-project-2010-ribbon-5038d333-8646-4c46-a0df-9be0ab380d8a)
* [Use a screen reader to explore and navigate Project](https://support.microsoft.com/en-us/accessibility/project/use-a-screen-reader-to-explore-and-navigate-project)
* [Create and work with subtasks and summary tasks](https://support.microsoft.com/en-us/project/create-and-work-with-subtasks-and-summary-tasks-in-project-desktop)
* [Edit a project in Project desktop](https://support.microsoft.com/en-us/project/edit-a-project-in-project-desktop)
* [Overview of Project views](https://support.microsoft.com/en-gb/office/overview-of-project-views-6cb1dbcd-5cd5-4cc2-a878-aa365564266d)

## Standard Ribbon tabs (#548)

| MSP standard tab (official source) | microProject tab | Implementation/test evidence | Status |
|---|---|---|---|
| File | `FileRibbonTask` | `RibbonStructureTest.standardRibbonUsesTheDesktopProjectTabOrder`; `RibbonCommandCatalogTest` | automated |
| Task | `TaskRibbonTask` | `RibbonStructureTest.taskAndResourceTabsSeparateOutlineAssignmentsAndTrackingWork` | automated |
| Resource | `ResourceRibbonTask` | `RibbonStructureTest.issue36ResourceCommandsMatchTheMsProjectRibbonModel` | automated |
| Report | `ReportRibbonTask` | `RibbonStructureTest.standardRibbonUsesTheDesktopProjectTabOrder`; catalog metadata validation | automated |
| Project | `ProjectRibbonTask` | `RibbonStructureTest.taskAndResourceTabsSeparateOutlineAssignmentsAndTrackingWork` | automated |
| View | `ViewRibbonTask` | `RibbonStructureTest.standardRibbonUsesTheDesktopProjectTabOrder`; catalog metadata validation | automated |
| Format | `FormatRibbonTask` | `RibbonStructureTest.standardRibbonUsesTheDesktopProjectTabOrder` | automated |

### Window-limited screenshot evidence

`RibbonTabGuiAcceptanceTest.mouseClickSelectsEveryRibbonTabExactlyOnceAndKeepsTheCommandSurfaceVisible`
uses a real visible `JFrame`, `Robot`, and the frame root-pane bounds.  For each
of the seven tabs it physically clicks the tab, asserts that exactly one tab is
selected and that the command surface remains visible, then captures only that
microProject window (not the desktop) with `Robot.createScreenCapture`.

Reproduction (Windows desktop session; do not run headless):

```text
.\gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest.mouseClickSelectsEveryRibbonTabExactlyOnceAndKeepsTheCommandSurfaceVisible" --max-workers=1 --console=plain
```

The test writes temporary evidence to
`modules/micrproject_ui/build/reports/guiTest-artifacts/ribbon-tab-{0..6}.png`
(`micrproject.gui.artifacts.dir` is configured by the Gradle `guiTest` task).
These PNGs are disposable build output and must not be copied into `docs/` or
committed.  The test also verifies the captured window is at least 900 pixels
wide and over 120 pixels high.  In the present source, all seven standard tabs
are implemented; no tab is recorded as unsupported.

The canonical order is asserted as File, Task, Resource, Report, Project,
View, Format.  Save, Undo, and Redo are intentionally limited to the quick
access toolbar, as required by the desktop information architecture.  This
proves the local structure and labels; it does not claim pixel-level or
version-specific equivalence with MSP.

## Structural commands (#549)

| Capability | MSP documented behavior | microProject implementation | Evidence |
|---|---|---|---|
| Multiple task selection | Indent/outdent applies to selected task(s) | `DocumentFrame.routeTaskCommand` resolves the task selection once and applies one mutation/undo path | `CommandRouteMatrixTest`; `TaskInformationRibbonGuiAcceptanceTest` |
| Indent / outdent | Task > Schedule > Indent/Outdent; Alt+Shift+Right/Left | `CommandId.INDENT` / `OUTDENT` through root-pane canonical route | `DocumentFrameHierarchyCommandTest`; `CommandRouteMatrixTest`; GUI acceptance test |
| Link / unlink | Dependencies are edited from the Task tab | `CommandId.LINK` / `UNLINK` in the document-frame route | `CommandRouteMatrixTest`; `GraphicManagerLinkRouteTest` |
| Insert / delete / cut / copy / paste | Task rows can be created, removed, and copied | `CommandId` routes to the document frame and returns typed `RibbonCommandResult` | `CommandRouteMatrixTest`; `RibbonButtonBehaviorTest`; `NodeListTransferablePasteFailureTest` |
| Read-only document | Editing commands are unavailable/rejected | command preconditions reject with `document-read-only`; paste and cell edits return false | `NodeListTransferablePasteFailureTest` (`readonlyProjectRejectsCellPaste`, `readonlyFieldRejectsCellPasteInsteadOfReportingASuccessfulNoOp`); route tests |
| Hidden/internal column | Views may hide fields without changing project data | the internal ID slot is retained and cannot be removed; user column layout is undoable | `SpreadSheetColumnPermissionTest`; `SpreadSheetColumnUndoLifecycleTest`; `CommonSpreadSheetSelectionStateTest` |
| Persistence | Structural edits must survive save/reopen | document generation and save/reload acceptance paths verify the model after redraw and reload | `DocumentGenerationTest`; `TaskInformationRibbonGuiAcceptanceTest` |

`CommandRouteMatrixTest` is the machine-readable inventory for all current
`CommandId` values.  It verifies that the canonical document-frame route is
used and that legacy action IDs round-trip to the same command.  It is a route
contract, not a claim that every command has identical MSP side effects.

## Verification

Focused local verification:

```text
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.menu.RibbonStructureTest" --tests "com.microproject.ui.ribbon.RibbonCommandCatalogTest" --tests "com.microproject.pm.graphic.frames.CommandRouteMatrixTest" --tests "com.microproject.pm.graphic.frames.DocumentFrameHierarchyCommandTest" --tests "com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnPermissionTest" --tests "com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnUndoLifecycleTest" --console=plain
.\gradlew.bat :micrproject_ui:test --console=plain
```

Both commands are expected to pass in a clean checkout.  The full physical
`guiTest` gate still has an unrelated pre-existing failure in
`CriticalChainStatusDialogGuiAcceptanceTest.unconfiguredNetworkOffersPhysicalRouteToCcpmSettings`; that failure is not used as evidence for these issues.

## Closure decision

The automated local contracts are complete.  #549 is **CLOSE-READY**: the
Microsoft public documentation is the normative compatibility source under
the repository rules, and the microProject route/model tests above provide the
implementation evidence.  No MSP installation is required to close it.

#548 is **CLOSED**.  On 2026-09-13 the focused Windows Robot test completed
successfully (`BUILD SUCCESSFUL`, 22s) and generated exactly seven temporary
window-only artifacts (`ribbon-tab-0.png` through `ribbon-tab-6.png`, each
non-empty) under `modules/micrproject_ui/build/reports/guiTest-artifacts`.
Neither issue required an MPO/POD format change.
