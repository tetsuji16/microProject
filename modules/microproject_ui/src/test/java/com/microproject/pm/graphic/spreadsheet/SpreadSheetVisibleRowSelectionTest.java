/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class SpreadSheetVisibleRowSelectionTest {
	/**
	 * Microsoft's Task Inspector guidance follows selected tasks and offers
	 * predecessor navigation. This Office-consistent contract resolves stable
	 * task identity through the current filtered/sorted visible row.
	 * Source: https://support.microsoft.com/en-us/project/view-and-track-scheduling-factors
	 */
	@Test
	void restoresStableTaskIdentityThroughSortedAndFilteredViewRows() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("visible-row-selection", undo), undo);
		project.initialize(false, false);
		NormalTask zulu = project.createScriptedTask();
		zulu.setName("Zulu task");
		NormalTask alpha = project.createScriptedTask();
		alpha.setName("Alpha task");
		NormalTask echo = project.createScriptedTask();
		echo.setName("Echo task");
		Node zuluNode = findNode(project.getTaskModel().getRoot(), zulu);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "inspector-visible-row", null);
		cache.update();
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.summary", true);
			sheetRef[0] = sheet;
		});
		SpreadSheet sheet = sheetRef[0];
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
			int nameColumn = -1;
			for (int column = 0; column < model.getColumnCount(); column++) {
				if ("Field.name".equals(model.getFieldInColumn(column).getId())) nameColumn = column;
			}
			assertTrue(nameColumn >= 0, "task table must expose its stable Name field");
			TableRowSorter<CommonSpreadSheetModel> sorter = new TableRowSorter<>(model);
			sheet.setRowSorter(sorter);
			sorter.setSortKeys(List.of(new RowSorter.SortKey(nameColumn, SortOrder.ASCENDING)));
			sorter.sort();
			int modelRow = sheet.nodesToRows(List.of(zuluNode))[0];
			int viewRow = sheet.taskNodeToViewRow(zuluNode);
			assertTrue(viewRow >= 0);
			assertTrue(viewRow != modelRow, "fixture must distinguish view coordinates from model coordinates");
			sheet.restoreTaskRowSelection(List.of(zuluNode));
			assertEquals(viewRow, sheet.getSelectedRow());
			assertEquals(modelRow, sheet.convertRowIndexToModel(sheet.getSelectedRow()));

			int finalNameColumn = nameColumn;
			sorter.setRowFilter(new RowFilter<>() {
				@Override public boolean include(Entry<? extends CommonSpreadSheetModel, ? extends Integer> entry) {
					return !"Zulu task".equals(String.valueOf(entry.getValue(finalNameColumn)));
				}
			});
			assertEquals(-1, sheet.taskNodeToViewRow(zuluNode), "a filtered task has no visible view row");
			sheet.restoreTaskRowSelection(List.of(zuluNode));
			assertEquals(-1, sheet.getSelectedRow(), "filtered predecessor must not map onto an unrelated visible row");
		});
	}

	private static Node findNode(Object candidate, Object task) {
		if (!(candidate instanceof Node node)) return null;
		if (node.getImpl() == task) return node;
		for (Object child : node.getChildren()) {
			Node found = findNode(child, task);
			if (found != null) return found;
		}
		return null;
	}
}
