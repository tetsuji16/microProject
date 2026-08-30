# Open issue triage — 2026-08-23

This inventory was checked against the open GitHub issue list and the current
`master` worktree. An issue remains open on GitHub when implementation is
waiting for reporter confirmation, is intentionally tracked as an umbrella, or
still has follow-up work; the open state alone does not mean that its original
bug is still reproducible.

## Status

| Issue | Current status | Evidence / remaining work |
|---|---|---|
| [#366](https://github.com/tetsuji16/ProjectLibre/issues/366) | Partial | Project-scoped custom-report persistence, Blank/Chart/Table/Comparison templates, Chart defaults, and the chart cache lifecycle are covered. Histogram uses `computeHistogram` while Chart uses `computeValues`; remaining work is a dedicated Resource Graph UI for peak allocation/over-allocation and a richer editable report-chart settings surface. |
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
| [#245](https://github.com/tetsuji16/ProjectLibre/issues/245) | Partial migration | Some persisted int-constant types already use enum adapters. Remaining types need one-at-a-time conversion with `fromInt`/`toInt` compatibility tests; a bulk replacement is unsafe for serialized formats. |
| [#228](https://github.com/tetsuji16/ProjectLibre/issues/228) | Partial | Safe known-size capacity fixes include dependency/calendar paths and Resource Leveling's complete task scan. Remaining allocations need profiling and size-bound analysis before changing them; dynamic collections should not receive guessed capacities. |
| [#215](https://github.com/tetsuji16/ProjectLibre/issues/215) | Phase 1 complete | Preferences cover user name, font family/size, row-lines default, and update checking. Remaining candidates are autosave, default format/calendar, theme, date/currency/locale, and per-view settings. |
| [#204](https://github.com/tetsuji16/ProjectLibre/issues/204) | Implemented | Timeline and Team Planner are embedded through `DockableProjectToolView`, routed through `DocumentFrame` top-view activation, and no longer require the modal Team Planner dialog workflow. |
| [#179](https://github.com/tetsuji16/ProjectLibre/issues/179) | Focused GUI verified | `TaskDurationGuiAcceptanceTest` and `TaskTableGanttGridGuiAcceptanceTest` verify full-row highlight, table/chart synchronization, selection, and drag behavior in the installed distribution. The issue was closed with the verification evidence recorded in its latest comment. |
| [#152](https://github.com/tetsuji16/ProjectLibre/issues/152) | Excluded (clean-room) | This clean-room umbrella is intentionally outside this change set. |
| [#84](https://github.com/tetsuji16/ProjectLibre/issues/84) | Not implemented as a repository-wide cleanup | Dead/commented code removal needs an inventory with compiler/reference checks. Next unit is a report-only scan, then small deletions verified by the owning module tests. |
| [#63](https://github.com/tetsuji16/ProjectLibre/issues/63) | Focused GUI verified | `TaskFontStyleTest`, `TaskInformationDialogTest`, and `TaskInformationGuiAcceptanceTest` verify font customization behavior and persistence paths. The issue was closed after focused GUI verification. |
| [#45](https://github.com/tetsuji16/ProjectLibre/issues/45) | Focused GUI verified | `SpreadSheetMouseInteractionTest` and `RibbonButtonBehaviorTest` verify keyboard/drag task movement, insertion feedback, undo, and drag-and-drop preference behavior. The issue was closed with focused test evidence. |
| [#36](https://github.com/tetsuji16/ProjectLibre/issues/36) | Partial | Resource assignment spreadsheet and ribbon command routing were fixed. Remaining audit items are resource-sheet bindings, usage-view time-phasing, resource information layout, and assignment-pane usability. |
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
.\gradlew.bat :micrproject_core:test :micrproject_exchange:test :micrproject_ui:test :micrproject_bootstrap:test --console=plain
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
`:micrproject_core:test` 全体（BUILD SUCCESSFUL、13秒）で挙動不変を確認した。

さらに `PercentWorkCompleteService` の収集済み葉タスク数／子ノード数が既知の一時
リストを事前確保した。`NormalTaskPercentCompleteTest`（BUILD SUCCESSFUL、4秒）と
`:micrproject_core:test` 全体（BUILD SUCCESSFUL、12秒）で回帰がないことを確認した。

`ProjectFactory.getCloseProjectsOnServerJob(Collection)` でも入力プロジェクト数を
事前容量に反映した。`ProjectFactoryClosingTest`（BUILD SUCCESSFUL、4秒）と
`:micrproject_core:test` 全体（BUILD SUCCESSFUL、12秒）で回帰がないことを確認した。

階層操作の一時リストでも、入力ノード数が取得できる `MutableNodeHierarchy` の子孫
収集と `DefaultNodeModel` の移動候補を事前確保した。`DefaultNodeModelTest`
（BUILD SUCCESSFUL、5秒）と `:micrproject_core:test` 全体（BUILD SUCCESSFUL、14秒）
で回帰がないことを確認した。

協調ログの `OperationLog` でも、入力操作数／JSON配列長が既知のMap・一時リストを
事前確保した。`OperationLogTest`（BUILD SUCCESSFUL、3秒）と
`:micrproject_core:test` 全体（BUILD SUCCESSFUL、13秒）で回帰がないことを確認した。

外部プロジェクト差分適用の `ProjectMergeService` でも、外部タスク件数が既知の変更
ノード通知リストを事前確保した。`:micrproject_core:test` 全体（BUILD SUCCESSFUL、
15秒）で回帰がないことを確認した。
