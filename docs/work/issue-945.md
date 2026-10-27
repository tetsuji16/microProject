# Issue #945 — Dark theme for Network, WBS, Critical Chain, and RBS

State: Theme integration implemented; candidate GUI CI rerun pending.
Issue: https://github.com/tetsuji16/microProject/issues/945
Contract frozen at: 2026-10-26, issue body and two comments.
Baseline HEAD: `ec0df6e9e6a4173cbf08301655b902f7c75c9efb`.

## Goal and scope

Make the Network, WBS, Critical Chain Network, and RBS views readable and visually consistent with the application's dark theme. The four-view scope comes from the issue body and replies.

Out of scope: light theme changes, unrelated view redesign, and chart behavior changes.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Network view has readable dark-theme canvas, labels, nodes, edges, and controls | pending | Network/RBS/WBS GUI assertion failed on candidate because foreground was not exposed on the view; corrected in follow-up |
| A2 | WBS view has readable dark-theme canvas, labels, hierarchy, and controls | pending | Candidate view-route assertion reached after Network; full GUI rerun pending |
| A3 | Critical Chain Network view has readable dark-theme canvas, labels, nodes, edges, and controls | pending | Focused pixel test passed; physical GUI evidence pending |
| A4 | RBS view has readable dark-theme canvas, labels, hierarchy, and controls | pending | Full GUI rerun pending |
| A5 | Views retain their current data and interactions in dark and light themes | pending | Module tests passed; GUI rerun pending |

## Implementation and verification

Dark theme semantic tokens cover Network/WBS/RBS form text and view foreground, plus Critical Chain canvas, text, node fills, borders, selection, edges, and legend. Light-theme colors and project-format distinctions remain intact.

On candidate `ee61b2ca0a3cd6cb40baa6543dc1bf795516ac1e`, `:microproject_ui:test --console=plain`, `:microproject_ui:compileGuiTestJava --console=plain`, and the focused Critical Chain pixel test passed. GitHub build passed. GitHub GUI acceptance run `40292081440` executed 53 tests with one failure: `TaskInformationRibbonGuiAcceptanceTest#robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews` found no readable foreground token on Network. Follow-up sets the view foreground and explicit renderer component foregrounds. Focused local Robot verification remained blocked before input by MSPaint PID 25288 overlapping the test window.

## Next step

Run the candidate's Windows GUI acceptance after the Network foreground correction. Close only after all acceptance criteria have passing evidence.
