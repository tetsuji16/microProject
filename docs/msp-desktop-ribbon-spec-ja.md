# MSP デスクトップ・リボン互換仕様

## 1. 目的と基準

この文書は microProject のリボンを Microsoft Project (MSP) デスクトップ版と
操作互換にするための UI 仕様である。比較対象は **Project Standard 2024 /
Project Professional 2024 / Project Online Desktop Client の Windows デスクトップ版**
とする。Project for the web、Planner の Web UI、Project Web App (PWA) は別製品であり、
このリボンの比較対象に含めない。

ここでいう互換は、タブ順、グループ順、コマンドの発見場所、選択状態に応じた有効化、
コマンド実行結果、キーボード操作およびコンテキスト・タブを含む。画面の一部を画像として
複製することではない。製品名、Microsoft のロゴ、アイコン、スクリーンショット、商標色を
流用せず、機能をクリーンルームで実装する。

MSP のリボンはライセンス、接続先、表示中のビュー、選択オブジェクト、ウィンドウ幅によって
差がある。以下の「必須」はオフラインの通常プロジェクトで再現する標準面、
「条件付き」は該当する機能またはコンテキストがある場合だけ表示又は有効化する面である。

## 2. フレームと共通挙動

1. 左上に Quick Access Toolbar (既定: Save, Undo, Redo) を置く。各コマンドはリボン内の
   同一 Action を呼び、独自の二重実装を持たない。
2. タブの通常順は `File, Task, Resource, Report, Project, View, Help`。開いているビューに
   応じてその右側にコンテキスト・タブを追加する。MSP 互換のベース面に独自コマンドを混在
   させない。
3. `File` は通常のリボン・バンドではなく Backstage である。New / Open / Info / Save /
   Save As / Print / Share (条件付き) / Export / Close / Account / Options を左ナビゲーション
   と詳細ペインで提供する。ローカル版で提供しない Share は無効なダミーを置かず、表示しない。
4. 一つの論理コマンドは一つの Action ID に対応させる。Quick Access、リボン、右クリック、
   ショートカットは同じ Action を使う。選択がない、読み取り専用、計算中などの場合は無効化し、
   実行後に選択、フォーカス、Undo、再描画を同期する。
5. 幅が不足したときはグループを左から順に縮小し、最後にグループ単位のドロップダウンへ畳む。
   コマンドを黙って消さない。ツールチップには名称、短い目的、ショートカット、無効理由を示す。
6. Alt の KeyTip、Tab/矢印キーでの移動、Space/Enter 実行、スクリーンリーダー名、状態の
   `selected`/`disabled` を全コマンドに持たせる。既存の root-pane InputMap/ActionMap を唯一の
   ショートカット登録層とする。

## 3. 通常タブの正規インベントリ

### Task

標準のグループ順は次のとおりである。タスク選択を必要とするものは、選択がないと無効化する。

| グループ | 必須コマンド | 主な条件・結果 |
|---|---|---|
| View | Gantt Chart（ビュー選択） | 現在ビューをタスク・ビューへ切替 |
| Clipboard | Paste, Cut, Copy, Format Painter | 表・セル・タスク選択に応じて有効化 |
| Font | フォント、サイズ、B/I/U、色、背景 | 表の選択範囲の書式を変更 |
| Schedule | % Complete、Mark on Track、Respect Links、Inactivate、Move Task、Inspect | 進捗更新、リンク尊重、無効化、再スケジュール、問題検出 |
| Tasks | Manually Schedule、Auto Schedule、Task Mode | 選択タスク又は新規タスクのモードを変更 |
| Insert | Task、Summary、Milestone、Recurring Task、Subproject | タスク挿入。Summary は選択行の直前に親を作る挙動を MSP と照合 |
| Properties | Information、Notes、Details、Add to Timeline | タスク情報、注記、詳細ペイン、タイムライン掲載 |
| Link To / Planner | Planner とのリンク | 接続・ライセンスがある場合だけ表示。オフライン互換面では省略可 |
| Editing | Find、Clear、Scroll to Task、Select | 検索、内容消去、可視領域への移動、選択操作 |

`Indent / Outdent`、`Link / Unlink`、`Move Up / Move Down` は MSP では Schedule 系の操作として
発見できなければならない。互換実装では視認上は Schedule 内に置き、別グループを作る場合も
同じメニューと Action に到達させる。`Ctrl+X/C/V`、`Delete`、`Insert`、`F2`、`Ctrl+F` の
キーバインドは一度だけ登録する。

### Resource

標準のグループ順は `View, Assignments, Insert, Properties, Level` で、接続や共有リソースを
実装する場合に `Resource Pool` を後置する。

| グループ | 必須コマンド |
|---|---|
| View | Resource Sheet、Resource Usage、Team Planner、Other Views |
| Assignments | Assign Resources、Team Planner / assignment 操作 |
| Insert | Add Resources（Work / Material / Cost の選択を含む） |
| Properties | Resource Information、Notes、Details |
| Level | Level Selection、Level Resource、Level All、Leveling Options、Clear Leveling、Next Overallocation |
| Resource Pool（条件付き） | Share Resources、Resource Pool、Substitute Resources、Refresh / resolve pool |

リソースのレベリングは単なる表示変更ではなくスケジュールを変更する。Undo、Clear Leveling、
保存・再読込までモデルの整合性を保つ。

### Report

`View Reports` のギャラリーを主面とし、`Dashboards, Resources, Costs, In Progress` の組込み
レポート、`New Report`、`Custom`、`Recent`、`Visual Reports`、`Compare Projects` を提供する。
組込みレポートの最低セットは Project Overview、Project Overview Dashboard、Resource Overview、
Cost Overview、Burndown、Milestone、Critical Tasks、Late Tasks とする。ライセンスや外部 Excel /
Visio が必要な Visual Reports は条件付きである。

### Project

グループ順は `Properties, Schedule, Status, Reports, Proofing` とする。

| グループ | 必須コマンド |
|---|---|
| Properties | Project Information、Custom Fields、Links Between Projects、WBS、Change Working Time |
| Schedule | Calculate Project、Set Baseline、Clear Baseline、Move Project |
| Status | Status Date、Update Project、Mark on Track |
| Reports | Visual Reports（条件付き） |
| Proofing | Spelling、Proofing language |

Baseline は複数スロットと選択タスク／全プロジェクトを区別する。Status Date、Update Project、
Mark on Track は進捗とスケジュールに影響するため、同じトランザクションと Undo 単位にする。

### View

グループ順は `Task Views, Resource Views, Data, Zoom, Split View, Window, Macros` とする。

| グループ | 必須コマンド |
|---|---|
| Task Views | Gantt Chart、Task Usage、Task Board（対応時）、Network Diagram、Calendar、Other Views |
| Resource Views | Team Planner、Resource Usage、Resource Sheet、Other Views |
| Data | Highlight、Filter、Group By、Sort、Outline、Tables |
| Zoom | Timescale、Zoom、Entire Project、Selected Tasks |
| Split View | Timeline、Details、Resource Graph |
| Window | New Window、Arrange All、Hide / Unhide、Switch Windows |
| Macros | Macros、Visual Basic（対応時のみ） |

Timeline と Details は同時表示不可という MSP の制約を守る。Data の Filter / Group By / Sort は
シート、Gantt、使用状況など現在ビューのデータ種別に合わせた候補を表示する。

### Help

Help、Training、Feedback、About を提供する。Microsoft のアカウント連携や Feedback 送信を
実装しない場合は、microProject のドキュメントと Issue URL に置換し、MSP の機能であるかのように
偽装しない。

## 4. コンテキスト・タブ

コンテキスト・タブは常時出してはならない。少なくとも次をビュー又は選択に連動させる。

| 条件 | リボン名 | 必須面 |
|---|---|---|
| Gantt Chart / Tracking Gantt | Gantt Chart Tools — Format | Text Styles、Gridlines、Layout、Insert Column、Column Settings、Custom Fields、Bar Styles、Critical Tasks、Slack、Late Tasks、Task Path、Baseline、Slippage、Gantt Chart Style、Outline Number、Project Summary Task、Summary Tasks |
| Timeline | Timeline Tools — Format | Font、Show/Hide、Insert、Existing Tasks、Copy Timeline、Date Format、Detail / Bar 表示 |
| Network Diagram | Network Diagram Tools — Format | Layout、Box Styles、Data Templates、Text Styles、Drawing |
| Calendar | Calendar Tools — Format | Bar Styles、Layout、Text Styles、Gridlines |
| Report | Report Tools — Design | テーマ、色、フォント、Image、Shape、Chart、Table、Text Box |
| Report 内の chart/table/picture/shape | Chart/Table/Picture/Drawing Tools | 選択オブジェクト固有の Design / Layout / Format |

Gantt の表示トグルはモデル値ではなくビュー設定として保存し、`Critical Tasks`、`Slack`、
`Late Tasks`、`Task Path`、`Baseline`、`Slippage` がバー・描画・凡例に正しく反映されることを
確認する。

### #473 の決定: Task と Gantt Chart Format は統合しない

**決定:** MSP 互換の通常面では `Task` の配下に `Gantt Chart Format` をサブメニュー又は
セクションとして入れない。Gantt Chart 又は Tracking Gantt がアクティブな間だけ、通常タブの
右側に **`Gantt Chart Tools — Format`**（現行 UI の表示名は `Gantt Chart Format`）を
コンテキスト・タブとして表示する。別ビューへ切り替えた時点でこのタブとそのコマンドは
利用不可にし、以前の Format 状態を通常タブとして残してはならない。

これは Task の混雑を緩和するためだけの決定ではない。両者は操作対象、適用範囲、保存先が
異なるためである。

| 区分 | Task | Gantt Chart Tools — Format |
|---|---|---|
| 操作対象 | 選択したタスク、その依存関係、割当、スケジュール | 現在表示している Gantt/Tracking Gantt のチャート面 |
| 主な結果 | タスク・プロジェクトのデータ、日程、進捗を変更する | バー、リンク線、グリッド、ラベル、レイアウトなどの見え方を変更する |
| 表示条件 | 通常タブとして常時表示 | 対象 Gantt ビューがアクティブなときだけ表示 |
| 状態の保存 | タスク／プロジェクトのデータと Undo トランザクション | ビュー設定。タスク・スケジュール値を変更しない |

したがって、`Link`、`Unlink`、`Indent`、進捗、タスク情報のように計画データを変える操作は
Task に置く。`Bar Styles`、`Gantt Chart Style`、`Layout`、`Gridlines`、`Critical Tasks`、
`Slack`、`Late Tasks`、`Task Path`、`Baseline`、`Slippage`、表示ラベルのようにチャートの
見え方を変える操作は Gantt Chart Format に置く。Task 内に同じ機能への別入口を追加して二重の
Action 又は異なる状態を作ってはならない。

例外として、単一タスクのバー書式はタスクを選んだときの `Task Information`（又はバーの
直接操作）から開いてよい。この場合も対象はそのタスクだけであり、Gantt 全体の表示書式を
変える Format コマンドへ混在させない。適用／取消、Undo、保存・再読込の境界を明確にする。

実装上は `FormatRibbonTask` を Gantt/Tracking Gantt 専用のコンテキスト・タブとして扱う。
Timeline、Network Diagram、Calendar、Report はそれぞれ固有の Format/Design タブを持つため、
同じ `FormatRibbonTask` のコマンド群を流用して誤ったビューに表示してはならない。幅不足時は
タブを消すのではなく、既定どおりグループ単位で畳み、畳まれたメニューからも同一 Action を
一度だけ実行できなければならない。

受入では、(1) Gantt/Tracking Gantt でのみ当該タブが見えること、(2) View で別ビューに切り
替えると消えること、(3) Format の操作が開始日・終了日・依存関係・進捗を変えないこと、
(4) ビュー設定が保存・再読込後に復元すること、(5) Task 操作と Format 操作の Undo が混ざら
ないことを、実 GUI で確認する。

## 5. 独自機能の置き方

互換を維持するため、独自機能は通常タブの MSP コマンドの間へ挿入しない。標準タブの右、
コンテキスト・タブの前後いずれかに独自タブ **microProject** を一つ置く。提案グループは
`Collaboration, Critical Chain (CCPM), Import/Export, Automation, Insights` である。

- MSP 互換コマンドは既存の MSP Action に委譲し、独自コマンドは `MicroProject.*` の別名前空間を
  使う。名称やアイコンで MSP コマンドと区別できるようにする。
- 既存の CCPM、共同編集、XLSX、ローカル同期のような差別化機能はこのタブに寄せる。ただし
  「Resource > Level」や「Project > Baseline」の意味を変えるための近道には使わない。
- 独自機能が選択タスクやアクティブ・ビューに作用する場合、Action の enablement は標準コマンドと
  同じ selection model を読む。専用の選択状態を持たない。
- 保存形式に追加データが必要なら、MSP 形式へ無言で書き込まない。native MPOF 又は sidecar を使い、
  MPP/XML/XLSX の読み書きで失われるデータは明示する。

### Critical Chain (CCPM) 拡張仕様

CCPM は MSP 互換の CPM 計算を置換する機能ではなく、リソース制約を含む別の分析・実行計画層である。
通常の MSP 互換計画を常に原本として保持し、CCPM の適用、再計算、解除は明示的な操作にする。

| グループ | コマンド | 有効条件と結果 |
|---|---|---|
| Plan | CCPM Settings | 設定ダイアログを開く。プロジェクト・バッファ係数、フィーディング・バッファ係数、リソース競合の解消方針、保護する固定日を設定する |
| Plan | Analyze Critical Chain | タスク、依存関係、割当、カレンダーを読み、リソース競合を考慮したクリティカル・チェーン候補とバッファ案を算出する。元計画は変更しない |
| Plan | Apply CCPM Plan | 確認ダイアログ後、承認済みのチェーン、バッファ、解消順序を CCPM 計画として保存し、専用の Undo 単位で適用する |
| Plan | Clear CCPM | CCPM が適用済みの場合のみ有効。CCPM による予定変更、バッファ、表示設定を除去し、適用直前の通常計画へ復元する |
| Monitor | Buffer Status | Project / Feeding / Resource Buffer ごとに消費量、残量、緑・黄・赤の閾値、影響タスクを表示する |
| Monitor | CCPM Network | クリティカル・チェーン、リソース制約リンク、フィーディング・チェーン、各バッファをネットワーク表示する |
| Monitor | Refresh Analysis | 実績、進捗、依存関係、リソース割当が変わった後に差分を再分析する。自動適用はしない |
| Display | Show Critical Chain | Gantt Chart にチェーンとリソース制約リンクを重ね描画するトグル。スケジュール値は変更しない |
| Display | Show Buffers | Gantt Chart / CCPM Network に各種バッファと消費状態を重ね描画するトグル。スケジュール値は変更しない |

CCPM のモデル要件は次のとおり。

1. `baselineSchedule`（通常の MSP 互換スケジュール）と `ccpmPlan`（派生データ）を分離する。
   CCPM を未適用の状態で保存したファイルを開いても、既存のタスク日付、依存関係、割当は変わらない。
2. クリティカル・チェーンは、依存関係だけの最長経路ではなく、リソースの同時利用不能を追加した
   経路として計算する。追加する resource constraint link は元の論理依存関係と区別して保持・描画する。
3. バッファは通常タスクとしてユーザーの WBS に混在させない。Project Buffer、Feeding Buffer、
   Resource Buffer を型付きの CCPM エンティティとして保持し、表示ビューで仮想行／バーにする。
   エクスポート先が CCPM を表現できない場合は、明示的な「バッファをタスクとして展開」操作を必要とする。
4. Apply 前には、変更対象タスク、変更前後の開始・終了日、挿入・更新するバッファ、解消される競合を
   プレビューする。固定制約、実績、完了タスク、外部サブプロジェクトを無言で移動しない。
5. Clear は `ccpmPlan` の作成時に記録したスナップショット又は操作ログから復元する。元計画への
   手編集と CCPM 適用後の編集が混在した場合は、破棄せず競合を表示して選択させる。
6. バッファ消費率は、予定日数の消化率ではなく、CCPM 計画で定義した残作業とチェーン／バッファの
   進捗から算出する。計算式、閾値、ステータス日を設定として保存し、レポートにも同じ値を使う。
7. CCPM 分析・適用中は UI をブロックしない。モデル計算はバックグラウンドで行い、Swing UI と
   selection model の更新は EDT で行う。計算対象が変更された場合は結果を破棄して再分析を促す。

CCPM の受入シナリオには、空プロジェクト、依存関係だけのチェーン、同一リソースの競合、複数の
フィーディング・チェーン、0 日マイルストーン、100% 完了タスク、固定制約、進捗更新後の再分析、
Apply → Save → Reload → Clear の復元を含める。Gantt の `Show Critical Chain` と `Show Buffers`
は表示だけで、元の開始日・終了日を変えないことも確認する。

## 6. 現在の実装との対応と実装順

現行のリボン定義は
`modules/micrproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties`、
表示語は `menu.properties` と `menu_ja.properties`、表示切替は
`GraphicManager.setVisibleContextualRibbonTabs`、カタログは
`RibbonCommandCatalog` にある。既に `File, Task, Resource, Report, Project, View, Format` の
骨格と一部の Action がある。

実装は次の順で行う。

1. 上記インベントリを Action ID、表示文字列、アイコンキー、グループ、表示条件、モデル操作、
   Undo 単位、ショートカットで機械可読な表にする。
2. Task / Resource / Project / View の既存 Action を MSP の発見場所に並べ直し、不足 Action は
   空ボタンでなく実装済みの機能だけ追加する。
3. Gantt、Timeline、Report などのコンテキストごとに Format を分離する。現状の一つの
   `FormatRibbonTask` を全ビューで流用して誤ったコマンドを出さない。
4. Action ごとに「選択なし」「読み取り専用」「手動/自動スケジュール」「空プロジェクト」、
   変更後の Undo/Redo、保存・再読込をテストする。表示幅を狭めた場合も畳まれたメニューから同じ
   Action が実行できることを GUI テストで確認する。
5. 最後に独自 `microProject` タブを足す。これは MSP 互換テストの対象から分離し、独自機能の
   テストを持つ。

## 7. 受入基準

- 2024 デスクトップ版の各標準タブについて、タブ順、グループ順、コマンド名、可視/有効状態を
  比較表とスクリーンショットで記録する。
- 同じコマンドを Quick Access、リボン、ショートカット、コンテキストメニューから呼んでも、
  一回だけ実行され、同じ Undo エントリになる。
- タスク挿入、アウトライン、依存関係、進捗、リソース割当・レベリング、ベースライン、
  フィルタ、ビュー設定を含む操作を保存・再読込後にも再現する。
- コンテキスト・タブは適切なビューにだけ現れ、ビューを変更すると消える。Report 内のさらに細かい
  オブジェクト選択タブも同じ原則で動く。
- 日本語と英語でラベルが欠けず、狭い幅・キーボード・高 DPI・スクリーンリーダーでも操作可能である。

## 8. 根拠

- Microsoft Support, [Learn the Project 2010 Ribbon](https://support.microsoft.com/en-us/office/learn-the-project-2010-ribbon-5038d333-8646-4c46-a0df-9be0ab380d8a): 基本タブ、Schedule、Timeline の挙動。
- Microsoft Support, [Edit a project in Project desktop](https://support.microsoft.com/en-us/project/edit-a-project-in-project-desktop): Gantt Chart と Task > Schedule の Indent / Outdent。
- Microsoft Support, [Overview of Project views](https://support.microsoft.com/en-gb/office/overview-of-project-views-6cb1dbcd-5cd5-4cc2-a878-aa365564266d): タスク、リソース、割当ビューの分類。
- Microsoft Support, [Create a Project report](https://support.microsoft.com/en-us/office/create-a-project-report-6e74dc79-0e2d-480b-b600-3a466bf289a3) および [Pick the right report](https://support.microsoft.com/en-us/project/pick-the-right-report-in-project): Report Tools と組込みレポート。
- Microsoft Support, [Filter tasks or resources](https://support.microsoft.com/en-us/project/filter-tasks-or-resources): View > Data の Filter と Format の Text Styles。
- 画面の現行確認: Project 2024 の [Project tab](https://menlo-academy.com/construction-programming-tips/project-statistics-button/)、[View tab](https://www.ppm.express/blog/start-using-microsoft-project-desktop)、[Gantt Chart Format](https://learn.microsoft.com/en-us/answers/questions/5880969/issue-with-conditional-gantt-bar-colors-in-microso)。第三者画面は配置確認にのみ使い、実装の一次根拠は Microsoft Support と実機検証とする。
