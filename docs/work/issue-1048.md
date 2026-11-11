# Issue #1048 — MpoFileImporter を責務ごとのクラスに分割する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1048
Contract frozen at: 2026-11-11, issue body plus the 2026-11-10 progress comment
Baseline HEAD: `db2d0c8c5` (slice 1, MpoSidecarJson)
Baseline working tree: clean

## Goal and scope

Move `MpoFileImporter` behaviour into one class per responsibility without changing
behaviour: `MpoSidecarJson`, `MpoEmbeddedProjects`, `MpoManifest`, `MpoExternalMerge`.

Out of scope: MPO format changes, other large classes (#738, #743).
Blockers: none. One class per pull request, in the order of the issue table.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Four classes extracted; `MpoFileImporter.java` at or below 1,500 lines | open | 1,919 lines after two slices |
| A2 | Each pull request is a move only, with no behaviour change | pass (slices 1-2) | Strict reconstruction from the removed spans plus the delegation edits differs by 0 lines |
| A3 | Saved MPO content is identical before and after the split (round-trip tests) | pass (slices 1-2) | `:microproject_exchange:test`, 405 tests, 0 failures |
| A4 | The earlier-version MPO reading tests pass | pass (slices 1-2) | Same `:microproject_exchange:test` run |
| A5 | Architecture and naming gates pass | pass (slices 1-2) | `verifyArchitectureBoundaries verifyNamingConventions` |

## Completed slices

- **Slice 1:** `MpoSidecarJson` owns layout, visibility, task splits, leveling options,
  text styles, native task schedules, baselines and their record validation.
- **Slice 2:** `MpoEmbeddedProjects` owns the embedded and linked project entries:
  linked-source discovery, entry reading and payload validation, extraction into the
  workspace, cross-project reference rewriting inside child archives, and failure lookup.
  Its methods are package-private; `MpoFileImporter` calls them by class name and keeps
  only the shared helpers (`readEntry`, `writeEntry`, `sha256`, `readManifest`,
  `validateExtensionName`, the archive validators, the entry-name constants and the
  extraction workspace root), widened to package-private.

## Current slice

**Next slice:** extract `MpoManifest` (manifest writing, metadata, settings and version
migration) in its own pull request.
**Done when:** the manifest methods live in `MpoManifest` with unchanged bodies, the
importer delegates to them, and `:microproject_exchange:test` reports the same results.
**Then:** `MpoExternalMerge`, then the line budget in A1.

## Remaining blockers

- A1: two classes still to extract; the line budget cannot be reached in one slice.

## Latest evidence

- Tests/build: `:microproject_exchange:test verifyArchitectureBoundaries verifyNamingConventions`
  — BUILD SUCCESSFUL, 405 exchange tests, 0 failures.
- Move-only proof: reconstructing `MpoFileImporter` from the pre-move source minus the
  moved spans plus the delegation and visibility edits differs by 0 lines; the same
  reconstruction of `MpoEmbeddedProjects` differs by 0 lines.
- Physical GUI/manual: not applicable; no Swing or user-facing route changed.
- CI: not run; the change has not been pushed. `build` (3 OS) and `GUI acceptance`
  remain to be observed on the pull request head.
