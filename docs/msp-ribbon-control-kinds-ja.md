# リボン コントロール種別の契約と Microsoft Project との差分

リボン上の各コマンドが「どの種類のコントロールか」「クリックで何が起きるか」を、Microsoft Project デスクトップ版に合わせるための契約です。配置（タブ・グループ・順序）は `docs/msp-desktop-ribbon-spec-ja.md` と `MspRibbonSpecDiffTest` が扱い、本書は種別と入力挙動だけを扱います。

## 規範ソース

- [MS-CUSTOMUI] Custom UI XML Markup Specification（コントロール要素の定義と挙動）
- Office Fluent UI Command Identifiers の Project 用一覧（`ProjectControls.xlsx`、コマンドごとの Control Type 列）

インストール済みの Microsoft Project は補助的な証拠であり、判定には必須ではありません。現時点の差分行は上記資料から導いた document-derived compatibility decision で、`ProjectControls.xlsx` との行単位の照合はまだ行っていません（下記 `evidence` 列を参照）。

## コントロール種別

`com.microproject.ribbon.RibbonControlKind` が種別と入力契約を定義します。

| 種別 | customUI 要素 | 本体クリック | ポップアップ | 押下状態 | 値 | 描画 |
|---|---|---|---|---|---|---|
| `BUTTON` | `button` | コマンド実行 | なし | なし | なし | 実装済み |
| `TOGGLE_BUTTON` | `toggleButton` | コマンド実行 | なし | あり | なし | 実装済み |
| `SPLIT_BUTTON` | `splitButton` | コマンド実行 | 矢印部で開く | なし | なし | 実装済み（矢印は右端） |
| `MENU` | `menu` | ポップアップを開くだけ | あり | なし | なし | 実装済み |
| `GALLERY` | `gallery` | ポップアップを開くだけ | あり | なし | あり | 未実装（メニューで代替） |
| `DROP_DOWN` | `dropDown` | ポップアップを開くだけ | あり | なし | あり | 未実装（メニューで代替） |
| `COMBO_BOX` | `comboBox` | ポップアップを開くだけ | あり | なし | あり | 未実装（メニューで代替） |
| `CHECK_BOX` | `checkBox` | コマンド実行 | なし | あり | なし | 未実装（ボタンで代替） |
| `EDIT_BOX` | `editBox` | なし | なし | なし | あり | 未実装 |
| `DIALOG_LAUNCHER` | `dialogBoxLauncher` | コマンド実行 | なし | なし | なし | モデルのみ |

「未実装」の種別はモデルと契約には載りますが、専用の見た目はまだありません。`RibbonControlPopups.isNativelyRendered` が現状を返します。

## リソースキー

`modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties` に書きます。

| キー | 意味 |
|---|---|
| `<id>.control` | 種別。`RibbonControlKind` 名または customUI 要素名（大小無視）。省略時は `<id>.presentation=SPLIT` なら `SPLIT_BUTTON`、`<id>.type=TOGGLE` なら `TOGGLE_BUTTON`、他は `BUTTON` |
| `<id>.items` | ポップアップに並べるコマンド id（空白区切り、`-` は区切り線）。ポップアップを持つ種別でのみ指定可 |
| `<id>.keytip` | KeyTip 文字（表示は未実装） |
| `<band>.dialogLauncher` | グループ右下のダイアログ起動ツールが実行するコマンド id（表示は未実装） |

例:

```properties
RibbonMarkOnTrack.control=splitButton
RibbonMarkOnTrack.items=RibbonMarkOnTrack RibbonUpdateTasks
RibbonMarkOnTrack.keytip=U
```

注意点:

- `<id>.items` の各 id は、`<id>.text` と Action を持つ既存コマンドである必要があります。
- 押下状態が必要な種別（`TOGGLE_BUTTON`、`CHECK_BOX`）は `<id>.type=TOGGLE` も併せて指定します。`.control` だけでは押下状態は付きません。
- 不正な `.control`、ポップアップを持たない種別への `.items`、コマンドへの `dialogBoxLauncher` 指定は、モデル生成時に `IllegalStateException` になります。

## 処理の流れ

1. `SwingRibbonFactory.applyControlContract` がキーを読み、`SwingRibbonModel.RibbonButton.withControl` で種別・項目・KeyTip をモデルに載せます。グループには `RibbonBand.withDialogLauncher` を使います。
2. `ModernRibbonPanel.createButton` が `RibbonControlPopups.install` を呼びます。
   - `SPLIT_BUTTON`: 本体はコマンドを実行し、右端の矢印ボタン（`microproject.ribbon.controlMenuButton.<id>`）がポップアップ（`microproject.ribbon.controlMenu.<id>`）を開きます。
   - `MENU` / `GALLERY` / `DROP_DOWN` / `COMBO_BOX`: 本体のコマンドは実行されず、クリックでポップアップだけが開きます。
   - 項目のないコントロールには何もしません。

## 契約ファイルと差分テスト

`modules/microproject_ui/src/test/resources/com/microproject/ui/ribbon/msp-ribbon-control-spec.csv` が、リボン上の全コマンド（クイック アクセス ツール バーを含む）について Microsoft Project での種別を持ちます。

| 列 | 内容 |
|---|---|
| `ribbonId` | コマンド id |
| `mspKind` | Microsoft Project での種別 |
| `evidence` | `BASELINE`: 現在の実装をそのまま記録した行（未照合）。`PENDING_VERIFY`: 資料から導いた差分で、行単位の照合待ち。`DOCUMENTED`: `ProjectControls.xlsx` と照合済み |
| `location` | `タブ/グループ` または `TaskBar` |
| `popupItems` | 想定するポップアップの内容（説明文） |

`MspRibbonControlKindDiffTest` はモデルの種別と CSV を比べます。未対応の差分は同テストの `KNOWN_GAPS` に「現在の種別」と共に列挙します。対応を実装すると、エントリを削除するまでテストが失敗するので、一覧は減る方向にしか動きません。回帰を通すためにエントリを足してはいけません。

## 現時点の差分（25 件）

| コマンド | 現在 | Microsoft Project | ポップアップの内容 |
|---|---|---|---|
| `RibbonPaste` | BUTTON | SPLIT_BUTTON | 貼り付け / 形式を選択して貼り付け |
| `RibbonCopy` | BUTTON | SPLIT_BUTTON | コピー / 図のコピー |
| `RibbonInsert` | BUTTON | SPLIT_BUTTON | タスク / 定期タスク / 空白行 / Outlook タスクのインポート |
| `RibbonMarkOnTrack` | BUTTON | SPLIT_BUTTON | 達成済みにする / タスクの更新 |
| `RibbonFind` | BUTTON | SPLIT_BUTTON | 検索 / 置換 / ジャンプ |
| `RibbonCalendarView` | BUTTON | SPLIT_BUTTON | ビュー切替 + ビュー一覧 |
| `RibbonTopBarUndo` | BUTTON | SPLIT_BUTTON | 元に戻す + 履歴一覧 |
| `RibbonTopBarRedo` | BUTTON | SPLIT_BUTTON | やり直し + 履歴一覧 |
| `RibbonGantt` | TOGGLE_BUTTON | SPLIT_BUTTON | ビュー切替 + タスク ビュー一覧 |
| `RibbonNetwork` | TOGGLE_BUTTON | SPLIT_BUTTON | ビュー切替 + ビュー一覧 |
| `RibbonResources` | TOGGLE_BUTTON | SPLIT_BUTTON | ビュー切替 + リソース ビュー一覧 |
| `RibbonTaskUsageDetail` | TOGGLE_BUTTON | SPLIT_BUTTON | ビュー切替 + ビュー一覧 |
| `RibbonResourceUsageDetail` | TOGGLE_BUTTON | SPLIT_BUTTON | ビュー切替 + ビュー一覧 |
| `RibbonClear` | BUTTON | MENU | 書式 / ハイパーリンク / メモ / すべて / 行全体のクリア |
| `RibbonInsertResource` | BUTTON | MENU | 時間単価型 / 数量単価型 / コスト型リソース |
| `RibbonGridlines` | BUTTON | MENU | 目盛線 / 進捗線 |
| `RibbonSaveBaseline` | SPLIT_BUTTON | MENU | 基準計画の設定 / クリア |
| `RibbonChooseSort` | SPLIT_BUTTON | MENU | 開始日順 / 終了日順 / 優先度順 / コスト順 / ID 順 / 並べ替え |
| `RibbonChooseFilter` | SPLIT_BUTTON | DROP_DOWN | フィルター一覧（現在値を表示） |
| `RibbonChooseGroup` | SPLIT_BUTTON | DROP_DOWN | グループ一覧（現在値を表示） |
| `RibbonTimeline` | BUTTON | CHECK_BOX | 分割ビューの「タイムライン」チェック |
| `RibbonDetails` | TOGGLE_BUTTON | CHECK_BOX | 分割ビューの「詳細」チェック |
| `RibbonTaskModeManual` | BUTTON | TOGGLE_BUTTON | 選択タスクが手動スケジュールなら押下 |
| `RibbonTaskModeAutomatic` | BUTTON | TOGGLE_BUTTON | 選択タスクが自動スケジュールなら押下 |
| `RibbonInactivateTask` | BUTTON | TOGGLE_BUTTON | 選択タスクが非アクティブなら押下 |

種別とは別に、次の挙動も未対応です。

- 大きい `SPLIT_BUTTON` の矢印位置（Office は下半分、現在は右端）
- KeyTip の表示とキーボード操作
- グループのダイアログ起動ツールの表示
- `GALLERY` / `DROP_DOWN` / `COMBO_BOX` / `CHECK_BOX` / `EDIT_BOX` の専用描画
- screentip / supertip

## 検証コマンド

```
.\gradlew.bat :microproject_ribbon:test
.\gradlew.bat :microproject_ui:test --tests "*MspRibbonControlKindDiffTest" --tests "*RibbonControlContractTest" --tests "*MspRibbonSpecDiffTest"
```
