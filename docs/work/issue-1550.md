# Issue #1550 — Initial Gantt fit

State: implementation and local verification complete; PR checks pending.
Issue: https://github.com/tetsuji16/microProject/issues/1550
Baseline: `7a2cda16bc9cb833d8b8f5a13c6ac5f5e52e7a34`.

## Diagnosis and change

The first fit ran at a 499px extent and its target was clamped to 11px by a 510px view. The responsive divider then reduced the extent to 252px; the normal center-preserving resize moved the project start offscreen. Fit after responsive layout validation; cancel pending work when a user gesture, explicit zoom or workspace restoration supersedes it. Preserve normal subsequent resize behavior and the existing file format.

## Evidence

- The initial-layout, pending zoom and restoration cases failed on the baseline and passed with the fix. The initial-layout test exercises the actual split lifecycle and the 499px to 252px transition.
- Focused tests cover the deferred scrollbar callback, manual scrollbar drag and the shared wheel listener architecture. `ScaledScrollPaneTest` covers subsequent center preservation.
- The real-window `GanttInitialFitGuiTest` covers final visible range, task dates and dependency, Undo state, MPO reload and later resize.
- Windows manual journey: new project starting 2026-11-28, Task A and Task B entered via the UI, predecessor 1 entered for Task B; dates 2026-11-30 and 2026-12-01, bars and dependency visible; saved MPO, closed and reopened with dates and dependency intact; scrolled and zoomed without an initial-fit jump. No application errors in captured stderr.
- Prescribed focused and full UI tests, source inventory check and `git diff --check`: pass. `clean build installDist`: pass. PR `build` and `GUI acceptance`: pending.

## Remaining scope

Manual view-position persistence across reopening remains an unresolved question in #1548; this change does not alter persistence.
