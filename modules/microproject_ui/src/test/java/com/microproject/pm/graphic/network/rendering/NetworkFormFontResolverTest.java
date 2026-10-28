/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.graphic.network.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.FormBoxLayout;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.FormBox;
import com.microproject.graphic.configuration.FormFormat;
import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.theme.MicroProjectThemeTokens;
import com.microproject.util.Environment;

class NetworkFormFontResolverTest {
	@Test
	void resolvesConfiguredNetworkFormFontStylesWithoutChangingTheirValues() {
		String previousFont = Environment.getFont(Environment.NETWORK_FONT);
		Environment.setFont("Dialog PLAIN 11", Environment.NETWORK_FONT);
		try {
			FormBoxLayout layout = new FormBoxLayout();
			layout.setTitleFont("_Default_ BOLD 10");
			layout.setLabelFont("Dialog ITALIC 12");
			layout.setValueFont(null);

			assertEquals(Font.BOLD, NetworkFormFontResolver.resolve(layout, "title").getStyle());
			assertEquals(9, NetworkFormFontResolver.resolve(layout, "title").getSize());
			assertEquals(Font.ITALIC, NetworkFormFontResolver.resolve(layout, "label").getStyle());
			assertEquals(12, NetworkFormFontResolver.resolve(layout, "label").getSize());
			assertEquals(Font.decode("Dialog PLAIN 11"), NetworkFormFontResolver.resolve(layout, "value"));
			assertNull(NetworkFormFontResolver.resolve(layout, "unknown"));
		} finally {
			Environment.setFont(previousFont, Environment.NETWORK_FONT);
		}
	}

	@Test
	void selectsReadableTextColorForNetworkCardFill() {
		boolean wasDark = !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground());
		MicroProjectTheme.installDark();
		try {
			FormComponent lightCard = formWithMiddleFill("WHITE");
		FormComponent darkCard = formWithMiddleFill("BLACK");

			assertEquals(new Color(0x202226), lightCard.getForeground());
			assertEquals(MicroProjectTheme.tokens().labelForeground(), darkCard.getForeground());
			for (java.awt.Component component : lightCard.getComponents())
				assertEquals(lightCard.getForeground(), component.getForeground());

			MicroProjectTheme.installLight();
			Color lightThemeForeground = MicroProjectTheme.tokens().labelForeground();
			assertEquals(lightThemeForeground, formWithMiddleFill("WHITE").getForeground());
			assertEquals(lightThemeForeground, formWithMiddleFill("BLACK").getForeground());
		} finally {
			if (wasDark) MicroProjectTheme.installDark(); else MicroProjectTheme.installLight();
		}
	}

	@Test
	void selectsTextColorFromDominantPatternBackground() {
		boolean wasDark = !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground());
		MicroProjectTheme.installDark();
		try {
			FormComponent patternedCard = formWithMiddleFill("BLACK", "DEFAULT", true);
			patternedCard.setSize(240, 80);
			java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(240, 80,
				java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D graphics = image.createGraphics();
			try {
				patternedCard.paint(graphics);
			} finally {
				graphics.dispose();
			}

			assertEquals(new Color(0x202226), patternedCard.getForeground(),
				"the mostly white texture background needs dark text even when its pattern color is black");
		} finally {
			if (wasDark) MicroProjectTheme.installDark(); else MicroProjectTheme.installLight();
		}
	}

	@Test
	void darkThemeRestylesOnlyUnmodifiedBuiltInWbsAndRbsCardsAtPaintTime() {
		// Microsoft Project documents that users choose bar shape, pattern, and color, but prescribes no dark-mode RGB values.
		// Therefore only untouched built-in signatures inherit microProject's semantic dark surface tokens; customized formats stay intact.
		// https://support.microsoft.com/en-us/project/format-the-bar-chart-of-a-gantt-chart-view
		boolean wasDark = !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground());
		MicroProjectTheme.installDark();
		try {
			FormComponent standard = formWithBuiltInStyle("WBS.task", "BLACK");
			FormComponent customized = formWithBuiltInStyle("WBS.task", "RED");
			assertEquals(MicroProjectTheme.tokens().labelForeground(), standard.getForeground());
			assertEquals(new Color(0x202226), customized.getForeground());
			assertEquals(MicroProjectTheme.tokens().spreadsheetBodyBackground(), dominantColor(render(standard)));
			assertEquals(Color.WHITE, dominantColor(render(customized)), "custom color must retain the configured white-backed pattern");
		} finally {
			if (wasDark) MicroProjectTheme.installDark(); else MicroProjectTheme.installLight();
		}
	}

	@Test
	void customStartAndEndShapesKeepTheirConfiguredColorsAndPaintInDarkTheme() {
		boolean wasDark = !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground());
		MicroProjectTheme.installDark();
		try {
			for (String endpoint : List.of("start", "end")) {
				FormComponent form = formWithBuiltInStyle("WBS.task", "BLACK", endpoint);
				var image = render(form);
				assertEquals(Color.WHITE, dominantColor(image), endpoint + " custom shape must keep its transparent paint background");
				assertTrue(containsColor(image, Color.RED), endpoint + " custom shape must keep its configured color");
			}
		} finally {
			if (wasDark) MicroProjectTheme.installDark(); else MicroProjectTheme.installLight();
		}
	}

	@Test
	void lightThemeUsesQuietAccentCardsForBuiltInPertFormatsAndPreservesCustomFormats() {
		boolean wasDark = !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.light().appBackground());
		MicroProjectTheme.installLight();
		try {
			FormComponent standard = formWithBuiltInStyle("Pert.task", "BLUE");
			FormComponent wbs = formWithBuiltInStyle("WBS.task", "BLACK");
			FormComponent customized = formWithBuiltInStyle("Pert.task", "RED");
			Color standardFill = dominantColor(render(standard));
			Color wbsFill = dominantColor(render(wbs));
			Color customFill = dominantColor(render(customized));

			assertNotEquals(Color.WHITE, standardFill, "the built-in task texture should become a restrained accent wash");
			assertNotEquals(Color.WHITE, wbsFill, "the built-in WBS card should use the quiet neutral surface");
			assertEquals(Color.WHITE, customFill, "customized format keeps its configured texture");
		} finally {
			if (wasDark) MicroProjectTheme.installDark(); else MicroProjectTheme.installLight();
		}
	}

	private static boolean containsColor(java.awt.image.BufferedImage image, Color expected) {
		for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
			if (image.getRGB(x, y) == expected.getRGB()) return true;
		return false;
	}

	private static java.awt.image.BufferedImage render(FormComponent form) {
		form.setSize(240, 80);
		var image = new java.awt.image.BufferedImage(240, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		var graphics = image.createGraphics();
		try { form.paint(graphics); } finally { graphics.dispose(); }
		return image;
	}

	private static Color dominantColor(java.awt.image.BufferedImage image) {
		var counts = new java.util.HashMap<Integer, Integer>();
		int best = 0, bestCount = 0;
		for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
			int rgb = image.getRGB(x, y) & 0x00ffffff;
			int count = counts.merge(rgb, 1, Integer::sum);
			if (count > bestCount) { best = rgb; bestCount = count; }
		}
		return new Color(best);
	}

	private static FormComponent formWithBuiltInStyle(String id, String colorName) {
		return formWithBuiltInStyle(id, colorName, null);
	}

	private static FormComponent formWithBuiltInStyle(String id, String colorName, String customEndpoint) {
		TexturedShape middle = new TexturedShape();
		middle.setShapeName("FULL_HEIGHT");
		middle.setColorName(colorName);
		middle.setPaintName(id.equals("Pert.task") ? "VERY_SPACED_DOTS" : "TRANSPARENT");
		middle.setStrokeName("LARGE_FRAMED");
		BarFormat format = new BarFormat();
		format.setId(id);
		format.setMiddle(middle);
		if (customEndpoint != null) {
			TexturedShape endpoint = new TexturedShape();
			endpoint.setShapeName("FULL_HEIGHT");
			endpoint.setColorName("RED");
			endpoint.setPaintName("TRANSPARENT");
			endpoint.setStrokeName("LARGE_FRAMED");
			if ("start".equals(customEndpoint)) format.setStart(endpoint);
			else format.setEnd(endpoint);
		}
		FormFormat form = new FormFormat();
		FormBoxLayout layout = new FormBoxLayout(); layout.setDefaultZoom(true); layout.setColumnGrid("pref"); layout.setRowGrid("pref"); form.addLayout(layout);
		FormBox box = new FormBox(); box.setColumn(1); box.setRow(1); box.setTextId("Field.name"); form.addBox(box);
		format.setForm(form);
		return new FormComponent(List.of(format), 0, false, true);
	}

	private static FormComponent formWithMiddleFill(String colorName) {
		return formWithMiddleFill(colorName, "SOLID", false);
	}

	private static FormComponent formWithMiddleFill(String colorName, String paintName, boolean texture) {
		TexturedShape middle = new TexturedShape();
		middle.setShapeName("FULL_HEIGHT");
		middle.setColorName(colorName);
		middle.setPaintName(paintName);
		BarFormat format = new BarFormat();
		format.setMiddle(middle);

		FormFormat form = new FormFormat();
		FormBoxLayout layout = new FormBoxLayout();
		layout.setDefaultZoom(true);
		layout.setColumnGrid("pref");
		layout.setRowGrid("pref");
		form.addLayout(layout);
		FormBox box = new FormBox();
		box.setColumn(1);
		box.setRow(1);
		box.setTextId("Field.name");
		form.addBox(box);
		format.setForm(form);
		return new FormComponent(List.of(format), 0, false, texture);
	}
}
