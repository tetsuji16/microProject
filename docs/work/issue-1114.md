# Issue 1114: Gantt and chart drawing colours follow a theme switch

State: implementation and local verification complete; CI on the candidate commit pending.
Baseline: `8e14a8998`.

## Finding

Most of the files named in the issue no longer hold a theme-dependent colour in a field on the baseline:
`GraphRenderer`, `GanttParamsImpl`, `Network` and `NameCellComponent` read the theme when they paint.
Two files still had fixed colours.

| File | Fixed colours | Decision |
|---|---|---|
| `CriticalChainBufferChartPanel` | green, amber and red zone fills (light pastels) | Changed. In the dark theme the labels use the light theme text colour and were drawn on the light pastels. |
| `ChartHelper` | `OFFICE_*` series colours | Not changed. They are the Office chart series colours and identify data series, not chrome; `ChartHelperColorTest` pins them. |

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | Zone fills are read from the current theme at every paint | Pass: `CriticalChainBufferChartPanelThemeTest.zoneColoursFollowTheThemeAndKeepTheLabelsReadable` (light, dark, light again). |
| A2 | Labels drawn on a zone keep a contrast of at least 4.5:1 in both themes | Pass: the same test. |
| A3 | Light theme zone colours are unchanged | Pass: the light values are the previous constants. |

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "*CriticalChainBufferChartPanelThemeTest" --tests "*CriticalChain*"`: pass.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: pass.
- `git diff --check`: clean.

## Limits

- The dark chart was not inspected in a running application; the dark values were chosen by contrast, not by a design reference.
