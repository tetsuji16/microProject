# Issue 1105: GUI review follow-up

Baseline: `a5f6cdf03` (origin/master), clean isolated worktree. Original scope: duration pin undo; first assigned Gantt bar movement; requested Gantt/theme GUI suite; dark-theme critical-chain buffer visual check; physical IME input, drag tooltip, and ribbon after dark-theme switching; Japanese bundle parity; large-project scheduler profile.

| Criterion | State and evidence |
|---|---|
| Duration pin undo and first-bar movement | Implemented by [#1177](https://github.com/tetsuji16/microProject/pull/1177). |
| Japanese bundle parity | Implemented by [#1139](https://github.com/tetsuji16/microProject/pull/1139); focused test passed at baseline. |
| Scheduler profile | [Profile](../performance/scheduler-large-project-profile.md); related bottleneck tracked by #1178. |
| Requested GUI suite | Failed at baseline: `GanttBarDateDragGuiAcceptanceTest.robotResizeProgressAndSplitUseTypedScheduleGatewayWithUndoAndPersistence` did not advance progress (before/actual 1794934800000; requested 1794978000000; drag x=810 to 817). Windows 11, Japanese, 100%, JDK 25.0.4.1; foreground overlap monitor observed none. Screenshot and environment are in the local `build/reports/guiTest-artifacts` directory. Suite still running. |
| Dark buffer, IME, drag tooltip, dark ribbon visual review | Pending physical visual evidence; do not infer from headless tests. |

Next slice: finish the specified GUI run, diagnose the progress drag failure using the captured state and screenshot, and fix its root cause; then perform the four explicit visual checks. Do not close until all original criteria have evidence.
