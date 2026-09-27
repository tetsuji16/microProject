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
| `Field.toTaskSheetScheduleValue` | Excluded: not present in the ProjectLibre 1.9.8 source (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`) or OpenProj baseline; its first appearance is in this repository's `54e5480390` refactor. This is microProject fork code, not ProjectLibre-origin code. | Not counted toward #727 or #595. |
| `Field.getGroupDuration` | Excluded: not present in the ProjectLibre 1.9.8 source or OpenProj baseline; introduced in this repository's later fork development. This is microProject fork code, not ProjectLibre-origin code. | Not counted toward #727 or #595. |

## Initial inventory finding

The previous audit classified the two `Field` responsibilities above as
ProjectLibre fork additions based on commits in this repository. That was too
broad: the cited commits are microProject fork commits and the responsibilities
do not occur in the upstream ProjectLibre 1.9.8 source. The general provenance
CSV is file-based and conservative, so it does not encode these hunk findings.
Until Phase 0 reviews further source, no Java file is promoted from `REVIEW`
based on naming or current implementation alone.

