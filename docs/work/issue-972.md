# Issue #972 — task table / Gantt divider

Baseline: `373803e51` on `origin/master`, clean isolated worktree.

The owner chose to restrict the divider to the last task-table column edge.
Microsoft documentation was searched in the issue; its range was not found, so
this is a product decision, not a Microsoft Project compatibility claim.

| Acceptance | Status and evidence |
| --- | --- |
| Document the Microsoft evidence | Recorded in issue comment, 2026-11-14. |
| Drag cannot pass the final column, including column and window changes | Focused split test passed; Robot journey passed after reproducing the pre-fix failure (divider 600, column edge 169). |
| Out-of-range saved divider is clamped on restore | GUI Robot journey writes an out-of-range workspace value, reloads it from disk, reopens the MPO through Backstage, and verifies the divider clamps at the final column edge. |
| GUI acceptance | Focused Robot journey passed locally on candidate `554d71073` plus the uncommitted regression test; PR checks on the resulting commit are pending. |

Scope: Gantt task table split; preserve other split views and existing workspace
state. Divider location is view state, not an undoable task mutation. Next slice: integrate against current master and pass the required PR checks. Then close the issue if the original acceptance evidence remains valid.


Latest verification: `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.WorkspaceConflictSaveGuiAcceptanceTest.reopeningMpoClampsOutOfRangeWorkspaceDivider" --max-workers=1 --console=plain` passed; exact candidate before commit: `554d71073` with the new test.


## Post-merge GUI regression (2026-11-17)

Stable release run [43969200466](https://github.com/tetsuji16/microProject/actions/runs/43969200466) failed when the divider Robot test followed the workspace-conflict test. The captured screenshot shows the modal `Message.projectAlreadyExists` prompt; native desktop evidence records a foreground `SunAwtDialog` before the file chooser. Reproduced locally by running the full `WorkspaceConflictSaveGuiAcceptanceTest` class on `8aa2ac423`.

Cause: the first test left its opened `Workspace conflict fixture` in the singleton `ProjectFactory` portfolio. `GraphicManager.cleanUp()` removes listeners and frames but does not remove that project. `Project.equals` compares project names, so loading the next same-named fixture triggers `Portfolio.addProject`'s confirmation dialog and blocks the Robot route.

The follow-up test fix closes the opened project through the normal document-close route, waits for removal from the shared portfolio, and uses a distinct project name for the divider fixture. The full class passed locally after the change. Focused hosted evidence is pending.
