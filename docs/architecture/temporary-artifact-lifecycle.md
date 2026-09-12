# Temporary-artifact lifecycle

All temporary data is outside the project directory unless the target format
requires an adjacent atomic-replacement file.  A temporary artifact is
published only after its contents and manifest have been fully written.

| Artifact / owner | Location | Success cleanup | Failure cleanup | Crash / TTL recovery |
|---|---|---|---|---|
| AutoRecovery snapshot / `AutoRecoveryStore` | `%LOCALAPPDATA%\microProject\temp` | Delete after successful document save | Keep the last valid snapshot; failed partial metadata is removed or marked | Startup scan removes managed stale artifacts after 7 days |
| Update MSI staging / `UpdateChecker` | `%LOCALAPPDATA%\microProject\updates` | Delete after apply or user cancellation | Delete `.part`, installer, and manifest; retain `.delete` marker if locked | Startup retry and 7-day TTL cleanup |
| MPOF embedded extraction / `MpoExtractionSession` | `%LOCALAPPDATA%\microProject\temp\microProject-temp-mpof-*` | Delete when owning `Project`/`DocumentFrame` closes | Close session and remove tree; failed entries receive `.delete` markers | `TemporaryWorkspace` validates the manifest, skips live process owners, then retries/removes stale trees |
| POD atomic save / `LocalFileImporter` | Destination parent, target-name `.tmp` | Atomic replace, then delete staging file | Preserve destination; enqueue staging path for retry | Next save and workspace cleanup retry the marker |
| MSPDI atomic save / `MicrosoftImporter`, `MSPDISerializer` | Destination parent, target-name `.tmp` | Atomic replace, then delete staging file | Preserve destination; enqueue staging path for retry | Next save and workspace cleanup retry the marker |
| MPOF atomic save / `MpoFileImporter` | Destination parent, target-name `.tmp` | Atomic replace, then delete staging file | Preserve destination and operation state; enqueue staging path | Next save/workspace cleanup retries the marker |
| Collaboration sidecar / `CollaborationMetadataStore` | Beside MPOF (`*.projectlibre-sync.json`) | Keep sidecar; release active OS/JVM lock | Preserve last valid JSON; partial write is removed/marked | Lock marker remains; active lock is released and stale temp is retried |

Deletion is idempotent.  A `.delete` marker never authorizes deleting an
unrelated path: cleanup is restricted to a path explicitly queued by the
writer or to a manifest-validated managed workspace entry.
