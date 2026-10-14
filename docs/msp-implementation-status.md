# Microsoft Project compatibility implementation status

Reviewed 2026-10-13. This report records completed implementation slices and
remaining work against [the roadmap](msp-compatibility-roadmap.md).
**The complete compatibility objective is not achieved.** Gate issues remain
open; an inventory or passing narrow test does not close an entire milestone.

## Implemented changes

- Standalone scope excludes connected Microsoft server/cloud services. MPO is
  the native/main save format; MPP remains a separate external exchange gate.
- Ordinary Save and close-time Save for an imported MPP/MPX/Planner document
  open the existing Save As chooser with an MPO suggestion. Source association
  is retained until the write succeeds. Save As of a legacy POD defaults to MPO;
  explicit XML/XLSX exports remain available. Swing-fallback overwrite refusal
  preserves existing output. Legacy POD writes are still a policy discrepancy.
- Resource Sort Order and header sorting share the existing single-field,
  view-only projection sorter. Selection identity and model resource order are
  preserved; Workspace fields and POD wire contracts are unchanged. English
  and Japanese labels are provided. The Sort split button now exposes that same
  action through a keyboard-focusable menu arrow; the menu item reuses the
  button's canonical Swing Action. The Japanese 100% Windows Robot journey
  physically opened the arrow menu by mouse and Space, chose Sort Order, and
  verified the canonical dialog route and cancellation result. Empty names and
  equal sort keys remain stable, and selection is cleared before the projection
  changes then restored by resource identity to avoid JTable retaining an
  out-of-range row during sort events. Multi-key, date, filter/group, outline,
  and renumber parity remain open.
- Shared button diagnostics recognize completed modal cancellation and fresh
  matching command outcomes. Stale no-change results cannot conceal a silent
  command failure; actual failed/rejected outcomes remain visible. Robot
  coverage includes repeated cancellation, injected route failure and recovery.
  Ordered visible row identities and view sort state make view-only projection
  changes observable without serializing workspace state in diagnostics.
- Update Project validates every target before mutation and restores task and
  project state after a pre-commit failure. Linked tasks are restored as a
  batch; Undo/Redo uses the same restoration path. Snapshot schedules are cloned
  on restore so later calculation cannot corrupt the captured Undo state.
  `ModelTransactionTest` also verifies that VALIDATE, AUTHORIZE, CAPTURE and
  ordinary APPLY failures report the failing stage, leave no partial state and
  do not call commit. SCHEDULE/INVARIANT rollback and COMMIT failure have
  separate coverage; these generic tests do not establish observer/Undo
  atomicity for every mutation family.
  Project-local ObjectEvent and ScheduleEvent notifications are deferred across
  the transaction: rollback publishes none, successful commit publishes the
  changes, and a COMMIT-stage Undo-listener failure still flushes committed
  state. Core rollback/success/commit-failure tests and the Japanese physical
  Project-menu Update Project journey passed; this does not cover every
  schedule/dependency mutation family.
- Dependency transition tests cover FS/SS/FF/SF links with zero, positive and
  negative lag, and a fixed-date FS case crosses a dated nonworking exception:
  Monday 2030-06-03 08:00–17:00 predecessor, Tuesday 2030-06-04 nonworking,
  8h lag consuming Wednesday work time, and successor start Thursday
  2030-06-06 08:00. The exact date rule is document-derived from Microsoft's
  calendar and lead/lag guidance, not an empirical Project run. The full
  link/lag matrix still needs independent expected-date coverage under varied
  calendars and constraints.
  See [transaction scope](architecture/model-transaction-slice.md).
- Diagnostic trace generation tolerates an uninitialized scheduling engine or
  absent trace task. A calendar warning no longer becomes an unrelated
  NullPointerException in a freshly started importer process.
- Task baselines 0–10 transfer start, finish and encoded duration through
  XML/MSPDI. Current schedule slot 11 is preserved. Partial and milestone
  baselines and native MPO persistence are covered. Assignment baseline
  timephased work distributions use MSPDI Type 4 for slot 0 and Type 16+6 per
  later slot; literal slot 0/2, explicit-zero slot 9, missing slot 1, XML and
  MPO round trips have focused evidence. Assignment baseline cost distributions
  and elapsed-duration XML normalization also have literal XML/MPO round-trip
  coverage; this is a bounded implementation result, not a complete M3 format
  manifest.
- Calendar date-only conversion separates local MPXJ calendar dates from UTC
  model day keys. Exception, work-week and recurrence conversion uses one
  implementation instead of inconsistent offsets.
- Native MPO loading restores an already-normalized status date exactly,
  instead of repeating user-input normalization.
- The report gate now compiles, fills and exports the bundled template with
  English/Japanese content. The test checks rendered text and PDF output; it
  does not prove full report visual equivalence.
- CI inventories test/support sources, rejects missing documented fixture paths
  and statically resolvable duplicate test names, and retains unresolved names
  for review. This does not replace a semantic test audit.

## Remaining milestone work

| Gate | Current boundary and next required work |
|---|---|
| M0 / #792 | Source/test/format/integration inventories are reconciled. A complete versioned command/field manifest, independent format manifests and exact current Microsoft reference evidence remain incomplete. |
| M1 / #793 | Current Swing ribbon is retained. A dated, source-linked **document-derived** comparison packet for Microsoft 365 Project desktop and Project Standard/Professional 2024 is in [the ribbon audit](RIBBON_MSP_PARITY_AUDIT_JA.md); Microsoft Project was not observed, and the packet does not claim pixel parity. Stable run [38495421214](https://github.com/tetsuji16/ProjectLibre/actions/runs/38495421214) passed its Windows full GUI, packaged-content/launch, Linux, and macOS package jobs for production candidate `702146871dbe9059412b83ab7780936e01f2e7f2`; the functional ja/100% gate plus five visual legs completed all en/ja × 100/125/150% combinations with zero failures and the expected scale-exclusive skips. Runner event counts and the public [v0.0.24.1244 GUI evidence archive](https://github.com/tetsuji16/ProjectLibre/releases/download/v0.0.24.1244/microProject-gui-evidence-v0.0.24.1244.zip) are detailed in the audit. The 36 archived images (six states × six locale/scale legs) were visually reviewed on 2026-10-14; tab labels and visible controls showed no observed overlap, but the narrow ribbon group row crops the rightmost caption at the viewport edge in ja/125% and ja/150%. This is a horizontal viewport edge; the archive alone does not prove the caption is readable after scrolling. Focused physical test [#531](https://github.com/tetsuji16/ProjectLibre/issues/531) passed and asserts Tab/Space/Down/Enter traversal plus EDT AccessibleContext names and canonical selected `JToggleButton` `CHECKED` state. A focused Robot assertion for next-arrow scrolling, complete caption/proxy bounds and the last-group popup is pending; production `ModernRibbonPanel` is unchanged from the archived source. The archive has no numeric bounds sidecar and does not claim measured window/component bounds. The curated archive also does not yet include contextual-tab captures. **#793 remains PARTIAL** pending these narrow-route and contextual-capture evidence rows plus the source-linked comparison matrix; neither Microsoft client was observed, and no empirical MSP capture or pixel parity is claimed. |
| M2 / #794 | The Resource Sort menu/header physical route and one Update Project transaction have evidence. Remaining issue rows are finite in the roadmap table below: M2.1 sort boundary/task-renumber cases; M2.2 dependency, failure-atomicity and mutation-family coverage; M2.3 resource/assignment/progress and baseline edit workflows; M2.4 command inventory, views, reports, print and locale/DPI evidence. One workflow does not close a package. |
| M3 / #796 | MPO (`.mpo`) is the authoritative native and default save format; earlier MPO read compatibility remains a gate. MPP reading remains in scope, while MPP writing is excluded by the user's 2026-10-12 decision. Assignment scalar/timephased work/cost baseline transfer, elapsed-duration normalization, literal zero/missing distinctions, XML/MSPDI and MPO round trips have focused evidence at `81dc3f5` and parent checkpoints. The 2026-10-13 checkpoint additionally preserves the MSPDI unassigned resource sentinel (ID 0 / UID -65535) through MPO save/reopen, restores task import state without edit-setter schedule recalculation, and verifies completed-milestone progress; core and exchange module tests passed together. `New Product.mpp` contains 38 standard-contour assignments and is not custom-MPP evidence. Issue [#805](https://github.com/tetsuji16/ProjectLibre/issues/805) provides provenance-backed custom-contour evidence for Project 9, limited to that project; provenance for Project 365/2024 remains incomplete, as do the independent MPP-read edition/build semantic manifest and remaining format rows in the roadmap. POD remains frozen legacy import-only coverage. |
| M4 / #797 | On 2026-10-12 the user explicitly excluded Microsoft Project VBA/macros, its macro-facing object model, COM automation, COM/VSTO add-ins, and local Office add-ins. These rows are EXCLUDED from the compatibility target; #797 can close as not planned. Native microProject local automation/extensions and offline collaboration are not excluded. |
| M5 / #795 | The full GUI run above is evidence for that earlier implementation head only. Final release still requires the exact final head's build, install/package launch, full GUI matrix, in-scope MPP-read/XML/MPO checks, POD frozen-fixture check and dated residual-coverage report. It remains blocked while M1/M2/M3 rows are partial. |

### Finite remaining rows for #793/#794

These are acceptance gaps, not scope exclusions. Keep each row open until its
linked evidence contains the required source, fixture, command, observed state
and result.

| Issue/package | Already evidenced | Remaining acceptance rows |
|---|---|---|
| #793 M1 | Swing renderer/ADR retained. The 36-image public archive has verified hashes and all six locale/scale legs; tab-strip, backstage and overflow images were visually reviewed on 2026-10-14. The narrow band viewport crops the rightmost caption in ja/125% and ja/150%; test the physical next-arrow route and fully-visible caption/proxy bounds before treating that as reachable. The archive has no contextual-tab captures or numeric bounds sidecar. Focused Windows test [#531](https://github.com/tetsuji16/ProjectLibre/issues/531) passed keyboard traversal and EDT accessible name/state assertions; production `ModernRibbonPanel` is unchanged from the screenshot source. | Publish the matched document-derived table with source applicability, published/access dates (record “not stated” where appropriate), and the microProject archive manifest paths/hashes/dimensions per state. Add contextual-tab screenshots to the candidate evidence or explicitly leave that acceptance row open. Keep numeric assertions separate from absent serialized bounds; no MSP observation or pixel-parity claim. |
| #794 M2.1 Editing/hierarchy/sorting | Resource name header toggle and numeric Standard Rate sort in both directions; empty names and equal keys retain stable order; selected resource identity and unchanged model order; Sort Order menu route; physical ja/100% evidence. Task View > Sort physically selected the existing generic `Sorter.StartDate`, sorted known distinct/equal dates stably, preserved task dates and outline order, and kept spreadsheet rows, Gantt projection rows, and the rendered Gantt selection highlight aligned at ja/100%. A separate opt-in permanent task-order checkbox now requires a second confirmation, sorts outline siblings using the existing sorter, renumbers IDs, restores selected row identity, and passed MPO reopen plus one Undo/Redo in the same physical ja/100% Robot journey. | Finish-date/descending and empty or missing task-date behavior; filter/group combinations; remaining inventory sort fields, multi-key semantics, and the distinct resource renumber contract. |
| #794 M2.2 Scheduling/dependencies/transactions | Update Project prevalidation/rollback, linked-task Undo/Redo, and project-local observer batching have evidence. On apply rollback, neither ScheduleEvent nor ObjectEvent observers see transient state; success and COMMIT-stage failure publish committed schedule state. `ModelTransactionTest` verifies VALIDATE/AUTHORIZE/CAPTURE/APPLY stage reporting and no partial state/commit, plus SCHEDULE/INVARIANT rollback and COMMIT semantics. Core full tests passed and the Japanese physical Project-menu route passed on Windows (session 8861). Existing transition matrix covers FS/SS/FF/SF and zero/positive/negative lag; a focused independent-date FS case crosses a dated project-calendar exception. #409 has partial evidence: project mutation scopes detach revision-tagged event facts before dispatch, the UI cache reconciles the current dependency graph before notifying views, `DefaultNodeModel` hierarchy commands use the shared Project mutation boundary, and single-project/single-pool `AssignmentService.newAssignments`/`replaceAssignment` enlist pool facts in that same revisioned publication. Undo groups are reserved in model commit order and activated during post-unlock publication, so delayed assignment edits remain one command and overlapping publications cannot merge or reorder history. `NodeIndentEdit` routes Undo/Redo through `NodeModel`. The focused `DefaultNodeModelTest` and full core/UI module tests passed on 2026-10-13. The clean JDK 25 command `:microproject_core:test :microproject_ui:test --no-daemon --console=plain` also passed after `clean` on 2026-10-13. GUI run 38339764343 (GUI486) passed task link/unlink and drag-between-bars dependency journeys; its assignment replace/Redo timeout was from the pre-fix head. This remains partial: targeted shared-pool/multi-project assignment writers now have focused core coverage, but the remaining public/legacy writer inventory, coherent immutable snapshots across every published model, and physical structural/assignment Undo evidence on the final head remain open. | Independent expected-date fixtures for the full FS/SS/FF/SF + lag matrix under varied calendars/constraints; verify only intended tasks move; warning/result behavior; Undo/Redo and MPO reopen for remaining mutation families; inject failures across mutation callers and prove model, Undo history and observers are unchanged. #409 remains partial: inventory and route every remaining public model writer through the shared barrier, verify coherent immutable snapshots for every published value, and complete physical structural/assignment Undo evidence on the final head. #403 architecture-child reconciliation remains open. |
| #794 M2.3 Resources/assignments/progress/baselines | Resource sort is view-only. Assignment work/cost and baseline work/cost import, edit/persistence slices have core/exchange evidence, including MPO reopen. | Selected-vs-all scope, proposed resources, leveling defaults/provenance, assignment totals, status date and actual-progress transitions; manual split preservation; baseline edit/clear/recapture; Undo/Redo and MPO reopen for each changed workflow. |
| #794 M2.4 Views/ribbon/context/reports/print/files | Current sort and Save As physical routes; report export fixture test. | Reconcile each #453/#769 command inventory row to source/status/enablement/physical route/visible result; view-only commands must preserve model; persistence where applicable; locale/DPI for changed routes; report value/rendering comparison and print page-boundary plus English/Japanese labels. Unsupported commands must be absent and documented. |
| #795 M5 | Build 1101 and full GUI run 38320038198 passed at commit 3a70041. | Run release checks against the final exact head, verify packaged launch, MPO older-version reads, MPP read (not write), XML/MSPDI and MPO round trips, frozen POD fixtures, all six GUI legs, and publish the dated residual-deviation/exclusion report. |

Undo action readiness update (2026-10-13): core/UI clean test run 49017 passed, and focused `RibbonButtonBehaviorTest` run 59496 passed after removing temporary test diagnostics. The root cause was a document-frame Undo action refresh during queued history delivery: `canUndo` was temporarily suppressed and no later edit callback refreshed the action. `EditSupport.historySettled()` now refreshes the frame only after committed history drains. A later GUI493 failure exposed a separate Redo ordering defect: assignment after-detail state replayed before the deferred replacement-connection edit, so the replacement was not attached and its detail restore was skipped. `EditSupport.postFinalEdit()` now places that snapshot after ordinary/deferred edits; the exact two-day shortcut focused tests passed in isolated run 1499 after restoring coordinator worktree changes; full core/UI module tests passed in run 63932 on the then-mixed workspace source. GUI493 ran against the earlier head; physical verification of the Redo fix on a new head remains pending. Issue #409 remains partial: shared-pool/multi-project writes and the remaining public/legacy writer inventory are open.

Shared resource-pool boundary update (2026-10-13): ResourcePool membership/ID mutations, shared-pool merge rewires, assignment creation/replacement, and portfolio close membership now use the shared coordinator; merge publishes task plus source/destination pool refreshes after detaching/switching/connecting, with independent pool revisions and object-identity membership. A failed reconnect restores assignment/task detail and association, then emits a conservative committed refresh and revision; it does not create an Undo edit. Current JDK 25 verification passed: :microproject_core:test :microproject_application:test :microproject_ui:test --console=plain --max-workers=1 --no-daemon (BUILD SUCCESSFUL, 1m53s). Existing CI Build1127 (38376421023) and GUI495 (38376420934) passed on base d61891de4; they predate these current uncommitted changes and are not final-head evidence. Remaining: audit other public/legacy writers, coherent immutable publication snapshots, persistence plus physical Undo/Redo acceptance on the final head. Issue #409 stays partial.

Assignment connect/remove boundary update (2026-10-13): Published initialized-project `newAssignment`, `connect`, single-assignment `remove`, and mixed-project collection removal now enter the shared model command boundary; uninitialized null-source import assembly remains silent. Service-owned creation/deletion Undo edits are recorded once, nested default-assignment maintenance inherits the outer Undo suppression, and collection removal restores assignment detail/schedule on Undo/Redo. The failed shared-pool reconnect regression also verifies the failed operation adds no Undo edit. JDK 25 `AssignmentServiceTest` passed, followed by `:microproject_core:test :microproject_application:test :microproject_ui:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` (BUILD SUCCESSFUL, 2m28s). This covers the current working tree, not physical GUI acceptance or every #409 writer; remaining public writers, renderer snapshots and final-head GUI Undo/Redo evidence keep #409 partial.

Dependency mutation boundary update (2026-10-13): `DependencyService` create/connect/remove/set-fields and bulk link/removal now enlist every endpoint owner and master event document through the shared coordinator. Each owning project receives one revision/batch only when a dependency fact affecting that project changed; cross-project Undo remains with each predecessor's project, and listener callbacks run after all scopes release. Initialized null-source writes publish a live refresh without Undo; uninitialized null-source import assembly remains silent. Dialog, Undo-edit and spreadsheet callers no longer issue a second update after `setFields`. Cross-project type, 3-project bulk link/Undo/callback, filtered candidate, same-ID ownership and null-source lifecycle regressions passed. JDK 25 combined core/UI/application/exchange tests passed on this working tree with `:microproject_core:test :microproject_ui:test :microproject_application:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` (BUILD SUCCESSFUL, 2m04s); the focused `DependencyServiceTest` also passed. Build1129 run 38383528544 and GUI497 run 38383528582 passed on base `8eb165bc` before this dependency-family patch; they are not evidence for the current head. #409 remains partial: other public/legacy mutation families, coherent immutable render snapshots, and final-head GUI Undo/Redo acceptance remain open. No milestone or release gate is closed by this family update.

Calendar live-commit boundary update (2026-10-13): Calendar dialog saves and `CalendarEdit` Undo/Redo delegate to `CalendarService.commitCalendars` / `commitCalendar`; staged scratch/import writes remain direct. Owner discovery and calendar history reservation happen under the global coordinator, and history delivery waits until every project/pool publication scope releases. Pre-apply failures discard the staged edit; a committed observer failure retains the correctly ordered Undo item. Calendar owner membership uses a transient identity set and immutable identity snapshot; `ResourceImpl.setWorkCalendar` now moves the actual resource identity between those memberships under the coordinator. Pool refreshes target affected ResourceImpl identities rather than only the pool document; semantic equality of a scratch clone is a no-op. The service completes each affected project schedule before detaching revisions and publishing view notifications. The numeric regression pins a task and assignment to Friday 2026-10-16 08:00, asserts the literal Monday 2026-10-19 17:00 finish before a staged Saturday 08:00–12:00 / 13:00–17:00 addition, then asserts the literal Saturday 2026-10-17 17:00 finish on commit, Undo, and Redo without caller-side recalculation. `MpxExportTrackingTest.committedSaturdayCalendarRecalculatesAndPersistsTaskAssignmentThroughMpo` checks raw native MPO `content.xml` and reloads the working Saturday, finish timestamps, and assignment work/actual/remaining values (16h/2h/14h). Focused handle 7591 and full `verifyArchitectureBoundaries` + core/UI/application/exchange handle 85038 passed on JDK 25 (BUILD SUCCESSFUL, 2m09s). No local physical GUI run was made; CI GUI acceptance is pending. #409 remains partial, with other public/legacy writers, immutable render snapshots and final-head physical Undo/Redo acceptance open.
## Dependency migration boundary

JasperReports remains at 6.21.5. The resolved and packaged runtime still includes
BeanUtils 1.11.0 and Commons Collections 3.2.2. Official JasperReports 7 migration
requires conversion of the old report templates using Jaspersoft Studio 7+;
no official standalone converter was established in this environment. Do not
upgrade only the dependency or apply a blanket exclusion: that previously
broke runtime behavior. Issues #242, #752 and #753 remain open.

## Verification results

Local JDK 25 runs cover all eight module test tasks: core 908, exchange 223,
UI 1,054, reports 12, application 37, bootstrap 14, contrib 2 and ribbon 5
(2,255 tests total, zero failures/errors, one documented core skip).
The maintenance-script suite passes 10 tests. The calendar XML fixture also
passes in separate startup-timezone JVMs for UTC, Tokyo and Mexico City.
Two UI test fixtures originally formatted UTC model dates in the host timezone;
the fixtures now use the production canonical UTC formatter. No date-editor
production behavior was changed to satisfy those assertions.

The Resource sort physical journey passed on Windows at Japanese locale and
100% UI scale after refreshing the runnable distribution with
`.\gradlew.bat :microproject_ui:installDist --rerun-tasks --no-daemon --max-workers=1 --console=plain`,
then running
`.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources" -PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --no-daemon --console=plain`.
The journey checks physical header sorting, mouse and Space activation of the
Sort menu arrow, the shared Sort Order action/dialog, numeric Standard Rate
ascending results, stable empty/equal text and numeric keys, selection identity,
model order, Workspace field state, and cancellation as a no-change outcome.
The follow-up sort regression clears selection before row-projection events and
restores the selected resource by identity. The task date-sort journey also
passed on Windows at ja/100% using the existing generic View > Sort chooser and
`Sorter.StartDate`. Its fixture uses `Task.setStart()` to retain explicit
SNET-constrained dates through project recalculation; it asserts an independent
date order with an equal-date tie, unchanged task dates and outline order,
spreadsheet/Gantt row identity, and the Gantt selection highlight after a real
table click. The resource and task journeys were rerun together after moving
their physical combo selection to the shared GUI test helper. Exact command:
`.\gradlew.bat :microproject_ui:installDist :microproject_ui:guiTest --tests 'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalViewSortOrdersTasksByStartDateAndKeepsGanttRowsAligned' --tests 'com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources' -PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --no-daemon --console=plain` (BUILD SUCCESSFUL; both tests passed). Finish-date/descending, empty/missing-date, and filtered/grouped rows remain open. Gradle evidence is in
`modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.xml`
and `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.xml`, plus `modules/microproject_ui/build/reports/tests/guiTest/index.html`.

The task sort journey now also covers the opt-in permanent-order route. It
physically selects a task by row header, checks the permanent option, cancels
the second confirmation and verifies unchanged task order/IDs, dirty state and
Undo availability. It then confirms, verifies stable outline order, renumbered
IDs, selected task identity and table/Gantt alignment, saves/reopens MPO, and
performs one Undo/Redo. The run passed after refreshing `installDist`, with
JDK 25 at ja/100%:
`.\gradlew.bat :microproject_ui:guiTest --tests 'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalViewSortOrdersTasksByStartDateAndKeepsGanttRowsAligned' -PguiTestSuite=full -PguiTestLocale=ja -PguiTestUiScale=1 --console=plain --no-daemon`.
The run also confirmed the selection must be restored after the view projection
refresh; this uses the existing `SpreadSheet.restoreTaskRowSelection` path and
guards the deferred EDT callback against a changed or closed document. This
closes the permanent task-order subrow only; other M2.1 sort boundaries remain
open.

The full core and UI unit suites passed after the permanent task sort changes:
`.\gradlew.bat :microproject_core:test :microproject_ui:test --console=plain --no-daemon`
(BUILD SUCCESSFUL, 1m29s).

Fresh install, architecture boundary, boundary fixture, dependency allowlist,
naming, and packaged sample MPP/POD import checks pass. These are local domain
and packaging checks, not Microsoft desktop full-interoperability evidence.
GitHub CI and Windows GUI results are recorded in the associated PR.

Resource spreadsheet writer update (2026-10-13): `DefaultNodeModel` now routes
Resource field edits and their existing `ModelFieldEdit` replay through one
coordinated owner-aware boundary. It commits one edit to the Resource model's
actual Undo controller, recalculates attached projects, and publishes resource
facts after pool/project locks release; no-op and parse failures add no history.
MPX/MPO conversion now carries CostRateTable A-E entries in both directions,
including effective intervals, standard/overtime rates and cost-per-use. The
round-trip regression uses a UI rate of 137.5/hour, separately asserts its
internal currency-per-millisecond value, consistent table/scalar XML value and
reloaded display. This is a document-derived rule from Microsoft's [Rate
element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/rate-element?view=project-client-2016),
[StandardRateFormat element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/standardrateformat-element?view=project-client-2016)
and [Resource schema](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-resources-element?view=project-client-2016),
not a measured MSP run. The focused rate round-trip and the formerly failing
POD serialized-payload stability regression both pass; the full Exchange module
suite passed on JDK 25 with
`.\gradlew.bat :microproject_exchange:test --console=plain --no-daemon --max-workers=1`.
`CostRateTables` exposes a non-creating lookup for optional tables so MPX
export does not mutate a project after its native payload has been serialized;
its existing manual `EnterpriseResource.writeObject` format is unchanged.
`DefaultNodeModelTest`, the no-mutation `CostRateTablesTest`, GUI test
compilation, and `verifyArchitectureBoundaries` also passed. The physical
ResourceView edit/Undo/Redo/MPO journey is compiled and awaits Windows GUI CI
evidence. Issue #409 remains partial: other published writers and immutable
render snapshots are still open.

## Verification policy

Record the exact command, commit, result and skipped cases with each published
change. The task-local JDK, proxy setup, fonts and generated build artifacts are
not product changes. A passing compile is not a passing test. Failed baseline
behavior is fixed or explicitly reproduced against the original commit;
it is not silently relabeled as successful validation.
