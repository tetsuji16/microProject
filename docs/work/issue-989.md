# Issue #989 — MSPDI default start/finish times shift with the host time zone

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/989
Contract frozen at: 2026-10-31, issue body at intake
Baseline HEAD: `47a8239fe`
Baseline working tree: clean worktree `claude/mpo-pod-io`

## Goal and scope

MSPDI and MPO `DefaultStartTime` / `DefaultFinishTime` keep their floating time of day in every host zone.

Out of scope: storing non-default project-level times; the importer does not read these elements, so an input of 09:00 still normalizes to the calendar option's 08:00 on reopen.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Root cause fixed in the converter, not masked | pass | `MPXConverter.toMpxTimeOfDay`; red in Tokyo, Los Angeles, Kolkata with the fix removed |
| A2 | Round trip across zones without changing the default zone mid-test | pass | `MspdiDefaultTimeZoneTest`, one child JVM per zone |
| A3 | Golden mask for these elements removed | pass | `MpoMspdiConsolidationGoldenTest`, digest updated to the correct 08:00/17:00 output |

## Next slice

Merge after CI is green for the exact PR SHA.

## Evidence

- `.\gradlew.bat :microproject_core:test :microproject_exchange:test :microproject_application:test :microproject_ui:test :microproject_ui:compileGuiTestJava --continue --console=plain` — BUILD SUCCESSFUL locally.
