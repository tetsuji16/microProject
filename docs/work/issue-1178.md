# Issue #1178 — Stack-safe scheduler dependency checks

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1178
Contract frozen at: 2026-11-17 (issue body fetched from GitHub)
Baseline HEAD: `a1add999f289676c5f50148a240ea7445694eb3d`
Baseline working tree: clean isolated worktree `microProject-issue1178`

## Goal and scope

Make `Task.dependsOn` stack-safe with lazy cycle-message construction, defer
critical-path work across bulk mutations, and demonstrate near-linear build
time using the opt-in scheduler profile.

Out of scope: cloud Project services; unrelated scheduler changes.
Blockers: the current 1k-to-2k profile remains materially superlinear.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Iterative, stack-safe dependency search; build cycle text only on a cycle | pass | Candidate diff; 6,000-task chain test passes |
| A2 | Bulk object-event mutations cause one critical-path pass | pass | Candidate `1e79d96`; `CriticalPath` transaction suspend/resume; full recalc 18ms / 21ms |
| A3 | Opt-in large-project build profile scales near-linearly | open | Latest candidate profile, 1k/2k: production-batch 304ms / 637ms; ordinary single-item workflow 8.617s / 42.292s (4.91x for 2x tasks); the required original path remains materially superlinear |

## Current slice

**Next slice:** Find and remove the remaining superlinear work in task creation,
dependency linking, and assignment paths without weakening cycle or schedule
correctness.
**Done when:** 1k and 2k profile runs on a verified production workflow show
near-linear build time, preserve schedule movement/undo checks, and the focused
core tests pass.
**Current PR:** Draft #1193 contains a finite verified slice. A3 remains open;
this PR must not be treated as satisfying the whole issue.

## Remaining blockers

- A3: Identify and fix the remaining nonlinear cost; repeat profile on the exact candidate SHA.

## Latest evidence

- Tests/build: Focused core invalidation/dependency/assignment rollback and Undo tests, MPO replay boundary tests, and exchange MPO dependency/assignment replay and linked-project refresh tests passed. PR CI for the updated head is pending.
- Profile: Latest opt-in 1k/2k run passed last-finish movement and undo restoration for all modes, but build time remains strongly superlinear. `production-batch` measures 304ms / 637ms, ordinary single-item `sequential` measures 8.617s / 42.292s (4.91x for 2x tasks), and synthetic whole-build `bulk` measures 190ms / 285ms. The assignment state-count delta is 150 / 150 for production-batch, 3,000 / 6,000 for sequential, and 3 / 3 for synthetic bulk. State-count increments are not direct counts of inner scheduling operations. The verified original sequential workflow remains materially superlinear; A3 is unmet. See the performance report for exact timings and caveats.
- Likely nonlinear source: every synthetic task gets its 1–5 day duration after attachment. `NormalTask.setDuration` captures a whole-project `ProjectTaskSnapshot` and calculation-state map before each command; that snapshot is used only for command-local exception compensation and discarded on success. UI schedule editing separately owns before/after snapshots for undo/redo. Per-task duration setup therefore adds repeated O(N) copying, about O(N²) overall, independently of critical-path event batching. An outer scheduling transaction does not safely share this rollback snapshot. A scoped batch API is now used only by linked-project refresh, with explicit outer rollback ownership; the original profile's setter-per-task path is unchanged.
- Linked-project refresh is a real bulk caller: `GraphicManager` invokes `ProjectMergeService.applyExternalTaskUpdates`. That path now passes its existing whole-project rollback snapshot and calculation-state map to `TaskDurationBatchService`, applies durations without per-task rollback snapshots and dependent invalidation, then recalculates once after all external task values are staged. The caller owns failure restoration; a regression test injects a failure after the first changed task and verifies schedules, calculation counters, task/project dirty state, metadata, revision, Undo history, and lock release. Successful refresh keeps its existing no-new-Undo behavior. This change does not change the original profile's direct per-task `setDuration` path, so A3 remains open until that profile itself meets its criterion.
- Candidate task-creation caller check: recurring-task insertion uses detached `newStandaloneNormalTaskInstance()` objects; they are not in the project task list while duration setters run, so that path does not incur the profile's per-task whole-project snapshots. A local Undo/Redo probe also exposed an existing null-`owningProject` failure when undoing the inserted subtree. It was excluded as an optimization target; a detached bulk API would not improve the measured build path and would obscure that failure.
- Transaction/schedule checks: the profile reports that the last task finish moves on edit and restores on undo. `Project.publishEventBatchNow` closes object-event scheduling transactions in `finally`; batch assignment closes project/resource-pool transactions in `finally`; focused failure-path tests for assignment cleanup and dependency graph rollback passed.
- Assignment caller boundary: `AssignmentDialog` invokes `AssignmentService.newAssignments` once for the selected tasks and resources. That API already owns one project/resource-pool `BatchUpdate`, but it closed its inner project `MultipleTransaction` before `runAtomicModelCommand` published the queued event batch. A one-task/one-resource regression measured a calculation-state delta of 6 before the change (two scheduler calculations), then 3 after an outer scheduling transaction was kept around the complete atomic command and publication (one `fastCalc`, with state counters advancing by 3). The undo remains one step; attach-after-mutation failure restores task/resource attachments, discards events and revisions, and releases transaction depth. The `production-batch` profile maps one resource per task with 50 independent `newAssignments` calls, so its assignment phase now records 150 state increments at both 1k and 2k (50 calls × one calculation each), down from 300; it does not establish a single calculation across the full build or satisfy A3.
- Latest profile rerun (no JFR, same 1k/2k scales): `production-batch` build 304ms / 637ms, with 150 assignment-state increments at both sizes; `sequential` build 8.617s / 42.292s (4.91x for 2x tasks), with 3,000 / 6,000 assignment-state increments; synthetic whole-build `bulk` build 190ms / 285ms with one calculation at each size. Each mode still reports final-task movement and undo restoration. `bulk` is not a verified user workflow; the original sequential acceptance remains materially superlinear and A3 stays open. These values are single-run indicative measurements; calculation-state increments are a counter, not a direct count of every scheduling loop pass.
- MPO replay caller: `MpoTaskOperationService.apply` now keeps one scheduler transaction around a ready batch only when all unapplied operations are `dependency.add` or `dependency.delete`. Graph validation reads the dependency graph, not calculated dates. Every other operation kind retains its original per-operation scheduler boundary; this avoids stale schedule reads in summary progress updates (which call `NormalTask.setPercentComplete`, read schedule dates, and update descendant stops), as well as unverified interactions in assignment/task/hierarchy edits. Core regression tests verify ordered dependency replay in one calculation, partial failure preserving earlier successful operations and recalculating before throwing, balanced transaction depth, and a dependency change followed by summary progress producing the same dates as direct sequential application. No MPO replay performance number is claimed; the original profile remains unchanged.
- Hierarchy renumber fast path safety: suffix renumbering is used only while the hierarchy knows its numbered prefix is sequential. Full renumber establishes that state; suffix appends preserve it; non-event additions/removals, moves, relocation, and indentation invalidate it. A normal append after a LOAD/SILENT numbered prefix therefore falls back to legacy full renumber. Regression coverage adds a silent task with ID 40 followed by an event append with ID 90 and verifies the final row IDs are 1 and 2.
- CI: focused local core and exchange tests passed; PR #1193 remains draft and A3 is still unmet.
