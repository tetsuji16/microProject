# Issue #953 — unused and commented-out production code

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/953
Contract frozen: 2026-11-01, issue body
Baseline HEAD: `1bc21058b`; isolated worktree clean.

## Goal and scope

Remove production code with demonstrably no callers and commented-out code. Exclude POD/MPO readers, compatibility adapters, contrib, and formatting tools. Document the detection method, evidence for every deletion, and module tests.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | Detection method recorded; each candidate removed or retained with reason | Open | initial search below; inventory incomplete |
| A2 | Every deletion has caller/search evidence | Open | first slice pending |
| A3 | Owning module tests and `git diff --check` pass | Open | pending |

## Current slice

**Next slice:** classify the remaining commented-code candidates in core `pm` packages and document exclusions.
**Done when:** each candidate is removed or retained with a reason and core tests pass.
**Then:** continue the remaining `src/main` inventory and unused-symbol audit.

## Evidence

- Baseline PowerShell search: `rg -n '^\s*//.*;' modules -g '**/src/main/**/*.java' -g '!**/microproject_contrib/**'` returned 1,317 candidate lines; this includes prose, imports and excluded compatibility paths and is not a defect count.
- Search by symbol and inspect runtime/reflection/configuration references before deleting any live declaration. Preserve explanatory prose and compatibility boundaries.

- Job slice: the deleted mutex, queue and exception-handler statements were already line comments. `rg` found no production or non-module reference to `exceptionHandlerRunnable` beyond its declaration and the deleted comments; the field type `InternalRunnable` is private. Live `runThread`, `groupMutex`/`globalMutex` release and completion paths remain. `JobQueue.startNext` remains public and was not removed based on repository search alone.
- `gradlew.bat :microproject_core:test --console=plain` passed on `1bc21058b` plus the Job slice (7 tasks, 3 executed).
- ScriptRunner slice removes only line-commented sample declarations and their line-commented explanatory text; no active declarations changed. `gradlew.bat :microproject_core:test --console=plain` passed (recompiled, tests up to date because compiled API was unchanged).
- Algorithm/association/company/configuration slice: candidate comments were inactive field/constant/branch/logger statements; active declarations and code paths were unchanged. One match remains in `Configuration.java:52`: prose documenting monitor reentrancy, not code. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed).
- Datatype/document/exchange/field slice: removed line-commented field and converter statements, including entire inactive conditional blocks; live read-only handling, conversions and summaries are unchanged. Four remaining matches in `DurationFormat`, `PercentFormat`, `RateFormat`, and `FieldValues` are explanatory prose about thread safety or capacity. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed).
- Grouping/graphic/scripting slice: deleted only inactive declarations, debug calls and obsolete commented-out blocks. The two remaining search hits in `AssignmentNodeModel` and `TexturedShape` are explanatory prose. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed); a subsequent indentation-only correction resolved the staged whitespace check.
- `PersonalContour`: removed four wholly line-commented incomplete or obsolete methods (`makeFlatIfPossible`, `setContiguousDuration`, `adjustUnitsOld`, `setValue`); the compiled API, serialization fields and active contour algorithms were unchanged. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed). Hermes Beam suggested the four ranges; each was checked against source before removal.
- `pm/criticalpath`: removed inactive listener registration, old schedule-copy and alternate calculation snippets, and commented interface declarations; repository search confirms `copyDatesFrom` exists only in its former commented block. One remaining search hit is prose explaining event barriers. Active scheduling paths are unchanged; `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed).
- `AssignmentDetail`: removed 105 lines from 13 inactive scheduling/debug blocks. A byte comparison of all non-comment, nonblank lines against `c623e70b2` confirmed that active code, including `readObject` and `writeObject`, is unchanged. Commented alternatives inside `readObject` were retained because POD/MPO readers are excluded. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed). Hermes Beam suggested ranges; each was verified before editing.
- `Assignment` and `ContourBucketIntervalGenerator`: removed 101 lines from inactive work, cost, delay and contour examples. Comparing all non-comment, nonblank lines with `93fe1b417` proved active code unchanged. Deserialization/readObject fragments were excluded. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed). Hermes Beam suggested ranges, independently verified in source.
- Calendar/costing slice removed inactive test, debug, and superseded code comments in seven files; active non-comment lines match `9997b3fcb` byte-for-byte. Two search matches remain as explanatory prose in `WorkingCalendar` and `WorkingHours`. The unused local null counter was retained: its second `getWeekDay` call is overridable, so removing it could affect external subclasses. `gradlew.bat :microproject_core:test --console=plain` passed (7 tasks, 2 executed).
