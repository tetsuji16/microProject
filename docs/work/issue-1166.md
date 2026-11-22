# Issue #1166 — Microsoft Project ribbon order

State: IN PROGRESS. Baseline `c15b635ef`. Original contract: compare every in-scope Microsoft Project desktop ribbon tab against the published ProjectControls workbook, including group order, control order and menu/split-button order, with no unexplained differences. Preserve all four original phases: core tabs; view-format contextual tabs; report design/table/chart/drawing/picture tools; and applicable desktop agile tools. Connected Project Online engagement features are outside product scope. #1180 tracks remaining actions and verification; its closure is not a substitute for this parent contract.

| Acceptance | State |
|---|---|
| Core tab and menu order and working controls | Open: known missing commands and groups remain in the order/spec diff tests. |
| View contextual tabs | Open: Task Form and Resource Form applicability and exclusion documented (no lower-pane split form view exists); existing 10 contextual tabs are compared by `MspRibbonOrderDiffTest`. |
| Report tool contextual tabs | Open: current report preview has no Microsoft-style report designer; the order test documents these tabs as unresolved phase 3 gaps. Microsoft documents creating reports and selecting charts, tables, shapes and pictures in [Create a Project report](https://support.microsoft.com/en-us/project/create-a-project-report). |
| Desktop agile tabs | Open: desktop feature applicability and implementation are not established; tracked as phase 4 unresolved gaps in the order test. Project Online engagement controls remain outside scope. |
| All in-scope tabs compared with no unexplained differences | Verified at tab level: `everyMicrosoftTabIsComparedOrHasADocumentedExclusionOrUnresolvedReason` asserts that every one of the 37 tabs from Microsoft's published control list is either compared in `TABS` or documented with an explicit reason in `EXCLUDED_TABS` / `UNRESOLVED_TABS`. |

## Active slice — record tab-level exclusions and unresolved desktop tabs

`MspRibbonOrderDiffTest` now locks the full tab-level inventory: every tab in `msp-ribbon-order.csv` (37 tabs in total derived from Microsoft's published ProjectControls workbook) must either be compared in `TABS` or classified with an explicit reason in `EXCLUDED_TABS` (connected cloud features: Resource Plan, Engagements, Share, Publish) or `UNRESOLVED_TABS` (Developer/VBA, Task Form, Resource Form, Compare Projects, Agile tabs, Report Tool tabs, and Backstage pages).

Done for this slice when `everyMicrosoftTabIsComparedOrHasADocumentedExclusionOrUnresolvedReason` passes and no unexplained tab remains. Next: implement a finite report-authoring slice with working model, selection and persistence before exposing its contextual tools; do not add placeholder tabs.

Verification on this candidate: `./gradlew.bat :microproject_ui:test --tests "com.microproject.ui.ribbon.MspRibbonSpecDiffTest" --tests "com.microproject.ui.ribbon.MspRibbonOrderDiffTest" --console=plain` passed. `python -m unittest discover -s scripts/tests` passed 24 tests. All 37 published tabs are explicitly accounted for.
