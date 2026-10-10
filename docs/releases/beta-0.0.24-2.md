# microProject 0.0.24 beta.2

ProjectLibre / OpenProj からの独立化を優先した、Windows 用ベータ版です。

## 変更

- 画面起動の shell と POD 判定 API を microProject / 形式名に統一。旧製品名の内部ブリッジを削除し、build gate で再導入を防止。
- 設定ファイルは `~/.microproject/microproject.conf` を優先。旧設定を読み取る fallback を維持し、空の新設定ディレクトリが旧 runtime 設定を隠す問題を防止。
- 新規 recovery を `%LOCALAPPDATA%/microProject/recovery` に保存。旧版の未解決 recovery は同じ配置で回復でき、正常終了による削除後の次回起動から新配置を使用。
- ローカルセッションを直接構築し、旧サーバー向けのクラス名による生成・設定登録を削除。初回アクセスを同期して別々のID採番器の生成を防止。
- 印刷サービスをUI側で直接構築し、coreにあるUIクラス名への依存を除去。
- 通常のプロジェクト Open、MPP/POD 読込、MPO の旧版読込と保存、タスク表・ガント・CCPM の beta.1 操作範囲を維持。

旧サーバーコードと core/Swing の分離は段階的に進めます。本版は全旧コードの除去完了を意味しません。POD のシリアライズ形式、既存 collaboration lock のパスとライセンス帰属情報は維持します。

## 配布

Windows の全出荷 gate に合格してから MSI、portable ZIP、SHA256SUMS を公開します。stable latest と無人自動更新feedは変更しません。

計画と棚卸しは [beta.2 計画](../beta2-release-plan.md) を参照してください。
