# Issue #1502 — assignment and availability contracts

State: IN PROGRESS — implementation verified, preparing PR
Issue: https://github.com/tetsuji16/microProject/issues/1502
Contract frozen at: issue body fetched 2026-11-28
Baseline HEAD: `b5c33c1f966d2d05af43ce63976cf5122704a37e` (origin/master)
Baseline working tree: clean dedicated worktree `microProject-1502`

## Goal and scope

Document the contracts in the eight approved assignment/availability files, type AbstractContourBucket's Comparator, and preserve sorting, null ordering, configuration names, serialized fields/UIDs, and POD round-trip behavior.

Out of scope: scheduling/availability semantics, POD/MPO format changes, aliases, other assignment or contour types.
Blockers: none. PR #1497 was running checks at preflight; its checklist passed and there were no failures or actionable review comments.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Specified eight files updated only in scope | pass | 8 production files plus AssignmentContourBehaviorTest and this packet (10 files total) |
| A2 | AbstractContourBucket uses Comparator<AbstractContourBucket>; comparison and null ordering retained | pass | AssignmentContourBehaviorTest comparator sort and null assertions |
| A3 | public contracts, constants, configuration.xml references, serialized class/UID/field names retained | pass | Source diff is Javadocs plus comparator generic; test pins class names, UIDs, field names/types; configuration.xml unchanged |
| A4 | contour POD round-trip and tests pass | pass | PodRoundTripTest: 26 tests, 0 failures; ClassRelocationsTest: 8 tests, 0 failures |
| A5 | files <=10, diff <400 lines, diff check clean | pass | 10 files; 203 changed lines; cached diff check clean |

## Current slice

**Next slice:** commit the reviewed changes, create one PR closing #1502, and wait for CI/reviews.
**Done when:** `build`, `GUI acceptance`, and checklist pass on the PR head; actionable reviews are resolved.
**Then:** report exact PR/check outcomes without merging.

## Remaining blockers

- PR CI and review pending.

## Latest evidence

- Baseline before edits: AssignmentContourBehaviorTest + HasAssignmentsImplTest passed; PersonalContourTest passed; ClassRelocationsTest passed; PodRoundTripTest passed.
- After edits: focused assignment tests 27 tests/0 failures; PersonalContourTest 3/0; ClassRelocationsTest 8/0; core suite 1,519 tests/0 failures/2 skipped; PodRoundTripTest 26/0.
- Tested on JDK 25.0.4.1 / Windows 11. Baseline commit: `b5c33c1f966d2d05af43ce63976cf5122704a37e`.
- CI: no PR yet.