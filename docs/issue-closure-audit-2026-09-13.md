# Issue closure audit — 2026-09-13

This audit uses the latest reported evidence: full GUI 121/0, UI tests 825/0,
focused #559 and #560 runs, and the deferred Menu/Ribbon work for #558.
Issues are not marked complete merely because a focused test passes; every
acceptance row in the issue body must have matching evidence.

| Issue | Decision | Completion evidence | Unmet condition |
|---|---|---|---|
| #558 | OPEN | Immutable request/result, calendar normalization, core boundary/Undo and MPO tests | Menu/Ribbon physical route and UI Robot matrix are explicitly deferred; no end-to-end selection→dialog→typed outcome→Undo/MPO evidence |
| #559 | CLOSED | Typed buffer kinds, generation/clear conflict handling, MPO round-trip, focused CCPM suite, plus the three named boundary/projection tests | Closed after `:micrproject_core:test --tests com.microproject.pm.ccpm.CriticalChainServiceTest` BUILD SUCCESSFUL (21s) and GitHub closure comment |
| #560 | OPEN | Contextual descriptor/catalog tests for Gantt, Network and Calendar; invalid placement fix focused tests | Focused tests do not prove the required physical contextual transition at the final GUI gate; Timeline and Report are unsupported and must remain explicitly non-advertised |
| #453 | OPEN | Standard command inventory, Ribbon comparison, CCPM implementation and child evidence | MSP-complete scope still contains unsupported/conditional commands and advanced CCPM/view/report behavior. Parent cannot close while #558/#559/#560 remain open |
| #535 | OPEN | Architecture children and most compatibility children closed | Parent completion requires all compatibility children, including #453 and its active children, to be closed |
| #552 | OPEN | Ledger exists and records gates | Zero unresolved open children is false while #535/#453/#558/#559/#560 remain open |

## Child issue proposals (non-duplicate)

These are the smallest remaining acceptance units; they should be entered as
GitHub children if no existing issue covers the same scope.

1. **#558-ui-route-matrix** — Bind Update Project through Ribbon, Menu, popup,
   and root-pane shortcut to one typed route. Acceptance: selected,
   read-only, invalid date, milestone, 0/100% and dependency cases each assert
   `CommandOutcome`, model state, one Undo, and MPO save/reload through a
   physical Robot representative.
2. **#559-resource-projection** — Add multiple-feeding/fixed-constraint and
   resource threshold boundary fixtures, then assert resource-buffer values in
   every supported CCPM view/report projection without WBS mutation.
3. **#560-contextual-robot** — Run a serial physical transition matrix for
   Gantt/Tracking Gantt, Network, and Calendar contextual Format tabs and assert
   unsupported Timeline/Report commands are absent.
4. **#453-compatibility-rollup** — Reconcile the standard-command inventory and
   advanced CCPM scope against Microsoft public documentation; close only after
   children above have evidence and unsupported features remain explicitly
   non-advertised.
5. **#561 (GitHub child of #560)** — Network/Calendar contextual CommandId
   matrix and English/Japanese × 100/125/150% physical Robot evidence,
   including explicit absence/disabled checks for unprovided Popup/Shortcut
   entries.

No source or generated artifact is required for this audit. The existing
ledger remains authoritative for lifecycle/temp-file and POD/MPO compatibility
contracts.

## GitHub cross-check

The current public issue state was cross-checked with the repository remote on
2026-09-13. GitHub now reports #559 CLOSED; #558 and #560 remain OPEN. The latest
comments attached to #558 record core/exchange success but explicitly defer
UI Robot/Undo/MPO route evidence; #559's later closure comment records the
three boundary/projection tests and a successful focused run; #560 has
implementation-start comments but no complete physical Robot result.
Therefore a locally reported “latest Robot” result is not closure evidence until
the exact command, test method, artifact path and outcome are linked to the
issue. No issue was closed by this audit.

## Current implementation ownership

- #558 UI route matrix: `micrproject_ui` (DocumentFrame/menu/ribbon/popup/root
  action), with core/exchange regression reuse.
- #559 resource projection: `micrproject_core` (named CCPM boundary fixtures),
  then `micrproject_ui`/`micrproject_reports` projection tests.
- #560 contextual Robot matrix: `micrproject_ui`; unsupported Timeline/Report
  commands remain unbound until real implementations exist.

The corresponding evidence comments were added to GitHub issues #558, #559,
and #560. #559 is now CLOSED; #558 and #560 remain OPEN.
