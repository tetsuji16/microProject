# 未変更 Java コード向け TODO メモ

最終更新: 2026-06-20

## 1. 目的

このファイルは、基準コミット `ea2f9e6edd9040e8edc86f0bb058bb68d323666a` と比較して、まだ修正されていない Java コードを対象にした残タスクの一覧です。  
「どこが古いまま残っているか」「なぜ後回しにしたか」「次に何を触るべきか」「どのテストで守るべきか」を、あとで見返してもそのまま再開できる粒度で残します。

## 2. 判定ルール

- 比較基準は `ea2f9e6edd9040e8edc86f0bb058bb68d323666a..HEAD`
- 対象は主に `projectlibre_core/src/com/projectlibre1/**` と `projectlibre_ui/src/com/projectlibre1/**`
- `org.pushingpixels.*`、`org.apache.batik.*`、`net.sf.mpxj.*` などの同梱第三者コードは、監査対象には含めても今回の実装対象からは外す
- 「未変更」は Git 上で当該ファイルが基準コミット以降に差分を持っていないことを意味する
- 既存のワークツリー変更は尊重し、巻き戻しや整理は行わない

## 3. 現状サマリ

### 3.1 未変更率

- 現行 Java 全体: `1442` ファイル中 `870` ファイルが未変更
- 自前コードに限定: `864` ファイル中 `576` ファイルが未変更
- 自前コードの未変更率: `66.67%`

### 3.2 未変更が多い主要領域

| 領域 | 未変更ファイル数の多さ | 補足 |
| --- | ---: | --- |
| `projectlibre_core/src/com/projectlibre1/grouping` | 60 | ノード変換、階層、集約、Undo 連携が古い |
| `projectlibre_core/src/com/projectlibre1/pm/assignment` | 45 | 工数配分、contour、集計、Assignment 本体が重い |
| `projectlibre_ui/src/com/projectlibre1/pm/graphic/spreadsheet` | 29 | UI イベントと共通表コンポーネントが密結合 |
| `projectlibre_ui/src/com/projectlibre1/dialog` | 27 | minimum size TODO、listener cleanup、ダイアログ共通化不足 |
| `projectlibre_core/src/com/projectlibre1/pm/task` | 24 | `Project` / `NormalTask` の肥大化が継続 |
| `projectlibre_core/src/com/projectlibre1/algorithm` | 22 | 未実装 generator / query 系が残存 |
| `projectlibre_core/src/com/projectlibre1/pm/scheduling` | 21 | 旧スケジューリング規約と null/stub が残る |
| `projectlibre_core/src/com/projectlibre1/undo` | 19 | catch TODO、イベント粒度の整理不足 |
| `projectlibre_core/src/com/projectlibre1/script` | 19 | 自動生成 stub と古い scripting 前提が残る |
| `projectlibre_ui/src/com/projectlibre1/pm/graphic/network` | 17 | 描画・選択・イベントの stub が多い |

### 3.3 すでに今回までで着手済みの範囲

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

- `projectlibre_core/src/com/projectlibre1/pm/assignment/Assignment.java`
- `projectlibre_core/src/com/projectlibre1/pm/assignment/AssignmentDetail.java`
- `projectlibre_core/src/com/projectlibre1/pm/assignment/AssignmentService.java`
- `projectlibre_core/src/com/projectlibre1/pm/assignment/contour/**`
- `projectlibre_core/src/com/projectlibre1/pm/assignment/functor/**`

残課題:

- `Assignment` 本体にスケジューリング、実績、コスト、contour 変形、remaining units 調整が混在している
- `AssignmentService` は一括割当時のイベント発火に TODO が残る
- contour/functor 群では `ContourBucketIntervalGenerator` の逆スケジュール方針が未確定

やること:

- `Assignment` を「日付/稼働計算」「コスト/実績」「contour 操作」「undo 用スナップショット」に分割する
- `AssignmentDetail` の calendar 参照の責務を helper 化する
- 一括割当 API を `AssignmentService` に追加し、イベントをバッチ通知に変える
- `ContourBucketIntervalGenerator` の逆スケジュール時の扱いを決める

必要テスト:

- contour 変更時に remaining work / actual work / units が壊れないこと
- 複数リソース一括割当でイベント数が過剰に増えないこと

検討メモ:

- `HasAssignmentsImpl` を整理した次はこの層に入るのが自然
- histogram 系の未検証コメントがあるため、UI 側 usage view と一緒に確認したい

## 5.3 Core: task / project 系

対象:

- `projectlibre_core/src/com/projectlibre1/pm/task/Project.java`
- `projectlibre_core/src/com/projectlibre1/pm/task/NormalTask.java`
- `projectlibre_core/src/com/projectlibre1/pm/task/Portfolio.java`
- `projectlibre_core/src/com/projectlibre1/pm/task/DefaultSubProj.java`

残課題:

- `Project` は巨大クラスのままで、baseline、copy、temporaryLocal、schedule 実装代行が混在している
- GUID/TODO、baseline TODO、copy 不完全コメントが残る
- `NormalTask` には duplicate event、calendar 調整、default assignment 仮対応などの TODO が多い
- `Portfolio` / `DefaultSubProj` に stub が残り、意図が確定していない

やること:

- `Project` から「baseline 管理」「ID/GUID 発行」「temporary/local 状態」「schedule facade」を順次外出しする
- `NormalTask` の progress 更新時イベントと schedule 更新時イベントを分離する
- subproject 用の最小 API を定義し、`DefaultSubProj` の stub を具体化するか unsupported に寄せる
- `Portfolio` の参照除去・ライフサイクル責務を明文化する

必要テスト:

- baseline 保存/読込と他 baseline 追加時の整合性
- progress 更新でイベントが重複しないこと
- subproject を含む copy/paste と delete が破綻しないこと

検討メモ:

- `Project` は広範囲に波及するので、一気に分割せず facade 導入から始める
- `RecurringTask*` 系の新規変更がこの層に乗っているため、衝突回避のためにも先に characterization test を増やす

## 5.4 Core: scheduling / dependency / critical path

対象:

- `projectlibre_core/src/com/projectlibre1/pm/scheduling/**`
- `projectlibre_core/src/com/projectlibre1/pm/dependency/**`
- `projectlibre_core/src/com/projectlibre1/pm/criticalpath/CriticalPath.java`
- `projectlibre_core/src/com/projectlibre1/pm/criticalpath/PredecessorTaskList.java`

残課題:

- `SchedulingType` に null return TODO が残る
- `DependencyFormat` に finder 責務・ParseException・current project 限定探索の TODO が残る
- `Dependency` 本体に stub メソッドがある
- `CriticalPath` は「今は全 project invalidate」で済ませている箇所がある

やること:

- scheduling type の解決不能ケースを null ではなく明示エラー/既定値で扱う
- dependency parse と task lookup を分離し、current project 前提を外せる形にする
- dependency 更新時の invalidate 範囲を狭めるための依存グラフ単位 helper を検討する
- `CriticalPath` の全 invalidate を部分 invalidate に寄せるため、変更単位の追跡情報を整理する

必要テスト:

- scheduling type 不正入力で silent null にならないこと
- cross-project / subproject を含む dependency parse
- dependency 追加/削除時の invalidate 範囲と再計算結果

検討メモ:

- `TaskSchedule` を触ったので、この層は次の有力候補
- ただし `DefaultNodeModel` のイベント整理と密接なので、順番は `grouping` とセットで考える

## 5.5 Core: calendar 系の残り

対象:

- `projectlibre_core/src/com/projectlibre1/pm/calendar/CalendarDefinition.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/CalendarService.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/CalendarCatalog.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/WorkingHours.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/WorkDay.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/WorkRange.java`
- `projectlibre_core/src/com/projectlibre1/pm/calendar/InvalidCalendar*`

残課題:

- `CalendarDefinition` に iterator 最適化、例外処理、stub が残る
- `CalendarService` に update event TODO、temporary copy 周辺の責務の曖昧さが残る
- `CalendarCatalog` は method stub がある
- 例外クラス群が constructor TODO のまま
- `WorkDay` / `WorkRange` / `WorkingHours` に catch TODO が残り、失敗時の方針が不明

やること:

- `CalendarDefinition` の例外日・差分継承・探索ロジックをテーブル操作 helper に分ける
- `CalendarService` の temporary copy と persist 対象を分離する
- 例外クラスを通常形へ統一し、無意味な auto-generated コメントを除去する
- `WorkRange` / `WorkDay` の例外処理を握り潰しから明示的失敗に変える

必要テスト:

- base calendar 継承、例外日、DST、閏日、月跨ぎの加減算
- temporary calendar copy 編集後の apply/cancel
- invalid calendar definition で期待通り例外になること

検討メモ:

- `WorkingCalendarTest` は追加済みだが、この層全体の網羅にはまだ足りない
- `ChangeWorkingTimeDialogBox` と合わせて UI 連携まで見る必要がある

## 5.6 Core: algorithm / query 系

対象:

- `projectlibre_core/src/com/projectlibre1/algorithm/Query.java`
- `projectlibre_core/src/com/projectlibre1/algorithm/CollectionIntervalGenerator.java`
- `projectlibre_core/src/com/projectlibre1/algorithm/RangeIntervalGenerator.java`
- `projectlibre_core/src/com/projectlibre1/algorithm/SelectFrom.java`
- `projectlibre_core/src/com/projectlibre1/algorithm/TimeIteratorGenerator.java`
- `projectlibre_core/src/com/projectlibre1/algorithm/ValueDivision.java`

残課題:

- generator / query 系に null return と stub が残り、呼び出し側が安全でない
- 現状使われていない経路と、実際に UI/レポートから使われる経路の切り分けが未完了

やること:

- 実使用箇所を先に洗い出し、未使用なら削除候補、使用中なら実装補完のどちらかに決める
- `Query` の null return は空配列/空 generator/例外のいずれかに統一する
- interval generator には最小限の contract test を足してからリファクタリングする

必要テスト:

- 範囲指定と iterator 組み合わせの基本ケース
- 空結果・不正入力・null 条件で NPE にならないこと

検討メモ:

- 優先度は core scheduling より一段下
- ただしレポートや時系列集計の不具合源になりやすいため、未使用調査だけでも先にやる価値はある

## 5.7 Core: undo / server / script 周辺

対象:

- `projectlibre_core/src/com/projectlibre1/undo/**`
- `projectlibre_core/src/com/projectlibre1/server/data/**`
- `projectlibre_core/src/com/projectlibre1/script/**`
- `projectlibre_core/src/com/projectlibre1/scripting/ScriptedFormula.java`
- `projectlibre_core/src/com/projectlibre1/company/DefaultUser.java`

残課題:

- Undo 系に catch TODO が残り、復旧失敗時の扱いが曖昧
- `DistributionConverter` / `DataUtil` など server data 変換系が例外握り潰し気味
- `DistributionHolder` や `DefaultUser` に自動生成 stub が残る
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

## 5.8 UI: dialog 系

対象:

- `projectlibre_ui/src/com/projectlibre1/dialog/*.java`
- `projectlibre_ui/src/com/projectlibre1/dialog/assignment/*.java`
- `projectlibre_ui/src/com/projectlibre1/dialog/calendar/*.java`
- `projectlibre_ui/src/com/projectlibre1/dialog/util/*.java`

残課題:

- `set minimum size` TODO が多数残る
- `ResourceInformationDialog` の availability/read-only hack が残る
- `ChangeWorkingTimeDialogBox` の persist TODO が残る
- `ComponentFactory` に access control policy hack が残る
- `FieldChangeListener` / `LookupDialog` / `LinkLabel` / `CalendarInterval` などに stub や catch TODO が残る
- `AssignmentDialog` / `AssignmentEntryPane` に hard-coded 幅や暫定イベント更新が残る

やること:

- minimum size TODO は一括で「dialog base class に寄せる」か「不要なら削除」のどちらかに統一する
- availability/read-only hack は `ResourceImpl` 側 helper と整合する形で UI モデルへ寄せる
- `ChangeWorkingTimeDialogBox` の persist 経路を `CalendarService` の temporary copy 方針と合わせて確定する
- `ComponentFactory` の field-specific hack を field metadata 側へ戻す
- `AssignmentDialog` 系は selection / replace / width 定義を XML/metadata へ寄せる

必要テスト:

- 主な dialog の初期サイズ、button enable/disable、escape/enter、focus restore
- calendar dialog の apply/cancel
- assignment replace と availability 編集の UI 回帰

検討メモ:

- `FindDialog` は一段片付いたが、dialog 群全体はまだ共通化余地が大きい
- 使い勝手変更が出やすいので、UI テストを増やしてから揃える

## 5.9 UI: spreadsheet / tree / usage / chart 周辺の残り

対象:

- `projectlibre_ui/src/com/projectlibre1/pm/graphic/spreadsheet/**`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/views/TreeView.java`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/views/UsageDetailView.java`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/views/GanttView.java`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/chart/ChartInfo.java`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/chart/ChartModel.java`

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

- `projectlibre_ui/src/com/projectlibre1/pm/graphic/network/**`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/pert/**`
- `projectlibre_ui/src/com/projectlibre1/pm/graphic/graph/**`

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

1. `grouping/core/model/DefaultNodeModel` の characterization test 追加
2. `pm/scheduling` と `pm/dependency` の null/stub 解消
3. `pm/assignment/Assignment` と `AssignmentDetail` の責務分割
4. `pm/task/Project` と `NormalTask` のイベント/状態分離
5. `pm/calendar/CalendarDefinition` と `CalendarService` の整理
6. dialog 共通改善と availability/read-only hack 解消
7. spreadsheet/editor/transfer の責務分割
8. network/pert/graph の lifecycle cleanup
9. 例外クラス・小 stub の一括掃除

## 8. 次回着手時の確認ポイント

- 既存ワークツリー差分に `RecurringTask*` 系や sample data の変更があるため、`pm/task` を触る前に衝突確認をする
- `ResourceImpl` / `HasAssignmentsImpl` / `TaskSchedule` / `WorkingCalendar` 周辺はすでに差分があるので、その上に積む形で進める
- UI テストは headless で回る前提を維持し、root で全 test を止める設計には戻さない
- 大きいクラスを触る前に、まず失敗例を固定するテストを追加する

## 9. 補足メモ

- 2026-06-20 時点で、今回の refactor 対象周辺のコンパイルと `:projectlibre_core:test` / `:projectlibre_ui:test` は通過済み
- この `todo.md` は「未変更領域の残課題メモ」であり、現在進行中の別作業差分一覧ではない
- 今後このファイルを更新するときは、各項目に「着手済み」「一部解消」「完了」を追記して履歴を残すと再開しやすい
