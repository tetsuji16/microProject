# Issue #1483 work packet

- Goal: restore the release-blocking packaged MPP/POD import smoke verification.
- Issue: https://github.com/tetsuji16/microProject/issues/1483
- Baseline tested: `9a58bdee786d1802375f38621f0ecef6eeb58a94` (`origin/master`); run 46055954774 failed in `verifyPackagedFileImports` with `ClassNotFoundException: com.microproject.integration.PackagedImportSmokeMain` (`BUILD FAILED` in 44s).
- Change: `build.gradle.kts` mainClass now uses `com.microproject.ui.app.PackagedImportSmokeMain`; dependencies, args, module scope, and fixtures unchanged.
- Focused verification after change: `.\gradlew.bat verifyPackagedFileImports --console=plain` → `BUILD SUCCESSFUL in 18s`; generated `microproject_ui.bat` classpath check OK; MPP loaded with 145 tasks; POD loaded with 145 tasks.
- Repository search: `rg -n "com\.microproject\.integration\.PackagedImportSmokeMain|PackagedImportSmokeMain" --glob '!**/build/**' .` shows no remaining stale fully qualified production/config reference. Current setting and test class package are `com.microproject.ui.app`; a historical path string remains in license provenance metadata.
- Broad verification: `.\gradlew.bat build --console=plain` → `BUILD SUCCESSFUL in 4m 41s`.
- Whitespace: `git diff --check` and `git diff --cached --check` were empty.
- PR: https://github.com/tetsuji16/microProject/pull/1484; head `3adc0f4e1` before packet update. Checklist passed; `build` and GUI acceptance were pending at last check.
- Next slice: update packet/body evidence for this final head, push documentation-only update, then watch required PR checks to completion.
