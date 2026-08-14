# License provenance audit (Phase 0/1)

- Current ref: `HEAD`
- OpenProj comparison commit: `d2fa3c20a`
- ProjectLibre baseline: `0530be227f4a10c5545cce8d3db20ac5a4d76a66`
- Official OpenProj 1.4 archive: `docs/legal/openproj-1.4-src.tar.gz` (SHA-256: `20071b090d841388860049ce49724e2773b8cec250d76e74264c71adf2a79ac6`)
- Archive source URL: https://sourceforge.net/projects/openproj/files/OpenProj%20Binaries/1.4/openproj-1.4-src.tar.gz/download
- The CSV is a conservative ledger. `REVIEW` is not a finding that the file is ProjectLibre-derived.
- `KEEP_OPENPROJ` means only that the normalized content matched the repository's OpenProj baseline; it is not a legal conclusion.

## Disposition counts

| Module | Total | KEEP_OPENPROJ | KEEP_THIRD_PARTY | KEEP_FORK_ORIGINAL | DELETE_PROJECTLIBRE_DELTA | REIMPLEMENT_PROJECTLIBRE_DELTA | REVIEW |
|---|---:|---:|---:|---:|---:|---:|---:|
| packaging | 26 | 0 | 4 | 0 | 0 | 0 | 22 |
| projectlibre_application | 11 | 0 | 0 | 0 | 0 | 0 | 11 |
| projectlibre_contrib | 32 | 15 | 5 | 0 | 0 | 0 | 12 |
| projectlibre_core | 802 | 281 | 0 | 0 | 0 | 0 | 521 |
| projectlibre_exchange | 111 | 16 | 0 | 0 | 0 | 0 | 95 |
| projectlibre_reports | 13 | 0 | 1 | 0 | 0 | 0 | 12 |
| projectlibre_ui | 922 | 193 | 38 | 0 | 0 | 0 | 691 |

## Required human follow-up

1. Supply and verify the official OpenProj 1.4 archive checksum.
2. Review every `REVIEW` row against OpenProj 1.4, ProjectLibre initial history, the 1.9.8 baseline, and current HEAD.
3. Split mixed files at hunk level before assigning `DELETE_PROJECTLIBRE_DELTA` or `REIMPLEMENT_PROJECTLIBRE_DELTA`.
4. Record reviewer, evidence, and the selected disposition in the CSV; do not remove CPAL notices before this review is complete.
