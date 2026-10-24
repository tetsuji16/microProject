# Issue #895 — 列見出しを折り返して全文を表示する

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/895
Contract frozen at: 2026-10-24, issue body as filed 2026-10-23
Baseline HEAD: `87ade4b35` (#899 branch, PR #900)
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Column headers that do not fit wrap onto several lines, and the header row
grows to fit. Wrapping can be switched off per column, and the setting is saved
with the table layout. The sheet header, the corner and the Gantt timescale
keep one height.

Out of scope: wrapping cell text (MSP "Wrap Text"); a full Field Settings
dialog; printed headers (they stay single-line unless the change is trivial).
Blockers: none.

Document-derived compatibility decision (not a measured MSP run): Microsoft
Support "Table Definition dialog box" defines a per-column "Header Wrapping":
the heading can wrap to multiple lines, and No cuts it off. "Auto-adjust header
row heights" is selected by default. The page does not state the wrapping
default; on (as the issue states) is inferred. With no Field Settings dialog
in the product, the per-column switch is a check item in the header popup menu
(product route, not MSP's dialog).

## Design

- One header-height source per view: the sheet computes the tallest wrapped
  header (minimum 30) and applies it to the sheet header and the Gantt
  timescale.
- `SpreadSheetFieldArray` keeps a parallel per-column wrap flag (default on),
  stored in MPO `layout.json` as `"wrapHeader": false` only when off. Older
  files load as on. The POD format is unchanged.
- The header popup item "Header Text Wrapping" / 見出しの折り返し commits
  through `applyColumnLayoutChange`, so it gets Undo/Redo.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Default: headers wrap to full text; header height fits | open | |
| A2 | Per-column off switch; saved/reloaded with the layout; undoable | open | |
| A3 | Corner, sheet header and Gantt timescale share one height; #860 outline closed | open | |
| A4 | ja/en, UI scale 1.0/1.5 physical GUI | open | |

## Current slice

**Next slice:** implementation and tests (delegated).
**Done when:** build passes; the grid GUI test passes in ja 1.0/1.5 and en.
**Then:** integration (PR) and close.

## Remaining blockers

- none

## Latest evidence

- Tests/build: pending
- Physical GUI/manual: pending
- CI: pending
