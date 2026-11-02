# Issue 967: application error with MPO format

Base: `e8a4a2350` (origin/master). Latest report: v0.1.0.1290, the row inserted after opening a POD was missing after reopening the MPO, and `.lock` files stayed after closing.

| ID | Acceptance | State and evidence |
| --- | --- | --- |
| A1 | POD, add a row, save as MPO, reopen: resources are present | Fixed by #966; the reporter confirmed it on v0.1.0.1290. |
| A2 | Remnant files do not cause a save error | Fixed: a re-save with a new document ID no longer fails on old sidecars. |
| A3 | Remnant files do not cause an error on open | Not reproduced; the reporter gave no error text. Closing now removes the remnants. |
| A4 | Inserted rows survive save and reopen | Fixed; `reporterRouteFromPodToReopenedMpoKeepsTheDocumentAndLeavesOnlyProjectFiles`. Physical route GUI-MPO-SAVE-02 passed on Windows: `robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource`, GUI run 41270550783. |
| A5 | Closing a project or the application leaves only project files beside it | v0.1.0.1291 failed for the application window: the reporter found the sync sidecar and lock left, and it reproduced here. Fix under test: `ProjectCloseEndsCollaborationTest` (window and desktop quit, busy and stuck locks, several projects), `CollaborationMetadataStoreTest.closingWaitsOutAProgramThatBrieflyHoldsTheFilesOpen`, `SaveThenCloseCollaborationTest` (dirty project, Yes, real save); physical route GUI-MPO-SAVE-03, `ApplicationCloseGuiAcceptanceTest`. |
| A6 | Reporter verification on a release | Pending |

## Causes

- A blank row was dropped on reload: `MspImporter` skipped MSPDI `IsNull` rows.
- Reopened summaries and durations were wrong: restoring a snapshot rolled up from children not yet restored, and left the import's dependency date and raw duration in place.
- Saving over an existing MPO wrote a copy rebuilt from MSPDI. It lost schedule snapshots, splits, leveling options, text styles, baseline profiles, and hidden flags. `MpoFileImporter.copyOfCapture` now applies them as opening does; `savingOverAnExistingFileKeepsTheTaskDataMspdiCannotHold` fails without it.
- Lock markers and sidecars stayed because they were never removed on close.
- After that fix they still stayed when the application window closed: only the project Close route ended the collaboration session, and the earlier evidence tested only that route. `ProjectFactory.getRemoveProjectJob`, where every close route meets after the save prompt, now ends it. Cancelling the prompt keeps the session.
- A review of that fix found the only removal attempt could lose to a brief delay: on the window's thread a lock wait stops after 300 ms, and a file another program had open ended the attempt. The session now ends on the close job's thread, and a busy lock or an open file is retried for up to 3 seconds. The limit also stops a lock nobody releases from blocking exit.
- The same review found the close route dropped the recovery snapshot and MPOF extraction before the save prompt, so Cancel left an open document without them. They are now released by the project's delete event, once the close is certain.

## Known limits

- A blank row at the end of a summary's children reopens before the next real task; blank rows after the last task are view filler and are not saved.
- MSPDI rounds summary percent complete to a whole percent.
- An older release waiting on a retired marker does not check the mark.
- A file held longer than the close wait stays until the same user next opens and closes the project; a Save As on the window's thread has the 300 ms wait. A damaged sidecar is never removed.

## 2026-11-02 reporter update: reopened MPO cannot be operated

Baseline: `51b441ce6c94714d00251360afaf3687c40d7202`, clean worktree. The reporter confirmed 1292 removes lock files, then reported that a single application instance reopening an MPO saved from POD cannot select or edit tasks. The two-instance result may instead have been a freeze. A black rectangle on resize was also reported; #946 tracks responsiveness and repaint. No shared cause has been established.

| ID | Required result | State |
| --- | --- | --- |
| A7 | Reporter POD -> Save As MPO -> application close -> fresh application -> open MPO: select and inspect a task, add/edit a task, save and reopen with changes intact | Open: physical end-to-end reproduction and diagnosis required. Existing GUI route closes only the project and compares rows; it does not restart the application or verify interaction after reopen. |
| A8 | Determine what happens with two independent instances, preserve both users' data, and communicate any edit denial; evaluate the reporter's proposed read-only behavior against the existing per-task collaboration contract | Open: behavior and cause not established. |
| A9 | Investigate reported resize rectangle and freeze with #946 evidence; verify physical interaction and rendered state after resize | Open: #1015 repaint change is merged after 1292, but reporter's failure is not proven fixed. |
| A10 | Explain the different lock files and their limits to the reporter | Open. |

Next slice: establish a physical single-instance reproduction on the current candidate using the reporter's POD and actual application restart. Capture screenshot, logs, thread dump and model/selection state upon failure before choosing a fix. Do not infer lock contention from a marker file's existence or claim a fix from a successful import alone. The two-instance policy and resize investigation follow without weakening the original acceptance criteria. Release and reporter verification remain pending.

## Baseline diagnostic attempt

- `./gradlew.bat installDist --console=plain`: passed at baseline `51b441ce6`; fresh runnable layout built.
- `.\.github\scripts\run-gui-acceptance.ps1 -Suite full -TestFilter 'com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource' -Locale en -Scale 1.0`: failed before valid product evidence as `GUI_ENVIRONMENT_CONTENDED`; Chrome overlapped the test window. Foreground evidence: `modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource-en-1-0-2069f093d8d24758a6ff2e582c987eee/`. Failure screenshot capture itself failed with a GDI+ error. No product conclusion can be drawn.
- Adversarial review: project-wide single-writer locking would contradict the existing task-lease and merge contract in `docs/mpof/onedrive-collaboration.md`. The reported selection failure is not evidence of a lease rejection. Preserve that contract while investigating.
- Hosted diagnostic run [41371871882](https://github.com/tetsuji16/microProject/actions/runs/41371871882), SHA `6a7a17ff8`, failed the new Insert assertion. Its failure screenshot shows the selected target was the pre-existing blank row (display row 2); therefore it does not establish a product defect. The fixture now asserts its target is a task and selects display row 1. Run 41371979292 was cancelled because its target assertion used the same blank row.
