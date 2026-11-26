# Issue #1374 — core の JSON の読み書きを Jackson 3 に移す

State: IN REVIEW (PR #1413)
Issue: https://github.com/tetsuji16/microProject/issues/1374
Contract frozen at: 2026-11-26, issue body as fetched
Baseline HEAD: `5cfbe1053`
Baseline working tree: clean

## Goal and scope

Migrate `microproject_core` module's JSON reading/writing from Jackson 2.x (`com.fasterxml.jackson.core:jackson-databind:2.23.0`) to Jackson 3.x (`tools.jackson.core:jackson-databind:3.1.7`). Only `microproject_core` is in scope; `microproject_exchange` migration is issue #1375.

Out of scope:
- `microproject_exchange` migration (#1375)
- JSON format changes (key names, ordering, schema version)
- Removing Jackson 2.x dependency from other modules (still used by exchange)

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| AC1 | `rg -n "com\.fasterxml\.jackson\.(core|databind)" modules/microproject_core` returns 0 matches | pass | `git grep -nE` returns 0 matches |
| AC2 | `rg -n "new ObjectMapper\(" modules/microproject_core/src/main` returns 0 matches; all mappers created via `JsonMapper.builderWithJackson2Defaults()` | pass | `git grep -nE` returns 0 matches; both mappers in `CollaborationMetadataStore.java:60` and `OperationLog.java:30` use `JsonMapper.builderWithJackson2Defaults()` |
| AC3 | Malformed JSON, duplicate keys, trailing tokens all throw `IOException` (tests exist) | pass | `OperationLogTest`: `malformedJsonBecomesIoException`, `duplicateJsonKeysBecomeIoException`, `trailingTokensBecomeIoException`, `jsonlInvalidInputsAndUnserializablePayloadsBecomeIoException` |
| AC4 | `OperationLog` and `CollaborationMetadataStore` produce identical byte output before/after migration (tests exist) | pass | `OperationLogTest.writeKeepsTheEstablishedBytes`, `OperationLogTest.writeJsonlKeepsTheEstablishedBytes`, `CollaborationMetadataStoreTest.savedMetadataKeepsTheEstablishedBytes` |
| AC5 | `:microproject_core:test` and other module tests pass | pass | Verified via Gradle module test runs after final edits |
| AC6 | `verifyDependencyAllowlist` passes | pass | Verified via `./gradlew.bat verifyDependencyAllowlist build` |
| AC7 | PR `build` and `GUI acceptance` checks pass | pending | Pending CI on updated head |
| AC8 | `gradle/verification-metadata.xml` updated with Jackson 3 artifacts | pass | Added `tools.jackson.core:jackson-core:3.1.7`, `tools.jackson.core:jackson-databind:3.1.7`, `com.fasterxml.jackson.core:jackson-annotations:2.21` (transitive) |

## Current slice

**Next slice:** Verify all updated tests and allowlist/build locally, push adversarial review fixes, wait for CI, and merge PR #1413 into `master`.

**Done when:** All acceptance criteria AC1-AC8 satisfied and PR #1413 merged into `master`.

## Remaining blockers

- none
