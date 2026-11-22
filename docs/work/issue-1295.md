# Issue #1295 Work Packet: Team Planner の資源名の列を固定し、幅を変えられるようにする

## 取り込み時の契約

- Issue: https://github.com/tetsuji16/microProject/issues/1295
- Base: `origin/master` `11a3034e9`
- ブランチ: `issue/1295-team-planner-frozen-column`

## 完了条件と証跡

| # | 完了条件 | 状態 | 証跡 |
|---|---|---|---|
| 1 | 横にスクロールしても資源名の列が左に残る | 実装済み | `TeamPlannerRowHeader` を `JScrollPane.setRowHeaderView` に置いた。`TeamPlannerDialogGuiAcceptanceTest.resourceColumnStaysOnTheLeftWhileTheTimelineScrolls` |
| 2 | 列の幅を変えられ、80 px より狭くならない | 実装済み | `setLabelWidth` の下限 80・上限は表示域の半分。`resourceColumnWidthStaysWithinItsContract` |
| 3 | バーのドラッグ・選択・「今日」が変更前と同じ | 実装済み | `dateToX` から `LABEL_WIDTH` を除き、行の基準を 0 にした。`TeamPlannerCanvasTest` |
| 4 | 手順 5 の試験 | 実装済み | `TeamPlannerCanvasTest`（2 件）、GUI 2 件 |
| 5 | `build` と `GUI acceptance` | 確認中 | CI |

## 変更

- `TeamPlannerCanvas`: 日付の帯と資源名を描かない。行の基準を `HEADER_HEIGHT` から 0 にし、`dateToX` の `LABEL_WIDTH` 加算を除いた。
- `TeamPlannerRowHeader`（新規）: 資源名だけを描く行ヘッダー。右端のドラッグで幅を変える（最小 80 px、最大は表示域の半分）。
- `TeamPlannerDateHeader`（新規）: 日付の帯を列ヘッダーへ移し、縦スクロールで消えないようにした。
- 左上の角（`UPPER_LEFT_CORNER`）を列幅に合わせて更新する。

## 検証

- `.\gradlew.bat :microproject_ui:test --console=plain` → BUILD SUCCESSFUL
- `.\gradlew.bat :microproject_ui:guiTest --tests "*TeamPlanner*"` → ローカルのデスクトップが競合して実行できないため、Windows の GUI acceptance で確認する。

## 次のスライス

なし（#1296 は時間の目盛りで別作業）。
