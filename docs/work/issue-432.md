# Issue #432 current-state packet

## Frozen acceptance contract

- A1 — Fresh Task 01–10 names and main Duration/Start/Finish values render readable without unnecessary clipping. **PASS** (production Robot, 1009×762 and 1300×820).
- A2 — Fresh default columns/split use available width responsively, while leaving useful Gantt content visible. **PASS** (window-size journey; bars visible 8→4→8).
- A3 — Explicit manual column widths survive rebuild/resize and header/body geometry stays aligned. **PASS** (physical header resize to 110 px; production width snapshot/rebuild and resize checks).
- A4 — Explicit divider restore/drag persists through resize and aligned sibling view does not overwrite it. **PASS** (Robot + shared lifecycle unit regression).
- A5 — Physical Windows evidence at multiple sizes and display scales. **PARTIAL** (two physical sizes at machine's current scale; actual scale percentage not recorded; 1.0/1.25/1.5 CI matrix remains pending).

## Intake and root cause

- Fetched #432 body and all comments once on 2026-10-24. Latest comment: https://github.com/tetsuji16/microProject/issues/432#issuecomment-6385062529.
- Baseline HEAD: `043a0a85834f49c7eb46bb45ddb17c3ad09d262e`; changes remain uncommitted.
- Baseline production journey at 1009×762 rendered the name, duration, start, and finish columns at 26 px despite null project/template width metadata. The pre-fix Robot assertion failed on the live GanttView.
- Root cause: `autoSizeColumnsToContent()` set preferred widths to 26 before JTable's first `doLayout`; GanttView's deferred floor checked actual width (then 150) and skipped. JTable subsequently materialized the 26 px preferred widths.
- The old column-width property heuristic also treated first Swing layout changes as user widths. Physical header-boundary gestures now establish manual provenance; ordinary layout does not. Configured manual widths bypass readable floors/autosizing.
- Divider root cause: non-silent programmatic divider property events were incorrectly treated as manual choices, preventing responsive defaults. FlatLaf's divider is an AWT `Component`, not a `JComponent`; the first tracker guard silently skipped it. `JSplitPane.resizeWeight=0.5` also moved restored pixel locations by half the frame width delta. The Gantt-only policy now tracks the actual `BasicSplitPaneUI` divider component, treats workspace restore/physical drag as explicit, and uses resizeWeight 0 while automatic defaults recalculate through shared `SplittedView` layout. Other split views keep their existing resize weight.
- The shared test snapshots widths through production `getFieldArrayWithWidths()` from a cloned field array, then restores through the project field-array/rebuild path. This is snapshot/rebuild evidence, not a full MPO save/reload.

## Implementation and evidence

- `SpreadSheetColumnModel` applies canonical default floors before first JTable layout, respects explicit manual widths below the floor, and exposes shared responsive minimum handling. `GanttView` no longer has duplicate hard-coded per-field floor logic.
- `ScaledScrollPane` has a default-disabled, Gantt-only center-date anchor for viewport-width changes. This keeps the visible timeline centered while the default split moves; vertical position and bounded horizontal position are retained. At 1300×820 → 1009×762 → 1300×820, visible task bars are 8 → 4 → 8. The original left-edge behavior showed 0 bars in the narrow viewport; the physical regression checks actual task-bar intersection and center date rather than merely checking a nonzero viewport.
- Shared `SplittedView` applies adaptive sizing only to views with an opt-in content-width hook; workspace restores remain explicit and are still published to MainView for sibling alignment.
- When a real divider gesture or workspace restore selects a shared split, `MainView` also marks the aligned peer explicit so a responsive sibling cannot later undo that choice. Ordinary silent alignment remains automatic. `SplittedViewLifecycleTest` covers automatic top + restored bottom.
- Existing `TaskTableGanttGridGuiAcceptanceTest` now runs one production MainRibbonFrame/GraphicManager/DocumentFrame/GanttView Robot journey, sizes 1300×820 → 1009×762 → 1300×820, checks rendered Task 01–10 names, main column widths/text, header/body alignment, Finish-cell visibility, actual Gantt viewport, physical name-column drag to 110 px, production snapshot/rebuild, resize preservation, divider drag, and restored pixel position after resize.
- Final focused Robot command passed: `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.productionTaskColumnsReportReadableLayoutAtCommonWindowSizes" --max-workers=1 --no-daemon --console=plain` (1 passed; 25 actionable tasks, 2 executed). It refreshed `installDist`; the Robot journey itself runs in-process.
- Targeted unit regressions passed after the center-anchor change: `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.timescale.ScaledScrollPaneTest" --tests "com.microproject.pm.graphic.views.SplittedViewLifecycleTest" --max-workers=1 --no-daemon --console=plain` (`BUILD SUCCESSFUL`; 20 actionable tasks, 2 executed).
- Final full UI module command passed on the stable production diff: `.\gradlew.bat :microproject_ui:test --max-workers=1 --no-daemon --console=plain` (`BUILD SUCCESSFUL`, 20 actionable tasks, one executed and 19 up-to-date; 1m16s).
- Final focused Robot artifacts: `modules/microproject_ui/build/reports/guiTest-artifacts/issue-432-layout.txt`, latest wide/narrow/wide screenshots `issue-432-1300x820-1692.png`, `issue-432-1009x762-3244.png`, `issue-432-1300x820-4797.png`, plus `issue-432-manual-width-after-rebuild.png` and `issue-432-manual-width-after-resize.png`. Baseline failure screenshots and column event log remain alongside them in that ignored build artifact directory.
- A separate installed launcher was started from `modules/microproject_ui/build/install/microproject_ui/bin/microproject_ui.bat` with `MICROPROJECT_UI_OPTS` setting `-Duser.home` and `-Djava.util.prefs.userRoot` to `modules/microproject_ui/build/reports/guiTest-artifacts/isolated-user-profile`, opening the disposable copy `modules/microproject_ui/build/reports/guiTest-artifacts/issue432-installed-launch.mpo` of `samples/CCPM sample English.mpo`. The visible title was `microProject - CCPM Visualization Sample [Master]` and the rendered table/Gantt were inspected. The user profile and source sample were not used or changed.
- Final source tested against baseline HEAD above plus the current uncommitted worktree diff. `git diff --check` passed.

## Remaining acceptance

- The physical GUI journey used the machine's current display scale, but its exact percentage was not recorded. DPI/scale variants 1.0, 1.25, and 1.5 remain pending in `.github/workflows/gui-acceptance.yml` (manual path); no real display setting was changed.
- Existing workspace splitter pixel positions are treated as explicit, including legacy saved values, because the frozen workspace/POD format has no provenance bit to distinguish a previously saved choice from an automatic default. This is the conservative persistence rule; no serialized workspace/POD field was added.

## Compatibility rationale

This is product layout behavior; no Microsoft Project parity claim is made. Restored splitter pixels and saved column widths follow the existing product workspace contract. The 0 resize weight is a document-derived product layout decision based on preserved pixel positions, not an empirical MSP observation.
