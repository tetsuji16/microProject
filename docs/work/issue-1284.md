# Issue #1284 — Project Summary Task 1/2: ビュー先頭のプロジェクトの概要行

State: IN PROGRESS. Intake 2026-11-23; baseline `c31d465f3` (isolated clean worktree).

Original contract: Microsoft Project の「プロジェクトの概要タスク」をビュー先頭に表示する。タスクモデルは変更しない。表示用の合成ノードとして実装し、名前のみ編集可能。リボンのチェックボックスと保存は #1285 で別途実装。

## Goal and scope

ビューの先頭にプロジェクトの概要行を表示し、プロジェクト全体のサマリー値（開始、終了、期間、作業時間、コスト、達成率など）を表示、プロジェクト名のみ編集可能にする。Ganttバーも描画する。他のタスク操作（削除、インデント、リンク等）はこの行では無効化する。

Out of scope: リボンのチェックボックス/ビューごとの保存 (#1285)、Network Diagram/Calendar での表示、MSPDI/MPP のタスク0 読み書き変更、POD 形式の変更。
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC-1 | フラグ off のとき、表示されるノードの並びが今と同じ（単体テストあり） | pass | `NodeCacheTransformerTest.projectSummaryTaskNotShownWhenFlagOff` |
| AC-2 | フラグ on のとき、先頭にプロジェクトの概要行が 1 行あり、終了日がプロジェクトの終了日と一致する（単体テストあり） | pass | `NodeCacheTransformerTest.projectSummaryTaskShownWhenFlagOn`, `NodeCacheTransformerTest.projectSummaryTaskEndDateMatchesProjectEnd` |
| AC-3 | 概要行を選んでいるとき、タスクの削除・インデント・リンクが無効（単体テストあり） | open | pending |
| AC-4 | 概要行の名前を変えると、プロジェクト名が変わり、1 回の Undo で戻る（単体テストあり） | open | pending |
| AC-5 | `Project` と `Task` のクラス階層、POD の読み書きに変更が無い | pass | No changes to Project/Task hierarchy or POD serialization |
| AC-6 | Ganttバーがサマリータスクと同じ形で、プロジェクトの開始から終了まで描画される | open | pending |
| AC-7 | `NodeCacheTransformerTest` が通る | pass | All 4 tests in `NodeCacheTransformerTest` pass |
| AC-8 | `python scripts/test_source_inventory.py --check` が通る | pass | Verified |
| AC-9 | `git diff --check` が通る | pass | Verified |
| AC-10 | `:microproject_ui:test --console=plain` が通る | pass | Verified |
| AC-11 | `build` と `GUI acceptance` が通る | open | Pending CI |

## Current slice

**Next slice:** 概要行の値表示（`Field` 経由）、名前編集と Undo、コマンド無効化、Gantt描画、残りのテストを実装する。
**Done when:** AC-3, AC-4, AC-6 のテストが通る。
**Then:** 統合テスト、GUI acceptance へ進む。

## Remaining blockers

- none

## Latest evidence

- Tests/build: `:microproject_ui:test --console=plain`, `:microproject_core:test`, `:microproject_exchange:test` すべて成功
- Physical GUI/manual: 未実施
- CI: `build` と `GUI acceptance` は PR 作成後に実行