/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class PercentEditorTest {
	@Test void acceptsWholeNumberPercentagesAsRatios() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PercentEditor editor = new PercentEditor();
			JTextField field = (JTextField) editor.getTableCellEditorComponent(new JTable(), 0.0d, true, 0, 0);
			field.setText("30");
			assertEquals(0.3d, ((Double) editor.getCellEditorValue()).doubleValue(), 0.00001d);
			field.setText("100");
			assertEquals(1.0d, ((Double) editor.getCellEditorValue()).doubleValue(), 0.00001d);
		});
	}
}
