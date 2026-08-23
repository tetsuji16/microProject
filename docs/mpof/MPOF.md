# MPOF — MicroProject Open Format (`.mpo`)

MPOF is the open, ODF-style project container for microProject.

## Container layout (v1.0)

- `mimetype` — first entry, stored uncompressed; content `application/vnd.microproject.openproject`
- `content.xml` — MSPDI project snapshot
- `META-INF/manifest.xml` — manifest JSON document with mandatory `formatVersion: 1.0`,
  `format: mpof`, and the snapshot SHA-256 checksum
- `changes/operations.json` + `changes/task-identities.json` — collaboration operation log (carried over)
- `ccpm.json` — CCPM settings and baseline (optional, carried over)
- unknown extra entries are preserved verbatim on round-trip

## Migration

- Save and open operations accept MPOF v1.0 (`.mpo`) only.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and validation coverage).
