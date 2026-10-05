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
| Task — グループ | View, Clipboard, Font, Schedule, Tasks, Insert, Properties, Link/Planner, Editing | Clipboard, Insert, Outline, Dependencies, Properties, Tracking, Editing | **部分実装**。現行列挙に Font/Format Painter/Respect Links/Inactivate/Inspect/Select/Clear/Add to Timeline 等が不足。MSP標準と製品固有コマンドの配置を照合 |
| Task — 既存コマンド | Schedule系にIndent/Outdent/Link/Unlink/Move Up/Down。Task Mode、進捗、挿入、編集の選択条件を適用 | 対応する既存Actionが複数ある。Task Mode、Mark on Track等は現在のリボンに存在 | **未検証**。各Actionを選択なし/複数選択/読取専用/Undo/Redo/保存再読込で監査。ボタンの押下後に選択状態を残すかも明記 |
| Resource — Level | Level Selection, Level Resource, Level All, Leveling Options, Clear Leveling, Next Overallocation | `ResourceLevelRibbonBand` は `RibbonLevelResources` のみ。押すと平準化設定/プレビューdialogを開く | **不足**。Level Selectionの固定タスク契約、Resource/All実行、Options、Clear、Next Overallocationを独立した正規コマンドへ接続。Resource Leveling ServiceのUndoと既存dialogを再利用 |
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
| 狭幅/折りたたみ | コマンドを黙って隠さず、group overflowで到達可能 | Swing `ModernRibbonPanel` が折りたたみとgroup overflowを提供 | **未検証**。標準/文脈tabを ja/en、100/125/150%、幅境界で全コマンド到達確認 |
| Visual system | Officeテーマ、選択/hover/pressed/disabled/focus状態とgeometry | 単一Swing renderer。ライト面はchrome `#F3F2F1`、command surface白。Flamingo ribbon rendererは使わない | **部分実装**。値は参照画像からの観測でMicrosoftのRGB normative specではない。#765の画像比較・状態matrixが完了条件 |

## Resource Level Selection command contract (基礎仕様)

Microsoft Supportの [Distribute project work evenly (level resource assignments)](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments) はLevel Selectionを「選択タスクだけを平準化」と定義する。未選択タスクを移動しないという扱いはその仕様から導く互換判断であり、MSP実測ではない。

```text
Command: ResourceLevelSelection (ID未採番; 現行標準Actionに追加前)
User routes: Resource > Level > Level Selection。メニュー/shortcut等は同じcanonical commandへ委譲。
Selection: active documentのTask selection snapshot。空選択はdisabled/rejected。selected task setは実行開始時に一度だけ解決。
Allowed state: writable project、計算/編集transactionが進行中でない。completed/inactive/read-only/summary等の個別対象可否をservice契約に合わせる。
Model before → after: 選択タスクのみleveling delay/splitを変更。未選択のassignment/task stateは保持し、固定負荷として候補判定に含める。
Visible before → after: active schedule/resource viewで変更を反映し、選択を保持。実結果の変更行と unresolved conflictsを示す。
Undo/Redo: 一つのresource-leveling transactionをCtrl+Z/Ctrl+Y各1回で正確に復元/再適用。
Persistence: native project save/reload後、対象タスクのdelay/splitと未選択タスク不変を確認。
Invalid state: empty selection/read-only projectはdisabledまたはreason付きrejected。silent no-op禁止。
Diagnostic result: changed/rejected/failed、selected stable task IDs、変更task IDs、conflict数、active view。
```

## 証拠と追跡

- Ribbon構成の実装: `modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties`
- コマンド所有権/placement: `modules/microproject_ui/src/main/java/com/microproject/ui/ribbon/RibbonCommandCatalog.java`
- 一般ボタンの physical-route一覧: `docs/RIBBON_COMMAND_GUI_TEST_CASES_JA.md`。dispatch成功は結果成功の証明ではない。
- レイアウト/色の検証契約: `TEST_PLAN.md` U-40 と `OfficeChromePanelVisualSmokeTest`。
- 互換根拠・未完了機能: issue #453、実装受入: issue #769、視覚受入: issue #765。

この表は現時点の敵対的監査であり、合格証明ではない。**不足**/**部分実装**/**未検証**の行を実装・試験証跡で解決するまで、#453/#765/#769を閉じない。
