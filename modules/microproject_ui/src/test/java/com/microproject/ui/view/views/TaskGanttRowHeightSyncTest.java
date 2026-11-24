/*
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
 */
package com.microproject.ui.view.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.awt.Point;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.junit.jupiter.api.Test;

import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
import com.microproject.ui.export.svg.GanttSVGRenderer;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.gantt.GanttParams;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.undo.DataFactoryUndoController;

class TaskGanttRowHeightSyncTest {
	@Test
	void variableGeometryPreservesOutlineIndentAndClosestPathBehavior() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
			DefaultMutableTreeNode parent = new DefaultMutableTreeNode("Parent");
			DefaultMutableTreeNode child = new DefaultMutableTreeNode("Group");
			DefaultMutableTreeNode leaf = new DefaultMutableTreeNode("Child");
			root.add(parent);
			parent.add(child);
			child.add(leaf);
			SpreadSheet sheet = new SpreadSheet();
			sheet.setModel(DefaultOutlineModel.createOutlineModel(new DefaultTreeModel(root), new TestRowModel(), false, "Name"));
			sheet.setRootVisible(false);
			TreePath parentPath = new TreePath(parent.getPath());
			TreePath childPath = new TreePath(child.getPath());
			TreePath leafPath = new TreePath(leaf.getPath());
			sheet.expandPath(parentPath);
			sheet.expandPath(childPath);
			sheet.setRowHeight(0, 30);
			sheet.setRowHeight(2, 48);

			java.awt.Rectangle childBounds = sheet.getPathBounds(leafPath);
			assertNotNull(childBounds);
			java.awt.Rectangle outlineBounds = sheet.getOutlineModel().getLayout().getBounds(leafPath, null);
			assertEquals(outlineBounds.x + sheet.getInsets().left, childBounds.x,
				"row-height adjustment must preserve Outline's hierarchical x offset");
			assertEquals(outlineBounds.width, childBounds.width,
				"row-height adjustment must preserve Outline's path width");
			assertEquals(30 + sheet.getRowHeight(1), childBounds.y);
			assertEquals(48, childBounds.height);
			assertEquals(leafPath, sheet.getClosestPathForLocation(childBounds.x, childBounds.y + 2));
			assertEquals(leafPath, sheet.getClosestPathForLocation(childBounds.x, sheet.getPreferredSize().height + 10),
				"Outline closest-path behavior should still resolve blank space below the rows");
		});
	}

	@Test
	void defaultAndPerRowHeightsStayAlignedAcrossTableGanttAndPrintRenderer() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("row-height-sync-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		for (int i = 0; i < 3; i++) project.createScriptedTask().setName("Task " + i);
		project.recalculate();
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(reference, "row-height-sync-test", null);
		Gantt gantt = new Gantt(project, "Gantt");
		cache.update();
		gantt.setCache(cache);
		cache.update();
		try {
			SwingUtilities.invokeAndWait(() -> {
				DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
				for (int i = 0; i < 3; i++) root.add(new DefaultMutableTreeNode("Task " + i));
				SpreadSheet sheet = new SpreadSheet();
				sheet.setModel(DefaultOutlineModel.createOutlineModel(new DefaultTreeModel(root), new TestRowModel(), false, "Name"));
				TaskGanttSyncSupport.applyRowHeight(sheet, gantt, 30);
				assertEquals(30, sheet.getRowHeight());
				assertEquals(30, gantt.getRowHeight());
				assertEquals(30, sheet.getRowHeight(2));
				assertEquals(30, gantt.getRowHeights().heightOf(2));

				RowHeights requested = new RowHeights(34, 3);
				requested.setRowHeight(2, 52);
				TaskGanttSyncSupport.applyRowHeight(sheet, gantt, requested);
				assertEquals(34, sheet.getRowHeight());
				assertEquals(34, gantt.getRowHeight());
				assertEquals(34, sheet.getRowHeight(0));
				assertEquals(52, sheet.getRowHeight(2));
				assertEquals(68, sheet.getCellRect(2, 0, true).y);
				assertEquals(2, sheet.rowAtPoint(new Point(2, 70)));
				assertEquals(120, sheet.getPreferredSize().height);
				assertEquals(34, gantt.getRowHeights().heightOf(0));
				assertEquals(52, gantt.getRowHeights().heightOf(2));

				GanttSVGRenderer print = new GanttSVGRenderer();
				print.init(project, cache, null, new ArrayList<>(), -1, true);
				print.setRowHeights(gantt.getRowHeights());
				RowHeights printHeights = ((GanttParams) print.getParams()).getRowHeights();
				assertEquals(52, printHeights.heightOf(2));
				assertEquals(34 + 34 + 52, printHeights.totalHeight(3));
				gantt.getRowHeights().clearRowHeight(2);
				assertEquals(52, printHeights.heightOf(2), "print keeps an independent snapshot of live heights");

				TaskGanttSyncSupport.applyRowHeight(sheet, gantt, 26);
				assertEquals(26, sheet.getRowHeight());
				assertEquals(26, gantt.getRowHeight());
				assertEquals(26, sheet.getRowHeight(2));
				assertEquals(26, gantt.getRowHeights().heightOf(2));
			});
		} finally {
			gantt.cleanUp();
			cache.close();
			reference.close();
		}
	}

	private static final class TestRowModel implements RowModel {
		@Override public int getColumnCount() { return 0; }
		@Override public Object getValueFor(Object node, int column) { return null; }
		@Override public Class<?> getColumnClass(int column) { return Object.class; }
		@Override public boolean isCellEditable(Object node, int column) { return false; }
		@Override public void setValueFor(Object node, int column, Object value) { }
		@Override public String getColumnName(int column) { return ""; }
	}
}
