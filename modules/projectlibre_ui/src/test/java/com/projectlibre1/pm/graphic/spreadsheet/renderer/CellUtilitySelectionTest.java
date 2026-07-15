package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

class CellUtilitySelectionTest {
	@Test
	void fullColumnSelectionDoesNotExposeTheLeadCellAsActive() {
		JTable table = new JTable(new DefaultTableModel(3, 2));
		table.setCellSelectionEnabled(true);
		table.setRowSelectionInterval(0, 2);
		table.setColumnSelectionInterval(1, 1);

		assertFalse(CellUtility.isActiveCell(table, 0, 1, true));
		assertFalse(CellUtility.isActiveCell(table, 2, 1, true));
	}

	@Test
	void ordinaryCellSelectionStillHasAnActiveCell() {
		JTable table = new JTable(new DefaultTableModel(3, 2));
		table.setCellSelectionEnabled(true);
		table.changeSelection(1, 1, false, false);

		assertTrue(CellUtility.isActiveCell(table, 1, 1, true));
	}
}
