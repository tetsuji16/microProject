# Issue #941 — Format Painter

State: In progress; existing implementation is present, acceptance evidence is incomplete.
Issue: https://github.com/tetsuji16/microProject/issues/941
Contract frozen at: 2026-10-26, open issue body.
Turn baseline HEAD: `e695b33ac2e8453774d13eca134b59e207bc71f5` (2026-10-27).
Turn baseline working tree: existing changes in `docs/MSP_STANDARD_COMMAND_INVENTORY.md`, Japanese book files, Format Painter service/GUI tests, `.codex-remote-attachments/`, `docs/book/`, and sample sync sidecars; preserve all. The scoped GUI-test assertion and this packet update are the only edits made in this turn.

## Goal and scope

Reconcile the current task text Format Painter implementation with every original #941 acceptance criterion and provide focused model and physical GUI evidence. The six copied attributes are font family, size, text color, bold, italic, and strikethrough; background and cell-specific formatting are outside the current product contract. Microsoft source S14 describes copying format to one or multiple targets but does not define Project task-field scope; this bounded attribute list is a document-derived product decision.

Out of scope: Gantt bar, Timeline, and Report-object formatting; cloud services; broad parity claims.
Blockers: exact-candidate GUI acceptance evidence is still required for closure.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Start from one valid source and apply once or continuously to task table targets | pass | Physical journey applies once to two selected targets together, then continuous mode to successive targets (tested tree below). |
| A2 | Visible armed/applied/stopped state; Escape exits mode | pass | Physical journey asserts armed/disarmed selection state, focus transfer, and Escape exit (tested tree below). |
| A3 | Explicit copied attributes and selection scope; no unrelated model-value changes | pass | Six task text attributes are copied to selected editable rows; journey asserts task names, dates, and durations are unchanged (tested tree below). |
| A4 | Undo/Redo and MPO save/reload preserve formatting contract | pass | Physical journey verifies atomic multi-target Undo/Redo and both targets after MPO reload (tested tree below). |
| A5 | Regress no selection, read-only, empty selection, multiple rows, and focus movement; physical source-to-multiple-target journey | pass | Resolver/service unit tests cover empty/no selection and read-only rejection; GUI journey covers mixed-selection disablement, focus transfer, and physical multi-target application (tested tree below). |

## Current slice

**Current slice:** Root cause was test selection leakage: after multi-target apply and Undo/Redo, `clickUntilSelected` could skip its click because the lead row was already the source while several rows remained selected. Replaced the helper call with a physical source-row click and an explicit single-selection assertion. The complete targeted GUI journey and focused unit tests now pass.
**Next slice:** Reconcile this worktree evidence against the exact committed candidate/release SHA before issue closure.
**Done when:** the changed physical multi-row route, focus transfer, model/view state, atomic Undo/Redo, and MPO reload pass on the exact candidate SHA.
**Then:** reconcile exact-candidate GUI evidence before closure.

## Remaining blockers

- GitHub checks for the submitted PR head.

## Latest evidence

- Baseline inspection: `TaskFormatPainterServiceTest` covered style/value preservation, single-target Undo/Redo, no-op/read-only, and failure rollback. The existing Robot journey covered one row per operation, Escape, Undo/Redo, and MPO reload.
- Tests: `:microproject_ui:test --tests "com.microproject.pm.graphic.frames.TaskFormatPainterServiceTest" --tests "com.microproject.pm.graphic.frames.ActiveTaskSelectionResolverTest" --console=plain` passed. A preceding `--rerun-tasks` run also passed for `TaskFormatPainterServiceTest` (20 tasks executed).
- Physical GUI: desktop check showed only ChatGPT and Chrome windows. After replacing the lead-row-only helper with a physical unmodified source-row click and exact single-selection assertion, `:microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload" --max-workers=1 --console=plain` passed, including multi-target formatting, Undo/Redo, MPO reload, continuous applications, Escape, and visible armed/disarmed state.
- Tested revision before PR preparation: original worktree HEAD `293b43fa03bb7e02970664ad81a692539a21433f` plus the then-uncommitted scoped GUI-test changes. During PR preparation, the scoped diff was replayed onto `origin/codex/issue-943-evm` without the unrelated #945 dark-theme commit.
- Exact PR-candidate tests: `:microproject_ui:test --tests "com.microproject.pm.graphic.frames.TaskFormatPainterServiceTest" --tests "com.microproject.pm.graphic.frames.ActiveTaskSelectionResolverTest" --console=plain` passed; physical `:microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.formatPainterCopiesTaskTextStyleOnceOrContinuouslyWithUndoAndMpoReload" --max-workers=1 --console=plain` passed on commit `48435261e322474e75a8f12eb30bf90e07d88a42`. The GitHub GUI run [40243663362](https://github.com/tetsuji16/microProject/actions/runs/40243663362) predates this test diff and is not evidence for the new assertions.
