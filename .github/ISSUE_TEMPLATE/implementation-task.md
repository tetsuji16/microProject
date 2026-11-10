---
name: 実装タスク
about: 本文だけを読んで実装できる作業を定義する（不具合の修正、改善、整理）
title: ''
labels: ''
assignees: ''
---

<!-- 書き方は docs/issue-authoring.md を参照。1 つの issue は 1 つの PR で終わる大きさにする。 -->

## 概要

<!-- 何が問題で、誰が何に困るか。3〜6 行。原因が分かっていれば書く。 -->

## 作業手順

<!-- 番号付き。各手順に対象のパスとシンボル名を書く。行番号を書くときは、基準にしたコミットも書く。 -->

1.
2.

## 検証

<!-- そのまま実行できるコマンド。TEST_PLAN.md の「1.3 issue に書く試験条件」から選ぶ。 -->

```
.\gradlew.bat :microproject_core:test --tests "com.microproject.<package>.<Test>" --console=plain
```

## 完了条件

<!-- 1 行が 1 つの確認。コマンドの結果かテストで判定できる形にする。変えてはいけないものも書く。 -->

- [ ]
- [ ]

## 範囲外

<!-- やらないこと。関連する issue の番号。 -->

## 共通ルール

- master から作業ブランチを作り、PR で出す（master へ直接 push しない）。必須チェックは `build` と `GUI acceptance`。
- `.editorconfig` に従う（UTF-8、CRLF、タブ）。新しいファイルには `Copyright (c) 2026 microProject` の MIT ヘッダーを付ける。
- 新しいコードは `com.microproject` 配下に置く。関係のない整形やリネームを混ぜない。
- テスト出力は失敗と要約だけを報告する。実行していない確認を「通った」と書かない。
