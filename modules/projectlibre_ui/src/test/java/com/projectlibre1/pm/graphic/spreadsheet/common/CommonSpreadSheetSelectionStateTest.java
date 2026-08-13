package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.DefaultListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

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

	@Test
	void changingTheActiveCellImmediatelyRepaintsBothTaskHeaders() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			TestSpreadSheet sheet = new TestSpreadSheet(2);
			sheet.changeSelection(0, 0, false, false);
			sheet.resetHeaderRepaintCounts();

			sheet.changeSelection(1, 1, false, false);

			assertTrue(sheet.columnHeader.repaintCount > 0,
				"the old active column header must not remain highlighted");
			assertTrue(sheet.recordingRowHeader.repaintCount > 0,
				"the old active row header must not remain highlighted");
		});
	}

	private static final class TestSpreadSheet extends CommonSpreadSheet {
		private final RecordingTableHeader columnHeader;
		private final RecordingRowHeader recordingRowHeader;
		private final int rowCount;

		private TestSpreadSheet() {
			this(1);
		}

		private TestSpreadSheet(int rowCount) {
			this.rowCount = rowCount;
			getColumnModel().addColumn(new TableColumn(0));
			getColumnModel().addColumn(new TableColumn(1));
			columnHeader = new RecordingTableHeader(getColumnModel());
			setTableHeader(columnHeader);
			recordingRowHeader = new RecordingRowHeader(this);
			rowHeader = recordingRowHeader;
			selection = new SpreadSheetSelectionModel(this);
			selection.setRowSelection(new DefaultListSelectionModel());
			selection.setColumnSelection(new DefaultListSelectionModel());
			setSelectionModel(selection.getRowSelection());
			getColumnModel().setSelectionModel(selection.getColumnSelection());
		}

		private void resetHeaderRepaintCounts() {
			columnHeader.repaintCount = 0;
			recordingRowHeader.repaintCount = 0;
		}

		@Override
		public int getRowCount() {
			return rowCount;
		}
	}

	private static final class RecordingTableHeader extends JTableHeader {
		private int repaintCount;

		private RecordingTableHeader(TableColumnModel model) {
			super(model);
		}

		@Override
		public void repaint() {
			repaintCount++;
			super.repaint();
		}
	}

	private static final class RecordingRowHeader extends SpreadSheetRowHeader {
		private int repaintCount;

		private RecordingRowHeader(CommonSpreadSheet table) {
			super(table);
		}

		@Override
		public void repaint() {
			repaintCount++;
			super.repaint();
		}
	}
}
