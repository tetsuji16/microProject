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
package com.microproject.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.util.FlatUiSupport;

class SpreadsheetSelectionAppearanceTest {
	private static final class PlainTable extends JTable {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void spreadsheetTablesUseExcelLikeBodyAndRangeSelectionColors() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			assertEquals(FlatUiSupport.spreadsheetBodyBackground(), sheet.getBackground());
			assertEquals(FlatUiSupport.spreadsheetRangeSelectionBackground(), sheet.getSelectionBackground());
			assertFalse(sheet.getBackground().equals(sheet.getSelectionBackground()));
		});
	}

	@Test
	void cellUtilityPrefersEditingThenActiveThenSelectionBorders() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			LineBorder editing = assertInstanceOf(LineBorder.class, CellUtility.resolveCellBorder(true, true, true));
			LineBorder active = assertInstanceOf(LineBorder.class, CellUtility.resolveCellBorder(false, true, true));
			assertEquals(2, editing.getThickness());
			assertEquals(1, active.getThickness());
			assertEquals(FlatUiSupport.spreadsheetActiveCellBorderColor(), editing.getLineColor());
			assertEquals(FlatUiSupport.spreadsheetActiveCellBorderColor(), active.getLineColor());
			assertEquals(FlatUiSupport.spreadsheetRangeSelectionBackground(), CellUtility.resolveSelectionBackground(null));
			assertEquals(FlatUiSupport.spreadsheetBodyBackground(), CellUtility.resolveTableBackground(null, 4, false));
			Color softened = CellUtility.resolveTableBackground(new Color(0x19, 0x24, 0x33), 4, false);
			assertTrue(softened.getRed() > 0x19);
			assertTrue(softened.getGreen() > 0x24);
			assertTrue(softened.getBlue() > 0x33);
			assertEquals(FlatUiSupport.spreadsheetRangeSelectionBackground(), CellUtility.resolveTableBackground(new Color(0x19, 0x24, 0x33), 4, true));
		});
	}

	@Test
	void rowHeaderRendererUsesSelectedHeaderBackgroundAndActiveOutline() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable rowHeader = new PlainTable();
			rowHeader.setModel(new DefaultTableModel(new Object[][] { { "1" }, { "2" } }, new Object[] { "#" }));
			rowHeader.setGridColor(FlatUiSupport.spreadsheetGridColor());
			rowHeader.getSelectionModel().setSelectionInterval(1, 1);

			JComponent component = (JComponent) new SpreadSheetRowHeaderRenderer()
				.getTableCellRendererComponent(rowHeader, "2", true, false, 1, 0);

			assertEquals(FlatUiSupport.spreadsheetHeaderSelectedBackground(), component.getBackground());
			LineBorder border = assertInstanceOf(LineBorder.class, component.getBorder());
			assertEquals(FlatUiSupport.spreadsheetActiveCellBorderColor(), border.getLineColor());
		});
	}

	@Test
	void columnHeaderRendererUsesSelectedHeaderBackgroundAndActiveOutline() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(new Object[][] { { "A", "B" } }, new Object[] { "Name", "Start" }));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			table.getColumnModel().getSelectionModel().setSelectionInterval(1, 1);

			JLabel component = (JLabel) new SpreadSheetColumnHeaderRenderer()
				.getTableCellRendererComponent(table, "Start", true, false, -1, 1);

			assertEquals(FlatUiSupport.spreadsheetHeaderSelectedBackground(), component.getBackground());
			assertTrue(component.getBorder() instanceof LineBorder);
		});
	}
}
