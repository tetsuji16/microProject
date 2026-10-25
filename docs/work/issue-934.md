# Issue #934 — M5.3 integrated release compatibility report

State: Complete; integrated release evidence and explicit residuals are reconciled.
Issue: https://github.com/tetsuji16/microProject/issues/934
Parent: https://github.com/tetsuji16/microProject/issues/795
Contract frozen at: 2026-10-25, issue body.
Baseline HEAD: `1875236e204f86d887f44eea46809b6f71aa6b0d`.
Baseline working tree: pre-existing untracked attachment and sample sync sidecars; preserve them.

## Goal and scope

Reconcile original M5 acceptance against current issue evidence, exact stable
release, format packets and platform packages. State residual deviations and
excluded scope explicitly. A passing release is not a claim of complete
Microsoft Project feature parity.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Reconcile M0–M3, M4 exclusion and required children | pass | #792/#793/#794/#796 closed; #797 closed not planned |
| A2 | Exact candidate, module/architecture checks and package links | pass | Release v0.0.24.1265, SHA `8404a6c3…`; run 39968551059 |
| A3 | Packaged launch, GUI/accessibility, en/ja 100/125/150% | pass for Windows reference gate; Unix package launch separately verified | Windows full matrix passed in run 39968551059; Linux/macOS launch and asset integrity passed in run 40198218372. Unix physical interaction/accessibility routes are outside the target-platform launch evidence and are not claimed. |
| A4 | MPP-read, XML/MSPDI, MPO round trips, POD frozen fixtures and diffs | pass within M3 bounded contracts | #796/#922 and format packets #916–#921 |
| A5 | Dated Microsoft 365 / Project 2024 comparison and six microProject legs | pass as document-derived comparison | #793 closure evidence; Microsoft desktop clients not installed, no empirical capture claimed |
| A6 | Dated residual report, issue and format status, reviewer decision | pass | This packet, with exact run/tag/SHA, supporting child issues, limits, and Sol's integrated evidence review |
| A7 | Honor post-closure v0.1 release direction | queued after #795 | User instruction on #795: publish stable `v0.1.0.<build>` after parent closure |

## Current report

### Gate disposition

- M0/#792 and M1/#793 are closed. M1 is visual-only: dated Microsoft-published
  references are compared as document-derived evidence; six microProject
  locale/scale legs and the evidence archive are recorded. No observed
  Microsoft client build or empirical parity claim is made.
- M2/#794 is closed after direct assessment of the frozen offline workflow
  contract and required children. See `docs/work/issue-794.md` and its linked
  evidence. Its package/release evidence is the exact v0.0.24.1265 run.
- M3/#796 is closed through #922. Its original criteria pass within the frozen
  bounded contracts, with mismatches and unknown versions/fields listed in
  `docs/work/issue-922.md` and `docs/msp-exchange-format-inventory.md`.
- M4/#797 is EXCLUDED by the explicit 2026-10-12 user decision: Microsoft
  Project VBA/macros, macro object model, COM/VSTO and local Office add-ins.
  Project Server/PWA/Online/Planner cloud services are out of scope. This does
  not exclude microProject-native automation or offline collaboration.

### Stable candidate and release checks

- Latest stable release: `v0.0.24.1265`, published 2026-10-23, exact source
  SHA `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`.
- Release workflow 39968551059 passed the Windows full GUI gate and packaged
  verification, and Linux/macOS target package jobs. Build and package evidence
  is linked from `docs/work/issue-794.md`.
- Windows GUI result: ja/100 full functional 241 tests, 0 failures/errors, 1
  allowed skip; visual ja/125, ja/150, en/100, en/125 and en/150 each 65 tests,
  0 failures/errors and 1 allowed skip. This is hosted Windows evidence.
- Linux ZIP digest `f869d9cf9d570e4dd3a378e676e90c1bf6d284ea9f38d2e326a0d46206b618df`;
  macOS ZIP digest `366ca73fb13597511d0fbc8e650fa5663b60c3675a8f6b9a2292b646dac7f3f9`.
  Both are release API asset digests for the v0.0.24.1265 packages.
- Linux/macOS target-host launch workflow 40198218372 passed both jobs on the
  same release: downloaded SHA-256 and byte counts match each release API
  asset, and both bounded launch steps passed. Logs are available as artifacts
  [Linux x64](https://github.com/tetsuji16/microProject/actions/runs/40198218372/artifacts/12599679693)
  and [macOS Apple Silicon](https://github.com/tetsuji16/microProject/actions/runs/40198218372/artifacts/12600003952).
- Hosted Linux had no AT-SPI accessibility bus. The Unix smoke is a launch
  check, not a physical interaction/accessibility journey. The full GUI matrix
  is the Windows reference-desktop gate. #933 separately verifies package
  currency and target-platform launch; no Linux/macOS accessibility parity is
  claimed by this release gate.
- #932 records the prior v0.0.24.1248 Windows release row; v0.0.24.1265
  supersedes it as the current stable release.

### Format and compatibility disposition

| Format / scope | Status | Evidence and residual limits |
|---|---|---|
| MPP read/import | VERIFIED for pinned MPP8/9/12/14 contracts | #917/#922 independent manifests, MPO reload and rejection paths; other fields and Microsoft 365/Project 2024 authored files remain unverified. |
| MPP write | EXCLUDED | Explicit user decision; MPO is native/default save. |
| XML/MSPDI read/write | VERIFIED for bounded duration, baseline, availability, invalid-input and XML→MPO→XML contracts | #918/#922; broader schema and contour combinations are listed as unverified. Literal rules are document-derived where noted. |
| MPO native/backward read | VERIFIED for bounded historical fixtures, native round-trip and save transaction contracts | #919/#922; arbitrary legacy patch/build and field combinations remain unverified. |
| POD import | VERIFIED for fixed frozen fixtures and wire bytes | #920/#922; import only, no new POD writer; unrepresented fields/defaults listed. |
| MPX, Planner v2, XLSX | VERIFIED for bounded advertised format contracts | #921/#922; broader versions/fields are not generalized. Planner means local GNOME Planner, not Microsoft Planner. |
| PMXML/JSON/SDEF | NOT ADVERTISED routes | Inventory classifies internal writer registrations; no reader/Open/Save route was found. |
| Microsoft visual comparison | VERIFIED as document-derived | #793 reference/source table and 48-image evidence archive; no Microsoft Project application capture or build claim. |
| Microsoft-specific automation / cloud services | EXCLUDED / OUT OF SCOPE | Explicit user decisions recorded in #795 and roadmap. |

### Dated residuals

- Both Unix launch logs report a signature-length error for the external update
  configuration at the legacy ProjectLibre GitHub URL. The installed app falls
  back to launch without update. The packaged application launch passed;
  updater configuration remains a separate release residual.
- Broader/unlisted MPP fields and versions, XML schema combinations, and
  arbitrary historical project contents are not covered by the bounded M3
  contract and are not claimed as verified compatibility.

## Current slice

**Completed:** All original #795 acceptance steps are mapped to dated evidence
and explicit limitations. The Windows reference GUI/locale/scale gate, bounded
format gates, and Unix package currency/launch gates passed. The packet does
not generalize the Windows GUI result to Unix interaction/accessibility.
**Then:** Close #934 and #795 against the documented in-scope gates. After the
parent is closed, publish stable `v0.1.0.<build>` as directed by the user.

## Remaining blockers

- No remaining blockers for the bounded M5 acceptance: Windows is the reference
  desktop gate and Linux/macOS are separately verified for current package and
  target-host launch. Their physical GUI/accessibility routes remain unclaimed.
- The documented report records every compatibility family by its accepted
  bounded contract; unknown fields/builds are explicitly not generalized.

## Latest evidence

- Release workflow and target package job status: run 39968551059, source SHA
  `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`.
- M3 integration packet: `docs/work/issue-922.md`; M3 parent #796 is closed.
- Target-host workflow run 40198218372 passed both package launch jobs. The
  workflow source is `5da89eb6eef51f87ed7525a3ad91b7e24cc0d6a8`.
