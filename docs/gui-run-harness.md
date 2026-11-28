# GUI run harness

Every `guiTest` test is recorded by `GuiRunRecorder` (a JUnit 5 extension registered in
`src/guiTest/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`). The record is
written to files only and does not change a test's result unless `microproject.gui.failOnUncaught`
is `true`. Files go under `<microproject.gui.artifacts.dir>/runs/<class>/<test>/`
(default `modules/microproject_ui/build/reports/guiTest-artifacts`).

## Files per test

| File | Content |
|---|---|
| `events.jsonl` | One JSON object per line. Common items: `seq`, `t` (ms since the test started), `wall` (ISO-8601), `type`. |
| `summary.json` | `class`, `method`, `status` (`passed`/`failed`/`aborted`/`disabled`), `durationMillis`, `counts`, and `failure` (type, message, first 20 stack lines) for a failed test. |
| `state-<n>.json` | One per snapshot: `gantt`, `view`, `project`, `tasks`. A `final` snapshot is always taken after the test. |
| `edt-stall-<n>.txt` | All-thread dump taken when the EDT stopped answering. |
| `components-final.json` | Failed tests only: the component tree of every showing window (class, name, text, visible, enabled, screen rectangle). |

`runs/summary.json` (one row per test plus totals) and `runs/report.html` (one self-contained table,
rows with failures, `uncaught`, `log-severe`, `edt-stall` or `edt-violation` first) are written by the
`guiRunReport` task, which runs after every `guiTest`.

## Event types

- `test-start`, `test-end`: bounds of the test.
- `step`: written by `GuiRunRecorder.step(name, attributes)`. The `gantt` item holds the visible chart's
  `leftEdge`/`rightEdge` (UTC ISO-8601) and `leftEdgeMillis`/`rightEdgeMillis`, or `null` without a Gantt view.
- `snapshot`: written by `GuiRunRecorder.snapshot(label)`; `file` names the `state-<n>.json`.
- `log`: a `java.util.logging` record of `WARNING` or higher (root logger and `com.microproject.runtime`).
- `uncaught`: an exception no thread handler caught. The previous default handler is still called and restored afterwards.
- `edt-stall`: the EDT did not answer for 500 ms. One event per stall; `durationMillis` is added when it answers again.
- `edt-violation`: `RepaintManager.addInvalidComponent`/`addDirtyRegion` called outside the EDT, with the top 15 stack lines.
  A `repaint()` called directly by application code is allowed and is not recorded.

`counts` in `summary.json`: `log-warning`, `log-severe`, `uncaught`, `edt-stall`, `edt-violation`, `step`, `snapshot`.

## State snapshot

`GuiStateSnapshot` reads on the EDT. `gantt` holds `leftEdge`, `leftEdgeMillis`, `rightEdge`, `rightEdgeMillis`,
`originMillis`, `viewportX`, `viewportWidth` (`null` when the Gantt chart is not the active top view). Times are stored
GMT-read milliseconds and written as UTC (ADR-0003). `view`, `project` (with `undo`) and `tasks` follow the issue text.
The task list comes from `ProjectStateSnapshot`, which needs only a `Project`. A test makes the recorder find its
window with `GuiRunRecorder.attach(manager)`; otherwise it uses a showing `MainRibbonFrame`.

## Options

- `-PguiTestJfr=true`: starts a Java Flight Recording per test JVM, written to `<artifacts>/jfr/`.
- `-PguiTestFailOnUncaught=true` (system property `microproject.gui.failOnUncaught`): a test with an `uncaught`
  event or a `log-severe` record fails in its `afterEach`. Default `false`. `GuiRunRecorderTest` itself provokes both
  and so fails under this option.
