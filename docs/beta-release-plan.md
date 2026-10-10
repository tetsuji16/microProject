# 0.0.24 ベータリリース計画

目的は、タスク表・ガント・CCPM を使って計画を作り、修正し、取り消し、保存後に再開できる最小実用版を公開すること。既存機能の品質を固め、新しい GUI 機能や全 MSP 互換性の拡張をベータの前提にしない。

## 実行順と完了条件

| 段階 | 作業 | 完了条件 |
| --- | --- | --- |
| 0 基準確定 | 修正済み commit `4a8309771`、JDK 25、再生成 installDist、決定的な outline/dependency/CCPM fixture を採用 | 同じ版・言語・DPIで再現でき、元サンプルを書き換えない |
| 1 タスク・構造 | canonical selection/command の既存実装を確認。表の入力、階層、Link/Unlink、Undo/Redo、MPO 再読込を検証 | 物理操作と永続モデル・描画が一致 |
| 2 ガント | 編集・ドラッグ・ズーム／スクロールの代表経路を既存 fixture で検証 | 選択とビューポートが意図どおりで、予定の変更と Undo/Redo が一致 |
| 3 CCPM | 既存の設定→適用→network/buffer を一つの Robot journey でつなぎ Undo/Redo・MPO 再読込も検証 | 基準、チェーン、表示が存在し、取り消しと再読込が成立 |
| 4 テスト効率化 | 全 unit/headless は維持。GUI は共有 fixture と代表経路を smoke、広い matrix は週次へ | 機能・境界・失敗の契約を維持して無駄な探索／fork／再compileを削減 |
| 5 配布 | version tag、Windows MSI/ZIP、JAR hash／実起動／読込、GUI smoke、SHA256SUMS | 全ゲート合格後のみ GitHub prerelease 公開 |

## 前段階からの品質改善

既存コードをベータ版と呼び替えるのではなく、代表操作を実行して障害を特定し、原因を修正してから公開する。起動・MPP/MPO 読込・ID 保持の修正に続き、CCPM Clear の Undo で失われる監視履歴と撤回記録を復元対象へ追加した。リソース置換の modal owner がメイン画面になっていた問題は、呼び出した割当ダイアログへ所有者を修正して解消する。新規行の物理入力→MPO 再読込の検証で、空行を含む XML と通常タスクの対応付けがずれる保存側の不具合を発見し、空行では通常タスク iterator を進めないよう修正する。既存の高 UID と予定 snapshot の回帰に空行を組み込む。GUI の失敗は製品の状態遷移とテストのフォーカス／描画判定を分けて調査し、待機や許容値で製品の欠陥を隠さない。

## 操作契約と所有者

所有者は canonical domain/command と view の責務で分ける。設定不足・選択不足・読取専用では変更しない。リボンへフォーカスしても stable task ID と active view を維持する。MPO は以前の対応版から読め、POD の wire layout は変更しない。

| 操作群 | 入力／必要状態 | モデル・画面の結果 | 取り消し／保存 | 実装・検証の所有者 |
| --- | --- | --- | --- | --- |
| 表の編集 | 編集可能な task row に複数文字の期間・進捗・日付を入力 | domain 値とセル・バーが一致。viewport を勝手に動かさない | 一回の Undo/Redo。構造変更後の MPO reload | spreadsheet input transaction、U26／TaskTableGanttGrid fixture |
| 階層・依存 | 単数／複数 task を選択して ribbon を操作 | canonical selection と parent/link が一致。行／バーも更新 | Undo/Redo と MPO | ActiveTaskSelectionResolver、TaskInformationRibbon fixture |
| ガント | 選択したバー・端と viewport を操作 | 日付／期間とバーの形状が一致 | 予定の Undo/Redo、MPO | canonical Gantt mutation、既存 drag/navigation fixture |
| CCPM | 編集可能な未設定 project の Configure→Apply | baseline と nonempty chain、network/buffer view | Ctrl+Z/Y、MPO reload 後も両表示 | CriticalChainService、CriticalChainStatusDialog fixture |
| ファイル | New/Open/Save、正しい extension と file chooser | 新規／読込／保存の正しい document | round trip、invalid inputで既存データ保持 | application/exchange、RibbonExternalCommand fixture |

CCPM は本製品の拡張機能であり MSP 相当と主張しない。既存 recovery sequence の課題を全部解消したとは扱わず、上記限定契約の現物ゲートだけをベータ出荷条件にする。

## 検証コマンド

```text
./gradlew build installDist verifyArchitectureBoundaries verifyPackagedFileImports
./gradlew :microproject_ui:guiTest -PguiTestSuite=smoke --max-workers=1
python -m unittest discover -s scripts/tests -v
```

Windows リリース workflow は同じ source commit に対して clean build、MSI/ZIP、GUI watchdog、配布 EXE の実起動を実施する。Linux のローカル Robot 合格は Windows での合格の代替にしない。

タグ（または管理用ブランチ `release/beta-0.0.24-1` の push）で出荷を開始する。タグは `v0.0.24-beta.1`、jpackage 数値版は `0.0.24.1`。beta は latest／安定版 update manifest を変更しない。`releaseChannel=beta` により MSI/ZIP の bootstrap launcher に自動更新禁止を固定し、stable feed が beta JAR を置き換える経路を防ぐ（共有 preferences は変更しない）。ゲート失敗時は公開せず、ログを保存して原因を修正し、同じ候補の検証をやり直す。公開後の blocker は次の beta 番号で修正し、既配布版のバイナリを差し替えない。

テスト方針は [beta-test-strategy](testing/beta-test-strategy.md)、配布ノートは [beta-0.0.24](releases/beta-0.0.24.md)。Windows の出荷結果は公開ゲート通過後に追記する。

## 候補版の検証結果（2026-10-10）

`releaseVersion=0.0.24.1` / `releaseChannel=beta`、JDK 25、ja / 100%、1920×1080 の Xvfb + 生存確認済み Openbox で実施。

| ゲート | 結果 |
| --- | --- |
| 全 module build / unit / headless | 2218 件、失敗 0、Windows 専用 1 件 skip |
| architecture / naming / distribution | 成功、再生成 installDist |
| 配布用限定 module の MPP/POD 読込 | 両 fixture 145 tasks、成功 |
| GUI smoke | 21 件、失敗・skip 0、1 分 9 秒 |
| scripts 回帰 | 8 件、成功 |
| Windows MSI/ZIP・launcher・GUI | 同一 source commit の release workflow で実施。合格前には公開しない |

発見した CCPM Clear の履歴欠落、置換 modal の owner、空行による MPO task identity のずれ、beta が stable feed を適用する経路を修正した。新規行→物理名前入力→MPO reload、削除→Undo/Redo、Gantt 依存／期間／進捗／split、CCPM 適用→Undo/Redo→MPO→両表示を代表ゲートに含めた。全画面・locale・DPI の課題をこの限定合格で閉じない。
