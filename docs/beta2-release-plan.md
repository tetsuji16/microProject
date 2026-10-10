# 0.0.24 beta.2 リリース計画

2026-10-10、未解決Issue 34件と現行 master `db483b2e2` を確認して策定。最優先は ProjectLibre / OpenProj 由来の不要な製品依存を減らすこと。「Open Project」は通常のファイルを開く操作と旧サーバー製品機能を区別する。OpenProject (別製品) の導入や置換は行わない。

## 優先順位と出荷範囲

| 優先 | Issue | beta.2 の判断 |
| --- | --- | --- |
| P0 | [#774](https://github.com/tetsuji16/ProjectLibre/issues/774) | 独自 shell 名、POD API、設定、recovery 所有ディレクトリを移行。通常 Open / MPP / MPO / POD は維持。下記の棚卸しを実施 |
| P0 | [#595](https://github.com/tetsuji16/ProjectLibre/issues/595) | 触る責務だけ整理。内部 API の旧版互換ブリッジを削除し、再導入を naming gate で防止 |
| P1 | [#737](https://github.com/tetsuji16/ProjectLibre/issues/737)、[#738](https://github.com/tetsuji16/ProjectLibre/issues/738)、[#740](https://github.com/tetsuji16/ProjectLibre/issues/740) | core metadata の UI 印刷クラス指定を除去、local session を型付き生成へ統合。次段階は Swing/Undo/Project 表示設定の port 分離と application への調整集約。POD の serialized fields を動かす大規模変更は beta.2 に混ぜない |
| P1 | [#729](https://github.com/tetsuji16/ProjectLibre/issues/729)、[#773](https://github.com/tetsuji16/ProjectLibre/issues/773) | 既存統合を再実装しない。追加は設定優先順位と recovery 継続の境界だけ。全 suite を削減したとは扱わない |
| P2 | #770 / #765 / #453 | リボン機能拡張と外観完全互換は独立課題。依存脱却より先にしない |

beta.2 は独立化の最初の出荷単位。コード起源の完全除去、core/UI 分離の完了、サーバー機能全削除を主張しない。

## 実行手順と完了条件

1. caller、reflection/resource、保存形式を追跡して候補を分類する。
2. `MicroProjectShell` に単一の shell を改名。全 production / unit / GUI caller を移行し、旧クラスを残さない。`isProjectLibreFile` 内部 API を削除、POD の数値 discriminator は値 `1` を保って `POD_FILE_TYPE` に改名。
3. 設定は `.microproject/microproject.conf` / `microProject/microproject.conf` を優先。旧配置は読取のみの fallback。空の新ディレクトリが旧 `run.conf` を隠さない。
4. 新規 recovery は `%LOCALAPPDATA%/microProject/recovery` (その他 OS: `~/.microproject/recovery`) を使用。旧 recovery metadata が残る間は旧 store 全体を使い、消去済みの次回起動から新配置へ移行。無断移動・上書きはしない。
5. 全 module tests、architecture/naming、再生成 installDist、限定 runtime の MPP/POD 読込を実行。Windows release は clean build、GUI smoke、MSI/ZIP、配布EXE実起動、hash の各 gate がすべて成功してから公開する。
6. session は `LocalSession` を直接構築し、core の `SessionImpls` / `Session.local` メタ登録を削除。model scope flag の true/false は同じ local backend に解決する現行契約を保持。初回アクセスを同期して別々の ID allocator 生成を防止。印刷実装は UI が `MicroProjectPrintService` を直接構築し、core にある UI クラス名・reflection を削除。
7. `v0.0.24-beta.2` / jpackage `0.0.24.2` / channel `beta`。stable latest と無人update feed は更新しない。出荷済み beta.1 asset は変更しない。

## #774 候補の調査結果

| 候補 / caller | 分類・根拠 | beta.2 |
| --- | --- | --- |
| `ProjectLibreShell` → `GraphicManager`, `DefaultFrameManager` | fork の Swing shell。serializable ではなく Java caller のみ。旧製品名を必要とするデータ契約なし | `MicroProjectShell` へ改名、全 caller / test 移行 |
| `FileHelper.isProjectLibreFile`, `ProjectFilePolicies.isProjectLibreFile` | すでに `isPodFile` に委譲する内部互換API。production caller なし。POD wire の class/field ではない | 削除。POD判定は単一経路 |
| `PROJECTLIBRE_FILE_TYPE` | POD の数値識別子。`LocalSession` / `FileHelper` の switch caller | 値を保った `POD_FILE_TYPE` に改名 |
| `ConfigurationFile` | 旧配置だけ探索する active 設定読込。user config は外部入力 | 新配置優先、旧配置読取fallback |
| `AutoRecoveryStore` | application が所有する runtime ファイル。beta.1 crash snapshot の発見が必要 | 新規保存の配置独立化、未解決旧snapshot は保持 |
| `CollaborationMetadataStore` suffix | active lease / process lock path。MPO artifact lifecycle が参照。名前だけ変えると二重lockになる | 保持。lock移行protocolと同時に別出荷単位で変更 |
| `ServerFileImporter`, `ServerLocalFileImporter` | `MicrosoftImporter` も前者を継承。後者は provider 登録と POD 非local load policy の caller あり | 未使用とは判定できず維持。型名だけで削除しない |
| `OpenProjectDialog`, `LoginDialog` | `GraphicManager` の非local Open/Insert と `StartupFactory` の非standalone login が参照。meta session 登録は local のみだが条件分岐が残る | caller と動的構成の整理が必要。通常Openとは別の次段階対象 |
| `SessionFactory` / `SessionImpls` | active meta 登録は LocalSession のみ。server scope は既に同じ local へfallback。session registry はserializedではない | 型付き生成へ統合、resetとqueue継承を保持、共有sessionの競合を同期 |
| `ExtendedPrintServiceFactory` / core meta | UIだけが使う印刷実装のクラス名をcore metadataが指定。POD descriptorではない | UIへ構築責務を移しreflectionとcore側指定を削除 |
| `SafeObjectInput`, provider class-name aliases | 既存 POD descriptor / saved options の互換境界 | 保持。POD wire不変、MPO旧版読込維持 |
| copyright / provenance / repository URL | 起源・ライセンス根拠。実行時依存ではない | 保持。改名で法的整理済みとは扱わない |

## 検証記録

JDK 25 / `releaseVersion=0.0.24.2` / `releaseChannel=beta`。

- 全8 module unit/headless: 2,221件、失敗0、error0、Windows専用1件skip。
- `bash ./gradlew build installDist verifyArchitectureBoundaries verifyPackagedFileImports -PreleaseVersion=0.0.24.2 -PreleaseChannel=beta --console=plain`: 初回成功。MPP/POD 各145 tasks。
- recovery / naming gate の追加後、UI test の旧 Gradle binary result 読込が `Test.getPreviousFailedTestClasses` で EOF/Buffer underflow。テスト実行前の古い結果ファイル破損として切り分け、生成済み UI test-results/reports を消去して再検証。
- `bash ./gradlew :microproject_ui:test :microproject_application:test verifyNamingConventions installDist verifyPackagedFileImports -PreleaseVersion=0.0.24.2 -PreleaseChannel=beta --console=plain --no-daemon`: 成功。最終 UI 1,049件 / application 35件、失敗0。MPP/POD 各145 tasks。
- `python -m unittest discover -s scripts/tests -v`: 8件成功。
- `git diff --check`: 成功。
- Windows GUI / MSI / ZIP / launcher: release workflow の結果を後記。ローカル Linux に window manager がないため物理 GUI は未実行。


## GitHub 反映状況

2026-10-10、ユーザーから commit / push / PR / merge の許可を取得。レビュー用ブランチ `beta2/product-independence` から検証後に統合する。#774 / #595 / #737 / #740 の親Issueは部分実施として維持する。


## 追加検討と検証（2026-10-10）

独立化をAPI名の整理だけで終わらせず、旧 session のクラス名登録と core metadata からUI印刷クラスを構築する責務を取り除いた。SessionFactory の同時初回取得、queue 差替え、clear後の再生成を3つの境界テストで確認する。印刷は既存の比率計算テストを直接生成ではなく factory 経由にして構築経路も検証する。POD/MPO のフィールド・descriptor・schema は変更していない。

追加後の `clean build installDist verifyArchitectureBoundaries verifyPackagedFileImports` (`releaseVersion=0.0.24.2`, `releaseChannel=beta`) は成功。全8 module 2,224件、失敗0・error0、Windows専用1件skip。MPP/POD 各145 tasks。旧名テストclassの残存によるincremental test失敗をcleanで除去し、古い結果を出荷根拠に使わない。

初期commit `4a3d487a06d9f4ebcc01451eefb281b70ed84374` は [PR #775](https://github.com/tetsuji16/ProjectLibre/pull/775) に反映済み。Windows GUI smoke は [run 38017108250](https://github.com/tetsuji16/ProjectLibre/actions/runs/38017108250) が成功。追加commitは同PRで再検証してから統合する。
