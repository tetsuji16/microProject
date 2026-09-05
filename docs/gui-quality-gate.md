# GUI Quality Gate

## Purpose

microProject GUI work is complete only when a real user operation produces the
intended persistent model change and the intended visible result.  A dispatched
Swing `Action`, a returned method call, or the absence of an exception is not
evidence of success.

This gate applies to every change under `micrproject_ui`, and to any other
change that changes a GUI-observable command, model projection, persistence
path, or keyboard shortcut.

For the current stabilization order, follow
[GUI recovery sequence](gui-recovery-sequence.md).  Do not skip a phase to
patch a later symptom unless the defect is a data-loss or security emergency.

## The command contract

Each user command must have a testable contract with all of these facts:

| Stage | Required assertion |
|---|---|
| Preconditions | Selection, active document/view, editability, locks, and required number/type of rows are valid. Disabled commands explain which precondition is absent. |
| Dispatch | The physical menu/ribbon/button/shortcut route invokes the one canonical command. |
| Model | The exact expected task, dependency, hierarchy, calendar, or resource state changes. |
| View | The active spreadsheet/Gantt/dialog reflects the new state after EDT repaint/revalidation. |
| Undo/Redo | One Ctrl+Z restores the exact before-state and one Ctrl+Y restores the exact after-state. |
| Persistence | Commands that modify project data survive save/reload. |
| Failure | Invalid input, no selection, locked data, and unsupported views have deterministic feedback; they may not silently return. |

The test must name the state it observes.  `action-complete`, `isVisible`, or
`no exception` by themselves never satisfy Model or View.

## Required test layers

1. **Headless contract test.** Exercise the canonical service/action on the
   EDT and assert before/after model state, undo/redo, and rejection behavior.
2. **Route integration test.** Assert that every menu, ribbon, context-menu,
   and shortcut entry point uses that same command and has the same enabled
   predicate.  Parallel implementations are a defect, not extra coverage.
3. **Robot acceptance test.** In `src/guiTest`, use real `Robot` mouse or key
   input against a visible `MainRibbonFrame` and assert the user-visible result
   plus the model result.  Calling `actionPerformed`, `doClick`, or a private
   helper is not a substitute for this layer.
4. **Visual-layout test.** Every new or changed dialog/tab/ribbon surface is
   captured at 100%, 125%, and 150% Windows scale with Japanese and English
   text.  The test asserts that interactive components and their labels are
   inside the viewport and do not overlap.  A screenshot is retained on test
   failure as evidence, not treated as the assertion itself.

Existing code without all four layers is technical debt.  A change touching
that area must add the missing layers before it is called fixed.

## Efficiency rule: reduce paths before adding cases

The goal is not a large test count.  The goal is a small number of canonical
paths with high-information tests.  Before adding a regression test, first
remove the duplicate responsibility that made the defect possible.

1. **One command pipeline.** A ribbon button, menu item, context menu, and
   shortcut delegate to one command object.  That command resolves selection,
   validates preconditions, mutates the model, posts Undo, and requests view
   refresh in one place.  Do not copy that sequence into each UI entry point.
2. **One selection snapshot.** Capture a typed, stable selection once at the
   start of a command.  Both enablement and execution use this resolver; a
   ribbon focus transfer may not choose another selection source.
3. **One observable result.** Commands return or publish a structured result
   such as `changed`, `rejected(reason)`, or `failed(error)`.  Diagnostics,
   UI feedback, and tests consume the same result rather than reimplementing
   guesses about success.
4. **Test invariants, not clicks in isolation.** Use a compact fixture matrix
   that covers a root task, parent, leaf, first/last sibling, and two selected
   tasks.  Reuse it for indent/outdent, expand/collapse, link/unlink,
   hide/show, Undo/Redo, and save/reload.  Add a new fixture only when it
   exercises a new state transition.
5. **One full Robot journey per command family.** A Robot test proves the
   physical route.  It must not be duplicated for every visual variant when a
   headless contract test already covers the same state machine.  Visual tests
   share a dialog/ribbon harness that iterates locale and scale settings.
6. **Delete redundant tests after consolidation.** If two tests prove the
   identical invariant through duplicated implementations, retain the clearer
   one and remove the other.  Test code is production code and follows the
   same no-duplication rule.

The review question is therefore: *which duplicated path or missing invariant
allowed this bug?*  “Add another case” is not an adequate answer by itself.

## Interaction-specific rules

### Selection-dependent commands

- Resolve selection once from the active `DocumentFrame`/spreadsheet at command
  execution and use that same resolved selection for enablement, action, undo,
  and diagnostics.
- Never make an enabled command silently no-op because focus moved to a ribbon
  button.  Preserve task selection across focus transfer.
- Commands that require N selected tasks (for example Link) must be disabled
  when fewer than N valid task nodes are selected, or give an explicit message.

### Hierarchy, dependency, and visibility commands

- Indent, outdent, move, expand, collapse, link, unlink, hide, and show must
  assert the outline/dependency/hidden state before and after the command.
- Cover root, first/last sibling, parent with children, leaf, multiple rows,
  mixed task/resource selection, read-only, and collaboration-lock cases.
- Test keyboard input through the actual focused component.  A shortcut must
  have one owner only; the root pane owns global shortcuts and a name editor may
  own only its documented editing-specific keys.

### Dialogs and windows

- A dialog is successful only when its content, focus, close button, primary
  action, Cancel, and Escape behave correctly.  `JDialog.isVisible()` alone is
  insufficient.
- Use layout managers and preferred/minimum component sizes.  Do not use fixed
  row heights or DLU values smaller than a component's preferred height.
- A secondary document window must have the same command chrome, close policy,
  title convention, and keyboard routing as the primary window unless the
  difference is explicitly specified and tested.

### Undo and persistence

- A user-visible mutation posts exactly one undoable edit.  Do not clear or
  replace unrelated history.
- Verify Undo from the same physical shortcut route the user uses, then verify
  Redo.  For project mutations, save and reload the fixture.

## Diagnostics requirements

Debug mode must report semantic outcomes, not just dispatch:

```
UI_COMMAND id=<id> selection=<count/type> precondition=<pass/fail>
UI_COMMAND id=<id> modelBefore=<summary> modelAfter=<summary>
UI_COMMAND id=<id> viewBefore=<summary> viewAfter=<summary>
UI_COMMAND id=<id> undo=<posted/none> reason=<reason>
```

Any `precondition=fail`, unchanged expected model, unchanged expected view, or
thrown exception is a `UI_COMMAND_FAILURE`.  Logs must include the active view
and stable task IDs, never only object identity or a blank event source.

## Review and release gate

- No GUI bug may be closed without a regression test that fails before the fix
  and passes after it.
- The implementation plan must identify the canonical command pipeline and
  explain any consolidation made.  A fix that only patches the UI entry point
  where the bug was observed is rejected when another route has the same
  responsibility.
- Prefer a shared fixture/harness and invariant assertion over a new
  copy-pasted scenario.  A new test case needs a stated new state transition,
  boundary, or failure mode.
- A PR touching GUI behavior must list its command contract, fixtures, exact
  test commands, and Robot evidence in its description.
- Run the focused unit tests and focused `guiTest` before review.  Run the full
  `:micrproject_ui:test` and `:micrproject_ui:guiTest --max-workers=1` before a
  release or when shared command, selection, layout, or shortcut code changes.
- Test failures, skipped desktop tests, visual regressions, or missing command
  contracts block release.  A waiver requires a linked open issue, a named
  owner, scope, and expiry; it cannot be used for a known data-loss, command,
  or layout defect.

## Regression matrix

The mandatory matrix is maintained in `TEST_PLAN.md` as U-18 through U-24.
Every newly reported GUI defect is assigned a row or added to an existing row
before implementation begins.
