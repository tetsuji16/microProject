# MSP task-table mouse contract

The task table follows Microsoft Project desktop behavior. The task row is
the selection unit, while the clicked field remains the active cell used by
F2, keyboard navigation, and field commands.

| Gesture | Result |
| --- | --- |
| Left click on a task cell | Select that task row across all visible columns and make the clicked column active. Finish an in-progress edit first. |
| Ctrl+left click | Toggle the clicked task row without losing the existing task selection. |
| Shift+left click | Extend the task selection from the selection anchor to the clicked row. |
| Left click on the name expander | Expand or collapse the task; it does not open Task Information. |
| Double-click on a task cell | Open Task Information for the task represented by the clicked view row. The cell editor is not started. |
| Double-click on the Notes field | Open Task Information directly on the Notes tab. |
| Right click on a task cell or ID row | Select the clicked task when it is not already selected, then show the task context menu for that row. |
| Drag the ID row | Select rows while dragging; on release, move the selected tasks only when the destination is valid. |

The row and table surfaces use the same clicked-row resolver. A stale
document selection must never change which task is opened by a double-click.
Non-task sheets retain their existing field-editing behavior.

The Notes-tab rule follows Microsoft's `Notes fields` guidance: double-clicking
the Notes field leads to the Task Information dialog's Notes tab. The
implementation identifies that field by the stable `Field.notes` ID, not by its
localized header. See
<https://support.microsoft.com/en-us/project/notes-fields>.
