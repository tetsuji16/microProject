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
types, while `com.microproject.core.time` remains required by the MPX converter
layer (`MpxDurationConverter`, `MpxRateConverter`, and time-phased converter
types). The two APIs have different contracts, including inheritance and unit
semantics. Converter adapters must be introduced before callers can move.
The legacy `core.time.Duration`, `Rate`, and `TimeUnit` are now explicitly
deprecated (without removal), so new callers cannot accidentally select them;
the MPX converter boundary remains the supported legacy consumer.

## #258 — hierarchy types

`com.microproject.core.hierarchy` models the legacy document hierarchy;
`com.microproject.grouping.core.hierarchy` models filtered/grouped view
hierarchies and mutable transformations. Their node contracts and lifecycle
are different. The active view pipeline uses the grouping hierarchy, while
legacy document code still uses the core hierarchy. No classes are deleted;
new code must import the hierarchy matching its layer.

## #245 — persisted choice types

The domain choice classes in `pm/` now expose nested `Kind` enums with explicit
persisted integer codes and strict `fromCode` validation. Their old integer
fields remain deprecated aliases so `.pod`, MPX, and PODX readers continue to
accept legacy values. Integer constants that are event flags, bitmasks, array
indexes, or calculation sentinels are not enums and remain integer APIs.

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
add a save/reload regression test for `.pod` and `.podx`, and only then remove
the deprecated implementation. Clean-room namespace work (#152) is excluded
from this document.
