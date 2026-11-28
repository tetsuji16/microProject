# Issue #1511 — core: separate node and outline contracts

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1511
Contract frozen at: owner-approved issue body/comments, 2026-11-28
Baseline HEAD after origin/master sync: `c7a17e287023224e65ff1bacbb00e67f0938f9ff`
Baseline working tree: clean except issue planning files

## Goal and scope

Expose a Swing-free public `com.microproject.node.OutlineModel` with nested `Hierarchy` through `OutlineCollection`; preserve the current Swing `NodeModel` / `NodeHierarchy` implementation boundary and migrate the direct Project/ResourcePool collection callers.

Out of scope: unrelated #737 Swing/core boundary work, schedule behavior, POD/MPO contract changes.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Swing-free OutlineModel and hierarchy contract, callers migrated, hierarchy behavior preserved | pass | `OutlineCollectionTest`, `MutableNodeHierarchyCoordinationTest`, `ProjectHierarchyQueriesTest` |
| A2 | HierarchyEvent serialized identity, UID, fields, relocation alias preserved | pass | `OutlineCollectionTest.hierarchyPersistenceAndConfigurationContractsRemainStable` |
| A3 | BelongsToHierarchy XML binding remains loadable | pass | configuration `Field.parentId` loaded in the same focused test |
| A4 | NamedNodeElement references audited; delete only when unreferenced | pass | `git grep -n -w NamedNodeElement -- modules samples`: 0 matches after deletion |
| A5 | Core, architecture, inventory, diff checks | pass | see verification below |

## Current slice

**Next slice:** Commit completed implementation, run PR checklist, open one PR, wait on required checks/reviews.
**Done when:** PR checks and reviews are green/resolved.
**Then:** Stop; no next issue.

## Remaining blockers

- PR `build`, `GUI acceptance`, checklist workflow and review pending.

## Latest evidence

- Baseline test-first commit: `2b0594796` (rebased from `41b9ed468`); `OutlineCollectionTest` failed on unchanged code with `ClassNotFoundException` for the not-yet-created `OutlineModel`.
- Focused tests: `MutableNodeHierarchyCoordinationTest`, `ProjectHierarchyQueriesTest`, `OutlineCollectionTest` all pass.
- Full core test: `:microproject_core:test` BUILD SUCCESSFUL; 1,522 tests, 0 failures/errors.
- Architecture: `verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures verifyNamingConventions` BUILD SUCCESSFUL.
- Inventory/diff: test source inventory and `git diff --check` pass.
- Physical GUI/manual: not applicable; no user-visible route changed.
- CI: no PR yet.