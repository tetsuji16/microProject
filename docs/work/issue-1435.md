# Issue #1435 — Count selected tasks in the status bar

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1435
Contract frozen at: 2026-11-28, issue #1435 body; no issue comments
Baseline HEAD: `7894f73079838e2a54666a6b4d848106caf157be`
Integrated base: `c4dad23e1c90cbc5c657032c151ff61f90d92a6f` (#1480 merged)
Code/tested SHA: `8714c87f1481772f585818b4a7c90aba9b25a0f8`
Baseline working tree: clean in isolated worktree; primary worktree's unrelated changes untouched

## Goal and scope

Count only selected nodes whose implementation is a Task when updating the document status bar. Empty selection events now reach listeners so clearing a prior task selection also updates the count to zero.

Out of scope: changing blank-row selection (#1434), Gantt selection counts, status text/translations.
Blockers: required PR checks remain pending.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | Regression tests fail before and pass after the fix | pass | `8714c87f1`; before: new helper tests failed compilation because selectedTaskCount was absent; empty-selection regression failed with count 1; after: focused suite passed |
| AC2 | One task plus 50 void nodes counts as 1 | pass | `8714c87f1`; `selectedTaskCountIgnoresVoidRows` test |
| AC3 | Void-only list counts as 0; null list/elements are ignored | pass | `8714c87f1`; `selectedTaskCountIgnoresVoidRowsOnly` test |
| AC4 | Empty project select-all shows 0 in GUI | pass | `a96db4b0c`; Windows GUI, Task Sheet, blank cell click then Ctrl+Shift+Space; visible status `選択中のタスク数: 0` |
| AC5 | 11-task sample select-all shows 11 in GUI | pass | `a96db4b0c`; Windows GUI, copied CCPM sample, Task Sheet, one task then Ctrl+Shift+Space; visible status 11 |
| AC6 | Source inventory check passes | pass | `8714c87f1`; issue verification command passed |
| AC7 | `git diff --check` is empty | pass | `8714c87f1`; command returned clean |

## Current slice

**Next slice:** Open PR and wait for required checks.
**Done when:** PR checklist, build, and GUI acceptance pass on the candidate SHA and review comments are resolved.
**Then:** Parent agent merges after required checks and review.

## Remaining blockers

- `build` and `GUI acceptance` PR checks are pending.

## Latest evidence

- Tests/build: on `8714c87f1`, focused `:microproject_ui:test --tests "com.microproject.ui.frame.DocumentStatusBarTest" --console=plain` → BUILD SUCCESSFUL; full `:microproject_ui:test --console=plain` → BUILD SUCCESSFUL (2m09s).
- Static check: `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` passed; `git diff --check` clean. Logs are in `%TEMP%\issue1435-final-focused.log`, `issue1435-final-ui.log`, and `issue1435-final-inventory.log`.
- Physical GUI/manual: Windows, `:microproject_ui:run`, Japanese, default display scale; empty project blank cell then Ctrl+Shift+Space displayed 0. Copy `%TEMP%\issue1435-CCPM-sample.mpo` (11 tasks), Task Sheet, one task then Ctrl+Shift+Space displayed 11. No project content was edited.
- Reuse basis: physical GUI evidence is on `a96db4b0c`; the only source change after that SHA is removal of a trailing blank line in `DocumentStatusBarTest.java`. Production source and GUI behavior are identical. CLI tests and inventory were rerun on `8714c87f1481772f585818b4a7c90aba9b25a0f8`; final PR checks will run on the final PR SHA.

Close and report in the same work once every criterion, evidence item, and
required child is resolved within the approved scope. Otherwise list only unmet
criterion IDs and the evidence still needed. For reused evidence, record its tested SHA and the scoped-diff basis for reuse.
