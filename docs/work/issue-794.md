# Issue #794 — Roadmap gate M2: offline desktop workflow parity

State: OPEN  
Issue: https://github.com/tetsuji16/ProjectLibre/issues/794  
Dependencies: #792 CLOSED; #793 CLOSED (met)  
Baseline HEAD: `2769b09e78c779301a3fe26c4fe056a151a479c8`  
Contract frozen: 2026-10-20 from current #794 body; see four packages and common
acceptance below. Closed children do not establish parent acceptance.

## Goal and scope

Complete M2 offline desktop workflow parity with package-level evidence for
editing/hierarchy/sorting, scheduling/dependencies/transactions,
resources/assignments/progress/baselines, and views/ribbon/context/reports/
print/files. Preserve the original package requirements and common per-PR
acceptance from the issue body. No product-wide cosmetic perfection criterion.

## Acceptance

| ID | Required evidence from frozen issue contract | State | Evidence |
|---|---|---|---|
| M2.1 | #408/#414: canonical physical command route, field/sort/filter/group and projection checks; #750 Sort Order menu; #761 explicit permanent renumber with confirmation, Undo, persistence | Open | #408/#414 OPEN; #750/#761 CLOSED. Historical evidence: [status log](../msp-implementation-status.md) |
| M2.2 | #403/#409/#410/#411/#453/#769: cited exact scheduling fixtures, link/lag matrix, intended mutations, diagnostics, Undo/Redo, MPO reopen, failure atomicity | Open | #403/#409/#410/#411/#453/#769 OPEN |
| M2.3 | #453/#769/#772: resource/assignment/progress/baseline scopes, provenance/totals/status/actuals/slots, manual split preservation, outcomes, Undo/Redo, MPO reopen | Open | #453/#769 OPEN; #772 CLOSED |
| M2.4 | #453/#765/#769/#770: source, enablement, physical route, visible result, no view-only mutation, relevant persistence, locale/DPI; report and print fixture checks | Open | #453/#765/#769/#770 OPEN |
| Common | Each PR: focused module tests, physical route when changed, failure atomicity, unrelated-state preservation, exact build/fixture/test/screenshot/log | Open | Current CI evidence below does not satisfy every row |

#868 unassigned-task MPO reload Work drift is OPEN. Whether it overlaps an
existing M2 acceptance criterion is unresolved. If it violates one, track it
as a blocker on that M2 row; it cannot be excluded by moving it to a follow-up.

## Current slice

**Next slice:** For #414, map existing evidence and inventory rows to its
acceptance IDs in one finite evidence-difference table; do not implement new
behavior or start a code audit. Identify the one smallest remaining
root-cause criterion, if any, and define the following slice. Retain the
contract's three adversarial reviews; reuse completed reviews and repeat only
for changed relevant code or a finding. **Done when:** each applicable #414
criterion has an evidence link or explicit gap, and one evidence-backed next
slice is named.  
**Then:** implement only that bounded #414 slice; continue M2.1 and remaining
packages after their criteria are separately resolved.

## Remaining blockers

- M2.1–M2.4 and common parent acceptance remain open; progress must not be
  represented as all-M2-complete.
- #414's issue acceptance remains broad: single active path; graph-wheel single
  owner; no command KeyListener except IME; LOC reduction; symmetric listener
  attach/detach; UI tests; clean build/installDist; Windows manual evidence; and
  three adversarial reviews. Next slice only maps evidence once and finds the
  smallest actual gap; it does not claim these criteria passed.

## Latest evidence

- Build: success at tested SHA `2769b09e78c779301a3fe26c4fe056a151a479c8`; [CI run](https://github.com/tetsuji16/ProjectLibre/actions/runs/39272414017).
- GUI smoke: success, 53 tests, 0 failures, 0 errors, 1 allowed skip; Japanese,
  100%, 1024×768 unchanged at tested SHA `2769b09e78c779301a3fe26c4fe056a151a479c8`. [CI run](https://github.com/tetsuji16/ProjectLibre/actions/runs/39272414000).
- GUI full locale/DPI matrix: not evidenced by this smoke run. Required check
  retains legacy name “Full GUI locale and DPI matrix”.
- No tests were run for this packet update.
