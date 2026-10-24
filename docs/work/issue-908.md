# Issue #908 — Sidecar name migration

State: PARTIAL (sidecar slice complete; XLSX and first-run preferences remain)
Issue: https://github.com/tetsuji16/microProject/issues/908
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: untracked `.codex-remote-attachments/` and `docs/work/issue-774.md`; other concurrent #774 slice edits observed and preserved.

## Scope and boundaries

This worker owns only the local collaboration metadata sidecar name migration.
New writes use `<base>.microproject-sync.json`. The former
`<base>.projectlibre-sync.json` remains readable and is preserved after copy.
MPO/MPOF local offline collaboration remains enabled; MPO/POD wire contracts
are unchanged. XLSX sheet/cell/meta and first-run preferences remain for later
slices of #908.

## Sidecar acceptance

| Result | State | Evidence |
|---|---|---|
| Canonical sidecar and matching canonical lock names used for new writes | pass | `CollaborationMetadataStoreTest` |
| Legacy-only sidecar read, canonical write, legacy file preserved | pass | `unversionedLegacySidecarLoadsAndIsUpgradedWithoutLosingUsers` |
| Canonical wins when both names exist | pass | `canonicalSidecarWinsWhenBothNamesExist` |
| Corrupt canonical fails without falling back to valid legacy data | pass | `malformedCanonicalSidecarDoesNotFallBackToLegacy` |
| Concurrent new stores during legacy migration retain source and both updates | pass | `concurrentLegacyMigrationAndUpdatesUseCanonicalLock` |
| Application load options advertise canonical name | pass | `ProjectLoadWorkflowTest` |
| UI multiple-open artifact cleanup recognizes canonical and legacy names | changed; GUI not run | `MultipleLocalProjectOpenGuiAcceptanceTest` |
| User/architecture documentation describes canonical name, fallback and mixed-version boundary | pass | `docs/mpof/onedrive-collaboration.md`; `docs/architecture/temporary-artifact-lifecycle.md` |

## Lock and migration boundary

Every new operation takes the canonical JVM/OS lock. When canonical metadata is
absent and legacy metadata exists, migration additionally takes the former
legacy marker lock while reading/copying, then writes canonical metadata under
the canonical lock. Lock order is canonical then legacy. Once canonical exists
it is authoritative, including when corrupt; no silent fallback or automatic
merge occurs. The legacy source is not automatically deleted. An older process
that writes the legacy file after migration releases its old lock can create a
divergent stale copy; later new clients still choose canonical. Concurrent
old/new app behavior after migration is therefore not a cross-version merge
contract.

## Verification

- `.\gradlew.bat :microproject_core:test --tests "com.microproject.collaboration.CollaborationMetadataStoreTest" --console=plain --no-daemon --max-workers=1` — passed, 16 tests (rerun after review changes).
- `.\gradlew.bat :microproject_application:test --tests "com.microproject.application.ProjectLoadWorkflowTest" --console=plain --no-daemon --max-workers=1` — passed, 5 tests.
- `.\gradlew.bat :microproject_application:test --console=plain --no-daemon --max-workers=1` — passed, 37 tests, 0 failures/errors/skips (rerun after review changes; integration evidence for #903's transient sidecar assertion fix).
- `.\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1` — passed; 4 existing `Unsafe` warnings reported while compiling UI test sources.
- `git diff --check` — passed.
- GUI acceptance was not run; no production UI behavior changed.

## Next slice

Continue #908's XLSX native sheet/cell/meta migration, then first-run preference
name migration. Do not close #908 until those and the full issue contract pass.
