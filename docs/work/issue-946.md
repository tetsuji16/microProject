# Issue #946 — Gantt and Resource views become unresponsive

State: Hardening and diagnostics in PR (draft); cause of the reported freeze NOT confirmed.
Issue: https://github.com/tetsuji16/microProject/issues/946
Contract frozen at: 2026-10-26, issue body and comment.
Reported version: 0.1.0.1271. Reporter saw this after about 30 minutes; views switch, but controls stop responding while Task Manager shows normal resource use.

## Goal and scope

Identify and fix the shared cause that leaves Gantt and Resource views visible but unusable after extended use. Keep the application responsive and both views' physical interactions functional after sustained runtime.

Out of scope: unrelated view redesign, broad performance optimization, and fixing unrelated UI recovery issues.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Reproduce or instrument the extended-use freeze and identify a confirmed shared cause | open | Instrumented (below); cause not confirmed |
| A2 | Gantt and Resource remain physically interactive after the triggering duration/sequence | open | Soak on 1271 passed; freeze not reproduced |
| A3 | No regression to model/view state, switching, or persistence | open | Module tests passed on the PR branch; exact-SHA CI pending |

## Evidence

- A1 instrumentation: `EdtWatchdog` and an always-on error log were added. The watchdog also reports when the mutation coordinator is unobtainable, or held by the EDT between events.
- Soak on v0.1.0.1271 passed: https://github.com/tetsuji16/microProject/actions/runs/41048935711. 12 minutes, 264+ cycles of Gantt/Resource switching and Robot clicks, recovery saves every 60s, MPO with a collaboration session, CCPM applied through the service (not the dialog). 0 uncaught exceptions, no EDT stall, no coordinator leak.
- So this journey did NOT reproduce the freeze. Its limits: it saves the MPO directly (not through GUI Save As) and switches views through `DocumentFrame` methods (not ribbon clicks).
- Reporter retested 0.1.0.1284 and reported it working fine so far (2026-10-29). Lock-leak fixes #950 and #959 are in that release. Resource import fix #966 is merged after 1284 and unreleased. The reporter's #967 stays separate.
- Earlier source review (comment 6419599124) candidates: coordinator lock leak in `Project.runModelCommand`/`TaskModeService`, uncoordinated off-EDT recovery save, collaboration timer without a guard, `JobQueue` dropping a named job. These are hardened in this change; none is confirmed as the reported cause.
- The soak (`ViewResponsivenessSoakGuiAcceptanceTest`) is not in the smoke or visual selectors; run it with `gui_test_filter`.
- Resource import slice (PR #966, merged as `c20d926ab`): `samples/Commercial construction project plan2.pod` → MPO reload lost resource-outline entries and resource-side assignment links. Pre-merge build 40728365984 and GUI smoke 40728365969 passed (53 tests, 0 failures/errors, 1 skipped). Targeted Windows GUI run 40726284523 passed on `d0f5746d4`; its later diff adds only an exchange test. That journey covers physical Gantt/Resource editing, Undo/Redo and a direct MPO roundtrip, not GUI Save/Open or background recovery.
- False leads for that journey: run 40725150896 failed before testing on a notification-registry permission, and run 40725634388 on `ff06fc24b` failed because the fixture checked focus on the task editor wrapper; focus belongs to its inner text component.
- Limited fixes rejected in review: a journal reservation let a stale external merge overwrite a later local edit and record the remote edit twice, and a revision check left other setters and non-merge saves unprotected. The merged fix (#976) compares the serialized model instead.

## Open and split out

- Data-loss race during MPO save: fixed in #976 (issue #970 closed); `MpoTransactionBoundaryTest` no longer has `@Disabled` tests.
- Complete recovery snapshot for linked projects: see `docs/work/issue-946-recovery-design.md`.
- CCPM preview and collaboration file I/O still run on the EDT.

## Next

1. Exact-SHA CI on the PR.
2. Release.
3. Reporter retests on MPO using the new error log; a freeze report should now carry the watchdog output.
