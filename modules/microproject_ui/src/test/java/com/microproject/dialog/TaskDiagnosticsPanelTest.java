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
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskDiagnosticsPanelTest {
	@Test
	void diagnosticsTableIsReadOnlyAndUsesOuterTabScrollViewport() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("diagnostics", undo);
		Project project = Project.createProject(pool, undo);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		SwingUtilities.invokeAndWait(() -> {
			TaskDiagnosticsPanel panel = new TaskDiagnosticsPanel(task);
			JTable table = findTable(panel);
			assertNotNull(table);
			assertEquals(4, table.getColumnCount());
			assertNotNull(table.getRowSorter());
			assertFalse(table.isCellEditable(0, 0));
			assertTrue(panel.getViewport().getView() instanceof JTable,
					"the diagnostics scroll viewport directly owns its table");
			assertSame(table, panel.getViewport().getView());
			assertFalse(containsNestedScrollPane(panel),
					"diagnostics has one scroll owner instead of nested scrollbars");
		});
	}

	private static JTable findTable(Container parent) {
		for (Component component : parent.getComponents()) {
			if (component instanceof JTable table)
				return table;
			if (component instanceof Container nested) {
				JTable table = findTable(nested);
				if (table != null)
					return table;
			}
		}
		return null;
	}

	private static boolean containsNestedScrollPane(Container parent) {
		for (Component component : parent.getComponents()) {
			if (component instanceof JScrollPane)
				return true;
			if (component instanceof Container nested && containsNestedScrollPane(nested))
				return true;
		}
		return false;
	}
}
