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
import com.microproject.util.ColorLuminance;
import com.microproject.util.FlatUiSupport;

/**
 * Fixed-ink ribbon icons must stay legible in both themes. The light theme keeps the intended
 * ink of each asset; the dark theme remaps it to the ribbon icon ink (#1218, #1221).
 */
class RibbonIconThemeContrastGuiAcceptanceTest {
	private static final double MINIMUM_CONTRAST = 3.0;
	/** A command, the light-theme ink its asset deliberately uses, and whether a task must be selected. */
	private record CommandIcon(String commandId, int lightInk, boolean needsSelectedTask) { }
	private static final int LIGHT_SURFACE_INK = 0x212121;
	private static final int BRAND_NAVY_INK = 0x1F3B73;
	private static final int BRAND_GREEN_INK = 0x2F6B53;
	private static final List<CommandIcon> RIBBON_COMMANDS = List.of(
		new CommandIcon("RibbonTaskInformation", LIGHT_SURFACE_INK, false),
		new CommandIcon("RibbonResourceInformation", LIGHT_SURFACE_INK, false),
		new CommandIcon("RibbonProjectInformation", LIGHT_SURFACE_INK, false),
		new CommandIcon("RibbonCustomFields", LIGHT_SURFACE_INK, false),
		new CommandIcon("RibbonBold", BRAND_NAVY_INK, true),
		new CommandIcon("RibbonItalic", BRAND_NAVY_INK, true),
		new CommandIcon("RibbonUnderline", BRAND_NAVY_INK, true));
	/** The alignment group lives on the Format tab, so it is measured and captured separately. */
	private static final List<CommandIcon> ALIGN_COMMANDS = List.of(
		new CommandIcon("RibbonAlignLeft", BRAND_GREEN_INK, true),
		new CommandIcon("RibbonAlignCenter", BRAND_GREEN_INK, true),
		new CommandIcon("RibbonAlignRight", BRAND_GREEN_INK, true));
	private static final List<String> FORMAT_ICON_KEYS = List.of("ribbon.bold", "ribbon.italic", "ribbon.underline");
	private static final List<String> ALIGN_ICON_KEYS = List.of("ribbon.alignLeft", "ribbon.alignCenter", "ribbon.alignRight");

	@Test
	void fixedInkRibbonIconsStayLegibleInBothThemes() throws Exception {
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
		// The report describes the font group with a task selected, so the measured icon is the
		// enabled state the user reads rather than the disabled variant.
		fixture.clickRow(0);

		assertCommands(fixture, dark, theme, surface, RIBBON_COMMANDS);
		capture(fixture, theme + "-font-group");

		assertCommands(fixture, dark, theme, surface, ALIGN_COMMANDS);
		capture(fixture, theme + "-align-group");

		// Each fixed-ink asset also ships 16/20/32 px variants; every size must carry the same ink.
		assertSizedInk(theme, dark, surface, FORMAT_ICON_KEYS, BRAND_NAVY_INK);
		assertSizedInk(theme, dark, surface, ALIGN_ICON_KEYS, BRAND_GREEN_INK);

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

	private static void assertCommands(RibbonCommandGuiFixture fixture, boolean dark, String theme, Color surface,
			List<CommandIcon> commands) throws Exception {
		for (CommandIcon command : commands) {
			String commandId = command.commandId();
			AbstractButton button = fixture.showCommand(commandId);
			assertNotNull(button, theme + " " + commandId + " must be visible in the ribbon");
			if (command.needsSelectedTask())
				assertTrue(fixture.onEdt(button::isEnabled),
					theme + " " + commandId + " must be enabled while a task is selected");
			Icon icon = fixture.onEdt(button::getIcon);
			assertNotNull(icon, theme + " " + commandId + " must have a ribbon icon");
			Color ink = dominantInk(icon);
			if (!dark)
				assertEquals(new Color(command.lightInk()), ink, theme + " " + commandId + " must keep its light ink");
			double contrast = contrast(ink, surface);
			assertTrue(contrast >= MINIMUM_CONTRAST, theme + " " + commandId + " icon contrast " + contrast
				+ " against " + surface + " is below " + MINIMUM_CONTRAST);
		}
	}

	private static void assertSizedInk(String theme, boolean dark, Color surface, List<String> iconKeys, int lightInk) {
		for (String iconKey : iconKeys) {
			for (int size : new int[] {16, 20, 32}) {
				Icon sizedIcon = IconManager.getRibbonIcon(iconKey, size, size);
				assertNotNull(sizedIcon, theme + " " + iconKey + " at " + size + " px must resolve");
				Color sizedInk = dominantInk(sizedIcon);
				if (!dark)
					assertEquals(new Color(lightInk), sizedInk,
						theme + " " + iconKey + " at " + size + " px must keep its light ink");
				double sizedContrast = contrast(sizedInk, surface);
				assertTrue(sizedContrast >= MINIMUM_CONTRAST, theme + " " + iconKey + " at " + size
					+ " px contrast " + sizedContrast + " against " + surface + " is below " + MINIMUM_CONTRAST);
			}
		}
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
				// Thin 16 px strokes (the alignment bars) never reach full opacity, so half-opaque
				// coverage still counts as the glyph ink; antialiasing keeps the source colour.
				if ((argb >>> 24) >= 128) colors.merge(argb & 0xFFFFFF, 1, Integer::sum);
			}
		}
		assertTrue(!colors.isEmpty(), "a ribbon icon must have ink pixels");
		int ink = colors.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
		return new Color(ink);
	}

	private static double contrast(Color ink, Color background) {
		double first = ColorLuminance.relativeLuminance(ink) + 0.05;
		double second = ColorLuminance.relativeLuminance(background) + 0.05;
		return Math.max(first, second) / Math.min(first, second);
	}

	private static void capture(RibbonCommandGuiFixture fixture, String artifactName) throws Exception {
		Rectangle bounds = fixture.onEdt(() -> new Rectangle(fixture.window.getLocationOnScreen(), fixture.window.getSize()));
		Robot robot = fixture.robot;
		BufferedImage image = robot.createScreenCapture(bounds);
		Path output = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"),
			"ribbon-icon-contrast-" + artifactName + ".png");
		Files.createDirectories(output.getParent());
		ImageIO.write(image, "png", output.toFile());
	}
}
