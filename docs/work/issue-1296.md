# Issue #1296 Work Packet: Team Planner の時間の目盛りを、表示タブのタイムスケールと拡大・縮小で変える

## 取り込み時の契約

- Issue: https://github.com/tetsuji16/microProject/issues/1296
- Base: `origin/master` `c916bbf05`
- ブランチ: `issue/1296-timescale-ribbon`

## 手順 1: 変更前の動き（master で確認）

| コマンド | Team Planner 表示中の変更前の動き |
|---|---|
| `RibbonTimescaleGallery` | `GraphicManager.showTimescaleChooser()` が最初に `getActiveGanttView()` を求め、Team Planner では null のため何も起きない。`ACTION_TIMESCALE` も `syncGanttViewRibbonState()` でガント表示中だけ有効なので、リボンは無効。 |
| `RibbonZoomIn` / `RibbonZoomOut` | `DocumentFrame.doZoomIn()/doZoomOut()` が `activeTopView`（Team Planner では `DockableProjectToolView`）へ委譲するが、その `zoomIn()/zoomOut()` は空実装で `canZoomIn()/canZoomOut()` は false。`setZoomButtons()` が両コマンドを無効にするため、押しても何も起きない。 |

目盛りは Team Planner 自身のツールバーの `JSlider`（8〜48 px/日）だけで変えていた。

## 完了条件と証跡

| # | 完了条件 | 状態 | 証跡 |
|---|---|---|---|
| 1 | 手順 1 の変更前の動きが PR 本文にある | 実装済み | 上の表。PR 本文にも転記 |
| 2 | 表示タブのタイムスケールで目盛りが変わる | 実装済み | `showTimescaleChooser()` が Team Planner 表示中は `TeamPlannerViewSettings.Timescale` の一覧を出し、`setTimescale` を呼ぶ。リボンのコンボボックス経路も `RibbonDropDownSource.currentValue/choices/select` が Team Planner の単位を返す |
| 3 | 拡大・縮小で目盛りが 1 段ずつ変わる | 実装済み | `DockableProjectToolView.zoomIn/zoomOut/canZoomIn/canZoomOut` が埋め込み Team Planner に委譲。`TeamPlannerCanvasTest.zoomMovesOneUnitAndStopsAtTheEnds` |
| 4 | 日付の見出しが 2 段になる | 実装済み | `TeamPlannerDateHeader.paintBand`。上段は選択単位の 1 つ上、下段は選択単位。`choosingATimeUnitChangesTheDayWidthAndTheHeaderBands` |
| 5 | ツールバーからスライダーが消える | 実装済み | `TeamPlannerDialogBox.buildUi()` から `JSlider` と `team.zoom` ラベルを削除し、未使用になった `team.zoom` の文言も両バンドルから削除 |
| 6 | 選んだ目盛りが保存され、開き直しても残る | 実装済み | `TeamPlannerViewSettings` の `timescale` プロパティ。`theChosenTimescaleSurvivesASaveAndRestore`、`theChosenTimescaleIsRestoredByTheSharedWorkspaceView` |
| 7 | Gantt など他の表示が変わらない | 実装済み | タイムスケールの選択肢と拡大・縮小は Team Planner 表示中だけ分岐。ガントの経路は未変更 |
| 8 | `build` と `GUI acceptance` | CI 必須 | ローカルの `:microproject_ui:test` と GUI テストで確認し、PR の必須チェックで確認する |

## 単位と 1 日あたりのピクセル数

| 単位 | px/日 |
|---|---|
| 時間 (HOURS) | 480 |
| 日 (DAYS) | 60 |
| 週 (WEEKS, 既定) | 20 |
| 月 (MONTHS) | 10 |
| 四半期 (QUARTERS) | 6 |
| 年 (YEARS) | 3 |

既定は変更前の `pixelsPerDay = 20` と同じ見た目になる週。

## 変更

- `TeamPlannerViewSettings`: `Timescale`（単位と px/日の並び、`finer`/`coarser`/`headerUpper`/`label`）と保存・復元を追加。
- `TeamPlannerDialogBox`: スライダーを削除。タイムラインの px/日を見出しの単位から決め、日付見出しを 2 段にし、縦線を選んだ単位の境界に描く。`timescaleStart`/`nextTimescaleStart`/`timescaleLabel` を追加（日付の切り下げは 1 つの `startOfDay` 補助に集約）。拡大・縮小の可否は `Timescale.hasFiner()`/`hasCoarser()` で単位の並びだけから決める。
- `DockableProjectToolView`: 埋め込み Team Planner の拡大・縮小と可否を実装。
- `GraphicManager`: Team Planner 表示中のタイムスケール選択（ダイアログ経路とリボンのコンボボックス経路の両方）と、`ACTION_TIMESCALE` の有効化。単位一覧は `TeamPlannerViewSettings.Timescale.labels()`／`at(int)` の 1 か所だけを参照し、両経路は `chooseTeamPlannerTimescale` で同じ適用処理を通る。
- 文言: `team.timescale*` を英語・日本語の usability バンドルに追加。
- `TimelineTeamPlannerRibbonGuiAcceptanceTest.teamPlannerTimescaleCommandsChangeItsOwnScaleOnly`: Team Planner 表示中のリボン拡大・縮小とタイムスケール（コンボボックス経路）で目盛りが 1 段ずつ変わること、その間 Gantt のタイムスケールが変わらないことを物理クリックで確かめる。

## 検証

- `.\\gradlew.bat :microproject_ui:test --tests "*TeamPlanner*" --console=plain` → BUILD SUCCESSFUL
- `.\\gradlew.bat :microproject_ui:compileGuiTestJava :microproject_ui:test --console=plain` → BUILD SUCCESSFUL
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
- `git diff --check` → 空
- `.\gradlew.bat :microproject_ui:guiTest --tests "*TimelineTeamPlannerRibbonGuiAcceptanceTest" --console=plain` → BUILD SUCCESSFUL（`timelineAndTeamPlannerFormatTabsActOnTheirViews` と `teamPlannerTimescaleCommandsChangeItsOwnScaleOnly` の 2 件とも PASS）
- `:microproject_ui:test --tests "*TeamPlanner*"` → `TeamPlannerViewSettingsTest` 7 件（新しい `bothTimescaleChoosersListTheUnitsInScaleOrder` を含む）と `TeamPlannerCanvasTest` とも失敗 0。
- `.\gradlew.bat :microproject_ui:guiTest --tests "*TeamPlannerDialogGuiAcceptanceTest" --console=plain` → `resourceColumn*` は PASS。`sharedPoolOverloadIsVisibleInTeamPlanner` と `robotEnterPressesTheDefaultCloseButton` は FAILED だが、変更を外した HEAD でも同じ 2 件が FAILED（`git stash` で確認）したため、この変更ではなく GUI 実行環境の既存の失敗。
- GUI acceptance の必須チェックは PR で確認する。

## 次のスライス

PR 本文に手順 1 の表と検証結果を載せ、必須 `build` と `GUI acceptance` が新しい head で通ることを確認する。
