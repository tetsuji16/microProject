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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.util.FlatUiSupport;

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

	@Test
	void headerHighlightUsesClickedActiveColumnAfterWholeRowSelection() throws Exception {
		final TestSpreadSheet[] sheetRef = new TestSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new TestSpreadSheet(2));
		TestSpreadSheet sheet = sheetRef[0];

		SwingUtilities.invokeAndWait(() -> {
			sheet.selectRowAndAllColumns(1);
			sheet.setRowHeaderSelectionActive(false);
			sheet.getSelection().setActiveCell(1, 1);
		});

		assertEquals(1, sheet.getActiveHeaderColumn(),
			"a task-cell row selection must highlight the clicked view column");
		assertEquals(0, sheet.getSelectedColumn(),
			"the regression must cover JTable's collapsed lead column");

		SwingUtilities.invokeAndWait(() -> sheet.selectColumnAndAllRows(0));
		assertEquals(0, sheet.getActiveHeaderColumn(),
			"an explicit column-header selection must highlight its selected column");

		SwingUtilities.invokeAndWait(() -> sheet.selectEntireSpreadsheet());
		assertEquals(-1, sheet.getActiveHeaderColumn(),
			"an entire-sheet selection must not claim a single active column");
	}

	@Test
	void currentViewColumnDoesNotUseTheRightmostLeadOfAWholeRowSelection() throws Exception {
		final TestSpreadSheet[] sheetRef = new TestSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new TestSpreadSheet(2));
		TestSpreadSheet sheet = sheetRef[0];

		SwingUtilities.invokeAndWait(() -> {
			sheet.selectRowAndAllColumns(1);
			sheet.setRowHeaderSelectionActive(false);
			sheet.getSelection().setActiveCell(1, 1);
		});
		assertEquals(1, sheet.getCurrentViewColumn());

		SwingUtilities.invokeAndWait(() -> sheet.getSelection().clearActiveCell());
		assertEquals(0, sheet.getCurrentViewColumn(),
			"a one-row selection must use the stable first cell, not JTable's lead column");

		SwingUtilities.invokeAndWait(() -> sheet.getColumnModel().getSelectionModel().setSelectionInterval(1, 1));
		assertEquals(1, sheet.getCurrentViewColumn(),
			"an explicit single-column selection remains a valid current column");

		SwingUtilities.invokeAndWait(() -> sheet.selectEntireSpreadsheet());
		assertEquals(-1, sheet.getCurrentViewColumn(),
			"an entire-sheet selection must not invent a current cell column");
	}

	@Test
	void commonHeaderRendererUsesTheSameActiveColumnResolver() throws Exception {
		final TestSpreadSheet[] sheetRef = new TestSpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = new TestSpreadSheet(2));
		TestSpreadSheet sheet = sheetRef[0];
		SwingUtilities.invokeAndWait(() -> {
			sheet.selectRowAndAllColumns(1);
			sheet.setRowHeaderSelectionActive(false);
			sheet.getSelection().setActiveCell(1, 1);
		});

		CommonTableHeader header = new CommonTableHeader(sheet.getColumnModel());
		javax.swing.table.TableCellRenderer renderer = header.getDefaultRenderer();
		JComponent active = (JComponent) renderer.getTableCellRendererComponent(sheet, null, false, false, -1, 1);
		java.awt.Color activeBackground = active.getBackground();
		JComponent first = (JComponent) renderer.getTableCellRendererComponent(sheet, null, false, false, -1, 0);
		assertEquals(FlatUiSupport.spreadsheetHeaderSelectedBackground(), activeBackground);
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), first.getBackground());
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
