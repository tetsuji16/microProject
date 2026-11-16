# Issue #1182 — Improved Timescale labels

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1182
Contract frozen at: 2026-11-16, issue body and reporter clarification
Baseline HEAD: `98065edaa` (origin/master)
Baseline working tree: clean in isolated worktree

## Goal and scope

Show Timescale chooser entries as only the configured parent/child hierarchy (for example, `Month > 3 days`, `Week > Day`, `Day > 6 hours`, `Year > Quarter`), without the `Timescale:` prefix or position suffix. The status bar may retain its zoom wording. Keep existing scale order and zoom levels.

Out of scope: adding finer zoom levels or changing scheduling/timescale behavior.
Blockers: physical GUI visual check remains.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Timescale chooser entries contain only configured parent/child intervals; all nine shipped scales match their configured hierarchy | pass | `DocumentStatusBarTest.configuredNineScaleChoicesShowOnlyTheirParentAndChildIntervals` |
| A2 | Missing scale uses a stable Timescale/index fallback; localized separator works in English/Japanese | pass | Focused fallback and bundle tests |
| A3 | Physical Timescale chooser display check | open | Pending GUI run |

## Current slice

**Next slice:** Run the focused GUI visual check for the Timescale chooser.
**Done when:** The chooser shows nine hierarchy-only entries without title or position text.
**Then:** Record the result and request integration review.

## Remaining blockers

- A3 physical GUI visual check.

## Latest evidence

- Tests/build: `./gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.DocumentStatusBarTest" --console=plain` — passed, 2026-11-16 (20 tasks; includes exact expected hierarchy for all nine configured levels).
- Physical GUI/manual: pending.
- CI: pending integration.
