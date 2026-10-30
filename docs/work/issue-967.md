# Issue 967: application error with MPO format

Baseline: `cff021950` (origin/master). Work is uncommitted on branch `claude/issues-967-970`. The latest release when the issue was filed was v0.1.0.1284.

| ID | Acceptance | State and evidence |
| --- | --- | --- |
| A1 | POD, add a row, save as MPO, reopen: resources are present | Fixed by #966, merged after v0.1.0.1284. `editedPodSavedAsMpoFileKeepsResourcesAcrossResave` passes, headless, including a second save. No release contains it yet |
| A2 | Remnant files do not cause a save error | Reproduced and fixed: `Cannot merge mpo files with different document IDs`. Each POD open gets a new document ID, so a remnant `.mpo` from an earlier attempt blocked Save As. An explicit Save As now replaces it; a plain save still refuses |
| A3 | Remnant files do not cause an error on open | Not reproduced. The reporter gave no error text. Sync sidecars are not tied to a document ID, and nothing here shows a cause |
| A4 | Reporter verification on a release | Pending |

The reporter's size comparison (32 KB MPO, 1,235 KB POD) was made on a file with no resources. The size after the fix has not been measured.

Next slice: release, then ask the reporter to retest and send the exact text if an error still appears on open (A3).
