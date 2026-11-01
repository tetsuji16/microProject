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

**Next slice:** classify the remaining `src/main` candidates and investigate unused symbols by callers, reflection and configuration.
**Done when:** each candidate has a justified disposition and affected module tests pass.
**Then:** check the complete inventory, CI and issue acceptance.

## Evidence

- Baseline PowerShell search: `rg -n '^\s*//.*;' modules -g '**/src/main/**/*.java' -g '!**/microproject_contrib/**'` returned 1,317 candidate lines; this includes prose, imports and excluded compatibility paths and is not a defect count.
- Search by symbol and inspect runtime/reflection/configuration references before deleting any live declaration. Preserve explanatory prose and compatibility boundaries.

- Job slice: the deleted mutex, queue and exception-handler statements were already line comments. `rg` found no production or non-module reference to `exceptionHandlerRunnable` beyond its declaration and the deleted comments; the field type `InternalRunnable` is private. Live `runThread`, `groupMutex`/`globalMutex` release and completion paths remain. `JobQueue.startNext` remains public and was not removed based on repository search alone.
- `gradlew.bat :microproject_core:test --console=plain` passed on `1bc21058b` plus the Job slice (7 tasks, 3 executed).
- ScriptRunner slice removes only line-commented sample declarations and their line-commented explanatory text; no active declarations changed. `gradlew.bat :microproject_core:test --console=plain` passed (recompiled, tests up to date because compiled API was unchanged).
