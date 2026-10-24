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
| A1 | Default: headers wrap to full text; header height fits | pass | `SpreadSheetColumnHeaderWrapTest`; Robot test on `2d64bdc86` |
| A2 | Per-column off switch; saved/reloaded with the layout; undoable | pass | popup toggle, Ctrl+Z/Ctrl+Y, MPO reload in the Robot test; `MpoFileImporterTest` |
| A3 | Corner, sheet header and Gantt timescale share one height; #860 outline closed | pass | Robot test; `CommonTableHeaderActiveOutlineTest` (1.0–1.75) |
| A4 | ja/en, UI scale 1.0/1.5 physical GUI | pass (CI) | runs 40056187083, 40056321554, 40056506982 and 40056708131 on `2d64bdc86`: 22/22 each, 0 skipped |

## Current slice

**Next slice:** merge #896, #898, #900, then #901.
**Done when:** #901 is merged.
**Then:** close #895.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `gradlew build` passed on `2d64bdc86`.
- CI: targeted grid class on `2d64bdc86` passed in ja 1.0, ja 1.5 (twice) and en.
- Root causes found on the way:
  - Column layout commands reset dragged widths; Undo/Redo now apply widths, via `setFieldArrayWithWidths`. A diagnostic showed 44 → 98.
  - The #896 on-screen outline capture rounded inward at 150%. The capture now has a margin; a headless reproducer showed the painted outline is closed.
  - The date-cell click test clicked an off-screen cell at 150%. This also fails on #899 when run alone; the test now scrolls the cell into view.
- Open, not reproduced: `physicalCustomColumnRenameUsesTheHeaderPopupDialog` intermittently lost 1–3 columns after Rename (3 full-class runs on 83da3187a–30a74f1d6, and once in 5 runs alone on #895; never in 4 runs alone on #899). It did not recur in the last 4 full-class runs or 3 single runs. The diagnostic in the test now reports the layout and Undo history if it recurs.
