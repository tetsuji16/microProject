# Issue #1503 — calendar values and interfaces

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1503
Contract frozen at: 2026-11-28, issue body and comments (no comments)
Baseline HEAD: `3c10c0cc5960513da25ff3ef3443fa46fb3111b2`
Baseline working tree: clean in isolated worktree

## Goal and scope

Document and refactor only the six approved calendar classes while preserving public contracts, configuration type names, serialized exception identity, and existing work-calendar boundary results.

Out of scope: working hours, date boundaries, holidays/recurrence semantics, persistence format, configuration names, other calendar implementations. Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Six named files only; public contracts and configuration.xml type names preserved | pass | Source diff contains Javadocs/blank-line normalization in the six approved classes; configuration XML was not modified. |
| A2 | WorkCalendar boundary start/end expectations match baseline | pass | Baseline and final `WorkingCalendarTest` + `CalendarDefinitionTest` passed, including weekend, single-day exception, and ranged-exception schedule boundary tests. |
| A3 | WorkRangeException class name and serialVersionUID preserved | pass | `SerialVersionUidPinTest` passed; class FQCN and UID source declaration unchanged. |
| A4 | WorkingCalendarTest, CalendarDefinitionTest, SerialVersionUidPinTest, and core suite pass | pass | All specified final commands report BUILD SUCCESSFUL. |
| A5 | At most 10 files, under 400 changed lines, clean diff check | pass | Six source files plus this packet; source diff is 39 insertions / 18 deletions; `git diff --check` is empty. |

## Current slice

**Next slice:** Commit the verified six-file source refactor and execution packet, then open one PR.
**Done when:** PR checklist passes and build/GUI acceptance have completed with no actionable review comments.
**Then:** Report the final evidence and leave the PR unmerged.

## Remaining blockers

- none

## Latest evidence

- Required focused calendar, serial UID, relocation, and core-suite commands all report BUILD SUCCESSFUL on final candidate; exact commands and evidence are in the PR body.
- Physical GUI/manual: not applicable; no user-visible route changes.
- CI: pending PR head.
