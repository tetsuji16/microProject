# Issue #984 - Local consolidation and small fixes

State: IN PROGRESS (branch claude/issue-984, not pushed)
Issue: https://github.com/tetsuji16/ProjectLibre/issues/984
Contract frozen at: issue body of 2026-10-31 plus extra item g from #981
Baseline HEAD: `cafba14fe`
Baseline working tree: clean

## Goal and scope

Judge every listed item as consolidated or a distinct responsibility; fix confirmed bugs with a regression test.

Out of scope: MpoFileImporter, MSPDISerializer, MpoArchiveBudgetValidator (#982); no Robot/guiTest runs.
Blockers: none

## Acceptance

| ID | Item | Verdict and reason |
|---|---|---|
| a1 | NodeFilter/NodeSorter/CommonTransformFactory six options | consolidated: Fields and 12 accessors now live once in CommonTransform; NodeTransformer keeps its fixed overrides (distinct semantics). |
| a2 | ActionLists/CellStyles | consolidated: FormulaCatalog base; FormulaCatalogConfigurationTest loads the real view.xml to prove digester id/name/add* binding. |
| b1 | Display name DefaultSubprojectHandler:222 / GraphicManager:645 | distinct: Same name-then-file order but different fallback text for cycle diagnostics vs a user message; save prompt is file-first. Short private helpers kept. |
| b2 | Action lookup GraphicManager / PrintPreviewFrame | consolidated: MenuActionsMap.requireAction; messages unchanged. |
| b3 | usability bundle references | consolidated: BaselineDialog, DocumentFrame, PopupDialogSupport use UsabilityStrings (text with fallback overload). |
| c1 | DurationFormat 216/251 | consolidated: formattedValue shared by format and formatCompact; the unit/label arguments differ and stay. |
| c2 | AssignmentDetail split span | consolidated: splitSpan shared by splitForLeveling and applySplit. |
| c3 | FieldUtil 67-115 | consolidated: Four copies of the series expansion became one loop over separators and sides. |
| c4 | LocaleDialog 652-698 | consolidated: collectImportedFiles for client and menu files. |
| c5 | PageSetup 983-1070 | consolidated: Paper, size, orientation and margin panels take their column spec; layouts stay different. |
| c6 | TaskCommandGateway | already consolidated: Reworked and merged by #978; not touched. |
| c7 | TaskVisibilityService 32/78 | consolidated: setHidden shared by hide and show. |
| c8 | RollupSpanCalculator 48/63 | consolidated: childBounds shared by task and project rollups. |
| c9 | TaskProgressService 44/79 | consolidated: leafTasks selection shared; the target functions differ and stay. |
| c10 | DefaultFrameManager 331/369 | consolidated: gridFor; cascade stays separate (panel vs window bounds). |
| c11 | MPXConverter 272/284 | consolidated: ProjectCalendarException extends ProjectCalendarHours, one addRanges. |
| c12 | MutableNodeHierarchy 129/227 | distinct: The second path records every attached node in the rollback journal; padding loops differ by that side effect. |
| c13 | CustomReportDialogBox 283/292 | consolidated: showPreview(card). |
| c14 | IndicatorsRenderer 81/96 | consolidated: emptyCell. |
| c15 | AssignmentEntryPane / TimesheetEntryPane | consolidated: DialogEntrySpreadSheet base for the editor-button overrides. |
| c16 | TeamPlannerDialogBox / TimelineDialogBox | consolidated: PopupDialogSupport.detachContentPanel. |
| c17 | DelegateTaskDialog / UpdateTaskDialog | distinct: Only a 3-line task-name label is shared; layouts and fields differ. Kept short duplicate. |
| c18 | CriticalChainService 368/412 | consolidated: levelingOptions(Settings) replaces three identical constructions. |
| c19 | PercentWorkCompleteService 93-132 | consolidated: resolveCutoff(forward or rollback) replaces two binary searches. |
| c20 | AvailabilityTable 94 / CostRateTable 64 | distinct: isGroupDirty is a non-transient serialized field of both; moving it to the base would change the serialized form. Kept in place. |
| c21 | BarFormat 284 / FormBox 69 | fixed bug (latent): Reference comparison field.getId()!=fieldId made the field re-resolve on every call; now Objects.equals. Behaviour-neutral, so no focused test. |
| c22 | Project 3326 / DurationDefaults 55 | consolidated: DurationDefaults.withCalendar. |
| d1 | Slot validation Exception/WorkWeek editors | consolidated: CalendarEditorSupport.workingHours; CalendarEditorSupportWorkingHoursTest. |
| d2 | CalendarDialogBox accepts 0, DurationDefaults throws | fixed bug: Confirmed: spinner minimum 0 vs positive-only record. Product rule (no MSP source consulted): minimum 0.01 (the editor's precision); a stored 0 is lifted. |
| e | BootstrapApplet reflection GraphicManager 285/371 | linked #738: Class exists only under src/test; removing the branches also removes GraphicManagerLinkRouteTest coverage and belongs to the GraphicManager decomposition. |
| f | FieldAliasDialog:112 | fixed bug: result.equals(oldName) compared a String with a JLabel (never equal); now compares the label text. The dialog needs a display, so no headless test. |
| g | ResourceInformationDialog table edits | fixed bug: Confirmed by ResourceTableEditDirtyTest (failed before): edits never reached the project. SpreadSheetModel commit listener marks the resource and pool group dirty. |

## Known gaps (proposed issues, not created)

- Resource table row delete/cut and Undo/Redo in the Resource Information sheets do not mark the project dirty (the commit listener covers typed, pasted and cleared cells only): "Resource Information: mark project dirty on availability/cost-rate row delete and undo".

## Latest evidence

- Tests/build: see final report of the delegating session (module tests, `:microproject_ui:compileGuiTestJava`).
- Physical GUI/manual: pending. Open Resource Information, edit an availability and a cost-rate cell, close the project and confirm the save prompt; Rename Field with an unchanged name.
