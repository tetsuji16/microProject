# Issue #978 — Unify task-command preconditions in DocumentFrame

State: IN PROGRESS (Robot evidence pending CI)
Issue: #978
Contract frozen at: issue text at intake (A1-A3)
Baseline HEAD: `25c41fd32`
Baseline working tree: clean worktree `microProject-w978`, branch `claude/issue-978`

## Goal and scope

Resolve selection, read-only and lock preconditions for task commands through
one typed boundary in `DocumentFrame`, with one rejection-reason order and a
per-command locking decision.

Out of scope: broad refactor of the command switch; TaskCommandGateway internals.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Selection resolution and rejection reasons go through one path (`resolveEditableTaskTargets` / `editableTaskRejection`) used by Percent, Mode, Inactivate, Respect Links, Mark on Track, Level/Clear Leveling, Link/Unlink | pass | `RibbonButtonBehaviorTest.taskCommandFamiliesShareOneRejectionReasonForDocumentAndSelectionPreconditions` |
| A2 | Locking decided per command, rationale recorded | pass | table below; helper Javadoc; `persistedStateTaskCommandsAreBlockedByCollaborationLockWithoutMutationOrUndo` |
| A3 | One Robot journey on the real ribbon route verifying model/view/Undo | pending (CI) | `TaskInformationRibbonGuiAcceptanceTest.percentCompleteRibbonRouteSetsSelectedTasksWithUndoRedoAndMpo` (extended with lock-denied click) |

## Canonical rejection order

no-active-document, document-read-only, resolver reason (no-selection,
task-read-only, task-row-selection-required, no-task-selection,
selection-too-small for Link), collaboration-lock-denied. Rejections carry the
selected task ids and the active view.

## Lock rationale (per command)

| Command | Locks | Why |
|---|---|---|
| Percent, Task Mode, Inactivate, Respect Links, Mark on Track | selected tasks, in the helper | mutate persisted task state, like field edits via TaskCommandGateway |
| Level / Clear Leveling | plan's affected tasks, after preview | mutated set differs from the selection |
| Link / Unlink | inside TaskCommandGateway | already locks there; no double lock |
| Hide / Show, Insert Milestone/Summary, Indent/Outdent | unchanged | only reason string unified |

## Reason-string changes

`lock-failed` became `collaboration-lock-denied` for Hide/Show, Insert
Milestone/Summary, Indent/Outdent and Link/Unlink (gateway LOCKED mapping).
Updated pinned strings: three `assertEquals` in
`TaskInformationRibbonGuiAcceptanceTest` (Hide popup, Alt+Shift+Right, Ribbon
Link). Behaviour changes: read-only task selection in the five mutation
commands now reports `task-read-only` (was `no-selection`); null project in
Link/Unlink reports `no-active-document`; sibling-row check extracted to
`siblingTaskRowsRejection`; `getActiveSpreadSheet()==null` no longer NPEs.

## Latest evidence

- Tests/build: `:microproject_ui:test --tests RibbonButtonBehaviorTest` 54 pass, 0 fail; full `:microproject_ui:test` see report; `:microproject_ui:compileGuiTestJava` OK.
- Physical GUI/manual: pending, run by CI (not run locally; shared desktop).
- CI: pending.
