# Issue #953 — Remove unused and commented-out code

State: In progress; first cleanup slice underway.
Issue: https://github.com/tetsuji16/microProject/issues/953
Contract frozen at: 2026-10-27, open issue body.
Baseline HEAD: `e695b33ac2e8453774d13eca134b59e207bc71f5`.
Baseline working tree: preserve existing unrelated documentation, book, GUI test, sample sidecar, and attachment changes; no core production/test files were modified at intake.

## Goal and scope

Remove production code from `modules/*/src/main` only when repository search proves it has no callers; remove commented-out code. Record the detector and caller-search evidence for every deletion, run the owning module tests, and keep an inventory of any retained candidates with reasons.

Out of scope: POD/MPO readers, compatibility adapters, `microproject_contrib`, format contracts, and automated formatting tools.
Blockers: none for the current core cleanup slice.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Detection method recorded | partial | `rg -n '^\\s*//.*;\\s*$' modules/<module>/src/main --glob '*.java'` detects standalone commented Java statements; additional commented declaration/block patterns are reviewed separately. |
| A2 | Every removal has caller-search evidence | partial | Current slice removes only an inert commented-out loop/return in `ResourcePool.isGroupDirty`; active method remains. |
| A3 | Owning module test and `git diff --check` pass | pass | `:microproject_core:test --console=plain` and `git diff --check` passed on baseline HEAD `e695b33ac2e8453774d13eca134b59e207bc71f5` plus this comment-only deletion. |
| A4 | Detection results are empty or remaining candidates have reasons | open | Core scan found additional candidates; continue after this finite slice. |

## Current slice

**Current slice:** Removed the obsolete commented-out loop and return from `ResourcePool.isGroupDirty`; the method's live `return isDirty` and public API are unchanged.
**Next slice:** Screen the remaining core commented-code candidates and classify, remove, or record reasons.
**Done when:** the next candidate is frozen with caller/provenance evidence before any additional edit.
**Then:** continue the module-by-module cleanup and residual inventory.

## Remaining blockers

- Complete the production-source scan and caller review across modules.
- Record retained candidates with reasons or reduce the detector result to zero.

## Latest evidence

- Baseline: `git status --short` and HEAD captured before edits; no pre-existing core source or test changes.
- Detection: `rg -n '^\\s*//.*;\\s*$' modules/microproject_core/src/main --glob '*.java'` identified commented statements, including the obsolete loop in `ResourcePool.isGroupDirty`.
- Caller review: `rg -n 'isGroupDirty\\(|setGroupDirty\\(' modules/microproject_core/src/main/java modules/microproject_core/src/test/java` locates live document dirty-state APIs and tests; this slice deletes only commented lines and leaves the live method implementation and API intact.
- Tests: `:microproject_core:test --console=plain` passed (2026-10-28; 7 actionable tasks, 2 executed).
- `git diff --check` passed (2026-10-28).
