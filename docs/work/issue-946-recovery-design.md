# Issue 946: recovery snapshot design constraints

Status: design only. Recovery still reads live models and is not safe against concurrent edits; linked branches remain disabled. This document does not claim a fix.

## Required contract

Capture one consistent in-memory generation of the complete open branch, including linked descendants and shared pools, each document's stable identity, its original reference paths, transient MPO extensions, CCPM state, and operation journal plus applied IDs. Never overwrite any original file during capture or recovery. Construct the generation under a synchronization boundary covering every model and journal writer, then release it before filesystem access. If capture, validation, or publication fails, retain the previous complete generation and do not publish partial metadata.

Recovery must load only members of the captured generation into isolated working storage; resolve internal links and shared-pool references to those members, retain original paths separately for subsequent explicit save, and fail closed when a required member is absent or invalid. It must never silently load an original as a substitute. Keep existing MPO/POD readers unchanged for ordinary files and retain the current metadata reader for existing recovery snapshots.

## Evidence and unresolved design work

- `ProjectFactory.getSaveProjectJob` traverses live projects and `LocalSession.getSaveProjectJob` passes them to asynchronous exporters. Its linked-project recovery guard prevents child writes to original locations but does not fix the single-project read race.
- `MpoFileImporter.embeddedProjectsFor` reads linked files from disk, so the current MPO writer neither captures unsaved linked changes nor meets the no-I/O-under-barrier requirement. `restoreEmbeddedProjectReferences` extracts valid members but leaves failed references in the model; a recovery route must explicitly prohibit original-file fallback.
- `ProjectMutationState` is not by itself a proven universal barrier: `Task.setName` delegates directly. Audit all physical editing, background scheduling, import/merge and shared-pool writers, and establish a common synchronization protocol before claiming an atomic capture.
- MPO operation journal state is transient. A copy must carry its entire baseline, operations and applied IDs. Independently appending the same unsaved change to live and detached copies creates different IDs for the same actor and sequence. Reserve the operation IDs once as part of the coordinated capture, and ensure normal saves, concurrent recovery captures, failed writes and retry cannot discard or duplicate that reservation. The exact transactional protocol and rollback/retry semantics still require implementation and concurrency tests; merely cloning the journal is insufficient.
- Existing `copyProjectForTransaction` serializes to MSPDI and copies selected transient state. It does not establish a complete branch or universal barrier and is not a recovery snapshot API.

## Acceptance tests before enabling the new path

1. Concurrent edits across master, child and pool produce one coherent generation; a deliberately blocked disk write does not hold the model barrier.
2. Recover unsaved master and nested child edits with original files unchanged, then verify links, shared pools, Undo/Redo where applicable, and save/reload. Missing or corrupt members never open originals.
3. Existing journal IDs and applied IDs survive capture, retries and subsequent normal saves; simultaneous capture/save cannot generate duplicate actor/sequence identities or lose operations. Test merge against the original after recovery.
4. Fault injection at capture, member write, manifest publish and metadata publish leaves the preceding complete snapshot recoverable. Existing snapshots still open.
5. Verify physical GUI recovery route and model/rendered state in addition to focused module tests.

## Writer inventory and migration order (2026-10-29)

The following is a **candidate inventory**, not proof of complete coverage. On the
current uncommitted tree, searching public `void` methods whose names start with
`set`, `add`, `remove`, `clear`, `update`, `restore`, `apply`, `replace`, `insert`,
or `delete` followed by an uppercase letter finds 639 declarations in the task,
resource, assignment, calendar and dependency packages. Reproduce the count from
the repository root with:

```powershell
rg -n '^\s*public (final )?void (set|add|remove|clear|update|restore|apply|replace|insert|delete)[A-Z]\w*\(' modules/microproject_core/src/main/java/com/microproject/pm/task modules/microproject_core/src/main/java/com/microproject/pm/resource modules/microproject_core/src/main/java/com/microproject/pm/assignment modules/microproject_core/src/main/java/com/microproject/pm/calendar modules/microproject_core/src/main/java/com/microproject/pm/dependency | Measure-Object -Line
```

This scan misses non-void or non-public mutators, direct field/collection writes,
mutable returned objects, inherited/delegated methods, and writers outside these
packages. Inventory also needs project/outline and subproject membership, resource
pools shared across projects, scheduling and snapshots, custom fields, baseline
profiles, CCPM/layout/other MPO extensions, operation journal and applied IDs,
import/merge, Undo/Redo and recovery; inspect each captured MPO entry and trace
its backing mutable objects and write routes. Treat generated declarations as
candidates and classify each by persistent state, owner, writer routes, and
whether it is detached/import-only. Do not use the count as an enforcement gate.

Known examples: `Task.setName` and `Task.setNotes` mutate directly without a
mutation scope; `NormalTask.setDuration` uses `runAtomicModelCommand`, but its
rollback snapshot and calculation-state capture currently happen before that
command acquires the coordinator. `ProjectMutationState` revisions therefore
cannot certify a coherent MPO snapshot. MPO serialization reads MSPDI and its
extension entries separately, while `embeddedProjectsFor` performs disk reads.

Implement and prove these stages in order:

1. Define owner tracking for attached and detached tasks, assignments, outlines,
   calendars and shared pools, including attach/detach, import, shared ownership,
   Undo and linked branches. A detached construction or read-only operation must
   not advance a live project revision; one actual committed command must advance
   the relevant owner generation once. Avoid linear membership scans on hot paths.
2. Define a typed mutation boundary around each complete mutation, including its
   precondition/rollback capture and all indirect changes. Route setters and
   command, scheduler, import/merge and Undo paths through it; group nested writes
   and publish events after release. Test reentrancy, rollback, shared owners,
   concurrent access, and import/detached cases. Mechanically inventory writer
   candidates in CI with explicit reviewed classifications; fail on a new or
   changed unclassified writer. Include direct fields and collection mutations
   with a suitable structural analysis, not only the declaration regex above.
3. Once every state read by MPO and recovery is covered, capture the complete
   branch and reserve journal operations under the same boundary. The result must
   be a complete detached in-memory representation, including all extensions,
   linked documents and pool state; never read files or wait for EDT work under
   the barrier. Measure bounded capture latency and ensure UI remains responsive.
   Do filesystem, compression and archive replacement only after release.
4. Connect ordinary save, recovery and merge to the same capture/reservation and
   commit protocol. Validate same-field conflicts, later edits before and after
   replacement, first save, failed replacement, retry and simultaneous saves;
   preserve operation IDs and applied IDs. A changed generation cannot be
   reported as fully saved or overwritten by a stale remote apply. Enable the
   new path only after the complete writer coverage and these tests pass.

A partial revision guard or a locked MSPDI-only copy does not satisfy this gate.
