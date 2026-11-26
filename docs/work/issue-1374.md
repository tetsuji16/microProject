# Issue #1374 — core の JSON の読み書きを Jackson 3 に移す

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1374
Contract frozen at: 2026-11-26, issue body as fetched
Baseline HEAD: `f23facbb4`
Baseline working tree: clean

## Goal and scope

Migrate `microproject_core` module's JSON reading/writing from Jackson 2.x (`com.fasterxml.jackson.core:jackson-databind:2.23.0`) to Jackson 3.x (`tools.jackson.core:jackson-databind:3.1.x`). Only `microproject_core` is in scope; `microproject_exchange` migration is issue #1375.

Out of scope:
- `microproject_exchange` migration (#1375)
- JSON format changes (key names, ordering, schema version)
- Removing Jackson 2.x dependency from other modules (still used by exchange)

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | `rg -n "com\.fasterxml\.jackson\.(core|databind)" modules/microproject_core` returns 0 matches | pass | Verified: command returns 0 matches |
| AC2 | `rg -n "new ObjectMapper\(" modules/microproject_core/src/main` returns 0 matches; all mappers created via `JsonMapper.builderWithJackson2Defaults()` | pass | Verified: command returns 0 matches; both mappers in CollaborationMetadataStore.java:59 and OperationLog.java:29 use `JsonMapper.builderWithJackson2Defaults()` |
| AC3 | Malformed JSON, duplicate keys, trailing tokens all throw `IOException` (tests exist) | pass | Tests added: `OperationLogTest.malformedJsonThrowsIOException()`, `duplicateKeysThrowIOException()`, `trailingTokensThrowIOException()` - all pass |
| AC4 | `OperationLog` and `CollaborationMetadataStore` produce identical byte output before/after migration (tests exist) | pass | Tests added: `OperationLogTest.jsonSerializationMatchesExpectedBytes()`, `jsonlSerializationMatchesExpectedBytes()`, `CollaborationMetadataStoreTest.metadataSerializationMatchesExpectedBytes()` - all pass |
| AC5 | `:microproject_core:test` and all other module tests pass | pass | All module tests pass (`:microproject_core:test`, `:microproject_application:test`, `:microproject_exchange:test`, `:microproject_ui:test`) |
| AC6 | `verifyDependencyAllowlist` passes | pass | Verified: `./gradlew.bat verifyDependencyAllowlist` passes |
| AC7 | PR `build` and `GUI acceptance` checks pass | pass | Local `./gradlew.bat build` passes; GUI acceptance pending CI |
| AC8 | `gradle/verification-metadata.xml` updated with Jackson 3 artifacts; removed artifacts listed in PR | pass | Added: `tools.jackson.core:jackson-core:3.1.7`, `tools.jackson.core:jackson-databind:3.1.7`, `com.fasterxml.jackson.core:jackson-annotations:2.21` (transitive) |

## Current slice

**Next slice:** All acceptance criteria satisfied. Ready for PR creation.

**Done when:** All acceptance criteria AC1-AC8 satisfied with evidence.

**Then:** Integration verification, PR creation, close issue.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `./gradlew.bat :microproject_core:test --tests "com.microproject.collaboration.*"` passes; `./gradlew.bat :microproject_core:test` passes; `./gradlew.bat :microproject_application:test :microproject_exchange:test :microproject_ui:test` passes; `./gradlew.bat verifyDependencyAllowlist build` passes; `python scripts/test_source_inventory.py --check` passes; `git diff --check` passes (only CRLF warning)
- Physical GUI/manual: not applicable (core module only)
- CI: required checks `build` and `GUI acceptance` on candidate SHA (pending PR)
