# GUI incident command inventory

This is the Phase 0 route and ownership record for GUI incidents carried by
[`gui-recovery-sequence.md`](gui-recovery-sequence.md) and its referenced
regression issues. A closed issue is not proof that every later-added route or
fixture was accepted. Rows with **RECHECK** remain open evidence questions.

## Baseline snapshot

- Repository revision: `bbf105092a2f2152815711a734b9f4c9fe0cb5d4` with local
  changes; `git status --short` was captured before building.
- Application version source: `build.gradle.kts` (`releaseVersion`, default
  `0.0.24`); development build metadata remains supplied by Gradle.
- Fresh distribution command: `.\gradlew.bat installDist --rerun-tasks
  --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL, 20 tasks
  executed. Output: `modules/microproject_ui/build/install/microproject_ui`.
- Last recorded physical GUI conditions for this worktree: Windows desktop,
  Japanese locale, UI scale 1.0, display 2560×1440. No display resolution was
  changed. This Phase 0 build itself was not a physical GUI run; recapture these
  values alongside the next Robot run before treating them as current evidence.
- Repository diagnostics preserved: `.git/m3-diagnostic.log` and
  `.git/timezone-utc-diagnostic.log`. No standalone app debug log was found
  among tracked files. Do not replace these diagnostics with dispatch-only
  success logs.
- Owner mapping: upstream issue assignment fields are empty for these closed
  incidents. Execution ownership is assigned to the primary agent for this
  repository; package phase is recorded in each row. This is local execution
  ownership, not a claim that GitHub assignees were updated.
- Compatibility claims use the Microsoft sources cited by each contract;
  document-derived rules are labeled as such and are not empirical MSP runs.

## Reported route contracts

| Issue / execution owner | Physical user route and precondition | Expected model/view result | Undo, redo, persistence | Current evidence / remaining question |
|---|---|---|---|---|
| [#60](https://github.com/tetsuji16/ProjectLibre/issues/60) — M2.4 / primary agent (upstream unassigned) | Physically select a real outline summary row and use Task ribbon **Hide Selected Tasks**; separately create a View > Group virtual row and select it. | Hiding a real summary hides its descendants, removes the empty summary branch from the task projection and Gantt, and leaves unrelated/read-only tasks unchanged. A virtual group resolves to editable member tasks and descendants. | Physical routes assert Undo/Redo, Show All, and MPO visibility persistence. | `robotHideActualOutlineSummaryAndDescendantsPersistsAndRoundTripsUndoRedo` verifies the real summary and descendants disappear from table/Gantt, survive hidden MPO reload, and return with Undo/Show All; it also asserts the expanded view state is preserved. Its Robot click targets the name cell away from the disclosure icon. `robotHideSelectedGroupedSummaryAndMembersPersistsAndRoundTripsUndoRedo` verifies group member IDs, descendants, recovery, and persistence. Both passed in the current-head full `TaskInformationRibbonGuiAcceptanceTest` class. Original-project-data comparison remains useful for user-specific repro details. |
| [#266](https://github.com/tetsuji16/ProjectLibre/issues/266) — M2.2 / primary agent (upstream unassigned) | Task ribbon **Unlink** with selected task(s); Task Information Predecessors/Successors > select one link > **Remove**; Calendar route with its single dependency marker. Test the multi-link case separately from single-link UI routes. | Remove only the incident/selected link set; model, task relation rows, and calendar dependency view agree. Unrelated links/task fields stay unchanged. | Calendar, Task Information cross-project, and Task ribbon multi-link Unlink now have route-specific physical Undo/Redo and MPO evidence for the tested paths. #266 remains open pending the wider Phase 2 audit. | Japanese/UI scale 1.0 runs without resolution changes: Calendar Remove passed model endpoints, unrelated-link preservation, rendered cards, physical Undo/Redo, and MPO reload. Task Information cross-project create and Remove passed for FS/SS/FF/SF, including both endpoints, rendered rows, physical Undo/Redo, and MPO reload. Task ribbon multi-link Unlink used a physical ribbon click and chooser keyboard input; it removed only the selected outgoing link, preserved the incoming link, updated the successor Predecessors cell and Gantt edge, then passed physical Ctrl+Z/Ctrl+Y and MPO reload. The Remove history-owner issue was tracked in [issue #870](https://github.com/tetsuji16/ProjectLibre/issues/870) and fixed in the current worktree. Focused GUI test passed; #266 remains open for broader recovery gates. |
| [#330](https://github.com/tetsuji16/ProjectLibre/issues/330) — M2.4 / primary agent (upstream unassigned) | Select a task row and use Task > Properties > Information; also exercise the reported Gantt task double-click route. | The Task Information dialog identifies the selected task and opens its corresponding tab. Opening/closing is view-only and leaves project model and dirty state unchanged. | Undo/Redo not applicable; no persistence mutation expected. | `TaskInformationGuiAcceptanceTest` and `TaskInformationRibbonGuiAcceptanceTest` cover the routes. Closed issue evidence is focused; the current-head physical rerun is pending Phase 1 revalidation. |
| [#460](https://github.com/tetsuji16/ProjectLibre/issues/460) — M2.4 / primary agent (upstream unassigned) | Route matrix: Project > Information; Resource > Change Working Time and New Base Calendar; Task > Clear Baseline; Task > Properties > Information. Inspect every tab and resize where supported. | Every label, editor, tab, action button, and title fits its parent and usable screen; no blank/incomplete Project Information panel. For edits, displayed values and project model agree. | View-only opening does not dirty project. Calendar/baseline edits use their defined Undo/Redo and MPO contract. | `ProjectInformationDialogGuiAcceptanceTest`, `ChangeWorkingTimeDialogGuiAcceptanceTest`, `BaselineDialogGuiAcceptanceTest`, and Task Information layout tests exist. **RECHECK** every surface added in later #460 comments against the evidence matrix in `docs/dialog-layout-audit.md`. |
| [#461](https://github.com/tetsuji16/ProjectLibre/issues/461) — Phase 3 / primary agent (upstream unassigned) | Resource > Create Resource Pool; verify the opened frame, then create/select a resource. | A registered resource-pool frame opens in the requested Resource view with its model initialized; it must not fall back to an empty Gantt. | Closing removes only the created document; resource data persists through its supported local save/reopen route. | `ResourcePoolInitialViewGuiAcceptanceTest.physicalCreateResourcePoolCommandOpensUsableResourceView` and the multi-project/pool journey exist. Current-head installed-layout rerun is pending. |
| [#462](https://github.com/tetsuji16/ProjectLibre/issues/462) — Phase 3 / primary agent (upstream unassigned) | From a project with tasks/resources, physically open Task Usage Detail and Resource Usage Detail through their Resource ribbon buttons. | Each becomes the active `UsageDetailView`, initializes its category/model, and displays rows for the fixture task/resource; no startup exception. | View switch is non-mutating; Undo and save are not applicable. | `TaskInformationRibbonGuiAcceptanceTest` lines 1231–1249 physically open both views and assert rows; `UsageDetailViewTest` is a helper unit test only. Current-head installed-layout rerun is pending. |
| [#463](https://github.com/tetsuji16/ProjectLibre/issues/463) — Phase 3 / primary agent (upstream unassigned) | With an assigned-resource fixture active, click Resource ribbon > Timesheet; close with Escape. | Timesheet's dedicated spreadsheet/model exists and shows the fixture assignment row. | Opening and Escape leave assignment work and project Undo history unchanged. | `TaskInformationRibbonGuiAcceptanceTest.timesheetRouteConstructsItsDedicatedSpreadsheet` physically covers route, populated row, non-mutation, and Undo history. Current-head installed-layout rerun is pending. |
| [#464](https://github.com/tetsuji16/ProjectLibre/issues/464) — Phase 1/6 / primary agent (upstream unassigned) | For each affected command, perform the documented mouse/keyboard gesture with its required selection, including ribbon focus changes. | Report one semantic changed/rejected/failed outcome with stable selected/affected IDs and active view; assert actual model and rendered state, not dispatch alone. | Assert Undo/Redo and persisted state for mutations; view-only commands preserve the model. | `docs/gui-quality-gate.md`, `docs/msp-task-command-contract.md`, and command acceptance fixtures define the shared gate. Current-head Japanese/100% run passed the acceptance class 58/58 after fresh `installDist`; broader GUI and locale/scale gates remain. |
| [#869](https://github.com/tetsuji16/ProjectLibre/issues/869) — M2.1/#414 / primary agent | Select an external task in an editable project and invoke Indent with the physical root-pane shortcut; compare ribbon enablement with the semantic result. | The ribbon remains disabled; shortcut reports `REJECTED/task-read-only` with the selected stable ID, and leaves hierarchy, visible order, and Undo history unchanged. | No mutation; Undo remains unchanged. | Resolver unit regressions passed (3 tests), physical GUI route passed (1 test), full UI unit suite passed, and serialized GUI acceptance passed 216 tests/49 suites with 0 failures/errors and 1 conditional skip at ja/100% and 2560×1440. No resolution-change test was run. Issue stays open pending later gates. |
| [#395](https://github.com/tetsuji16/ProjectLibre/issues/395) — Phase 4 / primary agent (upstream unassigned) | File > Open; select multiple project files in the native chooser. Also switch between two open projects and close each through its OS title-bar route. | Every selected project opens in its own registered frame; active frame, title, ribbon, and Resource Pool view agree; no orphan window remains. | Open/close does not rewrite project content; project identity and saved state survive reopen. | `MultipleLocalProjectOpenGuiAcceptanceTest.nativeFileOpenChooserSelectsTwoProjectsAndKeepsExistingProject` physically selects two MPO files in the native chooser and verifies the existing dirty document is preserved, both fixture contents/names open in separate windows, and all three documents stay registered. `ResourcePoolInitialViewGuiAcceptanceTest.physicalMultipleProjectAndResourcePoolWindowsSwitchCommandAndCloseCleanly` covers switching and OS title-bar close. Both run at the desktop's existing resolution; no resolution-changing test is used. |
| [#479](https://github.com/tetsuji16/ProjectLibre/issues/479) / [#482](https://github.com/tetsuji16/ProjectLibre/issues/482) — Phase 4 / primary agent (upstream unassigned) | On supported Windows runtime, drag native caption, use Alt+Space, maximize/restore, resize border, and close primary/secondary windows. | Windows owns native caption/system menu/snap/resize; bounds remain in usable work area; focus, active document, and title remain aligned. | Window actions do not mutate project data; closing each frame unregisters it. | `WindowShellNativeDecorationGuiAcceptanceTest` covers physical shell actions. Windows 11 client acceptance and exact-head run remain separate evidence requirements. |
| [#590](https://github.com/tetsuji16/ProjectLibre/issues/590) — Phase 5 / primary agent (upstream unassigned) | Physical routes for Change Working Time, Calendar Options, Help, Locale Settings, Update Tasks, and Recurring Task end-condition controls; include every issue-body/reply screenshot. | Text, fields, tabs, and buttons remain within the dialog, use preferred text height, do not overlap, and remain operable. | Opening/cancel is non-mutating; any applied data change has its own Undo/save contract. | Six-image assertion map is in `TEST_PLAN.md` U-29 and `docs/dialog-layout-audit.md`; remaining full matrix evidence is recorded there. |

## Phase 0 disposition

All rows have a local execution owner (the primary agent for this repository);
the upstream issues remain unassigned/closed as noted by their tracker state.
The required route, precondition, expected state, Undo/persistence fields are
recorded, and the current worktree has a fresh `installDist`. This satisfies
the Phase 0 inventory/build gate. **RECHECK** rows remain open behavior/evidence
work for their owning recovery phase. The #60 physical virtual-group case now
has current GUI evidence, while the specific earlier report of a summary row
remaining visible is still tracked until it can be compared with the original
project data. Do not treat issue tracker state as proof of behavior. #794
remains open.

The Phase 1 initial-command Robot gate was rerun after this build with
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest -Locale ja -Scale 1.0`.
It passed 59/59 on the installed current worktree, including the physical
virtual-group Hide journey. The runner reported
2560×1440; the resolution was not changed. This
revalidates Task Information, Link/Unlink, Indent/Outdent, Expand/Collapse,
Hide/Show, editor shortcuts, and selection shortage on the current tree; it is
not a full GUI suite or the broad locale/scale matrix.
