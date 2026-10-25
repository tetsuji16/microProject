# Issue #921 — advertised secondary external format contracts

State: ACTIVE; finite scope implemented and verified; no issue closure or external approval claimed.
Issue: https://github.com/tetsuji16/microProject/issues/921
Contract frozen: 2026-10-25 from issue body and finite intake.
Tested source SHA: `82bcd02209b349efdf2fba1979cdc47fb27ff364`; all listed changes are uncommitted.
Baseline working tree included untracked `.codex-remote-attachments/` and sample sync sidecars; preserved.

## Acceptance

| ID | Required result | State / evidence |
|---|---|---|
| A1 | State read/write direction and version for each discovered format. | Complete; matrix below distinguishes application paths from internal writers. |
| A2 | Independent fixtures and supported-direction round trip/MPO reload for applicable semantics. | Complete for bounded fields listed below; XLSX summary, native schema-3, and embedded MSPDI imports each have MPO save/reload coverage; no broad parity claim. |
| A3 | Separate XLSX schema-3 native payload, MSPDI payload, and summary fallback; exercise relevant edge cases; preserve POD. | Complete; `XlsxSupportTest`; no POD changes. |
| A4 | Record losses, failure behavior, exact source SHA, fixture/raw hashes, commands, results, and limitations. | Complete; evidence and gaps below. |

## Direction and version matrix

| Format | Read | Write | Bounded evidence / known losses |
|---|---|---|---|
| MPX | `MspImporter` → MPXJ `MPXReader`; MPX 4.0 fixture/read support. | `ProjectWriterFactory` → MPXJ `MPXWriter`; emits MPX 4.0. | Read: task order/identity, 8 dependency pairs/types/lags and calendar; MPO reload. Writer: task name and 40% progress readback. Other fields/version coverage unverified. Missing required Project Header record 30 now fails with typed `MPXJException`. |
| GNOME Planner | `MspImporter` → MPXJ `PlannerReader`; independent Planner v2 fixture. | `ProjectWriterFactory` → MPXJ `PlannerWriter`; emits v2. | Read: two task identities, 0% progress, FS predecessor, standard calendar, resource, MPO reload. Writer: task name preserved; source 40% completion becomes 0%. Other versions and fields unverified. A valid empty v2 project remains importable. |
| XLSX | `MicroProjectXlsxReader`: microProject native payload schema 3 first; embedded MSPDI fallback; external Tasks/Resources/Assignments summary fallback. | `MicroProjectXlsxWriter`: schema-3 native payload and MSPDI worksheet; summary sheets are external fallback. | Native schema-3, embedded MSPDI, and summary-only fallback imports each save/reload as MPO. Summary fallback covers hierarchy, 40% progress, resource/assignment units including zero and omitted default. No general spreadsheet parity claim. |
| PMXML | No reader path identified; not advertised for Open. | Internal factory registration only: MPXJ `PrimaveraPMFileWriter`. | Dispatch/non-exposure only; version and interoperability unverified. |
| JSON | No reader path identified; distinct from MPO sidecar/container data, not advertised for Open. | Internal factory registration only: MPXJ `JsonWriter`. | Dispatch/non-exposure only; version and interoperability unverified. |
| SDEF | No reader path identified; not advertised for Open. | Internal factory registration only: MPXJ `SDEFWriter`. | Dispatch/non-exposure only; version and interoperability unverified. |

MPP, MSPDI XML, and POD are covered by their separate issue/format contracts; this slice does not expand them. No absent reader/writer is claimed.

## Evidence and fixture semantics

- Independent MPX files are unchanged MPXJ v11.5.4 Project 98 samples from commit `c75014d0a7e310315ce46c426dc3108dc6e73f09`; source/test authority for relation identities and expected types is upstream [`TaskLinksTest`](https://github.com/joniles/mpxj/blob/c75014d0a7e310315ce46c426dc3108dc6e73f09/src/test/java/net/sf/mpxj/junit/task/TaskLinksTest.java), plus literal calendar/task records in the files. `task-links-project98.mpx` SHA-256 `633349BF58E0410502F0D5D42CFA0EABC3FE4CF913059A19F1F19937A2026BCD`; `calendar-calendars-project98.mpx` SHA-256 `95C19A4785DEE2E2BEE8FAFBE72632E51D7E53F1782E0D13359DFBA3E2D4B0C5`. MPX record 30 is the Project Header record ([Oracle MPX Data Map Guide](https://docs.oracle.com/cd/F88968_01/English/Mapping_and_Schema/ms_project_import_export_data_map_mpx/97822.htm)); Microsoft KB 270139 describes Project versions and MPX 4.0 record semantics. The failure contract derives from that required record, not an empty-file heuristic.
- Independent Planner fixture `planner-sample.planner` is unchanged from the same MPXJ commit, [`junit/data/planner-sample.planner`](https://github.com/joniles/mpxj/blob/c75014d0a7e310315ce46c426dc3108dc6e73f09/junit/data/planner-sample.planner); SHA-256 `955576BE825FE3CE40B034BE2F1CC839E3AE2CC0E7F8E2583B74B64CE1F2861F`. GNOME Planner source fixture declares `mrproject-version="2"`; tested values are literals from that fixture, including task IDs/order, predecessor type/ID, calendar week and resource units. MPXJ upstream is LGPL-2.1-or-later; provenance is recorded in `PLANNER-PROVENANCE.md`.
- Invalid MPX raw bytes (`MPX,Microsoft Project for Windows,4.0,ANSI` plus record 10, lacking record 30) SHA-256 `76FE85B7DC678D18A8E71B6E46D79D58C3FDD154DCF6483299B2E63731DBB82D`; invalid truncated Planner XML SHA-256 `84E9D9D90B2E558FB16D809511B2273E851F8182AF079B6E197A8BA276271DDD`; valid empty Planner v2 XML SHA-256 `0B5360A344799E7720CDD9A889E448869307A002AA2282BD178BB3E2F59AA06B`. Tests exercise actual `MspImporter` input boundaries, assert `MPXJException`, unchanged caller bytes and no `Completed` progress for failures; empty Planner imports with zero tasks.
- Primary compatibility basis is Microsoft's Project file-format support and historical MPX record documentation, GNOME Planner v2 XML source sample/format declaration, and literal input records; MPXJ tests provide independent expected MPX task-link identities. These checks are format-derived source evidence, not physical Microsoft Project runs.
- XLSX root cause/evidence: summary fallback initially produced 10 domain task-list entries for four rows (the three children appeared three times), and MPO load rejected duplicate leveling-option identities. `MicroProjectXlsxReader` added all rows to `ProjectFile` roots, then linked children using MPXJ `Task.addChildTask`; inspected MPXJ 11.5.4 bytecode shows this links the child but leaves it in the root list. Since `MspImporter` recursively imports from roots and children, it duplicated each child. Reader now removes each linked child from `ProjectFile.getChildTasks()` while retaining all tasks in the task registry. Regression asserts one root/four registered tasks, four unique imported task-list entries, and summary semantics after MPO reload.

## Verification and limitations

- `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MalformedExternalImporterTest" --console=plain` — PASS, 3 tests (two malformed imports and valid empty Planner).
- `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.MpxIndependentFixtureImportTest" --console=plain` — PASS, 3 tests; `PlannerIndependentFixtureImportTest` — PASS, 1; `RegisteredExternalWriterTest` plus `ProjectWriterFactoryTest` — PASS, 4; `XlsxSupportTest` — PASS, 22 (recorded in slice evidence before this final invalid-input slice).
- `.\gradlew.bat :microproject_exchange:cleanTest :microproject_exchange:test --no-daemon --max-workers=1 --console=plain` — PASS, 365 tests, 0 failures/errors; sequential clean rerun after an earlier overlapping invocation ended in `java.io.EOFException` while reading test results. `git diff --check` — PASS.
- Remaining limits: bounded fixture field coverage only; MPX/Planner writers are tested via format readback, not a claim of full interoperability; MPX valid empty-project semantics are not asserted. PMXML/JSON/SDEF versions, semantics, and read paths remain unverified/absent as shown above. Physical MSP was not used. POD wire format and product UI formats were not changed.
