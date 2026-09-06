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
