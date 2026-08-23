# podx operation log 0.1

`changes/operations.json` is the collaboration extension carried by podx. It
uses the schema in `schemas/changes-0.1.schema.json`. Each operation has a
globally unique ID, an actor ID, an actor-local sequence and explicit parent
operation IDs. Readers deduplicate by operation ID and order ready operations
by `(sequence, actorId, id)`; an operation with unknown parents remains pending.
The optional `appliedOperationIds` array is the snapshot generation marker: it
must contain exactly the causally ready operation IDs known to be applied to
`project.xml`. A reader rejects a marker that names an unknown or pending
operation; older logs without the field are interpreted using causal readiness.

The reference writer always emits this entry and preserves both ready and
causally pending operations byte-for-byte across a no-op save. It validates
UUID identifiers, operation kinds, sequence values, required arrays and strict
JSON field uniqueness before accepting a log.

This version provides the durable carrier and deterministic causal ordering.
The reference reader first applies `changes/task-identities.json` when present,
mapping legacy POD IDs to the snapshot's MSPDI UIDs, then applies task,
dependency, and assignment operations to the XML snapshot after validating
their payloads. All supported operation
kinds are idempotent when the same operation is replayed.

Writers generate task, dependency, and assignment operations by comparing each
save with the most recent podx snapshot. Readers also apply validated
`task.move` operations. A missing resource referenced by an assignment is
recreated from the operation's minimal resource metadata. Consequently,
`conflicts` records same-entity concurrent edits. The writer emits the exact
conflict set derived from the operation graph and readers reject stale or
fabricated conflict metadata; remaining resolution still follows deterministic
operation order and should be protected with leases.

The snapshot in `project.xml` is authoritative only together with the complete
operation set. Writers must preserve unresolved operations and never claim that
a partial log is complete. Version 0.1 readers which do not implement this
extension must preserve it byte-for-byte or refuse to save the document.
