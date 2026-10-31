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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.microproject.ui.theme.MicroProjectThemeTokens;

class ColorLuminanceTest {
	private static final List<Function<MicroProjectThemeTokens, Color>> BACKGROUNDS = List.of(
		MicroProjectThemeTokens::appBackground,
		MicroProjectThemeTokens::workspaceBackground,
		MicroProjectThemeTokens::panelBackground,
		MicroProjectThemeTokens::dialogBackground,
		MicroProjectThemeTokens::dialogSurfaceBackground,
		MicroProjectThemeTokens::ribbonChromeBackground,
		MicroProjectThemeTokens::ribbonSurfaceBackground,
		MicroProjectThemeTokens::tableBackground,
		MicroProjectThemeTokens::spreadsheetBodyBackground);

	/** Every theme surface that the former dark tests could receive. */
	@Test
	void classifiesEveryThemeBackgroundAsItsMode() {
		for (Function<MicroProjectThemeTokens, Color> background : BACKGROUNDS) {
			Color light = background.apply(MicroProjectThemeTokens.light());
			Color dark = background.apply(MicroProjectThemeTokens.dark());
			assertFalse(ColorLuminance.isDark(light), "light " + Integer.toHexString(light.getRGB()));
			assertTrue(ColorLuminance.isDark(dark), "dark " + Integer.toHexString(dark.getRGB()));
			// The retired RGB-sum test agreed on all real theme surfaces.
			assertEquals(light.getRed() + light.getGreen() + light.getBlue() < 384, ColorLuminance.isDark(light));
			assertEquals(dark.getRed() + dark.getGreen() + dark.getBlue() < 384, ColorLuminance.isDark(dark));
		}
	}

	@Test
	void classifiesBoundaryAndNullColours() {
		assertTrue(ColorLuminance.isDark(Color.BLACK));
		assertFalse(ColorLuminance.isDark(Color.WHITE));
		assertFalse(ColorLuminance.isDark(null));
		assertTrue(ColorLuminance.isDark(new Color(0x75, 0x75, 0x75)));
		assertFalse(ColorLuminance.isDark(new Color(0x76, 0x76, 0x76)));
		// Saturated blue is dark by luminance although its RGB sum is mid-range.
		assertTrue(ColorLuminance.isDark(new Color(0, 0, 255)));
		assertFalse(ColorLuminance.isDark(new Color(0, 255, 0)));
	}

	@Test
	void chartLegendPicksReadableTextByTheSameClassification() {
		for (Color color : List.of(Color.BLACK, Color.WHITE, new Color(0x483D8B), new Color(0xFFD966), new Color(0x2F5597)))
			assertEquals(ColorLuminance.relativeLuminance(color) > 0.179 ? Color.BLACK : Color.WHITE,
				ColorLuminance.isDark(color) ? Color.WHITE : Color.BLACK);
	}

	@Test
	void mixMatchesRetiredVariantsForPaintedWeights() {
		float[] flatWeights = {0.88f, 0.08f, 0.16f, 0.14f, 0.38f, 0.50f, 0.46f, 0.20f, 0.82f, 0.94f};
		float[] paletteRatios = {0.42f, 0.24f, 0.05f, 0.08f, 0.82f};
		Color[] colors = {Color.WHITE, Color.BLACK, new Color(0x0078D7), new Color(0x217346), new Color(0xD13438),
			new Color(0x1F2937), new Color(0xF8FAFC), new Color(0x202226), new Color(0xE3E5E8), new Color(0x4472C4)};
		for (Color a : colors)
			for (Color b : colors) {
				for (float weight : flatWeights)
					assertEquals(oldFlatBlend(a, b, weight), ColorLuminance.mix(b, a, weight));
				for (float ratio : paletteRatios) {
					assertEquals(oldMix(a, Color.WHITE, ratio), ColorLuminance.mix(a, Color.WHITE, ratio));
					assertEquals(oldMix(a, Color.BLACK, ratio), ColorLuminance.mix(a, Color.BLACK, ratio));
				}
			}
	}

	/** FormComponent used a double 0.10 weight; compare on its real light-theme inputs. */
	@Test
	void mixMatchesDiagramFormBlendOnLightThemeColours() {
		MicroProjectThemeTokens light = MicroProjectThemeTokens.light();
		for (Color accent : List.of(light.errorColor(), light.accentColor(), light.labelForeground()))
			assertEquals(oldDoubleBlend(light.spreadsheetBodyBackground(), accent, 0.10d),
				ColorLuminance.mix(light.spreadsheetBodyBackground(), accent, 0.10f));
	}

	@Test
	void mixClampsWeightAndStaysOpaque() {
		Color translucent = new Color(10, 20, 30, 40);
		assertEquals(new Color(200, 100, 50), ColorLuminance.mix(translucent, new Color(200, 100, 50), 2f));
		assertEquals(new Color(10, 20, 30), ColorLuminance.mix(translucent, new Color(200, 100, 50), -1f));
		assertEquals(255, ColorLuminance.mix(translucent, Color.WHITE, 0.5f).getAlpha());
	}

	private static Color oldFlatBlend(Color first, Color second, float firstWeight) {
		float weight = Math.max(0f, Math.min(1f, firstWeight));
		float secondWeight = 1f - weight;
		return new Color(Math.round(first.getRed() * weight + second.getRed() * secondWeight),
			Math.round(first.getGreen() * weight + second.getGreen() * secondWeight),
			Math.round(first.getBlue() * weight + second.getBlue() * secondWeight));
	}

	private static Color oldMix(Color color, Color target, float ratio) {
		float weight = Math.max(0f, Math.min(1f, ratio));
		float targetWeight = 1f - weight;
		return new Color(Math.round(color.getRed() * targetWeight + target.getRed() * weight),
			Math.round(color.getGreen() * targetWeight + target.getGreen() * weight),
			Math.round(color.getBlue() * targetWeight + target.getBlue() * weight));
	}

	private static Color oldDoubleBlend(Color background, Color foreground, double amount) {
		return new Color((int) Math.round(background.getRed() * (1d - amount) + foreground.getRed() * amount),
			(int) Math.round(background.getGreen() * (1d - amount) + foreground.getGreen() * amount),
			(int) Math.round(background.getBlue() * (1d - amount) + foreground.getBlue() * amount));
	}
}
