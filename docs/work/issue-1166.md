# Issue #1166 — Microsoft Project ribbon order

State: IN PROGRESS. Baseline `c15b635ef`. Original contract: compare every in-scope Microsoft Project desktop ribbon tab against the published ProjectControls workbook, including group order, control order and menu/split-button order, with no unexplained differences. Preserve all four original phases: core tabs; view-format contextual tabs; report design/table/chart/drawing/picture tools; and applicable desktop agile tools. Connected Project Online engagement features are outside product scope. #1180 tracks remaining actions and verification; its closure is not a substitute for this parent contract.

| Acceptance | State |
|---|---|
| Core tab and menu order and working controls | Open: known missing commands and groups remain in the order/spec diff tests. |
| View contextual tabs | Open: Task Form and Resource Form applicability and missing controls require evidence; existing tabs are compared by `MspRibbonOrderDiffTest`. |
| Report tool contextual tabs | Open: current report preview has no Microsoft-style report designer; the order test does not yet compare these tabs. Microsoft documents creating reports and selecting charts, tables, shapes and pictures in [Create a Project report](https://support.microsoft.com/en-us/project/create-a-project-report). |
| Desktop agile tabs | Open: desktop feature applicability and implementation are not established. Project Online engagement controls remain outside scope. |
| All in-scope tabs compared with no unexplained differences | Open. `MspRibbonOrderDiffTest.TABS` currently omits report tools and several specified contextual tabs. |

## Active slice — correct the scope inventory

The current repository instructions explicitly keep VBA/macros, COM automation/add-ins and local Office add-ins as unresolved desktop goals. The ribbon inventory nevertheless classified the Macros and Visual Basic commands as excluded based on a historical decision. Microsoft documents desktop macro recording at [Record a macro](https://support.microsoft.com/en-us/project/record-a-macro) and local VBA/add-ins at [Project client programming](https://learn.microsoft.com/en-us/office/client-developer/project/project-client-programming). Reclassify absent commands as required, known missing gaps rather than exclusions. The inventory also describes Visual Reports as unverified local Excel/Visio interoperability while the test excluded the command; reconcile it as a required missing route, not proof of implementation.

Done for this slice when the inventory, spec and both ribbon diff tests consistently expose these gaps and focused tests pass. This does not implement macros, add-ins, Visual Reports, or close #1166. Next: implement a finite report-authoring slice with working model, selection and persistence before exposing its contextual tools; do not add placeholder tabs.

Verification on this candidate: `./gradlew.bat :microproject_ui:test --tests "com.microproject.ui.ribbon.MspRibbonSpecDiffTest" --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --console=plain` passed. `python -m unittest discover -s scripts/tests` passed 26 tests. The current missing entries remain explicit ratchet gaps, not implemented or excluded features.
