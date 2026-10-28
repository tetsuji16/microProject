# Issue #953 — Remove unused and commented-out code

State: In progress; first cleanup slice underway.
Issue: https://github.com/tetsuji16/microProject/issues/953
Contract frozen at: 2026-10-27, open issue body.
Baseline HEAD: `e695b33ac2e8453774d13eca134b59e207bc71f5`.
Baseline working tree: preserve existing unrelated documentation, book, GUI test, sample sidecar, and attachment changes; no core production/test files were modified at intake.

## Goal and scope

Remove production code from `modules/*/src/main` only when repository search proves it has no callers; remove commented-out code. Record the detector and caller-search evidence for every deletion, run the owning module tests, and keep an inventory of any retained candidates with reasons.

Out of scope: POD/MPO readers, compatibility adapters, `microproject_contrib`, format contracts, and automated formatting tools.
Blockers: none for the current core cleanup slice.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Detection method recorded | partial | `rg -n '^\\s*//.*;\\s*$' modules/<module>/src/main --glob '*.java'` detects standalone commented Java statements; additional commented declaration/block patterns are reviewed separately. |
| A2 | Every removal has caller-search evidence | partial | The `ConfigurationFile` and `ResourcePool` removals delete comments only; active behavior is unchanged. `getConfDir()` remains live for `DirectoryClassLoader`, independently of the deleted commented fallback. |
| A3 | Owning module test and `git diff --check` pass | pass | `:microproject_core:test --console=plain` and `git diff --check` passed on baseline HEAD `e695b33ac2e8453774d13eca134b59e207bc71f5` plus this comment-only deletion. |
| A4 | Detection results are empty or remaining candidates have reasons | open | Core scan found additional candidates; continue after this finite slice. |

## Current slice

**Current slice:** Removed unreachable/commented code after active returns in `ConfigurationFile.getGeneratedDirectory`, `ConfigurationFile.getDirectoryBundle`, and `ResourcePool.isGroupDirty`; active behavior is unchanged.
**Done when:** caller/provenance evidence is recorded and the owning module test plus whitespace check pass.
**Next slice:** Continue the core commented-code inventory, starting with retained candidates in `ScriptRunner`, hierarchy interfaces, and compatibility/data classes; review ownership before removal.
**Then:** continue the module-by-module cleanup and residual inventory.

## Remaining blockers

- Complete the production-source scan and caller review across modules.
- Record retained candidates with reasons or reduce the detector result to zero.

## Latest evidence

- Baseline: HEAD `293b43fa03bb7e02970664ad81a692539a21433f`; working tree included user changes, including `ResourcePool.java`; its comment-only removal is included in this slice.
- Detection: `rg -n '^\s*//.*;\s*$' modules/microproject_core/src/main --glob '*.java'` returned 387 matches. This heuristic includes comments in retained code and is not itself a deletion list.
- Caller/provenance review: `rg -n 'getGeneratedDirectory\(|externalLocalesDirectory|getConfDir\(' modules/microproject_core/src/main modules/microproject_core/src/test` shows active API usage; `getConfDir()` also has a live use in `DirectoryClassLoader`. Deleted lines were only a commented-out branch after unconditional return.
- `getDirectoryBundle` caller review: `Messages` loads `DirectoryResourceBundles=client`; UI locale selection persists `useExternalLocales` and `externalLocalesDirectory`. The only alternative source was the commented `getConfDir()` branch. History shows this branch was already commented in the pre-rename code; no active locale flow or caller enables it. `getConfDir()` remains live for `DirectoryClassLoader`, independent of this dead branch. `ResourcePool.isGroupDirty()` still returns its `isDirty` field; only the commented project-iteration alternative after that return was removed.
- Current slice verification (HEAD `293b43fa03bb7e02970664ad81a692539a21433f` plus this comment deletion): `:microproject_core:test --console=plain` passed (7 tasks up-to-date); `git diff --check` passed.
- Retained candidates: commented declarations in `ScriptRunner`, hierarchy interfaces, and compatibility/data classes need ownership/provenance review before removal.
- Tests: `:microproject_core:test --console=plain` passed (2026-10-28; 7 actionable tasks, 2 executed).
- `git diff --check` passed (2026-10-28).
