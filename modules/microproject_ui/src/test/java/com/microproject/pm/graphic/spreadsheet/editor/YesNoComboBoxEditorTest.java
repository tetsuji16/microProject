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
package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.microproject.field.BooleanText;

class YesNoComboBoxEditorTest {
	@Test
	void listOffersYesThenNoAndCommitsTheChosenValueInAPlainTable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			YesNoComboBoxEditor editor = new YesNoComboBoxEditor(null);
			JTable table = new JTable(new DefaultTableModel(new Object[][] { { Boolean.TRUE } }, new Object[] { "Done" }));
			JComboBox<?> combo = (JComboBox<?>) editor.getTableCellEditorComponent(table, Boolean.TRUE, true, 0, 0);

			assertEquals(BooleanText.choices(null), items(combo));
			assertEquals(BooleanText.label(null, Boolean.TRUE), combo.getSelectedItem());
			assertTrue(!combo.isEditable(), "the choices are fixed");
			assertEquals(Boolean.TRUE, editor.getCellEditorValue());

			combo.setSelectedIndex(1);
			assertEquals(Boolean.FALSE, editor.getCellEditorValue());
			editor.getTableCellEditorComponent(table, Boolean.FALSE, true, 0, 0);
			assertEquals(BooleanText.label(null, Boolean.FALSE), combo.getSelectedItem());
		});
	}

	@Test
	void taskModeListOffersManualThenAutoScheduled() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			YesNoComboBoxEditor editor = new YesNoComboBoxEditor(BooleanText.TASK_MODE_FIELD_ID);
			JTable table = new JTable(new DefaultTableModel(1, 1));
			JComboBox<?> combo = (JComboBox<?>) editor.getTableCellEditorComponent(table, Boolean.FALSE, true, 0, 0);

			assertEquals(BooleanText.choices(BooleanText.TASK_MODE_FIELD_ID), items(combo));
			assertEquals(BooleanText.label(BooleanText.TASK_MODE_FIELD_ID, Boolean.FALSE), combo.getSelectedItem());
		});
	}

	private static List<Object> items(JComboBox<?> combo) {
		List<Object> items = new java.util.ArrayList<>();
		for (int index = 0; index < combo.getItemCount(); index++)
			items.add(combo.getItemAt(index));
		return items;
	}
}
