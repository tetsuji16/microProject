# Issue #1288 Work Packet: Network Diagram Align 1/2 - Manual Box Placement and Save

## Issue Contract (frozen at intake)

**Goal**: Enable manual box positioning in Network Diagram (PERT view). Users can drag boxes to new positions; positions persist in MPO; "Layout Now" restores auto-layout.

**Acceptance Criteria** (from issue):
- [ ] Box drag changes position; 1 Undo reverts (test exists)
- [ ] Non-moved boxes keep auto positions (test exists)
- [ ] Layout Now returns all boxes to auto layout (test exists)
- [ ] MPO save/reload preserves positions; POD bytes unchanged (test exists)
- [ ] Link-creation drag still works as before
- [ ] All verification commands pass

**Scope**:
- `Network.Workspace`: add transient `Map<Long, Point2D> manualPositions`
- `PertLayout.updateBounds()`: overlay manual positions after auto layout
- `NetworkInteractor`/`PertInteractor`: distinguish box move (inside shape) from link creation (outside)
- `MpoSidecarJson`: new entry `microproject/pert-manual-positions.json` with taskUid→{x,y}
- `ACTION_NETWORK_LAYOUT_NOW`: clears manualPositions
- Tests: `PertLayoutGeometryTest` additions, MPO round-trip test

**Out of Scope** (per issue):
- Multi-select, align commands, NetworkDiagramAlignMenu ribbon (#1289)
- "Layout Selection", "Layout Related Tasks" changes
- MSPDI/MPP box position export

## Baseline
- Commit: `c31d465f39cf5320bad7bf2c4ba463b8f8741934` (origin/master)
- Branch: `issue/1288-network-diagram-manual`

## Files to Modify
1. `modules/microproject_ui/src/main/java/com/microproject/pm/graphic/network/Network.java` — Workspace class
2. `modules/microproject_ui/src/main/java/com/microproject/pm/graphic/pert/PertLayout.java` — updateBounds()
3. `modules/microproject_ui/src/main/java/com/microproject/pm/graphic/network/NetworkInteractor.java` — drag handling
4. `modules/microproject_ui/src/main/java/com/microproject/pm/graphic/pert/PertInteractor.java` — link creation logic
5. `modules/microproject_exchange/src/main/java/com/microproject/exchange/MpoFileImporter.java` — new entry constant
6. `modules/microproject_exchange/src/main/java/com/microproject/exchange/MpoSidecarJson.java` — serialize/restore manual positions
7. `modules/microproject_ui/src/test/java/com/microproject/offline_graphics/PertLayoutGeometryTest.java` — unit tests

## Verification Commands
```powershell
.\gradlew.bat :microproject_ui:test --tests "*PertLayout*" --tests "*Network*" --console=plain
.\gradlew.bat :microproject_exchange:test --tests "*MpoFileImporterTest*" --console=plain
.\gradlew.bat :microproject_ui:test --console=plain
python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json
git diff --check
```

## Next Slice
Implement manual position storage in `Network.Workspace` and MPO sidecar serialization.