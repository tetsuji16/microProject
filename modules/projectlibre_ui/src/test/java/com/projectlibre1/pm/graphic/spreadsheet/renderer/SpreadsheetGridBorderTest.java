package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
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
	void rowHeaderUsesOneNativeHorizontalGridLine() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			SpreadSheetRowHeader rowHeader = new SpreadSheetRowHeader(sheet);

			rowHeader.updateUI();

			assertTrue(rowHeader.getShowHorizontalLines());
			assertFalse(rowHeader.getShowVerticalLines());
			assertEquals(sheet.getGridColor(), rowHeader.getGridColor());

			JComponent component = (JComponent) new SpreadSheetRowHeaderRenderer()
				.getTableCellRendererComponent(rowHeader, "1", false, false, 0, 0);
			assertFalse(hasBottomSeparator(component.getBorder(), rowHeader.getGridColor()));
		});
	}

	@Test
	void spreadsheetTablesUseNativeGridLinesInBothDirections() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			assertTrue(sheet.getShowHorizontalLines());
			assertTrue(sheet.getShowVerticalLines());
		});
	}

	@Test
	void nativeGridPaintsAContinuousSinglePixelIntersection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(2, 2));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			Color gridColor = new Color(0xCC, 0x22, 0x99);
			table.setGridColor(gridColor);
			table.setRowHeight(20);
			table.getColumnModel().getColumn(0).setPreferredWidth(40);
			table.getColumnModel().getColumn(1).setPreferredWidth(40);
			table.setSize(80, 40);
			table.doLayout();

			BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			try {
				table.paint(graphics);
			} finally {
				graphics.dispose();
			}

			int horizontalY = table.getCellRect(0, 0, true).y
				+ table.getCellRect(0, 0, true).height - 1;
			int verticalX = table.getCellRect(0, 0, true).x
				+ table.getCellRect(0, 0, true).width - 1;
			assertEquals(gridColor.getRGB(), image.getRGB(verticalX, horizontalY));
			assertEquals(gridColor.getRGB(), image.getRGB(verticalX - 1, horizontalY));
			assertEquals(gridColor.getRGB(), image.getRGB(verticalX, horizontalY - 1));
		});
	}

	@Test
	void rowHeaderDoesNotDrawVerticalGridLines() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			SpreadSheetRowHeader rowHeader = new SpreadSheetRowHeader(sheet);

			assertFalse(rowHeader.getShowVerticalLines());
		});
	}

	@Test
	void indicatorsRendererLeavesGridPaintingToTable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			CommonSpreadSheet sheet = new CommonSpreadSheet();
			Color separatorColor = sheet.getGridColor();

			assertFalse(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, false, false), separatorColor));
			assertFalse(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, true, false), separatorColor));
			assertFalse(hasBottomSeparator(IndicatorsRenderer.resolveCellBorder(sheet, false, true), separatorColor));
		});
	}

	@Test
	void cellBordersAreNotShrunkByAnExtraRowSeparator() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(new Object[][] { { "Task", new Date() } }, new Object[] { "Name", "Date" }));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			assertFalse(hasBottomSeparator(
				CellUtility.resolveCellBorder(false, true, false), table.getGridColor()));
		});
	}

	@Test
	void booleanRendererLeavesGridPaintingToTable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(new Object[][] { { Boolean.TRUE } }, new Object[] { "Done" }));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			Color separatorColor = table.getGridColor();

			JComponent component = (JComponent) new OfflineCapableBooleanRenderer()
				.getTableCellRendererComponent(table, Boolean.TRUE, false, false, 0, 0);

			assertFalse(hasBottomSeparator(component.getBorder(), separatorColor));
		});
	}

	@Test
	void specialRendererOverlayPaintsGridWithoutShrinkingSelectionBorder() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			PlainTable table = new PlainTable();
			table.setModel(new DefaultTableModel(1, 1));
			FlatUiSupport.applySpreadsheetTableStyle(table);
			Color gridColor = new Color(0xCC, 0x22, 0x99);
			table.setGridColor(gridColor);
			Border activeBorder = FlatUiSupport.spreadsheetActiveCellBorder();
			Border activeOverlay = CellUtility.withRowGridOverlay(table, activeBorder);
			Border gridOverlay = CellUtility.withRowGridOverlay(table, null);

			assertEquals(activeBorder.getBorderInsets(table), activeOverlay.getBorderInsets(table));

			BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			try {
				gridOverlay.paintBorder(table, graphics, 0, 0, 40, 20);
			} finally {
				graphics.dispose();
			}
			Insets insets = activeOverlay.getBorderInsets(table);
			assertEquals(1, insets.bottom);
			assertEquals(gridColor.getRGB(), image.getRGB(20, 19));

			graphics = image.createGraphics();
			try {
				activeOverlay.paintBorder(table, graphics, 0, 0, 40, 20);
			} finally {
				graphics.dispose();
			}
			assertEquals(FlatUiSupport.spreadsheetActiveCellBorderColor().getRGB(), image.getRGB(0, 19));
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
