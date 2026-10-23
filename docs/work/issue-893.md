# Issue #893 — 日本語UIの期間単位を日本語で表示する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/893
Contract frozen at: 2026-10-24, issue body as filed 2026-10-23
Baseline HEAD: `821f3d57e` (origin/master)
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

With `user.language=ja`, duration and work cells show Japanese unit names
(`3 日`, `4 時間`, `5 経過日`) and duration input accepts both the Japanese names
and the English abbreviations (`3d`, `24h`, `9ed`).

Out of scope: other untranslated ja keys (#254); full-width digit input.
Blockers: none.

Document-derived compatibility decision: Microsoft Project Japanese shows
durations with Japanese units (分, 時間, 日, 週, and 経過 for elapsed units),
per the Japanese edition of Microsoft Support "Change a task duration". Month is
shown as `か月` so it cannot be read as a calendar month; `月` is still accepted.
Not a measured MSP run.

## Design

`client_ja.properties` `Units.*` keys keep four display styles (now Japanese)
and list the English names after the fourth entry. `DurationFormat.UnitNames`
treats entries after the fourth as input-only aliases, quotes every alias in
the parse regex, and is built once from `Messages` instead of in each
formatter constructor. Durations persist as encoded millis; exporters do not
use the localized text (XLSX writes millis, MPX/MSPDI go through MPXJ).

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | ja shows minute/hour/day/week/month/elapsed units in Japanese; `?` kept | pass (unit) | `DurationUnitNamesTest` |
| A2 | ja accepts Japanese names and English abbreviations with the same value | pass (unit) | `DurationUnitNamesTest`; U-26 types `1 day`/`6 hours` physically in ja |
| A3 | Display-only: MPO/MPP values unchanged; save/reload shows the same text | pending | U-26 MPO reopen assertions (ja) |
| A4 | No English display/input regression | pass (unit) | `DurationUnitNamesTest.englishNamesAreUnchanged`; module tests pinned to en-US |

## Current slice

**Next slice:** run U-26 physical GUI journey in ja and en.
**Done when:** both pass on this branch's SHA.
**Then:** integration (PR) and close.

## Remaining blockers

- none

## Latest evidence

- Tests/build: pending
- Physical GUI/manual: pending
- CI: pending
