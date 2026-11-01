# Issue #999 — runtime identifier migration

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/999
Contract frozen: 2026-11-01
Baseline HEAD: `5f442e260`; worktree clean.

## Goal and scope

Use current runtime identifiers for new writes and migrate old preferences, configuration, recovery, and synchronization data without loss. Consolidate migration responsibility; rename in-memory thread and IME properties.

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | New installations write current identifiers only | Open | pending |
| A2 | Existing settings, recovery files and synchronization metadata migrate | Open | pending |
| A3 | Both names present and write failure cases tested | Open | pending |
| A4 | Core, application and relevant UI tests pass | Open | pending |

## Current slice

**Next slice:** specify recovery migration conflict, concurrent writer and failure handling before implementation; preserve old files on errors.
**Done when:** a migration design and deterministic tests cover both directories, failed publication and legacy snapshots without loss.
**Then:** implement remaining settings and synchronization migrations; follow the U-26 physical input gate for the IME change when desktop use is available.

## Evidence

- Baseline `AutoRecoveryStore.forUserDirectories` chooses the legacy directory when metadata exists, so it continues writing there. Existing tests explicitly characterize this behavior; migration must preserve recoverable generations and handle simultaneous canonical files.
- The issue includes IME input-path changes. `docs/agent-workflows.md` requires U-26 physical-route evidence for changes to keyboard/IME handling; the user has requested no local desktop operations during concurrent debugging.
