# Issue #1001 — file format compatibility identifiers

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1001
Contract frozen: 2026-11-01
Baseline HEAD: `e8a4a2350`; worktree clean.

## Goal and scope

Move format-specific identifiers into compatibility boundaries, preserving frozen POD bytes and legacy MPO/XLSX reads. Separate current write identifiers from read-only aliases and narrow the build allowlist.

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | Fixed POD, MPO, XLSX fixture read/save results unchanged | Local evidence | POD and historical MPO tests in exchange suite; fixed XLSX sample read/save/read task count in XlsxSupportTest |
| A2 | Identifiers appear only in compatibility classes; build allowlist narrowed | Local evidence | source search and verifyArchitectureBoundaries |
| A3 | core and exchange suites pass | Local evidence | core and exchange tests passed; XLSX focused suite rerun after changes |

## Current slice

**Next slice:** review and integrate candidate, and record exact-head CI evidence.
**Done when:** required checks pass on the candidate and fixture coverage is reviewed.
**Then:** close only with all acceptance evidence.

## Latest evidence

- `.\gradlew.bat :microproject_core:test :microproject_exchange:test --console=plain` passed before the final XLSX constant consolidation and test-only fixture addition.
- `.\gradlew.bat verifyArchitectureBoundaries --console=plain` passed.
- `.\gradlew.bat :microproject_exchange:test --tests 'test.com.microproject.exchange.XlsxSupportTest' --console=plain` passed after the final changes. An initial invocation used the wrong test package and found no tests; it was corrected.
- No local GUI operation.
