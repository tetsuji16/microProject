# Issue #972 — task table / Gantt divider

Baseline: `373803e51` on `origin/master`, clean isolated worktree.

The owner chose to restrict the divider to the last task-table column edge.
Microsoft documentation was searched in the issue; its range was not found, so
this is a product decision, not a Microsoft Project compatibility claim.

| Acceptance | Status and evidence |
| --- | --- |
| Document the Microsoft evidence | Recorded in issue comment, 2026-11-14. |
| Drag cannot pass the final column, including column and window changes | Focused split test passed; Robot journey passed after reproducing the pre-fix failure (divider 600, column edge 169). |
| Out-of-range saved divider is clamped on restore | Focused split restore test passed; full file reload remains unverified. |
| GUI acceptance | Pending exact-candidate CI. |

Scope: Gantt task table split; preserve other split views and existing workspace
state. Divider location is view state, not an undoable task mutation. Next slice: verify saved workspace through file reload and exact-candidate GUI acceptance before closure.
