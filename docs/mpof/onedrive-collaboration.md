# OneDrive shared-folder collaboration

microProject collaboration works with a OneDrive shared folder through the
OneDrive sync client. Put the `.mpo` file and its adjacent
`.projectlibre-sync.json` collaboration sidecar in the same shared folder, and have every
editor add that shared folder to OneDrive and wait until both files show as
synced before opening the project.

Each metadata read/modify/write also uses a stable
`<base>.projectlibre-sync.lock` file in the same folder as a short-lived
inter-process transaction lock. The file name is retained as a compatibility
identifier for existing shared folders; it is not a project-file lock and its
presence does not indicate that a process is currently writing. The active
`FileChannel`/operating-system lock is released when the writer exits, while
the empty marker file itself may remain after a normal or abnormal exit. This
serializes metadata updates before the atomic replacement of the `.mpo` file.

Do not delete or rename this lock file manually while another editor may be
open. The `projectlibre` portion is a legacy on-disk compatibility name; a
future migration may introduce a microProject-branded name only together with
an explicit mixed-version locking and sidecar migration protocol.

## Temporary files and cleanup contract

The sidecar writer creates a uniquely named temporary file in the same
directory as the sidecar (for example, `.projectlibre-sync.json.<random>.tmp`).
The file is fully written and flushed before it is atomically moved over the
sidecar. It is an implementation artifact, not a collaboration record. A
writer removes it in its `finally` path when the move fails; if Windows
Defender, OneDrive, or another process still has an open handle, removal may
be deferred and the next maintenance/startup cleanup may retry it.

The `.projectlibre-sync.lock` file is also an implementation artifact. Its
presence does **not** mean that a writer is running: the authoritative lock is
the active operating-system `FileLock` held on its open channel. The marker
file is normally not deleted after a successful transaction, normal shutdown,
or crash, because deleting it would introduce a create/delete race for other
editors and would make cloud-sync ordering less predictable. An empty or old
marker is therefore harmless.

Do not include `.tmp` files in conflict resolution or copy them between
machines. Do not delete `.lock` files as ordinary project cleanup. Removing a
sidecar or retiring a shared project is the only maintenance operation that
may remove the marker, and only after confirming that no active lease remains
and that acquiring the OS `FileLock` succeeds. If either check cannot be made
(for example, because OneDrive is offline), leave the marker and retry later.
Never infer writer liveness from file existence or timestamps alone.

Project deletion and "stop sharing" operations must treat the `.mpo`, sidecar,
and lock marker as one logical set. They should first stop/expire the local
leases, then acquire and release the OS lock, and only then remove the sidecar
and marker. A failed deletion must be reported and retried; it must not be
silently replaced by an empty sidecar.

The application uses `CollaborationMetadataStore.removeArtifactsIfUnowned()`
for the explicit **Delete project** and **Stop sharing** actions. It parses the
complete sidecar, rejects active leases, acquires and releases the stable OS
lock, and only then removes the sidecar and marker. A partial or malformed
sidecar returns `false` and is never replaced or deleted.

The JVM-local lock registry used to coordinate multiple `CollaborationMetadataStore`
instances is process-scoped and intentionally never evicts entries today.
This avoids a subtle race where removing a lock object while another thread is
waiting could create two monitors for the same canonical path. Long-lived
processes that open an unbounded number of distinct shared paths can therefore
retain registry keys. A reference-counted acquire/release registry (with
removal only after the last waiter leaves), plus a bounded maintenance policy,
is tracked separately in [issue #546](https://github.com/tetsuji16/ProjectLibre/issues/546);
do not implement naive `remove(key)` eviction.

The exchange regression suite also starts two saves simultaneously against one
shared file and verifies that both editors' task changes survive. This exercises
the same lock/merge path used by a OneDrive-synchronized folder; OneDrive
remains the file transport and synchronization service.

The adjacent `.projectlibre-sync.json` sidecar contains per-task leases and each editor renews its leases while
the project is open. An editor cannot acquire a task that has an active lease
from another editor. Sequential OneDrive-sync saves of independent task edits
merge their MPOF task-operation logs, so both edits survive the later save.

When a task with a local lease changes externally, the application does not
overwrite the shared MPOF. It offers **Save Copy** or **Cancel** so that both
versions remain recoverable. Reload the shared file after the other editor has
finished, then apply the saved copy intentionally.

This is a shared-folder protocol; it needs no OneDrive account token or Graph
application registration. Direct Microsoft Graph synchronization, conflict-copy
discovery, and conflict presentation are tracked in issue #321. The current
operation-log merge is limited to synchronized MPOF files and deterministic
same-entity ordering; it is not a Microsoft Graph synchronization service.
