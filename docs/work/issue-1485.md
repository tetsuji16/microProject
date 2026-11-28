# Issue #1485 — hosted GUI watcher startup

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1485
Contract frozen at: 2026-11-28, issue #1485
Baseline HEAD: `c80e3618ce450b13d4062571c0e6793c2d22b6a6`
Baseline working tree: clean isolated worktree; the primary workspace's pre-existing user changes remain untouched

## Goal and scope

Make hosted GUI watcher startup bounded and diagnosable while preserving fail-closed behavior, then run the new startup test in GUI acceptance.

Out of scope: bypassing the watcher; rerunning the failed stable full gate on the same SHA.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Delayed startup, successful readiness/cleanup, immediate failure, and timeout are tested. | pass | Local test passed on worktree changes; rerun after final source edit. |
| A2 | Failure evidence includes child PID/state, elapsed time, stdout/stderr, and watcher log. | pass | Local immediate-failure and timeout cases asserted diagnostic content. |
| A3 | GUI gate does not start unless watcher startup is ready. | pass | Startup helper throws on timeout before `Invoke-GuiGate` launches Gradle; timeout case passed. |
| A4 | Gradle build succeeds. | pass | `& $PWD\gradlew.bat build --console=plain`: BUILD SUCCESSFUL in 12s; 51 tasks, 4 executed and 47 up-to-date on final code. |
| A5 | `git diff --check` is empty. | pass | No output on the current diff; rerun after staging. |
| A6 | PR `build` and `GUI acceptance` pass. | pass | PR #1486 at `ee3344c087b9e46fcf16c56ab761a7e735ee0aa1`: `build`, `GUI acceptance`, `verify (windows-latest)`, `verify (ubuntu-latest)`, `verify (macos-latest)`, and `checklist` pass; [run](https://github.com/tetsuji16/microProject/actions/runs/46083174148), [GUI](https://github.com/tetsuji16/microProject/actions/runs/46083174127). |

## Current slice

**Next slice:** rerun checks on the evidence-packet-only follow-up, update PR evidence, then merge #1486.
**Done when:** required CI passes, review comments are resolved, and the PR is merged.
**Then:** push the new integrated candidate once to the stable branch and verify publication.

## Remaining blockers

- A6: PR checks and review are pending.
- The exact transient reason run 46059927014 lacked the watcher log remains unknown; the new diagnostic will distinguish initialization delay from child failure.

## Latest evidence

- Tests/build: `pwsh -NoProfile -File .github/scripts/test-hosted-warning-watcher-startup.ps1` passed all 3 cases after the final edit. `& $PWD\gradlew.bat build --console=plain` passed on final code in 12s; 51 tasks, 4 executed and 47 up-to-date.
- Physical GUI/manual: not applicable; no user-visible route changed. Hosted guard remains fail-closed.
- CI: PR #1486 required checks passed on code SHA `ee3344c087b9e46fcf16c56ab761a7e735ee0aa1`; review comments are empty.
