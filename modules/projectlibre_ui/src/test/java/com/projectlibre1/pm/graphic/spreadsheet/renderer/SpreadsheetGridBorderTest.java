package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.common.SpreadSheetRowHeader;
import com.projectlibre1.util.FlatUiSupport;

class SpreadsheetGridBorderTest {
	private static final class PlainTable extends JTable {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void rowHeaderTracksSpreadsheetGridSettingsAndKeepsBottomBorder() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			SpreadSheetRowHeader rowHeader = new SpreadSheetRowHeader(sheet);

			rowHeader.updateUI();

			assertTrue(rowHeader.getShowHorizontalLines());
			assertFalse(rowHeader.getShowVerticalLines());
			assertEquals(sheet.getGridColor(), rowHeader.getGridColor());

			JComponent component = (JComponent) new SpreadSheetRowHeaderRenderer()
				.getTableCellRendererComponent(rowHeader, "1", false, false, 0, 0);
			assertTrue(hasBottomSeparator(component.getBorder(), rowHeader.getGridColor()));
		});
	}

	@Test
	void nameCellComponentAddsBottomSeparatorWhenGridlinesAreEnabled() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			NameCellComponent component = NameCellComponent.getInstance();
			Color separatorColor = new Color(0x33, 0x66, 0x99);

			component.syncRowSeparator(true, separatorColor);

			assertTrue(hasBottomSeparator(component.getBorder(), separatorColor));
		});
	}

	@Test
	void spreadsheetTablesShowVerticalLinesByDefaultWhileRowHeaderDoesNot() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			SpreadSheetRowHeader rowHeader = new SpreadSheetRowHeader(sheet);

			assertFalse(sheet.getShowHorizontalLines());
			assertTrue(sheet.getShowVerticalLines());
			assertFalse(rowHeader.getShowVerticalLines());
		});
	}

	@Test
	void indicatorsRendererKeepsBottomSeparatorForNormalSelectedAndFocusedStates() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			Color separatorColor = sheet.getGridColor();

			assertTrue(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, false, false), separatorColor));
			assertTrue(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, true, false), separatorColor));
			assertTrue(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, false, true), separatorColor));
		});
	}

	@Test
	void cellUtilityAddsBottomSeparatorForNormalAndFocusedBorders() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(new Object[][] { { "Task", new Date() } }, new Object[] { "Name", "Date" }));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			Color separatorColor = table.getGridColor();

			assertTrue(hasBottomSeparator(CellUtility.withSpreadsheetGrid(table, FlatUiSupport.tableCellBorder()), separatorColor));
			assertTrue(hasBottomSeparator(CellUtility.withSpreadsheetGrid(table, FlatUiSupport.spreadsheetActiveCellBorder()), separatorColor));
		});
	}

	@Test
	void booleanRendererUsesSpreadsheetRowSeparatorBorder() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(new Object[][] { { Boolean.TRUE } }, new Object[] { "Done" }));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			Color separatorColor = table.getGridColor();

			JComponent component = (JComponent) new OfflineCapableBooleanRenderer()
				.getTableCellRendererComponent(table, Boolean.TRUE, false, false, 0, 0);

			assertTrue(hasBottomSeparator(component.getBorder(), separatorColor));
		});
	}

	private static boolean hasBottomSeparator(Border border, Color expectedColor) {
		if (border instanceof MatteBorder matteBorder) {
			return matteBorder.getBorderInsets().bottom > 0 && expectedColor.equals(matteBorder.getMatteColor());
		}
		if (border instanceof CompoundBorder compoundBorder) {
			return hasBottomSeparator(compoundBorder.getOutsideBorder(), expectedColor)
				|| hasBottomSeparator(compoundBorder.getInsideBorder(), expectedColor);
		}
		return false;
	}
}
