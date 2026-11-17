# Issue #1178 — Stack-safe scheduler dependency checks

State: implementation slice complete; A3 remains open pending PR review/CI.
Issue: https://github.com/tetsuji16/microProject/issues/1178
Branch: `issue/1178-scheduler-iterative-depends-on`
Baseline: master `5271fb2d5a46542c4b0adabc5c77074603db1b5e` (#1023 PR merge).

## Frozen acceptance

| ID | Contract | State / evidence |
|---|---|---|
| A1 | Stack-safe `dependsOn`; defer cycle text to cycle path. | Pass: iterative `DependencyGraph` traversal, identity visited set; 5k/50k chain tests. |
| A2 | Batch object-event scheduling to one critical-path pass. | Pass: existing batching plus bulk importer initialization; core suite green. |
| A3 | Opt-in large-project build profile scales near-linearly. | Partial: import-shaped builder 5k/10k = 622/726 ms (1.17x); original single-item profile 1k/2k = 9,801/43,177 ms (4.41x), so A3 remains open. |
| P1 | `DependencyGraph` iterative traversal in both dependency and successor directions. | Pass: 5k/50k chain test. |
| P2 | Centralized cycle check; iterative Kahn order throws for cycle. | Pass: self-link, 3-node cycle, diamond order, explicit cycle test. |
| P3 | MPP/MSPDI/CSV and MPO load use one bulk builder. | Partial: Microsoft MPP/MSPDI import and MPO's MSPDI snapshot route through `MspImporter` and the builder. No CSV task importer exists. |
| P4 | 5k bulk build timeout and 10k near-linear benchmark. | Pass: 5k initialization under 30s; 10k import-shaped builder profile is 1.17x the 5k time. |
| P5 | Preserve and report the original A3 sequential profile. | Evidence retained; unmet as above. |

## Implementation summary

- Added `DependencyGraph` with iterative identity-based traversal, `wouldCreateCycle`, and Kahn topological ordering.
- Routed `Task.dependsOn` and dependency registration through the shared graph behavior. The critical-path predecessor list seeds its arrangement in topological order; cyclic legacy/external placeholder graphs retain persisted order during initialization.
- Added `BulkScheduleBuilder.initialize` to suspend scheduling around project initialization and publish one final calculation. `MspImporter` uses it; MPP/MSPDI imports and MPO's embedded MSPDI snapshot share that path.
- No CSV task importer was found in `microproject_exchange`; no new CSV format/import contract was invented.
- Single-item schedule mutation propagation remains outside this slice. No `-Xss` workaround was added.

## Verification

- `.\gradlew.bat :microproject_core:test --console=plain` — passed.
- `.\gradlew.bat :microproject_exchange:test --console=plain` — passed, including MPO/MSPDI interoperability and round-trip coverage.
- Focused graph, builder, and dependency service tests — passed (5k builder under 30s; 50k chain stack safety).
- Builder profile command: `.\gradlew.bat "-Dmicroproject.profile=true" "-Dmicroproject.profile.mode=builder" "-Dmicroproject.profile.scales=5000,10000" :microproject_core:test --rerun --tests "*LargeProjectSchedulingProfile*" --console=plain -i` — passed; 622ms / 726ms; one final calculation each. The profile edit does not move the last task in builder mode; edit/Undo propagation remains covered in sequential mode and focused tests.
- Sequential profile: 1k/2k = 9,801ms / 43,177ms (4.41x); last task moves and Undo restores. This does not satisfy A3.
- `git diff --check` — pending final review.

## PR decision

Use `Refs #1178`, not `Closes #1178`: the import/bulk path is near-linear, but the issue's original single-item profile remains materially superlinear. PR body must list this remaining A3 criterion. Draft #1204's useful profile/status findings were carried in commit `acebd98eb`; after replacement PR exists, comment on #1204 that it was carried forward and close it.

## Compatibility and remaining work

- MPO is native and its load path is covered through MSPDI import; MPP remains interoperability-only. Existing exchange round-trip suite passed. No wire format was changed.
- CSV has no task importer in the repository, so CSV cannot be routed without a separate product-format decision.
- A3 remains the blocker for issue closure. Do not claim sequential workflow performance improved based on the builder profile.
