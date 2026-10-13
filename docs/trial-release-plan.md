# microProject 試用版の準備と確認

この試用版は、基本的なデスクトップ計画作成を利用者に試してもらうための prerelease です。Microsoft Project 全体との互換性完了を示すものではありません。ロードマップの M0 / M1 / M2 / M3 / M5 は引き続き未完了で、Microsoft Project 固有の自動化・拡張機能を扱う M4 は対象外です。MPO（`.mpo`）がネイティブ形式・標準保存先です。MPP は読込の相互運用対象で、MPP 書出しは対象外です。VBA、マクロ、COM、VSTO、Microsoft Office add-in も対象外です。

## 配布経路

`.github/workflows/release-and-pages.yml` の `workflow_dispatch` を使います。現在の `build.gradle.kts` の基準版は `0.0.24` です。GitHub Actions の **Release** workflow を、レビュー済みの最終 commit を含む branch/ref から手動実行し、`release_channel=beta`、`gui_suite=smoke` を選びます。workflow は `v0.0.24-beta.<run number>` と package version `0.0.24.<run number>` を割り当てます。Linux/macOS package job と、Windows の build・package・import・GUI smoke・package contents・launcher の必須検証が成功すると Release job が prerelease を公開します。Pages deployment は公開後の別 job です。公開 assets は Windows MSI と portable ZIP、Linux x64 / macOS Apple Silicon ZIP、および checksum です。

Beta は stable の `latest` assets と自動更新 manifest を更新しません。リリース前に同じ番号の beta release note がある場合はそれを使い、なければ `docs/releases/beta-0.0.24.md` を使います。試用者には成功した run が公開した prerelease URL とその release note を案内してください。

手動 workflow は GitHub 上の選択 ref を checkout します。作業ツリーの未コミット変更は入りません。試用対象の修正を含む最終 SHA をレビューしてから、その SHA が含まれる branch を push し、その branch を選んで workflow を実行します。

GitHub Actions UI を使わない場合は、`release/beta-0.0.24-<N>` branch の push でも同じ release workflow を開始できます。`<N>` は未使用の正整数で、同じ番号が tag `v0.0.24-beta.<N>` と package version `0.0.24.<N>` に使われます。push 前に `git ls-remote --heads origin "refs/heads/release/beta-0.0.24-*"` と `git ls-remote --tags origin "refs/tags/v0.0.24-beta.*"` で既存番号を確認し、branch と tag の両方に重複がない番号を選びます。確認時点では 1 と 2 が使用済みで、3 が次候補です。push する SHA は試用対象の最終レビュー済み commit に固定します。

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

現行ブランチの Build 1132 と GUI 500 は成功したというレビュー時点の CI 記録があります（実行範囲は当該 run 設定に従います）。これは今回公開する package 自体の出荷確認ではありません。公開候補の release workflow がすべて成功することを別途確認します。

基本的なタスク表、階層、依存関係、スケジュール、リソース、進捗、Undo / Redo、MPO 保存・再読込を優先して評価します。MPP 読込は別途確認中の相互運用機能です。MPO の過去版読込、ファイル形式ごとの全意味、全コマンド、全 locale / DPI、Microsoft 画面との視覚比較はロードマップに残っています。試用上の成功を M1〜M5 の完了や Microsoft Project 互換の保証として扱いません。Microsoft Project がインストールされていなくても試用できます。

不具合報告には prerelease の URL / tag、OS、画面倍率、アプリ言語、利用したファイル形式、再現手順、期待結果と実際の結果を含めてください。可能なら個人情報・機密情報を除いた小さな再現用 MPO と画面を添付します。保存内容の欠落、表とガントの不一致、Undo / Redo の破損、起動不能は優先して報告してください。
