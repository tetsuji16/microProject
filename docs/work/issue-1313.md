# Issue #1313 — Model and scheduling package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1313
Contract frozen at: 2026-11-24, issue body.
Baseline HEAD: `a6528239e`
Baseline working tree: clean isolated worktree.

## Goal and scope

Relocate grouping.core, server.access and pm packages as specified, preserving POD wire fixtures, earlier MPO reads and scheduling results. Core server.data was moved in #1311. Exclude class renames and unrelated packages.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | old package search empty outside relocation table/inventory | open | pending |
| A2 | historical MPO and POD fixtures read | open | pending |
| A3 | fixed POD bytes unchanged | open | pending |
| A4 | scheduling tests pass without changing expectations | open | pending |
| A5 | required build and GUI acceptance pass | open | pending |
| A6 | prescribed compile, architecture, tests, clean build and inventory | open | pending |

## Current slice

**Next slice:** relocate pm model and scheduling packages with historical class mappings.
**Done when:** production and test compilation and historical fixtures pass.
**Then:** run complete scheduling, exchange and application tests.

## Remaining blockers

- A1–A6 pending.

- Node/access relocation: architecture checks, ClassRelocationsTest, persisted class inventory and fixed POD/MPO fixture tests passed.
