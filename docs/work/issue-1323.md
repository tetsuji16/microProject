# Issue #1323 — custom tab and custom group command KeyTips

State: IN PROGRESS (implementation and local verification done; PR not opened)
Issue: https://github.com/tetsuji16/microProject/issues/1323
Contract frozen at: issue body of 2026-11-21
Baseline HEAD: `11a3034e9`
Baseline working tree: clean

## Goal and scope

User-defined ribbon tabs and the commands inside custom groups are reachable with the
keyboard: an Office-consistent tab KeyTip is assigned to every custom tab, and each command
in a custom group keeps its canonical KeyTip.

Out of scope: letting users edit KeyTips; ribbon appearance changes.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | A custom tab gets a tab KeyTip that does not take a default tab's KeyTip and is the same for the same customization | pass | `RibbonCustomizationRenderTest.customTabsGetTheOfficeFallbackKeyTipsInDisplayOrder` |
| A2 | A command added to a custom group keeps its canonical KeyTip | pass | `RibbonCustomizationRenderTest.aCustomGroupKeepsTheCanonicalKeyTipOfItsCommand` |
| A3 | KeyTips never collide with a KeyTip another tab already uses, including the prefix ambiguity of the KeyTip sequence | pass | `RibbonCustomizationRenderTest.aCustomTabSkipsAFallbackKeyTipAnotherTabAlreadyUses`, `.aFallbackKeyTipThatPrefixesATakenOneIsSkipped` |
| A4 | Alt → custom tab KeyTip → command KeyTip runs the command in the custom tab | pass | `RibbonKeyTipTest.aCustomTabKeyTipOpensTheCommandsOfItsCustomGroup`, `PreferencesCustomizeGuiAcceptanceTest.keyboardOnlyJourneyCreatesATabAddsACommandAppliesItAndRunsTheCommandByKeyTip` |
| A5 | Default tab KeyTips stay unchanged | pass | `RibbonCustomizationRenderTest.noCustomizationLeavesTheDefaultRibbonUnchanged`, existing KeyTip tests |

Rule source (document-derived): Office assigns a fallback KeyTip of the `Y` prefix followed
by a number (`Y1`, `Y2`, …) when the preferred KeyTip conflicts —
<https://learn.microsoft.com/en-us/office/dev/add-ins/design/add-custom-key-tips>. Custom tabs
are therefore numbered `Y1`, `Y2`, … in display order; a number that is already used, or that
is a prefix of a used KeyTip (the sequence narrows by prefix, so typing it would only narrow),
is skipped. The assignment is a pure function of the customization, so it is deterministic.

## Current slice

**Next slice:** open the pull request from `issue/1323-custom-tab-keytips` and run the required
`build` and `GUI acceptance` checks.
**Done when:** both required checks pass on the pull request head.
**Then:** comment the outcomes on the issue; close it once the checks are green.

## Remaining blockers

- The branch is committed locally only; no push, pull request, or CI run has been made.

## Latest evidence

- Tests/build: `.\gradlew.bat :microproject_ui:test --console=plain` — BUILD SUCCESSFUL;
  `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.PreferencesCustomizeGuiAcceptanceTest"`
  — 8 tests, 0 failures (one earlier run had a transient focus failure in
  `resetSelectedTabAndResetAllRestoreOnlyTheirOwnScope`; it passed alone and in the full-class
  rerun). `git diff --cached --check` — empty.
- Physical GUI/manual: the GUI acceptance class above drives the real Robot route; the locale/scale
  matrix and the required `GUI acceptance` workflow are pending CI.
- CI: not run (no push).
