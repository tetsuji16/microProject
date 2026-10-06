# MSP standard ribbon command inventory (#453 / #769)

This is a current source-to-spec inventory for the microProject desktop ribbon.
The target is Microsoft Project desktop command behavior presented through the
current Microsoft 365 ribbon visual language. Microsoft Project's task and
resource semantics remain the compatibility oracle for Project commands; the
Office customization guide is the oracle for ribbon customization mechanics.
An inferred rule is identified as document-derived, not as an observed MSP
result. No row claims pixel-level equivalence.

## Official baseline

- [Learn the Project ribbon](https://support.microsoft.com/en-us/office/learn-the-project-2010-ribbon)
- [Distribute project work evenly (level resource assignments)](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments)
- [Resource Leveling dialog box](https://support.microsoft.com/en-us/project/resource-leveling-dialog-box)
- [Update work on a project](https://support.microsoft.com/en-us/project/update-work-on-a-project)
- [View resource workloads and availability in Project desktop](https://support.microsoft.com/en-us/project/view-resource-workloads-and-availability-in-project-desktop)
- [Customize the ribbon in Office](https://support.microsoft.com/en-us/office/foundations-experiences/customize-the-ribbon-in-office)

## Current structure

| Surface | Current source state | Status and verification |
|---|---|---|
| File | File tab opens a Backstage view; commands route through canonical Actions | Implemented; `RibbonStructureTest`, Backstage Robot acceptance. Share/Account are omitted because this desktop fork has no cloud-account service. |
| Task, Resource, Report, Project, View, Help | Seven normal tabs, in this order after File | Implemented as tab structure; `RibbonStructureTest` and `RibbonTabGuiAcceptanceTest`. This does not mean all MSP commands within each tab are implemented. |
| Format, Network Format, Calendar Format | Separate contextual surfaces | Implemented for available views; `ContextualRibbonVisibilityContractTest` and ribbon Robot acceptance. |
| Flamingo renderer | Not used | Required architecture decision: Swing `ModernRibbonPanel`, see ADR-0002. There is one ribbon renderer. |
| QAT | Save/Undo/Redo defaults; implemented actions can be added/removed, retained order is stable, new entries append, reset restores defaults | Add and reset physically verified in `OfficeChromeRibbonDisplayGuiAcceptanceTest.quickAccessCustomizationAddsAndPersistsCanonicalRibbonAction`. Full ribbon configuration import/export is not implemented. |

## MSP command inventory

`SUPPORTED` means the named command is wired to an implementation; it does not
mean the full Project product has been reproduced. `PARTIAL` means the visible
command has a narrower implementation than MSP or lacks one of MSP's required
settings. `MISSING` means no equivalent command is available. Unsupported
product services must remain absent instead of appearing as enabled no-op
buttons.

| MSP capability | Current ribbon/action | Status | Known gap and evidence |
|---|---|---|---|
| Insert, delete, copy, cut, paste tasks | Task ribbon canonical Action routes | SUPPORTED | Route/state evidence in `CommandRouteMatrixTest`, `RibbonButtonBehaviorTest`, and task Robot journeys. |
| Indent/outdent, expand/collapse, move tasks | Task > Outline/Schedule | SUPPORTED | Canonical hierarchy command route with model, visible rows, Undo/Redo and persistence coverage in task GUI acceptance. |
| Link/unlink tasks | Task > Schedule | SUPPORTED | Canonical dependency route; route and model behavior covered by `CommandRouteMatrixTest` and dependency tests. |
| Task Information, Notes, Custom Fields, Assign Resources | Task ribbon | SUPPORTED | Dialog/assignment journeys exist; audit each command's full invalid-state behavior before parity closure. |
| Manual/Automatic task mode | Task > Schedule | SUPPORTED | `TaskModeServiceTest` and MPO round trip. No POD layout change is allowed for this or any other feature. |
| Mark on Track, Update Tasks, Update Project, Status Date | Task/Project ribbon | SUPPORTED | See `msp-task-command-contract.md`; selection, schedule, Undo/Redo and MPO evidence is recorded there. |
| Level Selection | Resource > Level | PARTIAL | Action exists. MSP defines selected tasks as the scope and allows multi-selection. Exact preservation of nonselected task dates/assignments, default clear behavior, and physical acceptance must be verified before parity closure. |
| Level All | Resource > Level | PARTIAL | Action exists and a Robot journey verifies all-project scope, selection preservation, Undo/Redo and MPO round trip. MSP defaults to clearing prior leveling effects, excludes proposed resources by default, and exposes split/manual-task controls. Current leveling does not yet provide the complete settings/provenance contract; issue #772 tracks provenance. POD format must remain byte/schema compatible. |
| Level Resource | Resource > Level | PARTIAL | Existing `LevelResourcesAction` is registered, but MSP's resource-specific selection behavior and multi-resource case require a route/result audit. |
| Leveling Options | Resource > Level | PARTIAL | Existing `RibbonLevelResources` opens the resource-leveling dialog. Compare every MSP option (calculation, sensitivity, range, order, proposed resources, manually scheduled tasks, split tasks) and its effects before claiming parity. |
| Clear Leveling | Resource > Level | MISSING | No dedicated clear-leveling ribbon Action was found. Requires safe provenance so clearing removes only leveling-created delays/splits and preserves manual edits. |
| Next Overallocation | Resource > Level | MISSING | No dedicated command or task-navigation result was found. Microsoft documents navigation to the next task with overallocated resources. |
| Task Inspector | Task | MISSING | No equivalent inspector route found; Microsoft documents a pane explaining scheduling factors and suggested changes. Do not bind a dummy Action. |
| Respect Links, Inactivate Task, Format Painter | Task | MISSING | No equivalent standard-ribbon Action found in current menu/catalog definitions. Determine MSP scope and implement or intentionally omit with a documented product reason. |
| Select/Clear and Summary/Milestone insertion | Task | MISSING/PARTIAL | No matching complete command set was found in current Task tab definitions. Recheck the standard Project edition and command semantics before implementing. |
| Entire Project / Selected Tasks zoom | View | MISSING | Current `RibbonZoomIn`/`RibbonZoomOut` are incremental. The named MSP scope commands and their selection requirements are not present. |
| Report/Timeline object-specific formatting | Contextual tabs | PARTIAL | Contextual surfaces exist for Network and Calendar. Report/Timeline object selection surfaces and their supported commands are not complete. |
| Cloud account, Project Online/Planner linking, VBA/Macros, Microsoft feedback/training services | File/Help | NOT SUPPORTED | This local desktop fork has no account/cloud or macro execution service. Keep these absent; local Help/About are product replacements, not MSP parity. |

## Office customization inventory (#770)

Microsoft's Windows Office guide describes adding/reordering/renaming/hiding
tabs; adding/reordering/renaming/removing custom groups; adding/removing and
reordering commands in custom groups; reset all or a selected default tab; and
export/import of ribbon and QAT settings. File cannot move or hide. Default
commands cannot be renamed, have their icons changed, or be reordered. Only
custom tabs can be removed. Import replaces the current ribbon and QAT
customizations.

| Capability | Current state |
|---|---|
| Ribbon display: Auto-hide / Tabs only / Always show | Implemented; physical display-option journeys and locale/scale visual matrix exist. |
| QAT add/remove, append order, reset | Implemented; add and reset physical Robot journey passed 2026-10-06. |
| Add, rename, reorder, hide custom/default tabs; remove custom tabs | Missing. |
| Add, rename, reorder, remove custom groups and add commands to them | Missing. |
| Rename custom commands and configure their display/icon | Missing. |
| Reset all ribbon customization and reset one default tab | Missing. |
| Export/import ribbon and QAT configuration | Missing. |
| Persist custom ribbon layout across process restart | Missing for custom tabs/groups; QAT persistence needs a production-shell restart acceptance test. |

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
