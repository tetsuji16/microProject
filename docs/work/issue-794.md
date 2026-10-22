# Issue #794 — Roadmap gate M2: offline desktop workflow parity

State: OPEN. Issue: https://github.com/tetsuji16/ProjectLibre/issues/794
Dependencies: #792 CLOSED; #793 CLOSED (met). Contract frozen 2026-10-20.
Contract evidence began at dirty worktree `40f5ee8d575a3327e727b9f62182c07aa55d2178`;
current HEAD: `1ca2b41485e4403e1d3b76d2fa0d8dd903c20217`, with three local commits
ahead and three origin commits not yet integrated. This is scoped local evidence,
not full exact-head release proof. Closed children do not establish parent acceptance.

## Goal and frozen acceptance

Complete offline desktop parity, verify all evidence, and release last.

| ID | Required evidence | State |
|---|---|---|
| M2.1 | #408/#414 physical routes; text/date/numeric edits, sort/filter/group/projection; #750 Sort Order; #761 permanent renumber confirmation/Undo/persistence | Open; #750/#761 closed. |
| M2.2 | #403/#409/#410/#411/#453/#769 scheduling, links/lags, transactions, diagnostics, Undo/Redo, MPO reopen, failure atomicity | Open. |
| M2.3 | #453/#769/#772 resource/assignment/progress/baseline scope, provenance, totals, actuals, splits, outcomes, Undo/Redo, MPO reopen | Open; #772 closed. |
| M2.4 | #453/#765/#769/#770 command/view/report/print source, enablement, physical route/result, model purity, persistence, locale/DPI | Open. |
| Common | Focused tests; physical route when changed; atomicity; unrelated-state preservation; exact build/fixture/test/screenshot/log | Open. |

Frozen detail: M2.1 includes text/date/numeric fields, ties/empty values, both
sort directions, filter/group, selection identity, table/Gantt alignment, and
unchanged model; display sort is separate from #761 permanent renumber. M2.2
requires Microsoft-rule literal dates, FS/SS/FF/SF, intended mutations,
diagnostics, Undo/Redo/MPO and injected-failure atomicity. M2.3 requires
selected/all scope, proposed resources, leveling defaults/provenance, assignment
totals, status date/actuals/baseline slots, outcome/selection, manual split
preservation, Undo/Redo/MPO. M2.4 requires view-only model purity, relevant
persistence and locale/DPI; reports compare rendered fixture values and print
pages in ja/en. Unsupported operations remain absent, never dummy-enabled.
For GUI failures, preserve artifacts and model state; classify product, fixture,
or environment before edits, and require root-cause review after two failed fixes.

## Current verified evidence

- #408 U-26 base and filtered/grouped/sorted projection routes pass physical
  edits with stable identity, unrelated-state checks, Undo/Redo and MPO reload.
  Exact commands, JUnit and artifacts: [status log](../msp-implementation-status.md)
  sections 2362 onward. Projection is document-derived, not measured MSP behavior.
- #414 U-32 Gantt/table keypad zoom, Resource View keypad no-op, U-33 bar-edge
  auto-scroll/Undo/Redo/MPO, and two-document shortcut isolation pass. Exact
  command/SHA/environment/evidence: [status log](../msp-implementation-status.md)
  sections 2332–2387. Other #414 gates remain open.
- #414 task Name-cell double-click opens Task Information for the clicked task
  among two; names/start/duration/progress and Undo list/state + Redo state are
  unchanged before and after Escape; project window remains visible. Microsoft
  documents this route for Project Online Desktop Client and Project Standard /
  Professional 2016–2024: [Link tasks in a project](https://support.microsoft.com/en-au/project/link-tasks-in-a-project),
  lines 197–203, checked 2026-10-20. This is document-derived, not an empirical
  MSP run. Command passed 1/1, zero failures/errors/skips:
  `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.spreadsheet.TaskInformationGuiAcceptanceTest.taskNameDoubleClickOpensTaskInformationForTheClickedTask" --console=plain`
  JUnit: `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.spreadsheet.TaskInformationGuiAcceptanceTest.xml`;
  captures: `modules/microproject_ui/build/reports/guiTest-artifacts/task-information-tab-00.png` … `task-information-tab-07.png`.
  SHA above; Windows 11 Pro, ja-JP, default 100%, 2560×1440 unchanged.
- #868: 57,600,000 ms Work survives MPO reload; provenance and MPP task import
  remain open. Remaining detailed M2.2/M2.3 evidence and limits are in the
  [status log](../msp-implementation-status.md).
- Phase 2 structural gate passed (218 UI unit/ja-100% GUI cases, one permitted
  conditional high-DPI skip); exact-head locale/DPI, acceptance and release gates
  remain open.
- #414 fixed-anchor range selection passed physically: row-header Shift range
  upward `{0,1}`, then cell Shift range downward `{1,2}`; stable IDs, table/Gantt
  selection coordinates, model and Undo/Redo stayed unchanged. No product defect
  reproduced or production change made. Command:
  `.\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalCellAndColumnHeaderClicksKeepSelectionHighlightCoordinatesAligned" --console=plain --max-workers=1`
  passed 1/1, zero errors/failures/skips. JUnit:
  `modules/microproject_ui/build/test-results/guiTest/TEST-com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.xml`
  (`2026-10-19T22:48:45.365Z`). Worktree SHA above; Windows 11, ja-JP, 100%,
  2560×1440 unchanged. Microsoft cross-control selection scope is a
  document-derived decision using the same Microsoft source above, not a
  measured MSP outcome.

## M2.2 schedule failure atomicity — verified slice

`executeScheduleEdit` now uses the existing project atomic boundary and restores
the existing schedule snapshot only when its callback throws during apply.
Regression calls real `ScheduleService.setCompleted`, then injects failure;
task/assignment schedule+progress, Undo+Redo, revision, pending events,
observers and mutation barrier remain unchanged. Post-commit publication and
other gateway policies are untouched. Before-fix test failed on task
completed-through (`expected 1792396800000`, actual `1792688400000`); JUnit:
`build/task-evidence/issue-794-schedule-pre-fix/TEST-com.microproject.pm.graphic.spreadsheet.command.TaskCommandGatewayTest.xml`
(SHA256 `3E2D225BC5A93E92EE7A52D40F29CC202FD374A4DD03D75E3B0EEF75A57AE4A0`).
After fix: TaskCommandGatewayTest 14/14, GanttInteractorDragTest 4/4, and full
`:microproject_ui:test` 1188/1188. Intermediate suite failure was fixture-only
Swing cache update off EDT; moving setup to EDT fixed it. Physical progress /
resize / split Robot passed 1/1, zero failures/errors/skips using
`\.github\scripts\run-gui-acceptance.ps1 -TestFilter com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotResizeProgressAndSplitUseTypedScheduleGatewayWithUndoAndPersistence -Locale ja -Scale 1.0`.
JUnit:
`modules/microproject_ui/build/reports/guiTest-artifacts/focused-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.robotResizeProgressAndSplitUseTypedScheduleGatewayWithUndoAndPersistence-ja-1-0-junit/TEST-com.microproject.pm.graphic.views.GanttBarDateDragGuiAcceptanceTest.xml`;
screenshots are in sibling `...-6fa634b0e2d4440285ae9034ab26050d/`. Head
`37c07cbf8092389afc2f995ee95640b6764b3a41`, dirty worktree; Windows 11, ja-JP,
100%, 2560×1440. Barrier probe daemonization focused rerun: 1/1. Broader M2.2
remains open. M2.2 MPO calendar inheritance now rejects invalid
`baseProjectCalendar` markers (including null, numeric, string, and false), a
missing project base, and contradictory nested + project bases. Regression uses
the existing explicit task-calendar round-trip fixture and checksum rewrite;
before the fix, the focused test failed because a string marker was silently
accepted. Pre-fix JUnit: `build/task-evidence/issue-794-mpo-calendar-prefx/TEST-com.microproject.exchange.MpoFileImporterTest.xml`
(1 test, 1 failure, timestamp `2026-10-19T23:00:04.752Z`). After the fix, the
focused round-trip passed; full `MpoFileImporterTest` passed 123/123 (0
failures/errors, timestamp `2026-10-19T23:00:29.474Z`). Tested HEAD
`038e27c216d8845fd82b2055b8ab237ad6157b0b`, dirty worktree; importer/test/packet
edits plus pre-existing core changes remain uncommitted. This malformed-input
rule is an MPO rejection decision; no Microsoft source applies. Next:
independently authored negative-lag MPP READ fixture (candidate `New Product.mpp`,
UID 24→27 FS−15 days).

## Remaining gates

Latest release attempt: tag `v0.0.24.1250`, workflow
[39774952702](https://github.com/tetsuji16/ProjectLibre/actions/runs/39774952702),
did not publish binaries because the visual GUI matrix failed. Functional-ja-100
and visual-ja/en-100 passed; at 125% failures were 28/28, and at 150% failures
were 42/43 (one conditional
skip). The runner's 1024x768 physical screen becomes 819x614 / 683x512 logical.
Confirmed causes include text-button overflow in the shared dialog button area,
dialogs shrinking below their preferred content, and ribbon tests locating only
currently visible commands instead of physically scrolling the viewport. A local
Robot rerun was blocked by Chrome overlap (`GUI_ENVIRONMENT_CONTENDED`); no input
was dispatched. Current fixes have 1,251 UI unit tests passing and GUI test sources
compiling, but need exact-head CI physical proof.

Current slice: validate the shared responsive dialog/button changes and physical
ribbon viewport scrolling in the 125/150% CI legs; repair any remaining product or
fixture defect, then rerun the full release matrix before considering a new tag.

M2.1–M2.4 and Common remain OPEN. #414 also requires all available
Ribbon/menu/toolbar/popup routes, three hostile reviews, and exact-head
locale/DPI GUI acceptance. Production Java net-reduction is an effort goal, not
a release gate, per the user's instruction. Other gaps include
M2.1 field types/boundaries; M2.2 independent negative-lag MPP READ fixture;
M2.3/#868 provenance and remaining resource/baseline scope; M2.4
command/view/report/print/locale coverage. Run candidate-SHA CI, fresh
installDist, full functional GUI and ja/en 100/125/150 visual gates, applicable
packaging checks, and final status/diff review. Preserve
`.codex-remote-attachments/`; release only after all acceptance/release checks.
