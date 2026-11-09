# Issue #1028 — Move dialog layout checks to headless tests

State: COMPLETE
Issue: https://github.com/tetsuji16/microProject/issues/1028
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `59b78535c91e3dcbded2262412ba4ed41375bfe8`
Baseline working tree: clean

## Goal and scope

Move display-independent dialog text clipping checks to `src/test`; add equivalent Japanese and English headless coverage for the seven named dialog families where a body panel can be built without a top-level window; remove only duplicate layout-only Robot coverage; preserve physical journeys. Do not change `src/main`.

Out of scope: visual/DPI audit and product code changes.
Blockers: Preferences dialog body has no separate headless construction seam; documented on #1028 (comment 6745938744), existing Robot layout checks retained as directed.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Shared assertion in src/test; GUI helper delegates without duplicate clipping logic | pass | `0d83d62ed`; `HeadlessLayoutAssertions` and delegation |
| A2 | Seven dialog families have a test or issue comment for inability | pass | Six headless tests; Preferences constraint comment on #1028 |
| A3 | Equivalent layout-only Robot coverage removed; physical routes retained | pass | Six target GUI test updates; Resource Information layout-only Robot class removed; other routes kept |
| A4 | Assertions not weakened; Japanese and English tested | pass | Six tests execute both locales with original clipping assertion |
| A5 | NativeChooserSnapshotParserTest moved if under src/main | pass | Inventory shows it is already under `src/guiTest/java`; condition does not apply |
| A6 | Focused UI tests, GUI compile, PR 3-OS build and GUI acceptance pass | pass | Exact PR head 424018a7: all 3 OS Build and GUI acceptance passed |
| A7 | No production code changes | pass | Diff contains no `src/main` paths |

## Current slice

**Next slice:** None.
**Done when:** All acceptance criteria passed and PR #1032 merged.
**Then:** Complete.

## Remaining blockers

- None.

## Latest evidence

- Headless tests added for Baseline, Change Working Time, Find, Project Information, Resource Information, and Resource Mapping. Each runs English and Japanese locales. `HeadlessDialogAssertionsTest` verifies nested layout and failure detection.
- Robot physical routes retained for Baseline, Change Working Time, Find, Project Information, and Resource Mapping. Resource Information's layout-only Robot class was removed after its General panel received headless coverage. Preferences remains unchanged.
- NativeChooserSnapshotParserTest is under `src/guiTest`, not `src/main`; no relocation is needed.
- Tested source before the documentation-only branch integration: `24fbfbbcd` plus current `src/main`-free dialog changes. Scoped `origin/master` integration adds #1027 journey coverage and does not change migrated dialog builders or tests.
- Commands passed:
  - `.\gradlew.bat :microproject_ui:test --tests "*LayoutTest" --tests "*NativeChooserSnapshotParserTest" --console=plain`
  - `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain`
  - `.\gradlew.bat :microproject_ui:test --console=plain`
  - `git diff --check`
- `src/main` unchanged. No local Robot execution. PR head 89053b271f43b2111452db95ff557b287c6dd248 passed GUI acceptance ([run 42661077554](https://github.com/tetsuji16/microProject/actions/runs/42661077554)) and Linux/Windows/macOS Build ([run 42661077339](https://github.com/tetsuji16/microProject/actions/runs/42661077339)).
- Prerequisite #1029 merged as `25b534e9`; #1027 has now also merged as `2b55231d`.

- Exact PR head 424018a757de2d6a9d578cf5d93a53bf2d898109: GUI acceptance passed ([run 42660060078](https://github.com/tetsuji16/microProject/actions/runs/42660060078)); Linux/Windows/macOS Build passed ([run 42660059944](https://github.com/tetsuji16/microProject/actions/runs/42660059944)).
- PR [#1032](https://github.com/tetsuji16/microProject/pull/1032) merged as `930ecb4c2ad3287bcc40a456d29ccd3fa6d038dd`; issue #1028 closed.
