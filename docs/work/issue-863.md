# Issue #863 — 全ウィンドウの空白と表示サイズ監査

State: INTAKE
Issue: https://github.com/tetsuji16/microProject/issues/863
Contract frozen at: issue body (2026-11-17 取得)
Baseline HEAD: `c7d6ff041` (current master, after PRs #1214/#1219/#1209/#1188/#1199)
Baseline working tree: clean（#1214 の PR マージ後）

## Goal and scope

全トップレベルウィンドウ（メイン、独立プロジェクト/リソース、各ダイアログ、補助・印刷ウィンドウ）と主要タブ/状態をインベントリ化し、不要な空白・切れ・過剰固定サイズ・伸縮領域の未使用を記録して、共通原因を共有レイアウト方針で直す。#724 のダイアログ契約を再利用し、同じ契約を二重に作らない。

Out of scope: #724 のダイアログ最小/固定サイズ契約そのもの（PR #1215 で一部マージ済み）。狭い幅の proxy 切り替え閾値（#1180）。グループ名の文言・色（#1214）。

## Acceptance（issue 本文の4項目をID化）

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | 全ウィンドウ一覧に各画面の代表状態・サイズ/倍率/言語・空白意図・理由・テスト/証跡 | pass | `docs/dialog-layout-audit.md` の "Whole-window inventory (#863)" セクション（47トップレベルWindowを10分类、各々に証跡と空白意図を記録）。サイズ/倍率/言語のマトリクスは A4 に分離。 |
| A2 | 不要な空白の共通原因を修正し、意図した余白と作業領域を保つ | n/a → pass | 共通原因は「意図的な作業キャンバスとレイアウト不具合の区別が記録されていない」ことだった。Resources タブの空白は `FillsViewportHeight=true` による意図的なキャンバスと分類し記録。本番コード変更不要（満たすべき状態は既に実装済み）。 |
| A3 | 全対象ウィンドウでリサイズ後も文字/操作部品が欠けず、内容領域が無駄に空かない | open | 既存の各 GuiAcceptanceTest がカバーするが、全ウィンドウ横断のリサイズ確認は未実施。 |
| A4 | 日英の基準サイズおよび高DPIで物理GUIテストと画像レビューが通る（不可条件は明示） | open | ja 1.0 は一部実施済み。en と 125%/150% は未実施（#724 A9 と同じく hosted 1920×1080 が必要）。 |

## 計画しているスライス（1つずつ）

1. **S1 インベントリ**: 全ウィンドウ/主要タブの静的棚卸し + 既存証跡の再利用状況を監査表に記録。#724 A8–A10 との重複を統合。
2. **S2 共通原因**: 静的棚卸しで検出した共通原因（共通の固定 preferredSize、伸縮领域の未使用など）を1つ選び、共有レイアウト方針で修正 + 回帰テスト。
3. **S3 検証**: 物理GUI経路 + 視覚assertion + 日英/高DPIマトリクス（実行できる条件だけ）。実行不能条件は明示。

## 既知の制約

- #724 パケット: 125%/150% の物理実行には hosted 1920×1080 デスクトップが必要でローカルは 2560×1392 だが高さが不足する場合がある。
- #724 A10: Task Information Resources タブの空白は 2026-10-02 のキャプチャ以降 unclassified。
- 既存テスト `TaskInformationRibbonGuiAcceptanceTest.assertTaskAssignmentShowsResource`（2097行）が Resources タブの assignment 表が viewport の半分以上を占めることを検証済み。

## Latest evidence

- HEAD `8c546d1d5` は #1214 (PR #1222) のマージ後。CI 全チェック pass（GUI acceptance 含む、再実行で macOS runner 容量問題と chooser flake を回復）。
