# Issue #1310 — UI package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1310
Contract frozen at: 2026-11-23, issue body
Baseline HEAD: `c916bbf05` (origin/master)
Baseline working tree: clean isolated integration worktree; original worktrees preserved.

## Goal and scope

Move microproject_ui packages, class references, launch configuration and package-relative resources under com.microproject.ui. Preserve supported project files and functionality.

Scope revision 2026-11-24: user authorized relocating the core PertManualPositions class needed for A2. Other core/exchange package relocation, class redesign and com.jgoodies remain out of scope.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | UI source/resource directories under com.microproject.ui | pass | directory inspection, `83e8b42d6` |
| A2 | old package reference grep is empty | pass | grep empty on `c09e99863` |
| A3 | installDist launch succeeds | pass | Windows launcher from fresh installDist on `c09e99863`; welcome and ribbon displayed |
| A4 | PR build and GUI acceptance pass | open | exact candidate CI pending |
| A5 | relocated persisted classes mapped; exchange tests pass if applicable | pass | no moved main classes in persisted inventory; exchange test and clean build passed, `83e8b42d6` |
| A6 | issue verification commands pass | open | compile, exchange/UI tests, architecture, inventory and startup passed; exact candidate CI pending |

## Current slice

**Next slice:** assess exact candidate build and GUI acceptance.
**Done when:** required CI checks pass on the pushed candidate.
**Then:** review final PR state.

## Remaining blockers

- A4: exact candidate CI pending.

## Latest evidence

- `83e8b42d6`: `./gradlew.bat clean build installDist --console=plain` passed on Windows.
- CI on `83e8b42d6`: Windows and macOS builds passed; Linux failed at a stale test path; GUI failed loading stale service provider name. Corrected locally.
- Corrected tree: `python -m unittest discover -s scripts/tests -v` passed (24 tests); `./gradlew.bat :microproject_ui:guiTest --tests "*GuiEnvironmentExtensionUserDataTest" --console=plain` passed (3 tests). This exercises service loading, not physical application startup.

- Integration with master `305e393c8`: UI compile and architecture checks passed; focused network, summary and ribbon unit tests passed. CI pending.

- `2ec4626ba`: PR build and GUI acceptance passed; user then authorized moving the core class to satisfy A2.

- `c09e99863`: exchange/UI tests, GUI compilation, architecture checks, source inventory and old-name grep passed; fresh installDist launcher showed the welcome screen and ribbon on Windows.
