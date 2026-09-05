# ProjectLibre テストプラン

## 1. テスト目的・背景

このリポジトリは ProjectLibre デスクトップアプリの開発フォークで、主な品質保証対象は以下です。

- プロジェクト計画データの読込/保存/再読込が破損なく行えること
- `MPP / XML / MPX / Planner / XLSX / POD` の import/export 互換性
- 共同編集用 sidecar メタデータ、ロック、外部変更検知が競合なく動作すること
- タスク、依存関係、カレンダー、工数、進捗、リソース、レポート、Gantt 表示が境界値でも正しいこと
- Swing UI 入力、IME、日付入力、Gantt/Report/Image 出力が実運用に耐えること
- JDK 25 / Gradle / Windows packaging 前提でビルド成果物が壊れないこと

重点は正常系確認ではなく、ファイル破損、外部更新競合、日付境界、ロック期限、巨大データ、未対応拡張子、UI イベント順序、並行更新で潜む不具合を露出させることです。

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
| F-06 | 正常 | 実 `samples/sampledata.mpp` | `MspImporter.importProject` | task/resource/calendar が 0 件でない |
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
| C-01 | 正常 | `.pod/.xml/.xlsx` | `isCollaborationCandidate` | true |
| C-02 | 正常 | `.mpp/.mpx/.planner` | 同上 | false |
| C-03 | 境界 | project path に拡張子なし、親なし相対パス | `buildSidecarFile` | `<base>.projectlibre-sync.json` を生成 |
| C-04 | 異常 | `CollaborationSession.create(null/非候補/null file)` | create | null を返し sidecar を作らない |
| C-05 | 正常 | 新規 sidecar | `load/mutate` | schema/user/locks/workspace が初期化 |
| C-06 | 異常 | 壊れた JSON, 配列 JSON, 巨大 JSON >1MB | `load` | 既定値復旧または RuntimeException。破損を silent success しない |
| C-07 | 境界 | JSON に制御文字、引用符、Unicode user | save/load | エスケープ復元一致 |
| C-08 | 並行 | 2 セッション同時 `mutate` | thread pool で 100 回更新 | JSON が壊れず全更新が整合 |
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
| U-17 | 受入 | 実 JFrame、タスク 1 件、期間列を選択 | `:micrproject_ui:guiTest` で Robot click → root-pane EditField → `3` を commit | F2 に対応する root-pane の一経路で期間だけが更新され、想定外モーダルなし |
| U-18 | 受入 | 選択依存の全リボン／メニュー操作 | 実 Robot でタスクを選択後、情報・リンク・インデント／アウトデント・展開／折り畳み・非表示を操作 | 押下時にも同じタスク選択が保持され、モデル変更と表示変更が一致。必要タスク数不足は明示的に無効化または通知 |
| U-19 | 回帰 | 階層、依存関係、非表示の各変更 | 実キーボード／リボンで変更 → Ctrl+Z → Ctrl+Y | 一操作が一つの Undo edit となり、前状態／後状態を完全に復元。選択・表示・ガントも一致 |
| U-20 | 回帰 | タスク／リソース使用状況、タイムシート | 各リボンボタンを実 Robot click で開く | 例外なしではなく、専用ビュー／ダイアログの内容モデルが初期化され、表示・閉じる操作まで完了 |
| U-21 | 視覚 | 全ダイアログと各タブ、日本語・英語、100/125/150% DPI | 実GUIの画面キャプチャとコンポーネント境界検査 | 全ラベル、入力欄、ボタン、タブが viewport 内で非重複。クリップ、ゼロ高さ、タイトルのみウィンドウは fail |
| U-22 | 回帰 | 変更可能なプロジェクト操作 | 操作 → 保存 → 再読込 → Undo/Redo可能な範囲を確認 | 保存後もモデル／表示が一致し、操作対象外のデータや Undo 履歴を破壊しない |
| U-23 | 異常 | 選択なし、複数不足、read-only、ロック済み、非対応 view | リボン、メニュー、ショートカットの各入口を実行 | 入口間で有効条件とエラー表示が一致し、silent no-op と例外漏出がない |
| U-24 | 診断 | UI debug mode | 成功・前提不成立・例外・表示未更新の各操作を実行 | ログに command ID、選択、モデル前後、表示前後、Undo 状態、失敗理由が記録される |

### Build / Packaging / Regression

| ID | 種別 | 入力/条件 | 手順 | 期待結果 / Assertion |
|---|---|---|---|---|
| B-01 | 正常 | clean checkout | `.\gradlew.bat projects` | multi-project 解決 |
| B-02 | 正常 | unit tests | `:micrproject_core:test`, `:micrproject_exchange:test`, `:micrproject_ui:test`, `:micrproject_reports:test` | 全 pass |
| B-03 | 正常 | build | `.\gradlew.bat build` | compile/jar 成功 |
| B-04 | 正常 | packaged import | `.\gradlew.bat verifyPackagedFileImports` | limited modules で MPP/POD 読込成功 |
| B-05 | 正常 | app dist | `.\gradlew.bat stageAppDist` | `micrproject_ui/build/install/micrproject_ui` 生成 |
| B-05a | 正常 | legacy packaging cleanup | `.\gradlew.bat cleanLegacyPackagingArtifacts` | `isolated-build` が削除され、Gradle 正本の成果物には影響しない |
| B-06 | 異常 | JAVA_HOME 未設定/不正 | package task | 既定 JDK 25 fallback または明確な失敗 |
| B-07 | 異常 | WiX なし | MSI/EXE package | 原因が分かる失敗、途中成果物破損なし |
| B-08 | 境界 | docs downloads 既存巨大 part | publish split exe | 古い part 削除、新 part/rebuild bat 生成 |

## 4. テスト環境・前提条件

- OS: Windows、JDK 25+、Gradle Wrapper 使用。
- Headless unit test: `java.awt.headless=true`。Swing/EDT 系は `SwingUtilities.invokeAndWait` を使う。
- GUI acceptance test: Windows のデスクトップセッションで `:micrproject_ui:guiTest` を実行する。`installDist` を依存に含み、Robot 操作の失敗時は `micrproject_ui/build/reports/guiTest-artifacts/` に画面を保存する。
- GUI quality gate: `docs/gui-quality-gate.md` を正本とする。GUI の修正は、物理操作・モデル／表示・Undo/Redo・保存再読込・視覚レイアウトの必要な層をすべて満たすまで完了扱いにしない。
- Sample data: `samples/sampledata.mpp`, `samples/Commercial construction project plan.{mpp,pod,xlsx,xml,json}`。
- 一時ファイル: JUnit の temp directory を使い、POD/XLSX/sidecar を毎回隔離。
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

### 2026-08-30 追加検証

- U-05/U-06: `YearlessDateInputParserTest` に数値時刻の範囲外（`25:00`）と、完全日付で `fallbackFormat == null` の異常系を追加し、いずれも `ParseException` で安全に拒否することを確認した。
- 実行: `./gradlew.bat :micrproject_core:test --tests "com.microproject.util.YearlessDateInputParserTest" --console=plain`（BUILD SUCCESSFUL）。
- B-02 / U-11: GitHub Actions の JDK 25 実行で `GanttWheelZoomTest.ctrlWheelKeepsTheCursorDateAnchored` がゼロサイズに近いテスト用 viewport のため不安定化した。テスト用 JScrollPane を実寸レイアウト（幅300px）で実体化し、実GUIと同じスクロール可能な幾何条件で再検証するよう修正した。UIモジュール全体（706 tests相当）をローカルで再実行し成功した。
- U-12: `TaskDateDependencyGuiAcceptanceTest.robotDateEditSkipsWeekendWhenSchedulingFsSuccessor` で、タスク表から土曜日（2026/06/13）を入力した場合に稼働日に正規化され、FS後続タスクの日付も依存関係どおり更新されることを実マウス操作で確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.common.TaskDateDependencyGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- U-13: `DefaultFrameManagerGuiAcceptanceTest.robotSwitchesBetweenTwoOpenProjectsWithoutMixingFrames` で、プロジェクト選択コンボを実マウス＋Home/End/Enter操作し、先頭→2番目→先頭を往復して各フレームの表示・active状態が排他的に切り替わることを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.workspace.DefaultFrameManagerGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-07: `ChangeWorkingTimeDialogGuiAcceptanceTest` を再実行し、実GUIで稼働日選択後の保存とCancelの双方が期待どおり反映／破棄されることを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-08: `TaskInformationGuiAcceptanceTest` を実GUIで再実行し、タスク情報画面の表示・編集・確定経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.TaskInformationGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-09: `ResourceLevelingDialogGuiAcceptanceTest` を実GUIで再実行し、リソース平準化ダイアログの表示・操作・確定経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.dialog.ResourceLevelingDialogGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-10: `PreferencesDialogGuiAcceptanceTest` を実GUIで再実行し、設定ダイアログの表示・入力・確定／取消経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.dialog.PreferencesDialogGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- CCPM-GUI-09: `CriticalChainStatusDialogGuiAcceptanceTest` を実GUIで再実行し、クリティカルチェーン状態ダイアログの表示・内容確認・終了経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.dialog.CriticalChainStatusDialogGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-08: `TaskTableGanttGridGuiAcceptanceTest` を実GUIで再実行し、タスク表とガント領域の選択・表示対応および再描画経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-13/14: `GanttBarDateDragGuiAcceptanceTest` を実GUIで再実行し、ガントバーのドラッグによる日付変更とFS/SS/FF/SF依存タスクの連動を確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-01: `WelcomeDialogGuiAcceptanceTest` を実GUIで再実行し、起動時ウェルカム画面の表示・操作・終了経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.dialog.WelcomeDialogGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-08: `RibbonTabGuiAcceptanceTest` を実GUIで再実行し、標準リボンタブの切替とコマンドボタン操作経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- CCPM-GUI-14補助検証: `ManualAndInactiveTaskSchedulingTest` を実行し、手動スケジュール時のスケジューリング挙動をモデル層で再確認した。`./gradlew.bat :micrproject_core:test --tests "com.microproject.pm.task.ManualAndInactiveTaskSchedulingTest" --console=plain`（BUILD SUCCESSFUL）。実GUIでのMS Project直接比較は引き続き外部環境待ち。
- B-03/B-05: `./gradlew.bat --no-daemon clean build installDist -x test --console=plain` でclean成果物と `modules/micrproject_ui/build/install/micrproject_ui` を再生成し、その後 `TaskDurationGuiAcceptanceTest` を実行して配布レイアウト経路を確認した（BUILD SUCCESSFUL）。全テスト込みのRelease検証はGitHub Actionsで成功済み。
- B-02: clean成果物再生成後に `./gradlew.bat :micrproject_ui:test --console=plain` を実行し、UIユニットテスト（BUILD SUCCESSFUL、53秒）を確認した。
- GUI-NC-10: `TaskDateDependencyGuiAcceptanceTest.robotInvalidDateRejectsInputAndPreservesOriginalValue` を実GUIで再実行し、不正日付入力時の警告表示、編集キャンセル、元データ保持を確認した。focused GUIテストはBUILD SUCCESSFUL。
- GUI-NC-10: `TaskDateDependencyGuiAcceptanceTest.robotInvalidPredecessorRejectsInputAndPreservesExistingLink` を実GUIで再実行し、不正先行タスクID入力時のエラー表示、既存リンク保持、編集終了を確認した。focused GUIテストはBUILD SUCCESSFUL。
- B-04 / CCPM-GUI-10: `./gradlew.bat :micrproject_exchange:test --console=plain` を実行し、MPP/POD/XML/XLSXのインポート・エクスポート・再読込およびCCPM依存スケジュール検証がBUILD SUCCESSFULであることを確認した。
- B-02: `./gradlew.bat :micrproject_reports:test --console=plain` を実行し、レポート生成モジュールの回帰テストがBUILD SUCCESSFULであることを確認した。
- B-02 / CCPM-GUI-10補助検証: `./gradlew.bat :micrproject_application:test --console=plain` を実行し、プロジェクト作成・保存ワークフローの回帰テストがBUILD SUCCESSFULであることを確認した。
- B-02: `./gradlew.bat :micrproject_core:test --console=plain` を実行し、スケジューリング・カレンダー・依存関係を含むcore全体の回帰テストがBUILD SUCCESSFULであることを確認した。
- GUI受入全体: `./gradlew.bat :micrproject_ui:guiTest --max-workers=1 --console=plain` を追加の狭幅リボンポップアップケース込みで実行し、16 GUIテストクラス（全受入ケース）がクラス間干渉・ウィンドウ残留なしでBUILD SUCCESSFUL（39秒）となることを確認した。
- GUI-NC-08: `OfficeChromeSearchGuiAcceptanceTest` を実GUIで再実行し、リボン／Office Chromeの検索入力と結果表示経路が成功することを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.ui.shell.OfficeChromeSearchGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-04: `TaskDurationGuiAcceptanceTest` を実GUIで再実行し、期間セル入力によるタスク期間更新とガント表示反映を確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.common.TaskDurationGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI-NC-12: `TaskTextInputGuiAcceptanceTest` を実GUIで再実行し、日本語・長文・空文字のテキスト入力で対象セルのみが更新されることを確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.TaskTextInputGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- CI節目確認: Release Run `33306149733`（commit `fbff56043`）のBuild/package、release staging、GitHub Release、Pages deployがすべてsuccess。`actions/checkout@v5`／`actions/setup-java@v5`更新後の配布経路も継続して正常であることを確認した。
- GUI-NC-03/08: `TaskTableGanttGridGuiAcceptanceTest.twentyMixedTasksRemainAccessibleAfterMouseScrollbarClick` を追加し、FSで連続する10タスク＋独立10タスク（計20タスク）を生成した実GUIで、20行の存在と縦スクロールバーのマウス操作を確認した。`./gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest" --console=plain`（BUILD SUCCESSFUL）。
- GUI受入回帰: 20タスクケース追加後の全体実行で、不正先行入力ケースの警告ダイアログがEDTを待たせる問題をスレッドダンプで検出。GUIテストの警告解除 watcher をモーダルループに依存しない直接disposeへ変更し、`./gradlew.bat :micrproject_ui:guiTest --max-workers=1 --console=plain`（BUILD SUCCESSFUL、33秒）で15クラスの完走を確認した。
- 最新コミット再検証: `./gradlew.bat :micrproject_ui:guiTest --max-workers=1 --console=plain` を`1c97aa0d5`上で再実行し、20タスクケースを含む全GUI受入スイートがBUILD SUCCESSFUL（33秒）となることを確認した。
- 20タスク構造回帰: 同ケースにタスク総数20、FSリンク数9、独立タスク数10の明示アサーションを追加し、focused実Robot GUIテストがBUILD SUCCESSFUL（8秒）となることを確認した。
- 20タスク構造回帰（全体）: 構造アサーション追加後の`4d02db9d4`で`./gradlew.bat :micrproject_ui:guiTest --max-workers=1 --console=plain`を再実行し、全GUI受入スイートがBUILD SUCCESSFUL（33秒）となることを確認した。
- Gantt表示証跡: `TaskTableGanttGridGuiAcceptanceTest`のfixtureに実GUIと同じ座標変換・標準バー形式・サイズ更新・描画待ちを設定し、Gantt表示の初期化不足を是正。focused実RobotテストはBUILD SUCCESSFUL（5秒）。空白キャプチャを製品不具合と誤登録しないよう、fixture起因として整理した。
- Gantt描画アサーション: 同ケースに`GanttUI.getNodeAt`の走査を追加し、少なくとも1つのタスクノードが実際に描画されることをfocused実Robotテスト（BUILD SUCCESSFUL、5秒）で確認した。
- 日付連動・20タスク再検証: `TaskDateDependencyGuiAcceptanceTest`（FS/SS/FF/SF、ラグ、Robot日付編集）と`TaskTableGanttGridGuiAcceptanceTest`（20タスク、連続/独立、実マウスのスクロール、Ganttノード描画）を同時実行し、BUILD SUCCESSFUL（11秒、22 actionable tasks）を確認した。MS Project本体との直接比較は実行環境待ち。
- MSPDI依存関係出力回帰: `MsProjectTaskMoveCompatibilityTest.xmlExportPreservesAllMicrosoftDependencyTypesAndDayLag` を追加し、FS/SS/FF/SFの4種類と1営業日ラグ（MSPDI `LinkLag=4800`）の型・先行UID・ラグ出力を検証した。期待値の単位誤りをテスト実行で検出・修正後、focused exchange testがBUILD SUCCESSFUL（4秒）。
- CI回帰確認: `983e7df15` のRelease Run `33309446480` では、負のnative UniqueIdがMSPDI正UIDへ再マッピングされる仕様をテストが直接期待していたため、交換テスト1件が失敗した。XML内の出力Predecessor UIDと比較するよう修正し、`:micrproject_exchange:test` 全体（106 tests）がBUILD SUCCESSFUL（16秒）。製品不具合ではなく追加テストの期待値バグとして#430へ記録。
- CI失敗伝播修正: `gradlew.bat` がGradle失敗後に終了コード0を返し、CIで`BUILD FAILED`後もrelease stagingへ進むことをローカル再現。`ENDLOCAL & EXIT /B %ERRORLEVEL%`を追加し、意図的な存在しないテスト指定で`BUILD FAILED`かつ`EXIT=1`となることを確認した。
- リポジトリclean build回帰: `./gradlew.bat --no-daemon clean build --console=plain` を実行して全モジュールのclean再コンパイル・テストを完走させ、続く `./gradlew.bat build --console=plain` で`BUILD SUCCESSFUL`かつ`EXIT=0`を確認した。
- clean成果物GUI回帰: clean build後に`./gradlew.bat :micrproject_ui:guiTest --max-workers=1 --console=plain`を実行し、installDist再生成を含む16 GUIテストクラスがBUILD SUCCESSFUL（41秒）となることを確認した。
- 配布物ファイル互換性回帰: `./gradlew.bat verifyPackagedFileImports --console=plain` を実行し、installDistのclasspath検証、`Commercial construction project plan.mpp`（145 tasks）および`.pod`（145 tasks）の実配布レイアウト読込がBUILD SUCCESSFUL（5秒）となることを確認した。
- #438 ロギング診断改善: MPXJ/POIのLog4j API向け`log4j-to-slf4j:2.24.3`ブリッジをexchangeへ追加。`verifyPackagedFileImports`実行時の「could not find a logging provider」警告が消え、MPP/POD（各145タスク）読込、`:micrproject_exchange:test`（BUILD SUCCESSFUL、15秒）、`TaskDateDependencyGuiAcceptanceTest`（11秒）、`:micrproject_ui:test`（53秒）を再確認した。
- リボン狭幅回帰: `RibbonTabGuiAcceptanceTest.narrowRibbonExposesCollapsedCommandsThroughMousePopup` で900px幅の折りたたみポップアップ生成と`RibbonHideSelectedTasks`配送を確認（focused BUILD SUCCESSFUL）。同じ画面の折りたたみボタンを`Robot`実座標でクリックするとイベントが発火しない環境差を再現し、画面座標・サイズを#430へ記録。ポップアップ生成自体は`doClick`で回帰防止し、実マウス経路は追加調査対象とした。
- GUI受入ハング回帰: `TaskDateDependencyGuiAcceptanceTest`の警告ダイアログ監視をSwing Timerへ変更し、モーダルEDTと監視スレッドの競合を解消。不正先行入力focusedケース（5秒）と全16クラスGUIスイート（37秒）がBUILD SUCCESSFULとなることを確認した。テスト同期不備として#430へ記録。
- GUI受入ダイアログ識別回帰: 警告／エラータイトルだけをSwing Timerの解除対象に限定し、他の表示ダイアログを誤って閉じないようにした。不正日付・不正先行入力focusedケース（6秒）と全16クラスGUIスイート（38秒）がBUILD SUCCESSFUL。
- 配布レイアウト再確認: GUI受入テスト用installDist再生成後に`verifyPackagedFileImports`を実行し、MPP/POD（各145タスク）の実配布レイアウト読込と互換classpathを確認（BUILD SUCCESSFUL、5秒）。
- clean build回帰（最新GUI同期修正後）: `./gradlew.bat --no-daemon clean build --console=plain`を全54タスク実行し、全モジュールのテスト・配布アーカイブ生成までBUILD SUCCESSFUL（2分37秒）。JDKのUnsafe/deprecation等の既存コンパイル警告のみで、失敗なし。
- GUI-NC-11/#395: 開くダイアログを複数選択モードにし、選択された全プロジェクトを順に開く`FileChooserProvider.chooseFileNames`／`GraphicManager.openLocalProject`経路を追加。`SwingFileChooserProviderTest`でopen=true、save=falseの選択モードを確認し、focused UI testはBUILD SUCCESSFUL。ネイティブOSダイアログの実Robot選択は環境依存の残作業。
- #395 API互換性回帰: `UiServicesFileChooserProviderTest`で、旧来の単一選択プロバイダが1件リストへ変換され、キャンセル（null）が空リストになることを確認（BUILD SUCCESSFUL）。
- #395 nullプロバイダ回帰: カスタムchooserがnullリストを返しても`GraphicManager.openLocalProject`がキャンセルとして安全に終了するガードを追加。core/UI focusedテスト（20 actionable tasks、13秒）がBUILD SUCCESSFUL。
- #395 複数選択順序回帰: `SwingFileChooserProvider.selectedFileNames`で複数選択の全ファイルを順序どおり返し、null要素と空配列を安全に扱うことを確認（UI focused test BUILD SUCCESSFUL、6秒）。
- #395 統合GUI再確認: 複数選択変換ヘルパー追加後にinstallDistを再生成し、全16クラス実Robot GUI受入スイートがBUILD SUCCESSFUL（40秒）となることを確認。
- #395 相対パス境界回帰: 複数選択結果の先頭ファイルに親ディレクトリがない場合も最終アクセス先保存でNPEにならないガードを追加し、`SwingFileChooserProviderTest`を再実行（BUILD SUCCESSFUL、3秒）。
- #395 UI回帰全体: 相対パスガード後に`:micrproject_ui:test`全体を実行し、BUILD SUCCESSFUL（52秒）。
- #228 ResourcePool容量回帰: `ResourcePool.findById`の既知リソース件数を逆引きMapの初期容量へ反映し、`ResourcePoolIdentityTest`（BUILD SUCCESSFUL、7秒）でID検索・未存在検索の挙動不変を確認。
- #228 core全体回帰: ResourcePool逆引きMap容量改善後に`:micrproject_core:test`全体を再実行し、スケジューリング・依存関係・リソース処理を含めBUILD SUCCESSFUL（14秒）。
- #395後続GUI回帰: null結果ガード後にinstallDistを再生成し、全16クラスの実Robot GUI受入スイートを再実行（BUILD SUCCESSFUL、44秒）。
- #228 MutableNodeHierarchy容量回帰: 既知件数を使ってリソース／タスク索引Mapを事前確保し、`ResourcePoolIdentityTest`（BUILD SUCCESSFUL、3秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、13秒）を再実行。
- CI節目確認: commit `6cbb74127` のPages Run `33315015452` はsuccess、Release Run `33315015982` はpendingのため完了待ち。
- #228 PercentWorkCompleteService容量回帰: 収集済み葉タスク数／子ノード数が既知の一時リストを事前確保し、`NormalTaskPercentCompleteTest`（BUILD SUCCESSFUL、4秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、12秒）を再実行。
- #228 ProjectFactory容量回帰: `getCloseProjectsOnServerJob(Collection)` の入力プロジェクト数をローカル／サーバー別リストの事前容量へ反映し、`ProjectFactoryClosingTest`（BUILD SUCCESSFUL、4秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、12秒）を再実行。
- #228 階層操作容量回帰: `MutableNodeHierarchy` の子孫収集と `DefaultNodeModel` の移動候補を入力ノード数で事前確保し、`DefaultNodeModelTest`（BUILD SUCCESSFUL、5秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、14秒）を再実行。
- #228 OperationLog容量回帰: 入力操作数／JSON配列長が既知の協調ログMap・一時リストを事前確保し、`OperationLogTest`（BUILD SUCCESSFUL、3秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、13秒）を再実行。
- #228 ProjectMergeService容量回帰: 外部タスク件数が既知の変更ノード通知リストを事前確保し、`:micrproject_core:test`全体（BUILD SUCCESSFUL、15秒）を再実行。
- #228 RemovalSnapshot／終了通知容量回帰: 既知のルート／ID件数をエントリ・コールバック通知リストへ反映し、`DefaultNodeModelTest`＋`ProjectFactoryClosingTest`（BUILD SUCCESSFUL、4秒）と`:micrproject_core:test`全体（BUILD SUCCESSFUL、13秒）を再実行。
- #228 MutableNodeHierarchy通知容量回帰: 削除ルート数／最低1件を削除・移動通知リストへ反映し、`:micrproject_core:test`全体（BUILD SUCCESSFUL、14秒）を再実行。
