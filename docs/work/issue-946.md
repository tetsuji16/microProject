# Issue #946 — Frozen Gantt/Resource view after ~30 minutes

State: In progress; latent freeze candidates fixed, root cause not yet confirmed by a thread dump.
Issue: https://github.com/tetsuji16/microProject/issues/946
Contract frozen at: 2026-10-27, owner's "Required work" checklist in the issue comments.
Baseline HEAD: `e695b33ac2e8453774d13eca134b59e207bc71f5` (branch `claude/issue-946-freeze`, separate worktree).

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Diagnostics: reporter thread dump or local reproduction | open | No dump yet. |
| C1 | Mutation coordinator cannot leak from `Project.runModelCommand` / `TaskModeService` | pass | Setup, post-operation callback, and `commitEventBatch` failures now close the scope; `ProjectModelCommandReleaseTest` fails on baseline (2/2) and passes after the fix. Other `begin()` callers audited: try-with-resources or explicit `closeAll`. |
| C2 | Auto-recovery does not mutate the live model concurrently with EDT commands | partial | Job construction (`setEarliestAndLatestDatesFromSchedule`) now runs under `ProjectMutationState.coordinated`. The export itself still reads the model on the job thread, same as every user save. |
| C3 | Collaboration timer survives a failing poll | pass (code) | `TimerTask` catches and logs `Throwable`; no dedicated test. |
| C4 | Built-in diagnostics (error log, uncaught handler, EDT watchdog) | open | |
| A2/A3 | Robot journey after root cause is confirmed | open | |

## Next slice

Built-in diagnostics (C4) so the next report includes a thread dump, then A1.
