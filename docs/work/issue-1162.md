# Issue #1162 — ribbon control kinds and activation

Baseline: `a568f45ec` (isolated worktree). Original contract: both kind and activation gap lists empty, #1154 reconciliation complete, and JA/EN 100/125/150% visual evidence. All listed children are closed; their closure does not itself prove the parent criteria.

| Criterion | State and evidence |
|---|---|
| Kind and activation gaps | Existing ratchets are empty, but two later-added font color rows are still `PENDING_VERIFY`; actual controls currently open dialogs. |
| #1154 reconciliation | Open: [Microsoft's ProjectControls workbook](https://github.com/OfficeDev/office-fluent-ui-command-identifiers/blob/main/Microsoft%20365/Current%20Channel/projectcontrols.xlsx) lists `FontFillBackColorPicker` and `FontColorPicker` as `gallery` on TabTask/GroupFont. Their CSV rows are `BUTTON`/`DIALOG`; the implementation needs gallery behavior before claiming parity. Review remaining BASELINE reasons. |
| JA/EN 100/125/150% visual check | Full hosted [run 43999020531](https://github.com/tetsuji16/microProject/actions/runs/43999020531) passed on `a1add999f`: Japanese 100% functional plus JA 125/150% and EN 100/125/150% visual legs. Revalidate any later relevant layout changes. |

## Active slice — preserve committed input values

The existing gallery/combo/edit control adapters put values in ActionEvent, but the shared MenuRibbonCommandSource dispatcher drops them. A pre-fix targeted test failed because the handler received the ribbon ID instead of the chosen value. Route input values through the canonical dispatch explicitly; preserve ordinary command identifiers. Add an actual input-control-to-dispatch integration test. Done when focused ribbon tests and CI pass. Next: implement and physically verify the two color galleries; #1162 remains open until all original criteria pass.
