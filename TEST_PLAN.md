# ProjectLibre テストプラン

## 1. テスト目的・背景

このリポジトリは ProjectLibre デスクトップアプリの開発フォークで、主な品質保証対象は以下です。

- プロジェクト計画データの読込/保存/再読込が破損なく行えること
- `MPP / XML / MPX / Planner / XLSX / POD` の交換経路と形式別の保証範囲。MPP は現時点で import の部分実装のみを確認しており、MPP export は未実装として追跡する（[形式別監査](docs/MSP_STANDARD_COMMAND_INVENTORY.md#file-format-coverage-at-current-master)）
- 共同編集用 sidecar メタデータ、ロック、外部変更検知が競合なく動作すること
- タスク、依存関係、カレンダー、工数、進捗、リソース、レポート、Gantt 表示が境界値でも正しいこと
- Swing UI 入力、IME、日付入力、Gantt/Report/Image 出力が実運用に耐えること
- JDK 25 / Gradle / Windows packaging 前提でビルド成果物が壊れないこと

重点は正常系確認ではなく、ファイル破損、外部更新競合、日付境界、ロック期限、巨大データ、未対応拡張子、UI イベント順序、並行更新で潜む不具合を露出させることです。

## 1.1 テスト層とケース選定

テスト数や「30ケース／100ケース」という件数自体を品質目標にしません。各ケースは、固有の状態遷移・境界・失敗条件を一つ検証し、同じ責務を別経路で繰り返す場合は、物理経路を証明する一つの受入テストと共有不変条件のテストに分けます。全体の棚卸しと整理は [#773](https://github.com/tetsuji16/ProjectLibre/issues/773) で追跡します。

| 層 | 主な配置 | 検証するもの | 選定ルール |
|---|---|---|---|
| ドメイン単体 | module の `src/test` | 純粋な計算、モデル遷移、不変条件、Undo/Redo | ヘッドレスで決定的に実行し、出力文字列や内部フィールドの存在確認で合格にしない |
| 交換・永続化統合 | `microproject_exchange/src/test` 等 | 実ファイルの読込、書込、再読込 | MPOは過去バージョンが生成したファイルの読込を維持する。PODはシリアライズ形式を固定し、代表fixtureで形式不変とround-tripを確認する |
| Swingコンポーネント | `src/test` | EDT上のコンポーネント状態、レンダリングモデル、レイアウト計算 | ネイティブ画面を必要としないこと。headless assumptionで常時skipする画面テストは `guiTest` に移す |
| 物理GUI受入 | `microproject_ui/src/guiTest` | Robotによる実画面経路、モデル・表示・Undo/Redo・保存 | 変更されたコマンドファミリーで共有する一つのユーザー行程を使い、`doClick`や可視確認だけで代用しない |
| 視覚・環境マトリクス | 共通GUI受入fixtureと画像証跡 | レイアウト、DPI、locale、画面状態 | 影響する制約だけを組み合わせ、全組み合わせは共通UI変更と定期／リリース監査に限定する |

JUnitの失敗時診断はレポートに残し、成功時の大量標準出力でテスト結果を埋めない。`src/test` にあるテストがネイティブUI前提なら、その前提が本当に必要か確認し、必要なら既存GUI旅程へ統合します。手動診断は実行可能なテストと混ぜず、残す場合は用途と起動方法を文書化します。

旧アプリ内部実装との後方互換は原則として要件にしません。MPOは既存ユーザーファイルを読み込めることを後方互換要件として維持し、バージョン付きfixtureで検証します。PODは独立した固定フォーマット境界であり、フィールド、順序、識別子、バージョンその他のシリアライズ構造を変更してはいけません。既存POD fixtureはこの形式不変と読込・round-tripの回帰確認に使い、旧アプリ内部APIの互換性を広げる目的には使いません。MPP/MPX/XML/XLSXは対応する外部形式との現行交換契約をテストし、旧microProjectアプリの内部動作との互換性とは区別します。sidecarや永続化enumは現行のサポート範囲を個別に定義し、MPO/PODの要件から自動的に後方互換対象へ広げません。

## 1.2 現行テストゲートに必要な代表契約

詳細な実行範囲は [beta-test-strategy](docs/testing/beta-test-strategy.md)、リリース条件は [beta-release-plan](docs/beta-release-plan.md) に記載する。これらは既存の beta 配布手順を説明する文書であり、互換性の達成証拠ではない。全 unit/headless の境界回帰は保持し、GUI は共有旅程を選ぶ。

MS Project 互換性に関する形式テストでは read と write を独立して扱う。現在の MPP reader fixture が開ける事実は部分的な読込経路の証拠に限る。完全な MPP 互換性には MPP writer が生成した実バイナリの schema/構造検査と、Project desktop で定義したフィールド・関係の round-trip fixture が必要である。XML/MSPDI の export を `.mpp` と命名することは合格条件にしない。

| 契約 | 回帰の配置 |
| --- | --- |
| タスク追加→物理名前入力→MPO reload、削除→Undo/Redo | TaskInformationRibbonGuiAcceptanceTest の既存 Insert/Delete 旅程 |
| Spreadsheet view cleanup→選択モデル listener detach | SpreadSheetCornerLifecycleTest: row/column selection modelsからCorner listenerが外れ、cleanup後の選択変更でcallbackされない |
| Gantt UI replacement→GraphInteractor detach | GraphInteractorLifecycleTest: `updateUI()` 反復後も現行 interactor のみが mouse/motion listener として残り、cleanup後は0件 |
| #414 Gantt wheel ownership | TaskTableGanttGridGuiAcceptanceTest.physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep: 物理plain/Shift/Ctrl+wheelが縦/横/zoomの意図した状態だけを各1段変更し、表との縦同期を維持 |
| #414 Gantt drag auto-scroll | GanttBarDateDragGuiAcceptanceTest.robotDragToChartEdgeAutoScrollsAndPersistsMovedTaskDate: 物理バー移動で表示端までドラッグし横スクロール、描画中のタスクバーと日付/model、Undo/Redo、MPO reloadを確認 |
| Offline print/export completion→renderer/cache detach | NetworkSVGRendererLifecycleTest, ExportJobSchedulerTest: renderer listener解除、filtered cache close、safe print copy、chooser/job cancelを確認 |
| Task row-header popup の物理経路 | TaskTableGanttGridGuiAcceptanceTest.physicalRowHeaderPopupSelectsTheClickedTaskAndOpensOnce: 通常 JTable UI delegate、未選択行、task ID、全行選択、popup対象行とinvoker、popup 1回を Robot で確認 |
| Usage Detail の左右ビュー生成 | TaskInformationRibbonGuiAcceptanceTest.usageDetailViewRouteConstructsWithoutRuntimeFailure: 割当済み1日タスクからTask/Resource Usageの左右モデルと行を確認 |
| Timesheet 初期化とキャンセル | TaskInformationRibbonGuiAcceptanceTest.timesheetRouteConstructsItsDedicatedSpreadsheet: 実割当行、モデル作業量不変、Escape終了、既存ビューとUndo状態維持をRobot確認 |
| Resource Pool 作成と初期ビュー | ResourcePoolInitialViewGuiAcceptanceTest.physicalCreateResourcePoolCommandOpensUsableResourceView: 物理Resource ribbonコマンドから作成しResourceView/Sheetを確認（現GUI実行は未通過、後述の環境・表示問題を切り分け中） |
| 表の期間・進捗・日付、階層・依存、バー変更 | U26、TaskTableGanttGrid、TaskInformationRibbon、GanttBarDateDrag の smoke |
| CCPM Configure/Apply→Undo/Redo→MPO→network/buffer | CriticalChainStatusDialogGuiAcceptanceTest の統合旅程 |
| 空行を挟むタスクの ID と予定を MPO 保存・再読込で保持 | MpoFileImporterTest の既存 native schedule snapshot 契約を強化 |
| CCPM Clear の Undo が監視履歴と撤回記録を完全復元 | CriticalChainServiceTest の既存 clearUndoRedo 契約 |
| 子 modal を閉じた後のモデルレス親への Escape | AssignmentDialogGuiAcceptanceTest。active window と focus owner の復帰を先に検証 |
| ベータ起動が安定版 feed を取得せず、ファイル引数を保持 | BootstrapUpdateTest の loopback feed hit=0 契約、AppImage launcher config 検査 |
| ダイアログの実際の重なり・文字欠け | 既存共有 assertion。FlatLaf JSpinner の縦 stepper 同士が共有する1px境界だけ除外 |

## 2. 対象コンポーネント / 関数一覧

| 領域 | 主な対象 |
|---|---|
| ファイル種別判定 | `FileHelper.isFileNameAllowed`, `isMicrosoftProjectFile`, `getFileExtension`, `getFileType` |
| Microsoft/ProjectLibre 交換 | `MspImporter.importProject`, `parseProject`, `normalizeExtension`, `MicrosoftImporter.saveProject`, `exportFile`, `loadProject` |
| XLSX / POD ラウンドトリップ | `ProjectLibreXlsxReader/Writer`, `ProjectMergeService.loadExternalProject` |
| 共同編集メタデータ | `CollaborationMetadataStore.load`, `mutate`, `withLockedMetadata`, JSON parser/writer |
| ロック管理 | `TaskLockManager.acquire`, `release`, `releaseAll`, `renewAll`, `describeOwner` |
| 外部変更検知 | `CollaborationSession.start/stop`, `poll`, `checkBeforeSave`, `afterSave`, `saveWorkspace`, `loadWorkspace` |
| マージ/競合 | `ProjectMergeService.findTaskConflicts`, `findDeletedTasks`, `applyExternalTaskUpdates`, `TaskState.matches` |
| スケジューリング/依存関係 | `SchedulingType`, `FixedUnits`, `FixedDuration`, `FixedWork`, `DependencyType`, `DependencyFormat`, `CriticalPath` |
| カレンダー | `CalendarService`, `CalendarDefinition.add/compare/adjustInsideCalendar`, `WorkWeek`, `WorkRange`, `WorkingCalendar` |
| UI 入力 | `YearlessDateInputParser.parse`, `CommonSpreadSheet.processKeyEvent`, `processInputMethodEvent`, date editor selection |
| Gantt/Report/Image | `GanttRenderer.progressRatioForSchedule`, `ReportViewer.clampZoomRatio`, `ImageExport.appendPdfExtensionIfMissing/export` |
| Gradle/Packaging | root `build.gradle.kts`, module `test`, `stageAppDist`, `verifyPackagedFileImports`, `packageWindows*` |

## 3. 網羅的なテストケース

### ファイル判定 / Import / Export

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| F-01 | 正常 | `plan.pod`, `plan.xml`, `plan.xlsx` | save/open 両方で `FileHelper` 判定 | 保存許可、適切な file type |
| F-02 | 正常 | `plan.mpp`, `plan.mpx`, `plan.planner` | open 判定 | 読込許可、保存は `mpp/mpx/planner` 不許可 |
| F-03 | 境界 | 大文字拡張子 `PLAN.XLSX`, 混在 `Plan.MpP` | 判定 | 小文字化され正しく認識 |
| F-04 | 異常 | `null`, 空文字, `file.`, `.pod`, 拡張子なし | 各 FileHelper API 呼出 | NPE なし。仕様上未許可または既定拡張子付与 |
| F-05 | 異常 | 未対応拡張子 `.csv`, `.txt`, `.xls` | import/export | 明示的失敗、または file type `0` |
| F-06 | 正常・部分カバレッジ | 実 `modules/microproject_exchange/testdata/New Product.mpp` | `XlsxSupportTest.mspImporterCanReadMpp` → `MspImporter.importProject` | project が非 null で task が 1 件以上。resource/calendar の完全性・他 Project 版や高度なフィールドの意味互換性は未検証 |
| F-06a | 経路 | MPXJ が生成する MPX stream、extension=`mpx` | `MspImporter.importProject(stream, "mpx", ...)` (`MpxStreamImportTest`) | MPX reader 経路から名前付き task を取り込む。実在する旧版 MPX fixture による製品間互換性は別途監査する |
| F-07 | 正常 | MPXJ 生成 XLSX | import | project 非 null、root summary 除外、子タスク保持 |
| F-08 | 境界 | `.xlsx` 拡張子だが中身は XML | `normalizeExtension` | `xml` として読込 |
| F-09 | 異常 | 空 XLSX, 壊れた ZIP, 途中切断 stream | import | 例外が握り潰されず、UI/job に失敗が伝播 |
| F-10 | 境界 | BOM 付き XML / 先頭空白 XML | `.xlsx` XML fallback | XML として扱えること |
| F-11 | 正常 | POD -> XLSX -> 再読込 | `MicrosoftImporter.saveProject` | task count, name, duration, percent complete が一致 |
| F-12 | 境界 | 0 タスク、1 タスク、10,000 タスク | import/export | OOM なし、時間上限内、件数一致 |
| F-13 | 境界 | duration 0, milestone, multi-day, percent 0/100 | round trip | duration/progress/milestone が劣化しない |
| F-14 | 異常 | circular dependency を含むファイル | import | `CircularDependencyException` 相当で失敗、partial project を残さない |
| F-15 | 異常 | 書込不可ディレクトリ/既存 read-only file | export | 例外、元ファイルを削除しない |
| F-16 | 状態 | 既存ファイル export の temp rename | `exportFile` | 失敗時に元ファイル保持、成功時だけ置換 |
| F-17 | 回帰 | 既存 `.pod` を別名 `.pod` に Save As | UI と `LocalSession` の両方で実行し、15 秒以内の完了を待機 | UI が応答を維持し、完了処理は EDT 上で実行される。元／保存先とも再読込可能で、保存先が現在のファイル名になる |

### 共同編集 / ロック / メタデータ

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| C-01 | 正常 | `.mpo` / `.MPO` | `isMpoCollaborationCandidate` | true |
| C-02 | 正常 | `.pod/.podx/.xml/.xlsx/.mpp/.mpx/.planner` | 同上 | false |
| C-03 | 境界 | project path に拡張子なし、親なし相対パス | `buildSidecarFile` | `<base>.projectlibre-sync.json` を生成 |
| C-04 | 異常 | `CollaborationSession.create(null/非候補/null file)` | create | null を返し sidecar を作らない |
| C-05 | 正常 | 新規 sidecar | `load/mutate` | schema/user/locks/workspace が初期化 |
| C-06 | 異常 | 壊れた JSON, 配列 JSON, 巨大 JSON >1MB | `load` | RuntimeException。破損したメタデータを既定値で上書きせず、silent success しない |
| C-07 | 境界 | JSON に制御文字、引用符、Unicode user | save/load | エスケープ復元一致 |
| C-08 | 並行 | 2 セッション同時 `mutate` | 各 session から 1 user を同時追加 | JSON が壊れず両更新が保持 |
| C-09 | 正常 | alice が task 1 acquire | `TaskLockManager.acquire` | true、sidecar に owner/lease/user |
| C-10 | 異常 | bob が同一 task acquire | acquire | false、owner は alice のまま |
| C-11 | 境界 | lease 期限切れ lock | bob acquire | cleanup 後 true |
| C-12 | 状態 | same user stale lock | `poll` | 外部変更警告なし |
| C-13 | 状態 | other user metadata change | `poll` | `externalChangePending=true`, 警告 1 回 |
| C-14 | 状態 | project file mtime/length 変更のみ | `poll` | pending は true、即警告しない |
| C-15 | 状態 | 変更検知後 1.5s 安定 | reload handler 設定 | EDT で reload 1 回 |
| C-16 | 状態 | 保存前に locked task 外部変更 | `checkBeforeSave` | conflict 検出、選択結果に応じ `SAVE_*` |
| C-17 | 正常 | `afterSave` | 保存後 | baseline 更新、pending/warned 解除 |
| C-18 | 正常 | `saveWorkspace/loadWorkspace` | serializable workspace | Base64 payload 復元一致 |
| C-19 | 異常 | workspace payload が壊れた Base64/非互換 class | load | null、クラッシュなし |
| C-20 | 並行 | UI thread release と timer renew が競合 | acquire/release/renewAll 反復 | `localLocks` と sidecar に不整合なし |
| C-21 | 旧形式 | schemaVersion を持たない sidecar と既知 user | `load` | schemaVersion 1 へ補完し、既存 fingerprint/user を保持して保存 |

### マージ / 競合検出

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| M-01 | 正常 | locked baseline と外部同一 task | `findTaskConflicts` | conflict なし |
| M-02 | 正常 | locked task の name 変更 | 同上 | `changedTaskIds` に uniqueId |
| M-03 | 正常 | locked task 削除 | 同上 | `deletedTaskIds` に uniqueId |
| M-04 | 境界 | uniqueId 不一致だが task id 一致 | fallback 検索 | id fallback で変更検出 |
| M-05 | 境界 | notes/predecessors/resource/duration/start/end/outline のみ変更 | 個別比較 | 各フィールド差分で conflict |
| M-06 | 異常 | external file 読込不能 | conflict/apply | 空結果、例外ログ確認。保存を誤許可しない設計検討 |
| M-07 | 正常 | unlocked task 外部変更 | `applyExternalTaskUpdates` | local task 更新、updated count +1 |
| M-08 | 正常 | locked task 外部変更 | apply | local 値保持、skipped に task id |
| M-09 | 境界 | target dirty=false | apply | 更新後、必要に応じ dirty 状態維持/復元 |
| M-10 | 境界 | external に新規 task 追加 | apply | 現実装は追加しない。仕様として明示し回帰テスト化 |
| M-11 | 境界 | external で task 削除 | apply | 現実装は削除しない。conflict 側で検出 |
| M-12 | 異常 | predecessor 文字列不正 | apply | 例外を握って他項目更新継続、ログ確認 |

### スケジュール / カレンダー / コスト

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| S-01 | 正常 | FS/SS/FF/SF 依存 | scheduling | 開始/終了が依存タイプ通り |
| S-02 | 境界 | lag 0, 正 lag, 負 lag | import + schedule | 日付が正しく前後 |
| S-03 | 異常 | 自己依存、循環依存 | initialize | circular error |
| S-04 | 境界 | duration 0 milestone | critical path | milestone として日付不変 |
| S-05 | 境界 | 24h calendar, 週末非稼働, 祝日例外 | calendar add/compare | 稼働時間だけ加算 |
| S-06 | 境界 | DST 切替日、月末、閏日 2/29 | `CalendarDefinition.add/compare` | 期待作業時間、日付飛びなし |
| S-07 | 異常 | WorkRange start > end, 重複 range | working hours set | `WorkRangeException` |
| S-08 | 境界 | Cost/EV で 0 除算 | `EarnedValueCalculator` | NaN/Infinity の仕様固定、UI 表示破綻なし |
| S-09 | 正常 | Fixed Units/Duration/Work | remaining work/duration/units 変更 | ルールごとの保存量が一致 |
| S-10 | 大量 | 10k tasks + 20k deps | schedule | 時間上限、stack overflow なし |
| S-11 | 互換性判断・境界 | `DefaultDurationUnits=Hours`、`MinutesPerDay=360`、`Work` 入力単位=`Minutes` の別プロジェクト2件 | 新規 task 作成、既定単位の省略入力、設定変更、MPO保存・再読込 | 新規 task は1作業日（6h）の推定期間を持ち、bare `1` は Duration 入力単位の1hとして解釈する。設定変更は所有プロジェクトだけに反映し、MPO再読込後もカレンダー係数と既定入力単位を保持する。文書由来の判断。Microsoft: [Duration task field](https://support.microsoft.com/en-us/project/duration-task-field), [Change the default task duration unit](https://support.microsoft.com/en-us/project/change-the-default-task-duration-unit), [Change the default time unit for work](https://support.microsoft.com/en-us/project/change-the-default-time-unit-for-work) |
| S-12 | 異常・原子性 | 複数タスクの一括リンクで後続リンクの sentinel 更新が失敗 | 先行リンクが接続された後に例外を注入し、依存グラフ、pending observer events、revision、Undo/Redo を比較 | 一括処理全体を巻き戻し、両方向の adjacency/sentinel、イベント、revision、通知、Undo/Redo 履歴が失敗前の状態と一致。Fixture: `DependencyServiceTest.failedBulkConnectRestoresGraphEventsRevisionAndUndoState` |
| S-13 | 異常・原子性 | 単一依存の create/connect/remove/init 中に sentinel 更新が失敗 | 失敗を注入し、両 endpoint の adjacency/sentinel、dependency fields/dirty、pending events、revision、Undo/Redo を失敗前後で比較 | 操作全体を巻き戻し、依存グラフと公開状態・Undo履歴を維持。Fixture: `DependencyServiceTest.failedSingleDependencyCreationRestoresGraphAndUndoState`, `failedSingleDependencyConnectRestoresGraphAndUndoState`, `failedSingleDependencyRemovalRestoresGraphAndUndoState`, `failedDependencyInitializationRestoresEndpointsGraphEventsAndUndoState` |
| S-14 | 互換性判断・境界 | successor SNET と SS predecessor-start 境界、predecessor start が SNET より前/後 | 独立した作業カレンダーの project で 1 日 task を接続し、predecessor/successor/unrelated の start/finish、SS 境界、diagnostics、Undo/Redo を比較 | successor start は max(SNET, predecessor start)。predecessor の開始が先なら SNET を保ち、後なら predecessor start に従う。hard-constraint を診断し dependency conflict は出さない。Microsoft の scheduling guidance からの文書由来判断で、MSP 実測ではない。Fixture: `TaskLinkAndBarScheduleTransitionsTest.startNoEarlierThanConstraintAndStartToStartLinkUseLaterBoundary` |
| S-15 | 永続化・Undo/Redo | FS/0 lag dependency を SS/8h に更新 | MPO へ保存・再読込、編集 Undo/Redo、再度保存して再読込 | Undo は FS/0 を、Redo と再読込後は SS/8h を保持。両 endpoint adjacency と再計算後の日付を照合。Fixture: `MpoFileImporterTest.mpoSavesAndReplaysDependencyOperations` |
| S-16 | 永続化・Undo/Redo | MPO 再読込後に dependency を削除 | 削除で両 endpoint adjacency から消えること、Undo で両方復元、Redo で再削除、保存・再読込と operation log を確認 | `dependency.delete` を記録し、再読込後も両 endpoint に link がなく、再計算後の日付が削除状態の保存値と一致。Fixture: `MpoFileImporterTest.mpoSavesAndReplaysDependencyOperations` |

### UI 入力 / Gantt / Report / Image

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| U-01 | 正常 | `2/2` reference `2020/12/01` | `YearlessDateInputParser.parse` | `2021/02/02` |
| U-02 | 正常 | `12/2` reference `2020/12/01` | parse | `2020/12/02` |
| U-03 | 境界 | `2/29` reference leap/non-leap | parse | 次の有効な閏年、または明示例外 |
| U-04 | 異常 | `13/1`, `2/30`, `0/1`, `1/0` | parse | invalid date 例外 |
| U-05 | 境界 | `1/2 9:30`, `1/2 25:00`, `1/2 abc` | parse | 時刻反映、不正時刻は例外または midnight 固定 |
| U-06 | 異常 | fallbackFormat null | parse | NPE ではなく仕様化された例外 |
| U-07 | 正常 | IME 入力開始 | `processInputMethodEvent` | 既存テキストを消さない |
| U-08 | 正常 | 日本語 key typed 連続 | `processKeyEvent` | editor text に追記 |
| U-09 | 正常 | Backspace/Delete | `isClearCellKey` | Backspace のみ clear 扱い |
| U-10 | 境界 | date editor select-all 後タイプ | selection stabilize | caret collapse、選択解除 |
| U-11 | 境界 | progress -0.2, 0, 0.44, 1, 1.5, NaN | `GanttRenderer.progressRatioForSchedule` | 0..1 clamp、NaN 方針固定 |
| U-12 | 境界 | report zoom 0, 0.09, 0.1, 1, 4, 4.1, NaN | `ReportViewer.clampZoomRatio` | 0.1..4.0 clamp、NaN 不許可 |
| U-13 | 正常 | image export basename | `appendPdfExtensionIfMissing` | `.pdf` 付与 |
| U-14 | 境界 | `foo.PDF`, `foo.png`, parent null | extension append | 大文字/PNG 方針を仕様化 |
| U-15 | 異常 | 0 page printable | export | 空/破損 PDF を作らない、job complete |
| U-16 | 大量 | 多ページ Gantt PDF | export | page count 分出力、progress 1.0、stream close |
| U-17 | 受入 | 実 JFrame、タスク 1 件、期間列を選択 | `:microproject_ui:guiTest` で Robot click → root-pane EditField → `3` を commit | F2 に対応する root-pane の一経路で期間だけが更新され、想定外モーダルなし |
| U-18 | 受入 | 選択依存の全リボン／メニュー操作 | 実 Robot でタスクを選択後、情報・リンク・インデント／アウトデント・展開／折り畳み・非表示を操作。Hide と対になる Show All は同じ Task Editing バンドから物理クリックする | 押下時にも同じタスク選択が保持され、モデル変更と表示変更が一致。非表示後の再表示導線が同一バンドで発見でき、必要タスク数不足は明示的に無効化または通知 |
| U-19 | 回帰 | 階層、依存関係、非表示の各変更 | 実キーボード／リボンで変更 → Ctrl+Z → Ctrl+Y | 一操作が一つの Undo edit となり、前状態／後状態を完全に復元。選択・表示・ガントも一致 |
| U-20 | 回帰 | タスク／リソース使用状況、タイムシート | 各リボンボタンを実 Robot click で開く | 例外なしではなく、専用ビュー／ダイアログの内容モデルが初期化され、表示・閉じる操作まで完了 |
| U-21 | 視覚 | 変更されたダイアログ／タブ／リボン面。共通レイアウト変更・定期/リリース監査では日英・100/125/150% DPI | 変更した制約に関連するlocale/scaleの共有GUI harnessを実行。全画面棚卸しは定期/リリース監査で実施 | 対象面のラベル、入力欄、ボタン、タブがviewport内で非重複。無関係なコード変更ではGUI matrixを反復しない |
| U-22 | 回帰 | 変更可能なプロジェクト操作 | 操作 → 保存 → 再読込 → Undo/Redo可能な範囲を確認 | 保存後もモデル／表示が一致し、操作対象外のデータや Undo 履歴を破壊しない |
| U-23 | 異常 | 選択なし、複数不足、read-only、ロック済み、非対応 view | リボン、メニュー、ショートカットの各入口を実行 | 入口間で有効条件とエラー表示が一致し、silent no-op と例外漏出がない |
| U-24 | 診断 | UI debug mode | 成功・前提不成立・例外・表示未更新の各操作を実行 | ログに command ID、選択、モデル前後、表示前後、Undo 状態、失敗理由が記録される |
| U-25 | MSP互換/回帰 | タスク移動（Alt+Shift+↑/↓、リボン/メニュー、行ドラッグ） | Microsoft公式ショートカット仕様を issue にリンク。全行選択で移動→Undo/Redo→保存/再読込、単一セル・read-only・lock は各入口で disabled/rejected 結果を確認 | 公式仕様の「entire row must be selected」に一致。全入口は同一選択判定・一回の順序変更・可観測な失敗理由を共有し、再読込後も順序が一致 |
| U-25-W | Windows window-shell acceptance (#479, #482) | Windows 11、FlatLaf native decorations 対応ランタイム、一次・二次 document window、日本語/英語、100/125/150% DPI（画面解像度は変更せず、実行環境の値を記録） | FlatLaf native-decoration capability を起動時に有効化してから、Robot で非操作 caption の drag、右上の OS 最大化/復元、Alt+Space system menu、端/角 resize、二次ウィンドウの close/focus を確認する。2プロジェクトとresource poolを開く結合行程では全shellを物理切替し、active frame・ResourceView・固有タイトルを確認し、副ウィンドウのリボンコマンド後に全書類を実タイトルバーから閉じる。通常表示へ復元した直後と close 前に、各 primary/secondary frame bounds を `GraphicsConfiguration` の usable work area（screen bounds から `Toolkit.getScreenInsets` を除いた領域）内に検査する。AWTの論理座標へDPI倍率を再乗算しない。 | 手製 drag/button を介さず、Windows が caption drag、Snap、system menu、resize、max/restore を所有する。通常表示の一次/二次frame全体がusable work areaに収まり、150% DPIでも内容・status領域が画面外に切れず到達できる。複数書類では切替ごとにactive documentとshell titleが一致し、ResourceViewが保持され、二次Ctrl+F1でprimary ribbonを変更せず、各書類を一度閉じた後にproject registrationやsecondary windowが残らない。header icon と taskbar/system-menu icon は同じ公式アセットに由来し、interactive header controls は caption として扱われない |
| U-26 | 入力トランザクション回帰 | プロジェクト既定 Duration unit=`Hours`、source workday=6h、physical bare duration=`10`、明示 duration=`1 day`/`6 hours`（同じ実値・異なる表示単位）、達成率=`10/50/99/100`、開始日=`2026/9/25`、既存日本語の Convert/再変換、残存期間、親子タスクをアウトデント後にMPO保存 | (1) headlessで editor attach 前に連続した key/input-method event を投入し、buffer→editor→commit を検証、(2) 実Robotで期間・達成率・日付を複数桁入力してEnter/フォーカス移動でcommitする。特に達成率=`10`はセルを物理クリックして選択後、F2・`editCellAt`等を使わず最初の数字キーで編集開始し、最初の文字と全桁のeditor受信、commit済みモデル値、描画文字列を検証する、(3) Convertで既存日本語を選択して再変換、(4) duration=8日 の 0%/10%/50%/99%/100% で domain値・renderer文字列・予定バー形状・timescale viewportを比較、(5) アウトデント→保存→再読込で操作ログを含むMPOを検証 | bare `10` は1文字ずつ物理入力され、source default Duration unit の10hとしてcommitされる。`1 day`と`6 hours`は6h/dayで同一の物理millisecondsだが、Task DurationFormatに由来する per-task表示単位は区別して描画され、project calendar option変更後も保持されることをcore regressionで確認する。Task scheduleは物理millisecondsに正規化し、表示単位は独立したtransient task metadataとして保持して標準MSPDI Task DurationFormat経由でMPOへ保存する（POD wireは不変）。MPO再読込後も階層・実値・task display unit・Hoursのproject defaultが整合する。日付は全桁が認識され、IME再変換は既存文字列を選択する。残存期間は Microsoft Project の `Duration - (Duration * Percent Complete)` に従い、10%時の8日は7.2日となる。小数を一律に丸めず、入力破損由来の値ではなく正確なduration値と表示を検証する。0%から99%までは完了オーバーレイだけが進み、予定バー全体とtimescale viewportは不意に変わらず、100%でも同じ表示範囲契約を守る。構造変更後の保存は成功し、再読込後も操作履歴が整合する。各入力は editor text だけでなく commit済みモデル値と描画文字列を検証する。 |
| U-27 | コンテキストメニュー回帰 | 列ヘッダ右クリック、列の挿入/非表示、行右クリックの変更系コマンド、読み取り専用・空白ヘッダ | (1) Robotで対象ヘッダを右クリックしてpopupと対象列を確認、(2) insert/hide は同一の列レイアウトmutationを通すroute integrationでモデル・表示・Undo/Redoを確認、(3) 永続化される列レイアウトは保存→再読込し、列順・幅・手動幅まで確認、(4) read-only/無効状態は操作不能または明示拒否を確認 | popup表示だけでは合格にしない。挿入位置、field array、プロジェクトの保存対象レイアウト、表示列、Undo/Redoが一致する。行popupは選択対象を変えず、全変更項目は対応するcanonical commandを一度だけ実行する。 |
| U-28 | 基本操作横断回帰（#587） | タスク表右クリック、期間・開始日/終了日・達成率入力、レベル上げ/下げ、Undo/Redo、概要・ヘルプ・言語・カレンダー・プロジェクト情報、ネットワーク/WBS、切断中Frame・EDT再描画、旧形式Boolean表示 | clean `installDist` から実Robotで各入口を操作する。入力は期間=`10`/`20`、日付=`2026/9/25`/`2026/10/05`、進捗=0%/10%/100%を1文字ずつcommitし、親子・依存あり/なしで比較する。階層・行操作・日付・期間・進捗・先行関係の各変更後にCtrl+Z→Ctrl+Yを実行する。各ダイアログ/ビューは内容、閉じる、再描画、表示範囲を確認し、変更系は保存→再読込する。切断中Frameの遅延ボタン更新、EDT外からの列構成変更、Boolean列の文字列表示を単体回帰で確認する。 | 右クリックの全可視項目は非空のローカライズ文言とアイコンを持つ。入力値はcommit済みモデル値・セル描画・スケジュール結果と一致し、意図した制約/依存再計算以外で日付が変わらない。Ganttはバー位置・幅・timescale viewportを操作前後およびUndo/Redo後で比較し、モデル変化と無関係な自動移動を許さない。ダイアログはタイトル帯のみ、ゼロ高さ、クリップ、空内容をfailとし、ネットワーク/WBSは専用ビューの内容モデルと可視領域が初期化される。切断Frameや列再構成の遅延描画でもNPE、ClassCastException、配列境界例外を出さず、XML/stderrにも未捕捉EDT例外を残さない。 |
| U-31 | Selected Tasks timescale zoom (#794 M2.4) | Gantt / Tracking Gantt / Task Usage / Resource Usage、選択なし・単一・複数・一部/全て日付なし | 選択された可視タスクから共通日付範囲を解決し、4ビューで同じ範囲を収める尺度と開始位置にする。選択なしまたは全て日付なしでは無効化し、viewportとモデルを変更しない。 | Microsoft Project `Application.ZoomTimescale` の `Selection=True` がGantt、Resource Usage、Task Usageで選択タスクにフィットすると記載するdocument-derived compatibility decision（https://learn.microsoft.com/en-us/office/vba/api/project.application.zoomtimescale）。Tracking GanttはGanttと共通の選択・時間軸コンポーネントを使う製品内互換ルートとして扱う。複数選択は有効日付の最小開始〜最大終了。zoomはview-onlyでプロジェクトデータ、Undo履歴、保存内容を変えない。物理Robotで4ビューの選択→実コマンド→表示範囲と日付不変を確認する。 |
| U-29 | フォーム行配置／文字クリップ（[#590](https://github.com/tetsuji16/ProjectLibre/issues/590)、[#460](https://github.com/tetsuji16/ProjectLibre/issues/460)再発） | #590本文画像1〜4: Change Working Time、Calendar Options、Help、Locale Settings。返信画像1 ([comment 5778929009](https://github.com/tetsuji16/ProjectLibre/issues/590#issuecomment-5778929009)): Update Tasks。返信画像2 ([comment 5779021864](https://github.com/tetsuji16/ProjectLibre/issues/590#issuecomment-5779021864)): Recurring Task（終了条件の日付/回数欄の赤丸部分）。全6画像の各画面、および #460で既知となった Project Information、Clear Baseline、Task Information のフォーム行 | issue本文と全返信の画像を列挙し、各画像→画面/部品→layout owner→回帰assertionを記録する。既存の Change Working Time Robot経路、Calendar Options ribbon経路、File > Locale/Help のRobot経路、Task > Update/Insert > Recurringの物理Robot経路を使用し、全ての可視テキスト部品のpreferred高さ、親内境界、兄弟重なりを検査する。全画面を `ja/en × 100/125/150%` の視覚マトリクスで実行する。過去 issue とコメント（閉鎖/再開を含む）を検索し、既知画面が共通layout原因を使う場合は同じ共有検査へ追加する。 | #590の全6画像に対応する物理画面・テストが揃い、ラベル/値/ボタンを横切る罫線や孤立したwidget片がない。文字付き部品はfont-derived preferred高さ以上、親pane内に収まり、隣接部品と重ならない。既知の共通原因は `DefaultFormBuilder.nextLine(int)` の互換実装が `nextLine(2)` を3dlu spacer trackへ誤配置すること、およびRecurring Taskの4部品を固定幅単一FlowLayoutに詰めたこと。過去の漏れは issue画像網羅性・旧issue再調査・描画寸法assertionを機械的に強制せず、「dialogが開いた/見える」を視覚検査として扱ったこと。 |
| U-30 | GUIデスクトップ競合（[#771](https://github.com/tetsuji16/ProjectLibre/issues/771)） | 別JVMの同時Robot suite、またはRobot対象frameに重なる外部foreground window | JUnitクラス開始でOSファイルロックを保持する。子JVMを使った `GuiDesktopSessionCoordinatorTest` で競合時の診断付き待機/取得を検証する。全GUI Robot入力は `GuiRobot` を経由し、Windowsのforeground監視が前面外部windowの重なりを記録した後のマウス/キー入力をdispatch前に拒否する。failure snapshotはPID/タイトル/座標/重なりを記録する。試験中のforeign foreground overlapは環境競合としてJUnit failureにし、元のassertion failureがあれば保持する。ローカルとWindows Actionsでfresh `installDist`、100% baseline、その成功後にlocale/scale matrixの順に実行する。 | 同一端末のProjectLibre Robot suiteはJVMをまたいで同時に入力しない。無関係な前面windowがテストframeを覆った試験は次のRobot入力を送らず `GUI_ENVIRONMENT_CONTENDED` と診断し、合格扱いにしない。元の機能assertionや画像を消さず、監視不能時もGUI acceptanceをfail closedにする。画面が占有されていない実環境で全ja/100% suiteが成功してからDPI/英語試験を開始する。 |
| U-35 | 主暦と副暦の視認性（[#864](https://github.com/tetsuji16/ProjectLibre/issues/864)） | 稼働時間変更カレンダーの月見出し・日セル（グレゴリオ暦日とOS/locale選択の副暦日が同時表示される環境） | 日付変換元、曜日・選択・編集・保存が参照する日付を確認する。日英で月表示と各日セルの暦体系を初見で判別できる凡例または同等の明示的ラベルを用意し、セルのアクセシブル名/ツールチップにも反映する。Robotで月またぎ・年またぎ・副暦閏月を移動し、代表日を選択して稼働例外を編集、保存/再読込する。デスクトップ解像度は変更しない。 | 主暦はグレゴリオ暦の日付として一貫し、副暦は補助情報として明確に区別される。曜日・選択・稼働時間編集・永続化は同一の主日付に結びつき、副暦を見誤って別の日付を編集できない。 |
| U-32 | Gantt wheel and keypad zoom routes (#414/#794) | Task table と Gantt grid が同期された可視タスク、同期対象外の独立横スクロール位置、開始/終了日の viewport、Gantt または Tracking Gantt がactive top viewである文書。非Gantt no-op fixture はResource Viewを使う。 | Robot で同一fixtureに plain wheel、Shift+wheel、Ctrl+wheelを物理送信し、Gantt focusとSpreadSheet focusの両方からCtrl+Numpad Divide/Multiplyを連続送信する。Resource Viewをactive viewに切り替えた後も両キーを物理送信する。各操作後に表/Ganttの非対象viewport位置、zoom index、および代表的なモデル日付を独立して記録する。既存desktop resolutionを維持する。 | plain wheel は同期縦スクロールを一段だけ進め、Shift は水平スクロールだけを一単位進め、Ctrl wheel と Ctrl+Numpad Divide/Multiply はzoomだけを変える。キーパッド操作はroot-paneに一箇所だけ登録され、Divideはzoom in、Multiplyはzoom outを一段だけ行い、非Gantt viewではno-op。非対象viewportやモデル値は変化せず、ホイール/キーの二重処理がない。 |
| U-33 | Gantt drag auto-scroll (#414/#794) | 長いタイムライン、表示範囲の端にある編集可能な task bar、保存可能な project | 実Robotでbarを掴み、chart viewport端へドラッグしてauto-scrollを発生させる。表示範囲、bar、モデル日付、Undo/Redo、MPO保存/再読込後の日付を確認する。既存 desktop resolution を維持する。 | 端へのドラッグでchartが水平auto-scrollし、barとtask dateが同じ移動を示す。1回のUndo/Redoで元/後の日付へ戻り、MPO再読込も後の日付を保持する。 |
| U-36 | Two-document shortcut isolation (#414/#794) | 独立した2つの project window、各1つの選択可能な task Name cell | Robotで各文書のName cellを物理選択し、document-root Ctrl+Deleteを実行する。各文書でCtrl+Z/Yを送り、両方の順序でウィンドウを切替える。選択task ID、各モデル値、Undo/Redoの対象を確認する。文書ウィンドウは既存解像度内で重ならない位置に配置する。 | 各実行でアクティブ文書の選択セルだけが消去され、非アクティブ文書の選択とモデルは不変。Ctrl+Z/Yはアクティブ文書だけを戻し/再適用する。ウィンドウ切替後も両文書の選択と値が混線しない。 |
| U-34 | 日本語IME再変換の環境確認（[#866](https://github.com/tetsuji16/ProjectLibre/issues/866)） | Windows/JDK 25、日本語IME、spreadsheet cellに選択済みの既存日本語文字列 | focused editorでVK_CONVERTを物理入力し、アプリAction到達、`InputContext.reconvert()` の結果、InputMethodEvent、IME候補/再変換表示、commit後のモデル値・描画値を記録する。テスト用診断でOS/JDK入力メソッド capability を分離し、設定や画面解像度を変更しない。 | Convertは正しいeditor-local routeに到達し、対応IMEなら選択文字列を再変換してモデル/描画へcommitする。未対応IMEの場合は文字列とselectionを保持して未対応として明確に記録し、再変換成功と誤認しない。根本原因: JDK 25 の Windows 入力メソッドアダプタ `sun.awt.windows.WInputMethod` は `InputMethodAdapter.reconvert()` を override しないため、Windows では IME 種別・設定に関係なく `InputContext.reconvert()` が常に `UnsupportedOperationException` を投げる（JDK `src.zip` で確認）。IME設定変更での再試行は無効。Convert は editor binding と非編集セルからの開始の両経路とも `ImeTextInputSupport.requestReconversion` 1本を通り、未対応時は文字列・selection・モデル値・描画値を保持して文書ステータスバーに未対応を表示する（`EditorSelectionBehaviorTest`、`CanonicalProjectWindowGuiAcceptanceTest.robotConvertReportsUnsupportedImeOnBothSpreadsheetRoutesWithoutChangingText`）。Windows でのネイティブ再変換（`IMR_RECONVERTSTRING`）はJDKが提供しないためプラットフォーム制約として扱い、U-26の再変換要件はこの未対応結果の決定的表示で判定する。 |

### 2026-09-19 古いGUIテストの監査

- `TaskDurationGuiAcceptanceTest` と `TaskDateDependencyGuiAcceptanceTest` は、Robotでセル選択・F2経路を確認するが、値は `editor.setText(...)` で注入する。そのため **route/commit契約** として保持し、U-26の物理多文字入力の証跡には数えない。
- `CommonSpreadSheetImeStartTest` と `CommonSpreadSheetDateTypingTest` は editor attach 前のイベント列、IME開始・Convert、全桁日付を検証する **headless入力列契約** として保持する。これらはOS IMEの物理入力を代替しない。
- `TaskInformationRibbonGuiAcceptanceTest` のアウトデント系は、物理リボン操作後の hierarchy/Undo/Redo と MPO保存・再読込までを同一fixtureで検証する。popup/shortcutは同じcanonical commandのroute検証に留め、保存テストを複製しない。
- U-27の列popupはInsert Column/Hide/AutoFilter/カスタムRename/列presetを、行popupはIndent/Hide-Show/DeleteとPaste Insertを、実右クリック・モデル/表示・Undo/Redo・必要なMPO再読込まで確認する。低レベルの`doClick`/Action経路は補助的なroute契約としてのみ扱う。
- 残存期間の表示については、duration変換テストだけでなく raw duration・進捗別の renderer文字列を検証する必要がある。MSP公式の計算式は小数の残存日数を許容するため、U-26で0%/中間進捗/100%を同じ表示契約に統合し、小数を見た目だけで0へ丸める回帰を防ぐ。
- 古いテストの `doClick` / `actionPerformed` / `setText` は、単体の構成・変換・ルート契約を検証する限り削除しない。物理入力・IME・保存の受入証跡として扱うことだけを禁止する。

### Build / Packaging / Regression

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| B-01 | 正常 | clean checkout | `.\gradlew.bat projects` | multi-project 解決 |
| B-02 | 正常 | unit tests | `:microproject_core:test`, `:microproject_exchange:test`, `:microproject_ui:test`, `:microproject_reports:test` | 全 pass |
| B-03 | 正常 | build | `.\gradlew.bat build` | compile/jar 成功 |
| B-04 | 正常 | packaged import | `.\gradlew.bat verifyPackagedFileImports` | limited modules で MPP/POD 読込成功 |
| B-05 | 正常 | app dist | `.\gradlew.bat stageAppDist` | `microproject_ui/build/install/microproject_ui` 生成 |
| B-05a | 正常 | legacy packaging cleanup | `.\gradlew.bat cleanLegacyPackagingArtifacts` | `isolated-build` が削除され、Gradle 正本の成果物には影響しない |
| B-06 | 異常 | JAVA_HOME 未設定/不正 | package task | 既定 JDK 25 fallback または明確な失敗 |
| B-07 | 異常 | WiX なし | MSI/EXE package | 原因が分かる失敗、途中成果物破損なし |
| B-08 | 境界 | docs downloads 既存巨大 part | publish split exe | 古い part 削除、新 part/rebuild bat 生成 |
| B-09 | リリース前GUIゲート | Windows release runner、clean checkout、生成済み `installDist` | `:microproject_ui:guiTest -PguiTestSuite=smoke --max-workers=1` を実行し、Issue #587の共有原因を代表するタスク表／Gantt、ポップアップ、ダイアログ、ビュー切替、物理入力、Undo/Redo、エラー監視を確認。全体スイートは定期／手動監査で `-PguiTestSuite=full` を実行 | headless unit testだけでは合格にしない。代表GUI受入が全件成功し、失敗時は `guiTest-artifacts` の画面とログを成果物として残す。release jobはこのゲート失敗時に公開処理へ進まない |
| B-10 | GUIゲート実装の強制性 | Windows release runner、現行commit、fresh `installDist`、日本語/英語、100/125/150% | B-09の機能ゲートに加え、日英×3倍率の視覚／代表Robot行列を実行する。各レッグを個別に記録し、外側のプロセス watchdog でEDT・モーダル・非daemon AWT停止を検出し、失敗時はjcmd・Window/Process一覧・JUnit XML・画面証跡を保存する。予期しないエラー画面、ダイアログ、EDT/AWT例外、未捕捉stderr/XMLスタックトレースは、テストが成功を返しても失敗扱いにする。期待される入力拒否のAlertログは、メッセージ・閉じる操作・モデル保持を確認したうえで例外と区別する。ゲート開始時に前回XMLを削除し、終了後に新しいJUnit XMLのskipを解析する。 |

## 4. テスト環境・前提条件

- OS: Windows、JDK 25+、Gradle Wrapper 使用。
- Headless unit test: `java.awt.headless=true`。Swing/EDT 系は `SwingUtilities.invokeAndWait` を使う。
- GUI acceptance test: Windows のデスクトップセッションで `:microproject_ui:guiTest` を実行する。`installDist` を依存に含み、Robot 操作の失敗時は `microproject_ui/build/reports/guiTest-artifacts/` に画面を保存する。
- B-10の結果は過去の実行記録で代用しない。現行commit、locale、DPI、fixture、実行コマンド、完走時刻、成果物パスを同じ検証記録に残す。後続実行で失敗した場合は、以前の `BUILD SUCCESSFUL` 記録を現行合格証拠として扱わない。
- GUI quality gate: `docs/gui-quality-gate.md` を正本とする。変更した契約に必要な層（物理操作、モデル／表示、Undo/Redo、保存再読込、視覚レイアウト）を選んで実行する。挙動を変えないリファクタリングで同じRobot/UI matrixを繰り返さず、対象モジュールのテストを優先する。広範なGUI smoke/full matrixはリリースまたは定期監査で実行する。
- Sample data: `samples/sampledata.mpp`, `samples/Commercial construction project plan.{mpp,pod,xlsx,xml,json}`。
- 一時ファイル: JUnit の temp directory を使い、POD/XLSX/sidecar を毎回隔離。
- テストソース棚卸し: `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`。全 `src/test` / `src/guiTest` Java ファイルの区分をJSONに記録し、テスト計画中の存在しないファイル参照と静的に解決できるDynamicTest名の重複を失敗にする。動的式はJSONで未解決一覧として残し、人が確認する。
- 並行性: thread pool で sidecar lock、`Timer` poll、UI thread 操作を重ねる。
- Locale/Timezone: `Asia/Tokyo`, `UTC`, DST あり timezone で日付/カレンダーを再実行。
- モック/スタブ: `ProjectWriterUtility` 生成ファイル、fake `WorkspaceSetting`, fake `Schedule`, fake `GraphPageable/ViewPrintable`。
- 性能: 大量タスクは unit では軽量生成、nightly で 10k+ task の import/schedule/export。

## 5. 合否判定基準

- 戻り値: import/export は project/file 非 null、件数、主要フィールド一致。
- 例外: 異常系は期待例外または明示的エラー状態。NPE、silent corruption、partial overwrite は fail。
- サイドエフェクト: sidecar JSON、lock lease、workspace payload、dirty flag、project mtime/length が期待通り。
- UI: EDT 上で警告/リロード/入力状態が一貫し、選択範囲や editor text が崩れない。
- ファイル: 元ファイル保護、temp rename の原子性、stream close、PDF/XLSX/POD の再読込可能性。
- 並行性: JSON 破損、二重 lock、期限切れ lock 残留、reload 二重発火がない。
- 性能: 大量データで OOM/StackOverflow なし。基準時間を CI/nightly で固定。
- 回帰: 既存テストに加え、上記 ID を unit/integration/manual に分類して CI で少なくとも unit + packaged import を必須化する。PR CI で `-x test` を使わず、保存・Save As の回帰テストを必ず実行する。

実行履歴は [docs/testing/test-execution-history.md](docs/testing/test-execution-history.md) に分離しています。履歴にあるテスト件数や成功記録は、その後のソース変更を含む現在の状態を保証しません。

## 2026-10-10 bootstrap and MPO regression contract

| Surface | Preconditions and route | Required outcome | Regression fixture |
|---|---|---|---|
| Desktop bootstrap | Full JDK 25; native window decoration capability may be absent; launch the regenerated installDist | OS decorations on unsupported platforms; no Java-painted title pane initialized before its Window; project dialogs open, render, and close without an uncaught EDT error | FlatLafSupportTest; RibbonExternalCommandGuiAcceptanceTest.robotInvokesRealFileRibbonCommandsAndOpensTheirDialogs |
| File / Backstage | File surface and its pages are created lazily; shared action state may change before or after creation | Persistent page buttons and direct navigation commands use the canonical dispatcher and registry; connection-state enablement propagates; File stays outside ribbon bands | RibbonAndToolbarButtonTest; StartupFactoryCommandStateTest; shared visitBackstagePages fixture; existing physical File journey |
| MSP assignment import | Attach saved resource assignments before the project is initialized, with or without a default assignment | Saved work, actual work and relative delay remain intact; source start initializes the offset origin; missing actual dates do not edit completion or duration; interactive replacement/recalculation applies only to initialized projects | NormalTaskDurationTest.attachingImportedAssignmentPreservesItsWorkAndDelay; MpxExportTrackingTest.historicalAssignmentImportUsesTheTaskStartAsItsOffsetOrigin; packaged MPP import |
| MPO identity and extraction | Save/reload a project with a signed nonzero project ID and shared resource pool; restricted OS may omit ProcessHandle start time | Project/pool IDs and task hierarchy survive save/reload and operation replay; extraction manifests always carry stable process ownership; cleanup retains the current process's open extraction | MpoFileImporterTest; MpoExtractionSessionTest; TemporaryWorkspaceTest |

These fixes do not change task command semantics or Undo/Redo. The existing MPO round-trip/replay fixtures verify persisted model state. MpxExportTrackingTest.exportKeepsPersistedTaskUidWhenNewParentPrecedesIt verifies that row IDs cannot consume persisted task UIDs. Linux GUI evidence does not replace the Windows-native caption or installer gates in U-25-W.
