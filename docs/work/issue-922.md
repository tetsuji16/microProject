# Issue #922 — M3 parent acceptance integration

State: Acceptance complete; docs-only integration change pending exact-head CI and merge.
Issue: https://github.com/tetsuji16/microProject/issues/922
Parent: https://github.com/tetsuji16/microProject/issues/796
Baseline HEAD: `b42f3e0a6228c3d2f9c2654f7b6ac176f4941c35`.
Baseline working tree: existing untracked `.codex-remote-attachments/` and sample sync sidecars; preserved.

## Acceptance

| ID | Required result | State / evidence |
|---|---|---|
| A1 | Every in-scope format/direction has explicit status, passing bounded contract tests, and listed mismatches. | **Pass within declared bounded scope.** Direction matrix below; MPP write excluded by the explicit 2026-10-12 decision. PMXML/JSON/SDEF are internal writer registrations, not advertised routes; absent reader routes and unverified semantics remain explicit. |
| A2 | Required MPP, XML, MPO, POD, and other-format rows have fixture, expected/actual, tested SHA, and evidence. | **Pass.** Packets #917–#921 and `docs/msp-exchange-format-inventory.md` record fixture identities/provenance, literal expected values, observed values, tested source SHA, commands/results, evidence links, losses and unsupported fields. |
| A3 | Assess #432 GUI evidence without conflating compile/action/smoke with physical and visual evidence. | **Pass.** `docs/work/issue-432.md`: physical Robot at 1.0/1.25/1.5 and multiple window sizes; rendered text, column/header/body geometry, task bars, divider/width persistence and redraw checks; exact tested-head Build, required smoke and focused CI100 recorded. Hosted CI150 could not run at sufficient work area and is explicitly not pass evidence. No display resolution was changed. This layout evidence is separate from file-format acceptance. |
| A4 | Reuse only scoped matching evidence, confirm required CI, and preserve every original parent criterion and required child. | **Pass.** #916–#921 are closed. Merged PRs #924–#929 have exact-head Build and GUI evidence in their packets; tested-source versus merge-tree boundaries are stated. Original #796 items 1–5 are each mapped below; no item or required format direction was deferred. |

## Direct mapping to original #796 acceptance

1. **Inventory and supported versions:** #916 and the format inventory map reader/writer entry points, routes and version evidence. MPO 1.0/prior 0.0.23, bounded MPP families, MPX 4.0, Planner v2 and XLSX native schema 3 are identified; unknown versions and absent/unadvertised routes remain explicit.
2. **Independent MPP read contract:** #917 pins MPP8/9/12/14 fixtures and literal semantic manifests, lists losses and typed malformed/unknown-family failures, and checks MPO reload. Other fields and Microsoft 365/Project 2024 authored builds remain unverified; no broad parity claim.
3. **XML/MSPDI contract:** #918 covers working/elapsed duration codes, baseline slots and type IDs, availability boundaries, invalid/missing input and XML→MPO→XML values. Literal expectations are document-derived where indicated; broader schema/contour combinations remain unverified.
4. **MPO and frozen POD:** #919 covers prior MPO reads, native reload and save transaction preservation; #920 pins three POD fixtures/descriptors, verifies selected import→MPO reload semantics, typed invalid/unsafe rejection, frozen wire bytes and import-only save policy. Recovery snapshot serialization is separately retained. Unsupported fields/defaults are listed.
5. **Exact evidence and compatibility classification:** #916–#921 and the format inventory contain commands, counts, hashes, tested SHAs, CI links, mismatches and unsupported scope. Microsoft-spec-derived inferences are labeled document-derived; no physical MSP run is claimed.

## Direction summary and bounded limits

| Format/direction | Integrated status |
|---|---|
| MPP read | Bounded contracts pass for pinned MPP8/9/12/14 fixtures; other fields/editions/builds unverified. |
| MPP write | Excluded by explicit user decision; MPO is native/default. |
| XML/MSPDI read/write | Bounded duration, baseline, availability, custom-field, malformed-input and round-trip contracts pass; broader parity unverified. |
| MPO native/read | Native/default; bounded historical and native transaction contracts pass; arbitrary legacy patch/build combinations unverified. |
| POD read | Frozen import-only contract passes for fixed fixtures; unrepresented fields/defaults listed; no POD write contract. |
| MPX read/write | MPX 4.0 bounded contracts pass for task/link/calendar reads and writer readback; other fields/versions unverified. |
| GNOME Planner read/write | Planner v2 bounded import and writer readback pass; other versions/fields unverified. This is not Microsoft Planner or a cloud service. |
| XLSX read/write | Native schema 3, embedded MSPDI and external summary fallback have distinct bounded contracts and MPO reload checks; general spreadsheet parity unverified. |
| PMXML/JSON/SDEF | Internal writer registrations only; no reader or advertised Open/Save route identified. Routing/non-exposure is recorded; interoperability remains unverified and is not represented as supported. |

## Verification and remaining work

This integration changes documentation only. It reuses source/test evidence because the production code, fixtures and dependencies are unchanged. Required Build and GUI acceptance checks for the integrated implementation PR heads passed: #927 (`c41af1560f0c83090b38a05f2ae55ba7e26bba10`, runs [40174521952](https://github.com/tetsuji16/microProject/actions/runs/40174521952) and [40174521961](https://github.com/tetsuji16/microProject/actions/runs/40174521961)); #928 (head `82bcd02209b349efdf2fba1979cdc47fb27ff364`, runs [40178032675](https://github.com/tetsuji16/microProject/actions/runs/40178032675) and [40178032648](https://github.com/tetsuji16/microProject/actions/runs/40178032648)); #929 (`6235dbf45983793a651d3ea923833dc60d1ec03f`, runs [40191516443](https://github.com/tetsuji16/microProject/actions/runs/40191516443) and [40191516426](https://github.com/tetsuji16/microProject/actions/runs/40191516426)). Each pair is Build and GUI acceptance, both passed. PR #923 separately records #432's physical/visual evidence and CI distinction.

No acceptance criterion remains open within the frozen scope. MPP writer, connected services, unrepresented file fields, unknown format versions and broader parity remain explicitly excluded or unverified as listed above; they do not defer an original required bounded contract. Final action: exact-head checks and merge of this docs-only integration PR, then close #922 and #796 with links to this packet and the merged evidence.
