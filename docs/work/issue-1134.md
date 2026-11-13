# Issue 1134: workspace restore does not notify existing views of the time-scale change

State: implementation and local verification complete; CI on the candidate commit pending.
Baseline: `8e14a8998`.

## Cause

`CoordinatesConverter.restoreWorkspace` replaced the scale index, origin and end without firing a
time-scale event. At start-up that is harmless because the views are created afterwards. When a file is
opened, `GraphicManager.initializeCollaboration` -> `DocumentFrame.restoreWorkspace` restores the saved
workspace into a frame whose Gantt view already exists. The chart then kept the width of the previous range
(`Gantt.updateSize` runs only from the notification), and `ScaledScrollPane` did not record the left-edge
date of later user scrolls, because it ignores viewport changes while the origin differs from the origin of
the last notification. The next origin change (reaching the leading edge, or an edit that moves the earliest
date) then moved the chart back to the date that was visible before the restore.

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | A restored time range resizes a view that already exists | Pass: `ScaledScrollPaneTest.workspaceRestoreOfTheTimeRangeUpdatesExistingViews`. On the baseline `CoordinatesConverter` it fails at the width assertion. |
| A2 | The restore keeps the date at the left edge | Pass: the same test. |
| A3 | A user scroll after the restore survives the next origin change | Pass: the same test scrolls 150 px and calls `extendViewBefore(30)`. |
| A4 | A restore that changes nothing fires no event | By construction: the event type is built from the values that changed and nothing is fired for 0. |

## Design note

The event type is the combination of what changed: `SCALE_CHANGE`, and `ORIGIN_AND_END_CHANGE` or
`END_ONLY_CHANGE`. `ScaledScrollPane` reads the type as a bit mask, the other listeners ignore it.

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.timescale.ScaledScrollPaneTest"`: pass (8 tests); the new test fails on the baseline `CoordinatesConverter`.
- `.\gradlew.bat :microproject_ui:test`: 1450 tests, 2 failed locally
  (`IconManagerRibbonIconTest.taskTreeExpandersPreserveTheirNinePixelRasterShapeAndScaleAsVectors`,
  `MicrosoftProjectGanttPaletteTest.mapsSummaryAndBaselineFormatsToTheirMicrosoftProjectColors`).
  These are the order-dependent local failures recorded in `docs/work/issue-1126.md`; they pass alone.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: see the pull request.

## Limits

- Found by code review after issue 1126; not observed in a running application.
- The workspace stores the scroll position in pixels. A view created for the first time after the origin
  has changed since the save shows a different date than it did when saved. Not changed here.
- The saved per-view state (scroll position, divider, columns) appears not to be applied to views that
  already exist when a file is opened, because `restoreWorkspaceFor` runs only when a view is created.
  Not verified in a running application and not changed here.
