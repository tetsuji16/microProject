/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.FlatUiSupport;

class ChartHeaderThemeTest {
	@BeforeAll
	static void installTheme() {
		MicroProjectTheme.installLight();
	}

	@Test
	void chartHeaderComponentsUseSharedTableHeaderStyle() {
		ChartCorner corner = new ChartCorner(null);
		AxisPanel axisPanel = new AxisPanel(null);

		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), corner.getBackground());
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), axisPanel.getBackground());
		assertEquals(FlatUiSupport.tableHeaderBorder(), corner.getBorder());
		assertEquals(FlatUiSupport.tableHeaderBorder(), axisPanel.getBorder());
	}
}
