# Issue #919 — MPO backward/native contracts

State: VERIFIED source/physical; delivery tracked in PR #927.
Issue: https://github.com/tetsuji16/microProject/issues/919
Frozen 2026-10-25: issue-body A1–A4 and finite intake matrix. Scope remains #919;
#920+ has not started. No display-resolution changes or scale-specific remediation.
Baseline HEAD `b0bf097825c4f59c101805fcb2b99991ca30eccd` (tree tested as
`dd49d8b52a0c07ce4c7668087e98e88ba15c156b`). Root will record final integrated SHA.

## Frozen acceptance

| ID | Contract | Evidence / status |
|---|---|---|
| A1 | Distinguish MPOF container 1.0 from app/payload generations; bounded cohorts without shrinking supported reads. | Pass for representative .23/.24 cohorts; no per-patch/build claim. See MPOF and inventory docs. |
| A2 | Previous-writer archive and independent literal semantics through current read/save/reopen. | Pass for .23 selected fields and .24 task/resource/assignment, dates, calendars, duration, constraints, progress, links, populated native entries, plus separate baseline-cost probe. |
| A3 | Native/default and transaction preservation, including failure/cancel/overwrite refusal. | Pass: post-fix physical Save As/cancel/refusal route below; headless transaction tests pass. |
| A4 | Provenance, exact commands/counts, limits, and evidence layers. | Evidence recorded below; required CI completion and merge remain tracked in live PR #927. |

## Fixture provenance and semantic limits

- `testdata/mpo/v024-1265-previous-writer.mpo`: isolated historical writer commit
  `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`, JDK `25.0.4.101-hotspot`, UTC/English.
  Generator asserts its literal model before save and uses no current production code.
- Accepted archive SHA-256 `0CBB233D8836B472F80FBC8932CAE99CC9858670A61AC1F4DC70E31B7C1DFDD8`;
  content SHA-256 `DDB6A49BD2F7D5CA2E6432FE29B5711D08E8DA406EDB43664E9E4C29C1E34A7A`.
  Initial uncommitted bytes were overwritten; exact-tag shape was recreated and failure
  reproduced. Accepted bytes are not claimed identical to that first copy.
- Populated literal payloads include layout, hidden UID 4201, manual split UID 4202,
  leveling options, Dialog 15pt styling, calendars, and baseline profile slot 0/resource
  UID 7/duration 28,800,000 ms. Import and MPO reopen assert corresponding state.
  Assignment-leveling v2 entry is empty; only entry presence is proven. Synthetic v1
  migration is separate. See `testdata/mpo/README.md` for field-level coverage.
- Synthetic fallback probes distinguish missing `task-schedules` from a retained v1
  payload without its project/task calendar graph. Missing split provenance removes
  `manualIntervals`, `levelingIntervals`, and `manualLevelingDates`; gaps stay manual.
- Unique source calendar UIDs resolve by imported identity before name/global fallback,
  document-derived from Microsoft's project-local [`CalendarUID`](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/calendaruid-element?view=project-client-2016)
  and [`UID`](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/uid-element?view=project-client-2016)
  rules. Duplicate UID 2 in malformed .23 content retains old Standard/base fallback
  as compatibility behavior, not Microsoft conformance; date parity is not claimed.
  Baseline assignments privately snapshot their effective task/resource calendar.

## Confirmed import-order defects and fixes

The physical journey completed source-preservation assertions, then scalar MPO reload
failed at `Assignment.initializeImportedBounds`. The MSPDI project start was applied
only after task import; a manual task recalculated against the initial Today boundary,
and a derived resource calendar clamped its assignment to the next day. The zero-duration
query returned one working day and hit the unchanged bounds guard. The literal regression
uses a fixed Sunday initial boundary, failed before the early header pass, and passes
through public MSPDI import and MPO save/reload after it.

Applying that early header unconditionally also failed summary-only XLSX fallback:
its header dates are inferred from task rows (not declared project bounds), and its
converted task start differed from the raw date by eight hours. Old-order A/B rerun
passed. The current gate applies the early existing header routine only when a secure
raw MSPDI parse sees a nonblank Project-level date matching `ScheduleFrom` direction.
The same provenance path handles direct XML, XML with `.xlsx` extension, and selected
embedded MSPDI fallback. Reader/provenance state resets per parse/consume. All readers
retain the final header/default and earliest-task correction. Temporary diagnostics
were removed; guard and source assertions remain unchanged.

## Verification and remaining gate

Final local source worktree runs used UTC/English and `--rerun-tasks`:

```powershell
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=en -Duser.country=US'; .\gradlew.bat :microproject_exchange:test --rerun-tasks --console=plain
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=en -Duser.country=US'; .\gradlew.bat :microproject_application:test --rerun-tasks --console=plain
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC -Duser.language=en -Duser.country=US'; .\gradlew.bat :microproject_ui:test --tests 'com.microproject.pm.graphic.frames.DocumentGenerationTest' --rerun-tasks --console=plain
```

Tested source SHA: `7f2d94b0626382d7048ac787658cee44b52c8f1a`.
Results: exchange 350, application 37, UI `DocumentGenerationTest` 3; each had 0 failures,
errors, or skips, with all tasks executed. Separate earlier evidence: core 1,183 and
`OfficeChromePanelVisualSmokeTest` 2 pass; these predate this gate and do not prove the importer fix.

Required Build [40172018137](https://github.com/tetsuji16/microProject/actions/runs/40172018137)
failed the XLSX summary fallback with the assignment bounds guard. Old-order A/B and the
provenance-gated source pass locally. Required Build [40174030261](https://github.com/tetsuji16/microProject/actions/runs/40174030261)
passed on the tested SHA; required GUI [40174030287](https://github.com/tetsuji16/microProject/actions/runs/40174030287)
was running at packet update. Check live PR #927 for final CI/merge; all CI is not claimed passed.
Targeted Robot
[40172015362](https://github.com/tetsuji16/microProject/actions/runs/40172015362)
reported 1 JUnit test / 0 failures / 0 errors / 0 skips, but the GUI gate failed on a
deferred EDT `CellUtility.setAppearance` NPE because `CellStyles` was uninitialized.
Shared `createWindow` initialization/assertion was integrated and GUI-test compilation
passed. The post-fix target physical run
[40174026825](https://github.com/tetsuji16/microProject/actions/runs/40174026825)
passed on the tested SHA: 1 test / 0 failures / 0 errors / 0 skips; deferred EDT gate
passed with no `CellStyles` initialization failure. Artifact [12591357259](https://github.com/tetsuji16/microProject/actions/runs/40174026825/artifacts/12591357259)
contains GUI report, logs, and pre-approval screenshot. Reuse this physical evidence for
the documentation-only delivery commit; source/tests are unchanged from `7f2d94b0626382d7048ac787658cee44b52c8f1a`.

Prior diagnostic runs 40127457597, 40168853294, 40169897816, 40170415649, 40170978982
remain failures, not pass evidence. Required GUI smoke
[40172018126](https://github.com/tetsuji16/microProject/actions/runs/40172018126)
passed 53 / 0 failures / 0 errors / 1 skip on pre-gate SHA; it does not cover the
post-fix physical route. No implementation remains in scope; stop after current #919
delivery and leave #920+ open.
