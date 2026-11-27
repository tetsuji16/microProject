# Issue #1435 — Count selected tasks in the status bar

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1435
Contract frozen at: 2026-11-28, issue body as read at intake
Baseline HEAD: `7894f73079838e2a54666a6b4d848106caf157be`
Baseline working tree: clean in the isolated worktree; the primary worktree has unrelated changes

## Goal and scope

Count only selected nodes whose implementation is a Task when updating the document status bar.

Out of scope: changing blank-row selection (#1434), Gantt selection counts, status text/translations.
Blockers: physical GUI acceptance requires current Windows desktop/fixture execution.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | Regression test fails before and passes after fix | open | pending |
| AC2 | One task plus 50 void nodes counts as 1 | open | pending |
| AC3 | Only void nodes count as 0 | open | pending |
| AC4 | Empty project select-all shows 0 in GUI | open | pending |
| AC5 | 11-task sample select-all shows 11 in GUI | open | pending |
| AC6 | Source inventory check passes | open | pending |
| AC7 | `git diff --check` is empty | open | pending |

## Current slice

**Next slice:** Add the focused regression test and capture its pre-fix failure.
**Done when:** the new assertions fail against baseline code for the missing helper.
**Then:** implement task-only counting and verify the full issue matrix.

## Remaining blockers

- Physical GUI and required CI acceptance remain pending.

## Latest evidence

- Tests/build: baseline recorded at `7894f73079838e2a54666a6b4d848106caf157be`; no issue-specific run yet.
- Physical GUI/manual: pending, empty project and 11-task fixture required.
- CI: no PR yet.
