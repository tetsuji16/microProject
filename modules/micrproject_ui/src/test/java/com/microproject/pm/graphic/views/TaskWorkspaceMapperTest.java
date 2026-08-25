/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.field.Field;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskWorkspaceMapperTest {
	@Test
	void v2SelectionFollowsDurableTaskAfterAnEarlierRowIsDeletedAndSerializes() throws Exception {
		Fixture fixture = createFixture("workspace-v2");
		try {
			CommonSpreadSheet.Workspace[] serialized = new CommonSpreadSheet.Workspace[1];
			ProjectionRowKey[] selectedKey = new ProjectionRowKey[1];
			SwingUtilities.invokeAndWait(() -> {
				int row = fixture.cache.getRowAt(fixture.secondNode);
				fixture.sheet.setRowSelectionInterval(row, row);
				fixture.sheet.setColumnSelectionInterval(0, 0);
				fixture.sheet.getSelection().setActiveCell(row, 0);
				CommonSpreadSheet.Workspace workspace =
						(CommonSpreadSheet.Workspace) fixture.sheet.createWorkspace(0);
				TaskWorkspaceMapper.capture(fixture.sheet, fixture.cache, workspace);
				selectedKey[0] = fixture.cache.getRowKeyAt(row);
				serialized[0] = roundTrip(workspace);
			});

			assertEquals(TaskWorkspaceMapper.SCHEMA_VERSION, serialized[0].getSchemaVersion());
			assertTrue(serialized[0].getSelectedRows().length > 0, "V1 rows remain dual-written during migration");
			assertTrue(serialized[0].getSelectedFieldIds().length > 0);

			SwingUtilities.invokeAndWait(() -> {
				fixture.cache.deleteNodes(List.of(fixture.firstNode.getNode()));
				fixture.cache.update();
				if (fixture.sheet.getColumnCount() > 1)
					fixture.sheet.moveColumn(0, fixture.sheet.getColumnCount() - 1);
				fixture.sheet.clearSelection();
				TaskWorkspaceMapper.restore(fixture.sheet, fixture.cache, serialized[0]);
				assertArrayEquals(new int[] { fixture.cache.getRowAt(selectedKey[0]) }, fixture.sheet.getSelectedRows());
				assertEquals(fixture.cache.getRowAt(selectedKey[0]), fixture.sheet.getSelection().getActiveRow());
				assertEquals(serialized[0].getSelectedFieldIds()[0], selectedFieldId(fixture.sheet));
			});
		} finally {
			fixture.close();
		}
	}

	@Test
	void v1WorkspaceStillRestoresLegacyNumericSelection() throws Exception {
		Fixture fixture = createFixture("workspace-v1");
		try {
			SwingUtilities.invokeAndWait(() -> {
				int row = fixture.cache.getRowAt(fixture.secondNode);
				CommonSpreadSheet.Workspace workspace = new CommonSpreadSheet.Workspace();
				workspace.setSelectedRows(new int[] { row });
				workspace.setSelectedColumns(new int[] { 0 });
				fixture.sheet.restoreWorkspace(workspace, 0);
				assertArrayEquals(new int[] { row }, fixture.sheet.getSelectedRows());
			});
		} finally {
			fixture.close();
		}
	}

	@Test
	void invalidV2IdentityDoesNotFallBackToStaleLegacyRow() throws Exception {
		Fixture fixture = createFixture("workspace-invalid-v2");
		try {
			SwingUtilities.invokeAndWait(() -> {
				CommonSpreadSheet.Workspace workspace = new CommonSpreadSheet.Workspace();
				workspace.setSchemaVersion(TaskWorkspaceMapper.SCHEMA_VERSION);
				workspace.setSelectedRows(new int[] { 0 });
				workspace.setSelectedEntityKeys(new String[] { "corrupt" });
				fixture.sheet.restoreWorkspace(workspace, 0);
				TaskWorkspaceMapper.restore(fixture.sheet, fixture.cache, workspace);
				assertEquals(0, fixture.sheet.getSelectedRowCount());
			});
		} finally {
			fixture.close();
		}
	}

	private static Fixture createFixture(String name) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		NormalTask first = createTask(project, "First");
		NormalTask second = createTask(project, "Second");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, name + "-view", null);
		cache.update();
		SpreadSheet[] sheet = new SpreadSheet[1];
		JScrollPane[] scrollPane = new JScrollPane[1];
		SwingUtilities.invokeAndWait(() -> {
			sheet[0] = new SpreadSheet();
			sheet[0].setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			SpreadSheetUtils.setFieldsAndContext(sheet[0], cache, SpreadSheetCategories.taskSpreadsheetCategory,
					"Spreadsheet.Task.entry", true);
			scrollPane[0] = new JScrollPane(sheet[0]);
			scrollPane[0].getViewport().setViewPosition(new java.awt.Point(0, 0));
		});
		return new Fixture(sheet[0], scrollPane[0], cache, reference,
				(GraphicNode) cache.getGraphicNode(project.getTaskModel().search(first)),
				(GraphicNode) cache.getGraphicNode(project.getTaskModel().search(second)));
	}

	private static NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static String selectedFieldId(SpreadSheet sheet) {
		int column = sheet.getSelectedColumn();
		Object identifier = sheet.getColumnModel().getColumn(column).getIdentifier();
		return identifier instanceof Field field ? field.getId() : null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T roundTrip(T value) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
				output.writeObject(value);
			}
			try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				return (T) input.readObject();
			}
		} catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	private record Fixture(SpreadSheet sheet, JScrollPane scrollPane, ViewNodeModelCache cache,
			ReferenceNodeModelCache reference, GraphicNode firstNode, GraphicNode secondNode) {
		private void close() throws Exception {
			SwingUtilities.invokeAndWait(() -> SpreadsheetViewSupport.cleanup(sheet));
			cache.close();
			reference.close();
		}
	}
}
