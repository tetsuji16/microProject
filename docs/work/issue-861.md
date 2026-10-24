# Issue #861 — HiDPI information icons appear blurry

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/861
Contract frozen at: 2026-10-24, issue description supplied for this task
Integration base: `7cbc3beba28adc16f46b1a8bcd9af2c0e20e9ef9`
Validation base: `25cc626b820d618dca52fd3e061d93d080ddbfcd`
Baseline working tree: only untracked `.codex-remote-attachments/`

## Goal and scope

Correct blurry information icons at Windows 100%, 125%, and 150% scaling.
Cover the task-table Indicators header and dialog/ribbon information icons.
Preserve the indicator's blue meaning, icon size, and accessible role.

Out of scope: unrelated icons and broad icon-system changes. Blockers: physical
125%/150% desktop evidence is unavailable in this environment.

Confirmed cause: the task-table header loaded a 16x16 PNG; the information
ribbon used SVG sources that both `IconManager` paths rasterized before Swing
paint. The baseline regression failed because both icons were raster
`ImageIcon`s. Current system DPI is 96 (100%) at 2560x1440.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Compare task-table and dialog/ribbon information icons at physical Windows 100%, 125%, and 150%; record screenshots and environment per scale | partial | Physical before/after CUA captures at 100%; physical 125/150 pending |
| A2 | Keep physical and off-screen render evidence separate; confirm cause | pass | 100% CUA capture and before/after Java2D renders at 100/125/150% under `modules/microproject_ui/build/reports/issue-861/{before,after}` |
| A3 | Correct root cause with vector/scale-specific assets, preserving meaning and size | pass | Blue 16x16 SVG for Indicators; shared lazy `FlatSVGIcon` path for the two target keys |
| A4 | Verify 100/125/150 rendering, icon visibility, and accessibility role | pass | Focused test checks vector type, scale paint, difference from 16px bilinear enlargement, and Indicators header `AccessibleRole.ICON` |

## Current slice

**Next slice:** in a responsive Windows Settings session, run the same project
screen at physical Windows 125% and 150%, recording before/after screenshots
and environment beside the off-screen evidence.
**Done when:** A1 has before/after evidence at physical 100/125/150%.
**Then:** integrate/close once the remaining physical matrix passes.

## Remaining blockers

- A1: physical 125/150% screenshots. Windows Settings did not expose a
  targetable window through CUA, so the existing 100% setting was preserved;
  display resolution was not changed.
- The GUI workflow's `gui_test_scale` is passed to Gradle's
  `sun.java2d.uiScale`; it tests Java rendering scale and does not change the
  Windows Scale & layout setting. Microsoft documents process DPI-awareness
  APIs separately from the OS scale setting; `SetDisplayConfig` scaling refers
  to desktop image/path scaling. No supported programmatic path for changing
  the Windows user scale was identified. Sources: [DPI awareness](https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process), [desktop image scaling](https://learn.microsoft.com/en-us/windows-hardware/drivers/display/scaling-the-desktop-image), [Microsoft DPI test guidance](https://learn.microsoft.com/en-us/windows/apps/develop/win2d/dpi-and-dips).
- CUA direct launch created `SystemSettings.exe` with a `設定` main-window title,
  but it was non-responsive and omitted from `listWindows`; a new Explorer
  window also had unavailable accessibility state. No app-log crash was found
  in the last 20 minutes. The unresponsive Settings process was left untouched.
- Physical screenshots are visible in the CUA tool transcript but that API
  cannot export them to a local file. Off-screen images are saved at the build
  report path above; those Java2D `Graphics2D.scale` renders are simulations,
  not physical Windows-DPI evidence.

## Latest evidence

- Tested working tree: validation base `25cc626b820d618dca52fd3e061d93d080ddbfcd`
  plus uncommitted changes. The focused regression failed 1/1 before production
  changes and passed after the vector change.
- Integration base is now `7cbc3beba28adc16f46b1a8bcd9af2c0e20e9ef9`; scoped
  base comparison shows only unrelated issue #864 docs and calendar tests, so
  issue #861 code/test evidence remains reusable.
- `:microproject_ui:test --tests IconManagerRibbonIconTest --tests RibbonUiAuditTest`
  passed (326 tests, 0 failures). After moving Swing header creation/role
  checks to the EDT, the focused tests passed again.
- `:microproject_ui:test --console=plain` passed in 1m50s before the final
  test-only EDT adjustment. `:microproject_ui:installDist --console=plain`
  passed before physical post-fix launch.
- Physical GUI: same `microProject - CCPM Visualization Sample [Master]` screen
  before/after at 100% (96 DPI, 2560x1440); both the blue Indicators header and
  black Ribbon Properties information icon remain visible. Post-fix run used a
  copy of the sample in the system temp directory.
- Off-screen evidence: `modules/microproject_ui/build/reports/issue-861/before`
  and `/after`; 100/125/150% output dimensions are 16/20/24px. The test checks
  125/150 outputs differ from bilinear enlargement of the 16px raster render.
  No focused physical Robot test exists for this visual-only change; CI not run.
- Incidental baseline app-open sidecars remain untracked:
  `samples/CCPM sample English.microproject-sync.json` and `.lock`. Their
  deletion was blocked by shell policy; the source sample itself is unchanged.
