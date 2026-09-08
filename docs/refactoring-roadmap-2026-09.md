# リファクタリング・ロードマップ（2026-09）

## 読む順番（ジュニアエンジニア向け）

microProject は、計画データを扱う **core** を中心に、ファイルを開閉する
**application**、ファイル形式を変換する **exchange**、画面を描く **ui** が載る
デスクトップアプリケーションです。変更時は必ず、ユーザー入力からモデル変更、
Undo/Redo、保存、再読込、再描画までの一連の経路を確認してください。

```text
bootstrap                  独立した更新ランチャー
contrib                    互換・第三者ライブラリ
ribbon                     再利用可能な Swing ribbon API / SPI（アプリ非依存）
core                       計画、日程、リソース、共同編集のドメイン
  ^
  |-- application          open/save と文書ライフサイクル
  |-- exchange             MPP / POD / MPO / XML / XLSX 変換
  |-- reports              レポート生成
       ^
       `-- ui              Swing、リボン、スプレッドシート、Gantt
```

`ui` は上位の組立て役であり、下位層から UI を直接参照してはいけません。
例外的に `core` 内には古い `com.microproject.exchange` の抽象型が残っています。
これはファイル互換性を守る既存境界であり、今回の段階では移動しません。

## 今回のレビュー結果

| 優先度 | 観測した事実 | 影響 | 方針 |
|---|---|---|---|
| P0 | `GraphicManager` は 5,084 行で、アクション登録、選択判定、画面遷移、ファイル操作を併有する | 1つのコマンド修正がリボン、メニュー、ショートカットで分岐しやすい | 単一のコマンド・ルーティング契約へ段階的に抽出する |
| P0 | `Project` は 3,038 行、`Task`/`Assignment` も 2,300 行超 | 予定計算、階層、イベント、永続化の変更範囲が読みにくい | 集約の公開 API を保ったまま、責務単位のサービスへ抽出する |
| P0 | `MpoFileImporter` 1,514 行と `Serializer` 1,762 行が読み書き・検証・マージを併有する | 共有フォルダ競合やラウンドトリップの修正が危険 | 読込、検証、マージ、書込を明示的な段階に分ける |
| P1 | `SpreadSheet` / `CommonSpreadSheet` は合計約3,800行 | Swing の EDT、選択、model/view index、貼付けが絡む | コマンドから表コンポーネントを切り離し、選択解決を一箇所にする |
| P1 | root Gradle には横断依存注入があり、既存の境界検査は reports/exchange に限定されていた | 新規モジュールや import が層を逆流しても見落とし得る | `verifyArchitectureBoundaries` を追加し、全主要モジュールを検査する |

コード行数は 2026-09-08 の `src/main` を対象にした概数です。行数は不具合の証明ではありませんが、責務分離を優先する判断材料です。

## 守る設計契約

1. 画面経路は一つ: 同じ操作はリボン、メニュー、コンテキストメニュー、ショートカットから入っても、同一のコマンド、選択解決、Undo 可能な変更へ到達させる。
2. core は Swing・画面状態を知らない: core はドメイン結果または例外を返し、表示と通知は application/ui が担う。
3. 形式は境界: POD/MPO/MPP/XML/XLSX の既存読込互換性を先にテストで固定してから、内部実装を交換する。
4. 変更は小さく可逆に: 1 PR は1責務。公開 API はアダプターを残して移行し、全 caller を移した後に削除する。
5. GUI は見た目だけで終えない: 実入力、モデル、表示、Undo/Redo、保存/再読込を確認する。詳細は `docs/gui-quality-gate.md` に従う。

## 構築した枠組み

root の `verifyArchitectureBoundaries` を追加しました。これは CI またはローカルで次を失敗として検出します。

- core/application/reports/exchange/bootstrap から、許可されない上位モジュールを import すること
- `SafeObjectInput` の旧 POD 読込別名以外に `com.projectlibre*` を再導入すること

従来の `verifyIndependentBoundaries` は互換エイリアスとして残し、同じ検査を呼びます。

`micrproject_ribbon` はリボンの公開 API/SPI 専用モジュールです。ここには
Project、メニュー、FlatLaf、アイコン資産を置きません。アプリ固有のリソース解析、
コマンド接続、描画は `micrproject_ui` 側に残し、`RibbonCommandSource` と
`CustomRibbonBandGenerator` を通して接続します。これにより将来の OSS 公開時にも
アプリの計画モデルやコマンド実装を依存関係として公開せずに済みます。

実行方法:

```powershell
.\gradlew.bat verifyArchitectureBoundaries --console=plain
```

## 実施順

1. 境界検査を CI の通常 build に接続する（失敗を早期に見つける）。
2. UI コマンド経路を抽出して、GUI の共有フィクスチャで契約を固定する。
3. spreadsheet の選択・変更・再描画を UI コマンドから分離する。
4. Project/Task/Assignment のドメイン責務を抽出し、保存互換性テストを先に追加する。
5. MPO の読込・検証・マージ・書込を段階 API に分割する。
6. 依存性を各モジュールで明示し、root の一括注入を縮小する。

各作業は下記の GitHub issue に分割します。完了条件を満たさない限り、巨大ファイルの機械的な分割や package rename はしません。

- #491: アーキテクチャ境界検査を CI の通常ゲートへ組み込む
- #492: `GraphicManager` のコマンド・ルーティングを単一契約へ抽出する
- #493: `SpreadSheet` の選択解決・変更・再描画を UI コマンドから分離する
- #494: `Project` 集約からライフサイクル・階層・予定計算責務を段階抽出する
- #495: MPO 入出力を読込・検証・マージ・書込の段階 API に分離する
- #496: Gradle の横断依存注入をモジュール宣言へ縮小する
