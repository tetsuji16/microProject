# Issue #902 — POD serialized descriptor inventory

State: IN PROGRESS — implementation complete; awaiting Sol review/integration
Issue: https://github.com/tetsuji16/microProject/issues/902
Contract frozen at: 2026-10-24, delegated slice request
Baseline HEAD: `0df49b5ef19d059127f128929efc27152e88f753`
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Pin each checked-in POD fixture's serialized class descriptors (FQN, UID and
fields) and keep every fixture in the existing POD roundtrip regression.
POD serialization format and production classes remain unchanged.

Out of scope: deleting `server.data` classes or claiming they are unused by all
user files. Blockers: exact-candidate CI and Sol integration review pending.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | All fixed POD fixture descriptors are recorded; inventory drift fails | pass | `PodDescriptorInventoryTest`; working tree based on `0df49b5` |
| A2 | Existing POD read/roundtrip and MPO prior-release read remain covered | pass | `PodRoundTripTest` (all 3 samples) and `MpoHistoricalFixtureTest` |
| A3 | No claim that fixtures exhaust all user POD files | pass | Scope note below |

## Current slice

**Next slice:** Sol reviews and integrates this bounded change.
**Done when:** reviewed and integrated with exact-head CI evidence.
**Then:** close #902.

## Evidence limits

The inventory detects changes to the serialized descriptor sets in the checked-in
fixed corpus. It does not compare descriptors to the current runtime schema and
does not prove that other users' POD files never contain additional serialized
classes or that `server.data` is unused for all user files. Existing POD read and
roundtrip tests provide the separate compatibility evidence for current code.

## Latest evidence

- Tests: `:microproject_exchange:test --tests "com.microproject.exchange.PodDescriptorInventoryTest" --tests "com.microproject.exchange.PodRoundTripTest.samplePodRoundTripPreservesTaskLayoutAndDependencies" --tests "com.microproject.exchange.MpoHistoricalFixtureTest" --console=plain --no-daemon --max-workers=1` passed.
- Legacy package remap: `:microproject_exchange:test --tests "com.microproject.exchange.PodRoundTripTest.legacyPackagePodFixtureExercisesClassNameRemapping" --console=plain --no-daemon --max-workers=1` passed.
- Inventory audit: `python scripts/test_source_inventory.py --check` completed; generated source inventory includes the added JUnit test.
- CI: pending exact integrated candidate SHA.
