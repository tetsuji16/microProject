# Issue 1126: Gantt viewport drift while editing the task table

State: implementation and local verification complete; CI on the candidate commit pending.
Baseline: `b29cbc0c0`.

## Cause

`ScaledScrollPane` keeps the date at the left edge of the chart when the coordinate origin moves
(`restoreViewportLeftDate`). It registers its time-scale listener after the chart, and
`CoordinatesConverter.fireTimeScaleChanged` notifies listeners in reverse registration order, so the pane
restores the position before the chart (`Gantt.timeScaleChanged` -> `updateSize`) has widened the view for the
new range. The scroll pane then clamps the position to the previous view width. The clamped position was
recorded as the new left-edge date, so every origin change that needed more room than the old view had
moved the visible range towards earlier dates. Origin changes come from reaching the leading edge
(`extendRangeAtEdge`, 30 days each) and from schedule edits that move the earliest date; a chart narrower
than its date range is affected, a chart wider than its range is not.

## Contract and results

| ID | Requirement | Result and evidence |
|---|---|---|
| A1 | An origin change keeps the left-edge date even when the view is resized after the pane is notified | Pass: `ScaledScrollPaneTest.originChangeKeepsTheLeftDateWhenTheViewGrowsAfterThePaneIsNotified`. On the baseline class it fails with `x=810, view width=2610`. |
| A2 | Schedule growth (later finish dates) does not pan the chart | Pass: the same test extends the end 20 times. |
| A3 | A user scroll after a restore is not pulled back | Pass: the same test scrolls 100 px and changes the range again. |
| A4 | Repeated task-table duration edits keep the left-edge date and the divider in a narrow production chart | Pass: `TaskTableGanttGridGuiAcceptanceTest.repeatedPhysicalDurationEditsKeepTheGanttLeftEdgeDate`. This test also passes on the baseline, so it is a guard for the reported workflow, not a reproduction. |

## Design note

A restore that the scroll pane clamps is kept pending; the pane repeats it from its viewport change
listener when the chart resizes the view within the same notification, and does not overwrite the
remembered date with the clamped position meanwhile. The pending state is cleared on the next event-queue
turn, so a view that never grows cannot pull a later user scroll back. This does not depend on listener
registration order.

Range extension at the edges is unchanged. It also runs for scroll bar events caused by layout, and that is
what leaves room to scroll before the first date; limiting it to user input would remove that room, because
a scroll bar at position 0 produces no further events.

## Verification

- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.timescale.ScaledScrollPaneTest"`: pass (7 tests); the new test fails on the baseline `ScaledScrollPane`.
- `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.repeatedPhysicalDurationEditsKeepTheGanttLeftEdgeDate"`: pass.
- `.\gradlew.bat :microproject_ui:test`: 1446 tests, 2 failed locally
  (`IconManagerRibbonIconTest.taskTreeExpandersPreserveTheirNinePixelRasterShapeAndScaleAsVectors`,
  `MicrosoftProjectGanttPaletteTest.mapsSummaryAndBaselineFormatsToTheirMicrosoftProjectColors`).
  The same two fail on the unchanged base sources in the full run and pass when run alone, so
  they are order-dependent local failures unrelated to this change.
- `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`: pass.

## Limits

- The gradual drift was not reproduced in a running application. The fix removes a confirmed cause that matches the report (a narrow chart, repeated edits, the range moving one way); another cause cannot be excluded.
