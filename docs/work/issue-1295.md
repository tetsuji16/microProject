# Issue #1295 Work Packet: Team Planner の資源名の列を固定し、幅を変えられるようにする

## 取り込み時の契約

- Issue: https://github.com/tetsuji16/microProject/issues/1295
- Base: `origin/master` `11a3034e9`
- ブランチ: `issue/1295-team-planner-frozen-column`

## 完了条件と証跡

| # | 完了条件 | 状態 | 証跡 |
|---|---|---|---|
| 1 | 横にスクロールしても資源名の列が左に残る | 実装済み | `TeamPlannerRowHeader` を `JScrollPane.setRowHeaderView` に置いた。`TeamPlannerDialogGuiAcceptanceTest.resourceColumnStaysOnTheLeftWhileTheTimelineScrolls` |
| 2 | 列の幅を変えられ、80 px より狭くならない | 実装済み | 80 px 下限、列とタイムラインの合計表示幅の半分を上限とする。GUIテストは物理 Robot ドラッグを確認 |
| 3 | バーのドラッグ・選択・「今日」が変更前と同じ | 実装済み | `dateToX` から旧ラベル幅を除去。バー開始位置テストは日付から算出した期待位置と比較し、ドラッグ日数テストも維持 |
| 4 | 手順 5 の試験 | 実装済み | `TeamPlannerCanvasTest`（位置・ドラッグ）、GUI（横スクロール・物理列幅ドラッグ） |
| 5 | `build` と `GUI acceptance` | CI 必須 | ローカル GUI は他ウィンドウの重なりで環境競合。幅変更 Robot ケースは pass、横スクロールケースは環境競合で完了せず |

## 変更

- `TeamPlannerCanvas`: 日付の帯と資源名を描かない。行の基準を `HEADER_HEIGHT` から 0 にし、`dateToX` の `LABEL_WIDTH` 加算を除いた。
- `TeamPlannerRowHeader`（新規）: 資源名だけを描く行ヘッダー。右端のドラッグで幅を変える（最小 80 px、最大は資源列とタイムラインを合わせた表示幅の半分）。
- `TeamPlannerDateHeader`（新規）: 日付の帯を列ヘッダーへ移し、縦スクロールで消えないようにした。
- 左上の角（`UPPER_LEFT_CORNER`）を列幅に合わせて更新する。

## 検証

- `.\gradlew.bat :microproject_ui:test --tests "*TeamPlannerCanvasTest" --console=plain` → BUILD SUCCESSFUL
- `.\gradlew.bat :microproject_ui:guiTest --tests "*TeamPlannerDialogGuiAcceptanceTest" --console=plain` → 幅変更 Robot ケースは PASS。横スクロールケースは `GUI_ENVIRONMENT_CONTENDED`。同実行で既存の共有プール/Enter ケースも失敗したため、実行全体は成功扱いにしない。Windows GUI acceptance CI で再確認する。
- `git diff --check` → pass

## 次のスライス

PR description のコマンド契約と検証欄も更新し、必須 `build` と `GUI acceptance` が新しい head で通ることを確認する（#1296 は時間の目盛りで別作業）。
