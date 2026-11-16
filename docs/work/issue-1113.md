# Issue 1113: the ribbon follows a theme change

State: implementation and focused verification complete; CI on the candidate commit pending.
Baseline: `b29cbc0c0`.

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | The ribbon chrome, tab text, command surface and group headings take the colours of a theme installed after the ribbon was built | Pass: `ModernRibbonPanel.updateUI()` re-reads the theme and rebuilds the tab bodies; `ribbonTakesTheColoursOfAThemeInstalledAfterItWasBuilt`. |
| A2 | The selected tab and the ribbon display mode survive the theme change | Pass: same test (Task tab, tabs-only mode). |
| A3 | Backstage command buttons have no fixed white background and are rebuilt for the new theme, also while the Backstage is open | Pass: the fixed `#ffffff` is replaced by the theme surface colour; `backstageCommandButtonsUseTheThemeSurfaceAndAreRebuiltOnThemeChange`. |
| A4 | The menu bar and the shell tool bar keep following the theme | Pass: `FlatUiSupport.followThemeBackground` sets the colour again on every UI update; `menuBarAndToolBarBackgroundsFollowTheTheme`. |

## Design note

`SwingUtilities.updateComponentTreeUI` updates a parent before its children. The ribbon rebuilds its children inside `updateUI()`, so the new children receive `updateUI()` afterwards. A `ColorUIResource` background would then be replaced by the look-and-feel default, so `FlatLafRibbonTheme` and `followThemeBackground` hand out plain `Color` values.

The Backstage view is cached and lives in the frame's layered pane, outside the ribbon's component tree. It is discarded on a theme change and, when open, shown again; the Backstage then returns to its first page.

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.ribbon.ModernRibbonPanelThemeTest" --console=plain`: pass (3 tests). The two ribbon tests fail on the baseline.
- Further results are recorded in the pull request.

## Limits

- Issue #1111 (applying a theme change without a restart) is still open, so no user-facing path triggers this refresh yet; the evidence is the component-level test, not an observation in a running application.
- After a theme change an open Backstage shows its first page again.
