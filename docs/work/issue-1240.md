# Issue #1240 — ダークモードの書式アイコン（太字・斜体・下線）

State: IN PROGRESS（PR 作成後、CI 待ち）
Issue: https://github.com/tetsuji16/microProject/issues/1240
Contract frozen at: 2026-11-18 issue body（#1221 のチェックリスト 2 の子）
Baseline HEAD: `c6fb9ad0b`
Baseline working tree: clean（`.workbuddy-ai/` と `.worktrees/` のみ未追跡）

## Goal and scope

ダークテーマで固定インク `#1f3b73` のため沈む `ribbon.bold` / `ribbon.italic` / `ribbon.underline` を、16 / 20 / 32 px のすべてで背景に対して 3:1 以上にする。ライトの意図した濃紺は維持する。

Out of scope: 配置アイコンとその他の単色素材（#1221 の 3〜4）、単色判定の一般化（同 5）、情報・カスタムフィールド（#1218、完了）、クイック アクセス ツールバー（`getRibbonIconTinted` 経由）、ライトの意匠変更。
Blockers: none（`status: ready`）。

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | ダークで 3 素材 × 16/20/32 px が 3:1 以上 | pass | `IconManagerRibbonIconTest.formatRibbonIconsKeepTheBrandInkOnlyOnLightRibbons`、修正前 1.284974463584611 → 修正後 PASSED |
| A2 | ライトで 3 素材のインクが `#1f3b73` のまま | pass | 同じテストが 9 通りの素材で `#1f3b73` の画素を要求 |
| A3 | `#1218` の情報・カスタムフィールドが退行しない | pass | `RibbonIconThemeContrastGuiAcceptanceTest` が 4 コマンドと 16/32 px の解決を継続して検査 |
| A4 | 追加テストが修正前に失敗し、修正後に通る | pass | 修正前 `ribbon.bold at 16 px contrast 1.284974463584611 ... below 3.0`、修正後 PASSED |
| A5 | タスク選択状態の B / I / U が押せて読める | pass | GUI テストが `fixture.clickRow(0)` の後に `isEnabled` を要求。目視 `ribbon-icon-contrast-dark-font-group.png` |
| A6 | `:microproject_ui:test`、GUI テスト、`build`、inventory、`git diff --check` | pass | 下記「Latest evidence」 |
| A7 | 複数色アイコン・意味色・印刷白を変えない | pass | 置換対象を 12 素材の名前と `#1f3b73` に限定。他の素材は `applyRibbonInk` が素通し |

## Current slice

**Next slice:** PR を作成し、必須チェック `build` と `GUI acceptance` の結果を待つ。
**Done when:** 両チェックが通り、変更 SHA で受入が再確認できる。
**Then:** close and report。#1221 のチェックリスト 2 を更新し、次は 3（配置アイコン）。

## Remaining blockers

- PR の必須チェック結果（未取得）

## Latest evidence

- Tests/build（HEAD `c6fb9ad0b` + 作業差分）:
  - `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` → exit 0、BUILD SUCCESSFUL
  - `.\gradlew.bat :microproject_ui:test --console=plain` → exit 0、BUILD SUCCESSFUL
  - `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonIconThemeContrastGuiAcceptanceTest" "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" --console=plain` → exit 0、PASSED
  - 同上 `-PguiTestLocale=en -PguiTestUiScale=1.25` → exit 0、PASSED
  - `.\gradlew.bat build --console=plain` → exit 0、BUILD SUCCESSFUL
  - `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
  - `git diff --check` → exit 0
- Physical GUI/manual: ダーク/ライトのフォント グループを実ウィンドウでキャプチャ（`ribbon-icon-contrast-{dark,light}-font-group.png`、日本語 100%）。ダークは B / I / U が明るいグレーで読め、ライトは従来の濃紺のまま。タスク 1 行を選択した状態（ステータス バー「選択中のタスク: 1」）。
- 注意: 上記 GUI 実行の後、`%TEMP%\mp997\app\microProject.exe` のウィンドウが前面を占め、再実行は `GUI_ENVIRONMENT_CONTENDED` で 2 回停止した。GUI テストのソースは上記実行から変えておらず、その後の差分は `IconManager` のコメントと `IconManagerRibbonIconTest` の定数位置だけなので、上記の結果をそのまま使う。次の GUI 実行は前面を空けてから行う。
- CI: pending（PR 作成後）
