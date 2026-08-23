# Open issue triage — 2026-08-23

This inventory was checked against the open GitHub issue list and the current
`master` worktree. An issue remains open on GitHub when implementation is
waiting for reporter confirmation, is intentionally tracked as an umbrella, or
still has follow-up work; the open state alone does not mean that its original
bug is still reproducible.

## Status

| Issue | Current status | Evidence / remaining work |
|---|---|---|
| [#366](https://github.com/tetsuji16/ProjectLibre/issues/366) | Partial | Project-scoped custom-report persistence, Blank/Chart/Table/Comparison templates, Chart defaults, and the chart cache lifecycle are covered. Histogram uses `computeHistogram` while Chart uses `computeValues`; remaining work is a dedicated Resource Graph UI for peak allocation/over-allocation and a richer editable report-chart settings surface. |
| [#356](https://github.com/tetsuji16/ProjectLibre/issues/356) | Implemented for current MPOF drafts | `MpoFileImporter` reads earlier draft layouts and rewrites them in the current MPOF layout. `MpoFileImporterTest` covers current/draft manifests, checksums, round trips, and unknown entries. Future format versions still require an explicit migration policy. |
| [#351](https://github.com/tetsuji16/ProjectLibre/issues/351) | Completed locally | README baseline metrics were recomputed from `0530be22`: 2386/2386 paths and 1538205 changed lines. |
| [#347](https://github.com/tetsuji16/ProjectLibre/issues/347) | Implemented | A plain task-cell click now selects the full task row while retaining the clicked cell as a separate active-cell coordinate for its focus border. Ctrl/Shift and keyboard selection behavior are preserved and headless regression-tested. |
| [#344](https://github.com/tetsuji16/ProjectLibre/issues/344) | Partial / draft format | MPOF `.mpo`, XML manifest, metadata, settings, JSONL operation log, legacy draft reads, and unknown-entry preservation are implemented. The specification is explicitly still a draft; finalizing versioning and compatibility policy remains. |
| [#338](https://github.com/tetsuji16/ProjectLibre/issues/338) | Blocked by release credentials | About-dialog discovery, MSI digest verification/staging, and update4j bootstrap tests exist. A release-grade feed still needs CI access to the update-signing private key plus packaging/release workflow integration; without that key the bootstrap correctly refuses remote replacement. |
| [#330](https://github.com/tetsuji16/ProjectLibre/issues/330) | Implemented; manual confirmation pending | Task Information routing for Gantt double-click and ribbon Information is covered by focused UI tests. A real desktop check against the current installed distribution remains. |
| [#267](https://github.com/tetsuji16/ProjectLibre/issues/267) | Implemented; manual confirmation pending | Summary-task date rollup is written back to the current schedule. `PodRoundTripTest.groupedTaskFollowsChildDateUpdate` covers the regression. Task Information editing should be included in manual confirmation with #330. |
| [#266](https://github.com/tetsuji16/ProjectLibre/issues/266) | Implemented; manual confirmation pending | Unlink removes all incident dependencies; Task Information predecessor/successor tabs also have New and Remove actions. Core and UI regression coverage exists. |
| [#257](https://github.com/tetsuji16/ProjectLibre/issues/257) | Excluded (clean-room foundation) | This is an explicit foundation for the clean-room type consolidation in #152. It is intentionally outside this change set. |
| [#245](https://github.com/tetsuji16/ProjectLibre/issues/245) | Partial migration | Some persisted int-constant types already use enum adapters. Remaining types need one-at-a-time conversion with `fromInt`/`toInt` compatibility tests; a bulk replacement is unsafe for serialized formats. |
| [#228](https://github.com/tetsuji16/ProjectLibre/issues/228) | Partial | Safe known-size capacity fixes include dependency/calendar paths and Resource Leveling's complete task scan. Remaining allocations need profiling and size-bound analysis before changing them; dynamic collections should not receive guessed capacities. |
| [#215](https://github.com/tetsuji16/ProjectLibre/issues/215) | Phase 1 complete | Preferences cover user name, font family/size, row-lines default, and update checking. Remaining candidates are autosave, default format/calendar, theme, date/currency/locale, and per-view settings. |
| [#204](https://github.com/tetsuji16/ProjectLibre/issues/204) | Implemented | Timeline and Team Planner are embedded through `DockableProjectToolView`, routed through `DocumentFrame` top-view activation, and no longer require the modal Team Planner dialog workflow. |
| [#179](https://github.com/tetsuji16/ProjectLibre/issues/179) | Implemented; manual confirmation pending | Gantt full-row highlight, table/chart synchronization, live drag selection, Ctrl/Shift selection, and accidental row-move prevention have focused tests. |
| [#152](https://github.com/tetsuji16/ProjectLibre/issues/152) | Excluded (clean-room) | This clean-room umbrella is intentionally outside this change set. |
| [#84](https://github.com/tetsuji16/ProjectLibre/issues/84) | Not implemented as a repository-wide cleanup | Dead/commented code removal needs an inventory with compiler/reference checks. Next unit is a report-only scan, then small deletions verified by the owning module tests. |
| [#63](https://github.com/tetsuji16/ProjectLibre/issues/63) | Implementation reported; manual confirmation pending | Task font customization is present in the current preference/UI work according to the issue history. A focused persistence/rendering test and installed-app check are still required before calling it complete. |
| [#45](https://github.com/tetsuji16/ProjectLibre/issues/45) | Implemented; manual confirmation pending | Keyboard/drag task movement, live insertion feedback, undo, and the drag-and-drop preference are covered by UI/core tests. |
| [#36](https://github.com/tetsuji16/ProjectLibre/issues/36) | Partial | Resource assignment spreadsheet and ribbon command routing were fixed. Remaining audit items are resource-sheet bindings, usage-view time-phasing, resource information layout, and assignment-pane usability. |

## Verification performed

The following commands completed successfully on the current worktree:

```text
.\gradlew.bat :micrproject_core:test :micrproject_exchange:test :micrproject_ui:test :micrproject_bootstrap:test --console=plain
```

These are automated checks only. Windows desktop behavior for the entries
marked “manual confirmation pending” still needs `clean build installDist` and
the scenarios described in `docs/build-and-run.md`.
