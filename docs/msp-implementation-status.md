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
  verified the canonical dialog route and cancellation result. Multi-key,
  outline, and renumber parity remain open.
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
| M1 / #793 | Current Swing ribbon is retained. The Windows full GUI workflow at commit `3a70041` passed run [38320038198](https://github.com/tetsuji16/ProjectLibre/actions/runs/38320038198), including the en/ja × 100/125/150% matrix and evidence upload. This is microProject acceptance evidence, not Microsoft Project capture. The user confirmed neither target client is installed, so the dated, source-linked document-derived comparison table for Microsoft 365 Project and Project 2024 remains missing; narrow-width reachability/accessibility evidence must be linked explicitly before closure. |
| M2 / #794 | The Resource Sort menu/header physical route and one Update Project transaction have evidence. Remaining issue rows are finite in the roadmap table below: M2.1 sort boundary/task-renumber cases; M2.2 dependency, failure-atomicity and mutation-family coverage; M2.3 resource/assignment/progress and baseline edit workflows; M2.4 command inventory, views, reports, print and locale/DPI evidence. One workflow does not close a package. |
| M3 / #796 | MPO (`.mpo`) is the authoritative native and default save format; earlier MPO read compatibility remains a gate. MPP reading remains in scope, while MPP writing is excluded by the user's 2026-10-12 decision. Assignment scalar/timephased work/cost baseline transfer, elapsed-duration normalization, literal zero/missing distinctions, XML/MSPDI and MPO round trips now have focused evidence at `81dc3f5` and its parent checkpoints. The independent MPP-read edition/build semantic manifest and remaining format rows in the roadmap are incomplete. POD remains frozen legacy import-only coverage. |
| M4 / #797 | On 2026-10-12 the user explicitly excluded Microsoft Project VBA/macros, its macro-facing object model, COM automation, COM/VSTO add-ins, and local Office add-ins. These rows are EXCLUDED from the compatibility target; #797 can close as not planned. Native microProject local automation/extensions and offline collaboration are not excluded. |
| M5 / #795 | The full GUI run above is evidence for that earlier implementation head only. Final release still requires the exact final head's build, install/package launch, full GUI matrix, in-scope MPP-read/XML/MPO checks, POD frozen-fixture check and dated residual-coverage report. It remains blocked while M1/M2/M3 rows are partial. |

### Finite remaining rows for #793/#794

These are acceptance gaps, not scope exclusions. Keep each row open until its
linked evidence contains the required source, fixture, command, observed state
and result.

| Issue/package | Already evidenced | Remaining acceptance rows |
|---|---|---|
| #793 M1 | Swing renderer/ADR retained; Windows run 38320038198 passed the six app locale/scale legs and uploaded evidence. | Publish a dated, state-matched document-derived comparison table for Microsoft 365 Project and Project 2024 using the applicable Microsoft UI sources; record applicability/access date and clearly label inference. Link narrow-width command reachability, focus/accessibility and screenshot-bound checks. No empirical Microsoft capture may be claimed. |
| #794 M2.1 Editing/hierarchy/sorting | Resource name header toggle, numeric Standard Rate sorting, Sort Order menu route, selected resource identity, unchanged model order; physical ja/100% evidence. | Date sort; ties and empty values; ascending/descending expectations; filter/group behavior; selection identity after each; task display sort keeping table/Gantt rows aligned; permanent renumber as a separate confirmed, undoable, persisted action. |
| #794 M2.2 Scheduling/dependencies/transactions | Update Project prevalidation/rollback and linked-task Undo/Redo slice. | Cited expected-date fixtures for FS/SS/FF/SF and lag units/calendars/constraints; verify only intended tasks move; warning/result behavior; Undo/Redo and MPO reopen; inject post-mutation failure and prove model, Undo history and observers are unchanged. Cover remaining mutation families and revisioned projection publication. |
| #794 M2.3 Resources/assignments/progress/baselines | Resource sort is view-only. Assignment work/cost and baseline work/cost import, edit/persistence slices have core/exchange evidence, including MPO reopen. | Selected-vs-all scope, proposed resources, leveling defaults/provenance, assignment totals, status date and actual-progress transitions; manual split preservation; baseline edit/clear/recapture; Undo/Redo and MPO reopen for each changed workflow. |
| #794 M2.4 Views/ribbon/context/reports/print/files | Current sort and Save As physical routes; report export fixture test. | Reconcile each #453/#769 command inventory row to source/status/enablement/physical route/visible result; view-only commands must preserve model; persistence where applicable; locale/DPI for changed routes; report value/rendering comparison and print page-boundary plus English/Japanese labels. Unsupported commands must be absent and documented. |
| #795 M5 | Build 1101 and full GUI run 38320038198 passed at commit 3a70041. | Run release checks against the final exact head, verify packaged launch, MPO older-version reads, MPP read (not write), XML/MSPDI and MPO round trips, frozen POD fixtures, all six GUI legs, and publish the dated residual-deviation/exclusion report. |

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

The Sort split-menu physical journey passed on Windows at Japanese locale and
100% UI scale after refreshing the runnable distribution with
`.\gradlew.bat :microproject_ui:installDist --rerun-tasks --no-daemon --max-workers=1 --console=plain`,
then running
`.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources" -PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --no-daemon --console=plain`.
The journey checks physical header sorting, mouse and Space activation of the
  Sort menu arrow, the shared Sort Order action/dialog, numeric Standard Rate
  ascending results, preserved selection/model order, Workspace field state,
  and cancellation as a no-change outcome. The fixture covers text and numeric
  sorting; date, tie/empty, filter/group, and Gantt-alignment legs remain open.
  Gradle evidence is in
`modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.views.ResourceHeaderSortingGuiAcceptanceTest.xml`
and `modules/microproject_ui/build/reports/tests/guiTest/index.html`.

Fresh install, architecture boundary, boundary fixture, dependency allowlist,
naming, and packaged sample MPP/POD import checks pass. These are local domain
and packaging checks, not Microsoft desktop full-interoperability evidence.
GitHub CI and Windows GUI results are recorded in the associated PR.

## Verification policy

Record the exact command, commit, result and skipped cases with each published
change. The task-local JDK, proxy setup, fonts and generated build artifacts are
not product changes. A passing compile is not a passing test. Failed baseline
behavior is fixed or explicitly reproduced against the original commit;
it is not silently relabeled as successful validation.
