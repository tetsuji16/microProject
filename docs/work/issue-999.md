# Issue #999 — 設定・復旧・同期ファイルの実行時識別子を現行名称へ揃える

State: DONE（PR で `.gitignore` の未追記を補い、実装済みを検証して close）
Issue: https://github.com/tetsuji16/microProject/issues/999
Contract frozen at: 2026-11-19 issue body（基準コミット `e8a4a2350` 時点の記載）
Baseline HEAD: `0ac245a4f`（master）
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` の変更と `.workbuddy-ai/` / `.worktrees/` のみ未追跡

## Goal and scope

利用者のディスクとメモリに残る識別子を現行名称（`microproject` / `microProject`）へ揃える。書き込みは現行名称のみ、読み込みは旧名称（`projectlibre` / `ProjectLibre`）も受け付け、データを失わない。

Out of scope: `com.projectlibre1.*` など交換形式のクラス記述子（`PodFormatIdentifiers`）、`VersionUtils` の `projectlibre.test.*` システム プロパティ（テスト専用）、`RecentProjectStoreTest` の一時 Preferences ノード名（テスト専用）。
Blockers: none。

## 検証した実装（master `0ac245a4f`）

issue の表の 7 行は、いずれも master で実装済みだった。行ごとの実装と回帰テストは次のとおり。

| # | 対象 | 実装 | 回帰テスト |
|---|---|---|---|
| 1 | `Main` の設定キー | `RUN_NUMBER_KEY` / `FIRST_RUN_KEY` へ書き込み、`LEGACY_*` は読み込みのみ（`Main.java` 55〜59、168〜222 行） | `MainFirstRunPreferencesTest` |
| 2 | `ConfigurationFile` の設定ディレクトリとファイル名 | 探索順は `.microproject` → `microProject` → `.projectlibre` → `ProjectLibre`。設定ファイル名は `microproject.conf`、旧 `projectlibre.conf` は読み込みフォールバック（44、57〜69 行） | `ConfigurationFileLocationTest` |
| 3 | `AutoRecoveryStore` の復旧ディレクトリ | 現行は `%LOCALAPPDATA%\microProject\recovery`。旧 `ProjectLibre\recovery` に未処理のメタデータがある間だけその場所を維持する（113〜130 行）。`properties.store` のコメントは `microProject AutoRecovery`（521 行） | `AutoRecoveryStoreTest` |
| 4 | ホーム配下の既定ディレクトリ | `microProject` を優先し、無いときだけ `ProjectLibre`（`SwingFileChooserProvider.java` 115〜125 行、`LocalSession.java` 220〜224 行） | `SwingFileChooserProviderTest`、`LocalSessionDescriptorRootsTest` |
| 5 | 同期メタデータのファイル名 | 書き込みは `.microproject-sync.json`。旧 `.projectlibre-sync.json` は現行ファイルが無いときだけ、旧ロックも保持したまま 1 回だけ読み込んで現行名へ書き出す（`CollaborationMetadataStore.java` 111〜117、474〜506 行） | `CollaborationMetadataStoreTest`（旧名称からの移行、両方ある場合、書き込み失敗の fault injection） |
| 6 | 協調セッションのスレッド名 | `microproject-collaboration-close-<instance>`（`CollaborationSession.java` 306 行） | `CollaborationMetadataStoreTest` 経由で close 経路を実行 |
| 7 | IME のクライアント プロパティ名 | `microproject.input.composing`、`microproject.compositionListenerInstalled`（`ImeTextInputSupport.java` 26 行、`SpreadSheetCellEditorAdapter.java` 53 行） | 既存の表計算エディター テスト |

## 残っていた差分（この PR で修正）

`.gitignore` は旧名称の `samples/*.projectlibre-sync.json` と `.lock` だけを無視していた。現行の `.microproject-sync.json` / `.lock` は追記されておらず、この作業ツリーでは `.git/info/exclude`（コミットされないローカル設定）で偶然無視されていた。クローンし直すと `samples/` の同期ファイルが未追跡として現れる。

- `.gitignore` に `/samples/*.microproject-sync.json` と `/samples/*.microproject-sync.lock` を追加（旧名称の 2 行はそのまま残す）。
- `CollaborationSidecarIgnoreTest` を追加。現行の sidecar 名と lock 名を `CollaborationMetadataStore` から取り、コミット済み `.gitignore` が現行名と旧名称の 4 行を持つことを確かめる。リポジトリ ルートはテストの作業ディレクトリから `settings.gradle` と `.gitignore` を持つディレクトリまで遡って求める。

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | 新規環境で現行名称だけが作られる | pass | 書き込み経路は現行名のみ（上表 1〜7）。`CollaborationSidecarIgnoreTest.sidecarAndLockUseTheCurrentName` が `plan.microproject-sync.json` / `.lock` を固定 |
| A2 | 旧名称のデータがある環境で設定が引き継がれる | pass | `ConfigurationFileLocationTest.canonicalSettingsOverrideLegacyWithoutModifyingThem`、`MainFirstRunPreferencesTest` |
| A3 | 旧名称の復旧ファイルが引き継がれる | pass | `AutoRecoveryStoreTest`（旧ディレクトリのスナップショットを列挙し続ける） |
| A4 | 旧名称の同期メタデータが引き継がれる | pass | `CollaborationMetadataStoreTest`（`.projectlibre-sync.json` からの移行） |
| A5 | 両方ある場合は現行が勝ち、旧データを書き換えない | pass | `ConfigurationFileLocationTest`（旧ファイルの内容が不変）、`CollaborationMetadataStoreTest` |
| A6 | 書き込みに失敗する場合の動作を固定 | pass | `CollaborationMetadataStoreTest` の fault injection（`writeTempFile` フック） |
| A7 | コミット済みの ignore 規則が現行名を覆う | pass | `CollaborationSidecarIgnoreTest.repositoryIgnoresTheCurrentAndLegacySidecarNames`。修正前は同テストが `CollaborationSidecarIgnoreTest.java:36` で失敗した |
| A8 | `:microproject_core:test`、`:microproject_application:test`、関連 UI テスト | pass | 下記「Latest evidence」 |

## Latest evidence

- `.\gradlew.bat :microproject_core:cleanTest :microproject_core:test --tests "com.microproject.preference.ConfigurationFileLocationTest" --tests "com.microproject.collaboration.CollaborationMetadataStoreTest" --tests "com.microproject.session.LocalSessionDescriptorRootsTest" --console=plain` → BUILD SUCCESSFUL。`CollaborationMetadataStoreTest` は 19 tests / 0 failures。
- `.\gradlew.bat :microproject_application:cleanTest :microproject_application:test --tests "com.microproject.application.AutoRecoveryStoreTest" --console=plain` → BUILD SUCCESSFUL。27 tests / 0 failures。
- `.\gradlew.bat :microproject_ui:cleanTest :microproject_ui:test --tests "com.microproject.main.MainFirstRunPreferencesTest" --tests "com.microproject.util.SwingFileChooserProviderTest" --console=plain` → BUILD SUCCESSFUL。
- `.\gradlew.bat :microproject_core:cleanTest :microproject_core:test --tests "com.microproject.collaboration.CollaborationSidecarIgnoreTest" --console=plain` → BUILD SUCCESSFUL（2 tests / 0 failures）。`.gitignore` の変更を外すと `repositoryIgnoresTheCurrentAndLegacySidecarNames` が失敗する（red 確認済み）。
- `git check-ignore -v "samples/CCPM sample English.microproject-sync.json"` → 修正前は `.git/info/exclude`（ローカル限定）が唯一の一致。修正後はコミット済み `.gitignore` が一致する。
