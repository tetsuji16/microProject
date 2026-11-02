# Issue #997 — Resource Information first-row removal and the other tables' row menus

State: IN PROGRESS (logic and headless evidence complete; physical GUI evidence pending)
Issue: https://github.com/tetsuji16/microProject/issues/997
Contract frozen at: 2026-11-02, issue body at intake
Baseline HEAD: `f4f45af3d`
Baseline working tree: clean

## Goal and scope

Decide and enforce what Delete and Cut do for the first (base) row of the availability and
cost-rate tables, and audit the row menus of the other non-task sheets.

In scope: the first-row removal contract, the row/data/dirty/undo state around a refused removal,
and the popup command inventory shared by non-task sheets.
Out of scope: adding an Undo controller to the table rows (see issue #987), and MSP behaviour that
has no equivalent in this fork.
Blockers: none for the code; this VM has no fontconfig/fonts, so the Swing/UI test source set can
only be compiled here. Physical GUI evidence is pending.

## Decision

The first row of an availability or cost-rate table has no effective date: it holds the base
interval (`ValueObjectForInterval.isFirst()`) every later row's range depends on, and later rows
are date changes. Deleting it is refused, and the refusal reaches the user.

This is a *document-derived compatibility decision*: Microsoft's [Enter costs for
resources](https://support.microsoft.com/en-us/project/enter-costs-for-resources) describes the
base rate row and later rows as effective-date changes; the fork's interval model already refused
the removal at the data layer. No installed MSP client was observed.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | First-row Delete and Cut are refused, and the refusal is visible to the user | implemented; UI evidence pending | `ValueObjectForIntervalTableTest.canRemoveKeepsTheFirstIntervalRowOnly` (runs here, green). `ResourceTableEditDirtyTest.deletingTheFirstRowIsRefusedWithAMessage` / `cuttingTheFirstRowIsRefusedWithAMessage` record the installed `AlertPresenter` message but cannot execute on this VM; CI must run them |
| A2 | A refused removal leaves the rendered row, `getList()`, the hierarchy and `project.needsSaving()` unchanged | model/state pass; view assertion pending | `modelRemovalRefusesTheFirstRowAndKeepsRowDataInStep` (runs here; red without `DefaultNodeModel.confirmRemove`). The row-count and `needsSaving` assertions live in the same two UI tests, CI-pending |
| A3 | Later rows stay removable by Delete and Cut | unchanged; not rerun here | `deletingAvailabilityRowMarksProjectForSaving`, `cuttingAvailabilityRowMarksProjectForSaving`, `deletingCostRateRowMarksProjectForSaving`, `cuttingCostRateRowMarksProjectForSaving` are untouched and pass on master; `microproject_ui` tests cannot execute on this VM |
| A4 | No serialized field or format change | pass | no `serialVersionUID` or `serialize`/`deserialize` change; only behaviour added |
| A5 | The other non-task sheets' row menus still offer what their action lists declare | partially verified | `bothTablesOfferRowCommandsFromTheirFirstPopup` (availability, cost rates). The popup builder that #995 restored is shared by every non-task sheet, and it drops an entry only when the sheet exposes no such action. Project and assignment-entry lists are Copy-only; the dependency list is Delete/Copy/Cut through the dependency route. Individual physical checks on those sheets remain pending |

## Current slice

**Next slice:** physical GUI check for A5 — right-click a row in the Project Information,
assignment-entry and Task Information dependency sheets, and check every item in each popup
actually runs. If one is dead, fix it in this issue.
**Done when:** the three sheets' popup commands run against real data, or A5's remaining risk is
recorded as a follow-up issue.
**Then:** close #997.

## Remaining blockers

- Physical GUI evidence for the first-row refusal and for A5. This VM has no fontconfig/fonts and no
  desktop session, so `:microproject_ui:test` cannot execute here; the new UI tests and the Robot
  gate have to run on a machine with a display (Windows is the reference target).

## Latest evidence

- `:microproject_core:test` — BUILD SUCCESSFUL (full module, includes the two new tests).
- `:microproject_exchange:test` — BUILD SUCCESSFUL (MPX import writes these tables directly, so the
  new factory veto cannot block an import).
- `modelRemovalRefusesTheFirstRowAndKeepsRowDataInStep` was confirmed red with
  `DefaultNodeModel.confirmRemove` reverted to `return true;`, then green with the fix.
- `:microproject_ui:compileJava` and `:microproject_ui:compileTestJava` — BUILD SUCCESSFUL. Test
  execution in `microproject_ui` is blocked here by the missing font configuration, which also
  fails the pre-existing `ResourceTableEditDirtyTest` cases.
- CI on the pushed head: pending.
