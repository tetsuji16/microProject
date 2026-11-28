# Issue #1487 — Customize Ribbon page test locator

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1487
Contract frozen at: 2026-11-28, issue #1487
Baseline HEAD: `c7a4d021c4ab3dc5a8912f0d0090911db708328a`
Baseline working tree: clean isolated worktree; pre-existing changes in the primary workspace are untouched

## Goal and scope

Make the Customize Ribbon GUI acceptance test resolve its named page through the `CategoryPageHolder` wrapper and preserve the current layout assertions.

Out of scope: production layout or user-visible behavior; the other full-release GUI failures pending diagnosis.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | `pageScroll` obtains `ribbonCustomizationPanel` and its outer category scroll pane through `CategoryPageHolder`. | pass | Local focused GUI test passed on uncommitted fix; exact PR SHA pending. |
| A2 | Focused GUI test checks page visibility, no horizontal scroll, and the Up button visible area. | pass | `:microproject_ui:guiTest --tests PreferencesCustomizeGuiAcceptanceTest.capturesTheCustomizedRibbonOverflowMenuAndBothCategoriesAtNormalAndNarrowWidth`: BUILD SUCCESSFUL, test PASSED (worktree SHA not committed). |
| A3 | Gradle build and required PR checks pass. | open | Local `build` passed; PR checks pending. |

## Current slice

**Next slice:** push branch and open PR #1487; wait for required checks and review.
**Done when:** A1–A3 have evidence and PR #1487 is merged.
**Then:** resume the stable release failure investigation and prepare a new exact-SHA candidate.

## Remaining blockers

- A1–A3 evidence is pending.
- Release run 46084523423 had three other GUI failures; the ResourceHeaderSorting test passed focused rerun 46089565939, while AutoRecovery failed focused rerun 46089947875 and TaskInformationRibbon is running as 46090348600.

## Latest evidence

- Tests/build: the pre-fix focused Preferences test failed at `pageScroll(dialog, "ribbonCustomizationPanel") == null` on SHA `c7a4d021c4ab3dc5a8912f0d0090911db708328a` in [run 46089557804](https://github.com/tetsuji16/microProject/actions/runs/46089557804); captured screenshot shows the page content.
- Physical GUI/manual: focused hosted physical test; Japanese, 100%, 1024x768; page content captured, locator failed.
- CI: no PR yet.
