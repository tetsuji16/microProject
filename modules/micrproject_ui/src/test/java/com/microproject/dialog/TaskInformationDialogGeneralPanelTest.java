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

import java.awt.GraphicsEnvironment;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.gantt.BarColorEditorPanel;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for issue #16 (Bar Color integrated into Task Information General tab)
 * and the double-click "first click fails, second opens empty" bug.
 *
 * It builds the full content panel (all tabs) through the production code path on the
 * EDT and asserts that no exception is thrown. A thrown exception here means the dialog
 * would fail to construct on the first double-click and leave a broken cached dialog
 * behind, so the second click would open that empty/broken dialog.
 *
 * Skipped automatically when running headless (GraphicsEnvironment.isHeadless()), since
 * createContentPanel() constructs real Swing components.
 */
class TaskInformationDialogGeneralPanelTest {

	@Test
	void reusedDialogRefreshesBarColorsAndReadOnlyState() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			BarColorEditorPanel editor = new BarColorEditorPanel(null,
					new BarFormat(0x111111, 0x222222, 0x333333), false, false, null);

			TaskInformationDialog.refreshBarColorFields(editor,
					new BarFormat(0xAABBCC, null, 0x010203), true);

			assertEquals(Integer.valueOf(0xAABBCC), editor.getStart().getRgb());
			assertEquals(null, editor.getMiddle().getRgb());
			assertEquals(Integer.valueOf(0x010203), editor.getEnd().getRgb());
			assertFalse(editor.isEnabled());
			assertFalse(editor.getStart().isEnabled());
			assertFalse(editor.getMiddle().isEnabled());
			assertFalse(editor.getEnd().isEnabled());
		});
	}

	@Test
	void firstConstructionBuildsContentPanelWithoutException() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"createContentPanel builds real Swing components; skip on headless CI");

		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		final JComponent[] panelHolder = new JComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			TaskInformationDialog dlg = TaskInformationDialog.getInstance(null, task, false);
			panelHolder[0] = dlg.createContentPanel(); // must not throw
		});
		assertNotNull(panelHolder[0], "content panel should be constructed without exception");
	}
}
