# Issue #1262 — 分割ビューとフォームの現状調査

State: SURVEY（製品コードは変更しない。最初の実装単位の Issue 本文案を issue にコメント済み）
Issue: https://github.com/tetsuji16/microProject/issues/1262
Contract frozen at: 2026-11-19 の issue 本文（基準 `a09ad021fa9504f99a681cc0c1432e019c41744f`）
Survey baseline: master `fb4bffbc9`

基準からの差分は 10 コミット。調査対象の 3 ファイル（`DocumentFrame.java`、`UsageDetailView.java`、`ChartView.java`）には差分がない（`git log --oneline a09ad021f..HEAD -- <3 files>` が空）。以下の行番号は `fb4bffbc9` のもの。

## 1. 上部ビューと下部ペインの分割

| 対象 | 実装 |
|---|---|
| 下部ビューの集合 | `DocumentFrame.BOTTOM_VIEW_ACTIONS`（213 行）= Resource Graph（`ACTION_HISTOGRAM`）、Charts（`ACTION_CHARTS`）、Task Usage（`ACTION_TASK_USAGE`）、Resource Usage（`ACTION_RESOURCE_USAGE`） |
| Details の切替 | `DocumentFrame.activateView` の `ACTION_DETAILS` 分岐（2205 行）。下部が開いていれば `deactivateBottomView()`、閉じていれば `lastBottomViewName`（null と `ACTION_NO_SUB_WINDOW` は `ACTION_TASK_USAGE`）で `activateBottomView(getBottomViewForRestore(), restoreName)` |
| 下部ビューの有効化 | `activateBottomView`（2300 行）。同一ビューの再選択は状態同期だけで早期 return。`onActivate(false)` → `mainView.setBottom` → `onActivate(true)` → `lastBottomViewName` 更新 → `setBottomActionSelected` → `toggleMinWidth` → `refreshUndoButtons` → `trackBottomPaneFocus` → 選択の引き渡し |
| 下部ビューの解除 | `deactivateBottomView`（2351 行）。閉じる前に `lastBottomViewName = lastBottomButton` を残し、`onActivate(false)`、`mainView.removeBottom()`、`lastBottomButton = ACTION_NO_SUB_WINDOW` |
| 3 つのトグルの選択状態 | `setBottomActionSelected`（2410 行）。4 つの下部ビュー、`ACTION_NO_SUB_WINDOW`、`ACTION_DETAILS` の選択状態を同時に更新する |
| 復元先の決定 | `getBottomViewForRestore`（2419 行）。`ACTION_RESOURCE_USAGE` / `ACTION_HISTOGRAM` / `ACTION_CHARTS` 以外はすべて Task Usage。**上部ビューの種別（タスク系・リソース系）を見ていない** |
| 選択の同期 | `selectionChanged`（2482 行）が下部コンポーネントの `SelectionNodeListener` へ転送し、`GraphicManager.selectionChanged` を呼ぶ。Task Inspector が表示中なら選択タスクを `showTask` へ渡す |
| ペインの器 | `MainView.setBottom`（98 行）/ `removeBottom`（159 行）。上下とも `SplittedView` のとき列幅を同期し、閉じるときは divider サイズを 0 にする |

`ACTION_TASK_USAGE_DETAIL` / `ACTION_RESOURCE_USAGE_DETAIL` は上部ビュー（詳細つき使用状況）であり、下部ペインではない（2169〜2174 行）。

## 2. 下部ペインに入るビュー（フォームではない）

| クラス | 中身 | 編集 |
|---|---|---|
| `UsageDetailView` | 左が割当の表、右がタイムスケール（`SplittedView`）。Task Usage と Resource Usage の両方 | セル編集は `TimeSpreadSheetModel.isCellEditable` と `Field.isReadOnly` 経由で、プロジェクトの `UndoController` に載る。`onActivate`（419 行）は空 |
| `ChartView` | リソース グラフ（`ACTION_HISTOGRAM`）と Charts（`ACTION_CHARTS`） | `onActivate`（217 行）は空 |

下部ペインに入るのは表とグラフだけで、`TaskForm` / `TaskDetailsForm` / `ResourceForm` に相当するクラスは `modules/microproject_ui/src` に存在しない（`grep -rn "TaskForm\|ResourceForm\|TaskDetailsForm"` はテストメソッド名だけを返す）。MSP の「下側にフォームを出し、OK / Next で確定する」経路は未実装。

## 3. フォームに相当する既存機能（別物として扱う）

| 機能 | 実装 | 分割ビューとの関係 |
|---|---|---|
| Task Inspector | `TaskInspectorPanel`（`com.microproject.dialog`、39 行）。`body.setEditable(false)`（214 行）で読み取り専用。`DocumentFrame.showTaskInspector`（2497 行）/`toggleTaskInspector`（2514 行）、リボン `RibbonTaskInspector`（`GraphicManager.RibbonTaskInspectorAction`、2387 行） | 右側の独立パネル。選択には追随するが編集できず、下部ペインでも MSP のフォームでもない |
| 情報ダイアログ | `TaskInformationDialog`（82 行）、`ResourceInformationDialog`（61 行）、`ProjectInformationDialog`、`InformationDialog` | モーダル。リボンの `RibbonTaskInformation` / `RibbonResourceInformation` から開く。下部ペインとは独立 |
| リボンの Details | `menuInternal.properties` 699〜702 行（`RibbonDetails.action=DetailsAction`、`type=TOGGLE`、`control=checkBox`）。`RibbonTaskDetails`（463 行）と `RibbonResourceDetails`（458 行）も同じ `DetailsAction` | 3 つとも同じ下部ペインのトグル。フォームは開かない |

## 4. 既存テストが証明していること

| テスト | 証明している内容 |
|---|---|
| `RibbonButtonBehaviorTest.bottomViewTransitionsKeepComponentAndRibbonStateInSync`（1711 行） | Resource Graph と Charts の切替、同一ビューの再選択が冪等、上部ビューを Gantt に変えても下部が外れない、`ACTION_NO_SUB_WINDOW` の冪等性、Task Usage を開いて Details で閉じ、再度 Details で Task Usage が戻る、`ACTION_DETAILS` の `SELECTED_KEY`、`Workspace.bottomViewName` の保存と同一セッション内の復元 |
| `RibbonButtonBehaviorTest.detailsSelectionClearsWhenTheDocumentLosesCommandFocus`（1820 行） | コマンド フォーカスを失うと Details の選択状態が外れる |
| `TaskInformationRibbonGuiAcceptanceTest.robotBottomViewSelectionRemainsVisibleUntilExplicitNoSubWindow`（2828 行） | 物理 Robot で View タブ → Details の checkBox をクリックし、下部が閉じる・復元する・選択状態が追随することを画面上で確認 |
| `TaskInformationRibbonGuiAcceptanceTest.physicalTaskInspectorFollowsSelectionAndNavigatesOnlyVisiblePredecessors`（7440 行） | Task Inspector が選択に追随し、非表示の先行タスクへは遷移しない |
| `RibbonButtonBehaviorTest.viewAndToggleRoutesAreInvokableAgainstALiveDocumentContext`（`RibbonDetails` を含む） | ルートが例外なく呼べることだけ。切替後の状態は見ていない |

## 5. 証明していないこと

- **リソース系の上部ビューでの Details**: Resource Sheet / Resource Usage を上部に置いて Details を押す旅程はない。コード上は「直前の下部ビューが無ければ Task Usage」なので、リソース ビューでもタスクの使用状況が下に出る。MSP の Resource Form 相当にはならない。
- **種別をまたぐ復元**: Task Usage を開いたままリソース系の上部ビューへ移り、Details を閉じて再度開いた場合の復元先。`lastBottomViewName` はそのまま使われる。
- **MPO 保存・再読込をまたぐ分割状態**: workspace の復元は同一セッション内だけで検証されている。ファイルへ保存して開き直す経路は未検証。
- **下部ペインでの編集の影響**: 編集可能セル経由のモデル変更・dirty・Undo は、分割ビューの文脈では検証されていない（Details の切替自体がモデルを変えないことは上記テストで確認済み）。
- **読取専用ドキュメントと listener 解放**: `UsageDetailView.cleanUp` はあるが、Details の開閉を繰り返したときの解放は分割ビューの文脈では未検証。
- **フォーム**: 実装も GUI 証跡もない。

## 6. 4 領域への分割と既存 Issue との整合

| 領域 | 現状 | 進め方 |
|---|---|---|
| (a) Details とビュー選択・選択同期 | 骨格は実装済み。穴はリソース系の復元先と種別をまたぐ復元 | 既存挙動の修正として独立に実装できる。**最初の実装単位** |
| (b) Task Form | 未実装 | (a) の後。下部ペインに編集可能なフォームを載せ、OK / Next の確定と未確定編集の扱いを決める必要がある |
| (c) Task Details Form | 未実装 | (b) の後。(b) のフォーム基盤を共有する |
| (d) Resource Form | 未実装 | (b)(c) と同じ基盤の後、または (b) と並行 |

- #1166: リボンの並びとコンテキストタブ。本調査はコマンドの意味（Details が何を開くか）だけを扱い、配置は変えない。
- #453: リボン互換の全体追跡。本調査はその配下の実装ではなく、分割ビューに限定した現状固定。
- #1259: 独自のドッキングとポップアップ。MSP の分割ビューとは別機能なので、#1259 の範囲には触れない。
- #1180 / #1202: Assignment Information 画面。下部フォームとは別の画面で、本調査の実装単位とは重複しない。

依存順は (a) → (b) → (c) → (d)。(a) だけは既存挙動の修正なので、ほかの前提を待たずに着手できる。

## 7. 最初の実装単位

全文は issue のコメントに記載した。骨子は「Details の復元先を上部ビューの種別に合わせる」。

- タスク系の上部ビューでは Task Usage、リソース系では Resource Usage を既定にする。
- 直前の下部ビューが同じ種別ならそれを優先し、異種なら種別の既定へフォールバックする。
- Details の開閉はモデル・dirty・Undo を変更しない（現行どおり）。

## 8. この調査で実行したコマンド

| command | result |
|---|---|
| `git log --oneline a09ad021f..HEAD -- modules/microproject_ui/src/main/java/com/microproject/pm/graphic/frames/DocumentFrame.java modules/microproject_ui/src/main/java/com/microproject/pm/graphic/views/UsageDetailView.java modules/microproject_ui/src/main/java/com/microproject/pm/graphic/views/ChartView.java` | 出力なし（対象 3 ファイルに差分なし） |
| `git rev-list --count a09ad021f..HEAD` | 10 |
| `grep -rn 'ACTION_DETAILS\|activateBottomView\|getBottomViewForRestore\|lastBottomViewName' modules/microproject_ui/src/main/java` | 上表の行番号（`rg` は未導入のため `grep` で代替） |
| `grep -rn 'RibbonDetails\|Task Form\|Resource Form' modules/microproject_ui/src docs` | `RibbonDetails` はリボン定義・仕様 CSV・テストに存在。`Task Form` / `Resource Form` は文字列としても存在しない |
| `.\gradlew.bat :microproject_ui:test --console=plain` | BUILD SUCCESSFUL（2m 7s、exit 0、20 tasks: 2 executed / 18 up-to-date）。ドキュメントのみの変更なのでテストは再実行されず、既存結果の再利用 |
