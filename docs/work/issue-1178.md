# Issue #1178 — Stack-safe scheduler dependency checks

State: bulk/import build paths meet A3; the single-item link workflow is quadratic by design and quantified.
Issue: https://github.com/tetsuji16/microProject/issues/1178
Branch: `issue/1178-build-scaling` (based on master `314d316a0`).
Baseline for the first slice: master `5271fb2d5a46542c4b0adabc5c77074603db1b5e` (#1023 PR merge).

## Frozen acceptance

| ID | Contract | State / evidence |
|---|---|---|
| A1 | Stack-safe `dependsOn`; defer cycle text to cycle path. | Pass: iterative `DependencyGraph` traversal, identity visited set; 5k/50k chain tests. |
| A2 | Batch object-event scheduling to one critical-path pass. | Pass: existing batching plus bulk importer initialization; core suite green. |
| A3 | Opt-in large-project build profile scales near-linear. | Pass for bulk inserts, open for repeated single-item edits. `bulk` 1k/2k = 165/254 ms, `production-batch` 194/299 ms, `builder` 5k/10k = 180/302 ms (all 1.5-1.7x for 2x tasks). Sequential single-item build is 0.98/4.4/32 s at 1k/2k/5k and stays quadratic for the reason quantified below. |
| P1 | `DependencyGraph` iterative traversal in both dependency and successor directions. | Pass: 5k/50k chain test. |
| P2 | Centralized cycle check; iterative Kahn order throws for cycle. | Pass: self-link, 3-node cycle, diamond order, explicit cycle test; new bidirectional search is checked against `dependsOn` for every ordered pair over 30 randomized hierarchy projects. |
| P3 | MPP/MSPDI/CSV and MPO load use one bulk builder. | Partial: Microsoft MPP/MSPDI import and MPO's MSPDI snapshot route through `MspImporter` and the builder. No CSV task importer exists. |
| P4 | 5k bulk build timeout and 10k near-linear benchmark. | Pass: 5k initialization well under 30s; 10k import-shaped builder profile is 1.68x the 5k time. |
| P5 | Preserve and report the original A3 sequential profile. | Pass: original measurements retained in the profile document; current numbers added there. |

## Implementation summary

- Added `DependencyGraph` with iterative identity-based traversal, `wouldCreateCycle`, and Kahn topological ordering.
- Routed `Task.dependsOn` and dependency registration through the shared graph behavior. The critical-path predecessor list seeds its arrangement in topological order; cyclic legacy/external placeholder graphs retain persisted order during initialization.
- Added `BulkScheduleBuilder.initialize` to suspend scheduling around project initialization and publish one final calculation. `MspImporter` uses it; MPP/MSPDI imports and MPO's embedded MSPDI snapshot share that path.
- No CSV task importer was found in `microproject_exchange`; no new CSV format/import contract was invented.
- Localized critical-path recalculation for summaries and assignment batches, batched MPO operation replay, and the summary bound re-flagging fix landed separately (#1281 and follow-ups).
- `DependencyGraph.wouldCreateCycle` now uses `DependencyGraph.precedes`, a bidirectional search over the same edge set as `dependsOn`. It expands forward from the successor and backward from the predecessor and stops when either side is exhausted, so appending a link to a task with no successors no longer walks the whole predecessor chain.
- No `-Xss` workaround was added.

## Why the single-item link phase stays quadratic

Deterministic counters over the link phase (`TaskSchedule.calcDates` invocations, temporary instrumentation, not committed):

| Tasks | calcDates | per link | per task per link |
|------:|----------:|---------:|------------------:|
| 1,000 | 1,592,737 | 1,594 | 1.59 |
| 2,000 | 6,265,677 | 3,134 | 1.57 |
| 5,000 | 38,764,497 | 7,754 | 1.55 |

Every single-item link recalculates about 1.6 times as many tasks as the project holds, whatever its size. The
synthetic chain is built left to right, so each link moves the project finish and the late dates of every task
genuinely change. `CriticalPath` recomputes them eagerly, and
`TaskDependencyInvalidationTest.extendingCriticalPathRefreshesLateDatesOutsideTheChangedDependencyClosure`
requires that behaviour. Removing this term needs a decision about deferring slack computation, not a local
optimization. The cycle check was the only remaining avoidable superlinear term and is fixed above.

## Verification

- `.\gradlew.bat :microproject_core:test :microproject_exchange:test :microproject_application:test --console=plain` — passed (1,502 core tests plus the exchange and application suites).
- `DependencyGraphTest.precedesMatchesDependsOnAcrossRandomHierarchies` — passed; 30 seeds x 13 tasks x 169 ordered pairs compared against `dependsOn`.
- `DependencyGraphTest.dependsOnTraversesLongChainsIteratively` — passed with the new `precedes` assertions at 5k and 50k.
- Profile commands (each with `--rerun --tests "*LargeProjectSchedulingProfile*" -i`):
  - `"-Dmicroproject.profile.mode=sequential" "-Dmicroproject.profile.scales=1000,2000,5000"` — last task moves after the edit and undo restores it in every row.
  - `"-Dmicroproject.profile.mode=bulk"` and `"-Dmicroproject.profile.mode=production-batch"` at 1000,2000.
  - `"-Dmicroproject.profile.mode=builder"` at 5000,10000.
- Wall-clock caveat: this machine is shared, and identical code varied by more than 2x between runs. A controlled
  alternating before/after comparison stayed inside that noise band, so no wall-clock speedup is claimed for the
  cycle-check change; the JFR share of `wouldCreateCycle` fell from 6.0% to 1.0%.

## Compatibility and remaining work

- MPO is native and its load path is covered through MSPDI import; MPP remains interoperability-only. No wire format was changed.
- CSV has no task importer in the repository, so CSV cannot be routed without a separate product-format decision.
- Deferring slack (late date) computation is the only route to a near-linear single-item link workflow. That is a
  behaviour change for every consumer that reads late dates right after an edit, and needs an owner decision.
