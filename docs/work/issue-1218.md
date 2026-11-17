# Issue #1218 — ダークモードの「情報」「カスタムフィールド」アイコン

State: IN PROGRESS（PR 作成後、CI 待ち）
Issue: https://github.com/tetsuji16/microProject/issues/1218
Contract frozen at: 2026-11-18 issue body + owner comment (#1221 の最初の子)
Baseline HEAD: `093732cad`
Baseline working tree: clean（ブランチ作成前）

## Goal and scope

ダークテーマで固定インク `#212121` のため見えなくなる `ribbon.information` と `ribbon.customFields` を、リボン・行メニュー・Backstage のすべてで 3:1 以上にする。ライトの見た目と意味色は維持する。

Out of scope: その他の単色アイコン（#1221）、白黒混在の再現条件（#1216）、他画面の固定色（#483）、リボンの意匠変更（#765）。
Blockers: none（`status: ready`）。

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | ダークでタスク/リソース/プロジェクトの「情報」とプロジェクトの「カスタムフィールド」が 3:1 以上 | pass | `RibbonIconThemeContrastGuiAcceptanceTest` ja/1.0 と en/1.25、修正前 1.15 → 修正後 ≥3 |
| A2 | ダークの行メニュー「タスク情報」と Backstage「情報」カードも 3:1 以上 | pass | 同じテストが 16px/32px の `getRibbonIcon` 解決を検査（両面ともこの経由） |
| A3 | ライトで現行インク `#212121` と見た目が変わらない（16.10:1） | pass | テストがライトのインクを明示的に assert |
| A4 | 追加テストが修正前に失敗し、修正後に通る（失敗時の比を記録） | pass | 修正前 `dark RibbonTaskInformation icon contrast 1.1525114313303584 ... below 3.0`、修正後 PASSED |
| A5 | `:microproject_ui:test`、GUI テスト、`build`、`test_source_inventory.py` が通る | pass | 下記「Latest evidence」 |
| A6 | 複数色アイコン・Gantt 意味色・印刷白を変えない | pass | 差し替え対象を 8 個の SVG 名に限定、`IconManagerRibbonIconTest` のモノクロ条件も維持 |

## Current slice

**Next slice:** PR を作成し、必須チェック `build` と `GUI acceptance` の結果を待つ。
**Done when:** 両チェックが通り、変更 SHA で受入が再確認できる。
**Then:** close and report。

## Remaining blockers

- PR の必須チェック結果（未取得）

## Latest evidence

- Tests/build（HEAD `093732cad` + 作業差分）:
  - `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonIconThemeContrastGuiAcceptanceTest" "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" --console=plain` → exit 0、PASSED
  - 同上 `-PguiTestLocale=en -PguiTestUiScale=1.25` → exit 0、PASSED
  - `.\gradlew.bat :microproject_ui:test --console=plain` → exit 0（1753 tests）
  - `.\gradlew.bat build --console=plain` → exit 0、BUILD SUCCESSFUL
  - `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
  - `git diff --check` → exit 0
- Physical GUI/manual: GUI テストが実ウィンドウをキャプチャ（`issue-1218-dark-ribbon.png` / `issue-1218-light-ribbon.png`）。ダークで「情報」と「カスタムフィールド」のアイコンが明るいグレーで描画されていることを目視確認。
- CI: pending（PR 作成後）
