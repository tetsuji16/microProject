# 画面写真の取得・検証記録

## 取得条件と環境

- 撮影日：2026年10月26日
- 対象リリース：microProject v0.1.0.1271（GitHub Releases公開版）
- 仮想環境：WSL2上のUbuntu、WSLg経由でデスクトップ版を起動
- 配布物：公式 `microProject-0.1.0.1271-linux-x64.zip`。GitHubの公開アセットdigest `sha256:883A5E582A9E5C6CA2F4CE612946D92A34E7BBACFE1E9DA730F2D022D33A2E11` と照合
- プロファイル：撮影専用の一時 `HOME` / XDG設定・データ領域を使用
- サンプル：新規作成した `Sample Project`。`Plan the launch`、`Draft requirements`、`Review requirements`、`Build prototype`、`Prepare demo` の5件。氏名・組織・実案件データ・外部ファイルを含まない

## 画像一覧と確認

| ファイル | 内容 | 個人情報確認 |
|---|---|---|
| `figures/ch02-task-dependencies.png` | 5件のタスク、期間、日付、先行タスク番号 | アプリ領域だけに切り抜き。合成タスクのみ。個人名、組織名、アカウント、パスなし |
| `figures/ch02-task-detail.png` | 同じタスク表からタスク名・期間・先行番号を拡大 | 上記画像のアプリ内表部分の切り抜き。個人情報なし |
| `figures/ch03-dates-predecessors.png` | 同じタスク表から日付・先行番号を拡大 | 上記画像のアプリ内表部分の切り抜き。個人情報なし |

| `figures/ch05-resource-usage.png` | Resource Usageビュー | 仮想環境内のアプリ領域のみ。合成名のみ |
| `figures/ch05-tracking-gantt.png` | 5件の練習タスクを表示するTracking Gantt | 背後のホスト画面を除いてアプリ領域のみ切り抜き。基準計画未設定の状態と本文で明記 |
| `figures/ch07-project-overview.png` | ProjectsビューのSample Project概要行 | 背後のホスト画面を除いてアプリ領域のみ切り抜き。合成名・合成日付のみ |
| `figures/ch08-ccpm-settings.png` | CCPM設定のResource Levelingタブ | CCPM設定ウィンドウ。計算結果を示す図ではないと本文で明記 |

デスクトップ全体には個人用ウィンドウが存在するため、採用画像にデスクトップや背後のウィンドウが入っていないことを一枚ずつ確認した。画像はPNGで保存し、画像自体も目視した。タスクの開始日は練習用に設定した2026年10月26日である。

## 継続課題

最初の撮影セットはライブ画面9点（全画面7点、アプリ内詳細切り抜き2点）だった。このうち英語版と重複した2点（`ch01-start-project.png`、`ch04-resource-sheet.png`）は削除済みで、現行原稿は残る7点と後から追加した操作写真8点、計15点のライブ画面を参照する（全画面13点、詳細切り抜き2点）。別に、調査記録用の `ch10-earned-value-report-live.png` が1点あるが、教科書には掲載しない。Tracking Ganttの写真は基準計画を設定していない例であり、CCPM設定画面も計算結果ではない。複数プロジェクトのリンク動作、CCPM適用後のネットワーク・バッファ状況は未撮影である。実装済み・確認済みと誤認させる画像は掲載しない。

第6章、第9章、第11章には概念図も併記する。第9章のタスクモード操作は第2章のタスクタブ画面を再利用し、第11章のフィルター／並べ替え／グループ操作はリソース割当画面に写るビュータブのコマンドを再利用する。第12章は計画の仕上げと受け渡しを扱うため、画面写真を用いない。

現行原稿で参照している次の7画像は英語表示のままで、本文のラベルと一致していないため、日本語ロケールでの撮り直しが未完了である：`ch02-task-dependencies.png`、`ch02-task-detail.png`、`ch03-dates-predecessors.png`、`ch05-resource-usage.png`、`ch05-tracking-gantt.png`、`ch07-project-overview.png`、`ch08-ccpm-settings.png`。このdraft PRでは撮り直しを行っておらず、7点を未対応として残す。日本語版と重複していた英語画像 `ch01-start-project.png`・`ch04-resource-sheet.png` と、第12章の改訂で使わなくなった `ch12-coverage-status` の図は削除済み。

## 操作手順に追加したライブ画面

microProjectの実画面で、新規作成からレポート表示までを操作して追加撮影した。プロジェクト・タスク・リソース名は教材用の合成データで、既存ファイルを開いたり保存したりしていない。

| ファイル | 本文で示す操作 | 個人情報確認 |
|---|---|---|
| `figures/ch01-new-project-dialog.png` | 新規プロジェクトの名前・管理者・開始日入力 | 合成名のみ |
| `figures/ch01-project-workspace.png` | 作成後のタスク表とガント領域 | 合成名のみ |
| `figures/ch02-task-table-live.png` | タスクタブと練習用行 | 合成タスクのみ |
| `figures/ch03-linked-tasks-live.png` | 先行タスク番号とガント依存線 | 合成タスクのみ |
| `figures/ch04-resource-sheet-live.png` | リソースシートへの担当者登録 | 合成名のみ |
| `figures/ch04-resource-assignment-live.png` | タスクへのリソース割当 | 合成名のみ |
| `figures/ch05-save-baseline-dialog.png` | プロジェクト全体の基準計画保存先 | 合成プロジェクトのみ |
| `figures/ch06-report-preview-live.png` | Project Detailsレポートのプレビュー | 合成プロジェクトのみ |
| `figures/ch10-earned-value-report-live.png` | 修正前のレポート不具合の調査記録。Issue #366/#943に記載 | 教科書には掲載しない。撮影時の旧版ではプレビューの見出しは出るが、合成サンプルのタスク行は空 |

第6章の写真はProject Detailsを示す。第10章の画面写真は修正前のレポート表示上の問題を記録する調査資料であり、現在の挙動を示す画像ではないため教科書には掲載しない。Issue #943の後続修正では、EVM列のプレビュー値とCSV値が一致するWindows GUI受け入れ確認が行われている。

第7〜9章と第11章は既存の実画面写真を手順に再配置して利用する。第7章のProjects写真はプロジェクト一覧、第2章のタスクタブ写真は「サブプロジェクト」コマンドとスケジュール欄、第8章のCCPM写真は設定ダイアログ、第4章のビュータブ写真はフィルター・並べ替え・グループの位置を示す。第8章の設定写真はプレビュー／適用後の結果ではない。プロジェクト間リンク、CCPM計算結果、定期タスクの生成結果、カスタムフィールド編集の画面写真は引き続き未収録とし、手順を確認できないものはその境界を本文に残す。
