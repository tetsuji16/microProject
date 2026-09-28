# Application use-case boundaries

Issue #740 tracks moving project workflows behind the application layer while
keeping Swing interaction and presentation in the UI module. This inventory is
based on current main-source references and Gradle dependencies; it is a
migration map, not a claim that the boundaries are already complete.

## Current UI references to integration modules

| UI caller | Current dependency | Responsibility | Boundary decision |
| --- | --- | --- | --- |
| `GraphicManager`, `DocumentFrame` | `ProjectArtifactLifecycleCoordinator` | Own the process temporary workspace, configure MPOF extraction root, and close extracted child resources on document close, manager cleanup, or project removal | The UI now calls a format-keyed application coordinator. Core owns only the lifecycle port contract; application owns the `TemporaryWorkspace`; exchange owns the adapter and archive registry. |
| `GraphicManager`, `ResourceMappingDialogCoordinator`, `ResourceMappingDialog` | `ResourceMappingForm` | Supply import mapping choices and receive the user's mapping decision | UI contract passed into exchange. The Swing dialog remains UI-owned; separate its presentation type from exchange only if the resulting contract remains UI-agnostic and does not duplicate mapping state. |
| `DocumentFrame` | reflective `ReportView` creation | Select and host a report view | View integration. Keep view selection and Swing hosting in UI; do not put `JPanel` or `DocumentFrame` in application. |
| `ReportView` | `DataSource`, `DataSourceProvider`, `ReportUtil`, `ReportViewer` | Build report data and render Jasper output | Mixed responsibility. The view and viewer are presentation; report definition selection/data preparation and report generation belong behind a report use case or report port. Preserve interactive column selection in UI. |

`modules/microproject_ui/build.gradle.kts` currently has direct dependencies on
`microproject_exchange` and `microproject_reports`. The references above are the
known source-level justification; each dependency can be removed only after its
callers are migrated or the remaining presentation API is placed in a smaller
boundary module.

## Existing application and port work

- `ProjectDocumentWorkflow` and `ProjectLoadWorkflow` already coordinate parts
  of save and load. Extend these instead of adding another UI-facing workflow.
- `ProjectPortCoordinator` delegates typed import/export requests through
  `PortRegistry`; `GraphicManager.saveLinkedSubproject` now uses its MPO export
  operation. Other open/save/import routes still use legacy job-based factories.
- `ProjectArtifactLifecycleCoordinator` routes workspace-root setup and project
  artifact cleanup through a format-scoped lifecycle port and owns the process
  `TemporaryWorkspace`. `GraphicManager` and `DocumentFrame` no longer own the
  workspace field or reference MPO importer/extraction-registry types.
- `DefaultFileImporterProvider` registers both legacy `SessionImporter` job
  factories and typed `ImportPort`/`ExportPort` adapters. These APIs serve
  different call contracts today. Migration must preserve progress, merge and
  resource-mapping callbacks, recovery behavior, atomic writes, and existing
  file-format compatibility before removing either registration path.
- `LocalSession` owns provider discovery and both registries. Provider
  registration may remain infrastructure/bootstrap; user workflow selection
  and result handling should move to application orchestration.

## Migration order

1. Trace each open/save/import/export/report caller through the UI event,
   importer or report adapter, persistence/output, and reload/display path.
2. Define application-owned request/result contracts for the workflow while
   leaving dialogs, view creation, and EDT scheduling in UI.
3. Migrate one complete user route at a time and test the route's format,
   diagnostics/cancellation, and save/reload or output behavior.
4. Consolidate or retire legacy and typed paths only after call-site search
   confirms equivalent behavior and the migrated route covers their contract.
5. Narrow Gradle dependencies and add architecture checks for the established
   boundary; avoid rules that reject intentional view/presentation references.

Do not combine this work with #737's removal of Swing/presentation concerns
from core or #738's `GraphicManager` lifecycle decomposition. Consume those
changes through their resulting APIs.
