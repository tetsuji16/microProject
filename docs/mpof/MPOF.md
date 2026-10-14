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
- `ccpm/history.jsonl` — persisted CCPM status observations and audit retractions, one JSON object per line.
  Observation entries use `kind="observation"` and stable `id`; retractions use
  `kind="retraction"`, refer to that id, and retain the actor, time, and required reason.
  Older id-less observation entries remain readable and are assigned a deterministic id on import.
- unknown extra entries are preserved verbatim on round-trip

## Version policy

`formatVersion` is `major.minor`:

- `ccpm/history.jsonl` is optional project metadata. Older `.mpo` files without it
  remain readable and import with empty CCPM history; current writes include the entry.
- A format change may update `formatVersion` and all checked-in samples/fixtures together.
- Missing or malformed `formatVersion`, an unsupported major version, or malformed
  present CCPM history is rejected.

## Evolving the format

The supported-version policy is centralized in `MpoFormatVersion`. The reader accepts
all minor revisions in the supported major version, including files from earlier
microProject releases. When this format changes, update the schema, checked-in samples,
fixtures, and documentation together without dropping reads for supported earlier MPO
files. A future incompatible major version needs an explicit reader or migration path.

Saving always rewrites the whole container at the version this build writes
(`formatVersion="1.0"`), so opening an older file and saving it upgrades it to the
current layout. One canonical check
(`MpoFileImporter.requireReadableFormatVersion`) is applied by both the current XML
manifest reader and the legacy JSON draft manifest reader.

Entry schemas and names may evolve; readers preserve unknown extra entries and
write only the version they explicitly support. This does not narrow the
supported earlier MPO reader-compatibility policy above.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and validation coverage).
