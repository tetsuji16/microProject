# microProject Beta 3 release plan

**Planning date:** 2026-10-11  
**Target:** Beta 3 in the week of 2027-04-11, about six months after Beta 2.  
**Primary outcome:** make microProject easier to evolve independently from ProjectLibre/OpenProj while keeping local project files and the task-table/Gantt workflow dependable.

This plan was prepared from all 34 issues open on 2026-10-11 and the latest comments on those issues. Issue status and branch evidence were checked against `master` at `b96e1236478bc28ea6f35fe1acddb1546ab305ba`. Closed issues were treated as regression history; they are not counted as remaining work unless a current open issue explicitly reopened the same requirement.

## Release objectives

1. Remove the normal desktop application's dependence on web-server project loading. Keep local Open/Save and supported Microsoft Project exchange working. Preserve only narrowly required serialized-format bridges.
2. Move presentation and Swing implementation details out of `microproject_core` in reversible slices. Keep POD serialization frozen and preserve MPO backward reads.
3. Reduce task-table/Gantt state divergence by implementing the task-view architecture from the current `master`, one verified transition at a time.
4. Ship only features whose physical interaction, model/view result, Undo/Redo, and persistence behavior are verified. Do not use a passing compile or button dispatch as release evidence.

## Six-month sequence

| Window | Work and issue links | Exit evidence |
|---|---|---|
| Oct–Nov 2026 | Finish the #774 server-route inventory and remove live server-mode selection from ordinary file opening. Establish the current-master baseline for #737, #403/#408–#411, #414, #729, and #773. Correct stale issue claims that rely on unmerged branches. | Normal Open selects local importers; MPO/POD/MSP exchange tests pass; architecture and test inventories refer to the current master SHA. |
| Nov–Dec 2026 | Start #737 with the smallest presentation boundary that can be moved without changing project semantics. Keep a checked, shrinking allowlist for remaining core Swing/presentation references. Inventory POD/MPO serialization-sensitive types before moving any class. | New core presentation references fail the build gate; focused core tests and the appropriate import/save/reload tests pass for each slice. |
| Dec 2026–Jan 2027 | Rebuild the #403 P2/P3 identity and projection work on current master; do not copy the unmerged `refactor/task-view-architecture-403` branch. Separate per-view row identity from mutable `GraphicNode.row`; migrate one table/Gantt selection journey. | Same task identity remains selected across table/Gantt after sort/filter; stale gestures reject deterministically; the two-view isolation and MPO reload cases pass. |
| Jan–Feb 2027 | Continue #403/#408 command slices and #409/#410 publication/transaction work only where a complete caller set can move together. Advance #414 shared command/input routes after their common contract is explicit. | Each changed gesture has one mutation/Undo route, a named invariant, and before/after model and rendered-view checks. No parallel old/new route remains for that gesture. |
| Feb–Mar 2027 | Complete release-critical #724 dialog checks and the relevant #773 suite consolidation. Run #728 hands-on defect discovery against the fresh install layout and register reproducible findings. Review #453/#769 command gaps for any blocker in the shipped workflows. | No known critical task, Gantt, CCPM, file-open, or save/reload defect; GUI evidence is current for the final candidate. |
| Mar–Apr 2027 | Freeze scope, resolve release blockers, run full module tests, GUI smoke and scheduled full GUI audit, build/package/launch the Windows distribution, refresh the user guide and release notes, then publish Beta 3. | All release gates below pass on one exact commit; published artifacts and checksums match that commit. |

Dates are planning windows, not permission to skip a gate. If a foundational slice fails its acceptance contract, defer affected feature work and move the release date rather than accumulate compatibility risk.

## Issue disposition

| Priority | Open issues | Beta 3 decision |
|---|---|---|
| P0 — architecture and product independence | [#774](https://github.com/tetsuji16/ProjectLibre/issues/774), [#737](https://github.com/tetsuji16/ProjectLibre/issues/737), [#403](https://github.com/tetsuji16/ProjectLibre/issues/403), [#408](https://github.com/tetsuji16/ProjectLibre/issues/408), [#409](https://github.com/tetsuji16/ProjectLibre/issues/409), [#410](https://github.com/tetsuji16/ProjectLibre/issues/410), [#411](https://github.com/tetsuji16/ProjectLibre/issues/411), [#414](https://github.com/tetsuji16/ProjectLibre/issues/414) | Release-driving milestones. Work from current `master`; old branch-only completion evidence for #403/#409–#411 is invalid. Remove server-only routing before deleting compatibility adapters. Do not combine these epics into one refactor. |
| P1 — release confidence and UI defects | [#724](https://github.com/tetsuji16/ProjectLibre/issues/724), [#728](https://github.com/tetsuji16/ProjectLibre/issues/728), [#729](https://github.com/tetsuji16/ProjectLibre/issues/729), [#773](https://github.com/tetsuji16/ProjectLibre/issues/773), [#388](https://github.com/tetsuji16/ProjectLibre/issues/388), [#483](https://github.com/tetsuji16/ProjectLibre/issues/483) | Address reproducible defects and test gaps that affect release paths. #388 is currently traced to the native OS/printer-driver dialog; do not replace that dialog just to change an OS-owned status label. #483 needs complete screen evidence before closure. #729 is a continuous duplication audit, not a reason for a broad pre-release rewrite. |
| P1 — runtime dependency cleanup | [#242](https://github.com/tetsuji16/ProjectLibre/issues/242), [#752](https://github.com/tetsuji16/ProjectLibre/issues/752), [#753](https://github.com/tetsuji16/ProjectLibre/issues/753) | Keep the known Commons Collections 3 runtime dependency until the report migration is supported by the official JasperReports 7 conversion workflow and rendering evidence. Do not ship a milestone dependency or globally exclude a required library to meet a count. |
| P2 — command compatibility and ribbon | [#453](https://github.com/tetsuji16/ProjectLibre/issues/453), [#765](https://github.com/tetsuji16/ProjectLibre/issues/765), [#769](https://github.com/tetsuji16/ProjectLibre/issues/769), [#770](https://github.com/tetsuji16/ProjectLibre/issues/770) | Finish only the command or accessibility gaps that block core workflows. Ribbon customization and visual parity are not prerequisites for Beta 3 unless a concrete release-critical gap is found. Keep the Swing `ModernRibbonPanel`; ADR-0002 rules out a Flamingo ribbon replacement. |
| P2 — user requests and translation | [#215](https://github.com/tetsuji16/ProjectLibre/issues/215), [#254](https://github.com/tetsuji16/ProjectLibre/issues/254), [#413](https://github.com/tetsuji16/ProjectLibre/issues/413), [#750](https://github.com/tetsuji16/ProjectLibre/issues/750), [#761](https://github.com/tetsuji16/ProjectLibre/issues/761) | Verify open requests against current master before scheduling: preferences and resource sorting have substantial prior implementation history. Fix confirmed Japanese release-string defects; defer the alternative-calendar concept and broad 34-locale refresh. Task sorting must use the existing hierarchy-aware projection, never a separate JTable sorter. |
| P3 — broad modernization and low-risk maintenance | [#84](https://github.com/tetsuji16/ProjectLibre/issues/84), [#228](https://github.com/tetsuji16/ProjectLibre/issues/228), [#595](https://github.com/tetsuji16/ProjectLibre/issues/595), [#738](https://github.com/tetsuji16/ProjectLibre/issues/738), [#740](https://github.com/tetsuji16/ProjectLibre/issues/740), [#743](https://github.com/tetsuji16/ProjectLibre/issues/743), [#748](https://github.com/tetsuji16/ProjectLibre/issues/748) | Continue only when directly supporting P0/P1 work or when a bounded, independently verified slice fits. Do not use converted-file counts as a release metric. Defer broad dead-code cleanup and diagram modernization that do not reduce an identified release risk. |

## Scope boundaries

- Keep standard local Open Project, supported MPP/XML/XLSX exchange, MPO backward reads, and the frozen POD serialized wire format.
- Treat `com.projectlibre1` aliases required by fixed old POD fixtures as compatibility bridges, not as permission to restore old application modules or UI routes.
- Keep server-generated exchange data classes when current MPP/POD/XLSX paths require them; remove server login/project-list/load behavior only after caller and data-flow audits.
- Do not adopt new UI frameworks or a parallel ribbon implementation during this release cycle.
- Keep appearance-only and speculative features behind the current command/selection and dialog recovery work.

## Beta 3 release gates

1. Exact-head `clean build` and all owning module tests pass; test reports contain no unexplained skips.
2. `verifyArchitectureBoundaries`, dependency allowlist, and naming checks pass. Core's presentation allowlist only shrinks through reviewed migrations and does not gain unexplained entries.
3. GUI smoke passes on the freshly generated install layout. The scheduled/full GUI audit covers its required Japanese/English and 100/125/150% legs; missing legs or unpaired skips block release.
4. Local POD open/save/reload, supported MPO backward-read fixtures, fixed POD fixtures, MSP exchange, and the touched Undo/Redo journeys pass.
5. Windows package contents are inspected and the packaged app launches. Release notes, user guide, download site, checksums, and GitHub Release assets describe the same build.
6. Any deferred P0 issue is listed with its precise remaining scope and evidence; no known data-loss, broken file-open, or task/Gantt command defect is waived.

## Work started with this plan

The ordinary Open Project path previously selected `ServerLocalFileImporter` for POD files whenever the caller reported a non-standalone session. The current desktop startup no longer has an active server session, so this preserved a dead server branch in the normal load decision. POD files now always select `LocalFileImporter`, and a regression test verifies both values of the old mode flag produce the same local route. The flag overload remains temporarily as a forwarding adapter while its large UI caller is simplified. The server importer registration remains temporarily available as a legacy adapter until its serialized-data and caller audit is complete; this change does not delete it or alter POD bytes.

Verification for this first slice is pending the focused application/exchange tests and Windows GUI acceptance on the proposed change. This plan does not claim Beta 3 readiness.
