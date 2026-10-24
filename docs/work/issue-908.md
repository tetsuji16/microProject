# Issue #908 — Sidecar name migration

State: IMPLEMENTED — frozen local verification passed; final CI/closure status in [integration record](https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503)
Issue: https://github.com/tetsuji16/microProject/issues/908
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: untracked `.codex-remote-attachments/` and `docs/work/issue-774.md`; other concurrent #774 slice edits observed and preserved.

## Scope and boundaries

This worker owns the bounded #908 slices recorded below. New collaboration
metadata writes use `<base>.microproject-sync.json`. The former
`<base>.projectlibre-sync.json` remains readable and is preserved after copy.
MPO/MPOF local offline collaboration remains enabled; MPO/POD wire contracts
are unchanged.

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

## XLSX name/data migration

The writer emits `_MicroProject_META`, `_MicroProject_MSPDI`, and
`_MicroProject_NATIVE`, with `microproject` / schema `3` metadata and the
`MicroProjectNativeData` marker for native project payloads. The reader gives
canonical names precedence, reads legacy `_PL_META`, `_PL_DATA`, `_PL_NATIVE`
and `_ProjectLibre_MSPDI` workbooks when canonical names are absent, and fails
closed for malformed canonical metadata or payloads. It accepts the missing
legacy metadata row labels emitted by the former writer, but validates kind and
schema values. The previous `writeMeta` called `createRow` twice per row,
discarding each label cell; the new writer creates each row once. The first
focused run exposed this (5 failures); after the fix the same test passed.

`ProjectLibreXlsxReader` / `ProjectLibreXlsxWriter` are now
`MicroProjectXlsxReader` / `MicroProjectXlsxWriter`, and all exchange callers
use the canonical types. No old Java type aliases remain. `XlsxSupportTest`
retains MPP/XML coverage and now asserts the exact writer metadata/marker,
native model round trip, old workbook read, name priority, and corrupt
canonical no-fallback behavior.

| XLSX criterion | State | Evidence |
|---|---|---|
| Canonical sheet/cell/metadata write and model round trip | pass | `XlsxSupportTest` |
| Legacy sheets/cell/meta read; canonical wins when both names exist | pass | `legacyNativeXlsxNamesReadAndCanonicalPayloadWinsOrFailsClosed` |
| Corrupt canonical metadata/native/MSPDI payload never falls back to legacy | pass | Same conflict/failure test |
| MPP and XML read contracts retained | pass | Existing `mspImporterCanReadMpp`, XML cases in `XlsxSupportTest` |
| Reader/writer Java identifiers use microProject names | pass | caller `rg` inventory; focused exchange test |

## First-run preferences and descriptor root

`Main` wrote `projectlibrefirstRun` but read `projectlibreFirstRun`; both old
keys are read in that order after canonical `microprojectFirstRun`, and startup
migrates the selected value once without replacing an existing canonical
timestamp. The run counter also moves to `microprojectRunNumber`, reading
`projectlibreRunNumber` as fallback. Tests use an isolated Preferences child
node. `LocalSession`'s `projectlibre.fileName` is written only to transient
`ProjectData.attributes`, has no repository readers, and is removed while the
in-memory descriptor-to-path map remains. Descriptor discovery now checks
`~/microProject` first and keeps `~/ProjectLibre` as read-only discovery for
existing local project files; it does not move or delete files. Focused tests
confirm both roots are ordered and old folder discovery remains available.

| Criterion | State | Evidence |
|---|---|---|
| Canonical first-run key priority; old casing keys read and migrated once | pass | `MainFirstRunPreferencesTest` |
| Canonical run counter write; legacy run counter fallback | pass | Same isolated Preferences test |
| `~/microProject` preferred while `~/ProjectLibre` remains read-only | pass | `LocalSessionDescriptorRootsTest` |

`SwingFileChooserProvider` now uses the saved `lastDirectory` first; when it
is unset, it prefers an existing `~/microProject`, then an existing
`~/ProjectLibre` as a read-only discovery fallback, and otherwise uses the
home directory without creating either folder. The private chooser field is
`nativeProjectFilter`. A focused temporary-home test covers all four cases.

## Latest verification

- `.\gradlew.bat :microproject_exchange:test --tests "test.com.microproject.exchange.XlsxSupportTest" --console=plain --no-daemon --max-workers=1` — pass, 21 tests, 0 failures/errors/skips (after `writeMeta` fix).
- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.main.MainFirstRunPreferencesTest" --console=plain --no-daemon --max-workers=1` — pass, 8 tests.
- `.\gradlew.bat :microproject_core:test --tests "com.microproject.session.LocalSessionDescriptorRootsTest" --console=plain --no-daemon --max-workers=1` — pass, 2 tests.
- `.\gradlew.bat :microproject_ui:test --tests "com.microproject.util.SwingFileChooserProviderTest" --console=plain --no-daemon --max-workers=1` — blocked before test compilation by concurrent #905 core API edits (`SessionFactory` methods and `Session.getLoadProjectDescriptorsJob`); rerun pending integration.
- Earlier sidecar evidence remains: core metadata 16 tests and application suite 37 tests passed; UI GUI-test sources compiled successfully. Physical GUI was not run.
- `git diff --check -- <owned XLSX/Main/LocalSession files>` — pass. Repository-wide `git diff --check` currently reports whitespace in concurrent, out-of-scope `GraphicManager.java` and `client_ru.properties.1251`; those files were not edited here.

## Final integration gate

The frozen candidate passed architecture/naming gates, maintenance 20/20,
source inventory, clean build/installDist (2,880 tests; no failures/errors/skips),
and GUI source compilation. This supersedes earlier slice-time pending local
checks above. Candidate SHA, Windows Robot outcomes and closure are maintained
in the linked integration record; physical acceptance is required before close.
