# リボンUIコマンドボタン操作ユースケース

microProjectの標準リボンに表示される全コマンドボタンを、アプリウィンドウ内のマウス操作で1回以上実行するためのユースケース一覧です。対象コマンドは `RibbonCommandCatalog` と同じIDで管理し、リボンのタブ・バンド・表示順を変更した場合もID単位で追跡します。

## 共通前提

- `clean build installDist` 後の `modules/micrproject_ui/build/install/micrproject_ui` を起動する。
- microProjectを最大化し、証跡画像はmicroProjectウィンドウだけを含める。デスクトップ、タスクバー、他アプリ、OSファイルダイアログは採用しない。
- 破壊的な操作（閉じる、削除、切断、ベースライン消去、CCPM消去）は専用の一時プロジェクトで実施し、確認ダイアログのキャンセル／確定を両方記録する。
- ダイアログを開くコマンドは「表示されたこと」「主要入力が可能なこと」「キャンセルして元の画面へ戻れること」を合格条件とする。表示を切り替えるコマンドは選択状態と画面内容の変化を記録する。

## ユースケース一覧

### ファイル（FileRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonNewProject | 新規プロジェクトを作成 | 新規ドキュメントが表示される |
| RibbonOpenProject | 保存済みプロジェクトを開く | 選択したプロジェクト名・タスクが表示される |
| RibbonRecentProjects | 最近使ったプロジェクトを開く | 最近使った一覧から選択したプロジェクトへ切り替わる |
| RibbonImportProject | MSP等の外部プロジェクトをインポート | インポート結果が表示され、エラー時は説明が出る |
| RibbonLocale | 表示言語を切り替える | 選択した言語が反映される |
| RibbonProjectLibreDocumentation | ドキュメントを開く | ヘルプ操作が開始される（外部画面は証跡外） |
| RibbonAboutProjectLibre | バージョン情報を開く | Aboutダイアログが表示される |
| RibbonSaveProject | 現在のプロジェクトを保存 | 保存完了後に保存エラーが表示されない |
| RibbonSaveProjectAs | 名前を付けて保存 | 指定名で保存できる |
| RibbonCloseProject | 現在のプロジェクトを閉じる | 確認後にドキュメントが閉じる |
| RibbonExportProject | プロジェクトを外部形式へ出力 | 出力処理が完了し、元の画面へ戻れる |
| RibbonPrint | 印刷を実行 | 印刷設定または印刷処理が表示される |
| RibbonPrintPreview | 印刷プレビューを表示 | プレビューが表示される |
| RibbonPDF | PDF出力 | PDF出力処理が開始される |

### タスク（TaskRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonInsert | タスクを挿入 | 選択行の位置にタスクが追加される |
| RibbonInsertRecurring | 定期タスクを挿入 | 定期タスク設定ダイアログが表示される |
| RibbonInsertProject | サブプロジェクトを挿入 | サブプロジェクト選択・挿入が可能 |
| RibbonIndent / RibbonOutdent | タスク階層を上げ下げ | WBS階層と表示が更新される |
| RibbonMoveTaskUp / RibbonMoveTaskDown | タスク行を上下移動 | 行順と選択状態が更新される |
| RibbonExpand / RibbonCollapse | サマリーを展開／折りたたみ | 子タスクの表示状態が変わる |
| RibbonLink / RibbonUnlink | タスク間の先行関係を作成／解除 | 矢印と先行タスク列が更新される |
| RibbonAssignResources | リソースを割り当てる | 割当ダイアログとタスクのリソース表示が更新される |
| RibbonDelegateTasks | タスクを委任する | 委任操作または結果ダイアログが表示される |
| RibbonTaskInformation | タスク情報を開く | タスク情報ダイアログが表示される |
| RibbonNotes | タスクメモを編集 | メモを保存して再表示できる |
| RibbonUpdateTasks | タスクの進捗を更新 | 更新値がタスク表とバーへ反映される |
| RibbonDelete | 選択タスクを削除 | 確認後に対象行だけが削除される |
| RibbonCustomFields | ユーザー設定フィールドを開く | カスタムフィールド操作が可能 |
| RibbonFind | タスクを検索 | 検索結果へ移動できる |
| RibbonScrollToTask | 選択タスクへスクロール | 対象タスクが表示領域へ移動する |
| RibbonHideSelectedTasks / RibbonShowAllTasks | タスクを非表示／全表示 | 対象行の表示状態が切り替わる |
| RibbonPaste / RibbonCopy / RibbonCut | タスクをコピー・切り取り・貼り付け | 対象行だけが正しく複製／移動される |

### リソース（ResourceRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonInsertResource | リソースを追加 | リソース行が追加される |
| RibbonResourceInformation | リソース情報を開く | リソース情報ダイアログが表示される |
| RibbonTimesheet | タイムシートを表示 | タイムシート画面が表示される |
| RibbonTeamFilter | チームリソース表示を切り替える | 選択状態と一覧が切り替わる |
| RibbonLevelResources | リソースを平準化 | 平準化候補が表示され、プレビュー／取消が可能 |

### レポート（ReportRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonReport | レポート一覧を表示 | レポート画面が表示される |
| RibbonCustomReport | カスタムレポートを開く | カスタムレポート操作が可能 |
| RibbonHistogram | リソースヒストグラムを表示 | ヒストグラムが描画される |
| RibbonCharts | チャートを表示 | チャートが描画される |
| RibbonTaskUsage / RibbonResourceUsage | タスク／リソース使用状況を表示 | 選択した使用状況ビューが表示される |
| RibbonCCPMBufferStatus | CCPMバッファ状況を表示 | バッファ状況ダイアログが表示される |

### プロジェクト（ProjectRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonProjectInformation | プロジェクト情報を開く | プロジェクト情報ダイアログが表示される |
| RibbonProjectsDialog | プロジェクト一覧を開く | プロジェクト一覧から切替できる |
| RibbonChangeWorkingTime | 稼働時間を変更 | カレンダー設定を開いて保存／取消できる |
| RibbonCalendarOptions | カレンダーオプションを開く | オプションを表示できる |
| RibbonUpdateProject | プロジェクトを更新 | 更新結果がタスク表へ反映される |
| RibbonRecalculate | スケジュールを再計算 | 依存タスクとガントバーが再計算される |
| RibbonSaveBaseline / RibbonClearBaseline | ベースラインを保存／消去 | 保存または確認後の消去結果が表示される |
| RibbonCCPMSettings | CCPM設定を開く | CCPM設定ダイアログが表示される |
| RibbonCCPMClear | CCPM計画を消去 | 確認後にCCPM状態が消去される |

### 表示（ViewRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonGantt / RibbonTrackingGantt | ガント／トラッキングガントを表示 | 選択ビューが表示される |
| RibbonNetwork / RibbonWBS | ネットワーク／WBSを表示 | グラフまたはWBSが描画される |
| RibbonResources / RibbonRBS | リソース／RBSを表示 | 選択ビューが表示される |
| RibbonTimeline / RibbonCalendarView | タイムライン／カレンダーを表示 | 選択ビューが表示される |
| RibbonProjects | プロジェクトビューを表示 | プロジェクト一覧ビューが表示される |
| RibbonTaskUsageDetail / RibbonResourceUsageDetail | 詳細使用状況を表示 | 詳細ビューが表示される |
| RibbonNoTextNoSubWindow | サブウィンドウ表示を切り替える | 表示モードが切り替わる |
| RibbonArrangeAll | 全ウィンドウを整列 | 開いているプロジェクトが整列される |
| RibbonChooseFilter / RibbonChooseSort / RibbonChooseGroup | フィルター・ソート・グループを変更 | 選択条件が一覧へ反映される |
| RibbonZoomIn / RibbonZoomOut | 時間軸を拡大／縮小 | ガントの時間スケールが変わる |
| RibbonCCPMNetwork | CCPMネットワークを表示 | CCPMネットワークが描画される |

### 書式（FormatRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonToggleProgressLine | 進捗線を切り替える | 進捗線の表示状態が変わる |
| RibbonLabelResourceNames / RibbonLabelTaskName | リソース名／タスク名ラベルを切り替える | バーラベルが切り替わる |
| RibbonGridlines | グリッド線を切り替える | グリッド線の表示状態が変わる |
| RibbonToggleCriticalChain | クリティカルチェーン表示を切り替える | クリティカルチェーンの表示状態が変わる |
| RibbonTimescale | 時間軸を変更する | 選択した時間軸が反映される |
| RibbonBar / RibbonBarStyles | バー表示／バー書式を変更する | ガントバー表示が変わる |
| RibbonTextStyles | テキスト書式を変更する | 表示テキスト書式が変わる |
| RibbonLayout | レイアウトを変更する | レイアウト変更後も操作可能 |

### クイックアクセス（QuickAccessToolbar）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonTopBarSaveProject | クイックアクセスから保存 | 保存結果が通常の保存と一致する |
| RibbonTopBarUndo / RibbonTopBarRedo | クイックアクセスから元に戻す／やり直す | 直前操作が1回だけ戻る／再適用される |

## 実施記録

各IDについて、タブ名、ボタン表示名、前提プロジェクト、マウス操作、結果、表示ずれ・無反応・例外の有無、アプリ限定スクリーンショットをIssueに追記します。ボタンが無効な場合は、無効化条件（選択なし、読み取り専用、対象ビュー外など）も記録し、意図した状態かを確認します。

### 自動化検証（2026-08-30）

`RibbonTabGuiAcceptanceTest.robotClicksEveryStandardRibbonCommandOnce` は、実ウィンドウ上で標準リボンの各タブを Robot で選択し、全コマンドを実マウスクリックして Action dispatch が一回だけ発生することを確認する。分割ボタンは主コマンド領域をクリックし、1500px幅の実ウィンドウで全コマンド（`RibbonHideSelectedTasks` を含む）が直接表示されることも確認した。レスポンシブなオーバーフローは既存のリボン構造テストと個別の表示検証で追跡する。

900px幅でのオーバーフロー受入試行では、タスクタブ切替後に `RibbonHideSelectedTasks` を含むポップアップトリガーを取得できなかった。これは幅計算／密度切替の実GUI課題候補として Issue #430 に記載し、実アプリのウィンドウ幅で追加確認するまで未解決として扱う。
