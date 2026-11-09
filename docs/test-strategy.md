# Test strategy

## Why this exists

Machine time is not the bottleneck: the unit suite (about 3,100 tests) runs in
about two and a half minutes, and the GUI smoke suite in about seven minutes on
CI. The cost is in how work is verified. Agents and contributors were writing
one physical Robot journey per command family, running the full `guiTest`
(45+ minutes) and the ja/en × 100/125/150% matrix for ordinary changes, and
waiting on Windows CI before they could finish. Most of those Robot tests
check the model, Undo/Redo, and MPO reload, which a headless test observes
just as well, in milliseconds, on every OS.

This document sets the rule: **test each contract at the lowest layer that can
observe it.** It refines [GUI quality gate](gui-quality-gate.md); where the
two disagree on which layer or which run is required, this document wins.

## Layers

| Layer | Where | Runs | What belongs here |
|---|---|---|---|
| L1 model | `src/test` in core, application, exchange | every PR, 3 OSes | Scheduling, calendars, hierarchy, file read/write, Undo of model edits. |
| L2 headless UI | `microproject_ui/src/test` (`java.awt.headless=true`) | every PR, 3 OSes | Command route: dispatch a `CommandId` (see `CommandRouteMatrixTest`), then check the model, rejection result, Undo/Redo, and MPO reload. Enablement and selection rules. Dialog content-panel layout with the container-based `DialogLayoutAssertions` checks. Renderers and table models. |
| L3 physical journey | `microproject_ui/src/guiTest`, tagged `@GuiJourney` | every PR (Windows) | One Robot test per **input mechanism** (below), proving that a real click, key, or drag reaches the canonical command and that the result is visible. Not one per command. |
| L4 visual audit | full `guiTest` and the locale × scale matrix | weekly schedule, stable release, or shared layout changes | Screenshots, clipping at 125/150%, English layout, window shell. |

A command that is reachable from the ribbon, a menu, a context menu, and a
shortcut shares one canonical command object (gate rule "one command
pipeline"). Its behavior is tested once at L2. Each input mechanism is tested
once at L3. Do not multiply the two.

## Input-mechanism journeys

`-PguiTestSuite=journeys` runs exactly the tests annotated with
`com.microproject.testsupport.GuiJourney`. There is one per mechanism:

| Mechanism | Journey |
|---|---|
| `RIBBON_CLICK` | `TaskInformationRibbonGuiAcceptanceTest.linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency` |
| `KEYBOARD_SHORTCUT` | `TwoDocumentShortcutIsolationGuiAcceptanceTest.physicalCtrlDeleteAndUndoStayWithEachActiveDocument` |
| `CELL_EDIT` | `U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction` |
| `GANTT_DRAG` | `GanttBarDateDragGuiAcceptanceTest.robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel` |
| `CONTEXT_MENU` | `TaskInformationRibbonGuiAcceptanceTest.robotTaskPopupInsertAddsSelectedRowCountBelowSelectionAndOneUndoRevertsBatch` |
| `HEADER_CLICK` | `ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources` |
| `DIALOG` | `ChangeWorkingTimeDialogGuiAcceptanceTest.robotOpensWorkingTimeDialogAndCancelsWithoutCommit` |
| `FILE_CHOOSER` | `RibbonExternalCommandGuiAcceptanceTest.robotEscapeAndCancelCloseTheFileOpenChooserWithoutOpeningAProject` |
| `WINDOW_SHELL` | `ApplicationCloseGuiAcceptanceTest.closingTheApplicationWindowLeavesOnlyTheProjectFile` |

The `smoke` suite still uses the hand-maintained list in
`modules/microproject_ui/build.gradle.kts` until the migration below replaces
it with the journey tag.

## What to run for a change

| Change | Required tests | Not required |
|---|---|---|
| Refactor with no behavior change | Compile and the owning module's `test` | Any `guiTest` |
| Model or command behavior | L1/L2 test of the canonical command: before/after state, rejection, Undo/Redo; MPO reload when project data changes | A new Robot test |
| New or changed physical route (new button, shortcut, drag, menu) | L2 route test that the route reaches the command; the journey for that mechanism, through CI | A Robot test per command on that route |
| Dialog layout | L2 content-panel layout assertion; CI journeys | The visual matrix, unless shared layout code changed |
| Shared layout, theme, scaling, or window shell | L2 assertions; `visual` or `full` CI dispatch | — |
| Release | `full` and the visual matrix (release workflow) | — |

Rules for agents and contributors:

- Do not run the full `guiTest` locally for an ordinary change. CI runs the
  journeys on every PR; the full suite runs on schedule and for releases.
- PR evidence is the test names you added or changed, the commands you ran
  with their result, and the CI run link. Screenshots are needed only for
  visual changes.
- A new Robot test needs a reason that no L2 test can give: a new input
  mechanism, or a defect that only reproduces through real OS input or focus.
  Write that reason in the test's Javadoc.
- A GUI bug still needs a regression test that fails before the fix. Put it at
  the lowest layer that reproduces the defect.

## Migration

The existing suite predates this rule. Work through these issues, tracked
under #773:

1. Switch `smoke` to the journey tag and remove the hand-maintained list.
2. Move command-behavior and dialog-layout checks from `guiTest` to L2, and
   move unit-level classes out of `guiTest`.
3. Split the largest test classes and remove dependence on the run date.

Until each is done, keep the existing tests passing; do not delete a Robot
test before its L2 replacement is merged.
