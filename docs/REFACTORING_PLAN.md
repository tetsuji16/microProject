# ProjectLibre リファクタリング状況

最終更新: 2026-07-19

## 方針

- Gradle の 6 サブプロジェクトを現行構成として維持する。
- プロジェクトファイル、スケジュール計算、Swing の model/view 変換、保存・再読込を互換性境界として扱う。
- 症状を隠す変更や、仕様の確定していない機能追加をリファクタリングに混ぜない。
- 同梱した第三者ソースやローカル bridge jar は、互換性テストを用意してから Maven 依存へ置き換える。
- 巨大クラスの分割は characterization test を先に追加し、責務ごとの小さな変更として進める。

## 今回完了した整理

### 依存関係

- `modules/projectlibre_exchange/src/main/java/net/sf/mpxj` に複製されていた MPXJ ソースを削除した。
- MPXJ は `net.sf.mpxj:mpxj:11.5.4` を Gradle version catalog から利用する。ProjectLibre 固有の writer 選択と enum 変換は `com.projectlibre1.exchange.mpxj` に隔離した。
- `modules/projectlibre_ui/src/main/java/org/pushingpixels` に複製されていた Flamingo、Neon、Trident ソースを削除した。
- Flamingo は `org.pushingpixels:flamengo:5.0`、Neon と Trident は既存の Radiance Maven 依存を利用する。
- 空になっていた contrib/report bridge classpath 定義を削除した。
- `isolated-build/` と `modules/projectlibre_contrib/lib` に Git 管理中の成果物はない。ローカル生成物を配布の正本として扱わない。

### 実装

- `ClassLoaderUtils` の Java バージョン初期化、比較処理、例外処理を明示化した。
- `DataSourceProvider` の singleton と遅延初期化を thread-safe にし、report field の実際の値型を返すようにした。
- 設定 XML の classpath resource が欠落した場合を安全に処理し、stream を確実に閉じるようにした。
- `Finder` の raw collection と厳密すぎる数値型比較を整理した。
- `CalendarManager` が名称変更済みカレンダーを削除した際に古い name index を残さないようにした。
- `OutlineCode` のフォーマット検証と数値連番処理を修正した。
- `FieldUtil` の deprecated reflection を置き換え、値型と互換な overloaded setter を選択するようにした。
- 公開される設定 field collection と周辺 collection を型安全にした。

各修正には、再現可能な箇所について focused regression test を追加した。

## 今回変更しない項目

次のコメントは、単純な構造整理ではなく計算仕様・永続化仕様・製品要件の決定を必要とする。既存ファイルや計算結果を推測で変更しないため、別タスクで仕様と characterization test を確定してから扱う。

- resource の remaining overtime cost と time-distributed overtime work
- earned value が参照する baseline の選択
- dependency parse が探索する project scope
- critical path の部分無効化戦略
- grouped calculated values の overlap semantics
- assignment hierarchy の void node と document ownership
- Groovy strategy、Commons Digester、Java serialization などの形式移行

## 次回の安全な進め方

1. 上記の仕様項目ごとに、現行挙動を固定する characterization test を追加する。
2. 保存・import/export を変える場合は旧形式読込と round trip test を先に用意する。
3. Swing の責務分割では EDT、model/view index、selection、再描画後の状態をテストする。
4. 依存バージョンを上げる場合は MPXJ import/export と ribbon/icon の互換 adapter を境界にして段階移行する。
5. 最後に `clean build installDist` と配布 classpath/import smoke test を実行する。

## 完了条件

- focused test と repository-wide build が成功する。
- `installDist` が最新の依存関係から再生成される。
- packaged file import smoke test が成功する。
- `git diff --check` と `git status` で、変更が意図した source、test、依存設定、文書だけであることを確認する。
- UI の手動確認を実施できない場合は、その事実と残存リスクを明記する。
