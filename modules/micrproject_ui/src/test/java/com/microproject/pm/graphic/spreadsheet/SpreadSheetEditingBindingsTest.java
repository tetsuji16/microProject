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
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SpreadSheetEditingBindingsTest {
	@Test
	void ctrlDFillsDownSelectedCells() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			Object action = sheet.getInputMap(JComponent.WHEN_FOCUSED)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
			assertEquals(SpreadSheet.FILL_DOWN_ACTION, action);
			assertNotNull(sheet.getActionMap().get(action));
		});
	}

	@Test
	void msProjectTaskMoveShortcutsAreInstalledOnSheetAndRowHeader() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
			KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

			assertEquals(SpreadSheet.MOVE_TASK_UP_ACTION, sheet.getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertEquals(SpreadSheet.MOVE_TASK_DOWN_ACTION, sheet.getInputMap(JComponent.WHEN_FOCUSED).get(down));
			assertEquals(SpreadSheet.MOVE_TASK_UP_ACTION, sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertEquals(SpreadSheet.MOVE_TASK_DOWN_ACTION, sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(down));
			assertNotNull(sheet.getActionMap().get(SpreadSheet.MOVE_TASK_UP_ACTION));
			assertNotNull(sheet.getRowHeader().getActionMap().get(SpreadSheet.MOVE_TASK_DOWN_ACTION));
		});
	}
}
