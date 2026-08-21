# ADR-0001: podx 0.1 container and collaboration model

Status: accepted for podx 0.1

## Decision

podx is a UTF-8 ZIP container with an MSPDI `project.xml` snapshot and a JSON
manifest. The snapshot is the interoperability boundary; CCPM and collaboration
are optional extensions. Unknown safe extension entries are preserved byte-for-
byte by a conforming editor.

Collaboration uses an append-only, UUID-addressed operation log. Operations carry
an actor UUID, a monotonic actor-local sequence, and causal parent operation IDs.
Readers sort ready operations deterministically by sequence, actor, and operation
ID. Concurrent overlapping fields are retained as conflict records rather than
silently discarded. Missing causal parents remain pending and are bounded; a
writer refuses an over-limit log.

The manifest `documentId` is the stable collaboration identity. External merges
must validate the project checksum and require the manifest document ID to match
the operation-log document ID. Writers use a temporary file followed by atomic
replacement where supported.

## Alternatives considered

- CRDT: rejected for 0.1 because task hierarchy, scheduling, dependencies, and
  resource assignments need domain-specific invariants that a generic CRDT does
  not provide.
- Last-writer-wins snapshot replacement: rejected because independent edits are
  lost and OneDrive conflict copies cannot be reconciled deterministically.
- Java serialization: rejected because it is implementation-specific and unsafe
  as an interoperability boundary; legacy `.pod` remains read/write compatible
  but is not extended with podx metadata.

## Compatibility and security

Unknown optional fields and extension entries are forward-compatible. Unsupported
required major versions, duplicate required entries, invalid JSON, unsafe paths,
checksum mismatches, and archive size limits are hard failures. The v0.1 schema,
operation contract, and conformance tests in this directory are implementation-
independent and MIT licensed.
