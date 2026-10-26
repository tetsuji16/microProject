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
**Verification:** `:microproject_ui:test --tests "com.microproject.dialog.CriticalChainApplyAndRenderTest" --console=plain` passed; the complete `:microproject_ui:test --console=plain` suite and `:microproject_ui:compileGuiTestJava --console=plain` passed on the working tree based on `ec0df6e9e6a4173cbf08301655b902f7c75c9efb`. The focused Network/WBS/RBS Robot test stopped before input when MSPaint (PID 25288) overlapped the test window. Critical Chain physical color assertions are compiled but have not run. Exact-candidate CI remains pending.
**Next slice:** Run focused rendering checks and physical GUI visual checks for all four views in dark and light themes.
**Done when:** each dark-theme view has focused checks and physical dark-theme GUI evidence.

## Remaining blockers

- Physical Robot execution for all four views remains pending because of the foreground overlap; exact-candidate CI remains pending.

## Latest evidence

- Comments expand target scope to Critical Chain Network and RBS.
- Tests/build: UI module tests and GUI test compilation passed; focused Critical Chain rendering pixel test passed.
- Physical GUI: blocked before Robot input by MSPaint PID 25288 (`gui-test-contended-1793002342258-44696.txt`); screenshot/view assertions remain unobserved.
- CI: none.
