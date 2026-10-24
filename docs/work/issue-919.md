# Issue #919 — MPO backward/native contracts

State: ACTIVE — historical/native slice and headless transaction/application
checks pass; physical GUI acceptance remains with Sol's integration gate.
Issue: https://github.com/tetsuji16/microProject/issues/919
Contract frozen: 2026-10-25, issue body A1–A4 and finite intake matrix.
Baseline HEAD: `b0bf097825c4f59c101805fcb2b99991ca30eccd` (tree tested as
`dd49d8b52a0c07ce4c7668087e98e88ba15c156b`). Preserve untracked attachments and
four sample sync JSON/lock files; do not touch root-owned issue-796/918 packets.

## Goal and scope

Preserve earlier supported MPO reads and verify native/default write plus
transaction preservation. MPP writing, POD wire changes, dependencies, cloud
services, and new UI routes are excluded. Two reproduced calendar defects were
fixed: source default calendar identity was not installed before task import,
and new baseline assignments used the global default instead of the effective
assignment calendar.

## Frozen acceptance

| ID | Required result | State | Evidence / remaining |
|---|---|---|---|
| A1 | Separate MPOF container from app/payload generations; previous-writer source/hash; bounded version contract | pass (representative contract) | MPOF 1.0 vs app 0.0.23/0.0.24 and actual v0.0.24.1265 source/hash documented; finite tested cohorts only, no per-patch/build matrix. Earlier supported reads remain in scope. |
| A2 | Literal semantic manifest through current read, save, reopen | pass for bounded fixtures | 0.23 selected fields; .24 task/resource/assignment, dates/calendar/duration/constraints/progress/link and populated native entries; .24 baseline-cost probe retained separately |
| A3 | Native/default, external Save As, failure/cancel/overwrite refusal preserve file/model | partial | Headless transaction/application tests pass; physical Robot route evidence remains Sol integration gate |
| A4 | Exact SHA, commands/counts and evidence layers recorded; no claim beyond evidence | partial | Source/archive SHA and test counts recorded; staged-blob hash and physical GUI remain pending |

## Implemented evidence

- `v024-1265-previous-writer.mpo` was generated from isolated `git archive`
  commit `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af` using JDK
  `25.0.4.101-hotspot`, UTC and English locale. Generator source asserts its
  source model before the historical writer call and uses no current code.
- Fixture archive SHA-256:
  `0CBB233D8836B472F80FBC8932CAE99CC9858670A61AC1F4DC70E31B7C1DFDD8`;
  content SHA-256:
  `DDB6A49BD2F7D5CA2E6432FE29B5711D08E8DA406EDB43664E9E4C29C1E34A7A`.
- Non-empty native literals: sheet layout fields/width/manual flags; hidden UID
  4201; UID 4202 manual split 14,400,000–18,000,000 ms; both leveling options
  false; Dialog 15pt `0x2457A6` bold/italic/strikethrough; project/task calendar
  definitions; baseline slot 0, resource UID 7, profile duration 28,800,000 ms.
  Current import and current MPO reopen assert the corresponding model state.
- Assignment-leveling-splits v2 entry exists but has zero assignments; it is
  not evidence for non-empty v2 contour provenance. Current synthetic v1
  downgrade test covers the marker migration separately.
- `missingTaskScheduleEntryAndCalendarFieldsFallBackToMspdi` checks both a
  missing schedule entry and a v1-shaped schedule payload with calendar
  definitions/references removed while preserving schedule values. Both
  assert literal date/calendar expectations and are explicitly synthetic.
- Existing split-without-provenance test now removes `manualIntervals`,
  `levelingIntervals`, and `manualLevelingDates` together; it asserts the gap
  remains manual and no leveling-date provenance is invented.
- Source calendar default resolves by imported object identity before name or
  process-global fallback when its UID is unique in the source project. The
  altered-default fixture test keeps source UID 42 effective and passes with
  global weeks made non-working. This document-derived rule follows Microsoft's
  [CalendarUID definition](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/calendaruid-element?view=project-client-2016)
  and [UID scope](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/uid-element?view=project-client-2016).
  For ambiguous duplicate UIDs (UID 2 `標準[Imported]`, `未割当`, `tetsu` in
  the .23 sample), preserve the prior Standard/base fallback as an old-MPO
  compatibility choice, not Microsoft behavior. Its selected 34h work assertion
  remains; the fixture does not claim schedule-date parity.
- A new baseline assignment privately snapshots its current effective
  task/resource calendar; a regression verifies later calendar edits do not
  mutate that snapshot.
- Generation, schema transitions, tested cohorts, and unpopulated fields are
  detailed in `testdata/mpo/README.md`, `docs/mpof/MPOF.md`, and the MPO rows of
  `docs/msp-exchange-format-inventory.md`.

## Current slice / next action

Verification on the current worktree: `:microproject_core:test` passed 1,183
tests, 0 failures/errors/skips; focused exchange transaction/baseline-cost and
historical/split set passed 13 tests, 0 failures/errors/skips; full
`:microproject_exchange:test` passed 347 tests, 0 failures/errors/skips;
`:microproject_application:test --tests com.microproject.application.ProjectDocumentWorkflowTest --tests com.microproject.application.ProjectFilePoliciesTest`
passed 7 tests, 0 failures/errors/skips. The historical class was also run
alone with `--rerun-tasks` (5 passed). Physical Robot evidence is still pending.

Exact commands:

```powershell
.\gradlew.bat :microproject_core:test --console=plain
.\gradlew.bat :microproject_exchange:test --console=plain
.\gradlew.bat :microproject_application:test --tests com.microproject.application.ProjectDocumentWorkflowTest --tests com.microproject.application.ProjectFilePoliciesTest --console=plain
.\gradlew.bat :microproject_exchange:test --tests com.microproject.exchange.MpoHistoricalFixtureTest --tests com.microproject.exchange.MpoTransactionBoundaryTest --tests com.microproject.exchange.MpoFileImporterTest.mpoTaskSplitMetadataWithoutProvenanceKeepsGapsManualAndDropsManualDateState --tests com.microproject.exchange.MpoFileImporterTest.assignmentLevelingSplitStateSurvivesMpoSaveReloadAndClear --tests com.microproject.exchange.MpoFileImporterTest.releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave --tests com.microproject.exchange.MpoFileImporterTest.mpoAtomicMoveFailureKeepsOriginalArchiveAndInMemoryProject --console=plain
.\gradlew.bat :microproject_exchange:test --tests com.microproject.exchange.MpoHistoricalFixtureTest --rerun-tasks --console=plain
```
Sol retains the physical Robot route
`RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource`
as a separate exact-candidate GUI integration gate; headless tests do not prove
physical source preservation.

The initial uncommitted generated archive bytes were overwritten during
generator iterations. The original same-task baseline/split model was recreated
from the exact tag and checked-in generator; source literals and hashes were
rechecked, and the original baseline-work failure was reproduced before the
calendar fixes. The accepted archive hash above is for that recreated output;
it is not asserted byte-identical to the first uncommitted archive. An
exploratory moved-split archive did not match the source model and was excluded.
An initial source-calendar test cleanup materialized inherited weekday overrides;
exact `makeScratchCopy`/`assignFrom` restoration now makes the .24
altered-default → .23 read/save/reopen sequence pass without clearing global
registries.

Sol intake review: A1 requires the frozen representative cohorts and explicit unverified build combinations, not a fixture for every patch. The inventory meets that bounded contract; earlier supported reads remain in scope and individual untested builds are not claimed verified.
