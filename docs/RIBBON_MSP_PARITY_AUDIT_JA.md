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
| Quick Access Toolbar | Save/Undo/Redoを同じActionへ接続 | Save/Undo/Redoあり | **未検証**。実アプリのja/100キャプチャでQAT Saveが単色tintにより黒い四角に潰れる表示不具合を確認し、アウトライン素材に変更した。共有tint後も内部が透明なことをheadless回帰テストと実画面キャプチャで確認。各ボタンが他経路と同一Action/Undo履歴であること、read-only時の有効状態は引き続き未検証 |
| Gantt / Tracking Gantt | Gantt Chart Tools — Format。表示・バー・テキスト・列/レイアウト等 | `FormatRibbonTask`を両viewで使い、Progress/Labels/Gridlines/Critical Chain/Timescale/Bar/Text/Layoutを提供 | **部分実装**。共有面がview固有の適切な機能を示すか、view変更時に状態が分離・保存されるか確認 |
| Timeline | Timeline Tools — Format | Timelineの表示切替はあるが、専用文脈タブはない | **不足**。選択タスクから追加、詳細表示、コピー/日付書式と物理routeを調査 |
| Network Diagram | Layout/Box Styles/Data Templates/Text Styles/Drawing | Network Format contextual tabはDisplay/Labels/Gridlines/Critical Chain/Layoutを共有 | **部分実装**。Network固有のレイアウト/box/text/drawing項目とview stateを照合 |
| Calendar | Bar Styles/Layout/Text Styles/Gridlines | Calendar Format contextual tabはBar Styles/Layout | **部分実装**。Text Styles/GridlinesとCalendarのphysical routeを確認 |
| Report object | Report Tools — Designおよび選択object固有のChart/Table/Picture/Drawing tools | Report tabはあるがReport object contextual tabsは未確認 | **不足または未実装**。選択object種類別の発見場所と無効状態を確定 |
| 独自機能 | 標準MSPタブと区別した製品固有領域 | 現行標準面にCCPM等のmicroProject独自機能が混在 | **要整理**。CCPM等は `microProject` 面に分離し、MSP標準の意味を変更しない |
| キーボード/支援技術 | KeyTips、Tab/矢印/Space/Enter、Accessible name、selected/disabled | root-paneに一元化したshortcut層はある。全コマンドのKeyTip/accessible state matrixは未確認 | **未検証**。全標準/文脈コマンドのキーボード・支援技術・selection state一覧を生成し、欠落を検出 |
| 狭幅/折りたたみ | Microsoft Support はリボンの表示状態・カスタマイズを記載するが、Project desktop の各幅境界、tab overflow、具体的なgroup reflowを数値規定しない | `ModernRibbonPanel` はグループを直接コマンド／代表アイコン付きmenuへ縮退。タブ行は幅不足時もFileと選択中タブを直接表示し、その他をローカライズ済み「…」menuから選ぶ。menuは既存 `showTab` 経路に戻る | **部分実装／product extension**。tab overflowはMicrosoft/Project機能と主張せず、320pxでの文字切れを防ぐための決定的な拡張として扱う。Robotは日本語・100%でFile/選択タブ固定、Space/Escape、物理popup選択、wide復帰、popup boundsを確認。画像 `ribbon-tabs-narrow-320.png` / `ribbon-tabs-overflow-focused-320.png` / `ribbon-tabs-overflow-popup-320.png`。全標準/文脈タブ・英語・125/150%の幅境界matrixは未検証。Projectの同条件参照画像がないため視覚同一性は断定しない |
| 白いcommand surface | タブ直下の白い面、丸みと余白は画像基準 | FlatLaf `JPanel` の `arc: 18` と左右16px insetで白い面を丸角表示。グループを同じ白い面に配置 | **部分実装**。U-40のpixel assertionで外角がchrome色、内側が白であることを確認。提供されたPowerPoint for Macの画像と外形は近づけたが、影とOS別の差まで完全一致したとは判定していない。Microsoft Supportは丸みや余白を規定していない |
| Visual system | Officeテーマ、選択/hover/pressed/disabled/focus状態とgeometry | 単一Swing renderer。ライト面はchrome `#F3F2F1`、command surface白。Flamingo ribbon rendererは使わない | **部分実装**。値は参照画像からの観測でMicrosoftのRGB normative specではない。#765の画像比較・状態matrixが完了条件 |

## Microsoft Support の Ribbon Display Options / customization 監査

基準: Microsoft Support [Office でリボンをカスタマイズする](https://support.microsoft.com/ja-jp/office/foundations-experiences/customize-the-ribbon-in-office)。適用先に Project Standard / Professional 2024 が含まれる。色の個別変更は同ページで不可とされるため、リボン個別色設定は実装漏れに数えない。

| 機能 | 現行状態 | 判定 |
|---|---|---|
| Ribbon Display Options: Full-screen mode / Show Tabs Only / Always Show Ribbon + Quick Access Toolbar | `OfficeChromePanel` に見出し、3つの排他的なチェックマーク状態、区切り、QATの表示切替を持つメニューがあり、ユーザー設定を保存する。表示中はリボン右下、Full-screen modeではタイトル領域に操作ボタンを置く。Robotは3状態の変更、復帰、QATの非表示/再表示、メニュー境界を確認 | **状態と物理経路を実装済み**。今回のユーザー提供画像はWordの表示オプション画像であり、Projectのキャプチャや全Office製品共通のピクセル仕様とは扱わない。MS資料は3状態と切替を規定する。Auto-hideは物理「その他」ボタンとAltキーで一時表示し、Ribbonタブ操作中は表示を保ち、文書クリックで設定モードを維持したまま再び隠れることをRobotで確認。新規に本番 `ProjectLibreShell` を再作成する物理テストを追加し、保存モードが新RibbonControllerへ復元されることを確認。日本語/英語 × 100/125/150% の同クラス6 legがローカルで成功。SHA `0fe7ac8f3` の[hosted GUI audit](https://github.com/tetsuji16/ProjectLibre/actions/runs/37304795984)は全件成功。専用クラス2テストと撮影をja/100%の基準スイートで再実行し、視覚マトリクス5 legも成功 |
| 表示切替入力 | Ctrl+F1 とタブ右クリックの折りたたみ/復帰に物理テストあり。タブのダブルクリックも Microsoft が明記 | **実装あり**。タブのダブルクリック物理Robotを追加し、折りたたみ/復帰を確認 |
| ユーザー定義タブ | 追加、並べ替え、名前変更、表示/非表示、カスタムタブ削除。Fileは固定かつ非表示不可 | **不足**。現行メニュー／設定に編集UIなし。#770 |
| グループとコマンド | カスタムグループ追加/削除/改名/順序変更、コマンド追加/削除/改名/順序変更。既定コマンドの名前・アイコン・順序は固定 | **不足**。現行設定に編集UIなし。#770 |
| 初期化と共有 | 全設定/選択タブのリセット、RibbonとQAT設定のexport/import | **不足**。現行設定に機能なし。#770 |

2026-10-05 の Robot で、表示オプションメニューが画面外へ切れる回帰を発見した。原因はメニューをボタンの上に配置したまま、ボタンをタイトル領域へ移したこと。メニューをボタンの下に右揃えし、ウィンドウ内に収まる境界assertionと4状態のスクリーンショットを追加した。Ctrl+F1 の初回失敗はRobotの前面ウィンドウ条件が不安定だったためで、テストウィンドウを前面固定すると単独GUIテストは成功した。これは製品のキー入力不具合ではなく、GUI fixture の問題だった。

2026-10-05 Auto-hide temporary reveal hostile review (`eda72e991`, Robot coverage extended in `844d7ecf8` and current follow-up): the first More-button click now reveals only; a separate following click opens the mode menu. This avoids a double-dispatch between the JButton action and FlatLaf mouse-release fallback. Temporary reveal state is not persisted as a mode change. A transient AWT mouse listener and focus-owner listener exist only while revealed and are removed on dismissal, mode change, or component disposal. Robot sequence: choose Auto-hide → More temporarily reveals → select Task and verify ribbon stays open → click document and verify the ribbon hides while Auto-hide remains selected → press Alt and verify temporary reveal → click document and verify it hides again. Captures: `ribbon-display-auto-hide-temporary-reveal-ja-1.0.png`, `ribbon-display-auto-hide-return-to-document-ja-1.0.png`, and `ribbon-display-auto-hide-alt-reveal-ja-1.0.png`. Local ja/100 focused GUI and display-mode unit tests passed. Production preference persistence is now verified across destruction and recreation of `MainRibbonFrame`; a full process restart, remaining position/reference reconciliation, and the exact-head hosted locale/DPI audit are still pending.

2026-10-05 display-options persistence and capture hostile review: production `ProjectLibreShell` already binds selected mode changes to user preferences, but existing Robot tests bypassed that shell and could not catch persistence wiring regressions. Added a physical menu journey that selects Tabs Only, disposes the first production shell, creates a fresh `MainRibbonFrame` through the same installer, and verifies the new controller both loads and renders the saved mode. Local GUI runs passed for ja/en × 100/125/150% after the 100% legs. Screenshot review found that Robot captured the display popup before Swing finished painting at higher scale, yielding a blank white rectangle even though visible-item and physical-click checks passed. The test capture now waits for AWT idle plus 250 ms; refreshed ja/100 and en/150 captures show the actual localized heading, three mode choices, selected checkmark, and QAT option. This was a test evidence race, not a product popup defect. Preference re-creation is verified; a full application process restart was not separately tested. Commit `0fe7ac8f3` completed the [hosted Windows GUI matrix](https://github.com/tetsuji16/ProjectLibre/actions/runs/37304795984): complete Japanese 100% suite first, then the prescribed five locale/scale visual legs.

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

## Resource Level All command contract (全体平準化)

Microsoft Project Standard 2024の [Distribute project work evenly (level resource assignments)](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments) は、Resource > Level > Level Allを計画内の全リソース/全タスクへ適用し、Level Selectionを選択タスクだけへの適用と区別する。UI文書はUndo単位、押下ラッチ、選択保持、MPO永続化の細部を規定しないため、ここはOfficeの一時実行コマンドとして定義した**document-derived compatibility decision**であり、MSP実測ではない。

```text
Command: CommandId.RESOURCE_LEVEL_ALL (`LevelAllAction` -> canonical task command)
Scope: active writable project全体。タスク選択を要求せず、選択中でも選択集合にscopeを限定しない。serviceのeligible判定に従う。
State: momentary command。処理後にpressed/selected状態を保持しない。
Model/view: ResourceLevelingService.preview(project, null, Options.defaults())のPlanを一度適用。タスク/セル選択を保持し、スケジュール表示を更新する。
Undo/Redo: 一つのPlan.applyとUndoableEditで全変更をUndo/Redoする。
Persistence: MPO保存/再読込後もLevelingDelay/splitsを保持する。
Evidence: `TaskInformationRibbonGuiAcceptanceTest.levelAllUsesEveryProjectResourceWithoutChangingTaskSelectionAndSupportsUndoRedoAndMpo` は2リソースの競合、物理的なResource > Level Allクリック、selection保持、単一Undo/Redo、MPO roundtripを検証する。`RibbonButtonBehaviorTest.levelAllIsEnabledWithoutSelectionAndDoesNotLatchAfterNoChange` は空selection時の有効性と非ラッチを検証する。
Limit: 本sliceは既定Optionsを用いる。リソース稼働率/タスク状態等の対象資格は現在のResourceLevelingService実装に委ね、MSPのLeveling Options全機能を主張しない。
```

## 証拠と追跡

- Ribbon構成の実装: `modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties`
- コマンド所有権/placement: `modules/microproject_ui/src/main/java/com/microproject/ui/ribbon/RibbonCommandCatalog.java`
- 一般ボタンの physical-route一覧: `docs/RIBBON_COMMAND_GUI_TEST_CASES_JA.md`。dispatch成功は結果成功の証明ではない。
- レイアウト/色の検証契約: `TEST_PLAN.md` U-40 と `OfficeChromePanelVisualSmokeTest`。
- 互換根拠・未完了機能: issue #453、実装受入: issue #769、視覚受入: issue #765。

2026-10-05 Task group order review: Microsoft Support [Link tasks in a project](https://support.microsoft.com/en-us/project/link-tasks-in-a-project), [Task Mode (task field)](https://support.microsoft.com/en-us/project/task-mode-task-field), and [Top-down planning](https://support.microsoft.com/en-us/project/top-down-planning) explicitly place Link/Unlink, task mode, and Indent under Task > Schedule. The ribbon had placed Link/Unlink under Dependencies, Indent under Outline, and Manual/Auto under Editing. These controls now reside in the Schedule band; command IDs and canonical Actions are unchanged. The Schedule band's column order stays after Properties to preserve the existing responsive collapse order and keep Properties/Resources directly available at standard widths. Microsoft sources do not define the order of every Task group, so this order remains unverified. This is a document-derived group-membership correction, not an empirical MSP run. The rest of the Task tab still differs and this does not establish complete parity.

2026-10-05 hostile review of the responsive group proxy found two behavior/accessibility defects in the common collapsed-band path: at 1200px the Editing group appeared as its first command's icon (Delete), with no indication it opened a group menu, and `styleRibbonLargeButton` forced the proxy non-focusable. Added a down disclosure mark, localized tooltip/accessibility description, and keyboard focus support. The shared ribbon-command border now paints the focus owner with the MSP-green accent so keyboard focus is visible. Robot verifies Space and Enter open the group menu, Escape closes it, then exercises the mouse command route. Screenshot review caught a missing-glyph square for the first triangle character and an absent focus outline; replaced it with a Windows-rendered triangle, added shared focus-state painting, and inspected the refreshed focused screenshot. Exact Project ribbon parity remains unverified; this corrects only the shared group-proxy discoverability and keyboard route.

2026-10-05 production-window visual review: `CanonicalProjectWindowGuiAcceptanceTest.fullRibbonWindowShowsAnEmptyTaskBetweenTwoTasks` captures the actual `MainRibbonFrame` / `GraphicManager` ribbon and Gantt spreadsheet, unlike `RibbonTabGuiAcceptanceTest`, which isolates the ribbon component. The full-window screenshot exposed the QAT Save icon as a solid dark square. `OfficeChromePanel.resolveActionIcon` applies the shared monochrome tint; the former filled multicolor floppy SVG lost its internal contrast under that transform. QAT Save now uses a dedicated outline SVG whose transparent interior survives the same shared tint. Added a pixel-level icon regression assertion and re-captured the full window at Japanese 100%. Save's command/action path is unchanged. QAT same-Action/Undo/read-only state, general view density, and exact MSP visual parity remain open.

この表は現時点の敵対的監査であり、合格証明ではない。**不足**/**部分実装**/**未検証**の行を実装・試験証跡で解決するまで、#453/#765/#769を閉じない。
