# Architecture decisions for compatibility-sensitive issues

This document records the caller audit for the non-clean-room consolidation
issues. It is part of the mitigation: a future change must not merge classes
merely because their short names match.

## #212 — configuration engines

`com.microproject.configuration.Configuration` is the active Digester engine
used by the application field, graphics, calendar, and script configuration.
`com.microproject.core.configuration.Configuration` is used only by the legacy
JAXB `core.fields`/`core.nodes` family and owns a different dictionary type.
The legacy classes are deprecated and documented as a compatibility boundary.
The three active callers now enter through the explicit
`com.microproject.core.configuration.LegacyConfiguration` facade, which keeps
the old JAXB dictionary and serialization behavior while making accidental
imports of the application engine visible in code review. Migration still
requires validating JAXB data round-trips before deletion; it is not a safe
alias or package merge.

## #257 — time types

`com.microproject.datatype.Duration`/`Rate` are the scheduling-domain value
types. The earlier MPX compatibility decision treated the `core.time` converter
helpers as active, but caller and resource searches found `MpxRateConverter`,
`MpxDurationConverter`, and `MpxTimephasedConverter` had no runtime entry point;
they and the unused importer reference were removed. Current production exchange
code uses the `datatype` types through `MPXConverter`. The deprecated
`core.time.Duration`, `Rate`, and `TimeUnit` and `TimeTypeBridge` remain as
source-compatibility APIs; deleting or changing them needs an explicit public API
compatibility decision. Other same-named classes still require their own caller
and serialization-boundary audit.

## #152 — same-name type consolidation

The names in this issue are not sufficient evidence that the implementations
can be merged. Current caller and boundary checks distinguish these pairs:

- `com.microproject.core.fields.Field` is JAXB metadata used by the legacy
  `core.fields`/`core.nodes` configuration family. `com.microproject.field.Field`
  is the runtime field descriptor used by the active application configuration,
  spreadsheet, undo, exchange, and reports paths. They do not represent the
  same responsibility; merging them would cross the JAXB configuration and
  runtime field-model boundary.
- `com.microproject.core.nodes.Node` is an ID-bearing legacy configuration
  node contract. `com.microproject.grouping.core.Node` is the Swing tree node
  contract used by the active task/resource outline and grouped-view pipeline.
  They have different parents, lifecycle, and callers; retain both until the
  legacy JAXB subsystem has an explicit migration and round-trip decision.
- The MPX importer checks `mpxTask.getSubProject()` while converting an MPXJ
  task into the internal `SubProj` model contract. Later internal consumers use
  `Task.isSubproject()`, which tests that model contract. These are source-format
  decoding and domain-model classification respectively, not duplicate
  subproject predicates. Keep the MPXJ-specific check in the exchange adapter.
- `com.microproject.configuration.Dictionary` indexes named application/view
  configuration items loaded through Digester. `com.microproject.core.dictionary.Dictionary`
  indexes `HasStringId` values by JAXB-era category and is retained by the
  legacy configuration facade. Their lookup keys, population lifecycle, and
  consumers differ; preserve both until the JAXB configuration boundary is
  migrated.
- `com.microproject.core.nodes.HasId` uses `NodeId` for outline graph identity;
  `com.microproject.pm.key.HasId` uses `long` for project task/resource identity.
  The APIs are not substitutable and are consumed by different model layers.
- `com.microproject.core.time.TimeInterval` and its `DefaultTimeIntervals`
  collection represent mutable data intervals with union/intersection
  operations. `com.microproject.timescale.TimeInterval` is an immutable display
  slot containing label text and two ranges. Similarly,
  `core.time.TimeIntervals` is an interval-set API, while
  `script.object.TimeIntervals` stores the time-scale viewport windows,
  history, and zoom/translation state. Keep the owning feature APIs separate.
- `com.microproject.main.Main` is the packaged desktop entry point: it configures
  runtime logging and normalizes command-line file arguments before dispatching
  to `pm.graphic.gantt.Main`, which initializes locale and creates the actual
  application frame. Both are part of one startup pipeline, not competing main
  implementations.
- `com.microproject.util.ClassLoaderUtils` is the core API used by core, UI, and
  reports. The duplicate `com.microproject.contrib.ClassLoaderUtils` had no
  application, configuration, or reflective callers; its only remaining
  references were its own definition and tests. It has been removed, and its
  Java-version comparison tests now exercise the canonical core class. This
  avoids a core-to-contrib delegation cycle and leaves one class-loading helper.

The remaining consolidation inventory must be decided pair by pair using
active callers and serialization/configuration boundaries. Do not remove a
deprecated public type solely because in-repository production callers have
migrated; document and resolve the source-compatibility policy first.

## #258 — hierarchy types

`com.microproject.core.hierarchy` models the legacy document hierarchy;
`com.microproject.grouping.core.hierarchy` models filtered/grouped view
hierarchies and mutable transformations. Their node contracts and lifecycle
are different. The active view pipeline uses the grouping hierarchy, while
legacy document code still uses the core hierarchy. No classes are deleted;
new code must import the hierarchy matching its layer.

## #245 — persisted choice types

Gantt interval dragging now uses ConstraintType.Kind for its target and internal comparison. Its Undo edit remains an integer-code compatibility boundary and stores the original raw code, preserving restoration of unknown legacy values.

The domain choice classes in `pm/` now expose nested `Kind` enums with explicit
persisted integer codes and strict `fromCode` validation. Their old integer
fields remain deprecated aliases so `.pod` and MPX readers continue to
accept legacy values. Integer constants that are event flags, bitmasks, array
indexes, or calculation sentinels are not enums and remain integer APIs.

`SchedulingType` now routes scheduling-rule selection through its `Kind`
overload, and task/assignment calculations compare typed values after decoding
the persisted task snapshot code. The int overload remains as a deprecated
compatibility adapter; its code mapping is covered alongside the enum API by
`SchedulingTypeTest`.

`RequestDemandType.Kind` now also crosses the `HasRequestDemandType` and
`AssignmentEntry` domain APIs. The public integer accessors remain deprecated
compatibility adapters, while assignment storage remains an `int` so existing
serialization and exchange readers keep their current field shape. Known codes
are round-tripped through `Kind`; unknown legacy values remain readable and
unchanged through the integer API, and typed conversion rejects them instead
of coercing them to a known value.

`HasExpenseType` now deprecates its integer accessors in favor of `ExpenseType.Kind`,
including a typed effective value. `Task` and `Project` keep the original integer
fields and accessors for persisted/exchange compatibility. Task inheritance has
one code resolver shared by the typed and integer views: the integer adapter
continues to return an unknown stored code unchanged, while the typed view
rejects a code outside the enum. ProjectDialog's configuration-backed combo
and sequential option indexes remain integer boundaries; its `Form` maps those
values to `Kind`, and project creation calls typed setters. A native POD
round-trip test confirms project type, status, and expense codes remain stable.

`ProjectSpecificFields` now exposes typed `ProjectType.Kind` and
`ProjectStatus.Kind` getters/setters. `Project` retains its integer fields for
serialization and old callers; defaults are sourced from each enum's stable
code. Unknown legacy values remain unchanged through the integer API and are
rejected by typed access. ProjectDialog maps its option indexes through `Kind`
before passing values to project creation; exchange DTO values remain integer
boundaries. `ResourceImpl` now provides typed booking-type access while keeping
its existing int field and deprecated adapters. Resource booking type is not
part of the current POD V1 resource DTO (`Serializer` explicitly omits the
resource implementation fields), so it is not treated as a persisted value in
this migration.

Contour codes now have `ContourTypes.Kind`, with MSPDI numeric codes retained
as the compatibility representation. Contour creation, assignment access, and
`AssignmentDetail` custom serialization use typed kinds internally; serialized
work/cost contour fields remain integers. Personal contour code 8 remains a
distinct kind and continues to serialize with its bucket data.

`Task` now exposes `EarnedValueMethodType.Kind` accessors while its stored task
field and exchange-facing integer accessors retain the same codes. The POD
round-trip regression confirms the physical-percent-complete choice survives
save and reload.

`Resource` now provides typed resource-kind accessors and a typed predicate;
`EnterpriseResource` uses kind predicates for work/material/cost decisions.
Integer resource fields remain the serialization and configuration-map
boundary. Unknown integer codes still classify as no known kind through the
predicates, while strict typed decoding rejects them. Native POD coverage
confirms a material resource retains its kind after reload.

Assignment accrual calculations now compare `Accrual.Kind` values rather than
integer codes. Persisted accrual codes stay integers on the resource model. A
nullable decoder handles unknown legacy codes in calculation paths so their
historic fallback remains end-accrual and non-prorated; strict `fromCode`
continues to reject unknown values at typed validation boundaries.

`Task` now exposes `ConstraintType.Kind` accessors and a typed scheduling
constraint overload. Constraint-date routing and date-editability checks use
typed kinds internally, with a nullable decoder retaining legacy behavior for
unknown stored values; the integer field remains the serialization boundary.
`ScheduleService` now accepts the typed kind, and Task Sheet / Team Planner
calls use it; the integer overload remains for import and legacy callers.
AssignmentService now selects fixed-duration scheduling through `SchedulingType.Kind`; NormalTask encodes that kind only at its existing integer-backed task snapshot boundary.
`Task` and `NormalTask` scheduling predicates use typed kinds for reverse
scheduling, date anchoring, and critical-task classification. The persisted
task field remains integer-backed; unknown codes still match none of the known
predicates.
Schedule diagnostics and critical-path sentinel updates now also consume typed
constraints. `SchedulingAlgorithm` keeps its integer method for source
compatibility and provides a typed default view; the project task default is
exposed internally as a `Kind` only.

Dependency creation, task dependency decisions, diagnostics, and graph link
routing now use `DependencyType.Kind`. Integer dependency fields, constructors,
accessors, formatted spreadsheet values, and route overloads remain compatibility
boundaries. The integer service adapter decodes the code before mutating so an
unknown type is rejected without creating a partially valid link.

Timesheet status decisions and display helpers use `TimesheetStatus.Kind`.
AssignmentData and legacy getters/setters retain their integer codes. Nullable
decoding keeps unknown statuses available through the raw API and preserves the
existing fallback display; aggregate integer queries retain their previous
unknown-code behavior.

Access-control policy callers use `AccessControlPolicy.Kind`, with `ProjectSpecificFields` adapting to the existing integer project field. `ProjectData` and serialized project storage remain code-based, and unknown stored values continue to be available through the integer API.

`ScheduleOption` now exposes its single scheduling-rule choice as
`SchedulingType.Kind`; the int getter/setter remain compatibility adapters and
the assignment collection initialization reads the typed choice. Project event
types also have `ProjectEvent.Kind`; Project emits typed kinds while the event's
integer API remains available to old listeners, including unknown raw values.
Assignment workflow flags now use `AssignmentWorkflowState.Kind` and EnumSet
conversion helpers at the domain and exchange DTO APIs; the persisted/wire mask
and legacy integer accessors remain unchanged so unknown raw bits are still
available through the integer path.
Critical-path task-reference boundaries use `TaskReference.Kind`; reverse-pass
traversal calls `opposite()` instead of negating the numeric code. The old type
getter remains a compatibility adapter, and the enum keeps the original -1/0/1
codes.

CriticalPath selects early/late schedules through `TaskSchedule.Kind`; the
opposite pass is represented by `Kind.opposite()` instead of negating an int.
Legacy integer constructors and `Task.getSchedule(int)` remain adapters, with
the latter preserving its historical fallback to the late schedule for unknown
codes. Assignment cost-rate selection uses `CostRateIndex.Kind`; the numeric
index in `AssignmentDetail` remains the POD compatibility representation.

## #260 — link routing

Gantt routing and Network routing share `LinkRouting` path primitives but have
different route signatures and geometry: Gantt supports dependency types,
vertical arrows, floors/ceilings, and quadratic curves; Network supports an
orthogonal intermediate coordinate and orientation. They remain separate
strategies under a common base, avoiding unsafe casts and preserving rendering
behavior. The common base now owns reusable orthogonal point generation for
Network routing; Gantt-specific dependency and curve handling remains in the
Gantt strategies.

## #261 — UI events

`GraphEvent`, `CacheEvent`, and `SelectionNodeEvent` are not interchangeable:
they carry different payloads and listener contracts (graph node lists, cache
insert/remove interval deltas, and selection current-node/category state).
They share `GraphicEvent` only. A common marker would not remove duplicated
responsibility and would make event dispatch less type-safe.

## Migration rule

Any future consolidation must add a compatibility adapter, update every caller,
add a save/reload regression test for `.pod` and `.mpo`, and only then remove
the deprecated implementation. Clean-room namespace work (#152) is excluded
from this document.

## #152 current duplicate-name audit (2026-09-27)

Grouped all production Java source files under `modules/**/src/main/java` by
simple class name. The remaining pairs are exactly `Dictionary`, `Duration`,
`Field`, `HasId`, `Main`, `Node`, `Rate`, `TimeInterval`, `TimeIntervals`, and
`TimeUnit`; `package-info` files are not classes and were excluded. Their
responsibilities and compatibility boundaries are classified above or under
the linked #257 decision. In particular, `core.time.Duration`, `Rate`, and
`TimeUnit` have no production callers outside their own compatibility bridge;
they remain deprecated source-compatibility types until an explicit public API
removal policy is adopted. The legacy JAXB `Field`, `Node`, and `Dictionary`
types retain their separate XML/configuration contracts.

Previously reported duplicate names `ClassLoaderUtils`, `Configuration`,
`ConfigurationFile`, `Finder`, and `Pert` are no longer duplicates in the
production tree: the redundant loader class was removed, the legacy
configuration types received explicit names, and `TaskFinder` / `PertChart`
now distinguish the project-local utilities from their separate interfaces
and domain API. The SubProj classification predicate is shared by
`Task.isSubproject`, `NodeModelUtil`, and removal snapshots; remaining
`instanceof SubProj` checks retrieve or operate on the reference and are not
parallel classification helpers.

This audit found no remaining same-name pair with interchangeable
responsibility that can be merged without crossing a compatibility or feature
boundary. Keep the documented legacy adapters and distinct domain/UI types;
track any future public API removal or JAXB migration separately.
