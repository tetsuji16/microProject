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
| A3 | Opt-in large-project build profile scales near-linearly | open | Current candidate profile, 1k/2k: production-batch 3.306s / 11.658s; ordinary single-item workflow 13.778s / 75.103s; materially superlinear |

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

- Tests/build: Current candidate; `:microproject_core:test --tests "*TaskDependencyInvalidationTest*" --tests "*DependencyServiceTest*" --tests "*AssignmentServiceTest.collectionRemovalBalancesPoolTransactionsWhenObserversThrow"` passed.
- Profile: Opt-in 1k/2k passed last-finish movement and undo restoration for all three modes, but total build time remains superlinear. The `production-batch` mode opens no synthetic whole-build `MultipleTransaction`; it exercises phase-sized `NodeModel.add`, `DependencyService.connect`, and grouped `AssignmentService.newAssignments` at their individual boundaries. It measures 3.306s / 11.658s (3.52x for 2x tasks). Sequential mode uses repeated `Project.createLocalTaskNode`, `DependencyService.newDependency`, and `AssignmentService.newAssignment` and measures 13.778s / 75.103s. Separate `bulk` mode wraps the synthetic build in one outer transaction, which has no ordinary production caller identified. See the performance report for exact timings and caveats.
- Likely nonlinear source: every synthetic task gets its 1–5 day duration after attachment. `NormalTask.setDuration` captures a whole-project `ProjectTaskSnapshot` and calculation-state map before each command; that snapshot is used only for command-local exception compensation and discarded on success. UI schedule editing separately owns before/after snapshots for undo/redo. Per-task duration setup therefore adds repeated O(N) copying, about O(N²) overall, independently of critical-path event batching. An outer scheduling transaction does not safely share this rollback snapshot. A batch duration API would need one explicit whole-batch rollback owner plus a single before/after undo record and exception/partial-mutation tests; none was added here.
- Transaction/schedule checks: the profile reports that the last task finish moves on edit and restores on undo. `Project.publishEventBatchNow` closes object-event scheduling transactions in `finally`; batch assignment closes project/resource-pool transactions in `finally`; focused failure-path tests for assignment cleanup and dependency graph rollback passed.
- CI: not yet run. PR #1193 remains draft; A3 is still unmet.
