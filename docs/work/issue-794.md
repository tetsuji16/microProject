# Issue #794 — Roadmap gate M2: offline desktop workflow parity

State: OPEN. Issue: https://github.com/tetsuji16/ProjectLibre/issues/794
PR #872: DRAFT, not merge-ready. Referenced dependencies #792 and #793 are closed; they are not registered subissues. Contract frozen from [original acceptance comment](https://github.com/tetsuji16/ProjectLibre/issues/794#issuecomment-6313928977).
Current head `806c24dd9fee3ad653c3667931b5116b9c46524a`; latest production-source SHA `569f729b0444704373edb1a99738022e7822e49a`. Changes after the product slice are test/docs/CI diagnostic or environment-guard changes. Preserve `.codex-remote-attachments/`.
Long-form evidence and older failures: [issue-794-evidence.md](issue-794-evidence.md). Follow [issue execution](../issue-execution.md); reuse earlier SHA evidence only when the scoped diff confirms relevant production code, fixture, and dependencies unchanged.

## Frozen acceptance and current status

| Gate | Original requirement | Current evidence / remaining status |
|---|---|---|
| M2.1 (#408/#414/#750/#761) | Physical text/date/numeric edits, ties/empty, both sort directions, filter/group, stable selection and table/Gantt alignment, unchanged model. Add Sort Order to Sort menu. Keep display sorting separate from explicit permanent renumber with confirmation, Undo, persistence. | Behavior rows recorded; #750/#761 closed. Retained six-file #414 input/editor refactor: 3,313→3,302 production LOC; 13→13 declarations; listener-add 16→13 and remove 8→8; three phase-specific reviews passed. Mixed historical whole-branch totals are not attributable. Exact integrated full GUI gate open. |
| M2.2 (#403/#409/#410/#411/#453/#769) | Microsoft-rule date fixtures; FS/SS/FF/SF and lag; intended-only mutation/diagnostics; Undo/Redo/MPO reopen; injected post-mutation failure leaves model, Undo and observers unchanged; confirm #403 phase dependencies. | Scoped scheduling, rollback, exchange and physical Gantt evidence recorded in archive. Latest full GUI did not complete its matrix; exact integrated full core/exchange/UI evidence remains open. |
| M2.3 (#453/#769/#772) | Selected/all resource scope, proposed resources, leveling defaults/provenance, assignment totals, status date/actuals/baselines; outcome/selection; preserve manual splits; Undo/Redo/MPO reopen. | Scoped resource, assignment, progress, baseline and MPO rows recorded. Assignment lock-warning physical journey passed 2/2 on `5ff818dd` (run `39629323260`). Exact integrated full GUI gate remains open. |
| M2.4 (#453/#765/#769/#770) | Command source/enablement and physical result; view-only purity and relevant persistence; locale/DPI. Reports compare fixture values/rendered output; print checks pagination and Japanese/English labels. Unsupported commands absent. | Scoped view/ribbon/report/print evidence recorded. Full Japanese/English and DPI legs have not passed on the integrated candidate; current full GUI gate open. |
| Common | Focused tests; physical GUI route when changed; failure atomicity and unrelated-state preservation; exact build, fixture, test and screenshot/log evidence. | Scoped evidence below and in archive. Exact integrated full GUI gate open. No original criterion is waived. |

Display sorting does not mutate outline order. The supported Windows/JDK 25 U-34 outcome is deterministic unsupported IME reconversion: preserve text/selection and report unsupported behavior; no native bridge requirement. See `TEST_PLAN.md` U-34 and #866 evidence.

## Scoped evidence reusable by source equivalence

- `clean build installDist` passed at `569f729b0` (65 tasks; 2,825 tests). The later `CollaborationHelperTest` was added after that local clean build; it passed in the later `5ff818dd` build CI. Build CI on `a70c372` passed (`39630731121`).
- Task Information GUI passed 72/72 (`39621984170`, `569f729b0`); OfficeChrome passed 3/3 (`39622025547`, `569f729b0`); selected zoom passed focused 1/1 (`39621461859`, `569f729b0`). Two-document shortcut passed on `da29adba1` (`39624763058`); window resize passed 1/1 on `37dc04802` (`39620097168`). Assignment warning/assign/reopen journey passed 2/2 on `5ff818dd` (`39629323260`).
- The six-file #414 comparison and three reviews are recorded above and in the archive. Product changes cover transaction-backed schedule/dependency/assignment/resource/progress/baseline workflows and snapshot Undo/Redo; pixel-boundary-safe selected-task zoom; usable-bounds Calendar dialog placement; canonical dialog key bindings and the accepted unsupported-IME outcome.
- User chose Day sensitivity as a document-derived product default. Microsoft's [Resource Leveling dialog box](https://support.microsoft.com/en-us/project/resource-leveling-dialog-box) states Month while [Distribute project work evenly by resource level](https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments) states Day. This is a product decision, not empirical MSP evidence.
- Installed startup at `e92ae81af0f2435793169712e8afbe0b595be7a9` loaded the installDist jars and rendered eight sample task rows plus Gantt. That evidence is limited to startup/rendering. No later distribution/runtime dependency changes require refreshing that scoped startup evidence; no fresh manual startup is an original frozen criterion.
- Packaging is not applicable: no distribution/runtime inputs, dependencies, icons, associations or release behavior changed. Publication was not requested.

## Latest CI and environment findings

- PR smoke and build passed at `a70c372` (`39630731114`, `39630731121`). Current head `806c24dd` build and PR smoke runs `39634389293` / `39634389301` were already in progress at packet update. No further hosted reruns are planned.
- Full GUI run `39629894061` at `5ff818dd`: 205 tests, 12 failures, 0 skips; the first Gantt keyboard-focus failure has an unknown precondition, then Azure Arc Setup notification overlap recurred across GUI routes and stopped the coordinator. No DPI legs ran. The failures do not establish a product regression.
- Targeted Gantt run `39632899332` at `a70c372`: 10 tests, 3 failures, 0 skips. The first failure's artifact shows the Azure Arc notification foreground before Robot dispatch; the later two routes were stopped by the desktop coordinator. No production zoom failure is proven.
- Microsoft documents [Azure Arc Setup](https://learn.microsoft.com/en-us/azure/azure-arc/servers/onboard-windows-server) as enabled by default on Windows Server 2025; supported removal is an OS capability operation, not a notification-only policy. Do not suppress/dismiss the popup or change runner configuration in this packet. Hosted runner and notification diagnostics are indexed in the evidence archive.
- User confirmed the existing 2560×1440 desktop is available now. Root was notified to reserve the PC exclusively for 30–60 minutes; local full-suite execution is approved/preparing, with resolution unchanged. Execute this next slice rather than waiting for another availability reply.

## Next slice

Run the prepared local full GUI suite with the desktop reserved; preserve the existing resolution and GUI quality gates. Do not dispatch further hosted reruns while Azure Arc overlap remains. Review only the unmet original acceptance rows and update this packet before any issue/PR completion decision.
