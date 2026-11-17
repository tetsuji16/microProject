# Issue #1180 — ribbon follow-ups

State: IN PROGRESS. Intake 2026-11-17; baseline `a1add999f` (isolated clean worktree).

Original contract: document Task Form, Resource Form, Task Board and Resource Plan exclusions; implement applicable report/agile tabs; add non-conflicting KeyTips; verify JA/EN at 100/125/150%; address Sheet Text Styles, Wrap Text, Drawings, Project Summary Task, Assignment Information, Network Align, Team Planner selected-task enablement, the batch-only icon test failure and reported local GUI failures. Preserve unresolved items; this packet does not narrow the issue.

| Acceptance | State / evidence |
|---|---|
| Exclusions, report/agile tabs and KeyTips | Open; audit against current #1166 implementation and Microsoft source. |
| JA/EN visual matrix | In progress: full GUI run 43999020531 on `a1add999f`. |
| Sheet Text Styles, Wrap Text, Drawings, Project Summary Task, Assignment Information, Network Align | Open; require model/route and persistence assessment. |
| Team Planner selected-task commands | Active slice; disabled without selected bars and enabled on selection. |
| Icon batch failure | Fixed in this slice: full UI batch reproduced 41 pixel differences under a dark theme and a second light-palette assertion failed; both tests assumed light defaults. With scoped light setup and restoration, the same full UI batch passed. |
| Local GUI failures | Open; verify affected routes and artifacts. |

## Active slice — Team Planner selection

Microsoft Support, [View your team's work with Team Planner](https://support.microsoft.com/en-us/project/view-your-team-s-work-with-team-planner), describes individual bar formatting and selection. The rule to disable selected-bar format commands without a selected task is a document-derived compatibility decision. Physical selection must enable Fill and Border; clearing selection or leaving the view must disable them, without mutating project data. A real Robot selection plus ribbon-state check is required.

Done when the focused Robot journey passes on Windows and relevant UI tests pass. Next: audit the remaining #1180 commands and phase coverage; do not close #1180 until every original row is satisfied.

## Icon test isolation slice

A Windows `./gradlew.bat :microproject_ui:test --rerun-tasks --console=plain` run at `d27a2f6` failed 2 of 1723 tests: the 9px expander raster comparison saw 41 changed pixels, and the baseline palette returned dark-theme `#C8CCD2` instead of light `#404040`. Both fixtures assert light colors without installing light semantic defaults. The relevant production colors intentionally adapt to dark mode; the tests now set light defaults for their light assertions and restore the previous theme. The same full UI command passed after the change, as did both focused test classes.

## Next slice — Assignment Information

Baseline `903a4ca3f`: the Task Usage and Resource Usage contextual tabs do not expose Assignment Information. The existing generic Information selection snapshots an assignment but routes it to Task Information or Resource Information, so it is not an assignment editor. Microsoft documents assignment work contour editing in [Work Contour (assignment field)](https://support.microsoft.com/en-us/project/work-contour-assignment-field) and formatted assignment notes in [Notes fields](https://support.microsoft.com/en-us/project/notes-fields). Assignment Information must address the selected assignment, preserve selection across focus changes, support the applicable assignment fields and notes, honor read-only state, and preserve changes through Undo/Redo and MPO reload. An assignment notes storage and format-conversion audit is required: the current `Assignment` class exposes no notes accessor, while task and resource notes have separate persistence routes. Do not substitute the existing Assign Resources dialog or mark the command complete on a dialog-only placeholder.

Done for this slice when the real contextual-ribbon route, assignment-specific editing, cancellation, read-only behavior, Undo/Redo, and MPO persistence have focused coverage including a physical GUI journey. Next after this slice: audit the remaining #1180 controls and contextual tabs.

Assignment Information candidate (draft, incomplete): a dedicated dialog and Task Usage/Resource Usage contextual ribbon action now target the selected assignment row. It exposes editable Units, Work Contour, and a basic rich-text Notes pane (bold/italic/underline); OK commits as one assignment-detail undo edit, Cancel discards, and read-only assignments cannot be changed. Unit tests cover editor RTF read/write, apply/Undo/Redo, read-only rejection, and the existing plain-text MPO notes behavior. A focused Robot GUI journey was added for physical assignment-row selection, ribbon invocation, edit, OK/Undo/Cancel; it has only been compiled, not run on hosted Windows. MPO reload for RTF is not satisfied.

Compatibility correction: Microsoft documents that UI Notes support rich text, while the MSPDI `<Notes>` element itself contains only a text value ([Notes fields](https://support.microsoft.com/en-us/project/notes-fields), [Notes Element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/notes-element?view=project-client-2016), [Assignment Elements and XML Structure](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/assignment-elements-and-xml-structure?view=project-client-2016)). MPXJ 16.11 `Notes` is a raw-string wrapper; this alone does not mean RTF markup is a compatible MSPDI representation. A new exporter helper now extracts readable text from RTF for MSPDI. Exact RTF therefore needs a native MPO archive extension keyed to stable serialized task/resource UIDs; this extension, strict malformed/duplicate/unknown-identity validation, and legacy-MPO fallback have not been implemented. Current RTF exact-round-trip assertions still expect MSPDI to carry RTF and conflict with this correction; those exchange tests are expected to fail until the test contract is corrected and the native MPO extension exists. External MSPDI formatting loss must be stated explicitly.

Evidence at pause: `:microproject_ui:compileJava` passed; `:microproject_ui:test --tests "com.microproject.dialog.AssignmentInformationDialogTest"` passed before the MSPDI compatibility correction; `:microproject_ui:compileGuiTestJava` passed. Before the correction, focused exchange tests passed when they asserted raw RTF through MSPDI. Do not treat those results as validating the corrected plain-text MSPDI contract, native RTF persistence, or the physical GUI route. The draft PR branch includes latest master `255a82274` (#1201). This candidate does not close the Assignment Information acceptance row or issue #1180.

The first hosted macOS batch exposed an invalid fixture assumption: a negative temporary task ID was renumbered during export. The fixture now explicitly assigns stable positive task/resource IDs and checks those after reload. `:microproject_exchange:test --tests "com.microproject.exchange.MpoFileImporterTest" --console=plain` passed on Windows with the corrected fixture.
