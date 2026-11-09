# Issue #1028 — Move dialog layout checks to headless tests

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1028
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `59b78535c91e3dcbded2262412ba4ed41375bfe8`
Baseline working tree: clean

## Goal and scope

Move display-independent text clipping/layout assertions into `src/test`, add headless L2 layout coverage for the seven named dialog families in Japanese and English, remove only equivalent layout-only Robot methods, preserve physical input journeys, and move `NativeChooserSnapshotParserTest` if its source class is in `src/main`.

Out of scope: changing production code, weakening assertions/locales, deleting physical input coverage, DPI visual audit.
Blockers: none; prerequisite PR #1029 and latest master baseline are present.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Headless assertion helper in src/test; GUI helper delegates with no duplicate logic | open | Pending |
| A2 | Seven named dialog families have equivalent headless layout tests or issue comments explain impossibility | open | Pending |
| A3 | Only corresponding layout-only Robot methods removed; physical routes retained | open | Pending |
| A4 | Same assertions and Japanese/English locale coverage | open | Pending |
| A5 | NativeChooserSnapshotParserTest relocated if currently in main | open | Source inventory pending |
| A6 | Focused UI tests, GUI compilation, exact-head CI build (3 OS) and GUI acceptance pass | open | Pending |
| A7 | No src/main changes | open | Pending |

## Current slice

**Next slice:** Extract the display-independent assertion methods, then migrate each dialog as one commit.
**Done when:** Helper extraction compiles and preserves the current recursive clipping rules.
**Then:** Add the seven dialog headless tests and classify/removal table.

## Remaining blockers

- Required tests and exact PR CI are outstanding.

## Latest evidence

- Baseline `git status` clean at `59b78535c91e3dcbded2262412ba4ed41375bfe8`.
- Inventory: seven target GUI test classes use `DialogLayoutAssertions`; also two out-of-scope GUI classes use it. `NativeChooserSnapshotParserTest` is currently under `src/guiTest`, not `src/main`.
- No implementation or verification yet.
