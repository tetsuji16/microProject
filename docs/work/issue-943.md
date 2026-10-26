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
| A3 | MPO save/reload values and presentation confirmed; implemented routes have focused regressions | partial | MPO seven-value round trip + report-column availability tests pass; physical EVM preview/CSV assertion is pending |
| A4 | Book/user-guide explainable operation scope defined from evidence | pass | Japanese chapters 5/10/12 describe tested calculation/input scope, status date, report field limitation, MPO persistence, and currency caveat; physical GUI evidence is explicitly marked pending |

## Current slice

**Next slice:** Commit the isolated #943 changes and run the targeted Windows GUI acceptance journey on that exact candidate SHA in GitHub Actions.
**Done when:** The physical Robot journey selects BCWS/BCWP/ACWP/CV/SV/CPI/SPI, observes calculated preview values, exports CSV, and asserts the exported values.
**Then:** Complete A3, integrate the passing candidate, and close #943 with all acceptance evidence.

## Remaining blockers

- A3 is partial. Local Robot execution was stopped twice by the desktop contention guard because foreground MSPaint (PID 25288, title `ch05-tracking-gantt.png - ペイント`) overlapped the report dialog; it stopped before product assertions. Use the isolated candidate's Windows GUI workflow for physical evidence. Mixed-source currency amounts cannot be detected or converted; guidance now says normalize before consolidation.

## Latest evidence

- Tests/build: `./gradlew.bat :microproject_core:test --tests "com.microproject.pm.costing.EarnedValueCalculatorTest" --console=plain`, `./gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.PodRoundTripTest" --console=plain`, and `./gradlew.bat :microproject_ui:test --tests "com.microproject.dialog.CustomReportDialogBoxCsvTest" --console=plain` — BUILD SUCCESSFUL. Book EPUB `--check` passed (EPUBCheck zero issues); `git diff --check` passed. Target GUI command failed before assertions due desktop contention; no screenshot was produced.
- Physical GUI/manual: `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.CustomReportDialogBoxChooserGuiAcceptanceTest.robotSelectsEarnedValueColumnsAndExportsTheirPreviewValues" --console=plain` failed before product assertions due `GUI_ENVIRONMENT_CONTENDED` from foreground MSPaint PID 25288. This is environment contention, not a product assertion result. No resolution or other app was changed.
- CI: targeted Windows GUI dispatch planned for the isolated candidate branch; no run yet.
