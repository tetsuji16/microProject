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
