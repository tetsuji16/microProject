# Issue #864 — カレンダー上の西暦日付と副暦日付の対応が分かりにくい

State: IN PROGRESS — scoped implementation/evidence complete; integration pending
Issue: https://github.com/tetsuji16/microProject/issues/864 (OPEN; checked 2026-10-24)
Contract frozen at: 2026-10-24, filed issue body
Baseline HEAD: `7cbc3beba28adc16f46b1a8bcd9af2c0e20e9ef9`
Tested tree: baseline SHA plus only the two uncommitted focused test additions below
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Make it immediately clear in Change Working Time that large dates and small secondary-calendar dates label the same day, with weekdays, selection, and work-time edits tied to Gregorian dates. Preserve click-to-model date, work-time mutation, persistence, and Japanese/English visible and accessible semantics. This is a product UX extension, not an MSP parity claim.

Issue context: screenshot showed October 2026 (Rab. II 20, 1448 AH) with small Islamic dates and no explanation. Reporter did not assert a date calculation bug. Existing behavior intentionally chooses a non-ISO chronology from a locale `u-ca-*` extension, else supported Windows `iCalendarType` settings; users configure it through locale/regional settings, with no in-dialog toggle. Gregorian-only settings display no companion dates.

Out of scope: changing authoritative chronology/date mapping or adding a calendar toggle. Blockers: PR CI on integrated candidate SHA.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Explain primary Gregorian vs secondary chronology and its setting source; weekdays, selection, edits use Gregorian dates | pass | Existing `AlternativeCalendarDisplay` and dialog legend/accessibility/tooltip present by `873f140b0`; ja/en physical Robot assertions pass |
| A2 | Clicked date, work-time mutation and native save/reload remain unchanged | pass | Japanese physical selection/edit Robot plus added MPO save/reload assertion pass; recurring exception Robot also passes Undo/Redo and POD roundtrip |
| A3 | Japanese/English visible and accessible explanation/tooltips | pass | `robotAddsRecurringException...` ja and `robotEnglishAlternativeCalendarExplainsGregorianEditDatesAndTooltips` en pass |
| A4 | Month, leap-day, era/year boundary mapping remains deterministic | pass | Added Islamic chronology coverage for Gregorian leap day and year/month transitions; existing Japanese Reiwa boundary coverage |

## Current slice

**Next slice:** integrate the focused coverage and run required PR checks.
**Done when:** review confirms the focused diff, UI build/tests pass, and GUI acceptance passes on candidate SHA.
**Then:** root agent decides issue closure; do not close externally in this worker task.

## Remaining blockers

- PR CI on the integrated candidate SHA.
- Successful Robot runs did not retain screenshots; no screenshot path exists. Visible legend, tooltip, accessibility description, selected model date, and persistence were asserted directly.

## Latest evidence

- Unit command: `.\gradlew.bat :microproject_ui:test --tests "com.microproject.util.AlternativeCalendarDisplayTest" --tests "com.microproject.dialog.calendar.CalendarViewRenderingTest" --console=plain --no-daemon --max-workers=1`; pass, 14 tests (9 + 5), 0 failures. XML: `modules/microproject_ui/build/test-results/test/TEST-com.microproject.util.AlternativeCalendarDisplayTest.xml`, `.../TEST-com.microproject.dialog.calendar.CalendarViewRenderingTest.xml`.
- GUI ja, native date persistence: `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotSelectsDisplayedDateMarksNonWorkingAndCommits" -PguiTestLocale=ja --console=plain --no-daemon --max-workers=1`; pass after adding MPO assertion. Also `robotAddsRecurringExceptionCommitsUndoesRedoesAndPersists` pass. GUI en: `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotEnglishAlternativeCalendarExplainsGregorianEditDatesAndTooltips" -PguiTestLocale=en --console=plain --no-daemon --max-workers=1`; pass. One earlier compile failure from a `WorkCalendar`/`WorkingCalendar` test cast was corrected; rerun passed. GUI XML: `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.xml`; HTML: `modules/microproject_ui/build/reports/tests/guiTest/*/index.html`. Last XML is the one-test ja rerun; earlier methods are recorded in Gradle output. No desktop overlap events; diagnostic monitor logs contain startup probes only.
- Source history: production explanation/accessibility is already present by `873f140b0`; this slice adds regression evidence only. `git diff --check` passed.
- CI: pending on integrated candidate SHA.
