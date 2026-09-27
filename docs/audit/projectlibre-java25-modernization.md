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
| `FieldUtil.getCategories` | The `FieldUtil` class exists in the OpenProj baseline, but this method does not; it first appears in ProjectLibre 1.9.8 (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`). Replaced its sized-array `toArray` call with `categories.toArray(String[]::new)`; result type, order, and contents are unchanged. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` passed 2026-09-27; regression asserts hierarchy order and contents. |
| `Dictionary.add` and `Dictionary.getClassesAsArray` | `Dictionary` is absent from the OpenProj baseline and exists in ProjectLibre 1.9.8 as `org.projectlibre.core.dictionary.Dictionary`. Bound `HasCategories` in the guard and read from the binding; replaced the sized-array conversion with `classes.toArray(Class<?>[]::new)`. Category/ALL indexing and class results are unchanged. | `:microproject_core:test --tests "com.microproject.core.dictionary.DictionaryTest" --console=plain` passed 2026-09-27; regression checks category and ALL indexes plus class-array contents. |

## Explicit exclusions

- `Field.toTaskSheetScheduleValue` is absent from the ProjectLibre 1.9.8
  source and OpenProj baseline; its first appearance is this repository's
  `54e5480390` refactor. It is microProject fork code, not ProjectLibre-origin
  code.
- `Field.getGroupDuration` is absent from the ProjectLibre 1.9.8 source and
  OpenProj baseline; it is a later microProject fork addition.
- These exclusions are not counted toward #727 or #595.

## Initial inventory finding

The previous audit classified the two `Field` responsibilities above as
ProjectLibre fork additions based on commits in this repository. The general
provenance CSV remains file-based and conservative; the verified hunk-level
finding above is recorded here rather than promoting a whole mixed file. Phase
0 continues for additional source; no Java file is promoted from `REVIEW` based
on naming or current implementation alone.

