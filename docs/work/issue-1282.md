# Issue #1282 — Text Styles 1/2: 種類ごとのシート文字書式モデルと MPO 保存

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1282
Contract frozen at: 2026-11-23, issue body as of master `622c10462`
Baseline HEAD: `622c10462`
Baseline working tree: clean

## Goal and scope

Create the core model (`SheetTextFormat`, `SheetTextStyleCategory`, `SheetTextStyles`) and MPO sidecar persistence for per-category sheet text styles, without changing UI/rendering or POD format.

Out of scope:
- Dialog, ribbon, rendering application (#1283)
- MSPDI/MPP export
- Gantt bar text style chooser changes

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | `SheetTextStylesTest` exists; `resolve` returns `MARKED_TASKS` format over `CRITICAL_TASKS` for a task matching both | open | pending |
| AC2 | MPO round-trip test: save 2 categories, reload, formats match | open | pending |
| AC3 | Existing MPO fixture without `sheetTextStyles` loads with `isEmpty() == true` | open | pending |
| AC4 | POD read/write tests unchanged and all pass | open | pending |
| AC5 | `git diff --check` passes; `build` and module tests pass | open | pending |

## Current slice

**Next slice:** Implement `SheetTextFormat`, `SheetTextStyleCategory`, `SheetTextStyles` in `microproject_core/pm/format/`; add `SheetTextStyles` field to `Project` (transient); add MPO sidecar read/write in `microproject_exchange/MpoSidecarJson.java`; add tests.

**Done when:** All AC1–AC5 pass locally.

**Then:** Verify CI `build` and `GUI acceptance` on pushed branch; open draft PR with `Closes #1282`.

## Remaining blockers

- none

## Latest evidence

- Tests/build: not yet run
- Physical GUI/manual: N/A (core/exchange change only)
- CI: pending