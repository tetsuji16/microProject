# タスクリンク・タスクバー移動 30ケース実行報告（2026-07-17）

## テスト構成

| ID | 件数 | 検証内容 |
|---|---:|---|
| `TLBM-LINK-01`〜`12` | 12 | FS/SS/FF/SF × lag 0・正・負。リンク両端、type、lag、再計算後の開始日時を検証 |
| `TLBM-LINK-13` | 1 | 3タスクFSチェーンで先頭期間を1日から4日へ変更し、後続2件が伝播移動 |
| `TLBM-LINK-14` | 1 | A→B作成後のB→A循環リンクを拒否し、既存リンクを保持 |
| `TLBM-LINK-15` | 1 | リンク解除後、predecessor/successor双方の一覧から削除 |
| `TLBM-BAR-16`〜`20` | 5 | 3日バーを-5/-2/-1/+1/+5稼働日移動。開始、終了方向、期間、SNETを検証 |
| `TLBM-BAR-21`〜`24` | 4 | 終了端をドラッグして1/2/5/10日にリサイズ。開始固定、期間、finishを検証 |
| `TLBM-BAR-25`〜`27` | 3 | 期間セルを1/3/7日に変更。開始固定と終了再計算を検証 |
| `TLBM-BAR-28` | 1 | 開始日を3稼働日後へ変更し、2日期間を維持 |
| `TLBM-BAR-29` | 1 | 終了日を4稼働日後へ変更し、2日期間を維持して開始を逆算 |
| `TLBM-BAR-30` | 1 | 0日マイルストーンを5稼働日移動し、開始=終了と0日期間を維持 |
| **合計** | **30** | |

## 検出・修正した不具合

1. ガント終了端ドラッグでFNLTを設定していたため、開始固定のリサイズにならず、開始が移動する／終了が元へ戻ることがあった。全バー操作をSNETで開始固定するよう修正した。
2. assignmentなしタスクの`moveInterval`がraw durationだけを変更し、remaining durationとfinishが不整合になっていた。通常の`setDuration`経路で同期するよう修正した。
3. interval更新直後に対象タスクを再計算対象へ登録していなかったため、旧finishキャッシュが再利用されていた。対象タスク自身も再計算待ちにした。
4. GanttInteractorがinterval更新後に制約確定のため再度スケジューリングし、更新済みfinishを旧値で上書きしていた。後段では制約値の確定とUndo登録だけを行うよう修正した。
5. duration同期後にcanonical finishを復元していなかった。呼び出し側が指定した終業時刻表現を保持するよう修正した。

## 実行結果

- 追加30ケース: 30 tests, failures 0, errors 0, skipped 0
- Core全体: 219 tests, failures 0, errors 0, skipped 0
- UI全体: 498 tests, failures 0, errors 0, skipped 0
- Exchange: 34 tests, failures 0, errors 0, skipped 0
- Application: 7 tests, failures 0, errors 0, skipped 0
- Reports: 5 tests, failures 0, errors 0, skipped 0
- 全体: 763 tests, failures 0, errors 0, skipped 0
- `.\gradlew.bat build --rerun-tasks --no-daemon --console=plain`: `BUILD SUCCESSFUL`
