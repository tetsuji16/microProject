# Issue #794 evidence archive

This archive preserves detailed acceptance notes, Phase 6 evidence, and chronological slice history. Its “Next finite slice” section is historical and superseded by the current packet at [issue-794.md](issue-794.md).

## Project Information date failure-atomicity slice (2026-10-21)

- Added injected failures after successful scheduling recalculation for both `Project.setStartDate` and `Project.setFinishDate`. The injected delegate records that task dates changed before throwing; the finish case explicitly sets and asserts backward task scheduling. The first strengthened run exposed a fixture false lead: a task created after `Project.setForward(false)` retains its default forward schedule direction unless set separately. With the fixture corrected, the backward finish case showed `NormalTask.restoreDetail` left the live `TaskSchedule.finish` at its post-recalculation value although the captured task detail and direction were restored. Recalculating during rollback did not correct that value. Rollback now also snapshots and restores the live current schedule direction, start, and finish after detail restoration. Assertions cover project boundary/start and end constraints, task dates and dirty state, mutation revision, Undo/Redo capability and names, and zero object/schedule observer events; successful start/finish recalculation behavior is covered by focused tests.
- Microsoft documents setting the project start or finish date through Project Information and choosing one scheduling method: [Set the start date or finish date for your project in Project desktop](https://support.microsoft.com/en-gb/project/set-the-start-date-or-finish-date-for-your-project-in-project-desktop) (Project Desktop 2016–2024). Atomic rollback is a product invariant, not an MSP-measured behavior.
- `ProjectScheduleBehaviorTest`: `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.ProjectScheduleBehaviorTest" --console=plain --no-daemon --max-workers=1` — passed 30/30 after strengthening failure injection and restoring explicit live schedule fields. Sol then ran `.\gradlew.bat :microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL; fresh XML: 1,151 tests, zero failures/errors/skips. This verifies the current working tree based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`, not exact-candidate CI. No GUI behavior changed and no GUI/DPI matrix was run.

## NormalTask duration failure atomicity slice (2026-10-21)

- `NormalTask.setDuration(long)` now runs its existing mutation sequence through `Project.runAtomicModelCommand`; on failure it restores project task details and the duration-specific estimated/display-unit/imported-work metadata. No alternate command or Undo path was added. A local subclass injects after dependent invalidation; it asserts the task’s duration and start/finish changed and estimated/display unit changed before throwing, then verifies restoration of duration, actual/progress/date facts, dependent schedule date, dirty flags, revision, Undo/Redo state/names, and object/schedule observer counts.
- Public barrier call sites in `Project.setStartDate`/`setFinishDate` and `ScheduleService.runAtomicIntervalEdit` use a true changed result for direct mutations. `NormalTask.setDuration` follows that contract so a successful mutation with no pending event facts still publishes a schedule revision. A focused success test asserts exactly one revision. Failure-state Undo names are copied with `List.copyOf` before injection.
- The first counter assertion exposed that `ProjectTaskSnapshot.restore` recalculates and leaves transient task calculation-state counters advanced. The setter rollback now captures/restores counters for every project task; a source → successor → downstream fixture proves all three counters advance before injection and return to baseline after rollback.
- Core integration later showed two internal CriticalPath sentinels call `setDuration(0)` while detached from `project.getTasks()`. Successful publication now requires canonical task-list registration while rollback remains atomic for detached tasks. Added a detached-task no-revision test; attached-task success still asserts one revision. `docs/issue-execution.md` now requires caller/initialization/detached-object inventory before enabling low-level setter publication.
- This slice was inspected at `HEAD 1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with uncommitted candidate edits. Command: `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.NormalTaskDurationTest" --tests "com.microproject.transaction.ProjectMutationStateTest" --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL; XML reports 29 duration tests and 11 mutation-state tests, zero failures/errors/skips. The direct setter has no separate Undo command contract; rollback preservation of the existing Undo/Redo stack is asserted. No GUI or DPI checks apply to this headless core slice.
# Issue #794 — Roadmap gate M2: offline desktop workflow parity

State: OPEN. Issue: https://github.com/tetsuji16/ProjectLibre/issues/794
Dependencies: #792 CLOSED; #793 CLOSED (met). Contract frozen 2026-10-20.
Baseline SHA: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`. Current tracked edits
are an uncommitted candidate; preserve untracked `.codex-remote-attachments/`.
The fresh clean-build GUI audit initially recorded one English/125%
promotion-test failure; after adding failure diagnostics, focused and full-matrix
runs passed. The candidate remains uncommitted. Do not release until every
frozen M2 criterion, Phase 6, and exact-SHA CI pass.

## Frozen acceptance

| Gate | Required evidence | State |
|---|---|---|
| M2.1 (#408/#414) | Physical text/date/numeric edit, sort/filter/group/projection, applicable ribbon/menu/toolbar/popup routes, stable selection/table/Gantt; #750 Sort Order and #761 permanent renumber confirmation/Undo/persistence | Scoped behavior evidence recorded; #750/#761 closed; global #414 metric and exact-candidate release/full GUI gates remain open |
| M2.2 (#403/#409/#410/#411/#453/#769) | Scheduling dates, FS/SS/FF/SF/lags, intended mutations, diagnostics, Undo/Redo, MPO reopen, failure atomicity | Partial; atomicity and dependency routes below; remaining families open |
| M2.3 (#453/#769/#772) | Resource/assignment/progress/baseline scope, provenance/defaults, totals, actuals/splits, outcomes, Undo/Redo and MPO reopen | Open; #772 closed |
| M2.4 (#453/#765/#769/#770) | Command/view/report/print source, enablement, physical route/result, model purity, persistence, ja/en/DPI | Open |
| Common | Focused tests, physical route when changed, atomicity, unrelated-state preservation, exact-head build/fixture/test/log and release gate | Open |

Display sorting must not mutate outline order and is distinct from #761. MSP
compatibility decisions must cite Microsoft source/version scope and distinguish
document-derived behavior from empirical application evidence. Preserve GUI
failure artifacts/model state; after two failed fixes, require root-cause review
and a reproducer/diagnostic before another fix.

## Latest completed slice: deadline FieldVerifier transaction (2026-10-21)

- The new `TaskDeadlineFieldVerifierTransactionTest` verifies observer failure restores the prior deadline and successful verification posts an undoable edit; the successful case performs MPO save/reload and checks the persisted deadline.
- Focused result XML: `modules/microproject_ui/build/test-results/test/TEST-com.microproject.dialog.util.TaskDeadlineFieldVerifierTransactionTest.xml` (2 tests, 0 failures/errors/skips; written 2026-10-21 03:07 local). Current UI test result XML set totals 1,251 tests with 0 failures/errors/skips. This was run with the uncommitted candidate on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; it is not exact-candidate CI.

## Constraint-date FieldVerifier transaction slice (2026-10-21)

- Configuration maps `Field.constraintDate` to the task `constraintDate` property with `date="true"`; Task Advanced displays it as sometimes read-only. The `FieldVerifier` date path calls `Field.setValue`, which invokes `Task.setConstraintDate`.
- Added `TaskConstraintFieldVerifierTransactionTest`: observer failure verifies constraint type/date, start/finish, dirty state, mutation revision, Undo/Redo, and unrelated task state; success verifies the constraint date through Undo/Redo and MPO save/reload by task UID plus name.
- The failing reproducer found that restoring only the field value could leave the edited and an unrelated task's finish changed. For `Field.constraintDate`, `FieldVerifier` now snapshots all tasks in that project and restores each task's constraint and schedule state during rollback.
- Verification: `.\gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.util.TaskConstraintFieldVerifierTransactionTest" --console=plain` — PASS, 2 tests, 0 failures/errors/skips. `git diff --check` on owned files — PASS. Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; edits remain uncommitted, so this is not exact-candidate CI.
## Verified evidence

- #414 owned-dialog key route: `FlatUiSupportDialogBindingsGuiAcceptanceTest.parentLookAndFeelRefreshPreservesOwnedDialogActions` retains the parent Look & Feel refresh and Action-map identity assertions, then physically presses Enter/F1/Escape against an active owned dialog, checking the focused target, exact-once dispatch, and visible content/close outcomes. The first diagnostic run left its target unfocused and Enter did not fire; screenshot/overlap evidence showed no foreign-window overlap. A standalone-Robot revision passed after adding a physical activation click and focus assertions, but was superseded because it bypassed the required `GuiRobot` boundary and created Swing components outside EDT. The final shared-`GuiRobot`, EDT-only fixture passed 1/1 on Windows 11 ja-JP/100% (2560×1440): `:microproject_ui:guiTest -PguiTestSuite=full -PguiTestLocale=ja -PguiTestUiScale=1 --tests "com.microproject.util.FlatUiSupportDialogBindingsGuiAcceptanceTest.parentLookAndFeelRefreshPreservesOwnedDialogActions" --console=plain --max-workers=1 --no-daemon`. The related headless `FlatUiSupportRootPaneBindingsTest` passed 1/1; `compileGuiTestJava` and `git diff --check` passed. `FlatUiSupport` production source remains unchanged from checkpoint `40f5ee8d5`; no production change was needed. This focused pass does not complete the broad M2.1 GUI matrix.

- Integration check after the FieldVerifier production change: `gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL in 1m52s; JUnit XML aggregate 1,253 tests, 0 failures/errors/skips. Same uncommitted candidate/base HEAD; not exact-candidate CI.

- M2.1 field/sort: `TaskSortFieldBoundaryProjectionTest` passed 12/12 at baseline
  SHA (text/dates/duration/percent/cost both ways, empty/ties, filter/group,
  stable identity, unchanged model/dirty/Undo). Dirty-candidate physical
  Start/Finish sort+selection, popup Expand/Collapse, Indent/Outdent, and
  Task-ribbon Move Up/Down focused Robot journeys passed. #408 U-26 and #414
  U-32/U-33/range evidence: `docs/msp-implementation-status.md` sections
  2332 onward. Global #414 metric and release gates remain open.
- M2.2 failure atomicity: real `ScheduleService.setCompleted` with injected
  apply failure preserved task/assignment state, Undo/Redo, revision/events,
  observers and barrier. Focused/full UI suites passed at recorded SHA. MPO
  calendar malformed markers rejected; importer suite passed 123/123. See
  status log for exact historical commands/SHA.
- MPP negative-lead READ: `MpxExportTrackingTest.mpp9NegativeLeadLinkUsesEightHourWorkdayAndSurvivesMpoReload`
  passed 1/1 at baseline SHA against fixed OpenProj-derived `New Product.mpp`,
  UID 24→27 FS−15d, schedule/calendar and MPO reopen. Source:
  https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task
  (document-derived); provenance `docs/audit/projectlibre-delta-items.csv`.
- M2.2/#403 candidate GUI: physical Ribbon Link/Unlink and Ctrl+F2 /
  Ctrl+Shift+F2, Gantt-edge DependencyDialog Type (FS→SS→FF→SF), and positive
  1 workday lag. The same physical edge route now enters `-1d` in the dialog
  after setting FS separately, asserting the calendar-adjusted successor
  start (bounded by project start), unchanged unrelated task dates/adjacency,
  retained edge and exact endpoints, Undo/Redo, and MPO lag/type/date reload.
  Negative lag is the document-derived lead/overlap rule from Microsoft's
  [lead/lag guidance](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task),
  not an observed MSP run. Root causes found during
  test work were assertion of encoded Duration raw long as zero, `(FF)` labels
  although UI shows `FF`, stale edge coordinates after schedule changes, and
  using endpoint stub instead of long connector; no product-code change.
  Microsoft scope: Project Online Desktop Client and Project Standard/
  Professional 2016–2024; document-derived, not observed MSP:
  https://support.microsoft.com/en-us/accessibility/project/keyboard-shortcuts-for-project
  https://support.microsoft.com/en-us/project/edit-a-project-in-project-desktop
  https://support.microsoft.com/en-gb/project/change-a-task-link
  https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
  Final focused command passed 1/1, 0 failures/errors/skips:
  `\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency -Locale ja -Scale 1.0`.
  JUnit `2026-10-20T09:51:06.958Z`; artifact
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency-ja-1-0-68e3226e211f4839b639295536a1dae7`.
  Windows 11 Pro x64, ja-JP, 100%, 2560×1440; dirty candidate based on SHA
  above. `git diff --check` passed.
  The extended negative-lag journey passed 1/1, 0 failures/errors/skips on
  2026-10-21 at candidate SHA `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`;
  artifact: `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency-ja-1-0-634fe4dcc05b49e3a54925db77dd2698`.
- M2.2/#409/#410/#411 cycle rejection: focused Robot journey passed 1/1,
  0 failures/errors/skips. Physically built A→B→C; descending Name projection
  placed C before A because Link uses visible row order; physically selected
  C+A and clicked Ribbon Link. Result was `REJECTED/invalid-association` with
  selected IDs `[C,A]`. Verified unchanged dependency adjacency, exact Gantt
  edges, all four task schedules, Undo/Redo availability and names, and MPO
  save/reload retaining only A→B and B→C. The cycle guard is a local safety
  invariant, not an MSP parity claim. The GUI log recorded the localized
  circular warning; this run did not verify the popup because the acceptance
  environment suppresses application alerts. Microsoft route scope:
  Project Online Desktop Client and Project Standard/Professional 2016–2024:
  https://support.microsoft.com/en-us/project/edit-a-project-in-project-desktop.
  Command: `\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.ribbonLinkRejectsCycleWithoutChangingExistingDependenciesOrSchedules -Locale ja -Scale 1.0`.
  JUnit `2026-10-20T10:09:05.481Z`; Windows 11 x64, ja-JP, 100%, 2560×1440.
  Artifact:
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.ribbonLinkRejectsCycleWithoutChangingExistingDependenciesOrSchedules-ja-1-0-3755e83aac9f40f6a81aa5df2f67462c/`.
- Cycle warning popup: later focused Robot run enabled the desktop presenter,
  physically displayed the Japanese circular-dependency warning, captured
  `ribbon-link-cycle-warning.png`, and physically dismissed it. The rejection
  invariants above remained green. Artifact:
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.ribbonLinkRejectsCycleWithoutChangingExistingDependenciesOrSchedules-ja-1-0-02e9192bbf2042f3a3b1a01d7d19f470/`.
- Offline linked-child refresh: fixed a null outline-node notification in
  `ProjectMergeService`; focused core test verifies schedule refresh still fires
  when the changed task has no current-outline node. `:microproject_core:test`
  passed. The physical Save refresh then passed 1/1, persisted the dirty child,
  and reopened its MPO. The Who Does What route also passed 1/1 after its purity
  assertion was corrected to compare real task IDs rather than 50 reserved
  trailing void rows; its CSV and print output remained verified. The English
  alternative-calendar journey now follows the configured UI language and its
  English calendar-format assertions pass.
- M2.1/#414 U-32 wheel routes: added physical plain/Shift/Ctrl+wheel coverage
  to the existing Gantt GUI fixture. From both Gantt and spreadsheet keyboard
  focus, the wheel is physically over the Gantt grid; plain wheel advances the
  synchronized vertical viewport once, Shift advances only Gantt horizontal
  position once, and Ctrl changes zoom once. The non-target viewport, table
  position, and task dates remain stable. `:microproject_ui:guiTestClasses`
  passed; focused `GanttBarDateDragGuiAcceptanceTest.robotWheelModifiersHaveOneOwnerFromGanttAndTaskTable`
  passed 1/1, 0 failures/errors/skips (JUnit `2026-10-20T14:40:55.144Z`,
  Windows 11, 2560×1440, existing resolution). See
  `modules/microproject_ui/build/reports/tests/guiTest/index.html`.
- M2.1 U-34/#866 unsupported-IME outcome: the existing physical Convert
  journey now starts with existing Japanese text `とうきょう`, selects it in
  the cell editor, sends VK_CONVERT, and verifies the unavailable status plus
  unchanged editor selection, committed task name, and rendered cell. Focused
  ja/100% Robot passed 1/1, 0 failures/errors/skips (JUnit
  `2026-10-20T14:45:46.320Z`); artifact
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.CanonicalProjectWindowGuiAcceptanceTest.robotConvertShowsUnsupportedImeStatusWithoutChangingEditedTextOrSelection-ja-1-0-8b75e6d51d0e499a9d6f886c268d6b0f/`.
  This is the accepted deterministic Windows/JDK 25 outcome. `TEST_PLAN.md`
  U-34 explicitly says this unsupported result satisfies U-26's reconversion
  criterion on this platform; a native bridge is not a remaining #794 gate.
- M2.2 linked-child replacement failure atomicity: `ProjectFactory` now restores
  the pre-call projection if replacement fails after the old child is detached
  and the replacement is projected, preserving the original exception and
  suppressing any rollback exception. The injected-failure core regression
  compares the exact existing topology (task node under the master placeholder),
  child ownership and registrations, resource-pool membership, reference status,
  revisions, Undo availability and observed notifications. Focused test passed
  1/1; `:microproject_core:test --console=plain --no-daemon --max-workers=1`
  passed 1,110 tests across 240 XML suites, zero failure/error/skip. JUnit:
  `modules/microproject_core/build/test-results/test/TEST-com.microproject.pm.task.ProjectFactorySubprojectReplacementTest.xml`.
- M2.2/#403 replacement success reconciliation: the same factory-level fixture
  verifies replacement task projection, old/new child and master ownership,
  portfolio/resource-pool membership, OPEN reference state, observer
  notifications, and revisions for all three projects. It exposed missing
  success revisions; `replaceOpenSubproject` now advances revisions only after
  reconciliation commits. The paired injected-failure case still confirms no
  revision or notification change. `:microproject_core:test --tests
  "com.microproject.pm.task.ProjectFactorySubprojectReplacementTest"
  --console=plain` passed 2/2; full `:microproject_core:test` also passed
  (same test-result XML). Undo is not part of this loader operation; MPO
  persistence is covered by the exchange round-trip below.
- M2.2 replacement MPO reopen: `MpoFileImporterTest.masterMpoPersistsLinkedChildReplacementFromUpdatedChildFile`
  updates the linked child MPO on disk, loads a replacement project from it,
  replaces the open projection, saves/reloads the master, then checks the stable
  reference ID, embedded/extracted child bytes, replacement task identity, and
  absence of stale child tasks. This mirrors discard/reload semantics; it does
  not claim that unsaved in-memory child changes are written by master save.
  Focused test passed 1/1 and `:microproject_exchange:test --console=plain
  --no-daemon --max-workers=1` passed 308 tests, 0 failures (JUnit
  `modules/microproject_exchange/build/test-results/test/TEST-com.microproject.exchange.MpoFileImporterTest.xml`).
- Earlier Phase 2 GUI exit gates: `:microproject_ui:test --console=plain
  --no-daemon --max-workers=1` passed. `run-gui-acceptance.ps1 -Suite full
  -Locale ja -Scale 1.0` passed 237 tests, 0 failures, 0 errors, 1 expected
  screen-width assumption skip. Visual legs ja/en at 100/125/150 each passed
  113 tests, 0 failures/errors, 1 same skip. The skipped direct-command sweep
  requires a full-width desktop; high-DPI layout has the dedicated visual
  matrix. Artifacts are under `guiTest-artifacts/functional-full-ja-100-
  12838ecba07d4edcb3d641035352365b/` and `guiTest-artifacts/visual-{ja,en}-
  {1.25,1.5,1.0}-*/`. Environment: Windows 11 x64, ja-JP test host,
  2560×1440, candidate based on baseline SHA above.

## Remaining release gates

Remaining: M2.1 global #414 net-reduction evidence; M2.3 and M2.4 acceptance;
Common and exact-candidate integration/release gates. Explicit dependencies
#792 and #793 are closed, and GitHub reports no registered required subissues;
references to related epics do not imply their full closure. Next slice:
reconcile one frozen M2.3 acceptance row against current tests and physical
evidence. After every frozen row passes, review/integrate the candidate, rerun
the full GUI suite from fresh `installDist` at the exact integrated SHA,
complete applicable packaging checks, and obtain candidate-SHA CI. Preserve
all untracked files.

## Phase 6 fresh-install evidence (2026-10-20)

- `.\gradlew.bat clean build installDist --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL, 65 tasks, 4m15s.
- Initial full matrix: functional 237, ja125/ja150/en100/en150 each 113, all clean; en125 had 1 failed promotion test. Initial artifact `modules/microproject_ui/build/reports/guiTest-artifacts/visual-en-1.25-e93a3686006d461eb6d74c1e0f56628a/`. Desktop snapshot listed a full-screen `ClickToDo` candidate while coordinator reported no overlap; this did not establish causality.
- Focused reproduction of `DefaultFrameManagerGuiAcceptanceTest.promotedPrimaryWindowFocusTracksTheCurrentDocumentAfterOriginalPrimaryCloses` passed 1/1. Added timeout diagnostics for each existing predicate and frame state, preserving all assertions and the physical route; no production change. Focused rerun passed 1/1.
- Fresh full-matrix rerun after diagnostic change: functional 237/0 failures/0 errors/1 expected width skip; ja100/ja125/ja150 and en100/en125/en150 each 113/0 failures/0 errors/1 same width skip. The previously failing en125 test passed. Artifacts: `functional-full-ja-100-646cc0f4e09340f5ba0230e3213246bc`, `visual-ja-1.25-6edddb1abcd64418807845339847ef16`, `visual-ja-1.5-41a566d97a804c458be72c08e7bd7b23`, `visual-en-1.0-75556dfe8db8422cb26a62bd0f1a3962`, `visual-en-1.25-70d6af24320142a99036e1adc0f3b93e`, `visual-en-1.5-cd4c3dbead8943d4bdf329c6348eb02e` under `modules/microproject_ui/build/reports/guiTest-artifacts/`. Matrix JUnit aggregates all have zero failures/errors. Expected skip: full-width direct command sweep is paired with dedicated high-DPI layout check. All six final legs had zero contention markers and zero foreground-overlap artifacts.
- Captured file-access and cycle-warning dialogs are intentional test cases; assertions verify their message, dismissal, and preservation outcomes. They are not the GUI suite failure.

## Next finite slice

Reproduce deadline field failure atomicity through Task Information's Advanced
tab. Trace `FieldVerifier` → `Field.setValue` → `Task.setDeadline` → event/Undo
publication. First test an observer exception and capture deadline, dirty/revision,
Undo/Redo, and observer state. Implement only if the focused reproducer proves
partial mutation; then verify Undo/Redo and MPO persistence. This is an M2.2
scheduling/diagnostic writer; no physical screenshot is required for the initial
model-state probe.

## Shared resource-pool transaction slice (2026-10-21)

- Added a coordinated-operation checkpoint for deferred publications and pool
  revisions, restorable resource-pool membership/revision state, project pool
  metadata snapshots, and assignment/task detail restoration for failed share
  operations. Injected-failure tests for both POOL_TAKES_PRECEDENCE and
  SHARER_TAKES_PRECEDENCE verify assignment identity/work/baseline, reverse
  links, membership, both project references/revisions, pool revisions, both
  Undo states, and observer silence. Project event/Undo scopes are nested
  across both projects; assignment/task details are restored once per task.
  Existing success coverage verifies single-revision publication and observer
  timing.
- Focused `:microproject_core:test --tests
  "com.microproject.pm.resource.SharedResourcePoolServiceTest" --console=plain
  --no-daemon --max-workers=1` passed; full `:microproject_core:test
  --console=plain --no-daemon --max-workers=1` passed (1114 tests, 0 failures,
  0 errors). Tested HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`.

## Task-mode failure atomicity slice (2026-10-21)

- Extended `TaskModeServiceTest.compensatesEarlierTasksWhenLaterModeApplicationFails`
  to inject a one-time scheduling failure in the second project and assert exact
  task modes/manual dates/schedule dates/dirty flags, both project revisions,
  shared Undo/Redo availability and names, zero object/schedule notifications,
  and identity-preserving exception propagation. The test exposed leaked
  schedule notifications; `TaskModeService` now batches events and revisions
  across affected projects, discarding batches and edits on rollback while
  preserving the original exception.
- Focused command `.\gradlew.bat :microproject_core:test --tests
  "com.microproject.pm.task.TaskModeServiceTest" --console=plain --no-daemon
  --max-workers=1` passed 2/2 tests. Full command `.\gradlew.bat
  :microproject_core:test --console=plain --no-daemon --max-workers=1` passed
  1,117 tests across 240 XML suites (0 failures/errors/skips). `git diff --check`
  passed. Next gap: atomicity
  evidence for the remaining scheduling/dependency mutation families and exact
  integrated-head/CI gates remain open.

## Linked replacement external-dependency rollback slice (2026-10-21)

- Added a failure seam after `Portfolio.handleExternalTasks(replacement, true, false)` and before shared-pool resolution. The regression uses a real external task placeholder and dependency in a separate open project; it verifies the same dependency object, predecessor/successor identities and association-list order, placeholder registration and project membership, old/replacement projection and portfolio/pool membership, all four project revisions, Undo state, observer silence, and identity-preserving propagation of the injected exception.
- Failure exposed that dependency rebinding and external-placeholder removal survived the enclosing replacement rollback. `DependencyService` now returns an inverse snapshot action for successful external resolution; `ExternalTaskManager` retains it for the enclosing portfolio operation, which restores external registrations and defers affected-project revisions until replacement success. The injected failure rolls back those actions and pending coordinated publications/revisions.
- Follow-up review found successful resolutions would retain inverse snapshots indefinitely. Resolution transactions now explicitly begin/commit/rollback around portfolio replacement; commit discards snapshots, rollback restores them, and ordinary open resolution stores no inverse snapshots. Success and ordinary-open regressions verify both paths.
- Focused `:microproject_core:test --tests "com.microproject.pm.task.ProjectFactorySubprojectReplacementTest" --console=plain --no-daemon --max-workers=1` passed 5/5. Full `:microproject_core:test --console=plain --no-daemon --max-workers=1` passed 1116 tests, 0 failures, 0 errors, 0 skipped. Tested HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working tree changes included).
- This integration gap was closed by the following Replacement shared-pool rollback slice, which injects failure after resolution and verifies restoration of replacement membership, pool-reference metadata/assignments, revisions, notifications, and Undo state.

## Replacement shared-pool rollback slice (2026-10-21)

- `Portfolio.replaceProject` now owns a shared-pool resolution scope, using each successful `SharedResourcePoolService` snapshot as the inverse operation. If a later replacement step fails, it restores successful pool resolutions in reverse order along with the existing external-task/project rollback.
- Added a post-resolution failure seam and regression coverage for a replacement child referencing an open saved pool with a colliding resource identity while an external dependency is rebound. The test checks child projection, dependency restoration, pool membership/resource and assignment identity, global resource ownership, persisted reference status, revisions, pending notifications, Undo state, and original exception identity.
- Focused `:microproject_core:test --tests "com.microproject.pm.task.ProjectFactorySubprojectReplacementTest" --console=plain --no-daemon --max-workers=1` passed 6/6. Full `:microproject_core:test --console=plain --no-daemon --max-workers=1` passed 1117 tests, 0 failures, 0 errors, 0 skipped. Tested HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working tree changes included).

## Task progress batch failure atomicity slice (2026-10-21)

- `TaskProgressService.markOnTrack` and `setPercentComplete` now execute the
  selected-task apply loop inside `Project.runAtomicModelCommand`. Before
  applying, the service captures project and selected-task details. If a later
  task setter fails, both snapshots are restored while the project event batch
  is still active; the atomic command discards pending object/schedule events
  and preserves the original exception.
- Added deterministic failure-after-update tests for both methods. They verify
  task and assignment progress/work/actual/remaining/baseline state, dates,
  dirty flags, project revision, Undo/Redo availability and names, zero object
  and schedule notifications, and exception identity.
- Focused `.\gradlew.bat :microproject_core:test --tests
  "com.microproject.pm.task.TaskProgressServiceTest" --console=plain
  --no-daemon --max-workers=1` passed 10 tests. Full `.\gradlew.bat
  :microproject_core:test --console=plain --no-daemon --max-workers=1` passed
  1,119 tests across 240 suites (0 failures/errors/skips). `git diff --check`
  passed. Tested HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working
  tree changes included).
- Next finite gap: failure atomicity for a multi-task baseline capture/overwrite
  workflow, including assignment baseline values, Undo/Redo, observer silence,
  and MPO reopen. Broader M2.2 mutation-family and integrated-head/CI gates
  remain open; this slice does not close #794.

## Baseline overwrite failure atomicity slice (2026-10-21)

- `Project.saveCurrentToSnapshot` now performs the selected-task replacement
  inside the project's atomic model/event boundary. If a later task fails, it
  restores prior/absent snapshots and imported assignment baseline overrides,
  then restores task dirty flags while preserving the original exception.
  `DocumentFrame.applyBaseline` remains the UI caller and posts the returned
  reversible change to the existing Undo support on success.
- Added deterministic failure-after-replacement coverage using a `NormalTask`
  override. It verifies snapshot slots, assignment baseline values/imported
  overrides/cost, dirty flags, revision, Undo/Redo availability and names, zero
  object/schedule notifications, and exception identity.
- Focused `ProjectScheduleBehaviorTest` passed. Full
  `:microproject_core:test --console=plain --no-daemon --max-workers=1` passed
  1,120 tests (0 failures/errors/skips); `git diff --check` passed. Tested HEAD:
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working tree changes included).
- Next finite gap: baseline failure rollback does not yet have MPO reopen
  coverage; broader M2.2 mutation-family and integrated-head/CI gates remain
  open, and this slice does not close #794.

## Baseline rollback MPO persistence slice (2026-10-21)

- Added `MpoFileImporterTest.failedBaselineOverwriteRollbackSurvivesMpoRoundTrip`.
  It injects the same later-task failure, saves the rolled-back project, reloads
  the MPO, and checks the original task baseline slot/start/cost, imported
  assignment baseline values/cost, and the previously absent task baseline.
  This fixture has no collaboration session or operation log to persist.
- Focused
  `:microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.failedBaselineOverwriteRollbackSurvivesMpoRoundTrip" --console=plain --no-daemon --max-workers=1`
  passed 1/1. Full `:microproject_exchange:test --console=plain --no-daemon
  --max-workers=1` passed 309 tests (0 failures/errors/skips). `git diff --check`
  passed. Tested HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working
  tree changes included).
- MPO reopen now covers this baseline failure rollback. Broader M2.2
  mutation-family and integrated-head/CI gates remain open; this slice does not
  close #794.

## Respect-links constraint-conflict rollback slice (2026-10-21)

- Extended `RespectLinksServiceTest.constraintConflictRollsBackDatesModeAndDirtyState`
  to assert assignment dates/work/cost, an unrelated task's dates and dirty
  state, project dirty/revision, Undo/Redo availability, and zero schedule/object
  notifications after an MSO-vs-FS constraint conflict. The existing rollback
  restored all state; no production change was needed.
- Focused `:microproject_core:test --tests
  "com.microproject.pm.task.RespectLinksServiceTest" --console=plain` passed
  12 tests. Full `:microproject_core:test --console=plain` passed 1,120 tests
  (0 failures/errors/skips). Focused `git diff --check` passed. Tested against
  baseline SHA `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current working
  candidate.
- Broader M2.2 mutation-family and integrated-head/CI gates remain open; this
  slice does not close #794.

## Respect-links success MPO round-trip slice (2026-10-21)

- Added `MpoFileImporterTest.successfulRespectLinksManualFsWorkingLagSurvivesMpoRoundTrip`.
  A literal-date 8-hour manual FS predecessor and one-workday lag move the
  successor; the test saves/reloads MPO and verifies successor dates, manual
  schedule mode, dependency type/lag, and restored endpoint identities.
- Focused exchange test passed 1/1. Full `:microproject_exchange:test
  --console=plain` passed 310 tests (0 failures/errors/skips). Focused
  `git diff --check` passed. Tested against baseline SHA
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current working candidate.
- Broader M2.2 acceptance and integrated-head/CI gates remain open; this slice
  does not close #794.

## Leveling split default checkbox Robot slice (2026-10-21)

- Extended the existing physical assignment-split Leveling journey to assert
  `Allow task splitting` is visible and selected before Robot interaction.
  The UI and layout are unchanged; the absence was an assertion gap only.
- `:microproject_ui:compileGuiTestJava --console=plain` passed. Focused
  `run-gui-acceptance.ps1 -TestFilter
  com.microproject.dialog.ResourceLevelingDialogGuiAcceptanceTest.physicalAssignmentSplitPreservesTaskAndPersistsItsContour
  -Locale ja -Scale 1.0` passed 1/1 (0 failures/errors/skips). Artifact:
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.dialog.ResourceLevelingDialogGuiAcceptanceTest.physicalAssignmentSplitPreservesTaskAndPersistsItsContour-ja-1-0-289f944db7aa48f6ad529fa46752512c`.
- This assertion-only slice needs no per-fix DPI screenshot; the final
  integrated candidate still requires the full Phase 6 ja/en × 100/125/150
  matrix.

## Gantt interval failure atomicity slice (2026-10-21)

- `ScheduleService.setInterval` now wraps `NormalTask` and `Assignment` bar
  changes in the owning project's atomic model command. A failure after interval
  mutation restores the target's detail and dirty state; assignment moves also
  restore the affected task detail. Failed edits discard revisions, Undo
  entries, and object/schedule notifications while rethrowing the original
  exception. Other `Schedule` implementations are not covered by this slice.
- `ScheduleServiceSplitTest` has failure-after-mutation tests for both task and
  assignment bars. Focused class passed 14 tests. Full
  `:microproject_core:test --console=plain` passed 1,123 tests, 0
  failures/errors/skips. Focused `git diff --check` passed. Tested against
  baseline SHA `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current
  candidate.
- Broader M2.2 mutation inventory and integrated-head/CI gates remain open;
  this slice does not close #794.

## Ordinary split failure atomicity slice (2026-10-21)

- `ScheduleService.split` now wraps ordinary `NormalTask` and `Assignment`
  splits in the owning project's atomic model boundary. Injected failures after
  mutation restore split provenance/contours, task and assignment dates and
  duration, dirty flags, revisions, Undo/Redo, and observer counts. Undo is
  posted only after success. Leveling-generated splits remain a separate path.
- `ScheduleServiceSplitTest` passed 16 tests; full
  `:microproject_core:test --console=plain` passed 1,125 tests (0
  failures/errors/skips). `git diff --check` passed with the existing docs
  LF/CRLF warning. Tested against baseline SHA
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current candidate.
- Leveling split and broader M2.2 mutation inventory and integrated-head/CI
  gates remain open; this slice does not close #794.

## Leveling assignment-split rollback slice (2026-10-21)

- Added a Resource Leveling Apply failure injection after an assignment split
  changes its contour. `ResourceLevelingServiceTest` verifies assignment
  leveling state/contour/duration/dates, owning task dates/duration and split
  provenance, pre-existing user task/assignment splits, dirty flags, project
  revision, Undo/Redo history, observer silence, and preservation of the
  injected cause. Production leveling code already restored this state; no
  production change was needed.
- Focused `ResourceLevelingServiceTest` passed 40 tests. Full
  `:microproject_core:test --console=plain` passed 1,126 tests (0
  failures/errors/skips). Focused `git diff --check` passed. Tested against
  baseline SHA `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current
  candidate.
- Broader M2.3 leveling acceptance, other M2.2 writers, and integrated-head/CI
  gates remain open; this slice does not close #794.

## Positive actual-work leveling lifecycle across MPO (2026-10-21)

- Added a selected, initially unstarted assignment that receives a Leveling
  split, then gets one hour of imported actual work plus positive remaining
  work. The test preserves a fixed unselected blocker and project Status Date,
  saves/reloads MPO, Clears Leveling, exercises Undo/Redo, then saves/reloads
  again. Exact actual, remaining, and total work and assignment contour are
  asserted throughout.
- The case exposed two defects: MPO-reloaded actual-work contour normalization
  caused Clear Leveling to reject a valid generated split, and clearing a
  zero-actual split left stale imported profiles that made MPO export reject
  assignment totals. The fix uses assignment-leveling sidecar v2 to record
  whether the source contour matched generated state before export. On load,
  only v2 records with that marker and exact agreement of imported
  actual+remaining, persisted generated total, assignment work, and current
  contour total may rebase the generated-contour baseline. V1 and false/missing
  markers retain strict contour matching. Clear discards stale imported
  profiles only when actual work is zero, deriving them from the restored
  contour; positive actual/remaining profiles remain intact.
- Added regressions proving v1 sidecar readability/strictness and that a
  same-total but different-shape user edit still rejects Clear. Full
  `:microproject_core:test --console=plain` passed 1,127 tests; full
  `:microproject_exchange:test --console=plain` passed 312 tests (0
  failures/errors/skips). `git diff --check` passed with only the existing docs
  LF/CRLF warning. Tested against baseline SHA
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current candidate.
- This verifies the supported route of recording positive actual work after
  leveling an unstarted task. Eligibility for leveling tasks already in
  progress was not changed. Broader M2.3 leveling defaults/provenance and
  integrated-head/CI gates remain open; this slice does not close #794.

## Assignment split provenance through MPO Clear Leveling (2026-10-21)

- Extended `MpoFileImporterTest.assignmentLevelingSplitStateSurvivesMpoSaveReloadAndClear`
  to combine a user-created assignment split with a Leveling-generated split,
  save/reload MPO, clear leveling, Undo/Redo the clear, then save/reload again.
  The regression verifies that only generated provenance is removed and the
  manual contour, duration, assignment/task dates, task scheduling mode, and
  task-owned manual split state survive.
- The new post-clear save exposed a data-loss defect: restored manual contour
  and stale imported timephased profiles disagreed, making MPO export fail with
  `Imported timephased profiles do not match assignment totals`. In
  `AssignmentDetail.clearLevelingGeneratedSplits`, the fix drops stale imported
  profiles when actual work is zero; export then derives profiles from the
  restored contour. The original failure was reproduced before the fix.
- Full `:microproject_exchange:test` passed 311 tests (0 failures/errors).
  Focused `ResourceLevelingServiceTest` passed 40 tests. After the core
  production edit, combined `:microproject_core:test
  :microproject_exchange:test --console=plain --no-daemon --max-workers=1`
  completed successfully; current XML aggregates show core 1,126 and exchange
  311 tests, 0 failures/errors/skips. `git diff --check` passed with only the
  existing docs LF/CRLF warning. Positive-actual-work imported profile behavior
  is covered in a later 2026-10-21 slice below.
- Broader M2.3 leveling acceptance, other M2.2 writers, and integrated-head/CI
  gates remain open; this slice does not close #794.

## Selected Baseline 10 scope and MPO slice (2026-10-21)

- Reused the selected Baseline 1 overwrite/Undo/Redo invariant for Baseline 10.
  It verifies preservation of Baseline 0, the other numbered slot, and
  unselected task data through overwrite, Undo, and Redo.
- Added selected Baseline 10 MPO save/reload coverage, including current
  schedule and Baseline 0/1 retention plus unselected task Baseline 10.
- Focused `ProjectScheduleBehaviorTest`: 27 passed; focused `MpoFileImporterTest`:
  131 passed. Full `:microproject_core:test :microproject_exchange:test
  --console=plain`: core 1,128 and exchange 313 passed, zero failures/errors.
  `git diff --check` passed. No production change in this slice.
- M2.3 and exact integrated-head/CI/release gates remain open; this does not
  close #794.

## Update Project selected actual-work scope slice (2026-10-21)

- Extended the existing physical Project-menu Update Project journey to three
  assigned tasks with a physical two-task selection. Apply updates the selected
  tasks to 100% through scheduled finish; the unselected task and assignment
  retain their 50% actual/remaining state. Undo/Redo and MPO save/reload check
  task and assignment actual/remaining work for all three tasks.
- Initial Robot run exposed a test-only identity mismatch: in-memory assignment
  UID `22` was assumed stable across MPO. Captured screenshot/window artifacts
  before diagnosis; the selected rows showed 100% and the unselected row 50%.
  The importer uses task UID + resource UID to resolve assignments for these
  comparisons. The helper now uses that stable pair; no production change.
- GUI compile passed. Focused
  `run-gui-acceptance.ps1 -TestFilter
  com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.updateProjectMenuRouteUsesDeferredDialogFixture
  -Locale ja -Scale 1.0` passed 1/1, zero failures/errors/skips. Artifact:
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.updateProjectMenuRouteUsesDeferredDialogFixture-ja-1-0-2b375e894b6c4351a5e4705faa02a57b`.
- Broader M2.3 progress transitions and exact integrated-head/CI/release gates
  remain open; this does not close #794.

## SF lag with SNET and calendar exception round-trip slice (2026-10-21)

- Added a fixed-date SF + 8h lag case with a Tuesday project-calendar
  exception, successor SNET, and an unrelated task. The core test verifies
  pre-link state, exact linked dates, intended-task-only changes, and link
  Undo/Redo. The MPO test verifies dates, SNET kind/date, SF type/lag, reciprocal
  endpoint identities, unrelated-task stability, and the calendar exception
  after save/reopen/recalculate.
- This is document-derived, not measured MSP behavior: Microsoft documents SF
  endpoints, working calendars, and lead/lag but not which calendar owns lag.
  The fixture retains the established predecessor-effective-calendar rule.
  Sources: [Link tasks](https://support.microsoft.com/en-us/project/link-tasks-in-a-project),
  [Project scheduling and calendars](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes),
  [lead/lag](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core and exchange tests passed. Combined
  `:microproject_core:test :microproject_exchange:test --console=plain
  --no-daemon --max-workers=1` passed core 1,121 and exchange 311 tests
  (0 failures/errors/skips). `git diff --check` passed with only the existing
  LF/CRLF warning. Tested against baseline SHA
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` with the current candidate.
- Broader M2.2 acceptance and integrated-head/CI gates remain open; this slice
  does not close #794.

## Task Advanced constraint-type FieldVerifier transaction slice (2026-10-21)

- Verified the actual Task Advanced field path: `TaskAdvancedPanel` appends
  `Field.constraintType`; configuration maps the field to `ScheduleWindow`'s
  `constraintType` property and eight static constraint options; the field
  component installs a `FieldVerifier` on the `JComboBox`.
- Added `TaskConstraintTypeFieldVerifierTransactionTest`. Its failure case
  injects an object observer exception and checks rollback of target and
  unrelated task constraint, start/finish, actual-start/finish, dirty flags,
  project revision, and Undo/Redo availability. Its success case verifies the
  SNET-to-MSO edit, retained target constraint date, target schedule scope,
  Undo/Redo, and MPO reload by unique ID plus name.
- The first run showed option-backed fields did not post a `FieldEdit`; after
  clearing fixture setup history, Undo/Redo verified this as a production gap.
  `FieldVerifier` now posts the edit after a successful option setter. Observer
  failure confirmed that scheduling can recompute other project tasks, so the
  existing `constraintDate` rollback snapshot workaround now also covers
  `constraintType` across the project task list. A non-target scheduled finish
  shifts on successful recalculation; its start, constraint and actual fields
  remain unchanged. This behavior needs the physical route/root-cause review.
- `.\gradlew.bat :microproject_ui:test --tests
  "com.microproject.dialog.util.TaskConstraintTypeFieldVerifierTransactionTest"
  --console=plain`: passed, 2/2. `.\gradlew.bat :microproject_ui:test
  --console=plain`: passed, 1,255/1,255, 0 failures/errors/skips.
  `git diff --check` on the owned production file passed. Tested candidate at
  baseline HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no commit, release,
  or exact-candidate CI. Physical GUI evidence remains pending.
- Next finite slice: Robot exercise of Task Advanced constraint-type selection,
  schedule outcome, Undo/Redo and MPO persistence; resolve whether unrelated
  task finish recomputation is expected. Broader M2.2 and release gates remain
  open.

## Task Advanced physical constraint-type route and undo fix (2026-10-21)

- The reused `TaskInformationRibbonGuiAcceptanceTest` now drives the physical
  task selection → Task ribbon → Information → Advanced route and selects ASAP
  from a deterministic SNET fixture. The explicit FS successor follows the
  target's earlier schedule; its start is normalized to the next work interval
  in its effective calendar. A disconnected task retains its start and finish.
  ASAP correctly clears the inapplicable constraint date to zero.
- Initial physical Ctrl+Z reproduced a real issue: it restored SNET but restored
  its date to project start rather than the original future SNET date. Failure
  screenshot and environment are preserved under
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.advancedConstraintTypePhysicalRouteUndoRedoAndMpoRecomputesFsSuccessor-ja-1-0-4abc0d68a64144b19905606196b1e876/`.
  `TaskInformationDialog.createAdvancedPanel()` builds a direct-object
  `FieldComponentMap` for the selected Task. For that narrow option-backed
  `Field.constraintType` path, `FieldVerifier` now posts `TaskConstraintEdit`
  with old/new type and date; all other option-backed fields keep `FieldEdit`.
- Added the exact ASAP undo/redo and MPO regression to
  `TaskConstraintTypeFieldVerifierTransactionTest`. Verification:
  `.\gradlew.bat :microproject_ui:test --tests
  "com.microproject.dialog.util.TaskConstraintTypeFieldVerifierTransactionTest"
  --console=plain --no-daemon --max-workers=1` — 3 passed;
  `.github/scripts/run-gui-acceptance.ps1 -Suite full -TestFilter
  com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.advancedConstraintTypePhysicalRouteUndoRedoAndMpoRecomputesFsSuccessor
  -Locale ja -Scale 1.0` — 1 passed at 2560x1440; and
  `.\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1`
  — 1,256 passed, 0 failures/errors/skips. Passing screenshot:
  `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.advancedConstraintTypePhysicalRouteUndoRedoAndMpoRecomputesFsSuccessor-ja-1-0-a99dc078f434495d955b2d4e177c5975/` contains before-edit and after-edit screenshots.
- `git diff --check` passed on tracked owned files; untracked focused test has no
  whitespace errors. Candidate remains uncommitted at HEAD
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; not exact-candidate CI. Next finite
  slice: physical Advanced constraint-date entry, invalid-input rejection,
  schedule scope, Undo/Redo, and MPO reload. Broader M2.2 and release gates
  remain open.

## Task Advanced physical constraint-date route and transaction fixes (2026-10-21)

- At ja-JP/100%, 2560x1440, the physical Task Information → Advanced route entered a deterministic SNET date later than the target's prior finish. The target start moved to the effective calendar's first valid work time on/after the constraint; the explicit FS successor moved, and the disconnected task stayed unchanged. The rendered field and model type/date matched; Ctrl+Z/Ctrl+Y restored exact type/date and schedules, and MPO reload preserved the edit by stable task identity.
- Invalid date text followed the physical Tab route and displayed the expected localized validation feedback. After dismissal, the committed constraint/schedules, Undo/Redo state and deadline remained unchanged; the dialog was closed through its visible button. No DPI/resolution matrix was run.
- Root causes: a blank date field converted null to the zero date on focus loss, creating an unrelated `Field.deadline` edit; a displayed constraint date could reparse to a different time, then normalize to the existing model value while still publishing a no-op edit; generic `FieldEdit.undo` also normalized an off-hours SNET date instead of restoring it exactly. `FieldVerifier` now skips the normalization-only setter/event, ignores unchanged blank date values, and uses `TaskConstraintEdit` snapshots for direct single-task constraint date/type edits. Other field edits retain generic `FieldEdit`.
- The first fixture date (+3 workdays) was not beyond the target's prior finish, so its failed finish-movement assertion was a test false positive. The fixture now uses a later deterministic date (+14 workdays) and asserts the calendar-adjusted start. Earlier GUI failure artifacts remain preserved; the passing artifact is `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.advancedConstraintDatePhysicalRouteRejectsInvalidTextAndPersistsUndoRedo-ja-1-0-e8474a5d8c3f43a8a3c193ae0ddab0ae/`.
- Compatibility is document-derived, not an empirical MSP run: Microsoft's Project desktop support guidance says SNET schedules on or after the constraint and entered 07:00 dates can schedule at the first work time (for an 08:00 calendar start). Sources: [How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes) and [Change default times for entered start or finish dates](https://support.microsoft.com/en-us/project/change-default-times-for-entered-start-or-finish-dates). The constraint field retains the entered date/time; only the scheduled task start is calendar-normalized.
- Verification on working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (not exact-candidate CI): focused `TaskConstraintFieldVerifierTransactionTest` — 4/4; `.github/scripts/run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.advancedConstraintDatePhysicalRouteRejectsInvalidTextAndPersistsUndoRedo -Locale ja -Scale 1.0` — 1/1; `./gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1` — 1,258 tests across 198 suites, 0 failures/errors/skips. GUI ran at ja-JP/100%, 2560x1440. `git diff --check` passed (only existing docs LF/CRLF warnings).
- No release or commit was made. Broader M2.2 and exact-integrated-head/CI gates remain open.

## SS negative lag with SNET across project-calendar exception (2026-10-21)

- Added one focused SS lead case with a Tuesday project-calendar exception. The predecessor's Friday SNET and 12-hour negative lag produce a Wednesday 12:00 link boundary across the exception; the successor's Monday SNET remains its lower bound, and calendar normalization schedules it Wednesday 13:00 through Thursday 13:00. The unrelated Monday task is unchanged. Core asserts exact schedules before/after link, Undo/Redo, dependency type/lag, and endpoint identity.
- The MPO round-trip case asserts the same predecessor/successor/unrelated schedules, successor SNET kind/date, SS type/negative lag, reciprocal endpoint identities, and preservation of the Tuesday nonworking exception after reload and recalculation.
- Compatibility is document-derived, not measured MSP behavior. Microsoft documents SS endpoint relationships, project/task calendars, and negative lag as lead/overlap; the precise lag-calendar ownership and exception-boundary arithmetic are inferred using the existing predecessor-effective-calendar rule. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project), [How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes), and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core test `TaskLinkAndBarScheduleTransitionsTest.startToStartTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException` passed 1/1; focused MPO test `MpoFileImporterTest.mpoRoundTripPreservesStartToStartLeadAndSnetAcrossProjectCalendarException` passed 1/1. Full `:microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed: 1,443 tests, 0 failures/errors/skips. `git diff --check` passed on owned files (existing LF/CRLF warning for issue packet). Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no exact-candidate CI, commit, or release.
- Broader M2.2 acceptance remains open. Next dependency slice selection remains with Sol against the frozen dependency mutation inventory; this calendar/lag route is complete.

## SF negative lag with successor SNET across project-calendar exception (2026-10-21)

- Tested candidate SHA: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (uncommitted shared working tree). No production code changed for this slice.
- Core fixture: project starts Monday 2030-06-03 08:00 with Tuesday 2030-06-04 marked nonworking; predecessor and successor are one working day; predecessor SNET Friday 2030-06-07 08:00; successor and unrelated task SNET Monday 08:00. Before link: predecessor Friday 08:00–17:00, successor and unrelated Monday 08:00–17:00. SF -12 working hours anchors predecessor START; backward arithmetic across the exception yields required successor FINISH Wednesday 2030-06-05 12:00. A one-day task finishing no earlier than that boundary starts Monday 13:00 and finishes Wednesday 13:00. Only successor changes. Core asserts that exact boundary, schedules, dependency type/lag, Undo/Redo, and unrelated task stability.
- MPO round trip asserts the same schedules and identities, successor SNET kind/date, SF type/negative lag, reciprocal endpoint identities, and Tuesday exception after save/reopen/recalculate.
- Compatibility is document-derived, not measured MSP behavior. Microsoft documents SF as successor finish constrained by predecessor start, negative lag as lead/overlap, and that calendars schedule tasks. It does not specify this exact lead-calendar ownership or exception arithmetic; this fixture follows the established predecessor-effective-calendar policy. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) (applies to Project desktop 2016–2024; SF relationship), [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task) (applies to Project desktop 2016–2024; negative lead), and [How Project schedules tasks: Behind the scenes](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes) (applies to Project desktop 2016–2024; calendars and scheduling).
- An initial test expectation incorrectly placed successor START on Wednesday 13:00. Captured focused core failure: expected `1906894800000` (Wednesday 13:00), actual `1906722000000` (Monday 13:00). Reviewing SF's finish-based relation and working-calendar duration showed actual was correct; the fixture now asserts Monday 13:00–Wednesday 13:00. No scheduler defect or production patch was found.
- Focused core command: `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest" --console=plain --no-daemon --max-workers=1` — 55/55 passed. Focused MPO command: `./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesStartToFinishLeadAndSnetAcrossProjectCalendarException" --console=plain --no-daemon --max-workers=1` — 1/1 passed.
- First full core+exchange run: core 1,130/1,130 passed; exchange 314/315 failed in existing `mpoRoundTripPreservesSnetAndStartToStartBoundary` at `verifySnetSsMpoRoundTrip` line 612 (expected 1906822800000, actual 1906909200000). That test passed in isolation. The new SF MPO fixture had mutated the project's shared default calendar; assigning it a scratch copy removed cross-fixture state. Full `:microproject_exchange:test` then passed (315/315), and the combined core+exchange command completed successfully on the isolated fixture.
- `git diff --check` passed. No physical GUI route is changed. Broader M2.2 acceptance and exact-integrated-head/CI gates remain open; Sol selects the next dependency slice.

## FS negative lag with successor SNET across project-calendar exception (2026-10-21)

- Tested candidate SHA: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (uncommitted shared working tree). No production code changed.
- The core fixture starts Monday 2030-06-03 08:00, excludes Tuesday 2030-06-04 with a project-calendar exception, and uses one-day tasks. The predecessor is constrained to Friday 2030-06-07 08:00–17:00; successor and unrelated task are constrained to Monday 08:00. FS -12 working hours resolves the boundary to Thursday 2030-06-06 12:00; successor SNET normalization schedules it Thursday 13:00–Friday 13:00. Only the successor changes. The test asserts exact dates, FS type/lag, Undo/Redo, and unrelated-task stability.
- MPO save/reopen/recalculate verifies those same dates, successor SNET kind/date, FS negative lag, endpoint IDs/reciprocal links, unrelated-task stability, and Tuesday's exception. Its project calendar is a scratch copy, preventing fixture leakage.
- Compatibility is a document-derived inference, not measured MSP behavior. Microsoft documents FS endpoint semantics, project/task calendars, and negative lag as lead/overlap, but not this exact calendar ownership or exception arithmetic; the established predecessor-effective-calendar rule is retained. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project), [How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes), and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core and MPO tests passed 1/1 each. Full `:microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed: core 1,131/1,131 and exchange 316/316, zero failures/errors/skips. `git diff --check` passed on both test files. No physical GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite dependency slice at this point: FF negative lag with successor SNET across a dated project-calendar exception (completed in the following section).

## FF negative lag with successor SNET across project-calendar exception (2026-10-21)

- Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production code changed. The core fixture uses a scratch-free isolated project fixture; the MPO fixture copies its project calendar before adding the dated Tuesday exception.
- Predecessor SNET is Wednesday 2030-06-05 08:00, finishing 17:00. FF -12 working hours resolves the exact required successor finish boundary to Monday 2030-06-03 12:00 while crossing Tuesday's nonworking project-calendar exception. The two-hour successor's Monday 08:00 SNET initially schedules 08:00–10:00; under the FF boundary it schedules 10:00–13:00. The 13:00 timestamp represents the two working hours 10:00–12:00 before the noon–13:00 nonworking interval and satisfies the required finish boundary. Core assertions cover the exact boundary, dependency type/lag, both schedules, intended successor-only scope, and Undo/Redo.
- MPO save/reopen/recalculate verifies endpoint identity/reciprocal links, FF type and negative lag, successor SNET type/date, exact post-reload schedules, unrelated task preservation, and the Tuesday exception. Microsoft sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project), [How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes), and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task). Endpoint semantics, calendar scheduling, and negative lag as lead/overlap are documented; exact lag-calendar ownership and exception-boundary arithmetic are document-derived inference, not measured MSP behavior.
- An initial expected-boundary assertion was corrected before success: Friday 17:00 minus 12 working hours is Thursday 12:00, not Wednesday 12:00, so the fixture was moved to Wednesday 17:00 as the predecessor finish. A later normalized-finish assertion was also corrected after the focused run showed the scheduler represents the two-hour 10:00–12:00 interval with a 13:00 finish timestamp; that behavior is explicitly asserted and explained in the test.
- Focused core: `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest.finishToFinishTwelveHourLeadRespectsSuccessorSnetAcrossDatedCalendarException" --console=plain --no-daemon --max-workers=1` — 1/1 passed. Focused MPO: `./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesFinishToFinishLeadAndSnetAcrossProjectCalendarException" --console=plain --no-daemon --max-workers=1` — 1/1 passed. Full `./gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed: core 1,132/1,132 and exchange 317/317, zero failures/errors/skips. After adding the final core SNET type/date assertion, the focused core test was rerun separately. `git diff --check` passed on the four owned files (the existing issue packet reports the LF-to-CRLF warning).
- Next finite slice: Sol selects the next unresolved M2.2 writer/dependency mutation family against the frozen acceptance inventory. No GUI route changed; broader M2.2 and exact-candidate CI gates remain open.

## Fixed-date FS +4 elapsed hours (2026-10-21)

- Tested candidate SHA: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (shared uncommitted worktree). No production change.
- Core fixture uses a predecessor ending Monday 2030-06-03 12:00, a +4 elapsed-hour FS lag, and a one-day successor. The exact successor schedule is Monday 16:00–Tuesday 16:00; an unrelated task stays Monday 08:00–17:00. Assertions cover FS and elapsed lag representation/value, schedule dates, unrelated-task scope, and dependency Undo/Redo.
- MPO save/reopen/recalculate preserves those dates, endpoint unique IDs and reciprocal links, FS type, and the elapsed 4-hour raw representation. No SNET constraint or calendar exception is involved.
- Microsoft documents FS relationships and that positive lag delays the successor, but does not define this exact elapsed-hour arithmetic in the cited support page. Treat the four-hour wall-clock interpretation as document-derived, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- The first combined-suite attempt exposed shared/default calendar sensitivity: isolated focused MPO passed, while the full exchange suite shifted the expected Tuesday finish to Wednesday (expected `1906819200000`, actual `1906905600000`, test line 101). The initial scratch copy inherited mutable defaults; the fixture now uses a fresh standard-calendar snapshot and explicit one-day task durations. No scheduler defect was found.
- Focused core and MPO tests passed 1/1 each. Final `./gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed with core 1,133/1,133 and exchange 318/318, zero failures/errors/skips. `git diff --check` passed (existing LF-to-CRLF warnings in shared docs). No physical GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: fixed-date SS +4 elapsed hours. M2.3/M2.4 and release integration remain deferred.

## Fixed-date SS +4 elapsed hours (2026-10-21)

- Tested working-tree candidate at baseline HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production code changed.
- Core fixed-date fixture uses predecessor Monday 2030-06-03 08:00–12:00, successor initially 08:00–17:00, and an unrelated task at 08:00–17:00. SS +4 elapsed hours has a Monday 12:00 dependency boundary; the standard calendar's noon break normalizes the one-day successor to Monday 13:00–Tuesday 13:00. Assertions cover SS type, elapsed lag value/encoding, exact task dates, intended task scope, and dependency Undo/Redo.
- MPO save/reopen/recalculate uses a fresh standard-calendar snapshot and explicit one-day durations. It preserves the dates, endpoint unique IDs and reciprocal links, SS type, and elapsed 4-hour lag representation/value. No SNET constraint or calendar exception is involved.
- Microsoft documents SS relationship semantics and that positive lag delays the successor, but not this exact elapsed-hour arithmetic or lunch-boundary normalization. Those details are document-derived inference, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core and MPO tests passed 1/1 each. Full `./gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed; fresh XML reports core 1,134/1,134 and exchange 319/319, with zero failures/errors/skips. `git diff --check` passed on the two tests and issue packet (Git noted the existing LF-to-CRLF conversion for the packet). No GUI or production behavior changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: Sol selects the next unresolved M2.2 writer/dependency mutation family against the frozen acceptance inventory. M2.3/M2.4 and release integration remain deferred.

## Fixed-date FF +4 elapsed hours (2026-10-21)

- Tested working-tree candidate SHA: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production change.
- Core uses an isolated fresh standard-calendar fixture. Predecessor runs Monday 2030-06-03 08:00–12:00; +4 elapsed hours yields an exact 16:00 FF finish boundary. The explicit two-hour successor shifts from its pre-link schedule to Monday 14:00–16:00; an unrelated one-day task remains Monday 08:00–17:00. Assertions cover FF type, elapsed lag representation/value, endpoint dates, unrelated-task scope, and Undo/Redo.
- MPO save/reopen/recalculate uses a fresh standard-calendar snapshot and explicit durations. It preserves endpoint unique IDs and reciprocal links, FF type, elapsed 4-hour encoding/value, post-reload dates, and unrelated task dates. No SNET constraint or calendar exception is involved.
- Microsoft documents FF finish-to-finish semantics and positive lag delaying the successor, but the exact elapsed-hour arithmetic is not stated in those support pages. The 16:00 boundary is a document-derived compatibility decision, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- The first focused core attempt revealed a test-fixture mismatch: the successor's duration remained one workday while its manual interval was four hours, so it could not represent the intended finish boundary. Making its two-hour duration explicit made the fixed-date scenario pass; no production defect was reproduced. The first MPO compile also used a nonexistent `CalendarOption.getMillisPerHour()` helper; replaced with one quarter of the configured standard workday.
- Focused core and MPO tests passed 1/1 each. `./gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed; fresh XML reports total core 1,135/1,135 and exchange 320/320 with zero failures/errors/skips. `git diff --check` passed on the two test files and issue packet/evidence record (existing LF-to-CRLF warnings on shared docs). No GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: Sol selects the next unresolved M2.2 writer/dependency mutation family against the frozen acceptance inventory. M2.3/M2.4 and release integration remain deferred.

## Fixed-date SF +4 elapsed hours (2026-10-21)

- Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production code changed. The core fixture uses a fresh standard-calendar snapshot. Predecessor runs Monday 2030-06-03 08:00–12:00; SF +4 elapsed hours creates a document-derived successor finish boundary at Monday 12:00. The explicit two-hour successor schedules 10:00–13:00 around the standard calendar's noon break; the unrelated one-day task remains 08:00–17:00. Core checks SF type, elapsed lag representation/value, exact dates, successor-only scope, and dependency Undo/Redo.
- MPO save/reopen/recalculate uses a fresh standard-calendar snapshot and explicit task durations. It preserves endpoint unique IDs and reciprocal links, SF type, elapsed 4-hour encoding/value, exact post-reload dates, and unrelated-task dates. No SNET constraint or calendar exception is involved.
- Microsoft documents SF endpoint semantics and positive lag delaying the successor, but not this exact elapsed-hour arithmetic or lunch-boundary normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core and MPO tests passed 1/1 each. A combined-suite retry initially overlapped an earlier still-running invocation and failed with `NoSuchFileException` for `modules/microproject_exchange/build/test-results/test/binary/in-progress-results-generic.bin`; this run is invalid as test evidence due to concurrent Gradle result-file writes. After confirming no test executor/wrapper processes remained, the final combined `:microproject_core:test :microproject_exchange:test` run executed both suites and completed successfully (core 1,136/1,136 and exchange 321/321, zero failures/errors/skips). `git diff --check` passed on the owned test files; shared issue docs produce the existing LF-to-CRLF warnings. No physical GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: Sol selects the next unresolved M2.2 lag/constraint interaction or writer/dependency mutation family against the frozen acceptance inventory. M2.3/M2.4 and release integration remain deferred.

## Fixed-date FS +50% predecessor-duration lag (2026-10-21)

- Core fixture uses an isolated fresh standard calendar and explicit durations. The predecessor is one workday, scheduled Monday 2030-06-03 08:00–17:00; the encoded lag is `Duration.setAsPercent(Duration.setPercentAsDecimal(0L, 0.5F))`. Assertions verify the stored percentage flag/fraction, `getLeadValue()` equals half the predecessor's duration (4 working hours), and adding that lead from Monday 17:00 on the effective calendar gives Tuesday 13:00. The automatic one-day successor schedules Tuesday 13:00–Wednesday 13:00. The unrelated task remains Monday 08:00–17:00. Dependency creation is checked through Undo/Redo and recalculation.
- MPO save/reopen/recalculate uses a fresh standard-calendar snapshot and explicit durations. It verifies endpoint unique IDs, reciprocal links, FS type, percent flag/fraction, exact dates, and unrelated task preservation.
- The first exploratory fixture had a manual predecessor interval 08:00–12:00 but retained a one-workday (8h) duration. Its half-duration was correctly 4h and calendar addition from noon gave Monday 17:00, after which successor normalization chose Tuesday 08:00. That interval/duration mismatch was removed; no production defect was reproduced. The final fixture matches the scheduled 08:00–17:00 workday to its explicit duration.
- Microsoft documents FS endpoint ordering and positive lag delaying the successor, but does not state exact percentage-lag arithmetic or this fixture's calendar-boundary result. The 50%-of-predecessor-duration rule is a narrow document-derived compatibility decision, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production code changed. Focused core and MPO tests passed 1/1 each. The final combined `:microproject_core:test :microproject_exchange:test` run passed with fresh XML totals of core 1,137/1,137 and exchange 322/322 (zero failures/errors/skips). `git diff --check` passed; issue packet has the existing LF-to-CRLF warning. No GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: Sol selects the next unresolved M2.2 lag/constraint interaction or writer/dependency mutation family against the frozen acceptance inventory. M2.3/M2.4 and release integration remain deferred.

## M2.2 fixed-date FS negative elapsed lag slice

Added a fixed-date core dependency case and MPO round-trip case for FS lag −4 elapsed hours. Microsoft defines FS endpoint ordering and negative lag as lead/overlap; exact boundary clock arithmetic is a document-derived compatibility decision, not an MSP measurement. With predecessor 2030-06-03 08:00–14:00 and explicit 5 work-hour duration across lunch, subtracting four wall-clock hours yields a Monday 10:00 boundary. The automatic successor moves from Monday 08:00 to 10:00 and its explicit 3 work-hour duration ends Monday 14:00 across lunch. Predecessor and unrelated task remain unchanged. Undo restores the unlinked successor schedule; Redo reapplies the boundary. MPO reopen/recalculate checks endpoint unique IDs, reciprocal links, FS type, negative elapsed flag/value, and all dates.

Sources: https://support.microsoft.com/en-us/project/link-tasks-in-a-project and https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task.

An exploratory 4-hour successor duration produced a Monday 13:00 endpoint instead of the predicted 12:00 because of standard-calendar finish endpoint normalization. A 3-hour duration crossing lunch provides a stable explicit endpoint at 14:00. An attempted later Tuesday initial start could not be retained by automatic scheduling; the final case instead starts automatically at 08:00 and proves actual movement to the 10:00 link boundary. No production defect was reproduced and no production code changed.

Focused core/MPO tests passed 1/1 each. Final `:microproject_core:test :microproject_exchange:test` run passed; fresh XML totals: core 1,138/1,138 and exchange 323/323, zero failures/errors/skips. `git diff --check` passed. Tested at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no GUI route changed, and broader M2.2 plus exact-candidate CI gates remain open.
Next finite slice: fixed-date SS negative elapsed lag (-4 hours), reusing this slice's explicit-duration and MPO identity/link contract.

## Fixed-date FS −50% predecessor-duration lead (2026-10-21)

- The core fixture uses a fresh standard calendar and explicit one-workday durations. The predecessor is fixed Monday 2030-06-03 08:00–17:00. The lag encoding is `Duration.setAsPercent(Duration.setPercentAsDecimal(0L, -0.5F))`; `Dependency.getLeadValue()` resolves to −4 working hours from the predecessor's duration.
- Microsoft documents that a negative percentage (including −50%) is lead time and that FS lead makes the successor overlap its predecessor. Applying 50% to the predecessor duration is the documented interpretation; the exact calendar boundary and fixture timestamps are a document-derived compatibility decision, not measured MSP behavior. Sources: [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task) and [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project).
- The exact FS required-start boundary from `Dependency.calcForwardDependencyDate` is Monday 12:00. The automatic one-day successor moves from its unlinked Monday 08:00 start to Monday 13:00 and finishes Tuesday 13:00; this overlaps the predecessor's final four working hours. The separate unrelated one-day task remains Monday 08:00–17:00. Undo removes the link and restores the successor's prior schedule; Redo and recalculation restore the link and dates.
- MPO save/reopen/recalculate preserves predecessor/successor unique IDs, reciprocal graph links, FS type, negative percent flag and −0.5 fraction, derived −4-hour lead, exact required-start boundary and scheduled dates, and the unrelated task's dates.
- The first focused boundary assertion manually called calendar `add` with the milestone/finish-boundary direction; its result was noon, but it did not express the duration-bearing predecessor semantics used by the FS dependency. The fixture was corrected to query `calcForwardDependencyDate(..., hasDuration=true)` and assert both the noon required boundary and 13:00 working-time normalization. No production defect was reproduced and no production code changed.
- Focused core and MPO tests passed 1/1 each. Full `:microproject_core:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed with fresh XML totals: core 1,139/1,139 and exchange 324/324, zero failures/errors/skips. `git diff --check` passed for owned files. Tested against working-tree baseline HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: fixed-date SS negative elapsed lag (−4 hours), with boundary arithmetic, successor normalization, Undo/Redo, and MPO reopen/recalculate identity/link checks.

## Combined verification after FS negative elapsed and FS negative percent (2026-10-21)

- Sol reran both module suites serially from the integrated shared worktree with `.\gradlew.bat :microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` after both new cases were present. BUILD SUCCESSFUL. Fresh XML: core 1,139/1,139 and exchange 324/324, zero failures/errors/skips. `git diff --check` passed; only the existing LF/CRLF notices remain.
- Tested uncommitted candidate is based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`. This is local integrated evidence, not exact-candidate CI. M2.1–M2.4, Common, fresh installDist/full GUI acceptance and release gates remain open.

## M2.2 fixed-date SS negative elapsed lag slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage for SS lag −4 elapsed hours using fresh standard-calendar snapshots and explicit durations. The predecessor runs Tuesday 2030-06-04 08:00–14:00 for 5 work hours. Subtracting four wall-clock hours gives a Tuesday 04:00 boundary; standard-calendar normalization places the 3-hour successor at Tuesday 08:00–11:00, moved later from its initial Monday 08:00 schedule. An unrelated task remains unchanged.
- Core asserts SS type, negative elapsed encoding/value, the predecessor-start-minus-four-hours boundary, normalization, predecessor/successor/unrelated dates, and link Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SS type, negative elapsed flag/value, and all dates after reopen/recalculate.
- Microsoft documents SS endpoint ordering and negative lag as lead/overlap, but does not state this exact clock arithmetic or calendar normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core `TaskLinkAndBarScheduleTransitionsTest` and exchange `MpoFileImporterTest` passed. The initial focused run caught an incorrect expected finish in the new fixture (3 work hours after 08:00 ends at 11:00 before lunch); corrected expectation passed. No production defect was reproduced. Full `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed serially. Fresh XML totals: core 1,140/1,140 and exchange 325/325, zero failures/errors/skips. `git diff --check` passed, with existing shared-document LF-to-CRLF warnings. Tested at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; this is local working-tree evidence, not exact-candidate CI.
- Next finite slice: fixed-date FF negative elapsed lag (−4 hours), with boundary arithmetic, successor normalization, Undo/Redo, and MPO reopen/recalculate identity/link checks. M2.2/M2.3/M2.4/Common and release gates remain open.

## M2.2 fixed-date FF negative elapsed lag slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage for FF lag −4 elapsed hours using a fresh standard-calendar snapshot and explicit durations. The predecessor runs Tuesday 2030-06-04 08:00–14:00 for 5 work hours. Subtracting four wall-clock hours gives a Tuesday 10:00 finish boundary; the 2-hour successor moves from its initial Monday 08:00–17:00 schedule to Tuesday 08:00–10:00. The predecessor and unrelated task retain their dates.
- Core asserts FF type, negative elapsed encoding/value, predecessor-finish-minus-four-hours boundary, successor movement and normalized dates, unrelated task dates, and link Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, FF type, negative elapsed flag/value, and all dates after reopen/recalculate.
- Microsoft documents FF endpoint ordering and negative lag as lead/overlap, but does not specify this exact clock arithmetic or calendar normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused `TaskLinkAndBarScheduleTransitionsTest` and `MpoFileImporterTest` passed. No production defect was reproduced and no production code changed. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,141/1,141 and exchange 326/326, zero failures/errors/skips. `git diff --check` passed. Tested at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; shared/unrelated working-tree changes and artifacts were preserved. No GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: fixed-date SF negative elapsed lag (−4 hours), with the same boundary, schedule, Undo/Redo, and MPO identity/link contract. M2.2/M2.3/M2.4/Common and release gates remain open.

## M2.2 fixed-date SF negative elapsed lag slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage for SF lag −4 elapsed hours using fresh standard-calendar snapshots and explicit durations. The explicit 5-hour predecessor runs Tuesday 2030-06-04 08:00–14:00; subtracting four wall-clock hours from its start gives a Tuesday 04:00 minimum-finish boundary. Standard-calendar normalization moves the explicit 3-hour successor from Monday 08:00 to Tuesday 08:00–11:00. The predecessor and unrelated task remain unchanged.
- Core asserts SF type, negative elapsed encoding/value, predecessor-start-minus-four-hours boundary, successor movement and normalized dates, unrelated task dates, and link Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SF type, negative elapsed flag/value, and all dates after reopen/recalculate.
- Microsoft documents SF endpoint ordering and negative lag as lead/overlap, but not this exact clock arithmetic or calendar normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- The first core expectation assumed a successor could be pulled to Monday for the Tue04 boundary. The actual SF semantics enforce a minimum finish, so the nonworking boundary normalizes forward to Tue08 and the successor moves later; corrected core and MPO assertions pass. No production defect was reproduced and no production code changed. Focused `TaskLinkAndBarScheduleTransitionsTest` and `MpoFileImporterTest` passed. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,142/1,142 and exchange 327/327, zero failures/errors/skips. `git diff --check` passed with existing LF/CRLF notices. No GUI route changed; M2.2/M2.3/M2.4/Common and release gates remain open.
- Next finite slice: Sol selects the next unresolved M2.2 lag/constraint or dependency mutation family from the frozen acceptance inventory.

## M2.2 fixed-date SS −50% predecessor-duration lead slice (2026-10-21)

- Added a core schedule/Undo/Redo case and MPO save/reopen/recalculate case with a fresh standard-calendar snapshot and explicit one-workday durations. The predecessor runs Tuesday 2030-06-04 08:00–17:00. SS −50% resolves to −4 working hours measured backward from predecessor start, giving a Monday 2030-06-03 12:00 required-start boundary. Standard-calendar lunch normalization schedules the one-day automatic successor at Monday 13:00–Tuesday 13:00. The unrelated task remains Monday 08:00–17:00.
- Core asserts SS type, negative percentage encoding/fraction, derived half-duration, exact predecessor-start boundary, successor movement and dates, unrelated-task preservation, and Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SS type, negative percentage flag/fraction, derived lag and required boundary, and dates after reopen/recalculate.
- Microsoft documents SS endpoint ordering and negative lag as lead time, but not this exact percentage arithmetic or timestamp. The arithmetic and exact boundary are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- No production defect was reproduced and no production code changed. Focused `TaskLinkAndBarScheduleTransitionsTest` passed 72/72 and focused `MpoFileImporterTest` passed 150/150. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,147/1,147 and exchange 332/332, zero failures/errors/skips. `git diff --check` passed with only existing LF/CRLF notices. Tested at working-tree candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; shared changes and artifacts were preserved.
- Next finite slice: fixed-date FF −50% predecessor-duration lead, with predecessor-finish boundary, successor movement/scope, Undo/Redo, and MPO identity/link preservation. M2.2/M2.3/M2.4/Common and release gates remain open.
## M2.2 fixed-date SS +50% predecessor-duration lag slice (2026-10-21)

- Added a core schedule/Undo/Redo case and an MPO save/reopen/recalculate case using fresh standard-calendar snapshots and explicit one-workday durations. The predecessor is Monday 2030-06-03 08:00–17:00. Its SS +50% lag is four working hours measured from the predecessor start, giving a Monday 13:00 required-start boundary; the automatic one-day successor moves from Monday 08:00–17:00 to Monday 13:00–Tuesday 13:00. The unrelated task remains Monday 08:00–17:00.
- Core assertions cover SS type, percentage flag and +0.5 fraction, derived half-duration, exact predecessor-start boundary, successor movement/scope, and Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SS type, percentage flag/fraction, and dates after reopen/recalculate.
- Microsoft documents SS endpoint ordering and positive lag delaying a successor, but does not specify this exact percentage arithmetic or the calendar-boundary timestamp. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- Focused core and MPO tests passed 1/1 each. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,143/1,143 and exchange 328/328, zero failures/errors/skips. `git diff --check` passed. Tested at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; no production defect was reproduced and no production code changed. Shared working-tree changes and artifacts were preserved.
- Next finite slice: fixed-date FF +50% predecessor-duration lag, with exact predecessor-finish boundary, successor movement/scope, Undo/Redo, and MPO identity/link checks.

## M2.2 fixed-date FF +50% predecessor-duration lag slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage using fresh standard-calendar snapshots and explicit one-workday durations. The predecessor is Monday 2030-06-03 08:00–17:00. FF +50% is four working hours from predecessor finish; the effective calendar yields Tuesday 2030-06-04 13:00 as the finish boundary. The automatic one-day successor moves from Monday 08:00–17:00 to Monday 13:00–Tuesday 13:00. The unrelated task remains Monday 08:00–17:00.
- Core asserts FF type, percentage flag and +0.5 fraction, derived half-duration, exact finish boundary, successor movement/scope, and Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, FF type, percentage flag/fraction, and exact dates after reopen/recalculate.
- Microsoft documents FF endpoint ordering and positive lag delaying a successor, but not exact percentage arithmetic or this calendar-boundary timestamp. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- No production defect was reproduced and no production code changed. Focused core `TaskLinkAndBarScheduleTransitionsTest` passed 69/69 and focused MPO round-trip passed 1/1. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,144/1,144 and exchange 329/329, zero failures/errors/skips. `git diff --check` passed with only existing LF/CRLF notices. Tested working-tree candidate at HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; unrelated/shared changes and attachments were preserved.
- Next finite slice: fixed-date SF +50% predecessor-duration lag, verifying its predecessor-start finish boundary, normalized successor movement/scope, Undo/Redo, and MPO identity/link preservation.

## M2.2 fixed-date SF −50% predecessor-duration lead slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage using fresh standard-calendar snapshots and explicit one-workday durations. The predecessor is Wednesday 2030-06-05 08:00–17:00. SF −50% resolves to −4 working hours from predecessor start; backward calendar arithmetic gives a Tuesday 2030-06-04 12:00 minimum-finish boundary. The automatic one-day successor moves from Monday 08:00–17:00 to Monday 13:00–Tuesday 13:00. The predecessor and unrelated Monday 08:00–17:00 task remain unchanged.
- Core asserts the negative lag value, percentage flag/fraction, SF type, derived half-duration, predecessor-start boundary, successor movement/scope, and Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SF type, negative percentage flag/fraction, lead value, and exact dates after reopen/recalculate.
- Microsoft documents SF endpoint ordering and negative lag as lead/overlap, but not this exact percentage arithmetic or backward calendar normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- The first focused core run exposed that the standard calendar computes this reverse boundary at Tuesday 12:00 (one hour earlier than an assumed Tuesday 13:00). The test was corrected to assert the calendar's explicit result and verify the successor finish remains at/after that SF boundary; no production defect was reproduced and no production code changed. Focused core/MPO tests passed 1/1 each. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,146/1,146 and exchange 331/331, zero failures/errors/skips. `git diff --check` passed with pre-existing shared-document LF/CRLF notices. Tested at working-tree candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; unrelated/shared changes and artifacts were preserved.
- Next finite slice: Sol selects the next unresolved M2.2 lag/constraint or dependency mutation family from the frozen acceptance inventory.

## M2.2 fixed-date SF +50% predecessor-duration lag slice (2026-10-21)

- Added core schedule/Undo/Redo and MPO save/reopen/recalculate coverage using fresh standard-calendar snapshots and explicit one-workday durations. The predecessor runs Tuesday 2030-06-04 08:00–17:00. SF +50% is four working hours from predecessor start; the standard calendar's lunch endpoint normalizes the boundary to Tuesday 13:00. The automatic one-day successor moves from Monday 08:00–17:00 to Monday 13:00–Tuesday 13:00. The unrelated task remains Monday 08:00–17:00.
- Core asserts SF type, percentage flag and +0.5 fraction, derived half-duration, exact predecessor-start finish boundary, successor movement/scope, and Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, SF type, percentage flag/fraction, and exact dates after reopen/recalculate.
- Microsoft documents SF endpoint ordering and positive lag delaying a successor, but does not specify this exact percentage arithmetic or calendar-boundary normalization. These are document-derived compatibility decisions, not measured MSP behavior. Sources: [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- No production defect was reproduced and no production code changed. Focused core and MPO tests passed 1/1 each. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,145/1,145 and exchange 330/330, zero failures/errors/skips. Tested at working-tree candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; shared changes and artifacts were preserved. No GUI route changed; broader M2.2 and exact-candidate CI gates remain open.
- Next finite slice: fixed-date SF −50% predecessor-duration lead, with predecessor-start boundary, successor normalization, Undo/Redo, and MPO identity/link checks. M2.2/M2.3/M2.4/Common and release gates remain open.

## M2.2 fixed-date FF −50% predecessor-duration lead slice (2026-10-21)

- Added a core schedule/Undo/Redo test and MPO save/reopen/recalculate test using fresh standard-calendar snapshots and explicit one-workday task durations. The predecessor is Wednesday 2030-06-05 08:00–17:00. Negative 50% is a four-working-hour lead from predecessor finish, giving a Wednesday 12:00 minimum-finish boundary. The automatic one-day successor moves from Monday 08:00–17:00 to Tuesday 13:00–Wednesday 13:00; its standard-calendar canonical finish is 13:00, after the noon boundary. The unrelated Monday task is unchanged.
- Core assertions cover FF type, negative percentage encoding/fraction, derived half-duration, exact predecessor-finish boundary, normalized successor movement/dates, unrelated-task preservation, and link Undo/Redo. MPO asserts endpoint unique IDs, reciprocal links, FF type, negative percentage flag/fraction and derived lead, and all dates after reopen/recalculate.
- Microsoft describes FF as a requirement that the successor not complete before the predecessor and permits negative percentage lead as an overlap. It does not specify this exact arithmetic or calendar endpoint normalization. The half-duration boundary and canonical dates are document-derived compatibility decisions, not MSP measurements. Sources: [How Project schedules tasks: Behind the scenes](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes) and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
- The first core assertion assumed the successor would move earlier. The FF relation is a minimum-finish constraint even with negative lag, so it instead moves later to satisfy Wednesday 12:00; standard-calendar normalization yields Wednesday 13:00. The MPO assertion initially used the successor-start calculation API while intending to inspect the FF finish boundary; it now derives that boundary directly from predecessor finish and lead. These were test expectation/API-selection corrections; no production defect was reproduced and no production code changed.
- Focused core and MPO tests passed 1/1 each. Full serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed; fresh XML totals: core 1,148/1,148 and exchange 333/333, zero failures/errors/skips. `git diff --check` passed with pre-existing shared-document LF/CRLF notices. Tested at working-tree candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; shared edits and artifacts were preserved.
- Next finite slice: Sol selects the next unresolved M2.2 scheduling/dependency family from the frozen acceptance inventory. M2.2/M2.3/M2.4/Common and exact-candidate CI remain open.

## M2.2 NormalTask duration failure atomicity (2026-10-21)

- Wrapped `NormalTask.setDuration(long)`'s model mutation in the project atomic command boundary. A deterministic injected exception after schedule mutation, metadata change, and transitive dependency invalidation confirms rollback of dates/duration, actual/progress state, estimated/display-unit metadata, dependent dates, all project-task calculation-state counters, dirty flags, revision, Undo/Redo, and object/schedule observers. Successful revision publication is limited to project-registered tasks; detached internal initialization tasks do not publish.
- Microsoft source consulted: https://support.microsoft.com/en-gb/project/set-the-start-date-or-finish-date-for-your-project-in-project-desktop (Project Information route and one scheduling method). Exact failure rollback/publication is a product integrity rule, not an MSP-measured behavior.
- Focused `NormalTaskDurationTest` (29) and `ProjectMutationStateTest` (11) passed, zero failures/errors/skips. Fresh serial `:microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed: BUILD SUCCESSFUL; 240 XML reports, 1,154 tests, 0 failures/errors/skips. `git diff --check` passed with shared-document LF/CRLF notices. Candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`, uncommitted. No GUI route or visual constraint changed, so no Robot/DPI evidence was applicable for this slice.
- Next slice: Sol audits the frozen M2.2 writer/dependency inventory and selects one bounded family. M2.1–M2.4/Common and exact-candidate CI/release gates remain open.
## M2.2 Task Sheet manual start failure atomicity (2026-10-21)

- Wrapped `TaskSheetScheduleWorkflow.applyStart` in the project atomic model command boundary. The route snapshots project task state and calculation-state counters, restoring both if schedule/dependent invalidation throws. The deterministic injected failure proves the task date changed before failure and asserts task/dependent dates, dirty flags, mutation revision, Undo names, and no object/schedule observer publication after rollback.
- Focused `NormalTaskDurationTest` includes success revision publication and the failure case. Command: ` .\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.NormalTaskDurationTest" --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL; test class passed, 0 failures/errors/skips. No MPO contract or GUI route changed.

## M2.2 Task Sheet manual start-date failure atomicity (2026-10-21)

- `Task.setStart(long, FieldContext)` routes Task Sheet edits through `TaskSheetScheduleWorkflow.applyStart`. The workflow now executes registered-task changes inside the project atomic command boundary and restores the project/task snapshot plus calculation-state counters after failure.
- `NormalTaskDurationTest.taskSheetStartFailureRestoresDependentScheduleAndPublicationState` injects failure after start mutation and dependency invalidation. It verifies target/dependent dates, dirty state, revision, Undo/Redo, and object/schedule observer silence. The success case verifies schedule movement and one revision.
- Focused `NormalTaskDurationTest` passed 30 tests, zero failures/errors/skips. Fresh serial `:microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed: BUILD SUCCESSFUL; 240 XML reports, 1,155 tests, zero failures/errors/skips. `git diff --check` passed with shared-document LF/CRLF notices. Candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`, uncommitted. This was a core workflow path with no changed GUI route or visual constraint; no Robot/DPI evidence applied.
- No MSP-specific compatibility behavior was inferred in this slice; rollback is a product data-integrity guarantee.
- Next: Sol selects one remaining M2.2 mutation family from the audit of the frozen inventory. M2.1–M2.4/Common and exact-candidate CI/release gates remain open.

## M2.2 Task Sheet manual finish-date failure atomicity (2026-10-21)

- Consolidated `applyStart`/`applyFinish` atomic snapshot rollback in `TaskSheetScheduleWorkflow.runAtomicScheduleEdit`. Finish mutations, including summary edits and task date/constraint/dependency invalidation, now use the same project snapshot restoration and calculation-state counter restoration path as start edits.
- Added deterministic failure injection after finish dates and dependency invalidation have changed. `NormalTaskDurationTest` verifies restored target and successor schedule, duration/estimated/display-unit metadata, calculation counters, task/project dirty state, revision, Undo/Redo availability and names, and silence from object/schedule observers. Successful finish verifies duration, moved start/end, and exactly one revision increment.
- `:microproject_core:test --tests "com.microproject.pm.task.NormalTaskDurationTest" --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL, 31 tests, zero failures/errors/skips. An intermediate run asserted `canUndo` on the successful Task Sheet field route and showed that this path does not register an Undo edit; removed that unsupported success assertion. Failure-state Undo/Redo preservation remains verified, and the existing start/finish schedule coverage remains intact.
- `git diff --check` passed. Candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`, uncommitted with shared edits preserved. No GUI route or compatibility semantics changed; no Robot/DPI or MSP evidence applies. M2.2/M2.3/M2.4/Common and exact-candidate CI/release gates remain open.

## M2.2 Task Sheet manual finish-date failure atomicity (2026-10-21)

- Consolidated project snapshot/rollback handling for Task Sheet `applyStart` and `applyFinish` into `runAtomicScheduleEdit`; both date writers now share the same project transaction boundary and restore task/project snapshots plus calculation-state counters if mutation/recalculation throws. Existing start/manual/constraint semantics were retained.
- The finish-date failure test injects an exception after finish schedule mutation and verifies task and dependent state, dirty/revision, Undo/Redo history, and observer silence. Success coverage verifies the schedule and one revision. The route does not currently create an Undo entry on success; that behavior was unchanged, and failure checks ensure history does not change.
- Focused `NormalTaskDurationTest` passed 31 tests, zero failures/errors/skips. Fresh serial `:microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1` passed: BUILD SUCCESSFUL; 240 XML reports, 1,156 tests, zero failures/errors/skips. `git diff --check` passed with LF/CRLF notices. Tested on uncommitted candidate based on HEAD `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`. No GUI route or visual constraints changed; Robot/DPI evidence is not applicable.
- Exact rollback is a product integrity guarantee; no MSP behavior was inferred.
- Next: Sol audits the frozen M2.2 inventory for the next bounded scheduling/dependency gap. M2.1–M2.4/Common and exact-candidate CI/release gates remain open.

## M2.2 Task Sheet duration transaction (2026-10-21)

- `TaskSheetScheduleWorkflow.applyDuration` now runs the complete duration/manual-date/dependency/parent-rollup route inside the existing atomic schedule-edit boundary. The workflow records before/after project task snapshots in the active undo command so Undo/Redo restore the manual dates and dependent recalculation together with the raw duration. Duration edits recalculate the project before publishing, so the successor is current within the single committed revision.
- Added `TaskSheetScheduleWorkflowTest`: ordinary unassigned FS predecessor keeps its start, changes finish, and moves its successor; Undo/Redo restore both schedules. A late injected exception after parent rollup verifies restored task/dependent/parent dates, duration/display metadata, calculation-state counters, dirty state, revision, Undo/Redo history, and observer silence. A compact WBS-parent fixture verifies manual duration updates the summary envelope while leaving child dates intact.
- Added `MpoFileImporterTest.taskSheetDurationEditSurvivesMpoSaveReloadAndRecalculate`: save/reopen/recalculate retains task unique IDs, duration, start/finish, dependency endpoint, and successor schedule.
- Verification: `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.TaskSheetScheduleWorkflowTest" --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL, 3 tests, zero failures/errors/skips. `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.taskSheetDurationEditSurvivesMpoSaveReloadAndRecalculate" --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL, 1 test, zero failures/errors/skips.
- The initial functional core run exposed that the old duration undo entry omitted the manual finish; the workflow-level snapshot edit fixed it and the focused suite passed afterward. An initial test compile also needed checked-exception declarations; no fixture/root-cause retries were needed after that. `git diff --check` passed.
- No MSP compatibility rule was introduced; this transaction is a product data-integrity guarantee. No GUI route or visual constraint changed, so Robot/DPI evidence is not applicable. Candidate remains uncommitted atop shared working-tree changes; full suites and exact-candidate CI/release gates remain open.
- Next: Sol selects the next bounded M2.2 gap from the frozen inventory.

### MPO identity follow-up

- Sol reported a full exchange-suite failure at the duration MPO test's `findByUniqueId(predecessorId)` lookup. The failure was not reproducible in two full `MpoFileImporterTest` runs; the final run passed 152/152 with zero failures/errors, and the duration case executed 15th/152. The original failed XML had already been replaced by the focused-run report before this investigation, so its actual IDs could not be read.
- Temporary diagnostics from the full class run before adjustment captured source IDs `567, 569` and reopened task-outline entries `[567:Task Sheet duration predecessor, 569:Task Sheet duration successor]`.
- Following the neighboring MPO lag round-trip tests and the helper's documented MSPDI export contract, the duration test now calls `assignPositiveUniqueIds(project)` before capturing endpoint IDs. Failure assertions report the requested IDs and reopened task outline. The focused test passed 1/1 and the full class passed 152/152 after this change. The original full-module-only failure remains unreproduced; the full `:microproject_exchange:test` suite is still required to confirm the integration gate.

### M2.2 duration workflow integration rerun

- After adding explicit positive MPO IDs and actionable reload outline diagnostics, fresh serial `:microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` had passed core but reported the new MPO case's predecessor lookup null once. Focused MPO and two full `MpoFileImporterTest` class runs passed; the original failure XML was overwritten before its requested/available IDs could be captured.
- Fresh serial rerun `:microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1` then passed: BUILD SUCCESSFUL; 35 XML reports, 334 tests, zero failures/errors/skips. Current core reports from the preceding combined run: 241 XML reports, 1,159 tests, zero failures/errors/skips. `git diff --check` passed. This confirms the current exchange module gate after the ID hardening, while preserving the earlier unexplained result as a historical test incident rather than claiming it reproduced or identifying a confirmed root cause.

## M2.2 U-26 physical Task Sheet duration / Undo-Redo gate (2026-10-21)

- Extended `U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction` with a dependent FS successor. A physical 10h → 12h duration commit changes predecessor finish and successor dates; the clean-window run reached model, rendered Task Sheet, and Gantt bar geometry checks. The project's six-hour day renders 12h as `1.5 days`.
- Physical Ctrl+Z restored predecessor/successor model dates, rendered duration/Start cells, and both Gantt bar geometries. It did **not** leave Redo available: `EditSupport.hasPendingHistory()==false`, `UndoController.canRedo()==false`, and `getRedoName()==null`. The editor was no longer active before Undo. Captured app trace: `UI_DEBUG undo start canUndo=true canRedo=false` and `UI_DEBUG undo complete canUndo=true canRedo=false selectedTasks=0`. The failure diagnostic also recorded history names `[, ModelField(id=Field.duration, node=U-26 physical input, value=10 hours, oldValue=6 days), DependencyCreation, , ]`. This is a confirmed GUI assertion failure; its root cause is **not yet confirmed**. The wrapper sequence is `DocumentFrame.doUndoRedo` → `finishAnyOperations()` → `UndoController.undo()`; duration Undo runs `ScheduleWorkflowEdit.restore()` via `ProjectTaskSnapshot.restore()` and `project.recalculate()`. The evidence does not yet identify which event, if any, truncates Redo.
- A separate focused comparator passed: `GanttBarDateDragGuiAcceptanceTest.robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel` — `:microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel" --console=plain --no-daemon --max-workers=1`, BUILD SUCCESSFUL, one test passed. This verifies physical Ctrl+Z/Ctrl+Y on the shared root-pane route for a dependency-creation edit; it does not prove the duration snapshot edit's Redo path.
- Later U-26 reruns for added EditSupport event tracing were blocked before Robot input by `GUI_ENVIRONMENT_CONTENDED`: foreground Docker Desktop overlapped the test window. These are separate environment failures, not repetitions of the clean-window product assertion. Captures: `modules/microproject_ui/build/reports/guiTest-artifacts/U26SpreadsheetInputTransactionGuiAcceptanceTest-robotTypesDurationAndPercentAsOneInputTransaction__.failure.png`, `.environment.txt`, `.windows.txt`, `.native-desktop.txt`, `.desktop-session.txt`, `.foreground-overlap...`, and `gui-test-contended-1792535940579-78108.txt` / `gui-test-contended-1792535965316-88256.txt`. Root inspected the earlier clean-window screenshot and environment captures: ja-JP/default 100%, test window foreground, no modal error dialog, and no overlapping window at that time. The later contended capture records Docker Desktop as the foreground overlap.
- No production edit was made from this bounded review. Comparison source: the passing Gantt journey uses the same physical root-pane Ctrl+Z/Ctrl+Y route; duration Undo is distinct because it restores a project snapshot and recalculates. Without a clean rerun carrying EditSupport listener diagnostics, the suspected post-Undo history mutation remains unproven. Keep the acceptance gate open until physical Ctrl+Y, model/render assertions, and MPO reload pass. No display resolution or GUI assertion was changed.
- Commands: focused U-26 ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction" --console=plain --no-daemon --max-workers=1` — clean-window assertion failure above; subsequent diagnostics reruns aborted at pre-dispatch desktop-contended check. Comparator Gantt command above — pass. `git diff --check` — pass, with existing LF/CRLF warnings in shared docs.

## M2.2 duration history replay and paste compound Undo/Redo slice (2026-10-21)

- Confirmed cause: `TaskSheetScheduleWorkflow.applyDuration` called
  `UndoController.editPosted` directly. During `ModelFieldEdit.undo`, replaying
  the task-sheet duration setter entered that workflow and inserted a new
  top-level edit while `CompoundEdit.undo` was still traversing its children.
  The new edit trimmed the active history; diagnostic `die()` tracing showed
  the paste's remaining `Field.name` edit was killed before its undo, and the
  physical U-26 route lost Redo. Routed the workflow edit through
  `EditSupport.postEdit`, which joins the active command group and honors replay
  suppression. Search found no other production callers of `editPosted` beyond
  the EditSupport listener and `DataFactoryUndoController` override.
- Extended `NodeListTransferablePasteFailureTest.multiCellValuePasteIsUndoneAsOneOperation`
  to assert that one Undo restores both pasted cells and one Redo reapplies both.
  The U-26 Robot journey now passes its existing Ctrl+Z/Ctrl+Y model, Task Sheet,
  dependent Gantt, and MPO save/reload assertions.
- Baseline: `ccc9db6ed7d5b2108593c10ef462780fbffe4edd`; working tree contained only
  the pre-existing untracked `.codex-remote-attachments/` before this slice.
  Tested the uncommitted candidate at that baseline. Focused paste test passed
  1/1; focused `TaskSheetScheduleWorkflowTest` passed 3/3; full
  `:microproject_ui:test` passed 1,258/1,258 (198 XML reports, zero
  failures/errors/skips). Physical U-26 passed 1/1 on Windows 11, ja-JP,
  default 100%/2560x1440. `git diff --check` passed.
- Commands:
  ` .\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferablePasteFailureTest.multiCellValuePasteIsUndoneAsOneOperation" --console=plain --no-daemon --max-workers=1`;
  ` .\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.TaskSheetScheduleWorkflowTest" --console=plain --no-daemon --max-workers=1`;
  ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction" --console=plain --no-daemon --max-workers=1`;
  ` .\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1`.
- Next finite slice: investigate the remaining Gantt bar-drag failure; broader
  M2.2/M2.3/M2.4/Common and integrated exact-head/release gates remain open.

## M2.2 Gantt FS bar-drag date oracle (2026-10-21)

- Baseline `77aa8e23a7eb6e1ff6ea4bb1d78400e7c5389e8b` reproduced the reported
  failure in `GanttBarDateDragGuiAcceptanceTest.robotDragMovesBarAndRecalculatesFsSuccessor`.
  The predecessor ended Friday 2026-10-23 17:00Z (`1792774800000`); the
  Standard-calendar ASAP successor began Monday 2026-10-26 08:00Z
  (`1793001600000`). The failed oracle equated successor start to the raw FS
  finish boundary. Independent calendar normalization returned Monday 08:00.
- This matches the earlier confirmed calendar-oracle finding in
  `docs/testing/regression-investigation-notes.md`. Microsoft documents that
  tasks are scheduled according to calendars and that FS is a predecessor-finish
  minimum; the Friday-to-Monday fixture expectation is a narrow
  document-derived interpretation, not an MSP measurement. Source:
  https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
- Only this FS journey uses a fixed June 2026 project start; the shared helper's
  other callers retain their existing expectations. It asserts the dragged
  predecessor ends Friday 2026-06-12 17:00Z, then checks the successor does not
  precede the FS boundary and starts Monday 2026-06-15 08:00Z. Existing
  assertion that the physical drag moves the predecessor remains in place. No
  production code changed; traced drag route
  is `GanttInteractor.applyIntervalDrag` → `ScheduleService.setInterval` →
  project dependency recalculation, with interval/constraint history grouped by
  `EditSupport`.
- The first fixed-date attempt omitted the project scheduling start constraint
  and therefore did not anchor the task timeline. The fixture now sets both the
  project start and start constraint, and the predecessor's SNET. A later
  no-movement diagnostic coincided with the user's report of accidental mouse
  input; its desktop capture found no foreign foreground window, but it is
  classified as possible desktop contention and not product evidence. A clean
  focused rerun passed 1/1 on Windows 11, ja-JP, 100% / 2560×1440.
- M2.2 composite-transition follow-up: extended the existing FS journey after its physical drag and literal Friday→Monday checks. One project `UndoController.undo()` restores predecessor and dependent successor dates while retaining the same attached FS link; one `redo()` restores both moved schedules. After repaint/EDT drain, the existing bar-bounds helper confirms both Gantt rows render their original and restored positions. MPO save/reload/recalculate resolves predecessor/successor by positive unique IDs and checks dates plus reciprocal endpoint/link type. Undo/Redo here is the model-history layer, not physical Ctrl+Z/Ctrl+Y; the Robot physical route remains the bar drag. ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed. Focused Robot passed 1/1; full Gantt class passed 10/10 after initial extension. After the render-bound assertions, GUI compilation passed again and the final focused Robot rerun passed 1/1. No production code changed; no display resolution or assertion was weakened. No further M2.2 behavior gap was identified in this frozen-row reconciliation; M2.3 leveling defaults/provenance and broader progress transitions remain open in existing evidence, and exact integrated-head/release gates remain.

### #414 Escape binder consolidation (candidate based on `699ce1ef6`)

- Hostile review 1 (design): accepted one canonical `PopupDialogSupport.bindRootPaneAction` installer, exact existing action keys and `WHEN_IN_FOCUSED_WINDOW` scope, and caller-owned Escape/Enter/F1, dispose, and option-pane result callbacks.
- Hostile review 2 (deletion): accepted the two old binding bodies removed, one canonical InputMap/ActionMap installation retained, no wrapper duplication, and callback lifecycle preserved.
- Hostile review 3 (final diff): accepted the strengthened `PopupEscapeAuditTest` requiring delegation/no direct duplicate map binding, headless exact-once callback test, and unchanged production lifecycle.
- Candidate production-file snapshot (`AbstractDialog.java`, `PopupDialogSupport.java`): 683→673 LOC, 3→3 class declarations, 6→6 textual `.add*Listener(...)` occurrences; `git diff --numstat 699ce1ef6 -- <two files>` +18/−28. Net −10 LOC for this slice only; this does not settle #414's historical net-reduction requirement.
- Focused command: ` .\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.util.PopupDialogSupportTest" --tests "com.microproject.dialog.AbstractDialogEnterKeyTest" --tests "com.microproject.dialog.PopupEscapeAuditTest" --tests "com.microproject.util.FlatUiSupportRootPaneBindingsTest" --console=plain --no-daemon --max-workers=1` — 9 tests passed. The initial run failed only because `PopupEscapeAuditTest` asserted the removed private helper/direct map writes; after updating the audit to require canonical delegation and forbid the old implementation, rerun passed.
- ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL. ` .\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1` — BUILD SUCCESSFUL; fresh XML reported 1,258 tests, 0 failures/errors/skips. Notified physical command ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.util.FlatUiSupportDialogBindingsGuiAcceptanceTest.parentLookAndFeelRefreshPreservesOwnedDialogActions" --console=plain --no-daemon --max-workers=1` — 1/1 passed; its fixture uses the canonical binder and verifies Enter/Escape/F1 exact-once outcomes and action identity after parent LAF refresh. `git diff --check` passed with only existing line-ending warnings.
- The proposed extra U-26 click→UID assertion was audited and found redundant; see the following M2.1 selection-transition audit.

### M2.1 selection-transition audit

- No additional click→UID test was added: `U26SpreadsheetInputTransactionGuiAcceptanceTest.robotFirstKeyEditsStableTaskThroughFilteredGroupedSortedProjection` already physically clicks the visible row in `typePercentFromFirstPhysicalKey`, captures selected task IDs before sending the first character, and asserts that the snapshot equals the target's stable UID. It separately checks table/Gantt projection alignment and unchanged project task order.
- Added `TaskInformationRibbonGuiAcceptanceTest.physicalIncompleteTaskFilterPreservesDomainAndSelectsVisibleTaskByStableId`: physical View tab → `RibbonChooseFilter` → Incomplete Tasks chooser → OK; completed task excluded from table and Gantt; visible row selection resolves to the target UID and aligned Gantt row; task IDs, dates, durations, progress, outline order, and project dirty state remain unchanged. Fixture gives the completed task one workday duration before setting 100%, and asserts effective percent 1.0.
- Initial physical run failed because the test created a zero-duration unassigned task. Diagnostic rerun captured pre-filter `Gantt / Filter.None`, selected combo item Incomplete Tasks, applied `Filter.IncompleteTasks`, matching cache/view transformer identity, and the completed task's effective percent/schedule percent both 0.0; compiled predicate consequently returned true. `NormalTask.getPercentComplete()` returns 0 for unassigned zero-duration tasks, so this was a fixture defect, not a UI/filter production failure. No production code changed. After anchoring nonzero duration, ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed and final notified ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalIncompleteTaskFilterPreservesDomainAndSelectsVisibleTaskByStableId" --console=plain --no-daemon --max-workers=1` passed 1/1. `git diff --check` passed.
- Classic-toolbar route reachability check: `Main.main` fixes `newLook=true` (`modules/microproject_ui/src/main/java/com/microproject/pm/graphic/gantt/Main.java:75`); `Environment.ribbonUI` defaults true and no production code calls `setRibbonUI` (`modules/microproject_core/src/main/java/com/microproject/util/Environment.java:47,223`); `GraphicManager` installs `MicroProjectShell.installRibbonShell` at `:4808`, while the classic branch is `:4824`. `ACTION_FILTER` is an action-map alias (`GraphicManager.java:1969-1970`), not a separate current user control; the ribbon maps to `ACTION_CHOOSE_FILTER`. `StandardToolBar` is absent from the default bundle and found only in legacy zh_CN/ru bundles, so a test-only classic-shell attempt failed during construction before any input. No toolbar Robot test or production route was added. Filter selection is view-only runtime state (`ViewTransformer.userFilterId`, `:63,172-174`) and is not part of the workspace/MPO contract; no filter persistence requirement applies. U-34 is not an open native-bridge requirement: `TEST_PLAN.md:204` explicitly accepts deterministic unsupported behavior on Windows/JDK 25; `EditorSelectionBehaviorTest.spreadsheetEditorAcceptsConvertKeyForReconversion` and `CanonicalProjectWindowGuiAcceptanceTest.robotConvertReportsUnsupportedImeOnBothSpreadsheetRoutesWithoutChangingText` cover the status, unchanged text/selection/model/render values, and both spreadsheet entry routes (the physical journey's exact artifact/run is recorded above). No M2.1 behavior gap is identified by this delta; global #414 historical net-reduction evidence and exact-candidate release/full GUI gates remain open.

## M2.3 acceptance reconciliation (2026-10-21)

- M2.3 is partially evidenced, not closed. Physical selected/all leveling and proposed-resource routes cover scope, selection, Undo/Redo and MPO; assignment Replace and Update Project cover totals and actual-work changes; Mark on Track covers explicit Status Date, calendar-derived progress, assignment preservation, Undo/Redo and MPO; Clear Leveling plus `assignmentLevelingSplitStateSurvivesMpoSaveReloadAndClear` cover manual/generated split provenance; Baseline 1 whole-project/selected-task physical journeys and Baseline 10 headless scope/MPO tests cover slots 0/1/10.
- Reusable physical runs are indexed in `docs/msp-implementation-status.md`: `TaskInformationRibbonGuiAcceptanceTest` class (56 tests, ja/100%, including Level Selection/All and Update Project), focused proposed-resource and manual-split journeys, assignment Replace, Mark on Track, and Baseline 1 routes. Focused core/exchange results for leveling provenance and Baseline 10 were run on candidates based on `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`. Some older Robot entries do not preserve the exact tested SHA; these results are behavior evidence, not exact integrated-head acceptance. Exact commands and artifact paths remain in their indexed entries above and in the status log.
- User decision: adopt Day as the document-derived product default despite the conflicting Microsoft Project pages (Month versus Day); do not claim measured MSP parity. `ResourceLevelingService.Options.defaults()` uses `Sensitivity.DAY`; `ResourceLevelingServiceTest.levelingSensitivityComparesWorkWithinTheSelectedCalendarPeriod` asserts it, and physical `ResourceLevelingDialogGuiAcceptanceTest.assignmentLevelingIsThePhysicalDefaultAndMovesOnlyTheAssignment` asserts the visible default. Baseline coverage uses separate all/selected UI scope at slot 1, a shared selected-overwrite model helper at slots 1 and 10, and a 0..10 MPO/MSPDI fixed-cost loop; no distinct per-slot path or index-mapping risk was identified.
- Current integration target is `fd03e2e93`. Fresh exact-head GUI/build/CI and release gates remain outstanding.

- M2.4 current delta: selected-task zoom is physically covered in Gantt, Tracking Gantt, Task Usage, and Resource Usage with view/model/Undo purity checks; Task Information, Project Details, and Who Does What report journeys compare rendered data, CSV output, and printable page content and preserve model state; native multiple-file Open is physically covered. Existing indexed Robot evidence is ja/100%; no English report/print run was found. Next finite acceptance action is to run the three existing report routes with en/100% and record localized labels/page content; do not duplicate their fixtures.
- M2.3 Day default update: user selected the document-derived Day product default. The source conflict remains recorded in `docs/msp-implementation-status.md` and the current packet links both Microsoft pages; existing core/physical default assertions already pass. No behavior code changed.
- M2.4 en/100% report slice at exact HEAD `fd03e2e937ef6ee6ea8adaad955649670f43f8ee`, Windows 11, 2560x1440. The authoritative `.github/scripts/run-gui-acceptance.ps1` passes `-Locale en -Scale 1.0` into the GUI test JVM (`user.language=en`, `user.country=US`, `sun.java2d.uiScale=1.0`). `physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute` passed 1/1; its separate artifacts are under `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute-en-1-0-b014f592d1314adba076cc3b28976541/` and `...-junit/`. Exact invocation: ` .\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter 'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute' -Locale en -Scale 1.0`.
- `physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues` was attempted with the same locale/scale but stopped before Robot input. JUnit reports `GuiDesktopSessionCoordinator$DesktopContendedException`; retained foreground-overlap evidence records Chrome overlapping the project window at `2026-10-21T13:19:45.9907122Z` and `13:19:46.2659230Z`. Artifacts are under `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues-en-1-0-2048a1797e70427588bb39b739fe17b1/` and `...-junit/`. Runner preserved the failed result and did not retry. Failure screenshot capture also failed in GDI+; classify this as desktop contention, not a report assertion/product failure. `physicalWhoDoesWhatReportShowsResourceGroupsTasksAndWorkTotalsWithoutMutatingProject` remains unrun. Resume only after desktop availability; preserve individual per-run artifacts.
- #414 metric reconciliation (fixed 11 commits already named in the packet): exact `git show --numstat <commit>` production-Java rows under `src/main/**/*.java`, with full per-commit changes and no selective hunk subtraction, are: `48216a8ca` +58/−14; `f9f1124dd` +117/−57; `2e2911725` +0/−1; `972a3d737` +12/−1; `adb33c28a` +8/−0; `c42c38c16` +1/−5; `df282b0dc` +9/−3; `038e27c21` +160/−37; `a160259e6` +41/−12; `0e515a832` +15/−2; `4da34782b` +49/−42. Across those fixed commits the aggregate is +470/−174 (net +296). The selected range is not a pure #414 change: popup-trigger commits lack #414 references, `038e27c21` combines input ownership with failed-schedule rollback, and `4da34782b` is explicitly #866 IME work. The roadmap states the #414 behavior but supplies no normative metric baseline SHA; `48216a8ca^` is a reproducible earliest selected snapshot only, not an issue-approved acceptance baseline. Consequently the historical net-reduction metric remains unproven; do not use this aggregate or cherry-picked deletions as proof.
- #414 dead registration-path removal after design review 1 and deletion review 2: removed only unused `FilterToolBarManager.addButtonsInRibbonBand(JComponent)`, with zero production/test callers. The three combo instances and live `addButtons`/`removeButtons` lifecycle remain unchanged. `FilterToolBarManager.java` whole-file count is 151→143 LOC, 1→1 classes, textual `.addActionListener(...)` sites 6→3, `.removeActionListener(...)` sites remain 3. This deletion is a bounded simplification, not proof of the global #414 reduction target. ` .\gradlew.bat :microproject_ui:compileJava :microproject_ui:test --console=plain --no-daemon --max-workers=1` passed; JUnit XML: 1,258 tests, failures 0, errors 0, skips 0. `git diff --check` passed. No Robot was run: removed method has no caller or physical route.
- Final review 3 passed the same unchanged eight-line deletion after verification; no remaining callers, API/lifecycle change, or blocker was found. Three review conditions were: keep both live toolbar lifecycle methods/three combos, remove only the dead duplicate registration body, and verify `compileJava` plus all UI module tests. No source changes followed review 3.

## Integrated boundary fix and final local build (2026-10-21)

- The original clean gate on `d8c83ffab5759cbcef7298e641c4b4db358ec435` exposed that `TaskSheetScheduleWorkflow` defined a Swing undo edit in core. The boundary check rejected the Swing references; this was not allowlisted. The reviewed fix moved project-snapshot undo-edit ownership into the existing `com.microproject.undo.ScheduleEdit`, retaining the workflow capture timing and the single `EditSupport.postEdit` path. Only `TaskSheetScheduleWorkflow.java` and `ScheduleEdit.java` are modified; no test/API result introduced Swing types into core.
- Before the clean gate, `verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures`, focused `TaskSheetScheduleWorkflowTest` (3/3), focused paste failure test (26/26), full core tests, and full UI tests passed. The existing meaningful undo/redo, rollback, and paste single-operation tests passed.
- Final approved command: ` .\gradlew.bat clean build installDist --console=plain --no-daemon --max-workers=1` — `BUILD SUCCESSFUL`, 65 actionable tasks (64 executed, 1 up-to-date), including architecture checks, all enabled module tests, and `:microproject_ui:installDist`. JUnit XML totals: application 37, bootstrap 14, contrib 2, core 1,161, exchange 334, reports 13, ribbon 5, UI 1,258; 2,824 total, zero failures/errors/skips. Full log: `%TEMP%\microProject-issue-794-preclean-d8c83ffab\clean-build-installDist-final.log`. Installed layout exists at `modules/microproject_ui/build/install/microproject_ui`. This ran with HEAD `d8c83ffab5759cbcef7298e641c4b4db358ec435` plus the two uncommitted boundary-fix source files; it is not evidence for a later exact committed SHA. Preserved pre-clean GUI and focused/core/UI result artifacts are under the same task-specific temp directory.
- Reproducible six-file #414 input-registration/editor-boundary comparison uses merge-base `a9e4ae85749d1e1f88d9dfebf1ef67ec0a78abbb` and current branch whole-file snapshots. File order: `AbstractDialog.java`, `CommonSpreadSheet.java`, `ImeTextInputSupport.java`, `SpreadSheetCellEditorAdapter.java`, `FilterToolBarManager.java`, `PopupDialogSupport.java`. LOC is 3313→3302 (−11); textual type declarations (`class|interface|enum|record`) 13→13; textual `.add*Listener(` occurrences 16→13; `.remove*Listener(` occurrences 8→8; full six-file numstat +67/−78. This is the retained input refactor scope, not selected-hunk arithmetic. Of 26 production Java paths changed from the merge-base overall, the other 20 are scheduling/workflow/domain, importer, field-verifier, or selected-task-zoom work outside #414 input registration. It satisfies the scoped current-branch metric; the fixed historical 11-commit aggregate remains mixed issue history and is not attributed to #414.
- English report/print GUI CI at `d8c83ffab`: run `39609126528` (`physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues`) and `39609132743` (`physicalWhoDoesWhatReportShowsResourceGroupsTasksAndWorkTotalsWithoutMutatingProject`), each 1 test, 0 failures/errors/skips, en/100%, fresh installDist successful. Earlier run `39608740302` failed filter validation before test execution. Local Project Details contention remains environmental evidence only.
- Remaining: full GUI CI on the final committed candidate and manual startup from the freshly generated installDist layout (the explicit #414 gate). No Robot was run after the boundary fix; the GUI suite was not claimed as physical evidence for that changed core undo ownership. Packaging is not applicable: this issue-resolution diff changes no distribution inputs, runtime dependencies/modules, icons, file associations, or release behavior; it is not a publication request.

## Installed startup and Gantt-edge smoke follow-up (2026-10-21)

- Manual installed-runtime check used exact candidate `e92ae81af0f2435793169712e8afbe0b595be7a9`. It launched `modules/microproject_ui/build/install/microproject_ui/bin/microproject_ui.bat` directly with `--fileNames` pointing at `%TEMP%\microProject-794-install-e92-20261021-230254\fixture.mpo`; `MICROPROJECT_UI_OPTS=-Duser.home=<run-root>\home` was scoped to the launched process. The copied sample SHA-256 matched its source. PowerShell Start-Process PID 71608 owned Java child PID 64676; stderr confirms jars loaded from the installDist `lib` directory. The only observed window was `microProject - CCPM Visualization Sample [Master]`, with eight task rows and Gantt bars visible. No task was edited or saved. Project and View tabs were focused, but no alternate view transition was verified. `app.stdout.log` is empty; `app.stderr.log` contains startup/import output. Native SKY screenshot was visible in the computer-use task output but no PNG export was saved; do not cite it as a filesystem artifact. Both launched process IDs exited after normal window close; sample hashes remained equal.
- Windows GUI smoke run `39612193315`, job `119263652897`, tested exact SHA `e92ae81af0f2435793169712e8afbe0b595be7a9`. JUnit XML in `%TEMP%\issue794-prsmoke-39612193315-job119263652897\test-results\guiTest\TEST-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.xml` reports 53 tests, 1 failure, 1 permitted skip. Failure: `TaskInformationRibbonGuiAcceptanceTest.linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency`, assertion at line 6065, “physical Gantt dependency edge did not open DependencyDialog.” Test environment was Windows Server 2025, Java 25.0.4.1, ja-JP, 100%, 1024×768, usable 1024×720; foreground overlap artifact reports no foreign overlap. The failure PNG is `reports\guiTest-artifacts\functional-smoke-ja-100-57e31b9fc344464f9b735aa6797e22f3\TaskInformationRibbonGuiAcceptanceTest-linkAndUnlinkSelectedTasksThroughRibbonRoundTripsDependency__.failure.png`; it shows selected task rows 5/6 with no bars/edge in the visible date range (header ends Oct 18 while run date is Oct 21). `createOutlineFixture` does not anchor dates, and the old helper selected the longest logical dependency-path segment midpoint without checking clip or display containment. Artifacts contain no physical click-coordinate or model-date snapshot, so the exact attempted point was not measured. This evidence supports an offscreen test hit-target precondition, not a production dialog defect.
- Corrective test-only change, pending review/Windows run: all five calls in the one failing journey use a shared helper that scrolls to the dependency successor via `Gantt.scrollToTask`, snapshots path geometry and checks candidate points on the EDT, filters by Gantt `getVisibleRect()` and the active `GraphicsConfiguration` bounds, and requires `gantt.getUI().getObjectAt(x,y)` to resolve the expected `GraphicDependency`. Candidate segments are visited longest-first with deterministic fractions, allowing an alternate rendered segment when a midpoint is covered by a bar. The helper emits IDs/dates/path/clip/display and chosen local/screen point, or throws with geometry diagnostics. The physical double-click and `DependencyDialog` assertion remain unchanged. ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed after this change; the focused Windows GUI case and exact-candidate full GUI suite are still pending.
- Full GUI run `39612211894` tested the same SHA and failed: 241 tests, 15 failures, 1 permitted skip. A separate read-only review is classifying the other failures. None are waived or considered out of scope; exact-candidate full GUI remains an open gate.

## Baseline ribbon overflow test precondition (2026-10-21)

- The seven baseline-related failures in full GUI run `39612211894` all resolved to direct visible-button lookup for `RibbonSaveBaseline` or `RibbonClearBaseline` in `TaskInformationRibbonGuiAcceptanceTest`. The representative failure screenshot at 1024×768 shows the Project tab's baseline group collapsed to its trigger; the visible command inventory contains neither child command. The environment artifact records no foreign foreground overlap. `menuInternal.properties` places both commands in `ProjectBaselineRibbonBand`; Save Baseline is a split command. This establishes a collapsed-ribbon visibility precondition, not a baseline command/model defect.
- The five direct/dynamic baseline lookups now call the existing `findVisibleOrOverflowButton(robot, command)` helper, which delegates to `RibbonGuiSupport.findVisibleOrExpand`. It physically opens the collapsed group and returns the actual command button. The existing physical click and dialog/model/Undo/Redo/MPO assertions remain unchanged. No product code or new input helper was added.
- ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed on working tree based on `dc9a185e00ffe7a4665cd17ea7e605965b39877f`. Focused Windows GUI verification is pending review; no new Robot/CI run was dispatched. The unrelated `.codex-remote-attachments/` directory was preserved.

## Native multi-file chooser path identity assertion (2026-10-21)

- The existing physical native chooser journey received exactly two file names but compared their raw strings to long fixture paths. On Windows GUI runners, the native dialog can return equivalent `RUNNER~1` short-path spellings. `MultipleLocalProjectOpenGuiAcceptanceTest` now uses existing `FilePathUtils.sameFileIdentity` for Alpha and Beta membership, retaining exact selection count, both expected file identities, project count/title/task, and existing-project dirty-state assertions. No production code changed.
- ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed on working tree based on `dc9a185e00ffe7a4665cd17ea7e605965b39877f`. Focused Windows GUI verification has not been dispatched; no native path failure is waived.

## Focused GUI test-oracle and zoom diagnostic batch (2026-10-21)

- This review batch is based on `dc9a185e00ffe7a4665cd17ea7e605965b39877f`. The existing physical native multi-file chooser assertion compares Alpha/Beta by `FilePathUtils.sameFileIdentity`, retaining exactly two returned files, both expected identities, both opened project title/task checks, and preservation of the preexisting dirty project. This handles equivalent Windows short-path spellings such as `RUNNER~1`; it does not accept basename-only matches.
- `RibbonExternalCommandGuiAcceptanceTest` raises only the native chooser readiness probe deadline from 4 seconds to a command-specific 30 seconds. Its bounded 2-second serialization drain, 250 ms diagnostic drain, and existing physical Escape/Cancel return/outcome assertions are unchanged. The normal `GuiAcceptanceSupport.await` remains 5 seconds after the user presses Cancel. No global timeout was changed.
- The existing four-view selected-task zoom journey now emits EDT-sampled before/after diagnostics for Gantt, Task Usage, Tracking Gantt, and Resource Usage. Each records active view, selected node type and task UID, task/assignment start/end, the resolver-equivalent selected range, expected task spans, visible date range, scale, and scroll viewport geometry. Existing visible-span awaits, model-preservation assertions, and Undo-history checks remain unchanged; no production code or additional journey was added.
- Focused evidence retained: dependency-edge Robot `physicalSelectedTasksZoomFitsBothSeparatedGanttTasksWithoutChangingModelOrUndo` diagnostics are pending a new Windows run. Full GUI run `39616214297` passed the dependency-edge method 1/1 at `dc9a185e0`. New smoke run `39616744745` on that commit reports 53 tests, 1 failure, 1 skip; the sole failure is outside this batch, `WindowShellNativeDecorationGuiAcceptanceTest.physicalCaptionDragUsesNativeWindowShell`, where right-edge drag left native bounds unchanged (`x=100,y=115,width=864,height=240`). Build run `39616744498` passed.
- Local verification on the working tree: ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed. ` .\gradlew.bat :microproject_ui:test --tests com.microproject.testsupport.NativeChooserProbeCompletionTest :microproject_ui:guiTest --tests com.microproject.pm.graphic.frames.NativeChooserSnapshotParserTest -PguiTestSuite=full -PguiTestLocale=ja "-PguiTestUiScale=1.0" --console=plain --no-daemon --max-workers=1` passed: 9 probe-completion tests and 5 native snapshot parser tests, zero failures/errors/skips. `git diff --check` passed. No Robot/CI was dispatched from this checkout for this batch.

## GUI fixture placement and Calendar close precondition (2026-10-21)

- Full GUI artifacts `39612211894` record a 1024×768 display. `TwoDocumentShortcutIsolationGuiAcceptanceTest` placed Alpha at `x=24,width=1000` and Beta at `x=104,width=920`; Beta therefore covered Alpha's task-row hit area. The fixture now gives both windows the primary's fitted bounds, activates the requested document owner before the physical row click, and asserts both usable-window bounds and that the task-cell hit point lies inside that window. Physical Ctrl+Delete/Undo/Redo and cross-document model/selection assertions remain.
- The same run records the standalone Quick Access JFrame at `x=130,y=130,width=1044,height=500` while the display is only 1024 pixels wide; its right-edge ribbon-options target was outside the visible clip. The three OfficeChrome creation paths now call existing `WindowBoundsSupport.fitWithinUsableScreen` and assert the fitted frame bounds. Existing click sites route through `WindowAcceptanceAssertions.clickComponent`, retaining real Robot input and requiring the pointer to enter the current showing target before press/release. Existing ribbon-layout, popup, preference, and screenshot assertions are unchanged. No new shared helper or product change was introduced for these fixture fixes.
- `CalendarViewDialogBox` used a fixed 1100×760 size and a plain `FlatLafDialog`; on the same 1024×768 desktop the physical close control landed near x=1086. It now fits after `setLocationRelativeTo`, matching the existing Task Information dialog pattern and covering the later recenter call in `DocumentFrame`. The existing `calendarDependencyMarkerOpensTheLinkDialogAndCanRemoveTheLink` physical close/Undo/Redo journey remains unchanged.
- Verification: ` .\gradlew.bat :microproject_ui:test --tests com.microproject.ui.shell.WindowBoundsSupportTest :microproject_ui:compileGuiTestJava --console=plain` passed; helper tests 5/5 and GUI-test source compilation succeeded. Scoped `git diff --check` passed. Focused Calendar-close, two-document, and OfficeChrome Robot reruns and exact-candidate GUI CI remain pending.
