# Issue #1284 — Project Summary Task 1/2: ビュー先頭のプロジェクトの概要行

State: IN PROGRESS. Intake 2026-11-23; baseline `c31d465f3` (isolated clean worktree).

Original contract: Microsoft Project の「プロジェクトの概要タスク」をビュー先頭に表示する。タスクモデルは変更しない。表示用の合成ノードとして実装し、名前のみ編集可能。リボンのチェックボックスと保存は #1285 で別途実装。

Microsoft evidence: https://support.microsoft.com/en-us/project/show-the-project-summary-task-in-project-desktop states that the project can be shown on a single row with its own summary taskbar. The page says it is hidden by default and is toggled from the Format tab. Keeping the whole-project row first during task filtering/sorting is a document-derived decision: it represents the whole project rather than a task subject to the active task filter or order.

## Goal and scope

ビューの先頭にプロジェクトの概要行を表示し、プロジェクト全体のサマリー値（開始、終了、期間、作業時間、コスト、達成率など）を表示、プロジェクト名のみ編集可能にする。Ganttバーも描画する。他のタスク操作（削除、インデント、リンク等）はこの行では無効化する。

Out of scope: リボンのチェックボックス/ビューごとの保存 (#1285)、Network Diagram/Calendar での表示、MSPDI/MPP のタスク0 読み書き変更、POD 形式の変更。
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC-1 | フラグ off のとき、表示されるノードの並びが今と同じ（単体テストあり） | pass | `NodeCacheTransformerTest.projectSummaryTaskNotShownWhenFlagOff` |
| AC-2 | フラグ on のとき、先頭にプロジェクトの概要行が 1 行あり、終了日がプロジェクトの終了日と一致する（単体テストあり） | pass | `NodeCacheTransformerTest.projectSummaryTaskShownWhenFlagOn`, `NodeCacheTransformerTest.projectSummaryTaskEndDateMatchesProjectEnd`; filter/sort also retain row first |
| AC-3 | 概要行を選んでいるとき、タスクの削除・インデント・リンクが無効（単体テストあり） | pass | `ProjectSummaryRowSpreadsheetTest.projectSummaryRowCannotBeDeletedOrCutFromTaskSheet`; `RibbonButtonBehaviorTest.projectSummarySelectionDisablesAndRejectsLinkDeleteAndIndent` |
| AC-4 | 概要行の名前を変えると、プロジェクト名が変わり、1 回の Undo で戻る（単体テストあり） | pass | `ProjectSummaryRowSpreadsheetTest.onlyProjectNameIsEditableAndOneUndoRestoresIt` verifies dirty state, one-step Undo/Redo, MPO save/reload |
| AC-5 | `Project` と `Task` のクラス階層、POD の読み書きに変更が無い | pass | No changes to Project/Task hierarchy or POD serialization |
| AC-6 | Ganttバーがサマリータスクと同じ形で、プロジェクトの開始から終了まで描画される | pass | `RibbonButtonBehaviorTest.projectSummaryRowUsesSummaryGanttBarFromProjectStartToFinish` calls `GanttUI.updateShape` and asserts geometry and intervals |
| AC-7 | `NodeCacheTransformerTest` が通る | pass | All 4 tests in `NodeCacheTransformerTest` pass |
| AC-8 | `python scripts/test_source_inventory.py --check` が通る | pass | Verified |
| AC-9 | `git diff --check` が通る | pass | Verified |
| AC-10 | `:microproject_ui:test --console=plain` が通る | pass | Verified |
| AC-11 | `build` と `GUI acceptance` が通る | open | Pending CI |

## Current slice

**Latest slice:** Added the explicit `ProjectSummaryRow` node class, so the synthetic row is distinguishable from other project objects. Summary row insertion runs after filter/sort/group transforms and remains at index 0; existing top-level task rows are one display level below it. Only the name is editable; its Project.setName edit marks the project dirty and supports Undo/Redo and MPO persistence. Delete/Cut/Indent/Outdent and task-link commands reject the summary row selection. The Gantt UI renderer now has a headless geometry integration test.
**Next slice:** Complete full verification and GUI acceptance; verify all CI checks on the final pushed head.
**Done when:** All listed acceptance checks and required CI pass.

## Task entry column mapping

The current default task entry sheet fields were exercised against the synthetic project row. Values use the existing `Field.getValue` path, which returns `null` for fields that do not apply to `Project`; these cells are blank. Every non-name field is read-only.

| Field | Project row behavior | Evidence |
|---|---|---|
| `Field.name` | `Project.getName()`, editable | `ProjectSummaryRowSpreadsheetTest` |
| `Field.duration` | Project duration | `ProjectSummaryRowSpreadsheetTest` |
| `Field.start` | Project start | `ProjectSummaryRowSpreadsheetTest` |
| `Field.finish` | Project finish | `ProjectSummaryRowSpreadsheetTest` |
| `Field.id`, `Field.indicators`, `Field.percentWorkComplete` | Evaluated through standard field access; no exception | all-column evaluation in `ProjectSummaryRowSpreadsheetTest` |
| `Field.manuallyScheduled`, `Field.predecessors`, `Field.resourceNames` | Not applicable to `Project`, blank | `ProjectSummaryRowSpreadsheetTest` |

Other task columns remain governed by `Field.isApplicable(Project.class)` and the normal Project field interfaces; no project-only custom mapping was introduced.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `.\gradlew.bat :microproject_core:test --console=plain` and `.\gradlew.bat :microproject_ui:test --console=plain` passed on current slice. `python scripts/test_source_inventory.py --check` also passed. Focused classes were rerun during implementation.
- Tests/build: focused `RibbonButtonBehaviorTest` (64), `NodeCacheTransformerTest` (6), and `ProjectSummaryRowSpreadsheetTest` (2) pass. Previous full `:microproject_ui:test` and `:microproject_core:test` pass. `git diff --check` passes.
- Physical GUI/manual: 未実施
- CI on PR head `134bd7c5`: `GUI acceptance` passed; Windows/macOS build passed; Ubuntu build failed before compile because Maven Central returned HTTP 429 while resolving `commons-lang3:3.22.0` and 16 other artifacts. Retry after final head update.
