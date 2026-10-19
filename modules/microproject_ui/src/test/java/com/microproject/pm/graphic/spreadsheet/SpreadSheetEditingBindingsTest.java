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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.netbeans.swing.etable.TableColumnSelector;

import org.junit.jupiter.api.Test;

class SpreadSheetEditingBindingsTest {
	@Test
	void taskMoveShortcutsAreNotInstalledOnSheetOrRowHeader() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
			KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

			assertNull(sheet.getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertNull(sheet.getInputMap(JComponent.WHEN_FOCUSED).get(down));
			assertNull(sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(up));
			assertNull(sheet.getRowHeader().getInputMap(JComponent.WHEN_FOCUSED).get(down));
		});
	}

	@Test
	void enabledRootPaneGanttZoomOutOwnsCtrlNumpadMultiply() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			JRootPane rootPane = new JRootPane();
			rootPane.getContentPane().add(sheet);
			KeyStroke multiply = KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, KeyEvent.CTRL_DOWN_MASK);
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(multiply, "GanttKeypadZoomOut");
			rootPane.getActionMap().put("GanttKeypadZoomOut", new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(ActionEvent event) { }
			});
			KeyEvent event = new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
				KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_MULTIPLY, '*', KeyEvent.KEY_LOCATION_NUMPAD);

			assertFalse(sheet.processKeyBinding(multiply, event, JComponent.WHEN_FOCUSED, true),
				"the focused-table pass must yield the Gantt shortcut to the window root pane");
			assertFalse(sheet.processKeyBinding(multiply, event, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, true),
				"the ancestor-table pass must not let ETable consume the same Gantt shortcut");
		});
	}

	@Test
	void disabledRootPaneGanttZoomOutLeavesEtableCtrlMultiplyColumnSelectionAvailable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			AtomicInteger selectorCalls = new AtomicInteger();
			sheet.setColumnSelector(new TableColumnSelector() {
				@Override public String[] selectVisibleColumns(TreeNode root, String[] visibleColumns) {
					selectorCalls.incrementAndGet();
					return visibleColumns;
				}
				@Override public String[] selectVisibleColumns(String[] availableColumns, String[] visibleColumns) {
					selectorCalls.incrementAndGet();
					return visibleColumns;
				}
			});
			JRootPane rootPane = new JRootPane();
			rootPane.getContentPane().add(sheet);
			KeyStroke multiply = KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, KeyEvent.CTRL_DOWN_MASK);
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(multiply, "GanttKeypadZoomOut");
			AbstractAction inactiveZoomAction = new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(ActionEvent event) { }
			};
			inactiveZoomAction.setEnabled(false);
			rootPane.getActionMap().put("GanttKeypadZoomOut", inactiveZoomAction);
			KeyEvent event = new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
				KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_MULTIPLY, '*', KeyEvent.KEY_LOCATION_NUMPAD);

			assertTrue(sheet.processKeyBinding(multiply, event, JComponent.WHEN_FOCUSED, true),
				"outside Gantt, ETable's existing Ctrl+* handler must keep owning the shortcut");
			assertEquals(1, selectorCalls.get(), "the existing column-selection route must run once");
		});
	}
}
