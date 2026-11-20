# Issue #943 — EVM user workflow and reporting

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/943
Contract frozen at: 2026-10-26, issue body fetched at intake
Baseline HEAD: `6d430c1591202a84ffeb08103988b868eb196313`
Baseline working tree: unrelated pre-existing changes in issue-944/945 files, docs/book/, issue-941/946 packets, sample sync files, and attachments; preserve all.

## Goal and scope

Determine and remedy missing user-facing EVM input/reference, status-date relationship, calculation presentation, report/export, and MPO persistence behavior per issue acceptance. Preserve existing MPO compatibility and established formula/model ownership.

Out of scope: Project Server/PWA/cloud integrations; changing external or POD wire formats; broad cosmetic UI work.
Blockers: none known.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | User-facing paths for PV/EV/AC and status date found or absent items grounded in code | pass | `configuration.xml` editable percent/actual-work and read-only actual-cost fields; task/project EVM spreadsheet presets; Status Date dialog and EVM fields now selectable in Custom Report |
| A2 | Known-value SV/CV/SPI/CPI formulas, aggregation, currency, and status-date relationship verified | pass | `EarnedValueProjectValuesTest` pins BAC, ACWP, BCWS, BCWP, CV, SV, CPI and SPI to hand calculations at two status dates, plus the MPO round trip and the no-baseline case (single task, internal units). Aggregation and currency rest on the report-level evidence below, not on that test: no summary rollup of EVM values is asserted anywhere. |
| A3 | MPO save/reload values and presentation confirmed; implemented routes have focused regressions | pass | MPO seven-value round trip + report-column availability tests pass; Windows Robot selected all seven EVM columns, asserted preview values, exported CSV, and asserted exported values (run `40341421912`) |
| A4 | Book/user-guide explainable operation scope defined from evidence | pass | Japanese chapters 5/10/12 describe tested calculation/input scope, status date, report fields, MPO persistence, and currency caveat; Windows Robot evidence is recorded below |

## Current slice

**Current slice:** Implementation and acceptance evidence are complete on the issue branch, rebased onto the current `master` head. PR #949 is the review/integration path.
**Done when:** Merge PR #949 after required checks are green; then close #943 with this packet linked as evidence.

## Remaining blockers

- No implementation blockers. The local Robot attempts were stopped twice by the desktop contention guard because foreground MSPaint overlapped the report dialog; hosted Windows GUI acceptance subsequently supplied physical-route evidence. Mixed-source currency amounts cannot be detected or converted; guidance says normalize before consolidation.

## Latest evidence

- Tests/build: `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.costing.EarnedValueCalculatorTest" --console=plain`, `./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.PodRoundTripTest" --console=plain`, and `./gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.CustomReportDialogBoxCsvTest" --console=plain` — BUILD SUCCESSFUL. Book EPUB `--check` passed (EPUBCheck zero issues); `git diff --check` passed. Target GUI command failed before assertions due desktop contention; no screenshot was produced.
- Physical GUI: [targeted Windows GUI acceptance run 40341421912](https://github.com/tetsuji16/microProject/actions/runs/40341421912) passed on code SHA `d9eed608ae6b2c5938fbdd926c19c2ba06a5d665` (Japanese locale, 100% scale): Robot test started and passed; 1 test, 0 failures/errors/skips. It selected BCWS/BCWP/ACWP/CV/SV/CPI/SPI, checked preview values, and exported/asserted CSV values.
- Targeted Windows GUI: [run 40346565761](https://github.com/tetsuji16/microProject/actions/runs/40346565761) passed on PR code SHA `efb32155370bb3d0365696c86ff3061dfaf51fc0` (Japanese locale, 100% scale): 1 test, 0 failures/errors/skips.
- PR checks on PR code SHA: [Build run 40346566646](https://github.com/tetsuji16/microProject/actions/runs/40346566646) and [GUI smoke run 40346566071](https://github.com/tetsuji16/microProject/actions/runs/40346566071) passed. The Build's initial attempt stalled during hosted runner font installation and was cancelled; rerun completed successfully.
- Local verification on code SHA `0be6535bfce1f3ad9b5c36bced1b8777e1946914` (packet-only changes followed): `./gradlew.bat verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures verifyNamingConventions build installDist --console=plain` — `BUILD SUCCESSFUL` (57 actionable tasks).
- Current branch is based on `master` commit `5e3a665d3`. All acceptance rows pass; no implementation blockers remain. PR #949 is ready for review/merge, after which #943 can be closed.

## Known-value regression for A2/A3 (PR #1269, follow-up to the merged PR #1263)

`EarnedValueProjectValuesTest` (`microproject_ui` test source) replaces the "> 0" checks with a fixture whose dates, budget and actuals are fixed. The project start is pinned to Monday 2026-11-16 08:00 JST **before** `initialize`, because a project without a start lets `initialize` schedule a background `recalculate()`/`setStart()` from the current clock; without that pin the elapsed part of the baseline — and therefore BCWS, BCWP and ACWP — changes with the run date.

Fixture: 10 working days at the default 8-hour day (80 h) at $60/h, saved as Baseline; the work then doubles to 20 days and 40 h are reported as actual (5 of 20 days, 25% complete).

The merged version of this test (#1263) pinned the mid-plan values to 16 h while still letting the clock choose the start. A run on a weekend snaps the start to Friday 17:00, the 24-hour status-date window then covers one working day, and BCWS comes out as 8 h — measured — so those dates failed. The fixed start removes the dependency instead of widening the tolerance.

| Value | Hand calculation | Asserted |
|---|---|---|
| BAC | 60 × 80 h | 1.728e10 |
| ACWP at the finish | 60 × 40 h | 8.64e9 |
| BCWS at the finish | the whole baseline | 1.728e10 |
| BCWP at the finish | 25% × BAC | 4.32e9 |
| Status date = end of the third working day (Mon–Wed) | 24 h elapsed | BCWS 24 h, ACWP 24 h, BCWP 12 h (efficiency 80 h ÷ 160 h), CV = SV = −12 h, SPI = CPI = 0.5 |

- The three time-phased values follow the status date: after three working days 24 h of the baseline is scheduled, 24 h has been spent and 12 h earned; at the finish the whole baseline is scheduled and the completed quarter is earned. SPI is 0.25 at the finish because the baseline was frozen at 80 h while the work doubled.
- Without a baseline BAC, BCWS and BCWP are 0, but ACWP still reports the cost of the work performed (24 h → 5.184e9), so CV is −5.184e9 and SV is 0. CPI is 0; SPI returns the divide-by-zero policy value (`CalculationPreference`, default 0).
- An MPO round trip reproduces the status date and all seven values, and moving the status date *after* the reload changes them as expected, which is what shows they are re-derived from the saved inputs rather than persisted.
- Assertions compare internal units (rate per hour × work in ms), so they do not depend on `GlobalPreferences.getCurrencyCode()`.

Not covered by this test and still standing on the earlier report-level evidence: aggregation of EVM values into a summary/project row (`configuration.xml` declares `sum="Sum"`) and currency handling for mixed-source costs. The test lives in `microproject_ui` because the round trip needs `MpoFileImporter` from `microproject_exchange`, which `microproject_core`'s test source cannot see.

Commands: `.\gradlew.bat :microproject_ui:cleanTest :microproject_ui:test --tests "com.microproject.pm.costing.EarnedValueProjectValuesTest" --console=plain` → BUILD SUCCESSFUL (2 tests, 0 failures). Mutation check: replacing `efficiency() * cost` with `cost` in `Assignment.bcwp` fails both tests (`BCWP is the earned half of those three days … expected 2.592E9 but was 5.184E9`), so the assertions do gate the calculation rather than restating it.
