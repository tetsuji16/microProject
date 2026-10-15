# Microsoft Project desktop compatibility roadmap

**Status:** goal and plan; compatibility is not complete.  
**Reviewed:** 2026-10-12. **Issue inventory:** 34 open issues returned by GitHub.

This document coordinates the goal in [msp-compatibility-goal.md](msp-compatibility-goal.md), the command-level evidence in [MSP_STANDARD_COMMAND_INVENTORY.md](MSP_STANDARD_COMMAND_INVENTORY.md), and the work boundaries below. It does not replace issue discussions or their history.


Current execution evidence and remaining blockers: [implementation status](msp-implementation-status.md).

## Target and evidence

The goal is complete compatibility with Microsoft Project desktop. This is an objective, not a claim about the current product. Cover the current Microsoft 365 Project desktop client and Project Standard/Professional desktop editions; retain Project 2024 as a reproducible perpetual-license baseline. For any claim about current appearance, record observed product name, exact version/build, update channel, architecture, date, Windows version, resolution, scale, locale, and active view. Do not infer a “latest” build from a marketing page. A Microsoft 365 capture and a Project 2024 capture are separate evidence. The OfficeTips image in #765 is a visual style reference, not proof of Project command structure.

The target is the standalone Project desktop application. Project Server, Project Web App (PWA), Project Online, Microsoft Planner/Planner in Teams, Project for the web, and other connected-service authentication, synchronization, publishing, and APIs are outside this product goal. Do not add connected-service imports, clients, or dependencies. This boundary does not exclude local project files, linked/master projects, shared resources, offline collaboration, or microProject-native local automation and extensions.

The complete target must account for:

- Planning semantics: tasks, hierarchy/WBS, resources, assignments, calendars, work/duration/cost, constraints, all link types and lag, manual/automatic scheduling, leveling, progress/actuals, baselines, critical path, reports.
- Desktop interactions: standard and contextual ribbon, Backstage, menus, dialogs, shortcuts, mouse routes, selection/enablement, accessibility, views, printing, Undo/Redo.
- Files: MPO (`.mpo`) is microProject's authoritative native format and default save format; preserve its project data and backward-read support for earlier supported versions. MPP (`.mpp`) is read-only external interoperability; per the product decision recorded 2026-10-12, MPP writing is outside scope and MPO is the replacement native save target. Project XML and each advertised external format have separate read/write coverage. Test semantic round trips against fixtures and report loss/unsupported fields. POD is frozen legacy import-only coverage; retain its wire contract and fixed import fixtures, and add no new POD write behavior.
- Microsoft Project local automation/extensions: on 2026-10-12 the user explicitly excluded Microsoft Project VBA/macros, the Microsoft Project object model as exposed to those macros, COM automation, COM/VSTO add-ins, and local Office task-pane add-ins from product scope. Mark these rows **EXCLUDED**, not unsupported or verified. This does not exclude microProject-native local automation/extensions or local/offline collaboration from their applicable product gates. Connected-service integrations are separately outside scope.

Microsoft-published specifications and support material are normative for behavior and issue closure; an installed MSP client is supplementary, not a prerequisite. Mark inferred rules as document-derived decisions. Current-appearance claims require dated captures with the exact target build. Keep missing, partial, unsupported, and unassessed rows visible.

Junior implementation issues must be executable specifications: named owner/reviewer; dependencies; source and exact relevant behavior; supported inputs and preconditions; ordered steps; expected observable and numeric outcomes; fixture identity; tolerance only where a source defines it; negative/invalid cases; atomic failure behavior; selection and unrelated-state invariants; Undo/Redo; save/reload; test commands; evidence paths. A command dispatch or successful compile alone is not acceptance.

For a scheduling change, use the owning module's fixture conventions and state input dates, calendars, duration units, dependency/lag, constraints, and exact expected start/finish. Derive each result from the cited Microsoft rule and fixture calendar; don't invent tolerance. Include a negative case, an injected failure that leaves model and Undo unchanged, and save/reload when data changes. For example, an FS link with one working-day lag must state predecessor finish, calendar, lag unit, expected successor start and explain any calendar boundary.

For UI work, name the production class/resource and route from physical gesture to canonical action, fixture, visible result and persistence boundary. Verify locale through the app preference, not only JVM locale. Record Robot command/test, screenshot/log path, exact build and scale so a reviewer can rerun without guessing.

## Proposed milestone gates

These are dependency-ordered gates, not delivery dates. GitHub milestones should mirror these names and assign issues only after dependencies are checked.

The connected GitHub interface does not expose milestone create/update operations and no authenticated GitHub CLI is installed. Six open gate-tracking issues (#792–#797, linked above) provide reviewable progress records; they are not native GitHub Milestone objects. Keep the gate table as the source of scope until native milestone support is available.

### Junior work-package sequence

Each package is a small PR with one observable contract. Before coding, check the current source and tests at the exact branch head; the file names below are entry points to inspect, not permission to duplicate an existing implementation.

| Package | Inspect first | Implementation/verification sequence |
|---|---|---|
| M0.1 target manifest | docs/MSP_STANDARD_COMMAND_INVENTORY.md; docs/MSP_RIBBON_STRUCTURE_AND_COMMAND_COMPARISON.md; issues #453/#769 | Record product editions, feature rows, source links, current status, owner issue, fixture/test evidence. Reconcile stale rows against current source and recent issue updates. |
| M0.2 file matrix | modules/microproject_exchange/src/main/java/com/microproject/core/pm/exchange/MspImporter.java; exchange tests and samples; [exchange format inventory](msp-exchange-format-inventory.md) | Identify MPO as native/default save, preserve backward reads, and inventory each external reader/writer separately. MPPReader is used by MspImporter; MPP writing is explicitly out of scope by the 2026-10-12 product decision. Keep MPP read, XML read/write, and POD import-only/frozen as separate contracts. Create fixed fixtures and record every semantic field and exact diff after round trips. |
| M1.1 ribbon visual baseline | docs/architecture/ADR-0002-swing-ribbon-renderer.md; modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties; ModernRibbonPanel; issue #765 | If a Microsoft client is unavailable, compare dated Microsoft-published UI references and dated microProject screenshots as document-derived evidence; record source edition/state/date and explicitly leave unpublished client build/environment facts unstated. Use direct app captures only for observed builds and never claim pixel parity from documentation. Change one design token/layout family per PR and rerun the six-axis visual suite. Never add Flamingo JRibbon. |
| M2.1 editing, sorting, hierarchy | modules/microproject_ui/src/main/java/com/microproject/ui/ribbon/RibbonCommandCatalog.java; spreadsheet/Gantt projection/sort classes; #408/#414/#750/#761 | For sort, test at least three field types (text, date, numeric), equal and empty values, ascending/descending, filter/group, selection identity, Gantt row alignment, and unchanged model order. Keep task permanent renumbering a separate explicit action with its own confirmation/Undo/save contract. Resource Name/Standard Rate header and Sort Order menu passed physical Windows Robot at ja/100%; issue #750 was closed completed on 2026-10-15 for its reviewed menu/header contract. Broader resource multi-key/date/filter/group behavior remains M2.1/#794 scope, not an expanded #750 claim. [Microsoft `Application.Sort`](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort) documents its `Renumber` argument for task sorting; resource permanent renumbering is not an applicable compatibility requirement. Task sort PRs #815/#827 cover date directions, multiple keys, ties, empty/missing values, flat Keep Outline projection, table/Gantt alignment, model invariants, and separate permanent renumber confirmation/Undo/Redo/MPO. `ViewNodeModelCacheHierarchyDependencyTest.taskSortAppliesToFilteredRowsWithinConfiguredGroupsWithoutChangingModelOrder` verifies the document-derived combination rule: filter first, retain configured group buckets, sort visible tasks within each bucket, and leave task order unchanged. This is an Office-consistent product decision, not measured MSP behavior. The PR #827 physical task sort method passed in Windows GUI run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004). |
| M2.2 schedule and dependency semantics | modules/microproject_core scheduling services and task/dependency tests; docs/msp-task-command-contract.md; #403/#409/#410 | Update Project now batches project ObjectEvent and ScheduleEvent notifications with its model transaction: failed apply restores task/project values and publishes no transient object/schedule events; successful commit publishes changes; a COMMIT-stage Undo-listener failure still publishes the already-committed model state. Core regression and physical Project-menu execution cover linked tasks, one Undo/Redo and MPO reopen. PR #813 adds six fixed-date cases: FS/SS with 8h lag and FS/SS/FF/SF with positive 4h lag across Tuesday/Wednesday calendar exceptions; 40 core tests passed with Build and GUI checks. The calendar-ownership rule is a document-derived product decision, not observed MSP parity. Remaining: independently expected dates for the full link/lag matrix under varied calendars/constraints; expected warnings/state for other mutation families; architecture-child reconciliation from #403; revisioned EDT publication and failure atomicity across remaining transaction callers. |
| M2.3 resources, assignments, progress, baselines | modules/microproject_core resource/task services; modules/microproject_exchange MPO tests; #453/#769/#772 | Specify allocation, over-allocation, proposed-resource and leveling options separately. Test selected vs all-resource scope, clear/recompute provenance, assignment totals, progress/status date, baseline slots, Undo/Redo and MPO round trip. Don't remove user-created splits when clearing leveling. |
| M2.4 views, ribbon, reports, print | modules/microproject_ui ribbon resources/catalog, GUI tests; modules/microproject_reports; #453/#765/#769/#770 | For every inventory row, provide enabled/disabled state, physical command route, exact visible result, no-mutation assertion for view-only actions, and locale/DPI evidence. For reports compare fixture values and rendered layout; for print inspect page boundaries and localized text. Record unsupported items as absent with reason, never as success-shaped dummy controls. |
| M3.1 MPP read | MspImporter and exchange import fixtures | Use Project-created MPP with named edition/build. Compare imported task/resource/calendar/link/baseline/actual values to an independently recorded manifest; document unsupported constructs and clear failures. |
| M3.2 MPP write | Product scope decision, 2026-10-12 | Excluded from this product goal at the user's direction: do not implement an MPP writer. MPO is the native replacement save format. Keep MPP read/import coverage explicit; this scope decision does not imply MPP read compatibility is complete. |

For scheduling fixtures, independently compute expected dates from the cited
rule and fixed working calendar. Record working-day and elapsed-day lag
separately; do not substitute unverified illustrative dates for an oracle.

| Milestone | Measurable exit criteria |
|---|---|
| **M0 — Target and coverage baseline** | Tracking issue [#792](https://github.com/tetsuji16/ProjectLibre/issues/792). Freeze dated target editions/source set. Inventory every command, workflow, format and integration with status, owner issue, source and evidence. |
| **M1 — Office-style ribbon foundation** | Tracking issue [#793](https://github.com/tetsuji16/ProjectLibre/issues/793); coordinate #765/#483/#724. Use Swing ModernRibbonPanel and ADR-0002. If Microsoft Project is unavailable, a dated comparison against Microsoft-published Office/Project UI references may be recorded as a **document-derived comparison**, not as an empirical application capture. English/Japanese × 100/125/150% DPI; narrow-width content remains reachable and the selected caption/control is fully visible after horizontal navigation; initial viewport crops are reported rather than called pixel parity. Keyboard/accessibility states verified. Visual gate only. |
| **M2 — Offline desktop workflow parity** | Parent tracking issue [#794](https://github.com/tetsuji16/ProjectLibre/issues/794); split reviewable packages: **M2.1 editing/hierarchy** (#408/#414/#750/#761); **M2.2 scheduling/dependencies** (#403/#409/#410/#411/#453/#769); **M2.3 resources/assignments/progress/baselines** (#453/#769 and children); **M2.4 views/ribbon/context/reports/print/file commands** (#453/#765/#769/#770). Each action needs source, selection/precondition, physical route, visible/model result, disabled reason, unrelated-state invariants, failure atomicity, Undo/Redo if mutating and MPO save/reload. Parent closes only after package acceptance. |
| **M3 — File interoperability** | Tracking issue [#796](https://github.com/tetsuji16/ProjectLibre/issues/796). MPO remains the native/default format and backward-reads earlier supported versions. MPP read/import is an external-format gate; MPP writing is excluded by the user's 2026-10-12 product decision and MPO is the native replacement. Compare IDs, hierarchy, calendars, resources/assignments, links/lag, constraints, dates, progress, baselines, custom fields and represented views; enumerate losses. Independently gate XML and other advertised formats. POD remains frozen legacy import-only coverage. |
| **M4 — Local automation and extensions** | Tracking issue [#797](https://github.com/tetsuji16/ProjectLibre/issues/797). The user's 2026-10-12 decision excludes Microsoft Project VBA/macros, its macro-facing object model, COM automation, COM/VSTO add-ins, and local Office task-pane add-ins; these rows are EXCLUDED from the compatibility goal. Native microProject automation and local/offline collaboration remain in their own product scope and are not removed by this decision. |
| **M5 — Release acceptance** | Tracking issue [#795](https://github.com/tetsuji16/ProjectLibre/issues/795). Automated tests, architecture, packaged launch, GUI quality, accessibility, locale/DPI, file round trips and dated visual comparison pass. Publish coverage report. “Complete” is prohibited while any in-scope row is partial, missing, unsupported or unassessed. |

## Open issue scope review

The following review covers all 34 open issues observed on 2026-10-12. It preserves prior discussion by recommending scope/sequence in this separate register; it is not a rewrite of issue bodies. These milestones are planning groupings, not statements that work is complete.

| Issue | Priority / milestone | Revised boundary, source area, and acceptance refinement |
|---|---|---|
| [#84](https://github.com/tetsuji16/ProjectLibre/issues/84) Cleanup | P3 / M5 | Remove only proven dead/commented code; owning-module tests; preserve serialization and compatibility adapters. No credit by deletion count. |
| [#215](https://github.com/tetsuji16/ProjectLibre/issues/215) Preferences | P2 / M2 | Separate user defaults from project values. Stage each preference; verify restart persistence, locale behavior, and no rewrite of opened project data. |
| [#228](https://github.com/tetsuji16/ProjectLibre/issues/228) Collection sizing | P3 / M5 | Only strict known bounds; review every candidate and explain non-conversions. Performance-relevant tests; conversion count is not a metric. |
| [#242](https://github.com/tetsuji16/ProjectLibre/issues/242) Commons Collections | P2 / M5 | Finish #752/#753 first; prove no 3.x runtime artifact in Gradle or packaged runtime; core/reports/UI and Windows acceptance pass. |
| [#254](https://github.com/tetsuji16/ProjectLibre/issues/254) Locale audit | P2 / M2 | Recount live keys/locales; do not treat stale reported numbers as current. Specify source-key parity, translation workflow, supported-locale smoke gate before pruning. |
| [#388](https://github.com/tetsuji16/ProjectLibre/issues/388) Japanese print label | P2 / M2 | Fix queue wording at source; physical Ctrl+P ja verification plus en regression and correct job count. |
| [#403](https://github.com/tetsuji16/ProjectLibre/issues/403) Table/Gantt architecture | P1 / M2 | Parent only. Close after children, legacy-path removal, multi-view/filter/sort/EDT checks and two-window verification. Not a substitute for feature acceptance. |
| [#408](https://github.com/tetsuji16/ProjectLibre/issues/408) Task commands | P1 / M2 | Enumerate each mutation entrypoint and use one typed boundary with result, Undo and persistence coverage; catalog-only audit cannot close. |
| [#409](https://github.com/tetsuji16/ProjectLibre/issues/409) Revisioned updates | P1 / M2 | Test stale publication rejection, atomic EDT snapshot install, ordering/races, structural edits, and cross-view state. |
| [#410](https://github.com/tetsuji16/ProjectLibre/issues/410) Rollback transactions | P1 / M2 | Inject failure at every transaction stage; model, Undo stack and observers remain unchanged after failure. |
| [#411](https://github.com/tetsuji16/ProjectLibre/issues/411) Value projections | P1 / M2 | Final architecture gate: render/hit test from immutable values; prove no supported path uses legacy mutable row state. |
| [#413](https://github.com/tetsuji16/ProjectLibre/issues/413) Alternative calendars | P3 / M5 | Product extension, not MSP parity. Define platform/calendar API, entry and display semantics; test DST, locale and persistence before UI. |
| [#414](https://github.com/tetsuji16/ProjectLibre/issues/414) Input consolidation | P1 / M2 | Consolidate keyboard/mouse/dialog routes; renderer migration excluded by ADR-0002. One binding/action per gesture and physical route tests. |
| [#453](https://github.com/tetsuji16/ProjectLibre/issues/453) Ribbon specification | P1 / M0-M2 | Keep normative functional inventory; date/version Microsoft sources, reconcile with #769, separate offline from licensed/service-conditional commands. CCPM is an extension. Zoom is PARTIAL: Entire Project works in Task Usage and Resource Usage; Selected Tasks only Gantt; Resource Graph is absent. |
| [#479](https://github.com/tetsuji16/ProjectLibre/issues/479) Windows shell | P2 / M5 | Keep native window/snap behavior isolated from ribbon. Verify packaged Windows build; shell polish does not establish ribbon compatibility. |
| [#483](https://github.com/tetsuji16/ProjectLibre/issues/483) FlatLaf coverage | P2 / M1 | Audit screens/states; preserve semantic chart/print colors; record platform differences. Not a proxy for MSP visual parity. |
| [#595](https://github.com/tetsuji16/ProjectLibre/issues/595) Java 25 modernization | P3 / M5 | Behavior-preserving slices; protect MPO, frozen POD and MPP/XML; measure reviewed batches and tests, not files reformatted. |
| [#724](https://github.com/tetsuji16/ProjectLibre/issues/724) Dialog sizing | P2 / M1 | Per-dialog min/preferred size; inspect tabs, locale, DPI, screen edges and resize behavior; no universal resize without evidence. |
| [#728](https://github.com/tetsuji16/ProjectLibre/issues/728) Manual QA | P1 / M5 | Start after stated dependencies. Record exact build, fixture, routes, screenshots, passes/failures and untested scope. |
| [#729](https://github.com/tetsuji16/ProjectLibre/issues/729) Duplication audit | P2 / M2 | Record callers, provenance, serialization and contracts; split confirmed duplicate work. Similar names alone do not justify merging. |
| [#737](https://github.com/tetsuji16/ProjectLibre/issues/737) Core/UI boundary | P2 / M2 | Small module-boundary slices with architecture and format checks; do not duplicate #730 importer work or mix semantic changes. |
| [#738](https://github.com/tetsuji16/ProjectLibre/issues/738) GraphicManager | P2 / M2 | Separate lifecycle/window/dialog/command/workspace ownership; verify shutdown, multi-window and recovery per extraction. |
| [#740](https://github.com/tetsuji16/ProjectLibre/issues/740) Application workflows | P2 / M2 | Move one workflow per slice; prove UI relinquishes exchange/report orchestration and preserve behavior/file compatibility. |
| [#743](https://github.com/tetsuji16/ProjectLibre/issues/743) Task model responsibilities | P2 / M2 | Extract one family at a time; invariant, MPO/MPP and POD tests before next family. |
| [#748](https://github.com/tetsuji16/ProjectLibre/issues/748) Diagrams | P2 / M2 | Distinct WBS/PERT/Gantt contracts. Test geometry, labels, links, selection, zoom, hit testing and fixtures; merge only proven shared work. |
| [#750](https://github.com/tetsuji16/ProjectLibre/issues/750) Resource sorting | P2 / M2.1 | **CLOSED completed 2026-10-15** for its reviewed acceptance: physical header toggle and Sort Order menu share the canonical view-only route, preserving model order, selected identity, and Workspace state; `ResourceHeaderSortingGuiAcceptanceTest.physicalHeaderClickSortsAndTogglesWithoutMutatingResources` passed in Windows run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004). Broader Microsoft resource multi-key/date/filter/group behavior remains tracked under M2.1/#794, not claimed by #750 closure. [Microsoft `Application.Sort`](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort) documents its `Renumber` argument for task sorting; permanent resource renumbering is not an applicable requirement. |
| [#752](https://github.com/tetsuji16/ProjectLibre/issues/752) BeanUtils | P2 / M5 | No repeat of source migration. Finish #753 dependency chain; preserve converters; runtime graph and package evidence. |
| [#753](https://github.com/tetsuji16/ProjectLibre/issues/753) JasperReports 7 | P2 / M5 | Migrate template/API by report family; compare rendered content/layout/data; prove packaged graph; #242 remains blocked until complete. |
| [#761](https://github.com/tetsuji16/ProjectLibre/issues/761) Task sorting | P2 / M2.1 | PRs #815/#827 cover physical date directions, multi-key directions, stable ties, missing/empty values, flat Keep Outline projection, selection/Gantt alignment, unchanged model/schedule, and separate confirmed permanent order with renumbering, Undo/Redo and MPO evidence. The exact Windows task sort method passed in GUI run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004). The filtered/grouped projection fixture now verifies that filtering limits visible tasks, configured group buckets retain their order, and the task comparator orders rows inside each bucket without changing outline order. This combination is a documented Office-consistent rule, not measured MSP behavior. |
| [#765](https://github.com/tetsuji16/ProjectLibre/issues/765) Office ribbon visuals | P1 / M1 | Visual-only gate on ModernRibbonPanel. Compare dated current Microsoft 365 and Project 2024 captures, exact build and environment; check density, type, icons, context and overflow. App locale must be confirmed from app preferences; JVM locale alone does not establish English UI evidence. |
| [#769](https://github.com/tetsuji16/ProjectLibre/issues/769) Ribbon functional audit | P1 / M2 | Reconcile every #453 row to source/test/result/condition. A dispatch is not proof; tag standard, conditional, extension and unsupported. |
| [#770](https://github.com/tetsuji16/ProjectLibre/issues/770) Ribbon customization | P2 / M2 | Separate user-scoped settings from command parity. Test persistence/restart, import/export, resets, invalid settings, keyboard/accessibility and locale/DPI; preserve Action IDs. |
| [#773](https://github.com/tetsuji16/ProjectLibre/issues/773) Test audit | P2 / M5 | Consolidate around distinct observable contracts; retain fixed interoperability fixtures and GUI representatives; justify every removal. |
| [#774](https://github.com/tetsuji16/ProjectLibre/issues/774) Legacy OpenProject paths | P2 / M0-M2 | Project Server/PWA login and connected-service behavior are out of scope; do not add them as dependencies. Review each legacy route by provenance and data behavior. Preserve local Open, master/linked/copy flows, supported MPP/XML, and serialized POD aliases until fixture/use review supports a safe change. |

## Presently visible gaps

The exchange inventory found MPXJ MPPReader use in MspImporter and no MPP writer
registration or call in the exchange module. MPXJ's official format matrix and
FAQ describe MPP as read-only; the pinned 11.5.4 release notes add no MPP writer.
The command inventory reports
gaps or partial scope for Task Inspector,
Format Painter, Select/Clear and insertion commands, resource leveling,
Selected Tasks zoom in usage views, and report/timeline object formatting.
MPP read and current Microsoft UI-reference comparison require explicit
inventory rows. MPP writing and the Microsoft Project-specific M4 automation
surfaces are excluded by the 2026-10-12 product decision; this does not mark
MPP read as verified or exclude microProject-native automation. Connected Project Server/PWA/Project
Online/Planner cloud services are expressly outside this goal. This is not yet
an exhaustive in-scope gap list; M0 makes that inventory exhaustive before any
claim expands.

## Gate closure snapshot (2026-10-12)

This status inventory does not claim that the parent issues are closable. The
user explicitly removed MPP writing from product scope on 2026-10-12; MPO is
the native save substitute. MPP reading remains in scope. The user also
confirmed that neither Microsoft 365 Project nor Project 2024 is installed.
Visual comparison may therefore use dated Microsoft-published UI references
as a **document-derived comparison**, never as an empirical MSP capture; the
six microProject locale/scale legs and app screenshots remain required.

| Gate | Acceptance criterion | Current evidence | Remaining work / status |
|---|---|---|---|
| #793 M1 visuals | Dated document-derived comparison evidence; all six microProject locale/scale legs pass; visual evidence stays separate from command parity. | Run [38495421214](https://github.com/tetsuji16/ProjectLibre/actions/runs/38495421214) passed for candidate `702146871dbe9059412b83ab7780936e01f2e7f2`. Public [v2 GUI evidence archive](https://github.com/tetsuji16/ProjectLibre/releases/download/v0.0.24.1244/microProject-gui-evidence-v2-v0.0.24.1244.zip) contains 48 PNGs (six states × six legs, including two contextual states) with manifest hashes, source paths and dimensions; archive SHA-256 `350f27a435fff35cda1fed6a22cb69e5da132e9a5bc8c04c7f15a0d2ec032056`. All captures were reviewed 2026-10-14. Microsoft-published sources/applicability/access dates and the Report/Chart/Table vs Network/Calendar visual difference are documented; no observed Microsoft client build/environment or pixel parity is claimed. Focused keyboard/accessibility GUI run [38500386903](https://github.com/tetsuji16/ProjectLibre/actions/runs/38500386903) and #816 physical narrow viewport route passed on Windows. | **COMPLETE for visual-only M1** under the user's document-derived compatibility direction. Microsoft reference builds/OS/window bounds are not published/measured. Report/Chart/Table Tools command functionality stays with #794/#453/#769. Numeric screenshot bounds remain unclaimed; Robot asserts the relevant viewport geometry. |
| #794 M2 offline workflows | Each M2.1–M2.4 package has source, route, model/view, failure atomicity, Undo/Redo and persistence evidence. | Resource Sort and physical MPP/XML→MPO save/reopen have focused evidence; one Update Project transaction has rollback/Undo coverage. | **PARTIAL**. Close the finite package rows in `docs/msp-implementation-status.md`: sort boundaries/task renumber; dependency and transaction families; resource/progress/baseline workflows; and command/view/report/print inventory evidence. |
| #796 M3 files | Explicit format/version read/write statuses, passing tests and listed mismatches. | MPO native save and assignment scalar baseline transfer (slots 0–10, assignment-only, explicit zero) have focused core/exchange/physical MPO reopen evidence. Assignment baseline timephased work and cost have literal XML/MPO round trips, with zero/missing distinction and cost-only baselines. Elapsed-duration XML normalization also has round-trip coverage. Microsoft documents Assignment/TimephasedData and the Type enumeration; work Type values are 4 for slot 0, 16 for slot 1, then +6 per slot, with adjacent cost Type values. Accessed 2026-10-13: [Baseline element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016), [Type element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/type-element-multiple-parents?view=project-client-2016). These are document-derived interoperability decisions, not empirical MSP measurements. The independent MPP-read semantic manifest and other uncovered format semantics remain incomplete. MPP writer is **OUT OF SCOPE** by user decision; MPP reader remains in scope. | **PARTIAL**. Complete the independent edition/build MPP-read manifest, remaining required XML semantics, and earlier MPO-version read evidence. Preserve frozen POD import behavior. |
| #797 M4 local automation | Enumerate and verify Microsoft Project VBA/macro/object-model/COM/VSTO/local add-in behavior. | On 2026-10-12 the user explicitly excluded every Microsoft Project-specific surface listed by this issue. Native microProject automation/extensions and offline collaboration remain in product scope outside this Microsoft-specific gate. | **EXCLUDED** by explicit user scope decision; #797 may close as not planned. This does not imply native microProject automation compatibility. |
| #795 M5 release | All in-scope rows VERIFIED; release checks, package launch, formats, locales/scales and dated coverage report pass. Any PARTIAL/MISSING/UNSUPPORTED/UNASSESSED row blocks a complete claim. | Focused tests and one physical Windows Japanese/100% workflow pass for the current slice. MPP writing and Microsoft Project-specific M4 automation are out of scope; no release-candidate package matrix or complete dated report exists. | **BLOCKED** by #793/#794/#796 residual rows and missing release matrix. No complete compatibility claim is supported. |

Assignment scalar, timephased work/cost, and elapsed-duration baseline
implementations are bounded M3 improvements, not closure of file
interoperability. The independent MPP-read manifest and remaining required
format semantics remain explicitly partial.

## Microsoft references

- [Project desktop supported file formats](https://support.microsoft.com/en-us/project/file-formats-supported-by-project-desktop)
- [Project Online desktop client service description](https://learn.microsoft.com/en-us/office365/servicedescriptions/project-online-service-description/project-online-desktop-client-service-description)
- [Project for the web and Project desktop](https://support.microsoft.com/en-us/project/project-for-the-web-and-project-online-desktop-client)
- [Microsoft Project Online retirement announcement](https://techcommunity.microsoft.com/blog/plannerblog/microsoft-project-online-is-retiring-what-you-need-to-know/4450558)
- [Office ribbon customization](https://support.microsoft.com/en-us/office/foundations-experiences/customize-the-ribbon-in-office)
- [Project 2024 lifecycle](https://learn.microsoft.com/en-us/lifecycle/products/project-2024)

Recheck mutable Microsoft sources at each milestone and retain retrieval date
with evidence.


## ジュニアエンジニア向け実施・完了基準

実装担当は以下の順序で進め、レビュー担当が証跡を確認してから親 Issue の
チェック項目を完了にします。目標全体の完成と、一つの PR の完了を分けます。

### 着手前に確定する仕様

1. 作業元コミット、対象 Issue、依存 Issue、担当者・レビュー担当者を記録する。
2. 対象コマンドを現在のインベントリと照合し、既存の実装・テスト・最新コメントを読む。
   「未実装」「部分実装」「実装済みだが未検証」を区別する。
3. Microsoft 公式資料の URL、確認日、該当する操作・計算規則を記録する。
   資料から推定した部分には「文書由来の判断」と明記する。
4. 入力値、事前状態、操作、期待結果、禁止する副作用を表にする。
   日付計算はカレンダー、稼働時間、時間帯、期間単位まで固定する。
5. 既存の正規 Action、選択解決、編集トランザクション、保存経路を特定する。
   別のリボン描画器、別のショートカット登録、別の Undo 経路を追加しない。

### 一つの PR の実施順序

1. 既存 fixture を再利用し、現在失敗する条件または未検証の契約を一つ再現する。
2. ドメイン・ユースケース・UI の責務に従って最小の変更を行う。
3. 通常入力、空値、同値、境界、無効入力を検証する。変更対象以外の状態も比較する。
4. データ更新では Undo → Redo → 保存 → 再読込を確認する。
   エラー注入時は途中変更、通知、Undo 履歴が残らないことを確認する。
5. UI 操作では物理操作から正規 Action までの経路を検証し、モデル値と画面表示を比較する。
   `doClick`、例外がないこと、ダイアログが開くことだけで完了にしない。
6. 変更したモジュールの必要なテストを実行し、無関係な生成物・サンプル変更を除く。
7. PR にコマンド、結果、fixture、ログ・画像の所在、残る制約を記載する。

### ソートの固定 fixture と期待結果

以下は表示ソート専用の fixture。行 ID は表示番号と区別する。

| 入力順 | 安定 ID | 名前 | 数値 | 日付 |
|---|---|---|---|---|
| 1 | R1 | B | 2 | 2026-10-13 |
| 2 | R2 | A | 1 | 2026-10-12 |
| 3 | R3 | A | 1 | 2026-10-12 |
| 4 | R4 | 空 | 空 | 空 |

- 空値を末尾、同値を入力順維持と定義した表示ソートの契約では、名前の昇順は
  R2,R3,R1,R4、降順は R1,R2,R3,R4。同値・空値の契約は Microsoft の資料と
  照合し、異なる場合は fixture の期待値を仕様と同時に修正する。
- R3 を選択してからソートし、選択された安定 ID が R3 のままであることを確認する。
  タスク fixture では表とガントの行 ID 列が一致することも確認する。
- メニューとヘッダーで同じ方向・キーを示す。ソート前後でモデルの順序、ID、
  リンク、日付、割当が変化しない。ビュー状態の保存範囲は Workspace の契約に従う。
- 恒久的再採番を選ぶ操作は別 fixture で検証する。変更する ID と保持する安定 ID、
  参照更新、確認、Undo、保存・再読込の期待値を先に定義する。

### 段階別の提出物

| 段階 | 必須提出物とレビュー条件 |
|---|---|
| M0 | 全機能・形式・接続のカバレッジ表。各行に版、公式資料、状態、担当 Issue、証跡。不明項目を隠さず、既存テストの契約と対応させる。 |
| M1 | 同じ表示状態の比較画像。製品版・ビルド・確認日・アプリ言語・DPI を記録。Microsoft Project がない場合は Microsoft 公開 UI 資料との document-derived 比較を明示し、実機 capture と偽らない。日英×100/125/150% と狭いウィンドウで文字欠け・到達不能コマンドを確認。 |
| M2 | M2.1～M2.4 ごとの操作契約と回帰結果。親は全パッケージの完了後に閉じる。日付・工数・コスト・基準計画の計算期待値を独立した manifest に保存。 |
| M3 | MPO を標準保存・ネイティブ形式として維持し、旧版読込を確認する。MPP は外部 read/import を検証する。2026-10-12 の製品判断により MPP write は対象外で、MPO が native save の代替形式。XML は別ゲート。POD は wire 形式固定の旧形式 import-only とし、fixture を維持。 |
| M4 | 2026-10-12 のユーザー判断により、Microsoft Project の VBA/macros、macro-facing object model、COM automation、COM/VSTO add-ins、local Office add-ins は対象外（EXCLUDED）。microProject 固有のローカル自動化やオフライン共同作業は別の製品範囲から除外しない。 |
| M5 | リリース候補のコミット・テスト・配布物・起動確認・残存差異の報告。Windows と Linux/macOS の確認状態を別々に記録。未実行を成功扱いにせず、公開説明を証明済み範囲に合わせる。 |

レビュー担当は仕様根拠、変更範囲、検証結果、データ保全、残存差異の五点を確認する。
未検証の製品全体について「完全互換が完成」と表記しない。
