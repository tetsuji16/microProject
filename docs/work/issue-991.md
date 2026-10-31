# Issue 991: Japanese IME reconversion key does nothing in the task table

Baseline: `25c41fd32` (origin/master). Branch `fix/windows-ime-reconversion`.

| ID | Acceptance | State and evidence |
| --- | --- | --- |
| A1 | Convert on selected text reopens it in the IME, on both spreadsheet routes | Passed on a real Microsoft IME: `CanonicalProjectWindowGuiAcceptanceTest.robotConvertReconvertsOrReportsUnsupportedImeOnBothSpreadsheetRoutes` |
| A2 | Committing updates the model and rendered cell, with Undo | Passed, same test |
| A3 | Cancelling restores the original text | Passed, same test; `EditorSelectionBehaviorTest.cancelledReconversionRestoresTheTextAnImeDoesNotCommitBack` |
| A4 | Without a usable IME, text and selection stay and the document reports it | Passed: `EditorSelectionBehaviorTest`, and the unsupported branch of the GUI test on hosted CI |

## Cause and fix

`sun.awt.windows.WInputMethod` does not implement `reconvert()`, so `InputContext.reconvert()` always throws on Windows. `WindowsImeReconversion` calls IMM32 through the Java FFM API instead: `ImmSetCompositionStringW` with `SCS_QUERYRECONVERTSTRING`, then `SCS_SETRECONVERTSTRING`. The call runs on the AWT toolkit thread, entered by a one-shot `WH_GETMESSAGE` hook and a posted registered message.

The launchers and packages now pass `--enable-native-access=ALL-UNNAMED`.

## Decisions

These are document-derived compatibility decisions, based on the Microsoft IMM32 documentation for `ImmSetCompositionStringW` and `RECONVERTSTRING`, not measured Microsoft Project behavior.

- A closed IME is opened by Convert. It stays open on success and is restored on failure.
- With no selection, the IME picks the clause at the caret.
- Empty text is a silent no-op. Convert during an active composition is ignored.
- An IME refusal, or no response within 2 seconds, shows the existing unsupported status and keeps the text and selection.

## Verification notes

The physical run used Microsoft IME, HKL `0x04110411`, Temurin 25.0.4.1. The user's Hermes window was in the foreground and overlapped the test window, so the desktop gate correctly blocked Robot input. The passing run moved the test window clear of it with a temporary uncommitted patch and waited for the window to be active before any key. The gate itself was not changed.

A separate throwaway harness with a foreground check before every key ran production code on a plain `JTextField`. Full selection, partial selection (`東京都庁` selecting `東京`), and a caret at the end or middle all gave correct results. Space and Enter committed `トウキョウ`, and Esc restored the original text.

## Known limits

- Hosted CI has no Japanese IME, so it proves only the unsupported branch. The reconversion branch needs a Windows desktop with a Japanese IME.
- Only Microsoft IME has been tried. Another IME that refuses the request gets the unsupported status.
- The Windows Win+/ reconversion route is handled by the OS and is out of scope.
