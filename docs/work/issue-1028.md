# Issue #1028 — Move dialog layout checks to headless tests

State: IN PROGRESS
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
| A6 | Focused UI tests, GUI compile, PR 3-OS build and GUI acceptance pass | open | Local commands passed; exact PR CI pending |
| A7 | No production code changes | pass | Diff contains no `src/main` paths |

## Current slice

**Next slice:** Push the reviewed branch, create master PR, and wait for its exact-head build and GUI acceptance.
**Done when:** Build passes on Linux/Windows/macOS and GUI acceptance passes on PR head.
**Then:** Merge PR and close #1028.

## Remaining blockers

- Exact PR CI is outstanding.
- PreferencesDialogBox remains on Robot layout checks until a product body-builder seam is introduced; this is explicitly documented on the issue.

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
- `src/main` unchanged. No local Robot execution; PR GUI acceptance remains the physical verification gate.
- Prerequisite #1029 merged as `25b534e9`; #1027 has now also merged as `2b55231d`.
