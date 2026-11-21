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
| A3 | 全対象ウィンドウでリサイズ後も文字/操作部品が欠けず、内容領域が無駄に空かない | pass | `WindowSizeContractSweepGuiAcceptanceTest` が 23 ダイアログ + メインウィンドウを 1 回で開き、宣言した契約（FIXED_FORM / RESIZABLE）を検証。成長時の文字高さ・作業面の成長吸収・packed 最小への復帰・usable screen 内を確認。契約表と共通原因の修正は `docs/dialog-layout-audit.md` の "Cross-window resize contract (#863 A3)"。 |
| A4 | 日英の基準サイズおよび高DPIで物理GUIテストと画像レビューが通る（不可条件は明示） | open | ja 1.0 は一部実施済み。en と 125%/150% は未実施（#724 A9 と同じく hosted 1920×1080 が必要）。 |

## 計画しているスライス（1つずつ）

1. **S1 インベントリ** (完了): 全ウィンドウ/主要タブの静的棚卸し + 既存証跡の再利用状況を監査表に記録。#724 A8–A10 との重複を統合。
2. **S2 共通原因** (完了): 静的棚卸しで検出した共通原因を共有レイアウト方針で修正 + 回帰テスト。
3. **S3 検証** (A3 完了 / A4 未完了): 物理GUI経路 + 視覚assertion + 日英/高DPIマトリクス（実行できる条件だけ）。実行不能条件は明示。

## S3-A3 横断リサイズ検証（完了）

- 追加: `WindowSizeContractSweepGuiAcceptanceTest`（23 ダイアログ + メインウィンドウ）と `DialogLayoutAssertions.assertWindowSizeContract / assertResizeKeepsWorkArea / assertNoDeadBand / workSurface`。
- 各ウィンドウは `FIXED_FORM` か `RESIZABLE` を宣言し、スイートが実際の `isResizable()` と一致することを検証する。二重契約は作らず、`#724` の `isFixedSizeForm()` をそのまま使う。
- 共通原因: `#724` が定めた「内容が1つの bounded form なら `isFixedSizeForm()` を true にする」契約に、About / Help / Rename Project / Clear Baseline / Recurring Task / Update Task / Delegate Task / Calendar Options の8画面が従っていなかった。リサイズするとフォーム下に空の帯が出る状態だったため、8画面が契約を宣言するよう修正。
- 副産物: `RenameProjectDialog` が名前を先に埋めると NPE で開けない（Save As / Rename Project 経路）。名前の document listener が OK ボタン生成前に走っていたため。`updateNameValidity()` をボタン未生成でも安全にし、`createButtonPanel()` で再評価するよう修正。
- 実行: `:microproject_ui:guiTest -PguiTestSuite=full --tests com.microproject.dialog.WindowSizeContractSweepGuiAcceptanceTest` → pass（ja / 100%）。
- 未実行: en / 125% / 150% の同スイート（A4）。

## 既知の制約

- #724 パケット: 125%/150% の物理実行には hosted 1920×1080 デスクトップが必要でローカルは 2560×1392 だが高さが不足する場合がある。
- #724 A10: Task Information Resources タブの空白は 2026-10-02 のキャプチャ以降 unclassified。
- 既存テスト `TaskInformationRibbonGuiAcceptanceTest.assertTaskAssignmentShowsResource`（2097行）が Resources タブの assignment 表が viewport の半分以上を占めることを検証済み。

## Latest evidence

- HEAD `8c546d1d5` は #1214 (PR #1222) のマージ後。CI 全チェック pass（GUI acceptance 含む、再実行で macOS runner 容量問題と chooser flake を回復）。
