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
| A2 | Known-value SV/CV/SPI/CPI formulas, aggregation, currency, and status-date relationship verified | pass | known formulas and MPO project aggregation/round-trip pass; early-vs-late status dates show BCWS increase and BCWP/ACWP nondecrease; mixed-source currency behavior documented |
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

## Known-value regression added on master (2026-11-19)

`EarnedValueProjectValuesTest` (microproject_ui test source) fixes the A2/A3 claims with exact numbers instead of "> 0" checks. Fixture: a 10-day task at the default 8-hour day (80 hours) with a $60/h resource, saved as Baseline; the work then doubles to 20 days and 5 days (40 hours) are completed, so the task is 25% complete.

| Value | Hand calculation | Observed |
|---|---|---|
| BAC | 60 × 80 h | 1.728e10 (costs are stored as rate per hour × work in ms) |
| ACWP at the finish | 60 × 40 h | 8.64e9 |
| BCWS at the finish | the whole baseline | 1.728e10 |
| BCWP at the finish | 25% × BAC | 4.32e9 |
| CV / SV / CPI / SPI | BCWP−ACWP / BCWP−BCWS / BCWP÷ACWP / BCWP÷BCWS | asserted at a mid-plan status date and at the finish |

- The status date drives all three time-phased values: with the status date three days after the start, 16 hours of the plan have elapsed, so BCWS is 16 hours of baseline, BCWP is 8 hours and ACWP is 16 hours (SPI = CPI = 0.5). Moving the status date to the finish schedules the whole baseline and earns the completed share.
- A task without a baseline reports BAC, BCWS, BCWP and SPI as 0.
- An MPO round trip reproduces the status date and all seven values, which confirms the values are re-derived from the saved inputs rather than persisted.
- Aggregation is `sum="Sum"` per `configuration.xml`; the currency comes from `GlobalPreferences.getCurrencyCode()` (locale currency when empty), which is why the assertions compare internal units.

Commands: `.\gradlew.bat :microproject_ui:cleanTest :microproject_ui:test --tests "com.microproject.pm.costing.EarnedValueProjectValuesTest" --console=plain` → BUILD SUCCESSFUL (2 tests, 0 failures).
