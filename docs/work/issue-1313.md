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
| A1 | old package search empty outside relocation table/inventory | pass | exact git grep on integration tree |
| A2 | historical MPO and POD fixtures read | pass | focused inventory and fixture tests |
| A3 | fixed POD bytes unchanged | pass | fixed hashes and descriptor inventory test |
| A4 | scheduling tests pass without changing expectations | open | pending |
| A5 | required build and GUI acceptance pass | open | pending |
| A6 | prescribed compile, architecture, tests, clean build and inventory | open | pending |

## Current slice

**Next slice:** complete clean build and assess exact candidate CI.
**Done when:** clean build, build check, GUI acceptance and review pass.
**Then:** merge and proceed to #1314.

## Remaining blockers

- A4–A6: scheduling/full tests, clean build and exact candidate CI pending.

- Node/access relocation: architecture checks, ClassRelocationsTest, persisted class inventory and fixed POD/MPO fixture tests passed.

- Model and scheduling relocation: all-module compile, architecture, GUI compilation, fixed POD/MPO and persisted class inventory tests passed. Source inventory and 24 maintenance-script tests passed. Clean build is running.
