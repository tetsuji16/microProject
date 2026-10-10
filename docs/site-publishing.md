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

GitHub Pages serves `pages-redirect/` only. When adding a public HTML page, add
its matching redirect stub, preserving query and fragment. A standalone Pages
workflow deploys documentation changes without running Windows packaging.

The rewritten guide is based on GraphicManager shortcut registration, the CCPM
settings/status implementation and the beta smoke acceptance scope. It does not
claim every legacy feature or complete MSP compatibility.
