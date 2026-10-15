# MSP standard ribbon command inventory (#453 / #769)

Inventory reviewed at master `8f980ce978414fd6d8d894992481e5c9d326bdd2` on
2026-10-15. Evidence links identify the exact test/build run where available;
an absent source edition or build is recorded as unknown rather than inferred.

This is the canonical command and ribbon evidence inventory for M0 (#792),
reconciled against #453, #769, #770, current source, tests, and the scoped
compatibility roadmap. It is a coverage inventory, not a claim of complete
Microsoft Project behavior. A row marked VERIFIED establishes only the
specific local behavior and evidence named in that row; it does not imply
product-wide parity. Inferred rules are document-derived, not observed MSP
results. No row claims pixel-level equivalence.

Use only these row states: `VERIFIED`, `PARTIAL`, `MISSING`, `UNSUPPORTED`,
`UNASSESSED`, and `EXCLUDED`. `VERIFIED` means the stated contract has current
source/test evidence; `PARTIAL` means implementation or evidence is bounded;
`MISSING` means no equivalent route was found; `UNSUPPORTED` means an explicit
product limitation; `UNASSESSED` means the row still needs source/code review;
`EXCLUDED` means an explicit scope decision. These statuses describe each
row's current evidence, not completion of #794, #796, or the overall goal.

For Microsoft-published support pages, the applicability entry below records
the Project editions listed in the page's current “Applies to” metadata.
Publication/build dates are recorded as unstated when the page does not give
them. Do not infer that a rolling support page proves behavior for a
specific Project build. The current compatibility target is Microsoft 365
Project desktop with Project 2024 as the reproducible perpetual-license
baseline; Project Server, PWA, Project Online/cloud services are excluded.
In historical issue #453, “Project Online Desktop Client” refers to the
standalone installable Project desktop client, which remains in scope; it does
not bring Project Online service connections into scope.

## Official baseline

| ID | Microsoft or project source | Applicability/version as stated by source |
|---|---|---|
| S1 | [Learn the Project ribbon](https://support.microsoft.com/en-us/office/learn-the-project-2010-ribbon) | Microsoft page explicitly names Project 2010; use for the ribbon concepts it describes, not as proof of current M365/2024 layout. |
| S2 | [Distribute project work evenly (level resource assignments)](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments); [Resource Leveling dialog box](https://support.microsoft.com/en-us/project/resource-leveling-dialog-box) | Both pages list Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S3 | [Update work on a project](https://support.microsoft.com/en-us/project/update-work-on-a-project) | Lists Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S4 | [View resource workloads and availability in Project desktop](https://support.microsoft.com/en-us/project/view-resource-workloads-and-availability-in-project-desktop) | Lists Project Online Desktop Client; Project Professional/Standard 2024, 2021, 2019, and 2016; and Project Online service. The service entry is outside product scope. Build and publication date are not stated. |
| S5 | [Customize the ribbon in Office](https://support.microsoft.com/en-us/office/foundations-experiences/customize-the-ribbon-in-office) | Applies To includes Project Online Desktop Client, Project Professional/Standard 2024 and 2021, alongside Microsoft 365 and other Office products. Build and publication date are not stated; #770 defines Windows Office customization scope. |
| S6 | [View and track scheduling factors](https://support.microsoft.com/en-us/project/view-and-track-scheduling-factors); [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project) | Both pages list Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S7 | [TaskRespectLinks API](https://learn.microsoft.com/en-us/office/vba/api/project.application.taskrespectlinks) | Microsoft Project VBA API contract; API page does not state the desktop edition/build to which this behavior applies. |
| S8 | [Active task field](https://support.microsoft.com/en-us/project/active-task-field) | Lists Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S9a | [Filter tasks or resources](https://support.microsoft.com/en-us/project/filter-tasks-or-resources) | Lists Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S9b | [Group tasks or resources](https://support.microsoft.com/en-us/project/group-tasks-or-resources) | Lists Project Online Desktop Client; Project Professional 2024, 2021, 2019, and 2016; Project Server 2019 and 2016. This page does not list Project Standard editions. Build and publication date are not stated. |
| S9c | [Sort tasks, resources, or dates in Project desktop](https://support.microsoft.com/en-us/project/sort-tasks-resources-or-dates-in-project-desktop) | Lists Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016. Build and publication date are not stated. |
| S10 | [Application.Sort](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort) | Microsoft Project VBA API reference; task/resource parameter semantics as stated there; desktop edition/build not stated. |
| S11 | Repository compatibility decisions and tests | Internal evidence only; not a Microsoft source. Cite the applicable issue/commit/test and mark Microsoft applicability not applicable. |

## Current structure

| Surface | Current source state | Status | Owner | Source/applicability | Evidence and unknowns |
|---|---|---|---|---|---|
| File | File tab opens a Backstage view; commands route through canonical Actions | VERIFIED | #794 M2.4 | #453; S1 is Project 2010 only; current edition applicability not stated | `RibbonStructureTest`, Backstage Robot acceptance; Share/Account omitted because there is no cloud-account service. |
| Task, Resource, Report, Project, View, Help | Seven normal tabs in this order after File | VERIFIED | #794 M2.4 | #453; S1 explicitly Project 2010, not a current M365/2024 layout claim | `RibbonStructureTest`, `RibbonTabGuiAcceptanceTest` verify tab structure only; individual command coverage is in the command table below. |
| Format, Network Format, Calendar Format | Separate contextual surfaces | PARTIAL | #794 M2.4 | #453; current edition applicability not stated by a cited Microsoft page | `ContextualRibbonVisibilityContractTest` and ribbon Robot acceptance cover existing views; Report/Timeline object surfaces remain incomplete. |
| Flamingo renderer | Not used | VERIFIED | #794 M2.4 | Internal ADR-0002; Microsoft applicability not applicable | Swing `ModernRibbonPanel` is the sole ribbon renderer; architecture only, not a user-visible MSP capability. |
| QAT | Save/Undo/Redo defaults; supported actions can be added/removed, order retained and reset restores defaults | PARTIAL | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021; build/date not stated | `OfficeChromeRibbonDisplayGuiAcceptanceTest.quickAccessCustomizationAddsAndPersistsCanonicalRibbonAction` verifies add/reset; full ribbon configuration import/export and production-shell restart acceptance remain unimplemented. |

## MSP command inventory

Each command row uses the canonical states above. A visible button alone is
not evidence of behavior; tests and gaps are named per row. Grouped rows list
their finite subcommand names in the capability cell; a PARTIAL status means
the listed commands' full Project behavior or evidence is incomplete, not
that unnamed capabilities are silently accepted. Unsupported and excluded
product services must remain absent instead of appearing as enabled no-op
buttons.

| MSP capability | Current ribbon/action | Status | Owner | Source/applicability | Evidence and explicit unknown |
|---|---|---|---|---|---|
| Insert, delete, copy, cut, paste tasks | Task ribbon canonical Action routes | PARTIAL | **Owner:** #794 M2.4 (source inventory). **Source/applicability:** #453 task surface; S1 is Project 2010 only; current edition applicability for this grouped command row is not stated. **Evidence/unknown:** `CommandRouteMatrixTest`, `RibbonButtonBehaviorTest`, and task Robot journeys verify local route/state; individual invalid-state and save/reload parity is not fully mapped. |
| Indent/outdent, expand/collapse, move tasks | Task > Outline/Schedule | PARTIAL | **Owner:** #794 M2.1/M2.4. **Source/applicability:** #453 task surface; S1 explicitly Project 2010, not a current M365/2024 version claim. **Evidence/unknown:** canonical hierarchy route and task GUI acceptance cover model, visible rows, Undo/Redo and persistence; command-level Microsoft source-to-result mapping remains to be reconciled. |
| Link/unlink tasks | Task > Schedule | PARTIAL | **Owner:** #794 M2.2/M2.4. **Source/applicability:** #453 task surface and S6; Project Online Desktop Client; Project Standard 2024; Project Professional/Standard 2021, 2019, and 2016; publication/build date not stated. **Evidence/unknown:** `CommandRouteMatrixTest` and dependency tests verify local route/model behavior; full link/lag/calendar/constraint evidence remains open in #794. |
| Task Information, Notes, Custom Fields, Assign Resources | Task ribbon | PARTIAL | **Owner:** #794 M2.3/M2.4. **Source/applicability:** #453 task surface; exact current Microsoft source and edition applicability are not assigned separately to the grouped commands. **Evidence/unknown:** dialog/assignment journeys exist; per-command invalid-state, mutation, Undo and persistence mapping remains to be split/reconciled. |
| Manual/Automatic task mode | Task > Schedule | PARTIAL | **Owner:** #794 M2.2/M2.4. **Source/applicability:** #453 task surface; edition/version-specific primary source not mapped for this row. **Evidence/unknown:** `TaskModeServiceTest` and MPO round trip verify local mode behavior; full conversion semantics remain outside this bounded evidence. No POD layout change is allowed. |
| Task view sort, Keep Outline, permanent task order | View > Data > Sort; Task > Outline > Sort | PARTIAL | **Owner:** #794 M2.1. **Source/applicability:** S9c lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; S9b grouping page lists Professional editions but not Standard; S10 documents task Renumber and states no desktop edition/build. **Evidence:** PRs #815/#827 cover field/direction keys, stable ties, missing/empty values, flat Keep Outline, table/Gantt alignment and permanent-order confirmation/Undo/Redo/MPO; PR #832 adds filtered/grouped projection composition and unchanged model order. Physical task sort passed in Windows GUI run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004); filtered/grouped fixture passed Build [38743643764](https://github.com/tetsuji16/ProjectLibre/actions/runs/38743643764). The filter-first, preserve-group-buckets, sort-within-bucket rule is document-derived, not measured MSP behavior. |
| Resource view sort | Resource Name/Standard Rate header and Sort Order menu | PARTIAL | **Owner:** #794 M2.1; bounded header/menu issue #750 is closed. **Source/applicability:** S9c lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; S10's Renumber argument is task-specific and does not state a desktop edition/build. **Evidence:** physical header/menu route passed ja/100% Windows run [38728563004](https://github.com/tetsuji16/ProjectLibre/actions/runs/38728563004), preserving model order, selection identity and Workspace state. Broader resource multi-key/date/filter/group behavior remains open. Resource permanent renumbering is not an applicable requirement. |
| Mark on Track, Update Tasks, Update Project, Status Date | Task/Project ribbon | PARTIAL | **Owner:** #794 M2.2/M2.4. **Source/applicability:** S3 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated; #453 groups the commands. **Evidence/unknown:** `msp-task-command-contract.md` records selection, schedule, Undo/Redo and MPO evidence; remaining mutation-family/transaction rows are listed under #794. |
| Level Selection | Resource > Level | PARTIAL | **Owner:** #794 M2.3. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** selected-task scope and multi-selection are documented; exact preservation of nonselected task dates/assignments, default clear behavior and final physical acceptance remain open. |
| Level All | Resource > Level | PARTIAL | **Owner:** #772 / #794 M2.3. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** Robot journey verifies all-project scope, selection, Undo/Redo and MPO; clearing effects, proposed-resource defaults, split/manual-task controls and provenance remain open. Preserve frozen POD schema. |
| Level Resource | Resource > Level | PARTIAL | **Owner:** #794 M2.3. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** `LevelResourcesAction` is registered; resource-specific selection and multi-resource route/results need audit. |
| Leveling Options | Resource > Level | PARTIAL | **Owner:** #794 M2.3. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** `RibbonLevelResources` opens the dialog; calculation, sensitivity, range, order, proposed resources, manual tasks and split settings need per-option result evidence. |
| Clear Leveling | Resource > Level | MISSING | **Owner:** #794 M2.3. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** no dedicated clear-leveling Action found; safe provenance must preserve leveling-independent delays/splits and manual edits. |
| Next Overallocation | Resource > Level | VERIFIED | **Owner:** #794 M2.4. **Source/applicability:** S2 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/decision:** Robot verifies next, wrap, no-candidate feedback and schedule preservation. Traversing visible task rows and excluding filtered tasks is a document-derived product decision, not measured MSP behavior. |
| Task Inspector | Task | MISSING | **Owner:** #794 M2.4. **Source/applicability:** S6 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** no inspector route found; Microsoft describes a pane explaining scheduling factors/suggested changes. Selection-following content and physical route remain unimplemented. This row replaces the duplicate Inspector entry in the earlier inventory. |
| Respect Links | Task > Schedule | PARTIAL | **Owner:** #794 M2.2/M2.4. **Source/applicability:** S6 and S7; S7 is a VBA API page and states no desktop edition/build. **Evidence:** merged PR #831 (`b03368a8d3677fde6ba2a18e6bb70cb4d057996c`); `RespectLinksServiceTest` covers FS/SS/FF/SF, positive/negative working and elapsed lag, with/without assignments. On exact current master `8f980ce978414fd6d8d894992481e5c9d326bdd2`, Build run [38745252670](https://github.com/tetsuji16/ProjectLibre/actions/runs/38745252670) passed; GUI push run [38745252701](https://github.com/tetsuji16/ProjectLibre/actions/runs/38745252701) selected `smoke` at 100% (ja), and physical `TaskInformationRibbonGuiAcceptanceTest.respectLinksRibbonRouteMovesTheSelectedManualTaskAndRoundTripsUndoRedoAndMpo` passed. This is Windows smoke evidence only, not a full GUI matrix; broader #794 scheduling/transaction coverage remains open. |
| Inactivate Task | Task | MISSING | **Owner:** #794 M2.4. **Source/applicability:** S8 lists Project Online Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated. **Evidence/unknown:** source scan found the task model field but no Inactivate ribbon Action in current `menuInternal.properties`/`RibbonCommandCatalog`; the model field alone does not implement the command. |
| Format Painter | Task | MISSING | **Owner:** #794 M2.4. **Source/applicability:** #453 lists the command; exact current Microsoft source/version is not mapped. **Evidence/unknown:** no equivalent command in the current standard ribbon/catalog; copied fields and formatting target need a source-backed contract. |
| Select/Clear and Summary/Milestone insertion | Task | PARTIAL | **Owner:** #794 M2.1/M2.4. **Source/applicability:** #453 lists the capability; per-command Microsoft source/version not mapped. **Evidence/unknown:** normal task, recurring task and subproject insertion exist; full Select/Clear and Summary/Milestone insertion are not exposed through current Task ribbon definitions. |
| Entire Project / Selected Tasks zoom | View | PARTIAL | **Owner:** #794 M2.4. **Source/applicability:** #769 references Microsoft `Application.ZoomTimescale`; source page applicability does not establish an observed current build. **Evidence/unknown:** Entire Project is physically verified in Gantt and both Usage views; Selected Tasks is only verified in Gantt, and Resource Graph has no microProject equivalent. |
| Report/Timeline object-specific formatting | Contextual tabs | PARTIAL | **Owner:** #794 M2.4. **Source/applicability:** #453 report/contextual requirements; exact current Microsoft source and edition applicability are not mapped per object command. **Evidence/unknown:** Network and Calendar contextual surfaces exist; Report/Timeline object selection surfaces and supported commands are incomplete. |
| Cloud services and Microsoft Project local automation/extensions | File/Help and local integration | EXCLUDED | **Owner:** #797 for VBA/macros, COM/VSTO and Office add-ins; #792 for connected services. **Source/applicability:** explicit user scope decision dated 2026-10-12; Microsoft edition applicability not applicable. **Evidence:** current inventory records Project Server/PWA/Project Online/Planner connected services as out of scope and Microsoft Project VBA/macros, COM/VSTO and local Office add-ins as excluded. Keep microProject-native automation distinct and in scope. |

## #453 / #769 specification crosswalk

The rows below account for the requirements in the historical specification;
they do not convert an unverified requirement into a pass. `#453` is the
project's normative feature list and names Project Standard/Professional 2024
and the standalone Windows Project desktop client as its target. S1 is only a
Project 2010 ribbon reference; the Microsoft support pages S2–S10 do not state
the current desktop edition/build unless the source-register row says
otherwise. For requirements with no mapped Microsoft primary source, the
project specification is cited and the source gap is explicit.

| #453 / #769 requirement | Status | Owner | Source / applicability | Evidence and explicit unknown |
|---|---|---|---|---|
| QAT defaults Save, Undo, Redo; all surfaces use one Action ID | PARTIAL | #770 / #794 M2.4 | #453 §2; S5 for customization, edition/build unstated | Default commands and canonical command routes exist; QAT customization add/reset has physical evidence. One-action equivalence across every menu, shortcut and ribbon command is not inventoried as a complete matrix. |
| Standard tabs and order: File, Task, Resource, Report, Project, View, Help | VERIFIED | #794 M2.4 | #453 §2; S1 is Project 2010 only | `RibbonStructureTest` and `RibbonTabGuiAcceptanceTest` verify the local seven-tab structure/order. Current Microsoft 365/2024 visual comparison is unobserved. |
| File backstage: New, Open, Info, Save, Save As, Print, Share, Export, Close, Account, Options | PARTIAL | #794 M2.4 | #453 §2; S1 Project 2010 concepts only; Share/Account cloud applicability excluded | Backstage and local file commands exist; Share/Account are omitted. Per-command current edition route, enablement, print/export result, and failure/Undo behavior are not fully mapped. |
| Narrow-width grouping, no silent command loss, tooltip purpose/shortcut/disabled reason | UNASSESSED | #794 M2.4 | #453 §2; no mapped version-specific Microsoft source | Responsive and tooltip coverage is not registered per command; do not count general tab screenshots as evidence. |
| KeyTips, keyboard traversal/activation, accessible names and selected/disabled state | PARTIAL | #794 M2.4 | #453 §2; current Project edition/build not stated by a cited source | Root-pane shortcut layer is canonical per project guide; all-command Alt/Tab/arrow/Space/Enter and screen-reader coverage is not established. |
| Task > View: select Gantt Chart | PARTIAL | #794 M2.4 | #453 §3 Task; S1 Project 2010 only | Gantt and task-view commands exist. View-choice availability and selected-state contract across all task views is not fully mapped. |
| Task > Clipboard: Paste, Cut, Copy | PARTIAL | #794 M2.1/M2.4 | #453 §3 Task; exact current Microsoft source/version not mapped | Route/state evidence: `CommandRouteMatrixTest`, `RibbonButtonBehaviorTest`, task GUI journeys. Per-command read-only, invalid-selection and save/reload cases remain incomplete. |
| Task > Clipboard: Format Painter | MISSING | #794 M2.4 | #453 §3 Task; current Microsoft source/version not mapped | No equivalent standard ribbon/catalog command found; source-backed copied-format scope and physical evidence are absent. |
| Task > Font: font, size, bold, italic, underline, font color and fill | MISSING | #794 M2.4 | #453 §3 Task; current Microsoft source/version not mapped | Standard Task ribbon formatting commands are not present in the current source inventory; no physical route evidence. |
| Task > Schedule: % Complete, Mark on Track | PARTIAL | #794 M2.2/M2.4 | #453 §3 Task; S3 lists Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated | `msp-task-command-contract.md` records selected update workflows; exact independent outcomes, invalid inputs, Undo and MPO evidence remain package-scoped. |
| Task > Schedule: Respect Links | PARTIAL | #794 M2.2/M2.4 | #453 §3 Task; S6 applies to Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; S7 API page has no client edition/build | PR #831 and its focused core/Windows GUI evidence are listed in the command table; broader schedule/transaction package remains open. |
| Task > Schedule: Move Task / Move Up / Move Down | PARTIAL | #794 M2.1/M2.4 | #453 §3 Task; current Microsoft source/version not mapped | Hierarchy move routes exist; command-by-command parity, disabled conditions and independent visible-result evidence are not fully crosswalked. |
| Task > Schedule: Inspect / Task Inspector | MISSING | #794 M2.4 | #453 §3 Task; S6 applies to Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions | No inspector route found; Microsoft documents selection-following scheduling factors. The required content and GUI route remain absent. |
| Task > Tasks: Manually Schedule, Auto Schedule, Task Mode | PARTIAL | #794 M2.2/M2.4 | #453 §3 Task; Microsoft source/version not mapped per action | `TaskModeServiceTest` and MPO round trip cover mode behavior; each command's precondition and full conversion contract remains open. |
| Task > Tasks: Inactivate | MISSING | #794 M2.4 | #453 §3 Task; S8 Active task field page has no stated edition/build | Current master has the `inactiveTask` model field and Task General dialog field, but no Inactivate Action in `menuInternal.properties` or `RibbonCommandCatalog`. This conflicts with #769's 2026-10-07 status note that says it is implemented; retain MISSING for the required ribbon command until its route is located and proven. |
| Task > Insert: Task, Summary, Milestone, Recurring Task, Subproject | PARTIAL | #794 M2.1/M2.4 | #453 §3 Task; current Microsoft source/version not mapped per insert type | Normal/recurring task and subproject insertion exist; Summary/Milestone behavior and selection-position parity are not fully established. |
| Task > Properties: Information, Notes, Details, Add to Timeline | PARTIAL | #794 M2.4 | #453 §3 Task; current Microsoft source/version not mapped | Dialogs/notes exist; Details pane and Timeline route, selection following, and per-command physical results are not fully mapped. |
| Task > Editing: Find, Clear, Scroll to Task, Select | PARTIAL | #794 M2.1/M2.4 | #453 §3 Task; current Microsoft source/version not mapped | Find and clear routes exist in local UI; the complete selection/scroll contracts and physical result matrix are unverified. |
| Task > Outline: Indent/Outdent, Link/Unlink | PARTIAL | #794 M2.1/M2.2 | #453 §3 Task; S6 for dependencies is undated | Canonical route and hierarchy/dependency tests exist; schedule/transaction and physical outcome gaps are tracked in #794. |
| Resource > View: Resource Sheet, Resource Usage, Team Planner, Other Views | PARTIAL | #794 M2.4 | #453 §3 Resource; S4 lists Project Online Desktop Client and Project Professional/Standard 2024/2021/2019/2016; build/date not stated | Resource Sheet/Usage exist; Team Planner and full other-view inventory are not established. Exact list and per-view evidence remain unknown. |
| Resource > Assignments: Assign Resources and Team Planner assignment actions | PARTIAL | #794 M2.3/M2.4 | #453 §3 Resource; current Microsoft source/version not mapped per command | Assignment dialogs/services and tests exist; Team Planner route and complete selected/multi-resource results are unverified. |
| Resource > Insert: Add Work, Material and Cost resources | PARTIAL | #794 M2.3 | #453 §3 Resource; current Microsoft source/version not mapped | Resource creation exists; separate type choice, fields, constraints and physical result coverage are not registered here. |
| Resource > Properties: Resource Information, Notes, Details | PARTIAL | #794 M2.3/M2.4 | #453 §3 Resource; current Microsoft source/version not mapped | Existing resource dialogs/fields provide bounded evidence; Notes/Details command route and persistence matrix remain unknown. |
| Resource > Level: Level Selection, Level Resource, Level All, Options, Clear Leveling, Next Overallocation | PARTIAL | #794 M2.3 | #453 §3 Resource; S2 lists Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; build/date not stated | Individual rows above record the implemented and missing actions. Assignment/date preservation, provenance, settings, and clear semantics remain open. |
| Resource Pool: share resources, pool, substitute resources, refresh/resolve pool | PARTIAL | #794 M2.3 | #453 §3 Resource; local shared-resource scope retained; cloud services excluded | Shared/local resource functionality remains in scope. The full pool command family and conflict/refresh behavior are not mapped to independent test evidence; no server/cloud claim. |
| Report: gallery, dashboards/resources/costs/in-progress, New/Custom/Recent, Visual Reports, Compare Projects | PARTIAL | #794 M2.4 | #453 §3 Report; Microsoft report sources linked in #453, edition/build not stated | Report surface exists but no full gallery/selection/rendered-value inventory; Visual Reports have external Excel/Visio conditions. Each report and compare route remains individually unverified. |
| Project > Properties: Project Information, Custom Fields, Links Between Projects, WBS, Change Working Time | PARTIAL | #794 M2.1–M2.4 | #453 §3 Project; command-specific Microsoft source/version not mapped | Local dialogs/services exist for some properties; source-to-result and cross-project/calendar behavior must be audited per command. |
| Project > Schedule: Calculate Project, Set/Clear Baseline, Move Project | PARTIAL | #794 M2.2/M2.3 | #453 §3 Project; current Microsoft source/version not mapped per action | Scheduling and baseline services exist; baseline slots/scope and move-project date invariants are not fully evidenced in this inventory. |
| Project > Status: Status Date, Update Project, Mark on Track | PARTIAL | #794 M2.2/M2.3 | #453 §3 Project; S3 is undated | Update Project transaction has bounded source/test evidence; status-date and remaining progress mutation families need independent outcome/Undo/MPO coverage. |
| Project > Reports: Visual Reports | PARTIAL | #794 M2.4 | #453 §3 Project; report sources cited in #453, edition/build not stated | External Excel/Visio conditions are explicit; user route, availability and output comparison remain unverified. |
| Project > Proofing: Spelling and proofing language | MISSING | #794 M2.4 | #453 §3 Project; current Microsoft source/version not mapped | No equivalent desktop proofing command found in the current inventory; no evidence of spelling/language workflow. |
| View > Task Views: Gantt, Task Usage, Task Board, Network Diagram, Calendar, Other Views | PARTIAL | #794 M2.4 | #453 §3 View; S1/S4 do not establish current 2024 view set | Gantt, usage, network and calendar views exist; Task Board and complete Other Views set are not evidenced. Exact view list is a known unknown. |
| View > Resource Views: Team Planner, Resource Usage, Resource Sheet, Other Views | PARTIAL | #794 M2.4 | #453 §3 View; S4 lists Project Online Desktop Client and Project Professional/Standard 2024/2021/2019/2016; build/date not stated | Resource Usage/Sheet exist; Team Planner and complete Other Views set remain unverified. |
| View > Data: Highlight, Filter, Group By, Sort, Outline, Tables | PARTIAL | #794 M2.1/M2.4 | #453 §3 View; S9a/S9c list Desktop Client and Project Standard 2024/2021/2019/2016 plus Professional editions; S9b lists Desktop Client and Professional editions only; S10 has no client edition/build | Sort and filtered/grouped task projections have PR #815/#827/#832 evidence; full Highlight/Filter/Group/Outline/Table source, view-availability, selection and GUI matrix remains open. |
| View > Zoom: Timescale, Zoom, Entire Project, Selected Tasks | PARTIAL | #794 M2.4 | #453 §3 View; #769 cites `Application.ZoomTimescale`; Microsoft current build unobserved | Entire Project has Gantt and both Usage-view evidence; Selected Tasks has Gantt-only evidence. Usage selection and Resource Graph are explicit gaps. |
| View > Split View: Timeline, Details, Resource Graph | PARTIAL | #794 M2.4 | #453 §3 View; current Microsoft source/version not mapped | Timeline/Details/Resource Graph command parity and Timeline+Details mutual-exclusion behavior are not fully evidenced; microProject has no Resource Graph equivalent. |
| View > Window: New Window, Arrange All, Hide/Unhide, Switch Windows | PARTIAL | #794 M2.4 | #453 §3 View; current Microsoft source/version not mapped | Multi-document windows exist; each command's open/hidden/arrange state and save interaction are not individually tested in the compatibility inventory. |
| View > Macros: Macros, Visual Basic | EXCLUDED | #797 | #453 §3 View; explicit user exclusion dated 2026-10-12 | Microsoft Project VBA/macros, macro object model and local Office add-ins are out of scope. Native microProject automation remains separate/in scope. |
| Help: Help, Training, Feedback, About | PARTIAL | #794 M2.4 | #453 §3 Help; current source edition not stated | Local Help/About replacements are present; Training/Feedback service behavior is excluded or replaced, not MSP parity. Per-route availability evidence remains incomplete. |
| Gantt/Tracking Gantt contextual formatting and display toggles | PARTIAL | #794 M2.4 | #453 §4; support pages do not state current edition/build | Existing Gantt contextual controls exist; full list (styles, columns, bars, critical/slack/late/path/baseline/slippage/outline/summary) and persistence/render evidence is not complete. |
| Timeline contextual formatting and existing-task/copy/date/detail controls | PARTIAL | #794 M2.4 | #453 §4; current Microsoft edition/build not stated | Timeline surface and all listed command outcomes/selection conditions are not established. |
| Network Diagram, Calendar contextual formatting | PARTIAL | #794 M2.4 | #453 §4; current Microsoft edition/build not stated | Contextual surfaces exist; complete Layout/Box Style/Data Template/Text/Drawing or Bar Style/Gridline behavior remains unverified. |
| Report design and selected chart/table/picture/shape contextual tabs | PARTIAL | #794 M2.4 | #453 §4; report source cited in #453, current edition/build not stated | Report contextual object surfaces and per-object selection/format routes are incomplete. |
| microProject tab keeps extensions separate from MSP commands | PARTIAL | #794 M2.4 | #453 §5; product extension, not MSP parity | Existing microProject-specific commands are not yet inventoried as a complete separate extension surface. Do not insert them among standard MSP actions. |
| CCPM Settings, Analyze, Apply, Clear | MISSING | #794 M2.4 | #453 §5 CCPM spec; product extension, Microsoft applicability not applicable | The specification defines preview, transactions, restoration and failure rules; current implementation/test evidence for these commands was not found in this inventory. |
| CCPM Buffer Status, Network, Refresh Analysis | MISSING | #794 M2.4 | #453 §5 CCPM spec; product extension | No current command/output evidence recorded; analysis must preserve ordinary CPM schedule when unapplied. |
| CCPM Show Critical Chain / Show Buffers | MISSING | #794 M2.4 | #453 §5 CCPM spec; product extension | No current display-only overlay proof recorded; must not mutate task dates. |
| CCPM model separation, snapshot/rollback, progress/constraint protection, background calculation | UNASSESSED | #794 M2.2/M2.3 | #453 §5; product extension | Required architecture/edge-case acceptance is specified; implementation, atomic failure, concurrent mutation and MPO persistence evidence need separate source audit. |
| CCPM scenarios: empty project, dependency-only chain, shared-resource contention, multiple feeding chains, milestone, completed task, fixed constraint, progress update, Apply/Save/Reload/Clear | UNASSESSED | #794 M2.2/M2.3 | #453 §5; product extension | Scenario list is explicit; no complete fixed-fixture independent expected-results matrix is registered. Do not claim CCPM acceptance. |
| #453 §6 machine-readable command mapping: Action ID, label, icon, group, visibility condition, model mutation, Undo unit, shortcut | PARTIAL | #794 M2.4 | #453 §6; project command catalog is internal evidence | `RibbonCommandCatalog` and route tests provide source metadata, but this inventory is not yet a generated exhaustive field-by-field crosswalk; missing fields and command-level mappings are explicit above. |
| #453 §7 group order, 2024 screen evidence, save/reload workflows and contextual-tab lifecycle | PARTIAL | #794 M2.1–M2.4 | #453 §7 states Project Standard/Professional 2024 and Windows desktop | Local tab order and selected GUI journeys are cited above. Current Microsoft 365/2024 screenshots and a complete group-order/save-reload/context lifecycle matrix are unobserved. |
| #453 no dummy controls for unavailable cloud/share/account features | VERIFIED | #792 | #453 §§2–3; explicit standalone product scope 2026-10-12 | Share/Account and cloud service actions are omitted; source scan confirms no cloud-linking command in standard catalog. Keep local shared-resource collaboration in scope. |
| #769 source-to-action, precondition, visible/model outcome and failure result for each command | PARTIAL | #794 M2.1–M2.4 | #769 acceptance; normative Project 2024 target; sources mapped per rows above | Existing tests cover selected flows; the crosswalk above explicitly identifies unmapped sources, physical outcomes and gaps. |
| #769 Undo/Redo and MPO save/reload for mutating commands | PARTIAL | #794 M2.1–M2.4 | #769 acceptance; product persistence contract | Evidence is named where available; all other mutating command rows explicitly remain unverified. |
| #769 English/Japanese, narrow width, keyboard, high DPI and accessibility | PARTIAL | #794 M2.4 / #765 | #769 acceptance; Project 2024 Windows target | Selected locale/scale and tab journeys exist; no complete all-command accessibility/width matrix is recorded. Physical MSP current-client comparison is unobserved. |

## File format coverage at current master

The repository exposes format handlers; that fact alone is not complete
interoperability. Read and write are separate contracts and must not be inferred
from a filename filter or an XML trailer.

| Format / direction | Current implementation evidence | Status | Owner | Provenance/applicability | Explicit unknown |
|---|---|---|---|---|---|
| MPP read | MPXJ MPPReader via `MspImporter`; `XlsxSupportTest.mspImporterCanReadMpp` opens `New Product.mpp`. | PARTIAL | #796 | Fixture provenance/version must be established per #805; MPXJ is implementation evidence, not a Microsoft source/version. | Field-level semantics and Project 365/2024 input provenance remain incomplete. |
| MPP write | No MPP writer in `ProjectWriterFactory`; `FileHelper` rejects `.mpp` writes. | EXCLUDED | #796 | Explicit user scope decision 2026-10-12; Microsoft version not applicable. | This exclusion does not imply MPP read compatibility is complete. |
| Microsoft Project XML / MSPDI read | `MspImporter` selects MPXJ MSPDIReader; tests include Microsoft XML inputs. | PARTIAL | #796 | Microsoft format; exact authoring edition/build provenance is not recorded for every fixture. | Semantic matrix and broader fixture provenance/coverage are incomplete. |
| Microsoft Project XML / MSPDI write | `ProjectWriterFactory` maps XML to MSPDIWriter; `MpxExportTrackingTest` and `MpxProjectConverterTest` cover selected exports/reloads. | PARTIAL | #796 | XML interoperability fixtures do not establish a particular Project edition/build unless each fixture records it. | Focused round trips do not prove full Project XML compatibility. |
| MPX read | `MspImporter` selects MPXReader. | PARTIAL | #796 | MPX implementation path is not versioned Microsoft Project evidence. | Exact field/version limits and field-loss matrix remain to be recorded. |
| MPX write | `ProjectWriterFactory` maps MPX to MPXWriter. | PARTIAL | #796 | MPX implementation path is not versioned Microsoft Project evidence. | Exact field/version limits and field-loss matrix remain to be recorded. |
| MPO native read | `MpoFileImporter` and importer/archive tests cover native read and backward-version handling. | VERIFIED | #796 | Product-owned format; Microsoft applicability not applicable. | Retain earlier supported MPO backward reads and field-level round trips; no MSP parity claim. |
| MPO native write | `MpoFileImporter` archive/container writer and transaction tests cover native write behavior. | VERIFIED | #796 | Product-owned format; Microsoft applicability not applicable. | Product-native contract only; no MSP parity claim. |
| POD compatibility | `PodRoundTripTest` covers established POD serialization and embedded MSPDI data. | PARTIAL | #796 | Frozen microProject legacy format; Microsoft applicability not applicable. | Preserve wire layout/fixed fixtures; current chooser/writer exposure remains a policy discrepancy. |
| XLSX read | `ProjectLibreXlsxReader` and `XlsxSupportTest` cover native payload, summary sheets, and MSPDI fallback. | PARTIAL | #796 | ProjectLibre XLSX native payload and external MSPDI fallback have distinct contracts; no Project edition implied. | Field-loss and external workbook semantic matrix remains incomplete. |
| XLSX write | `ProjectLibreXlsxWriter` and `XlsxSupportTest` cover native payload and MSPDI worksheet output. | PARTIAL | #796 | ProjectLibre XLSX native payload and external MSPDI fallback have distinct contracts; no Project edition implied. | Field-loss and external workbook semantic matrix remains incomplete. |
| PMXML export | Writer factory registers `PrimaveraPMFileWriter`; no importer path found. | PARTIAL | #796 | MPXJ writer path; Microsoft Project edition not applicable. | Export semantics and user-facing advertisement/contract need a separate row review. |
| GNOME Planner read | `PlannerReader` is registered; this is GNOME Planner file exchange, not Microsoft Planner cloud service. | PARTIAL | #796 | MPXJ exchange path; Microsoft Project edition not applicable. | Semantic import coverage is not established. |
| GNOME Planner write | `PlannerWriter` is registered; this is GNOME Planner file exchange, not Microsoft Planner cloud service. | PARTIAL | #796 | MPXJ exchange path; Microsoft Project edition not applicable. | Semantic export coverage is not established. |
| JSON export | `JsonWriter` is registered; no importer path found. | PARTIAL | #796 | MPXJ writer path; Microsoft Project edition not applicable. | Export contract, semantics and UI advertisement need separate review. |
| SDEF export | `SDEFWriter` is registered; no importer path found. | PARTIAL | #796 | MPXJ writer path; Microsoft Project edition not applicable. | Export contract, semantics and UI advertisement need separate review. |

The default project open filter accepts MPP read while the save filter rejects
it. That is the intended product boundary: MPP is import-only and MPO is the
native save substitute. Do not rename an XML or MPO file to .mpp.

## Local automation/extensions and excluded connected services

The user explicitly excluded Microsoft Project-specific local automation and
extension compatibility on 2026-10-12. This exclusion does not remove
microProject-native local automation/extensions or offline collaboration from
their applicable product scope. Project Server/PWA/Project Online and Microsoft
Planner cloud connections are also outside the standalone product goal.

| Surface | Current source state | Status/owner | Source/applicability and evidence |
|---|---|---|---|
| Microsoft Project VBA/macros and macro-facing Project object model | No Microsoft Project-compatible desktop VBA runtime or macro object model is exposed in the current UI/source scan. | EXCLUDED; #797 | Explicit user scope decision 2026-10-12; Microsoft edition not applicable. Does not exclude microProject-native APIs. |
| Microsoft Project COM automation/add-ins, VSTO, local Office task-pane add-ins | No Microsoft Project COM automation/add-in bridge or Office add-in host is present in the current Java desktop implementation. | EXCLUDED; #797 | Explicit user scope decision 2026-10-12; Microsoft edition not applicable. |
| Project Server / PWA / Project Online cloud connection | Desktop application has project-file/local collaboration flows; no Microsoft server connection workflow was found in this audit. | EXCLUDED; #792 | Explicit standalone desktop scope; no client or dependency should be added. |
| Microsoft Planner linking / cloud account | Backstage omits Share/Account and no cloud-linking action is in the standard ribbon catalog. | EXCLUDED; #792 | Explicit standalone desktop scope. GNOME Planner file exchange is a separate in-scope file row above; Microsoft Planner cloud service is not. |

## Office customization inventory (#770)

Microsoft's Windows Office guide describes adding/reordering/renaming/hiding
tabs; adding/reordering/renaming/removing custom groups; adding/removing and
reordering commands in custom groups; reset all or a selected default tab; and
export/import of ribbon and QAT settings. File cannot move or hide. Default
commands cannot be renamed, have their icons changed, or be reordered. Only
custom tabs can be removed. Import replaces the current ribbon and QAT
customizations.

| Capability | Status | Owner | Source/applicability | Evidence and explicit unknown |
|---|---|---|---|---|
| Ribbon display: Auto-hide / Tabs only / Always show | VERIFIED | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021; build/date not stated. | Physical display-option journeys and locale/scale visual matrix verify display only; user-edited ribbon composition is a separate capability. |
| QAT add/remove, append order, reset | PARTIAL | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021; build/date not stated. | `OfficeChromeRibbonDisplayGuiAcceptanceTest` verifies add/reset; import/export and production-shell restart acceptance remain open. |
| Add, rename, reorder, hide custom/default tabs; remove custom tabs | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | No customization UI/setting route in current implementation. |
| Add, rename, reorder, remove custom groups and add commands to them | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | No custom group editor or persisted user configuration. |
| Rename custom commands and configure their display/icon | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | No custom command configuration route. |
| Reset all ribbon customization and reset one default tab | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | Current QAT reset does not implement ribbon-wide or default-tab reset. |
| Export/import ribbon and QAT configuration | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | No configuration file import/export path. |
| Persist custom ribbon layout across process restart | MISSING | #770 | S5 lists Project Online Desktop Client, Project Professional/Standard 2024 and 2021, plus Microsoft 365 products; build/date not stated. | Custom tabs/groups do not exist; QAT production-shell restart remains unverified. |

Do not implement these with a second ribbon renderer or a second Action layer.
User configuration is user-scoped, separate from project data. Old application
and internal-API compatibility is not required.

## Project-file persistence boundary

MPO is the primary native project format and the required save/reload target for
new project features and MSP command acceptance. MPO must continue reading
existing files; new optional, versioned MPO metadata may carry new feature
state.

POD's serialized schema is frozen. Do not add serialized fields, record
components, alter class descriptors, or otherwise change its format. New
features do not need full POD persistence or behavioral parity when POD cannot
represent them. Keep existing POD open/save behavior where it already applies,
but test new feature persistence through MPO and document any POD limitation
instead of extending the POD schema.

## Review and closure rule

The code catalog proves what the current renderer advertises. It does not prove
MSP compatibility. Each implemented command requires a documented source,
selection/precondition contract, one canonical Action route, before/after
model and view assertions, Undo/Redo where it mutates project data, MPO
save/reload where relevant, and a physical Robot journey when the ribbon route
changes. Visual compatibility (#765) is reviewed separately using current
Office references and screenshots. Keep #453, #769 and #770 open until their
rows are implemented or explicitly resolved with an evidence-backed
unsupported decision.

## M0 inventory checkpoint (#792)

This document is the canonical current inventory; historical issue bodies are
not a second source of truth. M0 is an inventory-readiness gate, not a demand
that every capability be implemented. `MISSING`, `PARTIAL`, `UNSUPPORTED`, and
`EXCLUDED` are valid outcomes when the row includes its owner, source and
applicability, evidence, and explicit unknowns. They do not alone block M0.

The crosswalk above enumerates the named command/surface groups in #453, the
acceptance themes in #769, and every Office customization row in #770. Each
grouped capability cell lists the finite commands or views included in that
row; missing source mapping, test evidence, and unknown outcomes are stated in
its evidence cell. The exchange-format table separately enumerates each
read/write/native/frozen format direction. Microsoft support pages record the
editions in their current “Applies to” metadata; publication/build dates are
unknown where the pages omit them. Unobserved versions and incomplete test or
fixture provenance are not reported as passes. For exact source pages, use
IDs S1–S10; where no primary source/version is mapped, the row states that
gap. The #769 Inactivate statement is flagged as a source/code conflict rather
than silently treated as current evidence. MPP write remains explicitly
excluded. Respect Links and the two sort inventories have current
implementation/test evidence, but their broader #794 work remains open.
Project Online Desktop Client in old issue wording means the standalone
desktop application; connected Project Online services remain excluded.

M0 is ready for maintainer review against #792 acceptance: verify the finite
listed families and command names cover #453/#769/#770, and confirm the
explicit row dispositions and source-version unknowns are adequate; close
#792 if accepted. This
checkpoint does not claim overall compatibility completion; #794/#796 remain
open and the #795 release gate remains blocked by those feature/format gaps.
