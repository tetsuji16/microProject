# Issue #729 — Duplicate responsibility audit

State: Final inventory recorded; closes when the integrating PR merges.
Issue: https://github.com/tetsuji16/microProject/issues/729
Contract frozen at: issue body with the 2026-10-27 completion note. Duplicates found after this pass get their own issues.
Baseline HEAD: `a05d74c60` (origin/master)

## Goal and scope

One final audit of all production modules, listing every candidate as consolidated, registered as its own issue, or kept with a reason.

Out of scope: test and guiTest trees, third-party library internals, and locale bundle contents.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| C1 | All production modules and runtime registrations audited | pass | Method below |
| C2 | Every candidate classified with a reason | pass | Inventory below |
| C3 | Confirmed consolidation targets resolved, verification recorded | pass | Consolidated table; verification in the PR |

## Method

- Token clones: a 70-token window with literals abstracted over 1,211 files and about 1.05M tokens in `modules/*/src/main/java`, giving 344 pairs, plus 44 groups of same-signature static helpers. Every pair was read and classified.
- Semantic sweep by behaviour, following callers: runtime registration, XML parser setup, atomic moves, zip extraction, date and duration helpers, escaping, file-type checks, colour and EDT helpers, dirty and save state, close paths, selection, locale bundles, preferences, logging, version checks, OS checks, browse, numeric and working-time validation.
- Registration: the only `META-INF/services` file is `SessionImporterProvider`, read by `LocalSession`. All 83 `com.microproject.*` class names in `src/main/resources` resolve to source files.
- Dead code claims were checked in every source set and by string search.

Not covered in depth: application exit path, `GraphicManager:3812` and `:3867` dirty callers, and `*60` conversions outside the listed files.

## Consolidated in this change

| Responsibility | Canonical location | Replaced |
|---|---|---|
| Atomic move with fallback | `SafeFileReplace.moveAtomically` | `TemporaryWorkspace`, `AutoRecoveryStore`, `MpoFileImporter` (3), `MpoExtractionSession` (2), `CollaborationMetadataStore`, `UpdateChecker` |
| Hardened XML parser | `MspdiProjectBoundary.secureDocumentBuilderFactory` | 5 `MpoFileImporter` sites, `MSPDISerializer`. Four sites gain `ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_SCHEMA` |
| Temporary root | `TemporaryRoot` | `TemporaryWorkspace`, `MpoExtractionSession` |
| Identity list helpers | `IdentityCollections` | `ResourceLevelingService`, `DocumentFrame`, `CalendarService`, `SharedResourcePoolService`, `TeamPlannerService` |
| Windows check | `Environment.isWindows` | `FlatLafSupport`, `UpdateChecker`, `RuntimeErrorLogging`, `AlternativeCalendarDisplay`. "Darwin" no longer counts as Windows |
| MPO file name check | `FileHelper.isMpoFile` | `AutoRecoveryStore`, `CollaborationSession`, `CollaborationMetadataStore`, `ProjectMergeService`, `GraphicManager:664` |
| Import extension default | `FileHelper.getFileExtension(name, default)` | `MicrosoftImporter`, `MspImporter` |
| Markup escaping | `MarkupEscape` | `CriticalChainReportService`, `MpoFileImporter`, `WelcomeDialog`, `TeamPlannerDialogBox` |
| Browser launch | `BrowserControl` | `UpdateChecker` |
| MSPDI DOM and wall time | `MspdiProjectBoundary` | `MSPDISerializer`, `MpxCustomFieldsConverter`, `MspImporter` |
| Rate unit mapping | `MpxUtils.toProjectTimeUnit` | `MpxResourceConverter` |
| CCPM settings and history reading | `MpoFileImporter` private helpers | JSON and XML settings copies, history point and retraction copies |
| Import failure | `MicrosoftImporter.requireImportedProject` | `parseInput`, `parseFile` |
| Operation record JSON | `OperationLog.operationNode` | `OperationLog.write` |
| Buffer history undo state | `BufferHistoryState` | `CriticalChainBufferHistoryService`, `CriticalChainService` |
| Field lookup | `SpreadSheetFieldArray.getFromId` | `FieldArrayUtil` |
| Overallocated bucket | `ResourceLevelingService.firstOverallocatedBucket` | `isOverallocated`, `firstBlockingInterval` |
| Display value, Swing job body | `TaskSheetScheduleWorkflow.displayValue`, `Job.runSwing` | Local copies |
| Locale code parsing | `ConfigurationFile.getLocaleCodes` | `getLocale`. `getLocaleCodes` no longer overwrites the cached locale |
| UI helpers | `findNode`, `CalendarEditorSupport`, `GraphPopupMenu.BarMenuAction`, `AbstractNetworkLayout.findShape`, `NetworkParams` defaults, `DependencyDialog.chooseDependency`, `ColorLuminance` | `ViewNodeModelCache`, calendar editor panels, Gantt and network popups, PERT and XBS layouts, `Network` and `NetworkParamsImpl`, `CalendarViewDialogBox` and `DocumentFrame`, `ChartLegend` and `FormComponent` |

Dead code removed: `MicrosoftImporter.clampProgress`, `toAppletVersion` in `Messages` and `VersionUtils` (with `VersionParseTest`), `QuadraticGanttLinkRouting`, `GraphicManager.saveLocalDocument(Project,String)`, `OptionsFilter.getOptionValues`, both `clearLastTable`, `CalendarOptions`, `MpxOptionsConverter`, `ResourceUtil.getBundles`.

Behaviour differences accepted: a file named only `.mpp`, or ending in a dot, now defaults to XML import in `MspImporter`; a null MPX rate unit maps to minutes instead of failing; Swing HTML gains `&quot;`; `xmlEscape(null)` yields an empty string.

## Registered as separate issues

| Issue | Candidates |
|---|---|
| #978 | Task command precondition ladder in `DocumentFrame`, including the collaboration lock used by only one command |
| #979 | Cell editor `lastTable` split, name cell population, typed-key predicate |
| #980 | Snapshot transaction scaffold, dependency rollback, event batch state machines |
| #981 | Saved and dirty transitions, dirty rollback, window title builder |
| #982 | MPO and MSPDI writer envelopes, archive budget ladder (persisted bytes) |
| #983 | Dark checks, colour blends, ribbon button styling, link arrows, tooltip overrides, EDT wait wrappers |
| #984 | Configuration registries and local duplicates listed in the issue |

## Kept with a reason

| Candidate | Classification | Reason |
|---|---|---|
| `SessionImporterRegistry` and `PortRegistry` | Different contract | Session load and save workflow versus typed results; metadata already shared (#763). `requireKey` differs only in message |
| `.pod` class-name aliases, `deserialize` methods, `HasKeyImpl`, `SerializedDataObject`, `AssignmentData`, `TaskData`, `GroupData`, `RoleData` | Compatibility bridge | POD wire layout is immutable |
| `PertView` and `TreeView` `Workspace` | Compatibility bridge | Persisted view settings with distinct serial IDs |
| `buildLegacySidecarFile` | Compatibility bridge | Reads existing sidecars |
| `addDigesterEvents` tables | Different contract | Declarative rules bound to configuration XML by name |
| `ModernRibbonPanel` and Flamingo | Different contract | ADR-0002 keeps the Swing renderer |
| `core.time.Duration`, `Rate`, `TimeTypeBridge` | Kept API | `docs/issue-architecture-decisions.md` requires an explicit API decision |
| Same-named types from #152 | Different contract | Different models and APIs |
| `DependencyDialog` and `XbsDependencyDialog` | Different contract | Editable dialog versus remove-only dialog |
| `DefaultGanttLinkRouting` and removed quadratic routing | Different contract | Different geometry; only the prelude matched |
| `AssignmentCalendarNotifications` and `AssignmentCalendarSupport` | Different contract | Handler dispatch versus once-per-task wrapper |
| `UpdateChecker.deleteQuietly` and `TemporaryWorkspace.deleteQuietly` | Different contract | Cleanup queue versus retry markers |
| `JXXMonthView`, `ContribIntervals` and core date helpers | Module boundary | `contrib` cannot depend on core |
| `ContribIntervals` and `Intervals` | Different contract | Different element types across that boundary |
| `FieldAliasDialog` and `RenameDialog` | Different contract | Only rename checks uniqueness |
| `MpxExceptionConverter` and `MpxRangeConverter` | Different contract | Cross-midnight ranges are skipped versus defaulted |
| `PersonalContour` before and after buckets | Different contract | Opposite split, filler only after |
| `NewProjectAction` unsaved prompt and `getRemoveProjectJob` | Different contract | Different routes; both gate on `needsSaving()` |
| `ClassLoaderUtils.compareJavaVersion` and `VersionUtils.compareVersions` | Different contract | Runtime versions versus release tags |
| `isProjectFileName` and `isMicrosoftProjectFile` | Different contract | Startup argument filter versus importer type |
| Percent validation in `MpoTaskOperationService`, `DisplayMath`, `CriticalChainBufferChartPanel` | Different contract | Strict input, display clamp, and a 0 to 100 scale |
| `ProjectWriterFactory`, `SwingFileChooserProvider`, `LocalSession`, `LocalFileImporter` extension parsing | Different contract | Different failure and case rules |
| `GraphicManager:3969-3972` extension handling | Different contract | Appends instead of replacing |
| `LoadOptions` and `SaveOptions`, `forAllAssignments`, `getCustom*` | Different contract | Separate option types; traversal and field dictionaries differ |
| `DelegateTaskDialog` and `UpdateTaskDialog` forms | Different contract | Only the name label is shared (#984) |

Boilerplate, not merged: enum `fromCode` tables and `createReverseMap`; earned-value and field-dictionary accessors in `Project`, `NormalTask`, `EnterpriseResource`, `Assignment`, `Task`, `TaskSnapshot`; theme palettes and `UIManager` tables; scripted formula beans (`ActionListFactory`, `CellStyleFactory`, `NodeFilterFactory`, `NodeTransformerFactory`, `BarStyle`); `DataUtil` reflective wrappers; read-only dialog rows; `MenuFactory` accelerator parser and `OfficeChromePanel` icon geometry; `Gantt` and `Network` no-op stubs; `DependencyCreationEdit` and `DependencyDeletionEdit` mirror undo; `CollaborationMetadataStore` nested record getters; JAXB `Field` and `FieldList`.

## Earlier resolved items

#730, #763, #739, #741, #759, the FlatLaf check (`96ddb96ff`), Undo edit creation (`b9be93bcf`), the document frame ledger (`dde3bd721`), unreachable ribbon factories, and the legacy paste adapter (`cb56f84d6`).

## Remaining blockers

- none. Open architecture work stays in #408, #414, #737, #738 and #483.

## Latest evidence

- Tests/build: recorded in the PR and on the issue against the merged SHA.
- Physical GUI/manual: no route changes; GUI smoke runs in PR CI.
