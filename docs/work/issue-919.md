# Issue #919 — MPO backward/native contracts

State: ACTIVE — bounded historical/native contracts and headless workflows pass;
post-fix physical GUI CI is pending Sol's integration gate.
Issue: https://github.com/tetsuji16/microProject/issues/919
Frozen: 2026-10-25, issue-body A1–A4 and finite intake matrix. Scope is #919 only.
Baseline HEAD: `b0bf097825c4f59c101805fcb2b99991ca30eccd` (tree tested as
`dd49d8b52a0c07ce4c7668087e98e88ba15c156b`). Final tested worktree SHA is assigned
after root commits. Preserve unrelated attachment/sample sync files.

## Frozen acceptance

| ID | Contract | Evidence / status |
|---|---|---|
| A1 | Distinguish MPOF container 1.0 from app/payload generations; bound the tested historical cohorts without shrinking supported reads. | Pass for representative cohorts; no per-patch/build claim. MPOF introduction and .23/.24 evidence are inventoried in `docs/msp-exchange-format-inventory.md` and `docs/mpof/MPOF.md`. |
| A2 | Previous-writer archive and independent literal semantics through current read, save, reopen. | Pass for .23 selected fields and .24 task/resource/assignment, dates, calendars, duration, constraints, progress, links, native entries, and separately bounded baseline-cost probe. |
| A3 | Native/default writes and transaction preservation, including failure/cancel/overwrite refusal. | Headless transaction/application tests pass. Physical Ribbon Save As route previously failed after source-preservation assertions; post-fix exact-head Robot run remains pending. |
| A4 | Record provenance, exact evidence, commands/counts, and limitations. | Archive/content hashes and source are below. Current staged-blob hash and post-fix physical result remain pending. |

## Fixture provenance and semantic limits

- `testdata/mpo/v024-1265-previous-writer.mpo` was produced in an isolated
  archive of historical writer commit `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`,
  with JDK `25.0.4.101-hotspot`, UTC/English. The generator asserts the literal
  source model before invoking that writer and uses no current production code.
- Accepted archive SHA-256:
  `0CBB233D8836B472F80FBC8932CAE99CC9858670A61AC1F4DC70E31B7C1DFDD8`;
  content SHA-256: `DDB6A49BD2F7D5CA2E6432FE29B5711D08E8DA406EDB43664E9E4C29C1E34A7A`.
  Initial uncommitted bytes were overwritten during generator iteration; the
  exact-tag source shape was recreated and failure reproduced. Accepted bytes
  are not claimed identical to the first uncommitted archive.
- Literal populated payloads include sheet layout; hidden UID 4201; manual split
  UID 4202; leveling options; Dialog 15pt text styling; calendars; baseline
  profile slot 0/resource UID 7/duration 28,800,000 ms. Current import and MPO
  reopen assert their corresponding model state. Assignment-level leveling v2
  entry is empty; it proves entry presence only. Synthetic v1 migration tests
  are separately identified. See `testdata/mpo/README.md` for generation and
  payload-by-payload coverage.
- Synthetic missing-data probes distinguish absent `task-schedules` from a
  retained v1 schedule payload missing project/task calendar graph fields;
  schedule values remain and MSPDI calendar/date fallback is asserted. Missing
  split provenance removes `manualIntervals`, `levelingIntervals`, and
  `manualLevelingDates` together; gaps remain manual without invented dates.
- Unique source calendar UIDs resolve by imported source identity before name
  or global fallback. This is document-derived from Microsoft's project-local
  [`CalendarUID`](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/calendaruid-element?view=project-client-2016)
  and [`UID`](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/uid-element?view=project-client-2016)
  rules. Duplicate UID 2 in the .23 sample retains the prior Standard/base
  fallback as an old-file compatibility choice, not Microsoft behavior; date
  parity for that malformed calendar graph is not claimed. Baseline assignments
  privately snapshot the current effective task/resource calendar.

## Confirmed import-order defect and fix

The physical journey reached its scalar MPO reload after completing source
preservation assertions, then failed in `Assignment.initializeImportedBounds`.
The MSPDI project header start date was applied only after task import. A manual
task triggered recalculation against the importing project's initial Today
boundary; its derived resource calendar then clamped the assignment to the next
day. The raw task dates remained source dates, but the zero-duration assignment
query returned one working day, tripping the unchanged bounds guard.

`MspImporter` now applies the existing project-header routine immediately after
calendar import and before resources/tasks, then reapplies it after tasks for
the existing earliest-task correction. The regression uses a literal source
header and a fixed Sunday initial boundary (no runtime-date dependence), imports
through public `MspImporter`, and checks baseline/current manifest values before
and after MPO save/reload. It failed before the early call at the bounds guard
and passed after. The guard and source assertions are unchanged. Temporary
diagnostic logs and temporary repro tests were removed.

## Verification and remaining gate

Exact local commands (UTC/Japanese locale used for the latter runs):

```powershell
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=ja -Duser.country=JP'; .\gradlew.bat :microproject_exchange:test --console=plain
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=ja -Duser.country=JP'; .\gradlew.bat :microproject_application:test --console=plain
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=ja -Duser.country=JP'; .\gradlew.bat :microproject_ui:test --tests 'com.microproject.pm.graphic.frames.DocumentGenerationTest' --console=plain
```

Final local results: exchange 348 tests, application 37, UI `DocumentGenerationTest`
3; all 0 failures, 0 errors, 0 skipped. Earlier unchanged-source evidence remains
core 1,183 pass; UI `OfficeChromePanelVisualSmokeTest` 2 pass (0 skipped); the
latter does not cover the importer fix. The source-calendar and baseline-copy
regressions, historical archive, transactions, and synthetic fallback probes
remain in the exchange suite.

Physical runs 40127457597, 40168853294, 40169897816, 40170415649, and 40170978982
record prior failures during diagnosis; none is post-fix pass evidence. The
post-fix physical route
`RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource`
must run on the exact committed candidate. Do not change display resolution or
add resolution-specific remediation. No MPP write, POD wire, dependency, cloud,
or new GUI route is in this slice.
