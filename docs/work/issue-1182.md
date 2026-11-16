# Issue #1182 — Improved Timescale labels

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1182
Contract frozen at: 2026-11-16, issue body and reporter clarification
Baseline HEAD: `98065edaa` (origin/master)
Baseline working tree: clean in isolated worktree

## Goal and scope

Show Timescale chooser entries as only the configured parent/child hierarchy (for example, `Month > 3 days`, `Week > Day`, `Day > 6 hours`, `Year > Quarter`), without the `Timescale:` prefix or position suffix. The status bar may retain its zoom wording. Keep existing scale order and zoom levels.

Out of scope: adding finer zoom levels or changing scheduling/timescale behavior.
Blockers: candidate hosted checks and physical verification are recorded below.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Timescale chooser entries contain only configured parent/child intervals; all nine shipped scales match their configured hierarchy | pass | `DocumentStatusBarTest.configuredNineScaleChoicesShowOnlyTheirParentAndChildIntervals` |
| A2 | Missing scale uses a stable Timescale/index fallback; localized separator works in English/Japanese | pass | Focused fallback and bundle tests |
| A3 | Physical Timescale chooser display check | pass | Robot opened Format > Timescale and verified the visible nine-row hierarchy chooser; `9b3c900e7` |

## Current slice

**Next slice:** Push the rebased candidate and confirm required build/GUI checks.
**Done when:** Required hosted checks pass on the candidate containing `dcd2f89db` (#1184) and the Robot evidence is retained.
**Then:** request integration review.

## Remaining blockers

- Candidate hosted build and GUI checks after rebasing onto master.

## Latest evidence

- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.DocumentStatusBarTest" --console=plain` — passed, 2026-11-16 (20 tasks; includes exact expected hierarchy for all nine configured levels).
- Physical GUI: `./gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.timescaleRibbonChooserPhysicallyShowsConfiguredHierarchyLabels" --max-workers=1 --console=plain` — passed on `9b3c900e7`; the Robot opened the Format tab Timescale command, verified the chooser rendered all nine localized hierarchy rows, and physically opened the dropdown.
- Unit tests: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.DocumentStatusBarTest" --console=plain` — passed on `9b3c900e7`.
- Candidate includes master merges `dcd2f89db` (PR #1184) and `96bc3da4d` (PR #1173). Hosted checks pending push.
