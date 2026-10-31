# Issue #982 — consolidate duplicated MPO/MSPDI writer and archive-budget code

State: IN PROGRESS (implementation done, awaiting integration review)
Issue: https://github.com/tetsuji16/microProject/issues/982
Contract frozen at: issue #982 as given at intake (A1, A2 below)
Baseline HEAD: `25c41fd32` (origin/master; PR #985 already merged, it did not touch these pairs)
Baseline working tree: clean worktree `microProject-w982`, branch `claude/issue-982`

## Goal and scope

Remove four duplicated pairs without changing a persisted byte or any limit:
`MpoFileImporter` calendar writers, per-task leveling/text-style frames,
`MSPDISerializer` baseline work/cost row normalisers, and the two
`MpoArchiveBudgetValidator.validate` routes.

Out of scope: raising limits, POD wire format, MPO schema changes, GUI.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | MPO and MSPDI output byte-identical before/after | pass | golden digests captured on unmodified code, unchanged after; see below |
| A2 | One limit-check implementation; over-limit tests on ZipFile and ZipInputStream routes | pass | `Accounting` in `MpoArchiveBudgetValidator`; `MpoArchiveBudgetValidatorTest` |

## Golden evidence (A1)

`MpoMspdiConsolidationGoldenTest` imports one small MSPDI project (base + derived task
calendar each with an exception, a Wednesday override, leveling options No/No, a full task
text style, assignment baselines 0/1/2/9/10 with timephased work and cost rows), writes MPO
and MSPDI, and compares SHA-256 per MPO entry (15 entries) plus the MSPDI export with digests
pinned from the unmodified writers (production code unchanged when captured).

Non-deterministic values found (the writers are NOT fully byte-deterministic):
- MSPDI `<CurrentDate>` is the export wall clock (also inside `content.xml`).
- `documentId` is a random UUID per project (manifest, `operations/log.jsonl`); the test pins
  it with `Project.setDocumentId`.
- `profileUid` (baseline-profiles.json) and the manifest `projectUniqueId` come from a process-wide
  id counter (values like -999996253), so they depend on which tests ran earlier; masked in the test.
- The manifest records the digest of `content.xml` as written (clock included), so the test
  masks `projectSha256` and the `content.xml`/`baseline-profiles.json` entry digests; `content.xml` itself is hashed with
  the clock removed. Zip entry times were stable.
Within one JVM two saves match once these are masked; across JVMs the golden passes.

## Pairs and decisions

| Pair | Decision |
|---|---|
| `writeCalendar` / `scheduleCalendarJson` | shared `writeCalendarBody`; callers keep their own header keys (`name` vs `name/uniqueId/base*`) and sources (concrete definition vs own differences) as parameters, since they differ on purpose |
| `taskLevelingOptionsJson` + `restoreTaskLevelingOptions` / `taskTextStylesJson` + `restoreTaskTextStyles` | shared `perTaskOptionsJson` and `restorePerTaskOptions`; field lists, skip predicates, and all error messages stay per kind; validate-then-lookup order preserved |
| `normalizeAssignmentBaselineWorkRows` / `...CostRows` | shared generic `normalizeAssignmentBaselineRows`; type code and value formatter are parameters |
| `validate(Path)` / `validate(byte[])` | one private `Accounting` (entry count, depth/path, decompressed total); no ratio limit exists today and none was added |

Left as is on purpose: the stream route reports zero measurements when the archive cannot be
read, the file route reports the running counters (existing behaviour, kept).

## Current slice

**Next slice:** integration review of the diff.
**Done when:** reviewer accepts; CI green on the candidate SHA.
**Then:** merge.

## Remaining blockers

- none known

## Latest evidence

- Tests/build: `:microproject_exchange:cleanTest :microproject_exchange:test` 387 pass / 0 fail (golden matches on
  consolidated code, isolated and in the full suite); `:microproject_ui:test` for 5 classes that use MpoFileImporter pass.
  Final masks were re-pinned against the stashed (unmodified) production code before comparing.
- Physical GUI/manual: not applicable (no GUI-observable change).
- CI: pending push.

CI on `96d6ed4f7` showed the first golden depended on the OS line ends and the default time zone (#989). After masking both, `origin/master` `25c41fd32` and the consolidated code produce the same digests under Asia/Tokyo and UTC.

## CI evidence

Build passed on Windows, Linux and macOS at d7ab49e1e (PR #986).
