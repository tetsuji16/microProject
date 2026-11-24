# Issue #1288 Work Packet: PERT Manual Box Placement

## Frozen contract

**Goal:** Allow Network Diagram (PERT) task boxes to be moved manually, persist their positions in MPO, and restore automatic placement with Layout Now. The issue is a child of #1180; multiple selection and alignment commands remain in #1289.

**Scope:** Dragging a box interior moves it; dragging a box edge creates a dependency link. A move and Layout Now each support Undo/Redo. Non-moved boxes retain automatic layout. MPO preserves manual positions; POD's serialized bytes remain unchanged. MSPDI/MPP position export, multi-select, alignment commands, Layout Selection, and Layout Related Tasks are out of scope.

## Current implementation

- Manual positions live in the project's `PertManualPositions` transient document state as task UID → immutable logical-coordinate `Position`, independent of view zoom. They are not fields in `Network.Workspace` and are not Java-serialized.
- `PertLayout` applies overrides after automatic placement and computes bounds from the resulting shapes. Automatic boxes retain their previous geometry.
- `NetworkInteractor` treats a meaningful interior drag as a move and preserves edge drag for link creation. Pointer movement under 2 view pixels is treated as a click and creates no position or Undo edit.
- `MpoSidecarJson` persists the state in `microproject/pert-manual-positions.json`; reloading resolves entries using serialized task UIDs. POD serialization does not include the transient state.
- Move and Layout Now edits refresh the layout and mark the project dirty on initial edit, Undo, and Redo.

## Acceptance and evidence

| ID | Frozen acceptance | Status and evidence |
|---|---|---|
| A1 | Drag a box to move it; one Undo restores its automatic position. | Passed. `NetworkManualPositionUndoTest`; physical drag + Ctrl+Z/Y journey below also covers click-without-drag. |
| A2 | Boxes not manually moved retain automatic positions. | Passed. `PertLayoutGeometryTest.manualPositionOverridesOnlyThatTaskAndAutomaticBoundsStayStable`. |
| A3 | Layout Now returns all boxes to automatic layout and is undoable. | Passed. `NetworkManualPositionUndoTest.moveUndoRedoRefreshesLayoutAndLayoutNowIsUndoable`. |
| A4 | MPO save/reload preserves positions; POD serialization is unchanged. | MPO passed through two save/reload cycles including UID remap. POD test compares the stable Java document object-stream bytes before and after adding transient PERT state; fixed fixture hashes/descriptors also pass. A full POD comparison also passed: two independently loaded copies of the same migrated fixture produced identical Java stream and embedded MSPDI bytes with and without transient PERT state. Consecutive saves of one instance are not byte-stable independently of PERT state. |
| A5 | Link-creation drag still works. | Passed. Physical box-edge link drag in the targeted Windows GUI journey. |
| A6 | Issue verification commands succeed. | Passed on tested code SHA `eed14c933cf83be3ae00c9b128d07e2c0b5bceae`: local focused tests and full UI/exchange tests; CI Build succeeded on Ubuntu, Windows, and macOS; CI GUI acceptance succeeded. Exact commands and run links below. |

**Original verification commands:**

```powershell
.\gradlew.bat :microproject_ui:test --tests "*PertLayout*" --tests "*Network*" --console=plain
.\gradlew.bat :microproject_exchange:test --tests "*MpoFileImporterTest*" --console=plain
.\gradlew.bat :microproject_ui:test --console=plain
python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json
git diff --check
```

**Recorded evidence:**

- At `aaac8805e28d9957a8cf586e553f4278205db40d`: `verifyArchitectureBoundaries :microproject_ui:test :microproject_exchange:test` passed; focused geometry, move/Layout Now undo, MPO repeated-roundtrip, POD-byte, and fixed-fixture tests passed. `git diff --check` passed.
- At `eed14c933cf83be3ae00c9b128d07e2c0b5bceae`: `:microproject_core:test --tests "*JapaneseClientBundleTest"` and the focused physical journey passed locally in one Gradle invocation. The journey covers selection click without drag, physical interior drag, Ctrl+Z, Ctrl+Y, then physical edge drag to create a link.
- [Targeted Windows GUI run 45164095361](https://github.com/tetsuji16/microProject/actions/runs/45164095361): passed, 1 test, 0 failures; exact journey `robotMovesPertBoxUndoRedoAndCreatesLinkFromBoxEdge`, Japanese locale, scale 1.0. This is focused evidence, not full GUI coverage.
- [Build run 45164093200](https://github.com/tetsuji16/microProject/actions/runs/45164093200): required Build passed on Ubuntu, Windows, and macOS, including the source-inventory and enabled module tests.
- [GUI acceptance run 45164092845](https://github.com/tetsuji16/microProject/actions/runs/45164092845): required GUI acceptance passed.

## Diagnosed failures and fixes

- [Build run 45161209067](https://github.com/tetsuji16/microProject/actions/runs/45161209067) failed on all OSes because `client_ja.properties` lacked `Network.MoveBox.title`. The Japanese translation was added in `eed14c933` and `JapaneseClientBundleTest` passed afterward.
- [GUI run 45161212673](https://github.com/tetsuji16/microProject/actions/runs/45161212673) failed because a selection click on an already-selected box created a zero-distance manual override. Undo then correctly restored that click-created override, leaving a position after Undo. `NetworkInteractor` now ignores movement below 2 pixels; the focused physical journey passed locally and in run 45164095361.

## Remaining state / next slice

The tested implementation SHA `eed14c933` has successful required Build and GUI acceptance checks. POD evidence includes the full emitted Java stream and embedded MSPDI comparison from independent copies of the same fixture. This packet update is documentation-only and has not been pushed or tested as a new PR head. After this commit is pushed, verify the current PR head and required checks before marking the issue or PR complete. Keep the PR draft until required checks and review requirements are satisfied. No acceptance criterion is deferred; the only pending gate is required-check status for the post-documentation PR head.

- At `0f2bc1f21`, `PodRoundTripTest.manualPertDocumentStateDoesNotChangeFullPodFileAfterFixtureMigration`, the document stream comparison, and `podSaveIsByteStableOnRoundTrip` passed.
- At `6e072cd69`, `MpoFileImporterTest` passed including moving and clearing positions after reload and saving again.
