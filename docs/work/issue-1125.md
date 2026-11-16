# Issue 1125: read-only time-phased cells

State: implementation, review fixes and focused verification complete; CI on the candidate commit pending.
Baseline: `6d3fc473c`.

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | A cell that cannot be edited in the time-phased grid follows the active theme | Pass: `TimeSpreadSheet.prepareRenderer` uses `FlatUiSupport.spreadsheetReadOnlyBackground()` instead of the fixed `0xd3d3d3`. |
| A2 | The text stays readable in the light and dark themes | Pass: `TimeSpreadSheetReadOnlyCellTest` measures the pair the renderer actually paints (`spreadsheetReadOnlyForeground()` on `spreadsheetReadOnlyBackground()`): 4.59 in the light theme and 6.53 in the dark theme, both at least 4.5. |
| A3 | Selected and editable cells keep the renderer's colours | Pass: `selectedAndEditableCellsKeepTheRendererColours` and the grid-level test below. |
| A4 | The fix is applied by the grid, not only by the extracted helper | Pass: `theGridPaintsReadOnlyCellsWithTheThemeBackground` renders a cell through `TimeSpreadSheet.prepareRenderer` and fails when the fixed fill returns. |

## Design note

The read-only cue is the dimmed `spreadsheetReadOnlyForeground()`; the fill only has to follow the theme and must not be the removed fixed light grey. A visibly darker light-theme fill would push that text below the 4.5 contrast gate, because the read-only foreground on white is itself only 4.83. Custom themes can still set a stronger fill through `MicroProject.spreadsheetReadOnlyBackground`.

`prepareRenderer` asks the table (`isCellEditable(row, column)`) rather than repeating the view-column-to-model-column offset (`column + 1`) that the time column model defines. The table performs the same conversion as the renderer in `CellUtility`, and it stays correct when columns move.

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheetReadOnlyCellTest" --console=plain`: pass (5 tests).
- Temporary mutation check: replacing the theme fill with the removed `0xd3d3d3` makes 3 of the 5 tests fail, so the tests guard the bug.
- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.*" --console=plain`: 223 tests, 1 failure. `SpreadSheetEditorTableTrackingTest.nameEditorAppliesTheSameTaskTextStyleAsTheRenderer` fails on the font family (`Dialog` vs `Yu Gothic UI`); it also fails identically on the unmodified baseline `6d3fc473c` in this environment, passes when that class runs alone, and is unrelated to this change.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: pass.

## Limits

- The Resource Usage view was not observed in a running application in this slice; the evidence is the renderer-level test.
