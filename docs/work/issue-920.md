# Issue #920 — frozen POD read and wire contract

State: IMPLEMENTED; PR checks passed
Issue: https://github.com/tetsuji16/microProject/issues/920
Contract frozen at: 2026-10-25 (issue body and frozen finite intake)
Baseline HEAD: `c41af1560f0c83090b38a05f2ae55ba7e26bba10`
Baseline working tree: pre-existing untracked `.codex-remote-attachments/` and sample sync sidecars; preserved.

## Goal and scope

Preserve fixed POD imports and immutable serialized wire contract; prevent ordinary user saves from writing POD, while retaining internal recovery snapshot serialization and historical recovery.pod reading.

Out of scope: POD schema/wire changes, new POD write behavior, broad serializer deletion, unrelated GUI/product work.
Blockers: none. #919 is closed and PR #927 merged before this baseline.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Pin provenance/hash/version/descriptors and literal semantic manifests for three fixed POD fixtures. | passed | fixed hashes/version/descriptor + round-trip manifest tests on `d971cf201e145b0aaa528e5767785ba533e29a8a`; PR CI below |
| A2 | Existing import and selected MPO save/reload semantics; malformed/missing/unsafe rejection without partial publication or source mutation, retaining XML recovery. | passed | importer/round-trip tests and physical XML-recovery Robot on `d971cf201e145b0aaa528e5767785ba533e29a8a`; PR CI below |
| A3 | No field/layout/order/identifier/version/wire/alias expansion; ordinary Save/chooser/user dispatch cannot write POD; recovery snapshots/read remain. | passed | focused policy tests and physical MPP/POD Save As Robot on `d971cf201e145b0aaa528e5767785ba533e29a8a`; PR CI below |
| A4 | Unsupported fields and exact SHA/commands/results recorded; no original criterion deferred. | passed | limitations and exact commands recorded below; PR checks passed for rebased candidate `bdb53abe8b720cfe955388d763621620da4bb522` |

## Current slice

**Completed:** ordinary Save accepts MPO/XML/XLSX only; POD imports require Save As/MPO; Save chooser excludes POD while Open retains it; policy/workflow and LocalSession central dispatch reject ordinary POD targets before chooser/writer regardless of requested importer. Recovery snapshot `.recovery.pod` write remains covered and passes. The existing physical MPP Save As journey is now shared by MPP and copied `June_1_sample.pod`: both exercise ordinary Save, MPO sibling default, Escape cancellation, overwrite refusal, fresh MPO target success, source preservation, association/dirty-state transitions, and MPO task/assignment semantic reload. The existing scalar XML Save As route still runs after closing the POD-saved project.
**Next slice:** none. PR #928 contains the reviewed #920 change; merge after normal review.

## Remaining blockers

- External/original provenance of the three POD samples is absent from repository history and remains explicitly unknown; repository first-seen provenance and file hashes are pinned below.

## Latest evidence

- Tested SHA: `c41af1560f0c83090b38a05f2ae55ba7e26bba10` (workspace HEAD at slice start; changes are uncommitted).
- Fixture provenance: checked-in samples first appear in available repository history at `54e548039` (2026-07-12 structural refactor); external/original source provenance is not recorded in Git. The 2026-08-08 repository sample update is `d3d14605c`.
- Unsupported/unrepresented POD task fields: `levelingCanSplit` and `levelAssignments` are absent and load as `true` defaults; `recurringTask` and manual-date provenance are absent from the immutable descriptors. These are not asserted as persisted POD semantics. Relevant descriptor exclusions are checked in `MpoFileImporterTest` around its legacy descriptor contract.
- Fixed fixture SHA-256: `June_1_sample.pod` `f286eccf43e661de40e7a21a70d9b87cdfb0b444543a0f3c10649ad235328854`; `Commercial construction project plan.pod` `2d381d8b9cd2ea4e619444e5372b29b20183ba038cc64e595db3a7da762cda42`; `Commercial construction project plan2.pod` `ef3fae19814517d10b225b131c251c6ade27cc9154d5138cf3347479e6b1aab4`.
- Serialized version is pinned as `1.0.0`; wire descriptor inventory remains in `modules/microproject_exchange/src/test/resources/com/microproject/exchange/pod-descriptors.txt` and is checked unchanged by `PodDescriptorInventoryTest`.
- Literal representative semantic manifests pin hierarchy, dates, duration, progress, dependency, and resource assignment in each fixed fixture. Current MPO import→save→reload now checks the same selected fields. In the construction fixture, serialized zero-hour dependency lag (`10 << 57`, payload 0) is normalized by MSPDI to zero milliseconds; assert semantic lag 0, not raw type bits.
- Malformed/truncated/unsafe/missing importer tests assert typed IO failure, no project publication, and unchanged source bytes. `LocalFileImporter` now rethrows the serialized-payload error when its existing XML recovery scan finds no payload.
- Root cause of construction MPO reload failure: `MpxAssignmentConverter.from` restored nonzero assignment units before `initializeImportedBounds`; with task dates present, unit application derived work from the task span and violated the guard's zero-work precondition. Bounds are now initialized first, followed by units and explicit source work/progress. The fixed-fixture repro passes; no guard/assertion was weakened.
- GUI recovery: `RibbonExternalCommandGuiAcceptanceTest.robotOpensPodEmbeddedAndPlainXmlRecoveryFromTheFileRibbon` passed 1/1 on Windows. It imports an invalid serialized prefix with the intact embedded MSPDI trailer, then the same XML as a plain `.pod`; both publish the expected project/task state and preserve source bytes. The initial attempt failed on the second document because both files reused the same historical project identity; closing the first clean project before opening the second fixed the test setup. Superseded failure screenshot and desktop/environment artifacts are retained in `modules/microproject_ui/build/reports/guiTest-artifacts/`.
- Tests: ` .\gradlew.bat :microproject_exchange:test --tests "com.microproject.exchange.PodDescriptorInventoryTest" --tests "com.microproject.exchange.LocalFileImporterTest" --tests "com.microproject.exchange.PodRoundTripTest" --tests "com.microproject.core.pm.exchange.converters.mpx.MpxDependencyConverterLagTest" --console=plain` — PASS, 34 tests (1/4/22/7; zero failures/errors/skips). ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotOpensPodEmbeddedAndPlainXmlRecoveryFromTheFileRibbon" --console=plain --max-workers=1 --no-daemon` — PASS, 1/1, including GUI test compilation.
- Broad checks: ` .\gradlew.bat :microproject_core:test :microproject_application:test :microproject_exchange:test :microproject_ui:test --console=plain --max-workers=1 --no-daemon` — BUILD SUCCESSFUL; report totals core 1183, application 38, exchange 355, UI 1304; failures/errors/skips all zero.
- Save policy tests: ` .\gradlew.bat :microproject_core:test --tests "com.microproject.session.FileHelperEdgeCaseTest" --console=plain` — PASS, 5; ` .\gradlew.bat :microproject_application:test --tests "com.microproject.application.ProjectFilePoliciesTest" --tests "com.microproject.application.ProjectDocumentWorkflowTest" --console=plain` — PASS, 8; ` .\gradlew.bat :microproject_ui:test --tests "com.microproject.util.SwingFileChooserProviderTest" --console=plain` — PASS, 13; ` .\gradlew.bat :microproject_exchange:test --tests "com.microproject.session.LocalSessionSaveTest" --console=plain` — PASS, 4 (includes recovery snapshot POD write and ordinary direct-dispatch rejection).
- Physical Save As evidence: ` .\gradlew.bat :microproject_ui:compileGuiTestJava --console=plain` — PASS; ` .\gradlew.bat :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotSavesImportedMppAsMpoAndCancellationOrOverwriteRefusalKeepsSource" --console=plain --max-workers=1 --no-daemon` — PASS, 1 test (MPP and POD journeys plus scalar XML route), Windows host `EVO-T2S`; test source SHA-256: `RibbonExternalCommandGuiAcceptanceTest.java` `ab82dbf450b9ccc3e4e03ed4bcca3a3c2a72a89aab706b732afedd2ed7ca2deb`.
- Ordinary Save route evidence: the physical Ribbon Save As journey for the imported POD opens MPO-default Save As; cancellation and overwrite refusal preserve the POD source/target bytes and document state; new MPO save and reload retain selected semantics. Physical XML recovery route also passed 1/1 in `robotOpensPodEmbeddedAndPlainXmlRecoveryFromTheFileRibbon`.
- During verification the first LocalSession run found recovery snapshots still entered the ordinary extension/chooser path after POD became import-only. Recovery snapshots now bypass that ordinary-save check; the focused class passes with its existing `.recovery.pod` writer assertion intact. A stale importer alone does not block an allowed MPO target; the central rejection is based on the POD target extension.
- Tested base HEAD: `c41af1560f0c83090b38a05f2ae55ba7e26bba10`; implementation checks passed with only this packet update afterward. No POD fixture bytes or descriptor inventory were changed. Candidate commit SHA and exact-candidate CI result will be recorded after integration.
- Local full-module tests ran on `d971cf201e145b0aaa528e5767785ba533e29a8a`; PR candidate `bdb53abe8b720cfe955388d763621620da4bb522` is the same implementation diff rebased onto latest `master` after #919/#927 landed. No production/test source changed during rebase.
- Exact candidate CI: Build PASS, [run 40177582811](https://github.com/tetsuji16/microProject/actions/runs/40177582811); GUI acceptance PASS, [run 40177582845](https://github.com/tetsuji16/microProject/actions/runs/40177582845), both on `bdb53abe8b720cfe955388d763621620da4bb522`.

## Frozen finite intake

1. Three fixed POD samples: provenance, SHA-256, serialized version/descriptors, literal semantic manifests; import selected represented task hierarchy/dates/duration/progress/dependency/resource/assignment through MPO save/reload; omissions/defaults listed.
2. Reject invalid/truncated/missing/unsafe serialized payload at real importer boundary, preserve legitimate embedded/plain XML recovery, no partial publication/source mutation; reuse allowlist/descriptor/typed failure infrastructure and do not add aliases.
3. Consolidate user save policy for POD → Save As/MPO; remove POD Save chooser filter but retain Open; reject direct user POD save dispatch. Preserve internal recovery snapshots and historical recovery.pod reads.
4. Reuse MPP physical Save As command family for POD: actual Save opens MPO-default Save As; cancellation/overwrite refusal preserves original POD and target bytes, path association/model; successful MPO save/reload. Consolidate shared fixture logic.
5. Record exact tested SHA/commands/counts/hashes/evidence layers/unsupported fields. Run affected module checks and confirm fixed POD bytes/descriptors unchanged.
