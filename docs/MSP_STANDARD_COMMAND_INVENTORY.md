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
| Next Overallocation | Resource > Level | IMPLEMENTED | [Microsoft Project desktop guidance](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments) defines this as navigating to the next task with overallocated resources. Document-derived compatibility decision: traverse visible task rows after the current selection, wrap to the first visible candidate, and do not mutate model data. Hidden/filtered tasks are excluded by resolving candidates through current view rows. Robot coverage verifies next, wrap, no-candidate feedback, and schedule preservation. |
| Task Inspector | Task | MISSING | No equivalent inspector route found; Microsoft documents a pane explaining scheduling factors and suggested changes. Do not bind a dummy Action. |
| Respect Links | Task | MISSING | A source scan of current production core/UI found no Respect Links command/service or standard-ribbon Action. Confirm Microsoft TaskRespectLinks scope, then implement the canonical route and tests; no parity claim. |
| Inactivate Task | Task | PARTIAL | The domain Task has an inactiveTask property and schedule diagnostics understand inactive tasks, but the current standard ribbon has no Inactivate action in menuInternal.properties or RibbonCommandCatalog. The field/model alone does not satisfy the Project command. |
| Format Painter | Task | MISSING | No equivalent command in the current standard ribbon/catalog. Define copied scope and formatting target from the Project desktop source before implementation. |
| Task Inspector | Task | MISSING | No inspector pane/action found. Microsoft documents scheduling factors; implement its selection-following results and physical route before parity claim. |
| Select/Clear and Summary/Milestone insertion | Task | MISSING/PARTIAL | Current ribbon has normal task, recurring task, and subproject insertion. The complete MSP selection/clear command set and Summary/Milestone insertion behavior are not exposed by current Task ribbon definitions. |
| Entire Project / Selected Tasks zoom | View | PARTIAL | Entire Project is physically verified in Gantt, Task Usage, and Resource Usage. Selected Tasks is verified only in Gantt; current Usage selection does not expose a task snapshot, and microProject has no Resource Graph equivalent. See #769 and its 2026-10-07 updates. |
| Report/Timeline object-specific formatting | Contextual tabs | PARTIAL | Contextual surfaces exist for Network and Calendar. Report/Timeline object selection surfaces and their supported commands are not complete. |
| Cloud account, Project Online/Planner linking, VBA/Macros, Microsoft feedback/training services | File/Help | OUT OF SCOPE or OPEN LOCAL EXTENSION GAP | Project Server/PWA/Project Online/Planner cloud accounts and connected services are outside the standalone product goal; do not add clients or dummy controls. VBA/macros remain an unresolved local automation gap under M4. Local Help/About are product replacements, not MSP parity. |

## File format coverage at current master

The repository exposes format handlers; that fact alone is not complete
interoperability. Read and write are separate contracts and must not be inferred
from a filename filter or an XML trailer.

| Format / direction | Current implementation evidence | Coverage status |
|---|---|---|
| MPP read | MPXJ MPPReader is selected in modules/microproject_exchange/src/main/java/com/microproject/core/pm/exchange/MspImporter.java. XlsxSupportTest.mspImporterCanReadMpp opens the checked-in New Product.mpp fixture. | PARTIAL: one fixed project opens; field-by-field semantic compatibility and supported Project versions are not exhaustively verified. |
| MPP write | ProjectWriterFactory maps MPX, XML, PMXML, PLANNER, JSON, SDEF and XLSX writers; it has no MPP entry. XlsxSupportTest.fileHelperAcceptsMppForReadOnlyImport asserts .mpp is rejected for writing. | MISSING: no current MPP writer path. This is a hard M3 blocker. |
| Microsoft Project XML / MSPDI read | MspImporter chooses MPXJ MSPDIReader for XML. Existing import and conversion tests include Microsoft XML inputs. | PARTIAL: add a documented semantic matrix and broader fixture coverage. |
| Microsoft Project XML / MSPDI write | ProjectWriterFactory maps XML to MSPDIWriter. MpxExportTrackingTest and MpxProjectConverterTest cover selected exported fields and reloads. | PARTIAL: existing focused round trips do not prove complete Project XML compatibility. |
| MPX read/write | Import is selected in MspImporter; ProjectWriterFactory maps MPX to MPXWriter. | IMPLEMENTED as a legacy exchange path; record its exact field/version limits separately from MPP compatibility. |
| MPO native read/write | MpoFileImporter and the MPO archive/transaction/importer tests cover native read, write and versioned archive behavior. | IMPLEMENTED for the documented microProject format; continue backward-read and round-trip acceptance. |
| POD compatibility | PodRoundTripTest covers established POD serialization and embedded MSPDI data. | Existing compatibility path; keep the serialized wire format immutable and retain fixed fixtures. Do not add serialized fields or change descriptors. |
| XLSX read/write | ProjectLibreXlsxReader/Writer and XlsxSupportTest cover native payload, summary sheets and selected round trips. | IMPLEMENTED for the declared XLSX contract; maintain a field-loss inventory. |
| PMXML, Planner, JSON, SDEF | Writer factory includes these output handlers. | EXPORT handlers exist; importer support and Project desktop parity are not implied. Track format-specific read/write scope independently. |

The default project open filter accepts MPP read while the save filter rejects
it. MPP writing therefore must be added as a separate, carefully scoped
interoperability implementation; do not rename an XML or MPO file to .mpp.

## Local automation/extensions and excluded connected services

Local desktop automation and extension behavior remains part of the compatibility
goal. Project Server/PWA/Project Online and Microsoft Planner cloud connections
are outside the standalone product goal and are not missing-feature blockers.

| Surface | Current source state | Compatibility disposition |
|---|---|---|
| VBA / macro execution | No desktop VBA runtime or macro command was found in the current UI/source scan. | OPEN LOCAL AUTOMATION GAP; inventory and implementation/verification decision required in M4. |
| COM automation/add-ins, VSTO, local Office add-ins | No COM automation/add-in bridge or Office add-in host found in the current Java desktop implementation. | OPEN LOCAL EXTENSION GAP; inventory and implementation/verification decision required in M4. |
| Project Server / Project Online connection | The desktop application has project-file and local collaboration flows; no Microsoft server connection workflow was found in this audit. | OUT OF SCOPE for the standalone product goal; no client or dependency should be added. |
| Microsoft Planner linking / cloud account | Backstage omits Share/Account and no cloud linking action is in the standard ribbon catalog. | OUT OF SCOPE for the standalone product goal; do not render dummy controls. GNOME Planner file exchange is tracked separately in the format inventory. |

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
