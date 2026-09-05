# MS Project shortcut-key conformance

This document defines the Microsoft Project keyboard-shortcut conformance target for
microProject (issue #47). The owner decision (issue comment) is: *"Shouldn't be like
that, just a lack of the implementation. Will implement all of the shortcuts supported
by ms project."*

The authoritative shortcut list is Microsoft's own reference:
<https://support.microsoft.com/en-us/accessibility/project/keyboard-shortcuts-for-project>

## Root cause (why keyboard copy/cut/paste/delete "didn't work")

The app is a **ribbon** UI, not a classic menu-bar UI. The clipboard and most other
actions are only reachable through `JMenuItem.setAccelerator(...)`, and Swing only
dispatches a `JMenuItem` accelerator when that menu item lives in a **visible
`JMenuBar`**. In the ribbon shell there is no active menu bar, so those accelerators
never fire. `Ctrl+C`/`Ctrl+X` additionally relied on a component-level
`NodeListTransferHandler` binding that is fragile (and overridden by the spreadsheet's
own `Ctrl+V`/`Ctrl+D` input-map entries), so copy/cut were unreliable while paste
sometimes worked.

The robust fix reuses the app's existing **root-pane** shortcut layer
(`GraphicManager.addCtrlAccel(...)`, which installs on the `JRootPane` input map with
`WHEN_IN_FOCUSED_WINDOW` and uses the platform menu-shortcut mask). That layer already
works in the ribbon UI for `Ctrl+F/G/L/Z/Y/N/O/S/P/I` etc. We extend it to cover the
remaining MS Project editing / outline / information shortcuts via a single extracted
seam, `GraphicManager.applyMicrosoftShortcuts(InputMap, ActionMap)`, which all callers
delegate to through `putCtrlAccel` / `putShortcut`.

## Conformance table

Implemented in `GraphicManager.applyMicrosoftShortcuts(InputMap, ActionMap)` (root-pane,
`WHEN_IN_FOCUSED_WINDOW`), which every caller reaches through `putCtrlAccel` / `putShortcut`.
"Maps to" names the existing handler/action that already exists in the codebase, so no new
command logic was needed — only global wiring. This is the **single** shortcut layer; the
old component-level `KeyListener` and duplicate `InputMap` registrations on `SpreadSheet`
were removed so each key resolves to exactly one action.

| MS Project key | MS behavior | microProject action constant | Notes |
|---|---|---|---|
| `Ctrl+X` | Cut selected data | `ACTION_CUT` | Was missing globally (ribbon could not dispatch). |
| `Ctrl+C` | Copy selected data | `ACTION_COPY` | Was missing globally. |
| `Ctrl+V` | Paste copied/cut data | `ACTION_PASTE` | Global root-pane route (NodeListTransferHandler). `Ctrl+Shift+V` pastes values only. |
| `Ctrl+D` | Fill down a column | `ACTION_FILL_DOWN` | Global route → active spreadsheet `fillDownSelection()`. |
| `Delete` | Delete selected data / row | `ACTION_DELETE` | Global root-pane route (replaces the old `SpreadSheet` `KeyListener`). MS Project: `Delete` clears the selected cell when a single cell is selected, or deletes the row when the row is selected — both route through the existing `Delete` handler on the active spreadsheet. |
| `Ctrl+Z` | Undo | `ACTION_UNDO` | Already wired. |
| `Ctrl+Y` | Redo | `ACTION_REDO` | Already wired. |
| `Ctrl+F` / `Shift+F5` / `F3` | Find | `ACTION_FIND` | `Ctrl+F` and `Shift+F5` already wired; `F3` added to match MS. |
| `F5` | Go To | `ACTION_GOTO` | Global route (was only `Ctrl+G`/`Ctrl+L`). |
| `Ctrl+G` / `Ctrl+L` | Go To | `ACTION_GOTO` | Already wired. |
| `Ctrl+F2` | Link tasks | `ACTION_LINK` | New. |
| `Ctrl+Shift+F2` | Unlink tasks | `ACTION_UNLINK` | New. |
| `Alt+Shift+Right` | Indent selected task | `ACTION_INDENT` | New MS-conformant binding (app also keeps `Ctrl+.`). |
| `Alt+Shift+Left` | Outdent selected task | `ACTION_OUTDENT` | New MS-conformant binding (app also keeps `Ctrl+,`). |
| `Alt+Shift+Up` | Move task up | `ACTION_MOVE_TASK_UP` | Already on spreadsheet + row header. |
| `Alt+Shift+Down` | Move task down | `ACTION_MOVE_TASK_DOWN` | Already on spreadsheet + row header. |
| `Alt+Shift+-` | Hide subtasks | `ACTION_COLLAPSE` | New MS-conformant binding (app also keeps `Ctrl+-` for row delete). |
| `Alt+Shift+=` | Show subtasks | `ACTION_EXPAND` | New MS-conformant binding (app also keeps `Ctrl++`/`Ctrl+=). |
| `Ctrl+Space` | Select the row | `SelectRow` on active spreadsheet | New MS-conformant binding. |
| `Shift+Space` | Select the column | `SelectColumn` on active spreadsheet | New MS-conformant binding. |
| `Ctrl+Shift+Space` | Select the entire sheet | `SelectAll` on active spreadsheet | New MS-conformant binding. |
| `Ctrl+-` (Minus) | Delete the selected row | `ACTION_DELETE` on active spreadsheet row | New MS-conformant binding (MS `Ctrl+Minus` deletes the selected row). |
| `Insert` | Add a new task | `ACTION_NEW` on active spreadsheet | New global route (was only on spreadsheet `KeyListener`). |
| `F2` | Activate entry bar / edit field | edit selected cell on active spreadsheet | New global route (was only on spreadsheet). |
| `Shift+F2` | Display task/resource/assignment information | `ACTION_INFORMATION` | New. |
| `Ctrl+Shift+K` | Show the CCPM remaining-buffer (fever) chart | `ACTION_CCPM_BUFFER_STATUS` | New. CCPM is not an MS Project feature, so this uses a chord that does NOT collide with MS Project's reserved keys (Ctrl+B=Bold, Ctrl+Shift+B=set work to 100%). |
| `Ctrl+Shift+N` | Show the CCPM network | `ACTION_CCPM_NETWORK` | New. Avoids Ctrl+Shift+B (MSP: set work to 100%). |
| `Ctrl+Alt+C` | Open CCPM settings | `ACTION_CCPM_SETTINGS` | New. |
| `Ctrl+Shift+J` | Toggle the critical-chain Gantt overlay | `ACTION_TOGGLE_CRITICAL_CHAIN` | New. Avoids Ctrl+T (MSP: task information detail) and Ctrl+Shift+P (MSP: task information). |
| `Ctrl+Shift+F` | Font dialog | `ACTION_FONT` | New. Matches MS Project's Ctrl+Shift+F (Font). |
| `Ctrl+Shift+P` | Task Information | `ACTION_INFORMATION` | New. Matches MS Project's Ctrl+Shift+P; also reachable via Shift+F2. |

## Spreadsheet edit-entry and shortcut precedence

The task spreadsheet adopts Excel-style direct cell entry.  microProject does not
have a separate Entry Bar, so an editable selected cell is edited in place.

1. A shortcut always wins over text entry.  `Tab`, `Shift+Tab`, `Ctrl+Arrow`,
   `Ctrl+Z`, `Ctrl+Y`, and every key with Ctrl, Alt, or Meta are dispatched through
   their shortcut binding; they must never open an editor or insert a character.
2. When the name field has focus, this rule also applies *while editing*: `Tab` and
   `Shift+Tab` indent/outdent; `Ctrl+Left`/`Ctrl+Right` collapse/expand; and
   `Ctrl+Up`/`Ctrl+Down` move to the first/last visible task row, matching MSP sheet
   navigation.  The active edit is finished before navigation.
3. In a non-editing editable cell, a printable character starts in-place editing and
   replaces the existing value with that character.  The first IME composition starts
   the same in-place edit and replaces the existing value with the composed text.
   This is intentionally Excel-style direct entry, not an emulation of Project's
   Entry Bar.
4. `F2` starts in-place editing with the caret at the end of the existing value.
   `Enter` commits a valid value and moves selection one row down.  Invalid input or
   active IME composition prevents that commit/move.

The editor owns the keys in item 2 through its `WHEN_FOCUSED` input map.  This keeps
the shortcut resolution deterministic even though the text editor, rather than the
table, owns focus during cell editing.

### Not yet wired (no equivalent action in microProject)

These MS Project shortcuts have no corresponding handler/action in microProject yet, so
they are intentionally left unbound rather than wired to a silent no-op:

- `Alt+Shift+*` (show all outline levels) — `ACTION_ALL_CHILDREN` exists as a constant but
  has no registered handler, so it is not bound.
- `Ctrl+Shift+M` (insert milestone), `Ctrl+PageUp/Down` (move between sheets/views),
  `Alt+Home`/`Alt+End` (timescale start/end), `Alt+PageUp/Down` (timescale page),
  `F8`/`Shift+F8` (extend selection mode), `F6`/`Shift+F6` (move between panes) — no
  microProject action or would duplicate native window behavior.

### Already conformant / out of scope

These MS shortcuts already worked through the existing root-pane layer or spreadsheet
bindings and were left unchanged:

- `Ctrl+N/O/S/P` (new/open/save/print), `Ctrl+K` (insert), `Ctrl+Period/Comma` (indent/outdent),
  `Ctrl+Plus/Minus` (expand/collapse), `Ctrl+R` (recalculate).
- Spreadsheet component bindings: `Ctrl+V` (paste values), `Shift+Ctrl+V` (paste insert),
  `Ctrl+D` (fill down), `Alt+Shift+Up/Down` (move task), `Tab`/`Shift+Tab` (name-column indent/outdent),
  `F2` (edit).
- Gantt component bindings: `Ctrl+Left/Right` (zoom), `Ctrl+Z/Y` (undo/redo).
- `Enter`/`Esc` form behavior, `F10`/`Alt` menu-bar activation, `Alt+F4` close window (native).

### Deliberately not changed

- MS Project Online-only shortcuts (Team Planner, Timeline, Network Diagram, side pane)
  are not applicable to microProject's current views and are left unimplemented.
- The existing `menu.properties` `.accelerator` declarations are kept (harmless in
  ribbon, still useful in the classic/new-look menu-bar shells); they remain the
  menu-button tooltips' source.

## Verification

- `SpreadSheetEditingBindingsTest` / `SpreadSheetHierarchyNavigationTest` continue to
  assert the spreadsheet/row-header component bindings.
- A new `MicrosoftShortcutsRootPaneTest` asserts that the root-pane input map of the
  document frame resolves the MS-conformant keystrokes (`Ctrl+X/C/V/D`, `Ctrl+F2`,
  `Ctrl+Shift+F2`, `Alt+Shift+Left/Right`, `Shift+F2`, `Insert`, `Delete`, `F5`)
  to the expected action constants. The test drives `applyMicrosoftShortcuts` directly
  on a windowless `JPanel` so it runs headless.
- Manual (UI): launch `installDist`, open a sample, confirm Cut/Copy/Paste/Delete,
  Fill Down, Link/Unlink, Indent/Outdent (Alt+Shift+arrows), Insert (Insert key),
  Information (Shift+F2), Go To (F5) all respond to the keyboard.
