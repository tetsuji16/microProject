# Issue #1282 — Text Styles 1/2: 種類ごとのシート文字書式モデルと MPO 保存

State: IN REVIEW
Issue: https://github.com/tetsuji16/microProject/issues/1282
Contract frozen at: 2026-11-23, issue body as of master `622c10462`
Integration base: `origin/master` after package-layout refactor
Baseline branch HEAD: `6251956096`
Baseline working tree: clean in the dedicated integration worktree

## Goal and scope

Create the core model (`SheetTextFormat`, `SheetTextStyleCategory`, `SheetTextStyles`) and MPO sidecar persistence for per-category sheet text styles, without changing UI/rendering or POD format.

The task model did not have MSP's Marked field. To meet AC1, add a transient boolean to `Task` and store it in an MPO sidecar so existing POD serialization remains unchanged. Microsoft defines Marked as an entered Yes/No task field and documents that Text Styles' Marked Tasks category applies to marked tasks: https://support.microsoft.com/en-us/project/marked-task-field. This is a document-derived compatibility rule; physical MSP comparison was not performed.

Out of scope:
- Dialog, ribbon, rendering application (#1283)
- MSPDI/MPP export
- Gantt bar text style chooser changes

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | `SheetTextStylesTest` proves `MARKED_TASKS` wins over `CRITICAL_TASKS` for a task matching both | done | `markedTasksTakePriorityOverCriticalTasks`, tested SHA pending push |
| AC2 | MPO round-trip test: save 2 categories and a marked task, reload, values match | done | `mpoRoundTripPreservesSheetTextStylesAndMarkedTasks`, tested SHA pending push |
| AC3 | MPO archive without `sheetTextStyles` loads with `isEmpty() == true` | done | `olderMpoWithoutSheetTextStylesLoadsWithAnEmptyStyleSet`, tested SHA pending push |
| AC4 | POD read/write tests unchanged and all pass | done | `build`; both new POD-owned properties are transient and a focused reflection assertion enforces this |
| AC5 | `git diff --check` passes; `build` and module tests pass | done | full `build` and `git diff --cached --check` pass; exact SHA CI pending |

## Current slice

**Current slice:** Merged current master after its package-layout refactor; moved format types under `com.microproject.model.format`; added transient `Task.marked` plus MPO persistence; completed sheet-style MPO read/write and regression tests.

**Done when:** All AC1–AC5 pass locally.

**Next:** Push the integration merge and verify CI `build` and `GUI acceptance` on that exact SHA.

## Remaining blockers

- none

## Latest evidence

- `.\gradlew.bat :microproject_core:test --tests "*SheetTextStylesTest*" --console=plain`: PASS
- `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest.mpoRoundTripPreservesSheetTextStylesAndMarkedTasks" --tests "com.microproject.exchange.MpoFileImporterTest.olderMpoWithoutSheetTextStylesLoadsWithAnEmptyStyleSet" --console=plain`: PASS
- `.\gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain`: PASS
- `.\gradlew.bat build --console=plain`: PASS (5m16s)
- `git diff --cached --check`: PASS
- Physical GUI/manual: N/A (core/exchange change only)
- CI: pending on updated candidate SHA
