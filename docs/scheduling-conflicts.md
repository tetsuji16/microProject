# Task mode and dependency-conflict specification

microProject follows the Microsoft Project desktop behavior for newly entered tasks.

- A newly entered task is manually scheduled by default.
- Switching a task to manual scheduling preserves its current start and finish dates.
- A manually scheduled task is not moved by later dependency recalculation. If its dates violate an enabled FS, SS, FF, or SF predecessor link (including lag), microProject keeps the entered dates and displays a warning indicator. The Task Information diagnostics tab explains the conflict and recommends corrective actions.
- Switching a task to automatic scheduling immediately recalculates it from dependencies, duration, calendars, and constraints.
- Automatically scheduled tasks normally move to satisfy predecessor links. If a date constraint prevents a consistent result, diagnostics report the dependency conflict and the existing constraint/negative-slack conditions.
- Inactive tasks and disabled links do not produce dependency-conflict warnings.
- Imported tasks retain the task mode supplied by the source format. Existing serialized project files retain their stored mode.

The default Entry table exposes the **Manually Scheduled** checkbox. Checked means manual scheduling; clearing it switches the task to automatic scheduling.
