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

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression test for the digester "name" leak bug.
 *
 * <p>Because {@code Select} implements {@code java.util.Map}, digester's {@code SetPropertiesRule}
 * routed the {@code <select name="...">} attribute through commons-beanutils' Map-bean path and
 * called {@code put("name", <value>)} instead of {@code setName(...)}, injecting a spurious
 * {@code "name"} entry as the FIRST item of every static dropdown (constraint type, task type,
 * earned value method, ...). That made the combo boxes render a bogus "name" choice.
 *
 * <p>This test builds the real Task Information dialog through the production code path on the EDT
 * and asserts that none of its combo boxes contain a {@code "name"} item. It also asserts the
 * dynamic task-calendar combo has at least one real option (the empty-combo digester bug).
 *
 * <p>Skipped automatically when running headless, since the dialog builds real Swing components.
 */
class TaskInformationComboboxContentTest {

	@Test
	void noComboBoxContainsSpuriousNameItem() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"dialog builds real Swing components; skip on headless CI");

		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		final JComponent[] panelHolder = new JComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			TaskInformationDialog dlg = TaskInformationDialog.getInstance(null, task, false,
					new com.microproject.application.task.TaskCommandGateway(task.getOwningProject()));
			panelHolder[0] = dlg.createContentPanel(); // must not throw
		});

		final boolean[] foundName = { false };
		final int[] comboCount = { 0 };
		final int[] emptyCombos = { 0 };
		walk(panelHolder[0], foundName, comboCount, emptyCombos);

		assertFalse(foundName[0], "a combo box contains the spurious 'name' item");
		assertFalse(comboCount[0] == 0, "expected at least one combo box in the dialog");
		assertFalse(emptyCombos[0] > 0, "a combo box (e.g. task calendar) has zero items");
	}

	private static void walk(Container c, boolean[] foundName, int[] comboCount, int[] emptyCombos) {
		for (Component child : c.getComponents()) {
			if (child instanceof JComboBox) {
				comboCount[0]++;
				JComboBox<?> combo = (JComboBox<?>) child;
				if (combo.getItemCount() == 0) {
					emptyCombos[0]++;
				}
				for (int i = 0; i < combo.getItemCount(); i++) {
					Object item = combo.getItemAt(i);
					if ("name".equals(String.valueOf(item))) {
						foundName[0] = true;
						fail("combo box contains spurious 'name' item: " + child);
					}
				}
			} else if (child instanceof Container) {
				walk((Container) child, foundName, comboCount, emptyCombos);
			}
		}
	}
}
