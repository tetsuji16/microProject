# Issue #1048 — MpoFileImporter を責務ごとのクラスに分割する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1048
Contract frozen at: 2026-11-11, issue body plus the 2026-11-10 progress comment
Baseline HEAD: `c1ecd75eb3ccecf31fbc1f7700b21c16c3a88dd9` (#1097, shared records and operation state moved)
Baseline working tree: clean

## Goal and scope

Move `MpoFileImporter` behaviour into one class per responsibility without changing
behaviour: `MpoSidecarJson`, `MpoEmbeddedProjects`, `MpoManifest`, `MpoExternalMerge`.

Out of scope: MPO format changes, other large classes (#738, #743).
Blockers: none. One class per pull request, in the order of the issue table.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Four classes extracted; `MpoFileImporter.java` at or below 1,500 lines | open | 2,240 lines after the first slice |
| A2 | Each pull request is a move only, with no behaviour change | pass (slice 1) | Strict text reconstruction of the removed block and the new class differs by 0 lines |
| A3 | Saved MPO content is identical before and after the split (round-trip tests) | pass (slice 1) | `:microproject_exchange:test`, 405 tests, 0 failures |
| A4 | The earlier-version MPO reading tests pass | pass (slice 1) | Same `:microproject_exchange:test` run |
| A5 | Architecture and naming gates pass | pass (slice 1) | `verifyArchitectureBoundaries verifyNamingConventions` |

## Current slice

**Slice 1 (done):** `MpoSidecarJson` now owns layout, visibility, task splits, leveling
options, text styles, native task schedules, baselines and their record validation.
Its methods are package-private; `MpoFileImporter` calls them by class name.

**Next slice:** extract `MpoEmbeddedProjects` (embedded and linked project extraction and
write-back) in its own pull request.
**Done when:** the moved methods live in `MpoEmbeddedProjects` with unchanged bodies and
`:microproject_exchange:test` reports the same results.
**Then:** `MpoManifest`, then `MpoExternalMerge`.

## Remaining blockers

- A1: three classes still to extract; the line budget cannot be reached in one slice.

## Latest evidence

- Tests/build: `:microproject_exchange:test verifyArchitectureBoundaries verifyNamingConventions`
  — BUILD SUCCESSFUL, 405 exchange tests, 0 failures, on the slice-1 working tree from
  baseline `c1ecd75eb`.
- Physical GUI/manual: not applicable; no Swing or user-facing route changed.
- CI: not run; the change has not been pushed. `build` (3 OS) and `GUI acceptance`
  remain to be observed on the pull request head.
