# Exchange format inventory

**Reviewed:** 2026-10-16 at master `d4381676f`. This records implemented code paths and
their current verification scope; it is not a claim of full Microsoft Project
interoperability.

For the canonical M0 status, owner, source/applicability, and remaining-gap
register, see [MSP standard command and format inventory](MSP_STANDARD_COMMAND_INVENTORY.md).

## Reader and writer paths

| Format / direction | Handler | Current contract and evidence |
|---|---|---|---|
| MPP read | MPXJ `MPPReader` via `MspImporter` | Import is supported by the code path; semantics remain partial and fixture authoring provenance is incomplete. |
| MPP write | No writer registered; `ProjectWriterFactory` rejects `.mpp` | Explicitly outside product scope: MPO is the native save format. Do not relabel MSPDI XML as MPP. |
| MPX read | MPXJ `MPXReader` via `MspImporter` | Reader exists; import semantic coverage is partial and must be reported by field. |
| MPX write | MPXJ `MPXWriter` via `ProjectWriterFactory` | Writer exists; export semantic coverage is partial; no reverse-round-trip equivalence claim. |
| Microsoft Project XML / MSPDI read | MPXJ `MSPDIReader` through `MspImporter`; `.xml` also has the established `.pod` fallback path | Primary external exchange path; tests cover selected task identity, links, scheduling and tracking. |
| Microsoft Project XML / MSPDI write | `MSPDISerializer` → MPXJ `MSPDIWriter` | Task baseline dates/duration and assignment scalar Start/Finish/Work plus baseline timephased work (slots 0–10) have focused evidence. Assignment-only values do not manufacture task baselines; explicit zero and missing baseline work remain distinct. Baseline timephased cost and broader semantics remain partial. |
| POD (legacy) read | `LocalFileImporter`: frozen Java object stream first, then legacy embedded/plain XML recovery | Preserve fixed `.pod` import fixtures and wire layout; this remains distinct from ordinary MSPDI XML despite the fallback reader. |
| POD (legacy) write | Existing `LocalFileImporter` can still write frozen POD payloads | New write behavior is not in scope; current chooser/writer exposure is a source/target-policy discrepancy. |
| MPO (`.mpo`) read | `MpoFileImporter` archive validation/extraction; content decoded through `MicrosoftImporter` | Backward-read files from earlier supported MPO versions; versioned container, merge, checksum and recovery tests exist. |
| MPO (`.mpo`) write | `MpoFileImporter` archive/container writer | Authoritative native format and default save target (`FileHelper.DEFAULT_FILE_EXTENSION`); container, checksum and transaction behavior have dedicated tests. |
| XLSX read | `ProjectLibreXlsxReader`; native payload first, MSPDI sheet fallback through MPXJ | Native payload and external MSPDI fallback are distinct contracts; malformed/oversized input and representative reads are covered by `XlsxSupportTest`. |
| XLSX write | `ProjectLibreXlsxWriter`; native payload plus MSPDI worksheet | Native payload supports ProjectLibre round trips; representative output is covered by `XlsxSupportTest`. |
| GNOME Planner (`.planner`) read | MPXJ `PlannerReader` via `MspImporter` | GNOME Planner file exchange, not Microsoft Planner; semantic import coverage is not established. |
| GNOME Planner (`.planner`) write | MPXJ `PlannerWriter` via factory | Writer is wired; semantic export coverage is not established. |
| PMXML read | No reader path found in `MspImporter` | Import is not supported by the current importer. |
| PMXML write | MPXJ `PrimaveraPMFileWriter` via factory | Export-only writer registration. |
| JSON read | No reader path found in `MspImporter` | Import is not supported; this is distinct from MPO container JSON. |
| JSON write | MPXJ `JsonWriter` via factory | Export-only writer registration. |
| SDEF read | No reader path found in `MspImporter` | Import is not supported by the current importer. |
| SDEF write | MPXJ `SDEFWriter` via factory | Export-only writer registration. |

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
  edition/build provenance is recorded, and no Microsoft 365 Project or
  Project 2024 authored fixture is currently registered. The fixed-file regression test records
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
- MSPDI indexed custom fields: `ExtendedAttribute/FieldID` is the identity used
  for local task/resource values; `FieldName` and `Alias` are not persisted by
  the current model. Microsoft distinguishes literal `Value` from lookup
  `ValueGUID` ([ExtendedAttribute element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/extendedattribute-element?view=project-client-2016),
  [custom-field data](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/custom-field-data-in-xml?view=project-client-2016)).
  The importer supports mapped indexed fields represented by `CustomFieldsMapper`
  and rejects unknown or malformed IDs, missing values, lookup GUIDs,
  conflicting duplicates, and assignment-level custom values before changing
  project state. Numeric Cost/Number literals must be finite decimal values;
  unexpected parsed MPXJ runtime types and non-finite numeric or duration values
  also fail instead of being skipped or rounded into a misleading result.
  Same-value duplicates are accepted. Alias persistence and lookup-value
  resolution remain unsupported. Duration parsing remains delegated to MPXJ's
  `DurationFormat` interpretation. Literal MSPDI Task and Resource
  XML tests cover Cost, Date, Duration, Finish, Flag, Number, Start, and Text,
  including MPO save/reopen. Field type numbers follow Microsoft's
  [CFType element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/cftype-element?view=project-client-2016).
  MPXJ 11.5.4's pinned `DatatypeConverter.parseCustomFieldCurrency` /
  `printCustomFieldCurrency` path maps MSPDI currency values in hundredths; the
  literal test uses 1250/2250 for domain values 12.5/22.5. This is a pinned
  library conversion, not a general currency precision rule.
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

- `MpxCustomFieldsConverterTest` covers all eight indexed categories for task
  and resource MPXJ objects and literal MSPDI task/resource fields, then checks
  MPO save/reopen. Negative cases cover unsupported/malformed IDs, missing
  values, ValueGUID, assignment values, and conflicting duplicates. Currency
  expectations use the pinned MPXJ MSPDI hundredths conversion.
- Focused lifecycle and assignment-only XML/MPO round-trip tests passed after implementation. Full verification command `:microproject_core:test :microproject_exchange:test --no-daemon --max-workers=1 --console=plain` completed successfully on 2026-10-12 (`BUILD SUCCESSFUL`, 43s).
- Physical Windows Robot acceptance passed on 2026-10-12 with JDK 25, Japanese locale, and UI scale 1. Command: `gradlew.bat :microproject_ui:installDist :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource" -PguiTestLocale=ja -PguiTestUiScale=1 --no-daemon --max-workers=1 --console=plain`. The route imports the checked-in MPP copy, verifies cancellation and overwrite refusal preserve the source, saves MPO, reopens it, then opens a fixed MSPDI XML copy and verifies assignment baseline slots 0/9/10 survive Save As MPO and reopen. The checked-in New Product MPP currently has no assignment baseline scalars, so its round trip is not counted as scalar baseline evidence. The test report is `modules/microproject_ui/build/reports/tests/guiTest/index.html`.
- The physical test first failed because it expected ordinary Save on an XML source to prompt for Save As; XML is a supported explicit export format and ordinary Save remains associated with that XML path. A later Save As attempt timed out at the save-completion assertion; the run did not record a modal owner/title, so it is not evidence of a window-ownership defect. The corrected route uses the explicit Save As command and native MPO default, and passed.
- No Microsoft Project desktop application (Microsoft 365 Project or Project 2024) is installed on this host. The MSPDI behavior above is document-derived from the linked Microsoft schema references, not an empirical MSP comparison. Assignment baseline contours remain an open interoperability gate. MPP writing is explicitly out of scope by product decision; MPO is the native replacement.
