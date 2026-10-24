# Exchange format inventory

**Reviewed:** 2026-10-25 at tested source tree `8481f4dfc`. This inventory
records each advertised direction, fixture/version evidence, and gaps; it is
not a claim of full Microsoft Project interoperability. Tests below are named
as evidence, not re-reported as run during this documentation-only slice.

For the canonical M0 status, owner, source/applicability, and remaining-gap
register, see [MSP standard command and format inventory](MSP_STANDARD_COMMAND_INVENTORY.md).

## Reader and writer paths

| Format / direction | Handler | Contract, concrete evidence/version, and remaining gap (owner) |
|---|---|---|
| MPP read | MPXJ `MPPReader` via [`MspImporter`](../modules/microproject_exchange/src/main/java/com/microproject/core/pm/exchange/MspImporter.java) | Fixed inputs: [`mpp9timephasedsegments.mpp`](../modules/microproject_exchange/testdata/mpxj/mpp9timephasedsegments.mpp) ([source/provenance README](../modules/microproject_exchange/testdata/mpxj/README.md)); [`mpp14task-from2013.mpp`](../modules/microproject_exchange/testdata/mpxj/project2013/mpp14task-from2013.mpp) ([provenance README](../modules/microproject_exchange/testdata/mpxj/project2013/README.md)); Project 2019 MPP14 files [`task-links`](../modules/microproject_exchange/testdata/mpxj/project2019/task-links-project2019-mpp14.mpp), [`task-baselines`](../modules/microproject_exchange/testdata/mpxj/project2019/task-baselines-project2019-mpp14.mpp), [`calendar-calendars`](../modules/microproject_exchange/testdata/mpxj/project2019/calendar-calendars-project2019-mpp14.mpp), and [`assignment-assignments`](../modules/microproject_exchange/testdata/mpxj/project2019/assignment-assignments-project2019-mpp14.mpp) paired with MSPDI manifests ([provenance README](../modules/microproject_exchange/testdata/mpxj/project2019/README.md)). Assertions: [`MpxExportTrackingTest`](../modules/microproject_exchange/src/test/java/com/microproject/server/data/MpxExportTrackingTest.java) methods `mpp9CustomAssignmentContourMatchesIndependentManifestAndMpoReload`, `project2019Mpp14FixturesMatchIndependentManifestAndMpoReload`, and `project2013Mpp14TaskConstraintMatchesUpstreamManifestAndMpoReload`. MPP9 contour is 10 daily intervals/55h; Project 2019 editor build is 16.0.10730.20102; Project 2013 asserts `MUST_START_ON` (2006-08-23). Claims are limited to asserted manifest fields; other editions/builds (including Microsoft 365 and Project 2024) are unverified. Owner: #917. |
| MPP write | No writer registered; [`ProjectWriterFactory`](../modules/microproject_exchange/src/main/java/com/microproject/exchange/mpxj/ProjectWriterFactory.java) rejects `.mpp` | **OUT OF SCOPE** by user decision (2026-10-12); MPO is native/default. [`ProjectWriterFactoryTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/ProjectWriterFactoryTest.java) method `mppIsNotAdvertisedOrWrittenAsAnotherFormat` fixes rejection behavior. Do not relabel MSPDI XML as MPP. |
| MPX read | MPXJ `MPXReader` via `MspImporter` | Registered reader; no fixed semantic manifest identified. Coverage unverified. Owner: #921. |
| MPX write | MPXJ `MPXWriter` via `ProjectWriterFactory` | Registered writer; no fixed semantic manifest identified. Coverage unverified. Owner: #921. |
| Microsoft Project XML / MSPDI read | MPXJ `MSPDIReader` through `MspImporter`; `.xml` also has the established `.pod` fallback path | Specific test sources: [`MpxExportTrackingTest`](../modules/microproject_exchange/src/test/java/com/microproject/server/data/MpxExportTrackingTest.java) and [`MpxCustomFieldsConverterTest`](../modules/microproject_exchange/src/test/java/com/microproject/core/pm/exchange/converters/mpx/MpxCustomFieldsConverterTest.java). Baseline scalar input is checked-in [`Assignment Baseline Scalars.xml`](../modules/microproject_exchange/testdata/Assignment%20Baseline%20Scalars.xml); elapsed-duration and timephased cases are literals/generated within tests, not separate external golden files. Broader schema/version coverage remains incomplete. Owner: #918. |
| Microsoft Project XML / MSPDI write | `MSPDISerializer` → MPXJ `MSPDIWriter` | [`MpxExportTrackingTest`](../modules/microproject_exchange/src/test/java/com/microproject/server/data/MpxExportTrackingTest.java) covers task/assignment scalar baselines 0–10. `literalMspdiElapsedBaselineDurationUsesTaskDurationFormat` and `assignmentBaselineTimephasedWorkRoundTripsThroughMpoAndMspdi` additionally assert elapsed duration, selected assignment timephased work **and cost**, zero/missing distinctions, and MPO reload. Timephased case is a literal/generated in-test document, not an external golden file; do not infer all slot/contour combinations from selected fixtures. Not general XML parity. Owner: #918. |
| POD (legacy) read | [`LocalFileImporter`](../modules/microproject_exchange/src/main/java/com/microproject/exchange/LocalFileImporter.java): frozen Java object stream first, then legacy embedded/plain XML recovery | Fixed samples: [`June_1_sample.pod`](../samples/June_1_sample.pod), [`Commercial construction project plan.pod`](../samples/Commercial%20construction%20project%20plan.pod), and [`Commercial construction project plan2.pod`](../samples/Commercial%20construction%20project%20plan2.pod). [`PodDescriptorInventoryTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/PodDescriptorInventoryTest.java) compares these descriptors with [`pod-descriptors.txt`](../modules/microproject_exchange/src/test/resources/com/microproject/exchange/pod-descriptors.txt); [`PodRoundTripTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/PodRoundTripTest.java) has fixed sample import assertions. Frozen import-only wire contract. Owner: #920. |
| POD (legacy) write | Existing `LocalFileImporter` can still write frozen POD payloads | No new write contract: frozen legacy import-only policy. Existing chooser/writer exposure conflicts with that policy; discrepancy assigned to #920. |
| MPO (`.mpo`) read | [`MpoFileImporter`](../modules/microproject_exchange/src/main/java/com/microproject/exchange/MpoFileImporter.java) archive validation/extraction; content decoded through `MicrosoftImporter` | Fixed files [`ccpm-sample-previous-release.mpo`](../modules/microproject_exchange/testdata/mpo/ccpm-sample-previous-release.mpo) (prior microProject release 0.0.23) and [`fixed-schedule-previous-writer.mpo`](../modules/microproject_exchange/testdata/mpo/fixed-schedule-previous-writer.mpo) ([fixture provenance README](../modules/microproject_exchange/testdata/mpo/README.md)). [`MpoHistoricalFixtureTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/MpoHistoricalFixtureTest.java) asserts selected historical fields and literal schedule/work after reopen. Do not infer arbitrary future minor-version support. Owner: #919. |
| MPO (`.mpo`) write | [`MpoFileImporter`](../modules/microproject_exchange/src/main/java/com/microproject/exchange/MpoFileImporter.java) archive/container writer | Authoritative native format and default save target. Current writer format version is MPO 1.0. Container/checksum/transaction and persisted column-layout/manual-flag assertions are in [`MpoFileImporterTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/MpoFileImporterTest.java), including `taskColumnLayoutSurvivesMpoSaveAndReload`. Owner: #919. |
| XLSX read | `MicroProjectXlsxReader`; native payload first, MSPDI sheet fallback through MPXJ | [`externalXlsxSummaryFallbackPreservesHierarchyUnitsAndProgress`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/XlsxSupportTest.java) asserts hierarchy, progress, units, zero/omitted units; [`microsoftAndDirectMspImportUseTheSameNativeXlsxPayload`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/XlsxSupportTest.java) asserts schema-3 native payload equivalence. These are in-test fixture constructions, not separate checked-in external golden files; no general XLSX parity claim. Owner: #921. |
| XLSX write | `MicroProjectXlsxWriter`; native payload plus MSPDI worksheet | Native microProject payload schema 3 has focused same-payload/import assertions in [`XlsxSupportTest`](../modules/microproject_exchange/src/test/java/com/microproject/exchange/XlsxSupportTest.java); fixtures are constructed in-test, not separate external golden files. No general external XLSX parity claim. Owner: #921. |
| GNOME Planner (`.planner`) read | MPXJ `PlannerReader` via `MspImporter` | GNOME Planner, not Microsoft Planner; reader is registered but no fixed semantic manifest found. Owner: #921. |
| GNOME Planner (`.planner`) write | MPXJ `PlannerWriter` via factory | Writer is registered but no fixed semantic manifest found. Owner: #921. |
| PMXML read | No reader path found in `MspImporter` | Unsupported/unverified import; not advertised by desktop Open chooser. Owner: #921 (classify). |
| PMXML write | MPXJ `PrimaveraPMFileWriter` via factory | Internal factory registration only; not advertised by desktop Save chooser and no semantic contract found. Owner: #921 (classify). |
| JSON read | No reader path found in `MspImporter` | Unsupported/unverified import; distinct from MPO container JSON and not advertised by desktop Open chooser. Owner: #921 (classify). |
| JSON write | MPXJ `JsonWriter` via factory | Internal factory registration only; not advertised by desktop Save chooser and no semantic contract found. Owner: #921 (classify). |
| SDEF read | No reader path found in `MspImporter` | Unsupported/unverified import; not advertised by desktop Open chooser. Owner: #921 (classify). |
| SDEF write | MPXJ `SDEFWriter` via factory | Internal factory registration only; not advertised by desktop Save chooser and no semantic contract found. Owner: #921 (classify). |

The desktop Open chooser advertises MPO, POD, MPP, MPX, XML, XLSX, and GNOME
Planner; the Save chooser advertises MPO, POD, XML, and XLSX. MPO is the
default/native save format. Lower-level factory registrations for MPX, PMXML,
Planner, JSON, and SDEF are not desktop-advertised contracts. In particular,
the existing POD write exposure is recorded as a policy discrepancy, not as a
new POD write guarantee.

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

- The pinned [MPXJ 11.5.4 MPPReader dispatch](https://github.com/joniles/mpxj/blob/v11.5.4/src/main/java/net/sf/mpxj/mpp/MPPReader.java#L410-L424) registers MPP/MPT/GLOBAL format families 8, 9, 12 and 14; unknown format tokens fail `INVALID_FILE`. This describes dependency decoding, not proven application semantics. Fixed semantic fixtures currently cover MPP9 and MPP14; MPP8/12 and explicit unknown-token/malformed-input failure behavior need their own application evidence in #917.
- MPP read: `MpxExportTrackingTest` compares three fixture groups with fixed
  manifests: MPP9 contour (10 daily intervals/55h), Project 2019 MPP14 (editor
  build 16.0.10730.20102; paired MSPDI manifests include links, assignment
  progress and calendar names), and Project 2013 MPP14 task constraint
  (`MUST_START_ON`, 2006-08-23). Claims are limited to asserted manifest fields;
  this is not broad MSP parity. Microsoft 365/Project 2024 editions and other
  builds remain unverified. No authoring provenance is inferred beyond the
  checked-in fixture README/header.
  Calendar-name assertions do not prove working-week/exception semantics.
  `project2019TaskBaselineSlotsMatchIndependentManifestAndMpoReload` checks
  imported MPP baseline duration/cost slots 0/1/10, but its later MPO assertions
  use the MSPDI-derived project. MPP-derived baseline persistence therefore
  remains a missing assertion owned by #917, not a passing round-trip claim.
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
  `MpxExportTrackingTest.assignmentBaselineTimephasedWorkRoundTripsThroughMpoAndMspdi`
  also asserts assignment baseline timephased work and cost (XML types 4/5 and
  17/22/23), zero/missing distinctions, cost-only baselines, and MPO reload.
  This is a bounded timephased contract, not full baseline
  interoperability. See Microsoft [Baseline
  element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016)
  and [Work element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/work-element?view=project-client-2016).
  Assignment ordering follows Microsoft's [XML Schema for the Assignments
  Element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/xml-schema-for-the-assignments-element?view=project-client-2016),
  which places assignment Baseline records after ExtendedAttribute and before
  the local custom-field sequence. Microsoft's [Type element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/type-element-multiple-parents?view=project-client-2016)
  defines the timephased Type enumeration; for this bounded mapping, work types
  are 4 for slot 0, 16 for slot 1, then +6 per slot, with adjacent cost types.
  Sources accessed 2026-10-13; these compatibility choices are document-derived,
  not results from a physical MSP run.
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
- MPX: both directions are wired; no fixed semantic manifest or end-to-end
  contract was identified. Read/write semantics and supported version are
  unverified (#921).
- XLSX: native embedded payload schema 3 supports microProject round trips.
  `XlsxSupportTest.externalXlsxSummaryFallbackPreservesHierarchyUnitsAndProgress`
  asserts hierarchy, 40% progress, 50% assignment/resource units, and zero vs
  omitted units; `microsoftAndDirectMspImportUseTheSameNativeXlsxPayload`
  asserts schema-3 payload and equivalent task/document IDs. General XLSX
  parity and other versions are unverified (#921).
- GNOME Planner: reader and factory writer are wired, but no fixed semantic
  contract/version was found (#921). This is GNOME Planner, not Microsoft
  Planner; Microsoft Planner and cloud services are outside product scope.
- PMXML, JSON and SDEF: factory writers are internal registrations, not
  advertised Save formats; no readers or semantic contracts were found. Keep
  them classified as unadvertised/unverified, not supported product promises
  (#921).
- MPO: native/default save is format version 1.0. `MpoHistoricalFixtureTest`
  verifies selected fields from prior release 0.0.23 and literal schedule/work
  from a previous writer after reopen; `MpoFileImporterTest` covers persisted
  column widths/manual flags. These fixed fixtures do not prove acceptance of
  arbitrary future minor versions (#919).
- POD: legacy import-only target with frozen serialization; descriptor and
  import assertions are in `PodDescriptorInventoryTest` and `PodRoundTripTest`.
  Existing chooser/writer exposure is a policy discrepancy assigned to #920;
  do not treat it as a write contract. External-format tests
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
- No Microsoft Project desktop application (Microsoft 365 Project or Project 2024) is installed on this host. The MSPDI behavior above is document-derived from the linked Microsoft schema references, not an empirical MSP comparison. Broader assignment baseline contour semantics remain an open interoperability gate beyond the bounded work/cost fixture contract above. MPP writing is explicitly out of scope by product decision; MPO is the native replacement.
