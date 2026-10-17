/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.BarFormat;

class MicrosoftProjectGanttPaletteTest {
	private final MicrosoftProjectGanttPalette palette = new MicrosoftProjectGanttPalette();

	@Test
	void usesMicrosoftProjectColorsForTheGanttSurfaceAndTaskBars() {
		assertEquals(new Color(0x4472C4), palette.getTaskBar(null));
		assertEquals(new Color(0xD9E2F3), palette.getGridLine());
		assertEquals(new Color(0xD9E2F3), palette.getProgressTrackColor(MicrosoftProjectGanttTheme.TASK));
		assertEquals(new Color(0x2F5597), palette.getProgressFillColor(MicrosoftProjectGanttTheme.TASK));
		assertEquals(new Color(0xC00000), palette.getCriticalTaskColor());
	}

	@Test
	void mapsSummaryAndBaselineFormatsToTheirMicrosoftProjectColors() {
		assertEquals(new Color(0x404040), palette.getAccentColor(format("Bar.summary"), null, null));
		assertEquals(new Color(0x7F7F7F), palette.getAccentColor(format("Bar.baseline"), null, null));
	}

	@Test
	void darkThemeUsesDarkSurfaceAndReadableLinesInsteadOfLightColors() {
		try {
			com.microproject.ui.theme.MicroProjectTheme.installDark();
			Color background = palette.getChartBackground();
			assertEquals(FlatUiSupport.spreadsheetBodyBackground(), background);
			org.junit.jupiter.api.Assertions.assertTrue(luminance(background) < 0.2, "dark chart surface");
			org.junit.jupiter.api.Assertions.assertTrue(luminance(palette.getDependencyLinkColor()) > 0.4, "dependency links readable");
			org.junit.jupiter.api.Assertions.assertTrue(luminance(palette.getSummaryProgressColor(null)) > 0.4, "summary bars readable");
			org.junit.jupiter.api.Assertions.assertNotEquals(new Color(0xD9E2F3), palette.getGridLine());
			org.junit.jupiter.api.Assertions.assertTrue(luminance(MicrosoftProjectGanttTheme.nonWorkingFill()) < 0.3, "non-working shading stays dark");
		} finally {
			com.microproject.ui.theme.MicroProjectTheme.installLight();
		}
		assertEquals(Color.WHITE, palette.getChartBackground());
	}

	private static double luminance(Color c) {
		return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
	}

	private static BarFormat format(String id) {
		BarFormat format = new BarFormat();
		format.setId(id);
		return format;
	}
}
