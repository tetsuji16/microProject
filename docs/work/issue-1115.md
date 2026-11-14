# Issue #1115: Backstage navigation layout and colours

## Scope

Only the left navigation of the File/Backstage view and the theme tokens it uses. The detail pages are #1116 to #1120.

## Evidence

- Microsoft Support, "Keyboard shortcuts for the File menu in Microsoft 365 for Windows" (read 2026-11-14): Office-wide page. It lists Alt+F and one letter per destination, and the arrow keys, Enter and Esc inside the File menu. It does not show the Project navigation in one figure.
- The destination order follows the issue contract: New, Open / separator / Info, Save, Save As, Print, Export, Close / Options at the bottom. It was not compared with a running Microsoft Project.

## Change

- `MicroProjectThemeTokens`: four new colours, `backstageNavigationBackground`, `backstageNavigationForeground`, `backstageNavigationHoverBackground`, `backstageNavigationSelectedBackground`, for both themes.
- `ModernRibbonPanel.backstageView()`: the navigation is a header (icon-only Back), a scrolling destination list and a pinned Options area. Every destination is one `BackstageNavigationButton`. Up and Down move the focus, Enter activates, Esc is still handled by the Backstage host.
- `menuInternal.properties`: `FileBackstagePages` ends with `close options`.
- `menu.properties` / `menu_ja.properties`: `FileBackstage.back`; the Japanese Save caption is now the Microsoft Project wording for Save.

## Decisions that differ from the issue text

| Issue text | Implemented | Reason |
|---|---|---|
| Navigation background is `FlatUiSupport.ribbonAccentColor()` in both themes | Light: the accent `0x0F773D`. Dark: `0x1F5C3A` | The dark accent `0x54A878` gives about 2.9:1 with white text. The issue also requires 4.5:1, so the dark theme uses a darker green of the same hue. |
| Hover and selection are lighter than the pane | Light: hover is lighter, selection is darker. Dark: hover is lighter, selection is darker | A selection lighter than the light accent falls below 4.5:1 with white text. The selected destination also keeps the 4px white bar. |
| Key hints for the keys confirmed on the Microsoft page | None | The application has no KeyTip sequence after Alt+F, so a hint would describe a key that does nothing. |

The fixed `#ffffff` card background named in the issue was already removed by #1113.

## Acceptance

| # | Contract | Result |
|---|---|---|
| A1 | Order and separator; Options outside the scrolling list | Pass: `ModernRibbonBackstageNavigationTest.destinationsFollowTheMicrosoftProjectOrderWithOptionsPinnedBelowTheScrollingList`, `RibbonStructureTest`, and the on-screen order in `RibbonTabGuiAcceptanceTest.fileTabOpensBackstageAboveTheWorkspaceAndRestoresTheRibbon`. |
| A2 | Navigation colours come from the tokens and keep 4.5:1 in every state | Pass: `MicroProjectThemeTest.backstageNavigationTokensKeepWhiteTextReadableOnEveryState`, `ModernRibbonBackstageNavigationTest.navigationUsesTheAccentColourWithReadableTextInBothThemes`. |
| A3 | Back is an icon with a localized accessible name; Japanese Save caption | Pass: `ModernRibbonBackstageNavigationTest.backIsAnIconButtonWithALocalizedAccessibleName`. |
| A4 | Up, Down and Enter | Pass: bindings in `ModernRibbonBackstageNavigationTest.upDownAndEnterAreBoundOnEveryNavigationEntry`; real key presses in the GUI acceptance test. |
| A5 | Ribbon tab and display mode are restored | Pass: unchanged assertions of the GUI acceptance test. |

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "*ModernRibbonBackstageNavigationTest" --tests "*MicroProjectThemeTest" --tests "*ModernRibbonPanelThemeTest" --tests "*RibbonStructureTest" --tests "*RibbonAndToolbarButtonTest"`: pass.
- `.\gradlew.bat :microproject_ui:guiTest --tests "*RibbonTabGuiAcceptanceTest.fileTabOpensBackstageAboveTheWorkspaceAndRestoresTheRibbon"`: pass. Image: `modules/microproject_ui/build/reports/guiTest-artifacts/ribbon-file-backstage-open.png` (dark theme, Japanese).
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: pass.
- `git diff --check`: clean.

## Limits

- `RibbonTabGuiAcceptanceTest.backstageClosesBeforeTheOptionsCommandOpensItsModalDialog` fails on the local desktop both with this change and on unmodified master (`d9e7b74c7`): the focus does not return to the ribbon tab after the modal dialog. The hosted `GUI acceptance` check is the evidence for that test.
- The light theme and the short-window scrolling were checked by tests only, not by eye in a running application.
- Not compared with a running Microsoft Project.
