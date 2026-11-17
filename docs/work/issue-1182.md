# Issue #1182 — Improved Timescale labels

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1182
Baseline: `a5f6cdf03` (origin/master), clean isolated worktree

## Contract

The reporter clarified the nine parent > child labels and subsequently requested Title Case and removal of the status position. The later comment withdraws moving the Timescale command: leave it at the bottom. Preserve zoom levels and order; the two hour levels remain as currently supported. This is a product label choice, not a claim of exact Microsoft Project parity.

| ID | Acceptance | Evidence |
|---|---|---|
| A1 | Nine configured chooser hierarchies use requested English Title Case, no prefix or position | `DocumentStatusBarTest`; physical chooser Robot |
| A2 | Status shows the hierarchy in Title Case without `(3/9)`; Japanese also omits position | `DocumentStatusBarTest`; physical visible status assertion |
| A3 | Timescale command stays at bottom; scale order and functionality unchanged | No changes to command placement or scale configuration; physical chooser Robot |
| A4 | Missing scale remains displayable and localized hierarchy remains supported | `DocumentStatusBarTest` |

## Current slice

Next: push candidate, confirm hosted checks, and integrate. Physical chooser and visible status verified locally; no project data mutation is involved.

## Evidence

- `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.DocumentStatusBarTest" --console=plain` — passed, 16 tests, 2026-11-17. Initial run failed on Japanese resource before its correction; rerun passed.
- `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.timescaleRibbonChooserPhysicallyShowsConfiguredHierarchyLabels" --max-workers=1 --console=plain` — passed with visible status and physical chooser assertions, 2026-11-17.
