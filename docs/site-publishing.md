# Documentation publishing

Canonical site: https://o-server.main.jp/micro-project/

`website/` contains the public HTML source; `docs/images/` contains the shared images.
`docs/*.html` are redirects too, preventing the legacy branch-based Pages
publisher from restoring old content. The `tetsuji16/o-server` workflow
`micro-project.yml` checks out this repository's master, stages public HTML and
images only, and deploys them with the existing WebDAV secrets. It runs on push,
manual dispatch, and hourly so documentation changes propagate without cross-repo
credentials. Release binaries remain immutable GitHub Release assets linked from
the canonical download page.

The canonical download page always links to the latest Windows stable release.
Linux x64 and macOS Apple Silicon packages are built only when a substantial
update is manually dispatched with `build_unix_packages=true`. The page looks
up the newest stable release containing each platform asset independently and
shows that release tag beside its link; a Windows-only stable release therefore
does not replace the most recent Linux or macOS download. Prereleases are not
used for these stable download links.

GitHub Pages serves `pages-redirect/` only. When adding a public HTML page, add
its matching redirect stub, preserving query and fragment. A standalone Pages
workflow deploys documentation changes without running Windows packaging.

The rewritten guide is based on GraphicManager shortcut registration, the CCPM
settings/status implementation and the beta smoke acceptance scope. It does not
claim every legacy feature or complete MSP compatibility.
