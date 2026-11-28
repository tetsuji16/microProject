# Issue #1502 — assignment and availability contracts

State: IN PROGRESS — PR #1534 open; CI/review pending
Issue: https://github.com/tetsuji16/microProject/issues/1502
PR: https://github.com/tetsuji16/microProject/pull/1534
Contract frozen at: issue body fetched 2026-11-28
Baseline HEAD: `b5c33c1f966d2d05af43ce63976cf5122704a37e` (origin/master)
Code tested at: `ce472d9d296963bbce6612bbc42c125442058a44`
Baseline working tree: clean dedicated worktree `microProject-1502`

## Goal and scope

Document the contracts in the eight approved assignment/availability files, type AbstractContourBucket's Comparator, and preserve sorting, null ordering, configuration names, serialized fields/UIDs, and POD round-trip behavior.

Out of scope: scheduling/availability semantics, POD/MPO format changes, aliases, other assignment or contour types.
Blockers: PR checks/review pending. Preflight PR #1497 had no failed checks or actionable review comments.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Specified eight files updated only in scope | pass | 8 production files, AssignmentContourBehaviorTest, and packet (10 total) |
| A2 | AbstractContourBucket uses Comparator<AbstractContourBucket>; comparison and null ordering retained | pass | AssignmentContourBehaviorTest comparator sort and null assertions |
| A3 | public contracts, constants, configuration.xml references, serialized class/UID/field names retained | pass | Source diff is Javadocs plus comparator generic; test pins class names, UIDs, field names/types; configuration.xml unchanged |
| A4 | contour POD round-trip and tests pass | pass | PodRoundTripTest: 26 tests, 0 failures; ClassRelocationsTest: 8 tests, 0 failures |
| A5 | files <=10, diff <400 lines, diff check clean | pass | 10 files; 205 changed lines; cached diff check clean |

## Current slice

**Next slice:** wait for current PR-head `build`, `GUI acceptance`, and checklist checks; inspect review comments.
**Done when:** required checks pass and actionable reviews are resolved or documented.
**Then:** report PR/check outcomes without merging.

## Remaining blockers

- CI and review on the final PR head.

## Latest evidence

- Baseline before edits: AssignmentContourBehaviorTest + HasAssignmentsImplTest passed; PersonalContourTest passed; ClassRelocationsTest passed; PodRoundTripTest passed.
- After edits at code SHA `ce472d9`: focused assignment tests 27/0; PersonalContourTest 3/0; ClassRelocationsTest 8/0; core 1,519/0 failures/2 skipped; PodRoundTripTest 26/0.
- Environment: JDK 25.0.4.1 / Windows 11.
- PR #1534 checks: checklist passed; build matrix and GUI acceptance are running.