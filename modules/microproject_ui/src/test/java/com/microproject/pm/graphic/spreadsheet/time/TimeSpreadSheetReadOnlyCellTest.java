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
package com.microproject.pm.graphic.spreadsheet.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.VariableHeightLayoutCache;

import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/** Issue #1125: a time-phased cell that cannot be edited follows the theme. */
class TimeSpreadSheetReadOnlyCellTest {
	private static final double MINIMUM_TEXT_CONTRAST = 4.5;
	/** The fixed fill this fix removed; it must not return. */
	private static final Color LEGACY_FIXED_READ_ONLY_BACKGROUND = new Color(0xD3D3D3);

	@Test
	void readOnlyCellStaysReadableInTheLightTheme() {
		MicroProjectTheme.installLight();

		assertReadOnlyCellIsReadable();
		assertFalse(ColorLuminance.isDark(FlatUiSupport.spreadsheetReadOnlyBackground()),
			"the light theme must not paint a dark read-only cell");
	}

	@Test
	void readOnlyCellStaysReadableInTheDarkTheme() {
		try {
			MicroProjectTheme.installDark();

			assertReadOnlyCellIsReadable();
			assertTrue(ColorLuminance.isDark(FlatUiSupport.spreadsheetReadOnlyBackground()),
				"the dark theme must not paint a light read-only cell");
		} finally {
			MicroProjectTheme.installLight();
		}
	}

	@Test
	void readOnlyBackgroundFollowsTheTheme() {
		MicroProjectTheme.installLight();
		Color light = FlatUiSupport.spreadsheetReadOnlyBackground();
		try {
			MicroProjectTheme.installDark();

			assertNotEquals(light, FlatUiSupport.spreadsheetReadOnlyBackground(),
				"a read-only cell must not keep the previous theme's background");
		} finally {
			MicroProjectTheme.installLight();
		}
	}

	@Test
	void selectedAndEditableCellsKeepTheRendererColours() {
		MicroProjectTheme.installLight();
		Color rendererBackground = new Color(0x123456);

		JLabel selected = cell(rendererBackground);
		TimeSpreadSheet.applyReadOnlyBackground(selected, false, true);
		assertEquals(rendererBackground, selected.getBackground());

		JLabel editable = cell(rendererBackground);
		TimeSpreadSheet.applyReadOnlyBackground(editable, true, false);
		assertEquals(rendererBackground, editable.getBackground());
	}

	/** The grid must apply the fix itself, not only the extracted helper. */
	@Test
	void theGridPaintsReadOnlyCellsWithTheThemeBackground() throws Exception {
		MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			JLabel readOnly = paintedCell(false, false);
			assertEquals(FlatUiSupport.spreadsheetReadOnlyBackground(), readOnly.getBackground());
			assertNotEquals(LEGACY_FIXED_READ_ONLY_BACKGROUND, readOnly.getBackground(),
				"the fixed light grey fill is the bug this test guards");

			Color rendererBackground = new Color(0x123456);
			assertEquals(rendererBackground, paintedCell(false, true).getBackground(),
				"a selected read-only cell keeps the selection colours");
			assertEquals(rendererBackground, paintedCell(true, false).getBackground(),
				"an editable cell keeps the renderer colours");
		});
	}

	private static void assertReadOnlyCellIsReadable() {
		JLabel cell = cell(FlatUiSupport.spreadsheetBodyBackground());
		// CellUtility paints spreadsheetReadOnlyForeground() on a read-only cell,
		// so this is the text colour a user actually reads on this background.
		cell.setForeground(FlatUiSupport.spreadsheetReadOnlyForeground());

		TimeSpreadSheet.applyReadOnlyBackground(cell, false, false);

		assertEquals(FlatUiSupport.spreadsheetReadOnlyBackground(), cell.getBackground());
		assertNotEquals(LEGACY_FIXED_READ_ONLY_BACKGROUND, cell.getBackground(),
			"the read-only background must come from the theme");
		double contrast = contrast(cell.getForeground(), cell.getBackground());
		assertTrue(contrast >= MINIMUM_TEXT_CONTRAST,
			"text contrast on a read-only cell was " + contrast);
	}

	private static JLabel paintedCell(boolean editable, boolean selected) {
		TimeSpreadSheet sheet = new TimeSpreadSheet(null);
		sheet.setModel(new StubTimeModel(editable));
		sheet.getColumnModel().addColumn(new TableColumn(0));
		sheet.setCellSelectionEnabled(true);
		if (selected)
			sheet.changeSelection(0, 0, false, false);
		Color rendererBackground = new Color(0x123456);
		TableCellRenderer renderer = (table, value, isSelected, hasFocus, row, column) -> cell(rendererBackground);
		return (JLabel) sheet.prepareRenderer(renderer, 0, 0);
	}

	private static final class StubTimeModel extends TimeSpreadSheetModel {
		private static final long serialVersionUID = 1L;

		private final boolean editable;
		private final AbstractLayoutCache layout = new VariableHeightLayoutCache();

		StubTimeModel(boolean editable) {
			super(null, new ArrayList<>(), null, null);
			this.editable = editable;
		}

		/** Outline.setModel needs a layout; the stub has no node model cache. */
		@Override
		public AbstractLayoutCache getLayout() {
			return layout;
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
		public Object getValueAt(int row, int column) {
			return "8h";
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return editable;
		}
	}

	private static JLabel cell(Color background) {
		JLabel cell = new JLabel("8h");
		cell.setOpaque(true);
		cell.setBackground(background);
		return cell;
	}

	private static double contrast(Color first, Color second) {
		double a = ColorLuminance.relativeLuminance(first);
		double b = ColorLuminance.relativeLuminance(second);
		return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
	}
}
