# MPOF (`.mpo`) reference artifacts

This directory is the public, implementation-independent MPOF reference
documentation. The format specification is [`MPOF.md`](MPOF.md). The
repository's `MpoFileImporterTest` and `OperationLogTest` are the executable
conformance suite for checksum validation,
round trips, unknown extensions, deterministic merges, conflicts, pending
operations, applied-operation generations, and idempotent application.
The suite also covers `changes/task-identities.json`, which keeps negative or
generated POD task IDs resolvable after MSPDI snapshot reload.

All artifacts in this directory are MIT licensed and may be used without
depending on microProject implementation classes.
