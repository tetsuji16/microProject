# ProjectLibre Java 25 modernization progress

Status: in progress; this document tracks ProjectLibre-origin code separately
from OpenProj-origin work in issue [#595](https://github.com/tetsuji16/ProjectLibre/issues/595).
Plan: [#727](https://github.com/tetsuji16/ProjectLibre/issues/727).

## Scope and provenance rule

The target is active production code whose ProjectLibre origin is established
at the changed hunk by comparison with the OpenProj baseline and ProjectLibre
history/source. The file name, old copyright notice, or absence of an OpenProj
path match is not sufficient evidence. `docs/legal/license-provenance.csv`
currently classifies no Java source row as ProjectLibre-origin; unresolved
mixed files remain `REVIEW`. Existing hunk-level findings below are recorded
from `docs/audit/openproj-java25-modernization.md` and must not be counted as
OpenProj progress.

## Plan

- [ ] Phase 0: review provenance for active Java hunks and record verified
  ProjectLibre-origin candidates, callers, and compatibility boundaries.
- [ ] Phase 1: apply focused type-safety and Java language improvements where
  behavior and API erasure remain stable.
- [ ] Phase 2: modernize higher-risk APIs or redundant responsibilities only
  after caller, reflection, persistence, and UI-route review.
- [ ] Phase 3: verify the relevant module/callers and update this record and
  the issue checklist for each completed tranche.

## Verified modernization candidates

| ProjectLibre-origin responsibility | Evidence and change | Verification |
|---|---|---|
| `FieldUtil.getCategories` | The `FieldUtil` class exists in the OpenProj baseline, but this method does not; it first appears in ProjectLibre 1.9.8 (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`). Replaced its sized-array `toArray` call with `categories.toArray(String[]::new)` and applied diamond inference to its local collections; result type, order, and contents are unchanged. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` passed 2026-09-27; regression asserts hierarchy order and contents. |
| `Dictionary.add` and `Dictionary.getClassesAsArray` | `Dictionary` is absent from the OpenProj baseline and exists in ProjectLibre 1.9.8 as `org.projectlibre.core.dictionary.Dictionary`. Bound `HasCategories` in the guard, replaced the sized-array conversion with `classes.toArray(Class<?>[]::new)`, and applied diamond inference to collection construction and anonymous iterators. Category/ALL indexing and class results are unchanged. | `:microproject_core:test --tests "com.microproject.core.dictionary.DictionaryTest" --console=plain` passed 2026-09-27; regression checks category and ALL indexes plus class-array contents. |
| ProjectLibre JAXB map adapters | `DictionaryAdapter`, `DictionaryAdapterList`, `MapAdapter`, and `MapAdapterList` exist in ProjectLibre 1.9.8 and are absent from the OpenProj baseline. Replaced explicit generic constructor type arguments with diamond inference; collection types, capacities, and JAXB method signatures are unchanged. | Full `:microproject_core:test --console=plain` passed 2026-09-27. |
| `ResourceUtil.createObject` | `ResourceUtil` exists in ProjectLibre 1.9.8 and is absent from the OpenProj baseline. Replaced deprecated `Class.newInstance()` with `getDeclaredConstructor().newInstance()` while preserving package fallback and rethrowing the underlying constructor exception. | `:microproject_core:test --tests "com.microproject.core.util.ResourceUtilTest" --console=plain` passed 2026-09-27; covers successful construction and original exception identity. |
| `FieldUtil.convertField` Boolean skip condition | The Boolean `instanceof` plus cast condition matches ProjectLibre 1.9.8 `projectlibre_core/src/com/projectlibre/core/fields/FieldUtil.java`; it is a ProjectLibre-modified responsibility in a mixed-origin class. Replaced the redundant cast with a Boolean pattern binding; null and false still skip the setter, while true continues through conversion and reflection. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` verifies false skip and true setter behavior. |
| `Dictionary.iterator(DictionaryCategory)` empty result | The missing-category branch matches ProjectLibre 1.9.8 `org.projectlibre.core.dictionary.Dictionary`. Repository search found no production callers. Replaced its custom empty iterator with `Collections.emptyIterator()`, so `hasNext` is false and `next` follows the `Iterator` contract by throwing `NoSuchElementException`; removal follows the JDK empty iterator's `IllegalStateException` behavior. | `:microproject_core:test --tests "com.microproject.core.dictionary.DictionaryTest" --console=plain` verifies empty, exhausted-next, and remove behavior. |

## Explicit exclusions

- `Field.toTaskSheetScheduleValue` is absent from the ProjectLibre 1.9.8
  source and OpenProj baseline; its first appearance is this repository's
  `54e5480390` refactor. It is microProject fork code, not ProjectLibre-origin
  code.
- `Field.getGroupDuration` is absent from the ProjectLibre 1.9.8 source and
  OpenProj baseline; it is a later microProject fork addition.
- These exclusions are not counted toward #727 or #595.

## ProjectLibre-added type inventory

Compared additions between the OpenProj baseline (`d2fa3c20a`) and the
ProjectLibre 1.9.8 source (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`) with
the active `modules/` sources:

- `FieldList` is active and carries JAXB names/identity references. Its current
  class has no remaining generic construction, obsolete reflection API, or
  cast-after-`instanceof` candidate; its XML annotations remain a compatibility
  boundary.
- ProjectLibre additions `FieldManager`, `VersionComparator`,
  `ScheduleChangedEvent`, `Scheduler`, and `SnapshotList` have no active source
  counterpart under `modules/` and are not current modernization targets.
- The upstream-added `com.projectlibre.pm.tasks.TaskSnapshot` has no active
  counterpart. The similarly named active `com.microproject.pm.task.TaskSnapshot`
  corresponds to the separate `com.projectlibre1.pm.task.TaskSnapshot` path,
  which maps to OpenProj's `com.projity.pm.task.TaskSnapshot`; do not conflate
  these classes during provenance review.

## Initial inventory finding

The previous audit classified the two `Field` responsibilities above as
ProjectLibre fork additions based on commits in this repository. The general
provenance CSV remains file-based and conservative; the verified hunk-level
finding above is recorded here rather than promoting a whole mixed file. Phase
0 continues for additional source; no Java file is promoted from `REVIEW` based
on naming or current implementation alone.

