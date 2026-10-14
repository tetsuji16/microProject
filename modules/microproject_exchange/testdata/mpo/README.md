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
