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
| Baseline / Clear Baseline | Natural `pack()` size is the minimum; baseline selector column grows from its preferred width | Center on owning document frame (`doModal`) | Selector keeps its preferred height; button panel remains at the bottom |
| Project Information | Tabbed pane minimum 700×420 logical pixels; initial dialog minimum is its first packed size | Center on owning frame | Tabs and the growable extra-field row expand; text controls retain preferred heights |
| Resource Information | Preferred content 400×275 dlu; initial dialog minimum is its first packed size | Center on owning frame | Tabbed content expands; each form row remains font-preferred |
| Resource Mapping | Natural `pack()` size is the minimum; 310dlu growable table column | Center on owning frame | Association table expands; conditional project and access-control rows remain present |
| Find | Search field requests 30 columns; initial dialog minimum is its first packed size | Center on the active document frame | Search field and form retain preferred heights; direction buttons remain in the button panel |

The shared GUI assertions now exercise the target surface after growth and
check that the top-level window remains inside the usable monitor bounds. The
first successful pack establishes a lower bound, so manual or automated resize
must grow from that size; dialogs should never use fixed pixel offsets to fake
fit on a particular monitor.
