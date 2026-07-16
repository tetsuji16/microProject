# タスク表・ガントチャート重点テスト報告（2026-07-16）

## 判定方針

- タスク表の入力、貼り付け、階層、依存関係、Undo と、ガントの描画、進捗、移動・分割、表との同期を対象とする。
- 異常系では「例外にならない」だけでなく、部分更新、偽の成功、不要な Undo、古い描画キャッシュが残らないことを確認する。
- `--rerun-tasks` で再コンパイルから実行し、古いテスト成果物による見かけ上の成功を除外する。

## テストケースと評価結果

| ID | 対象・操作 | 期待結果 | 自動テスト | 結果 |
|---|---|---|---|---|
| TT-01 | 空行へのタスク追加中にセル編集を確定 | 入力値を失わず新規行を作成 | `CommonSpreadSheetAddNodeCommitTest` | PASS |
| TT-02 | 期間セルへ通常値・空値・不正値を入力 | 正常値だけ反映し、不正値で部分更新しない | `CommonSpreadSheetDurationEditingTest` | PASS |
| TT-03 | プロジェクト集約行の開始・期間を編集 | 子タスクのロールアップを壊さず手動 envelope に保持 | `CommonSpreadSheetSummaryScheduleEditingTest`, `ProjectScheduleBehaviorTest` | PASS |
| TT-04 | predecessor/successor の type・lag 編集 | 列位置ではなく Field ID で正しい依存を更新 | `SpreadSheetModelDependencyFieldRoutingTest` | PASS |
| TT-05 | indent/outdent と左右キーによる階層移動 | 親子関係と選択行が整合し、空行順序も保持 | `SpreadSheetHierarchyNavigationTest`, `DefaultNodeModelTest` | PASS |
| TT-06 | 列追加・削除・移動後の Undo/Redo | 列構成を正確に往復し、Undo ライフサイクルを壊さない | `SpreadSheetColumnUndoLifecycleTest` | PASS |
| TT-07 | 無効値を含む複数セル貼り付け | 全セルを事前検証し、1セルも部分適用しない | `NodeListTransferablePasteFailureTest` | PASS |
| TT-08 | read-only 親への構造貼り付け | 貼り付けを拒否し、成功扱いにしない | `NodeListTransferablePasteFailureTest` | PASS |
| TT-09 | node flavor 付き clipboard の「値として貼り付け」 | 構造貼り付けへ誤ルーティングせず文字列値だけ貼る | `NodeListTransferablePasteFailureTest` | PASS |
| TT-10 | IME、日付入力、Backspace、選択範囲付き入力 | 編集開始時の文字消失や誤クリアがない | `CommonSpreadSheetImeStartTest`, `CommonSpreadSheetDateTypingTest`, `CommonSpreadSheetBackspaceKeyTest` | PASS |
| GT-01 | 進捗率が負値、1超過、NaN のバー描画 | 描画比率を 0..1 に正規化し、座標を破綻させない | `GanttRendererProgressTest` | PASS |
| GT-02 | read-only schedule の split | データを変更せず失敗を返し、Undo を積まない | `ScheduleServiceSplitTest` | PASS |
| GT-03 | 同一値への constraint/interval 更新 | no-op と判定し、不要な Undo やイベントを作らない | `ScheduleServiceConstraintTest`, `ProjectScheduleBehaviorTest` | PASS |
| GT-04 | タスク表とガントの行・スタイル同期 | 対応するタスクとバー設定を選び、表示ずれを起こさない | `TaskGanttSyncSupportTest` | PASS |
| GT-05 | バー・リンクの選択ヒット領域 | 境界座標でも正しい対象を返し、範囲外を選択しない | `GanttSelectionGeometrySupportTest` | PASS |
| GT-06 | annotation/link/calendar/grid 別のスタイル選択 | 要求カテゴリだけ評価し、カテゴリ変更を即時反映 | `BarStylesTest` | PASS |
| GT-07 | 公開スタイル一覧へ追加・削除後に再描画 | キャッシュを破棄し、新規スタイルを表示、削除済みスタイルを非表示 | `BarStylesTest#mutatingTheExposedStyleListInvalidatesTheIndex` | **BUG検出→修正後PASS** |
| IO-01 | 実サンプル POD の保存・再読込 | タスク名、親子、開始、終了、期間、依存を完全保持 | `PodRoundTripTest`（2サンプル） | **BUG検出→修正後PASS** |

## 修正した不具合

1. `BarStyles` のカテゴリ別描画キャッシュが、`getRows()` からの追加・削除・置換・並べ替えで無効化されなかった。変更を監視するリストへ置き換え、全ミューテーションでキャッシュを破棄するよう修正した。
2. POD 再読込後に `Project` / `Assignment` の facade が古い外側インスタンスを参照し得た。facade を transient 化し、デシリアライズと clone 時に再生成するよう修正した。
3. 安全な POD 読込フィルタが、実データで必要な `ZoneInfo` と lock 実装を拒否していた。許可範囲を必要最小限で追加した。
4. POD メタデータ名が null のとき、復元済みタスク名を null で上書きしていた。値がある場合だけ上書きするよう修正した。
5. assignment シリアライズ失敗を内部で握り潰し、部分的なタスクデータを成功扱いし得た。`IOException` を呼び出し元へ伝播するよう修正した。
6. ガントの式ベーススタイル評価が共有引数配列を使い、並行評価で別タスクの値が混ざり得た。コンパイル済み `MethodHandle` と呼び出しごとの引数へ変更した。

## 実行結果

- `:projectlibre_core:test`: 189 tests, 0 failures, 0 errors, 0 skipped
- `:projectlibre_ui:test`: 398 tests, 0 failures, 0 errors, 0 skipped
- `:projectlibre_exchange:test`: 34 tests, 0 failures, 0 errors, 0 skipped（実サンプル2件の POD round-trip を含む）
- 全モジュール `build`: 成功
