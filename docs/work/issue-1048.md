# Issue #1048 — MpoFileImporter を責務ごとのクラスに分割する

State: COMPLETE (implementation); pull request and CI still to be observed
Issue: https://github.com/tetsuji16/microProject/issues/1048
Contract frozen at: 2026-11-11, issue body plus the 2026-11-10 progress comment
Baseline HEAD: `1ca4fba33` (slice 3, MpoManifest)
Baseline working tree: clean

## Goal and scope

Move `MpoFileImporter` behaviour into one class per responsibility without changing
behaviour: `MpoSidecarJson`, `MpoEmbeddedProjects`, `MpoManifest`, `MpoExternalMerge`.

Out of scope: MPO format changes, other large classes (#738, #743).
Blockers: none. One class per pull request, in the order of the issue table.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Four classes extracted; `MpoFileImporter.java` at or below 1,500 lines | pass | Four classes extracted; importer is 1,441 lines (from 3,404) |
| A2 | Each pull request is a move only, with no behaviour change | pass (slices 1-4) | Strict reconstruction from the removed spans plus the delegation/visibility edits differs by 0 lines in every slice |
| A3 | Saved MPO content is identical before and after the split (round-trip tests) | pass (slices 1-4) | `:microproject_exchange:test`, 405 tests, 0 failures |
| A4 | The earlier-version MPO reading tests pass | pass (slices 1-4) | Same `:microproject_exchange:test` run |
| A5 | Architecture and naming gates pass | pass (slices 1-4) | `verifyArchitectureBoundaries verifyNamingConventions` |
| A6 | Pull request opened and its `build` (3 OS) and `GUI acceptance` checks observed | pending (external) | Not pushed by this work; the branch carries the four slice commits |

## Completed slices

- **Slice 1:** `MpoSidecarJson` — layout, visibility, task splits, leveling options, text
  styles, native task schedules, baselines and their record validation.
- **Slice 2:** `MpoEmbeddedProjects` — linked-source discovery, embedded entry reading and
  payload validation, extraction into the workspace, cross-project reference rewriting
  inside child archives, and failure lookup.
- **Slice 3:** `MpoManifest` — `manifestFor` (all overloads), format-version gate,
  manifest validation/reading and the draft JSON reader, archive checksums, the meta and
  settings documents, and the legacy metadata migrations.
- **Slice 4:** `MpoExternalMerge` — external archive reading (`externalOperations`),
  snapshots and merge plans (`snapshotOf`, `planFor`, `validateMergePlan`), and the
  operation-state consumers (`writeCapture`, `snapshotForWrite`,
  `workingOperationStateFor`, `commitOperationState`).

### Slice 4 boundary note

`prepareExternalMerge` and `persistRecoveryCopy` stay in `MpoFileImporter` on purpose.
Both are instance methods bound to importer state (`replaceDifferentDocument` and the
`protected createTemporaryFile` seam that `MpoTransactionBoundaryTest` overrides); moving
them into a static utility class would require signature changes, which is not a move.
They keep their orchestration role and delegate the extracted helpers to
`MpoExternalMerge`. The 1,441-line result still meets A1.

All four classes are package-private `final` classes with package-private static
methods. Only shared primitives stay in `MpoFileImporter`, widened to package-private
instead of copied: entry read/write (`readEntry`, `writeEntry`, `writeMimetypeEntry`),
`sha256`, `readBoundedOperationLog`, `ensureOperationCount`, `validateCcpmHistory`,
`validateExtensionName`, `remapTaskOperations`, `captureModel`, `copyOfCapture`,
`secureDocumentBuilderFactory`, the XML/JSON scalar readers, `extractionWorkspaceRoot`,
and the entry-name/format constants.

## Current slice

**Next slice:** none for this issue. A6 needs a pull request: push the branch, open the
pull request with `Closes #1048`, and observe `build` and `GUI acceptance` on its head.

## Remaining blockers

- A6 only: external CI gates, which require the pull request that this work was asked
  not to open.

## Latest evidence

- Tests/build on the slice-4 tree: `:microproject_exchange:compileJava`,
  `:microproject_exchange:test`, `verifyArchitectureBoundaries`,
  `verifyNamingConventions` — BUILD SUCCESSFUL, 405 exchange tests, 0 failures, under the
  existing `-Xlint:all` + `-Werror` settings.
- Move-only proof: reconstructing `MpoFileImporter` from the pre-move source minus the
  moved spans (`709-736`, `743-751`, `755-765`, `767-772`, `940-987`, `989-1000`,
  `1002-1006`, `1008-1029`) plus the delegation and visibility edits differs by 0 lines;
  the same reconstruction of `MpoExternalMerge` differs by 0 lines.
- Earlier slices were not re-verified; their commits (`db2d0c8c5`, `ccf2dcb6d`,
  `1ca4fba33`) keep their recorded evidence.
- Physical GUI/manual: not applicable; no Swing or user-facing route changed.
