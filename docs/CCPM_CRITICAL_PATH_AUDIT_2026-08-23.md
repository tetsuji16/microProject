# CCPM / クリティカルパス検証記録（2026-08-23）

## 対象

- `samples/CCPM sample English.mpo`
- `samples/CCPM sample 日本語.mpo`
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

## 修正した不具合

CCPM ネットワーク図の preferred size が固定 760×420 だったため、長いチェーンや複数のフィーディングバッファが JScrollPane の外側で切れていました。解析結果に応じてスクロール可能領域を計算するよう修正しました。

## Issue 判定

今回の自動検証ではクリティカルパス計算の新たな再現不具合は確認できませんでした。そのため、クリティカルパス計算について新規 GitHub Issue は作成していません。実デスクトップでの表示確認は、`clean build installDist` 後に両サンプルを開き、タスク表・ガント・CCPM Network・Buffer Chart を順に確認する必要があります。

## 実行した検証

```text
.\gradlew.bat :micrproject_core:test --console=plain
.\gradlew.bat :micrproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.checkedInEnglishAndJapaneseCcpmSamplesLoadForVisualization" --console=plain
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.pm.graphic.views.DockableProjectToolViewTest" --console=plain
```
