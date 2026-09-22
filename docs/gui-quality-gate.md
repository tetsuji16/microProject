# GUI Quality Gate

## Purpose

microProject GUI work is complete only when a real user operation produces the
intended persistent model change and the intended visible result.  A dispatched
Swing `Action`, a returned method call, or the absence of an exception is not
evidence of success.

This gate applies to every change under `microproject_ui`, and to any other
change that changes a GUI-observable command, model projection, persistence
path, or keyboard shortcut.

An unexpected error dialog, error screen, uncaught EDT/AWT exception, or
stderr stack trace is a failed GUI result even when the JUnit method reports
success.  Each visible error must first be classified as an expected,
user-facing validation outcome or an unexpected defect.  Expected errors are
asserted for their message, close route, model preservation, and recovery;
unexpected errors are fixed at their shared cause and are never hidden by a
catch, retry-only workaround, or log suppression.

## MSP compatibility evidence and closure rule

When a change claims Microsoft Project Desktop compatibility, the compatibility
oracle is the applicable Microsoft-published specification or support document,
not an unavailable local installation of Microsoft Project.  The issue or PR
must link the exact source and name the documented product/version scope.  A
behavior not covered by that source is an implementation choice, not proof of
MSP compatibility.

A compatibility defect may be closed only when all of the following are true:

1. the documented MSP requirement is recorded together with its source;
2. the command contract identifies every supported physical route and the
   canonical implementation they share;
3. a pre-fix regression test and the required headless/Robot evidence pass;
4. mutation, Undo/Redo, and persistence evidence are present when the command
   changes project data; and
5. invalid states produce an observable disabled/rejected outcome, not a silent
   no-op.

If any condition is missing, leave the issue open with the missing evidence and
scope stated explicitly.  Direct execution against Microsoft Project is useful
supplementary evidence when available, but is never required to apply this rule.

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
| Failure | Invalid input, no selection, locked data, and unsupported views have a deterministic, observable disabled/rejected result; they may not silently return. |

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

The release gate uses the compact `-PguiTestSuite=smoke` matrix, which covers
each shared-cause family changed by the release.  The complete `full` suite is
reserved for scheduled/manual audits and broad UI changes; reducing the gate
does not permit removing a required invariant from the smoke classes.

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

### Context menus and column layouts

- A constructed `JPopupMenu` or `JMenuItem.doClick()` is route-integration
  evidence only. A reported context-menu command also requires Robot
  right-click on the actual header or row, physical menu-item selection, and
  an assertion of the resulting model and visible state.
- Column Insert and Hide share the persistent layout mutation and must share a
  compact headless Undo/Redo fixture. The physical header-popup journey proves
  the header view-coordinate to field-array-coordinate conversion. Custom
  rename, preset selection, and AutoFilter are separate state transitions; add
  one case each only when their dialog/filter behavior changes.
- Persisted column layouts require save/reload evidence. Row popup mutations
  reuse their command-family fixture; do not duplicate the same hierarchy or
  dependency journey once per popup item.

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

### Visual issue intake and historical regression closure

For every GUI issue with screenshots, treat the report as a set of visual
contracts, not as one representative example:

1. Enumerate every image in the issue body and every comment/reply. Open each
image and record its screen, affected controls, symptom, and source link in a
coverage matrix before changing code. Text extracted from the issue is not a
substitute for inspecting the pixels.
2. Search prior open, closed, and reopened issues plus their comments for the
same screen, component, layout helper, and symptom. Read the prior fix and its
test scope; a past closure is history, not evidence that the current paths are
covered.
3. Map every image to its owning shared layout/component rule and to at least
one regression assertion. Keep the issue open in the work record until every
image has a matching test or a documented reason it is unaffected.
4. Dialog visual checks must verify text-bearing labels, buttons, fields, and
combos have at least their font-derived preferred height, remain inside the
owning panel/window, and do not overlap sibling content. “Dialog opened” and
“component is visible” are not clipping checks.
5. When a common layout helper or dialog base class changes, rerun the
representative dialog family, including old screens implicated by historical
reports. Add a shared invariant assertion rather than isolated per-label pixel
offsets.

Issue #590 is a regression example: the issue body has four screenshots and
its replies add two more. All six must be represented. Earlier issue #460 had
already broadened the known scope from calendar dialogs to Help, Locale,
Project Information, Clear Baseline, and Task Information; closing from a
narrow subset failed to preserve that broader coverage. The follow-up reopened
for an empty Project Information dialog because its `p,3dlu,p`/`nextLine(2)`
pattern escaped the earlier checks. The countermeasure is image-to-test
traceability plus shared preferred-height assertions, not another one-off
layout workaround.

### Undo and persistence

- A user-visible mutation posts exactly one undoable edit.  Do not clear or
  replace unrelated history.
- Verify Undo from the same physical shortcut route the user uses, then verify
  Redo.  For project mutations, save and reload the fixture.

### Spreadsheet input transactions

An editable spreadsheet cell is an input transaction, not a single key event.
For every change to cell editing, editor activation, input-method handling,
value conversion, or displayed scheduling field, use the U-26 fixture from
`TEST_PLAN.md`. It is mandatory to cover all of these boundaries together:

1. physical multi-character ASCII input into numeric and date fields;
2. an input burst arriving before the editor receives focus;
3. IME composition and Convert/reconversion with pre-existing Japanese text;
4. conversion from editor text to the domain value and the rendered cell text;
5. a structural command followed by save/reload, so UI input cannot create a
   persistence-only failure.

Do not substitute `setText`, a parser-only test, or a single-key test for the
physical multi-character route. A test is incomplete if it asserts only editor
text: it must also assert the committed domain value and, where applicable,
the renderer's text. Split the fixture into headless event-sequence coverage
and one compact Robot journey; do not create one copy-pasted Robot case per
field.

#### Legacy-test classification

Existing tests that use `editor.setText(...)` or invoke an Action directly are
valuable conversion or route-integration tests, but they are **not** physical
input acceptance tests. Keep them when they prove a distinct parser, commit, or
failure contract; label their role in the test plan and add the missing Robot
journey to the shared input fixture. Do not rename or count an injected-text
test as evidence for keyboard, IME, focus-transfer, or rendering behavior.

During an audit, classify each old GUI test as one of: domain contract, route
integration, physical acceptance, visual layout, or obsolete duplicate. Remove
only an obsolete duplicate after a canonical test covers the same state
transition. Strengthen the others at their missing boundary rather than
rewriting a stable unit test into a timing-sensitive GUI test.

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
  test commands, Robot evidence, and (for MSP claims) the official-source URL
  and documented product/version scope in its description.
- A GUI compatibility claim cannot be closed from a passing action dispatch,
  a screenshot, or a comparison with an undocumented observed behavior.  The
  MSP-source evidence and the complete command contract above are mandatory.
- Run the focused unit tests and focused `guiTest` before review.  Run the full
  `:microproject_ui:test` and `:microproject_ui:guiTest --max-workers=1` before a
  release or when shared command, selection, layout, or shortcut code changes.
- Test failures, skipped desktop tests, visual regressions, or missing command
  contracts block release.  A waiver requires a linked open issue, a named
  owner, scope, and expiry; it cannot be used for a known data-loss, command,
  or layout defect.

## Enforcement gaps that are themselves release defects

The rules above are not evidence merely because they are written in this
document.  The release workflow and the test harness must mechanically enforce
them.  In particular:

- JUnit's method timeout is not an EDT or native-window watchdog.  A test that
  blocks in `SwingUtilities.invokeAndWait`, a modal loop, a font/layout pass,
  or a non-daemon AWT thread can leave the Gradle worker alive after the test
  timeout.  The release gate must therefore have an outer process-level
  timeout, capture a thread dump and window/process inventory, and fail the
  job if the child process does not exit.  A timeout that only prints a JUnit
  failure is insufficient.
- `forkEvery=1` isolates test classes but does not prove that a class released
  every Window, native focus grab, timer, executor, or AWT helper.  The shared
  GUI extension must assert zero unexpected windows and no surviving test-owned
  non-daemon helpers at teardown; the CI watchdog remains mandatory.
- The compact release `smoke` gate is not the complete U-21 visual matrix.
  It must still run the representative functional journey at the canonical
  desktop scale.  The `full` audit additionally runs the reusable
  visual/targeted-command suite for Japanese and English at 100%, 125%, and
  150%.  A requested full-audit leg that is missing is a gate failure, not a
  pass.  A mutually exclusive scale Assumption is acceptable only when the
  same case is executed and passes in its designated alternate scale leg;
  unexpected or unexplained skips remain failures.  The Windows workflow
  deletes stale GUI JUnit XML before each gate invocation, parses the fresh
  XML after the process exits, and allows only the two explicitly paired
  high-DPI assumption messages; all other skipped cases fail the release.
- Historical entries in `TEST_PLAN.md` describe evidence at a particular
  revision only.  They must not satisfy the current gate unless the current
  commit, install layout, locale, scale, fixture, test command, and result are
  recorded.  A later failing run supersedes an older `BUILD SUCCESSFUL` entry.
- A Robot test that retries because the first click was consumed by Windows
  foreground activation must record the retry as diagnostic evidence.  It may
  stabilize the harness, but it cannot turn an unresolved active/focused or
  command-registration failure into a pass by omitting the semantic assertion.
- The gate must run against the freshly generated `installDist` and, for a
  release, the packaged executable.  A compile or headless test result cannot
  substitute for either launch path.

## Regression matrix

The mandatory matrix is maintained in `TEST_PLAN.md`. Every newly reported GUI defect is assigned a row or added to an existing row before implementation begins.

## Active environmental waiver

`W-464-DPI-FULLWIDTH` is the only active desktop-test waiver.  It is linked to
open issue [#464](https://github.com/tetsuji16/ProjectLibre/issues/464), owned by
`tetsuji16`, and expires on 2026-10-31.  Its scope is limited to the direct
physical all-command ribbon sweep and the full-width Hide/Show mutation journey
when `guiTestUiScale > 1.0`; the fixed desktop runner cannot place every scaled
control inside one screen.  The 100% Robot journeys remain mandatory, and the
125/150% runs must still pass the dedicated visual, targeted-command, and
model/Undo checks.  This waiver does not cover any functional, persistence, or
layout failure.
