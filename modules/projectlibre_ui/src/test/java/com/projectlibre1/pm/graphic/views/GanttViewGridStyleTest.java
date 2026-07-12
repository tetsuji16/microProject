package com.projectlibre1.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.util.FlatUiSupport;

class GanttViewGridStyleTest {
	@Test
	void spreadsheetGridStyleKeepsHorizontalLinesOff() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			Color gridColor = FlatUiSupport.tableGridColor();

			GanttView.applySpreadsheetGridStyle(sheet, null, true, gridColor);
			assertFalse(sheet.getShowHorizontalLines());
			assertTrue(sheet.getShowVerticalLines());

			GanttView.applySpreadsheetGridStyle(sheet, null, false, gridColor);
			assertFalse(sheet.getShowHorizontalLines());
			assertFalse(sheet.getShowVerticalLines());
		});
	}
}
