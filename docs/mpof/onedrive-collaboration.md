# OneDrive shared-folder collaboration

microProject collaboration works with a OneDrive shared folder through the
OneDrive sync client. Put the `.mpo` file and its adjacent
`.projectlibre-sync.json` collaboration sidecar in the same shared folder, and have every
editor add that shared folder to OneDrive and wait until both files show as
synced before opening the project.

Each save also uses a stable `<project>.mpo.lock` sidecar as a short-lived
inter-process transaction lock. Keep it in the same folder; it is safe to
remain after a crash because the operating-system lock is released when the
writer exits. This serializes read/merge/write saves from multiple desktop
processes before the atomic replacement of the `.mpo` file.

The exchange regression suite also starts two saves simultaneously against one
shared file and verifies that both editors' task changes survive. This exercises
the same lock/merge path used by a OneDrive-synchronized folder; OneDrive
remains the file transport and synchronization service.

The sidecar contains per-task leases and each editor renews its leases while
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
