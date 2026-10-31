/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #979: name-cell and other spreadsheet editors share one previous-table
 * record, and the name editor renders through the same setup as the renderer.
 */
class SpreadSheetEditorTableTrackingTest {
	private static final int NAME_COLUMN = 1;

	@Test
	void movingFromNameEditorToOtherColumnEditorInAnotherTableStopsThePreviousEdit() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet a = sheet("A");
			SpreadSheet b = sheet("B");
			int other = nonNameEditableColumn(b);

			assertTrue(edit(a, 0, NAME_COLUMN));
			assertTrue(a.isEditing());
			assertTrue(edit(b, 0, other));

			assertFalse(a.isEditing(), "the name editor in the previous table must be stopped");
			assertTrue(b.isEditing());
		});
	}

	@Test
	void movingFromOtherColumnEditorToNameEditorInAnotherTableStopsThePreviousEdit() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet a = sheet("A");
			SpreadSheet b = sheet("B");
			int other = nonNameEditableColumn(a);

			assertTrue(edit(a, 0, other));
			assertTrue(a.isEditing());
			assertTrue(edit(b, 0, NAME_COLUMN));

			assertFalse(a.isEditing(), "the non-name editor in the previous table must be stopped");
			assertTrue(b.isEditing());
		});
	}

	@Test
	void nameEditorAppliesTheSameTaskTextStyleAsTheRenderer() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet a = sheet("A");
			NormalTask task = (NormalTask) ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) a.getModel()).getNode(0).getNode().getImpl();
			task.setFontBold(true);
			task.setFontSize(19);

			Font rendered = NameCellComponent.getComponent(a, "A task", true, false, 0, NAME_COLUMN)
				.getTextComponent().getFont();
			assertTrue(edit(a, 0, NAME_COLUMN));
			Font editing = ((NameCellComponent) a.getEditorComponent()).getTextComponent().getFont();

			assertEquals(19, editing.getSize());
			assertTrue(editing.isBold());
			assertEquals(rendered.getFamily(), editing.getFamily());
			assertEquals(rendered.getSize(), editing.getSize());
			assertEquals(rendered.getStyle(), editing.getStyle());
		});
	}

	private static boolean edit(SpreadSheet sheet, int row, int column) {
		sheet.setRowSelectionInterval(row, row);
		sheet.setColumnSelectionInterval(column, column);
		return sheet.editCellAt(row, column,
			new MouseEvent(sheet, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 2, false));
	}

	/** First non-name column whose cell can be edited in place. */
	private static int nonNameEditableColumn(SpreadSheet sheet) {
		for (int column = NAME_COLUMN + 1; column < sheet.getColumnCount(); column++) {
			if (sheet.isCellEditable(0, column)
				&& !(sheet.getCellEditor(0, column) instanceof SpreadSheetNameCellEditor))
				return column;
		}
		throw new AssertionError("no editable non-name column");
	}

	private static SpreadSheet sheet(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("tracking-" + name, undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Task " + name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);

		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			"tracking-" + name, null);
		SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry", true);
		return sheet;
	}
}
