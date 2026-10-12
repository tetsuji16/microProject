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

- #408 still owns schedule gesture stale checks and structural paste rollback;
  these UI entry points are not moved by this slice.
- #409 still needs a project domain revision source, legacy change accumulation,
  stale publication rejection, and atomic EDT installation. The current tree
  has a UI `RevisionedProjectionIndex` for topology, but no core domain journal
  or update coordinator. Its issue history also records missing evidence for
  all mutation-source instrumentation, nested/reentrant event tests, and
  mixed-revision visibility through real Swing listeners.
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
