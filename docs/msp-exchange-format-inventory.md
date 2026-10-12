# Exchange format inventory

**Reviewed:** 2026-10-12 against the current shared working tree. This records implemented code paths and
their current verification scope; it is not a claim of full Microsoft Project
interoperability.

## Reader and writer paths

| Format | Reader | Writer | Current contract and evidence |
|---|---|---|---|
| MPP | MPXJ `MPPReader` via `MspImporter` | None registered or called in the exchange module | Import is supported by the code path. Export is explicitly unsupported; `ProjectWriterFactory` rejects `.mpp`. Do not relabel MSPDI XML as MPP. `New Product.mpp` and sample MPPs are present, but there is no checked-in semantic manifest or MPP export round trip yet. |
| MPX | MPXJ `MPXReader` via `MspImporter` | MPXJ `MPXWriter` via `ProjectWriterFactory` | Reader and writer exist. Import/export semantic coverage is partial and must be reported by field; no declaration of reverse-round-trip equivalence. |
| Microsoft Project XML / MSPDI | MPXJ `MSPDIReader` through `MspImporter`; `.xml` also has the established `.pod` fallback path | `MSPDISerializer` → MPXJ `MSPDIWriter` | Primary external exchange path. Task baseline dates and duration (slots 0–10) now transfer in both directions. Assignment baseline/timephased data and work contours remain unsupported/lossy. Existing tests exercise selected task identity, links, scheduling and tracking cases. |
| POD (legacy) | `LocalFileImporter`: frozen Java object stream first, then legacy embedded/plain XML recovery | Existing `LocalFileImporter` implementation can still write frozen POD payloads | Target contract is import-only: preserve wire layout and fixed `.pod` import fixtures, add no new write behavior. Current chooser and writer still expose POD save; this is a source/target-policy discrepancy for follow-up, not a reason to expand POD scope. This remains distinct from ordinary MSPDI XML despite the fallback reader. |
| MPO (`.mpo`) | `MpoFileImporter` archive validation/extraction, with project content decoded through `MicrosoftImporter` | `MpoFileImporter` archive/container writer | Authoritative native format and default save target (`FileHelper.DEFAULT_FILE_EXTENSION`); backward-read files from earlier supported MPO versions. Its versioned container, merge, checksum, recovery and transaction behavior have dedicated tests. Do not substitute an XML-only export for MPO. |
| XLSX | `ProjectLibreXlsxReader`; native payload first, MSPDI sheet fallback through MPXJ | `ProjectLibreXlsxWriter`; native project payload plus MSPDI worksheet | ProjectLibre XLSX has a lossless native payload for its own round trip and MSPDI fallback for external workbook interpretation. `XlsxSupportTest` covers malformed/oversized inputs and representative round trips. |
| GNOME Planner (`.planner`) | MPXJ `PlannerReader` via `MspImporter` | MPXJ `PlannerWriter` via factory | This is GNOME Planner file exchange, not Microsoft Planner. Reader/writer classes are wired. Semantic round-trip manifest not yet established. |
| PMXML | No reader path found in `MspImporter` | MPXJ `PrimaveraPMFileWriter` via factory | Export-only writer registration; import is not supported by the current importer. |
| JSON | No reader path found in `MspImporter` | MPXJ `JsonWriter` via factory | Export-only writer registration; this is not the MPO container JSON. |
| SDEF | No reader path found in `MspImporter` | MPXJ `SDEFWriter` via factory | Export-only writer registration; import is not supported by the current importer. |

The desktop Open chooser advertises native MPO plus legacy POD and external MPP,
MPX, XML, XLSX and GNOME Planner. The Save chooser currently advertises MPO and
POD plus XML and XLSX; MPO is selected as the default native save format. Lower
level writer factory registrations for MPX, PMXML, Planner, JSON and SDEF are
separate from the desktop Save UI and do not imply that the UI advertises them.

## MPP write feasibility

The application pins MPXJ `11.5.4` in `gradle/libs.versions.toml`. MPXJ's
official [supported formats](https://www.mpxj.org/supported-formats/) page
describes MPP access as read-only, and its [FAQ](https://www.mpxj.org/faq/)
says it cannot currently write MPP. The official [11.5.4 change
record](https://www.mpxj.org/changes-report.html) lists that release's changes;
it adds no MPP writer. In this repository, `MPPReader` is used but no
`MPPWriter` is imported, constructed, registered, or called. The checked-in
`ProjectWriterFactory` exposes MPX, XML, PMXML, Planner, JSON, SDEF and XLSX
only, and its unknown-extension behavior throws `IllegalArgumentException`.
MPP export is unsupported in both the pinned library and product path. A future
writer would require a different genuine-MPP implementation and independent
Project desktop open/reopen and semantic checks before being advertised.
`ProjectWriterFactoryTest` locks the current failure contract and ensures `.mpp`
cannot silently select another writer.

## Current semantic coverage and known losses

- MPP read: implementation path exists; fixture and independent manifest
  coverage are incomplete.
- MPP write: unsupported; no MPP writer or writer registration exists.
- XML: import/export supports task baselines 0–10 for start, finish, and
  duration. Current schedule slot 11 is preserved separately. A generated
  MSPDI round trip with `MinutesPerDay=480` changes a two elapsed-day baseline
  to six working days in MPXJ 11.5.4; elapsed-unit preservation through XML is
  therefore unverified and currently lossy. A zero-duration milestone retains
  its equal start/finish, while MPXJ omits an explicit zero duration element.
  Task baseline assignment work/timephased distributions and contour details
  are not mapped.
- Calendar date-only exception and dated work-week fields map by calendar day,
  independent of UTC, Tokyo, and Mexico City JVM defaults. The contract is
  tested against a hand-authored MSPDI date fixture and MPXJ 11.5.4's
  host-local `DateHelper.getDayStartDate` behavior.
- MPX: both directions are wired, but broad semantic preservation is unverified.
- XLSX: ProjectLibre-native payload supports a native round trip. The MSPDI
  worksheet fallback follows XML/MSPDI limitations.
- GNOME Planner, PMXML, JSON and SDEF: writer presence is not evidence of semantic
  completeness; reader support is absent for PMXML, JSON and SDEF.
- MPO: the established native archive/default save format; retain backward-read
  coverage and its fixed compatibility fixtures.
- POD: legacy import-only target with frozen serialization; preserve fixed import
  fixtures. The current POD save chooser/writer is a discrepancy to resolve
  separately; do not use it to broaden the target contract. External-format tests
  must not replace native MPO or legacy POD fixtures.
