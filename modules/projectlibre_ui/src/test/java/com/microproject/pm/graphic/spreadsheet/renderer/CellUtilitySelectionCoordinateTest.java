package com.microproject.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

class CellUtilitySelectionCoordinateTest {
	@Test
	void activeCellUsesCurrentViewSelectionCoordinates() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTable table = new JTable(new DefaultTableModel(3, 3));
			table.setCellSelectionEnabled(true);
			table.changeSelection(1, 2, false, false);

			assertTrue(CellUtility.isActiveCell(table, 1, 2, false));
			assertFalse(CellUtility.isActiveCell(table, 0, 2, true));
			assertFalse(CellUtility.isActiveCell(table, 1, 1, true));
		});
	}
}
