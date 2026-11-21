# Dialog form row audit

This is the completed source audit for production `DefaultFormBuilder.nextLine`
calls in `microproject_ui`. The audit excludes comments, tests, and the builder's
own method implementation. It contains 161 call sites in 35 production files.

## Contract

`nextLine(n)` advances the builder cursor by `max(1, n)` grid rows. A row
argument is a count of grid tracks, not logical form rows. With alternating
`p,3dlu,p` tracks, `nextLine(2)` skips the spacer and lands on the next
preferred-height track. `addSeparator()` already advances one row. These rules
are covered by `ProjectDialogTest` for counts 0, 1, 2, 4, and 8.

## Complete caller inventory

The `nextLine` argument sequence is listed per source file; `1` means the
no-argument or zero-argument call advances one track. The full constructor and
cursor comparison for each builder scope follows below.

| Production source | Calls | Arguments in source order |
| --- | ---: | --- |
| `dialog/AboutDialog.java` | 3 | 2, 2, 2 |
| `dialog/assignment/AssignmentDialog.java` | 8 | 2, 2, 1, 2, 1, 2, 2, 2 |
| `dialog/assignment/ReplaceAssignmentDialog.java` | 2 | 2, 2 |
| `dialog/assignment/TimesheetDialog.java` | 3 | 2, 1, 2 |
| `dialog/BaselineDialog.java` | 4 | 2, 1, 2, 2 |
| `dialog/calendar/ChangeWorkingTimeDialogBox.java` | 10 | 2, 2, 2, 2, 2, 2, 2, 2, 1, 1 |
| `dialog/calendar/NewBaseCalendarDialog.java` | 2 | 2, 2 |
| `dialog/ColumnDialog.java` | 1 | 1 |
| `dialog/DelegateTaskDialog.java` | 1 | 2 |
| `dialog/DependencyDialog.java` | 2 | 2, 2 |
| `dialog/FieldAliasDialog.java` | 2 | 2, 2 |
| `dialog/FindDialog.java` | 2 | 2, 2 |
| `dialog/HelpDialog.java` | 6 | 2, 2, 2, 2, 2, 2 |
| `dialog/InformationDialog.java` | 2 | 2, 2 |
| `dialog/LocaleDialog.java` | 7 | 2, 2, 1, 2, 2, 2, 2 |
| `dialog/LoginDialog.java` | 3 | 2, 2, 2 |
| `dialog/LookupDialog.java` | 2 | 2, 2 |
| `dialog/OpenProjectDialog.java` | 1 | 2 |
| `dialog/options/CalendarDialogBox.java` | 9 | 2, 2, 2, 2, 2, 2, 2, 2, 2 |
| `dialog/ProjectDialog.java` | 7 | 2, 2, 2, 2, 2, 2, 2 |
| `dialog/ProjectInformationDialog.java` | 21 | 2 repeated 21 times |
| `dialog/RecurringTaskDialog.java` | 6 | 2, 2, 2, 2, 2, 2 |
| `dialog/RenameDialog.java` | 1 | 2 |
| `dialog/RenameProjectDialog.java` | 1 | 2 |
| `dialog/ResourceAdditionDialog.java` | 1 | 2 |
| `dialog/ResourceInformationDialog.java` | 10 | 2, 2, 2, 2, 2, 2, 2, 2, 1, 2 |
| `dialog/ResourceMappingDialog.java` | 4 | 2, 2, 2, 2 |
| `dialog/StatusDateDialog.java` | 1 | 2 |
| `dialog/TaskInformationDialog.java` | 14 | 1, 2, 2, 2, 2, 2, 1, 2, 4, 1, 2, 2, 2, 2 |
| `dialog/TransformParameterDialog.java` | 1 | 2 |
| `dialog/UpdateProjectDialogBox.java` | 7 | 2, 2, 2, 8, 1, 2, 2 |
| `dialog/UpdateTaskDialog.java` | 9 | 2, 1, 2, 2, 2, 1, 2, 2, 1 |
| `dialog/util/FieldComponentMap.java` | 1 | 2 |
| `dialog/XbsDependencyDialog.java` | 1 | 2 |
| `print/PageSetup.java` | 6 | 2, 2, 2, 2, 2, 2 |

## FormLayout and cursor comparison

Cursor maxima below begin at row 1 and add the `nextLine` increments in source
order. They are conservative when a branch is conditional. Dynamic rows from
`FlatUiSupport.preferredFormRows(n)` are expanded using its implementation
(`2n - 1` tracks). Every cursor target is within its declared FormLayout.

| Builder scope | Declared tracks | Cursor maximum | Audit |
| --- | ---: | ---: | --- |
| `AboutDialog.createContentPanel` | 15 | 7 | Pass; 7 preferred rows plus the trailing separator and button rows |
| `AssignmentDialog.createContentPanel` | 3 standalone / 5 team mode | 3 / 5 | Pass; conditional row expression and branch both accounted for |
| `AssignmentDialog.createEditorsButtons` | 4 | 4 | Pass |
| `AssignmentDialog.createButtons` | 8 | 8 | Pass |
| `ReplaceAssignmentDialog.createContentPanel` | 8 | 5 | Pass |
| `TimesheetDialog.createContentPanel` | 3 | 3 | Pass |
| `TimesheetDialog` editor-button panel | 4 | 4 | Pass |
| `BaselineDialog.createContentPanel` | 9 | 9 | Pass; mixed one- and two-track advances plus separator's automatic advance |
| `ChangeWorkingTimeDialogBox.createSettingsPanel` | 29 | 25 | Pass; five `Settings.CALENDAR_INTERVALS` iterations each add two tracks |
| `ChangeWorkingTimeDialogBox.createContentPanel` | 3 | 3 | Pass; separators advance once themselves |
| `NewBaseCalendarDialog.createContentPanel` | 17 | 5 | Pass; generated rows plus a growing final row |
| `ColumnDialog.createContentPanel` | 3 | 2 | Pass |
| `DelegateTaskDialog.createContentPanel` | 5 | 3 | Pass |
| `DependencyDialog.createContentPanel` | 15 | 5 | Pass |
| `FieldAliasDialog.createContentPanel` | 13 | 5 | Pass |
| `FindDialog.createContentPanel` | 9 | 5 | Pass |
| `HelpDialog.createContentPanel` | 27 | 13 | Pass |
| `InformationDialog.createNotesPanel` | 8 | 5 | Pass |
| `LocaleDialog.createContentPanel` | 15 | 14 | Pass; branch-specific rows included |
| `LoginDialog.createContentPanel` | 13 | 7 | Pass |
| `LookupDialog.createContentPanel` | 5 | 5 | Pass |
| `OpenProjectDialog.createContentPanel` | 4 | 3 | Pass |
| `CalendarDialogBox.createContentPanel` | 19 | 19 | Pass |
| `ProjectDialog.createContentPanel` | 28 | 15 | Pass; generated custom-field rows are in their own nested layout |
| `ProjectInformationDialog.createGeneralPanel` | 26 | 25 | Pass; access-control and custom-field branches included, followed by one growable track |
| `ProjectInformationDialog.createStatisticsPanel` | 19 | 19 | Pass |
| `RecurringTaskDialog.createContentPanel` | 13 | 13 | Pass |
| `RenameDialog.createContentPanel` | 9 | 3 | Pass |
| `RenameProjectDialog.createContentPanel` | 7 | 3 | Pass |
| `ResourceAdditionDialog.createContentPanel` | 9 | 3 | Pass |
| `ResourceInformationDialog.createGeneralPanel` | 9 | 9 | Fixed: its five content rows are separated with 3dlu tracks |
| `ResourceInformationDialog.createAvailabilityPanel` | 5 | 3 | Pass |
| `ResourceInformationDialog.createTasksPanel` | 5 | 5 | Pass |
| `ResourceInformationDialog.createCostsPanel` | 8 | 7 | Pass; includes separator's automatic advance |
| `ResourceMappingDialog.createContentPanel` | 13 | 9 | Pass |
| `StatusDateDialog.createContentPanel` | 3 | 3 | Pass |
| `TaskInformationDialog.createTextStylePanel` | 9 | 9 | Pass; separator's automatic advance is included |
| `TaskInformationDialog.createAdvancedPanel` | 21 | 19 | Pass; both separator advances are included |
| `TaskInformationDialog.createResourcesPanel` | 5 | 5 | Pass |
| `TransformParameterDialog.createContentPanel` | 19 | 3 | Pass |
| `UpdateProjectDialogBox.createContentPanel` | 21 | 21 | Pass; includes `nextLine(8)` and separator's automatic advance |
| `UpdateTaskDialog.createContentPanel` | 19 | 19 | Pass; three separator advances are included |
| `XbsDependencyDialog.createContentPanel` | 11 | 3 | Pass |
| `PageSetup.createVerticalPanel` | 16 | 13 | Pass; optional row branch included |

The Project Information general form previously declared 21 tracks although
its non-standalone path advances to row 25 before adding the custom-field
panel. It now uses `FlatUiSupport.preferredFormRows(13)` for 25 tracks and adds
one growable row 26 after it. This preserves the content positions and
standalone custom-field branch while giving all 13 logical content rows
preferred-height tracks separated by explicit 3dlu spacers.

The Resource Information General tab previously declared eleven consecutive
preferred tracks even though its five content rows advance by two tracks each.
The empty intervening tracks were `p` tracks instead of explicit `3dlu` spacing,
so Japanese labels and neighboring controls could crowd one another. It now
uses `preferredFormRows(5)`, aligning its track contract with the other audited
dialogs. Resource Mapping remains within its conditional 7/9-track layout.

## High-risk visual callers

| Area | Shared risk | Regression evidence |
| --- | --- | --- |
| Change Working Time | Repeated `p,3dlu,p` settings rows and nested editors | `ChangeWorkingTimeDialogGuiAcceptanceTest` and `DialogLayoutAssertions` |
| Calendar Options | Repeated settings rows and separator spacing | `TaskInformationRibbonGuiAcceptanceTest.robotCalendarOptionsRibbonRouteOpensUsableDialog` |
| Help / Locale | Fixed legacy rows and long localized labels | `RibbonExternalCommandGuiAcceptanceTest` and `DialogLayoutAssertions` |
| Update Tasks | Mixed separator and content rows | `TaskInformationRibbonGuiAcceptanceTest.robotOpensIssue590DialogsWithoutClippedText` |
| Recurring Task | End-condition controls on separate responsive rows | Same issue #590 Robot journey and range-panel assertion |
| Project Information | Conditional access control and trailing custom fields | `ProjectInformationDialogGuiAcceptanceTest` checks all tabs and post-resize text controls |
| Task Information | Text Style and other tab forms | Task Information Robot tab sweep |
| Find / Resource dialogs | Legacy form rows and button groups | Included in the complete caller inventory above; visual changes use the shared assertion |

The audit is shared-cause based. A per-label pixel offset is not an acceptable
fix. Re-run the inventory command and the cursor comparison whenever a
`FormLayout` or `nextLine` call changes:

```powershell
rg -n '^\s*(?:builder|settingBuilder)\.nextLine\s*\(' modules/microproject_ui/src/main/java/com/microproject/dialog modules/microproject_ui/src/main/java/com/microproject/print/PageSetup.java -g '*.java'
rg -n "new FormLayout|preferredFormRows" modules/microproject_ui/src/main/java/com/microproject/dialog modules/microproject_ui/src/main/java/com/microproject/print/PageSetup.java -g '*.java'
```

## Dialog size and viewport contract (#724)

| Dialog surface | Minimum / preferred size | Placement | Resize behavior |
| --- | --- | --- | --- |
| Baseline / Clear Baseline | Natural `pack()` size is the minimum and the maximum: the dialog is a fixed-size form (#863) | Center on owning document frame (`doModal`) | Not user-resizable; selector keeps its preferred height and width |
| Project Information | Tabbed pane minimum 700×420 logical pixels; initial dialog minimum is its first packed size | Center on owning frame | Tabs and the growable extra-field row expand; text controls retain preferred heights |
| Resource Information | Preferred content 400×275 dlu; initial dialog minimum is its first packed size | Center on owning frame | Tabbed content expands; each form row remains font-preferred |
| Resource Mapping | Natural `pack()` size is the minimum; 310dlu growable table column | Center on owning frame | Association table expands; conditional project and access-control rows remain present |
| Find | Search field requests 30 columns; initial dialog minimum is its first packed size | Center on the active document frame | Search field and form retain preferred heights; direction buttons remain in the button panel |
| AutoFilter | Natural `pack()` size; the value list scrolls inside a 320×280 preferred viewport | Centered on the owning document frame | Fixed-size form: the user cannot resize; a long value set scrolls instead of stretching the window |
| Custom column alias (`FieldAliasDialog`) | Natural `pack()` size; old-name label and one alias input | Centered through `AbstractDialog.pack()` | Fixed-size form: the alias input keeps its preferred height and packed width |
| Status Date | Natural `pack()` size; one date field plus the NA checkbox | Centered by the owning command route | Fixed-size form |
| Move Project | Natural `pack()` size; one date field | Centered by the owning command route | Fixed-size form |
| Update Project | Natural `pack()` size; radio groups and two date fields | Centered through `AbstractDialog.pack()` | Fixed-size form |

`AbstractDialog` owns the contract in one place: `initComponents()` calls
`setResizable(!isFixedSizeForm())`, and `pack()` keeps the first packed size as
the window minimum. `isFixedSizeForm()` defaults to `false`, so table, list, and
tabbed surfaces keep their existing resizable behavior; a dialog whose content
is one bounded form overrides it to `true`.
`DialogLayoutAssertions.assertFixedSizeForm` checks the non-resizable state, a
non-zero packed minimum, font-preferred text control heights, and usable-screen
containment, so a regression that reintroduces a fixed pixel size or a clipped
field fails the same route test that captured its screenshot. Screenshots are
written under `build/reports/guiTest-artifacts/issue-724-<dialog>-<locale>-<scale>.png`.

The 2026-10-05 candidate inventory is partly stale at `e14251d09`.
`LookupDialog`, `OpenProjectDialog`, and `LoginDialog` no longer exist in the
source tree, and `ResourceAdditionDialog` is referenced only by its own factory,
so it has no production route. Those four are removed from the matrix.
`FieldAliasDialog` was listed in the inventory and is now covered through the
task-table header popup (and the Custom Fields &gt; Rename button).
`RenameDialog` is a separate, still-uncovered surface reached by clicking the
current column in the column-list popup.

The shared GUI assertions now exercise the target surface after growth and
check that the top-level window remains inside the usable monitor bounds. The
first successful pack establishes a lower bound, so manual or automated resize
must grow from that size; dialogs should never use fixed pixel offsets to fake
fit on a particular monitor.

## Whole-window inventory (#863)

This section extends the dialog contract above to all top-level windows as
tracked by #863. It reuses the #724 evidence instead of duplicating it: a dialog
already listed in the dialog size table above keeps that row as its single size
contract, and this table records only the window-level audit state.

Inventory taken at `c7d6ff041` (current master, after PRs #1214, #1219, #1209, #1188, #1199) with a source scan of `class ... extends JFrame/JDialog/Frame` under `modules/microproject_ui/src/main/java`. The earlier scan at `8c546d1d5` produced the same class set; no window class was added or removed by those PRs. Counts: 30 `AbstractDialog` subclasses, 10 direct `FlatLafDialog` subclasses, 3 `FlatLafFrame` subclasses, 1 `JFrame`, 1 `JDialog`, plus 2 `DialogEntrySpreadSheet` panels that are not top-level windows.

| Window / surface | Representative state | Blank area intentional? | Evidence / contract source | Audit state |
|---|---|---|---|---|
| MainRibbonFrame / MainFrame | Gantt + task table, secondary project/resource windows | Yes — the empty task table and Gantt canvas are the work area | `RibbonTabGuiAcceptanceTest` (204 source refs), `MultipleLocalProjectOpenGuiAcceptanceTest`, `ResourcePoolInitialViewGuiAcceptanceTest` | Covered by physical routes; no uniform-size change |
| PrintPreviewFrame | Print preview of a page | Yes — margins and page canvas are intentional | `TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute`; `GraphPageable` owns the frame and packs it from the page size | Covered by physical route |
| TaskInformationDialog — General/Text Style/Advanced | Bounded form + growable rows | Mixed: header and form spacing intentional, unused viewport height is not | `TaskInformationRibbonGuiAcceptanceTest.assertTabsUseAvailableScreenHeight` and `assertTaskAssignmentShowsResource`; `scrollableTab()` sizes every tab to one viewport height | Covered; this is the source row for the #724 A10 conclusion |
| TaskInformationDialog — Resources | Assigned resources spreadsheet | Yes — with one assignment the grid has one data row and the rest is an empty spreadsheet canvas; `SpreadSheet` sets `FillsViewportHeight=true` in `FlatUiSupport.applySpreadsheetTableStyle`, so the grid paints through the viewport | `assertTaskAssignmentShowsResource` asserts `assignmentBounds.height * 2 >= resourcesView.getHeight()`; `TaskResourcesPanel` uses `fill:150dlu:grow`; screenshot `issue-460-task-information-ja-default.png` | Covered; #724 A10 conclusion recorded here |
| Preferences / Find / Calendar Options / Change Working Time / Recurring Task / Dependency / Resource Leveling / Baseline / Critical Chain / Team Planner / Timeline | Bounded or table-backed forms | Per the dialog size table above | #724 GUI routes plus the class-specific GuiAcceptanceTests listed in the caller inventory | Covered by #724 contract |
| WelcomeDialog | Start screen, recent-project list | Yes — empty tiles and recent area when no projects | `WelcomeDialogGuiAcceptanceTest` | Covered |
| About / Help / License | Bounded informational forms | Yes — fixed layout | `LicenseDialog` 19 refs, `HelpDialog` 12 refs, `AboutDialog` 4 refs | Covered by route tests |
| CustomReportDialogBox / Assignments (Assignment, ReplaceAssignment, Timesheet) | Table/list-backed forms | Table viewport is the work area | Dedicated GuiAcceptanceTests (7 / 13 / 5 / 8 refs) | Covered |
| PageSetup / FieldDialog / Projects / RenameProject / Gridlines / TransformParameter / XbsDependency | Bounded dialogs reached through a command route | Yes for bounded forms | #724 route tests use command routes, not class names, so the class-name reference count is zero for PageSetup / XbsDependency / TransformParameter / FieldDialog / Projects / RenameProject / Gridlines | Partly covered by #724; these seven need explicit remaining-surface evidence |
| CustomFieldsDialogBox | Custom fields of selected tasks | Yes — bounded list | `FlatLafLegacyDialogRefreshGuiAcceptanceTest` constructs it directly; `DocumentFrame` opens it through the Custom Fields command | Covered by dialog refresh test; route assertion remains in #724 A8 |

No minimum or preferred size was changed while building this inventory. The
Resources-tab conclusion is recorded in the row above: the apparent empty area
is the spreadsheet work canvas, not a layout defect, and the existing
`assertTaskAssignmentShowsResource` plus `FillsViewportHeight=true` evidence
closes #724 A10. The remaining unfilled rows are seven bounded dialogs whose
command-route evidence is tracked in #724 A8; they are deliberately not given a
second size contract here.

## Cross-window resize contract (#863 A3)

`WindowSizeContractSweepGuiAcceptanceTest` opens every constructible top-level
window in one GUI run and asserts the size contract each window declares. This
is the cross-window counterpart to the per-route journeys: the journeys prove
that a window is reachable, this sweep proves that growth and shrinking keep its
text and controls intact.

| Contract | Required result |
|---|---|
| `FIXED_FORM` | The window is not user-resizable, its packed size is its locked minimum, every text control keeps its font-derived preferred height, and no visible descendant leaves a bottom or right band wider than the border inset plus 16 logical pixels. |
| `RESIZABLE` | The window grows by up to 200x140 logical pixels inside the usable screen; text controls keep their font-derived height after growth; the declared work surface (tabbed pane, the scroll pane of a data view, or a paint canvas) absorbs at least half the growth on one axis; the same checks pass again at the packed minimum; and the window never leaves the usable screen. |

The sweep reports one `WINDOW_SIZE_CONTRACT` line per window and writes
`issue-863-resize-<window>-<locale>-<scale>.png` under
`build/reports/guiTest-artifacts/`. Sizes below are the window's declared
initial size in logical pixels at ja/100%; `DialogSizing` windows state their own
initial size instead of packing.

| Window | Contract | Initial size | Work surface |
|---|---|---:|---|
| MainRibbonFrame | RESIZABLE | 1200x720 | Task-table scroll pane |
| AboutDialog | FIXED_FORM | 397x235 | none (bounded form) |
| HelpDialog | FIXED_FORM | 836x343 | none (bounded form) |
| ProjectDialog | RESIZABLE | 579x380 | Notes scroll pane |
| ProjectInformationDialog | RESIZABLE | 617x516 | Tabbed pane |
| StatusDateDialog | FIXED_FORM | 303x153 | none (bounded form) |
| RenameProjectDialog | FIXED_FORM | 451x181 | none (bounded form) |
| BaselineDialog | FIXED_FORM | 346x211 | none (bounded form) |
| UpdateProjectDialogBox | FIXED_FORM | 454x302 | none (bounded form) |
| RecurringTaskDialog | FIXED_FORM | 538x372 | none (bounded form) |
| UpdateTaskDialog | FIXED_FORM | 445x304 | none (bounded form) |
| DelegateTaskDialog | FIXED_FORM | 150x136 | none (bounded form) |
| TaskInformationDialog | RESIZABLE | 749x1025 | Tabbed pane |
| ResourceInformationDialog | RESIZABLE | 699x557 | Tabbed pane |
| ResourceLevelingDialogBox | RESIZABLE | 900x520 | Tabbed pane |
| FieldAliasDialog | FIXED_FORM | 298x194 | none (bounded form) |
| CalendarDialogBox (Calendar Options) | FIXED_FORM | 346x375 | none (bounded form) |
| ChangeWorkingTimeDialogBox | RESIZABLE | 677x549 | Tabbed pane |
| TeamPlannerDialogBox | RESIZABLE | 1120x680 | Canvas scroll pane |
| CalendarViewDialogBox | RESIZABLE | 1100x760 | Calendar canvas |
| TimelineDialogBox | RESIZABLE | 1100x620 | Timeline canvas |
| CustomFieldsDialogBox | RESIZABLE | 900x620 | Table scroll pane |
| CustomReportDialogBox | RESIZABLE | 1200x760 | Column list / preview |
| LocaleDialog | RESIZABLE | 555x498 | External-language table |

### Shared cause fixed by this sweep

`#724` declared the fixed-size contract in `AbstractDialog.isFixedSizeForm()`
and stated that a dialog whose content is one bounded form must override it, but
eight bounded forms still returned the default `false`. Growing any of them
added an empty band under the form, which is the symptom #863 reports. They now
declare `isFixedSizeForm() == true`: About, Help, Rename Project, Clear
Baseline, Recurring Task, Update Task, Delegate Task, and Calendar Options.

The sweep also found a crash in `RenameProjectDialog`: the name document
listener ran while the content panel was still being built and dereferenced the
not-yet-created OK button, so Save As / Rename Project failed with an NPE as soon
as the form was prefilled. `updateNameValidity()` now tolerates the pre-button
state and `createButtonPanel()` re-evaluates it, so the initial enabled state
still matches the prefilled name.

### Windows not covered by the sweep

The sweep constructs windows through their public factories. Windows that exist
only behind a modal static entry point or need a live document/print context are
still covered by their own route journeys, and their resize evidence is not
claimed here:

- `LicenseDialog` (modal-only `showDialog`), `PreferencesDialogBox` (modal
  `showDialog`), `WelcomeDialog` (needs the menu manager), `LocaleDialog`
  variants that need a document frame.
- `AssignmentDialog`, `ReplaceAssignmentDialog`, `TimesheetDialog`,
  `ResourceMappingDialog`, `FindDialog` (need a document frame and assignment
  selection; they keep their #724 route tests).
- `PrintPreviewFrame` (needs a printable document), `ProjectsDialog`,
  `GridlinesDialogBox`, `AutoFilterDialog`, `ColumnDialog`, `DependencyDialog`,
  `XbsDependencyDialog`, `TransformParameterDialog`, `PageSetupDialog`,
  `NewBaseCalendarDialog`, `ResourceAdditionDialog` (need a Gantt view, a
  spreadsheet, a dependency, or a print setup).
