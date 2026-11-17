# Issue #1180 — ribbon follow-ups

State: IN PROGRESS. Intake 2026-11-17; baseline `a1add999f` (isolated clean worktree).

Original contract: document Task Form, Resource Form, Task Board and Resource Plan exclusions; implement applicable report/agile tabs; add non-conflicting KeyTips; verify JA/EN at 100/125/150%; address Sheet Text Styles, Wrap Text, Drawings, Project Summary Task, Assignment Information, Network Align, Team Planner selected-task enablement, the batch-only icon test failure and reported local GUI failures. Preserve unresolved items; this packet does not narrow the issue.

| Acceptance | State / evidence |
|---|---|
| Exclusions, report/agile tabs and KeyTips | Open; audit against current #1166 implementation and Microsoft source. |
| JA/EN visual matrix | In progress: full GUI run 43999020531 on `a1add999f`. |
| Sheet Text Styles, Wrap Text, Drawings, Project Summary Task, Assignment Information, Network Align | Open; require model/route and persistence assessment. |
| Team Planner selected-task commands | Active slice; disabled without selected bars and enabled on selection. |
| Icon batch failure and local GUI failures | Open; verify failure artifacts and current behavior. |

## Active slice — Team Planner selection

Microsoft Support, [View your team's work with Team Planner](https://support.microsoft.com/en-us/project/view-your-team-s-work-with-team-planner), describes individual bar formatting and selection. The rule to disable selected-bar format commands without a selected task is a document-derived compatibility decision. Physical selection must enable Fill and Border; clearing selection or leaving the view must disable them, without mutating project data. A real Robot selection plus ribbon-state check is required.

Done when the focused Robot journey passes on Windows and relevant UI tests pass. Next: audit the remaining #1180 commands and phase coverage; do not close #1180 until every original row is satisfied.
