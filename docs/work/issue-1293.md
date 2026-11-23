# Issue #1293 / #1334 — the Backstage Print page without waiting for printers or the document

State: IN PROGRESS (pull request open; checks running)
Issues: https://github.com/tetsuji16/microProject/issues/1293, https://github.com/tetsuji16/microProject/issues/1334
Contract frozen at: issue bodies of 2026-11-22
Baseline HEAD: `96f441330`
Baseline working tree: clean

## Goal and scope

Opening Backstage → Print must not wait for the printer list or for the print document. The
page appears immediately with its settings controls and a preview placeholder; the printer
list is read on a background thread, the document is then created on the event thread from the
resolved printers, and the placeholder is replaced in place.

#1334 is the part of #1293 that takes `PrintServiceLookup` out of `PrintDocument`'s
constructor and out of the event thread, so the two issues are implemented and closed together.

Out of scope: making the lookup or the document creation faster; the separate print preview
window (`PrintPreviewFrame`); print page layout (done in #1118).

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | `createBackstagePrintPage` does not call `getPrintServices()` or `PrintServiceLookup` on the event thread | pass | `ModernRibbonPanel.createBackstagePrintPage` builds the page first and runs `PrintDocument.lookupRealPrintServices()` / `lookupDefaultRealPrintService()` inside `SwingWorker.doInBackground` |
| A2 | The page parts exist before the document is created | pass | `BackstagePrintPageTest.printersLoadingShowsTheLoadingTextAndKeepsPrintingDisabled`, `.thePreviewPlaceholderIsReplacedInPlaceWhenTheDocumentIsReady` |
| A3 | Resolved printers keep the previous order (PDF first) | pass | `PrintDocumentFactoryTest.resolvedServicesKeepThePdfServiceFirstAndSkipTheLookup` |
| A4 | Passing resolved printers does not call `PrintServiceLookup` | pass | `PrintDocumentFactoryTest.resolvedServicesKeepThePdfServiceFirstAndSkipTheLookup` (lookup counters must stay 0), `.theResolvingConstructorStillReadsTheSystemPrinters` |
| A5 | Printers arriving after the page is shown fill the field and enable printing | pass | `BackstagePrintPageTest.printersArrivingAfterThePageIsShownFillTheFieldAndEnablePrinting` |
| A6 | No document still shows the previous page and keeps the print command routable | pass | Existing `BackstagePrintPageTest.noPrintersShowsMessageDisablesPrintAndStillShowsPreview`, `RibbonTabGuiAcceptanceTest` print-page route |

## Current slice

**Next slice:** confirm `build` and `GUI acceptance` on the pull request head.
**Done when:** both required checks pass and no review comment is unanswered.
**Then:** merge, close #1293 and #1334, and update index #1054.

## Remaining blockers

- The issue asks for a before/after timing measurement on `samples/Commercial construction project plan.mpo`.
  It was not taken: this environment has no attached printer set and cannot drive the packaged
  application window. The change removes the blocking call from the event thread instead of
  shortening it, so the page no longer waits for the lookup at all. A follow-up measurement on a
  desktop with network printers can be recorded in the pull request if needed.

## Latest evidence

- Tests: `.\gradlew.bat :microproject_ui:test --console=plain` — BUILD SUCCESSFUL
  (includes `BackstagePrintPageTest`, `BackstageSaveAsAndExportPageTest`,
  `PrintDocumentFactoryTest`).
- Boundaries: `.\gradlew.bat verifyNamingConventions verifyArchitectureBoundaries --console=plain`
  — BUILD SUCCESSFUL.
- `git diff --check` — empty.
- Physical GUI: not driven here; the required `GUI acceptance` check runs in CI.
