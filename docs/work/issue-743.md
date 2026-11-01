# Issue #743 — task model responsibilities

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/743
Contract frozen: 2026-11-01, issue body including 2026-10-27 clarification
Baseline HEAD: `1bc21058b`; isolated worktree clean.

## Goal and scope

Separate task responsibilities along behavior and invariants while keeping Task/NormalTask as stable serialization shells. Preserve task scheduling, assignments, progress, earned value, events, Undo and POD/MPO compatibility. Existing steps 1–2 are credited in the issue; remaining steps are assignment rollups, interval operations and a snapshot-coordinator decision.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | Step 3: assignment-to-task rollups extracted or a reasoned non-extraction decision | Open | pending |
| A2 | Step 4: interval operations extracted or a reasoned non-extraction decision | Open | pending |
| A3 | Step 5: snapshot coordinator extracted or a reasoned non-extraction decision | Open | pending |
| A4 | Preserve representative scheduling, assignment, progress and earned-value behavior | Open | pending regression tests |
| A5 | Preserve POD stream identity/fields and POD/MPO save/reload | Open | pending fixtures |
| A6 | Document final responsibility boundaries and no duplicate canonical implementation | Open | update responsibility map after review |

## Current slice

**Next slice:** inventory remaining assignment-to-task rollups and determine whether further extraction is justified.
**Done when:** remaining operations are extracted or their distinct aggregate invariants are documented.
**Then:** characterize interval operations before extracting them.

## Evidence

- `docs/architecture/task-model-responsibility-map.md` records distinct project, summary and assignment scopes, the stable POD serialization shell, and existing canonical services. Do not merge formulas merely because their names match.
- Existing test plans distinguish core behavior from POD and MPO compatibility evidence; neither fixture substitutes for the other.
- Baseline characterization `NormalTaskPercentCompleteTest.assignmentRollupsUseTheirEmptyAndMultipleAssignmentBoundaries` passed on `1bc21058b` plus test only. Empty assignment lists yield `Long.MAX_VALUE` for resume/stop and zero for maximum labor units; multi-assignment minima and maxima use all assignments. Existing WBS stop aggregation is distinct and remains on the task. `HasAssignmentsImpl` has no explicit UID, so no fields, signatures, or serialization methods may change.
- `AssignmentValueRollup` now centralizes assignment start/resume/stop minima and maximum labor units. Task WBS summary handling and all mutations/events remain in `NormalTask`. Baseline and changed-head characterization covers empty and multiple assignments; `HasAssignmentsImplTest` covers distinct starts.
- `gradlew.bat :microproject_core:test --console=plain` passed; `:microproject_exchange:test` with `PodRoundTripTest`, `PodDescriptorInventoryTest`, `MpoFileImporterTest` and `MpoHistoricalFixtureTest` passed. `serialver` reported `7226222599710405289L` for `HasAssignmentsImpl` before and after.
- Initial two-start fixture failed because `Task.setStart` changes the dependency constraint rather than the current schedule. Setting the fixture schedule directly produced distinct assignment starts; the focused test passed. This was a fixture setup failure, not a product failure.
