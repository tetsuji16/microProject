# Issue #996 — MSPDI/MPO default times

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/996
Contract frozen: 2026-11-01
Baseline HEAD: `e8a4a2350` (clean worktree)

## Goal and scope

Preserve project default start/finish times across MSPDI and MPO import, save and reload. Do not change the frozen POD serialization descriptor. Microsoft specifies optional project-level defaults for new tasks: https://learn.microsoft.com/en-us/office-project/xml-data-interchange/project-element?view=project-client-2016

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | 09:00 / 18:00 MSPDI and MPO save and reload | Pass locally | `MspdiDefaultTimeZoneTest` covers both reload routes and project isolation |
| A2 | Same time-zone matrix as `MspdiDefaultTimeZoneTest` | Pass locally | UTC, Asia/Tokyo, America/Los_Angeles, Asia/Kolkata; focused Gradle test |
| A3 | `MpoMspdiConsolidationGoldenTest` succeeds | Pass locally | focused Gradle test |
| A4 | Microsoft source documented in issue and regression test | Open | test cites source; issue update pending |

## Current slice

**Next slice:** review and integrate the working-tree change, record the tested candidate and complete the issue evidence.
**Done when:** candidate integration and required CI evidence are recorded and the specification reference is recorded on the issue.
**Then:** close only after all acceptance evidence is complete.

## Latest evidence

- Before implementation the focused test failed in all four zones on MSPDI export DefaultStartTime.
- Focused command: `.\gradlew.bat :microproject_exchange:test --tests 'com.microproject.exchange.MspdiDefaultTimeZoneTest' --tests 'com.microproject.exchange.MpoMspdiConsolidationGoldenTest' --console=plain` passed on the working tree.
- Core and exchange suites: `.\gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain` passed before the final test-only XML reload assertion.
- Physical GUI: not run; user requested no desktop operations.
