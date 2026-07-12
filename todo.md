# 未変更 Java コード向け TODO メモ

最終更新: 2026-07-11

## 1. 目的

このファイルは、基準コミット `ea2f9e6edd9040e8edc86f0bb058bb68d323666a` と比較して、まだ修正されていない Java コードと、現行の Gradle モジュール構成に関する残タスクの一覧です。  
「どこが古いまま残っているか」「なぜ後回しにしたか」「次に何を触るべきか」「どのテストで守るべきか」を、あとで見返してもそのまま再開できる粒度で残します。

## 2. 判定ルール

- 比較基準は `ea2f9e6edd9040e8edc86f0bb058bb68d323666a..HEAD`
- 対象は主に `modules/projectlibre_core/src/main/java/com/projectlibre1/**` と `modules/projectlibre_ui/src/main/java/com/projectlibre1/**`
- `org.pushingpixels.*`、`org.apache.batik.*`、`net.sf.mpxj.*` などの同梱第三者コードは、監査対象には含めても今回の実装対象からは外す
- 「未変更」は Git 上で当該ファイルが基準コミット以降に差分を持っていないことを意味する
- 既存のワークツリー変更は尊重し、巻き戻しや整理は行わない

## 3. 現状サマリ

### 3.1 未変更率（基準時点の監査値）

- 現行 Java 全体: `1442` ファイル中 `870` ファイルが未変更
- 自前コードに限定: `864` ファイル中 `576` ファイルが未変更
- 自前コードの未変更率: `66.67%`

> 注意: 上記は旧ルート構成を基準にした 2026-06-20 時点の監査値であり、`modules/*/src/main/java` への移行後の現行値ではない。構成移行を固定した後に再集計する。

### 3.2 未変更が多い主要領域

| 領域 | 未変更ファイル数の多さ | 補足 |
| --- | ---: | --- |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/grouping` | 再集計予定 | ノード変換、階層、集約、Undo 連携が古い |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment` | 再集計予定 | 工数配分、contour、集計、Assignment 本体が重い |
| `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/spreadsheet` | 再集計予定 | UI イベントと共通表コンポーネントが密結合 |
| `modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog` | 再集計予定 | minimum size TODO、listener cleanup、ダイアログ共通化不足 |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/task` | 再集計予定 | `Project` / `NormalTask` の肥大化が継続 |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm` | 再集計予定 | 未実装 generator / query 系が残存 |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/scheduling` | 再集計予定 | 旧スケジューリング規約と null/stub が残る |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/undo` | 再集計予定 | catch TODO、イベント粒度の整理不足 |
| `modules/projectlibre_core/src/main/java/com/projectlibre1/script` | 再集計予定 | 自動生成 stub と古い scripting 前提が残る |
| `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/network` | 再集計予定 | 描画・選択・イベントの stub が多い |

### 3.3 構成監査で確認したリスク

- 旧ルートの `projectlibre_core` / `projectlibre_ui` などを削除し、`modules/*` に移す大規模な移行差分がワークツリーにある。Git の rename として確定しないまま進めると、レビュー・履歴追跡・CI の対象漏れが起きる。
- `todo.md` の対象パスが旧レイアウトのままだった。誤ったファイルを調査・修正する直接的な原因になるため、本ファイル内の対象パスを現行レイアウトへ更新した。
- `projectlibre_core` が `projectlibre_contrib` に依存している。`contrib` には JasperReports、Groovy、PDF、UI 関連を含む広いライブラリと複数の役割が集まり、core の依存閉包・クラスパス・変更影響を不必要に広げている。
- `projectlibre_exchange` は外部 MPXJ 依存と、同系統の同梱ソース/bridge を併せ持つ。API の世代差を閉じ込めないまま利用箇所が増えると、import/export の挙動差やクラス衝突が再発しやすい。
- `modules/*/build` とルート `build`、`samples`、旧配布資材の境界が移行期間中に混在しやすい。生成物をソース・サンプルとして誤認しないよう、生成先と検証対象を明文化する必要がある。

### 3.4 構成改善の実装計画

1. **移行の基線を固定する**: `settings.gradle.kts` と各 module の `src/main/{java,resources}` / `src/test/{java,resources}` を正とし、旧ルート参照を README、docs、scripts、テスト、TODO から除去する。`projects`、全 test、`stageAppDist` を通してから旧資材の削除を確定する。
2. **依存方向を可視化する**: `core → contrib` の実依存を package 単位で棚卸しし、core が必要とする最小の共通層と、reports/script/packaging 用の層を分離する。直ちに module 分割せず、まず依存境界テストと Gradle の dependency report を追加する。
3. **交換形式の実装を一経路にする**: MPXJ の外部版・同梱 bridge・ProjectLibre adapter の責務を分け、呼び出し側が世代依存 API を直接参照しない形にする。既存サンプルの MPP/POD/XML/XLSX の round-trip テストを先に固定する。
4. **生成物の境界を固定する**: `build/` と `modules/*/build/` を生成専用にし、`samples/` は入力データ専用にする。実行スクリプト、配布タスク、`.gitignore` をこの前提に揃え、実行後の差分がソース変更に見えない検査を CI に追加する。
5. **現行構成で再監査する**: パスを `modules/.../src/main/java` に統一したうえで未変更率・TODO/stub 件数を再集計し、各項目に「着手済み / 一部解消 / 完了」と検証コマンドを付ける。

### 3.5 すでに今回までで着手済みの範囲

以下は今回のリファクタリングである程度整理済みで、今後は周辺波及や追補テスト中心に見る。

- `ResourceImpl`
  - 可用性/読み取り専用判定と割当状態判定を helper に分離
- `HasAssignmentsImpl`
  - clone/copy 経路を整理し、コピー責務を寄せた
- `TaskSchedule`
  - forward/backward 変換と依存日付無効化の責務を整理
- `WorkingCalendar`
  - concrete cache 無効化と assign 系責務を helper 寄りに整理
- `ProjectView` / `ResourceView`
  - spreadsheet 初期化まわりの重複を `SpreadsheetViewSupport` に寄せた
- `ChartView`
  - chart 側の初期化フローを簡素化
- `FindDialog`
  - button enable/disable、listener cleanup の最低限整理
- 追加済みテスト
  - `HasAssignmentsImplTest`
  - `WorkingCalendarTest`
  - `TaskScheduleTest` の補強
  - `SpreadsheetViewRefactorAuditTest`
  - `FindDialogCleanupAuditTest`
- 例外/補助クラスの TODO 掃除
  - `NodeException`
  - `FieldParseException`
  - `InvalidFormulaException`
  - `UniqueIdException`
  - `InvalidCalendarException`
  - `JobCanceledException`
  - `ExtendedProgressMonitor`
  - `CalendarInterval`
  - `LinkLabel`
  - `ExtButtonFactory`
- `DefaultNodeModel`
  - `replaceImpl` / `replaceImplAndSetFieldValue` の通知を `insert/remove` から `update` に寄せて、二重発火の一部を解消した
  - `remove` の Undo 前後処理と対象収集を helper 化して、削除フローを少し分離した
  - `addBefore` と `replaceImplAndSetFieldValue` の古い `need undo here` メモを除去した
  - `replaceImpl` の event 種別を固定する回帰テストを追加した
  - `remove` の event 種別を固定する回帰テストを追加した
  - `replaceImplAndSetFieldValue` の previous node reposition を helper 化した
  - `copy` の依存再構築を helper 化した
  - `copy` の依存張り直しを回帰テストで固定した
  - `search(Object key)` を identity index ベースに切り替え、検索 TODO を解消した

## 4. 今後の進め方

- 優先順位は「未変更」「肥大化」「TODO/stub 残存」「壊れやすいがテスト薄い」の掛け合わせで決める
- public API の変更は極力避け、package-private helper と内部テスト追加で進める
- 1 回で全面刷新せず、責務分割とテスト追加を交互に行う
- 既に触ったクラスの周辺で再び大きく設計を動かす場合は、まずテストを増やしてから入る

## 5. 残タスク一覧

## 5.2 Core: assignment 系の未着手本体

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment/Assignment.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment/AssignmentDetail.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment/AssignmentService.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment/contour/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/assignment/functor/**`

残課題:

- `Assignment` 本体にスケジューリング、実績、コスト、contour 変形、remaining units 調整が混在している

やること:

- `Assignment` を「日付/稼働計算」「コスト/実績」「contour 操作」「undo 用スナップショット」に分割する

必要テスト:

- contour 変更時に remaining work / actual work / units が壊れないこと

完了:

- `AssignmentService.newAssignments` が指定 delay を保持し、`undo=false` 時に undo 編集を作成しないよう修正した。重複リソース抑制とタスク状態復元を含む回帰テストを追加した
- 異なる Project を一括割当しようとした場合に、部分適用せず明示的に拒否するようにした
- `AssignmentDetail` の percent complete を 0..1 に正規化し、非有限値と zero-duration の remaining duration による NaN を防止した
- contour を personal 化しても work / actual work / remaining work / units が保持される回帰テストを追加した。setter 別の連動検証は残課題として維持する
- `AssignmentService` の一括割当で ResourcePool 側にも transaction 境界を通知し、複数リソース時の通知回数を `AssignmentServiceTest` で固定した
- `ContourBucketIntervalGenerator` が負の日付を使う reverse scheduling でも contour を進めるようにし、`AssignmentContourBehaviorTest` で固定した
- `AssignmentDetail` の effective calendar 解決を `AssignmentCalendarSupport` に切り出し、既存の intersection / baseline / actual exception 優先順位を core テストで確認した

検討メモ:

- `HasAssignmentsImpl` を整理した次はこの層に入るのが自然
- histogram 系の未検証コメントがあるため、UI 側 usage view と一緒に確認したい

## 5.3 Core: task / project 系

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/task/Project.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/task/NormalTask.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/task/Portfolio.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/task/DefaultSubProj.java`

残課題:

- `Project` は巨大クラスのままで、baseline、copy、temporaryLocal、schedule 実装代行が混在している
- GUID/TODO と copy 不完全コメントが残る
- `NormalTask` には duplicate event、calendar 調整、default assignment 仮対応などの TODO が多い
- `Portfolio` の subproject 登録・ライフサイクル責務が未確定

やること:

- `Project` から「baseline 管理」「ID/GUID 発行」「temporary/local 状態」「schedule facade」を順次外出しする
- `NormalTask` の progress 更新時イベントと schedule 更新時イベントを分離する
- subproject 用の最小 API を定義し、`Portfolio` と連携した登録・ライフサイクルを具体化する
- `Portfolio` の参照除去・ライフサイクル責務を明文化する

必要テスト:

- subproject を含む copy/paste と delete が破綻しないこと

完了:

- `DefaultSubProj` の ID、valid/open/writable/fetching 状態を実装し、`DefaultSubProjTest` を追加した
- `DefaultSubprojectHandler` の参照タスク管理、placeholder 作成、subproject 状態更新を実装し、`DefaultSubprojectHandlerTest` を追加した
- `Portfolio` の default calendar、selection manager、dirty state 伝播、assignment 有無判定を実装し、`PortfolioDocumentTest` を追加した
- baseline と追加 baseline が current 値から独立して保持されることを `ProjectScheduleBehaviorTest` で固定した
- `NormalTask.setActualStart()` が値不変時に schedule event を再発火しないようにし、actual start 更新経路の重複イベント回帰テストを追加した。progress 全体のイベント分離は残課題とする

検討メモ:

- `Project` は広範囲に波及するので、一気に分割せず facade 導入から始める
- `RecurringTask*` 系の新規変更がこの層に乗っているため、衝突回避のためにも先に characterization test を増やす

## 5.4 Core: scheduling / dependency / critical path

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/scheduling/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/dependency/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/criticalpath/CriticalPath.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/criticalpath/PredecessorTaskList.java`

残課題:

- `DependencyFormat` に finder 責務・ParseException・current project 限定探索の TODO が残る
- `Dependency` 本体に stub メソッドがある
- `CriticalPath` は「今は全 project invalidate」で済ませている箇所がある

やること:

- scheduling type の解決不能ケースを null ではなく明示エラー/既定値で扱う
- dependency parse と task lookup を分離し、current project 前提を外せる形にする
- `CriticalPath` の全 invalidate を部分 invalidate に寄せるため、変更単位の追跡情報を整理する

必要テスト:

- cross-project / subproject を含む dependency parse
- dependency 追加/削除時の invalidate 範囲と再計算結果

検討メモ:

- `TaskSchedule` を触ったので、この層は次の有力候補
- ただし `DefaultNodeModel` のイベント整理と密接なので、順番は `grouping` とセットで考える

進捗:

- `Dependency` の DataObject stub を実装し、名前・endpoint 由来 ID・永続化された ID の扱いを回帰テストで固定した。parse の cross-project 対応と invalidate 範囲の縮小は残課題とする
- dependency 更新時の invalidate を successor / WBS の影響閉包へ限定する helper を `Task` に追加し、独立した中間タスクを巻き込まないことを `TaskDependencyInvalidationTest` で固定した
- `DependencyFormat` が自動生成タスクの ID 設定に失敗した場合、ParseException の位置を 0 ではなく現在位置で返すよう修正した

## 5.5 Core: calendar 系の残り

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/CalendarDefinition.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/CalendarService.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/CalendarCatalog.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/WorkingHours.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/WorkDay.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/WorkRange.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/pm/calendar/InvalidCalendar*`

残課題:

- `CalendarDefinition` に iterator 最適化、例外処理、stub が残る
- `CalendarService` に update event TODO、temporary copy 周辺の責務の曖昧さが残る

やること:

- `CalendarDefinition` の例外日・差分継承・探索ロジックをテーブル操作 helper に分ける
- `CalendarService` の temporary copy と persist 対象を分離する

必要テスト:

- base calendar 継承、例外日、DST、閏日、月跨ぎの加減算
- temporary calendar copy 編集後の apply/cancel
- invalid calendar definition で期待通り例外になること

完了:

- `CalendarCatalog` の default calendar、dirty state、selection event manager を実装し、`CalendarCatalogTest` を追加した
- `WorkDay` / `WorkRange` の clone 失敗時に null を返さず、明示的な例外に統一した
- `CalendarDefinition` の name/category、null 名称拒否、invalidate、invalid 判定、自己依存判定を実装し、`CalendarDefinitionTest` を追加した。concrete 定義の `getBaseCalendar()` は親を持たない既存モデルに合わせて未変更とした
- `CalendarService` の scratch copy / apply / saveAndUpdate / null 境界を明示し、`CalendarServiceTest` を追加した。複雑な UI persist 連携は残課題として維持する

検討メモ:

- `WorkingCalendarTest` は追加済みだが、この層全体の網羅にはまだ足りない
- `ChangeWorkingTimeDialogBox` と合わせて UI 連携まで見る必要がある

## 5.6 Core: algorithm / query 系

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/Query.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/CollectionIntervalGenerator.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/RangeIntervalGenerator.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/SelectFrom.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/TimeIteratorGenerator.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/algorithm/ValueDivision.java`

残課題:

 - 一部 generator と `IntervalGeneratorSet` に、用途を確認してから扱う null return / stub が残る
- 現状使われていない経路と、実際に UI/レポートから使われる経路の切り分けが未完了

やること:

- 実使用箇所を先に洗い出し、未使用なら削除候補、使用中なら実装補完のどちらかに決める
- interval generator には最小限の contract test を足してからリファクタリングする

必要テスト:

- 範囲指定と iterator 組み合わせの基本ケース
- 空結果・不正入力・null 条件で NPE にならないこと

完了:

- `RangeIntervalGenerator` / `InstantIntervalGenerator` の `current()` が null を返す問題を修正し、`IntervalGeneratorContractTest` で現在区間の契約を固定した
- `Query.execute()` / `Query.create()` が訪問した区間を配列で返すようにし、`QueryTest` で実行結果の契約を固定した

検討メモ:

- 優先度は core scheduling より一段下
- ただしレポートや時系列集計の不具合源になりやすいため、未使用調査だけでも先にやる価値はある

## 5.7 Core: undo / server / script 周辺

対象:

- `modules/projectlibre_core/src/main/java/com/projectlibre1/undo/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/server/data/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/script/**`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/scripting/ScriptedFormula.java`
- `modules/projectlibre_core/src/main/java/com/projectlibre1/company/DefaultUser.java`

残課題:

- Undo 系の一部に、復旧失敗時の扱いを確認すべき箇所が残る
- `DistributionConverter` / `DataUtil` など server data 変換系が例外握り潰し気味
- `DistributionHolder` に自動生成 stub が残る
- `ScriptedFormula` は古い BSF/Groovy 前提 TODO が残る

やること:

- Undo 失敗時の復旧方針を定め、最低でもログ付き例外に統一する
- server data 変換は反射例外をまとめてラップし、入力データ不正と実装バグを分ける
- 使われない script object は削除候補を明示し、使うものだけ契約を固定する
- `DefaultUser` は実装を入れるか、テスト用/非対応として閉じるかを決める

必要テスト:

- Undo/Redo 失敗時に silent corruption しないこと
- server data 変換の round-trip
- scripted formula の読み込み失敗時のエラー表示

検討メモ:

- 機能面の優先度は中位だが、古い TODO が密集しているため掃除効果は大きい

進捗:

- `DefaultUser` を匿名ローカルユーザーとして明示化し、ID・名称・権限の契約を `DefaultUserTest` で固定した
- standalone の `DistributionConverter` が null ではなく空結果を返し、delegate 呼び出し失敗を明示的な例外にするよう修正した。`DistributionConverterTest` を追加した
- Field / Assignment / Dependency の Undo/Redo 失敗をログだけで握り潰さず、`CannotUndoException` / `CannotRedoException` として伝播するよう修正した

## 5.8 UI: dialog 系

対象:

- `modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog/*.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog/assignment/*.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog/calendar/*.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/dialog/util/*.java`

残課題:

- `ResourceInformationDialog` の availability/read-only 特例が残る
- `ChangeWorkingTimeDialogBox` の persist TODO が残る
- `ComponentFactory` に access control policy の特例が残る
- `FieldChangeListener` / `LookupDialog` / `LinkLabel` / `CalendarInterval` などに stub や catch TODO が残る
- `AssignmentDialog` / `AssignmentEntryPane` に hard-coded 幅や暫定イベント更新が残る

やること:

- availability/read-only の特例は `ResourceImpl` 側 helper と整合する形で UI モデルへ寄せる
- `ChangeWorkingTimeDialogBox` の persist 経路を `CalendarService` の temporary copy 方針と合わせて確定する
- `ComponentFactory` の field-specific な特例を field metadata 側へ戻す
- `AssignmentDialog` 系は selection / replace / width 定義を XML/metadata へ寄せる

必要テスト:

- 主な dialog の初期サイズ、button enable/disable、escape/enter、focus restore
- calendar dialog の apply/cancel
- assignment replace と availability 編集の UI 回帰

検討メモ:

- `FindDialog` は一段片付いたが、dialog 群全体はまだ共通化余地が大きい
- 使い勝手変更が出やすいので、UI テストを増やしてから揃える

進捗:

- `AbstractDialog.pack()` の共通 minimum size 固定と既存 audit test により、dialog 側の minimum size TODO は解消済みとして一覧から削除した
- core 側の calendar / scheduling 変更後も `:projectlibre_ui:test` が成功することを確認した

## 5.9 UI: spreadsheet / tree / usage / chart 周辺の残り

対象:

- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/spreadsheet/**`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/TreeView.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/UsageDetailView.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/views/GanttView.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/chart/ChartInfo.java`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/chart/ChartModel.java`

残課題:

- `SpreadsheetViewSupport` で view 初期化は寄せたが、spreadsheet 本体はイベント、editor、renderer、transfer がまだ密結合
- IME、date editor、selection stabilizing はテストはあるが、クラス境界はまだ粗い
- `TreeView` / `UsageDetailView` に stub が残る
- `GanttView` に hard-coded dictionary lookup TODO が残る
- `ChartInfo` は依然として state と UI 参照を多く抱えている
- `ChartModel` は scheduling 修正前提の TODO が残る

やること:

- spreadsheet を「入力イベント」「セル editor」「renderer」「transfer」「row/column header」に責務分割する
- `GanttView` の hard-coded lookup を `ProjectView` / `ResourceView` と同じ metadata lookup 流儀へ寄せる
- `TreeView` / `UsageDetailView` の stub は未使用なら削除、使用中なら実装補完
- `ChartInfo` から view 参照と selection relay をさらに切り出す
- `ChartModel` の TODO が依存する scheduling 前提を明文化する

必要テスト:

- spreadsheet IME、date editor、copy/paste、row/column selection
- usage view と assignment histogram の整合
- chart selection relay、mode 切替、workspace restore

検討メモ:

- spreadsheet は量が多いので、まず `editor` と `common/transfer` から切るのが現実的
- usage view は assignment の contour/availability 改修後に合わせて見る

## 5.10 UI: network / pert / graph

対象:

- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/network/**`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/pert/**`
- `modules/projectlibre_ui/src/main/java/com/projectlibre1/pm/graphic/graph/**`

残課題:

- `Network` / `NetworkParamsImpl` / `NetworkInteractor` / `PertView` に stub が残る
- `Graph` に unregister TODO が残る
- 画面は存在するが、イベント cleanup と依存更新の保証が弱い

やること:

- register/unregister のライフサイクルを view 共通 helper に寄せる
- stub メソッドの利用有無を確認し、必要なら最小実装、不要なら削除する
- network/pert 描画が node cache / dependency cache とどう連動するかを整理する

必要テスト:

- view close/open で listener がリークしないこと
- network/pert で selection と dependency redraw が崩れないこと

検討メモ:

- 優先度は中位以下
- `grouping` と `dependency` が安定してから触るのが安全

## 5.11 Core/UI 横断: 例外クラス・auto-generated stub の一掃

着手済み:

- 上記の小さな例外/補助クラス群から auto-generated TODO コメントを除去し、`FieldParseException(Throwable)` は cause を保持する実装に直した

残課題:

- `DefaultNodeModel` の挿入/削除/Undo/依存更新/イベント発火の大枠整理はまだ未完了
- `Money` を含む、まだ掃除対象に入れていない小さな例外/補助クラスの TODO を順次除去する
- 意味のない TODO コメントが保守コストだけを増やしているので、継続的に減らす

やること:

- 例外クラスは一括で標準 constructor 実装に揃える
- 実装する予定のない stub は削除、必要なら `UnsupportedOperationException` 等で意図を固定する
- 「TODO Auto-generated ...」は最終的にゼロに近づける

必要テスト:

- 例外クラスそのものの単体テストは不要
- 呼び出し元で例外型が期待通り保たれることだけ確認する

検討メモ:

- 単独で価値は小さいが、コードベースの可読性改善に効く
- 大きな refactor の前後で機械的に進めやすい

## 6. 今のところ後回しにしてよいもの

- `projectlibre_exchange` の古い converter TODO
  - 自前コードだが import/export 仕様を触るため、別フェーズで扱う
- 同梱第三者ライブラリ配下
  - フォーク維持ポリシーが必要なので今回は除外
- 画像や menu 定義ファイルの細かな整理
  - Java の未変更本体に比べると優先度が低い

## 7. 推奨実装順

1. モジュール移行の基線固定と依存境界の characterization test 追加
2. `grouping/core/model/DefaultNodeModel` の characterization test 追加
3. `pm/scheduling` と `pm/dependency` の null/stub 解消
4. `pm/assignment/Assignment` と `AssignmentDetail` の責務分割
5. `pm/task/Project` と `NormalTask` のイベント/状態分離
6. `pm/calendar/CalendarDefinition` と `CalendarService` の整理
7. dialog 共通改善と availability/read-only 特例の解消
8. spreadsheet/editor/transfer の責務分割
9. network/pert/graph の lifecycle cleanup
10. 例外クラス・小 stub の一括掃除

## 8. 次回着手時の確認ポイント

- 既存ワークツリー差分に `RecurringTask*` 系や sample data の変更があるため、`pm/task` を触る前に衝突確認をする
- `ResourceImpl` / `HasAssignmentsImpl` / `TaskSchedule` / `WorkingCalendar` 周辺はすでに差分があるので、その上に積む形で進める
- UI テストは headless で回る前提を維持し、root で全 test を止める設計には戻さない
- 大きいクラスを触る前に、まず失敗例を固定するテストを追加する

## 9. 補足メモ

- 2026-06-20 時点で、旧ルート構成を前提とした refactor 対象周辺のコンパイルと `:projectlibre_core:test` / `:projectlibre_ui:test` は通過済み。現行 `modules/*` 構成で再検証する。
- この `todo.md` は「未変更領域の残課題メモ」であり、現在進行中の別作業差分一覧ではない
- 今後このファイルを更新するときは、各項目に「着手済み」「一部解消」「完了」を追記して履歴を残すと再開しやすい
