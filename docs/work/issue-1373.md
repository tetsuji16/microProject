# Issue #1373 — Move Groovy from 3.0 to 5.0

**State:** Implementation complete, local verification green, CI pending
**Contract frozen at:** 2026-11-26
**Baseline HEAD:** `06985c530` (origin/master after #1377)
**Worktree:** `C:/Users/tetsu/Documents/codex/mp-1373`
**Branch:** `issue/1373-groovy5`

## Acceptance criteria (from issue)

| ID | Required result/evidence | Status | Evidence |
|---|---|---|---|
| AC1 | `ConfiguredFormulaCompilationTest` passes on the Groovy 3 commit and the Groovy 5 commit | done | `6b33a71a1` (3.0.25): 2 tests, 0 failures. `HEAD` (5.0.8): 2 tests, 0 failures |
| AC2 | `rg -n "org\.codehaus\.groovy" gradle/libs.versions.toml build.gradle.kts` is empty | done | `git grep`: exit 1, no match |
| AC3 | `installDist` lib has a `groovy-5.` jar and no `groovy-3.` jar | done | the lib directory lists `groovy-5.0.8.jar` only |
| AC4 | `:microproject_core:test` and `:microproject_ui:test` pass | done | core 1,490 tests / 2 skipped / 0 failures; ui 1,986 / 4 skipped / 0 failures |
| AC5 | `verifyDependencyAllowlist` passes | done | BUILD SUCCESSFUL |
| AC6 | PR `build` and `GUI acceptance` pass (filter, sort and group menus) | pending | CI on the pushed head |

## Implementation

- `modules/microproject_core/src/test/java/com/microproject/formula/ConfiguredFormulaCompilationTest.java`
  (new): compiles every filter, sorter and action-list formula the real configuration
  declares, and fails with the ids that do not compile.
- `gradle/libs.versions.toml`: `groovy = "5.0.8"`, module `org.apache.groovy:groovy`.
- Root `build.gradle.kts`: allowlist group `org.codehaus.groovy` → `org.apache.groovy`.
- `gradle/verification-metadata.xml`: added `org.apache.groovy:groovy:5.0.8`
  (sha256 `942993ee…62e0fa8`, taken from the Maven Central `.sha256`), removed
  `org.codehaus.groovy:groovy:3.0.25` and `org.codehaus.groovy:groovy:2.4.21` (2.4.21
  appears in no module's dependency report).
- `packaging/licenses/third-party/index.html`: Apache Groovy version 2.4.21 → 5.0.8.

`GroovyClassCompiler` and `FormulaClass` compiled and ran unchanged on Groovy 5.

## Findings reported on the issue

- `view.xml` defines no `<filters id="Filters.report">`, so `TransformList.getInstance("report_filters")`
  returns null and `ConverterContext.retrieveFilter()` would throw when `filterId` is set.
  The test skips that name and says so; the null dereference is left for a separate fix.
- `Filter.WhoDoesWhatReport` (`definition="…AssignmentCompositionFilter"`) does not instantiate
  on Groovy 3 either, so the test only compiles entries that declare a formula text.

## Verification commands run

```text
.\gradlew.bat :microproject_core:test --tests "com.microproject.formula.ConfiguredFormulaCompilationTest" --console=plain
.\gradlew.bat verifyDependencyAllowlist build installDist --console=plain
python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json
git diff --cached --check
```

All exited 0.

## Next step

Push the branch, open the PR with every completion criterion and both Groovy runs, wait for
`build` and `GUI acceptance`, then merge.
