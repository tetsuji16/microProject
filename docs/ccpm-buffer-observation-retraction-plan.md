# CCPM バッファ履歴: 誤プロット観測の破棄仕様

## 決定

フィーバーチャートの過去点は、グラフ座標を編集して訂正しない。ユーザーは誤って追加された観測を **破棄** できる。破棄は物理削除ではなく、元の観測 ID、破棄理由、実行者、実行時刻を保存する論理削除である。通常のグラフ・CSV・HTML には破棄済み観測を出さず、監査データには残す。

これは、CCPM の状態グラフが進捗・残作業・バッファ消費から導かれる記録であり、後から点の座標だけを変えると報告の由来を失うためである。元のプロジェクト実績を訂正する必要がある場合は、通常のタスク進捗／残期間の編集を行い、後続の観測として記録する。

## コマンド契約

```text
Command: CCPM_BUFFER_OBSERVATION_RETRACT
User routes: Buffer Status chart の点選択 → 「この観測を破棄…」
Selection: active CCPM document 内の未破棄 observationId を一件だけ
Allowed state: 文書が書込み可能、CCPM history があり、理由が空でない
Model before -> after: active observation を一件除外し、Retraction(observationId, actor, time, reason) を一件追加
Visible before -> after: 該当点と接続線が消え、状態メッセージに理由と時刻を示す。監査ビューでは破棄済みとして表示
Undo/Redo: 一回の Ctrl+Z が観測を復元し監査レコードを消す。Ctrl+Y が再破棄する
Persistence: MPOF ccpm/history.jsonl に observation id と retraction を保存。旧形式の id なし observation も読める
Invalid state: 選択なし、読み取り専用、理由なし、見つからない／既に破棄済みは明示的に拒否する
Diagnostic result: changed(retracted id) / rejected(reason-required|no-observation|history-empty|read-only) / failed
```

## 今回実装した骨組み

1. `CriticalChainBufferHistory.Point` に安定した `observationId` を追加した。旧コンストラクタは残し、既存の利用元と id のない MPOF v1 history を壊さない。
2. `Retraction` を監査エンティティとして追加した。理由は必須で、元の point を active history から除外する。
3. `CriticalChainBufferHistoryService.retract(...)` を唯一の model mutation 経路にした。`Outcome` は `changed/rejected` と理由を返し、Undo/Redo で active points と audit retractions を丸ごと復元する。
4. `ccpm/history.jsonl` は `kind=observation` と `kind=retraction` を書く。旧 history entry は読み込み時に決定的な UUID を与え、後方互換で復元する。

## 後続実装（ジュニア担当）

1. `CriticalChainBufferChartPanel` に hit-test を実装する。描画座標ではなく `observationId` を選択状態として保持し、隣接点が重なる場合は最新点を選ぶルールを明記する。
2. Buffer Status dialog に、選択済みかつ書込み可能な場合だけ有効になる「この観測を破棄…」ボタンを追加する。既存の chart / panel の別実装を作らない。
3. 破棄確認ダイアログは、観測時刻、進捗率、バッファ消費率、ゾーンを読み取り専用で出し、理由入力を必須にする。最新 active point は追加の確認文を表示する。
4. 確定時は `CriticalChainBufferHistoryService.retract` だけを呼ぶ。chart の list を直接 `remove` してはならない。`Outcome` を status label と diagnostic log に反映する。
5. Undo/Redo 後には chart と export の表示を再読込する。選択済み ID が消えたら選択を解除する。読み取り専用文書では mutation UI を表示せず、理由を tooltip/accessible description で説明する。
6. CSV/HTML には active point だけを出す。将来の「監査エクスポート」は通常エクスポートと別の明示操作にする。

## 受入基準

- headless: 理由ありの破棄で active point が一つ減り、retraction が一つ増える。空理由／未知 ID は変更なし。Undo/Redo が正確に前後状態を復元する。
- MPOF: active point と retraction を保存・再読込し、破棄済み点をグラフ・通常 export から除外する。id のない旧 JSONL も読む。
- GUI: 実 Robot で点をクリックし、理由を入力して破棄する。点・接続線・件数が変わり、タスク日付／依存関係／baseline は不変であることを確認する。Ctrl+Z/Ctrl+Y も実キー入力で確認する。
- layout: 日本語・英語、100/125/150% で確認ダイアログの観測値、理由欄、操作ボタンが viewport 内にあり、重ならない。
- route: ribbon/menu/shortcut を増やさない。Buffer Status 上の唯一の canonical service route にする。

## 非対象

- 過去観測の座標・時刻・進捗率の直接編集
- CCPM 計算式、バッファ閾値、タスク実績の自動修正
- 全履歴の一括削除（既存の CCPM Clear は別コマンド）
- 監査エクスポートと共同編集時の retraction conflict resolution
