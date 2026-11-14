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
| `mspActivation` | Microsoft Project で本体をクリックした結果（後述の「クリック時の挙動」） |
| `evidence` | `BASELINE`: 現在の実装をそのまま記録した行（未照合）。`PENDING_VERIFY`: 資料から導いた差分で、行単位の照合待ち。`DOCUMENTED`: `ProjectControls.xlsx` と照合済み |
| `location` | `タブ/グループ` または `TaskBar` |
| `popupItems` | 想定するポップアップの内容（説明文） |

`MspRibbonControlKindDiffTest` はモデルの種別と CSV を比べます。未対応の差分は同テストの `KNOWN_GAPS` に「現在の種別」と共に列挙します。対応を実装すると、エントリを削除するまでテストが失敗するので、一覧は減る方向にしか動きません。回帰を通すためにエントリを足してはいけません。

## 現時点の種別差分（1 件）

| コマンド | 現在 | Microsoft Project | ポップアップの内容 |
|---|---|---|---|
| `RibbonChooseSort` | SPLIT_BUTTON | MENU | 開始日順 / 終了日順 / 優先度順 / コスト順 / ID 順 / 並べ替え。「並べ替え」ダイアログのコマンドが未実装 |

種別は契約どおりでも、次の点は未対応です。

- KeyTip が未設定のコントロールがある（Help タブと文脈 Format タブのアクセス キー、Task / View タブの多くのコントロール）
- 本番の `menuInternal.properties` には `GALLERY` / `COMBO_BOX` / `EDIT_BOX` を使うコマンドがまだ無い（描画とモデルは対応済み）
- 「その他のフィルター / グループ」、形式を選択して貼り付け、図のコピー、空白行の挿入、置換、ジャンプはメニュー項目に無い（`.text` を持つコマンドが無い）
- グループのダイアログ起動ツールは 3 グループだけ（タスクの表示 → カレンダー ビューの設定、タスクの追跡 → タスク情報、リソースのプロパティ → リソース情報）

## クリック時の挙動（`RibbonActivation`）

種別（どの Office コントロールか）とは別に、本体をクリックした結果を `com.microproject.ribbon.RibbonActivation` で表します。分割ボタンは本体の挙動で分類します（矢印は常にポップアップを開きます）。

| 値 | クリックの結果 | クリック後の見た目 |
|---|---|---|
| `COMMAND` | 即時に実行する | 通常表示に戻る |
| `DIALOG` | ダイアログまたはウィンドウを開く | 通常表示に戻る |
| `POPUP` | コントロールに付いたポップアップを開く | 開いている間だけ押下表示 |
| `TOGGLE_STATE` | オン / オフの状態を切り替える | オンの間は押下（反転）表示が残る |
| `EXCLUSIVE_VIEW` | 表示中のビューを切り替える | そのビューが表示中の間は押下表示。再クリックしても解除されない |
| `MODE` | 入力モードに入る | モードが終わるまで押下表示 |
| `BACKSTAGE` | Backstage のページを開く | — |

### 宣言と既定値

`menuInternal.properties` の `<id>.activation` で宣言します。省略時は他のメタデータから推定します（`RibbonActivation.defaultFor`）。

1. 種別がポップアップ専用（`MENU` / `GALLERY` / `DROP_DOWN` / `COMBO_BOX`）→ `POPUP`
2. `<id>.backstagePage` があり `backstageImmediate` でない → `BACKSTAGE`
3. `<id>.type=TOGGLE` → `TOGGLE_STATE`
4. それ以外 → `COMMAND`

`DIALOG` / `EXCLUSIVE_VIEW` / `MODE` は推定できないので、必ず宣言します。次の矛盾はモデル構築時に `IllegalStateException` になります。

- `POPUP` とポップアップ専用の種別は必ず一致する
- 押下状態が残る値（`TOGGLE_STATE` / `EXCLUSIVE_VIEW` / `MODE`）と `type=TOGGLE` は必ず一致する
- `BACKSTAGE` と「Backstage のページを開くコマンド」は必ず一致する

### 押下表示の仕組み

- 押下状態が残る値：アプリケーション側が `MenuManager.setActionSelected(actionId, boolean)` を呼び、`Action.SELECTED_KEY` 経由でボタンの選択表示に反映します。呼び出し元は `DocumentFrame`（上ビュー、下ペイン、書式のコピー）と `GraphicManager`（進捗線、ラベル、クリティカル チェーン、チーム フィルター）です。
- `POPUP`：`RibbonControlPopups` が、ポップアップ表示中だけボタンの client property `MicroProject.ribbon.popupOpen` を `TRUE` にし、その間は押下色で描画します。
- 各ボタンは client property `MicroProject.ribbon.activation` に自分の値を持ちます。

### 契約と差分テスト

CSV の `mspActivation` 列が Microsoft Project での挙動です。`MspRibbonActivationDiffTest` がモデルと比べ、差分は `KNOWN_ACTIVATION_GAPS` に「現在の挙動」と共に列挙します（`KNOWN_GAPS` と同じく減る方向のみ）。同テストは、契約の種別と挙動の組み合わせが成り立つことも検査します。

### 現時点の挙動差分（1 件）

資料から導いた判断（document-derived compatibility decision）で、行単位の照合待ち（`PENDING_VERIFY`）です。

| コマンド | 現在 | Microsoft Project | 補足 |
|---|---|---|---|
| `RibbonChooseSort` | DIALOG | POPUP | クリックでメニューを開く。ダイアログはメニュー項目から開く |

契約と一致していても、次の点は未確認または未対応です。

- `DIALOG` / `EXCLUSIVE_VIEW` / `MODE` の GUI 受け入れテスト（`RibbonDialogCommandGuiAcceptanceTest`、`RibbonViewStateGuiAcceptanceTest`）は CI のデスクトップで実行する
- 製品独自の `RibbonTaskInspector` は状態を切り替えるのに押下表示が無い
- チェック ボックスは押下時の背景を JToggleButton と共有しており、Office の見た目と少し異なる

## 検証コマンド

```
.\gradlew.bat :microproject_ribbon:test
.\gradlew.bat :microproject_ui:test --tests "*MspRibbonControlKindDiffTest" --tests "*MspRibbonActivationDiffTest" --tests "*RibbonControlContractTest" --tests "*MspRibbonSpecDiffTest"
```
