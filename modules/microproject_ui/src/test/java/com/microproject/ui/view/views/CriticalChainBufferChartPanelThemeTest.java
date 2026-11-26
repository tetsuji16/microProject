/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.schedule.ccpm.CriticalChainService;
import com.microproject.ui.view.views.CriticalChainBufferChartPanel.Zone;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.util.ColorLuminance;
import com.microproject.ui.util.FlatUiSupport;

/** Issue #1114: the buffer chart zones follow the theme and keep their labels readable. */
class CriticalChainBufferChartPanelThemeTest {
	private static final double MINIMUM_TEXT_CONTRAST = 4.5;
	/** Sum of RGB channel differences that keeps two large fills visibly apart. */
	private static final int MINIMUM_BAND_SEPARATION = 40;
	private static final int LEFT = 64;
	private static final int RIGHT = 26;
	private static final int TOP = 30;
	private static final int BOTTOM = 52;

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

	@Test
	void selectedObservationOutlineStandsOutFromEveryZoneInBothThemes() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (boolean dark : new boolean[] { false, true }) {
				if (dark)
					MicroProjectTheme.installDark();
				else
					MicroProjectTheme.installLight();
				double outline = ColorLuminance.relativeLuminance(CriticalChainBufferChartPanel.selectionColor());
				for (Zone zone : Zone.values()) {
					double fill = ColorLuminance.relativeLuminance(CriticalChainBufferChartPanel.zoneColor(zone));
					double contrast = (Math.max(outline, fill) + 0.05) / (Math.min(outline, fill) + 0.05);
					assertTrue(contrast >= 3.0, (dark ? "dark " : "light ") + zone + " outline contrast " + contrast);
				}
			}
		});
	}

	/**
	 * Issue #1364: the dark-theme buffer chart itself, painted headlessly, must
	 * show three separable bands on the dark surface with readable labels.
	 */
	@Test
	void darkThemePaintsSeparableBandsAndReadableLabelsOnTheSurface() throws Exception {
		BufferedImage[] painted = new BufferedImage[1];
		SwingUtilities.invokeAndWait(() -> painted[0] = paintDarkChart());
		BufferedImage capture = painted[0];

		int chartWidth = capture.getWidth() - LEFT - RIGHT;
		int chartHeight = capture.getHeight() - TOP - BOTTOM;
		Color surface = new Color(capture.getRGB(capture.getWidth() - 6, capture.getHeight() - 6));
		Color text = CriticalChainBufferChartPanel.textColor();
		for (Zone zone : Zone.values()) {
			Color fill = CriticalChainBufferChartPanel.zoneColor(zone);
			Color band = new Color(capture.getRGB(xFor(10D, chartWidth), yFor(progressFor(zone), chartHeight)));
			assertEquals(fill, band, "the painted band must use the dark-theme zone colour: " + zone);
			assertTrue(colourDistance(fill, surface) >= MINIMUM_BAND_SEPARATION,
				zone + " band must stand out from the dark surface");
			assertTrue(contrast(text, fill) >= MINIMUM_TEXT_CONTRAST,
				zone + " label must stay readable on its band");
		}
		assertTrue(contrast(text, surface) >= MINIMUM_TEXT_CONTRAST, "labels must stay readable on the surface");
		Color green = CriticalChainBufferChartPanel.zoneColor(Zone.GREEN);
		Color amber = CriticalChainBufferChartPanel.zoneColor(Zone.AMBER);
		Color red = CriticalChainBufferChartPanel.zoneColor(Zone.RED);
		assertTrue(colourDistance(green, amber) >= MINIMUM_BAND_SEPARATION, "green and amber bands must differ");
		assertTrue(colourDistance(amber, red) >= MINIMUM_BAND_SEPARATION, "amber and red bands must differ");
		assertTrue(colourDistance(green, red) >= MINIMUM_BAND_SEPARATION, "green and red bands must differ");
	}

	/** Paints the chart the way the status dialog does, without a display. */
	private static BufferedImage paintDarkChart() {
		MicroProjectTheme.installDark();
		CriticalChainBufferChartPanel panel = new CriticalChainBufferChartPanel(null);
		panel.setSize(620, 420);
		panel.setAnalysis(darkAnalysis(), true);
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			panel.paint(graphics);
		} finally {
			graphics.dispose();
		}
		return image;
	}

	private static CriticalChainService.Analysis darkAnalysis() {
		CriticalChainService.Buffer buffer = new CriticalChainService.Buffer(1000L, 250L, 750L, 0.25D,
			CriticalChainService.BufferStatus.GREEN, CriticalChainService.BufferKind.PROJECT);
		return new CriticalChainService.Analysis(null, List.of(), 1000L, Map.of(), buffer, Map.of(), Map.of(), List.of());
	}

	/** A consumption value that sits well inside the requested band at 10% progress. */
	private static double progressFor(Zone zone) {
		return switch (zone) {
			case GREEN -> 5D;
			case AMBER -> 25D;
			case RED -> 60D;
		};
	}

	private static int xFor(double percent, int width) {
		return LEFT + (int) Math.round(percent * width / 100D);
	}

	private static int yFor(double percent, int height) {
		return TOP + height - (int) Math.round(percent * height / 100D);
	}

	private static int colourDistance(Color first, Color second) {
		return Math.abs(first.getRed() - second.getRed()) + Math.abs(first.getGreen() - second.getGreen())
				+ Math.abs(first.getBlue() - second.getBlue());
	}

	private static double contrast(Color first, Color second) {
		double firstLuminance = ColorLuminance.relativeLuminance(first);
		double secondLuminance = ColorLuminance.relativeLuminance(second);
		return (Math.max(firstLuminance, secondLuminance) + 0.05) / (Math.min(firstLuminance, secondLuminance) + 0.05);
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
