# Issue #724 — Dialog minimum-size and resize contracts

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/724
Contract frozen at: issue body plus owner comments through 2026-11-10 (ordering: #724 signs the dialog contract first, #863 extends it to non-dialog windows, and each dialog's evidence is recorded once here)
Baseline HEAD: `e14251d09`
Baseline working tree: clean for this issue; other sessions' in-progress files are not included

## Goal and scope

Define, enforce, and evidence one dialog size contract: a single bounded form
keeps its packed size, while a table, list, or tabbed data view stays resizable
and keeps its text controls at font-preferred heights and its window inside the
usable screen.

Out of scope for this slice: main-window and other non-dialog surfaces (#863);
the remaining dialog matrix (`RenameDialog`, `PageSetupDialog`,
`TransformParameterDialog`, `XbsDependencyDialog`, `LicenseDialog`,
`RenameProjectDialog`, plus the already-documented Baseline, Project Information,
Resource Information, Resource Mapping, and Find rows); and the full
ja/en × 100/125/150% matrix with the Task Information Resources-tab conclusion,
which stay tracked in this issue but are not claimed complete.

Blockers: none for the covered slice. The full matrix needs a hosted 1920×1080
desktop so the 125% and 150% runs have enough usable height.

## Acceptance

The issue body defines five prose criteria. This packet tracks them as IDs so
each one has an explicit evidence state.

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Each covered dialog declares its size contract once, not with per-dialog pixel sizes | pass | `AbstractDialog.initComponents` calls `setResizable(!isFixedSizeForm())`; `isFixedSizeForm()` defaults to `false` |
| A2 | `AutoFilterDialog` is a fixed-size form, route-displayed, screenshotted, and asserted | pass | `TaskTableGanttGridGuiAcceptanceTest.physicalColumnAutoFilterUsesDisplayedValuesAndUpdatesTheVisibleRows`; `issue-724-autofilter-ja-1.0.png` |
| A3 | `FieldAliasDialog` is a fixed-size form, route-displayed, screenshotted, and asserted | pass | `TaskTableGanttGridGuiAcceptanceTest.physicalCustomColumnRenameUsesTheHeaderPopupDialog`; `issue-724-field-alias-ja-1.0.png` |
| A4 | `StatusDateDialog` is a fixed-size form in both its visual-matrix and mutation routes | pass | `TaskInformationRibbonGuiAcceptanceTest.robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix` and `.statusDateRibbonRouteUsesSharedMutationFixture`; `issue-724-status-date-ja-1.0.png` |
| A5 | `MoveProjectDialog` is a fixed-size form through Project Information &gt; Move Project | pass | `ProjectInformationDialogGuiAcceptanceTest.projectInformationShowsAllTabsAndButtonsAfterResize`; `issue-724-move-project-ja-1.0.png` |
| A6 | `UpdateProjectDialogBox` is a fixed-size form through the Project ribbon route | pass | `TaskInformationRibbonGuiAcceptanceTest.robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix`; `issue-724-update-project-ja-1.0.png` |
| A7 | The audit document records the covered rows and removes unreachable candidates | pass | `docs/dialog-layout-audit.md`, "Dialog size and viewport contract (#724)" |
| A8 | Remaining reachable dialogs each get route → screenshot → assertion evidence | open | next slices |
| A9 | ja/en × 100/125/150%, including constrained usable height | open | needs hosted 1920×1080 desktop |
| A10 | Task Information Resources-tab blank-area conclusion is recorded | open | unclassified since the 2026-10-02 capture |
| A11 | Existing intentional spacing is recorded, and height is never shrunk uniformly across tabs | pass | The audit table and `docs/dialog-layout-audit.md` "Contract" section keep per-surface rules; this slice changes no uniform height |

A2–A6 cover one dialog each, so no route evidence is recorded twice. A8–A10
are the reasons this issue stays open with `Refs #724`.

## Current slice

**Next slice:** add `RenameDialog` through the column-list popup's current-column
item, then `XbsDependencyDialog` through the dependency properties route.
**Done when:** each new dialog has a route test that asserts its declared
contract and writes an `issue-724-*` screenshot.
**Then:** run the ja/en × 100/125/150% matrix and close A8–A10.

## Remaining blockers

- A8: `RenameDialog`, `PageSetupDialog`, `TransformParameterDialog`,
  `XbsDependencyDialog`, `LicenseDialog`, and `RenameProjectDialog` have no
  contract assertion yet. `LicenseDialog` already has a Help route, but the
  local Robot run is blocked by desktop contention: a foreground ChatGPT window
  overlaps the test window, and `GuiDesktopSessionCoordinator` refuses the input
  before dispatch.
- A9: the 125% and 150% runs need a hosted desktop taller than the local usable
  area.
- A10: the Resources-tab blank area is still unclassified.

## Latest evidence

- Compile: `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain`
  → BUILD SUCCESSFUL.
- Focused route tests, ja / scale 1.0 and en / scale 1.0:
  `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalCustomColumnRenameUsesTheHeaderPopupDialog" --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalColumnAutoFilterUsesDisplayedValuesAndUpdatesTheVisibleRows" --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotIssue592StatusDateAndUpdateProjectDialogsFitVisualMatrix" --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.statusDateRibbonRouteUsesSharedMutationFixture" --tests "com.microproject.dialog.ProjectInformationDialogGuiAcceptanceTest.projectInformationShowsAllTabsAndButtonsAfterResize" "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" --console=plain`
  and the same command with `"-PguiTestLocale=en"` → 5 tests completed, all passed
  in both locales.
- Unit tests: `.\gradlew.bat :microproject_ui:test --console=plain` → BUILD SUCCESSFUL.
- Full GUI suite: `.\gradlew.bat :microproject_ui:guiTest "-PguiTestLocale=ja" "-PguiTestUiScale=1.0" --console=plain`
  → aborted as `GUI_ENVIRONMENT_CONTENDED` after 68 `initializationError`s; a
  foreground unrelated window overlapped the 2560×1392 test desktop. No
  assertion from this change ran or failed.
- Inventory: `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`
  → exit 0. `git diff --check` → empty.
- Screenshots: `modules/microproject_ui/build/reports/guiTest-artifacts/issue-724-*-{ja,en}-1.0.png`.
- CI: pending candidate SHA.
