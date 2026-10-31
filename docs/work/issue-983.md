# Issue #983 — Consolidate dark-colour tests, RGB mixes and EDT waits

State: IN PROGRESS (visual evidence pending)
Issue: https://github.com/tetsuji16/microProject/issues/983
Contract frozen at: issue text at a05d74c60
Baseline HEAD: `25c41fd32`
Baseline working tree: clean worktree `microProject-w983` (branch `claude/issue-983`)

## Goal and scope

One dark/light classification, one RGB mix, one EDT-wait helper, plus the
small duplicate-paint/tooltip consolidations named in the issue.

Out of scope: `MpoFileImporter.runOnEdt` (separate module, cannot see the UI
dispatcher); the gantt/network arrowhead painters (kept separate, see below).
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | One dark test and one mix (`ColorLuminance.isDark` / `mix`); light and dark confirmed, no visual assertion weakened | code done; visual pending | unit table test; CI GUI suite pending |
| A2 | EDT waiting in `SwingUiDispatcher.callAndWait`/`runAndWait` | pass (unit) | `SwingUiDispatcherTest` |

## Decisions

- Dark = WCAG relative luminance <= 0.179 (the black/white contrast crossover,
  already used by `ChartLegend`). Sites moved: `FlatUiSupport.isDarkTheme`
  (was RGB sum < 384), `SpreadsheetAppearanceSupport` (was luma < 0.45),
  `CriticalChainGraphPanel` and three `FormComponent` / `ChartLegend` checks
  (were token-identity / luminance).
- Mix: `ColorLuminance.mix(base, target, targetWeight)`, float arithmetic.
  `FlatUiSupport.blend` (public) and `ClassicMSProjectPalette.mix` delegate and
  are bit-identical for every painted weight; `FormComponent` moved from
  double to float weight: identical on all light-theme inputs (tested).
- Ribbon large/small button styling: one private `styleRibbonCommandButton`.
- HTML tooltips: `HyperLinkToolTip.isHtmlTip/touchingLocation/createFor` used by
  `ButtonFactory`, `MenuFactory`, `TabbedNavigation`, `TransformComboBox`,
  `AbstractDialog` (its own location override is kept).
- Link arrows: `GanttRenderer` and `NetworkRenderer` `drawLinkArrows` differ
  (Gantt recolours and restores paint/colour; Network caches a dark-link
  shape and only repaints cross-project). Not merged.
- EDT callers: `StartupFactory`, `SpreadSheet.setFieldArray`,
  `GraphicManager.requestQuit`/`quitApplication`, `MenuRibbonCommandSource`.
  Exceptions now propagate as-is (runtime/error) or wrapped in
  `IllegalStateException` (checked, interrupt with flag restored). No test
  depended on the former messages. `quitApplication` keeps its own
  "must not wait on the EDT" guard (it has no callers).

## Classification deltas (old -> new)

- Real light/dark theme surfaces (9 token backgrounds each): unchanged.
- Spreadsheet cell fills (old luma<0.45 -> new): ~8% of an RGB grid flips,
  e.g. pure red FF0000 dark -> light (blend 0.94 -> 0.82). Pure blue,
  green, purple, 4472C4, C00000 unchanged.
- `isDarkTheme` UIManager `Panel.background`: e.g. 00B050-like greens flip;
  FlatLaf grey backgrounds (3C3F41, 2B2B2B, F2F2F2, FFFFFF) unchanged.

## Current slice

**Next slice:** run CI GUI acceptance for spreadsheet, Gantt/network and ribbon.
**Done when:** visual suite passes on the candidate SHA.
**Then:** close #983.

## Remaining blockers

- Physical GUI/visual evidence (shared desktop; not run locally).

## Latest evidence

- Tests/build: `:microproject_ui:test` (see report), new `ColorLuminanceTest`, `SwingUiDispatcherTest`.
- Physical GUI/manual: pending (CI).
- CI: pending.
