# Issue #796 — M3 native MPO and external interoperability

State: Original acceptance 1–5 evaluated and passed within the frozen bounded scope; docs-only #922 integration change pending exact-head CI and merge.
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
| A1 | Inventory readers/writers and supported versions, beyond extensions | passed | #916/#921; integrated in [#922 packet](issue-922.md) |
| A2 | Independent Project-origin MPP semantic manifest; losses and failures | passed, bounded fixtures | #917; integrated in [#922 packet](issue-922.md) |
| A3 | XML read/write: elapsed units, baseline/contours, malformed/missing, round trips | passed, bounded contracts | #918; integrated in [#922 packet](issue-922.md) |
| A4 | MPO backward reads/native save-reopen and unchanged fixed POD fixtures | passed, bounded contracts | #919/#920; integrated in [#922 packet](issue-922.md) |
| A5 | Exact tests/fixtures/outputs/status/losses; document-derived rules labeled | passed | #916–#922; integrated in [#922 packet](issue-922.md) |
| A6 | Every direction passes its contract; all required children resolved | passed within frozen scope | #916–#921 closed; direct parent assessment in [#922 packet](issue-922.md) |

## Current slice

**Current delivery:** Original parent criteria 1–5 and #922 A1–A4 are evaluated
in [the integration packet](issue-922.md). All six required children #916–#921
are closed and their bounded contracts pass. The only remaining step is to
merge the docs-only integration PR after its exact-head checks, then close #922
and this parent. No source implementation remains in scope.

## Children and closure

Native GitHub sub-issue relationships verified for #916–#922 on 2026-10-24.
Inventory #916 is closed via PR #924 / `ed276d5a4`; MPP #917 via PR #925 / `565998a34`; #918 via PR #926 / `b0bf09782`; #919 via PR #927 / `c626c6df4`; POD #920 via PR #928 / `5f31b7591`; other formats #921 via PR #929 / `42c81851b`. All six children are closed on GitHub.
Parent body has the ordered checklist; each child contains scope/dependencies/
acceptance/verification rules. No child status alone proves parent acceptance.
Do not remove or defer an original failing criterion to close a child or parent.

## Remaining blockers

- No acceptance blocker remains within the frozen scope. Exact-head Build and GUI checks for the documentation-only #922 integration PR remain before final close.

## Latest evidence

- #916 mapped format/version/source and fixture evidence; its inventory is the
  entry point for the remaining direction-specific gates.
- #432 closed via PR #923, integrated `a2af8b1a6` (tree identical to tested
  `8481f4dfc`): Build 40108488677, required smoke 40108488656 and focused CI100
  40108486647 passed; local Windows UI scales 1.0/1.25/1.5 passed. Hosted CI150
  40109069846 skipped for insufficient work area and failed its gate, so it is
  not pass evidence. See issue-432.md for the exact scope and links.
- These GUI checks do not establish file-format parity; no compatibility pass
  is inferred from a job name or the #432 closure.

- #917: MPP8/9/12/14 bounded manifests, calendar semantics and typed invalid-input checks passed; exchange module 339 tests, zero failures/errors/skips. Required Build 40116689195 and GUI 40116689198 passed on head 8ac404dcf, tree identical to merge 565998a34. Other fields/builds remain explicitly unverified.

- #918 integrated b0bf09782, same tree as tested dd49d8b52: core 1,182 and exchange 344 tests passed; final UTC exchange + UTC/Tokyo focused lifecycle checks passed. Required Build 40124226992 and Windows GUI 40124227051 passed (53 total, 1 known skip, 0 failures/errors). Initial Linux failure40123626606 was a test timezone/cache defect, corrected without changing expectations. See issue-918.md; no general XML parity claim.

- #919 tested source 7f2d94b0626382d7048ac787658cee44b52c8f1a: exchange 350, application 37, related UI 3 passed; unchanged core 1,183 and ribbon visual 2 are separately recorded. Physical 40174026825 passed 1/0 failures/0 errors/0 skips and its deferred-EDT gate. Docs-only delivery reuses matching source/test evidence; final exact-head required checks and merge are tracked in PR #927. See issue-919.md for historical fixture hashes, limits, and prior failures.

- #920 POD contract passed, including fixture hashes/descriptors, semantic MPO reload, typed malformed/unsafe rejection, frozen bytes and physical Save As/recovery routes. Exact candidate `bdb53abe8b720cfe955388d763621620da4bb522`: Build 40177582811 and GUI 40177582845 passed; merged via PR #928 as `5f31b7591b6dd7bdf0cb38a0948dfa552d60a023`.
- #921 MPX/Planner/XLSX bounded contracts and internal-only PMXML/JSON/SDEF classifications passed. Exact head `6235dbf45983793a651d3ea923833dc60d1ec03f`: Build 40191516443 and GUI 40191516426 passed; merged via PR #929 as `42c81851b0c9307d556c9bd1277153f3795a411c`.
- #922 direct assessment: [docs/work/issue-922.md](issue-922.md) maps each parent item and all format directions, reports GUI evidence separately, and preserves all listed unsupported scope. Source implementation and evidence are unchanged by this documentation-only closeout.
