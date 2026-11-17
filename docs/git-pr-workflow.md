# Git and pull request workflow

This document is the operational companion to the rules in [`AGENTS.md`](../AGENTS.md).

## Start an issue branch

1. Confirm the issue is open and read its acceptance criteria and dependencies.
2. Start from the latest `master`: `git fetch origin master` followed by `git switch -c issue/<number>-<summary> origin/master`.
3. Keep one issue in each branch and PR. Do not push to `master` directly, and never force-push. Keep incomplete PRs in draft; mark them ready only when the stated review criteria are met.

Use one of these in the PR body, never both:

- `Closes #N` when every acceptance criterion in scope is complete.
- `Refs #N` when work remains; list the unmet criteria and next steps.

Use commit subjects in the form `<type>: <imperative summary>` (for example, `fix: reject cyclic task links`). Keep commits focused on the issue and do not mention agent or model names.

## Review and merge

Before requesting merge, include the purpose, changes, exact verification commands and outcomes, and compatibility impact (including MPO/MPP/MSPDI where applicable). Run the required `build` and `GUI acceptance` checks. Merge only after required checks succeed and required review/conversation conditions are satisfied. Never bypass these rules with administrator rights. If the PR is stacked on another PR, change its base to `master` after the underlying PR merges, then review the resulting diff and checks again.

## Recommended `master` branch protection

The following is a recommended GitHub REST API request body for `PUT /repos/tetsuji16/microProject/branches/master/protection`. It protects `master` from direct changes, requires pull requests and the repository's `build` and `GUI acceptance` checks, disallows force pushes/deletion, and applies protection to administrators. Confirm the exact check context names in the repository's check runs before applying; replace the example contexts if GitHub reports different names.

```json
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["build", "GUI acceptance"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
```

Applying branch protection requires repository administrator or owner access. If that access is unavailable, record the setting as **repository configuration required** in the PR and use `Refs #N` until an owner applies or explicitly declines the recommendation. The `enforce_admins` setting is an owner decision because it removes administrator bypass for protected-branch rules.

See [GitHub's protected branches REST API](https://docs.github.com/en/rest/branches/branch-protection) for the request schema and permissions.
