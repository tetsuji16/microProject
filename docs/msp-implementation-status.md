# Microsoft Project compatibility implementation status

Reviewed 2026-10-12. This report records completed implementation slices and
remaining work against [the roadmap](msp-compatibility-roadmap.md).
**The complete compatibility objective is not achieved.** Gate issues remain
open; an inventory or passing narrow test does not close an entire milestone.

## Implemented changes

- Standalone scope excludes connected Microsoft server/cloud services. MPO is
  the native/main save format; MPP remains a separate external exchange gate.
- Resource Sort Order and header sorting share the existing single-field,
  view-only projection sorter. Selection identity and model resource order are
  preserved; Workspace fields and POD wire contracts are unchanged. English
  and Japanese labels are provided. Multi-key/outline/renumber parity remains open.
- Shared button diagnostics recognize completed modal cancellation and fresh
  matching command outcomes. Stale no-change results cannot conceal a silent
  command failure; actual failed/rejected outcomes remain visible. Robot
  coverage includes repeated cancellation, injected route failure and recovery.
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
  baselines and native MPO persistence are covered. Baseline assignment/work
  distributions and elapsed-duration XML normalization remain explicit gaps.
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
| M1 / #793 | Current Swing ribbon is retained and GUI work follows the repo-contained gates in `docs/gui-quality-gate.md` and `docs/gui-recovery-sequence.md`; no separate skill package is required. This container has no physical Robot desktop, so the latest Office appearance and full locale/DPI matrix remain unverified pending the Windows GUI workflow. Continue ordered implementation work while keeping that evidence gate open. |
| M2 / #794 | One production Update Project transaction is repaired. Other mutation families, revision journal/atomic observer publication, immutable projections and missing commands remain open. Do not mark #408–#411 complete from this slice. |
| M3 / #796 | MPO (`.mpo`) is the authoritative native and default save format; backward-read work remains a gate. MPP (`.mpp`) is a separate external interoperability format: reading exists, writing is absent because pinned MPXJ has no MPP writer. A genuine writer and independent semantic coverage are still required. XML elapsed baseline units, assignment baselines/contours and broad external format semantics remain incomplete. POD is frozen legacy import-only coverage. |
| M4 / #797 | Local automation and extension surfaces were inventoried. VBA/macros, COM automation/add-ins, VSTO and local Office add-ins remain unsupported or unverified. Project Server/PWA/Project Online/Microsoft Planner cloud services are outside the standalone product goal and are not blockers. GNOME Planner file exchange, local Team Planner, linked/master projects and offline collaboration remain separate in-scope desktop/file behaviors. |
| M5 / #795 | Local domain/exchange/report/script checks and fresh install validation must be paired with CI and GUI evidence. No complete-compatibility release is justified. |

## Dependency migration boundary

JasperReports remains at 6.21.5. The resolved and packaged runtime still includes
BeanUtils 1.11.0 and Commons Collections 3.2.2. Official JasperReports 7 migration
requires conversion of the old report templates using Jaspersoft Studio 7+;
no official standalone converter was established in this environment. Do not
upgrade only the dependency or apply a blanket exclusion: that previously
broke runtime behavior. Issues #242, #752 and #753 remain open.

## Verification results

Local JDK 25 runs cover all eight module test tasks: core 905, exchange 223,
UI 1,053, reports 12, application 35, bootstrap 14, contrib 2 and ribbon 5
(2,249 tests total, zero failures/errors, one documented core skip).
The maintenance-script suite passes 10 tests. The calendar XML fixture also
passes in separate startup-timezone JVMs for UTC, Tokyo and Mexico City.
Two UI test fixtures originally formatted UTC model dates in the host timezone;
the fixtures now use the production canonical UTC formatter. No date-editor
production behavior was changed to satisfy those assertions.

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
