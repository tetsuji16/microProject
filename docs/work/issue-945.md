# Issue #945 — Dark theme for Network, WBS, Critical Chain, and RBS

State: Partial; Network/WBS/RBS dark rendering and physical screenshots pass on the multi-node construction sample, including light/dark transitions. Critical Chain physical evidence exists; exact-candidate CI remains pending.
Issue: https://github.com/tetsuji16/microProject/issues/945
Contract frozen at: 2026-10-27, issue body and comments through https://github.com/tetsuji16/microProject/issues/945#issuecomment-6434040983.
Tested HEAD: `e695b33ac2e8453774d13eca134b59e207bc71f5` plus current uncommitted changes.
Baseline working tree: pre-existing edits in docs/MSP_STANDARD_COMMAND_INVENTORY.md and three UI test/source paths; untracked .codex-remote-attachments/, docs/book/, docs/work/issue-941.md, and sample sync sidecars. Preserve all.

## Goal and scope

Make the Network, WBS, Critical Chain Network, and RBS views readable and visually consistent with the application's dark theme. The four-view scope comes from the issue body and replies.

Out of scope: light theme changes, unrelated view redesign, and chart behavior changes.
Blockers: Exact-candidate CI remains pending; selected-node/high-contrast coverage is outside this slice.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Network view has readable dark-theme canvas, labels, nodes, edges, and controls | pass | `modules/microproject_ui/build/reports/guiTest-artifacts/issue-945-network-dark.png`; multi-node/edge physical capture, dark pixel ratio 97.58%, bright 0%; visible connector color `#8a8a8a`, contrast 4.61 against `#202226` |
| A2 | WBS view has readable dark-theme canvas, labels, hierarchy, and controls | pass | `issue-945-wbs-dark.png`; multi-level hierarchy, default card fill `#202226`, text `#e3e5e8`, contrast 12.62, screen dark ratio 99.13% |
| A3 | Critical Chain Network view has readable dark-theme canvas, labels, nodes, edges, and controls | pass | Physical Robot route passed and image exists at `issue-945-critical-chain-network-dark.png`; typography assertion tolerates antialiasing |
| A4 | RBS view has readable dark-theme canvas, labels, hierarchy, and controls | pass | `issue-945-rbs-dark.png`; multiple resource cards, dark default fill, text contrast 12.62, screen dark ratio 99.57% |
| A5 | Views retain their current data and interactions in dark and light themes | partial | Network/WBS/RBS GUI route switches dark→light→dark and preserves node counts; Critical Chain transition coverage remains pending |

## Current slice

**Changed source scope:** `FormComponent` chooses text contrast from the dominant rendered texture color and uses cached rendering-only dark clones for exact default card signatures. Custom color/pattern/shape/stroke settings remain unchanged. `NetworkRenderer` adjusts only the exact default `Link.link1` dark color. No format data is mutated or persisted. The GUI route offscreen-renders real forms and physically captures the multi-node sample.
**Root-cause review:** `TexturedShape.build()` wraps even `TRANSPARENT` with a white background while defaults use thick framed strokes. Before the fix, WBS/RBS offscreen output was 17,522 white vs 1,340 black pixels, despite `shape.getColor()=#000000`; the white background was the true card fill. Exact untouched built-ins now render with semantic dark surface/border and thinner frames. PERT default task and critical cards preserve blue/red pattern marks on the dark surface. Default links use Visual Studio Fluent `SystemFillSolidNeutral` dark value `#8a8a8a`; the physical screenshot contains 1,136 matching connector pixels and contrast is 4.61:1 on `#202226`. This Microsoft token is from Visual Studio's palette, not an MSP-specified color: https://learn.microsoft.com/visualstudio/extensibility/ux-guidelines/theme-color-token-reference. Microsoft Project documents user choice of shape/pattern/color, but no dark RGB; the Office theme page similarly omits chart RGB: https://support.microsoft.com/en-us/project/format-the-bar-chart-of-a-gantt-chart-view, https://support.microsoft.com/en-us/office/foundations-experiences/the-new-look-of-office. Windows color guidance emphasizes consistent semantic color use: https://learn.microsoft.com/en-us/windows/win32/uxguide/vis-color.
**Verification:** `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.network.rendering.NetworkFormFontResolverTest" --console=plain` passed (4 tests). `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews" --max-workers=1 --no-daemon --console=plain` passed using read-only `samples/Commercial construction project plan.pod`; physical screen pixel ratios: Network dark 97.58%, WBS dark 99.13%, RBS dark 99.57%, all bright 0%. Network connector `#8a8a8a` achieved contrast 4.61 and 1,136 captured pixels. WBS/RBS actual renderer fills `#202226`, text `#e3e5e8`, contrast 12.62; light transition fill `#ffffff`, text `#1f2937`, contrast 14.68. `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.CcpmSampleProgressGuiAcceptanceTest.mixedProgressSampleRendersInGanttAndCcpmStatusDialogs" --max-workers=1 --no-daemon --console=plain` passed with Critical Chain physical evidence. Restored-dark assertion false negative was corrected to check relative luminance because components use intermediate dark shades.
**Review follow-up (2026-10-28):** The contrast gate now renders each actual format individually and measures that render's local fill, preventing a high-area sibling form from masking a failing card. The light→dark route captures the physical view after restoration and checks dark canvas/card coverage plus Network connector pixels, alongside per-format rendered colors. A first GUI rerun exposed a test-only false failure: the light-theme capture name triggered the dark-pixel assertion (`dark=2.1%`, `bright=94.5%`); light captures are now excluded from that dark-only check, while restored-dark captures remain checked. `NetworkRenderer` resolves the default dark arrow through an identity-keyed cached copy, avoiding `TexturedShape` allocation on every paint. Cache entries validate source color, paint, shape identity, stroke, scales, and target theme color; original arrows remain unchanged. `DarkLinkArrowShapeCacheTest` covers reuse and invalidation for scale, shape, and theme-color changes.

**Verification:** `gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.network.DarkLinkArrowShapeCacheTest" --tests "com.microproject.pm.graphic.network.rendering.NetworkFormFontResolverTest" --console=plain` passed. `gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews" --max-workers=1 --no-daemon --console=plain` passed after correcting the light-capture assertion; the route covers Network/WBS/RBS dark, light, and restored-dark physical views. `git diff --check` passed.

**CI follow-up (2026-10-28):** GUI CI runs `40593939402` and `40595116620` both reported row-header drag (default cursor after two drag events) and Ribbon Link (lock rejection not observed). The no-main-format NPE in the Network contrast helper was reproduced with an empty main-format selection, fixed in `NetworkCellRenderer`/`NetworkRenderer`, and covered by `NetworkCellRendererTest.skipsNodeWithNoMainBarFormat`; the physical Network/WBS/RBS route passed after the fix. Added failure snapshots for row drag and Link. A local combined run reproduced both outstanding symptoms: row header reports `dragEnabled=false`, `dropTarget=null`, and only the Basic Swing transfer handler before release; Link command result is `REJECTED/task-selection-not-stable`, `tryAcquireTasks` was never called, and dependency/Undo state stayed unchanged. These observations localize each failure before the expected operation but do not establish why CI and local isolated runs differ. Do not classify as flaky or environmental; retain exact-candidate GUI diagnostics/reproduction as pending.

**Next slice:** Exact-candidate CI remains pending; investigate why the drag target is not initialized and why selected Link tasks fail stability validation before lock acquisition.
**Done when:** each dark-theme view has focused checks and physical dark-theme GUI evidence.

## Remaining blockers

- Exact-candidate CI, selected-node state, and high-contrast OS theme coverage remain pending.

## Latest evidence

- Comments expand target scope to Critical Chain Network and RBS.
- Tests/build: Network form unit tests (4) and physical Robot routes for Network/WBS/RBS and Critical Chain passed.
- Physical GUI: `modules/microproject_ui/build/reports/guiTest-artifacts/issue-945-{network,wbs,rbs}-dark.png`; screenshots show dark patterned cards, connected nodes, WBS hierarchy, and multiple RBS resources on dark canvases.
- Release check: v0.1.0.1276 was published 2026-10-27T04:15Z; commit `6d430c159` with theme changes is dated 2026-10-26T17:21+09:00 and is not an ancestor of release commit `6ac176f3db5f1eeb313570cb71f476f7ea13ed11`.
- CI: none.
