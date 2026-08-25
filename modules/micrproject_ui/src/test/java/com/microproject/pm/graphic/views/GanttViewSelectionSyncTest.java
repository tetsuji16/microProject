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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GanttViewSelectionSyncTest {
	@Test
	void dragSelectionHighlightsRowsBeforeMouseRelease() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			TestProjectionGantt gantt = newGantt();
			try {
				JTable table = new JTable(5, 1);
				TaskSelectionController controller = new TaskSelectionController(gantt, table);
				table.getSelectionModel().setValueIsAdjusting(true);
				table.setRowSelectionInterval(1, 3);

				assertEquals(Set.of(gantt.keyAt(1), gantt.keyAt(2), gantt.keyAt(3)), gantt.getHighlightedRowKeys());
				controller.close();
			} finally {
				gantt.cleanUp();
			}
		});
	}

	@Test
	void selectionAndChartClickResolveThroughTheSameProjectionIdentity() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Project project = newProject("projection-selection");
			createTask(project, "First");
			createTask(project, "Second");
			ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			ViewNodeModelCache cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
					.createFilteredCache(reference, "projection-selection-view", null);
			cache.update();
			Gantt gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			try {
				JTable table = new JTable(cache.getSize(), 1);
				TaskSelectionController controller = new TaskSelectionController(gantt, table);
				int selectedRow = Math.min(1, cache.getSize() - 1);
				table.setRowSelectionInterval(selectedRow, selectedRow);
				ProjectionRowKey key = cache.getRowKeyAt(selectedRow);

				assertEquals(Set.of(key), gantt.getHighlightedRowKeys());
				var installed=cache.getInstalledProjectionSnapshot();
				assertEquals(selectedRow, TaskSelectionController.projectionRowForClick(gantt, new Gantt.BarClick(key,
						installed.topology().domainRevision(), installed.topology().topologyRevision(), false, false)));
				controller.close();
			} finally {
				gantt.cleanUp();
				cache.close();
				reference.close();
			}
		});
	}

	@Test
	void selectedTaskFollowsItsIdentityWhenAnEarlierRowIsDeleted() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Project project = newProject("projection-selection-delete");
			NormalTask first = createTask(project, "First");
			NormalTask second = createTask(project, "Second");
			ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			ViewNodeModelCache cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
					.createFilteredCache(reference, "projection-selection-delete-view", null);
			cache.update();
			Gantt gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			JTable table = new JTable(cache.getSize(), 1);
			TaskSelectionController controller = new TaskSelectionController(gantt, table);
			try {
				GraphicNode firstNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(first));
				GraphicNode secondNode = (GraphicNode) cache.getGraphicNode(project.getTaskModel().search(second));
				ProjectionRowKey secondKey = cache.getRowKeyAt(cache.getRowAt(secondNode));
				table.setRowSelectionInterval(cache.getRowAt(secondNode), cache.getRowAt(secondNode));

				cache.deleteNodes(List.of(firstNode.getNode()));
				cache.update();

				assertEquals(Set.of(secondKey), gantt.getHighlightedRowKeys());
				assertEquals(cache.getRowAt(secondKey), table.getSelectedRow());
			} finally {
				controller.close();
				gantt.cleanUp();
				cache.close();
				reference.close();
			}
		});
	}

	@Test
	void projectionNotificationFromWorkerNeverMutatesTheTableOffEdt() throws Exception {
		Project project = newProject("projection-selection-edt");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
				.createFilteredCache(reference, "projection-selection-edt-view", null);
		cache.update();
		Gantt[] gantt = new Gantt[1];
		EdtCheckingTable[] table = new EdtCheckingTable[1];
		TaskSelectionController[] controller = new TaskSelectionController[1];
		SwingUtilities.invokeAndWait(() -> {
			gantt[0] = new Gantt(project, "Gantt");
			gantt[0].setCache(cache);
			table[0] = new EdtCheckingTable(Math.max(1, cache.getSize()), 1);
			controller[0] = new TaskSelectionController(gantt[0], table[0]);
		});

		Thread worker = new Thread(() -> controller[0].graphicNodesCompositeEvent(
				new CompositeCacheEvent(cache, List.of(), List.of())), "projection-notification-test");
		worker.start();
		worker.join();
		SwingUtilities.invokeAndWait(() -> { });

		assertFalse(table[0].mutatedOffEdt);
		SwingUtilities.invokeAndWait(() -> {
			controller[0].close();
			gantt[0].cleanUp();
		});
		cache.close();
		reference.close();
	}

	private static TestProjectionGantt newGantt() {
		return new TestProjectionGantt(newProject("gantt-selection-sync-test"));
	}

	private static Project newProject(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool(name, undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static final class TestProjectionGantt extends Gantt {
		private static final long serialVersionUID = 1L;

		private TestProjectionGantt(Project project) {
			super(project, "Gantt");
		}

		private ProjectionRowKey keyAt(int row) {
			return new ProjectionRowKey(ProjectionRowKey.Kind.TASK, null, 0L, row + 1L);
		}

		@Override
		public ProjectionRowKey getProjectionRowKey(int row) {
			return keyAt(row);
		}
	}

	private static final class EdtCheckingTable extends JTable {
		private static final long serialVersionUID = 1L;
		private volatile boolean mutatedOffEdt;

		private EdtCheckingTable(int rows, int columns) {
			super(rows, columns);
		}

		@Override
		public void clearSelection() {
			if (!SwingUtilities.isEventDispatchThread())
				mutatedOffEdt = true;
			super.clearSelection();
		}
	}
}
