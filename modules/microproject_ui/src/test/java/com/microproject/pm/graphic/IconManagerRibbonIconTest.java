/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.accessibility.AccessibleRole;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.microproject.pm.graphic.spreadsheet.renderer.IndicatorsRenderer;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;

import com.microproject.menu.testsupport.RibbonInventory;

class IconManagerRibbonIconTest {
	@Test
	void informationIconsRetainVectorRenderingAtCommonScaleFactors() throws Exception {
		var taskHeader = IconManager.getIcon("infomation.icon");
		var ribbon = IconManager.getRibbonIcon("ribbon.information", 16, 16);
		JLabel[] header = new JLabel[1];
		SwingUtilities.invokeAndWait(() -> {
			header[0] = IndicatorsRenderer.getCellHeader();
			assertNotNull(header[0].getIcon(), "the Indicators header must keep its visible icon");
			assertEquals(AccessibleRole.ICON, header[0].getAccessibleContext().getAccessibleRole());
		});
		assertNotNull(taskHeader);
		assertNotNull(ribbon);

		captureScaleEvidence("task-header", taskHeader);
		captureScaleEvidence("ribbon-information", ribbon);

		assertInstanceOf(FlatSVGIcon.class, taskHeader,
			"the task-table information header must remain vector-backed for HiDPI painting");
		assertInstanceOf(FlatSVGIcon.class, ribbon,
			"the ribbon information icon must remain vector-backed for HiDPI painting");
		for (var icon : List.of(taskHeader, ribbon)) {
			assertEquals(16, icon.getIconWidth());
			assertEquals(16, icon.getIconHeight());
			BufferedImage rasterAtLogicalSize = paint(icon, 16, 16);
			for (double scale : new double[] {1.0, 1.25, 1.5}) {
				BufferedImage scaled = paintAtScale(icon, scale);
				assertTrue(hasVisiblePixel(scaled),
					() -> "information icon must render at " + scale + "x");
				if (scale > 1.0) {
					BufferedImage enlargedRaster = enlarge(rasterAtLogicalSize, scaled.getWidth(), scaled.getHeight());
					assertTrue(differingPixelCount(scaled, enlargedRaster) > 0,
						() -> "" + scale + "x rendering must contain detail beyond a 16px raster enlargement");
				}
			}
		}
	}

	@Test
	void taskRibbonBandIconsRemainVectorBackedAcrossScaleAndStateVariants() {
		Color tint = new Color(0x5b, 0x6f, 0x91);
		for (String key : List.of("ribbon.moveTaskUp", "ribbon.markOnTrack", "ribbon.delete")) {
			for (int size : new int[] {16, 20, 32}) {
				assertInstanceOf(FlatSVGIcon.class, IconManager.getRibbonIcon(key, size, size),
					() -> key + " must use its SVG source at " + size + " logical pixels");
			}

			var normal = IconManager.getRibbonIcon(key, 32, 32);
			var disabled = IconManager.getRibbonIconDisabled(key, 32, 32);
			var tinted = IconManager.getRibbonIconTinted(key, 32, 32, tint);
			assertNotNull(disabled, key + " disabled state");
			assertNotNull(tinted, key + " tinted state");
			assertNotSame(normal, disabled, "disabled filtering must not mutate the cached normal icon");
			assertNotSame(normal, tinted, "tint filtering must not mutate the cached normal icon");
			assertNotNull(disabled.getImage(), "disabled ImageIcon consumers need a filtered logical raster");
			assertNotNull(tinted.getImage(), "tinted ImageIcon consumers need a filtered logical raster");

			BufferedImage logicalNormal = paint(normal, 32, 32);
			BufferedImage logicalDisabled = paint(disabled, 32, 32);
			BufferedImage logicalTinted = paint(tinted, 32, 32);
			assertEquals(Math.round(maxAlpha(logicalNormal) * 0.38f), maxAlpha((BufferedImage) disabled.getImage()),
				key + " disabled getImage must preserve its opacity semantics");
			assertTrue(opaquePixelsMatchRgbWithin((BufferedImage) tinted.getImage(), tint, 0),
				key + " tinted getImage must retain the exact requested interior colour");
			captureScaleEvidence(key + "-normal", normal);
			captureScaleEvidence(key + "-disabled", disabled);
			captureScaleEvidence(key + "-tinted", tinted);
			for (double scale : new double[] {1.0, 1.25, 1.5}) {
				int scaledSize = (int) Math.ceil(32 * scale);
				BufferedImage normalAtScale = paintAtScale(normal, scale);
				BufferedImage disabledAtScale = paintAtScale(disabled, scale);
				BufferedImage tintedAtScale = paintAtScale(tinted, scale);
				for (BufferedImage image : List.of(normalAtScale, disabledAtScale, tintedAtScale)) {
					assertEquals(scaledSize, image.getWidth(), key + " output width at " + scale + "x");
					assertEquals(scaledSize, image.getHeight(), key + " output height at " + scale + "x");
					assertTrue(hasVisiblePixel(image), key + " state must remain visible at " + scale + "x");
				}
				assertTrue(allVisiblePixelsAreGray(disabledAtScale),
					key + " disabled state must retain its gray appearance at " + scale + "x");
				assertEquals(Math.round(maxAlpha(normalAtScale) * 0.38f), maxAlpha(disabledAtScale),
					key + " disabled state must retain its 38% source opacity at " + scale + "x");
				assertTrue(opaquePixelsMatchRgbWithin(tintedAtScale, tint, 8),
					() -> key + " tinted state must retain its requested color on opaque pixels at " + scale + "x: "
						+ visibleRgbSummary(tintedAtScale));
				assertEquals(maxAlpha(normalAtScale), maxAlpha(tintedAtScale),
					key + " tinting must preserve source opacity at " + scale + "x");
				if (scale > 1.0) {
					BufferedImage enlargedRaster = enlarge(logicalNormal, scaledSize, scaledSize);
					assertTrue(differingPixelCount(normalAtScale, enlargedRaster) > 0,
						key + " " + scale + "x vector render must add detail beyond raster enlargement");
					assertTrue(differingPixelCount(disabledAtScale, enlarge(logicalDisabled, scaledSize, scaledSize)) > 0,
						key + " " + scale + "x disabled render must add detail beyond raster enlargement");
					assertTrue(differingPixelCount(tintedAtScale, enlarge(logicalTinted, scaledSize, scaledSize)) > 0,
						key + " " + scale + "x tinted render must add detail beyond raster enlargement");
				}
			}
		}
	}

	@Test
	void rasterizedRibbonIconsAreCachedByNameAndSize() {
		var first = IconManager.getRibbonIcon("ribbon.open", 20, 20);
		var repeated = IconManager.getRibbonIcon("ribbon.open", 20, 20);
		var otherSize = IconManager.getRibbonIcon("ribbon.open", 32, 32);

		assertSame(first, repeated, "rebuilding the same ribbon group should reuse its rasterized icon");
		assertNotSame(first, otherSize, "each optical size needs its own cached raster");
	}

	@Test
	void officeRibbonIconsKeepColourAndUseTranslucentDisabledVariants() {
		var normal = IconManager.getRibbonIcon("ribbon.open", 32, 32);
		var disabled = IconManager.getRibbonIconDisabled("ribbon.open", 32, 32);
		assertNotNull(normal);
		assertNotNull(disabled);
		BufferedImage normalCanvas = paint(normal, 32, 32);
		BufferedImage disabledCanvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		var disabledGraphics = disabledCanvas.createGraphics();
		try {
			disabled.paintIcon(null, disabledGraphics, 0, 0);
		} finally {
			disabledGraphics.dispose();
		}
		assertTrue(visibleColorCount(normalCanvas) >= 3, "enabled SVG should retain semantic colours");
		assertTrue(hasHighSaturationPixel(normalCanvas), "enabled SVG should retain a high-contrast source colour");
		assertTrue(maxAlpha(disabledCanvas) <= 100,
			"disabled SVG should use approximately 38% opacity; max alpha=" + maxAlpha(disabledCanvas));
	}

	@Test
	void quickAccessSaveIconPreservesItsInteriorWhenTheChromeAppliesItsSharedTint() {
		assertEquals("document-save-outline.svg", IconManager.getConfiguredIconName("ribbon.saveToolbar"));
		var menu = java.util.ResourceBundle.getBundle("com/microproject/menu/menuInternal");
		assertEquals("ribbon.saveToolbar", menu.getString("RibbonTopBarSaveProject.icon"),
			"the shared title-bar icon tint requires an outline glyph with transparent interior");
		BufferedImage image = paint(IconManager.getRibbonIconTinted("ribbon.saveToolbar", 32, 32,
			new java.awt.Color(0x243247)), 32, 32);
		assertTrue((image.getRGB(17, 20) >>> 24) == 0,
			"the center of the save-label outline must remain transparent after tinting");
		assertTrue(hasVisiblePixel(image), "the tinted quick-access save glyph must remain visible");
	}

	@Test
	void chartAndHistogramIconsUseTheOfficeSeriesPalette() {
		assertEquals("histogram.svg", IconManager.getConfiguredIconName("view.histogram"));
		var icon = IconManager.getIcon("view.histogram");
		assertNotNull(icon);
		BufferedImage image = paint(icon, icon.getIconWidth(), icon.getIconHeight());
		assertTrue(containsRgb(image, 0x4472C4), "first histogram bar should use the task blue");
		assertTrue(containsRgb(image, 0xED7D31), "second histogram bar should use the Office orange");
		assertTrue(containsRgb(image, 0x7F7F7F), "third histogram bar should use a neutral baseline gray");

		assertEquals("chart.svg", IconManager.getConfiguredIconName("view.charts"));
		var chartIcon = IconManager.getIcon("view.charts");
		assertNotNull(chartIcon);
		BufferedImage chartImage = paint(chartIcon, chartIcon.getIconWidth(), chartIcon.getIconHeight());
		assertEquals(2, accentColorCount(chartImage), "chart view icon should use the Office blue and orange accents");
	}

	@Test
	void informationRibbonIconUsesTheSharedFluentMonochromeGlyph() {
		assertEquals("fluent-info.svg", IconManager.getConfiguredIconName("ribbon.information"));
		assertMonochromeGlyph("ribbon.information");
	}

	@Test
	void customFieldsRibbonIconUsesTheSharedFluentMonochromeGlyph() {
		assertEquals("fluent-grid-dots.svg", IconManager.getConfiguredIconName("ribbon.customFields"));
		assertMonochromeGlyph("ribbon.customFields");
	}

	private static void assertMonochromeGlyph(String key) {
		BufferedImage image = paint(IconManager.getRibbonIcon(key, 32, 32), 32, 32);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getRGB(x, y);
				if ((pixel >>> 24) == 0) continue;
				int red = (pixel >>> 16) & 0xff;
				int green = (pixel >>> 8) & 0xff;
				int blue = pixel & 0xff;
				assertTrue(Math.abs(red - green) <= 1 && Math.abs(green - blue) <= 1,
					key + " must remain monochrome and not reintroduce the old green outlined icon frame");
			}
		}
	}

	/**
	 * Microsoft Project documents distinct manual and automatic task modes and
	 * mode indicators, but does not specify the ribbon pictograms' artwork or
	 * colors. The pencil and clock are product icons styled with the app theme.
	 * https://support.microsoft.com/en-us/project/task-mode-task-field
	 */
	@Test
	void manualAndAutomaticTaskModeIconsAreDistinctAndThemeColored() {
		assertEquals("task-mode-manual.svg", IconManager.getConfiguredIconName("ribbon.taskModeManual"));
		assertEquals("task-mode-automatic.svg", IconManager.getConfiguredIconName("ribbon.taskModeAutomatic"));
		for (int size : new int[] {16, 20, 32}) {
			assertTrue(IconManager.hasSizeSpecificRibbonResource("ribbon.taskModeManual", size));
			assertTrue(IconManager.hasSizeSpecificRibbonResource("ribbon.taskModeAutomatic", size));
		}
		BufferedImage manual = paint(IconManager.getRibbonIcon("ribbon.taskModeManual", 32, 32), 32, 32);
		BufferedImage automatic = paint(IconManager.getRibbonIcon("ribbon.taskModeAutomatic", 32, 32), 32, 32);
		assertTrue(containsRgb(manual, 0xED7D31), "manual scheduling should show the pencil accent");
		assertTrue(containsRgb(automatic, 0x70AD47), "automatic scheduling should show the clock accent");
	}
	static Set<String> standardRibbonKeys() {
		Set<String> keys = new LinkedHashSet<>();
		for (var spec : RibbonInventory.standardRibbon().buttons().values()) {
			if (spec.requiresIcon()) {
				keys.add(spec.iconKey());
			}
		}
		return keys;
	}

	static Stream<String> auxiliaryRibbonIconKeys() {
		Set<String> standardKeys = standardRibbonKeys();
		return Stream.of("ribbon.save", "ribbon.open", "ribbon.print", "ribbon.pdf", "ribbon.insertRecurring",
			"application.icon", "application.icon.small", "logo.microProject", "logo.microProject.ribbon")
			.filter(key -> !standardKeys.contains(key));
	}

	static Stream<Arguments> standardRibbonIconCases() {
		return standardRibbonKeys().stream()
			.flatMap(key -> IntStream.of(16, 20, 32).mapToObj(size -> Arguments.of(key, size)));
	}

	@ParameterizedTest
	@MethodSource("auxiliaryRibbonIconKeys")
	void ribbonIconsLoadSynchronouslyAndPaintVisiblePixels(String key) {
		var icon = IconManager.getRibbonIcon(key, 32, 32);
		assertNotNull(icon, () -> key + " did not resolve to a ribbon icon");
		assertEquals(32, icon.getIconWidth());
		assertEquals(32, icon.getIconHeight());
		BufferedImage canvas = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		var g2 = canvas.createGraphics();
		try {
			icon.paintIcon(null, g2, 0, 0);
		} finally {
			g2.dispose();
		}

		assertTrue(hasVisiblePixel(canvas), () -> key + " rendered only transparent pixels");
	}

	@ParameterizedTest
	@MethodSource("standardRibbonIconCases")
	void standardRibbonIconsResolveAtRequestedSizeAndPaintVisiblePixels(String key, int size) {
		assertTrue(IconManager.hasSvgResource(key), () -> key + " must resolve to an SVG ribbon asset");
		assertTrue(IconManager.hasSizeSpecificRibbonResource(key, size),
			() -> key + " is missing its optically sized " + size + "px SVG");
		var icon = IconManager.getRibbonIcon(key, size, size);
		assertNotNull(icon);
		assertEquals(size, icon.getIconWidth());
		assertEquals(size, icon.getIconHeight());
		assertTrue(hasVisiblePixel(paint(icon, size, size)),
			() -> key + " rendered only transparent pixels at " + size + "px");
	}

	@Test
	void standardRibbonIconsUseARestrainedOfficePalette() {
		List<String> overColouredIcons = new ArrayList<>();
		for (String key : standardRibbonKeys()) {
			var icon = IconManager.getRibbonIcon(key, 32, 32);
			if (accentColorCount(paint(icon, 32, 32)) > 2) {
				overColouredIcons.add(key);
			}
		}
		assertTrue(overColouredIcons.isEmpty(), () -> String.join(", ", overColouredIcons) + " should use no more than two accent colours");
	}

	private static boolean hasVisiblePixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (((image.getRGB(x, y) >>> 24) & 0xFF) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean containsRgb(BufferedImage image, int rgb) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, y) & 0x00FFFFFF) == rgb) return true;
			}
		}
		return false;
	}

	private static BufferedImage paint(javax.swing.Icon icon, int width, int height) {
		BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var graphics = canvas.createGraphics();
		try {
			icon.paintIcon(null, graphics, 0, 0);
		} finally {
			graphics.dispose();
		}
		return canvas;
	}

	private static BufferedImage paintAtScale(javax.swing.Icon icon, double scale) {
		int width = (int) Math.ceil(icon.getIconWidth() * scale);
		int height = (int) Math.ceil(icon.getIconHeight() * scale);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var g2 = image.createGraphics();
		try {
			g2.scale(scale, scale);
			icon.paintIcon(null, g2, 0, 0);
		} finally {
			g2.dispose();
		}
		return image;
	}

	private static BufferedImage enlarge(BufferedImage source, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var g2 = image.createGraphics();
		try {
			g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
				java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.drawImage(source, 0, 0, width, height, null);
		} finally {
			g2.dispose();
		}
		return image;
	}

	private static int differingPixelCount(BufferedImage first, BufferedImage second) {
		int count = 0;
		for (int y = 0; y < first.getHeight(); y++) {
			for (int x = 0; x < first.getWidth(); x++) {
				if (first.getRGB(x, y) != second.getRGB(x, y))
					count++;
			}
		}
		return count;
	}

	private static void captureScaleEvidence(String iconName, javax.swing.Icon icon) {
		String evidenceDirectory = System.getProperty("microproject.issue861.evidenceDir");
		if (evidenceDirectory == null || evidenceDirectory.isBlank())
			return;
		try {
			Path directory = Path.of(evidenceDirectory);
			Files.createDirectories(directory);
			for (double scale : new double[] {1.0, 1.25, 1.5}) {
				String scaleName = (int) Math.round(scale * 100) + "";
				Path output = directory.resolve(iconName + "-" + scaleName + ".png");
				ImageIO.write(paintAtScale(icon, scale), "png", output.toFile());
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("Unable to save issue #861 icon evidence", ex);
		}
	}

	private static int visibleColorCount(BufferedImage image) {
		Set<Integer> colors = new LinkedHashSet<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) > 32) colors.add(argb & 0x00ffffff);
			}
		}
		return colors.size();
	}

	private static int maxAlpha(BufferedImage image) {
		int max = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				max = Math.max(max, (image.getRGB(x, y) >>> 24) & 0xff);
			}
		}
		return max;
	}

	private static boolean allVisiblePixelsAreGray(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) == 0) continue;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				if (red != green || green != blue) return false;
			}
		}
		return true;
	}

	private static boolean opaquePixelsMatchRgbWithin(BufferedImage image, Color color, int tolerance) {
		boolean foundOpaquePixel = false;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) < 0xf0) continue;
				foundOpaquePixel = true;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				if (Math.abs(red - color.getRed()) > tolerance
					|| Math.abs(green - color.getGreen()) > tolerance
					|| Math.abs(blue - color.getBlue()) > tolerance) return false;
			}
		}
		return foundOpaquePixel;
	}

	private static String visibleRgbSummary(BufferedImage image) {
		Set<Integer> colors = new LinkedHashSet<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) > 0)
					colors.add(argb & 0x00ffffff);
			}
		}
		return colors.stream().limit(8).map(color -> String.format("#%06x", color)).toList().toString();
	}

	private static int accentColorCount(BufferedImage image) {
		Set<Integer> hueFamilies = new LinkedHashSet<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) < 0xe0) continue;
				int red = (argb >>> 16) & 0xff;
				int green = (argb >>> 8) & 0xff;
				int blue = argb & 0xff;
				float[] hsb = java.awt.Color.RGBtoHSB(red, green, blue, null);
				if (hsb[1] >= 0.30f && hsb[2] >= 0.25f) {
					// Anti-aliasing creates many RGB values from one source colour. Audit
					// semantic colour families instead of counting those raster shades.
					hueFamilies.add(Math.round(hsb[0] * 12.0f));
				}
			}
		}
		return hueFamilies.size();
	}

	private static boolean hasHighSaturationPixel(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				if (((argb >>> 24) & 0xff) < 160) continue;
				float[] hsb = java.awt.Color.RGBtoHSB(
					(argb >>> 16) & 0xff,
					(argb >>> 8) & 0xff,
					argb & 0xff,
					null);
				if (hsb[1] >= 0.55f && hsb[2] >= 0.45f) return true;
			}
		}
		return false;
	}
}
