/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel.Zone;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/** Issue #1114: the buffer chart zones follow the theme and keep their labels readable. */
class CriticalChainBufferChartPanelThemeTest {
	private static final double MINIMUM_TEXT_CONTRAST = 4.5;
	private boolean originalDark;

	@BeforeEach
	void captureTheme() {
		originalDark = FlatUiSupport.isDarkTheme();
	}

	@AfterEach
	void restoreTheme() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (originalDark)
				MicroProjectTheme.installDark();
			else
				MicroProjectTheme.installLight();
		});
	}

	@Test
	void zoneColoursFollowTheThemeAndKeepTheLabelsReadable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MicroProjectTheme.installLight();
			Color[] light = zoneColours();
			for (Zone zone : Zone.values()) {
				assertFalse(ColorLuminance.isDark(light[zone.ordinal()]), "light " + zone);
				assertReadable(zone);
			}

			MicroProjectTheme.installDark();
			for (Zone zone : Zone.values()) {
				Color dark = CriticalChainBufferChartPanel.zoneColor(zone);
				assertNotEquals(light[zone.ordinal()], dark, "the zone must change with the theme: " + zone);
				assertTrue(ColorLuminance.isDark(dark), "dark " + zone);
				assertReadable(zone);
			}

			MicroProjectTheme.installLight();
			for (Zone zone : Zone.values())
				assertFalse(ColorLuminance.isDark(CriticalChainBufferChartPanel.zoneColor(zone)), "light again " + zone);
		});
	}

	private static Color[] zoneColours() {
		Color[] colours = new Color[Zone.values().length];
		for (Zone zone : Zone.values())
			colours[zone.ordinal()] = CriticalChainBufferChartPanel.zoneColor(zone);
		return colours;
	}

	private static void assertReadable(Zone zone) {
		double text = ColorLuminance.relativeLuminance(CriticalChainBufferChartPanel.textColor());
		double fill = ColorLuminance.relativeLuminance(CriticalChainBufferChartPanel.zoneColor(zone));
		double contrast = (Math.max(text, fill) + 0.05) / (Math.min(text, fill) + 0.05);
		assertTrue(contrast >= MINIMUM_TEXT_CONTRAST, zone + " label contrast " + contrast);
	}
}
