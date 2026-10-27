# microProject はじめてのプロジェクト計画

日本語版EPUB 3と、その編集用Markdown原稿です。構成・見出し・学習目標・コラム・クイズ・練習問題・参考文献の形式は `o-server/mojo/learn/FORMAT.md` のMojo教科書に合わせています。

## 収録物

- `content/ja/book.json` — 書名、部、章の順序
- `content/ja/ch*.md` — 12章の原稿
- `epub/ja/front.json`、`epub/epub.css`、`epub/cover.jpg` — 前付け、組版、表紙
- `dist/microproject-user-guide-ja.epub` — 生成したEPUB
- `capture-plan.md` — 撮影環境・個人情報確認記録と追加撮影候補
- `issue-candidates.md` — 執筆時に見つけた機能ギャップ候補

## 再生成

Mojo教科書の既存EPUB生成器を利用します。microProjectリポジトリのルートで、同じPython環境と `o-server` チェックアウトを用意して次を実行してください。

```powershell
python ..\o-server\mojo\learn\build_epub.py `
  --lang ja `
  --content-dir docs/book/content/ja `
  --outputs-dir docs/book/outputs/ja `
  --figures-dir docs/book/figures `
  --assets-dir ..\o-server\mojo\learn\assets `
  --epub-dir docs/book/epub `
  --front docs/book/epub/ja/front.json `
  --cover docs/book/epub/cover.jpg `
  --private-words docs/book/.private-words `
  --cache-dir docs/book/cache `
  --out docs/book/dist/microproject-user-guide-ja.epub `
  --check
python docs/book/remove_template_trademark.py
```

The build uses the o-server Mojo textbook parser and EPUB 3 generator without modifying that repository. Its Japanese copyright-page template adds a Mojo trademark notice regardless of the book's subject; the post-processing script removes that unrelated notice and verifies that no Mojo reference remains in the EPUB. Run it after each build. The warning `実行日を outputs から取得できませんでした` is expected: this book has no code-run outputs, and its copyright page (`epub/ja/front.json`) does not use the run-date placeholder, so nothing is left blank. `cache/` contains only generated temporary files and may be removed after building.

## 画面写真

EPUBには公開版 v0.1.0.1271をWSL2のUbuntu仮想環境で起動し、合成データを使って撮影した画面写真を収録しています。画像の確認記録と追加撮影候補は `capture-plan.md` を参照してください。
