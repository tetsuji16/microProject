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
- `ccpm/history.jsonl` — persisted CCPM status observations, one JSON object per line
- unknown extra entries are preserved verbatim on round-trip

## Version policy

`formatVersion` is `major.minor`:

- This CCPM history format is the current format for this build; old `.mpo` files without
  `ccpm/history.jsonl` are not a compatibility target.
- A format change may update `formatVersion` and all checked-in samples/fixtures together.
- Missing or malformed `formatVersion`, or a missing required CCPM history entry, is rejected.

## Evolving the format

The supported-version policy is centralized in `MpoFormatVersion`. When this format
changes, update the schema, checked-in samples, fixtures, and documentation together.
No legacy reader or migration path is required for this feature.

Saving always rewrites the whole container at the version this build writes
(`formatVersion="1.0"`), so opening an older file and saving it upgrades it to the
current layout. One canonical check
(`MpoFileImporter.requireReadableFormatVersion`) is applied by both the current XML
manifest reader and the legacy JSON draft manifest reader.

This layout is a draft and is not a stability promise. Future revisions may change
entry schemas or names; readers preserve unknown extra entries and write only the
version they explicitly support.
The importer accepts only the current MPOF layout (`.mpo`) for CCPM history.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and validation coverage).
