# Issue #943 — EVM user workflow and reporting

State: COMPLETE — the reopen symptom is explained, fixed and covered by a physical-route GUI test
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
| A3 | MPO save/reload values and presentation confirmed; implemented routes have focused regressions | pass | MPO seven-value round trip + report-column availability tests; physical route `ReportViewEarnedValueGuiAcceptanceTest` now selects report + preset in the real Reports tab, asserts the task row and its hand-calculated values, and compares the exported CSV cell by cell. It also caught and fixes the empty-preview defect described below. |
| A4 | Book/user-guide explainable operation scope defined from evidence | pass | Japanese chapters 5/10/12 describe tested calculation/input scope, status date, report fields, MPO persistence, and currency caveat; Windows Robot evidence is recorded below |

## Current slice

**Current slice:** The reopen symptom (a report preview that showed only column headers) is reproduced, root-caused, fixed in `ReportPrintable`, and covered by a pixel regression plus a physical-route GUI acceptance test. This packet is the evidence for closing #943.
**Done when:** The PR that carries the renderer fix, the pixel regression, the GUI route test and this packet passes `build` and `GUI acceptance`.

## Remaining blockers

- No implementation blockers. Mixed-source currency amounts cannot be detected or converted; guidance says normalize before consolidation.

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

## Reopen symptom: "the preview shows only the column headers" (fixed)

The reporter selected **レポート > カスタムレポート > Task Information > 出来高 - コスト** on v0.1.0.1271 and saw the column headers and the CSV button, but no task row and no values. The same route now renders the data, so the defect was in the renderer, not in the report model or the EVM calculation.

### Root cause

`ReportPrintable.drawHeader()` fills each header cell dark and then sets the paint colour to `Color.WHITE` for the label. `drawRows()` started without restoring the colour, so every *non-summary* data row was painted white on the white page — invisible. The summary row sets `Color.BLACK` before drawing, which is why the project-total row was the only body text that survived, and why the page looked "empty except the headings". The same leak happens after a group-header band, which also paints its label white.

Why the previous tests missed it: `ReportRenderingTest` only counted ink pixels over the whole page (`inkPixels(image) > 100`); the header band and the footer alone always exceed that, so a page whose rows are all white still passed.

### Fix

- `ReportPrintable.drawRows()` restores `Color.BLACK` before laying out rows.
- The group-header band restores `Color.BLACK` after its white label.
- New `ReportRenderingTest.tableDataRowsPaintTheirValuesInsteadOfOnlyTheHeader()` renders a two-row table and asserts each row band contains ink. Removing the reset makes this test fail; restoring it makes it pass, so the assertion gates the regression.

### Evidence

| Item | Result |
|---|---|
| Physical route | `ReportViewEarnedValueGuiAcceptanceTest` (guiTest, `ja`, 1.0): `MainRibbonFrame` → `addProjectFrame` → `GraphicManager.showReportView()` (the Reports tab / Custom Report entry point), then physically clicks the report combo and the preset combo |
| Rows | 2 rows: the task row and the project total (`summaryRow(1)`); every preview page has `endRow > firstRow` |
| Values (hand calculation) | BCWS `60 × 24 h × HOUR_MILLIS` = 5.184e9; BCWP half of that; CV = BCWP − ACWP; CPI 0.5; BAC `60 × 80 h × HOUR_MILLIS` = 1.728e10; VAC = BAC − EAC. Amounts are the stored cost unit (rate per hour × work in ms) and are asserted to be formatted exactly as `Field.getText(task, null)` formats them for the task sheet |
| CSV | The preview's own CSV button writes the file, the shared chooser is asked exactly once, and every cell of the exported task row equals the preview cell; the header row equals the preview column names; the UTF-8 BOM is preserved |
| Artifacts | `report-view-earned-value.png` (the task row is now visible above the total row) and `report-view-earned-value.txt` (model rows and page ranges) under `build/reports/guiTest-artifacts` |

Commands (Windows):

```
.\gradlew.bat :microproject_reports:test --tests "com.microproject.reports.render.ReportRenderingTest" --console=plain
.\gradlew.bat :microproject_reports:test :microproject_ui:test --console=plain
.\gradlew.bat verifyNamingConventions verifyArchitectureBoundaries --console=plain
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.reports.view.ReportViewEarnedValueGuiAcceptanceTest -Locale ja -Scale 1.0
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.reports.view.ReportViewEarnedValueGuiAcceptanceTest -Locale en -Scale 1.0
.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter com.microproject.reports.view.ReportViewEarnedValueGuiAcceptanceTest -Locale ja -Scale 1.25
```

The Gradle commands report `BUILD SUCCESSFUL`. The authoritative runner reports `tests=1 failures=0 errors=0 skipped=0` for each of the three locale/scale legs (ja/1.0, en/1.0, ja/1.25) at 2560x1440; the PNG and the layout dump are kept per leg under `build/reports/guiTest-artifacts/focused-…-<locale>-<scale>-<id>/`.
