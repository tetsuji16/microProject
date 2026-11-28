# Issue #1475 — 遅い日付再計算の方針

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1475
Contract frozen at: 2026-11-28, issue body + comments
Baseline HEAD: `c7a4d021c4ab3dc5a8912f0d0090911db708328a`
Baseline working tree: isolated worktree, clean before this packet

## Goal and scope

Choose how to defer late-date/slack recalculation while preserving the first-read freshness contract. No module source changes in this issue.

Out of scope: implementing deferred scheduling; bulk insertion (#1178).
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Target and trigger boundary commented | pass | Decision comment; issue #1489 |
| A2 | Existing test's required behavior assessed | pass | `TaskDependencyInvalidationTest...` passed before implementation |
| A3 | Implementation issue number recorded | pass | #1489 |
| A4 | No module sources changed | pass | `git diff --stat -- modules` empty |

## Current slice

**Next slice:** Commit the decision packet and open a PR closing #1475.
**Done when:** Decision comment, #1489, packet, and required PR checks are recorded.
**Then:** #1489 implementation.

## Remaining blockers

- PR CI is pending.

## Latest evidence

- Searches: late reads 10 matches; slack references 216; invalidation markers 11.
- Test: `:microproject_core:test --tests "com.microproject.model.task.TaskDependencyInvalidationTest" --console=plain` → `BUILD SUCCESSFUL`.
- CI: pending exact PR SHA.
