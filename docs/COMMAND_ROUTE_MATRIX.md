# Command route matrix

`CommandRouteMatrixTest` is the executable matrix for the commands that have
multiple presentation routes. Each Ribbon, application-menu, popup, and
root-pane shortcut action must resolve its legacy action id through
`CommandId`, then delegate to `GraphicManager.dispatchTaskCommand`, and finally
to `DocumentFrame.routeTaskCommand`. The frame route is the only place where
selection validation, mutation, undo, and the `RibbonCommandResult` outcome are
decided.

| CommandId | Legacy action id | Ribbon | Menu | Popup | Shortcut |
|---|---|---:|---:|---:|---:|
| INSERT | InsertTask | ✓ | ✓ | ✓ | ✓ |
| DELETE | Delete | ✓ | ✓ | ✓ | ✓ |
| CUT | Cut | ✓ | ✓ | ✓ | ✓ |
| COPY | Copy | ✓ | ✓ | ✓ | ✓ |
| PASTE | Paste | ✓ | ✓ | ✓ | ✓ |
| PASTE_INSERT | PasteInsert | ✓ | ✓ | ✓ | ✓ |
| LINK | Link | ✓ | ✓ | ✓ | ✓ |
| UNLINK | Unlink | ✓ | ✓ | ✓ | ✓ |
| INDENT | Indent | ✓ | ✓ | ✓ | ✓ |
| OUTDENT | Outdent | ✓ | ✓ | ✓ | ✓ |
| EXPAND | Expand | ✓ | ✓ | ✓ | ✓ |
| COLLAPSE | Collapse | ✓ | ✓ | ✓ | ✓ |

The matrix test verifies the stable identity and canonical frame delegation.
GUI acceptance tests provide the physical representative evidence for the
Ribbon, popup, and root-pane keyboard families; unsupported commands must return
`REJECTED` or `NO_CHANGE`, never a false success.
