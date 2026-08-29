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

## Version compatibility

`formatVersion` is `major.minor`:

- **Same major version, any minor revision** — the file is read. Minor revisions are
  additive only, and unknown entries are preserved verbatim, so a container written by
  an older or a newer minor revision opens without data loss.
- **Different major version** — the file is rejected with an explicit
  `Unsupported MPOF format version <value>` message rather than a generic
  invalid-manifest error, because a major bump may change existing entry semantics.
- **Missing or malformed `formatVersion`** — rejected.

## Evolving the format

The supported-version policy is centralized in `MpoFormatVersion`. Additive changes
use the current major with a new minor version and retain unknown ZIP entries on a
load/save round trip. An incompatible change must introduce a new major version,
add its explicit reader/migration there, and retain the existing major reader until
the published deprecation window ends. This prevents a future specification update
from silently changing how existing `.mpo` files are interpreted.

Saving always rewrites the whole container at the version this build writes
(`formatVersion="1.0"`), so opening an older file and saving it upgrades it to the
current layout. One canonical check
(`MpoFileImporter.requireReadableFormatVersion`) is applied by both the current XML
manifest reader and the legacy JSON draft manifest reader.

This layout is a draft and is not a stability promise. Future revisions may change
entry schemas or names; readers preserve unknown extra entries and write only the
version they explicitly support.
The importer also reads the earlier MPOF draft that used a JSON manifest,
`ccpm.json`, and `changes/operations.json`; saving such a file rewrites it to the
current draft layout.

## Migration

- Save and open operations currently accept this draft MPOF layout (`.mpo`) only.

The implementation lives in `com.microproject.exchange.MpoFileImporter`
(see `MpoFileImporterTest` for round-trip and validation coverage).
