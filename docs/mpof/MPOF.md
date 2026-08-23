# MPOF — MicroProject Open Format (`.mpo`)

MPOF is the open, ODF-style project container for microProject.

## Container layout (draft, currently version 1.0)

- `mimetype` — first entry, stored uncompressed; content `application/vnd.microproject.openproject`
- `content.xml` — MSPDI project snapshot
- `meta.xml` — XML author, generator, and application version metadata
- `META-INF/manifest.xml` — XML manifest with `format="mpof"`, `formatVersion="1.0"`,
  the `content.xml` entry, and its SHA-256 checksum
- `operations/log.jsonl` + `changes/task-identities.json` — collaboration operation log, one validated JSON object per line
- `settings.xml` — CCPM settings and baseline (optional)
- unknown extra entries are preserved verbatim on round-trip

This layout is a draft and is not a stability promise. Future revisions may change
entry schemas or names; readers should reject unsupported `formatVersion` values,
preserve unknown extra entries, and write only the version they explicitly support.

## Migration

- Save and open operations currently accept this draft MPOF layout (`.mpo`) only.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and validation coverage).
