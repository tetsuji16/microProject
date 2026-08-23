# MPOF — MicroProject Open Format (`.mpo`)

MPOF is the open, ODF-style project container introduced by issue #344, superseding
the retired `podx` 0.x format (issue #317).

## Container layout (v1.0)

- `mimetype` — first entry, stored uncompressed; content `application/vnd.microproject.openproject`
- `content.xml` — MSPDI project snapshot (successor of podx `project.xml`)
- `META-INF/manifest.xml` — manifest JSON document with mandatory `formatVersion: 1.0`,
  `format: mpof`, and the snapshot SHA-256 checksum (successor of podx `manifest.json`)
- `changes/operations.json` + `changes/task-identities.json` — collaboration operation log (carried over)
- `ccpm.json` — CCPM settings and baseline (optional, carried over)
- unknown extra entries are preserved verbatim on round-trip

## Migration

- `.podx` save support is removed; saving always writes MPOF v1.0.
- Reading `.podx` remains supported read-only during the migration window; such files
  are upgraded automatically on the next save.
- Historical podx 0.1 documents are kept in this directory for reference:
  `v0.1.md`, `operation-log-v0.1.md`, `adr-0001-container-and-collaboration.md`,
  `schemas/` (legacy JSON schemas), and `examples/minimal`.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and legacy-read coverage).
