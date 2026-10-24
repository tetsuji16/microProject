# Issue #916 — M3 format inventory

**Issue:** [#916](https://github.com/tetsuji16/ProjectLibre/issues/916), frozen
2026-10-25. **State:** IN_PROGRESS; inventory slice recorded, pending #917–#922
and parent integration. **Parent:** #796, split into finite child slices.
**Owner:** Sol. **This slice:** documentation/inventory only (no production,
test, or dependency edits). **Baseline:** branch `docs/issue-916-format-contracts`,
HEAD `a2af8b1a65d44cd9d54c204c3cb4ef569101bfd5`; tested source tree
`8481f4dfc` is reported identical for relevant code. Baseline `git status --short`
had no tracked modifications; unrelated untracked entries were `.codex-remote-attachments/`,
`docs/work/issue-796.md`, and four sample `*.microproject-sync.{json,lock}` files.

## Frozen acceptance

| ID | Required result | Status / evidence |
|---|---|---|
| A1 | Enumerate every desktop-advertised read/write direction, version evidence, and unknown version explicitly; include discovered factory paths without calling them advertised. | **Mapped:** [format inventory](../msp-exchange-format-inventory.md). MPO 1.0; prior microProject 0.0.23 fixture; MPP9, Project 2013 MPP14, Project 2019 MPP14/build 16.0.10730.20102 fixtures; XLSX native payload schema 3. MPX, Planner, broader XML/XLSX, future MPO minor versions and other MPP editions/builds remain unverified. |
| A2 | For each row, map entrypoint, fixed fixture, asserted semantic fields, evidence SHA, known loss/missing proof, and owner child. | **Mapped:** [inventory](../msp-exchange-format-inventory.md) links reader/writer entrypoints, exact MPP/MPO/POD fixture identities and provenance, bounded test methods, gaps, and owners #917–#921. XML/XLSX evidence names in-test literal/generated inputs rather than external golden files; missing fixed contracts remain unverified and assigned. Source evidence SHA `8481f4dfc`; Build [40108488677](https://github.com/tetsuji16/ProjectLibre/actions/runs/40108488677). No new execution is claimed for this docs-only slice. |
| A3 | Keep format policy explicit: MPO native/default; preserve supported prior MPO reads; MPP read separate; MPP write excluded; POD frozen import-only. | **Recorded:** inventory and roadmap. Existing POD writer/chooser exposure is a policy discrepancy owned by #920, not a write contract. |
| A4 | Map every original #796 parent acceptance item 1–5 to inventory/evidence and a child; no child alone closes parent. | **Mapped:** item 1 (format/version inventory) → #916/#921; item 2 (MPP) → #917; item 3 (XML/MSPDI) → #918; item 4 (MPO/POD persistence) → #919/#920; item 5 (record exact tests, fixtures, outputs, status, unsupported fields, and document-derived vs empirical MSP decisions) → #916/#922. #922 owns direct integration/proof of all parent items 1–5; child closure is not parent proof. |

## Evidence boundaries

- MPP manifests assert only listed fields for checked-in MPP9/2013/2019 fixtures; Microsoft 365 Project and Project 2024 authored files/builds are unverified. Fixture README provenance is linked in the inventory.
- XML assignment baseline timephased work **and cost** have literal XML/MPO round-trip evidence, including zero/missing distinctions. This does not establish general XML parity.
- MPO fixtures establish selected prior-release 0.0.23 reads and a previous-writer schedule/work reopen, not arbitrary future-version compatibility.
- XLSX fixtures establish schema-3 native payload behavior and selected external fallback fields only. MPX/Planner and PMXML/JSON/SDEF factory paths lack fixed semantic contracts.
- POD fixed samples and descriptor manifest are linked in the inventory and preserve its frozen wire contract. Do not add a POD write contract.
- Build 40108488677 passed enabled module tests at `8481f4dfc`. Reuse is limited to unchanged relevant source/tests/dependencies; this documentation-only change does not rerun suites.

## Next slice

**#917 — MPP read contract.** Add/complete version-and-field matrix only for checked-in fixed fixtures, identify any true missing manifest proof, and record unsupported constructs/failure behavior. Done when the bounded contracts are independently reviewable and unknown editions/builds stay explicit. Then proceed in order: #918 XML, #919 MPO, #920 POD, #921 secondary formats, #922 parent integration.

## Current checks

- `git diff --check` — pending review of this follow-up.
- Tests/build — not run; documentation-only slice, reuse scoped source evidence above.
