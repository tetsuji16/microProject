# タスク表・ガントチャート追加100ケース実行報告（2026-07-17）

## 結論

前回の18ケースとは別に、IDが重複しない100ケースをJUnit動的テストとして追加した。JUnit XMLで `tests=100`、IDの総数100、ユニーク数100を確認した。初回は89件成功・11件失敗、原因を分類・修正した後は100件すべて成功した。

## 100ケースの構成

| ID範囲 | 件数 | 対象 | 主な入力・境界 |
|---|---:|---|---|
| `TC100-G001`〜`TC100-G020` | 20 | マイルストーン選択座標 | 正負・小数の中心座標、shapeとselection squareの大小・同値・0 |
| `TC100-L021`〜`TC100-L040` | 20 | ガント注釈配置 | バーの左右端、clip内外、狭いclip、負座標、null clip |
| `TC100-C041`〜`TC100-C050` | 10 | 注釈文字クリップ | null、空白、幅0、1文字、長文、日本語、依存関係ラベル |
| `TC100-K051`〜`TC100-K060` | 10 | 注釈キャッシュキー | field/formatのnull・空・Unicode・区切り文字・空白保持 |
| `TC100-S061`〜`TC100-S070` | 10 | タスク表カテゴリ解決 | project/task/resource/assignment/dependency/未知/null |
| `TC100-S071`〜`TC100-S080` | 10 | タスク表の列並べ替え | 2〜6列、先頭・末尾・中間からの双方向移動 |
| `TC100-V081`〜`TC100-V100` | 20 | タスク表とガントの行高同期 | baselineなし、疎なbaseline、0〜100、既定高・baseline高の0境界 |
| **合計** | **100** | | |

各ケースの入力値とassertionは次のテストソースを正本とする。

- `TaskTableGanttHundredCasesGanttTest`: 60件
- `TaskTableGanttHundredCasesSpreadsheetTest`: 20件
- `TaskTableGanttHundredCasesSyncTest`: 20件

## 初回失敗の評価

| 対象 | 初回結果 | 判定 | 対応 |
|---|---:|---|---|
| `TC100-S070` | FAIL | 製品バグ。カテゴリ未設定時に `String.equals` を呼びNPE | `SpreadSheetUtils.getFieldsForCategory(null)` がnullを返すよう修正 |
| `TC100-S071`〜`TC100-S080` | FAIL | テスト設定誤り。非表示ID列を生成せず、表示列数が不足 | 実際の列モデル契約どおりID列を含めて生成するよう修正 |
| その他89件 | PASS | 製品挙動は期待どおり | 変更なし |

テスト設定誤りは製品バグとして数えず、実装契約を確認してテストだけを修正した。製品コードは、再現性が確認できたnullカテゴリのNPEのみ修正した。

## 最終実行結果

### 追加100ケース

- Gantt: 60 tests, 0 failures, 0 errors
- Spreadsheet: 20 tests, 0 failures, 0 errors
- View synchronization: 20 tests, 0 failures, 0 errors
- 合計: **100 tests, 0 failures, 0 errors**
- 動的テストID: **100 IDs / 100 unique**

### 全回帰テスト（`build --rerun-tasks`）

- Core: 189 tests
- UI: 498 tests（既存398 + 追加100）
- Exchange: 34 tests
- Application: 7 tests
- Reports: 5 tests
- 合計: **733 tests, failures 0, errors 0, skipped 0**
- 全40 Gradleタスクを再実行し、`BUILD SUCCESSFUL`

## 実行コマンド

```powershell
.\gradlew.bat :projectlibre_ui:test --no-daemon --console=plain `
  --tests "com.projectlibre1.pm.graphic.gantt.TaskTableGanttHundredCasesGanttTest" `
  --tests "com.projectlibre1.pm.graphic.spreadsheet.TaskTableGanttHundredCasesSpreadsheetTest" `
  --tests "com.projectlibre1.pm.graphic.views.TaskTableGanttHundredCasesSyncTest"

.\gradlew.bat build --rerun-tasks --no-daemon --console=plain
```
