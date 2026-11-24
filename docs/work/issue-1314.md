# Issue #1314 — Package layout and naming gate

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1314
Contract frozen at: 2026-11-24, issue body.
Baseline HEAD: `a4122622c`
Baseline working tree: clean isolated worktree.

## Goal and scope

Document the current module packages and prior moves; reject reintroduction of retired production packages. Preserve historical reports and persisted-name compatibility aliases.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | package-layout document linked from AGENTS | pass | local diff |
| A2 | actual main source in com.microproject.pm makes naming gate fail | pass | temporary fixture; exit 1 and package identified; fixture removed |
| A3 | naming gate passes on candidate | pass | prescribed three Gradle verification tasks |
| A4 | required PR build and GUI acceptance | open | pending candidate CI |

## Current slice

**Next slice:** review and push candidate, then assess required checks.
**Done when:** build and GUI acceptance pass on the PR head.
**Then:** merge, check parent acceptance and close parent if complete.

## Remaining blockers

- A4: CI pending.

## Latest evidence

- `./gradlew.bat verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures verifyNamingConventions --console=plain` passed after removing the negative fixture.
- Parent old-name search over modules, excluding compatibility map and inventory, returned no matches.
