# 書籍執筆で記録した機能ギャップ

この一覧は、書籍執筆中に根拠を確認した機能ギャップと、追加調査が必要なテーマを分けて記録します。機能の調査中は未実装と断定しません。

## 登録済みissue

下記3項目は既存issueを検索し、同一の実装スコープがないことを確認して登録しました。状態と完了範囲は各issueの最新記録に合わせています。

### 出来高管理（EVM）の利用者向け経路

- 登録済みissue: [#943 EVM: 確認基準日・利用者向け入力経路・レポート表示を確認する](https://github.com/tetsuji16/microProject/issues/943)
- 状態：**COMPLETE**。利用者向け経路、既知値の計算、集計・基準日、MPO保存・再読込、およびカスタムレポートのプレビューとCSVを受け入れ確認済み。
- 操作範囲：PV・EV・ACは直接入力ではなく、基準計画、進捗・実績作業、リソース単価や固定費から計算する。状況報告日を設定し、カスタムレポートでBCWS/BCWP/ACWP/CV/SV/CPI/SPIを選択してプレビューし、必要ならCSVと照合する。
- 残る制約：複数通貨の費用をアプリ側で検出・換算しないため、統合前に通貨をそろえる。詳細と受け入れ証跡は [issue #943](https://github.com/tetsuji16/microProject/issues/943) および `docs/work/issue-943.md` を参照。
- 重複確認：#743はTask/NormalTaskのモデル責務分離、#453/#769は広域互換性・リボン監査であり、EVM利用者向けワークフローとは別範囲。

### タスク書式のコピー（Format Painter）

- 登録済みissue: [#941 MSP互換: タスク書式のコピー（Format Painter）を実装・検証する](https://github.com/tetsuji16/microProject/issues/941)

- 現状：タスクリボンの「書式のコピー」コマンド、タスク文字書式の適用サービス、Undo/Redo・MPO再読込を扱うGUI受け入れシナリオは実装済みです。issue #941 は現行リリース／SHAの受け入れ証跡確認のため open です。
- インベントリのFormat Painter行は `PARTIAL` に更新済みです。現行リリース／SHAでGUI受け入れ証跡を確認し、元の全条件を満たした場合に `VERIFIED` へ進めます。以前のowner #794 はクローズ済みです。
- MSPの参考資料：<https://support.microsoft.com/en-us/word/use-the-format-painter> (現行ページはProject desktopも対応製品に含むが、プロジェクトタスクのコピー対象フィールドは別途定義が必要)。
- 候補の受け入れ範囲：タスク書式のどの属性を複写するか定義し、選択範囲・解除・Undo/Redo・保存再読込まで確認する。既存のコピー／貼り付けとは責任を分ける。
- 重複確認：#453／#769は広範な仕様・監査であり、Format Painter固有の実装issueではありません。

### タイムライン／レポートのオブジェクト固有書式と操作範囲

- 登録済みissue: [#942 Timeline／Reportの選択オブジェクトに応じた書式コンテキスト面を実装・検証する](https://github.com/tetsuji16/microProject/issues/942)

- 根拠：同インベントリの「Report/Timeline object-specific formatting」は `PARTIAL`。以前のowner #794 はクローズ済みのため、新しい実装issueで追跡します。
- MSPの参考資料：<https://support.microsoft.com/en-us/project/create-a-timeline-in-project>、<https://support.microsoft.com/en-us/project/create-a-project-report>
- 候補の受け入れ範囲：必要な操作、選択対象、表示結果、保存・再読込を個別に定義し、既存ビューと混同しない。
- 重複確認：#366はレポート生成・Chart・Custom Reportの挙動を扱い、この選択コンテキスト課題とは別範囲。#453 §4／#769の仕様・監査と連携する。

## 追加調査が必要（まだissue化しない）

- カスタムフィールド、ビュー、フィルターの総合範囲：既存操作と一部検証がある。フィールド種類、数式、並べ替え・グループとの組合せを確認し、既存issueが所有していない不足だけを切り出す。
- 定期タスク：挿入経路とモデルが存在する。反復条件、日付境界、依存関係、保存後の動作を調べ、既存の互換性issueで追跡されているか照合する。
- リソース平準化：基本機能は実装済みだが、オプション別の適用範囲や結果の一部が確認中。既存issueの受け入れ条件と重複させない。

## 別issueを作らない項目

Microsoft Project VBA/macros、COM/VSTO、Officeアドイン、Project Server/Online等の接続サービス、MPP書き出しは、プロジェクトの明示的な製品範囲外である。書籍では対象外として記載し、互換性ギャップのissueとして新規作成しない。
