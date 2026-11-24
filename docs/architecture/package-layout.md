# Package layout

Production Java packages use the `com.microproject` prefix. Modules define the
build and dependency boundaries; package names describe the responsibility
inside each module. The lists below cover the first two package levels present
in production sources, including packages shared at a boundary.

## microproject_core

- `algorithm`, `algorithm.buffer`: algorithms and calculated-value buffers.
- `association`: association utilities.
- `collaboration`: local collaboration state and operations.
- `command`: shared command contracts.
- `configuration`: application configuration and definitions.
- `core.configuration`, `core.dictionary`, `core.fields`, `core.id`, `core.nodes`, `core.time`, `core.util`: retained legacy configuration and utility model; their further migration is outside #1316.
- `datatype`: domain value types.
- `document`: document state and lifecycle.
- `field`: field metadata and access.
- `formula`, `formula.builtin`: formula infrastructure and built-in formulas.
- `i18n`: localized client messages.
- `interval`: interval data and operations.
- `job`: background job coordination.
- `model.assignment`, `model.availability`, `model.calendar`, `model.costing`, `model.dependency`, `model.key`, `model.resource`, `model.snapshot`, `model.task`, `model.time`: project model and its value types.
- `node`, `node.event`, `node.hierarchy`, `node.model`, `node.summaries`, `node.transform`: outline nodes, events, hierarchy, models, summaries and view transformations.
- `options`: project and application options.
- `persistence`, `persistence.access`, `persistence.data`: file interfaces, legacy class relocation, access metadata and serialized data types.
- `port`: module-facing service interfaces.
- `preference`: user preference state.
- `print`: shared print configuration.
- `schedule`, `schedule.ccpm`, `schedule.criticalpath`, `schedule.pert`: scheduling, critical chain, critical path and PERT calculation.
- `script`, `script.object`: script integration and exposed objects.
- `session`: local session and import options.
- `timescale`: shared timescale model.
- `transaction`: transaction coordination.
- `undo`: shared undo contracts and edits.
- `user`: user and account model.
- `util`, `util.temporary`: shared utilities and temporary-file support.
- `view.configuration`, `view.network`: view configuration and network position state.
- `workspace`: workspace state.

## Other modules

- `microproject_application`: `application` coordinates document open/save and application workflows.
- `microproject_bootstrap`: `bootstrap` starts and updates the desktop application.
- `microproject_contrib`: `contrib`, `contrib.calendar` contain bundled compatibility code.
- `microproject_exchange`: `exchange`, `exchange.converter`, `exchange.mpx`, `exchange.mpxj`, `exchange.serialize`, `exchange.xlsx` implement external formats, serialization and related conversions.
- `microproject_reports`: `reports.adapter`, `reports.model`, `reports.render` provide report adapters, models and rendering.
- `microproject_ribbon`: `ribbon` contains ribbon models, commands and theme abstractions.
- `microproject_ui`: `ui.app`, `ui.command`, `ui.diagnostics`, `ui.dialog`, `ui.export`, `ui.frame`, `ui.help`, `ui.input`, `ui.menu`, `ui.print`, `ui.privacy`, `ui.report`, `ui.ribbon`, `ui.shell`, `ui.theme`, `ui.toolbar`, `ui.util`, `ui.view` contain the Swing application and views.

## Renamed packages

| Earlier package under `com.microproject` | Current package |
|---|---|
| `pm.graphic.frames`, `pm.graphic.laf`, other `pm.graphic` | `ui.frame`, `ui.theme`, `ui.view` |
| `dialog`, `menu`, `toolbar`, `print`, `export`, `help`, `main`, `util` in the UI module | corresponding packages under `ui` (`main` is `ui.app`) |
| `offline_graphics`, `graphic.configuration.cellstyles`, `reports.view` in the UI module | `ui.export.svg`, `ui.view.style`, `ui.report` |
| `core.pm.exchange.converters.mpx`, `core.pm.exchange.converters.type` | `exchange.mpx.converter`, `exchange.converter.type` |
| exchange `server.data`; core `server.data`, `server.access` | `exchange.serialize`; `persistence.data`, `persistence.access` |
| `strings`, `company`, `temporary`, `init` | `i18n`, `user`, `util.temporary`, `configuration` |
| `scripting`, `scripting.formulas` | `formula`, `formula.builtin` |
| core `exchange`, `graphic.configuration` | `persistence`, `view.configuration` |
| `grouping.core` | `node` |
| `pm.scheduling`, `pm.criticalpath`, `pm.ccpm` | `schedule`, `schedule.criticalpath`, `schedule.ccpm` |
| remaining `pm` model packages | corresponding packages under `model` |

`ClassRelocations` keeps explicit historical binary class names for supported
MPO/POD reads. These compatibility strings are not current package declarations.
