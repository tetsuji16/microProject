# Exchange format inventory

**Reviewed:** 2026-10-13 against the current shared working tree. This records implemented code paths and
their current verification scope; it is not a claim of full Microsoft Project
interoperability.

## Reader and writer paths

| Format | Reader | Writer | Current contract and evidence |
|---|---|---|---|
| MPP | MPXJ `MPPReader` via `MspImporter` | None registered or called in the exchange module | Import is supported by the code path. Export is explicitly outside this product's scope: MPO is the native save format and replacement. `ProjectWriterFactory` rejects `.mpp`; do not relabel MSPDI XML as MPP. Read/import semantics remain in scope. |
| MPX | MPXJ `MPXReader` via `MspImporter` | MPXJ `MPXWriter` via `ProjectWriterFactory` | Reader and writer exist. Import/export semantic coverage is partial and must be reported by field; no declaration of reverse-round-trip equivalence. |
| Microsoft Project XML / MSPDI | MPXJ `MSPDIReader` through `MspImporter`; `.xml` also has the established `.pod` fallback path | `MSPDISerializer` → MPXJ `MSPDIWriter` | Primary external exchange path. Task baseline dates and duration (slots 0–10) transfer in both directions. Assignment scalar Start/Finish/Work values and assignment baseline timephased **work** distributions transfer in slots 0–10; assignment-only values do not manufacture a task baseline. Explicit zero and missing baseline work remain distinct in XML and MPO. Assignment baseline timephased **cost** distributions and broader external semantics remain unsupported/partial. Existing tests exercise selected task identity, links, scheduling, tracking and assignment baseline work. |
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

Ordinary Save and close-time Save for import-only MPP/MPX/Planner sources now
ask for a writable target and suggest MPO. Cancelling retains the source path
and dirty state; successful saving associates the document with the MPO target.
Save As for XML, XLSX, MPP, MPX, Planner, and legacy POD sources also defaults
to MPO; the Save chooser still permits explicitly selecting XML or XLSX for
external export. MPO remains the default native format, and POD remains an
import-only format whose wire contract is frozen.

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

- MPP read: implementation path exists. `New Product.mpp` identifies itself in
  its file header as `Microsoft Project 9.0` / `MSProject.MPP9`; no editor
  edition/build provenance is recorded. The fixed-file regression test records
  one task and assignment's imported name, start, finish, work, actual work,
  remaining work, and units through MPO save/reload. This is regression
  evidence for that checked-in file, not an independent Microsoft Project
  semantic manifest or MSP parity evidence; the independent MPP-read manifest
  remains incomplete.
- MPP write: unsupported and explicitly outside product scope; MPO is the native
  save format. MPP read/import remains in scope.
- XML: import/export supports task baselines 0–10 for start, finish, and
  duration. Current schedule slot 11 is preserved separately. Literal MSPDI
  `DurationFormat=8` plus `PT48H0M0S` remains two elapsed days through XML
  import/export/reimport; this is document-derived from Microsoft's elapsed
  duration definition, not a claim about a physical MSP run. A zero-duration milestone retains
  its equal start/finish, while MPXJ omits an explicit zero duration element.
  Assignment baseline scalar Start/Finish/Work values transfer for slots 0–10,
  including assignment-only values that have no task-level baseline schedule.
  The task schedule remains absent in that case. MPXJ omits zero Work when
  writing, so the serializer restores an explicit zero Work element from the
  canonical assignment value before publishing XML. Literal XML tests cover
  missing Finish versus explicit zero Work, including a zero-only slot, and MPO
  save/reopen preserves nullable scalars. The DOM safeguard follows Microsoft's
  Assignment schema sequence: assignment ExtendedAttribute elements, Baseline
  records in ascending slot order, then local custom fields. It checks only
  direct same-namespace Work children and accepts any XML duration whose value
  is zero.
  Assignment timephased distributions and work contour details remain
  unsupported. This is a partial scalar contract, not full baseline
  interoperability. See Microsoft [Baseline
  element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016)
  and [Work element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/work-element?view=project-client-2016).
  Assignment ordering follows Microsoft's [XML Schema for the Assignments
  Element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-assignments-element?view=project-client-2016),
  which places assignment Baseline records after ExtendedAttribute and before
  the local custom-field sequence.
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
## Recent verification evidence

- Focused lifecycle and assignment-only XML/MPO round-trip tests passed after implementation. Full verification command `:microproject_core:test :microproject_exchange:test --no-daemon --max-workers=1 --console=plain` completed successfully on 2026-10-12 (`BUILD SUCCESSFUL`, 43s).
- Physical Windows Robot acceptance passed on 2026-10-12 with JDK 25, Japanese locale, and UI scale 1. Command: `gradlew.bat :microproject_ui:installDist :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource" -PguiTestLocale=ja -PguiTestUiScale=1 --no-daemon --max-workers=1 --console=plain`. The route imports the checked-in MPP copy, verifies cancellation and overwrite refusal preserve the source, saves MPO, reopens it, then opens a fixed MSPDI XML copy and verifies assignment baseline slots 0/9/10 survive Save As MPO and reopen. The checked-in New Product MPP currently has no assignment baseline scalars, so its round trip is not counted as scalar baseline evidence. The test report is `modules/microproject_ui/build/reports/tests/guiTest/index.html`.
- The physical test first failed because it expected ordinary Save on an XML source to prompt for Save As; XML is a supported explicit export format and ordinary Save remains associated with that XML path. A later Save As attempt timed out at the save-completion assertion; the run did not record a modal owner/title, so it is not evidence of a window-ownership defect. The corrected route uses the explicit Save As command and native MPO default, and passed.
- No Microsoft Project desktop application (Microsoft 365 Project or Project 2024) is installed on this host. The MSPDI behavior above is document-derived from the linked Microsoft schema references, not an empirical MSP comparison. Assignment baseline contours remain an open interoperability gate. MPP writing is explicitly out of scope by product decision; MPO is the native replacement.
