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
package com.microproject.model.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.node.Node;
import com.microproject.node.NodeFactory;
import com.microproject.node.model.DefaultNodeModel;
import com.microproject.node.model.NodeModel;
import com.microproject.undo.DataFactoryUndoController;

class GanttDrawingsTest {
	@Test
	void everyDrawingOperationIsOneUndoStep() {
		Project project = createProject();
		GanttDrawings drawings = project.getGanttDrawings();
		project.getUndoController().clear();

		GanttDrawing line = drawings.add(project, GanttDrawing.Kind.LINE, 11L, GanttDrawing.AnchorSide.START,
				0, 0, 30, 0, 0xFF000000, 0, "");
		assertEquals(List.of(line), drawings.getDrawings());
		project.getUndoController().undo();
		assertTrue(drawings.isEmpty(), "one undo removes the added drawing");
		project.getUndoController().redo();
		assertEquals(List.of(line), drawings.getDrawings());

		assertTrue(drawings.move(project, line.id(), 12.5, 4));
		assertEquals(12.5, drawings.find(line.id()).x());
		assertEquals(4, drawings.find(line.id()).y());
		project.getUndoController().undo();
		assertEquals(0, drawings.find(line.id()).x());
		project.getUndoController().redo();
		assertEquals(12.5, drawings.find(line.id()).x());

		assertTrue(drawings.format(project, line.id(), 0xFF112233, 0xFFAABBCC, "note"));
		assertEquals("note", drawings.find(line.id()).text());
		assertEquals(0xFF112233, drawings.find(line.id()).lineColor());
		project.getUndoController().undo();
		assertEquals("", drawings.find(line.id()).text());
		project.getUndoController().redo();
		assertEquals("note", drawings.find(line.id()).text());

		assertTrue(drawings.remove(project, line.id()));
		assertTrue(drawings.isEmpty());
		project.getUndoController().undo();
		assertEquals(1, drawings.size(), "one undo restores the removed drawing");
		assertEquals(line.id(), drawings.getDrawings().get(0).id());
		assertEquals("note", drawings.find(line.id()).text());
	}

	@Test
	void drawingIdsAreNotReusedAfterUndoingTheNewestDrawing() {
		Project project = createProject();
		GanttDrawings drawings = project.getGanttDrawings();
		project.getUndoController().clear();

		GanttDrawing first = drawings.add(project, GanttDrawing.Kind.RECTANGLE, 11L, GanttDrawing.AnchorSide.START,
				0, 0, 40, 12, 0xFF000000, 0xFFFFFF00, "");
		project.getUndoController().undo();
		GanttDrawing second = drawings.add(project, GanttDrawing.Kind.RECTANGLE, 11L, GanttDrawing.AnchorSide.START,
				0, 0, 40, 12, 0xFF000000, 0xFFFFFF00, "");

		assertTrue(second.id() > first.id(), "an undone id is never handed out again");
	}

	@Test
	void deletingATaskRemovesItsDrawingsAndOneUndoRestoresBoth() {
		Project project = createProject();
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		NormalTask task = new NormalTask(project);
		task.setOwningProject(project);
		task.setUniqueId(42L);
		Node node = NodeFactory.getInstance().createNode(task);
		model.add(root, node, NodeModel.SILENT);

		GanttDrawings drawings = project.getGanttDrawings();
		GanttDrawing drawing = drawings.add(project, GanttDrawing.Kind.RECTANGLE, 42L, GanttDrawing.AnchorSide.END,
				1, 2, 40, 12, 0xFF000000, 0xFFFFFF00, "note");
		GanttDrawing other = drawings.add(project, GanttDrawing.Kind.TEXT_BOX, 42L, GanttDrawing.AnchorSide.START,
				5, 6, 60, 14, 0xFF000000, 0, "other task");
		assertNotNull(other);
		project.getUndoController().clear();

		model.remove(node, NodeModel.NORMAL);

		assertTrue(drawings.isEmpty(), "the deleted task takes its drawings with it");
		assertEquals(0, root.getChildCount());

		project.getUndoController().undo();
		assertEquals(1, root.getChildCount());
		assertNotNull(drawings.find(drawing.id()), "one undo restores the task and its drawing");
		assertNotNull(drawings.find(other.id()));

		project.getUndoController().redo();
		assertTrue(drawings.isEmpty());
		assertEquals(0, root.getChildCount());
	}

	@Test
	void drawingsOfAnotherTaskSurviveATaskDeletion() {
		Project project = createProject();
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.getHierarchy().setNbEndVoidNodes(0);
		Node root = (Node) model.getHierarchy().getRoot();
		NormalTask deleted = new NormalTask(project);
		deleted.setOwningProject(project);
		deleted.setUniqueId(42L);
		NormalTask kept = new NormalTask(project);
		kept.setOwningProject(project);
		kept.setUniqueId(43L);
		Node deletedNode = NodeFactory.getInstance().createNode(deleted);
		Node keptNode = NodeFactory.getInstance().createNode(kept);
		model.add(root, deletedNode, NodeModel.SILENT);
		model.add(root, keptNode, NodeModel.SILENT);

		GanttDrawings drawings = project.getGanttDrawings();
		drawings.add(project, GanttDrawing.Kind.LINE, 42L, GanttDrawing.AnchorSide.START, 0, 0, 10, 0, 0xFF000000, 0, "");
		GanttDrawing keptDrawing = drawings.add(project, GanttDrawing.Kind.LINE, 43L, GanttDrawing.AnchorSide.END,
				0, 0, 10, 0, 0xFF000000, 0, "");
		project.getUndoController().clear();

		model.remove(deletedNode, NodeModel.NORMAL);

		assertEquals(List.of(keptDrawing), drawings.getDrawings());
		assertNull(drawings.find(1));
	}

	private static Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		project.getTaskModel();
		return project;
	}
}
