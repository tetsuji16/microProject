# Issue #1023 — Git / PR workflow rules

## Contract at intake

- Goal: document the repository's branch and pull request workflow and prevent direct or force pushes to `master`.
- Acceptance: (A1) `AGENTS.md` states one issue per `issue/<number>-<summary>` branch/PR, `Closes` versus `Refs`, draft use, merge checks, and commit subject format; (A2) `docs/git-pr-workflow.md` gives operational steps and recommended protection JSON; (A3) PR #1007 is closed without merging; (A4) `master` admin enforcement is enabled; (A5) script tests and PR `build` + `GUI acceptance` pass.
- Scope: documentation and the requested GitHub `master` protection setting. No application code changes.
- Baseline: clean worktree at `362030658b90dd25d8c1e95554bca780aee1c7b5` (`origin/master`), 2026-11-17.

## Acceptance state

| ID | State / evidence |
|---|---|
| A1 | Implemented in `AGENTS.md`; local review pending. |
| A2 | Implemented in `docs/git-pr-workflow.md`; local review pending. |
| A3 | Already closed, unmerged, verified in GitHub state and #1023 comments. |
| A4 | Enabled via GitHub API; readback returned `enabled: true`. Existing required checks are `build` and `GUI acceptance`. |
| A5 | Script tests passed (26); PR CI pending. |

## Next slice

Review the staged diff, commit and push `issue/1023-pr-workflow-rules`, open a PR with `Closes #1023`, and wait for `build` and `GUI acceptance` before considering #1023 complete.

## Latest verification

- `python -m unittest discover -s scripts/tests -p "test_*.py"` — 26 tests passed.
- GitHub protection readback — `enforce_admins: true`; required checks `build`, `GUI acceptance`.
