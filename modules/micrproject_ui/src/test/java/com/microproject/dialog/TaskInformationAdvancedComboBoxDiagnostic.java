/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.configuration.FieldDictionary;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Diagnostic: inspect the Task Information Advanced tab's JComboBox fields and
 * the Field.getOptions resolution for taskCalendar / taskType.
 */
class TaskInformationAdvancedComboBoxDiagnostic {

	@Test
	void dumpAdvancedComboBoxes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"createContentPanel builds real Swing components; skip on headless CI");

		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		final List<String> report = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			TaskInformationDialog dlg = TaskInformationDialog.getInstance(null, task, false);
			JComponent panel = dlg.createContentPanel();
			collectComboBoxes(panel, 0, report);

			// Directly resolve Field.getOptions for the dynamic/static selects
			for (String id : new String[] { "Field.taskCalendar", "Field.taskType", "Field.priority", "Field.earnedValueMethod" }) {
				Field f = FieldDictionary.getInstance().getFieldFromId(id);
				Object[] opts = f.getOptions(task);
				Object[] optsNull = f.getOptions(null);
				report.add(String.format("FIELD %s hasOptions=%b dynSelect=%b getOptions(task).len=%d getOptions(null).len=%d",
						id, f.hasOptions(), f.hasDynamicSelect(),
						opts == null ? -1 : opts.length, optsNull == null ? -1 : optsNull.length));
			}
		});
		for (String line : report) {
			System.out.println(line);
		}
		assertNotNull(report);
	}

	private static void collectComboBoxes(Container container, int depth, List<String> report) {
		for (Component comp : container.getComponents()) {
			if (comp instanceof JComboBox) {
				@SuppressWarnings("unchecked")
				JComboBox<Object> combo = (JComboBox<Object>) comp;
				int n = combo.getItemCount();
				Object sel = combo.getSelectedItem();
				int selIdx = combo.getSelectedIndex();
				StringBuilder items = new StringBuilder();
				for (int i = 0; i < Math.min(n, 8); i++) {
					if (i > 0) items.append(", ");
					items.append("[").append(i).append("]=").append(combo.getItemAt(i));
				}
				report.add(String.format(
						"COMBO depth=%d itemCount=%d selectedIndex=%d selectedItem=%s items=%s",
						depth, n, selIdx, sel, items));
			}
			if (comp instanceof Container) {
				collectComboBoxes((Container) comp, depth + 1, report);
			}
		}
	}
}
