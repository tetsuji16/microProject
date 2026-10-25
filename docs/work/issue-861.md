# Issue #861 — HiDPI information and ribbon icons

State: CLOSED (PR #913 merged; issue closed)
Issue: https://github.com/tetsuji16/microProject/issues/861
Contract frozen: 2026-10-24
Integration base: `7cbc3beba28adc16f46b1a8bcd9af2c0e20e9ef9`
Candidate commit: `24ceaa220d468b7afe42437bb4910b0956b66691`
Merged PR: [#913](https://github.com/tetsuji16/microProject/pull/913), head `70027b6fa803fc8ab40b6ab84e0d04f069f399f7`, merged 2026-10-24.

## Goal and scope

Fix blurry information icons at physical Windows 100%, 125%, and 150% scaling.
Cover the task-table Indicators header, dialog/ribbon information icons, and
user-reported Outline, Schedule, and Editing ribbon groups. Preserve the blue
indicator meaning, icon dimensions and accessible role; preserve normal,
disabled, and tinted state behavior for affected ribbon SVGs. Unrelated icons
and a general `getIcon` rewrite are out of scope.

Root cause: the task header used a 16x16 PNG, and `getRibbonIcon` rasterized SVG
sources before Swing paint. The candidate now uses cached `FlatSVGIcon` for all
ribbon SVGs. Disabled/tinted wrappers render at the active graphics scale,
then apply the existing pixel transform; PNG/raster fallback remains. State
wrappers preserve `ImageIcon.getImage()` and non-Graphics2D behavior.

## Acceptance

| ID | Requirement | State | Evidence |
|---|---|---|---|
| A1 | Before/after physical Windows 100%, 125%, 150% | pass | Six CUA screenshots and DPI readings in the manifest below. |
| A2 | Separate physical and simulated scale evidence; confirm cause | pass | PNGs are physical Windows captures. Java2D `Graphics2D.scale` renders are simulations at `modules/microproject_ui/build/reports/issue-861/{before,after}`. |
| A3 | Vector rendering preserves meaning, dimensions, cache and state behavior | pass | Implemented in candidate `24ceaa2`; reviewed by Sol. Raster fallback remains. |
| A4 | Verify affected icons, state variants, dimensions, visibility and accessible role | pass | Focused icon/ribbon tests and full UI module pass; physical screenshots show the reported groups. |

## Current slice

All acceptance criteria passed. The user's original physical scale is restored
to 100%, and issue #861 is closed.

## Verification

- `:microproject_ui:test --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` — 317 tests, 0 failures.
- `:microproject_ui:test --tests "com.microproject.ui.shell.MicroProjectShellTest.officeChromeHelpButtonPreservesItsMulticolorQuestionMarkIcon" --tests "com.microproject.pm.graphic.IconManagerRibbonIconTest" --console=plain` — pass.
- `:microproject_ui:test --console=plain` — 1,293 tests, 0 failures. The run started while Windows was at 150% and finished after the user restored 100%; the module tests are not physical GUI acceptance.
- `:microproject_ui:installDist --console=plain` — pass; candidate distribution was relaunched for physical captures.
- The help-icon test now paints the `Icon` into a buffer rather than assuming `ImageIcon.getImage()` is a `BufferedImage`; its white-fill, green-stroke, and multicolor assertions remain. Baseline-only test passed; candidate initially exposed this changed vector-image contract; targeted and full reruns pass after the test update.
- PR #913 Build [run 40092934809](https://github.com/tetsuji16/microProject/actions/runs/40092934809) — success.
- PR #913 GUI acceptance [run 40092934821](https://github.com/tetsuji16/microProject/actions/runs/40092934821) — success.
- Post-merge local test review at base `bd09807853d9981cd24a7a2c327c68f3aa8f9956`: focused icon and help-icon tests pass; `:microproject_ui:test --console=plain` passes (1,294 tests, 0 failures/errors). Changes are uncommitted test-only follow-ups separate from merged PR #913.
- The review removes implementation-class assertions, compares scaled rendering to a same-painter fixed-raster control and proves that control is rejected, checks PNG-only fallback states, and preserves `getImage()` pixel semantics through bounded `PixelGrabber` extraction.

## Physical evidence

All captures show `CCPM Visualization Sample [Master]` with populated task rows
and Gantt bars. Before/after use separate fixture copies with the same SHA-256;
the blank view did not reproduce. Temporary sync sidecars were removed; the
source sample was unchanged. Effective window DPI was 96 at 100% and 144 at
150% (`GetDpiForWindow`); the 125% session was measured at effective 120 DPI.

| Scale / DPI | Baseline image — SHA-256 | Candidate image — SHA-256 | PNG dimensions |
|---|---|---|---|
| 100% / 96 | [100-before-baseline.png](issue-861-evidence/100-before-baseline.png) — `FEEEC1738FBE896256495DE2E29798ABC02D44BCD75D8D2E4001CB90540C3196` | [100-after-candidate.png](issue-861-evidence/100-after-candidate.png) — `ABFECC03FE1FF4D11209C346D24A70D67300456F2C9D1D8B4D95795A71D4B849` | 1010×761 |
| 125% / 120 | [125-before-baseline.png](issue-861-evidence/125-before-baseline.png) — `71D0283F563474D415D91500E3D8024CF8234A08AD601062E7A589075FED82BE` | [125-after-candidate.png](issue-861-evidence/125-after-candidate.png) — `12AF50BF8BFF624F815376DFF2337FC70F54362ECFCF26972B64D3F799185F0E` | 1011×762 |
| 150% / 144 | [150-before-baseline.png](issue-861-evidence/150-before-baseline.png) — `A24F55E347CBC6C66642D35EB467E84D4CA360D432D0EA1547F2045FD6D63B2E` | [150-after-candidate.png](issue-861-evidence/150-after-candidate.png) — `960E484534DEFE49B1A163B2DF770F77AFE0E3FFC04B2C45B9610785FC09024B` | 1012×762 |

Fixture copies SHA-256: `B6C88BCADCB98660493F188E81F471B449B3393BA8983197C396D86724AAF862`.
Baseline was detached commit `7cbc3beba28adc16f46b1a8bcd9af2c0e20e9ef9`;
candidate was built from source matching commit `24ceaa220d468b7afe42437bb4910b0956b66691`.
At each physical scale, icon placement and visible size matched baseline while
the candidate outlines appeared sharper.
