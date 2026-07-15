package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.DefaultListSelectionModel;
import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;

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

	@Test
	void oneRowCellSelectionIsNotMistakenForAHeaderColumnSelection() throws Exception {
		final TestSpreadSheet[] sheetRef = new TestSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new TestSpreadSheet());
		TestSpreadSheet sheet = sheetRef[0];

		SwingUtilities.invokeAndWait(() -> sheet.changeSelection(0, 0, false, false));
		assertEquals(false, sheet.isColumnFullySelected(0));

		SwingUtilities.invokeAndWait(() -> sheet.selectColumnAndAllRows(0));
		assertEquals(true, sheet.isColumnFullySelected(0));

		SwingUtilities.invokeAndWait(() -> sheet.changeSelection(0, 0, false, false));
		assertEquals(false, sheet.isColumnFullySelected(0));
	}

	private static final class TestSpreadSheet extends CommonSpreadSheet {
		private TestSpreadSheet() {
			getColumnModel().addColumn(new TableColumn(0));
			selection = new SpreadSheetSelectionModel(this);
			selection.setRowSelection(new DefaultListSelectionModel());
			selection.setColumnSelection(new DefaultListSelectionModel());
			setSelectionModel(selection.getRowSelection());
			getColumnModel().setSelectionModel(selection.getColumnSelection());
		}

		@Override
		public int getRowCount() {
			return 1;
		}
	}
}
