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
| A3 | Opt-in large-project build profile scales near-linearly | open | Candidate `1e79d96`; `-Dmicroproject.profile=true -Dmicroproject.profile.scales=1000,2000`: build 1.863s / 9.344s; not near-linear |

## Current slice

**Next slice:** Find and remove the remaining superlinear work in the production
bulk build path without weakening cycle or schedule correctness.
**Done when:** 1k and 2k profile runs show near-linear build time, preserve
schedule movement/undo checks, and the focused core tests pass.
**Then:** Review diff, rebase onto current master, push and create PR.

## Remaining blockers

- A3: Identify and fix the remaining nonlinear cost; repeat profile on the exact candidate SHA.

## Latest evidence

- Tests/build: Candidate `1e79d96`; `:microproject_core:test --tests "*TaskDependencyInvalidationTest*" --tests "*DependencyServiceTest*"` passed.
- Profile: Candidate `1e79d96`; opt-in 1k/2k passed last-finish movement and undo restoration, but total build time remains superlinear.
- CI: not yet run; no PR created while A3 remains unmet.
