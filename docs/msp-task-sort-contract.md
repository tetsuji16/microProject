# Task sort contract

Microsoft Project Desktop documents sorting by primary, secondary, and tertiary
fields, with an independent ascending/descending choice for each. Its Sort dialog
also offers “Keep outline structure”; with it selected, summary tasks sort first
and their subtasks sort within each summary. `Application.Sort` defines
`Outline=False` as not preserving task/resource outline levels and says that
`Renumber` is ignored when `Outline=False`.

Sources:

- [Sort tasks, resources, or dates in Project desktop](https://support.microsoft.com/en-us/project/sort-tasks-resources-or-dates-in-project-desktop)
- [Application.Sort method (Project)](https://learn.microsoft.com/en-us/office/vba/api/project.application.sort)
- [Unique ID fields](https://support.microsoft.com/en-us/project/unique-id-fields)

microProject uses the existing user-sorter catalog as its field catalog and
supports up to three distinct keys. Each key has its own direction; Java's stable
list sort preserves encounter order when all keys tie. Missing Start/Finish dates
(the task or assignment model's zero sentinel) sort last in either direction. An empty text value is
a value and sorts by the field's comparator. These two rules are product-defined
because the cited Microsoft pages do not specify missing-value placement or exact
tie order.

With Keep Outline selected, view sorting compares sibling tasks recursively and
does not change model order, WBS, or IDs. Summary rows remain in the projection
when Show Summary is enabled; collapsed summaries keep their descendants hidden.
Filtered-out tasks remain excluded from the displayed projection. Grouping is
applied after sorting and retains its group rows; filtered and grouped task rows
continue to resolve selection by their underlying task identity.

With Keep Outline cleared, task rows form a flat view projection. Shown summary
rows, including the project summary row when enabled, participate as rows;
collapsed descendants are included. Filtering is applied before sorting and
grouping after sorting. The shared table/Gantt projection changes, while task
hierarchy, WBS, IDs, schedule, Undo state, and MPO data remain unchanged. This
view-only behavior is a product contract where the Microsoft source defines the
Outline parameter but not every visual detail. The permanent task-order command
always preserves the outline and retains its confirmation and Undo path; its
documented task-only Renumber behavior does not imply resource reordering.

Sort criteria are transient `ViewConfiguration` state shared by that view type;
MPO files do not store them. The sort options therefore reset with the application
view configuration and are not project data.
