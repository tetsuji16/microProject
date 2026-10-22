# Microsoft Project compatibility implementation status

Reviewed 2026-10-15. This report records completed implementation slices and
remaining work against [the roadmap](msp-compatibility-roadmap.md).
**The complete compatibility objective is not achieved.** Gate issues remain
open; an inventory or passing narrow test does not close an entire milestone.

For an issue's latest execution state and next slice, use its current-state
packet as described in [issue execution](issue-execution.md). Entries below
remain supporting historical evidence; the packet does not replace these
compatibility gates or the GUI recovery order.

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
  out-of-range row during sort events. Broader resource multi-key/date/filter/
  group behavior remains open under M2.1/#794. Issue #750 was closed on
  2026-10-15 for its reviewed menu/header contract. [Microsoft
  `Application.Sort`](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort)
  documents its `Renumber` argument for task sorting, so permanent resource
  renumbering is not an applicable requirement.
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
  The #794 M2.2 GUI slice now physically enters FS `-1d` through the Gantt
  edge `DependencyDialog`, checks the calendar-adjusted successor date with
  the project-start constraint, unaffected task dates/adjacency, Undo/Redo,
  and MPO reload. Focused ja/100% Robot journey passed at SHA
  `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`; negative-lag semantics remain a
  document-derived decision based on Microsoft's lead/lag guidance.
  See [transaction scope](architecture/model-transaction-slice.md).
- Diagnostic trace generation tolerates an uninitialized scheduling engine or
  absent trace task. A calendar warning no longer becomes an unrelated
  NullPointerException in a freshly started importer process.
- Task Inspector now has a Task ribbon route and a left pane that follows task
  selection. It displays task start/finish; actual-start and assignment facts;
  leveling delay; task constraint type/date and the nearest constrained summary
  ancestor; predecessor names, link types and lag/lead; child dates with a
  marker when a child date equals a summary bound (an observed match, not proof
  of causation); effective calendar name and weekly work hours; scheduling
  mode with an explicit Auto Schedule action for manual tasks; F9 recalculation
  guidance; and separate `ScheduleDiagnosticsService` findings. Predecessor
  navigation resolves a stable node identity through model and visible-row
  coordinates; hidden rows produce feedback. This is a partial,
  document-derived implementation of
  Microsoft's [scheduling factors guidance](https://support.microsoft.com/en-us/project/view-and-track-scheduling-factors),
  not a measured Project run. Headless panel/model-preservation and
  sorted/filtered row tests pass; Windows physical navigation and recalculated
  result evidence remain pending.
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
| M1 / #793 | **COMPLETE (document-derived; no pixel-parity claim).** The [ribbon audit](RIBBON_MSP_PARITY_AUDIT_JA.md) maps Microsoft-published sources and their edition/date/state scope to the six-leg app captures; user direction and `AGENTS.md` permit this normative comparison without an installed Project client. Stable source run [38495421214](https://github.com/tetsuji16/ProjectLibre/actions/runs/38495421214) passed Windows full GUI, packaged-content/launch, Linux, and macOS package jobs for candidate `702146871dbe9059412b83ab7780936e01f2e7f2`. Public [v2 evidence archive](https://github.com/tetsuji16/ProjectLibre/releases/download/v0.0.24.1244/microProject-gui-evidence-v2-v0.0.24.1244.zip) contains 48 PNGs (six states × six locale/scale legs plus two contextual states × six legs), with per-image source path/hash/bytes/dimensions and source run/artifact provenance; all images were visually reviewed on 2026-10-14. Focused keyboard/accessibility test [GUI run 531](https://github.com/tetsuji16/ProjectLibre/actions/runs/38500386903) verifies physical Tab/Space/Down/Enter traversal and EDT AccessibleContext name/selected/focus state. PR [#816](https://github.com/tetsuji16/ProjectLibre/pull/816) verifies physical narrow-band next-arrow scrolling, fully visible final caption/control bounds, and last-group popup reachability; method passed Windows GUI run [38505114590](https://github.com/tetsuji16/ProjectLibre/actions/runs/38505114590). Microsoft client build/channel/OS/window bounds are not stated or measured by the published references, and the archive does not serialize numeric window bounds. The Report/Chart/Table Tools reference state differs from the app's Network/Calendar Format captures; implementing those report commands is a separate #794/#453/#769 behavior scope and is not claimed here. |
| M2 / #794 | Issue #750 is closed for its reviewed Resource Sort menu/header contract; the Windows physical run is 38728563004. Task sort PRs #815/#827 cover the permanent-order contract, and the new filtered/grouped projection fixture records the document-derived composition rule. Remaining issue rows are finite in the roadmap table below: M2.1 broader resource sort boundaries; M2.2 dependency, failure-atomicity and mutation-family coverage; M2.3 resource/assignment/progress and baseline edit workflows; M2.4 command inventory, views, reports, print and locale/DPI evidence. One workflow does not close a package. |
| M3 / #796 | MPO (`.mpo`) is the authoritative native and default save format; earlier MPO read compatibility remains a gate. MPP reading remains in scope, while MPP writing is excluded by the user's 2026-10-12 decision. Assignment scalar/timephased work/cost baseline transfer, elapsed-duration normalization, literal zero/missing distinctions, XML/MSPDI and MPO round trips have focused evidence at `81dc3f5` and parent checkpoints. The 2026-10-13 checkpoint additionally preserves the MSPDI unassigned resource sentinel (ID 0 / UID -65535) through MPO save/reopen, restores task import state without edit-setter schedule recalculation, and verifies completed-milestone progress; core and exchange module tests passed together. `New Product.mpp` contains 38 standard-contour assignments and is not custom-MPP evidence. Issue [#805](https://github.com/tetsuji16/ProjectLibre/issues/805) provides provenance-backed custom-contour evidence for Project 9, limited to that project; provenance for Project 365/2024 remains incomplete, as do the independent MPP-read edition/build semantic manifest and remaining format rows in the roadmap. POD remains frozen legacy import-only coverage. |
| M4 / #797 | On 2026-10-12 the user explicitly excluded Microsoft Project VBA/macros, its macro-facing object model, COM automation, COM/VSTO add-ins, and local Office add-ins. These rows are EXCLUDED from the compatibility target; #797 can close as not planned. Native microProject local automation/extensions and offline collaboration are not excluded. |
| M5 / #795 | The full GUI run above is evidence for that earlier implementation head only. Final release still requires the exact final head's build, install/package launch, full GUI matrix, in-scope MPP-read/XML/MPO checks, POD frozen-fixture check and dated residual-coverage report. It remains blocked while M1/M2/M3 rows are partial. |

### Finite remaining rows for #793/#794

These are acceptance gaps, not scope exclusions. Keep each row open until its
linked evidence contains the required source, fixture, command, observed state
and result.

| Issue/package | Already evidenced | Remaining acceptance rows |
|---|---|---|
| #793 M1 | Swing renderer/ADR retained. The public v2 archive has 48 PNGs, per-image manifest hashes/source paths/dimensions and six locale/scale legs; all images were reviewed 2026-10-14. The dated document-derived source comparison is in the [ribbon audit](RIBBON_MSP_PARITY_AUDIT_JA.md); Microsoft references do not supply observed client build/environment facts, and no pixel-parity claim is made. Test [GUI run 531](https://github.com/tetsuji16/ProjectLibre/actions/runs/38500386903) passed physical keyboard and AccessibleContext assertions; #816's next-arrow/caption/proxy/popup Robot method passed GUI run [38505114590](https://github.com/tetsuji16/ProjectLibre/actions/runs/38505114590). The narrow row is partially cropped before scrolling and fully reachable afterward. | **COMPLETE for visual-only M1 via document-derived comparison**, following the user-authorized `AGENTS.md` evidence rule. Report/Chart/Table Tools functionality and corresponding command implementation remain tracked by #794/#453/#769, not claimed as ribbon parity here. Numeric bounds are asserted by test where applicable; the historical screenshot archive has no serialized numeric bounds. |
| #794 M2.1 Editing/hierarchy/sorting | Resource name header toggle and numeric Standard Rate sort in both directions; empty names and equal keys retain stable order; selected resource identity and unchanged model order; Sort Order menu route; physical ja/100% evidence. Issue #750 closed for this reviewed scope; the later Windows GUI run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004) passed both resource header/menu acceptance and `TaskInformationRibbonGuiAcceptanceTest.physicalTaskSortSupportsAscendingStartAndDescendingFinishForViewAndOutline`. Task sort PRs [#815](https://github.com/tetsuji16/ProjectLibre/pull/815)/[#827](https://github.com/tetsuji16/ProjectLibre/pull/827) cover task multi-key directions, stable ties, missing/empty values, flat Keep Outline projection, selection/Gantt alignment, unchanged model/schedule, and separately confirmed permanent ordering with renumber, Undo/Redo and MPO evidence. New `ViewNodeModelCacheHierarchyDependencyTest.taskSortAppliesToFilteredRowsWithinConfiguredGroupsWithoutChangingModelOrder` checks that active filtering excludes completed tasks, two nonempty Duration buckets remain visible in configured order, visible task names sort inside each bucket, selection maps through the filtered projection, and project task order remains unchanged. This composition is an Office-consistent product rule derived from Microsoft sort/filter/group documentation, not measured MSP behavior. The focused UI test passed on the current patch. #761 is ready for primary closure review after this fixture merges; broader resource sort parity remains M2.1/#794 work. [Microsoft `Application.Sort`](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort) documents its `Renumber` argument for task sorting, so permanent resource renumbering is not an applicable requirement. PR [#855](https://github.com/tetsuji16/ProjectLibre/pull/855) merged as `735a49c3d` and adds resource Key1–Key3 sorting through the single `ResourceView.applySortOrder` pipeline (dialog, header, Workspace), unit coverage for text/number/date, empty and equal values in both directions, a filter→group→sort resource projection fixture, and a physical Key1+Key2 Robot journey (dialog, visible order breaks the Name tie, Workspace save/restore of both keys, unchanged model). PR evidence covers Windows ja/100%, en/100%, ja/125%, ja/150%, en/125%, and en/150%; all six locale/scale legs now have physical journey evidence, with screen resolution held at 2560×1440. It also makes structural paste atomic (journaled rollback of hierarchy, task registration and the multiple-transaction bracket, with an injected-failure test) and fixes listener add/remove symmetry and Windows/Office Enter/default-button behavior in dialogs. | Resource multi-key implementation/tests are merged. Remaining M2.1 work includes #414 input-path consolidation. The installed-layout blank-project open/edit/save/reload path has now been manually verified at 2560×1440 and recorded in U-30. The current final-head full GUI gate passed the Japanese 100% full functional suite and the selected Japanese/English visual classes across all six locale/UI-scale conditions, but does not close these narrower M2.1 gaps. The final-head GUI audit also includes M2.3 leveling findings. |
| #794 M2.2 Scheduling/dependencies/transactions | Update Project prevalidation/rollback, linked-task Undo/Redo, and project-local observer batching have evidence. On apply rollback, neither ScheduleEvent nor ObjectEvent observers see transient state; success and COMMIT-stage failure publish committed schedule state. `ModelTransactionTest` verifies VALIDATE/AUTHORIZE/CAPTURE/APPLY stage reporting and no partial state/commit, plus SCHEDULE/INVARIANT rollback and COMMIT semantics. Core full tests passed and the Japanese physical Project-menu route passed on Windows (session 8861). PR [#813](https://github.com/tetsuji16/ProjectLibre/pull/813) adds six fixed-date dependency cases: FS/SS with 8h lag and FS/SS/FF/SF with positive 4h lag across Tuesday/Wednesday calendar exceptions; 40 core tests passed with Build and GUI checks. This is focused coverage, not the full varied-calendar/constraint matrix. #409 has partial evidence: project mutation scopes detach revision-tagged event facts before dispatch, the UI cache reconciles the current dependency graph before notifying views, `DefaultNodeModel` hierarchy commands use the shared Project mutation boundary, and single-project/single-pool `AssignmentService.newAssignments`/`replaceAssignment` enlist pool facts in that same revisioned publication. `replaceAssignment` now compensates partially attached replacements and restores source/task/assignment details on RuntimeException/Error inside `runAtomicModelCommand` with the pool event manager enlisted; the deterministic failure tests cover failure after source removal and after the first replacement, including before/after resource-link attach. Exact verification on 2026-10-19: `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.assignment.AssignmentServiceTest" --console=plain --no-daemon --max-workers=1` passed 24 tests (0 failures/errors/skips), and `git diff --check` passed (one existing LF-to-CRLF warning for this status file). Undo groups are reserved in model commit order and activated during post-unlock publication, so delayed assignment edits remain one command and overlapping publications cannot merge or reorder history. `NodeIndentEdit` routes Undo/Redo through `NodeModel`. The focused `DefaultNodeModelTest` and full core/UI module tests passed on 2026-10-13. GUI run 38339764343 (GUI486) passed task link/unlink and drag-between-bars dependency journeys; its assignment replace/Redo timeout was from the pre-fix head. This remains partial: targeted shared-pool/multi-project assignment writers now have focused core coverage, but the remaining public/legacy writer inventory, coherent immutable snapshots across every published model, and physical structural/assignment Undo evidence on the final head remain open. | Independent expected dates for remaining FS/SS/FF/SF lag combinations under varied calendars/constraints; verify only intended tasks move; warning/result behavior; Undo/Redo and MPO reopen for remaining mutation families; inject failures across mutation callers and prove model, Undo history and observers are unchanged. #409 remains partial: inventory and route every remaining public model writer through the shared barrier, verify coherent immutable snapshots for every published value, and complete physical structural/assignment Undo evidence on the final head. #403 linked-child replacement has focused success, injected-rollback, and updated-child MPO save/reopen coverage. `ProjectFactorySubprojectReplacementTest.setAfterProjectMembershipReplacement` verifies rollback when `Portfolio.replaceProject` fails after membership changes. |
| #794 M2.3 Resources/assignments/progress/baselines | Resource sort is view-only. Assignment work/cost and baseline work/cost import, edit/persistence slices have core/exchange evidence, including MPO reopen. Whole-project Clear Baseline now has physical route, Undo/Redo and MPO evidence. Baseline 1 Entire Project recapture now verifies the overwrite prompt, Baseline 0/current-value preservation, Undo/Redo and MPO in both states. Proposed-resource leveling has physical opt-in, Apply, Undo/Redo and exact assignment-start MPO round trip. Assignment Replace now verifies task Work/Actual Work/Remaining Work/Cost against assignment totals before and after Replace, physical Undo/Redo, and MPO reopen. Manual assignment split preservation across Clear Leveling now has physical GUI, Undo/Redo, and MPO round-trip evidence. Mark on Track now uses an explicit mid-task status date and verifies exact work-calendar progress, physical Undo/Redo and exact MPO persistence. Update Project now covers an assigned partial-progress task completed through a fixed date, with task/assignment actuals, Undo/Redo and MPO checks. Level Selection and Level All have separate physical scope, selection, Undo/Redo, and MPO coverage. | Remaining leveling sensitivity default/provenance; other status-date/actual-progress transitions; other numbered baseline slots and field/scope combinations; Undo/Redo and MPO reopen for each changed workflow. |
| #794 M2.4 Views/ribbon/context/reports/print/files | Current sort and Save As physical routes; Task Information report physical selection, CSV equality to every rendered row, distinct date/duration values, nonblank rendering of every pageable page, no model mutation, and date/duration/cost MPO reload checks. Project Details now verifies rendered Work/Cost labels and values against the project fields, physical CSV row equality, printable page rendering, and no model mutation. | Reconcile each #453/#769 command inventory row to source/status/enablement/physical route/visible result; view-only commands must preserve model; persistence where applicable; locale/DPI for changed routes; report coverage for other definitions, print page-boundary plus English/Japanese labels. Task Information has no Work/Cost columns. Newly observed unassigned-task Work drift across MPO reload is tracked by [#868](https://github.com/tetsuji16/ProjectLibre/issues/868). Unsupported commands must be absent and documented. |

Selected Tasks timescale zoom for Gantt, Tracking Gantt, Task Usage and Resource
Usage is implemented as a document-derived compatibility decision from Microsoft's
[`Application.ZoomTimescale`](https://learn.microsoft.com/en-us/office/vba/api/project.application.zoomtimescale)
documentation (`Selection=True` fits selected tasks in Gantt and both Usage
views). The shared
selection resolver spans valid task start/finish dates; the view selects a
timescale that fits the interval and scrolls to its start without changing
project data. Focused unit coverage checks no selection, unusable dates, one
task and multiple tasks. The localized View > Zoom command is wired through the
shared ribbon action pipeline and enabled only for a dated selection in any of
the four views. Physical Robot proof passed in
`TaskInformationRibbonGuiAcceptanceTest.usageDetailViewRouteConstructsWithoutRuntimeFailure`:
the Gantt and Tracking Gantt task rows, Task Usage task row, and Resource Usage
assignment row were selected; each visible timescale range contained the
selected task's start/finish and both dates remained unchanged. Run used
Japanese locale and UI scale 1; screen resolution was not changed.
The full `:microproject_ui:test :microproject_ui:installDist` rerun passed
1,163 tests with 0 failures, errors, or skips; `installDist` was already current.
The first full rerun exposed three missing ribbon audit registrations for the
new command (unique icon key, explicit view-only classification, and GUI
use-case coverage); these were added before the passing rerun. The focused
Japanese/UI-scale-1 physical route passed 1/1. No display resolution was changed.
The current Japanese/UI-scale-1 full GUI task also passed 211 tests with 0
failures, 0 errors, and 1 existing high-DPI-only conditional skip. The run did
not change display resolution and does not claim the six-leg locale/scale matrix.
| #795 M5 | Build 1101 and full GUI run 38320038198 passed at commit 3a70041. | Run release checks against the final exact head, verify packaged launch, MPO older-version reads, MPP read (not write), XML/MSPDI and MPO round trips, frozen POD fixtures, all six GUI legs, and publish the dated residual-deviation/exclusion report. |

### #414 / #794 — Phase 1 selection foundation on the current working tree

The current source contains `ActiveTaskSelectionResolver`; `DocumentFrame` uses
it to resolve the live spreadsheet selection for both selected-task access and
selection eligibility, and `GraphicManager.setButtonState()` derives hierarchy
and dependency enablement from those same frame methods. Command outcomes carry
selected IDs, affected IDs, status, reason, and the active view. A source audit
found that Indent/Outdent enablement accepted mixed task/resource selection,
while execution rejected it; Outdent also duplicated its parent check. The
enablement now uses the strict task-only selection contract, and its Outdent
check delegates to `DocumentFrame.canOutdent`. A regression test verifies that
mixed selection disables both hierarchy actions while Link/Unlink remain
enabled when enough editable tasks are selected. On 2026-10-18, the focused
headless verification passed on this working tree:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.ActiveTaskSelectionResolverTest" --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --tests "com.microproject.pm.graphic.frames.GraphicManagerLinkRouteTest" --console=plain` (BUILD SUCCESSFUL). Information button enablement and execution also used separate selection extraction. A typed `InformationSelection` snapshot now supplies general, Task, and Resource information enablement and is re-resolved by the execution route; only execution emits the existing multi-selection warning. `RibbonButtonBehaviorTest.informationEnablementUsesOneTypedSelectionSnapshotForEverySelectionKind` covers task, resource, assignment, project, multiple, and header-column selections. The full focused verification was rerun with `--rerun-tasks` on 2026-10-18: `RibbonButtonBehaviorTest` plus the resolver and Link route tests passed (BUILD SUCCESSFUL, 54 seconds). This confirms those focused
contracts, not Phase 1's complete physical Robot gate. The typed selection is
re-resolved at dispatch so it reflects the live selection; only dispatch emits
the existing multiple-selection warning. The headless test covers task,
resource, assignment, project, multiple, and header-column selection.
The follow-up source audit found `GraphicManager.dispatchTaskCommand()` was
overwriting frame-reported selection IDs with IDs captured before execution.
That contradicted Indent/Outdent's documented post-editor-commit selection
resolution. The dispatcher now preserves semantic result IDs and uses the
pre-route selection only for legacy `DISPATCHED` results with no selection
IDs. `CommandRouteMatrixTest` verifies both explicit-ID preservation and the
legacy fallback. After this correction, `:microproject_ui:test --console=plain`
passed on 2026-10-18.
Selection-shortage coverage now also checks the Information buttons and
Link/Unlink are disabled with no selected task, and the canonical Unlink route
reports `REJECTED/no-selection`. The existing physical empty-selection ribbon
journey now checks Task Information and Expand are disabled too. The focused
`RibbonButtonBehaviorTest` and `GraphicManagerLinkRouteTest` passed (59 tests,
0 failures), and the updated GUI test source compiled. The complete
`:microproject_ui:test --console=plain` suite then passed on the current tree
after those additions. These are not physical Robot results.
`TaskInformationRibbonGuiAcceptanceTest.robotClickOnTaskPropertiesInformationOpensTaskInformation`
now captures both UI and command-result diagnostics. A shared assertion now
compares the physical selected task IDs with semantic selected/affected IDs
and the independently observed view in the existing Task Information,
Link/Unlink, Indent/Outdent, Expand/Collapse, and Hide Selected/Show All Robot
journeys. The assertion keys the physical event by ribbon ID and the semantic
result by `CommandId.actionId` (for example, `RibbonIndent` versus `Indent`).
The existing name-cell editor shortcut journey also asserts the semantic
Indent/Outdent selected and affected IDs after each editor-commit route. The
first local Robot run was invalidated before input because Docker Desktop
overlapped the GUI window. With PID 5544 minimized, the focused 7-test run on
2026-10-18 exercised the exact ja/100% physical routes: Task Information,
Link/Unlink, Indent/Outdent, Expand/Collapse, Hide/Show, the editor shortcuts,
and selection shortage. The first run passed 3/7 and exposed missing result
selection IDs in Link/Indent/Collapse plus the shortcut's bypass of semantic
result diagnostics. `DocumentFrame` now reports operation selection IDs for
dependency and hierarchy commands; `GraphicManager` routes the hierarchy
shortcuts through the canonical command result. The final rerun passed all
7/7 Robot tests, including Undo/Redo and save/reload in the mutation journeys.
The focused shortcut test also passed separately. Phase 1's listed command
gate is now satisfied for this local ja/100% run. The regenerated installDist
was used by the GUI task; a separate manual launch and the broader GUI matrix
remain pending. This does not close #414 or the wider M2.1/M2 scope.

Shortcut registration guard (2026-10-19): `MicrosoftShortcutsRootPaneTest`
now uses a recording `InputMap` around `GraphicManager.applyMicrosoftShortcuts`
to detect duplicate keystrokes assigned to different action IDs before
Swing's last-write-wins behavior hides them. Current global registrations have
no conflicts. The focused class passed; no GUI or display-resolution test ran.

### #794 — Phase 2 shared structural command gate

The reusable `OutlineFixture` contains a summary with first and last child, a
distinct descendant leaf, two adjacent top-level tasks, and a designated
individually read-only external target. The physical Hide/Show, Collapse/Expand,
Indent/Outdent, and Link/Unlink ribbon journeys reuse it. The Indent/Outdent
journey verifies the external task is read-only while its project remains
writable. Each journey retains its model/view, Undo/Redo, MPO, and command-result
assertions. On 2026-10-18,
`:microproject_ui:compileGuiTestJava` passed and the four migrated ja/100%
physical journeys passed together (4 tests).

The shared outline fixture now covers physical ribbon, popup, and Alt+Shift
hierarchy routes in this GUI test class. The Collapse/Expand journey also uses
Robot Alt+Shift+Subtract/Add after the ribbon route and checks view state,
selection IDs, active view, and absence of Undo edits. The hierarchy shortcut
journey now rejects a collaboration-locked task without changing hierarchy,
table/Gantt rows, or Undo history, then verifies the successful route. The
complete GUI task was run on 2026-10-18 after the fixture update and found
11 failures. The selection-result defect was traced to result constructors
that populated affected IDs but left selected IDs empty. `DocumentFrame` now
publishes both ID sets for Task Mode, Respect Links, and Inactivate outcomes;
headless tests passed (43 tests), and the previously failing Respect Links and
Inactivate Robot journeys passed on rerun. Assignment and ribbon visibility
fixtures now isolate persisted ribbon display preferences. Resource leveling
now handles assignment-only changes, split persistence, and actual task IDs;
the physical Resource Pool test isolates Ribbon mode, environment flags,
JobQueue, and Swing alert state. A CCPM spreadsheet Robot fixture now waits for
its window and cell focus before asserting input. These fixes were followed by
a full Japanese/100% run on 2026-10-18:
`:microproject_ui:guiTest --max-workers=1 --no-daemon --console=plain
-PguiTestLocale=ja -PguiTestUiScale=1` completed 208 tests with 0 failures and
1 conditional high-DPI-only skip. The latest `:microproject_ui:test
--max-workers=1 --no-daemon --console=plain` completed successfully (all tasks
up-to-date); the full unit suite had also passed on the same production changes
before these GUI-fixture-only updates.

A later Phase 2 review found that Indent/Outdent asserted task existence and
visibility but not the rendered outline depth/order and vertical alignment of
the task's table row with its Gantt row. The fixture's former "read-only" task
was also an alias for an editable adjacent task; the test only toggled the
whole document read-only. The focused Indent/Outdent journey now asserts table
depth/order and table-to-Gantt row alignment before and after Indent, Outdent,
Undo/Redo, and MPO reload, and uses a distinct external task whose `isReadOnly`
is true in a writable project. Its physical read-only shortcut exposed
[issue #869](https://github.com/tetsuji16/ProjectLibre/issues/869): the command
result said `no-selection` for a selected read-only task. The resolver now
reports `task-read-only` with the selected ID; 3 resolver unit cases and the
focused physical journey pass. After these changes,
`:microproject_ui:test --console=plain --no-daemon --max-workers=1` passed.
The latest serialized Japanese/100% GUI gate
`:microproject_ui:guiTest --console=plain --no-daemon --max-workers=1
-PguiTestLocale=ja -PguiTestUiScale=1` passed 218 tests across 49 suites,
with 0 failures/errors and 1 conditional high-DPI-only skip. An earlier full
rerun had one failure in the unrelated window-promotion test; that test passed
both alone and in the subsequent full run. `:microproject_ui:test --console=plain
--no-daemon --max-workers=1` passed. `TaskInformationRibbonGuiAcceptanceTest`
passed as a full class. Display remained 2560×1440; no resolution-change test
was run. For issue #60, the real outline-summary Robot route now verifies the
selected parent and all descendants disappear from the task table and Gantt,
MPO persistence, Undo/Redo, Show All, and preservation of unrelated/read-only
tasks. A separate physical virtual-group route covers group member IDs and
MPO persistence. The test clicks away from the disclosure icon and asserts
that Hide/Undo preserves the expanded view state. The earlier reported
summary-row case is reproduced with a real outline summary. The current-head
Phase 2 exit gate passed as recorded below (UI suite plus physical GUI suite);
no original-project comparison file was supplied, so that comparison is not a
remaining Phase 2 gate. Broader M2 acceptance and the full locale/scale matrix
remain open.
This is local ja/100% evidence; the full locale/scale workflow matrix and
overall M2 acceptance remain open.
The remaining #266 multi-link Task ribbon Unlink gap now has a focused physical
Robot journey: it clicks the ribbon command, chooses one incident link using
keyboard input, verifies only that dependency disappears from the model,
successor Predecessors cell, and Gantt, then verifies physical Ctrl+Z/Ctrl+Y
and MPO save/reload. The Japanese, scale 1.0 run passed on 2026-10-19 at
2560×1440; no display-resolution changes were made. Broader Phase 2 structural
command gate review remains open.
### #794 — Phase 3 initialization and view creation (evidence recorded)

The Usage Detail grid now retains configured fields and falls back to assignment
fields only when configuration is missing or empty. Unit coverage and the
physical Task/Resource Usage route pass with populated models. The Timesheet
Robot route checks populated rows and Escape closure without changing active
view or Undo state. Resource Pool creation is verified by a physical ribbon
click that registers a master pool project and opens a populated ResourceView;
the fixture restores the shared JobQueue and records Alert warning diagnostics
if the actual dialog fails to appear. The dialog remains physically observed
and dismissed through Robot. The full Japanese/100% GUI suite passed on
2026-10-18 (208 tests, 0 failures, 1 conditional high-DPI-only skip), including
both Usage Detail routes and Timesheet Escape. Phase 3 evidence is recorded;
the recovery sequence can proceed to the Phase 4 window-shell audit. Overall
M2 acceptance remains open.

### #794 — Phase 4 window-shell status (functional exit gate passed)

Primary and secondary document shells now share root-pane shortcuts and one
window-focus activation path. Repeated activation of the current frame skips
container reparenting and repaint; `DefaultFrameManagerTest` covers that
idempotence. The title update path now targets the host that owns the document,
so switching projects cannot replace the primary title with a secondary
project name. This corrected a defect found by the new physical journey, which
asserts every shell retains its own project title after switching.

`ResourcePoolInitialViewGuiAcceptanceTest.physicalMultipleProjectAndResourcePoolWindowsSwitchCommandAndCloseCleanly`
passed with the Japanese/English × 100/125/150% matrix. It physically switches
between two projects and a resource pool, checks the active frame and retained
ResourceView after every switch, clicks a ribbon tab to establish keyboard
focus, invokes Ctrl+F1 on a secondary document, and closes each document using
the actual native title-bar button. The primary-shell close is isolated from
application shutdown by the fixture's document-close callback. No document
registration or secondary native window remains.

The 2026-10-18 `visual` GUI acceptance matrix completed all six ja/en ×
100/125/150% UI-scale legs without changing desktop resolution. Japanese 100%
passed 96 tests with 0 failures and 1 conditional skip. Japanese 125% and
150% each ran 96 tests with 1 failure and 1 conditional skip; both failures
were in
`RibbonExternalCommandGuiAcceptanceTest.robotEscapeCancelsDefaultWindowsFileOpenChooserWithoutOpeningAProject`
(125% failed the physical Win32 `IDCANCEL` click; 150% failed the Ctrl+O
Escape). English 100%, 125%, and 150% each passed 75 tests with 0 failures
and 1 conditional skip. The native window-shell journey for caption drag,
maximize/restore, Alt+Space system-menu commands, edge resize, and
usable-work-area bounds also passed in the matrix. The phase's
two-project/resource-pool focus, command, title, and close exit gate passed.
On 2026-10-18, the native caption drag/maximize/restore/system-menu/resize
journey also passed on this Windows 11 Pro host (`DisplayVersion=26H2`, build
26300.9711) at ja/100%, with the desktop left at 2560×1440. The exact Windows
servicing channel for this host has not been established, so this is not
stable-channel client evidence.

After the matrix run, a Win32-to-AWT `Robot` coordinate conversion was added
for the native `IDCANCEL` click, and the foreground details at Escape-send
time are now logged. `:microproject_ui:compileGuiTestJava` passed, and the
focused native chooser Robot test was rerun separately at Japanese 125% and
150% (1 test per scale); both passed with no skip or failure. The previous
150% intermittent Escape failure's cause remains unproven; on the rerun the
chooser was foreground at send time. A full `visual` rerun after the fix
completed all 96 Japanese 100% JUnit cases with 0 failures and 1 conditional
skip, but Gradle failed while finalizing its binary test results with
`NoSuchFileException: .../test-results/guiTest/binary/in-progress-results-generic.bin`;
the wrapper stopped before starting the locale/scale legs. The XML results
were all present and green, but the Gradle task result is failed and is not
counted as a passing gate. A serialized rerun of the same twelve Japanese 100%
classes then passed as a Gradle task (`BUILD SUCCESSFUL`, 96 tests, 0 failures,
0 errors, 1 conditional skip) with `--stacktrace`; no result-finalization error
recurred. This narrows the binary-results failure to an intermittent issue but
does not establish its cause or replace the still-pending full visual gate.
Per user direction, GUI verification uses the desktop's existing resolution;
no resolution-changing tests are needed or run.

The direct ribbon command Robot sweep no longer assumes a 1900px window from
the former CI resolution setup. It fits the current usable desktop work area
and physically opens responsive group proxies for commands that collapse at
that width; File/Backstage dispatch remains on its existing route. The focused
`RibbonTabGuiAcceptanceTest.robotClicksEveryStandardRibbonCommandOnce` run
passed (1 test, 0 failures/errors/skips) on 2026-10-18, along with
`:microproject_ui:compileGuiTestJava`. No desktop resolution was changed. This
keeps the single-route sweep viable on narrower CI desktops; it does not add a
resolution-matrix test or close the full final-head GUI gate.

#### U-29 popup visual audit (2026-10-18)

The shared screenshot helper waits for the dialog to own the foreground window,
waits for the Robot/event queue to settle, then verifies that captured pixels
match the rendered dialog before saving. Nine popup surfaces were reviewed:
the six #590 screens (Change Working Time, Calendar Options, Help, Locale
Settings, Update Tasks, Recurring Task), plus Project Information, Clear
Baseline, and Task Information. Task Information was captured on both General
and Resources tabs. The latest focused Task Information Robot route and
`ComponentFactoryTextStateTest` passed after fixing model-provided text that was
incorrectly shown as edited; fresh Task Information captures show normal
foreground text. Help and Locale captures are crisp, and the other reviewed
surfaces fit their dialog bounds. PNG evidence is under
`modules/microproject_ui/build/reports/guiTest-artifacts/`.

The current-tree `:microproject_ui:test --console=plain` suite also passed
(1,153 tests, 0 failures/errors, 0 skipped; BUILD SUCCESSFUL in 1m25s).
The focused checks were `:microproject_ui:test --tests
com.microproject.dialog.util.ComponentFactoryTextStateTest --console=plain`
and `:microproject_ui:guiTest --tests
com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotClickOnTaskPropertiesInformationOpensTaskInformation
-PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --console=plain`;
both passed on the final current source.

#### U-29 visual gate completion (2026-10-19)

The `visual` wrapper passed at the existing 2560×1440 desktop. It ran the
Japanese 100% functional leg and all six Japanese/English × UI-scale
100/125/150% legs serially. Each leg completed 96 tests with 0 failures, 0
errors, and 1 explicitly allowed conditional skip. The exact command was
`.\.github\scripts\run-gui-acceptance.ps1 -Suite visual`; no display-resolution
change was invoked. The six leg logs and JUnit bundles are under
`modules/microproject_ui/build/reports/guiTest-artifacts/`.

During this run, Clear Baseline exposed persisted ribbon state: the ribbon
could start in tabs-only mode, where clicking the already-selected Project tab
does not reopen its command surface. Its GUI journey now begins from that
collapsed state and physically restores the ribbon through the tab's display
menu before opening the command. A readiness race in that physical test route
was fixed by waiting for the exact display-mode popup and visible Show Ribbon
item before clicking. The Clear Baseline route passed in all six locale/scale
conditions, and its full-leg tests passed in the final wrapper. The native
Windows Open chooser probe also had a classification race: its authoritative
`ready=True` result could arrive before its stage sampler emitted a readiness
timestamp. The completion helper now accepts that explicit successful marker;
the regression test and physical Escape route passed. Neither fix adds retries
or suppressions.

After the full wrapper, the popup-state scan and stale-path clear were moved to
the Swing EDT. On that exact final test source, the Clear Baseline physical
journey passed again for all six Japanese/English × 100/125/150% conditions;
`:microproject_ui:compileGuiTestJava` passed as well. The production code did
not change after the full wrapper.

This passes Phase 5's shared-dialog visual exit gate on the current tree. It
does not close #794 or the broader M2 workflow. Follow-up reports remain #861
(reported HiDPI icon blur; exact scale not isolated,
`indicator-info-modern.png` is 16×16), #862 (Japanese Change Working Time
dialog still has an English button), and #863 (audit unintended blank space
across all application windows, coordinated with #724).

#### U-30 Phase 6 watchdog budget audit (2026-10-19)

Static review found the Windows GUI workflow's 110-minute test-step and
120-minute job limits were below the configured worst case for `full`: a
45-minute functional watchdog plus five additional 20-minute visual legs is up
to 145 minutes (Japanese 100% visual coverage is included in the full run).
The test-step limit is now 175 minutes and the enclosing job limit is 210
minutes, which allows the configured serial suite to finish and leaves 35
minutes outside the GUI step for build/setup and failure diagnostics. The
runner still captures a screenshot and terminates the Gradle process tree on an individual timeout;
workflow failure handling captures Java thread dumps and always uploads GUI
artifacts. `git diff --check` passed after this workflow-only adjustment. The
gate audit confirmed the screen dimensions are only read/logged and the
resolution-changing workflow step is absent; no resolution-changing test was
run.

The exact full gate command, `.\.github\scripts\run-gui-acceptance.ps1 -Suite full`,
completed successfully at 2560×1440. Its Japanese 100% full functional leg ran
209 GUI tests; the five additional locale/scale legs (ja/125%, ja/150%,
en/100%, en/125%, en/150%) ran 96 tests each. Every leg had 0 failures and 0
errors, with one documented conditional skip each. The six legs' JUnit bundles
and logs are in `modules/microproject_ui/build/reports/guiTest-artifacts/`.
The first full attempt exposed a fixture defect: the physical Create Resource
Pool test did not install the application's `SwingUiDispatcher`, so its
asynchronous test callback constructed Swing components on
`microproject-callback-dispatch-0`. The fixture now installs the same Swing
dispatcher as `StartupFactory` and resets it in teardown. The focused class
passed all 3 physical GUI tests, then passed again inside the complete full
gate; the gate's deferred-exception scan also passed. This was a test setup
defect, not evidence of a production application exception. The exact-head
installed-layout launch check also succeeded with
`.\scripts\run_projectlibre.ps1 -SkipBuild -LogRoot .\build\logs\projectlibre-phase6`:
the Java process was responsive, the main window title was `microProject`, and
the Japanese welcome dialog appeared. The screenshot is
`build/logs/projectlibre-phase6/20261019-025702/installDist-launch.png`; the
launcher and app logs are in the same directory. The app was closed after the
capture. This verifies startup only; manual interaction with a project opened
from the installed layout and the remaining Phase 6 review remain open. The
welcome dialog still says `ProjectLibre へようこそ`; this was recorded as
[Issue #865](https://github.com/tetsuji16/ProjectLibre/issues/865).

The installed-layout project interaction was then completed without changing
display resolution. A new blank project was saved as
`build/logs/projectlibre-phase6-manual/Codex verification project.mpo`, given
one task named `Verify persistence`, saved with Ctrl+S, closed, and reopened
through the app's Open flow. The task name and its rendered row were present
after reload. This confirms a basic installed-layout open/edit/save/reload path;
it does not close the broader M2.1 acceptance gaps. The active desktop remained
at 2560×1440 throughout.

Current-tree Phase 6 rerun (2026-10-19): the direct Japanese/UI-scale-1 full
GUI task passed 211 tests (0 failures/errors, 1 existing high-DPI-only
conditional skip). The strict matrix wrapper
`.\.github\scripts\run-gui-acceptance.ps1 -Suite full` then stopped in its
first Japanese/100% functional leg when Windows `ShellExperienceHost` showed a
`新しい通知` toast overlapping the full-screen test window. The contention
guard stopped Robot input; the leg recorded 149 tests with 45 failures caused
by `GUI_ENVIRONMENT_CONTENDED`, and no retry was applied. The five additional
locale/UI-scale legs did not run, so the current-tree six-leg matrix remains
unverified. The direct full task and strict wrapper both used the existing
display resolution; no resolution change was made. This is environmental
interference evidence, not a product behavior failure.

Current-tree visual-only rerun (2026-10-19):
`.\.github\scripts\run-gui-acceptance.ps1 -Suite visual` completed all six
visual legs after the Japanese/100% visual baseline passed. Each leg ran the
12 configured visual acceptance classes and reported 96 test cases, 0 failures,
0 errors, and 1 existing high-DPI-only conditional skip. The legs were ja/100%,
ja/125%, ja/150%, en/100%, en/125%, and en/150%. Only the application UI scale
and locale varied; the display remained 2560×1440 and no screen-resolution
change was made. The strict full gate remains incomplete because its separate
full functional leg encountered the Windows notification contention recorded
above. This visual result is current-tree evidence for the visual matrix only;
it does not close the remaining recovery-sequence phases or #794.

Strict full-gate recovery rerun (2026-10-19):
`.\.github\scripts\run-gui-acceptance.ps1 -Suite full` completed successfully
on the same current tree after the earlier notification-contended attempt. The
Japanese/100% full suite passed 211 tests (0 failures, 0 errors, 1 permitted
high-DPI-counterpart skip). The other five locale/UI-scale legs each passed 96
visual tests (0 failures, 0 errors, 1 permitted complementary skip), with no
contention marker or overlap candidate in their per-attempt artifacts. The
runner used the existing 2560×1440 desktop; only application UI locale/scale
varied and no screen-resolution change was made. This supersedes the earlier
statement that the six-leg matrix was unverified. The run predates the newly
identified primary-window promotion focus-listener gap, so the full gate must
be rerun after that lifecycle fix before it can serve as final-head evidence.

Primary-window promotion focus recovery (2026-10-19): issue #867 records the
reproducible focus mismatch found during #794 testing. The primary window now
resolves the current document when its native focus callback fires, and native
close of the primary document closes that document while independent projects
remain, allowing the existing promotion workflow to run. A physical Robot
journey failed against the old captured-document callback, then passed after
the fix: native primary close, activate the other secondary, refocus the
promoted primary, and assert both active-frame sources, title, and per-frame
active state. The targeted run used the existing display resolution. The full
six-leg strict gate then ran against this source state: the Japanese/100%
functional leg recorded 212 cases (0 failures, 0 errors, 1 permitted skip),
and each of the five locale/UI-scale visual legs recorded 97 cases (0 failures,
0 errors, 1 permitted skip). The promoted-primary Robot case is present and
passed in the full functional JUnit report. All six per-attempt artifact
directories have no environment-contention or overlap markers. The runner
reported the unchanged 2560×1440 desktop; no display-resolution test or setting
change was used. `build installDist` passed before the strict gate. The
preceding `clean build installDist` attempt could not remove a prior GUI
monitor log held open by a process, so the successful build was rerun without
`clean`; the suite's `installDist` task was up to date for all six legs.

#### U-31 PR #855 resource Key1+Key2 locale/scale evidence (2026-10-19)

Ran the physical `ResourceHeaderSortingGuiAcceptanceTest` route at English UI
locale with UI scales 125% and 150%; the screen stayed at 2560×1440 and no
resolution-changing step was used. Each focused run passed both tests with 0
failures, 0 errors, and 0 skips. The physical Sort Order journey verifies Name
ascending plus Standard Rate descending breaks a Name tie, then Workspace
save/restore preserves both keys while resource model order stays unchanged.
The second test verifies physical Standard Rate editing, Undo/Redo and MPO
round-trip. JUnit and stdout/stderr evidence are under
`modules/microproject_ui/build/reports/guiTest-artifacts/`, in the
`focused-com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest-en-1-25-*`
and `...-en-1-5-*` artifacts. stderr contained Java native-access warnings
only; no deferred/unhandled exception was reported. This fills the two missing
English scale legs for this focused route; it does not complete other M2.1 or
M2 package rows.

Exact commands:

```powershell
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest -Locale en -Scale 1.25
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest -Locale en -Scale 1.5
```

#### U-32 #414 Gantt wheel ownership (2026-10-19)

`TaskTableGanttGridGuiAcceptanceTest.physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep`
passed at ja/100% using a physical Robot route at the unchanged 2560×1440
desktop resolution. Plain wheel advances the synchronized Gantt/table vertical
position by exactly one five-row owner step without changing horizontal
position; Shift+wheel advances only the Gantt horizontal position by one unit;
Ctrl+wheel changes the Gantt timescale index exactly one step without moving
the vertical position. The synchronizer now registers one wheel listener on
each synchronized `JScrollPane` and removes it from that same pane on
deactivation; child views and headers no longer receive duplicate
registrations. `ScrollPaneSynchronizerTest` verifies single ownership,
idempotent activation, and balanced detach/reactivation. The synthetic
`GanttWheelZoomTest` dispatch route now targets the owning scroll pane, while
preserving the original Gantt-relative pointer location and modifiers. The
focused UI unit tests passed 20 tests; the physical U-32 gate completed 1 test
with 0 failures/errors/skips. Its stderr and deferred GUI exception scan
passed (stderr contains only the Java native-access warning and deprecation
note). Artifacts are
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep-ja-1-0.{stdout.log,stderr.log,failure.png}`.
The first test attempt exposed an incorrect test assumption that horizontal
Gantt motion must also move the task table's independent horizontal viewport;
that assertion was removed. No desktop resolution was changed. The suite
verifies the three wheel routes for this fixture, not the remaining #414
consolidation and acceptance rows.

Exact command: `.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep -Locale ja -Scale 1.0`.

Final-head Phase 6 strict rerun after wheel-listener ownership consolidation
(2026-10-19): `.\.github\scripts\run-gui-acceptance.ps1 -Suite full`
completed with the desktop at the existing 2560×1440 resolution. The
Japanese/100% full functional leg recorded 212 cases (0 failures, 0 errors,
1 permitted skip); each of the five Japanese/English 100/125/150% visual legs
recorded 97 cases (0 failures, 0 errors, 1 permitted skip). The physical
Gantt wheel contract is present in the functional JUnit report and passed.
All six per-attempt directories have no environment-contention or overlap
markers. The previous full-gate entry predates this wheel consolidation; this
run supersedes it as current-tree evidence. No screen-resolution change was
made.

#### U-33 #414 Gantt drag auto-scroll (2026-10-19)

`GanttBarDateDragGuiAcceptanceTest.robotDragToChartEdgeAutoScrollsAndPersistsMovedTaskDate`
uses the existing physical bar-drag route with an extended chart range. At
ja/100% and the unchanged 2560×1440 desktop resolution, Robot drags to the
visible chart edge, verifies horizontal viewport movement and a visible moved
bar, checks the later task start, then verifies one Undo, one Redo, and MPO
save/reload of the exact moved start. The focused journey passed; the full
fixture class then passed 7 tests with 0 failures, errors, or skips. The GUI
runner's deferred-exception scan passed; stderr contains only the known Java
restricted-native-access warning, with no stack trace. The final screenshot is
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest-ja-1-0-a77676e698804fb2b9c2132ed2cab63d/gantt-edge-auto-scroll.png`.
The chart-bar hit-region fixture helper now searches the complete chart width
so long-range timelines remain testable. No production change was needed.

Exact commands:

```powershell
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotDragToChartEdgeAutoScrollsAndPersistsMovedTaskDate -Locale ja -Scale 1.0
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest -Locale ja -Scale 1.0
```

#### U-34 日本語IME再変換の環境診断 (2026-10-19)

U-26の既存日本語セル再変換を確認するため、物理Robotで既存文字列
`とうきょう`を選択し、focused Swing editorへWindows `VK_CONVERT`を送った。
Convertのeditor-local bindingとアプリActionは各1回実行され、`InputContext`
のlocaleは`ja_JP`だったが、`isCompositionEnabled()`は`false`、
`reconvert()`は`UnsupportedOperationException`、InputMethodEventは0件で、
文字列は変化しなかった。稼働中のGUI test workerのWindows HKLは
`0x04110411`（Japanese）だった。設定変更や画面解像度の変更はしていない。
この結果はアプリのキーbinding漏れではなく、この実行環境のJava/Windows入力
メソッドが再変換を提供しなかったことを示す。ただし、他の日本語IMEでも同じ
かは未確認であり、機能合格とはみなさない。診断用のsource instrumentationと
一時testは除去済み。追跡Issueは[#866](https://github.com/tetsuji16/ProjectLibre/issues/866)。

通常のU-26 GUI test classは次で成功した（1 test、failure/error/skip 0）。

```powershell
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest -Locale ja -Scale 1.0
```

診断JUnit XMLは
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest.temporaryPhysicalImeProbe-ja-1-0-junit/TEST-com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest.xml`、
再実行ログは
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest-ja-1-0.stdout.log`。
後者の成功はIME再変換を検証していない。

Current-tree module verification was rerun sequentially after an initial run
collided with a GUI Gradle worker and failed during test compilation. The
authoritative rerun command was:

```powershell
.\gradlew.bat :microproject_core:test :microproject_ui:test :microproject_application:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1
```

It completed with `BUILD SUCCESSFUL` in 5 minutes. JUnit XML totals: core
1,057, UI 1,154, application 37, exchange 282; all modules had 0 failures,
0 errors, and 0 skipped tests. No GUI/display-resolution test ran as part of
this command.

### #414 / #794 M2.1 — spreadsheet corner listener lifecycle

`SpreadSheetCorner` registered itself with the task spreadsheet row and column
selection models but was not detached when `CommonSpreadSheet.cleanUp()` ran.
`CommonSpreadSheet.cleanUp()` now delegates to the corner, which removes both
registrations. `SpreadSheetCornerLifecycleTest.cleanupRemovesCornerFromBothSelectionModels`
checks the two listener lists and verifies that selection changes after cleanup
do not call the corner. Verified on the current working tree with
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.*" --console=plain`
(BUILD SUCCESSFUL) and `git diff --check`. This closes only that listener-detach
gap; the #414 net-reduction/input-route review and installDist manual acceptance,
and the remaining M2 workflow rows, remain open.

The same lifecycle audit found that every `Gantt.updateUI()` installed a new
`GanttInteractor` while the old interactor stayed registered on the Graph as a
mouse and mouse-motion listener. `GraphUI.uninstallUI()` now disposes the old
interactor, and `Graph.cleanUp()` detaches the active one before closing its
model. `GraphInteractorLifecycleTest` asserts one active interactor after three
UI replacements and no interactor listeners after repeated cleanup. The full
`:microproject_ui:test --console=plain` passed, followed by
`:microproject_ui:installDist --console=plain`; both ran on the current source.
The `installDist` payload was regenerated, but its manual desktop launch and
interaction check remains pending because this session has no targetable native
app window. This closes the identified listener-retention gap, not #414's
complete input-route inventory or #794's wider M2 acceptance.

The missing row-header physical route was also added to
`TaskTableGanttGridGuiAcceptanceTest` with the regular JTable UI delegate still
installed. The test starts with zero selected tasks, right-clicks an unselected
task row with Robot, and verifies stable task identity, full-row selection,
popup invoker/row/column and exactly one visible event. The focused Japanese
100% Windows GUI run passed with
`.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalRowHeaderPopupSelectsTheClickedTaskAndOpensOnce" '-PguiTestLocale=ja' '-PguiTestUiScale=1' --max-workers=1 --no-daemon --console=plain`.
This is physical route/selection/popup evidence; it does not claim a popup
mutation or Undo result because the lightweight GUI fixture's DocumentFrame
does not own the fixture table.

Current-worktree recheck on 2026-10-19 rebuilt `installDist` through the GUI
task and reran the same single Robot route with
`-PguiTestLocale=ja -PguiTestUiScale=1`; it passed. The route used the existing
desktop display and did not change its resolution. This recheck does not cover
the platform-specific BUTTON1 Control-click case, which remains pending on
macOS.

The same lifecycle audit found that `NetworkSVGRenderer` registered itself on
an offline print cache and neither released the listener nor closed the
renderer-owned filtered cache when its pageable ended. `cleanUp()` now removes
the listener and closes that exact cache; reinitializing with the same cache
does not duplicate registration, and safe print copies remain paintable. Print
preview close, print completion/cancel/error, export completion/cancel/error,
and export chooser cancellation all release the pageable. Export jobs use
`Job.addCompletionRunnable` so cleanup also runs when cancellation happens
before the export runnable starts. `NetworkSVGRendererLifecycleTest`,
`ExportJobSchedulerTest`, and `JobExceptionHandlerTest` cover the ownership,
idempotence, copy, and completion paths. The focused tests and full
`:microproject_ui:test` plus `:microproject_core:test --tests
"com.microproject.job.JobExceptionHandlerTest"` passed on the current working
tree. This closes the confirmed offline print-cache retention gap; it does not
complete #414's net-reduction audit or #794's broader M2 gates.

### #414 recovery sequence — Phase 3 view initialization evidence

The Task/Resource Usage journey uses a one-day assigned task and asserts
populated left and time-spreadsheet models for both views. The Timesheet
journey supplies an assignment, checks populated rows and unchanged work, then
sends Robot Escape and verifies the dialog closes while the active view and
Undo availability remain unchanged. `UsageDetailView.resolveDistributionFields`
retains a nonempty configured array and otherwise seeds the time grid from
assignment fields; `UsageDetailViewTest` covers both behaviors. Focused unit
and physical Task/Resource Usage Robot checks passed at Japanese/100% scale.

`ResourcePoolInitialViewGuiAcceptanceTest` now isolates persisted ribbon mode
with `ALWAYS_SHOW` and initializes the direct test shell with a JobQueue and
Swing alert presenter. The physical Create Resource Pool click registers a new
master-pool project, opens ResourceView, and exposes a populated spreadsheet.
The focused Japanese/100% Robot check passed (1/1). These routes satisfy the
local Phase 3 initialization/content/Escape gate; full GUI regression is
running on the current source. This does not imply overall M2 acceptance.

Undo action readiness update (2026-10-13): core/UI clean test run 49017 passed, and focused `RibbonButtonBehaviorTest` run 59496 passed after removing temporary test diagnostics. The root cause was a document-frame Undo action refresh during queued history delivery: `canUndo` was temporarily suppressed and no later edit callback refreshed the action. `EditSupport.historySettled()` now refreshes the frame only after committed history drains. A later GUI493 failure exposed a separate Redo ordering defect: assignment after-detail state replayed before the deferred replacement-connection edit, so the replacement was not attached and its detail restore was skipped. `EditSupport.postFinalEdit()` now places that snapshot after ordinary/deferred edits; the exact two-day shortcut focused tests passed in isolated run 1499 after restoring coordinator worktree changes; full core/UI module tests passed in run 63932 on the then-mixed workspace source. GUI493 ran against the earlier head; physical verification of the Redo fix on a new head remains pending. Issue #409 remains partial: shared-pool/multi-project writes and the remaining public/legacy writer inventory are open.

Shared resource-pool boundary update (2026-10-13): ResourcePool membership/ID mutations, shared-pool merge rewires, assignment creation/replacement, and portfolio close membership now use the shared coordinator; merge publishes task plus source/destination pool refreshes after detaching/switching/connecting, with independent pool revisions and object-identity membership. A failed reconnect restores assignment/task detail and association, then emits a conservative committed refresh and revision; it does not create an Undo edit. Current JDK 25 verification passed: :microproject_core:test :microproject_application:test :microproject_ui:test --console=plain --max-workers=1 --no-daemon (BUILD SUCCESSFUL, 1m53s). Existing CI Build1127 (38376421023) and GUI495 (38376420934) passed on base d61891de4; they predate these current uncommitted changes and are not final-head evidence. Remaining: audit other public/legacy writers, coherent immutable publication snapshots, persistence plus physical Undo/Redo acceptance on the final head. Issue #409 stays partial.

Assignment connect/remove boundary update (2026-10-13): Published initialized-project `newAssignment`, `connect`, single-assignment `remove`, and mixed-project collection removal now enter the shared model command boundary; uninitialized null-source import assembly remains silent. Service-owned creation/deletion Undo edits are recorded once, nested default-assignment maintenance inherits the outer Undo suppression, and collection removal restores assignment detail/schedule on Undo/Redo. The failed shared-pool reconnect regression also verifies the failed operation adds no Undo edit. JDK 25 `AssignmentServiceTest` passed, followed by `:microproject_core:test :microproject_application:test :microproject_ui:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` (BUILD SUCCESSFUL, 2m28s). This covers the current working tree, not physical GUI acceptance or every #409 writer; remaining public writers, renderer snapshots and final-head GUI Undo/Redo evidence keep #409 partial.

Dependency mutation boundary update (2026-10-13): `DependencyService` create/connect/remove/set-fields and bulk link/removal now enlist every endpoint owner and master event document through the shared coordinator. Each owning project receives one revision/batch only when a dependency fact affecting that project changed; legacy service overloads retain predecessor-project Undo ownership; UI commands can explicitly store history in the initiating project, and listener callbacks run after all scopes release. Initialized null-source writes publish a live refresh without Undo; uninitialized null-source import assembly remains silent. Dialog, Undo-edit and spreadsheet callers no longer issue a second update after `setFields`. Cross-project type, 3-project bulk link/Undo/callback, filtered candidate, same-ID ownership and null-source lifecycle regressions passed. JDK 25 combined core/UI/application/exchange tests passed on this working tree with `:microproject_core:test :microproject_ui:test :microproject_application:test :microproject_exchange:test --console=plain --no-daemon --max-workers=1` (BUILD SUCCESSFUL, 2m04s); the focused `DependencyServiceTest` also passed. Build1129 run 38383528544 and GUI497 run 38383528582 passed on base `8eb165bc` before this dependency-family patch; they are not evidence for the current head. #409 remains partial: other public/legacy mutation families, coherent immutable render snapshots, and final-head GUI Undo/Redo acceptance remain open. No milestone or release gate is closed by this family update.

Calendar live-commit boundary update (2026-10-13): Calendar dialog saves and `CalendarEdit` Undo/Redo delegate to `CalendarService.commitCalendars` / `commitCalendar`; staged scratch/import writes remain direct. Owner discovery and calendar history reservation happen under the global coordinator, and history delivery waits until every project/pool publication scope releases. Pre-apply failures discard the staged edit; a committed observer failure retains the correctly ordered Undo item. Calendar owner membership uses a transient identity set and immutable identity snapshot; `ResourceImpl.setWorkCalendar` now moves the actual resource identity between those memberships under the coordinator. Pool refreshes target affected ResourceImpl identities rather than only the pool document; semantic equality of a scratch clone is a no-op. The service completes each affected project schedule before detaching revisions and publishing view notifications. Calendar ownership is a document-derived product decision, not observed Microsoft Project parity. The numeric regression pins a task and assignment to Friday 2026-10-16 08:00, asserts the literal Monday 2026-10-19 17:00 finish before a staged Saturday 08:00–12:00 / 13:00–17:00 addition, then asserts the literal Saturday 2026-10-17 17:00 finish on commit, Undo, and Redo without caller-side recalculation. `MpxExportTrackingTest.committedSaturdayCalendarRecalculatesAndPersistsTaskAssignmentThroughMpo` checks raw native MPO `content.xml` and reloads the working Saturday, finish timestamps, and assignment work/actual/remaining values (16h/2h/14h). Focused handle 7591 and full `verifyArchitectureBoundaries` + core/UI/application/exchange handle 85038 passed on JDK 25 (BUILD SUCCESSFUL, 2m09s). No local physical GUI run was made; CI GUI acceptance is pending. #409 remains partial, with other public/legacy writers, immutable render snapshots and final-head physical Undo/Redo acceptance open.
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
`.\gradlew.bat :microproject_ui:installDist :microproject_ui:guiTest --tests 'com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalViewSortOrdersTasksByStartDateAndKeepsGanttRowsAligned' --tests 'com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources' -PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --no-daemon --console=plain` (BUILD SUCCESSFUL; both tests passed). Later PR #815/#827 adds physical ascending-start and descending-finish, multi-key, and permanent-order evidence; empty/missing-date, flat Keep Outline, and filter/group projection cases are now covered by the tests summarized above. Broader resource sort parity remains open. Gradle evidence is in
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

Successor task-calendar FS boundary update (2026-10-19): added a fixed-date
zero-lag case to `TaskLinkAndBarScheduleTransitionsTest`. A one-workday
predecessor on the ordinary project calendar finishes Tuesday 2026-11-03
17:00; the successor's task calendar marks Wednesday 2026-11-04 nonworking,
so its one-workday successor starts Thursday 2026-11-05 08:00 and finishes
17:00. The unrelated task remains Tuesday 08:00–17:00. This is a
document-derived compatibility decision, not an empirical MSP observation:
Microsoft documents FS boundary/task-calendar scheduling and task calendar
working times superseding the project calendar in [How Project schedules
tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes)
and [Task calendar (Task field)](https://support.microsoft.com/en-us/project/task-calendar-task-field).
The focused test class passed 47 tests (0 failures, errors, or skips) with
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest" --rerun-tasks --console=plain --no-daemon --max-workers=1`;
`git diff --check` passed. No GUI, Robot, or display-resolution test ran.

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

Dependency bulk-removal failure update (2026-10-18): `remove(List<Dependency>)`
now snapshots the touched adjacency/sentinel lists and dirty flags before the
atomic command. If unlinking fails partway through, it restores those graph
facts and discards observer facts and Undo edits created by the failed command;
the existing legacy `runModelCommand` failure contract is unchanged. The
injected sentinel-update failure regression checks graph restoration, no
revision advance, unchanged pending object/schedule observer facts, no observer
publication, and preserved Undo history. Both
`./gradlew.bat :microproject_core:test --tests
"com.microproject.pm.dependency.DependencyServiceTest" --console=plain` and
`./gradlew.bat :microproject_core:test --console=plain` passed on the current
working tree. This is one failure-atomicity slice; remaining mutation-family
failure injection and final-head physical GUI acceptance remain open.

Spreadsheet first-key input update (2026-10-18): U-26 now includes a physical
Robot path that selects the percent cell and types `10` without F2 or a
programmatic edit start. It asserts that the first digit starts the editor,
the complete value reaches it, and the committed task value and rendered `10%`
agree. `:microproject_ui:compileGuiTestJava` and the focused
`U26SpreadsheetInputTransactionGuiAcceptanceTest` GUI journey passed on the
current Windows desktop. The desktop display resolution was not changed.
Windows 11 VM evidence and the remaining U-26 cases remain open.

U-26 structural persistence integration update (2026-10-19): the existing
physical input journey now applies the parent→outdent transition to the same
task whose duration, start date, and progress were entered through Robot. After
MPO reopen it checks task identity/root parent, raw duration, 100% progress,
entered start date, zero remaining duration, project duration defaults,
per-task display unit, and duration/remaining/progress rendered text. The
focused Japanese/100% physical journey passed 1 test with 0 failures/errors/
skips; no desktop resolution was changed. The test re-enters 100% after the
duration format conversions because those edits reset progress in this fixture.
The full GUI gate passed on the same product code before this test-only
assertion strengthening; the focused journey is the evidence for the updated
U-26 test via
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest -Locale ja -Scale 1.0`.
Japanese IME reconversion remains an environment-dependent open investigation
tracked by #866.

Constraint/dependency boundary update (2026-10-19): added the M2.2 SNET + FS
boundary regression to `TaskLinkAndBarScheduleTransitionsTest`. It covers a
predecessor finishing before and after the successor's Start No Earlier Than
date, checks the scheduled dates and hard-constraint/dependency-conflict
diagnostics, and verifies link Undo/Redo. This is a document-derived Project
desktop compatibility rule, not a measured MSP run, based on Microsoft's
[How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes).
The shared test fixture now gives each project a scratch copy of its default
working calendar; previously, the dated-nonworking-exception case modified a
calendar shared by later projects and made test order affect scheduled dates.
The complete transition class passed (42 tests), followed by the full core
suite (1,059 tests; 0 failures, errors, or skips) using
`.\gradlew.bat :microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1`.
This closes only this M2.2 boundary slice; the remaining recovery phases and
physical GUI gates for issue #794 remain open. No test changed the desktop
resolution.

Bulk dependency-connect failure update (2026-10-19): `connect(List, …)` now
restores the full touched dependency adjacency/sentinel graph if a later link
fails, and runs through the atomic project command boundary so pending object
and schedule events, revisions, and Undo edits roll back with the graph. The
new `failedBulkConnectRestoresGraphEventsRevisionAndUndoState` case injects a
failure while connecting the second link and compares all endpoint lists,
revision, pre-existing pending events, observer notifications and Undo/Redo
availability against the pre-call state. Focused
`DependencyServiceTest` passed (31 tests); the full core suite then passed
(1,060 tests; 0 failures, errors, or skips) with
`.\gradlew.bat :microproject_core:test --rerun-tasks --console=plain --no-daemon --max-workers=1`.
`git diff --check` passed. No GUI or resolution-changing test ran. This covers
one bulk-link failure path; single-link and other mutation-family failure
atomicity plus MPO and physical GUI acceptance remain open.

Single dependency failure update (2026-10-19): `newDependency`, `connect(Dependency)`
and `remove(Dependency)` now restore touched adjacency/sentinel lists and dirty
state on runtime failure, using the atomic project command boundary. The
deserialization `initDependency` path also restores the dependency endpoints
and graph on runtime failure. New failure-injection cases cover create,
connect, remove, and deserialization initialization; the focused
`DependencyServiceTest` passed (35 tests). After this init fixture, the full
core suite passed (1,064 tests; 0 failures, errors, or skips) with
`.\gradlew.bat :microproject_core:test --console=plain --no-daemon --max-workers=1`.
`git diff --check` passed. GUI and display-resolution tests were not run.

After the dependency-service changes, the caller modules also passed:
`.\gradlew.bat :microproject_application:test :microproject_ui:test --console=plain --no-daemon --max-workers=1`
— application 37 tests and UI 1,154 tests, all with 0 failures, errors, or
skips. These headless module tests do not replace physical GUI evidence.

GUI gate before the single-operation atomicity update (2026-10-19):
`.\.github\scripts\run-gui-acceptance.ps1 -Suite full` completed with the
desktop at 2560×1440 throughout. The Japanese 100% functional leg reported 210
tests (209 passed, 1 paired high-DPI counterpart skip); each of the five
additional ja/en × 100/125/150% visual legs reported 96 tests (95 passed, 1
paired functional counterpart skip). All six legs had 0 failures and 0
errors. The runner's deferred GUI exception/error scan completed without
failure. No display-resolution-changing step or test ran. Per-leg stdout,
stderr, and JUnit bundles are in
`modules/microproject_ui/build/reports/guiTest-artifacts/` under
`functional-full-ja-100-*` and `visual-{ja,en}-{1.0,1.25,1.5}-*`. This records
the GUI release-readiness gate on the current working tree; it does not finish
the remaining M2.1–M2.4 acceptance rows or the full release package gate.

Constraint/dependency boundary update (2026-10-19): added the M2.2 SNET + SS
boundary regression to `TaskLinkAndBarScheduleTransitionsTest`. Two isolated
project fixtures cover the predecessor starting before and after the
successor's Start No Earlier Than date. The test checks exact predecessor,
successor, and unrelated-task dates, the zero-lag SS predecessor-start
boundary, hard-constraint/dependency-conflict diagnostics, and link Undo/Redo.
This is a document-derived Project desktop decision, not a measured MSP run,
based on Microsoft's [How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes).
The focused transition class passed (44 tests), and the full core suite passed
(1,066 tests; 0 failures, errors, or skips) with
`.\gradlew.bat :microproject_core:test --console=plain --no-daemon --max-workers=1`.
`git diff --check` passed. No GUI or display-resolution tests ran.

Dependency MPO persistence update (2026-10-19): expanded
`MpoFileImporterTest.mpoSavesAndReplaysDependencyOperations` from link-presence
only to round-trip field state. The fixture first saves/reopens FS with zero
lag, then changes the dependency to SS with an 8-hour lag and verifies Undo
restores FS/zero while Redo restores SS/eight hours. It saves and reopens the
redone project, checks both endpoint adjacency lists, dependency type/lag, and
successor start/finish after recalculation. The focused exchange test passed
(1 test); `git diff --check` passed. No GUI or display-resolution test ran.
This closes one dependency persistence slice; other M2.2 families and physical
GUI acceptance remain open.

Dependency deletion MPO update (2026-10-19): extended
`MpoFileImporterTest.mpoSavesAndReplaysDependencyOperations` to remove the
reopened SS/8h dependency with an undoable event source. The test verifies both
endpoint lists after removal, Undo restoration, Redo removal, a subsequent
`dependency.delete` operation, and deletion persistence after another MPO
reopen. The reloaded endpoint lists remain empty and recalculated successor
dates match the saved deleted-state dates. The focused test passed (1 test,
0 failures/errors/skips) with
`.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoSavesAndReplaysDependencyOperations" --console=plain --no-daemon --max-workers=1`;
`git diff --check` passed. No GUI or display-resolution tests ran.

Gantt keypad zoom route update (2026-10-19, issue #414): removed the
component-local Ctrl+Numpad Divide/Multiply registrations from `Gantt` and
registered both on the document root pane. Dispatch calls `GanttView.zoomIn`
or `zoomOut` only while the active top view is Gantt or Tracking Gantt; other
views and missing documents are no-ops. The focused UI suite passed 14 tests
(6 `GanttWheelZoomTest`, 8 `MicrosoftShortcutsRootPaneTest`, 0 failures) with
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.gantt.GanttWheelZoomTest" --tests "com.microproject.pm.graphic.frames.MicrosoftShortcutsRootPaneTest" --console=plain --no-daemon --max-workers=1`, and
`.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` passed. The focused Robot journey passed
1/1 via `.\gradlew.bat :microproject_ui:guiTest --max-workers=1 --console=plain -PguiTestLocale=ja --tests
"com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotCtrlNumpadDivideAndMultiplyZoomActiveGanttFromGanttAndSpreadsheetFocus"`
after its `installDist` dependency regenerated the runnable layout. It
physically pressed Ctrl+Numpad Divide and Ctrl+Numpad Multiply in the same
journey, observed exactly one document-root action dispatch for each key, and
verified the zoom index moved in opposite directions. Task start/end dates,
task-table viewport position, and Gantt vertical viewport position stayed
unchanged after both operations. No display resolution or GUI scale was changed.
The expanded focused Robot rerun passed 1/1 with the same GUI acceptance
command and test selector listed above. It physically pressed Divide and
Multiply with both Gantt and `SpreadSheet` holding focus, observing exactly one
root-pane action per key; zoom moved in and back out, while task dates, task
table viewport, and Gantt vertical viewport stayed unchanged. The cause of the
spreadsheet-focus failure was NetBeans `ETable.processKeyBinding`, which handles
Ctrl+`*` directly from the key character and consumes it on both its
`WHEN_FOCUSED` and `WHEN_ANCESTOR_OF_FOCUSED_COMPONENT` passes. `SpreadSheet`
now yields only this exact numpad chord when the active root-pane action is
enabled for a Gantt view; outside Gantt the inherited ETable column-selection
behavior remains active and is covered by
`SpreadSheetEditingBindingsTest.disabledRootPaneGanttZoomOutLeavesEtableCtrlMultiplyColumnSelectionAvailable`.
Focused UI shortcut/zoom tests passed, GUI test sources compiled, and
`git diff --check` passed.

### M2.2 External project refresh publication boundary (#794/#409, 2026-10-19)

External task refresh now stages matched values before applying them under one atomic target-project mutation. Rollback combines `ProjectTaskSnapshot` (schedules, dates, percent, snapshots, and dirty flags) with explicit name, notes, and predecessor restoration. Observers and hierarchy repaint receive notifications only after a complete successful refresh; injected failure coverage verifies restoration, unchanged revision, and suppressed publication. Display resolution was not changed.
After the fix, the focused headless suite passed 17 tests (6
`GanttWheelZoomTest`, 8 `MicrosoftShortcutsRootPaneTest`, and 3
`SpreadSheetEditingBindingsTest`) with `.\gradlew.bat :microproject_ui:test
--tests "com.microproject.pm.graphic.spreadsheet.SpreadSheetEditingBindingsTest"
--tests "com.microproject.pm.graphic.gantt.GanttWheelZoomTest" --tests
"com.microproject.pm.graphic.frames.MicrosoftShortcutsRootPaneTest"
:microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1`.
After the spreadsheet-focus routing fix, the full
`.\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1`
suite passed 1,159 tests with 0 failures, errors, or skips. `git diff --check`
passed.

Full GUI gate rerun (2026-10-19): the first Japanese/100% current-desktop run
reported one timeout in `MasterSubprojectMpoGuiAcceptanceTest` while waiting
for the linked child to materialize; the same case passed alone and with all
four tests in its class. Its load wait now allows up to 20 seconds and reports
whether the project reference appeared on timeout. The exact rerun command
`.\gradlew.bat :microproject_ui:guiTest --max-workers=1 --no-daemon --console=plain -PguiTestLocale=ja -PguiTestUiScale=1`
passed the full GUI task. The linked-child case
passed in that full run. One high-DPI-only ribbon method was skipped by its
existing `guiTestUiScale=1` assumption; this run does not claim the visual DPI
matrix. No display resolution was changed. The targeted linked-child case and
all four tests in its class also passed in focused reruns. `git diff --check`
passed.

SNET + positive FS-lag boundary update (2026-10-19): added two fixed-date M2.2
cases to `TaskLinkAndBarScheduleTransitionsTest` that combine a successor SNET
with a positive one-working-day FS lag across a dated Tuesday calendar
exception. When the constraint is Tuesday 2030-06-04 08:00, the lag boundary
falls on Thursday 2030-06-06 08:00 and wins; when the constraint is Friday
2030-06-07 08:00, the constraint wins. Both cases assert exact successor and
predecessor dates, dependency type/lag, an unchanged unrelated task, diagnostic
state, and link Undo/Redo. This is a document-derived compatibility decision,
not an empirical MSP result, based on Microsoft's documentation that SNET is a
lower bound for an FS-linked task and positive lag delays its successor:
[How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes)
and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
The focused class passed 46 tests with
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest" --console=plain --no-daemon --max-workers=1`;
`git diff --check` passed. This did not change product code or desktop
resolution; other M2.2 mutation families and final-head GUI acceptance remain
open.

Dependency `setFields` rejection atomicity update (2026-10-19): expanded
`DependencyServiceTest.setFieldsRollsBackLagWhenDependencyTypeChangeIsRejected`
to seed pre-existing pending object/schedule events before attempting the
invalid summary-task FF type change. The test now asserts the endpoint graph,
dependency fields, dependency/task/project dirty flags, mutation revision,
pending event counts, and Undo/Redo availability and labels all remain
unchanged after rejection. Focused JDK 25 test passed (1 test, 0
failures/errors) with
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.dependency.DependencyServiceTest.setFieldsRollsBackLagWhenDependencyTypeChangeIsRejected" --console=plain --no-daemon --max-workers=1`;
`git diff --check` passed. No GUI or display-resolution test ran; wider
dependency mutation-family and physical acceptance remain open.

Assignment replacement MPO update (2026-10-19): extended the existing
`AssignmentDialogGuiAcceptanceTest.robotReplaceWithActualWorkPreservesActualsAndSupportsUndoRedo`
physical journey. After Robot Replace and physical Undo/Redo, it serializes the
redone project to MPO bytes and reloads it, resolving the task and both
resources by stable names. It checks the exact two assignment associations,
reloaded task/resource references, preserved original actual work and zero
remaining work, and replacement zero actual work plus the transferred
remaining work. The focused class run passed 2 tests with 0 failures using
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.dialog.assignment.AssignmentDialogGuiAcceptanceTest -Locale ja -Scale 1.0`.
The display remained at 2560×1440; no resolution setting or helper was used.
`git diff --check` passed. The test file's full focus run includes both class
methods because the runner rejects method selectors. Other M2.3 workflows and
final-head GUI gates remain open.

Clear Baseline physical-route update (2026-10-19): added
`TaskInformationRibbonGuiAcceptanceTest.clearBaselineRibbonRouteUndoRedoAndMpoRoundTripPreserveCurrentValues`.
The Robot journey opens Project > Clear Baseline, verifies slot 0 and the
visible Entire Project selection, clears task and assignment baseline values,
and confirms current schedule/work/cost values remain unchanged. Physical
Undo restores both baseline records; Redo clears them again; MPO save/reopen
retains the cleared state and current values. This exercises the documented
whole-project clear route; it does not claim selected-task clear behavior.
Microsoft's [Create or update a baseline or an interim plan in Project
desktop](https://support.microsoft.com/en-us/project/create-or-update-a-baseline-or-an-interim-plan-in-project-desktop)
documents the numbered baseline clear action and whole-project scope. The
focused Japanese/100% GUI test passed (1 test, 0 failures) with
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.clearBaselineRibbonRouteUndoRedoAndMpoRoundTripPreserveCurrentValues -Locale ja -Scale 1.0`;
`git diff --check` passed. No display resolution was changed. Other M2.3
workflows and final-head GUI acceptance remain open.

Proposed-resource assignment leveling physical-route update (2026-10-19):
strengthened `ResourceLevelingDialogGuiAcceptanceTest.proposedResourcesStayOutOfPreviewUntilRobotEnablesThem`.
The physical preview first proves proposed-resource work stays excluded even
after manual tasks are enabled; after the user enables proposed bookings, the
preview identifies the assignment that Robot Apply moves. The fixture uses
manual tasks so assignment-level leveling must preserve both task starts and
finishes; the other assignment and the resource's proposed booking type also
remain unchanged. After closing the modal dialog, physical Ctrl+Z and Ctrl+Y
undo and redo the assignment change. MPO reopen retains the changed assignment
association, leveling delay, exact assignment start, and task dates. A new
`MpoFileImporterTest` regression first reproduced a one-working-day offset
when MSPDI omitted `<Delay>` but stored a nonzero `<LevelingDelay>`: import
inferred the leveling offset as manual delay from `Assignment.Start`, then
applied the leveling delay again. The `MpxAssignmentConverter` import now
clears that inferred manual delay only in this absent-Delay/nonzero-LevelingDelay
case; explicit Delay and LevelingDelay remain independent, covered by the
existing three-hour/one-hour round-trip test. Microsoft's
[Application.LevelingOptions](https://learn.microsoft.com/en-us/office/vba/api/project.application.levelingoptions)
documents `LevelProposedBookings`, and [Distribute project work evenly by
leveling resource assignments](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments)
documents proposed bookings and Level Selection/Level All. This is physical
microProject evidence, not an empirical MSP run. The focused Japanese/100% GUI
test passed (1 test, 0 failures) with
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.dialog.ResourceLevelingDialogGuiAcceptanceTest.proposedResourcesStayOutOfPreviewUntilRobotEnablesThem -Locale ja -Scale 1.0`.
The focused exchange command ran both importer tests: `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripDoesNotDoubleCountAssignmentLevelingDelayWhenManualDelayIsAbsent" --tests "com.microproject.exchange.MpoFileImporterTest.clearingAssignmentLevelingAfterMpoReloadPreservesManualAssignmentDelay" --console=plain --no-daemon --max-workers=1`; both passed. `git diff --check` passed. The runner reported 2560×1440; no display resolution was changed. Other M2.3 workflows and final-head GUI acceptance remain open.

### 2026-10-19 — Assignment Replace task totals

Extended `AssignmentDialogGuiAcceptanceTest.robotReplaceWithActualWorkPreservesActualsAndSupportsUndoRedo` to compare task Work, Actual Work, Remaining Work and Cost with the sum of its assignments before Replace, after Replace, after physical Undo and Redo, and after MPO save/reload. Distinct resource standard rates make Cost aggregation meaningful; the reopened file also verifies the exact two task-resource associations and their references. The focused Japanese/100% GUI class run passed both tests (0 failures) with
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.dialog.assignment.AssignmentDialogGuiAcceptanceTest -Locale ja -Scale 1.0`.
The runner reported 2560×1440; no display resolution was changed. Other M2.3 workflows and final-head GUI acceptance remain open.

### 2026-10-19 — Clear Leveling preserves a prior user assignment split

Added a physical Robot journey that creates a named user gap, verifies that
MPO retains it before leveling, uses Preview and Apply to add a leveling split,
then physically clears leveling and verifies the exact user contour, task and
other-assignment state. Physical Ctrl+Z/Ctrl+Y and a final MPO save/reload also
preserve the expected leveled/cleared states. Microsoft's [Resource leveling
dialog box](https://support.microsoft.com/en-us/project/resource-leveling-dialog-box)
defines Clear Leveling as removing leveling delay and leveling-created splits;
preserving the independent user split is a document-derived product contract,
not an empirical MSP measurement. A stale-preview failure was resolved in the
fixture by recalculating after the live project frame is initialized and before
the dialog captures its preview state; no production code changed. The focused
GUI command passed (1 test, 0 failures):
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.dialog.ResourceLevelingDialogGuiAcceptanceTest.clearLevelingKeepsUserAssignmentSplitAndSupportsPhysicalUndoRedoAndMpo -Locale ja -Scale 1.0`.
Core contour/Undo/Redo and MPO round-trip regressions passed with
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.assignment.AssignmentContourBehaviorTest.assignmentSplitKeepsPersonalContourWorkAndRequestedGapExact" --console=plain --no-daemon --max-workers=1`
and
`.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.manualAssignmentSplitSurvivesMpoSaveAndReloadWithExactWorkAndGap" --console=plain --no-daemon --max-workers=1`.
`git diff --check` passed. GUI locale was Japanese, scale 1.0, and the runner
reported 2560×1440; display resolution was not changed. Other M2.3 workflows
remain open.

### 2026-10-19 — Mark on Track uses an explicit status date and persists exact progress

Strengthened the physical ribbon journey with a three-day task, an explicitly
set interior Status Date, and pre-existing 25% progress. The expected value is
derived from the task's effective work calendar. Robot confirms the command's
exact model result, visible task-row refresh, physical Ctrl+Z/Ctrl+Y, and MPO
reopen with the exact Status Date and percentage. The first run exposed a real
MPO defect: MSPDI's integer `PercentComplete` restored 2/3 as 0.67. MPO now
stores the exact leaf-task fraction in its existing task-schedule extension;
legacy archives with no fraction keep their prior behavior, malformed values
are rejected, and MPP/POD import is unchanged. Operation-log replay also uses
the precision-preserving task setter. This is a document-derived product
behavior based on Microsoft's [Update work on a project](https://support.microsoft.com/en-us/project/update-work-on-a-project)
and [Actual Finish fields](https://support.microsoft.com/en-us/project/actual-finish-fields),
not an empirical Microsoft Project run.

Verification passed:

- `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.TaskProgressServiceTest" --console=plain --no-daemon --max-workers=1` — core progress service cases.
- `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoTaskPercentCompleteOperationReplayPreservesFractionalPrecision" --tests "com.microproject.exchange.MpoFileImporterTest.leafTaskPercentCompleteSurvivesMpoSaveReloadAndNoOpSave" --tests "com.microproject.exchange.MpoFileImporterTest.mpoTaskScheduleWithoutPercentCompleteRetainsLegacyLoadBehavior" --tests "com.microproject.exchange.MpoFileImporterTest.mpoTaskScheduleRejectsInvalidLeafPercentCompleteMetadata" --console=plain --no-daemon --max-workers=1` — 4 passed, 0 failed.
- `.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest -Locale ja -Scale 1.0` — 56 passed, 0 failed. This class run covers additional GUI routes because the runner accepts class filters only.
- `git diff --check` passed. GUI locale ja, scale 1.0, display 2560×1440; no resolution change.

Other M2.3 workflows and final-head GUI acceptance remain open.

### 2026-10-19 — Level Selection and Level All scope evidence

The existing physical journeys `selectedTaskLevelingUsesTheResourceRibbonAndSupportsUndoRedoAndPersistence`
and `levelAllUsesEveryProjectResourceWithoutChangingTaskSelectionAndSupportsUndoRedoAndMpo`
passed in the Japanese/100% `TaskInformationRibbonGuiAcceptanceTest` class
run (56 tests, 0 failures, resolution unchanged). Level Selection applies to
the selected task only while the unselected leveling anchor stays fixed;
Level All applies across every project resource while preserving selection.
Both journeys verify Undo/Redo and MPO state. This follows Microsoft's
[Resource Leveling dialog box](https://support.microsoft.com/en-us/project/resource-leveling-dialog-box)
and [Distribute project work evenly by leveling resource assignments](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments).
The pages disagree about the default sensitivity period (Month versus Day), so
the MSP default remains unresolved and is not claimed as compatibility.

### 2026-10-19 — Recapture an existing Baseline 1 across the project

`TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneEntireProjectPreservesBaselineZeroAndCurrentValuesAcrossUndoRedoAndMpo`
physically selects Baseline 1 and Entire Project in Save Baseline, confirms
the visible overwrite prompt, and verifies the new task baseline start/work/cost.
It also proves Baseline 0 and current schedule/work/cost stay unchanged.
Physical Ctrl+Z restores the old Baseline 1; Ctrl+Y restores the recaptured
values. MPO save/reload is checked for both history states. Microsoft's
[Create or update a baseline or an interim plan in Project desktop](https://support.microsoft.com/en-us/project/create-or-update-a-baseline-or-an-interim-plan-in-project-desktop)
documents numbered baseline selection, Entire Project scope, and overwrite
confirmation. This is microProject physical evidence, not an empirical MSP run.
The focused Japanese/100% command passed (1 test, 0 failures):
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneEntireProjectPreservesBaselineZeroAndCurrentValuesAcrossUndoRedoAndMpo -Locale ja -Scale 1.0`.
`git diff --check` passed; resolution was not changed.

### 2026-10-19 — Update Project completes partially worked assignments

Expanded `TaskInformationRibbonGuiAcceptanceTest.updateProjectMenuRouteUsesDeferredDialogFixture`
to use a four-day assigned task at 50%, a fixed Status Date at scheduled start,
and a complete-through date after scheduled finish. The physical Project >
Update Project journey verifies 100% completion, task and assignment
Actual Start/Finish, Actual Work equaling Work, zero Remaining Work, and matching
single-assignment totals. Ctrl+Z restores every partial-progress actual value;
Ctrl+Y restores completion; MPO reload retains status date and task/assignment
actuals. The acceptance records a document-derived rule (not MSP measurement):
the current implementation's completion setter clips the completed portion at
the scheduled finish. Microsoft's [Update work on a project](https://support.microsoft.com/en-us/project/update-work-on-a-project),
[Set the status date for project reporting](https://support.microsoft.com/en-us/project/set-the-status-date-for-project-reporting),
and [Actual Finish fields](https://support.microsoft.com/en-us/project/actual-finish-fields)
support the actual/remaining fields and note that finish-date behavior depends
on calculation preferences.

The core command
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.UpdateProjectRequestTest.completingPartiallyWorkedAssignedTaskSetsExactActualsAndUndoRestoresThem" --console=plain`
passed (1 test). The focused Japanese/100% GUI command passed (1 test):
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.updateProjectMenuRouteUsesDeferredDialogFixture -Locale ja -Scale 1.0`.
`git diff --check` passed; resolution was not changed. Other M2.3 workflows
and final-head GUI acceptance remain open.

### 2026-10-19 — Task Information report CSV and print rendering

The physical `TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute`
selects Task Information, compares all CSV headers and row values with the
rendered report model, verifies distinct task dates and durations, renders each
printable page and checks visible content, and confirms report operations do
not mutate the project. MPO reload checks dates, durations, and fixed costs.
Task Information has no Work or Cost columns, so this slice does not claim
output consistency for those fields or for other report definitions.

Verification passed:

- `.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute -Locale ja -Scale 1.0` — 1 passed, 0 failed.
- `git diff --check` passed. Display was 2560×1440; resolution was not changed.

During fixture development, an unassigned normal task's implicit Work changed
from 57,600,000 to 28,800,000 after MPO reload. This is tracked separately in
[issue #868](https://github.com/tetsuji16/ProjectLibre/issues/868); the report
journey does not assert that field across reload. Other M2.4 command and report
coverage and final-head acceptance remain open.

### 2026-10-19 — Project Details Work and Cost report values

The new physical `TaskInformationRibbonGuiAcceptanceTest.physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues`
selects Project Details in the rendered report selector and compares its
scheduled Work and Cost labels and formatted values with the project fields.
It exports through the physical CSV button, checks every parsed CSV row against
the rendered model and the known Work/Cost values, renders the printable page,
and confirms report selection/export leaves those project fields unchanged.
This adds focused value coverage for one report definition; it does not close
the remaining M2.4 command, report, print, or persistence inventory.

Verification passed:

- `.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalProjectDetailsReportShowsProjectWorkAndCostAndExportsTheSameValues -Locale ja -Scale 1.0` — 1 passed, 0 failed.
- Display was 2560×1440; resolution was not changed.

### 2026-10-19 — Phase 0 inventory and current-head Phase 1 recheck

Added [`gui-incident-command-inventory.md`](gui-incident-command-inventory.md)
to map the recovery-sequence incident routes, selection preconditions, expected
model/view outcomes, Undo/persistence expectations, local execution owner, and
remaining evidence questions. `installDist` was rebuilt from the current dirty
worktree with `--rerun-tasks` (BUILD SUCCESSFUL, 20 tasks executed).

Then ran
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest -Locale ja -Scale 1.0`;
the runner reported 2560×1440 and the class passed 58/58, 0 failures/errors or
skips. This includes the Phase 1 physical Task Information, Link/Unlink,
Indent/Outdent, Expand/Collapse, Hide/Show, editor shortcut, and selection
shortage contracts. Display resolution was not changed. This is not the full
GUI suite or broad locale/scale matrix; M2.1–M2.4 and #794 remain open.

### 2026-10-19 — Cross-project dependency Undo ownership

Cross-project dependency edits now accept an explicit command-project Undo
owner. Task Information creation and the dependency-grid Delete factory pass the
project whose task dialog initiated the operation; the shared Link/Unlink
gateway passes its active project for both single and bulk mutations. Existing
service overloads retain their predecessor-project default for callers that do
not specify an owner. The production cause was that Task Information Remove
used a dependency node factory which always recorded Undo on the predecessor
project, while Ctrl+Z used the successor's active document history. This was
tracked in [issue #870](https://github.com/tetsuji16/ProjectLibre/issues/870).

The physical `TaskInformationCrossProjectGuiAcceptanceTest` now covers link
creation and removal for FS/SS/FF/SF: model endpoints, unrelated-link
preservation, rendered dependency rows, physical Ctrl+Z/Ctrl+Y, and MPO reload.
All four journeys passed. The test fixture explicitly marks the document active
and enables its Undo/Redo commands because it uses a lightweight document-frame
stub without a rendered project view. The focused Calendar marker Remove
journey also passed its model, rendered-card, Undo/Redo, and MPO checks.

Verification passed:

- `:microproject_core:test --tests "com.microproject.pm.dependency.DependencyServiceTest" --console=plain --no-daemon --max-workers=1` — passed.
- `:microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.command.TaskCommandGatewayTest" --console=plain --no-daemon --max-workers=1` — passed.
- `.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.dialog.TaskInformationCrossProjectGuiAcceptanceTest -Locale ja -Scale 1.0` — 4 passed.
- `.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.calendarDependencyMarkerOpensTheLinkDialogAndCanRemoveTheLink -Locale ja -Scale 1.0` — 1 passed.
- Display was 2560×1440; resolution was not changed.

The multi-link Task ribbon Unlink route passed its focused physical journey on
2026-10-19: the Robot clicked the ribbon button, selected the outgoing link
through chooser keyboard input, verified the incoming link remained and the
successor Predecessors cell/Gantt edge changed, then passed physical Undo/Redo
and MPO save/reload. The focused run used Japanese locale and scale 1.0 at
2560×1440 with no resolution change. The current-head Phase 2 exit gate then
passed: `:microproject_ui:test --console=plain --no-daemon --max-workers=1`
and `:microproject_ui:guiTest --console=plain --no-daemon --max-workers=1
-PguiTestLocale=ja -PguiTestUiScale=1` both completed successfully. The GUI
suite reported 218 tests, 0 failures/errors, and 1 conditional high-DPI-only
skip. No monitor resolution was changed. Phase 2 evidence is complete for the
current local routes; broader M2 acceptance and the full locale/scale matrix
remain open. #266 remains open pending upstream issue review/closure.

The current-head Phase 6 strict GUI gate passed after a fresh
`clean installDist` and again after the #871 monitor fix:
`\.github\scripts\run-gui-acceptance.ps1 -Suite full`.
The Japanese/100% functional leg recorded 218 cases (217 passed, 1 permitted
conditional skip, 0 failures); each of the five English/Japanese 100/125/150%
visual legs recorded 102 cases (101 passed, 1 conditional skip, 0 failures).
All six attempt directories had no environment-contention markers
or overlap events. The runner reported the unchanged 2560×1440 desktop; only
application UI scale varied, and no display resolution was changed. The full
locale/scale matrix still has the documented conditional high-DPI skip and
broader M2 acceptance remains open.

The clean build initially exposed a stale desktop-monitor watcher left from a
previous GUI run; it kept a log open and blocked deletion of generated UI build
artifacts. [Issue #871](https://github.com/tetsuji16/ProjectLibre/issues/871)
tracks the cause. The watcher now polls its target process and closes its log
when that process exits. A focused PowerShell lifecycle check verifies the
stop record and exclusive log access, and the Windows GUI workflow runs this
check before acceptance. The focused multi-link Unlink GUI test passed after
the fix. A subsequent `clean installDist` passed both before and after the
strict full GUI gate, with no leftover watcher processes. #871 remains open
until this worktree change is reviewed and merged.
## Verification policy

Record the exact command, commit, result and skipped cases with each published
change. The task-local JDK, proxy setup, fonts and generated build artifacts are
not product changes. A passing compile is not a passing test. Failed baseline
behavior is fixed or explicitly reproduced against the original commit;
it is not silently relabeled as successful validation.

## M2.1 Paste Insert physical evidence (2026-10-19)

The task-row popup Paste Insert route was exercised physically from row-header
Copy through the shared `TaskPasteIntent` batch path. The Robot journey verifies
the inserted task's name and position by task object identity (task row IDs are
renumbered by insertion), spreadsheet and Gantt row publication, one Ctrl+Z
restoring the exact task identities/order and both views, one Ctrl+Y restoring
the inserted row, and MPO save/reload preserving its order. The same fixture
also verifies that the ribbon Paste command still dispatches through its
canonical edit pipeline. Both routes now preserve the typed gateway result,
including stable selected and affected task IDs. The wrapped DocumentFrame
route defers spreadsheet publication so the outer GraphicManager action
records exactly one semantic result; direct TransferHandler imports continue
to publish once at the spreadsheet boundary. The Robot journey asserts one
`UI_COMMAND_RESULT` record for each ribbon paste command. Structural paste
still uses the typed batch gateway, and the injected mid-batch rollback test
covers partial failure.

Verification passed on the unchanged 2560×1440 desktop at Japanese locale and
UI scale 1.0; no display resolution was changed:

- `\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.copyCutPasteThroughRibbonUseTheSharedEditPipeline -Locale ja -Scale 1.0` — 1 passed.
- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.command.TaskCommandGatewayTest" --tests "com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferablePasteFailureTest" --console=plain --no-daemon --max-workers=1` — passed.

This closes only the Paste Insert evidence slice. Issue #408 remains open for
shared mutation outcomes/refresh; parent issue #794 remains open for the other
M2 packages and release evidence.

### M2.1 Gantt schedule gateway rejection coverage (2026-10-19)

`TaskCommandGatewayTest` now checks that a valid captured Gantt progress intent
is rejected before its mutation callback when the project is read-only
(`REJECTED`) or collaboration locking denies access (`LOCKED`). Both cases
preserve schedule and constraint values and leave Undo availability and its
presentation name unchanged. The focused test class passed 13 tests with no
failures; `git diff --check` passed. No display resolution was changed.

This adds gateway rejection coverage to the existing move/resize/progress/split
schedule-intent route. It does not close #408: shared mutation outcomes and
refresh behavior remain open, as do the other M2 rows under #794.

### M2.1 Gantt gesture outcome publication (2026-10-19)

Gantt schedule gestures now publish their `TaskCommandResult` through the shared
`RibbonCommandResult` diagnostic state, including the captured task ID, the
affected task ID when changed, and the active document view. The same semantic
status determines whether the gesture reports a change and refreshes Undo
controls. Existing schedule/model events continue to drive view updates; this
change does not add a parallel repaint path.

The physical Gantt bar-move journey asserted `CHANGED`, matching selected and
affected task IDs, and active view `Gantt`. The focused Robot run passed on the
unchanged 2560×1440 desktop, Japanese locale, UI scale 1.0. The focused command
gateway class passed 13 tests; production and GUI test compilation passed:

- `.\gradlew.bat :microproject_ui:compileJava :microproject_ui:compileGuiTestJava :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.command.TaskCommandGatewayTest" --console=plain --no-daemon --max-workers=1`
- `.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotDragMovesBarAndRecalculatesFfSuccessor -Locale ja -Scale 1.0`

This closes only schedule-gesture result publication. Shared result handling for
other mutation families and the complete #408 view-refresh criteria remain
open; #794 remains open.

### M2.1 Spreadsheet task-cell outcome publication (2026-10-19)

Task-field edits routed through `TaskCommandGateway` now publish the semantic
gateway result through the owning spreadsheet to `GraphicManager`, including
the currently selected task IDs, the edited task ID when changed, and the
active view supplied by the shared result recorder. Changed and no-op outcomes
are covered in the existing stable-intent spreadsheet test; mutation,
model-event refresh, Undo, and MPO round-trip remain on their existing paths.

The focused test passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.command.TaskCommandGatewayTest.spreadsheetModelRoutesTaskFieldInputThroughStableIntent" --console=plain`
and `git diff --check` passed for the touched files. The physical U-26 Robot
journey was not rerun for this result-publication change. No display resolution
was changed. Other shared mutation families and final-head GUI acceptance
remain open; #794 remains open.

### M2.4 MPO unassigned task Work persistence (#868, 2026-10-19)

An unassigned normal leaf task's scalar Work is now stored in the additive
`TASK_SCHEDULES_ENTRY` MPO metadata and restored after MSPDI task and
assignment reconstruction. The restore path does not call the scheduling
rule, so it does not rewrite Start, Finish, or Duration. The field is optional
for older MPO files; malformed values and values targeting assigned tasks or
WBS parents are rejected. The transient restored value is discarded after
normal Work/Duration edits and assignment add/remove operations, so it cannot
mask later model changes. MPP/MSPDI shared import and POD serialization remain
unchanged.

The regression first reproduced Work drifting from 24 hours to 8 hours while
Start, Finish, and Duration stayed fixed. Tests now cover initial reload,
no-op save/reload, assigned-task controls, prior MPO metadata without the new
field, invalid metadata, and clearing the restored value after task and
assignment edits.

Verification passed:

- `:microproject_exchange:test` — 293 tests, zero failures, errors, or skips.
- `:microproject_core:test` — 1,072 tests passed.
- `git diff --check` — passed.

These checks were headless and did not change display resolution. The fix is
limited to the MPO persistence defect tracked by #868; M2.4 and #794 remain
open.

### M2.4 Change Working Time calendar date clarity (#864, 2026-10-19)

The Change Working Time calendar now labels Gregorian dates as the working
date and explains that smaller day numbers and the appended month title come
from the selected companion chronology. The legend names that chronology and
whether it came from language/locale or Windows regional settings. Gregorian
weekdays, selection, exception editing, and saved dates are explicitly stated
as authoritative. Per-day tooltips show both full dates, including dates
before 1970, and the calendar exposes the same rule through its accessible
description. ISO-only locales identify Gregorian-only display.

Verification passed:

- `:microproject_ui:test --tests "com.microproject.util.AlternativeCalendarDisplayTest" --tests "com.microproject.dialog.calendar.CalendarViewRenderingTest" --console=plain` — 13 tests, zero failures, errors, or skips.
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotAddsRecurringExceptionCommitsUndoesRedoesAndPersists" --max-workers=1 --console=plain --no-daemon "-PguiTestLocale=ja" "-PguiTestUiScale=1.0"` — 1 passed; selected date, exception edit, Undo/Redo, and save/reload remained Gregorian-based.
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotEnglishAlternativeCalendarExplainsGregorianEditDatesAndTooltips" --max-workers=1 --console=plain --no-daemon "-PguiTestLocale=en" "-PguiTestUiScale=1.0"` — 1 passed; Hijrah name, locale source, accessibility description, and cell tooltip were visible.
- `git diff --check` — passed.

Both Robot checks used the current desktop resolution and UI scale 1.0; no
resolution change was made. Non-Japanese/English localization and the full
alternate-calendar month/year matrix remain unverified. This closes the
reported date-role ambiguity slice only; #864 and #794 remain open.

### M2.1 Spreadsheet row-move outcome and persistence (2026-10-19)

Both spreadsheet row-move entrypoints now publish the Task Gateway result via
one shared result-recording helper also used by task-cell edits. Stable task
keys supply the selected IDs; affected IDs are present only for a changed
outcome. The physical Alt+Shift+Up journey asserts the changed outcome, IDs,
active view, model row order, Undo/Redo, and task order plus stable identities
after MPO save/reopen.

The focused task-cell command tests and physical row-move journey passed; the
GUI result XML reports one test with zero skips, failures, or errors. The
journey ran at the existing 2560×1440 resolution, unchanged before and after.
`git diff --check` passed for the implementation and acceptance-test files.
This closes only the row-move result/persistence slice; #414 and other M2.1
command outcomes remain open.

### M2.1 Gantt dependency gesture outcome publication (2026-10-19)

The Gantt bar-to-bar dependency gesture now publishes the gateway outcome to
`GraphicManager`, including both endpoint task IDs and the active view. The
physical route asserts `CHANGED`, endpoint IDs, and Gantt view, then retains its
existing dependency projection, Undo/Redo and MPO reopen assertions. Invalid
endpoints and rejected gateway results publish `REJECTED`. The existing
dependency mutation and model-event refresh path is unchanged.

Verification passed on Windows 11, JDK 25.0.4.1, Japanese locale, UI scale
1.0, and the existing 2560×1440 display:

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.gantt.GanttInteractorDragTest" --console=plain`
- `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain `
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel" --max-workers=1 --console=plain "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" `

The focused GUI XML reports one test, zero skips, failures, or errors. No
display resolution was changed. This closes only dependency-gesture result
publication; other mutation families and the complete #408/#794 acceptance
remain open.

### M2.4 Phase 4 native multiple-file Open route (2026-10-19)

`MultipleLocalProjectOpenGuiAcceptanceTest.nativeFileOpenChooserSelectsTwoProjectsAndKeepsExistingProject`
uses the real Windows native File > Open chooser to select two temporary MPO
files. It verifies the chooser returns both exact paths, both loaded projects
retain their fixture names and task content in distinct desktop windows, and
an already-open dirty project remains registered and dirty. The test cleans up
the chooser provider, manager queue, documents, fixture files, and environment
state. This supplements the existing multiple-open test that supplied file
names through a stub provider.

Verification passed:

- `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain`
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.MultipleLocalProjectOpenGuiAcceptanceTest.nativeFileOpenChooserSelectsTwoProjectsAndKeepsExistingProject" --max-workers=1 --console=plain "-PguiTestLocale=ja" "-PguiTestUiScale=1.0"`
- GUI result XML: 1 test, 0 skips, failures, or errors; `git diff --check` passed.

The physical run used the existing 2560×1440 desktop; no resolution change
was made. This closes the native multiple-selection evidence gap in the #395
inventory row only. Remaining Phase 4/client acceptance and M2.1–M2.4 rows
remain open; #794 remains open.

### M2.4 Selected Tasks zoom with multiple physical selections (#794, 2026-10-19)

A Windows Robot journey now physically selects two dated task rows 60 days
apart and invokes the real View ribbon command in Gantt, Tracking Gantt, Task
Usage, and Resource Usage. Each route verifies both stable task IDs were
selected and the visible range spans the first start through the second finish.
All routes assert that task dates and project Undo history remain unchanged.
The Gantt-family sheets use Ctrl-click; both Usage sheets require Shift-click
for this row selection, as Ctrl-click selected only one Usage row.

Verification passed:

- `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — passed.
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.physicalSelectedTasksZoomFitsBothSeparatedGanttTasksWithoutChangingModelOrUndo" --max-workers=1 --console=plain --no-daemon "-PguiTestLocale=ja" "-PguiTestUiScale=1.0"` — 1 Robot test passed.
- `git diff --check` — passed.

The desktop resolution was not changed. This closes the four-view
multi-selection evidence row in U-31; other #794 command, report, print, and
file compatibility work remains open.

### M2.4 Who Does What report journey (#794, 2026-10-19)

Added a focused GUI acceptance journey for the standard Who Does What report.
Its fixture has two resources and two assigned tasks with distinguishable work.
The journey selects the report by its visible name, checks resource grouping,
assignment rows and group totals, compares every rendered row and header with
the exported CSV, confirms every printable page renders, and verifies report
selection/export leave project, task, assignment, dirty, and Undo state intact.

Final-source verification passed `:microproject_ui:compileGuiTestJava` and
`git diff --check`. The final physical Robot run is pending because keyboard
input is unavailable across Windows; no resolution change was made. This adds
coverage but does not close the M2.4 report/print acceptance row or #794.

### M2.1 Gantt wheel ownership headless verification (#414, 2026-10-19)

Re-ran `GanttWheelZoomTest` on the current worktree. All 6 tests passed,
including the single synchronized-wheel owner, no duplicate Gantt wheel
listener, Ctrl+wheel zoom direction/anchor behavior, and confirmation that
keypad zoom shortcuts are not registered on the Gantt component. No display
resolution was changed. U-32 physical keypad input from both Gantt and
spreadsheet focus is now verified in the 2026-10-20 re-run below. Physical
plain/Shift/Ctrl wheel scrolling remains a separate pending route.

### M2 shared workflow module regression run (2026-10-19)

After the dialog-binding lifecycle change, the current worktree's UI unit suite
completed with 1,172 tests across 191
suites, 0 failures, 0 errors, and 0 skips:
`.\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1`.
Core and exchange suites were explicitly rerun (not reused from Gradle's
up-to-date cache) with
`.\gradlew.bat :microproject_core:test :microproject_exchange:test --rerun-tasks --console=plain --no-daemon --max-workers=1`.
Core completed 1,072 tests across 239 suites; exchange completed 293 tests
across 35 suites. Both had 0 failures, 0 errors, and 0 skips. `git diff --check`
passed. These headless suites do not satisfy the recovery sequence's
physical Robot gates; no display resolution was changed.

### M2.1 Preserve dialog key actions across Look & Feel refresh (#414, 2026-10-19)

`FlatUiSupport.updateOwnedWindowTrees` previously retained only the Escape
binding when refreshing an owned dialog tree. It now snapshots and restores all
explicit `WHEN_IN_FOCUSED_WINDOW` root-pane bindings that have dialog-owned
non-`UIResource` Actions, including Enter, F1, and dialog-specific shortcuts.
Look-and-Feel `UIResource` actions remain replaceable during refresh. This is a
lifecycle/input-path fix; it does not change the action handlers or scheduling
state.

`FlatUiSupportRootPaneBindingsTest` simulates replacing the root-pane maps and
verifies Escape/Enter/F1/Ctrl+Z bindings and their Action identities survive,
while an updated Look-and-Feel binding and Action are not frozen. The two focused
`FlatUiSupport` tests passed. The GUI integration test
`FlatUiSupportDialogBindingsGuiAcceptanceTest.parentLookAndFeelRefreshPreservesOwnedDialogActions`
also passed: it switched between FlatLaf dark/light themes, then refreshed a
real owned `JDialog` through its parent frame and verified all four application
Actions remained intact. It did not send keyboard events or change display
resolution, so physical keypress behavior remains unverified. `git diff --check`
passed; #414/#794 remain open.

`MicrosoftShortcutsRootPaneTest` now records every `InputMap.put` call during
document root-pane shortcut installation and asserts each KeyStroke is written
once, including identical-action repeats. The complete focused class passed
9 tests with 0 failures/errors/skips. This strengthens the #414 duplicate-route
regression gate without sending physical key events.

### M2.1 refreshed installed layout (#414, 2026-10-19)

Regenerated the runnable distribution from the current source with
`.\gradlew.bat :microproject_ui:installDist --rerun-tasks --console=plain --no-daemon --max-workers=1`.
Gradle executed all 20 tasks and completed successfully; the fresh layout is
`modules/microproject_ui/build/install/microproject_ui`. This is build evidence
only. The repository's clean launcher stops existing ProjectLibre Java
processes before launching, so no app process was started; the installed-layout
physical route remains pending while Windows keyboard input is unavailable.

### M2.1 selection source precedence consolidation (#414, 2026-10-19)

`ActiveTaskSelectionResolver.preferPhysicalRowSelection` now owns the common
precedence rule used by ordinary task commands (active sheet) and Hide Selected
(top sheet): the presence of physical selected rows wins over a potentially stale frame
selection, even if the row-to-node projection is temporarily empty; otherwise the
frame selection is used. The distinct active/top
sheet lookup, header-column rejection, and virtual group-row expansion remain
unchanged. `ActiveTaskSelectionResolverTest` covers physical-row precedence and
frame-selection fallback; existing Hide Selected coverage continues to exercise
group expansion.

Focused headless verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.ActiveTaskSelectionResolverTest" --tests "com.microproject.pm.graphic.frames.TaskVisibilitySelectionResolverTest" --console=plain --no-daemon --max-workers=1`
(7 tests total: 6 resolver tests and 1 visibility/group-expansion test; 0 failures,
0 errors, 0 skipped). `git diff --check` passed. No
resolution changes or physical keyboard events were used. Physical GUI gates
remain pending.

### M2.1 Link/Unlink command outcome consistency (#414, 2026-10-19)

Link and Unlink now check project-level read-only state before task-selection
eligibility, so a read-only project reports the same `document-read-only`
reason through both commands. Their rejected results retain resolver-selected
stable IDs and have no affected IDs. The shared task-result publishers now use
the top view ID for every outcome; rejected/failed outcomes have empty affected
IDs, while changed/no-change outcomes retain selected IDs as affected IDs.
`RibbonButtonBehaviorTest` covers read-only Link/Unlink reason parity, selected
IDs and empty affected IDs, the active view on rejected one-task Link, and the
active view and empty affected IDs on a rejected read-only hierarchy command.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`

### M2.1 Expand/Collapse mixed-selection route consistency (#794, 2026-10-19)

Expand and Collapse now pass one `resolveTaskSelection(false, false)` snapshot
through the task-table mutation pipeline, matching the button enablement
precondition. Mixed task/resource selections reject with `mixed-selection`,
retain all selected stable task IDs, report no affected IDs, and include the
active view. The resolver now collects task IDs even when a non-task row comes
before a task. `RibbonButtonBehaviorTest` verifies both actions are disabled
and both direct routes leave the table row projection and task hierarchy
unchanged; `ActiveTaskSelectionResolverTest` covers mixed row ordering.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.ActiveTaskSelectionResolverTest" --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(52 tests: 6 resolver and 46 ribbon behavior; 0 failures, 0 errors, 0 skipped).
No Robot GUI input or display-resolution changes were used.
(43 tests total, 0 failures, 0 errors, 0 skipped). GUI/Robot input and display-
resolution changes were not used.

### M2.1 Task Information command outcome semantics (#794, 2026-10-19)

Task Information is a dialog route, so both its dispatched and rejected
diagnostics now keep selected stable IDs, publish the active view, and leave
affected IDs empty. The headless ribbon test checks rejected routing and the
no-selection case; GUI acceptance now expects empty affected IDs after the
dialog is visibly opened.

`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
passed (43 tests; 0 failures, 0 errors, 0 skipped).
`.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1`
passed. Robot GUI acceptance was not executed and display resolution was not
changed.

### M2.1 Ribbon display-mode popup trigger consolidation (#414, 2026-10-19)

The ribbon-tab display-mode popup now routes press and release events through
the shared `PopupTriggerController`: press+release platforms present one menu,
press-only and release-only trigger platforms present one menu, and ordinary
clicks present none. The headless event regression checks the single expected
localized menu item for the current state, invokes it, and verifies the display
mode actually changes. Double-left-click collapse remains covered in the same
route test. After regenerating the runnable layout with
`.\gradlew.bat :microproject_ui:installDist --console=plain --no-daemon --max-workers=1`,
the physical Robot test
`.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest.rightClickTabCollapsesAndRestoresRibbonCommands" --max-workers=1 --console=plain --no-daemon`
passed at the current working-tree head: right-click opened the popup, selecting
its item collapsed/restored the ribbon, and double-click retained its toggle
behavior. The headless test separately verifies the exact popup item label and
single dispatch for each platform trigger pattern. Display resolution was not
changed.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.ribbon.ModernRibbonPanelDisplayModeTest" --rerun-tasks --console=plain --no-daemon --max-workers=1`
(2 tests; 0 failures, 0 errors, 0 skipped). `git diff --check` passed for the
owned source and test files.

Physical GUI verification passed for the display-mode popup at the current
working-tree head as recorded above; no display-resolution change was used.

### M2.2 Batch assignment removal failure atomicity (#794, 2026-10-19)

Same-project `removeCollectionGroup` now runs through `Project.runAtomicModelCommand`
with its resource-pool event managers as participants. Before removal it captures
each assignment detail and each affected task detail/current snapshot. If a
removal or in-operation transaction-close observer fails before commit,
compensation restores task/resource reverse links and captured model details
before the exception is rethrown; the atomic boundary discards queued
project/pool events, Undo edits, and revision changes. The deterministic test throws from the second resource's
`removeAssignment` after `super` has detached it, and verifies assignment
identities, values, task snapshot, Undo, events, and revisions. The existing
successful single-Undo-group test still passes. Multi-project requests remain
independent per-project groups; a failure in a later group does not roll back
already committed earlier groups. A two-project regression injects failure
after detach in the second group and verifies the first group's commit, Undo,
revision and event remain while the second group's links, Undo, revision and
events are restored or discarded.

Focused verification passed:
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.assignment.AssignmentServiceTest" --console=plain --no-daemon --max-workers=1`
(32 tests; 0 failures, 0 errors, 0 skipped). GUI/Robot and display-resolution
tests were not run.

Object-event publication is post-commit and occurs after the outer coordinated
mutation releases its barrier. An observer exception can therefore reach the
caller even though removal, Undo history, and project/pool revisions are
already committed. The follow-up regression verifies that committed state is
retained and the callback runs after barrier release. If an earlier observer
has already run before a later observer throws, those external side effects
cannot be retracted. This post-commit observer failure is outside the atomic
rollback guarantee.

### M2.2 Batch assignment creation failure atomicity (#794, 2026-10-19)

`newAssignments` now compensates partial task/resource attachment failures using
the shared `AssignmentModelState` snapshot/restore path also used by replacement
and collection removal. The snapshot retains assignment identities/details,
bidirectional links, affected task details, and the original current snapshot.
Project/pool operations use `Project.runAtomicModelCommand`, so a failure
discards project and resource-pool events, Undo edits, and revisions after
compensation. The original project/pool command-boundary condition is preserved
for initialized and uninitialized projects alike; only the boundary's failure
mode changes from non-atomic to atomic.

The deterministic regression attaches one ordinary resource, then injects an
`AFTER_ATTACH` exception from a registered `ResourceImpl` subclass on the next
resource. It verifies restoration of the pre-existing assignment identity and
links, absence of both new assignments, task values and snapshot, no Undo, no
events, and unchanged project/pool revisions. The assignment-service suite also
retains the replacement failure tests and collection-removal single-Undo-group
coverage.

The same local compensation now covers the single-assignment `newAssignment`,
`connect`, and `remove` paths. Published operations use the atomic project
boundary; direct/non-published operations retain their existing execution path
and still restore locally on failure. Regressions inject failures after the
task-side attach for creation and connect, and after resource detach for
removal; all three verify restored links, no Undo/event/revision leakage.

`rewireSharedPoolAssignment` now uses the project atomic boundary with both the
source and destination pool event managers enlisted. A failed reconnect restores
the original assignment/task state and discards project/pool events, Undo, and
revisions. The failure regression injects an exception after the replacement
resource has actually attached; the successful path still refreshes both
resources in a single pool revision.

Focused verification passed:
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.assignment.AssignmentServiceTest" --console=plain --no-daemon --max-workers=1`
(31 tests; 0 failures, 0 errors, 0 skipped; rerun with `--rerun-tasks`). GUI/Robot and display-resolution
tests were not run.


### M2.1 Read-only hierarchy mutation outcome selection (#794, 2026-10-19)

When Expand or Collapse is rejected because the project is read-only, the
captured stable task IDs are now retained in the result, affected IDs remain
empty, and the current view ID is reported. The focused test routes both
commands against a selected task and verifies the result contract and that the
table row count does not change.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(46 tests; 0 failures, 0 errors, 0 skipped). No Robot GUI input or
display-resolution changes were used.

### M2.1 Indent/Outdent read-only outcome selection evidence (#794, 2026-10-19)

Indent and Outdent retain resolver-captured selected task IDs when the
project-level read-only guard rejects the command. Affected IDs remain empty
and the active top view is reported. The focused test uses a visible physical
table-row selection and verifies both commands reject without changing the
row projection or selected task's WBS parent.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(46 tests; 0 failures, 0 errors, 0 skipped). No Robot GUI input or
display-resolution changes were used.

### M2.1 Task visibility rejection outcome view (#794, 2026-10-19)

`DocumentFrame.routeTaskCommand` now normalizes every result, including early
rejections and legacy dispatched fallbacks, to the current top view ID. The
regression test verifies that a read-only Hide Selected result retains the
selected stable task ID, reports no affected IDs, and includes the active view.
Changed and no-change outcomes retain their existing affected task IDs.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(44 tests; 0 failures, 0 errors, 0 skipped). No Robot GUI input or
display-resolution changes were used.

### M2.1 Task Information terminal presentation outcome (#794, 2026-10-19)

The ribbon Task Information action now records its semantic outcome after the
dialog presenter returns. A visible dialog yields `DISPATCHED`; a completed
presentation attempt without a visible dialog yields `FAILED/dialog-not-visible`;
runtime exceptions and errors yield `FAILED` using the exception-class reason
from `RibbonCommandResult.failed`, then propagate. All terminal results retain
the selected stable task ID, empty affected IDs, and current active view. Route
rejection and missing selection remain `REJECTED`. A protected presenter seam
lets the headless test cover invisible and throwing presentations. The Robot GUI
acceptance checks visible dialog discovery before asserting `DISPATCHED`, and
explicitly checks empty affected IDs. Non-ribbon callers retain their separate
UI failure warning when the dialog is not visible.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(46 tests; 0 failures, 0 errors, 0 skipped).
`.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1`
passed. Robot GUI acceptance was not executed and display resolution was not
changed.

### M2.1 Row-header BUTTON1 popup trigger (#794, 2026-10-19)

Row-header popup-trigger handling now takes precedence over ordinary left
selection when the platform marks BUTTON1 as a popup gesture. Regression tests
cover popup-on-press, popup-on-release, and both-event dispatch exactly once,
while retaining the existing right-button popup cases. Physical macOS
Control-click evidence remains pending; display resolution was not changed.

### M2.1 Shared active task-selection owner (#794, 2026-10-19)

Task commands and Hide Selected now resolve through one owner that pairs the
active spreadsheet with its physical row selection and header-column state.
The regression selects a task in the bottom Gantt spreadsheet while the top
WBS view has no spreadsheet, leaves a different stale frame selection, and
verifies Hide Selected reports and hides only the bottom row's stable task ID.
Existing top-sheet and virtual-group visibility coverage remains in place.

Focused verification passed:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.RibbonButtonBehaviorTest" --console=plain --no-daemon --max-workers=1`
(47 tests; 0 failures, 0 errors, 0 skipped). `git diff --check` passed. No
display-resolution changes were used.

### M2.2 External task dependency endpoint rebind (#794/#409, 2026-10-19)

Project-open resolution now delegates placeholder endpoint rebinding to one
`DependencyService` transaction. It snapshots both endpoint lists and endpoint,
disabled, and dirty fields; removes dependencies from the placeholder lists;
adds each link once to the resolved task; and publishes delete/create events
inside the affected projects' existing mutation boundaries without adding user
Undo history. Unloaded or unmatched references remain unresolved as before.
Circularity validation still disables and warns for invalid links. Tests cover
both placeholder endpoint directions and an injected second-link failure; the
failure restores endpoint references/lists and suppresses queued events and
revision advances. The affected real-task start/end sentinel relationships are
recomputed, and rollback tests verify those sentinel links are restored. The
open-time resolver now changes external flags and invalidates schedules only
after dependency resolution succeeds; tests cover manager retry after an
injected failure and unchanged dirty/group-dirty state. Placeholder removal
from owner and root task lists/projections now runs in the same atomic mutation
as dependency rebinding. A project-side snapshot preserves task-list position
and each outline's node, parent, and position, restoring through the node model
so its search index is rebuilt. Tests cover owner/root removal, root-projection
failure rollback, event/revision suppression, and successful retry. The legacy
`Project.removeExternal` behavior used by MPO task deletion remains unchanged.
Display resolution was not changed.

Focused verification passed:
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.dependency.DependencyServiceTest" --console=plain`
(42 tests; 0 failures, 0 errors, 0 skipped). `git diff --check` passed.

### M2.2 SNET + FF zero-lag boundary (#794, 2026-10-20)

Extended `TaskLinkAndBarScheduleTransitionsTest` with fixed-date SNET + FF
cases where the predecessor finishes before and after the successor's SNET
date. The common FS/FF fixture verifies exact schedules, unchanged unrelated
task, diagnostics, and link Undo/Redo. Microsoft documents SNET as a lower bound
and FF as a finish dependency; this is a document-derived compatibility
decision, not an empirical MSP result. Source: [How Project schedules tasks:
Behind the scenes](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes).
The FF contract is asserted through the successor finish boundary because
`Dependency.getDate(true)` represents the calculated successor start for FF.

Focused verification passed:
`.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest" --console=plain --no-daemon --max-workers=1`
(49 tests; 0 failures, 0 errors, 0 skipped). Further SNET + SF coverage,
varied calendars/lags, persistence and mutation-family cases remain; this one
boundary slice does not close M2.2.

MPO persistence follow-up (2026-10-20): added a fixed-date save/reopen/recalculate
regression family to `MpoFileImporterTest`, porting both cases: predecessor
finishes before SNET (Monday 17:00 vs Tuesday 08:00) and after SNET (Tuesday
17:00 vs Tuesday 08:00). Both check exact schedules before and after reload,
restored successor SNET date, FF zero lag, and reciprocal endpoint identity.
This extends the document-derived boundary coverage above; it is not an
empirical MSP result.

### M2.2 SNET + SF zero-lag boundary (#794, 2026-10-20)

Added two fixed-date SNET + SF boundary cases to
`TaskLinkAndBarScheduleTransitionsTest`: predecessor start before and after the
successor's SNET date. The cases assert exact schedules, unrelated-task
preservation, diagnostics, and dependency Undo/Redo. This is a document-derived
decision from Microsoft's Project scheduling documentation, not a measured MSP
run. Verification: the focused core test class passed 51 tests with 0 failures,
errors, or skips. The remaining constraint/link combinations, varied calendars,
lag types, and MPO persistence remain open.

### M2.2 constraint publication failure after commit (#794, 2026-10-20)

`ScheduleService.setConstraint` now records `TaskConstraintEdit` before
dispatching the ObjectEvent. Previously, an observer exception escaped after
changing the task but before adding Undo history. A deterministic throwing
listener verifies the commit-stage policy: publication failure propagates while
the changed constraint remains undoable and redoable. The listener is removed
in `finally`. A failure during constraint application now restores the captured
type/date before any edit or event is published; the regression checks revision,
Undo state, and event count. Post-commit observer failure behavior is unchanged.
This covers one writer family; broader public/legacy mutation inventory and
observer consistency remain open.

### M2.2 SNET + SF MPO persistence (#794, 2026-10-20)

Added a fixed-date MPO save/reopen/recalculate regression combining a successor
SNET constraint with a predecessor-to-successor zero-lag SF dependency. It
asserts exact task dates before and after reload, constraint kind/date,
dependency kind/lag and reciprocal endpoint adjacency. The initial fixture's
calendar-derived expected finish did not match the scheduled workday; the
fixture now uses explicit start constraints and validates its exact schedule
before saving. Focused `MpoFileImporterTest` passed 1 test with 0 failures,
errors, or skips. Other mutation-family MPO round trips and M2.2 acceptance
remain open.

### M2.2 dependency field-edit publication failure (#794/#409, 2026-10-20)

`DependencyService.setFields` now has a deterministic commit-failure test:
after a valid lag/type edit, an ObjectEvent listener throws during publication.
The original exception propagates after the new fields and revision are
committed; the test verifies the event revision, Undo/Redo, and listener
detachment. This exercises the established post-commit publication policy, not
pre-commit model rollback. The listener test copies event facts while the pooled
event is live. Focused `DependencyServiceTest` passed 43 tests with 0 failures,
errors, or skips. Remaining writer families, failure rollback, MPO round trips,
and M2.2 acceptance remain open.

### M2.2 legacy dependency update publication failure (#794/#409, 2026-10-20)

Added a focused test through `Dependency.doUpdateService`: a throwing observer
receives the committed dependency update, then the original exception
propagates. The test verifies the dependency's dirty state, one revision-tagged
event, and listener detachment. `setFields` and legacy `update` tests share a
small listener helper that copies event facts before the pooled event is
recycled. Focused `DependencyServiceTest` passed 44 tests with 0 failures,
errors, or skips. This does not complete the writer inventory.

### M2.2 edited dependency fields through MPO (#794, 2026-10-20)

Added `MpoFileImporterTest.mpoRoundTripPreservesSetFieldsSsWorkingLagAndRecalculatedSchedule`.
It changes a created FS/0 dependency through `DependencyService.setFields` to SS
with one working-day lag, then verifies type, lag, endpoint identity, and exact
Tuesday 08:00–13:00 successor schedule before and after MPO save/reopen/recalculate
on the standard calendar. Focused command `./gradlew.bat :microproject_exchange:test
--tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSetFieldsSsWorkingLagAndRecalculatedSchedule"
--console=plain` passed (1 test, 0 failures/errors/skips).

MPO schedule metadata now carries the project calendar and only explicitly
assigned task calendars, including each calendar's local differences and base
relationship; a task based on the project calendar is rebound to that restored
project calendar object. The sidecar version remains 1. Restore tolerates absent
calendar fields so older MPO files retain their `content.xml` calendar behavior. Regression
`mpoRoundTripPreservesProjectAndExplicitTaskCalendarsForSetFieldsSsLag` ports
the fixture from `TaskLinkAndBarScheduleTransitionsTest.workingDayLagUsesPredecessorTaskCalendarAcrossNonworkingDays`:
project Tuesday exception, predecessor Wednesday exception, 4h tasks, Monday
SNET, and FS/0 changed by `setFields` to SS + 8h. It checks Thursday
08:00–13:00 before save and after reopen/recalculate, the restored local
Wednesday exception and task-to-project base identity, project-calendar Tuesday
exception, inheritance for the successor (no explicit calendar metadata), and
dependency kind, lag, and endpoint identity. Microsoft documents dependency
endpoints, task calendars, and lag but not the lag-owning calendar; this remains
the existing document-derived predecessor-calendar rule, not measured MSP
behavior ([link tasks](https://support.microsoft.com/en-us/project/link-tasks-in-a-project),
[scheduling tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes),
[lead/lag](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task)).
The focused calendar regression and `releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave`
(which loads the checked-in `legacy-assignment-baseline-cost-probe.mpo` released
fixture and verifies its pre-calendar-metadata path) passed 2 tests with 0
failures/errors/skips. This closes only the custom-calendar MPO
persistence slice; M2.2 remains open.

Integrated MPO focused rerun after inheritance review (2026-10-20):
`./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesProjectAndExplicitTaskCalendarsForSetFieldsSsLag" --tests "com.microproject.exchange.MpoFileImporterTest.releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave" --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSetFieldsSsWorkingLagAndRecalculatedSchedule" --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSfDependencyAndSuccessorStartConstraint" --console=plain --no-daemon --max-workers=1`
passed 4 tests with 0 failures/errors/skips. The calendar test also asserts the explicit predecessor calendar's restored base is the same restored project-calendar instance. `git diff --check` passed; only the existing LF-to-CRLF warnings for the two edited documentation files were reported.

### M2.2 SNET + FF MPO persistence (#794, 2026-10-20)

Ported both fixed-date cases from `TaskLinkAndBarScheduleTransitionsTest`:
predecessor finish before and after successor SNET. The MPO test asserts exact
predecessor/successor dates before and after reload/recalculation, successor
SNET kind/date, FF/zero lag, and reciprocal endpoint identity. Focused command
`./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSnetAndFinishToFinishBoundary" --tests "com.microproject.exchange.MpoFileImporterTest.releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave" --console=plain --no-daemon --max-workers=1`
passed both tests with 0 failures/errors/skips. A second Sol-run of the same
command reported BUILD SUCCESSFUL; `git diff --check` passed.

The subsequent `mpoRoundTripPreservesSnetAndFinishToFinishBoundary` regression ports both SNET+FF zero-lag cases: predecessor finishes Monday before Tuesday SNET, and Tuesday 17:00 after the Tuesday 08:00 SNET. It asserts exact schedules before and after MPO reload, SNET kind/date, FF/zero lag, and reciprocal endpoint identity. Focused MPO plus old released fixture tests passed 2 tests; the full `MpoFileImporterTest` class passed 118 tests with 0 failures/errors/skips.

### M2.2 SNET + SS zero-lag MPO persistence (#794, 2026-10-20)

Ported both fixed-date SNET + SS cases from
`TaskLinkAndBarScheduleTransitionsTest` into an MPO save/reopen/recalculate
regression. It checks exact schedules on each side of the round trip, the
successor SNET kind/date, SS/zero lag, and reciprocal endpoint identity. The
compatibility rule remains document-derived: SS compares starts and SNET is a
lower bound, as described by Microsoft's [task scheduling documentation](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes).

### M2.2 SNET + SS MPO persistence (#794, 2026-10-20)

Ported both fixed-date cases from `TaskLinkAndBarScheduleTransitionsTest`:
predecessor starts before successor SNET (Mon start, Tue SNET) and starts after
it (Tue start, Mon SNET). The MPO regression compares exact pre-link and linked
schedules, then verifies exact post-reload/recalculate dates, successor SNET
kind/date, SS/zero lag, and reciprocal endpoints. Focused command
`./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSnetAndStartToStartBoundary" --tests "com.microproject.exchange.MpoFileImporterTest.releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave" --console=plain --no-daemon --max-workers=1`
passed both tests with 0 failures/errors/skips. Sol independently reran the
same command after correcting the second boundary to Tue predecessor / Mon SNET;
BUILD SUCCESSFUL. `git diff --check` passed.

After adding SNET+FF and corrected SNET+SS MPO boundary families, the full
`MpoFileImporterTest` class was rerun on the current worktree:
`./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest" --console=plain --no-daemon --max-workers=1`
passed 119 tests with 0 failures/errors/skips. `git diff --check` passed, with
only the expected LF-to-CRLF warnings for the edited docs.

### M2.2 MPO inherited project calendar restoration (#794, 2026-10-20)

When the schedule sidecar contains `projectCalendar`, task records without an
explicit `calendar` now restore as inherited (`null`) calendars. Older sidecars
without `projectCalendar` keep the content.xml-derived task calendar behavior;
explicit task calendars still restore against the restored project calendar.
This prevents content.xml's materialized default task calendar from overriding
the sidecar's project-calendar exception. The SNET + positive-FS-lag cases keep
their original Thursday schedule expectations and verify inheritance before
and after reload. The focused lag, explicit/inherited calendar, and released
legacy fixture tests passed (3 tests); the full `MpoFileImporterTest` class
passed 120 tests with 0 failures/errors/skips.

### M2.2 SNET + positive FS lag across MPO calendar restore (#794, 2026-10-20)

A two-case MPO regression ports the core SNET/positive-FS-lag/project-Tuesday-
exception fixture. It verifies dates, lag, SNET, calendar exception, and endpoints
before and after reload/recalculation. First run exposed actual import behavior:
source tasks had null explicit calendars (inheriting the project calendar), but
after `content.xml` import they held an explicit default calendar, so Tuesday was
counted as working. When new schedule-sidecar `projectCalendar` metadata exists,
restore now treats a missing per-task calendar field as intentionally inherited
and clears the imported explicit calendar. Old sidecars without project-calendar
metadata retain their prior `content.xml` task-calendar behavior; explicitly
stored task calendars remain restored. The original Thursday expectation was
kept. Focused calendar/inheritance/legacy tests passed 3 tests, and full
`MpoFileImporterTest` passed 120 tests with 0 failures/errors/skips. Sol reran
the three focused tests successfully. Before explicit recalculation the imported
dependency boundary may reflect stale schedule dates; the tested contract is the
schedule after recalculation, which matches the source fixture.

### M2.2 constraint application rollback (#794, 2026-10-20)

`ScheduleService.setConstraint` now compensates when `setScheduleConstraint`
throws during application, restoring the captured type/date before rethrowing
the original failure (a compensation failure is suppressed on it). A regression
injects a one-time failure after the SNET date setter mutates state and verifies
the prior constraint, undo availability, mutation revision, and ObjectEvent
count are unchanged. The post-commit observer-failure/Undo behavior remains
covered separately. `./gradlew.bat :microproject_core:test --tests
"com.microproject.pm.scheduling.ScheduleServiceConstraintTest" --console=plain`
passed 7 tests; `git diff --check` passed.

### M2.2 dependency field application rollback (#794, 2026-10-20)

`DependencyService.setFields` now restores lag and type when applying the type
fails with a runtime exception or error after lag has been assigned. The focused
regression passes a null kind to fail deterministically at that point and checks
fields/endpoints, revision, Undo state, and the ObjectEvent stream remain
unchanged. Existing publication-failure behavior remains a separate post-commit
contract.

`TaskModeService.apply` now captures each affected project's `ProjectTaskSnapshot`
and restores project schedule/model state if applying a selection fails,
including the task whose setter throws after changing its mode. It also restores
the exact pre-apply manual date fields and rethrows the original failure. The
focused two-project regression checks task modes, schedule values, manual dates,
and dirty flags after the second project's recalculation fails; no result/change
object is returned. Successful Undo/Redo remains covered by the existing test.

The regression now also checks both project revisions, shared Undo/Redo
availability and names, zero ObjectEvent/ScheduleEvent notifications, and the
identity of an injected later-project exception. Its first run exposed three
leaked schedule notifications. `TaskModeService` now batches events and revision
scopes for affected projects and discards pending publications/history on
failure. Focused `TaskModeServiceTest` passed 2/2; full
`:microproject_core:test --console=plain --no-daemon --max-workers=1` passed
1,117 tests across 240 XML suites (0 failures/errors/skips). Details and next
gap are recorded in `docs/work/issue-794.md`.

Sol reran the three recent M2.2 rollback families together on the integrated
worktree: `./gradlew.bat :microproject_core:test --tests
"com.microproject.pm.scheduling.ScheduleServiceConstraintTest" --tests
"com.microproject.pm.dependency.DependencyServiceTest" --tests
"com.microproject.pm.task.TaskModeServiceTest" --console=plain --no-daemon
--max-workers=1` passed (56 tests total; 0 failures/errors). `git diff --check`
passed; only expected documentation LF-to-CRLF notices were reported.

### M2.2 FF lead across project-calendar exception (#794, 2026-10-20)

`TaskLinkAndBarScheduleTransitionsTest.finishToFinishTwelveHourLeadUsesBoundaryAcrossDatedCalendarException`
uses a Wednesday 08:00–17:00 predecessor, Tuesday project-calendar exception,
and one-day successor. Microsoft documents negative lag as lead/overlap but not
the exact dated-calendar boundary, so the rule is recorded as a document-derived
compatibility inference, not an MSP result. A -12h FF lead resolves to Monday
12:00; with successor SNET Monday 08:00, the schedule is Monday 08:00–17:00.
Predecessor and unrelated-task dates remain fixed. The earlier -8h candidate was
rejected as an invalid oracle: the strict chronological finish boundary placed
Monday 17:00 before required Wednesday 08:00. The focused test and full
`TaskLinkAndBarScheduleTransitionsTest` class passed (52 tests); `git diff --check`
passed. Source: https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task

MPO persistence coverage: `MpoFileImporterTest.mpoRoundTripPreservesFinishToFinishLeadAcrossProjectCalendarException`
asserts the Wednesday predecessor, Monday constrained successor, unrelated
Monday task, Tuesday nonworking exception, SNET/FF/-12h values, and reciprocal
dependency endpoints before save and after reload/recalculation. The focused
exchange test passed (1 test, 0 failures/errors/skips); this verifies MPO
round-trip behavior and does not constitute a physical MSP experiment.
Sol also reran the full importer class:
`./gradlew.bat :microproject_exchange:test --tests
"com.microproject.exchange.MpoFileImporterTest" --console=plain --no-daemon
--max-workers=1` passed 121 tests with 0 failures/errors/skips.

Superseded by the focused MPP READ proof recorded in
[`docs/work/issue-794.md`](work/issue-794.md): fixed `New Product.mpp` contains
UID 24→27 FS−15 days, verified with raw MPXJ fields, imported schedule
semantics, and MPO save/reload. The OpenProj 1.4-derived fixture is independent
of the product writer, but was not newly authored for this test. The Microsoft
lead/overlap rule is document-derived, not a measured MSP result:
https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task

### M2.2 TaskModeService success through MPO (#794, 2026-10-20)

`MpoFileImporterTest.successfulManualModeChangeSurvivesUndoRedoAndMpoRecalculation`
uses `TaskModeService.apply` to switch a scheduled task to MANUAL, verifies the
captured manual dates, exercises Undo/Redo, then saves/reopens/recalculates MPO
and asserts manual mode, manual dates, and effective start/finish remain exact.
The focused test passed (1 test, 0 failures/errors/skips). This combines the
service success path with MPO persistence; the existing `taskSchedulingModeSurvivesMpoRoundTrip`
test remains the direct model-field persistence case.

The older MPP READ gap note is superseded by the verified fixture and evidence
in [`docs/work/issue-794.md`](work/issue-794.md). No production MPP writer is in
scope per the recorded 2026-10-12 decision above.

### M2.2 #403 linked-child replacement reconciliation (2026-10-21)

`ProjectFactorySubprojectReplacementTest` verifies success reconciliation, rollback after projection failure, and rollback after portfolio node/resource-pool membership has changed. Success exposes revisions on master/old/new child only after reconciliation and publishes parent/portfolio events. Failure restores the projection, portfolio node and membership, pool membership, revisions, Undo state, and observers. The portfolio update is deferred until the mutation scopes commit. Core full tests passed: 1,112 tests across 240 XML suites, 0 failures/errors/skips.

`MpoFileImporterTest.masterMpoPersistsLinkedChildReplacementFromUpdatedChildFile` updates the linked child MPO on disk, loads the replacement, replaces the current child projection, saves/reopens the master MPO, and verifies stable reference ID, identical embedded/extracted child bytes, replacement task identity, and no stale task. Focused test passed 1/1; full `:microproject_exchange:test --console=plain --no-daemon --max-workers=1` passed 308 tests, 0 failures. The master embeds linked-file bytes; this does not persist unsaved in-memory child changes. External-task and shared-resource-pool mutations were not present in the rollback fixture and remain unverified.

### M2.3 Baseline 1 Selected Tasks Robot route (#794, 2026-10-20)

Added `TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo`. It physically selects Baseline 1 + Selected Tasks, confirms overwrite, checks selected and non-selected scope, Baseline 0/current values, Ctrl+Z/Ctrl+Y, and MPO reload for undone/redone state. `compileGuiTestJava` passed in the GUI runner; both physical attempts were stopped before route input by the pre-existing `ProjectLibre へようこそ` window (PID 52244, Java, bounds overlap the test window). The guarded runner retained failure and did not retry/suppress it. Exact targeted command: `.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo -Locale ja -Scale 1.0`. JUnit: `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.xml`; latest overlap evidence: `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo-ja-1-0-bda2a5fbeb294a4d9f9065ed9a71901c/`. On a later run, after normally closing the welcome dialog and minimizing the empty workspace app frame, the test passed (1 test, 0 failures/errors/skips); no display setting was changed. Artifacts: `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo-ja-1-0-57326019b2774286821f2e7dc0c54624/`. Physical route verified overwrite confirmation, Undo/Redo, and MPO reload.

Supporting model check: `ProjectScheduleBehaviorTest.overwritingSelectedBaselineRestoresPriorAndAbsentTaskSnapshotsOnUndoRedo` passed via `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.ProjectScheduleBehaviorTest.overwritingSelectedBaselineRestoresPriorAndAbsentTaskSnapshotsOnUndoRedo" --console=plain --no-daemon --max-workers=1`. It covers selected-scope overwrite, outside-selection and unrelated-slot preservation, assignment baselines, and Undo/Redo; it does not substitute for the blocked Robot route.

### M2.3 Mark on Track assigned-task progress preservation (#794, 2026-10-20)

`TaskProgressServiceTest.markOnTrackPreservesImportedAssignmentProgressAndWorkAcrossUndoRedo` confirms that Status Date updates task-level percent-complete while independent assignment percent, Actual Work, and Remaining Work stay unchanged before/after apply, Undo, and Redo. The focused `TaskProgressServiceTest` class passed via `.\gradlew.bat :microproject_core:test --tests "com.microproject.pm.task.TaskProgressServiceTest" --console=plain --no-daemon --max-workers=1`. This is the Mark on Track command contract; Update Project has separate assigned-actuals coverage. No product code changed.

The physical run later passed after clearing that verified workspace-app interference: the welcome dialog was normally closed and the empty app frame was minimized; no display setting changed. Exact command `.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo -Locale ja -Scale 1.0` passed (1 test, 0 failures/errors/skips). JUnit is `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.xml`; run artifacts are under `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.recaptureBaselineOneSelectedTasksPreservesUnselectedAndBaselineZeroAcrossUndoRedoAndMpo-ja-1-0-57326019b2774286821f2e7dc0c54624/`. The Robot route passed the overwrite confirmation and physical Undo/Redo; both MPO reload states preserve selected Baseline 1, the non-selected task's slot, Baseline 0, and selected current values.


### M2.3 Mark on Track assignment persistence through MPO (#794, 2026-10-20)

MpoFileImporterTest.markOnTrackPreservesIndependentAssignmentProgressAndWorkAcrossMpoRoundTrip applies Mark on Track to an assigned task with independent assignment percent, Actual Work, and Remaining Work, then saves/reopens MPO. It asserts task progress changes, assignment state and task/assignment work totals persist, and project Status Date reloads exactly. Focused command \.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.markOnTrackPreservesIndependentAssignmentProgressAndWorkAcrossMpoRoundTrip" --console=plain passed (1 test).

### M2.1 #414 input-path ownership audit (#794, 2026-10-20)

Focused source checks passed on worktree HEAD `40f5ee8d575a3327e727b9f62182c07aa55d2178` (working changes present): `UiInputArchitectureTest`, `MicrosoftShortcutsRootPaneTest`, and `GanttWheelZoomTest` (20 tests; 0 failures/errors/skips). They confirm no production KeyListener path, unique root-pane shortcut writes, one Gantt wheel owner, and balanced GlobalPreferences listener registration.

A deeper route trace found `MenuFactory.initializeJMenuItem` installs `.accelerator` values on real menu items; `MenuManager` uses `ExtMenuFactory`, and `GraphicManager` installs root-pane bindings for overlapping Ctrl+N/O/S/P/X/C/V/F/Z/Y. The classic document menu is attached by `MicroProjectShell`; duplicate registration is therefore present when those menus are active. Actual double-dispatch ordering was not measured. The bounded fix removes only exact overlaps from document menus using the root-pane InputMap, preserving unrelated accelerators and PrintPreview's separate toolbar use.

`DocumentMenuAcceleratorTest` verifies exact-overlap removal, unrelated and
parent-only keys, null-map behavior, and MenuManager forwarding (3 tests).
Focused architecture tests passed (20 tests); the UI suite passed 1,186 tests
before the third focused test was added, which passed separately. Exact commands:
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.menu.DocumentMenuAcceleratorTest" --console=plain` and
`.\gradlew.bat :microproject_ui:test --console=plain --no-daemon --max-workers=1`.
`git diff --check` passed. The physical classic-menu shortcut route is recorded
in the succeeding section; no display-resolution change was made.

The first Robot attempt used the new `ClassicMenuRootPaneShortcutGuiAcceptanceTest`
fixture and failed before route input while awaiting the task spreadsheet; its
captured screen showed WindowsTerminal covering the application, and the runner
recorded `GUI_ENVIRONMENT_CONTENDED` (PID 72400). Artifact directory:
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.ClassicMenuRootPaneShortcutGuiAcceptanceTest.physicalIndentShortcutMutatesOnceWithClassicDocumentMenuAndUndoRedo-en-1-0-adf16515bfd74283a815e0905d317235/`.
Review found the fixture read `getCurrentFrame()` immediately after adding the
project; it now retains the returned `DocumentFrame`, awaits visibility, and
passes its root-pane map to `MenuManager` before constructing the menu. The
corrected test compiled with `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1`; GUI was not rerun because contention remained. The original command was
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.ClassicMenuRootPaneShortcutGuiAcceptanceTest.physicalIndentShortcutMutatesOnceWithClassicDocumentMenuAndUndoRedo -Locale en -Scale 1.0`; it is not physical route evidence.

### M2.1 #414 physical classic-menu shortcut route (#794, 2026-10-20)

After correcting the fixture's menu creation and retaining the returned
`DocumentFrame`, the physical Robot journey passed at 2560×1440, English, 100%:
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.menu.ClassicMenuRootPaneShortcutGuiAcceptanceTest.physicalIndentShortcutMutatesOnceWithClassicDocumentMenuAndUndoRedo -Locale en -Scale 1.0`.
Result: 1 test, 0 failures/errors/skips. The real root-pane Ctrl+. Indent
shortcut ran after physical task selection; model hierarchy changed, one
physical Ctrl+Z restored the original parent state, and Ctrl+Y reapplied it.
The fixture menu items come from `ExtMenuFactory` after the same root-pane map
is supplied, and the overlapping Ctrl+N accelerator is absent. Test result:
`modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.menu.ClassicMenuRootPaneShortcutGuiAcceptanceTest.xml`.
Run logs are under
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.menu.ClassicMenuRootPaneShortcutGuiAcceptanceTest.physicalIndentShortcutMutatesOnceWithClassicDocumentMenuAndUndoRedo-en-1-0-1453d4908fe84754a23850789dfbb698/`.
No display setting changed. The runner reported a passing GUI gate; my outer
PowerShell wrapper then returned 1 because its `$LASTEXITCODE` guard was null
after invoking a PowerShell script, not because the test failed.

### M2.1 #414 spreadsheet/root-pane shortcut ownership (#794, 2026-10-20)

At worktree HEAD `40f5ee8d575a3327e727b9f62182c07aa55d2178` (working changes
present), `CommonSpreadSheet` now removes exact root-owned Ctrl+X/C/V,
Ctrl+Space, Shift+Space, and Ctrl+Shift+Space bindings from its focused and
ancestor InputMap chains. `SpreadSheet` repeats that cleanup after registering
its transfer handler. The spreadsheet also removes ETable's ancestor Escape
action; the root Escape command cancels an active editor first and otherwise
stops Format Painter. Generic `CommonTable` retains its editor-local Escape
cancellation, disabled outside editing.

The root ownership inventory initially exposed duplicate ancestor clipboard,
selection, and Escape mappings. A read-only review and focused map/action test
confirmed Escape's two ancestor entries resolved to enabled
`BasicTableUI$Actions` (`cancel`) even with no editor. The regression now checks
all root-pane keys against both spreadsheet map chains; only focused Escape to
`cancelEdit` is allowed for in-editor cancellation, and ancestor Escape must be
absent. Focused UI verification passed 30 tests (0 failures/errors/skips):
`.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.MicrosoftShortcutsRootPaneTest" --tests "com.microproject.pm.graphic.UiInputArchitectureTest" --tests "com.microproject.pm.graphic.spreadsheet.SpreadSheetEditingBindingsTest" --tests "com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetSelectionStateTest" --tests "com.microproject.pm.graphic.spreadsheet.common.CommonTableEscapeKeyTest" --console=plain --no-daemon --max-workers=1`.
`compileGuiTestJava` passed, and `git diff --check` passed.

Physical verification used the existing
`TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload`
journey at English/100%, 2560×1440. Ctrl+Space selected the full source row;
a physical Name-cell click restored ordinary row selection. With continuous
Format Painter active, F2 opened the Name editor; Escape canceled editing and
left Format Painter armed; a later Escape stopped it. Final run passed 1/1:
`.\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload -Locale en -Scale 1.0`.
JUnit: `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.xml`;
run directory:
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload-en-1-0-f7f23b720c4242ee88cfeed2b1023050/`.

The first focused Robot attempt was stopped before input by a foreign
WindowsTerminal window (PID 72400); the overlapping attempt artifacts remain
under `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload-en-1-0-5b60afed4e4648d9b883bdaf608bbc04/`. An uncontended run then exposed a
fixture mistake: it selected the non-editable Indicator column before F2. The
failure screenshot showed the selected source row; the fixture was corrected
to click Name and then passed. That diagnostic run is retained under
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload-en-1-0-a16f9b1bc09a4a31a722b9b3a62ed7e2/`. No resolution or display scale changed.

### U-32 physical keypad zoom re-verification (2026-10-20)

On the current worktree, the focused Robot case passed in Japanese at UI scale
1.0 on the unchanged 2560×1440 desktop. It physically sent Ctrl+Numpad Divide
and Multiply from both Gantt and spreadsheet focus, observed one root-pane
dispatch per chord and inverse zoom steps, and verified unchanged task dates,
task-table viewport, and Gantt vertical viewport. The GUI runner regenerated
`installDist`. Command:

`& .\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotCtrlNumpadDivideAndMultiplyZoomActiveGanttFromGanttAndSpreadsheetFocus -Locale ja -Scale 1.0`

Result: 1 test, 0 failures/errors/skips. JUnit:
`modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.xml`.
Artifact:
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotCtrlNumpadDivideAndMultiplyZoomActiveGanttFromGanttAndSpreadsheetFocus-ja-1-0-fd8c8336e33c45fbb36f2b86aa46ebc6/`.
The run temporarily minimized and restored the overlapping WindowsTerminal
window (PID 72400); no screen geometry or UI scale changed.

The complementary non-Gantt no-op journey was added to the same GUI acceptance
class. On worktree HEAD `40f5ee8d575a3327e727b9f62182c07aa55d2178`,
`robotCtrlNumpadZoomIsNoOpWhenResourceViewIsActive` switched the active top view
to Resource View, physically sent Ctrl+Numpad Divide and Multiply, and verified
that the active view stayed Resource View, Gantt zoom and viewport were
unchanged, and predecessor/successor start/end values were preserved. Windows
11, 2560×1440, Japanese/100% UI scale; no display settings changed. Command:

`.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest" --console=plain`

Result: GUI compilation succeeded; 9 tests passed, 0 failures/errors/skips.
JUnit: `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.xml`.

### M2.1 #408 U-26 GraphicManager physical outcome and reversibility (2026-10-20)

The existing `U26SpreadsheetInputTransactionGuiAcceptanceTest` physical
transaction now observes the actual application owner, `GraphicManager`, and
asserts its `RibbonCommandResult`: command ID `SpreadsheetCellEdit`, status
`CHANGED`, `selectedTaskIds` equal the natural selected row IDs captured before
input, `affectedTaskIds` equal the target task, and `activeViewId` equal the
active top view. The physical field transaction commits 10%; model and rendered
value show 10%, Undo/Redo show 0%/10%, and the existing MPO reload assertions
remain in the journey.

Verification passed: `compileGuiTestJava`; full GUI runner command
`& .\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest -Locale ja -Scale 1.0`;
JUnit 1 passed, 0 failures/errors/skips. The run was Japanese at 100% on the
unchanged 2560×1440 desktop. Artifact:
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest-ja-1-0-ec8e471d416547cd83318f64f2c8fcfe/`.
The first attempt contended with WindowsTerminal PID 72400; the window was
minimized/restored before the successful rerun. No display change was made.

The filtered/grouped/sorted physical edit slice is complete; its exact current
worktree evidence and remaining M2.1 criteria are tracked in
[docs/work/issue-794.md](work/issue-794.md).

### M2.1 #414 two-document shortcut isolation (2026-10-20)

Added `TwoDocumentShortcutIsolationGuiAcceptanceTest.physicalCtrlDeleteAndUndoStayWithEachActiveDocument` and U-36 in `TEST_PLAN.md`. With two independent project windows open, Robot physically selected each task Name cell and sent the document-root Ctrl+Delete, Ctrl+Z, and Ctrl+Y routes. Assertions prove the inactive document's task value is preserved after clear, undo and redo, and the selected row still resolves to its own task after switching back. Uses the existing GraphicManager shortcut/action pipeline; no duplicate command wiring was added.

Verification passed: `compileGuiTestJava`; `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TwoDocumentShortcutIsolationGuiAcceptanceTest" --console=plain` (1 test passed, 0 failures/errors/skips). Worktree HEAD `40f5ee8d575a3327e727b9f62182c07aa55d2178`; Windows 11, Java 25.0.4.1, ja-JP, 100% UI scale, 2560×1440, unchanged resolution. The first attempt failed because the two windows overlapped and the click intended for Alpha landed without activating it; the failure artifact showed Beta covering it. The passing run places windows apart before physical interaction. #414 and M2.1 remain open for their other routes and gates.

### M2.2 Task progress batch failure atomicity (#794, 2026-10-21)

`TaskProgressService.markOnTrack` and `setPercentComplete` now apply through
`Project.runAtomicModelCommand`, with project and selected-task detail snapshots
restored on a later-task exception. New deterministic tests exercise failure
after the second task has already changed and verify task/assignment progress,
work, actuals, remaining work, baseline state, dates, dirty flags, revision,
Undo/Redo state and names, zero object/schedule notifications, and original
exception identity.

Focused `.\gradlew.bat :microproject_core:test --tests
"com.microproject.pm.task.TaskProgressServiceTest" --console=plain --no-daemon
--max-workers=1` passed 10 tests. Full `.\gradlew.bat :microproject_core:test
--console=plain --no-daemon --max-workers=1` passed 1,119 tests across 240
suites (0 failures/errors/skips); `git diff --check` passed. Tested HEAD:
`1ca2b41485e4403e1d3b76d2fa0d8dd903c20217` (working tree changes included).
The selected-task baseline overwrite failure path now restores prior and absent
snapshot slots, assignment imported baseline overrides/cost, dirty flags, and
discards revisions, Undo state, and object/schedule notifications. Focused
`ProjectScheduleBehaviorTest` and full core test run passed; details and tested
SHA are in [`docs/work/issue-794.md`](work/issue-794.md). Baseline rollback MPO
reopen coverage was added in a later 2026-10-21 slice in the issue packet;
broader M2.2 and #794 gates remain open.
### M2.2 SNET + SF working-time lag across project-calendar exception (#794, 2026-10-21)

Added a fixed-date SF + 8h lag case with a Tuesday project-calendar exception,
successor SNET, and an unrelated task. The predecessor starts Monday 13:00 and
finishes Monday 17:00; the lag boundary is Wednesday 13:00, and the successor
is Wednesday 08:00–13:00 after recalculation. The core regression verifies
pre-link state, exact linked state, intended-task-only changes, and link
Undo/Redo. The MPO regression verifies dates, SNET kind/date, SF type/lag,
reciprocal endpoint identity, unrelated-task stability, and the calendar
exception after save/reopen/recalculate.

This is a document-derived compatibility decision, not measured MSP behavior:
Microsoft documents SF dependency endpoints, working calendars, and lead/lag,
but does not specify which calendar owns lag. We retain the existing
predecessor-effective-calendar product rule. Sources:
[Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project),
[How Project schedules tasks](https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes),
and [Add lead or lag time to a task](https://support.microsoft.com/en-us/project/add-lead-or-lag-time-to-a-task).
