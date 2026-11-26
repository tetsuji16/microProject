# Issue 1364: dark-theme CCPM buffer chart evidence

The issue asked for a visual check of the CCPM buffer chart in the dark theme.
The check is now an automated headless test instead of a screenshot, so the
same property is pinned on every run.

## What the test fixes

`modules/microproject_ui/src/test/java/com/microproject/ui/view/views/CriticalChainBufferChartPanelThemeTest.java`
gained `darkThemePaintsSeparableBandsAndReadableLabelsOnTheSurface`:

- it installs the dark theme and paints a `CriticalChainBufferChartPanel` into
  a `BufferedImage` with a finished CCPM analysis (one project buffer at 25%
  consumption), the same paint path the status dialog uses, without a display;
- the pixel inside each band (green at 5% consumption, amber at 25%, red at 60%,
  all at 10% progress) must equal `zoneColor(Zone)` for the dark theme, so the
  band is actually filled with the themed colour;
- the three band colours must be at least 40 apart in summed RGB distance
  (`MINIMUM_BAND_SEPARATION`), and each must be that far from the sampled panel
  surface, so the bands are separable from each other and from the background;
- `textColor()` must reach a WCAG contrast ratio of 4.5 against the surface and
  against every band, so labels stay readable on the dark chart.

The existing tests in the same class still cover the light theme, the theme
switch, and the selected-observation outline against every zone fill.

## Recorded result

| Item | Value |
|---|---|
| Base | `master` after `4e308fb40` (#1415 merge) |
| Command | `.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.view.views.CriticalChainBufferChartPanelThemeTest" --console=plain` |
| Result | BUILD SUCCESSFUL; `CriticalChainBufferChartPanelThemeTest` tests=3, skipped=0, failures=0, errors=0 |

No project file was saved and no application was launched, so the manual
`installDist` / `run_microproject_clean.bat` steps are not part of this record.
