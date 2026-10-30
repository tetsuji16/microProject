# Issue 970: MPO save can report success while losing an edit

Baseline: `cff021950` (origin/master). Work is uncommitted on branch `claude/issues-967-970`.

| ID | Acceptance | State and evidence |
| --- | --- | --- |
| A1 | The four `MpoTransactionBoundaryTest` cases are enabled and pass | Passed, headless, on the uncommitted tree |
| A2 | Ordinary save, recovery and merge use one capture and commit protocol | Done for single-project saves. `exportFileLocked` is the only route; recovery snapshots now always use it. Linked branches stay rejected by recovery |
| A3 | Operation IDs survive retry and failed replacement | Passed: `failedMergedReplacementKeepsArchiveModelAndReservedOperationIds` |

## Protocol

1. Capture: read all archive content from the live model into bytes, reserve the journal operations, read the content again, and compare. A difference retries, up to three times. No lock is held while reading.
2. Build and replace the archive from the capture only. Linked files are read here.
3. Verify: read the content again off the EDT. A difference throws `MpoPartialApplyException`, and the project stays unsaved.
4. Commit: on the EDT when remote operations must be applied. A cheap journal comparison runs in that same step, so a remote value cannot overwrite an edit made just before it.
5. `LocalSession` clears the unsaved state only if the same cheap comparison still passes.

Detection compares content rather than tracking writers, so it covers every setter that affects a saved file. This replaces the writer inventory proposed in `issue-946-recovery-design.md`. `<CurrentDate>` is excluded because the serializer stamps the wall clock there.

## Decisions

- `laterDifferentFieldSurvivesMergeAndRetry` used to expect the first save to succeed even though the later notes were not in the file. That is the reported defect, so it now expects `MpoPartialApplyException`, then a successful retry.
- `failedMergedReplacement...` used to expect an untouched journal, which contradicts A3. It now expects stable reserved IDs.
- Recovery snapshots are always MPO. Existing POD snapshots still open.
- The merge baseline comes from the live model, not the MSPDI copy, which can round values differently.

## Known limits

- A non-journaled edit, such as a duration, made between step 3 and step 5 can still lose its unsaved mark. The gap is one EDT queue turn. Such an edit stays in the open model, and the next save writes it.
- An edit during step 1 on a very large project can exhaust the retries and fail the save with a retryable error. The edit is not lost.
- Saves read the content two more times. Measured on one read: 145-task sample about 0.1 s, synthetic 3,145 flat tasks about 4 s, all off the EDT. Removing quadratic lookups took the large case from 5.6 s to 4.2 s.
- Multi-project recovery remains rejected, as before.

Next slice: physical GUI evidence from the Windows GUI workflow on the exact candidate SHA, then close.
