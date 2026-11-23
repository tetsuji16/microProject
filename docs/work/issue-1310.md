# Issue #1310 — UI package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1310
Contract frozen at: 2026-11-23, issue body
Baseline HEAD: `c916bbf05` (origin/master)
Baseline working tree: clean isolated integration worktree; original worktrees preserved.

## Goal and scope

Move microproject_ui packages, class references, launch configuration and package-relative resources under com.microproject.ui. Preserve supported project files and functionality.

Out of scope: class redesign, core/exchange package relocation, com.jgoodies.
Blockers: none established.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | UI source/resource directories under com.microproject.ui | pass | directory inspection, integration tree |
| A2 | old package reference grep is empty | pass | tracked and untracked source search, integration tree |
| A3 | installDist launch succeeds | open | pending |
| A4 | PR build and GUI acceptance pass | open | pending PR CI |
| A5 | relocated persisted classes mapped; exchange tests pass if applicable | pass | no moved main classes in persisted inventory; exchange tests passed |
| A6 | issue verification commands pass | open | compile and architecture tasks passed; remaining tests pending |

## Current slice

**Next slice:** complete clean build and installDist; inspect startup.
**Done when:** focused commands pass and unresolved failures are documented.
**Then:** push candidate and inspect required PR checks.

## Remaining blockers

- Clean build, startup and PR checks pending.

## Latest evidence

- UI compile and architecture checks passed; UI, reports, and exchange tests passed on integration tree. Source inventory check passed.
- Physical GUI/manual: pending.
- CI: pending PR.
