# Issue #796 — M3 native MPO and external interoperability

State: OPEN
Issue: https://github.com/tetsuji16/microProject/issues/796
Contract frozen at: 2026-10-24, original body and comments through #823
Baseline HEAD: `043a0a85834f49c7eb46bb45ddb17c3ad09d262e`
Baseline working tree: unrelated untracked attachment directory and sample sync JSON/locks; preserve them.

## Goal and scope

Every advertised in-scope format/direction has an explicit status, passing
contract tests and listed mismatches. Preserve all original acceptance items.
MPO is native/default; MPP is read-only interoperability; POD is frozen import-only.
Out of scope: MPP writing (explicit user decision, 2026-10-12), connected services.
Owner/reviewer: Sol; bounded investigation/implementation/verification: Luna.

## Acceptance

| ID | Required result/evidence | State | Owner/evidence |
|---|---|---|---|
| A1 | Inventory readers/writers and supported versions, beyond extensions | open | #916, #921 |
| A2 | Independent Project-origin MPP semantic manifest; losses and failures | open | #917; bounded prior evidence #805/#823 |
| A3 | XML read/write: elapsed units, baseline/contours, malformed/missing, round trips | open | #918; reconcile existing tests before new work |
| A4 | MPO backward reads/native save-reopen and unchanged fixed POD fixtures | open | #919/#920; bounded evidence #807/#818 |
| A5 | Exact tests/fixtures/outputs/status/losses; document-derived rules labeled | open | #916–#922 |
| A6 | Every direction passes its contract; all required children resolved | open | #922 evaluates original parent criteria directly |

## Current slice

**Next slice:** #916 freezes the finite format/field/version evidence matrix.
**Done when:** every advertised direction has an implementation/version/fixture/
actual-assertion/evidence/loss row and a named remaining owner, and stale XML
timephased-cost statements are reconciled without claiming untested parity.
**Then:** #917, #918, #919, #920, #921, #922, one active implementation at a time.

## Children and closure

Native GitHub sub-issue relationships verified for #916–#922 on 2026-10-24.
Parent body has the ordered checklist; each child contains scope/dependencies/
acceptance/verification rules. No child status alone proves parent acceptance.
Do not remove or defer an original failing criterion to close a child or parent.

## Remaining blockers

- A1–A6 require current-source evidence reconciliation and direction-specific
  validation; stale parent status does not prove a current implementation gap.
- Historical GUI success without content/geometry assertions remains insufficient;
  use the new #432 evidence only for its stated routes and scales.

## Latest evidence

- Read-only Luna inventory: current tests include MPP9/MPP14 manifests,
  elapsed duration and assignment timephased baseline work/cost; inventory and
  roadmap descriptions differ. #916 must inspect actual assertions and history.
- #432 closed via PR #923, integrated `a2af8b1a6` (tree identical to tested
  `8481f4dfc`): Build 40108488677, required smoke 40108488656 and focused CI100
  40108486647 passed; local Windows UI scales 1.0/1.25/1.5 passed. Hosted CI150
  40109069846 skipped for insufficient work area and failed its gate, so it is
  not pass evidence. See issue-432.md for the exact scope and links.
- These GUI checks do not establish file-format parity. #916 must map current
  exchange tests and fixtures directly; no new compatibility pass is claimed
  from a job name or the #432 closure.
