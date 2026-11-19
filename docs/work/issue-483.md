# Issue #483 — FlatLaf 適用漏れの画面を共通テーマ経路へ統合する

State: In progress; このスライスでは実行時テーマ切り替えの色解決を修正し、ダークテーマのガント証跡を追加した。本文の受入条件のうち画面横断の視覚証跡は未完了で、issue は開いたままにする。
Issue: https://github.com/tetsuji16/microProject/issues/483
Contract frozen at: 2026-11-20, open issue body（2026-09-29 / 2026-10-26 / 2026-11-12 のコメントを含む）。
Baseline HEAD: `a09ad021f`（`origin/master`、2026-11-19）。

## Goal and scope

このスライスは、再起動なしのテーマ切り替え（#1111〜#1114 の経路）で、トークンから描画した面が別トークンの色へ化ける問題を扱う。

`ThemeManager` は切り替え前後の `UIManager` 値を RGB で対応付けて既存コンポーネントの色を貼り替える。しかし light テーマでは複数の面トークンが同じ白（`#ffffff` など）になるため、RGB だけでは
ビューポート、ダイアログ面、パネルを区別できない。結果として、白い面は「最初に一致した別トークン」のダーク値（例 `#2c2f34`）へ化けていた。これは切り替えのたびに起こり、どのトークンが勝つかは `HashMap` の走査順に依存していた。

対象:

- `FlatUiSupport.applyViewportSurface` / `applyDataSurface` / `applyPanelSurface` / `applyWorkspaceSurface` / `applyTableHeaderStyle` が描いた面は、使用したトークン名をクライアントプロパティへ記録し、切り替え後は RGB ではなくそのトークンから解決し直す。
- `ThemeManager` はウィンドウツリーの RGB 再マップ後に `FlatUiSupport.refreshThemeColors` を呼ぶ（ウィンドウ外のキャッシュ ポップアップ経路も同様）。
- ダークテーマのガント（タイムスケール、列ヘッダー、両ビューポート）を実行時切り替えで検証する GUI 受入テストを追加する。

Out of scope: ガントの業務色（バー、進捗、クリティカル パス）、印刷用紙の白、チャートの系列色は意味色として維持する。MSP の配色そのものの変更は行わない。
Blockers: なし。

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | テーマ切り替え後、トークンから描いた面が自分のトークン色になる | pass | `ThemeTokenSurfaceRefreshTest.tokenPaintedSurfacesResolveTheirOwnColourAfterASwitch` |
| A2 | light で同じ白になる 2 つの面トークンが、ダークで別の色へ解決される | pass | 同上（viewport `#202226` と panel `#25272b` を比較） |
| A3 | 実画面のガントで、タイムスケールとタスク表ヘッダー、両ビューポートが切り替えに追従する | pass | `DarkThemeGanttGuiAcceptanceTest.ganttTimeScaleAndTaskColumnsFollowARunTimeThemeSwitch`（ja / 100%） |
| A4 | 切り替えを light へ戻すと元のトークン色に戻る | pass | 同テストの復元アサーション |
| A5 | 画面横断の視覚証跡（CalendarView、Timeline、CustomFields、CustomReport、PageSetup/PrintPreview、collaboration、ダークのカレンダー表・列、ja 125%） | open | 既存 `FlatLafLegacyDialogRefreshGuiAcceptanceTest` は 5 ダイアログの UI 再適用のみ。collaboration、印刷、125% は未取得 |

## Current slice

**Current slice:** RGB 再マップの曖昧さを、トークン名の記録と再解決で解消した。あわせて、ダークテーマのガント証跡を GUI テストで固定した。
**Next slice:** 本文の画面ごとの証跡行列を埋める。少なくとも collaboration の警告・確認ダイアログ、PageSetup/PrintPreview、ja 125% のガントとカレンダー表。
**Done when:** 本文「受け入れ条件」の全画面で、表示後・実操作・再描画後の外観が ja 100% / 125% で取得され、意味色の例外がコードコメントとテストで説明されている。
**Then:** PR 本文を `Closes #483` へ切り替えて close する。

## Latest evidence

- 現象の再現: 修正前の `DarkThemeGanttGuiAcceptanceTest` は、ダーク切り替え後のガント ビューポートが `#2c2f34`（ダイアログ面のダーク値）になる一方、`FlatUiSupport.viewportBackground()` は `#202226` であることを示して失敗した。ライトでは両面とも `#ffffff` のため、RGB 再マップが区別できていなかった。
- 修正後: 同じテストで タイムスケール `#f2f2f2` → `#2b2e33`、タスク表ヘッダー `#f2f2f2` → `#2b2e33`、ガント／表ビューポート `#ffffff` → `#202226` を確認し、light へ戻すと `#ffffff` に復元された。スクリーンショット `theme-switch-gantt-light.png` / `theme-switch-gantt-dark.png` / `theme-switch-gantt-light-restored.png` と `theme-switch-gantt.txt` を成果物に残す。
- `.\gradlew.bat :microproject_ui:test --console=plain` → BUILD SUCCESSFUL（全件）。
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.theme.DarkThemeGanttGuiAcceptanceTest" --tests "com.microproject.dialog.FlatLafLegacyDialogRefreshGuiAcceptanceTest" --tests "com.microproject.util.FlatUiSupportDialogBindingsGuiAcceptanceTest" -PguiTestLocale=ja -PguiTestUiScale=1.0 --console=plain` → BUILD SUCCESSFUL（3 tests / 0 failures）。
