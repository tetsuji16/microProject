# Issue #946 — Gantt and Resource views become unresponsive

State: In progress; source inspection found no confirmed cause.
Issue: https://github.com/tetsuji16/microProject/issues/946
Contract frozen at: 2026-10-26, issue body and comment.
Reported version: 0.1.0.1271. Reporter saw this after about 30 minutes; views switch, but controls stop responding while Task Manager shows normal resource use.
Baseline HEAD: `ec0df6e9e6a4173cbf08301655b902f7c75c9efb`.
Baseline working tree: pre-existing edits in docs/MSP_STANDARD_COMMAND_INVENTORY.md and three UI test/source paths; untracked .codex-remote-attachments/, docs/book/, docs/work/issue-941.md, and sample sync sidecars. Preserve all.

## Goal and scope

Identify and fix the shared cause that leaves Gantt and Resource views visible but unusable after extended use. Keep the application responsive and both views' physical interactions functional after sustained runtime.

Out of scope: unrelated view redesign, broad performance optimization, and fixing unrelated UI recovery issues.
Blockers: no reproducer, logs, thread dump, or exact interaction sequence is attached yet.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Reproduce or instrument the extended-use freeze and identify a confirmed shared cause | open | Reporter version known; local diagnosis pending |
| A2 | Gantt and Resource remain physically interactive after the triggering duration/sequence | open | Reproducer and exact-candidate GUI evidence pending |
| A3 | No regression to model/view state, switching, or persistence | open | Relevant focused regression evidence pending |

## Current slice

**Completed investigation:** Shared `DocumentFrame` switches views through `activateTopView` (`DocumentFrame.java:1988-2008`); `MainView` manages split-view scroll/time-scale listeners (`MainView.java:160-181`); document cleanup removes listeners and closes view caches (`DocumentFrame.java:2633-2666`). No code-level cause explains the 30-minute delay; listener accumulation remains unconfirmed.
**Next slice:** Capture two `jcmd <pid> Thread.print` dumps 10 seconds apart during the freeze, plus `jcmd <pid> GC.heap_info`; record CPU/heap/GC activity and exact steps/view-switch cadence before the freeze.
**Done when:** diagnostics confirm an EDT/event-queue stall or identify a concrete repeated-work/listener-accumulation path and reproducible trigger.
**Then:** implement the confirmed root-cause fix, add a regression reproducer, and capture physical evidence.

## Remaining blockers

- Reporter reproduction details and diagnostic evidence are not available. Requested these details from the user; source inspection narrowed the useful diagnostics to EDT thread dumps and heap/GC state during failure.
- Do not attempt a speculative fix before a reproducer or diagnostic exists.

## Latest evidence

- Comments: reporter specified version 0.1.0.1271.
- Source investigation: shared view activation, scroll synchronization, and cleanup paths inspected; no 30-minute failure cause confirmed.
- Tests/build: none run for this issue.
- Physical GUI: pending reproducer.
- CI: none.

## Source review 2026-10-27 (issue comment 6419599124)

- Candidate 1: `Project.runModelCommand` (`Project.java:321-326`) and `TaskModeService` call `ProjectMutationState.begin()` before `try`; an exception in `atomicEventCheckpoint`/`beginEventBatch` leaks the process-wide fair `COORDINATOR` lock.
- Candidate 2: `AutoRecoveryManager.saveOnWorker` -> `ProjectFactory.getSaveProjectJob` mutates (`setEarliestAndLatestDatesFromSchedule`) and exports the live model off the EDT without coordination (5-minute cadence).
- Candidate 3: `CollaborationSession.start()` TimerTask has no try/catch; one exception kills heartbeats.
- Candidate 4: `JobQueue.schedule` silently drops a job whose name stays in `executingJobs`.
- Note: a blocked EDT does not make Windows show Not responding for Java, so an EDT block is still consistent with the report.
- Requested from reporter: two `jcmd <pid> Thread.print` dumps, opt-in log `-Dmicroproject.ui.debug=true`, what still responds, file type and location, sync sidecars, sleep/lock.

## Hardening slice 2026-10-27 (uncommitted, base 1d08ee174)

- `Project.runModelCommand` and `TaskModeService.apply` now release the `ProjectMutationState` barrier and close any started event batch when batch setup throws. `CollaborationSession` timer polls go through `runGuardedPoll`, so one failure no longer cancels heartbeats.
- Tests: new `ProjectMutationStateTest` x2, `TaskModeServiceTest` x1, `CollaborationSessionPollGuardTest` x2. Without the fix, the new tests failed and the leaked static lock failed 10 other tests in the same JVM. With the fix, `:microproject_core:test` passed all 1190 tests.
- This is hardening of confirmed latent defects. It does not confirm the #946 cause; A1 still needs a thread dump.
