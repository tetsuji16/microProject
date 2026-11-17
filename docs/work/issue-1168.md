# Issue #1168 — Split Task

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1168
Contract frozen: 2026-11-17, revision 1
Branch: `issue/1168-split-task`
Base: PR #1178 merge `d6ef9b53064eb1414498069c5d601fadc26ccc18`
Draft source: #1203 commits `6a2e01b0f3853ac4925363e4d6b93ae110ad059d` and `3b9330d34a966a73c4daba555d6d7f19ff98a833`, cherry-picked as `f43c98a1a` and `174759b74`.
PR: https://github.com/tetsuji16/microProject/pull/1210 (`Refs #1168`); superseded #1203 closed with transfer note.

## Goal and scope

Implement Split Task across task schedule/model, MPO persistence, MPP/MSPDI interoperability, Gantt interaction/rendering, Undo/Redo, and ribbon wiring. Project Server/Online and unrelated ribbon gaps are out of scope.

## Acceptance

| ID | Requirement | State | Evidence |
|---|---|---|---|
| A1 | Core split intervals and schedule semantics, including edge behavior | Existing coverage; partial semantic characterization | `ScheduleServiceSplitTest` covers split/clear/undo; Microsoft documents interaction but not all calendar edges. |
| A2 | MPO exact persistence and old-file defaults | Existing coverage passes; new imported-origin roundtrip added | `MpoFileImporterTest`, `MpoHistoricalFixtureTest`; current candidate adds imported/leveling/manual provenance check. |
| A3 | MPP/MSPDI split import/export and roundtrip | **Open** | Real Project 2019 MPP/MSPDI Task 3 Stop/Resume and 50% progress roundtrip; it does not prove arbitrary multi-gap/manual split roundtrip. Generated MSPDI multi-gap contour test exists. Microsoft Task schema documents one Stop/Resume pair; MPXJ `getWorkSplits()` summarizes work ranges. Pinned MPXJ has no MPP writer. |
| A4 | Gantt rendering, interactive route, Undo/Redo, persistence | Complete for implemented scope | Hosted Robot Split journey `44024645564` on `598b1982c`; ReadOnly journey `44024179937`. |
| A5 | Ribbon Split Task command enters Gantt split tool | Complete | Same hosted journeys; ribbon and popup share `canStartSplitMode`. |

## Current slice

**Done:** Import task work gaps as IMPORT provenance separately from manual and leveling gaps; retain imported gaps through leveling clear; persist provenance through MPO; retain positive source span for zero-work custom MSPDI assignment contours. Preserve old sidecar defaults.

**Remaining before issue closure:** Complete A3: MSPDI `microProject_Segments` reversible extension and precedence (extension > Stop/Resume > TimephasedData); single-split Stop/Resume and multi-split/zero-work roundtrip; MPP Stop/Resume import coverage; document MPP export loss. Verify segment count/start/finish/work/origin, invariants, dependent/summary propagation, and calendar/resource overlap cases. Do not infer arbitrary manual split support from assignment contours.

## Draft failure record (before fixes)

Draft PR #1203 CI run `44034676132` on clean baseline `c15b635ef` completed 412 tests with two candidate-only failures; both also reproduced locally before edits:

1. `MpoFileImporterTest.assignmentLevelingSplitStateSurvivesMpoSaveReloadAndClear`: expected no manual task gaps after reload; got `[1h..2h, 6h..7h]`.
2. `MpxExportTrackingTest.exportsPositiveSpanZeroWorkPersonalContourAndReloadsIt`: assignment end was not after start.

The baseline passed both. The initial #1203 implementation added imported interval provenance to task snapshots and MPO sidecar, but its follow-up `3b9330d34` was initially missed when constructing #1210. That commit disables inferred task gaps when reading MPO's embedded `content.xml` (the native task-split sidecar remains authoritative) and removes the scalar-work clearing that destroyed custom contour bounds. Both fixes are now integrated as `174759b74`.

PR #1210 GUI run `44087707462` failed twice at `U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction`: reload returned 16h for the physically entered 10h duration. The preceding master GUI run on `d6ef9b530` passed. The failure was traced to the omitted #1203 follow-up above; the new candidate is being verified. Do not count the earlier GUI failure as cleared until the new run finishes.

## Latest local verification

- `:microproject_core:test --console=plain`: PASS (1,409 tests; 2 skipped) before the exchange-only follow-up; core source is unchanged.
- `:microproject_exchange:test --console=plain`: PASS (413 tests) before `174759b74`; focused recheck and a full exchange rerun pass after it, including both original regressions, imported/manual/leveling MPO provenance roundtrip, generated two-gap MSPDI import, and cross-assignment union.
- `build test --console=plain`: PASS on the working tree with `174759b74` (57 tasks; 2m16s).
- Hosted PR CI before the follow-up: cross-platform Build passed; GUI acceptance failed twice at U-26 10h→16h MPO reload. This candidate is pending a fresh Build/GUI run after the follow-up.
- Source docs: Microsoft [Task element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc), [Stop element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/stop-element?view=project-client-2016), [Tasks schema](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016), [TimephasedData](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/timephaseddata-element?view=project-client-2016), [Task.Split](https://learn.microsoft.com/en-us/office/vba/api/project.task.split). Compatibility conclusions about undocumented encodings remain open/document-derived, not measured MSP behavior.

## Integration

Push follow-up commit `174759b74` after focused and full local exchange checks, then monitor replacement PR #1210 CI. Keep `Refs #1168` while A3 remains open. Do not close issue #1168 until A3 acceptance is evidenced.
