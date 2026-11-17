# Issue #1162 — ribbon control kinds and activation

Baseline: `903a4ca3f` (master after #1196; #1194 input-value dispatcher is included). Original contract: both kind and activation gap lists empty, #1154 reconciliation complete, and JA/EN 100/125/150% visual evidence. All listed children are closed; their closure does not itself prove the parent criteria.

| Criterion | State and evidence |
|---|---|
| Kind and activation gaps | The ratchets are empty, but the two font-color controls are still pending semantic parity. Their current direct chooser route is not a gallery and must not be counted as complete merely because the ratchets are empty. |
| #1154 reconciliation | Open: [Microsoft's Current Channel ProjectControls workbook](https://github.com/OfficeDev/office-fluent-ui-command-identifiers/blob/main/Microsoft%20365/Current%20Channel/projectcontrols.xlsx) lists `FontFillBackColorPicker` and `FontColorPicker` as `gallery` on TabTask/GroupFont. Microsoft Support documents sheet cell background fill: select one or more cells, then Task > Font > Background Color; choose Theme color, Standard Color, or More Colors ([Highlight cell backgrounds](https://support.microsoft.com/en-us/project/highlight-cell-backgrounds)). This slice retains the app's existing persisted task-format contract: gallery selection applies an RGB/automatic text or background override to each selected writable task through the shared formatting command. It does not claim cell-by-cell formatting parity. |
| JA/EN 100/125/150% visual check | Full hosted [run 43999020531](https://github.com/tetsuji16/microProject/actions/runs/43999020531) passed on `a1add999f`: Japanese 100% functional plus JA 125/150% and EN 100/125/150% visual legs. Revalidate any later relevant layout changes. |

## Completed foundation — preserve committed input values

PR [#1194](https://github.com/tetsuji16/microProject/pull/1194) added the missing `RibbonCommandInvocation.value` propagation through the shared `MenuRibbonCommandSource` dispatcher, with an input-to-dispatch test. The target commands now have a safe common route for gallery-selected color values.

## Font and fill color gallery implementation slice

The two Task/Font controls now declare `GALLERY,POPUP` and render through the existing `ModernRibbonPanel`/`RibbonControlPopups` path. The gallery offers ten default Office theme colors with lighter/darker variants, Standard colors, Automatic (clear override), and More Colors (the existing chooser). RGB or Automatic goes through `RibbonCommandInvocation.value`, then the same selected-task formatting transaction and Undo edit as the older chooser route. The GUI acceptance covers a two-task selection, attributes and an unselected task that must be preserved, renderer colors, Undo/Redo, More Colors, MPO round-trip using stable task IDs, no selection, and read-only project behavior.

Local verification on this branch after rebasing on `903a4ca3f`:

- Focused unit command: `:microproject_ui:test --tests com.microproject.ui.ribbon.RibbonValueControlTest --tests com.microproject.ui.ribbon.RibbonControlContractTest --tests com.microproject.ui.ribbon.MspRibbonControlKindDiffTest --tests com.microproject.ui.ribbon.MspRibbonActivationDiffTest --tests com.microproject.pm.graphic.frames.TaskFormatPainterServiceTest` — 56 tests passed.
- Windows physical Robot command: `:microproject_ui:guiTest --tests com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.taskColorGalleriesApplySelectedTasksWithUndoRedoAndMpoReload` — passed. It used physical keyboard Home/Down/Right to open the gallery, Space to choose a swatch, Enter to open More Colors, Tab to move focus, Escape to dismiss, and a mouse click to confirm the chooser. It also checked accessible names, popup display bounds, application focus restoration, task formatting, Undo/Redo, persistence, and no-selection/read-only states.
- The first physical More Colors attempt exposed a nested-menu issue: hovering a `JMenuItem` inside the gallery changed Swing's menu-selection path and dismissed the gallery before click dispatch. The More Colors/Automatic rows are now ordinary focusable `JButton`s inside the gallery, and physical hover/click succeeds. Escape closes the gallery and restores focus to the owning application window.

Remaining limits: the Theme palette is a static default Office theme because this app does not currently resolve the active document theme into the picker. This does not establish dynamic per-document theme parity. The hosted Robot/full CI and the original JA/EN 100/125/150% visual matrix must still run on the final PR candidate. The original parent acceptance also requires reviewing every remaining `BASELINE` reason; this slice does not close #1162.
