# Issue #1336 — Fonts used by printing and print preview

State: IN REVIEW
Issue: https://github.com/tetsuji16/microProject/issues/1336
Branch: `issue/1336-print-preview-aspect`
Base: `origin/master` `c916bbf05`

## Goal and scope

Judge whether the fonts used by actual printing and by the print preview are appropriate, and fix
the horizontal squash of the print preview reported in the issue thread.

## Fonts

No change. Print and preview share one rendering path: `GraphPageable`'s constructor calls
`FontManager.setOfflineDefaultFont(FontManager.getDefaultFont())`, and the table and Gantt renderers
read `FontManager.getOfflineDefaultFont()`. That font is the runtime UI font, which contains Japanese
glyphs on a Japanese system, and a user-chosen cell or text-style font still wins. Page geometry is in
points and follows the page setup scaling, so the preview and the paper agree. The only visible
consequence is that the same file can look slightly different on another OS, which matches Microsoft
Project and is not a defect.

## The squash reported in the thread

`GraphPageable.updateZoom(pagesWide, pagesTall)` treated the two fit-to limits as one limit per axis:
`getWRatio` produced the horizontal scale and `getHRatio` the vertical one, and both were applied
independently. A drawing fitted to "1 page wide by 1 page tall" was therefore stretched to fill the
page in both directions.

Measured on a 12-task Gantt chart with a landscape Letter page (`736 x 556` imageable):

| Fit | total zoom X | total zoom Y | X/Y |
|---|---|---|---|
| before, 1 x 1 | 0.4118 | 2.0366 | 0.202 |
| before, 1 x 3 | 0.4118 | 4.5950 | 0.090 |
| after, 1 x 1 | 0.4118 | 0.4118 | 1.000 |
| after, 1 x 3 | 0.4118 | 0.4118 | 1.000 |

A ratio of 0.202 is a fivefold horizontal crush; rendered to a 560 x 720 preview the glyphs collapse
into vertical slivers. After the change the drawing keeps its proportions and still fits inside the
requested page grid.

The fix takes the smaller of the two scales, so the grid stays an upper bound rather than an exact
target. Fit-to with an automatic page height (`pagesTall == 0`) and the plain percentage scaling path
are unchanged; the saved fit-to settings are re-applied on load, so an existing file is corrected
without user action.

## Acceptance

| ID | Requirement | State | Evidence |
|---|---|---|---|
| A1 | Font judgment recorded, no font change | Complete | Issue comment; `FontManager` / `GraphPageable` path above. |
| A2 | A fit-to grid uses one scale for both axes | Complete | `GraphPageableFitToZoomTest`. |
| A3 | The drawing still fits inside the requested grid | Complete | Same test asserts the page count stays within the grid. |
| A4 | Width-only fit and percentage scaling are unchanged | Complete | `fittingToAWidthOnlyUsesOneScaleForBothAxes`; default project still reports `0.77 / 0.77`. |

## Verification

- `:microproject_ui:test --tests "com.microproject.print.GraphPageableFitToZoomTest"`: PASS.
- `:microproject_ui:test`: PASS.
- `scripts/test_source_inventory.py --check`: PASS.
- Rendered page before/after compared as images; the squashed glyphs are upright after the change.
- `GUI acceptance`: required before merge.
