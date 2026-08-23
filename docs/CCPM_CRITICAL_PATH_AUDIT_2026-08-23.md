# CCPM / クリティカルパス検証記録（2026-08-23）

## 対象

- `samples/CCPM path comparison English.mpo`
- `samples/CCPM path comparison 日本語.mpo`
- 通常のクリティカルパス計算、依存関係、リソース制約によるクリティカルチェーン、MPO 保存・再読込、ネットワーク図、バッファチャート

## 検証内容

| 項目 | 結果 |
|---|---|
| POD → MPO 変換 | 成功 |
| 日英サンプルの MPO 再読込 | 成功 |
| タスク達成率 0/25/50/75/100% の混在 | 成功。プロジェクト達成率は 0% と 100% の間 |
| 依存関係を含むクリティカルパス計算 | 既存の core テスト一式で成功 |
| クリティカルスラック閾値、完了タスク、固定制約、期限 | 既存の互換性テストで成功 |
| FS/SS/FF/SF と遅延、期間変更、カレンダー境界 | 既存のスケジュールテストで成功 |
| リソース競合による追加エッジ | CCPM サービスのテストで成功 |
| CCPM ベースライン保存後の再解析 | 日英 MPO の回帰テストで成功 |
| ネットワーク図・バッファチャートの headless 描画 | UI 描画テストで成功 |

## サンプルに含めた主要パス

11 タスクの小規模な計画です。依存関係はすべて Finish-to-Start で、次の経路を比較できます。

- 主経路: Requirements → Architecture → Implementation → System Test → Release
- 準主経路: Requirements → Operations Preparation → Environment Setup → Integration Test → Release
- 非クリティカル経路: Requirements → User Guide → Training Preparation → Release
- 短いレビュー経路: Architecture → Security Review → Release

Implementation / Integration Test、System Test / Integration Test、User Guide / Training Preparation に担当リソースを割り当て、CCPM 適用時にはリソース制約も Network に現れる構成です。達成率は説明用に 0/25/50/75% を中心に設定し、完了扱いが主経路を消さないよう Release は未完了にしています。

## 修正した不具合

CCPM ネットワーク図の preferred size が固定 760×420 だったため、長いチェーンや複数のフィーディングバッファが JScrollPane の外側で切れていました。解析結果に応じてスクロール可能領域を計算するよう修正しました。

## Issue 判定

実デスクトップで `CCPM sample 日本語.mpo` を開いた結果、先行関係と開始日の不整合を確認したため、下記のIssueとして記録します。自動テストでは依存関係の存在自体は検証していますが、依存関係に対する再スケジュール結果までは検証していません。

### Issue: CCPMサンプルでFS先行タスクの終了前に後続タスクが開始される

- **対象**: `samples/CCPM sample 日本語.mpo`
- **再現条件**: CCPMサンプルを開き、タスク表とガントチャートを表示する。
- **期待結果**: FS（Finish-to-Start）の先行関係では、後続タスクの開始日は先行タスクの終了日以降になる。
- **実際の結果**:
  - 工程3 → 工程4: 工程3終了 `2026-06-08` に対し、工程4開始 `2026-06-02`
  - 工程5 → 工程6: 工程5終了 `2026-06-15` に対し、工程6開始 `2026-06-05`
  - 工程6 → 工程7: 工程6終了 `2026-06-12` に対し、工程7開始 `2026-06-08`
- **補足**: MPO内部の依存関係は該当箇所が `Type=1`（FS）、遅延0として保存されている。工程4 → 工程5は工程4終了日と工程5開始日が同日で、境界上は整合する。
- **影響**: 実績開始済みタスクでは、タスク表の開始日が先行タスクの終了日より前に見える。
- **仕様確認**: 対象タスクには `ActualStart` と進捗が設定されている。Microsoft Projectでは、実績開始日を入力したタスクのスケジュールは実績開始日を基準に再計算され、実績開始後に先行関係だけで開始日を後ろへ移動しない。このため、今回の表示はMS Project仕様に反する不具合ではない。
- **対策判定**: 依存関係で実績開始日を後ろへ移動する修正は、実績履歴を破壊しMS Project非準拠となるため実施しない。必要であれば別Issueとして「依存関係違反の警告表示」を検討する。

### Issue: MPO読込後にタスク達成率が割当進捗で上書きされる

- **対象**: `samples/CCPM sample 日本語.mpo`
- **再現条件**: MPOを開き、タスク5の「達成率」と「作業時間の達成率」を確認する。
- **原因**: 読込時にタスクの `PercentComplete` を設定した後、割当の `PercentWorkComplete` を追加すると、モデルがタスク達成率を割当進捗から再集計していた。
- **対策**: タスク達成率とタスク作業時間達成率を、割当進捗とは独立した外部読込値として保持する。通常のタスク／割当編集時は外部値を解除し、通常の再集計へ戻す。
- **回帰確認**: 工程5のタスク達成率・作業時間達成率がともにMPO記録値の100%になるテストを追加。

### Issue: ガントチャートの塗り領域・バー種別がMS Project仕様とテーマ実装で分離されていない

- **対象**: ガントチャートの通常タスク、進捗、要約、マイルストーン、期限、基準計画、クリティカルタスク、依存関係。
- **仕様確認**: MS Projectでは、通常タスクは予定期間のバー、進捗はバー内の完了領域、要約タスクは要約バー、マイルストーンは菱形、期限は矢印、基準計画は別バー、クリティカルタスクはクリティカル色、依存関係はリンクとして表示される。期限はスケジュールを拘束せず、期限超過の指標として表示される。参考: [Deadline field](https://support.microsoft.com/en-US/project/deadline-task-field)、[critical path](https://support.microsoft.com/en-US/project/manage-your-project-s-critical-path)、[milestone](https://support.microsoft.com/en-US/project/add-a-milestone)、[baseline](https://support.microsoft.com/en-us/project/create-or-update-a-baseline-or-an-interim-plan-in-project-desktop)
- **確認結果**: バー種別・塗り領域・進捗オーバーレイ・基準計画・期限矢印・クリティカル色・依存関係リンクは個別の描画経路に分離されており、形状と表示領域はMS Projectの意味に準拠している。色値だけは要求どおりMonday.comテーマ色を使用している。
- **原因**: 要約バーの背景色生成だけが `GanttRenderer` にMondayテーマを直接参照しており、パレット切替時に選択中のテーマと一致しなかった。
- **対策**: 要約バー背景色と完了領域色を `GanttColorPalette` の責務へ移し、MondayテーマとClassic MS Projectテーマがそれぞれの色体系で描画されるよう修正した。
- **回帰確認**: `GanttRendererProgressTest`、`GanttCriticalTaskColorTest`、`MondayGanttThemeTest`、`ClassicMSProjectPaletteTest` を実行し、全件成功。

GitHub Issueへの外部登録は、この検証記録とは別に行う必要があります。実デスクトップでの表示確認は、`clean build installDist` 後に両サンプルを開き、タスク表・ガント・CCPM Network・Buffer Chart を順に確認する必要があります。

## 実行した検証

```text
.\gradlew.bat :micrproject_core:test --console=plain
.\gradlew.bat :micrproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.checkedInEnglishAndJapaneseCcpmSamplesLoadForVisualization" --console=plain
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.pm.graphic.views.DockableProjectToolViewTest" --console=plain
```
