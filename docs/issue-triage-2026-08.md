# Open issue triage — revalidated 2026-09-27

This inventory was rechecked against GitHub's open issue list on 2026-09-27 and
the current worktree. The open issues are #595, #228, #215, #152, and #84. An issue remains open on GitHub when implementation is
waiting for reporter confirmation, is intentionally tracked as an umbrella, or
still has follow-up work; the open state alone does not mean that its original
bug is still reproducible.

## Status

| Issue | Current status | Evidence / remaining work |
|---|---|---|
| [#366](https://github.com/tetsuji16/ProjectLibre/issues/366) | Completed; closed 2026-08-29 | GitHub's final acceptance audit records distinct Report / Custom Report / Resource Graph / Report Chart routes, project-scoped four-template persistence, separate chart data contracts, automatic report refresh, and 25 tests with zero failures/errors. The implementation is present in the current tree; the former partial status was stale. |
| [#356](https://github.com/tetsuji16/ProjectLibre/issues/356) | Implemented for current MPOF drafts | `MpoFileImporter` reads earlier draft layouts and rewrites them in the current MPOF layout. `MpoFileImporterTest` covers current/draft manifests, checksums, round trips, and unknown entries. Future format versions still require an explicit migration policy. |
| [#351](https://github.com/tetsuji16/ProjectLibre/issues/351) | Completed locally | README baseline metrics were recomputed from `0530be22`: 2386/2386 paths and 1538205 changed lines. |
| [#347](https://github.com/tetsuji16/ProjectLibre/issues/347) | Implemented | A plain task-cell click now selects the full task row while retaining the clicked cell as a separate active-cell coordinate for its focus border. Ctrl/Shift and keyboard selection behavior are preserved and headless regression-tested. |
| [#344](https://github.com/tetsuji16/ProjectLibre/issues/344) | Completed | MPOF `.mpo`, XML manifest, metadata, settings, JSONL operation log, legacy draft reads, and unknown-entry preservation are implemented. `MpoFormatVersion` centralizes the future-version compatibility policy. |
| [#338](https://github.com/tetsuji16/ProjectLibre/issues/338) | Blocked by release credentials | About-dialog discovery, MSI digest verification/staging, and update4j bootstrap tests exist. A release-grade feed still needs CI access to the update-signing private key plus packaging/release workflow integration; without that key the bootstrap correctly refuses remote replacement. |
| [#430](https://github.com/tetsuji16/ProjectLibre/issues/430) | Partial; environment pending | ProjectLibre CCPM GUI scenarios and the full Robot suite are verified. Direct Microsoft Project comparison and Robot mouse activation in the narrow-ribbon popup environment remain unavailable. |
| [#395](https://github.com/tetsuji16/ProjectLibre/issues/395) | Implemented; native-dialog confirmation pending | Open dialogs now enable multi-selection and `GraphicManager.openLocalProject` opens every selected path; unit coverage verifies the chooser mode. Native OS file-dialog Robot confirmation remains pending. |
| [#330](https://github.com/tetsuji16/ProjectLibre/issues/330) | Focused GUI verified | Installed-distribution Robot coverage and `TaskInformationGuiAcceptanceTest` verify Task Information routing from Gantt double-click and ribbon Information. The issue was closed with the verification evidence recorded in its latest comment. |
| [#267](https://github.com/tetsuji16/ProjectLibre/issues/267) | Focused regression verified | `PodRoundTripTest.groupedTaskFollowsChildDateUpdate` verifies summary-task date rollup and persistence. The issue was closed after the focused verification; direct Microsoft Project comparison remains outside the available environment. |
| [#266](https://github.com/tetsuji16/ProjectLibre/issues/266) | Focused regression verified | `DependencyServiceTest` verifies unlink removes all incident dependencies, with Task Information predecessor/successor coverage. The issue was closed with focused test evidence. |
| [#257](https://github.com/tetsuji16/ProjectLibre/issues/257) | Excluded (clean-room foundation) | This is an explicit foundation for the clean-room type consolidation in #152. It is intentionally outside this change set. |
| [#245](https://github.com/tetsuji16/ProjectLibre/issues/245) | Completed | Every audited choice family has a `Kind` enum and stable integer-code adapter; production callers use typed values. Deprecated integer aliases and serialized/API boundaries remain for compatibility. Scalar constants/sentinels are documented as non-enums. Core tests, POD round-trip tests, UI compilation, and `git diff --check` passed; issue closed after final audit. |
| [#228](https://github.com/tetsuji16/ProjectLibre/issues/228) | Partial | Safe known-size capacity fixes cover serializer deserialization and referring-subproject output conversion, Resource Leveling and CCPM buffers/indexes, hierarchy/subproject snapshots/queries, resource merge matching, Gantt interval normalization, project/resource/task indexes, dependency snapshots, assignment replacement undo details, `WorkingCalendar` copies of known-size recurring-exception and work-week lists, `OperationLog` validation sets, MPXJ task/resource mapping indexes sized from the parsed `ProjectFile` collections, server resource-mapping results sized from the returned collection plus the unassigned-resource entry, `DocumentFrame.Workspace.views` sized for its 15 fixed view entries, `GraphicManager.getSelectedResourcesForTimesheet` sized from selected implementation count, and `ChartLegend` path/resource result lists sized from their source arrays/lists; the project-update target list is sized from the full task list or selected implementations. `ChangeWorkingTimeDialogBox` also pre-sizes its calendar choices from the exact sizes of the base, project, and optional document calendar lists. `CalendarViewDialogBox` pre-sizes click targets to the fixed maximum of 42 days × 3 visible tasks per day. UI compilation passed; this changes allocation only, not the rendered layout or task filtering. `ProjectHierarchyQueries.descendants` estimates result size from the root's known direct-child count; focused regression tests protect recurrence getters, duplicate handling, encounter order, and fallback; Gantt interval regression, full UI tests, full exchange tests, `RibbonButtonBehaviorTest`, and UI compilation after the timesheet / chart-list capacity changes pass. `ChangeWorkingTimeDialogBoxSaveTest` passed after sizing the concatenated calendar list. Remaining default-capacity allocations are mostly dynamic or sparse result collections, static small maps, or no-known-size collections; audit each for a defensible upper bound before changing. |
| [#595](https://github.com/tetsuji16/ProjectLibre/issues/595) | Partial; provenance audit in progress | Continued only on OpenProj-derived hunks with active callers. Recent work consolidates `ClassUtils` natural comparators, Job alert dispatch, and import-recovery alerts, and modernizes active `Project`, `Task`, `Job` timing, `UndoController` node index, `SubProj` field-value map, `ProjectFactory` reflected resource hierarchy and save/project-removal/close traversal callbacks, `Session.getCloseProjectsJob`, `SessionFactory` credentials, `AbstractMutableNodeHierarchy.toList`, `AssignmentEntry` assignment storage, `Messages.getProperties`, `Synchronizer`, `DependencyGraph`, `NodeDeletionEdit`, `DefaultFrameManager.getAllFrames`, `MutableNodeHierarchy` paste associations, `CellCache.baseIndex`, `ReferenceNodeModelCache.buildEdges`, `CommonSpreadSheetModel.getPreviousVisibleNodesFromRow`, `DocumentFrame` selection-restoration callback, `DependencyService`, `FieldDialog`, `StartupFactory`, `GraphicManager`, `VersionUtils`, `UniqueIdPool`, `LafManagerImpl`, calendar `Intervals`, Jasper report `DataSource` / `ReportDefinition` / `ReportAdapter` / `ReportView`, the manually launchable `UILister` diagnostic, the Gantt calendar-format visitor, the chart-legend project-task visitor, the Linker trailing-void prepass, the Gantt popup typed bar-style traversal, DocumentFrame view cleanup, remote-project startup completion, and `Project.getRowHeight` baseline typing. The latest row-height change keeps the erased `SortedSet` descriptor and removes a cast that rejected non-`TaskSnapshot` `DataSnapshot` values; a marker-snapshot regression verifies the row-height result, the full core suite passes, and UI compilation confirms callers. Core, exchange, and UI test suites passed together; latest core tests passed after the hierarchy/paste-association and messages changes, focused AssignmentEntry tests plus UI compilation passed after typing assignment storage, split synchronization lifecycle tests passed after typing `Synchronizer`, PERT dependency graph UI compilation passed after its collection typing, focused DefaultNodeModel undo/redo tests passed after typing the node restoration list, `DefaultFrameManagerTest` passed after typing the frame list, focused cache / chart tests passed after typing `CellCache.baseIndex`, focused reference-cache hierarchy/dependency plus spreadsheet tests passed after typing edge construction, and UI compilation passed after typing the sibling-list API and callers. UI compilation also passed after modernizing the DocumentFrame selection-restoration callback; `ProjectFactoryClosingTest` passed after modernizing the project-removal traversal callback, and full core tests passed after modernizing its server-close traversal callback. Full core and exchange tests passed after modernizing the project-save traversal callback. Focused frame-manager / ribbon / shutdown and reports tests also passed in earlier verification runs. Two earlier UI-suite runs reported one failure in `ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored()`; that class passes in isolation. The OpenProj audit remains incomplete; see `docs/audit/openproj-java25-modernization.md`. |
| [#413](https://github.com/tetsuji16/ProjectLibre/issues/413) | Partial | Calendar View uses a selected `ca` locale chronology or a supported Windows `iCalendarType` mapping (Japanese, Taiwan/Minguo, Thai, Um Al Qura); ISO Gregorian dates remain authoritative. Unit and physical Calendar ribbon Robot coverage pass. Other Windows calendar IDs without equivalent built-in Java chronologies and the remaining calendar columns/headers still need implementation. |
| [#215](https://github.com/tetsuji16/ProjectLibre/issues/215) | Partial; preference coverage expanded | Preferences cover user name, font family/size, row-lines default, default Gantt bar text and position, default Gantt bar color, grid-line color, update checking, auto-recovery interval (1–1440 minutes), and a persisted light/dark application theme. Theme selection takes effect on restart and switches FlatLaf plus the semantic application chrome palette; project work surfaces such as printed pages remain white. Language/locale selection is available through the separate Locale dialog and persists in `ConfigurationFile`; startup uses that locale or falls back to the OS-provided Java default. `DateTime` and `Money` formatters follow the active Java default locale; focused core coverage verifies US vs Germany date and currency output, and a Robot journey physically selects German/Germany and verifies the persisted `de_DE` setting plus corresponding formatter output. Remaining verification: inspect representative date/time/currency fields in an open project document and prove the no-override path uses the OS Java locale. |
| [#204](https://github.com/tetsuji16/ProjectLibre/issues/204) | Implemented | Timeline and Team Planner are embedded through `DockableProjectToolView`, routed through `DocumentFrame` top-view activation, and no longer require the modal Team Planner dialog workflow. |
| [#179](https://github.com/tetsuji16/ProjectLibre/issues/179) | Focused GUI verified | `TaskDurationGuiAcceptanceTest` and `TaskTableGanttGridGuiAcceptanceTest` verify full-row highlight, table/chart synchronization, selection, and drag behavior in the installed distribution. The issue was closed with the verification evidence recorded in its latest comment. |
| [#152](https://github.com/tetsuji16/ProjectLibre/issues/152) | Partial | Removed the unreferenced MPX converter helper chain; exchange uses `datatype` types through `MPXConverter`. Caller audit classifies `core.fields.Field` (legacy JAXB metadata) vs `field.Field` (runtime field descriptor), and `core.nodes.Node` (legacy ID contract) vs `grouping.core.Node` (outline tree contract), as different responsibilities; merging would cross configuration/runtime or hierarchy lifecycle boundaries. MPXJ `getSubProject()` import decoding is a separate exchange concern; internal `Task` and outline-node subproject checks now share `SubProj.isSubprojectReference(Object)`. Deprecated `core.time` types and `TimeTypeBridge` remain public source-compatibility APIs. Remaining same-name pairs and removal policy still need caller and persistence-boundary decisions; see `docs/issue-architecture-decisions.md`. |
| [#84](https://github.com/tetsuji16/ProjectLibre/issues/84) | Partial | Removed the unreferenced `NodeModelUtil.dumpTask` diagnostic after repository-wide search found only its self-recursive definition; no production, test, configuration, or reflection caller was found. Removed obsolete `Serializer` comments for `PersistedAssignment`, deleted task/link ID cache APIs, and the removed `IncrementalData` collector after caller searches found no definitions or active references. Kept the separate commented summary-task recovery sketch for compatibility review. `FieldComponentMap` now reuses its existing checkbox predicate instead of repeating the same `instanceof` check. The unused `id` field in `Project.setAllTasksAsUnchangedFromPersisted` was removed after caller and body review. Full UI and exchange tests passed after these cleanups; full core tests passed after the method removal, and POD round-trip passed for adjacent serializer work. Continue report-only inventory and delete only individually verified dead code. |
| [#63](https://github.com/tetsuji16/ProjectLibre/issues/63) | Focused GUI verified | `TaskFontStyleTest`, `TaskInformationDialogTest`, and `TaskInformationGuiAcceptanceTest` verify font customization behavior and persistence paths. The issue was closed after focused GUI verification. |
| [#45](https://github.com/tetsuji16/ProjectLibre/issues/45) | Focused GUI verified | `SpreadSheetMouseInteractionTest` and `RibbonButtonBehaviorTest` verify keyboard/drag task movement, insertion feedback, undo, and drag-and-drop preference behavior. The issue was closed with focused test evidence. |
| [#36](https://github.com/tetsuji16/ProjectLibre/issues/36) | Partial | Resource assignment spreadsheet and ribbon command routing were fixed. Remaining audit items are resource-sheet bindings, usage-view time-phasing, resource information layout, and assignment-pane usability. |

## Follow-up recorded 2026-09-27

For #152, the unreferenced `com.microproject.contrib.ClassLoaderUtils` duplicate was removed after repository-wide production, configuration, and reflection searches found no callers; its Java-version comparison cases now test the canonical core implementation. `:microproject_core:test :microproject_contrib:test --console=plain` passed. For #595, active `ViewTransformer` and `ProjectFactory` callbacks now use Java 25 lambda/method-reference syntax, and `Task.arrangeTask` types its output as `Collection<? super TaskReference>` without excluding the existing `List<Object>` caller. `TransformListTest`, `ProjectFactoryClosingTest`, `NormalTaskDurationTest`, and full `:microproject_core:test --console=plain` passed. For #228, `TransformList.getFactories(view, type)` now pre-sizes its filtered result to the factory count, a strict upper bound; its order/filter test passed. For #215, the preferences screen now persists a light/dark theme choice, warns that restart is required, and initializes matching FlatLaf and semantic application colors. Global preference/theme unit tests, full core/UI unit suites, and the physical Preferences GUI acceptance journey passed with Japanese defaults and English at 100% scale. Dark application chrome is implemented; document/print work surfaces remain white for readability. `ConfigurationFile` falls back to the OS-provided Java locale and startup applies the selected locale globally. Core `MoneyTest` verifies US/Germany date and currency output; a Robot journey now chooses German/Germany through Locale Settings and verifies `de_DE` persistence and German formatter output. Remaining #215 verification is representative project field rendering and the no-override OS-locale path.

The #228 capacity audit also sized `TransformList`'s null-view authorization list to its strict one-item maximum and the single-project report adapter list to its exact size. `TransformListTest` passed and `:microproject_ui:compileJava --console=plain` passed.
| New: legacy `.pod` conversion guidance conflicts with MPO default | Implemented locally | Deprecated-format recovery now recommends `xml or mpo`. Legacy `.pod` opening and explicit `.pod` Save As compatibility remain available. |

## New issue: remove `.pod` as a recommended conversion target

The application has `.mpo` as its default native file format, but legacy `.pod`
paths remain user-visible:

- `LocalFileImporter` displays `Message.ImportOldFormatError`, whose text tells
  users to convert deprecated files to XML or POD.
- The standalone open/save chooser exposes `ProjectLibre Open Project (*.pod)`.
- Save As for an existing `.pod` keeps the `.pod` extension.

The `.pod` reader should remain available for backward compatibility, but no
new user-facing recovery or conversion guidance should recommend `.pod`.
The deprecated-format message now recommends `.mpo` (and XML where
appropriate). The explicit legacy `.pod` chooser filter and `.pod`-preserving
Save As behavior remain as opt-in legacy compatibility.

Acceptance criteria:

1. Deprecated-format recovery guidance no longer recommends saving to `.pod`.
2. New/unspecified native saves continue to default to `.mpo`.
3. Existing `.pod` files remain openable and can still be explicitly preserved
   or migrated according to the final compatibility policy.
4. Tests cover the message/policy and the Save As behavior for both new `.mpo`
   and existing `.pod` projects.

## Verification performed

The following commands completed successfully on the current worktree:

```text
.\gradlew.bat :microproject_core:test :microproject_exchange:test :microproject_ui:test :microproject_bootstrap:test --console=plain
```

These checks include focused Robot-based GUI acceptance runs against the clean
installed distribution for the entries marked “Focused GUI verified”. A real
Microsoft Project comparison and the real file-dialog multi-project open flow
remain unavailable/pending and are tracked by issues #430 and #395; those
scenarios must not be described as complete until that environment is provided.

最新の節目確認（commit `6cbb74127`）では Pages Run `33315015452` が
success、Release Run `33315015982` は pending（GitHub Actions のリリース処理待ち）
で、Release の完了結果は未確定である。

#228 では、プロジェクト追加時に件数が既知のリソース／タスク索引 Map を事前容量で
生成する改善を追加した。`ResourcePoolIdentityTest`（BUILD SUCCESSFUL、3秒）と
`:microproject_core:test` 全体（BUILD SUCCESSFUL、13秒）で挙動不変を確認した。

さらに `PercentWorkCompleteService` の収集済み葉タスク数／子ノード数が既知の一時
リストを事前確保した。`NormalTaskPercentCompleteTest`（BUILD SUCCESSFUL、4秒）と
`:microproject_core:test` 全体（BUILD SUCCESSFUL、12秒）で回帰がないことを確認した。

`ProjectFactory.getCloseProjectsOnServerJob(Collection)` でも入力プロジェクト数を
事前容量に反映した。`ProjectFactoryClosingTest`（BUILD SUCCESSFUL、4秒）と
`:microproject_core:test` 全体（BUILD SUCCESSFUL、12秒）で回帰がないことを確認した。

階層操作の一時リストでも、入力ノード数が取得できる `MutableNodeHierarchy` の子孫
収集と `DefaultNodeModel` の移動候補を事前確保した。`DefaultNodeModelTest`
（BUILD SUCCESSFUL、5秒）と `:microproject_core:test` 全体（BUILD SUCCESSFUL、14秒）
で回帰がないことを確認した。

協調ログの `OperationLog` でも、入力操作数／JSON配列長が既知のMap・一時リストを
事前確保した。`OperationLogTest`（BUILD SUCCESSFUL、3秒）と
`:microproject_core:test` 全体（BUILD SUCCESSFUL、13秒）で回帰がないことを確認した。

さらに、因果関係の親ID、競合ID、適用済みIDを保持するSetも、対応する操作数または
JSON配列長を上限として事前容量を設定した。祖先キャッシュは競合候補が疎な場合に
過剰確保となるため、既定容量のままとした。`OperationLogTest`とcore全体テストで
JSON/JSONLの読込、因果順序、競合メタデータ、適用済み世代の検証を確認した。

`WorkingCalendar` の recurring exception と dated work-week getter はコピー元リストの件数が
既知なので、その件数で戻り値リストを事前確保した。`CalendarRecurrenceTest` の focused test
と `:microproject_core:test` 全体で回帰がないことを確認した。

外部プロジェクト差分適用の `ProjectMergeService` でも、外部タスク件数が既知の変更
ノード通知リストを事前確保した。`:microproject_core:test` 全体（BUILD SUCCESSFUL、
15秒）で回帰がないことを確認した。

さらに `DefaultNodeModel.RemovalSnapshot` のルート件数既知のエントリ一覧と、
`ProjectFactory` の終了コールバック通知一覧を入力件数で事前確保した。
focusedテスト（BUILD SUCCESSFUL、4秒）と `:microproject_core:test` 全体（BUILD
SUCCESSFUL、13秒）で回帰がないことを確認した。

`MutableNodeHierarchy` の削除通知リスト（削除ルート数）と移動通知リスト（最低1件）
も事前容量を設定した。`:microproject_core:test` 全体（BUILD SUCCESSFUL、14秒）で
回帰がないことを確認した。

`ScheduleBackupEdit` は入力が単一ScheduleでもCollectionでも、正規化後の件数が
既知になった時点でバックアップMapを事前確保するようにした。Collection入力を使う
undoテストとcore全体テストが成功した。

`CalendarRecurrence.occurrenceDates()` は `AFTER_OCCURRENCES` の場合、検証済みの
発生回数を結果件数の上限として初期容量に使う。`BY_DATE` は生成件数が事前に
確定しないため従来どおりとし、`CalendarRecurrenceTest` とcore全体テストで
日次・週次・月次・年次の既存日付結果を確認した。

依存関係の切断時に作る incident snapshot も、先行リンク数と後続リンク数の合計で
初期容量を確保するようにした。順序と重複を保ったまま、`DependencyServiceTest` と
`:microproject_core:test` 全体が成功した。

サブプロジェクトのattach/detachで作るルートノード一覧は、Swingツリーの
`getChildCount()`を事前容量に使用した。`Node.getChildren()`は遅延未初期化時にnull
となるケースをテストで確認したため、そのAPIには依存せず、
`DefaultSubprojectHandlerTest` とcore全体テストで移動動作を再確認した。

`ProjectFactory.restoreLinkedLocalSubprojects`の参照一覧はmasterのタスク件数を上限として
事前確保し、`AssignmentEntry.setAssignmentsFromTaskList`は初回一致時に入力タスク数を
上限に確保する。割当なしの場合はnullのままにして従来の未割当状態を保つ。
`LocalSessionSubprojectTest`、`DefaultSubprojectHandlerTest`、`AssignmentEntryTest`と
core全体テストが成功した。

`ResourcePool.setLocalParent`の単一ノード移動一覧も容量1で生成し、タスク側の同じ
処理と揃えた。`ResourcePoolIdentityTest`の親変更確認とcore全体テストが成功した。

`ResourcePool.userResources()`は全リソースを走査するため、結果リスト容量に全件数を
上限として指定した。ユーザーアカウントの有無による抽出と順序を
`ResourcePoolIdentityTest`で確認した。

POD `Serializer.saveTasks` の外部 predecessor 用索引 Map はプロジェクトの task list 件数が
上限となるため、その件数に基づく初期容量を設定した。POD round-trip テスト
（`PodRoundTripTest`）が成功し、保存・再読込を確認した。

同じ保存経路でタスクの割当出力リストも現在割当数を初期容量に使うようにした。
過去 baseline にだけ残る割当はその後追加されるため、これは見積りであり上限ではない。
`PodRoundTripTest` が成功した。

`ResourceLevelingService.Plan` の分割タスク詳細バックアップ用
`IdentityHashMap` は、生成時に分割候補数を見積り容量として使うようにした。
実際にバックアップされる異なるタスク数は分割候補数以下である。
`:microproject_core:test` と `ResourceLevelingServiceTest` の既存 apply/revert 経路で
回帰がないことを確認した。

MPXJ import の時間分布Mapは、解析済み`ProjectFile.getResourceAssignments().size()`を
容量見積りに使うようにした。MSPDIとMPXの分岐で同じ空Mapを作っていた初期化も
一本化し、`MpxImportState`の既存容量計算を再利用する。Microsoft importer、tracking
import、XLSX fallbackの各focused testが成功した。

ChartLegendで選択プロジェクトをoutline taskへ展開する一時リストは、選択プロジェクト
ごとのtask list件数合計を上限に事前確保する。outline反復は全task listの部分集合であり、
対象件数と順序は変えていない。`ChartInfoWorkspaceTest`とUI compilationが成功した。

`DocumentFrame.doDefineCodeDialog`の選択タスク一覧も、選択implementation件数で
事前確保した。Task以外を除外する条件は維持し、UI compilationが成功した。

`PersonalContour`のduration切出し、bucket挿入、shift/extend、packed contour生成に
使う一時ArrayListは、入力bucket配列長（分割・挿入を含む場合は最大追加数込み）を
容量に使う。既存のbucket順序・切出し計算を変えず、`PersonalContourTest`と
`AssignmentContourBehaviorTest`が成功した。
`TimesheetEntryPane` now pre-sizes the resolved resource list from the unique-resource set and the assignment list from the summed per-resource association counts. Non-assignment filtering, encounter order, and the selected-resource behavior are unchanged. `:microproject_ui:compileJava --console=plain` passed.
`WorkingHours.intersectWith` now reserves at most the smaller input interval-array length, a strict upper bound because each produced intersection advances at least one input index. The existing intersection boundary/order assertions in `WorkingHoursTest` passed.
Preferences now expose the existing auto-recovery interval from the application Preferences route. Apply persists a value from 1 to 1440 minutes through `AutoRecoveryManager`, updates the active timer delay, and restarts its countdown only when recovery is enabled; cancel leaves the setting untouched. The existing dialog APIs remain source-compatible and only the application route supplies the recovery control. `AutoRecoveryManagerConcurrencyTest` covers clamping and reload; `PreferencesDialogGuiAcceptanceTest` uses Robot to enter 12 and physically Apply, and verifies dialog text bounds / usable-screen bounds. `:microproject_ui:test --tests com.microproject.pm.graphic.frames.AutoRecoveryManagerConcurrencyTest` and focused GUI acceptance passed at Japanese default scale and English 100%.
Verification note: the full `:microproject_ui:test --console=plain` run completed 927 tests with one failure and seven skips: the unrelated existing `ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored()` assertion. The same `ScaledScrollPaneTest` class passed when rerun alone; the failure is recorded rather than attributed to the preference change.
`AssignmentFieldClosureCollection` now gives its single-functor constructor an exact capacity of one; the collection constructor already sizes from its supplied collection. The existing closure behavior, consumer-only support, ordering, and cost aggregation tests passed in `AssignmentFieldClosureCollectionTest`.

For #595, `DefaultNodeModel.ImplComparator` now declares `Comparator<Object>` while preserving its OpenProj-derived identity comparison. A focused regression distinguishes identity from `equals`; `Portfolio.ImplComparator` remains separate because its `equals` behavior is different. Full core tests and application compilation passed.
