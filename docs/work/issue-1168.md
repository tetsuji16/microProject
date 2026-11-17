# Issue #1168 — Split Task

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1168
Contract frozen: 2026-11-17, revision 1
Branch: `issue/1168-split-task`
Base: PR #1178 merge `d6ef9b53064eb1414498069c5d601fadc26ccc18`
Draft source: #1203 candidate `6a2e01b0f3853ac4925363e4d6b93ae110ad059d`, cherry-picked as `f43c98a1afdc103aba055d969eb2c772525cde72`.

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

The baseline passed both. Candidate fixes add imported interval provenance to task snapshots and MPO sidecar, restore it on MSPDI import, and retain custom contour bounds after clearing scalar work. The focused reproducers and new MPO imported-origin roundtrip test pass after fixes.

## Latest local verification

- `:microproject_core:test --console=plain`: PASS (1,409 tests; 2 skipped) after narrowing zero-work bounds handling.
- `:microproject_exchange:test --console=plain`: PASS (413 tests) on the latest working tree.
- Focused pre-fix tests reproduced both draft #1203 regressions. After fixes, the zero-work contour, leveling provenance, MPO imported-origin roundtrip, old-sidecar defaults, and generated two-gap MSPDI regressions pass.
- Source docs: Microsoft [Task element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/task-element?view=project-client-odc), [Stop element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/stop-element?view=project-client-2016), [Tasks schema](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-tasks-element?view=project-client-2016), [TimephasedData](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/timephaseddata-element?view=project-client-2016), [Task.Split](https://learn.microsoft.com/en-us/office/vba/api/project.task.split). Compatibility conclusions about undocumented encodings remain open/document-derived, not measured MSP behavior.

## Integration

Create replacement PR #1168 with `Refs #1168` while A3 remains open. Mention the two pre-fix failures and fixes, generated MSPDI coverage limits, and draft #1203 disposition. Close #1203 after replacement PR is created, linking it and stating the failures were transferred. Do not close issue #1168 until A3 acceptance is evidenced.
