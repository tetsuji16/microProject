# Issue #550 — command outcome matrix

The command contract is intentionally verified at two levels: one shared
canonical-route fixture checks the outcome envelope, while the Robot fixtures
click one representative from each presentation family. This avoids copying
the same model assertions into every Ribbon/menu/popup/shortcut test.

| Command family | Physical representatives | Shared outcome assertions | Persistence / history evidence |
|---|---|---|---|
| Clipboard (`COPY`, `CUT`, `PASTE`, `PASTE_INSERT`) | Ribbon clipboard buttons; task popup Paste Insert; shortcut root pane | stable command id, one dispatch | `TaskInformationRibbonGuiAcceptanceTest.copyCutPasteThroughRibbonUseTheSharedEditPipeline` |
| Hierarchy (`INDENT`, `OUTDENT`, `EXPAND`, `COLLAPSE`) | Ribbon, task popup, `Alt+Shift`/outline shortcuts | affected task identity and active view remain stable | `indentAndOutdentSelectedTaskThroughRibbonRoundTripsHierarchy`, popup indent persistence, undo/redo tests |
| Dependencies (`LINK`, `UNLINK`) | Ribbon/menu/popup and `Ctrl+F2`/`Ctrl+Shift+F2` | exactly one route, stable selected task ids and status | `GraphicManagerLinkRouteTest`, dependency GUI acceptance and MPO round trips |
| Task mutation (`INSERT`, `DELETE`) | Ribbon/menu/popup and Insert/Delete shortcuts | one `CHANGED`/`REJECTED` outcome with selection repair | Ribbon delete Robot save/reload and undo/redo coverage |

`CommandRouteMatrixTest` is the common outcome fixture. For every `CommandId`
it asserts exactly one invocation, stable `commandId`, `CHANGED` status,
affected task ID `[42]` (fixture identity), and active view `task`. Physical
tests then prove that the representative input reaches this same canonical
route. Save/reload and Undo/Redo remain assertions of the affected physical
scenario, rather than duplicated in the matrix.

Verification command:

```powershell
.\gradlew.bat :micrproject_ui:test --tests "com.microproject.pm.graphic.frames.CommandRouteMatrixTest" --console=plain
.\gradlew.bat :micrproject_ui:guiTest --tests "com.microproject.ui.ribbon.RibbonTabGuiAcceptanceTest.robotClicksEveryStandardRibbonCommandOnce" --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.copyCutPasteThroughRibbonUseTheSharedEditPipeline" -PguiTestLocale=ja -PguiTestUiScale=1 --max-workers=1 --no-daemon --console=plain
```

Serial execution on 2026-09-13 completed successfully in 44 seconds. The
focused GUI report contains both requested physical representatives with
`tests=1`, `failures=0`, and `errors=0`. The Ribbon sweep physically clicked
every standard Ribbon command except the documented popup-only Paste Insert;
the clipboard case exercised the real Ribbon edit pipeline. The remaining
hierarchy, dependency, popup and shortcut representatives are covered by the
named Robot/route tests in the matrix above and the common
`CommandRouteMatrixTest`. The unrelated CCPM settings GUI failure is tracked
outside this command-family issue.
