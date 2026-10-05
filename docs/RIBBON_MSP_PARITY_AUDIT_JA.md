# MSP 2024 Ribbon parity audit

監査日: 2026-10-05
基準仕様: issue [#453](https://github.com/tetsuji16/ProjectLibre/issues/453) の MSP Standard / Professional 2024 Windows デスクトップ互換仕様。
画面のスタイル基準: issue [#765](https://github.com/tetsuji16/ProjectLibre/issues/765)。
機能監査・受入証跡: issue [#769](https://github.com/tetsuji16/ProjectLibre/issues/769)。

## 判定ルール

- **実装あり**: 実画面にコマンドがあり、同じ意味の既存 Action と結果検証がある。
- **部分実装**: 一部の面・条件・状態だけが実装または証明されている。
- **不足**: 標準仕様にある面またはコマンドがリボンから使えない。
- **未検証**: 実装は見えるが、モデル結果、Undo/Redo、保存再読込、無効状態の証拠が足りない。
- **製品差**: MSP の機能ではない microProject 固有機能。MSP の標準コマンド群と区別する。

ボタンが表示されること、Action がdispatchされたこと、構造テストが現行配置を固定していることだけでは「実装あり」と判定しない。基準仕様にない挙動は文書由来の互換判断として明記し、実機測定値とは扱わない。

## 標準面の差分

| 面 | MSP 2024 基準 | 現行 microProject | 判定と次の確認 |
|---|---|---|---|
| File | Backstage の左ナビゲーションと詳細面: New/Open/Info/Save/Save As/Print/Export/Close/Account/Options。非対応の Share は出さない | `FileRibbonTask` の通常タブに File/Exchange/Print/Preferences のバンドを表示 | **不足**。Backstage 専用の表示・戻り操作・File選択時のタブ選択状態を実装。既存ファイル操作は同じActionを再利用 |
| Task — グループ | Microsoft Support は Task > Schedule に Link/Unlink を明記し、Task Mode と Indent も Task > Schedule と案内する。 | Clipboard, Insert, Outline, Dependencies, Properties, Schedule, Editing。Schedule は Mark on Track/Update Tasks/Link/Unlink/Indent/Outdent/Manual/Auto を含む | **部分実装**。2026-10-05に明示的な公式 group membership に合わせ Link/Unlink/Indent/Outdent/Task Mode を Schedule へ移した。既存の幅優先縮退順を保ち、標準幅でもProperties/Resourcesが先に隠れないようScheduleの列位置は保持した。Robot画像と構造回帰を更新。View/Font/Tasks等のグループと他の標準コマンドは不足。Microsoft資料は全グループの順序を明記しておらず、順序は未検証 |
| Task — 既存コマンド | Schedule系にIndent/Outdent/Link/Unlink/Move Up/Down。Task Mode、進捗、挿入、編集の選択条件を適用 | 対応する既存Actionが複数ある。Task Mode、Mark on Track等は現在のリボンに存在 | **未検証**。各Actionを選択なし/複数選択/読取専用/Undo/Redo/保存再読込で監査。ボタンの押下後に選択状態を残すかも明記 |
| Resource — Level | Level Selection, Level Resource, Level All, Leveling Options, Clear Leveling, Next Overallocation | `ResourceLevelRibbonBand` は `RibbonLevelSelection` と `RibbonLevelResources`。前者は選択タスクを平準化し、後者は既存のプレビューdialogを開く | **部分実装**。Level SelectionはCommandId、正規Action、Undo/Redo、MPO再読込をRobotで確認済み。Level Resource/All/Options/Clear/Next Overallocationは未実装または意味の区別が未確認 |
| Resource — その他 | View, Assignments, Insert, Properties, Level。共有poolは条件付き | Resource Sheet/Usage等のView経路、Insert Resource/Information/Timesheet/Team Filter/Pool経路 | **部分実装**。発見順、Team Planner/Other Views、Resource Information/Notes/Details、Material/Cost追加を照合。Pool機能は接続条件を確認 |
| Report | 組込みレポートギャラリーと Custom/Recent/Visual Reports。Excel/Visio依存機能は条件付き | Custom Report, Histogram, Charts, Task/Resource Usage, CCPM Buffer Status | **部分実装**。標準組込みレポートの種類・カテゴリ・Recent/Visual Reportsの対応条件を照合 |
| Project | Properties, Schedule, Status, Reports, Proofing。Baselineの複数slot/範囲選択を含む | Information/Calendar/Projects、Schedule/Subproject、Status、Baseline、CCPM各band | **部分実装**。Custom Fields/Links/WBS/Visual Reports/Proofing、複数baseline操作、および進捗変更の一括Undoを照合 |
| View | Task/Resource Views, Data, Zoom, Split View, Window, Macros。Entire Project/Selected Tasksを含む | Task/Resource/Window/Data/Zoom band。Gantt/Tracking/Network/WBS/Calendar/Timeline等あり | **部分実装**。Task Board/Other Views、Highlight/Outline/Tables、Entire Project/Selected Tasks、Split View制約、Window/Macrosを照合 |
| Help | Help/Training/Feedback/About | Viewの後ろに標準Helpタブ。Documentation/Aboutあり | **部分実装**。HelpとAboutは存在。Training/FeedbackはMS連携を偽装せず、製品固有リンクを置くか明示的に省略 |

## 文脈タブと共通操作

| 状態 | MSP 2024 基準 | 現行 | 判定と次の確認 |
|---|---|---|---|
| Quick Access Toolbar | Save/Undo/Redoを同じActionへ接続 | Save/Undo/Redoあり | **未検証**。他経路と同一Action/Undo履歴であること、read-only時の有効状態を確認 |
| Gantt / Tracking Gantt | Gantt Chart Tools — Format。表示・バー・テキスト・列/レイアウト等 | `FormatRibbonTask`を両viewで使い、Progress/Labels/Gridlines/Critical Chain/Timescale/Bar/Text/Layoutを提供 | **部分実装**。共有面がview固有の適切な機能を示すか、view変更時に状態が分離・保存されるか確認 |
| Timeline | Timeline Tools — Format | Timelineの表示切替はあるが、専用文脈タブはない | **不足**。選択タスクから追加、詳細表示、コピー/日付書式と物理routeを調査 |
| Network Diagram | Layout/Box Styles/Data Templates/Text Styles/Drawing | Network Format contextual tabはDisplay/Labels/Gridlines/Critical Chain/Layoutを共有 | **部分実装**。Network固有のレイアウト/box/text/drawing項目とview stateを照合 |
| Calendar | Bar Styles/Layout/Text Styles/Gridlines | Calendar Format contextual tabはBar Styles/Layout | **部分実装**。Text Styles/GridlinesとCalendarのphysical routeを確認 |
| Report object | Report Tools — Designおよび選択object固有のChart/Table/Picture/Drawing tools | Report tabはあるがReport object contextual tabsは未確認 | **不足または未実装**。選択object種類別の発見場所と無効状態を確定 |
| 独自機能 | 標準MSPタブと区別した製品固有領域 | 現行標準面にCCPM等のmicroProject独自機能が混在 | **要整理**。CCPM等は `microProject` 面に分離し、MSP標準の意味を変更しない |
| キーボード/支援技術 | KeyTips、Tab/矢印/Space/Enter、Accessible name、selected/disabled | root-paneに一元化したshortcut層はある。全コマンドのKeyTip/accessible state matrixは未確認 | **未検証**。全標準/文脈コマンドのキーボード・支援技術・selection state一覧を生成し、欠落を検出 |
| 狭幅/折りたたみ | Microsoft Support はコマンドの表示/非表示、リボンの表示状態とカスタマイズを記載するが、Project desktop の各幅境界や具体的なgroup reflowを数値規定しない | `ModernRibbonPanel` が実幅に合わせて直接コマンド、縮小コマンド、代表アイコン付きグループメニューへ遷移 | **部分実装**。同一Robotウィンドウで1200/672/320pxを通り、Task > Pasteを狭幅メニューから実行。全タブ・ja/en・100/125/150%の幅境界matrixは未検証。記録画像 `ribbon-task-wide-1200.png` / `ribbon-task-medium-672.png` / `ribbon-task-narrow-320.png`。Microsoftの「リボンサイズを縮小できない」はユーザーによる固定サイズ変更についての記載で、レスポンシブ遷移の完全な実装仕様ではないため、縮小表示との同一性は断定しない |
| 白いcommand surface | タブ直下の白い面、丸みと余白は画像基準 | FlatLaf `JPanel` の `arc: 18` と左右16px insetで白い面を丸角表示。グループを同じ白い面に配置 | **部分実装**。U-40のpixel assertionで外角がchrome色、内側が白であることを確認。提供されたPowerPoint for Macの画像と外形は近づけたが、影とOS別の差まで完全一致したとは判定していない。Microsoft Supportは丸みや余白を規定していない |
| Visual system | Officeテーマ、選択/hover/pressed/disabled/focus状態とgeometry | 単一Swing renderer。ライト面はchrome `#F3F2F1`、command surface白。Flamingo ribbon rendererは使わない | **部分実装**。値は参照画像からの観測でMicrosoftのRGB normative specではない。#765の画像比較・状態matrixが完了条件 |

## Microsoft Support の Ribbon Display Options / customization 監査

基準: Microsoft Support [Office でリボンをカスタマイズする](https://support.microsoft.com/ja-jp/office/foundations-experiences/customize-the-ribbon-in-office)。適用先に Project Standard / Professional 2024 が含まれる。色の個別変更は同ページで不可とされるため、リボン個別色設定は実装漏れに数えない。

| 機能 | 現行状態 | 判定 |
|---|---|---|
| Ribbon Display Options: Auto-hide / Show Tabs Only / Always Show | `OfficeChromePanel` の3項目ラジオメニュー、`ModernRibbonPanel` の状態、`RibbonDisplayPreferences` によるユーザー設定保存あり。更新したRobotは3項目すべてを選び、非表示から復帰することを確認 | **機能は部分実装**。メニュー配置の切れを修正済み。ボタンは現在タイトルバー右側にあり、#575 の完了記録が要求したリボン右下配置と一致するかは未解決。Auto-hide 後のAltによる一時表示と自動復帰は物理検証なし |
| 表示切替入力 | Ctrl+F1 とタブ右クリックの折りたたみ/復帰に物理テストあり。タブのダブルクリックも Microsoft が明記 | **実装あり**。タブのダブルクリック物理Robotを追加し、折りたたみ/復帰を確認 |
| ユーザー定義タブ | 追加、並べ替え、名前変更、表示/非表示、カスタムタブ削除。Fileは固定かつ非表示不可 | **不足**。現行メニュー／設定に編集UIなし。#770 |
| グループとコマンド | カスタムグループ追加/削除/改名/順序変更、コマンド追加/削除/改名/順序変更。既定コマンドの名前・アイコン・順序は固定 | **不足**。現行設定に編集UIなし。#770 |
| 初期化と共有 | 全設定/選択タブのリセット、RibbonとQAT設定のexport/import | **不足**。現行設定に機能なし。#770 |

2026-10-05 の Robot で、表示オプションメニューが画面外へ切れる回帰を発見した。原因はメニューをボタンの上に配置したまま、ボタンをタイトル領域へ移したこと。メニューをボタンの下に右揃えし、ウィンドウ内に収まる境界assertionと4状態のスクリーンショットを追加した。Ctrl+F1 の初回失敗はRobotの前面ウィンドウ条件が不安定だったためで、テストウィンドウを前面固定すると単独GUIテストは成功した。これは製品のキー入力不具合ではなく、GUI fixture の問題だった。

## Resource Level Selection command contract (基礎仕様)

Microsoft Supportの [Distribute project work evenly (level resource assignments)](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments) はLevel Selectionを「選択タスクだけを平準化」と定義する。未選択タスクを移動しないという扱いはその仕様から導く互換判断であり、MSP実測ではない。

```text
Command: CommandId.RESOURCE_LEVEL_SELECTION (`LevelSelectionAction` -> canonical task command)
User routes: Resource > Level > Level Selection。既存 `LevelResourcesAction` は設定/preview dialog を開く別コマンドとして維持。
Selection: active documentのTask selection snapshot。空選択はdisabled/rejected。selected task setは実行開始時に一度だけ解決。
Allowed state: writable project、計算/編集transactionが進行中でない。completed/inactive/read-only/summary等の個別対象可否をservice契約に合わせる。
Model before → after: 選択タスクのみresource-leveling由来のdelay/splitを変更。計画時は未選択assignmentを固定負荷として含める。Apply後のschedule再計算に伴う依存先の派生日付変更は許容し、LevelingDelay/splitを未選択タスクへ直接付与しない。
Visible before → after: active schedule/resource viewで変更を反映し、選択を保持。実結果の変更行と unresolved conflictsを示す。
Undo/Redo: 一つのresource-leveling transactionをCtrl+Z/Ctrl+Y各1回で正確に復元/再適用。
Persistence: native project save/reload後、対象タスクのdelay/split、未選択タスクのdelay/split不変、依存関係による派生日付を確認。
Invalid state: empty selection/read-only projectはdisabledまたはreason付きrejected。silent no-op禁止。
Diagnostic result: changed/rejected/failed、selected stable task IDs、変更task IDs、conflict数、active view。Robot: `TaskInformationRibbonGuiAcceptanceTest.selectedTaskLevelingUsesTheResourceRibbonAndSupportsUndoRedoAndPersistence`。
```

## 証拠と追跡

- Ribbon構成の実装: `modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties`
- コマンド所有権/placement: `modules/microproject_ui/src/main/java/com/microproject/ui/ribbon/RibbonCommandCatalog.java`
- 一般ボタンの physical-route一覧: `docs/RIBBON_COMMAND_GUI_TEST_CASES_JA.md`。dispatch成功は結果成功の証明ではない。
- レイアウト/色の検証契約: `TEST_PLAN.md` U-40 と `OfficeChromePanelVisualSmokeTest`。
- 互換根拠・未完了機能: issue #453、実装受入: issue #769、視覚受入: issue #765。

2026-10-05 Task group order review: Microsoft Support [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project), [Task Mode (task field)](https://support.microsoft.com/en-us/project/task-mode-task-field), and [Top-down planning](https://support.microsoft.com/en-us/project/top-down-planning) explicitly place Link/Unlink, task mode, and Indent under Task > Schedule. The ribbon had placed Link/Unlink under Dependencies, Indent under Outline, and Manual/Auto under Editing. These controls now reside in the Schedule band; command IDs and canonical Actions are unchanged. The Schedule band's column order stays after Properties to preserve the existing responsive collapse order and keep Properties/Resources directly available at standard widths. Microsoft sources do not define the order of every Task group, so this order remains unverified. This is a document-derived group-membership correction, not an empirical MSP run. The rest of the Task tab still differs and this does not establish complete parity.

この表は現時点の敵対的監査であり、合格証明ではない。**不足**/**部分実装**/**未検証**の行を実装・試験証跡で解決するまで、#453/#765/#769を閉じない。
