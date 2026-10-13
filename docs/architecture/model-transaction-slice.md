# Model transaction slice

Status: implemented on the current checkout, 2026-10-12. This is a bounded
rollback slice for the existing `UpdateProjectCommand`; it does not close
architecture issues #403, #408, #409, #410, or #411.

`ModelTransaction` in `microproject_core` provides a caller-serialized
`validate → authorize → capture → apply → schedule → invariant → commit`
sequence. A failed apply, schedule, or invariant stage restores the captured
state before returning. The transaction does not own a project lock or suppress
domain events; the caller must serialize concurrent edits. Its commit callback
is a publication boundary, not a reversible stage: a commit exception is
reported as `COMMIT` and does not restore the model because listeners may
already have received the result. Fatal `Error`s propagate. A fatal error
during apply, scheduling, or invariant checks restores the captured state
first; before capture, it propagates directly. A fatal commit error propagates
with the model left applied.

`UpdateProjectCommand.execute` is the first production caller. Before capture,
it validates that targets are non-null, unique task instances owned by the
command's project and authorizes against project read-only state. Its rollback
snapshot captures each task's existing detail backup, imported progress
override, and dirty flag, plus project dirty/group-dirty state. A changed batch
performs one project-wide schedule pass before invariant checks and posts one
`UpdateProjectEdit` only after all targeted task updates complete.
`TaskSnapshotBackup.restore` clones its saved current schedule when restoring,
so a live recalculation cannot mutate and corrupt the backup reused by Undo
and Redo. The failure-injection test mutates a predecessor and its FS-linked successor,
then throws after the second task. Rollback restores all task and assignment
details with per-task recalculation suppressed, runs the project's existing
scheduler once after the full batch is restored, and restores dirty flags last.
Undo/redo share the same batch restore helper and also recalculate once after
restoring every task, then restore task dirty flags. Tests verify leaf and
summary current schedule, early/late windows, work, and progress through
success, three repeated Undo/Redo cycles, and an additional ordinary project
recalculation after both Undo and Redo; project
dirty state on failure; an empty affected-ID result; and no Undo edit. A mixed
local/foreign target batch is rejected before any mutation.

This restores model values, task/project dirty state, and the Undo result for
the tested pre-commit failure path. The existing task setters and
`restoreDetail` can emit scheduling/object events during mutation and recovery,
so observers may see intermediate notifications. This slice does not claim
atomic observer publication or rollback of mutations made by unrelated command
paths. A listener exception during `EditSupport.postEdit` can occur after an
earlier listener accepted the edit; the commit-failure test confirms the
command reports `COMMIT` while leaving the model and already-accepted Undo edit
applied.

MPO/POD and other serialized formats are unchanged. No revision journal,
coalesced change set, EDT snapshot coordinator, collaboration lease, or
immutable task projection is added here.

## Verification

On JDK 25, `:microproject_core:test` passed with 904 tests, 0 failures, 0
errors, and 1 skipped. The focused suites were `UpdateProjectRequestTest` (6
tests) and `ModelTransactionTest` (5 tests). `git diff --check` passed.

## Remaining architecture dependencies

The 2026-10-13 hierarchy/gateway consolidation passed
`:microproject_core:test :microproject_ui:test --console=plain --no-daemon`
on JDK 25 (BUILD SUCCESSFUL). Focused coverage asserts one revision for a
multi-relocation permanent sort, one revision for each Undo/Redo, no revision
for a no-op, nested publication only after the outer batch closes, and an
apply-then-throw invalidation after the barrier is released. GUI readiness was
not available for a structural Robot rerun; the prior structural GUI evidence
remains pending rather than inferred from these module tests.

- #408 still owns schedule gesture stale checks and structural paste rollback;
  these UI entry points are not moved by this slice.
- #409 is still open. The bounded `UpdateProjectCommand` path now has a
  project-local mutation revision, detached event batches tagged with their
  committed revision, and a UI cache refresh path that drops stale callbacks,
  installs one revision into bound views before notifying, and rejects callbacks
  after close/rebind. Core/UI tests cover two-writer event-batch isolation,
  stale callback delivery, close/rebind, EDT notification, shared table/Gantt
  revision, and cached schedule interval copying. Core and UI full test tasks
  passed on JDK 25; the Update Project physical journey passed at ja/125%.
  The structural Sort/Gantt Robot rerun was blocked before Robot input because
  Docker Desktop occupied the foreground window, so that physical path is not
  verified by this checkpoint.

  `TaskCommandGateway` now holds the same barrier across field, hierarchy,
  paste, schedule, and dependency commands. It advances one revision for a
  changed result or newly emitted facts; an exception after an apply boundary
  publishes existing event facts, or a schedule invalidation when no facts were
  emitted. Preflight rejection and no-op leave the revision unchanged. Focused
  gateway tests cover the apply-then-throw path and verify publication occurs
  after the barrier is released. `DataFactoryUndoController` now gives Project
  Undo/Redo the same outer barrier; nested edits coalesce into one revision,
  unavailable history is a no-op, and a nested rollback batch is replaced with
  a fresh schedule invalidation if the outer mutation committed. Focused core
  and UI tests cover these cases. Project-backed `DefaultNodeModel.relocate`
  and `sortOutlineBy` use the same `Project.runModelMutation` boundary:
  permanent sort planning and its multiple sibling relocations share one
  revision, while nested relocation/Undo calls contribute facts to the outer
  batch and cannot publish before it closes. Preflight/no-op leaves the
  revision unchanged; an exception after hierarchy apply publishes a fresh
  schedule invalidation if no event facts were emitted. Focused core tests
  cover changed sort, Undo/Redo, no-op, nested publication, and apply failure.
  `TaskCommandGateway` adapts typed results and checked exceptions to
  `Project.runModelCommand`; its legacy apply paths do not expose a common
  rollback contract, so failure after its apply marker conservatively advances
  the revision and publishes existing facts or a schedule invalidation. This
  is distinct from `UpdateProjectCommand`, whose inner `ModelTransaction`
  restores state on failed pre-commit stages and preserves applied state on
  commit failure. Other command families are not covered by these boundaries
  yet.

  This is not complete revisioned publication. The cache still updates mutable
  nodes in place. Remaining writer inventory, outside the new gateway and
  history boundaries, includes
  direct task/resource/project field setters and `Field.fireEvent`; hierarchy
  mutation through `DefaultNodeModel.add/cut/paste/remove/indent/move`, direct
  `MutableNodeHierarchy` and
  `OutlineCollectionImpl`; schedule/actual setters in `NormalTask`; dependency,
  assignment, and calendar services; project task create/delete and recalculation
  paths; and linked/subproject/portfolio updates. These must move through their
  existing command/service transaction boundaries rather than being wrapped
  one setter at a time. A later phase must snapshot every renderer-visible
  schedule value immutably and atomically install the same revision across
  bound views. Reentrant publication and full writer coverage remain open.
- #410 still needs a transaction boundary for broader task/resource/calendar,
  dependency, hierarchy, and ID-allocation cascades, plus authorization lease
  and post-commit-effect audits.
- #411 still requires immutable value projections and removal of supported
  renderer paths that read mutable `GraphicNode` state. The current tree has no
  `GraphicNode.row` field or `GanttRenderer.setRow` call, but Gantt rendering
  still consumes mutable graphic-node/domain objects through the transitional
  topology index.
- #403 remains the parent gate and still requires legacy-path removal and
  independent two-window verification after its child contracts are met.
