# Issue 967: application error with MPO format

Base: `e8a4a2350` (origin/master). Latest report: v0.1.0.1290, the row inserted after opening a POD was missing after reopening the MPO, and `.lock` files stayed after closing.

| ID | Acceptance | State and evidence |
| --- | --- | --- |
| A1 | POD, add a row, save as MPO, reopen: resources are present | Fixed by #966; the reporter confirmed it on v0.1.0.1290. |
| A2 | Remnant files do not cause a save error | Fixed: a re-save with a new document ID no longer fails on old sidecars. |
| A3 | Remnant files do not cause an error on open | Not reproduced; the reporter gave no error text. Closing now removes the remnants. |
| A4 | Inserted rows survive save and reopen | Fixed; `reporterRouteFromPodToReopenedMpoKeepsTheDocumentAndLeavesOnlyProjectFiles`. Physical route GUI-MPO-SAVE-02 passed on Windows: `robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource`, GUI run 41270550783. |
| A5 | Closing a project or the application leaves only project files beside it | v0.1.0.1291 failed for the application window: the reporter found the sync sidecar and lock left, and it reproduced here. Fix under test: `ProjectCloseEndsCollaborationTest`; physical route GUI-MPO-SAVE-03, `ApplicationCloseGuiAcceptanceTest`. |
| A6 | Reporter verification on a release | Pending |

## Causes

- A blank row was dropped on reload: `MspImporter` skipped MSPDI `IsNull` rows.
- Reopened summaries and durations were wrong: restoring a snapshot rolled up from children not yet restored, and left the import's dependency date and raw duration in place.
- Saving over an existing MPO wrote a copy rebuilt from MSPDI. It lost schedule snapshots, splits, leveling options, text styles, baseline profiles, and hidden flags. `MpoFileImporter.copyOfCapture` now applies them as opening does; `savingOverAnExistingFileKeepsTheTaskDataMspdiCannotHold` fails without it.
- Lock markers and sidecars stayed because they were never removed on close.
- After that fix they still stayed when the application window closed: only the project Close route ended the collaboration session, and the earlier evidence tested only that route. `ProjectFactory.getRemoveProjectJob`, where every close route meets after the save prompt, now ends it. Cancelling the prompt keeps the session.

## Known limits

- A blank row at the end of a summary's children reopens before the next real task; blank rows after the last task are view filler and are not saved.
- MSPDI rounds summary percent complete to a whole percent.
- An older release waiting on a retired marker does not check the mark.

Next slice: release, reporter retest (A6).
