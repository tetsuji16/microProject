# CCPM ネットワークグラフ: 実装計画

## 結論

microProject は、資源制約を加味したチェーン、プロジェクト／フィーディングバッファ、バッファ消費の分析基盤を既に持つ。一方、ネットワーク表示はタスク名と ID を直接描くだけの読み取り専用 `JPanel` であり、CCPM を実行管理に使うための「何が危険で、誰が次に何をすべきか」という導線が足りない。

最初の実装単位は、既存のスケジュール計算を変えずに、ネットワークを探索・判断できる表示面にすることである。複数プロジェクトの優先度最適化や、新しい CCPM 計算方式はこの Issue の対象外とする。

## 比較と差分

| 観点 | MSP 標準 | CCPM+ for MS Project | ProChain / Fusion Online | microProject 現状 | 優先する差分 |
|---|---|---|---|---|---|
| クリティカル性 | 依存関係と slack による Critical Path。複数パスも表示可能 | 資源制約を含む Critical Chain を識別 | 資源制約を含む計画を計算 | `CriticalChainService` が資源制約 edge を分析 | 計算はある。表示上で dependency と resource constraint を明確化する |
| バッファ | 任意の slack／遅延の確認 | Project / Feeding buffer の配置・色付き Gantt、閾値 | Project / Feeding buffer、バッファ消費と fever/time chart | Project / Feeding buffer と fever chart がある | グラフの各バッファに消費率、残量、行動状態、保護対象を出す |
| 実行判断 | クリティカルタスクを追跡 | 緑・黄・赤とリカバリー判断、resource priority list | バッファ状態、task/endpoint priority | 状態文字列と基本チャートのみ | 凡例、色以外の状態表現、危険バッファへの drill-down を追加する |
| 操作性 | Gantt/Network 上でビュー操作 | filters、critical chain/buffer 表示、リソースフィルタ | ポートフォリオ、優先度、時系列表示 | スクロールだけ。hover、選択、ズーム、Gantt/表との同期なし | node selection と task table/Gantt の相互遷移、fit/zoom、フィルタを追加する |
| アクセシビリティ | 標準のキーボード／表示設定 | 製品依存 | 製品依存 | パネル名・tooltip のみ | フォーカス可能な node、テキスト凡例、色以外の状態を追加する |

MSP 自体は critical path と total slack の確認を提供するが、共有資源を含む CCPM の chain/buffer 管理は標準機能の範囲外である。[Microsoft の critical path 説明](https://support.microsoft.com/et-ee/project/manage-your-project-s-critical-path) はこの比較の MSP 側の基準にする。[CCPM+ の機能一覧](https://advanced-projects.com/ccpmsoftware.aspx) は buffer report、fever chart、色付き Gantt、資源別の優先タスクを示す。[ProChain の比較基準](https://www.prochain.com/what-to-look-for-in-critical-chain-project-management-software/) は資源制約スケジューリング、バッファ計算／消費、優先順位、複数プロジェクト可視化を、CCPM 製品を判定する軸としている。

## 現在の構造と今回の骨組み

```text
Project + assignments/dependencies/progress
  -> CriticalChainService.Analysis       (唯一の CCPM 分析入力)
  -> CriticalChainGraphScene             (不変の描画・hit-test 用投影)
  -> CriticalChainGraphPanel             (Swing paint のみ)
  -> ResourceLevelingDialogBox / CriticalChainStatusDialogBox
```

今回、`CriticalChainGraphScene` を導入した。各 node は `task:<uniqueId>`、`project-buffer`、`feeding-buffer:<taskId>` の安定キー、種別、ラベル、詳細、矩形を持つ。edge は dependency、resource constraint、buffer protection を型で区別する。`CriticalChainGraphPanel` は scene を描くだけで、今後の選択、ズーム、フィルタ、アクセシブルな node 移動、SVG/印刷出力が別々に分析や座標を再計算しないようにする。

この変更は表示専用であり、タスク、依存関係、日程、CCPM baseline を変更しない。Undo/Redo および保存形式への影響もない。

## 実装ロードマップ

### Phase 1 — 一覧性と説明可能性（この Issue の必須範囲）

1. `CriticalChainGraphScene` を唯一の node/edge/geometry 提供者にし、循環・孤立 edge・空分析を安全に投影する。
2. 描画に凡例を追加する。dependency、resource constraint、buffer protection の線種・名称、GREEN/AMBER/RED の文字列を併記し、色だけで状態を伝えない。
3. task node に ID、名称、開始／終了、残期間または進捗を、buffer node に計画量・消費量・残量・消費率・状態を表示する。長い名称は省略するが tooltip と accessible description では完全な値を出す。
4. project buffer を末端 critical task へ、feeding buffer を保護対象 task へ矢印で接続する。resource constraint は依存関係と別の色・線種・tooltip にする。
5. `Fit to window`、100% 表示、拡大／縮小を提供する。計算済み scene の座標を transform するだけとし、再分析しない。

### Phase 2 — 調査から行動へ（後続 Issue）

1. node の mouse/keyboard selection と hover card を実装し、task node を選ぶと task table と Gantt の同一 `uniqueId` を選択・表示する。バッファ node は保護対象、影響 chain、推奨アクションを出す。
2. フィルタは `all / critical chain only / resource constraints / at-risk buffers` とし、scene から派生した view state として保持する。分析結果やプロジェクトデータは変更しない。
3. GREEN/AMBER/RED の状態、しきい値、バッファ消費量をバッファ表と fever chart で共通の `Analysis` から表示する。黄色は「回復計画」、赤は「回復実行」という具体的な説明を表示する。

### Phase 3 — CCPM 実行・ポートフォリオ（別の大規模機能）

1. resource buffer と resource 別の次作業リスト。
2. 複数プロジェクトの buffer overview と endpoint priority。
3. calendar-time chart、印刷／SVG・PDF export、共有用の静的スナップショット。

## コマンド契約

```text
Command: ACTION_CCPM_NETWORK
User routes: microProject/CCPM ribbon, Project > CCPM menu, Ctrl+Shift+N
Selection: 不要。アクティブ文書のみ
Allowed state: プロジェクトあり。適用済み CCPM 計画がなければ設定・適用への明示導線
Model before -> after: 変更なし。CriticalChainService.analysis(project) の読取だけ
Visible before -> after: CCPM network dialog に scene、凡例、選択状態、ズーム状態を表示
Undo/Redo: なし（表示専用）
Persistence: CCPM baseline/settings は既存 MPOF のみ。zoom/filter/selection は document transient state
Invalid state: 空または未適用なら「CCPM を設定して適用」ボタンを表示。無言 return は禁止
Diagnostic result: opened / rejected(no-active-project|no-applied-plan) / failed と scene node/edge 数
```

## 実装タスク（ジュニア担当用）

1. `CriticalChainGraphScene` に表示用メタデータを拡張し、task と buffer の全文 tooltip/accessibility text、edge の説明、バッファ集計値を追加する。`CriticalChainService.Analysis` を再計算・変更しない。
2. `CriticalChainGraphPanel` に `GraphViewportState`（zoom、pan、filter、selected key）を追加する。scene は不変、viewport state は document transient state とする。
3. 描画を `GraphRenderer` へ分離し、edge → node → selection/focus → legend の順で描く。resource constraint と buffer protection は線種と名前で区別し、GREEN/AMBER/RED は形状／テキストも使う。
4. keyboard focus を panel に置き、Tab/Shift+Tab で node、矢印で近接 node、Enter で選択、`F` で Fit、`+/-` でズームする。マウス click と hover も同じ `selectNode(key)` 経路に通す。
5. task node 選択は既存 document selection resolver を経由して spreadsheet/Gantt に同期する。新しい独自 selection model、component-level shortcut、KeyListener は作らない。buffer node は mutation せず status/detail を表示する。
6. status dialog と resource leveling preview が同じ scene/renderer を使うことを確認する。Gantt overlay、計算、保存、baseline、Undo の経路は変更しない。
7. 日本語・英語、100/125/150% DPI で凡例、tooltip、ズームボタン、ノード詳細が viewport 内にあり重ならないことを検証する。

## 受入条件と検証

- 空分析、一本の dependency chain、resource constraint、複数 feeding buffer、複数 terminal task の scene を headless test で検証する。node key の一意性、edge の両端、hit-test、preferred size、project/feeding buffer の接続を assert する。
- `ACTION_CCPM_NETWORK` の menu/ribbon/root-pane shortcut は一つの action に届くことを route integration test で確認する。
- `src/guiTest` では、実 Robot で task node をクリックし、task table と Gantt の同一 task を選択・可視化する。buffer node のクリックでは project data が変わらないことも assert する。
- 表示専用の操作では task 日付、依存関係、assignment、baseline、Undo stack が不変であることを assert する。CCPM apply 後の MPO 保存・再読込でも同じ analysis から graph を開けることを確認する。
- 日本語／英語と 100/125/150% で visual-layout harness を実行し、凡例・操作部・詳細が clip／overlap しないことを bounds assertion で確認する。

実行コマンド:

```powershell
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.dialog.CriticalChainApplyAndRenderTest" --console=plain
.\gradlew.bat :micrproject_ui:test --console=plain
.\gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.views.CcpmSampleProgressGuiAcceptanceTest" --max-workers=1 --console=plain
.\gradlew.bat :micrproject_ui:guiTest -PguiTestLocale=ja -PguiTestUiScale=125 --max-workers=1 --console=plain
.\gradlew.bat :micrproject_ui:guiTest -PguiTestLocale=en -PguiTestUiScale=150 --max-workers=1 --console=plain
```

## 非対象・注意点

- CCPM 計算式、バッファ係数、resource leveling の意味、MPOF schema はこのグラフ UI Issue で変更しない。
- MS Project の標準 Critical Path 表示を CCPM 表示で置換しない。CCPM は microProject 独自の分析面として維持する。
- 表示の click handler から task を直接更新しない。選択同期以外の変更は、将来の canonical command と一つの Undo transaction を通す。
- 実装完了の判定に「ダイアログが開く」「例外がない」だけは使わない。上記の model/view/route/DPI/persistence の assertion が必要である。
