# Issue #1359 — Page Setup text truncation

State: READY FOR PR INTEGRATION
Issue: https://github.com/tetsuji16/microProject/issues/1359
Contract frozen at: issue #1359 current body, 2026-11-25
Baseline HEAD: `663e0f12df08f29fa77eb768a545043486c1cd96`
Baseline working tree: clean in issue worktree
Integrated base: `248abef9e` (#1405 merged to master)

## Goal and scope

Keep Page Setup paper, margin, orientation, and scaling labels/inputs fully visible in Japanese and English. Replace undersized fixed tracks with preferred-size tracks and ensure the parent dialog packs to the preferred width of its spanning Paper and Scaling panels.

Out of scope: print scaling formulas, printer settings, actual print execution.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Japanese and English Page Setup labels and inputs fit | pass | `PageSetupDialogGuiAcceptanceTest` physical route passed ja/en at 1.0 scale, explicitly asserting visible selected paper size, width/height values, and fit bounds; screenshots `modules/microproject_ui/build/reports/guiTest-artifacts/issue-1359-page-setup-{ja-ja,en-en}-1.0.png` show Letter, dimensions, margins and scaling. Headless preferred width test passed both locales. |
| A2 | Regression test fails before and passes after | pass | Headless `localizedPageSetupKeepsPaperAndScalingControlsAtPreferredWidth` fails against baseline master (ja spinner width 53 vs preferred 75), passes candidate. |
| A3 | Scale/page-count settings remain reflected in preview | pass | `GraphPageableFitToZoomTest.backstagePreviewRefreshesPageCountAfterFitChanges` observes actual Backstage preview page count changing to `1 / 1`; `requestedFitHeightSurvivesLayoutRefreshAndPaperChange` and `acceptedSettingsAreReadByANewPrintDocument` verify limits and accepted settings. Both classes passed. No print scaling formula changed. |
| A4 | UI build, GUI acceptance, inventory and whitespace checks succeed | local pass; PR CI pending | `:microproject_ui:test --tests "com.microproject.ui.print.PageSetupSettingsTransactionTest"`, physical GUI test ja/en, inventory check pass. Required exact-head `build` and `GUI acceptance` remain to be run by PR. |

## Root cause and fix

Physical ja runs found two nested-span failures. Initially the Scaling panel preferred width was 372px but its bounds were 270px and the dialog content preferred width was 293px; the parent `createDialogPanel()` used non-growing columns. The Paper panel bounds were 36px vs 153px preferred, and its inner paper-size panel was 0px vs 153px preferred. Growing the parent columns and Paper span columns lets both panels keep their preferred widths. The ja/en screenshots show the selected Paper size (`Letter`), width/height values, margins and scaling controls. Undersized fixed tracks were replaced with preferred tracks.

## Current slice

**Next:** commit and push the implementation; check PR feedback, then require exact-head `build` and `GUI acceptance` before merge.
**Done when:** PR #1363 has no actionable unresolved reviews and both required checks pass on its head.

## Latest evidence

- Base refreshed: fetched `origin/master` and merged `248abef9e` (#1405); no conflicts.
- `:microproject_ui:test --console=plain` — passed in 2m01s.
- `:microproject_ui:test --tests "com.microproject.ui.print.PageSetupSettingsTransactionTest" --tests "com.microproject.ui.print.GraphPageableFitToZoomTest" --console=plain` — passed after final candidate restoration.
- `:microproject_ui:guiTest --tests "com.microproject.ui.ribbon.PageSetupDialogGuiAcceptanceTest" -PguiTestLocale=ja -PguiTestUiScale=1.0 --console=plain` — passed; physical screenshot retained, paper combo and values explicitly asserted.
- Same focused GUI command with `-PguiTestLocale=en` — passed; physical screenshot retained, paper combo and values explicitly asserted.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` — passed.
- Revised headless regression runs on EDT and preserves parent-assigned child bounds. Candidate passes; baseline `origin/master` fails in Japanese (spinner width 53px vs 75px preferred).
- CI on earlier SHA `663e0f12d`: Build and GUI acceptance passed. On `608d07b27`, `verify` passed on macOS, Ubuntu, and Windows, and GUI acceptance passed; aggregate Build failed only because the still-unchecked issue criterion failed the checklist validator.
- CI on the final pushed candidate remains required before merge.
