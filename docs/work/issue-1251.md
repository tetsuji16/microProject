# Issue #1251 — リボン コントラスト回帰を standard ribbon 全コマンドへ拡張

State: IN PROGRESS（PR #1248 に同梱、CI 確認中）
Issue: https://github.com/tetsuji16/microProject/issues/1251
Contract frozen at: 2026-11-19 issue body（#1221 のチェックリスト 6）
Baseline HEAD: `2ac37d742`（#1245 のブランチ上）
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` の変更と `.workbuddy-ai/` / `.worktrees/` のみ未追跡

## Goal and scope

ダークのリボン コントラスト回帰を、`images.properties` の `ribbon.*` キーだけでなく standard ribbon の全コマンド アイコンへ広げる。standard ribbon は 132 キーを使い、うち 24 キーは `ribbon.*` 以外にあるため、その分が未検査だった。

Out of scope: 複数色のイラストの意匠、GUI テストのコマンド一覧の拡張、ライトの意匠変更。
Blockers: none（#1248 のブランチに 1 コミット足す。スタックを深くしないため別 PR にはしない）。

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | 走査対象が `ribbon.*` 114 + standard ribbon のみ 24 = 138 キーになる | pass | `sweepKeys()` が和集合を返す。計測時に `ribbon=114 standard=132 union=138` を確認 |
| A2 | standard ribbon の全アイコン キーが解決する | pass | 走査前に `getRibbonIcon(key, 32, 32)` の非 null を要求 |
| A3 | 単色素材がライトでインク維持、ダークで 3:1 以上 | pass | `everyMonochromeRibbonIconKeepsItsInkOnlyOnLightRibbons` PASSED |
| A4 | allowlist が空 | pass | 未是正の単色素材がないため allowlist を持たない（複数色は単色判定の対象外） |
| A5 | `:microproject_ui:test`、`build`、inventory、`git diff --check` | pass | 下記 |

## Latest evidence

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` → exit 0
- `.\gradlew.bat :microproject_ui:test --console=plain` → exit 0（1m 51s）
- `.\gradlew.bat build --console=plain` → exit 0（21s）
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → exit 0
- `git diff --check` → exit 0
- CI: PR #1248 の追加コミットで確認する。
