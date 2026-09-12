# MSP standard command inventory (#453 / #553)

The inventory distinguishes implemented commands from conditional or
unsupported MSP features.  Unsupported features are intentionally not bound
to a successful no-op.

| MSP area | microProject status | Evidence / boundary |
|---|---|---|
| File, Task, Resource, Report, Project, View, Format standard tabs | SUPPORTED | `RibbonStructureTest`, `RibbonCommandCatalogTest` |
| Task insert/delete/copy/cut/paste, outline, links | SUPPORTED | `CommandRouteMatrixTest`, `TaskInformationRibbonGuiAcceptanceTest`, `RibbonTabGuiAcceptanceTest` |
| Read-only/selection enablement and one canonical action route | SUPPORTED | `RibbonButtonBehaviorTest`, `MicrosoftShortcutsRootPaneTest`, paste/column permission tests |
| Save/Undo/Redo quick access | SUPPORTED | `RibbonStructureTest.quickAccessContainsOnlyDocumentWideCommands` |
| Resource pool and level resources | CONDITIONAL | Visible/enabled only for supported resource context; catalog and route tests |
| CCPM analysis/monitoring | CONDITIONAL (microProject extension) | Explicit Project/Report commands; kept separate from MSP standard semantics |
| Project Online/Planner linking, Visual Basic/Macros, external Excel/Visio visual reports | NOT_SUPPORTED | No Ribbon binding; no successful no-op; future work requires a typed route and acceptance row |
| MSP Help/Feedback/account services | NOT_SUPPORTED or microProject replacement | Local documentation/about routes only; no Microsoft account impersonation |
| Timeline/advanced object-specific context commands not implemented by the current view | NOT_SUPPORTED | Contextual tab must not advertise an unavailable command |

The executable catalog is the source of truth for what the current Ribbon can
advertise.  `RibbonCommandCatalogTest` rejects unknown or duplicate buttons and
requires English/Japanese metadata and a documented GUI use case for every
advertised command.  This prevents an unsupported MSP feature from silently
appearing as a successful command.

## Official baseline

* [Learn the Project 2010 ribbon](https://support.microsoft.com/en-us/office/learn-the-project-2010-ribbon-5038d333-8646-4c46-a0df-9be0ab380d8a)
* [Create and work with subtasks and summary tasks](https://support.microsoft.com/en-us/project/create-and-work-with-subtasks-and-summary-tasks-in-project-desktop)
* [Overview of Project views](https://support.microsoft.com/en-gb/office/overview-of-project-views-6cb1dbcd-5cd5-4cc2-a878-aa365564266d)
