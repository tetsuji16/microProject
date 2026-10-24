# Issue #774 — Remove obsolete server paths and unnecessary legacy names

State: IMPLEMENTED — local gates passed; candidate CI and physical acceptance tracked in the integration record
Issue: https://github.com/tetsuji16/microProject/issues/774
Contract frozen at: 2026-10-24, original issue and scope clarification comments
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Complete the eight linked slices #902–#909. Preserve local Open/Save, supported
MPP/XML exchange, master/linked projects, shared resources, offline collaboration,
MPO backward-read, frozen POD wire identifiers and required namespace bridges.
Server/cloud integrations are out of scope. A server-related name alone is not
evidence that a persisted type or local feature can be removed.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| P1 | Candidate provenance, callers, dynamic references, classification and persistence impact recorded | pass (local candidate) | #903–#909 packets |
| P2 | Local Open and supported MSP exchange preserved | local pass; candidate GUI gate | affected module and physical-route evidence |
| P3 | Obsolete server/login paths removed only after usage/data review | pass (local candidate) | #903–#906 |
| P4 | Frozen POD and MPO backward-read preserved | pass (local candidate) | #902 / PR #910; later integration tests pending |
| P5 | Focused tests and required GUI/persistence evidence per slice | local pass; candidate GUI gate | child packets and candidate CI |
| P6 | Only intentional changes; no generated/sample side effects | pass (local candidate) | final diff/status review |
| P7 | Unnecessary legacy display/identifiers removed; necessary old data still readable | local pass; candidate GUI gate | #907–#909 |

## Current slice

**Next slice:** integrate the frozen candidate and complete the linked CI/Robot gates.
**Done when:** Build, Windows smoke and the targeted resource-mapping journey pass.
**Then:** close required children and #774 using the original P1–P7 contract.

## Remaining work

- #902: merged as PR #910; Build and GUI acceptance passed; issue closed.
- #903–#909: implementation, scoped independent review and local gates complete.
- Candidate Build and Windows smoke/targeted Robot gates remain before closure.

## Latest evidence

- #902: five selected tests pass; source inventory check passes; see issue-902.md.
- Integrated working-tree verification (HEAD `c4a02f26895f781fe8d91bd1b097b701d11b51db`, all concurrent issue slices present): `verifyArchitectureBoundaries verifyArchitectureBoundaryFixtures verifyNamingConventions` passed; `python -m unittest discover -s scripts/tests -v` passed 20/20; `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` passed.
- `clean build installDist --console=plain --no-daemon --max-workers=1` passed, 66/66 tasks; generated module test XML totals: 2,880 tests, 0 failures, 0 errors, 0 skipped. Per module: application 37, bootstrap 14, contrib 2, core 1,182, exchange 336, reports 13, ribbon 5, UI 1,291.
- The first clean attempt failed before compilation because its Tee log was inside `build/audit`, which `clean` could not delete while the log was open. Rerunning the identical command with log at `%TEMP%\issue-774-clean-build-installDist.log` passed; this was a logging-path issue, not a product/test failure.
- `:microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` initially failed in the new `ResourceMappingDialogGuiAcceptanceTest`: two accesses at lines 56/86 called JTable methods through `associationTable`'s private inner `AssociationTable` type. The owner changed those test assertions to use an accessible `JTable` reference; retry passed (20 actionable tasks, 1 executed/19 up-to-date). Production code is unchanged by this fix.
- Final status after build: 213 entries in the shared dirty tree; no status entries under `samples`, `docs/downloads`, `build`, or `packaging`. Existing untracked `.codex-remote-attachments/` remains untouched.
- Live integration acceptance record (in-place update after CI/closure): https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503
- Physical GUI: retain exact candidate evidence per affected child; no substitute
  from headless tests or an unrelated earlier SHA.
- Integration: review each bounded diff; run broad candidate checks once after
  independent slices are integrated. Do not close the parent before all original
  criteria and required children have evidence.
