# Issue #945 — Dark theme for Network, WBS, Critical Chain, and RBS

State: Theme integration implemented; visual/GUI verification pending.
Issue: https://github.com/tetsuji16/microProject/issues/945
Contract frozen at: 2026-10-26, issue body and two comments.
Baseline HEAD: `ec0df6e9e6a4173cbf08301655b902f7c75c9efb`.
Baseline working tree: pre-existing edits in docs/MSP_STANDARD_COMMAND_INVENTORY.md and three UI test/source paths; untracked .codex-remote-attachments/, docs/book/, docs/work/issue-941.md, and sample sync sidecars. Preserve all.

## Goal and scope

Make the Network, WBS, Critical Chain Network, and RBS views readable and visually consistent with the application's dark theme. The four-view scope comes from the issue body and replies.

Out of scope: light theme changes, unrelated view redesign, and chart behavior changes.
Blockers: GUI visual evidence and focused regressions remain pending.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Network view has readable dark-theme canvas, labels, nodes, edges, and controls | open | Screenshot assertions and physical view evidence pending |
| A2 | WBS view has readable dark-theme canvas, labels, hierarchy, and controls | open | Screenshot assertions and physical view evidence pending |
| A3 | Critical Chain Network view has readable dark-theme canvas, labels, nodes, edges, and controls | open | Added by issue comment; evidence pending |
| A4 | RBS view has readable dark-theme canvas, labels, hierarchy, and controls | open | Added by issue comment; evidence pending |
| A5 | Views retain their current data and interactions in dark and light themes | open | Focused view/theme regressions pending |

## Current slice

**Changed source scope:** `FormComponent` captions used by Network/WBS/RBS and `CriticalChainGraphPanel`; semantic theme tokens now cover dark canvas, text, node fills, borders, selection, edges, and legend. Light-theme colors and project-format distinctions remain intact.
**Verification:** On candidate `ee61b2ca0a3cd6cb40baa6543dc1bf795516ac1e`, `:microproject_ui:test --console=plain` and `:microproject_ui:compileGuiTestJava --console=plain` passed. The focused Critical Chain pixel test passed. Focused Network/WBS/RBS Robot attempt on identical scoped code stopped before input when MSPaint (PID 25288) overlapped the window. Critical Chain physical color assertions are compiled but have not run. Exact-candidate GitHub CI remains pending.
**Next slice:** Run the exact-candidate GitHub Windows GUI acceptance and inspect its four-view screenshots/assertions.
**Done when:** each dark-theme view has focused checks and physical dark-theme GUI evidence.

## Remaining blockers

- Physical Robot execution for all four views remains pending because of the foreground overlap; exact-candidate CI remains pending.

## Latest evidence

- Comments expand target scope to Critical Chain Network and RBS.
- Tests/build: candidate `ee61b2ca0a3cd6cb40baa6543dc1bf795516ac1e` — UI module tests and GUI test compilation passed; focused Critical Chain rendering pixel test passed.
- Physical GUI: blocked before Robot input by MSPaint PID 25288 (`gui-test-contended-1793002342258-44696.txt`); screenshot/view assertions remain unobserved.
- CI: none.
