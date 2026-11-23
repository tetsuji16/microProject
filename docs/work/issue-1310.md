# Issue #1310 — UI package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1310
Contract frozen at: 2026-11-23, issue body
Baseline HEAD: `c916bbf05` (origin/master)
Baseline working tree: clean isolated integration worktree; original worktrees preserved.

## Goal and scope

Move microproject_ui packages, class references, launch configuration and package-relative resources under com.microproject.ui. Preserve supported project files and functionality.

Out of scope: class redesign, core/exchange package relocation, com.jgoodies.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | UI source/resource directories under com.microproject.ui | pass | directory inspection, `83e8b42d6` |
| A2 | old package reference grep is empty | pass | source search, `83e8b42d6` |
| A3 | installDist launch succeeds | open | physical startup pending |
| A4 | PR build and GUI acceptance pass | open | exact candidate CI pending |
| A5 | relocated persisted classes mapped; exchange tests pass if applicable | pass | no moved main classes in persisted inventory; exchange test and clean build passed, `83e8b42d6` |
| A6 | issue verification commands pass | open | clean build and compile, architecture, source inventory passed; physical startup pending |

## Current slice

**Next slice:** push corrected CI references and assess exact candidate build and GUI acceptance.
**Done when:** both required checks pass and startup is demonstrated.
**Then:** review final diff and prepare PR for review.

## Remaining blockers

- A3, A4: physical startup and passing candidate CI.

## Latest evidence

- `83e8b42d6`: `./gradlew.bat clean build installDist --console=plain` passed on Windows.
- CI on `83e8b42d6`: Windows and macOS builds passed; Linux failed at a stale test path; GUI failed loading stale service provider name. Corrected locally.
- Corrected tree: `python -m unittest discover -s scripts/tests -v` passed (24 tests); `./gradlew.bat :microproject_ui:guiTest --tests "*GuiEnvironmentExtensionUserDataTest" --console=plain` passed (3 tests). This exercises service loading, not physical application startup.
