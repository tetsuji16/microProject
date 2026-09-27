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
| `Field.toTaskSheetScheduleValue` | Introduced in ProjectLibre fork commit `54e5480390`; later changed in `b2c7ae394` to Java pattern bindings for `Number`, `Duration`, and `Date`. The branches and null fallback are unchanged. Excluded from #595. | `:microproject_core:test` passed on 2026-09-27; this verifies the current implementations but is not a before/after regression dedicated to modernization. |
| `Field.getGroupDuration` | Introduced in ProjectLibre fork commit `82de370c3d`; later changed in `4849e197e` to bind `Project` in the `instanceof` guard and reuse the binding. Null handling and calendar behavior are unchanged. Excluded from #595. | `:microproject_core:test` passed on 2026-09-27; this verifies the current implementations but is not a before/after regression dedicated to modernization. |

## Initial inventory finding

The existing OpenProj modernization audit explicitly identifies the two
`Field` responsibilities above as ProjectLibre fork additions. The general
provenance CSV is file-based and conservative, so it does not yet encode these
hunk findings. Until Phase 0 reviews further source, no additional Java file
is promoted from `REVIEW` based on naming or current implementation alone.

