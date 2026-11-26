# Issue #1359 — Page Setup text truncation

State: READY FOR PR INTEGRATION
Issue: https://github.com/tetsuji16/microProject/issues/1359
Contract frozen at: issue #1359 current body, 2026-11-25
Baseline HEAD: `663e0f12df08f29fa77eb768a545043486c1cd96`
Baseline working tree: clean in issue worktree
Integrated base: `248abef9e` (#1405 merged to master)

## Goal and scope

Keep Page Setup paper, margin, orientation, and scaling labels/inputs fully visible in Japanese and English. Replace undersized fixed tracks with preferred-size tracks and ensure the parent dialog packs to the preferred width of its spanning Scaling panel.

Out of scope: print scaling formulas, printer settings, actual print execution.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Japanese and English Page Setup labels and inputs fit | pass | `PageSetupDialogGuiAcceptanceTest` physical route passed ja/en at 1.0 scale; screenshots `modules/microproject_ui/build/reports/guiTest-artifacts/issue-1359-page-setup-{ja-ja,en-en}-1.0.png`. Headless preferred width test passed both locales. |
| A2 | Regression test fails before and passes after | pass | Headless `localizedPageSetupKeepsPaperAndScalingControlsAtPreferredWidth` fails against baseline master (ja spinner width 53 vs preferred 75), passes candidate. |
| A3 | Scale/page-count settings remain reflected in preview | pass | `GraphPageableFitToZoomTest.backstagePreviewRefreshesPageCountAfterFitChanges` observes actual Backstage preview page count changing to `1 / 1`; `requestedFitHeightSurvivesLayoutRefreshAndPaperChange` and `acceptedSettingsAreReadByANewPrintDocument` verify limits and accepted settings. Both classes passed. No print scaling formula changed. |
| A4 | UI build, GUI acceptance, inventory and whitespace checks succeed | local pass; PR CI pending | `:microproject_ui:test --tests "com.microproject.ui.print.PageSetupSettingsTransactionTest"`, physical GUI test ja/en, inventory check pass. Required exact-head `build` and `GUI acceptance` remain to be run by PR. |

## Root cause and fix

A physical ja run showed dialog content preferred width 293px, while the Scaling panel required 372px; the 3-column span was laid out at 270px and clipped `縦横比を`. The parent `createDialogPanel()` used non-growing `p` columns, so `pack()` did not honor the spanning panel's preferred width. Changing the two occupied parent columns to `p:grow` makes parent width follow the span (root pane 397px; Scaling bounds/preferred width 372px). Undersized child fixed tracks were also replaced with preferred tracks.

## Current slice

**Next:** commit and push this implementation; check PR feedback, then require exact-head `build` and `GUI acceptance` before merge.
**Done when:** PR #1363 has no actionable unresolved reviews and both required checks pass on its head.

## Latest evidence

- Base refreshed: fetched `origin/master` and merged `248abef9e` (#1405); no conflicts.
- `:microproject_ui:test --tests "com.microproject.ui.print.PageSetupSettingsTransactionTest" --console=plain` — passed.
- `:microproject_ui:guiTest --tests "com.microproject.ui.ribbon.PageSetupDialogGuiAcceptanceTest" -PguiTestLocale=ja -PguiTestUiScale=1.0 --console=plain` — passed; physical screenshot retained.
- Same focused GUI command with `-PguiTestLocale=en` — passed; physical screenshot retained.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` — passed on earlier slice; rerun before commit.
- CI: pending on pushed exact candidate SHA.
