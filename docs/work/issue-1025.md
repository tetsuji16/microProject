# Issue #1025 — Make test project start constraints deterministic

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1025
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `51f95d1c374b9f1edbfef501fd019aa48d89c67b` (#1026 merged)
Baseline working tree: clean

## Goal and scope

Set both `Project.start` and the scheduling start constraint in tests that use
the chosen date as the schedule origin. Preserve value-only project-span and
view-range tests, and the already-correct Gantt bar drag fixture.

Out of scope: production code, date changes, and `DateTime` clock injection.
Blockers: none; prerequisite PR #1026 merged as `51f95d1c`.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | All schedule-origin setter calls use one helper per file | pass | `ea582a482`; 28 core, 25 MPO, 1 GUI call replaced |
| A2 | Preserved setter calls are listed with reasons | pass | `ea582a482`; see rationale below |
| A3 | 2030 fixture moved temporarily to 2020 same weekday and passes, then restored | pass | `ea582a482`; transition class passed with June 1–2, 2020 dates |
| A4 | Core, exchange, UI tests pass; GUI acceptance CI passes | partial | Local module tests pass; PR GUI acceptance pending |
| A5 | No production changes or weakened/disabled assertions | pass | `ea582a482`; six test-only files changed |

## Current slice

**Next slice:** Publish the reviewed commit and obtain the required CI on the
exact PR candidate SHA.
**Done when:** Linux/Windows/macOS Build and GUI acceptance pass for the PR.
**Then:** Merge the PR and close #1025.

## Remaining blockers

- GUI acceptance CI is outstanding.

## Latest evidence

- Tested source candidate: `ea582a482f210892fe3936fd9359905279db048e`.
- Exact commands and outcomes:
  - `.\gradlew.bat :microproject_core:test :microproject_exchange:test :microproject_ui:test --console=plain` — passed.
  - `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — passed.
  - Temporarily moved `finishToStartElapsedFourHourLagUsesFixedWallClockDatesAndUndoRedo`
    from Monday 2030-06-03 to Monday 2020-06-01; the focused
    `:microproject_core:test --tests com.microproject.pm.scheduling.TaskLinkAndBarScheduleTransitionsTest`
    passed. Original dates restored byte-for-byte.
- Preserved calls: `ProjectScheduleBehaviorTest` (three value-only project-span
  and backup/restore mutations); `GanttWheelZoomTest` and
  `GraphInteractorLifecycleTest` (visible chart time origin only); and
  `GanttBarDateDragGuiAcceptanceTest` (already sets both start values).
- The 28 transition-test calls, 25 MPO scheduling round-trip calls, and one
  working-time GUI scheduling fixture now call one helper in each file.
- Review: `git diff --check` passed; no production sources or assertions
  changed. Exact-candidate GUI acceptance is pending PR CI.
- Prerequisite: #1026 merged; CI passed Linux/Windows/macOS Build and GUI
  acceptance on PR head `462c1bc3`.
