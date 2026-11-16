# Issue #1181 — Full GUI acceptance failures

State: IN PROGRESS

Issue: https://github.com/tetsuji16/microProject/issues/1181

Baseline: `98065edaa8638d228dc49efa0cfb0566f78f0eaa` (origin/master), isolated worktree clean at intake.

## Contract

| ID | Required result | Status / evidence |
|---|---|---|
| A1 | Resolve the 21 failures in the full Windows GUI suite, including real command and view behavior | Open: [stable release run](https://github.com/tetsuji16/microProject/actions/runs/43721423702), 257 tests, 21 failures; executor also reported heap exhaustion |
| A2 | Full GUI suite passes on hosted Windows | Open |
| A3 | Re-run stable release by pushing master to `release/stable-0.1.0` | Open, after A1–A2 and integration |

The logged heap exhaustion does not establish the cause of the individual assertions. Preserve all 21 original criteria and distinguish product defects, stale test routes, and environment failures using evidence.

## Current slice

Integrate and run the full hosted Windows GUI acceptance on the reviewed candidate. Done when the full suite and required locale/scale legs pass on that candidate. Next: integrate to master and rerun the stable release gate as originally required.

## Evidence

- Hosted run: Windows 100% Japanese functional full gate failed; locale/scale matrix was not reached. See linked run and `gui-acceptance-evidence-43721423702` artifact.
- Local environment: Windows 11, Temurin JDK 25.0.4.1, Japanese default GUI locale.
- On baseline `98065edaa`, `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.mutationCommandsAreDisabledWithoutTaskSelection" --max-workers=1 --console=plain` failed at `RibbonHideSelectedTasks` not visible. The test selected View while the current configuration places Hide/Show in Task > Editing. After physically selecting Task before those checks, the same command passed on the working tree; original enablement assertions remain intact.

- Calendar tests initially failed looking for the removed `RibbonCalendarViewSettings` launcher. The current View > Calendar route opens an embedded view. After adapting the existing journeys, unlinking three tasks rescheduled them onto the same day; physical Calendar Format > Best Fit made all three visible. Undo restored the dependency model but left its marker stale. `DocumentFrame` now schedules task/dependency event refresh of standalone and embedded calendars on the EDT.
- Working-tree physical Robot command: `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.mutationCommandsAreDisabledWithoutTaskSelection" --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotCalendarCommandOpensUsableCalendarDialog" --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.calendarDependencyMarkerOpensTheLinkDialogAndCanRemoveTheLink" --max-workers=1 --console=plain` — all 3 passed, including dependency Remove, Ctrl+Z/Y, rendered markers and MPO reload. No hosted full gate has been run.
- Baseline journeys: `RibbonSaveBaseline` is now a menu; tests previously opened it without selecting the Save item. Shared physical menu helper now selects Save/Clear. Focused GUI run passed all five release-failing Baseline methods, including model, Undo/Redo and MPO assertions (working tree based on `98065edaa`).
- Filter/group tests opened a combo-box popup but expected a legacy dialog immediately. A shared physical popup helper now selects More Filters / More Groups. Both original journeys passed with model/view, selection, Undo/Redo and persistence assertions retained.
- View-state class: exclusive-view test passed in the isolated local run. Format Painter failed because targeting a different task with identical formatting never ended single-use mode. The model now ends the mode without creating an edit; the original physical test passes. Microsoft [Use the Format Painter](https://support.microsoft.com/en-us/word/use-the-format-painter), applicable to Project Professional/Standard 2021/2024 and Project desktop subscription, specifies single use on one click, repeated use after double-click, and Escape to stop.
- Ribbon tab/focus: `mouseClickSelectsEveryRibbonTabExactlyOnceAndKeepsTheCommandSurfaceVisible`, `viewRibbonKeepsTheLargeGanttButtonVisibleAndDispatchesItOnce`, and `backstageClosesBeforeTheOptionsCommandOpensItsModalDialog` passed locally; standard Backstage return test also passed. Backstage action wrapper remains an uncommitted candidate; focused navigation/theme unit tests passed but whole UI module remains unrun.
- `robotClicksEveryStandardRibbonCommandOnce` passed locally (1m 51s). Diagnostic assertions isolated the prior Backstage obstruction to hiding the active contextual tab: `firstVisibleTabId()` chose the File tab, opening Backstage instead of selecting a normal ribbon tab. It now excludes Backstage from the fallback. The sweep physically exercises popup items and direct controls according to control kind.
- Hosted full suite, exact candidate CI and release rerun remain open.
- Dialog-command physical sweep: all commands passed after restoring the Gantt view between journeys, preparing a saved resource pool, initializing the session job queue as startup does, and asserting embedded view transitions and the reused Resource Notes dialog correctly. Command: `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonDialogCommandGuiAcceptanceTest.everyDialogCommandOpensAWindowAndReleasesItsButton" --max-workers=1 --console=plain`.
- POD resource, large-project resource edit and MPO round trip, Report > Gantt, and Gantt resize/progress/split physical tests passed locally. The Gantt finish gesture had moved only 3 screen pixels, below the desktop drag threshold; its fixture now targets three calendar days and asserts a sufficient pixel delta.
- `./gradlew.bat :microproject_ui:test --console=plain` passed after calendar-refresh coalescing. The focused calendar dependency Remove/Undo/Redo/MPO GUI journey also passed afterward. Hosted exact-candidate evidence remains pending.
