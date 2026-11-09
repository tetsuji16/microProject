# Issue #1027 — Switch GUI smoke to tagged journeys

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1027
Contract frozen at: 2026-11-10, issue body fetched from GitHub
Baseline HEAD: `25b534e927fc8963429c5208c83c8de7a96fc787` (#1029 merged)
Baseline working tree: clean

## Goal and scope

Make `smoke` an alias of the `@GuiJourney` suite, remove the hand-maintained
smoke patterns, add the one-tag-per-mechanism Gradle guard, and update the
specified policy docs. Preserve all tests and assertions.

Out of scope: removing Robot tests, moving tests to L2, changing visual/full
suite contents, and stable-release policy.
Blockers: none; prerequisite PR #1029 merged as `25b534e9`.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Smoke is selected by the journey tag; hand-maintained patterns removed | pass | Local diff on current branch |
| A2 | Each InputMechanism appears exactly once; guard reports missing/duplicate values | pass | Guard probe reports missing `GANTT_WHEEL` and duplicate `HEADER_CLICK` |
| A3 | Every old smoke item classified in PR body | open | Pending |
| A4 | Specified docs match the new behavior | pass | Local diff on current branch |
| A5 | Compile, dry-run, and negative guard probe pass | pass | Local commands below |
| A6 | Exact PR smoke CI and 3 OS Build pass | open | Pending PR |

## Current slice

**Next slice:** Record the complete legacy smoke disposition table in the PR
description, including the stale Gantt wheel selector.
**Done when:** All 33 method entries and both full-class entries have a
documented disposition.
**Then:** Commit, push, create the master PR, and verify smoke CI and all OSes.

## Remaining blockers

- PR CI is outstanding.

## Latest evidence

- Baseline clean; prerequisite #1029 merged as `25b534e9`.
- The 33 old method selectors include one stale entry:
  `physicalGanttWheelScrollsByOneOwnerStep` no longer exists. The current
  physical wheel test is `physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep`;
  it is now the `GANTT_WHEEL` representative.
- Local checks:
  - `:microproject_ui:compileGuiTestJava` — passed.
  - `:microproject_ui:guiTest -PguiTestSuite=smoke --dry-run` — passed.
  - `:microproject_ui:guiTest -PguiTestSuite=journeys --dry-run` — passed.
  - Negative guard probe changed `GANTT_WHEEL` to `HEADER_CLICK`; configuration
    failed with both missing and duplicate names, then source was restored.
- Scope: Gradle suite selection, one existing wheel journey, and the four
  documentation paths listed in the issue. No Robot tests or assertions removed.
