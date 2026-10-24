# Historical MPO import fixtures

These fixtures exercise the current reader against MPOF 1.0 archives written
by the previous supported source revision. Their manifests describe the literal
writer-side project and the corresponding MSPDI `content.xml` fields. The
historical reader's recalculated dates are not used as expected values.

## CCPM sample

`ccpm-sample-previous-release.mpo` is the unchanged `samples/CCPM sample
English.mpo` blob from microProject commit
[`b1141909de51d1a4841aa651ad4769d6dde63450`](https://github.com/tetsuji16/ProjectLibre/commit/b1141909de51d1a4841aa651ad4769d6dde63450),
dated 2026-08-31. Its metadata records `applicationVersion="0.0.23"` and MPOF
`formatVersion="1.0"`; it is a prior-release project from the repository, not
a synthetically relabeled archive. SHA-256:
`27cbe46398364321bd927817fa001888c523403492e05ecf18b3f5059b387266`.

The compact source manifest below is read from its raw `content.xml`:

| Item | Source values |
|---|---|
| Project | `CCPM Visualization Sample`; 8 tasks |
| Task UID 18, `Phase 2: Design and Verification` | ID 2; raw duration PT34H; start `2026-05-28T11:00:00`; finish `2026-05-31T04:30:00`; task percent complete 25%; calendar UID -1 |
| Assignment for Task UID 18 | resource UID 1; work 34h; actual work 32h; remaining work 2h; units 1.0 |
| Task UID 85, `Phase 8: Design and Verification` | start `2026-05-29T02:00:00`; finish `2026-05-29T11:00:00`; duration PT8H; ASAP constraint; calendar UID -1 |
| Assignment for Task UID 85 | resource UID 1; work 32h; actual work 4h; remaining work 28h; units 1.0 |
| Task UID 21, `Phase 5: Design and Verification` | predecessor Task UID 20, `Phase 4: Design and Verification`; dependency type 1 (FS); lag 0 |

The test checks Task UID 18's raw duration type and value, progress, assignment
work and units, and the UID 21 dependency after import and current MPO save and
reopen. It intentionally does not assert this fixture's derived schedule. A
bounded UTC/en-US comparison of this one fixture against the reader at commit
`b1141909de51d1a4841aa651ad4769d6dde63450` found Task UID 18's source and
assignment values were not retained by that old reader; UID 85 has explicit
source dates, but the old reader placed it on the probe date (2026-10-14). The
old importer's `updateEarliestTaskStart(task)` read the schedule getter rather
than the MPXJ source start, allowing the default project date to anchor the
recalculation. This is a historical reader defect, not a date oracle or a
missing-date source case. Current schedule outputs therefore remain outside
this fixture's assertions.

The source calendar list also contains ambiguous duplicate UID 2 entries:
`標準[Imported]`, `未割当`, and `tetsu` (among other duplicated IDs). For this
legacy shape the importer keeps its established standard/base calendar choice
instead of guessing by calendar object or name. This is a bounded old-MPO
compatibility decision, not a claim that duplicate UIDs conform to MSPDI or a
measured Microsoft Project behavior. The supported selected-field assertions
above remain active.

## Fixed schedule written by the previous source revision

`fixed-schedule-previous-writer.mpo` was written with the old
`MpoFileImporter.saveProject(Project, OutputStream)` implementation at commit
`b1141909de51d1a4841aa651ad4769d6dde63450`. Its metadata records
`applicationVersion="0.0.23"`; the archive declares MPOF `formatVersion="1.0"`.
It uses a simple project authored with the old model API, rather than the
CCPM sample's duplicate calendar UIDs and already-inconsistent assignment
values. SHA-256:
`2b8e492009edbf779122495f15959f8e1ad86d39a07192f4f8ccec6bce42e996`.

The independent source manifest is the pre-write model state, checked before
calling the old writer, and matches the archive's raw `content.xml`:

| Item | Literal source values |
|---|---|
| Project | `Fixed schedule native fixture`; start `2030-06-03T08:00:00`; base calendar UID 42 |
| Calendar UID 42 | Monday–Friday 08:00–12:00 and 13:00–17:00; no weekend work |
| Resource UID 7 | `Fixture resource`; maximum units 0.5; inherits project base calendar UID 42 |
| Task UID 4201, ID 1 | `Predecessor — 8h`; Monday 08:00–17:00; PT8H; 0%; SNET constraint at Monday 08:00; calendar UID 42; auto-scheduled |
| Assignment for Task UID 4201 | resource UID 7; work 8h; actual work 0h; remaining work 8h; units 1.0 |
| Task UID 4202, ID 2 | `Successor — partial progress`; Tuesday 08:00–17:00; PT8H; 25%; SNET constraint at Tuesday 08:00; calendar UID 42; auto-scheduled |
| Assignment for Task UID 4202 | resource UID 7; work 8h; actual work 2h; remaining work 6h; units 1.0 |
| Dependency | Task UID 4201 to Task UID 4202; FS; zero lag |

Dates in this table are floating MSPDI wall times. The regression test checks
those literal model-wall dates, durations, task progress and constraints,
calendar identities, resource identity, assignment quantity/work fields, and
dependency after the current reader imports this old-writer archive and again
after current MPO save and reopen. Expected schedule dates come from the
pre-write project model and its explicit calendar intervals, not from current
reader output. The resource omits a separate calendar reference and therefore
inherits the project calendar; the project and both tasks explicitly reference
calendar UID 42.

To reproduce the archive, use JDK 25 with the old source revision above and
copy `fixed-schedule-previous-writer-generator.java.txt` to
`modules/micrproject_exchange/src/test/java/com/microproject/exchange/FixedSchedulePreviousWriterGeneratorTest.java`.
Run `:micrproject_exchange:test --tests
com.microproject.exchange.FixedSchedulePreviousWriterGeneratorTest` with
`TZ=UTC` and
`JAVA_TOOL_OPTIONS=-Duser.timezone=UTC -Duser.language=en -Duser.country=US` so
the generated floating MSPDI timestamps match the manifest. The source includes
the old model construction calls and pre-write literal assertions; it writes to
the path in `fixed.schedule.fixture.output` (defaulting to a temporary file).
Remove the temporary test after generation. ZIP entry timestamps and generated
metadata may vary, so the SHA-256 above identifies only the checked-in archive;
reproduction is validated by its literal model and XML semantics, not by a
claim of byte-identical regenerated archives.

Both assignments deliberately retain units 1.0 while the resource maximum is
0.5. This models an overallocated assignment and checks that import preserves
the saved assignment value without silently changing the resource capacity.
The test deliberately uses the current reader as the prior-writer contract
check. The old reader's clock-sensitive schedule and zero-unit assignment
outputs are not normative expectations. The CCPM test remains a selected-field
compatibility case and makes no claim that one fixture proves broad MPO or
Microsoft Project parity.

## Representative archive written by the tagged 0.0.24.1265 writer

`v024-1265-previous-writer.mpo` was generated by the actual source tag
`v0.0.24.1265` (`8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`) in an isolated
`git archive` source tree using JDK `25.0.4.101-hotspot`. The checked-in
generator source is
[`v024-1265-previous-writer-generator.java.txt`](v024-1265-previous-writer-generator.java.txt).
It asserts the literal project model before invoking that revision's own
`MpoFileImporter.saveProject`; it uses no current production classes. The test
then pins the resulting archive SHA-256
`0CBB233D8836B472F80FBC8932CAE99CC9858670A61AC1F4DC70E31B7C1DFDD8`,
`content.xml` SHA-256
`DDB6A49BD2F7D5CA2E6432FE29B5711D08E8DA406EDB43664E9E4C29C1E34A7A`,
`applicationVersion="0.0.24"`, and raw ZIP entry order. The generator ran with
UTC, English locale, and an explicit output path; ZIP bytes from a separate
run need not match because archive metadata can vary.

The independent pre-write/current-reader manifest covers the fixed project,
calendar, task dates, durations, SNET constraints, progress, dependency,
resource and assignment work already listed above. This generated archive
also has non-empty layout widths/manual flags; hidden task UID 4201; task UID
4202 manual split offsets 14,400,000–18,000,000 ms; disabled task split and
assignment leveling options; Dialog 15pt `0x2457A6` bold/italic/strikethrough
text style; and baseline slot 0 with resource UID 7 and 28,800,000 ms profile
duration. Its task schedule payload has the literal project/task calendar
definitions. `MpoHistoricalFixtureTest` asserts these raw values and checks
the corresponding current model after import and current MPO save/reopen. The
assignment-leveling-splits entry is present as version 2 with no assignments;
this fixture does not prove a non-empty v2 generated assignment-split state.
No claim is made for native fields the fixture does not populate.

To reproduce from a clean checkout of the tag, copy the generator text file
to `modules/microproject_exchange/src/test/java/com/microproject/exchange/V024TaggedWriterGeneratorTest.java`
and run the exchange test with `JAVA_TOOL_OPTIONS` including
`-Duser.timezone=UTC -Duser.language=en -Duser.country=US` and
`-Dfixed.schedule.fixture.output=<absolute-output-path>`, followed by
`.\gradlew.bat :microproject_exchange:test --tests com.microproject.exchange.V024TaggedWriterGeneratorTest --console=plain --rerun-tasks`.
The generator's source manifest assertions run before the old writer call.

## Assignment baseline cost written by the released raw-row serializer

`legacy-assignment-baseline-cost-probe.mpo` was saved by the
`MpoFileImporter.saveProject(Project, OutputStream)` implementation at released
source commit `144675b75` (application version `0.0.24`). Its pre-write model
had one assignment baseline timephased cost of 125.0 and resource CostPerUse
7.5. The old writer emitted the custom assignment row as MSPDI
`Type=5, Value=125` directly, while MPXJ's standard resource writer emitted
`CostPerUse=750`. Archive SHA-256:
`F13568BBCAE62CFA4973D3BF471C1C5A0A2EFF6BE2DEC24FC4E122369D95D3AD`.
`content.xml` SHA-256, also recorded as the manifest `projectSha256` and
`content.xml` entry hash:
`1A1D73D4BF6CF23A6D050DED4DED4A0528D51B6F68C802DF8FBFC72A21F2B755`.

`MpoFileImporterTest.releasedMpoWriterBaselineCostFixtureKeepsItsModelCurrencyAndUpgradesOnSave`
checks the archive SHA and domain values, reads the old custom row as 125.0
through the MPO-only migration, and confirms ordinary MSPDI interprets the
same literal `Value=125` as 1.25. It also checks standard `CostPerUse=750` as
7.5, then saves and reloads with the new metadata unit marker so the old-row
migration cannot apply twice. The fixture was generated from the source model
through the released writer in a clean probe at `144675b75`; the pre-write
model values and emitted XML values are asserted by the test and recorded here.
