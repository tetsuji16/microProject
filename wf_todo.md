# wf_todo.md

## 前提と方針

- このメモは GUI 操作の見た目ではなく、実装上の関数入口を基準にタスク作業ワークフローを評価する。
- 対象はタスク作業ワークフローに限定し、ファイル保存・読込・ビュー切替などの文書ライフサイクルは含めない。
- ガントから発火する同等操作も対象に含め、表編集経路と同一の関数契約を満たすかを確認する。
- 判断に迷う仕様は MS Project 準拠とし、特にリンク既定値、summary task 制約、階層変更後の再計算、split と resize の整合性を優先する。

### 現状の主要入口

- `SpreadSheetModel` / `CommonSpreadSheetModel`
  - 行編集、空行の実体化、セル値更新、前後行参照を担う。
- `DefaultNodeModel` / `Project`
  - `Node` の差し替え、階層移動、親子関係、WBS 関係の更新を担う。
- `NodeListTransferHandler` / `NodeListTransferable`
  - cut/copy/paste/paste insert/値貼り付けの入口を担う。
- `DependencyService`
  - link/unlink、依存関係更新、循環チェック、Undo 登録の入口を担う。
- `ScheduleService`
  - ガント由来の move/resize/split/constraint/進捗変更の入口を担う。
- `GanttInteractor`
  - ガント上のドラッグ操作を `DependencyService` と `ScheduleService` へ変換する入口を担う。

## 想定ワークフロー

### 1. 新規タスク作成

- 空行に名前を入力して新規タスク化する。
- 空行に複数列を貼り付けて新規タスクを連続作成する。
- 挿入操作から新規タスクを作成する。
- 親タスク配下、ルート直下、subproject 周辺で作成する。

### 2. タスク編集

- 既存タスクの名前を変更する。
- 期間、開始、終了、制約日、制約種別を変更する。
- 進捗率や完了バーを更新する。
- 依存列から predecessor/successor を編集する。

### 3. 階層編集

- indent/outdent で親子関係を変更する。
- summary task 化、summary 解除、親変更後の WBS 更新を行う。
- 階層変更後に summary 再計算、依存関係制約、子孫リンク禁止が維持されることを確認する。

### 4. 並べ替えと移動

- cut/copy/paste で構造ごと移動する。
- paste insert で選択位置に構造挿入する。
- 値貼り付けで既存セルを一括更新する。
- 複数行、複数列、subproject を含む貼り付けを扱う。

### 5. 依存関係

- 表から link/unlink する。
- ガントからドラッグして link を作成する。
- 循環、summary、親子、external、closed subproject 制約が同じ条件で評価されることを確認する。

### 6. ガント編集

- バー移動で開始/終了を変える。
- 端ドラッグで resize する。
- 進捗バー操作で `% complete` を更新する。
- split を追加する。

### 7. 履歴

- undo/redo 後に task/tree/schedule/dependency が元に戻る。
- 表編集経路とガント経路で同じ undo 粒度になる。
- 選択行、フォーカス、スクロール位置の復元方針が一貫している。

## TODO タスク

### A. 関数入口の統一

- [ ] `TaskCreationWorkflow` を定義し、空行実体化、挿入、新規連続作成を同一入口に寄せる。
  - 受け入れ条件: 表の空行入力、挿入、複数行貼り付けのいずれでも、同一の作成関数が呼ばれ、親子関係と Undo 登録が一致する。
  - 着手済み: `DefaultNodeModel.newNode(...)` が `actionType` を捨てていたので、SILENT 呼び出しでも Undo が積まれる問題を修正した。
  - 着手済み: `newNode(...)` の途中挿入経路も `actionType` を渡すように揃え、末尾追加だけ直した不整合を潰した。
  - 着手済み: `addBefore(LinkedList, ...)` も `actionType` を捨てずに使うようにして、まとめ挿入の Undo 契約を揃えた。
  - 着手済み: `NodeCreationEdit` が呼び出し元の可変 `List` を保持したままだと undo が壊れるので、作成時にスナップショットするようにした。
  - 着手済み: `CommonSpreadSheet.addNodeForImpl(...)` も `addBefore(current, ...)` に寄せ、作成後に Undo できない経路を修正した。
- [ ] `TaskHierarchyWorkflow` を定義し、indent/outdent と親変更後の再計算を一元化する。
  - 受け入れ条件: 表操作経路と他経路で、WBS 親、summary 状態、子一覧キャッシュが同じ結果になる。
  - 着手済み: `ViewNodeModelCache.createHierarchyDependency(...)` が普通の dependency を作っていたので、子ノードを親の下へ move する実装に修正した。
  - 着手済み: `MutableNodeHierarchy.indent(...)` が前方の空行を逆順で移していたので、indent 後の空行順序を元の並びのまま保つように修正した。
- [ ] `TaskClipboardWorkflow` を定義し、構造貼り付け、値貼り付け、paste insert を明示的に分離する。
  - 受け入れ条件: `paste`、`paste as values`、`paste insert` が別関数として識別され、失敗時挙動も個別に定義される。
  - 着手済み: `paste as values` が node list flavor を先に拾って構造貼り付けに落ちていたので、文字列 flavor を優先するようにした。
  - 着手済み: `NodeListTransferablePasteFailureTest` に、node flavor を持つ clipboard でも `paste as values` が文字列経路を使う回帰を追加した。
  - 着手済み: `SpreadSheet` の clipboard paste 系を `pasteClipboardContents()` と `pasteClipboardInsertedContents()` に分けて、`Paste` / `Paste Insert` の入口を分離した。
  - 着手済み: `GraphicManager.PasteInsertAction` も `beforeActionRoute("pasteInsert")` を通すようにして、`Paste` と同じ前段ガードに揃えた。
- [ ] `TaskDependencyWorkflow` を定義し、表 link/unlink とガント link を同じ制約判定に通す。
  - 受け入れ条件: summary、親子、external、closed subproject の禁止条件が経路に依らず一致する。
  - 着手済み: `DependencyService` に writable / closed subproject の回帰を追加し、`GanttInteractor` の link 作成もサービス側の制約へ寄せた。
  - 着手済み: `removeAnyDependencies(List tasks)` は read-only を含んでも editable な依存解除を継続するようにして、混在選択での全停止を避けた。
  - 着手済み: `connect(List tasks, ...)` も read-only を選択順から除外した上で editable 同士を連結するようにして、途中に read-only があってもリンク列が分断されないようにした。
  - 着手済み: `GraphicManager` の `Link` / `Unlink` も `beforeActionRoute(...)` を通すようにして、外部ルート前段の扱いを揃えた。
  - 着手済み: `SpreadSheetModel` の dependency type 編集で不正文字列が来たときに `NullPointerException` ではなく明示的な例外で失敗するようにした。
- [ ] `TaskScheduleWorkflow` を定義し、move/resize/split/progress 更新を `ScheduleService` 直呼びから整理する。
  - 受け入れ条件: ガント操作ごとの前提条件、Undo 粒度、制約反映順序が workflow 契約として明文化される。
  - 着手済み: `ScheduleServiceConstraintTest` に、変更なしの constraint / interval 更新で Undo が積まれない回帰を追加した。
  - 着手済み: `ScheduleService.setInterval` を変更有無を返すようにして、`GanttInteractor` 側の no-op 判定をサービス返り値へ寄せた。
  - 着手済み: `ScheduleService.split` も変更有無を返すようにして、read-only で何も起きていない split を Gantt 側が成功扱いしないようにした。

### B. 実装漏れの調査

- [ ] 空行新規作成と挿入操作の差異を棚卸しし、親設定、WBS、Undo、エラー処理の差を洗い出す。
  - 受け入れ条件: 差分一覧に「同じであるべきか」「意図的差異か」が記録されている。
- [ ] `paste`、`paste as values`、`paste insert` の仕様差とエラー処理差を整理する。
  - 受け入れ条件: 文字列貼り付けと `Node` 構造貼り付けの経路差、部分失敗時の扱い、subproject 変換の有無が明文化される。
- [ ] ガントの link 作成が常に FS/0lag 固定でよいかを評価する。
  - 受け入れ条件: MS Project 準拠で FS/0lag を既定にするか、変更可能にするかが結論付きで記録される。
- [ ] ガントの move/resize/split が表編集と同じ制約・Undo 粒度になるかを確認する。
  - 受け入れ条件: 少なくとも開始/終了変更、制約変更、split について同値性の確認項目が列挙されている。
  - 着手済み: `ScheduleServiceSplitTest` に、read-only schedule の split が失敗として扱われる回帰を追加した。
- [ ] 複数選択時の indent/outdent、delete、unlink が部分成功しないかを確認する。
  - 受け入れ条件: 一部 read-only や一部 invalid が混ざるケースで、全失敗・部分成功・スキップのどれが仕様か決まっている。
  - 着手済み: `removeAnyDependencies` は read-only を含む選択を no-op にするようにして、read-only タスクの依存関係が壊れないようにした。
- [ ] subproject を含む cut/paste 時の normal task 変換仕様を評価する。
  - 受け入れ条件: 現状仕様が保持すべきか、workflow 側へ昇格すべきか、もしくは見直すべきかが記録されている。
  - 着手済み: subproject を貼り付けたときに `NormalTask` へ正規化される現行経路を回帰テストで固定した。

### C. 仕様見直し候補

- [ ] 新規タスク作成を「空行を実体化する内部実装」から切り離し、専用の作成 workflow に寄せるか決める。
  - 受け入れ条件: `VoidNode` を UI 専用概念に留めるか、ドメイン側にも許すかが決まっている。
- [ ] 依存関係作成の既定値を FS/0lag として明文化する。
  - 受け入れ条件: 表とガントの両経路で同一既定値を使う方針が書かれている。
- [ ] summary task へのリンク禁止、親子間リンク禁止、external/closed subproject 制約を関数契約に昇格する。
  - 受け入れ条件: GUI 側メッセージではなく、サービス契約として禁止条件が列挙されている。
- [ ] 値貼り付け失敗時の無視、部分適用、集約エラー表示の方針を決める。
  - 受け入れ条件: 現状の黙殺動作を維持するか変更するかが決まり、理由がある。
- [ ] Undo/Redo 契約に選択、フォーカス、スクロール位置の復元を含めるか決める。
  - 受け入れ条件: データだけ戻ればよいのか、操作体験まで契約対象にするのかが決まっている。

### D. テスト追加タスク

- [ ] workflow/service 単位の回帰テスト設計を追加する。
  - 受け入れ条件: 各 workflow に成功ケースと失敗ケースが最低 1 つずつある。
- [ ] 表編集経路とガント経路の結果同値テストを追加する。
  - 受け入れ条件: 少なくとも link 作成、日付移動、進捗変更の 3 操作で同値比較ができる。
- [ ] 既存テストとの対応表を作る。
  - 受け入れ条件: `DefaultNodeModelTest`、`DependencyServiceTest`、`ScheduleServiceConstraintTest`、spreadsheet 系テストで何が担保済みか整理されている。
- [ ] gantt 系の不足テストを列挙し、追加優先度を付ける。
  - 受け入れ条件: `GanttInteractor` 直下で不足している観点が、操作別に TODO 化されている。

## MS Project 準拠で決める項目

- ガントのドラッグリンクは FS/0lag を既定とする。
- summary task とその子孫のリンクは禁止する。
- hierarchy 変更後は WBS と summary 再計算を即時反映する。
- split、resize、move は依存関係と制約を壊さない方向を優先する。
- 貼り付けは「構造貼り付け」と「値貼り付け」を別 workflow として扱う。

## テスト観点テンプレート

各 TODO の検証では、最低限次を記述する。

- 入力前提
  - 選択状態、read-only、subproject、summary、dependency 既存状態。
- 成功結果
  - task、tree、schedule、dependency、undo stack がどう変わるか。
- 失敗結果
  - 例外、無変更保証、部分適用禁止、エラーメッセージ方針。
- 経路同値
  - spreadsheet と gantt が同じ関数契約を満たすか。

## 既存テストとの対応メモ

- `DefaultNodeModelTest`
  - `Node` 差し替え、検索整合、削除などの基礎整合を担保済み。
- `DependencyServiceTest`
  - FS/0lag の既定、自己リンク禁止、依存関係付き resize の基本整合を担保済み。
- `ScheduleServiceConstraintTest`
  - 制約変更と Undo/Redo の基本整合を担保済み。
- spreadsheet editor / IME / selection 系テスト
  - 入力系 UI の局所挙動はあるが、workflow 単位の同値性は不足。
- gantt 系
  - 描画寄りテストはあるが、操作 workflow の回帰テストは不足候補として扱う。

## 現時点で確認した不具合候補

- `SpreadSheet` の `Paste` が構造貼り付けではなく値貼り付けへ流れる。
  - 影響: cut/copy/paste で階層や依存を保てない。
  - 重点確認: `paste`、`paste as values`、`paste insert` の経路分離。
  - 着手済み: `Paste` の実体を値貼り付け固定から外し、`insertClipboardContents()` を通すようにした。
  - 着手済み: `ctrl+V` も clipboard の node list flavor を優先して構造貼り付けに流すようにした。
  - 着手済み: `importData()` でも現在の編集を確定してから貼り付けるようにし、編集中セルが残ったまま貼り付けないようにした。
- 新規作成が編集中セルを確定せずに走り、値が未保存のまま行操作へ移ることがある。
  - 影響: `new` の直前に編集中の入力が失われる。
  - 重点確認: 新規作成前にも `finishCurrentOperations()` を通す。
  - 着手済み: `SpreadSheet.newAction` で現在の編集を先に確定するようにした。
  - 着手済み: `CommonSpreadSheet.addNodeForImpl` でも現在の編集を先に確定するようにした。
  - 着手済み: 多選択時は最後の選択行が read-only でも、作成可能なノードを後ろから探して新規作成するようにした。
  - 着手済み: `NodeListTransferablePasteFailureTest` に、read-only 末尾選択でも `ACTION_NEW` が成功する回帰を追加した。
- 値貼り付けがセル単位で例外を黙殺し、部分成功を隠す。
  - 影響: 失敗したのに一部だけ反映される。
  - 重点確認: 部分適用禁止か、集約エラー表示か。
  - 着手済み: 事前に parse-only で全セルを検証してから本適用するようにし、セル適用ヘルパーは成功/失敗を返すようにした。
  - 着手済み: 検証失敗時は行/列貼り付けにフォールバックしないようにした。
  - 着手済み: `NodeListTransferablePasteFailureTest` で、無効な複数セル貼り付けが行貼り付けに落ちないことを回帰化した。
  - 着手済み: `NodeListTransferHandler.importData` も失敗を `false` で返すようにし、外側の貼り付け契約まで伝播させた。
  - 着手済み: 単セル選択のフォールバック貼り付けも原子的に扱うようにし、部分適用を止めた。
  - 着手済み: 失敗時は `Message.invalidInput` を表示するようにし、無反応な黙殺をやめた。
- 構造貼り付けが read-only 親などで失敗しても成功扱いのままになりうる。
  - 影響: 貼り付け失敗が UI に伝わらず、後続操作の前提が壊れる。
  - 重点確認: `pasteNodes` の成否を `importData` へ返し、read-only 親への貼り付けを拒否する。
  - 着手済み: `NodeModelCache.pasteNodes` を boolean 化し、失敗を返せるようにした。
  - 着手済み: `NodeListTransferHandler.importData` で構造貼り付けの成否を返すようにした。
  - 着手済み: `NodeListTransferablePasteFailureTest` に read-only 親への構造貼り付け失敗を追加した。
- 依存列編集が列番号前提で、無効入力時に途中更新が残る。
  - 影響: `lag` だけ変わるなどの半端な更新が起きる。
  - 重点確認: 例外時のロールバックと列定義依存の排除。
  - 着手済み: `setFields` で `lag` / `type` のロールバックを追加し、source 側が外部タスクのリンクも拒否するようにした。
  - 着手済み: `SpreadSheetModel` の dependency 編集を列番号ではなく `Field.id` で判定するようにした。
  - 着手済み: `connect(List tasks, ...)` でも前段タスクを read-only / external として弾くようにし、連結一覧の経路差を縮めた。
  - 着手済み: `DependencyServiceTest#connectListSkipsReadOnlyPredecessorTasks` で、前段が external のときにリンク生成しない回帰を追加した。
- ガントの link 作成が source 側の read-only / external / closed subproject 制約を十分に見ていない。
  - 影響: 経路によって禁止リンクが通る。
  - 重点確認: 表編集とガント編集で同一の制約判定を使う。
  - 着手済み: `GanttInteractor` で例外時に成功扱いをやめ、source read-only を弾く条件を追加した。
  - 着手済み: link 作成の前段 read-only フィルタを外し、`DependencyService` の制約判定に統一した。
- `Project` の schedule 関連の未実装が undo / split / resize に影響する。
  - 影響: `backupDetail` / `restoreDetail` / `split` / `moveInterval` の整合が崩れる。
  - 重点確認: subproject を含むガント操作が実際に更新されるか。
  - 着手済み: `Project.setDirty(...)` が引数ではなく既存フィールドを再代入していたので、dirty フラグが立たないバグを修正した。
  - 着手済み: `ScheduleService.consumeIntervals(...)` が例外時に `consuming` を戻さないため、再入防止フラグが残るバグを修正した。
  - 着手済み: `moveInterval` と `backupDetail` / `restoreDetail` の最小実装を追加し、プロジェクト span の移動と undo を通した。
  - 着手済み: `split` が no-op のときは undo edit を積まないようにし、プロジェクト split の偽履歴を止めた。
  - 着手済み: `GanttInteractor` の split も project no-op を成功扱いしないように揃えた。
  - 着手済み: `ProjectData.getBudgetStatusIndicator()` が `SPI` 用ラベルを返していたので、CPI ラベルを返すように修正した。
  - 着手済み: `Task` / `Resource` 側の `getScheduleStatusIndicator()` も `CPI` ラベルを返す誤りだったので、SPI ラベルへ修正した。
  - 着手済み: `setCompleted` も変更有無を返すようにして、read-only / no-op の進捗ドラッグが成功扱いにならないようにした。
- `MutableNodeHierarchy.paste` の空き行パディングが同一 `VoidNode` 使い回しになっている。
  - 影響: 空行や挿入位置の再現が壊れる。
  - 重点確認: 空白行は毎回新規ノードで補う。
  - 着手済み: 空き位置を埋めるたびに新しい `VoidNode` を生成するよう修正し、重複参照の回帰テストを追加した。
