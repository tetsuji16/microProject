# Issue #917 — MPP import semantic contracts

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/917
Contract frozen at: 2026-10-25, issue body plus finite matrix
Baseline HEAD: `ed276d5a497b93640079ed72562cafc625fe24cb`
Baseline working tree: preserve unrelated untracked attachment directory and sample sync JSON/locks.

## Goal and scope

Verify the pinned MPP8/9/12/14 input families against Project-origin literal
manifests, including selected fields after MPO save/reload. MPP writing,
connected services, POD wire behavior, and broad parity claims are excluded.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Independent literal manifests match import and MPO reload | pass | Project-origin fixture tests cover MPP8/9/12/14 and MPO reloads; full exchange module passed on tested base SHA below |
| A2 | Target fields, losses, unsupported and failure behavior are explicit | pass | Test and fixture manifests record task/hierarchy/link/lag, calendar periods, assignments, baselines/contour, MPP99/truncated-OLE failure, and unrepresented calendar cases |
| A3 | Reuse fixtures, cite primary sources, label document-derived decisions | pass | MPXJ v11.5.4 fixture provenance and Microsoft DayType/DayWorking/WorkingTime references are in fixture READMEs |
| A4 | Exact source/command/results and remaining criteria recorded | pass | Full focused class and exchange module results below; physical MSP was not used |

## Current slice

**Next slice:** Sol reviews and integrates this bounded implementation.
**Done when:** all A1–A4 evidence is reviewed and the issue is closed by its owner.
**Then:** no further #917 implementation slice remains.

## Evidence limits (not blockers for the frozen matrix)

- Explicit `DayType=WORKING` over a nonworking parent with no effective MPXJ
  intervals, dated calendar exceptions, and other calendar constructs absent
  from this fixture remain unverified. The importer does not invent default
  working hours for these cases.
- Physical Microsoft Project comparison is supplementary and was not run;
  all compatibility rules above are source/document-derived and limited to
  fixed fixtures. It is not a completion blocker.

## Latest evidence

- Tested base SHA: `ed276d5a497b93640079ed72562cafc625fe24cb` plus current uncommitted scoped changes.
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.server.data.MpxExportTrackingTest.project2003Mpp8AndProject2007Mpp12LinksMatchIndependentManifestAndMpoReload --tests com.microproject.server.data.MpxExportTrackingTest.malformedAndUnknownFamilyMppFailAtImporterBoundaryWithoutPublicationOrInputMutation --tests com.microproject.server.data.MpxExportTrackingTest.project2019TaskBaselineSlotsMatchIndependentManifestAndMpoReload --console=plain` (3 selected methods).
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.server.data.MpxExportTrackingTest.project2019Mpp14FixturesMatchIndependentManifestAndMpoReload --tests com.microproject.core.pm.exchange.converters.mpx.MpxCalendarWorkingHoursTest --console=plain`.
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.core.pm.exchange.converters.mpx.MpxCalendarWorkingHoursTest --tests com.microproject.server.data.MpxExportTrackingTest.project2019Mpp14FixturesMatchIndependentManifestAndMpoReload --console=plain` after adding the retained-positive-hours plus `NON_WORKING` weekday/dated-week proof.
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.server.data.MpxExportTrackingTest.project2019Mpp14FixturesMatchIndependentManifestAndMpoReload --console=plain` after adding absolute 08:00–12:00/13:00–17:00 boundaries and `WorkCalendar.compare` checks for Monday, lunch, Saturday, and Sunday.
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.server.data.MpxExportTrackingTest.project2019TaskBaselineSlotsMatchIndependentManifestAndMpoReload --console=plain` after tightening MPP fixed-cost expectations to exact values.
- PASS: SHA-256 of the four staged Project 2003/2007 fixture blobs matches the literal hashes in their provenance READMEs; scoped `.gitattributes` marks only the new MPP/MSPDI fixture files `-text`.
- PASS: `.\gradlew.bat :microproject_exchange:test --tests com.microproject.server.data.MpxExportTrackingTest --console=plain` (all class tests).
- PASS: `.\gradlew.bat :microproject_exchange:test --console=plain` (`BUILD SUCCESSFUL`, 48s).
- PASS: final `.\gradlew.bat :microproject_exchange:test --console=plain` (`BUILD SUCCESSFUL` in 57s; 1 test task executed, 9 tasks up-to-date).
- PASS: final `.\gradlew.bat :microproject_exchange:test --console=plain` after mechanical calendar-loop consolidation and typed malformed-input assertions (`BUILD SUCCESSFUL` in 49s; 339 tests, 0 failures, 0 errors, 0 skipped).
- No GUI route changed; no physical GUI run applies. Physical Microsoft Project comparison was not used; compatibility rules are source/document-derived.
