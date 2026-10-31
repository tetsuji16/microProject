# Issue #981 - one saved transition, one dirty rollback, one window title

State: IN PROGRESS (implemented, awaiting full verification and review)
Issue: https://github.com/tetsuji16/microProject/issues/981
Contract frozen at: coordinator brief for #981
Baseline HEAD: `25c41fd32`
Baseline working tree: clean (worktree microProject-w981, branch claude/issue-981)

## Goal and scope

Single `Project` API for the saved transition and for dirty-state rollback, the
linked MPO child save uses the same confirmation as LocalSession, and one
window-title builder.

Out of scope: RespectLinksService / TaskInactivationService / DependencyService
skeletons (#980); ProjectMergeService:605 (conditional merge-restore of the
group flag, different responsibility, left as is); Project.postDeserialization
(load baseline, not a user save); CalendarCatalog / Portfolio flags.
Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | `Project.markSaved()` and `captureDirtyState()`/`restoreDirtyState()`; LocalSession, GraphicManager (child save, discard reload), ProjectTaskSnapshot, ScheduleService, TaskFormatPainterService use them; after rollback `needsSaving()` equals the pre-edit value | pass | ProjectDirtyStateTest, ScheduleServiceSplitTest, TaskFormatPainterServiceTest |
| A2 | Saving a linked MPO child does not report later edits as saved | pass | MpoTransactionBoundaryTest.linkedChildSaveMarksSavedOnlyWhenNoEditRacedTheExport |
| T | One title builder `DocumentTitles.windowTitle(project, fallback, isSaving)` used by GraphicManager.setTitle and DefaultFrameManager.describeWindow | pass | DocumentTitlesTest |
| D | Dirty-table question answered | see below | ProjectDirtyStateTest note |

## Design notes

- Linked MPO child: `ExportPort.ExportRequest(target, markSaved)`; the adapter
  calls `importer.confirmSavedState()` (the #976 check) after `exportFile()` and
  only then `project.markSaved()`; otherwise it returns `EXPORT_CHANGED_DURING_SAVE`
  and the project stays modified. `ProjectPortCoordinator.saveProject` is the entry.
  No Swing job is scheduled, so the EDT path is unchanged. The non-MPO branch
  uses the generic save job (LocalSession end job) and then checks `needsSaving()`.
- Title: `GraphicManager.refreshSaveStatus` called `setTitle(isSaving)` and then
  `FrameManager.update()`, which re-titled every window via describeWindow with
  the `*` regardless of saving, so the saving title was overwritten. The order is
  now update() first, then setTitle(isSaving). Other windows (not current) still show
  `*` while saving: no saving state is known for them.
- `restoreDirtyState` writes isDirty directly and uses `setGroupDirty` so group
  listeners fire as before.

## Dirty-table finding (D)

- Only `Project.needsSaving()` reads a group flag. `AvailabilityTable` and
  `CostRateTable` `isGroupDirty` are never read; they are set only by
  DefaultNodeModel structural row operations (add/relocate) and nothing consumes them.
- A real-route probe (`Field.setValue` of Field.maximumUnits on the resource's
  availability row, then `project.needsSaving()`) returned false: Availability and
  CostRate rows are not DataObjects and not BelongsToDocument, and the tables have
  no back-reference to a resource or pool. So table edits made through the generic
  Field route do not by themselves mark the project modified. Whether the Resource
  dialog marks the resource dirty on OK was not verified (needs a GUI run).
- Not fixed: a fix needs an owner link on the tables (design decision, serialized
  classes). The flags cannot be removed: they are non-transient fields of POD
  serialized classes (wire format immutable).

## Latest evidence

- core+application+exchange+ui test tasks: BUILD SUCCESSFUL (385 exchange tests, 0 failed); guiTest compiles.
- Physical GUI: pending (not run locally; shared desktop). Window title during save
  and linked-child refresh-with-save need physical confirmation.
