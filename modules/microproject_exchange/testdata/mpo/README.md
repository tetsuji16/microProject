# Historical MPO import fixture

`ccpm-sample-previous-release.mpo` is the unchanged `samples/CCPM sample
English.mpo` blob from microProject commit
[`b1141909de51d1a4841aa651ad4769d6dde63450`](https://github.com/tetsuji16/ProjectLibre/commit/b1141909de51d1a4841aa651ad4769d6dde63450),
dated 2026-08-31. Its metadata records `applicationVersion="0.0.23"` and MPOF
`formatVersion="1.0"`; it is a prior-release project from the repository, not a
synthetically relabeled archive. The root repository `LICENSE` is MIT.

The test's compact input manifest is taken from this fixture's raw
`content.xml`:

| Item | Expected source values |
|---|---|
| Project | `CCPM Visualization Sample`; 8 tasks |
| Task UID 18, `Phase 2: Design and Verification` | ID 2; raw duration 34h; start `2026-05-28T11:00:00`; finish `2026-05-31T04:30:00`; task percent complete 25%; calendar UID -1 |
| Assignment for Task UID 18 | resource UID 1; same start/finish; work 34h; actual work 32h; remaining work 2h; units 1.0; work complete 94% |
| Task UID 21, `Phase 5: Design and Verification` | one predecessor: Task UID 20, `Phase 4: Design and Verification`; MSPDI dependency type 1 (FS); lag 0, format 7 |

The test checks the raw task duration's `DAYS` type with `Duration.getType` and
decodes its millisecond value with `Duration.millis`. It also checks imported
task progress (the source's 25% maps to model fraction 0.25), assignment work,
actual/remaining work and units, and the dependency. It repeats those checks
after current MPO save and reopen. Dates in the table describe the source XML;
derived schedule dates and effective duration are not asserted and remain
unresolved scheduling-semantics questions. The fixture has three base
calendars with UID 2, while Task UID 18 uses calendar UID -1; the old and
current readers have not been compared on this fixture. SHA-256:
`27cbe46398364321bd927817fa001888c523403492e05ecf18b3f5059b387266`.

This is evidence that a real previous-release MPO opens and retains selected
task progress, assignment, and dependency fields. It does not claim a
format-version migration: the historical and current containers identify
themselves as MPOF 1.0. Separate manifest-version tests cover other minor
revisions and the early draft-reader contract.
