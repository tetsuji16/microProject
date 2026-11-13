# Issue 1125: read-only time-phased cells

State: implementation and focused verification complete; candidate CI pending.
Baseline: `6d3fc473c`.

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | A cell that cannot be edited in the time-phased grid follows the active theme | Pass: `TimeSpreadSheet.prepareRenderer` uses `FlatUiSupport.spreadsheetReadOnlyBackground()` instead of the fixed `0xd3d3d3`. |
| A2 | The text stays readable in the light and dark themes | Pass: `TimeSpreadSheetReadOnlyCellTest` checks a contrast of at least 4.5 in both themes and a dark background in the dark theme. |
| A3 | Selected and editable cells keep the renderer's colours | Pass: `selectedAndEditableCellsKeepTheRendererColours`. |

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheetReadOnlyCellTest" --console=plain`: pass.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: pass.

## Limits

- The Resource Usage view was not observed in a running application in this slice; the evidence is the renderer-level test.
