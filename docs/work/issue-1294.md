# Issue #1294 Work Packet: Assignment Information Dialog

## Issue Reference
- **Issue**: #1294 - Task Usage と Resource Usage から割り当て情報のダイアログを開いて、単位、作業の配分、メモを編集する
- **Branch**: `issue/1294-assignment-info-dialog`
- **Base commit**: `c916bbf05` (origin/master)
- **Priority**: medium
- **Labels**: enhancement, area: ui, area: msp-compat, status: ready

## Frozen Acceptance Contract (from issue body)

### Overview
Microsoft Project では、Task Usage と Resource Usage の表で割り当ての行を選び、書式タブの「Information」（割り当て情報）を押すと、割り当て情報のダイアログが開く。microProject の master には、このコマンドもダイアログも無い。割り当ての単位、作業の配分、メモを変えるには、別の表の列を探して直接打つしかない。

### Work Steps (from issue)
1. `RibbonCommandCatalog.java` に `RibbonAssignmentInformation` を登録する。置き場所は Task Usage と Resource Usage の書式タブ（`TASK_USAGE_FORMAT`、`RESOURCE_USAGE_FORMAT`）の Assignment グループ。`menuInternal.properties` に動作とアイコンを、`menu.properties` と `menu_ja.properties` に名前と画面のヒントを足す。リボンの順番の表（`msp-ribbon-order.csv`、`msp-ribbon-spec.csv`、`msp-ribbon-control-spec.csv`）も合わせる。
2. 割り当ての行が選ばれていないときは、コマンドを無効にする。
3. `AssignmentInformationDialog.java` を作る。General に、タスク名と資源名（読み取り専用）、作業、開始、終了、コスト（読み取り専用）と、単位（`Assignment.getUnits`）、作業の配分（`Assignment.getWorkContour`）の編集欄を置く。Notes に、平文のメモ（`Assignment.getNotes` / `setNotes`）を置く。ダイアログの部品は `AbstractDialog` と `PopupDialogSupport` を使い、`JOptionPane` を直接使わない。
4. OK で、変更を 1 回の編集の取引にまとめて反映し、1 回の元に戻す（Undo）で全部が戻るようにする。Cancel では何も変えない。読み取り専用のプロジェクトでは、編集欄を無効にする。
5. 試験を足す。ダイアログの単体の試験（値の読み込み、OK での反映と 1 回の Undo、Cancel、読み取り専用）と、リボンの物理的な経路を通る `@GuiJourney` の試験を 1 件。

### Verification Commands
```powershell
.\gradlew.bat :microproject_ui:test --tests "*AssignmentInformation*" --console=plain
.\gradlew.bat :microproject_ui:test --console=plain
python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json
```

### Completion Criteria (checkboxes)
- [ ] Task Usage と Resource Usage の書式タブに `RibbonAssignmentInformation` があり、割り当ての行を選ぶと有効、選ばないと無効になる。
- [ ] ダイアログで単位、作業の配分、メモを変えて OK を押すと、表の値が変わり、1 回の Undo で全部が元に戻る。
- [ ] Cancel では値が変わらない。読み取り専用のプロジェクトでは編集できない。
- [ ] 変えたメモと単位が、MPO に保存して開き直しても残る（保存の仕組みは #1198 のものを使う）。
- [ ] `MspRibbonOrderDiffTest` と `RibbonStructureTest` が通る。
- [ ] 足した文言が、日本語と英語の両方にある。
- [ ] `build` と `GUI acceptance` が通る。

### Out of Scope
- Tracking タブ（実績の作業、残りの作業、実績の開始と終了）。General と Notes が入ったあとで別の issue にする。
- 書式付きのメモ（リッチテキスト）と、その MPO・MSPDI での保存。
- 割り当ての行をダブルクリックして開く経路。
- 予約の種類（Booking type）など、 Project Server に関係する欄。

## Current State Analysis (master c916bbf05)

### Files that need changes:
1. `modules/microproject_ui/src/main/java/com/microproject/ui/ribbon/RibbonCommandCatalog.java` - Add RibbonAssignmentInformation placement
2. `modules/microproject_ui/src/main/resources/com/microproject/menu/menuInternal.properties` - Add command definition, icon, action, activation, band placement
3. `modules/microproject_ui/src/main/resources/com/microproject/menu/menu.properties` - English strings
4. `modules/microproject_ui/src/main/resources/com/microproject/menu/menu_ja.properties` - Japanese strings
5. `modules/microproject_ui/src/test/resources/com/microproject/ui/ribbon/msp-ribbon-order.csv` - Ribbon order
6. `modules/microproject_ui/src/test/resources/com/microproject/ui/ribbon/msp-ribbon-spec.csv` - Ribbon spec
7. `modules/microproject_ui/src/test/resources/com/microproject/ui/ribbon/msp-ribbon-control-spec.csv` - Control spec

### New files to create:
1. `modules/microproject_ui/src/main/java/com/microproject/dialog/AssignmentInformationDialog.java` - Dialog implementation
2. `modules/microproject_ui/src/main/java/com/microproject/dialog/AssignmentInformationEditService.java` - Edit service
3. `modules/microproject_ui/src/test/java/com/microproject/dialog/AssignmentInformationDialogTest.java` - Unit test
4. `modules/microproject_ui/src/guiTest/java/com/microproject/dialog/AssignmentInformationGuiAcceptanceTest.java` - GUI acceptance test

### Reference implementation (from draft branch `codex/issue-1180-assignment-info-screen`, commit 11c9c9fb8)
The draft implementation exists in commits:
- 11c9c9fb8: Ribbon command registration
- (implied): AssignmentInformationDialog and AssignmentInformationEditService
- 704984f62: GUI acceptance test (Robot-based)
- 7c7ebe41b: GUI test cases documentation

### Key implementation details from draft:
- Dialog uses `AbstractDialog` + `PopupDialogSupport`
- Fields: units (JSpinner 0-10000%), work contour (JComboBox of ContourTypes.Kind), notes (JTextPane with RTF support)
- Read-only fields: task name, resource name, work, start, finish, cost
- Edit service uses `AssignmentDetailStateEdit` for single Undo transaction
- Validation: units 0-100%, contour required
- Icon: `ribbon.assignResources`
- Action: `RibbonAssignmentInformationAction`
- Activation: DIALOG

## Next Slice
**Implement all changes in one slice** - the issue is scoped to a single PR.

### Definition of Done:
All acceptance criteria verified with test commands passing.

### Remaining Blockers:
None identified.

## Test Evidence Tracking
| Criterion | Test Command | Status | Evidence |
|-----------|-------------|--------|----------|
| Ribbon command registered | `MspRibbonOrderDiffTest`, `RibbonStructureTest` | Pending | |
| Dialog opens for selected assignment | `AssignmentInformationGuiAcceptanceTest` | Pending | |
| Units/contour/notes edit + OK + Undo | `AssignmentInformationDialogTest`, `AssignmentInformationGuiAcceptanceTest` | Pending | |
| Cancel no-op | `AssignmentInformationDialogTest` | Pending | |
| Read-only project disables edits | `AssignmentInformationDialogTest` | Pending | |
| MPO persistence | Manual + existing MPO tests | Pending | |
| Japanese/English strings | `test_source_inventory.py` | Pending | |
| Build + GUI acceptance | `gradlew build`, GUI workflow | Pending | |