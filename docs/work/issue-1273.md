# Issue #1273 — UsageDetailValuesTest fails with the run date

State: IN PROGRESS (fix in review)
Issue: https://github.com/tetsuji16/microProject/issues/1273
Contract frozen: issue body at intake
Baseline HEAD: `fb4bffbc9` (origin/master)
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` modified by a desktop run (not committed)

## Goal and scope

`UsageDetailValuesTest` must reach the same result on any run date, and the
calendar comparison it makes must still distinguish the project calendar from the
task calendar.

Out of scope: a sweep of other date-dependent tests (#773), any change to the
scheduling engine (not needed: the cause is in the fixture).
Blockers: none.

## Cause

- `createProject()` did not pin the start, so `Project.initialize` derived the
  schedule from the current clock. On 2026-11-20 (Friday) the assignment started
  Friday 17:00; on 2026-11-19 it did not.
- `DAY` is `CalendarOption.getMillisPerDay()`, an 8-hour **working** day, so
  `start + 7 * DAY` is a 56-hour window, not seven calendar days. From a Monday
  08:00 start it ends Wednesday 16:00, and `compare` then reports only the whole
  days (Mon + Tue = 16 h) for both calendars, because the excluded Wednesday is
  only partly inside the window.
- `WorkingCalendar.getStandardBasedInstance()` and `setWeekDay` are correct: a
  per-day probe shows the task calendar returning 0 on Wednesday and Thursday.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | The class passes regardless of run date | Pass | Start pinned to Monday 2026-11-16 08:00 JST before `initialize`; window is Monday 08:00 → Friday 08:00 of that week |
| A2 | The test still compares the two calendars (not a no-op) | Pass | Mutation check: removing the two `setWeekDay` calls fails `resourceWithoutCalendarUsesTheProjectCalendarNotTheTaskCalendar` |
| A3 | `:microproject_core:test` passes | Pass | `./gradlew.bat :microproject_core:test --console=plain` → BUILD SUCCESSFUL (1411 tests) |
| A4 | No scheduling code changed | Pass | Diff touches one test file plus this packet |

## Current slice

**Next slice:** run the required checks on the PR and, once `build` and
`GUI acceptance` pass, close #1273 and rebase the other open PRs on it.
**Done when:** both required checks are green on the PR head.

## Remaining blockers

- CI on the PR head.

## Latest evidence

- Tests/build: `:microproject_core:test --tests "com.microproject.pm.assignment.UsageDetailValuesTest"` → 8 tests, 0 failures; `:microproject_core:test` → BUILD SUCCESSFUL.
- Physical GUI/manual: not applicable (test fixture only).
- CI: pending.
