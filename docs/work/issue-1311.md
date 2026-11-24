# Issue #1311 — Exchange package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1311
Contract frozen at: 2026-11-24, issue body; user authorized core relocation.
Baseline HEAD: `fc3e14feb`
Baseline working tree: clean isolated worktree.

## Goal and scope

Move exchange classes into exchange packages, retain MPO/POD compatibility, unchanged POD fixtures and format, and validate historical round trips. Core server.data types are required by exchange and are named in A1; moving them to persistence.data, the specified #1313 destination, is necessary to satisfy A1 and authorized by the user. Remaining core moves belong to #1312 and #1313.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | no core.pm/server.data references in exchange | pass | git grep empty on working tree |
| A2 | historical MPO and POD fixtures read | pass | exchange tests; inventory and historical fixture tests |
| A3 | POD fixture bytes unchanged | pass | fixed fixture SHA-256 and descriptor inventory test |
| A4 | MPO fixture round trip preserves tasks, resources, assignments | pass | MpoHistoricalFixtureTest.fixedSchedulePreviousWriterFixturePreservesLiteralScheduleAndWorkAfterReopen |
| A5 | required build and GUI acceptance pass | open | pending |
| A6 | prescribed compile, architecture, tests, clean build and inventory checks | open | pending |

## Current slice

**Next slice:** complete clean build and assess PR CI.
**Done when:** clean build, required checks and final review pass.
**Then:** merge and proceed to #1312.

## Remaining blockers

- A5–A6: clean build and exact-candidate CI pending.

## Latest evidence

- Scope conflict recorded in issue comment 7124677721.

- Compile and architecture checks passed. Core, exchange and application tests passed after updating the descriptor inventory reader to use the production relocation map; original POD hashes and wire descriptors remain unchanged. Source inventory passed.
