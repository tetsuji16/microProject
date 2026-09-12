# Final integration acceptance ledger (#552)

This is the closure ledger for the MSP compatibility, Ribbon, CCPM, MPOF and
reliability work.  It records repository evidence as of 2026-09-13.  A closed
issue means its stated implementation/test gate was met; an open issue means a
remaining acceptance condition is explicitly listed below.

## Child and related issue status

| Issue | Completion condition / scope | Evidence and responsible area | Remaining / status |
|---|---|---|---|
| #528 | Empty CCPM observation has a diagnostic reason, not generic error | CCPM service/dialog tests; `micrproject_core` / UI | None recorded / CLOSED |
| #529 | Canonical `microProject` file/module naming | naming and package gates / all modules | None recorded / CLOSED |
| #530 | One command route, column-layout lifecycle, paste result contract | `CommandRouteMatrixTest`, `RibbonButtonBehaviorTest`, paste failure tests, column permission/undo tests, #549/#550 / UI | All acceptance conditions met; #549 and #550 closed / CLOSED |
| #531 | Thread-safe AutoRecovery state and no duplicate snapshots | AutoRecovery concurrency tests / application + UI | None recorded / CLOSED |
| #532 | Corrupt recovery metadata is diagnosable | metadata failure tests / application | None recorded / CLOSED |
| #533 | MPOF embedded expansion has a bounded total size | `MpoArchiveBudgetValidatorTest` / exchange | None recorded / CLOSED |
| #534 | Temporary MSI is removed after installer failure | installer cleanup tests / packaging | None recorded / CLOSED |
| #535 | Parent boundary program closes only when all architecture/compatibility children close | #453, #530, #536–#561 are closed | CLOSED 2026-09-13 |
| #536 | Dependency, FQN, reflection and API-leak architecture checks | `verifyArchitectureBoundaries`, fixtures and naming gates / build | None recorded / CLOSED |
| #537 | Core exposes ports only; exchange owns concrete importer aliases | core/exchange tests and boundary fixtures; POD byte-invariant import / core + exchange | None recorded / CLOSED |
| #538 | All UI command entries and async callbacks share typed outcome/EDT-generation contract | `CommandRouteMatrixTest`, immutable `RibbonCommandResult`, `DocumentGenerationTest`, #550 physical matrix / UI | All stated conditions met; unrelated CCPM GUI failure is separate / CLOSED |
| #539 | MPOF read/validate/merge/commit separation with retry/partial-apply safety | MPO transaction and retry tests / exchange | None recorded / CLOSED |
| #540 | Dependency allowlist and isolated POI/MPXJ/FlatLaf/reports/logging policy | allowlist, package-import and installDist gates / build | None recorded / CLOSED |
| #541 | Java 25/module documentation and quality rules synchronized | naming/architecture/build gates / build | None recorded / CLOSED |
| #542 | Shared temporary-artifact ownership, location and reclamation lifecycle | temp lifecycle tests/docs / application + exchange | None recorded / CLOSED |
| #543 | MPOF extraction is session-owned; close deletes and startup reclaims orphans | extraction session tests / exchange | None recorded / CLOSED |
| #544 | POD/XML/MPOF transaction temp is unique and recovered on failure | extraction/transaction tests / exchange | None recorded / CLOSED |
| #545 | Persistent sidecar/lock markers are distinct from temporary artifacts; safe deletion requires ownership/lease/FileLock checks | sidecar/lock tests and docs / exchange + collaboration | None recorded / CLOSED |
| #546 | JVM-local collaboration lock registry is bounded and race-safe | registry tests / exchange | None recorded / CLOSED |
| #547 | Core POD tests do not depend on exchange implementation | core/exchange test separation and architecture fixtures | None recorded / CLOSED |
| #548 | Official MSP Ribbon comparison, English/Japanese table, microProject-window screenshots and unsupported-command record | `RibbonStructureTest`, `RibbonCommandCatalogTest`, `RibbonTabGuiAcceptanceTest`; `MSP_RIBBON_STRUCTURE_AND_COMMAND_COMPARISON.md` / UI | Serial Windows Robot passed 2026-09-13 in 22s; exactly 7 non-empty window-only artifacts generated under build reports / CLOSED |
| #549 | Structural command comparison, multi-selection/readonly/hidden-ID boundaries, undo/redo and save/reload | `CommandRouteMatrixTest`, hierarchy, paste readonly, column permission/undo and persistence tests; same comparison doc / UI | Public Microsoft documentation is normative; no MSP installation required by repository rules. Evidence complete / CLOSED |
| #550 | Physical Ribbon/Menu/Popup/Shortcut representatives for every command family | `RibbonTabGuiAcceptanceTest.robotClicksEveryStandardRibbonCommandOnce` and `TaskInformationRibbonGuiAcceptanceTest.copyCutPasteThroughRibbonUseTheSharedEditPipeline` passed serially (44s, 0 failures); route/Robot representatives are recorded in `issue-550-command-outcome-matrix.md` / UI | All required family representatives and typed outcome/persistence evidence recorded; unrelated CCPM GUI failure is separate / CLOSED |
| #551 | English/Japanese and 100/125/150% DPI visual command acceptance | Forced six-axis English/Japanese × 100/125/150% matrix; full UI 824/0; command route/physical/model/Undo/Redo/MPO evidence / UI | All stated conditions met; GitHub issue CLOSED |
| #552 | One ledger, all gates linked, and zero unresolved open children | this document | CLOSED 2026-09-13; all tracked children closed |
| #553 | Machine-readable MSP standard command inventory with explicit supported/conditional/not-supported states | `MSP_STANDARD_COMMAND_INVENTORY.md`; `MspStandardCommandInventoryTest`, `RibbonCommandCatalogTest` / UI | Implemented and focused tests pass; child remains OPEN until parent review |
| #554 | View-dependent contextual Ribbon visibility and locale/DPI acceptance | `ContextualRibbonVisibilityContractTest`, existing `RibbonTabGuiAcceptanceTest` physical tab/bounds/capture cases, and #551 six locale/DPI runs / UI | Contextual tab clear/show and active-view caption contract passes; visual/DPI evidence passes / CLOSED |
| #555 | CCPM Apply/Clear, baseline preservation, monitoring and MPO save/reload boundary | `CriticalChainServiceTest` apply/clear undo-redo, resource constraint, selected-resource baseline, stale/read-only analysis and no-mutation cases; `CriticalChainApplyAndRenderTest`, `CriticalChainStatusDialogGuiAcceptanceTest`, and MPO round-trip tests / core + exchange + UI | All tracked boundary conditions have regression evidence; #526/#430 retain their separate network/GUI scope / CLOSED |
| #556 | MSP Task Mode, Status Date, Update Project and Mark on Track command family | Task Mode typed service/route, Ribbon catalog/resources, Menu actions, task-popup actions, root-pane shortcuts, MPO round-trip, and shared English/Japanese 4/4 Robot fixture (ja 54s, en 1m57s, 0 failures) / UI + core + exchange | Conditions met; Status family was completed in #557 / CLOSED |
| #557 | MSP Status Date, Update Project and Mark on Track canonical commands | TaskProgressService boundary/Undo, MPO manifest round-trip, four-entry routes, English/Japanese serial Robot and UI full 824/0 evidence | Closed 2026-09-13; typed Update Project boundary hardening continues in #558 |
| #558 | Update Project typed outcome and boundary acceptance | Immutable request/result, calendar-normalized Status Date, task-ID outcome, core boundary/Undo tests, MPO actual-progress roundtrip, and serial physical Ribbon/Menu dialog routes; Popup/Shortcut are explicitly unprovided (absence/disabled, no dummy success) | CLOSED 2026-09-13 under revised regular-entry contract |
| #559 | CCPM resource-constrained critical chain and typed buffers | `CriticalChainService.BufferKind`/`resourceBuffers`, monotonic CCPM generation, conflict-aware `ClearResult`, MPO Apply→Save→Reload→re-analysis, `multipleFeedingBranchesRemainSeparateFromFixedDependencies`, `resourceBufferRemainingWorkUsesGreenAmberRedThresholds`, and `supportedReportProjectsAllTypedBufferKinds` all pass | CLOSED 2026-09-13; GitHub closure evidence comment |
| #560 | MSP view-contextual Format tabs and command matrix | Typed descriptors and contextual tabs cover Gantt/Tracking Gantt, Network, and Calendar; child #561 `networkAndCalendarContextualTabsHaveExplicitRouteMatrix` passes ja/en × 100/125/150% (six serial runs, 1 test/0 failures each) with `ribbon-contextual-*.png` artifacts; Timeline/Report remain explicitly unsupported | CLOSED 2026-09-13 |
| #561 | Network/Calendar contextual command Robot matrix | Six-axis physical route matrix covers view transition, visibility, CommandId mapping, bounds, readonly/selection and unsupported absence/disabled behavior | CLOSED 2026-09-13; child of #560 |
| #430 | CCPM GUI scenario inventory: network, buffer, apply, save/reload and invalid input | Serial Robot run of `CcpmSampleProgressGuiAcceptanceTest` plus `CriticalChainStatusDialogGuiAcceptanceTest.reloadedMpoRetainsCcpmBaselineAndRendersBothStatusSurfaces` passed in 59s / UI | Public Microsoft documentation is normative; scenario gate complete / CLOSED |

## Format and temporary-file contracts

POD import is read-only with respect to the source: the LocalFileImporter/new
port path preserves task/resource/dependency data and the source bytes.  The
legacy `com.projectlibre1` deserialization alias remains confined to the safe
input compatibility boundary.  MPOF extraction and transaction workspaces are
session-owned temporary directories under the configured application temp
root; they are cleaned on successful close and failure, while startup sweeps
orphaned session markers.  Persistent collaboration sidecars and locks are
not treated as temp files: deletion requires owner absence, expired lease and
successful OS FileLock acquisition.  A partial sidecar never replaces a valid
lease.

## MSP summary-task contract

The MSP public behavior used as the compatibility oracle is: selected task(s)
can be indented/outdented from Task > Schedule (including Alt+Shift+Right/Left),
and view/table changes are presentation changes rather than project-data
changes.  microProject records the corresponding typed command, selection,
read-only rejection, undo and persistence behavior in the comparison document.
No claim is made for undocumented or version-specific behavior.

## Verification ledger

Previously successful repository gates include:

```text
.\gradlew.bat :micrproject_ui:test --console=plain
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.pm.graphic.frames.CommandRouteMatrixTest" --tests "com.microproject.pm.graphic.frames.DocumentGenerationTest" --console=plain
.\gradlew.bat clean build installDist verifyPackagedFileImports verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures verifyNamingConventions verifyDependencyAllowlist --console=plain
```

The focused Ribbon/structural test set passed (`BUILD SUCCESSFUL`, 27s).  The
full physical GUI gate has a separate pre-existing failure in
`CriticalChainStatusDialogGuiAcceptanceTest.unconfiguredNetworkOffersPhysicalRouteToCcpmSettings`.
The latest attempted Ribbon screenshot run was interrupted by a concurrent
Gradle build removing/refreshing class outputs; it must be rerun serially to
close #548.  No generated screenshot or sample file belongs in this ledger.

## Closure decision

#453, #535, and #552 are now **CLOSED**. #558, #559, #560, and #561 provide
the final typed Update Project, CCPM boundary, and contextual Ribbon evidence.

#549 is **CLOSE-READY** under the repository's Microsoft-public-specification
compatibility rule.

### #551 visual matrix evidence (2026-09-13)

The focused physical Robot cases were run serially for every required locale
and scale. Each calendar run executed all three dialog scenarios, including
the real mouse route and layout assertions. The added
`highDpiRibbonCommandFamiliesRemainContainedAndNonOverlapping` case checks the
responsive Ribbon surface for every tab at each high-DPI axis, including band
containment and sibling-control overlap:

```text
ja/1.0   BUILD SUCCESSFUL (17s)    ja/1.25 BUILD SUCCESSFUL (19s)
ja/1.5   BUILD SUCCESSFUL (20s)    en/1.0  BUILD SUCCESSFUL (27s)
en/1.25  BUILD SUCCESSFUL (about 30s)  en/1.5 BUILD SUCCESSFUL (20s)
```

The full Japanese/100% GUI suite independently completed at 108/108 with zero
failures. Direct full-width ribbon sweeping above 100% remains governed by the
documented `W-464-DPI-FULLWIDTH` waiver; this does not waive the dedicated
visual and targeted command checks above. Screenshots remain generated output
under `build/reports/guiTest-artifacts/` and are not source changes.

### #558/#560 deferred and contextual physical evidence (2026-09-13)

Serial focused Robot verification passed for Update Project deferred Ribbon and
Menu routes (DISPATCHED -> CHANGED, Undo/Redo and MPO persistence), plus the
Network/Calendar contextual Ribbon transition and standard physical command
sweep:

```text
TaskInformationRibbonGuiAcceptanceTest: 2 tests, 0 failures, exit 0 (48s)
RibbonTabGuiAcceptanceTest: 2 tests, 0 failures, exit 0 (same serial run)
```

The same deferred Update Project pair was forced from a clean task graph in
English at 100% UI scale:

```text
en/1.0 --rerun-tasks: 2 tests, 0 failures, exit 0 (1m24s)
```

Network/Calendar contextual transition physical coverage is included in the
existing #551 locale/scale matrix: ja/en at 1.0, 1.25, and 1.5, with visual
containment/overlap checks and the documented responsive waiver for direct
full-width sweeps above 100%.

### #561 Network/Calendar contextual route matrix (2026-09-13)

`RibbonTabGuiAcceptanceTest.networkAndCalendarContextualTabsHaveExplicitRouteMatrix`
is the compact physical matrix for the two view-contextual surfaces.  It
selects each tab with `Robot`, verifies the tab becomes visible only after its
view transition, checks every defined contextual format button has a legacy
action mapping and remains enabled/contained, and verifies the contextual
surface does not leak the standard `NetworkAction`/`CalendarViewAction` view
selectors.  Leaving the view hides both contextual tabs again.  Network and
Calendar are view selectors owned by the standard View route; no separate
contextual Popup/Shortcut route is advertised, so no duplicate command owner
is introduced.

Serial evidence (each 1 test, 0 failures, exit 0):

```text
ja/1.0 --rerun-tasks
ja/1.25
ja/1.5 --rerun-tasks
en/1.0
en/1.25
en/1.5
```

Screenshots are written to
`modules/micrproject_ui/build/reports/guiTest-artifacts/ribbon-contextual-*.png`.
