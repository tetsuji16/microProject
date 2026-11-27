/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

import com.microproject.node.Node;
import com.microproject.model.task.Task;
import com.microproject.ui.view.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.ui.view.spreadsheet.renderer.NameCellComponent;

/** Select All covers task rows only; the trailing blank rows are never selected. */
class SpreadSheetVoidRowSelectionTest {
	@Test
	void selectAllOnEmptyProjectSelectsNothing() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			assertTrue(fx.rowCount() > 0, "the empty project still shows blank rows");
			AtomicReference<List<Node>> nodes = new AtomicReference<>();
			AtomicInteger rows = new AtomicInteger(-1);
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				sheet.selectAll();
				nodes.set(sheet.getSelectedNodes());
				rows.set(sheet.getSelectedRowCount());
			});
			assertTrue(nodes.get().isEmpty(), "selected nodes: " + nodes.get().size());
			assertEquals(0, rows.get());
		}
	}

	@Test
	void selectAllSelectsOnlyTaskRows() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second", "Third")) {
			AtomicReference<List<Node>> nodes = new AtomicReference<>();
			AtomicInteger rows = new AtomicInteger(-1);
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				sheet.selectAll();
				nodes.set(sheet.getSelectedNodes());
				rows.set(sheet.getSelectedRowCount());
			});
			assertEquals(3, rows.get());
			assertEquals(3, nodes.get().size());
			for (Node node : nodes.get()) {
				assertFalse(node.isVoid());
				assertInstanceOf(Task.class, node.getImpl());
			}
		}
	}

	@Test
	void selectAllUsesVisibleRowsWhenRowsAreSorted() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Alpha", "Zulu")) {
			AtomicReference<List<Integer>> selectedRows = new AtomicReference<>();
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
				int nameColumn = -1;
				for (int column = 0; column < model.getColumnCount(); column++) {
					if ("Field.name".equals(model.getFieldInColumn(column).getId())) nameColumn = column;
				}
				assertTrue(nameColumn >= 0);
				final int sortColumn = nameColumn;
				TableRowSorter<CommonSpreadSheetModel> sorter = new TableRowSorter<>(model);
				sheet.setRowSorter(sorter);
				sorter.setSortKeys(List.of(new RowSorter.SortKey(sortColumn, SortOrder.ASCENDING)));
				sorter.sort();
				List<Integer> expectedTaskRows = new java.util.ArrayList<>();
				boolean hasRemappedTaskRow = false;
				for (int viewRow = 0; viewRow < sheet.getRowCount(); viewRow++) {
					int modelRow = sheet.convertRowIndexToModel(viewRow);
					var node = model.getNode(modelRow);
					if (node != null && !node.isVoid()) {
						expectedTaskRows.add(viewRow);
						hasRemappedTaskRow |= modelRow != viewRow;
					}
				}
				assertTrue(hasRemappedTaskRow, "fixture must make visible and model rows differ");
				sheet.selectAll();
				selectedRows.set(java.util.Arrays.stream(sheet.getSelectedRows()).boxed().toList());
				assertEquals(expectedTaskRows, selectedRows.get());
			});
		}
	}

	@Test
	void selectAllResolvesFilteredModelRowsBeyondTheViewCount() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Alpha", "Bravo", "Zulu")) {
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
				int nameColumn = -1;
				for (int column = 0; column < model.getColumnCount(); column++) {
					if ("Field.name".equals(model.getFieldInColumn(column).getId())) nameColumn = column;
				}
				assertTrue(nameColumn >= 0);
				final int filterColumn = nameColumn;
				TableRowSorter<CommonSpreadSheetModel> sorter = new TableRowSorter<>(model);
				sheet.setRowSorter(sorter);
				sorter.setRowFilter(new javax.swing.RowFilter<>() {
					@Override public boolean include(Entry<? extends CommonSpreadSheetModel, ? extends Integer> entry) {
						return "Zulu".equals(String.valueOf(entry.getValue(filterColumn)));
					}
				});
				sorter.sort();
				assertEquals(1, sorter.getViewRowCount());
				int modelRow = sheet.convertRowIndexToModel(0);
				assertEquals(2, modelRow);
				assertTrue(modelRow >= sorter.getViewRowCount(),
					"the visible task's model row must be outside the filtered view-row range");

				sheet.selectAll();
				assertEquals(List.of(0), java.util.Arrays.stream(sheet.getSelectedRows()).boxed().toList());
				assertEquals(fx.task(2), model.getNode(sheet.convertRowIndexToModel(sheet.getSelectedRow())).getNode().getImpl(),
					"Select All must keep the visible Zulu task selected even though its model row exceeds view count");
			});
		}
	}

	@Test
	void selectAllCommitsTheActiveCellEdit() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("Before")) {
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				CommonSpreadSheetModel model = (CommonSpreadSheetModel) sheet.getModel();
				int taskRow = -1;
				for (int modelRow = 0; modelRow < model.getRowCount(); modelRow++) {
					var node = model.getNode(modelRow);
					if (node != null && node.getNode().getImpl() == fx.task(0)) {
						taskRow = sheet.convertRowIndexToView(modelRow);
						break;
					}
				}
				assertTrue(taskRow >= 0);
				assertTrue(sheet.editCellAt(taskRow, 1, new MouseEvent(sheet, MouseEvent.MOUSE_CLICKED,
					System.currentTimeMillis(), 0, 0, 0, 2, false)));
				JTextField editor = (JTextField) ((NameCellComponent) sheet.getEditorComponent()).getTextComponent();
				editor.setText("After");
				sheet.selectAll();
				assertEquals("After", fx.task(0).getName());
				assertFalse(sheet.isEditing());
			});
		}
	}
}
