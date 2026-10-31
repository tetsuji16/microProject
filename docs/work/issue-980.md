# Issue #980 - Consolidate duplicated mutation / event-batch skeletons

State: IN PROGRESS (awaiting integration review)
Issue: https://github.com/tetsuji16/microProject/issues/980
Contract frozen at: issue text at intake (A1, A2)
Baseline HEAD: `25c41fd32`
Baseline working tree: clean worktree `microProject-w980`

## Goal and scope

One capture / batch / ModelTransaction / rollback / Undo skeleton for task-set
mutations, one rollback helper in DependencyService, and one deferred-batch
state machine for the two event managers.

Out of scope: public APIs of the managers, thread/lock semantics, behaviour of
the services.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | One skeleton; barrier released after exception (#946) | pass | `ProjectTaskMutation`, `restoringOnFailure`; `ProjectModelCommandReleaseTest` unchanged and green |
| A2 | Concurrency tests retained, publish order unchanged | pass | no test removed; `DeferredEventBatchTest` order case |

## Design

- `pm/task/ProjectTaskMutation` (package-private): used by RespectLinksService
  and TaskInactivationService. Rollback source = service instance, Undo source
  = service class, as before.
- `DependencyService.restoringOnFailure`: five identical try/catch-restore sites.
- `util/DeferredEventBatch<E>`: depth, rollback-only, deferred list, detach.
  Monitor moved from the manager to the batch object; all state is still
  mutated only under one monitor, listeners are still dispatched outside it by
  the caller of `publishBatch`.

## Differences between duplicates (diffed)

- Object manager stores facts at fire time (event recycled); schedule manager
  stores the event and converts to facts at detach under the monitor. Kept via
  a convert function.
- `hasPendingEventFor` and `PendingBatch.hasEvents` exist only on the object
  manager. Kept there.
- `ObjectEventManager.publishBatch` / `ScheduleEventManager.publishBatch`
  differ only in the failure message text. Not unified.
- No behavioural bug found.

## Latest evidence

- `:microproject_core:test` pass; `:microproject_application:test`,
  `:microproject_exchange:test`, `:microproject_ui:test` pass (no Robot/guiTest).
