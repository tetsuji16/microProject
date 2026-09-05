# GUI recovery sequence

## Operating rule

Do not implement new GUI features while a preceding recovery phase is open.
Each phase fixes a shared cause, adds/reuses one command-contract fixture, and
passes its exit gate before the next phase begins.  The order intentionally
favors foundational command state over individual ribbon buttons.

## Phase 0 — Baseline and freeze

**Purpose:** establish one reproducible starting point.

- Preserve the current debug log and issue links (#60, #266, #330, #460,
  #462, #463, #464).
- Regenerate `installDist`; record application version, locale, DPI, fixture,
  and the exact user route for each failure.
- Do not close issues or claim a button healthy from `action-complete` logs.
- Do not add feature work or isolated UI workarounds.

**Exit gate:** a command inventory exists with route, required selection,
expected model/view delta, Undo behavior, and issue owner for every reported
failure.

## Phase 1 — Canonical command and selection foundation

**Purpose:** eliminate the common “enabled but no-op after ribbon focus” fault.

- Create one typed active-task selection resolver owned by the document/view
  layer.
- Route command enablement and execution through the same resolver and
  precondition validator.
- Define a common semantic `CommandOutcome` for changed/rejected/failed,
  including selected stable IDs and active view.
- Wire diagnostics to this outcome; remove logs that prove dispatch only.

**Initial commands:** Task Information (#330), Link/Unlink (#266), Indent,
Outdent, Expand, Collapse, Hide Selected, Show All.

**Exit gate:** Robot selects a task or two tasks, then clicks each physical
ribbon route.  Enablement, execution selection, and diagnostic selection agree.
Selection shortage is disabled or explicitly explained, never silent.

## Phase 2 — Structural mutations and reversibility

**Purpose:** make hierarchy, dependency, and visibility changes correct once.

- Consolidate Indent/Outdent, Expand/Collapse, Link/Unlink, and Hide/Show on
  the canonical command pipeline.
- Use one reusable outline fixture: parent with two children, first/last
  sibling, leaf, two adjacent tasks, and locked/read-only task.
- Correct Tab, Shift+Tab, Ctrl+arrow, ribbon, menu, and popup routes only by
  delegating to the canonical command; do not create parallel key handlers.

**Exit gate:** for every structural command, assert model delta, spreadsheet and
Gantt delta, one Ctrl+Z, one Ctrl+Y, and save/reload where the mutation is
persisted.  Run focused tests plus `:micrproject_ui:test` and
`:micrproject_ui:guiTest --max-workers=1`.

## Phase 3 — Crash-free view and dialog initialization

**Purpose:** eliminate commands that currently throw before showing useful UI.

- Fix Task/Resource Usage Detail field-array initialization (#462).
- Fix Timesheet’s `DefaultTableModel`/`CommonSpreadSheetModel` boundary (#463).
- Verify resource-pool creation opens the specified resource view, not an
  empty Gantt (#461).

**Exit gate:** real Robot click opens each view/dialog, its content model is
non-null and populated as expected, Close/Escape works, and no
`UI_COMMAND_FAILURE` or uncaught exception is logged.

## Phase 4 — Window shell and command parity

**Purpose:** make every project window behave as one product.

- Consolidate primary and secondary document-window construction (#395).
- Ensure ribbon/command chrome, title format, close behavior, focus, and
global shortcuts follow one explicit policy.

**Exit gate:** open two projects and a resource pool, switch between them,
invoke commands, and close each through the actual title-bar close route.
No orphan window, missing ribbon, stale active frame, or inconsistent title.

## Phase 5 — Shared dialog layout system

**Purpose:** remove clipping at its layout root rather than per-label patches.

- Establish a shared dialog form/layout policy that honors preferred component
height and viewport bounds.
- Migrate affected dialogs/tabs through that policy: task information,
calendars, baseline, project information, recurring task, find, and resource
pool dialogs (#460).
- Do not use arbitrary per-language pixel offsets as fixes.

**Exit gate:** reusable visual harness checks Japanese and English at
100/125/150% DPI.  All labels, controls, tabs, buttons, and title bars are in
their viewport and non-overlapping; button Close/Cancel/Help actions work.

## Phase 6 — Diagnostic hardening and release readiness

**Purpose:** prevent silent recurrence and make failures actionable.

- Expand debug outcomes to model/view/Undo facts for each command family.
- Retire superseded one-off tests after equivalent routes are consolidated.
- Run the full GUI suite and the appropriate module suites from a fresh
`installDist`; review artifacts and issue status.
- Keep unresolved defects open with explicit scope and risk.  Do not use a
passing headless build as evidence of GUI acceptance.

**Exit gate:** every issue closed in these phases has a command contract,
focused invariant test, physical Robot evidence, and required visual or
persistence evidence.  `git diff --check`, status, and release verification
are clean.

## Work selection within a phase

Choose the next item by this order:

1. data loss, crash, or inability to close a window;
2. shared command/selection defect affecting several controls;
3. Undo/Redo or save/reload corruption;
4. layout/accessibility defect affecting a shared dialog system;
5. isolated command or cosmetic defect.

If a report appears to belong to an earlier phase, return to that phase rather
than starting a workaround in a later one.
