# Issue #1025 — Make test project start constraints deterministic

State: COMPLETE
Issue: https://github.com/tetsuji16/microProject/issues/1025
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `51f95d1c374b9f1edbfef501fd019aa48d89c67b` (#1026 merged)
Baseline working tree: clean

## Goal and scope

Set both `Project.start` and the scheduling start constraint in tests that use the chosen date as schedule origin. Preserve value-only project-span/view-range tests and the already-correct Gantt bar drag fixture.

Out of scope: production code, date changes, and `DateTime` clock injection.
Blockers: none; prerequisite PR #1026 merged.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | All schedule-origin setter calls use one helper per file | pass | `ea582a482`; 28 core, 25 MPO, 1 GUI call replaced |
| A2 | Preserved setter calls are listed with reasons | pass | `ea582a482`; value-only and view-range calls documented below |
| A3 | 2030 fixture moved temporarily to 2020 same weekday and passes, then restored | pass | `ea582a482`; focused transition class passed; original dates restored byte-for-byte |
| A4 | Core, exchange, UI tests pass; GUI acceptance CI passes | pass | Exact PR head `5c369795`; Linux/Windows/macOS Build and GUI acceptance passed; PR #1030 merged |
| A5 | No production changes or weakened/disabled assertions | pass | `ea582a482`; test-only change |

## Current slice

**Next slice:** None.
**Done when:** All acceptance criteria passed and PR #1030 merged.
**Then:** Complete.

## Remaining blockers

- None.

## Latest evidence

- Tested source candidate: `ea582a482f210892fe3936fd9359905279db048e`; merged master commit: `59b78535c91e3dcbded2262412ba4ed41375bfe8`.
- Commands passed: `:microproject_core:test :microproject_exchange:test :microproject_ui:test`; `:microproject_ui:compileGuiTestJava`.
- Temporary-date probe passed for Monday 2020-06-01 and Tuesday 2020-06-02; fixture restored.
- Preserved: `ProjectScheduleBehaviorTest` (value-only span/backup); `GanttWheelZoomTest` and `GraphInteractorLifecycleTest` (chart origin only); `GanttBarDateDragGuiAcceptanceTest` (already sets both start values).
- Exact PR head `5c369795a51bbded9544798c1ca6270a8b2d0402`: Build passed on Linux, Windows, and macOS ([run 42653637843](https://github.com/tetsuji16/microProject/actions/runs/42653637843)); GUI acceptance passed ([run 42653637847](https://github.com/tetsuji16/microProject/actions/runs/42653637847)).
- [PR #1030](https://github.com/tetsuji16/microProject/pull/1030) merged; issue #1025 closed.
