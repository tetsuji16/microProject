# Phase 1: ProjectLibre-delta scope

The unit of replacement is a ProjectLibre change relative to OpenProj, not a current file and not a current license header. A file can contain OpenProj origin, ProjectLibre additions, and fork changes at the same time.

The planned independent product name is **microProject**. Do not rename packages, file-format identifiers, or provenance labels solely for branding during this audit.

| Area | Phase 1 boundary | Default action | Evidence required |
|---|---|---|---|
| `projectlibre_core` | calendar, task/resource, cost, baseline, persistence hunks introduced after the OpenProj origin | `REVIEW` until hunk comparison | OpenProj 1.4, initial ProjectLibre commit, current diff |
| `projectlibre_exchange` | ProjectLibre-specific server/collaboration and format extensions | delete unused; independently reimplement required behavior | call graph, format fixtures, hunk comparison |
| `projectlibre_ui` | ProjectLibre branding, UI additions, menu/resource changes, translations and assets | remove brand material; review code hunk-by-hunk | asset provenance, screenshots, OpenProj UI comparison |
| `projectlibre_reports` | ProjectLibre-specific report adapters/templates | delete unused; reimplement required templates | template provenance and output regression |
| `projectlibre_contrib` | ProjectLibre additions versus OpenProj-bundled and third-party sources | retain upstream; replace only ProjectLibre additions | upstream source/license and dependency metadata |
| `projectlibre_application` | no OpenProj module counterpart | review as fork/ProjectLibre-origin candidate; do not assume either | introduction commit and author evidence |
| `packaging`, docs, samples | ProjectLibre-specific brand, attribution, URLs and copied fixtures | retain required OpenProj notices; remove only confirmed ProjectLibre material | asset checksum, author/license, distribution inventory |

## Disposition rules

- `KEEP_OPENPROJ`: normalized content matches the OpenProj repository baseline. This is a technical match, not a legal opinion.
- `KEEP_THIRD_PARTY`: an upstream notice or dependency identifies another licensor; keep its notice and license.
- `KEEP_FORK_ORIGINAL`: use only after a reviewer documents that the work was created without ProjectLibre expression.
- `DELETE_PROJECTLIBRE_DELTA`: remove an unused ProjectLibre-only feature or asset.
- `REIMPLEMENT_PROJECTLIBRE_DELTA`: required behavior is specified by a public format, API contract, or black-box test and is implemented without copying ProjectLibre expression.
- `REVIEW`: origin or mixed hunk remains unresolved. Do not remove headers or attribution while it is `REVIEW`.

## Current generated inventory

The current conservative run is in `license-provenance.csv` and its counts are in `license-provenance-summary.md`. As of the generated run:

- 505 rows match the repository's normalized OpenProj baseline;
- 48 rows contain a detectable third-party notice;
- 1,364 rows remain `REVIEW`;
- zero rows are classified as ProjectLibre delta until a human hunk review supplies evidence.

The 1,364 `REVIEW` rows are not a claim that 1,364 files must be rewritten.
