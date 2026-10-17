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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class CommonTableEscapeKeyTest {

	/**
	 * Microsoft documents "press Esc" to stop Format Painter. The table must not
	 * consume Escape when no editor is open, otherwise the root-pane window-level
	 * shortcut never runs.
	 */
	@Test
	void escapeIsOnlyConsumedWhileEditing() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			javax.swing.tree.DefaultMutableTreeNode root = new javax.swing.tree.DefaultMutableTreeNode("root");
			root.add(new javax.swing.tree.DefaultMutableTreeNode("task"));
			CommonTable table = new CommonTable(org.netbeans.swing.outline.DefaultOutlineModel.createOutlineModel(
				new javax.swing.tree.DefaultTreeModel(root), new EditableTextRowModel(), true, "Name"));
			KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
			Object key = table.getInputMap(JComponent.WHEN_FOCUSED).get(escape);
			Action cancel = key == null ? null : table.getActionMap().get(key);
			assertNotNull(cancel, "the table keeps an Escape editor-cancel action");
			assertFalse(cancel.isEnabled(), "Escape must pass to the window when no cell editor is open");

			assertTrue(table.editCellAt(0, 1), "fixture cell must be editable");
			assertTrue(cancel.isEnabled(), "Escape must still cancel an open cell editor");
			cancel.actionPerformed(new java.awt.event.ActionEvent(table, java.awt.event.ActionEvent.ACTION_PERFORMED,
				String.valueOf(key)));
			assertFalse(table.isEditing(), "Escape cancellation must remove the editor");
		});
	}

	private static final class EditableTextRowModel implements org.netbeans.swing.outline.RowModel {
		private final java.util.Map<Object, Object> values = new java.util.HashMap<>();
		@Override public int getColumnCount() { return 1; }
		@Override public Object getValueFor(Object node, int column) { return values.getOrDefault(node, ""); }
		@Override public Class<?> getColumnClass(int column) { return String.class; }
		@Override public boolean isCellEditable(Object node, int column) { return true; }
		@Override public void setValueFor(Object node, int column, Object value) { values.put(node, value); }
		@Override public String getColumnName(int column) { return "Text"; }
	}
}
