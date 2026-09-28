# Task model responsibility map

Status: inventory for issue [#743](https://github.com/tetsuji16/ProjectLibre/issues/743). This records the current shape and a safe extraction order; it does not authorize moving serialized task state out of its compatibility classes.

## Current size and public surface

As inspected on 2026-09-28, `Task.java` has 2,344 lines and 269 public method declarations. `NormalTask.java` has 2,382 lines and 224 public method declarations. The larger concern is the number of contracts implemented by these two classes: `Task` implements identity, notes, calendar, dependencies, schedule/window, snapshots, time-distributed data, custom fields, document/hierarchy membership, leveling, timesheets, expense type, and task-link reference interfaces. `NormalTask` adds allocation, task-specific scheduling fields, assignments, earned-value values/fields, time-distributed fields, baseline scheduling fields, and indicators.

Both are persistent compatibility types. `Task` has `serialVersionUID = 786665335611L`; `NormalTask` has `serialVersionUID = 273898992929L`. `NormalTask` also has explicit versioned `writeObject` / `readObject` methods. Keep these FQCNs, UIDs, serialized fields, and stream methods as the stable persistence shell during extraction.

## Responsibility map

| Current owner | Responsibilities observed in source | Important connections and invariants |
| --- | --- | --- |
| `Task` | Identity, WBS parent/children, dependency strings and traversal, common scheduling dates/windows, constraints, manual/inactive/hidden flags, custom fields, external/subproject linkage, summary state, cloning and persistence reinitialization | The project owns shared schedule/dependency state. WBS and dependency traversal affect ordering and cycle handling. Many fields are serialized directly; transient helpers are rebuilt after load. |
| `NormalTask` | Duration/calendar changes; assignment create/update/remove; task and assignment actual/remaining values; baseline snapshot access; cost/work and earned-value projections; progress overrides; interval moves and splits; timesheet state; versioned serialization and cloning | Current and baseline values live in snapshots and interact with assignment calendars, scheduling rules, parent rollups, and dependency recalculation. Mutations emit scheduling/object events and dirty state. |
| `TaskSnapshot` and assignment detail types | Concrete schedule, cost, work, assignment, and date state for a scenario/snapshot | These objects are part of existing snapshot, save/reload, and clone behavior. Do not duplicate their formulas in new services. |
| Existing services (`EarnedValueCalculator`, `ScheduleService`, `AssignmentService`, `TaskModeService`, `TaskProgressService`) | Reusable calculations and selected task operations already extracted from the aggregate | Search and route callers through an existing canonical service before adding another helper. Preserve event and transaction boundaries at the aggregate. |

The class names alone do not define clean boundaries: schedule edits can update assignments, and assignment edits can trigger task scheduling. A line-count split or field relocation would obscure those transitions and can alter Java's default serialized field layout.

## Recommended extraction sequence

1. Establish characterization tests around existing callers for schedule/date changes, assignment rollups, progress and earned value, plus native POD round trips. Record event order, dirty flags, Undo behavior, and calculated values before extraction.
2. Extract stateless earned-value and cost aggregation calculations first. Reuse `EarnedValueCalculator`; pass a narrow read-only input contract and keep mutations/events in `NormalTask`.
3. Extract assignment-to-task rollup calculations next. Keep assignment create/remove, shared snapshot mutation, scheduling-rule invocation, and event publication coordinated by the aggregate.
4. Extract interval operations only after their interactions with scheduling rules, dependency updates, splits, and parent rollups are captured by tests.
5. Consider a separate snapshot coordinator only if it can preserve the existing `TaskSnapshot` graph and stream shape. Keep `Task` / `NormalTask` serialization methods as adapters to the canonical state implementation.

For each step, search all production callers first; preserve one canonical implementation, run the owning core tests, and add a POD save/load/reload check when serialized state or snapshot behavior changes. MPO exchange coverage is separate and must not be used as a substitute for native POD compatibility.
