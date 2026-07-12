package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class CommonSpreadSheetSelectionStateTest {
	@Test
	void emptySheetSelectionAccessorsReturnSafeDefaults() throws Exception {
		final CommonSpreadSheet[] sheetRef = new CommonSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new CommonSpreadSheet());

		CommonSpreadSheet sheet = sheetRef[0];

		assertEquals(new ArrayList<>(), sheet.getSelectedNodes());
		assertEquals(new ArrayList<>(), sheet.getSelectedNodesImpl());
		assertEquals(new ArrayList<>(), sheet.getSelectedFields());
		assertEquals(new ArrayList<>(), sheet.getSelectableFields());
		assertNull(sheet.getCurrentRowImpl());
		assertNull(sheet.getCurrentRowNode());
	}

	@Test
	void emptySheetSelectionOperationsDoNothing() throws Exception {
		final CommonSpreadSheet[] sheetRef = new CommonSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new CommonSpreadSheet());

		CommonSpreadSheet sheet = sheetRef[0];

		sheet.selectRowAndAllColumns(0);
		sheet.selectColumnAndAllRows(0);
		sheet.selectEntireSpreadsheet();
	}
}
