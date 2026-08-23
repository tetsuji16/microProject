# MPOF (`.mpo`) reference artifacts

This directory is the public, implementation-independent podx 0.1 reference
package. The specification is [`v0.1.md`](v0.1.md), the collaboration contract
is [`operation-log-v0.1.md`](operation-log-v0.1.md), and the design rationale is
[`adr-0001-container-and-collaboration.md`](adr-0001-container-and-collaboration.md).

The JSON schemas under `schemas/` are normative for the fields they describe.
`examples/minimal/` is an unpacked, human-readable example; replace the
`projectSha256` placeholder with the lowercase SHA-256 of its `project.xml`
when constructing a ZIP container. The repository's `PodxFileImporterTest` and
`OperationLogTest` are the executable conformance suite for checksum validation,
round trips, unknown extensions, deterministic merges, conflicts, pending
operations, applied-operation generations, and idempotent application.
The suite also covers `changes/task-identities.json`, which keeps negative or
generated POD task IDs resolvable after MSPDI snapshot reload.

All artifacts in this directory are MIT licensed and may be used without
depending on microProject implementation classes.
