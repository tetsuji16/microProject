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
- `layout.json` — optional task-sheet field IDs, widths, and manual-width flags
- `microproject/visibility.json` — hidden task identities (optional)
- `microproject/task-splits.json` — manual/generated split contours and optional manual-leveling date provenance
- `microproject/task-leveling-options.json` — per-task split and assignment leveling switches
- `microproject/task-text-styles.json` — per-task font family, size, color, and style flags
- `microproject/task-schedules.json` — project/task calendar definitions and schedule fallback snapshots
- `microproject/baseline-profiles.json` — baseline task/assignment profiles and fixed-cost calendars
- `microproject/assignment-leveling-splits.json` — generated assignment split ownership and source-contour provenance
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

## Observed application and payload generations

The `formatVersion="1.0"` container generation is distinct from
`meta.xml`'s `applicationVersion`. MPOF first appears in the checked-in
previous-release cohort at application `0.0.23` (source commit
`b1141909de51d1a4841aa651ad4769d6dde63450`). The fixed-schedule archive and
the representative tagged-writer archive add source evidence for application
`0.0.24` (`v0.0.24.1265`, source `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`).
The earlier released raw-row baseline-cost probe was written at
`144675b75`; it remains a separate migration probe, not a general schedule
fixture. These cohorts do not prove every patch release or build combination.

Payload entries also have independent schema generations. The task split v1
record gained manual and leveling interval ownership and manual-leveling dates
in `e3ca33cbf`; records without provenance treat existing intervals as manual.
The task schedule v1 sidecar, introduced by `abb392ba4`, stores project/task
calendar definitions and snapshots; when absent, the MSPDI import supplies
calendar and schedule fallback. Assignment-leveling split v1 was upgraded to
v2 with `sourceContourMatchesGenerated` by `ab2f63ce5`; v1 remains readable
without that marker. Baseline profiles first appear at `8967bcc18`; task text
styles at `e48cfff0d` with validation hardened at `03cadf604`. The v0.0.24.1265
fixture exercises the non-empty layout, visibility, split, leveling-options,
text-style, schedule, and baseline-profile entries. It carries an empty v2
assignment-leveling entry; non-empty v2 contour provenance is covered by the
current synthetic round-trip/version probes, not by that historical cohort.

`MpoHistoricalFixtureTest.missingTaskScheduleEntryAndCalendarFieldsFallBackToMspdi`
is explicitly synthetic: it checks both a missing task-schedules entry and a
v1-shaped entry whose schedule values remain while project/task calendar
definitions are removed. It does not relabel the manipulated archive as a
historical writer output. The current test matrix also covers split provenance
fallback and assignment-leveling v1 compatibility.
