# microProject

リポジトリのルールは `AGENTS.md` にまとめてあります。必ず最初に読んでください。

@AGENTS.md

## 作業の種類ごとに読むもの

| 作業 | 読むもの |
|---|---|
| issue を実装する | `docs/issue-execution.md`、スキル `implement-issue` |
| issue を書く、整理する | `docs/issue-authoring.md`、スキル `write-issue` |
| 試験を選ぶ | `TEST_PLAN.md` の「1.3 issue に書く試験条件」 |
| Microsoft Project との互換を判断する | `docs/agent-workflows.md` |
| 画面を変える | `docs/gui-quality-gate.md` |

## コンテキストの節約

- テストとビルドの出力は、ファイルに保存して、失敗と要約だけを読む。
- ファイルは `rg` で位置を確かめてから、必要な範囲だけを読む。
- 画面の確認は、文字で読める方法を先に使う。画像は、見た目の判断に必要なときだけ使う。
