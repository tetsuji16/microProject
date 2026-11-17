/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.IconManager;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.theme.MicroProjectThemeTokens;
import com.microproject.ui.theme.ThemeManager;
import com.microproject.util.FlatUiSupport;

/** Fixed-ink information and custom-fields icons must stay legible in both themes (#1218). */
class RibbonIconThemeContrastGuiAcceptanceTest {
	private static final double MINIMUM_CONTRAST = 3.0;
	private static final List<String> RIBBON_COMMANDS = List.of(
		"RibbonTaskInformation", "RibbonResourceInformation", "RibbonProjectInformation", "RibbonCustomFields");

	@Test
	void informationAndCustomFieldsIconsStayLegibleInBothThemes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		boolean originalDark = FlatUiSupport.isDarkTheme();
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(3)) {
			assertTheme(fixture, false, "light");
			assertTheme(fixture, true, "dark");
		} finally {
			ThemeManager.applyTheme(originalDark);
		}
	}

	private static void assertTheme(RibbonCommandGuiFixture fixture, boolean dark, String theme) throws Exception {
		ThemeManager.applyTheme(dark);
		fixture.robot.waitForIdle();
		MicroProjectThemeTokens tokens = MicroProjectTheme.tokens();
		Color surface = tokens.ribbonSurfaceBackground();
		assertTrue(FlatUiSupport.isDarkTheme() == dark, "the " + theme + " theme must be active");
		capture(fixture, theme);

		for (String commandId : RIBBON_COMMANDS) {
			AbstractButton button = fixture.showCommand(commandId);
			assertNotNull(button, theme + " " + commandId + " must be visible in the ribbon");
			Icon icon = fixture.onEdt(button::getIcon);
			assertNotNull(icon, theme + " " + commandId + " must have a ribbon icon");
			Color ink = dominantInk(icon);
			if (!dark)
				assertEquals(new Color(0x212121), ink, theme + " " + commandId + " must keep its light ink");
			double contrast = contrast(ink, surface);
			assertTrue(contrast >= MINIMUM_CONTRAST, theme + " " + commandId + " icon contrast " + contrast
				+ " against " + surface + " is below " + MINIMUM_CONTRAST);
		}

		// The spreadsheet row menu and the Backstage information card both resolve the same
		// icon resource through IconManager.getRibbonIcon; check those resolutions as well.
		Icon menuIcon = IconManager.getRibbonIcon("ribbon.information", 16, 16);
		assertNotNull(menuIcon, "the information menu icon must resolve");
		double menuContrast = contrast(dominantInk(menuIcon), tokens.tableBackground());
		assertTrue(menuContrast >= MINIMUM_CONTRAST, theme + " row-menu information icon contrast " + menuContrast
			+ " is below " + MINIMUM_CONTRAST);
		Icon cardIcon = IconManager.getRibbonIcon("ribbon.information", 32, 32);
		assertNotNull(cardIcon, "the Backstage information card icon must resolve");
		double cardContrast = contrast(dominantInk(cardIcon), surface);
		assertTrue(cardContrast >= MINIMUM_CONTRAST, theme + " Backstage information card icon contrast " + cardContrast
			+ " is below " + MINIMUM_CONTRAST);
	}

	private static Color dominantInk(Icon icon) {
		BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D graphics = image.createGraphics();
		try {
			icon.paintIcon(new JLabel(), graphics, 0, 0);
		} finally {
			graphics.dispose();
		}
		Map<Integer, Integer> colors = new HashMap<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if ((argb >>> 24) >= 200) colors.merge(argb & 0xFFFFFF, 1, Integer::sum);
			}
		}
		assertTrue(!colors.isEmpty(), "a ribbon icon must have opaque ink pixels");
		int ink = colors.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
		return new Color(ink);
	}

	private static double contrast(Color ink, Color background) {
		double first = luminance(ink);
		double second = luminance(background);
		if (first < second) {
			double swap = first;
			first = second;
			second = swap;
		}
		return (first + 0.05) / (second + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
	}

	private static double linear(int value) {
		double channel = value / 255.0;
		return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
	}

	private static void capture(RibbonCommandGuiFixture fixture, String theme) throws Exception {
		Rectangle bounds = fixture.onEdt(() -> new Rectangle(fixture.window.getLocationOnScreen(), fixture.window.getSize()));
		Robot robot = fixture.robot;
		BufferedImage image = robot.createScreenCapture(bounds);
		Path output = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"),
			"issue-1218-" + theme + "-ribbon.png");
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());
	}
}
