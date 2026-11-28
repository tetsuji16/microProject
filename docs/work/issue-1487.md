# Issue #1487 — Customize Ribbon page test locator

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1487
Contract frozen at: 2026-11-28, issue #1487
Baseline HEAD: `c7a4d021c4ab3dc5a8912f0d0090911db708328a`
Baseline working tree: clean isolated worktree; pre-existing changes in the primary workspace are untouched

## Goal and scope

Make the Customize Ribbon GUI acceptance test resolve its named page through the `CategoryPageHolder` wrapper and preserve the current layout assertions.

Out of scope: production layout or user-visible behavior; the other full-release GUI failures.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | `pageScroll` obtains `ribbonCustomizationPanel` and its outer category scroll pane through `CategoryPageHolder`. | pass | Focused GUI test passed with code committed at `72bbff4b3e58669d77377e6d6c70d06cc8e5a19e`; the helper searches the named visible page then walks to its containing scroll pane. |
| A2 | Focused GUI test checks page visibility, no horizontal scroll, and the Up button visible area. | pass | `capturesTheCustomizedRibbonOverflowMenuAndBothCategoriesAtNormalAndNarrowWidth` passed locally on code SHA `72bbff4b3e58669d77377e6d6c70d06cc8e5a19e`. |
| A3 | Gradle build and required PR checks pass. | pass | Local build passed; PR #1488 checks `build`, `GUI acceptance`, checklist, and Windows/Linux/macOS verify passed on head `3094945b6366f7e791dafa96c01ddea49c966221`. [GUI run](https://github.com/tetsuji16/microProject/actions/runs/46092276961), [checks](https://github.com/tetsuji16/microProject/actions/runs/46092276902), [checklist](https://github.com/tetsuji16/microProject/actions/runs/46092277505). |

## Current slice

**Next slice:** wait for checks on the packet-only follow-up head, then merge PR #1488.
**Done when:** A1–A3 evidence is retained and PR #1488 is merged.
**Then:** investigate the separate AutoRecovery F2 failure before restarting the release gate.

## Remaining blockers

- Merge PR #1488 only after the current head has passing required checks and no unresolved actionable review.
- Release run 46084523423 had three other GUI failures; the ResourceHeaderSorting test passed focused rerun 46089565939, AutoRecovery failed focused rerun 46089947875, and TaskInformationRibbon passed focused rerun 46090348600.

## Latest evidence

- Tests/build: focused GUI test passed in 59s; `build --console=plain` passed in 4m 46s (51 tasks, 30 executed, 21 up-to-date) on code SHA `72bbff4b3e58669d77377e6d6c70d06cc8e5a19e`.
- Physical GUI/manual: focused Windows GUI on Japanese/100%/1024x768 passed and retained page, horizontal-scroll, and Up-button visibility checks.
- CI: PR #1488 passed GUI acceptance and the three OS verification legs; checklist passed. The latest code and packet head passed required checks. A subsequent packet-only commit is being checked; no source, test, fixture, or build inputs changed.
