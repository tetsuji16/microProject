/*
 * Copyright (c) 2026 microProject
 * Licensed under the MIT License.
 */
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SpreadSheetGridVisibilityTest {
	@Test
	void gridVisibilityTogglesBothDirectionsOfTheSheet() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();

			sheet.setGridVisible(false);
			assertFalse(sheet.isGridVisible());
			assertFalse(sheet.getShowHorizontalLines());
			assertFalse(sheet.getShowVerticalLines());

			sheet.setGridVisible(true);
			assertTrue(sheet.isGridVisible());
			assertTrue(sheet.getShowHorizontalLines());
			assertTrue(sheet.getShowVerticalLines());
		});
	}
}
