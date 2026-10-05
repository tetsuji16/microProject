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
| microproject_application | 16 | 0 | 0 | 0 | 0 | 0 | 16 |
| microproject_bootstrap | 7 | 0 | 0 | 0 | 0 | 0 | 7 |
| microproject_contrib | 20 | 13 | 2 | 0 | 0 | 0 | 5 |
| microproject_core | 924 | 154 | 0 | 0 | 0 | 0 | 770 |
| microproject_exchange | 119 | 9 | 0 | 0 | 0 | 0 | 110 |
| microproject_reports | 15 | 0 | 9 | 0 | 0 | 0 | 6 |
| microproject_ribbon | 11 | 0 | 0 | 0 | 0 | 0 | 11 |
| microproject_ui | 1204 | 153 | 40 | 0 | 0 | 0 | 1011 |
| packaging | 30 | 0 | 4 | 0 | 0 | 0 | 26 |

## Required human follow-up

1. Supply and verify the official OpenProj 1.4 archive checksum.
2. Review every `REVIEW` row against OpenProj 1.4, ProjectLibre initial history, the 1.9.8 baseline, and current HEAD.
3. Split mixed files at hunk level before assigning `DELETE_PROJECTLIBRE_DELTA` or `REIMPLEMENT_PROJECTLIBRE_DELTA`.
4. Record reviewer, evidence, and the selected disposition in the CSV; do not remove CPAL notices before this review is complete.
