# Issue #1245 — 残りの単色リボン アイコン（71 素材）と単色判定の一般化

State: IN PROGRESS（PR 作成前）
Issue: https://github.com/tetsuji16/microProject/issues/1245
Contract frozen at: 2026-11-19 issue body（#1221 のチェックリスト 4 と 5）
Baseline HEAD: `ec0a648c6`（#1243 のブランチ上。master は `62dc73b23` 系）
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` の変更と `.workbuddy-ai/` / `.worktrees/` のみ未追跡

## Goal and scope

ダークのリボンに残る固定インクの単色アイコン 71 ファイル（`#5b6573` 31、`#0f6cbd` 18、`#217346` 10、`#2563a7` 8、`#4472c4` 4）を 3:1 以上にする。あわせて、素材名の列挙（`RIBBON_ICON_INKS`）をやめ、素材の色から単色を判定する経路へ一般化する（#1221 の 4 と 5 をまとめて行う）。

Out of scope: 複数色のイラスト 287 ファイル、GUI のコマンド単位の全件監査と allowlist（#1221 の 6）、ライトの意匠変更、クイック アクセス ツールバー、Gantt の意味色。
Blockers: #1241 / #1244 が未マージのため、このブランチは #1244 の上に積む。

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | 単色 104 ファイルがダークで 3:1 以上 | pass | `IconManagerRibbonIconTest.everyMonochromeRibbonIconKeepsItsInkOnlyOnLightRibbons` が 31 キー × 16/20/32 px = 93 通りを走査して PASSED |
| A2 | ライトで単色素材のインクが素材の色のまま | pass | 同じテストがサイズ別素材（`recalculate-16.svg` は `#0f6cbd`、`recalculate.svg` は `#4b83bd`）まで解決して画素を要求 |
| A3 | 複数色のイラストがダークで変わらない | pass | `multiColourRibbonIllustrationsKeepTheirPixelsOnADarkRibbon`（`ribbon.paste` / `ribbon.ccpmSettings` / `ribbon.bar` の 16/32 px が画素一致） |
| A4 | 追加テストが修正前に失敗し、修正後に通る | pass | 修正前 `ribbon.ccpmNetwork at 16 px must not paint #2563a7 on a dark ribbon`、修正後 PASSED |
| A5 | `#1218` / `#1240` / `#1243` の 32 ファイルが退行しない | pass | 既存の `formatRibbonIcons…` / `alignRibbonIcons…` / 情報アイコンのテストが PASSED |
| A6 | `:microproject_ui:test`、`build`、inventory、`git diff --check` | pass | 下記「Latest evidence」 |
| A7 | 単色判定が素材名の列挙に依存しない | pass | `RIBBON_ICON_INKS` を削除。`monochromeInkOf(URL)` が SVG の 16 進色 / ラスタの不透明画素から判定し、`URL` ごとにキャッシュ |

## Current slice

**Next slice:** 変更をコミットし、PR を作成して CI を dispatch する。
**Done when:** `build` と `GUI acceptance` が通り、変更 SHA で受入が再確認できる。
**Then:** #1245 を close し、#1221 の 4 と 5 を完了にする。次は 6（コマンド単位の監査と allowlist）。

## Remaining blockers

- GUI テストのローカル実行が `GUI_ENVIRONMENT_CONTENDED` で止まる（前面の OpenCode ウィンドウと重なる）。CI の dispatch で確認する。

## Latest evidence

- Tests/build（HEAD `ec0a648c6` + 作業差分）:
  - `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` → exit 0、BUILD SUCCESSFUL
  - `.\gradlew.bat :microproject_ui:test --console=plain` → exit 0、BUILD SUCCESSFUL（3m 12s）
  - `.\gradlew.bat build --console=plain` → exit 0、BUILD SUCCESSFUL（1m 6s）
  - `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
  - `git diff --check` → exit 0
- 監査（2026-11-19、リポジトリの SVG 391 ファイルを静的走査）: 単色 104、複数色 287。単色の内訳は `#5b6573` 31、`#0f6cbd` 18、`#2f6b53` 12、`#1f3b73` 12、`#217346` 10、`#2563a7` 8、`#212121` 8、`#4472c4` 4、`#4b83bd` 1。`#4b83bd`（`recalculate.svg`）だけは 3.54:1 で基準内。
- 注意: `recalculate` はベース素材が `#4b83bd`、16/20/32 px のサイズ別素材が `#0f6cbd` と色が違う。テストはサイズ別素材を解決して期待インクを決める。
- GUI: ローカルは `GUI_ENVIRONMENT_CONTENDED`（前面 pid 2216 `OpenCode`）で 2 回停止。CI で確認する。
- CI: 未取得（PR 作成後）
