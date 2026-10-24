# Issue #907 — Display and identifier renames

State: IMPLEMENTED — frozen local verification passed; final CI/closure status in [integration record](https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503)
Issue: https://github.com/tetsuji16/microProject/issues/907
Baseline HEAD: `66066d683a835d4282e06240dd59b1c2b7ef493a`
Baseline working tree: #904 core/UI edits and issue packet changes were in progress; the XLSX reader/writer, callers, and `XlsxSupportTest` were clean at intake.

## Scope

This packet retains the XLSX reader/writer rename record below and adds the
non-XLSX display and internal identifier slice: Help/About ribbon/menu IDs,
GUI component locators, `ProjectLibreDateField`, transient alert/action keys,
the XML collaboration separator constant name, and current-product resource
labels. Every source caller, locale bundle, test, and GUI inventory row is
updated with its canonical name. No persisted format value is rewritten.

## Acceptance

| Result | State | Evidence |
|---|---|---|
| Canonical XLSX reader/writer type names and filenames; every caller updated | implemented; focused test passed | `MicroProjectXlsxReader` / `MicroProjectXlsxWriter`; sidecar reported `XlsxSupportTest` 21/21 passed |
| Non-XLSX display/action/component/date-field identifiers canonicalized across callers and GUI inventory | implemented; focused verification passed | core resource/transform tests 7/7 and UI chooser/license/ribbon tests 31/31; GUI test source compiles |
| Help/About and Help→License physical ribbon routes render canonical titles | implemented; compile verified; GUI evidence pending | `RibbonExternalCommandGuiAcceptanceTest` physically clicks Help/About and License, checks both loaded tabs, then checks OK and Escape close routes |
| Existing MPP/XML/XLSX interoperability retained | focused XLSX test passed; broader exchange suite pending | sidecar `XlsxSupportTest` 21/21; full exchange suite not run in this slice |
| MPO/POD data and required legacy format descriptions preserved | pass by scope; verify diff | MPO/POD payload values, POD labels, import guidance, aliases, and separator literal are unchanged |

## Provenance and compatibility

The former public Java class names were local exchange implementation details
used by `MicrosoftImporter`, `MspImporter`, `ProjectWriterFactory`, and
`XlsxSupportTest`. The module does not promise compatibility for these internal
types. XLSX remains an external interoperability format; no MPO/POD serialized
data is changed. Existing legacy format strings remain only where required for
reading old workbooks.

The UI action IDs, Swing component names, `MicroProjectDateField`, message
title keys, and chooser/action resource IDs are transient implementation or
presentation names, not saved project data. Alert titles now use one
`Title.MicroProjectError` / `Title.MicroProjectWarning` key per locale through
direct lookup; obsolete `Open_Title.*`, `Title.ProjectLibre*`, and
`Title.Projity*` aliases are removed. The collaboration XML separator's Java
constant was renamed only; its `@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@`
wire value and fixed fixture remain unchanged. The old ProjectLibre/OpenProj
POD/native file-format labels, legacy-format conversion guidance, third-party
license attribution, and `AutoRecoveryStore` legacy read paths are intentional
compatibility records and remain untouched. #908 owns its local-session and
preference descriptor changes.

The documented launcher entry points are now `scripts/run_microproject.ps1`
and `scripts/run_microproject.bat`; active documentation and references use
those names. The process matcher still recognizes `projectlibre` to stop an
older install, and `projectlibre-contrib.jar` remains the actual artifact
name. Launcher temp-file prefixes and the log environment variable are
canonical. `scripts/shot.ps1` now writes under repository `build/gui-shots/`
instead of a machine-specific absolute path.

## Verification

Candidate tested SHA: `c4a02f26895f781fe8d91bd1b097b701d11b51db` (shared working tree).
Command run successfully:
`gradlew.bat :microproject_core:test --tests ClientBundleProductNameTest --tests TransformListTest --tests CommonTransformFactoryTest --tests LegacyJaxbConfigurationTest :microproject_ui:test --tests SwingFileChooserProviderTest --tests LicenseDialogResourceTest --tests RibbonStructureTest --tests RibbonCommandCatalogTest :microproject_ui:compileGuiTestJava --console=plain`.
The physical Robot journey remains for the shared Windows GUI acceptance run.
Latest maintenance verification for the launcher slice passed: PowerShell AST
parse for `run_microproject.ps1` and `shot.ps1`; Bash syntax parse for both
launch scripts; `python -m unittest discover -s scripts/tests -v` (20/20); and
`python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json`.
No active caller references the former launcher filenames. `git diff --check`
passed for this slice.

## Next slice

Run the shared Windows GUI acceptance and broader exchange suite, then record
the physical Help/About/License journey evidence before closing #907.

## Final integration gate

The frozen candidate passed architecture/naming gates, maintenance 20/20,
source inventory, clean build/installDist (2,880 tests; no failures/errors/skips),
and GUI source compilation. This supersedes earlier slice-time pending local
checks above. Candidate SHA, Windows Robot outcomes and closure are maintained
in the linked integration record; physical acceptance is required before close.
