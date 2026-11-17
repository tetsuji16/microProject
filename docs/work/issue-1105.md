# Issue 1105: GUI review follow-up

Current review: `a1add999f289676c5f50148a240ea7445694eb3d` (master), separate clean `installDist` worktree; GUI observation used an unsaved temporary project and an isolated `user.home`. Original scope: duration pin undo; first assigned Gantt bar movement; requested Gantt/theme GUI suite; dark-theme critical-chain buffer visual check; physical IME input, drag tooltip, and ribbon after dark-theme switching; Japanese bundle parity; large-project scheduler profile.

| Criterion | State and evidence |
|---|---|
| Duration pin undo and first-bar movement | Implemented by [#1177](https://github.com/tetsuji16/microProject/pull/1177). |
| Japanese bundle parity | Implemented by [#1139](https://github.com/tetsuji16/microProject/pull/1139); focused test passed at baseline. |
| Scheduler profile | [Profile](../performance/scheduler-large-project-profile.md); related bottleneck tracked by #1178. |
| Requested GUI suite | The original GUI failure (progress drag did not advance) was fixed in [#1189](https://github.com/tetsuji16/microProject/pull/1189), merged to the reviewed master. Exact-candidate hosted GUI acceptance is still running: [run 43999020531](https://github.com/tetsuji16/microProject/actions/runs/43999020531), head `a1add999f289676c5f50148a240ea7445694eb3d`; don't claim this gate until its conclusion is recorded. |
| Dark-theme critical-chain buffer bands | Visually reviewed on Windows 11 at 100%, JDK 25.0.4.101, current `a1add999f`, using a temporary project in the current `installDist` build. Applied dark theme from Options, opened CCPM via Ctrl+Shift+K, applied CCPM, and inspected the Buffer Consumption tab: green/amber/muted-red bands and their legend remain distinguishable against the dark surface. |
| Dark-theme ribbon | Visually reviewed after live theme switching in the same temporary app session. Task and Reports ribbons remained legible against the dark ribbon surface. |
| Physical IME input | **Unverified.** Direct Unicode text entry rendered Japanese, but is not IME evidence. Physical letter-key entry produced Latin `KANJI`; the `@oai/sky` input API rejected the `Zenkaku_Hankaku` key (`unsupported key`), so an IME composition/reconversion route could not be exercised in this session. |
| Drag-time tooltip | **Unverified.** The supported desktop `drag` action sends the press/move/release as one operation, so no screenshot can be captured while the tooltip is transiently shown. Two temporary-project drags hit the wrong one-day bar region (one altered its start, one its progress); each was undone and the original 0% / 2026-11-17 state was visually restored. Neither is evidence that the drag tooltip appears. |

Next slice: finish the exact-candidate GUI acceptance gate by recording run `43999020531`'s final conclusion. Then seek a physical IME route and an in-drag tooltip capture that the current desktop API can observe; leave #1105 open unless both criteria are evidenced. No application project was saved; the task-bar edits used for coordinate discovery were undone.
