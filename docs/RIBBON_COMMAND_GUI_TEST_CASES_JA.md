# リボンUIコマンドボタン操作ユースケース

microProjectの標準リボンに表示される全コマンドボタンを、アプリウィンドウ内のマウス操作で1回以上実行するためのユースケース一覧です。対象コマンドは `RibbonCommandCatalog` と同じIDで管理し、リボンのタブ・バンド・表示順を変更した場合もID単位で追跡します。

## 共通前提

- `clean build installDist` 後の `modules/microproject_ui/build/install/microproject_ui` を起動する。
- microProjectを最大化し、証跡画像はmicroProjectウィンドウだけを含める。デスクトップ、タスクバー、他アプリ、OSファイルダイアログは採用しない。
- 破壊的な操作（閉じる、削除、切断、ベースライン消去、CCPM消去）は専用の一時プロジェクトで実施し、確認ダイアログのキャンセル／確定を両方記録する。
- ダイアログを開くコマンドは「表示されたこと」「主要入力が可能なこと」「キャンセルして元の画面へ戻れること」を合格条件とする。表示を切り替えるコマンドは選択状態と画面内容の変化を記録する。

## ユースケース一覧

### ファイル（FileRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonNewProject | 新規プロジェクトを作成 | 新規ドキュメントが表示される |
| RibbonNewMasterProject | 新規マスタープロジェクトを作成 | ローカルのマスター文書が作成され、サブプロジェクトを挿入できる |
| RibbonOpenProject | 保存済みプロジェクトを開く | 選択したプロジェクト名・タスクが表示される |
| RibbonRecentProjects | 最近使ったプロジェクトを開く | 最近使った一覧から選択したプロジェクトへ切り替わる |
| RibbonBackstageProjectInformation | File > Info からプロジェクト情報を開く | 情報・統計ダイアログが表示され、戻ると同じプロジェクト画面が残る |
| RibbonBackstageOptions | File > Options を開く | アプリケーション設定が表示され、既存の設定保存経路を使える |
| RibbonImportProject | MSP等の外部プロジェクトをインポート | インポート結果が表示され、エラー時は説明が出る |
| RibbonLocale | 表示言語を切り替える | 選択した言語が反映される |
| RibbonMicroProjectDocumentation | ドキュメントを開く | ヘルプ操作が開始される（外部画面は証跡外） |
| RibbonAboutMicroProject | バージョン情報を開く | Aboutダイアログが表示される |
| RibbonSaveProject | 現在のプロジェクトを保存 | 保存完了後に保存エラーが表示されない |
| RibbonSaveProjectAs | 名前を付けて保存 | 指定名で保存できる |
| RibbonSaveMpoAs | MPOとして保存 | マスターをMPOアーカイブとして保存できる |
| RibbonCloseProject | 現在のプロジェクトを閉じる | 確認後にドキュメントが閉じる |
| RibbonExportProject | プロジェクトを外部形式へ出力 | 出力処理が完了し、元の画面へ戻れる |
| RibbonPrint | 印刷を実行 | 印刷設定または印刷処理が表示される |
| RibbonPrintPreview | 印刷プレビューを表示 | プレビューが表示される |
| RibbonPDF | PDF出力 | PDF出力処理が開始される |

### タスク（TaskRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonInsert | タスクを挿入 | 選択行の位置にタスクが追加される |
| RibbonTaskInspector | Task > Inspect Task を選ぶ | 選択タスクのスケジュール要因を左ペインに表示する。先行タスクへの移動は表示中の行だけを選択し、フィルター等で隠れた場合は理由を表示して選択・モデル・Undo状態を変えない。閉じる／別タスク選択後も読み取り専用を保つ。明示的な自動スケジュール操作は既存の Task Mode/Undo 経路を使う。 |
| RibbonInsertRecurring | 定期タスクを挿入 | 定期タスク設定ダイアログが表示される |
| RibbonInsertProject | サブプロジェクトを挿入 | サブプロジェクト選択・挿入が可能 |
| RibbonIndent / RibbonOutdent | タスク階層を上げ下げ | WBS階層と表示が更新される |
| RibbonMoveTaskUp / RibbonMoveTaskDown | タスク行を上下移動 | 行順と選択状態が更新される |
| RibbonExpand / RibbonCollapse | サマリーを展開／折りたたみ | 子タスクの表示状態が変わる |
| RibbonLink / RibbonUnlink | タスク間の先行関係を作成／解除 | 矢印と先行タスク列が更新される |
| RibbonRespectLinks | 先行リンクと競合する手動スケジュールのタスクを選び、Task > Schedule の「リンクを反映」を実行する | 選択タスクの表示日程とガントバーがリンク制約に沿って変わり、手動モードと他タスクを維持する。対象外タスクでは操作が無効、制約を満たせない場合は変更なしで理由を表示する。Undo/RedoとMPO保存再読込で日程を確認する。根拠: [Microsoftのタスクリンク定義](https://support.microsoft.com/en-us/project/link-tasks-in-a-project)。 |
| RibbonInactivateTask | Task > Tracking の「非アクティブ化」を選択タスクに実行する | Active フィールドが false になり、タスク行・依存関係・進捗は保持される。タスク行とガント投影に残り、Undo/RedoとMPO保存再読込で状態を確認する。非アクティブな選択だけなら無効。根拠: [Microsoft Active task field](https://support.microsoft.com/en-us/project/active-task-field)。 |
| RibbonInsertMilestone | Task > Insert の「マイルストーン」を選択タスク行に実行する | 選択行の上に期間 0 のタスクが 1 件挿入され、Undo/Redo は 1 回で元に戻る。MPO 保存再読込後もマイルストーンのまま。読み取り専用では無効。根拠: [Microsoft Add a milestone](https://support.microsoft.com/en-us/project/add-a-milestone-to-your-project)。 |
| RibbonInsertSummary | 連続したタスク行を選び、Task > Insert の「要約タスク」を実行する | 選択行の上に要約タスクが挿入され、選択タスクがその下位に設定される。Undo/Redo は 1 回で一括して戻る。読み取り専用・未選択では無効。根拠: [Microsoft Create a summary task](https://support.microsoft.com/en-us/project/create-a-summary-task)。 |
| RibbonClear | Task > Editing の「クリア」を選択セルに実行する | 選択セルの内容だけが消え、行は削除されない。Ctrl+Delete と同一の ClearContents Action。 |
| RibbonSelectAll | Task > Editing の「すべて選択」を実行する | 作業中の表の全行・全列が選択される。Ctrl+Shift+Space と同一の Action。 |
| RibbonTaskDetails | Task > Properties の「詳細」を実行する | View の「詳細」と同一の Action で詳細ペインを表示／非表示し、両ボタンの選択状態が一致する。 |
| RibbonPercentComplete0 / RibbonPercentComplete25 / RibbonPercentComplete50 / RibbonPercentComplete75 / RibbonPercentComplete100 | Task > Schedule の達成率ボタンを選択タスクに実行する | 選択した編集可能タスクの達成率が 0/25/50/75/100% になり、要約タスクは変更されない。Undo/Redo は 1 回で戻り、MPO 保存再読込で保持される。未選択・読み取り専用では無効。根拠: [Microsoft Update progress using percent complete](https://support.microsoft.com/en-us/project/update-progress-using-percent-complete)。 |
| RibbonAssignResources | リソースを割り当てる | 割当ダイアログとタスクのリソース表示が更新される |
| RibbonDelegateTasks | タスクを委任する | 委任操作または結果ダイアログが表示される |
| RibbonTaskInformation | タスク情報を開く | タスク情報ダイアログが表示される |
| RibbonFormatPainter | 1つの編集可能なタスクを選び、書式のコピーを1回押して別タスクを選択する。連続適用はボタンを2回押し、Escapeで解除する | 6つのタスク文字書式（フォント、サイズ、色、太字、斜体、取り消し線）だけを適用し、タスクデータやガントバーの個別書式を変えない。複数選択・読み取り専用・ロック対象は拒否する。Undo/RedoとMPO保存再読込を確認する。Microsoftの説明は1回適用、ダブルクリックで連続適用、Escapeで終了を定義するがタスクの個別書式項目は列挙しないため、6項目の範囲はdocument-derived product contractとする。根拠: [Microsoft Format Painter](https://support.microsoft.com/en-us/word/use-the-format-painter)。 |
| RibbonTaskViewGantt | Task > 表示 のガントチャートギャラリーを開き、ビューを選ぶ | 選択したガント系ビューへ切り替わる。既存のビュー切替アクションと同一。 |
| RibbonBold | タスクを選択して Task > フォントの「太字」を押す | 選択タスクのテキストが太字になりトグル状態が反映される。Undo/Redoで戻る。 |
| RibbonItalic | タスクを選択して Task > フォントの「斜体」を押す | 選択タスクのテキストが斜体になりトグル状態が反映される。Undo/Redoで戻る。 |
| RibbonFontName | タスクを選択して Task > フォントのフォント名コンボからフォントを選ぶ | 選択タスクのフォントが変わりコンボに現在値が表示される。Undo/Redoで1回で戻り、MPO保存再読込で保持される。 |
| RibbonFontSize | タスクを選択して Task > フォントのフォント サイズコンボから値を選ぶ | 選択タスクの文字サイズが変わる。Undo/Redoで1回で戻り、MPO保存再読込で保持される。 |
| RibbonUnderline | タスクを選択して Task > フォントの「下線」を押す | 選択タスクのテキストに下線が引かれトグル状態が反映される。Undo/Redoで戻り、MPO保存再読込で保持される。 |
| RibbonFillColor | タスクを選択して Task > フォントの「背景色」を押し色を選ぶ | 選択タスクのセルの背景色が変わる。キャンセルでは変更されない。Undo/Redoで戻り、MPO保存再読込で保持される。 |
| RibbonMoveTask | タスクを選択して Task > タスクの「移動」から前後の日数を選ぶ | 手動スケジュールのタスクは期間を保ったまま開始日が移動し、自動スケジュールのタスクには開始日の制約（Start No Earlier Than）が付く。集約タスクと非アクティブタスクは対象外。「ユーザー設定」は稼働日数を入力する。Undo/Redoで戻る。 |
| RibbonScheduleMode | タスクを選択して Task > タスクの「モード」から手動/自動スケジュールを選ぶ | RibbonTaskModeManual / RibbonTaskModeAutomatic と同じ動作でタスクモードが変わる。 |
| RibbonAddToTimeline | タスクを選択して Task > プロパティの「タイムラインに追加」を押す | 選択タスクがタイムラインの対象になり（全て対象済みなら解除）、トグル状態が反映される。Undo/Redoで戻る。 |
| RibbonFontColor | タスクを選択して「フォントの色」を押し色を選ぶ | 選択タスクのテキスト色が変わる。キャンセルでは変更されない。Undo/Redoで戻る。 |
| RibbonFont | Task > フォントの「フォント...」を押す | テキストスタイルダイアログが開く。 |
| RibbonFill | セル範囲を選択して Task > 編集の「フィル」から方向を選ぶ | 選択範囲の先頭（上/左方向は末尾）のセル内容が方向に沿って編集可能セルへコピーされる。Undo/Redoで戻る。RibbonFillDown/RibbonFillUp/RibbonFillLeft/RibbonFillRight。 |
| RibbonResourceViews | Resource > 表示 のギャラリーからリソース系ビューを選ぶ | 選択したリソース系ビューへ切り替わる。 |
| RibbonResourceNotes | リソースを選択して Resource > プロパティの「メモ」を押す | リソースのメモ表示/編集が開く。 |
| RibbonResourceDetails | Resource > プロパティの「詳細」を押す | 詳細ペインが切り替わりトグル状態が反映される。 |
| RibbonNotes | タスクメモを編集 | メモを保存して再表示できる |
| RibbonUpdateTasks | タスクの進捗を更新 | 更新値がタスク表とバーへ反映される |
| RibbonDelete | 選択タスクを削除 | 確認後に対象行だけが削除される |
| RibbonTaskModeManual / RibbonTaskModeAutomatic | 手動／自動スケジュールへ切り替え | 選択タスクのスケジュールモードが変わり、Undo/Redoと保存再読込で保持される |
| RibbonCustomFields | ユーザー設定フィールドを開く | カスタムフィールド操作が可能 |
| RibbonFind | タスクを検索 | 検索結果へ移動できる |
| RibbonScrollToTask | 選択タスクへスクロール | 対象タスクが表示領域へ移動する |
| RibbonScrollToToday | 今日へスクロール（製品独自） | ガントの時間軸で現在の日付が表示領域の中央に来る。選択とタスクの日付は変わらない |
| RibbonHideSelectedTasks / RibbonShowAllTasks | タスクを非表示／全表示 | 対象行の表示状態が切り替わる |
| RibbonPrivacyMask | プライバシー表示 | タスク名・リソース名・プロジェクト名を表示上だけ匿名化し、再実行で復元する |
| RibbonPaste / RibbonCopy / RibbonCut | タスクをコピー・切り取り・貼り付け | 対象行だけが正しく複製／移動される |

### リソース（ResourceRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonInsertResource | リソースを追加 | リソース行が追加される |
| RibbonResourceInformation | リソース情報を開く | リソース情報ダイアログが表示される |
| RibbonTimesheet | タイムシートを表示 | タイムシート画面が表示される |
| RibbonTeamFilter | チームリソース表示を切り替える | 選択状態と一覧が切り替わる |
| RibbonLevelSelection | 選択したタスクを平準化 | 選択行だけに平準化を適用し、非選択行を固定条件として保ち、Undo/Redo とMPO再読込で状態を確認する |
| RibbonLevelAll | プロジェクト全体を平準化 | タスク選択を必要とせず、全リソースの対象タスクに適用する。現在の選択表示を保持し、1回のUndo/RedoとMPO再読込で全対象の結果を確認する。ボタン自体は押下状態を保持しない |
| RibbonClearLevelSelection | 選択タスクの平準化を解除 | 確認後に選択行の平準化遅延と自動分割だけを解除する。手動分割、他タスク、選択表示を保持し、Undo/Redo とMPO保存・再読込を確認する |
| RibbonClearLevelAll | 全タスクの平準化を解除 | 確認後にプロジェクト全体の平準化遅延と自動分割だけを解除する。手動分割を保持し、Undo/Redo とMPO保存・再読込を確認する |
| RibbonLevelResources | リソースを平準化 | 平準化候補が表示され、プレビュー／取消が可能 |
| RibbonNextOverallocation | 次の過割当へ移動 | Microsoft公式の「次の過負荷リソースを持つタスクへ移動」を根拠とし、文書導出の表示行順で現在行の次を選択する。フィルター等で非表示の行は飛ばし、末尾では先頭へ巡回する。候補ゼロは説明を表示。選択/表示のみでモデル変更・Undo・保存対象変更は行わない。実Robotで次候補、巡回、候補なし、日程不変を検証する |
| RibbonUseResourcePool | 共有リソースプールを使用 | 保存済みで開いているプールを選択し、競合時の優先元を選択して共有できる |
| RibbonCreateResourcePool | リソースプールを作成 | 独立したリソースプール文書が作成され、保存後に共有元として選択できる |
| RibbonRefreshResourcePool | 共有リソースプールを更新 | 参照先プールを開いた状態で、未保存変更を失わずに再解決できる |

### レポート（ReportRibbonTask）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonReportDashboards / RibbonReportResources / RibbonReportInProgress / RibbonReportCustom | レポートギャラリーから既存レポートを開く | 選んだレポートが表示される |
| RibbonReportRecent | 今セッションで開いたレポートを再表示 | 直近のレポートが開く |
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
| RibbonStatusDate | 状況報告日を設定 | 状況報告日が設定され、Undo/Redoで復元される |
| RibbonMarkOnTrack | 選択タスクを予定どおりに更新 | 状況報告日に基づく進捗へ更新され、Undo/Redoで復元される |
| RibbonMoveProject | プロジェクト全体を移動 | 新しい開始日に合わせてタスクとガントバーが移動し、Undo／Redo と保存／再読込後も結果が保たれる |
| RibbonRecalculate | スケジュールを再計算 | 依存タスクとガントバーが再計算される |
| RibbonRefreshSubprojects | マスターのサブプロジェクトを更新 | 各リンクが安全に再読み込みされ、未保存の子プロジェクトでは保存・破棄・キャンセルを選べる |
| RibbonOpenSubproject | 選択したサブプロジェクトを開く | 選択したリンク先を正規化された既存ウィンドウで開き、既に開いている場合はそのウィンドウを前面に表示する |
| RibbonRemoveSubproject | 選択したサブプロジェクトを削除する | マスターからのリンクのみを安全に削除し、子プロジェクトのファイルおよび独立した文書は変更しない |
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
| RibbonDetails | 詳細ペインを表示／非表示 | 下部ビューの表示状態が切り替わる |
| RibbonArrangeAll | 全ウィンドウを整列 | 開いているプロジェクトが整列される |
| RibbonSwitchWindows | 開いているプロジェクトの一覧から切り替え | 選んだプロジェクトのウィンドウが前面になる |
| RibbonTeamPlanner | チームプランナーを表示 | チームプランナー画面が表示される |
| RibbonTimescaleGallery | 時間スケールを一覧から選択 | ガントの時間スケールが変わる |
| RibbonChooseFilter / RibbonChooseSort / RibbonChooseGroup | フィルター・ソート・グループを変更 | 選択条件が一覧へ反映される |
| RibbonZoomIn / RibbonZoomOut | 時間軸を拡大／縮小 | ガントの時間スケールが変わる |
| RibbonZoomEntireProject | Gantt / Task Usage / Resource Usage で View > ズーム の「プロジェクト全体」を押す | タイムスケールがプロジェクトの開始から終了までを表示できる縮尺に変わる。データは変更されない。 |
| RibbonZoomSelectedTasks | Gantt / Tracking Gantt / Task Usage / Resource Usage で選択タスクにズーム | 選択タスクの日付範囲が可視範囲に収まり、タスク日付は変わらない。日付付き選択がない場合は無効。 |
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
| RibbonInsertColumn | 選択列の前に列を挿入する | 選んだフィールドの列が挿入される |
| RibbonAlignLeft / RibbonAlignCenter / RibbonAlignRight | 選択列の配置を左／中央／右にする | 列の文字配置が変わり、保存して開き直しても保たれる |
| RibbonColumnSettings (RibbonHideColumn / RibbonFieldSettings) | 列を非表示にする／フィールドの設定を開く | 列が非表示になる／配置と見出し折り返しを変更できる |
| RibbonCustomFields | カスタムフィールドを開く（列グループ） | カスタムフィールド操作が可能 |
| RibbonFormatBarStyles (RibbonFormatBar / RibbonBarStylesItem) | 選択タスクのバー書式／バー スタイルを開く | 各ダイアログが開き、結果がガントに反映される |
| RibbonCriticalTasks | クリティカル タスクの強調を切り替える | クリティカル タスクのバー色が切り替わる |
| RibbonSlack | スラック表示を切り替える | タスク後ろのスラックバーが表示／非表示になる |
| RibbonLateTasks | 遅れているタスクの強調を切り替える | 遅れているタスクのバー色が切り替わる |
| RibbonTaskPath (RibbonPathPredecessors / RibbonPathDrivingPredecessors / RibbonPathSuccessors / RibbonPathDrivenSuccessors / RibbonPathRemove) | 選択タスクの先行／後続パスを強調する | 種類ごとの色でバーが強調され、選択変更に追従し、解除で元に戻る |
| RibbonShowBaseline | ベースライン バーの表示を切り替える | ベースライン バーが表示／非表示になる |
| RibbonSlippage | スリップ線の表示を切り替える | ベースライン開始から現在の開始までの線が表示／非表示になる |
| RibbonGanttStyleGallery (RibbonGanttStyleDefault / Blue / Green / Orange / Purple / Gray) | ガント チャートの配色を選ぶ | バーの配色が切り替わり、ビューと共に保存される |
| RibbonOutlineNumbers | アウトライン番号の表示を切り替える | タスク名の前に番号が表示／非表示になる |
| RibbonSummaryTasks | サマリー タスクの行を切り替える | サマリー タスクの行が表示／非表示になる |

### クイックアクセス（QuickAccessToolbar）

| ID | 操作ユースケース | 合格条件 |
|---|---|---|
| RibbonTopBarSaveProject | クイックアクセスから保存 | 保存結果が通常の保存と一致する |
| RibbonTopBarUndo / RibbonTopBarRedo | クイックアクセスから元に戻す／やり直す | 直前操作が1回だけ戻る／再適用される |

## 実施記録

各IDについて、タブ名、ボタン表示名、前提プロジェクト、マウス操作、結果、表示ずれ・無反応・例外の有無、アプリ限定スクリーンショットをIssueに追記します。ボタンが無効な場合は、無効化条件（選択なし、読み取り専用、対象ビュー外など）も記録し、意図した状態かを確認します。

### 自動化検証（2026-08-30）

`RibbonTabGuiAcceptanceTest.robotClicksEveryStandardRibbonCommandOnce` は、実ウィンドウ上で標準リボンの各タブを Robot で選択し、全コマンドの物理的な到達性と一回の Action dispatch をスモーク確認する。これはコマンドの成功証拠ではない。各選択依存・モデル変更・表示更新・Undo/Redo・保存再読込の合否は、コマンド契約テストと対象別Robot受入テストで検証する。分割ボタンは主コマンド領域をクリックし、1500px幅の実ウィンドウで全コマンド（`RibbonHideSelectedTasks` を含む）が直接表示されることも確認した。レスポンシブなオーバーフローは既存のリボン構造テストと個別の表示検証で追跡する。

900px幅でのオーバーフロー受入試行では、タスクタブ切替後に `RibbonHideSelectedTasks` を含むポップアップトリガーを取得できなかった。これは幅計算／密度切替の実GUI課題候補として Issue #430 に記載し、実アプリのウィンドウ幅で追加確認するまで未解決として扱う。
