# Issue #1243 — ダークモードの配置アイコン（左・中央・右揃え）

State: IN PROGRESS（PR 作成前）
Issue: https://github.com/tetsuji16/microProject/issues/1243
Contract frozen at: 2026-11-19 issue body（#1221 のチェックリスト 3 の子）
Baseline HEAD: `0650c924e`（#1241 のブランチ上。master は `62dc73b23` 系）
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` の変更と `.workbuddy-ai/` / `.worktrees/` のみ未追跡

## Goal and scope

ダークテーマで固定インク `#2f6b53` のため沈む `ribbon.alignLeft` / `ribbon.alignCenter` / `ribbon.alignRight` を、16 / 20 / 32 px のすべてで背景に対して 3:1 以上にする。ライトの意図した深緑は維持する。

Out of scope: ビュー切り替え（`view.*`）・進捗率（`ribbon.percent*`）・その他の単色素材（#1221 の 4）、単色判定の一般化（同 5）、情報・カスタムフィールド（#1218）、書式（#1240）、クイック アクセス ツールバー（`getRibbonIconTinted` 経由）、ライトの意匠変更。
Blockers: #1241 が未マージのため、このブランチは #1241 の上に積む（PR は #1241 のブランチを base にする）。

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | ダークで 3 素材 × 16/20/32 px が 3:1 以上 | pass | `IconManagerRibbonIconTest.alignRibbonIconsKeepTheBrandInkOnlyOnLightRibbons`、修正前 2.2280064199371243 → 修正後 PASSED |
| A2 | ライトで 3 素材のインクが `#2f6b53` のまま | pass | 同じテストが 9 通りの素材で `#2f6b53` の画素を要求 |
| A3 | `#1218` / `#1240` のアイコンが退行しない | pass | `RibbonIconThemeContrastGuiAcceptanceTest` が情報 4 コマンド + 書式 3 コマンドを継続して検査 |
| A4 | 追加テストが修正前に失敗し、修正後に通る | pass | 修正前 `ribbon.alignLeft at 16 px contrast 2.2280064199371243 ... below 3.0`、修正後 PASSED |
| A5 | タスク選択状態で配置 3 コマンドが押せて読める | pass | GUI テストが `fixture.clickRow(0)` の後に `isEnabled` を要求。目視 `ribbon-icon-contrast-dark-align-group.png` |
| A6 | `:microproject_ui:test`、GUI テスト、`build`、inventory、`git diff --check` | pass | 下記「Latest evidence」 |
| A7 | 複数色アイコン・意味色・印刷白を変えない | pass | 置換対象を 12 素材の名前と `#2f6b53` に限定。他の素材は `applyRibbonInk` が素通し |

## Current slice

**Next slice:** 変更をコミットし、PR を作成する。
**Done when:** PR の必須チェックが通り、変更 SHA で受入が再確認できる。
**Then:** #1243 を close し、次は #1221 の 4（ビュー・進捗率ほか）。

## Remaining blockers

- PR の必須チェック結果（未取得）

## Latest evidence

- Tests/build（HEAD `0650c924e` + 作業差分）:
  - `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` → exit 0、BUILD SUCCESSFUL
  - `.\gradlew.bat :microproject_ui:test --console=plain` → exit 0、BUILD SUCCESSFUL（1m 44s）
  - `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonIconThemeContrastGuiAcceptanceTest" "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" --console=plain` → exit 0、PASSED
  - 同上 `-PguiTestLocale=en -PguiTestUiScale=1.25` → exit 0、PASSED
  - `.\gradlew.bat build --console=plain` → exit 0、BUILD SUCCESSFUL
  - `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
  - `git diff --check` → exit 0
- Physical GUI/manual: ダーク/ライトの書式タブ（配置グループ）を実ウィンドウでキャプチャ（`ribbon-icon-contrast-{dark,light}-align-group.png`、日本語 100%）。ダークは 3 アイコンが明るいグレーで読め、ライトは従来の深緑のまま。タスク 1 行を選択した状態（ステータス バー「選択中のタスク: 1」）。
- 計測メモ: 配置素材のストロークは viewBox 64 に対して 5 なので、16 px では 1.25 px 幅になり alpha 200 以上の画素が出ない。`IconManagerRibbonIconTest` の `dominantInk(image, minimumAlpha)` と GUI テストの `dominantInk` は alpha 128 以上をインクとして数える（アンチエイリアスの色は元のインク色のまま）。
- CI: 未取得（PR 作成後）
