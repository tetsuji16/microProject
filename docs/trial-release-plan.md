# microProject 安定版の試用と確認

この手順は、現在実装されている基本的なデスクトップ計画作成機能を安定版で試すためのものです。「安定版」は記載した実装範囲と出荷確認の位置づけであり、Microsoft Project 全体との互換性完了を示しません。ロードマップの M0 / M1 / M2 / M3 / M5 は引き続き未完了で、Microsoft Project 固有の自動化・拡張機能を扱う M4 は対象外です。MPO（`.mpo`）がネイティブ形式・標準保存先です。MPP は対応範囲を限定した読込相互運用で、MPP 書出しは対象外です。VBA、マクロ、COM、VSTO、Microsoft Office add-in も対象外です。

## 配布経路

PR reviewでは exact-head Build、GUI smoke、変更した物理操作の重点的な証拠を確認します。merge 前の重複 full GUI は必須にしません。merge後、`.github/workflows/release-and-pages.yml` の **Release** workflow を `release/stable-0.0.24` branch のレビュー済み commit から開始します。branch push または `release_channel=stable` の手動実行が使え、stable gate は常に `gui_suite=full` を要求します。workflow は run number から `v0.0.24.<run number>` と package version `0.0.24.<run number>` を割り当て、build・全 module tests・package・import・full GUI acceptance・package contents・launcher 検証が成功した後だけ tag と stable release を作成します。stable tag を事前作成しないでください。大きな cross-platform 更新の場合だけ `build_unix_packages=true` を指定し、その場合は Linux と macOS の両方の build 成功も公開条件です。Pages deployment は公開後の別 job です。checksum はその release に含む assets を対象に生成します。

通常リリースは Windows を毎回ビルドします。Linux と macOS の配布物は大きな更新時だけ作成し、手動実行時に `build_unix_packages=true` を選びます。既定値は `false` で、tag / release branch の push も Windows のみです。選択した Unix build が失敗した場合は、そのリリース全体を公開しません。公開サイトは OS ごとの stable Release assets を確認し、Linux と macOS は各 OS の最新 stable 配布 tag を個別に表示します。各 OS の配布版は異なる場合があります。

過去の beta release は prerelease の履歴として残っています。現在の安定版を案内するときは、成功した stable run が公開した release URL と release note を使い、beta の URL を試用版として案内しないでください。

手動 workflow は GitHub 上の選択 ref を checkout します。作業ツリーの未コミット変更は入りません。試用対象の修正を含む最終 SHA をレビューしてから、その SHA が含まれる branch を push し、その branch を選んで workflow を実行します。

GitHub Actions UI を使わず push で開始する場合は、`release/stable-0.0.24` branch への push で同じ full release workflow が実行されます。stable branch は `master` に統合済みの最終レビュー済み commit から作成し、push する SHA をその候補に固定します。stable tag と package version は workflow が run number から生成します。Tag push は `v*-beta.*` の beta prerelease に限定し、stable release は stable branch push または matching branch からの stable dispatch のみです。

## 試用者向け 10〜15 分チェック

重要な計画ではなく、新規の小さな計画か複製ファイルで確認します。OS、アプリ版、表示倍率、アプリ言語を最初に記録してください。

1. 新しいプロジェクトを作り、親タスクと子タスクを含む 3〜5 件のタスクを入力します。期間、開始日、終了日を設定します。
2. タスク間に依存関係を追加し、ローカルリソースを一つ割り当てます。開始日・期間・依存関係を変え、タスク表とガントの表示が整合するか見ます。
3. 進捗を 0%、途中の値、100% に変え、数値とガント表示を確認します。
4. 階層、依存関係、進捗の変更をそれぞれ Undo / Redo し、前後の状態が戻るか見ます。
5. MPO に保存し、プロジェクトを閉じて開き直します。タスク階層、日付・期間、依存関係、リソース割当、進捗が残るか確認します。
6. 既存 MPP の読込を試す場合は複製したファイルを使い、読込後に MPO へ保存して開き直します。MPP へ書き戻す操作は試用対象ではありません。

各操作は、モデル値だけでなくタスク表とガントの見た目、Undo / Redo 後、MPO 再読込後まで確認します。問題が起きたときは続けて上書き保存せず、再現手順と画面を先に記録します。

## 試用の範囲と未確認事項

stable release workflow の成功は、対象 commit の build、tests、package、full GUI acceptance、package launch の出荷確認です。個別の利用環境や記載外の機能すべてを保証するものではありません。

基本的なタスク表、階層、依存関係、スケジュール、リソース、進捗、Undo / Redo、MPO 保存・再読込を優先して評価します。MPP 読込は対象ファイルや意味の網羅を保証しません。MPO の全過去版、ファイル形式ごとの全意味、全コマンド、全 locale / DPI、Microsoft 画面との視覚比較はロードマップに残っています。試用上の成功や stable release の公開を M1〜M5 の完了や Microsoft Project 互換の保証として扱いません。Microsoft Project がインストールされていなくても試用できます。

不具合報告には release の URL / tag、OS、画面倍率、アプリ言語、利用したファイル形式、再現手順、期待結果と実際の結果を含めてください。可能なら個人情報・機密情報を除いた小さな再現用 MPO と画面を添付します。保存内容の欠落、表とガントの不一致、Undo / Redo の破損、起動不能は優先して報告してください。
